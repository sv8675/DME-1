/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.RequestContext;

/**
 * Facede class to process the send request with Payload
 * 
 *
 */
public class RequestFacade {
	RequestContext context = null;
	RequestProcessorIntf reqProcessor = null;
	
	public RequestFacade(RequestContext context, RequestProcessorIntf reqProcessor) {		
		this.context = context;
		this.reqProcessor = reqProcessor;
	}
	
	public void send(DME2Payload payload) throws DME2Exception {
		// validate the payload object and return exception if not valid
		RequestValidator.validate(payload);
		reqProcessor.send(context, payload);
	}
}