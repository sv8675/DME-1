package com.att.aft.dme2.cache.service;

import java.util.Set;

import com.att.aft.dme2.cache.domain.CacheConfiguration;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.mbean.DME2CacheMXBean;

/**
 * cache interface providing the below features as a part of this interface;
 * this interface or its implementing classes should not be instantiated directly;<br>
 * use {@link DME2CacheManager#createCache(String)} to create;<br>
 * use {@link DME2CacheManager#getCache(String)} to get the instance of the desired cache;
 * <ul>
 * 	<li>{@link #getCacheName()}</li>
 * 	<li>{@link #get(Key)}</li>
 * 	<li>{@link #put(Key, Value)}</li>
 * 	<li>{@link #remove(Key)}</li>
 * 	<li>{@link #refresh()}</li>
 * 	<li>{@link #refreshEntry(Key)}</li>
 * 	<li>{@link #shutdownTimerTask()}</li>
 * 	<li>{@link #clear()}</li>
 * </ul> 
 * @author ab850e
 *
 */
@SuppressWarnings("rawtypes")
public interface DME2Cache extends DME2CacheMXBean
{
	/**
	 * refreshing the cache using the registered data handler (if any)
	 * @see com.att.aft.dme2.cache.handler.service.CacheableDataHandler
	 */
	public void refresh();
	
	/**
	 * this will help to gracefully shutdown all registered cache level scheduled tasks<br> 
	 * as a hook to the shutdown ({@link DME2CacheManager#shutdown}) of Hazelcast container
	 */
	public void shutdownTimerTask();
	
	/**
	 * clear/empty the entries for this cache
	 */
	public void clear();

	/**
	 * get the value for the specified key from this cache
	 * @param k key for which value to be retrieved
	 * @return value for the entry again the specified key
	 */
	public Value get(Key k);

	/**
	 * put the value v for the specified key k in this cache; there is a subsequent process to wrap this v with {@link CacheElement}
	 * in order to admin specific properties to be used within the lifetime for this entry
	 * @param k key of the entry to be put in this cache
	 * @param v value of the entry to be put in this cache
	 * @return value for the entry again the specified key
	 */
	public void put(Key k, Value v);

	/**
	 * remove the entry from this cache with the specified key 
	 * @param k key of the cache ement which has to be removed
	 */
	public void remove(Key k);
	
	/**
	 * reload the specific cache entry on request,  
	 * @param k
	 * @return
	 * @throws CacheException
	 */
	public Value refreshEntry(Key k) throws CacheException;

	/**
	 * get all the keys for this cache
	 * @return
	 */
	public Set<Key> getKeySet();

	/**
	 * check if the cache is currently being refreshed;
	 * @return if being refreshed, return true; else false
	 */
	public boolean isRefreshing();

	/**
	 * get the cache name
	 * @return if being refreshed, return true; else false
	 */
	public String getCacheName();
	
	/**
	 * get the CacheEntryView
	 * @return return the entry view to be used for stats
	 */
	public CacheEntryView getEntryView();
	
	/**
	 * get the Cache config
	 * @return return the cache config
	 */
	public CacheConfiguration getCacheConfig();
}
