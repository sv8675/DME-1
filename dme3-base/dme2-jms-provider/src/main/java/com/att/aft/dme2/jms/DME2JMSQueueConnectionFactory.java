/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSQueueConnectionFactory extends DME2JMSConnectionFactory
		implements QueueConnectionFactory, Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private transient DME2JMSManager manager;
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSQueueConnectionFactory.class.getName());

	public DME2JMSQueueConnectionFactory() throws Exception {
		manager = DME2JMSManager.getDefaultInstance();
	}

	protected DME2JMSQueueConnectionFactory(DME2JMSManager manager, String uriStr) throws URISyntaxException {
		try {
			URI uri = new URI(uriStr);
			String path = uri.getPath();
			name = path;
			this.manager = manager;
		} catch (Exception e) {
			logger.debug(null, "DME2JMSQueueConnectionFactory", "Exception",
					new ErrorContext().add("extendedMessage", e.toString()));
		}
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		return new DME2JMSQueueConnection(manager, name, null, null);
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		return new DME2JMSQueueConnection(manager, name, username, password);
	}

	@Override
	public String toString() {
		return "DME2JMSQueueConnectionFactory: Name=" + name;
	}

	@Override
	public Connection createConnection(String username, String password) {
		return new DME2JMSQueueConnection(manager, name, username, password);
	}

	@Override
	public Connection createConnection() throws JMSException {
		return new DME2JMSQueueConnection(manager, name, null, null);
	}

	@Override
	protected void buildFromProperties(Properties props) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void populateProperties(Properties props) {
		// TODO Auto-generated method stub

	}

	protected String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	protected DME2JMSManager getManager() {
		return manager;
	}

	protected void setManager(DME2JMSManager manager) {
		this.manager = manager;
	}

}
