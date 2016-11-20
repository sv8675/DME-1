/*
 * Copyright 2016 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.DME2SimpleReplyHandler;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.util.DME2Constants;

/**
 * The Class TestFailover.
 */
@Ignore
public class TestGrmFailover extends DME2BaseTestCase {

	/** The bham_1_ launcher. */
	private ServerControllerLauncher bham_1_Launcher;

	/** The bham_2_ launcher. */
	private ServerControllerLauncher bham_2_Launcher;

	/** The char_1_ launcher. */
	private ServerControllerLauncher char_1_Launcher;
	
	private DME2Manager manager = null;


	@Before
	public void setUp() {
    super.setUp();
		Properties props = null;
		try {
			props = RegistryFsSetup.init();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DME2Configuration config = new DME2Configuration("TestGrm", props);
    try {
      manager = new DME2Manager("TestGrm", config);
    } catch ( DME2Exception e ) {
      throw new RuntimeException( e );
    }
  }

	@After
	public void tearDown() {
    System.clearProperty("AFT_DME2_GRM_URLS");
    System.clearProperty("AFT_DME2_DEBUG_GRM_EPS");
    cleanup( bham_1_Launcher );
    cleanup( bham_2_Launcher );
    cleanup( char_1_Launcher );
    super.tearDown();
	}

  private void cleanup(ServerControllerLauncher launcher){
    if(launcher != null){
      launcher.destroy();
    }
  }

	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void testGrmPublish() throws Exception {

		String service = "com.att.aft.dme2.TestGrmPublish";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 3267;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "/service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
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
	

	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void testGrmUnpublish() throws Exception
	{
		Properties props = new Properties();
		props.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		props.setProperty("DME2_ROUTEINFO_CACHE_TTL_MS", "200");
		props.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "200");

	
		DME2Configuration config = new DME2Configuration("testGrmUnpublish1", props);			
		DME2Manager manager = new DME2Manager("testGrmUnpublish1", config);
	
//		DME2Manager manager = new DME2Manager("testGrmUnpublish1", new Properties());
		String service = "com.att.aft.dme2.TestGrmUnpublish1";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		
		int port = 32672;
		int port_2 = 32673;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "BAU_SE";
		String serviceName = "/service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		
		try
		{
			svcRegistry.publish(serviceName, null, hostname, port, latitude, longitude, "http");
			System.out.println("Successfully published the first service with port: " + port);
			
			svcRegistry.publish(serviceName, null, hostname, port_2, latitude, longitude, "http");
			System.out.println("Successfully published the first service with port: " + (port_2));
			
			Thread.sleep(7000);

			List<DME2Endpoint> endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
			System.out.println("Number of Endpoints returned from findEndpoints() method (Should be 2): " + endpoints.size());
			assertEquals(2, endpoints.size());
			
			DME2Endpoint found = null;
			
			for (DME2Endpoint ep : endpoints)
			{
				if (ep.getHost().equals(hostname) && ep.getPort() == port && ep.getLatitude() == latitude
						&& ep.getLongitude() == longitude)
				{
					found = ep;
				}
			}
			
			System.out.println("Found registered endpoint: " + found);
			assertNotNull(found);

			svcRegistry.unpublish(serviceName, hostname, port);
			System.out.println("Service unpublished successfully with port: " + port);

			Thread.sleep(7000);

			endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
			System.out.println("Number of Endpoints returned from findEndpoints() method after unpublishing the first service (Should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			found = null;
			
			for (DME2Endpoint ep : endpoints)
			{
				if (ep.getHost().equals(hostname) && ep.getPort() == port_2 && ep.getLatitude() == latitude
						&& ep.getLongitude() == longitude)
				{
					found = ep;
				}
			}
			
			System.out.println("Found registered endpoint: " + found);
			assertNotNull(found);
			
			svcRegistry.unpublish(serviceName, hostname, port_2);
			System.out.println("Service unpublished successfully with port: " + port_2);
			
			endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
			System.out.println("Number of Endpoints returned from findEndpoints() method after unpublishing the second service (Should be 0): " + endpoints.size());
			assertEquals(0, endpoints.size());
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
				svcRegistry.unpublish(serviceName, hostname, port);
			}
			catch(Exception e)
			{
				
			}
			
			try
			{
				svcRegistry.unpublish(serviceName, hostname, port_2);
			}
			catch(Exception e)
			{
				
			}
		}

	}
	
	public void GRMFailover_testRequest() throws Exception {
		//System.setProperty("DME2_SEP_CACHE_TTL_MS", "300000");
		//System.setProperty("DME2_ROUTEINFO_CACHE_TTL_MS", "300000");
		//System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "300000");

		Properties props = new Properties();
		props.setProperty("DME2_SEP_CACHE_TTL_MS", "300000");
		props.setProperty("DME2_ROUTEINFO_CACHE_TTL_MS", "300000");
		props.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "300000");
		
		DME2Configuration config = new DME2Configuration("GRMFailover_testRequest", props);			
		DME2Manager manager = new DME2Manager("GRMFailover_testRequest", config);
		
//		DME2Manager manager = new DME2Manager("GRMFailover_testRequest",props);
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost",
				"brcbsp01",
				"-serverPort",
				"4600",
				"-registryType",
				"GRM",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se" };
		bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
		bham_1_Launcher.launch();

		String[] bham_2_bau_se_args = {
				"-serverHost",
				"brcbsp02",
				"-serverPort",
				"4600",
				"-registryType",
				"GRM",
				"-servletClass",
				"EchoServlet",
				"-serviceName",
				"service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
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
			Thread.sleep(5000);
		} catch (Exception ex) {
		}
		// try to call a service we just registered
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";

		
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		
//		DME2Client sender = new DME2Client(manager,new URI(uriStr), 30000);
		DME2Payload payload = new DME2TextPayload("this is a test");
//		sender.setPayload("this is a test");
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(payload);

		String reply = replyHandler.getResponse(60000);
		System.out.println(reply);
		// stop server that replied
		String otherServer = null;
		if (reply == null) {
			fail("first reply is null");
		} else if (reply.indexOf("bham_1_bau_se") != -1) {
			bham_1_Launcher.destroy();
			otherServer = "bham_2_bau_se";
		} else if (reply.indexOf("bham_2_bau_se") != -1) {
			bham_2_Launcher.destroy();
			otherServer = "bham_1_bau_se";
		} else {
			fail("reply is not from bham_1_bau_se or bham_2_bau_se.  reply="
					+ reply);
		}

		Thread.sleep(5000);

		request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		sender = new DME2Client(manager, request);
		
		//sender = new DME2Client(new URI(uriStr), 30000);
		payload = new DME2TextPayload("this is a test");

//		sender.setPayload("this is a test");
		replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(payload);
		reply = replyHandler.getResponse(60000);
		System.out.println("reply=" + reply);
		// reply should be from char server...
		if (reply == null || reply.indexOf(otherServer) == -1) {
			fail("reply is null or not from the otherServer.  otherServer="
					+ otherServer + "  reply=" + reply);
		}

	}
	
	
	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void testGrmPublish_failover() throws DME2Exception, InterruptedException, IOException {
		
		Properties props = RegistryFsSetup.init();
		props.setProperty("AFT_ENVIRONMENT","AFTUAT");
		props.setProperty("AFT_LATITUDE","80.5");
		props.setProperty("AFT_LONGITUDE","33.4");
		props.setProperty("DME2.DEBUG","true");
		props.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_FAILOVER_URLS_TO_USE);
		
