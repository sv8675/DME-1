/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

public class DME2CustomCacheTest 
{/*
	private static DME2CacheManager cacheManager = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2CustomCacheTest.class);
	public DME2CustomCacheTest() 
	{
		
	}
	@Before
	public void setUp()
	{
		cacheManager = DME2CacheFactory.getCacheManager();
		int i=0;
		while(!cacheManager.isCacheContainerRunning() && i<=6)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			++i;
		}
		Assert.assertTrue(cacheManager.isCacheContainerRunning());
	}


	public void defaultCacheStartTest() 
	{
		LOGGER.debug(null, "defaultCacheStartTest","starting test [defaultCacheStartTest]");
		
		String cacheName = "Custom-Cache";
		DME2Cache cache = cacheManager.getCache(cacheName);
		LOGGER.debug(null, "defaultCacheStartTest","got test cache: [{}]",cache); 
		Key<String> keyToRemove = null;
		
		for(int i=1;i<=5;i++)
		{
			Key<String> k = new Key<String>("testKey"+i); 
			if(i==1) keyToRemove = k;
			Value<String> v = new Value<String>("testValue"+i);
			
			cache.put(k, v);
		}
		LOGGER.debug(null, "defaultCacheStartTest","START: validating cache data");

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Value<String> v = null;
		try{
			v = cache.get(keyToRemove);
		} catch (RuntimeException ce) 
		{
			LOGGER.debug(null, "defaultCacheStartTest","warning during cache get [{}]", ce.getMessage());
		}
		LOGGER.debug(null, "defaultCacheStartTest","got removed Value = [{}]", v);
		
		LOGGER.debug(null, "defaultCacheStartTest","tried retreiving removed Value");
		
		boolean success = true;
		for(int i=1;i<=1;i++)
		{
			Key<String> k = new Key<String>("testKey"+i);
		}	
		Assert.assertTrue(success);
		LOGGER.debug(null, "defaultCacheStartTest","END: validating cache data");
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Assert.assertNotNull(cacheManager);
		LOGGER.debug(null, "defaultCacheStartTest","ending test [defaultCacheStartTest]");
	}
	
	@After
	public void tearDown()
	{
		cacheManager.shutdown();
	}
*/}
