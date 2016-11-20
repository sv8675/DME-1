/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContextListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder.RequestDispatcherType;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.TestInitParamFilter;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URI;
import com.att.aft.dme2.util.DME2URI.DME2UriType;
import com.att.aft.dme2.util.DME2Utils;


public class TestDME2ServiceHolder extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestDME2ServiceHolder.class );

  @Before
  public void setUp() {
    super.setUp();
    System.setProperty( "AFT_DME2_PUBLISH_METRICS", "false" );
    System.setProperty( "org.eclipse.jetty.util.UrlEncoding.charset", "UTF-8" );
    System.setProperty( "metrics.debug", "true" );
  }

  @After
  public void tearDown() {
    super.tearDown();
    System.clearProperty( "AFT_DME2_PUBLISH_METRICS" );
    System.clearProperty( "org.eclipse.jetty.util.UrlEncoding.charset" );
    System.clearProperty( "metrics.debug" );
  }


    /*@Test
    public void testDME2MetricsFilter() throws Exception
	{
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2ServiceHolder_DME2MetricsFilter");

		DME2Manager mgr = DME2Manager.getDefaultInstance();
		String svcURI = "/service=com.att.aft.FilterTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

		try
		{
			String containerHost = null;

			try
			{
				containerHost = InetAddress.getLocalHost().getHostName();
			}
			catch (Exception e)	{}

			System.setProperty("lrmHost", containerHost);

			ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
			dlist.add(DME2FilterHolder.RequestDispatcherType.REQUEST);
			dlist.add(DME2FilterHolder.RequestDispatcherType.FORWARD);

			TestDME2LogFilter logFilter = new TestDME2LogFilter(); //Creating Log Filter
			DME2MetricsFilter metricsFilter = new DME2MetricsFilter(svcURI); //Creating Metrics Filter

			ArrayList<DME2FilterHolder.RequestDispatcherType> dlist1 = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
			dlist1.add(DME2FilterHolder.RequestDispatcherType.REQUEST);
			dlist1.add(DME2FilterHolder.RequestDispatcherType.FORWARD);
			dlist1.add(DME2FilterHolder.RequestDispatcherType.ASYNC);

			DME2FilterHolder filterHolder = new DME2FilterHolder(logFilter, "/FilterTest", EnumSet.copyOf(dlist));
			DME2FilterHolder filterHolder1 = new DME2FilterHolder(metricsFilter, svcURI, EnumSet.copyOf(dlist1));

			List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
			flist.add(filterHolder);
			flist.add(filterHolder1);

			DME2ServiceHolder svcHolder1 = new DME2ServiceHolder();
			svcHolder1.setServiceURI(svcURI);
			svcHolder1.setManager(mgr);
			svcHolder1.setServlet(new EchoResponseServlet(svcURI, "1"));
			svcHolder1.disableMetricsFilter();
			svcHolder1.setFilters(flist);

			mgr.getServer().addService(svcHolder1);
			mgr.getServer().start();

			MetricsCollector collector = MetricsCollectorFactory.getMetricsCollector("NA", "NA", "NA", "NA", TestConstants.GRM_PLATFORM_TO_USE, containerHost, "NA", "NA");
			collector.setDisablePublish(true);

			Thread.sleep(5000);

			//Invoke the above registered FilterTest service by resolving
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.FilterTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
			
			Request request = new RequestBuilder(mgr.getClient(), new HttpConversation(), new URI(clientURI)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();

			DME2Client client = new DME2Client(mgr, request);
			//client.setPayload("<data>testmessagewithtrademark</data>");
			
			String reply = (String) client.sendAndWait(new TextPayload("<data>testmessagewithtrademark</data>"));
			collector.publish();

			System.out.println("Reply from EchoServlet1 =" + reply);
			Thread.sleep(5000);

			// com.att.test.MetricsTestCollectorClient,1.0.0,TESTRO,DEV,SANDBOX-LAB,ACNFL084Q1,1234,PTE
			// com.att.aft.FilterTest,1.0.0,SERVER,2373,HTTP,1,cb1c722f-8746-658d-3687-b89676ee8e14,203,203
			// Fetch metrics data and confirm that MetricsFilter had submitted data
			long eventTime = System.currentTimeMillis();

			Timeslot slot = collector.getTimeslotForTime(eventTime);
			Collection<SvcProtocolMetrics> metricsc = slot.getAllMetrics();
			Iterator<SvcProtocolMetrics> it = metricsc.iterator();

			String service = null;
			String role = null;
			int i = 0;
			
			while (it.hasNext())
			{
				SvcProtocolMetrics m = it.next();
				
				System.out.println(String.format("service: %s, timeslot: %s, metricsTimeslot: %s, name: %s, role: %s, loopCount: %s", m.getService(),m.getTimeslot(), m.getMetricsTimeslot(), m.getService().getName(), m.getService().getRole(), ++i));
				service = m.getService().getName();
				role = m.getService().getRole();

				//Only save it if its server - we are ignoring the client role data for this test
				if ( !role.equals("SERVER")){
					role = null;
				}else {
					break;
				}
				
			}
			assertEquals("com.att.aft.FilterTest1",service);
			assertEquals("SERVER", role);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try{mgr.unbindServiceListener(svcURI);}
			catch (Exception e)	{}
			
			try	{mgr.getServer().stop();}
			catch (Exception e)	{}
			
			System.clearProperty("lrmHost");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}
	} */


  @Test
  @Ignore
  public void testGZIPCompression() throws Exception {
  
//    DME2Manager mgr = new DME2Manager();
	System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");

	Properties props = RegistryFsSetup.init();
	props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
	//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
	//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");
	
	DME2Configuration config = new DME2Configuration("testPayloadCompression", props);
	DME2Manager mgr = new DME2Manager("testPayloadCompression", config);

    String svcURI = "service=com.att.aft.TestGZIPCompression/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2ServiceHolder_GZIPCompression" );
	System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");

    try {

      DME2TestContextListener ctxLsnr = new DME2TestContextListener();
      ArrayList<ServletContextListener> clist = new ArrayList<ServletContextListener>();
      clist.add( ctxLsnr );

      //Creating GZip filter
//      org.eclipse.jetty.servlets.GzipFilter filter = new org.eclipse.jetty.servlets.GzipFilter();


//GZipFilter is deprecated in Jetty 9 and we are using GZipHandler instead using the flag - DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH
      //Create service holder for service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setServlet( new EchoResponseServlet( svcURI, "1" ) );
      svcHolder.setContext( "/TestGZIPCompression" );
      svcHolder.disableMetricsFilter();
      svcHolder.setContextListeners( clist );
//      svcHolder.setFilters( flist );

      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 3000 );

      //Call the service that was just registered
      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.TestGZIPCompression/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      Request request =
          new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withHeader("Accept-Encoding", "gzip").withSubContext("/TestGZIPCompression").withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      DME2Client client = new DME2Client( mgr, request );
      //client.setPayload("<data>testmessagewithtrademark</data>");
      //client.setSubContext("/TestGZIPCompression");
