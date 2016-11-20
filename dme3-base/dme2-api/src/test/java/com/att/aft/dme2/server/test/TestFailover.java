/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;

/**
 * The Class TestFailover.
 */
public class TestFailover extends DME2BaseTestCase {

	private ServerControllerLauncher bham_1_Launcher;
	private ServerControllerLauncher bham_2_Launcher;
	private ServerControllerLauncher bham_3_Launcher;
	private ServerControllerLauncher char_1_Launcher;

	@Override
	public void setUp() {
		super.setUp();
	}

	@Override
	public void tearDown() {
		super.tearDown();
		if ( bham_1_Launcher != null ) {
			bham_1_Launcher.destroy();
		}
		if ( bham_2_Launcher != null ) {
			bham_2_Launcher.destroy();
		}
		if ( bham_3_Launcher != null ) {
			bham_3_Launcher.destroy();
		}
		if ( char_1_Launcher != null ) {
			char_1_Launcher.destroy();
		}

		try {
			Thread.sleep( 5000 );
		} catch ( Exception ex ) {

		}
	}

	public void testRequest() throws Exception {
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost", "brcbsp01",
				"-serverPort", "4600",
				"-registryType", "FS",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_1_bau_se" };

		String[] bham_2_bau_se_args = {
				"-serverHost", "brcbsp02",
				"-serverPort", "4601",
				"-registryType", "FS",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_2_bau_se" };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );

		try {
			bham_1_Launcher.launch();
			bham_2_Launcher.launch();

			Thread.sleep( 5000 );

			System.out.println( "Started services for: /service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE." );
			Locations.CHAR.set();

			// try to call a service we just registered
			String uriStr =
					"http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testRequest", props );

			DME2Manager manager = new DME2Manager( "testRequest", config );
			//			DME2Manager manager = new DME2Manager("testRequest", RegistryFsSetup.init());

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			sender.setResponseHandlers( replyHandler );

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			DME2Payload payload = new DME2TextPayload( "this is a test" );

			//sender.setPayload("this is a test");
			//sender.setReplyHandler(replyHandler);
			sender.send( payload );

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response 1 = " + reply );
			assertTrue( reply.contains( "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" ) );

			//The server that used to service the first request will be shutdown. The second server will be left up and the ID of that server will be assigned to the below variable
			String otherServer = null;

			// stop server that replied
			if ( reply.indexOf( "bham_1_bau_se" ) != -1 ) {
				System.out.println( "Destroying bham_1_launcher" );
				bham_1_Launcher.destroy();
				bham_1_Launcher.destroy();
				bham_1_Launcher.destroy();
				otherServer = "bham_2_bau_se";
			} else if ( reply.indexOf( "bham_2_bau_se" ) != -1 ) {
				System.out.println( "Destroying bham_2_launcher" );
				bham_2_Launcher.destroy();
				bham_2_Launcher.destroy();
				bham_2_Launcher.destroy();

				otherServer = "bham_1_bau_se";
			} else {
				fail( "Reply is not from bham_1_bau_se or bham_2_bau_se. Reply= " + reply );
			}

			Thread.sleep( 5000 );

			replyHandler = new EchoReplyHandler();

			request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

			sender = new DME2Client( manager, request );

			sender.setResponseHandlers( replyHandler );
			//sender = new DME2Client(manager, new URI(uriStr), 30000);
			//sender.setPayload("this is a test");
			//sender.setReplyHandler(replyHandler);
			sender.send( payload );

			reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response 2 (Should contain serviceID: " + otherServer + ") = " + reply );
			assertTrue( reply.contains( otherServer ) );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}

	@Test
	public void testPreferLocalWithQueryParam() throws Exception {
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost", "brcbsp01",
				"-serverPort", "49901-49999",
				"-registryType", "FS",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_1_bau_se" };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_1_Launcher.launch();

		Thread.sleep( 5000 );
		System.out.println( "Started service for: /service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE." );
		Locations.CHAR.set();

		try {
			// try to call a service we just registered
			String uriStr =
					"http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST?preferLocal=true";

			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testPreferLocal", props );

			DME2Manager mgr = new DME2Manager( "testPreferLocal", config );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			Request request = new RequestBuilder( new URI( uriStr ) )
					.withHeader( "AFT_DME2_REQ_TRACE_ON", "true" )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr )
					.build();

			DME2Client sender = new DME2Client( mgr, request );

			DME2Payload payload = new DME2TextPayload( "TEST IS A TEST" );

			sender.setResponseHandlers( replyHandler );
			sender.send( payload );

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response = " + reply );

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
			System.out.println( "TraceInfo = " + traceInfo );

			assertTrue( traceInfo.contains( ":preferredLocal];" ) );
			assertTrue( reply != null );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}

	}

