package com.att.aft.dme2.manager.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.service.CacheEntryView;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheManager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;
import com.att.aft.dme2.manager.registry.util.DME2EndpointTestUtil;
import com.att.aft.dme2.manager.registry.util.DME2ServiceEndpointDataTestUtil;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.DME2Utils;

@PrepareForTest({ DME2EndpointCacheGRM.class, DME2CacheFactory.class, DME2URIUtils.class })
@SuppressStaticInitializationFor({ "com.att.aft.dme2.server.DME2Manager" })
@PowerMockIgnore( "javax.management.*" )
@RunWith( PowerMockRunner.class )
public class DME2EndpointCacheGRMTest  {
	private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
	private static final String DEFAULT_CACHE_NAME =
			DME2Endpoint.class.getName() + "_" + DME2EndpointRegistryType.GRM + "_" + DEFAULT_MANAGER_NAME;
	private static final String DEFAULT_RUNNING_INSTANCE_NAME = RandomStringUtils.randomAlphanumeric( 15 );
	private static final Long DEFAULT_LAST_PERSISTED = RandomUtils.nextLong();
	private static final DME2ServiceEndpointData DEFAULT_SERVICE_ENDPOINT_DATA =
			DME2ServiceEndpointDataTestUtil.createDefaultServiceEndpointData();
	private static final List<DME2ServiceEndpointData> DEFAULT_SERVICE_ENDPOINT_DATA_LIST =
			Arrays.asList( DEFAULT_SERVICE_ENDPOINT_DATA );
	private static final CacheElement.Key DEFAULT_CACHE_KEY_SERVICE_ENDPOINT_DATA_SERVICE_URI =
			new CacheElement.Key<String>( DEFAULT_SERVICE_ENDPOINT_DATA.getServiceURI() );
	private static final CacheElement.Value DEFAULT_CACHE_VALUE_SERVICE_ENDPOINT_DATA =
			new CacheElement.Value<DME2ServiceEndpointData>( DEFAULT_SERVICE_ENDPOINT_DATA );
	private static final String DEFAULT_CACHED_ENDPOINTS_FILE =
			System.getProperty( "user.home" ) + File.separator + ".aft" + File.separator + File.separator +
			DEFAULT_RUNNING_INSTANCE_NAME + File.separator + ".cached-endpoints.ser";
	private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 30 );
	private static final CacheElement.Key<String> DEFAULT_SERVICE_CACHE_KEY = new CacheElement.Key<String>( DEFAULT_SERVICE );
	private static final List<DME2Endpoint> DEFAULT_ENDPOINT_LIST = DME2EndpointTestUtil.createDefaultDME2EndpointList();
	private static final String DEFAULT_VERSION = RandomStringUtils.randomAlphanumeric( 10 );
	private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 10 );
	private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 15 );
	private static final CacheElement.Value<DME2ServiceEndpointData> DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT =
			new CacheElement.Value<DME2ServiceEndpointData>( DEFAULT_SERVICE_ENDPOINT_DATA );
	private static final Set<CacheElement.Key> DEFAULT_CACHE_KEY_SET = new HashSet<CacheElement.Key>();
	private static final String DEFAULT_CACHE_KEY_SET_VALUE = RandomStringUtils.randomAlphabetic( 10 );
	private static final CacheElement DEFAULT_CACHE_ELEMENT = new CacheElement();


	static {
		DEFAULT_CACHE_KEY_SET.add( new CacheElement.Key<String>(DEFAULT_CACHE_KEY_SET_VALUE ));
	}

	private static final long DEFAULT_ENDPOINT_CACHE_TTL = RandomUtils.nextLong();
	private static final String DEFAULT_PATH = RandomStringUtils.randomAlphanumeric( 20 );
	private static final CacheElement.Key<String> DEFAULT_PATH_CACHE_KEY = new CacheElement.Key<String>( DEFAULT_PATH );
	// Super confusing name... but it's basically the CacheElement.Key version of the String element in the set that's
	// returned from cache.getKeySet();
	private static final CacheElement.Key DEFAULT_CACHE_KEY_SET_VALUE_AS_KEY = new CacheElement.Key<String>( DEFAULT_CACHE_KEY_SET_VALUE );
	private static final Integer DEFAULT_SEP_CACHE_TTL_MS = RandomUtils.nextInt();

	private DME2EndpointRegistryGRM mockRegistry;
	private DME2Manager mockManager;
	private DME2CacheManager mockCacheManager;
	private DME2Cache mockCache;
	private DME2Configuration mockConfig;
	private File mockFile;
	private File mockCachedEndpointsFile;
	private ObjectMapper mockObjectMapper;
	private DmeUniformResource mockUniformResource;
	private DME2Configuration mockConstantsConfig;
	private CacheEntryView mockEntryView;

	@Before
	public void setUpTest() {
		mockRegistry = mock( DME2EndpointRegistryGRM.class );
		mockManager = mock( DME2Manager.class );
		mockCacheManager = mock( DME2CacheManager.class );
		mockCache = mock( AbstractCache.class );
		mockStatic( DME2CacheFactory.class );
		mockStatic( DME2Manager.class );
		mockConfig = mock( DME2Configuration.class );
		mockFile = mock( File.class );
		mockCachedEndpointsFile = mock( File.class );
		mockObjectMapper = mock( ObjectMapper.class );
		mockUniformResource = mock( DmeUniformResource.class );
		mockConstantsConfig = mock( DME2Configuration.class );
		mockEntryView = mock( CacheEntryView.class );
	}

	@Test
	public void test_ctor_removepersistedendpointsonstartup() throws Exception {
		System.setProperty( DME2Constants.DME2_REMOVE_PERSISTENT_CACHE_ON_STARTUP, "true" );

		// record

		mockInit();
		whenNew( File.class ).withArguments( anyString() ).thenReturn( mockFile );
		when( mockFile.exists() ).thenReturn( true );
		when( mockFile.delete() ).thenReturn( true );

		// play

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry,DEFAULT_MANAGER_NAME, false );

		// verify

		verifyInit();
		//    verifyNew( File.class ).withArguments( anyString() );
		//    verify( mockFile ).exists();
		//    verify( mockFile ).delete();
	}

	@Test
	public void test_ctor_removepersistedendpointsonstartup_exception() throws Exception {
		System.setProperty( DME2Constants.DME2_REMOVE_PERSISTENT_CACHE_ON_STARTUP, "true" );

		// record

		mockInit();
		whenNew( File.class ).withArguments( anyString() ).thenThrow( new Exception( "Whatever" ));

		// play

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );

		// verify

		verifyInit();
		//    verifyNew( File.class ).withArguments( anyString() );
	}

	@Test
	public void test_ctor_null_instancename() throws DME2Exception {
		// record

		mockInit();

		// play

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );

		// verify

		verifyInit();
	}

	@Test
	public void test_ctor_cachedEndpointsFile_doesnt_exist() throws Exception {
		// record

		mockInitCtor();
		mockInitConfig();
		when( DME2Utils.getRunningInstanceName(mockConfig) ).thenReturn( DEFAULT_RUNNING_INSTANCE_NAME );
		whenNew( File.class ).withArguments( DEFAULT_CACHED_ENDPOINTS_FILE ).thenReturn( mockCachedEndpointsFile );
		when( mockCachedEndpointsFile.exists() ).thenReturn( false );

		// play

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );

		// verify

		verifyInitCtor();
		verifyInitConfig();
		verifyStatic();
		//    DME2Manager.getRunningInstanceName();
		//    verifyNew( File.class ).withArguments( DEFAULT_CACHED_ENDPOINTS_FILE );
		//    verify( mockCachedEndpointsFile ).exists();
	}

	@Test
	public void test_ctor() throws Exception {
		// record

		mockInitCtor();
		mockInitConfig();
		when( DME2Utils.getRunningInstanceName(mockConfig) ).thenReturn( DEFAULT_RUNNING_INSTANCE_NAME );
		whenNew( File.class ).withArguments( DEFAULT_CACHED_ENDPOINTS_FILE ).thenReturn( mockCachedEndpointsFile );
		when( mockCachedEndpointsFile.exists() ).thenReturn( true );
		when( mockCachedEndpointsFile.lastModified() ).thenReturn( DEFAULT_LAST_PERSISTED );
		whenNew( ObjectMapper.class ).withNoArguments().thenReturn( mockObjectMapper );

		when( mockObjectMapper
				.readValue( eq( mockFile ), (TypeReference<List<DME2ServiceEndpointData>>) any() ) ).thenReturn( DEFAULT_SERVICE_ENDPOINT_DATA_LIST );
		whenNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA.getServiceURI() ).thenReturn( DEFAULT_CACHE_KEY_SERVICE_ENDPOINT_DATA_SERVICE_URI );
		whenNew( CacheElement.Value.class ).withArguments( DEFAULT_SERVICE_ENDPOINT_DATA ).thenReturn( DEFAULT_CACHE_VALUE_SERVICE_ENDPOINT_DATA );
		doNothing().when( mockCache ).put( DEFAULT_CACHE_KEY_SERVICE_ENDPOINT_DATA_SERVICE_URI, DEFAULT_CACHE_VALUE_SERVICE_ENDPOINT_DATA );

		//    replayAll();
		// play

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );

		// verify
		//    verifyAll();
		verifyInitCtor();
		verifyInitConfig();
		verifyStatic();
		//    DME2Manager.getRunningInstanceName();
		//    verifyNew( File.class, times(2) ).withArguments( DEFAULT_CACHED_ENDPOINTS_FILE );
		//    verify( mockCachedEndpointsFile ).exists();
	}

	@Test
	public void test_put() throws Exception {
		mockInit();
		whenNew( CacheElement.Key.class ).withAnyArguments().thenReturn( DEFAULT_SERVICE_CACHE_KEY );
		whenNew( CacheElement.Value.class ).withAnyArguments().thenReturn( DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT );
		whenNew( CacheElement.class ).withAnyArguments().thenReturn( DEFAULT_CACHE_ELEMENT );
		when( ((AbstractCache) mockCache).createElement( any(), any() )).thenReturn( DEFAULT_CACHE_ELEMENT );
		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		endpointCache.put( DEFAULT_SERVICE, DEFAULT_SERVICE_ENDPOINT_DATA );

		verifyInit();
		verify( (AbstractCache) mockCache ).put( DEFAULT_SERVICE_CACHE_KEY, DEFAULT_CACHE_ELEMENT );
	}

	@Test
	public void test_removeFromCache() throws Exception {
		mockInit();
		whenNew( CacheElement.Key.class ).withAnyArguments().thenReturn( DEFAULT_SERVICE_CACHE_KEY );

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		endpointCache.remove( DEFAULT_SERVICE );

		verifyInit();
		verify( mockCache ).remove( DEFAULT_SERVICE_CACHE_KEY );
	}

	// Test is irrelevant now that cache uses the cacheholder refresh
	@Test
	@Ignore
	public void test_refreshCachedEndpoint() throws Exception {
		mockInit();
		whenNew( DmeUniformResource.class ).withParameterTypes( DME2Configuration.class, URI.class ).withArguments( eq(mockConfig), (URI) anyObject() ).thenReturn(
				mockUniformResource );
		when( mockUniformResource.getService() ).thenReturn( DEFAULT_SERVICE );
		when( mockUniformResource.getVersion() ).thenReturn( DEFAULT_VERSION );
		when( mockUniformResource.getEnvContext() ).thenReturn( DEFAULT_ENV_CONTEXT );
		when( mockUniformResource.getRouteOffer() ).thenReturn( DEFAULT_ROUTE_OFFER );
		when( mockCache.getEntryView() ).thenReturn( mockEntryView );
		when( mockEntryView.getEntry( any() )).thenReturn( DEFAULT_CACHE_ELEMENT );
		when( mockRegistry.fetchEndpoints( eq( DEFAULT_SERVICE ), eq( DEFAULT_VERSION ), eq( DEFAULT_ENV_CONTEXT ), eq( DEFAULT_ROUTE_OFFER ), anyString() )).thenReturn( DEFAULT_ENDPOINT_LIST );

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		List<DME2Endpoint> endpointList = endpointCache.refreshCachedEndpoint( DEFAULT_SERVICE );
		assertEquals( DEFAULT_ENDPOINT_LIST, endpointList );

		verify( mockCache ).put( anyObject(), anyObject() );


	}

	/*
  @Test
  public void test_refreshCachedEndpoint_URI_Exception() throws Exception {
    mockInit();
    whenNew( DmeUniformResource.class ).withParameterTypes( URI.class ).withArguments( anyObject() ).thenThrow( new Exception( "Hi! I'm an exception!" ));

    DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockRegistry );
    try {
      endpointCache.refreshCachedEndpoint( DEFAULT_SERVICE );
    } catch ( DME2Exception e ) {
      Assert.assertEquals( e.getCause().getMessage(), "Hi! I'm an exception!" );
      return;
    }
    fail( "Should've thrown an exception" );
  }
	 */

	@Test
	public void test_containsKey_false() throws DME2Exception {
		mockInit();
		when( ((AbstractCache)mockCache).getKeySet() ).thenReturn( DEFAULT_CACHE_KEY_SET );

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		assertFalse( endpointCache.containsKey( DEFAULT_SERVICE ) );

		verifyInit();
	}

	@Test
	public void test_containsKey_true() throws DME2Exception {
		mockInit();
		when( ((AbstractCache)mockCache).get( anyObject() ) ).thenReturn( DEFAULT_CACHE_VALUE_SERVICE_ENDPOINT_DATA );

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		assertTrue( endpointCache.containsKey( DEFAULT_CACHE_KEY_SET_VALUE ) );

		verifyInit();
	}

	/*@Test
  public void test_setEndpointCacheTTL() throws Exception {
    mockInit();
    DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockRegistry );
    Assert.assertEquals(
        DME2UnitTestUtil.getPrivate( DME2EndpointCacheGRM.class.getDeclaredField( "endpointCacheTTL" ), endpointCache ),
        0L );
    endpointCache.setEndpointCacheTTL( DEFAULT_ENDPOINT_CACHE_TTL );
    Assert.assertEquals( DME2UnitTestUtil.getPrivate( DME2EndpointCacheGRM.class.getDeclaredField( "endpointCacheTTL" ), endpointCache ), DEFAULT_ENDPOINT_CACHE_TTL );
    verifyInit();
  }*/

	@Test
	public void test_getCurrentSize() throws DME2Exception {
		mockInit();
		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		endpointCache.getCurrentSize();
		verifyInit();
		verify( (AbstractCache) mockCache ).getCurrentSize();
	}

	@Test
	public void test_clear() throws DME2Exception {
		mockInit();
		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		endpointCache.clear();
		verifyInit();
		verify( mockCache ).clear();
	}

	@Test
	public void test_template() throws DME2Exception {
		mockInit();
		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		verifyInit();
	}

	@Test
	public void test_getEndpoints() throws Exception {
		// Hijack the DME2Constants' config
		//DME2UnitTestUtil.setFinalStatic( DME2Constants.class.getDeclaredField( "config" ), mockConstantsConfig );

		mockInit();
		whenNew( DmeUniformResource.class ).withParameterTypes( DME2Configuration.class, URI.class ).withArguments( eq(mockConfig), (URI) anyObject() ).thenReturn(
				mockUniformResource );
		// Add the ROUTE_OFFER_SEP so we hit the if statement in getEndpoints
		when( mockUniformResource.getRouteOffer() ).thenReturn( DEFAULT_ROUTE_OFFER + DME2Constants.DME2_ROUTE_OFFER_SEP );
		when( mockUniformResource.getPath() ).thenReturn( DEFAULT_PATH );
		whenNew( CacheElement.Key.class ).withArguments( DEFAULT_PATH ).thenReturn( DEFAULT_PATH_CACHE_KEY );
		when( mockCache.get( DEFAULT_PATH_CACHE_KEY ) ).thenReturn( DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT );
		when( mockUniformResource.getService() ).thenReturn( DEFAULT_SERVICE );
		when( mockUniformResource.getVersion() ).thenReturn( DEFAULT_VERSION );
		when( mockUniformResource.getEnvContext() ).thenReturn( DEFAULT_ENV_CONTEXT );
		mockStatic( DME2URIUtils.class );
		when( DME2URIUtils.buildServiceURIString( eq( DEFAULT_SERVICE ), eq( DEFAULT_VERSION ), eq( DEFAULT_ENV_CONTEXT ),
				anyString() )).thenReturn( DEFAULT_SERVICE );
		whenNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE ).thenReturn( DEFAULT_SERVICE_CACHE_KEY );
		when( mockCache.get( DEFAULT_SERVICE_CACHE_KEY )).thenReturn( DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT );
		when( mockConstantsConfig.getInt("AFT_DME2_FAST_CACHE_EP_ELIGIBLE_COUNT") ).thenReturn( DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT.getValue().getEndpointList().size() );
		when( mockManager.isUrlInStaleList( DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT.getValue().getEndpointList().get(0).toURLString() )).thenReturn( false );

		DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockConfig, mockRegistry, DEFAULT_MANAGER_NAME, false );
		List<DME2Endpoint> endpoints = endpointCache.getEndpoints( DEFAULT_SERVICE );
		assertEquals( endpoints, DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT.getValue().getEndpointList() );

		verifyNew( DmeUniformResource.class ).withArguments( eq(mockConfig), (URI) anyObject() );
		verify( mockUniformResource ).getRouteOffer();
		verify( mockUniformResource ).getPath();
		verifyNew( CacheElement.Key.class ).withArguments( DEFAULT_PATH );
		verify( mockCache ).get( DEFAULT_PATH_CACHE_KEY );
		verify( mockUniformResource ).getService();
		verify( mockUniformResource ).getVersion();
		verify( mockUniformResource ).getEnvContext();
		verifyStatic();
		DME2URIUtils.buildServiceURIString( eq( DEFAULT_SERVICE ), eq( DEFAULT_VERSION ), eq( DEFAULT_ENV_CONTEXT ),
				anyString() );
		verifyNew( CacheElement.Key.class ).withArguments( DEFAULT_SERVICE );
		verify( mockCache ).get( DEFAULT_SERVICE_CACHE_KEY );
	}
	/*

  @Test
  public void test_persistCachedEndpoints() throws Exception {
    File mockParentFile = mock(File.class);

    System.setProperty( DME2Constants.DME2_REMOVE_PERSISTENT_CACHE_ON_STARTUP, "false" );
    System.setProperty( DME2Constants.DME2_DISABLE_PERSISTENT_CACHE, "false" );
    System.setProperty( DME2Constants.DME2_CACHED_ENDPOINTS_FILE, DEFAULT_CACHED_ENDPOINTS_FILE );

    mockInit();
    when( ((AbstractCache) mockCache).getCurrentSize() ).thenReturn( 1 );
    whenNew( File.class ).withAnyArguments().thenReturn( mockFile );
    when( mockFile.exists() ).thenReturn( false );
    when( mockFile.getParentFile() ).thenReturn( mockParentFile );
    when( mockParentFile.exists() ).thenReturn( false );
    whenNew( ObjectMapper.class ).withNoArguments().thenReturn( mockObjectMapper );
    when( ((AbstractCache) mockCache).getKeySet() ).thenReturn( DEFAULT_CACHE_KEY_SET );
    when( mockCache.get( DEFAULT_CACHE_KEY_SET_VALUE_AS_KEY )).thenReturn( DEFAULT_SERVICE_ENDPOINT_CACHE_ELEMENT );

    DME2EndpointCacheGRM endpointCache = new DME2EndpointCacheGRM( mockRegistry );
    endpointCache.persistCachedEndpoints( false );

    verifyInit();
	 */
	/*    verifyNew( File.class, times(3) ).withArguments( anyString() );
    verify( mockFile, times(2) ).exists();
    verify( mockParentFile ).exists();
    verify( mockParentFile ).mkdirs();
    verify( mockFile ).createNewFile();
    verifyNew( ObjectMapper.class ).withNoArguments();
    verify( ((AbstractCache) mockCache), times(2) ).getKeySet();
    verify( mockCache, atLeastOnce() ).get( DEFAULT_CACHE_KEY_SET_VALUE_AS_KEY );
    verify( mockObjectMapper ).writeValue( eq(mockFile), anyObject() );*//*

  }
     */

	private void verifyInit() throws DME2Exception {
		verifyInitCtor();
		//verifyStatic();
		//    DME2Manager.getRunningInstanceName();
		verifyInitConfig();
	}

	private void verifyInitCtor() throws DME2Exception {
		//verify( mockRegistry ).getManager();
		//    verify( mockManager, atLeastOnce() ).getName();
		verifyStatic( times(2) );
		DME2CacheFactory.getCacheManager(mockConfig);
		verify( mockCacheManager ).getCache( DEFAULT_CACHE_NAME );
		verify( mockCacheManager ).createCache( eq(DEFAULT_CACHE_NAME), eq("EndpointCache"),
				(com.att.aft.dme2.cache.service.DME2CacheableCallback) any() );
	}

	private void verifyInitConfig() {
		verify( mockRegistry, atLeastOnce() ).getConfig();
		/* verify( mockConfig ).getInt( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" );
    verify( mockConfig ).getInt( "DME2_UNUSED_ENDPOINT_REMOVAL_DELAY" );
    verify( mockConfig ).getInt( "DME2_UNUSED_ENDPOINT_REMOVAL_DURATION_MS" );
    verify( mockConfig ).getInt( "DME2_SEP_CACHE_TTL_MS" );
    verify( mockConfig ).getInt( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS" );
    verify( mockConfig ).getInt( "DME2_PERSIST_CACHED_ENDPOINTS_FREQUENCY_MS" );*/
	}

	private void mockInit() throws DME2Exception {
		mockInitCtor();
		//  when( DME2Manager.getRunningInstanceName() ).thenReturn( null );
		mockInitConfig();
	}

	private void mockInitCtor() throws DME2Exception {
		//when( mockRegistry.getManager() ).thenReturn( mockManager );
		when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
		Mockito.when( DME2CacheFactory.getCacheManager(mockConfig) ).thenReturn( mockCacheManager );
		when( mockCacheManager.getCache( Mockito.anyString() ) ).thenReturn( null );
		when( mockCacheManager.createCache( Mockito.anyString(), Mockito.anyString(),
				(com.att.aft.dme2.cache.service.DME2CacheableCallback) Mockito.any() ) ).thenReturn( mockCache );
	}

	private void mockInitConfig() {
		when( mockRegistry.getConfig() ).thenReturn( mockConfig );
		when( mockConfig.getInt( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" )).thenReturn( 1000000 );
		when( mockConfig.getInt( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS" )).thenReturn( 1000000 );
		when( mockConfig.getInt( "DME2_UNUSED_ENDPOINT_REMOVAL_DELAY" ) ).thenReturn( 1000000 );
		when( mockConfig.getInt( "DME2_PERSIST_CACHED_ENDPOINTS_FREQUENCY_MS" ) ).thenReturn( 1000000 );
		when( mockConfig.getInt( "DME2_SEP_CACHE_TTL_MS")).thenReturn( DEFAULT_SEP_CACHE_TTL_MS );
	}
}

