package com.att.aft.dme2.event;

public class EventProcessingException extends Exception {

	public EventProcessingException() {
	}

	public EventProcessingException(String message) {
		super(message);
	}

	public EventProcessingException(Throwable cause) {
		super(cause);
	}

	public EventProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public EventProcessingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
