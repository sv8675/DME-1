package com.att.aft.dme2.util;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2ExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(DME2ExceptionHandler.class);

	/**
	 * Handles an Exception in the sense that it checks if the input Exception
	 * is an unchecked Exception. If it is, then a DME2Exception is created then
	 * the unchecked Exception is wrapped within the DME2Exception and returned.
	 * If the input Exception is of type DME2Exception, then it is just
	 * returned.
	 */
	public static DME2Exception handleException(Exception e, String uriString) {
		DME2Exception ex = null;

		if (e instanceof DME2Exception) {
			return (DME2Exception) e;
		} else {
			ErrorContext ec = new ErrorContext();
			ec.add("URI", uriString);
			ec.add("Exception", e.toString() );
			ec.add( "ExceptionMessage", e.getMessage() );

			ex = new DME2Exception("AFT-DME2-9000", ec, e);
			logger.error(null, "handleException", DME2Constants.EXCEPTION_HANDLER_MSG, ec, ex);
		}
		return ex;
	}
}