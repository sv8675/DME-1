/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.net.URI;

import org.eclipse.jetty.client.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.RequestValidator;
import com.att.aft.dme2.api.http.MessageHeaderUtils;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.DME2EndpointIterator;
import com.att.aft.dme2.iterator.DefaultEndpointIteratorBuilder;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.DmeUniformResource.DmeUrlType;
import com.att.aft.dme2.request.FilePayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;

//@PowerMockIgnore( {"javax.management.*"}  )
//@SuppressStaticInitializationFor({"com.att.aft.dme2.iterator.factory.EndpointIteratorFactory" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({EndpointIteratorFactory.class, RequestValidator.class, MessageHeaderUtils.class})
@SuppressStaticInitializationFor({ "com.att.aft.dme2.server.DME2Manager"})
@PowerMockIgnore("javax.management.*")
//@PrepareForTest(com.att.aft.dme2.api.DefaultRequestProcessor.class)
//@PrepareForTest(DefaultEndpointIteratorBuilder.class)
//@PowerMockIgnore( { "javax.xml.parsers.DocumentBuilder", "javax.xml.parsers.DocumentBuilderFactory", "javax.xml.parsers.FactoryFinder","javax.management.*","com.hazelcast.*", "com.sun.org.apache.xerces.internal.jaxp.*"} )
public class DME2ExchangeClientMetricsEventTest extends DME2BaseTestCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2ExchangeClientMetricsEventTest.class.getName());
	String textFile="src/test/etc/rinfo.txt";
	String textFile1="src/test/etc/rinfo1.txt";
	String binaryFile = "src/test/etc/CSPConfigureWebApp.jar";
	String binaryFile1 = "src/test/etc/lrm-api.jar";
	String invalidFile = "src/test/etc/rinfo10.txt";
	//String binaryFile = "C:\\temp/heap_7624_1.bin";
	DME2EndpointIterator mockDefaultEndpointIterator = null;
	DME2Endpoint defaultEndpoint = null;
	DME2EndpointReference defaultOrderedEndpointHolder = null;
	DME2RouteOffer  defaultRouteOfferHolder = null;
	String service = "/service=com.att.aft.dme2.TestPayloadAsStream/version=1.0.0/envContext=DEV/routeOffer=D1";
	DME2Manager mgr = null;
	String version = "1.0";
	String envContext = "DEV";
	String fqName = "fqName"; 
	Integer sequence = new Integer(1);
	EndpointIteratorFactory mockEndpointIteratorFactory = null;
	HttpClient httpClient=null;
	DME2Configuration defaultConfig = null;
	DmeUrlType dmeUrlType = null;
	DmeUniformResource dmeUniformResource = null;
	DME2Configuration defaultdME2Configuration = null;
	Request jettyRequest = null;
	
    @Before
    public void setUp()
	{
		super.setUp();
		mockDefaultEndpointIterator = mock ( DME2EndpointIterator.class );
		defaultConfig = new DME2Configuration("DME2ExchangeClientMetricsEventTest");
		httpClient = mock ( HttpClient.class ); 
		defaultdME2Configuration = new DME2Configuration("DME2ExchangeClientMetricsEventTest");
		jettyRequest = mock(Request.class);
		
		mockStatic( EndpointIteratorFactory.class );
		//mockStatic( RequestValidator.class );
		mockStatic( MessageHeaderUtils.class );
		//expect(EndpointIteratorFactory.getDefaultIterator(iteratorCreatingAttributes).generateNewId()).andReturn(expectedId);
		try {
			whenNew( DME2EndpointIterator.class ).withAnyArguments().thenReturn(mockDefaultEndpointIterator);
			
		
			defaultEndpoint = new DME2Endpoint(2.3);
			defaultEndpoint.setProtocol("HTTP");
			defaultEndpoint.setPath("path");
			defaultEndpoint.setHost("host");
			defaultEndpoint.setPort(2021);
			
			DME2Endpoint[] endpoints = { defaultEndpoint };
			defaultRouteOfferHolder = new DME2RouteOffer(service, version, envContext, fqName, endpoints, mgr);
			
			defaultOrderedEndpointHolder = new DME2EndpointReference();
			defaultOrderedEndpointHolder.setDistanceBand(2.3);
			defaultOrderedEndpointHolder.setEndpoint(defaultEndpoint);
			defaultOrderedEndpointHolder.setManager(mgr);
			defaultOrderedEndpointHolder.setRouteOffer(defaultRouteOfferHolder);
			defaultOrderedEndpointHolder.setSequence(sequence);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


    @After
    public void tearDown()
	{
		super.tearDown();
	}
    @Test
    public void testRequestMetricsWithTextFileAsPayload() throws Exception
	{
    	org.eclipse.jetty.client.api.Request jettyRequest = mock(org.eclipse.jetty.client.api.Request.class);
    	mgr = mock ( DME2Manager.class );
    	defaultOrderedEndpointHolder.setManager(mgr);
		when( mockDefaultEndpointIterator.hasNext() ).thenReturn(true, true, false);
		when( mockDefaultEndpointIterator.next() ).thenReturn(defaultOrderedEndpointHolder);
		when( mgr.getClient() ).thenReturn( httpClient );	
		when( httpClient.newRequest("HTTP://host:2021/path") ).thenReturn(jettyRequest);	
		
		when( mgr.getConfig() ).thenReturn( defaultdME2Configuration ); 
		when( EndpointIteratorFactory.getDefaultEndpointIteratorBuilder( anyObject() )).thenReturn(new DefaultEndpointIteratorBuilder(defaultConfig));
		when( EndpointIteratorFactory.getDefaultIterator( anyObject() )).thenReturn(mockDefaultEndpointIterator);
		//when( httpClient.newRequest( anyString(),  );
		mgr.bindServiceListener(service, new EchoFileServlet());

		Thread.sleep(1000);
		
		try{
			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestPayloadAsStream/version=1.0.0/envContext=DEV/routeOffer=D1";
			
			Request request = new RequestBuilder(new URI(uriStr)).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(uriStr).build();
			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payload = new FilePayload(textFile, true, false);
			sender.send(payload);
			//String reply = (String) sender.sendAndWait(payload);

			//LOGGER.debug(null, "testTextFileAsPayload", "REPLY = {}", reply);
			
			
		}catch (Exception e)
		{
			e.printStackTrace();
			if(!e.getMessage().startsWith("[AFT-DME2-0707]: [AFT-DME2-0998]:")){
				fail(e.getMessage());
			}
		}
		finally
		{
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
		}

		//verify
		verify(mockDefaultEndpointIterator, atLeast(1)).start( anyObject());
	}
}
