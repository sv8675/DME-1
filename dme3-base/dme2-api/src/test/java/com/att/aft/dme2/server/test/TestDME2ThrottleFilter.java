/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.handler.DME2RestfulHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.util.DME2Constants;

@net.jcip.annotations.NotThreadSafe

public class TestDME2ThrottleFilter extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestDME2ThrottleFilter.class );
  private static final Long WAIT_LONG_TIME_MILLIS = 60000l;
  private static final Long WAIT_SHORT_TIME_MILLIS = 5000l;
  private static final int MAX_THREAD_POOL_SIZE = 100;
  private static final double PARTNER_THROTTLE_10_PCT = 10.0;
  private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE =
      (int) Math.ceil( MAX_THREAD_POOL_SIZE * ( PARTNER_THROTTLE_10_PCT / 100.0 ) );
  private static final double PARTNER_THROTTLE_20_PCT = 20.0;
  private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE =
      (int) Math.ceil( MAX_THREAD_POOL_SIZE * ( PARTNER_THROTTLE_20_PCT / 100.0 ) );
  private static final double PARTNER_THROTTLE_90_PCT = 90.0;
  private static final double PARTNER_THROTTLE_100_PCT = 100.0;
  private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_90_PCT_THROTTLE = (int) Math.ceil(MAX_THREAD_POOL_SIZE * (PARTNER_THROTTLE_90_PCT / 100.0));
  private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_100_PCT_THROTTLE = (int) Math.ceil(MAX_THREAD_POOL_SIZE * (PARTNER_THROTTLE_100_PCT / 100.0));

  private static final String REGULAR_PARTNER_NAME = "REGULAR_PARTNER_NAME";
  private static final String PARTNER_NAME_TO_THROTTLE = "PTE";
  private static final String PARTNER_NAME_TO_THROTTLE_2 = "PTE2";

  @Before
  public void setUp() {
    try {
      RegistryFsSetup.cleanup();
    } catch ( Exception e ) {

    }
    super.setUp();
  }

  @After
  public void tearDown() {
    try {
      RegistryFsSetup.cleanup();
    } catch ( Exception e ) {

    }
    super.tearDown();
  }

  @Ignore
  @Test
  public void limitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable() throws Exception {
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter7/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    DME2RestfulHandler replyHandler = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18703",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter7/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
          "-DAFT_DME2_DISABLE_THROTTLE_FILTER=false",
          "-DAFT_DME2_THROTTLE_PCT_PER_PARTNER=" + PARTNER_THROTTLE_10_PCT };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      System.out
          .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 5000l );
      //send one more request that should be over the max active limit
      try {
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE,manager, uriWithPartnerToThrottle, null );
        replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  @Ignore
  public void disablesThrottleFilterUsingServiceUriParameter() throws Exception {
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter6/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18704",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter6/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttleFilterDisabled=true",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );

      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }

      //send one more request that should be over the max active limit
      DME2RestfulHandler replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle );

      DME2RestfulHandler.ResponseInfo responseInfo = replyHandler.getResponse( 10000 );
      assertEquals( "Was expecting a successful 200!", DME2Constants.DME2_RESPONSE_STATUS_200,
          responseInfo.getCode().intValue() );
    } finally {
      try {

        Thread.sleep( WAIT_SHORT_TIME_MILLIS );
        bham_1_Launcher.destroy();

      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_SHORT_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  @Ignore
  public void limitsAPartnerUponMaxActiveRequestsButAllowsOthers() throws Exception {
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter5/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18705",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter5/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttlePctPerPartner=" +
              PARTNER_THROTTLE_10_PCT,
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-DAFT_DME2_DISABLE_THROTTLE_FILTER=false",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      System.out.println( "******TEST CASE :: testLimitsAPartnerUponMaxActiveRequestsButAllowsOthers******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest( i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 3000 );
      //send one more request that should be over the max active limit
      try {
        DME2RestfulHandler replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE,manager, uriWithPartnerToThrottle );
        replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception and not reach this line" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
        System.out.println( "******** Got 429 as expected as partner " + PARTNER_NAME_TO_THROTTLE +
            " has reached throttle limit**********" );
      }

      String uriWithOtherPartner = MYSERVICE_URI + "/partner=" + REGULAR_PARTNER_NAME;
      //send another request from a different partner
      System.out.println( "Sending another request as a different partner " + REGULAR_PARTNER_NAME + " and uri " +
          uriWithOtherPartner );

      DME2RestfulHandler regularPartnerReplyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE+1,manager, uriWithOtherPartner, null );
      DME2RestfulHandler.ResponseInfo responseInfo = regularPartnerReplyHandler.getResponse( 3000 );
      assertEquals( "Was expecting a successful 200!", DME2Constants.DME2_RESPONSE_STATUS_200,
          responseInfo.getCode().intValue() );
      System.out.println( "********Got " + responseInfo.getCode().intValue() + " as expected for another partner " +
          REGULAR_PARTNER_NAME + " as it allows this partner request to go through**********" );
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_SHORT_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( 15000 );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  @Ignore
  public void allowsAPartnerWhenActiveRequestsGoesBelowMaxAllowed() throws Exception {
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter4/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    DME2RestfulHandler replyHandler = null;
    DME2RestfulHandler.ResponseInfo responseInfo = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18701",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter4/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttlePctPerPartner=" +
              PARTNER_THROTTLE_10_PCT,
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-DAFT_DME2_DISABLE_THROTTLE_FILTER=false",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 60000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );

      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 1; i <= MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 5000 );
      //send one more request that should be over the max active limit
      try {
        replyHandler = sendARequest(MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle );
        replyHandler.getResponse( 2000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }

      Thread.sleep( 120000 );
      //send some requests and eventually one of them will get through
      replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE+1,manager, uriWithPartnerToThrottle, null );
      responseInfo = replyHandler.getResponse( 3000 );
      assertEquals( "Was expecting a successful 200!", DME2Constants.DME2_RESPONSE_STATUS_200,
          responseInfo.getCode().intValue() );
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_SHORT_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_SHORT_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testSamePartnerWithMultipleServicesWithDiffThrottlePercentsIsThrottled() throws Exception {
    DME2Manager manager = null;
    DME2RestfulHandler replyHandler = null;
    DME2RestfulHandler.ResponseInfo responseInfo = null;
    try {
      System.setProperty( "platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
      System.setProperty( "DME2.DEBUG", "true" );
      System.setProperty( "lrmRName", "com.att.aft.dme2.api.TestDME2ThrottleFilter" );
      System.setProperty( "lrmRVer", "1.0.0" );
      System.setProperty( "lrmRO", "TESTRO" );
      System.setProperty( "lrmEnv", "DEV" );
      System.setProperty( "AFT_DME2_MAX_POOL_SIZE", String.valueOf( MAX_THREAD_POOL_SIZE ) );

      try {
        System.setProperty( "lrmHost", InetAddress.getLocalHost().getHostName() );
      } catch ( Exception e ) {
      }
      System.setProperty( "Pid", "1357" );

      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );

      String serviceNameFor10PctThrottle =
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter1/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttlePctPerPartner=" +
              PARTNER_THROTTLE_10_PCT + "&throttleFilterDisabled=false";
      manager.bindServiceListener( serviceNameFor10PctThrottle, new EchoServlet( "", "" ) );

      String serviceNameFor20PctThrottle =
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter2/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttlePctPerPartner=" +
              PARTNER_THROTTLE_20_PCT + "&throttleFilterDisabled=false";
      manager.bindServiceListener( serviceNameFor20PctThrottle, new EchoServlet( "", "" ) );
      System.out
          .println( "******TEST CASE :: testSamePartnerWithMultipleServicesWithDiffThrottlePercentsIsThrottled******" );

      // ** serviceNameFor10PctThrottle is Throttled 10% ** //
      String uriStrFor10PctThrottle =
          "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter1/version=1.0.0/envContext=PROD/dataContext=205977/partner=" +
              PARTNER_NAME_TO_THROTTLE;
      System.out.println( "********Sending 10% throttle = " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE +
          " requests to partner " + PARTNER_NAME_TO_THROTTLE + " and uri " + uriStrFor10PctThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER for service with 10% throttle that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest(i, manager, uriStrFor10PctThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 5000 );
      //verify we have max active limit on service with 10% throttle
      try {
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE,manager, uriStrFor10PctThrottle );
        responseInfo = replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
      //send a request for service with 20% throttle
      String uriStrFor20PctThrottle =
          "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter2/version=1.0.0/envContext=PROD/dataContext=205977/partner=" +
              PARTNER_NAME_TO_THROTTLE;
      //verify we can still access other services for the same partner
      replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE+1,manager, uriStrFor20PctThrottle );
      responseInfo = replyHandler.getResponse( 3000 );
      assertEquals( "Was expecting a 200 OK", DME2Constants.DME2_RESPONSE_STATUS_200,
          responseInfo.getCode().intValue() );

      // ** serviceNameFor20PctThrottle is Throttled 20% ** //
      //send MAX_ACTIVE_THREADS_PER_PARTNER for service with 20% throttle that will wait in EchoServlet's service method
      System.out.println( "********Sending 20% throttle =" + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE +
          " requests to partner " + PARTNER_NAME_TO_THROTTLE + " and uri " + uriStrFor20PctThrottle );
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++ ) {
        sendARequest( i,manager, uriStrFor20PctThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 5000 );
      //verify we have max active limit on service with 20% throttle
      try {
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE,manager, uriStrFor20PctThrottle );
        responseInfo = replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
    } finally {
      try {
        manager.shutdown();
        Thread.sleep( WAIT_SHORT_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  // fails and runs ok randomly
  @Test
  @Ignore  
  public void testThrottle90PercentAsJVMPropertyIsApplied() throws Exception {
    logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", LogMessage.METHOD_ENTER );
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18702",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
          "-DAFT_DME2_DISABLE_THROTTLE_FILTER=false",
          "-DAFT_DME2_THROTTLE_PCT_PER_PARTNER=" + PARTNER_THROTTLE_90_PCT };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      System.out
          .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "Sending request {}", i );
        sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      // CJR - 03/22/16 - Added sleep, since send that should fail was sometimes being received before previous sends.
      Thread.sleep( 10000 );
      //send one more request that should be over the max active limit
      try {
        logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "sending final request" );
        DME2RestfulHandler replyHandler = sendARequest(MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle, null );
        DME2RestfulHandler.ResponseInfo responseInfo = replyHandler.getResponse( 5000 );
        System.out.println( "********Got " + responseInfo.getCode().intValue() +
            " code and " + responseInfo.getBody() + " while partner has reached throttle limit**********" );
        logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "responseInfo {}", responseInfo.getBody() );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        logger.error( null, "testThrottlePercentAsJVMPropertyIsApplied", "Exception", e );
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }
  
//fails and runs ok randomly
@Ignore
  @Test
 public void testThrottle100PercentAsJVMPropertyIsApplied() throws Exception {
   logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", LogMessage.METHOD_ENTER );
   String MYSERVICE_URI =
       "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/dataContext=205977";

   DME2Manager manager = null;
   ServerControllerLauncher bham_1_Launcher = null;
   try {
     setupOtherSystemProperties();
     // run the server in bham.
     String[] bham_1_bau_se_args = {
         "-serverHost",
         "brcbsp01",
         "-serverPort",
         "18702",
         "-registryType",
         "FS",
         "-servletClass",
         "EchoServlet",
         "-serviceName",
         "service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
         "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
         "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
         "-DAFT_DME2_DISABLE_THROTTLE_FILTER=false",
         "-DAFT_DME2_THROTTLE_PCT_PER_PARTNER=" + PARTNER_THROTTLE_100_PCT };
     bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
     bham_1_Launcher.launch();

     Thread.sleep( 15000 );

     Locations.CHAR.set();
     String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
     manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
     System.out
         .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
     System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
         PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
     //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
     for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
       logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "Sending request {}", i );
       sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
     }
     // CJR - 03/22/16 - Added sleep, since send that should fail was sometimes being received before previous sends.
     Thread.sleep( 10000 );
     //send one more request that should be over the max active limit
     try {
       logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "sending final request" );
       DME2RestfulHandler replyHandler = sendARequest(MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle, null );
       DME2RestfulHandler.ResponseInfo responseInfo = replyHandler.getResponse( 5000 );
       System.out.println( "********Got " + responseInfo.getCode().intValue() +
           " code and " + responseInfo.getBody() + " while partner has reached throttle limit**********" );
       logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "responseInfo {}", responseInfo.getBody() );
       fail( "after maxing out threads should have tried failover and thrown an exception" );
     } catch ( DME2Exception e ) {
       logger.error( null, "testThrottlePercentAsJVMPropertyIsApplied", "Exception", e );
       assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
       assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
     }
   } finally {
     try {
       bham_1_Launcher.destroy();
       Thread.sleep( WAIT_LONG_TIME_MILLIS );
     } catch ( Exception e ) {
     }
     try {
       manager.shutdown();
       Thread.sleep( WAIT_LONG_TIME_MILLIS );
     } catch ( Exception e ) {
     }
   }
 }

	@Ignore
	@Test
  public void throttlePercentAsJVMPropertyIsApplied() throws Exception {
    logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", LogMessage.METHOD_ENTER );
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18702",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
          "-DAFT_DME2_DISABLE_THROTTLE_FILTER=false",
          "-DAFT_DME2_THROTTLE_PCT_PER_PARTNER=" + PARTNER_THROTTLE_10_PCT };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      System.out
          .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "Sending request {}", i );
        sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      // CJR - 03/22/16 - Added sleep, since send that should fail was sometimes being received before previous sends.
      Thread.sleep( 10000 );
      //send one more request that should be over the max active limit
      try {
        logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "sending final request" );
        DME2RestfulHandler replyHandler = sendARequest(MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle, null );
        DME2RestfulHandler.ResponseInfo responseInfo = replyHandler.getResponse( 5000 );
        System.out.println( "********Got " + responseInfo.getCode().intValue() +
            " code and " + responseInfo.getBody() + " while partner has reached throttle limit**********" );
        logger.debug( null, "testThrottlePercentAsJVMPropertyIsApplied", "responseInfo {}", responseInfo.getBody() );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        logger.error( null, "testThrottlePercentAsJVMPropertyIsApplied", "Exception", e );
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }
  
  private DME2RestfulHandler sendARequest( int requestNumber, DME2Manager manager, String uriStr, Long milliSecondsToWait )
      throws DME2Exception, URISyntaxException {
    DME2Client sender = new DME2Client( manager, new URI( uriStr ), 90000 );
    sender.setAllowAllHttpReturnCodes( true );
    HashMap<String, String> headers = new HashMap<String, String>();
    if ( milliSecondsToWait != null ) {
      headers.put( "echoSleepTimeMs", milliSecondsToWait.toString() );
    }
    headers.put( "testRequestNumber", String.valueOf( requestNumber ) );
    sender.setHeaders( headers );
    sender.setPayload( "Req " + requestNumber + " FROM THROTTLE after sleeping: " + milliSecondsToWait );
    DME2RestfulHandler replyHandler = new DME2RestfulHandler( uriStr );
    sender.setReplyHandler( replyHandler );
    sender.send();
    return replyHandler;
  }

  private DME2RestfulHandler sendARequest( int requestNumber, DME2Manager manager, String uriStr )
      throws DME2Exception, URISyntaxException {
    return sendARequest( requestNumber, manager, uriStr, null );
  }

  private void setupOtherSystemProperties() {
    System.setProperty( "platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
    System.setProperty( "DME2.DEBUG", "true" );
    System.setProperty( "lrmRName", "com.att.aft.dme2.api.TestDME2ThrottleFilter" );
    System.setProperty( "lrmRVer", "1.0.0" );
    System.setProperty( "lrmRO", "TESTRO" );
    System.setProperty( "lrmEnv", "DEV" );
    System.setProperty( "AFT_DME2_PUBLISH_METRICS", "false" );
    System.setProperty( "AFT_DME2_MAX_POOL_SIZE", String.valueOf( MAX_THREAD_POOL_SIZE ) );
    try {
      System.setProperty( "lrmHost", InetAddress.getLocalHost().getHostName() );
    } catch ( Exception e ) {
    }
    System.setProperty( "Pid", "1357" );
  }

  @Ignore
  @Test
  public void testLimitsPerPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable() throws Exception {
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter9/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    DME2RestfulHandler replyHandler = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-throttleDisabled",
          "false",
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18710",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter9/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-throttleConfig", "dme2-throttle-config.properties",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
      };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      System.out
          .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest( i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 5000l );
      //send one more request that should be over the max active limit
      try {
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle, null );
        replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  @Ignore
  public void limitsPerServiceForTwoPartnersViaPartnerConfig() throws Exception {

    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter10/version=1.0.0/envContext=PROD/dataContext=205977";
    System.setProperty( DME2Constants.Cache.DME2_DISABLE_PERSISTENT_CACHE, "true" );
    System.setProperty( DME2Constants.Cache.DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS, "0" );

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    DME2RestfulHandler replyHandler = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-throttleDisabled",
          "false",
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18701",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter10/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-throttleConfig", "dme2-throttle-config.properties",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
      };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      String uriWithPartnerToThrottle2 = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE_2;
      String uriWithDefPartnerToThrottle = MYSERVICE_URI + "/partner=" + REGULAR_PARTNER_NAME;

      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      System.out
          .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        logger.debug( null, "testLimitsPerServiceForTwoPartnersViaPartnerConfig", "Sending request {}", i );
        sendARequest( i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
        sendARequest( i, manager, uriWithPartnerToThrottle2, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 5000l );
      //send one more request that should be over the max active limit
      try {
        logger.debug( null, "testLimitsPerServiceForTwoPartnersViaPartnerConfig", "Sending final request to partner 1" );
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle, null );
        replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
      //send max requests for the second partner
      try {
        logger.debug( null, "testLimitsPerServiceForTwoPartnersViaPartnerConfig", "Sending final request to partner 2" );
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithPartnerToThrottle2, null );
        replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }

      //send a request for another partner which should succeed
      DME2RestfulHandler regularPartnerReplyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriWithDefPartnerToThrottle, null );
      DME2RestfulHandler.ResponseInfo responseInfo = regularPartnerReplyHandler.getResponse( 3000 );
      assertEquals( "Was expecting a successful 200!", DME2Constants.DME2_RESPONSE_STATUS_200,
          responseInfo.getCode().intValue() );

    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  @Ignore
  public void samePartnerWithMultipleServicesWithDiffThrottlePercentsViaPartnerConfig() throws Exception {
    DME2Manager manager = null;
    DME2RestfulHandler replyHandler = null;
    DME2RestfulHandler.ResponseInfo responseInfo = null;
    String serviceNameFor10PctThrottle = null;
    String serviceNameFor20PctThrottle = null;
    try {
      System.setProperty( "platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
      System.setProperty( "DME2.DEBUG", "true" );
      System.setProperty( "lrmRName", "com.att.aft.dme2.api.TestDME2ThrottleFilter" );
      System.setProperty( "lrmRVer", "1.0.0" );
      System.setProperty( "lrmRO", "TESTRO" );
      System.setProperty( "lrmEnv", "DEV" );
      System.setProperty( "AFT_DME2_MAX_POOL_SIZE", String.valueOf( MAX_THREAD_POOL_SIZE ) );
      System.setProperty( "AFT_DME2_THROTTLE_FILTER_CONFIG_FILE", "dme2-throttle-config.properties" );
      System.setProperty("AFT_DME2_DISABLE_THROTTLE_FILTER", "false");

      try {
        System.setProperty( "lrmHost", InetAddress.getLocalHost().getHostName() );
      } catch ( Exception e ) {
      }
      System.setProperty( "Pid", "1357" );

      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );

      serviceNameFor10PctThrottle =
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter11/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttleFilterDisabled=false";
      manager.bindServiceListener( serviceNameFor10PctThrottle, new EchoServlet( "", "" ) );

      serviceNameFor20PctThrottle =
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter12/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttleFilterDisabled=false";
      manager.bindServiceListener( serviceNameFor20PctThrottle, new EchoServlet( "", "" ) );

      Thread.sleep( 30000 );

      System.out
          .println( "******TEST CASE :: testSamePartnerWithMultipleServicesWithDiffThrottlePercentsIsThrottled******" );

      // ** serviceNameFor10PctThrottle is Throttled 10% ** //
      String uriStrFor10PctThrottle =
          "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter11/version=1.0.0/envContext=PROD/dataContext=205977/partner=" +
              PARTNER_NAME_TO_THROTTLE;
      System.out.println( "********Sending 10% throttle = " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE +
          " requests to partner " + PARTNER_NAME_TO_THROTTLE + " and uri " + uriStrFor10PctThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER for service with 10% throttle that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest( i, manager, uriStrFor10PctThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 15000 );
      //verify we have max active limit on service with 10% throttle
      try {
        System.out.println( "********Sending one more request to partner " + PARTNER_NAME_TO_THROTTLE + " and uri " +
            uriStrFor10PctThrottle );
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE, manager, uriStrFor10PctThrottle );
        responseInfo = replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
      //send a request for service with 20% throttle
      String uriStrFor20PctThrottle =
          "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter12/version=1.0.0/envContext=PROD/dataContext=205977/partner=" +
              PARTNER_NAME_TO_THROTTLE;
      //verify we can still access other services for the same partner
      replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE+1, manager, uriStrFor20PctThrottle );
      responseInfo = replyHandler.getResponse( 3000 );
      assertEquals( "Was expecting a 200 OK", DME2Constants.DME2_RESPONSE_STATUS_200,
          responseInfo.getCode().intValue() );

      // ** serviceNameFor20PctThrottle is Throttled 20% ** //
      //send MAX_ACTIVE_THREADS_PER_PARTNER for service with 20% throttle that will wait in EchoServlet's service method
      System.out.println( "********Sending 20% throttle =" + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE +
          " requests to partner " + PARTNER_NAME_TO_THROTTLE + " and uri " + uriStrFor20PctThrottle );
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++ ) {
        sendARequest( i, manager, uriStrFor20PctThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 5000 );
      //verify we have max active limit on service with 20% throttle
      try {
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE, manager, uriStrFor20PctThrottle );
        responseInfo = replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
    } finally {
      try {
        manager.unbindServiceListener( serviceNameFor10PctThrottle );
        manager.unbindServiceListener( serviceNameFor20PctThrottle );
        manager.shutdown();
        Thread.sleep( WAIT_SHORT_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testLimitsPerPartnerRequestCountWithWaitPeriod() throws Exception {
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter13/version=1.0.0/envContext=PROD/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    DME2RestfulHandler replyHandler = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18710",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter13/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-throttleConfig", "dme2-throttle-config.properties",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
      };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      Thread.sleep( 15000 );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      System.out
          .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      Thread.sleep( 60000l );
      //send one more request that should be over the max active limit
      try {
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE,manager, uriWithPartnerToThrottle, null );
        replyHandler.getResponse( 3000 );
        System.out.println(
            "after maxing out threads should have not failed over since the threads should have finished by now" );
      } catch ( DME2Exception e ) {
        fail( "No Exception should have been thrown" );
      }
    } catch ( Exception e ) {
      e.printStackTrace();
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testLimitsPerPartnerRequestViaJMX() throws Exception {
    logger.debug( null, "testLimitsPerPartnerRequestViaJMX", LogMessage.METHOD_ENTER );
/*
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter14/version=1.0.0/envContext=PROD/dataContext=205977";
*/
    String MYSERVICE_URI =
        "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter14/version=1.0.0/envContext=LAB/dataContext=205977";

    DME2Manager manager = null;
    ServerControllerLauncher bham_1_Launcher = null;
    DME2RestfulHandler replyHandler = null;
    try {
      setupOtherSystemProperties();
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          "brcbsp01",
          "-serverPort",
          "18710",
          "-registryType",
          "FS",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          //"service=com.att.aft.dme2.api.TestDME2ThrottleFilter14/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
          "service=com.att.aft.dme2.api.TestDME2ThrottleFilter14/version=1.0.0/envContext=LAB/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-DAFT_DME2_MAX_POOL_SIZE=" + MAX_THREAD_POOL_SIZE,
          "-Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=5000"
      };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      logger.debug( null, "testLimitsPerPartnerRequestViaJMX", "Launching controller" );
      bham_1_Launcher.launch();
      logger.debug( null, "testLimitsPerPartnerRequestViaJMX", "Sleeping for 15 seconds" );
      Thread.sleep( 15000 );
      manager = new DME2Manager( "RegistryFsSetup", RegistryFsSetup.init() );
      setThrottleConfig( "com.att.aft.dme2.api.TestDME2ThrottleFilter14", "PTE", "10" );

      Locations.CHAR.set();
      String uriWithPartnerToThrottle = MYSERVICE_URI + "/partner=" + PARTNER_NAME_TO_THROTTLE;

      System.out
          .println( "******TEST CASE :: testLimitsAPartnerRequestCountToGivenPercentOfMaxThreadPoolAvailable******" );
      System.out.println( "Sending " + MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE + " requests to partner " +
          PARTNER_NAME_TO_THROTTLE + " and uri " + uriWithPartnerToThrottle );
      //send MAX_ACTIVE_THREADS_PER_PARTNER that will wait in EchoServlet's service method
      for ( int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE; i++ ) {
        logger.debug( null, "testLimitsPerPartnerRequestViaJMX", "Sending request {}", i );
        sendARequest(i, manager, uriWithPartnerToThrottle, WAIT_LONG_TIME_MILLIS );
      }
      logger.debug( null, "testLimitsPerPartnerRequestViaJMX", "Sleeping for 5 seconds" );
      //Thread.sleep( 5000l );
      //send one more request that should be over the max active limit
      try {
        logger.debug( null, "testLimitsPerPartnerRequestViaJMX", "Sending final request" );
        replyHandler = sendARequest( MAX_ACTIVE_THREADS_PER_PARTNER_WITH_10_PCT_THROTTLE,manager, uriWithPartnerToThrottle, null );
        replyHandler.getResponse( 3000 );
        fail( "after maxing out threads should have tried failover and thrown an exception" );
      } catch ( DME2Exception e ) {
        logger.debug( null, "testLimitsPerPartnerRequestViaJMX", "Caught exception", e );
        assertEquals( "Expecting " + DME2Constants.DME2_ALL_EP_FAILED_MSGCODE + " got " + e.getErrorCode(), DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode() );
        assertTrue( e.getMessage().contains( "onResponseCompleteStatus=429" ) );
      }
    } catch ( Exception e ) {
      logger.error( null, "testLimitsPerPartnerRequestViaJMX", "Exception", e );
    } finally {
      try {
        bham_1_Launcher.destroy();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
      try {
        manager.shutdown();
        Thread.sleep( WAIT_LONG_TIME_MILLIS );
      } catch ( Exception e ) {
      }
    }
  }

  public void setThrottleConfig( String service, String partner, String throttlePct ) throws Exception {
    logger.debug( null, "setThrottleConfig", LogMessage.METHOD_ENTER );
    JMXConnector jmxc = null;
    try {
      // create JMX client
      JMXServiceURL url = new JMXServiceURL( "service:jmx:rmi:///jndi/rmi://:5000/jmxrmi" );
      jmxc = JMXConnectorFactory.connect( url, null );
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      ObjectName mbeanName =
          new ObjectName( "com.att.aft.dme2:type=ThrottleConfig,name=DME2ThrottleConfig-DefaultDME2Manager" );

      Object opParams[] = { service, partner, Float.valueOf( throttlePct ).floatValue() };

      String opSig[] = { String.class.getName(), String.class.getName(), float.class.getName() };

      //  Invoke operation
      mbsc.invoke( mbeanName, "setThrottleConfigForPartner", opParams, opSig );
      logger
          .debug( null, "setThrottleConfig", "Service: {} Partner: {} Throttlepct: {}", service, partner, throttlePct );
    } catch ( Exception e ) {
      logger.error( null, "setThrottleConfig", "Exception", e );
    } finally {
      jmxc.close();
    }
  }
}