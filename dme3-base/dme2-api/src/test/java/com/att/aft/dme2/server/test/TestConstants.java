/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

/**
 * The Interface TestConstants.
 */
public interface TestConstants {

	/** The myservice_route offer1. */
	String myservice_routeOffer1 = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";

	/** The myservice_route offer2. */
	String myservice_routeOffer2 = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";

	/** The myservice_route offer3. */
	String myservice_routeOffer3 = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=WALMART_SE";
	
	static final String SANDBOX_LAB_GRM_LWP_DIRECT_HTTP_URLS = "";
	static final String SANDBOX_LAB_GRM_LWP_DIRECT_HTTPS_URLS = "";

	//static final String SANDBOX_DEV_GRM_LWP_DIRECT_HTTP_URLS = "";
	static final String SANDBOX_DEV_GRM_LWP_DIRECT_HTTP_URLS = "";
	static final String SANDBOX_DEV_GRM_LWP_DIRECT_HTTPS_URLS = "";
	
	static final String SANDBOX_DEV_GRM_REST_DIRECT_HTTP_URLS = "";

	static final String SCLD_PLATFORM_FOR_SANDBOX_LAB = "SANDBOX-LAB";
	static final String SCLD_PLATFORM_FOR_SANDBOX_DEV = "SANDBOX-DEV";
	
	static final String SANDBOX_DEV_FAILOVER_GRM_LWP_DIRECT_HTTP_URLS = "";
	static final String SANDBOX_LAB_FAILOVER_GRM_LWP_DIRECT_HTTP_URLS = "";
	
	// Modify the following constants based on which GRM env to be used
	//public final static String GRM_PLATFORM_TO_USE = SCLD_PLATFORM_FOR_SANDBOX_DEV;
	public final static String GRM_PLATFORM_TO_USE = SCLD_PLATFORM_FOR_SANDBOX_DEV;
  public final static String GRM_ENV_TO_USE = GRM_PLATFORM_TO_USE.equals( SCLD_PLATFORM_FOR_SANDBOX_DEV ) ? "DEV" : "LAB";
	public final static String GRM_LWP_DIRECT_HTTP_URLS_TO_USE = SANDBOX_LAB_GRM_LWP_DIRECT_HTTP_URLS;
	public final static String GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE = SANDBOX_DEV_GRM_LWP_DIRECT_HTTP_URLS;
	public final static String GRM_LWP_FAILOVER_URLS_TO_USE = SANDBOX_LAB_FAILOVER_GRM_LWP_DIRECT_HTTP_URLS;
  public final static String GRM_DNS_SERVER = "";


}
