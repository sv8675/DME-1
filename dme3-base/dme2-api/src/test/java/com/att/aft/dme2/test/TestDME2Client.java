package com.att.aft.dme2.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.TestIteratorEndpointHandler;
import com.att.aft.dme2.iterator.test.servlet.DME2SimpleServlet;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.server.test.EchoServlet;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.server.test.RegistryGrmSetup;
import com.att.aft.dme2.server.test.RouteInfoCreatorUtil;
//import com.att.aft.dme2.test.DME2FilterHolder.RequestDispatcherType;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2URIUtils;

public class TestDME2Client extends DME2BaseTestCase {

	DME2RouteInfo mockRouteInfo;
	DmeUniformResource mockUniformResource;

	@Before
	public void setUp() {
		super.setUp();
		System.setProperty( "AFT_DME2_COLLECT_SERVICE_STATS", "true" );
		System.setProperty( "AFT_DME2_WS_ENABLE_TRACE_ROUTE", "true" );
	}

	@After
	public void tearDown() {
		super.tearDown();
		System.clearProperty( "AFT_DME2_COLLECT_SERVICE_STATS" );
		System.clearProperty( "AFT_DME2_WS_ENABLE_TRACE_ROUTE" );
	}

	@Test
	public void testDME2ClientRequestWithDME2Search() throws Exception {
		//Testing new functionality where user can define a stalenessInMin attribute for a routeOffer.
		//If this attribute is present, it will override the default value for how long an end point remains in the stale cache


		String serviceName = "com.att.aft.dme2.test.TestDME2ClientWithSearch";
		String serviceVersion = "1.0.0";
		String envContext = "TEST";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		DME2Manager mgr = null;
		//String service = "/service=com.att.aft.dme2.test.TestDME2ClientWithSearch/version=1.0.0/envContext=LAB/routeOffer=DME2_PRIMARY";

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();
			//      props.setProperty( "AFT_DME2_PORT", "30312" );

			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "TestDME2ClientWithSearch" ) );

			//Create RouteInfo for this test case
			RouteInfo routeInfo =
					RouteInfoCreatorUtil.createRouteInfo( "com.att.aft.dme2.test.TestDME2ClientWithSearch", "1.0.0", "TEST" );

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM( config, routeInfo, "LAB" );

			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2ClientWithSearch/version=1.0.0/routeOffer=TEST/envContext=TEST/partner=DEFAULT";

			TestIteratorEndpointHandler handler = new TestIteratorEndpointHandler();
			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "Http" )
					.withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withIteratorEndpointOrderHandler(handler).build();

			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "Testing testCustomResponseHandler" );


			try {
				String reply = (String) client.sendAndWait( payload );
				System.out.println( "REPLY=" + reply );
				assertTrue( reply != null );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e != null );
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			assertTrue( e != null );

		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2ClientRequestWithDirectURI_WithContext() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndContext";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();
			//      props.setProperty( "AFT_DME2_PORT", "32406" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );

			String pattern[] = { "/test" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			//svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
			//svcHolder.setContext("/test");

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );
			System.out.println( serviceURI );

			String clientURI =
					String.format( "http://%s:%s%s", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort(), serviceURI );
			//String clientURI = "http://DME2RESOLVE" + serviceURI;

			//client.setAllowAllHttpReturnCodes(true);

			DME2Client client = new DME2Client(mgr, new URI(clientURI), 300000);
			client.setContext("/test");
			client.setPayload("THIS IS A TEST");
			client.setMethod("GET");
			client.setAllowAllHttpReturnCodes(true);
			String reply = client.sendAndWait(30000);

			System.out.println( "\n ------- Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndContext/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2ClientRequestWithDirectURI_WithContext_WithReqBuilder() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndContextWithReqBuilder";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {

			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();
			//      props.setProperty( "AFT_DME2_PORT", "32404" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );

			String pattern[] = { "/test" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			//svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
			//svcHolder.setContext("/test");

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );
			System.out.println( serviceURI );

			String clientURI =
					String.format( "http://%s:%s%s", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort(), serviceURI );
			//String clientURI = "http://DME2RESOLVE" + serviceURI;

			//client.setAllowAllHttpReturnCodes(true);

			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/test" ).withReadTimeout( 30000 ).withLookupURL( clientURI ).build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

			String reply = (String) client.sendAndWait( payload );

			System.out.println( "\n ------- Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndContextWithReqBuilder/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2ClientRequestWithDirectURI_WithSubContext() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContext";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {

			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();
			//      props.setProperty( "AFT_DME2_PORT", "54329" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );

			String pattern[] = { "/test", "/test/foo" };
			//String pattern[] = {"/test/foo"};
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			//svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
			//svcHolder.setContext("/test");

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 6000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );


			String clientURI =
					String.format( "http://%s:%s%s", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort(), serviceURI );
			//String clientURI = "http://DME2RESOLVE" + serviceURI;

			//client.setAllowAllHttpReturnCodes(true);
			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/test" ).withSubContext( "/foo" ).withReadTimeout( 30000 ).withLookupURL( clientURI )
					.build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

			String reply = (String) client.sendAndWait( payload );
			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContext/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			//Check exception message and validate that it has the real uri that was attempted with the context path appended to it
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}


	@Test
	public void testDME2ClientRequestWithDirectURI_WithSubContext_2() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContext_2";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {
			Properties props = new Properties();
			//props.setProperty( "AFT_DME2_PORT", "54339" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      
			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );


			String pattern[] = { "/foo" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			svcHolder.setServlet( new EchoResponseServlet( serviceURI, "testID" ) );
			svcHolder.setContext( "/root" );

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );

			System.out.println( " mgr.getServer().getServerProperties().getPort() : " + mgr.getServer().getServerProperties().getPort());
			String clientURI =
					String.format( "http://%s:%s%s", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort(), serviceURI );
			//client.setAllowAllHttpReturnCodes(true);

			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/root" ).withSubContext( "/foo" ).withReadTimeout( 30000 ).withLookupURL( clientURI )
					.build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

			String reply = (String) client.sendAndWait( payload );
			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContext_2/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}


	@Test
	public void testDME2ClientRequestWithDirectURI_WithSubContext_3() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContext_3";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {
			Properties props = new Properties();
			//      props.setProperty( "AFT_DME2_PORT", "54338" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );          

			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			String pattern[] = { "/foo" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			svcHolder.setServlet( new EchoResponseServlet( serviceURI, "testID" ) );
			svcHolder.setContext( "/root" );

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );

			String clientURI = "http://DME2RESOLVE" + serviceURI;

			//client.setAllowAllHttpReturnCodes(true);
			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/root" ).withSubContext( "/foo" ).withReadTimeout( 30000 ).withLookupURL( clientURI )
					.build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
			String reply = (String) client.sendAndWait( payload );
			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContext_3/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2ClientRequestWithDirectURI_WithSubContextContainingMulitplePaths() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContextContainingMulitplePaths";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {

			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();
			//      props.setProperty( "AFT_DME2_PORT", "54325" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            

			DME2Configuration config = new DME2Configuration(
					"com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContextContainingMulitplePaths", props );
			mgr = new DME2Manager(
					"com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContextContainingMulitplePaths", config );

			String pattern[] = { "/test", "/test/foo", "/test/foo/bar" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			//svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
			//svcHolder.setContext("/test");

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );


			String clientURI =
					String.format( "http://%s:%s%s", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort(), serviceURI );
			//String clientURI = "http://DME2RESOLVE" + serviceURI;

			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/test" ).withReadTimeout( 30000 ).withLookupURL( clientURI ).build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

			String reply = (String) client.sendAndWait( payload );
			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContextContainingMulitplePaths/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

			Request request2 =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/test" ).withSubContext( "/foo" ).withReadTimeout( 30000 ).withLookupURL( clientURI )
					.build();
			DME2Client client2 = new DME2Client( mgr, request2 );
			DME2Payload payload2 = new DME2TextPayload( "THIS IS A TEST" );

			reply = (String) client2.sendAndWait( payload2 );
			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContextContainingMulitplePaths/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

			Request request3 =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/test" ).withSubContext( "/foo/bar" ).withReadTimeout( 30000 ).withLookupURL( clientURI )
					.build();
			DME2Client client3 = new DME2Client( mgr, request3 );
			DME2Payload payload3 = new DME2TextPayload( "THIS IS A TEST" );

			reply = (String) client.sendAndWait( payload3 );
			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndSubContextContainingMulitplePaths/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2ClientRequestWithDirectURI_WithSubContextInClientURI() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithSubContextInClientURI";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {
			Properties props = new Properties();
			//props.setProperty( "AFT_DME2_PORT", "54324" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );

			String pattern[] = { "/test", "/test/foo", "/test/foo/bar" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			//svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
			//svcHolder.setContext("/test");

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );

			String clientURI =
					String.format( "http://%s:%s/test/foo", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort() );
			//String clientURI = "http://DME2RESOLVE" + serviceURI;

			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withReadTimeout( 30000 ).withLookupURL( clientURI ).build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
			String reply = (String) client.sendAndWait( payload );


			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithSubContextInClientURI/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2ClientRequestWithDirectURI_WithDME2ContextPattern() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndDME2ContextPattern";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {
			Properties props = new Properties();
			//      props.setProperty( "AFT_DME2_PORT", "54311" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			String pattern[] =
				{ "/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndDME2ContextPattern/version=1.0.0/envContext=LAB/routeOffer=TEST" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );


			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );

			String clientURI =
					String.format( "http://%s:%s%s", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort(), serviceURI );

			System.out.println("clientURI : " + clientURI);

			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );
			//String response = (String) client.sendAndWait(payload);

			String reply = (String) client.sendAndWait( payload );
			System.out.println( "Response returned from the service response: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientRequestWithDirectURIAndDME2ContextPattern/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testInvokeDME2ClientWithJDBCLookupURI() {

		String clientURI =
				"dme2://DME2SEARCH/driver=jdbc:oracle:thin/service=com.att.aft.dme2.test.TestPublishDME2JDBCEndpoint/version=1.0.0/envContext=LAB/partner=TARGET";


		DME2Manager mgr = new DME2Manager();
		try {
			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "Http" )
					.withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();
			new DME2Client( mgr, request );

			fail(
					"Occurred in test case - Expecting exception to be thrown with error code of [AFT-DME2-9706]. DME2Client should not accept JDBC Lookup URI." );
		} catch ( Exception e ) {
			e.printStackTrace();
			assertTrue( e.getMessage().contains( "AFT-DME2-9706" ) );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( clientURI );
			} catch ( Exception e ) {
			}
		}
	}


	@Test
	public void testDME2ClientWithDME2Protocol() {
		try {
			String clientURI =
					"dme2://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2ClientWithDME2Protocol/version=1.0.0/envContext=LAB/partner=TARGET";

			DME2Manager mgr = new DME2Manager();
			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "Http" )
					.withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();
			new DME2Client( mgr, request );

			Request request1 =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "Http" )
					.withReadTimeout( 30000 ).withReturnResponseAsBytes( true ).withLookupURL( clientURI ).build();
			new DME2Client( mgr, request1 );

			Request request3 =
					new RequestBuilder(new URI( clientURI ) ).withCharset( "UTF-8" )
					.withHttpMethod( "Http" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( false )
					.withLookupURL( clientURI ).build();
			new DME2Client( mgr, request3 );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
		}
	}


	@Test
	public void testDME2ClientWithDME2Protocol_SendClientRequest() {
		DME2Manager mgr = null;

		String serviceName = "com.att.aft.dme2.test.TestDME2ClientWithDME2ProtocolSendClientRequest";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		try {

			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();
			//props.setProperty( "AFT_DME2_PORT", "32405" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );

			String pattern[] = { "/test" };
			DME2ServletHolder servletHolder =
					new DME2ServletHolder( new EchoResponseServlet( serviceURI, "testID" ), pattern );

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add( servletHolder );

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( serviceURI );
			svcHolder.setManager( mgr );
			svcHolder.setServletHolders( servletHolderList );
			//svcHolder.setServlet(new EchoResponseServlet(serviceURI, "testID"));
			//svcHolder.setContext("/test");

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add( contextListener );
			svcHolder.setContextListeners( contextList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 3000 );

			List<DME2Endpoint> endpoints =
					mgr.getEndpointRegistry().findEndpoints( serviceName, serviceVersion, envContext, routeOffer );
			System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
			assertTrue( endpoints.size() == 1 );
			System.out.println( endpoints.get( 0 ).toURLString() );
			System.out.println( serviceURI );

			String clientURI = "http://DME2RESOLVE" + serviceURI;

			//client.setAllowAllHttpReturnCodes(true);
			Request request =
					new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "GET" )
					.withContext( "/test" ).withReadTimeout( 30000 ).withLookupURL( clientURI ).build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

			String reply = (String) client.sendAndWait( payload );
			System.out.println( "Response returned from the service: " + reply );
			assertTrue( reply.contains(
					"EchoServlet:::testID:::/service=com.att.aft.dme2.test.TestDME2ClientWithDME2ProtocolSendClientRequest/version=1.0.0/envContext=LAB/routeOffer=TEST;Request=THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		} finally {
			System.clearProperty( "AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2ClientWithDME2Protocol_SendClientRequestWithDirectURI() throws Exception {
		DME2Manager mgr = null;
		String serviceName = "/service=com.att.aft.dme2.test.TestDME2ClientWithDME2Protocol_DirectURI/version=1.0.0/envContext=LAB/routeOffer=TEST";

		try {

			Properties props = new Properties();
			//props.setProperty( "AFT_DME2_PORT", "30906" );
			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );
			System.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );      

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999" );      
			props.setProperty( "AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999" );            

			String name = "com.att.aft.dme2.test.TestDME2ClientWithDME2Protocol_DirectURI";
			DME2Configuration config = new DME2Configuration( name, props );
			mgr = new DME2Manager( name, config );
			mgr.bindServiceListener( serviceName, new DME2SimpleServlet() );
			Thread.sleep( 3000 );
			String clientURI = String.format( "dme2://%s:%s%s", InetAddress.getLocalHost().getCanonicalHostName(), mgr.getServer().getServerProperties().getPort(), serviceName );

			Request request = new RequestBuilder(new URI( clientURI ) ).withReadTimeout( 20000 ).withLookupURL( clientURI ).build();
			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "THIS IS A TEST" );

			String reply = (String) client.sendAndWait( payload );
			System.out.println("Reply: " + reply);
			assertTrue( reply.contains( "THIS IS A TEST" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage());
		} finally {
			try {
				mgr.unbindServiceListener( serviceName );
			} catch ( Exception e ) {}
		}
	}

	@Ignore
	@Test
	public void testEndpointShuffle() throws Exception {

		List<DME2Manager> mgrInstances = new ArrayList<DME2Manager>();
		String DME2Managers[] =
			{ "TestEndpointShuffle1", "TestEndpointShuffle2", "TestEndpointShuffle3", "TestEndpointShuffle4",
			"TestEndpointShuffle5" };
		String ports[] = { "53500", "53501", "53502", "53503", "53504" };
		String serviceName = "com.att.aft.dme2.test.TestEndpointShuffle1";
		String version = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI =
				"/service=" + serviceName + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
		try {
			for ( int i = 0; i < DME2Managers.length; i++ ) {
				mgrInstances.add( publishService( serviceURI, DME2Managers[i], ports[i], serviceURI ) );
			}

			Properties props = RegistryGrmSetup.init();
			DME2Configuration config = new DME2Configuration( serviceName, props );
			DME2Manager mgr = new DME2Manager( serviceName, config );
			DME2Endpoint[] endpoints = mgr.findEndpoints( serviceName, version, envContext, routeOffer, false );

			System.out.println( "Endpoint Length = " + endpoints.length );
			assertTrue( endpoints.length == DME2Managers.length );

			List<String> traceResponses = new ArrayList<String>();
			for ( int i = 0; i < 40; i++ ) {

				EchoReplyHandler replyHandler = new EchoReplyHandler();
				Request request =
						new RequestBuilder(new URI( "http://DME2RESOLVE/" + serviceURI ) )
						.withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withResponseHandlers( replyHandler )
						.withReadTimeout( 30000 ).withLookupURL( "http://DME2RESOLVE/" + serviceURI ).build();
				DME2Client client = new DME2Client( mgr, request );
				DME2Payload payload = new DME2TextPayload( "test" );
				client.send( payload );
				String reply = replyHandler.getResponse( 10000 );
				System.out.println( "REPLY 2=" + reply );

				Map<String, String> rheader = replyHandler.getResponseHeaders();
				String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );

				traceResponses.add( traceStr );
				System.out.println("traceStr: " + traceStr );

				assertTrue( traceStr.contains( ":onResponseCompleteStatus=200" ) );
				Thread.sleep( 200 );
			}


			// Get uniq url's from trace
			Set<String> uniqValues = new HashSet<String>( traceResponses );
			System.out.println( "uniqValues: " + uniqValues );

			// Assuming all calls got 200 response,
			assertTrue( uniqValues.size() == DME2Managers.length );
			Iterator<String> it = uniqValues.iterator();
			while ( it.hasNext() ) {
				String key = it.next();
				int count = Math.max( 0, Collections.frequency( traceResponses, key ) );
				System.out.println( String.format( "endpoint=%s; count=%s", key, count ) );
				assertTrue( String.format( "Expected the count to be greater than 2 but instead it was %s", count ),
						count >= 2 );
			}


			// DO similar with DME2SEARCH
			serviceURI = "/service=" + serviceName + "/version=" + version + "/envContext=" + envContext + "/partner=ABC";
			traceResponses = new ArrayList<String>();
			for ( int i = 0; i < 40; i++ ) {
				EchoReplyHandler replyHandler = new EchoReplyHandler();
				Request request = new RequestBuilder(new URI( serviceURI ) )
						.withHeader( "AFT_DME2_REQ_TRACE_ON", "true" ).withResponseHandlers( replyHandler ).withReadTimeout( 30000 )
						.withLookupURL( "http://DME2SEARCH/" + serviceURI ).build();
				DME2Client client = new DME2Client( mgr, request );
				DME2Payload payload = new DME2TextPayload( "test" );
				client.send( payload );
				String reply = replyHandler.getResponse( 10000 );
				System.out.println( "REPLY 2=" + reply );

				Map<String, String> rheader = replyHandler.getResponseHeaders();
				String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );
				traceResponses.add( traceStr );
				System.out.println( traceStr );
				assertTrue( traceStr.contains( ":onResponseCompleteStatus=200" ) );
				Thread.sleep( 200 );
			}

			// Get uniq url's from trace
			uniqValues = new HashSet<String>( traceResponses );
			System.out.println( "uniqValues:" + uniqValues );
			// Assuming all calls got 200 response,
			assertTrue( uniqValues.size() == DME2Managers.length );
			it = uniqValues.iterator();
			while ( it.hasNext() ) {
				String key = it.next();
				int count = Math.max( 0, Collections.frequency( traceResponses, key ) );
				System.out.println( String.format( "endpoint=%s; count=%s", key, count ) );
				assertTrue( String.format( "Expected the count to be greater than 2 but instead it was %s", count ),
						count >= 2 );
			}
		} finally {
			for ( int i = 0; i < mgrInstances.size(); i++ ) {
				try {
					shutdownServer( mgrInstances.get( i ), serviceURI );
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		}

	}

	private DME2Manager publishService( String svcURI, String managerName, String port, String contextPath )
			throws Exception {
		Properties props = new Properties(); //TODO  RegistryGrmSetup.init();
		props.setProperty( "AFT_DME2_PORT", port );
		DME2Manager mgr = new DME2Manager();//(managerName,props);
		//String svcURI = "service="+serviceName+"/version="+version+"/envContext="+envContext+"/routeOffer="+routeOffer;
		//String svcURI1="service=com.att.aft.ServletHolderTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/";

		try {
			EchoResponseServlet echoServlet = new EchoResponseServlet( svcURI, "1" );
			String pattern[] = { contextPath };

			DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
			srvHolder.setContextPath( contextPath );

			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add( srvHolder );

			// Adding a Log filter to print incoming msg.
			ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
			dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
			dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );

			// Create service holder for each service registration
			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( svcURI );
			svcHolder.setManager( mgr );
			//svcHolder.setContext(contextPath);
			svcHolder.setServletHolders( shList );

			mgr.getServer().start();
			mgr.bindService( svcHolder );

			Thread.sleep( 10000 );

			return mgr;
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
		return mgr;
	}

	private void shutdownServer( DME2Manager mgr, String svcURI )
			throws Exception {
		{
			if ( mgr != null ) {
				try {
					mgr.unbindServiceListener( svcURI );
				} catch ( Exception e ) {
				}

				try {
					mgr.getServer().stop();
				} catch ( Exception e ) {
				}
			}
		}
	}

	@Test
	public void testDME2ClientNullUriDME2Search() throws Exception {
		DME2Manager mgr = null;
		try {			
			DME2Client sender = new DME2Client(mgr, null, 20000);
		} catch(Exception e){
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0605"));			
		} 
	}

	@Test
	public void testDME2ClientRequestWithCustomEndpointOrderHandler() throws Exception {
		Properties props = RegistryFsSetup.init();

		DME2Manager mgr1 = new DME2Manager("FSMgr", new DME2Configuration("FSMgr", props));

		String reply = null;

		String service = "/service=com.att.aft.dme2.server.test.TestDME2CustomEndpointOderHandler/version=1.0.0/envContext=LAB/routeOffer=FO1";

		//cleanPreviousEndpoints("com.att.aft.dme2.server.test.TestDME2CustomEndpointOderHandler", "1.0.0", "DEV");

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

			String resUri = "http://DME2RESOLVE/service=com.att.aft.dme2.server.test.TestDME2CustomEndpointOderHandler/version=1.0.0/envContext=LAB/routeOffer=FO1";
			Request request = new RequestBuilder(new URI(resUri))
					.withReadTimeout(10000).withReturnResponseAsBytes(false).withLookupURL(resUri)
					.withHeader("AFT_DME2_REQ_TRACE_ON", "true").withIteratorEndpointOrderHandler(new TestIteratorEndpointHandler()).build();

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			DME2Client client = new DME2Client(mgr1, request);
			client.setResponseHandlers(replyHandler);

			try {
				client.send(new DME2TextPayload(""));
				Thread.sleep(10000);				
				reply = replyHandler.getResponse(10000); // Should throw
				// Exception
			} catch (Exception e) {
				e.printStackTrace();
				String traceInfo = replyHandler.getResponseHeaders().get("AFT_DME2_REQ_TRACE_INFO");
				System.out.println(traceInfo);
				// The trace info should contain endpoint with port 8080,
				// 8081 and 8082 attempted.
				//				assertTrue(traceInfo.contains(":8082") && !traceInfo.contains(":8081") && !traceInfo.contains(":8080"));
				assertTrue(StringUtils.countMatches(traceInfo, "808") == 1);
			}

			try {
				mgr1.unbindServiceListener(service);
			} catch (Exception e) {
			}

		}  catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				mgr1.stop();
			} catch (Exception e) {
			}

		}
	}

	@Ignore
	@Test
	public void testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST() throws Exception {

		System.setProperty("GRM_EDGE_DIRECT_HOST", "hlth450.hydc.sbc.com");
		System.setProperty("GRM_EDGE_NODE_PORT", "9427");

		String serviceName = "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST";
		String serviceVersion = "1.0.0";
		String envContext = "TEST";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		DME2Manager mgr = null;

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();

			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");
			System.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");      
			props.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST" ) );

			//Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfo( "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST", "1.0.0", "TEST" );

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM( config, routeInfo, "LAB" );

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST/version=1.0.0/routeOffer=TEST/envContext=TEST/partner=DEFAULT";

			TestIteratorEndpointHandler handler = new TestIteratorEndpointHandler();
			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "Http" )
					.withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withIteratorEndpointOrderHandler(handler).build();

			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "Testing testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST" );


			try {
				String reply = (String) client.sendAndWait( payload );
				System.out.println( "REPLY=" + reply );
				assertTrue( reply != null );
			} catch ( Exception e ) {
				e.printStackTrace();
				fail(e.getMessage());
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			System.clearProperty("GRM_EDGE_DIRECT_HOST");
			System.clearProperty("GRM_EDGE_NODE_PORT");

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}
	
	@Ignore
	@Test
	public void testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Port() throws Exception {

		System.setProperty("GRM_EDGE_DIRECT_HOST", "hlth450.hydc.sbc.com");
		System.setProperty("GRM_EDGE_NODE_PORT", "1111");

		String serviceName = "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Port";
		String serviceVersion = "1.0.0";
		String envContext = "TEST";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		DME2Manager mgr = null;

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();

			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");
			System.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");      
			props.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Port" ) );

			//Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfo( "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Port", "1.0.0", "TEST" );

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM( config, routeInfo, "LAB" );

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Port/version=1.0.0/routeOffer=TEST/envContext=TEST/partner=DEFAULT";

			TestIteratorEndpointHandler handler = new TestIteratorEndpointHandler();
			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "Http" )
					.withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withIteratorEndpointOrderHandler(handler).build();

			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "Testing testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Port" );


			try {
				String reply = (String) client.sendAndWait( payload );
				System.out.println( "REPLY=" + reply );
				fail("This test case shouldn't even come here.");
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0911"));
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME" );
			System.clearProperty("GRM_EDGE_DIRECT_HOST");
			System.clearProperty("GRM_EDGE_NODE_PORT");

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}

	@Ignore
	@Test
	public void testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Context() throws Exception {

		System.setProperty("GRM_EDGE_DIRECT_HOST", "hlth450.hydc.sbc.com");
		System.setProperty("GRM_EDGE_NODE_PORT", "9427");
		System.setProperty("GRM_EDGE_CONTEXT_PATH", "/Hello/Itsme/Ivebeen/Wondering");

		String serviceName = "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Context";
		String serviceVersion = "1.0.0";
		String envContext = "TEST";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		DME2Manager mgr = null;

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();

			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");
			System.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");      
			props.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Context" ) );

			//Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfo( "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Context", "1.0.0", "TEST" );

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM( config, routeInfo, "LAB" );

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Context/version=1.0.0/routeOffer=TEST/envContext=TEST/partner=DEFAULT";

			TestIteratorEndpointHandler handler = new TestIteratorEndpointHandler();
			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "Http" )
					.withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withIteratorEndpointOrderHandler(handler).build();

			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "Testing testDME2ClientRequestWithDME2Search_GRM_EDGE_DIRECT_HOST_Wrong_Context" );


			try {
				String reply = (String) client.sendAndWait( payload );
				System.out.println( "REPLY=" + reply );
				fail("This test case shouldn't even come here.");
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0911"));
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME" );
			System.clearProperty("GRM_EDGE_DIRECT_HOST");
			System.clearProperty("GRM_EDGE_NODE_PORT");
			System.clearProperty("GRM_EDGE_CONTEXT_PATH");

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}
	
	@Ignore
	@Test
	public void testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS() throws Exception {

		System.setProperty("GRM_EDGE_CUSTOM_DNS", "grmcore-it-test.dev.att.com");
		System.setProperty("GRM_EDGE_NODE_PORT", "9427");

		String serviceName = "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS";
		String serviceVersion = "1.0.0";
		String envContext = "TEST";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		DME2Manager mgr = null;

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();

			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");
			System.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");      
			props.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS" ) );

			//Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfo( "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS", "1.0.0", "TEST" );

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM( config, routeInfo, "LAB" );

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS/version=1.0.0/routeOffer=TEST/envContext=TEST/partner=DEFAULT";

			TestIteratorEndpointHandler handler = new TestIteratorEndpointHandler();
			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "Http" )
					.withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withIteratorEndpointOrderHandler(handler).build();

			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "Testing testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS" );


			try {
				String reply = (String) client.sendAndWait( payload );
				System.out.println( "REPLY=" + reply );
				assertTrue( reply != null );
			} catch ( Exception e ) {
				e.printStackTrace();
				fail(e.getMessage());
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			System.clearProperty("GRM_EDGE_CUSTOM_DNS");
			System.clearProperty("GRM_EDGE_NODE_PORT");

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}
	
	@Ignore
	@Test
	public void testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Port() throws Exception {

		System.setProperty("GRM_EDGE_CUSTOM_DNS", "grmcore-it-test.dev.att.com");
		System.setProperty("GRM_EDGE_NODE_PORT", "1111");

		String serviceName = "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Port";
		String serviceVersion = "1.0.0";
		String envContext = "TEST";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		DME2Manager mgr = null;

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();

			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");
			System.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");      
			props.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Port" ) );

			//Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfo( "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Port", "1.0.0", "TEST" );

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM( config, routeInfo, "LAB" );

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Port/version=1.0.0/routeOffer=TEST/envContext=TEST/partner=DEFAULT";

			TestIteratorEndpointHandler handler = new TestIteratorEndpointHandler();
			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "Http" )
					.withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withIteratorEndpointOrderHandler(handler).build();

			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "Testing testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Port" );


			try {
				String reply = (String) client.sendAndWait( payload );
				System.out.println( "REPLY=" + reply );
				fail("This test case shouldn't even come here.");
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0911"));
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME" );
			System.clearProperty("GRM_EDGE_CUSTOM_DNS");
			System.clearProperty("GRM_EDGE_NODE_PORT");

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}
	
	@Ignore
	@Test
	public void testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Context() throws Exception {

		System.setProperty("GRM_EDGE_CUSTOM_DNS", "grmcore-it-test.dev.att.com");
		System.setProperty("GRM_EDGE_NODE_PORT", "9427");
		System.setProperty("GRM_EDGE_CONTEXT_PATH", "/Hello/Itsme/Ivebeen/Wondering");

		String serviceName = "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Context";
		String serviceVersion = "1.0.0";
		String envContext = "TEST";
		String routeOffer = "TEST";
		String serviceURI = DME2URIUtils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );

		DME2Manager mgr = null;

		try {
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			Properties props = new Properties();

			System.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");
			System.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");

			props.setProperty("AFT_DME2_PORT_RANGE", "49901-49999");      
			props.setProperty("AFT_DME2_SERVER_DEFAULT_PORT_RANGE", "49901-49999");
			//cleanPreviousEndpoints( serviceName, serviceVersion, envContext );

			DME2Configuration config = new DME2Configuration( serviceName, props );
			mgr = new DME2Manager( serviceName, config );
			mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Context" ) );

			//Create RouteInfo for this test case
			RouteInfo routeInfo = RouteInfoCreatorUtil.createRouteInfo( "com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Context", "1.0.0", "TEST" );

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoInGRM( config, routeInfo, "LAB" );

			String uriStr = "http://DME2SEARCH/service=com.att.aft.dme2.test.testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Context/version=1.0.0/routeOffer=TEST/envContext=TEST/partner=DEFAULT";

			TestIteratorEndpointHandler handler = new TestIteratorEndpointHandler();
			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "Http" )
					.withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withIteratorEndpointOrderHandler(handler).build();

			DME2Client client = new DME2Client( mgr, request );
			DME2Payload payload = new DME2TextPayload( "Testing testDME2ClientRequestWithDME2Search_GRM_EDGE_CUSTOM_DNS_Wrong_Context" );


			try {
				String reply = (String) client.sendAndWait( payload );
				System.out.println( "REPLY=" + reply );
				fail("This test case shouldn't even come here.");
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0911"));
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE" );
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME" );
			System.clearProperty("GRM_EDGE_CUSTOM_DNS");
			System.clearProperty("GRM_EDGE_NODE_PORT");
			System.clearProperty("GRM_EDGE_CONTEXT_PATH");

			try {
				mgr.unbindServiceListener( serviceURI );
			} catch ( Exception e ) {
			}
		}
	}
}