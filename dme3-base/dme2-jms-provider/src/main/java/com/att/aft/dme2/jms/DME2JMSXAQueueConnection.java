/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueSession;
import javax.jms.XASession;

public class DME2JMSXAQueueConnection extends DME2JMSQueueConnection implements XAQueueConnection {

	public DME2JMSXAQueueConnection(DME2JMSManager manager, String name, String username, String password) {
		super(manager, name, username, password);
	}

	@Override
	public XAQueueSession createXAQueueSession() throws JMSException {
		return new DME2JMSXAQueueSession(this, true, Session.AUTO_ACKNOWLEDGE);
	}

	@Override
	public XASession createXASession() throws JMSException {
		return new DME2JMSXAQueueSession(this, true, Session.AUTO_ACKNOWLEDGE);
	}

}
