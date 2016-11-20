/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Utils;

public class TestURI 
extends DME2BaseTestCase
{
	
	@Override
	public void setUp()
	{
		super.setUp();
	}
	
	/** verify URI encoding behavior to address possible issue in DME2URI class
	 *  we don't want to urlencode the uri.getQuery() text, or else it will be double-encoded
	 */
	public void testURIEncoding()
	throws Exception
	{
		URI uri = new URI("http://server:123/service?key=word1+word2");  // space represented as +
		
		String query = uri.getQuery();
		assertTrue(query.contains("+"));   // uri.getQuery() retains the +, didn't decode
		assertFalse(query.contains(" "));
		
		String encodedQuery = URLEncoder.encode(query, "UTF-8");
		assertFalse(encodedQuery.contains("+"));
		assertFalse(encodedQuery.contains(" "));
		assertTrue(encodedQuery.toLowerCase().contains("%2b"));  // now double-encoded
	}
	

	@Ignore
	public void testClientRequestWithURLEncoding()
	throws Exception
	{
		
		Properties props = RegistryFsSetup.init();
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		DME2Manager mgr = null;
		String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestClientRequestWithURIEncoding", "1.0.0", "LAB", "DME2_TEST");
		String query = "?key=word1 word2&key2=word3 word4";
		
		try
		{
			
			DME2Configuration config = new DME2Configuration("testClientRequestWithURLEncoding", props);			

			mgr = new DME2Manager("testClientRequestWithURLEncoding", config);
			
		//	mgr = new DME2Manager("testClientRequestWithURLEncoding", props);
			mgr.bindServiceListener(serviceURI, new EchoResponseServlet(serviceURI, "ID_1"));
			
			Thread.sleep(3000);
			String clientURL = String.format("http://DME2RESOLVE%s%s", serviceURI, query);
			
			Request request = new RequestBuilder(new URI(clientURL)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURL).build();

			DME2Client client = new DME2Client(mgr, request);
			
			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
			//client.setPayload("THIS IS A TEST");
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			String resp = (String) client.sendAndWait(payload);
			System.out.println(resp);
			assertTrue(resp.contains("THIS IS A TEST"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				mgr.unbindServiceListener(serviceURI);
				
			}
			catch (Exception e)
			{
			}
		}
	}
	
	  @Ignore	
	public void testClientRequestWithPreEncodedURL()
	throws Exception
	{
		
		Properties props = RegistryFsSetup.init();
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		DME2Manager mgr = null;
		String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestClientRequestWithURIEncoding", "1.0.0", "LAB", "DME2_TEST");
		
		try
		{
			
			DME2Configuration config = new DME2Configuration("testClientRequestWithPreEncodedURL", props);			

			mgr = new DME2Manager("testClientRequestWithPreEncodedURL", config);
			
//			mgr = new DME2Manager("testClientRequestWithPreEncodedURL", props);
			mgr.bindServiceListener(serviceURI, new EchoResponseServlet(serviceURI, "ID_1"));
			
			Thread.sleep(3000);
			String preEncodedClientURL = 
					"http://DME2RESOLVE%2Fservice%3Dcom.att.aft.dme2.test.TestClientRequestWithURIEncoding%2Fversion%3D1.0.0%2FenvContext%3DLAB%2FrouteOffer%3DDME2_TEST%3Fkey%3Dword1%2Bword2%26key2%3Dword3%2Bword4";
			
			Request request = new RequestBuilder(new URI(preEncodedClientURL)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(preEncodedClientURL).build();

			DME2Client client = new DME2Client(mgr, request);
			
			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
			//client.setPayload("THIS IS A TEST");
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			String resp = (String) client.sendAndWait(payload);

			
//			DME2Client client = new DME2Client(mgr, new URL(preEncodedClientURL), 30000, null, false, true);
//			client.setPayload("THIS IS A TEST");
			
//			String resp = (String) client.sendAndWait(30000);
			System.out.println(resp);
			assertTrue(resp.contains("THIS IS A TEST"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				mgr.unbindServiceListener(serviceURI);
				
			}
			catch (Exception e)
			{
			}
		}
	}
	
	  @Ignore	
	public void testClientRequestWithURIEncoding()
	throws Exception
	{
		
		Properties props = RegistryFsSetup.init();
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		DME2Manager mgr = null;
		String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestClientRequestWithURIEncoding", "1.0.0", "LAB", "DME2_TEST");
		String query = "?key=word1 word2&key2=word3 word4";
		
		try
		{

			DME2Configuration config = new DME2Configuration("testClientRequestWithURIEncoding", props);			

			mgr = new DME2Manager("testClientRequestWithURIEncoding", config);
			
//			mgr = new DME2Manager("testClientRequestWithURIEncoding", props);
			mgr.bindServiceListener(serviceURI, new EchoResponseServlet(serviceURI, "ID_1"));
			
			Thread.sleep(3000);

			String clientURL = String.format("http://DME2RESOLVE%s%s", serviceURI, query);
			
			Request request = new RequestBuilder(new URI(clientURL)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURL).build();

			DME2Client client = new DME2Client(mgr, request);
			
			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
			//client.setPayload("THIS IS A TEST");
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			String resp = (String) client.sendAndWait(payload);
			
			
			/*When using a URI with special characters, we must encode the URI before passing it into the DME2Client*/
			
//			DME2Client client = new DME2Client(mgr, new URI(DME2Utils.encodeURIString(clientURI, false)), 30000, null, false, false);
//			client.setPayload("THIS IS A TEST");
			
//			String resp = (String) client.sendAndWait(30000);
			System.out.println(resp);
			assertTrue(resp.contains("THIS IS A TEST"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				mgr.unbindServiceListener(serviceURI);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	  @Ignore	
	public void testClientRequestFailureWithURIEncoding()
	throws Exception
	{
		
		Properties props = RegistryFsSetup.init();
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		DME2Manager mgr = null;
		String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestClientRequestWithURIEncoding", "1.0.0", "LAB", "DME2_TEST");
		String query = "?key=word1 word2&key2=word3 word4";
		
		try
		{
			
			DME2Configuration config = new DME2Configuration("testClientRequestFailureWithURIEncoding", props);			

			mgr = new DME2Manager("testClientRequestFailureWithURIEncoding", config);
			
//			mgr = new DME2Manager("testClientRequestFailureWithURIEncoding", props);
			mgr.bindServiceListener(serviceURI, new EchoResponseServlet(serviceURI, "ID_1"));
			
			Thread.sleep(3000);
			String clientURL = String.format("http://DME2RESOLVE%s%s", serviceURI, query);

			Request request = new RequestBuilder(new URI(clientURL)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURL).build();

			DME2Client client = new DME2Client(mgr, request);
			
			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
			//client.setPayload("THIS IS A TEST");
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			String resp = (String) client.sendAndWait(payload);
			
			
			/*When using a URI with special characters, we must encode the URI before passing it into the DME2Client. 
			 * In this case, we aren't doing that, so we should see get an exception*/
//			DME2Client client = new DME2Client(mgr, new URI(clientURI), 30000, null, false, false);
//			client.setPayload("THIS IS A TEST");
			
//			String resp = (String) client.sendAndWait(30000);
			System.out.println(resp);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			assertTrue(e.getMessage().contains("Illegal character in query"));
		}
		finally{
			try
			{
				mgr.unbindServiceListener(serviceURI);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	  @Ignore
	public void testClientRequestWithPreEncodedURI()
	throws Exception
	{
		
		Properties props = RegistryFsSetup.init();
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		DME2Manager mgr = null;
		String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestClientRequestWithURIEncoding", "1.0.0", "LAB", "DME2_TEST");
		
		try
		{

			DME2Configuration config = new DME2Configuration("testClientRequestWithPreEncodedURI", props);			

			mgr = new DME2Manager("testClientRequestWithPreEncodedURI", config);
			
//			mgr = new DME2Manager("testClientRequestWithPreEncodedURI", props);
			mgr.bindServiceListener(serviceURI, new EchoResponseServlet(serviceURI, "ID_1"));
			
			Thread.sleep(3000);
			String preEncodedClientURI = 
					"http://DME2RESOLVE%2Fservice%3Dcom.att.aft.dme2.test.TestClientRequestWithURIEncoding%2Fversion%3D1.0.0%2FenvContext%3DLAB%2FrouteOffer%3DDME2_TEST%3Fkey%3Dword1%2Bword2%26key2%3Dword3%2Bword4";
			
			Request request = new RequestBuilder(new URI(preEncodedClientURI)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(preEncodedClientURI).build();

			DME2Client client = new DME2Client(mgr, request);
			
			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
			//client.setPayload("THIS IS A TEST");
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			String resp = (String) client.sendAndWait(payload);

//			DME2Client client = new DME2Client(mgr, new URI(preEncodedClientURI), 30000, null, false, true);
//			client.setPayload("THIS IS A TEST");
			
//			String resp = (String) client.sendAndWait(30000);
			System.out.println(resp);
			assertTrue(resp.contains("THIS IS A TEST"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				mgr.unbindServiceListener(serviceURI);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	  @Ignore	  
	public void testClientRequestWithURLEncoding_DirectURL()
	throws Exception
	{	
		Properties props = RegistryFsSetup.init();
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		props.setProperty("AFT_DME2_PORT","54321");
		
		DME2Manager mgr = null;
		String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestClientRequestWithURIEncoding", "1.0.0", "LAB", "DME2_TEST");
		String query = "?key=word1 word2&key2=word3 word4";
		
		try
		{
			
			DME2Configuration config = new DME2Configuration("testClientRequestWithURLEncoding_DirectURL", props);			

			mgr = new DME2Manager("testClientRequestWithURLEncoding_DirectURL", config);
			
//			mgr = new DME2Manager("testClientRequestWithURLEncoding_DirectURL", props);
			mgr.bindServiceListener(serviceURI, new EchoServlet(serviceURI, "ID_1"));
			
			Thread.sleep(3000);
			
			String clientURL = String.format("http://%s:%s%s%s",  InetAddress.getLocalHost().getCanonicalHostName(), "54321", serviceURI, query);
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("testReturnQueryParams", "true");
			
			Request request = new RequestBuilder(new URI(clientURL)).withHttpMethod("POST").withHeaders(headers).withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURL).build();

			DME2Client client = new DME2Client(mgr, request);
			
			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
			//client.setPayload("THIS IS A TEST");
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			String resp = (String) client.sendAndWait(payload);

			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
//			client.setHeaders(headers);
//			client.setPayload("THIS IS A TEST");
			
//			String resp = (String) client.sendAndWait(30000);
			System.out.println(resp);
			assertTrue(resp.contains("key=word1+word2&key2=word3+word4"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				mgr.unbindServiceListener(serviceURI);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	  @Ignore
	public void testClientRequestWithURIEncoding_DirectURI()
	throws Exception
	{	
		Properties props = RegistryFsSetup.init();
		props.setProperty("DME2.DEBUG", "true");
		props.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		props.setProperty("AFT_DME2_PORT","32405");
		
		DME2Manager mgr = null;
		String serviceURI = DME2Utils.buildServiceURIString("com.att.aft.dme2.test.TestClientRequestWithURIEncoding", "1.0.0", "LAB", "DME2_TEST");
		String query = "?key=word1 word2&key2=word3 word4";
		
		try
		{

			DME2Configuration config = new DME2Configuration("testClientRequestWithURIEncoding_DirectURI", props);			

			mgr = new DME2Manager("testClientRequestWithURIEncoding_DirectURI", config);
			
//			mgr = new DME2Manager("testClientRequestWithURIEncoding_DirectURI", props);
			mgr.bindServiceListener(serviceURI, new EchoServlet(serviceURI, "ID_1"));
			
			Thread.sleep(3000);
			
			String clientURI = String.format("http://%s:%s%s%s",  InetAddress.getLocalHost().getCanonicalHostName(), "32405", serviceURI, query);
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("testReturnQueryParams", "true");
			
			Request request = new RequestBuilder(new URI(DME2Utils.encodeURIString(clientURI, false))).withHttpMethod("POST").withHeaders(headers).withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(DME2Utils.encodeURIString(clientURI, false)).build();

			DME2Client client = new DME2Client(mgr, request);
			
			
//			DME2Client client = new DME2Client(mgr, new URL(clientURL), 30000, null, false, false);
			//client.setPayload("THIS IS A TEST");
			DME2Payload payload = new DME2TextPayload("THIS IS A TEST");
			
			String resp = (String) client.sendAndWait(payload);

			
//			DME2Client client = new DME2Client(mgr, new URI(DME2Utils.encodeURIString(clientURI, false)), 30000, null, false, true);
//			client.setHeaders(headers);
//			client.setPayload("THIS IS A TEST");
			
//			String resp = (String) client.sendAndWait(30000);
			System.out.println(resp);
			assertTrue(resp.contains("key=word1+word2&key2=word3+word4"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally{
			try
			{
				mgr.unbindServiceListener(serviceURI);
			}
			catch (Exception e)
			{
			}
		}
	}
}
