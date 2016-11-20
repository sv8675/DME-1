/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.jms.XAQueueSession;
import javax.transaction.xa.XAResource;

import com.att.aft.dme2.jms.util.DME2JMSExceptionHandler;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSQueueSession extends DME2JMSSession implements QueueSession, XAQueueSession {

	private static final Logger logger = LoggerFactory.getLogger(DME2JMSQueueSession.class.getName());
	private boolean transacted = false;
	private final int ackMode;
	private DME2JMSQueueConnection connection = null;
	private boolean closed = false;
	private final DME2JMSManager manager;
	private Message distinquishedMessage = null;
	private MessageListener listener = null;
	private DME2JMSXAResource xaResource = null;

	private static final String QUEUENAME = "queueName";
	private static final String MSGSELECTOR = "messageSelector";
	private static final String DESTTYPE = "DestinationType";

	protected DME2JMSQueueSession(DME2JMSQueueConnection connection, boolean transacted, int ackMode) {
		logger.debug(null, "DME2JMSQueueSession", JMSLogMessage.SESSION_CREATED, connection);
		this.connection = connection;
		this.transacted = transacted;
		this.ackMode = ackMode;
		this.manager = connection.getManager();
	}

	@Override
	public QueueReceiver createReceiver(Queue queue) throws JMSException {
		assertNotClosed();
		if (queue instanceof DME2JMSTemporaryQueue) {
			DME2JMSTemporaryQueue tempQueue = (DME2JMSTemporaryQueue) queue;
			if (tempQueue.getQueueConnection() != this.connection) {
				throw new DME2JMSException("AFT-DME2-6000", new ErrorContext().add(QUEUENAME, queue.getQueueName()));
			}
		}

		return new DME2JMSQueueReceiver(manager, queue);
	}

	@Override
	public QueueReceiver createReceiver(Queue queue, String messageSelector) throws JMSException {
		assertNotClosed();
		if (queue instanceof DME2JMSTemporaryQueue) {
			DME2JMSTemporaryQueue tempQueue = (DME2JMSTemporaryQueue) queue;
			if (tempQueue.getQueueConnection() != this.connection) {
				throw new DME2JMSException("AFT-DME2-6001",
						new ErrorContext().add(QUEUENAME, queue.getQueueName()).add(MSGSELECTOR, messageSelector));
			}
		}

		DME2JMSQueueReceiver receiver = new DME2JMSQueueReceiver(manager, queue, messageSelector);
		return receiver;
	}

	@Override
	public QueueSender createSender(Queue queue) throws JMSException {
		assertNotClosed();
		DME2JMSQueueSender sender = new DME2JMSQueueSender((DME2JMSQueue) queue, this);
		return sender;
	}

	@Override
	public void setMessageListener(MessageListener listener) throws JMSException {
		assertNotClosed();
		logger.debug(null, "setMessageListener", JMSLogMessage.SESSION_LISTENER, listener.getClass().getName());
		this.listener = listener;
	}

	@Override
	public Queue createQueue(String arg0) throws JMSException {
		assertNotClosed();
		DME2JMSQueue queue;
		try {
			queue = manager.getQueue(arg0);
		} catch (Exception e) {
			logger.error(null, "createQueue", "AFT-DME2-6002 {}",
					new ErrorContext().add(QUEUENAME, arg0).add("extendedMessage", e.getMessage()), e);
			throw new DME2JMSException("AFT-DME2-6002",
					new ErrorContext().add(QUEUENAME, arg0).add("extendedMessage", e.getMessage()), e);
		}
		return queue;
	}

	@Override
	public MessageConsumer createConsumer(Destination dest) throws JMSException {
		assertNotClosed();
		DME2JMSQueueReceiver rec = null;

		try {
			if (dest instanceof DME2JMSTemporaryQueue) {
				DME2JMSTemporaryQueue tempQueue = (DME2JMSTemporaryQueue) dest;

				if (tempQueue.getQueueConnection() != this.connection) {
					throw new DME2JMSException("AFT-DME2-6003",
							new ErrorContext().add(QUEUENAME, tempQueue.getQueueName()));
				}
			}

			if (dest instanceof DME2JMSQueue) {
				rec = new DME2JMSQueueReceiver(manager, (DME2JMSQueue) dest);
			} else {
				throw new DME2JMSException("AFT-DME2-6004",
						new ErrorContext().add(DESTTYPE, dest.getClass().getName()));

			}
		} catch (Exception e) {
			DME2JMSExceptionHandler.handleException(e, "Not Specified");
		}

		return rec;
	}

	@Override
	public MessageProducer createProducer(Destination dest) throws JMSException {
		assertNotClosed();
		if (dest == null) {
			return new DME2JMSQueueSender(null, this);
		}
		if (dest instanceof DME2JMSQueue) {
			return new DME2JMSQueueSender((DME2JMSQueue) dest, this);
		} else {
			throw new DME2JMSException("AFT-DME2-6005", new ErrorContext().add(DESTTYPE, dest.getClass().getName()));

		}
	}

	@Override
	public TextMessage createTextMessage() throws JMSException {
		assertNotClosed();
		return new DME2JMSTextMessage();
	}

	@Override
	public TextMessage createTextMessage(String text) throws JMSException {
		assertNotClosed();
		DME2JMSTextMessage m = new DME2JMSTextMessage();
		m.setText(text);
		return m;
	}

	@Override
	public void close() throws JMSException {
		closed = true;
		manager.closeTemporaryQueues(this);
	}

	@Override
	public void commit() throws JMSException {
		/**
		 * assertNotClosed(); throw new
		 * DME2JMSNotImplementedException("Session.commit");
		 */

	}

	@Override
	public QueueBrowser createBrowser(Queue arg0) throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.createBrowser");

	}

	@Override
	public QueueBrowser createBrowser(Queue arg0, String arg1) throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.createBrowser");

	}

	@Override
	public BytesMessage createBytesMessage() throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.createBytesMessage");

	}

	@Override
	public MessageConsumer createConsumer(Destination dest, String selector) throws JMSException {
		assertNotClosed();
		if (dest instanceof DME2JMSTemporaryQueue) {
			DME2JMSTemporaryQueue tempQueue = (DME2JMSTemporaryQueue) dest;
			if (tempQueue.getQueueConnection() != this.connection) {
				throw new DME2JMSException("AFT-DME2-6006", new ErrorContext());
			}
		}

		if (dest instanceof DME2JMSQueue) {
			return new DME2JMSQueueReceiver(manager, (DME2JMSQueue) dest, selector);
		} else {
			throw new DME2JMSException("AFT-DME2-6007",
					new ErrorContext().add(DESTTYPE, dest.getClass().getName()).add(MSGSELECTOR, selector));
		}
	}

	@Override
	public MessageConsumer createConsumer(Destination dest, String selector, boolean arg2) throws JMSException {
		assertNotClosed();
		if (dest instanceof DME2JMSTemporaryQueue) {
			DME2JMSTemporaryQueue tempQueue = (DME2JMSTemporaryQueue) dest;
			if (tempQueue.getQueueConnection() != this.connection) {
				throw new DME2JMSException("AFT-DME2-6008",
						new ErrorContext().add(QUEUENAME, tempQueue.getQueueName()).add(MSGSELECTOR, selector));
			}
		}

		if (dest instanceof DME2JMSQueue) {
			return new DME2JMSQueueReceiver(manager, (DME2JMSQueue) dest, selector);
		} else {
			throw new DME2JMSException("AFT-DME2-6009",
					new ErrorContext().add(DESTTYPE, dest.getClass().getName()).add(MSGSELECTOR, selector));
		}
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1) throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.createDurableSubscriber");
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1, String arg2, boolean arg3)
			throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.createDurableSubscriber");
	}

	@Override
	public MapMessage createMapMessage() throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Message Type Not Supported (map)");
	}

	@Override
	public Message createMessage() throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Message Type Not Supported (generic)");
	}

	@Override
	public ObjectMessage createObjectMessage() throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Message Type Not Supported (object)");
	}

	@Override
	public ObjectMessage createObjectMessage(Serializable arg0) throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Message Type Not Supported (object)");
	}

	@Override
	public StreamMessage createStreamMessage() throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Message Type Not Supported (stream)");

	}

	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		assertNotClosed();
		return (TemporaryQueue) connection.createTemporaryDestination(this, false);
	}

	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.createTemporaryTopic");
	}

	@Override
	public Topic createTopic(String arg0) throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.createTopic");
	}

	@Override
	public int getAcknowledgeMode() throws JMSException {
		assertNotClosed();
		return 0;
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		assertNotClosed();
		return listener;
	}

	@Override
	public boolean getTransacted() throws JMSException {
		assertNotClosed();
		return this.transacted;
	}

	@Override
	public void recover() throws JMSException {
		/**
		 * assertNotClosed(); throw new IllegalStateException (
		 * "Rollback not supported - non-transacted sesson");
		 */
	}

	@Override
	public void rollback() throws JMSException {
		return;
		/**
		 * assertNotClosed(); throw new IllegalStateException (
		 * "Rollback not supported - non-transacted sesson");
		 */
	}

	@Override
	public void run() {
		if (this.distinquishedMessage == null) {
			logger.debug(null, "run", JMSLogMessage.SESSION_NO_NEXTMSG);
			return;
		}
		if (this.listener == null) {
			logger.debug(null, "run", JMSLogMessage.SESSION_NO_LISTENER);
		}

		listener.onMessage(this.distinquishedMessage);
		this.distinquishedMessage = null;
	}

	@Override
	public void unsubscribe(String arg0) throws JMSException {
		assertNotClosed();
		throw new DME2JMSNotImplementedException("Session.unsubscribe");
	}

	public boolean isTransacted() {
		return transacted;
	}

	public int getAckMode() {
		return ackMode;
	}

	private void assertNotClosed() throws JMSException {
		if (!isOpen()) {
			throw new DME2JMSException("AFT-DME2-6010", new ErrorContext());
		}

	}

	@Override
	public boolean isOpen() {
		if (closed) {
			return false;
		}

		return connection.isOpen();
	}

	/**
	 * Sets a distinquished "next message" for this session. When set, the next
	 * listener or consumer will retrieve this message first.
	 * 
	 * @param m
	 */
	void setDistinquishedMessage(Message m) {
		this.distinquishedMessage = m;
	}

	Message getDistinquishedMessage() {
		return this.distinquishedMessage;
	}

	@Override
	public Session getSession() throws JMSException {
		return this;
	}

	@Override
	public XAResource getXAResource() {
		if (xaResource == null) {
			xaResource = new DME2JMSXAResource();
		}
		return xaResource;
	}

	@Override
	public QueueSession getQueueSession() throws JMSException {
		return this;
	}
}
