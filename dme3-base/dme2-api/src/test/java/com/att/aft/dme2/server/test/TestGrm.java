/*
 * Copyright 2016 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;

/**
 * The Class TestFailover.
 */
@Ignore
public class TestGrm extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestGrm.class );

  /**
   * The bham_1_ launcher.
   */
  public ServerControllerLauncher bham_1_Launcher;

  /**
   * The bham_2_ launcher.
   */
  public ServerControllerLauncher bham_2_Launcher;

  /**
   * The bham_3_ launcher.
   */
  public ServerControllerLauncher bham_3_Launcher;

  /**
   * The char_1_ launcher.
   */
  public ServerControllerLauncher char_1_Launcher;

  @Before
  public void setUp() {
    super.setUp();
    System.setProperty( "AFT_LATITUDE", "33.373900" );
    System.setProperty( "AFT_LONGITUDE", "-86.798300" );
    System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
    System.setProperty( "SCLD_PLATFORM", TestConstants.GRM_PLATFORM_TO_USE );

    Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
    DME2Configuration config = new DME2Configuration( "TestGrm", props );
    try {
      DME2Manager mgr = new DME2Manager( "TestGrm", config );
    } catch ( DME2Exception e ) {
      throw new RuntimeException( e );
    }
    System.clearProperty( "AFT_DME2_GRM_URLS" );
  }

  @After
  public void tearDown() {
    if ( bham_1_Launcher != null ) {
      bham_1_Launcher.destroy();
    }

    if ( bham_2_Launcher != null ) {
      bham_2_Launcher.destroy();
    }

    if ( char_1_Launcher != null ) {
      char_1_Launcher.destroy();
    }
    System.clearProperty( "lrmRName" );
    System.clearProperty( "lrmRVer" );
    System.clearProperty( "lrmEnv" );
    System.clearProperty( "Pid" );
    System.clearProperty( "DME2_EP_ACCESSOR_CLASS" );
    super.tearDown();
  }

  /**
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmPublish() throws DME2Exception, InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmPublish" );
    String service = "com.att.aft.dme2.TestGrmPublish";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 3267;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName =
        "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

    Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}

    DME2Configuration config = new DME2Configuration( "testGrmPublish", props );

    DME2Manager manager = new DME2Manager( "testGrmPublish", config );

//		DME2Manager manager = new DME2Manager("testGrmPublish", new Properties());
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude, longitude, "http" );

    System.err.println( "Service published successfully." );

    try {

      Thread.sleep( 10000 );

      DME2Endpoint[] endpoints = svcRegistry.find( service, version, envContext, routeOffer );
      //System.out.println(endpoints[0].getServiceName());

      DME2Endpoint found = null;
      for ( DME2Endpoint ep : endpoints ) {
        if ( ep.getHost().equals( hostname ) && ep.getPort() == port
            && ep.getLatitude() == latitude
            && ep.getLongitude() == longitude ) {
          found = ep;
        }
      }
      System.err.println( "Found registered endpoint: " + found );
      assertNotNull( found );

      svcRegistry.unpublish( serviceName, hostname, port );
      svcRegistry.refresh();
      //Thread.sleep(10000);

      endpoints = svcRegistry.find( service, version, envContext, routeOffer );
      //System.out.println(endpoints[0].getServiceName());

      System.out.println( "endpoints: " + endpoints );
      if ( endpoints != null ) {
        for ( DME2Endpoint ep : endpoints ) {
          System.out.println( ep );

          if ( ep.getHost().equals( hostname ) && ep.getPort() == port
              && ep.getLatitude() == latitude
              && ep.getLongitude() == longitude ) {
            fail( "Found an endpoint after unpublishing the same." );
          }
        }
      }
    } finally {
      try {
        svcRegistry.unpublish( serviceName, hostname, port );
      } catch ( Exception e ) {
      }
    }
  }

  /**
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmRestfulPublish() throws DME2Exception, InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmRestfulPublish" );
    String service = "TestGrmRestfulPublish.dme2.aft.att.com";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 3267;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName =
        "http://" + service + "/test/restful?version=" + version + "&envContext=" + envContext + "&routeOffer=" +
            routeOffer;

    Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}

    DME2Configuration config = new DME2Configuration( "testGrmRestfulPublish", props );

    DME2Manager manager = new DME2Manager( "testGrmRestfulPublish", config );
//		DME2Manager manager = new DME2Manager("testGrmRestfulPublish", new Properties());
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude, longitude, "http" );

    System.err.println( "Service published successfully." );

    try {
      Thread.sleep( 10000 );

      DME2Endpoint[] endpoints =
          svcRegistry.find( "com.att.aft.dme2.TestGrmRestfulPublish*", version, envContext, routeOffer );

      DME2Endpoint found = null;
      for ( DME2Endpoint ep : endpoints ) {
        if ( ep.getHost().equals( hostname ) && ep.getPort() == port
            && ep.getLatitude() == latitude
            && ep.getLongitude() == longitude ) {
          found = ep;
        }
      }
      System.err.println( "Found registered endpoint: " + found );
      assertNotNull( found );
    } finally {
      try {
        svcRegistry.unpublish( serviceName, hostname, port );
      } catch ( Exception e ) {
      }
    }
  }

  /**
   * Test GRM Publish API with lat/long from DME2Manager properties
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmPublish_1() throws DME2Exception, InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmPublish_1" );
    String service = "com.att.aft.dme2.TestGrmPublish1";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 48978;
    double latitude = 33.3739;
    double longitude = -86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName =
        "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

    Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    props.put( "AFT_LATITUDE", latitude );
    props.put( "AFT_LONGITUDE", longitude );

    DME2Configuration config = new DME2Configuration( "testGrmPublish1", props );

    DME2Manager manager = new DME2Manager( "testGrmPublish1", config );

//		DME2Manager manager = new DME2Manager("testGrmPublish1", props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, "http", null );

    System.err.println( "Service published successfully." );

    Thread.sleep( 10000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version, envContext, routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getPort() == port
          && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
      }
    }
    System.err.println( "Found registered endpoint: " + found );
    assertNotNull( found );

    svcRegistry.unpublish( serviceName, hostname, port );
  }

  /**
   * GRM Publish with lat/long from system properties
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmPublish_2() throws DME2Exception, InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmPublish_2" );
    String service = "com.att.aft.dme2.TestGrmPublish2";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 48979;
    double latitude = 33.3739;
    double longitude = -86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName =
        "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

    Properties props = new Properties();
    System.setProperty( "AFT_LATITUDE", latitude + "" );
    System.setProperty( "AFT_LONGITUDE", longitude + "" );

    DME2Configuration config = new DME2Configuration( "testGrmPublish1", props );

    DME2Manager manager = new DME2Manager( "testGrmPublish1", config );

    //DME2Manager manager = new DME2Manager("testGrmPublish1", props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, "http", null );

    System.err.println( "Service published successfully." );

    Thread.sleep( 10000 );

    try {
      DME2Endpoint[] endpoints = svcRegistry.find( service, version, envContext, routeOffer );

      DME2Endpoint found = null;
      for ( DME2Endpoint ep : endpoints ) {
        if ( ep.getHost().equals( hostname ) && ep.getPort() == port
            && ep.getLatitude() == latitude
            && ep.getLongitude() == longitude ) {
          found = ep;
        }
      }
      System.err.println( "Found registered endpoint: " + found );
      assertNotNull( found );
    } finally {
      try {
        svcRegistry.unpublish( serviceName, hostname, port );
      } catch ( Exception e ) {
      }

      System.clearProperty( "AFT_LATITUDE" );
      System.clearProperty( "AFT_LONGITUDE" );
    }
  }

  /**
   * GRM Publish with lat/long from system properties
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmPublish_RetryAddServiceEndpoint() throws Exception {
    System.setProperty( "SCLD_PLATFORM", "SANDBOX-DEV" );
    System.out.println( "-----------------------------------------------------------" );
    System.out.println( "Starting testGrmPublish_RetryAddServiceEndpoint" );
    System.out.println( "-----------------------------------------------------------" );
    String service = "com.att.aft.dme2." + RandomStringUtils.randomAlphanumeric( 25 );
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 48979;
    double latitude = 33.3739;
    double longitude = -86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName =
        "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

    Properties props = new Properties();
    System.setProperty( "AFT_LATITUDE", latitude + "" );
    System.setProperty( "AFT_LONGITUDE", longitude + "" );
    //System.setProperty( "AFT_DME2_GRM_URLS", "http://lltd001.chdc.att.com:912/GRMLWPService/v1");
    System.setProperty( "AFT_DME2_GRM_URLS",
        "" );
    //System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE);
    System.setProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true" );
    props.setProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "15000" );
    System.setProperty( "DME2_SEP_CACHE_TTL_MS", "15000" );
    System.setProperty( "DME2.DEBUG", "true" );

    DME2Configuration config = new DME2Configuration( "RetryAddServiceEndpoint", props );
/*
    assertEquals( "http://zldv0432.vci.att.com:912/GRMLWPService/v1,http://zldv1330.vci.att.com:912/GRMLWPService/v1",
        config.getProperty( "AFT_DME2_GRM_URLS" ) );
*/
    // This isn't optimal, but ...
    //DME2UnitTestUtil.setFinalStatic( GRMAccessorFactory.class.getDeclaredField( "grmAccessorHandler" ), null, null );
    GRMAccessorFactory.getInstance().close();
    DME2Manager manager = new DME2Manager( "RetryAddServiceEndpoint", config );
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    boolean exceptionFound = false;
    try {
      svcRegistry.publish( serviceName, null, hostname, port, "http", null );
    } catch ( Exception e ) {
      // Publish expected to fail with wrong GRM URL's in use
      e.printStackTrace();
      exceptionFound = true;
    }
    assertTrue( exceptionFound );

     // This is unbelievably messy...
    Properties props2 = new Properties();
    props2.setProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE );
    props2.setProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true" );
    DME2Configuration config2 = new DME2Configuration( "RetryAddServiceEndpoint2Config", props2 );

    assertEquals( TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE, config2.getProperty( "AFT_DME2_GRM_URLS" ) );
//    DME2UnitTestUtil.setFinalStatic( GRMAccessorFactory.class.getDeclaredField( "grmAccessorHandler" ), null, null );
    GRMAccessorFactory.getInstance().close();
    DME2UnitTestUtil
        .setFinalStatic( DME2EndpointRegistryGRM.class.getDeclaredField( "grm" ), manager.getEndpointRegistry(),
            GRMAccessorFactory.getGrmAccessorHandlerInstance( config2,
                SecurityContext.create( config2 ) ) );

