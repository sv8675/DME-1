/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSTemporaryQueue extends DME2JMSLocalQueue implements TemporaryQueue {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSTemporaryQueue.class.getName());

	private String identifier = null;
	private URI uri = null;
	private QueueSession session = null;
	private QueueConnection connection = null;
	private long createTime = -1;
	private long lastPutTime = -1;
	private long lastGetTime = -1;
	private static final int CONSTANT_1000 = 1000;
	//private static final int CONSTANT_60 = 60;
	private static final long CONSTANT_60L = 60L;
	private long idleTimeoutMs = CONSTANT_1000 * CONSTANT_60L;
	private final DME2JMSManager manager;

	private static final String NOW = ", now=";
	private static final String IDLETIMOUTMS = ", idleTimeoutMs=";
	private static final String LASTPUTTIME = ", lastPutTime=";
	private static final String LASTGETTIME = ", lastGetTime=";
	private DME2Configuration config;

	public DME2JMSTemporaryQueue(DME2JMSManager manager, URI uri, QueueSession session, QueueConnection qConnection)
			throws JMSException {
		super(manager, uri, true);
		super.setClient(true);
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();
		this.uri = uri;
		this.identifier = uri.toString();
		this.session = session;
		this.connection = qConnection;
		this.createTime = System.currentTimeMillis();
		manager.getDME2Manager().getConfig().getLong(JMSConstants.DME2_TEMPQUEUE_IDLETIMEOUT_MS);
		manager.addTemporaryQueue(uri, this);
		logger.debug(null, "DME2JMSTemporaryQueue", "Created temporary queue: {}. {}", identifier, this.toString());
	}

	protected QueueConnection getQueueConnection() {
		return this.connection;
	}

	public QueueSession getQueueSession() {
		return this.session;
	}

	@Override
	public long getCreateTime() {
		return this.createTime;
	}

	public long getLastPutTime() {
		return this.lastPutTime;
	}

	public long getLastGetTime() {
		return this.lastGetTime;
	}

	public URI getURI() {
		return this.uri;
	}

	public void setIdleTimeoutMs( long idleTimeoutMs ) {
		this.idleTimeoutMs = idleTimeoutMs;
	}

	@Override
	protected Message get(long timeout, String filter) throws JMSException {
		logger.debug(null, "Message", LogMessage.METHOD_ENTER);

		if (isClosed()) {
			logger.debug(null, "get", "TemporaryQueue closed {}", getQueueName());
			throw new DME2JMSException("AFT-DME2-6300",
					new ErrorContext().add("QueueName", this.identifier).add("messageSelector", filter).add( "idleTimeoutMs", String.valueOf( this.idleTimeoutMs )));
		}
		this.lastGetTime = System.currentTimeMillis();

		Message msg = super.get(timeout, filter);

		if (msg instanceof DME2JMSMessage) {
			// log the message
			java.util.Properties debugProps = ((DME2JMSMessage) msg).getProperties();
			StringBuffer debugSB = new StringBuffer();
			Enumeration<?> e = debugProps.propertyNames();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				Object value = debugProps.get(key);
				if (debugSB.length() > 1) {
					debugSB.append(",");
				}
				debugSB.append(key);
				debugSB.append("=");
				debugSB.append(value);
			}
			logger.debug(null, "get", "get:", debugSB);
		}
		logger.debug(null, "get", LogMessage.METHOD_EXIT);
		return msg;
	}

	@Override
	public void put(DME2JMSMessage m) throws JMSException {
		logger.debug(null, "get", LogMessage.METHOD_ENTER);
		// log the message
		java.util.Properties debugProps = m.getProperties();
		StringBuffer debugSB = new StringBuffer();
		Enumeration<?> e = debugProps.propertyNames();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Object value = debugProps.get(key);
			if (debugSB.length() > 1) {
				debugSB.append(",");
			}
			debugSB.append(key);
			debugSB.append("=");
			debugSB.append(value);
		}
		logger.debug(null, "put", "put:", debugSB);
		this.lastPutTime = System.currentTimeMillis();
		if (isClosed()) {
			throw new DME2JMSException("AFT-DME2-6301", new ErrorContext().add("QueueName", this.identifier).add( "idleTimeoutMs", String.valueOf( idleTimeoutMs )));
		}
		super.put(m);
		logger.debug(null, "put", LogMessage.METHOD_EXIT);
	}

	@Override
	public void delete() throws JMSException {
		logger.debug(null, "delete", LogMessage.METHOD_ENTER);
		/*
		 * First check if there are any QueueReceivers bound to this TempQueue
		 */
		if (config.getBoolean(JMSConstants.DME2_JMS_TEMP_QUEUE_REC_CLEANUP)) {
			if (manager.containsQueueReceivers(this)) {
				List<DME2JMSQueueReceiver> receivers = manager.getQueueReceivers(this);

				if (receivers != null && !receivers.isEmpty()) {
					List<DME2JMSQueueReceiver> receiverTempList = new ArrayList<DME2JMSQueueReceiver>(receivers);

					for (DME2JMSQueueReceiver receiver : receiverTempList) {
						if (receiver.hasListeners() || receiver.isReceiverWaiting()) {
							throw new DME2JMSException("AFT-DME2-6302",
									new ErrorContext().add("QueueIdentifier", identifier));
						}
					}
				}
			}
		}

		super.close();
		manager.removeTemporaryQueue(uri);
		logger.debug(null, "delete", LogMessage.METHOD_EXIT);
	}

	protected boolean isClosed() {
		logger.debug(null, "isClosed", LogMessage.METHOD_ENTER);
		logger.debug( null, "isClosed", "TempQueue {}. idleTimeoutMs={}", this, idleTimeoutMs );
		if (!isOpen()) {
			logger.debug(null, "isClosed", "DME2JMSTemporaryQueue.isClosed isOpen=false. TempQueue ", this, ". TempQueue is percevied as closed");
			return true;
		}
		logger.debug(null, "isClosed", "DME2JMSTemporaryQueue.isClosed isOpen=true;");

		if (this.idleTimeoutMs == -1) {
			logger.debug(null, "isClosed", "DME2JMSTemporaryQueue.isClosed idleTimeoutMs=-1");
			return false;
		}
		logger.debug(null, "isClosed", "DME2JMSTemporaryQueue.isClosed idleTimeoutMs>1");

		long nowMillis = System.currentTimeMillis();
		// queue was created and has not reached the idleTimeout period
		// and no put or get has been performed.
		if (((nowMillis - this.createTime) < this.idleTimeoutMs) && (this.lastGetTime == -1)
				&& (this.lastPutTime == -1)) {
			logger.debug(null, "isClosed", "TempQueue ", this, " created but has not reached the idleTimeout period and no put and get performed. createTime=", createTime, IDLETIMOUTMS, idleTimeoutMs, LASTPUTTIME, this.lastPutTime, LASTGETTIME, this.lastGetTime, NOW, nowMillis);
			return false;
		}
		logger.debug(null, "isClosed", "DME2JMSTemporaryQueue isClosed TempQueue ", this,
				" created but either reached the idleTimeout period or put and get performed. createTime=",
				this.createTime, IDLETIMOUTMS, this.idleTimeoutMs, LASTPUTTIME, this.lastPutTime, LASTGETTIME,
				this.lastGetTime, NOW, nowMillis, ";", Thread.currentThread().getName());

		// queue was created and has exceeded the idleTimeout period
		// and no put or get has been performed.
		if (((nowMillis - this.createTime) > this.idleTimeoutMs)
				&& ((this.lastGetTime == -1) || (this.lastPutTime == -1))) {
			logger.debug(null, "isClosed", "TempQueue ", this,
					" created and has exceeded the idleTimeout period and no put or get performed. TempQueue is percevied as closed. createTime=",
					createTime, IDLETIMOUTMS, idleTimeoutMs, LASTPUTTIME, this.lastPutTime, LASTGETTIME,
					this.lastGetTime, NOW, nowMillis);
			return true;
		}
		logger.debug(null, "isClosed",
				" created but either has not exceeded the idleTimeout period or put or get had been performed. createTime=",
				this.createTime, IDLETIMOUTMS, this.idleTimeoutMs, LASTPUTTIME, this.lastPutTime, LASTGETTIME,
				this.lastGetTime, NOW, nowMillis, ";");

		if (((nowMillis - this.lastGetTime) < this.idleTimeoutMs)
				|| ((nowMillis - this.lastPutTime) < this.idleTimeoutMs)) {
			logger.debug(null, "isClosed", "TempQueue ", this,
					" created and last put or get time has not exceeded idleTimeout period. createTime=", createTime,
					IDLETIMOUTMS, idleTimeoutMs, LASTPUTTIME, this.lastPutTime, LASTGETTIME, this.lastGetTime, NOW,
					nowMillis);
			return false;
		} else {
			logger.debug(null, "isClosed", "TempQueue ", this,
					" created and last put or get time has exceeded idleTimeout period. TempQueue is percevied as closed. createTime=", createTime,
					IDLETIMOUTMS, idleTimeoutMs, LASTPUTTIME, this.lastPutTime, LASTGETTIME, this.lastGetTime, NOW,
					nowMillis);
			return true;
		}
	}
}
