package com.att.aft.dme2.server.api.websocket;

/**
 * Base class for implementing the handler for processing binary messages.
 *
 */
public abstract class DME2WSCliBinaryMessageHandler extends DME2WSCliMessageHandler {
	public DME2WSCliBinaryMessageHandler() {		
	};
	
	@Override
	public void processTextMessage(String Message) {

	}
  
	@Override
	public  void sendTextMessage(String message) {
		
	}
}
