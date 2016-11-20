/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

public interface AsyncRequestProcessorHandlerIntf {
	public void onConnectionFailed( Throwable x ) throws DME2Exception;
	public void onException( Throwable x ) throws DME2Exception;
	public void onExpire() throws DME2Exception;
}
