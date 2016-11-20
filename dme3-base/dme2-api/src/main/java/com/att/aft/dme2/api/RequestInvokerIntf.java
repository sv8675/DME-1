/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.RequestContext;
import com.att.aft.dme2.util.InternalConnectionFailedException;

public interface RequestInvokerIntf {
	public void init(RequestContext context, ActionType action, DME2Payload payload) throws DME2Exception;
	public void execute(ActionType action, RequestContext context, DME2Payload payload) throws InternalConnectionFailedException, DME2Exception;
	public void createExchange(String resolvedUrl, RequestContext context, DME2BaseEndpointIterator iterator) throws DME2Exception;
}
