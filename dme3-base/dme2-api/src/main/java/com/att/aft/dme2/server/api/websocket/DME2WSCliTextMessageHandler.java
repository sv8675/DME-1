package com.att.aft.dme2.server.api.websocket;

/**
 *  Base class that defines teh test message interface. 
 *  Clients will derive from this class for processing text messages
 *
 */
public abstract class DME2WSCliTextMessageHandler extends DME2WSCliMessageHandler {

	public DME2WSCliTextMessageHandler() {};
	

	@Override
	public void processBinaryMessage(byte[] arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void sendBinaryMessage(byte[] data, int offset, int length) {
	
	}
   
}
