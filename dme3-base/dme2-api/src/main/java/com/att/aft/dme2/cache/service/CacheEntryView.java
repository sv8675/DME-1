package com.att.aft.dme2.cache.service;

import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;

public interface CacheEntryView {
	public CacheElement getEntry(Key key);
}
