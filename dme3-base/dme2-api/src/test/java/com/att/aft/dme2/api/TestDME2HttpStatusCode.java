/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.net.URI;
import java.util.Properties;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.server.test.EchoServlet;
import com.att.aft.dme2.server.test.FailoverServlet;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.util.DME2Constants;

import junit.framework.TestCase;

public class TestDME2HttpStatusCode extends TestCase {

  @Before
  public void setUp() {
    RegistryFsSetup.cleanup();
    //super.setUp();
    System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON","true");
    System.setProperty("java.security.auth.login.config","src/test/etc/mylogin.conf");
    System.setProperty("org.eclipse.jetty.util.log.DEBUG","true");
    //System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestAuthentication");
    Locations.BHAM.set();
  }
  @After
  public void tearDown() throws Exception {
    RegistryFsSetup.cleanup();
    super.tearDown();
    System.clearProperty("java.security.auth.login.config");
    System.clearProperty("org.eclipse.jetty.util.log.DEBUG");

  }


  /*
   * (non-Javadoc)
   *
   * @see junit.framework.TestCase#setUp()
   */
/*
  @Override
  public void setUp() throws Exception {
    System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
    System.setProperty( "AFT_ENVIRONMENT", "DEV" );
    System.setProperty( "java.security.auth.login.config", "src/test/etc/mylogin.conf" );
    System.setProperty( "org.eclipse.jetty.util.log.DEBUG", "true" );
    //System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestAuthentication");
    Locations.BHAM.set();
  }
*/

  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test01_AuthFailureDefaultBehavior() throws Exception {
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    EchoServlet es = null, es1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      String allowedRoles[] = { "myclientrole" };
      String loginMethod = "BASIC";
      String realm = "myrealm";

      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode01", RegistryFsSetup.init() );
      manager = new DME2Manager( "TestDME2HttpStatusCode01", config );

      es = new EchoServlet( name, "bau_se_1" );
      manager.bindServiceListener( name, es, realm, allowedRoles, loginMethod );

      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51655" );

      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode011", props );
      manager1 = new DME2Manager( "TestDME2HttpStatusCode011", config1 );
      es1 = new EchoServlet( name, "bau_se_1" );
      manager1.bindServiceListener( name, es1, realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "abc" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      Thread.sleep(10000);
      String reply = replyHandler.getResponse( 60000 );
      assertNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      if ( es != null ) {
        es.destroy();
      }
      if ( es1 != null ) {
        es1.destroy();
      }
    }
    assertTrue( exceptionFound );
  }

  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test02_AuthFailure_NonFailover_MessageHeader() throws Exception {
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    EchoServlet es = null, es1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      String allowedRoles[] = { "myclientrole" };
      String loginMethod = "BASIC";
      String realm = "myrealm";

      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51257" );
      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode02", props );

      manager = new DME2Manager( "TestDME2HttpStatusCode02", config );

      es = new EchoServlet( name, "bau_se_1" );
      manager.bindServiceListener( name, es, realm, allowedRoles, loginMethod );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51256" );

      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode021", props1 );
      manager1 = new DME2Manager( "TestDME2HttpStatusCode021", config1 );

      es1 = new EchoServlet( name, "bau_se_1" );
      manager1.bindServiceListener( name, es1, realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "abc" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" )
          .withHeader( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "200" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      Thread.sleep(10000);
      String reply = replyHandler.getResponse( 120000 );

      assertNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( traceInfo.contains( "51257" ) && traceInfo.contains("51256"));
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      if ( es != null ) {
        es.destroy();
      }
      if ( es1 != null ) {
        es1.destroy();
      }
    }
    assertTrue( exceptionFound );
  }

  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test03_AuthFailure_NonFailover_QueryParam() throws Exception {
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    EchoServlet es = null, es1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      String allowedRoles[] = { "myclientrole" };
      String loginMethod = "BASIC";
      String realm = "myrealm";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51357" );
      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode03", props );

      manager = new DME2Manager( "TestDME2HttpStatusCode03", config );

      es = new EchoServlet( name, "bau_se_1" );
      manager.bindServiceListener( name, es, realm, allowedRoles, loginMethod );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51356" );

      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode031", props1 );

      manager1 = new DME2Manager( "TestDME2HttpStatusCode031", config1 );
      es1 = new EchoServlet( name, "bau_se_1" );
      manager1.bindServiceListener( name, es1, realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST?dme2NonFailoverStatusCodes=200";

      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "abc" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      String reply = replyHandler.getResponse( 60000 );
      String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
      assertNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( traceInfo.contains( "5135" ) ); //&& traceInfo.contains("51356"));
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      if ( es != null ) {
        es.destroy();
      }
      if ( es1 != null ) {
        es1.destroy();
      }
    }
    assertTrue( exceptionFound );
  }

  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test04_AuthFailure_NonFailover_ManagerParam() throws Exception {
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    EchoServlet es = null, es1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      String allowedRoles[] = { "myclientrole" };
      String loginMethod = "BASIC";
      String realm = "myrealm";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51457" );
      props.put( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "200" );

      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode04", props );
      manager = new DME2Manager( "TestDME2HttpStatusCode04", config );
      es = new EchoServlet( name, "bau_se_1" );
      manager.bindServiceListener( name, es, realm, allowedRoles, loginMethod );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51456" );
      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode041", props1 );

      manager1 = new DME2Manager( "TestDME2HttpStatusCode041", config1 );

      es1 = new EchoServlet( name, "bau_se_1" );
      manager1.bindServiceListener( name, es1, realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "abc" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      Thread.sleep(10000);
      String reply = replyHandler.getResponse( 60000 );
      assertNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( traceInfo.contains( "51457" ) && traceInfo.contains( "51456" ) );
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      if ( es != null ) {
        es.destroy();
      }
      if ( es1 != null ) {
        es1.destroy();
      }
    }
    assertTrue( exceptionFound );
  }

  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test05_ServerUnavailableFailure_NonFailover_ManagerParam() throws Exception {
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51447" );
      props.put( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "200,503" );

      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode05", props );
      manager = new DME2Manager( "TestDME2HttpStatusCode05", config );
      manager.bindServiceListener( name, new FailoverServlet( name, "bau_se_1" ), null, null, null );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51446" );

      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode051", props1 );
      manager1 = new DME2Manager( "TestDME2HttpStatusCode051", config1 );

      manager1.bindServiceListener( name, new FailoverServlet( name, "bau_se_1" ), null, null, null );

      // try to call a service we just registered
      String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
      Request request = new RequestBuilder( new URI( uriStr ) )
          .withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 ).withReturnResponseAsBytes( false )
          .withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      String reply = replyHandler.getResponse( 60000 );
      assertNotNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( ( traceInfo.contains( "51447" ) || traceInfo.contains( "51446" ) ) && traceInfo.contains( "onResponseCompleteStatus=503" ) );
      assertFalse( ( traceInfo.contains( "51447" ) && traceInfo.contains( "51446" ) ) && traceInfo.contains( "onResponseCompleteStatus=503" ) );
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
    }
    assertFalse( exceptionFound );
  }

  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test06_ServerUnavailableFailure_NonFailover_DisableSCLookup() throws Exception {
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    FailoverServlet fs = null, fs1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51547" );
      props.put( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "200,503" );
      props.put( "AFT_DME2_LOOKUP_NON_FAILOVER_SC", "true" );

      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode06", props );
      manager = new DME2Manager( "TestDME2HttpStatusCode06", config );

      fs = new FailoverServlet( name, "bau_se_1" );
      manager.bindServiceListener( name, fs, null, null, null );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51546" );

      fs1 = new FailoverServlet( name, "bau_se_1" );
      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode061", props1 );
      manager1 = new DME2Manager( "TestDME2HttpStatusCode061", config1 );

      manager1.bindServiceListener( name, fs1, null, null, null );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
      Request request = new RequestBuilder( new URI( uriStr ) )
          .withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 ).withReturnResponseAsBytes( false )
          .withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      //Thread.sleep(10000);
      String reply = replyHandler.getResponse( 60000 );
      assertNotNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( ( traceInfo.contains( "51546" ) ) && traceInfo.contains( "onResponseCompleteStatus=503" ) );
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      if ( fs != null ) {
        fs.destroy();
      }
      if ( fs1 != null ) {
        fs1.destroy();
      }
    }
    assertFalse( exceptionFound );
  }

  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test07_ServerAuthSuccessful_200Failover() throws Exception {
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    String allowedRoles[] = { "myclientrole" };
    String loginMethod = "BASIC";
    String realm = "myrealm";
    EchoServlet es = null, es1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51597" );
      props.put( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "302" );

      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode07", props );
      manager = new DME2Manager( "TestDME2HttpStatusCode07", config );
      manager.getServer().getServerProperties().setPort( 51597 );

      es = new EchoServlet( name, "bau_se_1" );
      manager.bindServiceListener( name, es, realm, allowedRoles, loginMethod );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51596" );
      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode071", props1 );
      manager1 = new DME2Manager( "TestDME2HttpStatusCode071", config1 );

      es1 = new EchoServlet( name, "bau_se_1" );
      manager1.bindServiceListener( name, es1, realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "test" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "this is a test" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      Thread.sleep(10000);
      System.out.println( "===================inside reply================" );
      String reply = replyHandler.getResponse( 60000 );
      assertNotNull( reply );
      System.out.println( "===================inside reply================" + reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      System.out.println( "===================inside Exception================" );
      //TODO response header is not populated
      String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( ( traceInfo.contains( "51597" ) || traceInfo.contains( "51596" ) ) && traceInfo.contains( "onResponseCompleteStatus=200" ) );
      exceptionFound = true;
      assertTrue( exceptionFound );
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      if ( es != null ) {
        es.destroy();
      }
      if ( es1 != null ) {
        es1.destroy();
      }
    }
    
  }


  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test08_ServerAuthSuccessful_200FailoverSC_IgnoreLookupSCOverride() throws Exception {
    System.setProperty( "AFT_DME2_LOOKUP_NON_FAILOVER_SC", "false" );
    DME2Constants.DME2_LOOKUP_NON_FAILOVER_SC = false;
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    String allowedRoles[] = { "myclientrole" };
    String loginMethod = "BASIC";
    String realm = "myrealm";
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51697" );
      props.put( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "302" );
      props.put( "AFT_DME2_LOOKUP_NON_FAILOVER_SC", "false" );

      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode08", props );
      manager = new DME2Manager( "TestDME2HttpStatusCode08", config );

      manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), realm, allowedRoles, loginMethod );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51696" );

      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode081", props1 );
      manager1 = new DME2Manager( "TestDME2HttpStatusCode081", config1 );
      manager1.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "test" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      Thread.sleep(10000);
      String reply = replyHandler.getResponse( 60000 );
      assertNotNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( ( traceInfo.contains( "51697" ) || traceInfo.contains( "51696" ) ) && traceInfo.contains( "onResponseCompleteStatus=200" ) );
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      System.clearProperty( "AFT_DME2_LOOKUP_NON_FAILOVER_SC" );
      DME2Constants.DME2_LOOKUP_NON_FAILOVER_SC = true;
    }
    assertFalse( exceptionFound );
  }


  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test09_ServerAuthFailure_NonFailover_DisableSCLookup() throws Exception {
    DME2Constants.DME2_LOOKUP_NON_FAILOVER_SC = false;
    System.setProperty( "AFT_DME2_LOOKUP_NON_FAILOVER_SC", "false" );
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    String allowedRoles[] = { "myclientrole" };
    String loginMethod = "BASIC";
    String realm = "myrealm";
    EchoServlet es = null, es1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51557" );
      props.put( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "302" );

      String managerName = RandomStringUtils.randomAlphanumeric( 20 );
      DME2Configuration config = new DME2Configuration( managerName, props );
      manager = new DME2Manager( managerName, config );
      manager.getServer().getServerProperties().setPort( 51557 );

      es = new EchoServlet( name, "bau_se_1");
          manager.bindServiceListener( name, es, realm, allowedRoles, loginMethod );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51556" );
      DME2Configuration config1 = new DME2Configuration( managerName + "1", props1 );
      manager1 = new DME2Manager( managerName + "1", config1 );
      es1 = new EchoServlet( name, "bau_se_1");
          manager1.bindServiceListener( name, es1, realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "abc" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      Thread.sleep(10000);
      String reply = replyHandler.getResponse( 60000 );
      assertNotNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue(e!=null);
      assertTrue(  (traceInfo.contains("51557") || traceInfo.contains("51556") ) && traceInfo.contains("onResponseCompleteStatus=401"));
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      System.clearProperty( "AFT_DME2_LOOKUP_NON_FAILOVER_SC" );
      DME2Constants.DME2_LOOKUP_NON_FAILOVER_SC = true;
      if ( es != null ) {
        es.destroy();
      }
      if ( es1 != null ) {
        es1.destroy();
      }
    }
    assertTrue( exceptionFound );
  }


  /**
   * Test client authentication.
   *
   * @throws Exception the exception
   */
  public void test10_AuthFailure_NonFailover_JVMParam() throws Exception {
    System.setProperty( "AFT_DME2_NON_FAILOVER_HTTP_SCS", "200" );
    boolean exceptionFound = false;
    DME2Manager manager = null;
    DME2Manager manager1 = null;
    com.att.aft.dme2.test.EchoReplyHandler replyHandler = new com.att.aft.dme2.test.EchoReplyHandler();
    EchoServlet es = null, es1 = null;
    try {
      String name = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
      String allowedRoles[] = { "myclientrole" };
      String loginMethod = "BASIC";
      String realm = "myrealm";
      Properties props = RegistryFsSetup.init();
      props.put( "AFT_DME2_PORT", "51567" );

      DME2Configuration config = new DME2Configuration( "TestDME2HttpStatusCode010", props );
      manager = new DME2Manager( "TestDME2HttpStatusCode010", config );

      es = new EchoServlet( name, "bau_se_1" );
      manager.bindServiceListener( name, es, realm, allowedRoles, loginMethod );

      Properties props1 = RegistryFsSetup.init();
      props1.put( "AFT_DME2_PORT", "51568" );

      DME2Configuration config1 = new DME2Configuration( "TestDME2HttpStatusCode0101", props1 );
      manager1 = new DME2Manager( "TestDME2HttpStatusCode0101", config1 );
      es1 = new EchoServlet( name, "bau_se_1" );
      manager1.bindServiceListener( name, es1, realm, allowedRoles, loginMethod );

      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
      Request request = new RequestBuilder( new URI( uriStr ) )
          .withAuthCreds( realm, "test", "abc" ).withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withReadTimeout( 20000 )
          .withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
      DME2Client sender = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      Thread.sleep(10000);
      String reply = replyHandler.getResponse( 60000 );
      assertNull( reply );
    } catch ( Exception e ) {
      e.printStackTrace();
      System.out.println( "replyHandler.getResponseHeaders()=" + replyHandler.getResponseHeaders() );
      String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO" );
      System.out.println( "traceInfo=" + traceInfo );
      assertTrue( e != null );
      assertTrue( traceInfo.contains( "51567" ) || traceInfo.contains( "51568" ) );
      exceptionFound = true;
    } finally {
      try {
        manager.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      try {
        manager1.unbindServiceListener( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
      } catch ( Exception e ) {
      }
      if ( es != null ) {
        es.destroy();
      }
      if ( es1 != null ) {
        es1.destroy();
      }
      System.clearProperty( "AFT_DME2_NON_FAILOVER_HTTP_SCS" );
    }
    assertTrue( exceptionFound );

  }
}