/*
    Properties props2 = new Properties();
    props2.setProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DIRECT_HTTP_URLS_TO_USE );
    props2.setProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true"  );
    DME2Configuration config2 = new DME2Configuration( "RetryAddServiceEndpoint2Config", props2 );
*/
    //assertEquals(props2.getProperty( "AFT_DME2_GRM_URLS" ), config2.getProperty( "AFT_DME2_GRM_URLS" ));
    //config2.setOverrideProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DIRECT_HTTP_URLS_TO_USE);
    //config2.setOverrideProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true" );
    //manager = new DME2Manager( "RetryAddServiceEndpoint2", config2 );

    List<DME2Endpoint> endpoints = manager.getEndpointRegistry().findEndpoints( service, version, envContext,
        routeOffer );
    assertTrue( endpoints.size() == 0 );

    // The idea here is that, even though the publish to GRM failed, the endpoints are stored in a list inside the
    // registry.  This list will be refreshed on a timer, which should push those endpoints to GRM automatically

    Thread.sleep( config.getInt( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" ) + 5000 );
    props.setProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE );
    DME2Configuration config1 = new DME2Configuration( "RetryAddServiceEndpoint1", props );
    DME2Manager manager1 = new DME2Manager( "RetryAddServiceEndpoint1", config1 );
    svcRegistry = manager1.getEndpointRegistry();
    endpoints = svcRegistry.findEndpoints( service, version, envContext, routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getPort() == port
          && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
      }
    }
    System.err.println( "Found registered endpoint: " + found );
    assertNotNull( "Supposed to have found an enpoint, but none was found", found );

    List<DME2Endpoint> endpointList =
        manager.getEndpointRegistry().findEndpoints( service, version, envContext, routeOffer );

    found = null;
    for ( DME2Endpoint ep : endpointList ) {
      if ( ep.getHost().equals( hostname ) && ep.getPort() == port
          && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
        System.out.println( "Found endpoints=" + found );
      }
    }
    System.err.println( "Found registered endpoint: " + found );
    assertNotNull( found );
    try {
      svcRegistry.unpublish( serviceName, hostname, port );
    } catch ( Exception e ) {

    } finally {
      System.clearProperty( "AFT_LATITUDE" );
      System.clearProperty( "AFT_LONGITUDE" );
      System.clearProperty( "AFT_DME2_GRM_URLS" );
      System.clearProperty( "AFT_DME2_FORCE_GRM_LOOKUP" );
      System.clearProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" );
      System.clearProperty( "DME2_SEP_CACHE_TTL_MS" );
    }
    System.out.println( "-----------------------------------------------------------" );
    System.out.println( "Ending testGrmPublish_RetryAddServiceEndpoint" );
    System.out.println( "-----------------------------------------------------------" );
  }

  /**
   * GRM Publish with lat/long from system properties
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmPublish_RetryAddServiceEndpoint_InvalidInput()
      throws Exception {
    System.err.println( "--- START: RetryAddServiceEndpointInvalidInput" );
    String service = "com.att.aft.dme2.TestGrmPublishRetryAddServiceEndpointInvalidInput";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 48979;
    double latitude = 33.3739;
    double longitude = -86.7983;

    String version = "1.0.0";
    String envContext = "DEVL";
    String routeOffer = "BAU_SE";
    String serviceName =
        "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

    Properties props = new Properties();
    System.setProperty( "AFT_LATITUDE", latitude + "" );
    System.setProperty( "AFT_LONGITUDE", longitude + "" );
    System.setProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "15000" );
    System.setProperty( "DME2_SEP_CACHE_TTL_MS", "15000" );
    System.setProperty( "DME2.DEBUG", "true" );

    DME2Configuration config = new DME2Configuration( "RetryAddServiceEndpointInvalidInput", props );
    DME2Manager manager = new DME2Manager( "RetryAddServiceEndpointInvalidInput", config );

    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    try {
      svcRegistry.publish( serviceName, null, hostname, port, "http", null );
    } catch ( Exception e ) {
      // Publish expected to fail with wrong GRM URL's in use
      e.printStackTrace();
    }
    List<DME2Endpoint> endpoints = null;
    try {
      endpoints = svcRegistry.findEndpoints( service, version, envContext, routeOffer );
    } catch ( Exception e ) {

    }
    assertNull( endpoints );

    Thread.sleep( 10000 );
    Thread.sleep( 30000 );
    DME2Configuration config1 = new DME2Configuration( "RetryAddServiceEndpoint1", props );
    DME2Manager manager1 = new DME2Manager( "RetryAddServiceEndpoint1", config1 );
    DME2EndpointRegistry grmRegistry = manager.getEndpointRegistry();
    List<DME2Endpoint> endpointList = (List<DME2Endpoint>) DME2UnitTestUtil
        .getPrivate( grmRegistry.getClass().getDeclaredField( "localPublishedList" ), grmRegistry );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpointList ) {
      if ( ep.getHost().equals( hostname ) && ep.getPort() == port
          && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
      }
    }
    System.err.println( "Found registered endpoint: " + found );
    assertNull( found );

    try {
      svcRegistry.unpublish( serviceName, hostname, port );
    } catch ( Exception e ) {

    } finally {
      System.clearProperty( "AFT_LATITUDE" );
      System.clearProperty( "AFT_LONGITUDE" );
      System.clearProperty( "AFT_DME2_GRM_URLS" );
      System.clearProperty( "AFT_DME2_FORCE_GRM_LOOKUP" );
      System.clearProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" );
      System.clearProperty( "DME2_SEP_CACHE_TTL_MS" );
    }
  }

  /**
   * Test to use scld_platform system arg to identify SOA platform platform jvm arg is removed
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmPublishWithScldPlat() throws DME2Exception, InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmPublishWithScldPlatform" );
    String service = "com.att.aft.dme2.TestGrmPublishWithScldPlatform";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 3267;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName =
        "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    Properties props = new Properties();
    System.clearProperty( "platform" );
    System.setProperty( "SCLD_PLATFORM", TestConstants.GRM_PLATFORM_TO_USE );
    System.setProperty( "platform", "SANDBOX-ABC" );

    //Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "TestGrmPublishWithScldPlatform", props );

    DME2Manager manager = new DME2Manager( "TestGrmPublishWithScldPlatform", config );

    //DME2Manager manager = new DME2Manager("TestGrmPublishWithScldPlatform",props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude, longitude, "http" );

    System.err.println( "Service published successfully." );

    try {
      Thread.sleep( 10000 );

      DME2Endpoint[] endpoints = svcRegistry.find( service, version, envContext, routeOffer );

      DME2Endpoint found = null;
      for ( DME2Endpoint ep : endpoints ) {
        if ( ep.getHost().equals( hostname ) && ep.getPort() == port
            && ep.getLatitude() == latitude
            && ep.getLongitude() == longitude ) {
          found = ep;
        }
      }
      System.err.println( "Found registered endpoint: " + found );
      assertNotNull( found );

    } finally {
      try {
        svcRegistry.unpublish( serviceName, hostname, port );
      } catch ( Exception e ) {
      }
    }
  }

  /**
   * test case that would test the defect about multiple endpoints published in same jvm, not being updated for lease
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmMultipleEndpointsLease() throws DME2Exception, InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmMultipleEndpointsLease" );

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );

    String service = "com.att.aft.dme2.TestGRMMultipleEndpointsLease";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32676;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext +
        "/routeOffer=" + routeOffer;

    DME2Configuration config = new DME2Configuration( "testGrmMultipleEndpointsLease", props );
    DME2Manager manager = new DME2Manager( "testGrmMultipleEndpointsLease", config );
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude, longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude, longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude, longitude, "http" );
    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version, envContext, routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
        System.err.println( "Found endpoints after publish " + ep );
      }
    }
    // System.err.println("Found registered endpoint: " + found);
    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.refresh();
    Thread.sleep( 11000 );
    List<DME2Endpoint> leasedEndpoints = svcRegistry.findEndpoints( service, version, envContext, routeOffer );

    for ( DME2Endpoint leasedEp : leasedEndpoints ) {
      System.err.println( "Found endpoints after lease " + leasedEp );
      if ( !hostname.equals( leasedEp.getHost() ) ) {
        continue;
      }
      int leasedEpPort = leasedEp.getPort();
      for ( DME2Endpoint ep : endpoints ) {
        int epPort = ep.getPort();
        if ( leasedEpPort == epPort && hostname.equals( ep.getHost() ) ) {
          long pubEpLease = ep.getLease();
          long leasedEpLease = leasedEp.getLease();
          if ( leasedEpLease <= pubEpLease ) {
            // Endpoints have not refreshed, throw error
            fail( "Leased endpoint for " + leasedEp.getHost() + ":" + leasedEp.getPort() + leasedEp.getPath() +
                " has expiration time=" + leasedEp.getLease() + "which should have been greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() + ep.getPath() + " expiration time=" + ep.getLease() );
          } else {
            System.err.println( "Leased endpoint for " +
                leasedEp.getHost() + ":" + leasedEp.getPort() +
                leasedEp.getPath() + " has expiration time=" +
                leasedEp.getLease() + " is greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          }
        }
      }
    }

    svcRegistry.unpublish( serviceName, hostname, port );
    Thread.sleep( 3000 );
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    Thread.sleep( 3000 );
    svcRegistry.unpublish( serviceName, hostname, port + 2 );
    System.err.println( "Service unpublished successfully." );
    Thread.sleep( 3000 );

    List<DME2Endpoint> ups = svcRegistry.findEndpoints( service, version, envContext, routeOffer );
    found = null;
    for ( DME2Endpoint ep1 : ups ) {
      if ( ep1.getHost().equals( hostname ) && ep1.getPort() == port && ep1.getLatitude() == latitude
          && ep1.getLongitude() == longitude ) {
        found = ep1;
        System.err.println( "Found endpoints after unpublish ;" + ep1 );
      }
    }
    System.err.println( "Found registered endpoint - should be null: " + found );
    assertNull( found );
  }

  /**
   * test case that would test the defect about multiple endpoints published in same jvm, not being updated for lease
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmRestfulMultipleEndpointsLease() throws DME2Exception,
      InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmRestfulMultipleEndpointsLease" );
/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
    System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );

    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "12000");
    // System.setProperty("DME2.DEBUG","true");

    //String service = "TestGRMRestfulMultipleEndpointsLease.dme2.aft.att.com";
    String servicePrefix = RandomStringUtils.randomAlphanumeric( 20 );
    String service = servicePrefix + ".dme2.aft.att.com";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32676;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName = "http://" + service + "/test/restful1?version=" + version +
        "&envContext=" + envContext + "&routeOffer=" + routeOffer;

    //Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "TestGRMRestfulMultipleEndpointsLease", props );

    DME2Manager manager = new DME2Manager( "TestGRMRestfulMultipleEndpointsLease", config );

    //DME2Manager manager = new DME2Manager("TestGRMRestfulMultipleEndpointsLease", props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );

    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    // DME2 Cache has 0 entries, DME2 Cache has 0 entries
    List<DME2Endpoint> endpoints =
        svcRegistry.findEndpoints( "com.att.aft.dme2." + servicePrefix + "*", version, envContext, routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
        System.err.println( "Found endpoints after publish " + ep );
      }
    }
    // System.err.println("Found registered endpoint: " + found);
    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.refresh();
    Thread.sleep( 11000 );

    // Both caches still at 0
    List<DME2Endpoint> leasedEndpoints =
        svcRegistry.findEndpoints( "com.att.aft.dme2." + servicePrefix + "/test/restful1", version,
            envContext, routeOffer );
    // svcRegistry.

    // DME2 Cache has 2 entries with 3 endpoints each
    // DME2 Cache has 2 entries, 1 (normal routeoffer) with 3, 1 (default routeoffer) with 6
    for ( DME2Endpoint leasedEp : leasedEndpoints ) {
      System.err.println( "Found endpoints after lease " + leasedEp );
      int leasedEpPort = leasedEp.getPort();
      for ( DME2Endpoint ep : endpoints ) {
        int epPort = ep.getPort();
        if ( leasedEpPort == epPort && leasedEp.getHost().equals( hostname ) ) {
          long pubEpLease = ep.getLease();
          long leasedEpLease = leasedEp.getLease();
          if ( leasedEpLease <= pubEpLease ) {
            // Endpoints have not refreshed, throw error
            fail( "Leased endpoint for " +
                leasedEp.getHost() +
                ":" +
                leasedEp.getPort() +
                leasedEp.getPath() +
                " has expiration time=" +
                leasedEp.getLease() +
                "which should have been greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          } else {
            System.err.println( "Leased endpoint for " +
                leasedEp.getHost() + ":" + leasedEp.getPort() +
                leasedEp.getPath() + " has expiration time=" +
                leasedEp.getLease() +
                " is greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          }
        }
      }
    }

    svcRegistry.unpublish( serviceName, hostname, port );
    Thread.sleep( 3000 );
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    Thread.sleep( 3000 );
    svcRegistry.unpublish( serviceName, hostname, port + 2 );
    System.err.println( "Service unpublished successfully." );
    // svcRegistry.refresh();
    // Thread.sleep(61000);
    //svcRegistry.refresh();
    Thread.sleep( 3000 );

    List<DME2Endpoint> ups = svcRegistry.findEndpoints( "com.att.aft.dme2." + servicePrefix + "*", version, envContext,
        routeOffer );
    found = null;
    for ( DME2Endpoint ep1 : ups ) {
      if ( ep1.getHost().equals( hostname ) && ep1.getPort() == port
          && ep1.getLatitude() == latitude
          && ep1.getLongitude() == longitude ) {
        found = ep1;
        System.err.println( "Found endpoints after unpublish ;" + ep1 );
      }
    }
    System.err.println( "Found registered endpoint - should be null: " +
        found );
    assertNull( found );
		/*System.clearProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS");
		System.clearProperty("DME2_SEP_CACHE_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_EMPTY_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS");*/

  }

  /**
   * test case that would test the defect about multiple endpoints published in same jvm, not being updated for lease
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmRestfulTemplateMultipleEndpointsLease() throws DME2Exception,
      InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmRestfulTemplateMultipleEndpointsLease" );
		/*System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );

    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "12000");
    // System.setProperty("DME2.DEBUG","true");

    //String service = "TestGRMRestfulTemplateMultipleEndpointsLease.dme2.aft.att.com";
    String servicePrefix = RandomStringUtils.randomAlphabetic( 20 );
    String service = servicePrefix + ".dme2.aft.att.com";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32676;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName = "http://" + service + "/restful/{acct}/1.8c/(id)?version=" + version +
        "&envContext=" + envContext + "&routeOffer=" + routeOffer;

    //Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "TestGRMRestfulTemplateMultipleEndpointsLease", props );

    DME2Manager manager = new DME2Manager( "TestGRMRestfulTemplateMultipleEndpointsLease", config );

    //DME2Manager manager = new DME2Manager("TestGRMRestfulTemplateMultipleEndpointsLease", props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );

    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( "com.att.aft.dme2." + servicePrefix + "*", version,
        envContext, routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
        System.err.println( "Found endpoints after publish " + ep );
      }
    }
    // System.err.println("Found registered endpoint: " + found);
    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.refresh();
    Thread.sleep( 11000 );
    List<DME2Endpoint> leasedEndpoints =
        svcRegistry.findEndpoints( "com.att.aft.dme2." + servicePrefix + "/restful/{acct}/1.8c/(id)", version,
            envContext, routeOffer );
    // svcRegistry.

    for ( DME2Endpoint leasedEp : leasedEndpoints ) {
      System.err.println( "Found endpoints after lease " + leasedEp );
      int leasedEpPort = leasedEp.getPort();
      for ( DME2Endpoint ep : endpoints ) {
        int epPort = ep.getPort();
        if ( leasedEpPort == epPort ) {
          long pubEpLease = ep.getLease();
          long leasedEpLease = leasedEp.getLease();
          if ( leasedEpLease <= pubEpLease ) {
            // Endpoints have not refreshed, throw error
            fail( "Leased endpoint for " +
                leasedEp.getHost() +
                ":" +
                leasedEp.getPort() +
                leasedEp.getPath() +
                " has expiration time=" +
                leasedEp.getLease() +
                "which should have been greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          } else {
            System.err.println( "Leased endpoint for " +
                leasedEp.getHost() + ":" + leasedEp.getPort() +
                leasedEp.getPath() + " has expiration time=" +
                leasedEp.getLease() +
                " is greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          }
        }
      }
    }

    svcRegistry.unpublish( serviceName, hostname, port );
    Thread.sleep( 3000 );
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    Thread.sleep( 3000 );
    svcRegistry.unpublish( serviceName, hostname, port + 2 );
    System.err.println( "Service unpublished successfully." );
    // svcRegistry.refresh();
    // Thread.sleep(61000);
    //svcRegistry.refresh();
    Thread.sleep( 3000 );

    List<DME2Endpoint> ups = svcRegistry.findEndpoints( "com.att.aft.dme2." + servicePrefix + "*", version, envContext,
        routeOffer );
    found = null;
    for ( DME2Endpoint ep1 : ups ) {
      if ( ep1.getHost().equals( hostname ) && ep1.getPort() == port
          && ep1.getLatitude() == latitude
          && ep1.getLongitude() == longitude ) {
        found = ep1;
        System.err.println( "Found endpoints after unpublish ;" + ep1 );
      }
    }
    System.err.println( "Found registered endpoint - should be null: " +
        found );
    assertNull( found );
		/*System.clearProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS");
		System.clearProperty("DME2_SEP_CACHE_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_EMPTY_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS");*/

  }

  /**
   * test case that would test the defect about multiple routeOffer endpoints returned while input routeOffer matched
   * -DRO in same jvm, not being updated for lease
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmResolveForSingleRouteOffer() throws DME2Exception,
      InterruptedException, UnknownHostException {
    String routeOffer = "BAU_SE";
    String routeOffer1 = "BAU_NE";
/*		System.err.println("--- START: testGrmMultipleEndpointsLease");
		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/
    System.setProperty( "RO", routeOffer );

    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "12000");
    // System.setProperty("DME2.DEBUG","true");

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "12000" );

    String service = "com.att.aft.dme2.TestGrmResolveForSingleRouteOffer";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32673;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";

    String serviceName = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    String serviceName1 = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer1;

    //Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "testGrmMultipleEndpointsLease", props );

    DME2Manager manager = new DME2Manager( "testGrmMultipleEndpointsLease", config );

    //DME2Manager manager = new DME2Manager("testGrmMultipleEndpointsLease",
    //		props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName1, null, hostname, port, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName1, null, hostname, port + 1, latitude,
        longitude, "http" );

    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version,
        envContext, routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        if ( ep.getRouteOffer() != routeOffer ) {
          found = ep;
          System.err.println( "Found endpoints after publish " + ep );
        }
      }
    }
    // System.err.println("Found registered endpoint: " + found);
    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.unpublish( serviceName, hostname, port );
    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 2 );

    // svcRegistry.unpublish(serviceName1, hostname, port);
    // svcRegistry.unpublish(serviceName1, hostname, port+1);

    System.err.println( "Service unpublished successfully." );
    // svcRegistry.refresh();
    // Thread.sleep(61000);
    // svcRegistry.refresh();
    Thread.sleep( 3000 );

    List<DME2Endpoint> ups = svcRegistry.findEndpoints( service, version, envContext,
        routeOffer );
    found = null;
    for ( DME2Endpoint ep1 : ups ) {
      if ( ep1.getHost().equals( hostname ) && ep1.getPort() == port
          && ep1.getLatitude() == latitude
          && ep1.getLongitude() == longitude ) {
        if ( ep1.getRouteOffer() == routeOffer ) {
          found = ep1;
          System.err.println( "Found endpoints after unpublish ;" +
              ep1 );
        }
      }
    }
    System.err.println( "Found registered endpoint - should be null: " +
        found );
    assertNull( found );
    System.clearProperty( "RO" );
	/*	System.clearProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS");
		System.clearProperty("DME2_SEP_CACHE_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_EMPTY_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS");*/

  }

  public void testGrmEnvContextOverride() throws DME2Exception, InterruptedException, UnknownHostException {

    String routeOffer = "BAU_SE";
    String routeOffer1 = "BAU_NE";
    System.err.println( "--- START: testGrmEnvContextOverride" );

    System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "12000");
    // System.setProperty("DME2.DEBUG","true");

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "12000" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    System.setProperty( "lrmEnv", "DEV" );

    String service = "com.att.aft.dme2.TestGrmEnvContextOverride";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32674;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "LAB";

    String serviceName = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    String serviceName1 = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer1;

    //Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "TestGrmEnvContextOverride", props );

    DME2Manager manager = new DME2Manager( "TestGrmEnvContextOverride", config );

    //DME2Manager manager = new DME2Manager("TestGrmEnvContextOverride", props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName1, null, hostname, port, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName1, null, hostname, port + 1, latitude,
        longitude, "http" );

    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version, "DEV",
        routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        if ( !envContext.equals( ep.getEnvContext() ) ) {
          found = ep;
          System.err.println( "Found endpoints after publish " + ep );
        }
      }
    }

    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.unpublish( serviceName, hostname, port );
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    svcRegistry.unpublish( serviceName, hostname, port + 2 );

    System.err.println( "Service unpublished successfully." );
    Thread.sleep( 3000 );

    List<DME2Endpoint> ups = svcRegistry.findEndpoints( service, version, "DEV",
        routeOffer );
    found = null;
    for ( DME2Endpoint ep1 : ups ) {
      if ( ep1.getHost().equals( hostname ) && ep1.getPort() == port && ep1.getLatitude() == latitude
          && ep1.getLongitude() == longitude ) {
        if ( "DEV".equals( ep1.getEnvContext() ) ) {
          found = ep1;
          System.err.println( "Found endpoints after unpublish ;" + ep1 );
        }
      }
    }
    System.err.println( "Found registered endpoint - should be null: " + found );
    assertNull( found );
    System.clearProperty( "platform" );
  }

  /**
   * test case that would test the defect about partner affinity search has multiple routeOffers in one route with same
   * sequence and on refresh the endpoints did not get added [ DME2 cached ep string with routeOffers stiched together
   * e.g. RO1~RO2~RO3 and expected GRM to return routeoffer strings with value RO1~RO2~RO3 for all EP's ]
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmRefreshCachedEndpoints() throws DME2Exception,
      InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmRefreshCachedEndpoints" );

/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/

    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000");
    System.setProperty( "DME2.DEBUG", "true" );

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000" );

    String service = "com.att.aft.dme2.TestGRMRefreshCachedEndpoints";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32675;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String routeOffer1 = "BAU_SE1";
    String serviceName = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    String serviceName1 = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer1;

//		Properties props = RegistryFsSetup.init();

    String managerName = "testGrmRefreshCachedEndpoints" + RandomStringUtils.randomAlphanumeric( 25 );
    DME2Configuration config = new DME2Configuration( managerName, props );

    DME2Manager manager = new DME2Manager( managerName, config );

    //DME2Manager manager = new DME2Manager("testGrmRefreshCachedEndpoints", props);

    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );

    svcRegistry.publish( serviceName1, null, hostname, port + 3, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName1, null, hostname, port + 4, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName1, null, hostname, port + 5, latitude,
        longitude, "http" );

    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version,
        envContext, routeOffer + "~" + routeOffer1 );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
        System.err.println( "Found endpoints after publish " + ep );
      }
    }
    // System.err.println("Found registered endpoint: " + found);
    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.refresh();
    Thread.sleep( 11000 );
    List<DME2Endpoint> leasedEndpoints = svcRegistry.findEndpoints( service, version,
        envContext, routeOffer + "~" + routeOffer1 );
    // svcRegistry.

    for ( DME2Endpoint leasedEp : leasedEndpoints ) {
      System.err.println( "Found endpoints after lease " + leasedEp );
      int leasedEpPort = leasedEp.getPort();
      for ( DME2Endpoint ep : endpoints ) {
        int epPort = ep.getPort();
        if ( leasedEpPort == epPort && leasedEp.getHost().equals( hostname ) ) {
          long pubEpLease = ep.getLease();
          long leasedEpLease = leasedEp.getLease();
          if ( leasedEpLease <= pubEpLease ) {
            // Endpoints have not refreshed, throw error
            fail( "Leased endpoint for " +
                leasedEp.getHost() +
                ":" +
                leasedEp.getPort() +
                leasedEp.getPath() +
                " has expiration time=" +
                leasedEp.getLease() +
                "which should have been greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          } else {
            System.err.println( "Leased endpoint for " +
                leasedEp.getHost() + ":" + leasedEp.getPort() +
                leasedEp.getPath() + " has expiration time=" +
                leasedEp.getLease() +
                " is greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          }
        }
      }
    }

    svcRegistry.unpublish( serviceName, hostname, port );
    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 2 );
    System.err.println( "Service unpublished successfully." );
    // svcRegistry.refresh();
    // Thread.sleep(61000);
    // svcRegistry.refresh();
    Thread.sleep( 3000 );

    List<DME2Endpoint> ups = svcRegistry.findEndpoints( service, version, envContext,
        routeOffer );
    found = null;
    for ( DME2Endpoint ep1 : ups ) {
      if ( ep1.getHost().equals( hostname ) && ep1.getPort() == port
          && ep1.getLatitude() == latitude
          && ep1.getLongitude() == longitude ) {
        found = ep1;
        System.err.println( "Found endpoints after unpublish ;" + ep1 );
      }
    }
    System.err.println( "Found registered endpoint - should be null: " +
        found );
/*		System.clearProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS");
		System.clearProperty("DME2_SEP_CACHE_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_EMPTY_TTL_MS");
		System.clearProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS");*/

  }

  @Test
  public void testGrmRefreshSelectiveCachedEndpoints() throws Exception {
    logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", LogMessage.METHOD_ENTER );
    Properties props = new Properties();
    props.setProperty( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.setProperty( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.setProperty( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.setProperty( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    props.setProperty( "DME2_SERVICE_LAST_QUERIED_INTERVAL_MS", "5000" );
    props.setProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "10000" );
    props.setProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS",
        "8000" ); // Setting a lower value for renew, so that more frequent expiry updates happen.
    props.setProperty( "DME2.DEBUG", "true" );

    DME2Manager manager = null;

    String service =
        "/service=com.att.aft.TestGrmRefreshSelectiveCachedEndpoints1/version=1.0.0/envContext=DEV/routeOffer=D1";
    EchoServlet s = new EchoServlet( service, "1" );

    String service1 =
        "/service=com.att.aft.TestGrmRefreshSelectiveCachedEndpoints2/version=1.0.0/envContext=DEV/routeOffer=D1";
    EchoServlet s1 = new EchoServlet( service1, "1" );

    try {
      DME2Configuration config = new DME2Configuration( "testGrmRefreshSelectiveCachedEndpoints", props );

      manager = new DME2Manager( "testGrmRefreshSelectiveCachedEndpoints", config );
      manager.bindServiceListener( service, s );
      manager.bindServiceListener( service1, s1 );

      Thread.sleep( 5000 );
      System.out.println( "Service published successfully." );

      // Send some requests to service, so that lastQueriedMap is updated.
      Request request = new RequestBuilder( 
          new URI( "http://DME2SEARCH/" + service + "/partner=AA" ) ).withReturnResponseAsBytes( true )
          .withLookupURL( "http://DME2SEARCH/" + service + "/partner=AA" ).withReadTimeout( 30000 )
          .withPerEndpointTimeoutMs( 20000 ).build();

      DME2Client client = new DME2Client( manager, request );
      DME2Payload payload = new DME2TextPayload( "echo" );

      String reply = (String) client.sendAndWait( payload );
      assertTrue( reply != null );

      request = new RequestBuilder( 
          new URI( "http://DME2SEARCH/" + service1 + "/partner=AA" ) ).withHttpMethod( "POST" )
          .withPerEndpointTimeoutMs( 20000 ).withReadTimeout( 30000 )
          .withReturnResponseAsBytes( false ).withLookupURL( "http://DME2SEARCH/" + service1 + "/partner=AA" ).build();

      client = new DME2Client( manager, request );
      payload = new DME2TextPayload( "echo" );

      logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", "first request" );
      reply = (String) client.sendAndWait( payload );
      assertTrue( reply != null );

      Thread.sleep( 5000 );

      DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
      logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", "first find" );
      List<DME2Endpoint> endpoints =
          svcRegistry.findEndpoints( "com.att.aft.TestGrmRefreshSelectiveCachedEndpoints1", "1.0.0", "DEV", "D1" );
      DME2Endpoint found = null;
      String url1 = null;

      long leaseTime1 = 0;
      long leaseTime2 = 0;

      for ( DME2Endpoint ep : endpoints ) {
        found = ep;
        url1 = ep.getProtocol() + "://" + ep.getHost() + ":" + ep.getPort() + ep.getContextPath();
        leaseTime1 = ep.getLease();
        logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints",
            "Found endpoints after publish " + ep + "; URL = " + url1 );
      }

      assertNotNull( found );

      found = null;
      String url2 = null;
      logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", "second find" );
      endpoints =
          svcRegistry.findEndpoints( "com.att.aft.TestGrmRefreshSelectiveCachedEndpoints2", "1.0.0", "DEV", "D1" );

      for ( DME2Endpoint ep : endpoints ) {
        found = ep;
        url2 = ep.getProtocol() + "://" + ep.getHost() + ":" + ep.getPort() + ep.getContextPath();
        leaseTime2 = ep.getLease();
        logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints",
            "Found endpoints after publish " + ep + "; URL = " + url2 );
      }

      assertNotNull( found );

      request = new RequestBuilder( 
          new URI( "http://DME2SEARCH/" + service + "/partner=AA" ) ).withPerEndpointTimeoutMs( 20000 )
          .withReadTimeout( 30000 )
          .withReturnResponseAsBytes( true ).withLookupURL( "http://DME2SEARCH/" + service + "/partner=AA" ).build();

      client = new DME2Client( manager, request );

      //client = new DME2Client(new URI("http://DME2SEARCH/" + service + "/partner=AA"), 10000);
      payload = new DME2TextPayload( "echo" );

      logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", "second request" );
      reply = (String) client.sendAndWait( payload );
      assertTrue( reply != null );

      Thread.sleep( 10000 );

      logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", "third find" );
      endpoints =
          svcRegistry.findEndpoints( "com.att.aft.TestGrmRefreshSelectiveCachedEndpoints1", "1.0.0", "DEV", "D1" );
      for ( DME2Endpoint ep : endpoints ) {
        found = ep;
        url1 = ep.getProtocol() + "://" + ep.getHost() + ":" + ep.getPort() + ep.getContextPath();
        assertTrue( leaseTime1 != ep.getLease() );
        logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints",
            "Found endpoints after publish " + ep + "; URL = " + url1 );
      }

      Thread.sleep( 10000 );

      logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", "fourth find" );
      endpoints =
          svcRegistry.findEndpoints( "com.att.aft.TestGrmRefreshSelectiveCachedEndpoints2", "1.0.0", "DEV", "D1" );
      for ( DME2Endpoint ep : endpoints ) {
        found = ep;
        url2 = ep.getProtocol() + "://" + ep.getHost() + ":" + ep.getPort() + ep.getContextPath();
        leaseTime2 = ep.getLease();
        logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints",
            "Found endpoints after publish " + ep + "; URL = " + url2 );
      }

      Thread.sleep( 10000 );
      logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints", "fifth find" );
      endpoints =
          svcRegistry.findEndpoints( "com.att.aft.TestGrmRefreshSelectiveCachedEndpoints2", "1.0.0", "DEV", "D1" );
      for ( DME2Endpoint ep : endpoints ) {
        found = ep;
        url2 = ep.getProtocol() + "://" + ep.getHost() + ":" + ep.getPort() + ep.getContextPath();

        logger.debug( null, "testGrmRefreshSelectiveCachedEndpoints",
            "Found endpoints after publish " + ep + "; URL = " + url2 );
        assertFalse( leaseTime2 == ep.getLease() );
      }
    } finally {
      try {
        manager.unbindServiceListener( service );
      } catch ( Exception e ) {
      }
      try {
        manager.unbindServiceListener( service1 );
      } catch ( Exception e ) {

      }
    }
  }

  /**
   * test case that would test the defect about partner affinity search has multiple routeOffers in one route with same
   * sequence and on refresh the endpoints did not get added [ DME2 cached ep string with routeOffers stiched together
   * e.g. RO1~RO2~RO3 and expected GRM to return routeoffer strings with value RO1~RO2~RO3 for all EP's ]
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void testGrmRefreshCachedEndpointsContextPath()
      throws DME2Exception, InterruptedException, IOException {
    System.err
        .println( "--- START: testGrmRefreshCachedEndpointsContextPath" );

/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/

    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000");
    System.setProperty( "DME2.DEBUG", "true" );

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    props.putAll( RegistryFsSetup.init() );

    String service = "com.att.aft.dme2.TestGRMRefreshCachedEndpointsContextPath";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32677;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String routeOffer1 = "BAU_SE1";
    String testPath = "/testPath";
    String serviceName = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    String serviceName1 = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer1;

//		Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "testGrmRefreshCachedEndpointsContextPath", props );

    DME2Manager manager = new DME2Manager( "testGrmRefreshCachedEndpointsContextPath", config );

//		DME2Manager manager = new DME2Manager("testGrmRefreshCachedEndpointsContextPath", props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, testPath, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );

    svcRegistry.publish( serviceName1, null, hostname, port + 3, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName1, null, hostname, port + 4, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName1, null, hostname, port + 5, latitude,
        longitude, "http" );

    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version,
        envContext, routeOffer + "~" + routeOffer1 );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
        System.err.println( "Found endpoints after publish " + ep );
      }
    }
    // System.err.println("Found registered endpoint: " + found);
    assertNotNull( found );
    Thread.sleep( 20000 );
    svcRegistry.refresh();
    Thread.sleep( 25000 );
    List<DME2Endpoint> leasedEndpoints = svcRegistry.findEndpoints( service, version,
        envContext, routeOffer + "~" + routeOffer1 );
    // svcRegistry.

    boolean testPathFoundInEndpoint = false;
    for ( DME2Endpoint leasedEp : leasedEndpoints ) {
      System.err.println( "Found endpoints after lease " + leasedEp + "; " +
          leasedEp.getLease() );
      int leasedEpPort = leasedEp.getPort();
      for ( DME2Endpoint ep : endpoints ) {
        int epPort = ep.getPort();
        String epPath = ep.getContextPath();
        if ( epPath.equals( testPath ) ) {
          testPathFoundInEndpoint = true;
        }
        if ( leasedEpPort == epPort ) {
          long pubEpLease = ep.getLease();
          long leasedEpLease = leasedEp.getLease();
          if ( leasedEpLease < pubEpLease ) {
            // Endpoints have not refreshed, throw error
            System.err
                .println( "Leased endpoint for " +
                    leasedEp.getHost() +
                    ":" +
                    leasedEp.getPort() +
                    leasedEp.getPath() +
                    " has expiration time=" +
                    leasedEp.getLease() +
                    "which should have been greater than published Ep " +
                    ep.getHost() + ":" + ep.getPort() +
                    ep.getPath() + " expiration time=" +
                    ep.getLease() );
            fail( "Leased endpoint for " +
                leasedEp.getHost() +
                ":" +
                leasedEp.getPort() +
                leasedEp.getPath() +
                " has expiration time=" +
                leasedEp.getLease() +
                "which should have been greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          } else {
            System.err.println( "Leased endpoint for " +
                leasedEp.getHost() + ":" + leasedEp.getPort() +
                leasedEp.getPath() + " has expiration time=" +
                leasedEp.getLease() +
                " is greater than published Ep " +
                ep.getHost() + ":" + ep.getPort() +
                ep.getPath() + " expiration time=" +
                ep.getLease() );
          }
        }
      }
    }
    assertTrue( testPathFoundInEndpoint );
    svcRegistry.unpublish( serviceName, hostname, port );

    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 2 );
    System.err.println( "Service unpublished successfully." );

    // the fact we got success back from GRM is "good enough".
    // grm has the test cases to validate that deletes in the datastore work
    // svcRegistry.refresh();
    // Thread.sleep(61000);
    // svcRegistry.refresh();
    /**
     * Thread.sleep(60000);
     *
     * List<DME2Endpoint> ups = svcRegistry.findEndpoints(service, version, envContext,
     * routeOffer); found = null; for (DME2Endpoint ep1 : ups) { if (
     * ep1.getHost().equals(hostname) && ep1.getPort() == port &&
     * ep1.getLatitude() == latitude && ep1.getLongitude() == longitude ) {
     * if (System.currentTimeMillis() - 180000 < ep1.getLease() ) { found =
     * ep1; System.err.println("Found endpoints after unpublish ;" + ep1); }
     * } } System.err.println("Found registered endpoint - should be null: "
     * + found); assertNull(found);
     * System.clearProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS");
     * System.clearProperty("DME2_SEP_CACHE_TTL_MS");
     * System.clearProperty("DME2_SEP_CACHE_EMPTY_TTL_MS");
     * System.clearProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS");
     */
  }

  /**
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  @Test
  public void testGrmUnpublish() throws DME2Exception, InterruptedException,
      UnknownHostException {
    System.err.println( "--- START: testGrmUnpublish" );

/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/
    System.setProperty( "DME2.DEBUG", "true" );

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );

    String service = "com.att.aft.dme2.TestGrmUnpublish";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32678;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String serviceName = "service=" + service + "/version=" + version +
        "/envContext=" + envContext + "/routeOffer=" + routeOffer;

    //Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "testGrmUnpublish", props );

    DME2Manager manager = new DME2Manager( "testGrmUnpublish", config );

//		DME2Manager manager = new DME2Manager("testGrmUnpublish", props);
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();

    try {
      svcRegistry.unpublish( serviceName, hostname, port );
    } catch ( Throwable e ) {
    }
    try {
      svcRegistry.unpublish( serviceName, hostname, port + 1 );
    } catch ( Throwable e ) {
    }
    try {
      svcRegistry.unpublish( serviceName, hostname, port + 2 );
    } catch ( Throwable e ) {
    }

    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );
    System.err.println( "Service published successfully." );

    Thread.sleep( 10000 );

    List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version,
        envContext, routeOffer );

    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getPort() == port
          && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
      }
    }
    System.err.println( "Found registered endpoint: " + found );
    assertNotNull( found );

    svcRegistry.unpublish( serviceName, hostname, port );
    System.err.println( "Service unpublished successfully." );
    // the fact we got a successful response from GRM is enough - GRM has
    // the test case to confirm delete worked in datastore
    // svcRegistry.refresh();
    // Thread.sleep(3000);
    // svcRegistry.refresh();
    /**
     * Thread.sleep(15000);
     *
     * endpoints = svcRegistry.findEndpoints(service, version, envContext,
     * routeOffer); found = null; for (DME2Endpoint ep : endpoints) { if (
     * ep.getHost().equals(hostname) && ep.getPort() == port &&
     * ep.getLatitude() == latitude && ep.getLongitude() == longitude ) {
     * found = ep; } } //System.clearProperty("DME2.DEBUG");
     * System.err.println("Found registered endpoint - should be null: " +
     * found); assertNull(found);
     */
    try {
      svcRegistry.unpublish( serviceName, hostname, port + 1 );
      svcRegistry.unpublish( serviceName, hostname, port + 2 );
    } catch ( Exception e ) {

    }
  }

  public void GRM_testRequest() throws Exception {

    Properties props = new Properties();
    props.put( "DME2_SEP_CACHE_TTL_MS", "300000" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "300000" );
    props.put( "DME2_LEASE_REG_MS", "300000" );

    // run the server in bham.
    String[] bham_1_bau_se_args = {
        "-serverHost",
        InetAddress.getLocalHost().getCanonicalHostName(),
        "-serverPort",
        "4600",
        "-registryType",
        "GRM",
        "-servletClass",
        "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
        "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
        "-platform", TestConstants.GRM_PLATFORM_TO_USE };
    bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
    bham_1_Launcher.launch();

    String[] bham_2_bau_se_args = {
        "-serverHost",
        InetAddress.getLocalHost().getCanonicalHostName(),
        "-serverPort",
        "4601",
        "-registryType",
        "GRM",
        "-servletClass",
        "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
        "-serviceCity", "BHAM", "-serverid", "bham_2_bau_se",
        "-platform", TestConstants.GRM_PLATFORM_TO_USE };
    bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
    bham_2_Launcher.launch();

		/*
		 * String[] char_1_bau_se_args = { "-serverHost", "crcbsp01",
		 * "-serverPort", "4600", "-registryType", "FS", "-servletClass",
		 * "EchoServlet", "-serviceName",
		 * "service=com.att.aft.dme2.MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE"
		 * , "-serviceCity", "CHAR", "-serverid", "char_1_bau_se" };
		 * char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);
		 * char_1_Launcher.launch();
		 */

    try {
      Thread.sleep( 5000 );
    } catch ( Exception ex ) {
    }
    // try to call a service we just registered
    Locations.CHAR.set();
    String uriStr =
        "http://DME2SEARCH/service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";

//		Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "GRM_testRequest", props );

    DME2Manager manager = new DME2Manager( "GRM_testRequest", config );

//		DME2Manager manager = new DME2Manager("GRM_testRequest",			props);

    Request request =
        new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
            .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

    DME2Client sender = new DME2Client( manager, request );

//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
    DME2Payload payload = new DME2TextPayload( "this is a test" );
    EchoReplyHandler replyHandler = new EchoReplyHandler();
    sender.setResponseHandlers( replyHandler );
    sender.send( payload );

    String reply = replyHandler.getResponse( 60000 );
    System.err.println( reply );
    // stop server that replied
    String otherServer = null;
    if ( reply == null ) {
      fail( "first reply is null" );
    } else if ( reply.indexOf( "bham_1_bau_se" ) != -1 ) {
      bham_1_Launcher.destroy();
      otherServer = "bham_2_bau_se";
    } else if ( reply.indexOf( "bham_2_bau_se" ) != -1 ) {
      bham_2_Launcher.destroy();
      otherServer = "bham_1_bau_se";
    } else {
      fail( "reply is not from bham_1_bau_se or bham_2_bau_se.  reply=" +
          reply );
    }

    Thread.sleep( 5000 );

    request =
        new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
            .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

    sender = new DME2Client( manager, request );

    //sender = new DME2Client(manager, new URI(uriStr), 30000);
    payload = new DME2TextPayload( "this is a test" );
    replyHandler = new EchoReplyHandler();
    sender.setResponseHandlers( replyHandler );
    sender.send( payload );
    reply = replyHandler.getResponse( 60000 );
    System.err.println( "reply=" + reply );
    // reply should be from char server...
    if ( reply == null || reply.indexOf( otherServer ) == -1 ) {
      fail( "reply is null or not from the otherServer.  otherServer=" +
          otherServer + "  reply=" + reply );
    }
  }

  /**
   * testAllowEmptyEndpoints - to validate DME2 accepts zero endpoints returned from GRM and refreshes the cache to
   * remove the route offer specific service endpoints
   *
   * @throws Exception
   */

  public void GRM_testAllowEmptyEndpoints() throws Exception {
    logger.debug( null, "GRM_testAllowEmptyEndpoints", LogMessage.METHOD_ENTER );
/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/
    System.setProperty( "DME2.DEBUG", "true" );

    Properties p = new Properties();
    p.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    p.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    p.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    p.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );

    //Commenting the below since default is made true as part of SCLD-905
    //DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM = true;
    //System.setProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM", "true");
    try {
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54630",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpoints/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      String[] char_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54643",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsWithMultipleService/version=1.0.0/envContext=DEV/routeOffer=BAU_SW",
          "-serviceCity", "BHAM", "-serverid", "char_1_bau_se_args",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      char_1_Launcher = new ServerControllerLauncher( char_1_bau_se_args );
      char_1_Launcher.launch();

      String[] bham_2_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54631",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpoints/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_2_bau_se",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      bham_2_Launcher.launch();

      try {
        Thread.sleep( 5000 );
      } catch ( Exception ex ) {
      }
      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpoints/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";
      Properties props = RegistryFsSetup.init();
      //props.setProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM", "true");

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestAllowEmptyEndpoints", props );

      DME2Manager manager = new DME2Manager( "TestAllowEmptyEndpoints", config );

//			DME2Manager manager = new DME2Manager("TestAllowEmptyEndpoints", p);

      Request request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      DME2Client sender = new DME2Client( manager, request );

//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
      DME2Payload payload = new DME2TextPayload( "this is a test" );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      String reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );
      // shutdown both servers and check the endpoints

      try {
        bham_2_Launcher.destroy();
      } catch ( Exception e ) {

      }
      try {
        Thread.sleep( 15000 );
      } catch ( Exception ex ) {
      }
      // refresh all cached entries
      // manager.getEndpointRegistry().refresh();
      logger.debug( null, "GRM_testAllowEmptyEndpoints", "DME2_ALLOW_EMPTY_SEP_GRM: {}",
          DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM );
      List<DME2Endpoint> eps = manager.getEndpointRegistry().findEndpoints(
          "com.att.aft.dme2.TestAllowEmptyEndpoints", "1.0.0", "DEV",
          "BAU_SE" );
      assertTrue( eps.size() == 0 );

      //request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

      //sender = new DME2Client(mgr, request);

      //sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );

      // Make sure other service clients in the same jvm works fine with grm returned zero ep's allowed.
      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpointsWithMultipleService/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );
      //sender = new DME2Client(manager, new URI(uriStr), 30000);

      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );

    } finally {
      try {
        if ( bham_1_Launcher != null ) {
          bham_1_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }
      try {
        if ( bham_2_Launcher != null ) {
          bham_2_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }

      try {
        Thread.sleep( 10000 );
      } catch ( Exception ex ) {
      }
			/*System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");*/
      //System.clearProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM");
      //DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM = false;
      logger.debug( null, "GRM_testAllowEmptyEndpoints", LogMessage.METHOD_EXIT );
    }
  }

  public void GRM_testAllowEmptyEndpointsGRMFails() throws Exception {
/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/
    System.setProperty( "DME2.DEBUG", "true" );

    Properties p = new Properties();
    p.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    p.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    p.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    p.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );

    //Commenting the below since default is made true as part of SCLD-905
    //DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM = true;
    //System.setProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM", "true");
    try {
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54630",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsGRMFails/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      String[] char_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54643",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsGRMFails/version=1.0.0/envContext=DEV/routeOffer=BAU_SW",
          "-serviceCity", "BHAM", "-serverid", "char_1_bau_se_args",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      char_1_Launcher = new ServerControllerLauncher( char_1_bau_se_args );
      char_1_Launcher.launch();

      String[] bham_2_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54631",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsGRMFails/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_2_bau_se",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      bham_2_Launcher.launch();

      try {
        Thread.sleep( 5000 );
      } catch ( Exception ex ) {
      }
      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpointsGRMFails/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";
      Properties props = RegistryFsSetup.init();
      //props.setProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM", "true");

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestAllowEmptyEndpointsGRMFails", props );

      DME2Manager manager = new DME2Manager( "TestAllowEmptyEndpointsGRMFails", config );

      //DME2Manager manager = new DME2Manager("TestAllowEmptyEndpointsGRMFails", p);

      Request request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      DME2Client sender = new DME2Client( manager, request );

      //		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
      DME2Payload payload = new DME2TextPayload( "this is a test" );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      String reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );
      // shutdown both servers and check the endpoints

      try {
        bham_2_Launcher.destroy();
      } catch ( Exception e ) {

      }
      // Forcing GRM url to be loaded with an invalid one instead of discovery provided
      // To simulate grm call failure when refresh happens
      // GRM call failure during refresh should stop the endpoints from being removed on client side.
      // registry lookup should return non-zero endpoints for BAU_SE endpoint that was shutdown in above destroy step
      System.setProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true" );
      System.setProperty( "AFT_DME2_GRM_URLS", "http://sarek.mo.sbc.com:9898/grmlwp/v1" );
      try {
        Thread.sleep( 15000 );
      } catch ( Exception ex ) {
      }
      // refresh all cached entries
      // manager.getEndpointRegistry().refresh();
      System.out.println( "...." + DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM );
      List<DME2Endpoint> eps = manager.getEndpointRegistry().findEndpoints(
          "com.att.aft.dme2.TestAllowEmptyEndpointsGRMFails", "1.0.0", "DEV",
          "BAU_SE" );
      // GRM endpoint is set to invalid, which should simulate GRM failure
      // and the endpoint cached locally should not have been removed.
      assertTrue( eps.size() > 0 );

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

//			sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );

      // Make sure other service clients in the same jvm works fine with grm returned zero ep's allowed.
      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpointsGRMFails/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

//			sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );

    } finally {
      try {
        if ( bham_1_Launcher != null ) {
          bham_1_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }
      try {
        if ( bham_2_Launcher != null ) {
          bham_2_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }
      try {
        if ( char_1_Launcher != null ) {
          char_1_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }

      try {
        Thread.sleep( 10000 );
      } catch ( Exception ex ) {
      }
/*			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("AFT_DME2_GRM_URLS");
			System.clearProperty("AFT_DME2_FORCE_GRM_LOOKUP");*/
      //System.clearProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM");
      //DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM = false;
    }
  }

  public void GRM_testAllowEmptyEndpointsForSingleRouteOffer()
      throws Exception {
/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/
    System.setProperty( "DME2.DEBUG", "true" );
    System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );

    Properties p = new Properties();
    p.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    p.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    p.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    p.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    //DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM = true;
    //System.setProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM", "true");
    try {
      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54640",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsForSingleRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      try {
        Thread.sleep( 5000 );
      } catch ( Exception ex ) {
      }
      // try to call a service we just registered
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpointsForSingleRouteOffer/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";
      Properties props = RegistryFsSetup.init();
      //props.setProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM", "true");

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestAllowEmptyEndpointsForSingleRouteOffer", props );

      DME2Manager manager = new DME2Manager( "TestAllowEmptyEndpointsForSingleRouteOffer", config );

//			DME2Manager manager = new DME2Manager("TestAllowEmptyEndpointsForSingleRouteOffer", p);

      Request request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      DME2Client sender = new DME2Client( manager, request );

      //DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
      DME2Payload payload = new DME2TextPayload( "this is a test" );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      String reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );
      // shutdown both servers and check the endpoints

      try {
        bham_1_Launcher.destroy();
      } catch ( Exception e ) {

      }
      try {
        Thread.sleep( 15000 );
      } catch ( Exception ex ) {
      }
      // refresh all cached entries
      // manager.getEndpointRegistry().refresh();
      System.out.println( "...." + DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM );
      List<DME2Endpoint> eps = manager
          .getEndpointRegistry()
          .findEndpoints( "com.att.aft.dme2.TestAllowEmptyEndpointsForSingleRouteOffer",
              "1.0.0", "DEV", "BAU_SE" );
      assertTrue( eps.size() == 0 );

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

      //sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      try {
        sender.send( payload );

        reply = replyHandler.getResponse( 60000 );
        System.err.println( reply );
      } catch ( Exception e ) {
        // Server had been shutdown, local cache allows zero endpoints
        // forced
        // so requests should fail with no endpoints code
        e.printStackTrace();
        assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) );
      }
      uriStr =
          "http://DME2RESOLVE/service=com.att.aft.dme2.TestAllowEmptyEndpointsForSingleRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

      //sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      try {
        sender.send( payload );

        reply = replyHandler.getResponse( 60000 );
        System.err.println( reply );
      } catch ( Exception e ) {
        // Server had been shutdown, local cache allows zero endpoints
        // forced
        // so requests should fail with no endpoints code for
        // DME2RESOLVE query also
        e.printStackTrace();
        assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) || e.getMessage().contains( "AFT-DME2-0702" ) );
      }

      // Start the process again
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();
      try {
        Thread.sleep( 5000 );
      } catch ( Exception ex ) {
      }
      reply = null;

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

      //sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );
      reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );

      assertTrue( reply != null );

    } finally {
			/*System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");*/
      //System.clearProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM");
      //DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM = false;
      try {
        if ( bham_1_Launcher != null ) {
          bham_1_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }
      try {
        if ( bham_2_Launcher != null ) {
          bham_2_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }
      try {
        Thread.sleep( 10000 );
      } catch ( Exception ex ) {
      }

    }
  }

  public void GRM_testAllowEmptyEndpointsFalse() throws Exception {
		/*System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
		System.setProperty("DME2_SEP_CACHE_TTL_MS", "300000");
		System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "60000");
		System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "60000");*/
    System.setProperty( "AFT_DME2_ALLOW_EMPTY_SEP_GRM", "true" );
    DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM = false;

    Properties p = new Properties();
    p.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    p.put( "DME2_SEP_CACHE_TTL_MS", "300000" );
    p.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "60000" );
    p.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "60000" );

    // run the server in bham.
    try {
      String[] bham_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54640",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsFalse/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      String[] char_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54648",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsBAUSW/version=1.0.0/envContext=DEV/routeOffer=BAU_SW",
          "-serviceCity", "BHAM", "-serverid", "char_1_bau_se_args",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      char_1_Launcher = new ServerControllerLauncher( char_1_bau_se_args );
      char_1_Launcher.launch();

      String[] bham_2_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
          "-serverPort",
          "54641",
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          "service=com.att.aft.dme2.TestAllowEmptyEndpointsFalse/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
          "-serviceCity", "BHAM", "-serverid", "bham_2_bau_se",
          "-platform", TestConstants.GRM_PLATFORM_TO_USE };
      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      bham_2_Launcher.launch();

      try {
        Thread.sleep( 5000 );
      } catch ( Exception ex ) {
      }
      // try to call a service we just registered
      Locations.CHAR.set();
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpointsFalse/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";

      Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestAllowEmptyEndpointsFalse", props );

      DME2Manager manager = new DME2Manager( "TestAllowEmptyEndpointsFalse", config );

