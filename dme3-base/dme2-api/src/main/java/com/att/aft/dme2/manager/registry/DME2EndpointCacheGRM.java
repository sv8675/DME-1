package com.att.aft.dme2.manager.registry;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.DME2CacheStats;
import com.att.aft.dme2.cache.DME2CacheStatsHolder;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.ErrorContext;

/**
 * DME2 Endpoint Cache for GRM
 */
public class DME2EndpointCacheGRM extends DME2AbstractRegistryCache<String, DME2ServiceEndpointData> {
  private static final Logger logger = LoggerFactory.getLogger( DME2EndpointCacheGRM.class );
  private final byte[] cacheLockMonitor = new byte[0];
  private transient DME2EndpointRegistryGRM registry;
  private final Map<String, DME2CacheStatsHolder> cacheStats = Collections.synchronizedMap(new HashMap<String, DME2CacheStatsHolder>());
  private boolean enableCacheStats = true;
  private long endpointLeaseRenewFrequency;
  private long sepCacheAllStaleTtlMs;
  private Timer renewLeaseTimer;
  private byte[] lock = new byte[0];
  private long endpointLastQueriedInterval;
  private long infrequentEndpointCacheTTL;
  private long endpointCacheTTL;
  private long endpointCacheEmptyTTL;
  private long[] emptyCacheTTLRefreshIntervals;
  private long[] emptyCacheTTLRefreshDefaultIntervals = new long[]{ 300000, 300000, 300000, 600000, 900000 };
  private DME2Configuration config;

  /**
   * Basic constructor
   *
   * @param registry Endpoint Registry
   * @throws DME2Exception
   */
  public DME2EndpointCacheGRM( DME2Configuration config, DME2EndpointRegistryGRM registry, String managerName,
                               boolean isStale ) throws DME2Exception {
    super( config, DME2Endpoint.class, DME2EndpointRegistryType.GRM, registry, managerName, isStale );
    this.config = config;
    this.registry = registry;
    initialize();
  }

