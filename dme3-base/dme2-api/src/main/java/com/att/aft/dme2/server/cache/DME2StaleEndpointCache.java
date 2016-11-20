package com.att.aft.dme2.server.cache;

import static com.att.aft.dme2.logging.LogMessage.METHOD_ENTER;
import static com.att.aft.dme2.logging.LogMessage.METHOD_EXIT;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public abstract class DME2StaleEndpointCache implements DME2Cache
{
	private final Map<String, Long> cache = new ConcurrentHashMap<String, Long>(16, 0.9f, 1);
	private static final Logger logger = LoggerFactory.getLogger( DME2StaleEndpointCache.class );
	private final byte[] lock = new byte[0];
	private DME2Configuration config;
	
	Timer staleEndpointCleanupTimer;
	private boolean enableCacheStats;
	
	public DME2StaleEndpointCache(DME2Configuration config)
	{
		this.config = config;
	}
	
	public DME2Cache getCache()
	{
		return this;
	}

	
	@Override
	public void refresh()
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public void clear()
	{
		logger.debug( null, "clear", METHOD_ENTER );
		synchronized(lock)
		{
			cache.clear();
		}
		logger.debug( null, "clear", METHOD_EXIT );
	}


	@Override
	public int getCurrentSize()
	{
		return cache.size();
	}

	@Override
	public String getKeys()
	{
		return cache.keySet().toString();
	}


	@Override
	public void shutdownTimerTask()
	{
		if(staleEndpointCleanupTimer != null){
			staleEndpointCleanupTimer.cancel();
		}
	}
}