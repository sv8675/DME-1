/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
@PrepareForTest( DME2CacheFactory.class )
@PowerMockIgnore( "javax.management.*" )
@SuppressStaticInitializationFor( "com.att.aft.dme2.server.DME2Manager" )
public class DME2RouteInfoCacheFSTest {
  private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 10 );
  private static final CacheElement.Key<String> DEFAULT_SERVICE_CACHE_KEY = new CacheElement.Key<String>( DEFAULT_SERVICE );
  private static final Exception DEFAULT_EXCEPTION = new DME2Exception( RandomStringUtils.randomAlphanumeric( 7 ), RandomStringUtils.randomAlphanumeric( 30 ));
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 10 );
  private DME2EndpointRegistryFS mockRegistry;
  private DME2Manager mockManager;
  private DME2CacheManager mockCacheManager;
  private DME2Cache mockCache;
  private CacheElement mockElement;
  private DME2RouteInfo mockRouteInfo; // It's hard to make a default Route Info, so just mock it
  private DME2Configuration mockConfig;
  
  @Before
  public void setUpTest() {
    mockRegistry = mock( DME2EndpointRegistryFS.class );
    mockManager = mock( DME2Manager.class );
    mockStatic( DME2CacheFactory.class );
    mockCacheManager = mock( DME2CacheManager.class );
    mockCache = mock( DME2Cache.class );
    mockRouteInfo = mock( DME2RouteInfo.class );
    mockConfig = mock( DME2Configuration.class );
    mockElement = mock( CacheElement.class );
  }

  @Test
  public void test_ctor() throws DME2Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( anyString() )).thenReturn( mockCache );

    // play

    DME2RouteInfoCacheFS routeInfoCache = new DME2RouteInfoCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME );
    assertNotNull( routeInfoCache );

    // verify
    // Nothing to verify as it's all AbstractCache code
  }

  // TODO: Re-enable once create cacheKEy/Element/Value are moved to a utility
  @Test
  @Ignore
  public void test_fetch_from_source() throws DME2Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.etManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( anyString() )).thenReturn( mockCache );
    // DME2RouteInfoCacheFS.fetchFromSource
    when( mockRegistry.fetchRouteInfoFromSource( anyString() )).thenReturn( mockRouteInfo );


    // play

    DME2RouteInfoCacheFS routeInfoCache = new DME2RouteInfoCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME );
    assertNotNull( routeInfoCache );
    CacheElement routeInfoElement = routeInfoCache.fetchFromSource( DEFAULT_SERVICE_CACHE_KEY );
    assertEquals( routeInfoElement.getValue().getValue(), mockRouteInfo );

    // verify
    verify( mockRegistry ).fetchRouteInfoFromSource( DEFAULT_SERVICE );
  }

  // TODO: Re-enable once create cacheKEy/Element/Value are moved to a utility
  @Test
  @Ignore
  public void test_fetch_from_source_using_set() throws DME2Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( anyString() )).thenReturn( mockCache );
    // DME2RouteInfoCache.fetchFromSource
    when( mockRegistry.fetchRouteInfoFromSource( DEFAULT_SERVICE )).thenReturn( mockRouteInfo );
    when( mockRegistry.fetchRouteInfoFromSource( DEFAULT_SERVICE + "a" )).thenThrow( DEFAULT_EXCEPTION );

    // play

    DME2RouteInfoCacheFS routeInfoCache = new DME2RouteInfoCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME );
    assertNotNull( routeInfoCache );
    Set<CacheElement.Key<String>> serviceSet = new HashSet<CacheElement.Key<String>>();
    CacheElement.Key<String> badServiceKey = new CacheElement.Key<String>( DEFAULT_SERVICE + "a" );
    serviceSet.add( DEFAULT_SERVICE_CACHE_KEY );
    serviceSet.add( badServiceKey );
    Map<CacheElement.Key<String>, Pair<CacheElement, Exception>> routeInfoMap = routeInfoCache.fetchFromSource( serviceSet );
    assertNotNull( routeInfoMap );
    assertEquals( routeInfoMap.size(), 2 );

    for ( Map.Entry<CacheElement.Key<String>,Pair<CacheElement,Exception>> entry : routeInfoMap.entrySet() ) {
      String key = entry.getKey().getKey();
      CacheElement value = entry.getValue().getLeft();
      Exception exception = entry.getValue().getRight();

      if ( DEFAULT_SERVICE.equals( key ) ) {
        assertEquals( value.getValue(), mockRouteInfo );
        assertNull( exception );
      } else if ( (DEFAULT_SERVICE + "a").equals( key )) {
        assertNull( value );
        assertEquals( exception, DEFAULT_EXCEPTION );
      } else {
        fail( "Unknown key " + key );
      }
    }

    // verify
    verify( mockRegistry ).fetchRouteInfoFromSource( DEFAULT_SERVICE );
    verify( mockRegistry ).fetchRouteInfoFromSource( DEFAULT_SERVICE + "a" );
  }
}

