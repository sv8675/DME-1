/*
 * Copyright 2016 AT&T Intellectual Properties, Inc.
 */
package com.att.aft.dme2.api;

import java.io.InputStream;
import java.util.Map;

/**
 * Interface for async reply handlers.
 */
public interface DME2ReplyHandler {

	/**
	 * Called whenever an exception occurs during calls to the server.
	 * 
	 * @param requestHeaders
	 *            the request headers
	 * @param e
	 *            the e
	 */
	public void handleException( Map<String, String> requestHeaders, Throwable e );

	/**
	 * Called when a reply is returned from the server. This can be any reply,
	 * not only successful replies.
	 * 
	 * @param responseCode
	 *            the response code
	 * @param responseMessage
	 *            the response message
	 * @param in
	 *            the in
	 * @param requestHeaders
	 *            the request headers
	 * @param responseHeaders
	 *            the response headers
	 */

	public void handleReply( int responseCode, String responseMessage,
                           InputStream in, Map<String, String> requestHeaders,
                           Map<String, String> responseHeaders );

}
