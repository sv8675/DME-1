/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.handler.service;

import com.att.aft.dme2.cache.domain.CacheEvent;
import com.att.aft.dme2.cache.service.DME2Cache;

public interface CacheEventHandler
{
	public void onPut(DME2Cache cache, CacheEvent cacheEvent);
	public void onGet(DME2Cache cache, CacheEvent cacheEvent);
	public void onUpdate(DME2Cache cache, CacheEvent cacheEvent);
	public void onRemove(DME2Cache cache, CacheEvent cacheEvent);
	public void onBeforeRemove(DME2Cache cache, CacheEvent cacheEvent) throws Exception;
	public void onEviction(DME2Cache cache, CacheEvent cacheEvent);
	public void onRefresh(DME2Cache cache, CacheEvent cacheEvent);
}
