/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;

/**
 * The Class TestVersions.
 */
@Ignore
public class TestVersionsGrm  {

	/** The bham_1_0_0_ launcher. */
	private static ServerControllerLauncher bham_1_0_0_Launcher;

	/** The bham_1_1_0_ launcher. */
	private static ServerControllerLauncher bham_1_1_0_Launcher;

	/** The char_1_1_1_ launcher. */
	private static ServerControllerLauncher char_1_1_1_Launcher;
	
	private static DME2Manager manager = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
//	@Override
//	protected void setUp() throws Exception {
  @BeforeClass
  public static void setUpTestCase() throws DME2Exception, IOException {
		Properties props = RegistryFsSetup.init();
		System.setProperty("platform",TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG","true");
		DME2Configuration config = new DME2Configuration("TestVersionsGrm", props);			

		manager = new DME2Manager("TestVersionsGrm", config);
		init();
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	//@Override
	//protected void tearDown() throws Exception {
  @AfterClass
  public static void tearDownTest() throws Exception {
		if (bham_1_0_0_Launcher != null) {
			bham_1_0_0_Launcher.destroy();
		}

		if (bham_1_1_0_Launcher != null) {
			bham_1_1_0_Launcher.destroy();
		}

		if (char_1_1_1_Launcher != null) {
			char_1_1_1_Launcher.destroy();
		}

	}

	/**
	 * Execute once before all test cases.
	 * @throws IOException 
	 */
	private static void init() throws IOException {
		// run the server in bham.
		String[] bham_1_0_0_bau_se_args = {
				"-serverHost",
				InetAddress.getLocalHost().getCanonicalHostName(),
				"-serverPort",
				"4600",
				"-registryType",
				"GRM",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=com.att.aft.dme2.TestVersionsGrm/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_0_0_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };
		bham_1_0_0_Launcher = new ServerControllerLauncher(bham_1_0_0_bau_se_args);
		bham_1_0_0_Launcher.launch();

		String[] bham_1_1_0_bau_se_args = {
				"-serverHost",
				InetAddress.getLocalHost().getCanonicalHostName(),
				"-serverPort",
				"4601",
				"-registryType",
				"GRM",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=com.att.aft.dme2.TestVersionsGrm/version=1.1.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_1_0_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };
		bham_1_1_0_Launcher = new ServerControllerLauncher(bham_1_1_0_bau_se_args);
		bham_1_1_0_Launcher.launch();

		String[] char_1_1_1_bau_se_args = {
				"-serverHost",
				InetAddress.getLocalHost().getCanonicalHostName(),
				"-serverPort",
				"4602",
				"-registryType",
				"GRM",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=com.att.aft.dme2.TestVersionsGrm/version=1.1.1/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "CHAR", "-serverid", "char_1_1_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };
		char_1_1_1_Launcher = new ServerControllerLauncher(char_1_1_1_bau_se_args);
		char_1_1_1_Launcher.launch();

		try {
			Thread.sleep(10000);
		} catch (Exception ex) {
		}
	}



	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_0_0() throws Exception {
		tryVersion("1.0.0","/version=1.0.0/");
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_1_0() throws Exception {

		tryVersion("1.1.0","/version=1.1.0/");
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_1_1() throws Exception {

		tryVersion("1.1.1","/version=1.1.1/");
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1() throws Exception {

		tryVersion("1","/version=1.");
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_1() throws Exception {

		tryVersion("1.1","/version=1.1.");
	}
	
	private void tryVersion(String version, String expected) throws Exception {
		String versionText = "version=" + version;
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestVersionsGrm/" + versionText + "/envContext=DEV/dataContext=205977/partner=TEST";

		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
//		sender.setPayload("this is a test");
		EchoReplyHandler replyHandler = new EchoReplyHandler();
//		sender.setReplyHandler(replyHandler);

		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(300000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);

		DME2Payload payload = new DME2TextPayload("TEST IS A TEST");

		sender.setResponseHandlers(replyHandler);
		sender.send(payload);

		String reply = replyHandler.getResponse(31000);
		System.out.println(reply);
		

	  assertNotNull("first reply is null", reply);
    assertTrue("\"" + expected + "\" not found in reply="+reply, reply.contains(expected) );
	}

	/*public static void main(String a[]) throws Exception {
		TestVersionsGrm grm = new TestVersionsGrm();
		grm.setUp();
		grm.testVersion_1_0_0();
	}*/
}
