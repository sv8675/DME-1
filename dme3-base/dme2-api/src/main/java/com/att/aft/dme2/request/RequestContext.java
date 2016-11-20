package com.att.aft.dme2.request;

import com.att.aft.dme2.api.DME2Manager;

public class RequestContext {
	//
	private Request request;

	//
	private DME2Manager mgr;
	
	private DmeUniformResource uniformResource; 
	//
	private LoggingContext logContext;
	
	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}
	
	public DmeUniformResource getUniformResource() {
		return uniformResource;
	}

	public void setUniformResource(DmeUniformResource uniformResource) {
		this.uniformResource = uniformResource;
	}

	public DME2Manager getMgr() {
		return mgr;
	}

	public void setMgr(DME2Manager mgr) {
		this.mgr = mgr;
	}

	public LoggingContext getLogContext() {
		return logContext;
	}

	public void setLogContext(LoggingContext logContext) {
		this.logContext = logContext;
	}
	
	
}
