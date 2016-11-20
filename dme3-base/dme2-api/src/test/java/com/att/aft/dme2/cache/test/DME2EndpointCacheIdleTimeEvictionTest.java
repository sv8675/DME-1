/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2EndpointCacheIdleTimeEvictionTest
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2EndpointCacheIdleTimeEvictionTest.class.getName());
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test"+System.currentTimeMillis();
	private static DME2Configuration config = null;
	private static DME2Cache cache = null;
	private static Value<String> idleValue = new Value<String>("IdleValue");
	private static Key<String> idleKey = new Key<String>("IdleKey");
	ExecutorService executorService = Executors.newFixedThreadPool(1);
	int keySize = 3;

	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig() throws DME2Exception{
		String defaultPropFile = "dme-api_defaultConfigs.properties";
		String overridePropFile = "override_configs_endpoint_cache_idletimeout_test.properties";
		config = new DME2Configuration("DME2EndpointCacheIdleTimeEvictionTest",defaultPropFile,overridePropFile);
		cache = DME2CacheFactory.getCacheManager(config).createCache(cacheName, cacheType, null);
	}

	@Test
	public void endpointCacheIdleTimeoutTest() 
	{
		LOGGER.debug(null,"endpointCacheIdleTimeoutTest", "starting test");
		
		//setting up the data
		for(int i=1;i<=keySize;i++)
		{
			Key<String> k = new Key<String>("testDataKey"+i); 
			Value<String> v = new Value<String>("testDataValue"+i);
			cache.put(k, v);
		}
		cache.put(idleKey, idleValue); 
		keySize++;//to account for the additional entry outside loop
		
		//validate - check the data as being setup is at the state before refresh
		Assert.assertEquals(keySize, cache.getCurrentSize());
		Assert.assertEquals(idleValue, cache.get(idleKey));
		
		//keep this running so that it ensures all the keys except the idleKey are being touched
		//so that when the idle remover service kicks in, all keys are returned as not idle except the "idleKey"
		executorService.execute(new Runnable() {
		    public void run() {
		    	while(true){
					for(int i=1;i<=keySize-1;i++)
					{
						Key<String> k = new Key<String>("testDataKey"+i); 
						cache.get(k);
					}
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		    	}
		    }
		});

		//sleep - so as to make sure the idle checker kicks in and removes the idle entry
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		executorService.shutdown();

		//validate 
		//refresh will occur but no data would be removed as per the test case 
		Assert.assertEquals(keySize-1, cache.getCurrentSize());
		//refresh will modify only the value of key as specified in the test
		Assert.assertEquals(null, cache.get(idleKey));
		
		LOGGER.debug(null,"endpointCacheIdleTimeoutTest", "ending test");
	}
}
