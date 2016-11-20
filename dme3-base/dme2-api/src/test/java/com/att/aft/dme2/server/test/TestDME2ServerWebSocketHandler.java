/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;


import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.att.aft.dme2.server.api.websocket.DME2ServerWSConnection;
import com.att.aft.dme2.server.api.websocket.DME2ServerWebSocketHandler;

public class TestDME2ServerWebSocketHandler  extends DME2ServerWebSocketHandler{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void onOpen(DME2ServerWSConnection dme2wsConnection) {
	
	    System.out.println("Connection opened : {}" + dme2wsConnection);
	       		
	}
	
	@Override
	public void onMessage( String data) {
		// TODO Auto-generated method stub
		System.out.println("I am in onMessage() in handler");
        try
        {
            // echo back this TEXT message
        	this.getDme2wsConnection().sendMessage("I am fine. What can I do for you?");
//            this.connection.close(1011, "Unexpected Error");
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
        	
        }
		
	}
	
	@Override
	public void onClose(int closeCode,
			String message) {
	       System.out.println("Closed {} : {}" + closeCode + message);         
	}

	@Override
	public void onMessage(byte[] data, int offset, int length) {
        // Wrap in Jetty Buffer (only for logging reasons)
		
		Buffer buf =  ByteBuffer.wrap(data, offset, length);
        //Buffer buf = new ByteBuffer(data,offset,length);
        System.out.println("on Text : {}" + buf.toString());
        try {
        	String msg = "I am fine. What can I do for you?";
        	this.getDme2wsConnection().sendMessage(msg.getBytes(),0, msg.length());
         } catch (IOException e) {
           e.printStackTrace();
        }		
	}

	
}