//			DME2Manager manager = new DME2Manager("TestAllowEmptyEndpointsFalse", p);

      Request request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      DME2Client sender = new DME2Client( manager, request );

//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
      DME2Payload payload = new DME2TextPayload( "this is a test" );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      String reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );
      // shutdown both servers and check the endpoints

      try {
        bham_2_Launcher.destroy();
      } catch ( Exception e ) {

      }
      try {
        Thread.sleep( 15000 );
      } catch ( Exception ex ) {
      }
      System.out.println( "...." + DME2Constants.DME2_ALLOW_EMPTY_SEP_GRM );

      List<DME2Endpoint> eps = manager.getEndpointRegistry().findEndpoints(
          "com.att.aft.dme2.TestAllowEmptyEndpointsFalse", "1.0.0",
          "DEV", "BAU_SE" );
      assertTrue( eps.size() > 0 );

      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpointsBAUSW/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

//			sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );

      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.TestAllowEmptyEndpointsFalse/version=1.0.0/envContext=DEV/dataContext=205977/partner=TEST";

      request =
          new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( manager, request );

//			sender = new DME2Client(manager, new URI(uriStr), 30000);
      payload = new DME2TextPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setResponseHandlers( replyHandler );
      sender.send( payload );

      reply = replyHandler.getResponse( 60000 );
      System.err.println( reply );
      assertTrue( reply != null );

    } finally {
			/*System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");*/
      //System.clearProperty("AFT_DME2_ALLOW_EMPTY_SEP_GRM");

      try {
        if ( bham_1_Launcher != null ) {
          bham_1_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }

      try {
        if ( bham_2_Launcher != null ) {
          bham_2_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }

      try {
        if ( char_1_Launcher != null ) {
          char_1_Launcher.destroy();
        }
      } catch ( Exception e ) {
      }

      try {
        Thread.sleep( 10000 );
      } catch ( Exception ex ) {
      }

    }
  }

  @Ignore  
  public void GRM_testMultipleVersionSelector() throws Exception {
/*		System.setProperty("DME2_EP_TTL_MS", "300000");
		System.setProperty("DME2_ROUTEINFO_CACHE_TTL_MS", "300000");
		System.setProperty("DME2_LEASE_REG_MS", "300000");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);*/

    Properties props = RegistryFsSetup.init();
    props.put( "DME2_SEP_CACHE_TTL_MS", "300000" );
    props.put( "DME2_ROUTEINFO_CACHE_TTL_MS", "300000" );
    props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "300000" );

    props.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );

