/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2Constants;

/**
 * Class that maintains statistics of cache refresh details for each service
 * Will be retained for life time of JVM as long as cache entries for service is retained
 * This object is not designed to be thread-safe for 2 reasons
 * 	1. CacheStats are specific to a single service
 * 	2. Refresh happens from one single thread and in sequential manner for all service
 *     No option for concurrent threads performing refresh at this time of design
 * @author t.sivanandham
 *
 */

public final class DME2CacheStatsHolder{
	
	private String serviceName;
	/*
	private long initTime;
	private long refreshCount;
	private long refreshSuccessCount;
	private long refreshFailedCount;
	private long refreshSuccessAvgElapsedTimeInMs;
	private long refreshFailedAvgElapsedTimeInMs;
	private long lastRefreshElapsedTimeInMs;
	private long lastSuccessAt;
	private long lastExceptionAt;
	private int lastRefreshHour; */
	DME2CacheStats stats = null;
	private final Map<Integer, DME2CacheStats> hourlyStats = Collections.synchronizedMap(new HashMap<Integer, DME2CacheStats>());
	private transient DME2Configuration config;
	
	public DME2CacheStatsHolder(String serviceName, final DME2Configuration config) {
		this.serviceName = serviceName;
		this.stats = new DME2CacheStats(this.serviceName);
		this.config = config;
	}
	
	/**
	 * DME2 cache refresh thread execution should be calling this on successful refresh
	 * 
	 * @param elapsedTimeInMs
	 */
	public void recordRefreshSuccess(long elapsedTimeInMs, boolean cacheStatsEnabled) {
		boolean configStatsEnabled = this.config.getBoolean(DME2Constants.Cache.DISABLE_CACHE_STATS, false);
		if(!cacheStatsEnabled || configStatsEnabled) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		int currentHr = cal.HOUR_OF_DAY;
		boolean hourRolled = ((this.stats.lastRefreshHour != currentHr)?true:false);
		this.stats.refreshCount++;
		this.stats.refreshSuccessAvgElapsedTimeInMs = ( (this.stats.refreshSuccessAvgElapsedTimeInMs * this.stats.refreshCount) + elapsedTimeInMs) / (this.stats.refreshCount+1) ;
		this.stats.refreshSuccessCount++;
		this.stats.lastSuccessAt = System.currentTimeMillis();
		this.stats.lastRefreshElapsedTimeInMs = elapsedTimeInMs;
		this.stats.lastRefreshHour = currentHr;
		recordCurrentHourStats(true,hourRolled,currentHr,elapsedTimeInMs);
	}
	
	public void recordRefreshFailure(long elapsedTimeInMs, boolean cacheStatsEnabled) {
		boolean configStatsEnabled = this.config.getBoolean(DME2Constants.Cache.DISABLE_CACHE_STATS, false);
		if(!cacheStatsEnabled || configStatsEnabled) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		int currentHr = cal.HOUR_OF_DAY;
		boolean hourRolled = ((this.stats.lastRefreshHour != currentHr)?true:false);
		this.stats.refreshCount++;
		this.stats.refreshFailedAvgElapsedTimeInMs = ( (this.stats.refreshFailedAvgElapsedTimeInMs * this.stats.refreshCount) + elapsedTimeInMs) / (this.stats.refreshCount+1) ;
		this.stats.refreshFailedCount++;
		this.stats.lastExceptionAt = System.currentTimeMillis();
		this.stats.lastRefreshElapsedTimeInMs = elapsedTimeInMs;
		this.stats.lastRefreshHour = currentHr;
		recordCurrentHourStats(false,hourRolled,currentHr,elapsedTimeInMs);
	}
	
