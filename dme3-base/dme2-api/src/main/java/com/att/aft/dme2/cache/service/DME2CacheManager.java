/**
 * 
 */
package com.att.aft.dme2.cache.service;

import com.att.aft.dme2.cache.domain.CacheConfiguration;
import com.att.aft.dme2.mbean.DME2CacheJMXBean;


/**
 * cache manager used to maintain and execute admin process for all the caches
 * @author ab850e
 *
 */
public interface DME2CacheManager 
{
	//public boolean isEndpointStale(String endpoint);
	//public boolean isUrlInStaleList(String url);
	//public void addStaleEndpoint(String url);
	//public void removeStaleEndpoint(String url);
	//public ServiceEndpointData fetchEndpoints(String service);
	//public void addEndpoint(String service, ServiceEndpointData data);
	//public List<DME2Endpoint> getEndpoints(String service); 
	//public void refresh(String cacheName);
	//public DME2RouteInfo getRouteInfo(String path);
	//public void addStaleRouteOffer(String url);
	//public boolean isRouteOfferStale(String url);	
	/**
	 * Create an instance of the cache type for the specific cache name. The source is needed to be specified if there is a data handler binded to the cache. 
	 * DME2CacheableCallback would be used to fetch endpoints based on the cache keys during cache.refresh. If it is a custom/user cache you may it as null  
	 * @param name of the cache
	 * @param cacheType ({@link com.att.aft.dme2.cache.domain.CacheTypeNames})
	 * @param source instance of registry which is implementing ({@link DME2CacheableCallback})  
	 * @return
	 */
	public DME2Cache createCache(String name, String cacheType, DME2CacheableCallback source);
	public DME2Cache createCache(final CacheConfiguration cacheConfig);
	public DME2CacheJMXBean getCacheBean(String name);
	//public void clearCache(String name);
	//public void persistCache(String name);
	/**
	 * request to shutdown all the caches
	 */
	public void shutdown();
	//public void persist();
	public DME2Cache getCache(String cacheName);
	public boolean isCacheContainerRunning();
}