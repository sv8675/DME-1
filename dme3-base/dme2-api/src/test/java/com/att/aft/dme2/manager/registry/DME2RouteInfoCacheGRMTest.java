/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheManager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;

@RunWith( PowerMockRunner.class )
@PrepareForTest(DME2CacheFactory.class)
@PowerMockIgnore( "javax.management.*" )
@SuppressStaticInitializationFor( "com.att.aft.dme2.server.DME2Manager" )
public class DME2RouteInfoCacheGRMTest {
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 10 );

  private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_VERSION = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_SERVICE = "/service=" + DEFAULT_SERVICE_NAME + "/version=" + DEFAULT_VERSION + "/envContext=" + DEFAULT_ENV_CONTEXT;
  private static final CacheElement.Key<String> DEFAULT_SERVICE_CACHE_KEY =
      new CacheElement.Key<String>( DEFAULT_SERVICE );

  private DME2EndpointRegistryGRM mockRegistry;
  private DME2Manager mockManager;
  private DME2CacheManager mockCacheManager;
  private DME2Cache mockCache;
  private DME2RouteInfo mockRouteInfo;
  private DME2Configuration mockConfig;
  
  @Before
  public void setUpTest() {
    mockRegistry = mock( DME2EndpointRegistryGRM.class );
    mockManager = mock( DME2Manager.class );
    mockStatic( DME2CacheFactory.class );
    mockCacheManager = mock( DME2CacheManager.class );
    mockCache = mock( DME2Cache.class );
    mockRouteInfo = mock( DME2RouteInfo.class );
    mockConfig = mock( DME2Configuration.class );
  }

  // TODO: Re-enable once create cache key/element/value moves to a util class
  @Test
  @Ignore
  public void test_fetch_from_source() throws DME2Exception {
// record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( anyString() )).thenReturn( mockCache );
    // DME2RouteInfoCacheFS.fetchFromSource
    when( mockRegistry.fetchRouteInfo( anyString(), anyString(), anyString() )).thenReturn( mockRouteInfo );

    // play

    DME2RouteInfoCacheGRM routeInfoCache = new DME2RouteInfoCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME );
    assertNotNull( routeInfoCache );
    CacheElement cacheElement = routeInfoCache.fetchFromSource( DEFAULT_SERVICE_CACHE_KEY );
    assertEquals( cacheElement.getValue().getValue(), mockRouteInfo );

    // verify
    verify( mockRegistry ).fetchRouteInfo( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT );
  }
}