	@Test
	public void testPreferLocalWithRequestBuilder() throws Exception {
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost", "brcbsp01",
				"-serverPort", "49901-49999",
				"-registryType", "FS",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_1_bau_se" };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_1_Launcher.launch();

		Thread.sleep( 5000 );
		System.out.println( "Started service for: /service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE." );
		Locations.CHAR.set();

		try {
			// try to call a service we just registered
			String uriStr =
					"http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testPreferLocal", props );

			DME2Manager mgr = new DME2Manager( "testPreferLocal", config );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			Request request = new RequestBuilder( new URI( uriStr ) )
					.withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withHttpMethod( "POST" ).withReadTimeout( 300000 )
					.withReturnResponseAsBytes( false ).withLookupURL( uriStr )
					.withPreferLocalEPs( true ) // set to true
					.build();

			DME2Client sender = new DME2Client( mgr, request );

			DME2Payload payload = new DME2TextPayload( "TEST IS A TEST" );

			sender.setResponseHandlers( replyHandler );
			sender.send( payload );

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response = " + reply );

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
			System.out.println( "TraceInfo = " + traceInfo );

			assertTrue( traceInfo.contains( ":preferredLocal];" ) );
			assertTrue( reply != null );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}

	}

	@Test
	public void testPreferLocalOverrideWithRequestBuilder() throws Exception {
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost", "brcbsp01",
				"-serverPort", "49901-49999",
				"-registryType", "FS",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_1_bau_se" };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_1_Launcher.launch();

		Thread.sleep( 5000 );
		System.out.println( "Started service for: /service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE." );
		Locations.CHAR.set();

		try {
			// try to call a service we just registered
			String uriStr =
					"http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST?preferLocal=true";

			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testPreferLocal", props );

			DME2Manager mgr = new DME2Manager( "testPreferLocal", config );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			Request request = new RequestBuilder( new URI( uriStr ) )
					.withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withHttpMethod( "POST" ).withReadTimeout( 300000 )
					.withReturnResponseAsBytes( false ).withLookupURL( uriStr )
					.withPreferLocalEPs( false ) // override to false
					.build();

			DME2Client sender = new DME2Client( mgr, request );

			DME2Payload payload = new DME2TextPayload( "TEST IS A TEST" );

			sender.setResponseHandlers( replyHandler );
			sender.send( payload );

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response = " + reply );

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
			System.out.println( "TraceInfo = " + traceInfo );

			assertFalse( traceInfo.contains( ":preferredLocal];" ) );
			assertTrue( reply != null );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}

	}

	public void testLargePayloadRequest() throws Exception {
		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost", "brcbsp01",
				"-serverPort", "49901-49999",
				"-registryType", "FS",
				"-servletClass", "EchoResponseServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_1_bau_se" };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_1_Launcher.launch();

		Thread.sleep( 5000 );

		Locations.CHAR.set();
		System.out.println( "Started service for: /service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE." );

		try {
			// try to call a service we just registered
			String uriStr =
					"http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testPreferLocal", props );

			DME2Manager manager = new DME2Manager( "testPreferLocal", config );

			//DME2Manager manager = new DME2Manager("testPreferLocal", RegistryFsSetup.init());

			StringBuffer buf = new StringBuffer();
			for ( int i = 0; i < 10000; i++ ) {
				buf.append( "test" );
			}

			System.out.println( "Request payload length = " + buf.toString().length() );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "TEST IS A TEST" );

			sender.setResponseHandlers( replyHandler );

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload(buf.toString());
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response String = " + reply );
			System.out.println( "Response length = " + reply.length() );
			assertTrue( reply.length() >= 40000 );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				bham_1_Launcher.destroy();
			} catch ( Exception e ) {
			}
		}
	}

	public void testFaultReply() throws Exception {
		String serviceUri = "/service=com.att.aft.test.MyFaultService/version=1.0.0/envContext=LAB/routeOffer=FAULT_OUT";
		DME2Manager manager = null;

		try {
			Properties props = RegistryFsSetup.init();
			props.put( "AFT_DME2_PARSE_FAULT", "false" );

			DME2ServiceHolder holder = new DME2ServiceHolder();
			holder.setServlet( new EchoServlet( serviceUri, "fault_server_1" ) );
			holder.setServiceURI( serviceUri );

			DME2Configuration config = new DME2Configuration( "FaultFailoverManager", props );

			manager = new DME2Manager( "FaultFailoverManager", config );

			//manager = new DME2Manager("FaultFailoverManager", props);
			manager.addService( holder );
			manager.start();

			Thread.sleep( 5000 );

			// try to call a service we just registered
			Locations.CHAR.set();
			String uriStr = "/service=com.att.aft.test.MyFaultService/version=1.0.0/envContext=LAB/partner=xyz";

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			Request request = new RequestBuilder( new URI( uriStr ) )
					.withHeader( "testReturnFault", "true" ).withHeader( "Content-Type", "text/plain" ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();
			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			DME2Client client = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			client.setResponseHandlers( replyHandler );
			//			sender.setPayload(buf.toString());
			//			sender.addHeader("testReturnFault", "true");
			//		sender.addHeader("Content-Type", "text/plain");
			client.send( payload );

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response length = " + reply );
			assertTrue( reply.contains( "Fault" ) );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( serviceUri );
			} catch ( Exception e ) {
			}
		}
	}

	public void testIgnoreFailoverOnExpire() throws Exception {
		String name = "/service=com.att.aft.TestIgnoreFailoverOnExpire/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		DME2Manager manager = null;

		try {
			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testIgnoreFailoverOnExpire", props );
			manager = new DME2Manager( "testIgnoreFailoverOnExpire", config );
			//manager = new DME2Manager("testIgnoreFailoverOnExpire", RegistryFsSetup.init());
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_2" ), null, null, null );

			Thread.sleep( 5000 );

			String uriStr =
					"http://DME2RESOLVE/service=com.att.aft.TestIgnoreFailoverOnExpire/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "31000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "30000" );
			hm.put( "com.att.aft.dme2.jms.ignoreFailOverOnExpire", "true" );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setHeaders(hm);
			//			sender.setPayload(buf.toString());
			//			sender.setReplyHandler(replyHandler);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			sender.setResponseHandlers( replyHandler );

			try {
				sender.send( payload );
				String reply = replyHandler.getResponse( 40000 );
				System.out.println( "REPLY 1 length =" + reply );

			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0709" ) );
				return;
			}

			fail( "If it reaches here, ignore failover exception did not occur" );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}
		}
	}

	public void testIgnoreFailoverOnExpireViaURIQueryParams() throws Exception {
		String name = "service=com.att.aft.TestIgnoreFailoverOnExpire1/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		DME2Manager manager = null;

		try {

			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testIgnoreFailoverOnExpireViaURIQueryParams", props );

			manager = new DME2Manager( "testIgnoreFailoverOnExpireViaURIQueryParams", config );

			//manager = new DME2Manager("testIgnoreFailoverOnExpireViaURIQueryParams", RegistryFsSetup.init());
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_2" ), null, null, null );

			Thread.sleep( 5000 );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "31000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "30000" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			String uriStr =
					"http://DME2RESOLVE/service=com.att.aft.TestIgnoreFailoverOnExpire1/version=1.0.0/envContext=LAB/routeOffer=BAU_SE?ignoreFailoverOnExpire=true";

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setHeaders(hm);
			//			sender.setPayload(buf.toString());
			//			sender.setReplyHandler(replyHandler);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			sender.setResponseHandlers( replyHandler );

			try {
				sender.send( payload );
				String reply = replyHandler.getResponse( 40000 );
				System.out.println( "REPLY 1 length =" + reply );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0709" ) );
				return;
			}

			fail( "If it reaches here, ignore failover exception did not occur" );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}
		}
	}

	public void testIgnoreFailoverOnExpireViaJvmArg() throws Exception {
		String name = "/service=com.att.aft.TestIgnoreFailoverOnExpire2/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		DME2Manager manager = null;

		try {

			Properties props = RegistryFsSetup.init();
			props.put( "AFT_DME2_IGNORE_FAILOVER_ONEXPIRE", "true" );

			DME2Configuration config = new DME2Configuration( "testIgnoreFailoverOnExpireViaJvmArg", props );

			manager = new DME2Manager( "testIgnoreFailoverOnExpireViaJvmArg", config );
			//manager = new DME2Manager("testIgnoreFailoverOnExpireViaJvmArg", props);
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_2" ), null, null, null );

			Thread.sleep( 5000 );

			String uriStr =
					"http://DME2RESOLVE/service=com.att.aft.TestIgnoreFailoverOnExpire2/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "31000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "30000" );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 31000);
			//			sender.setHeaders(hm);
			//			sender.setPayload(buf.toString());
			//			sender.setReplyHandler(replyHandler);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			sender.setResponseHandlers( replyHandler );

			try {
				sender.send( payload );
				String reply = replyHandler.getResponse( 40000 );
				System.out.println( "REPLY 1 length =" + reply );

			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0709" ) );
				return;
			}

			fail( "If it reaches here, ignore failover exception did not occur" );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}
		}
	}

	public void testExchangeRoundTripTimeout() throws Exception {
		String name = "/service=com.att.aft.TestExchangeRoundTripTimeout/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

		DME2Manager manager = null;
		DME2Manager manager1 = null;

		try {

			Properties props = RegistryFsSetup.init();
			DME2Configuration config = new DME2Configuration( "testExchangeRoundTripTimeout", props );

			manager = new DME2Manager( "testExchangeRoundTripTimeout", config );

			//manager = new DME2Manager("testExchangeRoundTripTimeout", RegistryFsSetup.init());

			Properties props1 = RegistryFsSetup.init();
			DME2Configuration config1 = new DME2Configuration( "testExchangeRoundTripTimeout1", props1 );

			manager1 = new DME2Manager( "testExchangeRoundTripTimeout1", config1 );

			//			manager1 = new DME2Manager("testExchangeRoundTripTimeout1", RegistryFsSetup.init());

			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager1.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_2" ), null, null, null );

			String uriStr =
					"http://DME2RESOLVE/service=com.att.aft.TestExchangeRoundTripTimeout/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "31000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "30000" );
			hm.put( "AFT_DME2_ROUNDTRIP_TIMEOUT_MS", "29000" );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setHeaders(hm);
			//			sender.setPayload(buf.toString());
			//			sender.setReplyHandler(replyHandler);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			sender.setResponseHandlers( replyHandler );

			try {
				sender.send( payload );
				String reply = replyHandler.getResponse( 40000 );
				System.out.println( "REPLY 1 length =" + reply );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0713" ) );
				return;
			}

			fail( "If it reaches here, ignore failover exception did not occur" );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}

			try {
				manager1.unbindServiceListener( name );
			} catch ( Exception e ) {
			}
		}
	}

	public void testExchangeRoundTripTimeoutViaQueryParam() throws Exception {
		String name =
				"service=com.att.aft.TestExchangeRoundTripTimeoutViaQueryParam/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

		DME2Manager manager = null;
		DME2Manager manager1 = null;

		try {
			Properties props = RegistryFsSetup.init();
			DME2Configuration config = new DME2Configuration( "testExchangeRoundTripTimeoutViaQueryParam", props );

			manager = new DME2Manager( "testExchangeRoundTripTimeoutViaQueryParam", config );
			//			manager = new DME2Manager("testExchangeRoundTripTimeoutViaQueryParam", RegistryFsSetup.init());

			Properties props1 = RegistryFsSetup.init();

			DME2Configuration config1 = new DME2Configuration( "testExchangeRoundTripTimeoutViaQueryParam1", props1 );

			manager1 = new DME2Manager( "testExchangeRoundTripTimeoutViaQueryParam1", config1 );

			//			manager1 = new DME2Manager("testExchangeRoundTripTimeoutViaQueryParam1", RegistryFsSetup.init());

			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager1.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_2" ), null, null, null );

			Thread.sleep( 3000 );

			String uriStr =
					"http://DME2RESOLVE/service=com.att.aft.TestExchangeRoundTripTimeoutViaQueryParam/version=1.0.0/envContext=LAB/routeOffer=BAU_SE?roundTripTimeout=29500";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "31000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "30000" );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setHeaders(hm);
			//			sender.setPayload(buf.toString());
			//			sender.setReplyHandler(replyHandler);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			sender.setResponseHandlers( replyHandler );

			try {
				sender.send( payload );
				String reply = replyHandler.getResponse( 40000 );
				System.out.println( "REPLY 1 length =" + reply );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0713" ) );
				return;
			}

			fail( "If it reaches here, ignore failover exception did not occur" );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}

			try {
				manager1.unbindServiceListener( name );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testExchangeRoundTripTimeoutEnforcement() throws Exception {
		/********************************************************************************************
		 * This test case is to test whether DME2 will honor the round trip timeout set by clients. *
		 * Created service A and B, and DME2 will failover from A to B. Each service takes more     *
		 * time than the endpoint timeout to respond. As a result, DME2 will set the endpoint       *
		 * timeout on the next endpoint to a lower value, making it possible for the entire call    *
		 * to be returned within the roundtrip timeout.                                             *
		 ********************************************************************************************/

		String name =
				"service=com.att.aft.TestExchangeRoundTripTimeoutFailover/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		String name1 =
				"service=com.att.aft.TestExchangeRoundTripTimeoutFailover/version=1.0.0/envContext=LAB/routeOffer=BAU_NE";

		DME2Manager manager = null;
		DME2Manager manager2 = null;
		DME2Manager manager3 = null;
		DME2Manager manager4 = null;
		long startTime = 0;

		// We need DME2 to return at this roundtrip timeout
		String roundtripTimeoutString = "10000";

		try {
			Properties props = RegistryFsSetup.init();
			props.put( "AFT_DME2_ROUNDTRIP_TIMEOUT_MS", roundtripTimeoutString );
			props.put( "AFT_DME2_STRICTLY_ENFORCE_ROUNDTRIP_TIMEOUT", "true" );

			manager = new DME2Manager( "TestExchangeRoundTripTimeoutFailover", props );
			manager2 = new DME2Manager( "TestExchangeRoundTripTimeoutFailover2", props );
			manager3 = new DME2Manager( "TestExchangeRoundTripTimeoutFailover3", props );
			manager4 = new DME2Manager( "TestExchangeRoundTripTimeoutFailover4", props );

			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager2.bindServiceListener( name1, new EchoServlet( name1, "bau_se_2" ), null, null, null );
			manager3.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager4.bindServiceListener( name1, new EchoServlet( name1, "bau_se_2" ), null, null, null );

			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName( "com.att.aft.TestExchangeRoundTripTimeoutFailover" );
			rtInfo.setEnvContext( "LAB" );

			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups( rtGrps );

			RegistryFsSetup grmInit = new RegistryFsSetup();
//			grmInit.saveRouteInfoForRoundTripFailover( manager.getConfig(), rtInfo, "LAB" );

			Thread.sleep( 5000 );

			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.TestExchangeRoundTripTimeoutFailover/version=1.0.0/envContext=LAB/partner=test2";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "30000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "9000" );
			hm.put( "AFT_DME2_ROUNDTRIP_TIMEOUT_MS", roundtripTimeoutString );
			hm.put( "AFT_DME2_REQ_TRACE_ON", "true" );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B" +
					"ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>" +
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><" +
					"/SnapshotRequest></soap:Body></soap:Envelope>" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client sender = new DME2Client( manager, new URI( uriStr ), 10000 );
			sender.setPayload( buf.toString() );
			sender.setHeaders( hm );
			sender.setReplyHandler( replyHandler );
			startTime = System.currentTimeMillis();
			sender.send();

			String reply = replyHandler.getResponse( Long.valueOf( roundtripTimeoutString ).longValue() );
			System.out.println( "REPLY 1 length =" + reply );

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
			System.out.println( "traceInfo=" + traceInfo );
			fail( "This test case should never got to here!" );

		} catch ( Exception e ) {
			// Now we take a look at how much time DME2 actually took. I gave it a 2000ms for overhead
			long timeTaken = ( System.currentTimeMillis() - startTime );
			System.out.println( "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@DME2 actually took: " + timeTaken + "ms." );
			assertTrue( timeTaken < ( Long.valueOf( roundtripTimeoutString ).longValue() + 2000 ) );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}
			try {
				manager2.unbindServiceListener( name1 );
			} catch ( Exception e ) {
			}
			try {
				manager3.unbindServiceListener( name1 );
			} catch ( Exception e ) {
			}
			try {
				manager4.unbindServiceListener( name1 );
			} catch ( Exception e ) {
			}

		}

	}

	public void testExchangeRoundTripTimeoutViaJMSPropOverride() throws Exception {
		String name =
				"/service=com.att.aft.TestExchangeRoundTripTimeoutViaJMSPropOverride/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		DME2Manager manager = null;
		DME2Manager manager1 = null;

		try {

			Properties props = RegistryFsSetup.init();
			props.put( "AFT_DME2_ROUNDTRIP_TIMEOUT_MS", "120000" );

			DME2Configuration config = new DME2Configuration( "TestExchangeRoundTripTimeoutViaJMSPropOverride", props );

			//manager = new DME2Manager("testExchangeRoundTripTimeoutViaQueryParam", config);

			manager = new DME2Manager( "TestExchangeRoundTripTimeoutViaJMSPropOverride", config );

			manager1 = new DME2Manager( "TestExchangeRoundTripTimeoutViaJMSPropOverride1", config );

			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager1.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_2" ), null, null, null );

			Thread.sleep( 3000 );

			String uriStr =
					"http://DME2RESOLVE/service=com.att.aft.TestExchangeRoundTripTimeoutViaJMSPropOverride/version=1.0.0/envContext=LAB/routeOffer=BAU_SE?roundTripTimeout=29500";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "11000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "10000" );
			hm.put( "AFT_DME2_ROUNDTRIP_TIMEOUT_MS", "9000" );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setHeaders(hm);
			//			sender.setPayload(buf.toString());
			//			sender.setReplyHandler(replyHandler);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			sender.setResponseHandlers( replyHandler );

			try {
				sender.send( payload );
				String reply = replyHandler.getResponse( 40000 );
				System.out.println( "REPLY 1 length =" + reply );

			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0713" ) );
				return;
			}

			fail( "If it reaches here, ignore failover exception did not occur" );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}

			try {
				manager1.unbindServiceListener( name );
			} catch ( Exception e ) {
			}
		}
	}

	public void testExchangeRoundTripTimeoutFailover() throws Exception {
		String name =
				"service=com.att.aft.TestExchangeRoundTripTimeoutFailover/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		String name1 =
				"service=com.att.aft.TestExchangeRoundTripTimeoutFailover/version=1.0.0/envContext=LAB/routeOffer=BAU_NE";

		DME2Manager manager = null;
		DME2Manager manager2 = null;

		try {
			Properties props = RegistryFsSetup.init();
			props.put( "AFT_DME2_ROUNDTRIP_TIMEOUT_MS", "120000" );

			DME2Configuration config = new DME2Configuration( "TestExchangeRoundTripTimeoutViaJMSPropOverride", props );

			//manager = new DME2Manager("testExchangeRoundTripTimeoutViaQueryParam", config);

			//manager = new DME2Manager("TestExchangeRoundTripTimeoutViaJMSPropOverride",	config);
			manager = new DME2Manager( "TestExchangeRoundTripTimeoutFailover", config );
			manager2 = new DME2Manager( "TestExchangeRoundTripTimeoutFailover2", config );

			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			manager2.bindServiceListener( name1, new EchoResponseServlet( name1, "bau_se_2" ), null, null, null );

			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName( "com.att.aft.TestExchangeRoundTripTimeoutFailover" );
			rtInfo.setEnvContext( "LAB" );

			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups( rtGrps );

			RegistryFsSetup grmInit = new RegistryFsSetup();
//			grmInit.saveRouteInfoForRoundTripFailover( config, rtInfo, "LAB" );

			Thread.sleep( 5000 );

			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.TestExchangeRoundTripTimeoutFailover/version=1.0.0/envContext=LAB/partner=test2";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs", "11000" );
			hm.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "10000" );
			hm.put( "AFT_DME2_ROUNDTRIP_TIMEOUT_MS", "120000" );
			hm.put( "AFT_DME2_REQ_TRACE_ON", "true" );

			StringBuffer buf = new StringBuffer();
			buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
					+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
					+
					"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
					+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload(buf.toString());
			//			sender.setHeaders(hm);
			//			sender.setReplyHandler(replyHandler);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( buf.toString() );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			String reply = replyHandler.getResponse( 40000 );
			System.out.println( "REPLY 1 length =" + reply );
			assertTrue( reply.contains( "bau_se_2" ) );

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
			System.out.println( "traceInfo=" + traceInfo );
			assertTrue( traceInfo.contains( "onExpire" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}

		}

	}

	public void testEndpointStaleness() throws Exception {
		String[] bham_1_bau_se_args = {
				"-serverHost", "localhost",
				"-serverPort", "4900",
				"-registryType", "FS",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_3_bau_se" };

		String[] bham_2_bau_se_args = {
				"-serverHost", "localhost",
				"-serverPort", "4902",
				"-registryType", "FS",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_4_bau_se" };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );

		bham_1_Launcher.launch();
		bham_2_Launcher.launch();

		Properties props = RegistryFsSetup.init();
		props.put( "AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS", "10000" );
		props.put( "AFT_DME2_REQ_TRACE_ON", "true" );

		Thread.sleep( 5000 );
		Locations.CHAR.set();

		props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration( "testEndpointStaleness", props );

		DME2Manager manager = new DME2Manager( "testEndpointStaleness", config );
		//		DME2Manager manager = new DME2Manager("testEndpointStaleness", props);
		String uriStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

		int counter = 0;
		boolean flipFlag = false;

		for ( int i = 0; i < 20; i++ ) {
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "AFT_DME2_REQ_TRACE_ON", "true" );

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload("this is endpoint staleness test");
			//			sender.setReplyHandler(replyHandler);
			//			sender.setHeaders(hm);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "this is endpoint staleness test" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			try {
				replyHandler.getResponse( 10000 );
				counter++;

				String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );

				if ( traceInfo != null ) {
					System.out.println( " TRACEINFO from request " + traceInfo + " requestCounter=" + counter );
					if ( !flipFlag && traceInfo.contains( "4902" ) ) {
						bham_2_Launcher.destroy();
						Thread.sleep( 5000 );
						flipFlag = true;
					} else if ( flipFlag && traceInfo.contains( "4902" ) ) {
						fail( " endpoint staleness did not set effectively" );
					}
				}
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}

		Thread.sleep( 10000 );

		// sleep for 10 secs and validate whether request had 4902 port url attempted
		EchoReplyHandler replyHandler = new EchoReplyHandler();

		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put( "AFT_DME2_REQ_TRACE_ON", "true" );

		/**    DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
 sender.setPayload("this is endpoint staleness test");
 sender.setReplyHandler(replyHandler);
 sender.setHeaders(hm);
 sender.send();
		 */
		Request request =
				new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
				.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
				.withLookupURL( uriStr ).build();

		DME2Client sender = new DME2Client( manager, request );

		DME2Payload payload = new DME2TextPayload( "this is endpoint staleness test" );

		sender.setResponseHandlers( replyHandler );

		sender.send( payload );

		try {
			replyHandler.getResponse( 10000 );
			counter++;

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );

			if ( traceInfo != null ) {
				System.out.println( " TRACEINFO from request " + traceInfo );
				assertFalse( traceInfo.contains( "4902" ) );
				return;
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		fail( "if it reaches here, endpoint staleness did not set as expected" );
	}

	/**  @SuppressWarnings("unused") public void testExchangeOnException() throws Exception
{

//NOTE: this test case needs to be reworked.  The SSL approach worked in earlier versions of
//jetty but it appears later versions will NOT response to certain SSL failure conditions
//like this, probably to avoid acknowledging something about the server.  This causes
//a timeout condition rather than an onException condition

String currDir = (new File(System.getProperty("user.dir"))).getAbsolutePath();
String srcConfigDir = currDir + File.separator + "src" + File.separator	+ "test" + File.separator + "etc" + File.separator + "svc_config";
String fsDir = currDir + "/dme2-fs-registry";

String name = "/service=MyService/version=5.0.0/envContext=PROD/routeOffer=EXCHANGE_EXCEPTION";
String allowedRoles[] = { "myclientrole" };
String loginMethod = "BASIC";
String realm = "myrealm";
String javaHome = System.getProperty("java.home");

Properties props = new Properties();
props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
props.setProperty("AFT_DME2_CLIENT_MAX_RETRY_RECURSION", "4");
props.setProperty("AFT_DME2_SVCCONFIG_DIR", "file:///" + srcConfigDir);
props.setProperty("AFT_DME2_EP_REGISTRY_FS_DIR", fsDir);
props.setProperty("DME2_EP_REGISTRY_CLASS", "DME2FS");
props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
props.setProperty("AFT_LATITUDE", "33.373900");
props.setProperty("AFT_LONGITUDE", "-86.798300");
props.setProperty("AFT_DME2_REQ_TRACE_ON", "true");

DME2Configuration config = new DME2Configuration("testExchangeOnException", props);

DME2Manager mgr = new DME2Manager("testExchangeOnException", config);

//		DME2Manager mgr = new DME2Manager("testExchangeOnException", props);
mgr.bindServiceListener(name, new EchoServlet(name, "EXCHANGE_EXCEPTION"), null, null, null);

Properties props1 = new Properties();
props1.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
props1.setProperty("AFT_DME2_CLIENT_MAX_RETRY_RECURSION", "4");
props1.setProperty("AFT_DME2_SVCCONFIG_DIR", "file:///" + srcConfigDir);
props1.setProperty("AFT_DME2_EP_REGISTRY_FS_DIR", fsDir);
props1.setProperty("DME2_EP_REGISTRY_CLASS", "DME2FS");
props1.setProperty("AFT_ENVIRONMENT", "AFTUAT");
props1.setProperty("AFT_LATITUDE", "33.373900");
props1.setProperty("AFT_LONGITUDE", "-86.798300");
props1.setProperty("AFT_DME2_REQ_TRACE_ON", "true");

DME2Configuration config1 = new DME2Configuration("testExchange1OnException", props1);

DME2Manager mgr1 = new DME2Manager("testExchange1OnException", config1);
//		DME2Manager mgr1 = new DME2Manager("testExchange1OnException", props1);
mgr1.bindServiceListener(name, new EchoServlet(name, "EXCHANGE_EXCEPTION"), null, null, null);

Thread.sleep(5000);

Locations.CHAR.set();
String uriStr = "http://DME2RESOLVE/service=MyService/version=5.0.0/envContext=PROD/routeoffer=EXCHANGE_EXCEPTION";

int counter = 0;
boolean flipFlag = false;
boolean onExceptionFound = false;

try
{
// sleep for 10 secs and validate whether request had 4902 port url attempted
for (int i = 0; i < 5; i++)
{
EchoReplyHandler replyHandler = new EchoReplyHandler();

HashMap<String, String> hm = new HashMap<String, String>();
hm.put("AFT_DME2_REQ_TRACE_ON", "true");

//				DME2Client sender = new DME2Client(mgr, new URI(uriStr), 30000);
//				sender.setPayload("this is endpoint staleness test");
//				sender.setReplyHandler(replyHandler);
//				sender.setHeaders(hm);
//				sender.send();

Request request = new RequestBuilder(mgr.getClient(), new HttpConversation(), new URI(uriStr)).withHeaders(hm).withHttpMethod("POST").withReadTimeout(300000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();

DME2Client sender = new DME2Client(mgr, request);

Payload payload = new TextPayload("this is endpoint staleness test");

sender.setResponseHandlers(replyHandler);

sender.send(payload);

//This needs to be turned on
// Invoke onException explicitly


if (i == 1)	sender.invokeOnException();

try
{
replyHandler.getResponse(10000);
counter++;
String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");

if (traceInfo != null)
{
System.out.println(" TRACEINFO from request " + traceInfo);
onExceptionFound = traceInfo.contains("onException");

if (onExceptionFound) break;
}
}
catch (Exception e)
{
e.printStackTrace();

if (replyHandler.getResponseHeaders() == null)
{
fail("no headers on response, so we know we didn't get a response through onException");
}

String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");

if (traceInfo != null)
{
System.out.println(" TRACEINFO from request " + traceInfo);
onExceptionFound = traceInfo.contains("onException");

if (onExceptionFound) break;
}
}
}

assertTrue(onExceptionFound);
}
catch (Exception e)
{
e.printStackTrace();
fail(e.getMessage());
}
finally
{
try	{mgr.unbindServiceListener(name);}
catch (Exception ex){}

try	{mgr1.unbindServiceListener(name);}
catch (Exception ex){}
}
}
	 */

	/**
	 * @SuppressWarnings("unused") public void testExchangeOnExceptionDotry() throws Exception {
	 * <p>
	 * //NOTE : this test case needs to be reworked.  The SSL approach worked in earlier versions of //jetty but it
	 * appears later versions will NOT response to certain SSL failure conditions //like this, probably to avoid
	 * acknowledging something about the server.  This causes //a timeout condition rather than an onException condition
	 * <p>
	 * String currDir = (new File(System.getProperty("user.dir"))).getAbsolutePath(); String srcConfigDir = currDir +
	 * File.separator + "src" + File.separator	+ "test" + File.separator + "etc" + File.separator + "svc_config"; String
	 * fsDir = currDir + "/dme2-fs-registry"; String name = "service=MyService/version=5.0.0/envContext=PROD/routeOffer=EXCHANGE_EXCEPTION_DOTRY";
	 * String allowedRoles[] = { "myclientrole" }; String loginMethod = "BASIC"; String realm = "myrealm"; String javaHome
	 * = System.getProperty("java.home");
	 * <p>
	 * Properties props = new Properties(); props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	 * props.setProperty("AFT_DME2_CLIENT_MAX_RETRY_RECURSION", "4"); props.setProperty("AFT_DME2_SVCCONFIG_DIR",
	 * "file:///" + srcConfigDir); props.setProperty("AFT_DME2_EP_REGISTRY_FS_DIR", fsDir);
	 * props.setProperty("DME2_EP_REGISTRY_CLASS", "DME2FS"); props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
	 * props.setProperty("AFT_LATITUDE", "33.373900"); props.setProperty("AFT_LONGITUDE", "-86.798300");
	 * props.setProperty("AFT_DME2_REQ_TRACE_ON", "true");
	 * <p>
	 * DME2Configuration config = new DME2Configuration("testExchangeOnExceptionDotry", props);
	 * <p>
	 * DME2Manager mgr = new DME2Manager("testExchangeOnExceptionDotry", config);
	 * <p>
	 * //		DME2Manager mgr = new DME2Manager("testExchangeOnExceptionDotry", props); mgr.bindServiceListener(name, new
	 * EchoServlet(name, "EXCHANGE_EXCEPTION_DOTRY"), null, null, null);
	 * <p>
	 * Properties props1 = new Properties(); props1.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	 * props1.setProperty("AFT_DME2_CLIENT_MAX_RETRY_RECURSION", "4"); props1.setProperty("AFT_DME2_SVCCONFIG_DIR",
	 * "file:///" + srcConfigDir); props1.setProperty("AFT_DME2_EP_REGISTRY_FS_DIR", fsDir);
	 * props1.setProperty("DME2_EP_REGISTRY_CLASS", "DME2FS"); props1.setProperty("AFT_ENVIRONMENT", "AFTUAT");
	 * props1.setProperty("AFT_LATITUDE", "33.373900"); props1.setProperty("AFT_LONGITUDE", "-86.798300");
	 * props1.setProperty("AFT_DME2_REQ_TRACE_ON", "true");
	 * <p>
	 * DME2Configuration config1 = new DME2Configuration("testExchange1OnExceptionDoTry", props1);
	 * <p>
	 * DME2Configuration config = new DME2Configuration("testExchangeOnExceptionDotry", props);
	 * <p>
	 * DME2Manager mgr = new DME2Manager("testExchangeOnExceptionDotry", config);
	 * <p>
	 * //		DME2Manager mgr1 = new DME2Manager("testExchange1OnExceptionDoTry", config1); //		DME2Manager mgr1 = new
	 * DME2Manager("testExchange1OnExceptionDoTry", props1); mgr1.bindServiceListener(name, new EchoServlet(name,
	 * "EXCHANGE_EXCEPTIONDoTry"), null, null, null);
	 * <p>
	 * Thread.sleep(5000);
	 * <p>
	 * Locations.CHAR.set(); String uriStr = "http://DME2RESOLVE/service=MyService/version=5.0.0/envContext=PROD/routeoffer=EXCHANGE_EXCEPTION_DOTRY";
	 * <p>
	 * int counter = 0; boolean flipFlag = false; boolean onExceptionFound = false;
	 * <p>
	 * try { // sleep for 10 secs and validate whether request had 4902 port url attempted for (int i = 0; i < 5; i++) {
	 * EchoReplyHandler replyHandler = new EchoReplyHandler();
	 * <p>
	 * HashMap<String, String> hm = new HashMap<String, String>(); hm.put("AFT_DME2_REQ_TRACE_ON", "true");
	 * <p>
	 * DME2Client sender = new DME2Client(mgr, new URI(uriStr), 30000); sender.setPayload("this is endpoint staleness
	 * test"); sender.setReplyHandler(replyHandler); sender.setHeaders(hm); sender.send();
	 * <p>
	 * // Invoke onException explicitly if (i == 1)	sender.invokeOnException();
	 * <p>
	 * try { replyHandler.getResponse(10000); counter++;
	 * <p>
	 * String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO"); if (traceInfo != null) {
	 * System.out.println(" TRACEINFO from request " + traceInfo);
	 * <p>
	 * // Identify whether same URL was invoked multiple times String[] eps = traceInfo.split(";"); ArrayList<String> list
	 * = new ArrayList<String>();
	 * <p>
	 * for (int j = 0; j < eps.length; j++) { list.add(eps[j]); }
	 * <p>
	 * Set<String> set = new HashSet<String>(list); ArrayList<String> uniqList = new ArrayList<String>(set);
	 * <p>
	 * //doTry() should have avoided picking same url multiple times // so the uniqList size and list size should be
	 * equal, if not //it indicates there was duplicate attempt for same endpoint. assertEquals(list.size(),
	 * uniqList.size());
	 * <p>
	 * onExceptionFound = traceInfo.contains("onException"); if (onExceptionFound) break; } } catch (Exception e) {
	 * e.printStackTrace(); if (replyHandler.getResponseHeaders() == null) { fail("no headers on response, so we know we
	 * didn't get a response through onException"); }
	 * <p>
	 * String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
	 * <p>
	 * if (traceInfo != null) { System.out.println(" TRACEINFO from request " + traceInfo); onExceptionFound =
	 * traceInfo.contains("onException"); if (onExceptionFound) break; } } }
	 * <p>
	 * assertTrue(onExceptionFound); } catch (Exception e) { e.printStackTrace(); fail(e.getMessage()); } finally {
	 * try	{mgr.unbindServiceListener(name);} catch (Exception ex){}
	 * <p>
	 * try	{mgr1.unbindServiceListener(name);} catch (Exception ex){} } }
	 */

	public void testAllEndpointsFailedMessage() throws Exception {

		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration( "testAllEndpointsFailedMessage", props );

		DME2Manager manager = new DME2Manager( "testAllEndpointsFailedMessage", config );
		//		DME2Manager manager = new DME2Manager("testAllEndpointsFailedMessage", RegistryFsSetup.init());

		String[] bham_1_bau_se_args = {
				"-serverHost", "localhost",
				"-serverPort", "4950",
				"-registryType", "FS",
				"-servletClass", "FailoverServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_3_bau_se" };

		String[] bham_2_bau_se_args = {
				"-serverHost", "localhost",
				"-serverPort", "4952",
				"-registryType", "FS",
				"-servletClass", "FailoverServlet",
				"-serviceName", "service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_4_bau_se" };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );

		bham_1_Launcher.launch();
		bham_2_Launcher.launch();

		Thread.sleep( 5000 );
		Locations.CHAR.set();

		try {
			String uriStr =
					"http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload("this is a test");
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "this is all endpoint failure test" );

			sender.setResponseHandlers( replyHandler );
			sender.send( payload );
			try {
				replyHandler.getResponse( 30000 );
				Thread.sleep( 5000 );

				//				sender.setPayload("this is all endpoint failure test");
				sender.sendAndWait( payload );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) );
				return;
			}

			fail( "if it reaches here, all endpoint failure did not occur" );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}

	public void testSingleEndpointFastCacheRefreshWithDME2Resolve() throws Exception {
		String name =
				"/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		DME2Manager manager = null;
		DME2Manager manager1 = null;

		try {
			Properties props = RegistryFsSetup.init();
			props.setProperty( "AFT_DME2_PORT", "8765" ); // First service endpoint will have this port.
			props.setProperty( "DME2_SEP_CACHE_EMPTY_TTL_MS", "60000" );

			Properties props1 = RegistryFsSetup.init();
			props1.setProperty( "AFT_DME2_PORT", "8766" ); // Second service endpoint will have this port.
			//props1.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS","60000");
			props.setProperty( "DME2_SEP_EMPTY_CACHE_TTL_INTERVALS", "60000" );
			props.setProperty( "DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE", "false" );

			DME2Configuration config = new DME2Configuration( "TestSingleEndpointFastCacheRefreshWithDME2Resolve", props );

			manager = new DME2Manager( "TestSingleEndpointFastCacheRefreshWithDME2Resolve", config );

			//			manager = new DME2Manager("TestSingleEndpointFastCacheRefreshWithDME2Resolve", props);
			//			manager = new DME2Manager("TestSingleEndpointFastCacheRefreshWithDME2Resolve", config);

			DME2Configuration config1 = new DME2Configuration( "TestSingleEndpointFastCacheRefreshWithDME2Resolve", props1 );

			manager1 = new DME2Manager( "TestSingleEndpointFastCacheRefreshWithDME2Resolve", config1 );

			// Only going to publish the first endpoint for now.
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			Thread.sleep( 2000 );

			// Send request to the service. Everything should work as normal and we should get a response back.
			String clientURI =
					"http://DME2RESOLVE/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(clientURI), 31000);
			//			sender.setPayload("THIS IS A TEST!");
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			Request request = new RequestBuilder( new URI( clientURI ) )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( clientURI ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST!" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			String response = replyHandler.getResponse( 20000 );
			System.out.println( "--- Response from service: " + response );
			assertTrue( response.contains(
					"EchoServlet:::bau_se_1:::/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE" ) );

			// Check the endpoint cache and validate that the endpoint with 8765 is there and has the standard cache TTL value
			/**      DME2EndpointRegistry registry = manager.getEndpointRegistry();
 Map<String, DME2ServiceEndpointData> cache = manager.getStaleCache().;

 System.out.println("Endpoint cache contents: " + cache);
 assertTrue(cache.containsKey("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE"));

 DME2ServiceEndpointData data = cache.get("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE");
 System.out.println("Endpoints for cache entry: " + data.getEndpointList());
 System.out.println("CacheTTL valie for cache entry: " + data.getCacheTTL());

 assertTrue(data.getEndpointList().toString().contains(":8765"));
 assertTrue(data.getCacheTTL() == 300000);
			 */
			// Now, let's shutdown the first service. This will trigger a refresh to be called.
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}

			try {
				manager.getServer().stop();
			} catch ( Exception e ) {
			}
			Thread.sleep( 3000 );

			// Check the cache again. Unpublish should have removed the cached entries endpoint and set the TTL value to 1 minute (the empty TTL rate).
			/**      cache = registry.getEndpointCache();

 System.out.println("Endpoint cache contents: " + cache);
 assertTrue(cache.containsKey("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE"));

 data = cache.get("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE");
 System.out.println("Endpoints for cache entry: " + data.getEndpointList());
 System.out.println("CacheTTL valie for cache entry: " + data.getCacheTTL());

 assertTrue(data.getEndpointList().isEmpty());
 assertTrue(data.getCacheTTL() == 60000);
			 */
			// Now, let's publish the second service endpoint
			manager1.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );

			// Sleep for about a minute to let the cache refresh happen for the empty cache entry.
			// After a minute, refresh should have discovered the endpoint we just published and added to the cache and reset the cacheTTL value to the standard rate.
			Thread.sleep( 120000 );

			// Check the cache and see if the endpoint with port 8766 was added.
			/**      cache = registry.getEndpointCache();
 data = cache.get("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE");
 System.out.println("Endpoints for cache entry: " + data.getEndpointList());
 System.out.println("CacheTTL value for cache entry: " + data.getCacheTTL());

 assertTrue(data.getEndpointList().toString().contains(":8766"));
 assertTrue(data.getCacheTTL() == 300000);
			 */

			// Finally, let's send one last request. This should hit the endpoint listening on port 8766
			replyHandler = new EchoReplyHandler();

			//sender = new DME2Client(manager, new URI(clientURI), 31000);
			//sender.setPayload("THIS IS A TEST!");
			//sender.setReplyHandler(replyHandler);
			//sender.send();

			request = new RequestBuilder( new URI( clientURI ) )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( clientURI ).build();

			sender = new DME2Client( manager, request );

			payload = new DME2TextPayload( "THIS IS A TEST!" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			response = replyHandler.getResponse( 20000 );
			System.out.println( "--- Response from service: " + response );
			assertTrue( response.contains(
					"EchoServlet:::bau_se_1:::/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Resolve/version=1.0.0/envContext=LAB/routeOffer=BAU_SE" ) );

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
			if ( traceInfo != null ) {
				System.out.println( "--- Trace Info from request: " + traceInfo );
				assertTrue( traceInfo.contains( ":8766" ) );
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception ex ) {
			}

			try {
				manager1.unbindServiceListener( name );
			} catch ( Exception ex ) {
			}
		}
	}

	public void testSingleEndpointFastCacheRefreshWithDME2Search() throws Exception {
		DME2Manager manager = null;
		DME2Manager manager1 = null;

		String name =
				"/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

		try {
			Properties props = RegistryFsSetup.init();
			props.setProperty( "AFT_DME2_PORT", "8775" ); // First service endpoint will have this port.
			//props.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS","60000");
			props.setProperty( "DME2_SEP_EMPTY_CACHE_TTL_INTERVALS", "60000" );
			props.setProperty( "DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE", "false" );

			Properties props1 = RegistryFsSetup.init();
			props1.setProperty( "AFT_DME2_PORT", "8776" ); // Second service endpoint will have this port.
			//props1.setProperty("DME2_SEP_CACHE_EMPTY_TTL_MS","60000");
			props1.setProperty( "DME2_SEP_EMPTY_CACHE_TTL_INTERVALS", "60000" );
			props1.setProperty( "DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE", "false" );

			DME2Configuration config = new DME2Configuration( "TestSingleEndpointFastCacheRefreshWithDME2Search", props );

			manager = new DME2Manager( "TestSingleEndpointFastCacheRefreshWithDME2Search", config );

			//			manager = new DME2Manager("TestSingleEndpointFastCacheRefreshWithDME2Search", props);

			DME2Configuration config1 = new DME2Configuration( "TestSingleEndpointFastCacheRefreshWithDME2Search1", props1 );

			manager1 = new DME2Manager( "TestSingleEndpointFastCacheRefreshWithDME2Search1", config );

			//			manager1 = new DME2Manager("TestSingleEndpointFastCacheRefreshWithDME2Search1",	props1);

			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );
			Thread.sleep( 3000 );

			String clientURI =
					"http://DME2SEARCH/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/partner=YS";
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(clientURI), 31000);
			//			sender.setPayload("THIS IS A TEST!");
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			Request request = new RequestBuilder( new URI( clientURI ) )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( clientURI ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST!" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			String response = replyHandler.getResponse( 20000 );
			System.out.println( "--- Response from service: " + response );
			assertTrue( response.contains(
					"EchoServlet:::bau_se_1:::/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE" ) );

			// Check the endpoint cache and validate that the endpoint with 8765 is there and has the standard cache TTL value
			/**      DME2EndpointRegistryGRM registry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
 Map<String, DME2ServiceEndpointData> cache = registry.getEndpointCache();

 System.out.println("Endpoint cache contents: " + cache);
 assertTrue(cache.containsKey("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE"));

 DME2ServiceEndpointData data = cache.get("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE");
 System.out.println("Endpoints for cache entry: " + data.getEndpointList());
 System.out.println("CacheTTL value for cache entry: " + data.getCacheTTL());

 assertTrue(data.getEndpointList().toString().contains(":8775"));
 assertTrue(data.getCacheTTL() == 300000);
			 */
			// Now, let's shutdown the first service. This will trigger a refresh to be called.
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {
			}

			try {
				manager.getServer().stop();
			} catch ( Exception e ) {
			}
			Thread.sleep( 3000 );

			// Check the cache again. Unpublish should have removed the cached entries endpoint and set the TTL value to 1 minute (the empty TTL rate).
			/**      cache = registry.getEndpointCache();

 System.out.println("Endpoint cache contents: " + cache);
 assertTrue(cache.containsKey("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE"));

 data = cache.get("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE");
 System.out.println("Endpoints for cache entry: " + data.getEndpointList());
 System.out.println("CacheTTL valie for cache entry: " + data.getCacheTTL());

 assertTrue(data.getEndpointList().isEmpty());
 assertTrue(data.getCacheTTL() == 60000);
			 */
			// Now, let's publish the second service endpoint
			manager1.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );

			// Sleep for about a minute to let the cache refresh happen for the empty cache entry.
			// After a minute, refresh should have discovered the endpoint we just published and added to the cache and reset the cacheTTL value to the standard rate.
			Thread.sleep( 120000 );

			// Check the cache and see if the endpoint with port 8766 was added.
			/**      cache = registry.getEndpointCache();
 data = cache.get("/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE");
 System.out.println("Endpoints for cache entry: " + data.getEndpointList());
 System.out.println("CacheTTL valie for cache entry: " + data.getCacheTTL());

 assertTrue(data.getEndpointList().toString().contains(":8776"));
 assertTrue(data.getCacheTTL() == 300000);
			 */
			// Finally, let's send one last request. This should hit the endpoint listening on port 8766
			replyHandler = new EchoReplyHandler();

			//			sender = new DME2Client(manager, new URI(clientURI), 31000);
			//			sender.setPayload("THIS IS A TEST!");
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			request = new RequestBuilder( new URI( clientURI ) )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( clientURI ).build();

			sender = new DME2Client( manager, request );

			payload = new DME2TextPayload( "THIS IS A TEST!" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			//			response = replyHandler.getResponse(20000);
			System.out.println( "--- Response from service: " + response );
			assertTrue( response.contains(
					"EchoServlet:::bau_se_1:::/service=com.att.aft.TestSingleEndpointFastCacheRefreshWithDME2Search/version=1.0.0/envContext=LAB/routeOffer=BAU_SE" ) );

			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
			if ( traceInfo != null ) {
				System.out.println( "--- Trace Info from request: " + traceInfo );
				assertTrue( traceInfo.contains( ":8776" ) );
			}
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception ex ) {
			}

			try {
				manager1.unbindServiceListener( name );
			} catch ( Exception ex ) {
			}
		}
	}

	/**
	 * @throws Exception
	 */
	public void testEndpointLeaseExpiryIgnored() throws Exception {
		DME2Manager manager = null;

		String service = "com.att.aft.TestEndpointLeaseExpiryIgnored";
		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String name =
				"service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

		try {

			Properties props = RegistryFsSetup.init();
			props.setProperty( "AFT_DME2_PORT",
					"9775" ); // Set 2 different ports so that endpoints are being different and refresh loads new reference.
			props.setProperty( DME2Constants.DME2_SEP_LEASE_LENGTH_MS, "2000" );

			DME2Configuration config = new DME2Configuration( "TestEndpointLeaseExpiryIgnored", props );

			manager = new DME2Manager( "TestEndpointLeaseExpiryIgnored", config );

			//manager = new DME2Manager("TestEndpointLeaseExpiryIgnored",	props);
			manager.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ), null, null, null );

			DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
			List<DME2Endpoint> endpoints = svcRegistry.findEndpoints( service, version, envContext, routeOffer );

			Thread.sleep( 3000 );

			DME2Endpoint found = null;
			for ( DME2Endpoint ep : endpoints ) {
				//The below indicates the endpoint has an expired time already and if this endpoint request works on below call it indicates lease expiry is being ignored for the call too.
				if ( ep.getLease() < System.currentTimeMillis() ) {
					found = ep;
				}
			}
			assertNotNull( found );

			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.TestEndpointLeaseExpiryIgnored/version=1.0.0/envContext=DEV/partner=DEF";

			for ( int i = 0; i < 1; i++ ) {

				StringBuffer buf = new StringBuffer();
				buf.append( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:B"
						+ "ody><SnapshotRequest xmlns=\"http://aft.att.com/metrics/metricsquery\"><interval>TEST</interval><env>"
						+
						"LAB</env><grouping>HOST</grouping><grouping>CONTAINER_NAME</grouping><grouping>PROTOCOL</grouping><query/><"
						+ "/SnapshotRequest></soap:Body></soap:Envelope>" );

				EchoReplyHandler replyHandler = new EchoReplyHandler();

				//				DME2Client sender = new DME2Client(manager, new URI(uriStr), 31000);
				//				sender.setPayload(buf.toString());
				//				sender.setReplyHandler(replyHandler);

				Request request = new RequestBuilder( new URI( uriStr ) )
						.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
						.withLookupURL( uriStr ).build();

				DME2Client sender = new DME2Client( manager, request );

				DME2Payload payload = new DME2TextPayload( buf.toString() );

				try {
					sender.send( payload );

					String reply = replyHandler.getResponse( 40000 );
					assertTrue( reply != null );
				} catch ( Exception e ) {
					e.printStackTrace();
					assertFalse( e == null );
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			try {
				manager.unbindServiceListener( name );
			} catch ( Exception e ) {

			}
		}
	}

	public void testServerUnavailable() throws Exception {
		DME2Manager manager = null;
		try {
			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "testServerUnavailable", props );

			manager = new DME2Manager( "testServerUnavailable", config );

			//			manager = new DME2Manager("testServerUnavailable", RegistryFsSetup.init());
			Locations.CHAR.set();

			String uriStr =
					"http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=TEST";
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload("this is a test");
			//			sender.setReplyHandler(replyHandler);
			//			sender.setPayload("this is all endpoint failure test");
			//			sender.sendAndWait(30000);

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

			DME2Client client = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "this is all endpoint failure test" );

			client.setResponseHandlers( replyHandler );

			client.sendAndWait( payload );
		} catch ( Exception e ) {
			e.printStackTrace();
			assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) || e.getMessage().contains( "AFT-DME2-0703" ) );
		} finally {
			if ( manager != null ) {
				manager.stop();
			}
		}
	}

	public void testPrimarySequenceFailOver() throws Exception {

		// run the server in bham.
		String[] bham_1_bau_se_args = {
				"-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				"-serverPort", "10600",
				"-registryType", "GRM",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=com.att.aft.dme2.ROFailOverService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
				"-serviceCity", "BHAM",
				"-serverid", "bham_1_bau_se",
				"-platform", TestConstants.GRM_PLATFORM_TO_USE };

		String[] bham_2_bau_se_args = {
				"-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
				"-serverPort", "10601",
				"-registryType", "GRM",
				"-servletClass", "EchoServlet",
				"-serviceName", "service=com.att.aft.dme2.ROFailOverService/version=1.0.0/envContext=DEV/routeOffer=ATL",
				"-serviceCity", "BHAM",
				"-serverid", "bham_2_bau_se",
				"-platform", TestConstants.GRM_PLATFORM_TO_USE };

		bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
		bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );

		bham_1_Launcher.launch();
		bham_2_Launcher.launch();

		// Save the route info
		RouteInfo rtInfo = new RouteInfo();
		rtInfo.setServiceName( "com.att.aft.dme2.ROFailOverService" );
		rtInfo.setEnvContext( "DEV" );

		RouteGroups rtGrps = new RouteGroups();
		rtInfo.setRouteGroups( rtGrps );

		Properties props = RegistryFsSetup.init();

		RegistryFsSetup grmInit = new RegistryFsSetup();

		DME2Configuration config = new DME2Configuration( "TestPrimarySequenceFailOver", props );

