/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.util.DME2URIUtils;

public class DME2FileHandlerTest {
  private static final String DEFAULT_SERVICE_NAME = "sample_filehandler_file";
  private static final String DEFAULT_FILE_NAME = DME2FileHandlerTest.class.getResource( "/DME2FileHandler/" + DEFAULT_SERVICE_NAME + ".txt" ).getFile();
  private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_VERSION = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 7 );
  private static final String DEFAULT_REAL_SERVICE_NAME = DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
  private static final String DEFAULT_DIR_NAME = DEFAULT_FILE_NAME.replace( DEFAULT_SERVICE_NAME + ".txt", "" );
  private static final File DEFAULT_DIR = new File( DEFAULT_DIR_NAME );
  private static final String DEFAULT_PROPERTY_NAME = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_PROPERTY_VALUE = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_PORT = RandomStringUtils.randomNumeric( 3 );
  private static final String DEFAULT_LATITUDE = Double.toString( RandomUtils.nextDouble() );
  private static final String DEFAULT_LONGITUDE = Double.toString( RandomUtils.nextDouble() );
  private static final String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphanumeric( 5 );
  private static final String DEFAULT_CONTEXT_PATH = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_SERVICE_PATH = RandomStringUtils.randomAlphanumeric( 15 );

  @Before
  public void setUpTest() {

  }

  @Test
  public void test_get_last_modified_exists() throws DME2Exception {
    DME2FileHandler fileHandler = new DME2FileHandler( DEFAULT_DIR, DEFAULT_SERVICE_NAME, 0L, 0, 0 );
    assertTrue( fileHandler.getLastModified() > 0L );
  }

  @Test
  public void test_store_properties() throws IOException, DME2Exception {
    File file = new File( DEFAULT_FILE_NAME );
    if ( !file.delete() ) {
      fail( "Couldn't delete " + DEFAULT_FILE_NAME );
    }
    if ( !file.createNewFile() ) {
      fail( "Couldn't create " + DEFAULT_FILE_NAME + " or file already exists" );
    }

    Properties props = new Properties();
    props.setProperty( DEFAULT_PROPERTY_NAME, DEFAULT_PROPERTY_VALUE );

    DME2FileHandler fileHandler = new DME2FileHandler( DEFAULT_DIR, DEFAULT_SERVICE_NAME, 0L, 0, 0 );
    Properties actualProperties = fileHandler.readProperties();
    assertEquals( actualProperties.size(), 0 );

    fileHandler.storeProperties( props, false );

    actualProperties = fileHandler.readProperties();
    assertNotNull( actualProperties );
    assertEquals( actualProperties.size(), 1 );
    assertEquals( actualProperties.getProperty( DEFAULT_PROPERTY_NAME ), DEFAULT_PROPERTY_VALUE );
    System.out.println( DEFAULT_PROPERTY_NAME + " : " + actualProperties.getProperty( DEFAULT_PROPERTY_NAME ) );
  }

  @Test
  public void test_read_endpoints() throws IOException, DME2Exception {
    File file = new File( DEFAULT_DIR_NAME + DEFAULT_REAL_SERVICE_NAME + ".txt" );
    file.delete();

    Properties props = new Properties();
    String propsKey = DEFAULT_HOST + "," + DEFAULT_PORT;
    props.setProperty( propsKey, "latitude=" + DEFAULT_LATITUDE + ";longitude=" + DEFAULT_LONGITUDE + ";lease="
        + System.currentTimeMillis() + ";protocol=" + DEFAULT_PROTOCOL + ";contextPath=" +
        ( DEFAULT_CONTEXT_PATH == null ? DEFAULT_SERVICE_PATH : DEFAULT_CONTEXT_PATH ) + ";routeOffer=" + DEFAULT_ROUTE_OFFER );

    try {
      DME2FileHandler fileHandler = new DME2FileHandler( DEFAULT_DIR, DEFAULT_REAL_SERVICE_NAME, 1000000L, 0, 0 );
      fileHandler.storeProperties( props, false );

      List<DME2Endpoint> endpoints = fileHandler.readEndpoints();
      assertNotNull( endpoints );
      assertEquals( endpoints.size(), 1 );

      DME2Endpoint endpoint = endpoints.get( 0 );
      assertNotNull( endpoint );
      assertEquals( endpoint.getEnvContext(), DEFAULT_ENV_CONTEXT );
      assertEquals( endpoint.getServiceVersion(), DEFAULT_VERSION );
      assertEquals( endpoint.getServiceName(), DEFAULT_REAL_SERVICE_NAME );
      assertEquals( endpoint.getContextPath(), DEFAULT_CONTEXT_PATH );
      assertEquals( endpoint.getHost(), DEFAULT_HOST );
      assertEquals( endpoint.getLatitude(), Double.valueOf( DEFAULT_LATITUDE ), 0.001 );
      assertEquals( endpoint.getLongitude(), Double.valueOf( DEFAULT_LONGITUDE ), 0.001 );
      assertEquals( endpoint.getPath(), DEFAULT_CONTEXT_PATH );
      assertEquals( (int) endpoint.getPort(), (int) Integer.valueOf( DEFAULT_PORT ) );
      assertEquals( endpoint.getProtocol(), DEFAULT_PROTOCOL );
      assertEquals( endpoint.getRouteOffer(), DEFAULT_ROUTE_OFFER );
    } finally {
      if ( !file.delete() ) {
        fail( "Couldn't delete " + DEFAULT_FILE_NAME );
      }
    }
  }
}

