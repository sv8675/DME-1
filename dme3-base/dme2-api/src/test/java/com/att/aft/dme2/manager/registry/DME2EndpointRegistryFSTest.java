/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
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
import com.att.aft.dme2.manager.registry.util.DME2EndpointTestUtil;
import com.att.aft.dme2.manager.registry.util.DME2FileUtil;
import com.att.aft.dme2.manager.registry.util.DME2Protocol;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.DME2ValidationUtil;

@PrepareForTest({ DME2EndpointRegistryFS.class, DME2AbstractEndpointRegistry.class, DME2URIUtils.class, DME2ValidationUtil.class, JAXBContext.class, DME2FileUtil.class })
@SuppressStaticInitializationFor({"com.att.aft.dme2.server.DME2Manager", "com.att.aft.dme2.util.DME2Configuration" })
@PowerMockIgnore({ "javax.management.*", "org.slf4j.*" } )
@RunWith( PowerMockRunner.class )
public class DME2EndpointRegistryFSTest {
  private static final String DEFAULT_DIR = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_SERVICE_VERSION = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 5 );
  private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 15 );
  private static final Long DEFAULT_FILE_LAST_UPDATE_TIME_MS = RandomUtils.nextLong();
  private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 50 );
  private static final List<DME2Endpoint> DEFAULT_ENDPOINTS = DME2EndpointTestUtil.createDefaultDME2EndpointList();
  private static final String DEFAULT_PATH = RandomStringUtils.randomAlphanumeric( 50 );
  private static final String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 20 );
  private static final int DEFAULT_PORT = RandomUtils.nextInt();
  private static final double DEFAULT_LATITUDE = RandomUtils.nextDouble();
  private static final double DEFAULT_LONGITUDE = RandomUtils.nextDouble();
  private static final String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphanumeric( 10 );
  private static final boolean DEFAULT_UPDATE_LEASE = RandomUtils.nextInt( 1 ) == 1;
  private static final Long DEFAULT_LATITUDE_LONG = RandomUtils.nextLong();
  private static final Long DEFAULT_LONGITUDE_LONG = RandomUtils.nextLong();
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private static final List<RouteGroup> DEFAULT_ROUTE_GROUP_LIST = Arrays.asList(new RouteGroup());

  DME2EndpointRegistryFS endpointRegistry;
  Properties defaultProperties;
  File mockFile;
  File mockLockFile;
  RandomAccessFile mockRandomAccessFile;
  FileChannel mockFileChannel;
  FileLock mockFileLock;
  File mockParentFile;
  FileInputStream mockFileInputStream;
  Properties mockProperties;
  File mockInitFile;
  DME2Configuration mockConfiguration;
  DME2FileHandler mockFileHandler;
  DME2CacheManager mockCacheManager;
  DME2Cache mockCache;
  CacheElement.Value mockCacheValue;
  DME2RouteInfo mockRouteInfo;
  RouteInfo mockBaseRouteInfo; // Can't use the object, since we can't set a lot of things we need

  private CacheElement.Value<DME2RouteInfo> mockRouteInfoValue;
  private DmeUniformResource mockUniformResource;
  private DME2EndpointCacheFS mockEndpointCache;
  private DME2RouteInfoCacheFS mockRouteInfoCache;
  private DME2Manager mockManager;
  private StreamSource mockStreamSource;
  private JAXBContext mockJAXBContext;
  private Unmarshaller mockUnmarshaller;
  private JAXBElement mockJAXBElement;
  private RouteGroups mockRouteGroups;
  private DME2StaleCache mockStaleCache;

  @Before
  public void setUpTest() {
    mockManager = mock( DME2Manager.class );
    mockEndpointCache = mock( DME2EndpointCacheFS.class );
    mockRouteInfoCache = mock( DME2RouteInfoCacheFS.class );
    mockConfiguration = mock( DME2Configuration.class );
    mockFileHandler = mock( DME2FileHandler.class );
    mockCacheManager = mock( DME2CacheManager.class );
    defaultProperties = new Properties();
    defaultProperties.setProperty( "AFT_DME2_EP_REGISTRY_FS_DIR", "" );

    mockFile = mock( File.class );
    mockLockFile = mock( File.class );
    mockRandomAccessFile = PowerMockito.mock( RandomAccessFile.class );
    mockFileChannel = mock( FileChannel.class );
    mockFileLock = mock( FileLock.class );
    mockParentFile = mock( File.class );
    mockFileInputStream = mock( FileInputStream.class );
    mockProperties = mock( Properties.class );
    mockInitFile = mock( File.class );
    mockStreamSource = mock( StreamSource.class );
    mockJAXBContext = mock( JAXBContext.class );
    mockStatic( JAXBContext.class );
    mockUnmarshaller = mock( Unmarshaller.class );
    mockJAXBElement = mock( JAXBElement.class );
    mockBaseRouteInfo = mock( RouteInfo.class );
    mockRouteGroups = mock( RouteGroups.class );

    mockCache = mock( DME2Cache.class );
    mockCacheValue = mock( CacheElement.Value.class );
    mockRouteInfo = mock( DME2RouteInfo.class );

    mockRouteInfoValue = mock( CacheElement.Value.class );
    mockStaleCache = mock( DME2StaleCache.class );
    mockUniformResource = mock( DmeUniformResource.class );
    mockStatic( DME2URIUtils.class );
    mockStatic( DME2ValidationUtil.class );
    //mockStatic( DME2RouteInfoLoader.class );
  }

  private void mockCtor() throws Exception {
    when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
    whenNew( DME2Configuration.class ).withAnyArguments().thenReturn( mockConfiguration );
    whenNew( DME2EndpointCacheFS.class ).withAnyArguments().thenReturn( mockEndpointCache );
    whenNew( DME2RouteInfoCacheFS.class ).withAnyArguments().thenReturn( mockRouteInfoCache );
    whenNew( DME2StaleCache.class ).withAnyArguments().thenReturn( mockStaleCache );
    when( JAXBContext.newInstance( anyString() ) ).thenReturn( mockJAXBContext );
    when( mockJAXBContext.createUnmarshaller() ).thenReturn( mockUnmarshaller );
    endpointRegistry = new DME2EndpointRegistryFS( mockConfiguration, DEFAULT_MANAGER_NAME );
  }

  private void verifyCtor() throws Exception {
//    verify( mockManager, atLeastOnce() ).getName();
//    verifyNew( DME2Configuration.class ).withArguments( DEFAULT_MANAGER_NAME );
    verifyNew( DME2RouteInfoCacheFS.class ).withArguments( mockConfiguration, endpointRegistry, DEFAULT_MANAGER_NAME );
    //verifyNew( DME2RouteInfoCacheFS.class ).withArguments( endpointRegistry, DEFAULT_MANAGER_NAME );
  }

  private void mockInit() throws Exception {
    when( mockConfiguration.getProperty( DME2EndpointRegistryFS.CONFIG_KEY_FILE )).thenReturn( null );
    whenNew( File.class ).withAnyArguments().thenReturn( mockInitFile );
    when( mockInitFile.exists() ).thenReturn( false );
    when( mockInitFile.mkdirs() ).thenReturn( true );
    endpointRegistry.init(null);
  }

  private void verifyInit() throws Exception {
    verify( mockConfiguration ).getProperty( DME2EndpointRegistryFS.CONFIG_KEY_FILE );
    verifyNew( File.class ).withArguments( "dme2-fs-registry" );
    verify( mockInitFile, atLeastOnce() ).exists();
    verify( mockInitFile ).mkdirs();
  }

  @Test
  public void test_ctor() throws Exception {
    mockCtor();
    mockInit();
    verifyCtor();
    verifyInit();
  }

  @Test
  public void test_find_endpoints() throws Exception {
    // RECORD
    mockCtor();
    mockInit();

    PowerMockito.when( DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER ) ).thenReturn( DEFAULT_SERVICE );
    whenNew( DME2FileHandler.class ).withAnyArguments().thenReturn( mockFileHandler );
    Mockito.when( mockEndpointCache.get( DEFAULT_SERVICE_NAME ) ).thenReturn( null );
    Mockito.when( mockFileHandler.readEndpoints() ).thenReturn( DEFAULT_ENDPOINTS );
    Mockito.doNothing().when( mockEndpointCache ).putEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_ENDPOINTS );

    // play

    List<DME2Endpoint> endpoints =  endpointRegistry
          .findEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );

    assertEquals( endpoints, DEFAULT_ENDPOINTS );

    // verify

    verifyCtor();
    verifyInit();

    PowerMockito.verifyStatic();
    DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
    verifyNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), eq( DEFAULT_SERVICE ),
        anyLong(), anyDouble(), anyDouble() );
    verify( mockEndpointCache ).getEndpoints( DEFAULT_SERVICE );
    verify( mockFileHandler ).readEndpoints();
    verify( mockEndpointCache ).putEndpoints( DEFAULT_SERVICE, DEFAULT_ENDPOINTS );
  }

  @Test
  public void test_endpoints_load_props_failure() throws Exception {
    // RECORD

    mockCtor();
    mockInit();

    PowerMockito.when( DME2URIUtils
        .buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT,
            DEFAULT_ROUTE_OFFER ) ).thenReturn( DEFAULT_SERVICE );
    whenNew( DME2FileHandler.class )
        .withArguments( eq( mockInitFile ), eq( DEFAULT_SERVICE ), anyLong(), anyDouble(), anyDouble() )
        .thenReturn( mockFileHandler );
    Mockito.when( mockFileHandler.readEndpoints() ).thenThrow( new DME2Exception( "blah", "blah" ) );

    // play

    try {
      endpointRegistry
          .findEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
    } catch ( DME2Exception e ) {
      assertEquals( "blah", e.getErrorCode() );
      assertEquals( "blah", e.getErrorMessage() );
      return;
    } finally {
      verifyCtor();
      verifyInit();
      PowerMockito.verifyStatic();
      DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
      verifyNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), eq( DEFAULT_SERVICE ),
          anyLong(), anyDouble(), anyDouble() );
      verify( mockEndpointCache ).getEndpoints( DEFAULT_SERVICE );
      verify( mockFileHandler ).readEndpoints();
    }

    fail( "Should've thrown error" );
  }

  @Test
  public void test_get_route_info_from_file() throws Exception {
    // record
    mockCtor();
    mockInit();

    PowerMockito.when(
        DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT ) )
        .thenReturn(
            DEFAULT_SERVICE );
    mockStatic( DME2FileUtil.class );
    when( DME2FileUtil.hierarchicalFileLookup( mockInitFile, DEFAULT_SERVICE + "/routeInfo.xml" )).thenReturn( Arrays.asList( mockFile ));
    whenNew( File.class ).withArguments( mockInitFile, DEFAULT_SERVICE + "/routeInfo.xml" ).thenReturn( mockFile );
    whenNew( StreamSource.class ).withArguments( mockFile ).thenReturn( mockStreamSource );
    when( mockFile.exists() ).thenReturn( true );
    when( mockUnmarshaller.unmarshal( (StreamSource) any() )).thenReturn( mockJAXBElement );
    when( mockJAXBElement.getValue() ).thenReturn( mockBaseRouteInfo );

    when( mockBaseRouteInfo.getRouteGroups() ).thenReturn( mockRouteGroups );
    when( mockRouteGroups.getRouteGroup() ).thenReturn( DEFAULT_ROUTE_GROUP_LIST );
   // PowerMockito.when( DME2RouteInfoLoader.load( mockFile ) ).thenReturn( mockRouteInfo );

    // play
    DME2RouteInfo routeInfo =
        endpointRegistry.getRouteInfo( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT );
    assertNotNull( routeInfo );
