/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2EndpointTestUtil;
import com.att.aft.dme2.manager.registry.util.DME2Protocol;
import com.att.aft.dme2.manager.registry.util.ServiceEndpointTestUtil;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.registry.accessor.SoapGRMAccessor;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.DME2ValidationUtil;
import com.att.aft.dme2.util.XMLGregorianCalendarConverter;

@RunWith(PowerMockRunner.class)
@PrepareForTest(
    { DME2EndpointRegistryGRM.class, Thread.class, JAXBContext.class, DME2ValidationUtil.class, DME2URIUtils.class, GRMAccessorFactory.class, DME2RouteInfo.class })
@SuppressStaticInitializationFor({ "com.att.aft.dme2.api.DME2Manager", "com.att.aft.dme2.manager.registry.DME2RouteInfo" })
@PowerMockIgnore("javax.management.*")
public class DME2EndpointRegistryGRMTest {
  private static final Logger logger = LoggerFactory.getLogger( DME2EndpointRegistryGRMTest.class );
  private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 50 );
  private static final String DEFAULT_PATH = RandomStringUtils.randomAlphanumeric( 25 );
  private static final String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 20 );
  private static final int DEFAULT_PORT = RandomUtils.nextInt();
  private static final String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphanumeric( 5 );
  private static final DME2Endpoint DEFAULT_ENDPOINT = DME2EndpointTestUtil.createDefaultDME2Endpoint();
  private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 30 );
  private static final String DEFAULT_SERVICE_VERSION = RandomStringUtils.randomAlphanumeric( 5 );
  private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 5 );
  private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 10 );
  private static final List<DME2Endpoint> DEFAULT_ENDPOINT_LIST = DME2EndpointTestUtil.createDefaultDME2EndpointList();

  // Try to build an actual RouteInfo is always failing due to something in the SCLD Config.  Just use a mock
  //private static final DME2RouteInfo DEFAULT_ROUTE_INFO = DME2RouteInfoUtil.createDefaultRouteInfo();

  /*private static final RouteInfo DEFAULT_ROUTE_INFO = new RouteInfo();
  private static final RouteGroups DEFAULT_ROUTE_GROUPS = new RouteGroups();

  static {
    DEFAULT_ROUTE_INFO.setRouteGroups( DEFAULT_ROUTE_GROUPS );
    DEFAULT_ROUTE_GROUPS.
  }*/
  private static final String DEFAULT_ROUTE_INFO_XML =
      "<RouteInfo serviceName=\"MyServiceName\" serviceVersion=\"MyServiceVersion\" envContext=\"MyEnvContext\"><routeGroups><routeGroup name=\"MyRouteGroup\"/></routeGroups></RouteInfo>";
  private static final List<ServiceEndpoint> DEFAULT_SERVICE_ENDPOINT_LIST = new ArrayList<ServiceEndpoint>();
  private static final ServiceEndpoint DEFAULT_SERVICE_ENDPOINT = ServiceEndpointTestUtil.createDefaultServiceEndpoint();

  static {
    DEFAULT_SERVICE_ENDPOINT_LIST.add( DEFAULT_SERVICE_ENDPOINT );
  }
  String someRandomString = RandomStringUtils.randomAlphanumeric( 30 );
  Set<CacheElement.Key> serviceURISet = new HashSet<CacheElement.Key>();

  private DME2Manager mockManager;
  private DME2Configuration mockConfiguration;
  private Thread mockThread;
  private ClassLoader mockContextClassLoader;
  private JAXBContext mockJAXBContext;
  private DME2EndpointCacheGRM mockEndpointCache;
  private DME2RouteInfoCacheGRM mockRouteInfoCache;
  private SoapGRMAccessor mockGRMServiceAccessor;
  private DmeUniformResource mockUniformResource;
  private SoapGRMAccessor mockSoapGRMAccessor;
  private DME2RouteInfo mockRouteInfo;
  private DME2StaleCache mockStaleCache;

  @BeforeClass
  public static void setUp() {
    mockStatic( Thread.class );
    mockStatic( DME2ValidationUtil.class );
  }

  @Before
  public void setUpTest() {
    if ( serviceURISet.isEmpty() ) {
      serviceURISet.add( new CacheElement.Key<String>(someRandomString) );
    }
    mockManager = mock( DME2Manager.class );
    mockConfiguration = mock( DME2Configuration.class );
    mockThread = mock( Thread.class );
    mockContextClassLoader = mock( ClassLoader.class );
    mockJAXBContext = mock( JAXBContext.class );
    mockEndpointCache = mock( DME2EndpointCacheGRM.class );
    mockRouteInfoCache = mock( DME2RouteInfoCacheGRM.class );
    mockGRMServiceAccessor = mock( SoapGRMAccessor.class );
    mockUniformResource = mock( DmeUniformResource.class );
    mockSoapGRMAccessor = mock( SoapGRMAccessor.class );
    mockRouteInfo = mock(DME2RouteInfo.class);
    mockStaleCache = mock( DME2StaleCache.class );
  }

  @Test
  public void test_ctor() throws Exception {
    // record

    mockCtor();

    // play

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );

    // verify

    verifyCtor();
  }

  @Test
  public void test_init() throws Exception {
    System.setProperty( "AFT_DME2_PERSISTED_CACHE_FILE_TTL_MS", "0L" );
    // record

    mockCtor();
    mockInit();

    // play

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    endpointRegistry.init( null );

   /* Assert.assertTrue( (
        (HashMap<String, Boolean>) DME2UnitTestUtil
        .getPrivate( DME2EndpointRegistryGRM.class.getDeclaredField( "fetchEndpointsFromGRM" ), endpointRegistry ) )
        .get( someRandomString ) );
    Assert.assertTrue( ( (HashMap<String, Boolean>) DME2UnitTestUtil
        .getPrivate( DME2EndpointRegistryGRM.class.getDeclaredField( "fetchRouteInfoFromGRM" ), endpointRegistry ) )
        .get( someRandomString ) );*/

    // verify

    verifyCtor();
    verifyInit( endpointRegistry );
  }

  @Test
  public void test_publish() throws Exception {
    mockCtor();
    mockInit();

    mockStatic( DME2ValidationUtil.class );
    PowerMockito.doNothing().when( DME2ValidationUtil.class );
    DME2ValidationUtil.validateJDBCEndpointRequiredFields( null, DEFAULT_SERVICE );
    PowerMockito.whenNew( DmeUniformResource.class ).withAnyArguments().thenReturn( mockUniformResource );

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    endpointRegistry.init( null );
    endpointRegistry.publish( DEFAULT_SERVICE, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DME2Protocol.DME2JDBC );

    verifyCtor();
    verifyInit( endpointRegistry );
    PowerMockito.verifyStatic();
    DME2ValidationUtil.validateJDBCEndpointRequiredFields( (Properties) Mockito.any(), (String) Mockito.any() );
    verifyNew( DmeUniformResource.class ).withArguments( eq( mockConfiguration ), Mockito.anyString() );
    verify( mockGRMServiceAccessor ).addServiceEndPoint(
        (com.att.aft.dme2.registry.dto.ServiceEndpoint) anyObject() );
  }

  @Test
  public void test_unpublish() throws Exception {
    mockCtor();
    mockInit();
    PowerMockito.whenNew( DmeUniformResource.class ).withAnyArguments().thenReturn( mockUniformResource );
    Mockito.when( mockEndpointCache.containsKey( "/" + DEFAULT_ENDPOINT.getPath() )).thenReturn( true );

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    endpointRegistry.init( null );
    endpointRegistry.unpublish( DEFAULT_ENDPOINT );

    verifyCtor();
    verifyInit( endpointRegistry );
    verifyNew( DmeUniformResource.class ).withArguments( eq( mockConfiguration), Mockito.anyString() );
    verify( mockGRMServiceAccessor ).deleteServiceEndPoint(
        (com.att.aft.dme2.registry.dto.ServiceEndpoint) anyObject() );
    verify( mockEndpointCache ).containsKey( "/" + DEFAULT_ENDPOINT.getPath() );
    //verify( mockEndpointCache ).refreshCachedEndpoint( "/" + DEFAULT_ENDPOINT.getPath() );
  }


  @Test
  public void test_find_endpoints() throws Exception {
    mockCtor();
    mockInit();
    mockStatic( DME2URIUtils.class );
    when( DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER )).thenReturn(
        DEFAULT_SERVICE );
    when( mockEndpointCache.getEndpoints( DEFAULT_SERVICE )).thenReturn( DEFAULT_ENDPOINT_LIST );

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    endpointRegistry.init( null );
    endpointRegistry.findEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );

    verifyCtor();
    verifyInit( endpointRegistry );
    verifyStatic( times( 2 ));
    DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
    verify( mockEndpointCache ).getEndpoints( DEFAULT_SERVICE );
  }
