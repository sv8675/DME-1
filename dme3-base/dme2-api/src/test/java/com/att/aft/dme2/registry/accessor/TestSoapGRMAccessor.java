package com.att.aft.dme2.registry.accessor;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.server.test.TestConstants;

@Ignore
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.xml.*","javax.management.*","com.sun.org.*", "org.xml.sax.*", "org.w3c.*" })
public class TestSoapGRMAccessor {

	private DME2Configuration mockConfig;

	@BeforeClass
	public static void setUp() {
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", "SANDBOX-DEV");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
		System.setProperty("DME2_GRM_SERVER_PROTOCOL", "http");
		System.setProperty("GRMACESSOR_HANDLER_IMPL", "com.att.aft.dme2.registry.accessor.SoapGRMAccessor");
	}

	@AfterClass
	public static void tearDown() {
		System.clearProperty("DME2.DEBUG");
		System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
		System.clearProperty("platform");
		System.clearProperty("AFT_ENVIRONMENT");
		System.clearProperty("AFT_LATITUDE");
		System.clearProperty("AFT_LONGITUDE");
		System.clearProperty("AFT_DME2_GRM_URLS");
		System.clearProperty("DME2_GRM_SERVER_PROTOCOL");
		System.clearProperty("GRMACESSOR_HANDLER_IMPL");
	}

	@Before
	public void setUpTest() {
		mockConfig = Mockito.mock( DME2Configuration.class );
	}

	@Test
	public void testAddServiceEndpoint() {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-1" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );
		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			grm.addServiceEndPoint( dmeEndpoint );
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint( dmeEndpoint );
			Assert.assertNotNull( findEndPoint );
			Assert.assertSame( findEndPoint.size(), 1 );
			ServiceEndpoint ep = findEndPoint.get( 0 );
			Assert.assertEquals( dmeEndpoint.getName(), ep.getName() );
			Assert.assertEquals( dmeEndpoint.getVersion(), ep.getVersion().toString() );
			Assert.assertEquals( dmeEndpoint.getPort(), ep.getPort() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getHostAddress(), ep.getHostAddress() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getRouteOffer(), ep.getRouteOffer() );
			Assert.assertEquals( dmeEndpoint.getLatitude(), ep.getLatitude() );
			Assert.assertEquals( dmeEndpoint.getLongitude(), ep.getLongitude() );
			Assert.assertEquals( dmeEndpoint.getContextPath(), ep.getContextPath() );

		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void testUpdateServiceEndpoint() {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-2" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );

		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			grm.addServiceEndPoint( dmeEndpoint );
			dmeEndpoint.setProtocol( "jms" );
			grm.updateServiceEndPoint( dmeEndpoint );
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint( dmeEndpoint );
			Assert.assertNotNull( findEndPoint );
			Assert.assertSame( findEndPoint.size(), 1 );
			ServiceEndpoint ep = findEndPoint.get( 0 );
			Assert.assertEquals( dmeEndpoint.getName(), ep.getName() );
			Assert.assertEquals( dmeEndpoint.getVersion(), ep.getVersion().toString() );
			Assert.assertEquals( dmeEndpoint.getPort(), ep.getPort() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getHostAddress(), ep.getHostAddress() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getRouteOffer(), ep.getRouteOffer() );
			Assert.assertEquals( dmeEndpoint.getLatitude(), ep.getLatitude() );
			Assert.assertEquals( dmeEndpoint.getLongitude(), ep.getLongitude() );
			Assert.assertEquals( dmeEndpoint.getContextPath(), ep.getContextPath() );
		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void testDeleteServiceEndpoint() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-3/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-3" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );
		DME2Configuration config = new DME2Configuration();
		BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
		grm.addServiceEndPoint( dmeEndpoint );
		List<ServiceEndpoint> findEndPointResult = grm.findRunningServiceEndPoint( dmeEndpoint );
		Assert.assertNotNull( findEndPointResult );
		grm.deleteServiceEndPoint( dmeEndpoint );
		List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint( dmeEndpoint );
		Assert.assertNotNull( findEndPoint );
		Assert.assertEquals( findEndPoint.size(), 0 );
	}

	@Test
	public void testGetRouteInfo() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-4/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-4" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );

		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			grm.addServiceEndPoint( dmeEndpoint );
			String routeInfo = grm.getRouteInfo( dmeEndpoint );
			Assert.assertNotNull( routeInfo );
		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void testFindRunningServiceEndPoint() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-5/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-5" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );
		Assert.assertTrue( true );

		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			grm.addServiceEndPoint( dmeEndpoint );
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint( dmeEndpoint );
			Assert.assertNotNull( findEndPoint );
			Assert.assertSame( findEndPoint.size(), 1 );
			ServiceEndpoint ep = findEndPoint.get( 0 );
			Assert.assertEquals( dmeEndpoint.getName(), ep.getName() );
			Assert.assertEquals( dmeEndpoint.getVersion(), ep.getVersion().toString() );
			Assert.assertEquals( dmeEndpoint.getPort(), ep.getPort() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getHostAddress(), ep.getHostAddress() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getRouteOffer(), ep.getRouteOffer() );
			Assert.assertEquals( dmeEndpoint.getLatitude(), ep.getLatitude() );
			Assert.assertEquals( dmeEndpoint.getLongitude(), ep.getLongitude() );
			Assert.assertEquals( dmeEndpoint.getContextPath(), ep.getContextPath() );
		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void testAddServiceEndpointFailure() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-3/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-3" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );

		DME2Configuration config = new DME2Configuration();
		BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
		grm.addServiceEndPoint( dmeEndpoint );
		List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint( dmeEndpoint );
		Assert.assertNotNull( findEndPoint );
		Assert.assertSame( findEndPoint.size(), 1 );
		ServiceEndpoint ep = findEndPoint.get( 0 );
		Assert.assertEquals( dmeEndpoint.getName(), ep.getName() );
		Assert.assertEquals( dmeEndpoint.getVersion(), ep.getVersion().toString() );
		Assert.assertEquals( dmeEndpoint.getPort(), ep.getPort() );
		Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
		Assert.assertEquals( dmeEndpoint.getHostAddress(), ep.getHostAddress() );
		Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
		Assert.assertEquals( dmeEndpoint.getRouteOffer(), ep.getRouteOffer() );
		Assert.assertEquals( dmeEndpoint.getLatitude(), ep.getLatitude() );
		Assert.assertEquals( dmeEndpoint.getLongitude(), ep.getLongitude() );
		Assert.assertEquals( dmeEndpoint.getContextPath(), ep.getContextPath() );

	}

