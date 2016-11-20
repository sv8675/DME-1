/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

public class DME2JMSInitialContextFactory implements InitialContextFactory {

	public DME2JMSInitialContextFactory() throws Exception {

	}
	
	@Override
	public Context getInitialContext(Hashtable<?, ?> environment)
			throws NamingException {
		return new DME2JMSInitialContext(environment);
	}

}
