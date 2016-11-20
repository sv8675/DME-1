/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSContinuationQueue extends DME2JMSQueue {
	private static final long serialVersionUID = 1L;
	private final Map<String, Continuation> continuations = Collections
			.synchronizedMap(new HashMap<String, Continuation>());
	// HashMap that stores information on elapsedTime, requestQueueName and
	// request partner info
	private final Map<String, String> requestInfo = Collections.synchronizedMap(new HashMap<String, String>());

	private static final Logger logger = LoggerFactory.getLogger(DME2JMSContinuationQueue.class.getName());

	private final DME2JMSManager manager;
	private DME2Configuration config;

	private static final int CONSTANT_100 = 100;

	private static final String HASHCODE = "; hashCode=";
	private static final String REQUESTCODE = "requestQueue";
	private static final String CODE = "Code";
	private static final String RESULT = "Result";
	private static final String EXTENDEDMESSAGE = "extendedMessage";

	public static final String CONTENT_LENGTH_LABEL = "Content-Length";
	public static final String ENABLE_CONTENT_LENGTH_LABEL = "AFT_DME2_SET_RESLEN";

	public DME2JMSContinuationQueue(DME2JMSManager manager, URI name) throws JMSException {
		super(manager, name);
		this.manager = manager;
		logger.debug(null, "DME2JMSContinuationQueue", "Creating contQueue {};uri={}", this, name.getPath());
	}

	public synchronized void addContinuation(String correlationID, Continuation continuation, String requestQueueName,
			String requestPartnerName) {
		logger.debug(null, "addContinuation", "Adding correlationID {} {} {}; contQueue={}", correlationID, HASHCODE,
				correlationID.hashCode(), this);
		// Storing request time, queueName and partner info
		continuations.put(correlationID, continuation);
		requestInfo.put(correlationID, System.currentTimeMillis() + DME2Constants.LOGRECORDSEP + requestQueueName
				+ DME2Constants.LOGRECORDSEP + ((requestPartnerName != null) ? requestPartnerName : ""));
		manager.addContinuation(correlationID, this);
		logger.debug(null, "addContinuation", "Added correlationID {}; Continuation Object={}", correlationID,
				continuation);
	}

	/*
	 * Handle a put event on this Queue by finding and using the Continuation
	 * associated with the messages CorrelationID
	 * 
	 * @see com.att.aft.hjms.HttpJMSQueue#put(com.att.aft.hjms.HttpJMSMessage)
	 */
	@Override
	public void put(DME2JMSMessage m) throws JMSException {
		logger.debug(null, "put", LogMessage.METHOD_ENTER);
		boolean replySuccessful = false;
		boolean failover = false;
		boolean failed = false;
		long msgSize = 0;
		long elapsedTime = 0;
		String reqQueue = null;
		String requestPartner = null;

		String msgId = m.getJMSCorrelationID();
		String conversationId = m.getStringProperty("JMSConversationID");
		String requestQueue = m.getStringProperty(REQUESTCODE);

		String trackingId = msgId + (conversationId == null ? "" : "(" + conversationId + ")");
		if (requestQueue != null) {
			DME2Constants.setContext(requestQueue + DME2Constants.LOGRECORDSEP + trackingId, null);
		} else {
			DME2Constants.setContext(trackingId, null);
		}

		// log the message
		java.util.Properties debugProps = m.getProperties();
		StringBuffer debugSB = new StringBuffer();
		Enumeration<?> e = debugProps.propertyNames();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Object value = debugProps.get(key);
			if (debugSB.length() > 1) {
				debugSB.append(",");
			}
			debugSB.append(key);
			debugSB.append("=");
			debugSB.append(value);
			logger.debug(null, "put", "put:{}", debugSB);
		}

		m.setJMSDestination(this);

		String value = requestInfo.get(m.getJMSCorrelationID());
		if (value != null) {
			try {
				String[] values = value.split("\\" + DME2Constants.LOGRECORDSEP);
				long time = Long.parseLong(values[0]);
				reqQueue = values[1];
				elapsedTime = System.currentTimeMillis() - time;
        if ( values.length >= 3 )
  				requestPartner = values[2];
			} catch (Exception e1) {
				// Ignore error in parsing requestQueue or time or
				// requestPartner
				logger.debug(null, "put", LogMessage.DEBUG_MESSAGE, "Exception", e1);
			}
		}

		if (m != null) {
			if (m instanceof DME2JMSTextMessage) {
				DME2JMSTextMessage tm = (DME2JMSTextMessage) m;
				if (tm.getText() != null) {
					msgSize = tm.getText().length();
				}
			}
		}
		logger.debug(null, "put", "Continuations get {}; contQueue={} {} {}; hmsize={}; containsKey={}",
				m.getJMSCorrelationID(), this, HASHCODE, m.getJMSCorrelationID().hashCode(), continuations.size(),
				continuations.get(m.getJMSCorrelationID()));
		Continuation c = continuations.get(m.getJMSCorrelationID());
		logger.debug(null, "put", "Continuations get returned for {}; Continuation={}; contQueue={}{}{}",
				m.getJMSCorrelationID(), c, this, HASHCODE, m.getJMSCorrelationID().hashCode());

		if (c == null) {
			Thread.yield();
			try {
				Thread.sleep(CONSTANT_100);
			} catch (Exception e1) {
				// ignore
				logger.debug(null, "put", LogMessage.DEBUG_MESSAGE, "Exception", e1);
			}
			logger.debug(null, "put", "Continuations get after sleep {} {}", m.getJMSCorrelationID(),
					Thread.currentThread().getName());
			c = continuations.get(m.getJMSCorrelationID());
			if (c == null) {
				throw new DME2JMSException("AFT-DME2-5105", new ErrorContext().add(REQUESTCODE, reqQueue));
			}
		}

		// this is required to avoid race condition where Jetty has not received
		// the Contination back from
		// the servlet yet
		while (c.isSuspended() && c.isResumed()) {
			logger.debug(null, "put", "Continuation suspended : {}|{};Continuation={}{}{}", m.getJMSMessageID(),
					m.getJMSReplyTo(), c, HASHCODE, m.getJMSCorrelationID().hashCode());
			try {
				Thread.yield();
			} catch (Exception e1) {
				logger.debug(null, "put", LogMessage.DEBUG_MESSAGE, "Exception", e1);
			}
		}

		HttpServletResponse resp = (HttpServletResponse) c.getServletResponse();

		String charset = m.getStringProperty(DME2Constants.DME2_JMS_REQUEST_CHARSET_CLASS);
		if (charset == null || "null".equalsIgnoreCase(charset)) {
			// if(m){
			charset = manager.getDME2Manager().getCharacterSet();
			if (charset == null || "null".equalsIgnoreCase(charset)) {
				charset = "ISO-8859-1"; // default HTTP charset...
			}
			// }
		}

		String contentType = m.getStringProperty("com.att.aft.dme2.jms.contentType");
		if (contentType != null) {
			resp.setContentType(contentType + "; charset=" + charset);
		} else {
			if (charset != null) {
				resp.setContentType("text/plain; charset=".concat(charset));
			}
		}

		// if the listener implementation indicated a failover should be
		// performed by client...
		if (m.getBooleanProperty("JMSXDME2ForceFailoverFlag")) {
			int failoverCode = m.getIntProperty("JMSXDME2ForceFailoverCode");
			String failoverMessage = m.getStringProperty("JMSXDME2ForceFailoverMessage");
			if (failoverCode < DME2Constants.DME2_ERROR_CODE_404) {
				failoverCode = DME2Constants.DME2_ERROR_CODE_503;
			}
			if (failoverMessage == null || failoverMessage.length() == 0) {
				failoverMessage = "SERVICE IMPLEMENTATION IS UNAVAILABLE";
			}
			try {
				this.continuations.remove(m.getJMSCorrelationID());
				this.requestInfo.remove(m.getJMSCorrelationID());
				logger.warn(null, "put", "AFT-DME2-5102", new ErrorContext().add(CODE, "Server.Reply.Failover")
						.add(RESULT, "503").add("FailoverMessage", failoverMessage));
				failover = true;
				resp.sendError(failoverCode, failoverMessage);
			} catch (IOException e1) {
				failed = true;
				logger.warn(null, "put", "AFT-DME2-5103",
						new ErrorContext().add(CODE, "Server.Reply").add(RESULT, "FailoverFault")
								.add(EXTENDEDMESSAGE, e1.getMessage()).add("Elapsed", elapsedTime + ""),
						e1);

			}
		}
		// handle a normal reply
		else {
			Enumeration<?> e1 = m.getPropertyNames();
			while (e1.hasMoreElements()) {
				String key = (String) e1.nextElement();
				String debugValue = m.getStringProperty(key);
				resp.setHeader(key, debugValue);
			}
			m.genID();

			DME2Configuration config = this.manager.getDME2Manager().getConfig();
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSMessageID", m.getJMSMessageID());
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSCorrelationID",
					m.getJMSCorrelationID());
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSType", m.getJMSType());
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSDeliveryMode",
					"" + m.getJMSDeliveryMode());
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSExpiration",
					"" + m.getJMSExpiration());
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSPriority",
					"" + m.getJMSPriority());
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSTimestamp",
					"" + m.getJMSTimestamp());
			Queue queue = (Queue) m.getJMSDestination();
			resp.setHeader(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + "JMSDestination",
					queue.getQueueName());

			// boolean enableContentLength =
			// Boolean.parseBoolean(Configuration.getInstance()
			// .getProperty(ENABLE_CONTENT_LENGTH_LABEL,
			// DME2Constants.ENABLE_CONTENT_LENGTH));
			boolean enableContentLength = config.getBoolean(DME2Constants.ENABLE_CONTENT_LENGTH);
			byte[] rawBytes = null;
			if (m instanceof DME2JMSTextMessage) {
				DME2JMSTextMessage m1 = (DME2JMSTextMessage) m;
				if (m1.getText() != null) {
					logger.debug(null, "put", "Response Data= {}| ResponseLength={}", m1.getText(),
							m1.getText().length());
					try {
						rawBytes = m1.getText().getBytes(charset);
						logger.debug(null, "put", "raw bytes size = {}", rawBytes.length);
					} catch (UnsupportedEncodingException e11) {
						throw new DME2JMSException("AFT-DME2-5106",
								new ErrorContext().add(REQUESTCODE, reqQueue).add("replyQueue", queue.getQueueName())
										.add("messageID", m.getJMSMessageID())
										.add("correlationID", m.getJMSCorrelationID()));
					}
				}

				if (enableContentLength && rawBytes != null) {
					resp.setContentLength(rawBytes.length);
					resp.setIntHeader(CONTENT_LENGTH_LABEL, rawBytes.length);
				}
			}

			try {
				if (rawBytes != null) {
					resp.getOutputStream().write(rawBytes);
				}

				replySuccessful = true;
				logger.debug(null, "put", "Continuation reply successful : {}|{};replySuccessful={}",
						m.getJMSMessageID(), m.getJMSReplyTo(), replySuccessful);
			} catch (IOException io) {
				failed = true;
				logger.error(null, "put", "AFT-DME2-5104 {}", new ErrorContext().add(CODE, "Server.Reply")
						.add(RESULT, "Failed").add(EXTENDEDMESSAGE, io.getMessage()).add("Elapsed", elapsedTime + ""),
						io);
				JMSException je = new JMSException("Response send failed");
				je.initCause(io);
				throw new DME2JMSException("AFT-DME2-5105",
						new ErrorContext().add(EXTENDEDMESSAGE, io.getMessage()).add(REQUESTCODE, reqQueue), je);
			} finally {
				manager.removeContinuation(m.getJMSCorrelationID());
				if (replySuccessful) {
					logger.info(null, "put", "AFT-DME2-5100 {}", new ErrorContext().add(CODE, "Server.Reply")
							.add(RESULT, "Success").add("Elapsed", elapsedTime + ""));
					if (m != null) {
						// Ignore any error in collecting stats.
						// Should not interfere regular execution path
						createSuccessResponseMetricsEvent(msgSize, reqQueue, elapsedTime, m, requestPartner);
					}
				}
			}
		}
		if (failed) {
			try {
				createErrorResponseMetricsEvent( msgSize, reqQueue, elapsedTime, m, requestPartner );
			} catch ( Exception e2 ) {

			}
		}
		if (failover) {
			try {
				createFailoverRequestMetricsEvent( msgSize, reqQueue, elapsedTime, m, requestPartner );
			} catch ( Exception e2 ) {

			}
		}

		if (c.isSuspended()) {
			logger.debug(null, "put", "Continuation completed for : {}|{}", m.getJMSMessageID(), m.getJMSReplyTo());
			c.complete();
		}
		logger.debug(null, "put", "Continuation state for : {}|{} {} {} {}", m.getJMSMessageID(), m.getJMSReplyTo(),
				c.isExpired(), c.isInitial(), c.isResumed());

		this.continuations.remove(m.getJMSCorrelationID());
		this.requestInfo.remove(m.getJMSCorrelationID());
		logger.debug(null, "put", LogMessage.METHOD_EXIT);
	}

	private void createSuccessResponseMetricsEvent(final long msgSize, final String reqQueue, final long elapsedTime,
			final DME2JMSMessage m, final String requestPartner) {
		try {
			createMetricsEvent(msgSize, reqQueue, elapsedTime, m, requestPartner, DME2Constants.REPLY_EVENT,
					"EventProcessor.Success");
		} catch (Exception e11) {
			logger.debug(null, "put", "AFT-DME2-5101",
					new ErrorContext().add(CODE, "EventProcessor.Fault").add(EXTENDEDMESSAGE, e11.getMessage()));
		}
	}

	private void createErrorResponseMetricsEvent(final long msgSize, final String reqQueue, final long elapsedTime,
			final DME2JMSMessage m, final String requestPartner) throws JMSException {
		createMetricsEvent(msgSize, reqQueue, elapsedTime, m, requestPartner, DME2Constants.FAULT_EVENT,
				"EventProcessor.Failed");
	}

	private void createFailoverRequestMetricsEvent(final long msgSize, final String reqQueue, final long elapsedTime,
			final DME2JMSMessage m, final String requestPartner) throws JMSException {
		createMetricsEvent(msgSize, reqQueue, elapsedTime, m, requestPartner, DME2Constants.FAILOVER_EVENT,
				"EventProcessor.Failover");
	}

	private void createMetricsEvent(final long msgSize, final String reqQueue, final long elapsedTime,
			final DME2JMSMessage m, final String requestPartner, final String eventType, final String errorCode)
					throws JMSException {

		HashMap<String, Object> props = new HashMap<String, Object>();
		props.put(DME2Constants.MSG_SIZE, msgSize);
		props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
		props.put(eventType, true);
		props.put(DME2Constants.QUEUE_NAME, reqQueue);
		props.put(DME2Constants.ELAPSED_TIME, elapsedTime);
		if (m.getJMSCorrelationID() != null) {
			props.put(DME2Constants.MESSAGE_ID, m.getJMSCorrelationID());
		} else {
			props.put(DME2Constants.MESSAGE_ID, m.getJMSMessageID());
		}
		if (requestPartner != null && requestPartner.length() > 0) {
			props.put(DME2Constants.DME2_REQUEST_PARTNER, requestPartner);
		}
		props.put(DME2Constants.DME2_INTERFACE_PORT, this.manager.getDME2Manager().getPort() + "");
		props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,
				config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL));
		try {
			manager.getDME2Manager().postStatEvent(props);
		} catch (Exception e1) {
			logger.debug(null, "put", "AFT-DME2-5101",
					new ErrorContext().add(CODE, errorCode).add(EXTENDEDMESSAGE, e1.getMessage()));
		}
	}

	@Override
	public String toString() {
		try {
			return "ContinuationQueue: " + this.getQueueName();
		} catch (JMSException e) {
			return "ContinuationQueue";
		}
	}
}
