package com.att.aft.dme2.iterator.service;

import java.util.List;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.event.EventProcessor;

/**
 * 
 * decorator to be used for building the iterator instance
 *
 */
public interface EndpointIteratorBuilder
{
	/**
	 * set the chain of routeOfferOrderHandler which would be used to ordering of the route offers
	 * @param iteratorRouteOfferOrderHandler iteratorRouteOfferOrderHandler to be added to order the route offers
	 * @return 
	 */
	public EndpointIteratorBuilder addIteratorRouteOfferOrderHandler(IteratorRouteOfferOrderHandler iteratorRouteOfferOrderHandler);

	/**
	 * set the chain of endpointorderhandlers which would be used to process the list of the endpoints
	 * @param iteratorEndpointOrderHandler iteratorEndpointOrderHandler to be added in the chain of IteratorEndpointOrderHandlers to order the endpoints as desired
	 * @return
	 */
	public EndpointIteratorBuilder addIteratorEndpointOrderHandler(IteratorEndpointOrderHandler iteratorEndpointOrderHandler);
	
	/**
	 * set the chain of endpointorderhandlers which would be used to process the list of the endpoints
	 * @param iteratorEndpointOrderHandlers
	 * @return
	 */
	public EndpointIteratorBuilder setIteratorEndpointOrderHandlers(List<IteratorEndpointOrderHandler> iteratorEndpointOrderHandlers);
	
	/**
	 * set the chain of routeOfferOrderHandler which would be used to ordering of the route offers
	 * @param iteratorRouteOfferOrderHandlers
	 * @return
	 */
	public EndpointIteratorBuilder setIteratorRouteOfferOrderHandlers(List<IteratorRouteOfferOrderHandler> iteratorRouteOfferOrderHandlers);

	/**
	 * set the service Url string to be used by the endpoint iterator to build the DME2UniformResource
	 * @param initialUrl
	 * @return 
	 */
	public EndpointIteratorBuilder setServiceURI(String initialUrl);
	/**
	 * properties to be used for building the default endpoint ordering
	 * @param props
	 * @return
	 */
	public EndpointIteratorBuilder setProps(Properties props);
	
	/**
	 * set the urlformatter
	 * @param urlFormatter instance as set by the user
	 * @return EndpointIteratorBuilder instance
	 */
	public EndpointIteratorBuilder setUrlFormatter(DME2EndpointURLFormatter urlFormatter);
	/**
	 * sets the DME2Manager for this user
	 * @param manager DME2Manager for this user
	 * @return EndpointIteratorBuilder instance
	 */
	public EndpointIteratorBuilder setManager(DME2Manager manager);
	
	/**
	 * sets the request event processor to be used to handle the iterator metrics request event
	 * @param requestEventProcessor request event processor
	 * @return EndpointIteratorBuilder instance
	 */
	public EndpointIteratorBuilder setRequestEventProcessor(EventProcessor requestEventProcessor);
	/**
	 * sets the reply event processor to be used to handle the iterator metrics reply event
	 * @param replyEventProcessor reply event processor
	 * @return EndpointIteratorBuilder instance
	 */
	public EndpointIteratorBuilder setReplyEventProcessor(EventProcessor replyEventProcessor);
	/**
	 * sets the fault event processor to be used to handle the iterator metrics fault event
	 * @param faultEventProcessor fault event processor
	 * @return EndpointIteratorBuilder instance
	 */
	public EndpointIteratorBuilder setFaultEventProcessor(EventProcessor faultEventProcessor);
	/**
	 * sets the timeout event processor to be used to handle the iterator metrics timeout event
	 * @param timeoutEventProcessor timeout event processor
	 * @return EndpointIteratorBuilder instance
	 */
	public EndpointIteratorBuilder setTimeoutEventProcessor(EventProcessor timeoutEventProcessor);
	/**
	 * build the EndpointIterator instance after using all the attributes as set the user and the defaults
	 * @return EndpointIterator instance
	 * @throws DME2Exception
	 */
	public DME2BaseEndpointIterator build() throws DME2Exception;
	
}
