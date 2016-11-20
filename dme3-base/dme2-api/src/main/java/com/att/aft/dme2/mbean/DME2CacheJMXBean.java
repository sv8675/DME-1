package com.att.aft.dme2.mbean;

import com.att.aft.dme2.cache.domain.CacheElement.Key;

public interface DME2CacheJMXBean {
	public void clear();
	public int getCurrentSize();
	public long getCacheEntryTTLValue(Key key);
	public long getCacheEntryExpirationTime(Key key);
	/**
	 * get all the keys for this cache
	 * @return
	 */
	public String getKeys();
}
