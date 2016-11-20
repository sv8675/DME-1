package com.att.aft.dme2.cache.service;

import java.util.Map;
import java.util.Set;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheElement;

/**
 * Methods that can be called from the cache object onto the callback object
 */
public interface DME2CacheableCallback<K,V> {
  /**
   * Hook back to fetch data from the source
   * @param requestValue Request value to retrieve source information
   * @return Value wrapped in CacheElement.Value
   * @throws DME2Exception General fetch exception
   */
  public CacheElement fetchFromSource( CacheElement.Key<K> requestValue ) throws DME2Exception;

  /**
   * Hook back to the fetch data from the source.  Exceptions will be carried back with the return map.
   *
   * @param requestValues Key set to request
   * @return Map of Keys to either Value or Exception
   */
  public Map<CacheElement.Key<K>, org.apache.commons.lang3.tuple.Pair<CacheElement, Exception>> fetchFromSource(
      Set<CacheElement.Key<K>> requestValues );

  /**
   * Refreshes the entire cache
   */
  public void refresh();
}
