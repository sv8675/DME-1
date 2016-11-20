package com.att.aft.dme2.cache.hz;

import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.service.CacheEntryView;
import com.hazelcast.core.IMap;

public class HzCacheEntryView implements CacheEntryView {
	
	private IMap<CacheElement.Key, CacheElement> cacheMap = null;
	
	public HzCacheEntryView(final IMap<CacheElement.Key, CacheElement> cacheMap){
		this.cacheMap = cacheMap;
	}

	@Override
	public CacheElement getEntry(Key key) {
		return cacheMap.get(key);
	}
	
}
