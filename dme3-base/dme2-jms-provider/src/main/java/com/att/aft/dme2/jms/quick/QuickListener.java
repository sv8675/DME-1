/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.quick;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;

import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * Wrapper/holder for provided MessageListener
 */
@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class QuickListener implements Runnable, MessageListener {

	private static final Logger logger = LoggerFactory.getLogger(QuickListener.class.getName());

	private MessageListener listener;

	public QuickListener(Connection connection, Destination destination, Session session, MessageListener listener) {
		this.listener = listener;
	}

	@Override
	public void onMessage(Message message) {
		long start = System.currentTimeMillis();
		try {
			Queue replyTo = (Queue) message.getJMSReplyTo();
			listener.onMessage(message);
			long elapsed = System.currentTimeMillis() - start;
			if (replyTo == null) {
				logger.info(null, "onMessage", JMSLogMessage.QUICK_RECOW, message.getJMSMessageID(),
						message.getJMSCorrelationID(), elapsed);
			} else {
				logger.info(null, "onMessage", JMSLogMessage.QUICK_RECOW, message.getJMSMessageID(),
						message.getJMSCorrelationID(), replyTo.getQueueName(), elapsed);
			}
		} catch (Throwable e) {
			long elapsed = System.currentTimeMillis() - start;
			try {
				logger.info(null, "onMessage", JMSLogMessage.QUICK_RECOW, message.getJMSMessageID(),
						message.getJMSCorrelationID(), elapsed, e);

			} catch (JMSException e1) {
				// failed to get message attributes for debug message
				logger.warn(null, "onMessage", JMSLogMessage.QUICK_RECRR, "??; message=" + message, "", elapsed, e);
			}
		}

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
