package com.att.aft.dme2.iterator.service;

import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

/**
 * This is service for ordering endpoints for the iterator. There is a basic default ordering. And there can be concrete custom ordering as well. <br>
 * Implement this service to create any custom ordering. Then register the custom ordering implementation using the {@link DefaultEndpointIteratorBuilder#addIteratorEndpointOrderHandler(...)}<br>
 * Any number of custom ordering implementation can be added as a chain. The order in which they are added, they would be executed in the same order.
 */
public interface IteratorEndpointOrderHandler {
	/**
	 * This method would be invoked for all the endpoint ordering classes in chain<br>
	 * The data structure would be available to the implementation ordering class in this format <br>
	 * Map<Integer,Map<Double,DME2Endpoint[]>>  <br>
	 * where, 
	 * 	Integer is the RouteOffer sequence
	 * 	Double is the distance band of the endpoint
	 * 	DME2Endpoint is the list of endpoints for this distance band and RouteOffer sequence
	 */
	public Map<Integer,Map<Double,DME2Endpoint[]>> order(Map<Integer,Map<Double,DME2Endpoint[]>> endpointGroupByRouteOfferSequenceMap) throws DME2Exception;
}
