package com.att.aft.dme2.server.api.websocket;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

class DME2WsConnectionRetry implements Runnable {
	/** The logger. */
	//private static Logger logger = DME2Constants.getLogger( DME2WsConnectionRetry.class.getName() );
  private static Logger logger = LoggerFactory.getLogger( DME2WsConnectionRetry.class );
	private DME2WSCliConnManager wsConnMgr;
	private String action = "retry";
	private int closeCode;
	private String message;

	public DME2WsConnectionRetry(DME2WSCliConnManager wsConnMgr, String action, int closeCode, String message) {
		this.wsConnMgr = wsConnMgr;
		this.action = action;
		this.closeCode = closeCode;
		this.message = message;		
	}

	@Override
	public void run() {
		try {
			if ((action != null) && action.toLowerCase().equals("retry"))
				wsConnMgr.retryConnection();
			else 
				wsConnMgr.failoverConnection();
		} catch (Exception e) {
			wsConnMgr.getDme2Socket().getHandler().onException(e);
		}
	}
}
