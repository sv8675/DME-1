/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.util.DME2Constants;
import com.att.scld.grm.types.v1.ClientJVMInstance;

@SuppressStaticInitializationFor({"com.att.aft.dme2.manager.registry.DME2AbstractEndpointRegistry"})
public class DME2AbstractEndpointRegistryTest {

  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_ROUTEOFFER_STALENESS = RandomStringUtils.randomNumeric(
      RandomUtils.nextInt( 5 ) + 1 );
  private DME2Configuration mockConfiguration;

  @Before
  public void setUpTest() {
  	// mockConfiguration = new DME2Configuration( DEFAULT_MANAGER_NAME );
    mockConfiguration = mock( DME2Configuration.class );
  }

  @Test
  public void test_ctor() throws Exception {
    SimpleEndpointRegistry registry = new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertNotNull( registry );
    assertEquals( DEFAULT_MANAGER_NAME,
        DME2UnitTestUtil.getPrivate( DME2AbstractEndpointRegistry.class.getDeclaredField( "managerName" ), registry ) );
    assertEquals( mockConfiguration,
        DME2UnitTestUtil.getPrivate( DME2AbstractEndpointRegistry.class.getDeclaredField( "config" ), registry ) );
  }

  // Test registry constructor where lat is not found in the config
  @Test
  public void test_ctor_null_lat() throws Exception {
    // record
    DME2UnitTestUtil.setFinalStatic( DME2AbstractEndpointRegistry.class.getDeclaredField( "clientLatitude" ), null, null );
    when( mockConfiguration.getDouble( DME2Constants.AFT_LATITUDE ) ).thenReturn( null );

    // play
    try {
      new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    } catch ( DME2Exception e ) {
      return;
    }
    fail( "Should've thrown an exception" );
  }

  // Test registry constructor where long is not found in the config
  @Test
  public void test_ctor_null_long() throws Exception {
    DME2UnitTestUtil.setFinalStatic( DME2AbstractEndpointRegistry.class.getDeclaredField( "clientLongitude" ), null, null );
    // record
    when( mockConfiguration.getDouble( DME2Constants.AFT_LATITUDE )).thenReturn( RandomUtils.nextDouble() );
    when( mockConfiguration.getDouble( DME2Constants.AFT_LONGITUDE )).thenReturn( null );

    // play
    try {
      new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    } catch ( DME2Exception e ) {
      return;
    }
    fail( "Should've thrown an exception" );
  }

  // Tests setting distance bands when no exclusion is present
  @Test
   public void test_ctor_bands_no_exclusion( ) throws DME2Exception {
    when( mockConfiguration.getProperty( DME2Constants.DME2_ENDPOINT_BANDS )).thenReturn( "1,100,1000" );
    when( mockConfiguration.getBoolean( DME2Constants.DME2_ENDPOINT_BANDS_EXCLUDE_OUT_OF_BAND )).thenReturn( false );

    SimpleEndpointRegistry registry = new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertNotNull( registry );
    double[] distanceBands = registry.getDistanceBands();
    assertNotNull( distanceBands );
    assertEquals( 4, distanceBands.length );
    assertEquals( 1d, distanceBands[0], 0.01 );
    assertEquals( 100d, distanceBands[1], 0.01 );
    assertEquals( 1000d, distanceBands[2], 0.01 );
    assertEquals( DME2AbstractEndpointRegistry.CALCULATED_DISTANCE_MAX, distanceBands[3], 0.01 );
  }

  @Test
  public void test_ctor_bands_with_exclusion( ) throws DME2Exception {
    when( mockConfiguration.getProperty( DME2Constants.DME2_ENDPOINT_BANDS )).thenReturn( "1,100,1000" );
    when( mockConfiguration.getBoolean( DME2Constants.DME2_ENDPOINT_BANDS_EXCLUDE_OUT_OF_BAND )).thenReturn( true );

    SimpleEndpointRegistry registry = new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertNotNull( registry );
    double[] distanceBands = registry.getDistanceBands();
    assertNotNull( distanceBands );
    assertEquals( 3, distanceBands.length );
    assertEquals( 1d, distanceBands[0], 0.01 );
    assertEquals( 100d, distanceBands[1], 0.01 );
    assertEquals( 1000d, distanceBands[2], 0.01 );
  }

  @Test
  public void test_ctor_bands_empty( ) throws DME2Exception {
    when( mockConfiguration.getProperty( DME2Constants.DME2_ENDPOINT_BANDS )).thenReturn( null );
    when( mockConfiguration.getBoolean( DME2Constants.DME2_ENDPOINT_BANDS_EXCLUDE_OUT_OF_BAND )).thenReturn( true );

    SimpleEndpointRegistry registry = new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertNotNull( registry );
    double[] distanceBands = registry.getDistanceBands();
    assertNotNull( distanceBands );
    assertEquals( 4, distanceBands.length );
    assertEquals( 0.1d, distanceBands[0], 0.01 );
    assertEquals( 500.0d, distanceBands[1], 0.01 );
    assertEquals( 5000.0d, distanceBands[2], 0.01 );
    assertEquals( DME2AbstractEndpointRegistry.CALCULATED_DISTANCE_MAX, distanceBands[3], 0.01 );
  }

