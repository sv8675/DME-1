/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.HashSet;
import java.util.List;
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
import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.service.DME2CacheManager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;
import com.att.aft.dme2.manager.registry.util.DME2ServiceEndpointDataTestUtil;


@PrepareForTest({DME2CacheFactory.class, DME2AbstractRegistryCache.class, DME2EndpointCacheFS.class })
@SuppressStaticInitializationFor("com.att.aft.dme2.server.DME2Manager")
@PowerMockIgnore( "javax.management.*" )
@RunWith( PowerMockRunner.class )
public class DME2EndpointCacheFSTest {
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_CACHE_NAME =
      DME2Endpoint.class.getName() + "_" + DME2EndpointRegistryType.FileSystem + "_" + DEFAULT_MANAGER_NAME;
  private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 15 );
  private static final CacheElement.Key DEFAULT_CACHE_ELEMENT_KEY_SERVICE =
      new CacheElement.Key<String>( DEFAULT_SERVICE );
  private static final DME2ServiceEndpointData DEFAULT_SERVICE_ENDPOINT_DATA =
      DME2ServiceEndpointDataTestUtil.createDefaultServiceEndpointData();
  private static final CacheElement.Value DEFAULT_CACHE_ELEMENT_VALUE = new CacheElement.Value();
  private static final CacheElement DEFAULT_CACHE_ELEMENT =
      new CacheElement(  );
  private static final String DEFAULT_EXCEPTION_CODE = RandomStringUtils.randomAlphanumeric( 7 );
  private static final String DEFAULT_EXCEPTION_MSG = RandomStringUtils.randomAlphanumeric( 30 );
  private static final DME2Exception DEFAULT_FETCH_ENDPOINTS_EXCEPTION =
      new DME2Exception( DEFAULT_EXCEPTION_CODE, DEFAULT_EXCEPTION_MSG );

  static {

  }
  private DME2EndpointRegistryFS mockRegistry;
  private DME2Manager mockManager;
  private DME2CacheManager mockCacheManager;
  private AbstractCache mockCache;
  private DME2Configuration mockConfig;

  @Before
  public void setUpTest() {
    mockRegistry = mock( DME2EndpointRegistryFS.class );
    mockManager = mock( DME2Manager.class );
    mockStatic( DME2CacheFactory.class );
    mockCacheManager = mock( DME2CacheManager.class );
    mockCache = mock( AbstractCache.class );
    mockConfig = mock( DME2Configuration.class );
  }

  @Test
  public void test_ctor() throws DME2Exception {
    // record
    // DME2AbstractRegistryCache ctor

    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( DEFAULT_CACHE_NAME )).thenReturn( null );
    when( mockCacheManager.createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() )).thenReturn( mockCache );

    // play

    DME2EndpointCacheFS endpointCache = new DME2EndpointCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
    assertNotNull( endpointCache );

    // verify
    // DME2AbstractRegistryCache ctor
    //verify( mockRegistry ).getManager();
    // verify( mockManager ).getName();
    verifyStatic( times(2) );
    DME2CacheFactory.getCacheManager(mockConfig);
    verify( mockCacheManager ).getCache( DEFAULT_CACHE_NAME );
    verify( mockCacheManager ).createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() );
  }

  @Test
  public void test_getEndpoints() throws Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( DEFAULT_CACHE_NAME )).thenReturn( null );
    when( mockCacheManager.createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() )).thenReturn( mockCache );
    // getEndpoints / get
    whenNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE ).thenReturn( DEFAULT_CACHE_ELEMENT_KEY_SERVICE );
    when( mockCache.get( DEFAULT_CACHE_ELEMENT_KEY_SERVICE )).thenReturn( new CacheElement.Value<DME2ServiceEndpointData>( DEFAULT_SERVICE_ENDPOINT_DATA) );

    // play

    DME2EndpointCacheFS endpointCache = new DME2EndpointCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
    assertNotNull( endpointCache );
    List<DME2Endpoint> endpoints = endpointCache.getEndpoints( DEFAULT_SERVICE );
    assertEquals( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(), endpoints );

    // verify
    // DME2AbstractRegistryCache ctor
    //verify( mockRegistry ).getManager();
    // verify( mockManager ).getName();
    verifyStatic( times( 2 ) );
    DME2CacheFactory.getCacheManager(mockConfig);
    verify( mockCacheManager ).getCache( DEFAULT_CACHE_NAME );
    verify( mockCacheManager ).createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() );
    // getEndpoints / get
    verifyNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE );
    verify( mockCache ).get( DEFAULT_CACHE_ELEMENT_KEY_SERVICE );
  }

  @Test
  public void test_getEndpoints_null() throws Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( DEFAULT_CACHE_NAME )).thenReturn( null );
    when( mockCacheManager.createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() )).thenReturn( mockCache );
    // getEndpoints / get
    whenNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE ).thenReturn( DEFAULT_CACHE_ELEMENT_KEY_SERVICE );
    when( mockCache.get( DEFAULT_CACHE_ELEMENT_KEY_SERVICE )).thenReturn( null );

    // play

    DME2EndpointCacheFS endpointCache = new DME2EndpointCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
    assertNotNull( endpointCache );
    List<DME2Endpoint> endpoints = endpointCache.getEndpoints( DEFAULT_SERVICE );
    assertEquals( endpoints.size(), 0 );

    // verify
    // DME2AbstractRegistryCache ctor
    //verify( mockRegistry ).getManager();
    // verify( mockManager ).getName();
    verifyStatic( times( 2 ) );
    DME2CacheFactory.getCacheManager(mockConfig);
    verify( mockCacheManager ).getCache( DEFAULT_CACHE_NAME );
    verify( mockCacheManager ).createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() );
    // getEndpoints / get
    verifyNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE );
    verify( mockCache ).get( DEFAULT_CACHE_ELEMENT_KEY_SERVICE );
  }

  public void test_put() throws Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( DEFAULT_CACHE_NAME )).thenReturn( null );
    when( mockCacheManager.createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() )).thenReturn( mockCache );
    // putEndpoints
    whenNew( DME2ServiceEndpointData.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(), DEFAULT_SERVICE, 0L, 0L ).thenReturn( DEFAULT_SERVICE_ENDPOINT_DATA );
    // DME2AbstractRegistryCache.put
    whenNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE ).thenReturn( DEFAULT_CACHE_ELEMENT_KEY_SERVICE );
    whenNew( CacheElement.Value.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA ).thenReturn(
        DEFAULT_CACHE_ELEMENT_VALUE );
    doNothing().when( mockCache ).put( DEFAULT_CACHE_ELEMENT_KEY_SERVICE, DEFAULT_CACHE_ELEMENT_VALUE );

    // play

    DME2EndpointCacheFS endpointCache = new DME2EndpointCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
    assertNotNull( endpointCache );
    endpointCache.putEndpoints( DEFAULT_SERVICE, DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList() );

    // verify
    // DME2AbstractRegistryCache ctor
    //verify( mockRegistry ).getManager();
    // verify( mockManager ).getName();
    verifyStatic( times( 2 ) );
    DME2CacheFactory.getCacheManager(mockConfig);
    verify( mockCacheManager ).getCache( DEFAULT_CACHE_NAME );
    verify( mockCacheManager ).createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() );
    // putEndpoints
    verifyNew( DME2ServiceEndpointData.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(),
        DEFAULT_SERVICE, 0L, 0L );
    // DME2AbstractRegistryCache.put
    verifyNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE );
    verifyNew( CacheElement.Value.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA );
    verify( mockCache ).put( DEFAULT_CACHE_ELEMENT_KEY_SERVICE, DEFAULT_CACHE_ELEMENT_VALUE );
  }

  // TODO: Fix this at some point by making the createKey/Value/Element methods into a util
  @Test
  @Ignore
  public void test_fetch_from_source() throws Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( DEFAULT_CACHE_NAME )).thenReturn( null );
    when( mockCacheManager.createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() )).thenReturn( mockCache );
    // DME2EndpointRegistryFS.fetchEndpointsFromSource
    when( mockRegistry.fetchEndpointsFromSource( DEFAULT_SERVICE )).thenReturn( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList() );
    when( mockCache.createElement( any(), any() )).thenReturn( new CacheElement( ));
    // fetchFromSource
