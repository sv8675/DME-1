package com.att.aft.dme2.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.registry.accessor.GRMEndPointsCache;
import com.att.aft.dme2.server.test.RegistryGrmSetup;
import com.att.aft.dme2.server.test.ServerControllerLauncher;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.server.test.TestGrm;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.util.DME2ParameterNames;


@Ignore
public class TestGrmWithDNSBootStrapping extends TestGrm {
	@BeforeClass
	public static void setUpBeforeClass() {
		TestGrm.setUpBeforeClass();
		setupGRMDNSDiscovery();
		System.setProperty( DME2ParameterNames.CACHE_REFRESH_START_DELAY_MS, "3000" ); // 3 seconds delay to refresh cache
		System.setProperty( DME2ParameterNames.CACHE_REFRESH_INTERVAL_MS, "1000" ); // 1 seconds intervals to refresh cache
	}

	@AfterClass
	public static void tearDownAfterClass() {
		System.setProperty( DME2ParameterNames.AFT_DME2_USE_AFT_DISCOVERY, "true" );
	}

	@Before
	public void setUp() {
		super.setUp();
		setupGRMDNSDiscovery();
		GRMAccessorFactory.close();
	}

	@After
	public void tearDown() {
		System.setProperty( DME2ParameterNames.CACHE_REFRESH_START_DELAY_MS, "1000" ); // 1 seconds delay to refresh cache
		System.setProperty( DME2ParameterNames.CACHE_REFRESH_INTERVAL_MS, "300000" ); // 5 minute intervals to refresh cache
		try {
			GRMAccessorFactory.close();
		} catch ( Exception e ) {
		}
		super.tearDown();
	}

	@Test
	public void testGrmPublish() throws DME2Exception, InterruptedException, UnknownHostException {
		super.testGrmPublish();
	}

	@Test
	public void testGrmPublish_HTTPS() throws DME2Exception, InterruptedException, UnknownHostException {
		System.setProperty( DME2ParameterNames.GRM_SERVER_PROTOCOL, "https" );
		System.setProperty( DME2ParameterNames.GRM_SERVER_PORT, "9227" );
		super.testGrmPublish();
	}

	@Test
	public void testCacheFileIsOverWritten() throws DME2Exception, InterruptedException, IOException {
		File fileep = new File("logs");
		if (fileep.exists())
			fileep.delete();
		System.setProperty( DME2ParameterNames.CACHE_REFRESH_START_DELAY_MS, "0" ); // 3 seconds delay to refresh cache
		System.setProperty( DME2ParameterNames.CACHE_REFRESH_INTERVAL_MS, "1000" ); // 1 seconds intervals to refresh cache
		//System.setProperty( "SCLD_PLATFORM", "SANDBOX-DEV" );
		//System.setProperty(DME2ParameterNames.AFT_DME2_USE_AFT_DISCOVERY, "false"); // AFT discovery does not seem to be using the local GRM cache
		//System.setProperty(DME2ParameterNames.GRM_DNS_BOOTSTRAP, "zldv0432.vci.att.com"); // specify the grm dns name

		long start = System.currentTimeMillis();
		super.testGrmPublish();
		long finish = System.currentTimeMillis();
		System.out.println( "total time in ms to test=" + ( finish - start ) );
		String cacheFileNAme =
				GRMEndPointsCache.getInstance( new DME2Configuration( "testCacheFileIsOverWritten" ) ).getCacheFileName();

		//   File file = new File( cacheFileNAme );
		//   long date1 = file.lastModified();
		//    long size1 = file.length();
		Path filePath = FileSystems.getDefault().getPath("logs", "dme2grmendpoints.txt");

		long size1 = Files.size(filePath);
		List<String> content1 = Files.readAllLines(filePath);
		System.out.println(content1);
		FileTime time1 = Files.getLastModifiedTime(filePath);
		Thread.sleep( 20000 );

		//    File file2 = new File( cacheFileNAme );
		//    long date2 = file2.lastModified();
		long size2 = Files.size(filePath);
		FileTime time2 = Files.getLastModifiedTime(filePath);
		List<String> content2 = Files.readAllLines(filePath);
		System.out.println(content2);

		assertNotEquals( time1.toMillis(), time2.toMillis());
		// make sure size is not increased
		//    assertEquals( size1 , size2 );

		/*   System.clearProperty( DME2ParameterNames.CACHE_REFRESH_START_DELAY_MS );
    System.clearProperty( DME2ParameterNames.CACHE_REFRESH_INTERVAL_MS );
    System.clearProperty( DME2ParameterNames.AFT_DME2_USE_AFT_DISCOVERY );
    System.clearProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP );
		 */
	}