  @Test
  public void test_ctor_null_manager_name() {
    try {
      new SimpleEndpointRegistry( mockConfiguration, null );
    } catch ( DME2Exception e ) {
      return;
    }
    fail( "Should've thrown an exception" );
  }

  @Test
  public void test_ctor_empty_manager_name() {
    try {
      new SimpleEndpointRegistry( mockConfiguration, "" );
    } catch ( DME2Exception e ) {
      return;
    }
    fail( "Should've thrown an exception" );
  }

  @Test
  public void test_init() throws Exception {
    System.setProperty( DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, DEFAULT_ROUTEOFFER_STALENESS);
    DME2UnitTestUtil.setFinalStatic(  DME2AbstractEndpointRegistry.class.getDeclaredField( "ROUTEOFFER_STALE_PERIOD_IN_MS" ), null, null );
    SimpleEndpointRegistry registry = new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertNull( DME2UnitTestUtil.getPrivate( DME2AbstractEndpointRegistry.class.getDeclaredField( "ROUTEOFFER_STALE_PERIOD_IN_MS" ), null ));
    DME2UnitTestUtil.executePrivateVoid( DME2AbstractEndpointRegistry.class.getDeclaredMethod( "staticInit" ), null  );
    assertEquals( (Long)(Long.valueOf( DEFAULT_ROUTEOFFER_STALENESS )*60000), (Long) DME2UnitTestUtil.getPrivate( DME2AbstractEndpointRegistry.class.getDeclaredField( "ROUTEOFFER_STALE_PERIOD_IN_MS" ), null ) );
  }

  @Test
  public void test_init_stale_period_parse_exception() throws Exception {
    System.setProperty( DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN, "NotAValue");
    DME2UnitTestUtil.setFinalStatic(  DME2AbstractEndpointRegistry.class.getDeclaredField( "ROUTEOFFER_STALE_PERIOD_IN_MS" ), null, null );
    SimpleEndpointRegistry registry = new SimpleEndpointRegistry( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertNull( DME2UnitTestUtil.getPrivate( DME2AbstractEndpointRegistry.class.getDeclaredField( "ROUTEOFFER_STALE_PERIOD_IN_MS" ), null ));
    DME2UnitTestUtil.executePrivateVoid( DME2AbstractEndpointRegistry.class.getDeclaredMethod( "staticInit" ), null  );
    assertEquals( (Long)(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN_DEFAULT *60000), (Long) DME2UnitTestUtil.getPrivate( DME2AbstractEndpointRegistry.class.getDeclaredField( "ROUTEOFFER_STALE_PERIOD_IN_MS" ), null ) );
  }

  class SimpleEndpointRegistry extends DME2AbstractEndpointRegistry {
    Logger logger = LoggerFactory.getLogger( SimpleEndpointRegistry.class );

    public SimpleEndpointRegistry( DME2Configuration configuration, String managerName ) throws DME2Exception {
      super( configuration, managerName );
    }

    @Override
    public void publish( String service, String path, String host, int port, double latitude, double longitude,
                         String protocol ) throws DME2Exception {

    }

    @Override
    public void publish( String service, String path, String host, int port, double latitude, double longitude,
                         String protocol, boolean updateLease ) throws DME2Exception {

    }

    @Override
    public void publish( String service, String path, String host, int port, String protocol, Properties props )
        throws DME2Exception {

    }

    @Override
    public void publish( String service, String path, String host, int port, String protocol ) throws DME2Exception {

    }

    @Override
    public void publish( String service, String path, String host, int port, String protocol, boolean updateLease )
        throws DME2Exception {

    }

    @Override
    public void unpublish( String serviceName, String host, int port ) throws DME2Exception {

    }

    @Override
    public List<DME2Endpoint> findEndpoints( String serviceName, String serviceVersion, String envContext,
                                             String routeOffer ) throws DME2Exception {
      return null;
    }

    @Override
    public DME2RouteInfo getRouteInfo( String serviceName, String serviceVersion, String envContext )
        throws DME2Exception {
      return null;
    }

    @Override
    public void lease( DME2Endpoint endpoint ) throws DME2Exception {

    }

    @Override
    public void refresh() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void publish( String serviceURI, String contextPath, String hostAddress, int port, double latitude,
                         double longitude, String protocol, Properties props, boolean updateLease )
        throws DME2Exception {

    }
    
    @Override
    public DME2Endpoint[] find(String serviceKey, String version, String env,
    		String routeOffer) throws DME2Exception {
    	return null;
     }

    @Override
    public void registerJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {

    }

    @Override
    public void updateJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {

    }

    @Override
    public void deregisterJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {

    }

    @Override
    public List<ClientJVMInstance> findRegisteredJVM( String envContext, Boolean activeOnly, String hostAddress,
                                                      String mechID, String processID ) throws DME2Exception {
      return null;
    }

    public void setStaleEndpointCache( DME2StaleCache cache ) {
      staleEndpointCache = cache;
    }

    public void setStaleRouteOfferCache( DME2StaleCache cache ) {
      staleRouteOfferCache = cache;
    }
  }
}

