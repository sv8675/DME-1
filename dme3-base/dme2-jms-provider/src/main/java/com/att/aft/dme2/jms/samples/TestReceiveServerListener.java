/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import java.lang.management.ManagementFactory;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class TestReceiveServerListener extends Thread implements javax.jms.MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(TestReceiveServerListener.class.getName());
	private int id = 0;
	private QueueSession session = null;
	private static int counter = 0;
	private Queue serverDest = null;
	private QueueReceiver receiver = null;
	private QueueConnection connection;
	private String PID_DATA;
	private Message msg = null;
	private int receiveTimeout;
	private static final int CONSTANT_10 = 10;
	private static final int CONSTANT_250 = 250;

	public TestReceiveServerListener(QueueConnection connection, Queue serverDest, int receiveTimeout) {
		logger.debug( null, "ctor", LogMessage.METHOD_ENTER );
		logger.info( null, "ctor", "connection={} serverDest={} receiveTimeout={}", connection, serverDest, receiveTimeout );
		this.connection = connection;
		this.serverDest = serverDest;
		this.receiveTimeout = receiveTimeout;
		id = counter++;
		PID_DATA = ManagementFactory.getRuntimeMXBean().getName();
		logger.debug( null, "ctor", LogMessage.METHOD_EXIT );
	}

	public void start1() throws JMSException {
		session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
		receiver = session.createReceiver(serverDest);
		receiver.setMessageListener(this);
	}

	public void stop1() throws JMSException {
		receiver.setMessageListener(null);
		receiver.close();
		session.close();
	}

	public void run() {
    logger.debug( null, "run", LogMessage.METHOD_ENTER );
		try {
			session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
			receiver = session.createReceiver(serverDest);
			while (true) {
				this.msg = receiver.receive(receiveTimeout);
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
    logger.debug( null, "run", LogMessage.METHOD_EXIT );
	}

	private Random random = new Random();

	public void onMessage(Message message) {
		try {
			logger.debug(null, "onMessage", "Message arrived : {} for reply {}", message.getJMSMessageID(),
					message.getJMSReplyTo());
			Queue replyTo = (Queue) message.getJMSReplyTo();
			if (replyTo == null) {
				logger.info(null, "onMessage", JMSLogMessage.CLIENT_RECOW, id, message.getJMSMessageID(),
						message.getJMSCorrelationID());
				return;
			} else {
				Thread.sleep(CONSTANT_10);
				Thread.sleep(random.nextInt(CONSTANT_10) + CONSTANT_250);
				TextMessage replyMessage = session
						.createTextMessage(((TextMessage) message).getText() + "; Receiver: PID@HOST: " + PID_DATA);
				replyMessage.setJMSCorrelationID(message.getJMSMessageID());
				MessageProducer producer = session.createProducer(replyTo);
				producer.send(replyMessage);
				logger.debug(null, "onMessage", "Message left : {} for reply {}", message.getJMSMessageID(),
						message.getJMSReplyTo());
				producer.close();
				logger.info(null, "onMessage", JMSLogMessage.CLIENT_RECRR, id, replyMessage.getJMSMessageID(),
						replyMessage.getJMSCorrelationID(), replyTo.getQueueName());
			}
		} catch (JMSException e) {
			try {
				logger.debug(null, "onMessage", "Message exception: {} for reply {}", message.getJMSMessageID(),
						message.getJMSReplyTo(), e);
			} catch (Exception e1) {
				logger.debug(null, "onMessage", "Message exception: {}", message, e);

			}
			logger.error(null, "onMessage", JMSLogMessage.CLIENT_EX, id, e);
		} catch (Exception e) {
			logger.warn(null, "onMessage", JMSLogMessage.CLIENT_EX, id, e);
		}
	}

}