		DME2Configuration config = new DME2Configuration("TestGrmPublish_failover", props);			

		DME2Manager manager = new DME2Manager("TestGrmPublish_failover", config);
		
//		DME2Manager manager = new DME2Manager("TestGrmPublish_failover", props);

		String service = "com.att.aft.dme2.TestGrmPublish_Failover";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32673;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "/service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 

		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName,null, hostname, port, latitude, longitude, "http");

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
		manager.stop();
	}
	
	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void testGrmUnpublish_failover() throws DME2Exception, InterruptedException, IOException {
		
		Properties props = RegistryFsSetup.init();
		props.setProperty("AFT_ENVIRONMENT","AFTUAT");
		props.setProperty("AFT_LATITUDE","80.5");
		props.setProperty("AFT_LONGITUDE","33.4");
		props.setProperty("DME2.DEBUG","true");
		props.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_FAILOVER_URLS_TO_USE);
		props.setProperty("DME2_SEP_CACHE_TTL_MS", "2000");
		props.setProperty("DME2_ROUTEINFO_CACHE_TTL_MS", "200");
		props.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "200");

		
		DME2Configuration config = new DME2Configuration("TestGrmUnpublish_failover", props);			

		DME2Manager manager = new DME2Manager("TestGrmUnpublish_failover", config);
		
