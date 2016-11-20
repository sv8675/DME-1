/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.XAQueueSession;

public class DME2JMSXAQueueSession extends DME2JMSQueueSession implements XAQueueSession {

	protected DME2JMSXAQueueSession(DME2JMSXAQueueConnection connection, boolean transacted, int ackMode) {
		super(connection, transacted, ackMode);
	}
}
