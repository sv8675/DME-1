/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import java.lang.management.ManagementFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class TestServerListener implements javax.jms.MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(TestServerListener.class.getName());
	private int id = 0;
	private QueueSession session = null;
	private static int counter = 0;
	private Queue serverDest = null;
	private QueueReceiver receiver = null;
	private final QueueConnection connection;
	private final String PID_DATA;

	public TestServerListener(QueueConnection connection, Queue serverDest) {
		this.connection = connection;
		this.serverDest = serverDest;
		id = counter++;
		PID_DATA = ManagementFactory.getRuntimeMXBean().getName();
	}

	public void start() throws JMSException {
		session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
		receiver = session.createReceiver(serverDest);
		receiver.setMessageListener(this);
	}

	public void stop() throws JMSException {
		receiver.setMessageListener(null);
		receiver.close();
		session.close();
	}

	@Override
	public void onMessage(Message message) {
		try {
			Queue replyTo = (Queue) message.getJMSReplyTo();
			if (replyTo == null) {
				logger.info(null, "onMessage", JMSLogMessage.CLIENT_RECOW, id, message.getJMSMessageID(),
						message.getJMSCorrelationID());
				return;
			} else {
				TextMessage replyMessage = session
						.createTextMessage(((TextMessage) message).getText() + "; Receiver: PID@HOST: " + PID_DATA);
				replyMessage.setJMSCorrelationID(message.getJMSMessageID());
				MessageProducer producer = session.createProducer(replyTo);
				producer.send(replyMessage);
				producer.close();
				logger.info(null, "onMessage", JMSLogMessage.CLIENT_RECRR, id, replyMessage.getJMSMessageID(),
						replyMessage.getJMSCorrelationID(), replyTo.getQueueName());
			}
		} catch (Exception e) {
			logger.warn(null, "onMessage", JMSLogMessage.CLIENT_EX, id, e);
		}
	}

}
