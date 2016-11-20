/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;

public class DME2FileHandlerMultiFileTest {
  private static final String BASE_DIR = System.getProperty( "user.dir" ) + "/src/test/resources";
  private static final File BASE_FILE = new File( BASE_DIR );

  // We'll create (and try to retrieve) endpoints with these service paths
  private static final List<String> ENDPOINT_FILE_NAMES = Arrays.asList(
      "/service=MyService/version=1.0/envContext=DEV",
      "/service=MyService/version=1.0.1/envContext=DEV",
      "/service=MyService/version=11.0/envContext=DEV",
      "/service=MyService/version=1.0/envContext=NON-PROD"
  );

  // Map of service path to properties
  private static final Map<String,Map<String,String>> propsMap = new HashMap<String,Map<String,String>>();

  // Map of service path to expected service paths (what the hierarchical lookup will return to us)
  private static final Map<String,List<String>> expectedTiesMap = new HashMap<String,List<String>>();

  @BeforeClass
  public static void setUp() {
     // Create files if they do not exist
    for ( String s : ENDPOINT_FILE_NAMES ) {
      File f = new File( BASE_DIR + s + ".txt");
      if ( !f.getParentFile().exists() && !f.getParentFile().mkdirs() ) {
        fail( "Unable to create directories for " + f.getAbsolutePath() );
      }
      try {
        if ( !f.exists() && !f.createNewFile() ) {
          fail( "Unable to create file " + f.getAbsolutePath() );
        }
      } catch ( IOException e ) {
        e.printStackTrace();
        fail( "Unable to create file " + f.getAbsolutePath() );
      }
      Map<String,String> properties = new HashMap<String,String>();
      properties.put( "host", RandomStringUtils.randomAlphanumeric( 20 ));
      properties.put( "port", Integer.toString( RandomUtils.nextInt( 1000 ) ));
      properties.put( "latitude", Double.toString( RandomUtils.nextDouble() ));
      properties.put( "longitude", Double.toString( RandomUtils.nextDouble() ));
      properties.put( "protocol", RandomStringUtils.randomAlphanumeric( 5 ));
      properties.put( "contextPath", RandomStringUtils.randomAlphanumeric( 20 ));
      properties.put( "routeOffer", RandomStringUtils.randomAlphanumeric( 10 ) );

      propsMap.put( s, properties );
    }

    expectedTiesMap.put( ENDPOINT_FILE_NAMES.get(0), Arrays.asList( ENDPOINT_FILE_NAMES.get(0), ENDPOINT_FILE_NAMES.get(1) ));
    expectedTiesMap.put( ENDPOINT_FILE_NAMES.get(1), Arrays.asList( ENDPOINT_FILE_NAMES.get(1)));
    expectedTiesMap.put( ENDPOINT_FILE_NAMES.get(2), Arrays.asList( ENDPOINT_FILE_NAMES.get(2)));
    expectedTiesMap.put( ENDPOINT_FILE_NAMES.get(3), Arrays.asList( ENDPOINT_FILE_NAMES.get(3)));
  }

  @AfterClass
  public static void tearDown() {
    for ( String s : ENDPOINT_FILE_NAMES ) {
      File f = new File( BASE_DIR + s + ".txt" );
      if ( f.exists() && !f.delete() ) {
        System.err.println( "Unable to delete " + f.getAbsolutePath() );
      }
    }
  }

  @Test
  public void test_store_and_retrieve() throws DME2Exception {
    // First store

    for ( String s : ENDPOINT_FILE_NAMES ) {
      DME2FileHandler fileHandler = new DME2FileHandler( BASE_FILE, s, System.currentTimeMillis(), 0, 0 );
      fileHandler.storeProperties( mapToProperties( propsMap.get(s) ), false );
    }

    // Then retrieve and validate

    for ( String s : ENDPOINT_FILE_NAMES ) {
      DME2FileHandler fileHandler = new DME2FileHandler( BASE_FILE, s, System.currentTimeMillis(), 0, 0 );
      Properties actualProperties = fileHandler.readProperties();
      for ( String t : expectedTiesMap.get( s ) ) {
        Properties expectedProperties = mapToProperties( propsMap.get( t ) );
        for ( String key : expectedProperties.stringPropertyNames() ) {
          // We need to ignore the lease time, since it's based on currentTimeMillis (so they will always be different)
          String errMsg = "actual service: " + s + " expected ties: " + t + " expected key: " + key + " actual keys: " + actualProperties.stringPropertyNames().toString();
          String actualProperty = actualProperties.getProperty( key );
          String expectedProperty = expectedProperties.getProperty( key );
          assertNotNull( errMsg, actualProperty );
          assertNotNull( errMsg, expectedProperty );
          String[] actualProps = actualProperty.split( ";" );
          String[] expectedProps = expectedProperty.split( ";" );
          errMsg = "actual: " + actualProperty + " expected: " + expectedProperty;
          assertEquals( errMsg, expectedProps.length, actualProps.length );

          for ( int i = 0; i < actualProps.length; i++ ) {
            if ( !actualProps[i].startsWith( "lease=" ) ) {
              assertEquals( errMsg, expectedProps[i], actualProps[i] );
            }
          }
        }
      }
    }
  }

  private Properties mapToProperties( Map<String,String> props ) {
    Properties properties = new Properties(  );
    String propsKey = props.get( "host" ) + "," + props.get( "port" );
    properties.setProperty( propsKey, "latitude=" + props.get("latitude") + ";longitude=" + props.get("longitude") + ";lease="
        + System.currentTimeMillis() + ";protocol=" + props.get("protocol") + ";contextPath=" + props.get("contextPath") + ";routeOffer=" + props.get("routeOffer"));
    return properties;
  }
}
