/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;


public interface  DME2ExchangeReplyHandler {

	abstract  void handleReply( DME2ExchangeResponseContext responseData );
	
	abstract  void handleFault( DME2ExchangeFaultContext responseData );
	
	abstract  void handleEndpointFault( DME2ExchangeFaultContext responseData );
}
