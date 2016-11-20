/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.test;

import com.att.aft.dme2.config.DME2Configuration;

public class DME2RouteOfferCacheTest extends DME2AbstractCommonCacheTest
{
	private static String cacheType = "RouteInfoCache";
	private static String cacheName = "RouteOfferCache-Test"+System.currentTimeMillis();
	protected static final DME2Configuration config = new DME2Configuration("DME2RouteOfferCacheTest");

	public DME2RouteOfferCacheTest() 
	{
		super(cacheType, cacheName, config);
	}
}
