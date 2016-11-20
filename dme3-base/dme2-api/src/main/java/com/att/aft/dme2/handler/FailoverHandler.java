package com.att.aft.dme2.handler;

import com.att.aft.dme2.api.DME2Response;

/*
 * This interface is used by clients to determine if fail over is required or not
 * 
 */
public interface FailoverHandler {

	public boolean isFailoverRequired(DME2Response dme2Response);

}