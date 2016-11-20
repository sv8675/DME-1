package com.att.aft.dme2.iterator.factory;

import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.DME2EndpointIterator;
import com.att.aft.dme2.iterator.DefaultEndpointIteratorBuilder;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.domain.IteratorCreatingAttributes;
import com.att.aft.dme2.iterator.handler.order.DefaultEndpointOrderHandler;
import com.att.aft.dme2.iterator.handler.order.DefaultRouteOfferOrderHandler;
import com.att.aft.dme2.iterator.handler.order.EndpointPreferredRouteOfferOrderHandler;
import com.att.aft.dme2.iterator.handler.order.EndpointPreferredUrlOrderHandler;
import com.att.aft.dme2.iterator.handler.order.PreferredRouteOfferOrderHandler;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;
import com.att.aft.dme2.iterator.service.EndpointIteratorBuilder;
import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.iterator.service.IteratorRouteOfferOrderHandler;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class EndpointIteratorFactory {
	
	public EndpointIteratorFactory() {
	}
	
	public static EndpointIteratorBuilder getDefaultEndpointIteratorBuilder(DME2Configuration config){
		return  new DefaultEndpointIteratorBuilder(config);
	}

	public static DME2BaseEndpointIterator getDefaultIterator(final IteratorCreatingAttributes iteratorCreatingAttributes) throws DME2Exception{
		return new DME2EndpointIterator(iteratorCreatingAttributes);
	}
	
	public DME2BaseEndpointIterator getBaseIterator(final String newUrl, Properties props, DME2EndpointURLFormatter urlFormatterImpl, DME2Manager manager) throws DME2Exception {
		//TODO
		return null;
	}
	
	public static IteratorEndpointOrderHandler getEndpointPreferredUrlOrderHandler(final String preferredURL, final DME2EndpointURLFormatter urlFormatter){
		return new EndpointPreferredUrlOrderHandler(preferredURL,urlFormatter);
	}
	public static IteratorEndpointOrderHandler getEndpointPreferredRouteOfferOrderHandler(final String preferredRouteOffer){
		return new EndpointPreferredRouteOfferOrderHandler(preferredRouteOffer);
	}
	public static IteratorEndpointOrderHandler getDefaultEndpointOrderHandler(final SortedMap<Integer, DME2Endpoint[]> unorderedEndpointByRouteOfferSeqMap, final DME2Manager manager){
		return new DefaultEndpointOrderHandler(unorderedEndpointByRouteOfferSeqMap, manager);
	}
	public static IteratorRouteOfferOrderHandler getPreferredRouteOfferOrderHandler(final String preferredRouteOffer){
		return new PreferredRouteOfferOrderHandler(preferredRouteOffer);
	}
	public static IteratorRouteOfferOrderHandler getDefaultRouteOfferOrderHandler(final List<DME2RouteOffer> routeOffers){
		return new DefaultRouteOfferOrderHandler(routeOffers);
	}
}