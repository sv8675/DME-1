/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.unit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.iterator.service.IteratorRouteOfferOrderHandler;
import com.att.aft.dme2.iterator.test.util.DME2EndpointTestUtil;
import com.att.aft.dme2.iterator.test.util.DME2RouteofferHolderTestUtil;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.google.common.collect.ListMultimap;

//@PrepareForTest({ EndpointIteratorFactory.class })
@PowerMockIgnore( "javax.management.*" )
public class IteratorOrderingMockTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(IteratorOrderingMockTest.class.getName());
	String DEFAULT_PREFERRED_URL = RandomStringUtils.randomAlphanumeric( 20 );
	Properties defaultProperties; 
	Map<String, Object> paramMap=null;
	SortedMap<Integer, DME2Endpoint[]> unorderedEndpointByRouteOfferSeqMap = null;
	int numberOfTestRecords = 5;
	double[] distance_band_arr = {3.1,2.1,1.2,2.3}; 

	DME2Manager mockManager;
	DME2Manager mockDME2Manager;
	public IteratorOrderingMockTest() {
	}

	@Before
	public void setUpTest() throws Exception {
		LOGGER.info(null, "setUpTest", "start");
		//mockManager = mock ( DME2Manager.class );
		mockDME2Manager  = mock ( DME2Manager.class );
		mockManager = new DME2Manager();

		defaultProperties = new Properties();
		defaultProperties.put("AFT_DME2_PREFERRED_URL", DEFAULT_PREFERRED_URL);
		
		LOGGER.info(null, "setUpTest", "end");
	}

	@Test
	public void testDefaultEndpointOrdering() throws Exception
	{
		LOGGER.info(null, "testDefaultEndpointOrdering", "start");
		IteratorEndpointOrderHandler defaultEndpointOrdering;
		Map<Integer, Map<Double, DME2Endpoint[]>> testRouteOfferEndpointMap=null;
		List<IteratorEndpointOrderHandler> iteratorEndpointOrderHandlers = new ArrayList<IteratorEndpointOrderHandler>();
		
		//record
		mockInit();
		when( mockDME2Manager.getDistanceBands()).thenReturn( distance_band_arr );
		unorderedEndpointByRouteOfferSeqMap = getUnorderedEndpoint();
		defaultEndpointOrdering = EndpointIteratorFactory.getDefaultEndpointOrderHandler(unorderedEndpointByRouteOfferSeqMap, mockDME2Manager);

		//play
		iteratorEndpointOrderHandlers.add(defaultEndpointOrdering);
		
		LOGGER.info(null, "testDefaultEndpointOrdering", "endpoint ordering - start");
		for(IteratorEndpointOrderHandler iteratorEndpointOrderHandler: iteratorEndpointOrderHandlers){
			testRouteOfferEndpointMap = iteratorEndpointOrderHandler.order(testRouteOfferEndpointMap);
		}
		LOGGER.info(null, "testDefaultEndpointOrdering", "endpoint ordering - end");
		
		//verify
		for( Integer routeOfferSeq: testRouteOfferEndpointMap.keySet()){
			LOGGER.info(null, "testDefaultEndpointOrdering", "routeOfferSeq:{}",routeOfferSeq);
			
			double prevDistanceBand = 0;
			for( Double distanceBand: testRouteOfferEndpointMap.get(routeOfferSeq).keySet()){
				//int noOfDME2Endpoint = testRouteOfferEndpointMap.get(routeOfferSeq).get(distanceBand).length;
				//LOGGER.info(null, "testDefaultEndpointOrdering", "Records: "+noOfDME2Endpoint+"--distanceBand:"+distanceBand);

				Assert.assertTrue(prevDistanceBand<distanceBand);
				prevDistanceBand = distanceBand;
			}
		}
		
		verifyInit();
		LOGGER.info(null, "testDefaultEndpointOrdering", "end");
	}
	
	@Test
	public void testPreferredEndpointUrlOrdering() throws Exception
	{
		LOGGER.info(null, "testDefaultEndpointOrdering", "start");
		String preferredURL = "preferredURL";
		IteratorEndpointOrderHandler defaultEndpointOrdering;
		IteratorEndpointOrderHandler preferredEndpointUrlOrdering;
		String context="context";
		String extraContext="extraContext";
		String queryString="queryString";
		String host="host";
		int port = 10;
		String path="path";
		String protocol="protocol";
		Map<Integer, Map<Double, DME2Endpoint[]>> testRouteOfferEndpointMap=null;
		List<IteratorEndpointOrderHandler> iteratorEndpointOrderHandlers = new ArrayList<IteratorEndpointOrderHandler>();
		
		//record
		mockInit();
		when( mockDME2Manager.getDistanceBands()).thenReturn( distance_band_arr );
		
		unorderedEndpointByRouteOfferSeqMap = getUnorderedEndpoint(context,extraContext,queryString, host, port, path, protocol);
		defaultEndpointOrdering = EndpointIteratorFactory.getDefaultEndpointOrderHandler(unorderedEndpointByRouteOfferSeqMap, mockDME2Manager);
		preferredEndpointUrlOrdering = EndpointIteratorFactory.getEndpointPreferredUrlOrderHandler(preferredURL, null);
		//play
		iteratorEndpointOrderHandlers.add(0, defaultEndpointOrdering);
		iteratorEndpointOrderHandlers.add(1, preferredEndpointUrlOrdering);
		
		LOGGER.info(null, "testDefaultEndpointOrdering", "endpoint ordering - start");
		for(IteratorEndpointOrderHandler iteratorEndpointOrderHandler: iteratorEndpointOrderHandlers){
			testRouteOfferEndpointMap = iteratorEndpointOrderHandler.order(testRouteOfferEndpointMap);
		}
		LOGGER.info(null, "testDefaultEndpointOrdering", "endpoint ordering - end");
		
		//verify
		for( Integer routeOfferSeq: testRouteOfferEndpointMap.keySet()){
			LOGGER.info(null, "testDefaultEndpointOrdering", "routeOfferSeq:{}",routeOfferSeq);
			
			double prevDistanceBand = 0;
			for( Double distanceBand: testRouteOfferEndpointMap.get(routeOfferSeq).keySet()){
				Assert.assertTrue(prevDistanceBand<distanceBand);
				prevDistanceBand = distanceBand;
			}
		}
		
		verifyInit();
		LOGGER.info(null, "testDefaultEndpointOrdering", "end");
	}

	@Test
	public void testDefaultRouteOfferOrdering() throws Exception
	{
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "start");
		IteratorRouteOfferOrderHandler defaultRouteOfferOrdering;
		int size=6;
		
		//record
		mockInit();
		List<DME2RouteOffer> routeOfferHolders = getRouteOfferHolders(6);
		Map<Integer, Integer> keyRouteOfferCount = new HashMap<Integer, Integer>();
		
		for(DME2RouteOffer routeOfferHolder: routeOfferHolders){
			if(keyRouteOfferCount.get(routeOfferHolder.getSequence())!=null){
				keyRouteOfferCount.put(routeOfferHolder.getSequence(), keyRouteOfferCount.get(routeOfferHolder.getSequence())+1);
			}else{
				keyRouteOfferCount.put(routeOfferHolder.getSequence(), 1);
			}
		}

		//play
		defaultRouteOfferOrdering = EndpointIteratorFactory.getDefaultRouteOfferOrderHandler(routeOfferHolders);
		
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "endpoint ordering - start");
		ListMultimap<Integer,DME2RouteOffer> routeOfferMap =  defaultRouteOfferOrdering.order(null);
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "endpoint ordering - end");
		
		
		//verify
		for(Integer sequence: routeOfferMap.keySet()){
			Assert.assertEquals(keyRouteOfferCount.get(sequence).intValue(), routeOfferMap.get(sequence).size());
		}
		verifyInit();
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "end");
	}

	@Test
	public void testPreferredRouteOfferOrdering() throws Exception
	{
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "start");
		List<IteratorRouteOfferOrderHandler> iteratorRouteOfferOrderHandlers = new ArrayList<IteratorRouteOfferOrderHandler>();
		IteratorRouteOfferOrderHandler defaultRouteOfferOrdering;
		IteratorRouteOfferOrderHandler preferredRouteOfferOrdering;
		String preferredRouteOffer = "test_preferred_route_offer";
		ListMultimap<Integer,DME2RouteOffer> routeOfferMap =  null;
		int size=6;
		
		//record
		mockInit();
		List<DME2RouteOffer> routeOfferHolders = getRouteOfferHolders(preferredRouteOffer, size);

		//play
		defaultRouteOfferOrdering = EndpointIteratorFactory.getDefaultRouteOfferOrderHandler(routeOfferHolders);
		preferredRouteOfferOrdering = EndpointIteratorFactory.getPreferredRouteOfferOrderHandler(preferredRouteOffer);
		
		iteratorRouteOfferOrderHandlers.add(preferredRouteOfferOrdering);//@index 1
		iteratorRouteOfferOrderHandlers.add(0,defaultRouteOfferOrdering);//@index 0
		
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "endpoint ordering - start");
		for(IteratorRouteOfferOrderHandler iteratorRouteOfferOrderHandler: iteratorRouteOfferOrderHandlers){
			routeOfferMap = iteratorRouteOfferOrderHandler.order(routeOfferMap);
		}
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "endpoint ordering - end");
		
		//verify
		Assert.assertEquals(routeOfferMap.get(-1).get(0).getSearchFilter(), preferredRouteOffer);
		verifyInit();
		LOGGER.info(null, "testDefaultRouteOfferOrdering", "end");
	}

	private SortedMap<Integer, DME2Endpoint[]> getUnorderedEndpoint(){
		SortedMap<Integer, DME2Endpoint[]> unorderedEndpoint = new TreeMap<Integer, DME2Endpoint[]>();

		for(int i=0;i<numberOfTestRecords;i++){
			unorderedEndpoint.put(getNextInt(), DME2EndpointTestUtil.createDefaultDME2Endpoints(numberOfTestRecords));
		}
		
		return unorderedEndpoint;
	}
	private SortedMap<Integer, DME2Endpoint[]> getUnorderedEndpoint(String context,String extraContext,String queryString, String host, int port, String path, String protocol){
		SortedMap<Integer, DME2Endpoint[]> unorderedEndpoint = new TreeMap<Integer, DME2Endpoint[]>();

		for(int i=0;i<numberOfTestRecords;i++){
			unorderedEndpoint.put(getNextInt(), DME2EndpointTestUtil.createDefaultDME2Endpoints(numberOfTestRecords,context,extraContext,queryString, host, port, path, protocol));
		}
		
		return unorderedEndpoint;
	}
	
	private List<DME2RouteOffer> getRouteOfferHolders(int size){
		List<DME2RouteOffer> routeOfferHolders = new ArrayList<DME2RouteOffer>();
		for(int i=0;i<size;++i){
			routeOfferHolders.add(DME2RouteofferHolderTestUtil.getRouteOfferHolder(mockManager));
		}
		return routeOfferHolders;
	}
	private List<DME2RouteOffer> getRouteOfferHolders(final String preferredRouteOffer, int size){
		List<DME2RouteOffer> routeOfferHolders = new ArrayList<DME2RouteOffer>();
		for(int i=0;i<size;++i){
			if(i==size/2){
				routeOfferHolders.add(DME2RouteofferHolderTestUtil.getRouteOfferHolder(mockManager,preferredRouteOffer));
			}else{
				routeOfferHolders.add(DME2RouteofferHolderTestUtil.getRouteOfferHolder(mockManager));			}
		}
		return routeOfferHolders;
	}

	private void mockInit() throws Exception {
		LOGGER.info(null, "mockInit", "start");
		mockInitCtor();
		LOGGER.info(null, "mockInit", "end");
	}
	private void mockInitCtor() throws Exception {
	}
	private void verifyInit() {
		LOGGER.info(null, "verifyInitCtor", "start");
		verifyInitCtor();
		LOGGER.info(null, "verifyInitCtor", "end");
	}
	private void verifyInitCtor() {
		/*try{
		verifyStatic( times(2) );
		EndpointIteratorFactory.getEndpointOrderingHandler(mockManager, defaultProperties, null);
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}*/
	}
	private int getNextInt(){
		int nextInt = ThreadLocalRandom.current().nextInt(400, 4000);
		LOGGER.info(null, "getNextInt", "next int:{}",nextInt);
		return nextInt;
	}
	
	
	public String toURLString(String context,String extraContext, String queryString, String host, int port, String path, String protocol) {
		if (protocol == null) {
		      protocol = "http";
		    }

		    if (path != null && path.startsWith("/")) {
		      return protocol + "://" + host + ":" + port + path;
		    } else {
		      return protocol + "://" + host + ":" + port + "/" + path;
		    }
	}

}
