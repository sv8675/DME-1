/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.util.Map;

public class DME2ExchangeResponseContext {
	private int responseCode;
	private Map<String,String> requestHeaders;
	private Map<String,String> responseHeaders;
	private String service;
	private String routeOffer;
  private String version;
	private String requestURL;

	public DME2ExchangeResponseContext(String service, int responseCode, Map<String,String> requestHeaders, Map<String,String> responseHeaders, String routeOffer, String version, String requestURL) {
		this.service = service;
		this.routeOffer = routeOffer;
    this.version = version; // version should also be in requestURL
		this.requestURL = requestURL;
		this.setResponseCode(responseCode);
		this.setRequestHeaders(requestHeaders);
		this.setResponseHeaders(responseHeaders);
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public Map<String,String> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(Map<String,String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public Map<String,String> getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String,String> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
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
