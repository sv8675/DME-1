/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.util.DME2URIUtils;

public class DME2JDBCEndpointTest {
  private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 30 );
  private static final String DEFAULT_VERSION = RandomStringUtils.randomNumeric( 3 );
  private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 15 );
  private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_SERVICE_PATH = DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
  private static final double DEFAULT_DISTANCE = RandomUtils.nextDouble();

  DME2JDBCEndpoint ep;
  @Before
  public void setUpTest() {
    ep = new DME2JDBCEndpoint( DEFAULT_SERVICE_PATH, DEFAULT_DISTANCE );
  }

  @Test
  public void test_ctor() {
    assertNotNull( ep );
    assertEquals( DEFAULT_SERVICE_NAME, ep.getServiceName() );
    assertEquals( DEFAULT_VERSION, ep.getServiceVersion() );
    assertEquals( DEFAULT_ENV_CONTEXT, ep.getEnvContext() );
    assertEquals( DEFAULT_ROUTE_OFFER, ep.getRouteOffer() );
    assertEquals( DEFAULT_DISTANCE, ep.getDistance(), 0.1d );
  }

  @Test
  public void test_database_name() {
    assertNull( ep.getDatabaseName() );
    String databaseName = RandomStringUtils.randomAlphanumeric( 25 );
    ep.setDatabaseName( databaseName );
    assertEquals( databaseName, ep.getDatabaseName() );
  }

  @Test
  public void test_health_check_user() {
    assertNull( ep.getHealthCheckUser() );
    String healthCheckUser = RandomStringUtils.randomAlphanumeric( 25 );
    ep.setHealthCheckUser( healthCheckUser );
    assertEquals( healthCheckUser, ep.getHealthCheckUser() );
  }

  @Test
  public void test_health_check_password() {
    assertNull( ep.getHealthCheckPassword() );
    String healthCheckPassword = RandomStringUtils.randomAlphanumeric( 20 );
    ep.setHealthCheckPassword( healthCheckPassword );
    assertEquals( healthCheckPassword, ep.getHealthCheckPassword() );
  }

  @Test
  public void test_health_check_driver() {
    assertNull( ep.getHealthCheckDriver() );
    String healthCheckDriver = RandomStringUtils.randomAlphanumeric( 15 );
    ep.setHealthCheckDriver( healthCheckDriver );
    assertEquals( healthCheckDriver, ep.getHealthCheckDriver() );
  }
}
