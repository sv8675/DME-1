/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.JMSException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.util.ErrorCatalog;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSException extends JMSException {
	private static final long serialVersionUID = 1L;

	public DME2JMSException(String msg, Throwable cause) {
		super(msg);
		super.initCause(cause);
	}

	public DME2JMSException(String code, ErrorContext context, Throwable t) {
		this(code, ErrorCatalog.getInstance().getErrorMessage(code, context), t);
	}

	public DME2JMSException(String code, ErrorContext context) {
		this(code, ErrorCatalog.getInstance().getErrorMessage(code, context));
	}

	public DME2JMSException(String msg) {
		super(msg);
	}

	public DME2JMSException(String errorCode, String errorMessage) {
		super("[" + errorCode + "]: " + errorMessage, errorCode);
	}

	public DME2JMSException(String errorCode, String errorMessage, Throwable cause) {
		super("[" + errorCode + "]: " + errorMessage, errorCode);
		super.initCause(cause);
	}

	public DME2JMSException(DME2Exception e) {
		super("[" + e.getErrorCode() + "]: " + e.getErrorMessage(), e.getErrorCode());
		if (e.getCause() == null) {
			super.initCause(e);
		} else {
			super.initCause(e.getCause());
		}
	}
	/**
	 * public DME2JMSException(String code, String format, Object... objs) {
	 * super("[" + code + "]: " + String.format(format, objs)); this.code =
	 * code; this.msg = String.format(format, objs); }
	 */
}
