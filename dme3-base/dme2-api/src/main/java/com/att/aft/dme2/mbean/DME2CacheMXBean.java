package com.att.aft.dme2.mbean;


import com.att.aft.dme2.cache.DME2CacheStats;

public interface DME2CacheMXBean 
{
	public void clear();
	public int getCurrentSize();
	public long getCacheTTLValue(String key);
	public long getExpirationTime(String key);
	public String getKeys();
	public DME2CacheStats getStats(String serviceName, Integer hourOfDay);
	public void disableCacheStats();
	public void enableCacheStats();
	public boolean isCacheStatsEnabled();
}
