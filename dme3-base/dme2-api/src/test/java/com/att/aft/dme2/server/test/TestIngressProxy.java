/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.test.DME2BaseTestCase;

public class TestIngressProxy extends DME2BaseTestCase {


	@Before
    public void setUp()
	{
		super.setUp();
		System.setProperty("AFT_DME2_PROXY_SKIPEXIT", "true");
		//This test class requires a different platform than others. Im not clear why.
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
	}


	@After
    public void tearDown()
	{
		System.clearProperty("AFT_DME2_GRM_URLS");
		System.clearProperty("platform");
		System.clearProperty("AFT_DME2_PROXY_SKIPEXIT");
	}
	
	
	@Ignore
    @Test
    public void testIngressProxy() throws Exception
	{
		
		//RegistryFsSetup.init();
		
		String[] args = { "-p", "56100" };
		IngressProxyThread pt = new IngressProxyThread(args);
		
		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		int timeout = 120000;

		String urlStr = "http://localhost:56100/service=com.att.aft.DME2CREchoService/version=1/envContext=TEST/routeOffer=BAU/partner=xyz";
		String payload = "Sending ECHOTest";
		
		URL url = new URL(urlStr);
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(timeout);
		conn.setReadTimeout(timeout);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		
		OutputStream out = conn.getOutputStream();
		out.write(payload.getBytes());
		out.flush();
		out.close();

		Thread.sleep(5000);
		
		int respCode = conn.getResponseCode();
		assertEquals(200, respCode);

		InputStream istream = conn.getInputStream();
		String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");

		InputStreamReader input = new InputStreamReader(istream);
		final char[] buffer = new char[8096];
		StringBuilder output = new StringBuilder(8096);
		
		try
		{
			for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0, buffer.length))
			{
				output.append(buffer, 0, read);
			}
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		System.out.println(output);
		
