package com.att.aft.dme2.server.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.util.DME2Constants;

import junit.framework.TestCase;

public class TestUseVersionRange
extends TestCase
{
	DME2Manager manager = null;
	
	public void setUp()
	throws Exception
	{
	//	RegistryGrmSetup.init();

		
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG","true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_COLLECT_SERVICE_STATS", "false");
		
		Properties props = new Properties();
//		props.setProperty("DME2_EP_REGISTRY_CLASS", MemoryRegistry.class.getCanonicalName());
    props.setProperty( DME2Constants.DME2_EP_REGISTRY_CLASS, DME2Constants.DME2MEMORY );
		props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		props.setProperty("AFT_LATITUDE", "33.373900");
		props.setProperty("AFT_LONGITUDE", "-86.798300");
		

		DME2Configuration config = new DME2Configuration("RAMMgr", props);			
		
		manager = new DME2Manager("RAMMgr",config);
	}
	
	/** multiple endpoints for the same service URL -- dme2 should try them all */
	public void testTryAllIdenticalEndpoints()
	throws Exception 
	{
		final List<String> endpoints = Arrays.asList("/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1",
													 "/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1",
													 "/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1");
		final List<Integer> ports = Arrays.asList(9080, 9081, 9082);
		final String clientURI = "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1";
		final List<String> assertions = Arrays.asList(":9080", ":9081", ":9082");
		
		assertTryEndpoints(endpoints, ports, clientURI, assertions);
	}
	
	/** try only services with matching route offer */
	public void testTryResolvableAnyRouteOffer()
	throws Exception 
	{
		final List<String> endpoints = Arrays.asList("/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1",
													 "/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO2",
													 "/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO3");
		final List<Integer> ports = Arrays.asList(9080, 9081, 9082);
		final String clientURI = "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1";
		final List<String> assertions = Arrays.asList(":9080");
		final List<String> exclusions = Arrays.asList(":9081", ":9082");
		assertTryEndpoints(endpoints, ports, clientURI, assertions, exclusions);
	}
	
	/** try only services with matching route offer */
	public void testTrySearchMatchingVersionRange()
	throws Exception 
	{
//		final List<String> endpoints = Arrays.asList("/service=com.att.afttest3.ExchangeFailover/version=4.0.1/envContext=LAB/routeOffer=FO1?clientSupportedVersions=1,3",
//				 "/service=com.att.afttest3.ExchangeFailover/version=4.0.2/envContext=LAB/routeOffer=FO1?clientSupportedVersions=1,2",
//				 "/service=com.att.afttest3.ExchangeFailover/version=4.0.3/envContext=LAB/routeOffer=FO1?clientSupportedVersions=2,4");
		//final List<String> endpoints = Arrays.asList("/service=com.att.afttest3.ExchangeFailover/version=3.0.1/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,3",
	  //												 "/service=com.att.afttest3.ExchangeFailover/version=2.0.2/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,2",
		//											 "/service=com.att.afttest3.ExchangeFailover/version=4.0.3/envContext=LAB/routeOffer=FO1?supportedVersionRange=2,4");
    final List<String> endpoints = Arrays.asList("/service=com.att.aft.ExchangeFailover/version=3.0.1/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,3",
        												 "/service=com.att.aft.ExchangeFailover/version=2.0.2/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,2",
        											 "/service=com.att.aft.ExchangeFailover/version=4.0.3/envContext=LAB/routeOffer=FO1?supportedVersionRange=2,4");
		final List<Integer> ports = Arrays.asList(9080, 9081, 9082);
		
		//final String clientURI = "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=1.0/envContext=LAB/routeOffer=FO1";
    final String clientURI = "http://DME2RESOLVE/service=com.att.aft.ExchangeFailover/version=1.0/envContext=LAB/routeOffer=FO1";
		final List<String> assertions = Arrays.asList(":9080", ":9081");
		final List<String> exclusions = Arrays.asList(":9082");
		assertTryEndpoints(endpoints, ports, clientURI, assertions, exclusions);
	}
	
	/** try only services with matching route offer */
	public void testTrySearchNoExactVersion()
	throws Exception 
	{
		final List<String> endpoints = Arrays.asList("/service=com.att.afttest3.ExchangeFailover/version=3.0.1/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,3",
													 "/service=com.att.afttest3.ExchangeFailover/version=2.0.2/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,2",
													 "/service=com.att.afttest3.ExchangeFailover/version=4.0.3/envContext=LAB/routeOffer=FO1?supportedVersionRange=2,4");
		final List<Integer> ports = Arrays.asList(8080, 8081, 8082);
		
		final String clientURI = "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=1.0/envContext=LAB/routeOffer=FO1?matchVersionRange=false";
		final List<String> assertions = Collections.emptyList();
		final List<String> exclusions = Arrays.asList(":9080", ":9081", ":9082");
		assertTryEndpoints(endpoints, ports, clientURI, assertions, exclusions);
	}
	
	/** try only services with matching route offer */
	public void testTrySearchMatchExactVersion()
	throws Exception 
	{
		final List<String> endpoints = Arrays.asList("/service=com.att.afttest3.ExchangeFailover/version=3.0.1/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,3",
													 "/service=com.att.afttest3.ExchangeFailover/version=2.0.2/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,2",
													 "/service=com.att.afttest3.ExchangeFailover/version=4.0.3/envContext=LAB/routeOffer=FO1?supportedVersionRange=2,4");
		final List<Integer> ports = Arrays.asList(9080, 9081, 9082);
		
		final String clientURI = "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=2.0.2/envContext=LAB/routeOffer=FO1?matchVersionRange=false";
		final List<String> assertions = Arrays.asList(":9081");
		final List<String> exclusions = Arrays.asList(":9080", ":9082");
		assertTryEndpoints(endpoints, ports, clientURI, assertions, exclusions);
	}
	
	/** try only services with matching route offer */
	public void testTrySearchMatchesMajorMinorVersion()
	throws Exception 
	{
		final List<String> endpoints = Arrays.asList("/service=com.att.afttest3.ExchangeFailover/version=3.0.1/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,3",
													 "/service=com.att.afttest3.ExchangeFailover/version=2.0.2/envContext=LAB/routeOffer=FO1?supportedVersionRange=1,2",
													 "/service=com.att.afttest3.ExchangeFailover/version=4.0.3/envContext=LAB/routeOffer=FO1?supportedVersionRange=2,4");
		final List<Integer> ports = Arrays.asList(9080, 9081, 9082);
		
		final String clientURI = "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=2.0/envContext=LAB/routeOffer=FO1?matchVersionRange=false";
		final List<String> assertions = Arrays.asList(":9081");
		final List<String> exclusions = Arrays.asList(":9080", ":9082");
		assertTryEndpoints(endpoints, ports, clientURI, assertions, exclusions);
	}
	
	public void assertTryEndpoints(List<String> endpoints, List<Integer> ports, String clientURI, List<String> expectations) 
	throws Exception
	{
		assertTryEndpoints(endpoints, ports, clientURI, expectations, Collections.emptyList());
	}
	
	/** register the endpoints at the given ports then try to connect to the clientURI.
	 *  connection will fail (nothing is actually listening at those endpoints),
	 *  but trace should reveal which endpoints were tried --
	 *  so verify that trace contains each of the string expressions in expectations.
	 */
	public void assertTryEndpoints(List<String> endpoints, List<Integer> ports, String clientURI, List<String> expectations, List<String> negativeExpectations) throws Exception {
		assertEquals(ports.size(), endpoints.size());
		
		final DME2EndpointRegistry registry = manager.getEndpointRegistry();
		final Iterator<Integer> pi = ports.iterator();
		final Iterator<String> ei = endpoints.iterator();

    try {
      while ( pi.hasNext() ) {
        String serviceName = ei.next();
        Integer port = pi.next();
        registry.publish( serviceName, null, "127.0.0.1", port, "http" );

      }

      EchoReplyHandler replyHandler = new EchoReplyHandler();
      Request request = new RequestBuilder(new URI( clientURI ) )
          .withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withHttpMethod( "POST" ).withReadTimeout( 300000 )
          .withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();
      DME2Client client = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "" );
      client.setResponseHandlers( replyHandler );

      try {
        client.send( payload );
        String reply = replyHandler.getResponse( 15000 );
        fail( "DME2 connection should have failed, got reply: " + reply );
      } catch ( Exception e ) {
      }

      final Map<String, String> headers = replyHandler.getResponseHeaders();
      String traceInfo = headers == null ? "" : headers.get( "AFT_DME2_REQ_TRACE_INFO" );
      // The trace info should contain all 3 endpoints with port 8080,8081
      // and 8082 attempted.
      System.out.println( "=====traceinfo=======" + traceInfo );
      for ( String expected : expectations ) {
        assertTrue( "trace should contain[" + expected + "]: " + traceInfo, traceInfo.contains( expected ) );
      }

      for ( String notExpected : negativeExpectations ) {
        assertFalse( "trace should not contain [" + notExpected + "]: " + traceInfo,
            traceInfo.contains( notExpected ) );
      }
    } catch ( Exception e ) {
      throw e;
    } finally {
      DME2UnitTestUtil.setFinalStatic( registry.getClass().getDeclaredField( "endpoints" ), registry,
          new ArrayList<DME2Endpoint>() );
    }
	}
	

}
