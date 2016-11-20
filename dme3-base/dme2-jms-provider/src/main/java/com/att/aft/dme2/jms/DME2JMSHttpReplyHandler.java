/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.att.aft.dme2.api.DME2ReplyHandler;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/**
 * Convert HTTP Replies into JMS Messages and put on correct local queue.
 */

@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class DME2JMSHttpReplyHandler implements DME2ReplyHandler,AsyncResponseHandlerIntf {

	private static final Logger logger = LoggerFactory.getLogger(DME2JMSHttpReplyHandler.class.getName());
	private final DME2JMSManager manager;
	private DME2Configuration config;
	
	protected DME2JMSHttpReplyHandler(DME2JMSManager manager) {
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();
	}

	@Override
	public void handleException(Map<String, String> requestHeaders, Throwable e) {
		logger.debug(null, "handleException", LogMessage.METHOD_ENTER);
		String replyToQueue = null;
		try {
			replyToQueue = requestHeaders.get("JMSReplyTo");
			if (replyToQueue == null) {
				replyToQueue = requestHeaders.get(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSReplyTo");
			}
			// for one-way, no handling of replies
			if (replyToQueue != null) {

				replyToQueue = "http://DME2LOCAL" + replyToQueue;

				DME2JMSMessage m = manager.createErrorMessage(e, requestHeaders);

				// The replyQueue should have been created already and available
				// in cache
				// Invoking getQueueFromCache to avoid creating replyQueue that
				// would have been
				// deleted already due to response timedout scenario.
				DME2JMSQueue queue = manager.getQueueFromCache(replyToQueue);

				if (queue == null) {
					logger.error(null, "handleException", "AFT-DME2-5300", new ErrorContext()
							.add("JMSDestination", replyToQueue).add("extendedMessage", e.getMessage()), e);
				} else {
					if (m != null) {
						queue.put(m);
					}
				}
			}
		} catch (Throwable x) {
			logger.error(null, "handleException", "AFT-DME2-5301",
					new ErrorContext().add("JMSDestination", replyToQueue).add("extendedMessage", x.getMessage()), x);
		} finally {
			logger.debug(null, "handleException", LogMessage.METHOD_EXIT);
		}
	}

	@Override
	public void handleReply(int rc, String rm, InputStream in, Map<String, String> requestHeaders,
			Map<String, String> responseHeaders) {
		logger.debug(null, "handleReply", LogMessage.METHOD_ENTER);
		String replyToQueue = null;
		try {
			Map<String, String> finalResponseHeaders = new HashMap<String, String>();

			for (String key : responseHeaders.keySet()) {
				if (key != null) {
					finalResponseHeaders.put(key, responseHeaders.get(key));
				}
			}

			// needed for compliance with SOAP/JMS spec
			if (!finalResponseHeaders.containsKey("Content-Type")) {
				finalResponseHeaders.put("Content-Type", "text/xml");
				if (requestHeaders.containsKey("SOAPJMS_bindingVersion")
						|| requestHeaders.containsKey("SOAPJMS_contentType")) {
					finalResponseHeaders.put("SOAPJMS_contentType", "text/xml");
					if (!finalResponseHeaders.containsKey("SOAPJMS_requestURI")) {
						finalResponseHeaders.put("SOAPJMS_requestURI", "DummyValueAddedByDME2");
					}
				}
			}

			replyToQueue = finalResponseHeaders.get(DME2Constants.JMSDESTINATION);
			if(replyToQueue == null){
				replyToQueue = finalResponseHeaders.get(config.getProperty(DME2Constants.DME2_HEADER_PREFIX).concat(DME2Constants.JMSDESTINATION));
			}

			// for one-way, no handling of replies
			if (replyToQueue != null) {

				replyToQueue = "http://DME2LOCAL" + replyToQueue;

				DME2JMSMessage m = manager.createMessage(in, finalResponseHeaders);
				// The replyQueue should have been created already and available
				// in cache
				// Invoking getQueueFromCache to avoid creating replyQueue that
				// would have been
				// deleted already due to response timedout scenario.
				DME2JMSQueue queue = manager.getQueueFromCache(replyToQueue);

				if (queue == null) {
					logger.error(null, "handleReply", "AFT-DME2-5300",
							new ErrorContext().add("JMSDestination", replyToQueue));
				} else {
					queue.put(m);
				}
			}
		} catch (Throwable e) {
			logger.error(null, "hanleReply", "AFT-DME2-5302", new ErrorContext().add("JMSDestination", replyToQueue),
					e);

		} finally {
			logger.debug(null, "handleReply", LogMessage.METHOD_EXIT);
		}
	}
	
	@Override
	public String getResponse(long timeoutMs) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}	

}
