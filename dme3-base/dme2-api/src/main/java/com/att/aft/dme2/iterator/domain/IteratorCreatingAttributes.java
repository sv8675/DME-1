package com.att.aft.dme2.iterator.domain;

import java.util.List;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.event.EventProcessor;

public class IteratorCreatingAttributes {

	private EventProcessor requestEventProcessor;
	private EventProcessor replyEventProcessor;
	private EventProcessor faultEventProcessor;
	private EventProcessor timeoutEventProcessor;
	private EventProcessor cancelRequestEventProcessor;
	private DME2Manager manager; 
	private List<DME2EndpointReference> endpointHolders;
	private String queryParamMinActiveEndPoint;
	private DME2Configuration config;
	
	public EventProcessor getCancelRequestEventProcessor() {
		return cancelRequestEventProcessor;
	}
	public void setCancelRequestEventProcessor(EventProcessor cancelRequestEventProcessor) {
		this.cancelRequestEventProcessor = cancelRequestEventProcessor;
	}
	public EventProcessor getRequestEventProcessor() {
		return requestEventProcessor;
	}
	public void setRequestEventProcessor(EventProcessor requestEventProcessor) {
		this.requestEventProcessor = requestEventProcessor;
	}
	public EventProcessor getReplyEventProcessor() {
		return replyEventProcessor;
	}
	public void setReplyEventProcessor(EventProcessor replyEventProcessor) {
		this.replyEventProcessor = replyEventProcessor;
	}
	public EventProcessor getFaultEventProcessor() {
		return faultEventProcessor;
	}
	public void setFaultEventProcessor(EventProcessor faultEventProcessor) {
		this.faultEventProcessor = faultEventProcessor;
	}
	public EventProcessor getTimeoutEventProcessor() {
		return timeoutEventProcessor;
	}
	public void setTimeoutEventProcessor(EventProcessor timeoutEventProcessor) {
		this.timeoutEventProcessor = timeoutEventProcessor;
	}
	public DME2Manager getManager() {
		return manager;
	}
	public void setManager(DME2Manager manager) {
		this.manager = manager;
	}
	public List<DME2EndpointReference> getEndpointHolders() {
		return endpointHolders;
	}
	public void setEndpointHolders(List<DME2EndpointReference> endpointHolders) {
		this.endpointHolders = endpointHolders;
	}
	public String getQueryParamMinActiveEndPoint() {
		return queryParamMinActiveEndPoint;
	}
	public void setQueryParamMinActiveEndPoint(String queryParamMinActiveEndPoint) {
		this.queryParamMinActiveEndPoint = queryParamMinActiveEndPoint;
	}
	public DME2Configuration getConfig() {
		return config;
	}
	public void setConfig(DME2Configuration config) {
		this.config = config;
	}
}
