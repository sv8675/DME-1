package com.att.aft.dme2.util;

/**
 * put name of all argument parameters need to pass to the application in here. this would be an easy one place location to see what are configuration parameters for DME2
 * 
 * @author ar671m
 *
 */
public class DME2ParameterNames {
    public final static String AFT_DME2_USE_AFT_DISCOVERY = "AFT_DME2_USE_AFT_DISCOVERY";
    public final static String GRM_SERVER_PROTOCOL = "DME2_GRM_SERVER_PROTOCOL";
    public final static String GRM_SERVER_PORT = "DME2_GRM_SERVER_PORT";
    public final static String GRM_SERVER_PATH = "DME2_GRM_SERVER_PATH";
    public final static String GRM_SERVICE_NAME = "DME2_GRM_SERVICE_NAME";
    public final static String GRM_ENVIRONMENT = "GRM_ENVIRONMENT";
    public final static String GRM_SERVICE_VERSION = "DME2_GRM_SERVICE_VERSION";
    public final static String GRM_DNS_BOOTSTRAP = "DME2_GRM_DNS_BOOTSTRAP";
    public final static String GRM_STATIC_ENDPOINT = "GRM_STATIC_ENDPOINT";
    public final static String OVERRIDE_GRM_SERVER_PATH = "DME2_OVERRIDE_GRM_SERVER_PATH";
    
    
    public final static String GRM_EDGE_DIRECT_HOST = "GRM_EDGE_DIRECT_HOST";
    public final static String GRM_EDGE_NODE_PORT = "GRM_EDGE_NODE_PORT";
    public final static String GRM_EDGE_CONTEXT_PATH = "GRM_EDGE_CONTEXT_PATH";
    public final static String GRM_EDGE_CUSTOM_DNS = "GRM_EDGE_CUSTOM_DNS";
    public final static String AFT_DME2_GRM_URLS = "AFT_DME2_GRM_URLS";
    
    
    // GRM Cache
    public final static String GRM_SERVER_CACHE_FILE = "GRM_SERVER_CACHE_FILE";
    public final static String GRM_SERVER_CACHE_FILE_DEFAULT = "etc/dme2grmendpoints.txt";
    public final static String CACHE_REFRESH_INTERVAL_MS = "GRM_ENDPOINT_CACHE_REFRESH_INTERVAL_MS";
    public final static String CACHE_REFRESH_START_DELAY_MS = "GRM_ENDPOINT_CACHE__REFRESH_START_DELAY_MS";

    private DME2ParameterNames() {
    }
}