	@Override
	public String toString(){
		return "CacheRefreshStats for service\t" + serviceName 
				+ "initialized at=\t" + new Date(this.stats.initTime)
				+ "stats retrieved at=\t" + new Date()
				+ "refreshCount=\t" + this.stats.refreshCount  
				+ "refreshSuccessCount=\t" + this.stats.refreshSuccessCount
				+ "refreshFailureCount=\t" + this.stats.refreshFailedCount
				+ "lastSuccessAt=\t" + new Date(this.stats.lastSuccessAt)
				+ "lastExceptionAt=\t" + new Date(this.stats.lastExceptionAt)
				+ "refreshSuccessAvgElapsedTimeInMs=\t" + this.stats.refreshSuccessAvgElapsedTimeInMs
				+ "refreshFailureAvgElapsedTimeInMs=\t" + this.stats.refreshFailedAvgElapsedTimeInMs
				+ "lastRefreshElapsedTimeInMs=\t" + this.stats.lastRefreshElapsedTimeInMs
				+ "lastRefreshWas=\t" + ( (this.stats.lastExceptionAt>this.stats.lastSuccessAt)?"FAILED":"SUCCESSFUL");
	}
	
	public DME2CacheStats getHourlyStats(int hourOfDay) {
		if(hourOfDay == 24) {
			return getLastDayStats();
		}
		DME2CacheStats stats = hourlyStats.get(hourOfDay);
		if(stats != null)
			return stats;
		return null;
	}
	
	public DME2CacheStats getStats() {
		return this.stats;
	}
	
	private DME2CacheStats getLastDayStats() {
		DME2CacheStats lastDayStats = null;
		try {
			Set<Integer> keys = hourlyStats.keySet();
			if (keys != null && keys.iterator() != null) {
				while (keys.iterator().hasNext()) {
					Iterator<Integer> it = keys.iterator();
					DME2CacheStats stats = hourlyStats.get(it.next());
					if(lastDayStats == null) {
						lastDayStats = stats;
					}
					else {
						this.addStats(stats);
					}
				}
			}
			if(lastDayStats != null) {
				return lastDayStats;
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
	
	private void recordCurrentHourStats(boolean successful,boolean hourRolledOver,int hourOfDay, long elapsedTimeInMs){
		DME2CacheStats stats = null;
		if(hourRolledOver){
			stats = new DME2CacheStats(this.serviceName);
			hourlyStats.put(hourOfDay, stats);
		}
		else{
			stats = hourlyStats.get(hourOfDay);
		}
		if(stats == null) {
			return;
		}
		stats.setRefreshSuccessAvgElapsedTimeInMs(( (stats.getRefreshSuccessAvgElapsedTimeInMs() * stats.getRefreshCount()) + elapsedTimeInMs) / (stats.getRefreshCount()+1)) ;
		stats.setRefreshCount(stats.getRefreshCount() + 1);
		stats.setLastRefreshElapsedTimeInMs(elapsedTimeInMs);
		stats.setLastRefreshHour(hourOfDay);
		if(successful) {
			stats.setRefreshSuccessCount(stats.getRefreshSuccessCount() + 1);
			stats.setLastSuccessAt(System.currentTimeMillis());
		}
		else {
			stats.setRefreshFailedCount(stats.getRefreshFailedCount() + 1);
			stats.setLastExceptionAt(System.currentTimeMillis());
		}
	}
	
	private DME2CacheStats addStats(DME2CacheStats other) {
		DME2CacheStats lstats = null;
		if(other != null) {
			lstats = new DME2CacheStats(other.getServiceName());
			lstats.refreshCount = this.stats.refreshCount + other.getRefreshCount();
			lstats.refreshSuccessCount = this.stats.refreshSuccessCount + other.getRefreshSuccessCount();
			lstats.refreshFailedCount = this.stats.refreshFailedCount + other.getRefreshFailedCount();
			lstats.lastSuccessAt = 0;
			lstats.refreshSuccessAvgElapsedTimeInMs = (this.stats.refreshSuccessAvgElapsedTimeInMs + other.getRefreshSuccessAvgElapsedTimeInMs())/2;
			lstats.refreshFailedAvgElapsedTimeInMs = (this.stats.refreshFailedAvgElapsedTimeInMs + other.getRefreshFailedAvgElapsedTimeInMs())/2;
			lstats.lastRefreshElapsedTimeInMs = 0;
		}
		else {
			return null;
		}
		return lstats;
	}
}

