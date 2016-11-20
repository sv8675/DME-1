/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.handler.cacheabledata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;

import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

@SuppressWarnings("unchecked")
public class EndpointCacheDataHandler<K,V> extends AbstractCacheDataHandler<K,V>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointCacheDataHandler.class.getName());
	
	public EndpointCacheDataHandler()
	{
		super();
	}

	@Override
	public Map<Key<K>,Pair<CacheElement, Exception>> getFreshDataToReloadAllEntries(Set<Key> keySet, DME2CacheableCallback source)
	{
		LOGGER.debug(null, "getFreshDataToReloadAllEntries", "retreving data by endpointdatahandler for all keys");
		Map<Key<K>,Pair<CacheElement, Exception>> dataMap = new TreeMap<>();
		Set<Key> keysToFetchData = new HashSet<Key>();

		if(keySet!=null)
		{
			List<Key> keys = new ArrayList<Key>();
			
			for(Key key : keySet)
			{
				keys.add(key);
			}
	
			// CSSA-11214 - randomize the ep list being refreshed everytime.
			Collections.shuffle(keys);			
			LOGGER.debug(null, "getFreshDataToReloadAllEntries", LogMessage.REFRESH_ENDPOINTS, new Date());
			
			for(Key key: keys)
			{
				keysToFetchData.add(key);
			}
			
			dataMap = source.fetchFromSource( keysToFetchData );
	
			LOGGER.info(null, "getFreshDataToReloadAllEntries", "end");
		}
		return dataMap;
	}

	@SuppressWarnings({ "rawtypes" })
	public CacheElement getFreshDataToReloadCacheEntry(Key key, DME2CacheableCallback source)
	{
		LOGGER.info(null, "EndpointCacheDataHandler.getFreshDataToReloadCacheEntry()", "start: [{}]", key);
		long start = System.currentTimeMillis();
		CacheElement returnValue = null;

		try
		{
			if(key!=null && source!=null)
			{
				returnValue = source.fetchFromSource(key);
			}
			LOGGER.info(null, "EndpointCacheDataHandler.getFreshDataToReloadCacheEntry", "serviceUri [{}]; return value [{}]", key!=null?key:"null",returnValue!=null?returnValue:"null");
		}catch (Exception e){
			LOGGER.error(null, null, "EndpointCacheDataHandler.getFreshDataToReloadCacheEntry", "exception while retreving data by endpointdatahandler for serviceUri [{}]; time taken [{}]",key,System.currentTimeMillis()-start);
			//throw new CacheException(ErrorCatalogue.CACHE_006, key, e.getMessage());
		}

		LOGGER.info(null, "EndpointCacheDataHandler.getFreshDataToReloadCacheEntry", "retreving data by datahandler for serviceUri [{}]; time taken [{}]", key,System.currentTimeMillis()-start);
		return returnValue;
	}
}