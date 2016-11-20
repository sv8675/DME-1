package com.att.aft.dme2.iterator.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

/**
 * split a flat list of endpoints into groups by distance (see manager.getDistanceBands())
 * shuffle endpoints within each band and return as a list of bands, each a list of endpoints
 * Organize the list into "bands" based on distance, then randomize the content of each band
 * @author ab850e
 */
public class EndpointsByDistance {
	/**
	 * @param eps DME2Endpoints
	 * @return the http endpoint[][]
	 */
	public static DME2Endpoint[][] organize(final DME2Manager manager, final DME2Endpoint[] eps) {
		Map<Integer, List<DME2Endpoint>> bandLists = new HashMap<Integer, List<DME2Endpoint>>();

		double[] bands = manager.getDistanceBands();

		for (int i = 0; i < bands.length; i++) {
			bandLists.put(i, new ArrayList<DME2Endpoint>());
		}

		for (DME2Endpoint ep : eps) {
			double distance = ep.getDistance();
			for (int i = 0; i < bands.length; i++) {
				if (distance < bands[i]) {
					// workaround - sometimes we seem to get all eps back for multiple offers at once..
					bandLists.get(i).add(ep);
					break;
				}
			}
		}

		DME2Endpoint[][] bandedEps = new DME2Endpoint[bands.length][0];

		int counter = 0;
		for (List<DME2Endpoint> list : bandLists.values()) {
			Collections.shuffle(list);
			bandedEps[counter] = list.toArray(new DME2Endpoint[list.size()]);
			counter++;
		}

		return bandedEps;
	}
	/**
	 * group all the provided endpoints with respect to their distance bands
	 * @param endpoints
	 * @return
	 * @throws DME2Exception
	 */
	public static Map<Double, DME2Endpoint[]> organize(final DME2Endpoint[] endpoints) throws DME2Exception
	{
		// Get the DME2Manager
		return organize(endpoints, new DME2Manager());
	}
	public static SortedMap<Integer, Map<Double, DME2Endpoint[]>> organize(final SortedMap<Integer, DME2Endpoint[]> unorderedEndpointByRouteOfferSeqMap, final DME2Manager manager) throws DME2Exception
	{
		SortedMap<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence = new TreeMap<Integer, Map<Double, DME2Endpoint[]>>();
		double[] distanceBands = manager.getDistanceBands();
		for( Entry<Integer,DME2Endpoint[]> entry: unorderedEndpointByRouteOfferSeqMap.entrySet()){
			endpointsGroupedBySequence.put(entry.getKey(), organize(manager, entry.getValue(), distanceBands));
		}
		return endpointsGroupedBySequence;
	}
	public static Map<Double, DME2Endpoint[]> organize(final DME2Endpoint[] endpoints, final DME2Manager manager) throws DME2Exception
	{
		// Get the bands from the DME2Manager
		return organize(manager, endpoints, manager.getDistanceBands());
	}
	public static Map<Double, DME2Endpoint[]> organize(final DME2Manager manager, final DME2Endpoint[] endpoints, final double[] distanceBands) throws DME2Exception
	{
		// Map that contains list of endpoints that are associated with a given distance band
		Map<Double, List<DME2Endpoint>> bandToEndpointMap = new TreeMap<Double, List<DME2Endpoint>>();
		Map<Double, DME2Endpoint[]> map = new TreeMap<Double, DME2Endpoint[]>();

		// Place each distance band in the map and initialize the lists for each band.
		for (double distanceBand : distanceBands){
			bandToEndpointMap.put(distanceBand, new ArrayList<DME2Endpoint>());
		}

		// Loop thru the endpoint array and check if the distance for each endpoint falls within a certain distance band
		for (DME2Endpoint endpoint : endpoints){
			double endpointDistance = endpoint.getDistance();

			for (Double band : bandToEndpointMap.keySet()){
				if (endpointDistance < band){
					bandToEndpointMap.get(band).add(endpoint);
					break;
				}
			}
		}

		// Randomizing endpoints within the same distance band to follow round robin strategy
		for (Map.Entry<Double, List<DME2Endpoint>> entry : bandToEndpointMap.entrySet())
		{
			double band = entry.getKey();
			List<DME2Endpoint> endpointListUnshuffled = entry.getValue();

			if (endpointListUnshuffled.size() > 0)
			{
				List<DME2Endpoint> endpointList = randomizeEndpoints(endpointListUnshuffled);

				DME2Endpoint[] endpointArray = endpointList.toArray(new DME2Endpoint[endpointList.size()]);
				map.put(band, endpointArray);
			}
		}

		return map;
	}
	
	private static List<DME2Endpoint> randomizeEndpoints(List<DME2Endpoint> endpoints){
		Random random = new Random();
		
		List<DME2Endpoint> newList = new ArrayList<DME2Endpoint>();
		newList.addAll(endpoints);
		
		Collections.shuffle(newList, random);
		return newList;
	}
}
