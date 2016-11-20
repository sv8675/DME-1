/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class TestThrottleMessageListener implements javax.jms.MessageListener {
	private static final Logger logger = LoggerFactory.getLogger( TestThrottleMessageListener.class );
	
	private QueueConnection connection = null;
	private Queue dest = null;
	private QueueReceiver receiver = null;
	private QueueSession session = null;

	public TestThrottleMessageListener(QueueConnection connection, QueueSession session, Queue dest) {
		this.connection = connection;
		this.session = session;
		this.dest = dest;
	}

	public void start() throws JMSException {
		session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
		receiver = session.createReceiver(dest);
		receiver.setMessageListener(this);
	}

	public void stop() throws JMSException {
		try {
			if (receiver != null)
				receiver.close();
		} catch (Exception e) {
		}
		try {
			if (session != null)
				session.close();
		} catch (Exception e) {
		}
	}

	public void setReceiver(QueueReceiver receiver) {
		this.receiver = receiver;
	}

	public void onMessage(Message msg) {
		try {
			logger.debug( null, "onMessage", "TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " received request");
			Queue replyTo = (Queue) msg.getJMSReplyTo();
			logger.debug( null, "onMessage", "TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " will respond to " + replyTo.getQueueName());
			// Sleep on message body number of milli seconds
			long milliSecondsToSleep = 0l;
			try {
				milliSecondsToSleep = Long.valueOf(((TextMessage) msg).getText());
				logger.debug( null, "onMessage", "TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " Sleeping for " + milliSecondsToSleep);
				Thread.sleep(milliSecondsToSleep);
			} catch (Exception exception) {
				logger.debug( null, "onMessage", "TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " Slept for " + milliSecondsToSleep);
			}

			if (replyTo != null) {
				TextMessage replyMsg = session.createTextMessage();
				replyMsg.setText("TestThrottleMessageListener:::" + " Slept for " + milliSecondsToSleep);
				replyMsg.setJMSCorrelationID(msg.getJMSMessageID());
				replyMsg.setJMSExpiration(System.currentTimeMillis() + 60000);

				MessageProducer producer = session.createProducer(replyTo);
				producer.send(replyMsg);
				producer.close();
				logger.debug( null, "onMessage", "TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " responded to " + replyTo.getQueueName());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public QueueReceiver getReceiver() {
		return receiver;
	}
}