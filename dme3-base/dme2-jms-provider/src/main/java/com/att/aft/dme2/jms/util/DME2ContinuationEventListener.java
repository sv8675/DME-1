/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import javax.jms.JMSException;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;

import com.att.aft.dme2.jms.DME2JMSContinuationQueue;
import com.att.aft.dme2.jms.DME2JMSTextMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2ContinuationEventListener implements ContinuationListener {
	private DME2JMSContinuationQueue cQueue;
	private String msgId;
	private String reqQueue;
	private static final Logger logger = LoggerFactory.getLogger(DME2ContinuationEventListener.class.getName());

	public DME2ContinuationEventListener(DME2JMSContinuationQueue cQueue, String msgId, String reqQueue) {
		super();
		this.cQueue = cQueue;
		this.msgId = msgId;
		this.reqQueue = reqQueue;
	}

	@Override
	public void onComplete(Continuation arg0) {
		logger.debug(null, "onComplete", "DME2ContinuationEventListener onComplete invoked for correlationID",
				this.msgId);
	}

	@Override
	public void onTimeout(Continuation arg0) {
		logger.debug(null, "onComplete", "Continuation onTimeout invoked for correlationID: ", this.msgId);
		DME2JMSTextMessage errMsg = new DME2JMSTextMessage();
		try {
			errMsg.setText("");
			logger.debug(null, "onComplete", "DME2ContinuationEventListener timeout invoked for correlationID ",
					this.msgId);
			errMsg.setJMSCorrelationID(this.msgId);
			errMsg.setStringProperty("requestQueue", this.reqQueue);
			errMsg.setBooleanProperty("JMSXDME2ForceFailoverFlag", true);
			errMsg.setIntProperty("JMSXDME2ForceFailoverCode", -1);
			errMsg.setStringProperty("JMSXDME2ForceFailoverMessage",
					"Continuation timed out for correlationID:" + msgId);
			this.cQueue.put(errMsg);
		} catch (JMSException e) {
			logger.debug(null, "exception in onTimeout for ", this.msgId, e);
			logger.error(null, "onTimeout", JMSLogMessage.CONTINUATION_FAIL, this.msgId, e);

		}
		try {
			arg0.complete();
		} catch (Exception e) {
			// ignore any error in completing the continuation
			logger.error(null, "onTimeout", JMSLogMessage.CONTINUATION_FAIL, this.msgId, e);
		}
	}
}
