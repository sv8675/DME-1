/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.Serializable;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2JMSTopicConnectionFactory extends DME2JMSConnectionFactory
		implements TopicConnectionFactory, Serializable {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSTopicConnectionFactory.class.getName());

	private static final long serialVersionUID = 1L;

	private static DME2JMSTopicConnection dummy = new DME2JMSTopicConnection();

	public DME2JMSTopicConnectionFactory() {
		logger.warn(null, "DME2JMSTopicConnectionFactory",
				"*****  Creating Dummy Topic Connection Factory, WILL NOT BE USABLE!! *****");
	}

	@Override
	public Connection createConnection() throws JMSException {
		return dummy;
	}

	@Override
	public Connection createConnection(String userName, String password) throws JMSException {
		return dummy;
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		return dummy;
	}

	@Override
	public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
		return dummy;
	}

	@Override
	protected void buildFromProperties(Properties props) {
	}

	@Override
	protected void populateProperties(Properties props) {
	}

}