  /**
   * Initialize endpoint cache GRM
   */
  public void initialize() {
    endpointLeaseRenewFrequency = registry.getConfig()
        .getInt( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" ); // Default, Integer.toString( endpointLeaseLength / 2 ) ) );
    sepCacheAllStaleTtlMs =
        registry.getConfig().getLong( "DME2_SEP_CACHE_ALL_STALE_TTL_MS" ); // default, "300000" ) );
    endpointLastQueriedInterval =
        registry.getConfig().getInt( "DME2_SERVICE_LAST_QUERIED_INTERVAL_MS" ); //Default, 900000);
    infrequentEndpointCacheTTL =
        registry.getConfig().getInt( "DME2_SEP_CACHE_INFREQUENT_TTL_MS" ); // Default, 900000 * 2);
    endpointCacheTTL = registry.getConfig().getInt( "DME2_SEP_CACHE_TTL_MS" ); // Default, 300000);
    endpointCacheEmptyTTL = registry.getConfig().getInt( "DME2_SEP_CACHE_EMPTY_TTL_MS" ); // Default, 300000);
    emptyCacheTTLRefreshIntervals = getEmptyCacheTTLIntervalsFromProperties(
        registry.getConfig().getProperty( "DME2_SEP_EMPTY_CACHE_TTL_INTERVALS" ),
        emptyCacheTTLRefreshDefaultIntervals );

    
    
		/* Initiate timer for extending lease of published endpoints */

    renewLeaseTimer = new Timer( "DME2::DME2EndpointRegistryGRM::epExpireTimer", true );
    renewLeaseTimer.scheduleAtFixedRate( new TimerTask() {

      @Override
      public void run() {
        try {
          registry.renewAllLeases();
        } catch ( Throwable e ) {
          logger.warn( null, "run", LogMessage.ERROR_RENEWING_ALL, e );
        }
      }

    }, endpointLeaseRenewFrequency, endpointLeaseRenewFrequency );
  }

  /**
   * Convenience method to retrieve endpoints from the cache based upon the service name
   *
   * @param serviceName Service Name
   * @return list of DME2Endpoints associated with the service name
   */

  public List<DME2Endpoint> getEndpoints( String serviceName ) {
    List<DME2Endpoint> endpoints = new ArrayList<DME2Endpoint>();
    if ( !serviceName.startsWith( "/" ) ) {
      serviceName = "/" + serviceName;
    }
    logger.debug( null, "getEndpoints", "Retrieving endpoints for serviceName {}", serviceName );
    try {
      endpoints = getEndpoints( new DmeUniformResource( config, new URI( "http://DME2LOCAL" + serviceName ) ) );
    } catch ( MalformedURLException e ) {
      logger.debug( null, "get", LogMessage.DEBUG_MESSAGE, "URISyntaxException", e );
    } catch ( URISyntaxException e ) {
      logger.debug( null, "get", LogMessage.DEBUG_MESSAGE, "MalformedURLException", e );
    }
    return endpoints;
  }


  /**
   * Convenience method to retrieve endpoint data from the cache based upon the service name
   *
   * @param serviceName Service Name
   * @return DME2 service endpoint data
   */
  public DME2ServiceEndpointData getEndpointData( String serviceName ) {
    CacheElement.Value<DME2ServiceEndpointData> cacheValue = cache.get( createCacheKey( serviceName ) );
    return cacheValue != null ? cacheValue.getValue() : null;
  }


  /**
   * Convenience method to retrieve endpoints from the cache based upon the uniform resource
   *
   * @param uniformResource Uniform resource
   * @return list of dme2 endpoints
   */
  public List<DME2Endpoint> getEndpoints( DmeUniformResource uniformResource ) {
    logger.debug( null, "getEndpoint", LogMessage.METHOD_ENTER );
    String routeOffer = uniformResource.getRouteOffer();
    List<DME2Endpoint> endpoints = new ArrayList<DME2Endpoint>();
    DME2ServiceEndpointData endpointData = getSingleServiceEndpointData( uniformResource.getPath() );
    if ( routeOffer != null && routeOffer.contains( DME2Constants.DME2_ROUTE_OFFER_SEP ) ) {
      String[] routeOffers = routeOffer.split( DME2Constants.DME2_ROUTE_OFFER_SEP );
      String serviceName = uniformResource.getService();
      String serviceVersion = uniformResource.getVersion();
      String envContext = uniformResource.getEnvContext();
      for ( String subRouteOffer : routeOffers ) {
        String thisServiceName =
            DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, subRouteOffer );
        List<DME2Endpoint> thisEndpointList = getSingleServiceEndpoints( thisServiceName );
        if ( thisEndpointList != null ) {
          endpoints.addAll( thisEndpointList );
        }
      }
    } else if ( endpointData != null ) {
      endpoints = endpointData.getEndpointList();
    }

    if ( endpoints == null || endpoints.isEmpty() ) {
      //registry.getManager().addStaleRouteOffer( uniformResource.getService(), null );
      //registry.addStaleRouteOffer( uniformResource.getService(), null );
      registry.addStaleRouteOffer( uniformResource.getPath(), null );
      endpoints = new ArrayList<DME2Endpoint>();
    }

    /**
     * Refresh logic for stale endpoints should always adhere to the below refresh logic table
     * For service with endpoints list count as 1 and if that one endpoint is marked stale, the cache ttl
     * for the service should be set to 60 secs in future. The refresh timer is set to run at SEP_REFRESH_CACHE_TIMER_FREQ_MS (20 secs) interval
     * and setting value lower than 60 secs might result in frequent refreshes, while the endpoints might be stale in a valid situation like resource issue
     *
     * For service with endpoint list count > 1 and endpoint stale list > 0, the cache ttl
     * for the service should follow the regular 5 mins in future refresh cycle
     *
     * 	epList in routeOffer	Stale ep count	Add fast cache	Refresh time
     * 	1						1				Y				60 secs
     *  >1						>0				N				5 mins
     */


    /**
     * CSSA-11398 - DME2 should refresh services with one endpoint list at a faster rate ( 60 secs ) and the logic in getCachedEndpoint needs to fixed for this.
     * Right now for any stale endpoint in list, grm call is happening on client invocation stack
     */

    if ( endpoints.size() == config.getInt( DME2Constants.AFT_DME2_FAST_CACHE_EP_ELIGIBLE_COUNT ) ) {
      /*If there are any stales in this list, make sure the service is in the fast refresh list with 60 secs cache expiry TTL*/
      int staleEndpointCount = countStaleEndpoints( uniformResource.getPath(), endpoints );

      if ( staleEndpointCount > config.getInt( DME2Constants.AFT_DME2_FAST_CACHE_STALE_EP_ELIGIBLE_COUNT ) ) {
        // check if the serviceURI is in the staleCacheList to be updated more frequently
        synchronized ( cacheLockMonitor ) {
          // if currently cached item is > stale endpoint frequency or there is no ttl, set ttl to the stale frequency
          Long ttl = null;

          if ( endpointData != null ) {
            ttl = endpointData.getExpirationTime();
          }

          if ( ttl == null || ttl > System.currentTimeMillis() + this.sepCacheAllStaleTtlMs ) {
            endpointData.setCacheTTL( this.sepCacheAllStaleTtlMs );
            put( uniformResource.getPath(), endpointData );
            logger.debug( null, "getEndpoint",
                "Code=DMEEndpointRegistryGRM.getCachedEndpoints; Adding service {} to list for allep stale refresh thread ;",
                uniformResource.getPath() );
          }
        }
      }
    }
    logger.debug( null, "getEndpoint", LogMessage.METHOD_EXIT );
    return endpoints;
  }

