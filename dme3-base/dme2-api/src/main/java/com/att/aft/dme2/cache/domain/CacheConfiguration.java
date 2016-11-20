/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.domain;

import com.att.aft.dme2.cache.handler.service.CacheEventHandler;
import com.att.aft.dme2.cache.handler.service.CacheableDataHandler;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;

/**
 * @author ab850e
 *
 */
public class CacheConfiguration
{
	private CacheableDataHandler dataLoader;
	private CacheEventHandler eventHandler;
	//private long cacheTtl, cacheIdleTimeout, persistTimerInterval, refreshInterval, elementTtl;
	private String cacheName;
	//private TimeUnit timeunit; 
	//boolean persistEnableFlag;
	private CacheTypeElement cacheType;
	private DME2CacheableCallback source;
	
	public DME2CacheableCallback getCacheDataSource() {
		return source;
	}

	public CacheConfiguration setSource(DME2CacheableCallback source) {
		this.source = source;
		return this;
	}

	public CacheTypeElement getCacheType() {
		return cacheType;
	}

	public void setCacheType(CacheTypeElement cacheType) {
		this.cacheType = cacheType;
	}

	private CacheConfiguration()
	{
		init();
	}
	
	public static CacheConfiguration getInstance()
	{
		return new CacheConfiguration();
	}
	
	private void init()
	{
	}


	/**
	 * @return the eventHandler
	 */
	public CacheEventHandler getEventHandler() {
		return eventHandler;
	}

	/**
	 * @param eventHandler the eventHandler to set
	 */
	public void setEventHandler(CacheEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	public CacheConfiguration setDataLoader(CacheableDataHandler dataLoader)
	{
		this.dataLoader = dataLoader; 
		return this;
	}
	public CacheableDataHandler getDataLoader()
	{
		return this.dataLoader;
	}

	/**
	 * @return the cacheName
	 */
	public String getCacheName() {
		return cacheName;
	}

	/**
	 * @param cacheName the cacheName to set
	 */
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

}
