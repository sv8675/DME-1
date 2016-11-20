/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * test whether if ttl is provided then are the entries removed if no source for refreshing the data is provided
 */
package com.att.aft.dme2.cache.test;

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

public class DME2EndpointCacheTTLEvictionTest  
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2EndpointCacheTTLEvictionTest.class.getName());
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test"+System.currentTimeMillis();
	protected static DME2Configuration config = null;
	protected static DME2Cache cache = null;
	
	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig() throws DME2Exception{
		LOGGER.debug(null,"setupConfig", "DME2EndpointCacheTTLEvictionTest setupConfig - start");
		String defaultPropFile = "dme-api_defaultConfigs.properties";
		String overridePropFile = "override_configs_endpoint_cache_ttl_eviction_test.properties";
		config = new DME2Configuration("DME2EndpointCacheTTLEvictionTest",defaultPropFile,overridePropFile);
		cache = DME2CacheFactory.getCacheManager(config).createCache(cacheName, cacheType, null);
	}

	@Test
	public void endpointCacheTtlEvictionTest() 
	{
		LOGGER.debug(null,"endpointCacheTtlEvictionTest", "starting test");
		int keySize = 3;
		
		for(int i=1;i<=keySize;i++)
		{
			Key<String> k = new Key<String>("testDataKey"+i); 
			Value<String> v = new Value<String>("testDataValue"+i);
			cache.put(k, v);
		}
		
		Assert.assertEquals(keySize, cache.getCurrentSize());

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//refresh will occur but no data would be removed as no data source has been provided 
		Assert.assertEquals(keySize, cache.getCurrentSize());
		
		LOGGER.debug(null,"endpointCacheTtlEvictionTest", "ending test");
	}
}