  protected int countStaleEndpoints( String serviceURI, List<DME2Endpoint> epList ) {
    List<DME2Endpoint> staleEndpointsList = new ArrayList<DME2Endpoint>();

    for ( DME2Endpoint endpoint : epList ) {
      //if ( endpoint != null && registry.getManager().isUrlInStaleList( endpoint.toURLString() ) ) {
      if ( endpoint != null && registry.isEndpointStale( endpoint.toURLString() ) ) {
        staleEndpointsList.add( endpoint );
      }
    }

    if ( staleEndpointsList.size() > 0 ) {
      logger.debug( null, "countStaleEndpoints", LogMessage.CACHED_STALE, serviceURI, staleEndpointsList );
    }

    return staleEndpointsList.size();
  }

  protected List<DME2Endpoint> getSingleServiceEndpoints( String serviceName ) {
    CacheElement.Value<DME2ServiceEndpointData> cacheElement = cache.get( createCacheKey( serviceName ) );
    if ( cacheElement != null ) {
      if ( cacheElement.getValue() != null ) {
        return cacheElement.getValue().getEndpointList();
      }
    }
    return null;
  }

  protected DME2ServiceEndpointData getSingleServiceEndpointData( String serviceName ) {
    CacheElement.Value<DME2ServiceEndpointData> cacheElement = cache.get( createCacheKey( serviceName ) );
    if ( cacheElement != null ) {
      if ( cacheElement.getValue() != null ) {
        return cacheElement.getValue();
      }
    }
    return null;
  }

  /**
   * Shutdown tasks
   */
  public void shutdownTimerTask() {
    logger.debug( null, "shutdownTimerTask", LogMessage.METHOD_ENTER );
    if ( renewLeaseTimer != null ) {
      renewLeaseTimer.cancel();
    }
    cache.shutdownTimerTask();
    logger.debug( null, "shutdownTimerTask", LogMessage.METHOD_EXIT );
  }

  @Override
  public String toString() {
    return ( (AbstractCache) cache ).getKeys();
  }

  @Override
  public CacheElement fetchFromSource( CacheElement.Key requestValue )
      throws DME2Exception {
    DME2ServiceEndpointData endpointData = null;
    if ( requestValue != null ) {
      String service = (String) requestValue.getKey();
      Map<String, String> serviceMap = DME2URIUtils.splitServiceURIString( service );
      List<DME2Endpoint> endpoints = registry.fetchEndpoints( serviceMap.get( DME2Constants.SERVICE_PATH_KEY_SERVICE ),
          serviceMap.get( DME2Constants.SERVICE_PATH_KEY_VERSION ),
          serviceMap.get( DME2Constants.SERVICE_PATH_KEY_ENV_CONTEXT ),
          serviceMap.get( DME2Constants.SERVICE_PATH_KEY_ROUTE_OFFER ), service );

      endpointData =
          new DME2ServiceEndpointData( endpoints, service, endpointCacheTTL, 0 );
    }
    return createCacheElement( requestValue, createCacheValue( endpointData ) );
  }

