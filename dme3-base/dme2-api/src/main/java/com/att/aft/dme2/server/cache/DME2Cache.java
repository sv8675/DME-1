package com.att.aft.dme2.server.cache;

public interface DME2Cache
{
	public void initialize();
	public void refresh();
	public void shutdownTimerTask();
	DME2CacheStats getStats( String serviceName, Integer hourOfDay );
	void disableCacheStats();
	void enableCacheStats();
	boolean isCacheStatsEnabled();
}
