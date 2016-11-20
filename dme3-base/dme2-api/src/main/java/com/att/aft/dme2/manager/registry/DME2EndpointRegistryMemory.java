package com.att.aft.dme2.manager.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.CollectionUtils;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2DistanceUtil;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.ErrorContext;
import com.att.scld.grm.types.v1.ClientJVMInstance;

public class DME2EndpointRegistryMemory extends DME2AbstractEndpointRegistry {
//    private DME2Manager manager;

  //private final List<DME2Endpoint> endpoints = new ArrayList<DME2Endpoint>();
  private static final Logger LOGGER = LoggerFactory.getLogger( DME2EndpointRegistryMemory.class );
  private final List<DME2Endpoint> endpoints = new ArrayList<DME2Endpoint>();
  private double latitude, longitude;

  /**
   * Base constructor
   *
   * @param config      Configuration
   * @param managerName Manager Name
   * @throws com.att.aft.dme2.api.DME2Exception
   */
  public DME2EndpointRegistryMemory( DME2Configuration config, String managerName )
      throws DME2Exception {
    super( config, managerName );
    LOGGER.debug( null, "DME2EndpointRegistryMemory", LogMessage.METHOD_ENTER );
    staleEndpointCache =
        new DME2StaleCache( config, DME2Endpoint.class, DME2EndpointRegistryType.Memory, this, managerName );
    staleRouteOfferCache =
        new DME2StaleCache( config, DME2RouteOffer.class, DME2EndpointRegistryType.Memory, this, managerName );
    LOGGER.debug( null, "DME2EndpointRegistryMemory", LogMessage.METHOD_EXIT );
  }

