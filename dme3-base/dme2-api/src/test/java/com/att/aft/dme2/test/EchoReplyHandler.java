/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;

/**
 * The Class EchoReplyHandler.
 */
public class EchoReplyHandler implements AsyncResponseHandlerIntf {


	/** The Constant MSG_PARSING_BUFFER. */
	private static final int MSG_PARSING_BUFFER = 8096;

	/** The e. */
	private Throwable e = null;

	/** The response. */
	private String response = null;

	/** The waiter. */
	private byte[] waiter = new byte[0];

	private Map<String,String> respheaders;
	/**
	 * Gets the response.
	 * 
	 * @param timeoutMs
	 *            the timeout ms
	 * @return the response
	 * @throws Exception
	 *             the exception
	 */
	public String getResponse(long timeoutMs) throws Exception {
		synchronized (waiter) {
			if (response != null) {
				return response;
			} else if (e != null) {
				if (!(e instanceof Exception)) {
					throw new RuntimeException(e);
				} else {
					throw (Exception) e;
				}
			}

			waiter.wait(timeoutMs);

			if (response != null) {
				return response;
			} else if (e != null) {
				if (!(e instanceof Exception)) {
					throw new RuntimeException(e);
				} else {
					throw (Exception) e;
				}
			}

			throw new Exception("Timed out");
		}
	}


	@Override
	public void handleException(Map<String, String> requestHeaders, Throwable e) {
		System.out.println("===================inside handleException================" + requestHeaders);
		System.out.println("===================inside handleException================" + e.getMessage());
		this.e = e;
		this.respheaders = requestHeaders;
		synchronized (waiter) {
			waiter.notifyAll();
		}
	}


	@Override
	public void handleReply(int responseCode, String responseMessage,
			InputStream in, Map<String, String> requestHeaders,
			Map<String, String> responseHeaders) {
		System.out.println("===================inside handleReply================" + requestHeaders);
		this.respheaders = responseHeaders;
		if (responseCode == 200) {
			// TODO: code parsing to a string
			// response = "got a response";
			// response = responseMessage;
			// read through reply stream
			InputStreamReader input = new InputStreamReader(in/* , "UTF-8" */);
			final char[] buffer = new char[MSG_PARSING_BUFFER];
			StringBuilder output = new StringBuilder(MSG_PARSING_BUFFER);
			try {
				for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input
						.read(buffer, 0, buffer.length)) {
					output.append(buffer, 0, read);
				}
			} catch (IOException e) {
				// ignore - this is not a buffer anyone cared about...
			}
			response = output.toString();
		} else {
			if(responseMessage != null) {
				response = responseMessage;
			}
			else {
				e = new Exception("Call Failed, RC=" + responseCode + " - "
					+ responseMessage);
			}
			
		}
		synchronized (waiter) {
			waiter.notifyAll();
		}
	}

	public Map<String,String> getResponseHeaders() {
		return this.respheaders;
	}
}
