/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;


public interface DME2ExchangeRequestHandler {
	abstract  void handleRequest( DME2ExchangeRequestContext requestData );
}