		/*check for some known data from the echo service
		Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
		
		assertTrue(output.toString().contains("PID@HOST"));
		assertTrue(streamheader == null);
	}
	
    
    @Ignore
    @Test
    public void testIngressProxyWithStream() throws Exception
	{
		//
		
		String[] args = { "-p", "5669" };
		IngressProxyThread pt = new IngressProxyThread(args);
		
		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		int timeout = 60000;
		
		//com.att.aft.DME2CREchoService service is not running in SANDBOX-DEV/UAT that's why changed the envContext from UAT to TEST
		String urlStr = "http://localhost:5669/service=com.att.aft.DME2CREchoService/version=1/envContext=TEST/routeOffer=BAU/partner=xyz?DME2.streammode=true";
		String payload = "Sending ECHOTest";
		
		URL url = new URL(urlStr);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(timeout);
		conn.setReadTimeout(timeout);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		
		OutputStream out = conn.getOutputStream();
		out.write(payload.getBytes());
		out.flush();
		out.close();

		Thread.sleep(3000);
		
		int respCode = conn.getResponseCode();
		assertEquals(200, respCode);
		
		String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");
		InputStream istream = conn.getInputStream();

		InputStreamReader input = new InputStreamReader(istream);
		final char[] buffer = new char[8096];
		StringBuilder output = new StringBuilder(8096);
		
		try
		{
			for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0, buffer.length))
			{
				output.append(buffer, 0, read);
			}
		}
		catch (IOException e){
			e.printStackTrace();
		}

		System.out.println("Response Output =" + output);
		System.out.println("Response header  X-DME2_PROXY_STREAM=" + streamheader);
		
		/*check for some known data from the echo service
		Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
		
		assertTrue(output.toString().contains("PID@HOST"));
		assertTrue(streamheader != null);
	}
	
	
    @Test
    public void testIngressProxyWithSubContext() throws Exception
	{
		
		
		DME2Manager mgr = null;
		Properties props = new Properties();
		props.setProperty("platform", "NON-PROD");
		props.setProperty("AFT_LATITUDE", "33");
		props.setProperty("AFT_LONGITUDE", "44");
		props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");

		mgr =new DME2Manager("testIngressProxyWithCompression", new DME2Configuration("IngressProxyWithSubContext", props));
		
		String serviceURI = "/service=com.att.aft.IngressProxyWithSubContext/version=1.0.0/envContext=DEV/routeOffer=DEFAULT";
		
		try
		{
			EchoResponseServlet echoServlet = new EchoResponseServlet(serviceURI, "1");
			String pattern[] = { "/test", "/servletholder" };
			
			DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
			srvHolder.setContextPath("/servletholdertest");

			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add(srvHolder);
			
			// Create service holder for each service registration
			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(serviceURI);
			svcHolder.setManager(mgr);
			svcHolder.setContext("/");
			svcHolder.setServletHolders(shList);
			svcHolder.disableMetricsFilter();

			mgr.getServer().start();
			mgr.bindService(svcHolder);
			
			String[] args = { "-p", "5699" };
			IngressProxyThread pt = new IngressProxyThread(args);
			
			Thread t = new Thread(pt);
			t.setDaemon(true);
			t.start();

			Thread.sleep(10000);
			int timeout = 120000;
			
			String payload = "Sending ECHOTest";
			String clientURI = "http://localhost:5699/service=com.att.aft.IngressProxyWithSubContext/version=1.0.0/envContext=DEV/routeOffer=DEFAULT/partner=abc/subContext=/servletholdertest";
			
			InputStream istream = null;
			URL url = new URL(clientURI);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			OutputStream out = conn.getOutputStream();
			out.write(payload.getBytes());
			out.flush();
			out.close();

			Thread.sleep(2000);
			
			int respCode = conn.getResponseCode();
			if (respCode != 200)
			{
				InputStreamReader err = new InputStreamReader(conn.getErrorStream());
				final char[] buffer = new char[8096];
				StringBuilder output = new StringBuilder(8096);
				
				try
				{
					for (int read = err.read(buffer, 0, buffer.length); read != -1; read = err.read(buffer, 0,
							buffer.length))
					{
						output.append(buffer, 0, read);
					}
				}
				catch (IOException e){
					e.printStackTrace();
				}
				
				System.out.println(output.toString());
			}
			
			assertEquals(200, respCode);
			istream = conn.getInputStream();

			InputStreamReader input = new InputStreamReader(istream);
			final char[] buffer = new char[8096];
			StringBuilder output = new StringBuilder(8096);
			
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0,
						buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){
				e.printStackTrace();
			}
			
			System.out.println(output.toString());
			
			/*check for some known data from the echo service
			Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
			assertTrue(output.toString() != null);
		}catch(Exception ex){
			fail(ex.getMessage());
		}
		finally
		{
		}
	}
	
    
    @Test
    public void testIngressProxyWithCompression() throws Exception
	{
		
		
		DME2Manager mgr = null;	
		DME2ServiceHolder svcHolder = null;
		
		String svcURI = "/service=com.att.aft.IngressProxyWithCompression/version=1.0.0/envContext=DEV/routeOffer=DEFAULT";
		
		try
		{
			Properties props = new Properties();
			props.setProperty("platform", "NON-PROD");
			props.setProperty("AFT_LATITUDE", "33");
			props.setProperty("AFT_LONGITUDE", "44");
			props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
			props.setProperty("DME2.DEBUG", "true");
			props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			
			mgr =new DME2Manager("testIngressProxyWithCompression", new DME2Configuration("testIngressProxyWithCompression", props));

			GZIPEchoResponseServlet echoServlet = new GZIPEchoResponseServlet(svcURI, "1");
			String pattern[] = { "/test", "/servletholder" };
			
			DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
			srvHolder.setContextPath("/servletholdertest");

			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add(srvHolder);
			
			svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(svcURI);
			svcHolder.setManager(mgr);
			svcHolder.setContext("/");
			svcHolder.setServletHolders(shList);
			svcHolder.disableMetricsFilter();

			mgr.getServer().start();
			mgr.bindService(svcHolder);
			
			Thread.sleep(4000);
			
			String[] args = { "-p", "5671" };
			IngressProxyThread pt = new IngressProxyThread(args);
			Thread t = new Thread(pt);
			t.setDaemon(true);
			t.start();

			Thread.sleep(10000);
			int timeout = 120000;
			
			String payload = "Sending ECHOTest";
			String clientURI = "http://localhost:5671/service=com.att.aft.IngressProxyWithCompression/version=1.0.0/envContext=DEV/routeOffer=DEFAULT/partner=abc/subContext=/servletholdertest";
			
			URL url = new URL(clientURI);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Content-Encoding", "gzip");
			conn.setRequestProperty("Accept-Encoding", "gzip");
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(conn.getOutputStream());
			gzipOutputStream.write(payload.getBytes());
			gzipOutputStream.close();

			Thread.sleep(3000);
			int respCode = conn.getResponseCode();
			
			if (respCode != 200)
			{
				InputStreamReader err = new InputStreamReader(conn.getErrorStream());
				final char[] buffer = new char[8096];
				StringBuilder output = new StringBuilder(8096);
				
				try
				{
					for (int read = err.read(buffer, 0, buffer.length); read != -1; read = err.read(buffer, 0,
							buffer.length))
					{
						output.append(buffer, 0, read);
					}
				}
				catch (IOException e){}
				
				System.out.println(output.toString());
			}
			
			assertEquals(200, respCode);
			InputStream istream = conn.getInputStream();

			InputStreamReader input = new InputStreamReader(istream);
			final char[] buffer = new char[8096];
			StringBuilder output = new StringBuilder(8096);
			
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0,
						buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){}
			System.out.println(output);
			
			/*check for some known data from the echo service
			Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
			assertTrue(output.toString() != null);
		}
		finally
		{
			try{mgr.unbindServiceListener(svcURI);}
			catch(Exception e){}
			
			try{mgr.getServer().stop();}
			catch(Exception e){}
		}
	}
	
    
    @Test
    public void testIngressProxyWithReceiveCompression() throws Exception
	{
		

		String svcURI = "/service=com.att.aft.IngressProxyWithReceiveCompression/version=1.0.0/envContext=DEV/routeOffer=DEFAULT";
		DME2ServiceHolder svcHolder = new DME2ServiceHolder();
		DME2Manager mgr = null;
		
		try
		{
			Properties props = new Properties();
			props.setProperty("platform", "NON-PROD");
			props.setProperty("AFT_LATITUDE", "33");
			props.setProperty("AFT_LONGITUDE", "44");
			props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
			props.setProperty("DME2.DEBUG", "true");
			props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			
			mgr =new DME2Manager("testIngressProxyWithReceiveCompression", new DME2Configuration("testIngressProxyWithReceiveCompression", props));

			EchoResponseServlet echoServlet = new EchoResponseServlet(svcURI, "1");
			String pattern[] = { "/test", "/servletholder" };
			
			DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
			srvHolder.setContextPath("/servletholdertest");

			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add(srvHolder);
			
/**			org.eclipse.jetty.servlets.GzipFilter filter = new org.eclipse.jetty.servlets.GzipFilter();
			
			ArrayList<DME2FilterHolder.RequestDispatcherType> dlist = new ArrayList<DME2FilterHolder.RequestDispatcherType>();
			dlist.add(DME2FilterHolder.RequestDispatcherType.REQUEST);
			dlist.add(DME2FilterHolder.RequestDispatcherType.FORWARD);
			dlist.add(DME2FilterHolder.RequestDispatcherType.ASYNC);

			DME2FilterHolder filterHolder = new DME2FilterHolder(filter, "/servletholdertest", EnumSet.copyOf(dlist));
			List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
			flist.add(filterHolder);
*/
			svcHolder.setServiceURI(svcURI);
			svcHolder.setManager(mgr);
			svcHolder.setContext("/");
			svcHolder.setServletHolders(shList);
			svcHolder.disableMetricsFilter();
//			svcHolder.setFilters(flist);

