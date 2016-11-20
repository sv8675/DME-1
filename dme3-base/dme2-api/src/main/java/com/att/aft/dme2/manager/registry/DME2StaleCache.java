package com.att.aft.dme2.manager.registry;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.DME2CacheStats;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;

public class DME2StaleCache extends DME2AbstractRegistryCache<String,Long> {
  private boolean enableCacheStats;
  /**
   * Base constructor
   *
   * @param valueClass  Class being used for caching
   * @param type        type of endpoint registry
   * @param registry    actual registry for callbacks
   * @param managerName
   */
  public DME2StaleCache( DME2Configuration config, Class valueClass, DME2EndpointRegistryType type,
                         DME2EndpointRegistry registry, String managerName )
      throws DME2Exception {
    super( config, valueClass, type, registry, managerName, true );
  }

  @Override
  public CacheElement fetchFromSource( CacheElement.Key<String> requestValue ) throws DME2Exception {
    throw new UnsupportedOperationException( "Invalid operation for this type of cache" );
  }

	@Override
	public void refresh() {
		throw new UnsupportedOperationException( "Invalid operation for this type of cache" );
	}

	@Override
	public DME2CacheStats getStats(String serviceName, Integer hourOfDay) {
		
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void disableCacheStats() {
		enableCacheStats = false;
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
