/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;

import junit.framework.TestCase;

@Ignore
public class TestDME2RegistrationWithRangeOfVersions extends TestCase{
	
	public void setUp() throws Exception{
		//System.setProperty("AFT_DME2_GRM_URLS", "http://0.0.0.0:8001/GRMService/v1");
		System.setProperty("DME2.DEBUG","true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		RegistryFsSetup.init();
	}
	
	public void testDME2ServiceRegistration_WithClientSupportedRangeOfVersions_HappyScenario() throws Exception{
		
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_WithClientSupportedVersion");

		DME2Configuration config = new DME2Configuration("testClientSupportedVersions", new Properties());			
		mgr = new DME2Manager("testClientSupportedVersions", config);
		
		
		try{
			service = "/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY?supportedVersionRange=68.0,70.5";
			
			//mgr = new DME2Manager("testClientSupportedVersions", new Properties());
			mgr.bindServiceListener(service, new EchoServlet(service, "testClientSupportedVersions"));
			
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Request request = new RequestBuilder(new URI(uriStr)).withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);

			//DME2Client sender = new DME2Client(mgr, new URI(uriStr), 10000);
			//sender.setPayload("This is a test");
			
			String reply = (String) sender.sendAndWait(new DME2TextPayload("This is a test"));
			
			uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/partner=TEST";
			
//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
			//sender.setPayload("This is a test");
			
//			reply = (String) sender.sendAndWait(10000);
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
				mgr.unbindServiceListener(service);
			}catch(Exception e){
				e.printStackTrace();
			}
			finally {
				System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			}
			
		}
	}
	
	public void testDME2ServiceRegistration_WithClientSupportedRangeOfVersions_InvalidServiceVersion() throws Exception{
		
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_InvalidVersionRange");
		
		try{
			service = "/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY?clientSupportedVersions=68.0,70.5";
			
			DME2Configuration config = new DME2Configuration("testClientSupportedVersions", new Properties());			
			mgr = new DME2Manager("testClientSupportedVersions", config);

			//mgr = new DME2Manager("testClientSupportedVersions", new Properties());
			mgr.bindServiceListener(service, new EchoServlet(service, "testClientSupportedVersions"));
			
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.testClientSupportedVersions/version=70.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Request request = new RequestBuilder(new URI(uriStr)).withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");
			String reply;			
			try {
				reply = (String) sender.sendAndWait(new DME2TextPayload("This is a test"));
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("AFT-DME2-0702"));
			}
			
			uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testClientSupportedVersions/version=70.0.0/envContext=LAB/partner=TEST";
			
//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
			request = new RequestBuilder(new URI(uriStr)).withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			sender = new DME2Client(mgr, request);
//			sender.setPayload("This is a test");
			
			try {
				reply = (String) sender.sendAndWait(new DME2TextPayload("This is a test"));
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("AFT-DME2-0702"));
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
			mgr.unbindServiceListener(service);
			} catch(Exception e) {
				
			}finally {
				System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			}
		}
	}
	
	public void testDME2ServiceRegistration_WithClientSupportedRangeOfVersions_InvalidClientSupportedVersion() throws Exception{
		
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_InvalidClientVersionRange");
		
		try{
			service = "/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY?clientSupportedVersions=68.0.8,70.5.4";
			
			DME2Configuration config = new DME2Configuration("testClientSupportedVersions", new Properties());			
			mgr = new DME2Manager("testClientSupportedVersions", config);
//			mgr = new DME2Manager("testClientSupportedVersions", new Properties());
			
			try{
				//Should fail with: [GRMSVC-1043]
				mgr.bindServiceListener(service, new EchoServlet(service, "testClientSupportedVersions"));
			}catch(Exception e){
				assertTrue(e.getMessage().contains("GRMSVC-1043"));
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				mgr.unbindServiceListener(service);
			}catch(Exception e) {
				
			}
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}
	}
	
	public void testDME2ServiceRegistration_WithClientSupportedRangeOfVersions_ClientVersionExceedsServiceVersion() throws Exception{
		
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_ClientVersionExceedsServiceVersion");
		
		try{
			service = "/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY?clientSupportedVersions=88.0,90.5.4";
			
			DME2Configuration config = new DME2Configuration("testClientSupportedVersions", new Properties());			
			mgr = new DME2Manager("testClientSupportedVersions", config);
//			mgr = new DME2Manager("testClientSupportedVersions", new Properties());
			mgr.bindServiceListener(service, new EchoServlet(service, "testClientSupportedVersions"));
			
		}catch(Exception e){
			assertTrue(e.getMessage().contains("GRMSVC-1043"));
		}finally{
			try {
			mgr.unbindServiceListener(service);
			} catch(Exception e) {
				
			}
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}
	}
	
	public void testDME2ServiceRegistration_WithClientSupportedRangeOfVersions_UpdateLease() throws Exception{
		
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_WithClientSupportedVersion");

		
		try{
			Properties props = new Properties();
			props.put("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "10000");
			
			service = "/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY?supportedVersionRange=68.0,70.5";
			
			DME2Configuration config = new DME2Configuration("testClientSupportedVersions", new Properties());			
			mgr = new DME2Manager("testClientSupportedVersions", config);
//			mgr = new DME2Manager("testClientSupportedVersions", props);
			mgr.bindServiceListener(service, new EchoServlet(service, "testClientSupportedVersions"));
			
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY";

			Request request = new RequestBuilder(new URI(uriStr)).withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			
//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");
			
			String reply = (String) sender.sendAndWait(new DME2TextPayload("This is a test"));
			
			uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/partner=TEST";
			
//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");

			request = new RequestBuilder(new URI(uriStr)).withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			sender = new DME2Client(mgr, request);
						
			reply = (String) sender.sendAndWait(new DME2TextPayload("This is a test"));
			
			assertTrue(reply.contains("EchoServlet:::testClientSupportedVersions:::/service=com.att.aft.dme2.test.testClientSupportedVersions/version=72.0.0/envContext=LAB/routeOffer=PRIMARY?supportedVersionRange=68.0,70.5"));
			
			/*Unpublish the service*/
			mgr.unbindServiceListener(service);
			
			/*Sleep to allow time for lease update to occur*/
			Thread.sleep(10000);
			
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.testClientSupportedVersions", "72.0.0", "LAB", "PRIMARY");
			assertEquals(endpoints.get(0).getDmeUniformResource().getSupportedVersionRange(), "68.0,70.5");
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
				mgr.unbindServiceListener(service);
			}catch(Exception e){
				e.printStackTrace();
			}
			finally {
				System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			}
			
		}
	}

}
