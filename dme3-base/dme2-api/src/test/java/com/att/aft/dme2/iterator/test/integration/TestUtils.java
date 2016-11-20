/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.integration;

import java.util.Properties;

import com.att.aft.dme2.server.test.TestConstants;

public class TestUtils
{

	public static Properties initAFTProperties()
	{

	    Properties props = new Properties();
		props.setProperty("DME2_EP_REGISTRY_CLASS", "DME2FS");
		props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		props.setProperty("AFT_LATITUDE", "33.373900");
		//props.setProperty("AFT_LATITUDE", Long.toString( RandomUtils.nextLong() ));
		props.setProperty("AFT_LONGITUDE", "-86.798300");
		//props.setProperty("AFT_LONGITUDE", Long.toString( RandomUtils.nextLong() ));
		
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
	    System.setProperty("DME2.DEBUG", "true");
	    System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
		return props;
	}
	
}
