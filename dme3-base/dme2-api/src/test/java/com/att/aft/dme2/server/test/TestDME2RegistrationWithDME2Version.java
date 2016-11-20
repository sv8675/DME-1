/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;

import junit.framework.TestCase;

@Ignore
public class TestDME2RegistrationWithDME2Version extends TestCase {
	
	public void setUp() throws Exception{
		System.setProperty("AFT_DME2_GRM_URLS", "http://0.0.0.0:8001/GRMService/v1");
		System.setProperty("DME2.DEBUG","true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		RegistryFsSetup.init();
	}

	public void testServiceRegistration_WithDME2VersionAddedToGRMRegistry(){
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_WithDME2Version");

		try{
			service = "/service=com.att.aft.dme2.test.testServiceRegistrationWithDME2Version/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

			DME2Configuration config = new DME2Configuration("testPayloadCompression", new Properties());			
			mgr = new DME2Manager("testServiceRegistrationWithDME2Version", config);
			
//			mgr = new DME2Manager("testServiceRegistrationWithDME2Version", new Properties());
			mgr.bindServiceListener(service, new EchoServlet(service, "testServiceRegistrationWithDME2Version"));
			
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.testServiceRegistrationWithDME2Version/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
			
			Request request = new RequestBuilder(new URI(uriStr)).withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
//			sender.setResponseHandlers(handler);

//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");
			
			String reply = (String) sender.sendAndWait(payload);
			String DME2Version = DME2Manager.getVersion();
			
//			DmeUniformResource uri = new DmeUniformResource(config, uriStr);
			
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.testServiceRegistrationWithDME2Version", "1.0.0", "LAB", "DEFAULT");
			
			assertTrue(endpoints.get(0).getDME2Version().equals(DME2Version));
			String s = "";
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			
			try {
				mgr.unbindServiceListener(service);
			} catch (DME2Exception e) {
				e.printStackTrace();
			}
			finally {
				System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			}
		}
	}
}
