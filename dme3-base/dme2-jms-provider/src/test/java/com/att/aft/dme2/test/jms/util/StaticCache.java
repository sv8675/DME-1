/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

public class StaticCache {
	
	private String routeOffer;
	private static StaticCache instance;
	private static boolean handleFaultInvoked=false;
	private static boolean handleEndpointFaultInvoked=false;
	private StaticCache() {
		
	}
	public static synchronized StaticCache getInstance() {
		if(instance == null)
			instance = new StaticCache();
		return instance;
	}
	
	public synchronized void setRouteOffer(String routeOffer) {
		this.routeOffer = routeOffer;
	}
	
	public String getRouteOffer(){
		return this.routeOffer;
	}
	public  boolean isHandleFaultInvoked() {
		return handleFaultInvoked;
	}
	public  void setHandleFaultInvoked(boolean handleFaultInvoked) {
		StaticCache.handleFaultInvoked = handleFaultInvoked;
	}
	public  boolean isHandleEndpointFaultInvoked() {
		return handleEndpointFaultInvoked;
	}
	public  void setHandleEndpointFaultInvoked(
			boolean handleEndpointFaultInvoked) {
		StaticCache.handleEndpointFaultInvoked = handleEndpointFaultInvoked;
	}
}