//		grmInit.saveRouteInfoForROFailover( config, rtInfo, "DEV" );

		Thread.sleep( 5000 );
		Locations.CHAR.set();

		DME2Manager manager = new DME2Manager( "TestPrimarySequenceFailOver", config );

		String uriStr =
				"http://DME2SEARCH/service=com.att.aft.dme2.ROFailOverService/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
		EchoReplyHandler replyHandler = new EchoReplyHandler();

		//		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		//		sender.setPayload("this is a test");
		//		sender.setReplyHandler(replyHandler);
		//		sender.send();

		Request request =
				new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
				.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

		DME2Client client = new DME2Client( manager, request );

		DME2Payload payload = new DME2TextPayload( "this is a test" );

		client.setResponseHandlers( replyHandler );

		client.send( payload );

		String reply = replyHandler.getResponse( 60000 );
		System.out.println( "Response = " + reply );
		assertNotNull( reply );

		//Stop server that replied
		String otherServer = null;
		if ( reply.indexOf( "bham_1_bau_se" ) != -1 ) {
			bham_1_Launcher.destroy();
			otherServer = "bham_2_bau_se";
		} else if ( reply.indexOf( "bham_2_bau_se" ) != -1 ) {
			bham_2_Launcher.destroy();
			otherServer = "bham_1_bau_se";
		} else {
			fail( "reply is not from bham_1_bau_se or bham_2_bau_se.  reply= " + reply );
		}

		Thread.sleep( 5000 );
		replyHandler = new EchoReplyHandler();

		//sender = new DME2Client(manager, new URI(uriStr), 30000);
		//sender.setPayload("this is a test");
		//sender.setReplyHandler(replyHandler);
		//sender.send();

		request =
				new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
				.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

		client = new DME2Client( manager, request );

		payload = new DME2TextPayload( "this is a test" );

		client.setResponseHandlers( replyHandler );

		client.send( payload );

		reply = replyHandler.getResponse( 60000 );
		System.out.println( "Response 2 = " + reply );
		assertTrue( reply.contains( otherServer ) );
	}

	public void testEofExceptionFailOver() throws Exception {
		try {

			Properties props = RegistryFsSetup.init();
			props.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );

			DME2Configuration config = new DME2Configuration( "TestEofExceptionFailOver", props );

			DME2Manager manager = new DME2Manager( "TestEofExceptionFailOver", config );

			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName( "com.att.aft.dme2.TestEofExceptionFailOver" );
			rtInfo.setEnvContext( "DEV" );

			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups( rtGrps );

			// run the server in bham.
			String[] bham_1_bau_se_args = {
					"-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
					"-serverPort", "21600",
					"-registryType", "GRM",
					"-servletClass", "EchoServlet",
					"-serviceName",
					"service=com.att.aft.dme2.TestEofExceptionFailOver/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
					"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

			String[] bham_2_bau_se_args = {
					"-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
					"-serverPort", "21601",
					"-registryType", "GRM",
					"-servletClass", "EchoResponseServlet",
					"-serviceName",
					"service=com.att.aft.dme2.TestEofExceptionFailOver/version=1.0.0/envContext=DEV/routeOffer=ATL",
					"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

			bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
			bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();

			RegistryFsSetup grmInit = new RegistryFsSetup();
//			grmInit.saveRouteInfoForEofFailover( config, rtInfo, "DEV" );

			Thread.sleep( 5000 );
			Locations.CHAR.set();
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.TestEofExceptionFailOver/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "echoSleepTimeMs",
					"120000" ); // Have the request sleeping for 2 mins on server side , so that we can destroy the server between to trigger EofException and let dme2 failover
			hm.put( "AFT_DME2_REQ_TRACE_ON", "true" );

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload("this is a test");
			//			sender.setReplyHandler(replyHandler);
			//			sender.setHeaders(hm);
			//			sender.send();

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "this is a test" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			// Kill the primary sequence instance: bham_1_Launcher
			bham_1_Launcher.destroy();
			Thread.sleep( 10000 );

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( reply );

			assertTrue( reply.contains( "routeOffer=ATL" ) );
			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );

			System.out.println( traceInfo );
			assertTrue( traceInfo.contains( "onException=early EOF" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
		}
	}

	public void testPrimarySequenceFailOverAndRestore() throws Exception {
		try {
			// run the server in bham.
			String[] bham_1_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
					"-serverPort", "10600", "-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
					"service=com.att.aft.dme2.ROFailOverService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
					"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

			String[] bham_2_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
					"-serverPort", "10601", "-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
					"service=com.att.aft.dme2.ROFailOverService/version=1.0.0/envContext=DEV/routeOffer=ATL",
					"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

			String[] bham_3_bau_se_args = { "-serverHost", InetAddress.getLocalHost().getCanonicalHostName(),
					"-serverPort", "10602", "-registryType", "GRM", "-servletClass", "EchoServlet", "-serviceName",
					"service=com.att.aft.dme2.ROFailOverService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE",
					"-serviceCity", "BHAM", "-serverid", "bham_3_bau_se", "-platform", TestConstants.GRM_PLATFORM_TO_USE };

			bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
			bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );

			bham_1_Launcher.launch();
			bham_2_Launcher.launch();

			// Save the route info
			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName( "com.att.aft.dme2.ROFailOverService" );
			rtInfo.setEnvContext( "DEV" );

			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups( rtGrps );

			RegistryFsSetup grmInit = new RegistryFsSetup();

			Properties props = RegistryFsSetup.init();

			DME2Configuration config = new DME2Configuration( "TestPrimarySequenceFailOverAndRestore", props );

//			grmInit.saveRouteInfoForROFailover( config, rtInfo, "DEV" );

			Thread.sleep( 5000 );

			props.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
			props.setProperty( "AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS", "40000" );

			DME2Manager manager = new DME2Manager( "TestPrimarySequenceFailOverAndRestore", config );

			//DME2Manager manager = new DME2Manager("TestPrimarySequenceFailOverAndRestore", props);
			Locations.CHAR.set();

			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.ROFailOverService/version=1.0.0/envContext=DEV/dataContext=205977/partner=test2";
			EchoReplyHandler replyHandler = new EchoReplyHandler();

			//			DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload("this is a test");
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			Request request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

			DME2Client sender = new DME2Client( manager, request );

			DME2Payload payload = new DME2TextPayload( "this is a test" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			String reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response = " + reply );
			assertNotNull( reply );

			// stop server that replied
			String otherServer = null;
			if ( reply.indexOf( "bham_1_bau_se" ) != -1 ) {
				bham_1_Launcher.destroy();
				otherServer = "bham_2_bau_se";
			} else if ( reply.indexOf( "bham_2_bau_se" ) != -1 ) {
				bham_2_Launcher.destroy();
				otherServer = "bham_1_bau_se";
			} else {
				fail( "reply is not from bham_1_bau_se or bham_2_bau_se. Reply = " + reply );
			}

			Thread.sleep( 5000 );
			replyHandler = new EchoReplyHandler();

			//sender = new DME2Client(manager, new URI(uriStr), 30000);
			//sender.setPayload("this is a test");
			//sender.setReplyHandler(replyHandler);
			//sender.send();

			request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

			sender = new DME2Client( manager, request );

			payload = new DME2TextPayload( "this is a test" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );
			reply = replyHandler.getResponse( 60000 );
			System.out.println( "Response 2 = " + reply );
			assertNotNull( reply );
			assertTrue( reply.contains( "routeOffer=ATL" ) );

			//Start the primary sequence routeoffer BAU_SE again. Run the server in bham.
			bham_3_Launcher = new ServerControllerLauncher( bham_3_bau_se_args );
			bham_3_Launcher.launch();

			Thread.sleep( 5000 );
			manager.getEndpointRegistry().refresh();

			replyHandler = new EchoReplyHandler();

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put( "AFT_DME2_REQ_TRACE_ON", "true" );

			//			sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload("this is a test");
			//			sender.setHeaders(hm);
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			request = new RequestBuilder( new URI( uriStr ) ).withHeaders( hm )
					.withHttpMethod( "POST" ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr )
					.build();

			sender = new DME2Client( manager, request );

			payload = new DME2TextPayload( "this is a test" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			reply = replyHandler.getResponse( 60000 );
			Thread.sleep( 5000 );

			replyHandler = new EchoReplyHandler();

			hm = new HashMap<String, String>();
			hm.put( "AFT_DME2_REQ_TRACE_ON", "true" );

			//			sender = new DME2Client(manager, new URI(uriStr), 30000);
			//			sender.setPayload("this is a test");
			//			sender.setHeaders(hm);
			//			sender.setReplyHandler(replyHandler);
			//			sender.send();

			request =
					new RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
					.withHeaders( hm ).withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr )
					.build();

			sender = new DME2Client( manager, request );

			payload = new DME2TextPayload( "this is a test" );

			sender.setResponseHandlers( replyHandler );

			sender.send( payload );

			reply = replyHandler.getResponse( 60000 );
			String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );

			System.out.println( "Response 3 = " + reply );
			System.out.println( traceInfo );

			assertTrue( traceInfo.contains( ":10602" ) );
			assertNotNull( reply );
			assertTrue( reply.contains( "routeOffer=BAU_SE" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			e.getMessage();
		} finally {

		}
	}

}