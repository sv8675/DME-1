/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;

@PrepareForTest({ DME2EndpointRegistryFactory.class })
@PowerMockIgnore("javax.management.*")
@SuppressStaticInitializationFor("com.att.aft.dme2.server.DME2Manager")
@RunWith( PowerMockRunner.class )
public class DME2EndpointRegistryFactoryTest {
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private DME2EndpointRegistryGRM mockEndpointRegistryGRM;
  private DME2Manager mockManager;
  private DME2Configuration mockConfiguration;
  private DME2RouteInfoCacheFS mockEndpointCache;
  private DME2RouteInfoCacheFS mockRouteInfoCache;
  private DME2EndpointRegistryFS mockEndpointRegistryFS;

  @Before
  public void setUpTest() {
    mockEndpointRegistryGRM = mock( DME2EndpointRegistryGRM.class );
    mockManager = mock( DME2Manager.class );
    mockEndpointRegistryFS = mock( DME2EndpointRegistryFS.class );
  }

  @Test
  public void testSingleton() {
    DME2EndpointRegistryFactory factory1 = DME2EndpointRegistryFactory.getInstance();
    DME2EndpointRegistryFactory factory2 = DME2EndpointRegistryFactory.getInstance();
    assertEquals( factory1, factory2 );
  }

  @Test
  public void testCreateFSRegistry() throws Exception {
    // record
    whenNew( DME2EndpointRegistryFS.class ).withArguments( mockConfiguration, DEFAULT_MANAGER_NAME ).thenReturn( mockEndpointRegistryFS );

    // play
    DME2EndpointRegistry endpointRegistryFS = DME2EndpointRegistryFactory.getInstance()
        .createEndpointRegistry( DEFAULT_MANAGER_NAME, mockConfiguration, DME2EndpointRegistryType.FileSystem, DEFAULT_MANAGER_NAME, null );

    // verify
    assertEquals( endpointRegistryFS, mockEndpointRegistryFS );
    verifyNew( DME2EndpointRegistryFS.class ).withArguments( mockConfiguration, DEFAULT_MANAGER_NAME );
  }

  @Test
  public void testCreateGRMRegistryMissingLatitude() throws Exception {
    whenNew( DME2EndpointRegistryGRM.class ).withArguments( mockConfiguration, DEFAULT_MANAGER_NAME ).thenReturn( mockEndpointRegistryGRM );
    Mockito.doNothing().when( mockEndpointRegistryGRM ).init( (java.util.Properties) Mockito.any() );

    DME2EndpointRegistry registry = DME2EndpointRegistryFactory.getInstance().createEndpointRegistry(
        DEFAULT_MANAGER_NAME, mockConfiguration, DME2EndpointRegistryType.GRM, DEFAULT_MANAGER_NAME, null );
    assertNotNull( registry );
    assertTrue( registry instanceof DME2EndpointRegistryGRM );

    verifyNew( DME2EndpointRegistryGRM.class ).withArguments( mockConfiguration, DEFAULT_MANAGER_NAME );
    Mockito.verify( mockEndpointRegistryGRM ).init( (java.util.Properties) Mockito.any() );
  }

  @Test
  public void testNullEndpointRegistry() {
    try {
      DME2EndpointRegistryFactory.getInstance().createEndpointRegistry( DEFAULT_MANAGER_NAME, mockConfiguration, null, DEFAULT_MANAGER_NAME, null );
    } catch ( UnsupportedOperationException e ) {
      return;
    } catch ( DME2Exception e ) {
      fail( e.getMessage() );
    }
    fail( "Should've thrown exception" );
  }
}
