/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;

@net.jcip.annotations.NotThreadSafe
public class TestDME2ExchangeFailoverRest extends DME2BaseTestCase {

  private final static String ENV = "DEV";

  private String[] serviceURI;
  private DME2Manager[] managers;
  private HttpServlet[] servlets;

  @Before
  public void setUp() {
    super.setUp();

    System.setProperty( "AFT_DME2_COLLECT_SERVICE_STATS", "false" );

    serviceURI = null;
    managers = null;
    servlets = null;
  }

  @After
  public void tearDown() {
    super.tearDown();
    System.clearProperty( "AFT_DME2_COLLECT_SERVICE_STATS" );
    System.clearProperty( "com.sun.management.jmxremote.authenticate" );
    System.clearProperty( "com.sun.management.jmxremote.ssl" );
    System.clearProperty( "com.sun.management.jmxremote.port" );

        /*
         * put the constant value back
         * 
         * this is a dangerous change and possible root cause of false unit test failure if some other test is running in parallel some server environments like jenkins do this,
         * this may cause conflict
         * 
         * a quick fix is to put all unit tests depending on this value should be put on one class that doesn't run parallel
         */

    // TODO: COMMENTED OUT IN MERGE - DO WE NEED THIS?
       /* boolean dme2LookUpNonFailoverSC = Boolean.parseBoolean( Configuration.getInstance().getProperty("AFT_DME2_LOOKUP_NON_FAILOVER_SC", DME2Constants.TRUE));
        DME2Constants.setDME2_LOOKUP_NON_FAILOVER_SC(dme2LookUpNonFailoverSC);*/

    // STOP or shutdown any active manager
    if ( managers != null ) {
      for ( DME2Manager manager : managers ) {
        try {
          manager.stop();
          // manager.shutdown();
        } catch ( Exception e ) {
        }
      }
    }
  }

