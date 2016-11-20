/**
 * 
 */
package com.att.aft.dme2.iterator.service;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.google.common.collect.ListMultimap;

/**
 * This is service for ordering route offers for the iterator. There can be concrete custom ordering of route offers. <br>
 * Implement this service to create any custom route offer ordering. Then register the custom ordering implementation using the {@link DefaultEndpointIteratorBuilder#addIteratorRouteOfferHandler(...)}<br>
 * Any number of custom route offer ordering implementation can be added as a chain. <br>
 * The order in which they are added, they would be executed in the same order.
 */
public interface IteratorRouteOfferOrderHandler {
	/**
	 * This method would be invoked for all the routeoffer ordering classes added in chain<br>
	 * The data structure would be available to the implementation ordering class in this format <br>
	 * ListMultimap<Integer, RouteOfferHolder>  <br>
	 * where, 
	 * 	Integer is the RouteOffer sequence
	 * 	RouteOfferHolder is the holder of the RouteOffer along with some additional attributes
	 */
	public ListMultimap<Integer, DME2RouteOffer> order(ListMultimap<Integer, DME2RouteOffer> routeOfferGrpBySequenceMap) throws DME2Exception;
}
