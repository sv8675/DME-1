/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import static com.att.aft.dme2.logging.LogMessage.METHOD_ENTER;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.Message;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.att.aft.dme2.api.DME2Server;
import com.att.aft.dme2.api.util.DME2MessageHeaderUtils;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.instrument.JMSEventSampler;
import com.att.aft.dme2.jms.util.DME2ContinuationEventListener;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private final DME2JMSManager manager;
	private Integer maxRetry = 0;
	private static final long SLEEP_INTERVAL = 5L;
	private static final int TIMEOUT_5 = 5;
	private static final int TIMEOUT_60 = 60;
	private static final long TIMEOUT_1000L = 1000L;
	private static final int DME2_RECEIVE_MAX_RETRY = 3;

	private static final int CONTENT_LENGTH = 100;
	private static final int JMSXDME2FORCEFAILOVERCODE = 222;

	// Sleep time in ms between retry msg being submitted
	private Long maxSleep = SLEEP_INTERVAL;
	private static final String CLASS_NAME = "com.att.aft.dme2.jms.DME2JMSServlet";
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSServlet.class.getName());
	private Long continuationTimeout = TIMEOUT_5 * TIMEOUT_60 * TIMEOUT_1000L;

	private static final String EVENT = "Event";
	private static final String RESULT = "Result";
	private static final String REASONCODE = "ReasonCode";
	private static final String FAULT = "Fault";
	private static final String EXTENDEDMESSAGE = "extendedMessage";

	private DME2Configuration config;

	protected DME2JMSServlet(DME2JMSManager manager) {
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();
		try {
			maxRetry = config.getInt(JMSConstants.AFT_DME2_RECEIVE_MAX_RETRY);
			if (maxRetry == null) {
				maxRetry = config.getInt(JMSConstants.DME2_RECEIVE_MAX_RETRY);
			}
			maxSleep = config.getLong(JMSConstants.AFT_DME2_RETRY_SLEEP);
			if (maxSleep == null) {
				maxSleep = config.getLong(JMSConstants.DME2_RETRY_SLEEP);
			}

			continuationTimeout = config.getLong(JMSConstants.AFT_DME2_CONT_TIMEOUT, continuationTimeout);
			if (continuationTimeout == null) {
				continuationTimeout = config.getLong("AFT_DME2_CONT_TIMEOUT");
			}
		} catch (Exception e) {
			maxRetry = DME2_RECEIVE_MAX_RETRY;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doHead(javax.servlet.http.
	 * HttpServletRequest , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Set the content length and type
		resp.setContentLength(CONTENT_LENGTH);
		resp.setContentType("text/html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doHead(javax.servlet.http.
	 * HttpServletRequest , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.
	 * HttpServletRequest , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.debug( null, "doPost", METHOD_ENTER );
		String serviceName = null;
		String version = null;
		String contextPath = null;
		String endpoint = req.getServerName() + ":" + req.getServerPort() + ":" + req.getServletPath();
		String requestPartnerName = null;

		try {
			DME2Server server = this.manager.getDME2Manager().getServer();
			String healthCheck = req.getHeader("DME2HealthCheck");

			if (healthCheck != null) {
				if (server.getServerPoolIdleThreads() <= 0) {
					/* No threads available to process. Return 503 */
					resp.sendError(DME2Constants.DME2_ERROR_CODE_503, "NO IDLE THREADS AVAILABLE");
					resp.flushBuffer();
					return;
				}

				resp.setStatus(DME2Constants.DME2_RESPONSE_STATUS_200);
				return;
			}

			if (server.getServerPoolIdleThreads() <= 0) {
				/* No threads available to process. Return 503 */
				resp.sendError(DME2Constants.DME2_ERROR_CODE_503, "NO IDLE THREADS AVAILABLE");
				resp.flushBuffer();
				return;
			}

			String msgId = req.getHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSMessageID");
			if (msgId == null) {
				msgId = req.getHeader("JMSMessageID");
			}

			if (msgId == null) {
				msgId = "ID:" + UUID.randomUUID();
			}

			// Set continuation timeout
			this.setRequestTimeout(req, msgId);

			String conversationId = req.getHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSConversationID");
			if (conversationId == null) {
				conversationId = req.getHeader("JMSConversationID");
			}

			String trackingId = msgId + (conversationId == null ? "" : "(" + conversationId + ")");
			String queueName = req.getRequestURI();

			if (queueName == null) {
				logger.warn(null, "doPost", "AFT-DME2-6200 {}",
						new ErrorContext().add(EVENT, "Server.Reply").add(RESULT, FAULT).add(REASONCODE, "404"));

				resp.sendError(DME2Constants.DME2_ERROR_CODE_404, "JMSDestination not set");
				return;
			}

			if (req != null) {
				contextPath = req.getServletPath();
			}

			if (contextPath != null) {
				DME2Constants.setContext(contextPath + DME2Constants.LOGRECORDSEP + trackingId, null);
			} else {
				DME2Constants.setContext(trackingId, null);
			}
			logger.info(null, "doPost", "AFT-DME2-6201 {}",
					new ErrorContext().add(EVENT, "Server.Recieve").add("RemoteHost", req.getRemoteHost())
							.add("RemotePort", req.getRemotePort() + "").add("JMSMessageId", msgId)
							.add("ConversationID", conversationId).add("Queue", req.getRequestURI()));

			DME2JMSQueue requestQueue = manager.getQueue("http://DME2LOCAL" + queueName);

			if (requestQueue == null) {
				logger.warn(null, "doPost", "AFT-DME2-6202 {}",
						new ErrorContext().add(EVENT, "Server.Reply").add(RESULT, FAULT).add(REASONCODE, "404"));

				resp.sendError(DME2Constants.DME2_ERROR_CODE_404, "QUEUE [" + queueName + "] NOT FOUND ON THIS SERVER");
				return;
			}

			// NetworkCrawler.getInstance().addClientJMX(req.getHeader(NetworkCrawler.JMX_HOSTNAME_MSGPROP));

			// parse the payload into a message object
			DME2JMSMessage message = manager.createMessage(req.getInputStream(), genHeaderMap(req),
					req.getCharacterEncoding());
			message.setStringProperty(JMSEventSampler.ENDPOINT_MSGPROP, endpoint);
			message.setLongProperty(JMSEventSampler.RECEIVETIME_MSGPROP, System.currentTimeMillis());
			message.setBooleanProperty("com.att.aft.dme2.jms.isReceiveToService", true);
			message.setStringProperty("requestQueue", queueName);
			logger.debug(null, "doPost", "JMSMessage ID={};timestamp={}", (message == null ? message : message.getJMSMessageID()),
					 message.getJMSTimestamp());

			// Fetching request partner from message header
			requestPartnerName = req.getHeader("DME2_REQUEST_PARTNER");
			if (requestPartnerName == null) {
				requestPartnerName = req.getHeader("DME2_JMS_REQUEST_PARTNER");
			}

			// get the replyTo queue - note we want to use a continuation queue
			// here
			String replyToQueueName = req.getHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSReplyTo");
			if (replyToQueueName == null) {
				replyToQueueName = req.getHeader("JMSReplyTo");
			}
			if (replyToQueueName != null) {
				String handleAsSyncResponse = req.getHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSX_ReplyToQueueSync");
				if (handleAsSyncResponse == null) {
					handleAsSyncResponse = req.getHeader("JMSX_ReplyToQueueSync");
				}
				if (handleAsSyncResponse == null) {
					handleAsSyncResponse = "true";
				}

				// if sync, we need to get a continuation to handle the reply
				// over the same channel
				if (handleAsSyncResponse.equals("true")) {
					URI uri = new URI(replyToQueueName);
					DME2JMSContinuationQueue replyToQueue = new DME2JMSContinuationQueue(manager, uri);
					message.setJMSReplyTo(replyToQueue);

					Continuation continuation = ContinuationSupport.getContinuation(req);
					continuation.setTimeout(continuationTimeout);

					String contId = message.getJMSCorrelationID();
					if (contId == null) {
						contId = message.getJMSMessageID();
					}
					logger.debug(null, "doPost", "Created continuation queue for uri={}; CorrelationID={}; Continuation Object={}; contQueueObject={}", uri.getPath(),
							contId,  continuation, replyToQueue);

					DME2ContinuationEventListener listener = new DME2ContinuationEventListener(replyToQueue, contId,
							requestQueue.getQueueName());

					continuation.addContinuationListener(listener);
					logger.debug(null, "doPost",
							"Code=Trace.DME2JMSServlet.service;Suspending request with JMSMessageID {}", msgId);

					continuation.suspend(resp);

					if (message.getJMSCorrelationID() != null) {
						logger.debug(null, "doPost", "Adding continuation queue for JMSCorrelationID={}; Continuation Object={}; contQueueObject={}",
								message.getJMSCorrelationID(), continuation, replyToQueue);
						replyToQueue.addContinuation(message.getJMSCorrelationID(), continuation,
								requestQueue.getQueueName(), requestPartnerName);
					} else {
						logger.debug(null, "doPost", "Adding continuation queue for JMSMessageID={}; Continuation Object={}; contQueueObject={}",
								message.getJMSMessageID(), continuation, replyToQueue);

						replyToQueue.addContinuation(message.getJMSMessageID(), continuation,
								requestQueue.getQueueName(), requestPartnerName);
					}
					logger.debug(null, "doPost", "DME2JMSServlet JMSMessage added {}", message.getJMSMessageID());
					try {
						requestQueue.put(message);
						Thread.yield();
						logger.debug(null, "doPost", "DME2JMSServlet JMSMessage put succeeded {}",
								message.getJMSMessageID());
					} catch (DME2JMSServiceUnavailableException e) {
						// catch that exception and save for logging if all
						// retries fail.
						try {
							retryMessage(message, e);
						} catch (Exception e1) {
							logger.warn(null, "doPost", "AFT-DME2-6203 {}",
									new ErrorContext().add(EVENT, "Server.Reply.Failover").add(RESULT, FAULT)
											.add(REASONCODE, "503").add("Endpoint", endpoint)
											.add(EXTENDEDMESSAGE, e.toString()),
									e);

							resp.sendError(DME2Constants.DME2_ERROR_CODE_503, e1.getMessage());
						}
						return;
					}
				}
				// for async, we just set the queue, put and exit
				else {
					DME2JMSQueue replyToQueue = manager.getQueue(req.getParameter("replyToQueue"));
					message.setJMSReplyTo(replyToQueue);
					requestQueue.put(message);
					resp.setStatus(DME2Constants.DME2_RESPONSE_STATUS_200);
				}

			} else {
				try {
					requestQueue.put(message);
					resp.setStatus(DME2Constants.DME2_RESPONSE_STATUS_200);
				} catch (DME2JMSServiceUnavailableException e) {
					try {
						retryMessage(message, e);
					} catch (Exception e1) {
						logger.warn(null, "doPost", "AFT-DME2-6204 {}",
								new ErrorContext().add(EVENT, "Server.Reply.Failover").add(RESULT, FAULT)
										.add(REASONCODE, "503").add("Endpoint", endpoint)
										.add(EXTENDEDMESSAGE, e.toString()));

						resp.sendError(DME2Constants.DME2_ERROR_CODE_503, e1.getMessage());
					}
				}
			}
			logger.debug(null, "doPost", "Code=Trace.DME2JMSServlet.service;Completed request from {}:{} with message ID: {} to put() on queue {}",
					req.getRemoteHost(),  req.getRemotePort(), msgId, req.getRequestURI());
			return;
		} catch (Exception e) {
			logger.warn(null, "doPost", "AFT-DME2-6205 {}",
					new ErrorContext().add(EVENT, "Server.Reply.Failed").add(RESULT, FAULT).add(REASONCODE, "500")
							.add("service", serviceName).add("version", version).add(EXTENDEDMESSAGE, e.toString()));

			resp.sendError(DME2Constants.DME2_ERROR_CODE_500, e.toString());
			return;
		}
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	private Map<String, String> genHeaderMap(HttpServletRequest req) {
		Enumeration<?> e = req.getHeaderNames();
		Map<String, String> map = new HashMap<String, String>();
		boolean enableHeaderFiltering = config.getBoolean("DME2_FILTER_HTTP_HEADERS");
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = req.getHeader(key);
			// if we are supposed to filter request headers
			if (enableHeaderFiltering) {
				List<Object> filter = getHeaderFilter();
				// check to see if its a filtered header
				if (!filter.contains(key)) {
					map.put(key, value);
				}

			} else {
				map.put(key, value);
			}
		}
		Map<String, String> modHeaders = DME2MessageHeaderUtils.removeHeaderPrefix(config, map);
		return modHeaders;
	}

	/**
	 * 
	 * @param message
	 * @throws Exception
	 */
	private void retryMessage(Message message, Throwable th) throws Exception {
		logger.debug( null, "retryMessage", METHOD_ENTER );
		int retryCnt = 0;
		String queueName = message.getStringProperty("requestQueue");
		DME2JMSQueue requestQueue = manager.getQueue("http://DME2LOCAL" + queueName);
		if (requestQueue == null) {
			forceFailover(message, "Request queue not found");
			logger.debug( null, "retryMessage", LogMessage.METHOD_EXIT );
			return;
		}
		if (!requestQueue.isClient()) {
			logger.debug(null, "retryMessage", "ServiceProvider receives message using timeout, retry allowed");

		} else {
			logger.debug( null, "retryMessage", LogMessage.METHOD_EXIT );
			return;
		}

		try {
			// Message can be retried till max retry count
			retryCnt = message.getIntProperty("retry_count");
		} catch (Exception e) {
			// ignore any error
			logger.debug(null, "retryMessage", "Exception {}", new ErrorContext().add(EXTENDEDMESSAGE, e.toString()));
		}
		while (true) {
			// Message had been tried already till max count, so just
			// throw listener expired

			if (retryCnt >= maxRetry) {
				logger.error(null, "retryMessage", JMSLogMessage.MAX_RETRIES, retryCnt, maxRetry,
						message.getJMSMessageID(), message.getJMSReplyTo(), th);
				forceFailover(message, "Listener expired");
				logger.debug( null, "retryMessage", LogMessage.METHOD_EXIT );
				return;
			}
			retryCnt++;
			logger.info(null, "retryMessage", JMSLogMessage.SERVLET_RETRY, retryCnt, maxRetry,
					message.getJMSMessageID(), message.getJMSReplyTo());
			message.setIntProperty("retry_count", retryCnt);
			Thread.yield();
			Thread.sleep(maxSleep);
			try {
				requestQueue.put((DME2JMSMessage) message);
				break;
			} catch (Exception e) {
				// ignore error in put and keep retrying till max.
				logger.debug(null, "retryMessage", "Exception", new ErrorContext().add(EXTENDEDMESSAGE, e.toString()));
			}
		}
		logger.debug( null, "retryMessage", LogMessage.METHOD_EXIT );
	}

	/**
	 * 
	 * @param message
	 * @param failoverMsg
	 * @throws Exception
	 */
	private void forceFailover(Message message, String failoverMsg) throws Exception {
		logger.warn(null, "forceFailover", "AFT-DME2-6206 {}", new ErrorContext().add(EVENT, "Server.Reply.Failover")
				.add(RESULT, FAULT).add(REASONCODE, "503").add("failoverMessage", failoverMsg));
		message.setBooleanProperty("JMSXDME2ForceFailoverFlag", true);
		message.setIntProperty("JMSXDME2ForceFailoverCode", JMSXDME2FORCEFAILOVERCODE);
		message.setStringProperty("JMSXDME2ForceFailoverMessage", failoverMsg);
		message.setJMSCorrelationID(message.getJMSMessageID());
		DME2JMSContinuationQueue replyToQueue = (DME2JMSContinuationQueue) message.getJMSReplyTo();
		replyToQueue.put((DME2JMSMessage) message);
		logger.debug( null, "forceFailover", LogMessage.METHOD_EXIT);
		return;
	}

	/**
	 * Sets JMSExpiration time from message as continuation timeout for request.
	 * 
	 * @param req
	 * @param msgId
	 */
	private void setRequestTimeout(HttpServletRequest req, String msgId) {
		long overrideTimeout = config.getLong(JMSConstants.AFT_DME2_SERVER_REPLY_OVERRIDE_TIMEOUT_MS);

		if (overrideTimeout > 0L) {
			this.continuationTimeout = overrideTimeout;

		} else {
			String serverTimeout = req.getHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "AFT_DME2_EP_READ_TIMEOUT_MS");
			if (serverTimeout == null) {
				serverTimeout = req.getHeader("AFT_DME2_EP_READ_TIMEOUT_MS");
			}
			if (serverTimeout != null) {
				try {
					this.continuationTimeout = Long.parseLong(serverTimeout);
				} catch (Exception e) {
					// ignore exception. default value will be assigned
					logger.debug(null, "setRequestTimeout", "Exception {}",
							new ErrorContext().add(EXTENDEDMESSAGE, e.toString()));
				}
			} else {
				this.continuationTimeout = config.getLong(DME2Constants.AFT_DME2_SERVER_REPLY_DEFAULT_TIMEOUT_MS);
			}
		}
		logger.debug(null, "setRequestTimeout",
				"DME2JMSServlet.setRequestTimeout;Setting continuation timeout for request with JMSMessageID {}; ContinuationTimeoutinMs={}", msgId,
				this.continuationTimeout);

	}

	private List<Object> getHeaderFilter() {
		String defaultHttpHeadersToRemove = config.getProperty("DME2_HTTP_DEFAULT_HEADERS_TO_REMOVE").replaceAll("\\s",
				"");
		String httpHeadersToRemove = config.getProperty("DME2_HTTP_HEADERS_TO_REMOVE").replaceAll("\\s", "");
		return Arrays.asList(ArrayUtils.addAll(defaultHttpHeadersToRemove.split(","), httpHeadersToRemove.split(",")));
	}

}
