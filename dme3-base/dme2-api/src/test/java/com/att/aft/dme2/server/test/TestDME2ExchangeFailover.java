package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.FailoverFactory;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.test.PreferredRouteReplyHandler;
import com.att.aft.dme2.test.PreferredRouteRequestHandler;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.util.DME2Constants;

@net.jcip.annotations.NotThreadSafe
public class TestDME2ExchangeFailover extends DME2BaseTestCase {

	private static final Logger logger = LoggerFactory.getLogger(TestDME2ExchangeFailover.class);

	@Before
	public void setUp() {
		super.setUp();
		System.setProperty("AFT_DME2_COLLECT_SERVICE_STATS", "false");
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

	@After
	public void tearDown() {
		super.tearDown();

		System.clearProperty("AFT_DME2_COLLECT_SERVICE_STATS");
		System.clearProperty("com.sun.management.jmxremote.authenticate");
		System.clearProperty("com.sun.management.jmxremote.ssl");
		System.clearProperty("com.sun.management.jmxremote.port");
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

	@Test
	public void testReloadAfterAllEndpointsStale() throws Exception {

		System.setProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE", "false");
		System.setProperty("AFT_DME2_PF_SERVICE_NAME",
				"com.att.aft.TestDME2ExchangeFailover_ReloadAfterAllEndpointsStale");

//		cleanPreviousEndpoints("com.att.aft.TestReloadAfterAllEndpointsStale", "1.0.0", "DEV");
		DME2Manager mgr = null;
		String service = null;

		try {
			DME2Configuration config = new DME2Configuration("testReloadAfterAllEndpointsStale");
			mgr = new DME2Manager("testReloadAfterAllEndpointsStale", config);

			service = "/service=com.att.aft.TestReloadAfterAllEndpointsStale/version=1.0.0/envContext=DEV/routeOffer=D1";
			EchoServlet s = new EchoServlet(service, "1");

			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2SEARCH/service=com.att.aft.TestReloadAfterAllEndpointsStale/version=1.0.0/envContext=DEV/partner=D";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true")
					.withHeader("AFT_DME2_EXCHANGE_FAILOVER_HANDLERS",
							"com.att.aft.dme2.handler.DefaultLoggingFailoverFaultHandler")
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);

			EchoReplyHandler replyHandler = new EchoReplyHandler();
			// sender.setResponseHandlers(replyHandler);

			DME2Payload payload = new DME2TextPayload("this is a test");
			//// sender.setPayload("this is a test");
			String reply = (String) sender.sendAndWait(payload);

			// String reply = replyHandler.getResponse(60000);
			System.out.println("REPLY 1 =" + reply);

			assertTrue(reply != null);
			reply = null;

			// stop server that replied
			mgr.getServer().stop();

			// Fixing this test case to not expect a reload quite right away
			// after
			// all endpoints go stale.
			// Earlier the logic was

			try {
				// request.setPort(10);
				sender = new DME2Client(mgr, request);
				//// sender.setPayload("this is a test");
				reply = (String) sender.sendAndWait(payload);

				System.out.println("REPLY 2=" + reply);

				Thread.sleep(5000);
			} catch (Exception e) {

			}

			// mgr.getServer().setPort(9999);
			mgr.getServer().start();
			mgr.bindServiceListener(service, s);
			Thread.sleep(30000);

			sender = new DME2Client(mgr, request);
			//// sender.setPayload("this is a test");
			reply = (String) sender.sendAndWait(payload);

			System.out.println("REPLY 2=" + reply);

			assertTrue(reply != null);
			reply = null;

			mgr.getServer().stop();
			Thread.sleep(1000);

			sender = new DME2Client(mgr, request);
			//// sender.setPayload("this is a test");

			try {
				reply = (String) sender.sendAndWait(payload);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0703"));
			}

		} catch (Exception e) {

		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");

			try {
				mgr.unbindServiceListener(service);

			} catch (Exception e) {

			}
			try {
				mgr.stop();

			} catch (Exception e) {
			}
			try {
				DME2Manager.getDefaultInstance().stop();
			} catch (Exception e) {

			}
		}

	}