  @Test
  @Ignore
  public void testAllFailOver() {
    final int SERVICES = 3;
    serviceURI = new String[SERVICES];
    servlets = new HttpServlet[SERVICES + 1];

    final String serviceName = "com.att.aft.dme2.api.test.ExchangeFailoverRest1";
    final String service = "/service=" + serviceName + "/version=1.0.0/envContext=" + ENV + "/";
    serviceURI[0] = service + "routeOffer=F01";
    serviceURI[1] = service + "routeOffer=F02";
    serviceURI[2] = service + "routeOffer=F03";
    final String searchUri = "http://DME2SEARCH" + service + "partner=test1";

    try {
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PARSE_FAULT", "true" );
      createManagers( SERVICES, props );

      servlets[0] = new FailoverRestServlet( serviceURI[0], "1", 503 );
      servlets[1] = new FailoverRestServlet( serviceURI[1], "2", 429 );
      servlets[2] = new FailoverRestServlet( serviceURI[2], "3", 503 );

      // Set Header for F2 on ServerSide, just for test not necessary
      FailoverRestServlet fs = (FailoverRestServlet) servlets[1];
      fs.setNfsc( "429" );
      bindServices();

      // create & save the route info
      RouteInfo rtInfo = createRouteInfo( serviceName );
      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoInGRM( managers[0].getConfig(), rtInfo, ENV );

      String reply = null;
      DME2Client client = new DME2Client( managers[SERVICES], new URI( searchUri ), 10000 );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setReplyHandler( replyHandler );
      client.setAllowAllHttpReturnCodes( true ); // Don't break on code!=200,!401
      client.setPayload( "Testing REST Failover" );
      client.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      client.send();

      try {
        reply = replyHandler.getResponse( 60000 );
        System.err.println( "reply=" + reply );
        fail( "should fail over all options" );
      } catch ( Exception e ) {
        String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
        assertTrue( traceInfo.contains( "routeOffer=F01:onResponseCompleteStatus=503" ) );
        assertTrue( traceInfo.contains( "routeOffer=F02:onResponseCompleteStatus=429" ) );
        assertTrue( traceInfo.contains( "routeOffer=F03:onResponseCompleteStatus=503" ) );
        e.printStackTrace();
        assertNotNull( e );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      // TODO: COMMENTED OUT IN MERGE - DO WE NEED THIS?
      //DME2Constants.setDME2_LOOKUP_NON_FAILOVER_SC(false);
      unbindServices();
    }
  }


  @Test
  @Ignore
  public void testOverrideByProperty() {
    final int SERVICES = 3;
    serviceURI = new String[SERVICES];
    servlets = new HttpServlet[SERVICES + 1];

    final String serviceName = "com.att.aft.dme2.api.test.ExchangeFailoverRest2";
    final String service = "/service=" + serviceName + "/version=1.0.0/envContext=" + ENV + "/";
    serviceURI[0] = service + "routeOffer=F01";
    serviceURI[1] = service + "routeOffer=F02";
    serviceURI[2] = service + "routeOffer=F03";
    final String searchUri = "http://DME2SEARCH" + service + "partner=test1";

    try {
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PARSE_FAULT", "true" );
      props.put( "AFT_DME2_NON_FAILOVER_HTTP_REST_SCS", "666,509" ); // Override fail over properties
      createManagers( SERVICES, props );

      servlets[0] = new FailoverRestServlet( serviceURI[0], "1", 308 ); // permanent redirect
      servlets[1] = new FailoverRestServlet( serviceURI[1], "2", 429 ); // too many request
      servlets[2] = new FailoverRestServlet( serviceURI[2], "3", 509 ); // Bandwidth Limit Exceeded

      bindServices();

      // create & save the route info
      RouteInfo rtInfo = createRouteInfo( serviceName );
      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoInGRM( managers[0].getConfig(), rtInfo, ENV );

      String reply = null;
      DME2Client client = new DME2Client( managers[SERVICES], new URI( searchUri ), 10000 );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setReplyHandler( replyHandler );
      client.setAllowAllHttpReturnCodes( true ); // Don't break on code!=200,!401
      client.setPayload( "Testing REST Failover" );
      client.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      client.send();

      try {
    	Thread.sleep(10000);
        reply = replyHandler.getResponse( 60000 );
        System.err.println( "reply=" + reply );
        String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println( traceInfo );
        assertTrue( traceInfo.contains( "routeOffer=F01:onResponseCompleteStatus=308" ) );
        assertTrue( traceInfo.contains( "routeOffer=F02:onResponseCompleteStatus=429" ) );
        assertTrue( traceInfo.contains( "routeOffer=F03:onResponseCompleteStatus=509" ) );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertNotNull( e );
        fail( "Should have matched with overriden status" );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      // TODO: COMMENTED OUT IN MERGE - DO WE NEED THIS?
      //DME2Constants.setDME2_LOOKUP_NON_FAILOVER_SC(false);
      unbindServices();
    }
  }

  @Test
  @Ignore
  public void testOverrideByClientHeader() {
    final int SERVICES = 3;
    serviceURI = new String[SERVICES];
    servlets = new HttpServlet[SERVICES + 1];

    final String serviceName = "com.att.aft.dme2.api.test.ExchangeFailoverRest3";
    final String service = "/service=" + serviceName + "/version=1.0.0/envContext=" + ENV + "/";
    serviceURI[0] = service + "routeOffer=F01";
    serviceURI[1] = service + "routeOffer=F02";
    serviceURI[2] = service + "routeOffer=F03";
    final String searchUri = "http://DME2SEARCH" + service + "partner=test1";

    try {
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PARSE_FAULT", "true" );
      createManagers( SERVICES, props );

      servlets[0] = new FailoverRestServlet( serviceURI[0], "1", 308 ); // permanent redirect
      servlets[1] = new FailoverRestServlet( serviceURI[1], "2", 429 ); // too many request
      servlets[2] = new FailoverRestServlet( serviceURI[2], "3", 509 ); // Bandwidth Limit Exceeded

      bindServices();

      // create & save the route info
      RouteInfo rtInfo = createRouteInfo( serviceName );
      RegistryFsSetup grmInit = new RegistryFsSetup();
 //     grmInit.saveRouteInfoInGRM( managers[0].getConfig(), rtInfo, ENV );

      String reply = null;
      DME2Client client = new DME2Client( managers[SERVICES], new URI( searchUri ), 10000 );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setReplyHandler( replyHandler );
      client.setAllowAllHttpReturnCodes( true ); // Don't break on code!=200,!401
      client.setPayload( "Testing REST Failover" );
      client.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      client
          .addHeader( "AFT_DME2_NON_FAILOVER_HTTP_REST_SCS", "200,501-509,620" ); // Allow Failover for 509 by client header
      client.send();

      try {
    	Thread.sleep(10000);
        reply = replyHandler.getResponse( 60000 );
        System.err.println( "reply=" + reply );
        String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println( "traceInfo============ " + traceInfo );
        assertTrue( traceInfo.contains( "routeOffer=F01:onResponseCompleteStatus=308" ) );
        assertTrue( traceInfo.contains( "routeOffer=F02:onResponseCompleteStatus=429" ) );
        assertTrue( traceInfo.contains( "routeOffer=F03:onResponseCompleteStatus=509" ) );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertNotNull( e );
        fail( "Should have matched with overriden status" );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      // TODO: COMMENTED OUT IN MERGE - DO WE NEED THIS?
      //DME2Constants.setDME2_LOOKUP_NON_FAILOVER_SC(false);
      unbindServices();
    }
  }

  @Test
  @Ignore
  public void testOverrideByQueryParam() {
    final int SERVICES = 3;
    serviceURI = new String[SERVICES];
    servlets = new HttpServlet[SERVICES + 1];

    final String serviceName = "com.att.aft.dme2.api.test.ExchangeFailoverRest4";
    final String service = "/service=" + serviceName + "/version=1.0.0/envContext=" + ENV + "/";
    serviceURI[0] = service + "routeOffer=F01";
    serviceURI[1] = service + "routeOffer=F02";
    serviceURI[2] = service + "routeOffer=F03";
    final String searchUri = "http://DME2SEARCH" + service + "/partner=test1" + "?dme2NonFailoverStatusCodes=509";

    try {
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PARSE_FAULT", "true" );
      createManagers( SERVICES, props );

      servlets[0] = new FailoverRestServlet( serviceURI[0], "1", 308 ); // Permanent redirect
      servlets[1] = new FailoverRestServlet( serviceURI[1], "2", 429 ); // Too many request
      servlets[2] = new FailoverRestServlet( serviceURI[2], "3", 509 ); // Bandwidth Limit Exceeded

      bindServices();

      // create & save the route info
      RouteInfo rtInfo = createRouteInfo( serviceName );
      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoInGRM( managers[0].getConfig(), rtInfo, ENV );

      Locations.CHAR.set();

      String reply = null;
      DME2Client client = new DME2Client( managers[SERVICES], new URI( searchUri ), 60000 );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setReplyHandler( replyHandler );
      client.setAllowAllHttpReturnCodes( true ); // Don't break on code!=200,!401
      client.setPayload( "Testing REST Failover" );
      client.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      client.send();

      try {
    	Thread.sleep(10000);
        reply = replyHandler.getResponse( 60000 );
        System.err.println( "reply=" + reply );
        String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println("traceInfo ========== " + traceInfo );
        assertTrue( traceInfo.contains( "routeOffer=F01:onResponseCompleteStatus=308" ) );
        assertTrue( traceInfo.contains( "routeOffer=F02:onResponseCompleteStatus=429" ) );
        assertTrue( traceInfo.contains( "routeOffer=F03:onResponseCompleteStatus=509" ) );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertNotNull( e );
        fail( "Should have matched with overriden status" );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      // TODO: COMMENTED OUT IN MERGE - DO WE NEED THIS?
      //DME2Constants.setDME2_LOOKUP_NON_FAILOVER_SC(false);
      unbindServices();
    }
  }

  @Test
  @Ignore
  public void test404isNonFailOver() {
    final int SERVICES = 3;
    serviceURI = new String[SERVICES];
    servlets = new HttpServlet[SERVICES + 1];

    final String serviceName = "com.att.aft.dme2.api.test.ExchangeFailoverRest5";
    final String service = "/service=" + serviceName + "/version=1.0.0/envContext=" + ENV + "/";
    serviceURI[0] = service + "routeOffer=F01";
    serviceURI[1] = service + "routeOffer=F02";
    serviceURI[2] = service + "routeOffer=F03";
    final String searchUri = "http://DME2SEARCH" + service + "/partner=test1";

    try {
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PARSE_FAULT", "true" );
      createManagers( SERVICES, props );

      servlets[0] = new FailoverRestServlet( serviceURI[0], "1", 429 );
      servlets[1] = new FailoverRestServlet( serviceURI[1], "2", 404 );
      servlets[2] = new FailoverRestServlet( serviceURI[2], "3", 503 );

      bindServices();

      // create & save the route info
      RouteInfo rtInfo = createRouteInfo( serviceName );
      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoInGRM( managers[0].getConfig(), rtInfo, ENV );

      Locations.CHAR.set();

      String reply = null;
      DME2Client client = new DME2Client( managers[SERVICES], new URI( searchUri ), 10000 );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setReplyHandler( replyHandler );
      client.setAllowAllHttpReturnCodes( true ); // Don't break on code!=200,!401
      client.setPayload( "Testing REST Failover" );
      client.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      client.send();

      try {
        reply = replyHandler.getResponse( 60000 );
        System.err.println( "reply=" + reply );
        String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println( traceInfo );
        assertTrue( traceInfo.contains( "routeOffer=F01:onResponseCompleteStatus=429" ) );
        assertTrue( traceInfo.contains( "routeOffer=F02:onResponseCompleteStatus=404" ) );
        assertTrue( "404 is non failover code 503 should never be tried",
            !traceInfo.contains( "routeOffer=F03:onResponseCompleteStatus=503" ) );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertNotNull( e );
        String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println( traceInfo );
        fail( "Test Failed because of above exception" );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      // TODO: COMMENTED OUT IN MERGE - DO WE NEED THIS?
      // DME2Constants.setDME2_LOOKUP_NON_FAILOVER_SC(false);
      unbindServices();
    }
  }

  // 2- Add Test Case to prove SOAP working same as before

  private static RouteInfo createRouteInfo( String serviceName ) {
    // routeInfo.setServiceVersion("*");
    RouteInfo routeInfo = new RouteInfo();
    routeInfo.setServiceName( serviceName );
    routeInfo.setEnvContext( ENV );

    RouteGroups routeGroups = new RouteGroups();
    routeInfo.setRouteGroups( routeGroups );

    RouteGroup routeGroup = new RouteGroup();
    routeGroup.setName( "FO" );
    routeGroup.getPartner().add( "test1" );
    routeGroup.getPartner().add( "test2" );
    routeGroup.getPartner().add( "test3" );
    routeGroups.getRouteGroup().add( routeGroup );

    Route route = new Route();
    route.setName( "rt_def" );
    routeGroup.getRoute().add( route );

    RouteOffer routeOffer1 = new RouteOffer();
    routeOffer1.setActive( true );
    routeOffer1.setSequence( 1 );
    routeOffer1.setName( "F01" );
    route.getRouteOffer().add( routeOffer1 );

    RouteOffer routeOffer2 = new RouteOffer();
    routeOffer2.setActive( true );
    routeOffer2.setSequence( 2 );
    routeOffer2.setName( "F02" );
    route.getRouteOffer().add( routeOffer2 );

    RouteOffer routeOffer3 = new RouteOffer();
    routeOffer3.setActive( true );
    routeOffer3.setSequence( 3 );
    routeOffer3.setName( "F03" );
    route.getRouteOffer().add( routeOffer3 );

    return routeInfo;
  }

  /**
   * These are utility methods that can be put in a common code like a parent but this make the test non thread safe
   * since they depend on object properties
   *
   * @throws InterruptedException
   */
  protected void bindServices() throws DME2Exception, InterruptedException {
    for ( int i = 0; i < serviceURI.length; i++ ) {
      managers[i].bindServiceListener( serviceURI[i], servlets[i] );
    }
    Thread.sleep( 3000 );
  }

  protected void unbindServices() {
    for ( int i = 0; i < serviceURI.length; i++ ) {
      try {
        managers[i].unbindServiceListener( serviceURI[i] );
      } catch ( Exception e ) {
      }
    }
  }

  protected void createManagers( int number, Properties props ) throws DME2Exception {
    managers = new DME2Manager[number + 1]; // create one more for client!
    for ( int i = 0; i <= number; i++ ) {
      managers[i] = new DME2Manager( "Manager" + i, props );
    }
  }
}

class FailoverRestServlet extends FailoverServlet {
  private static final long serialVersionUID = 6916571301213942821L;
  protected String nfsc;
  protected Integer errorCode;
  protected Integer statusCode;
  protected String content;

  public FailoverRestServlet( String service, String serverId, Integer statusCode, Integer errorCode ) {
    super( service, serverId );
    this.statusCode = statusCode;
    this.errorCode = errorCode;
    if ( serverId.startsWith( "ERRORCODE_" ) ) {
      String codeStr = serverId.substring( "ERRORCODE_".length(), serverId.length() );
      errorCode = Integer.valueOf( codeStr );
    } else if ( serverId.startsWith( "STATUS_" ) ) {
      String codeStr = serverId.substring( "STATUS_".length(), serverId.length() );
      statusCode = Integer.valueOf( codeStr );
    }

  }

  public FailoverRestServlet( String service, String serverId, Integer statusCode ) {
    this( service, serverId, statusCode, null );
  }

  public FailoverRestServlet( String service, String serverId ) {
    this( service, serverId, null, null );
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest , javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
    // set as a header so it can be checked on the response for testing purposes
    String charset = req.getCharacterEncoding();
    resp.setHeader( "com.att.aft.dme2.test.charset", charset );

    if ( nfsc != null ) {
      resp.setHeader( "AFT_DME2_NON_FAILOVER_HTTP_SCS", nfsc );
    }
    // resp.getWriter().println("FailoverRequired=true");
    if ( content != null ) {
      resp.getWriter().println( content );
    }
    if ( errorCode != null ) {
      resp.sendError( errorCode );
    } else if ( statusCode != null ) {
      resp.setStatus( statusCode );
    }

    resp.flushBuffer();
    return;
  }

  public void setErrorCode( Integer errorCode ) {
    this.errorCode = errorCode;
  }

  public void setStatusCode( Integer statusCode ) {
    this.statusCode = statusCode;
  }

  public void setContent( String content ) {
    this.content = content;
  }

  public void setNfsc( String nfsc ) {
    this.nfsc = nfsc;
  }

}