//    whenNew( DME2ServiceEndpointData.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(), DEFAULT_SERVICE, 0L, 0L ).thenReturn( DEFAULT_SERVICE_ENDPOINT_DATA );
    // DME2AbstractRegistryCache.createCacheValue
//    whenNew( CacheElement.Value.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA ).thenReturn( DEFAULT_CACHE_ELEMENT_VALUE );

    // play

    DME2EndpointCacheFS endpointCache = new DME2EndpointCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
    assertNotNull( endpointCache );
    CacheElement cacheElement = endpointCache.fetchFromSource( DEFAULT_CACHE_ELEMENT_KEY_SERVICE );
    assertNotNull( cacheElement );
    assertNotNull( cacheElement.getValue( ) );
    assertNotNull( cacheElement.getValue( ) );
    assertTrue( (cacheElement.getValue().getValue() instanceof DME2ServiceEndpointData ));
    assertEquals( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(), ((DME2ServiceEndpointData) cacheElement.getValue().getValue()).getEndpointList() );
    //assertEquals( cacheElement, DEFAULT_CACHE_ELEMENT );
    //assertEquals( cacheElementValue.getValue(), DEFAULT_CACHE_ELEMENT.getValue() );

    // verify
    // DME2AbstractRegistryCache ctor
   /* verify( mockRegistry ).getManager();
    // verify( mockManager ).getName();
    verifyStatic( times( 2 ) );
    DME2CacheFactory.getCacheManager();
    verify( mockCacheManager ).getCache( DEFAULT_CACHE_NAME );
    verify( mockCacheManager ).createCache( DEFAULT_CACHE_NAME, "Endpoint" );*/
    // DME2EndpointRegistryFS.fetchEndpointsFromSource
    verify( mockRegistry ).fetchEndpointsFromSource( DEFAULT_SERVICE );
    // fetchFromSource
    verifyNew( DME2ServiceEndpointData.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(),
        DEFAULT_SERVICE, 0L, 0L );
    // DME2AbstractRegistryCache.createCacheValue
    verifyNew( CacheElement.Value.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA );

  }

  // TODO: Fix this at some point by making the createKey/Value/Element methods into a util
  @Test
  @Ignore
  public void test_fetch_from_source_using_set() throws Exception {
    // record
    // DME2AbstractRegistryCache ctor
    //when( mockRegistry.getManager() ).thenReturn( mockManager );
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
    when( mockCacheManager.getCache( DEFAULT_CACHE_NAME )).thenReturn( null );
    when( mockCacheManager.createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() )).thenReturn( mockCache );
    // DME2EndpointRegistryFS.fetchEndpointsFromSource
    when( mockRegistry.fetchEndpointsFromSource( DEFAULT_SERVICE )).thenReturn( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList() );
    // fetchFromSource
    whenNew( DME2ServiceEndpointData.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(), DEFAULT_SERVICE, 0L, 0L ).thenReturn( DEFAULT_SERVICE_ENDPOINT_DATA );
    // DME2AbstractRegistryCache.createCacheValue
    whenNew( CacheElement.Value.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA ).thenReturn(
        DEFAULT_CACHE_ELEMENT_VALUE );
    // DME2EndpointRegistryFS.fetchEndpointsFromSource
    when( mockRegistry.fetchEndpointsFromSource( DEFAULT_SERVICE + "a" ) ).thenThrow(
        DEFAULT_FETCH_ENDPOINTS_EXCEPTION );


    // play

    DME2EndpointCacheFS endpointCache = new DME2EndpointCacheFS( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
    assertNotNull( endpointCache );
    Set<CacheElement.Key<String>> serviceSet = new HashSet<CacheElement.Key<String>>();
    serviceSet.add( new CacheElement.Key<String>( DEFAULT_SERVICE ));
    serviceSet.add( new CacheElement.Key<String>( DEFAULT_SERVICE + "a" ));
    Map<CacheElement.Key<String>, Pair<CacheElement, Exception>> fetchedMap = endpointCache.fetchFromSource( serviceSet );
    assertNotNull( fetchedMap );
    assertEquals( fetchedMap.size(), 2 );
    for ( Map.Entry<CacheElement.Key<String>,Pair<CacheElement, Exception>> entry : fetchedMap.entrySet() ) {
      String key = entry.getKey().getKey();
      CacheElement value = entry.getValue().getLeft();
      Exception exception = entry.getValue().getRight();

      if ( DEFAULT_SERVICE.equals( key ) ) {
        assertEquals( value.getValue().getValue(),  DEFAULT_SERVICE_ENDPOINT_DATA );
        assertNull( exception );
      } else if ( (DEFAULT_SERVICE + "a").equals( key )) {
        assertNull( value );
        assertEquals( exception, DEFAULT_FETCH_ENDPOINTS_EXCEPTION );
      } else {
        fail( "Received unknown key in map: " + key );
      }
    }


    // verify
    // DME2AbstractRegistryCache ctor
    //verify( mockRegistry ).getManager();
    // verify( mockManager ).getName();
    verifyStatic( times( 2 ) );
    DME2CacheFactory.getCacheManager(mockConfig);
    verify( mockCacheManager ).getCache( DEFAULT_CACHE_NAME );
    verify( mockCacheManager ).createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
        (com.att.aft.dme2.cache.service.DME2CacheableCallback) any() );
    // DME2EndpointRegistryFS.fetchEndpointsFromSource
    verify( mockRegistry ).fetchEndpointsFromSource( DEFAULT_SERVICE );
    // fetchFromSource
    verifyNew( DME2ServiceEndpointData.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA.getEndpointList(), DEFAULT_SERVICE, 0L, 0L );
    // DME2AbstractRegistryCache.createCacheValue
    verifyNew( CacheElement.Value.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA );
    // DME2EndpointRegistryFS.fetchEndpointsFromSource
    verify( mockRegistry ).fetchEndpointsFromSource( DEFAULT_SERVICE + "a" );

  }
}

