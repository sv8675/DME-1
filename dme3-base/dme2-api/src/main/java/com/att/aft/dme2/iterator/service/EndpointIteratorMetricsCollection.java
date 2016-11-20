package com.att.aft.dme2.iterator.service;

import java.util.Set;

import com.att.aft.dme2.iterator.domain.IteratorMetricsEvent;


/**
 * create an instance of the endpoint metrics collection using the endpointIteratorFactory
 * <p>set the request event handler, reply event handler and the timedout event failure handler. Otherwise, the defaults would be used
 * <p>if start is invoked for the same serviceUri without calling end, then the start time would be reset for that service
 * <p>for start, EventType.REQUEST_EVENT would be created with the same conversion id being unique for every instance of this metrics collection 
 * <p>for end success or failure in happy path, EventType.REPLY_EVENT would be created with the same conversion id being unique for every instance of this metrics collection
 * <p>for end success or failure is not invoked for a given EventType.REQUEST_EVENT within specified time,
 * <p>then EventType.TIMEOUT_EVENT would be created with the same conversion id being unique for every instance of this metrics collection
 * <p>finally, during finalize of this instance, timeout check for every serviceUri requested for this instance would be checked and for every remaining serviceUri EventType.TIMEOUT_EVENT would be created 
 */

public interface EndpointIteratorMetricsCollection {

	/**
	 * marker method to denote the metric collection start for the iterator 
	 * @param iteratorMetricsEvent 
	 */
	public void start(final IteratorMetricsEvent iteratorMetricsEvent);

	/**
	 * marker method to denote the successful end for the iterator metrics collection 
	 * @param iteratorMetricsEvent 
	 */
	public void endSuccess(final IteratorMetricsEvent iteratorMetricsEvent);

	/**
	 * marker method to denote the end in failure for the iterator metrics collection 
	 * @param iteratorMetricsEvent 
	 */
	public void endFailure(final IteratorMetricsEvent iteratorMetricsEvent);
	/**
	 * get set of active services for the iterator metrics collection 
	 * @param iteratorMetricsEvent 
	 */
	public Set<String> getActiveServices();
	
	/**
	 * set the unique ID to be used to raise the metrics event, once set the same Id would be used to raise any subsequent event on this instance
	 * @param uniqueId
	 */
	public void setMetricsConversationId(final String uniqueId);
}
