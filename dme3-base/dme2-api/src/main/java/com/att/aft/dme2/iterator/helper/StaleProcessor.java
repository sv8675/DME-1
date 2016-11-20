package com.att.aft.dme2.iterator.helper;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;

public class StaleProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(StaleProcessor.class.getName());
	private StaleProcessor() {
	}
	public static boolean containsStaleRouteOffer(DME2Configuration config, final DME2RouteOffer offer, final DME2Manager manager){
		LOGGER.debug(null, "containsStaleRouteOffer", "start; checking for stale RouteOffers using search filter: {}", offer.getSearchFilter() );
		
		int staleCount = 0;
		String[] routeOffers = offer.getSearchFilter().split(config.getProperty(DME2Constants.DME2_RO_SEP));

		for (String routeOffer : routeOffers){
			String serviceURI = null;
			if (routeOffer.equals(DmeUniformResource.DmeUrlType.DIRECT.toString()))
				continue;
			serviceURI = DME2URIUtils.buildServiceURIString(offer.getService(), offer.getVersion(), offer.getEnvContext(), routeOffer);

			if (!manager.isRouteOfferStale(serviceURI))
				continue;
			staleCount++;
		}

		if (staleCount == routeOffers.length){
			LOGGER.debug(null, "containsStaleRouteOffer", "All RouteOffers for service {} were previously marked stale. " +
          "The number of RouteOffers in the search filter was: {}.", offer.getService(), routeOffers.length);
			return true;
		}
		return false;
	}
	/**
	 * check whether the provided endpoint is stale or not
	 * @param manager
	 * @param endpoint
	 * @return
	 */
	public static boolean isStale(final DME2Manager manager, final DME2Endpoint endpoint){
		return manager.isUrlInStaleList(endpoint.getServiceEndpointID());
	}
	/**
	 * set the endpoint as stale
	 * @param orderedRouteOffer
	 * @param manager
	 * @param endpoint
	 */
	public static void setStale(final DME2RouteOffer orderedRouteOffer, final DME2Manager manager, final DME2Endpoint endpoint){
		Long stalenessDuration = 0L;
		if(orderedRouteOffer != null){
			if(orderedRouteOffer.getRouteOffer() != null){
				stalenessDuration = orderedRouteOffer.getRouteOffer().getStalenessInMins();
				if(stalenessDuration != null){
					stalenessDuration = (stalenessDuration * 60000); // Converting to milliseconds.
				}
			}
		}
		LOGGER.debug(null, "setStale", "Marking Endpoint [{}] stale.",endpoint.getServiceEndpointID());
		manager.addStaleEndpoint(endpoint.getServiceEndpointID(), stalenessDuration != null ? stalenessDuration : 0);
	}
}
