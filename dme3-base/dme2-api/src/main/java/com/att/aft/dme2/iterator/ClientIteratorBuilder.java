package com.att.aft.dme2.iterator;

import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;

public class ClientIteratorBuilder {
	private DME2Configuration config;
	
	public ClientIteratorBuilder(DME2Configuration config) {
		this.config = config;
	}
	
	public void createIterator() throws DME2Exception{
		String url=null;
		Properties props=null;
		DME2EndpointURLFormatter urlFormatterImpl=null;
		DME2Manager manager = null;
		//RouteOfferOrdering routeOfferOrdering = null;
		//teratorEndpointOrderHandler<Map<Integer, Map<Double, DME2Endpoint[]>>, Map<Integer, DME2Endpoint[]>> endpointOrdering = null;
		
		//endpointOrdering = EndpointIteratorFactory.getEndpointOrderHandler(manager, props, urlFormatterImpl);
		//routeOfferOrdering = EndpointIteratorFactory.getRouteOfferOrderingHandler(props);
		
		DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory.getDefaultEndpointIteratorBuilder(config)
														.setServiceURI(url)
														.setProps(props)
														.setManager(manager)
														.setUrlFormatter(urlFormatterImpl)
														//.setRouteOfferOrdering(routeOfferOrdering)
														//.setEndpointOrdering(endpointOrdering)
														.build();
														
	}

}