	@Test
	public void testUpdateServiceEndpointFailure() {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-3/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-3" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );

		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			// grm.addServiceEndPoint(dmeEndpoint);
			// dmeEndpoint.setProtocol("jms");
			grm.updateServiceEndPoint( dmeEndpoint );
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint( dmeEndpoint );
			Assert.assertNotNull( findEndPoint );
			Assert.assertSame( findEndPoint.size(), 1 );
			ServiceEndpoint ep = findEndPoint.get( 0 );
			Assert.assertEquals( dmeEndpoint.getName(), ep.getName() );
			Assert.assertEquals( dmeEndpoint.getVersion(), ep.getVersion().toString() );
			Assert.assertEquals( dmeEndpoint.getPort(), ep.getPort() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getHostAddress(), ep.getHostAddress() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getRouteOffer(), ep.getRouteOffer() );
			Assert.assertEquals( dmeEndpoint.getLatitude(), ep.getLatitude() );
			Assert.assertEquals( dmeEndpoint.getLongitude(), ep.getLongitude() );
			Assert.assertEquals( dmeEndpoint.getContextPath(), ep.getContextPath() );

			for ( ServiceEndpoint endpoint : findEndPoint ) {
				System.out.println( "end point name is: " + endpoint.getName() );
			}
		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void testDeleteServiceEndpointFailure() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-3/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-3" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );

		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			grm.updateServiceEndPoint( dmeEndpoint );
			grm.deleteServiceEndPoint( dmeEndpoint );

		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}

	}

	@Test
	public void testGetRouteInfoFailure() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-45/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-45" );
		dmeEndpoint.setRouteOffer( "DEFAULT1" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );

		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			String routeInfo = grm.getRouteInfo( dmeEndpoint );
			System.out.println( "routeinfo: " + routeInfo );
			Assert.assertNotNull( routeInfo );
		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void testFindRunningServiceEndPointFailure() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion( "1.0.0" );
		dmeEndpoint.setHostAddress( "TestHost" );
		dmeEndpoint
		.setContextPath( "/service=com.att.test.TestService-5/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
		dmeEndpoint.setPort( "12345" );
		dmeEndpoint.setName( "com.att.test.TestService-5" );
		dmeEndpoint.setRouteOffer( "DEFAULT" );
		dmeEndpoint.setLatitude( "1.11" );
		dmeEndpoint.setLongitude( "-2.22" );
		dmeEndpoint.setEnv( "LAB" );
		dmeEndpoint.setProtocol( "http" );
		Assert.assertTrue( true );

		try {
			DME2Configuration config = new DME2Configuration();
			BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint( dmeEndpoint );
			Assert.assertNotNull( findEndPoint );
			Assert.assertSame( findEndPoint.size(), 1 );
			ServiceEndpoint ep = findEndPoint.get( 0 );
			Assert.assertEquals( dmeEndpoint.getName(), ep.getName() );
			Assert.assertEquals( dmeEndpoint.getVersion(), ep.getVersion().toString() );
			Assert.assertEquals( dmeEndpoint.getPort(), ep.getPort() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getHostAddress(), ep.getHostAddress() );
			Assert.assertEquals( dmeEndpoint.getProtocol(), ep.getProtocol() );
			Assert.assertEquals( dmeEndpoint.getRouteOffer(), ep.getRouteOffer() );
			Assert.assertEquals( dmeEndpoint.getLatitude(), ep.getLatitude() );
			Assert.assertEquals( dmeEndpoint.getLongitude(), ep.getLongitude() );
			Assert.assertEquals( dmeEndpoint.getContextPath(), ep.getContextPath() );
		} catch ( Exception ex ) {
			Assert.assertTrue( false );
		}
	}

	@Test
	public void test_get_discovery_url_null_env_letter() throws Exception {
		try {
			// There's really no need to tell the mockConfig to return anything specific - we want the return from envLetter
			// to be null, and that will happen automatically if config returns null for everything.

			DME2Configuration config = new DME2Configuration(  );
			SoapGRMAccessor accessor = (SoapGRMAccessor) GRMAccessorFactory.getGrmAccessorHandlerInstance( config, SecurityContext.create( config ));

			// If this doesn't throw an exception, we're ok
			accessor.getDiscoveryURL();
		} finally {
			//DME2UnitTestUtil.setFinalStatic( SoapGRMAccessor.class.getDeclaredField( "config" ), config );
		}
	}
}
