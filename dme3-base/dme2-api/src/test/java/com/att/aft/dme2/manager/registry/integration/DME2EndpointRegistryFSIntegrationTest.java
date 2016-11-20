package com.att.aft.dme2.manager.registry.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryType;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.util.DME2URIUtils;

public class DME2EndpointRegistryFSIntegrationTest {
	private static final String DEFAULT_CONTAINER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
	private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
	private static final String DEFAULT_PATH = RandomStringUtils.randomAlphanumeric( 10 );
	private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 15 );
	private static final String DEFAULT_VERSION = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 10 );
	private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 16 );
	private static final String DEFAULT_SERVICE = DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
	private static final String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 30 );
	private static final int DEFAULT_PORT = RandomUtils.nextInt( 100 );
	private static final double DEFAULT_LAT = RandomUtils.nextDouble();
	private static final double DEFAULT_LONG = RandomUtils.nextDouble();
	private static final String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphanumeric( 5 );

	@Test
	public void test_fs_registry() throws DME2Exception {
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		try {
			DME2EndpointRegistry registry = DME2EndpointRegistryFactory.getInstance().createEndpointRegistry( DEFAULT_CONTAINER_NAME, new DME2Configuration( DEFAULT_MANAGER_NAME ),
					DME2EndpointRegistryType.FileSystem, DEFAULT_MANAGER_NAME, null );
			registry.publish( DEFAULT_SERVICE, DEFAULT_PATH, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_LAT, DEFAULT_LONG, DEFAULT_PROTOCOL ) ;
			List<DME2Endpoint> endpointList = registry.findEndpoints( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV_CONTEXT, DEFAULT_ROUTE_OFFER );
			assertNotNull( endpointList );
			assertEquals( 1, endpointList.size() );
		} finally {
			System.clearProperty("AFT_ENVIRONMENT");
			System.clearProperty("AFT_LATITUDE");
			System.clearProperty("AFT_LONGITUDE");
			System.clearProperty("DME2.DEBUG");
			System.clearProperty("platform");
			System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
		}
	}
}