  public void refresh() {
    refreshAllCachedEndpoints();
  }

  private void refreshAllCachedEndpoints() {
    List<String> serviceURIList = new ArrayList<String>();

    if ( cache == null ) {
      // For whatever reason, this may be called upon startup, when cache is not yet initialized.
      // Not sure if this is the appropriate response, but it's better than an NPE
      return;
    }
    for ( CacheElement.Key key : cache.getKeySet() ) {
      if ( key != null ) {
        serviceURIList.add( (String) key.getKey() );
      }
    }

    // CSSA-11214 - randomize the ep list being refreshed everytime.
    Collections.shuffle( serviceURIList );

    logger.debug( null, "refreshAllCachedEndpoints",
        "Code=Trace.DME2EndpointCache.refreshCachedDME2Endpoints; Refreshing cached endpoints" );
    logger.debug( null, "refreshAllCachedEndpoints", LogMessage.REFRESH_ENDPOINTS, new Date() );

    for ( String serviceURI : serviceURIList ) {
      try {
        Long expiration = 0L;

        Map<String, String> map = DME2Utils.splitServiceURIString( serviceURI );
				if(map != null && map.containsKey("routeOffer")) {
					String routeOfferVal = map.get("routeOffer");
					// If the cached entry is of type grouped sequence routeOffer
					// E.g service=com.att.aft.DME2Echo/version=1.0/envContext=DEV/routeOffer=D1~D2
					// then do not trigger refresh
					// This logic needs to be revisited, if in future, DME2 sends findRunningEndpoints by
					// single routeOffer. Currently DME2 doesn't query GRM API by individual routeOffer,
					// but GRM supports it though
					if(routeOfferVal != null && config.getProperty( DME2Constants.DME2_ROUTE_OFFER_SEP ) != null && routeOfferVal.contains( config.getProperty( DME2Constants.DME2_ROUTE_OFFER_SEP ))) {
						logger.debug( null, "refreshAllCachedEndpoints", LogMessage.SKIP_REFRESH_ENDPOINTS, serviceURI);
						continue;
					}

				}
        DME2ServiceEndpointData cachedEndpoint = get( serviceURI );
        if ( cachedEndpoint != null ) {
          expiration = cachedEndpoint.getExpirationTime();
        }

        if ( expiration <= System.currentTimeMillis() ) {
          logger.debug( null, "refreshAllCachedEndpoints", LogMessage.UPDATE_ENDPOINTS, serviceURI );
          logger.debug( null, "refreshAllCachedEndpoints",
              "Code=Trace.DME2EndpointCache.refreshCachedDME2Endpoints; Updating endpoints for {}", serviceURI );

          refreshCachedEndpoint( serviceURI );

          logger.debug( null, "refreshAllCachedEndpoints", LogMessage.REFRESH_LOOKUP, serviceURI );
          logger.debug( null, "refreshAllCachedEndpoints", "Refreshed cached endpoints for lookup [{}]", serviceURI );
        }
      } catch ( DME2Exception e ) {
        logger.debug( null, "refreshAllCachedEndpoints", LogMessage.REFRESH_FAILED, serviceURI, e );
        logger.debug( null, "refreshAllCachedEndpoints", "Refresh of cached endpoints for [{}] failed: {}", serviceURI,
            e );
      }
    }
    //registry.setEndpointCacheUpdatedAt(System.currentTimeMillis());
  }

