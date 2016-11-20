package com.att.aft.dme2.server.api.websocket;

import java.io.IOException;

import org.eclipse.jetty.websocket.core.api.WebSocketConnection;

public class DME2ServerWSConnection {
 	private WebSocketConnection connection;
 	private String trackingId;
	


	public DME2ServerWSConnection(WebSocketConnection  connection) {
		// TODO Auto-generated constructor stub
		this.connection=connection;
	}

	protected WebSocketConnection getConnection() {
		return connection;
	}

	protected void setConnection(WebSocketConnection connection) {
		this.connection = connection;
	}
	

	public void sendMessage(String data) throws IOException{
		//TODO
		this.getConnection().write(null, null, data);		 
		//this.getConnection().sendMessage(data);
	 }
	
	public  void sendMessage(byte[] data, int offset, int length) throws IOException{
		//TODO
		this.getConnection().write(null, null, data, offset, length);		 
    	 //this.getConnection().sendMessage(data, offset, length); 
     }
	
	public  boolean isOpen(){
		  
		 if(this.connection!=null){
			 return this.connection.isOpen();
		 }
		 return false;
	  }

      /**
       * @param ms The time in ms that the connection can be idle before closing
       */
	public  void setMaxIdleTime(int ms){
    	  
		 if(this.connection!=null){
			 this.connection.getPolicy().setIdleTimeout(ms);
		 }
      }
      
      /**
       * @param size size<0 No aggregation of frames to messages, >=0 max size of text frame aggregation buffer in characters
       */
	public void setMaxTextMessageSize(int size){
		 if(this.connection!=null){
			 this.connection.getPolicy().setMaxTextMessageSize(size);
		 }
      };
      
      /**
       * @param size size<0 no aggregation of binary frames, >=0 size of binary frame aggregation buffer
       */
      public void setMaxBinaryMessageSize(int size){
    	  
    	  if(this.connection!=null){
    		  this.connection.getPolicy().setMaxBinaryMessageSize(size);
    	  }
      };
      
      /**
       * @return The time in ms that the connection can be idle before closing
       */
      public  int getMaxIdleTime(){
    	  
    	  if(this.connection!=null){
    		  return this.connection.getPolicy().getIdleTimeout();
    	  }
    	  return 0;
      };
      
      /**
       * Size in characters of the maximum text message to be received
       * @return size <0 No aggregation of frames to messages, >=0 max size of text frame aggregation buffer in characters
       */
      public  int getMaxTextMessageSize(){
    	  if(this.connection!=null){
    		  return this.connection.getPolicy().getMaxTextMessageSize();
    	  }
    	  return 0;
      }
      
      /**
       * Size in bytes of the maximum binary message to be received
       * @return size <0 no aggregation of binary frames, >=0 size of binary frame aggregation buffer
       */
      public  int getMaxBinaryMessageSize(){
    	  
    	  if(this.connection!=null){
    		  return this.connection.getPolicy().getMaxBinaryMessageSize();
    	  }
    	  return 0;
      }
      
      public String getTrackingId() {
    		return trackingId;
	  }

      public void setTrackingId(String trackingId) {
    		this.trackingId = trackingId;
      }
      
    
	
}
