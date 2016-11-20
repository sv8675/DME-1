/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ServerSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSConnectionConsumer implements ConnectionConsumer, MessageListener {

	private DME2JMSQueue destination;
	private ServerSessionPool sessionPool;
	private boolean open = true;
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSConnectionConsumer.class.getName());

	public DME2JMSConnectionConsumer(DME2JMSQueueConnection connection, DME2JMSQueue destination,
			String messageSelector, ServerSessionPool sessionPool, DME2JMSMessageConsumer bridge) throws JMSException {
		this.destination = destination;
		this.sessionPool = sessionPool;
		this.destination.addListener(bridge, this, messageSelector);
	}

	@Override
	public void close() throws JMSException {
		open = false;
	}

	@Override
	public ServerSessionPool getServerSessionPool() throws JMSException {
		if (!open) {
			throw new DME2JMSException("AFT-DME2-5001", new ErrorContext());
		}
		return sessionPool;
	}

	/**
	 * Called whenever a message arrives for this listener
	 * 
	 * @param m
	 */
	public void onMessage(Message m) {
		ServerSession serverSession;
		try {
			serverSession = sessionPool.getServerSession();
			if (serverSession == null) {
				logger.debug(null, "onMessage", "AFT-DME2-5002", new ErrorContext());
				return;
			}
			Session s = serverSession.getSession();
			if (s instanceof DME2JMSQueueSession) {
				DME2JMSQueueSession s2 = (DME2JMSQueueSession) s;
				MessageListener l = s2.getMessageListener();
				if (l == null) {
					logger.debug(null, "onMessage", "AFT-DME2-5003", new ErrorContext());
					return;
				}
				((DME2JMSQueueSession) s).setDistinquishedMessage(m);
				serverSession.start();
			}
		} catch (JMSException e) {
			logger.error(null, "onMessage", "AFT-DME2-5004", new ErrorContext(), e);
		}
	}
}
