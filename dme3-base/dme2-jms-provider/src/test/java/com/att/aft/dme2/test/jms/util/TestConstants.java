/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSConstants;

public class TestConstants
{
	public static final String jndiClass = "com.att.aft.dme2.jms.DME2JMSInitialContextFactory";
	public static final String jndiUrl = "qcf://dme2";
	public static final String clientConn = "qcf://dme2";
	
	public static final String serviceToRegister = "http://DME2LOCAL/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
	public static final String authServiceToRegister = "http://DME2LOCAL/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?realm=myrealm&loginMethod=BASIC&allowedRoles=myclientrole";
	public static final String dme2SearchStr= "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD";
	public static final String dme2ResolveStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
	
	public static final String dataContext = "205977";
	public static final String partner = "TEST";
	public static final int listenerCount = 10;
	// TODO Check service URIs
	public static final String serviceRcvToRegister = "http://DME2LOCAL/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?server=true";
	public static final String failserviceToRegister = "http://DME2LOCAL/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE?server=true";
	public static final String dme2FailSvcSearchStr= "http://DME2SEARCH/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV";
	public static final String dme2FailSvcResolveStr = "http://DME2RESOLVE/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE";
	public static final String dme2ConnTimeoutResolveStr = "http://DME2RESOLVE/service=com.att.test.FastFailService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE?connectTimeOutInMs=1";
	public static final String remoteMsgSelectorService = "http://DME2LOCAL/service=com.att.test.RemoteMsgSelector/version=1.1.0/envContext=DEV/routeOffer=BAU_SE?server=true";;
	public static final String remoteMsgSelectorSearchStr= "http://DME2SEARCH/service=com.att.test.RemoteMsgSelector/version=1.1.0/envContext=DEV";
	public static final String remoteMsgSelectorResolveStr = "http://DME2RESOLVE/service=com.att.test.RemoteMsgSelector/version=1.1.0/envContext=DEV/routeOffer=BAU_SE";
	public static final String longRunServiceToRegister = "http://DME2LOCAL/service=com.att.test.LongRunService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE?server=true";
	public static final String longRunServiceToRegisterAFT = "http://DME2LOCAL/service=com.att.test.LongRunService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE?server=true";
	public static final String longRunServiceSearchStr= "http://DME2SEARCH/service=com.att.test.LongRunService/version=1.0.0/envContext=DEV";
	public static final String longRunServiceSearchStrAFT= "http://DME2SEARCH/service=com.att.test.LongRunService/version=1.0.0/envContext=DEV";
	public static final String longRunServiceResolveStr = "http://DME2RESOLVE/service=com.att.test.LongRunService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
	public static final String continuationReplyServiceToRegister = "http://DME2LOCAL/service=com.att.test.ContinuationQueueService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE?server=true";
	public static final String continuationReplyServiceSearchStr = "http://DME2RESOLVE/service=com.att.test.ContinuationQueueService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";;
	public static final String emptyReplyServiceToRegister = "http://DME2LOCAL/service=com.att.test.TestEmptyReplyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE?server=true";
	public static final String emptyReplyServiceSearchStr= "http://DME2SEARCH/service=com.att.test.TestEmptyReplyService/version=1.0.0/envContext=DEV";
	public static final String emptyReplyServiceResolveStr = "http://DME2RESOLVE/service=com.att.test.TestEmptyReplyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
	
	// TODO Change URIs
	static final String SANDBOX_LAB_GRM_LWP_DIRECT_HTTP_URLS = "";
	static final String SANDBOX_LAB_GRM_LWP_DIRECT_HTTPS_URLS = "";

	static final String SANDBOX_DEV_GRM_LWP_DIRECT_HTTP_URLS = "";
	static final String SANDBOX_DEV_GRM_LWP_DIRECT_HTTPS_URLS = "";
	
	static final String SCLD_PLATFORM_FOR_SANDBOX_LAB = "SANDBOX-LAB";
	static final String SCLD_PLATFORM_FOR_SANDBOX_DEV = "SANDBOX-DEV";
	
	static final String SANDBOX_DEV_FAILOVER_GRM_LWP_DIRECT_HTTP_URLS = "";
	static final String SANDBOX_LAB_FAILOVER_GRM_LWP_DIRECT_HTTP_URLS = "";
	
	// Modify the following constants based on which GRM env to be used
	public final static String GRM_PLATFORM_TO_USE = SCLD_PLATFORM_FOR_SANDBOX_DEV;
	public final static String GRM_LWP_DIRECT_HTTP_URLS_TO_USE = SANDBOX_DEV_GRM_LWP_DIRECT_HTTP_URLS;
	public final static String GRM_LWP_DIRECT_HTTPS_URLS_TO_USE = SANDBOX_DEV_GRM_LWP_DIRECT_HTTP_URLS;
	public final static String GRM_LWP_FAILOVER_URLS_TO_USE = SANDBOX_DEV_FAILOVER_GRM_LWP_DIRECT_HTTP_URLS;	
	
	public final static String JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME = "com.att.aft.TestJMSExchangePreferredRouteOffer";
	public final static String JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME_ARINDAM = "com.att.aft.TestJMSExchangePreferredRouteOfferArindam";

  public static String jmsHeaderServiceToRegister ="http://DME2LOCAL/service=com.att.test.TestJMSHeaderReplyService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE?server=true";;
  public static String jmsHeaderServiceSearchStr ="http://DME2SEARCH/service=com.att.test.TestJMSHeaderReplyService/version=1.1.0/envContext=DEV";
  public static String jmsHeaderServiceResolveStr ="http://DME2RESOLVE/service=com.att.test.TestJMSHeaderReplyService/version=1.1.0/envContext=DEV/routeOffer=BAU_SE";

  public static void removePortCache() {
		List<String> defaultConfigs = new ArrayList<String>();
		defaultConfigs.add(JMSConstants.JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME);
		defaultConfigs.add(JMSConstants.DME_API_DEFAULT_CONFIG_FILE_NAME);
//		defaultConfigs.add(JMSConstants.METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME);		
		DME2Configuration config = new DME2Configuration("port-cache", defaultConfigs, null, null);
		
		String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE", System.getProperty("user.home") + "/.aft/.dme2PortCache");
		try {
			File file = new File(portCacheFilePath);
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception e) {

		}
	}
}
