/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.test.DME2BaseTestCase2;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.types.DataPartition;
import com.att.aft.dme2.types.DataPartitions;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.util.DME2Constants;

@Ignore
public class TestDME2RouteOfferStickyKeyOverride extends DME2BaseTestCase2 {

	final static String serviceNameBase = "com.att.aft.dme2.TestDME2RouteOfferStickeyKeyOverride";
	private Properties props;
	private RegistryFsSetup grmInit;

	@Before
	public void setUp() {
		System.setProperty("DME2_EP_TTL_MS", "300000");
		System.setProperty("DME2_RT_TTL_MS", "300000");
		System.setProperty("DME2_LEASE_REG_MS", "300000");

		// Create DME2Manager for clients
		Locations.CHAR.set();
		try {
			props = RegistryFsSetup.init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		props.setProperty("platform", platform);
		grmInit = new RegistryFsSetup();
	}

	@After
	public void tearDown() {
		System.clearProperty("DME2_EP_TTL_MS");
		System.clearProperty("DME2_RT_TTL_MS");
		System.clearProperty("DME2_LEASE_REG_MS");
		StaticCache.reset();
		grmInit = null;
		props = null;
	}

	@Test
	@Ignore
	public void test_1_RequestStickyKeyDifferentWithPreferredRouteOffer() throws Exception {
		DME2Manager manager = null;
		String serviceName = serviceNameBase + "1";
		String service = serviceName + "/version=" + version + "/envContext=" + env;

		try {
			serverLaunchers = new ServerControllerLauncher[6];
			serverLaunchers[0] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=1", "BHAM", "server1"));
			serverLaunchers[1] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=2", "BHAM", "server2"));
			serverLaunchers[2] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=3", "BHAM", "server3"));
			serverLaunchers[3] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=4", "BHAM", "server4"));
			serverLaunchers[4] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=5", "BHAM", "server5"));
			serverLaunchers[5] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=6", "BHAM", "server6"));
			startServers();
			Thread.sleep(5000);

			// Save the route info in GRM
			RouteInfo rtInfo = createRouteInfo_1_RequestStickyKeyDifferentWithPreferredRouteOffer(serviceName);
//			grmInit.saveRouteInfoInGRM(DME2Manager.getDefaultInstance().getConfig(), rtInfo, env);

			props.put("AFT_DME2_PREFERRED_ROUTEOFFER", "6");
      props.put( "SCLD_PLATFORM", platform ); // "SANDBOX-LAB" );
			StaticCache.getInstance().setRouteOffer("6");

			// Create DME2Manager for clients
			manager = new DME2Manager("TestDME2RouteOfferStickeyKeyOverride", props);
			String uriStr = buildSearchURI(service, partner, "1");
			System.out.println("Seaching for URI=" + uriStr);
			EchoReplyHandler replyHandler = send(manager, uriStr);

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertNotNull(reply);
			assertTrue(reply.contains("routeOffer=6"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			destroyServers();
		}
	}

	@Test
	@Ignore
	public void test_2_RequestStickyKeyMatchesWithPreferredRouteOffer() throws Exception {
		DME2Manager manager = null;
		String serviceName = serviceNameBase + "2";
		String service = serviceName + "/version=" + version + "/envContext=" + env;

		try {
			serverLaunchers = new ServerControllerLauncher[3];
			serverLaunchers[0] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=1", "BHAM", "server1"));
			serverLaunchers[1] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=2", "BHAM", "server2"));
			serverLaunchers[2] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=3", "BHAM", "server3"));
			startServers();
			Thread.sleep(5000);

			// Save the route info in GRM
			RouteInfo rtInfo = createRouteInfo_2_RequestStickyKeyMatchesWithPreferredRouteOffer(serviceName);
//			grmInit.saveRouteInfoInGRM(DME2Manager.getDefaultInstance().getConfig(), rtInfo, env);

			StaticCache.getInstance().setRouteOffer("3");
			props.put("AFT_DME2_PREFERRED_ROUTEOFFER", "3");
      props.put( "SCLD_PLATFORM", platform ); //"SANDBOX-LAB" );

			// Create DME2Manager for clients
			manager = new DME2Manager("TestDME2RouteOfferStickeyKeyOverride", props);
			String uriStr = "http://DME2SEARCH/service=" + service + "/stickySelectorKey=1/partner=" + partner;
			EchoReplyHandler replyHandler = send(manager, uriStr);

			String reply = replyHandler.getResponse(60000);
			System.err.println("reply: " + reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println("traceInfo: " + traceInfo);

			assertNotNull(reply);
			assertTrue(reply.contains("routeOffer=3"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			destroyServers();
		}
	}

	@Test
	@Ignore
	public void test_3_NoStickyKeyOnPrefferedRouteOffer() throws Exception {
		DME2Manager manager = null;
		String serviceName = serviceNameBase + "3";
		String service = serviceName + "/version=" + version + "/envContext=" + env;

		try {
			serverLaunchers = new ServerControllerLauncher[6];
			serverLaunchers[0] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=1", "BHAM", "server1"));
			serverLaunchers[1] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=2", "BHAM", "server2"));
			serverLaunchers[2] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=3", "BHAM", "server3"));
			serverLaunchers[3] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=4", "BHAM", "server4"));
			serverLaunchers[4] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=5", "BHAM", "server5"));
			serverLaunchers[5] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=6", "BHAM", "server6"));
			startServers();
			Thread.sleep(5000);

			// Save the route info in GRM
			RouteInfo rtInfo = createRouteInfo_3_NoStickyKeyOnPrefferedRouteOffer(serviceName);
//			grmInit.saveRouteInfoInGRM(DME2Manager.getDefaultInstance().getConfig(), rtInfo, env);

			props.put( DME2Constants.Iterator.AFT_DME2_PREFERRED_ROUTEOFFER, "6");
      props.put( "SCLD_PLATFORM", platform );
			StaticCache.getInstance().setRouteOffer("6");

			// Create DME2Manager for clients
			manager = new DME2Manager("TestDME2RouteOfferStickeyKeyOverride", props);
			String uriStr = buildSearchURI(service, partner, "1");
			EchoReplyHandler replyHandler = send(manager, uriStr);

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertNotNull(reply);
			assertTrue(reply.contains("routeOffer=6"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			destroyServers();
		}
	}

	@Test
	public void test_4_InactivePrefferedRouteOffer() throws Exception {
		DME2Manager manager = null;
		String serviceName = serviceNameBase + "4";
		String service = serviceName + "/version=" + version + "/envContext=" + env;

		try {
			serverLaunchers = new ServerControllerLauncher[3];
			serverLaunchers[0] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=1", "BHAM", "server1"));
			serverLaunchers[1] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=2", "BHAM", "server2"));
			serverLaunchers[2] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=3", "BHAM", "server3"));
			startServers();
			Thread.sleep(5000);

			// Save the route info in GRM
			RouteInfo rtInfo = createRouteInfo_4_InactivePrefferedRouteOffer(serviceName);
//			grmInit.saveRouteInfoInGRM(DME2Manager.getDefaultInstance().getConfig(), rtInfo, env);

			// set preferred route offer
			StaticCache.getInstance().setRouteOffer("3");
			props.put("AFT_DME2_PREFERRED_ROUTEOFFER", "3");
      props.put("SCLD_PLATFORM", platform);

			// Create DME2Manager for clients
			manager = new DME2Manager("TestDME2RouteOfferStickeyKeyOverride", props);

			StaticCache.getInstance().setRouteOffer("3");
			String uriStr = buildSearchURI(service, partner, "1");
			EchoReplyHandler replyHandler = send(manager, uriStr);

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertNotNull(reply);
			assertTrue(reply.contains("routeOffer=1"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			destroyServers();
		}
	}

/*
	@Test
	public void test_5_DataPartition() throws Exception {
		DME2Manager manager = null;
		String serviceName = serviceNameBase + "5";
		String service = serviceName + "/version=" + version + "/envContext=" + env;

		try {
			serverLaunchers = new ServerControllerLauncher[3];
			serverLaunchers[0] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=1", "BHAM", "server1"));
			serverLaunchers[1] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=2", "BHAM", "server2"));
			serverLaunchers[2] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=3", "BHAM", "server3"));
			startServers();
			Thread.sleep(5000);

			// Save the route info in GRM
			RouteInfo rtInfo = createRouteInfo_5_DataPartition(serviceName);
			grmInit.saveRouteInfoInGRM(rtInfo, env);

			// set preferred route offer
			StaticCache.getInstance().setRouteOffer("3");
			props.put("AFT_DME2_PREFERRED_ROUTEOFFER", "3");

			// Create DME2Manager for clients
			manager = new DME2Manager("TestDME2RouteOfferStickeyKeyOverride", props);

			StaticCache.getInstance().setRouteOffer("3");
			String uriStr = buildSearchURI(service, partner, "1") + "/dataContext=300";
			EchoReplyHandler replyHandler = send(manager, uriStr);

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertNotNull(reply);
			assertTrue(reply.contains("routeOffer=3"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			destroyServers();
		}
	}
*/

	@Test
	@Ignore
	public void testNoPreferredRouteOffer() throws Exception {
    System.setProperty( "SCLD_PLATFORM", platform ); //"SANDBOX-LAB" );
    DME2Manager manager = null;
		String serviceName = serviceNameBase + "NPR";
		String service = serviceName + "/version=" + version + "/envContext=" + env;

		try {
			serverLaunchers = new ServerControllerLauncher[3];
			serverLaunchers[0] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=RO_A", "BHAM", "bham_1_bau_se"));
			serverLaunchers[1] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=RO_B", "BHAM", "bham_2_bau_se"));
			serverLaunchers[2] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=RO_C", "CHAR", "char_1_bau_se"));
			startServers();
			Thread.sleep(5000);

			// Save the route info
			RouteInfo rtInfo = createRouteInfo_0(serviceName);
//			grmInit.saveRouteInfoInGRM(DME2Manager.getDefaultInstance().getConfig(), rtInfo, env);

			// Create DME2Manager for clients
			manager = new DME2Manager("TestDME2RouteOfferStickeyKeyOverride", props);
			String uriStr = "http://DME2SEARCH/service=" + service + "/stickySelectorKey=SK1/partner=" + partner;
			EchoReplyHandler replyHandler = send(manager, uriStr);

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertNotNull(reply);
			assertTrue(reply.contains("routeOffer=RO_A") || reply.contains("routeOffer=RO_B"));
		} finally {
			destroyServers();
		}
	}

	@Test
	@Ignore
	public void testWithPreferredRouteOffer() throws Exception {
		DME2Manager manager = null;
		String serviceName = serviceNameBase + "PR";
		String service = serviceName + "/version=" + version + "/envContext=" + env;

		try {
			serverLaunchers = new ServerControllerLauncher[3];
			serverLaunchers[0] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=RO_A", "BHAM", "bham_1_bau_se"));
			serverLaunchers[1] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=RO_B", "BHAM", "bham_2_bau_se"));
			serverLaunchers[2] = new ServerControllerLauncher(buildSCArgs(service + "/routeOffer=RO_C", "CHAR", "char_1_bau_se"));
			startServers();
			Thread.sleep(5000);

			// Save the route info
			RouteInfo rtInfo = createRouteInfo_0(serviceName);
//			grmInit.saveRouteInfoInGRM(DME2Manager.getDefaultInstance().getConfig(), rtInfo, env);

			// Create DME2Manager for clients
			props.put("AFT_DME2_PREFERRED_ROUTEOFFER", "RO_C");
      props.put( "SCLD_PLATFORM", platform ); //"SANDBOX-LAB" );
			manager = new DME2Manager("TestDME2RouteOfferStickeyKeyOverride", props);

			// /dataContext=205977
			String uriStr = "http://DME2SEARCH/service=" + service + "/stickySelectorKey=SK1/partner=" + partner;
			StaticCache.getInstance().setRouteOffer("RO_C");

			EchoReplyHandler replyHandler = send(manager, uriStr);

			String reply = replyHandler.getResponse(60000);
			System.err.println("reply: " + reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println("traceInfo: " + traceInfo);

			assertNotNull(reply);
			assertTrue(reply.contains("routeOffer=RO_C"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			destroyServers();
		}
	}

	private RouteInfo createRouteInfo_0(String serviceName) {
		RouteInfo routeInfo = addRouteInfo(serviceName);
		RouteGroup routeGroup1 = addRouteGroup(routeInfo.getRouteGroups(), "RG1", partner);

		Route route1 = addRoute(routeGroup1, "1");
		Route route2 = addRoute(routeGroup1, "2");
		route1.setStickySelectorKey("SK1");

		addRouteOffer(route1, "RO_A", 1);
		addRouteOffer(route1, "RO_B", 1);
		addRouteOffer(route2, "RO_C", 1);

		return routeInfo;
	}

	@SuppressWarnings("unused")
	private RouteInfo createRouteInfo_1_RequestStickyKeyDifferentWithPreferredRouteOffer(String serviceName) {
		RouteInfo routeInfo = addRouteInfo(serviceName);
		RouteGroup routeGroup1 = addRouteGroup(routeInfo.getRouteGroups(), "1", partner);

		Route route1 = addRoute(routeGroup1, "1");
		Route route2 = addRoute(routeGroup1, "2");
		Route route3 = addRoute(routeGroup1, "3");
		route1.setStickySelectorKey("1");
		route2.setStickySelectorKey("3");
		route3.setStickySelectorKey("6");

		RouteOffer ro1 = addRouteOffer(route1, "1", 1);
		RouteOffer ro2 = addRouteOffer(route1, "2", 2);
		RouteOffer ro3 = addRouteOffer(route2, "3", 1);
		RouteOffer ro4 = addRouteOffer(route2, "4", 2);
		RouteOffer ro5 = addRouteOffer(route3, "5", 2);
		RouteOffer ro6 = addRouteOffer(route3, "6", 3); // preferred route offer
		return routeInfo;
	}

	@SuppressWarnings("unused")
	private RouteInfo createRouteInfo_2_RequestStickyKeyMatchesWithPreferredRouteOffer(String serviceName) {
		RouteInfo routeInfo = addRouteInfo(serviceName);
		RouteGroup routeGroup1 = addRouteGroup(routeInfo.getRouteGroups(), "2", partner);

		Route route1 = addRoute(routeGroup1, "1");
		route1.setStickySelectorKey("1");

		RouteOffer ro1 = addRouteOffer(route1, "1", 1);
		RouteOffer ro2 = addRouteOffer(route1, "2", 2);
		RouteOffer ro3 = addRouteOffer(route1, "3", 1); // preferred route offer

		return routeInfo;
	}

	@SuppressWarnings("unused")
	private RouteInfo createRouteInfo_3_NoStickyKeyOnPrefferedRouteOffer(String serviceName) {
		RouteInfo routeInfo = addRouteInfo(serviceName);
		RouteGroup routeGroup1 = addRouteGroup(routeInfo.getRouteGroups(), "1", partner);

		Route route1 = addRoute(routeGroup1, "1");
		Route route2 = addRoute(routeGroup1, "2");
		Route route3 = addRoute(routeGroup1, "3");
		route1.setStickySelectorKey("1");
		route2.setStickySelectorKey("3");

		RouteOffer ro1 = addRouteOffer(route1, "1", 1);
		RouteOffer ro2 = addRouteOffer(route1, "2", 2);
		RouteOffer ro3 = addRouteOffer(route2, "3", 1);
		RouteOffer ro4 = addRouteOffer(route2, "4", 2);
		RouteOffer ro5 = addRouteOffer(route3, "5", 2);
		RouteOffer ro6 = addRouteOffer(route3, "6", 3); // preferred route offer
		return routeInfo;
	}

	@SuppressWarnings("unused")
	private RouteInfo createRouteInfo_4_InactivePrefferedRouteOffer(String serviceName) {
		RouteInfo routeInfo = addRouteInfo(serviceName);
		RouteGroup routeGroup1 = addRouteGroup(routeInfo.getRouteGroups(), "2", partner);

		Route route1 = addRoute(routeGroup1, "1");
		route1.setStickySelectorKey("1");

		RouteOffer ro1 = addRouteOffer(route1, "1", 1);
		RouteOffer ro2 = addRouteOffer(route1, "2", 2);
		RouteOffer ro3 = addRouteOffer(route1, "3", 1); // preferred route offer
		ro3.setActive(false);

		return routeInfo;
	}

	@SuppressWarnings("unused")
	private RouteInfo createRouteInfo_5_DataPartition(String serviceName) {
		RouteInfo routeInfo = addRouteInfo(serviceName);
		DataPartition dp1 = buildDataPartition("X", "100", "300");
		DataPartition dp2 = buildDataPartition("Y", "400", "600");
		DataPartitions dps = buildDataPartitions(dp1, dp2);
		routeInfo.setDataPartitions(dps);
		RouteGroup routeGroup1 = addRouteGroup(routeInfo.getRouteGroups(), "2", partner);
		routeInfo.setDataPartitionKeyPath("/x/y/z");

		Route route1 = addRoute(routeGroup1, "1");
		route1.setStickySelectorKey("1");
		route1.getDataPartitionRef().add("X");
		Route route2 = addRoute(routeGroup1, "2");
		route2.getDataPartitionRef().add("Y");

		RouteOffer ro1 = addRouteOffer(route1, "1", 1);
		RouteOffer ro2 = addRouteOffer(route1, "2", 2);
		RouteOffer ro3 = addRouteOffer(route2, "3", 1); // preferred route offer
		ro3.setActive(false);

		return routeInfo;
	}

	public static DataPartitions buildDataPartitions(DataPartition... partitions){
		DataPartitions dp = new DataPartitions();
		dp.getDataPartition().addAll(Arrays.asList(partitions));
		return dp;
	}

	public static DataPartition buildDataPartition(String name, String low, String high){
		DataPartition partition = new DataPartition();
		partition.setName(name);
		partition.setLow(low);
		partition.setHigh(high);
		return partition;
	}

    protected EchoReplyHandler send(DME2Manager manager, String uriStr) throws DME2Exception, URISyntaxException {
		EchoReplyHandler replyHandler = new EchoReplyHandler();

		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		sender.addHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS", "com.att.aft.dme2.server.test.PreferredRouteForceRequestHandler");
		sender.setPayload("this is a test");
		sender.setReplyHandler(replyHandler);
		sender.send();

		return replyHandler;
	}
}
