/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import java.io.File;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * verifying the features of the cache persistence
 */
public class DME2DefaultCachePersistenceTest extends DME2AbstractCommonCacheTest
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2DefaultCachePersistenceTest.class.getName());
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test-DME2DefaultCachePersistenceTest";
	protected static DME2Configuration config = null;
	
	public DME2DefaultCachePersistenceTest() 
	{
		
		super(cacheType, cacheName, config);
	}
	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig(){
		String defaultPropFile = "dme-api_defaultConfigs.properties";
		String overridePropFile = "override_configs_defaultCache_persistence_test.properties";
		config = new DME2Configuration("DME2DefaultCachePersistenceTest",defaultPropFile,overridePropFile);
	}
	/**
	 * verify the persistence of cache 
	 */
	@SuppressWarnings({ "unchecked", "resource" })
	@Test
	public void cachePersistenceTest() 
	{
		sop(null, "cacheRemoveKeyTest", "starting test [cacheStartTest] with cache:"+cache);
		//record
		Key<String> keyToRemove = null;
		int noOfKeys = 5;
		
		for(int i=1;i<=noOfKeys;i++)
		{
			Key<String> k = new Key<String>("testKey"+i); 
			if(i==1) keyToRemove = k;
			Value<String> v = new Value<String>("testValue"+i);
			
			cache.put(k, v);
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String user_home = System.getProperty( "user.home" );
		
		String testCachePersistentFileName = "/tmp/EndpointCache-Test-DME2DefaultCachePersistenceCountTest.aft.cached.ser";
		Scanner persistFileScanner=null;
		File persistFile = null;
		int counter = 0;
		try {
			persistFile = new File(testCachePersistentFileName);
			
			if(!(persistFile.isFile() && persistFile.exists())){
				persistFile = new File( this.getClass().getResource( testCachePersistentFileName ).getFile() );
			}
			persistFileScanner = new Scanner(persistFile).useDelimiter("[^a-zA-Z]+");
			while (persistFileScanner.hasNext()){
		        String word = persistFileScanner.next();
		        if(word.equalsIgnoreCase("lastAccessedTime")){
		        	counter++;
		        }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//checking whether the key is removed
		sop(null, "cacheRemoveKeyTest", "START: validating cache data");
		Value<String> v = null;
		try{
			v = cache.get(keyToRemove);
		} catch (RuntimeException ce) 
		{
			sop(null, "cacheRemoveKeyTest", "warning during cache get [{}]", ce.getMessage());
		}
		Assert.assertNotNull(getCacheManager());
		Assert.assertEquals(counter, cache.getCurrentSize());
		
		sop(null, "cacheRemoveKeyTest", "ending test [cacheRemoveKeyTest]");
	}
}