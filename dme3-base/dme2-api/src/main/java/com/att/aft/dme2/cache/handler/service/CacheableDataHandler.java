/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.handler.service;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;

/**
 * Any class implementing CacheableDataHandler can be used to register its instance as a data handler for the <br> cache to be used to refresh or reload data on request
 * <br><b>See Also:</b><br> {@link com.att.aft.dme2.cache.service.DME2Cache#refresh()}, {@link com.att.aft.dme2.cache.service.DME2Cache#refreshEntry(Key) } 
 * @author ab850e
 *
 * @param <K> instance of {@link Key}
 * @param <V> instance of {@link Value}
 */
public interface CacheableDataHandler<K,V>
{
	/**
	 * Retrieve cacheable data from the "source" for all keys. A Map would be returned with values for every key to be cached; for any error for a given key, there is expected to be an exception for the same key; 
	 * if there is exception then the existing entry would not be touched; if any key is present in the cache which is not present in this map, then that entry in the cache would be removed.  
	 * @param keySet containing all the keys for which data to be retrieved
	 * @param source from where data would be retrieved
	 * @return for every key return the value to be cached; for any error, there is expected to be an exception for the same key; if there is exception then the existing entry 
	 * will not be  
	 * @throws CacheException
	 */
	public Map<Key<K>,Pair<CacheElement, Exception>> getDataForAllKeys(Set<Key> keySet, DME2CacheableCallback source) throws CacheException;
	public CacheElement getData(Key<K> key, DME2CacheableCallback source) throws CacheException;
}
