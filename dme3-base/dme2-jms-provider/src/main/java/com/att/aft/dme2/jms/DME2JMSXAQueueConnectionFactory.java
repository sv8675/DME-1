/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.net.URISyntaxException;

import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;

import com.att.aft.dme2.jms.samples.TestClientSender;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2JMSXAQueueConnectionFactory extends DME2JMSQueueConnectionFactory implements XAQueueConnectionFactory {
	private static final long serialVersionUID = 1L;
	private static boolean firstTime = true;
	private static final Logger logger = LoggerFactory.getLogger(TestClientSender.class.getName());

	protected DME2JMSXAQueueConnectionFactory(DME2JMSManager manager, String uriStr) throws URISyntaxException {
		super(manager, uriStr);
		if (firstTime) {
			synchronized (DME2JMSXAQueueConnectionFactory.class) {
				if (firstTime) {
					firstTime = false;
					logger.info(null, "DME2JMSXAQueueConnectionFactory", JMSLogMessage.FACTORY_INIT);
				}
			}
		}
	}

	@Override
	public XAQueueConnection createXAQueueConnection() throws JMSException {
		return new DME2JMSXAQueueConnection(getManager(), getName(), null, null);
	}

	@Override
	public XAQueueConnection createXAQueueConnection(String username, String pwd) throws JMSException {
		return new DME2JMSXAQueueConnection(getManager(), getName(), username, pwd);
	}

	@Override
	public XAConnection createXAConnection() throws JMSException {
		return new DME2JMSXAQueueConnection(getManager(), getName(), null, null);
	}

	@Override
	public XAConnection createXAConnection(String username, String pwd) throws JMSException {
		return new DME2JMSXAQueueConnection(getManager(), getName(), username, pwd);
	}

}
