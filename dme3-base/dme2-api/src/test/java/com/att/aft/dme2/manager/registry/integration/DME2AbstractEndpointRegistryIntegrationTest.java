package com.att.aft.dme2.manager.registry.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2AbstractEndpointRegistry;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryType;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.manager.registry.DME2StaleCache;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.scld.grm.types.v1.ClientJVMInstance;

public class DME2AbstractEndpointRegistryIntegrationTest {
	private static final String DEFAULT_ENDPOINT_URL = RandomStringUtils.randomAlphanumeric( 30 );
	private static final Long DEFAULT_ENDPOINT_EXPIRE_TIME = RandomUtils.nextLong();
	private static final String DEFAULT_ROUTE_OFFER_URL = RandomStringUtils.randomAlphanumeric( 30 );
	private static final Long DEFAULT_ROUTE_OFFER_EXPIRE_TIME = RandomUtils.nextLong();
	private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );

	private DME2Configuration configuration;

	@Before
	public void setUpTest() {
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		configuration = new DME2Configuration( DEFAULT_MANAGER_NAME );
	}
	
	@After
	public void tearDownTest() {
		System.clearProperty("AFT_ENVIRONMENT");
		System.clearProperty("AFT_LATITUDE");
		System.clearProperty("AFT_LONGITUDE");
		System.clearProperty("DME2.DEBUG");
		System.clearProperty("platform");
		System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
	}

	@Test
	public void test_endpoint_expiration() throws DME2Exception {
		SimpleEndpointRegistry registry = new SimpleEndpointRegistry( configuration, DEFAULT_MANAGER_NAME );
		DME2StaleCache staleEndpointCache = new DME2StaleCache( configuration, DME2Endpoint.class, DME2EndpointRegistryType.FileSystem, registry, DEFAULT_MANAGER_NAME );
		registry.setStaleEndpointCache( staleEndpointCache );

		// Add two stale endpoints
		registry.addStaleEndpoint( DEFAULT_ENDPOINT_URL, DEFAULT_ENDPOINT_EXPIRE_TIME );
		registry.addStaleEndpoint( DEFAULT_ENDPOINT_URL + "s", DEFAULT_ENDPOINT_EXPIRE_TIME  + 1);

		// Check that they are both stale
		assertTrue( registry.isEndpointStale( DEFAULT_ENDPOINT_URL ) );
		assertTrue( registry.isEndpointStale( DEFAULT_ENDPOINT_URL + "s" ) );

		// Check that the expiration times are correct
		assertEquals( DEFAULT_ENDPOINT_EXPIRE_TIME, registry.getEndpointExpirationTime( DEFAULT_ENDPOINT_URL ) );
		assertEquals( (Long)(DEFAULT_ENDPOINT_EXPIRE_TIME + 1), registry.getEndpointExpirationTime( DEFAULT_ENDPOINT_URL + "s" ));

		// Remove one
		registry.removeStaleEndpoint( DEFAULT_ENDPOINT_URL );

		// Ensure that it was removed
		assertFalse( registry.isEndpointStale( DEFAULT_ENDPOINT_URL ));
		assertNull( registry.getEndpointExpirationTime( DEFAULT_ENDPOINT_URL ));

		// Ensure the other was not removed
		assertTrue( registry.isEndpointStale( DEFAULT_ENDPOINT_URL + "s" ));
		assertEquals( (Long)(DEFAULT_ENDPOINT_EXPIRE_TIME + 1), registry.getEndpointExpirationTime( DEFAULT_ENDPOINT_URL + "s" ));

		// Now clear the cache
		registry.clearStaleEndpoints();

		// Assert the other was removed
		assertFalse( registry.isEndpointStale( DEFAULT_ENDPOINT_URL ));
		assertNull( registry.getEndpointExpirationTime( DEFAULT_ENDPOINT_URL ));
		assertFalse( registry.isEndpointStale( DEFAULT_ENDPOINT_URL + "s" ));
		assertNull( registry.getEndpointExpirationTime( DEFAULT_ENDPOINT_URL + "s" ));
	}

	@Test
	public void test_routeoffer_expiration() throws Exception {
		SimpleEndpointRegistry registry = new SimpleEndpointRegistry( configuration, DEFAULT_MANAGER_NAME );
		DME2UnitTestUtil
		.setFinalStatic( DME2AbstractEndpointRegistry.class.getDeclaredField( "ROUTEOFFER_STALE_PERIOD_IN_MS" ), null, null  );
		DME2StaleCache staleEndpointCache = new DME2StaleCache( configuration, DME2RouteOffer.class, DME2EndpointRegistryType.FileSystem, registry, DEFAULT_MANAGER_NAME );
		registry.setStaleRouteOfferCache( staleEndpointCache );

		// Add two stale endpoints
		registry.addStaleRouteOffer( DEFAULT_ROUTE_OFFER_URL, DEFAULT_ROUTE_OFFER_EXPIRE_TIME );
		registry.addStaleRouteOffer( DEFAULT_ROUTE_OFFER_URL + "s", DEFAULT_ROUTE_OFFER_EXPIRE_TIME + 1 );

		// Check that they are both stale
		assertTrue( registry.isRouteOfferStale( DEFAULT_ROUTE_OFFER_URL ) );
		assertTrue( registry.isRouteOfferStale( DEFAULT_ROUTE_OFFER_URL + "s" ) );

		// Check that the expiration times are correct
		assertEquals( DEFAULT_ROUTE_OFFER_EXPIRE_TIME, registry.getRouteOfferExpirationTime( DEFAULT_ROUTE_OFFER_URL ) );
		assertEquals( (Long)(DEFAULT_ROUTE_OFFER_EXPIRE_TIME + 1), registry.getRouteOfferExpirationTime(
				DEFAULT_ROUTE_OFFER_URL + "s" ));

		// Remove one
		registry.removeStaleRouteOffer( DEFAULT_ROUTE_OFFER_URL );

		// Ensure that it was removed
		assertFalse( registry.isRouteOfferStale( DEFAULT_ROUTE_OFFER_URL ));
		assertNull( registry.getRouteOfferExpirationTime( DEFAULT_ROUTE_OFFER_URL ));

		// Ensure the other was not removed
		assertTrue( registry.isRouteOfferStale( DEFAULT_ROUTE_OFFER_URL + "s" ));
		assertEquals( (Long)(DEFAULT_ROUTE_OFFER_EXPIRE_TIME + 1), registry.getRouteOfferExpirationTime(
				DEFAULT_ROUTE_OFFER_URL + "s" ));

		// Now clear the cache
		registry.clearStaleRouteOffers();

		// Assert the other was removed
		assertFalse( registry.isRouteOfferStale( DEFAULT_ROUTE_OFFER_URL ));
		assertNull( registry.getRouteOfferExpirationTime( DEFAULT_ROUTE_OFFER_URL ));
		assertFalse( registry.isRouteOfferStale( DEFAULT_ROUTE_OFFER_URL + "s" ));
		assertNull( registry.getRouteOfferExpirationTime( DEFAULT_ROUTE_OFFER_URL + "s" ));
	}

	class SimpleEndpointRegistry extends DME2AbstractEndpointRegistry {
		Logger logger = LoggerFactory.getLogger( SimpleEndpointRegistry.class );

		public SimpleEndpointRegistry( DME2Configuration configuration, String managerName ) throws DME2Exception {
			super( configuration, managerName );
		}

		@Override
		public void publish( String service, String path, String host, int port, double latitude, double longitude,
				String protocol ) throws DME2Exception {

		}

		@Override
		public void publish( String service, String path, String host, int port, double latitude, double longitude,
				String protocol, boolean updateLease ) throws DME2Exception {

		}

		@Override
		public void publish( String service, String path, String host, int port, String protocol, Properties props )
				throws DME2Exception {

		}

		@Override
		public void publish( String service, String path, String host, int port, String protocol ) throws DME2Exception {

		}

		@Override
		public void publish( String service, String path, String host, int port, String protocol, boolean updateLease )
				throws DME2Exception {

		}

		@Override
		public void unpublish( String serviceName, String host, int port ) throws DME2Exception {

		}

		@Override
		public List<DME2Endpoint> findEndpoints( String serviceName, String serviceVersion, String envContext,
				String routeOffer ) throws DME2Exception {
			return null;
		}

		@Override
		public DME2RouteInfo getRouteInfo( String serviceName, String serviceVersion, String envContext )
				throws DME2Exception {
			return null;
		}

		@Override
		public void lease( DME2Endpoint endpoint ) throws DME2Exception {

		}

		@Override
		public void refresh() {

		}

		@Override
		public void shutdown() {

		}

		@Override
		public void publish( String serviceURI, String contextPath, String hostAddress, int port, double latitude,
				double longitude, String protocol, Properties props, boolean updateLease )
						throws DME2Exception {

		}

		@Override
		public DME2Endpoint[] find(String serviceKey, String version, String env,
				String routeOffer) throws DME2Exception {
			return null;
		}

		@Override
		public void registerJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {

		}

		@Override
		public void updateJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {

		}

		@Override
		public void deregisterJVM( String envContext, ClientJVMInstance instanceInfo ) throws DME2Exception {

		}

		@Override
		public List<ClientJVMInstance> findRegisteredJVM( String envContext, Boolean activeOnly, String hostAddress,
				String mechID, String processID ) throws DME2Exception {
			return null;
		}

		public void setStaleEndpointCache( DME2StaleCache cache ) {
			staleEndpointCache = cache;
		}

		public void setStaleRouteOfferCache( DME2StaleCache cache ) {
			staleRouteOfferCache = cache;
		}
	}
}
