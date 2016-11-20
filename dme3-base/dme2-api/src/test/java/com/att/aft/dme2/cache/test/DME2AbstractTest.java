/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.hz.HzCache;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheManager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public abstract class DME2AbstractTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2AbstractTest.class.getName());
	private static DME2CacheManager cacheManager = null;
	private int SIZE = 2;
	private static DME2Configuration configuration = null;
	
	public DME2AbstractTest(String cacheName) 
	{
		createCacheManager();
		//setUpCacheTestData(cacheManager, cacheName);
	}
	public DME2AbstractTest(DME2Configuration config) 
	{
		DME2AbstractTest.configuration = config;
		createCacheManager();
	}

	protected DME2CacheManager getCacheManager(){
		return cacheManager;
	}
	
	@BeforeClass
	public static void createCacheManager()
	{
		try {
			if(configuration!=null){
				cacheManager = DME2CacheFactory.getCacheManager(configuration);
			}
		} catch (DME2Exception e) {
			e.printStackTrace();
		}
		LOGGER.info(null, "DME2AbstractTest", "cacheManager: [{}]", cacheManager);
	}
	
	protected int getActualCacheSize()
	{
		return SIZE;
	}
	
	public void setUpCacheTestData(final DME2CacheManager cacheManager, final String cacheName)
	{
		DME2Cache cache = cacheManager.getCache(cacheName);

		LOGGER.debug(null, "DME2AbstractTest.setUpCacheTestData", "got test cache: [{}]",cache); 
		
		for(int i=1;i<=SIZE;i++)
		{
			Key<String> k = new Key<String>("testDataKey"+i); 
			Value<String> v = new Value<String>("testDataValue"+i);
			//cache.put(k, v);
		}
	}
	
	@AfterClass public static void tearDown(){
		if(cacheManager!=null){
			cacheManager.shutdown();
		}
		HzCache.shutdown();
	}
}
