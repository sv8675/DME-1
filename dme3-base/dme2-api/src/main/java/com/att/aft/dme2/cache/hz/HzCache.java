package com.att.aft.dme2.cache.hz;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.domain.CacheConfiguration;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.cache.service.CacheEntryView;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.Fork;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IMap;

/**
 * 
 */
@SuppressWarnings("rawtypes")
public class HzCache extends AbstractCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(HzCache.class.getName());
	private static String configFilenameWithPath = null;
	private static long LOCK_TIMEOUT_MS;
	private IMap<CacheElement.Key, CacheElement> cacheMap = null;
	private static int lockCount = 0;
	private static HazelcastInstance hz = null;
	private boolean isRefreshInProgress = false;
	private String cacheName = null;
	private ExecutorService PUT_ASYNC_SERVICE = Fork.createFixedThreadExecutorPool("PUT_ASYNC_SERVICE", 10);
	private CacheEntryView cacheEntryView = null;

	/**
	 * to be used for user cache
	 * @param cacheName - unique name of the cache 
	 * @param cacheType - cache type to be initialized
	 * @throws CacheException
	 */
	public HzCache(final String cacheName, final String cacheType,final DME2CacheableCallback source, final DME2Configuration config) throws CacheException{
		super(cacheName, cacheType, source, config);
		this.cacheName = cacheName;
		initialize();
	}

	/**
	 * to be used for user cache
	 * @param cacheConfig
	 */
	public HzCache(CacheConfiguration cacheConfig) 
	{
		super(cacheConfig);
		this.cacheConfig=cacheConfig;
		initialize();
	}
	
	@Override
	public CacheEntryView getEntryView(){
		return cacheEntryView;
	}
	
	/**
	 * loading the configuration from file
	 * @return Config object holding all the configuration details
	 */
	private static Config loadHzConfig()
	{
		LOGGER.info(null, "HzCache.loadHzConfig", "start");
		Config local_config = null;
		try{
			InputStream hzConfigXMLInputStream = HzCache.class.getResourceAsStream( DME2Constants.Cache.HZ_CACHE_CONFIG_FILE_NAME );
			if(hzConfigXMLInputStream==null){
				throw new CacheException(ErrorCatalogue.CACHE_021, DME2Constants.Cache.HZ_CACHE_CONFIG_FILE_NAME);
			}
			local_config = new XmlConfigBuilder( hzConfigXMLInputStream ).build();

			//local_config = new FileSystemXmlConfig(configFilenameWithPath);
			local_config.setInstanceName("DME_CACHE_INSTANCE");
		}catch(Exception e){
			LOGGER.warn(null, "HzCache.loadHzConfig", "exception");
			throw new CacheException(ErrorCatalogue.CACHE_010, e, DME2Constants.Cache.HZ_CACHE_CONFIG_FILE_NAME);
		}
		LOGGER.info(null, "HzCache.loadHzConfig", "complete");
		
		return local_config;
	}

	/**
	 * starting the Hazelcast container
	 * @return HazelcastInstance
	 */
	private static HazelcastInstance startHzContainer()
	{
		LOGGER.info(null, "HzCache.startHzContainer", "creating new Hazelcast container");
		return Hazelcast.newHazelcastInstance(loadHzConfig());
	}
	
	/**
	 * get the running Hazelcast instance; create if not exists
	 * @return HazelcastInstance
	 */
	public static HazelcastInstance getHzRunningInstance()
	{
		LOGGER.info(null, "HzCache.getHzRunningInstance", "start");
		if( hz == null)
		{
			LOGGER.info(null, "HzCache.getHzRunningInstance", "cache container is not running");
			hz = startHzContainer();
			LOGGER.info(null, "HzCache.getHzRunningInstance","cache container started");
		}
		LOGGER.info(null, "HzCache.getHzRunningInstance", "complete");
		return hz;
	}
	
	private HazelcastInstance getHz()
	{
		return hz;
	}

	public void initialize() 
	{
		LOGGER.info(null, "HzCache.initialize", "start - cache: {}", getCacheName());
		
		cacheMap = getHzRunningInstance().getMap(getCacheName());
		cacheEntryView = new HzCacheEntryView(cacheMap); 
				
		configFilenameWithPath = getConfig().getProperty(DME2Constants.Cache.CACHE_CONFIG_FILE_PATH_WITH_NAME);
		LOCK_TIMEOUT_MS = getConfig().getLong(DME2Constants.Cache.LOCK_TIMEOUT_MS);
		synchronized(this){
			if(hz==null){
				hz = startHzContainer();
			}
		}		
		super.init();

		LOGGER.info(null, "HzCache.initialize", "completed - cache: [{}]", getCacheName());
	}
	
	public boolean isContainerRunning()
	{
		if(hz==null || !hz.getLifecycleService().isRunning())
		{
			LOGGER.info(null, "HzCache.isRunning", "completed; hz[{}].getLifecycleService.isRunning: [{}]",hz, hz!=null?hz.getLifecycleService().isRunning():false); 
		}
		return hz!=null?hz.getLifecycleService().isRunning():false;
	}

	public IMap<CacheElement.Key, CacheElement> getCacheMap()
	{
		return this.cacheMap;
	}
	
	public void put(final Key k, final CacheElement element)
	{
		LOGGER.info(null, "AbstractCache.put(k,element)", "start put cache: [{}]", getCacheName());
		try{
			getCacheMap().put(k, element);
		}catch(Exception e){
			LOGGER.warn(null, "AbstractCache.put(k,element)", "cache:[{}] put operation encountered exception:[{}] ", getCacheName(), e.getMessage() );
		}
		LOGGER.info(null, "AbstractCache.put(k,element)", "completed put cache: [{}]", getCacheName());
	}
	
	private void putAsync(final Key k, final CacheElement element){
		PUT_ASYNC_SERVICE.execute(new Runnable() {
		    public void run() {
		    	put(k, element);
		    }
		});
	}
	
	//to update the last accessed time so that the unusedendpoint task can identify potential candidates
	private void updateLastAccessedTime(final Key key, final CacheElement element){
		element.setLastAccessedTime(getCurrentTimeMS());
		putAsync(key, element);
	}
	
	private Value get(final Key key, final boolean updateLastAccess){
		LOGGER.info(null, "HzCache.get", "start - cache: [{}] ,[{}]", getCacheName(), key);
		Value v = null;
		CacheElement element = null;
		
		try{
			element = (CacheElement)getCacheMap().get(key);
			if(updateLastAccess && element!=null && !element.isMarkedForRemoval()){
				updateLastAccessedTime(key, element);
			}
		}catch(HazelcastInstanceNotActiveException hze){
			LOGGER.warn(null, "HzCache.get" , "hazelcast is probably down!!!");
		}
		
		if(element!=null && !element.isMarkedForRemoval())
		{
			v = element.getValue();
		}
		LOGGER.info(null, "HzCache.get", "completed - cache: [{}] ,[{}], [{}]", getCacheName(), key, v);
		return v;
	}
	
	@Override
	public Value get(final Key key)
	{
		return get(key, true);
	}

	@Override
	public void remove(final Key key)
	{
		LOGGER.info(null, "HzCache.remove", "start - cache: [{}] ,[{}]", getCacheName(), key);
		try{
			CacheElement element = getCacheMap().get(key);
			if(element!=null){
				element.setMarkedForRemoval(true);
				getCacheMap().set(key, element);
				getCacheMap().removeAsync(key);
			}			
		}catch(HazelcastInstanceNotActiveException hze){
			LOGGER.warn(null, "HzCache.remove" , "hazelcast is probably down!!!");
		}
		LOGGER.info(null, "HzCache.remove", "completed- cache: [{}] ,[{}]", getCacheName(), key);
	}

  @Override
  public void shutdownTimerTask() {
    LOGGER.info(null, "shutdownTimerTask", "start");
    super.shutdownTimerTask();
    shutdown();
    LOGGER.info(null, "shutdownTimerTask", "exit");
  }

  public static void shutdown()
	{
		LOGGER.info(null, "HzCache.shutdown", "start");
		if(hz!=null && hz.getLifecycleService().isRunning())
		{
			getHzRunningInstance().shutdown();
		}
		hz = null;
		LOGGER.info(null, "HzCache.shutdown", "completed");
	}

	@Override
	public void lock(Key key)
	{
		LOGGER.info(null, "HzCache.lock", "start - cache: [{}], [{}]", getCacheName(), key);
		if(!getCacheMap().isLocked(key))
		{
			LOGGER.info(null, "HzCache.lock", "acquiring lock with [{}], [{}], lock count [{}]",getCacheName(), key, ++lockCount);
			getCacheMap().lock(key, LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		}else
		{
			LOGGER.info(null, "HzCache.lock", "exception - cache: [{}], [{}]", getCacheName(), key);
			throw new CacheException(CacheException.ErrorCatalogue.CACHE_005,getCacheName(),key);
		}
		LOGGER.info(null, "HzCache.lock", "complete - cache: [{}], [{}]", getCacheName(), key);
	}

	@Override
	public void unlock(Key key) 
	{
		LOGGER.info(null, "HzCache.unlock", "start - cache: [{}], [{}]", getCacheName(), key);
		if(getCacheMap().isLocked(key))
		{
			LOGGER.info(null, "HzCache.unlock", "releasing lock with  [{}], [{}], lock count [{}]",getCacheName(), key, --lockCount);
			getCacheMap().unlock(key);
		}
		LOGGER.info(null, "HzCache.unlock", "complete - cache: [{}], [{}]", getCacheName(), key);
	}

	@Override
	public void clear() 
	{
		LOGGER.info(null, "HzCache.clear", "start - cache: [{}]", getCacheName());
		if(isContainerRunning())
		{
			try{
				getCacheMap().clear();
			}catch(HazelcastInstanceNotActiveException hze){
				LOGGER.warn(null, "HzCache.clear" , "hazelcast is probably down!!!");
			}
		}else
		{
			LOGGER.info(null, "HzCache.clear", "container not running [{}]", getCacheName());
		}
		LOGGER.info(null, "HzCache.clear", "end - cache: [{}]", getCacheName());
	}

	@Override
	public int getCurrentSize() 
	{
		LOGGER.info(null, "HzCache.getCurrentSize", "start - cache: [{}]", getCacheName());
		int size = -1;
		if(isContainerRunning())
		{
			try{
				size = getKeySet().size();
			}catch(HazelcastInstanceNotActiveException hze){
				LOGGER.error(null, "HzCache.getCurrentSize" , "error encountered, hazelcast is probably down!!!");
			}
		}else
		{
			LOGGER.info(null, "HzCache.getCurrentSize", "container not running [{}]", getCacheName());
		}
		LOGGER.info(null, "HzCache.getCurrentSize", "end - cache: [{}]", getCacheName());
		return size;
	}

	@Override
	public long getExpirationTime(String key)
	{
		return getExpirationTime(new Key<String>(key));
	}

	public long getCacheTTLValue(Key key)
	{
		LOGGER.info(null, "HzCache.getCacheEntryTTLValue", "start - cache: [{}]", getCacheName());
		long ttl=-1;
		if(isContainerRunning())
		{
			try{
				if(getCacheMap()!=null)
				{
					if(getCacheMap().getEntryView(key)!=null)
					{
						ttl = getCacheMap().getEntryView(key).getTtl();
					}else
					{
						LOGGER.warn(null, "HzCache.getCacheEntryTTLValue", "key: {} does not exist");
					}
				}
				
			}catch(HazelcastInstanceNotActiveException hze){
				LOGGER.warn(null, "HzCache.getCacheEntryTTLValue" , "hazelcast is probably down!!!");
			}
		}else
		{
			LOGGER.info(null, "HzCache.getCacheEntryTTLValue", "container not running [{}]", getCacheName());
		}
		LOGGER.info(null, "HzCache.getCacheEntryTTLValue", "end - cache: [{}], cache entry ttl: [{}]", getCacheName(), ttl);
		return ttl;
	}
		
	public long getExpirationTime(Key key)
	{
		LOGGER.info(null, "HzCache.getCacheEntryExpirationTime", "start - cache: [{}]", getCacheName());
		long expirationTime=-1;
		
		if(isContainerRunning())
		{
			try{
				if(getCacheMap()!=null){
					if( getCacheMap().getEntryView(key)!=null ){
						expirationTime = getCacheMap().getEntryView(key).getExpirationTime();
					}else{
						LOGGER.warn(null, "HzCache.getExpirationTime", "key: {} does not exist");
					}
				}
			}catch(HazelcastInstanceNotActiveException hze){
				LOGGER.warn(null, "HzCache.getCacheEntryTTLValue" , "hazelcast is probably down!!!");
			}

		}else
		{
			LOGGER.info(null, "HzCache.getCacheEntryExpirationTime", "container not running [{}]", getCacheName());
		}
		LOGGER.info(null, "HzCache.getCacheEntryExpirationTime", "end - cache: [{}], cache entry expiration time: [{}]", getCacheName(), expirationTime);
		return expirationTime;
	}

	@Override
	public Set<Key> getKeySet() 
	{
		Set<Key> keys = new HashSet<Key>();
		try{
			for(Key k : getCacheMap().keySet()){
				if(getCacheMap().get(k)!=null && !getCacheMap().get(k).isMarkedForRemoval()){
					keys.add(k);
				}
			}
		}catch(HazelcastInstanceNotActiveException hze){
			LOGGER.warn(null, "HzCache.getCacheEntryTTLValue" , "hazelcast is probably down!!!");
		}
		return keys;
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
	public void setRefreshInProgress(boolean refreshInProgress) {
		this.isRefreshInProgress = refreshInProgress;
	}

	@Override
	public boolean isRefreshing() {
		return isRefreshInProgress;
	}
	
	@Override
	public String getCacheName() 
	{
		return this.cacheName;
	}

	@Override
	public long getCacheTTLValue(String key) {
		return getCacheTTLValue(new Key<String>(key));
	}

	
}