package com.att.aft.dme2.event;

/**
 * The EventType enum specifes various event types.
 * 
 * enum EventType
 *
 */
public enum EventType {
	REQUEST_EVENT("REQUEST_EVENT"), REPLY_EVENT("REPLY_EVENT"), FAULT_EVENT("FAULT_EVENT"), CANCEL_REQUEST_EVENT("CANCEL_REQUEST_EVENT"),LOG_EVENT(
			"LOG_EVENT"), TIMEOUT_EVENT("TIMEOUT_EVENT"), INIT_EVENT("INIT_EVENT"), FAILOVER_EVENT("FAILOVER_EVENT");

	private final String name;

	public String getName() {
		return name;
	}

	EventType(String name) {
		this.name = name;
	}
}