			mgr.getServer().start();
			mgr.bindService(svcHolder);
			
			String[] args = { "-p", "5633" };
			IngressProxyThread pt = new IngressProxyThread(args);
			
			Thread t = new Thread(pt);
			t.setDaemon(true);
			t.start();

			Thread.sleep(10000);
			int timeout = 120000;
			
			String payload = "Sending ECHOTest";
			String clientURI = "http://localhost:5633/service=com.att.aft.IngressProxyWithReceiveCompression/version=1.0.0/envContext=DEV/routeOffer=DEFAULT/partner=abc/subContext=/servletholdertest";
			
			URL url = new URL(clientURI);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Accept-Encoding", "gzip");
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.getOutputStream().write(payload.getBytes());
			conn.getOutputStream().flush();
			conn.getOutputStream().close();
			
			Thread.sleep(3000);

			int respCode = conn.getResponseCode();
			if (respCode != 200)
			{
				InputStreamReader err = new InputStreamReader(conn.getErrorStream());
				final char[] buffer = new char[8096];
				StringBuilder output = new StringBuilder(8096);
				
				try
				{
					for (int read = err.read(buffer, 0, buffer.length); read != -1; read = err.read(buffer, 0,
							buffer.length))
					{
						output.append(buffer, 0, read);
					}
				}
				catch (IOException e){
					e.printStackTrace();
				}
				
				System.out.println(output.toString());
			}
			
