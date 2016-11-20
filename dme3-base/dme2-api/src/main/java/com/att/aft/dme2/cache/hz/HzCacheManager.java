/**
 * 
 */
package com.att.aft.dme2.cache.hz;

import java.util.HashMap;
import java.util.Map;

import com.att.aft.dme2.cache.AbstractCacheManager;
import com.att.aft.dme2.cache.domain.CacheConfiguration;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.mbean.DME2CacheJMXBean;

/**
 *
 */
public class HzCacheManager extends AbstractCacheManager
{
	private static final Logger LOGGER = LoggerFactory.getLogger(HzCacheManager.class.getName());
	private static Map<String, HzCacheManager> globalCacheManagerRegister = new HashMap<String, HzCacheManager>();
	
	
	public static HzCacheManager getInstance(final DME2Configuration config){
		String managerName = config.getManagerName()!=null?config.getManagerName():"--default--";
		HzCacheManager hzCacheManager = globalCacheManagerRegister.get(managerName);
		if(hzCacheManager==null){
			hzCacheManager = new HzCacheManager(config);
			globalCacheManagerRegister.put(managerName, hzCacheManager);
		}
		return hzCacheManager;
	}
	
	public HzCacheManager()
	{
		super();
	}
	public HzCacheManager(final DME2Configuration config)
	{
		super(config);
		
	}
	
	@Override
	public DME2Cache createCache(final String cacheName, final String cacheType, final DME2CacheableCallback source) 
	{
		LOGGER.info(null, "createCache", "creating cache instance for [{}]", cacheName);
		if(cacheName==null){
			throw new CacheException(ErrorCatalogue.CACHE_015);
		}
		if(cacheType==null){
			throw new CacheException(ErrorCatalogue.CACHE_016);
		}
		if(globalCacheRegister.contains(cacheName))
		{
			throw new CacheException(ErrorCatalogue.CACHE_014, cacheName);
		}
		
		HzCache cache = new HzCache(cacheName, cacheType, source, config);
		globalCacheRegister.add(cacheName);
		instanceCacheManagerRegister.put(cacheName, cache);
		LOGGER.info(null, "createCache", "created cache instance for [{}]", cacheName);
		return cache;
	}
	@Override
	public DME2Cache createCache(final CacheConfiguration cacheConfig) 
	{
		if(cacheConfig==null){
			throw new CacheException(ErrorCatalogue.CACHE_017);
		}
		if(cacheConfig.getCacheName()==null){
			throw new CacheException(ErrorCatalogue.CACHE_015);
		}
		if(!globalCacheRegister.contains(cacheConfig.getCacheName()))
		{
			throw new CacheException(ErrorCatalogue.CACHE_014, cacheConfig.getCacheName());
		}
		
		HzCache cache = new HzCache(cacheConfig);
		globalCacheRegister.add(cacheConfig.getCacheName());
		instanceCacheManagerRegister.put(cacheConfig.getCacheName(), cache);
		LOGGER.info(null, "createCache", "created cache instance for [{}]", cacheConfig.getCacheName());
		return cache;
	}

	@Override
	public DME2Cache getCache(String cacheName) {
		return instanceCacheManagerRegister.get(cacheName);
	}

	@Override
	public void shutdown() 
	{
		super.shutdown();
		globalCacheManagerRegister.remove(getConfig().getManagerName());
	}
	public boolean isCacheContainerRunning() 
	{
		LOGGER.debug(null, "isCacheContainerRunning", "cacheRegister is [{}]", globalCacheRegister);
		return globalCacheRegister.isEmpty()?false:true;
	}

	@Override
	public DME2CacheJMXBean getCacheBean(String name) 
	{
		return (DME2CacheJMXBean)getCache(name);
	}
}