package com.att.aft.dme2.test;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.util.DME2ParameterNames;

public abstract class DME2BaseTestCase{
	private static final Logger logger = LoggerFactory.getLogger( DME2BaseTestCase.class );
	/*try TODO
	static{
		final InputStream inputStream = DME2BaseTestCase.class.getResourceAsStream("/console.logging.properties");

		{
		    LogManager.getLogManager().readConfiguration(inputStream);
		    System.out.println("Successfully loaded console.logging.properties");
		}
		catch (final IOException e)
		{
		    Logger.getAnonymousLogger().severe("Could not load default console.logging.properties file");
		    Logger.getAnonymousLogger().severe(e.getMessage());
		}
	}*/



	@BeforeClass
	public static void setUpBeforeClass(){
		System.out.println("+++++++++++ initializing test class +++++++++++");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		// System.setProperty( "SCLD_PLATFORM", TestConstants.GRM_PLATFORM_TO_USE );
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	}

	protected static void setupGRMDNSDiscovery() {
		System.setProperty(DME2ParameterNames.GRM_SERVER_CACHE_FILE, "logs/dme2grmendpoints.txt");
	}

	@AfterClass
	public static void tearDownAfterClass() {
		System.out.println("+++++++++++ tearing down test class +++++++++++");
		System.clearProperty("AFT_LATITUDE");
		System.clearProperty("AFT_LONGITUDE");
		System.clearProperty("AFT_ENVIRONMENT");
		System.clearProperty("platform");
		System.clearProperty(DME2ParameterNames.AFT_DME2_USE_AFT_DISCOVERY);
	}

	@Before
	public void setUp() {
		System.out.println("+++++++++++ initializing method +++++++++++");
	}

	@After
	public void tearDown() {
		System.out.println("+++++++++++ tearing down +++++++++++");
		// @TODO these are not set why are they cleared?
		System.clearProperty("AFT_DME2_PUBLISH_METRICS");
		System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
		System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
		System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		System.clearProperty("DME2_EP_REGISTRY_CLASS");
		System.clearProperty("lrmHost");
		System.clearProperty("lrmRName");
		System.clearProperty("lrmRVer");
		System.clearProperty("lrmRO");
		System.clearProperty("lrmEnv");
	}

	public void cleanPreviousEndpoints( String serviceName, String serviceVersion, String envContext )
			throws Exception {
		System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" ); // Stolen from ServerLauncher
		logger.debug( null, "cleanPreviousEndpoints", LogMessage.METHOD_ENTER );
		DME2Configuration config = new DME2Configuration( serviceName );
		DME2Manager manager = new DME2Manager( serviceName, config );
		BaseAccessor grm = (BaseAccessor) DME2UnitTestUtil
				.getPrivate( DME2EndpointRegistryGRM.class.getDeclaredField( "grm" ),
						( (DME2EndpointRegistryGRM) manager.getEndpointRegistry() ) );
		ServiceEndpoint serviceEndpoint = new ServiceEndpoint();//DME3EndpointUtil.convertToServiceEndpoint( endpoint );
		serviceEndpoint.setName( serviceName );
		serviceEndpoint.setVersion( serviceVersion );
		serviceEndpoint.setEnv( envContext );
		try {
			List<ServiceEndpoint> serviceEndpointList = grm.findRunningServiceEndPoint( serviceEndpoint );
			if ( serviceEndpointList != null ) {
				for ( ServiceEndpoint sep : serviceEndpointList ) {
					logger.debug( null, "cleanPreviousEndpoints", "Removing old endpoint {} {} {}", sep.getName(),
							sep.getHostAddress(), sep.getPort() );
					try {
						manager.getEndpointRegistry()
						.unpublish( "/service=" + sep.getName() + "/envContext=" + envContext + "/version=" + sep.getVersion(),
								sep.getHostAddress(), Integer.valueOf( sep.getPort() ) );
					} catch ( Exception e ) {
						logger.debug( null, "cleanPreviousEndpoints", "Error cleaning endpoint {} {} {}", sep.getName(),
								sep.getHostAddress(), sep.getPort(), e );
					}
				}
			}
		} catch ( Exception e ) {
			logger.debug( null, "cleanPreviousEndpoints", "Error cleaning endpoints", e );
		}

		logger.debug( null, "cleanPreviousEndpoints", LogMessage.METHOD_EXIT );
	}
}
