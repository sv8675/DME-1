package com.att.aft.dme2.manager.registry;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.DME2CacheStats;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;

public class DME2RouteOfferCache extends DME2AbstractRegistryCache<String,DME2RouteOffer> {

  /**
   * Base constructor
   *
   * @param valueClass Class being used for caching
   * @param type       type of endpoint registry
   * @param registry   actual registry for callbacks
   * @param isStale    is this a stale cache?
   * @throws com.att.aft.dme2.api.DME2Exception
   */
  public DME2RouteOfferCache( DME2Configuration config, Class valueClass, DME2EndpointRegistryType type,
                              DME2EndpointRegistry registry, String managerName, boolean isStale ) throws DME2Exception {
    super( config, valueClass, type, registry, managerName, isStale );
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
  public CacheElement fetchFromSource( CacheElement.Key<String> requestValue )
      throws DME2Exception {
    throw new DME2Exception( "AFT-DME2-0000", "Method not implemented" );
  }

  @Override
  public void refresh() {

  }

}
