/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.request.DmeUniformResource;

public class DME2EndpointTest {

  private static final double DEFAULT_DISTANCE = RandomUtils.nextDouble();
  private static final String DEFAULT_SIMPLE_NAME = RandomStringUtils.randomAlphanumeric( 25 );
  private static final Boolean DEFAULT_CACHED = RandomUtils.nextInt( 2 ) == 0;
  private static final String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 10 );
  private static final int DEFAULT_PORT = RandomUtils.nextInt();
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 15 );
  private static final String DEFAULT_SERVICE_VERSION = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 10 );

  DME2Endpoint ep;

  @Before
  public void setUpTest() {
    ep = new DME2Endpoint( DEFAULT_DISTANCE );
  }

  @Test
  public void test_simple_name() {
    assertNull( ep.getSimpleName() );
    ep.setSimpleName( DEFAULT_SIMPLE_NAME );
    assertEquals( DEFAULT_SIMPLE_NAME, ep.getSimpleName() );
  }

  @Test
  public void test_get_cached() {
    assertNull( ep.getCached() );
    ep.setCached( DEFAULT_CACHED );
    assertEquals( DEFAULT_CACHED, ep.getCached() );
  }

  @Test
  public void test_to_url_string() {
    ep.setProtocol( null );
    ep.setPath( "/ABCDEF" );
    ep.setHost( DEFAULT_HOST );
    ep.setPort( DEFAULT_PORT );
    assertEquals( "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/ABCDEF", ep.toURLString() );
  }

  @Test
  public void test_to_url_string_blah() {
    ep.setProtocol( "blah" );
    ep.setPath( "ABCDEF" );
    ep.setHost( DEFAULT_HOST );
    ep.setPort( DEFAULT_PORT );
    assertEquals( "blah://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/ABCDEF", ep.toURLString() );
  }

  @Test
  public void test_dme2_version() {
    String dme2Version = RandomStringUtils.randomNumeric( 5 );
    assertNull( ep.getDME2Version() );
    ep.setDME2Version( dme2Version );
    assertEquals( dme2Version, ep.getDME2Version() );
  }

  @Test
  public void test_getserviceendpointid_direct() {
    String toString = RandomStringUtils.randomAlphanumeric( 30 );
    DmeUniformResource uniformResource = mock( DmeUniformResource.class );
    when( uniformResource.getUrlType() ).thenReturn( DmeUniformResource.DmeUrlType.DIRECT );
    when( uniformResource.toString() ).thenReturn( toString );

    ep.setDmeUniformResource( uniformResource );
    assertEquals( toString, ep.getServiceEndpointID() );
  }

  @Test
  public void test_getserviceendpointid_null() {
    ep.setSimpleName( DEFAULT_SIMPLE_NAME );
    ep.setServiceVersion( DEFAULT_SERVICE_VERSION );
    ep.setPort( DEFAULT_PORT );
    ep.setHost( DEFAULT_HOST );
    ep.setEnvContext( DEFAULT_ENV_CONTEXT );
    ep.setServiceName( "/envContext=" + DEFAULT_ENV_CONTEXT + "/" );

    assertEquals( DEFAULT_SIMPLE_NAME + ":" + DEFAULT_SERVICE_VERSION + ":" + DEFAULT_PORT + "|" + DEFAULT_HOST + "-" +
        DEFAULT_ENV_CONTEXT, ep.getServiceEndpointID() );
  }

  @Test
  public void test_to_url_string_overload() {
    ep.setProtocol( null );
    ep.setHost( DEFAULT_HOST );
    ep.setPort( DEFAULT_PORT );
    String context = "/" + RandomStringUtils.randomAlphanumeric( 10 ) + "?";
    String extraContext = "/" + RandomStringUtils.randomAlphanumeric( 10 );
    String queryString = "?" + RandomStringUtils.randomAlphanumeric( 10 );

    String expectedUrl = "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT + context.substring( 0, context.length()-1 ) + extraContext + queryString;
    assertEquals( expectedUrl, ep.toURLString( context, extraContext, queryString ));
  }

  @Test
  public void test_to_url_string_overload_2() {
    String protocol = RandomStringUtils.randomAlphabetic( 4 );
    ep.setProtocol( protocol );
    ep.setHost( DEFAULT_HOST );
    ep.setPort( DEFAULT_PORT );
    String context = RandomStringUtils.randomAlphanumeric( 10 ) + "/";
    String extraContext = "/" + RandomStringUtils.randomAlphanumeric( 10 );
    String queryString =  RandomStringUtils.randomAlphanumeric( 10 );

    String expectedUrl = protocol + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/" + context + extraContext.substring( 1 ) + "?" + queryString;
    assertEquals( expectedUrl, ep.toURLString( context, extraContext, queryString ));
  }

  @Test
  public void test_to_url_string_overload_3() {
    String protocol = RandomStringUtils.randomAlphabetic( 4 );
    ep.setProtocol( protocol );
    ep.setHost( DEFAULT_HOST );
    ep.setPort( DEFAULT_PORT );
    String path = "/" + RandomStringUtils.randomAlphanumeric( 10 );
    ep.setPath( path );
    String context = null;
    String extraContext = "?" + RandomStringUtils.randomAlphanumeric( 10 );
    String queryString =  RandomStringUtils.randomAlphanumeric( 10 );

    String expectedUrl = protocol + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + path + "/" + extraContext + "?" + queryString.substring(1);
    assertEquals( expectedUrl, ep.toURLString( context, extraContext, queryString ));
  }

  @Test
  public void test_compare_null() {
    assertEquals( -1, ep.compareTo( null ));
  }

  @Test
  public void test_compare_greater() {
    DME2Endpoint ep2 = new DME2Endpoint( DEFAULT_DISTANCE + 1 );
    assertEquals( -1, ep.compareTo( ep2 ));
    assertEquals( 1, ep2.compareTo( ep ));
  }

  @Test
  public void test_compare_less() {
    DME2Endpoint ep2 = new DME2Endpoint( DEFAULT_DISTANCE -1 );
    assertEquals( 1, ep.compareTo( ep2 ));
    assertEquals( -1, ep2.compareTo( ep ));
  }

  @Test
  public void test_compare_equal() {
    DME2Endpoint ep2 = new DME2Endpoint( DEFAULT_DISTANCE );
    assertEquals( 0, ep.compareTo( ep2 ));
    assertEquals( 0, ep2.compareTo( ep ));
  }
}