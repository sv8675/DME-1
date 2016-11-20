/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import javax.jms.JMSException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.jms.DME2JMSException;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSExceptionHandler.class.getName());

	public static DME2JMSException handleException(Exception e, String queueName) throws JMSException {
		DME2JMSException ex = null;

		if (e instanceof JMSException) {
			throw (JMSException) e;
		} else {
			ErrorContext ec = new ErrorContext();
			ec.add("queueName", queueName);

			DME2Exception dme2Exception = new DME2Exception("AFT-DME2-9000", ec, e);
			ex = new DME2JMSException(dme2Exception);
			logger.error(null, "onTimeout", "AFT-DME2-9000", ec, ex);
		}
		return ex;
	}
}
