/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

public class TestThrottleMessageReceiver extends Thread implements javax.jms.MessageListener {
	private QueueConnection connection = null;
	private Queue dest = null;
	private QueueReceiver receiver = null;
	private QueueSession session = null;
	private int receiveTimeout;

	public TestThrottleMessageReceiver(QueueConnection connection, QueueSession session, Queue dest, int receiveTimeout) {
		this.connection = connection;
		this.session = session;
		this.dest = dest;
		this.receiveTimeout = receiveTimeout;
	}

	public void run() {
		try {
			session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
			receiver = session.createReceiver(this.dest);
			while (true) {
				Message msg = receiver.receive(this.receiveTimeout);
				if (msg != null) {
					try {
						this.onMessage(msg);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void destroy() {
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
			System.out.println("TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " received request");
			Queue replyTo = (Queue) msg.getJMSReplyTo();
			System.out.println("TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " will respond to " + replyTo.getQueueName());
			// Sleep on message body number of milli seconds
			long milliSecondsToSleep = 0l;
			try {
				milliSecondsToSleep = Long.valueOf(((TextMessage) msg).getText());
				System.out.println("TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " Sleeping for " + milliSecondsToSleep);
				Thread.sleep(milliSecondsToSleep);
			} catch (Exception exception) {
				System.out.println("TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " Slept for " + milliSecondsToSleep);
			}

			if (replyTo != null) {
				TextMessage replyMsg = session.createTextMessage();
				replyMsg.setText("TestThrottleMessageListener:::" + " Slept for " + milliSecondsToSleep);
				replyMsg.setJMSCorrelationID(msg.getJMSMessageID());
				replyMsg.setJMSExpiration(System.currentTimeMillis() + 60000);

				MessageProducer producer = session.createProducer(replyTo);
				producer.send(replyMsg);
				producer.close();
				System.out.println("TestThrottleMessageListener thread=" + Thread.currentThread().getId() + " responded to " + replyTo.getQueueName());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
