package com.att.aft.dme2.event;

import java.util.HashMap;

/**
 * DME2Event class represents a DME2 API event.
 * Earlier in DME2 it was a Hashmap with various properties.
 * Each event has a unique MessageId which identifies the event.
 * 
 */

public class DME2Event {
	long eventTime;
	String messageId;
	long elapsedTime;
	long replyMsgSize;
	String role;
	String partner;
	String protocol;
	EventType type;
	String service;
	String queueName;
	String clientAddress;
	long reqMsgSize;
	String interfacePort;

	public String getInterfacePort() {
		return interfacePort;
	}

	public void setInterfacePort(String interfacePort) {
		this.interfacePort = interfacePort;
	}

	public long getReqMsgSize() {
		return reqMsgSize;
	}

	public void setReqMsgSize(long reqMsgSize) {
		this.reqMsgSize = reqMsgSize;
	}

	public void setClientAddress(String clientAddress) {
		this.clientAddress = clientAddress;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	@Override
	public String toString() {
		return "Event [messageId=" + messageId + ", type=" + type + "]";
	}

	HashMap<String, Object> eventProps;

	public HashMap<String, Object> getEventProps() {
		return eventProps;
	}

	public void setEventProps(HashMap<String, Object> eventProps) {
		this.eventProps = eventProps;
	}

	public long getEventTime() {
		return eventTime;
	}

	public void setEventTime(long eventTime) {
		this.eventTime = eventTime;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public long getElapsedTime() {
		return elapsedTime;
	}

	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	public long getReplyMsgSize() {
		return replyMsgSize;
	}

	public void setReplyMsgSize(long replyMsgSize) {
		this.replyMsgSize = replyMsgSize;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getPartner() {
		return partner;
	}

	public void setPartner(String partner) {
		this.partner = partner;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getQueueName() {
		return queueName;
	}

	public String getClientAddress() {
		return clientAddress;
	}
}