//    assertEquals( routeInfo, mockRouteInfo );

    verifyCtor();
    verifyInit();
    PowerMockito.verifyStatic();
    DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT );
    verify( mockRouteInfoCache ).get( DEFAULT_SERVICE );
    verifyNew( File.class ).withArguments( mockInitFile, DEFAULT_SERVICE + "/routeInfo.xml" );
    //verify( mockFile ).exists();
    //verifyStatic();
    //DME2RouteInfoLoader.load( mockFile );
    verify( mockRouteInfoCache ).put( eq(DEFAULT_SERVICE), (DME2RouteInfo) any() );
  }

  @Test
  public void test_get_route_info_file_does_not_exist() throws Exception {
    // record
    mockCtor();
    mockInit();

    PowerMockito.when(
        DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT ) )
        .thenReturn( DEFAULT_SERVICE );
    whenNew( File.class ).withArguments( mockInitFile, DEFAULT_SERVICE + "/routeInfo.xml" ).thenReturn(
        mockFile );
    when( mockFile.exists() ).thenReturn( false );

    // play
    try {
      endpointRegistry.getRouteInfo( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT );
    } catch ( DME2Exception e ) {
      assertEquals( e.getErrorCode(), DME2Constants.EXP_REG_ROUTE_INFO_FILE_NOT_FOUND );
      verifyCtor();
      verifyInit();
      verifyStatic();
      DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT );
      verify( mockRouteInfoCache ).get( DEFAULT_SERVICE );
      verifyNew( File.class, atLeastOnce() ).withArguments( mockInitFile, DEFAULT_SERVICE + "/routeInfo.xml" );