			assertEquals(200, respCode);
			InputStream istream = conn.getInputStream();

			InputStreamReader input = new InputStreamReader(istream);
			final char[] buffer = new char[8096];
			StringBuilder output = new StringBuilder(8096);
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0,
						buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){
				e.printStackTrace();
			}
			
			System.out.println(output);
			
			/*check for some known data from the echo service
			Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
			assertTrue(output.toString() != null);
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		finally
		{
			try{mgr.unbindServiceListener(svcURI);}
			catch(Exception e){}
			
			try{mgr.getServer().stop();}
			catch(Exception e){}
		}

	}
	
    
    @Test
    public void testIngressProxyWithCompressionTurnedOff() throws Exception
	{
		
		
		DME2ServiceHolder svcHolder = new DME2ServiceHolder();
		DME2Manager mgr = null;
		String svcURI = "service=com.att.aft.IngressProxyWithCompressionTurnedOff/version=1.0.0/envContext=DEV/routeOffer=DEFAULT";
		
		try
		{
			Properties props = new Properties();
			props.setProperty("platform", "NON-PROD");
			props.setProperty("AFT_LATITUDE", "33");
			props.setProperty("AFT_LONGITUDE", "44");
			props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
			props.setProperty("DME2.DEBUG", "true");
			props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			props.setProperty("AFT_DME2_ALLOW_COMPRESS_ENCODING", "false");
			
			mgr =new DME2Manager("testIngressProxyWithCompressionTurnedOff", new DME2Configuration("testIngressProxyWithCompressionTurnedOff", props));

			GZIPEchoResponseServlet echoServlet = new GZIPEchoResponseServlet(svcURI, "1");
			String pattern[] = { "/test", "/servletholder" };
			
			DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
			srvHolder.setContextPath("/servletholdertest");

			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add(srvHolder);
			
			svcHolder.setServiceURI(svcURI);
			svcHolder.setManager(mgr);
			svcHolder.setContext("/");
			svcHolder.setServletHolders(shList);
			svcHolder.disableMetricsFilter();
			
			mgr.getServer().start();
			mgr.bindService(svcHolder);
			
			String[] args = { "-p", "5667" };
			IngressProxyThread pt = new IngressProxyThread(args);
			
			Thread t = new Thread(pt);
			t.setDaemon(true);
			t.start();

			Thread.sleep(10000);
			int timeout = 120000;
			
			String payload = "Sending ECHOTest";
			String clientURI = "http://localhost:5667/service=com.att.aft.IngressProxyWithCompressionTurnedOff/version=1.0.0/envContext=DEV/routeOffer=DEFAULT/partner=abc/subContext=/servletholdertest";
			
			
			URL url = new URL(clientURI);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Content-Encoding", "gzip");
			conn.setRequestProperty("Accept-Encoding", "gzip");
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(conn.getOutputStream());
			gzipOutputStream.write(payload.getBytes());
			gzipOutputStream.close();
			
			Thread.sleep(3000);

			int respCode = conn.getResponseCode();
			if (respCode != 200)
			{
				InputStreamReader err = new InputStreamReader(conn.getErrorStream());
				final char[] buffer = new char[8096];
				StringBuilder output = new StringBuilder(8096);
				
				try
				{
					for (int read = err.read(buffer, 0, buffer.length); read != -1; read = err.read(buffer, 0,
							buffer.length))
					{
						output.append(buffer, 0, read);
					}
				}
				catch (IOException e){
					e.printStackTrace();
				}
				
				System.out.println(output.toString());
			}
			
			assertEquals(200, respCode);
			InputStream istream = conn.getInputStream();

			InputStreamReader input = new InputStreamReader(istream);
			final char[] buffer = new char[8096];
			StringBuilder output = new StringBuilder(8096);
			
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0,
						buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){
				e.printStackTrace();
			}
			
			System.out.println(output);
			
			/*check for some known data from the echo service
			Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
			assertTrue(output.toString() != null);
		}
		finally
		{
			try{mgr.unbindServiceListener(svcURI);}
			catch(Exception e){}
			
			try{mgr.getServer().stop();}
			catch(Exception e){}
		}
	}

    
    @Test
    public void testIngressProxyError() throws Exception
	{
		
		
		String[] args = { "-p", "5698" };
		IngressProxyThread pt = new IngressProxyThread(args);
		
		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		int timeout = 120000;
		
		String clientURI = "http://localhost:5698/service=com.att.aft.DME2IngressProxy/version=1.0.0/envContext=UAT/routeOffer=DEFAULT?service=com.att.aft.DME2EchoServiceii&version=1.0.0&envContext=TEST&routeOffer=BAU";
		String payload = "Sending ECHOTest";
		
		URL url = new URL(clientURI);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(timeout);
		conn.setReadTimeout(timeout);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		
		OutputStream out = conn.getOutputStream();
		out.write(payload.getBytes());
		out.flush();
		out.close();

		Thread.sleep(3000);
		
		int respCode = conn.getResponseCode();
		assertEquals(500, respCode);
	}

	
    @Ignore
    @Test
    public void testIngressProxy_WithResponseStream() throws Exception
	{

		
		
		String[] args = { "-p", "5642" };
		IngressProxyThread pt = new IngressProxyThread(args);

		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		int timeout = 120000;
		
		//com.att.aft.DME2CREchoService service is not running in SANDBOX-DEV/UAT that's why changed the envContext from UAT to TEST
		String clientURI = "http://localhost:5642/service=com.att.aft.DME2CREchoService/version=1/envContext=TEST/routeOffer=BAU/partner=xyz";
		String payload = "Sending ECHOTest";

		URL url = new URL(clientURI);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(timeout);
		conn.setReadTimeout(timeout);
		conn.setDoInput(true);
		conn.setDoOutput(true);

		OutputStream out = conn.getOutputStream();
		out.write(payload.getBytes());
		out.flush();
		out.close();

		Thread.sleep(3000);
		
		int respCode = conn.getResponseCode();
		assertEquals(200, respCode);

		InputStream istream = conn.getInputStream();
		String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");
		String proxyStreamMode = conn.getHeaderField("X-DME2_PROXY_RESPONSE_STREAM");

		InputStreamReader input = new InputStreamReader(istream);
		final char[] buffer = new char[8096];
		StringBuilder output = new StringBuilder(8096);
		
		try
		{
			for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0,
					buffer.length))
			{
				output.append(buffer, 0, read);
			}
		}
		catch (IOException e){}
		
		System.out.println(output);
		
		/*check for some known data from the echo service
		Sending ECHOTest; Receiver: PID@HOST: 19299@hltd216.hydc.sbc.com*/
		assertTrue(output.toString().contains("PID@HOST"));
		assertTrue(streamheader == null);
		assertTrue(proxyStreamMode != null);
	}
	
	
    @Ignore
    @Test
    public void testIngressProxyResponseStream_WithBinaryData() throws Exception
	{
		
		
		String[] args = { "-p", "56297" };
		IngressProxyThread pt = new IngressProxyThread(args);
		
		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		
		int timeout = 120000;
	
		FileInputStream file = new FileInputStream("src/test/etc/CSPConfigureWebApp.jar");
		byte[] bytes = new byte[256 * 1024];
		file.read(bytes);
		
		InputStream istream = null;
		
		URL url = null;
		//com.att.aft.DME2CREchoService service is not running in SANDBOX-DEV/UAT that's why changed the envContext from UAT to TEST
		url = new URL("http://localhost:56297/service=com.att.aft.DME2CREchoService/version=1/envContext=TEST/routeOffer=BAU/partner=xyz");
		
		HttpURLConnection conn = null;
		conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(timeout);
		conn.setReadTimeout(timeout);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		
		OutputStream out = conn.getOutputStream();
		out.write(bytes);
		out.flush();
		out.close();

		Thread.sleep(3000);
		
		int respCode = conn.getResponseCode();
		assertEquals(200, respCode);
		
		istream = conn.getInputStream();
		
		String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");
		String proxyStreamMode = conn.getHeaderField("X-DME2_PROXY_RESPONSE_STREAM");

		InputStreamReader input = new InputStreamReader(istream);
		final char[] buffer = new char[8096];
		StringBuilder output = new StringBuilder(8096);
		
		try
		{
			for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0,
					buffer.length))
			{
				output.append(buffer, 0, read);
			}
		}
		catch (IOException e){}
		
		System.out.println(output);
		
		assertTrue(output.toString().contains("PID@HOST"));
		assertTrue(streamheader == null);
		assertTrue(proxyStreamMode != null);
	}
    
    @Ignore
    @Test
    public void testIngressProxyUsingNaturalURI() throws Exception
	{
		
		
		String[] args = { "-p", "5995" };
		IngressProxyThread pt = new IngressProxyThread(args);
		
		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		
		DME2Manager mgr =new DME2Manager("testIngressProxyUsingNaturalURI", new DME2Configuration("testIngressProxyUsingNaturalURI", new Properties()));
		String svcURI = "http://TestIngressProxyNaturalURI.aft.att.com/testPath/{id}?version=1.0.0&envContext=LAB&routeOffer=DEFAULT";
		
		try
		{
			// Create service holder for each service registration
			EchoServlet echoServlet = new EchoServlet("service=com.att.aft.TestIngressProxyNaturalURI/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/", "1");
			
			String pattern[] = { "/test" };
			DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
			srvHolder.setContextPath("/testPath/*");
			
			Properties params = new Properties();
			params.setProperty("testContextParam", "TEST_CONTEXT_PARAM");
			
			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add(srvHolder);

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(svcURI);
			svcHolder.setManager(mgr);
			svcHolder.setContext("/testPath");
			svcHolder.setContextParams(params);
			svcHolder.setServletHolders(shList);

			mgr.addService(svcHolder);
			mgr.getServer().start();
			//mgr.bindService(svcHolder);

			Thread.sleep(4000);

			/*//Invoke the above registered FilterTest service by resolving endpoints via SOA registry
			String clientURI = "http://DME2RESOLVE/service=com.att.aft.ServletContextParamTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
			EchoReplyHandler replyHandler = new EchoReplyHandler();
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("testReturnServletContextParam", "test");
			
			
			DME2Client client = new DME2Client(new URI(clientURI), 10000);
			client.setReplyHandler(replyHandler);
			client.addHeader("testReturnServletContextParam", "test");
			client.setHeaders(headers);
			client.setSubContext("/ServletContextParamTest");
			client.setPayload("test");
			
			String reply = (String) client.sendAndWait(10000);
			
			System.out.println("Reply from EchoServlet " + reply);
			assertTrue(reply.contains("contextParam=TEST_CONTEXT_PARAM"));

			String clientURI_2 = "http://ServletContextParamTest1.aft.att.com/ServletContextParamTest?version=1.0.0&envContext=LAB&routeOffer=DEFAULT";
			replyHandler = new EchoReplyHandler();
			
			headers = new HashMap<String, String>();
			headers.put("testReturnServletContextParam", "test");
			
			client = new DME2Client(new URI(clientURI_2), 10000);
			client.setReplyHandler(replyHandler);
			client.setHeaders(headers);
			client.setSubContext("/ServletContextParamTest");
			client.setPayload("test");

			reply = (String) client.sendAndWait(10000);
			System.out.println("Reply from EchoServlet " + reply);
			
			assertTrue(reply.contains("contextParam=TEST_CONTEXT_PARAM"));*/
			
			int timeout = 120000;
			
			String urlStr = "http://localhost:5995/service=TestIngressProxyNaturalURI.aft.att.com/testPath/test/version=1/envContext=LAB/routeOffer=DEFAULT";
			String payload = "Sending ECHOTest";
			
			URL url = new URL(urlStr);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			OutputStream out = conn.getOutputStream();
			out.write(payload.getBytes());
			out.flush();
			out.close();

			Thread.sleep(5000);
			
			int respCode = conn.getResponseCode();
			assertEquals(200, respCode);

			InputStream istream = conn.getInputStream();
			String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");

			InputStreamReader input = new InputStreamReader(istream);
			char[] buffer = new char[8096];
			StringBuilder output = new StringBuilder(8096);
			
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0, buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){}
			
			System.out.println(output);
			
			/*check for some known data from bound servlet */
			
			assertTrue(output.toString().contains("EchoServlet:::1:::service=com.att.aft.TestIngressProxyNaturalURI"));
			assertTrue(streamheader == null);
			
		    urlStr = "http://localhost:5995/service=TestIngressProxyNaturalURI.aft.att.com/testPath/test/version=1/envContext=LAB/routeOffer=DEFAULT?subContext=/testsubContext/abc";
			payload = "Sending ECHOTest";

			// Check for subContext passed to IngressProxy
			 url = new URL(urlStr);
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			out = conn.getOutputStream();
			out.write(payload.getBytes());
			out.flush();
			out.close();

			Thread.sleep(5000);
			
			respCode = conn.getResponseCode();
			assertEquals(500, respCode);

			istream = conn.getErrorStream();
			streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");

			 input = new InputStreamReader(istream);
			 buffer = new char[8096];
			 output = new StringBuilder(8096);
			
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0, buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){}
			
			System.out.println(output);
			
			/*check for some known data from bound servlet */
			
			//assertTrue(output.toString().contains(":onResponseCompleteStatus=404"));
			assertTrue(streamheader == null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(svcURI);
			}
			catch (Exception e)
			{
			}
			
			try
			{
				mgr.getServer().stop();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	
    
	@Ignore
    @Test
    public void testIngressProxyWithAllowHeadersOff() throws Exception
	{
		
		
		String[] args = { "-p", "5996" };
		IngressProxyThread pt = new IngressProxyThread(args);
		
		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		
		DME2Manager mgr =new DME2Manager("testIngressProxyWithAllowHeadersOff", new DME2Configuration("testIngressProxyWithAllowHeadersOff", new Properties()));
		String svcURI = "http://TestIngressProxyWithAllowHeaders.aft.att.com/testPath/{id}?version=1.0.0&envContext=LAB&routeOffer=DEFAULT";
		
		try
		{
			// Create service holder for each service registration
			FailoverServlet echoServlet = new FailoverServlet("service=com.att.aft.TestIngressProxyWithAllowHeaders/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/", "1");
			
			String pattern[] = { "/test" };
			DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
			srvHolder.setContextPath("/testPath/*");
			
			Properties params = new Properties();
			params.setProperty("testContextParam", "TEST_CONTEXT_PARAM");
			
			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add(srvHolder);

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(svcURI);
			svcHolder.setManager(mgr);
			svcHolder.setContext("/testPath");
			svcHolder.setContextParams(params);
			svcHolder.setServletHolders(shList);

			mgr.addService(svcHolder);
			mgr.getServer().start();
			//mgr.bindService(svcHolder);

			Thread.sleep(4000);

			int timeout = 120000;
			
			String urlStr = "http://localhost:5996/service=TestIngressProxyWithAllowHeaders.aft.att.com/testPath/test/version=1/envContext=LAB/routeOffer=DEFAULT";
			String payload = "Sending ECHOTest";
			
			URL url = new URL(urlStr);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			OutputStream out = conn.getOutputStream();
			out.write(payload.getBytes());
			out.flush();
			out.close();

			Thread.sleep(5000);
			
			int respCode = conn.getResponseCode();
			assertEquals(500, respCode);

			InputStream istream = conn.getErrorStream();
			String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");

			InputStreamReader input = new InputStreamReader(istream);
			final char[] buffer = new char[8096];
			StringBuilder output = new StringBuilder(8096);
			
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0, buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){}
			
			System.out.println(output);
			
			/*check for some known data from bound servlet */
			
			assertTrue(output.toString().contains("AFT-DME2-0703"));
			assertTrue(streamheader == null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(svcURI);
			}
			catch (Exception e)
			{
			}
			
			try
			{
				mgr.getServer().stop();
			}
			catch (Exception e)
			{
			}
		}
	}
	
    @Ignore
    @Test
    public void testIngressProxyWithAllowHeadersOn() throws Exception
	{
		
		
		String[] args = { "-p", "5997" };
		IngressProxyThread pt = new IngressProxyThread(args);
		
		Thread t = new Thread(pt);
		t.setDaemon(true);
		t.start();

		Thread.sleep(10000);
		
		DME2Manager mgr =new DME2Manager("testIngressProxyWithAllowHeadersOn", new DME2Configuration("testIngressProxyWithAllowHeadersOn", new Properties()));
		String svcURI = "http://TestIngressProxyWithAllowHeadersOn1.aft.att.com/testPath/{id}?version=1.0.0&envContext=LAB&routeOffer=DEFAULT";
		
		try
		{
			// Create service holder for each service registration
			FailoverServlet echoServlet = new FailoverServlet("service=com.att.aft.TestIngressProxyWithAllowHeadersOn1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/", "1");
			
			String pattern[] = { "/test" };
			DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
			srvHolder.setContextPath("/testPath/*");
			
			Properties params = new Properties();
			params.setProperty("testContextParam", "TEST_CONTEXT_PARAM");
			
			List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
			shList.add(srvHolder);

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(svcURI);
			svcHolder.setManager(mgr);
			svcHolder.setContext("/testPath");
			svcHolder.setContextParams(params);
			svcHolder.setServletHolders(shList);

			mgr.addService(svcHolder);
			mgr.getServer().start();
			//mgr.bindService(svcHolder);

			Thread.sleep(4000);

			int timeout = 120000;
			
			String urlStr = "http://localhost:5997/service=TestIngressProxyWithAllowHeadersOn1.aft.att.com/testPath/test/version=1/envContext=LAB/routeOffer=DEFAULT?DME2.allowhttpcode=true";
			String payload = "Sending ECHOTest";
			
			URL url = new URL(urlStr);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			OutputStream out = conn.getOutputStream();
			out.write(payload.getBytes());
			out.flush();
			out.close();

			Thread.sleep(5000);
			
			int respCode = conn.getResponseCode();
      assertTrue( conn.getResponseMessage().contains("onResponseCompleteStatus=503"));  // there should be a 503 among servers tried
			assertEquals(500, respCode);

			InputStream istream = conn.getErrorStream();
			String streamheader = conn.getHeaderField("X-DME2_PROXY_STREAM");

			InputStreamReader input = new InputStreamReader(istream);
			final char[] buffer = new char[8096];
			StringBuilder output = new StringBuilder(8096);
			
			try
			{
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0, buffer.length))
				{
					output.append(buffer, 0, read);
				}
			}
			catch (IOException e){}
			
			System.out.println(output);
			
			/*check for some known data from bound servlet */
			
			//assertTrue(output.toString().contains("503 Service Unavailable"));
      /* make sure failure is from failover */
      assertTrue(output.toString().contains("AFT-DME2-0703"));
			assertTrue(streamheader == null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(svcURI);
			}
			catch (Exception e)
			{
			}
			
			try
			{
				mgr.getServer().stop();
			}
			catch (Exception e)
			{
			}
		}
	}

}
