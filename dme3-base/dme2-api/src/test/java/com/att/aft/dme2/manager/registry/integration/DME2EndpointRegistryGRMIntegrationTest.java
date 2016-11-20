/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry.integration;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.server.test.TestConstants;

@Ignore
public class DME2EndpointRegistryGRMIntegrationTest {
  private static final String DEFAULT_VERSION = "1.0.0";
  private static final String DEFAULT_HOST = "TestHost";
  private static final String DEFAULT_SERVICE = "/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
  private static final String DEFAULT_PATH = "/some/random/path";
  private static final int DEFAULT_PORT = 12345;
  private static final String DEFAULT_SERVICE_NAME = "com.att.test.TestService-2";
  private static final String DEFAULT_ROUTE_OFFER = "DEFAULT";
  private static final double DEFAULT_LATITUDE = 1.11;
  private static final double DEFAULT_LONGITUDE = -2.22;
  private static final String DEFAULT_PROTOCOL = "http";
  private static final String DEFAULT_ENV_CONTEXT = "LAB";
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 10 );

  @BeforeClass
  public static void setUp() {
    /*System.setProperty( "dme2_api_config", DME2EndpointRegistryGRMIntegrationTest.class.getResource(
        "/dme-api_defaultConfigs.properties" ).getFile());*/
    System.setProperty( "AFT_LATITUDE", Long.toString( RandomUtils.nextLong() ) );
    System.setProperty( "AFT_LONGITUDE", Long.toString( RandomUtils.nextLong() ) );
    System.setProperty( "AFT_ENVIRONMENT", "AFTUAT");
    System.setProperty("DME2.DEBUG", "true");
    System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
    System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
    System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
    System.setProperty("AFT_LATITUDE", "33.373900");
    System.setProperty("AFT_LONGITUDE", "-86.798300");
    System.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
  }

  @Test
  public void testPublish() throws DME2Exception, MalformedURLException {
    String managerName = RandomStringUtils.randomAlphanumeric( 30 );
    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( new DME2Configuration( managerName ), managerName );
    endpointRegistry.init( null );

    // publish
    endpointRegistry.publish( DEFAULT_SERVICE, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_PROTOCOL );

    // find
    List<DME2Endpoint> endpoints = endpointRegistry.findEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
    Assert.assertNotNull( endpoints );
    Assert.assertTrue( endpoints.size() > 0 );

    // unpublish
    endpointRegistry.unpublish( DEFAULT_SERVICE, DEFAULT_HOST, DEFAULT_PORT );

    // find
    endpoints = endpointRegistry.findEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
    Assert.assertNotNull( endpoints );
    Assert.assertEquals( 0, endpoints.size() );
  }

  @Test
@Ignore 
  public void testGetRouteInfo() throws DME2Exception {
    String managerName = RandomStringUtils.randomAlphanumeric( 30 );
    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( new DME2Configuration( managerName ), managerName );
    endpointRegistry.init( null );

    // publish
    endpointRegistry.publish( DEFAULT_SERVICE, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_PROTOCOL );

    // get route info
    DME2RouteInfo routeInfo = endpointRegistry.getRouteInfo( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT );
    Assert.assertNotNull( routeInfo );
    Assert.assertEquals( DEFAULT_SERVICE_NAME, routeInfo.getServiceName() );
    Assert.assertEquals( DEFAULT_VERSION, routeInfo.getServiceVersion() );
    Assert.assertEquals( DEFAULT_ENV_CONTEXT, routeInfo.getEnvContext() );
  }
}
