/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.handler.cacheabledata;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.handler.service.CacheableDataHandler;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public abstract class AbstractCacheDataHandler<K, V> implements	CacheableDataHandler<K, V> 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheDataHandler.class.getName());

	public AbstractCacheDataHandler() {
	}
	
	@SuppressWarnings({ "rawtypes" })
	@Override
	public  Map<Key<K>,Pair<CacheElement, Exception>> getDataForAllKeys(Set<Key> keySet, DME2CacheableCallback source)
	{
		LOGGER.debug(null, "AbstractCacheDataHandler.getDataForAllKeys", "start");
		long start = System.currentTimeMillis();
		 Map<Key<K>,Pair<CacheElement, Exception>> dataMap = getFreshDataToReloadAllEntries(keySet, source);
		
	//	DME2CacheFactory.getRegistryDelegator().touchRegistryUpdateTime();
		
		LOGGER.debug(null, "AbstractCacheDataHandler.getDataForAllKeys", "end - retreving data by endpointdatahandler for all serviceUris; time taken [{}]",System.currentTimeMillis()-start);

		return dataMap;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public CacheElement getData(Key<K> key, DME2CacheableCallback source) throws CacheException
	{
		LOGGER.debug(null, "AbstractCacheDataHandler.getData", "start");
		long start = System.currentTimeMillis();
		CacheElement data = getFreshDataToReloadCacheEntry(key, source);
		LOGGER.debug(null, "AbstractCacheDataHandler.getDataForAllKeys", "end - retreving data by endpointdatahandler for all serviceUris; time taken [{}]",System.currentTimeMillis()-start);
		return data;
	}

	@SuppressWarnings("rawtypes")
	public abstract CacheElement getFreshDataToReloadCacheEntry(Key key, DME2CacheableCallback source) throws CacheException;
	public abstract Map<Key<K>,Pair<CacheElement, Exception>> getFreshDataToReloadAllEntries(Set<Key> keySet, DME2CacheableCallback source) throws CacheException;
}
