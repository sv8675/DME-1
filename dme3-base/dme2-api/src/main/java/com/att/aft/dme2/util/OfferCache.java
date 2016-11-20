package com.att.aft.dme2.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OfferCache {
	private static Map<String,Long> staleOffers = Collections.synchronizedMap(new HashMap<String,Long>());
	// Time in milliseconds
	private static long stalenessTime = 0;
	private static OfferCache instance = new OfferCache();

	/**
	 * 
	 */
	private OfferCache() {
		try {
			String stalenessTimeStr = System.getProperty(
					"DME2_OFFER_STALENESS", "5");
			stalenessTime = Long.parseLong(stalenessTimeStr) * 60 * 1000;
		} catch (NumberFormatException ne) {
			// Defaulting to 5 mins
			stalenessTime = 5 * 60 * 1000;
		}
	}

	/**
	 * 
	 * @return
	 */
	public static OfferCache getInstance() {
		if (instance == null) {
			synchronized(staleOffers) {
				if (instance == null) {
					instance = new OfferCache();
				}
			}
		}
		return instance;
	}

	/**
	 * 
	 * @param offer
	 */
	public synchronized void setStale(String offer) {
		if (staleOffers != null) {
			if (staleOffers.get(offer) == null) {
				staleOffers.put(offer, new Long(System.currentTimeMillis()));
			}
		}
	}

	/**
	 * 
	 * @param offer
	 */
	public synchronized void removeStaleness(String offer) {
		if (staleOffers != null) {
			if (staleOffers.get(offer) != null) {
				staleOffers.remove(offer);
			}
		}
	}

	/**
	 * 
	 * @param offers
	 */
	public synchronized void removeStaleness(List<String> offers) {
		if (staleOffers != null) {
			Iterator<String> it = offers.iterator();
			while (it.hasNext()) {
				String offer = it.next();
				if (staleOffers.get(offer) != null) {
					staleOffers.remove(offer);
				}
			}
		}
	}

	/**
	 * 
	 * @param offer
	 * @return
	 */
	public boolean isStale(String offer)
	{
		if (staleOffers != null && offer != null)
		{
			List<String> offerList = new ArrayList<String>();
			synchronized(staleOffers){
				offerList.addAll(staleOffers.keySet());
			}
			for(String staleOffer: offerList)
			{
				if (staleOffer.equalsIgnoreCase(offer))
				{
					Long time = staleOffers.get(offer);
					if(time == null){
						return false;
					}
					
					long currentTime = System.currentTimeMillis();
					if ((currentTime - time.longValue()) < stalenessTime)
					{
						return true;
					}
					else
					{
						removeStaleness(offer);
						return false;
					}
				}
			}
		}
		
		return false;
	}
}
