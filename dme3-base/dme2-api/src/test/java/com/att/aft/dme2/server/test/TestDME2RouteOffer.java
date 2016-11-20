/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;


import static com.att.aft.dme2.server.test.RouteInfoCreatorUtil.buildRoute;
import static com.att.aft.dme2.server.test.RouteInfoCreatorUtil.buildRouteGroup;
import static com.att.aft.dme2.server.test.RouteInfoCreatorUtil.buildRouteGroups;
import static com.att.aft.dme2.server.test.RouteInfoCreatorUtil.buildRouteInfo;
import static com.att.aft.dme2.server.test.RouteInfoCreatorUtil.buildRouteOffers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointCacheGRM;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.manager.registry.DME2ServiceEndpointData;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;

public class TestDME2RouteOffer extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestDME2RouteOffer.class );
  @Before
  public void setUp()
  {
    super.setUp();
    System.setProperty("AFT_DME2_COLLECT_SERVICE_STATS", "false");
  }

  @After
  public void tearDown() {
    System.clearProperty( DME2Constants.DME2_MIN_ACTIVE_END_POINTS);
    super.tearDown();
  }

  @Ignore
  @Test
  public void testSetRouteOfferStale() throws Exception {
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");
    System.setProperty( "SCLD_PLATFORM", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
    DME2Manager mgr_1 = null;
    DME2Manager mgr_2 = null;

    String serviceName = "com.att.aft.dme2.test.TestSetRouteOfferStale";
    String serviceVersion = "1.0.0";
    String envContext = "LAB";
    String routeOffer = "TEST_1";
    String routeOffer_2 = "TEST_2";

    
    
    String serviceURI_1 = DME2Utils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );
    String serviceURI_2 = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer_2);

    try
    {

      cleanPreviousEndpoints(serviceName, serviceVersion, envContext);	
      Properties props = new Properties();

      //Publish first service
      mgr_1 = new DME2Manager("testSetRouteOfferStale_1.1", props);
      mgr_1.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_1"));

      //Publish second service
      mgr_2 = new DME2Manager("testSetRouteOfferStale_1.2", props);
      mgr_2.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "testID_2"));
      Thread.sleep(3000);

      //Make a call to the service and validate the routeOffer TEST_1 is consumed.
      String clientURI = String.format("http://DME2SEARCH/service=%s/version=%s/envContext=%s/partner=DME2_TEST", serviceName, serviceVersion, envContext);

      EchoReplyHandler handler = new EchoReplyHandler();
      DME2Client client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.setReplyHandler(handler);
      client.setPayload("THIS IS A TEST");
      client.send();

      String reply =  handler.getResponse(60000);
      System.out.println("Response returned from service = " + reply);
      assertTrue(reply.contains("TEST"));

      //Unpublish service with routeOffer TEST_1
      mgr_1.unbindServiceListener(serviceURI_1);
      Thread.sleep(5000);


      //Refresh the endpoint cache and validate that endpoint with routeOffer TEST_1 is no longer cached
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr_1.getEndpointRegistry();
      //registry.getRegistryEndpointCache().refresh();
      DME2EndpointCacheGRM endpointCache = (DME2EndpointCacheGRM) DME2UnitTestUtil
          .getPrivate( registry.getClass().getDeclaredField( "endpointCache" ), registry );
      endpointCache.refresh();

      //Map<String, DME2ServiceEndpointData> endpointCache = registry.getEndpointCache();
     // System.out.println("Number of Endpoints cached for service " + serviceURI_1 + " (should be 0): " + endpointCache.get(serviceURI_1).getEndpointList().size());
      //assertEquals(0, endpointCache.get(serviceURI_1).getEndpointList().size());
      DME2ServiceEndpointData epData = endpointCache.get( serviceURI_1 );

      if ( epData != null && epData.getEndpointList() != null )
        assertEquals( 0, epData.getEndpointList().size() );
      
      //Validate that routeOffer TEST_1 was marked stale
      //Map<String, Long> staleRouteOfferCache = mgr_1.getStaleCache().getStaleRouteOfferCache().getCache();
      //System.out.println("Contents of stale routeOffer cache: " + staleRouteOfferCache);
      assertTrue( mgr_1.getStaleCache().isRouteOfferStale( serviceURI_1 ) );

      //Map<String, DME2ServiceEndpointData> cache1 = registry.getEndpointCache();
      DME2ServiceEndpointData data = endpointCache.get("/service=com.att.aft.dme2.test.TestSetRouteOfferStale/version=1.0.0/envContext=LAB/routeOffer=TEST_1");
      System.out.println("Endpoints for cache entry: " + data.getEndpointList());
      System.out.println("CacheTTL value for cache entry: " + data.getCacheTTL());

      assertTrue(data.getCacheTTL() == 300000);

      //Send another request and validate that it went to routeOffer TEST_2 and that routeOffer TEST_1 was marked stale in the traceInfo
      EchoReplyHandler handler_2 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_2);
      client.send();
      reply =  handler_2.getResponse(60000);

      System.out.println("Response returned from service = " + reply);
      System.out.println("Response trace info = " + handler_2.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO"));
      //assertTrue(handler_2.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO").contains("STALE_ROUTEOFFER=/service=com.att.aft.dme2.test.TestSetRouteOfferStale/version=1.0.0/envContext=LAB/routeOffer=TEST_1"));

      //Starting service with routeOffer TEST_1 again
      mgr_1.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_1"));
      Thread.sleep(3000);

      //Even though TEST_1 was just brought back up, the RouteOffer is still stale, so if we make another client call, it should go to TEST_2
      EchoReplyHandler handler_3 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_3);
      client.send();
      reply =  handler_3.getResponse(60000);

      System.out.println("Response returned from service = " + reply);
      System.out.println("Response trace info = " + handler_3.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO"));
      //assertTrue(handler_3.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO").contains("STALE_ROUTEOFFER=/service=com.att.aft.dme2.test.TestSetRouteOfferStale/version=1.0.0/envContext=LAB/routeOffer=TEST_1"));

      //Refresh the endpoint cache
      //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI_1);
      endpointCache.refreshCachedEndpoint( serviceURI_1 );
      //Map<String, DME2ServiceEndpointData> cache = registry.getRegistryEndpointCache().getCache();

      System.out.println( "Number of Endpoints cached for service " + serviceURI_1 + " (should be 1): " +
          endpointCache.get( serviceURI_1 ).getEndpointList().size() );
      //assertEquals(1, cache.get(serviceURI_1).getEndpointList().size());
      assertEquals(1, endpointCache.get(serviceURI_1).getEndpointList().size());

      //Make another client call and validate that service with routeOffer TEST_1 is now being used.
      EchoReplyHandler handler_4 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_4);
      client.send();
      reply =  handler_4.getResponse(60000);

      System.out.println("Response returned from service = " + reply);
      System.out.println("Response trace info = " + handler_4.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO"));
      String expected = "/service=com.att.aft.dme2.test.TestSetRouteOfferStale/version=1.0.0/envContext=LAB/routeOffer=TEST_1:onResponseCompleteStatus=200";
      String actual = handler_4.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
      assertTrue(String.format("Did not find expected string [%s] in the trace info [%s]", expected,actual),actual.contains(expected));

    }
    finally
    {
      try
      {
        mgr_1.unbindServiceListener(serviceURI_1);
      }
      catch (DME2Exception e){}

      try
      {
        mgr_2.unbindServiceListener(serviceURI_2);
      }
      catch (DME2Exception e){}

      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
    }
  }

  // For some reason, TEST_1 and TEST_2 both have sequence 1.  in 2.x, TEST_1 and TEST_2 have sequences 1 and 2
  // respectively

  @Ignore
  @Test
  public void testSetRouteOfferStale_2() throws Exception {
    logger.debug( null, "testSetRouteOfferStale_2", LogMessage.METHOD_ENTER );
    System.setProperty( "SCLD_PLATFORM", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
    System.setProperty( DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1" );
    DME2Manager mgr_1 = null;
    DME2Manager mgr_2 = null;
    DME2Manager mgr_3 = null;

    String serviceName = "com.att.aft.dme2.test.TestSetRouteOfferStale_2";
    String serviceVersion = "1.0.0";
    String envContext = "LAB";
    String routeOffer = "TEST_1";
    String routeOffer_2 = "TEST_2";

    String serviceURI_1 = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer);
    String serviceURI_2 = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer_2);

    try
    {
      cleanPreviousEndpoints(serviceName, serviceVersion, envContext);	

      Properties props = new Properties();

      //Publish first service
      mgr_1 = new DME2Manager("testSetRouteOfferStale_2.1", props);
      mgr_1.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_1"));

      //Publish second service
      mgr_2 = new DME2Manager("testSetRouteOfferStale_2.2", props);
      mgr_2.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "testID_2"));

      //Publish a third service with RouteOffer TEST_1
      mgr_3 = new DME2Manager("testSetRouteOfferStale_3.3", props);
      mgr_3.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_3"));
      Thread.sleep(3000);


      //Make a call to the service and validate the routeOffer TEST_1 is consumed.
      String clientURI = String.format("http://DME2SEARCH/service=%s/version=%s/envContext=%s/partner=DME2_TEST", serviceName, serviceVersion, envContext);

      logger.debug( null, "testSetRouteOfferStale_2", "Sending request 1" );
      EchoReplyHandler handler = new EchoReplyHandler();
      DME2Client client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.setReplyHandler(handler);
      client.setPayload("THIS IS A TEST");
      client.send();

      String reply =  handler.getResponse(60000);
      logger.debug( null, "testSetRouteOfferStale_2", "Request 1: Response returned from service = {}", reply);
      assertTrue(reply.contains("TEST_1"));

      //Unpublish the first service with routeOffer TEST_1
      try
      {
        mgr_1.unbindServiceListener(serviceURI_1);
      }
      catch (Exception e)
      {

      }
      Thread.sleep(5000);


      //Refresh the endpoint cache and validate that only one endpoint with routeOffer TEST_1 remains
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr_1.getEndpointRegistry();
      //registry.getRegistryEndpointCache().refresh();
      DME2EndpointCacheGRM endpointCache = (DME2EndpointCacheGRM) DME2UnitTestUtil.getPrivate( registry.getClass().getDeclaredField( "endpointCache" ), registry );
      endpointCache.refresh();

      //Map<String, DME2ServiceEndpointData> endpointCache = registry.getEndpointCache();
      logger.debug( null, "testSetRouteOfferStale_2", "{}", endpointCache);
      logger.debug( null, "testSetRouteOfferStale_2", "Request 1: Number of Endpoints cached for service {} (should be 1): {}",
          serviceURI_1, endpointCache.get( serviceURI_1 ) );
      assertEquals(1, endpointCache.get(serviceURI_1).getEndpointList().size());

      //Validate that routeOffer TEST_1 was NOT marked stale since there is another active Endpoint for TEST_1 routeOffer
      //Map<String, Long> staleRouteOfferCache = mgr_1.getStaleRouteOfferCache().getCache();
      //System.out.println("Contents of stale routeOffer cache: " + staleRouteOfferCache);
      //assertTrue(!staleRouteOfferCache.containsKey(serviceURI_1));
      assertFalse( mgr_3.getStaleCache().isRouteOfferStale( serviceURI_1  ));

      //Send another request. It should find and use the remaining TEST_1 endpoint.
      logger.debug( null, "testSetRouteOfferStale_2", "Sending request 2" );
      EchoReplyHandler handler_2 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_2);
      client.send();
      reply =  handler_2.getResponse(60000);

      logger.debug( null, "testSetRouteOfferStale_2", "Request 2: Response returned from service = {}", reply );
      logger.debug( null, "testSetRouteOfferStale_2", "Request 2: Response trace info = {}",
          handler_2.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" ) );
      assertTrue(handler_2.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO").contains("/service=com.att.aft.dme2.test.TestSetRouteOfferStale_2/version=1.0.0/envContext=LAB/routeOffer=TEST_1"));

      //Shutdown the remaining TEST_1 endpoint to force a failover to the secondary TEST_2 endpoint
      try
      {
        mgr_3.unbindServiceListener(serviceURI_1);
      }
      catch (Exception e)
      {

      }
      Thread.sleep(5000);

      //Refresh the cache. TEST_1 routeOffer should be stale now
      //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI_1);
      endpointCache.refreshCachedEndpoint( serviceURI_1 );

      //endpointCache = registry.getEndpointCache();
      logger.debug( null, "testSetRouteOfferStale_2",
          "Request 2: Number of Endpoints cached for service " + serviceURI_1 + " (should be 0): " +
              endpointCache.get( serviceURI_1 ) );
      assertEquals( 0, endpointCache.get( serviceURI_1 ).getEndpointList().size() );

      //Validate that routeOffer TEST_1 was  marked stale since the other TEST_1 instance was shutdown
      //staleRouteOfferCache = mgr_1.getStaleRouteOfferCache().getCache();
      //System.out.println("Contents of stale routeOffer cache: " + staleRouteOfferCache);
      //assertTrue(staleRouteOfferCache.containsKey(serviceURI_1));
      assertTrue( mgr_1.getStaleCache().isRouteOfferStale( serviceURI_1 ) );

      //This client call should go to TEST_2. Also, response trace should confirm that TEST_1 routeOffer is now stale
      EchoReplyHandler handler_3 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_3);
      client.send();
      reply =  handler_3.getResponse(60000);

      logger.debug( null, "testSetRouteOfferStale_2", "Response returned from service = {}", reply );
      logger.debug( null, "testSetRouteOfferStale_2", "Response trace info = {}",
          handler_3.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" ) );
      //assertTrue(handler_3.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO").contains("STALE_ROUTEOFFER=/service=com.att.aft.dme2.test.TestSetRouteOfferStale_2/version=1.0.0/envContext=LAB/routeOffer=TEST_1"));
      assertTrue(handler_3.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO").contains("/service=com.att.aft.dme2.test.TestSetRouteOfferStale_2/version=1.0.0/envContext=LAB/routeOffer=TEST_2:onResponseCompleteStatus=200"));

      //Bring back up both TEST_1 instances
      mgr_1.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_1"));
      mgr_3.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_3"));
      Thread.sleep(3000);

      //registry = (DME2EndpointRegistryGRM) mgr_1.getEndpointRegistry();
      //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI_1);
      endpointCache.refreshCachedEndpoint( serviceURI_1 );
      //System.out.println("Stale routeOffer cache (Should not see TEST_1 routeOffer): " + mgr_1.getStaleRouteOfferCache().getCache());

      //Now that TEST_1 endpoints is up, make a client request. Because TEST_1 routeOffer will now return endpoints, it will be available again

      EchoReplyHandler handler_4 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_4);
      client.send();
      reply =  handler_4.getResponse(60000);

      logger.debug( null, "testSetRouteOfferStale_2", "Response returned from service = {}", reply );
      logger.debug( null, "testSetRouteOfferStale_2", "Response trace info = {}",
          handler_4.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" ) );
      assertTrue(handler_4.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO").contains("/service=com.att.aft.dme2.test.TestSetRouteOfferStale_2/version=1.0.0/envContext=LAB/routeOffer=TEST_1:onResponseCompleteStatus=200"));


      //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI_1);
      endpointCache.refreshCachedEndpoint( serviceURI_1 );
      //Map<String, DME2ServiceEndpointData> cache = registry.getRegistryEndpointCache().getCache();

      logger.debug( null, "testSetRouteOfferStale_2", "Number of Endpoints cached for service {} (should be 2): {}",
          serviceURI_1, endpointCache.get( serviceURI_1 ).getEndpointList().size() );
      assertEquals(2, endpointCache.get(serviceURI_1).getEndpointList().size());

      //Thread.sleep(120000);
      //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI_1);
      endpointCache.refreshCachedEndpoint( serviceURI_1 );

      //Make another client call and validate that service with routeOffer TEST_1 is now being used.
      EchoReplyHandler handler_5 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_5);
      client.send();
      reply =  handler_5.getResponse(60000);

      System.out.println("Response returned from service = " + reply);
      System.out.println("Response trace info = " + handler_5.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO"));
      String expected = "/service=com.att.aft.dme2.test.TestSetRouteOfferStale_2/version=1.0.0/envContext=LAB/routeOffer=TEST_1:onResponseCompleteStatus=200";
      String actual = handler_5.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
      assertTrue(String.format("Did not find expected string [%s] in the trace info [%s]", expected,actual),actual.contains(expected));


    }
    finally
    {
      try
      {
        mgr_1.unbindServiceListener(serviceURI_1);
      }
      catch (DME2Exception e){}

      try
      {
        mgr_2.unbindServiceListener(serviceURI_2);
      }
      catch (DME2Exception e){}

      try
      {
        mgr_3.unbindServiceListener(serviceURI_1);
      }
      catch (DME2Exception e){}

      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
      logger.debug( null, "testSetRouteOfferStale_2", LogMessage.METHOD_EXIT );
    }
  }

@Ignore
  @Test
  public void testSetRouteOfferStaleForExpiredEndpoint() throws Exception {
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "2");
    DME2Manager mgr = null;

    String serviceName = "com.att.aft.dme2.test.TestSetRouteOfferStaleForExpiredEndpoint";
    String serviceVersion = "1.0.0";
    String envContext = "LAB";
    String routeOffer = "TEST";
    String serviceURI = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer);

    try
    {
      cleanPreviousEndpoints(serviceName, serviceVersion, envContext);	

      Properties props = new Properties();
      props.put(DME2Constants.DME2_SEP_LEASE_LENGTH_MS, "15000");
      props.put("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "3000000"); //Extending the Endpoint lease renewal frequency so that it doesn't kick in during the test
      props.put("DME2_SEP_IGNORE_LEASE_EXPIRED", "false");
      props.put("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "3000000");

      
      mgr = new DME2Manager("testSetRouteOfferStaleForExpiredEndpoint", props);
      mgr.bindServiceListener(serviceURI, new EchoResponseServlet(serviceURI, "testID"));
      
      System.out.println(mgr.getConfig().getProperty(DME2Constants.DME2_SEP_LEASE_LENGTH_MS));
      System.out.println(mgr.getConfig().getProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS"));
      
      Thread.sleep(40000);

      //Attempting to find the Endpoint, but should not get anything back since the lease of the Endpoint had expired by the time it was fetched from GRM
      DME2Endpoint[] endpoints = mgr.getEndpointRegistry().find(serviceName, serviceVersion, envContext, routeOffer);
      assertEquals( 0, endpoints.length );
      
      //RouteOffer should be marked stale for the service. Validate that it was placed in the DME2StaleRouteOfferCache
      //DME2StaleRouteOfferCache staleRouteOfferCache = mgr.getStaleRouteOfferCache();
      //System.out.println("Stale RouteOffer cache contents = " + staleRouteOfferCache.getCache().toString());
      //assertTrue(staleRouteOfferCache.getCache().containsKey(serviceURI));
      assertTrue( mgr.getStaleCache().isRouteOfferStale( serviceURI ));

      //After 2 minutes, the RouteOffer for the the service should be removed from the stale cache, so sleep for some time and check the cache again.
      Thread.sleep(240000);

      //DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr.getEndpointRegistry();
      //registry.clearStaleEndpoints();

      //staleRouteOfferCache = mgr.getStaleRouteOfferCache();
      System.out.println("Stale RouteOffer cache contents after waiting for some time = " + mgr.getStaleCache().getStaleRouteOffers());
      //assertTrue(staleRouteOfferCache.getCache().isEmpty());
      assertTrue( mgr.getStaleCache().getStaleRouteOffers().isEmpty() );
    }
    finally
    {
      try
      {
        mgr.unbindServiceListener(serviceURI);
      }
      catch (DME2Exception e){}

      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
    }
  }

@Ignore
  @Test
  public void testFailureOnAllRouteOffersMarkedStale()
  {
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");
    DME2Manager mgr_1 = null;
    DME2Manager mgr_2 = null;

    String serviceName = "com.att.aft.dme2.test.TestFailureOnAllRouteOffersMarkedStale";
    String serviceVersion = "1.0.0";
    String envContext = "LAB";
    String routeOffer = "TEST_1";
    String routeOffer_2 = "TEST_2";

    String serviceURI_1 = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer);
    String serviceURI_2 = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer_2);

    EchoReplyHandler handler = null;
    EchoReplyHandler handler_2 = null;
    DME2Client client = null;

    try
    {

      cleanPreviousEndpoints(serviceName, serviceVersion, envContext);	
	
      Properties props = new Properties();

      //Publish first service
      mgr_1 = new DME2Manager("testFailureOnAllRouteOffersMarkedStale_1", props);
      mgr_1.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_1"));

      //Publish second service
      mgr_2 = new DME2Manager("testFailureOnAllRouteOffersMarkedStale_2", props);
      mgr_2.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "testID_2"));
      Thread.sleep(5000);

      //Make a call to the service and validate the routeOffer TEST_1 is consumed.
      String clientURI = String.format("http://DME2SEARCH/service=%s/version=%s/envContext=%s/partner=DME2_TEST", serviceName, serviceVersion, envContext);

      handler = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.setReplyHandler(handler);
      client.setPayload("THIS IS A TEST");
      client.send();

      String reply =  handler.getResponse(60000);
      System.out.println("Response returned from service = " + reply);
//      assertTrue(reply.contains("TEST_1"));
      assertTrue(reply.contains("TEST"));

      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr_1.getEndpointRegistry();
      //Map<String, DME2ServiceEndpointData> endpointCache = registry.getEndpointCache();
      DME2EndpointCacheGRM endpointCache = (DME2EndpointCacheGRM) DME2UnitTestUtil.getPrivate( registry.getClass().getDeclaredField( "endpointCache" ), registry );

      DME2EndpointRegistryGRM registry2 = (DME2EndpointRegistryGRM) mgr_2.getEndpointRegistry();
      //Map<String, DME2ServiceEndpointData> endpointCache = registry.getEndpointCache();
      DME2EndpointCacheGRM endpointCache2 = (DME2EndpointCacheGRM) DME2UnitTestUtil.getPrivate( registry2.getClass().getDeclaredField( "endpointCache" ), registry2 );
      
      
      System.out.println("Number of Endpoints cached for service " + serviceURI_1 + " (should be 1): " + endpointCache.get(serviceURI_1).getEndpointList().size());
      assertEquals(1, endpointCache.get(serviceURI_1).getEndpointList().size());

      System.out.println("Number of Endpoints cached for service " + serviceURI_2 + " (should be 1): " + endpointCache.get(serviceURI_2).getEndpointList().size());
      assertEquals(1, endpointCache.get(serviceURI_2).getEndpointList().size());

      //Unpublish service with routeOffer TEST_1 and TEST_2
      try
      {
        mgr_1.unbindServiceListener(serviceURI_1);
      }
      catch (Exception e)
      {
      }


      try
      {
        mgr_2.unbindServiceListener(serviceURI_2);
      }
      catch (Exception e)
      {
      }
      Thread.sleep(5000);



      //Refresh the endpoint cache and validate that endpoint with routeOffer TEST_1 is no longer cached

      //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI_1);
      endpointCache.refreshCachedEndpoint( serviceURI_1 );
      //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI_2);
      endpointCache2.refreshCachedEndpoint( serviceURI_2 );

      //endpointCache = registry.getEndpointCache();
      DME2ServiceEndpointData epData = endpointCache.get( serviceURI_1 );
      System.out.println("Number of Endpoints cached for service " + serviceURI_1 + " (should be 0): " + epData);
//      assertEquals(0, endpointCache.get(serviceURI_1).getEndpointList().size());
      if ( epData != null && epData.getEndpointList() != null )
        assertEquals(epData.getEndpointList().size(),0);

      epData = endpointCache.get( serviceURI_2 );
      System.out.println("Number of Endpoints cached for service " + serviceURI_2 + " (should be 0): " + epData);
//      assertEquals(0, endpointCache.get(serviceURI_2).getEndpointList().size());
      if ( epData != null && epData.getEndpointList() != null  )
        assertEquals(epData.getEndpointList().size(),0);
      
      //Validate that routeOffer TEST_1 and TEST_2 were marked stale
      //Map<String, Long> staleRouteOfferCache = mgr_1.getStaleRouteOfferCache().getCache();
      //System.out.println("Contents of stale routeOffer cache: " + staleRouteOfferCache);
      //assertTrue(staleRouteOfferCache.containsKey(serviceURI_1));
      assertTrue( mgr_1.getStaleCache().isRouteOfferStale( serviceURI_1 ) );
      //assertTrue(staleRouteOfferCache.containsKey(serviceURI_2));
      assertTrue( mgr_2.getStaleCache().isRouteOfferStale( serviceURI_2 ));

      //Bring both services back up
      mgr_1.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_1"));
      mgr_2.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "testID_2"));
      Thread.sleep(3000);

      //Send  a client request. Endpoints should be resolved, but because their routeOffers have been marked stale, the request should fail
      handler_2 = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setPayload("THIS IS A TEST");
      client.setReplyHandler(handler_2);
      client.send();
      reply =  handler_2.getResponse(60000);
      //fail("Error occured in test case - Expecting at AFT-DME2-0702 exception to be thrown. All routeOffers should be marked stale and request should have failed.");

    }
    catch(Exception e)
    {
      e.printStackTrace();
      assertTrue(e.getMessage().contains("AFT-DME2-0702"));
			
			/*RouteOffer staleness check is handled in the DME2EndpointIterator. Its not checked in the Exchange anymore 
			 * and there isn't any handle on how to log it in the ep traceInfo*/

      //System.out.println(mgr_1.getStaleRouteOfferCache().getCache());
      //assertTrue(mgr_1.getStaleRouteOfferCache().getCache().containsKey(serviceURI_1));
      assertNotNull( mgr_1 );
      assertTrue( mgr_1.getStaleCache().isRouteOfferStale( serviceURI_1 ));
      //assertTrue(mgr_1.getStaleRouteOfferCache().getCache().containsKey(serviceURI_2));
      assertTrue( mgr_1.getStaleCache().isRouteOfferStale( serviceURI_2 ));

    }
    finally
    {
      try
      {
        mgr_1.unbindServiceListener(serviceURI_1);
      }
      catch (DME2Exception e){}

      try
      {
        mgr_2.unbindServiceListener(serviceURI_2);
      }
      catch (DME2Exception e){}

      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
    }
  }

