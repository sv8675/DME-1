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
import java.util.UUID;

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
import com.att.aft.dme2.event.DME2EventManager;
import com.att.aft.dme2.iterator.DefaultEndpointIteratorBuilder;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.IteratorCreatingAttributes;
import com.att.aft.dme2.iterator.domain.IteratorMetricsEvent;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.metrics.DefaultEndpointIteratorMetricsCollection;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.test.util.DME2EndpointTestUtil;
import com.att.aft.dme2.iterator.test.util.DME2RouteofferHolderTestUtil;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;

@PowerMockIgnore( {"javax.management.*"}  )
@SuppressStaticInitializationFor({"com.att.aft.dme2.event.DME2EventManager", "com.att.aft.dme2.server.DME2Manager", "com.att.aft.dme2.config.DME2Configuration", "com.att.aft.dme2.request.DmeUniformResource" })
@RunWith( PowerMockRunner.class )
public class EndpointIteratorMetricsCollectionMockTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointIteratorMetricsCollectionMockTest.class.getName());
	private static final String DEFAULT_SERVICE = RandomStringUtils.randomAlphanumeric( 30 );
	private static final String DEFAULT_VERSION = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 5 );
	private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 10 );
	private static final String DEFAULT_FQ_NAME = RandomStringUtils.randomAlphanumeric( 5 );
	private static final Double DEFAULT_DISTANCE_BAND = RandomUtils.nextDouble();
	private static final Integer DEFAULT_ORDER_ROUTEOFFER_SEDQUENCE = Integer.valueOf(RandomUtils.nextInt(10));
	private static final String DEFAULT_MIN_ACTIVE_ENDPOINTS = null;
	private static final String DEFAULT_MANAGER_NAME = RandomStringUtils.randomAlphanumeric( 20 );
	private static final String AFT_DME2_INTERFACE_SERVER_ROLE = "SERVER";
	private static String DME2_INTERFACE_HTTP_PROTOCOL = "HTTP";
	private static final int MAX_THREAD_COUNT_TIMEOUT_CHECKER = 10;
	private static final long EVENT_TIMEOUT_TOTAL_WAITING_MS = 40000;
	private static final long EVENT_CHECKER_SCHEDULER_DELAY_MS = 5000;
	private static final String DEFAULT_EVENT_ROLE = "test_client";
	private static final String DEFAULT_EVENT_PROTOCOL = "test_http";
	
	private Map<String, String> DEAFULT_QUERY_PARAMS_MAP = null;
	private DME2BaseEndpointIterator DEFAULT_ENDPOINT_ITERATOR = null;
	private int AFT_DME2_EVENT_QUEUE_SIZE = 10000;
	private boolean AFT_DME2_LOG_REJECTED_EVENTS = true;
	private DME2Manager mockManager;
	private DefaultEndpointIteratorBuilder mockDefaultEndpointIteratorBuilder;
	private DmeUniformResource mockUniformResource;
	private DME2Endpoint DEFAULT_DME2ENDPOINT;
	//private DME2Configuration mockConfiguration;
	private DME2Configuration mockConstantsConfig;
	private DME2Configuration config;
	private DME2EventManager eventManager;

	@Before
	public void setUpTest() throws Exception {
		try{
			LOGGER.info(null,  "setUpTest", "start");
			//mockConfiguration = mock( DME2Configuration.class );
			config = new DME2Configuration("EndpointIteratorMetricsCollectionMockTest");
			mockConstantsConfig = mock( DME2Configuration.class );
			mockUniformResource = mock( DmeUniformResource.class );
			mockDefaultEndpointIteratorBuilder = mock ( DefaultEndpointIteratorBuilder.class );
			mockManager = mock( DME2Manager.class );
			eventManager = mock( DME2EventManager.class );
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}
	
	private String generateUniqueTransactionReference(){
		StringBuffer uniqueReference = new StringBuffer();

		uniqueReference.append(this.hashCode());
		uniqueReference.append("-");
		uniqueReference.append(UUID.randomUUID().toString());

		//transactionReference = uniqueReference.toString();
		
		return uniqueReference.toString();
		
	}
	
	private IteratorMetricsEvent createIteratorMetricsEvent(final DME2Endpoint endpoint){
		IteratorMetricsEvent iteratorMetricsEvent = new IteratorMetricsEvent();
		iteratorMetricsEvent.setServiceUri(endpoint.getServiceName());
		iteratorMetricsEvent.setConversationId(generateUniqueTransactionReference());
		iteratorMetricsEvent.setRole(DEFAULT_EVENT_ROLE);
		iteratorMetricsEvent.setProtocol(DEFAULT_EVENT_PROTOCOL);

		return iteratorMetricsEvent;
	}
	
	@Test
	public void testDME2EndpointIteratorMetricsCollectionReplyEvents() throws Exception
	{
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","service name 4:{}",service_4);
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

		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","mocking complete");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","going to instatiate manager");

		DME2BaseEndpointIterator endpointIterator = DEFAULT_ENDPOINT_ITERATOR;

		//play
		try {
			LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","going to iterate the iterator");
			
			if(endpointIterator.hasNext())
			{
				do{
					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","getting endpoint");
					DME2EndpointReference ref = endpointIterator.next();
					DME2Endpoint endpoint = ref.getEndpoint();
					
					//start metrics
					endpointIterator.start(createIteratorMetricsEvent(endpoint));

					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","endpoint service name:{}",endpoint.getServiceName());
					servicesToValidate.add(endpoint.getServiceName());
					
					//end metrics
					endpointIterator.endSuccess(createIteratorMetricsEvent(endpoint));
					
				}while(endpointIterator.hasNext());
			}else{
				LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","no endpoints found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//verify
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionReplyEvents","start verifying");
		assertTrue(endpointIterator.getActiveServices().isEmpty());
		assertEquals(numberOfServices, servicesToValidate.size());
		for(int i=0;i<numberOfServices;i++){
			assertTrue(servicesToValidate.contains(serviceNames[i]));
		}
		
		verify(mockManager, atLeastOnce()).isUrlInStaleList( anyString() );
		verifyCtor();
	}

	@Test
	public void testDME2EndpointIteratorMetricsCollectionEndFailureEvents() throws Exception
	{
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","service name 4:{}",service_4);
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

		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","mocking complete");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","going to instatiate manager");

		DME2BaseEndpointIterator endpointIterator = DEFAULT_ENDPOINT_ITERATOR;

		//play
		try {
			LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","going to iterate the iterator");
			
			int i = 0;
			if(endpointIterator.hasNext())
			{
				do{
					DME2EndpointReference ref = endpointIterator.next();
					DME2Endpoint endpoint = ref.getEndpoint();
					try{
						LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","getting endpoint");
						//start collecting iterator metrics
						endpointIterator.start(createIteratorMetricsEvent(endpoint));
	
	
						LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","endpoint service name:{}",endpoint.getServiceName());
						
						
						servicesToValidate.add(endpoint.getServiceName());
						if(i!=0 && i%2==0){
							throw new Exception("mocking error scenario");
						}
						i++;
						
						//metrics collection end in success
						endpointIterator.endSuccess(createIteratorMetricsEvent(endpoint));
					}catch(Exception e){
						//metrics collection end in failure
						endpointIterator.endFailure(createIteratorMetricsEvent(endpoint));
					}
				}while(endpointIterator.hasNext());
			}else{
				LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","no endpoints found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//verify
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollectionEndFailureEvents","start verifying");
		assertTrue(endpointIterator.getActiveServices().isEmpty());
		assertEquals(numberOfServices, servicesToValidate.size());
		for(int i=0;i<numberOfServices;i++){
			assertTrue(servicesToValidate.contains(serviceNames[i]));
		}
		
		verify(mockManager, atLeastOnce()).isUrlInStaleList( anyString() );
		verifyCtor();
	}
	
	@Test
	public void testDME2EndpointIteratorMetricsCollectionStartCancelEvents() throws Exception
	{
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","service name 4:"+service_4);
		String[] serviceNames = {service_1, service_2, service_3, service_4};
		DME2Configuration config = new DME2Configuration();

		List<String> servicesToValidate = new ArrayList<String>();

		int numberOfServices = 4;

		//record
		mockInit();

		DEAFULT_QUERY_PARAMS_MAP = new HashMap<String, String>();
		DEFAULT_ENDPOINT_ITERATOR = getNewIterator(numberOfServices, serviceNames);

		when( mockManager.isUrlInStaleList( anyString() )).thenReturn( false );
		whenNew( DefaultEndpointIteratorBuilder.class ).withAnyArguments().thenReturn(mockDefaultEndpointIteratorBuilder);
		when( mockDefaultEndpointIteratorBuilder.build() ).thenReturn(DEFAULT_ENDPOINT_ITERATOR);

		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","mocking complete");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","going to instatiate manager");

		DME2BaseEndpointIterator endpointIterator = DEFAULT_ENDPOINT_ITERATOR;

		DME2UnitTestUtil.setFinalStatic( DefaultEndpointIteratorMetricsCollection.class.getDeclaredField( "DISABLE_METRICS" ), null, false );

		//play
		try {
			LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","going to iterate the iterator");
			
			if(endpointIterator.hasNext())
			{
				do{
					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","getting endpoint");
					DME2EndpointReference ref = endpointIterator.next();
					DME2Endpoint endpoint = ref.getEndpoint();
					endpointIterator.start(createIteratorMetricsEvent(endpoint));


					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","endpoint service name:"+endpoint.getServiceName());
					servicesToValidate.add(endpoint.getServiceName());
				}while(endpointIterator.hasNext());
			}else{
				LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","no endpoints found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//verify
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","start verifying");
		assertEquals(1, endpointIterator.getActiveServices().size());
		
		verify(mockManager, atLeastOnce()).isUrlInStaleList( anyString() );
		verifyCtor();
	}
	@Test
	public void testDME2EndpointIteratorMetricsCollectionTimedoutEvents() throws Exception
	{
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","service name 4:{}",service_4);
		String[] serviceNames = {service_1, service_2, service_3, service_4};
		DME2Configuration config = new DME2Configuration();

		List<String> servicesToValidate = new ArrayList<String>();

		int numberOfServices = 4;

		//record
		mockInit();

		DEAFULT_QUERY_PARAMS_MAP = new HashMap<String, String>();
		DEFAULT_ENDPOINT_ITERATOR = getNewIterator(numberOfServices, serviceNames);

		when( mockManager.isUrlInStaleList( anyString() )).thenReturn( false );
		whenNew( DefaultEndpointIteratorBuilder.class ).withAnyArguments().thenReturn(mockDefaultEndpointIteratorBuilder);
		when( mockDefaultEndpointIteratorBuilder.build() ).thenReturn(DEFAULT_ENDPOINT_ITERATOR);

		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","mocking complete");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","going to instatiate manager");

		DME2BaseEndpointIterator endpointIterator = DEFAULT_ENDPOINT_ITERATOR;

		//play
		try {
			LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","going to iterate the iterator");
			
			if(endpointIterator.hasNext())
			{
				do{
					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","getting endpoint");
					DME2EndpointReference ref = endpointIterator.next();
					DME2Endpoint endpoint = ref.getEndpoint();
					endpointIterator.start(createIteratorMetricsEvent(endpoint));


					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","endpoint service name:"+endpoint.getServiceName());
					servicesToValidate.add(endpoint.getServiceName());
				}while(endpointIterator.hasNext());
			}else{
				LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","no endpoints found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Thread.sleep(config.getLong(DME2Constants.Iterator.EVENT_TIMEOUT_TOTAL_WAITING_MS)+10000);//ensuring all related events are timedout
		
		//verify
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","start verifying");
		assertTrue(endpointIterator.getActiveServices().isEmpty());
		assertEquals(numberOfServices, servicesToValidate.size());
		for(int i=0;i<numberOfServices;i++){
			assertTrue(servicesToValidate.contains(serviceNames[i]));
		}
		
		verify(mockManager, atLeastOnce()).isUrlInStaleList( anyString() );
		verifyCtor();
	}
	
	@Test
	public void testDME2EndpointIteratorMetricsCollectionClientTimedoutEvents() throws Exception
	{
		String service_1 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A1");
		String service_2 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "A2");
		String service_3 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B1");
		String service_4 = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", "1.0.0", "LAB", "B2");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","service name 4:"+service_4);
		String[] serviceNames = {service_1, service_2, service_3, service_4};
		DME2Configuration config = new DME2Configuration();

		List<String> servicesToValidate = new ArrayList<String>();

		int numberOfServices = 4;

		//record
		mockInit();

		DEAFULT_QUERY_PARAMS_MAP = new HashMap<String, String>();
		DEFAULT_ENDPOINT_ITERATOR = getNewIterator(numberOfServices, serviceNames);

		when( mockManager.isUrlInStaleList( anyString() )).thenReturn( false );
		whenNew( DefaultEndpointIteratorBuilder.class ).withAnyArguments().thenReturn(mockDefaultEndpointIteratorBuilder);
		when( mockDefaultEndpointIteratorBuilder.build() ).thenReturn(DEFAULT_ENDPOINT_ITERATOR);

		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","mocking complete");
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","going to instatiate manager");

		DME2BaseEndpointIterator endpointIterator = DEFAULT_ENDPOINT_ITERATOR;

		//play
		try {
			LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","going to iterate the iterator");
			
			if(endpointIterator.hasNext())
			{
				do{
					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","getting endpoint");
					DME2EndpointReference ref = endpointIterator.next();
					DME2Endpoint endpoint = ref.getEndpoint();
					IteratorMetricsEvent iteratorMetricsEvent = createIteratorMetricsEvent(endpoint);
					iteratorMetricsEvent.setTimeOutMS(100);
					endpointIterator.start(iteratorMetricsEvent);


					LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","endpoint service name:{}",endpoint.getServiceName());
					servicesToValidate.add(endpoint.getServiceName());
				}while(endpointIterator.hasNext());
			}else{
				LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","no endpoints found");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Thread.sleep(config.getLong(DME2Constants.Iterator.EVENT_TIMEOUT_TOTAL_WAITING_MS)+10000);//ensuring all related events are timedout
		
		//verify
		LOGGER.info(null,"testDME2EndpointIteratorMetricsCollection","start verifying");
		assertTrue(endpointIterator.getActiveServices().isEmpty());
		assertEquals(numberOfServices, servicesToValidate.size());
		for(int i=0;i<numberOfServices;i++){
			assertTrue(servicesToValidate.contains(serviceNames[i]));
		}
		
		verify(mockManager, atLeastOnce()).isUrlInStaleList( anyString() );
		verifyCtor();
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
		
		//DME2UnitTestUtil.setFinalStatic( DME2Constants.class.getDeclaredField( "config" ), mockConstantsConfig );

		when( mockConstantsConfig.getInt("AFT_DME2_EVENT_QUEUE_SIZE") ).thenReturn(AFT_DME2_EVENT_QUEUE_SIZE);
		when( mockConstantsConfig.getBoolean("AFT_DME2_LOG_REJECTED_EVENTS") ).thenReturn(AFT_DME2_LOG_REJECTED_EVENTS);
		when( mockConstantsConfig.getProperty("AFT_DME2_INTERFACE_SERVER_ROLE") ).thenReturn(AFT_DME2_INTERFACE_SERVER_ROLE);
		when( mockConstantsConfig.getProperty("DME2_INTERFACE_HTTP_PROTOCOL") ).thenReturn(DME2_INTERFACE_HTTP_PROTOCOL);
		
		when( mockConstantsConfig.getInt("MAX_THREAD_COUNT_TIMEOUT_CHECKER") ).thenReturn(MAX_THREAD_COUNT_TIMEOUT_CHECKER);
		when( mockConstantsConfig.getLong("EVENT_TIMEOUT_TOTAL_WAITING_MS") ).thenReturn(EVENT_TIMEOUT_TOTAL_WAITING_MS);
		when( mockConstantsConfig.getLong("EVENT_CHECKER_SCHEDULER_DELAY_MS") ).thenReturn(EVENT_CHECKER_SCHEDULER_DELAY_MS);
		
		//whenNew( DME2EventManager.class ).withAnyArguments().thenReturn( eventManager );
		
		DME2UnitTestUtil.setFinalStatic( DME2EventManager.class.getDeclaredField( "INSTANCE" ), null, eventManager );
		
		//when( DME2EventManager.getInstance() ).thenReturn( eventManager );
		when( mockManager.getName() ).thenReturn( DEFAULT_MANAGER_NAME );
		when( mockUniformResource.getService() ).thenReturn( DEFAULT_SERVICE );
		when( mockUniformResource.getVersion() ).thenReturn( DEFAULT_VERSION );
		when( mockUniformResource.getEnvContext() ).thenReturn( DEFAULT_ENV_CONTEXT );
		when( mockUniformResource.getRouteOffer() ).thenReturn( DEFAULT_ROUTE_OFFER );
		when( mockUniformResource.getQueryParamsMap() ).thenReturn( DEAFULT_QUERY_PARAMS_MAP );
	}

	private void verifyCtor() throws Exception {
	}

	private void mockInitConfig() {
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
		iteratorCreatingAttributes.setConfig(config);
		endpointIterator = EndpointIteratorFactory.getDefaultIterator(iteratorCreatingAttributes);
		LOGGER.info(null,"getNewIterator", "exit");
		return endpointIterator;
	}

	// Test for properly garbage collecting elements
}