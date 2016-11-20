package com.att.aft.dme2.event;

public class EventQueueException extends Exception {

	public EventQueueException() {
	}

	public EventQueueException(String message) {
		super(message);
	}

	public EventQueueException(Throwable cause) {
		super(cause);
	}

	public EventQueueException(String message, Throwable cause) {
		super(message, cause);
	}

	public EventQueueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