  public List<DME2Endpoint> refreshCachedEndpoint( String service ) throws DME2Exception {
    long start = System.currentTimeMillis();

    List<DME2Endpoint> endpointList = new ArrayList<DME2Endpoint>();

    String serviceName = null;
    String serviceVersion = null;
    String envContext = null;
    String routeOffer = null;

    String cacheStatsURI = null;
    DME2CacheStatsHolder statsHolder = null;
    long startTime = 0L;

    try {
      String uriStr = "http://DME2LOCAL/" + service;
      DmeUniformResource uri = new DmeUniformResource( config, new URI( uriStr ) );

      serviceName = uri.getService();
      serviceVersion = uri.getVersion();
      envContext = uri.getEnvContext();
      routeOffer = uri.getRouteOffer();
      cacheStatsURI = "/service=" +serviceName + "/version=" + serviceVersion + "/envContext="+envContext;
			statsHolder = this.cacheStats.get(cacheStatsURI);
			if(statsHolder == null) {

				statsHolder = new DME2CacheStatsHolder(cacheStatsURI,config);
				this.cacheStats.put(cacheStatsURI,statsHolder);
			}
      String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );
      startTime = System.currentTimeMillis();
      endpointList.addAll( registry.fetchEndpoints( serviceName, serviceVersion, envContext, routeOffer, serviceURI ) );
      statsHolder.recordRefreshSuccess(System.currentTimeMillis()-startTime, this.isCacheStatsEnabled());
    } catch ( Exception e ) {
      statsHolder.recordRefreshFailure(System.currentTimeMillis()-startTime, this.isCacheStatsEnabled());
      throw new DME2Exception( "AFT-DME2-0605", new ErrorContext()
          .add( "extendedMessage", e.getMessage() )
//          .add( "manager", registry.getManager().getName() )
          .add( "uri", service ), e );
    }

    synchronized ( lock ) {
      /*If empty list was returned, still cache it to prevent DOS on GRM Server*/
      long ttl;
      long lastQueriedAt = 0;

      if ( endpointList.size() > 0 ) {
        logger.debug( null, "refreshCachedEndpoint", "Endpoint list has 0 elements for {}", service );
        DME2ServiceEndpointData cachedEndpoint = get( service );
        if ( cachedEndpoint != null ) {
          lastQueriedAt = cachedEndpoint.getLastQueried();
        }

        if ( lastQueriedAt > 0 && config.getBoolean( DME2Constants.AFT_DME2_ENABLE_SELECTIVE_REFRESH ) ) {
          if ( ( System.currentTimeMillis() - lastQueriedAt ) >= this.endpointLastQueriedInterval ) {
            ttl = this.infrequentEndpointCacheTTL;
          } else {
						/*refresh list on standard interval*/
            ttl = endpointCacheTTL;
          }
        } else {
					/*refresh list on standard interval*/
          ttl = endpointCacheTTL;
        }
        put( service, new DME2ServiceEndpointData( endpointList, service, ttl, System.currentTimeMillis() ) );
      } else {
				/*If we get here, then 0 endpoints returned from GRM.*/

				/*DON'T put the new list in the cache - if we already had a list, we'll keep using it until we get new non-zero list back*/
        logger.warn( null, "refreshCachedEndpoint", LogMessage.REFRESH_DEFERRED, "refreshCachedEndpoint", service );
        ttl = endpointCacheEmptyTTL;

        DME2ServiceEndpointData cachedEndpoint = get( service );
        if ( cachedEndpoint != null ) {
          int emptyCacheRefreshAttemptCount = cachedEndpoint.getEmptyCacheRefreshAttemptCount();
          if ( emptyCacheRefreshAttemptCount == ( emptyCacheTTLRefreshIntervals.length - 1 ) ) {
						/*If we get here, it means that we are already on the last refresh interval, so just stay at the same value*/
            ttl = emptyCacheTTLRefreshIntervals[emptyCacheTTLRefreshIntervals.length - 1];
            String msg = String.format(
                "SEP Empty Cache TTL has already reached the last interval for service %s. TTL value will remain at: %s. Current empty cache refresh attempt count: %s ",
                service, ttl, emptyCacheRefreshAttemptCount );
            logger.debug( null, "refreshCachedEndpoint", LogMessage.DEBUG_MESSAGE, msg );
          } else if ( emptyCacheTTLRefreshIntervals.length == 1 ) {
						/*If only 1 interval is defined, keep using it until we get endpoints back.*/
            ttl = emptyCacheTTLRefreshIntervals[0];
          } else {
						/*Increase the count and advance to the next interval in the array*/
            ++emptyCacheRefreshAttemptCount;
            ttl = emptyCacheTTLRefreshIntervals[emptyCacheRefreshAttemptCount];
            get( service ).setEmptyCacheRefreshAttemptCount( emptyCacheRefreshAttemptCount );

            String msg = String
                .format( "Advancing to next Emtpy Cache TTL interval value for service %s. New value: %s", service,
                    ttl );
            logger.debug( null, "refreshCachedEndpoint", "New empty cache refresh attempt count: {}", emptyCacheRefreshAttemptCount );
            logger.debug( null, "refreshCachedEndpoint", LogMessage.DEBUG_MESSAGE, msg );
          }
        }
      }

      CacheElement cachedEndpointElement = getCacheElement( service );
      // put the ttl for next check - we need to change BOTH the endpoint and the cached element!
      if ( cachedEndpointElement != null && cachedEndpointElement.getValue() != null ) {
        CacheElement.Value wrappedValue = cachedEndpointElement.getValue();
        if ( wrappedValue.getValue() != null && wrappedValue.getValue() instanceof DME2ServiceEndpointData ) {

          DME2ServiceEndpointData data = (DME2ServiceEndpointData) wrappedValue.getValue();
          logger.debug( null, "refreshCachedEndpoint", "Setting {} endpoint ttl to {}", data.getServiceURI(), ttl );
          data.setCacheTTL( ttl );
          // Now update the cacheELement ttl/expiration time!  The EndpointData's "setCacheTTL" method automatically
          // takes care of its expiration time as well
          cachedEndpointElement.setTtl( data.getCacheTTL() );
        }
      }

      logger.debug( null, "refreshCachedEndpoint", LogMessage.CACHED_ENDPOINTS, service,
          ( System.currentTimeMillis() - start ), endpointList.size() );
    }

