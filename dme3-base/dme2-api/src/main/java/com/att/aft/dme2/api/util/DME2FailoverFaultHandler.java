/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

/**
 * 
 * This interface is should be implemented and the class registered as a handler
 * to be invoked in the event of endpoint failovers and route offer failovers
 *
 */

public interface DME2FailoverFaultHandler {
	public void handleEndpointFailover(DME2ExchangeFaultContext context);

	public void handleRouteOfferFailover(DME2ExchangeFaultContext context);
}
