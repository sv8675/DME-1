package com.att.aft.dme2.iterator.handler.order;

import java.util.List;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.helper.RouteOfferOrganize;
import com.att.aft.dme2.iterator.service.IteratorRouteOfferOrderHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.google.common.collect.ListMultimap;

public class DefaultRouteOfferOrderHandler implements IteratorRouteOfferOrderHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRouteOfferOrderHandler.class.getName());
	private List<DME2RouteOffer> routeOffers = null; 
	
	public DefaultRouteOfferOrderHandler(final List<DME2RouteOffer> routeOffers) {
		this.routeOffers = routeOffers;
	}
	
	@Override
	public ListMultimap<Integer, DME2RouteOffer> order(ListMultimap<Integer, DME2RouteOffer> routeOfferGrpBySequenceMap) throws DME2Exception {
		LOGGER.debug(null, "order", "start");
		long startTime = System.currentTimeMillis();
		LOGGER.debug(null, "order", "get endpoint list: {}", routeOffers);
		
		if(routeOffers!=null){
			routeOfferGrpBySequenceMap = RouteOfferOrganize.withSequence(routeOffers);
		}
		
		LOGGER.debug(null, "orderRouteOffers", "end;elapsed time:{}",(System.currentTimeMillis()-startTime));
		return routeOfferGrpBySequenceMap;
	}
}