/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.DefaultEndpointIteratorBuilder;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.IteratorCreatingAttributes;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.test.util.DME2EndpointTestUtil;
import com.att.aft.dme2.iterator.test.util.DME2RouteofferHolderTestUtil;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2URIUtils;

@PowerMockIgnore( {"javax.management.*"}  )
@SuppressStaticInitializationFor({"com.att.aft.dme2.server.DME2Manager", "com.att.aft.dme2.config.DME2Configuration", "com.att.aft.dme2.request.DmeUniformResource" })
@RunWith( PowerMockRunner.class )
public class IteratorTraversalMockTest{

	private static final Logger LOGGER = LoggerFactory.getLogger(IteratorTraversalMockTest.class.getName());
	private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 30 );
	private static final String DEFAULT_VERSION = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 10 );
	private static final String DEFAULT_FQ_NAME = RandomStringUtils.randomAlphanumeric( 5 );
	private static final Double DEFAULT_DISTANCE_BAND = RandomUtils.nextDouble();
	private static final Integer DEFAULT_ORDER_ROUTEOFFER_SEDQUENCE = Integer.valueOf(RandomUtils.nextInt(10));
	private static final String DEFAULT_MIN_ACTIVE_ENDPOINTS = "2";
	private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
	private Map<String, String> DEAFULT_QUERY_PARAMS_MAP = null;
	private DME2BaseEndpointIterator DEFAULT_ENDPOINT_ITERATOR = null;

	private DME2Manager mockManager;
	private DefaultEndpointIteratorBuilder mockDefaultEndpointIteratorBuilder;
	private DmeUniformResource mockUniformResource;
	private DME2Endpoint DEFAULT_DME2ENDPOINT;
	DME2Configuration mockConfiguration;

	@Before
	public void setUpTest() throws Exception {
		try{
			LOGGER.info(null,  "setUpTest", "start");
			//mockConfiguration = mock( DME2Configuration.class );
			mockConfiguration = new DME2Configuration("IteratorTraversalMockTest");
			mockUniformResource = mock( DmeUniformResource.class );
			mockDefaultEndpointIteratorBuilder = mock ( DefaultEndpointIteratorBuilder.class );
			mockManager = mock( DME2Manager.class );
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}
	@Test
	public void testMockCtor() throws Exception
	{
		//record
		mockInit();
		
		//play
		LOGGER.info(null, "testMockCtor", "DME2Manager name={}",mockManager.getName());
		LOGGER.info(null, "testMockCtor", "service name={}",mockUniformResource.getService());
		
		//verify
		verifyCtor();
	}

	/**
	 * assume the endpoints are provided correctly; it is going to be verified whether the iterator is iterated and all the records are retreived correctly
	 * @throws Exception
	 */
	@Test
	public void testDME2EndpointIterator_OrderEndpointsNotStaleNoActiveQueryParam() throws Exception
	{
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		LOGGER.info(null,"testDME2EndpointIterator_OrderEndpointsNotStaleNoActiveQueryParam","service name 4:{}",service_4);
		String[] serviceNames = {service_1, service_2, service_3, service_4};

		List<String> servicesToValidate = new ArrayList<String>();

		int numberOfServices = 4;

		//record
		mockInit();

		DEAFULT_QUERY_PARAMS_MAP = new HashMap<String, String>();
		DEFAULT_ENDPOINT_ITERATOR = getNewIterator(numberOfServices, serviceNames);

		when( mockManager.isUrlInStaleList( anyString() )).thenReturn( false );
		whenNew( DefaultEndpointIteratorBuilder.class ).withAnyArguments().thenReturn(mockDefaultEndpointIteratorBuilder);
		when( mockDefaultEndpointIteratorBuilder.build() ).thenReturn(DEFAULT_ENDPOINT_ITERATOR);

		LOGGER.info(null,"testDME2EndpointIterator_OrderEndpointsNotStaleNoActiveQueryParam","mocking complete");
		LOGGER.info(null,"testDME2EndpointIterator","going to instatiate manager");

		DME2BaseEndpointIterator endpointIterator = DEFAULT_ENDPOINT_ITERATOR;

		//play
		try {
			LOGGER.info(null,"testDME2EndpointIterator","going to iterate the iterator");

			if(endpointIterator.hasNext())
			{
				do{
					LOGGER.info(null,"testDME2EndpointIterator","getting endpoint");
					DME2EndpointReference ref = endpointIterator.next();
					DME2Endpoint endpoint = ref.getEndpoint();

					LOGGER.info(null,"testDME2EndpointIterator","endpoint service name:{}",endpoint.getServiceName());
					servicesToValidate.add(endpoint.getServiceName());
				}while(endpointIterator.hasNext());
			}else{
				LOGGER.info(null,"testDME2EndpointIterator","no endpoints found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		//verify
		LOGGER.info(null,"testDME2EndpointIterator","start verifying");
		assertEquals(numberOfServices, servicesToValidate.size());
		for(int i=0;i<numberOfServices;i++){
			assertTrue(servicesToValidate.contains(serviceNames[i]));
		}
		
		verify(mockManager, atLeastOnce()).isUrlInStaleList( anyString() );
		verifyCtor();
	}

	@Test
	public void testDME2EndpointIterator_OrderEndpointsNotStaleWithActiveQueryParam() throws Exception
	{
		DEAFULT_QUERY_PARAMS_MAP = new HashMap<String, String>();
		DEAFULT_QUERY_PARAMS_MAP.put("minActiveEndPoints", DEFAULT_MIN_ACTIVE_ENDPOINTS);

		when( mockManager.isUrlInStaleList( anyString() )).thenReturn( false );

		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		LOGGER.info(null,"testDME2EndpointIterator_OrderEndpointsNotStaleNoActiveQueryParam","service name 4:{}",service_4);
		String[] serviceNames = {service_1, service_2, service_3, service_4};

		List<String> servicesToValidate = new ArrayList<String>();

		int numberOfServices = 4;

		//record
		mockInit();

		DEFAULT_ENDPOINT_ITERATOR = getNewIterator(numberOfServices, serviceNames);

		when( mockManager.isUrlInStaleList( anyString() )).thenReturn( false );
		whenNew( DefaultEndpointIteratorBuilder.class ).withAnyArguments().thenReturn(mockDefaultEndpointIteratorBuilder);
		when( mockDefaultEndpointIteratorBuilder.build() ).thenReturn(DEFAULT_ENDPOINT_ITERATOR);

		LOGGER.info(null,"testDME2EndpointIterator_OrderEndpointsNotStaleNoActiveQueryParam","mocking complete");
		LOGGER.info(null,"testDME2EndpointIterator","going to instatiate manager");

		DME2BaseEndpointIterator endpointIterator = DEFAULT_ENDPOINT_ITERATOR;

		//play
		try {
			LOGGER.info(null,"testDME2EndpointIterator","going to iterate the iterator");

			if(endpointIterator.hasNext())
			{
				do{
					LOGGER.info(null,"testDME2EndpointIterator","getting endpoint");
					DME2EndpointReference ref = endpointIterator.next();
					DME2Endpoint endpoint = ref.getEndpoint();

					LOGGER.info(null,"testDME2EndpointIterator","endpoint service name:{}",endpoint.getServiceName());
					servicesToValidate.add(endpoint.getServiceName());
				}while(endpointIterator.hasNext());
			}else{
				LOGGER.info(null,"testDME2EndpointIterator","no endpoints found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		//verify
		LOGGER.info(null,"testDME2EndpointIterator","start verifying");
		assertEquals(numberOfServices, servicesToValidate.size());
		for(int i=0;i<numberOfServices;i++){
			assertTrue(servicesToValidate.contains(serviceNames[i]));
		}
		
		verify(mockManager, atLeastOnce()).isUrlInStaleList( anyString() );
		verifyCtor();
		DEAFULT_QUERY_PARAMS_MAP.remove("minActiveEndPoints");
	}

	private void mockInit() throws Exception {
		mockInitCtor();
		mockInitConfig();
	}

	private void mockInitCtor() throws Exception {

		whenNew( DME2Manager.class ).withAnyArguments().thenReturn( mockManager );
		//whenNew( DME2Configuration.class ).withAnyArguments().thenReturn( mockConfiguration );
		//whenNew( DME2SimpleServlet.class ).withNoArguments().thenReturn( mockServlet );
		whenNew( DmeUniformResource.class ).withAnyArguments().thenReturn( mockUniformResource );

		when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
		when( mockUniformResource.getService() ).thenReturn( DEFAULT_SERVICE );
		when( mockUniformResource.getVersion() ).thenReturn( DEFAULT_VERSION );
		when( mockUniformResource.getEnvContext() ).thenReturn( DEFAULT_ENV_CONTEXT );
		when( mockUniformResource.getRouteOffer() ).thenReturn( DEFAULT_ROUTE_OFFER );
		when( mockUniformResource.getQueryParamsMap() ).thenReturn( DEAFULT_QUERY_PARAMS_MAP );
	}

	private void verifyCtor() throws Exception {
		//verifyNew( DME2Manager.class ).withArguments( DEFAULT_MANAGER_NAME );
		//verify( mockManager, atLeastOnce() ).getName();
		//verify( mockConfiguration ).getProperty( "Test" );
		//verifyNew( DME2Configuration.class ).withArguments( DEFAULT_MANAGER_NAME );
		//verifyNew( DME2SimpleServlet.class ).withArguments( mockServlet );
		//verify( mockUniformResource ).getService();
	}

	private void mockInitConfig() {
		/*when( mockRegistry.getConfig() ).thenReturn( mockConfig );
		when( mockConfig.getInt( "DME2_SEP_LEASE_RENEW_FREQUENCY_MS" )).thenReturn( 1000000 );
		when( mockConfig.getInt( "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS" )).thenReturn( 1000000 );
		when( mockConfig.getInt( "DME2_UNUSED_ENDPOINT_REMOVAL_DELAY" ) ).thenReturn( 1000000 );
		when( mockConfig.getInt( "DME2_PERSIST_CACHED_ENDPOINTS_FREQUENCY_MS" ) ).thenReturn( 1000000 );*/
	}

	public DME2BaseEndpointIterator getNewIterator(int size, String[] serviceNames) throws DME2Exception{
		LOGGER.info(null,"getNewIterator", "enter");
		DME2BaseEndpointIterator endpointIterator = null;

		List<DME2EndpointReference> endpointHolders = new ArrayList<DME2EndpointReference>();
		DME2EndpointReference orderedEndpointHolder = null;

		for(int i=0;i<size;++i)
		{
			DEFAULT_DME2ENDPOINT = DME2EndpointTestUtil.createDefaultDME2Endpoint(serviceNames[i]);
			DEFAULT_DME2ENDPOINT.setDmeUniformResource(mockUniformResource);
			orderedEndpointHolder = new DME2EndpointReference()
			.setDistanceBand(DEFAULT_DISTANCE_BAND)
			.setEndpoint(DEFAULT_DME2ENDPOINT)
			.setManager(mockManager)
			.setRouteOffer(DME2RouteofferHolderTestUtil.getRouteOfferHolder(mockManager))
			.setSequence(DEFAULT_ORDER_ROUTEOFFER_SEDQUENCE);
			endpointHolders.add(orderedEndpointHolder);
		}
		LOGGER.info(null,"getNewIterator", "get default iterator");
		LOGGER.info(null,"getNewIterator", "mockManager:{}",mockManager);
		LOGGER.info(null,"getNewIterator", "endpointHolders:{}",endpointHolders);
		LOGGER.info(null,"getNewIterator", "mockUniformResource:{}",mockUniformResource.getQueryParamsMap());
		LOGGER.info(null,"getNewIterator", "DEAFULT_QUERY_PARAMS_MAP.minActiveEndPoints:{}"+ DEAFULT_QUERY_PARAMS_MAP.get("minActiveEndPoints"));
		
		IteratorCreatingAttributes iteratorCreatingAttributes = new IteratorCreatingAttributes();
		iteratorCreatingAttributes.setManager(mockManager);
		iteratorCreatingAttributes.setEndpointHolders(endpointHolders);
		iteratorCreatingAttributes.setQueryParamMinActiveEndPoint(DEAFULT_QUERY_PARAMS_MAP.get("minActiveEndPoints"));
		iteratorCreatingAttributes.setConfig(mockConfiguration);
		endpointIterator = EndpointIteratorFactory.getDefaultIterator(iteratorCreatingAttributes);
		
		LOGGER.info(null,"getNewIterator", "exit");
		return endpointIterator;
	}
}