//      request.header( "Accept-Encoding", "gzip" );

      String reply = (String) client.sendAndWait( new DME2TextPayload( "<data>testmessagewithtrademark</data>" ) );

      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "testmessagewithtrademark" ) );
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
      try {
        mgr.unbindServiceListener( svcURI );
      } catch ( Exception e ) {
      }

      try {
        mgr.getServer().stop();
      } catch ( Exception e ) {
      }

      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
      System.clearProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH");
    }
  }


  @Test
  @Ignore
  public void testGZIPCompressionWithUTF8() throws Exception {
//    DME2Manager mgr = new DME2Manager();

//    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2ServiceHolder_GZIPCompressionWithLargeData" );
	System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");

	Properties props = RegistryFsSetup.init();
	props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
	//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
	//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");
	
	DME2Configuration config = new DME2Configuration("testPayloadCompression", props);
	DME2Manager mgr = new DME2Manager("testPayloadCompression", config);
  
    String svcURI = "service=com.att.aft.TestGZIPCompressionWithUTF8/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2ServiceHolder_GZIPCompressionWithUTF8" );
	System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");

    String utf8String = null;

    try {
      String tempData = null;
      File f = new File( "src/test/etc/utf8data.txt" );
      StringBuffer strBuf = new StringBuffer();
      if ( f.exists() ) {
        BufferedReader br = new BufferedReader( new FileReader( f ) );
        while ( ( tempData = br.readLine() ) != null ) {
          strBuf.append( tempData );
        }
        utf8String = strBuf.toString();
      }
    } catch ( Exception e ) {
    	e.printStackTrace();
    }

    System.out.println("Request :" + utf8String);
    try {
      DME2TestContextListener ctxLsnr = new DME2TestContextListener();
      ArrayList<ServletContextListener> clist = new ArrayList<ServletContextListener>();
      clist.add( ctxLsnr );

      //Adding GZip Filter
/**      org.eclipse.jetty.servlets.GzipFilter filter = new org.eclipse.jetty.servlets.GzipFilter();
      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );
      dlist.add( DME2FilterHolder.RequestDispatcherType.ASYNC );

      DME2FilterHolder filterHolder =
          new DME2FilterHolder( filter, "/TestGZIPCompressionWithUTF8", EnumSet.copyOf( dlist ) );
      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );
*/
      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setServlet( new EchoServlet( svcURI, "1" ) );
      svcHolder.setContext( "/TestGZIPCompressionWithUTF8" );
      svcHolder.disableMetricsFilter();
      svcHolder.setContextListeners( clist );
//      svcHolder.setFilters( flist );

      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 3000 );

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.TestGZIPCompressionWithUTF8/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      HttpRequest request =
    		  (HttpRequest)new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 100000 ).withCharset("UTF-8").withSubContext("/TestGZIPCompressionWithUTF8").withHeader("Content-Type", "text/plain; charset=utf-8").withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();

      Map<String, String> headers = new HashMap<String, String>();
      headers.put( "testReturnCharSet", "UTF-8" );
      headers.put( "testEchoBack", "true" );
      headers.put( "Accept-Encoding", "gzip" );
      request.setHeaders(headers);

      DME2Client client = new DME2Client( mgr, request );
      //client.setPayload(utf8String);
      //client.setSubContext("/TestGZIPCompressionWithUTF8");
      //client.setHeaders(headers);

      String reply = (String) client.sendAndWait( new DME2TextPayload( utf8String, "UTF-8" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertEquals( utf8String, reply );
    } finally {
      try {
        mgr.unbindServiceListener( svcURI );
      } catch ( Exception e ) {
      }

      try {
        mgr.getServer().stop();
      } catch ( Exception e ) {
      }

      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
      System.clearProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH");      
    }
  }

  
  @Test
  public void testGZIPCompressionWithLargeDataNew() throws Exception
	{
	  	DME2Configuration config = new DME2Configuration(DME2Configuration.DME2_DEFAULT_CONFIG_MANAGER_NAME);
		DME2Manager mgr = new DME2Manager(DME2Configuration.DME2_DEFAULT_CONFIG_MANAGER_NAME, config);
		
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2ServiceHolder_GZIPCompressionWithLargeData");
		String svcURI = "service=com.att.aft.TestGZIPCompressionWithLargeData/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
		String utf8String = null;
		
		try
		{
			char[] chars = new char[1000000];
			Arrays.fill(chars, 'a');
			utf8String = new String(chars);
		}
		catch (Exception e)	{}
		
		try
		{
			DME2TestContextListener ctxLsnr = new DME2TestContextListener();
			ArrayList<ServletContextListener> clist = new ArrayList<ServletContextListener>();
			clist.add(ctxLsnr);

			// Adding a Log filter to print incoming msg.
			org.eclipse.jetty.servlets.GzipFilter filter = new org.eclipse.jetty.servlets.GzipFilter();
			ArrayList<RequestDispatcherType> dlist = new ArrayList<RequestDispatcherType>();
			dlist.add(DME2FilterHolder.RequestDispatcherType.REQUEST);
			dlist.add(DME2FilterHolder.RequestDispatcherType.FORWARD);
			dlist.add(DME2FilterHolder.RequestDispatcherType.ASYNC);

			DME2FilterHolder filterHolder = new DME2FilterHolder(filter, "/TestGZIPCompressionWithLargeData", EnumSet.copyOf(dlist));
			List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
			flist.add(filterHolder);

			// Create service holder for each service registration
			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(svcURI);
			svcHolder.setManager(mgr);
			svcHolder.setServlet(new EchoServlet(svcURI, "1"));
			svcHolder.setContext("/TestGZIPCompressionWithLargeData");
			svcHolder.disableMetricsFilter();
			svcHolder.setContextListeners(clist);
			svcHolder.setFilters(flist);
			
			mgr.getServer().start();
			mgr.bindService(svcHolder);

			Thread.sleep(3000);

			String clientURI = "http://DME2RESOLVE/service=com.att.aft.TestGZIPCompressionWithLargeData/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
			
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("testEchoBack", "true");
			headers.put("Accept-Encoding", "gzip");
			
			 Request request =
			          new RequestBuilder(new URI( clientURI ) ).withHttpMethod( "POST" )
			              .withReadTimeout( 400000 ).withSubContext("/TestGZIPCompressionWithLargeData").withHeaders(headers).withReturnResponseAsBytes( false ).withSubContext( "/TestGZIPCompressionWithLargeData" ).withLookupURL( clientURI ).build();

			 

			DME2Client client = new DME2Client(mgr, request);
			String reply = (String) client.sendAndWait( new DME2TextPayload( utf8String ) );
			System.out.println("Reply from EchoServlet " + reply);
			System.out.println("==============================lenght---=========================== " + reply.length());
			assertEquals(utf8String, reply);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try	{mgr.unbindServiceListener(svcURI);	}
			catch (Exception e)	{}
			
			try	{mgr.getServer().stop();}
			catch (Exception e)	{}
			
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}
	}

  @Test
  @Ignore
  public void testGZIPCompressionWithLargeData() throws Exception {
//    DME2Manager mgr = new DME2Manager();
	    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2ServiceHolder_GZIPCompressionWithLargeData" );
		System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");

	Properties props = RegistryFsSetup.init();
	props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
	//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
	//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");

	DME2Configuration config = new DME2Configuration("testPayloadCompression", props);
	DME2Manager mgr = new DME2Manager("testPayloadCompression", config);

    
    String svcURI =
        "service=com.att.aft.TestGZIPCompressionWithLargeData/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
    String utf8String = null;


      char[] chars = new char[10000000];
      Arrays.fill( chars, 'a' );
      utf8String = new String( chars );

    try {
      DME2TestContextListener ctxLsnr = new DME2TestContextListener();
      ArrayList<ServletContextListener> clist = new ArrayList<ServletContextListener>();
      clist.add( ctxLsnr );

      // Adding a Log filter to print incoming msg.
/**      org.eclipse.jetty.servlets.GzipFilter filter = new org.eclipse.jetty.servlets.GzipFilter();
      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );
      dlist.add( DME2FilterHolder.RequestDispatcherType.ASYNC );

      DME2FilterHolder filterHolder =
          new DME2FilterHolder( filter, "/TestGZIPCompressionWithLargeData", EnumSet.copyOf( dlist ) );
      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );
*/
      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setServlet( new EchoServlet( svcURI, "1" ) );
      svcHolder.setContext( "/TestGZIPCompressionWithLargeData" );
      svcHolder.disableMetricsFilter();
      svcHolder.setContextListeners( clist );
//      svcHolder.setFilters( flist );

      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 3000 );

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.TestGZIPCompressionWithLargeData/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      HttpRequest request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 100000 ).withSubContext("/TestGZIPCompressionWithLargeData").withHeader("Accept-Encoding", "gzip").withReturnResponseAsBytes( false ).withSubContext( "/TestGZIPCompressionWithLargeData" ).withLookupURL( clientURI ).build();

      Map<String, String> headers = new HashMap<String, String>();
      headers.put( "testEchoBack", "true" );
      headers.put( "Accept-Encoding", "gzip" );
      request.setHeaders(headers);

      DME2Client client = new DME2Client( mgr, request );
      //client.setPayload(utf8String);
      //client.setSubContext("/TestGZIPCompressionWithLargeData");
      //client.setHeaders(headers);

      String reply = (String) client.sendAndWait( new DME2TextPayload( utf8String ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.equals(utf8String) );
    } finally {
      try {
        mgr.unbindServiceListener( svcURI );
      } catch ( Exception e ) {
      }

      try {
        mgr.getServer().stop();
      } catch ( Exception e ) {
      }

      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
      System.clearProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH");
    }
  }


  @Test
  public void testDME2Filter() throws Exception {
    DME2Manager mgr = new DME2Manager();
    String svcURI = "service=com.att.aft.FilterTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

    try {
      DME2TestContextListener ctxLsnr = new DME2TestContextListener();
      ArrayList<ServletContextListener> clist = new ArrayList<ServletContextListener>();
      clist.add( ctxLsnr );

      // Adding a Log filter to print incoming msg.
      TestDME2LogFilter filter = new TestDME2LogFilter();
      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );
      dlist.add( DME2FilterHolder.RequestDispatcherType.ASYNC );

      DME2FilterHolder filterHolder = new DME2FilterHolder( filter, "/FilterTest", EnumSet.copyOf( dlist ) );
      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );

      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setServlet( new EchoResponseServlet( svcURI, "1" ) );
      svcHolder.setContext( "/FilterTest" );
      svcHolder.disableMetricsFilter();
      svcHolder.setContextListeners( clist );
      svcHolder.setFilters( flist );

      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 3000 );

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.FilterTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).withSubContext(
              "/FilterTest" ).build();

      DME2Client client = new DME2Client( mgr, request );
      //client.setPayload("<data>testmessagewithtrademark</data>");
      //client.setSubContext("/FilterTest");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "<data>testmessagewithtrademark</data>" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "TestDME2LogFilter" ) );
    } finally {
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

  @Test
  public void testDME2ServletHolder() throws Exception {
    DME2Manager mgr = new DME2Manager();
    String svcURI = "service=com.att.aft.ServletHolderTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
    String svcURI1 = "service=com.att.aft.ServletHolderTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/";
    try {
      EchoResponseServlet echoServlet = new EchoResponseServlet( svcURI, "1" );
      String pattern[] = { "/test", "/servletholder" };

      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/servletholdertest" );

      EchoResponseServlet echoServlet1 = new EchoResponseServlet( svcURI1, "1" );
      String pattern1[] = { "/test1", "/servletholder1" };

      DME2ServletHolder srvHolder1 = new DME2ServletHolder( echoServlet1, pattern1 );
      srvHolder1.setContextPath( "/servletholdertest1" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );
      shList.add( srvHolder1 );

      // Adding a Log filter to print incoming msg.
      TestDME2LogFilter filter = new TestDME2LogFilter();
      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );

      DME2FilterHolder filterHolder = new DME2FilterHolder( filter, "/FilterTest", EnumSet.copyOf( dlist ) );
      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );

      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/ServletHolderTest" );
      svcHolder.setFilters( flist );
      svcHolder.setServletHolders( shList );

      mgr.getServer().start();
      Thread.sleep( 10000 );
      mgr.bindService( svcHolder );

      Thread.sleep( 10000 );
      //Thread.sleep(4000);

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.ServletHolderTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI )
              .withSubContext( "/test" ).build();

      EchoReplyHandler replyHandler = new EchoReplyHandler();

      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setSubContext("/test");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "ServletHolderTest/" ) );

      Thread.sleep( 1000 );
      EchoReplyHandler replyHandler1 = new EchoReplyHandler();

      Request request1 =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI )
              .withSubContext( "/servletholdertest" ).build();

      DME2Client client1 = new DME2Client( mgr, request1 );
      client1.setResponseHandlers( replyHandler1 );
      //client1.setSubContext("/servletholdertest");
      //client1.setPayload("test");

      String reply1 = (String) client1.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply1 );
      assertTrue( reply1.contains( "ServletHolderTest/" ) );
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
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


  @Test
  public void testDME2ServletInitParam() throws Exception {
    DME2Manager mgr = new DME2Manager();
    String svcURI = "/service=com.att.aft.ServletInitParamTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

    try {
      EchoServlet echoServlet = new EchoServlet( svcURI, "1" );
      String pattern[] = { "/test" };

      Properties params = new Properties();
      params.setProperty( "testParam", "TEST_INIT_PARAM" );

      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/ServletInitParamTest" );
      srvHolder.setInitParams( params );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );

      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/ServletInitParamTest" );
      svcHolder.setServletHolders( shList );

      mgr.addService( svcHolder );
      mgr.getServer().start();

      Thread.sleep( 4000 );

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.ServletInitParamTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      HttpRequest request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).withHeader(
              "testReturnServletParam", "test" ).withSubContext( "/ServletInitParamTest" ).build();

      EchoReplyHandler replyHandler = new EchoReplyHandler();

      Map<String, String> headers = new HashMap<String, String>();
      headers.put( "testReturnServletParam", "test" );
      request.setHeaders(headers);
      
      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setHeaders(headers);
      //client.setSubContext("/ServletInitParamTest");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "servletParam=TEST_INIT_PARAM" ) );
    } finally {
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


  @Test
  public void testDME2ServletContextParam() throws Exception {
    DME2Manager mgr = new DME2Manager();
    String svcURI = "service=com.att.aft.ServletContextParamTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

    try {

      EchoServlet echoServlet = new EchoServlet( svcURI, "1" );
      String pattern[] = { "/test" };

      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/ServletContextParamTest" );

      Properties params = new Properties();
      params.setProperty( "testContextParam", "TEST_CONTEXT_PARAM" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );

      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/ServletContextParamTest" );
      svcHolder.setServletHolders( shList );
      svcHolder.setContextParams( params );

      mgr.addService( svcHolder );
      mgr.getServer().start();

      Thread.sleep( 4000 );

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.ServletContextParamTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).withHeader(
              "testReturnServletContextParam", "test" ).withSubContext( "/ServletContextParamTest" ).build();

      EchoReplyHandler replyHandler = new EchoReplyHandler();

      Map<String, String> headers = new HashMap<String, String>();
      headers.put( "testReturnServletContextParam", "test" );

      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setHeaders(headers);
      //client.setSubContext("/ServletContextParamTest");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "contextParam=TEST_CONTEXT_PARAM" ) );
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
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


  @Ignore
  @Test
  public void testDME2ServletContextParamUsingNaturalURI() throws Exception {
    DME2Manager mgr = new DME2Manager();
    String svcURI = "http://ServletContextParamTest1.aft.att.com/?version=1.0.0&envContext=LAB&routeOffer=DEFAULT";

    try {
      // Create service holder for each service registration
      EchoServlet echoServlet = new EchoServlet(
          "service=com.att.aft.ServletContextParamTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/", "1" );

      String pattern[] = { "/test" };
      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/ServletContextParamTest" );

      Properties params = new Properties();
      params.setProperty( "testContextParam", "TEST_CONTEXT_PARAM" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );

      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/ServletContextParamTest" );
      svcHolder.setContextParams( params );
      svcHolder.setServletHolders( shList );

      mgr.addService( svcHolder );
      mgr.getServer().start();
      //mgr.bindService(svcHolder);

      Thread.sleep( 4000 );

      //Invoke the above registered FilterTest service by resolving endpoints via SOA registry
      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.ServletContextParamTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      HttpRequest request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).withHeader(
              "testReturnServletContextParam", "test" ).withSubContext( "/ServletContextParamTest" ).build();


      EchoReplyHandler replyHandler = new EchoReplyHandler();

      Map<String, String> headers = new HashMap<String, String>();
      headers.put( "testReturnServletContextParam", "test" );
      request.setHeaders(headers);


      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
     // request.header( "testReturnServletContextParam", "test" );
      //client.setHeaders(headers);
      //client.setSubContext("/ServletContextParamTest");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );

      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "contextParam=TEST_CONTEXT_PARAM" ) );

      String clientURI_2 =
          "http://ServletContextParamTest1.aft.att.com/ServletContextParamTest?version=1.0.0&envContext=LAB&routeOffer=DEFAULT";
      request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_2 ).withHeader( "testReturnServletContextParam", "test" ).withSubContext( "/ServletContextParamTest" ).build();

      replyHandler = new EchoReplyHandler();

      headers = new HashMap<String, String>();
      headers.put( "testReturnServletContextParam", "test" );

      client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setHeaders(headers);
      //client.setSubContext("/ServletContextParamTest");
      //client.setPayload("test");

      reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );

      assertTrue( reply.contains( "contextParam=TEST_CONTEXT_PARAM" ) );
    } finally {
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


  public void notestRestfulContext() throws Exception {
    String uriStr =
        "http://TestService.restful.att.com/rest/nimbus/cust/service/(id)/user/(userid)?envContext=LAB&version=1&routeOffer=TEST";
    //String uriEnStr = URLEncoder.encode(uriStr, "UTF-8");
    DME2Manager mgr = new DME2Manager();
    //URI uri = new URI(uriStr);
    //String host = uri.getHost();
    DME2URI dme2Uri = new DME2URI( new URI( uriStr ) );
    dme2Uri.assertValid();
    assertTrue( dme2Uri.getType() == DME2UriType.STANDARD );

    //String endpointPath = "/rest/nimbus/cust/service/{id}/user/{userid},/rest/nimbus/acct/service/{id}/user/{userid},/rest/nimbus/acct/.*";

    //Check url using variable pattern template , {} , ()
    String serviceURI =
        "http://TestService12.abc2.att.com/rest/nimbus/cust/checkAcct/(acctid)/abc/{id}?envContext=DEV&version=2.0.0&routeOffer=TEST";
    mgr.getEndpointRegistry().publish( serviceURI, null, "135.70.138.100", 7865, "http", null );
    Thread.sleep( 1000 );

    String clientURI =
        "http://TestService12.abc2.att.com/rest/nimbus/cust/checkAcct/123/abc/123?envContext=DEV&version=2&routeOffer=TEST";
    Request request =
        new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).build();


    DME2Client client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected as we are not binding any servlet
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) );
    }

    mgr.getEndpointRegistry().unpublish( serviceURI, "135.70.138.100", 7865 );
    Thread.sleep( 1000 );

		/*Check url using variable pattern template , {} , () and context path having "." Period (.) is special case in GRM used
		to split namespace and service. */
    String serviceURI_2 =
        "http://test-v1-8c.nimbus.att.com/nimbus/rest/test/v1.8c/{id}/user/{onid}?version=1.0.0&envContext=DEV&routeOffer=WEST";
    mgr.getEndpointRegistry().publish( serviceURI_2, null, "135.70.138.100", 17865, "http", null );

    Thread.sleep( 1000 );
    String clientURI_2 =
        "http://test-v1-8c.nimbus.att.com/nimbus/rest/test/v1.8c/123/user/123?version=1&envContext=DEV&routeOffer=WEST";
    request =
        new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_2 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected as we are not binding any servlet
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) );
    }

    mgr.getEndpointRegistry().unpublish( serviceURI_2, "135.70.138.100", 17865 );
    Thread.sleep( 1000 );

    // Test case for 2 different context paths published under same service
    String serviceURI_3 =
        "http://myapp.restful.att.com/nimbus/restful/myapp/{id}?version=1.0.0&envContext=DEV&routeOffer=WEST";

    mgr.getEndpointRegistry().publish( serviceURI_3, null, "135.204.107.65", 8080, "http", null );
    mgr.getEndpointRegistry().publish( serviceURI_3, null, "135.204.107.64", 8080, "http", null );
    Thread.sleep( 1000 );

    // Below call should fail because /restful1/myapp/1234 does not match any of above published uri's
    String clientURI_3 =
        "http://myapp.restful.att.com/nimbus/restful1/myapp/1234?version=1&envContext=DEV&routeOffer=WEST";
    request =
        new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_3 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0702 - found no endpoint, because the contextpath in client URI is not matching what's registered.
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) );
    }

    String clientURI_4 =
        "http://myapp.restful.att.com/nimbus/restful/myapp/1234?version=1&envContext=DEV&routeOffer=WEST";
    request =
        new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_4 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected behavior
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) );
    }

    String clientURI_5 = "http://myapp.restful.att.com/nimbus/restful/myapp/1234?version=1&envContext=DEV&partner=ABC";
    request =
        new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_5 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected behavior
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) );
    }

    String clientURI_6 = "http://myapp.restful.att.com/nimbus/restful/myapp/1234?version=1&envContext=DEV&partner=WEST";
    request =
        new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_6 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected behavior
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) );
    }

    String clientURI_7 =
        "http://DME2SEARCH/service=com.att.restful.myapp/nimbus/restful/myapp/1234/version=1/envContext=DEV/partner=WEST";
    request =
        new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_7 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      System.out.println( "FAILURE in clientURI_7" );
      // AFT-DME2-0703 - found endpoint, but no response as expected behavior
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0703" ) );
      System.exit( 1 );
    }


    mgr.getEndpointRegistry().unpublish( serviceURI_3, "135.204.107.65", 8080 );
    mgr.getEndpointRegistry().unpublish( serviceURI_3, "135.204.107.64", 8080 );
    Thread.sleep( 1000 );

    // Publish URI with namespace derived from queryparam
    String serviceURI_4 =
        "http://m2e.myapp.restful/nimbus/restful/myapp/id?version=1.0.0&envContext=DEV&routeOffer=WEST&ns=com.att.test.nsdemo";
    mgr.getEndpointRegistry().publish( serviceURI_4, null, "135.204.107.70", 8080, "http", null );


    mgr.getEndpointRegistry().unpublish( serviceURI_4, "135.204.107.70", 8080 );
  }


  @Test
  public void testDME2FilterInitParam() throws Exception {
    DME2Manager mgr = new DME2Manager();
    String svcURI = "service=com.att.aft.ServletFilterParamTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

    EchoServlet echoServlet = null;
    try {
      echoServlet = new EchoServlet( svcURI, "1" );
      String pattern[] = { "/test" };

      Properties params = new Properties();
      params.setProperty( "testParam", "TEST_INIT_PARAM" );

      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/ServletFilterParamTest" );
      srvHolder.setInitParams( params );

      // Additionally add MetricsServletFilter always to front of filter list
      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );
      dlist.add( DME2FilterHolder.RequestDispatcherType.ASYNC );

      Properties initParams = new Properties();
      initParams.setProperty( "testFilterParam", "TEST_FILTER_PARAM" );

      DME2FilterHolder fholder = null;
      fholder = new DME2FilterHolder( new TestInitParamFilter( "test" ), "/*", EnumSet.copyOf( dlist ) );
      fholder.setInitParams( initParams );

      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( fholder );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );

      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/ServletFilterParamTest" );
      svcHolder.setFilters( flist );
      svcHolder.setServletHolders( shList );
      svcHolder.disableMetricsFilter();

      mgr.addService( svcHolder );
      mgr.getServer().start();

      Thread.sleep( 4000 );

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.ServletFilterParamTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 100000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI ).withHeader( "testReturnFilterParam", "test" ).withContext( "/ServletFilterParamTest/test" ).build();

      EchoReplyHandler replyHandler = new EchoReplyHandler();

      Map<String, String> headers = new HashMap<String, String>();
      headers.put( "testReturnFilterParam", "test" );

      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setHeaders(headers);
      //client.setContext("/ServletFilterParamTest/test");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "filterParam=TEST_FILTER_PARAM" ) );
    } finally {
      try {
        mgr.unbindServiceListener( svcURI );
      } catch ( Exception e ) {
      }

      try {
        mgr.getServer().stop();
      } catch ( Exception e ) {
      }

      if ( echoServlet != null ) {
      //  echoServlet.destroy();
      }
    }
  }


  @Test
  @Ignore
  public void testDME2ServletHolderRestfulService() throws Exception {
    Properties props = RegistryFsSetup.init();
    DME2Manager mgr = new DME2Manager( "DME2ServletHolderRestfulService",
        new DME2Configuration( "DME2ServletHolderRestfulService", props ) );
    String svcURI =
        "http://DME2ServletHolderRestfulService.test.dme2.aft.att.com/dme2/restful/myapp/id?version=1.0.0&envContext=DEV&routeOffer=WEST";

    try {
      EchoResponseServlet echoServlet = new EchoResponseServlet(
          "service=com.att.aft.dme2.TestDME2ServletHolderRestfulService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/",
          "1" );
      String pattern[] = { "/test", "/dme2/restful/myapp/id" };

      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/test" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );

      // Adding a Log filter to print incoming msg.
      TestDME2LogFilter filter = new TestDME2LogFilter();
      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );

      DME2FilterHolder filterHolder = new DME2FilterHolder( filter, "/FilterTest", EnumSet.copyOf( dlist ) );
      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );

      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setServletHolders( shList );

      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 5000 );

      String clientURI =
          "http://DME2ServletHolderRestfulService.test.dme2.aft.att.com/dme2/restful/myapp/id?version=1.0.0&envContext=DEV&routeOffer=DEFAULT";
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI )
              .withSubContext( "/test" ).build();

      EchoReplyHandler replyHandler = new EchoReplyHandler();

      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setSubContext("/test");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "TestDME2ServletHolderRestfulService/" ) );
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
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


  @Test
  @Ignore
  public void testDME2ServletHolderRestfulService_WithDME2SEARCH() throws Exception {
    Properties props = RegistryFsSetup.init();
    DME2Manager mgr = new DME2Manager( "TestDME2SearchServletHolderRestfulService",
        new DME2Configuration( "TestDME2SearchServletHolderRestfulService", props ) );
    String svcURI =
        "http://TestDME2SearchServletHolderRestfulService.test.dme2.aft.att.com/dme2/restful/myapp/id?version=1.0.0&envContext=DEV&routeOffer=WEST";

    try {

      EchoResponseServlet echoServlet = new EchoResponseServlet(
          "service=com.att.aft.dme2.TestDME2SearchServletHolderRestfulService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/",
          "1" );
      String pattern[] = { "/test", "/dme2/restful/myapp/id/test" };

      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/test" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );

      // Adding a Log filter to print incoming msg.
      TestDME2LogFilter filter = new TestDME2LogFilter();

      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );

      DME2FilterHolder filterHolder = new DME2FilterHolder( filter, "/FilterTest", EnumSet.copyOf( dlist ) );

      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );

      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setServletHolders( shList );

      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 10000 );

      // Invokes the above registered FilterTest service by resolving endpoints via SOA registry
      String clientURI =
          "http://TestDME2SearchServletHolderRestfulService.test.dme2.aft.att.com/dme2/restful/myapp/id?version=1.0.0&envContext=DEV&partner=TEST";
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI )
              .withSubContext( "/test" ).build();

      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setSubContext("/test");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );

      assertTrue( reply.contains( "TestDME2SearchServletHolderRestfulService/" ) );
      Thread.sleep( 400 );

    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
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


  @Test
  @Ignore
  public void testDME2ServletHolderServiceAlias() throws Exception {
    Properties props = RegistryFsSetup.init();
    DME2Manager mgr = new DME2Manager( "AliasTestMgr", new DME2Configuration( "AliasTestMgr", props ) );
    String svcURI = "/service=com.att.aft.ServletHolderAliasTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
    String svcURI1 = "/service=com.att.aft.ServletHolderAliasTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

    try {
      EchoResponseServlet echoServlet = new EchoResponseServlet( svcURI, "1" );
      String pattern[] = { "/test", "/servletholderalias" };

      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/ServletHolderAliasTest/servletholderaliastest" );

      EchoResponseServlet echoServlet1 = new EchoResponseServlet( svcURI1, "1" );
      String pattern1[] = { "/test1", "/servletholderalias1" };

      DME2ServletHolder srvHolder1 = new DME2ServletHolder( echoServlet1, pattern1 );
      srvHolder1.setContextPath( "/servletholderaliastest1" );

      ArrayList<String> serviceAlias = new ArrayList<String>();
      serviceAlias.add( "service=com.att.aft.AliasTest1/version=1.0.1/envContext=LAB/routeOffer=DEFAULT/" );
      serviceAlias.add( "service=com.att.aft.AliasTest2/version=1.2.0/envContext=LAB/routeOffer=DEFAULT/" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );
      shList.add( srvHolder1 );

      // Adding a Log filter to print incoming msg.
      TestDME2LogFilter filter = new TestDME2LogFilter();
      ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );

      DME2FilterHolder filterHolder = new DME2FilterHolder( filter, "/FilterTest", EnumSet.copyOf( dlist ) );
      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );

      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setServiceAliases( serviceAlias );
      svcHolder.setFilters( flist );
      svcHolder.setServletHolders( shList );

      // mgr.addService(svcHolder);
      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 4000 );

      String clientURI =
          "http://DME2RESOLVE/service=com.att.aft.AliasTest1/version=1.0.1/envContext=LAB/routeOffer=DEFAULT";
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "POST" )
              .withReadTimeout( 10000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI )
              .withContext( "/test" )//.withSubContext( "/test" )
               .build();

      EchoReplyHandler replyHandler = new EchoReplyHandler();
      DME2Client client = new DME2Client( mgr, request );
      client.setResponseHandlers( replyHandler );
      //client.setContext("/test");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "ServletHolderAliasTest/" ) );
    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    } finally {
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

  @Ignore
  @Test
  public void testRestfulContext1() throws Exception {
    System.setProperty( "platform", "SANDBOX-DEV" );
    // Test case for 2 different context paths published under same service
    String serviceURI_3 =
        "http://myapp.restful.att.com/nimbus/restful/myapp/{id}?version=1.0.0&envContext=DEV&routeOffer=WEST";

    DME2Manager mgr = new DME2Manager();

    mgr.getEndpointRegistry().publish( serviceURI_3, null, "135.204.107.65", 8080, "http", null );
    mgr.getEndpointRegistry().publish( serviceURI_3, null, "135.204.107.64", 8080, "http", null );
    Thread.sleep( 1000 );

    String clientURI_7 =
        "http://DME2SEARCH/service=com.att.restful.myapp/nimbus/restful/myapp/1234/version=1/envContext=DEV/partner=WEST";
    Request request =
        new RequestBuilder( new URI( clientURI_7 ) ).withHttpMethod( "GET" )
            .withReadTimeout( 10000 ).withExchangeRoundTripTimeOut( 24000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_7 ).build();

    DME2Client client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      logger.debug( null, "testRestfulContext1", "Sending request 1" );
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected behavior
      e.printStackTrace();
      logger.debug( null, "testRestfulContext1", "Exception", e );
      assertTrue( e.getMessage().contains( "AFT-DME2-0707" ) );
    }

    // Below call should fail because /restful1/myapp/1234 does not match any of above published uri's
    String clientURI_3 =
        "http://myapp.restful.att.com/nimbus/restful1/myapp/1234?version=1&envContext=DEV&routeOffer=WEST";
    request =
        new RequestBuilder( new URI( clientURI_3 ) ).withHttpMethod( "GET" )
            .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_3 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0702 - found no endpoint, because the contextpath in client URI is not matching what's registered.
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) );
    }

    String clientURI_4 =
        "http://myapp.restful.att.com/nimbus/restful/myapp/1234?version=1&envContext=DEV&routeOffer=WEST";
    request =
        new RequestBuilder( new URI( clientURI_4 ) ).withHttpMethod( "GET" )
            .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_4 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected behavior
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0707" ) );
    }

    String clientURI_6 = "http://myapp.restful.att.com/nimbus/restful/myapp/1234?version=1&envContext=DEV&partner=WEST";
    request =
        new RequestBuilder( new URI( clientURI_6 ) ).withHttpMethod( "GET" )
            .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI_6 ).build();

    client = new DME2Client( mgr, request );
    //client.setMethod("GET");
    //client.setPayload("test");

    try {
      client.sendAndWait( new DME2TextPayload( "test" ) );
    } catch ( Exception e ) {
      // AFT-DME2-0703 - found endpoint, but no response as expected behavior
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "AFT-DME2-0707" ) );
    }


    mgr.getEndpointRegistry().unpublish( serviceURI_3, "135.204.107.65", 8080 );
    mgr.getEndpointRegistry().unpublish( serviceURI_3, "135.204.107.64", 8080 );
    Thread.sleep( 1000 );
  }

  @Test
  @Ignore
  public void testRestfulServiceWithAffinityRouting() throws Exception {
    DME2Manager mgr = null;
    DME2Manager mgr1 = null;

    String serviceName1 = "com.att.aft.dme2.test.TestRestfulService2";
    String serviceVersion1 = "1.0.0";
    String envContext1 = "LAB";
    String routeOffer1 = "TEST1";

    String serviceName2 = "com.att.aft.dme2.test.TestRestfulService2";
    String serviceVersion2 = "1.0.0";
    String envContext2 = "LAB";
    String routeOffer2 = "TEST2";

    String serviceURI1 =
        "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext2?version=1.0.0&envContext=LAB&routeOffer=TEST1";
    String serviceURI2 =
        "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext2?version=1.0.0&envContext=LAB&routeOffer=TEST2";

    String clientURI1 = "/restful/subcontext2?version=1.0.0&envContext=LAB&routeOffer=TEST1";
    String clientURI2 = "/restful/subcontext2?version=1.0.0&envContext=LAB&routeOffer=TEST2";

    String bindServiceURI1 = DME2Utils.buildServiceURIString( serviceName1, serviceVersion1, envContext1, routeOffer1 );
    String bindServiceURI2 = DME2Utils.buildServiceURIString( serviceName2, serviceVersion2, envContext2, routeOffer2 );

    String restfulServiceURI1 =
        "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext2/{id}?envContext=LAB&version=1.0.0&routeOffer=TEST1";
    String restfulServiceURI2 =
        "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext2/{id}?envContext=LAB&version=1.0.0&routeOffer=TEST2";

    int port1 = 32464;
    int port2 = 32466;

    String hostAddress = null;

    try {
      hostAddress = InetAddress.getLocalHost().getCanonicalHostName();
      // Save the route info
      RouteInfo rtInfo = new RouteInfo();
      rtInfo.setServiceName( "com.att.aft.dme2.test.TestRestfulService2" );
      rtInfo.setEnvContext( "LAB" );

      RouteGroups rtGrps = new RouteGroups();
      rtInfo.setRouteGroups( rtGrps );

      Properties props = new Properties();
      props.setProperty( "AFT_DME2_PORT", "" + port1 );
     // props.setProperty( "AFT_DME2_CLIENT_PROXY_HOST", "one.proxy.att.com" );
     // props.setProperty( "AFT_DME2_CLIENT_PROXY_PORT", "8080" );
      props.setProperty( DME2Constants.DME2_GRM_AUTH, "true" );

      RegistryFsSetup.init();
      RegistryFsSetup grmInit = new RegistryFsSetup();
      DME2Configuration config = new DME2Configuration( "com.att.aft.dme2.test.TestRestfulService2", props );

//      grmInit.saveRouteInfoForRestfulSearchROFailover(config, rtInfo, "LAB");
      mgr = new DME2Manager( "com.att.aft.dme2.test.TestRestfulService2", config, props );

      String pattern[] = { "/test", "/restful/subcontext2/*" };
      DME2ServletHolder servletHolder =
          new DME2ServletHolder( new EchoResponseServlet( bindServiceURI1, "testID1" ), pattern );

      List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
      servletHolderList.add( servletHolder );

      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder.setServiceURI( bindServiceURI1 );
      svcHolder.setManager( mgr );
      svcHolder.setServletHolders( servletHolderList );

      DME2TestContextListener contextListener = new DME2TestContextListener();

      ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
      contextList.add( contextListener );
      svcHolder.setContextListeners( contextList );

      Properties props1 = new Properties();
      props1.setProperty( "AFT_DME2_PORT", "" + port2 );

      mgr1 = new DME2Manager( "com.att.aft.dme2.test.TestRestfulService3",
          new DME2Configuration( "com.att.aft.dme2.test.TestRestfulService3", props1 ) );

      String pattern1[] = { "/test", "/restful/subcontext2/*" };
      DME2ServletHolder servletHolder1 =
          new DME2ServletHolder( new EchoResponseServlet( bindServiceURI2, "testID2" ), pattern1 );

      List<DME2ServletHolder> servletHolderList1 = new ArrayList<DME2ServletHolder>();
      servletHolderList1.add( servletHolder1 );

      DME2ServiceHolder svcHolder1 = new DME2ServiceHolder();
      svcHolder1.setServiceURI( bindServiceURI2 );
      svcHolder1.setManager( mgr1 );
      svcHolder1.setServletHolders( servletHolderList1 );

      DME2TestContextListener contextListener1 = new DME2TestContextListener();

      ArrayList<ServletContextListener> contextList1 = new ArrayList<ServletContextListener>();
      contextList1.add( contextListener1 );
      svcHolder1.setContextListeners( contextList1 );

      mgr.getServer().start();
      mgr.bindService( svcHolder );

      mgr1.getServer().start();
      mgr1.bindService( svcHolder1 );
      Thread.sleep( 5000 );

      List<DME2Endpoint> endpoints =
          mgr.getEndpointRegistry().findEndpoints( serviceName1, serviceVersion1, envContext1, routeOffer1 );
      System.out.println( "Number of Endpoints returned from GRM = " + endpoints.size() );
      assertTrue( endpoints.size() == 1 );
      System.out.println( endpoints.get( 0 ).toURLString() );

      // Test DIRECT URI for restful URI
      String clientURI = String.format( "http://%s:%s%s", hostAddress, "" + port1, clientURI1 );
      System.out.println( "ClientURI=" + clientURI );
      Request request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( clientURI )
              .withSubContext( "/test" ).build();

      DME2Client client = new DME2Client( mgr, request );
      String reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID1:::/service=com.att.aft.dme2.test.TestRestfulService2/version=1.0.0/envContext=LAB/routeOffer=TEST1;Request=THIS IS A TEST" ) );

      // Register using restful style URI
      mgr.getEndpointRegistry().publish( restfulServiceURI1, null, hostAddress, port1, "http", null );
      mgr.getEndpointRegistry().publish( restfulServiceURI2, null, hostAddress, port2, "http", null );

      Thread.sleep( 3000 );

      // Test RESOLVE URI for restful URI
      String restClientURI1 =
          "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestRestfulService2/restful/subcontext2/abc/version=1/envContext=LAB/routeOffer=TEST1";
      System.out.println( "ClientURI=" + restClientURI1 );
      request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI1 ).build();

      client = new DME2Client( mgr, request );
      reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID1:::/service=com.att.aft.dme2.test.TestRestfulService2/version=1.0.0/envContext=LAB/routeOffer=TEST1;Request=THIS IS A TEST" ) );

      // Test SEARCH URI for restful URI with dataContext
      String restClientURI2 =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestRestfulService2/restful/subcontext2/abc/version=1/envContext=LAB/partner=test1/dataContext=404707";
      System.out.println( "ClientURI=" + restClientURI2 );

      request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI2 ).build();


      client = new DME2Client( mgr, request );
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);
      reply = "";
      reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID1:::/service=com.att.aft.dme2.test.TestRestfulService2/version=1.0.0/envContext=LAB/routeOffer=TEST1;Request=THIS IS A TEST" ) );


      // Test STANDARD URI for restful URI with partner
      String restClientURI3 = "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext2/abc?version=1&envContext=LAB&partner=test1&dataContext=404707&stickySelectorKey=SSKEY1";
      System.out.println( "ClientURI=" + restClientURI3 );

      request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI3 ).build();
      client = new DME2Client( mgr, request );
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);
      reply = "";
      reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
      System.out.println( "Response returned from the service: " + reply );
      assertTrue( reply.contains(
          "EchoServlet:::testID2:::/service=com.att.aft.dme2.test.TestRestfulService2/version=1.0.0/envContext=LAB/routeOffer=TEST2;Request=THIS IS A TEST" ) );

      // Test STANDARD URI for restful URI with same partner, but just stickyKey, no dataContext
      String restClientURI4 =
          "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext2/abc?version=1&envContext=LAB&partner=test1&stickySelectorKey=SSKEY2";
      System.out.println( "ClientURI=" + restClientURI4 );

      request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI4 ).build();

      client = new DME2Client( mgr, request );
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);
      reply = "";
      try {
        reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
        System.out.println( "Response returned from the service: " + reply );
        assertTrue( reply == null );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue( "Expected " + e.getMessage() + " to contain AFT-DME2-0702 and routeOffersTried=TEST3", e.getMessage().contains( "routeOffersTried=TEST3" ) && e.getMessage().contains( "AFT-DME2-0702" ) );
      }

      // Test STANDARD URI for restful URI with context path not matching
      String restClientURI5 =
          "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext?version=1&envContext=LAB&partner=test1&stickySelectorKey=SSKEY1";
      System.out.println( "ClientURI=" + restClientURI5 );

      request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI5 ).build();

      client = new DME2Client( mgr, request );
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);
      reply = "";
      try {
        reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
        System.out.println( "Response returned from the service: " + reply );
        assertTrue( reply == null );
      } catch ( Exception e ) {
        System.out.println( "Response returned from the service cp not matching: " + reply );
        e.printStackTrace();
        assertTrue( e.getMessage().contains( "AFT-DME2-0103" ) );
      }

      // // Test STANDARD URI for restful URI with partner/invalid dataContext
      String restClientURI6 =
          "http://TestRestfulService2.test.dme2.aft.att.com/restful/subcontext?version=1&envContext=LAB&partner=test1&dataContext=40470";
      System.out.println( "ClientURI=" + restClientURI6 );

      request =
          new RequestBuilder( new URI( clientURI ) ).withHttpMethod( "GET" )
              .withReadTimeout( 300000 ).withReturnResponseAsBytes( false ).withLookupURL( restClientURI6 )
              .withSubContext( "/test" ).build();


      client = new DME2Client( mgr, request );
      //client.setPayload("THIS IS A TEST");
      //client.setMethod("GET");
      //client.setAllowAllHttpReturnCodes(true);
      reply = "";
      try {
        reply = (String) client.sendAndWait( new DME2TextPayload( "THIS IS A TEST" ) );
        System.out.println( "Response returned from the service: " + reply );
        assertTrue( reply == null );
      } catch ( Exception e ) {
        System.out.println( "Response returned from the service cp not matching: " + reply );
        e.printStackTrace();
        assertTrue( e.getMessage().contains( "AFT-DME2-0101" ) );
      }
    } finally {
      if ( mgr != null ) {
        try {
          mgr.unbindServiceListener( serviceURI1 );
        } catch ( DME2Exception e ) {
        }
      }
      if ( mgr1 != null ) {
        try {
          mgr1.getServer().stop();
          mgr1.unbindServiceListener( serviceURI2 );
        } catch ( DME2Exception e ) {
        }
      }

      if ( mgr != null ) {
        try {
          mgr.getEndpointRegistry().unpublish( restfulServiceURI1, hostAddress, port1 );
        } catch ( DME2Exception e ) {
        }

        try {
          mgr.getEndpointRegistry().unpublish( restfulServiceURI2, hostAddress, port2 );
        } catch ( DME2Exception e ) {
        }
      }
    }
  }
}
