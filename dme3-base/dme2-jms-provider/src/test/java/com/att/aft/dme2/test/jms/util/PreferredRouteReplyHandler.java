/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2ExchangeReplyHandler;
import com.att.aft.dme2.api.util.DME2ExchangeResponseContext;

public class PreferredRouteReplyHandler implements DME2ExchangeReplyHandler {

	@Override
	public void handleReply(DME2ExchangeResponseContext responseData) {
		try {
			Thread.sleep(1000);
		} catch (Exception e) {

		}
		// TODO Auto-generated method stub
		if (responseData != null) {
			System.out.println("Response data " + responseData.getRouteOffer());
			if (responseData.getRouteOffer() != null) {
				if (responseData.getRouteOffer().equals("BAU_NE"))
					StaticCache.getInstance().setRouteOffer("BAU_SE");
				if (responseData.getRouteOffer().equals("BAU_SE"))
					StaticCache.getInstance().setRouteOffer("BAU_NW");
			}
		}
	}

	@Override
	public void handleFault(DME2ExchangeFaultContext responseData) {
		// TODO Auto-generated method stub
		StaticCache.getInstance().setHandleFaultInvoked(true);
	}

	@Override
	public void handleEndpointFault(DME2ExchangeFaultContext responseData) {
		// TODO Auto-generated method stub
		StaticCache.getInstance().setHandleEndpointFaultInvoked(true);
	}

}
