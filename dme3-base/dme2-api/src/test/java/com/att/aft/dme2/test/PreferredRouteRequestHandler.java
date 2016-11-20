/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test;

import com.att.aft.dme2.api.util.DME2ExchangeRequestContext;
import com.att.aft.dme2.api.util.DME2ExchangeRequestHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.server.test.StaticCache;

public class PreferredRouteRequestHandler implements DME2ExchangeRequestHandler {
  private static final Logger logger = LoggerFactory.getLogger( PreferredRouteRequestHandler.class );

  @Override
  public void handleRequest( DME2ExchangeRequestContext requestData ) {
    logger.debug( null, "handleRequest", LogMessage.METHOD_ENTER );
    logger.debug( null, "handleRequest", "In PreferredRouteRequestHandler.handleRequest: requestData: " + requestData +
        ", StaticCache.getInstance().getRouteOffer(): " + StaticCache.getInstance().getRouteOffer() );
    if ( requestData != null ) {
      requestData.setPreferredRouteOffer( StaticCache.getInstance().getRouteOffer() );
    }
    logger.debug( null, "handleRequest", LogMessage.METHOD_EXIT );
  }

}
