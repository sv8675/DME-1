package com.att.aft.dme2.server.cache;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.att.aft.dme2.server.mbean.DME2CacheMXBean;

public class DME2StaleRouteOfferCache implements DME2Cache, DME2CacheMXBean
{

	private final Map<String, Long> cache = new ConcurrentHashMap<String, Long>();
	private final byte[] lock = new byte[0];
	
	Timer staleRouteOfferCleanupTimer;
	private boolean enableCacheStats;
	
	public DME2StaleRouteOfferCache()
	{
		initialize();
	}
	
	public Map<String, Long> getCache()
	{
		return cache;
	}
	
	@Override
	public void clear()
	{
		synchronized(lock)
		{
			cache.clear();
		}
		
	}

	@Override
	public int getCurrentSize()
	{
		return cache.size();
	}

	@Override
	public long getCacheTTLValue(String key)
	{
		/* Operation not used for this cache */
		return 0;
	}

	@Override
	public long getExpirationTime(String key)
	{
		if(cache.get(key) != null)
		{
			return cache.get(key);
		}
		return 0;
	}

	@Override
	public String getKeys()
	{
		return cache.keySet().toString();
	}

	@Override
	public void initialize()
	{
		/* Schedule timer to throw out expired staleness entries in local cache every 15 mins */
		
		staleRouteOfferCleanupTimer = new Timer("DME2StaleEndpointCache::StaleRouteOfferCleanupTimer", true);
		staleRouteOfferCleanupTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				for (String key : cache.keySet())
				{
					long expirationTime = cache.get(key);
					if (System.currentTimeMillis() > expirationTime)
					{
						cache.remove(key);
					}
				}
			}
		}, 60000, 60000);
		
	}

	@Override
	public void refresh()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdownTimerTask()
	{
		if(staleRouteOfferCleanupTimer != null){
			staleRouteOfferCleanupTimer.cancel();
		}
		
	}

	@Override
	public DME2CacheStats getStats(String serviceName, Integer hourOfDay) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disableCacheStats() {
		enableCacheStats = false;
	}

	@Override
	public void enableCacheStats() {
		enableCacheStats = true;
	}

	@Override
	public boolean isCacheStatsEnabled() {
		return enableCacheStats;
	}
}
