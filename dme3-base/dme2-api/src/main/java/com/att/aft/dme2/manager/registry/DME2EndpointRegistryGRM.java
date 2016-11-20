package com.att.aft.dme2.manager.registry;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2EndpointUtil;
import com.att.aft.dme2.manager.registry.util.DME2Protocol;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.DME2ValidationUtil;
import com.att.aft.dme2.util.ErrorContext;
import com.att.scld.grm.types.v1.ClientJVMAction;
import com.att.scld.grm.types.v1.ClientJVMInstance;
import com.att.scld.grm.v1.FindClientJVMInstanceRequest;
import com.att.scld.grm.v1.RegisterClientJVMInstanceRequest;

/**
 * DME2 Endpoint Registry for GRM
 */
public class DME2EndpointRegistryGRM extends DME2AbstractEndpointRegistry implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger( DME2EndpointRegistryGRM.class );
  private static final String EXTENDED_STRING = "extendedMessage";
  private static final String MANAGER = "manager";
  private static final String KEY_ENABLE_GRM_TOPOLOGY_SERVICE_OVERRIDE = "KEY_ENABLE_GRM_TOPOLOGY_SERVICE_OVERRIDE";
  private static final String DME2_EP_ACCESSOR_CLASS = "DME2_EP_ACCESSOR_CLASS";
  private static final String AFT_DME2_GRM_USER = DME2Constants.DME2_GRM_USER;
  private static final String AFT_DME2_GRM_PASS = DME2Constants.DME2_GRM_PASS;
  private static final String DME_EXCEPTION_INIT_EP_REGISTRY_GRM = "AFT-DME2-0600";
  private static final String REGEX0TO9 = "=[0-9]+";

  private DME2Configuration config;
  private JAXBContext jaxBContext = null;
  private BaseAccessor grm;
  private DME2EndpointCacheGRM endpointCache;
  private DME2RouteInfoCacheGRM routeInfoCache;
  private long sepCacheTtlMs;
  private long sepCacheEmptyTtlMs;
  private long routeInfoCacheTtlMs;
  private int sEPLEASELENGTHMS;
  private boolean iGNOREEXPIREDLEASESEPS;
  private final Map<String, Boolean> fetchRouteInfoFromGRM = new HashMap<String, Boolean>();
  private boolean initialized = false;
  private final byte[] ldapLockObj = new byte[0];
  private final byte[] routeInfoLockObj = new byte[0];

  private long[] emptyCacheTTLRefreshIntervals;
  private final long[] emptyCacheTTLRefreshDefaultIntervals = new long[]{ 300000, 300000, 300000, 600000, 900000 };
  /**
   * List of endpoints published locally
   */
  private final List<DME2Endpoint> localPublishedList = Collections.synchronizedList( new ArrayList<DME2Endpoint>() );

  /**
   * DME2 Endpoint Registry GRM
   *
   * @param managerName Manager
   * @throws DME2Exception
   */
  public DME2EndpointRegistryGRM( DME2Configuration config, String managerName ) throws DME2Exception {
    super( config, managerName );
    this.config = config;
    staleEndpointCache =
        new DME2StaleCache( config, DME2Endpoint.class, DME2EndpointRegistryType.GRM, this, managerName );
    staleRouteOfferCache =
        new DME2StaleCache( config, DME2RouteOffer.class, DME2EndpointRegistryType.GRM, this, managerName );
    // Try with the ContextClassLoader, the Class' ClassLoader and the System Class Loader

    // TODO: Fix to read config
    emptyCacheTTLRefreshIntervals = emptyCacheTTLRefreshDefaultIntervals;

    ClassLoader[] cls = new ClassLoader[]{
        Thread.currentThread().getContextClassLoader(),
        getClass().getClassLoader(),
        ClassLoader.getSystemClassLoader()
    };

    for ( ClassLoader cl : Arrays.asList( cls ) ) {
      if ( cl != null ) {
        try {
          Thread.currentThread().setContextClassLoader( cl );
          loadJAXBContext();
          break;
        } catch ( Exception e ) {
          // Just try again with the next ClassLoader
        }
      }
    }
    if ( jaxBContext == null ) {
      throw new DME2Exception( DME2Constants.EXC_REGISTRY_JAXB,
          "Unable to create JAXBContext with any ClassLoader for RouteInfo" );
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param properties Properties to use in initialization
   * @throws DME2Exception
   */
  @Override
  public void init( Properties properties ) throws DME2Exception {
    super.init( properties );

    endpointCache = new DME2EndpointCacheGRM( config, this, managerName, false );
    routeInfoCache = new DME2RouteInfoCacheGRM( config, this, managerName );

    try {
      grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance( config, SecurityContext.create( config ) );

			/*	Defaults
        CACHE_REFRESH_FREQUENCY - 300000 (5 mins)
				LEASE_FREQUENCY - 900000 (15 mins)
			 	ROUTE_INFO_REFRESH_FREQUENCY - 300000 (5 mins)*/

      // ttl for healthy sep lists
      sepCacheTtlMs = getConfig().getLong( "DME2_SEP_CACHE_TTL_MS" ); // Default, "300000" ) );

      // ttl for empty sep lists - by default, check more frequently than standard refresh rate
      sepCacheEmptyTtlMs = getConfig().getLong( "DME2_SEP_CACHE_EMPTY_TTL_MS" ); // Default, "300000" ) );

      // ttl for route info data
      routeInfoCacheTtlMs = getConfig().getLong( "DME2_ROUTEINFO_CACHE_TTL_MS" ); // Default, "300000" ) );

      // lease length
      // default 30 mins
      sEPLEASELENGTHMS = getConfig().getInt( DME2Constants.DME2_SEP_LEASE_LENGTH_MS ); // Default, "1800000" ) );

      // when true, we will ignore the expiration data when considering endpoints.  If false, we'll ignore
      // stale endpoints. You generally should NEVER ignore any endpoints returned...
      iGNOREEXPIREDLEASESEPS = getConfig().getBoolean( "DME2_SEP_IGNORE_LEASE_EXPIRED" ); // Default, "true" ) );

			/*Default to 1 hour*/
      /** The time-to-live value that the cached entries should remain in the cache for. If the entries exceed this value,
       * then the next time a call to find Endpoints/RouteInfo is made. Get the information from GRM rather than getting the old cached data.*/

      boolean shutdownEnabled = getConfig().getBoolean( "AFT_DME2_REG_SHUTDOWN_HOOK" ); // Default, "true" ) );

      if ( shutdownEnabled ) {
        Runtime.getRuntime().addShutdownHook( new Thread( this ) );
      }

      initialized = true;
    } catch ( Throwable e ) {
      throw new DME2Exception( DME_EXCEPTION_INIT_EP_REGISTRY_GRM,
          new ErrorContext().add( EXTENDED_STRING, e.getMessage() )
          //.add( MANAGER, this.getManager().getName() )
          , e );
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param service   Service name
   * @param path      Service path
   * @param host      Service host
   * @param port      Service port
   * @param latitude  Service location latitude
   * @param longitude Service location longitude
   * @param protocol  Service access protocol
   * @throws DME2Exception
   */
  @Override
  public void publish( String service, String path, String host, int port, double latitude, double longitude,
                       String protocol ) throws DME2Exception {
    publish( service, path, host, port, latitude, longitude, protocol, null, false, null );
  }

  /**
   * {@inheritDoc}
   *
   * @param service     Service name
   * @param path        Service path
   * @param host        Service host
   * @param port        Service port
   * @param latitude    Service location latitude
   * @param longitude   Service location longitude
   * @param protocol    Service access protocol
   * @param updateLease Whether to renew the service lease
   * @throws DME2Exception
   */
  @Override
  public void publish( String service, String path, String host, int port, double latitude, double longitude,
                       String protocol, boolean updateLease ) throws DME2Exception {
    publish( service, path, host, port, latitude, longitude, protocol, null, updateLease, null );
  }

  /**
   * {@inheritDoc}
   *
   * @param service  Service name
   * @param path     Service path
   * @param host     Service host
   * @param port     Service port
   * @param protocol Service access protocol
   * @param props    Service Properties
   * @throws DME2Exception
   */
  @Override
  public void publish( String service, String path, String host, int port, String protocol, Properties props )
      throws DME2Exception {
    publish( service, path, host, port, getConfig().getDouble( "AFT_LATITUDE" ), getConfig().getDouble(
        "AFT_LONGITUDE" ), protocol, null, false, props );
  }

  /**
   * {@inheritDoc}
   *
   * @param service  Service name
   * @param path     Service path
   * @param host     Service host
   * @param port     Service port
   * @param protocol Service access protocol
   * @throws DME2Exception
   */
  @Override
  public void publish( String service, String path, String host, int port, String protocol ) throws DME2Exception {
    publish( service, path, host, port, protocol, null );
  }

  /**
   * {@inheritDoc}
   *
   * @param service     Service name
   * @param path        Service path
   * @param host        Service host
   * @param port        Service port
   * @param protocol    Service access protocol
   * @param updateLease Whether to renew the service lease
   * @throws DME2Exception
   */
  @Override
  public void publish( String service, String path, String host, int port, String protocol, boolean updateLease )
      throws DME2Exception {
    publish( service, path, host, port, getConfig().getDouble( "AFT_LATITUDE" ), getConfig().getDouble(
        "AFT_LONGITUDE" ), protocol, null, updateLease, null );
  }

  @Override
  public void publish( String serviceURI, String contextPath, String hostAddress, int port, double latitude,
                       double longitude, String protocol, Properties props, boolean updateLease ) throws DME2Exception {
    publish( serviceURI, contextPath, hostAddress, port, latitude, longitude, protocol, null, updateLease, props );
  }

  private void publish( final String servicePath, String contextPath, String host, int port, Double latitude,
                        Double longitude,
                        String protocol, String namespace, boolean updateLease, Properties props )
      throws DME2Exception {
    assert ( initialized );  // Remove?

    /* Validate required fields for JDBCEndpoints */
    if ( protocol.equalsIgnoreCase( DME2Protocol.DME2JDBC ) ) {
      try {
        DME2ValidationUtil.validateJDBCEndpointRequiredFields( props, servicePath );
      } catch ( DME2Exception e ) {
        throw new DME2Exception( e.getErrorCode(), e.getErrorMessage() );
      }
    }

    //serviceName = tmpService; //TODO validate
    //Build the uniform resource.
    DmeUniformResource uniformResource = buildUniformResource( protocol, servicePath, host, port );

    //clean up the servicePath name
    //strip out the query parms from the servicePath name.
    String tmpPath = servicePath;
    if ( servicePath.contains( "?" ) ) {
      tmpPath = servicePath.split( "\\?" )[0];
    }

		/*Resolving the contextPath*/

    if ( uniformResource.getUrlType() == DmeUniformResource.DmeUrlType.STANDARD &&
        uniformResource.getBindContext() != null ) {
      contextPath = uniformResource.getBindContext();
    } else if ( contextPath == null ) {
      contextPath = tmpPath;
    }

    logger.info( null, "publish",
        "DME2Constants.AFT_DME2_CONTAINER_ENV_KEY: config-{}, config.getProp - {}, config.getProp.config.getProp - {}",
        config != null ? config : null,
        config != null && config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ENV_KEY ) != null ?
            config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ENV_KEY ) : null,
        config != null && config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ENV_KEY ) != null ?
            config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ENV_KEY ) ) : null );
    //now the environment
    String lrmEnv = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ENV_KEY ) );
    // If lrmEnv is found use it for registering the endpoint overriding the envContext from input
    // This will keep the lrm container instance and SEP envContext's in sync.
    String uriEnv = uniformResource.getEnvContext();
    String env = ( lrmEnv != null ? lrmEnv : uriEnv );

    // The below check is to ensure the contextPath published also has the right envContext used
    if ( lrmEnv != null ) {
      logger.debug( null, "publish", LogMessage.PUBLISH_OVERRIDE, lrmEnv, uniformResource.getEnvContext() );
    }

    if ( lrmEnv != null && !uriEnv.equals( lrmEnv ) ) {
      tmpPath = tmpPath.replace( "envContext=" + uriEnv, "envContext=" + lrmEnv );
    }

    //the host name
    String hostFromArgs = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_HOST_KEY ) );
    if ( hostFromArgs == null ) {
      hostFromArgs = host;
    }
    String routeOffer = uniformResource.getRouteOffer();

    if ( routeOffer == null ) {
      routeOffer = config.getProperty( DME2Constants.AFT_DME2_DEFAULT_RO );
    }

    // validate whether runtime provided lrmhost had been spoofed.
    String runTimeHost = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_HOST_KEY ) );

    if ( runTimeHost != null && !DME2Utils.isHostMyLocalHost( runTimeHost, config.getBoolean( DME2Constants.DME2_DEBUG )
    ) ) {
      // The lrmHost arg provided is not a valid address of local host, cannot proceed
      throw new DME2Exception( "AFT-DME2-0613", new ErrorContext()
          //.add( MANAGER, this.getManager().getName() )
          .add( "uri", uniformResource.toString() )
          .add( "host", host )
          .add( "lrmhost", runTimeHost )
          .add( "port", "" + port )
          .add( EXTENDED_STRING, "JVM arg provided lrmhost value does not match a localhost address" ) );
    }

    //build the DME2Endpoint
    DME2Endpoint publishedEndpoint =
        DME2EndpointUtil
            .buildDME2Endpoint( getClientLatitude(), getClientLongitude(), uniformResource, contextPath, env,
                hostFromArgs, port, protocol, latitude, longitude,
                props );

    //publish it.
    try {
      publish( publishedEndpoint );
    } catch ( DME2Exception e ) {
      //conditionally add it to the cache
      if ( isGRMRetryException( e ) ) {
        this.localPublishedList.add( publishedEndpoint );
      }
      throw e;
    }

    //add it to the cache
    this.localPublishedList.add( publishedEndpoint );
    logger.debug( null, "publish", LogMessage.PUBLISH_ENDPOINT, host, port );
  }

  private void publish( DME2Endpoint publishedEndpoint ) throws DME2Exception {
    grm.addServiceEndPoint( DME2EndpointUtil.convertToServiceEndpoint( config, publishedEndpoint ) );
  }

  /**
   * Unpublish the particular endpoint
   *
   * @param endpoint DME2 Endpoint
   * @throws DME2Exception
   */
  public void unpublish( DME2Endpoint endpoint ) throws DME2Exception {
    if ( endpoint.getDmeUniformResource() != null ) {
      unpublish( endpoint.getDmeUniformResource().getOriginalURL().toString(), endpoint.getContextPath(),
          endpoint.getHost(), endpoint.getPort() );
    } else {
      unpublish( endpoint.getPath(), endpoint.getContextPath(), endpoint.getHost(), endpoint.getPort() );
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param serviceName Service name
   * @param host        Service host
   * @param port        Service port
   * @throws DME2Exception
   */
  @Override
  public void unpublish( String serviceName, String host, int port ) throws DME2Exception {
    unpublish( serviceName, null, host, port );
  }

  private void unpublish( final String originalServicePath, String contextPath, String host, int port )
      throws DME2Exception {
    String servicePath = originalServicePath;
    logger.debug( null, "unpublish", LogMessage.UNPUBLISH_ENTER );

    String urlStr = null;

    DmeUniformResource uniformResource = buildUniformResource( null, servicePath, host, port );
    String lrmEnv = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ENV_KEY ) );

    String serviceName = uniformResource.getService();
    String version = uniformResource.getVersion();
    String namespace = uniformResource.getNamespace();
    String env = lrmEnv != null ? lrmEnv : uniformResource.getEnvContext();

    logger.debug( null, "unpublish", LogMessage.UNPUBLISH_ENV, lrmEnv, uniformResource.getEnvContext() );

    ServiceEndpoint serviceEndpoint = new ServiceEndpoint();
    serviceEndpoint.setContextPath( contextPath );
    serviceEndpoint.setHostAddress( host );
    serviceEndpoint.setPort( String.valueOf( port ) );
    serviceEndpoint.setName( serviceName );
    serviceEndpoint.setVersion( version );
    serviceEndpoint.setEnv( env );

    grm.deleteServiceEndPoint( serviceEndpoint );

    logger.debug( null, "unpublish", LogMessage.UNPUBLISHED, host, port );

    if ( !servicePath.startsWith( "/" ) ) {
      servicePath = "/" + servicePath;
    }

    // Remove it from localPublishedList also
    ArrayList<DME2Endpoint> publishedList = new ArrayList<DME2Endpoint>();
    publishedList.addAll( localPublishedList );

    String csvcName = namespace == null ? serviceName : namespace + DME2Constants.getNAME_SEP() + serviceName;

    for ( DME2Endpoint ep : publishedList ) {
      if ( ep.getHost().equals( host ) && ep.getPort() == port && ep.getServiceName().equals( csvcName ) &&
          ep.getEnvContext().equals( env ) ) {
        logger.debug( null, "unpublish",
            "DME2EndpointRegistryGRM.unpublish removing from publishedList ep serviceName={},envContext={},host={},port={}",
            ep.getServiceName(), ep.getEnvContext(), ep.getHost(), ep.getPort() );
        localPublishedList.remove( ep );
      }
    }

    // update local cache if it has this service
    if ( endpointCache.containsKey( servicePath ) ) {
      logger.debug( null, "unpublish", "Removing/refreshing endpoint for localPublishedEndpoint servicePath {}",
          servicePath );

      endpointCache.refreshCachedEndpoint( servicePath );
    }

    logger.debug( null, "unpublish", LogMessage.UNPUBLISH_EXIT );
  }

  // When an endpoint is unpublished, it is part of a ServiceEndpoint, which is what is stored in the cache.
  // This will refresh the serviceURI to find the individual endpoints and update the cache.

  // This is a duplicate of the cache refresh
  public List<DME2Endpoint> refreshCachedEndpoint( String service ) throws DME2Exception {
    logger.debug( null, "refreshCachedEndpoint", LogMessage.METHOD_ENTER );
    long start = System.currentTimeMillis();

    List<DME2Endpoint> endpointList = new ArrayList<DME2Endpoint>();

    String serviceName = null;
    String serviceVersion = null;
    String envContext = null;
    String routeOffer = null;

    try {
      String uriStr = "http://DME2LOCAL/" + service;
      DmeUniformResource uri = new DmeUniformResource( config, new URI( uriStr ) );

      serviceName = uri.getService();
      serviceVersion = uri.getVersion();
      envContext = uri.getEnvContext();
      routeOffer = uri.getRouteOffer();
      String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );
      endpointList.addAll( fetchEndpoints( serviceName, serviceVersion, envContext, routeOffer, serviceURI ) );
    } catch ( Exception e ) {
      throw new DME2Exception( "AFT-DME2-0605", new ErrorContext()
          .add( "extendedMessage", e.getMessage() )
          .add( "manager", managerName )
          .add( "uri", service ), e );
    }

    // whatever we got back needs to be cached

    synchronized ( ldapLockObj ) {
      // If empty list was returned, still cache it to prevent DOS on GRM Server

      long ttl;
      long lastQueriedAt = 0;

      if ( endpointList.size() > 0 ) {
        ttl = 0;
        endpointCache
            .put( service, new DME2ServiceEndpointData( endpointList, service, 0, System.currentTimeMillis() ) );
      } else {
        // If we get here, then 0 endpoints returned from GRM.
        // DON'T put the new list in the cache - if we already had a list, we'll keep using it until we get new non-zero list back

        logger.warn( null, "refreshCachedEndpoint", LogMessage.REFRESH_DEFERRED, service );
        ttl = 0;

        if ( endpointCache.get( service ) != null ) {
          int emptyCacheRefreshAttemptCount = endpointCache.get( service ).getEmptyCacheRefreshAttemptCount();
          if ( emptyCacheRefreshAttemptCount == ( emptyCacheTTLRefreshIntervals.length - 1 ) ) {
           // If we get here, it means that we are already on the last refresh interval, so just stay at the same value

            ttl = emptyCacheTTLRefreshIntervals[emptyCacheTTLRefreshIntervals.length - 1];
            logger.debug( null, "refreshCachedEndpoint",
                "SEP Empty Cache TTL has already reached the last interval for service {}. TTL value will remain at: {}. Current empty cache refresh attempt count: {} ",
                service, ttl, emptyCacheRefreshAttemptCount );
          } else if ( emptyCacheTTLRefreshIntervals.length == 1 ) {
            //If only 1 interval is defined, keep using it until we get endpoints back.

            ttl = emptyCacheTTLRefreshIntervals[0];
          } else {
            //Increase the count and advance to the next interval in the array

            ++emptyCacheRefreshAttemptCount;
            ttl = emptyCacheTTLRefreshIntervals[emptyCacheRefreshAttemptCount];
            endpointCache.get( service ).setEmptyCacheRefreshAttemptCount( emptyCacheRefreshAttemptCount );

            logger.debug( null, "refreshCachedEndpoint", "New empty cache refresh attempt count: {}",
                emptyCacheRefreshAttemptCount );
            logger.debug( null, "refreshCachedEndpoint",
                "Advancing to next Emtpy Cache TTL interval value for service {}. New value: {}", service, ttl );
          }
        }
      }

      // put the ttl for next check
      if ( endpointCache.get( service ) != null ) {
        endpointCache.get( service ).setCacheTTL( ttl );
      }

      logger.debug( null, "refreshCachedEndpoint", LogMessage.CACHED_ENDPOINTS, service,
          ( System.currentTimeMillis() - start ), endpointList.size() );
    }
    logger.debug( null, "refreshCachedEndpoint", LogMessage.METHOD_EXIT );
    return endpointList;
  }

  @Override
  public List<DME2Endpoint> findEndpoints( String serviceName, String serviceVersion, String envContext,
                                           String routeOffer ) throws DME2Exception {
    long start = System.currentTimeMillis();
    assert ( initialized );

    String servicePath = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );
    List<DME2Endpoint> endpointList = null;
    if ( endpointCache.get(servicePath) != null ) {
      endpointList = endpointCache.getEndpoints( servicePath );
    }
		/*If endpoints where found in the cache, return them*/
    if ( endpointList != null && endpointList.size() > 0 ) {
      for ( DME2Endpoint endpoint : endpointList ) {
        endpoint.setCached( true );
      }
      return endpointList;
    }

    endpointList = fetchEndpoints( serviceName, serviceVersion, envContext, routeOffer, servicePath );

    if ( endpointList == null ) {
      return null;
    }

    // whatever we got back needs to be cached
 /*   synchronized ( ldapLockObj ) {
      *//*If empty list was returned, still cache it*//*
      long ttl;

      ttl = endpointList.isEmpty() ? sepCacheEmptyTtlMs : sepCacheTtlMs;

      endpointCache.put( servicePath,
          new DME2ServiceEndpointData( endpointList, servicePath, ttl, System.currentTimeMillis() ) );
      logger.debug( null, "findEndpoints", LogMessage.CACHED_ENDPOINTS, servicePath,
          ( System.currentTimeMillis() - start ),
          endpointList.size() );
    }
*/
    // This is confusing - why are we setting these as not cached if we just put them in the cache?

    for ( DME2Endpoint endpoint : endpointList ) {
      endpoint.setCached( false );
    }

    return endpointList;
  }

  @Override
  public DME2RouteInfo getRouteInfo( String serviceName, String serviceVersion, String envContext )
      throws DME2Exception {
    DME2RouteInfo data = null;

    String path = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext );

    if ( fetchRouteInfoFromGRM.containsKey( serviceName ) && !fetchRouteInfoFromGRM.get( serviceName ) ) {
      data = routeInfoCache.get( path );
    } else if ( !fetchRouteInfoFromGRM.containsKey( serviceName ) ) {
      data = routeInfoCache.get( path );
    }

    if ( data != null ) {
      return data;
    } else {
      try {
        data = fetchRouteInfo( serviceName, serviceVersion, envContext );
        fetchRouteInfoFromGRM.remove( serviceName );
      } catch ( DME2Exception e ) {
        fetchRouteInfoFromGRM.remove( serviceName );
        throw e;
      }

      synchronized ( this.routeInfoLockObj ) {
        routeInfoCache.put( path, data );
      }

      logger.debug( null, "getRouteInfo", LogMessage.CACHED_PATH, path );
      return data;
    }
  }

  @Override
  public void lease( DME2Endpoint endpoint ) throws DME2Exception {
    renewLease( endpoint );
    logger.debug( null, "lease", LogMessage.RENEW_LEASE, endpoint.getPath(), endpoint.getHost(), endpoint.getPort(),
        sEPLEASELENGTHMS );
  }

  @Override
  public void refresh() {
    assert ( initialized );

    synchronized ( ldapLockObj ) {
      endpointCache.clear();
    }

    try {
      endpointCache.refresh();
    } catch ( Exception e ) {
      logger.warn( null, "refresh", LogMessage.FORCE_REFRESH_FAILED );
    }

    renewAllLeases();
    logger.info( null, "refresh", "AFT-DME2-0630 {}", new ErrorContext().add( "RefreshSuccessful", "" ) );
  }

  private void renewLease( DME2Endpoint endpoint ) throws DME2Exception {
    assert ( initialized );  // We really want an assert?
    grm.updateServiceEndPoint( DME2EndpointUtil.convertToServiceEndpoint( config, endpoint ) );
    logger.debug( null, "renewLease", LogMessage.PUBLISH_LEASE, endpoint.getHost(), endpoint.getPort() );
  }

  @Override
  public void shutdown() {
    logger.debug( null, "shutdown", LogMessage.METHOD_ENTER );
    /* Cancelling cache timer tasks */
    endpointCache.shutdownTimerTask();
    routeInfoCache.shutdownTimerTask();

		/* Unpublish all endpoints */
    unpublishAll();
    logger.debug( null, "shutdown", LogMessage.METHOD_EXIT );
  }

  /**
   * unpublish all endpoints published from this instance
   */

  private void unpublishAll() {
    assert ( initialized );
    ArrayList<DME2Endpoint> localEpList = new ArrayList<DME2Endpoint>();
    localEpList.addAll( localPublishedList );

    for ( DME2Endpoint ep : localEpList ) {
      try {
        unpublish( ep );
      } catch ( Exception e ) {
        logger.warn( null, "unpublishAll", LogMessage.UNPUBLISH_IGNORABLE, e );
      }
    }
  }

  void loadJAXBContext() throws DME2Exception {
    try {
      jaxBContext = JAXBContext.newInstance( RouteInfo.class.getPackage().getName() );
    } catch ( JAXBException e ) {
      throw new DME2Exception( DME_EXCEPTION_INIT_EP_REGISTRY_GRM, new ErrorContext()
          .add( EXTENDED_STRING, e.getMessage() )
          //.add( MANAGER, ( this.getManager() != null ? this.getManager().getName() : "Not Initialized" ) )
          , e );
    }
  }

  @Override
  public void run() {
    shutdown();
  }

  protected void renewAllLeases() {
    assert ( initialized );

    logger.debug( null, "renewAllLeases", LogMessage.RENEW_ALL_START, localPublishedList.size() );

    long start = System.currentTimeMillis();

    // CSSA-11214 - randomize published list for lease operation
    Collections.shuffle( localPublishedList );

    // Avoid ConcurrentModificationException
    ArrayList<DME2Endpoint> localEpList = new ArrayList<DME2Endpoint>();
    localEpList.addAll( localPublishedList );
    for ( DME2Endpoint ep : localEpList ) {
      try {
        lease( ep );
        logger.debug( null, "renewAllLeases", LogMessage.RENEW_ENDPOINT, ep.toURLString() );
      } catch ( DME2Exception e ) {
        logger.warn( null, "renewAllLeases", LogMessage.RENEW_ENDPT_FAIL, ep.toURLString(), e );
      }
    }

    logger.debug( null, "renewAllLeases", LogMessage.RENEW_ALL_END, localPublishedList.size(),
        ( System.currentTimeMillis() - start ) );
  }

  protected List<DME2Endpoint> fetchEndpoints( String serviceName, String serviceVersion,
                                               String envContext, String routeOffer,
                                               String servicePath ) throws DME2Exception {
    long start = System.currentTimeMillis();

    ServiceEndpoint serviceEndpoint = new ServiceEndpoint();//DME2EndpointUtil.convertToServiceEndpoint( endpoint );
    serviceEndpoint.setName( serviceName );
    serviceEndpoint.setVersion( serviceVersion );
    serviceEndpoint.setEnv( envContext );
    //serviceEndpoint.setRouteOffer( routeOffer );
    // Call GRM to retrieve the endpoints*/
    List<ServiceEndpoint> serviceEndpointList = grm.findRunningServiceEndPoint( serviceEndpoint );
    logger.debug( null, "fetchEndpoints", "Got {} endpoints from GRM", serviceEndpointList.size() );
    String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );
    Iterator endpointIterator = serviceEndpointList.iterator();
    Map<String, List<DME2Endpoint>> tempCache = new HashMap<String, List<DME2Endpoint>>();

    List<DME2Endpoint> epList = new ArrayList<DME2Endpoint>();

    while ( endpointIterator.hasNext() ) {

      // Filter invalid endpoints

      ServiceEndpoint sep = (ServiceEndpoint) endpointIterator.next();
      String errorMsg = "";

      errorMsg += ( sep.getLatitude() == null ? "latitude " : "" );
      errorMsg += ( sep.getLongitude() == null ? "longitude " : "" );
      errorMsg += ( sep.getVersion() == null ? "version " : "" );
      errorMsg += ( sep.getRouteOffer() == null ? "route offer " : "" );
      errorMsg += ( sep.getPort() == null ? "listen port " : "" );
      errorMsg += ( sep.getHostAddress() == null ? "host address " : "" );
      if ( !errorMsg.isEmpty() ) {
        errorMsg = "Invalid endpoint returned from GRM. Missing " + errorMsg;
      }

      if ( !errorMsg.isEmpty() ) {
        logger.warn( null, "fetchEndpoints", LogMessage.DEBUG_MESSAGE, errorMsg );
        endpointIterator.remove();
        continue;
      }

      try {
        Double.parseDouble( sep.getLatitude() );
        Double.parseDouble( sep.getLongitude() );
        Double.parseDouble( sep.getPort() );
      } catch ( NumberFormatException e ) {
        logger.warn( null, "fetchEndpoints",
            "Invalid endpoint found for URI: {}. Detailed message: NumberFormatException -  {}.", sep.getName(),
            e.getMessage() );
        endpointIterator.remove();
        continue;
      }

      // Filter endpoints by RO?

      long expireTime =
          sep.getExpirationTime() != null ? sep.getExpirationTime().toGregorianCalendar(
              DME2Manager.getTimezone(), DME2Manager.getLocale(), null ).getTimeInMillis() :
              0;
      String service =
          DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, sep.getRouteOffer() );
      DME2Endpoint dme2Endpoint =
          DME2EndpointUtil.convertEndpoint( sep, service, expireTime, getClientLatitude(), getClientLongitude() );
      List<DME2Endpoint> endpointList;
      if ( tempCache.get( service ) != null ) {
        endpointList = tempCache.get( service );
      } else {
        endpointList = new ArrayList<DME2Endpoint>();
      }
      if ( iGNOREEXPIREDLEASESEPS || !isLeaseExpired( sep ) ) {
        endpointList.add( dme2Endpoint );
      }
      logger.debug( null, "fetchEndpoints", "Adding/updating tempcache key {} with endpoint list of size {}", service,
          endpointList.size() );
      tempCache.put( service, endpointList );
    }
    if ( tempCache.size() > 0 ) {
      Collection<List<DME2Endpoint>> values = tempCache.values();
      Iterator<List<DME2Endpoint>> iter = values.iterator();

      ArrayList<DME2Endpoint> allEpList = new ArrayList<DME2Endpoint>();

      while ( iter.hasNext() ) {
        ArrayList<DME2Endpoint> localList = (ArrayList<DME2Endpoint>) iter.next();
        allEpList.addAll( localList );
      }

      String defaultRouteURI = DME2URIUtils
          .buildServiceURIString( serviceName, serviceVersion, envContext,
              config.getProperty( DME2Constants.AFT_DME2_DEFAULT_RO ) );
      tempCache.put( defaultRouteURI, allEpList );

      // update cache with populated list and return a epList for queried routeOffer
      Iterator<String> it = tempCache.keySet().iterator();

      while ( it.hasNext() ) {
        //fservice is the service, including the route offer.
        String fservice = it.next();
        List<DME2Endpoint> flist = tempCache.get( fservice );

        // whatever we got back needs to be cached
        synchronized ( ldapLockObj ) {
          long ttl = 0;

          if ( flist.size() > 0 ) {
            // refresh list on standard interval
            ttl = sepCacheTtlMs;

						/*If Endpoints were returned, remove routeOffer from stale cache if present*/
            //if ( super.getManager().isRouteOfferStale( fservice ) ) {
            //  super.getManager().removeStaleRouteOffer( fservice );
            //}
            if ( isRouteOfferStale( fservice ) ) {
              removeStaleRouteOffer( fservice );
            }
          } else {
            // do NOT put the new list in the cache - if we already had a list, we'll keep using it until we get new non-zero list back
            logger.warn( null, "fetchEndpoints", LogMessage.REFRESH_DEFERRED, fservice );

            // but we'll check for new list more fre*quently
            ttl = sepCacheEmptyTtlMs;
          }

          DME2ServiceEndpointData endpointData = endpointCache.getEndpointData( fservice );
          if ( endpointData != null ) {
            endpointData.setEndpointList( flist );
            endpointData.setCacheTTL( ttl );
            endpointData.setLastQueried( System.currentTimeMillis() );
          } else {
            endpointData = new DME2ServiceEndpointData( flist, fservice, ttl, System.currentTimeMillis() );
          }
          endpointCache.put( fservice, endpointData );
          logger.debug( null, "fetchEndpoints", LogMessage.CACHED_ENDPOINTS_FETCH, fservice,
              ( System.currentTimeMillis() - start ), flist.size() );
        }
      }
    }

    if ( config.getBoolean( DME2Constants.AFT_DME2_ALLOW_EMPTY_SEP_GRM ) ) {
      String partialURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext );
      ArrayList<String> cachedEpList = new ArrayList<String>();
      for ( CacheElement.Key<String> cacheElement : endpointCache.getKeySet() ) {
        cachedEpList.add( cacheElement.getKey() );
      }

      for ( String endpointURI : cachedEpList ) {
        //the endpointURI can also have the concatenated route offer.  We dont want to mark those as stale.
        if ( endpointURI.startsWith( partialURI ) && !endpointURI.contains( DME2Constants.DME2_ROUTE_OFFER_SEP ) ) {
          //if its a concatenated route offer, we need to check the route offers for the individual tokens
          // This happens when we have multiple route offers with the same sequence
          if ( routeOffer != null && routeOffer.contains( DME2Constants.DME2_ROUTE_OFFER_SEP ) ) {
            String[] routeOffers = routeOffer.split( DME2Constants.DME2_ROUTE_OFFER_SEP );
            for ( int i = 0; i < routeOffers.length; i++ ) {
              String roService =
                  DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffers[i] );
              checkAndMarkStale( tempCache, roService );
            }
          } else {
            ///non concatenated route offer
            checkAndMarkStale( tempCache, endpointURI );
          }
        }
      }
    }
    return endpointCache.getEndpoints( serviceURI );
  }

  protected boolean isLeaseExpired( ServiceEndpoint endpoint ) {
    logger.debug( null, "isLeaseExpired", LogMessage.METHOD_ENTER );
    long expireTime = 0;
    if ( endpoint.getExpirationTime() != null ) {
      GregorianCalendar gc =
          endpoint.getExpirationTime().toGregorianCalendar( );
      logger.debug( null, "isLeaseExpired", "gc: {}", gc );
      expireTime = gc.getTimeInMillis();
    }
    logger.debug( null, "isLeaseExpired",
        "expirationTime: {} convertedExpirationTime: {} currentTime: {} diff: {} TZ: {} Locale: {}",
        endpoint.getExpirationTime(), expireTime, System.currentTimeMillis(), expireTime - System.currentTimeMillis(),
        DME2Manager.getTimezone(), DME2Manager.getLocale() );
    return ( expireTime < System.currentTimeMillis() );
  }

  private void checkAndMarkStale( Map<String, List<DME2Endpoint>> filteredEndpoints, String endpointURI ) {
    List<DME2Endpoint> clist = filteredEndpoints.get( endpointURI );

    if ( clist == null || ( clist != null && clist.size() == 0 ) ) {
      /* If there are any services that have empty Endpoint lists, mark the associated RouteOffer stale. Default time will be 15 minutes
			 * but this can be overriden by using AFT_DME2_ROUTEOFFER_STALENESS_IN_MIN. */
      //super.getManager().addStaleRouteOffer( endpointURI, null );
      addStaleRouteOffer( endpointURI, null );

      // GRM did not return any endpoints and cache needs to be refreshed in this case for the service
      synchronized ( ldapLockObj ) {
				/*Refresh list on fast cache interval. Put the new list in the cache*/
        DME2ServiceEndpointData endpointData = endpointCache.getEndpointData( endpointURI );
        if ( endpointData != null ) {
          endpointData.setEndpointList( new ArrayList<DME2Endpoint>() );
          endpointData.setCacheTTL( sepCacheEmptyTtlMs );
          endpointData.setLastQueried( System.currentTimeMillis() );
        } else {
          endpointData = new DME2ServiceEndpointData( new ArrayList<DME2Endpoint>(), endpointURI, sepCacheEmptyTtlMs,
              System.currentTimeMillis() );
        }
        endpointCache.put( endpointURI, endpointData );
      }
    }
  }

  protected DME2RouteInfo fetchRouteInfo( String service, String version, String env ) throws DME2Exception {
    assert ( initialized );  // Do we really need an assert here?

    logger.debug( null, "fetchRouteInfo", LogMessage.ENTER_CODEPOINT, "DME2EndpointRegistryGRM.getRouteInfo" );
    DME2RouteInfo data = null;

    ServiceEndpoint serviceEndpoint = new ServiceEndpoint();
    serviceEndpoint.setEnv( env );
    serviceEndpoint.setVersion( version );
    serviceEndpoint.setName( service );

    // No username / password?

    String xmlContent = grm.getRouteInfo( serviceEndpoint );

    logger.debug( null, "fetchRouteInfo", "xmlContent {}", xmlContent );
    if ( xmlContent != null ) {
      data = this.buildRouteInfo( service, version, env, xmlContent );
    }

    logger.debug( null, "fetchRouteInfo", LogMessage.EXIT_CODEPOINT, "DME2EndpointRegistryGRM.getRouteInfo" );

    return data;
  }

  private DME2RouteInfo buildRouteInfo( String service, String version, String envContext, String xmlContent )
      throws DME2Exception {
    logger.debug( null, "buildRouteInfo", LogMessage.ENTER_CODEPOINT, "DME2EndpointRegistryGRM.buildRouteInfo" );

    long start = System.currentTimeMillis();
    DME2RouteInfo data = null;

    try {
      Unmarshaller unmarshaller = jaxBContext.createUnmarshaller();
      JAXBElement<RouteInfo> element =
          unmarshaller.unmarshal( new StreamSource( new StringReader( xmlContent ) ), RouteInfo.class );

      RouteInfo rtInfo = element.getValue();

      if ( rtInfo.getServiceName() == null ) {
        rtInfo.setServiceName( service );
      }

      rtInfo.setServiceVersion( version );

      if ( rtInfo.getEnvContext() == null ) {
        rtInfo.setEnvContext( envContext );
      }

      data = new DME2RouteInfo( rtInfo, getConfig() );
    } catch ( JAXBException je ) {
      throw new DME2Exception( "AFT-DME2-0602", new ErrorContext()
          .add( EXTENDED_STRING, je.getMessage() )
              //.add( MANAGER, this.getManager().getName() )
          .add( DME2Constants.SERVICE, service ), je );
    } catch ( Exception e ) {
      throw new DME2Exception( "AFT-DME2-0604", new ErrorContext()
          .add( EXTENDED_STRING, e.getMessage() )
              //.add( MANAGER, this.getManager().getName() )
          .add( DME2Constants.SERVICE, service ), e );
    }

    logger.debug( null, "buildRouteInfo", LogMessage.EXIT_CODEPOINT_TIME, "DME2EndpointRegistryGRM.buildRouteInfo",
        ( System.currentTimeMillis() - start ) );
    logger.debug( null, "buildRouteInfo", "DME2EndpointRegistryGRM.buildRouteInfo {} ms",
        ( System.currentTimeMillis() - start ) );
    return data;
  }

  public boolean isGRMRetryException( DME2Exception e ) {
    String errorCode = e.getErrorCode();
    if ( errorCode != null ) {
      String failoverErrCodes = config.getProperty( DME2Constants.AFT_DME2_GRM_FAILOVER_ERROR_CODES );
      String codes[] = failoverErrCodes.split( "," );
      for ( String code : codes ) {
        if ( code.endsWith( "*" ) ) {
          if ( errorCode
              .contains( code.substring( 0, code.indexOf( "*" ) ) ) ) {
            return true;
          }
        } else {
          if ( errorCode.equalsIgnoreCase( code ) ) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public DME2Endpoint[] find( String serviceKey, String version, String env,
                              String routeOffer ) throws DME2Exception {
    List<DME2Endpoint> endpoints = findEndpoints( serviceKey, version, env, routeOffer );
    return endpoints.toArray( new DME2Endpoint[endpoints.size()] );
  }

  @Override
  public void registerJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {
    long start = System.currentTimeMillis();

    RegisterClientJVMInstanceRequest registerReq = new RegisterClientJVMInstanceRequest();
    registerReq.setEnv( envContext );
    registerReq.setAction( ClientJVMAction.REGISTER );
    registerReq.setClientJvmInstance( instanceInfo );

    String user = config.getProperty( DME2Constants.DME2_GRM_USER );
    String pass = config.getProperty( DME2Constants.DME2_GRM_PASS );
    SecurityContext ctx = SecurityContext.create( user, pass, config.getBoolean( "AFT_DME2_GRM_USE_SSL", false ) );

    BaseAccessor grmSA = GRMAccessorFactory.getGrmAccessorHandlerInstance( config, SecurityContext.create( config ) );
    grmSA.registerClientJVMInstance( registerReq );

    /*LogUtil.getINSTANCE().report( LOGGER, Level.FINE, LogMessage.JVM_REGISTER, "DME2EndpointRegistryGRM.registerJVM",
        ( System.currentTimeMillis() - start ) );*/
    logger.debug( null, "registerJVM", LogMessage.JVM_REGISTER, "DME2EndpointRegistryGRM.registerJVM",
        ( System.currentTimeMillis() - start ) );
  }

  @Override
  public void updateJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {
    long start = System.currentTimeMillis();

    RegisterClientJVMInstanceRequest registerReq = new RegisterClientJVMInstanceRequest();
    registerReq.setEnv( envContext );
    registerReq.setAction( ClientJVMAction.REFRESH );
    registerReq.setClientJvmInstance( instanceInfo );

    String user = config.getProperty( DME2Constants.DME2_GRM_USER );
    String pass = config.getProperty( DME2Constants.DME2_GRM_PASS );
    SecurityContext ctx = SecurityContext.create( user, pass, config.getBoolean( "AFT_DME2_GRM_USE_SSL", false ) );

    BaseAccessor grmSA = GRMAccessorFactory.getGrmAccessorHandlerInstance( config, SecurityContext.create( config ) );
    grmSA.registerClientJVMInstance( registerReq );

    logger.debug( null, "updateJVM", LogMessage.JVM_REGISTER, "DME2EndpointRegistryGRM.updateJVM",
        ( System.currentTimeMillis() - start ) );
  }

  @Override
  public void deregisterJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {
    long start = System.currentTimeMillis();

    RegisterClientJVMInstanceRequest registerReq = new RegisterClientJVMInstanceRequest();
    registerReq.setEnv( envContext );
    registerReq.setAction( ClientJVMAction.DEREGISTER );
    registerReq.setClientJvmInstance( instanceInfo );

    String user = config.getProperty( DME2Constants.DME2_GRM_USER );
    String pass = config.getProperty( DME2Constants.DME2_GRM_PASS );
    SecurityContext ctx = SecurityContext.create( user, pass, config.getBoolean( "AFT_DME2_GRM_USE_SSL", false ) );

    BaseAccessor grmSA = GRMAccessorFactory.getGrmAccessorHandlerInstance( config, SecurityContext.create( config ) );
    grmSA.registerClientJVMInstance( registerReq );

    logger.debug( null, "deregisterJVM", LogMessage.JVM_REGISTER, "DME2EndpointRegistryGRM.deregisterJVM",
        ( System.currentTimeMillis() - start ) );
  }

  @Override
  public List<ClientJVMInstance> findRegisteredJVM( String envContext, Boolean activeOnly, String hostAddress,
                                                    String mechID, String processID ) throws DME2Exception {
    logger.debug( null, "findRegisteredJVM", LogMessage.METHOD_ENTER );
    long start = System.currentTimeMillis();

    List<ClientJVMInstance> jvmlist = new ArrayList<ClientJVMInstance>();

    try {
      FindClientJVMInstanceRequest findReq = new FindClientJVMInstanceRequest();
      findReq.setEnv( envContext );
      findReq.setActiveOnly( activeOnly );
      findReq.setHostAddress( hostAddress );
      findReq.setMechId( mechID );
      findReq.setProcessId( processID );

      String user = config.getProperty( DME2Constants.DME2_GRM_USER );
      String pass = config.getProperty( DME2Constants.DME2_GRM_PASS );
      SecurityContext ctx = SecurityContext.create( user, pass, config.getBoolean( "AFT_DME2_GRM_USE_SSL", false ) );

      BaseAccessor grmSA = GRMAccessorFactory.getGrmAccessorHandlerInstance( config, SecurityContext.create( config ) );
      jvmlist = grmSA.findClientJVMInstance( findReq );
    } finally {
      logger.debug( null, "findRegisteredJVM", LogMessage.JVM_FIND, "DME2EndpointRegistryGRM.deregisterJVM", jvmlist.size(),
              ( System.currentTimeMillis() - start ) );
    }
    logger.debug( null, "findRegisteredJVM", LogMessage.METHOD_EXIT );
    return jvmlist;
  }
  
  public boolean containsServiceEndpoint(String serviceName) {
	  return (endpointCache.getEndpointData(serviceName)==null) ? false: true;	  
  }
  
  public long getEndpointTTL(String serviceName) {
	  DME2ServiceEndpointData data = endpointCache.getEndpointData(serviceName);
	  if (data != null)
		  return data.getCacheTTL();
	  
	  return 0;
  }
  
}
