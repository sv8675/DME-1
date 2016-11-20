/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;
public class DME2JMSNotImplementedException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public DME2JMSNotImplementedException(String method) {
		super(method + " not implemented");
	}

}
