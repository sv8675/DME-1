/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContextListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Utils;

public class TestDME2ServiceHolder_testRestfulService extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestDME2ServiceHolder_testRestfulService.class );

  @Before
  public void setUp() {
    super.setUp();
    System.setProperty( "AFT_DME2_PUBLISH_METRICS", "false" );
    System.setProperty( "org.eclipse.jetty.util.UrlEncoding.charset", "UTF-8" );
    System.setProperty( "metrics.debug", "true" );
  }

  @After
  public void tearDown() {
    super.tearDown();
    System.clearProperty( "AFT_DME2_PUBLISH_METRICS" );
    System.clearProperty( "org.eclipse.jetty.util.UrlEncoding.charset" );
    System.clearProperty( "metrics.debug" );
  }

  @Test
  @Ignore
  public void testRestfulService() throws Exception {
    DME2Manager mgr = null;
    DME2Manager mgr1 = null;

    String serviceName1 = "com.att.aft.dme2.test.TestRestfulService1";
    String serviceVersion1 = "1.0.0";
    String envContext1 = "LAB";
    String routeOffer1 = "TEST1";

    String serviceName2 = "com.att.aft.dme2.test.TestRestfulService1";
    String serviceVersion2 = "1.0.0";
    String envContext2 = "LAB";
    String routeOffer2 = "TEST2";

    String serviceURI1 =
        "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1?version=1.0.0&envContext=LAB&routeOffer=TEST1";
    String serviceURI2 =
        "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1?version=1.0.0&envContext=LAB&routeOffer=TEST2";

    String clientURI1 = "/restful/subcontext1?version=1.0.0&envContext=LAB&routeOffer=TEST1";
    String clientURI2 = "/restful/subcontext1?version=1.0.0&envContext=LAB&routeOffer=TEST2";

    String bindServiceURI1 = DME2Utils.buildServiceURIString( serviceName1, serviceVersion1, envContext1, routeOffer1 );
    String bindServiceURI2 = DME2Utils.buildServiceURIString( serviceName2, serviceVersion2, envContext2, routeOffer2 );

    String restfulServiceURI1 =
        "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1/{id}?envContext=LAB&version=1.0.0&routeOffer=TEST1";
    String restfulServiceURI2 =
        "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1/{id}?envContext=LAB&version=1.0.0&routeOffer=TEST2";

    int port1 = 32454;
    int port2 = 32456;

    String hostAddress = null;

    try {
      hostAddress = InetAddress.getLocalHost().getCanonicalHostName();
      // Save the route info
      RouteInfo rtInfo = new RouteInfo();
      rtInfo.setServiceName( "com.att.aft.dme2.test.TestRestfulService1" );
      rtInfo.setEnvContext( "LAB" );

      RouteGroups rtGrps = new RouteGroups();
      rtInfo.setRouteGroups( rtGrps );

      Properties props = new Properties();
      props.setProperty( "AFT_DME2_PORT", "" + port1 );
      props.setProperty( "DME2_GRM_AUTH", "true" );

      DME2Configuration config = new DME2Configuration( "com.att.aft.dme2.test.TestRestfulService1", props );
      RegistryFsSetup grmInit = new RegistryFsSetup();
      grmInit.init();
//      grmInit.saveRouteInfoForRestfulROFailover( config, rtInfo, "LAB" );


      mgr = new DME2Manager( "com.att.aft.dme2.test.TestRestfulService1", config );

      String pattern[] = { "/test", "/restful/subcontext1/*" };
      DME2ServletHolder servletHolder =
          new DME2ServletHolder( new EchoResponseServlet( bindServiceURI1, "testID1" ), pattern );

      List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
      servletHolderList.add( servletHolder );

      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( bindServiceURI1 );
      svcHolder.setManager( mgr );
      svcHolder.setServletHolders( servletHolderList );
      //svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
      //svcHolder.setContext("/test");

      DME2TestContextListener contextListener = new DME2TestContextListener();

      ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
      contextList.add( contextListener );
      svcHolder.setContextListeners( contextList );

      Properties props1 = new Properties();
      props1.setProperty( "AFT_DME2_PORT", "" + port2 );

      DME2Configuration config1 = new DME2Configuration( "com.att.aft.dme2.test.TestRestfulService2", props1 );

      mgr1 = new DME2Manager( "com.att.aft.dme2.test.TestRestfulService2", config1 );

      String pattern1[] = { "/test", "/restful/subcontext1/*" };
      DME2ServletHolder servletHolder1 =
          new DME2ServletHolder( new EchoResponseServlet( bindServiceURI2, "testID2" ), pattern1 );

      List<DME2ServletHolder> servletHolderList1 = new ArrayList<DME2ServletHolder>();
      servletHolderList1.add( servletHolder1 );

      DME2ServiceHolder svcHolder1 = new DME2ServiceHolder();
      svcHolder1.setServiceURI( bindServiceURI2 );
      svcHolder1.setManager( mgr1 );
      svcHolder1.setServletHolders( servletHolderList1 );
      //svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
      //svcHolder.setContext("/test");

      DME2TestContextListener contextListener1 = new DME2TestContextListener();

      ArrayList<ServletContextListener> contextList1 = new ArrayList<ServletContextListener>();
      contextList1.add( contextListener1 );
      svcHolder1.setContextListeners( contextList1 );


      mgr.getServer().start();
      mgr.bindService( svcHolder );

      mgr1.getServer().start();
      mgr1.bindService( svcHolder1 );
      Thread.sleep( 5000 );

      List<DME2Endpoint> endpoints =
          mgr.getEndpointRegistry().findEndpoints( serviceName1, serviceVersion1, envContext1, routeOffer1 );
      System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
      // CJR - 1/21/16 - Check only for endpoints from this host
      int endpointCount = 0;
      for ( DME2Endpoint ep : endpoints ) {
        System.out.println( " ENDPOINT RETURNED: " + ep.getServiceName() + " Host: " + ep.getHost() + " Port: " + ep.getPort() );
        if ( hostAddress.equals( ep.getHost() )) {
          endpointCount++;
        }
      }
      assertEquals( 1, endpointCount );
      System.out.println( endpoints.get( 0 ).toURLString() );
      // TODO: Remove sleep below
      //Thread.sleep(25000);

      // Test DIRECT URI for restful URI
      String clientURI = String.format( "http://%s:%s%s", hostAddress, "" + port1, clientURI1 );
      //String clientURI = "http://DME2RESOLVE" + serviceURI;
      System.out.println( "ClientURI=" + clientURI );
      Request request =
          new HttpRequest.RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );
      //client.setContext("/test");
      //client.setSubContext("/test");
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);

      String reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID1:::/service=com.att.aft.dme2.test.TestRestfulService1/version=1.0.0/envContext=LAB/routeOffer=TEST1;Request=THIS IS A TEST" ) );

      // Register using restful style URI
      //String serviceURI1 = "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1?version=1.0.0&envContext=LAB&routeOffer=TEST1";
      //String serviceURI2 = "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1?version=1.0.0&envContext=LAB&routeOffer=TEST2";
      //String restfulServiceURI1 = "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1/{id}?envContext=LAB&version=1.0.0&routeOffer=TEST1";
      //String restfulServiceURI2 = "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1/{id}?envContext=LAB&version=1.0.0&routeOffer=TEST2";
      DME2Manager.getDefaultInstance().getEndpointRegistry().publish( restfulServiceURI1, null, hostAddress, port1, "http", null );
      DME2Manager.getDefaultInstance().getEndpointRegistry().publish( restfulServiceURI2, null, hostAddress, port2, "http", null );

      Thread.sleep( 3000 );

      // Test RESOLVE URI for restful URI
      String restClientURI1 =
          "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestRestfulService1/restful/subcontext1/abc/version=1/envContext=LAB/routeOffer=TEST1";
      //String clientURI = "http://DME2RESOLVE" + serviceURI;
      System.out.println( "ClientURI=" + restClientURI1 );

      request =
          new HttpRequest.RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI1 ).build();


      client = new DME2Client( mgr, request );
      //client.setContext("/test");
      //client.setSubContext("/test");
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);

      reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID1:::/service=com.att.aft.dme2.test.TestRestfulService1/version=1.0.0/envContext=LAB/routeOffer=TEST1;Request=THIS IS A TEST" ) );

      // Test SEARCH URI for restful URI
      String restClientURI2 =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestRestfulService1/restful/subcontext1/abc/version=1/envContext=LAB/partner=test1";
      //String clientURI = "http://DME2RESOLVE" + serviceURI;
      System.out.println( "ClientURI=" + restClientURI2 );
      request =
          new HttpRequest.RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI2 ).build();


      client = new DME2Client( mgr, request );
      //client.setContext("/test");
      //client.setSubContext("/test");
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);

      reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID1:::/service=com.att.aft.dme2.test.TestRestfulService1/version=1.0.0/envContext=LAB/routeOffer=TEST1;Request=THIS IS A TEST" ) );


      // Test STANDARD URI for restful URI with partner
      String restClientURI3 =
          "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1/abc?version=1&envContext=LAB&partner=test1";
      //String clientURI = "http://DME2RESOLVE" + serviceURI;
      System.out.println( "ClientURI=" + restClientURI3 );
      request =
          new HttpRequest.RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI3 ).withContext( "/test" ).withSubContext( "/test" ).build();

      client = new DME2Client( mgr, request );
      //client.setContext("/test");
      //client.setSubContext("/test");
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);

      reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID1:::/service=com.att.aft.dme2.test.TestRestfulService1/version=1.0.0/envContext=LAB/routeOffer=TEST1;Request=THIS IS A TEST" ) );

      // Test STANDARD URI for restful URI with different partner - name SET and reply payload should be different
      String restClientURI4 =
          "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1/abc?version=1&envContext=LAB&partner=SET";
      //String clientURI = "http://DME2RESOLVE" + serviceURI;
      System.out.println( "ClientURI=" + restClientURI4 );
      request =
          new HttpRequest.RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI4 ).build();

      client = new DME2Client( mgr, request );
      //client.setContext("/test");
      //client.setSubContext("/test");
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);

      reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID2:::/service=com.att.aft.dme2.test.TestRestfulService1/version=1.0.0/envContext=LAB/routeOffer=TEST2;Request=THIS IS A TEST" ) );

      // Test STANDARD URI for restful URI with context path not matching
      String restClientURI5 =
          "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext?version=1&envContext=LAB&partner=test1";
      //String clientURI = "http://DME2RESOLVE" + serviceURI;
      System.out.println( "ClientURI=" + restClientURI5 );
      request =
          new HttpRequest.RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI5 ).build();

      client = new DME2Client( mgr, request );
      //client.setContext("/test");
      //client.setSubContext("/test");
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);
      try {
        reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
        System.out.println( "Response returned from the service: " + reply );
        assertTrue( reply == null );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue(
            e.getMessage().contains( "routeOffersTried=TEST1,TEST2" ) && e.getMessage().contains( "AFT-DME2-0702" ) );
      }


      try {
        mgr.getServer().stop();
        mgr.unbindServiceListener( restfulServiceURI1 );
        DME2Manager.getDefaultInstance().getEndpointRegistry().unpublish( restfulServiceURI1, hostAddress, port1 );
      } catch ( DME2Exception e ) {
        e.printStackTrace();
      }

      Thread.sleep(3000);
      // Test STANDARD URI for restful URI with partner to failover as primary endpoints should be shutdown now.
      String restClientURI6 = "http://TestRestfulService1.test.dme2.aft.att.com/restful/subcontext1/abc?version=1&envContext=LAB&partner=test1";
      //String clientURI = "http://DME2RESOLVE" + serviceURI;
      System.out.println("ClientURI=" + restClientURI6);
      client = new DME2Client(mgr1, new URI(restClientURI6), 300000);
      //client.setContext("/test");
      //client.setSubContext("/test");
      client.setPayload("THIS IS A TEST");
      client.setMethod("GET");
      client.setAllowAllHttpReturnCodes(true);

      reply = client.sendAndWait(300000);
      System.out.println("Response returned from the service: " + reply);
      assertTrue(reply.contains("EchoServlet:::testID2:::/service=com.att.aft.dme2.test.TestRestfulService1/version=1.0.0/envContext=LAB/routeOffer=TEST2;Request=THIS IS A TEST"));
    }  finally {
      if ( mgr != null ) {
        try {
          mgr.unbindServiceListener( serviceURI1 );
        } catch ( DME2Exception e ) {
        }
      }
      if ( mgr1 != null ) {
        try {
          mgr1.getServer().stop();
          mgr1.unbindServiceListener( serviceURI2 );
        } catch ( Exception e ) {
        }
      }

      if ( mgr != null ) {
        try {
          mgr.getEndpointRegistry().unpublish( restfulServiceURI1, hostAddress, port1 );
        } catch ( DME2Exception e ) {
        }

        try {
          mgr1.getEndpointRegistry().unpublish( restfulServiceURI2, hostAddress, port2 );
        } catch ( DME2Exception e ) {
        }
      }
    }
  }
}
