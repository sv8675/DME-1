/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2FailoverFaultHandler;

public class TestDME2FailoverFaultHandler implements DME2FailoverFaultHandler {

    @Override
    public void handleEndpointFailover(DME2ExchangeFaultContext context) {
        StaticCache.getInstance().put("handleEndpointFailover", true);
        StaticCache.getInstance().put("DME2ExchangeFaultContext", context);
    }

    @Override
    public void handleRouteOfferFailover(DME2ExchangeFaultContext context) {
        StaticCache.getInstance().put("handleRouteOfferFailover", true);
        StaticCache.getInstance().put("DME2ExchangeFaultContext", context);
    }

}
