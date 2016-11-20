/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * this class is used to store users application state (basic simulation of
 * storage)
 *
 */
public class StaticCache {

    private static StaticCache instance;
    private static boolean handleFaultInvoked = false;
    private static boolean handleEndpointFaultInvoked = false;

    private String routeOffer;
    private String preferredVersion;
    
    private Map<String, Object> values = new HashMap<String, Object>();

    private StaticCache() {

    }

    public static synchronized StaticCache getInstance() {
        if (instance == null)
            instance = new StaticCache();
        return instance;
    }

    public boolean isHandleFaultInvoked() {
        return handleFaultInvoked;
    }

    public void setHandleFaultInvoked(boolean handleFaultInvoked) {
        StaticCache.handleFaultInvoked = handleFaultInvoked;
    }

    public boolean isHandleEndpointFaultInvoked() {
        return handleEndpointFaultInvoked;
    }

    public void setHandleEndpointFaultInvoked(boolean handleEndpointFaultInvoked) {
        StaticCache.handleEndpointFaultInvoked = handleEndpointFaultInvoked;
    }

    public static void reset() {
        instance = null;
    }

    public String getRouteOffer() {
        return this.routeOffer;
    }

    public synchronized void setRouteOffer(String routeOffer) {
        this.routeOffer = routeOffer;
    }

    public String getPreferredVersion() {
        return preferredVersion;
    }

    public synchronized void setPreferredVersion(String preferredVersion) {
        this.preferredVersion = preferredVersion;
    }
    
    public Object get(String key) {
        return values.get(key);
    }
    
    public void put(String key, Object value) {
        values.put(key, value);
    }
}