package com.att.aft.dme2.server.api.websocket;

import java.util.HashMap;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.core.api.WebSocketConnection;
import org.eclipse.jetty.websocket.core.api.WebSocketException;
import org.eclipse.jetty.websocket.core.api.WebSocketListener;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;

/**
 * Implements the jetty websock and text and binary message interfaces. 
 * This class is registered during websocketclient creation. This class gets invoked when connection is
 * opened, closed and when messages are received. 
 * 
 * Logging and failover of connection if enabled happens when the connection si closed for specific reasons.
 * 
 *
 */
public class DME2CliWebSocket implements WebSocketListener { //implements WebSocket, WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {
	
	private static Logger logger = LoggerFactory.getLogger( DME2CliWebSocket.class );
    private DME2WSCliConnection connection = null;
    private DME2WSCliMessageHandler handler = null;
    private WebSocketClient wsClient = null;
    private DmeUniformResource uri = null;
	private DME2Manager dme2Mgr = null;
	private String endpoint = null;
	private int maxConnectionIdleTime = 60;
	private String trackingID;
	/** Captures failures with ep's attempted **/
	private final StringBuffer epTraceRoute = new StringBuffer();
	public final static  String EP ="[EP=";
	private DME2WSCliConnManager wsConnMgr;
	private long connectionOpenedTime;
	private boolean logStats = true;
	private final String RETRY = "retry";
	private final String FAILOVER = "failover";
	private DME2Configuration config = new DME2Configuration();
	
    public DME2CliWebSocket(DME2WSCliMessageHandler handler, DME2Manager mgr) {
	    this.handler = handler;
	    this.dme2Mgr = mgr;
    }	      
	
