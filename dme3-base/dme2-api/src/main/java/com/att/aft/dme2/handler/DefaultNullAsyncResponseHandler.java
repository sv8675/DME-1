package com.att.aft.dme2.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * This class is used by DME2HttpClient when no DME2HttpReplyHandler
 * is provided.
 */
public class DefaultNullAsyncResponseHandler implements AsyncResponseHandlerIntf {
//	private static Logger logger = Logger.getLogger(DefaultAsyncReplyHandler.class.getName());
	/** The Constant MSG_PARSING_BUFFER. */
	private static final int MSG_PARSING_BUFFER = 8096;
	/** The logger. */
	private static final Logger logger = LoggerFactory.getLogger(DefaultNullAsyncResponseHandler.class.getName());
	

	@Override
	public void handleException(Map<String, String> requestHeaders, Throwable e) {

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
			logger.warn(null, "handleReply", "IOException: " , e);
			// ignore - this is not a buffer anyone cared about...
		}

	}

	@Override
	public String getResponse(long timeoutMs) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
