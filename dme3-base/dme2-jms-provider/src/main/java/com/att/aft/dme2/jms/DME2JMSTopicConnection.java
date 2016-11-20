/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2JMSTopicConnection implements TopicConnection, ConnectionConsumer {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSTopicConnection.class.getName());

	private static DME2JMSTopicSession session = new DME2JMSTopicSession(null, false, 0);
	private static ConnectionMetaData metadata = new DME2JMSConnectionMetaData();
	private ExceptionListener exceptionListener;
	private String clientId;

	public DME2JMSTopicConnection() {
		logger.warn(null, "DME2JMSTopicConnection",
				"*****  Creating Dummy Topic Connection, WILL NOT BE USABLE!! *****");

	}

	@Override
	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
		return session;
	}

	@Override
	public String getClientID() throws JMSException {
		return clientId;
	}

	@Override
	public void setClientID(String clientID) throws JMSException {
		this.clientId = clientID;
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		return metadata;
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return exceptionListener;
	}

	@Override
	public void setExceptionListener(ExceptionListener listener) throws JMSException {
		this.exceptionListener = listener;
	}

	@Override
	public void start() throws JMSException {
	}

	@Override
	public void stop() throws JMSException {
	}

	@Override
	public void close() throws JMSException {
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
			ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return this;
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(Topic topic, String messageSelector,
			ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return this;
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
			String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return this;
	}

	@Override
	public TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {
		return session;
	}

	@Override
	public ServerSessionPool getServerSessionPool() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

}
