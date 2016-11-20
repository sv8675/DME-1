/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import java.io.File;
import java.io.FileNotFoundException;
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
public class DME2CachePersistenceTest extends DME2AbstractCommonCacheTest
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2CachePersistenceTest.class.getName());
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test-DME2CachePersistenceTest";
	protected static DME2Configuration config = null;
	
	public DME2CachePersistenceTest() 
	{
		
		super(cacheType, cacheName, config);
	}
	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig(){
		String defaultPropFile = "dme-api_defaultConfigs.properties";
		String overridePropFile = "dme2_defaultConfigs_override_cache_persistence.properties";
		config = new DME2Configuration("DME2CachePersistenceTest",defaultPropFile,overridePropFile);
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
		
		String testCachePersistentFileName = "/tmp/EndpointCache-Test-DME2CachePersistenceTest.aft.cached.ser";
		Scanner file=null;
		int counter = 0;
		try {
			File f = new File( testCachePersistentFileName );
			if(f!=null && !f.exists()){
				f = new File( this.getClass().getResource( testCachePersistentFileName ).getFile() );
			}
			file = new Scanner(f).useDelimiter("[^a-zA-Z]+");
			while (file.hasNext()){
		        String word = file.next();
		        if(word.equalsIgnoreCase("lastAccessedTime")){
		        	counter++;
		        }
			}
		} catch (FileNotFoundException e) {
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