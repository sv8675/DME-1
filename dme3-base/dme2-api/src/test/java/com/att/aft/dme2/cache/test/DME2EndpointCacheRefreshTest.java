/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2EndpointCacheRefreshTest 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2EndpointCacheRefreshTest.class.getName());
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test"+System.currentTimeMillis();
	private static DME2Configuration config = null;
	private static DME2Cache cache = null;
	private static DME2CacheableCallbackTest<Key<String>,Value<String>> refreshDataSourceTest = null;  
	private static Value<String> refreshedValue = new Value<String>("RefreshTestDataValue");
	private static CacheElement refreshedElement = new CacheElement( );
	private static Value<String> valueBeforeRefresh = new Value<String>("ValueBeforeRefresh");
	private static CacheElement elementBeforeRefresh = new CacheElement();
	private static Key<String> keyToRefresh = new Key<String>("RefreshKey");

	static {
		refreshedElement.setKey( keyToRefresh );
		refreshedElement.setValue( refreshedValue );
		elementBeforeRefresh.setValue( valueBeforeRefresh );
	}
	
	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig() throws DME2Exception{
		String defaultPropFile = "dme-api_defaultConfigs.properties";
		String overridePropFile = "override_configs_endpoint_cache_ttl_eviction_test.properties";
		config = new DME2Configuration("DME2EndpointCacheRefreshTest",defaultPropFile,overridePropFile);
		refreshDataSourceTest  = new DME2CacheableCallbackTest(keyToRefresh, refreshedElement );
		cache = DME2CacheFactory.getCacheManager(config).createCache(cacheName, cacheType, refreshDataSourceTest);
	}

	// Irrelevant now that cache uses registrycache for refreshing
	@Test
	@Ignore
	public void endpointCacheRefreshTest() 
	{
		LOGGER.debug(null,"endpointCacheRefreshTest", "starting test");
		
		//setting up the data
		int keySize = 3;
		for(int i=1;i<=keySize;i++)
		{
			Key<String> k = new Key<String>("testDataKey"+i); 
			Value<String> v = new Value<String>("testDataValue"+i);
			cache.put(k, v);
		}
		cache.put(keyToRefresh, valueBeforeRefresh); 
		keySize++;//to account for the additional entry outside loop
		
		//validate - check the data as being setup is at the state before refresh
		Assert.assertEquals(keySize, cache.getCurrentSize());
		Assert.assertEquals(valueBeforeRefresh, cache.get(keyToRefresh));

		//sleep - so as to make sure the refresh kicks in
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//validate 
		//refresh will occur but no data would be removed as per the test case 
		Assert.assertEquals(keySize, cache.getCurrentSize());
		//refresh will modify only the value of key as specified in the test
		Assert.assertEquals(refreshedValue, cache.get(keyToRefresh));
		
		LOGGER.debug(null,"endpointCacheRefreshTest", "ending test");
	}
}