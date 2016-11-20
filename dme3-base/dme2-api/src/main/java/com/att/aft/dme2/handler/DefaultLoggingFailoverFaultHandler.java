package com.att.aft.dme2.handler;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2FailoverFaultHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DefaultLoggingFailoverFaultHandler implements DME2FailoverFaultHandler {
	/** The logger. */
	 private static Logger logger = LoggerFactory.getLogger(DefaultLoggingFailoverFaultHandler.class.getName());
	
	@Override
	public void handleEndpointFailover(DME2ExchangeFaultContext context) {
		logger.warn(null, "handleEndpointFailover", LogMessage.SEP_FAILOVER, context.getService(),context.getRequestURL(),context.getRouteOffer(),context.getResponseCode(),context.getException());
	}

	@Override
	/**
	 * The DME2Exchange already has a log message when the route offer is failed over. We dont need to log it again here.
	 */
	public void handleRouteOfferFailover(DME2ExchangeFaultContext context) {
		//noop		
	}

}
