/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry.util;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;

public class DME2RouteInfoTestUtil {
  public static DME2RouteInfo createDefaultRouteInfo() {
    try {
      return new DME2RouteInfo(null, new DME2Configuration(  ));
    } catch ( DME2Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
}
