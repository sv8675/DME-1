package com.att.aft.dme2.iterator.domain;

/**
 * 
 * entity required by the iterator metrics collection for setting all the required attributes for raising any required event
 *
 */
public class IteratorMetricsEvent {

	private String serviceUri;
	private String conversationId;
	private String clientIp;
	private String role;
	private String protocol;
	private long timeOutMS=-1l;
	private long eventTime=-1l;
	private String partner;
	
	public String getPartner() {
		return partner;
	}
	public void setPartner(String partner) {
		this.partner = partner;
	}
	public long getEventTime() {
		return eventTime;
	}
	public void setEventTime(long eventTime) {
		this.eventTime = eventTime;
	}
	public String getServiceUri() {
		return serviceUri;
	}
	public void setServiceUri(String serviceUri) {
		this.serviceUri = serviceUri;
	}
	public String getConversationId() {
		return conversationId;
	}
	/**
	 * set a id preferably unique for a specific conversation 
	 * @param conversationId placeholder for the conversation id 
	 */
	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}
	public String getClientIp() {
		return clientIp;
	}
	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public long getTimeOutMS() {
		return timeOutMS;
	}
	public void setTimeOutMS(long timeOutMS) {
		this.timeOutMS = timeOutMS;
	}
	@Override
	public String toString() {
		return "IteratorMetricsEvent [serviceUri=" + serviceUri + ", conversationId=" + conversationId + ", clientIp="
				+ clientIp + ", role=" + role + ", partner=" + partner + ", protocol=" + protocol + ", timeOutMS=" + timeOutMS + ", eventTime="
				+ eventTime + "]";
	}
}