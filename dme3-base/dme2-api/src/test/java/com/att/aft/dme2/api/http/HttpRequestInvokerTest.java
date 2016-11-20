package com.att.aft.dme2.api.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.input.CharSequenceInputStream;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.handler.DefaultAsyncResponseHandler;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.IteratorCreatingAttributes;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.request.BinaryPayload;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.FilePayload;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.RequestContext;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.util.DME2URIUtils;

public class HttpRequestInvokerTest {
	private static final String DEFAULT_TEXT_PAYLOAD = RandomStringUtils.randomAlphanumeric( 100 );
	private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
	private static final String DEFAULT_VERSION = Integer.toString( RandomUtils.nextInt( 100 ) );
	private static final String DEFAULT_ENV = RandomStringUtils.randomAlphanumeric( 3 );
	private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_SERVICE =
			DME2URIUtils.buildServiceURIString( DEFAULT_SERVICE_NAME, DEFAULT_VERSION, DEFAULT_ENV, DEFAULT_ROUTE_OFFER );
	private static final String DEFAULT_HOST = "127.0.0.1";
	private static final String DEFAULT_PORT = "45678";
	private static final String DEFAULT_URI = "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT + DEFAULT_SERVICE;

	private static Result result;
	private static Server server;
	@BeforeClass
	public static void setUpTest() throws Exception {
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		server = new Server(new InetSocketAddress("127.0.0.1", 45678));
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new SimpleServlet()), "/*");
		server.start();
	}

	@AfterClass
	public static void tearDownTest() {
		new Thread() {
			@Override
			public void run() {
				if ( server != null ) {
					try {
						server.stop();
					} catch ( Exception e ) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		System.clearProperty("AFT_ENVIRONMENT");
		System.clearProperty("AFT_LATITUDE");
		System.clearProperty("AFT_LONGITUDE");
		System.clearProperty("DME2.DEBUG");
		System.clearProperty("platform");
		System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
	}
	@Before
	public void setupTest() {
		HttpRequestInvokerTest.result = null;
	}

	@Test
	public void test_execute_text_payload() throws Exception {
		String managerName = RandomStringUtils.randomAlphanumeric( 10 );
		RequestContext context = new RequestContext();
		DME2Configuration config = new DME2Configuration( managerName );
		AsyncResponseHandlerIntf responseHandler = new DefaultAsyncResponseHandler( config, DEFAULT_URI, false );
		context.setMgr( new DME2Manager( managerName, config ) );
		context.setRequest( new HttpRequest(new URI( DEFAULT_URI ) ) );
		context.getRequest().setLookupUri( DEFAULT_URI );
		context.getRequest().setReadTimeout( 10000 );
		context.getRequest().setCharset( "UTF-8" );
		context.getRequest().setResponseHandler( responseHandler );
		DmeUniformResource uniformResource = new DmeUniformResource(config, DEFAULT_URI); 
		context.getRequest().setUniformResource(uniformResource);

		IteratorCreatingAttributes ica = new IteratorCreatingAttributes();
		ica.setConfig( config );
		ica.setEndpointHolders( new ArrayList<DME2EndpointReference>() );

		HttpRequestInvoker invoker = new HttpRequestInvoker( context );
		invoker.createExchange( DEFAULT_HOST, context, EndpointIteratorFactory.getDefaultIterator( ica ) );
		System.out.println( "SENDING CONTENT: " + DEFAULT_TEXT_PAYLOAD );
		Object response = null;
		try {
			invoker.execute( null, context, new DME2TextPayload( DEFAULT_TEXT_PAYLOAD ) );
			response = responseHandler.getResponse( 10000 );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		System.out.println("response: " + response);

		assertNotNull( response );
		assertTrue( response instanceof String );
		assertTrue( ( (String) response ).contains( DEFAULT_TEXT_PAYLOAD ) );
	}

	@Test
	public void test_execute_file_payload() throws Exception {
		String managerName = RandomStringUtils.randomAlphanumeric( 10 );
		RequestContext context = new RequestContext();
		DME2Configuration config = new DME2Configuration( managerName );
		AsyncResponseHandlerIntf responseHandler = new DefaultAsyncResponseHandler( config, DEFAULT_URI, false );
		context.setMgr( new DME2Manager( managerName, config ) );
		context.setRequest( new HttpRequest(new URI( DEFAULT_URI ) ) );
		context.getRequest().setLookupUri( DEFAULT_URI );
		context.getRequest().setReadTimeout( 100 );
		context.getRequest().setCharset( "UTF-8" );
		context.getRequest().setResponseHandler( responseHandler );
		DmeUniformResource uniformResource = new DmeUniformResource(config, DEFAULT_URI); 
		context.getRequest().setUniformResource(uniformResource);

		IteratorCreatingAttributes ica = new IteratorCreatingAttributes();
		ica.setConfig( config );
		ica.setEndpointHolders( new ArrayList<DME2EndpointReference>() );

		HttpRequestInvoker invoker = new HttpRequestInvoker( context );
		invoker.createExchange( DEFAULT_HOST, context, EndpointIteratorFactory.getDefaultIterator( ica ) );
		String fileName = HttpRequestInvokerTest.class.getResource( "/cache-types.xml" ).getFile();
		File file = new File( fileName );
		FileReader fr = new FileReader( file );
		String output = "";
		int i;

		while ( ( i = fr.read() ) != -1 ) {
			output += String.copyValueOf( Character.toChars( i ) );
		}

		output = output.substring( 0, output.length() - 1 );
		System.out.println( "SENDING CONTENT: " + output );
		invoker.execute( null, context,
				new FilePayload( HttpRequestInvokerTest.class.getResource( "/cache-types.xml" ).getFile(), false, false ) );
		Object response = responseHandler.getResponse( 1000 );
		assertNotNull( response );
		assertTrue( response instanceof String );
		System.out.println( "GOT RESPONSE: " + response );
		response = ( (String) response ).replaceAll( "\n|\r", "" );
		output = output.replaceAll( "\n|\r", "" );
		System.out.println( response );
		System.out.println( output );
		assertTrue( ( (String) response ).contains( output ) );
	}

	@Test
	public void test_stream_payload() throws Exception {
		String managerName = RandomStringUtils.randomAlphanumeric( 10 );
		RequestContext context = new RequestContext();
		DME2Configuration config = new DME2Configuration( managerName );
		AsyncResponseHandlerIntf responseHandler = new DefaultAsyncResponseHandler( config, DEFAULT_URI, false );
		context.setMgr( new DME2Manager( managerName, config ) );
		context.setRequest( new HttpRequest(new URI( DEFAULT_URI ) ) );
		context.getRequest().setLookupUri( DEFAULT_URI );
		context.getRequest().setReadTimeout( 100 );
		context.getRequest().setCharset( "UTF-8" );
		context.getRequest().setResponseHandler( responseHandler );
		DmeUniformResource uniformResource = new DmeUniformResource(config, DEFAULT_URI); 
		context.getRequest().setUniformResource(uniformResource);

		IteratorCreatingAttributes ica = new IteratorCreatingAttributes();
		ica.setConfig( config );
		ica.setEndpointHolders( new ArrayList<DME2EndpointReference>() );

		HttpRequestInvoker invoker = new HttpRequestInvoker( context );
		invoker.createExchange( DEFAULT_HOST, context, EndpointIteratorFactory.getDefaultIterator( ica ) );

		// String[] charsets = Charset.availableCharsets().keySet().toArray( new String[] { });
		InputStream is = new CharSequenceInputStream( DEFAULT_TEXT_PAYLOAD, "UTF-8" );
		DME2StreamPayload payload = new DME2StreamPayload( is );
		invoker.execute( null, context, payload );
		Object response = responseHandler.getResponse( 1000 );
		System.out.println("response: " + response);

		assertNotNull( response );
		assertTrue( response instanceof String );
		String responseString = (String) response;
		assertTrue( responseString.contains( DEFAULT_TEXT_PAYLOAD ) );
	}

	@Test
	public void test_binary_payload() throws Exception {
		String managerName = RandomStringUtils.randomAlphanumeric( 10 );
		RequestContext context = new RequestContext();
		DME2Configuration config = new DME2Configuration( managerName );
		AsyncResponseHandlerIntf responseHandler = new DefaultAsyncResponseHandler( config, DEFAULT_URI, false );
		context.setMgr( new DME2Manager( managerName, config ) );
		context.setRequest( new HttpRequest(new URI( DEFAULT_URI ) ) );
		context.getRequest().setLookupUri( DEFAULT_URI );
		context.getRequest().setReadTimeout( 100 );
		context.getRequest().setCharset( "UTF-8" );
		context.getRequest().setResponseHandler( responseHandler );

		IteratorCreatingAttributes ica = new IteratorCreatingAttributes();
		ica.setConfig( config );
		ica.setEndpointHolders( new ArrayList<DME2EndpointReference>() );

		int byteBufferLength = RandomUtils.nextInt(1024);
		byte[] bytes = new byte[byteBufferLength];
		new Random().nextBytes( bytes );

		BinaryPayload payload = new BinaryPayload( bytes );
	}

	public static class SimpleServlet extends HttpServlet {

		private static final long serialVersionUID = 1L;

		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			System.out.println("=========Inside the doGet Method of the Custom Servlet===========");
			InputStreamReader ir = new InputStreamReader(req.getInputStream());
			final char[] buffer = new char[8096];
			StringBuilder responseText = new StringBuilder(8096);
			try {
				for (int read = ir.read(buffer, 0, buffer.length); read != -1; read = ir.read(buffer, 0, buffer.length)) {
					responseText.append(buffer, 0, read);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			System.out.println("responseText: " + responseText);
			resp.setContentType("text/html");
			resp.setStatus(HttpServletResponse.SC_OK);
			//resp.getWriter().println("EchoStart\n" + req.getQueryString() + "\nEchoEnd\n" );
			resp.getWriter().println("EchoStart\n" + responseText + "\nEchoEnd\n" );
		}

		protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			System.out.println("=========Inside the doPost Method of the Custom Servlet===========");
			resp.setContentType("text/html");
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println("EchoStart");
			BufferedReader br = req.getReader();
			String line = null;
			while ( ( line = br.readLine() ) != null ) {
				System.out.println( line );
				resp.getWriter().println( line );
			}
			resp.getWriter().println( "EchoEnd" );
			System.out.println("=========Done with the doPost Method of the Custom Servlet===========");
		}
	}

	@Test
	public void test_createExchangeWithException() throws Exception {
		String managerName = RandomStringUtils.randomAlphanumeric( 10 );
		RequestContext context = new RequestContext();
		DME2Configuration config = new DME2Configuration( managerName );
		AsyncResponseHandlerIntf responseHandler = new DefaultAsyncResponseHandler( config, DEFAULT_URI, false );
		context.setMgr( new DME2Manager( managerName, config ) );
		context.setRequest( new HttpRequest(new URI( DEFAULT_URI ) ) );
		context.getRequest().setLookupUri( DEFAULT_URI );
		context.getRequest().setReadTimeout( 10000 );
		context.getRequest().setCharset( "UTF-8" );
		context.getRequest().setResponseHandler( responseHandler );

		IteratorCreatingAttributes ica = new IteratorCreatingAttributes();
		ica.setConfig( config );
		ica.setEndpointHolders( new ArrayList<DME2EndpointReference>() );

		HttpRequestInvoker invoker = new HttpRequestInvoker( context );
		// should throw exception, as DmeUniformResource is not passed in context.request
		boolean exceptionFound = false;
		try {
			invoker.createExchange( DEFAULT_HOST, context, EndpointIteratorFactory.getDefaultIterator( ica ) );
		} catch (Exception e) {
			e.printStackTrace();
			if(e.getMessage().contains("AFT-DME2-0016"))
				exceptionFound = true;
		}
		assertTrue(exceptionFound);
	}

}