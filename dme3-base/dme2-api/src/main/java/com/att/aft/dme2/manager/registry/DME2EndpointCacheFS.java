package com.att.aft.dme2.manager.registry;

import java.util.ArrayList;
import java.util.List;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.DME2CacheStats;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * DME2 Endpoint Cache for File System Registry
 */
public class DME2EndpointCacheFS extends DME2AbstractRegistryCache<String,DME2ServiceEndpointData> {
  private static final Logger logger = LoggerFactory.getLogger( DME2EndpointCacheFS.class );
  private DME2EndpointRegistryFS registry;

  /**
   * Base constructor
   * @param registry Endpoint Registry
   * @throws DME2Exception
   */
  public DME2EndpointCacheFS(  DME2Configuration config, DME2EndpointRegistryFS registry, String managerName, boolean isStale ) throws DME2Exception {
    super( config, DME2Endpoint.class, DME2EndpointRegistryType.FileSystem, registry, managerName, isStale );
    this.registry = registry;
  }

  /**
   * Get endpoints
   * @param service Service Path
   * @return List of DME2 Endpoints
   */
  public List<DME2Endpoint> getEndpoints( String service ) {
    DME2ServiceEndpointData serviceEndpointData = get( service );
    if ( serviceEndpointData != null ) {
      return serviceEndpointData.getEndpointList();
    }
    return new ArrayList<DME2Endpoint>();
  }

  /**
   * Associate a list of endpoints with a particular service path
   * @param service service path
   * @param endpoints list of endpoints
   */
  public void putEndpoints( String service, List<DME2Endpoint> endpoints ) {
    // TTL and lastQueried should be set by cache
    DME2ServiceEndpointData serviceEndpointData = new DME2ServiceEndpointData( endpoints, service, 0, 0 );
    put( service, serviceEndpointData );
  }

  /**
   * {@inheritDoc}
   * @param requestValue Request value to retrieve source information
   * @return CacheElement Value containing Service Endpoint data
   * @throws DME2Exception
   */
  @Override
  public CacheElement fetchFromSource( CacheElement.Key<String> requestValue ) throws DME2Exception {
    if ( requestValue == null ) {
      logger.warn( null, "fetchFromSource", "Request element was null" );
      return null;
    }
    List<DME2Endpoint> endpoints = registry.fetchEndpointsFromSource( requestValue.getKey() );
    CacheElement.Value<DME2ServiceEndpointData> value = createCacheValue( new DME2ServiceEndpointData( endpoints, requestValue.getKey(), 0, 0 ) );
    return createCacheElement( requestValue, value );
  }

  @Override
  public void refresh() {

  }

  @Override
  public DME2CacheStats getStats( String serviceName, Integer hourOfDay ) {
    return null;
  }

  @Override
  public void disableCacheStats() {

  }

  @Override
  public void enableCacheStats() {

  }

  @Override
  public boolean isCacheStatsEnabled() {
    return false;
  }
}
