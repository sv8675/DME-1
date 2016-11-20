package com.att.aft.dme2.iterator.handler.order;

import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.iterator.helper.RouteOfferOrganize;
import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class EndpointPreferredRouteOfferOrderHandler implements IteratorEndpointOrderHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointPreferredRouteOfferOrderHandler.class.getName());
	private String preferredRouteOffer = null; 
	
	public EndpointPreferredRouteOfferOrderHandler(final String preferredRouteOffer) {
		this.preferredRouteOffer = preferredRouteOffer;
	}
	
	/*@Override
	public ListMultimap<Integer, DME2RouteOffer> order(ListMultimap<Integer, DME2RouteOffer> routeOfferGrpBySequenceMap) throws DME2Exception {
		LOGGER.info(null, "order", "start");
		long startTime = System.currentTimeMillis();
		LOGGER.info(null, "order", "get endpoint map:{}", routeOfferGrpBySequenceMap);
		LOGGER.info(null, "order", "resolvePreferredRouteOffer:{}", preferredRouteOffer);
		
		if(routeOfferGrpBySequenceMap!=null){
			routeOfferGrpBySequenceMap=RouteOfferOrganize.pushPreferredToFront(routeOfferGrpBySequenceMap, getPreferredRouteOffer());
		}
		
		LOGGER.info(null, "orderRouteOffers", "end;elapsed time:{}",(System.currentTimeMillis()-startTime));
		return routeOfferGrpBySequenceMap;
	}*/
	
	protected String getPreferredRouteOffer(){
		return this.preferredRouteOffer;
	}

	@Override
	public Map<Integer, Map<Double, DME2Endpoint[]>> order(
			Map<Integer, Map<Double, DME2Endpoint[]>> endpointGroupByRouteOfferSequenceMap) throws DME2Exception {
		LOGGER.info(null, "order", "start");
		long startTime = System.currentTimeMillis();
		LOGGER.info(null, "order", "get endpoint map:{}", endpointGroupByRouteOfferSequenceMap);
		LOGGER.info(null, "order", "resolvePreferredRouteOffer:{}", preferredRouteOffer);

		if(endpointGroupByRouteOfferSequenceMap!=null){
			endpointGroupByRouteOfferSequenceMap=RouteOfferOrganize.pushEndpointToFrontBasedOnPreferredRouteOffer(endpointGroupByRouteOfferSequenceMap, getPreferredRouteOffer());
		}
		
		LOGGER.info(null, "orderRouteOffers", "end;elapsed time:{}",(System.currentTimeMillis()-startTime));
		return endpointGroupByRouteOfferSequenceMap;
	}
}