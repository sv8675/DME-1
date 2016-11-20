/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.domain;

public enum CacheEventType 
{
	//PUT("EVENT_PUT"),
	//UPDATE("EVENT_UPDATE"),
	//REMOVE("EVENT_REMOVE"),
	//REFRESH("EVENT_REFRESH"),
	//BEFORE_REMOVE("INTERCEPTING_BEING_REMOVED"),
	//AFTERGET("INTERCEPTING_AFTER_GET"),
	EVICT("EVENT_EVICT");

	private String value;
	private CacheEventType(String value) 
	{
		this.value = value;
	}
	public String getValue() 
	{
		return value;
	}
}
