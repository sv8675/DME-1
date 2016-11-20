/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.DME2RestfulHandler;
import com.att.aft.dme2.handler.DME2RestfulHandler.ResponseInfo;
import com.att.aft.dme2.handler.DME2SimpleReplyHandler;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.server.test.EchoServlet;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.Locations;


public class TestAuthentication extends DME2BaseTestCase 
{

	DME2Manager manager = null;

	@Before
	public void setUp()
	{
		RegistryFsSetup.cleanup();
		super.setUp();
		
		System.setProperty("java.security.auth.login.config", "src/test/etc/mylogin.conf");
		System.setProperty("org.eclipse.jetty.util.log.DEBUG", "true");
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestAuthentication");
		Locations.BHAM.set();
	}

	@After
	public void tearDown(){
		super.tearDown();
		System.clearProperty("java.security.auth.login.config");
		System.clearProperty("org.eclipse.jetty.util.log.DEBUG");
		System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		RegistryFsSetup.cleanup();
	}

	@Test
	public void testClientAuthentication() throws Exception 
	{
		DME2Configuration config = new DME2Configuration("TestDME2Manager", RegistryFsSetup.init());
		DME2Manager manager = new DME2Manager("TestDME2Manager", config);
		
		String name = "service=MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String allowedRoles[] = {"myclientrole"};
		String loginMethod = "BASIC";
		String realm = "myrealm";
		
		try
		{
			manager.bindServiceListener(name, new EchoServlet(name, "bau_se_1"), realm, allowedRoles, loginMethod);
			Thread.sleep(2000);

			String uriStr = "http://DME2RESOLVE/"+name;

			DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler(manager.getConfig(), "MyService", false);

			Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withAuthCreds(realm, "test", "test").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			
			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);

			DME2TextPayload payload = new DME2TextPayload("this is a test");
			sender.send(payload);
			
			String reply = replyHandler.getResponse(60000);
			assertEquals("EchoServlet:::bau_se_1:::service=MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE", reply.trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try	{manager.unbindServiceListener(name);}
			catch (Exception e)	{}
		}
	}
	
