/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.config.DME2Configuration;

/**
 * verifying the features of the cache persistence
 */
public class DME2CacheWarmupTest extends DME2AbstractCommonCacheTest
{
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test-DME2CacheWarmupTest";
	protected static DME2Configuration config = null;
	
	public DME2CacheWarmupTest() 
	{
		
		super(cacheType, cacheName, config);
	}
	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig(){
		String defaultPropFile = "dme-api_defaultConfigs.properties";
		String overridePropFile = "dme2_defaultConfigs_override_cache_warmup.properties";
		config = new DME2Configuration("DME2CacheWarmupTest",defaultPropFile,overridePropFile);
		//config = new DME2Configuration();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void cacheWarmupTest() 
	{
		sop(null, "cacheWarmupTest", "starting test [cacheStartTest] with cache:"+cache);
		//record
		Key<String> keyToRemove = null;
		int noOfKeys = 5;
		
		Assert.assertNotNull(getCacheManager());
		Assert.assertEquals(noOfKeys, cache.getCurrentSize());
		sop(null, "cacheRemoveKeyTest", "ending test [cacheRemoveKeyTest]");
	}

}