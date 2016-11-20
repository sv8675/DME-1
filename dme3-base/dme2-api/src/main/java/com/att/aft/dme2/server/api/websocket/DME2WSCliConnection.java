package com.att.aft.dme2.server.api.websocket;

import java.util.HashMap;

import org.eclipse.jetty.websocket.core.api.WebSocketConnection;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/**
 * Wrapper class around the Jetty websocket connection. Provides methods to the
 * client to send messages and close connection explicitly
 * 
 * 
 */
public class DME2WSCliConnection {
	
	private static Logger logger = LoggerFactory.getLogger( DME2WSCliConnection.class );

	private String trackingId;
	private WebSocketConnection connection = null;
	private long connectStartTime;
	private long connectEndTime;
	private DmeUniformResource uri;
	private DME2Manager dme2Mgr;
	private String endpoint;
	private DME2WSCliConnManager wsConnMgr = null;
	private DME2Configuration config = new DME2Configuration();
	
	public DME2WSCliConnection(WebSocketConnection conn, DME2Manager mgr,
			DME2WSCliConnManager wsConnMgr) {
		connection = conn;
		connectStartTime = System.currentTimeMillis();
		dme2Mgr = mgr;
		this.wsConnMgr = wsConnMgr;
	}

	public void sendMessage(String message) throws DME2Exception {
		try {
			if ((connection != null) && (connection.isOpen())) {
				logger.info( null, "sendMessage", LogMessage.WS_CONNECTION_SEND_MSG, trackingId, uri, endpoint, message.length());

				if (wsConnMgr.isLogStats()) {
					 //Post request statistics to Metrics Service 
					HashMap<String, Object> props = new HashMap<String, Object>();
					props.put(DME2Constants.EVENT_TIME,
							System.currentTimeMillis());
					props.put(DME2Constants.FAULT_EVENT, false);
					props.put(DME2Constants.ELAPSED_TIME, 0);
					props.put(DME2Constants.DME2_WS_CONNECT_ID, trackingId);
					props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,
							DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
					props.put(DME2Constants.DME2_INTERFACE_ROLE,
							config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
					props.put(DME2Constants.DME2_INTERFACE_PORT, uri.getUrl()
							.getPort() + "");

					if (this.uri.getPartner() != null) {
						props.put(DME2Constants.DME2_REQUEST_PARTNER,
								this.uri.getPartner());
					}

					logger.debug( null, "sendMessage", "DME2Exchange postWSSendEvent {}",
							props);
					dme2Mgr.postStatEvent( props );
				}
				synchronized(wsConnMgr.getLock()) {	
					connection.write(null, null, message);
					//connection.sendMessage(message);
				}
			} else {
				ErrorContext ec = new ErrorContext();
				ec.add("WS_TRACKING_ID", trackingId);
				ec.add("URI", uri.toString());
				ec.add("ENDPOINT", endpoint);
				ec.add("MESSAGE_LEN", String.valueOf(message.length()));
				DME2Exception ex = new DME2Exception("AFT-DME2-3004", ec);
				throw ex;
			}
		} catch (DME2Exception e) { 
			throw e;
		} catch (Exception e) {
			ErrorContext ec = new ErrorContext();
			ec.add("WS_TRACKING_ID", this.getTrackingId());
			ec.add("URI", uri.toString());
			ec.add("ENDPOINT", endpoint);
			ec.add("MESSAGE_LEN", String.valueOf(message.length()));
			ec.add("ERROR_MESSAGE", e.getMessage());

			DME2Exception ex = new DME2Exception("AFT-DME2-3005", ec, e);
			logger.error( null, "sendMessage", "AFT-DME2-3005", ec, ex);
			throw ex;
		}
		
	}

	public void sendMessage(byte[] data, int offset, int length)
			throws DME2Exception {
		try {
			if ((connection != null) && (connection.isOpen())) {
				logger.info( null, "sendMessage(byte[], int, int)", LogMessage.WS_CONNECTION_SEND_MSG, trackingId, uri, endpoint, data.length);

        if (wsConnMgr.isLogStats()) {
					//Post request statistics to Metrics Service 
					HashMap<String, Object> props = new HashMap<String, Object>();
					props.put(DME2Constants.EVENT_TIME,
							System.currentTimeMillis());
					props.put(DME2Constants.FAULT_EVENT, false);
					props.put(DME2Constants.ELAPSED_TIME, 0);
					props.put(DME2Constants.DME2_WS_CONNECT_ID, trackingId);
					props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,
							DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
					props.put(DME2Constants.DME2_INTERFACE_ROLE,
							config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
					props.put(DME2Constants.DME2_INTERFACE_PORT, uri.getUrl()
							.getPort() + "");

					if (this.uri.getPartner() != null) {
						props.put(DME2Constants.DME2_REQUEST_PARTNER,
								this.uri.getPartner());
					}

					logger.debug( null, "sendMessage", "DME2Exchange postWSSendEvent {}",
							props);
					dme2Mgr.postStatEvent( props );
				}
				synchronized(wsConnMgr.getLock()) {	
					connection.write(null, null, data, offset, length);
				}
			} else {
				ErrorContext ec = new ErrorContext();
				ec.add("WS_TRACKING_ID", trackingId);
				ec.add("URI", uri.toString());
				ec.add("ENDPOINT", endpoint);
				ec.add("MESSAGE_LEN", String.valueOf(data.toString().length()));

				DME2Exception ex = new DME2Exception("AFT-DME2-3004", ec);
				logger.error( null, "sendMessage(byte[], int, int)", "AFT-DME2-3004 {}", ec, ex);
				throw ex;
			}
		} catch (DME2Exception e) { 
			throw e;
		} catch (Exception e) {
			ErrorContext ec = new ErrorContext();
			ec.add("WS_CONNECT_ID", trackingId);
			ec.add("URI", uri.toString());
			ec.add("ENDPOINT", endpoint);
			ec.add("MESSAGE", data.toString());
			ec.add("ERROR_MESSAGE", e.getMessage());

			DME2Exception ex = new DME2Exception("AFT-DME2-3005", ec, e);
			logger.error( null, "sendMessage(byte[], int, int)", "AFT-DME2-3005 {}", ec, ex );
			throw ex;
		}		
	}

	public void close() throws DME2Exception {
		synchronized(wsConnMgr.getLock()) {	
			try {
				if ((connection != null) && (connection.isOpen())) {
					connectEndTime = System.currentTimeMillis();
					wsConnMgr.setUserClose(true);
					connection.close();
				}
			} catch (Exception e) {
				ErrorContext ec = new ErrorContext();
				ec.add("WS_CONNECT_ID", trackingId);
				ec.add("URI", uri.toString());
				ec.add("ENDPOINT", endpoint);
				ec.add("ERROR_MESSAGE", e.getMessage());
	
				DME2Exception ex = new DME2Exception("AFT-DME2-3006", ec, e);
				logger.error( null, "close", "AFT-DME2-3006 {}", ec, ex);
				throw ex;
			}
		}
	}

	public boolean isOpen() {
		synchronized(wsConnMgr.getLock()) {
			if (connection != null)
				return connection.isOpen();
		}
		return false;
	}

	public void setMaxIdleTime(int ms) {
		if (connection != null)
			this.connection.getPolicy().setIdleTimeout(ms);
	}

	public void setMaxTextMessageSize(int size) {
		if (connection != null)
			connection.getPolicy().setMaxTextMessageSize(size);
	}

	public void setMaxBinaryMessageSize(int size) {
		if (connection != null)
			connection.getPolicy().setMaxBinaryMessageSize(size);

	}

	public int getMaxIdleTime() {
		if (connection != null)
			return connection.getPolicy().getIdleTimeout();
		return 0;
	}

	public int getMaxTextMessageSize() {
		if (connection != null)
			return connection.getPolicy().getMaxTextMessageSize();
		return 0;
	}

	public int getMaxBinaryMessageSize() {
		if (connection != null)
			return connection.getPolicy().getMaxBinaryMessageSize();
		return 0;
	}

	public long getConnectStartTime() {
		return connectStartTime;
	}

	public long getConnectEndTime() {
		return connectEndTime;
	}

	public void setConnectEndTime(long connectEndTime) {
		this.connectEndTime = connectEndTime;
	}

	public DmeUniformResource getUri() {
		return uri;
	}

	public void setUri(DmeUniformResource uri) {
		this.uri = uri;
	}

	public String getTrackingId() {
		return trackingId;
	}

	public void setTrackingId(String trackingId) {
		this.trackingId = trackingId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public DME2WSCliConnManager getWsConnMgr() {
		return wsConnMgr;
	}

	public WebSocketConnection getConnection() {		
		return connection;
	}

	public void setConnection(WebSocketConnection connection) {
		synchronized(wsConnMgr.getLock()) {		
			this.connection = connection;
		}
	}

}
