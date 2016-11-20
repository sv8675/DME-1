/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.net.URI;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.server.test.TestConstants;

public class DME2CoreTests {
	
	private DmeUniformResource mockUniformResource;
	
	@Before
	public void setUp() {
		System.out.println("\n in setUp \n");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		
		try {
			mockInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void mockInit() throws Exception {
		try {
			whenNew( DmeUniformResource.class ).withParameterTypes( URI.class  ).withArguments( anyObject() ).thenReturn( mockUniformResource );
			whenNew( DmeUniformResource.class ).withParameterTypes( String.class  ).withArguments( anyObject() ).thenReturn( mockUniformResource );
			whenNew( DmeUniformResource.class ).withParameterTypes( URL.class  ).withArguments( anyObject() ).thenReturn( mockUniformResource );

			mockUniformResource = mock( DmeUniformResource.class );
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testDME2ClientWithoutURIForException() throws Exception {
		DME2Manager mgr = new DME2Manager();
		Request request = new RequestBuilder(new URI("")).withHttpMethod("Http").withReadTimeout(30000).withReturnResponseAsBytes(false).build();
		try {
			DME2Client client = new DME2Client(mgr, request);
		} catch (DME2Exception ex) {
			Assert.assertTrue(true);
			return;
		}
		Assert.fail();
	}
	
	@Test
  @Ignore
	public void testDME2ClientSend() throws Exception {
		DME2Manager mgr = new DME2Manager();
		String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints/version=1.0.0/envContext=LAB/partner=DME2_TEST";
		Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("Http").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();
		DME2Client client = new DME2Client(mgr, request);
		DME2Payload payload = new DME2TextPayload("Testing testDME2ClientSend");
		client.send(payload);
	}
	
	@Test
  @Ignore
	public void testDME2ClientSendAndWait() throws Exception {
		DME2Manager mgr = new DME2Manager();
		String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints/version=1.0.0/envContext=LAB/partner=DME2_TEST";
		Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("Http").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();
		DME2Client client = new DME2Client(mgr, request);
		DME2Payload payload = new DME2TextPayload("Testing testDME2ClientSendAndWait");
		String response = (String) client.sendAndWait(payload);
		System.out.println("response: " + response);
		Assert.assertNotNull(response);
	}
	
	@Test
  @Ignore
	public void testCustomResponseHandler() throws Exception {
		DME2Manager mgr = new DME2Manager();
		String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints/version=1.0.0/envContext=LAB/partner=DME2_TEST";
		Request request = new RequestBuilder(new URI(clientURI)).withHttpMethod("Http").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI).build();
		CustomResponseHandler customResponseHandler = new CustomResponseHandler(clientURI); 
		request.setResponseHandler(customResponseHandler);
		DME2Client client = new DME2Client(mgr, request);
		DME2Payload payload = new DME2TextPayload("Testing testCustomResponseHandler");
		String response = (String) client.sendAndWait(payload);
		System.out.println("response: " + response);
		Assert.assertNotNull(response);
		Assert.assertEquals("CustomResponseHandler.getResponse testing", response);
	}
	
	
}
