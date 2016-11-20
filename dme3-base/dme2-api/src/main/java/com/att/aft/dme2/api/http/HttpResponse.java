/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.http;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpFields;

import com.att.aft.dme2.api.DME2Response;
import com.att.aft.dme2.request.DME2Payload;

/*
 * This class is for HttpResponse.
 * Clients can use this class set the Response Headers and
 * can determine if fail over is required or not when passed to FailoverHandler interface.
 */
public class HttpResponse extends DME2Response {
	private Map<String, String> responseHeaders = new HashMap<String, String>();

	public String getReplyMessage() {
		return super.getReplyMessage();
	}

	public void setReplyMessage(String replyMessage) {
		super.setReplyMessage(replyMessage);
	}

	public Integer getRespCode() {
		return respCode;
	}

	public void setRespCode(Integer respCode) {
		this.respCode = respCode;
	}

	public Map<String, String> getRespHeaders() {
		return respHeaders;
	}

	public void setRespHeaders(Map<String, String> respHeaders) {
		this.respHeaders = respHeaders;
	}

	private Integer respCode;
	private Map<String, String> respHeaders;

	public void buildResponseObject(Result result) {
		this.setReplyMessage(result.getResponse().toString());
		this.setRespCode(result.getResponse().getStatus());
		HttpFields httpFields = result.getResponse().getHeaders();
		Map<String, String> headersMap = convertResponseHeadersAsMap(httpFields);
		this.setRespHeaders(headersMap);
		this.respCode = result.getResponse().getStatus();
	}

	public Map<String, String> convertResponseHeadersAsMap(HttpFields httpFields) {
		Map<String, String> _headers = new HashMap<String, String>();
		if (httpFields == null) {
			return _headers;
		}
		Enumeration<String> e1 = httpFields.getFieldNames();
		while (e1.hasMoreElements()) {
			String key = e1.nextElement();
			_headers.put(key, httpFields.get(key));
		}

		if (MapUtils.isNotEmpty(responseHeaders)) {
			_headers.putAll(responseHeaders);
		}
		return _headers;
	}

	public void setPayLoad(DME2Payload payload) {
		this.payload = payload;

	}
}
