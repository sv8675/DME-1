package com.att.aft.dme2.handler;

import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
/*
 * This interface will give the next fail over end point.
 * The method exposed takes in end point iterator and if retry for the url is required or not.
 * Implemenation class shall iterate over the iterator and give the next fail over end point.
 */
public interface FailoverEndpoint {
	public DME2EndpointReference getNextFailoverEndpoint(DME2BaseEndpointIterator iterator, boolean retryCurrentlUrl);
}
