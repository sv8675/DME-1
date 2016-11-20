package com.att.aft.dme2.request;

public class LoggingContext {
	private String trackingId;
	private String conversationId;
	private boolean sendTraceInfo; 
	private boolean traceInfo; 
	
	public boolean isSendTraceInfo() {
		return sendTraceInfo;
	}

	public void setSendTraceInfo(boolean sendTraceInfo) {
		this.sendTraceInfo = sendTraceInfo;
	}

	public String getTrackingId() {
		return trackingId;
	}
	
	public void setTrackingId(String trackingId) {
		this.trackingId = trackingId;
	}
	
	public String getConversationId() {
		return conversationId;
	}
	
	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}	
	
	public void startStep(String step) {
		
	}
	
	public void completeStep(String step) {
		
	}
}
