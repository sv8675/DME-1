package com.att.aft.dme2.cache.service;

import com.att.aft.dme2.config.DME2Configuration;

public interface CacheSerialization {
	/**
	 * persist the entries of the cache into flat file
	 * @param cache of which the entries has to be persisted
	 * @param config configuration details from which required information has to be retrieved  
	 * @return true if success, otherwise fail
	 */
	public boolean persist(DME2Cache cache, DME2Configuration config);
	
	/**
	 * on system startup warm up the cache from the persisted file   
	 * @param cache cache to be warmed up 
	 * @param config configuration details from which required information has to be retrieved
	 * @return true if success, otherwise fail
	 */
	public boolean load(DME2Cache cache, DME2Configuration config);
	
	public boolean isStale(DME2Cache cache, DME2Configuration config);
}
