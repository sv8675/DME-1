/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.lang.management.ManagementFactory;
import java.util.Enumeration;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSDefaultListener extends Thread implements javax.jms.MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSDefaultListener.class.getName());
	private long listener_id = 0;
	private static long counter = 0;
	private Message msg = null;
	private boolean active = false;
	private final DME2JMSManager manager;
	private DME2Configuration config;
	private int maxRetry = 0;
	private static final int CONSTANT_MAXSLEEP = 5;
	private static final int CONSTANT_MAXRETRY = 3;

	private static final int CONSTANT_JMSXDME2FORCEFAILOVERCODE = 222;
	// Sleep time in ms between retry msg being submitted
	private long maxSleep = CONSTANT_MAXSLEEP;

	/**
	 * 
	 * @param serverDest
	 */
	public DME2JMSDefaultListener(DME2JMSManager manager, Queue serverDest) {
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();
		if (counter == Long.MAX_VALUE) {
			counter = 0;
		}
		listener_id = counter++;
		ManagementFactory.getRuntimeMXBean().getName();
		try {
			maxRetry = Integer.parseInt(config.getProperty(JMSConstants.AFT_DME2_RECEIVE_MAX_RETRY));
			maxSleep = Integer.parseInt(config.getProperty(JMSConstants.AFT_DME2_RETRY_SLEEP));
		} catch (Exception e) {
			// Default to 3
			maxRetry = CONSTANT_MAXRETRY;
		}
	}

	/**
	 * 
	 * @return
	 */
	public long getID() {
		return listener_id;
	}

	/**
	 * 
	 */
	@Override
	public void onMessage(Message message) {
		int retryCnt = 0;
		try {
			synchronized (this) {
				this.msg = message;

				// if no threads are waiting
				if (!active) {
					while (true) {
						try {
							// Message can be retried till max retry count
							retryCnt = message.getIntProperty("retry_count");
						} catch (Exception e) {
							// ignore any error
							logger.debug(null, "onMessage", "Exception",
									new ErrorContext().add("extendedMessage", e.toString()));
						}
						// Message had been tried already till max count, so
						// just
						// throw
						// listener expired
						if (retryCnt >= maxRetry) {
							forceFailover(this.msg, "Listener expired");
							return;
						}
						retryCnt++;
						logger.debug(null, "onMessage", "Retry count: " + retryCnt + ": max " + maxRetry + " for "
								+ message.getJMSMessageID() + " reply queue " + message.getJMSReplyTo());
						message.setIntProperty("retry_count", retryCnt);

						// first validate the local queue exists (append
						// DME2LOCAL
						// so we get a local queue)
						String queueName = message.getStringProperty("requestQueue");
						DME2JMSQueue requestQueue = manager.getQueue("http://DME2LOCAL" + queueName);
						if (requestQueue == null) {
							forceFailover(this.msg, "Request queue not found");
							return;
						}

						try {
							boolean retryStatus = retryMessage(this.msg);
							if (retryStatus) {
								return;
							} else {
								// keep continuing retry
								continue;
							}
						} catch (Exception e) {
							// Request queue put failed, throw a 503 to retry on
							// client
							forceFailover(this.msg, "Listener expired");
							return;
						}
					}
				}
				notify();
				logger.debug(null, "onMessage", "JMSDefaultListener received: messageID: ", message.getJMSMessageID(),
						" active : ", active, " listenerID: ", listener_id);

			}
		} catch (Exception e) {
			logger.debug(null, "onMessage", "[", listener_id, "] ", e.toString(), e);
		}
	}

	/**
	 * 
	 * @param timeout
	 * @param filter
	 * @return
	 * @throws JMSException
	 */
	public Message get(long timeout, String filter) throws JMSException {

		try {
			synchronized (this) {
				active = true;
				long start = System.currentTimeMillis();
				try {
					wait(timeout);
					logger.debug(null, "onMessage", "JMSDefaultListener wait ended, messageID: ",
							(msg == null ? msg : msg.getJMSMessageID()), " active: ", active, ", wait time: ",
							(System.currentTimeMillis() - start), " ms, listenerID: ", listener_id);
				} catch (InterruptedException e1) {
					active = false;
				}
				active = false;
			}
		} catch (Exception e) {
			throw new DME2JMSException("AFT-DME2-5200", new ErrorContext().add("extendedMessage", e.getMessage())
					.add("manager", manager.getDME2Manager().getName()), e);
		}
		return this.msg;
	}

	/**
	 * 
	 * @param message
	 * @param failoverMsg
	 * @throws Exception
	 */
	private void forceFailover(Message message, String failoverMsg) throws Exception {
		logger.warn(null, "forceFailover", "AFT-DME2-5201", new ErrorContext().add("Code", "Server.Reply.Failover")
				.add("Result", "503").add("FailoverMessage", failoverMsg));
		message.setBooleanProperty("JMSXDME2ForceFailoverFlag", true);
		message.setIntProperty("JMSXDME2ForceFailoverCode", CONSTANT_JMSXDME2FORCEFAILOVERCODE);
		message.setStringProperty("JMSXDME2ForceFailoverMessage", failoverMsg);
		message.setJMSCorrelationID(message.getJMSMessageID());
		DME2JMSContinuationQueue replyToQueue = (DME2JMSContinuationQueue) message.getJMSReplyTo();
		replyToQueue.put((DME2JMSMessage) message);
		return;
	}

	/**
	 * 
	 * @param message
	 * @return
	 * @throws Exception
	 */
	private boolean retryMessage(Message message) throws Exception {
		String queueName = message.getStringProperty("requestQueue");
		DME2JMSQueue requestQueue = manager.getQueue("http://DME2LOCAL" + queueName);
		if (requestQueue == null) {
			forceFailover(message, "Request queue not found");
			return true;
		}
		Thread.yield();
		Thread.sleep(maxSleep);
		try {
			requestQueue.put((DME2JMSMessage) message);
		} catch (Exception e) {
			// Ignore exception till all retry attempts are reached
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param message
	 * @return
	 * @throws JMSException
	 */
	public DME2JMSTextMessage copy(DME2JMSMessage message) throws JMSException {

		DME2JMSTextMessage copy = new DME2JMSTextMessage();
		Properties properties = message.getProperties();
		Enumeration<?> propertyNames = message.getPropertyNames();
		while (propertyNames.hasMoreElements()) {
			String name = propertyNames.nextElement().toString();
			String value = properties.getProperty(name);
			copy.setStringProperty(name, value);
		}
		if (message instanceof DME2JMSTextMessage) {
			copy.setText(((DME2JMSTextMessage) message).getText());
		}
		if (message.getJMSCorrelationID() != null) {
			copy.setJMSCorrelationID(message.getJMSCorrelationID());
		}
		if (message.getJMSCorrelationIDAsBytes() != null) {
			copy.setJMSCorrelationIDAsBytes(message.getJMSCorrelationIDAsBytes());
		}

		copy.setJMSDeliveryMode(message.getJMSDeliveryMode());
		if (message.getJMSDestination() != null) {
			copy.setJMSDestination(message.getJMSDestination());
		}

		copy.setJMSExpiration(message.getJMSExpiration());
		if (message.getJMSMessageID() != null) {
			copy.setJMSMessageID(message.getJMSMessageID());
		}

		copy.setJMSPriority(message.getJMSPriority());
		copy.setJMSRedelivered(message.getJMSRedelivered());
		if (message.getJMSReplyTo() != null) {
			copy.setJMSReplyTo(message.getJMSReplyTo());
		}

		copy.setJMSTimestamp(message.getJMSTimestamp());
		if (message.getJMSType() != null) {
			copy.setJMSType(message.getJMSType());
		}
		return copy;
	}
}
