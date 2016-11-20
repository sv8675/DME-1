/**
 *
 */
package com.att.aft.dme2.cache.legacy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.domain.CacheConfiguration;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.service.CacheEntryView;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.Fork;


/**
 * @author ab850e
 *
 */
@SuppressWarnings("rawtypes")
public class DME2DefaultCache extends AbstractCache
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2DefaultCache.class.getName());
	private final Map<Key, CacheElement> cache = new ConcurrentHashMap<Key,CacheElement>( 16,0.9f,1 );
	private boolean isRefreshInProgress = false;
	private String cacheName = null;
	private CacheEntryView cacheEntryView = null;
	private ExecutorService PUT_ASYNC_SERVICE = Fork.createFixedThreadExecutorPool("PUT_ASYNC_SERVICE", 10);
	private ExecutorService REMOVE_ASYNC_SERVICE = Fork.createFixedThreadExecutorPool("REMOVE_ASYNC_SERVICE", 10);

	/**
	 * @param cacheName
	 */
	public DME2DefaultCache(final String cacheName, final String cacheType, final DME2CacheableCallback source, final DME2Configuration config)
	{
		super(cacheName,cacheType, source, config);
		this.cacheName = cacheName;
		initialize();
	}

	/**
	 * to be used for user cache
	 * @param cacheConfig
	 */
	public DME2DefaultCache(CacheConfiguration cacheConfig) 
	{
		super(cacheConfig);
		this.cacheConfig=cacheConfig;
		initialize();
	}

	public void initialize()
	{
		LOGGER.debug(null, "DME2DefaultCache.initialize", "start - cache: {}", getCacheName());
		cacheEntryView = new DefaultCacheEntryView(cache);
		super.init();
		LOGGER.debug(null, "DME2DefaultCache.initialize", "completed - cache: [{}]", getCacheName());
	}

	@Override
	public CacheEntryView getEntryView(){
		return cacheEntryView;
	}
	
	/* (non-Javadoc)
	 * @see com.att.aft.dme2.cache.service.DME2Cache#refresh()
	 */

	public void put(final Key k, final CacheElement element)
	{
		LOGGER.debug(null, "DME2DefaultCache.put(k,element)", "start - cache: [{}]", getCacheName());
		try{
			getCacheMap().put(k, element);
		}catch(Exception e){
			if(e instanceof CacheException)
			{
				LOGGER.debug(null, "DME2DefaultCache.put(k,element)", "exception in put - cache: [{}]", getCacheName());
			}
		}
		LOGGER.debug(null, "DME2DefaultCache.put(k,element)", "completed - cache: [{}]", getCacheName());
	}

	/* (non-Javadoc)
	 * @see com.att.aft.dme2.cache.service.DME2Cache#clear()
	 */
	@Override
	public void clear()
	{
		LOGGER.debug(null, "DME2DefaultCache.clear", "start - cache: [{}]", getCacheName());
		getCacheMap().clear();
		LOGGER.debug(null, "DME2DefaultCache.clear", "end - cache: [{}]", getCacheName());
	}

	//to update the last accessed time so that the unusedendpoint task can identify potential candidates
	private void putAsync(final Key k, final CacheElement element){
		PUT_ASYNC_SERVICE.execute(new Runnable() {
		    public void run() {
		    	put(k, element);
		    }
		});
	}
	private void updateLastAccessedTime(final Key key, final CacheElement element){
		element.setLastAccessedTime(getCurrentTimeMS());
		putAsync(key, element);
	}
	private Value get(final Key key, final boolean updateLastAccess){
		LOGGER.debug(null, "DME2DefaultCache.get", "start - cache: [{}] ,[{}]", getCacheName(), key);
		Value v = null;
		CacheElement element = null;
		
		element = (CacheElement)getCacheMap().get(key);
		if(updateLastAccess && element!=null && !element.isMarkedForRemoval()){
			updateLastAccessedTime(key, element);
		}

		if(element!=null && !element.isMarkedForRemoval())
		{
			v = element.getValue();
		}
		LOGGER.debug(null, "DME2DefaultCache.get", "completed - cache: [{}] ,[{}], [{}]", getCacheName(), key, v);
		return v;
	}
	/* (non-Javadoc)
	 * @see com.att.aft.dme2.cache.service.DME2Cache#get(com.att.aft.dme2.cache.domain.CacheElement.Key)
	 */
	@Override
	public Value get(final Key key)
	{
		return get(key, true);
	}

	/* (non-Javadoc)
	 * @see com.att.aft.dme2.cache.service.DME2Cache#remove(com.att.aft.dme2.cache.domain.CacheElement.Key)
	 */
	@Override
	public void remove(Key key)
	{
		LOGGER.debug(null, "DME2DefaultCache.remove(k,element)", "start - cache: [{}], key[{}]", getCacheName(), key);
		try{
			getCacheMap().remove(key);
			/*CacheElement element = getCacheMap().get(key);
			if(element!=null){
				element.setMarkedForRemoval(true);
				//getCacheMap().put(key, element);//to make sure the element marked for removal and not retrieved again till actually deleted by remove async service
				cache.remove(key);
				//getCacheMap().remove(key);
				REMOVE_ASYNC_SERVICE.execute(new Runnable() {
				    public void run() {
				    	try{
				    		getCacheMap().remove(key);
				    	}catch(Exception ex){
				    		LOGGER.error(null, "DME2DefaultCache.remove" , "key: [{}] not removed, error: [{}]",key, ex.getMessage());
				    	}
				    }
				});
			}*/			
		}catch(Exception e){
			LOGGER.error(null, "DME2DefaultCache.remove" , "key: [{}] not removed, error: [{}]",key, e.getMessage());
		}
		LOGGER.debug(null, "DME2DefaultCache.remove(k,element)", "completed - cache: [{}], key[{}]", getCacheName(), key);
	}

	/* (non-Javadoc)
	 * @see com.att.aft.dme2.mbean.DME2CacheJMXBean#getCurrentSize()
	 */
	@Override
	public int getCurrentSize()
	{
		LOGGER.debug(null, "DME2DefaultCache.getCurrentSize", "start - cache: [{}]", getCacheName());
		int size = 0;
		for(Key key : getCacheMap().keySet()){
			if(!getCacheMap().get(key).isMarkedForRemoval())
				size++;
		}
		LOGGER.debug(null, "DME2DefaultCache.getCurrentSize", "end - cache: [{}]", getCacheName());
		return size;
	}

	
	@Override
	public long getCacheTTLValue(String key)
	{
		LOGGER.info(null, "DME2DefaultCache.getCacheTTLValue ", key);
		return getCacheTTLValue(new Key<String>(key));
	}	
	/* (non-Javadoc)
	 * @see com.att.aft.dme2.mbean.DME2CacheJMXBean#getCacheEntryTTLValue(com.att.aft.dme2.cache.domain.CacheElement.Key)
	 */

	public long getCacheTTLValue(Key key)
	{
		LOGGER.debug(null, "DME2DefaultCache.getCacheEntryTTLValue", "start - cache: [{}]", getCacheName());
		long ttl=-1;
		if(cacheEntryView!=null && getCacheMap()!=null)
		{
			if(cacheEntryView.getEntry(key)!=null)
			{
				ttl = cacheEntryView.getEntry(key).getTtl();
			}else
			{
				LOGGER.warn(null, "DME2DefaultCache.getCacheEntryTTLValue", "key: {} does not exist");
			}
		}
		LOGGER.debug(null, "DME2DefaultCache.getCacheEntryTTLValue", "end - cache: [{}], cache entry ttl: [{}]", getCacheName(), ttl);
		return ttl;
	}

	@Override
	public long getExpirationTime(String key)
	{
		return getExpirationTime(new Key<String>(key));
	}
	
	/* (non-Javadoc)
	 * @see com.att.aft.dme2.mbean.DME2CacheMXBean#getExpirationTime(com.att.aft.dme2.cache.domain.CacheElement.Key)
	 */
	public long getExpirationTime(Key key)
	{
		LOGGER.debug(null, "DME2DefaultCache.getCacheEntryExpirationTime", "start - cache: [{}]", getCacheName());
		long expirationTime=-1;
		if(cacheEntryView!=null && getCacheMap()!=null)
		{
			if(cacheEntryView.getEntry(key)!=null)
			{
				expirationTime = cacheEntryView.getEntry(key).getExpirationTime();
			}else
			{
				LOGGER.warn(null, "DME2DefaultCache.getExpirationTime", "key: {} does not exist");
			}
		}
		LOGGER.debug(null, "DME2DefaultCache.getCacheEntryExpirationTime", "end - cache: [{}], cache entry expiration time: [{}]", getCacheName(), expirationTime);
		return expirationTime;
	}

	@Override
	public Set<Key> getKeySet() 
	{
		Set<Key> keys = new HashSet<Key>();
		try{
			for(Key k : getCacheMap().keySet()){
				if(cacheEntryView.getEntry(k)!=null && !cacheEntryView.getEntry(k).isMarkedForRemoval()){
					keys.add(k);
				}
			}
		}catch(Exception e){
			LOGGER.warn(null, "DME2DefaultCache.getKeySet" , "error while getting keyset");
		}
		return keys;
	}

	@Override
	public Map<Key, CacheElement> getCacheMap()
	{
		return cache;
	}
	@Override
	public void lock(Key k) {
	}
	@Override
	public void unlock(Key k) {
	}
	@Override
	public boolean isPutAllow(Key key, Value value)
	{
		return true;
	}
	@Override
	public void refresh(){
		super.refresh();
	}
	@Override
	public void checkNRemoveUnusedEndpoints(){
		super.checkNRemoveUnusedEndpoints();
	}
	@Override
	public boolean isRefreshing() {
		return isRefreshInProgress;
	}

	@Override
	public String getCacheName() {
		return this.cacheName;
	}
	
	@Override
	public void setRefreshInProgress(boolean refreshInProgress) {
		this.isRefreshInProgress = refreshInProgress;
	}

}
