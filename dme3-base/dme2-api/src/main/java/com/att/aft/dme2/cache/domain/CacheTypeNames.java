/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.domain;

/**
 * 
 * CacheTypeNames that are currently available for initialization.<br> 
 * <ul>
 * 	<li>EndpointCache</li>
 * 	<li>RouteOfferCache</li>
 * 	<li>StaleEndpointCache</li>
 * 	<li>StaleRouteOfferCache</li>
 * 	<li>Default</li>
 * </ul>
 * "Default" would be for custom cache; this is yet to be implemented
 *
 */
public enum CacheTypeNames {
	EndpointCache,
	RouteOfferCache,
	StaleEndpointCache,
	StaleRouteOfferCache,
	Default; 
}
