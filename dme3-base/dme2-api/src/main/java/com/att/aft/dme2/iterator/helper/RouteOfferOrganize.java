package com.att.aft.dme2.iterator.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

public class RouteOfferOrganize {
	private static final Logger LOGGER = LoggerFactory.getLogger(RouteOfferOrganize.class.getName());
	private RouteOfferOrganize() {
	}

	/**
	 * returns a ordered map of routeoffers keyed with their sequence 
	 * @param routeOffers
	 * @return
	 */
	public static ListMultimap<Integer, DME2RouteOffer> withSequence(List<DME2RouteOffer> routeOffers){
		Function<DME2RouteOffer, Integer> keyFun = new Function<DME2RouteOffer, Integer>(){
			@Override
			public Integer apply(DME2RouteOffer routeOfferHolder){
				return routeOfferHolder.getSequence();
			}
		};
		//Wrapping Multimaps.index(routeOffers, keyFun) because it was returning an Immutable map that couldn't be modified later.
		return ArrayListMultimap.create(Multimaps.index(routeOffers, keyFun));
	}
	/** Checks of the collection of RouteOffers contains the preferred RouteOffer value that was provided in the method argument. 
	 * Returns true if the preferred RouteOffer is found.*/
	public static ListMultimap<Integer, DME2RouteOffer> pushPreferredToFront(ListMultimap<Integer, DME2RouteOffer> routeOffersGrpBySeq, String preferredRouteOffer)
	{
		Set<Integer> sequences = routeOffersGrpBySeq.keySet();
		if(preferredRouteOffer != null && routeOffersGrpBySeq != null && routeOffersGrpBySeq.size() > 1 ){
			for(Integer sequence : sequences)
			{
				List<DME2RouteOffer> routeOfferList = routeOffersGrpBySeq.get(sequence);
				for(DME2RouteOffer routeOffer : routeOfferList)
				{
					if(routeOffer.getSearchFilter().contains(preferredRouteOffer)){
						//Move the preferred routeOffer to the beginning of the collection.
						routeOffersGrpBySeq.put(-1, routeOffer);
						return routeOffersGrpBySeq;
					}
				}
			}
		}
		LOGGER.info(null, "containsPreferredRouteOffer", "cannot find preferred route offer: [{}], {}",preferredRouteOffer, routeOffersGrpBySeq);
		return routeOffersGrpBySeq;
	}
	public static Map<Integer, Map<Double, DME2Endpoint[]>> pushEndpointToFrontBasedOnPreferredRouteOffer(Map<Integer, Map<Double, DME2Endpoint[]>> endpointGroupByRouteOfferSequenceMap, String preferredRouteOffer)
	{
		if(preferredRouteOffer != null && endpointGroupByRouteOfferSequenceMap != null){
			
			List<DME2Endpoint> preferredEndpointsList = new ArrayList<DME2Endpoint>();
			Set<Integer> sequences = endpointGroupByRouteOfferSequenceMap.keySet();
			
			for(Integer sequence : sequences)
			{
				 Map<Double, DME2Endpoint[]> distanceBandsToEndpoints = endpointGroupByRouteOfferSequenceMap.get(sequence);
				 Set<Double> distanceBands = distanceBandsToEndpoints.keySet();
				 
				 for(Double distanceBand : distanceBands)
				 {
					 DME2Endpoint[] eps = distanceBandsToEndpoints.get(distanceBand);
					 for(DME2Endpoint ep : eps)
					 {
						 if(ep.getRouteOffer().equals(preferredRouteOffer)){
							 preferredEndpointsList.add(ep); 
						 }
					 }
				 }
			} //End sequence loop
			
			if(!preferredEndpointsList.isEmpty())
			{
				Map<Double, DME2Endpoint[]> preferredEndpointsMap = new HashMap<Double, DME2Endpoint[]>();
				DME2Endpoint[] preferredEndpointArray = new DME2Endpoint[preferredEndpointsList.size()];
				preferredEndpointsList.toArray(preferredEndpointArray);
				preferredEndpointsMap.put(0D, preferredEndpointArray);
				
				endpointGroupByRouteOfferSequenceMap.put(-1, preferredEndpointsMap);
			}
		}
		LOGGER.info(null, "containsPreferredRouteOffer", "cannot find preferred route offer: [{}], {}",preferredRouteOffer, endpointGroupByRouteOfferSequenceMap);
		return endpointGroupByRouteOfferSequenceMap;
	}

}
