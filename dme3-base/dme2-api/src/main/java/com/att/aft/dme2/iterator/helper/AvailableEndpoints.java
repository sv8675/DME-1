package com.att.aft.dme2.iterator.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.google.common.collect.ListMultimap;

public class AvailableEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvailableEndpoints.class.getName());
	/**
	 * Gets the available endpoints.
	 * 
	 * @return the available endpoints
	 * @throws DME2Exception
	 *             the e http exception
	 */
	public static DME2Endpoint[][] find(final DME2RouteOffer dme2RouteOffer, final boolean useVersionRange, final DME2Manager manager) throws DME2Exception {
		//TODO: Need to make this "roundrobin" the endpoints in each band.
		//by returning a sliding list to each call to getAvailableEndpoints.
		//this can probably be done with a linked list:
		//also the call to registry.find each time will need to 
		//be altered to compare/add/remove from the linked lists
		//if the endpoints have changed since the last call.
		String serviceKey = dme2RouteOffer.isSearchWithWildcard()?dme2RouteOffer.getService().concat("*"):dme2RouteOffer.getService();

		final DME2Endpoint[] eps = dme2RouteOffer.getHardCodedEndpoints()!=null ? dme2RouteOffer.getHardCodedEndpoints() : manager.findEndpoints(serviceKey, dme2RouteOffer.getVersion(), dme2RouteOffer.getEnvContext(), dme2RouteOffer.getSearchFilter(), useVersionRange);
		return EndpointsByDistance.organize(manager, eps);
	}
	
	public static DME2Endpoint[] findUnorderedEndpoints(final DME2Manager manager, final DmeUniformResource uniformResource) throws DME2Exception {
		final DME2Endpoint[] eps = manager.getEndpoints(uniformResource);
		LOGGER.debug(null,  "findUnorderedEndpoints", "end: endpoints count #{}", eps!=null?eps.length:-1 );
		return eps;
	}

	public static SortedMap<Integer, DME2Endpoint[]> findByRouteOffer(DME2Configuration config,
			ListMultimap<Integer, DME2RouteOffer> routeOffersGroupedBySequence, 
			DME2Manager manager, 
			DmeUniformResource resource)
	{
		//Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence = new TreeMap<Integer, Map<Double, DME2Endpoint[]>>();
		SortedMap<Integer, DME2Endpoint[]> endpointMap = new TreeMap<Integer, DME2Endpoint[]>();

		try{
			for (Integer sequence : routeOffersGroupedBySequence.keySet()){
				for (DME2RouteOffer routeOffer : routeOffersGroupedBySequence.get(sequence)){
					String searchKey = resource.getRegistryFindEndpointSearchKey();
					if(searchKey == null){
						searchKey = routeOffer.getService();
					}
					
					if(StaleProcessor.containsStaleRouteOffer(config, routeOffer,manager)){
						LOGGER.debug(null,  "findByRouteOffer", "RouteOffer was previously marked stale: {}",routeOffer.getRouteOffer().getName());
						continue;
					}

          String versionToUse = config.getProperty( DME2Constants.AFT_DME2_PREFERRED_VERSION );
          if ( StringUtils.isEmpty( versionToUse )) {
            versionToUse = routeOffer.getVersion();
          }
          DME2Endpoint[] endpoints = manager.findEndpoints(searchKey, versionToUse, routeOffer.getEnvContext(), routeOffer.getSearchFilter(), resource.isUsingVersionRanges());
					
					// If find() returned an empty endpoint Array for the given routeOffer log a message about this event 
					if(endpoints.length == 0){
						manager.addStaleRouteOffer(DME2URIUtils.buildServiceURIString(searchKey, routeOffer.getVersion(), routeOffer.getEnvContext(), routeOffer.getRouteOffer().getName()), 0L);
						LOGGER.debug(null,  "findByRouteOffer", "0 Endpoints were returned for routeOffer, marking stale: {}", routeOffer.getRouteOffer().getName());
					}
					
					LOGGER.debug(null,  "findByRouteOffer", "Number of Endpoints returned for routeOffer {}: {}", routeOffer.getSearchFilter(),endpoints.length);
					
					endpointMap.put(sequence, endpoints);
				}
			}
		}catch (DME2Exception e){
			LOGGER.error(null,  "findByRouteOffer", "DME2Exception:{}",e);
		}
		return endpointMap;
	}	
	public static List<DME2EndpointReference> createOrderedEndpointHolders(final DME2Manager manager, final ListMultimap<Integer, DME2RouteOffer> routeOffersGroupedBySeqence, final Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence)
	{
		List<DME2EndpointReference> endpointReference = new ArrayList<DME2EndpointReference>();
		
		Set<Integer> endpointSequences = endpointsGroupedBySequence.keySet();
		for(Integer sequence : endpointSequences)
		{
			Map<Double, DME2Endpoint[]> bandedEndpoints = endpointsGroupedBySequence.get(sequence);
			Set<Double> endpointDistanceBands = bandedEndpoints.keySet();
			
			for(Double distanceBand : endpointDistanceBands)
			{
				DME2Endpoint[] endpoints = bandedEndpoints.get(distanceBand);
				for(DME2Endpoint endpoint : endpoints)
				{
					DME2RouteOffer routeOfferHolder = getRouteOfferForEndpoint(routeOffersGroupedBySeqence, endpoint, sequence);
					
					DME2EndpointReference reference = new DME2EndpointReference()
															.setSequence(sequence)
															.setDistanceBand(distanceBand)
															.setRouteOffer(routeOfferHolder)
															.setEndpoint(endpoint)
															.setManager(manager);
					
					endpointReference.add(reference);
				}
			}
		}
		
		return endpointReference;
	}
	public static DME2RouteOffer getRouteOfferForEndpoint(ListMultimap<Integer, DME2RouteOffer> routeOfferMultiMap, DME2Endpoint endpoint, Integer sequence)
	{
		if(routeOfferMultiMap == null || endpoint == null || sequence == null)
		{
			return null;
		}
		
		List<DME2RouteOffer> routeOffers = routeOfferMultiMap.get(sequence);
		DME2RouteOffer tempRouteOffer = null;
		
		if(endpoint.getRouteOffer() != null)
		{
			for(DME2RouteOffer routeOffer : routeOffers)
			{
        if ( routeOffer != null && routeOffer.getSearchFilter() != null ) {
          String[] tokens = routeOffer.getSearchFilter().split( "~" );
          for ( String tok : tokens ) {
            if ( endpoint.getRouteOffer().contains( tok ) || tok.contains( "DEFAULT" ) ) {
              tempRouteOffer = routeOffer;
              break;
            }
          }
        }
			}
		}
		
		return tempRouteOffer;
	}
}
