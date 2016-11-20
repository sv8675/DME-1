/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.exception;


/**
 * The Class DME3Exception.
 */

public class ConfigException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The code. */
	private String code = null;

	/** The msg. */
	private String msg = null;

	/**
	 * Instantiates a new e http exception.
	 * 
	 * @param code
	 *            the code
	 * @param format
	 *            the format
	 * @param objs
	 *            the objs
	 */
	public ConfigException(String code, String format, Object... objs) {
		super("[" + code + "]: " + String.format(format, objs));
		this.code = code;
		this.msg = String.format(format, objs);
	}

	/**
	 * Instantiates a new e http exception.
	 * 
	 * @param code
	 *            the code
	 * @param format
	 *            the format
	 * @param objs
	 *            the objs
	 */
	public ConfigException(String code, String msg, Throwable e) {
		super("[" + code + "]: " + msg, e);
		this.code = code;
		this.msg = msg;
	}

	/**
	 * Instantiates a new e http exception.
	 * 
	 * @param code
	 *            the code
	 * @param msg
	 *            the msg
	 */
	public ConfigException(String code, Throwable msg) {
		super("[" + code + "]: " + msg.getLocalizedMessage(), msg);
		this.code = code;
		this.msg = msg.getLocalizedMessage();
	}

	/**
	 * Gets the error code.
	 * 
	 * @return the error code
	 */
	public String getErrorCode() {
		return code;
	}

	/**
	 * Gets the error message.
	 * 
	 * @return the error message
	 */
	public String getErrorMessage() {
		return msg;
	}

}
