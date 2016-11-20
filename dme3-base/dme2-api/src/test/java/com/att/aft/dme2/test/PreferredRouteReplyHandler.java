/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2ExchangeReplyHandler;
import com.att.aft.dme2.api.util.DME2ExchangeResponseContext;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.server.test.StaticCache;

public class PreferredRouteReplyHandler implements DME2ExchangeReplyHandler {
  private static final Logger logger = LoggerFactory.getLogger( PreferredRouteReplyHandler.class );
  DME2ExchangeResponseContext responseDataFromDME2 = null;

	@Override
	public void handleReply(DME2ExchangeResponseContext responseData) {
		
		logger.debug( null, "handleReply", "In PreferredRouteReplyHandler.handleReply: responseData: " + responseData + ", StaticCache.getInstance().getRouteOffer(): " + StaticCache.getInstance().getRouteOffer());

		// TODO Auto-generated method stub
		if(responseData != null) {
					System.out.println("Response data " + responseData.getRouteOffer());
					if(responseData.getRouteOffer() != null ){
						if(responseData.getRouteOffer().equals("BAU_NE"))
							StaticCache.getInstance().setRouteOffer("BAU_SE");
						if(responseData.getRouteOffer().equals("BAU_SE"))
							StaticCache.getInstance().setRouteOffer("BAU_NW");
					}
		}

    logger.debug( null, "handleReply",
        "StaticCache.getInstance().getRouteOffer(): " + StaticCache.getInstance().getRouteOffer() );
	}

	@Override
	public void handleFault(DME2ExchangeFaultContext responseData) {
		// TODO Auto-generated method stub
    logger.debug( null, "handleFault", LogMessage.METHOD_ENTER );
        StaticCache.getInstance().setHandleFaultInvoked( true );
    logger.debug( null, "handleFault", LogMessage.METHOD_EXIT );
	}

	@Override
	public void handleEndpointFault(DME2ExchangeFaultContext responseData) {
		// TODO Auto-generated method stub
    logger.debug( null, "handleEndpointFault", LogMessage.METHOD_ENTER );
		StaticCache.getInstance().setHandleEndpointFaultInvoked(true);
    logger.debug( null, "handleEndpointFault", LogMessage.METHOD_EXIT );
	}

  public DME2ExchangeResponseContext getDME2ExchangeResponseContext() {
    return responseDataFromDME2;
  }
}

