/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;


public class DME2CacheableCallbackTest<K,V> implements DME2CacheableCallback<K, V> {
	
	private Key<K> keyToChange = null;
	private CacheElement valueToReturn = null;
	
	public DME2CacheableCallbackTest(Key<K> keyToChange, CacheElement toBeValue){
		this.keyToChange =  keyToChange;
		this.valueToReturn = toBeValue;
	}
	
	@Override
	public CacheElement fetchFromSource(Key<K> requestValue) throws DME2Exception {
		if(requestValue.equals(keyToChange)){
			return valueToReturn;
		}
		return null;
	}

	@Override
	public Map<Key<K>, Pair<CacheElement, Exception>> fetchFromSource(Set<Key<K>> requestValues) {
		Map<Key<K>, Pair<CacheElement, Exception>> retMap = new HashMap<>();
		CacheElement v = null;
		Exception ex = null;
		for(Key<K> k : requestValues){
			try {
				v = fetchFromSource(k);
				retMap.put(k, new ImmutablePair<>(v, ex));
			} catch (DME2Exception e) {
				v = null;
				ex = e;
				e.printStackTrace();
			}
			retMap.put(k, new ImmutablePair<>(v, ex));
		}
		return retMap;
	}

	@Override
	public void refresh() {

	}

}
