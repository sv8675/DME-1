/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.Session;
import javax.jms.XASession;

public abstract class DME2JMSSession implements Session, XASession {
	public abstract boolean isOpen();
}
