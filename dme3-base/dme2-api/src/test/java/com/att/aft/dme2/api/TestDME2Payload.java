package com.att.aft.dme2.api;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.BinaryPayload;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.FilePayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.server.test.DME2TestReplyHandler;
import com.att.aft.dme2.server.test.EchoFileServlet;
import com.att.aft.dme2.server.test.EchoServlet;
import com.att.aft.dme2.server.test.FailoverServlet;
import com.att.aft.dme2.server.test.GWServlet;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.server.test.RegistryGrmSetup;
import com.att.aft.dme2.server.test.RestfulServlet;
import com.att.aft.dme2.server.test.StreamReplyFailoverServlet;
import com.att.aft.dme2.server.test.TestStreamReplyHandler;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;


public class TestDME2Payload {

	@Before
	public void setUp() throws Exception {
		System.setProperty( "DME2.DEBUG", "true" );
		System.setProperty( "platform", "SANDBOX-DEV" );
		System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
		Properties props = RegistryFsSetup.init();
		DME2Configuration config = new DME2Configuration( "testDME2StreamPayload", props );
		//			DME2Manager mgr = new DME2Manager("testDME2StreamPayload", config);
		//mgr = new DME2Manager("testDME2StreamPayload", config);
		Locations.BHAM.set();
	}

