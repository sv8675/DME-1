/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.handler;

import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class TestIteratorEndpointHandler implements IteratorEndpointOrderHandler {

	@Override
	public Map<Integer, Map<Double, DME2Endpoint[]>> order(Map<Integer, Map<Double, DME2Endpoint[]>> endpointGroupByRouteOfferSequenceMap) throws DME2Exception {
		
		 Map<Double, DME2Endpoint[]> endpointsByDist = endpointGroupByRouteOfferSequenceMap.get(1);
		 
		DME2Endpoint[] endpoints = endpointsByDist.get(endpointsByDist.keySet().toArray()[0]);
		System.out.println("Number of enpoints found=" + endpoints.length);
		
		if (endpoints.length > 0) {
			DME2Endpoint[] newEps = new DME2Endpoint[] {endpoints[0]};
			endpointsByDist.put((Double)endpointsByDist.keySet().toArray()[0], newEps);
		}
			
		return endpointGroupByRouteOfferSequenceMap;
	}

}
