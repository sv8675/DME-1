/**
 * 
 */
package com.att.aft.dme2.cache.legacy;

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
 * @author ab850e
 *
 */
public class DME2DefaultCacheManager extends AbstractCacheManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(DME2DefaultCacheManager.class.getName());
	private static Map<String, DME2DefaultCacheManager> globalCacheManagerRegister = new HashMap<String, DME2DefaultCacheManager>();
	
	public static DME2DefaultCacheManager getInstance(final DME2Configuration config){
		String managerName = config.getManagerName()!=null?config.getManagerName():"--default--";
		DME2DefaultCacheManager dME2DefaultCacheManager = globalCacheManagerRegister.get(managerName);
		if(dME2DefaultCacheManager==null){
			dME2DefaultCacheManager = new DME2DefaultCacheManager(config);
			globalCacheManagerRegister.put(managerName, dME2DefaultCacheManager);
		}
		return dME2DefaultCacheManager;
	}

	public DME2DefaultCacheManager(DME2Configuration config)
	{
		super(config);
	}
	
	@Override
	public DME2Cache createCache(final String cacheName, final String cacheType, final DME2CacheableCallback source) 
	{
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

		DME2DefaultCache cache = new DME2DefaultCache(cacheName, cacheType, source, config);
		globalCacheRegister.add(cacheName);
		instanceCacheManagerRegister.put(cacheName, cache);
		LOGGER.debug(null, "createCache", "created cache instance for [{}]", cacheName);
		return cache;
	}

	@Override
	public void shutdown() 
	{
		super.shutdown();
		globalCacheManagerRegister.remove(getConfig().getManagerName());
	}

	@Override
	public DME2Cache getCache(String cacheName) {
		return instanceCacheManagerRegister.get(cacheName);
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

	@Override
	public DME2Cache createCache(CacheConfiguration cacheConfig) {
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

		DME2DefaultCache cache = new DME2DefaultCache(cacheConfig.getCacheName(), cacheConfig.getCacheType().getName(), cacheConfig.getCacheDataSource(), getConfig());
		globalCacheRegister.add(cacheConfig.getCacheName());
		instanceCacheManagerRegister.put(cacheConfig.getCacheName(), cache);
		LOGGER.debug(null, "createCache", "created cache instance for [{}]", cacheConfig.getCacheName());
		return cache;
	}
}