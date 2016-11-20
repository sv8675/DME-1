package com.att.aft.dme2.iterator.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.dme2.DME2JdbcEndpoint;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.util.DME2Constants;

public class EndpointPreference {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointPreference.class.getName());
	
	private EndpointPreference() {
	}

	public static String resolvePreferredRouteOffer(DME2Configuration config, final Properties props)
	{
		String preferredRouteOffer = null;
		if(props != null){
			preferredRouteOffer = props.getProperty(DME2Constants.Iterator.AFT_DME2_PREFERRED_ROUTEOFFER);
			if(preferredRouteOffer != null){
				return preferredRouteOffer;
			}
		}
		
		/*If null, try getting it from the System or DME2Manager properties*/
		//preferredRouteOffer = System.getProperty(DME2Constants.PREFERRED_ROUTE_OFFER, manager.getStringProp(Constants.PREFERRED_ROUTE_OFFER, null));
		preferredRouteOffer =  config.getProperty(DME2Constants.Iterator.AFT_DME2_PREFERRED_ROUTEOFFER);
		return preferredRouteOffer;
	}
	public static String resolvePreferredConnection(DME2Configuration config, final Properties props)
	{
		LOGGER.debug(null, "resolvePreferredConnection", "start");
		String preferredURL = null;
		if(props != null){
			preferredURL = props.getProperty(DME2Constants.Iterator.AFT_DME2_PREFERRED_URL);
			if(preferredURL != null) {
				return preferredURL;
			}
		}
		
		/*If null, try getting it from the System or DME2Manager properties*/
		//preferredURL = System.getProperty(Constants.PREFERRED_URL, manager.getStringProp(DME2Constants.Iterator.PREFERRED_URL, null));
		preferredURL = config.getProperty(DME2Constants.Iterator.AFT_DME2_PREFERRED_URL);
		
		return preferredURL;
		
	}
	/** Iterates over the collection of DME2Endpoints and determines if an Endpoint's RouteOffer value
	 *  matches the preferredRouteOffer that was provided in the method argument. If this is true, the DME2Endpoint collection will be reorganized to have
	 *  those "preferred" Endpoints positioned first in the list.*/
	public static void organizeByPreferredRouteOffer(final Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGrpBySeqDistMap, final String preferredRouteOffer)
	{
		LOGGER.debug(null, "organizeByPreferredRouteOffer", "Preferred routeOffer found! Reorganizing endpoints based on the following preferred routeOffer: {}", preferredRouteOffer);
		List<DME2Endpoint> preferredEndpoints = new ArrayList<DME2Endpoint>();
		
		Set<Integer> sequences = endpointsGrpBySeqDistMap.keySet();
		for(Integer sequence : sequences)
		{
			 Map<Double, DME2Endpoint[]> distanceBandsToEndpointsMap = endpointsGrpBySeqDistMap.get(sequence);
			 Set<Double> distanceBands = distanceBandsToEndpointsMap.keySet();
			 
			 for(Double distanceBand : distanceBands)
			 {
				 DME2Endpoint[] eps = distanceBandsToEndpointsMap.get(distanceBand);
				 for(DME2Endpoint ep : eps)
				 {
					 if(ep.getRouteOffer().equals(preferredRouteOffer)){
						 preferredEndpoints.add(ep); 
					 }
				 }
			 }
		} //End sequence loop
		
		if(!preferredEndpoints.isEmpty())
		{
			Map<Double, DME2Endpoint[]> preferredEndpointsMap = new HashMap<Double, DME2Endpoint[]>();
			DME2Endpoint[] preferredEndpointArray = new DME2Endpoint[preferredEndpoints.size()];
			preferredEndpoints.toArray(preferredEndpointArray);
			preferredEndpointsMap.put(0D, preferredEndpointArray);
			
			endpointsGrpBySeqDistMap.put(-1, preferredEndpointsMap);
		}
	}
	
	public static Map<Integer, Map<Double, DME2Endpoint[]>> organizeEndpointsByPreferredURL(Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGrpBySeqDistMap, String preferredURL, DME2EndpointURLFormatter urlFormatter)
	{
		DME2Endpoint endpointWithPreferredURL = null;
		boolean preferredURLFound = false;
		
		if(endpointsGrpBySeqDistMap != null){
			Set<Integer> sequences = endpointsGrpBySeqDistMap.keySet();
			for(Integer sequence : sequences){
				Map<Double, DME2Endpoint[]> distanceBandsToEndpoints = endpointsGrpBySeqDistMap.get(sequence);
				Set<Double> distanceBands = distanceBandsToEndpoints.keySet();
				 
				 for(Double distanceBand : distanceBands){
					 DME2Endpoint[] eps = distanceBandsToEndpoints.get(distanceBand);
					 for(DME2Endpoint ep : eps){
						 String finalURLString = null;
						 
						 if(urlFormatter != null){
							 finalURLString = urlFormatter.formatURL(ep);
							 if(ep instanceof DME2JdbcEndpoint){
								if (finalURLString.startsWith("jdbc:dme2jdbc")){
									finalURLString = finalURLString.substring(9);
								}
							}
						 }else{
							 finalURLString = ep.toURLString();
						 }
						 
						 /*If true, then this endpoint is the one with the preferred connection/URL, so move it to the beginning*/
						 if(finalURLString.equals(preferredURL)){
							 endpointWithPreferredURL = ep;
               preferredURLFound = true;
               break;
						 }
					 }
					 if(preferredURLFound){
						 break;
					 }
				 }
				 if(preferredURLFound){
					 break;
				 }
			}
			if(preferredURLFound && endpointWithPreferredURL != null){
				/*Need to add this endpoint to the beginning of the list*/
				DME2Endpoint[] endpointArray = {endpointWithPreferredURL};
				
				 Map<Double, DME2Endpoint[]> map = new HashMap<Double, DME2Endpoint[]>();
				 map.put(0.0, endpointArray);
				 
				 /*Adding the above entry to the master collection under sequence -1*/
				 endpointsGrpBySeqDistMap.put(-1, map);			 
			}
		}
		return endpointsGrpBySeqDistMap;
	}

}
