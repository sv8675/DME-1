/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.util;

import org.apache.commons.lang3.RandomStringUtils;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;

public class DME2RouteofferHolderTestUtil {

	private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 30 );
	private static final String DEFAULT_SERVICE_VERSION = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_FQ_NAME = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_SEARCH_FILTER = RandomStringUtils.randomAlphanumeric( 15 );

	public DME2RouteofferHolderTestUtil() {
		// TODO Auto-generated constructor stub
	}

	public static DME2RouteOffer getRouteOfferHolder(DME2Manager manager){

		DME2RouteOffer routeOfferHolder = new DME2RouteOffer(DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, RouteOfferTestUtil.createDefaultRouteOffer(), DEFAULT_FQ_NAME, manager);
		
		routeOfferHolder = routeOfferHolder.withSearchFilter(DEFAULT_SEARCH_FILTER);

		return routeOfferHolder;
	}
	public static DME2RouteOffer getRouteOfferHolder(DME2Manager manager, final String searchFilter){

		DME2RouteOffer routeOfferHolder = new DME2RouteOffer(DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, RouteOfferTestUtil.createDefaultRouteOffer(), DEFAULT_FQ_NAME, manager);
		
		routeOfferHolder = routeOfferHolder.withSearchFilter(searchFilter);

		return routeOfferHolder;
	}
}