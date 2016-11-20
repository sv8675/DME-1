/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.net.URI;
import java.util.Properties;
import java.util.Set;

import javax.management.ObjectName;

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
import com.att.aft.dme2.test.GetDME2JMXDetails;
import com.att.aft.dme2.test.Locations;

import junit.framework.Assert;

/**
 * The Class TestVersions.
 */
public class TestDME2JMXBean {
	
	private static ServerControllerLauncher char_1_1_1_Launcher;

	private static DME2Manager manager = null;

	/**
	 * Inits the.
	 */
	private static void init() {
		// run the server in bham.
		String[] char_1_1_1_bau_se_args = {
		        "-Dcom.sun.management.jmxremote.authenticate=false",
		        "-Dcom.sun.management.jmxremote.ssl=false",
		        "-Dcom.sun.management.jmxremote.port=5000",								
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
				"-serviceCity", "CHAR", "-serverid", "char_1_1_1_bau_se"
				};
		char_1_1_1_Launcher = new ServerControllerLauncher(char_1_1_1_bau_se_args);
		char_1_1_1_Launcher.launch();
		
		try {
			Thread.sleep(20000);
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

		if (char_1_1_1_Launcher != null) {
			char_1_1_1_Launcher.destroy();
		}

	}



	@SuppressWarnings("deprecation")
	private void tryJMXBeans(String version, String expected) throws Exception {
		String versionText = "version=" + version;
		Locations.CHAR.set();
		String uriStr = "http://DME2SEARCH/service=MyService/" + versionText + "/envContext=PROD/dataContext=205977/partner=TEST";
		RegistryFsSetup.copyRoutingFile(uriStr);

		Locations.CHAR.set();
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
		System.out.println("reply : " +reply);
				
		GetDME2JMXDetails jmxInt = new GetDME2JMXDetails();
		Set<ObjectName> objectSet = jmxInt.getObjectSet();
	    System.out.println("objectSet : " + objectSet);	
	    Assert.assertNotNull(objectSet);
	    String objectSetStr = objectSet.toString();
	    //[com.att.aft.scld:type=ConfigurationManager,name=JmxConfigurationManager-DefaultDME2Manager, JmxInterface:type=dme2, com.att.aft.dme2:type=dme2Cache,name=StaleEndpointCache, com.att.aft.dme2:type=ThrottleConfig,name=DME2ThrottleConfig-DefaultDME2Manager, JMImplementation:type=MBeanServerDelegate, java.lang:type=Runtime, java.lang:type=Threading, java.lang:type=OperatingSystem, java.lang:type=MemoryPool,name=Code Cache, java.nio:type=BufferPool,name=direct, java.lang:type=Compilation, java.lang:type=MemoryManager,name=CodeCacheManager, com.att.aft.scld:type=ConfigurationManager,name=JmxConfigurationManager-dme2_config_manager, java.util.logging:type=Logging, java.lang:type=ClassLoading, java.lang:type=MemoryManager,name=Metaspace Manager, java.lang:type=GarbageCollector,name=PS MarkSweep, com.att.aft.dme2:type=RegistryCache,name=dme2EndpointCache-DME2Server-FS, com.att.aft.dme2:type=dme2Manager,name=DME2Server-FS, java.lang:type=MemoryPool,name=Metaspace, com.att.aft.scld:type=ConfigurationManager,name=JmxConfigurationManager-DME2Server-FS, java.lang:type=MemoryPool,name=PS Old Gen, com.att.aft.dme2:type=dme2Cache,name=StaleRouteOfferCache, java.lang:type=GarbageCollector,name=PS Scavenge, java.lang:type=MemoryPool,name=PS Eden Space, java.lang:type=MemoryPool,name=Compressed Class Space, java.lang:type=Memory, java.nio:type=BufferPool,name=mapped, com.att.aft.dme2:type=RegistryCache,name=dme2RouteInfoCache-DME2Server-FS, java.lang:type=MemoryPool,name=PS Survivor Space, com.sun.management:type=DiagnosticCommand, com.att.aft.dme2:type=dme2Manager,name=DefaultDME2Manager, com.sun.management:type=HotSpotDiagnostic]
	    Assert.assertTrue(objectSetStr.contains("com.att.aft.dme2:type=dme2CacheFS,name=StaleEndpointCache"));
	    Assert.assertTrue(objectSetStr.contains("com.att.aft.dme2:type=RegistryCacheFS,name=dme2EndpointCache-DME2Server-FS"));
	    Assert.assertTrue(objectSetStr.contains("com.att.aft.dme2:type=RegistryCacheFS,name=dme2RouteInfoCache-DME2Server-FS"));	    
	    Assert.assertTrue(objectSetStr.contains("com.att.aft.dme2:type=dme2CacheFS,name=StaleRouteOfferCache"));
//	    Assert.assertTrue(objectSetStr.contains("com.att.aft.dme2:type=dme2CacheGRM,name=StaleEndpointCache"));
//	    Assert.assertTrue(objectSetStr.contains("com.att.aft.dme2:type=dme2CacheGRM,name=StaleRouteOfferCache"));
	}
	
	  @Test
		public void testVersion_1_1_2AndJMX() throws Exception {
			tryJMXBeans("1.1.1","/version=1.1.1/");
	  }
	
}
