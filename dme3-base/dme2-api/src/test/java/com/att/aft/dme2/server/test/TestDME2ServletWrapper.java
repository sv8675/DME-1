/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;

import junit.framework.TestCase;

@Ignore
public class TestDME2ServletWrapper extends TestCase
{
  private static final Logger logger = LoggerFactory.getLogger( TestDME2ServletWrapper.class );

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();	
		System.setProperty("DME2.DEBUG","true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_COLLECT_SERVICE_STATS", "false");
		RegistryFsSetup.init();
	}
  public void cleanPreviousEndpoints( String serviceName, String serviceVersion, String envContext )
      throws Exception {
    //System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" ); // Stolen from ServerLauncher
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

	public void testDME2ServletWrapper_PublishService() throws Exception
	{
		String descriptorLocation = "src/test/resources/web.xml";
		String resourceBase = "src/test/resources/";
		
		Server server = new Server(12345);
		WebAppContext context = null;
		
		try
		{
			context = new WebAppContext();
			context.setDescriptor(descriptorLocation);
			context.setResourceBase(resourceBase);
			context.setContextPath("/");
			context.setParentLoaderPriority(true);
			
			server.setHandler(context);
			server.start();
			
//			System.out.println("Successfully started server on port: " + server.getConnectors()[0].getPort());
			System.out.println("Successfully deployed webapp.");
			
			/*DME2ServletWrapper should have published Service. Validate that it is there. */
			DME2Manager mgr = new DME2Manager();
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST"));
			
			endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST_2");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST_2"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{server.stop();}
			catch(Exception e){}
			
			try{context.destroy();}
			catch(Exception e){}
			
		}
	}
	
	
	public void testDME2ServletWrapper_PublishService_GetParamsFromWebXMLDescriptor() throws Exception {
		String descriptorLocation = "src/test/resources/web_2.xml";
		String resourceBase = "src/test/resources/";
		
		/* foo.properties does not exist, so get the init params from the web.xml */
		System.setProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE", "foo.properties");
		
		Server server = new Server(12345);
		WebAppContext context = null;
		
		try {
			context = new WebAppContext();
			context.setDescriptor(descriptorLocation);
			context.setResourceBase(resourceBase);
			context.setContextPath("/");
			context.setParentLoaderPriority(true);
			
			server.setHandler(context);
			server.start();
			
			//System.out.println("Successfully started server on port: " + server.getConnectors()[0].getPort());
			System.out.println("Successfully deployed webapp.");
			
			/*DME2ServletWrapper should have published Service. Validate that it is there. */
			DME2Manager mgr = new DME2Manager();
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{server.stop();}
			catch(Exception e){}
			
			try{context.destroy();}
			catch(Exception e){}
			
			System.clearProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE");
		}
	}
	
	public void testDME2ServletWrapper_PublishService_UseDefaultPort() throws Exception
	{
		/* If port is not provided in servlet-init-properties, application will default to using port: 8080 */
		String descriptorLocation = "src/test/resources/web.xml";
		String resourceBase = "src/test/resources/";
		
		System.setProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE", "dme2-servlet-init-missing-port.properties");
		
		Server server = new Server(12345);
		WebAppContext context = null;
		
		try
		{
			context = new WebAppContext();
			context.setDescriptor(descriptorLocation);
			context.setResourceBase(resourceBase);
			context.setContextPath("/");
			context.setParentLoaderPriority(true);
			
			server.setHandler(context);
			server.start();
			
			Properties props = RegistryFsSetup.init();
			DME2Configuration config = new DME2Configuration("testDME2ServletWrapper_PublishService_UseDefaultPort", props);
			
			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_PublishService_UseDefaultPort", config);
			
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST"));
			assertTrue(endpoints.get(0).getPort() == 8080);
			
			//String clientURI = "http://GACDTNL05MJ8949.ITServices.sbc.com:12345/servletwrapper";
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST";
			
//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
//			client.setPayload("TEST IS A TEST");
			
			/*Should fail since application is really listening on port 12345*/
//			client.sendAndWait(30000);

			Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();			
			
			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payload = new DME2TextPayload("TEST IS A TEST");
			
			String reply = (String) sender.sendAndWait(payload);
			System.out.println("REPLY = " + reply);
			
			fail("Got Error in test case - This should have failed with [AFT-DME2-0707] error. The port used to publish the service is 8080, but the application server is listening on 12345.");
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0707"));
		}
		finally
		{
			try{server.stop();}
			catch(Exception e){}
			
			try{context.destroy();}
			catch(Exception e){}
			
			System.clearProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE");
			
		}
	}
	
	
	public void testDME2ServletWrapper_PublishService_UseDefaultHost() throws Exception
	{
		/* If host is not provided in servlet-init-properties, application will default to using localhost */
		String descriptorLocation = "src/test/resources/web.xml";
		String resourceBase = "src/test/resources/";
		
		System.setProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE", "dme2-servlet-init-missing-host.properties");
		
		Server server = new Server(12345);
		WebAppContext context = null;
		
		try
		{
			context = new WebAppContext();
			context.setDescriptor(descriptorLocation);
			context.setResourceBase(resourceBase);
			context.setContextPath("/");
			context.setParentLoaderPriority(true);
			
			server.setHandler(context);
			server.start();
			
			Properties props = RegistryFsSetup.init();
			DME2Configuration config = new DME2Configuration("testDME2ServletWrapper_SendClientRequest", props);			
			
			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_SendClientRequest", config);
			
			/*DME2ServletWrapper should have published Service. Validate that it is there. */
//			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_SendClientRequest", new Properties());
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST"));
			assertTrue(endpoints.get(0).getHost().equals(InetAddress.getLocalHost().getCanonicalHostName()));
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{server.stop();}
			catch(Exception e){}
			
			try{context.destroy();}
			catch(Exception e){}
			
			System.clearProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE");
			
		}
	}
	
	
	public void testDME2ServletWrapper_PublishService_UseDefaultProtocol() throws Exception
	{
		/* If protocol is not provided in servlet-init-properties, application will default to using http */
		String descriptorLocation = "src/test/resources/web.xml";
		String resourceBase = "src/test/resources/";
		
		System.setProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE", "dme2-servlet-init-missing-protocol.properties");
		
		Server server = new Server(12345);
		WebAppContext context = null;
		
		try
		{
			context = new WebAppContext();
			context.setDescriptor(descriptorLocation);
			context.setResourceBase(resourceBase);
			context.setContextPath("/");
			context.setParentLoaderPriority(true);
			
			server.setHandler(context);
			server.start();
			
			/*DME2ServletWrapper should have published Service. Validate that it is there. */
			//DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_SendClientRequest", new Properties());

			Properties props = RegistryFsSetup.init();
			DME2Configuration config = new DME2Configuration("testDME2ServletWrapper_PublishService_UseDefaultProtocol", props);
			
			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_PublishService_UseDefaultProtocol", config);
			
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST"));
			assertTrue(endpoints.get(0).getProtocol().equalsIgnoreCase("http"));
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{server.stop();}
			catch(Exception e){}
			
			try{context.destroy();}
			catch(Exception e){}
			
			System.clearProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE");
			
		}
	}
	
	
	public void testDME2ServletWrapper_UnpublishService() throws Exception
	{
		String descriptorLocation = "src/test/resources/web.xml";
		String resourceBase = "src/test/resources/";
		
		Server server = new Server(12345);
		WebAppContext context = null;
		
		try {
			context = new WebAppContext();
			context.setDescriptor(descriptorLocation);
			context.setResourceBase(resourceBase);
			context.setContextPath("/");
			context.setParentLoaderPriority(true);
			
			server.setHandler(context);
			server.start();
			
			System.out.println("Successfully started server on port: " + server.getConnectors()[0]);
			System.out.println("Successfully deployed webapp.");
			
			/*DME2ServletWrapper should have published Service. Validate that it is there. */
///			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_UnpublishService", new Properties());

			Properties props = RegistryFsSetup.init();
			DME2Configuration config = new DME2Configuration("testDME2ServletWrapper_UnpublishService", props);			
			
			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_UnpublishService", config);
			
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST"));
			
			
			endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST_2");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST_2"));
			
			/* By calling destroy, the service should get unpublished from GRM */
			System.out.println("Calling destroy to unpublish the service.");
			server.stop();
			context.destroy();
			Thread.sleep(3000);
			
			mgr =  new DME2Manager("testDME2ServletWrapper_UnpublishService2", config);
			endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			System.out.println("Number of endpoints returned from GRM (should be 0): " + endpoints.size());
			assertEquals(0, endpoints.size());
			
			endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST_2");
			System.out.println("Number of endpoints returned from GRM (should be 0): " + endpoints.size());
			assertEquals(0, endpoints.size());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{context.destroy();}
			catch(Exception e){}
			
		}
	}
	
	
	public void testDME2ServletWrapper_SendClientRequest() throws Exception
	{
		String descriptorLocation = "src/test/resources/web.xml";
		String resourceBase = "src/test/resources/";

    cleanPreviousEndpoints( "com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB" );
		Server server = new Server(12345);
		WebAppContext context = null;

		try
		{
			context = new WebAppContext();
			context.setDescriptor(descriptorLocation);
			context.setResourceBase(resourceBase);
			context.setContextPath("/");
			context.setParentLoaderPriority(true);
			
			server.setHandler(context);
			server.start();
			
			System.out.println("Successfully started server on port: " + server.getConnectors()[0]);
			System.out.println("Successfully deployed webapp.");
			
			/*DME2ServletWrapper should have published Service. Validate that it is there. */
//			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_SendClientRequest", new Properties());

			Properties props = RegistryFsSetup.init();
			DME2Configuration config = new DME2Configuration("testDME2ServletWrapper_SendClientRequest", props);			
			
			DME2Manager mgr =  new DME2Manager("testDME2ServletWrapper_SendClientRequest", config);
			
			List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints("com.att.aft.dme2.test.TestDME2ServletWrapper", "1.0.0", "LAB", "TEST");
			
			System.out.println("Number of endpoints returned from GRM (should be 1): " + endpoints.size());
			assertEquals(1, endpoints.size());
			
			System.out.println("Returned endpoint for service: " + endpoints.get(0).getServiceName());
			assertTrue(endpoints.get(0).getServiceName().equals("/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST"));
			
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2ServletWrapper/version=1.0.0/envContext=LAB/routeOffer=TEST";
			
//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
//			client.setPayload("TEST IS A TEST");

			Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();			
			
			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payload = new DME2TextPayload("TEST IS A TEST");
			
			String response = (String) sender.sendAndWait(payload);
			assertTrue(response.contains("TEST IS A TEST"));
			
			System.out.println("Response from service: " + response);
		}
		finally
		{
			try{server.stop();}
			catch(Exception e){}
			
			try{context.destroy();}
			catch(Exception e){}
			
		}
	}
	
}
