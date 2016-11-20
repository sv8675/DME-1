package com.att.aft.dme2.server.cache;

import java.beans.ConstructorProperties;

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
public class DME2CacheStats
{
	String serviceName;
	long initTime;
	long refreshCount;
	long refreshSuccessCount;
	long refreshFailedCount;
	long refreshSuccessAvgElapsedTimeInMs;
	long refreshFailedAvgElapsedTimeInMs;
	long lastRefreshElapsedTimeInMs;
	long lastSuccessAt;
	long lastExceptionAt;
	long lastRefreshHour;
	
	public DME2CacheStats(String serviceName) {
		this.serviceName = serviceName;
		this.initTime = System.currentTimeMillis();
	}
	
	@ConstructorProperties({"serviceName","initTime","refresCount","refreshSuccessCount","refreshFailedCount","refreshSuccessAvgElapsedTimeInMs","refreshFailedAvgElapsedTimeInMs","lastRefreshElapsedTimeInMs","lastSuccessAt","lastExceptionAt","lastRefreshHour"})
	public DME2CacheStats(String serviceName,long initTIme, long refreshCount, long refreshFailedCount, long refreshSuccessAvgElapsedTimeInMs, long refreshFailedAvgElapsedTimeInMs, long lastRefreshElapsedTimeInMs, long lastSuccessAt,long lastExceptionAt, long lastRefreshHour ) {
		this.serviceName = serviceName;
		this.initTime = initTIme;
		this.refreshCount = refreshCount;
		this.refreshFailedCount = refreshFailedCount;
		this.refreshSuccessAvgElapsedTimeInMs = refreshSuccessAvgElapsedTimeInMs;
		this.refreshFailedAvgElapsedTimeInMs = refreshFailedAvgElapsedTimeInMs;
		this.lastRefreshElapsedTimeInMs = lastRefreshElapsedTimeInMs;
		this.lastSuccessAt = lastSuccessAt;
		this.lastExceptionAt = lastExceptionAt;
		this.lastRefreshHour = lastRefreshHour;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public long getInitTime() {
		return initTime;
	}
	public void setInitTime(long initTime) {
		this.initTime = initTime;
	}
	public long getRefreshCount() {
		return refreshCount;
	}
	public void setRefreshCount(long refreshCount) {
		this.refreshCount = refreshCount;
	}
	public long getRefreshSuccessCount() {
		return refreshSuccessCount;
	}
	public void setRefreshSuccessCount(long refreshSuccessCount) {
		this.refreshSuccessCount = refreshSuccessCount;
	}
	public long getRefreshFailedCount() {
		return refreshFailedCount;
	}
	public void setRefreshFailedCount(long refreshFailedCount) {
		this.refreshFailedCount = refreshFailedCount;
	}
	public long getRefreshSuccessAvgElapsedTimeInMs() {
		return refreshSuccessAvgElapsedTimeInMs;
	}
	public void setRefreshSuccessAvgElapsedTimeInMs(
			long refreshSuccessAvgElapsedTimeInMs) {
		this.refreshSuccessAvgElapsedTimeInMs = refreshSuccessAvgElapsedTimeInMs;
	}
	public long getRefreshFailedAvgElapsedTimeInMs() {
		return refreshFailedAvgElapsedTimeInMs;
	}
	public void setRefreshFailedAvgElapsedTimeInMs(
			long refreshFailedAvgElapsedTimeInMs) {
		this.refreshFailedAvgElapsedTimeInMs = refreshFailedAvgElapsedTimeInMs;
	}
	public long getLastRefreshElapsedTimeInMs() {
		return lastRefreshElapsedTimeInMs;
	}
	public void setLastRefreshElapsedTimeInMs(long lastRefreshElapsedTimeInMs) {
		this.lastRefreshElapsedTimeInMs = lastRefreshElapsedTimeInMs;
	}
	public long getLastSuccessAt() {
		return lastSuccessAt;
	}
	public void setLastSuccessAt(long lastSuccessAt) {
		this.lastSuccessAt = lastSuccessAt;
	}
	public long getLastExceptionAt() {
		return lastExceptionAt;
	}
	public void setLastExceptionAt(long lastExceptionAt) {
		this.lastExceptionAt = lastExceptionAt;
	}
	public long getLastRefreshHour() {
		return lastRefreshHour;
	}
	public void setLastRefreshHour(int lastRefreshHour) {
		this.lastRefreshHour = lastRefreshHour;
	}	
}