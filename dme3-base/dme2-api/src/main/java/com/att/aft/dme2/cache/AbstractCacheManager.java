/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.att.aft.dme2.cache.domain.CacheTypeElement;
import com.att.aft.dme2.cache.domain.CacheTypes;
import com.att.aft.dme2.cache.service.CacheTaskScheduler;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheManager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public abstract class AbstractCacheManager implements DME2CacheManager 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheManager.class);
	protected final Map<String, CacheTaskScheduler> cacheManagerScheduleTaskRegister = new HashMap<String, CacheTaskScheduler>();
	//to check for duplicates cache keys
	protected static final Set<String> globalCacheRegister = new HashSet<String>();
	//track the caches created for this manager to be used to clean up global cache when this manager is shutdown
	protected final Map<String, DME2Cache> instanceCacheManagerRegister = new HashMap<String, DME2Cache>();
	
	protected DME2Configuration config = null;

	public AbstractCacheManager() 
	{
		//startAllCaches();
	}
	
	public AbstractCacheManager(DME2Configuration config) 
	{
		this.config = config;
		
	}
	
	protected DME2Configuration getConfig(){
		return this.config;
	}
	
	public boolean registerScheduledTask(CacheTaskScheduler schedule) 
	{
		if(schedule!=null)
		{
			cacheManagerScheduleTaskRegister.put(schedule.getTaskName().concat(":").concat(String.valueOf(System.currentTimeMillis())), schedule);
		}
		return true;
	}

	private void startAllCaches(final DME2Configuration config)
	{
		for (CacheTypeElement cacheTypeElement : CacheTypes.getCacheTypes(config)) 
		{
			//createCache(cacheTypeElement.getName());
		}
	}
	@Override
	public void shutdown() 
	{
		shutdownCacheManagerRegisteredTimers();
		shutdownCacheRegisteredTimers();
	}
	
	private void shutdownCacheManagerRegisteredTimers()
	{
		for(Entry<String, CacheTaskScheduler> entry: cacheManagerScheduleTaskRegister.entrySet())
		{
			entry.getValue().cancel();
		}
	}
	private void shutdownCacheRegisteredTimers()
	{
		for (Map.Entry<String,DME2Cache> cacheEntry : instanceCacheManagerRegister.entrySet())  
		{
			getCache(cacheEntry.getKey()).shutdownTimerTask();
			globalCacheRegister.remove(cacheEntry.getKey());
		}
		instanceCacheManagerRegister.clear();
	}
}