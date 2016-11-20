/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import com.att.aft.dme2.api.util.DME2ExchangeRequestContext;
import com.att.aft.dme2.api.util.DME2ExchangeRequestHandler;

public class PreferredRouteRequestHandler implements DME2ExchangeRequestHandler {
	@Override
	public void handleRequest(DME2ExchangeRequestContext requestData) {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(1000);
		} catch (Exception e) {

		}
		if (requestData != null)
			requestData.setPreferredRouteOffer(StaticCache.getInstance().getRouteOffer());
	}

}
