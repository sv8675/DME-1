/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.util;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.att.aft.dme2.types.RouteOffer;

public class RouteOfferTestUtil {

 	public RouteOfferTestUtil() {
		// TODO Auto-generated constructor stub
	}

	public static RouteOffer createDefaultRouteOffer(){
	    
		String DEFAULT_NAME = RandomStringUtils.randomAlphanumeric( 20 );
	    int DEFAULT_SEQUENCE = RandomUtils.nextInt(10);
	    Boolean DEFAULT_ALLOW_DYNAMIC_STICKINESS = RandomUtils.nextBoolean();
	    Boolean DEFAULT_ACTIVE = RandomUtils.nextBoolean();
	    Long DEFAULT_STALENESS_IN_MIN = RandomUtils.nextLong();
	    String DEFAULT_VERSION_MAP_REF = RandomStringUtils.randomAlphanumeric( 10 );

	    RouteOffer defaultRouteOffer = new RouteOffer();
		
		defaultRouteOffer.setActive(DEFAULT_ACTIVE);
		defaultRouteOffer.setAllowDynamicStickiness(DEFAULT_ALLOW_DYNAMIC_STICKINESS);
		defaultRouteOffer.setName(DEFAULT_NAME);
		defaultRouteOffer.setSequence(DEFAULT_SEQUENCE);
		defaultRouteOffer.setStalenessInMins(DEFAULT_STALENESS_IN_MIN);
		defaultRouteOffer.setVersionMapRef(DEFAULT_VERSION_MAP_REF);
		
		return defaultRouteOffer;
	}
}
