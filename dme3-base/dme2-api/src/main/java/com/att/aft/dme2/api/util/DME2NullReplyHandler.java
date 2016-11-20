/*
 * Copyright 2016 AT&T Intellectual Properties, Inc.
 */
package com.att.aft.dme2.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.att.aft.dme2.api.DME2ReplyHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * This class is used by DME2HttpClient when no DME2HttpReplyHandler
 * is provided.
 */
public class DME2NullReplyHandler implements DME2ReplyHandler {

	private static Logger logger = LoggerFactory.getLogger( DME2NullReplyHandler.class );
	/** The Constant MSG_PARSING_BUFFER. */
	private static final int MSG_PARSING_BUFFER = 8096;

	@Override
	public void handleException(Map<String, String> requestHeaders, Throwable e) {
     logger.error( null, "handleException", "Exception: ", e );
	}

	@Override
	public void handleReply(int rc, String rm, InputStream in,
			Map<String, String> requestHeaders,
			Map<String, String> responseHeaders) {

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
			logger.debug( null, "handleReply", LogMessage.DEBUG_MESSAGE, "IOException", e);

			// ignore - this is not a buffer anyone cared about...
		}

	}
}
