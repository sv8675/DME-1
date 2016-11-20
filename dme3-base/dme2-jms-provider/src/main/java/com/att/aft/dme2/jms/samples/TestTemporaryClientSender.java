/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

@SuppressWarnings("PMD.SystemPrintln")
public class TestTemporaryClientSender implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(TestTemporaryClientSender.class.getName());

	private static int sentCounter = 0;
	private static int successCounter = 0;
	private static int failCounter = 0;
	private static int timeoutCounter = 0;
	private static int mismatchCounter = 0;

	public static final void dumpCounters() {
		System.err.println("Sent=" + sentCounter + ", Success=" + successCounter + ", Timeout=" + timeoutCounter
				+ ", MisMatch=" + mismatchCounter + ", Fail=" + failCounter);
	}

	private String ID = null;
	private static final int CONSTANT_100 = 100;
	private final long pausetime = CONSTANT_100;

	private static final int CONSTANT_20000 = 20000;

	private boolean running = false;

	private QueueSender sender = null;
	private QueueConnection conn = null;

	private QueueSession session = null;

	private final String jndiClass;
	private final String jndiUrl;
	private final String clientConn;
	private final String clientDest;
	private String clientReplyTo;

	public String getID() {
		return ID;
	}

	public TestTemporaryClientSender(String ID, String jndiClass, String jndiUrl, String clientConn,
			String clientDest) {
		this.ID = ID;
		this.jndiClass = jndiClass;
		this.jndiUrl = jndiUrl;
		this.clientConn = clientConn;
		this.clientDest = clientDest;
	}

	public void start() throws JMSException, NamingException {
		if (conn != null) {
			return;
		}
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", jndiClass);
		table.put("java.naming.provider.url", jndiUrl);

		System.out.println("Getting InitialContext");
		InitialContext context = new InitialContext(table);

		System.out.println("Looking up QueueConnectionFactory");
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(clientConn);

		System.out.println("Looking up requeust Queue");
		Queue requestQueue = (Queue) context.lookup(clientDest);

		System.out.println("Creating QueueConnection");
		conn = qcf.createQueueConnection();

		System.out.println("Creating Session");
		session = conn.createQueueSession(true, 0);

		Queue strandedQueue = session.createTemporaryQueue();
		System.out.println("Created stranded queue:" + strandedQueue + " at " + System.currentTimeMillis());

		System.out.println("Creating MessageProducer");
		sender = session.createSender(requestQueue);
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			try {
				TextMessage message = session.createTextMessage();
				message.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
				message.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
				String sentID = ID + "::" + System.currentTimeMillis();
				message.setText(sentID);
				long start = System.currentTimeMillis();
				sentCounter++;
				TextMessage response = send(message, CONSTANT_20000);
				long elapsed = System.currentTimeMillis() - start;
				if (response == null) {
					logger.info(null, "run", JMSLogMessage.CLIENT_TIMEOUT, ID, elapsed, message.getJMSMessageID(),
							clientReplyTo);
					timeoutCounter++;
				} else {
					logger.debug(null, "run", JMSLogMessage.CLIENT_DATA, sentID, response.getText());
					if (response.getText().startsWith(sentID)) {
						logger.info(null, "run", JMSLogMessage.CLIENT_SUCCESS, ID, response.getText(), elapsed,
								response.getJMSMessageID(), response.getJMSCorrelationID(), clientReplyTo);
						successCounter++;
					} else {
						logger.info(null, "run", JMSLogMessage.CLIENT_MISMATCH, ID, elapsed, response.getJMSMessageID(),
								response.getJMSCorrelationID(), clientReplyTo);
						mismatchCounter++;
					}
				}
				Thread.sleep(pausetime);
				running = false;
			} catch (JMSException e) {
				logger.warn(null, "run", JMSLogMessage.CLIENT_JMSEX_RCV, ID, e);
				try {
					Thread.sleep(pausetime);
				} catch (InterruptedException ie) {
				}
			} catch (Exception e) {
				logger.error(null, "run", JMSLogMessage.CLIENT_FATAL, ID, e);
				failCounter++;
				return;
			}
		}
	}

	private TextMessage send(TextMessage message, long timeout) throws JMSException {

		Queue replyToQueue = session.createTemporaryQueue();
		message.setJMSReplyTo(replyToQueue);

		sender.send(message);

		QueueReceiver consumer = session.createReceiver(replyToQueue,
				"JMSCorrelationID = '" + message.getJMSMessageID() + "'");

		return (TextMessage) consumer.receive(timeout);

	}

	public void stop() throws JMSException {
		running = false;
		if (conn != null) {
			sender.close();
			session.close();
			conn.close();
			sender = null;
			session = null;
		}
		conn = null;

	}
}
