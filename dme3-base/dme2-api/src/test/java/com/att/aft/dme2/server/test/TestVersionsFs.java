/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
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
public class TestVersionsFs {

	/** The bham_1_0_0_ launcher. */
	private static ServerControllerLauncher bham_1_0_0_Launcher;

	/** The bham_1_1_0_ launcher. */
	private static ServerControllerLauncher bham_1_1_0_Launcher;

	/** The char_1_1_1_ launcher. */
	private static ServerControllerLauncher char_1_1_1_Launcher;
	
	private static DME2Manager manager = null;

	/**
	 * Inits the.
	 */
	private static void init() {
		// run the server in bham.
		String[] bham_1_0_0_bau_se_args = {
				"-serverHost",
				"brcbsp01",
				"-serverPort",
				"4600",
				"-registryType",
				"FS",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_0_0_bau_se" };
		bham_1_0_0_Launcher = new ServerControllerLauncher(bham_1_0_0_bau_se_args);
		bham_1_0_0_Launcher.launch();

		String[] bham_1_1_0_bau_se_args = {
				"-serverHost",
				"brcbsp02",
				"-serverPort",
				"4601",
				"-registryType",
				"FS",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=MyService/version=1.1.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_1_0_bau_se" };
		bham_1_1_0_Launcher = new ServerControllerLauncher(bham_1_1_0_bau_se_args);
		bham_1_1_0_Launcher.launch();

		String[] char_1_1_1_bau_se_args = {
				"-serverHost",
				"crcbsp01",
				"-serverPort",
				"4602",
				"-registryType",
				"FS",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=MyService/version=1.1.1/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "CHAR", "-serverid", "char_1_1_1_bau_se" };
		char_1_1_1_Launcher = new ServerControllerLauncher(char_1_1_1_bau_se_args);
		char_1_1_1_Launcher.launch();

		try {
			Thread.sleep(5000);
		} catch (Exception ex) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@BeforeClass
	public static void setUpTest() throws Exception {
		Properties props = RegistryFsSetup.init();
		DME2Configuration config = new DME2Configuration("TestRegistry", props);			

		manager = new DME2Manager("TestRegistry", config);
		init();
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@AfterClass
	public static void tearDown() throws Exception {
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
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_0_0() throws Exception {
		tryVersion("1.0.0","/version=1.0.0/", 4600);
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_1_0() throws Exception {

		tryVersion("1.1.0","/version=1.1.0/", 4601);
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_1_1() throws Exception {

		tryVersion("1.1.1","/version=1.1.1/", 4602);
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1() throws Exception {

		tryVersion("1","/version=1.1.1/", 4602);
	}

	/**
	 * 
	 * @throws Exception
	 */
  @Test
	public void testVersion_1_1() throws Exception {

		tryVersion("1.1","/version=1.1.1/", 4602);
	}

	private void tryVersion(String version, String expected, int port) throws Exception {
		String versionText = "version=" + version;
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=MyService/" + versionText + "/envContext=PROD/dataContext=205977/partner=TEST";
		RegistryFsSetup.copyRoutingFile(uriStr);

		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(300000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client client = new DME2Client(manager, request);
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
//		sender.setPayload("this is a test");
		EchoReplyHandler replyHandler = new EchoReplyHandler();
//		sender.setReplyHandler(replyHandler);
		DME2Payload payload = new DME2TextPayload("TEST IS A TEST");

		client.setResponseHandlers(replyHandler);

		client.send(payload);

		String reply = replyHandler.getResponse(60000);
		System.out.println(reply);
		
		if (reply == null ) {
			fail("first reply is null");
		} else if (reply.indexOf(expected) == -1) {
			fail("\"" + expected + "\" not found in reply="+reply);
		}

	}

}
