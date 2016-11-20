/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

public class DME2ServiceRequestData {
	private long requestTimeout = 0;
	private long eventTime = 0;
	private String messageID = null;
	private String clientAddrString = null;

	public DME2ServiceRequestData(String messageID, long requestTimeout,
			long eventTime, String clientAddrString) {
		this.messageID = messageID;
		this.requestTimeout = requestTimeout;
		this.eventTime = eventTime;
		this.setClientAddrString(clientAddrString);
	}

	public long getRequestTimeout() {
		return requestTimeout;
	}

	public long getEventTime() {
		return eventTime;
	}

	public String getMessageID() {
		return messageID;
	}

	public void setClientAddrString(String clientAddrString) {
		this.clientAddrString = clientAddrString;
	}

	public String getClientAddrString() {
		return clientAddrString;
	}

}
