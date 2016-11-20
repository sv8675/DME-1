/**
 * 
 */
package com.att.aft.dme2.cache.test;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;
import org.junit.BeforeClass;
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
import com.att.aft.dme2.mbean.DME2CacheMXBean;

public class DME2CacheJMXBeanTest
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2CacheJMXBeanTest.class.getName());
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test"+System.currentTimeMillis();
	private static DME2Configuration config = null;
	private static DME2Cache cache = null;
	
	
	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig() throws DME2Exception{
		config = new DME2Configuration("DME2CacheJMXBeanTest");
		cache = DME2CacheFactory.getCacheManager(config).createCache(cacheName, cacheType, null); //setting source as null so that no refresh occurs
	}

	@Test
	public void endpointCacheMXBeanTest() 
	{
		LOGGER.debug(null,"endpointCacheMXBeanTest", "starting test");
		
		//setting up the data
		int indexRangeMin = 1;
		int indexRangeMax = 20;
		int randIndex = 1;
		String keyGeneratePrefix = "testDataKey"; 
		String dataGeneratePrefix = "testDataValue"; 
		
		for(int i=indexRangeMin;i<=indexRangeMax;i++)
		{
			Key<String> k = new Key<String>(keyGeneratePrefix + i); 
			Value<String> v = new Value<String>(dataGeneratePrefix + i);
			cache.put(k, v);
		}

		DME2CacheMXBean caheMXBean = (DME2CacheMXBean)cache;
		
		randIndex = ThreadLocalRandom.current().nextInt(indexRangeMin, indexRangeMax + 1);
		Key<String> keyTtlCheck = new Key<String>(keyGeneratePrefix + randIndex);
		CacheElement checkTtl = cache.getEntryView().getEntry(keyTtlCheck);
		
		randIndex = ThreadLocalRandom.current().nextInt(indexRangeMin, indexRangeMax + 1);
		Key<String> keyExpTimeCheck = new Key<String>(keyGeneratePrefix + randIndex);
		CacheElement checkExpTime = cache.getEntryView().getEntry(keyExpTimeCheck);
		// This needs to be cleaned up as its mocking the JMX call not actually doing a JMX call.
		Assert.assertEquals(indexRangeMax, caheMXBean.getCurrentSize());
		Assert.assertEquals(checkTtl.getTtl(), caheMXBean.getCacheTTLValue(keyExpTimeCheck.getString()));
		Assert.assertEquals(checkExpTime.getExpirationTime(), caheMXBean.getExpirationTime(keyExpTimeCheck.getString()));
		for(int i=indexRangeMin;i<=indexRangeMax;i++){
			Key<String> k = new Key<String>(keyGeneratePrefix + i);
			Assert.assertTrue(cache.getKeys().contains(k.getString()));
		}
		caheMXBean.disableCacheStats();
		Assert.assertEquals(false, caheMXBean.isCacheStatsEnabled());
		caheMXBean.enableCacheStats();
		Assert.assertEquals(true, caheMXBean.isCacheStatsEnabled());
		
		LOGGER.debug(null,"endpointCacheMXBeanTest", "ending test");
	}
}