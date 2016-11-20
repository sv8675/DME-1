/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;

public class TestDME2PreferredVersion extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestDME2PreferredVersion.class );
  ArrayList<ServerControllerLauncher> serverLunchers;

  @Before
  public void setUp() {
    super.setUp();
    System.setProperty( "AFT_DME2_COLLECT_SERVICE_STATS", "false" );
  }

  @After
  public void tearDown() {
    System.clearProperty( "AFT_DME2_COLLECT_SERVICE_STATS" );
    System.clearProperty( "com.sun.management.jmxremote.authenticate" );
    System.clearProperty( "com.sun.management.jmxremote.ssl" );
    System.clearProperty( "com.sun.management.jmxremote.port" );
    super.tearDown();
  }

  /**
   * Test connects 3 servers, first test connects to first one having version 1.0.0
   * <p/>
   * then turn it off and make sure another one with same version is resolved
   * <p/>
   * then turn off that second server and make sure the server with wrong version is not resolved
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testCasePassOtherVersions() throws Exception {
    logger.debug( null, "testCasePassOtherVersions", "entering" );
    DME2Manager manager = null;
	System.setProperty("AFT_ENVIRONMENT", "AFTUAT");

    ServerControllerLauncher bham_1_Launcher = null;
    ServerControllerLauncher bham_2_Launcher = null;
    ServerControllerLauncher char_1_Launcher = null;

    String[] bham_1_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort", "4600",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion1/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
        "-serviceCity", "BHAM",
        "-serverid", "bham_1_bau_se",
        "-Dplatform=SANDBOX-DEV","-DAFT_ENVIRONMENT=AFTUAT" };

    String[] bham_2_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort","4601",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion1/version=2.0.0/envContext=DEV/routeOffer=BAU_SE",
        "-serviceCity", "BHAM",
        "-serverid", "bham_2_bau_se",
        "-Dplatform=SANDBOX-DEV","-DAFT_ENVIRONMENT=AFTUAT" };

    String[] char_1_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort", "4602",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion1/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
        "-serviceCity", "CHAR",
        "-serverid", "char_1_bau_se",
        "-Dplatform=SANDBOX-DEV" , "-DAFT_ENVIRONMENT=AFTUAT" };

    try {

      System.setProperty( "DME2_EP_TTL_MS", "300000" );
      System.setProperty( "DME2_RT_TTL_MS", "300000" );
      System.setProperty( "DME2_LEASE_REG_MS", "300000" );
      System.setProperty( "platform", "SANDBOX-DEV" );

      Properties props = RegistryFsSetup.init();
      props.setProperty( "SCLD_PLATFORM", "SANDBOX-DEV" );

      manager = new DME2Manager( "TestDME2ExchangePreferredVersion", props );

      RouteInfo rtInfo = createRouteInfoForPreferredVersion( "com.att.aft.dme2.TestDME2ExchangePreferredVersion1" );

      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      char_1_Launcher = new ServerControllerLauncher( char_1_bau_se_args );

      bham_1_Launcher.launch();
      bham_2_Launcher.launch();
      char_1_Launcher.launch();

      Thread.sleep( 5000 );

      // Save the route info
      RegistryFsSetup grmInit = new RegistryFsSetup();
 //     grmInit.saveRouteInfoInGRM( manager.getConfig(), rtInfo, "DEV" );

      // try to call a service we just registered

      Locations.CHAR.set();

      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredVersion1/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      DME2Client sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.addHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
          PreferredVersionRequestHandler.class.getName() );
      sender.addHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredVersionReplyHandler.class.getName() );
      sender.setPayload( "this is a test" );
      sender.setReplyHandler( replyHandler );
      sender.send();
      String reply = replyHandler.getResponse( 60000 );
      System.out.println( "reply1 = " + reply );
      assertNotNull( "reply from server should not be null", reply );
      assertTrue( "first reply should be from version 1.0", reply.contains( "version=1.0.0" ) );

      // stop server that replied
      if ( reply.contains( "routeOffer=BAU_NE" ) ) {
        logger.debug( null, "testCasePassOtherVersions", "Destroying BAU_NE endpoint" );
        try {
          bham_1_Launcher.destroy();
        } catch ( Exception e ) {

        }
      } else if ( reply.contains( "routeOffer=BAU_NW" ) ) {
        logger.debug( null, "testCasePassOtherVersions", "Destroying BAU_NW endpoint" );
        try {
          char_1_Launcher.destroy();
        } catch ( Exception e ) {

        }
      }
      Thread.sleep( 5000 );

      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredVersion1/version=2.0.0/envContext=DEV/dataContext=205977/partner=test2";
      replyHandler = new EchoReplyHandler();
      sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.setPayload( "this is a test" );
      sender.addHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
          PreferredVersionRequestHandler.class.getName() );
      sender.addHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredVersionReplyHandler.class.getName() );
      sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      sender.setReplyHandler( replyHandler );
      sender.send();
      reply = replyHandler.getResponse( 60000 );
      System.out.println( "reply2 = " + reply );
      assertNotNull( "reply from server should not be null", reply );
      assertTrue( "second reply should be from version 1.0, but was " + reply, reply.contains( "version=1.0.0" ) );

      // make sure it still connects to same server and handlers have not replaced the value with wrong version
      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredVersion1/version=2.0.0/envContext=DEV/dataContext=205977/partner=test2";
      replyHandler = new EchoReplyHandler();
      sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.setPayload( "this is a test" );
      sender.addHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
          PreferredVersionRequestHandler.class.getName() );
      sender.addHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredVersionReplyHandler.class.getName() );
      sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      sender.setReplyHandler( replyHandler );
      sender.send();
      reply = replyHandler.getResponse( 60000 );
      System.out.println( "reply3 = " + reply );
      assertNotNull( "reply from server should not be null", reply );
      assertTrue( "third reply should be from version 1.0", reply.contains( "version=1.0.0" ) );

      // stop the second server that replied, and make sure the one with wrong version is not resolved
      if ( reply.contains( "routeOffer=BAU_NE" ) ) {
        logger.debug( null, "testCasePassOtherVersions", "destroying BAU_NE endpoint" );
        try {
          bham_1_Launcher.destroy();
        } catch ( Exception e ) {

        }
      } else if ( reply.contains( "routeOffer=BAU_NW" ) ) {
        logger.debug( null, "testCasePassOtherVersions", "destroying BAU_NW endpoint" );
        try {
          char_1_Launcher.destroy();
        } catch ( Exception e  ) {

        }
      }
      Thread.sleep( 5000 );

      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredVersion1/version=2.0.0/envContext=DEV/dataContext=205977/partner=test2";
      replyHandler = new EchoReplyHandler();
      sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.setPayload( "this is a test" );
      sender.addHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
          PreferredVersionRequestHandler.class.getName() );
      sender.addHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredVersionReplyHandler.class.getName() );
      sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      sender.setReplyHandler( replyHandler );
      try {
        sender.send();
        reply = replyHandler.getResponse( 60000 );
        System.out.println( "reply4 = " + reply );
        fail( "should not found the server because version is different and preferred_version handler is used." );
      } catch ( Exception ex ) {
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      throw e;
    } finally {
      StaticCache.reset();

      System.clearProperty( "DME2_EP_TTL_MS" );
      System.clearProperty( "DME2_RT_TTL_MS" );
      System.clearProperty( "DME2_LEASE_REG_MS" );
      System.clearProperty( "platform" );

      try {
        bham_1_Launcher.destroy();
      } catch ( Exception e ) {
      }

      try {
        bham_2_Launcher.destroy();
      } catch ( Exception e ) {
      }

      try {
        char_1_Launcher.destroy();
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  @Ignore
  public void testCaseNullPreferredVersion() throws Exception {
    DME2Manager manager = null;

    ServerControllerLauncher bham_1_Launcher = null;
    ServerControllerLauncher bham_2_Launcher = null;
    ServerControllerLauncher char_1_Launcher = null;

    String[] bham_1_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort", "4600",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion2/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
        "-serviceCity", "BHAM",
        "-serverid", "bham_1_bau_se",
        "-platform", "SANDBOX-DEV" };

    String[] bham_2_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort","4601",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion2/version=2.0.0/envContext=DEV/routeOffer=BAU_SE",
        "-serviceCity", "BHAM",
        "-serverid", "bham_2_bau_se",
        "-platform", "SANDBOX-DEV" };

    String[] char_1_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort", "4602",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion2/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
        "-serviceCity", "CHAR",
        "-serverid", "char_1_bau_se",
        "-platform", "SANDBOX-DEV" };

    try {

      System.setProperty( "DME2_EP_TTL_MS", "300000" );
      System.setProperty( "DME2_RT_TTL_MS", "300000" );
      System.setProperty( "DME2_LEASE_REG_MS", "300000" );
      System.setProperty( "platform", "SANDBOX-DEV" );

      Properties props = RegistryFsSetup.init();
      props.setProperty( "SCLD_PLATFORM", "SANDBOX-DEV" );

      manager = new DME2Manager( "TestDME2ExchangePreferredVersion2", props );

      RouteInfo rtInfo = createRouteInfoForPreferredVersion( "com.att.aft.dme2.TestDME2ExchangePreferredVersion2" );

      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      char_1_Launcher = new ServerControllerLauncher( char_1_bau_se_args );

      bham_1_Launcher.launch();
      bham_2_Launcher.launch();
      char_1_Launcher.launch();

      Thread.sleep( 5000 );

      // Save the route info
      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoInGRM( manager.getConfig(), rtInfo, "DEV" );

      // try to call a service we just registered
      Locations.CHAR.set();

      // ******************************************************************************
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredVersion2/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      DME2Client sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.addHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
          PreferredVersionRequestHandler.class.getName() );
      sender.addHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredVersionReplyHandler.class.getName() );
      sender.setPayload( "this is a test" );
      sender.setReplyHandler( replyHandler );
      sender.send();
      String reply = replyHandler.getResponse( 60000 );
      System.out.println( reply );
      assertNotNull( "reply from server should not be null", reply );
      if ( !reply.contains( "version=1.0.0" ) ) {
        fail( "first reply should be from version 1.0" );
      }

      // ******************************************************************************
      // stop server that replied
      bham_1_Launcher.destroy();
      Thread.sleep( 5000 );
      StaticCache.getInstance().setPreferredVersion( null );

      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredVersion2/version=2.0.0/envContext=DEV/dataContext=205977/partner=test2";
      replyHandler = new EchoReplyHandler();
      sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.setPayload( "this is a test" );
      sender.addHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
          PreferredVersionRequestHandler.class.getName() );
      sender.addHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredVersionReplyHandler.class.getName() );
      sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      sender.setReplyHandler( replyHandler );
      sender.send();
      reply = replyHandler.getResponse( 60000 );
      System.out.println( "reply=" + reply );
      assertNotNull( "reply from server should not be null", reply );
      if ( !reply.contains( "version=2.0.0" ) ) {
        fail( "second reply should be from version 2.0.0" );
      }


    } finally {
      StaticCache.reset();

      System.clearProperty( "DME2_EP_TTL_MS" );
      System.clearProperty( "DME2_RT_TTL_MS" );
      System.clearProperty( "DME2_LEASE_REG_MS" );
      System.clearProperty( "platform" );

      try {
        bham_1_Launcher.destroy();
      } catch ( Exception e ) {
      }

      try {
        bham_2_Launcher.destroy();
      } catch ( Exception e ) {
      }

      try {
        char_1_Launcher.destroy();
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  @Ignore
  public void testFaultReplyHandler() throws Exception {
    DME2Manager manager = null;

    ServerControllerLauncher bham_1_Launcher = null;
    ServerControllerLauncher bham_2_Launcher = null;
    ServerControllerLauncher char_1_Launcher = null;

    String[] bham_1_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort", "4600",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion3/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
        "-serviceCity", "BHAM",
        "-serverid", "bham_1_bau_se",
        "-platform", "SANDBOX-DEV" };

    String[] bham_2_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort","4601",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion3/version=2.0.0/envContext=DEV/routeOffer=BAU_SE",
        "-serviceCity", "BHAM",
        "-serverid", "bham_2_bau_se",
        "-platform", "SANDBOX-DEV" };

    String[] char_1_bau_se_args = {
        "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
                /*"-serverPort", "4602",*/
        "-registryType", "GRM",
        "-servletClass", "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.TestDME2ExchangePreferredVersion3/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
        "-serviceCity", "CHAR",
        "-serverid", "char_1_bau_se",
        "-platform", "SANDBOX-DEV" };

    try {

      System.setProperty( "DME2_EP_TTL_MS", "300000" );
      System.setProperty( "DME2_RT_TTL_MS", "300000" );
      System.setProperty( "DME2_LEASE_REG_MS", "300000" );
      System.setProperty( "platform", "SANDBOX-DEV" );

      Properties props = RegistryFsSetup.init();
      props.setProperty( "SCLD_PLATFORM", "SANDBOX-DEV" );

      manager = new DME2Manager( "TestDME2ExchangePreferredVersion3", props );

      RouteInfo rtInfo = createRouteInfoForPreferredVersion( "com.att.aft.dme2.TestDME2ExchangePreferredVersion3" );

      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      char_1_Launcher = new ServerControllerLauncher( char_1_bau_se_args );

      bham_1_Launcher.launch();
      bham_2_Launcher.launch();
      char_1_Launcher.launch();

      Thread.sleep( 5000 );

      // Save the route info
      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoInGRM( manager.getConfig(), rtInfo, "DEV" );

      // try to call a service we just registered
      Locations.CHAR.set();

      // ******************************************************************************
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredVersion3/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      DME2Client sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.addHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
          PreferredVersionRequestHandler.class.getName() );
      sender.addHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS",
          PreferredVersionReplyHandler.class.getName() + "," + TestDME2FailoverFaultHandler.class.getName()  );
      sender.addHeader( "AFT_DME2_EP_READ_TIMEOUT_MS", "1000" );
      sender.addHeader( "echoSleepTimeMs", "30000" );
      sender.setPayload( "this is a test" );
      sender.setReplyHandler( replyHandler );
      sender.send();
      String reply = "";
      try {
        reply = replyHandler.getResponse( 60000 );
        System.out.println( "reply=" + reply );
        fail( "should not found the server because version is different and preferred_version handler is used." );
      } catch ( Exception ex ) {
        Boolean handleEndpointFailover = (Boolean) StaticCache.getInstance().get( "handleEndpointFailover" );
        DME2ExchangeFaultContext context =
            (DME2ExchangeFaultContext) StaticCache.getInstance().get( "DME2ExchangeFaultContext" );
        assertTrue( "handleEndpointFailover is not called", handleEndpointFailover );
        assertNotNull( "DME2ExchangeFaultContext is not set", context );
        String version = context.getVersion();
        assertEquals( "1.0.0", version );
      }


    } finally {
      StaticCache.reset();

      System.clearProperty( "DME2_EP_TTL_MS" );
      System.clearProperty( "DME2_RT_TTL_MS" );
      System.clearProperty( "DME2_LEASE_REG_MS" );
      System.clearProperty( "platform" );

      try {
        bham_1_Launcher.destroy();
      } catch ( Exception e ) {
      }

      try {
        bham_2_Launcher.destroy();
      } catch ( Exception e ) {
      }

      try {
        char_1_Launcher.destroy();
      } catch ( Exception e ) {
      }
    }
  }

  public static RouteInfo createRouteInfoForPreferredVersion( String serviceName ) {
    RouteInfo routeInfo = new RouteInfo();
    // "com.att.aft.dme2.TestDME2ExchangePreferredVersion"
    routeInfo.setServiceName( serviceName );
    //routeInfo.setServiceVersion("*");
    routeInfo.setEnvContext( "DEV" );

    RouteGroups routeGroups = new RouteGroups();
    routeInfo.setRouteGroups( routeGroups );

    RouteGroup routeGroup = new RouteGroup();
    routeGroup.setName( "RG1" );
    routeGroup.getPartner().add( "test1" );
    routeGroup.getPartner().add( "test2" );
    routeGroup.getPartner().add( "test3" );

    Route route = new Route();
    route.setName( "rt1" );

    RouteOffer routeOffer1 = new RouteOffer();
    routeOffer1.setActive( true );
    routeOffer1.setSequence( 1 );
    routeOffer1.setName( "BAU_NE" );

    Route route2 = new Route();
    route2.setName( "rt2" );

    RouteOffer routeOffer2 = new RouteOffer();
    routeOffer2.setActive( true );
    routeOffer2.setSequence( 1 );
    routeOffer2.setName( "BAU_SE" );

    Route route3 = new Route();
    route3.setName( "rt3" );

    RouteOffer routeOffer3 = new RouteOffer();
    routeOffer3.setActive( true );
    routeOffer3.setSequence( 1 );
    routeOffer3.setName( "BAU_NW" );

    route.getRouteOffer().add( routeOffer1 );
    route.getRouteOffer().add( routeOffer2 );
    route.getRouteOffer().add( routeOffer3 );

    routeGroup.getRoute().add( route );
    //routeGroup.getRoute().add(route2);
    //routeGroup.getRoute().add(route3);

    routeGroups.getRouteGroup();
    routeGroups.getRouteGroup().add( routeGroup );

    return routeInfo;
  }
}
