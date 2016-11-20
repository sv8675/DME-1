/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueReceiver;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.DME2JMSExceptionHandler;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSQueueReceiver extends DME2JMSMessageConsumer implements QueueReceiver {

	private final DME2JMSQueue queue;
	private String messageSelector = null;
	private MessageListener listener = null;
	private boolean open = true;
	private DME2Configuration config;
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSQueueReceiver.class.getName());
	private Message msg = null;
	private static Map<String, Boolean> registeredListeners = Collections
			.synchronizedMap(new HashMap<String, Boolean>());

	/* True if this reciever is waiting on a get/put operation */
	private boolean isReceiverWaiting;

	/* True if this reciever has listeners attached */
	private boolean hasListeners;

	private final DME2JMSManager manager;

	protected DME2JMSQueueReceiver(DME2JMSManager manager, Queue queue) {
		this.queue = (DME2JMSQueue) queue;
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();

		if (config.getBoolean(JMSConstants.DME2_JMS_TEMP_QUEUE_REC_CLEANUP)) {
			if (this.manager != null && queue instanceof DME2JMSTemporaryQueue) {
				this.manager.addQueueReceiverToMap(queue, this);
			}
		}

	}

	protected DME2JMSQueueReceiver(DME2JMSManager manager, Queue queue, String messageSelector) {
		this.queue = (DME2JMSQueue) queue;
		this.messageSelector = messageSelector;
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();

		if (config.getBoolean(JMSConstants.DME2_JMS_TEMP_QUEUE_REC_CLEANUP)) {
			if (this.manager != null && queue instanceof DME2JMSTemporaryQueue) {
				this.manager.addQueueReceiverToMap(queue, this);
			}
		}
	}

	@Override
	public Queue getQueue() throws JMSException {
		return queue;
	}

	@Override
	public void close() throws JMSException {
		try {
			if (this.listener != null) {
				this.removeMessageListener(this.listener);
			}
			open = false;
			this.listener = null;
		} finally {
			if (config.getBoolean(JMSConstants.DME2_JMS_TEMP_QUEUE_REC_CLEANUP)) {
				if (manager != null) {
					manager.removeQueueReceiverFromMap(queue, this);
				}
			}
		}
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return listener;
	}

	@Override
	public String getMessageSelector() throws JMSException {
		return messageSelector;
	}

	@Override
	public Message receive() throws JMSException {
		return receive(0);
	}

	@Override
	public Message receive(long ttl) throws JMSException {
		logger.debug(null, "receive", LogMessage.METHOD_ENTER);

		if (!open) {
			throw new DME2JMSException("AFT-DME2-5800", new ErrorContext());
		}

		try {
			isReceiverWaiting = true;
			synchronized (registeredListeners) {
				if (!queue.isClient() && registeredListeners.get(queue.getQueueName()) == null) {
					// Register the queue/bind
					try {
						logger.debug(null, "receive", "Queue {} is being registered", queue.getQueueName() );

						/*
						if(queue.getRealmName() != null) {
							manager.getDME2Manager().bindServiceListener(queue.getQueueNameURI(), new DME2JMSServlet(manager),queue.getRealmName(), queue.getAllowedRoles(), queue.getLoginMethod());
						} else {
							manager.getDME2Manager().bindServiceListener(queue.getQueueNameURI(), new DME2JMSServlet(manager));
						}*/

						DME2JMSServiceHolder serviceHolder = new DME2JMSServiceHolder(queue);
						serviceHolder.setServiceURI(queue.getQueueNameURI());
						serviceHolder.setServlet(new DME2JMSServlet(manager));
						serviceHolder.setSecurityRealm(queue.getRealmName());
						serviceHolder.setAllowedRoles(queue.getAllowedRoles());
						serviceHolder.setLoginMethod(queue.getLoginMethod());
						serviceHolder.setManager(this.manager.getDME2Manager());
						manager.getDME2Manager().bindService(serviceHolder);
						registeredListeners.put(queue.getQueueName(), true);
					} catch (DME2Exception e) {
						throw new DME2JMSException("AFT-DME2-5801", new ErrorContext(), e);
					}
				}
			}
      if ((this.queue instanceof DME2JMSLocalQueue) && !queue.isClient())
			{
				queue.getActiveReceiverCount().incrementAndGet();
			}

			if (!(this.queue instanceof DME2JMSLocalQueue)) {
				throw new DME2JMSException("AFT-DME2-5802",
						new ErrorContext().add("QueueName", this.queue.getQueueName()));
			}

			DME2JMSLocalQueue lQueue = (DME2JMSLocalQueue) this.queue;
			logger.debug(null, "receive", JMSLogMessage.QUEUE_INVOKE, lQueue.getQueueName(), messageSelector);
			logger.debug(null, "receive", "Code=Trace.DME2JMSQueueReceiver.receive; QueueReceiver invoking queue {} get with message selector {}",
					lQueue.getQueueName(), this.messageSelector);
			long start = System.currentTimeMillis();
			this.msg = lQueue.get(ttl, this.messageSelector);
			logger.debug(null, "receive", "Returning messageID: {} in {}", (msg == null ? msg : this.msg.getJMSMessageID()),
					 (System.currentTimeMillis() - start));

			if (lQueue.isClient()) {
				// Log if message get had timed out
				if (this.msg == null) {
					logger.info(null, "receive", "AFT-DME2-5804 {}",
							new ErrorContext().add("Event", "Client.Response.Timedout").add("Timeout", ttl + ""));
					logger.info(null, "receive", "Code=Client.Response.Timedout;Timeout={};MessageSelector={}", ttl, this.messageSelector);
				}

				if (this.msg instanceof DME2JMSErrorMessage) {
					throw new DME2JMSException("AFT-DME2-5803",
							new ErrorContext().add("EndpointsAttempted",
									msg.getStringProperty("AFT_DME2_REQ_TRACE_INFO")),
							((DME2JMSErrorMessage) this.msg).getJMSException());
				}
			}
		} catch (Exception e) {
			throw DME2JMSExceptionHandler.handleException(e, queue.getQueueName());
		} finally {
			isReceiverWaiting = false;
      if ((this.queue instanceof DME2JMSLocalQueue) && !queue.isClient()) {
				queue.getActiveReceiverCount().decrementAndGet();
			}
		}

		return this.msg;
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		return receive(-1);
	}

	@Override
	public void setMessageListener(MessageListener listener) throws JMSException {
		logger.debug(null, "setMessageListener", "DME2JMSQueueReceiver setMessageListener {} for queue {}", listener,
				queue.getQueueName());
		queue.addListener(this, listener, this.messageSelector);
		hasListeners = true;
		this.listener = listener;
	}

	public void removeMessageListener(DME2JMSMessageConsumer consumer, MessageListener listener) throws JMSException {
		queue.removeListener(this, listener);
		logger.debug(null, "removeMessageListener", "DME2JMSQueueReceiver removeMessageListener {} for queue {}", listener,
				queue.getQueueName());
		hasListeners = false;
		this.listener = listener;
	}

	public void removeMessageListener(MessageListener listener) throws JMSException {
		queue.removeListener(this);
		hasListeners = false;
		this.listener = listener;
	}

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

	public static Map<String, Boolean> getRegisteredListeners() {
		return registeredListeners;
	}

	public boolean hasListeners() {
		return hasListeners;
	}

	public boolean isReceiverWaiting() {
		return isReceiverWaiting;
	}
}
