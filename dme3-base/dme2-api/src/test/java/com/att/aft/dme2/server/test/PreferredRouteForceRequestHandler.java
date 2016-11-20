/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import com.att.aft.dme2.api.util.DME2ExchangeRequestContext;
import com.att.aft.dme2.api.util.DME2ExchangeRequestHandler;

public class PreferredRouteForceRequestHandler implements DME2ExchangeRequestHandler
{
	@Override
	public void handleRequest(DME2ExchangeRequestContext requestData)
	{
		if (requestData != null) {
			requestData.setPreferredRouteOffer(StaticCache.getInstance().getRouteOffer());
			requestData.setForcePreferredRouteOffer(true);
		}
	}

}