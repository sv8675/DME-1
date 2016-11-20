/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.proxy;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.LoggerFactory;


public class DME2IngressContinuationListener implements ContinuationListener {
	private String msgId;
	private static final com.att.aft.dme2.logging.Logger logger = LoggerFactory.getLogger(DME2IngressContinuationListener.class.getName());
	HttpServletResponse resp;
	
	public DME2IngressContinuationListener(String msgId) {
		super();
		this.msgId = msgId;
	}

	public DME2IngressContinuationListener(String msgId, HttpServletResponse resp) {
		super();
		this.msgId = msgId;
		this.resp = resp;
	}

	@Override
	public void onComplete(Continuation arg0) 
	{
		logger.debug( null, "onComplete", "Continuation : {}", arg0);
		logger.debug( null, "onComplete", "msgId: {}", msgId);
	}

	@Override
	public void onTimeout(Continuation arg0) {
		try {
			logger.debug( null, "onTimeout", "msgId: {}", msgId );
			logger.debug( null, "onTimeout", "Continuation servletResponse: {}", arg0.getServletResponse() );
			HttpServletResponse resp = (HttpServletResponse)arg0.getServletResponse();
			resp.sendError(500);
			arg0.complete(); 
		}catch(Exception e) {
			//ignore any error in completing the continuation
			logger.error( null, "onTimeout", LogMessage.ON_TIMEOUT_EXCEPTION, msgId, e);
		}
	}

}
