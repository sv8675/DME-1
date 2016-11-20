/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.event.DefaultMetricsCollector;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;

@Ignore
public class TestMetricsCollection extends DME2BaseTestCase {

	/** The bham_1_ launcher. */
	private ServerControllerLauncher bham_1_Launcher;

	private DME2Manager manager = null;


	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() {
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("AFT_DME2_DISABLE_METRICS", "false");
		System.setProperty("platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		Properties props = null;
		try{	
			props = RegistryFsSetup.init();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		props.setProperty("platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		props.setProperty("AFT_DME2_DISABLE_METRICS", "false");		

		DME2Configuration config = new DME2Configuration("RegistryFsSetup", props);			
		
		try{	
			manager = new DME2Manager("RegistryFsSetup", config);
		}catch(Exception ex){
			ex.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	public void tearDown() {
		if (bham_1_Launcher != null) {
			bham_1_Launcher.destroy();
		}
	}

	@Test
	public void testMetricsEnv() throws Exception {
		String containerName="com.att.test.MetricsTestCollectorClient"; 
		String containerVersion="1.0.0"; 
		String containerRO="TESTRO"; 
		String containerEnv="LAB"; 
		String containerPlat=TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV; 
		String containerHost=null; 
		String containerPid="1234"; 
		String containerPartner="TEST";
		System.setProperty("platform", containerPlat);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
		//System.setProperty("platform","NON-LAB")
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("AFT_DME2_DISABLE_METRICS", "false");

		try {
			containerHost = InetAddress.getLocalHost().getHostName();
		}catch(Exception e) {
			
		}
		System.setProperty("lrmHost",containerHost);
		System.setProperty("Pid",containerPid);
		System.setProperty("partner",containerPartner);
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost",
				"brcbsp01",
				"-serverPort",
				"18700",
				"-registryType",
				"FS",
				"-servletClass",
				"TestMetricsServlet",
				"-serviceName",
				"service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se" };
		bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
		bham_1_Launcher.launch();

		Thread.sleep(30000);
		System.out.println(containerName + " , " + containerVersion + " , " + containerRO + " , " + containerEnv + " , " + 
				containerPlat + " , " + containerHost + " , " + containerPid + " , " + "TEST");
		
		DefaultMetricsCollector collector = DefaultMetricsCollector.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
				containerPlat, containerHost, containerPid, "TEST");
		System.err.println("collector : "+ collector);
//		collector.setDisablePublish(true);

/**		long eventTime1 = System.currentTimeMillis();
		Timeslot slot1 = collector.getTimeslotForTime(eventTime1);
		Collection<SvcProtocolMetrics> metricsc1 = slot1.getAllMetrics();
		Iterator<SvcProtocolMetrics> it1 = metricsc1.iterator();
		System.err.println("slot1 : "+ slot1);
		
		String env1 = null;
		while(it1.hasNext()) {
			SvcProtocolMetrics m = it1.next();
			System.err.println(m.getService() + ":" + m.getTimeslot() + ":" + m.getMetricsTimeslot());
			env1 = m.getContainer().getEnv();
		}
*/		
		// try to call a service we just registered
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
		//.withHeader(DME2Constants.DME2_REQUEST_PARTNER_CLASS,"TEST").withHeader(DME2Constants.DME2_REQUEST_PARTNER,"TEST").
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);		
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		DME2Payload payload = new DME2TextPayload("this is a test"); 
//		sender.setPayload("this is a test");
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		
		sender.setResponseHandlers(replyHandler);
		sender.send(payload);

		String reply = replyHandler.getResponse(60000);
		System.out.println("REPLY 1 =" + reply);
		bham_1_Launcher.destroy();
		Thread.sleep(5000);
		
		//com.att.test.MetricsTestCollectorClient,1.0.0,TESTRO,DEV,SANDBOX-LAB,ACNFL084Q1,1234,PTE
		//MetricsCollector collector = MetricsCollectorFactory.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
		//		containerPlat, containerHost, containerPid, containerPartner);
		//Thread.sleep(1000);
		System.out.println("Boolean.parseBoolean(config.getProperty(AFT_DME2_DISABLE_METRICS)) : " + Boolean.parseBoolean(manager.getConfig().getProperty("AFT_DME2_DISABLE_METRICS")));
		long eventTime = System.currentTimeMillis();
//		collector.addEvent(eventTime, uriStr, containerVersion, DME2Constants.DME2_INTERFACE_SERVER_ROLE, "1234", DME2Constants.DME2_INTERFACE_HTTP_PROTOCOL, Event.RESPONSE, "TestMsg", 1000, 900);
/**		Timeslot slot = collector.getTimeslotForTime(eventTime);
		Collection<SvcProtocolMetrics> metricsc = slot.getAllMetrics();
		Iterator<SvcProtocolMetrics> it = metricsc.iterator();
		String env = null;
		while(it.hasNext()) {
			SvcProtocolMetrics m = it.next();
			System.out.println(m.getService() + ":" + m.getTimeslot() + ":" + m.getMetricsTimeslot());
			env = m.getContainer().getEnv();
		}

		assertEquals(env,"LAB");
*/
	}	

	/**
	 * public void testDummy() throws Exception { System.out.println("Success");
	 * }
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testMetricsHttpClientRequest() throws Exception {
		Thread.sleep(15000);
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
		System.setProperty("DME2.DEBUG", "true");
		String containerName="com.att.test.MetricsTestCollectorClient"; 
		String containerVersion="1.0.0"; 
		String containerRO="TESTRO"; 
		String containerEnv="DEV"; 
		String containerPlat=TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV; 
		String containerHost=null; 
		String containerPid="1234"; 
		String containerPartner="TEST";
		System.setProperty("platform", containerPlat);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
		//System.setProperty("platform","NON-LAB");
		//System.setProperty("AFT_DME2_DISABLE_METRICS", "false");
		try {
			containerHost = InetAddress.getLocalHost().getHostName();
		}catch(Exception e) {
			
		}
		System.setProperty("lrmHost",containerHost);
		System.setProperty("Pid",containerPid);
		System.setProperty("partner",containerPartner);

		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost",
				"brcbsp01",
				"-serverPort",
				"18701",
				"-registryType",
				"FS",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se" };
		bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
		bham_1_Launcher.launch();
		DefaultMetricsCollector collector = DefaultMetricsCollector.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
				containerPlat, containerHost, containerPid, "TEST");
//		collector.setDisablePublish(true);
		Thread.sleep(15000);
		// try to call a service we just registered
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
		
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);		
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		DME2Payload payload = new DME2TextPayload("this is a test"); 
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(payload);

		String reply = replyHandler.getResponse(60000);
		System.out.println("REPLY 1 =" + reply);
		// stop server that replied
		bham_1_Launcher.destroy();
		Thread.sleep(5000);

		// check metrics data
		//MetricsCollector collector = MetricsCollectorFactory.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
		//		containerPlat, containerHost, containerPid, containerPartner);

		long eventTime = System.currentTimeMillis();
		//System.out.println(collector.getCurrentTimeSlot());
/**		Timeslot slot = collector.getTimeslotForTime(eventTime);
		SvcProtocolMetrics metrics = slot.getServiceProtocolMetrics("MyService", "1.0.0", "CLIENT", null, "HTTP");
		Collection<SvcProtocolMetrics> metricsc = slot.getAllMetrics();
		Iterator<SvcProtocolMetrics> it = metricsc.iterator();
		while(it.hasNext()) {
			SvcProtocolMetrics m = it.next();
			if(m.getService() != null) {
				com.att.aft.metrics.core.Service svc = m.getService();
				System.err.println("Metrics service "+svc);
				assertTrue(svc.getName().equals("MyService") && svc.getRole().equals("CLIENT"));
				return;
			}
		}
*/
		fail(" If it gets here, then metrics lookup did not fetch any data");
	}
	
	@Test
	public void testMetricsIgnoreServiceList() throws Exception {
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		String containerName="com.att.test.MetricsTestCollectorClient"; 
		String containerVersion="1.0.0"; 
		String containerRO="TESTRO"; 
		String containerEnv="DEV"; 
		String containerPlat=TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV; 
		String containerHost=null; 
		String containerPid="1234"; 
		String containerPartner="PTE";
		System.setProperty("platform", containerPlat);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
		System.clearProperty("partner");
		//System.setProperty("platform","NON-LAB");
		System.setProperty("AFT_DME2_DISABLE_METRICS", "false");
		try {
			containerHost = InetAddress.getLocalHost().getHostName();
		}catch(Exception e) {
			
		}
		System.setProperty("lrmHost",containerHost);
		System.setProperty("Pid",containerPid);
		System.setProperty("partner",containerPartner);

		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost",
				"brcbsp01",
				"-serverPort",
				"18702",
				"-registryType",
				"FS",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se" };
		bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
		bham_1_Launcher.launch();
		DefaultMetricsCollector collector = DefaultMetricsCollector.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
				containerPlat, containerHost, containerPid, "TEST");
//		collector.setDisablePublish(true);
		Thread.sleep(25000);
		// try to call a service we just registered
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
		
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);		
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		
		DME2Payload payload = new DME2TextPayload("this is a test"); 
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(payload);

//		sender.setPayload("this is a test");
//		EchoReplyHandler replyHandler = new EchoReplyHandler();
//		sender.setReplyHandler(replyHandler);
//		sender.send();

