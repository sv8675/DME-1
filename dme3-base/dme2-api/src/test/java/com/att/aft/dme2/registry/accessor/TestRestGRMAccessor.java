package com.att.aft.dme2.registry.accessor;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.util.DME2ParameterNames;

public class TestRestGRMAccessor {

	private static DME2Configuration config;

	@BeforeClass
	public static void setUp() {
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		config = new DME2Configuration();
		System.setProperty("AFT_DME2_GRM_USE_SSL", "true");

		File file = new File( System.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE, DME2ParameterNames.GRM_SERVER_CACHE_FILE_DEFAULT ) );
		if ( file.delete() ) {
			System.out.println( "DNS Cache File " + file.getName() + " is Deleted" );
		} else {
			System.out.println( "DNS Cache File " + file.getName() + " is NOT Deleted" );
		}
	}

	@AfterClass
	public static void tearDown() {
		System.clearProperty("AFT_DME2_GRM_USE_SSL");
	}

	@Test
	@Ignore
	public void testAddServiceEndpoint() {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion("1.0.0");
		dmeEndpoint.setHostAddress("TestHost");
		dmeEndpoint
		.setContextPath("/service=com.att.test.TestService-1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		dmeEndpoint.setPort("12345");
		dmeEndpoint.setName("com.att.test.TestService-1");
		dmeEndpoint.setRouteOffer("DEFAULT");
		dmeEndpoint.setLatitude("1.11");
		dmeEndpoint.setLongitude("-2.22");
		dmeEndpoint.setEnv("LAB");
		dmeEndpoint.setProtocol("http");
		try {
			BaseAccessor grm = GRMAccessorFactory.getGrmAccessorHandlerInstance( config, SecurityContext.create(config) );
			grm.addServiceEndPoint(dmeEndpoint);
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint(dmeEndpoint);
			Assert.assertNotNull(findEndPoint);
			Assert.assertSame(findEndPoint.size(), 1);
			ServiceEndpoint ep = findEndPoint.get(0);
			Assert.assertEquals(dmeEndpoint.getName(), ep.getName());
			Assert.assertEquals(dmeEndpoint.getVersion(), ep.getVersion().toString());
			Assert.assertEquals(dmeEndpoint.getPort(), ep.getPort());
			Assert.assertEquals(dmeEndpoint.getProtocol(), ep.getProtocol());
			Assert.assertEquals(dmeEndpoint.getHostAddress(), ep.getHostAddress());
			Assert.assertEquals(dmeEndpoint.getProtocol(), ep.getProtocol());
			Assert.assertEquals(dmeEndpoint.getRouteOffer(), ep.getRouteOffer());
			Assert.assertEquals(dmeEndpoint.getLatitude(), ep.getLatitude());
			Assert.assertEquals(dmeEndpoint.getLongitude(), ep.getLongitude());
			Assert.assertEquals(dmeEndpoint.getContextPath(), ep.getContextPath());

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	@Test
	@Ignore
	public void testUpdateServiceEndpoint() {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion("1.0.0");
		dmeEndpoint.setHostAddress("TestHost");
		dmeEndpoint
		.setContextPath("/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		dmeEndpoint.setPort("12345");
		dmeEndpoint.setName("com.att.test.TestService-2");
		dmeEndpoint.setRouteOffer("DEFAULT");
		dmeEndpoint.setLatitude("1.11");
		dmeEndpoint.setLongitude("-2.22");
		dmeEndpoint.setEnv("LAB");
		dmeEndpoint.setProtocol("http");

		try {
			BaseAccessor grm = GRMAccessorFactory.getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			grm.addServiceEndPoint(dmeEndpoint);
			dmeEndpoint.setProtocol("jms");
			grm.updateServiceEndPoint(dmeEndpoint);
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint(dmeEndpoint);
			Assert.assertNotNull(findEndPoint);
			Assert.assertSame(findEndPoint.size(), 1);
			ServiceEndpoint ep = findEndPoint.get(0);
			Assert.assertEquals(dmeEndpoint.getName(), ep.getName());
			Assert.assertEquals(dmeEndpoint.getVersion(), ep.getVersion().toString());
			Assert.assertEquals(dmeEndpoint.getPort(), ep.getPort());
			Assert.assertEquals(dmeEndpoint.getProtocol(), ep.getProtocol());
			Assert.assertEquals(dmeEndpoint.getHostAddress(), ep.getHostAddress());
			Assert.assertEquals(dmeEndpoint.getProtocol(), ep.getProtocol());
			Assert.assertEquals(dmeEndpoint.getRouteOffer(), ep.getRouteOffer());
			Assert.assertEquals(dmeEndpoint.getLatitude(), ep.getLatitude());
			Assert.assertEquals(dmeEndpoint.getLongitude(), ep.getLongitude());
			Assert.assertEquals(dmeEndpoint.getContextPath(), ep.getContextPath());
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	@Test
	@Ignore
	public void testDeleteServiceEndpoint() throws DME2Exception {

		ServiceEndpoint dmeEndpoint = new ServiceEndpoint();
		dmeEndpoint.setVersion("1.0.0");
		dmeEndpoint.setHostAddress("TestHost");
		dmeEndpoint
		.setContextPath("/service=com.att.test.TestService-3/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		dmeEndpoint.setPort("12345");
		dmeEndpoint.setName("com.att.test.TestService-3");
		dmeEndpoint.setRouteOffer("DEFAULT");
		dmeEndpoint.setLatitude("1.11");
		dmeEndpoint.setLongitude("-2.22");
		dmeEndpoint.setEnv("LAB");
		dmeEndpoint.setProtocol("http");

		try {
			BaseAccessor grm = GRMAccessorFactory.getGrmAccessorHandlerInstance(config, SecurityContext.create( config ));
			grm.addServiceEndPoint(dmeEndpoint);
			List<ServiceEndpoint> findEndPointResult = grm.findRunningServiceEndPoint(dmeEndpoint);
			Assert.assertNotNull(findEndPointResult);
			Assert.assertTrue(findEndPointResult.size() > 0);
			grm.deleteServiceEndPoint(dmeEndpoint);
			List<ServiceEndpoint> findEndPoint = grm.findRunningServiceEndPoint(dmeEndpoint);
			Assert.assertNotNull(findEndPoint);
			Assert.assertEquals(findEndPoint.size(), 0);
		} catch (Exception ex) {
			ex.printStackTrace();			
			fail(ex.getMessage());
		}

	}

}
