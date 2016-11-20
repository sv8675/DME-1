package com.att.aft.dme2.iterator.handler.order;

import java.util.Map;
import java.util.SortedMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.helper.EndpointsByDistance;
import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class DefaultEndpointOrderHandler implements IteratorEndpointOrderHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEndpointOrderHandler.class.getName());
	private DME2Manager dMEManager = null;
	private SortedMap<Integer, DME2Endpoint[]> unorderedEndpointByRouteOfferSeqMap = null;
	
	public DefaultEndpointOrderHandler(final SortedMap<Integer, DME2Endpoint[]> unorderedEndpointByRouteOfferSeqMap, final DME2Manager manager) {
		this.dMEManager = manager;
		this.unorderedEndpointByRouteOfferSeqMap = unorderedEndpointByRouteOfferSeqMap;
	}
	
	@Override
	public Map<Integer, Map<Double, DME2Endpoint[]>> order(Map<Integer, Map<Double, DME2Endpoint[]>> orderedEndpointByRouteOfferSeqMap) throws DME2Exception {
		LOGGER.debug(null, "order", "start");
		
		long startTime = System.currentTimeMillis();
		LOGGER.debug(null, "order", "get endpoint map:[{}]",unorderedEndpointByRouteOfferSeqMap!=null?unorderedEndpointByRouteOfferSeqMap:null);

		if(unorderedEndpointByRouteOfferSeqMap!=null){
			orderedEndpointByRouteOfferSeqMap = EndpointsByDistance.organize(unorderedEndpointByRouteOfferSeqMap, dMEManager);
			LOGGER.debug(null, "orderEndpoints", "endpointsGroupedBySequence:{}", orderedEndpointByRouteOfferSeqMap);
		}
		
		LOGGER.debug(null, "order", "end;elapsed time:{}",(System.currentTimeMillis()-startTime));
		return orderedEndpointByRouteOfferSeqMap;
	}
}