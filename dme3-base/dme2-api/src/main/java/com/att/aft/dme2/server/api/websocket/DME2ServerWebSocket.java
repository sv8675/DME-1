package com.att.aft.dme2.server.api.websocket;

import java.util.HashMap;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jetty.websocket.core.api.WebSocketConnection;
import org.eclipse.jetty.websocket.core.api.WebSocketException;
import org.eclipse.jetty.websocket.core.api.WebSocketListener;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2ServerWebSocket implements WebSocketListener { //, WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

	DME2ServerWebSocketHandler handler;
	DME2Manager dme2Manager;
	DME2ServerWSConnection dme2ServerWSConnection;
	private static final String CLASS_NAME = "com.att.aft.dme2.server.api.websocket.DME2ServerWebSocket";
	private static Logger logger = LoggerFactory.getLogger( DME2ServerWebSocket.class );
	String trackingId;
	private DME2Configuration config = new DME2Configuration();
	
	public DME2ServerWebSocket() {
		// TODO Auto-generated constructor stub
	}

	public DME2ServerWebSocket(DME2ServerWebSocketHandler dme2ServerWebSocketHandler, DME2Manager dme2Manager,String trackingId)  throws 	 DME2Exception {
		try{
			this.handler =(DME2ServerWebSocketHandler)dme2ServerWebSocketHandler.clone();
		}catch(Exception e){
			ErrorContext ec = new ErrorContext();
			ec.add("Code", "DME2Server.Fault");
			ec.add("extendedMessage", e.getMessage());
			ec.add("StackTrace", ExceptionUtils.getStackTrace(e));
			logger.info( null, "ctor", LogMessage.WS_SERVER_WEBSOCKET_HANDLER_INSTANTIATION_EXCEPTION, ec );
			throw new DME2Exception(LogMessage.WS_SERVER_WEBSOCKET_HANDLER_INSTANTIATION_EXCEPTION.getTemplate(),ec);
		}

		this.dme2Manager =dme2Manager;
		this.trackingId=trackingId;
	}

	public void setDME2Handler(DME2ServerWebSocketHandler handler) {
		this.handler = handler;
	}


	public void onWebSocketConnect(WebSocketConnection connection) {

	
		Integer maxIdleTime=config.getInt("AFT_DME2_SERVER_WEBSOCKET_CONNECTION_MAX_IDLETIME");
		Integer maxTxtMsgSize=config.getInt("AFT_DME2_SERVER_WEBSOCKET_MAX_TEXT_MESSAGE_SIZE");
		Integer maxBinMsgSize=config.getInt("AFT_DME2_SERVER_WEBSOCKET_MAX_BINARY_MESSAGE_SIZE");
		if(connection!=null ){
			if(maxIdleTime!=0){
				connection.getPolicy().setIdleTimeout(maxIdleTime);	
			}
			connection.getPolicy().setMaxTextMessageSize(maxTxtMsgSize);//32 kb
			connection.getPolicy().setMaxBinaryMessageSize(maxBinMsgSize);//32 kb
		}
		DME2Constants.setContext(this.getTrackingId(), null);
	

	try{
		DME2ServerWSConnection dme2ServerWSConnection1=new DME2ServerWSConnection(connection);
		dme2ServerWSConnection1.setTrackingId( this.getTrackingId() );
	    this.setDme2ServerWSConnection( dme2ServerWSConnection1 );
	    
	    ErrorContext errC = new ErrorContext();
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_SERVICE_NAME, this.handler.getDme2ServiceName());
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_PORT, String.valueOf(
          this.dme2Manager.getServer().getServerProperties().getPort() ));
	    errC.add( DME2Constants.AFT_DME2_SERVER_WEBSOCKET_TRACKING_ID, dme2ServerWSConnection1.getTrackingId() );
	    errC.add("AFT_DME2_SERVER_WEBSOCKET_CONNECTION_MAXIDLETIME", (maxIdleTime==0)?"DEFAULT": String.valueOf(maxIdleTime));
	    errC.add("AFT_DME2_SERVER_WEBSOCKET_MAX_TEXT_MESSAGE_SIZE",String.valueOf(maxTxtMsgSize));
	    errC.add("AFT_DME2_SERVER_WEBSOCKET_MAX_BINARY_MESSAGE_SIZE",String.valueOf(maxBinMsgSize));

	    logger.info( null, "onWebSocketConnect",   LogMessage.WS_SERVER_CONNECTION_OPEN_MSG,errC);
	    handler.setDme2wsConnection( dme2ServerWSConnection1 );
	}catch(Exception e){
		
		ErrorContext ec = new ErrorContext();
		ec.add("Code", "DME2Server.Fault");
		ec.add("extendedMessage", e.getMessage());
		ec.add("StackTrace", ExceptionUtils.getStackTrace(e));
		logger.info( null, "onWebSocketConnect",   LogMessage.WS_SERVER_WEBSOCKET_ON_OPEN_EXCEPTION,ec);

	 }
	}

	

	public void onMessage(String data) {
		
	
		DME2Constants.setContext(this.getTrackingId(), null);
	    ErrorContext errC = new ErrorContext();
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_SERVICE_NAME, this.handler.getDme2ServiceName());
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_PORT, String.valueOf(
          this.dme2Manager.getServer().getServerProperties().getPort() ));
	    errC.add( DME2Constants.AFT_DME2_SERVER_WEBSOCKET_TRACKING_ID, dme2ServerWSConnection.getTrackingId() );

	}

	@Override
	public void onWebSocketBinary(byte[] data, int offset, int length) {

		DME2Constants.setContext(this.getTrackingId(), null);
	    ErrorContext errC = new ErrorContext();
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_SERVICE_NAME, this.handler.getDme2ServiceName());
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_PORT, String.valueOf(
          this.dme2Manager.getServer().getServerProperties().getPort() ));
	    errC.add( DME2Constants.AFT_DME2_SERVER_WEBSOCKET_TRACKING_ID, dme2ServerWSConnection.getTrackingId() );

	    logger.info( null, "onWebSocketBinary",   LogMessage.WS_SERVER_CONNECTION_SEND_MSG,errC);
		
		if(config.getBoolean(DME2Constants.AFT_DME2_WEBSOCKET_METRICS_COLLECTION)){
			HashMap<String, Object> props = new HashMap<String, Object>();
			props.put(DME2Constants.MSG_SIZE, (data != null) ? data.length : 0);
			props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
			props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,
					DME2Constants.DME2_INTERFACE_WEBSOCKET_PROTOCOL);
			props.put(DME2Constants.DME2_INTERFACE_ROLE,
					config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
			props.put( DME2Constants.DME2_INTERFACE_PORT, this.dme2Manager.getServer().getServerProperties().getPort() );
			props.put(DME2Constants.DME2_WEBSOCKET_SERVICE_NAME, this.handler.getDme2ServiceName());
			props.put(DME2Constants.FAULT_EVENT, false);
			props.put(DME2Constants.ELAPSED_TIME, 0);
			try {
				this.dme2Manager.postStatEvent( props );

			} catch (Exception e) {
				
				ErrorContext ec = new ErrorContext();
				ec.add("Code", "DME2Server.Fault");
				ec.add("extendedMessage", e.getMessage());
				ec.add("StackTrace", ExceptionUtils.getStackTrace(e));

				logger.info( null, "onWebSocketBinary",   LogMessage.WS_SERVER_WEBSOCKET_METRICS_COLLECTION_EXCEPTION,ec);
			}
		
		}
		
		
		try {
			handler.onMessage( data, offset, length);

		} catch (Exception e) {
			
			ErrorContext ec = new ErrorContext();
			ec.add("Code", "DME2Server.Fault");
			ec.add("extendedMessage", e.getMessage());


			logger.info( null, "onWebSocketBinary",   LogMessage.WS_SERVER_WEBSOCKET_ON_MESSAGE_BINARY_EXCEPTION,ec);
		}
	}
	public void onClose(int closeCode, String message) {
		
		DME2Constants.setContext(this.getTrackingId(), null);
	    ErrorContext errC = new ErrorContext();
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_SERVICE_NAME, this.handler.getDme2ServiceName());
	    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_PORT, String.valueOf(
          this.dme2Manager.getServer().getServerProperties().getPort() ));
	    errC.add( DME2Constants.AFT_DME2_SERVER_WEBSOCKET_TRACKING_ID, dme2ServerWSConnection.getTrackingId() );

	    logger.info( null, "onClose",   LogMessage.WS_SERVER_CONNECTION_CLOSE_MSG,errC);
		
		
		try{
			handler.onClose(closeCode, message);
		}catch(Exception e){
			
			ErrorContext ec = new ErrorContext();
			ec.add("Code", "DME2Server.Fault");
			ec.add("extendedMessage", e.getMessage());
			ec.add("StackTrace", ExceptionUtils.getStackTrace(e));
			logger.info( null, "onClose",   LogMessage.WS_SERVER_WEBSOCKET_ON_CLOSE_EXCEPTION,ec);
		}
	
	}

	  
	  		
	  public DME2Manager getDme2Manager() {
			return dme2Manager;
	  }

	  public void setDme2Manager( DME2Manager dme2Manager ) {
			this.dme2Manager = dme2Manager;
	  }
	  
		public DME2ServerWSConnection getDme2ServerWSConnection() {
			return dme2ServerWSConnection;
		}

		public void setDme2ServerWSConnection(
        DME2ServerWSConnection dme2ServerWSConnection ) {
			this.dme2ServerWSConnection = dme2ServerWSConnection;
		}

		public String getTrackingId() {
			return trackingId;
		}

		public void setTrackingId(String trackingId) {
			this.trackingId = trackingId;
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onWebSocketException(WebSocketException error) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onWebSocketText(String data) {						
			DME2Constants.setContext(this.getTrackingId(), null);
		    ErrorContext errC = new ErrorContext();
		    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_SERVICE_NAME, this.handler.getDme2ServiceName());
		    errC.add(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_SERVER_PORT, String.valueOf(
	          this.dme2Manager.getServer().getServerProperties().getPort() ));
		    errC.add( DME2Constants.AFT_DME2_SERVER_WEBSOCKET_TRACKING_ID, dme2ServerWSConnection.getTrackingId() );

		    logger.info( null, "onMessage",   LogMessage.WS_SERVER_CONNECTION_SEND_MSG,errC);
			
			if(config.getBoolean(DME2Constants.AFT_DME2_WEBSOCKET_METRICS_COLLECTION)){
				//use dme2constant to check
				HashMap<String, Object> props = new HashMap<String, Object>();
				props.put(DME2Constants.MSG_SIZE, (data != null) ? data.length() : 0);
				props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
				props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,
						DME2Constants.DME2_INTERFACE_WEBSOCKET_PROTOCOL);
				props.put(DME2Constants.DME2_INTERFACE_ROLE,
						config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
				props.put( DME2Constants.DME2_INTERFACE_PORT, this.dme2Manager.getServer().getServerProperties().getPort() );
				props.put(DME2Constants.DME2_WEBSOCKET_SERVICE_NAME, this.handler.getDme2ServiceName());
				props.put(DME2Constants.FAULT_EVENT, false);
				props.put(DME2Constants.ELAPSED_TIME, 0);

				try {
					this.dme2Manager.postStatEvent( props );
					//this._connection.sendMessage("return: "+data);
				} catch (Exception e) {
					ErrorContext ec = new ErrorContext();
					ec.add("Code", "DME2Server.Fault");
					ec.add("extendedMessage", e.getMessage());
					ec.add("StackTrace", ExceptionUtils.getStackTrace(e));
					logger.info( null, "onMessage",   LogMessage.WS_SERVER_WEBSOCKET_METRICS_COLLECTION_EXCEPTION,ec);
				}
			
			}

			try {
			
				handler.onMessage( data);

			} catch (Exception e) {
				ErrorContext ec = new ErrorContext();
				ec.add("Code", "DME2Server.Fault");
				ec.add("extendedMessage", e.getMessage());
				ec.add("StackTrace", ExceptionUtils.getStackTrace(e));

				logger.info( null, "onMessage",   LogMessage.WS_SERVER_WEBSOCKET_HANDLER_EXCEPTION,ec);
			}
		
					
		}

}
