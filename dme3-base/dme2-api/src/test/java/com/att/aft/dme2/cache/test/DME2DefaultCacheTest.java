/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import org.junit.BeforeClass;

import com.att.aft.dme2.config.DME2Configuration;

/**
 * 
 * validate the default cache for all basic cache features
 *
 */

public class DME2DefaultCacheTest extends DME2AbstractCommonCacheTest
{
	private static String cacheType = "RouteInfoCache";
	private static String cacheName = "RouteInfoCache-Test"+System.currentTimeMillis();
	protected static DME2Configuration config = null;

	public DME2DefaultCacheTest() 
	{
		super(cacheType, cacheName, config);
	}
	
	//load the custom properties to override with the default implementation  
	@BeforeClass public static void setupConfig(){
		String defaultPropFile = "dme-api_defaultConfigs.properties";
		String overridePropFile = "override_configs_defaultCache_persistence.properties";
		config = new DME2Configuration("DME2DefaultCacheTest",defaultPropFile,overridePropFile);
	}
}
