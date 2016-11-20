package com.att.aft.dme2.test;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.server.test.ServerControllerLauncher;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2ParameterNames;

public class DME2BaseTestCase2 {

	protected String env = "DEV";
	protected String partner = "partnerTest";
	protected String version = "1.0.0";
	protected String platform = TestConstants.GRM_PLATFORM_TO_USE;

	// *********
	protected ServerControllerLauncher serverLaunchers[];
	protected DME2Manager[] managers;

	static {
		final InputStream inputStream = DME2BaseTestCase.class.getResourceAsStream("/console.logging.properties");
		/*	try {
			LogManager.getLogManager().readConfiguration(inputStream);
			System.out.println("Successfully loaded console.logging.properties");
		} catch (final IOException e) {
			Logger.getAnonymousLogger().severe("Could not load default console.logging.properties file");
			Logger.getAnonymousLogger().severe(e.getMessage());
		}*/
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("+++++++++++ initializing test class +++++++++++");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", "SANDBOX-DEV");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		/*System.setProperty(DME2Constants.DME2_GRM_USER, DME2Constants.DME2_GRM_USER);
		System.setProperty(DME2Constants.DME2_GRM_PASS, DME2Constants.DME2_GRM_PASS);*/
	}

	protected static void setupGRMDNSDiscovery() {
		System.setProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE, "logs/dme2grmendpoints.txt");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		System.out.println("+++++++++++ tearing down test class +++++++++++");
		System.clearProperty("AFT_ENVIRONMENT");
		System.clearProperty("AFT_LATITUDE");
		System.clearProperty("AFT_LONGITUDE");
		System.clearProperty("DME2.DEBUG");
		System.clearProperty("platform");
		System.clearProperty(DME2Constants.DME2_GRM_USER);
		System.clearProperty(DME2Constants.DME2_GRM_PASS);
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("+++++++++++ initializing method +++++++++++");
	}

	@After
	public void tearDown() throws Exception {
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

	/****
	 * Logic to build route info
	 */
	protected RouteInfo addRouteInfo(String serviceName) {
		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setServiceName(serviceName);
		routeInfo.setServiceVersion(version);
		routeInfo.setEnvContext(env);
		RouteGroups routeGroups = new RouteGroups();
		routeInfo.setRouteGroups(routeGroups);
		return routeInfo;
	}

	protected RouteGroup addRouteGroup(RouteGroups routeGroups, String name, String partner) {
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName(name);
		routeGroup.getPartner().add(partner);
		routeGroups.getRouteGroup().add(routeGroup);
		return routeGroup;
	}

	protected Route addRoute(RouteGroup routeGroup, String name) {
		return addRoute(routeGroup, name, null);
	}

	protected Route addRoute(RouteGroup routeGroup, String name, String stickySelectorKey) {
		Route route = new Route();
		route.setStickySelectorKey(stickySelectorKey);
		route.setName(name);
		routeGroup.getRoute().add(route);
		return route;
	}

	protected RouteOffer addRouteOffer(Route route, String name, int sequence) {
		RouteOffer routeOffer = new RouteOffer();
		routeOffer.setActive(true);
		routeOffer.setName(name);
		routeOffer.setSequence(sequence);
		route.getRouteOffer().add(routeOffer);
		return routeOffer;
	}

	// ********************************************************
	protected String [] buildSCArgs(String service, String serviceCity, String serverId) throws UnknownHostException {
		String[] args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(), "-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=" + service, "-serviceCity", serviceCity, "-serverid", serverId, "-platform", platform };
		return args;
	}

	protected void startServers() {
		for (ServerControllerLauncher serverLauncher : serverLaunchers) {
			serverLauncher.launch();
		}
	}

	protected void destroyServers() {
		for (ServerControllerLauncher serverLauncher : serverLaunchers) {
			try {
				serverLauncher.destroy();
			} catch (Exception e) {
			}

		}
	}

	protected void createManagers(int number, Properties props) throws DME2Exception {
		managers = new DME2Manager[number + 1]; // create one more for client!
		for (int i = 0; i <= number; i++) {
			managers[i] = new DME2Manager("Manager" + i, props);
		}
	}

	protected void stopManagers() {
		if (managers != null) {
			for (DME2Manager manager : managers) {
				try {
					manager.stop();
					// manager.shutdown();
				} catch (Exception e) {
				}
			}
		}
	}

	protected String buildSearchURI(String service, String partner, String stickySelectorKey) {
		StringBuffer buffer = new StringBuffer(400);
		buffer.append("http://DME2SEARCH/service=");
		buffer.append(service);
		if (partner!=null) {
			buffer.append("/partner=");
			buffer.append(partner);
		}
		if (stickySelectorKey != null) {
			buffer.append("/stickySelectorKey=");
			buffer.append(stickySelectorKey);
		}
		return buffer.toString();
	}
}
