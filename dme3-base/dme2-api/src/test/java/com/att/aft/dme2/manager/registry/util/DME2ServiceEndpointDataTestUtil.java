/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry.util;

import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2ServiceEndpointData;

public class DME2ServiceEndpointDataTestUtil {
  public static final List<DME2Endpoint> DEFAULT_ENDPOINT_LIST = DME2EndpointTestUtil.createDefaultDME2EndpointList();
  public static final String DEFAULT_SERVICE_URI = RandomStringUtils.randomAlphanumeric( 30 );
  public static final long DEFAULT_CACHE_TTL = RandomUtils.nextLong();
  public static final long DEFAULT_LAST_QUERIED = RandomUtils.nextLong();

  public static DME2ServiceEndpointData createDefaultServiceEndpointData() {
    return createServiceEndpointData( DEFAULT_ENDPOINT_LIST, DEFAULT_SERVICE_URI, DEFAULT_CACHE_TTL, DEFAULT_LAST_QUERIED );
  }

  public static DME2ServiceEndpointData createServiceEndpointData( List<DME2Endpoint> endpointList, String serviceUri,
                                                                    long cacheTtl, long lastQueried ) {
    return new DME2ServiceEndpointData( endpointList, serviceUri, cacheTtl, lastQueried );
  }
}
