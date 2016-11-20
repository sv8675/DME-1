/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.DME2NullServlet;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.EchoReplyHandler;

/**
 * Unit test for simple App.
 */
@Ignore
public class TestDME2SSL {
	  private static final Logger logger = LoggerFactory.getLogger( TestDME2SSL.class );

 /**
   * Create the test case.
   *
   * @param testName
   *            name of the test case
   */
	@Before
	public void setUp() {
    	System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
	 	System.setProperty("AFT_LATITUDE", "33.6");
	 	System.setProperty("AFT_LONGITUDE", "-86.6");
	 	System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
	 	System.setProperty("DME2.DEBUG", "true");
  	}

	/**
	 * Validate both SSL and NON-SSL endpoints work from a JVM.
	 * @throws Exception
	 */
	@Test
	public void testSSLEnabled() throws Exception {
	    DME2Manager serverManager = null;
	    DME2Manager serverManager1 = null;
	    DME2Manager clientManager = null;
	    try {
	      //String uriStr = "/service=com.att.aft.test.QuickStartService/version=1.0.0/envContext=DEV/routeOffer=DEFAULT";
	      String uriStr = "/service=com.att.aft.test.QuickStartService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
	      String uriStr1 = "/service=com.att.aft.test.QuickStartService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
	      Properties props =  new Properties();
	      props.setProperty("AFT_DME2_KEYSTORE", TestDME2SSL.class.getResource( "/m2e.jks" ).getFile());
	      props.setProperty("AFT_DME2_KEY_PASSWORD", "password");
	      props.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
	      props.setProperty("AFT_DME2_QUICKSTART_SERVICE", uriStr);
	
	      props.setProperty("AFT_DME2_SSL_ENABLE", "true");
	      props.setProperty("AFT_DME2_ALLOW_RENEGOTIATE", "true");
	      System.setProperty("javax.net.debug", "all");
	      System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	      System.setProperty("DME2.DEBUG", "true");
	
	
	      DME2Configuration config = new DME2Configuration("testSSLEnabled", props);
	
	      serverManager = new DME2Manager("testSSLEnabled", config);
	      // create a new DME2 manager with above SSL properties
	      //serverManager = new DME2Manager("testSSLEnabled", props);
	      serverManager.bindServiceListener(uriStr, new DME2NullServlet());
	      serverManager.start();
	
	      DME2Configuration config1 = new DME2Configuration("testSSLEnabled1", new Properties());
	
	      serverManager1 = new DME2Manager("testSSLEnabled1", config1);
	      //serverManager1 = new DME2Manager("testSSLEnabled", new Properties());
	      serverManager1.bindServiceListener(uriStr1, new DME2NullServlet());
	      serverManager1.start();
	
	      Thread.sleep(10000);
	      DME2Configuration config2 = new DME2Configuration("testSSLEnabledClient", new Properties());
	
	      clientManager = new DME2Manager("testSSLEnabledClient", config2);
	
	      // now try to call our https service
	      //clientManager = new DME2Manager("testSSLEnabledClient", new Properties());
	
	      //DME2Client client = clientManager.newClient(new URI(uriStr + "/partner=JUnit"), 5000L);
	//		DME2Client client = clientManager.newClient(new URI("http://DME2RESOLVE"+uriStr), 5000L);
	
	      Request request = new RequestBuilder(new URI("http://DME2RESOVE/"+uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL("http://DME2RESOLVE/"+uriStr).build();
	
	      DME2Client client = new DME2Client(clientManager, request);
	      client.sendAndWait(new DME2TextPayload("THIS IS A TEST"));
	
	      //client.setPayload("hello");
	      // if we get no exception, we are good to proceed
	      //client.sendAndWait(60000L);
	
	
	
	      // now try to call our https service with no http
	//		DME2Manager clientManager1 = new DME2Manager("testSSLDisabledClient",  new Properties());
	
	      DME2Configuration config3 = new DME2Configuration("testSSLDisabledClient", new Properties());
	
	      DME2Manager mgr = new DME2Manager("testSSLDisabledClient", config3);
	
	      //DME2Client client = clientManager.newClient(new URI(uriStr + "/partner=JUnit"), 5000L);
	//		DME2Client client1 = clientManager1.newClient(new URI("http://DME2RESOLVE"+uriStr1), 5000L);
	//		client1.setPayload("hello");
	      // if we get no exception, we are good to proceed
	//		client1.sendAndWait(60000L);
	
	      request = new RequestBuilder(new URI("http://DME2RESOLVE/"+uriStr1)).withHttpMethod("POST").withReadTimeout(3000).withReturnResponseAsBytes(false).withLookupURL("http://DME2RESOLVE/"+uriStr1).build();
	
	      client = new DME2Client(mgr, request);
	      client.sendAndWait(new DME2TextPayload("THIS IS A TEST"));
	
	
	    } finally {
	      try {
	        if (serverManager != null) serverManager.stop();
	      } catch (Exception e) { }
	      try {
	        if (serverManager1 != null) serverManager1.stop();
	      } catch (Exception e) { }
	      try {
	        if (clientManager != null) clientManager.stop();
	      } catch (Exception e) { }

	    }
	}

	@Test
	  @Ignore
	  public void testSSLEndpointsRenegotiate() throws Exception {
	    //System.setProperty("AFT_DME2_CLIENT_ALLOW_RENEGOTIATE", "false");
	    Properties props = RegistryFsSetup.init();
	    props.setProperty("AFT_DME2_CLIENT_ALLOW_RENEGOTIATE", "false");
	    props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON","true");
	    props.setProperty("AFT_DME2_SSL_ENABLE","true");
	    props.setProperty("DME2.DEBUG", "true");
	    ServerControllerLauncher bhamLauncher3 = null;
	    try{
	      DME2Configuration config = new DME2Configuration("testSSLEndpointsRenegotiate", props);
	      DME2Manager manager = new DME2Manager("testSSLEndpointsRenegotiate", config, props);
	      
		  cleanPreviousEndpoints("com.att.aft.TestSSLEndpointsRenegotiate2", "1.0.0", "DEV");

	      String[] bham_1_bau_se_args = {
	          "-registryType",
	          "GRM",
						/*"-serverPort",
						"45670",*/
						"-servletClass",
						"EchoServlet",
						"-serviceName",
						"service=com.att.aft.TestSSLEndpointsRenegotiate2/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
						"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", " -DAFT_DME2_KEYSTORE=" +  TestDME2SSL.class.getResource( "/m2e.jks" ).getFile(),
						" -Dplatform=" + TestConstants.GRM_PLATFORM_TO_USE," -DAFT_DME2_KEY_PASSWORD=password"," -DAFT_DME2_KEYSTORE_PASSWORD=password"," -DAFT_DME2_SSL_ENABLE=true"," -Djavax.net.debug=all" };
				ServerControllerLauncher bhamLauncher = new ServerControllerLauncher(bham_1_bau_se_args);
				bhamLauncher.launchSSLServer();
				try {
					Thread.sleep(30000);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				// try to call a service we just registered
				String uriStr = "http://DME2SEARCH/service=com.att.aft.TestSSLEndpointsRenegotiate2/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";
				Map<String,String> headers = new HashMap<String,String>();
				headers.put("AFT_DME2_REQ_TRACE_ON", "true");
				
				Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withHeaders(headers).withReadTimeout(3000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
	
				DME2Client sender = new DME2Client(manager, request);
				EchoReplyHandler replyHandler = new EchoReplyHandler();
				sender.setResponseHandlers(replyHandler);
				sender.send(new DME2TextPayload("this is a test"));
	
				String reply = replyHandler.getResponse(30000);
				System.out.println("REPLY 1=" + reply);
				Map<String,String> rheader = replyHandler.getResponseHeaders();
				
				String traceStr = rheader.get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println(traceStr);
			
				try {
					bhamLauncher.destroy();
				} catch(Exception e) {
						e.printStackTrace();
				}
				
				Thread.sleep(20000);
				System.out.println("Destroyed launchers ");
				
				sender = new DME2Client(manager, request);
				replyHandler = new EchoReplyHandler();
				sender.setResponseHandlers(replyHandler);
				sender.send(new DME2TextPayload("this is a test"));
				
				boolean exceptionInGettingResponse = false;
				try {
				String reply1 = replyHandler.getResponse(10000);
				System.out.println("REPLY 1.0=" + reply1);
				}
				catch(Exception e) {
					exceptionInGettingResponse=true;
				}
				assert(exceptionInGettingResponse);
				
				bhamLauncher3 = new ServerControllerLauncher(bham_1_bau_se_args);
				bhamLauncher3.launchSSLServer();
				try {
					Thread.sleep(20000);
				} catch (Exception ex) {
				}
			
		
				sender = new DME2Client(manager, request);
				replyHandler = new EchoReplyHandler();
				sender.setResponseHandlers(replyHandler);
				sender.send(new DME2TextPayload("this is a test"));
				
				reply = replyHandler.getResponse(10000);
				
				System.out.println("REPLY 2=" + reply);
				rheader = replyHandler.getResponseHeaders();
				
				traceStr = rheader.get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println(traceStr);	
				
	    } finally{
	      try{
	        if(bhamLauncher3 != null)
	          bhamLauncher3.destroy();
	      } catch(Exception e) {
	
	      }
	    }
	}



  /**
   * Validate SSL endpoints work with a new instance.
   * @throws Exception
   */
	@Test
	  @Ignore
	public void testSSLEndpointsRenegotiateOff() throws Exception {
	    System.setProperty("AFT_DME2_CLIENT_ALLOW_RENEGOTIATE", "false");
	    System.setProperty("DME2.DEBUG", "true");
	    
	    try {
	      Properties props = RegistryFsSetup.init();
	      props.setProperty("AFT_DME2_CLIENT_ALLOW_RENEGOTIATE", "false");
	      props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON","true");
	      props.setProperty("AFT_DME2_SSL_ENABLE","true");
	      props.setProperty("DME2.DEBUG", "true");
	      //DME2Manager manager = new DME2Manager("testSSLEndpointsRenegotiateOff", props);
	      //Properties props = RegistryFsSetup.init();

	      DME2Configuration config = new DME2Configuration("testSSLEndpointsRenegotiateOff", props);
	
	      DME2Manager manager = new DME2Manager("testSSLEndpointsRenegotiateOff", config);
		  cleanPreviousEndpoints("com.att.aft.TestSSLEndpointsRenegotiateOff", "1.0.0", "DEV");
	
	      String[] bham_1_bau_se_args = {
	          "-registryType",
	          "GRM",
	          "-serverPort",
	          "45670",
	          "-servletClass",
	          "EchoServlet",
	          "-serviceName",
	          "service=com.att.aft.TestSSLEndpointsRenegotiateOff/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
	          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", " -DAFT_DME2_KEYSTORE=" + TestDME2SSL.class.getResource( "/m2e.jks" ).getFile(),
	          " -Dplatform=" + TestConstants.GRM_PLATFORM_TO_USE," -DAFT_DME2_KEY_PASSWORD=password"," -DAFT_DME2_KEYSTORE_PASSWORD=password"," -DAFT_DME2_SSL_ENABLE=true","-DAFT_DME2_CLIENT_ALLOW_RENEGOTIATE=false"};//,"-Djavax.net.debug=all" };
	      ServerControllerLauncher bhamLauncher = new ServerControllerLauncher(bham_1_bau_se_args);
	      bhamLauncher.launchSSLServer1();
	      try {
	        Thread.sleep(15000);
	      } catch (Exception ex) {
	      }
	
	      // try to call a service we just registered
	      String uriStr = "http://DME2SEARCH/service=com.att.aft.TestSSLEndpointsRenegotiateOff/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";
	
	      Map<String,String> headers = new HashMap<String,String>();
	      headers.put("AFT_DME2_REQ_TRACE_ON", "true");
	
	      Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withHeaders(headers).withReadTimeout(3000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
	
	      DME2Client client = new DME2Client(manager, request);
	
	//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 3000);
	      //sender.setPayload("this is a test");
	      EchoReplyHandler replyHandler = new EchoReplyHandler();
	      client.setResponseHandlers(replyHandler);
	
	      //sender.setHeaders(headers);
	      DME2Payload payload = new DME2TextPayload("this is a test");
	      client.send(payload);
	
	      String reply = replyHandler.getResponse(10000);
	      System.out.println("REPLY 1=" + reply);
	      Map<String,String> rheader = replyHandler.getResponseHeaders();
	
	      String traceStr = rheader.get("AFT_DME2_REQ_TRACE_INFO");
	      System.out.println(traceStr);
	      
	      try {
	        bhamLauncher.destroy();
	      } catch(Exception e) {
	
	      }
	
			Thread.sleep(10000);
			System.out.println("Destroyed launchers ");
	
	      request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withHeaders(headers).withReadTimeout(3000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
	
	      client = new DME2Client(manager, request);
	
	      //sender = new DME2Client(manager, new URI(uriStr), 3000);
	      //sender.setPayload("this is a test");
	      replyHandler = new EchoReplyHandler();
	      payload = new DME2TextPayload("this is a test");
	      client.setResponseHandlers(replyHandler);
	      
	      //sender.setReplyHandler(replyHandler);
	      //sender.setHeaders(headers);
	      //client.send();
	
	      boolean exceptionInGettingResponse = false;
	      try {
	    	client.send(payload);
	        String reply1 = replyHandler.getResponse(10000);
	        System.out.println("REPLY 1.0=" + reply1);
	      }
	      catch(Exception e) {
	        exceptionInGettingResponse=true;
	      }
	      assert(exceptionInGettingResponse);
	
	      ServerControllerLauncher bhamLauncher3 = new ServerControllerLauncher(bham_1_bau_se_args);
	      bhamLauncher3.launchSSLServer1();
	      try {
	        Thread.sleep(20000);
	      } catch (Exception ex) {
	      }
	
	      Thread.sleep(2000);
	
	      request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withHeaders(headers).withReadTimeout(3000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
	
	      client = new DME2Client(manager, request);
	
	//		sender = new DME2Client(manager, new URI(uriStr), 3000);
	//		sender.setPayload("this is a test");
	      replyHandler = new EchoReplyHandler();
	      client.setResponseHandlers(replyHandler);
	//		client.setReplyHandler(replyHandler);
	//		sender.setHeaders(headers);
	      
	
	      exceptionInGettingResponse = false;
	      try {
	    	client.send(payload);
	        String reply1 = replyHandler.getResponse(10000);
	        System.out.println("REPLY 2=" + reply1);
	        rheader = replyHandler.getResponseHeaders();
	
	        traceStr = rheader.get("AFT_DME2_REQ_TRACE_INFO");
	        System.out.println(traceStr);
	      }
	      catch(Exception e) {
	        // Since reneg is turned off, expecting a failure on 2nd request
	        exceptionInGettingResponse=true;
	        assert(exceptionInGettingResponse);
	      }
	      bhamLauncher3.destroy();
	      Thread.sleep(10000);
	      //bhamLauncher4.destroy();
	
	    } finally {
	      System.clearProperty("AFT_DME2_CLIENT_ALLOW_RENEGOTIATE");
	    }
	}


	/**
	 * Validate exclude protocols for Jetty Server
	 * @throws Exception
	 */
	//Jetty 9 doesn't support exclude protocols method
	@Ignore
	@Test
	public void testSSLExcludeProtocols() throws Exception {
	    //System.setProperty("AFT_DME2_CLIENT_ALLOW_RENEGOTIATE", "false");
	    System.setProperty("AFT_DME2_SSL_EXCLUDE_PROTOCOLS","SSLv3");
	    System.setProperty("AFT_DME2_CLIENT_SSL_EXCLUDE_PROTOCOLS","SSLv3");
	    System.setProperty("AFT_DME2_SSL_ENABLE","true");
	    System.setProperty("DME2.DEBUG", "true");
	    try {
	      String uriStr = "/service=com.att.aft.TestSSLExcludeProtocols/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
	      Properties props1 =  new Properties();
	      props1.setProperty("AFT_DME2_KEYSTORE", TestDME2SSL.class.getResource( "/m2e.jks" ).getFile());
	      props1.setProperty("AFT_DME2_KEY_PASSWORD", "password");
	      props1.setProperty("AFT_DME2_PORT", "46899");
	      props1.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
	      props1.setProperty("AFT_DME2_QUICKSTART_SERVICE", uriStr);
	
	      props1.setProperty("AFT_DME2_SSL_ENABLE", "true");
	
	
	      System.setProperty("javax.net.debug", "all");
	      //System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	
	      //Properties props = RegistryFsSetup.init();
	
	      DME2Configuration config = new DME2Configuration("TestSSLExcludeProtocols", props1);
	
	      DME2Manager serverManager = new DME2Manager("TestSSLExcludeProtocols", config);
	
	      // create a new DME2 manager with above SSL properties
	      //DME2Manager serverManager = new DME2Manager("TestSSLExcludeProtocols", props1);
	      serverManager.bindServiceListener(uriStr,  new EchoServlet(
	          "service=com.att.aft.com.att.aft.TestSSLExcludeProtocols/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
	          "1"));
	      serverManager.start();
	
	      try {
	        Thread.sleep(15000);
	      } catch (Exception ex) {
	      }
	
	      // try to call a service we just registered
	      String clientURI = "http://DME2RESOLVE/service=com.att.aft.TestSSLExcludeProtocols/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
	
	      Map<String,String> headers = new HashMap<String,String>();
	      headers.put("AFT_DME2_REQ_TRACE_ON", "true");
	
	      Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("POST").withHeaders(headers).withReadTimeout(3000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();
	
	      DME2Client client = new DME2Client(serverManager, request);
	//		DME2Client sender = new DME2Client(serverManager, new URI(clientURI), 3000);
	//		sender.setPayload("this is a test");
	      EchoReplyHandler replyHandler = new EchoReplyHandler();
	      client.setResponseHandlers(replyHandler);
	//		sender.setHeaders(headers);
	      DME2Payload payload = new DME2TextPayload("this is a test");
	      try {
	        client.send(payload);
	        boolean excluded = false;
	        String[] protocols = serverManager.getServer().getSSLExcludeProtocol();
	        if(protocols != null) {
	          for(String prot:protocols){
	            assert(prot.equalsIgnoreCase("sslv3"));
	            excluded = true;
	          }
	        }
	        assert(excluded); 
	
	        String reply = replyHandler.getResponse(10000);
	        System.out.println("REPLY 1=" + reply);
	        Map<String,String> rheader = replyHandler.getResponseHeaders();
	
	        String traceStr = rheader.get("AFT_DME2_REQ_TRACE_INFO");
	        System.out.println(traceStr);
	      } finally {
	        try {
	          serverManager.unbindServiceListener(uriStr);
	        } catch(Exception e) {
	
	        }
	      }
	
	    }catch(Exception ex){
	      ex.printStackTrace();
	      throw ex;
	    }
	    finally {
	      System.clearProperty("AFT_DME2_CLIENT_ALLOW_RENEGOTIATE");
	      System.clearProperty("AFT_DME2_SSL_EXCLUDE_PROTOCOLS");
	    }
	}
	
	  public void cleanPreviousEndpoints( String serviceName, String serviceVersion, String envContext )
		      throws Exception {
		    System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" ); // Stolen from ServerLauncher
		    logger.debug( null, "cleanPreviousEndpoints", LogMessage.METHOD_ENTER );
		    DME2Configuration config = new DME2Configuration( serviceName );
		    DME2Manager manager = new DME2Manager( serviceName, config );
		    BaseAccessor grm = (BaseAccessor) DME2UnitTestUtil
		        .getPrivate( DME2EndpointRegistryGRM.class.getDeclaredField( "grm" ),
		            ( (DME2EndpointRegistryGRM) manager.getEndpointRegistry() ) );
		    ServiceEndpoint serviceEndpoint = new ServiceEndpoint();//DME3EndpointUtil.convertToServiceEndpoint( endpoint );
		    serviceEndpoint.setName( serviceName );
		    serviceEndpoint.setVersion( serviceVersion );
		    serviceEndpoint.setEnv( envContext );
		    try {
		      List<ServiceEndpoint> serviceEndpointList = grm.findRunningServiceEndPoint( serviceEndpoint );
		      if ( serviceEndpointList != null ) {
		        for ( ServiceEndpoint sep : serviceEndpointList ) {
		          logger.debug( null, "cleanPreviousEndpoints", "Removing old endpoint {} {} {}", sep.getName(),
		              sep.getHostAddress(), sep.getPort() );
		          try {
		            manager.getEndpointRegistry()
		                .unpublish( "/service=" + sep.getName() + "/envContext=" + envContext + "/version=" + sep.getVersion(),
		                    sep.getHostAddress(), Integer.valueOf( sep.getPort() ) );
		          } catch ( Exception e ) {
		            logger.debug( null, "cleanPreviousEndpoints", "Error cleaning endpoint {} {} {}", sep.getName(),
		                sep.getHostAddress(), sep.getPort(), e );
		          }
		        }
		      }
		    } catch ( Exception e ) {
		      logger.debug( null, "cleanPreviousEndpoints", "Error cleaning endpoints", e );
		    }

		    logger.debug( null, "cleanPreviousEndpoints", LogMessage.METHOD_EXIT );
		  }
	
}
