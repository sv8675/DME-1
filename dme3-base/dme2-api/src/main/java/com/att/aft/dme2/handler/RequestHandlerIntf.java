package com.att.aft.dme2.handler;

import com.att.aft.dme2.request.Request;

public interface RequestHandlerIntf {
	abstract String getPreferredRouteOffer( final Request request );
}
