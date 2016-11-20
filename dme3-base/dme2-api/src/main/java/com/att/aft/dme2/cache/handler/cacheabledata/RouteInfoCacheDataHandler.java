/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.handler.cacheabledata;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;

import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

@SuppressWarnings("unchecked")
public class RouteInfoCacheDataHandler<K,V> extends AbstractCacheDataHandler<K,V>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RouteInfoCacheDataHandler.class.getName());
	//private static String DME_URI_PART = DME2Configuration.getStringProp(DME2Constants.Cache.DME_URI_PART);

	public RouteInfoCacheDataHandler()
	{
		super();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<Key<K>,Pair<CacheElement, Exception>> getFreshDataToReloadAllEntries(Set<Key> keySet, DME2CacheableCallback source)
	{
		LOGGER.debug(null, "RouteInfoCacheDataHandler.getFreshDataToReloadAllEntries", "start - retreving data by RouteInfoCacheDataHandler for all keys");
		Map<Key<K>,Pair<CacheElement, Exception>> registryCacheDataMap = new TreeMap<>();
		
		try{
			registryCacheDataMap = source.fetchFromSource(keySet);
		}catch(Exception e){
			LOGGER.debug(null, "RouteInfoCacheDataHandler.getFreshDataToReloadAllEntries", "error - retreiving routeinfo cacheable data: {}", e.getMessage());
			throw e;
		}
		
		LOGGER.info(null, "EndpointCacheDataHandler.getFreshDataToReloadAllEntries", "end");
		return registryCacheDataMap;
	}

	@SuppressWarnings({ "rawtypes" })
	public CacheElement getFreshDataToReloadCacheEntry(Key key, DME2CacheableCallback source)
	{
		LOGGER.debug(null, "RouteInfoCacheDataHandler.getFreshDataToReloadCacheEntry", "start retreving data by RouteInfoCacheDataHandler serviceUri [{}]", key);
		long start = System.currentTimeMillis();

		CacheElement returnValue = null;

		/*String serviceName = null;
		String serviceVersion = null;
		String envContext = null;
		String routeOffer = null;*/

		try{
			/*String uriStr = DME_URI_PART + key.getString();
			DME2UniformResource uri = new DME2UniformResource(new URI(uriStr));

			serviceName = uri.getService();
			serviceVersion = uri.getVersion();
			envContext = uri.getEnvContext();
			routeOffer = uri.getRouteOffer();
			String serviceURI = DME2URIUtils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer);
			returnValue = new Value(DME2CacheFactory.getRegistryDelegator().getRouteInfo(serviceURI, serviceName, serviceVersion, envContext, routeOffer));*/

			if(key!=null && source!=null){
				returnValue = source.fetchFromSource(key); 
			}
			LOGGER.info(null, "getFreshDataToReloadCacheEntry", "serviceUri [{}]; return value [{}]", key!=null?key:"null",returnValue!=null?returnValue:"null");
		}catch (Exception e)
		{
			LOGGER.error(null, null, "RouteInfoCacheDataHandler.getFreshDataToReloadCacheEntry", "exception while retreving data by RouteInfoCacheDataHandler for serviceUri [{}]; time taken [{}]",key,System.currentTimeMillis()-start);
			//throw new CacheException(ErrorCatalogue.CACHE_006, key, e.getMessage());
		}

		LOGGER.debug(null, "RouteInfoCacheDataHandler.getFreshDataToReloadCacheEntry", "end - retrieving data by datahandler for serviceUri [{}]; time taken [{}]", key,System.currentTimeMillis()-start);
		return returnValue;
	}
}