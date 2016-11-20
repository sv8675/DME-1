package com.att.aft.dme2.server.api.websocket;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * Basec lass that defines the messages interface
 *
 */
public abstract class DME2WSCliMessageHandler {
	private static Logger logger = LoggerFactory.getLogger( DME2WSCliMessageHandler.class );

	// DME2 connection object that wraps the Jetty Websocket connection object
	private DME2WSCliConnection connection;

	// Method to be overridden by the client and gets invoked when a message is
	// received from the server
	public abstract void processTextMessage(String message) ;

	// Method to be overridden by the client and gets invoked when a websocket
	// connection is closed
	public abstract void onConnClose(int closeCode, String message);
	
	// Method to be overridden by the client and gets invoked when a websocket
	// connection is opened
	public abstract void onOpen(DME2WSCliConnection conn);   

	public abstract void processBinaryMessage(byte[] data, int offset,
			int length);

	public DME2WSCliMessageHandler() {
	};

	public DME2WSCliConnection getConnection() {
		return connection;
	}

	public void setConnection(DME2WSCliConnection connection) {
		this.connection = connection;
	}

	public void sendTextMessage(String message) throws DME2Exception {
		connection.sendMessage(message);
	}

	public void sendBinaryMessage(byte[] data, int offset, int length) throws DME2Exception {
		connection.sendMessage(data, offset, length);
	}

	public void closeConnection() throws DME2Exception {
		connection.close();
	}
	
	public abstract void onException(Exception e);
	
}