  @Override
  //public DME2Endpoint[] find(String serviceName, String serviceVersion, String envContext, String routeOffer)
  //    throws DME2Exception
  // {
  public List<DME2Endpoint> findEndpoints( String serviceName, String serviceVersion, String envContext,
                                           String routeOffer ) {
    LOGGER.debug( null, "findEndpoints", LogMessage.METHOD_ENTER );
    LOGGER.info( null, "findEndpoints", "serviceName={} serviceVersion={} envContext={} routeOffer={}", serviceName,
        serviceVersion, envContext, routeOffer );
    final List<DME2Endpoint> matching = new ArrayList<DME2Endpoint>();
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getServiceName().equals( serviceName )
          && versionMatches( ep, serviceVersion )
          && ep.getEnvContext().equals( envContext )
          && ep.getRouteOffer().equals( routeOffer ) ) {
        matching.add( ep );
      }

    }
    //return matching.toArray(new DME2Endpoint[matching.size()]);
    LOGGER.debug( null, "findEndpoints", LogMessage.METHOD_EXIT );
    return matching;
  }


  private boolean versionMatches( DME2Endpoint ep, String requestedVersion ) {
    if ( requestedVersion == null ) {
      return true;
    }

    final String range = ep.getSupportedVersionRange();
    if ( range == null ) {
      final String serviceVersion = ep.getServiceVersion();
      return serviceVersion.equals( requestedVersion ) || serviceVersion.startsWith( requestedVersion + "." );
    }

    final String[] rv = requestedVersion.split( "\\." );
    final String[] startEnd = range.split( "," );
    final String[] start = startEnd[0].split( "\\." );
    switch ( Math.min( rv.length, start.length ) ) {
      case 1:
        if ( Integer.parseInt( rv[0] ) < Integer.parseInt( start[0] ) ) {
          return false;
        }
        break;
      case 2:
        if ( Integer.parseInt( rv[0] ) < Integer.parseInt( start[0] ) ) {
          return false;
        }
        if ( Integer.parseInt( rv[0] ) == Integer.parseInt( start[0] )
            && Integer.parseInt( rv[1] ) < Integer.parseInt( start[1] ) ) {
          return false;
        }
        break;
      case 3:
        if ( Integer.parseInt( rv[0] ) < Integer.parseInt( start[0] ) ) {
          return false;
        }
        if ( Integer.parseInt( rv[0] ) == Integer.parseInt( start[0] )
            && Integer.parseInt( rv[1] ) < Integer.parseInt( start[1] ) ) {
          return false;
        }
        if ( Integer.parseInt( rv[0] ) == Integer.parseInt( start[0] )
            && Integer.parseInt( rv[1] ) == Integer.parseInt( start[1] )
            && rv[1].compareTo( start[1] ) < 0 ) {
          return false;
        }
        break;
      default:
        throw new IllegalArgumentException();
    }

    if ( startEnd[1].equals( "*" ) ) {
      return true;
    }
    final String[] end = startEnd[1].split( "\\." );
    switch ( end.length ) {
      case 1:
        if ( Integer.parseInt( rv[0] ) > Integer.parseInt( end[0] ) ) {
          return false;
        }
        break;
      case 2:
        if ( Integer.parseInt( rv[0] ) > Integer.parseInt( end[0] ) ) {
          return false;
        }
        if ( Integer.parseInt( rv[0] ) == Integer.parseInt( end[0] )
            && Integer.parseInt( rv[1] ) > Integer.parseInt( end[1] ) ) {
          return false;
        }
        break;
      case 3:
        if ( Integer.parseInt( rv[0] ) > Integer.parseInt( end[0] ) ) {
          return false;
        }
        if ( Integer.parseInt( rv[0] ) == Integer.parseInt( end[0] )
            && Integer.parseInt( rv[1] ) > Integer.parseInt( end[1] ) ) {
          return false;
        }
        if ( Integer.parseInt( rv[0] ) == Integer.parseInt( end[0] )
            && Integer.parseInt( rv[1] ) == Integer.parseInt( end[1] )
            && rv[1].compareTo( end[1] ) > 0 ) {
          return false;
        }
        break;
      default:
        throw new IllegalArgumentException();
    }
    return true;
  }

  @Override
  public DME2RouteInfo getRouteInfo( String service, String version, String envContext )
      throws DME2Exception {
    throw new UnsupportedOperationException();


  }

  @Override
  public void lease( DME2Endpoint endpoint ) throws DME2Exception {

  }

  @Override
  public void publish( String servicePath, String contextPath, String host, int port, String protocol,
                       Properties props )
      throws DME2Exception {
    publish( servicePath, contextPath, host, port, latitude, longitude, protocol );
  }

  @Override
  public void publish( String service, String path, String host, int port, double latitude, double longitude,
                       String protocol ) throws DME2Exception {
    publish( service, path, host, port, latitude, longitude, protocol, null, false );
  }

  @Override
  public void publish( String service, String path, String host, int port, double latitude, double longitude,
                       String protocol, boolean updateLease )
      throws DME2Exception {
    this.publish( service, path, host, port, latitude, longitude, protocol );
  }

  @Override
  public void publish( String service, String path, String host, int port, String protocol, boolean updateLease )
      throws DME2Exception {
    publish( service, path, host, port, latitude, longitude, protocol, null, updateLease, null );
  }

  private void publish( final String newService, String path, String host, int port, double latitude, double longitude,
                        String protocol, String namespace, boolean updateLease, Properties props )
      throws DME2Exception {
    LOGGER.debug( null, "publish", LogMessage.METHOD_ENTER );
    LOGGER.info( null, "publish", "newService={} path={} host={} port={} latitude={} longitude={} protocol={} namespace={} updateLease={} props={}", newService, path, host, port, latitude, longitude, protocol, namespace, updateLease, props );
    String service = newService;
    final String uri = service.startsWith( "http://" ) ? service :
        ( "http://" + host + ":" + port + ( service.startsWith( "/" ) ? "" : "/" ) + service );
    DmeUniformResource uniformResource = null;
    try {
      uniformResource = new DmeUniformResource( getConfig(), uri );
    } catch ( Exception e ) {
      throw new DME2Exception( "AFT-DME2-0607",
          new ErrorContext().add( "extendedMessage", e.getMessage() )
              .add( "manager", managerName )
              .add( "service", service ).add( "host", host )
              .add( "port", "" + port )
              .add( "latitude", "" + latitude )
              .add( "longitude", "" + longitude )
              .add( "protocol", protocol ), e
      );
    }

    if ( service.contains( "?" ) ) {
      String[] tokens = service.split( "\\?" );
      service = tokens[0];
    }

    String serviceName = uniformResource.getService();

    String env = uniformResource.getEnvContext();


    // DME2Endpoint publishedEndpoint = new DME2Endpoint(getManager());
    DME2Endpoint publishedEndpoint = new DME2Endpoint( DME2DistanceUtil
        .calculateDistanceBetween( DME2AbstractEndpointRegistry.getClientLatitude(),
            DME2AbstractEndpointRegistry.getClientLongitude(), latitude, longitude ) );
    publishedEndpoint.setHost( host );
    publishedEndpoint.setPort( port );
    publishedEndpoint.setDmeUniformResource( uniformResource );

    if ( path != null ) {
      if ( uniformResource.getUrlType() == DmeUniformResource.DmeUrlType.STANDARD ) {
        publishedEndpoint.setContextPath( uniformResource.getBindContext() );
      } else {
        publishedEndpoint.setContextPath( path );
      }

    } else {
      if ( uniformResource.getUrlType() == DmeUniformResource.DmeUrlType.STANDARD ) {
        String cpath = uniformResource.getBindContext() + ( path == null ? "" : ( "," + path ) );
        publishedEndpoint.setContextPath( cpath );
      } else {
        publishedEndpoint.setContextPath( service );
      }
    }

    publishedEndpoint.setPath( service );
    publishedEndpoint.setServiceName( serviceName );
    publishedEndpoint.setEnvContext( env );
    publishedEndpoint.setProtocol( protocol );
    publishedEndpoint.setLatitude( latitude );
    publishedEndpoint.setLongitude( longitude );
    publishedEndpoint.setServiceVersion( uniformResource.getVersion() );
    publishedEndpoint.setRouteOffer( uniformResource.getRouteOffer() );

    final String versionRange = uniformResource.getSupportedVersionRange();
    if ( versionRange != null ) {
      publishedEndpoint.setSupportedVersionRange( versionRange );
    }

    LOGGER.info( null, "publish", "Published Endpoint: {}", publishedEndpoint );
    endpoints.add( publishedEndpoint );
    LOGGER.debug( null, "findEndpoints", LogMessage.METHOD_ENTER );
  }

  @Override
  public void unpublish( String serviceName, String host, int port )
      throws DME2Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void refresh() {
    throw new UnsupportedOperationException();
  }

  public long getEndpointCacheUpdatedAt() {
    return 0;
  }