//		Properties props = RegistryFsSetup.init();

    DME2Configuration config = new DME2Configuration( "TestGrmMultipleVersion", props );

    DME2Manager manager = new DME2Manager( "TestGrmMultipleVersion", config );

    //	DME2Manager manager = new DME2Manager("TestGrmMultipleVersion", props);

    RouteInfo rtInfo = new RouteInfo();
    rtInfo.setServiceName( "com.att.aft.dme2.MyService" );
    //rtInfo.setServiceVersion("*");
    rtInfo.setEnvContext( "DEV" );
    RouteGroups rtGrps = new RouteGroups();
    rtInfo.setRouteGroups( rtGrps );

    RouteGroup rg1 = new RouteGroup();
    rg1.setName( "RG1" );
    rg1.getPartner().add( "test1" );
    rg1.getPartner().add( "test2" );
    rg1.getPartner().add( "test3" );

    Route rt1 = new Route();
    rt1.setName( "rt1" );
    rt1.setVersionSelector( "1.0.0" );
    RouteOffer ro1 = new RouteOffer();
    ro1.setActive( true );
    ro1.setSequence( 1 );
    ro1.setName( "BAU_SE" );

    Route rt2 = new Route();
    rt2.setName( "rt2" );
    rt2.setVersionSelector( "2.0.0" );
    RouteOffer ro2 = new RouteOffer();
    ro2.setActive( true );
    ro2.setSequence( 1 );
    ro2.setName( "BAU_SE" );

    Route rt3 = new Route();
    rt3.setName( "rt3" );
    RouteOffer ro3 = new RouteOffer();
    ro3.setActive( true );
    ro3.setSequence( 1 );
    ro3.setName( "BAU_SE" );

    rt1.getRouteOffer().add( ro1 );
    rt1.getRouteOffer().add( ro2 );
    rt1.getRouteOffer().add( ro3 );

    rtGrps.getRouteGroup();
    rtGrps.getRouteGroup().add( rg1 );

    // Ignoreing the above constructed routeInfo, since there is some
    // parsing issue while marshalling the
    // the routeInfo obj to xml

    // run the server in bham.
    String[] bham_1_bau_se_args = {
        "-serverHost",
        InetAddress.getLocalHost().getCanonicalHostName(),
        "-serverPort",
        "4600",
        "-registryType",
        "GRM",
        "-servletClass",
        "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
        "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
        "-platform", TestConstants.GRM_PLATFORM_TO_USE };
    bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
    bham_1_Launcher.launch();

    String[] bham_2_bau_se_args = {
        "-serverHost",
        InetAddress.getLocalHost().getCanonicalHostName(),
        "-serverPort",
        "4601",
        "-registryType",
        "GRM",
        "-servletClass",
        "EchoServlet",
        "-serviceName",
        "service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
        "-serviceCity", "BHAM", "-serverid", "bham_2_bau_se",
        "-platform", TestConstants.GRM_PLATFORM_TO_USE };
    bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
    bham_2_Launcher.launch();

		/*
		 * String[] char_1_bau_se_args = { "-serverHost", "crcbsp01",
		 * "-serverPort", "4600", "-registryType", "FS", "-servletClass",
		 * "EchoServlet", "-serviceName",
		 * "service=com.att.aft.dme2.MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE"
		 * , "-serviceCity", "CHAR", "-serverid", "char_1_bau_se" };
		 * char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);
		 * char_1_Launcher.launch();
		 */

    try {
      Thread.sleep( 5000 );
    } catch ( Exception ex ) {
    }
    // Save the route info

    RegistryFsSetup grmInit = new RegistryFsSetup();

