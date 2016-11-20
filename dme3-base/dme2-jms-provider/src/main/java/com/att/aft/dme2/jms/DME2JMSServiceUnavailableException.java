/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.JMSException;

import com.att.aft.dme2.util.ErrorCatalog;
import com.att.aft.dme2.util.ErrorContext;

@SuppressWarnings("serial")
public class DME2JMSServiceUnavailableException extends JMSException {

	public DME2JMSServiceUnavailableException(String reason) {
		super(reason);
	}
	
	public DME2JMSServiceUnavailableException(String code, ErrorContext context) { this(code, ErrorCatalog.getInstance().getErrorMessage(code, context)); }
	public DME2JMSServiceUnavailableException(String code, ErrorContext context, Throwable t) { this(code, ErrorCatalog.getInstance().getErrorMessage(code, context), t); }
	
	public DME2JMSServiceUnavailableException(String code, String reason) {
		super(code, reason);
	}
	
	public DME2JMSServiceUnavailableException(String code, String format, Object... objs) {
		super("[" + code + "]: " + String.format(format, objs));
	}
}
