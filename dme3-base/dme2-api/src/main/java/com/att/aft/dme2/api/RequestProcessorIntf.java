/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.RequestContext;

public interface RequestProcessorIntf {
	public void send(RequestContext context, DME2Payload payload) throws DME2Exception;

	public boolean send(RequestContext context, DME2EndpointReference endpoint, DME2Payload payload) throws DME2Exception;

	public void sendAndWait(RequestContext context, DME2Payload payload) throws DME2Exception;

	public void retry(RequestContext context, DME2Payload payload) throws DME2Exception;

	// this will get invoked by the retry thread pool when the request is
	// resubmitted due to failover
	public void failover(RequestContext context, DME2Payload payload) throws DME2Exception;
}
