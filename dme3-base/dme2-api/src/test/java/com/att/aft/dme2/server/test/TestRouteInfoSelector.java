/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.types.RouteInfo;

@Ignore
public class TestRouteInfoSelector {
	static String envContext = "DEV";
	static String fullVersion = "1.0.0";
	static String version = "1";
	static String dataContext = "GLR";
	static String svcName = "com.att.aft.dme2.test.TestDataPartition";
	static String name = "service=" + svcName + "/version=" + fullVersion + "/envContext=" + envContext + "/routeOffer=BAU_SE";
	static String name1 = "service=" + svcName + "/version=" + fullVersion + "/envContext=" + envContext + "/routeOffer=BAU_SW";
	static String name2 = "service=" + svcName + "/version=" + fullVersion + "/envContext=" + envContext + "/routeOffer=BAU_NE";
	static String name3 = "service=" + svcName + "/version=" + fullVersion + "/envContext=" + envContext + "/routeOffer=BAU_NW";
	static DME2Manager manager;
	static DME2Manager manager1;
	static DME2Manager manager2;
	static DME2Manager manager3;

	static StringBuffer buf = new StringBuffer();
	static HashMap<String, String> hm = new HashMap<String, String>();
	
    @Before
    public void setUp() throws Exception {
    	
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", "SANDBOX-DEV");
			System.setProperty("AFT_DME2_GRM_URLS", TestConstants.SANDBOX_DEV_GRM_LWP_DIRECT_HTTP_URLS );
		RegistryFsSetup.init();
		// manager =new DME2Manager(new DME2Configuration("RegistryFsSetup", RegistryFsSetup.init());

		buf.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
				+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
				+ "LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
				+ "/SnapshotRequest></soap:Body></soap:Envelope>");

		// hm.put("echoSleepTimeMs", "11000");
		hm.put("AFT_DME2_EP_READ_TIMEOUT_MS", "10000");
		hm.put("AFT_DME2_ROUNDTRIP_TIMEOUT_MS", "120000");
		hm.put("AFT_DME2_REQ_TRACE_ON", "true");

		manager =new DME2Manager("TestDataPartition", new DME2Configuration("TestDataPartition", RegistryFsSetup.init()));
		manager1 =new DME2Manager("TestDataPartition1", new DME2Configuration("TestDataPartition1", RegistryFsSetup.init()));
		manager2 =new DME2Manager("TestDataPartition2",  new DME2Configuration("TestDataPartition2", RegistryFsSetup.init()));
		manager3 =new DME2Manager("TestDataPartition3", new DME2Configuration("TestDataPartition3", RegistryFsSetup.init()));

		RouteInfo rtInfo = new RouteInfo();
		rtInfo.setServiceName(svcName);
		// rtInfo.setServiceVersion("*");
		rtInfo.setEnvContext(envContext);

		manager.bindServiceListener(name, new EchoServlet(name, "bau_se_1"), null, null, null);
		manager1.bindServiceListener(name1, new EchoServlet(name1, "bau_sw_1"), null, null, null);
		manager2.bindServiceListener(name2, new EchoServlet(name2, "bau_ne_1"),	null, null, null);
		manager3.bindServiceListener(name3, new EchoServlet(name3, "bau_nw_1"),	null, null, null);
		