    @Override
    public void onWebSocketText(String message)
    {    	
    	//Message received. Log it and send the event
		logger.info( null, "onWebSocketText", LogMessage.WS_CONNECTION_RECEIVE_MSG, connection.getTrackingId(),this.uri, this.endpoint, message.length());
    	
		if (logStats) {
			/* Post response event to Metrics Service */
			HashMap<String, Object> props = new HashMap<String, Object>();
			props.put(DME2Constants.MSG_SIZE, message.length());
			props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
			props.put(DME2Constants.REPLY_EVENT, true);
			props.put(DME2Constants.QUEUE_NAME, this.uri.getUrl().toString());
			props.put(DME2Constants.DME2_INTERFACE_PORT, this.uri.getUrl().getPort() + "");
			props.put(DME2Constants.ELAPSED_TIME, 0);
			props.put(DME2Constants.MESSAGE_ID, this.trackingID);
			props.put(DME2Constants.DME2_INTERFACE_ROLE, config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
			props.put(DME2Constants.DME2_INTERFACE_PROTOCOL, DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
			props.put(DME2Constants.REPLY_EVENT, true);
			
			if (this.uri.getPartner() != null)
			{
				props.put(DME2Constants.DME2_REQUEST_PARTNER, this.uri.getPartner());
			}
	
			logger.debug( null, "onWebSocketText", "DME2Exchange postWSReceiveEvent ", props);
			dme2Mgr.postStatEvent( props );
		}

		try {
			handler.processTextMessage(message);
		} catch (Exception e) {
			logger.warn( null, "onWebSocketText", LogMessage.WS_CLI_HANDLER_EXCEPTION, "OnMessage(text)", this.uri, this.endpoint, connection.getTrackingId(), e.getMessage());
		}
    }
	       
    @Override
    public void onWebSocketConnect(WebSocketConnection conn)    
    {   
    	synchronized(this.wsConnMgr.getLock()) {
 	    	if (this.connection == null) {
	    		this.connection = new DME2WSCliConnection(conn, dme2Mgr, wsConnMgr);
	    		this.getHandler().setConnection(connection);
	    		this.connection.setMaxIdleTime(maxConnectionIdleTime);
	    		this.connection.setUri(uri);
	    		this.connection.setTrackingId(trackingID);
	    		this.connection.setEndpoint(endpoint); 
	    		this.wsConnMgr.setUserClose(false);
	    	} else {
	    		//this must be the case of a failover
//	    		this.getHandler().setConnection(connection);
	    		this.connection.setMaxIdleTime(maxConnectionIdleTime);
	    		this.connection.setUri(uri);
	    		this.connection.setTrackingId(trackingID);
	    		this.connection.setEndpoint(endpoint);  
	    		this.connection.setConnection(conn);
	    		this.wsConnMgr.setUserClose(false);
	    	}    	
    	
	    	connectionOpenedTime = System.currentTimeMillis();
	    	//Connection Open. Log it and send the event
	    	logger.info( null, "onWebSocketConnect(WebSocketConnection)", LogMessage.WS_CONNECTION_OPEN_MSG, this.connection.getTrackingId(), this.uri, this.endpoint);
	    	
			if (logStats) {
				/* Post request statistics to Metrics Service */
				HashMap<String, Object> props = new HashMap<String, Object>();
				props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
				props.put(DME2Constants.FAULT_EVENT, false);
				props.put(DME2Constants.QUEUE_NAME, endpoint);
				props.put(DME2Constants.ELAPSED_TIME, 0);
				props.put(DME2Constants.DME2_WS_CONNECT_ID, this.connection.getTrackingId());
				props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
				props.put(DME2Constants.DME2_INTERFACE_ROLE, config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
		
				if (this.uri.getPartner() != null)
				{
					props.put(DME2Constants.DME2_REQUEST_PARTNER, this.uri.getPartner());
				}

				logger.debug( null, "onWebSocketConnect", "DME2Exchange postWSConnectionOpenEvent {}", props);
				dme2Mgr.postStatEvent( props );
			}
			
			try {
				handler.onOpen(connection);
			} catch (Exception e) {
				logger.warn( null, "onWebSocketConnect(WebSocketConnection)", LogMessage.WS_CLI_HANDLER_EXCEPTION, "OnOpen",
					this.uri, this.endpoint, connection.getTrackingId(), e.getMessage());
			}
    	}
    }
   
    @Override
    public void onWebSocketClose(int closeCode, String message)
    {
    	if (connection == null)
    		return;
    	//Message received. Log it and send the event
    	logger.info( null, "onWebSocketClose", LogMessage.WS_CONNECTION_CLOSE_MSG, connection.getTrackingId(),  closeCode, this.uri, this.endpoint);
    	
		if (logStats) {
			/* Post request statistics to Metrics Service */
			HashMap<String, Object> props = new HashMap<String, Object>();
			props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
			props.put(DME2Constants.FAULT_EVENT, false);
			props.put(DME2Constants.QUEUE_NAME, endpoint);
			props.put(DME2Constants.ELAPSED_TIME, (System.currentTimeMillis() - getConnection().getConnectStartTime()));
			props.put(DME2Constants.DME2_WS_CONNECT_ID, this.connection.getTrackingId());
			props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
			props.put(DME2Constants.DME2_INTERFACE_ROLE, config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
	
			if (this.uri.getPartner() != null)
			{
				props.put(DME2Constants.DME2_REQUEST_PARTNER, this.uri.getPartner());
			}

			logger.debug( null, "onWebSocketConnect", "DME2Exchange postWSConnectionCloseEvent ", props);
			dme2Mgr.postStatEvent( props );
		}
		
		//check the return codes and determine if this is a normal close or requires retry or a new connection
		//ASsume that the client is hte only one that can explicitly close the connection. Failover in any other case i.e 
		//idletimeout or server closes the connection
		synchronized (this.wsConnMgr.getLock()) {
			try {
				if ((closeCode == 1000) && (!wsConnMgr.isUserClose())) {
					long currentTime = System.currentTimeMillis();
					long elapsedTime = currentTime - connectionOpenedTime;
					//checking to see if the close happened bacuase of idle connection
					if (elapsedTime >= this.maxConnectionIdleTime) {
						logger.debug( null, "onWebSocketClose", LogMessage.WS_CONN_RETRY, this.uri, connection.getTrackingId(),  closeCode,this.endpoint);
	
						wsConnMgr.setUserClose(false);
						this.dme2Mgr.getWsRetryThreadpool().submit(new DME2WsConnectionRetry(wsConnMgr, RETRY, closeCode, message));
					} else {
						handler.onConnClose(closeCode, message);
					}
					return;
				} else if((closeCode == 1000) && (wsConnMgr.isUserClose())) {
					handler.onConnClose(closeCode, message);
					return;
				}
				
				if (wsConnMgr.isFailoverRequired(message, closeCode)) {				
					if (wsConnMgr.isRetryRequired(message, closeCode)) {
						logger.debug( null, "onWebSocketClose", LogMessage.WS_CONN_RETRY, this.uri, connection.getTrackingId(),closeCode, this.endpoint);
						this.dme2Mgr.getWsRetryThreadpool().submit(new DME2WsConnectionRetry(wsConnMgr, RETRY, closeCode, message));
					} else if (wsConnMgr.isHandleFailover()) {
						logger.debug( null, "onWebSocketClose", LogMessage.WS_CONN_FAILOVER, this.uri, connection.getTrackingId());
						this.dme2Mgr.getWsRetryThreadpool().submit(new DME2WsConnectionRetry(wsConnMgr, FAILOVER, closeCode, message));
					} else {
						handler.onConnClose(closeCode, message);
					}
				} else {
					handler.onConnClose(closeCode, message);
				}
			} catch (Exception e) {
				logger.warn( null, "onWebSocketClose", LogMessage.WS_CLI_HANDLER_EXCEPTION, this.uri, this.endpoint, connection.getTrackingId(), e.getMessage());
			}
		}
    }

	@Override
	public void onWebSocketBinary(byte[] data, int offset, int length) {
		synchronized(this.wsConnMgr.getLock()) {		
	    	//Message received. Log it and send the event
			logger.info( null, "onWebSocketBinary", LogMessage.WS_CONNECTION_RECEIVE_MSG, connection.getTrackingId(), this.uri, this.endpoint, data.length);
			
			if (logStats) {
				/* Post response event to Metrics Service */
				HashMap<String, Object> props = new HashMap<String, Object>();
				props.put(DME2Constants.MSG_SIZE, data.length);
				props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
				props.put(DME2Constants.REPLY_EVENT, true);
				props.put(DME2Constants.QUEUE_NAME, this.uri.getUrl().toString());
				props.put(DME2Constants.DME2_INTERFACE_PORT, this.uri.getUrl().getPort() + "");
				props.put(DME2Constants.ELAPSED_TIME, 0);
				props.put(DME2Constants.MESSAGE_ID, this.trackingID);
				props.put(DME2Constants.DME2_INTERFACE_ROLE, config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
				props.put(DME2Constants.DME2_INTERFACE_PROTOCOL, DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
				props.put(DME2Constants.REPLY_EVENT, true);
				
				if (this.uri.getPartner() != null)
				{
					props.put(DME2Constants.DME2_REQUEST_PARTNER, this.uri.getPartner());
				}

				logger.debug( null, "onWebSocketBinary", "DME2Exchange postWSReceiveEvent ", props);
				dme2Mgr.postStatEvent( props );
			}
			
			try {
				handler.processBinaryMessage(data, offset, length);	
			} catch (Exception e) {
				logger.warn( null, "onWebSocketBinary", LogMessage.WS_CLI_HANDLER_EXCEPTION, this.uri, this.endpoint, connection.getTrackingId(), e.getMessage());
			}
		}
	}
	
	@Override
	public void onWebSocketException(WebSocketException error) {
		// TODO Auto-generated method stub
		
	}

	public DME2WSCliConnection getConnection() {
		return this.connection;
	}


	public void setConnection(DME2WSCliConnection connection) {
		this.connection = connection;
	}

	public WebSocketClient getWsClient() {
		return this.wsClient;
	}

	public void setWsClient(WebSocketClient wsClient) {
		this.wsClient = wsClient;
	}

	public DmeUniformResource getUri() {
		return uri;
	}

	public void setUri(DmeUniformResource uri) {
		this.uri = uri;
	}

	public DME2WSCliMessageHandler getHandler() {
		return this.handler;
	}

	public void setHandler(DME2WSCliMessageHandler handler) {
		this.handler = handler;
	}

	public DME2Manager getDme2Mgr() {
		return this.dme2Mgr;
	}

	public String getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public int getMaxConnectionIdleTime() {
		return this.maxConnectionIdleTime;
	}

	public void setMaxConnectionIdleTime(int maxConnectionIdleTime) {
		this.maxConnectionIdleTime = maxConnectionIdleTime;
	}

	public void setTrackingId(String trackingID) {
		this.trackingID = trackingID;		
	}

	
	public String getTrackingID() {
		return trackingID;
	}

	public StringBuffer getEpTraceRoute() {
		return this.epTraceRoute;
	}

	public DME2WSCliConnManager getWsConnMgr() {
		return this.wsConnMgr;
	}

	public void setWsConnMgr(DME2WSCliConnManager wsConnMgr) {
		this.wsConnMgr = wsConnMgr;		
		this.logStats = wsConnMgr.isLogStats();
	}

	public boolean isLogStats() {
		return logStats;
	}

	public void setLogStats(boolean logStats) {
		this.logStats = logStats;
	}
}