//      verify( mockFile ).exists();
      return;
    }
    fail( "Should've thrown an exception" );
  }

  @Test
  public void test_get_route_info_from_cache() throws Exception {
    // record
    mockCtor();
    mockInit();

    PowerMockito.when(
        DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT ) )
        .thenReturn( DEFAULT_SERVICE );
    when( mockRouteInfoCache.get( DEFAULT_SERVICE )).thenReturn( mockRouteInfo );
    whenNew( File.class ).withArguments( mockInitFile, DEFAULT_SERVICE + "/routeInfo.xml" ).thenReturn(
        mockFile );
    when( mockRouteInfo.lastUpdated() ).thenReturn( 1L );
    when( mockFile.lastModified() ).thenReturn( 0L );

    // play
    DME2RouteInfo routeInfo =
        endpointRegistry.getRouteInfo( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT );

    assertNotNull( routeInfo );
    assertEquals( routeInfo, mockRouteInfo );

    verifyCtor();
    verifyInit();
  }

  @Test
  public void test_publish_servicepath_http() throws Exception {
    // record
    String service = DME2EndpointRegistryFS.HTTP_PREFIX + DEFAULT_SERVICE;

    mockCtor();
    mockInit();
    whenNew( DmeUniformResource.class ).withAnyArguments().thenReturn( mockUniformResource );
    when( mockUniformResource.getService() ).thenReturn( DEFAULT_SERVICE );
    when( mockUniformResource.getVersion() ).thenReturn( "" );
    when( mockUniformResource.getEnvContext() ).thenReturn( DEFAULT_ENV_CONTEXT );
    when( mockUniformResource.getRouteOffer() ).thenReturn( DEFAULT_ROUTE_OFFER ).thenReturn( DEFAULT_ROUTE_OFFER );
    whenNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() ).thenReturn( mockFileHandler );
    when( mockFileHandler.readProperties() ).thenReturn( mockProperties );
    Mockito.doNothing().when( mockFileHandler ).storeProperties( mockProperties, true );

    // play

    endpointRegistry.publish( service, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_LATITUDE, DEFAULT_LONGITUDE,
        DEFAULT_PROTOCOL, DEFAULT_UPDATE_LEASE );

    // verify
    verifyCtor();
    verifyInit();
    verifyNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() );
    verify( mockFileHandler ).readProperties();
    verify( mockFileHandler ).storeProperties( mockProperties, true );
  }

  @Test
  public void test_publish_servicepath_forward_slash() throws Exception {
    // record
    String service = DME2EndpointRegistryFS.FORWARD_SLASH + DEFAULT_SERVICE;

    mockCtor();
    mockInit();
    whenNew( DmeUniformResource.class ).withParameterTypes( DME2Configuration.class, String.class ).withArguments( eq(mockConfiguration), anyString() ).thenReturn( mockUniformResource );
    when( mockUniformResource.getService() ).thenReturn( DEFAULT_SERVICE );
    when( mockUniformResource.getVersion() ).thenReturn( "" );
    when( mockUniformResource.getEnvContext() ).thenReturn( DEFAULT_ENV_CONTEXT );
    when( mockUniformResource.getRouteOffer() ).thenReturn( DEFAULT_ROUTE_OFFER ).thenReturn( DEFAULT_ROUTE_OFFER );
    whenNew( DME2FileHandler.class ).withAnyArguments().thenReturn( mockFileHandler );
    when( mockFileHandler.readProperties() ).thenReturn( mockProperties );
    Mockito.doNothing().when( mockFileHandler ).storeProperties( mockProperties, true );

    // play

    endpointRegistry.publish( service, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_LATITUDE, DEFAULT_LONGITUDE,
        DEFAULT_PROTOCOL, DEFAULT_UPDATE_LEASE );

    // verify
    verifyCtor();
    verifyInit();
    verifyNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() );
    verify( mockFileHandler ).readProperties();
    verify( mockFileHandler ).storeProperties( mockProperties, true );
  }

  @Test
  public void test_publish_servicepath_other() throws Exception {
    // record
    String service = "www.blah.com/blah";

    mockCtor();
    mockInit();
    whenNew( DmeUniformResource.class ).withParameterTypes( DME2Configuration.class, String.class ).withArguments( eq(mockConfiguration), anyString() ).thenReturn( mockUniformResource );
    when( mockUniformResource.getService() ).thenReturn( DEFAULT_SERVICE );
    when( mockUniformResource.getVersion() ).thenReturn( "" );
    when( mockUniformResource.getEnvContext() ).thenReturn( DEFAULT_ENV_CONTEXT );
    when( mockUniformResource.getRouteOffer() ).thenReturn( DEFAULT_ROUTE_OFFER ).thenReturn( DEFAULT_ROUTE_OFFER );
    whenNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() ).thenReturn( mockFileHandler );
    when( mockFileHandler.readProperties() ).thenReturn( mockProperties );
    Mockito.doNothing().when( mockFileHandler ).storeProperties( mockProperties, true );

    // play

    endpointRegistry.publish( service, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_LATITUDE, DEFAULT_LONGITUDE,
        DEFAULT_PROTOCOL, DEFAULT_UPDATE_LEASE );

    // verify
    verifyCtor();
    verifyInit();
    verifyNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() );
    verify( mockFileHandler ).readProperties();
    verify( mockFileHandler ).storeProperties( mockProperties, true );
  }

  @Test
  public void test_publish_servicepath_exception() throws Exception {
    // record
    String service = "http://imnota*validurl:-10";
    mockCtor();
    mockInit();
    whenNew( DME2FileHandler.class ).withAnyArguments().thenReturn( mockFileHandler );
    when( mockFileHandler.readProperties() ).thenReturn( new Properties() );

    // play
    try {
      endpointRegistry.publish( service, service, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_LATITUDE, DEFAULT_LONGITUDE,
          DEFAULT_PROTOCOL, DEFAULT_UPDATE_LEASE );
    } catch ( DME2Exception e ) {
      assertEquals( e.getErrorCode(), DME2Constants.EXP_GEN_URI_EXCEPTION );
//      verify( mockManager, times( 1 ) ).getName();
      //verifyNew( DME2Configuration.class ).withArguments( DEFAULT_MANAGER_NAME );
      verifyNew( DME2RouteInfoCacheFS.class ).withArguments( mockConfiguration, endpointRegistry, DEFAULT_MANAGER_NAME );
      //verifyNew( DME2RouteInfoCacheFS.class ).withArguments( endpointRegistry, DEFAULT_MANAGER_NAME );
      verifyInit();
      return;
    }
    fail( "Should've thrown an exception" );
  }

  @Test
  public void test_publish_jdbc() throws Exception {
    // record
    mockCtor();
    mockInit();
    //DME2UnitTestUtil.setFinalStatic( DmeUniformResource.class.getDeclaredField( "config" ), mockConfiguration );
    when( mockConfiguration.getLong( "AFT_LATITUDE" )).thenReturn( DEFAULT_LATITUDE_LONG );
    when( mockConfiguration.getLong( "AFT_LONGITYDE" )).thenReturn( DEFAULT_LONGITUDE_LONG );

    whenNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() ).thenReturn( mockFileHandler );
    when( mockFileHandler.readProperties() ).thenReturn( mockProperties );
    PowerMockito.doNothing().when( DME2ValidationUtil.class );
    DME2ValidationUtil.validateJDBCEndpointRequiredFields( mockProperties, DEFAULT_SERVICE );
    Mockito.doNothing().when( mockFileHandler ).storeProperties( mockProperties, true );

    endpointRegistry
        .publish( DEFAULT_SERVICE, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DME2Protocol.DME2JDBC, mockProperties );

    // verify
    verifyCtor();
    verifyInit();
    verifyNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() );
    verify( mockFileHandler ).readProperties();
    verify( mockFileHandler ).storeProperties( mockProperties, true );
  }

  @Test
  public void test_publish_jdbc_exception() throws Exception {
    // record
    mockCtor();
    mockInit();

    whenNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
        eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() ).thenReturn( mockFileHandler );
    when( mockFileHandler.readProperties() ).thenReturn( mockProperties );
    PowerMockito.doThrow( new DME2Exception( "blah", "blah" ) ).when( DME2ValidationUtil.class );
    DME2ValidationUtil.validateJDBCEndpointRequiredFields( mockProperties, DEFAULT_SERVICE );
   // Mockito.doNothing().when( mockFileHandler ).storeProperties( mockProperties, true );

    try {
      endpointRegistry
          .publish( DEFAULT_SERVICE, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DME2Protocol.DME2JDBC, mockProperties );
    } catch ( DME2Exception e ) {
      assertEquals( e.getErrorCode(), "blah" );
      assertEquals( e.getErrorMessage(), "blah" );
      verifyCtor();
      verifyInit();
      verifyNew( DME2FileHandler.class ).withArguments( eq( mockInitFile ), anyString(),
          eq( DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS ), anyDouble(), anyDouble() );
      verify( mockFileHandler ).readProperties();
      verifyStatic();
      DME2ValidationUtil.validateJDBCEndpointRequiredFields( mockProperties, DEFAULT_SERVICE );
     // verify( mockFileHandler ).storeProperties( mockProperties, true );
      return;
    }

    fail( "Should've thrown an error" );
  }

  @Test
  public void test_unpublish() throws Exception {
    mockCtor();
    mockInit();
    whenNew( DME2FileHandler.class )
        .withArguments( eq(mockInitFile), eq(DEFAULT_SERVICE_NAME), eq(DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS), anyDouble(), anyDouble() ).thenReturn(
        mockFileHandler );
    when( mockFileHandler.readProperties() ).thenReturn( mockProperties );
    Mockito.doNothing().when( mockFileHandler ).storeProperties( (Properties) any(), eq( false ) );

    endpointRegistry.unpublish( DEFAULT_SERVICE_NAME, DEFAULT_HOST, DEFAULT_PORT );

    verifyCtor();
    verifyInit();
    verifyNew( DME2FileHandler.class )
        .withArguments( eq(mockInitFile), eq(DEFAULT_SERVICE_NAME), eq(DME2EndpointRegistryFS.DEFAULT_CACHE_STALENESS), anyDouble(), anyDouble() );
    verify( mockFileHandler ).readProperties();
    verify( mockFileHandler ).storeProperties( (Properties) any(), eq( false ) );
  }

  @Test
  public void test_refresh_no_endpoints() throws Exception {
    mockCtor();
    mockInit();

    endpointRegistry.refresh();

    verifyCtor();
    verifyInit();
  }
}

