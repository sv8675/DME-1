package com.att.aft.dme2.cache.legacy;

import java.util.Map;

import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.service.CacheEntryView;

public class DefaultCacheEntryView implements CacheEntryView {
	
	private Map<Key, CacheElement> cacheMap = null;
	
	public DefaultCacheEntryView(final Map<Key, CacheElement> cacheMap){
		this.cacheMap = cacheMap;
	}

	@Override
	public CacheElement getEntry(Key key) {
		return cacheMap.get(key);
	}

}
