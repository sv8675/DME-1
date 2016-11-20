package com.att.aft.dme2.manager.registry;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.DME2CacheStats;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;

public class DME2RouteInfoCacheFS extends DME2AbstractRegistryCache<String,DME2RouteInfo> {
  private DME2EndpointRegistryFS registry;

  public DME2RouteInfoCacheFS(  DME2Configuration config, DME2EndpointRegistryFS registry, String managerName ) throws DME2Exception {
    super( config, DME2RouteInfo.class, DME2EndpointRegistryType.FileSystem, registry, managerName, false );
    this.registry = registry;
  }

  @Override
  public CacheElement fetchFromSource( @SuppressWarnings("rawtypes") CacheElement.Key requestValue ) throws DME2Exception {
    return createCacheElement( requestValue, createCacheValue( registry.fetchRouteInfoFromSource( (String) requestValue.getKey() ) ));
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

  @Override
  public void refresh() {

  }
}