    return endpointList;
  }

  private long[] getEmptyCacheTTLIntervalsFromProperties( String property, long[] defaultValue ) {
    long[] emptyCacheTTLIntervals = null;
    if ( property == null ) {
      return defaultValue;
    }

    try {
      String[] tokens = property.split( "," );

			/*Check if each value to make sure it is a valid long type*/
      for ( String token : tokens ) {
        if ( !DME2URIUtils.isParseable( token.trim(), Long.class ) ) {
          return defaultValue;
        }
        boolean enforceMinEmptyCacheTTLIntervalValue = registry.getConfig().getBoolean(
            "DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE" ); // Default, true);

        if ( enforceMinEmptyCacheTTLIntervalValue ) {
          if ( Long.parseLong( token ) < 300000 ) {
            logger.warn( null, "getEmptyCacheTTLIntervalsFromProperties",
                "Interval values cannot be less than 5 minutes. Value provided: {}. Using default interval values of: {}",
                token, Arrays
                    .asList( emptyCacheTTLRefreshDefaultIntervals )
            );
            return defaultValue; /* If any interval value is less than 5 minutes, use the defaults */
          }
        }
      }

      emptyCacheTTLIntervals = new long[tokens.length];
      for ( int i = 0; i < tokens.length; i++ ) {
        emptyCacheTTLIntervals[i] = Long.parseLong( tokens[i].trim() );
      }
    } catch ( Exception e ) {
      logger.debug( null, "getEmptyCacheTTLIntervalsFromProperties",
          "Error occurred while attempting while resolving Empty SEP Cache TTL Intervals. Using default.", e );
      return defaultValue;
    }

    logger.debug( null, "getEmptyCacheTTLIntervalsFromProperties",
        "Empty SEP Cache TTL Intervals resolved from properties: {}", property );
    return emptyCacheTTLIntervals;
  }

  @Override
	public DME2CacheStats getStats(String serviceName, Integer hourOfDay) {

		if(serviceName != null && this.cacheStats.get(serviceName) != null) {
			DME2CacheStatsHolder stats = this.cacheStats.get(serviceName);
			if(hourOfDay >=0 && hourOfDay <=23) {
				return stats.getHourlyStats(hourOfDay);
			}
			return this.cacheStats.get(serviceName).getStats();
		}
		return null;
	}


	@Override
	public void disableCacheStats() {
		enableCacheStats=false;
	}


	@Override
	public void enableCacheStats() {		
		enableCacheStats = true;
	}


	@Override
	public boolean isCacheStatsEnabled() {
		return enableCacheStats;
	}

}