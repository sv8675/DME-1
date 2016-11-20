/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test;

import java.util.Map;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2ExchangeReplyHandler;
import com.att.aft.dme2.api.util.DME2ExchangeRequestContext;
import com.att.aft.dme2.api.util.DME2ExchangeRequestHandler;
import com.att.aft.dme2.api.util.DME2ExchangeResponseContext;

public class MessageLoggerHandler implements DME2ExchangeReplyHandler, DME2ExchangeRequestHandler {

	@Override
	public void handleRequest(DME2ExchangeRequestContext requestData) {
		// TODO Auto-generated method stub
		System.out.println("MessageLoggerHandler.handleRequest invoked");
		if (requestData != null)
			requestData.setPreferredRouteOffer(null);
		if (requestData.getRequestHeaders() != null) {
			Map<String,String> reqHdr = requestData.getRequestHeaders();
			String temp = reqHdr.get("AFT_DME2_TEST_THROW_EXCEPTION");
			if(temp != null) {
				boolean throwException = Boolean.parseBoolean(temp);
				if(throwException) {
					// Throwing this exception should not cause DME2 request/reply flow to fail
					throw new RuntimeException("ForcedException for failure scenario");
				}
			}
		}
	}

	@Override
	public void handleEndpointFault(DME2ExchangeFaultContext arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleFault(DME2ExchangeFaultContext arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleReply(DME2ExchangeResponseContext arg0) {
		// TODO Auto-generated method stub
		System.out.println("MessageLoggerHandler.handleReply url="
				+ arg0.getRequestURL() + ";routeOffer=" + arg0.getRouteOffer());
	}

}