//    grmInit.saveRouteInfo( config, rtInfo, "DEV" );
    try {
      Thread.sleep( 10000 );
    } catch ( Exception ex ) {
    }

    // try to call a service we just registered
    Locations.CHAR.set();
    String uriStr =
        "http://DME2SEARCH/service=com.att.aft.dme2.MyService/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";

    Request request =
        new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
            .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

    DME2Client sender = new DME2Client( manager, request );

//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
    DME2Payload payload = new DME2TextPayload( "this is a test" );
    EchoReplyHandler replyHandler = new EchoReplyHandler();
    sender.setResponseHandlers( replyHandler );
    sender.send( payload );

    String reply = replyHandler.getResponse( 60000 );
    System.err.println( reply );
    // stop server that replied
    String otherServer = null;
    if ( reply == null ) {
      fail( "first reply is null" );
    } else if ( reply.indexOf( "bham_1_bau_se" ) != -1 ) {
      bham_1_Launcher.destroy();
      otherServer = "bham_2_bau_se";
    } else if ( reply.indexOf( "bham_2_bau_se" ) != -1 ) {
      bham_2_Launcher.destroy();
      otherServer = "bham_1_bau_se";
    } else {
      fail( "reply is not from bham_1_bau_se or bham_2_bau_se.  reply=" +
          reply );
    }

    Thread.sleep( 5000 );

    request =
        new RequestBuilder(  new URI( uriStr ) ).withHttpMethod( "POST" )
            .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

    sender = new DME2Client( manager, request );

    //sender = new DME2Client(manager, new URI(uriStr), 30000);

    payload = new DME2TextPayload( "this is a test" );
    replyHandler = new EchoReplyHandler();
    sender.setResponseHandlers( replyHandler );
    sender.send( payload );
    reply = replyHandler.getResponse( 60000 );
    System.err.println( "reply=" + reply );
    // reply should be not null
    if ( reply == null ) {
      fail( "reply is null or not from the otherServer.  otherServer=" +
          otherServer + "  reply=" + reply );
    }

/*		System.clearProperty("DME2_EP_TTL_MS");
		System.clearProperty("DME2_RT_TTL_MS");
		System.clearProperty("DME2_LEASE_REG_MS");*/
    System.clearProperty( "platform" );
  }

  @Test
  @Ignore  
  public void testRefreshCachedRouteInfo_WithOverrideFromDME2Bootstrap()
      throws DME2Exception, InterruptedException, URISyntaxException, MalformedURLException {
    System.setProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE );
    System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
    DME2Manager mgr = null;

    String serviceURI_1 = DME2Utils
        .buildServiceURIString( "com.att.aft.dme2.test.testRefreshCachedRouteInfo_WithOverrideFromDME2Bootstrap", "1.0.0", "LAB", "PRIMARY" );
    String serviceURI_2 = DME2Utils
        .buildServiceURIString( "com.att.aft.dme2.test.testRefreshCachedRouteInfo_WithOverrideFromDME2Bootstrap", "1.0.0", "LAB", "SECONDARY" );
    String clientURI =
        "http://DME2SEARCH/service=com.att.aft.dme2.test.testRefreshCachedRouteInfo_WithOverrideFromDME2Bootstrap/version=1.0.0/envContext=LAB/partner=DME2_TEST";

    try {
      Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      //Properties props = RegistryFsSetup.init();

      //	Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestRefreshCachedInfo", props );

      mgr = new DME2Manager( "TestRefreshCachedInfo", config );

      //	mgr = new DME2Manager("TestRefreshCachedInfo", props);
      mgr.bindServiceListener( serviceURI_1, new EchoResponseServlet( serviceURI_1, "ID_1" ) );

      Thread.sleep( 1000 );

      //mgr.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "ID_2"));

      Thread.sleep( 1000 );

      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );

      //	DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

      String reply = (String) client.sendAndWait( payload );
      System.out.println( reply );

      assertTrue( reply.contains( "Request=THIS IS A TEST" ) );
			
			/*Getting the routeInfo cache from the registry to use for validation*/
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr.getEndpointRegistry();

//			Map<String, DME2RouteInfo> routeInfoData = registry.getRouteInfoCache();
			
			/*Checking if routeInfo for the service used in this test case is present in the cache*/
//			String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestRefreshCachedRouteInfo", "1.0.0", "LAB");
//			assertTrue(routeInfoData.containsKey(serviceURI));
			
			/*Checking if the routeInfo contains DME2BootstrapData*/
//			String DME2BoostrapData = routeInfoData.get(serviceURI).getDME2BootstrapProperties();
//			assertTrue(DME2BoostrapData != null);
			
			/*Checking if the cache TTL value is the same as what was provided in the DME2BootstrapData*/
//			long TTL = routeInfoData.get(serviceURI).getCacheTTL();
//			assertTrue(TTL == 15000);

/**      long currentExpiration = routeInfoData.get(serviceURI).getExpirationTime();
 Thread.sleep(5000);
 assertTrue(currentExpiration > System.currentTimeMillis());

 long currentExpiration_2 = routeInfoData.get(serviceURI).getExpirationTime();
 assertTrue(currentExpiration == currentExpiration_2);

 Thread.sleep(17000);
 long newExpiration = routeInfoData.get(serviceURI).getExpirationTime();

 assertTrue(newExpiration > currentExpiration);

 TTL = routeInfoData.get(serviceURI).getCacheTTL();
 assertTrue(TTL == 15000);
 */
    } finally {
      try {
        mgr.unbindServiceListener( serviceURI_1 );
      } catch ( Exception e ) {
      }

      try {
        mgr.unbindServiceListener( serviceURI_2 );
      } catch ( Exception e ) {
      }

    }
  }

  @Test
  public void testRefreshCachedEndpoints_WithOverrideFromDME2Bootstrap()
      throws InterruptedException, DME2Exception, URISyntaxException, MalformedURLException {
		/*NOTE: using same service name as in above test*/
    System.setProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE );
    System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
    DME2Manager mgr = null;

    String serviceURI_1 = DME2Utils
        .buildServiceURIString( "com.att.aft.dme2.test.TestRefreshCachedRouteInfo", "1.0.0", "LAB", "PRIMARY" );
    String serviceURI_2 = DME2Utils
        .buildServiceURIString( "com.att.aft.dme2.test.TestRefreshCachedRouteInfo", "1.0.0", "LAB", "SECONDARY" );
    String clientURI =
        "http://DME2SEARCH/service=com.att.aft.dme2.test.TestRefreshCachedRouteInfo/version=1.0.0/envContext=LAB/partner=DME2_TEST";

    try {
      Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      //Properties props = RegistryFsSetup.init();

      //	Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestRefreshCachedInfo", props );

      mgr = new DME2Manager( "TestRefreshCachedInfo", config );

//			mgr = new DME2Manager("TestRefreshCachedInfo", props);
      mgr.bindServiceListener( serviceURI_1, new EchoResponseServlet( serviceURI_1, "ID_1" ) );

      Thread.sleep( 1000 );

      //mgr.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "ID_2"));

      Thread.sleep( 1000 );

      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );

//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

      String reply = (String) client.sendAndWait( payload );
      System.out.println( reply );

      assertTrue( reply.contains( "Request=THIS IS A TEST" ) );
			
			/*Getting the routeInfo and endpoint cache from the registry to use for validation*/
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr.getEndpointRegistry();

//			Map<String, DME2RouteInfo> routeInfoData = registry.getRouteInfoCache();
			
			/*Checking if routeInfo for the service used in this test case is present in the cache*/
      String serviceURI =
          DME2Utils.buildServiceURIString( "com.att.aft.dme2.test.TestRefreshCachedRouteInfo", "1.0.0", "LAB" );
//			assertTrue(routeInfoData.containsKey(serviceURI));
			
			/*Checking if the routeInfo contains DME2BootstrapData*/
//			String DME2BoostrapData = routeInfoData.get(serviceURI).getDME2BootstrapProperties();
//			assertNotNull(DME2BoostrapData);
						
			/*Getting the endpoint cache from the registry to use for validation*/
//			Map<String, DME2ServiceEndpointData> endpointCache = registry.getEndpointCache();
//			DME2ServiceEndpointData endpointData = endpointCache.get(serviceURI_1);

//			assertNotNull(endpointData);
//			assertTrue(endpointData.getCacheTTL() == 15000);

//			long currentExpiration = endpointData.getExpirationTime();
//			Thread.sleep(5000);
//			assertTrue(currentExpiration > System.currentTimeMillis());

//			long currentExpiration_2 = endpointCache.get(serviceURI_1).getExpirationTime();
//			assertTrue(currentExpiration == currentExpiration_2);

//			Thread.sleep(19000);
//			long newExpiration = endpointCache.get(serviceURI_1).getExpirationTime();
//			assertTrue(newExpiration > currentExpiration);

//			long TTL = endpointCache.get(serviceURI_1).getCacheTTL();
//			assertTrue(TTL == 15000);

    } finally {
      try {
        mgr.unbindServiceListener( serviceURI_1 );
      } catch ( Exception e ) {
      }

      try {
        mgr.unbindServiceListener( serviceURI_2 );
      } catch ( Exception e ) {
      }

    }
  }

  @Test
  public void testRefreshRegistryCache_WithOverrideFromDME2Bootstrap_InvalidValues()
      throws DME2Exception, MalformedURLException, URISyntaxException, InterruptedException {
		/*NOTE: using same service name as in above test*/
    System.setProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE );
    System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
    DME2Manager mgr = null;

    String serviceURI_1 = DME2Utils
        .buildServiceURIString( "com.att.aft.dme2.test.TestRefreshCachedRouteInfoInvalid", "1.0.0", "LAB", "PRIMARY" );
    String serviceURI_2 = DME2Utils
        .buildServiceURIString( "com.att.aft.dme2.test.TestRefreshCachedRouteInfoInvalid", "1.0.0", "LAB",
            "SECONDARY" );
    String clientURI =
        "http://DME2SEARCH/service=com.att.aft.dme2.test.TestRefreshCachedRouteInfoInvalid/version=1.0.0/envContext=LAB/partner=DME2_TEST";

    try {
      Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      //Properties props = RegistryFsSetup.init();

      //		Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestRefreshCachedInfoInvalid", props );

      mgr = new DME2Manager( "TestRefreshCachedInfoInvalid", config );

      //	mgr = new DME2Manager("TestRefreshCachedInfoInvalid", props);
      mgr.bindServiceListener( serviceURI_1, new EchoResponseServlet( serviceURI_1, "ID_1" ) );

      Thread.sleep( 1000 );

      //mgr.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, "ID_2"));

      Thread.sleep( 1000 );

      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );

//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

      String reply = (String) client.sendAndWait( payload );
      System.out.println( reply );

      assertTrue( reply.contains( "Request=THIS IS A TEST" ) );
			
			/*Getting the routeInfo and endpoint cache from the registry to use for validation*/
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr.getEndpointRegistry();

//			Map<String, DME2RouteInfo> routeInfoData = registry.getRouteInfoCache();
			
			/*Checking if routeInfo for the service used in this test case is present in the cache*/
/**      String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestRefreshCachedRouteInfoInvalid", "1.0.0", "LAB");
 assertTrue(routeInfoData.containsKey(serviceURI));
 */			
			/*Checking if the routeInfo contains DME2BootstrapData*/
/**      String DME2BoostrapData = routeInfoData.get(serviceURI).getDME2BootstrapProperties();
 assertNotNull(DME2BoostrapData);

 System.err.println(String.format("DME2Bootstrap data retrived from GRM: \n %s", DME2BoostrapData));
 */						
			/*Getting the endpoint cache from the registry to use for validation*/
/**      Map<String, DME2ServiceEndpointData> endpointCache = registry.getEndpointCache();
 DME2ServiceEndpointData endpointData = endpointCache.get(serviceURI_1);

 assertNotNull(endpointData);
 */			
			/*Since invalid values where provided, these should be set to defaul of 5 minutes*/
