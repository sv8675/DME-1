/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueSender;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.DME2JMSExceptionHandler;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSQueueSender implements QueueSender {

	private static final Logger logger = LoggerFactory.getLogger(DME2JMSQueueSender.class.getName());

	private final DME2JMSQueue queue;
	private final DME2JMSSession session;
	private boolean open = true;
	private int deliveryMode = DeliveryMode.NON_PERSISTENT;
	private boolean disableMessageID;
	private boolean disableMessageTimestamp;
	private static final int CONSTANT_FOUR = 4;
	private static final String TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY = "dme2.tempqueue.idletimeoutms";
	private int priority = CONSTANT_FOUR;
	private long timeToLive = 0;
	private DME2Configuration config;

	protected DME2JMSQueueSender(DME2JMSQueue queue, DME2JMSQueueSession session) {
		this.queue = queue;
		this.session = session;
		if (null != queue) {
			this.config = queue.getConfig();
		}
		try {
			disableMessageTimestamp = config.getBoolean(JMSConstants.DME2_DISABLE_TIMESTAMP);
		} catch (Exception e) {
			disableMessageTimestamp = false;
		}
	}

	@Override
	public Queue getQueue() throws JMSException {
		return queue;
	}

	@Override
	public void send(Message message) throws JMSException {
		send(queue, message);
	}

	@Override
	public void send(Queue queue, Message message) throws JMSException {
		send(queue, message, deliveryMode, priority, timeToLive);

	}

	@Override
	public void send(Message message, int deliveryMode, int priority, long ttl) throws JMSException {
		send(queue, message, deliveryMode, priority, ttl);
	}

	@Override
	public void send(Queue queue, Message message, int deliveryMode, int priority, long ttl) throws JMSException {
		logger.debug(null, "send", LogMessage.METHOD_ENTER);
		long expiration = 0L;
		long timeStamp = System.currentTimeMillis();

		try {
			if (!isOpen()) {
				throw new DME2JMSException("AFT-DME2-5900", new ErrorContext());
			}
			if (!(queue instanceof DME2JMSQueue)) {
				throw new DME2JMSException("AFT-DME2-5901", new ErrorContext());
			}
			if (!(message instanceof DME2JMSMessage)) {
				throw new DME2JMSException("AFT-DME2-5902", new ErrorContext());
			}
			DME2JMSQueue q2 = (DME2JMSQueue) queue;
			if (q2 instanceof DME2JMSTemporaryQueue && message.getLongProperty(TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY) != 0) {
				logger.debug(null, "send", "DME2JMSQueueSender send DME2JMSQueueSender send Received custom temporary queue idle timeout specified by client. "
											+ TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY + "=" + message.getLongProperty(TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY) + "ms."
											+ "JMSTimestamp={}; CorrelationID={}", message.getJMSTimestamp(),
											message.getJMSCorrelationID());
				((DME2JMSTemporaryQueue)q2).setIdleTimeoutMs(message.getLongProperty(TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY));
			}

			if (q2 instanceof DME2JMSTemporaryQueue && message.getLongProperty(TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY) != 0) {
			  logger.debug( null, "send", "DME2JMSQueueSender send Received custom temporary queue idle timeout specified by client. {}={}ms.", TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY, message.getLongProperty(TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY));
				((DME2JMSTemporaryQueue)q2).setIdleTimeoutMs(message.getLongProperty(TEMP_QUEUE_IDLE_TIMEOUT_PROPERTY));
			}

			if (!disableMessageTimestamp) {
				message.setJMSTimestamp(timeStamp);
				logger.debug(null, "send", "DME2JMSQueueSender send JMSTimestamp={}; CorrelationID={}", message.getJMSTimestamp(),
						message.getJMSCorrelationID());

				if (ttl > 0L) {
					expiration = ttl + timeStamp;
				}
			}
			if (expiration > 0L) {
				message.setJMSExpiration(expiration);
			} else {
				if (ttl > 0L) {
					message.setJMSExpiration(System.currentTimeMillis() + ttl);
				}
			}
			logger.debug( null, "send", "message correlationID: {} AFT_DME2_EP_READ_TIMEOUT_MS: {}", message.getJMSCorrelationID(), message.getStringProperty( "AFT_DME2_EP_READ_TIMEOUT_MS" ));
			q2.put((DME2JMSMessage) message);
		} catch (Exception e) {
			throw DME2JMSExceptionHandler.handleException(e, queue.getQueueName());
		}
		logger.debug(null, "send", LogMessage.METHOD_EXIT);
	}

	private boolean isOpen() {
		if (!open) {
			return false;
		}

		return session.isOpen();
	}

	@Override
	public void close() throws JMSException {
		open = false;
	}

	@Override
	public int getDeliveryMode() throws JMSException {
		return deliveryMode;
	}

	@Override
	public Destination getDestination() throws JMSException {
		return queue;
	}

	@Override
	public boolean getDisableMessageID() throws JMSException {
		return disableMessageID;
	}

	@Override
	public boolean getDisableMessageTimestamp() throws JMSException {
		return disableMessageTimestamp;
	}

	@Override
	public int getPriority() throws JMSException {
		return priority;
	}

	@Override
	public long getTimeToLive() throws JMSException {
		return timeToLive;
	}

	@Override
	public void send(Destination dest, Message message) throws JMSException {
		send((Queue) dest, message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void send(Destination dest, Message message, int deliveryMode, int priority, long ttl) throws JMSException {
		send((Queue) dest, message, deliveryMode, priority, ttl);
	}

	@Override
	public void setDeliveryMode(int deliveryMode) throws JMSException {
		if (deliveryMode == DeliveryMode.PERSISTENT) {
			throw new DME2JMSException("AFT-DME2-5903", new ErrorContext());
		}
		this.deliveryMode = deliveryMode;
	}

	@Override
	public void setDisableMessageID(boolean disableMessageID) throws JMSException {
		this.disableMessageID = disableMessageID;
	}

	@Override
	public void setDisableMessageTimestamp(boolean disableMessageTimestamp) throws JMSException {
		this.disableMessageTimestamp = disableMessageTimestamp;
	}

	@Override
	public void setPriority(int priority) throws JMSException {
		this.priority = priority;
	}

	@Override
	public void setTimeToLive(long timeToLive) throws JMSException {
		this.timeToLive = timeToLive;
	}

}
