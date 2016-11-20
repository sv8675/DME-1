/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;


public class TestDME2PayloadCompression extends DME2BaseTestCase
{
	//TODO: Add test case where there are two services and the filter is enabled for both

  @Before
	public void setUp()
	{
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33");
		System.setProperty("AFT_LONGITUDE", "44");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("DME2.DEBUG", "true");
    super.setUp();
	}
	
	//1
  @Ignore
	public void testPayloadCompression_ResponseSizeGreaterThanConfiguredThreshold() throws Exception {
		DME2Manager mgr = null;
		String service = null;
		//System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.dme2.api.TestDME2PayloadCompression.testPayloadCompression_ResponseSizeGreaterThanConfiguredThreshold");
		System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
		System.setProperty("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1000");

		EchoResponseServlet ers = null;
		try {
			service = "/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Properties props = RegistryFsSetup.init();
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
			props.put("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1000");
			//props.put("DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES", "application/javascript");
			//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
			//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");

			DME2Configuration config = new DME2Configuration("testPayloadCompression1", props);
			mgr = new DME2Manager("testPayloadCompression1", config);
			
//			mgr = new DME2Manager("testPayloadCompression", props);
			mgr.disableMetricsFilter();
			ers = new EchoResponseServlet(service, "testPayloadCompression1");
			mgr.bindServiceListener(service, ers);
			
			Thread.sleep(5000);
			
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			EchoReplyHandler handler = new EchoReplyHandler();
			
			//Request request = new RequestBuilder(mgr.getClient(), new HttpConversation(), new URI(uriStr)).withHeader("Accept-Encoding", "gzip").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			Request request = new RequestBuilder(new URI(uriStr)).withHeader("Accept-Encoding", "gzip").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
//			Payload payload = new TextPayload("THIS IS A TEST");
			sender.setResponseHandlers(handler);

//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 100000);
//			sender.setReplyHandler(handler);
//			sender.addHeader("accept-encoding", "gzip");
			DME2TextPayload payload = new DME2TextPayload("Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal. " +
					"Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war." +
					" We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated that that nation might live. It is altogether fitting and proper that we should do this. " +
					"But, are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. " +
					"The world will little note, nor long remember what we say here, but it can never forget what they did here. It are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. " +
					"It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated devotion -- " +
					"that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated" +
					" -- and are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln");
//			Thread.sleep(1000000);
			sender.send(payload);
			
			String reply = handler.getResponse(60000);
			System.err.println(reply);
			
			Map<String, String> headers = handler.getResponseHeaders();
			String contentEndcoding = headers.get("Content-Encoding");
			assertTrue(contentEndcoding.equals("gzip"));
			
		} finally {
			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e)	{

			}
      if ( ers != null ) {
        System.out.println( "Destroying response servlet" );
        ers.destroy();
      }
		}
	}
	
	//3
	public void testPayloadCompression_WithLargePayLoad() throws Exception
	{
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.dme2.api.TestDME2PayloadCompression.testPayloadCompression_ResponseSizeGreaterThanConfiguredThreshold");
		System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
		
		try
		{
			service = "service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Properties props = new Properties();
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
			props.put("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1500");
			props.put("DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES", "application/javascript");
			//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
			//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");
			
			
			DME2Configuration config = new DME2Configuration("testPayloadCompression2", props);
			mgr = new DME2Manager("testPayloadCompression2", config);
//			mgr = new DME2Manager("testPayloadCompression", props);
			mgr.bindServiceListener(service, new EchoResponseServlet(service, "testPayloadCompression2"));
					
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			EchoReplyHandler handler = new EchoReplyHandler();
			
			String text = "Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal. " +
					"Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war." +
					" We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this. " +
					"But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. " +
					"The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. " +
					"It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- " +
					"that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom" +
					" -- and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln";
			
			StringBuilder builder = new StringBuilder();
			
			for(int i = 0; i < 5000; i++)
			{
				builder.append(text);
			}
					
			Request request = new RequestBuilder(new URI(uriStr)).withHeader("Accept-Encoding", "gzip").withHeader("AFT_DME2_REQ_TRACE_ON", "true").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			sender.setResponseHandlers(handler);

//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 100000);
//			sender.setReplyHandler(handler);
//			sender.addHeader("accept-encoding", "gzip");
//			sender.setPayload(builder.toString());
			sender.send(new DME2TextPayload(builder.toString()));
			
			String reply = handler.getResponse(60000);
			System.err.println(reply);
			
			Map<String, String> headers = handler.getResponseHeaders();
			String contentEndcoding = headers.get("Content-Encoding");
			assertTrue(contentEndcoding.equals("gzip"));
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(service);
			}
			catch (Exception e)
			{

			}
		}
	}
	
	
	//this test runs fine when run individually
	  @Ignore
	public void testPayloadCompression_WithoutEncodingHeaderSetOnClient() throws Exception
	{
		DME2Manager mgr = null;
		String service = null;
		//System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.dme2.api.TestDME2PayloadCompression.testPayloadCompression_ResponseSizeGreaterThanConfiguredThreshold");
		System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
		System.setProperty("DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES", "application/javascript");
		
		try
		{
			service = "/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Properties props = RegistryFsSetup.init();
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
			props.put("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1500");
			props.put("DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES", "application/javascript");
			//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
			//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");
			
			DME2Configuration config = new DME2Configuration("testPayloadCompression_WithoutEncodingHeaderSetOnClient", props);
			mgr = new DME2Manager("testPayloadCompression_WithoutEncodingHeaderSetOnClient", config);
			
//			mgr = new DME2Manager("testPayloadCompression", props);
			mgr.bindServiceListener(service, new EchoResponseServlet(service, "testPayloadCompression_WithoutEncodingHeaderSetOnClient"));
			Thread.sleep(10000);			
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			EchoReplyHandler handler = new EchoReplyHandler();
			
			Request request = new RequestBuilder(new URI(uriStr)).withHeader("AFT_DME2_REQ_TRACE_ON", "true").withReadTimeout(20000)
					.withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			sender.setResponseHandlers(handler);

//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 100000);
//			sender.setReplyHandler(handler);
			
			DME2TextPayload payload = new DME2TextPayload("Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal. " +
					"Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war." +
					" We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this. " +
					"But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. " +
					"The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. " +
					"It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- " +
					"that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom" +
					" -- and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln");
			
			sender.send(payload);
			
			String reply = handler.getResponse(120000);
			System.err.println(reply);
			
			Map<String, String> headers = handler.getResponseHeaders();
			assertTrue(reply.contains("EchoServlet:::testPayloadCompression_WithoutEncodingHeaderSetOnClient:::/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY;Request=Four score and seven years"));
			String contentEndcoding = headers.get("Content-Encoding");
			System.err.println("contentEndcoding : "+ contentEndcoding);
			assertTrue(contentEndcoding != null); //Contenting-Enccoding is set by the Jetty so checking for content-Encoding not null
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(service);
			}
			catch (Exception e)
			{

			}
		}
	}
	
	
	  @Ignore
	public void testPayloadCompression_ResponseSizeLessThanConfiguredThreshold() throws Exception
	{
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.dme2.api.TestDME2PayloadCompression.testPayloadCompression_ResponseSizeLessThanConfiguredThreshold");
		System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
		
		try
		{
			service = "/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Properties props = RegistryFsSetup.init();
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
			props.put("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1500");
			//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
			//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");
			
			DME2Configuration config = new DME2Configuration("testPayloadCompression4", props);
			mgr = new DME2Manager("testPayloadCompression4", config);
			
//			mgr = new DME2Manager("testPayloadCompression", props);
			mgr.disableMetricsFilter();
			mgr.disableMetrics();
			mgr.bindServiceListener(service, new EchoResponseServlet(service, "testPayloadCompression"));
					
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			EchoReplyHandler handler = new EchoReplyHandler();
			
			Request request = new RequestBuilder(new URI(uriStr)).withHeader("accept-encoding", "gzip").withHeader("AFT_DME2_REQ_TRACE_ON", "true").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			sender.setResponseHandlers(handler);

//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 100000);
//			sender.setReplyHandler(handler);
//			sender.addHeader("accept-encoding", "gzip");
			DME2TextPayload payload = new DME2TextPayload("Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.");
			sender.send(payload);
			
			String reply = handler.getResponse(60000);
			System.err.println(reply);
			
			Map<String, String> headers = handler.getResponseHeaders();
			String contentEndcoding = headers.get("Content-Encoding");
			assertTrue(contentEndcoding == null);
			
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(service);
			}
			catch (Exception e)
			{

			}
		}
	}
	
	//this test runs success individually
	  @Ignore
	public void testPayloadCompression_WithGZipFilterDisabled() throws Exception
	{
		DME2Manager mgr = null;
		String service = null;
		System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.dme2.api.TestDME2PayloadCompression.testPayloadCompression_WithGZipFilterDisabled");
		System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "false");
		
		try
		{
			service = "/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Properties props = RegistryFsSetup.init();
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "false");
			props.put("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1500");
			//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
			//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");
			
			DME2Configuration config = new DME2Configuration("testPayloadCompression5", props);
			mgr = new DME2Manager("testPayloadCompression5", config);
			