		String reply = replyHandler.getResponse(60000);
		System.out.println("REPLY 1 =" + reply);

		request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		sender = new DME2Client(manager, request);		
		
		//sender = new DME2Client(manager, new URI(uriStr), 30000);
		
		payload = new DME2TextPayload("this is a test"); 
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(payload);
		
		//sender.setPayload("this is a test");
		//replyHandler = new EchoReplyHandler();
		//sender.setReplyHandler(replyHandler);
		//sender.send();
		
		
		reply = replyHandler.getResponse(60000);
		System.out.println("REPLY 2=" + reply);
		Thread.sleep(2000);
		// check metrics data
		//MetricsCollector collector = MetricsCollectorFactory.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
		//		containerPlat, containerHost, containerPid, containerPartner);

		long eventTime = System.currentTimeMillis();
		//System.out.println(collector.getCurrentTimeSlot());
/**		Timeslot slot = collector.getTimeslotForTime(eventTime);
		SvcProtocolMetrics metrics = slot.getServiceProtocolMetrics("MyService", "1.0.0", "CLIENT", null, "HTTP");
		Collection<SvcProtocolMetrics> metricsc = slot.getAllMetrics();
		Iterator<SvcProtocolMetrics> it = metricsc.iterator();
		while(it.hasNext()) {
			SvcProtocolMetrics m = it.next();
			com.att.aft.metrics.core.Service svc = m.getService();
			System.err.println("Metrics service "+svc);
			assertFalse(svc.getName().equals("MetricsService"));
		}
*/		
		bham_1_Launcher.destroy();
		Thread.sleep(25000);
	}
	@Test
	public void testMetricsHttpServerRequest() throws Exception {
		Thread.sleep(10000);
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
		System.setProperty("DME2.DEBUG", "true");
		String containerName="com.att.test.MetricsTestCollectorClient"; 
		String containerVersion="1.0.0"; 
		String containerRO="TESTRO"; 
		String containerEnv="DEV"; 
		String containerPlat=TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV; 
		String containerHost=null; 
		String containerPid="1234"; 
		String containerPartner="PTE";
		System.setProperty("platform", containerPlat);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
		//System.setProperty("platform","NON-LAB");
		System.setProperty("AFT_DME2_DISABLE_METRICS", "false");
		try {
			containerHost = InetAddress.getLocalHost().getHostName();
		}catch(Exception e) {
			
		}
		System.setProperty("lrmHost",containerHost);
		System.setProperty("Pid",containerPid);
		System.setProperty("partner",containerPartner);
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost",
				"brcbsp01",
				"-serverPort",
				"18703",
				"-registryType",
				"FS",
				"-servletClass",
				"TestMetricsServlet",
				"-serviceName",
				"service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se" };
		bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
		bham_1_Launcher.launch();
		DefaultMetricsCollector collector = DefaultMetricsCollector.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
				containerPlat, containerHost, containerPid, containerPartner);
//		collector.setDisablePublish(true);
		Thread.sleep(15000);

		// try to call a service we just registered
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

		
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);		
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		
		DME2Payload payload = new DME2TextPayload("this is a test"); 
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(payload);
		
		
//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
//		sender.setPayload("this is a test");
//		EchoReplyHandler replyHandler = new EchoReplyHandler();
//		sender.setReplyHandler(replyHandler);
//		sender.send();

		String reply = replyHandler.getResponse(60000);
		System.out.println("REPLY in testMetricsHttpServerRequest =" + reply);
		assertTrue(reply.contains("role=SERVER") && reply.contains("name=MyService"));
		// stop server that replied
		bham_1_Launcher.destroy();
		Thread.sleep(15000);
	}	
	
	public static void main(String a[] ) throws Exception {
		TestMetricsCollection failOver = new TestMetricsCollection();
		failOver.setUp();
		failOver.testMetricsHttpClientRequest();
	}

}