	@Test
	public void testDME2ExchangeFailover() throws Exception {
		DME2Manager mgr1 = new DME2Manager("FSMgr", new DME2Configuration("FSMgr", RegistryFsSetup.init()));
		DME2Manager mgr2 = new DME2Manager("FSMgr2", new DME2Configuration("FSMgr2", RegistryFsSetup.init()));
		DME2Manager mgr3 = new DME2Manager("FSMgr3", new DME2Configuration("FSMgr3", RegistryFsSetup.init()));

		String reply = null;

		String service = "/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1";
		String service1 = "/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO2";
		String service2 = "/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO3";

//		cleanPreviousEndpoints("com.att.aft.ExchangeFailover", "1.0.0", "DEV");
		try {
			// Publishing 3 services that don't really exist on given host.
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8080, "http", null);
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8081, "http", null);
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8082, "http", null);

			EchoServlet s = new EchoServlet(service, "1");
			EchoServlet s1 = new EchoServlet(service1, "1");
			EchoServlet s2 = new EchoServlet(service2, "1");

			mgr2.bindServiceListener(service1, s1);
			mgr3.bindServiceListener(service2, s2);

			Thread.sleep(5000);

			/*
			 * Registered 3 endpoints above that does not have a valid server
			 * instance running. First DME2RESOLVE attempt for FO1, DME2Client
			 * has all endpoints non-stale to start with, tries all endpoints,
			 * fails and marks them stale since endpoints are not really active.
			 * The traceInfo should show all those 3 endpoints being attempted
			 */

			String resUri = "http://DME2RESOLVE/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1";
			Request request = new RequestBuilder(new URI(resUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(resUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client client = new DME2Client(mgr1, request);
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				reply = replyHandler.getResponse(180000); // Should throw
															// Exception
			} catch (Exception e) {
				e.printStackTrace();
				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println("\n traceInfo 1st part -----------"+ traceInfo);
				// The trace info should contain all 3 endpoints with port 8080,
				// 8081 and 8082 attempted.
				assertTrue(traceInfo.contains(":8080") && traceInfo.contains(":8081") && traceInfo.contains(":8082"));
			}

			/*
			 * 2nd attempt from same DME2Client, since all endpoints would have
			 * been marked stale by now, DME2 is supposed to hit all endpoints
			 * stale condition and try everything still validating that stale
			 * endpoints are not ignored when all endpoints stale condition is
			 * met
			 */

			System.out.println("\n 2nd part of the test start -----------");

			replyHandler = new EchoReplyHandler();

			Request request2 = new RequestBuilder(new URI(resUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(resUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			client = new DME2Client(mgr1, request2);
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				reply = replyHandler.getResponse(180000); // Should throw
															// Exception
			} catch (Exception e) {
				e.printStackTrace();
				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println("\n traceInfo 2nd part: " + traceInfo);
				// The trace info should contain all 3 endpoints with port 8080,
				// 8081 and 8082 attempted.
				assertTrue(traceInfo.contains(":8080") && traceInfo.contains(":8081") && traceInfo.contains(":8082"));
			}

			// 3rd attempt from DME2Client instance on same jvm, expected to
			// retry all stale endpoints againt.
			replyHandler = new EchoReplyHandler();

			Request request3 = new RequestBuilder(new URI(resUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(resUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			client = new DME2Client(mgr1, request3);
			// client.setPayload("");
			// client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				reply = replyHandler.getResponse(180000); // Should throw
															// Exception
			} catch (Exception e) {
				e.printStackTrace();
				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");

				// The trace info should contain all 3 endpoints with port 8080,
				// 8081 and 8082 attempted.
				assertTrue(traceInfo.contains(":8080") && traceInfo.contains(":8081") && traceInfo.contains(":8082"));
			}

			/*
			 * 4th attempt uses a DME2SEARCH, primary FO1 route offers are all
			 * inactive, but FO2 ( sequence 2 ) and FO3 ( sequence 3 ) endpoints
			 * are active The request is expected to be processed by active FO2
			 * endpoint
			 */
			String searchUri = "http://DME2SEARCH/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/partner=FO";
			request = new RequestBuilder(new URI(searchUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(searchUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			replyHandler = new EchoReplyHandler();

			client = new DME2Client(mgr1, request);
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				reply = replyHandler.getResponse(180000);
				System.err.println("reply=" + reply);

				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println(traceInfo);
				assertTrue(traceInfo.contains("routeOffer=FO2:onResponseCompleteStatus=200"));
			} catch (Exception e) {
				e.printStackTrace();
				assertNotNull(e);
			}

			// Bringing up an active service endpoint for primary sequence
			// routeOffer FO1
			mgr1.bindServiceListener(service, s);
			Thread.sleep(3000);

			// 5th attempt uses DME2SEARCH, primary route offer FO1 has one
			// active endpoint now The request is expected to be processed by
			// primary route offer FO1
			replyHandler = new EchoReplyHandler();

			searchUri = "http://DME2SEARCH/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/partner=FO";
			request = new RequestBuilder(new URI(searchUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(searchUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			client = new DME2Client(mgr1, request);
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				reply = replyHandler.getResponse(180000);
				System.err.println("reply=" + reply);

				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println(traceInfo);
				assertTrue("Expected traceInfo to contain routeOffer=FO1:onResponseCompleteStatus=200 but was "
						+ traceInfo, traceInfo.contains("routeOffer=FO1:onResponseCompleteStatus=200"));
			} catch (Exception e) {
				e.printStackTrace();
				assertNotNull(e);
			}

			// Shutting down FO2 ( sequence 2 ) and FO1 ( sequence 1 )
			// endpoints.
			try {
				mgr1.unbindServiceListener(service);
			} catch (Exception e) {
			}

			try {
				mgr2.unbindServiceListener(service1);
			} catch (Exception e) {
			}

			/*
			 * 5th attempt uses DME2SEARCH, route offer FO3 has one active
			 * endpoint now The request is expected to be processed by only
			 * active route offer FO3 endpoint
			 */
			replyHandler = new EchoReplyHandler();

			searchUri = "http://DME2SEARCH/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/partner=FO";
			request = new RequestBuilder(new URI(searchUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(searchUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			client = new DME2Client(mgr1, request);
			// client.setPayload("");
			// client.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				reply = replyHandler.getResponse(180000);
				System.err.println("reply=" + reply);

				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println(traceInfo);
				assertTrue(traceInfo.contains("routeOffer=FO3:onResponseCompleteStatus=200"));
			} catch (Exception e) {
				e.printStackTrace();
				assertNotNull(e);
			}

		} finally {
			try {
				mgr1.unbindServiceListener(service2);
			} catch (Exception e) {
			}

			try {
				mgr2.unbindServiceListener(service2);
			} catch (Exception e) {
			}

			try {
				mgr3.unbindServiceListener(service2);
			} catch (Exception e) {
			}
			try {
				mgr1.stop();
			} catch (Exception e) {
			}
			try {
				mgr2.stop();
			} catch (Exception e) {
			}
			try {
				mgr3.stop();
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testFailoverclass() {
		
		FailoverFactory.getInstance().testFailoverFactory();
		Properties props = new Properties();
		props.setProperty("FAILOVER_HANDLER_IMPL", "com.att.aft.dme2.server.test.TestDME2CustomFailoverHandler");
		DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestFailoverclass", props);
		String failoverHandlerClassName = config.getProperty(DME2Constants.FAILOVER_HANDLER_IMPL);
		try {
			FailoverHandler failoverHandler = FailoverFactory.getFailoverHandler(config);
			assertEquals(failoverHandlerClassName, failoverHandler.getClass().getName());
		} catch (DME2Exception e) {
			fail("Exception not expected");
		}
	}

	@Test
	public void testDME2ExchangeFailoverWithCustomFailOverHandler() throws Exception {

		Properties props = RegistryFsSetup.init();
		props.setProperty("FAILOVER_HANDLER_IMPL", "com.att.aft.dme2.server.test.TestDME2CustomFailoverHandler");

		DME2Manager mgr1 = new DME2Manager("FSMgr", new DME2Configuration("FSMgr", props));

		String reply = null;

		String service = "/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1";

//		cleanPreviousEndpoints("com.att.aft.ExchangeFailover", "1.0.0", "DEV");
		try {
			// Publishing 1 services that don't really exist on given host.
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8080, "http", null);
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8081, "http", null);
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8082, "http", null);

			EchoServlet s = new EchoServlet(service, "1");

			Thread.sleep(5000);

			/*
			 * Registered 1 endpoints above that does not have a valid server
			 * instance running.
			 */

			String resUri = "http://DME2RESOLVE/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1";
			Request request = new RequestBuilder(new URI(resUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(resUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client client = new DME2Client(mgr1, request);
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				Thread.sleep(50000);				
				reply = replyHandler.getResponse(60000); // Should throw
															// Exception
			} catch (Exception e) {
				e.printStackTrace();
				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");

				// The trace info should contain endpoint with port 8080,
				// 8081 and 8082 attempted.
				assertTrue(traceInfo.contains(":8080") && traceInfo.contains(":8081") && traceInfo.contains(":8082"));
			}

			try {
				mgr1.unbindServiceListener(service);
			} catch (Exception e) {
			}

		} finally {

			try {
				mgr1.stop();
			} catch (Exception e) {
			}

		}

	}

	@Test
	public void testDME2ExchangeFailoverHandlersOnConnectFailed() throws Exception {
		DME2Manager mgr1 = new DME2Manager("testDME2ExchangeFailoverHandlersOnConnectFailed",
				new DME2Configuration("testDME2ExchangeFailoverHandlersOnConnectFailed", RegistryFsSetup.init()));

//		cleanPreviousEndpoints("com.att.afttest3.DME2ExchangeFailoverHandlersOnConnectFailed", "1.0.0", "DEV");
		String service = "/service=com.att.afttest3.DME2ExchangeFailoverHandlersOnConnectFailed/version=1.0.0/envContext=LAB/routeOffer=FO1";

		try {
			// Publishing 3 services that don't really exist on given host.
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8082, "http", null);
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8083, "http", null);
			mgr1.getEndpointRegistry().publish(service, null, "135.204.107.65", 8084, "http", null);

			Thread.sleep(5000);

			/*
			 * Registered 3 endpoints above that does not have a valid server
			 * instance running. First DME2RESOLVE attempt for FO1, DME2Client
			 * has all endpoints non-stale to start with, tries all endpoints,
			 * fails and marks them stale since endpoints are not really active.
			 * The traceInfo should show all those 3 endpoints being attempted
			 */

			String resUri = "http://DME2RESOLVE/service=com.att.afttest3.DME2ExchangeFailoverHandlersOnConnectFailed/version=1.0.0/envContext=LAB/routeOffer=FO1";

			Request request = new RequestBuilder(new URI(resUri))
					.withReadTimeout(35000).withReturnResponseAsBytes(false).withLookupURL(resUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").withHeader("AFT_DME2_EXCHANGE_FAILOVER_HANDLERS",
							"com.att.aft.dme2.server.test.FailoverTestHandler")
					.build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();
			DME2Client client = new DME2Client(mgr1, request);
			client.setResponseHandlers(replyHandler);
			client.send(new DME2TextPayload(""));

			try {
				Thread.sleep(50000);
				replyHandler.getResponse(3600000); // Should throw Exception
				fail("replyHandler should have thrown an exception but did not.");
			} catch (Exception e) {
				e.printStackTrace();
			}

			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println("trace info is: "+traceInfo);
			String[] ports = new String[] { ":8082", ":8083", ":8084" };

			// get the list of the failed urls from the handler
			Set<String> urls = FailoverTestHandler.getFaultContexts().keySet();
			System.err.println("urls are: "+urls);

			// The list of urls that the handler managed should contain all of
			// our endpoints.
			for (String port : ports) {
				String url = "http://135.204.107.65" + port
						+ "/service=com.att.afttest3.DME2ExchangeFailoverHandlersOnConnectFailed/version=1.0.0/envContext=LAB/routeOffer=FO1";
				assertTrue(
						String.format("Expected to find url %s in the list of failed over endpoints but did not", url),
						urls.contains(url));
				assertTrue(String.format("Expected to find url %s in the trace info but did not", url),
						traceInfo.contains(url));
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			FailoverTestHandler.getFaultContexts().clear();
			try {
				mgr1.stop();
			} catch (Exception e) {
			}
		}

	}

	@Ignore
	@Test
	public void testDME2ExchangeRouteOfferFailoverHandlers() throws Exception {
		ServerControllerLauncher bham_bau_ne_launcher = null;
		ServerControllerLauncher bham_bau_se_launcher = null;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangeRouteOfferFailoverHandlers", "1.0.0", "DEV");
		String[] bham_1_bau_ne_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "24600", */
				"-registryType", "GRM", "-servletClass", "FailoverServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangeRouteOfferFailoverHandlers/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] bham_2_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "24601", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangeRouteOfferFailoverHandlers/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		try {
			Properties props = RegistryGrmSetup.init();
			DME2Configuration config = new DME2Configuration("TestDME2ExchangeRouteOfferFailoverHandlers", props);

			DME2Manager manager = new DME2Manager("TestDME2ExchangeRouteOfferFailoverHandlers", config);
			RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoForRouteOfferFailoverHandlers();

			bham_bau_ne_launcher = new ServerControllerLauncher(bham_1_bau_ne_args);
			bham_bau_se_launcher = new ServerControllerLauncher(bham_2_bau_se_args);

			bham_bau_ne_launcher.launch();
			bham_bau_se_launcher.launch();

			Thread.sleep(15000);

			// Save the route info
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			Locations.CHAR.set();

			// Try to call a service we just registered
			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangeRouteOfferFailoverHandlers/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";

			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_FAILOVER_HANDLERS",
							"com.att.aft.dme2.server.test.FailoverTestHandler")
					.build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();
			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload(""));

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);

			Set<String> failoverURLs = FailoverTestHandler.getFaultContexts().keySet();
			// should only be one url
			assertEquals(1, failoverURLs.size());
			// Arash: This was already failing before changes

			// make sure that the handler was invoked for the BAU_SE route offer
			for (String url : failoverURLs) {
				System.err.println(url);
				assertTrue(url.contains("routeOffer=BAU_NE"));

			}

		} finally {

			try {
				bham_bau_ne_launcher.destroy();
			} catch (Exception e) {

			}

			try {
				bham_bau_se_launcher.destroy();
			} catch (Exception e) {

			}
		}
	}

	/**
	 * Test jmx request.
	 *
	 * @throws Exception
	 */

	@Ignore
	@Test
	public void testDME2WithJMXInterface() throws Exception {
		System.setProperty("com.sun.management.jmxremote.authenticate", "false");
		System.setProperty("com.sun.management.jmxremote.ssl", "false");
		System.setProperty("com.sun.management.jmxremote.port", "51259"); // 51259

		Properties props = new Properties();
		props.setProperty("AFT_DME2_SSL_ENABLE", "true");

		ServerControllerLauncher launcher = null;
		DME2Manager mgr = null;

		try {
			String[] bham_1_bau_se_args = { "-jettyport", "44989", "-serverid", "JMXInterface-Test",
					"-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false",
					"-Dcom.sun.management.jmxremote.port=51259" }; // 51259

			launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			launcher.launchWebServer();

			Thread.sleep(10000);
			DME2Configuration config = new DME2Configuration("testDME2WithJMXInterface", props);
			mgr = new DME2Manager("testDME2WithJMXInterface", config);

			String clientURI = "http://localhost:51259/service=com.att.aft/version=1.0/envContext=TEST/routeOffer=TEST";

			Request request = new RequestBuilder(new URI(clientURI))
					.withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();

			DME2Client client = new DME2Client(mgr, request);

			try {
				client.sendAndWait(new DME2TextPayload("test"));
				Thread.sleep(10000);
			} catch (Exception ex) {
				ex.printStackTrace();
				assertTrue(ex.getMessage().contains("AFT-DME2-0703"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				if (mgr.getServer().isRunning()) {
					mgr.stop();
				}
			} catch (Exception e) {
			}

			try {
				launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Test jmx request
	 *
	 * @throws Exception
	 */

	@Test
	public void testDME2WithJMXInterfaceDisableContentCheck() throws Exception {
		System.setProperty("com.sun.management.jmxremote.authenticate", "false");
		System.setProperty("com.sun.management.jmxremote.ssl", "false");
		System.setProperty("com.sun.management.jmxremote.port", "51278");

		String[] bham_1_bau_se_args = { "-jettyport", "44889", "-serverid", "JMXInterface-Test",
				"-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false",
				"-Dcom.sun.management.jmxremote.port=51278" };

		ServerControllerLauncher launcher = null;

		try {
			launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			launcher.launchWebServer();

			Thread.sleep(10000);

			Properties props = new Properties();
			props.put("AFT_DME2_CLIENT_IGNORE_CONTENT_CHECK", "false");

			// DME2Manager mgr = DME2Manager.getDefaultInstance();
			// mgr.setProperty("AFT_DME2_CLIENT_IGNORE_CONTENT_CHECK", "false");

			DME2Manager mgr = new DME2Manager("JMXInterface-Test", new DME2Configuration("JMXInterface-Test", props));

			String clientURI = "http://localhost:51278/service=com.att.aft/version=1.0/envContext=TEST/routeOffer=TEST";
			Request request = new RequestBuilder(new URI(clientURI))
					.withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();

			DME2Client client = new DME2Client(mgr, request);
			// client.setPayload("test");

			try {
				String reply = (String) client.sendAndWait(new DME2TextPayload("test"));
				System.out.println("REPLY from JMX port: " + reply);
				assertTrue(reply.length() == 1 || reply.contains("NULL") || reply == null);
			} catch (Exception ex) {
				ex.printStackTrace();
				assertFalse(ex.getMessage().contains("AFT-DME2-0703"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			System.clearProperty("com.sun.management.jmxremote.authenticate");
			System.clearProperty("com.sun.management.jmxremote.ssl");
			System.clearProperty("com.sun.management.jmxremote.port");

			try {
				launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangeFailoverSequenceOnSameRequest() throws Exception {
		DME2Manager manager = null;

		/** The bham_1_ launcher. */
		ServerControllerLauncher prim_1_Launcher = null;
		ServerControllerLauncher prim_2_Launcher = null;

		/** The bham_2_ launcher. */
		ServerControllerLauncher second_1_Launcher = null;

		/** The char_1_ launcher. */
		ServerControllerLauncher third_1_Launcher = null;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest", "1.0.0", "DEV");
		try {

			System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);

			Properties props = RegistryGrmSetup.init();

			DME2Configuration config = new DME2Configuration("TestDME2ExchangeFailoverSequenceOnSameRequest1", props);
			manager = new DME2Manager("TestDME2ExchangeFailoverSequenceOnSameRequest1", config);

			RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithFailoverSequenceOnSameRequest();

			// run the primary server.
			String[] prim_1_args = { "-serverHost", InetAddress.getLocalHost()
					.getCanonicalHostName(), /* "-serverPort", "26000", */
					"-registryType", "GRM", "-servletClass", "FailoverServlet", "-serviceName",
					"service=com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest/version=1.0.0/envContext=DEV/routeOffer=PRIM",
					"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform",
					TestConstants.GRM_PLATFORM_TO_USE };

			prim_1_Launcher = new ServerControllerLauncher(prim_1_args);
			prim_1_Launcher.launch();

			String[] prim_2_args = { "-serverHost", InetAddress.getLocalHost()
					.getCanonicalHostName(), /* "-serverPort", "26010", */
					"-registryType", "GRM", "-servletClass", "FailoverServlet", "-serviceName",
					"service=com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest/version=1.0.0/envContext=DEV/routeOffer=PRIM",
					"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform",
					TestConstants.GRM_PLATFORM_TO_USE };

			prim_2_Launcher = new ServerControllerLauncher(prim_2_args);
			prim_2_Launcher.launch();

			Thread.sleep(15000);
			String[] second_args = { "-serverHost", InetAddress.getLocalHost()
					.getCanonicalHostName(), /* "-serverPort", "26001", */
					"-registryType", "GRM", "-servletClass", "FailoverServlet", "-serviceName",
					"service=com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest/version=1.0.0/envContext=DEV/routeOffer=SECOND",
					"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform",
					TestConstants.GRM_PLATFORM_TO_USE };

			second_1_Launcher = new ServerControllerLauncher(second_args);
			second_1_Launcher.launch();

			String[] third_args = { "-serverHost", InetAddress.getLocalHost()
					.getCanonicalHostName(), /* "-serverPort", "26002", */
					"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
					"service=com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest/version=1.0.0/envContext=DEV/routeOffer=THIRD",
					"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform",
					TestConstants.GRM_PLATFORM_TO_USE };

			third_1_Launcher = new ServerControllerLauncher(third_args);
			third_1_Launcher.launch();

			try {
				Thread.sleep(5000);

			} catch (Exception ex) {

			}

			// Save the route info
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			// try to call a service we just registered
			Locations.CHAR.set();

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").withHeader("AFT_DME2_EXCHANGE_FAILOVER_HANDLERS",
							"com.att.aft.dme2.handler.DefaultLoggingFailoverFaultHandler")
					.build();

			DME2Client sender = new DME2Client(manager, request);
			EchoReplyHandler replyHandler = new EchoReplyHandler();
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
			Thread.sleep(50000);
			String reply = replyHandler.getResponse(60000);
			System.out.println("reply1=" + reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertTrue(traceInfo.contains("/routeOffer=THIRD:onResponseCompleteStatus=200"));

			Thread.sleep(5000);

			Request request2 = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			sender = new DME2Client(manager, request2);

			replyHandler = new EchoReplyHandler();
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
			Thread.sleep(50000);
			reply = replyHandler.getResponse(60000);
			System.out.println("reply2=" + reply);

			traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertTrue(traceInfo.contains("/routeOffer=THIRD:onResponseCompleteStatus=200"));

			int threadCount = 15;
			Thread threads[] = new Thread[threadCount];
			for (int i = 0; i < threadCount; i++) {
				threads[i] = new Thread(new TestDME2ClientThread(i + ""));
				threads[i].start();
			}
			try {
				Thread.sleep(15000);
			} catch (Exception ex) {
			}
			for (int i = 0; i < threadCount; i++) {
				threads[i].join();
			}
			Iterator<String> it = TestDME2ClientThread.resultsMap.keySet().iterator();
			boolean failed = false;
			while (it.hasNext()) {
				// System.out.println( "ResultsMap has entry" );
				String key = it.next();
				System.out.println("Thread " + key + " fthird instance" + TestDME2ClientThread.resultsMap.get(key));
				if (TestDME2ClientThread.resultsMap.get(key) != true) {
					if (!failed) {
						failed = true;
					}
					System.out.println("Thread " + key + " failed to get response from third instance");
				}
			}
			// reply should be not null
			if (failed) {
				fail("reply is null or failed to get response from third instance");
			}

		} finally {

			System.clearProperty("platform");

			try {
				if (prim_1_Launcher != null) {
					prim_1_Launcher.destroy();
				}

			} catch (Exception e) {

			}

			try {
				if (prim_2_Launcher != null) {
					prim_2_Launcher.destroy();
				}

			} catch (Exception e) {

			}

			try {
				if (second_1_Launcher != null) {
					second_1_Launcher.destroy();
				}

			} catch (Exception e) {

			}

			try {
				if (third_1_Launcher != null) {
					third_1_Launcher.destroy();
				}

			} catch (Exception e) {

			}
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangePreferredRouteOffer() throws Exception {
    System.setProperty( "platform", "SANDBOX-DEV" );
		DME2Manager manager = null;

		ServerControllerLauncher bham_1_Launcher = null;
		ServerControllerLauncher bham_2_Launcher = null;
		ServerControllerLauncher char_1_Launcher = null;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer", "1.0.0", "DEV");
		String[] bham_1_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "4600", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] bham_2_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort","4601", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] char_1_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "4602", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
				"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		try {

			System.setProperty("DME2_EP_TTL_MS", "300000");
			System.setProperty("DME2_RT_TTL_MS", "300000");
			System.setProperty("DME2_LEASE_REG_MS", "300000");
			System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);

			Properties props = RegistryGrmSetup.init();
			DME2Configuration config = new DME2Configuration("TestDME2ExchangePreferredRouteOffer", props);
			manager = new DME2Manager("TestDME2ExchangePreferredRouteOffer.testDME2ExchangePreferredRouteOffer",
					config);

			RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithPreferredRouteOffer();

			bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
			char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();
			char_1_Launcher.launch();

			Thread.sleep(5000);

			// Save the route info
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			// try to call a service we just registered
			Locations.CHAR.set();

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							PreferredRouteRequestHandler.class.getName())
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredRouteReplyHandler.class.getName())
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();
			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
			Thread.sleep(50000);
			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);

			// stop server that replied
			String otherServer = null;
			Thread.sleep(5000);

			replyHandler = new EchoReplyHandler();

			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", "com.att.aft.dme2.test.PreferredRouteReplyHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
			Thread.sleep(50000);
			reply = replyHandler.getResponse(60000);
			System.err.println("reply***=" + reply);

			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			assertTrue(String.format("Did not find [/routeOffer=BAU_SE:onResponseCompleteStatus=200] in traceInfo : %s",
					traceInfo), traceInfo.contains("/routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			// reply should be not null
			if (reply == null) {
				fail("reply is null or not from the otherServer.  otherServer=" + otherServer + "  reply=" + reply);
			}
		} finally {
			StaticCache.reset();

			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				bham_2_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Ignore	
	@Test
	public void testDME2ExchangeMultipleHandlersList() throws Exception {
		ServerControllerLauncher bham_1_Launcher = null;
		ServerControllerLauncher bham_2_Launcher = null;
		ServerControllerLauncher char_1_Launcher = null;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer", "1.0.0", "DEV");
		String[] bham_1_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "54610", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] bham_2_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "54611", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] char_1_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "54612", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
				"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		try {
			Properties props = RegistryGrmSetup.init();
			System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);

			props.setProperty("DME2_EP_TTL_MS", "300000");
			props.setProperty("DME2_RT_TTL_MS", "300000");
			props.setProperty("DME2_LEASE_REG_MS", "300000");

			DME2Configuration config = new DME2Configuration("testDME2ExchangeMultipleHandlersList", props);
			DME2Manager manager = new DME2Manager("testDME2ExchangeMultipleHandlersList", config);
			RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithPreferredRouteOffer();

			bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
			char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();
			char_1_Launcher.launch();

			Thread.sleep(5000);

			// Save the routeInfo
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			Locations.CHAR.set();

			// Try to call a service we just registered
			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			EchoReplyHandler replyHandler = new EchoReplyHandler();
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteReplyHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.build();

			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);

			assertTrue(reply != null);

			// Stop server that replied
			String otherServer = null;
			Thread.sleep(5000);

			replyHandler = new EchoReplyHandler();
			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteReplyHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(60000);
			System.err.println("reply=" + reply);

			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			// Since preferred route offer is overridden by
			// MessageLoggerHandler, default primary sequence should be used for
			// 2nd request also.
			assertTrue("TraceInfo was expected to contain BAU_NE, but was " + traceInfo,
					traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));

			// Reply should be not null
			if (reply == null) {
				fail("reply is null or not from the otherServer.  otherServer=" + otherServer + "  reply=" + reply);
			}
		} finally {
			StaticCache.reset();

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				bham_2_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2HandlerChainExceptionIgnored() throws Exception {
    System.setProperty( "SCLD_PLATFORM", TestConstants.GRM_PLATFORM_TO_USE );
		ServerControllerLauncher bham_1_Launcher = null;
		ServerControllerLauncher bham_2_Launcher = null;
		ServerControllerLauncher char_1_Launcher = null;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer", "1.0.0", "DEV");
		String[] bham_1_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "54620", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] bham_2_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "54621", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] char_1_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "54622", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
				"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		try {
			Properties props = RegistryGrmSetup.init();
			props.setProperty("DME2_EP_TTL_MS", "300000");
			props.setProperty("DME2_RT_TTL_MS", "300000");
			props.setProperty("DME2_LEASE_REG_MS", "300000");

			DME2Configuration config = new DME2Configuration("testDME2HandlerChainExceptionIgnored", props);
			DME2Manager manager = new DME2Manager("testDME2HandlerChainExceptionIgnored", config);

			// Ignoring the below constructed routeInfo, since there is some
			// parsing issue while marshalling the the routeInfo obj to xml
			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer");
			rtInfo.setEnvContext("DEV");

			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups(rtGrps);

			RouteGroup rg1 = new RouteGroup();
			rg1.setName("RG1");
			rg1.getPartner().add("test1");
			rg1.getPartner().add("test2");
			rg1.getPartner().add("test3");

			Route rt1 = new Route();
			rt1.setName("rt1");

			RouteOffer ro1 = new RouteOffer();
			ro1.setActive(true);
			ro1.setSequence(1);
			ro1.setName("BAU_NE");

			Route rt2 = new Route();
			rt2.setName("rt2");

			RouteOffer ro2 = new RouteOffer();
			ro2.setActive(true);
			ro2.setSequence(2);
			ro2.setName("BAU_SE");

			Route rt3 = new Route();
			rt3.setName("rt3");
			RouteOffer ro3 = new RouteOffer();
			ro3.setActive(true);
			ro3.setSequence(3);
			ro3.setName("BAU_SW");

			rt1.getRouteOffer().add(ro1);
			rt1.getRouteOffer().add(ro2);
			rt1.getRouteOffer().add(ro3);

			rtGrps.getRouteGroup();
			rtGrps.getRouteGroup().add(rg1);

			bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
			char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();
			char_1_Launcher.launch();

			Thread.sleep(5000);

			// Save the route info
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoForPreferedRoute(config, rtInfo, "DEV");

			Locations.CHAR.set();

			String preferredRouteOffer = config.getProperty(DME2Constants.Iterator.AFT_DME2_PREFERRED_ROUTEOFFER);

			// try to call a service we just registered
			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteReplyHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.withHeader("AFT_DME2_TEST_THROW_EXCEPTION", "true").build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();
			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);

			assertTrue(reply != null);

			// Stop server that replied
			String otherServer = null;
			Thread.sleep(5000);

			replyHandler = new EchoReplyHandler();

			Request request2 = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteReplyHandler,com.att.aft.dme2.test.MessageLoggerHandler")
					.withHeader("AFT_DME2_TEST_THROW_EXCEPTION", "true").withHeader("AFT_DME2_REQ_TRACE_ON", "true")
					.build();

			sender = new DME2Client(manager, request2);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(60000);

			System.err.println("reply=" + reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			// Since preferred route offer is overridden by
			// MessageLoggerHandler, default primary sequence should be used for
			// 2nd request also.
			assertTrue(traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));

			// Reply should be not null
			if (reply == null) {
				fail("reply is null or not from the otherServer.  otherServer=" + otherServer + "  reply=" + reply);
			}
		} finally {
			StaticCache.reset();

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				bham_2_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangePreferredRouteOfferFailsOnInvocation() throws Exception {
   //System.setProperty( "SCLD_PLATFORM", "SANDBOX-LAB" );
		ServerControllerLauncher bham_1_Launcher = null;
		ServerControllerLauncher bham_2_Launcher = null;
		ServerControllerLauncher char_1_Launcher = null;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer", "1.0.0", "DEV");
		String[] bham_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "14600", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] bham_2_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				/* "-serverPort", "14601", */"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] char_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "14602", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
				"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		try {

			Properties props = RegistryGrmSetup.init();
			props.setProperty("DME2_EP_TTL_MS", "300000");
			props.setProperty("DME2_RT_TTL_MS", "300000");
			props.setProperty("DME2_LEASE_REG_MS", "300000");

			DME2Configuration config = new DME2Configuration("TestDME2ExchangePreferredRouteOfferFailsOnInvocation",
					props);
			DME2Manager manager = new DME2Manager("TestDME2ExchangePreferredRouteOfferFailsOnInvocation", config);

			bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
			char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();
			char_1_Launcher.launch();

			// Save the route info
			RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithPreferredRouteOffer();
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			Thread.sleep(5000);

			// Try to call a service we just registered
			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes( false ).withLookupURL( uriStr )
					.withHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
              PreferredRouteRequestHandler.class.getName() )
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredRouteReplyHandler.class.getName() )
					.build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			String reply = replyHandler.getResponse(60000);
			System.err.println("Response (Should contain routeOffer BAU_NE): " + reply);
			assertTrue(reply.contains("BAU_NE"));

			System.out.println("RouteOffer contained in static cache: " + StaticCache.getInstance().getRouteOffer());
			StaticCache cache = StaticCache.getInstance();
			System.out.println(cache);
			// Stop server that replied
			String otherServer = null;

			// Destroy the BAU_SE instance so that preferred routeoffer call
			// would fail
			bham_2_Launcher.destroy();
			Thread.sleep(5000);  // We don't want it to mark the routeoffer stale, otherwise it won't even try it!

			assertFalse(StaticCache.getInstance().isHandleEndpointFaultInvoked());
			replyHandler = new EchoReplyHandler();

			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader( "AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
              PreferredRouteRequestHandler.class.getName() )
					.withHeader( "AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredRouteReplyHandler.class.getName() )
					.withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).build();
			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));
			
			reply = replyHandler.getResponse(60000);
			logger.debug(null, "testDME2ExchangePreferredRouteOfferFailsOnInvocation", "reply=" + reply);

			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
      logger.debug(null, "testDME2ExchangePreferredRouteOfferFailsOnInvocation", "=====traceInfo====" + traceInfo);

			assertTrue(traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));
			assertTrue(StaticCache.getInstance().isHandleEndpointFaultInvoked());

			// Reply should be not null
			if (reply == null) {
				fail("Reply is null or not from the otherServer.  otherServer=" + otherServer + "  reply=" + reply);
			}
		} finally {

			StaticCache.reset();

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				bham_2_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangePreferredRouteOfferAndAllROFails() throws Exception {
    //System.setProperty( "SCLD_PLATFORM", "SANDBOX-LAB" );
		ServerControllerLauncher bham_1_Launcher = null;
		ServerControllerLauncher bham_2_Launcher = null;
		ServerControllerLauncher char_1_Launcher = null;

    String platformToUse = TestConstants.GRM_PLATFORM_TO_USE;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails", "1.0.0", "DEV");
		String[] bham_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "24600", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", platformToUse };

		String[] bham_2_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "24601", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", platformToUse };

		String[] char_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "24602", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
				"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform", platformToUse };

		try {
			Properties props = RegistryGrmSetup.init();
			props.setProperty("DME2_EP_TTL_MS", "300000");
			props.setProperty("DME2_RT_TTL_MS", "300000");
			props.setProperty("DME2_LEASE_REG_MS", "300000");

			DME2Configuration config = new DME2Configuration("TestDME2ExchangePreferredRouteOfferAndAllROFails", props);
			DME2Manager manager = new DME2Manager("TestDME2ExchangePreferredRouteOfferAndAllROFails", config);
			RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithPreferredRouteOfferAndAllROFails();

			bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
			char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();
			char_1_Launcher.launch();

			Thread.sleep(15000);

			// Save the route info
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			Locations.CHAR.set();

			// Try to call a service we just registered

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							PreferredRouteRequestHandler.class.getName())
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredRouteReplyHandler.class.getName())
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client sender = new DME2Client(manager, request);

			//// sender.setPayload("this is a test");
			sender.setResponseHandlers(replyHandler);
      logger.debug( null, "testDME2ExchangePreferredRouteOfferAndAllROFails", "first send" );
			sender.send(new DME2TextPayload("this is a test"));

			String reply = replyHandler.getResponse(60000);
			logger.debug( null, "testDME2ExchangePreferredRouteOfferAndAllROFails", "First Reply={}", reply);

			// Destroy the BAU_SE instance so that preferred routeoffer call
			// would fail
      logger.debug( null, "testDME2ExchangePreferredRouteOfferAndAllROFails", "Destroy launcher #2" );
			bham_2_Launcher.destroy();
			logger.debug( null, "testDME2ExchangePreferredRouteOfferAndAllROFails", "Sleeping for {} ms", (config.getLong(DME2Constants.Cache.DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS) - 1000));
     // Thread.sleep( config.getLong(DME2Constants.Cache.DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS) - 1000 );
      Thread.sleep( 7000 );
      // Can't be sure if this will get marked stale, so needs to be removed
     /* if ( manager.isRouteOfferStale( "/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails/version=1.0.0/envContext=DEV/routeOffer=BAU_SE"  )) {
        manager.removeStaleRouteOffer( "/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails/version=1.0.0/envContext=DEV/routeOffer=BAU_SE" );
      }*/
			replyHandler = new EchoReplyHandler();

			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							PreferredRouteRequestHandler.class.getName() )
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredRouteReplyHandler.class.getName() )
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();
			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
      logger.debug( null, "testDME2ExchangePreferredRouteOfferAndAllROFails", "Send second request" );
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(60000);
			logger.debug( null, "testDME2ExchangePreferredRouteOfferAndAllROFails", "EXIT:reply={}", reply);

			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
      logger.debug( null, "testDME2ExchangePreferredRouteOfferAndAllROFails", traceInfo);
			String expected = "/routeOffer=BAU_SE:onConnectionFailed=Connection refused";

			assertTrue(String.format("Did not find expected string [%s] in trace info [%s]", expected, traceInfo),
					traceInfo.contains(expected));
			expected = "/routeOffer=BAU_NE:onResponseCompleteStatus=200";
			assertTrue(String.format("Did not find expected string [%s] in trace info [%s]", expected, traceInfo),
					traceInfo.contains(expected));

			assertTrue(StaticCache.getInstance().isHandleEndpointFaultInvoked());

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			Thread.sleep(10000);
			replyHandler = new EchoReplyHandler();

			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", "com.att.aft.dme2.test.PreferredRouteReplyHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();
			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(20000);
			assertTrue(reply != null);

			replyHandler = new EchoReplyHandler();

			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", "com.att.aft.dme2.test.PreferredRouteReplyHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();
			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			try {
				reply = replyHandler.getResponse(20000);
			} catch (Exception e) {
				// should fail since all endpoints are down
				e.printStackTrace();
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}

			Thread.sleep(10000);
			replyHandler = new EchoReplyHandler();

			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", "com.att.aft.dme2.test.PreferredRouteReplyHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();
			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			try {
				reply = replyHandler.getResponse(20000);
			} catch (Exception e) {
				// should fail since all endpoints are down
				assertTrue(String.format("Expected error code AFT-DME2-0703 not found in exception message: %s",
						e.getMessage()), e.getMessage().contains("AFT-DME2-0703"));
			}
		} finally {

			StaticCache.reset();

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				bham_2_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangePreferredRouteOfferNotFound() throws Exception {
    System.setProperty( "platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
    logger.debug( null, "testDME2ExchangePreferredRouteOfferNotFound", LogMessage.METHOD_ENTER );
		ServerControllerLauncher bham_1_Launcher = null;
		ServerControllerLauncher bham_2_Launcher = null;
		ServerControllerLauncher char_1_Launcher = null;

  	cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer", "1.0.0", "DEV");
		String[] bham_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "34600", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_ne", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] bham_2_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "34601", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] char_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "34602", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
				"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		try {
			Properties props = RegistryGrmSetup.init();
			props.setProperty("DME2_EP_TTL_MS", "300000");
			props.setProperty("DME2_RT_TTL_MS", "300000");
			props.setProperty("DME2_LEASE_REG_MS", "300000");
      props.setProperty( "SCLD_PLATFORM", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);

			DME2Configuration config = new DME2Configuration("TestDME2ExchangePreferredRouteOfferNotFound", props);
			DME2Manager manager = new DME2Manager("TestDME2ExchangePreferredRouteOfferNotFound", config);
			RouteInfo rtInfo = RouteInfoCreatorUtil.createRoutInfoWithPreferredRouteOfferNotFound();

			bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
			char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();
			char_1_Launcher.launch();

			Thread.sleep(5000);

			// Save the route info
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init(TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			// try to call a service we just registered
			Locations.CHAR.set();

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							PreferredRouteRequestHandler.class.getName() )
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredRouteReplyHandler.class.getName() )
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			String reply = replyHandler.getResponse(60000);
      logger.debug( null, "testDME2ExchangePreferredRouteOfferNotFound", reply);

			// stop server that replied
			String otherServer = null;
			Thread.sleep(5000);

			replyHandler = new EchoReplyHandler();

			request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							PreferredRouteRequestHandler.class.getName() )
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", PreferredRouteReplyHandler.class.getName())
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();
			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(60000);
      logger.debug( null, "testDME2ExchangePreferredRouteOfferNotFound", "reply={}", reply);

			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
      logger.debug( null, "testDME2ExchangePreferredRouteOfferNotFound", "TraceInfo={}", traceInfo);

			// Since the preferred routeOffer is not found, default primary
			// sequence BAU_NE should have been used
			assertTrue("Expected /routeOffer=BAU_NE:onResponseCompleteStatus=200 but found" + traceInfo, traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));

			// reply should be not null
			if (reply == null) {
				fail("reply is null or not from the otherServer.  otherServer=" + otherServer + "  reply=" + reply);
			}

		} finally {
			StaticCache.reset();

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				bham_2_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}
      logger.debug( null, "testDME2ExchangePreferredRouteOfferNotFound", LogMessage.METHOD_EXIT );
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangeFailoverWhenPrimaryDownOnStart() throws Exception {
		ServerControllerLauncher bham_1_Launcher = null;
		ServerControllerLauncher bham_2_Launcher = null;
		ServerControllerLauncher char_1_Launcher = null;

		cleanPreviousEndpoints("com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown", "1.0.0", "DEV");
		/* BHAM Service args - BAU_NE - SEQUENCE 1 */
		String[] bham_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "54600", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/routeOffer=BAU_NE",
				"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		// Don't launch the primary routeOffer in start for testing primary seq
		// failover when primary RO endpoints are down on startup

		/* BHAM Service args - BAU_SE - SEQUENCE 2 */
		String[] bham_2_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "54601", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		// Don't launch the secondary routeOffer in start for testing primary
		// seq failover when primary/secondary RO endpoints are down on startup

		/* CHAR Service args - BAU_NW - SEQUENCE 3 */
		String[] char_1_bau_se_args = { "-serverHost",
				InetAddress.getLocalHost()
						.getCanonicalHostName(), /* "-serverPort", "54602", */
				"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
				"service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/routeOffer=BAU_NW",
				"-serviceCity", "CHAR", "-serverid", "char_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

		try {
			Properties props = RegistryGrmSetup.init();
			props.setProperty("DME2_EP_TTL_MS", "300000");
			props.setProperty("DME2_RT_TTL_MS", "300000");
			props.setProperty("DME2_LEASE_REG_MS", "300000");

			DME2Configuration config = new DME2Configuration("TestDME2ExchangeFailoverWhenPrimaryDown", props);
			DME2Manager manager = new DME2Manager("TestDME2ExchangeFailoverWhenPrimaryDown", config);

			/*
			 * Creating Controller Launcher Objects for each server, passing in
			 * associated args.
			 */
			bham_1_Launcher = new ServerControllerLauncher(bham_1_bau_se_args);
			bham_2_Launcher = new ServerControllerLauncher(bham_2_bau_se_args);
			char_1_Launcher = new ServerControllerLauncher(char_1_bau_se_args);

			/*
			 * For now, don't start services with PRIMARY and SECONARY
			 * routeOffers.
			 */
			char_1_Launcher.launch();

			/* Allow some wait time to let server get up and running */
			Thread.sleep(15000);

			RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithFailoverWhenPrimaryDownOnStart();

			// Save the routeInfo for this service
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			Locations.CHAR.set();

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", "com.att.aft.dme2.test.PreferredRouteReplyHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			/*
			 * Send client request. This should hit routeOffer NW since it is
			 * the only one running.
			 */
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			String reply = replyHandler.getResponse(60000);
			System.err.println(reply);

			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			assertTrue(traceInfo.contains("/routeOffer=BAU_NW:onResponseCompleteStatus=200"));

			// Now start service with PRIMARY routeOffer (BAU_NE)
			bham_1_Launcher.launch();

			// stop server that replied
			String otherServer = null;
			Thread.sleep(5000);

			DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
			// registry.getRegistryEndpointCache().refreshCachedEndpoint("/service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/routeOffer=BAU_NE");
			registry.refreshCachedEndpoint(
					"/service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/routeOffer=BAU_NE");

			replyHandler = new EchoReplyHandler();

			Request request2 = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", "com.att.aft.dme2.test.PreferredRouteReplyHandler")
					.withHeader("AFT_DME2_EXCHANGE_FAILOVER_HANDLERS",
							"com.att.aft.dme2.handler.DefaultLoggingFailoverFaultHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			sender = new DME2Client(manager, request2);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(60000);
			System.err.println("reply=" + reply);

			traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println("TRACE INFO :" + traceInfo);

			// The sequence should have failed over to primary if its available
			assertTrue("TraceInfo expected to contain BAU_NE but was " + traceInfo,
					traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));

			// reply should be not null
			if (reply == null) {
				fail("reply is null or not from the otherServer.  otherServer=" + otherServer + "  reply=" + reply);
			}

			// Start preferred RO and check now.
			bham_2_Launcher.launch();

			Thread.sleep(5000);
			// registry.getRegistryEndpointCache().refreshCachedEndpoint("/service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/routeOffer=BAU_SE");
			registry.refreshCachedEndpoint(
					"/service=com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown/version=1.0.0/envContext=DEV/routeOffer=BAU_SE");
			replyHandler = new EchoReplyHandler();

			Request request3 = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr)
					.withHeader("AFT_DME2_EXCHANGE_REQUEST_HANDLERS",
							"com.att.aft.dme2.test.PreferredRouteRequestHandler")
					.withHeader("AFT_DME2_EXCHANGE_REPLY_HANDLERS", "com.att.aft.dme2.test.PreferredRouteReplyHandler")
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			sender = new DME2Client(manager, request3);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(60000);
			System.err.println("reply=" + reply);

			traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			// The sequence should have failed over to primary if its available
			assertTrue(traceInfo.contains("/routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			try {
				if (bham_1_Launcher != null) {
					bham_1_Launcher.destroy();
				}
			} catch (Exception e) {
			}

			try {
				if (bham_2_Launcher != null) {
					bham_2_Launcher.destroy();
				}

			} catch (Exception e) {
			}

			Thread.sleep(5000);
			replyHandler = new EchoReplyHandler();

			sender = new DME2Client(manager, request);
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			reply = replyHandler.getResponse(60000);
			System.err.println("reply=" + reply);

			traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println(traceInfo);

			// The sequence should have failed over to sequence 3 since 1 and 2
			// are unavail
			assertTrue(traceInfo.contains("/routeOffer=BAU_NW:onResponseCompleteStatus=200"));
		} finally {

			StaticCache.reset();

			try {
				bham_1_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				bham_2_Launcher.destroy();
			} catch (Exception e) {
			}

			try {
				char_1_Launcher.destroy();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2EndpointStalenessInMin() throws Exception {
		// Testing new functionality where user can define a stalenessInMin
		// attribute for a routeOffer.
		// If this attribute is present, it will override the default value for
		// how long an end point remains in the stale cache

		cleanPreviousEndpoints("com.att.aft.dme2.test.TestEndpointStalenessInMin", "1.0.0", "DEV");
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.test.TestEndpointStalenessInMin/version=1.0.0/envContext=LAB/routeOffer=DME2_PRIMARY";

		try {
			Properties props = new Properties();
			props.put("AFT_DME2_EP_READ_TIMEOUT_MS", "20000");

			DME2Configuration config = new DME2Configuration("TestEndpointStalenessInMin", props);

			mgr = new DME2Manager("TestEndpointStalenessInMin", config);
			mgr.bindServiceListener(service, new FailoverServlet(service, "TestService"));

			// Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfoWithStalenessInMins(
					"com.att.aft.dme2.test.TestEndpointStalenessInMin", "1.0.0", "LAB");

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, routeInfo, "LAB");

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestEndpointStalenessInMin/version=1.0.0/envContext=LAB/partner=DME2_PARTNER";
			Request request = new RequestBuilder(new URI(uriStr))
					.withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);
			try {
				sender.sendAndWait(new DME2TextPayload("This is a test"));
			} catch (Exception e) {
				if (e.getMessage().contains("AFT-DME2-0999")) {
					assertTrue(e.getMessage().contains("AFT-DME2-0999"));
				} else if (e.getMessage().contains("AFT-DME2-0703")) {
					assertTrue(e.getMessage().contains("AFT-DME2-0703"));
				}
			}

			DME2Endpoint[] endpoints = mgr.findEndpoints("com.att.aft.dme2.test.TestEndpointStalenessInMin", "1.0.0",
					"LAB", "DME2_PRIMARY", false);

			// Map<String, Long> staleCache = mgr.getStaleCache();
			// System.out.println("Contents of stale cache:" +
			// staleCache.keySet());

			assertTrue(mgr.isEndpointStale(endpoints[0].getServiceEndpointID()));

			Thread.sleep(65000);

			assertTrue(!mgr.isEndpointStale(endpoints[0].getServiceEndpointID()));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {
			}
			try {
				mgr.stop();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangeParseResponseFault() throws Exception {
		DME2Manager mgr_1 = null;
		DME2Manager mgr_2 = null;

		cleanPreviousEndpoints("com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault", "1.0.0", "DEV");
		String serviceURI_1 = "/service=com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
		String serviceURI_2 = "/service=com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault/version=1.0.0/envContext=LAB/routeOffer=SECONDARY";
		String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault/version=1.0.0/envContext=LAB/partner=FAULT";

		try {
			Properties props = RegistryGrmSetup.init();
			props.put("AFT_DME2_PARSE_FAULT", "true");
			props.put("DME2_LOOKUP_NON_FAILOVER_SC", "false");

			DME2Configuration config = new DME2Configuration("testDME2ExchangeParseResponseFault_1", props);

			mgr_1 = new DME2Manager("testDME2ExchangeParseResponseFault_1", config);
			mgr_1.bindServiceListener(serviceURI_1, new EchoServlet(serviceURI_1, "Fail_500"));

			mgr_2 = new DME2Manager("testDME2ExchangeParseResponseFault_2", config);
			mgr_2.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, ""));

			// Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfoForParseResponseFault(
					"com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault", "1.0.0", "LAB");

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			RegistryGrmSetup.init();
			grmInit.saveRouteInfoInGRM(config, routeInfo, "LAB");

			Thread.sleep(3000);

			EchoReplyHandler handler = new EchoReplyHandler();

			Request request = new RequestBuilder(new URI(clientURI))
					.withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI)
					.withHeader("testReturnFailoverEnabledFault", "true").withHeader("AFT_DME2_REQ_TRACE_ON", "true")
					.build();

			DME2Client client = new DME2Client(mgr_1, request);
			client.setResponseHandlers(handler);
			client.send(new DME2TextPayload("THIS IS A TEST"));

			Thread.sleep(3000);

			String resp = handler.getResponse(120000);
			System.out.printf("Got back response from service: %s", resp);
			assertTrue(resp.contains("THIS IS A TEST"));

			Map<String, String> responseHeaders = handler.getResponseHeaders();
			String traceInfo = responseHeaders.get("AFT_DME2_REQ_TRACE_INFO");

			if (traceInfo != null) {
				System.out.printf("Response Trace Info: %s", traceInfo);
				assertTrue(traceInfo.contains("routeOffer=PRIMARY:onResponseCompleteStatus=500"));
				assertTrue(traceInfo.contains("routeOffer=SECONDARY:onResponseCompleteStatus=200"));
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				mgr_1.unbindServiceListener(serviceURI_1);
			} catch (DME2Exception e) {

			}

			try {
				mgr_2.unbindServiceListener(serviceURI_2);
			} catch (DME2Exception e) {

			}
			try {
				mgr_1.stop();
			} catch (Exception e) {
			}
			try {
				mgr_2.stop();
			} catch (Exception e) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2ExchangeDoesNotSetAEndpointStaleOnThrottleLimitError() throws Exception {
    System.setProperty( "SCLD_PLATFORM", TestConstants.GRM_PLATFORM_TO_USE );
		logger.debug(null, "testDME2ExchangeDoesNotSetAEndpointStaleOnThrottleLimitError", LogMessage.METHOD_ENTER);
		Integer maxPoolSize = 50;
		Double throttlePct_10 = 10.0;
		Integer maxActiveReqPerPartner = (int) Math.ceil(maxPoolSize * (throttlePct_10 / 100.0));

		DME2Manager manager = null;
		ServerControllerLauncher bham_bau_ne_launcher = null;
		ServerControllerLauncher bham_bau_se_launcher = null;

		try {

			String[] bham_1_bau_ne_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
					"-registryType", "GRM",
					// "-servletClass", "FailoverServlet",
					"-servletClass", "EchoServlet", "-serviceName",
					"service=com.att.aft.dme2.api.TestDME2ThrottleFilter9/version=1.0.0/envContext=DEV/routeOffer=BAU_NE?throttleFilterDisabled=false",
					"-serviceCity", "BHAM", "-serverid", "bham_1_bau_ne", "-platform", TestConstants.GRM_PLATFORM_TO_USE,
					"-DAFT_DME2_MAX_POOL_SIZE=" + maxPoolSize.toString(),
					"-DAFT_DME2_THROTTLE_PCT_PER_PARTNER=" + throttlePct_10.intValue() };

			String[] bham_2_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
					"-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
					"service=com.att.aft.dme2.api.TestDME2ThrottleFilter9/version=1.0.0/envContext=DEV/routeOffer=BAU_SE?throttleFilterDisabled=false",
					"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE,
					"-DAFT_DME2_MAX_POOL_SIZE=" + maxPoolSize.toString(),
					"-DAFT_DME2_THROTTLE_PCT_PER_PARTNER=" + throttlePct_10.intValue() };

			Properties props = RegistryGrmSetup.init();

			DME2Configuration config = new DME2Configuration("TestDME2ExchangeRouteOfferFailoverHandlers", props);
			manager = new DME2Manager("TestDME2ExchangeRouteOfferFailoverHandlers", config);
			RouteInfo rtInfo = createThrottleRouteInfoWithPreferredRouteOffer();
			// Save the route info
			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM(config, rtInfo, "DEV");

			bham_bau_ne_launcher = new ServerControllerLauncher(bham_1_bau_ne_args);
			bham_bau_se_launcher = new ServerControllerLauncher(bham_2_bau_se_args);

			bham_bau_ne_launcher.launch();
			bham_bau_se_launcher.launch();

			Thread.sleep(60000);

			Locations.CHAR.set();
			String uriWithPartnerToThrottle = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter9/version=1.0.0/envContext=DEV/dataContext=205977/partner=test1";
			// send maxActiveReqPerPartner to BAU_NE route that will wait in
			// EchoServlet's service method
			// send one more so it fails on BAU_NE and gets routed to BAU_SE
			for (int i = 0; i < maxActiveReqPerPartner + 1; i++) {
				DME2Client sender = new DME2Client(manager, new URI(uriWithPartnerToThrottle), 20000);
				sender.addHeader("AFT_DME2_EXCHANGE_FAILOVER_HANDLERS",
						FailoverTestHandler.class.getName() );
				sender.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
				sender.addHeader("echoSleepTimeMs", "20000");
				sender.setPayload(
						"These are going to wait 20 seconds thus increasing the number of active requests for the partner");
				logger.debug(null, "testDME2ExchangeDoesNotSetAEndpointStaleOnThrottleLimitError",
						"Sending request {} of {}", i + 1, maxActiveReqPerPartner + 1);
				sender.send();
			}
      // Sleep for just a short time to make sure this last request is the one that hits the server last.
      Thread.sleep(1000);
			// send another request to go over maxActiveReqPerPartner limit on
			// BAU_NE and failover to BAU_SE
			EchoReplyHandler replyHandler = new EchoReplyHandler();
			DME2Client sender = new DME2Client(manager, new URI(uriWithPartnerToThrottle), 20000);
			sender.addHeader("AFT_DME2_EXCHANGE_FAILOVER_HANDLERS", FailoverTestHandler.class.getName() );
			sender.addHeader("AFT_DME2_REQ_TRACE_ON", "true");
			sender.setPayload("This should be routed to BAU_SE");
			sender.setReplyHandler(replyHandler);
			logger.debug(null, "testDME2ExchangeDoesNotSetAEndpointStaleOnThrottleLimitError", "Sending final request");
			sender.send();
			logger.debug(null, "testDME2ExchangeDoesNotSetAEndpointStaleOnThrottleLimitError",
					"Receiving final response");
			String reply = replyHandler.getResponse(60000);
			logger.debug(null, "testDME2ExchangeDoesNotSetAEndpointStaleOnThrottleLimitError",
					"Received final response {}", reply);
			System.out.println("@@@@Response: " + reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			// assert it first tries BAU_NE even if it failed once above
			assertTrue("Expected traceInfo to contain BAU_SE/Status=200, but was " + traceInfo,
					traceInfo.contains("routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			// DME2EndpointIteratorFactory factory =
			// DME2EndpointIteratorFactory.getInstance();
			// DME2EndpointIterator iter = (DME2EndpointIterator)
			// factory.getIterator(uriWithPartnerToThrottle, null, null,
			// manager);

			// assertTrue(iter.getManager().getStaleRouteOfferCache().getCache().isEmpty());

		} finally {
			try {
				bham_bau_ne_launcher.destroy();
				bham_bau_se_launcher.destroy();
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(15000);
			} catch (Exception e) {
			}
		}
	}

	private RouteInfo createThrottleRouteInfoWithPreferredRouteOffer() {

		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setServiceName("com.att.aft.dme2.api.TestDME2ThrottleFilter9");
		routeInfo.setServiceVersion("1.0.0");
		routeInfo.setEnvContext("DEV");

		RouteGroups routeGroups = new RouteGroups();
		routeInfo.setRouteGroups(routeGroups);

		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("RG1");
		routeGroup.getPartner().add("test1");
		routeGroup.getPartner().add("test2");
		routeGroup.getPartner().add("test3");

		Route route = new Route();
		route.setName("rt1");

		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setSequence(1);
		routeOffer1.setName("BAU_NE");

		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setSequence(2);
		routeOffer2.setName("BAU_SE");

		route.getRouteOffer().add(routeOffer1);
		route.getRouteOffer().add(routeOffer2);

		routeGroup.getRoute().add(route);

		routeGroups.getRouteGroup().add(routeGroup);

		return routeInfo;
	}

}

class TestDME2ClientThread implements Runnable {
	public static final HashMap<String, Boolean> resultsMap = new HashMap<String, Boolean>();
	String id;

	public TestDME2ClientThread(String id) {
		this.id = id;
	}

	@Override
	public void run() {
		String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
		System.err.println("Starting thread " + id);
		try {

			Properties props2 = RegistryGrmSetup.init();
			props2.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
			DME2Manager manager2 = new DME2Manager("TestDME2ExchangeFailoverSequenceOnSameRequest2" + id,
					new DME2Configuration("TestDME2ExchangeFailoverSequenceOnSameRequest2" + id, props2));

			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).withHeader("AFT_DME2_REQ_TRACE_ON", "true").build();

			DME2Client sender = new DME2Client(manager2, request);
			// sender.setPayload("this is a test");

			EchoReplyHandler replyHandler = new EchoReplyHandler();
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload("this is a test"));

			Thread.sleep(50000);
			String reply = replyHandler.getResponse(60000);
			System.err.println("Reply from " + id + " : " + reply);
			String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
			System.out.println("Trace info from " + id + " : " + traceInfo);

			if (traceInfo.contains("/routeOffer=THIRD:onResponseCompleteStatus=200")) {
				resultsMap.put(id, true);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println("Exception from " + id + " : " + e.getMessage());
			e.printStackTrace();
			resultsMap.put(id, false);
		}
	}

}