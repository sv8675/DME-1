package com.att.aft.dme2.iterator.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2JDBCEndpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;



public class DME2CollectionUtils
{
  private static final Logger logger = LoggerFactory.getLogger( DME2CollectionUtils.class.getName() );

  public static ListMultimap<Integer, DME2RouteOffer> organizeRouteOffersBySequence(List<DME2RouteOffer> routeOffers)
  {
    Function<DME2RouteOffer, Integer> keyFun = new Function<DME2RouteOffer, Integer>()
    {

      @Override
      public Integer apply(DME2RouteOffer routeOffer)
      {
        return routeOffer.getSequence();
      }

    };

    //Wrapping Multimaps.index(routeOffers, keyFun) because it was returning an Immutable map that couldn't be modified later.
    return ArrayListMultimap.create( Multimaps.index( routeOffers, keyFun ) );
  }


  public static Map<Integer, Map<Double, DME2Endpoint[]>>  findDME2EndpointByRouteOffer(ListMultimap<Integer, DME2RouteOffer> routeOfferMultimap, DME2Manager manager, DmeUniformResource resource)
  {
    Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence = new TreeMap<Integer, Map<Double, DME2Endpoint[]>>();

    try
    {
      for (Integer sequence : routeOfferMultimap.keySet())
      {
        List<DME2RouteOffer> routeOffers = routeOfferMultimap.get(sequence);

        for (DME2RouteOffer routeOffer : routeOffers)
        {
          String searchKey = resource.getRegistryFindEndpointSearchKey();
          if(searchKey == null){
            searchKey = routeOffer.getService();
          }

          if(containsStaleRouteOffer(routeOffer,manager))
          {
            logger.debug( null, "findDME2EndpointByRouteOffer", "RouteOffer was previously marked stale: {}", routeOffer.getRouteOffer().getName());
            continue;
          }

          DME2Endpoint[] endpoints = manager.findEndpoints(searchKey, routeOffer.getVersion(), routeOffer.getEnvContext(), routeOffer.getSearchFilter(), resource.isUsingVersionRanges());
					
					/* If find() returned an empty endpoint Array for the given routeOffer log a message about this event */
          if(endpoints.length == 0)
          {
            manager.addStaleRouteOffer( DME2Utils
                .buildServiceURIString( searchKey, routeOffer.getVersion(), routeOffer.getEnvContext(),
                    routeOffer.getRouteOffer().getName() ), 0L);
            logger.debug( null, "findDME2EndpointByRouteOffer",
                "0 Endpoints were returned for routeOffer, marking stale: {}", routeOffer.getRouteOffer().getName() );
          }

          logger.debug( null, "findDME2EndpointByRouteOffer", "Number of Endpoints returned for routeOffer {}: {}",
              routeOffer.getSearchFilter(), endpoints.length );

          Map<Double, DME2Endpoint[]> distanceToEndpointMap = organizeEndpoints(endpoints);
          endpointsGroupedBySequence.put(sequence, distanceToEndpointMap);
        }
      }
    }
    catch (DME2Exception e)
    {
      logger.debug( null, "findDME2EndpointByRouteOffer", "DME2Exception", e );
			/*TODO: Throw exception here*/
    }

    return endpointsGroupedBySequence;
  }


