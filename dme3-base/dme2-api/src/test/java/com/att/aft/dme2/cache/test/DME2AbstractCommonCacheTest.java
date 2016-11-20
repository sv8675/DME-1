/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * verifying the features of the endpoint cache
 */

public abstract class DME2AbstractCommonCacheTest extends DME2AbstractTest
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2AbstractCommonCacheTest.class.getName());
	private String cacheType;
	private String cacheName;
	protected static DME2Cache cache = null;
	
	public DME2AbstractCommonCacheTest(final String cacheType, final String cacheName, final DME2Configuration config) 
	{
		super(config);
		this.cacheType = cacheType;
		this.cacheName = cacheName;
	}
	
	protected void sop(String ignore, String name, String partmsg){
		sop(ignore,name,partmsg,null);
	}
	protected void sop(String ignore, String name, String partmsg, Object obj1){
		sop(ignore,name,partmsg,obj1,null);
	}
	protected void sop(String ignore, String name, String partmsg, Object obj1, Object obj2){
		//System.out.printf("%s %s %s %s\n",name,partmsg,obj1, obj2);
		LOGGER.debug(null, name, partmsg, obj1, obj2);
	}
	
	@Before
	public void setup(){
		sop(null, "setup", "cacheManager:{[]}",getCacheManager());
		if(cache==null){
			cache = getCacheManager().createCache(cacheName, cacheType, null);
		}
	}

	/**
	 * verify the cache started and elements are successfully put in cache
	 */
	@Test
	public void cacheStartTest() 
	{
		sop(null, "cacheStartTest", "starting test [cacheStartTest] with cache:"+cache);

		int noOfKeys = 5;
		for(int i=1;i<=noOfKeys;i++)
		{
			Key<String> k = new Key<String>("testKey"+i); 
			Value<String> v = new Value<String>("testValue"+i);
			
			cache.put(k, v);
		}
		sop(null, "cacheStartTest", "START: validating cache data");
		Assert.assertNotNull(getCacheManager());
		Assert.assertEquals(noOfKeys, cache.getKeySet().size());
		Assert.assertEquals(noOfKeys, cache.getCurrentSize());
		sop(null, "cacheStartTest", "END: validating cache data");

		sop(null, "cacheStartTest", "ending test [cacheStartTest]");
	}
	
	/**
	 * verify the removal of a specified key 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void cacheRemoveKeyTest() 
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
		
		//removing a key
		cache.remove(keyToRemove);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
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
		Assert.assertEquals(null,v);
		Assert.assertNotNull(getCacheManager());
		Assert.assertEquals(noOfKeys-1, cache.getCurrentSize());//-1 since 1 key was removed
		sop(null, "cacheRemoveKeyTest", "ending test [cacheRemoveKeyTest]");
	}

	/**
	 * verify the refresh of a specified key when source not available 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void cacheRefreshKeyOnDataRetreiveFailureTest() 
	{
		sop(null, "cacheRefreshKeyOnDataRetreiveFailureTest", "starting test [cacheStartTest] with cache:"+cache);
		//record
		Key<String> keyToRefresh = null;
		int noOfKeys = 5;
		
		for(int i=1;i<=noOfKeys;i++)
		{
			Key<String> k = new Key<String>("testKey"+i); 
			if(i==1) keyToRefresh = k;
			Value<String> v = new Value<String>("testValue"+i);
			
			cache.put(k, v);
		}
		
		//removing a key
		cache.refreshEntry(keyToRefresh);

		//checking whether the key is removed
		sop(null, "cacheRefreshKeyOnDataRetreiveFailureTest", "START: validating cache data");
		Value<String> v = null;
		try{
			v = cache.get(keyToRefresh);
		} catch (RuntimeException ce) 
		{
			sop(null, "cacheRefreshKeyOnDataRetreiveFailureTest", "warning during cache get [{}]", ce.getMessage());
		}
		Assert.assertNotEquals(null,v);
		Assert.assertNotNull(getCacheManager());
		Assert.assertEquals(noOfKeys, cache.getCurrentSize());
		sop(null, "cacheRefreshKeyOnDataRetreiveFailureTest", "ending test [cacheRemoveKeyOnDataRetreiveFailureTest]");
	}

	@Test
	public void uniqueKeyTest() 
	{
		sop(null, "uniqueKeyTest", "starting test [uniqueKeyTest]");
		sop(null, "uniqueKeyTest", "cacheManager:{[]}",getCacheManager());

		sop(null, "uniqueKeyTest", "cache:{[]}",cache);
		sop(null, "uniqueKeyTest", "got test cache: [{}]",cache); 
		
		Key<String> oldInstanceKey = null;
		
		for(int i=1;i<=10;i++)
		{
			Key<String> k = new Key<String>("testKey"+i); 
			if(i==1) oldInstanceKey = k;
			Value<String> v = new Value<String>("testValue"+i);
			
			cache.put(k, v);
		}
		sop(null, "uniqueKeyTest", "START: validating cache data");
		
		Value<String> existingValueBeforeUpdate = cache.get(oldInstanceKey);
		
		Key<String> newKeyInstanceForExistingKey = new Key<String>("testKey"+1);
		Value<String> newValue = new Value<String>("testNewValue");
		
		cache.put(newKeyInstanceForExistingKey, newValue);
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Value<String> getValueWithOldInstanceKey = cache.get(oldInstanceKey);
		Value<String> getValueWithNewInstanceKey = cache.get(newKeyInstanceForExistingKey);
		
		sop(null, "uniqueKeyTest", "key: [{}], value returned:existingValueBeforeUpdate [{}]", oldInstanceKey, existingValueBeforeUpdate); 
		sop(null, "uniqueKeyTest", "key: [{}], value returned:getValueWithOldInstanceKey [{}]", oldInstanceKey, getValueWithOldInstanceKey);
		sop(null, "uniqueKeyTest", "key: [{}], value getValueWithNewInstanceKey [{}]", newKeyInstanceForExistingKey, getValueWithNewInstanceKey);
		sop(null, "uniqueKeyTest", "keys [{}]", cache.getKeys());
		
		for(Key key : cache.getKeySet()){
			sop(null, "uniqueKeyTest", "[{}]={}", key, cache.get(key));
		}
		
		assertEquals(getValueWithOldInstanceKey, getValueWithNewInstanceKey);
		
		sop(null, "uniqueKeyTest", "ending test [uniqueKeyTest]");
	}

	@After
	public void clear()
	{
		if(cache!=null){
			cache.clear();
		}
	}
	@AfterClass public static void tearDown(){
		cache=null;
	}
}