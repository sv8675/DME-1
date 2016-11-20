/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.net.URI;

import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

import com.att.aft.dme2.jms.util.UniqueIdGenerator;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSQueueConnection extends DME2JMSConnection implements QueueConnection {

	private String username = null;
	private String password = null;
	private boolean open = false;
	private boolean started = false;
	private ExceptionListener exceptionListener;
	private String clientID;
	private DME2JMSManager manager;

	public DME2JMSQueueConnection(DME2JMSManager manager, String name, String username, String password) {
		this.username = username;
		this.password = password;
		this.open = true;
		this.manager = manager;
		if (this.manager != null) {
			this.manager.setClientCredentials(username, password);
		}
	}

	/**
	 * Option method per spec
	 */
	@Override
	public ConnectionConsumer createConnectionConsumer(Queue destination, String messageSelector,
			ServerSessionPool sessionPool, int count) throws JMSException {
		DME2JMSMessageConsumer messageConsumer = (DME2JMSMessageConsumer) this.createQueueSession(false, 0)
				.createConsumer(destination, messageSelector);
		return new DME2JMSConnectionConsumer(this, (DME2JMSQueue) destination, messageSelector, sessionPool,
				messageConsumer);
	}

	/**
	 * Option method per spec
	 */
	@Override
	public QueueSession createQueueSession(boolean transacted, int ackMode) throws JMSException {
		return new DME2JMSQueueSession(this, transacted, ackMode);
	}

	@Override
	public void close() throws JMSException {
		open = false;
		started = false;
		manager.closeTemporaryQueues(this);
	}

	/**
	 * Option method per spec
	 */
	@Override
	public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
			ServerSessionPool sessionPool, int maxMessage) throws JMSException {
		if (!(destination instanceof DME2JMSQueue)) {
			throw new DME2JMSException("AFT-DME2-5700",
					new ErrorContext().add("DestinationType", destination.getClass().getName()));
		}
		return this.createConnectionConsumer((DME2JMSQueue) destination, messageSelector, sessionPool, maxMessage);
	}

	/**
	 * Option method per spec
	 */
	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic arg0, String arg1, String arg2,
			ServerSessionPool arg3, int arg4) throws JMSException {
		throw new DME2JMSNotImplementedException("Connection.createDurableConnectionConsumer");

	}

	@Override
	public Session createSession(boolean transacted, int acknowledged) throws JMSException {
		return new DME2JMSQueueSession(this, transacted, acknowledged);
	}

	@Override
	public String getClientID() throws JMSException {
		return clientID;
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return exceptionListener;
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		return DME2JMSConnectionMetaData.getMETA_DATA();
	}

	@Override
	public void setClientID(String clientID) throws JMSException {
		this.clientID = clientID;
	}

	@Override
	public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
		this.exceptionListener = exceptionListener;
	}

	@Override
	public void start() throws JMSException {
		started = true;
	}

	@Override
	public void stop() throws JMSException {
		started = false;
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isOpen() {
		return open;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	protected Destination createTemporaryDestination(Session session, boolean topic) throws JMSException {
		if (topic) {
			throw new DME2JMSNotImplementedException("Topic ");
		}
		final URI uri = UniqueIdGenerator.getUniqueTemporaryQueueURI();
		DME2JMSTemporaryQueue tempQueue = new DME2JMSTemporaryQueue(manager, uri, (QueueSession) session, this);
		return tempQueue;

	}

	public DME2JMSManager getManager() {
		return manager;
	}

	void setManager(DME2JMSManager manager) {
		this.manager = manager;
	}
}
