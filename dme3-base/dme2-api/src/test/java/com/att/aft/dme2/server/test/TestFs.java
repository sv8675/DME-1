/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.manager.registry.DME2JDBCEndpoint;
import com.att.aft.dme2.manager.registry.util.DME2Protocol;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.util.DME2Constants;

import junit.framework.TestCase;

/**
 * The Class TestFailover.
 */
public class TestFs extends TestCase {

	/** The bham_1_ launcher. */
	private ServerControllerLauncher bham_1_Launcher;

	/** The bham_2_ launcher. */
	private ServerControllerLauncher bham_2_Launcher;

	/** The char_1_ launcher. */
	private ServerControllerLauncher char_1_Launcher;
	
	private DME2Manager manager = null;


	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	@BeforeClass
	protected void setUp() throws Exception {
		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration("TestFs", props);			
		
		manager = new DME2Manager("TestFs", config);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	@AfterClass
	protected void tearDown() throws Exception {
		if (bham_1_Launcher != null) {
			bham_1_Launcher.destroy();
		}

		if (bham_2_Launcher != null) {
			bham_2_Launcher.destroy();
		}

		if (char_1_Launcher != null) {
			char_1_Launcher.destroy();
		}

	}



	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void testFsPublish() throws DME2Exception, InterruptedException, UnknownHostException {

		String service = "com.att.aft.dme2.TestFSPublish";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 3267;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName, null,hostname, port, latitude, longitude, "http");

		System.out.println("Service published successfully.");
		
		Thread.sleep(10000);
		
		List<DME2Endpoint> endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		
		DME2Endpoint found = null;
		for (DME2Endpoint ep : endpoints) {
			if ( ep.getHost().equals(hostname) && 
				 ep.getPort() == port &&
				 ep.getLatitude() == latitude &&
				 ep.getLongitude() == longitude ) {
				found = ep;
			}
		}
		System.out.println("Found registered endpoint: " + found);
		assertNotNull(found);
		
		svcRegistry.unpublish(serviceName, hostname, port);
	}
	
	
	public void testFsPublishJDBCEndpoint() throws DME2Exception, InterruptedException, UnknownHostException {

		String service = "com.att.aft.dme2.TestFSPublishJDBCEndpoint";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 3267;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		// Properties required to publish the first JDBC Endpoint
		Properties props = new Properties();
		props.setProperty(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME, "EODBD2CS");
		props.setProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER, "TEST_HEALTH_CHECK_USER");
		props.setProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_PASSWORD, "TEST_HEALTH_CHECK_PASS");
		props.setProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER, "MYSQL");
				
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName, null, hostname, port, DME2Protocol.DME2JDBC, props);

		System.out.println("Service published successfully.");
		
		Thread.sleep(10000);
		
		List<DME2Endpoint> endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		
		DME2JDBCEndpoint found = null;
		for (DME2Endpoint ep : endpoints) {
			DME2JDBCEndpoint jdbcEndpoint = (DME2JDBCEndpoint) ep;
			if (jdbcEndpoint.getHost().equals(hostname) && jdbcEndpoint.getPort() == port  && jdbcEndpoint.getDatabaseName().equals("EODBD2CS")
					&& jdbcEndpoint.getHealthCheckDriver().equals("MYSQL"))
			{
				found = jdbcEndpoint;
			}
		}
		System.out.println("Found registered endpoint: " + found);
		assertNotNull(found);
		
		svcRegistry.unpublish(serviceName, hostname, port);
	}

	/**
	 * test case that would test the defect about multiple endpoints published
	 * in same jvm, not being updated for lease
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void testFsMultipleEndpointsLease() throws DME2Exception, InterruptedException, UnknownHostException 
	{
		System.setProperty("DME2_EP_TTL_MS", "200");
		System.setProperty("DME2_RT_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_TTL_MS","200");
		System.setProperty("DME2_LEASE_REG_MS", "200");
		
		// Setting a lower value for renew, so that more frequent expiry updates happen.
		System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS","12000");
		//System.setProperty("DME2.DEBUG","true");

		String service = "com.att.aft.dme2.TestGRMMultipleEndpointsLease";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32672;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 

		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName, null,hostname, port, latitude, longitude, "http");
		// we need to publish 3 endpoints with same port, lat, long, but different context 
		svcRegistry.publish(serviceName,null, hostname, port+1, latitude, longitude, "http");
		svcRegistry.publish(serviceName,null, hostname, port+2, latitude, longitude, "http");
		
		System.out.println("Service published successfully.");
		
		Thread.sleep(1000);

		List<DME2Endpoint> endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		
		DME2Endpoint found = null;
		for (DME2Endpoint ep : endpoints)
		{
			if (ep.getHost().equals(hostname) && ep.getLatitude() == latitude && ep.getLongitude() == longitude)
			{
				found = ep;
				System.out.println("Found endpoints after publish " + ep);
			}
		}

		assertNotNull(found);
		Thread.sleep(1000);
		svcRegistry.refresh();
		Thread.sleep(3000);
		
		List<DME2Endpoint> leasedEndpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		
		for (DME2Endpoint leasedEp : leasedEndpoints)
		{
			System.out.println("Found endpoints after lease " + leasedEp);
			int leasedEpPort = leasedEp.getPort();
			
			/*SCLD-2627 - Adding the folloing assertion to validate that protocol is not null after updating lease*/
			assertNotNull(leasedEp.getProtocol());
			
			for (DME2Endpoint ep : endpoints)
			{
				int epPort = ep.getPort();
				if (leasedEpPort == epPort)
				{
					long pubEpLease = ep.getLease();
					long leasedEpLease = leasedEp.getLease();
					
					if (leasedEpLease <= pubEpLease)
					{
						// Endpoints have not refreshed, throw error
						fail("Leased endpoint for " + leasedEp.getHost() + ":" + leasedEp.getPort()	+ leasedEp.getPath() + " has expiration time=" + leasedEp.getLease()
								+ "which should have been greater than published Ep " + ep.getHost() + ":" + ep.getPort() + ep.getPath() + " expiration time=" + ep.getLease());
					}
					else
					{
						System.out.println("Leased endpoint for " + leasedEp.getHost() + ":" + leasedEp.getPort() + leasedEp.getPath() + " has expiration time=" + leasedEp.getLease()
								+ " is greater than published Ep " + ep.getHost() + ":" + ep.getPort() + ep.getPath() + " expiration time=" + ep.getLease());
					}
				}
			}
		}
		
		svcRegistry.unpublish(serviceName, hostname, port);
		//Thread.sleep(3000);
		svcRegistry.unpublish(serviceName, hostname, port+1);
		//Thread.sleep(3000);
		svcRegistry.unpublish(serviceName, hostname, port+2);
		System.out.println("Service unpublished successfully.");
		//svcRegistry.refresh();
		//Thread.sleep(61000);
		svcRegistry.refresh();
		Thread.sleep(3000);

		List<DME2Endpoint> ups = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		found = null;
		for (DME2Endpoint ep1 : ups) {
			if ( ep1.getHost().equals(hostname) && 
				 ep1.getPort() == port &&
				 ep1.getLatitude() == latitude &&
				 ep1.getLongitude() == longitude ) {
				found = ep1;
				System.out.println("Found endpoints after unpublish ;" + ep1);
			}
		}
		System.out.println("Found registered endpoint - should be null: " + found);
		assertNull(found);
		
	}
	
	
	/**
	 * test case that would test the defect about multiple endpoints published
	 * in same jvm, not being updated for lease
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void testFsMultipleEndpointsRemovedAfterLease() throws DME2Exception, InterruptedException, UnknownHostException {
		System.setProperty("DME2_EP_TTL_MS", "200");
		System.setProperty("DME2_RT_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_TTL_MS","200");
		System.setProperty("DME2_LEASE_REG_MS", "200");
		
		// Setting a lower value for renew, so that more frequent expiry updates happen.
		System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS","12000");
		//System.setProperty("DME2.DEBUG","true");

		String service = "com.att.aft.dme2.TestGRMMultipleEndpointsRemovedAfterLease";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32672;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 

		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName, null,hostname, port, latitude, longitude, "http");
		// we need to publish 3 endpoints with same port, lat, long, but different context 
		svcRegistry.publish(serviceName,null, hostname, port+1, latitude, longitude, "http");
		svcRegistry.publish(serviceName,null, hostname, port+2, latitude, longitude, "http");

		System.out.println("Service published successfully.");
		
		Thread.sleep(1000);

		List<DME2Endpoint> endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		
		DME2Endpoint found = null;
		for (DME2Endpoint ep : endpoints) {
			if ( ep.getHost().equals(hostname) && 
				 ep.getLatitude() == latitude &&
				 ep.getLongitude() == longitude ) {
				found = ep;
				System.out.println("Found endpoints after publish " + ep);
			}
		}
		//System.out.println("Found registered endpoint: " + found);
		assertNotNull(found);
		Thread.sleep(1000);
		svcRegistry.refresh();
		Thread.sleep(3000);
		List<DME2Endpoint> leasedEndpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		//svcRegistry.

		int leasedCount=0;
		for (DME2Endpoint leasedEp : leasedEndpoints) {
			System.out.println("Found endpoints after lease " +leasedEp);
			int leasedEpPort = leasedEp.getPort();
			for (DME2Endpoint ep : endpoints) {
				int epPort = ep.getPort();
				if(leasedEpPort == epPort) {
					leasedCount++;
					long pubEpLease = ep.getLease();
					long leasedEpLease = leasedEp.getLease();
					if(leasedEpLease <= pubEpLease) {
						// Endpoints have not refreshed, throw error
						fail("Leased endpoint for " + leasedEp.getHost() + ":" + leasedEp.getPort() + leasedEp.getPath() + " has expiration time="+ leasedEp.getLease()
								+"which should have been greater than published Ep " + ep.getHost() + ":" + ep.getPort() + ep.getPath() + " expiration time=" + ep.getLease()
								);
					}
					else {
						System.out.println("Leased endpoint for " + leasedEp.getHost() + ":" + leasedEp.getPort() + leasedEp.getPath() + " has expiration time="+ leasedEp.getLease()
						+" is greater than published Ep " + ep.getHost() + ":" + ep.getPort() + ep.getPath() + " expiration time=" + ep.getLease()
						);
					}
				}
			}	
		}
		assertTrue(leasedCount==3);
		svcRegistry.unpublish(serviceName, hostname, port);
		//Thread.sleep(3000);
		svcRegistry.unpublish(serviceName, hostname, port+1);
		//Thread.sleep(3000);
		svcRegistry.unpublish(serviceName, hostname, port+2);
		System.out.println("Service unpublished successfully.");
		//svcRegistry.refresh();
		//Thread.sleep(61000);
		svcRegistry.refresh();
		Thread.sleep(3000);

		List<DME2Endpoint> ups = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		found = null;
		for (DME2Endpoint ep1 : ups) {
			if ( ep1.getHost().equals(hostname) && 
				 ep1.getPort() == port &&
				 ep1.getLatitude() == latitude &&
				 ep1.getLongitude() == longitude ) {
				found = ep1;
				System.out.println("Found endpoints after unpublish ;" + ep1);
			}
		}
		System.out.println("Found registered endpoint - should be null: " + found);
		assertNull(found);
		
	}


	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void testFsUnpublish() throws DME2Exception, InterruptedException, UnknownHostException {
		System.setProperty("DME2_EP_TTL_MS", "200");
		System.setProperty("DME2_RT_TTL_MS", "200");
		System.setProperty("DME2_LEASE_REG_MS", "200");
		
		

		String service = "com.att.aft.dme2.TestFSUnpublish";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32672;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName, null,hostname, port, latitude, longitude, "http");

		System.out.println("Service published successfully.");
		
		Thread.sleep(10000);

		List<DME2Endpoint> endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		
		DME2Endpoint found = null;
		for (DME2Endpoint ep : endpoints) {
			if ( ep.getHost().equals(hostname) && 
				 ep.getPort() == port &&
				 ep.getLatitude() == latitude &&
				 ep.getLongitude() == longitude ) {
				found = ep;
			}
		}
		System.out.println("Found registered endpoint: " + found);
		assertNotNull(found);
	
		svcRegistry.unpublish(serviceName, hostname, port);
		System.out.println("Service unpublished successfully.");
		svcRegistry.refresh();
		Thread.sleep(3000);
		//cliRegistry.refreshCachedDME2Endpoints();
		Thread.sleep(3000);

		endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		found = null;
		for (DME2Endpoint ep : endpoints) {
			if ( ep.getHost().equals(hostname) && 
				 ep.getPort() == port &&
				 ep.getLatitude() == latitude &&
				 ep.getLongitude() == longitude ) {
				found = ep;
			}
		}
		System.out.println("Found registered endpoint - should be null: " + found);
		assertNull(found);

	}
	
	public void testFsUnpublishJDBCEndpoint() throws DME2Exception, InterruptedException, UnknownHostException {
		System.setProperty("DME2_EP_TTL_MS", "200");
		System.setProperty("DME2_RT_TTL_MS", "200");
		System.setProperty("DME2_LEASE_REG_MS", "200");
		
		

		String service = "com.att.aft.dme2.TestFSUnpublish";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32672;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		// Properties required to publish the first JDBC Endpoint
		Properties props = new Properties();
		props.setProperty(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME, "EODBD2CS");
		props.setProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER, "TEST_HEALTH_CHECK_USER");
		props.setProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_PASSWORD, "TEST_HEALTH_CHECK_PASS");
		props.setProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER, "MYSQL");
				
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName, null,hostname, port, DME2Protocol.DME2JDBC, props);

		System.out.println("Service published successfully.");
		
		Thread.sleep(10000);

		List<DME2Endpoint> endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		
		DME2JDBCEndpoint found = null;
		for (DME2Endpoint ep : endpoints) {
			DME2JDBCEndpoint jdbcEndpoint = (DME2JDBCEndpoint) ep;
			if (jdbcEndpoint.getHost().equals(hostname) && jdbcEndpoint.getPort() == port  && jdbcEndpoint.getDatabaseName().equals("EODBD2CS")
					&& jdbcEndpoint.getHealthCheckDriver().equals("MYSQL"))
			{
				found = jdbcEndpoint;
			}
		}
		
		System.out.println("Found registered endpoint: " + found);
		assertNotNull(found);
	
		svcRegistry.unpublish(serviceName, hostname, port);
		System.out.println("Service unpublished successfully.");
		svcRegistry.refresh();
		Thread.sleep(3000);

		endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		found = null;
		
		for (DME2Endpoint ep : endpoints) {
			DME2JDBCEndpoint jdbcEndpoint = (DME2JDBCEndpoint) ep;
			if (jdbcEndpoint.getHost().equals(hostname) && jdbcEndpoint.getPort() == port  && jdbcEndpoint.getDatabaseName().equals("EODBD2CS")
					&& jdbcEndpoint.getHealthCheckDriver().equals("MYSQL"))
			{
				found = jdbcEndpoint;
			}
		}
		System.out.println("Found registered endpoint - should be null: " + found);
		assertNull(found);

	}
	
	@Ignore
	@Test
	public void request() throws Exception {
		System.setProperty("DME2_EP_TTL_MS", "300000");
		System.setProperty("DME2_RT_TTL_MS", "300000");
		System.setProperty("DME2_LEASE_REG_MS", "300000");

		// run the server in bham.
		String[] bham_1_bau_se_args = {
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
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se" };
		bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
		bham_1_Launcher.launch();

		String[] bham_2_bau_se_args = {
				"-serverHost",
				"brcbsp02",
				"-serverPort",
				"4601",
				"-registryType",
				"FS",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se" };
		bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
		bham_2_Launcher.launch();

		/*
		 * String[] char_1_bau_se_args = { "-serverHost", "crcbsp01",
		 * "-serverPort", "4600", "-registryType", "FS", "-servletClass",
		 * "EchoServlet", "-serviceName",
		 * "service=com.att.aft.dme2.MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE"
		 * , "-serviceCity", "CHAR", "-serverid", "char_1_bau_se" };
		 * char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);
		 * char_1_Launcher.launch();
		 */

		try {
			Thread.sleep(10000);
		} catch (Exception ex) {
		}
		// try to call a service we just registered
		Locations.CHAR.set();
		String uriStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/partner=TEST/routeOffer=BAU_SE";

		//DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		//sender.setPayload("this is a test");
		EchoReplyHandler replyHandler = new EchoReplyHandler();