	@Test
	  @Ignore
    public void testClientAuthenticationWithRESTService() throws Exception 
	{
		String svcName = "/service=com.att.aft.dme2.test.TestClientAuthWithRESTService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
		DME2Manager manager = new DME2Manager("TestDME2ManagerAuth", RegistryFsSetup.init());
		
		try
		{
			String allowedRoles[] = {"myclientrole"};
			String loginMethod = "BASIC";
			String realm = "myrealm";
			manager.bindServiceListener(svcName, new EchoServlet(svcName,"testClientAuthenticationWithRESTService"), realm, allowedRoles, loginMethod);
			Thread.sleep(2000);
			
			// try to call a service we just registered
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestClientAuthWithRESTService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
			
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			ResponseInfo resp = DME2RestfulHandler.callService(uriStr, 30000, "POST", null, new HashMap<String, String>(), new HashMap<String, String>(), payload, "test", "test");
			String reply = resp.getBody();
			
			assertTrue(reply.contains("EchoServlet:::testClientAuthenticationWithRESTService:::/service=com.att.aft.dme2.test.TestClientAuthWithRESTService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT"));
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}finally
		{
			try
			{
				manager.unbindServiceListener(svcName);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/*
     * TODO: This method depends on already existing route info. Needs to be reworked
     */
    @Test
    public void testClientAuthentication_WithMultipleCredentials() throws Exception
	{
		String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String allowedRoles[] = { "myclientrole" };
		String loginMethod = "BASIC";
		String realm = "myrealm";
		DME2Manager manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
		
		try
		{
			manager.bindServiceListener(name, new EchoServlet(name, "bau_se_1"), realm, allowedRoles, loginMethod);
			Thread.sleep(2000);
			
			// try to call a service we just registered
			String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
			
			DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler("MyService");
			
			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			sender.setPayload("this is a test");
			sender.setReplyHandler(replyHandler);
			sender.setCredentials("test", "test");
			sender.send();

			String reply = replyHandler.getResponse(60000);

			assertEquals("EchoServlet:::bau_se_1:::service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE", reply.trim());
			
			// Set new client username/password
			DME2SimpleReplyHandler replyHandler2 = new DME2SimpleReplyHandler("MyService");
			
			DME2Client sender2 = new DME2Client(manager, new URI(uriStr), 30000);
			sender2.setPayload("this is a test");
			sender2.setReplyHandler(replyHandler2);
			sender2.setCredentials("test2", "test");
			sender2.send();

			reply = replyHandler2.getResponse(60000);
			assertEquals("EchoServlet:::bau_se_1:::service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE", reply.trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				manager.unbindServiceListener(name);
			}
			catch (Exception e)
			{
			}
		}
	}
	
    
     // TODO: This method depends on already existaing route info. Needs to be reworked
     
    @Test
    public void testClientAuthenticationFailure_WithMultipleCredentials() throws Exception
	{
		String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String allowedRoles[] = { "myclientrole" };
		String loginMethod = "BASIC";
		String realm = "myrealm";
		DME2Manager manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
		
		try
		{
			manager.bindServiceListener(name, new EchoServlet(name, "bau_se_1"), realm, allowedRoles, loginMethod);
			Thread.sleep(2000);
			
			// try to call a service we just registered
			String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
			
			DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler("MyService");
			
			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			sender.setPayload("this is a test");
			sender.setReplyHandler(replyHandler);
			sender.setCredentials("test", "test");
			sender.send();

			String reply = replyHandler.getResponse(60000);

			assertEquals("EchoServlet:::bau_se_1:::service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE", reply.trim());
			
			// Set new client username/password
			DME2SimpleReplyHandler replyHandler2 = new DME2SimpleReplyHandler("MyService");
			
			DME2Client sender2 = new DME2Client(manager, new URI(uriStr), 30000);
			sender2.setPayload("this is a test");
			sender2.setReplyHandler(replyHandler2);
			sender2.setCredentials("test2", "test");
			sender2.send();

			reply = replyHandler2.getResponse(60000);
			assertEquals("EchoServlet:::bau_se_1:::service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE", reply.trim());
			
			// Use invalid credentials
			DME2SimpleReplyHandler replyHandler3 = new DME2SimpleReplyHandler("MyService");
			
			DME2Client sender3 = new DME2Client(manager, new URI(uriStr), 30000);
			sender3.setPayload("this is a test");
			sender3.setReplyHandler(replyHandler3);
			sender3.setCredentials("invalid", "invalid");
			sender3.send();

			try
			{
				reply = replyHandler3.getResponse(60000);
			}
			catch (Exception e)
			{
				reply = e.getMessage();
			}
			
			System.out.println("Authorization error message: " + reply);
			assertTrue(reply.contains("[AFT-DME2-0707]"));

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				manager.unbindServiceListener(name);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	
    @Test
    public void testClientAuthenticationFailure() throws Exception {
		String name = "service=com.att.aft.dme2.test.testClientAuthenticationFailure/version=1.0.0/envContext=PROD/routeOffer=AUTH_FAIL";
		String allowedRoles[] = {"myclientrole"};
		String loginMethod = "BASIC";
		String realm = "myrealm";
		Properties props = RegistryFsSetup.init();
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		DME2Manager manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
		
		manager.bindServiceListener(name, new EchoServlet(name,"bau_se_1"), realm, allowedRoles, loginMethod);
		Thread.sleep(2000);
		
		// try to call a service we just registered
		String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.testClientAuthenticationFailure/version=1.0.0/envContext=PROD/routeOffer=AUTH_FAIL";
		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		sender.setPayload("this is a test");
		DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler("MyService");
		sender.setReplyHandler(replyHandler);
		
		sender.send();

		String reply = null;
		try {
			reply = replyHandler.getResponse(60000);
		}catch(Exception e) {
			reply = e.getMessage();
		}
		System.out.println("Authorization error message: " + reply);
		assertTrue(reply.contains("[AFT-DME2-0707]"));
		try {
			manager.unbindServiceListener("service=com.att.aft.dme2.test.testClientAuthenticationFailure/version=1.0.0/envContext=PROD/routeOffer=BAU_SE");
		} catch (Exception e) {
		}finally{
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}
		
	}
	
    @Test
    public void testClientAuthenticationFailureWithStreamPayload() throws Exception {
		String name = "service=com.att.aft.dme2.test.testClientAuthenticationFailureWithStreamPayload/version=1.0.0/envContext=PROD/routeOffer=AUTH_FAIL";
		String allowedRoles[] = {"myclientrole"};
		String loginMethod = "BASIC";
		String realm = "myrealm";
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		DME2Manager manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
		
		manager.bindServiceListener(name, new EchoServlet(name,"bau_se_1"), realm, allowedRoles, loginMethod);
		Thread.sleep(2000);
		
		// try to call a service we just registered
		String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.testClientAuthenticationFailureWithStreamPayload/version=1.0.0/envContext=PROD/routeOffer=AUTH_FAIL";
		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		InputStream in_s = new FileInputStream("m2e.jks");
		sender.setDME2Payload(new DME2StreamPayload(in_s));
		DME2SimpleReplyHandler replyHandler = new DME2SimpleReplyHandler("testClientAuthenticationFailureWithStreamPayload");
		sender.setReplyHandler(replyHandler);
		
		sender.send();

		String reply = null;
		try {
			reply = replyHandler.getResponse(60000);
		}catch(Exception e) {
			reply = e.getMessage();
		}
		System.out.println("Authorization error message: " + reply);
		assertTrue(reply.contains("[AFT-DME2-0707]"));
		try {
			manager.unbindServiceListener("service=com.att.aft.dme2.test.testClientAuthenticationFailureWithStreamPayload/version=1.0.0/envContext=PROD/routeOffer=BAU_SE");
		} catch (Exception e) {
		}finally{
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}
		System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
	}
	
    @Test
    @Ignore
   public void testClientAuthenticationFailureWithRESTService() throws Exception {
		String svcName = "/service=com.att.aft.dme2.test.TestClientAuthFailureWithRESTService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
		DME2Manager manager = new DME2Manager("TestDME2ManagerAuthFailure", RegistryFsSetup.init());
		try {
			String allowedRoles[] = {"myclientrole"};
			String loginMethod = "BASIC";
			String realm = "myrealm";

			manager.bindServiceListener(svcName, new EchoServlet(svcName,"testClientAuthenticationWithRESTService"), realm, allowedRoles, loginMethod);
			Thread.sleep(2000);
			
			// try to call a service we just registered
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestClientAuthFailureWithRESTService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
			
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			ResponseInfo resp = DME2RestfulHandler.callService(uriStr, 30000, "POST", null, new HashMap<String, String>(), new HashMap<String, String>(), payload, "invalid", "invalid");
			String reply = resp.getBody();
			
			System.err.println("Authorization error message: " + reply);
			assertTrue(reply.contains("Error 401 Unauthorized"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				manager.unbindServiceListener(svcName);
			} catch (Exception e) {}
		}
	}	
}