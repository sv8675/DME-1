/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.util.Map;

public class DME2ExchangeFaultContext {
	private int responseCode;
	private Map<String,String> requestHeaders;
	private String service;
	private String routeOffer;
	private String requestURL;
  private String version;
	
	private Throwable exception;
	
	public DME2ExchangeFaultContext(String service, int responseCode, Map<String,String> requestHeaders, String routeOffer, String version, String requestURL, Throwable exception) {
		this.service = service;
		this.setRouteOffer(routeOffer);
		this.setRequestURL(requestURL);
		this.setResponseCode(responseCode);
		this.setRequestHeaders(requestHeaders);
		this.setException(exception);
    this.version = version;
	}

	public Map<String,String> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(Map<String,String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getRouteOffer() {
		return routeOffer;
	}

	public void setRouteOffer(String routeOffer) {
		this.routeOffer = routeOffer;
	}

	public String getRequestURL() {
		return requestURL;
	}

	public void setRequestURL(String requestURL) {
		this.requestURL = requestURL;
	}

  public String getVersion() {
    return version;
  }

  public void setVersion( String version ) {
    this.version = version;
  }
}
