/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.handler.DME2SimpleReplyHandler;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.Locations;


/**
 * The Class TestDME2Manager.
 */
public class TestDME2Manager extends DME2BaseTestCase{

	/*
	 * (non-Javadoc)
	 * 
	 * ()
	 */
    @Before
    public void setUp() {
			RegistryFsSetup.cleanup();
    	super.setUp();
    	System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);

		Locations.BHAM.set();
	}

	/**
	 * Test client request.
	 * 
	 * @throws Exception
	 *             the exception
	 */
    @Test
    public void testClientRequest() throws Exception {
		/** The manager. */
		DME2Manager manager = null;
		try{
		manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
    	
		String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		//manager.bindServiceListener(name, new DME2NullServlet(name));
		manager.bindServiceListener(name, new EchoServlet(name,"bau_se_1"));
		
		name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
		//manager.bindServiceListener(name, new DME2NullServlet(name));
		manager.bindServiceListener(name, new EchoServlet(name,"apple_se_1"));

		name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=WALMART_SE";
		//manager.bindServiceListener(name, new DME2NullServlet(name));
		manager.bindServiceListener(name, new EchoServlet(name,"walmart_se_1"));

		// to allow servlet init to happen
		Thread.sleep(10000);
		// try to call a service we just registered
		//String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
		String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		sender.setPayload("this is a test");
		DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler(manager.getConfig(), "MyService", false);
		sender.setReplyHandler(replyHandler);
		sender.send();

		String reply = replyHandler.getResponse(60000);

		assertEquals("EchoServlet:::bau_se_1:::service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE", reply.trim());
		} finally{
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=WALMART_SE");
			}
			catch (Exception e) {
				// ignore
			}
		} 
	}

	/**
	 * Test client request.
	 * 
	 * @throws Exception
	 *             the exception
	 */
    @Test
    public void testClientRequestCharSetUTF8() throws Exception {
		String utf8String = null;
		DME2Manager manager = null;
		try{
			manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
			File f = new File("src/test/etc/utf8data.txt");
			String tempData = null;
			StringBuffer strBuf = new StringBuffer();
			if(f.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(f));
				while( (tempData=br.readLine()) != null){
					strBuf.append(tempData);
				}
				utf8String = strBuf.toString();
			}
			System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
			//manager.bindServiceListener(name, new DME2NullServlet(name));
			manager.bindServiceListener(name, new EchoServlet(name,"bau_se_1"));
			
			name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
			//manager.bindServiceListener(name, new DME2NullServlet(name));
			manager.bindServiceListener(name, new EchoServlet(name,"apple_se_1"));
	
			name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=WALMART_SE";
			//manager.bindServiceListener(name, new DME2NullServlet(name));
			manager.bindServiceListener(name, new EchoServlet(name,"walmart_se_1"));
	
			// to allow servlet init to happen
			Thread.sleep(1000);
			// try to call a service we just registered
			//String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
			String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000, "UTF-8");
			sender.setPayload(utf8String);
			System.out.println("Payload "+ utf8String);
			Map<String,String> headers = new HashMap<String,String>();
			headers.put("testReturnCharSet", "UTF-8");
			headers.put("testEchoBack", "true");
			sender.setHeaders(headers);
			TestReplyHandler replyHandler = new TestReplyHandler(uriStr);
			sender.setReplyHandler(replyHandler);
			sender.send();
			String response = replyHandler.getResponse(60000);
			System.out.println("response " + response);
			if (replyHandler.echoedCharSet == null) {
				fail("charset was not set to UTF-8 as expected, instead was null");
			}
			
			if (!replyHandler.echoedCharSet.equals("UTF-8")) {
				fail("charset was not set to UTF-8 as expected, instead was " + replyHandler.echoedCharSet);
			}
			
			assertEquals(utf8String, response);
			
		} finally {
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=WALMART_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.getServer().stop();
			}catch (Exception e) {
			}
		}
	}	
	
	/**
	 * Test client request.
	 * 
	 * @throws Exception
	 *             the exception
	 */
    @Test
    public void testClientRequestHeaderMapAltered() throws Exception {
		String utf8String = null;
		DME2Manager manager = null;
		try{
			manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
			File f = new File("src/test/etc/utf8data.txt");
			String tempData = null;
			StringBuffer strBuf = new StringBuffer();
			if(f.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(f));
				while( (tempData=br.readLine()) != null){
					strBuf.append(tempData);
				}
				utf8String = strBuf.toString();
			}
			System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
			//manager.bindServiceListener(name, new DME2NullServlet(name));
			manager.bindServiceListener(name, new EchoServlet(name,"bau_se_1"));
			
			name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
			//manager.bindServiceListener(name, new DME2NullServlet(name));
			manager.bindServiceListener(name, new EchoServlet(name,"apple_se_1"));
	
			name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=WALMART_SE";
			//manager.bindServiceListener(name, new DME2NullServlet(name));
			manager.bindServiceListener(name, new EchoServlet(name,"walmart_se_1"));
	
			// to allow servlet init to happen
			Thread.sleep(1000);
			// try to call a service we just registered
			//String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
			String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000, "UTF-8");
			sender.setPayload(utf8String);
			Map<String,String> headers = new HashMap<String,String>();
			headers.put("testReturnCharSet", "UTF-8");
			headers.put("testEchoBack", "true");
			sender.setHeaders(headers);
			// Altering the header map should not impact the request
			headers.remove("testReturnCharSet");
			headers.remove("testEchoBack");
			TestReplyHandler replyHandler = new TestReplyHandler(uriStr);
			sender.setReplyHandler(replyHandler);
			sender.send();
			String response = replyHandler.getResponse(60000);
			
			if (replyHandler.echoedCharSet == null) {
				fail("charset was not set to UTF-8 as expected, instead was null");
			}
			
			if (!replyHandler.echoedCharSet.equals("UTF-8")) {
				fail("charset was not set to UTF-8 as expected, instead was " + replyHandler.echoedCharSet);
			}
			
			assertEquals(utf8String, response);
			
		} finally {
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.unbindServiceListener("service=MyService/version=1.0.0/envContext=PROD/routeOffer=WALMART_SE");
			}
			catch (Exception e) {
				// ignore
			}
			try {
				manager.getServer().stop();
			}catch (Exception e) {
			}
		}
	}	

	/**
	 * public void testDummy() throws Exception { System.out.println("Success");
	 * }
	 * 
	 * @throws Exception
	 *             the exception
	 */

    @Test
    public void testDME2ManagerWithDefaultServer() throws Exception {
		String service = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
		DME2Manager manager = null;
		try{
			manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
			manager.bindServiceListener(service, new EchoServlet(service,
				"routeOffer1"));
			DME2EndpointRegistry registry = manager.getEndpointRegistry();
			DME2Endpoint[] endpoints = manager.findEndpoints("MyService", "1.0.0", "PROD",
					"APPLE_SE", false);
	
			assertEquals(service, endpoints[0].getPath());
		} finally{
		try {
			manager.unbindServiceListener(service);
		} catch (Exception e) {
		}
		}
	}
	
	
    @Test
    @Ignore
    public void testDME2ManagerForShutdown() throws Exception {
		try {
		DME2Manager manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
			DME2Manager manager1 = new DME2Manager("TestDME2ManagerForShutdown", RegistryFsSetup.init());

		String service = "/service=com.att.test.MyServiceForShutdown/version=1.0.0/envContext=LAB/routeOffer=APPLE_SE";
		manager1.bindServiceListener(service, new EchoServlet(service,
				"routeOffer2"));
		Thread.sleep(10000);
		DME2EndpointRegistry registry = manager1.getEndpointRegistry();
		DME2Endpoint[] endpoints = registry.find("com.att.test.MyServiceForShutdown", "1.0.0", "LAB", "APPLE_SE");
		System.out.println("Service " + service);
		System.out.println("Service " + endpoints[0].getPath());
		assertEquals(service, endpoints[0].getPath());
		
		try{
			manager.shutdown();
		}catch (Exception e) {
			assertFalse(e==null);
		}
		// manager.shutdown will shutdown the dme2server and unpublish associated endpoints
		assertTrue(!manager.getServer().isRunning());
		
		try {
			manager.unbindServiceListener(service);
		} catch (Exception e) {
		}
		} finally {
			System.clearProperty("platform");
		}
	}

}

class TestReplyHandler extends DME2SimpleReplyHandler {
	
	private Map<String, String> responseHeaders;

	public TestReplyHandler(String service) throws DME2Exception {
		super(DME2Manager.getDefaultInstance().getConfig(), service, false);
	}

	@Override
	public void handleException(Map<String, String> requestHeaders,
			Throwable e) {
		super.handleException(requestHeaders, e);
		this.echoedCharSet = requestHeaders.get("com.att.aft.dme2.test.charset");				
	}

	@Override
	public void handleReply(int responseCode, String responseMessage,
			InputStream in, Map<String, String> requestHeaders,
			Map<String, String> responseHeaders) {
		this.echoedCharSet = responseHeaders.get("com.att.aft.dme2.test.charset");
		this.responseHeaders = responseHeaders;
		super.handleReply(responseCode,  responseMessage, in, requestHeaders, responseHeaders);
	}
	
	public Map<String, String> getResponseHeaders()
	{
		return responseHeaders;
	}
	
	public String echoedCharSet = null;
}

