/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.util.UUID;

import javax.jms.MessageConsumer;

public abstract class DME2JMSMessageConsumer implements MessageConsumer {
	private String id = UUID.randomUUID().toString();

	public void setID(String id) {
		this.id = id;
	}

	public String getID() {
		return id;
	}

	
}
