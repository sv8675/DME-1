/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import com.att.aft.dme2.config.DME2Configuration;

/**
 * verifying the features of the endpoint cache
 */
public class DME2EndpointCacheTest extends DME2AbstractCommonCacheTest
{
	private static String cacheType = "EndpointCache";
	private static String cacheName = "EndpointCache-Test"+System.currentTimeMillis();
	protected static final DME2Configuration config = new DME2Configuration("DME2EndpointCacheTest");
	
	public DME2EndpointCacheTest() 
	{
		super(cacheType, cacheName, config);
	}
}