/*
  @Test
  public void test_template() {
    mockCtor();
    mockInit();
    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockManager );
    verifyCtor();
    verifyInit(endpointRegistry);
  }
*/
  @Test
  public void test_getRouteInfo() throws Exception {
    mockCtor();
    mockInit();
    when( mockRouteInfoCache.get( Mockito.anyString() )).thenReturn( mockRouteInfo );

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    endpointRegistry.init( null );
    DME2RouteInfo actualRouteInfo = endpointRegistry.getRouteInfo( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT );
    assertEquals( actualRouteInfo, mockRouteInfo );

    verifyCtor();
    verifyInit(endpointRegistry);
  }

  @Test
  public void test_getRouteInfo_null_cache() throws Exception {
    whenNew( DME2Configuration.class ).withAnyArguments().thenReturn( mockConfiguration );
    whenNew( DME2StaleCache.class ).withAnyArguments().thenReturn( mockStaleCache );
    whenNew( DME2EndpointCacheGRM.class ).withAnyArguments().thenReturn( mockEndpointCache );
    whenNew( DME2RouteInfoCacheGRM.class ).withAnyArguments().thenReturn( mockRouteInfoCache );
    mockStatic( GRMAccessorFactory.class );
    when( mockRouteInfoCache.get( Mockito.anyString() )).thenReturn( null );
    when( mockConfiguration.getProperty( DME2Constants.DME2_GRM_USER )).thenReturn( "Whatever" );
    when( mockConfiguration.getProperty( DME2Constants.DME2_GRM_PASS )).thenReturn( "Whatever" );
    GRMAccessorFactory mockGRMAccessorFactory = mock( GRMAccessorFactory.class );
    when( GRMAccessorFactory.getInstance() ).thenReturn( mockGRMAccessorFactory );
    when ( mockGRMAccessorFactory.getGrmAccessorHandlerInstance( anyObject(), anyObject()) ).thenReturn( mockGRMServiceAccessor );
    when( mockGRMServiceAccessor.getRouteInfo(  anyObject() )).thenReturn( DEFAULT_ROUTE_INFO_XML );

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    endpointRegistry.init( null );
    DME2RouteInfo actualRouteInfo = endpointRegistry.getRouteInfo( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT );
    assertNotNull( actualRouteInfo );
    assertEquals( actualRouteInfo.getServiceName(), "MyServiceName" );
    assertEquals( actualRouteInfo.getServiceVersion(), DEFAULT_SERVICE_VERSION );
    assertEquals( actualRouteInfo.getEnvContext(), "MyEnvContext" );

    verifyInit( endpointRegistry );
  }

  @Test
  public void test_fetchEndpoints() throws Exception {
    mockCtor();
    mockInit();
    ServiceEndpoint serviceEndpoint = ServiceEndpointTestUtil.createDefaultServiceEndpoint();
    serviceEndpoint.setExpirationTime( XMLGregorianCalendarConverter.asXMLGregorianCalendar( new Date( System.currentTimeMillis() + 1000000 )) );
    List<ServiceEndpoint> serviceEndpoints = new ArrayList<ServiceEndpoint>();
    serviceEndpoints.add( serviceEndpoint );
    whenNew( SoapGRMAccessor.class ).withAnyArguments().thenReturn( mockSoapGRMAccessor );
    when( mockGRMServiceAccessor.findRunningServiceEndPoint( (com.att.aft.dme2.registry.dto.ServiceEndpoint) anyObject() )).thenReturn( serviceEndpoints );
    mockStatic( DME2URIUtils.class );
    when( DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER )).thenReturn( DEFAULT_SERVICE );
    when( DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_SERVICE_ENDPOINT.getRouteOffer() )).thenReturn( DEFAULT_SERVICE );
    when( mockEndpointCache.getEndpoints( DEFAULT_SERVICE )).thenReturn( DEFAULT_ENDPOINT_LIST );

    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    endpointRegistry.init( null );
    List<DME2Endpoint> actualEndpoints = endpointRegistry.fetchEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_SERVICE_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER, null );
    assertEquals(DEFAULT_ENDPOINT_LIST, actualEndpoints);

    verifyCtor();
    verifyInit(endpointRegistry);
    verify( mockEndpointCache ).put( eq( DEFAULT_SERVICE ), (DME2ServiceEndpointData) anyObject() );
  }

  private void mockCtor() throws Exception {
    whenNew( DME2Configuration.class ).withAnyArguments().thenReturn( mockConfiguration );
    whenNew( DME2StaleCache.class ).withAnyArguments().thenReturn( mockStaleCache );
    mockStatic( Thread.class );
    when( Thread.currentThread() ).thenReturn( mockThread );
    when( mockThread.getContextClassLoader() ).thenReturn( mockContextClassLoader );
    mockStatic( JAXBContext.class );
    when( JAXBContext.newInstance( Mockito.anyString() ) ).thenReturn( mockJAXBContext );
    mockStatic( DME2Manager.class );
    when( DME2Manager.getTimezone() ).thenReturn( TimeZone.getDefault() );
  }

  private void verifyCtor() throws Exception {
//    verify( mockManager ).getName();
//    verifyNew( DME2Configuration.class ).withArguments( DEFAULT_MANAGER_NAME );
    verifyStatic( times( 2 ));
    Thread.currentThread();
    verify( mockThread ).getContextClassLoader();
    verifyStatic();
    JAXBContext.newInstance( RouteInfo.class.getPackage().getName() );
  }

  private void mockInit() throws Exception {
    whenNew( DME2EndpointCacheGRM.class ).withAnyArguments().thenReturn( mockEndpointCache );
    whenNew( DME2RouteInfoCacheGRM.class ).withAnyArguments().thenReturn( mockRouteInfoCache );
    //whenNew( SoapGRMAccessor.class ).withAnyArguments().thenReturn( mockGRMServiceAccessor );
    mockStatic( GRMAccessorFactory.class );
    GRMAccessorFactory mockGRMAccessorFactory = mock( GRMAccessorFactory.class );
    when( GRMAccessorFactory.getInstance() ).thenReturn( mockGRMAccessorFactory );
    when ( mockGRMAccessorFactory.getGrmAccessorHandlerInstance( (DME2Configuration) anyObject(), anyObject() )).thenReturn(
        mockGRMServiceAccessor );
    when( mockConfiguration.getProperty( "DME2_EP_ACCESSOR_CLASS" ) ).thenReturn( "GRMAccessor" );
    when( mockConfiguration.getProperty( DME2Constants.DME2_GRM_USER )).thenReturn( "Whatever" );
    when( mockConfiguration.getProperty( DME2Constants.DME2_GRM_PASS )).thenReturn( "Whatever" );
    when( mockEndpointCache.getCurrentSize() ).thenReturn( 1 );
    //when( mockEndpointCache.getLastPersisted() ).thenReturn( 0L );
    when( mockEndpointCache.getKeySet() ).thenReturn( serviceURISet );
    when( mockRouteInfoCache.getCurrentSize() ).thenReturn( 1 );
    //when( mockRouteInfoCache.getLastPersisted() ).thenReturn( 0L );
    when( mockRouteInfoCache.getKeySet() ).thenReturn( serviceURISet );
  }

  private void verifyInit( DME2EndpointRegistryGRM endpointRegistry ) throws Exception {
    verifyNew( DME2EndpointCacheGRM.class ).withArguments( mockConfiguration, endpointRegistry, DEFAULT_MANAGER_NAME, false );
    verifyNew( DME2RouteInfoCacheGRM.class ).withArguments( mockConfiguration, endpointRegistry, DEFAULT_MANAGER_NAME );
    //verifyNew( SoapGRMAccessor.class ).withArguments();
//    verify( mockEndpointCache ).getCurrentSize();
    //verify( mockEndpointCache ).getLastPersisted();
//    verify( mockEndpointCache ).getKeySet();
//    verify( mockRouteInfoCache ).getCurrentSize();
//    verify( mockRouteInfoCache ).getLastPersisted();
//    verify( mockRouteInfoCache ).getKeySet();
  }

  @Test
  public void testIsLeaseExpired() throws Exception {
    mockCtor();
    mockInit();
    ServiceEndpoint sep = new ServiceEndpoint();
    sep.setExpirationTime( XMLGregorianCalendarConverter.asXMLGregorianCalendar( new Date( Calendar
        .getInstance( ).getTime().getTime() + 10000) ) );
    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertFalse( endpointRegistry.isLeaseExpired( sep ) );
  }

  @Test
  public void testIsLeaseExpiredNextTimezone() throws Exception {
    logger.debug( null, "testIsLeaseExpiredNextTimezone", LogMessage.METHOD_ENTER );

    mockCtor();
    mockInit();
    ServiceEndpoint sep = new ServiceEndpoint();
    XMLGregorianCalendar xmlgc = XMLGregorianCalendarConverter.asXMLGregorianCalendar( new Date( Calendar.getInstance().getTime().getTime() + 10000 ));
    xmlgc.setTimezone( xmlgc.getTimezone() + 120 );
    sep.setExpirationTime( xmlgc );
    SimpleDateFormat sdf = new SimpleDateFormat( "YYYY-MM-dd HH:mm:SS Z" );
    logger.debug( null, "testIsLeaseExpiredNextTimezone", "Current Time: {} Expiration Time: {}", sdf.format( new Date() ), sdf.format( xmlgc.toGregorianCalendar().getTime() ));
    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertTrue( endpointRegistry.isLeaseExpired( sep ) );
    logger.debug( null, "testIsLeaseExpiredNextTimezone", LogMessage.METHOD_EXIT );
  }

  @Test
  public void testIsLeaseExpiredPrevTimezone() throws Exception {
    logger.debug( null, "testIsLeaseExpiredPrevTimezone", LogMessage.METHOD_ENTER );

    mockCtor();
    mockInit();
    ServiceEndpoint sep = new ServiceEndpoint();
    XMLGregorianCalendar xmlgc = XMLGregorianCalendarConverter.asXMLGregorianCalendar( new Date( Calendar.getInstance().getTime().getTime() + 10000 ));
    logger.debug( null, "testIsLeaseExpiredPrevTimezone", "Current timezone: {}", xmlgc.getTimezone());
    xmlgc.setTimezone( xmlgc.getTimezone()-120 );
    logger.debug( null, "testIsLeaseExpiredPrevTimezone", "Current timezone: {}", xmlgc.getTimezone());
    sep.setExpirationTime( xmlgc );
    SimpleDateFormat sdf = new SimpleDateFormat( "YYYY-MM-dd HH:mm:ss Z" );
    logger.debug( null, "testIsLeaseExpiredPrevTimezone", "Current Time: {} Expiration Time: {}", sdf.format( new Date() ), sdf.format( xmlgc.toGregorianCalendar().getTime() ));
    DME2EndpointRegistryGRM endpointRegistry = new DME2EndpointRegistryGRM( mockConfiguration, DEFAULT_MANAGER_NAME );
    assertFalse( endpointRegistry.isLeaseExpired( sep ) );
    logger.debug( null, "testIsLeaseExpiredPrevTimezone", LogMessage.METHOD_EXIT );
  }
}

