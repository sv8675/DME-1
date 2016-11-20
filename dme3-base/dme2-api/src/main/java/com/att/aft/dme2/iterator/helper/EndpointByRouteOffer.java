package com.att.aft.dme2.iterator.helper;

import java.util.Map;
import java.util.TreeMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2URIUtils;
import com.google.common.collect.ListMultimap;

public class EndpointByRouteOffer {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointByRouteOffer.class.getName());
	private EndpointByRouteOffer() {
	}
	
	public static Map<Integer, DME2Endpoint[]>  find(DME2Configuration config,
			ListMultimap<Integer, DME2RouteOffer> routeOffersGroupedBySequence, 
			DME2Manager manager, 
			DmeUniformResource resource)
	{
		//Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence = new TreeMap<Integer, Map<Double, DME2Endpoint[]>>();
		Map<Integer, DME2Endpoint[]> endpointMap = new TreeMap<Integer, DME2Endpoint[]>();

		try{
			for (Integer sequence : routeOffersGroupedBySequence.keySet()){
				for (DME2RouteOffer routeOffer : routeOffersGroupedBySequence.get(sequence)){
					String searchKey = resource.getRegistryFindEndpointSearchKey();
					if(searchKey == null){
						searchKey = routeOffer.getService();
					}
					
					if(StaleProcessor.containsStaleRouteOffer(config,routeOffer,manager)){
						LOGGER.info(null,  "find", "RouteOffer was previously marked stale: {}",routeOffer.getRouteOffer().getName());
						continue;
					}
					
					DME2Endpoint[] endpoints = manager.findEndpoints(searchKey, routeOffer.getVersion(), routeOffer.getEnvContext(), routeOffer.getSearchFilter(), resource.isUsingVersionRanges());
					
					// If find() returned an empty endpoint Array for the given routeOffer log a message about this event 
					if(endpoints.length == 0){
						manager.addStaleRouteOffer(DME2URIUtils.buildServiceURIString(searchKey, routeOffer.getVersion(), routeOffer.getEnvContext(), routeOffer.getRouteOffer().getName()), 0L);
						LOGGER.info(null,  "find", "0 Endpoints were returned for routeOffer, marking stale: {}", routeOffer.getRouteOffer().getName());
					}
					
					LOGGER.info(null,  "find", "Number of Endpoints returned for routeOffer {}: {}", routeOffer.getSearchFilter(),endpoints.length);
					
					endpointMap.put(sequence, endpoints);

					//Map<Double, DME2Endpoint[]> distanceToEndpointMap = EndpointsByDistance.organize(endpoints);
					//endpointsGroupedBySequence.put(sequence, distanceToEndpointMap);
				}
			}
		}catch (DME2Exception e){
			LOGGER.info(null,  "find", "DME2Exception:{}",e);
		}
		//return endpointsGroupedBySequence;
		return endpointMap;
	}	
/*	public static Map<Integer, Map<Double, DME2Endpoint[]>>  find(
			Multimap<Integer, RouteOfferHolder> routeOfferMultimap, 
			DME2Manager manager, 
			DME2UniformResource resource)
	{
		Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence = new TreeMap<Integer, Map<Double, DME2Endpoint[]>>();

		try{
			for (Integer sequence : routeOfferMultimap.keySet()){
				for (RouteOfferHolder routeOffer : routeOfferMultimap.get(sequence)){
					String searchKey = resource.getRegistryFindEndpointSearchKey();
					if(searchKey == null){
						searchKey = routeOffer.getService();
					}
					
					if(StaleProcessor.containsStale(routeOffer,manager)){
						LOGGER.info(null,  "find", "RouteOffer was previously marked stale: {}",routeOffer.getRouteOffer().getName());
						continue;
					}
					
					DME2Endpoint[] endpoints = manager.findEndpoints(searchKey, routeOffer.getVersion(), routeOffer.getEnvContext(), routeOffer.getSearchFilter(), resource.isUsingVersionRanges());
					
					 If find() returned an empty endpoint Array for the given routeOffer log a message about this event 
					if(endpoints.length == 0){
						manager.addStaleRouteOffer(DME2URIUtils.buildServiceURIString(searchKey, routeOffer.getVersion(), routeOffer.getEnvContext(), routeOffer.getRouteOffer().getName()), 0L);
						LOGGER.info(null,  "find", "0 Endpoints were returned for routeOffer, marking stale: {}", routeOffer.getRouteOffer().getName());
					}
					
					LOGGER.info(null,  "find", "Number of Endpoints returned for routeOffer {}: {}", routeOffer.getSearchFilter(),endpoints.length);

					Map<Double, DME2Endpoint[]> distanceToEndpointMap = EndpointsByDistance.organize(endpoints);
					endpointsGroupedBySequence.put(sequence, distanceToEndpointMap);
				}
			}
		}catch (DME2Exception e){
			LOGGER.info(null,  "find", "DME2Exception:{}",e);
		}
		return endpointsGroupedBySequence;
	}
*/	
}