@Ignore
  @Test
  public void testSetDME2RouteOfferStale_WithPrimaryServiceDownOnStartup()
  {
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");
    DME2Manager mgr_1 = null;
    DME2Manager mgr_2 = null;

    String serviceName = "com.att.aft.dme2.test.TestSetDME2RouteOfferStale_WithPrimaryServiceDownOnStartup";
    String serviceVersion = "1.0.0";
    String envContext = "LAB";
    String routeOffer = "TEST_1";
    String routeOffer_2 = "TEST_2";

    String serviceURI_1 = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer);
    String serviceURI_2 = DME2Utils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer_2);

    try
    {
    	
      cleanPreviousEndpoints(serviceName, serviceVersion, envContext);	
 	
      Properties props = new Properties();

      //Start with primary service down
      mgr_1 = new DME2Manager("testSetDME2RouteOfferStale_WithPrimaryServiceDownOnStartup_1", props);
      //mgr_1.bindServiceListener(serviceURI_1, new EchoResponseServlet(serviceURI_1, "testID_1"));

      //Publish second service
      mgr_2 = new DME2Manager("testSetDME2RouteOfferStale_WithPrimaryServiceDownOnStartup_2", props);
      mgr_2.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "testID_2"));
      Thread.sleep(3000);

      //Make a call to the service and validate the routeOffer TEST_2 is consumed.
      String clientURI = String.format("http://DME2SEARCH/service=%s/version=%s/envContext=%s/partner=DME2_TEST", serviceName, serviceVersion, envContext);

      EchoReplyHandler handler = new EchoReplyHandler();
      DME2Client client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setReplyHandler(handler);
      client.setPayload("THIS IS A TEST");
      client.send();

      String reply =  handler.getResponse(60000);
      System.out.println("Response returned from service = " + reply);
      assertTrue(reply.contains("TEST_2"));

      handler = new EchoReplyHandler();
      client = new DME2Client(mgr_1, new URI(clientURI), 300000);
      client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
      client.setReplyHandler(handler);
      client.setPayload("THIS IS A TEST");
      client.send();

      reply =  handler.getResponse(60000);
      System.out.println("Response returned from service = " + reply);
      assertTrue(reply.contains("TEST_2"));

      System.out.println("Response trace info = " + handler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO"));
      //assertTrue(handler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO").contains("STALE_ROUTEOFFER=/service=com.att.aft.dme2.test.TestSetRouteOfferStale/version=1.0.0/envContext=LAB/routeOffer=TEST_1"));

    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail(e.getMessage());
    }
    finally
    {
      try
      {
        mgr_1.unbindServiceListener(serviceURI_1);
      }
      catch (Exception e){}

      try
      {
        mgr_2.unbindServiceListener(serviceURI_2);
      }
      catch (Exception e){}

      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
    }
  }
  @Test
  @Ignore
  public void testFailoverWithinTheSameSequence() throws Exception{
    //shorten the cache interval
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");




    String serviceName="com.att.aft.dme2.test.TestFailoverWithinTheSameSequence";
    String version="1.0.0";
    String envContext="DEV";
    String partner = "partner1";

    DME2Manager mgr1 =null;
    DME2Manager mgr2 =null;
    DME2Manager mgr3 =null;

    String serviceURI1 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"FIRST");
    String serviceURI2 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"SECOND");
    String serviceURI3 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"THIRD");

    Map<String,DME2Manager> managers = new HashMap<String,DME2Manager>();

    try{
    	
      cleanPreviousEndpoints(serviceName, version, envContext);	
    	
      //Build our routeInfo
      List<RouteOffer> routeOffers = buildRouteOffers( 1, "FIRST", "SECOND", "THIRD" );
      Route route = buildRoute("rt1", null, null, routeOffers);
      RouteGroup group = buildRouteGroup("RG1", route, partner);
      RouteInfo routeInfo = buildRouteInfo(envContext, serviceName, buildRouteGroups(group));

      //save our routing info
      RegistryFsSetup grm = new RegistryFsSetup();
      RegistryFsSetup.init();
//      grm.saveRouteInfo2(DME2Manager.getDefaultInstance().getConfig(), routeInfo, envContext);

      //set up our three servers.

      //server 1
      Properties props1 = RegistryFsSetup.init();
      mgr1 = new DME2Manager("testFailoverWithinTheSameSequenceManager1", props1);
      mgr1.bindServiceListener(serviceURI1, new EchoServlet(serviceURI1,serviceName));
      managers.put("FIRST", mgr1);



      //server 2
      Properties props2 = RegistryFsSetup.init();
      mgr2 = new DME2Manager("testFailoverWithinTheSameSequenceManager2", props2);
      mgr2.bindServiceListener(serviceURI2,new EchoServlet(serviceURI2,serviceName));
      managers.put("SECOND", mgr2);


      //server3
      Properties props3 = RegistryFsSetup.init();
      mgr3 = new DME2Manager("testFailoverWithinTheSameSequenceManager3", props3);
      mgr3.bindServiceListener(serviceURI3,new EchoServlet(serviceURI3,serviceName));
      managers.put("THIRD", mgr3);

      //now build a client
      Properties clientProps = RegistryFsSetup.init();
      clientProps.setProperty("platform",TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
      DME2Manager mgr = new DME2Manager("Client", clientProps);

      String uriString =buildSearchURIString(serviceName, version, envContext, partner);
      DME2Client client = new DME2Client(mgr, new URI(uriString), 30000l);
      client.setReplyHandler(new EchoReplyHandler());
      client.setPayload("this is a test");



      //make an initial call. Make sure we dont have any stale route offers.
      String FirstReply = client.sendAndWait(30000);
      System.out.println("reply: " + FirstReply);
      //System.out.println("Stale cache entries: " + mgr.getStaleRouteOfferCache().getCache().keySet());
      //assertTrue(String.format("Unexpectedly found entries in the stale route offer cache. [%s]",mgr.getStaleRouteOfferCache().getCache().keySet()),mgr.getStaleRouteOfferCache().getCache().size()==0 );
      assertEquals(String.format("Unexpectedly found entries in the stale route offer cache. [%s]",mgr.getStaleCache().getStaleRouteOffers()),0,mgr.getStaleCache().getStaleRouteOffers().size() );


      //call GRM. Get the searchKey from the route info and determine the order to shut down the servers.
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr.getEndpointRegistry();
      DME2RouteInfo ri = registry.getRouteInfo(serviceName, version, envContext);
      String searchFilter= ri.getRouteOffers(envContext, partner, null, null, false).get(0).getSearchFilter();
      System.out.println("Filter: " + searchFilter);

      String[] offers = searchFilter.split( DME2Constants.DME2_ROUTE_OFFER_SEP );

      List<String> expectedStale = new ArrayList<String>();
      //shut down the first two. refresh, then check
      for(int i=0; i<offers.length; i++){
        String offer = offers[i];
        String serviceURI = DME2Utils.buildServiceURIString(serviceName, version, envContext,offer);
        DME2Manager m = managers.get(offer);
        System.out.println("Shutting down service "+ serviceURI);
        m.unbindServiceListener(serviceURI);
        expectedStale.add(serviceURI);
        //sleep to let the server shutdown complete
        Thread.sleep(5000);
        //refresh the cache
        registry = (DME2EndpointRegistryGRM) mgr.getEndpointRegistry();
        //registry.getRegistryEndpointCache().refreshCachedEndpoint(serviceURI);
        DME2EndpointCacheGRM endpointCache = (DME2EndpointCacheGRM) DME2UnitTestUtil.getPrivate( registry.getClass().getDeclaredField( "endpointCache" ), registry );
        endpointCache.refreshCachedEndpoint( serviceURI );

        //set up a new client
        client = new DME2Client(mgr, new URI(uriString), 30000l);
        client.setReplyHandler(new EchoReplyHandler());
        client.setPayload("this is a test");

        //for the first two we expect success
        if (i<offers.length-1){
          logger.debug( null, "testFailoverWithinTheSameSequence", "Iteration {} Checking for route offer {}", i,
              offer );
          //make another call. Check to make sure that we get a response. Make sure that the stale cache has only entries for the servers we shut down
          String reply = client.sendAndWait(30000);
          logger.debug( null, "testFailoverWithinTheSameSequence", "Iteration {} reply: {}", i, reply );

          //System.out.println("Stale cache entries: " + mgr.getStaleRouteOfferCache().getCache().keySet());
          logger.debug( null, "testFailoverWithinTheSameSequence", "Iteration {} stale cache entries expected: {}", i, expectedStale);
          logger.debug( null, "testFailoverWithinTheSameSequence", "Iteration {} stale cache entries: {}", i, mgr.getStaleCache().getStaleRouteOffers());

          //check to make sure that the stale cache has the entry for the shut down server.
          //assertTrue(String.format("Unexpectedly state in the stale route offer cache[%s]",mgr.getStaleRouteOfferCache().getCache().keySet()),mgr.getStaleRouteOfferCache().getCache().keySet().containsAll(expectedStale));
          assertTrue(String.format("Unexpectedly state in the stale route offer cache[%s]",mgr.getStaleCache().getStaleRouteOffers()),mgr.getStaleCache().getStaleRouteOffers().containsAll(expectedStale));
          //check to make sure we dont have any extras on the stale cache
          //assertEquals(String.format("Did not find the expected number of stale entries in the cache[%s]. Cache: [%s]",expectedStale.size(), mgr.getStaleRouteOfferCache().getCache().keySet()),i+1, mgr.getStaleRouteOfferCache().getCache().keySet().size());
          assertEquals(String.format("Did not find the expected number of stale entries in the cache[%s]. Cache: [%s]",expectedStale.size(), mgr.getStaleCache().getStaleRouteOffers()),i+1, mgr.getStaleCache().getStaleRouteOffers().size());
          //make sure the response did not come from the shut down server.
          assertFalse(String.format("Unexpectedly received the reply from the %s route offer. [%s]", offer, reply),reply != null && reply.contains(serviceURI));

        } else{
          //now with the last one shut down, We  the call to the service to fail in this case, as all servers are down.
          String reply = null;
          ////The DEFAULT route offer should now be stale, since all the endpoints are down.
          expectedStale.add(DME2Utils.buildServiceURIString(serviceName, version, envContext,"DEFAULT"));
          try {
            //make another call. Check to make sure that we get a response. Make sure that the stale cache has only entries for the servers we shut down, plus the DEFAULT route offer
            reply = client.sendAndWait(30000);
            System.out.println("reply: " + reply);
            //System.out.println("Stale cache entries: " + mgr.getStaleRouteOfferCache().getCache().keySet());
            System.out.println("Stale cache entries: " + mgr.getStaleCache().getStaleRouteOffers());
            fail("Should have thown an exception since all servers are shut down.");
          }
          catch (DME2Exception e) {
            //System.out.println("Stale cache entries: " + mgr.getStaleRouteOfferCache().getCache().keySet());
            System.out.println("Stale cache entries: " + mgr.getStaleCache().getStaleRouteOffers());
            // check to make sure that the stale cache has the entries for all the shut down servers
            //assertTrue(String.format("Unexpectedly found entries in the stale route offer cache. [%s]. Expected: %s", mgr.getStaleRouteOfferCache().getCache().keySet(), expectedStale),	mgr.getStaleRouteOfferCache().getCache().keySet().containsAll(expectedStale));
            assertTrue(String.format("Unexpectedly found entries in the stale route offer cache. [%s]. Expected: %s", mgr.getStaleCache().getStaleRouteOffers(), expectedStale),	mgr.getStaleCache().getStaleRouteOffers().containsAll(expectedStale));
            //
            //assertEquals(String.format("Did not find the expected number of stale entries in the cache[%s]. Cache: [%s]",expectedStale.size(), mgr.getStaleRouteOfferCache().getCache().keySet()),expectedStale.size(), mgr.getStaleRouteOfferCache().getCache().keySet().size());
            assertTrue( String
                .format( "Did not find the expected number of stale entries in the cache[%s]. Cache: [%s]",
                    expectedStale.size(), mgr.getStaleCache().getStaleRouteOffers() ),
                mgr.getStaleCache().getStaleRouteOffers().containsAll( expectedStale ) );
            // make sure the response did not come.
            assertTrue(String.format("Unexpectedly received the reply from a server that should have been shut down. [%s]", reply), reply == null);

          } finally{
            //cleanup.
            mgr.shutdown();
          }
        }

      }

    } finally{
      if(mgr1 != null){
        try {
          mgr1.unbindServiceListener(serviceURI1);
        }catch (Exception e){}
      }
      if(mgr2 != null){
        try {
          mgr2.unbindServiceListener(serviceURI2);
        }
        catch (Exception e) {
        }
      }
      if(mgr3 != null){
        try {
          mgr3.unbindServiceListener(serviceURI3);
        }catch (Exception e){}
      }
      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
    }

  }


  @Test
  @Ignore
  public void testFailoverWithMinActiveEndPoints() throws Exception{
    //Assuming min = 2....if there were 2 on sequence 1, and one of the endpoints on sequence 1 was shut down
    //we should add all of the endpoints from sequence 2.
    //if the sequence 1 endpoint comes back, we should no longer be using any of the endpoints from sequence 2

    //shorten the cache interval
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");

    String serviceName="com.att.aft.dme2.test.TestFailoverWithMinActiveEndPoints";
    String version="1.0.0";
    String envContext="DEV";
    String partner = "partner1";

    DME2Manager mgr1 =null;
    DME2Manager mgr2 =null;
    DME2Manager mgr3 =null;
    DME2Manager mgr4 =null;
    DME2Manager mgr5 =null;
    DME2Manager mgr6 =null;


    String serviceURI1 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"1A");
    String serviceURI2 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"1B");
    String serviceURI3 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"2A");
    String serviceURI4 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"2B");
    String serviceURI5 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"3A");
    String serviceURI6 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"3B");


    List<AssertionError> errors = new ArrayList<AssertionError>();
    try{
    	
      cleanPreviousEndpoints(serviceName, version, envContext);	
    	
      //Build our routeInfo
      //6 route offers, evenly distributed accross 3 sequences
      List<RouteOffer> routeOffers = buildRouteOffers(1,"1A");
      routeOffers.addAll(buildRouteOffers(1,"1B"));
      routeOffers.addAll(buildRouteOffers(2,"2A"));
      routeOffers.addAll(buildRouteOffers(2,"2B"));
      routeOffers.addAll(buildRouteOffers(3,"3A"));
      routeOffers.addAll(buildRouteOffers(3,"3B"));

      Route route = buildRoute("rt1", null, null, routeOffers);
      RouteGroup group = buildRouteGroup("RG1", route, partner);
      RouteInfo routeInfo = buildRouteInfo(envContext, serviceName, buildRouteGroups(group));

      //save our routing info
      RegistryFsSetup grm = new RegistryFsSetup();
      grm.init();
//      grm.saveRouteInfo2(DME2Manager.getDefaultInstance().getConfig(), routeInfo, envContext);
      //set up our three servers.

      //server 1
      Properties props1 = RegistryFsSetup.init();
      mgr1 = new DME2Manager("Manager1", props1);
      mgr1.bindServiceListener(serviceURI1, new EchoServlet(serviceURI1,serviceName));


      //server 2
      Properties props2 = RegistryFsSetup.init();
      mgr2 = new DME2Manager("Manager2", props2);
      mgr2.bindServiceListener(serviceURI2,new EchoServlet(serviceURI2,serviceName));

      //server3
      Properties props3 = RegistryFsSetup.init();
      mgr3 = new DME2Manager("Manager3", props3);
      mgr3.bindServiceListener(serviceURI3,new EchoServlet(serviceURI3,serviceName));

      //server4
      Properties props4 = RegistryFsSetup.init();
      mgr4 = new DME2Manager("Manager4", props4);
      mgr4.bindServiceListener(serviceURI4,new EchoServlet(serviceURI4,serviceName));

      //server5
      Properties props5 = RegistryFsSetup.init();
      mgr5 = new DME2Manager("Manager5", props5);
      mgr5.bindServiceListener(serviceURI5,new EchoServlet(serviceURI5,serviceName));

      //server6
      Properties props6 = RegistryFsSetup.init();
      mgr6 = new DME2Manager("Manager6", props6);
      mgr6.bindServiceListener(serviceURI6,new EchoServlet(serviceURI6,serviceName));

      Thread.sleep(3000);
      //set up our expected results
      String template="[RO:%s|SEQ:%s]";
      List<String> expected;

      //scenario 1.
      //all servers running. min active = count in seq 1. Should only have seq 1 in headers.
      //1A, 1B, seq 1
      Collection<String> references = getEPReferenceHeaderList("Scenario1Manager", serviceName, version, envContext, partner, 2);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",1));
      expected.add(String.format(template, "1B",1));
      System.out.println("Expecting references: " + expected);

      errors.addAll(compare(expected,references,"scenario 1- 1A, 1B, seq 1"));

      //scenario 2
      //all servers running. min active = (count in seq 1)+1. Should have seq 2 in headers.
      //1A, 1B, 2A, 2C seq 2
      references = getEPReferenceHeaderList("Scenario2Manager", serviceName, version, envContext, partner, 3);

      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",2));
      expected.add(String.format(template, "1B",2));
      expected.add(String.format(template, "2A",2));
      expected.add(String.format(template, "2B",2));
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 2 - 1A, 1B, 2A, 2C seq 2"));
      //scenario 3
      //all servers running. min active = (count in seq1+seq2). Should have seq 2 in headers.
      //1A, 1B, 2A, 2B seq 2
      references = getEPReferenceHeaderList("Scenario3Manager", serviceName, version, envContext, partner, 4);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",2));
      expected.add(String.format(template, "1B",2));
      expected.add(String.format(template, "2A",2));
      expected.add(String.format(template, "2B",2));
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 3 - 1A, 1B, 2A, 2B seq 2"));

      //scenario 4
      //all servers running. min active = (count in seq1+seq2)+1. Should have seq 3 in headers.
      //1A, 1B, 2A, 2B, 3A, 3B seq 3
      references = getEPReferenceHeaderList("Scenario4Manager", serviceName, version, envContext, partner, 5);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",3));
      expected.add(String.format(template, "1B",3));
      expected.add(String.format(template, "2A",3));
      expected.add(String.format(template, "2B",3));
      expected.add(String.format(template, "3A",3));
      expected.add(String.format(template, "3B",3));
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 4 - 1A, 1B, 2A, 2B, 3A, 3B seq 3"));

      //scenario 5
      //all servers running. min active = (count in seq1+seq2+seq3). Should have seq 3 in headers.
      //1A, 1B, 2A, 2B, 3A, 3B seq 3
      references = getEPReferenceHeaderList("Scenario5Manager", serviceName, version, envContext, partner, 6);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",3));
      expected.add(String.format(template, "1B",3));
      expected.add(String.format(template, "2A",3));
      expected.add(String.format(template, "2B",3));
      expected.add(String.format(template, "3A",3));
      expected.add(String.format(template, "3B",3));
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 5 - 1A, 1B, 2A, 2B, 3A, 3B seq 3"));

      //scenario 6	--make sure we dont have issues if the min active exceeds the total number of endpoints
      //all servers running. min active = (count in seq1+seq2+seq3)+1. Should have seq 3 in headers.
      //1A, 1B, 2A, 2B, 3A, 3B seq 3
      references = getEPReferenceHeaderList("Scenario6Manager", serviceName, version, envContext, partner, 7);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",3));
      expected.add(String.format(template, "1B",3));
      expected.add(String.format(template, "2A",3));
      expected.add(String.format(template, "2B",3));
      expected.add(String.format(template, "3A",3));
      expected.add(String.format(template, "3B",3));
      System.out.println("Expecting references: " + expected);
      try{
        assertTrue("Looks like we had a problem when the minActiveEndpoints value exceeded the total number of endpoints", references != null);
      } catch (AssertionError e){
        errors.add(e);
      }
      errors.addAll(compare(expected,references,"scenario 6 - 1A, 1B, 2A, 2B, 3A, 3B seq 3"));


      //scenario 7
      //one seq 1 server down. min active = count in seq 1.  Should have seq 1 and seq 2 in headers.
      //1B, 2A, 2B seq 2

      logger.debug( null, "testFailoverWithMinActiveEndPoints", "STARTING SCENARIO 7" );
      try{
        mgr1.shutdown();
      } catch(Exception e){}

      references = getEPReferenceHeaderList("Scenario7Manager", serviceName, version, envContext, partner, 2);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1B",2));
      expected.add(String.format(template, "2A",2));
      expected.add(String.format(template, "2B",2));
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 7 - 1B, 2A, 2B seq 2"));

      logger.debug( null, "testFailoverWithMinActiveEndPoints", "COMPLETED SCENARIO 7" );
      //scenario 8
      //two seq 1 server down. min active = count in seq 1.  Should have seq 2 in headers.
      //2A, 2B

      try{
        mgr2.shutdown();
      } catch(Exception e){}
      expected = new ArrayList<String>();
      expected.add(String.format(template, "2A",2));
      expected.add(String.format(template, "2B",2));
      System.out.println("Expecting references: " + expected);
      references = getEPReferenceHeaderList("Scenario8Manager", serviceName, version, envContext, partner, 2);

      errors.addAll(compare(expected,references,"scenario 8 - 2A, 2B seq 2"));
      //scenario 9
      //one seq 2 server down. min active = count in seq 1.  Should have seq 2 and seq 3 in headers.
      //2B, 3A, 3B seq 3
      expected = new ArrayList<String>();
      expected.add(String.format(template, "2B",3));
      expected.add(String.format(template, "3A",3));
      expected.add(String.format(template, "3B",3));
      System.out.println("Expecting references: " + expected);
      try{
        mgr3.shutdown();
      } catch(Exception e){}

      references = getEPReferenceHeaderList("Scenario9Manager", serviceName, version, envContext, partner, 2);
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 9 - 2B, 3A, 3B seq 3"));

      //scenario 10
      //two seq 2 server down. min active = count in seq 1.  Should have seq 3 in headers.
      //3A, 3B
      try{
        mgr4.shutdown();
      } catch(Exception e){}
      expected = new ArrayList<String>();
      expected.add(String.format(template, "3A",3));
      expected.add(String.format(template, "3B",3));
      references = getEPReferenceHeaderList("Scenario10Manager", serviceName, version, envContext, partner, 2);
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 10 - 3A, 3B seq 3"));



      //scenario 11. again, make sure we dont have a problem if the min active exceeds the number of running endpoints.
      //3B
      try{
        mgr5.shutdown();
      } catch(Exception e){}
      expected = new ArrayList<String>();
      expected.add(String.format(template, "3B",3));
      references = getEPReferenceHeaderList("Scenario11Manager", serviceName, version, envContext, partner, 2);
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected,references,"scenario 11 - 3B seq 3"));

      //scneario 12. server from lower sequence comes back
      //now lets start some back up. Should end up with 1A and 3B
      //1A, 3B seq 3
      mgr1 = new DME2Manager("Manager1", RegistryFsSetup.init());
      mgr1.bindServiceListener(serviceURI1, new EchoServlet(serviceURI1,serviceName));
      //Thread.sleep(3000);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",3));
      expected.add(String.format(template, "3B",3));
      System.out.println("Expecting references: " + expected);
      references = getEPReferenceHeaderList("Scenario12Manager", serviceName, version, envContext, partner, 2);
      errors.addAll(compare(expected,references,"scenario 12-1A, 3B seq 3"));


      //scneario 13.
      //now lets start some back up.
      //1A, 2A seq 2
      mgr3 = new DME2Manager("Manager3", RegistryFsSetup.init());
      mgr3.bindServiceListener(serviceURI3, new EchoServlet(serviceURI3,serviceName));
      //Thread.sleep(3000);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",2));
      expected.add(String.format(template, "2A",2));
      System.out.println("Expecting references: " + expected);
      references = getEPReferenceHeaderList("Scenario13Manager", serviceName, version, envContext, partner, 2);
      errors.addAll(compare(expected,references,"scenario 13-1A, 2A seq 2"));

      //scneario 14.
      //1A, 1B seq 1

      mgr2 = new DME2Manager("Manager2", RegistryFsSetup.init());
      mgr2.bindServiceListener(serviceURI2, new EchoServlet(serviceURI2,serviceName));
      //Thread.sleep(3000);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",1));
      expected.add(String.format(template, "1B",1));
      System.out.println("Expecting references: " + expected);
      references = getEPReferenceHeaderList("Scenario14Manager", serviceName, version, envContext, partner, 2);
      errors.addAll(compare(expected,references,"scenario 14-1A, 1B seq 1"));




    } finally{
      if(mgr1 != null){
        try {
          mgr1.shutdown();
        }catch (Throwable e){}
      }
      if(mgr2 != null){
        try {
          mgr2.shutdown();
        }catch (Throwable e){}
      }
      if(mgr3 != null){
        try {
          mgr3.shutdown();
        }catch (Throwable e){}
      }
      if(mgr4 != null){
        try {
          mgr4.shutdown();
        }catch (Throwable e){}
      }
      if(mgr5 != null){
        try {
          mgr5.shutdown();
        }catch (Throwable e){}
      }
      if(mgr6 != null){
        try {
          mgr6.shutdown();
        }catch (Throwable e){}
      }
      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);

      if(errors.size() !=0){
        fail(errors.toString());
      }
    }


  }

  private Set<String> getEPReferenceHeaderList(String managerName, String serviceName, String version, String envContext, String partner, int minActiveEndpoints) throws Exception{
    String uriString =buildSearchURIString(serviceName, version, envContext, partner);
    if(minActiveEndpoints >0){
      uriString = DME2Utils.appendQueryStringToPath(uriString, "minActiveEndPoints="+minActiveEndpoints);
    }

    DME2Manager clientMgr = null;
    Set<String> references = new HashSet<String>();
    try{
      //set up a new client
      Properties clientProperties = RegistryFsSetup.init();
      clientProperties.setProperty("platform",TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
      // In 2.x this creates a new manager every time (with a clean cache).  This is not the case in 3.x!  It will
      // reuse the same manager, registry and route info cache with the previously updated sequences in the route
      // info!
      clientMgr = new DME2Manager(managerName, clientProperties);

      DME2Client c = new DME2Client(clientMgr, new URI(uriString), 30000l);
      EchoReplyHandler echoRepHandler=new EchoReplyHandler();
      Map<String, String> headers = new HashMap<String, String>();
      headers.put("AFT_DME2_REQ_TRACE_ON", "true");
      c.setHeaders(headers);
      c.setReplyHandler(echoRepHandler);
      c.setPayload("this is a test");
      c.send();
      String r = echoRepHandler.getResponse(60000);
      System.out.println("reply: " + r);
      String trace=	echoRepHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
      System.out.println("trace: "+ trace);

      //split the trace to pull out the EPReferences
      int start = trace.indexOf("EPREFERENCES=[")+14;
      String refs = trace.substring(start, trace.indexOf("]];"));
      references.addAll(Arrays.asList(refs.split(",")));




      return references;
    } finally {
      if(clientMgr != null){
        try {
          clientMgr.shutdown();
        }catch (Exception e){}
      }
    }
  }

  @Test
  @Ignore
  public void testFailoverWithNoMinActiveEndPoints() throws Exception{
    //Tests to make sure that the logic that pulls in endpoint refs from other sequences does not get invoked without the property set
    //shorten the cache interval
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");
    String serviceName="com.att.aft.dme2.test.TestFailoverWithNoMinActiveEndPoints";
    String version="1.0.0";
    String envContext="DEV";
    String partner = "partner1";

    DME2Manager mgr1 =null;
    DME2Manager mgr2 =null;
    DME2Manager mgr3 =null;


    String serviceURI1 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"1A");
    String serviceURI2 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"1B");
    String serviceURI3 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"2A");



    List<AssertionError> errors = new ArrayList<AssertionError>();


    try{
    	
      cleanPreviousEndpoints(serviceName, version, envContext);	
    	
      //Build our routeInfo
      List<RouteOffer> routeOffers = buildRouteOffers(1,"1A");
      routeOffers.addAll(buildRouteOffers(1,"1B"));
      routeOffers.addAll(buildRouteOffers(2,"2A"));

      Route route = buildRoute("rt1", null, null, routeOffers);
      RouteGroup group = buildRouteGroup("RG1", route, partner);
      RouteInfo routeInfo = buildRouteInfo(envContext, serviceName, buildRouteGroups(group));

      //save our routing info
      RegistryFsSetup grm = new RegistryFsSetup();
      grm.init();
//      grm.saveRouteInfo2(DME2Manager.getDefaultInstance().getConfig(), routeInfo, envContext);


      String template="[RO:%s|SEQ:%s]";
      List<String> expected;
      Collection<String> references;

      //scenario 1- 1A only
      //server 1
      Properties props1 = RegistryFsSetup.init();
      mgr1 = new DME2Manager("Manager1", props1); //1A
      mgr1.bindServiceListener(serviceURI1, new EchoServlet(serviceURI1,serviceName));
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",1));

      Thread.sleep(2000);

      references = getEPReferenceHeaderList("Scenario1Manager", serviceName, version, envContext, partner, 0);
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected, references,"Scenario 1 - 1A seq 1"));


      //scenario 2- 1A Seq1, 2A seq 2
      //server3
      Properties props3 = RegistryFsSetup.init();
      mgr3 = new DME2Manager("Manager3", props3); //2A
      mgr3.bindServiceListener(serviceURI3,new EchoServlet(serviceURI3,serviceName));
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",1));
      expected.add(String.format(template, "2A",2));
      Thread.sleep(2000);

      references = getEPReferenceHeaderList("Scenario2Manager", serviceName, version, envContext, partner, 0);
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected, references,"Scenario 2 - 1A seq 1, 2A seq 2"));

      //scenario 3- 1B
      //start server 2
      Properties props2 = RegistryFsSetup.init();
      mgr2 = new DME2Manager("Manager2", props2); //2B
      mgr2.bindServiceListener(serviceURI2,new EchoServlet(serviceURI2,serviceName));

      Thread.sleep(1000);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",1));
      expected.add(String.format(template, "1B",1));
      expected.add(String.format(template, "2A",2));
      Thread.sleep(2000);

      references = getEPReferenceHeaderList("Scenario3Manager", serviceName, version, envContext, partner, 0);
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected, references,"Scenario 3 - 1A, 1B seq 1, 2A seq 2"));
      //shut down server 1
      try{
        mgr1.shutdown();
      } catch(Exception e){

      }
      Thread.sleep(1000);

      //make sure only server 1B is in the list now for sequence 1
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1B",1));
      expected.add(String.format(template, "2A",2));
      Thread.sleep(2000);
      references = getEPReferenceHeaderList("Scenario4Manager", serviceName, version, envContext, partner, 0);
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected, references,"Scenario 4 - 1B seq 1, 2A seq 2"));

    }catch(DME2Exception e){
        e.printStackTrace();
      fail("Unexpected exception :" + e.getMessage());
    } finally{
      if(mgr1 != null){
        try {
          mgr1.shutdown();
        }catch (Throwable e){}
      }
      if(mgr2 != null){
        try {
          mgr2.shutdown();
        }catch (Throwable e){}
      }
      if(mgr3 != null){
        try {
          mgr3.shutdown();
        }catch (Throwable e){}
      }
      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);

      if(errors.size()!= 0){
        fail(errors.toString());
      }
    }

  }


  @Test
  @Ignore
  public void testFailoverWithMinActiveEndPointsSystemProperty() throws Exception{

    //shorten the cache interval
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");
    System.setProperty(DME2Constants.DME2_MIN_ACTIVE_END_POINTS,"2");

    String serviceName="com.att.aft.dme2.test.TestFailoverWithMinActiveEndPointsSystemProperty";
    String version="1.0.0";
    String envContext="DEV";
    String partner = "partner1";

    DME2Manager mgr1 =null;
    DME2Manager mgr2 =null;
    DME2Manager mgr3 =null;
    DME2Manager mgr4 =null;



    String serviceURI1 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"1A");
    String serviceURI2 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"1B");
    String serviceURI3 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"2A");
    String serviceURI4 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"2B");

    List<AssertionError> errors = new ArrayList<AssertionError>();

    try{
    	
      cleanPreviousEndpoints(serviceName, version, envContext);	
    	
      //Build our routeInfo
      List<RouteOffer> routeOffers = buildRouteOffers(1,"1A");//0
      routeOffers.addAll(buildRouteOffers(1,"1B"));//1
      routeOffers.addAll(buildRouteOffers(2,"2A"));//2
      routeOffers.addAll(buildRouteOffers(2,"2B"));//3




      Route route = buildRoute("rt1", null, null, routeOffers);
      RouteGroup group = buildRouteGroup("RG1", route, partner);
      RouteInfo routeInfo = buildRouteInfo(envContext, serviceName, buildRouteGroups(group));

      //save our routing info
      RegistryFsSetup grm = new RegistryFsSetup();
      grm.init();
//      grm.saveRouteInfo2(DME2Manager.getDefaultInstance().getConfig(), routeInfo, envContext);

      //set up our three servers.

      //server 1 1A
      Properties props1 = RegistryFsSetup.init();
      mgr1 = new DME2Manager("testFailoverWithMinActiveEndPointsSystemPropertyManager1", props1);
      mgr1.bindServiceListener(serviceURI1, new EchoServlet(serviceURI1,serviceName));

      //server 2 1B
      Properties props2 = RegistryFsSetup.init();
      mgr2 = new DME2Manager("testFailoverWithMinActiveEndPointsSystemPropertyManager2", props2);
      mgr2.bindServiceListener(serviceURI2,new EchoServlet(serviceURI2,serviceName));


      //server3 2A
      Properties props3 = RegistryFsSetup.init();
      mgr3 = new DME2Manager("testFailoverWithMinActiveEndPointsSystemPropertyManager3", props3);
      mgr3.bindServiceListener(serviceURI3,new EchoServlet(serviceURI3,serviceName));



      //server4 2B
      Properties props4 = RegistryFsSetup.init();
      mgr4 = new DME2Manager("testFailoverWithMinActiveEndPointsSystemPropertyManager4", props4);
      mgr4.bindServiceListener(serviceURI4,new EchoServlet(serviceURI4,serviceName));

      Thread.sleep(3000);

      //set up our expected results
      String template="[RO:%s|SEQ:%s]";
      List<String> expected;

      //scenario 1.
      //all managers running. min active = count in seq 1. Should only have seq 1 in headers.
      //1A, 1B, seq 1
      Set<String> references = getEPReferenceHeaderList("testFailoverWithMinActiveEndPointsSystemPropertyScenario1Manager", serviceName, version, envContext, partner, 0);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",1));
      expected.add(String.format(template, "1B",1));
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected, references,"Scenario 1 - 1A, 1B, seq 1"));
      //scenario 2
      //all managers running. min active = (count in seq 1)+1. Should have seq 2 in headers.
      //1A, 1B, 2A, 2C seq 2

      try{
        mgr2.shutdown();
      } catch(Exception e){

      }
      Thread.sleep(4000);

      references = getEPReferenceHeaderList("testFailoverWithMinActiveEndPointsSystemPropertyScenario2Manager", serviceName, version, envContext, partner, 0);
      expected = new ArrayList<String>();
      expected.add(String.format(template, "1A",2));
      expected.add(String.format(template, "2A",2));
      expected.add(String.format(template, "2B",2));
      System.out.println("Expecting references: " + expected);
      errors.addAll(compare(expected, references,"Scenario 2 - 1A, 1B, 2A, 2C seq 2"));


    }catch(DME2Exception e){
        e.printStackTrace();
      fail("Unexpected exception: " + e.getMessage());

    } finally{
      if(mgr1 != null){
        try {
          mgr1.shutdown();
        }catch (Throwable e){}
      }
      if(mgr2 != null){
        try {
          mgr2.shutdown();
        }catch (Throwable e){}
      }
      if(mgr3 != null){
        try {
          mgr3.shutdown();
        }catch (Throwable e){}
      }
      if(mgr4 != null){
        try {
          mgr4.shutdown();
        }catch (Throwable e){}
      }
      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
      System.clearProperty(DME2Constants.DME2_MIN_ACTIVE_END_POINTS);
      if(errors.size() !=0){
        fail(errors.toString());
      }
    }

  }


  @Test
  @Ignore
  public void testFailoverWithMinActiveEndPoints_OverRideQueryParamTest() throws Exception{
    //shorten the cache interval
    System.setProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "1");
    //setting the system property to 2. We will use queryParms to override this
    System.setProperty(DME2Constants.DME2_MIN_ACTIVE_END_POINTS,"2");

    String serviceName="com.att.aft.dme2.test.TestFailoverWithMinActiveEndPoints_OverRideQueryParamTest";
    String version="1.0.0";
    String envContext="DEV";
    String partner = "partner1";

    DME2Manager mgr1 =null;


    String serviceURI1 = DME2Utils.buildServiceURIString(serviceName, version, envContext,"1A");


    DME2Manager clientMgr = null;
    try{
    	
      cleanPreviousEndpoints(serviceName, version, envContext);	
    	
      //Build our routeInfo
      List<RouteOffer> routeOffers = buildRouteOffers(1,"1A");



      Route route = buildRoute("rt1", null, null, routeOffers);
      RouteGroup group = buildRouteGroup("RG1", route, partner);
      RouteInfo routeInfo = buildRouteInfo(envContext, serviceName, buildRouteGroups(group));

      //save our routing info
      RegistryFsSetup grm = new RegistryFsSetup();
      grm.init();
//      grm.saveRouteInfo2(DME2Manager.getDefaultInstance().getConfig(), routeInfo, envContext);


      //server 1
      Properties props1 = RegistryFsSetup.init();
      mgr1 = new DME2Manager("testFailoverWithMinActiveEndPoints_OverRideQueryParamTestManager1", props1);
      mgr1.bindServiceListener(serviceURI1, new EchoServlet(serviceURI1,serviceName));

      String uriString =buildSearchURIString(serviceName, version, envContext, partner);
      uriString=DME2Utils.appendQueryStringToPath(uriString, "minActiveEndPoints=4");


      Properties clientProperties = RegistryFsSetup.init();
      clientProperties.setProperty("platform",TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
      clientMgr = new DME2Manager("Client", clientProperties);

      DME2Client c = new DME2Client(clientMgr, new URI(uriString), 30000l);
      EchoReplyHandler echoRepHandler=new EchoReplyHandler();
      Map<String, String> headers = new HashMap<String, String>();
      headers.put("AFT_DME2_REQ_TRACE_ON", "true");
      c.setHeaders(headers);
      c.setReplyHandler(echoRepHandler);
      c.setPayload("this is a test");
      c.send();
      String r = echoRepHandler.getResponse(60000);
      System.out.println("reply: " + r);
      String trace=	echoRepHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
      String expected = "MINACTIVEENDPOINTS=4";
      System.out.println("Trace: " + trace);
      assertTrue(String.format("Did not find expected string %s in trace. Trace: [%s]", expected, trace),trace.contains(expected));

    }catch(DME2Exception e){
      fail("Unexpected exception: " + e.getMessage());
      e.printStackTrace();


    } finally{
      if(mgr1 != null){
        try {
          mgr1.shutdown();
        }catch (Throwable e){}
      }

      System.clearProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
      System.clearProperty(DME2Constants.DME2_MIN_ACTIVE_END_POINTS);
    }

  }





  private static String buildSearchURIString(String serviceName, String version, String envContext, String partner,String...params)
  {
    if (serviceName == null || version == null || envContext == null || partner == null)
    {
      return null;
    }

    return String.format("http://DME2SEARCH/service=%s/version=%s/envContext=%s/partner=%s", serviceName, version, envContext, partner);

  }

  private List<AssertionError> compare(Collection<String> expected, Collection<String> actual, String scenario){
    System.out.println("Actual: " + actual);
    List<AssertionError> errors = new ArrayList<AssertionError>();
    try{
      assertTrue(String.format("Did not find expected strings %s in the returned endpoint references %s for scenario %s", expected, actual, scenario), actual.containsAll(expected));
    } catch(AssertionError e){
      errors.add(e);
    }

    try{
      assertEquals(String.format("Did not find expected number of references %s in trace %s for scenario %s",expected.size(), actual, scenario), expected.size(), actual.size());
    } catch(AssertionError e){
      errors.add(e);
    }

    return errors;
  }


}