//			mgr = new DME2Manager("testPayloadCompression", props);
			mgr.disableMetricsFilter();
			mgr.disableMetrics();
			mgr.bindServiceListener(service, new EchoResponseServlet(service, "testPayloadCompression"));
					
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			EchoReplyHandler handler = new EchoReplyHandler();

			Request request = new RequestBuilder(new URI(uriStr)).withHeader("accept-encoding", "gzip").withHeader("AFT_DME2_REQ_TRACE_ON", "true").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			sender.setResponseHandlers(handler);
			
//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 100000);
//			sender.setReplyHandler(handler);
//			sender.addHeader("accept-encoding", "gzip");
			DME2TextPayload payload = new DME2TextPayload("Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.");
			sender.send(payload);
			
			String reply = handler.getResponse(60000);
			System.err.println(reply);
			
			Map<String, String> headers = handler.getResponseHeaders();
			String contentEndcoding = headers.get("Content-Encoding");
			
			assertTrue(reply.contains("EchoServlet:::testPayloadCompression:::/service=com.att.aft.dme2.test.TestPayloadCompression/version=1.0.0/envContext=LAB/routeOffer=PRIMARY;Request=Four score and seven years"));
			assertEquals(mgr.getConfig().getProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH"), "false");
			assertTrue(contentEndcoding == null);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			try
			{
				mgr.unbindServiceListener(service);
			}
			catch (Exception e)
			{

			}
		}
	}
	
	//2
	  @Ignore
	public void testPayloadCompression_DisableCompressionForSingleService() throws Exception {
		/* Start two services and provide "disableCompression" query string for one.*/
		DME2Manager mgr = null;
		String service = null;
		//System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.dme2.api.TestDME2PayloadCompression.testPayloadCompression_DisableCompressionForSingleService");
		System.setProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
		
		try {
			/*Start the first service with compression enabled.*/
			service = "/service=com.att.aft.dme2.test.TestPayloadCompression6/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			Properties props = RegistryFsSetup.init();
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
			props.put("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1500");
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
			//props.put("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
			//props.put("AFT_DME2_CLIENT_PROXY_PORT", "9999");

			DME2Configuration config = new DME2Configuration("testPayloadCompression6", props);
			mgr = new DME2Manager("testPayloadCompression6", config);
			
			
//			mgr = new DME2Manager("testPayloadCompression", props);
			mgr.disableMetricsFilter();
			mgr.disableMetrics();
			mgr.bindServiceListener(service, new EchoResponseServlet(service, "testPayloadCompression6"));

			String service2 = "/service=com.att.aft.dme2.test.TestPayloadCompression7/version=1.0.0/envContext=LAB/routeOffer=PRIMARY?disableCompression=true&clientSupportedVersions=1.1,*";

			DME2Configuration config2 = new DME2Configuration("testPayloadCompression7", props);
			DME2Manager mgr2 = new DME2Manager("testPayloadCompression7", config2);

			mgr2.bindServiceListener(service2, new EchoResponseServlet(service2, "testPayloadCompression7"));
			
			Thread.sleep(10000);
			
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestPayloadCompression6/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			EchoReplyHandler handler = new EchoReplyHandler();

			Request request = new RequestBuilder(new URI(uriStr)).withHeader("User-Agent", "777").withHeader("accept-encoding", "gzip").withHeader("AFT_DME2_REQ_TRACE_ON", "true").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			sender.setResponseHandlers(handler);
			
//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 100000);
//			sender.setReplyHandler(handler);
//			sender.addHeader("accept-encoding", "gzip");
			DME2TextPayload payload = new DME2TextPayload("Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal. " +
					"Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war." +
					" We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this. " +
					"But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. " +
					"The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. " +
					"It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- " +
					"that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom" +
					" -- and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln");
			sender.send(payload);
			
			String reply = handler.getResponse(60000);
			System.err.println(reply);
			
			Map<String, String> headers = handler.getResponseHeaders();
			String contentEndcoding = headers.get("Content-Encoding");
			assertTrue(contentEndcoding.equals("gzip"));
			///service=com.att.aft.dme2.test.TestPayloadCompression7/version=1.0.0/envContext=LAB/routeOffer=PRIMARY
			
			/*Start the second service with disableCompression=true*/
			
			Thread.sleep(10000);
			String uriStr2 = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestPayloadCompression7/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
			
			EchoReplyHandler handler2 = new EchoReplyHandler();

			
			Request request2 = new RequestBuilder(new URI(uriStr2)).withHeader("accept-encoding", "gzip").withHeader("AFT_DME2_REQ_TRACE_ON", "true").withReadTimeout(20000).withReturnResponseAsBytes(false).withLookupURL(uriStr2).build();
			DME2Client sender2 = new DME2Client(mgr2, request2);
			sender2.setResponseHandlers(handler2);
			
//			DME2Client sender2 = new DME2Client(mgr, new URI(uriStr), 100000);
//			sender2.setReplyHandler(handler2);
//			sender2.addHeader("accept-encoding", "gzip");
			DME2TextPayload payload2 = new DME2TextPayload("Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal. " +
					"Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war." +
					" We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this. " +
					"But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. " +
					"The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. " +
					"It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- " +
					"that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom" +
					" -- and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln");
			sender2.send(payload2);
			
			String reply2 = handler2.getResponse(100000);
			System.err.println(reply2);
			
			Map<String, String> headers2 = handler2.getResponseHeaders();
			String contentEndcoding2 = headers2.get("Content-Encoding");
			
			assertTrue(reply2.contains("EchoServlet:::testPayloadCompression7:::/service=com.att.aft.dme2.test.TestPayloadCompression7/version=1.0.0/envContext=LAB/routeOffer=PRIMARY?disableCompression=true&clientSupportedVersions=1.1,*;Request=Four score and seven years ago"));
			assertEquals(mgr2.getConfig().getProperty("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH"), "true");
			assertTrue(contentEndcoding2 == null);
		} finally {
			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {

			}
		}
	}
	
	
}
