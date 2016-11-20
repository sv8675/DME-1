/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.domain;

public class CacheEvent {

	private CacheElement cacheNewElement;
	private CacheElement cacheOldElement;
	private CacheEventType cacheEventType;
	
	public CacheEvent() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the cacheEventType
	 */
	public CacheEventType getCacheEventType() {
		return cacheEventType;
	}

	/**
	 * @param cacheEventType the cacheEventType to set
	 */
	public void setCacheEventType(CacheEventType cacheEvent) {
		this.cacheEventType = cacheEvent;
	}

	/**
	 * @return the cacheNewElement
	 */
	public CacheElement getCacheNewElement() {
		return cacheNewElement;
	}

	/**
	 * @param cacheNewElement the cacheNewElement to set
	 */
	public void setCacheNewElement(CacheElement cacheNewElement) {
		this.cacheNewElement = cacheNewElement;
	}

	/**
	 * @return the cacheOldElement
	 */
	public CacheElement getCacheOldElement() {
		return cacheOldElement;
	}

	/**
	 * @param cacheOldElement the cacheOldElement to set
	 */
	public void setCacheOldElement(CacheElement cacheOldElement) {
		this.cacheOldElement = cacheOldElement;
	}

}