/**      System.err.println(String.format("Endpoint Cache TTL time: %s", endpointData.getCacheTTL()));
 System.err.println(String.format("RouteInfo Cache TTL time: %s", routeInfoData.get(serviceURI).getCacheTTL()));

 assertTrue(endpointData.getCacheTTL() == 300000);
 assertTrue(routeInfoData.get(serviceURI).getCacheTTL() == 300000);
 */
    } finally {
      try {
        mgr.unbindServiceListener( serviceURI_1 );
      } catch ( Exception e ) {
      }

      try {
        mgr.unbindServiceListener( serviceURI_2 );
      } catch ( Exception e ) {
      }

    }
  }

  @Test
  public void testFindAndFilterInvalidEndpoints() {
    DME2Manager mgr = null;
    String serviceURI =
        "/service=com.att.aft.dme2.test.TestFindAndFilterInvalidEndpoints/version=1.0.0/envContext=LAB/routeOffer=";

    try {

      Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "testFindAndFilterInvalidEndpoints", props );

      mgr = new DME2Manager( "testFindAndFilterInvalidEndpoints", config );

      //	mgr = new DME2Manager("testFindAndFilterInvalidEndpoints", RegistryFsSetup.init());
      //mgr.getEndpointRegistry().publish(serviceURI, serviceURI, InetAddress.getLocalHost().getHostAddress(), 2525, "http", false);
      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry()
          .findEndpoints( "com.att.aft.dme2.test.TestFindAndFilterInvalidEndpoints", "1.0.0", "LAB", null );

      System.out.println( "Endpoint List Size = " + endpoints.size() );
      assertEquals( 0, endpoints.size() );
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
    }
  }

  @Test
  public void testPersistCachedEndpoints() throws DME2Exception, InterruptedException {
    System.out.println( "-----------------------------------------------------------" );
    System.out.println( "Starting testPersistCachedEndpoints" );
    System.out.println( "-----------------------------------------------------------" );
    Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
    props.setProperty( DME2Constants.AFT_DME2_CONTAINER_ENV_KEY, "LAB" );

    DME2Configuration config = new DME2Configuration( "testGrmPublish1", props );

    DME2Manager mgr = null;
    //String serviceName = "com.att.aft.dme2.test.TestPersistCachedEndpoints";
    String serviceName = "com.att.aft.dme2.test." + RandomStringUtils.randomAlphanumeric( 25 );
    String serviceURI = "/service=" + serviceName + "/version=1.0.0/envContext=LAB/routeOffer=TEST_1";

    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", serviceURI );

    try {
      mgr = new DME2Manager( "TestPersistCachedEndpoints", config );
      mgr.bindServiceListener( serviceURI, new EchoResponseServlet( serviceURI, "test_ID" ) );
      Thread.sleep( 3000 );

      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints( serviceName, "1.0.0", "LAB", "TEST_1" );
      System.out.println( "Number of Endpoints returned from GRM (should be 1): " + endpoints.size() );
      assertEquals( 1, endpoints.size() );
    } finally {
      if ( mgr != null ) {
        try {
          mgr.unbindServiceListener( serviceURI );
        } catch ( DME2Exception e ) {
          System.out.println( "Error unbinding service" );
        }
      }
      System.clearProperty( DME2Constants.DME2_PERFORM_GRM_HEALTH_CHECK );
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
    }
    System.out.println( "-----------------------------------------------------------" );
    System.out.println( "Ending testPersistCachedEndpoints" );
    System.out.println( "-----------------------------------------------------------" );
  }

  @Test
  public void testPersistCachedEndpoints_LoadEndpointsFromFileOnStartUp() {
    DME2Manager mgr = null;
    //String serviceURI = "/service=com.att.aft.dme2.test.TestPersistCachedEndpoints/version=1.0.0/envContext=LAB/routeOffer=TEST_1";
    System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
    System.setProperty( "AFT_LATITUDE", "33.373900" );
    System.setProperty( "AFT_LONGITUDE", "-86.798300" );

    try {
      Properties props = new Properties();
      props.put( DME2Constants.DME2_CACHED_ENDPOINTS_FILE, "src/test/resources/cached-endpoints.ser" );

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestPersistCachedEndpoints", props );

      mgr = new DME2Manager( "TestPersistCachedEndpoints", config );

      //	mgr = new DME2Manager("TestPersistCachedEndpoints", props);

			 /*Check if Endpoint cache contains the service. They should have been load from the file when the cache was initialized*/
//			Map<String, DME2ServiceEndpointData> endpointCache = ((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().getCache();
//			System.out.println("Contents of Endpoint cache: " + endpointCache);
//			assertEquals(2, endpointCache.size()); //Could contain two entries, one for routeoffer TEST_1 and one for routeoffer DEFAULT
			
			/*Persisting the endpoints to the file*/
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().persistCachedEndpoints(false);
			
			/*Clear the endpoint cache and manually load the entry that has been persisted to the file*/
//			endpointCache.clear();
//			assertEquals(0, endpointCache.size());

//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().loadPersistedEndpoints();
//			System.out.println("Contents of Endpoint cache: " + endpointCache);
//			assertEquals(2, endpointCache.size()); //Could contain two entries, one for routeoffer TEST_1 and one for routeoffer DEFAULT
//			assertTrue(endpointCache.containsKey("/service=com.att.aft.dme2.test.TestPersistCachedEndpoints/version=1.0.0/envContext=LAB/routeOffer=TEST_1")); 	

    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      System.clearProperty( DME2Constants.DME2_PERFORM_GRM_HEALTH_CHECK );
    }
  }

  @Test
  public void testPersistCachedEndpoints_NoFileNameProvided() throws DME2Exception, InterruptedException {
    System.out.println( "-----------------------------------------------------------" );
    System.out.println( "Starting testPersistCachedEndpoints_NoFileNameProvided" );
    System.out.println( "-----------------------------------------------------------" );
    DME2Manager mgr = null;
    String serviceURI =
        "/service=com.att.aft.dme2.test.TestPersistCachedEndpoints/version=1.0.0/envContext=LAB/routeOffer=TEST_1";

    try {
      Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      DME2Configuration config = new DME2Configuration( "TestPersistCachedEndpoints", props );
      mgr = new DME2Manager( "TestPersistCachedEndpoints", config );

      mgr.bindServiceListener( serviceURI, new EchoResponseServlet( serviceURI, "test_ID" ) );
      Thread.sleep( 3000 );

      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry()
          .findEndpoints( "com.att.aft.dme2.test.TestPersistCachedEndpoints", "1.0.0", "LAB", "TEST_1" );
      System.out.println( "Number of Endpoints returned from GRM (should be 1): " + endpoints.size() );
      assertEquals( 1, endpoints.size() );
    } finally {
      if ( mgr != null ) {
        try {
          mgr.unbindServiceListener( serviceURI );
        } catch ( DME2Exception e ) {
          System.out.println( "Error unbinding listener" );
          e.printStackTrace();
        }
      }
    }
    System.out.println( "-----------------------------------------------------------" );
    System.out.println( "Ending testPersistCachedEndpoints_NoFileNameProvided" );
    System.out.println( "-----------------------------------------------------------" );
  }

  @Test
  public void testPersistCachedEndpoints_DisableLoadViaSystemProperty() throws DME2Exception, InterruptedException {
    DME2Manager mgr = null;
    String serviceURI =
        "/service=com.att.aft.dme2.test.TestPersistCachedEndpoints/version=1.0.0/envContext=LAB/routeOffer=TEST_1";

    try {
      System.setProperty( "AFT_DME2_PF_SERVICE_NAME", serviceURI );
      System.setProperty( DME2Constants.Cache.CACHE_ENABLE_PERSISTENCE, "false" );

      //		Properties props = new Properties();

      Properties props = null; 
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

      DME2Configuration config = new DME2Configuration( "TestPersistCachedEndpoints", props );

      mgr = new DME2Manager( "TestPersistCachedEndpoints", config );

//			mgr = new DME2Manager("TestPersistCachedEndpoints", props);
      mgr.bindServiceListener( serviceURI, new EchoResponseServlet( serviceURI, "test_ID" ) );
      Thread.sleep( 3000 );

      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry()
          .findEndpoints( "com.att.aft.dme2.test.TestPersistCachedEndpoints", "1.0.0", "LAB", "TEST_1" );
      System.out.println( "Number of Endpoints returned from GRM (should be 1): " + endpoints.size() );
      assertEquals( 1, endpoints.size() );
			
			 /*Check if Endpoint cache contains the service */
//			Map<String, DME2ServiceEndpointData> endpointCache = ((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().getCache();
//			System.out.println("Contents of Endpoint cache: " + endpointCache);
//			assertEquals(2, endpointCache.size()); //Could contain two entries, one for routeoffer TEST_1 and one for routeoffer DEFAULT
			
			/*Persisting the endpoints to the file. This operation should just return since no fileName was provided to save the endpoints to.*/
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().persistCachedEndpoints(false);
			
			/*Clear the endpoint cache and manually load the entry that has been persisted to the file*/
//			endpointCache.clear();
//			assertEquals(0, endpointCache.size());
			
			/*The load operation should be ignored due to the System Property override*/
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().loadPersistedEndpoints();
//			System.out.println("Contents of Endpoint cache: " + endpointCache);
//			assertEquals(0, endpointCache.size()); 

    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( DME2Exception e ) {
      }

      try {
//				((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().removePersistedEndpointCacheAtStartUp();
      } catch ( Exception e ) {
				/* Ignoring exception */
      }

      System.clearProperty( DME2Constants.Cache.CACHE_ENABLE_PERSISTENCE );
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
    }
  }

  @Test
  public void testPersistCachedRouteInfo()
      throws DME2Exception, InterruptedException, URISyntaxException, MalformedURLException {
    DME2Manager mgr = null;

    String serviceURI =
        "/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo/version=1.0.0/envContext=LAB/routeOffer=TEST_1";
    System.setProperty( "AFT_DME2_PF_SERVICE_NAME",
        "/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo/version=1.0.0/envContext=LAB" );
    System.setProperty( DME2Constants.DME2_PERFORM_GRM_HEALTH_CHECK, "false" );

    try {
      //Properties props = new Properties();

      Properties props =null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

      DME2Configuration config = new DME2Configuration( "TestPersistCachedRouteInfo", props );

      mgr = new DME2Manager( "TestPersistCachedRouteInfo", config );

//			mgr = new DME2Manager("TestPersistCachedRouteInfo", props);
      mgr.bindServiceListener( serviceURI, new EchoResponseServlet( serviceURI, "test_ID" ) );
      Thread.sleep( 3000 );

      String clientURI =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo/version=1.0.0/envContext=LAB/partner=DME2_TEST";

      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );

//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      String reply = (String) client.sendAndWait( payload );

      System.out.println( reply );
      assertTrue( reply.contains( "Request=THIS IS A TEST" ) );

      /** Check if RouteInfo cache contains the entry 
       Map<String, DME2RouteInfo> routeInfoCache = ((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().getCache();
       System.out.println("Contents of RouteInfo cache = " + routeInfoCache);
       assertEquals(1, routeInfoCache.size());
       */
			
			/*Persist the cache routeInfo to the file*/
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().persistCachedRouteInfo(false);
			
			
			/*Clear the cache, then manually load the entries from the file*/
//			routeInfoCache.clear();
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().loadPersistedRouteInfo();

//			System.out.println("Contents of cache after loading persisted routeInfo from file: " + routeInfoCache);
//			assertEquals(1, routeInfoCache.size()); 
//			assertTrue(routeInfoCache.containsKey("/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo/version=1.0.0/envContext=LAB"));

    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( DME2Exception e ) {
      }

      try {
//				((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().removePersistedRouteInfoCacheAtStartUp();
      } catch ( Exception e ) {
				 /*Ignoring exception */
      }

      System.clearProperty( DME2Constants.DME2_PERFORM_GRM_HEALTH_CHECK );
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
    }
  }

  @Test
  public void testPersistCachedRouteInfo_LoadRouteInfoFromFileOnStartUp() {
    DME2Manager mgr = null;
    System.setProperty( DME2Constants.DME2_PERFORM_GRM_HEALTH_CHECK, "false" );
    //String serviceURI = "/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo/version=1.0.0/envContext=LAB/routeOffer=TEST_1";

    try {
      Properties props = new Properties();
      props.put( DME2Constants.DME2_CACHED_ROUTEINFO_FILE, "src/test/resources/cached-routeinfo.ser" );

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "testGrmPublish1", props );

      mgr = new DME2Manager( "TestPersistCachedRouteInfo", config );

      //		mgr = new DME2Manager("TestPersistCachedRouteInfo", props);
			
			/* Check if RouteInfo cache contains the entry */
//			Map<String, DME2RouteInfo> routeInfoCache = ((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().getCache();
//			System.out.println("Contents of RouteInfo cache = " + routeInfoCache);
//			assertEquals(1, routeInfoCache.size());
			
			/*Persist the cache routeInfo to the file*/
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().persistCachedRouteInfo(false);
			
			
			/*Clear the cache, then manually load the entries from the file*/
//			routeInfoCache.clear();
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().loadPersistedRouteInfo();

//			System.out.println("Contents of cache after loading persisted routeInfo from file: " + routeInfoCache);
//			assertEquals(1, routeInfoCache.size()); 
//			assertTrue(routeInfoCache.containsKey("/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo/version=1.0.0/envContext=LAB"));

    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      System.clearProperty( DME2Constants.DME2_PERFORM_GRM_HEALTH_CHECK );
    }
  }

  @Test
  public void testPersistCachedRouteInfo_NoFileNameProvided()
      throws DME2Exception, MalformedURLException, URISyntaxException, InterruptedException {
    DME2Manager mgr = null;
    String serviceURI =
        "/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo_NoFileNameProvided/version=1.0.0/envContext=LAB/routeOffer=TEST_1";

    try {
//			Properties props = new Properties();

      Properties props =null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

      DME2Configuration config = new DME2Configuration( "TestPersistCachedRouteInfo", props );

      mgr = new DME2Manager( "TestPersistCachedRouteInfo", config );

//			mgr = new DME2Manager("TestPersistCachedRouteInfo", props);
      mgr.bindServiceListener( serviceURI, new EchoResponseServlet( serviceURI, "test_ID" ) );
      Thread.sleep( 3000 );

      String clientURI =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestPersistCachedRouteInfo_NoFileNameProvided/version=1.0.0/envContext=LAB/partner=DME2_TEST";

      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );

//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      String reply = (String) client.sendAndWait( payload );

      System.out.println( reply );
      assertTrue( reply.contains( "Request=THIS IS A TEST" ) );
			
			/* Check if RouteInfo cache contains the entry */
//			Map<String, DME2RouteInfo> routeInfoCache = ((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().getCache();
//			System.out.println("Contents of RouteInfo cache = " + routeInfoCache);
//			assertEquals(1, routeInfoCache.size());
			
			
			/*Persist the cache routeInfo to the file. This operation should return, since no file was provided to save the routeInfo to. */
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().persistCachedRouteInfo(false);
			
			
			/*Clear the cache, then manually load the entries from the file*/
//			routeInfoCache.clear();
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().loadPersistedRouteInfo();

//			System.out.println("Contents of cache after loading persisted routeInfo from file: " + routeInfoCache);
//			assertEquals(0, routeInfoCache.size()); 

    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( DME2Exception e ) {
      }

      try {
//				((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().removePersistedRouteInfoCacheAtStartUp();
      } catch ( Exception e ) {
				 /*Ignoring exception */
      }

    }
  }

  @Test
  public void testPersistCachedRouteInfo_DisableLoadViaSystemProperty()
      throws DME2Exception, MalformedURLException, URISyntaxException, InterruptedException {
    DME2Manager mgr = null;
    String serviceURI =
        "/service=com.att.aft.dme2.test.testPersistCachedRouteInfo_DisableLoadViaSystemProperty/version=1.0.0/envContext=LAB/routeOffer=TEST_1";

    try {
      System.setProperty( "AFT_DME2_PF_SERVICE_NAME", serviceURI );
      System.setProperty( DME2Constants.Cache.DME2_DISABLE_PERSISTENT_CACHE, "false" );

      //		Properties props = new Properties();

      Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

      DME2Configuration config = new DME2Configuration( "testGrmPublish1", props );

      mgr = new DME2Manager( "TestPersistCachedRouteInfo", config );

//			mgr = new DME2Manager("TestPersistCachedRouteInfo", props);
      mgr.bindServiceListener( serviceURI, new EchoResponseServlet( serviceURI, "test_ID" ) );
      Thread.sleep( 3000 );

      String clientURI =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.testPersistCachedRouteInfo_DisableLoadViaSystemProperty/version=1.0.0/envContext=LAB/partner=DME2_TEST";

      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );

//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000);
      DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
      String reply = (String) client.sendAndWait( payload );

      System.out.println( reply );
      assertTrue( reply.contains( "Request=THIS IS A TEST" ) );
			
			/* Check if RouteInfo cache contains the entry */
//			Map<String, DME2RouteInfo> routeInfoCache = ((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().getCache();
//			System.out.println("Contents of RouteInfo cache = " + routeInfoCache);
//			assertEquals(1, routeInfoCache.size());
//			
//			
//			/*Persist the cache routeInfo to the file. This operation should return, since no file was provided to save the routeInfo to. */
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().persistCachedRouteInfo(false);
//			
//			
//			/*Clear the cache, then manually load the entries from the file
//			The load operation should be ignored due to the System Property override*/
//			routeInfoCache.clear();
//			((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().loadPersistedRouteInfo();
//			
//			
//			System.out.println("Contents of cache after loading persisted routeInfo from file: " + routeInfoCache);
//			assertEquals(0, routeInfoCache.size()); 

    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( DME2Exception e ) {
      }

      try {
//				((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryRouteInfoCache().removePersistedRouteInfoCacheAtStartUp();
      } catch ( Exception e ) {
				/* Ignoring exception */
      }
      System.clearProperty( DME2Constants.Cache.DME2_DISABLE_PERSISTENT_CACHE_LOAD );
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

    }
  }

  @Test
  public void testPublishEnpoint_WithProperties() throws DME2Exception, UnknownHostException, InterruptedException {
    DME2Manager mgr = null;
    // String serviceName = "com.att.aft.dme2.test.TestPublishEndpointWithProperties";
    String serviceName = "com.att.aft.dme2.test." + RandomStringUtils.randomAlphanumeric( 25 );
    String serviceURI = DME2Utils.buildServiceURIString( serviceName, "1.0.0", "LAB", "TEST" );

    try {
      Properties props = new Properties();
      props.setProperty( "TEST_PROP_1", "TEST_VALUE_1" );
      props.setProperty( "TEST_PROP_2", "TEST_VALUE_2" );
      props.setProperty( "TEST_PROP_3", "TEST_VALUE_3" );

      //	Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "testPublishEnpoint_WithProperties", props );

      mgr = new DME2Manager( "testPublishEnpoint_WithProperties", config );

      //		mgr = new DME2Manager("testPublishEnpoint_WithProperties", RegistryFsSetup.init());
      mgr.getEndpointRegistry()
          .publish( serviceURI, serviceURI, InetAddress.getLocalHost().getHostAddress(), 32406, "http", props );

      Thread.sleep( 3000 );

      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints( serviceName, "1.0.0", "LAB", "TEST" );
      assertEquals( 1, endpoints.size() );

      DME2Endpoint endpoint = endpoints.get( 0 );
      System.out.println( endpoint.getEndpointProperties() );

      assertEquals( 3, endpoint.getEndpointProperties().size() );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_1" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_2" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_3" ) );
    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testRenewEndpointLease() throws DME2Exception, InterruptedException, UnknownHostException {
    DME2Manager mgr = null;
    //String serviceName = "com.att.aft.dme2.test.TestRenewEndpointLease";
    String serviceName = "com.att.aft." + RandomStringUtils.randomAlphanumeric( 25 );
    String serviceURI = DME2Utils.buildServiceURIString( serviceName, "1.0.0", "LAB", "TEST" );

    try {
      Properties props = new Properties();
      props.setProperty( "TEST_PROP_1", "TEST_VALUE_1" );
      props.setProperty( "TEST_PROP_2", "TEST_VALUE_2" );
      props.setProperty( "TEST_PROP_3", "TEST_VALUE_3" );

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "testRenewEndpointLease", props );

      mgr = new DME2Manager( "testRenewEndpointLease", config );

      //		mgr = new DME2Manager("testRenewEndpointLease", RegistryFsSetup.init());
      mgr.getEndpointRegistry()
          .publish( serviceURI, serviceURI, InetAddress.getLocalHost().getHostAddress(), 32406, "http", props );

      Thread.sleep( 3000 );

      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints( serviceName, "1.0.0", "LAB", "TEST" );
      assertEquals( 1, endpoints.size() );

      DME2Endpoint endpoint = endpoints.get( 0 );
      System.out.println( endpoint.getEndpointProperties() );

      assertEquals( 3, endpoint.getEndpointProperties().size() );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_1" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_2" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_3" ) );

      ( (DME2EndpointRegistryGRM) mgr.getEndpointRegistry() ).refresh();//renewAllLeases();

      endpoints = mgr.getEndpointRegistry().findEndpoints( serviceName, "1.0.0", "LAB", "TEST" );
      assertEquals( 1, endpoints.size() );

      endpoint = endpoints.get( 0 );
      System.out.println( endpoint.getEndpointProperties() );

      assertEquals( 3, endpoint.getEndpointProperties().size() );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_1" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_2" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_3" ) );
    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testRenewEndpointLease_OnPublish() throws DME2Exception, InterruptedException, UnknownHostException {
    DME2Manager mgr = null;
    //String serviceName = "com.att.aft.dme2.test.TestRenewEndpointLease_OnPublish";

    String serviceName = "com.att.aft.dme2.test." + RandomStringUtils.randomAlphanumeric( 25 );
    String serviceURI = DME2Utils.buildServiceURIString( serviceName, "1.0.0", "LAB", "TEST" );

    try {
      Properties props = new Properties();
      props.setProperty( "TEST_PROP_1", "TEST_VALUE_1" );
      props.setProperty( "TEST_PROP_2", "TEST_VALUE_2" );
      props.setProperty( "TEST_PROP_3", "TEST_VALUE_3" );

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "testGrmPublish1", props );

      mgr = new DME2Manager( "TestRenewEndpointLease_OnPublish", config );

//			mgr = new DME2Manager("TestRenewEndpointLease_OnPublish", RegistryFsSetup.init());
      mgr.getEndpointRegistry()
          .publish( serviceURI, serviceURI, InetAddress.getLocalHost().getHostAddress(), 32406, 33.22, 36.4, "http",
              props, true );

      Thread.sleep( 3000 );

      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints( serviceName, "1.0.0", "LAB", "TEST" );
      assertEquals( 1, endpoints.size() );

      DME2Endpoint endpoint = endpoints.get( 0 );
      System.out.println( endpoint.getEndpointProperties() );

      assertEquals( 3, endpoint.getEndpointProperties().size() );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_1" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_2" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_3" ) );

      ( (DME2EndpointRegistryGRM) mgr.getEndpointRegistry() ).refresh();//renewAllLeases();

      endpoints = mgr.getEndpointRegistry().findEndpoints( serviceName, "1.0.0", "LAB", "TEST" );
      assertEquals( 1, endpoints.size() );

      endpoint = endpoints.get( 0 );
      System.out.println( endpoint.getEndpointProperties() );

      assertEquals( 3, endpoint.getEndpointProperties().size() );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_1" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_2" ) );
      assertTrue( endpoint.getEndpointProperties().containsKey( "TEST_PROP_3" ) );
    } finally {
      try {
        DME2Endpoint[] endpointsArr =
            mgr.getEndpointRegistry().find( serviceName, "1.0.0", "LAB", "TEST" );
        for ( DME2Endpoint ep : endpointsArr ) {
          try {
            mgr.getEndpointRegistry()
                .unpublish( serviceName, ep.getHost(), ep.getPort() );
          } catch ( Exception e ) {
          }
        }
        mgr.unbindServiceListener( serviceURI );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testPublishStaticEndpoint() {
    DME2Manager mgr = null;
    // String serviceName = "com.att.aft.dme2.test.TestPublishStaticEndpoint";
    String serviceName = "com.att.aft.dme2.test." + RandomStringUtils.randomAlphanumeric( 25 );
    String serviceURI = DME2Utils.buildServiceURIString( serviceName, "1.0.0", "LAB", "TEST" );

    try {
      Properties props = new Properties();
      props.setProperty( DME2Constants.DME2_REGISTER_STATIC_ENDPOINT, "true" );

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestRenewEndpointLease_OnPublish", props );

      mgr = new DME2Manager( "TestRenewEndpointLease_OnPublish", config );

//			mgr = new DME2Manager("TestRenewEndpointLease_OnPublish", RegistryFsSetup.init());
      mgr.getEndpointRegistry()
          .publish( serviceURI, serviceURI, InetAddress.getLocalHost().getHostAddress(), 32406, 33.22, 36.4, "http",
              props,
              true );

      Thread.sleep( 3000 );

      List<DME2Endpoint> endpoints = mgr.getEndpointRegistry().findEndpoints( serviceName, "1.0.0", "LAB", "TEST" );
      assertEquals( 1, endpoints.size() );

      DME2Endpoint endpoint = endpoints.get( 0 );
      System.out.println( endpoint.getEndpointProperties() );
      System.out.println( endpoint.getLease() );

      //Make sure lease is greater than 15 minutes
      logger.debug( null, "testPublishStaticEndpoint", "Lease Expiration: {} Current Time: {} Difference: {}",
          endpoint.getLease(), System.currentTimeMillis(), ( endpoint.getLease() - System.currentTimeMillis() ) );
      assertTrue( endpoint.getLease() > ( System.currentTimeMillis() + 900000L ) );

    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals()  throws InterruptedException, MalformedURLException, DME2Exception, URISyntaxException
	{
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;
		
		String serviceURI_1 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals", "1.0.0", "LAB", "PRIMARY");
		String serviceURI_2 = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals", "1.0.0", "LAB", "SECONDARY");
		String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals/version=1.0.0/envContext=LAB/partner=DME2_TEST";
		
		try
		{
			Properties props = new Properties();
			props.setProperty("DME2_SEP_EMPTY_CACHE_TTL_INTERVALS", "60001,120001");
			props.setProperty("DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE", "false");
			
			mgr_1 = new DME2Manager("TestServer_1", props);
			mgr_2 = new DME2Manager("TestServer_2", props);
			
			mgr_1.bindServiceListener(serviceURI_1, new EchoServlet(serviceURI_1, "ID_1"));
			mgr_2.bindServiceListener(serviceURI_2, new EchoServlet(serviceURI_2, "ID_2"));
			
			Thread.sleep(5000);
			
			/*Send a request just to make sure everything is working and that PRIMARY is being consumed*/
			DME2Client client = new DME2Client(mgr_1, new URI(clientURI), 30000);
			client.setPayload("Test is a test: testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals()");
			String response = client.sendAndWait(30000);
			
			System.out.println("Response from service: " + response);
			assertTrue(response.contains("PRIMARY"));
			
			/*Shutdown the service with PRIMARY routeOffer*/
			mgr_1.unbindServiceListener(serviceURI_1);
			Thread.sleep(5000);
			
			/*Refresh should have been called as part of of the shutdown and PRIMARY routeOffer 
			 * should have been updated to use the empty cache ttl, checking the cache to validate*/
			DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr_1.getEndpointRegistry();
//			Map<String, DME2ServiceEndpointData> cache = registry.getEndpointCache();
			
//			System.out.println("Cached contents: " + cache.toString());
			assertTrue(registry.containsServiceEndpoint(serviceURI_1));
			assertTrue(registry.containsServiceEndpoint(serviceURI_2));

      logger.debug(null, "testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals1", "Cache TTL Value for service with PRIMARY routeoffer (should be 60001): " + registry.getEndpointTTL(serviceURI_1));
			assertEquals(60001L, registry.getEndpointTTL(serviceURI_1));
			
			/*Sleep for some time to let the refresh kick in. After the next refresh, the service should be using the next cache ttl interval*/
			Thread.sleep(80000);
			
//			cache = registry.getEndpointCache();
			logger.debug(null, "testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals1", "Cache TTL Value for service with PRIMARY routeoffer (should be 120001): " + registry.getEndpointTTL(serviceURI_1));
			assertEquals(120001L, registry.getEndpointTTL(serviceURI_1));
			
			/*Sleep again and check the value. Since it is the last interval declared, it should not change from the last value that was set.*/
			Thread.sleep(80000);
			
//			cache = registry.getEndpointCache();
			System.out.println("Cache TTL Value for service with PRIMARY routeoffer (should be 120001): " + registry.getEndpointTTL(serviceURI_1));
			assertEquals(120001L, registry.getEndpointTTL(serviceURI_1));
			
			/*Send a request. This one should hit SECONDARY routeOffer since PRIMARY is still down*/
			client = new DME2Client(mgr_1, new URI(clientURI), 30000);
			client.setPayload("Test is a test: testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals()");
			response = client.sendAndWait(30000);
			
			System.out.println("Response from service: " + response);
			assertTrue(response.contains("SECONDARY"));
			
			/*Bring back up PRIMARY routeOffer and let refresh kick in. This service should now be using the standard cache TTL value*/
			mgr_1.bindServiceListener(serviceURI_1, new EchoServlet(serviceURI_1, "ID_1"));
			Thread.sleep(80000);
			
//			cache = registry.getEndpointCache();
			System.out.println("Cache TTL Value for service with PRIMARY routeoffer (should be 300000): " + registry.getEndpointTTL(serviceURI_1));
			assertEquals(300000L, registry.getEndpointTTL(serviceURI_1));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr_1.unbindServiceListener(serviceURI_1);
			}catch(Exception e){}
			
			try{mgr_2.unbindServiceListener(serviceURI_2);
			}catch(Exception e){}
		}
	}
	

 
  @Test
  public void testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals_InvalidPropertyInput() {
    DME2Manager mgr_1 = null;

    String serviceURI_1 = DME2Utils.buildServiceURIString(
        "com.att.aft.dme2.test.TestRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals__InvalidPropertyInput", "1.0.0",
        "LAB", "PRIMARY" );
    String clientURI =
        "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals__InvalidPropertyInput/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";

    try {
      Properties props = new Properties();
      props.setProperty( "DME2_SEP_EMPTY_CACHE_TTL_INTERVALS", "60001,120001,abc/xyz" );

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestServer_1", props );

      mgr_1 = new DME2Manager( "TestServer_1", config );

//			mgr_1 = new DME2Manager("TestServer_1", props);
      mgr_1.bindServiceListener( serviceURI_1, new EchoServlet( serviceURI_1, "ID_1" ) );

      Thread.sleep( 5000 );

      Request request =
          new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr_1, request );
			
			/*Send a request just to make sure everything is working and that PRIMARY is being consumed*/
      //		DME2Client client = new DME2Client(mgr_1, new URI(clientURI), 30000);
      DME2Payload payload =
          new DME2TextPayload( "Test is a test: testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals()" );
      String response = (String) client.sendAndWait( payload );

      System.out.println( "Response from service: " + response );
      assertTrue( response.contains( "PRIMARY" ) );
			
			/*Shutdown the service with PRIMARY routeOffer*/
      mgr_1.unbindServiceListener( serviceURI_1 );
      Thread.sleep( 5000 );
			
			/*Refresh should have been called as part of of the shutdown and PRIMARY routeOffer 
			 * should have been updated to use the empty cache ttl, checking the cache to validate*/
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr_1.getEndpointRegistry();
//			Map<String, DME2ServiceEndpointData> cache = registry.getEndpointCache();

//			System.out.println("Cached contents: " + cache.toString());
//			assertTrue(cache.containsKey(serviceURI_1));
			
			/*Because invalid property was passed into DME2Manager, the interval values should have defaulted to 300000, 300000, 300000, 600000, 900000.
			 * The first interval should be 5 minutes. Checking this below... */
//			System.out.println("Cache TTL Value for service with PRIMARY routeoffer (should be 300000): " + cache.get(serviceURI_1).getCacheTTL());
//			assertEquals(300000, cache.get(serviceURI_1).getCacheTTL());
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      try {
        mgr_1.unbindServiceListener( serviceURI_1 );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals_SingleIntervalValueProvided() {
    DME2Manager mgr_1 = null;

    String serviceURI_1 = DME2Utils.buildServiceURIString(
        "com.att.aft.dme2.test.TestRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals__SingleIntervalValueProvided",
        "1.0.0", "LAB", "PRIMARY" );
    String clientURI =
        "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals__SingleIntervalValueProvided/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";

    try {
      Properties props = new Properties();
      props.setProperty( "DME2_SEP_EMPTY_CACHE_TTL_INTERVALS", "60001" );
      props.setProperty( "DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE", "false" );

//			Properties props = RegistryFsSetup.init();

      DME2Configuration config = new DME2Configuration( "TestServer_1", props );

      mgr_1 = new DME2Manager( "TestServer_1", config );

//			mgr_1 = new DME2Manager("TestServer_1", props);
      mgr_1.bindServiceListener( serviceURI_1, new EchoServlet( serviceURI_1, "ID_1" ) );

      Thread.sleep( 5000 );
			
			/*Send a request just to make sure everything is working and that PRIMARY is being consumed*/

      Request request =
          new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr_1, request );

      //DME2Client client = new DME2Client(mgr_1, new URI(clientURI), 30000);
      DME2Payload payload =
          new DME2TextPayload( "Test is a test: testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals()" );
      String response = (String) client.sendAndWait( payload );

      System.out.println( "Response from service: " + response );
      assertTrue( response.contains( "PRIMARY" ) );
			
			/*Shutdown the service with PRIMARY routeOffer*/
      mgr_1.unbindServiceListener( serviceURI_1 );
      Thread.sleep( 5000 );
			
			/*Refresh should have been called as part of of the shutdown and PRIMARY routeOffer 
			 * should have been updated to use the empty cache ttl, checking the cache to validate*/
      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) mgr_1.getEndpointRegistry();
//			Map<String, DME2ServiceEndpointData> cache = registry.getEndpointCache();
//			
//			System.out.println("Cached contents: " + cache.toString());
//			assertTrue(cache.containsKey(serviceURI_1));
//			
//			System.out.println("Cache TTL Value for service with PRIMARY routeoffer (should be 60001): " + cache.get(serviceURI_1).getCacheTTL());
//			assertEquals(60001, cache.get(serviceURI_1).getCacheTTL());
//			
//			Thread.sleep(80000);
//			cache = registry.getEndpointCache();
//			System.out.println("Cache TTL Value for service with PRIMARY routeoffer (should be 60001): " + cache.get(serviceURI_1).getCacheTTL());
//			assertEquals(60001, cache.get(serviceURI_1).getCacheTTL());
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      try {
        mgr_1.unbindServiceListener( serviceURI_1 );
      } catch ( Exception e ) {
      }
    }
  }

  @Test
  public void testGrmRefreshAttemptsCachedEndpoints() throws DME2Exception,
      InterruptedException, UnknownHostException {
    System.err.println( "--- START: testGrmRefreshAttemptsCachedEndpoints" );

	/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
			System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
			System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
			System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/

    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000");
    System.setProperty( "DME2.DEBUG", "true" );

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000" );
    props.put( "DME2_ROUTEINFO_CACHE_TTL_MS", "200" );
    props.put( "DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS", "10000" );

    String service = "com.att.aft.dme2.TestGRMRefreshAttemptsCachedEndpoints";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32675;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String routeOffer1 = "BAU_SE1";
    String serviceName = "service=" + service + "/version=" + version
        + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    String serviceName1 = "service=" + service + "/version=" + version
        + "/envContext=" + envContext + "/routeOffer=" + routeOffer1;

    DME2Manager manager = new DME2Manager( "testGrmRefreshAttemptsCachedEndpoints",
        props );
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );

  }

  // Registry Cache is not exeposed in 3.x as a JMX bean
  @Ignore
  @Test
  public void testGrmRefreshAttemptsCachedRouteInfo() throws DME2Exception,
      InterruptedException, UnknownHostException, URISyntaxException {
    System.err.println( "--- START: testGrmRefreshAttemptsCachedRouteInfo" );

    System.setProperty( "DME2.DEBUG", "true" );
    //System.setProperty("AFT_DME2_GRM_URLS", "http://zldv0432.vci.att.com:9127/GRMLWPService/v1,http://zldv1330.vci.att.com:9127/GRMLWPService/v1");
    System.setProperty( "AFT_DME2_GRM_URLS", "http://hlth451.hydc.sbc.com:9127/GRMLWPService/v1" );

    try {
      Properties props = new Properties();
      props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
      props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
      props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
      props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
      props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000" );
      props.put( "DME2_ROUTEINFO_CACHE_TTL_MS", "200" );
      props.put( "DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS", "10000" );
      props.put( "AFT_ENVIRONMENT", "AFTUAT" );

      String service = "com.att.aft.dme2.TestGrmRefreshAttemptsCachedRouteInfo";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 32675;
      double latitude = 33.3739;
      double longitude = 86.7983;

      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";
      String routeOffer1 = "BAU_SE1";
      String routeOffer2 = "BAU_NE";

      String serviceName = "service=" + service + "/version=" + version
          + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      String serviceName1 = "service=" + service + "/version=" + version
          + "/envContext=" + envContext + "/routeOffer=" + routeOffer1;
      String serviceName2 = "service=" + service + "/version=" + version
          + "/envContext=" + envContext + "/routeOffer=" + routeOffer2;

      DME2Manager manager = new DME2Manager( "testGrmRefreshAttemptsCachedRouteInfo",
          props );
      RouteInfo rtInfo = new RouteInfo();
      rtInfo.setServiceName( service );
      //rtInfo.setServiceVersion("*");
      rtInfo.setEnvContext( envContext );
      RouteGroups rtGrps = new RouteGroups();
      rtInfo.setRouteGroups( rtGrps );

      RouteGroup rg1 = new RouteGroup();
      rg1.setName( "RG1" );
      rg1.getPartner().add( "test1" );
      rg1.getPartner().add( "test2" );
      rg1.getPartner().add( "test3" );

      Route rt1 = new Route();
      rt1.setName( "rt1" );
      RouteOffer ro1 = new RouteOffer();
      ro1.setActive( true );
      ro1.setSequence( 1 );
      ro1.setName( "BAU_SE" );

      RouteOffer ro2 = new RouteOffer();
      ro2.setActive( true );
      ro2.setSequence( 1 );
      ro2.setName( "BAU_SE1" );

      RouteOffer ro3 = new RouteOffer();
      ro3.setActive( true );
      ro3.setSequence( 2 );
      ro3.setName( "BAU_NE" );

      rt1.getRouteOffer().add( ro1 );
      rt1.getRouteOffer().add( ro2 );
      rt1.getRouteOffer().add( ro3 );

      rg1.getRoute().add( rt1 );

      rtGrps.getRouteGroup();
      rtGrps.getRouteGroup().add( rg1 );

      // Ignoreing the above constructed routeInfo, since there is some
      // parsing issue while marshalling the
      // the routeInfo obj to xml

      // run the server in bham.
      String[] bham_1_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
	/*				"-serverPort",
					"4600",*/
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          serviceName,
          "-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
          "-platform", "SANDBOX-DEV" };
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      String[] bham_2_bau_se_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
	/*				"-serverPort",
					"4601",*/
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          serviceName1,
          "-serviceCity", "BHAM", "-serverid", "bham_2_bau_se",
          "-platform", "SANDBOX-DEV" };
      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      bham_2_Launcher.launch();

      String[] bham_2_bau_ne_args = {
          "-serverHost",
          InetAddress.getLocalHost().getCanonicalHostName(),
	/*				"-serverPort",
					"4601",*/
          "-registryType",
          "GRM",
          "-servletClass",
          "EchoServlet",
          "-serviceName",
          serviceName2,
          "-serviceCity", "BHAM", "-serverid", "bham_2_bau_ne",
          "-platform", "SANDBOX-DEV" };
      bham_3_Launcher = new ServerControllerLauncher( bham_2_bau_ne_args );
      bham_3_Launcher.launch();

      try {
        Thread.sleep( 5000 );
      } catch ( Exception ex ) {
      }
      // Save the route info

      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoCacheAttemptsRefreshRouteInfo( DME2Manager.getDefaultInstance().getConfig(), rtInfo, "DEV" );
      try {
        Thread.sleep( 10000 );
      } catch ( Exception ex ) {
      }

      System.err.println( "Service published successfully." );

      Thread.sleep( 1000 );

      String uriStr = "http://DME2SEARCH/service=" + service + "/version=" + version + "/envContext=" + envContext +
          "/partner=test2";
      DME2Client sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      sender.setPayload( "this is a test" );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      sender.setReplyHandler( replyHandler );
      sender.send();

      String reply = null;
      try {
        reply = replyHandler.getResponse( 60000 );
        Map<String, String> rheader = replyHandler.getResponseHeaders();
        String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println( traceStr );
        assertTrue( traceStr.contains( "routeOffer=BAU_SE:onResponseCompleteStatus=200" ) ||
            traceStr.contains( "routeOffer=BAU_SE1:onResponseCompleteStatus=200" ) );
      } catch ( Exception e1 ) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
        assertTrue( e1 == null );
      }
      System.err.println( reply );

      //shutdown one of instance to simulate empty endpoints scenario
      bham_2_Launcher.destroy();
      bham_1_Launcher.destroy();

      DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
      DME2Endpoint[] endpoints = svcRegistry.find( service, version,
          envContext, routeOffer + "~" + routeOffer1 );

      DME2Endpoint found = null;
      for ( DME2Endpoint ep : endpoints ) {
        if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
            && ep.getLongitude() == longitude ) {
          found = ep;
          System.err.println( "Found endpoints after publish " + ep );
        }
      }
      // The registry does not hold endpoints now for shutdown endpoints of primary sequence
      assertNull( found );
      Thread.sleep( 11000 );
      svcRegistry.refresh();
      Thread.sleep( 11000 );

      endpoints = svcRegistry.find( service, version,
          envContext, routeOffer2 );

      found = null;
      for ( DME2Endpoint ep : endpoints ) {
        System.out.println( "ep.getHost()=" + ep.getHost() + ";" + ep.getHost().equals( hostname ) );
        System.out.println( "ep.getLatitude()=" + ep.getLatitude() + "" + ( ep.getLatitude() == latitude ) );
        System.out.println( "ep.getLongitude()=" + ep.getLongitude() + "" + ( ep.getLongitude() == longitude ) );
        if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude ) {
          found = ep;
          System.err.println( "Found endpoints after publish " + ep );
        }
      }
      // endpoints for routeOffer2 found.
      assertNotNull( found );

      uriStr = "http://DME2SEARCH/service=" + service + "/version=" + version + "/envContext=" + envContext +
          "/partner=test2";
      sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      sender.setPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setReplyHandler( replyHandler );
      sender.send();

      reply = null;
      try {
        reply = replyHandler.getResponse( 60000 );
        Map<String, String> rheader = replyHandler.getResponseHeaders();
        String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println( traceStr );
        assertTrue( traceStr.contains( "routeOffer=BAU_NE:onResponseCompleteStatus=200" ) );
      } catch ( Exception e1 ) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
        assertTrue( e1 == null );
      }
      System.err.println( reply );

      DME2Endpoint[] leasedEndpoints = svcRegistry.find( service, version,
          envContext, routeOffer + "~" + routeOffer1 );
      found = null;
      for ( DME2Endpoint ep : endpoints ) {
        if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
            && ep.getLongitude() == longitude ) {
          found = ep;
          System.err.println( "Found endpoints after publish " + ep );
        }
      }
      // The registry should have refreshed endpoints to lose seq 1 endpoints that were shutdown.
      assertNull( found );

      // Check for number of refresh attempts now for endpoint cache
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      try {
        ObjectName name = new ObjectName(
            "com.att.aft.dme2:type=RegistryCache,name=DME2RouteInfoCache-testGrmRefreshAttemptsCachedRouteInfo" );
        MBeanInfo info = server.getMBeanInfo( name );
        Object[] params = new Object[2];
        params[1] = new Integer( Calendar.getInstance().HOUR_OF_DAY );
        params[0] = new String( "/service=" + service + "/version=" + version + "/envContext=" + envContext );
        System.out.println( "Param=" + params[0] );
        String[] signature = { "java.lang.String", "java.lang.Integer" };
        CompositeDataSupport cds = (CompositeDataSupport) server.invoke( name, "getStats", params, signature );
        System.out.println( "count=" + (Long) cds.get( "refreshCount" ) );
        //stats.getHourlyStats(Calendar.getInstance().HOUR_OF_DAY);
        assertTrue( (Long) cds.get( "refreshCount" ) >= 2 );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue( e == null );
      }

      // start back 1 & 2 launcher to add new endpoints
      bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
      bham_1_Launcher.launch();

      bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
      bham_2_Launcher.launch();
      // svcRegistry.

      Thread.sleep( 11000 );
      svcRegistry.refresh();
      Thread.sleep( 11000 );

      // Again primary sequence should be used
      uriStr = "http://DME2SEARCH/service=" + service + "/version=" + version + "/envContext=" + envContext +
          "/partner=test2";
      sender = new DME2Client( manager, new URI( uriStr ), 30000 );
      sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
      sender.setPayload( "this is a test" );
      replyHandler = new EchoReplyHandler();
      sender.setReplyHandler( replyHandler );
      sender.send();

      reply = null;
      try {
        reply = replyHandler.getResponse( 60000 );
        Map<String, String> rheader = replyHandler.getResponseHeaders();
        String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );
        System.out.println( traceStr );
        // request got restored back to primary sequence as refresh should have got endpoints back.
        assertTrue( traceStr.contains( "routeOffer=BAU_SE:onResponseCompleteStatus=200" ) ||
            traceStr.contains( "routeOffer=BAU_SE1:onResponseCompleteStatus=200" ) );
      } catch ( Exception e1 ) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
        assertTrue( e1 == null );
      }
      System.err.println( reply );

      // Check for number of refresh attempts now for endpoint cache after second refresh attempt

      //Check for number of refresh attempts now for routeInfo cache
      try {
        ObjectName name = new ObjectName(
            "com.att.aft.dme2:type=RegistryCache,name=DME2RouteInfoCache-testGrmRefreshAttemptsCachedRouteInfo" );
        MBeanInfo info = server.getMBeanInfo( name );
        Object[] params = new Object[2];
        params[1] = new Integer( Calendar.getInstance().HOUR_OF_DAY );
        params[0] = new String( "/service=" + service + "/version=" + version + "/envContext=" + envContext );
        System.out.println( "Param=" + params[0] );
        String[] signature = { "java.lang.String", "java.lang.Integer" };
        CompositeDataSupport cds = (CompositeDataSupport) server.invoke( name, "getStats", params, signature );
        System.out.println( (Long) cds.get( "refreshCount" ) );
        assertTrue( (Long) cds.get( "refreshCount" ) >= 4 );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue( e == null );
      }

      for ( DME2Endpoint leasedEp : leasedEndpoints ) {
        System.err.println( "Found endpoints after lease " + leasedEp );
        int leasedEpPort = leasedEp.getPort();
        for ( DME2Endpoint ep : endpoints ) {
          int epPort = ep.getPort();
          if ( leasedEpPort == epPort ) {
            long pubEpLease = ep.getLease();
            long leasedEpLease = leasedEp.getLease();

            if ( leasedEpLease <= pubEpLease ) {
              // Endpoints have not refreshed, throw error
              fail( "Leased endpoint for "
                  + leasedEp.getHost()
                  + ":"
                  + leasedEp.getPort()
                  + leasedEp.getPath()
                  + " has expiration time="
                  + leasedEp.getLease()
                  + "which should have been greater than published Ep "
                  + ep.getHost() + ":" + ep.getPort()
                  + ep.getPath() + " expiration time="
                  + ep.getLease() );
            } else {
              System.err.println( "Leased endpoint for "
                  + leasedEp.getHost() + ":" + leasedEp.getPort()
                  + leasedEp.getPath() + " has expiration time="
                  + leasedEp.getLease()
                  + " is greater than published Ep "
                  + ep.getHost() + ":" + ep.getPort()
                  + ep.getPath() + " expiration time="
                  + ep.getLease() );
            }
          }
        }
      }
    } finally {
      try {
        bham_1_Launcher.destroy();
      } catch ( Exception e ) {

      }
      try {
        bham_2_Launcher.destroy();
      } catch ( Exception e ) {

      }
      try {
        bham_3_Launcher.destroy();
      } catch ( Exception e ) {

      }
      System.err.println( "Service unpublished successfully." );
    }
  }

  // Registry Cache is not exeposed in 3.x as a JMX bean
  @Ignore
  @Test
  public void testCacheStatsDisabled() throws DME2Exception,
      InterruptedException, UnknownHostException, IntrospectionException, InstanceNotFoundException,
      ReflectionException, MalformedObjectNameException, MBeanException {
    System.err.println( "--- START: testCacheStatsDisabled" );

	/*		System.setProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000");
			System.setProperty("DME2_SEP_CACHE_TTL_MS", "200");
			System.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS", "200");
			System.setProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200");*/

    // Setting a lower value for renew, so that more frequent expiry updates
    // happen.
    //System.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000");
    System.setProperty( "DME2.DEBUG", "true" );

    Properties props = new Properties();
    props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
    props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
    props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000" );
    props.put( "DME2_PERSIST_CACHED_ROUTEINFO_FREQUENCY_MS", "200" );
    props.put( "DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS", "10000" );
    props.put( "AFT_DME2_DISABLE_CACHE_STATS", "true" );

    String service = "com.att.aft.dme2.TestCacheStatsDisabled";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 32675;
    double latitude = 33.3739;
    double longitude = 86.7983;

    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    String routeOffer1 = "BAU_SE1";
    String serviceName = "service=" + service + "/version=" + version
        + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    String serviceName1 = "service=" + service + "/version=" + version
        + "/envContext=" + envContext + "/routeOffer=" + routeOffer1;

    DME2Manager manager = new DME2Manager( "testCacheStatsDisabled",
        props );
    DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
    svcRegistry.publish( serviceName, null, hostname, port, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName, null, hostname, port + 1, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName, null, hostname, port + 2, latitude,
        longitude, "http" );

    svcRegistry.publish( serviceName1, null, hostname, port + 3, latitude,
        longitude, "http" );
    // we need to publish 3 endpoints with same port, lat, long, but
    // different context
    svcRegistry.publish( serviceName1, null, hostname, port + 4, latitude,
        longitude, "http" );
    svcRegistry.publish( serviceName1, null, hostname, port + 5, latitude,
        longitude, "http" );

    System.err.println( "Service published successfully." );

    Thread.sleep( 1000 );

    DME2Endpoint[] endpoints = svcRegistry.find( service, version,
        envContext, routeOffer + "~" + routeOffer1 );
    DME2RouteInfo rtInfo = svcRegistry.getRouteInfo( service, version,
        envContext );
    DME2Endpoint found = null;
    for ( DME2Endpoint ep : endpoints ) {
      if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
          && ep.getLongitude() == longitude ) {
        found = ep;
        System.err.println( "Found endpoints after publish " + ep );
      }
    }
    // System.err.println("Found registered endpoint: " + found);
    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.refresh();
    Thread.sleep( 11000 );
    DME2Endpoint[] leasedEndpoints = svcRegistry.find( service, version,
        envContext, routeOffer + "~" + routeOffer1 );

    // Check for number of refresh attempts now for endpoint cache
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName name =
        new ObjectName( "com.att.aft.dme2:type=RegistryCache,name=DME2EndpointCacheGRM-testCacheStatsDisabled" );
    MBeanInfo info = server.getMBeanInfo( name );
    Object[] params = new Object[2];
    params[1] = new Integer( Calendar.getInstance().HOUR_OF_DAY );
    params[0] = new String( "/service=" + service + "/version=" + version + "/envContext=" + envContext );
    System.out.println( "Param=" + params[0] );
    String[] signature = { "java.lang.String", "java.lang.Integer" };
    CompositeData cds = (CompositeData) server.invoke( name, "getStats", params, signature );
    assertTrue( cds == null );

    //Check for number of refresh attempts now for routeInfo cache
    name = new ObjectName( "com.att.aft.dme2:type=RegistryCache,name=DME2RouteInfoCacheGRM-testCacheStatsDisabled" );
    //info = server.getMBeanInfo(name);
    params = new Object[2];
    params[1] = new Integer( Calendar.getInstance().HOUR_OF_DAY );
    params[0] = new String( "/service=" + service + "/version=" + version + "/envContext=" + envContext );
    System.out.println( "Param=" + params[0] );
    signature = new String[]{ "java.lang.String", "java.lang.Integer" };
    cds = (CompositeData) server.invoke( name, "getStats", params, signature );
    assertTrue( cds == null );
    // svcRegistry.

    assertNotNull( found );
    Thread.sleep( 11000 );
    svcRegistry.refresh();
    Thread.sleep( 11000 );

    for ( DME2Endpoint leasedEp : leasedEndpoints ) {
      System.err.println( "Found endpoints after lease " + leasedEp );
      int leasedEpPort = leasedEp.getPort();
      for ( DME2Endpoint ep : endpoints ) {
        int epPort = ep.getPort();
        if ( leasedEpPort == epPort ) {
          long pubEpLease = ep.getLease();
          long leasedEpLease = leasedEp.getLease();

          if ( leasedEpLease <= pubEpLease ) {
            // Endpoints have not refreshed, throw error
            fail( "Leased endpoint for "
                + leasedEp.getHost()
                + ":"
                + leasedEp.getPort()
                + leasedEp.getPath()
                + " has expiration time="
                + leasedEp.getLease()
                + "which should have been greater than published Ep "
                + ep.getHost() + ":" + ep.getPort()
                + ep.getPath() + " expiration time="
                + ep.getLease() );
          } else {
            System.err.println( "Leased endpoint for "
                + leasedEp.getHost() + ":" + leasedEp.getPort()
                + leasedEp.getPath() + " has expiration time="
                + leasedEp.getLease()
                + " is greater than published Ep "
                + ep.getHost() + ":" + ep.getPort()
                + ep.getPath() + " expiration time="
                + ep.getLease() );
          }
        }
      }
    }

    svcRegistry.unpublish( serviceName, hostname, port );
    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 1 );
    // Thread.sleep(3000);
    svcRegistry.unpublish( serviceName, hostname, port + 2 );
    System.err.println( "Service unpublished successfully." );
    // svcRegistry.refresh();
    // Thread.sleep(61000);
    // svcRegistry.refresh();
    Thread.sleep( 3000 );

    DME2Endpoint[] ups = svcRegistry.find( service, version, envContext,
        routeOffer );
    found = null;
    for ( DME2Endpoint ep1 : ups ) {
      if ( ep1.getHost().equals( hostname ) && ep1.getPort() == port
          && ep1.getLatitude() == latitude
          && ep1.getLongitude() == longitude ) {
        found = ep1;
        System.err.println( "Found endpoints after unpublish ;" + ep1 );
      }
    }
    System.err.println( "Found registered endpoint - should be null: "
        + found );
	/*		System.clearProperty("DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS");
			System.clearProperty("DME2_SEP_CACHE_TTL_MS");
			System.clearProperty("DME2_SEP_CACHE_EMPTY_TTL_MS");
			System.clearProperty("DME2_SEP_CACHE_ALL_STALE_TTL_MS");*/

  }
}