/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

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

import com.att.aft.dme2.jms.DME2JMSDefaultListener;
import com.att.aft.dme2.jms.DME2JMSErrorMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class FastFailoverMessageListener extends Thread implements javax.jms.MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSDefaultListener.class.getName());
	private int id = 0;
	private QueueSession session = null;
	private static int counter = 0;
	private Queue serverDest = null;
	private QueueReceiver receiver = null;
	private QueueConnection connection;
	private String PID_DATA;
	private Message msg = null;
	private int serverPort = 0;
	boolean stopSignal = false;

	public FastFailoverMessageListener(QueueConnection connection, Queue serverDest, int port) {
		this.connection = connection;
		this.serverDest = serverDest;
		this.serverPort = port;

		id = counter++;
		PID_DATA = ManagementFactory.getRuntimeMXBean().getName();
		try {
			session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
			receiver = session.createReceiver(serverDest);
			receiver.setMessageListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start1() throws JMSException {
		session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
		receiver = session.createReceiver(serverDest);
		receiver.setMessageListener(this);
	}

	public void stop1() throws JMSException {
		stopSignal = true;
		receiver.setMessageListener(null);
		receiver.close();
		session.close();
	}

	public void run() {
		try {
			session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
			receiver = session.createReceiver(serverDest);
			while (!stopSignal) {
				this.msg = receiver.receive(2000);
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

	private Random random = new Random();

	public void onMessage(Message message) {
		try {
			System.err.println(new java.util.Date() + "\t Message arrived : " + message.getJMSMessageID()
					+ " for reply " + message.getJMSReplyTo());
			Queue replyTo = (Queue) message.getJMSReplyTo();
			if (replyTo == null) {
				logger.info(null, "onMessage", "AFTJMSSVR.RECOW " + " - [" + id + "] Processed OneWay MessageID="
						+ message.getJMSMessageID() + ", CorrelationID=" + message.getJMSCorrelationID());
				return;
			} else {
				// identify fault msg return
				TextMessage txtMessage = (TextMessage) message;
				if (txtMessage.getText() != null) {
					String inText = txtMessage.getText();
					if (inText.equals("sendFault")) {
						JMSException jmse = new JMSException("sendingFaultReply");
						DME2JMSErrorMessage em = new DME2JMSErrorMessage(jmse, false);
						em.setJMSCorrelationID(message.getJMSMessageID());
						em.setText(jmse.getMessage());
						MessageProducer producer = session.createProducer(replyTo);
						producer.send(em);
						System.err.println(new java.util.Date() + "\t sendFault Message left : "
								+ message.getJMSMessageID() + " for reply " + message.getJMSReplyTo());
						producer.close();
						logger.info(null, "onMessage",
								"AFTJMSSVR.RECRR " + " - [" + id + "] Processed sendFault RequestReply MessageID="
										+ em.getJMSMessageID() + ", CorrelationID=" + em.getJMSCorrelationID()
										+ ", ReplyTo=" + replyTo.getQueueName());
					}
					if (inText.equals("sendFailoverFault")) {

					}
				}

				// Port 9595 indicates server this is a fast fail server, takes
				// long time to respond
				// and rejecting any request more than one allowed.
				if (this.serverPort == 9595) {
					// sleep for 9 secs
					Thread.sleep(9000);
				}
				int sleepTimeProp = 0;
				TextMessage replyMessage = session
						.createTextMessage("FastFailoverMessage[" + ((TextMessage) message).getText()
								+ "] Receiver: PID@HOST: " + PID_DATA + " [ServerPort=" + serverPort + "]");
				replyMessage.setJMSCorrelationID(message.getJMSMessageID());
				MessageProducer producer = session.createProducer(replyTo);
				producer.send(replyMessage);
				System.err.println(new java.util.Date() + "\t Message left : " + message.getJMSMessageID()
						+ " for reply " + message.getJMSReplyTo());
				producer.close();
				logger.info(null, "onMessage",
						"AFTJMSSVR.RECRR " + " - [" + id + "] Processed RequestReply MessageID="
								+ replyMessage.getJMSMessageID() + ", CorrelationID="
								+ replyMessage.getJMSCorrelationID() + ", ReplyTo=" + replyTo.getQueueName());
			}
		} catch (JMSException e) {
			try {
				System.err.println(new java.util.Date() + "\t Message exception : " + message.getJMSMessageID()
						+ " for reply " + message.getJMSReplyTo());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			logger.error(null, "onMessage", "[" + id + "] " + e.toString(), e);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(null, "onMessage", "[" + id + "] " + e.toString(), e);
		}
	}
}
