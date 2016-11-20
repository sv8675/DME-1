package com.att.aft.dme2.server.api.websocket;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jetty.websocket.core.api.WebSocketConnection;
import org.eclipse.jetty.websocket.core.api.WebSocketException;
import org.eclipse.jetty.websocket.core.api.WebSocketListener;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class GRMHealthCheckServerWebSocket implements WebSocketListener {//, WebSocket.OnTextMessage {
	protected WebSocketConnection _connection;
	private static final String CLASS_NAME = "com.att.aft.dme2.server.api.websocket.GRMHealthCheckServerWebSocket";

  private static Logger logger = LoggerFactory.getLogger( CLASS_NAME );
	
	public GRMHealthCheckServerWebSocket() {

	}
	
	@Override
	public void onWebSocketConnect(WebSocketConnection connection) {
		// TODO Auto-generated method stub
		logger.info( null, "onWebSocketText(WebSocketConnection)", LogMessage.DEBUG_MESSAGE, "onOpen:GRMHealthCheckServerWebSocket ");
		this._connection = connection;
	}

	@Override
	public void onWebSocketText(String data) {

		logger.info( null, "onWebSocketText", LogMessage.DEBUG_MESSAGE, "onMessage:GRMHealthCheckServerWebSocket {}",data);
		
		try {
			//TODO
			this._connection.write(null, null, "I AM GREAT");
		} catch (Exception e) {
			ErrorContext ec = new ErrorContext();
			ec.add("Code", "DME2Server.Fault");
			ec.add("extendedMessage", e.getMessage());
			ec.add("StackTrace", ExceptionUtils.getStackTrace(e));
			logger.info( null, "onWebSocketText", LogMessage.WS_SERVER_GRM_HEALTHCHECK_EXCEPTION,ec);
		}
	}

	public void onClose(int closeCode, String message) {
		// TODO Auto-generated method stub
		logger.info( null, "onClose", LogMessage.DEBUG_MESSAGE, "onClose:GRMHealthCheckServerWebSocket: closeCode: {} message: {}", closeCode, message);
		this._connection = null;
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWebSocketException(WebSocketException error) {
		// TODO Auto-generated method stub
		
	}
}
