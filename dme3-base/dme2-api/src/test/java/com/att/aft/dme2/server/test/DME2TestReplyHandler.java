/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.io.InputStream;
import java.util.Map;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.DME2SimpleReplyHandler;

public class DME2TestReplyHandler extends DME2SimpleReplyHandler {
	
	private Map<String, String> responseHeaders;

	public DME2TestReplyHandler(DME2Configuration config, String service, boolean allowAllHttpReturnCodes) {
		super(config, service, allowAllHttpReturnCodes);
	}

	@Override
	public void handleException(Map<String, String> requestHeaders,
			Throwable e) {
		super.handleException(requestHeaders, e);
		this.echoedCharSet = requestHeaders.get("com.att.aft.dme2.test.charset");				
	}

	@Override
	public void handleReply(int responseCode, String responseMessage,
			InputStream in, Map<String, String> requestHeaders,
			Map<String, String> responseHeaders) {
		this.echoedCharSet = responseHeaders.get("com.att.aft.dme2.test.charset");
		this.responseHeaders = responseHeaders;
		super.handleReply(responseCode,  responseMessage, in, requestHeaders, responseHeaders);
	}
	
	public Map<String, String> getResponseHeaders()
	{
		return responseHeaders;
	}
	
	public String echoedCharSet = null;
}
