/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import com.att.aft.dme2.api.util.DME2ExchangeRequestContext;
import com.att.aft.dme2.api.util.DME2ExchangeRequestHandler;

/**
 * sample handler saving preferred version in the url in memory for unit testing
 */
public class PreferredVersionRequestHandler implements DME2ExchangeRequestHandler {
    @Override
    public void handleRequest(DME2ExchangeRequestContext requestData) {
        if (requestData != null)
            requestData.setPreferredVersion(StaticCache.getInstance().getPreferredVersion());
    }
}
