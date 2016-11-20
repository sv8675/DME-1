/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.net.URI;
import java.util.Properties;

import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.types.RouteInfo;

import junit.framework.TestCase;

@Ignore
public class TestDME2RouteInfoVersionSelectorNotMatch extends TestCase {
	public void setUp() throws Exception{
		//System.setProperty("AFT_DME2_GRM_URLS", "http://0.0.0.0:8001/GRMService/v1");
		System.setProperty("DME2.DEBUG","true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		RegistryFsSetup.init();
	}
	
	@Ignore  
	public void testVersionSelectorNotMatch() throws Exception{
		DME2Manager mgr = null;
		String serviceURI = "/service=com.att.dme2.test.TestVersionSelector_InputsDoNotMatch/version=7.0.0/envContext=LAB/routeOffer=SAMPLE";

		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.dme2.test.TestVersionSelector_InputsDoNotMatch");
		try{
		RegistryFsSetup.init();

		RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithVersionSelector();
		
		Properties props = RegistryFsSetup.init();
		DME2Configuration config = new DME2Configuration("TestVersionSelector_InputsDoNotMatch", props);			

		RegistryFsSetup grmInit = new RegistryFsSetup();
//		grmInit.saveRouteInfoInGRM(config, rtInfo, "LAB");

		mgr = new DME2Manager("TestVersionSelector_InputsDoNotMatch", config);
				
//		mgr = new DME2Manager("TestVersionSelector_InputsDoNotMatch", RegistryFsSetup.init());
		mgr.bindServiceListener(serviceURI, new EchoServlet(serviceURI, "TestVersionSelector_InputsDoNotMatch"));
		
		String uriStr = "http://DME2SEARCH/service=com.att.dme2.test.TestVersionSelector_InputsDoNotMatch/version=7.0.0/envContext=LAB/partner=DME2_PARTNER";

		Request request = new RequestBuilder(new URI(uriStr)).withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
		DME2Client sender = new DME2Client(mgr, request);
//		Payload payload = new TextPayload("THIS IS A TEST");
		
//		DME2Client sender = new DME2Client(mgr, new URI(uriStr), 10000);
//		sender.setPayload("This is a test");
		
		String reply = null;
		
		reply = (String) sender.sendAndWait(new DME2TextPayload("This is a test"));
		
		
		assertNotNull(reply);
		
		}finally{
			try {
			mgr.unbindServiceListener(serviceURI);
			} catch(Exception e) {	}
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}
		
	}
}