//		DME2Manager manager = new DME2Manager("TestGrmUnpublish_failover", props);

		String service = "com.att.aft.dme2.TestGrmUnpublish_failover";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32674;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName,null, hostname, port, latitude, longitude, "http");
		//svcRegistry.publish(serviceName,null, hostname, port+1, latitude, longitude, "http");

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
		//svcRegistry.refresh();
		//Thread.sleep(3000);
		svcRegistry.refresh();
		Thread.sleep(5000);

		endpoints  = svcRegistry.findEndpoints(service, version, envContext, routeOffer);
		found = null;
		for (DME2Endpoint ep : endpoints) {
			if ( ep.getHost().equals(hostname) && 
				 ep.getPort() == port &&
				 ep.getLatitude() == latitude &&
				 ep.getLongitude() == longitude ) {
				System.out.println(" Endpoint name="+ep.getServiceName());
				found = ep;
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
	 * @throws IOException 
	 */
	public void testGrmUnavailable() throws DME2Exception, InterruptedException, IOException {
		System.setProperty("com.att.aft.discovery.client.traceEnabled", "true");
		System.setProperty("com.att.aft.discovery.client.provider.suspensionMins", "0");
		System.setProperty("com.att.aft.discovery.client.provider.expirationMillis", "1");
		Properties props = RegistryFsSetup.init();
		props.setProperty("AFT_ENVIRONMENT","AFTUAT");
		props.setProperty("AFT_LATITUDE","80.5");
		props.setProperty("AFT_LONGITUDE","33.4");
		props.setProperty("DME2.DEBUG","true");
		props.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		props.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
		//System.setProperty("AFT_DME2_GRM_URLS", "http://zldv0432.vci.att.com:9127/GRMLWPService/v1,http://zldv0432.vci.att.com:9127/GRMLWPService/v1");
		//System.setProperty("AFT_DME2_GRM_URLS", "http://sarek.mo.sbc.com:917/GRMLWPService/v1,http://sarek.mo.sbc.com:9127/GRMLWPService/v1");
		//props.setProperty("DME2_EP_TTL_MS", "200000");
		//props.setProperty("DME2_RT_TTL_MS", "200000");
		//props.setProperty("DME2_LEASE_REG_MS", "200000");
		
		DME2Configuration config = new DME2Configuration("TestGrmUnavailable", props);			

		DME2Manager manager = new DME2Manager("TestGrmUnavailable", config);
		
		
	//	DME2Manager manager = new DME2Manager("TestGrmUnavailable", props);

		String service = "com.att.aft.dme2.TestGrmUnavailable";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32675;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName, null,hostname, port, latitude, longitude, "http");
		Thread.sleep(1000);
		svcRegistry.publish(serviceName,null, hostname, port+1, latitude, longitude, "http");

		System.out.println("testGrmUnavailable Service published successfully.");
		
		Thread.sleep(2000);

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
		System.err.println("testGrmUnavailable Found registered endpoint: " + found);
		assertNotNull(found);
		
		System.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_FAILOVER_URLS_TO_USE);
		System.setProperty("AFT_DME2_FORCE_GRM_LOOKUP", "true");
		long leaseExpectedStart = System.currentTimeMillis();
		// a long sleep to let refreshAllCachedEndpoint 
		Thread.sleep(61000);
		
//		long cacheUpdatedAt = svcRegistry.getEndpointCacheUpdatedAt();
//		if(cacheUpdatedAt >0){
//			assertTrue(cacheUpdatedAt-leaseExpectedStart<=61000);
//			System.out.println("testGrmUnavailable: Cached endpoints last refreshed at " + new Date(cacheUpdatedAt));
//		}
	
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
		System.out.println("testGrmUnavailable Found registered endpoint after refresh: " + found);
		assertNotNull(found);
		System.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
		boolean failed = false;
		Thread.sleep(3000);
		try {
		svcRegistry.unpublish(serviceName, hostname, port);
		System.out.println("testGrmUnavailable Service unpublished successfully.");
		}catch(Exception e) {
			failed = true;
			System.out.println(" Error in unpublishing. Exception message: " + e.getMessage());
		}
		svcRegistry.refresh();
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
		System.out.println("testGrmUnavailable Found registered endpoint - should be null : " + found);
		assertNull(found);
		props.clear();
		System.clearProperty("com.att.aft.discovery.client.traceEnabled");
		System.clearProperty("com.att.aft.discovery.client.provider.suspensionMins");
		System.clearProperty("com.att.aft.discovery.client.provider.expirationMillis");

	}
	
	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void testGRMConnectTimeout() throws DME2Exception, InterruptedException, IOException {
		System.setProperty("com.att.aft.discovery.client.traceEnabled", "true");
		System.setProperty("com.att.aft.discovery.client.provider.suspensionMins", "0");
		System.setProperty("com.att.aft.discovery.client.provider.expirationMillis", "1");
		Properties props = RegistryFsSetup.init();
		props.setProperty("AFT_ENVIRONMENT","AFTUAT");
		props.setProperty("AFT_LATITUDE","80.5");
		props.setProperty("AFT_LONGITUDE","33.4");
		props.setProperty("DME2.DEBUG","true");
		props.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		props.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
	
		
		DME2Configuration config = new DME2Configuration("TestGrmUnavailableTimeout", props);			

		DME2Manager manager = new DME2Manager("TestGrmUnavailableTimeout", config);
		
		
//		DME2Manager manager = new DME2Manager("TestGrmUnavailableTimeout", props);

		String service = "com.att.aft.dme2.TestGrmUnavailableTimeout";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 32676;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer; 
		
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		//svcRegistry.unpublish(serviceName, hostname, port);
		long startTime = System.currentTimeMillis();
		svcRegistry.publish(serviceName, null,hostname, port, latitude, longitude, "http");
		long elapsed = System.currentTimeMillis() - startTime;
		System.out.println("Elapsed time with wrong url in grm endpoints" + elapsed);
		// connect timeout is 5 secs and read timeout is 60 secs now.
		// elapsed on failover with wrong should be less than 5 secs and grm elapsed should not go beyond 1 min
		assertTrue ( elapsed < 61000);
		svcRegistry.unpublish(serviceName, hostname, port);
	}
	
	public void testGRMFailover_RetryStaleOffers() 
	{
		/* Attempts to invoke GRM using 1 valid URL and 2 invalid ones. 
		   The invalid ones are tried first and should finally failover to the valid one.
		   The invalid URLs will be marked stale, leaving only one active valid URL.
		   On the next call to GRM (Invoked by the DME2Client), GRMAccessor will add the stale URLs
		   to the list containing the active URL. The stale endpoints should be placed at the END of the list
		   so that they are tried last. Also, the read timeout for the next call is set very low to induce failover 
		   on the valid endpoint. This should then attempt to use the two other endpoints in the list (which were previously
		   marked stale, but added back to the end of the active list). These two URLs will fail leaded to a [AFT-DME2-0902] exception
		   */


		System.setProperty("AFT_DME2_GRM_URLS",
				"http://invalid1.mo.sbc.com:9127/GRMLWPService/v1, " +
						"http://invalid2.mo.sbc.com:9127/GRMLWPService/v1, " +
						TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
		
		System.setProperty("AFT_DME2_DEBUG_GRM_EPS", "true");
		System.setProperty("AFT_DME2_COLLECT_SERVICE_STATS", "false");
		
		String serviceURI = "/service=com.att.aft.dme2.test.TestGRMFailoverRetryWithStaleOffers/version=1.0.0/envContext=LAB/routeOffer=TEST";
		String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestGRMFailoverRetryWithStaleOffers/version=1.0.0/envContext=LAB/routeOffer=TEST";
		
		try
		{	
			manager.disableMetrics();
			manager.disableMetricsFilter();
			manager.bindServiceListener(serviceURI, new EchoServlet(serviceURI, "TestGRMFailover"));
			
			Thread.sleep(5000);
			
//			List<String> attemptedOffers = GRMServiceAccessor.getAttemptedOffers();
//			System.err.println("Attempted Offers" + attemptedOffers.toString());
			//TODO: Need to revisit this logic of attemptedOffers since offers is static and 
			// can be updated by multiple threads at sametime
			//assertTrue(attemptedOffers.size() == 3);
//			assertTrue(attemptedOffers.contains("http://zldv0432.vci.att.com:9127/GRMLWPService/v1"));
			
//			BaseAccessor.readTimeout = 1;
			
			DME2SimpleReplyHandler handler = new DME2SimpleReplyHandler(manager.getConfig(), "serviceURI", false);

			Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();

			DME2Client client = new DME2Client(manager, request);
			
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
//			DME2Client client = new DME2Client(manager, new URI(clientURI), 30000);
			//client.setPayload("THIS IS A TEST");
			client.setResponseHandlers(handler);
			client.send(payload);
			
		}
		catch (Exception e)
		{
//			List<String> attemptedOffers = GRMServiceAccessor.getAttemptedOffers();
//			System.err.println("Attempted Offers" + attemptedOffers.toString());
			
			//assertTrue(attemptedOffers.size() == 3);
//			assertTrue(attemptedOffers.contains("http://zldv0432.vci.att.com:9127/GRMLWPService/v1"));
//			assertTrue(attemptedOffers.contains("http://invalid1.mo.sbc.com:9127/GRMLWPService/v1"));
//			assertTrue(attemptedOffers.contains("http://invalid2.mo.sbc.com:9127/GRMLWPService/v1"));
			
			//assertTrue(attemptedOffers.get(0).contains("http://zldv0432.vci.att.com:9127/GRMLWPService/v1"));
			assertTrue(e.getMessage().contains("[AFT-DME2-0902]"));
		}
		finally
		{
			try
			{
				manager.unbindServiceListener(serviceURI);
			}
			catch (DME2Exception e)
			{
				
			}
		}
	}
	
	public void testGRMTopologyFailover_RetryStaleOffers() 
	{
		/* Attempts to invoke GRM using 1 valid URL and 2 invalid ones. 
		   The invalid ones are tried first and should finally failover to the valid one.
		   The invalid URLs will be marked stale, leaving only one active valid URL.
		   On the next call to GRM (Invoked by the DME2Client), GRMAccessor will add the stale URLs
		   to the list containing the active URL. The stale endpoints should be placed at the END of the list
		   so that they are tried last. Also, the read timeout for the next call is set very low to induce failover 
		   on the valid endpoint. This should then attempt to use the two other endpoints in the list (which were previously
		   marked stale, but added back to the end of the active list). These two URLs will fail leaded to a [AFT-DME2-0902] exception
		   */


		System.setProperty("AFT_DME2_GRM_TOPOLOGY_URLS",
				"http://invalid1.mo.sbc.com:9127/GRMLWPService/v1, " +
						"http://invalid2.mo.sbc.com:9127/GRMLWPService/v1, http://zld01854.vci.att.com:8080/rest");
		
		System.setProperty("AFT_DME2_DEBUG_GRM_EPS", "true");
		System.setProperty("KEY_ENABLE_GRM_TOPOLOGY_SERVICE_OVERRIDE", "true");
		
		DME2Manager mgr = null;
		String serviceURI = "/service=com.att.aft.dme2.test.TestGRMFailoverRetryWithStaleOffers/version=1.0.0/envContext=LAB/routeOffer=TEST";
		String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestGRMFailoverRetryWithStaleOffers/version=1.0.0/envContext=LAB/routeOffer=TEST";
		
		try
		{	Properties props = RegistryFsSetup.init(); 
			props.put(DME2Constants.KEY_ENABLE_GRM_TOPOLOGY_SERVICE, "true");
			
			DME2Configuration config = new DME2Configuration("TestGrm2", props);			

			mgr = new DME2Manager("TestGrm2", config);
			
	//		mgr = new DME2Manager("TestGrm2", props);
			mgr.disableMetrics();
			mgr.disableMetricsFilter();
			mgr.bindServiceListener(serviceURI, new EchoServlet(serviceURI, "TestGRMFailover"));
			
			
//			List<String> attemptedOffers = GRMTopologyAccessor.getAttemptedOffers();
//			System.err.println("Attempted Offers" + attemptedOffers.toString());
			
			//assertTrue(attemptedOffers.size() == 3);
			//assertTrue(attemptedOffers.get(2).contains("http://zldv0432.vci.att.com:8080/rest"));
			
//			BaseAccessor.readTimeout = 1;
			
			DME2SimpleReplyHandler handler = new DME2SimpleReplyHandler(config, "serviceURI", false);
			
			Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();

			DME2Client client = new DME2Client(mgr, request);
			
//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
//			client.setPayload("THIS IS A TEST");
			
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			client.setResponseHandlers(handler);
			client.send(payload);
			
		}
		catch (Exception e)
		{
			//TODO: This won't work until we enable use of TopologyAccessor 
			
			//List<String> attemptedOffers = GRMTopologyAccessor.getAttemptedOffers();
			//System.err.println("Attempted Offers" + attemptedOffers.toString());
			
			//assertTrue(attemptedOffers.size() == 3);
			//assertTrue(attemptedOffers.contains("http://zldv0432.vci.att.com:8080/rest"));
			
			//assertTrue(attemptedOffers.contains("http://invalid1.mo.sbc.com:9127/GRMLWPService/v1"));
			//assertTrue(attemptedOffers.contains("http://invalid2.mo.sbc.com:9127/GRMLWPService/v1"));
			
			//assertTrue(e.getMessage().contains("[AFT-DME2-0902]"));
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(serviceURI);
			}
			catch (DME2Exception e)
			{
				
			}
		}
	}


	public static void main(String a[]) throws Exception {
		TestGrmFailover fl = new TestGrmFailover();
		fl.setUp();
		fl.testGrmUnavailable();
	}

}