		// Thread.sleep(5000);
		RegistryFsSetup grmInit = new RegistryFsSetup();
		//grmInit.saveRouteInfoWithDataPartition(rtInfo, envContext);
//		grmInit.saveRouteInfoWithListAndRangeDataPartition(new DME2Configuration("TestDataPartition", RegistryFsSetup.init()), rtInfo, envContext);
		// sleep for few mins so that GrM can persist data to cassandra
		Thread.sleep(10000);
	}
	
    @After
    public void tearDown() throws Exception {
		try {
			manager.unbindServiceListener(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			manager1.unbindServiceListener(name1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			manager2.unbindServiceListener(name2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			manager3.unbindServiceListener(name3);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    
    @Test
    public void testRouteInfo_AllRouteHasSelector_InputNoSelectors() throws Exception {

		// Try with no stickySelector. Expected result is it can go to one
		// of BAU_SW or BAU_SW
		String uriStr = "http://DME2SEARCH/service=" + svcName + "/version="
				+ fullVersion + "/envContext=" + envContext + "/partner=SET";
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(new DME2TextPayload(buf.toString()));
		String reply = replyHandler.getResponse(10000);
		System.out.println("REPLY 1 length =" + reply);
		assertTrue(reply.contains("bau_se_1") || reply.contains("bau_sw_1") || reply.contains("bau_nw_1"));
		String traceInfo = replyHandler.getResponseHeaders().get(
				"AFT_DME2_REQ_TRACE_INFO");
		System.out.println("traceInfo=" + traceInfo);
		// assertTrue(traceInfo.contains("onExpire"));

	}
	
    
    @Test
    public void testRouteInfo_RouteHasJustDataPartSel_InputDataContext()
			throws Exception {
		// Try with no stickySelector, but just dataContext. Expected result
		// is has to go only to BAU_SW
		String uriStr = "http://DME2SEARCH/service=" + svcName + "/version="
				+ fullVersion + "/envContext=" + envContext
				+ "/partner=SET/dataContext=AKT";
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(new DME2TextPayload(buf.toString()));
		String reply = replyHandler.getResponse(10000);
		System.out.println("REPLY 1 length =" + reply);
		assertTrue(reply.contains("bau_sw_1"));
		String traceInfo = replyHandler.getResponseHeaders().get(
				"AFT_DME2_REQ_TRACE_INFO");
		System.out.println("traceInfo=" + traceInfo);
		// assertTrue(traceInfo.contains("onExpire"));
	}
     
    @Test
    public void testRouteInfo_RouteHasJustDataPartSel_InputDataContextAndStickySel()
			throws Exception {
		// Try with stickySelector, and dataContext. Expected result is has
		// to go only to BAU_SW since atleast one matching selector is found
		String uriStr = "http://DME2SEARCH/service=" + svcName + "/version="
				+ fullVersion + "/envContext=" + envContext
				+ "/partner=SET/dataContext=AKT/stickySelectorKey=Q24A";
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		//sender.setReplyHandler(replyHandler);
		try {
			sender.setResponseHandlers(replyHandler);
			sender.send(new DME2TextPayload(buf.toString()));
			String reply = replyHandler.getResponse(10000);
			assertTrue(reply == null);
			System.out.println("REPLY 1 length =" + reply);
			assertTrue(reply.contains("bau_sw_1"));
			String traceInfo = replyHandler.getResponseHeaders().get(
					"AFT_DME2_REQ_TRACE_INFO");
			System.out.println("traceInfo=" + traceInfo);
			// assertTrue(traceInfo.contains("onExpire"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0103"));
		}
	}
    
    @Test
    public void testRouteInfo_RouteDataPartAndStickySel_InputDataContextAndStickySel()
			throws Exception {
		// Try with stickySelector, and dataContext. Expected result is has
		// to go only to BAU_SW
		String uriStr = "http://DME2SEARCH/service=" + svcName + "/version="
				+ fullVersion + "/envContext=" + envContext
				+ "/partner=SET/dataContext=PAC/stickySelectorKey=Q24A";
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(new DME2TextPayload(buf.toString()));
		String reply = replyHandler.getResponse(10000);
		System.out.println("REPLY 1 length =" + reply);
		assertTrue(reply.contains("bau_sw_1"));
		String traceInfo = replyHandler.getResponseHeaders().get(
				"AFT_DME2_REQ_TRACE_INFO");
		System.out.println("traceInfo=" + traceInfo);
		// assertTrue(traceInfo.contains("onExpire"));
	}
    
    @Test
    public void testRouteInfo_RouteDataPartAndStickySel_InputOnlyStickySel() throws Exception {

		// Try just with stickySelector , and NO dataContext. Expected
		// result is has
		// to go only to BAU_SW
		String uriStr = "http://DME2SEARCH/service=" + svcName + "/version=" + fullVersion + "/envContext=" + envContext + "/partner=SET/stickySelectorKey=Q24A";
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(new DME2TextPayload(buf.toString()));
		
		String reply = replyHandler.getResponse(10000);
		System.out.println("REPLY 1 length =" + reply);
		
		assertTrue(reply.contains("bau_sw_1"));
		String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
		System.out.println("traceInfo=" + traceInfo);
		// assertTrue(traceInfo.contains("onExpire"));
	}
    
    @Test
    public void testRouteInfo_NoRouteMatchesDCAndStickSelTogether_InputDataContextAndStickySel()
			throws Exception {

		// Try with stickySelector, and dataContext that could not be
		// resolved together in a route. Expected result is failure based on
		// route config
		/*
		 * sb.append("<route name=\"DEFAULT2\">");
		 * sb.append("<dataPartitionRef>PACRouting</dataPartitionRef>");
		 * sb.append("<stickySelectorKey>Q24A</stickySelectorKey>"); sb.append
		 * ("<routeOffer name=\"BAU_SW\" sequence=\"1\" active=\"true\"/>");
		 * sb.append("</route>");
		 * 
		 * sb.append("<route name=\"DEFAULT3\">");
		 * sb.append("<dataPartitionRef>AKTRouting</dataPartitionRef>");
		 * sb.append
		 * ("<routeOffer name=\"BAU_SW\" sequence=\"1\" active=\"true\"/>");
		 * sb.append("</route>");
		 * 
		 * sb.append("</routeGroup>");
		 */
		String uriStr = "http://DME2SEARCH/service=" + svcName + "/version="
				+ fullVersion + "/envContext=" + envContext
				+ "/partner=SET/dataContext=AKT/stickySelectorKey=Q24A";
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		try {
			sender.send(new DME2TextPayload(buf.toString()));
			String reply = replyHandler.getResponse(10000);
			assertTrue(reply == null);
			System.out.println("REPLY 1 length =" + reply);
			assertTrue(reply.contains("bau_sw_1"));
			String traceInfo = replyHandler.getResponseHeaders().get(
					"AFT_DME2_REQ_TRACE_INFO");
			System.out.println("traceInfo=" + traceInfo);
			// assertTrue(traceInfo.contains("onExpire"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0103"));
		}

	}
	
    @Test
    public void testRouteInfo_RouteHasJustDataPartSel_InputDataContext_ListDataPartition()
			throws Exception {
		// Try with no stickySelector, but just dataContext. Expected result
		// is has to go only to BAU_SW
		String uriStr = "http://DME2SEARCH/service=" + svcName + "/version="
				+ fullVersion + "/envContext=" + envContext
				+ "/partner=SET/dataContext=LISTVAL2";
		Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(manager, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		sender.send(new DME2TextPayload(buf.toString()));
		String reply = replyHandler.getResponse(10000);
		System.out.println("REPLY 1 length =" + reply);
		assertTrue(reply.contains("bau_nw_1"));
		String traceInfo = replyHandler.getResponseHeaders().get(
				"AFT_DME2_REQ_TRACE_INFO");
		System.out.println("traceInfo=" + traceInfo);
		// assertTrue(traceInfo.contains("onExpire"));
	}
	
    
    @Test
    public void testSelectorsANDOperation() throws Exception{
		DME2Manager mgr1 =new DME2Manager("FSMgr",new DME2Configuration("FSMgr",RegistryFsSetup.init()));
		String uriStr = "http://DME2SEARCH/service=" + "com.att.afttest3.DataPartitionEnabler" + "/version="
				+ "1.0.0" + "/envContext=" + "LAB"
				+ "/partner=DP/dataContext=205977/stickySelectorKey=DPSEL";
		Request request = new RequestBuilder( new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		DME2Client sender = new DME2Client(mgr1, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		EchoReplyHandler replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		try {
			sender.send(new DME2TextPayload(buf.toString()));
			String reply = replyHandler.getResponse(10000);
			assertTrue(reply==null);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0702"));
		}
		
		uriStr = "http://DME2SEARCH/service=" + "com.att.afttest3.DataPartitionEnabler" + "/version="
				+ "1.0.0" + "/envContext=" + "LAB"
				+ "/partner=DP/stickySelectorKey=DPSEL";
		
		request = new RequestBuilder( new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		sender = new DME2Client(mgr1, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		try {
			sender.send(new DME2TextPayload(buf.toString()));
			String reply = replyHandler.getResponse(10000);
			assertTrue(reply==null);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0702"));
		}
		
		
		uriStr = "http://DME2SEARCH/service=" + "com.att.afttest3.DataPartitionEnabler" + "/version="
				+ "1.0.0" + "/envContext=" + "LAB"
				+ "/partner=DP/dataContext=205444";
		request = new RequestBuilder( new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		sender = new DME2Client(mgr1, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		try {
			sender.send(new DME2TextPayload(buf.toString()));
			String reply = replyHandler.getResponse(10000);
			assertTrue(reply==null);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0702"));
		}
		
		uriStr = "http://DME2SEARCH/service=" + "com.att.afttest3.DataPartitionEnabler" + "/version="
				+ "1.0.0" + "/envContext=" + "LAB"
				+ "/partner=DP";
		request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		sender = new DME2Client(mgr1, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		try {
			sender.send(new DME2TextPayload(buf.toString()));
			String reply = replyHandler.getResponse(10000);
			assertTrue(reply==null);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0702"));
		}
		
		uriStr = "http://DME2SEARCH/service=" + "com.att.afttest3.DataPartitionEnabler" + "/version="
				+ "1.0.0" + "/envContext=" + "LAB"
				+ "/partner=ABC/dataContext=205977";
		request = new RequestBuilder( new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

		sender = new DME2Client(mgr1, request);
		//sender.setHeaders(hm);
		//sender.setPayload(buf.toString());
		replyHandler = new EchoReplyHandler();
		sender.setResponseHandlers(replyHandler);
		try {
			sender.send(new DME2TextPayload(buf.toString()));
			String reply = replyHandler.getResponse(10000);
			assertTrue(reply==null);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0103"));
		}
	}
}