/*
    @Override
    public void unpublish(String servicePath, String contextPath, String host, int port)
        throws DME2Exception
    {
      throw new UnsupportedOperationException();
    }
*/

  @Override
  public void shutdown() {
  }

  @Override
  public void init( Properties properties ) {
  }

  //    @Override
  public void setEndpointCacheUpdatedAt( long currentTime ) {
  }

  @Override
  public void publish( String service, String path, String host, int port,
                       String protocol ) throws DME2Exception {
    // TODO Auto-generated method stub
    publish( service, path, host, port, protocol, null );
  }

  @Override
  public void publish( String serviceURI, String serviceURI2, String hostAddress, int port, double latitude,
                       double longitude,
                       String protocol, Properties props, boolean updateLease ) throws DME2Exception {
    publish( serviceURI, serviceURI2, hostAddress, port, latitude, longitude, protocol, null, updateLease, props );

  }
  
  @Override
  public DME2Endpoint[] find(String serviceKey, String version, String env,
  		String routeOffer) throws DME2Exception {
  	List<DME2Endpoint> endpoints = findEndpoints(serviceKey, version, env, routeOffer);
  	DME2Endpoint[] endpointArray = null;
  	if(CollectionUtils.isNotEmpty(endpoints)) {
  		endpointArray = endpoints.toArray(new DME2Endpoint[endpoints.size()]);
  	}
  	return endpointArray;
   }

  @Override
  public void registerJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {
    throw new UnsupportedOperationException(  );
  }

  @Override
	public void updateJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deregisterJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ClientJVMInstance> findRegisteredJVM(String envContext, Boolean activeOnly, String hostAddress, String mechID, String processID) throws DME2Exception {
		throw new UnsupportedOperationException();
	}
}


