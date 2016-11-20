package com.att.aft.dme2.registry.accessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.test.DME2BaseTestCase2;
import com.att.aft.dme2.util.DME2ParameterNames;

public class GRMEndPointsDiscoveryHelperGRMTest extends DME2BaseTestCase2 {
	private static final Logger logger = LoggerFactory.getLogger( GRMEndPointsDiscoveryHelperGRMTest.class );

	private String dnsName;
	private String serviceName;
	private String environment;
	private String protocol;
	private String port;
	private String version;
	private String path;

	private SecurityContext sc = null;
	private GRMEndPointsCache cache = null;
	private DME2Configuration config;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		setupGRMDNSDiscovery();

		dnsName = "";
		serviceName = "";
		environment = "TEST";
		protocol = System.getProperty(DME2ParameterNames.GRM_SERVER_PROTOCOL, "https");
		port = System.getProperty(DME2ParameterNames.GRM_SERVER_PORT, "9427");
		version = "1.0.0";
		path = System.getProperty(DME2ParameterNames.GRM_SERVER_PATH, "/GRMLWPService/v1");
		config = new DME2Configuration(  );

		try {
			sc = SecurityContext.create(config);
		} catch (DME2Exception e) {
			fail("Can't create security context");
		}
		cache = GRMEndPointsCache.getInstance(config);
	}

	@After
	public void tearDown() throws Exception {
		cache.clear();
		super.tearDown();
	}

	@Test
	public void test1_GetGRMEndpoints_Unsuccessfull_Exception() {
		BaseAccessor grmServiceAccessor = mock(BaseAccessor.class);
		try {
			when(grmServiceAccessor.findRunningServiceEndPoint(any(ServiceEndpoint.class))).thenThrow(new DME2Exception("badURL", "badURL", new Exception()));
		} catch (DME2Exception e) {
			e.printStackTrace();
		}

		GRMEndPointsDiscoveryHelperGRM grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(environment, protocol, serviceName, version, grmServiceAccessor, config);
		List<String> result = grmEndPointsDiscoveryHelperGRM.getGRMEndpoints();

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void test2_GetGRMEndpoints_Unsuccessfull_NullList() {
		BaseAccessor grmServiceAccessor = mock(BaseAccessor.class);
		try {
			when(grmServiceAccessor.findRunningServiceEndPoint(any(ServiceEndpoint.class))).thenReturn(null);
		} catch (DME2Exception e) {
			e.printStackTrace();
		}

		GRMEndPointsDiscoveryHelperGRM grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(environment, protocol, serviceName, version, grmServiceAccessor, config);
		List<String> result = grmEndPointsDiscoveryHelperGRM.getGRMEndpoints();

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void test3_GetGRMEndpoints_Unsuccessfull_EmptyList() {
		BaseAccessor grmServiceAccessor = mock(BaseAccessor.class);
		try {
			when(grmServiceAccessor.findRunningServiceEndPoint(any(ServiceEndpoint.class))).thenReturn(new LinkedList<ServiceEndpoint>());
		} catch (DME2Exception e) {
			e.printStackTrace();
		}

		GRMEndPointsDiscoveryHelperGRM grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(environment, protocol, serviceName, version, grmServiceAccessor, config);
		List<String> result = grmEndPointsDiscoveryHelperGRM.getGRMEndpoints();

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Ignore
	@Test
	public void testIntegration1_GetGRMEndpointsReturnResults() throws Exception {
		// put them in cache
		cache.addAllAddressList(getSeedEndpoint());
		// make an GRMServiceAccessor from it
		BaseAccessor grmServiceAccessor = GRMAccessorFactory.getGrmAccessorHandlerInstance( config,
				SecurityContext.create( config ) );//new SoapGRMAccessor(sc, cache);
		DME2UnitTestUtil.setFinalStatic(
				AbstractGRMAccessor.class.getDeclaredField( "grmEndPointDiscovery" ),
				grmServiceAccessor, cache );
		// make GRMEndPointsDiscoveryHelperGRM
		GRMEndPointsDiscoveryHelperGRM grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(environment, protocol, serviceName, version, grmServiceAccessor, config);
		List<String> grmEndpointsInGRM = grmEndPointsDiscoveryHelperGRM.getGRMEndpoints();
		assertNotNull(grmEndpointsInGRM);
		assertNotEquals(0, grmEndpointsInGRM.size());
		// currently there are 3 http servers running in lab
		// @TODO check why 6 server is returned, we expect 3 since we have set protocol
		// assertEquals(3, grmEndpointsInGRM.size());
	}

	@Test
	@Ignore
	public void testIntegration2_GetGRMEndpoints_https() throws Exception {
		System.setProperty(DME2ParameterNames.GRM_SERVER_PROTOCOL, "https");
		System.setProperty(DME2ParameterNames.GRM_SERVER_PORT, "9227");
		protocol = System.getProperty(DME2ParameterNames.GRM_SERVER_PROTOCOL, "http");
		port = System.getProperty(DME2ParameterNames.GRM_SERVER_PORT, "9127");
		if ("https".equalsIgnoreCase(protocol)) {
			sc.setSSL(true);
		}
		testIntegration1_GetGRMEndpointsReturnResults();
	}

	@Test
	public void testIntegration2_GetGRMEndpoints_BadSeeds() throws Exception {
		final String badGRMSeedEndping = "http://localhost:9271/GRMLWPService/v1";

		String cacheFile = config.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE );
		String dnsBootstrap = config.getProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP );
		// Bad test - shouldn't load up cache!
		config.setOverrideProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE, "/dev/null" );
		System.setProperty(  DME2ParameterNames.GRM_SERVER_CACHE_FILE, "/dev/null" );
		config.setOverrideProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, badGRMSeedEndping );
		System.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, badGRMSeedEndping );
		try {
			DME2UnitTestUtil.setFinalStatic( GRMEndPointsCache.class.getDeclaredField( "instance" ), null, null );
			logger.debug( null, "testIntegration2_GetGRMEndpoints_BadSeeds", "Starting over with new cache" );
			cache = GRMEndPointsCache.getInstance( config );
			cache.addEndpointURL( badGRMSeedEndping );

			BaseAccessor grmServiceAccessor = GRMAccessorFactory.getGrmAccessorHandlerInstance( config,
					SecurityContext.create( config ) ); //new SoapGRMAccessor(sc, cache);
			GRMEndPointsDiscoveryHelperGRM grmEndPointsDiscoveryHelperGRM =
					new GRMEndPointsDiscoveryHelperGRM( environment, protocol, serviceName, version, grmServiceAccessor, config );

			List<String> grmEndpointsInGRM = grmEndPointsDiscoveryHelperGRM.getGRMEndpoints();
			assertNotNull( grmEndpointsInGRM );
			assertEquals( 0, grmEndpointsInGRM.size() );
		} finally {
			config.setOverrideProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE, cacheFile );
			config.setOverrideProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, dnsBootstrap );
			System.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, dnsBootstrap );
			System.setProperty(  DME2ParameterNames.GRM_SERVER_CACHE_FILE, cacheFile );
			DME2UnitTestUtil.setFinalStatic( GRMEndPointsCache.class.getDeclaredField( "instance" ), null, null );
		}
	}

	private List<String> getSeedEndpoint() {
		// get GRM seeds from DNS
		GRMEndPointsDiscoveryHelperDNS grmEndPointsDiscoveryHelperDNS = new GRMEndPointsDiscoveryHelperDNS(dnsName, protocol, port, path);
		List<String> seedEndpoints = grmEndPointsDiscoveryHelperDNS.getGRMEndpoints();
		return seedEndpoints;
	}
}
