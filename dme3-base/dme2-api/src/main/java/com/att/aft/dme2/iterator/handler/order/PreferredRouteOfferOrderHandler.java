package com.att.aft.dme2.iterator.handler.order;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.helper.RouteOfferOrganize;
import com.att.aft.dme2.iterator.service.IteratorRouteOfferOrderHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.google.common.collect.ListMultimap;

public class PreferredRouteOfferOrderHandler implements IteratorRouteOfferOrderHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferredRouteOfferOrderHandler.class.getName());
	private String preferredRouteOffer = null; 
	
	public PreferredRouteOfferOrderHandler(final String preferredRouteOffer) {
		this.preferredRouteOffer = preferredRouteOffer;
	}
	
	@Override
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
	}
	
	protected String getPreferredRouteOffer(){
		return this.preferredRouteOffer;
	}
}