	// @TODO enable the test when DNS is defined
	//@Test
	public void testNameGenerationFromDNS() throws DME2Exception, InterruptedException, UnknownHostException {
		System.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, TestConstants.GRM_DNS_SERVER );
		System.clearProperty( DME2ParameterNames.GRM_ENVIRONMENT );
		super.testGrmPublish();
	}

	// this test takes 252 seconds so you can disable it here!
	@Test
	@Override
	public void testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals()
			throws InterruptedException, MalformedURLException, DME2Exception, URISyntaxException {

		super.testRefreshEndpointsWithStaggeredEmptyCacheTTLIntervals();
	}

	@Test
	public void testGrmPublish_RetryAddServiceEndpoint()
			throws Exception {
		System.err.println( "--- START: RetryAddServiceEndpoint" );
		String service = "com.att.aft.dme2.TestGrmPublishRetryAddServiceEndpoint";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 48979;
		double latitude = 33.3739;
		double longitude = -86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName =
				"service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

		String dummyDnsCacheFile = "idontexist";
		try {
			File file = new File( System.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE ) );
			if ( file.delete() ) {
				System.out.println( "DNS Cache File " + file.getName() + " is Deleted" );
			} else {
				System.out.println( "DNS Cache File " + file.getName() + " is NOT Deleted" );
			}
			file = new File(dummyDnsCacheFile );
			if ( file.delete() ) {
				System.out.println( "DNS Cache File " + file.getName() + " is Deleted" );
			} else {
				System.out.println( "DNS Cache File " + file.getName() + " is NOT Deleted" );
			}
		} catch ( Exception e ) {
			System.out.println( "DNS Cache File is NOT Deleted - Deletion FAILED" );
		}

		try {
			//Properties props = new Properties();
			System.setProperty( "AFT_LATITUDE", latitude + "" );
			System.setProperty( "AFT_LONGITUDE", longitude + "" );
			System.setProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true" );
			System.setProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "15000" );
			System.setProperty( "DME2_SEP_CACHE_TTL_MS", "15000" );
			System.setProperty( "DME2.DEBUG", "true" );
			System.setProperty( "AFT_ENVIRONMENT", "SANDBOX_DEV" );
			//System.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, "aafdev.test.att.com" );
			//System.setProperty( DME2ParameterNames.AFT_DME2_USE_AFT_DISCOVERY, "false" );
			System.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, "aaftest.test.att.com" );
			Properties props = RegistryGrmSetup.init();//new Properties();
			//props.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, "aafdev.test.att.com" );
			props.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, "aaftest.test.att.com" );
			props.setProperty( DME2ParameterNames.AFT_DME2_USE_AFT_DISCOVERY, "false" );
			props.setProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE, "idontexist" );
			props.setProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true" );

			DME2Configuration config = new DME2Configuration( "RetryAddServiceEndpoint", props );
			assertEquals( props.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE ),
					config.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE ) );
			// Explicitly unset the grm accessor
			//     DME2Manager.getDefaultInstance();
			//DME2UnitTestUtil.setFinalStatic( GRMAccessorFactory.class.getDeclaredField( "grmAccessorHandler" ), null, null );
			//      GRMAccessorFactory.getInstance().resetGrmAccessorHandler();
			GRMAccessorFactory.getInstance().close();
			DME2UnitTestUtil.setFinalStatic( GRMEndPointsCache.class.getDeclaredField( "instance" ), null, null );
			//DME2UnitTestUtil.setFinalStatic( GRMEndPointsCache.class.getDeclaredField( "instance" ), null, null );
			DME2Manager manager = new DME2Manager( "RetryAddServiceEndpoint", config );
			DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
			boolean exceptionFound = false;
			try {
				svcRegistry.publish( serviceName, null, hostname, port, "http", null );
			} catch ( Exception e ) {
				// Publish expected to fail with wrong GRM URL's in use
				e.printStackTrace();
				exceptionFound = true;
			}
			assertTrue( exceptionFound );

			//      GRMAccessorFactory.getInstance().close();
			//      System.setProperty( DME2ParameterNames.CACHE_REFRESH_START_DELAY_MS, "30000" ); // 3 seconds delay to refresh cache
			//      System.setProperty( DME2ParameterNames.CACHE_REFRESH_INTERVAL_MS, "10000" ); // 1 seconds intervals to refresh cache
			System.clearProperty(DME2ParameterNames.GRM_DNS_BOOTSTRAP);
			System.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, TestConstants.GRM_DNS_SERVER );
			System.setProperty( "AFT_DME2_FORCE_GRM_LOOKUP", "true" );
			Properties props1 = RegistryGrmSetup.init();//new Properties();

			props1.setProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP, TestConstants.GRM_DNS_SERVER );
			props1.setProperty( DME2ParameterNames.AFT_DME2_USE_AFT_DISCOVERY, "false" );
			props1.setProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE, "idontexist" );

			GRMAccessorFactory.getInstance().close();

			//      try {
			//        File file = new File( System.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE ) );
			//        if ( file.delete() ) {
			//          System.out.println( "DNS Cache File " + file.getName() + " is Deleted" );
			//        } else {
			//          System.out.println( "DNS Cache File " + file.getName() + " is NOT Deleted" );
			//        }
			//      } catch ( Exception e ) {
			//        System.out.println( "DNS Cache File is NOT Deleted - Deletion FAILED" );
			//      }

			//the class shouldnt have cached the endpoints
			/*      DME2Configuration config2 = new DME2Configuration( "RetryAddServiceEndpoint2", props );
      manager = new DME2Manager( "RetryAddServiceEndpoint2", config2 );
       DME2Endpoint[] endpoints = svcRegistry.find( service, version, envContext, routeOffer );

      System.out.println( "\nendpoints: " + endpoints );

      if ( endpoints != null ) {
        assertEquals( String.format( "Expected to find 0 endpoints, instead found %s endpoints", endpoints.length ), 0,
            endpoints.length );
      }

      svcRegistry.publish( serviceName, null, hostname, port, "http", null );

			 */  
			DME2Configuration config1 = new DME2Configuration( "RetryAddServiceEndpoint1", props1 );

			DME2Manager manager1 = new DME2Manager( "RetryAddServiceEndpoint1", config1 );
			DME2EndpointRegistry svcRegistry1 = manager1.getEndpointRegistry();
			svcRegistry1.publish( serviceName, null, hostname, port, "http", null );
			DME2Endpoint[] endpoints = svcRegistry1.find( service, version, envContext, routeOffer );

			DME2Endpoint found = null;
			for ( DME2Endpoint ep : endpoints ) {
				if ( ep.getHost().equals( hostname ) && ep.getPort() == port
						&& ep.getLatitude() == latitude
						&& ep.getLongitude() == longitude ) {
					found = ep;
				}
			}
			System.err.println( "Found registered endpoint: " + found );
			assertNotNull( found );

			List<DME2Endpoint> endpointList =
					(List<DME2Endpoint>) DME2UnitTestUtil
					.getPrivate( DME2EndpointRegistryGRM.class.getDeclaredField( "localPublishedList" ),
							(DME2EndpointRegistryGRM) manager
							.getEndpointRegistry() );//((DME2EndpointRegistryGRM)manager.getEndpointRegistry()).getPublishedEndpoints();

			found = null;
			for ( DME2Endpoint ep : endpointList ) {
				if ( ep.getHost().equals( hostname ) && ep.getPort() == port
						&& ep.getLatitude() == latitude
						&& ep.getLongitude() == longitude ) {
					found = ep;
					System.out.println( "Found endpoints=" + found );
				}
			}
			System.err.println( "Found registered endpoint: " + found );
			assertNotNull( found );
			try {
				svcRegistry1.unpublish( serviceName, hostname, port );
			} catch ( Exception e ) {

			} finally {
				System.clearProperty( "AFT_LATITUDE" );
				System.clearProperty( "AFT_LONGITUDE" );
				System.clearProperty( "AFT_DME2_GRM_URLS" );
				System.clearProperty( "AFT_DME2_FORCE_GRM_LOOKUP" );
				System.clearProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" );
				System.clearProperty( "DME2_SEP_CACHE_TTL_MS" );
				System.clearProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP );
			}
		} finally {
			System.clearProperty( "AFT_LATITUDE" );
			System.clearProperty( "AFT_LONGITUDE" );
			System.clearProperty( "AFT_DME2_GRM_URLS" );
			System.clearProperty( "AFT_DME2_FORCE_GRM_LOOKUP" );
			System.clearProperty( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" );
			System.clearProperty( "DME2_SEP_CACHE_TTL_MS" );
			System.clearProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP );
			// Need to reinstance cache because we gave it a bad cache file name
			DME2UnitTestUtil.setFinalStatic( GRMEndPointsCache.class.getDeclaredField( "instance" ), null, null );
		}
	}

	@Test
	public void testGrmRefreshAttemptsCachedRouteInfo() throws DME2Exception,
	InterruptedException, UnknownHostException, URISyntaxException {

		DME2Configuration config = null;
		System.err.println( "--- START: testGrmRefreshAttemptsCachedRouteInfo" );

		System.setProperty( "DME2.DEBUG", "true" );

		try {
			Properties props = new Properties();
			props.put( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS", "10000" );
			props.put( "DME2_SEP_CACHE_TTL_MS", "200" );
			props.put( "DME2_SEP_CACHE_EMPTY_TTL_MS", "200" );
			props.put( "DME2_SEP_CACHE_ALL_STALE_TTL_MS", "200" );
			props.put( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "8000" );
			props.put( "DME2_ROUTEINFO_CACHE_TTL_MS", "200" );
			props.put( "DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS", "10000" );

			config = new DME2Configuration( "testGrmRefreshAttemptsCachedRouteInfo", props );

			String service = "com.att.aft.dme2.TestGrmRefreshAttemptsCachedRouteInfo";
			String hostname = InetAddress.getLocalHost().getCanonicalHostName();
			int port = 32675;
			double latitude = 33.3739;
			double longitude = 86.7983;

			String version = "1.0.0";
			String envContext = "DEV";
			String routeOffer = "BAU_SE";
			String routeOffer1 = "BAU_SE1";
			String routeOffer2 = "BAU_NE";

			String serviceName = "service=" + service + "/version=" + version
					+ "/envContext=" + envContext + "/routeOffer=" + routeOffer;
			String serviceName1 = "service=" + service + "/version=" + version
					+ "/envContext=" + envContext + "/routeOffer=" + routeOffer1;
			String serviceName2 = "service=" + service + "/version=" + version
					+ "/envContext=" + envContext + "/routeOffer=" + routeOffer2;

			DME2Manager manager = new DME2Manager( "testGrmRefreshAttemptsCachedRouteInfo",
					config );
			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName( service );
			//rtInfo.setServiceVersion("*");
			rtInfo.setEnvContext( envContext );
			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups( rtGrps );

			RouteGroup rg1 = new RouteGroup();
			rg1.setName( "RG1" );
			rg1.getPartner().add( "test1" );
			rg1.getPartner().add( "test2" );
			rg1.getPartner().add( "test3" );

			Route rt1 = new Route();
			rt1.setName( "rt1" );

			RouteOffer ro1 = new RouteOffer();
			ro1.setActive( true );
			ro1.setSequence( 1 );
			ro1.setName( "BAU_SE" );

			RouteOffer ro2 = new RouteOffer();
			ro2.setActive( true );
			ro2.setSequence( 1 );
			ro2.setName( "BAU_SE1" );

			RouteOffer ro3 = new RouteOffer();
			ro3.setActive( true );
			ro3.setSequence( 2 );
			ro3.setName( "BAU_NE" );

			rt1.getRouteOffer().add( ro1 );
			rt1.getRouteOffer().add( ro2 );
			rt1.getRouteOffer().add( ro3 );

			rg1.getRoute().add( rt1 );

			rtGrps.getRouteGroup();
			rtGrps.getRouteGroup().add( rg1 );

			// Ignoreing the above constructed routeInfo, since there is some
			// parsing issue while marshalling the
			// the routeInfo obj to xml

			// run the server in bham.
			String[] bham_1_bau_se_args = {
					"-serverHost",
					InetAddress.getLocalHost().getCanonicalHostName(),
					/*				"-serverPort",
					"4600",*/
					"-registryType",
					"GRM",
					"-servletClass",
					"EchoServlet",
					"-serviceName",
					serviceName,
					"-serviceCity", "BHAM", "-serverid", "bham_1_bau_se",
					"-platform", TestConstants.GRM_PLATFORM_TO_USE };
			bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
			bham_1_Launcher.launch();

			String[] bham_2_bau_se_args = {
					"-serverHost",
					InetAddress.getLocalHost().getCanonicalHostName(),
					/*				"-serverPort",
					"4601",*/
					"-registryType",
					"GRM",
					"-servletClass",
					"EchoServlet",
					"-serviceName",
					serviceName1,
					"-serviceCity", "BHAM", "-serverid", "bham_2_bau_se",
					"-platform", TestConstants.GRM_PLATFORM_TO_USE };
			bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
			bham_2_Launcher.launch();

			String[] bham_2_bau_ne_args = {
					"-serverHost",
					InetAddress.getLocalHost().getCanonicalHostName(),
					/*				"-serverPort",
					"4601",*/
					"-registryType",
					"GRM",
					"-servletClass",
					"EchoServlet",
					"-serviceName",
					serviceName2,
					"-serviceCity", "BHAM", "-serverid", "bham_2_bau_ne",
					"-platform", TestConstants.GRM_PLATFORM_TO_USE };
			bham_3_Launcher = new ServerControllerLauncher( bham_2_bau_ne_args );
			bham_3_Launcher.launch();

			try {
				Thread.sleep( 5000 );
			} catch ( Exception ex ) {
			}
			// Save the route info

			RegistryGrmSetup grmInit = new RegistryGrmSetup();
			grmInit.saveRouteInfoCacheAttemptsRefreshRouteInfo( config, rtInfo, "DEV" );
			try {
				Thread.sleep( 10000 );
			} catch ( Exception ex ) {
			}

			System.err.println( "Service published successfully." );

			Thread.sleep( 1000 );

			String uriStr = "http://DME2SEARCH/service=" + service + "/version=" + version + "/envContext=" + envContext +
					"/partner=test2";
			DME2Client sender = new DME2Client( manager, new URI( uriStr ), 30000 );
			sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
			sender.setPayload( "this is a test" );
			EchoReplyHandler replyHandler = new EchoReplyHandler();
			sender.setReplyHandler( replyHandler );
			sender.send();

			String reply = null;
			try {
				reply = replyHandler.getResponse( 60000 );
				Map<String, String> rheader = replyHandler.getResponseHeaders();
				String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );
				System.out.println( traceStr );
				assertTrue( traceStr.contains( "routeOffer=BAU_SE:onResponseCompleteStatus=200" ) ||
						traceStr.contains( "routeOffer=BAU_SE1:onResponseCompleteStatus=200" ) );
			} catch ( Exception e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				assertTrue( e1 == null );
			}
			System.err.println( reply );

			//shutdown one of instance to simulate empty endpoints scenario
			bham_2_Launcher.destroy();
			bham_1_Launcher.destroy();

			DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
			DME2Endpoint[] endpoints = svcRegistry.find( service, version,
					envContext, routeOffer + "~" + routeOffer1 );

			DME2Endpoint found = null;
			for ( DME2Endpoint ep : endpoints ) {
				if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
						&& ep.getLongitude() == longitude ) {
					found = ep;
					System.err.println( "Found endpoints after publish " + ep );
				}
			}
			// The registry does not hold endpoints now for shutdown endpoints of primary sequence
			assertNull( found );
			Thread.sleep( 11000 );
			svcRegistry.refresh();
			Thread.sleep( 11000 );

			endpoints = svcRegistry.find( service, version,
					envContext, routeOffer2 );

			found = null;
			for ( DME2Endpoint ep : endpoints ) {
				System.out.println( "ep.getHost()=" + ep.getHost() + ";" + ep.getHost().equals( hostname ) );
				System.out.println( "ep.getLatitude()=" + ep.getLatitude() + "" + ( ep.getLatitude() == latitude ) );
				System.out.println( "ep.getLongitude()=" + ep.getLongitude() + "" + ( ep.getLongitude() == longitude ) );
				if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude ) {
					found = ep;
					System.err.println( "Found endpoints after publish " + ep );
				}
			}
			// endpoints for routeOffer2 found.
			assertNotNull( found );

			uriStr = "http://DME2SEARCH/service=" + service + "/version=" + version + "/envContext=" + envContext +
					"/partner=test2";
			sender = new DME2Client( manager, new URI( uriStr ), 30000 );
			sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
			sender.setPayload( "this is a test" );
			replyHandler = new EchoReplyHandler();
			sender.setReplyHandler( replyHandler );
			sender.send();

			reply = null;
			try {
				reply = replyHandler.getResponse( 60000 );
				Map<String, String> rheader = replyHandler.getResponseHeaders();
				String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );
				System.out.println( traceStr );
				assertTrue( traceStr.contains( "routeOffer=BAU_NE:onResponseCompleteStatus=200" ) );
			} catch ( Exception e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				assertTrue( e1 == null );
			}
			System.err.println( reply );

			DME2Endpoint[] leasedEndpoints = svcRegistry.find( service, version,
					envContext, routeOffer + "~" + routeOffer1 );
			found = null;
			for ( DME2Endpoint ep : endpoints ) {
				if ( ep.getHost().equals( hostname ) && ep.getLatitude() == latitude
						&& ep.getLongitude() == longitude ) {
					found = ep;
					System.err.println( "Found endpoints after publish " + ep );
				}
			}
			// The registry should have refreshed endpoints to lose seq 1 endpoints that were shutdown.
			assertNull( found );

			// Check for number of refresh attempts now for endpoint cache
			/*      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      try {
        ObjectName name = new ObjectName(
            "com.att.aft.dme2:type=RegistryCache,name=DME2RouteInfoCache-testGrmRefreshAttemptsCachedRouteInfo" );
        MBeanInfo info = server.getMBeanInfo( name );
        Object[] params = new Object[2];
        params[1] = new Integer( Calendar.getInstance().HOUR_OF_DAY );
        params[0] = new String( "/service=" + service + "/version=" + version + "/envContext=" + envContext );
        System.out.println( "Param=" + params[0] );
        String[] signature = { "java.lang.String", "java.lang.Integer" };
        CompositeDataSupport cds = (CompositeDataSupport) server.invoke( name, "getStats", params, signature );
        System.out.println( "count=" + (Long) cds.get( "refreshCount" ) );
        //stats.getHourlyStats(Calendar.getInstance().HOUR_OF_DAY);
        assertTrue( (Long) cds.get( "refreshCount" ) >= 2 );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue( e == null );
      }
			 */
			// start back 1 & 2 launcher to add new endpoints
			bham_1_Launcher = new ServerControllerLauncher( bham_1_bau_se_args );
			bham_1_Launcher.launch();

			bham_2_Launcher = new ServerControllerLauncher( bham_2_bau_se_args );
			bham_2_Launcher.launch();
			// svcRegistry.

			Thread.sleep( 11000 );
			svcRegistry.refresh();
			Thread.sleep( 11000 );

			// Again primary sequence should be used
			uriStr = "http://DME2SEARCH/service=" + service + "/version=" + version + "/envContext=" + envContext +
					"/partner=test2";
			sender = new DME2Client( manager, new URI( uriStr ), 30000 );
			sender.addHeader( "AFT_DME2_REQ_TRACE_ON", "true" );
			sender.setPayload( "this is a test" );
			replyHandler = new EchoReplyHandler();
			sender.setReplyHandler( replyHandler );
			sender.send();

			reply = null;
			try {
				reply = replyHandler.getResponse( 60000 );
				Map<String, String> rheader = replyHandler.getResponseHeaders();
				String traceStr = rheader.get( "AFT_DME2_REQ_TRACE_INFO" );
				System.out.println( traceStr );
				// request got restored back to primary sequence as refresh should have got endpoints back.
				assertTrue( traceStr.contains( "routeOffer=BAU_SE:onResponseCompleteStatus=200" ) ||
						traceStr.contains( "routeOffer=BAU_SE1:onResponseCompleteStatus=200" ) );
			} catch ( Exception e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				assertTrue( e1 == null );
			}
			System.err.println( reply );

			// Check for number of refresh attempts now for endpoint cache after second refresh attempt

			//Check for number of refresh attempts now for routeInfo cache
			/*      try {
        ObjectName name = new ObjectName(
            "com.att.aft.dme2:type=RegistryCache,name=DME2RouteInfoCache-testGrmRefreshAttemptsCachedRouteInfo" );
        MBeanInfo info = server.getMBeanInfo( name );
        Object[] params = new Object[2];
        params[1] = new Integer( Calendar.getInstance().HOUR_OF_DAY );
        params[0] = new String( "/service=" + service + "/version=" + version + "/envContext=" + envContext );
        System.out.println( "Param=" + params[0] );
        String[] signature = { "java.lang.String", "java.lang.Integer" };
        CompositeDataSupport cds = (CompositeDataSupport) server.invoke( name, "getStats", params, signature );
        System.out.println( (Long) cds.get( "refreshCount" ) );
        assertTrue( (Long) cds.get( "refreshCount" ) >= 4 );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue( e == null );
      }
			 */
			for ( DME2Endpoint leasedEp : leasedEndpoints ) {
				System.err.println( "Found endpoints after lease " + leasedEp );
				int leasedEpPort = leasedEp.getPort();
				for ( DME2Endpoint ep : endpoints ) {
					int epPort = ep.getPort();
					if ( leasedEpPort == epPort ) {
						long pubEpLease = ep.getLease();
						long leasedEpLease = leasedEp.getLease();

						if ( leasedEpLease <= pubEpLease ) {
							// Endpoints have not refreshed, throw error
							fail( "Leased endpoint for "
									+ leasedEp.getHost()
									+ ":"
									+ leasedEp.getPort()
									+ leasedEp.getPath()
									+ " has expiration time="
									+ leasedEp.getLease()
									+ "which should have been greater than published Ep "
									+ ep.getHost() + ":" + ep.getPort()
									+ ep.getPath() + " expiration time="
									+ ep.getLease() );
						} else {
							System.err.println( "Leased endpoint for "
									+ leasedEp.getHost() + ":" + leasedEp.getPort()
									+ leasedEp.getPath() + " has expiration time="
									+ leasedEp.getLease()
									+ " is greater than published Ep "
									+ ep.getHost() + ":" + ep.getPort()
									+ ep.getPath() + " expiration time="
									+ ep.getLease() );
						}
					}
				}
			}
		} catch ( Exception e ) {

			e.printStackTrace();
		} finally {
			try {
				bham_1_Launcher.destroy();
			} catch ( Exception e ) {

			}
			try {
				bham_2_Launcher.destroy();
			} catch ( Exception e ) {

			}
			try {
				bham_3_Launcher.destroy();
			} catch ( Exception e ) {

			}
			System.err.println( "Service unpublished successfully." );
		}
	}
}