//		sender.setReplyHandler(replyHandler);
//		sender.send();

		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(300000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);

		DME2Payload payload = new DME2TextPayload("TEST IS A TEST");

		sender.setResponseHandlers(replyHandler);
		sender.send(payload);
		
		String reply = replyHandler.getResponse(60000);
		System.out.println("Reply on first request=" + reply);
		// stop server that replied
		String otherServer = null;
		if (reply == null) {
			fail("first reply is null");
		} else if (reply.indexOf("bham_1_bau_se") != -1) {
			System.out.println(" Destroying bham_1_bau_se" );
			bham_1_Launcher.destroy();
			otherServer = "bham_2_bau_se";
		} else if (reply.indexOf("bham_2_bau_se") != -1) {
			System.out.println(" Destroying bham_2_bau_se" );
			bham_2_Launcher.destroy();
			otherServer = "bham_1_bau_se";
		} else {
			fail("reply is not from bham_1_bau_se or bham_2_bau_se.  reply="
					+ reply);
		}

		Thread.sleep(20000);

//		sender = new DME2Client(manager, new URI(uriStr), 30000);
//		sender.setPayload("this is a test");
		replyHandler = new EchoReplyHandler();
//		sender.setReplyHandler(replyHandler);
//		sender.send();

		request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(300000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		sender = new DME2Client(manager, request);

		payload = new DME2TextPayload("TEST IS A TEST");

		sender.setResponseHandlers(replyHandler);
		sender.send(payload);
		
		reply = replyHandler.getResponse(60000);
		System.out.println("reply on next request=" + reply);
		// reply should be from other server...
		if (reply == null || reply.indexOf(otherServer) == -1) {
			fail("reply is null or not from the otherServer.  otherServer="
					+ otherServer + "  reply=" + reply);
		}

	}
	
	
	public void testContextPathWithFileRegistry() 
	{
		/* SCLD-2357 */
		DME2ServiceHolder serviceHolder = null;
		String serviceURI = "service=com.att.aft.dme2.test.TestContextPathWithFileRegistry/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		
		try
		{			
			serviceHolder = new DME2ServiceHolder();
			serviceHolder.setManager(manager);
			serviceHolder.setContext("/testContextPath/abc/def");
			serviceHolder.setServiceURI(serviceURI);
			serviceHolder.setServlet(new EchoResponseServlet("ID_1", "serviceURI"));
			
			manager.getServer().start();
			manager.publish(serviceHolder);
			System.out.println("////////////////// SUCCESSFULLY PUBLISHED SERVICE: " + serviceURI + "////////////////// ");
			
			Thread.sleep(2000);
			
			List<DME2Endpoint> endpoints = manager.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestContextPathWithFileRegistry", "1.0.0", "PROD", "BAU_SE");
			assertEquals(endpoints.get(0).getContextPath(), "/testContextPath/abc/def");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				manager.unpublish(serviceHolder);
			}
			catch (DME2Exception e)
			{
				
			}
		}

	}
	
	public void testContextPathWithFileRegistry_NoContextProvided() 
	{
		//System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		/* SCLD-2357 */
		DME2ServiceHolder serviceHolder = null;
		String serviceURI = "/service=com.att.aft.dme2.test.TestNoContextPathProvidedWithFileRegistry/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String clientURI = "http://DME2RESOLVE" + serviceURI;
		
		try
		{			
			serviceHolder = new DME2ServiceHolder();
			serviceHolder.setManager(manager);
			serviceHolder.setServiceURI(serviceURI);
			serviceHolder.setServlet(new EchoResponseServlet("ID_1", serviceURI));
			serviceHolder.disableMetricsFilter();
			
			manager.getServer().addService(serviceHolder);
			manager.getServer().start();
			//manager.publish(serviceHolder);
			System.out.println("////////////////// SUCCESSFULLY PUBLISHED SERVICE: " + serviceURI + "////////////////// ");
			
			Thread.sleep(2000);
			
			List<DME2Endpoint> endpoints = manager.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestNoContextPathProvidedWithFileRegistry", "1.0.0", "PROD", "BAU_SE");
			assertEquals(serviceURI, endpoints.get(0).getContextPath());
			
			
//			DME2Client client = new DME2Client(manager, new URI(clientURI), 30000);
//			client.setPayload("TEST");
//			String response =  client.sendAndWait(30000);
//			System.out.println(response);

			Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("POST").withReadTimeout(300000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();

			DME2Client sender = new DME2Client(manager, request);

			DME2Payload payload = new DME2TextPayload("TEST IS A TEST");

			String response =  (String) sender.sendAndWait(payload);
			
			
			assertNotNull(response);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				manager.unpublish(serviceHolder);
			}
			catch (DME2Exception e)
			{
				
			}
		}

	}
	
	public static void main(String a[]) throws Exception {
		TestFs fs = new TestFs();
		fs.setUp();
		fs.testFsMultipleEndpointsLease();
	}

}
