/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2ExchangeReplyHandler;
import com.att.aft.dme2.api.util.DME2ExchangeResponseContext;

/**
 * sample handler filling preferred version from cache
 */
public class PreferredVersionReplyHandler implements DME2ExchangeReplyHandler {
    @Override
    public void handleReply(DME2ExchangeResponseContext responseData) {
        if (responseData != null) {
            String preferredVersion = responseData.getVersion();
            System.out.println("Response data " + preferredVersion);
            if (preferredVersion != null) {
                StaticCache.getInstance().setPreferredVersion(preferredVersion);
            }
        }
    }

    @Override
    public void handleFault(DME2ExchangeFaultContext responseData) {
        StaticCache.getInstance().setHandleFaultInvoked(true);
    }

    @Override
    public void handleEndpointFault(DME2ExchangeFaultContext responseData) {
        StaticCache.getInstance().setHandleEndpointFaultInvoked(true);
    }
}