	@Test
	public void testDME2StreamPayload() throws Exception {
		Properties props = RegistryFsSetup.init();
		//			props.put("AFT_DME2_PORT", "51567");

		DME2Configuration config = new DME2Configuration( "testDME2StreamPayload", props );
		DME2Manager mgr = new DME2Manager( "testDME2StreamPayload", config );

		//DME2Manager mgr = new DME2Manager("testDME2StreamPayload", RegistryFsSetup.init());
		String serviceURIStr =
				"service=com.att.aft.dme2.TestDME2StreamPayload/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String clientURIStr =
				"http://DME2SEARCH/service=com.att.aft.dme2.TestDME2StreamPayload/version=1.0.0/envContext=DEV/partner=TEST";

		try {
			File f = new File( "src/test/etc/eng_f7.wav" );

			long fileLen = f.length();
			byte[] b = new byte[(int) f.length()];

			RandomAccessFile ra = new RandomAccessFile( f, "rw" );
			ra.read( b );

			ByteArrayInputStream bos = new ByteArrayInputStream( b );

			mgr.bindServiceListener( serviceURIStr, new GWServlet( serviceURIStr, "bau_se_1" ) );

			/* Add sleep to allow servlet init to happen */
			Thread.sleep( 10000 );

			try {
				DME2StreamPayload txtPayload = new DME2StreamPayload( bos );
				DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( config, clientURIStr, false );

				Map<String, String> headers = new HashMap<String, String>();
				headers.put( "testReturnStream", "true" );
				headers.put( "Content-Type", "audio/wav" );

				/**          DME2Client sender = new DME2Client(mgr, new URI(clientURIStr), 30000, "UTF-8");
 sender.setDME2Payload(txtPayload);
 sender.setHeaders(headers);
 sender.setReplyHandler(replyHandler);
 sender.send();
				 */
				Request request = new RequestBuilder(new URI( clientURIStr ) )
						.withHeaders( headers ).withReadTimeout( 20000 ).withReturnResponseAsBytes( false )
						.withLookupURL( clientURIStr ).build();
				DME2Client sender = new DME2Client( mgr, request );
				sender.setResponseHandlers( replyHandler );
				sender.send( txtPayload );

				Thread.sleep( 5000 );

				String response = replyHandler.getResponse( 60000 );
				System.out.println( "==========r e s p o n s e==================" + response );

				// response contains stream size read by servlet
				assertTrue(response.contains("size=" + fileLen));
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr );

			} catch ( Exception e ) {
			}

			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {

			}
		}
	}


	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testDME2PayloadClientRequestCharSetUTF8() throws Exception {
		String utf8String = null;

		Properties props = RegistryGrmSetup.init();
		//			props.put("AFT_DME2_PORT", "51567");

		DME2Configuration config = new DME2Configuration( "testDME2PayloadClientRequestCharSetUTF8", props );
		DME2Manager mgr = new DME2Manager( "testDME2PayloadClientRequestCharSetUTF8", config );

		//			DME2Manager mgr = new DME2Manager("testDME2PayloadClientRequestCharSetUTF8", RegistryFsSetup.init());
		String name =
				"service=com.att.aft.dme2.TestDME2PayloadClientRequestCharSetUTF8/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		try {
			File f = new File( "src/test/etc/utf8data.txt" );
			String tempData = null;
			StringBuffer strBuf = new StringBuffer();
			if ( f.exists() ) {
				BufferedReader br = new BufferedReader( new FileReader( f ) );
				while ( ( tempData = br.readLine() ) != null ) {
					strBuf.append( tempData );
				}
				utf8String = strBuf.toString();
			}
			System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
			// mgr.bindServiceListener(name, new DME2NullServlet(name));
			mgr.bindServiceListener( name, new EchoServlet( name, "bau_se_1" ) );

			// to allow servlet init to happen
			Thread.sleep( 1000 );
			// try to call a service we just registered
			// String uriStr =
			// "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.TestDME2PayloadClientRequestCharSetUTF8/version=1.0.0/envContext=DEV/partner=TEST";

			//				DME2Client sender = new DME2Client(mgr, new URI(uriStr), 30000, "UTF-8");
			DME2TextPayload txtPayload = new DME2TextPayload( utf8String );
			// txtPayload.set

			Map<String, String> headers = new HashMap<String, String>();
			headers.put( "testReturnCharSet", "UTF-8" );
			headers.put( "testEchoBack", "true" );

			//				sender.setPayload(txtPayload);
			//				sender.setHeaders(headers);
			DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( config, uriStr, false );
			//				sender.setReplyHandler(replyHandler);
			//				sender.send();

			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHeaders( headers )
					.withCharset( "UTF-8" ).withReadTimeout( 20000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();
			DME2Client sender = new DME2Client( mgr, request );
			sender.setResponseHandlers( replyHandler );
			sender.send( txtPayload );

			String response = replyHandler.getResponse( 60000 );

			if ( replyHandler.echoedCharSet == null ) {
				fail( "charset was not set to UTF-8 as expected, instead was null" );
			}

			if ( !replyHandler.echoedCharSet.equals( "UTF-8" ) ) {
				fail( "charset was not set to UTF-8 as expected, instead was " + replyHandler.echoedCharSet );
			}


			assertTrue(response.contains("<data>testmessagewithtrademark"));
			assertTrue( response.contains( "testmessagewithtrademark" ) );

		} finally {
			try {
				mgr.unbindServiceListener( name );

			} catch ( Exception e ) {
			}
			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}
		}
	}


	// this works fine when run individually
	@Ignore
	@Test
	public void testDME2StreamPayloadIgnoreFailover() throws Exception {
		String utf8String = null;

		Properties props = RegistryGrmSetup.init();
		//			props.put("AFT_DME2_PORT", "51567");

		DME2Configuration config = new DME2Configuration( "testDME2StreamPayloadIgnoreFailover", props );
		DME2Manager mgr = new DME2Manager( "testDME2StreamPayloadIgnoreFailover", config );

		//			DME2Manager mgr = new DME2Manager("testDME2StreamPayloadIgnoreFailover", RegistryFsSetup.init());

		String name =
				"service=com.att.aft.dme2.TestDME2StreamPayloadIgnoreFailover/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String name1 =
				"service=com.att.aft.dme2.TestDME2StreamPayloadIgnoreFailover/version=1.0.0/envContext=DEV/routeOffer=WALMART";
		try {
			File f = new File( "src/test/etc/eng_f7.wav" );
			long fileLen = f.length();
			FileInputStream fis = null;
			String tempData = null;
			StringBuffer strBuf = new StringBuffer();
			if ( f.exists() ) {
				fis = new FileInputStream( f );
			}
			System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
			// mgr.bindServiceListener(name, new DME2NullServlet(name));
			mgr.bindServiceListener( name, new GWServlet( name, "bau_se_1" ) );
			mgr.bindServiceListener( name1, new GWServlet( name, "walmart" ) );

			// to allow servlet init to happen
			Thread.sleep( 10000 );
			// try to call a service we just registered
			// String uriStr =
			// "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.TestDME2StreamPayloadIgnoreFailover/version=1.0.0/envContext=DEV/partner=TEST";

			//				DME2Client sender = new DME2Client(mgr, new URI(uriStr), 30000, "UTF-8");
			DME2StreamPayload txtPayload = new DME2StreamPayload( fis );
			// txtPayload.set
			//				sender.setDME2Payload(txtPayload);
			Map<String, String> headers = new HashMap<String, String>();
			headers.put( "testReturnFault1", "true" );
			//				sender.setHeaders(headers);

			EchoReplyHandler replyHandler = new EchoReplyHandler();

			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHeaders( headers )
					.withCharset( "UTF-8" ).withReadTimeout( 20000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();
			DME2Client sender = new DME2Client( mgr, request );
			sender.setResponseHandlers( replyHandler );
			sender.send( txtPayload );

			//				sender.setReplyHandler(replyHandler);
			//				sender.send();
			try {
				String response = replyHandler.getResponse( 60000 );

				System.out.println( response );
				// response contains stream size read by servlet
				assertTrue( response == null );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains( "AFT-DME2-0717" ) );
			}

		} finally {
			try {
				mgr.unbindServiceListener( name );

			} catch ( Exception e ) {
			}
			try {
				mgr.unbindServiceListener( name1 );

			} catch ( Exception e ) {
			}
			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}
			try {
				Thread.sleep( 15000 );
			} catch ( Exception e ) {

			}
		}
	}

	@Test
	public void testDME2NoPayload() throws Exception {
		try {
			// try to call a service we just registered
			// String uriStr =
			// "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.TestDME2CallDownStreamPayload/version=1.0.0/envContext=DEV/partner=TEST";
			DME2Configuration config = new DME2Configuration( "TestDME2NoPayload", RegistryFsSetup.init() );
			DME2Manager mgr = new DME2Manager( "TestDME2NoPayload", config );
			//				DME2Client sender = new DME2Client(mgr, new URI(uriStr), 30000, "UTF-8");
			Map<String, String> headers = new HashMap<String, String>();
			headers.put( "testCallDownstream", "true" );
			//				sender.setHeaders(headers);

			DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( mgr.getConfig(), uriStr, false );

			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHeaders( headers )
					.withCharset( "UTF-8" ).withReadTimeout( 20000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();
			DME2Client sender = new DME2Client( mgr, request );
			sender.setResponseHandlers( replyHandler );
			//				sender.send(txtPayload);


			try {
				sender.send( null ); // adding null for now for no payload
				String response = replyHandler.getResponse( 60000 );

				System.out.println( response );
				// response contains stream size read by servlet
				assertTrue( response.contains( "Chunked response 1" ) );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0701"));
			}

		} finally {
		}

	}

	@Ignore
	@Test
	public void testDME2StreamPayloadIgnoreFailoverOnTimeout() throws Exception {
		String utf8String = null;
		Properties props = RegistryGrmSetup.init();
		//			props.put("AFT_DME2_PORT", "51567");

		DME2Configuration config = new DME2Configuration( "testDME2StreamPayloadIgnoreFailover", props );
		DME2Manager mgr = new DME2Manager( "testDME2StreamPayloadIgnoreFailover", config );

		//			DME2Manager mgr = new DME2Manager("testDME2StreamPayloadIgnoreFailover", RegistryFsSetup.init());

		String name =
				"service=com.att.aft.dme2.TestDME2StreamPayloadIgnoreFailoverOnTimeout/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String name1 =
				"service=com.att.aft.dme2.TestDME2StreamPayloadIgnoreFailoverOnTimeout/version=1.0.0/envContext=DEV/routeOffer=WALMART";
		try {
			File f = new File( "src/test/etc/eng_f7.wav" );
			long fileLen = f.length();
			RandomAccessFile ra = new RandomAccessFile( f, "rw" );
			byte[] b = new byte[(int) f.length()];
			ra.read( b );
			ByteArrayInputStream bos = new ByteArrayInputStream( b );
			System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
			// mgr.bindServiceListener(name, new DME2NullServlet(name));
			mgr.bindServiceListener( name, new GWServlet( name, "bau_se_1" ) );
			mgr.bindServiceListener( name1, new GWServlet( name, "walmart" ) );

			// to allow servlet init to happen
			Thread.sleep( 10000 );
			// try to call a service we just registered
			// String uriStr =
			// "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.TestDME2StreamPayloadIgnoreFailoverOnTimeout/version=1.0.0/envContext=DEV/partner=TEST";
			DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( mgr.getConfig(), uriStr, false );

			//				DME2Client sender = new DME2Client(mgr, new URI(uriStr), 30000, "UTF-8");
			Map<String, String> headers = new HashMap<String, String>();
			headers.put( "testReturnFault", "true" );
			headers.put( "echoSleepTimeMs", "15000" );
			headers.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "9000" );

			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHeaders( headers )
					.withCharset( "UTF-8" ).withReadTimeout( 30000 ).withPerEndpointTimeoutMs(9000).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();
			DME2Client sender = new DME2Client( mgr, request );
			DME2StreamPayload txtPayload = new DME2StreamPayload( bos );
			// txtPayload.set
			//sender.setDME2Payload(txtPayload);
			sender.setResponseHandlers( replyHandler );

			//				sender.setHeaders(headers);
			//				sender.setReplyHandler(replyHandler);
			sender.send( txtPayload );
			try {
				String response = replyHandler.getResponse( 60000 );

				System.out.println( response );
				// response contains stream size read by servlet
				assertTrue( response == null );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e.getMessage().contains("AFT-DME2-0717"));
			}

		} finally {
			try {
				mgr.unbindServiceListener( name );

			} catch ( Exception e ) {
			}
			try {
				mgr.unbindServiceListener( name1 );

			} catch ( Exception e ) {
			}
			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}
			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {

			}
		}
	}

	@Ignore
	@Test
	public void testDME2CallDownStreamPayload() throws Exception {
		String name =
				"service=com.att.aft.dme2.TestDME2CallDownStreamPayload/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String name1 =
				"service=com.att.aft.dme2.TestDME2CallDownStreamPayload/version=1.0.0/envContext=DEV/routeOffer=WALMART";

		Properties props = RegistryFsSetup.init();
		//			props.put("AFT_DME2_PORT", "51567");
		DME2Configuration config = new DME2Configuration( "testDME2StreamPayloadIgnoreFailover", props );
		DME2Manager mgr = new DME2Manager( "testDME2StreamPayloadIgnoreFailover", config );

		//			DME2Manager mgr = new DME2Manager("testDME2StreamPayloadIgnoreFailover", RegistryFsSetup.init());
		String svcURI = "service=com.att.aft.dme2.SpeechRestfulServlet/version=1.0.0/envContext=DEV/routeOffer=DEFAULT";
		try {

			// Create service holder for each service registration
			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI( svcURI );
			svcHolder.setManager( mgr );
			svcHolder.setContext( "/RSServlet" );

			RestfulServlet echoServlet = new RestfulServlet();
			String pattern[] = { "/rsservlet" };
			DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
			srvHolder.setContextPath( "/rsservlet" );

			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add( srvHolder );
			// If context is set, DME2 will use this for publishing as context with
			// endpoint registration, else serviceURI above will be used
			// svcHolder.setContext("/FilterTest");
			// Below is to disable the default metrics filter thats added to
			// capture DME2 Metrics event of http traffic. By default MetricsFilter
			// is enabled
			// svcHolder.disableMetricsFilter();

			svcHolder.setServletHolders( shList );

			// mgr.addService(svcHolder);
			mgr.getServer().start();
			mgr.bindService( svcHolder );
			Thread.sleep( 2000 );
			File f = new File( "src/test/etc/eng_f7.wav" );
			long fileLen = f.length();
			FileInputStream fis = null;
			String tempData = null;
			StringBuffer strBuf = new StringBuffer();
			byte[] b = "Request payload in text format".getBytes();
			ByteArrayInputStream bos = new ByteArrayInputStream( b );
			if ( f.exists() ) {
				fis = new FileInputStream( f );
			}
			System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
			// mgr.bindServiceListener(name, new DME2NullServlet(name));
			mgr.bindServiceListener( name, new GWServlet( name, "bau_se_1" ) );

			// to allow servlet init to happen
			Thread.sleep( 5000 );
			// try to call a service we just registered
			// String uriStr =
			// "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
			String uriStr =
					"http://DME2SEARCH/service=com.att.aft.dme2.TestDME2CallDownStreamPayload/version=1.0.0/envContext=DEV/partner=TEST";

			//				Request request = new RequestBuilder(mgr.getClient(), new HttpConversation(), new URI(uriStr)).withHeaders(headers).withCharset("UTF-8").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			//				DME2Client sender = new DME2Client(mgr, request);

			//				DME2Client sender = new DME2Client(mgr, new URI(uriStr), 30000, "UTF-8");
			DME2StreamPayload txtPayload = new DME2StreamPayload( bos );
			// txtPayload.set
			//sender.setPayload(txtPayload);
			Map<String, String> headers = new HashMap<String, String>();
			headers.put( "testCallDownstream", "true" );
			//sender.setHeaders(headers);
			DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( mgr.getConfig(), uriStr, false );

			Request request =
					new RequestBuilder(new URI( uriStr ) ).withHeaders( headers )
					.withCharset( "UTF-8" ).withReadTimeout( 20000 ).withReturnResponseAsBytes( false )
					.withLookupURL( uriStr ).build();
			DME2Client sender = new DME2Client( mgr, request );
			//				StreamPayload txtPayload = new StreamPayload(bos);
			// txtPayload.set
			//sender.setDME2Payload(txtPayload);
			sender.setResponseHandlers( replyHandler );
			//				sender.setReplyHandler(replyHandler);

			sender.send( txtPayload );
			try {
				String response = replyHandler.getResponse( 60000 );

				System.out.println( " Response " + response );
				assertTrue( response.contains( "Chunked response" ) );
			} catch ( Exception e ) {
				e.printStackTrace();
				assertTrue( e != null );
			}

		} finally {
			try {
				mgr.unbindServiceListener( name );

			} catch ( Exception e ) {
			}
			try {
				mgr.unbindServiceListener( svcURI );

			} catch ( Exception e ) {
			}
			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}
			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {

			}
		}
	}

	@Test
	public void testDME2Payload_WithTextAndFileInputProvided() throws Exception {
		/*If both text and file payload are provided, file payload should take priority and the text payload will be ignored*/
		Properties props = RegistryFsSetup.init();
		//			props.put("AFT_DME2_PORT", "51567");
		DME2Configuration config = new DME2Configuration( "testDME2FilePayload", props );
		DME2Manager mgr = new DME2Manager( "testDME2FilePayload", config );

		//			DME2Manager mgr = new DME2Manager("testDME2FilePayload", RegistryFsSetup.init());
		String serviceURIStr =
				"/service=com.att.aft.dme2.test.TestDME2FilePayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		String clientURIStr =
				"http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2FilePayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

		try {
			mgr.bindServiceListener( serviceURIStr, new EchoFileServlet() );
			Thread.sleep( 3000 );

			try {
				String txtFile = "src/test/etc/rinfo.txt";
				FilePayload filePayLoad = new FilePayload( txtFile, false, false );

				Request request = new RequestBuilder(new URI( clientURIStr ) )
						.withCharset( "UTF-8" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( false )
						.withLookupURL( clientURIStr ).build();
				DME2Client sender = new DME2Client( mgr, request );

				//					DME2Client sender = new DME2Client(mgr, new URI(clientURIStr), 30000);
				//					sender.setDME2Payload(filePayLoad);
				//					sender.setMethod("POST");
				//					TextPayload 
				//					sender.setPayload("This is a test.");

				String response = (String) sender.sendAndWait( filePayLoad );
				System.err.println( response );

				assertTrue( response != null );
				assertTrue( response.contains( "No file uploaded" ) );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr );

			} catch ( Exception e ) {
			}

			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2Payload_WithTextAndMultiPartFileInputProvided() throws Exception {
		/*If both text and file payload are provided, file payload should take priority and the text payload will be ignored*/
		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration( "testDME2FilePayload", props );
		DME2Manager mgr = new DME2Manager( "testDME2FilePayload", config );

		//			DME2Manager mgr = new DME2Manager("testDME2FilePayload", RegistryFsSetup.init());
		String serviceURIStr =
				"service=com.att.aft.dme2.test.TestDME2FilePayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		String clientURIStr =
				"http://DME2RESOLVE/service=com.att.aft.dme2.test.TestDME2FilePayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

		try {
			mgr.bindServiceListener( serviceURIStr, new EchoFileServlet() );
			Thread.sleep( 3000 );

			String txtFile = "src/test/etc/rinfo.txt";
			FilePayload filePayLoad = new FilePayload( txtFile, true, false );

			Request request = new RequestBuilder(new URI(clientURIStr)).withHttpMethod("POST").withReadTimeout(15000).withReturnResponseAsBytes(false).withLookupURL(clientURIStr).build();

			DME2Client sender = new DME2Client( mgr, request );
			String response = (String) sender.sendAndWait( filePayLoad );
			System.err.println( response );

			assertTrue( response != null );
			assertTrue( response.contains( "Uploaded Filename: " ) );
		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr );
			} catch ( Exception e ) {
			}

			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {
			}
		}
	}

	@Test
	public void testDME2Payload_WithStreamPayloadAndTextPayloadInputProvided() throws Exception {

		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration( "testDME2StreamPayload", props );
		DME2Manager mgr = new DME2Manager( "testDME2StreamPayload", config );

		//			DME2Manager mgr = new DME2Manager("testDME2StreamPayload", RegistryFsSetup.init());
		String serviceURIStr =
				"service=com.att.aft.dme2.TestDME2StreamPayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		String clientURIStr =
				"http://DME2RESOLVE/service=com.att.aft.dme2.TestDME2StreamPayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

		try {
			File f = new File( "src/test/etc/eng_f7.wav" );

			long fileLen = f.length();
			byte[] b = new byte[(int) f.length()];

			RandomAccessFile ra = new RandomAccessFile( f, "rw" );
			ra.read( b );

			ByteArrayInputStream bos = new ByteArrayInputStream( b );

			mgr.bindServiceListener( serviceURIStr, new GWServlet( serviceURIStr, "bau_se_1" ) );

			Thread.sleep( 10000 );


			DME2StreamPayload streamPayload = new DME2StreamPayload( bos );
			DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( mgr.getConfig(), clientURIStr, false );

			Map<String, String> headers = new HashMap<String, String>();
			headers.put( "testReturnStream", "true" );
			headers.put( "Content-Type", "audio/wav" );

			Request request = new RequestBuilder(new URI( clientURIStr ) )
					.withHeaders( headers ).withCharset( "UTF-8" ).withHttpMethod( "POST" ).withReadTimeout( 30000 )
					.withReturnResponseAsBytes( false ).withLookupURL( clientURIStr ).build();
			DME2Client sender = new DME2Client( mgr, request );
			//					DME2Client sender = new DME2Client(mgr, new URI(clientURIStr), 300000, "UTF-8");
			//					sender.setDME2Payload(streamPayload);
			//					sender.setPayload("This is a test");
			//					sender.setHeaders(headers);
			sender.setResponseHandlers( replyHandler );
			sender.send( streamPayload );

			Thread.sleep( 5000 );

			String response = replyHandler.getResponse( 30000 );
			System.out.println( response );

			// response contains stream size read by servlet
			assertTrue( "response was " + response + ", expected to see size=" + fileLen, response.contains( "size=" + fileLen ) );

		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr );

			} catch ( Exception e ) {
			}

			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {

			}
		}
	}


	//this needs to be check for AsyncResponseHandler
	@Test
	public void testDME2TextPayload_ReturnResponseBytes() throws Exception {
		//System.setProperty("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
		//System.setProperty("AFT_DME2_CLIENT_PROXY_PORT", "9999");

		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration( "testDME2Payload_ReturnResponseBytes", props );
		DME2Manager mgr = new DME2Manager( "testDME2Payload_ReturnResponseBytes", config );

		//			DME2Manager mgr = new DME2Manager("testDME2Payload_ReturnResponseBytes", RegistryFsSetup.init());
		String serviceURIStr =
				"service=com.att.aft.dme2.TestDME2StreamPayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";
		String clientURIStr =
				"http://DME2RESOLVE/service=com.att.aft.dme2.TestDME2StreamPayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE";

		try {

			mgr.disableMetrics();
			mgr.disableMetricsFilter();
			mgr.bindServiceListener( serviceURIStr, new GWServlet( serviceURIStr, "bau_se_1" ) );

			Thread.sleep( 2000 );

			try {
				TestStreamReplyHandler replyHandler = new TestStreamReplyHandler();

				Request request = new RequestBuilder(new URI( clientURIStr ) )
						.withCharset( "UTF-8" ).withHttpMethod( "POST" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( true )
						.withLookupURL( clientURIStr ).build();
				DME2Client sender = new DME2Client( mgr, request );

				//					DME2Client sender = new DME2Client(mgr, new URI(clientURIStr), 300000, "UTF-8", true);
				//					sender.setAllowAllHttpReturnCodes(true);
				//					sender.setPayload("This is a test");
				//					sender.setResponseHandlers(replyHandler);

				sender.setResponseHandlers(replyHandler);
				sender.send( new DME2TextPayload( "This is a test" ) );

				Thread.sleep( 2000 );

				System.err.println( replyHandler.getResponse( 30000 ) );
				ByteArrayOutputStream response = replyHandler.getByteStream();

				System.err.println( "////// RESULT = " + new String( response.toByteArray() ) + " " + replyHandler + "===========" + response.toString());

				assertTrue( response.toString().contains(
						"EchoServlet:::GWServletbau_se_1:::service=com.att.aft.dme2.TestDME2StreamPayload/version=1.0.0/envContext=LAB/routeOffer=BAU_SE" ) );

			} catch ( Exception e ) {
				e.printStackTrace();
				fail( e.getMessage() );
			}
		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr );

			} catch ( Exception e ) {
			}

			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 1500 );
			} catch ( Exception e ) {

			}
		}
	}


	//this needs to be check for AsyncResponseHandler
	@Ignore
	@Test
	public void testDME2TextPayload_ReturnResponseBytesOnFailover() throws Exception {
		//System.setProperty("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
		//System.setProperty("AFT_DME2_CLIENT_PROXY_PORT", "9999");

		Properties props = RegistryGrmSetup.init();

		DME2Configuration config = new DME2Configuration( "testDME2Payload_ReturnResponseBytes", props );
		DME2Manager mgr = new DME2Manager( "testDME2Payload_ReturnResponseBytes", config );

		//			DME2Manager mgr = new DME2Manager("testDME2Payload_ReturnResponseBytes", RegistryFsSetup.init());
		DME2Manager mgr2 = new DME2Manager( "testDME2Payload_ReturnResponseBytes2", config );

		String serviceURIStr =
				"service=com.att.aft.dme2.test.TestDME2StreamReply/version=1.0.0/envContext=LAB/routeOffer=SUCCESS";
		String serviceURIStr2 =
				"service=com.att.aft.dme2.test.TestDME2StreamReply/version=1.0.0/envContext=LAB/routeOffer=FAIL";
		String clientURIStr =
				"http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2StreamReply/version=1.0.0/envContext=LAB/partner=TEST";

		try {

			mgr.disableMetrics();
			mgr.disableMetricsFilter();
			mgr.bindServiceListener( serviceURIStr, new StreamReplyFailoverServlet( "SUCCESS" ) );
			mgr2.bindServiceListener( serviceURIStr2, new StreamReplyFailoverServlet( "FAIL" ) );

			Thread.sleep( 3000 );

			try {
				TestStreamReplyHandler replyHandler = new TestStreamReplyHandler();

				Request request = new RequestBuilder(new URI( clientURIStr ) )
						.withCharset( "UTF-8" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( true )
						.withLookupURL( clientURIStr ).build();
				DME2Client sender = new DME2Client( mgr, request );
				//					DME2Client sender = new DME2Client(mgr, new URI(clientURIStr), 300000, "UTF-8", true);
				//					sender.setPayload("This is a test");
				sender.setResponseHandlers(replyHandler);
				sender.send( new DME2TextPayload( "This is a test" ) );

				//Thread.sleep(2000);

				System.err.println( replyHandler.getResponse( 30000 ) );
				ByteArrayOutputStream response = replyHandler.getByteStream();

				System.err.println( "////// RESULT = " + new String( response.toByteArray() ) + " " + replyHandler );

				assertTrue( response.toString().contains( "SUCCESSFUL REQUEST" ) );
			} catch ( Exception e ) {
				e.printStackTrace();
				fail( e.getMessage() );
			}
		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr );

			} catch ( Exception e ) {
			}

			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 1500 );
			} catch ( Exception e ) {

			}
		}
	}

	@Test
	public void testDME2BinaryPayload() throws Exception {

		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration( "testDME2StreamPayload", props );
		DME2Manager mgr = new DME2Manager( "testDME2StreamPayload", config );

		//			DME2Manager mgr = new DME2Manager("testDME2StreamPayload", RegistryFsSetup.init());
		String serviceURIStr =
				"service=com.att.aft.dme2.TestDME2BinaryPayload/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String clientURIStr =
				"http://DME2RESOLVE/service=com.att.aft.dme2.TestDME2BinaryPayload/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

		try {

			mgr.bindServiceListener( serviceURIStr, new GWServlet( serviceURIStr, "bau_se_1" ) );

			/* Add sleep to allow servlet init to happen */
			Thread.sleep( 10000 );

			try {
				BinaryPayload binaryPayload = new BinaryPayload( ( "This is a test" ).getBytes() );
				DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( mgr.getConfig(), clientURIStr, false );

				Map<String, String> headers = new HashMap<String, String>();
				headers.put( "testReturnStream", "true" );
				headers.put( "Content-Type", "audio/wav" );

				Request request = new RequestBuilder(new URI( clientURIStr ) )
						.withHeaders( headers ).withCharset( "UTF-8" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( false )
						.withLookupURL( clientURIStr ).build();
				DME2Client sender = new DME2Client( mgr, request );

				//					DME2Client sender = new DME2Client(mgr, new URI(clientURIStr), 30000, "UTF-8");
				//					sender.setDME2Payload(binaryPayload);
				//					sender.setHeaders(headers);
				//					sender.setReplyHandler(replyHandler);
				sender.send( binaryPayload );

				Thread.sleep( 5000 );

				String response = replyHandler.getResponse( 60000 );
				System.out.println( response );

				// response contains stream size read by servlet
				assertTrue( response.contains( "size=" + ( "This is a test" ).length() ) );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr );

			} catch ( Exception e ) {
			}

			try {
				mgr.getServer().stop();
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {

			}
		}
	}

	@Test
	public void testDME2BinaryPayloadFailover() throws Exception {

		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration( "testDME2BInaryPayload", props );
		DME2Manager mgr = new DME2Manager( "testDME2BInaryPayload", config );

		//TODO: Save routeInfo for this service as part of test case
		//			DME2Manager mgr = new DME2Manager("testDME2BInaryPayload", RegistryFsSetup.init());
		DME2Manager mgr_2 = new DME2Manager( "testDME2BInaryPayload2", config );
		String serviceURIStr_1 =
				"/service=com.att.aft.dme2.test.TestDME2BinaryPayloadFailover/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
		String serviceURIStr_2 =
				"/service=com.att.aft.dme2.test.TestDME2BinaryPayloadFailover/version=1.0.0/envContext=LAB/routeOffer=SECONDARY";
		String clientURIStr =
				"http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2BinaryPayloadFailover/version=1.0.0/envContext=LAB/partner=TEST";

		try {

			mgr.bindServiceListener( serviceURIStr_1, new FailoverServlet( serviceURIStr_1, "bau_se_1" ) );
			mgr_2.bindServiceListener( serviceURIStr_2, new GWServlet( serviceURIStr_2, "bau_se_1" ) );

			/* Add sleep to allow servlet init to happen */
			Thread.sleep( 10000 );

			try {
				BinaryPayload binaryPayload = new BinaryPayload( ( "This is a test" ).getBytes() );
				DME2TestReplyHandler replyHandler = new DME2TestReplyHandler( mgr.getConfig(), clientURIStr, false );

				Map<String, String> headers = new HashMap<String, String>();
				headers.put( "testReturnStream", "true" );
				headers.put( "Content-Type", "audio/wav" );
				headers.put( "AFT_DME2_REQ_TRACE_ON", "true" );

				Request request = new RequestBuilder(new URI( clientURIStr ) )
						.withHeaders( headers ).withCharset( "UTF-8" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( false )
						.withLookupURL( clientURIStr ).build();
				DME2Client sender = new DME2Client( mgr, request );

				//					DME2Client sender = new DME2Client(mgr, new URI(clientURIStr), 30000, "UTF-8");
				//					sender.setDME2Payload(binaryPayload);
				//					sender.setHeaders(headers);


				//sender.setReplyHandler(replyHandler);
				sender.send( binaryPayload );

				Thread.sleep( 5000 );

				String response = replyHandler.getResponse( 60000 );
				System.out.println( response );

				String traceInfo = replyHandler.getResponseHeaders().get( "AFT_DME2_REQ_TRACE_INFO" );
				System.out.println( traceInfo );

				// response contains stream size read by servlet
				assertTrue( response.contains( "size=" + ( "This is a test" ).length() ) );
				assertTrue( traceInfo.contains( "routeOffer=PRIMARY:onResponseCompleteStatus=503" ) );
				assertTrue( traceInfo.contains( "routeOffer=SECONDARY:onResponseCompleteStatus=200" ) );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		} finally {
			try {
				mgr.unbindServiceListener( serviceURIStr_1 );

			} catch ( Exception e ) {
			}

			try {
				mgr_2.unbindServiceListener( serviceURIStr_2 );
			} catch ( Exception e ) {
			}

			try {
				Thread.sleep( 5000 );
			} catch ( Exception e ) {

			}
		}
	}


}
