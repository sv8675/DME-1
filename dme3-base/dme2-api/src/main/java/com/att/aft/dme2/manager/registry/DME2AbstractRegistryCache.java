package com.att.aft.dme2.manager.registry;

import static com.att.aft.dme2.logging.LogMessage.METHOD_ENTER;
import static com.att.aft.dme2.logging.LogMessage.METHOD_EXIT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.DME2CacheStats;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * Abstracted registry cache
 *
 * @param <K>
 *            Key used for CacheElement.Key
 * @param <V>
 *            Value used for CacheElement.Value
 */
public abstract class DME2AbstractRegistryCache<K, V> implements DME2CacheableCallback<K, V> {
  private static final Logger logger = LoggerFactory.getLogger( DME2AbstractRegistryCache.class );

	protected DME2Cache cache;
	public DME2Cache getCache() {
		return cache;
	}

	protected String cacheName;
	protected DME2Configuration config;

	/**
	 * Base constructor
	 *
	 * @param valueClass
	 *            Class being used for caching
	 * @param type
	 *            type of endpoint registry
	 * @param registry
	 *            actual registry for callbacks
	 * @throws DME2Exception
	 */
	public DME2AbstractRegistryCache(DME2Configuration config, Class valueClass, DME2EndpointRegistryType type,
			DME2EndpointRegistry registry, String managerName, boolean isStale) throws DME2Exception {
		this.config = config;
		cacheName = valueClass.getName() + "_" + type.toString() + "_" + managerName; // registry.getManager().getName();
		if (isStale) {
			cacheName += "_stale";
		}
		cache = DME2CacheFactory.getCacheManager(config).getCache(cacheName);
		if (cache == null) {
			// TODO: Change cacheType to enum (should be a cache object)
			String cacheType;
			if (DME2Endpoint.class.equals(valueClass)) {
				cacheType = "EndpointCache";
			} else if (DME2RouteInfo.class.equals(valueClass)) {
				cacheType = "RouteInfoCache";
			} else if (DME2RouteOffer.class.equals(valueClass)) {
				cacheType = "RouteOfferCache";
			} else {
				throw new DME2Exception("SOME-CODE", "Unknown cache type");
			}
			if (isStale) {
				cacheType = "Stale" + cacheType;
			}
			cache = DME2CacheFactory.getCacheManager(config).createCache(cacheName, cacheType, this);
		}
	}

	/**
	 * Get the value stored under the key
	 * 
	 * @param key
	 *            Key to perform lookup
	 * @return Value referenced by key
	 */
	public V get(K key) {
		CacheElement.Value<V> cacheElement = cache.get(createCacheKey(key));
		if (cacheElement != null) {
			return cacheElement.getValue();
		}
		return null;
	}

	/**
	 * Gets the cache element (wrapper object)
	 * @param key Key value
	 * @return cache element
   */
	public CacheElement getCacheElement( K key ) {
		return cache.getEntryView().getEntry( createCacheKey(key) );
	}

	/**
	 * Puts the value into the cache under the designated key
	 * 
	 * @param key
	 *            Key to store as reference
	 * @param value
	 *            Value to store
	 */
	public void put(K key, V value) {
		CacheElement.Key cacheKey = createCacheKey( key );
		((AbstractCache) cache).put(cacheKey, createCacheElement( cacheKey, createCacheValue(value)));
	}

	protected CacheElement.Key<K> createCacheKey(K key) {
		return new CacheElement.Key<K>(key);
	}

	protected CacheElement.Value<V> createCacheValue(V value) {
		return new CacheElement.Value<V>(value);
	}

	/**
	 * Remove the value from the cache designated by the key
	 * 
	 * @param key
	 *            Key to remove
	 */
	public void remove(K key) {
		cache.remove(createCacheKey(key));
	}

	/**
	 * Gets all keys in the cache
	 * 
	 * @return Set of keys
	 */
	public Set<CacheElement.Key> getKeySet() {
		return ((AbstractCache) cache).getKeySet();
	}

	/**
	 * Return true or false based upon whether the cache contains a key
	 * 
	 * @param key
	 *            Key to check
	 * @return true or false depending upon whether the cache contains the key
	 */
	public boolean containsKey(K key) {
    return get( key ) != null;
	}

	/**
	 * Get the current number of keys in the cache
	 * 
	 * @return number of keys in the cache
	 */
	public int getCurrentSize() {
		return ((AbstractCache) cache).getCurrentSize();
	}

	/**
	 * Clears all entries in the cache
	 */
	public void clear() {
		
		logger.debug( null, "clear", METHOD_ENTER );
		cache.clear();
		logger.debug( null, "clear", METHOD_EXIT );
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param requestValues
	 *            Key set to request
	 * @return Map of Keys to Pairs of Value, Exception - one of Value or
	 *         Exception will be null depending upon the ability to retrieve
	 */
	@Override
	public Map<CacheElement.Key<K>, Pair<CacheElement, Exception>> fetchFromSource(
			Set<CacheElement.Key<K>> requestValues) {
		Map<CacheElement.Key<K>, Pair<CacheElement, Exception>> map = new HashMap<CacheElement.Key<K>, Pair<CacheElement, Exception>>();
		for (CacheElement.Key<K> key : requestValues) {
			try {
				CacheElement value = fetchFromSource(key);
				map.put(key, new ImmutablePair<CacheElement, Exception>(value, null));
			} catch (DME2Exception e) {
				map.put(key, new ImmutablePair<CacheElement, Exception>(null, e));
			}
		}
		return map;
	}

	protected CacheElement createCacheElement( CacheElement.Key k, CacheElement.Value v ) {
		return ((AbstractCache) cache).createElement( k, v );
	}

  public abstract DME2CacheStats getStats( String serviceName, Integer hourOfDay );

  public abstract void disableCacheStats();

  public abstract void enableCacheStats();

  public abstract boolean isCacheStatsEnabled();
}
