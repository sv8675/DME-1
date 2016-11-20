/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.client.api.Result;

import com.att.aft.dme2.request.DME2Payload;

/*
 * Base class for handling DME2 Response.
 * Subclasses can be HTTPRespone or WebSocket Response
 */
public class DME2Response {
	private Map<String, String> responseHeaders = new HashMap<String, String>();
	private String replyMessage;

	public String getReplyMessage() {
		return replyMessage;
	}

	public void setReplyMessage(String replyMessage) {
		this.replyMessage = replyMessage;
	}

	public Result result;

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public Map<String, String> respHeaders;

	public Map<String, String> getRespHeaders() {
		return respHeaders;
	}

	public void setRespHeaders(Map<String, String> respHeaders) {
		this.respHeaders = respHeaders;
	}

	public Integer respCode;

	public Integer getRespCode() {
		return respCode;
	}

	public void setRespCode(Integer respCode) {
		this.respCode = respCode;
	}

	public DME2Payload payload;

	public DME2Payload getPayload() {
		return payload;
	}



}
