/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverEndpoint;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;

/*
 * DefailtFailover End point implementation class.
 * Determines the next failover end point based on the retryurl and endpoint iterator
 */
public class DME2DefaultFailoverEndpoint implements FailoverEndpoint {
	private static DME2Configuration config;

	public DME2DefaultFailoverEndpoint(DME2Configuration configuration) {
		config = configuration;

	}

	/*
	 * Retrieves the next fail over end point for retryurl=true Iterates through
	 * the endpoint iterator and fetch the next fail over end point.
	 */
	@Override
	public DME2EndpointReference getNextFailoverEndpoint(DME2BaseEndpointIterator iterator, boolean retryCurrentlUrl) {
		if (!retryCurrentlUrl && null != iterator) {
			while (iterator.hasNext()) {
				DME2EndpointReference next = iterator.next();
				return next;
			}
		}
		return null;
	}

}