  public static SortedMap<Double, DME2Endpoint[]> organizeEndpoints(DME2Endpoint[] endpoints) throws DME2Exception
  {

    // Map that contains list of endpoints that are associated with a given distance band
    Map<Double, List<DME2Endpoint>> bandToEndpointMap = new TreeMap<Double, List<DME2Endpoint>>();

    SortedMap<Double, DME2Endpoint[]> map = new TreeMap<Double, DME2Endpoint[]>();

    // Get the bands from the DME2Manager
    double[] distanceBands = DME2Manager.getDefaultInstance().getDistanceBands();


    // Place each distance band in the map and initialize the lists for each band.
    for (double distanceBand : distanceBands)
    {
      bandToEndpointMap.put(distanceBand, new ArrayList<DME2Endpoint>());
    }


    // Loop thru the endpoint array and check if the distance for each endpoint falls within a certain distance band
    for (DME2Endpoint endpoint : endpoints)
    {
      double endpointDistance = endpoint.getDistance();

      for (Double band : bandToEndpointMap.keySet())
      {
        if (endpointDistance < band)
        {
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


    Collections.shuffle( newList, random );
    return newList;
  }

  public static List<String> randomizeURLs(List<String> endpoints){
    Random random = new Random();

    List<String> newList = new ArrayList<String>();
    newList.addAll(endpoints);

    Collections.shuffle(newList, random);
    return newList;
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
        String[] tokens = routeOffer.getSearchFilter().split("~");
        for(String tok : tokens)
        {
          if(endpoint.getRouteOffer().contains(tok) || tok.contains("DEFAULT"))
          {
            tempRouteOffer = routeOffer;
            break;
          }
        }
      }
    }

    return tempRouteOffer;
  }


  public static List<DME2EndpointReference> createDME2EndpointReferences(DME2Manager manager, ListMultimap<Integer, DME2RouteOffer> routeOffersGroupedBySeqence, Map<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence)
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
          DME2RouteOffer routeOffer = getRouteOfferForEndpoint(routeOffersGroupedBySeqence, endpoint, sequence);

          DME2EndpointReference reference = new DME2EndpointReference(manager, endpoint);
          reference.setSequence(sequence);
          reference.setDistanceBand(distanceBand);
          reference.setRouteOffer(routeOffer);

          endpointReference.add(reference);
        }
      }
    }

    return endpointReference;
  }

  /** Checks of the collection of RouteOffers contains the preferred RouteOffer value that was provided in the method argument. 
   * Returns true if the preferred RouteOffer is found.*/
  public static boolean containsPreferredRouteOffer(ListMultimap<Integer, DME2RouteOffer> routeOfferMultiMap, String preferredRouteOffer)
  {
    Set<Integer> sequences = routeOfferMultiMap.keySet();
    for(Integer sequence : sequences)
    {
      List<DME2RouteOffer> routeOfferList = routeOfferMultiMap.get(sequence);
      for(DME2RouteOffer routeOffer : routeOfferList)
      {
        if(routeOffer.getSearchFilter().contains(preferredRouteOffer))
        {
          //Move the preferred routeOffer to the beginning of the collection.
          routeOfferMultiMap.put(-1, routeOffer);
          return true;
        }
      }
    }
    return false;
  }


  /** Iterates over the collection of DME2Endpoints and determines if an Endpoint's RouteOffer value
   *  matches the preferredRouteOffer that was provided in the method argument. If this is true, the DME2Endpoint collection will be reorganized to have
   *  those "preferred" Endpoints positioned first in the list.*/
  public static void organizeEndpointsByPreferredRouteOffer(Map<Integer, Map<Double, DME2Endpoint[]>> sequenceToEndpointMap, String preferredRouteOffer)
  {
    if(sequenceToEndpointMap != null)
    {
      List<DME2Endpoint> preferredEndpointsList = new ArrayList<DME2Endpoint>();

      Set<Integer> sequences = sequenceToEndpointMap.keySet();
      for(Integer sequence : sequences)
      {
        Map<Double, DME2Endpoint[]> distanceBandsToEndpoints = sequenceToEndpointMap.get(sequence);
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

        sequenceToEndpointMap.put(-1, preferredEndpointsMap);
      }
    } //End if sequenceToEndpointMap != null
  }


  public static boolean containsPreferredURL(Map<Integer, Map<Double, DME2Endpoint[]>> sequenceToEndpointMap, String preferredURL, DME2EndpointURLFormatter urlFormatter)
  {
    Set<Integer> sequences = sequenceToEndpointMap.keySet();
    for(Integer sequence : sequences)
    {
      Map<Double, DME2Endpoint[]> distanceBandsToEndpoints = sequenceToEndpointMap.get(sequence);
      Set<Double> distanceBands = distanceBandsToEndpoints.keySet();

      for(Double distanceBand : distanceBands)
      {
        DME2Endpoint[] eps = distanceBandsToEndpoints.get(distanceBand);
        for(DME2Endpoint ep : eps)
        {
          String finalURLString = null;

          if(urlFormatter != null)
          {
            finalURLString = urlFormatter.formatURL(ep);

            if(ep instanceof DME2JDBCEndpoint )
            {
              if (finalURLString.startsWith("jdbc:dme2jdbc"))
              {
                finalURLString = finalURLString.substring(9);
              }
            }
          }
          else
          {
            finalURLString = ep.toURLString();
          }

          if(finalURLString.equals(preferredURL)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  public static void organizeEndpointsByPreferredURL(Map<Integer, Map<Double, DME2Endpoint[]>> sequenceToEndpointMap, String preferredURL, DME2EndpointURLFormatter urlFormatter)
  {
    DME2Endpoint endpointWithPreferredURL = null;
    boolean preferredURLFound = false;

    if(sequenceToEndpointMap != null)
    {
      Set<Integer> sequences = sequenceToEndpointMap.keySet();
      for(Integer sequence : sequences)
      {
        Map<Double, DME2Endpoint[]> distanceBandsToEndpoints = sequenceToEndpointMap.get(sequence);
        Set<Double> distanceBands = distanceBandsToEndpoints.keySet();

        for(Double distanceBand : distanceBands)
        {
          DME2Endpoint[] eps = distanceBandsToEndpoints.get(distanceBand);
          for(DME2Endpoint ep : eps)
          {
            String finalURLString = null;

            if(urlFormatter != null)
            {
              finalURLString = urlFormatter.formatURL(ep);
              if(ep instanceof DME2JDBCEndpoint)
              {
                if (finalURLString.startsWith("jdbc:dme2jdbc"))
                {
                  finalURLString = finalURLString.substring(9);
                }
              }

            }
            else
            {
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

      if(preferredURLFound && endpointWithPreferredURL != null)
      {
				/*Need to add this endpoint to the beginning of the list*/
        DME2Endpoint[] endpointArray = {endpointWithPreferredURL};

        Map<Double, DME2Endpoint[]> map = new HashMap<Double, DME2Endpoint[]>();
        map.put(0.0, endpointArray);
				 
				 /*Adding the above entry to the master collection under sequence -1*/
        sequenceToEndpointMap.put(-1, map);
      }
    }
  }

  private static boolean containsStaleRouteOffer(DME2RouteOffer offer, DME2Manager mgr)
  {
    logger.debug( null, "containsStaleRouteOffer","Checking for stale RouteOffers using search filter: {}", offer.getSearchFilter() );

    int staleCount = 0;
    String[] routeOffers = offer.getSearchFilter().split( DME2Constants.DME2_ROUTE_OFFER_SEP );
    for (String routeOffer : routeOffers)
    {
      String serviceURI = null;
      if (routeOffer.equals(DmeUniformResource.DmeUrlType.DIRECT.toString()))
        continue;
      serviceURI = DME2Utils.buildServiceURIString(offer.getService(), offer.getVersion(), offer.getEnvContext(), routeOffer);

      if (!mgr.isRouteOfferStale(serviceURI))
        continue;
      staleCount++;
    }

    if (staleCount == routeOffers.length)
    {
      String msg = String.format("All RouteOffers for service %s were previously marked stale. The number of RouteOffers in the search filter was: %s.", new Object[] { offer.getService(), Integer.valueOf(routeOffers.length) });
      logger.debug( null, "containsStaleRouteOffer", msg );
      return true;
    }

    return false;
  }
}
