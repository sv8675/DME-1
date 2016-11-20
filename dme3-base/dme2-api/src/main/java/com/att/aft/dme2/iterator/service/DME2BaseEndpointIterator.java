package com.att.aft.dme2.iterator.service;

import java.util.Iterator;
import java.util.List;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.bean.EndpointIteratorMXBean;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;

public interface DME2BaseEndpointIterator
		extends Iterator<DME2EndpointReference>, EndpointIteratorMXBean, EndpointIteratorMetricsCollection {
	/**
	 * indicate that the Uri has been tried
	 */
	public void setRouteOffersTried(String str);

	public String getRouteOffersTried();

	public DME2Manager getManager();

	/**
	 * sets the current OrderedEndpointHolder stale
	 */
	public void setStale();

	/**
	 * verified whether the current OrderedEndpointHolder is stale (true) or not
	 * (false)
	 */
	public boolean isStale();

	/**
	 * to check whether all elements are exhausted/iterated
	 */
	public boolean isAllElementsExhausted();

	/**
	 * Returns the current(pointed by the EndpointIterator) RouteOfferHolder for
	 * the given ;
	 * 
	 * @return
	 */
	public DME2RouteOffer getCurrentDME2RouteOffer();

	public DME2EndpointReference getCurrentEndpointReference();

	/**
	 * @return the endpointReferenceList
	 */
	public List<DME2EndpointReference> getEndpointReferenceList();

	/**
	 * @return the minium active endpoints
	 */
	public int getMinActiveEndPoints();
}
