/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.quick;

import java.lang.management.ManagementFactory;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class QuickNoOpListener implements javax.jms.MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(QuickNoOpListener.class.getName());
	private QueueSession session = null;
	private String PID_DATA;

	public QuickNoOpListener(Connection connection, Destination serverDest, Session session) {
		this.session = (QueueSession) session;
		PID_DATA = ManagementFactory.getRuntimeMXBean().getName();
	}

	public void onMessage(Message message) {
		try {
			Queue replyTo = (Queue) message.getJMSReplyTo();
			if (replyTo == null) {
				return;
			} else {
				TextMessage replyMessage = session
						.createTextMessage(((TextMessage) message).getText() + "; Receiver: PID@HOST: " + PID_DATA);
				replyMessage.setJMSCorrelationID(message.getJMSMessageID());
				MessageProducer producer = session.createProducer(replyTo);
				producer.send(replyMessage);
				producer.close();
			}
		} catch (JMSException e) {
			logger.error(null, "onMessage", e.toString(), e);
		} catch (Exception e) {
			logger.error(null, "onMessage", e.toString(), e);
		}
	}
}
