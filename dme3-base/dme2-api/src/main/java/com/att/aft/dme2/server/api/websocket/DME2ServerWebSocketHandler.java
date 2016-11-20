package com.att.aft.dme2.server.api.websocket;

import java.io.IOException;
import java.io.Serializable;


public abstract class DME2ServerWebSocketHandler implements Serializable,Cloneable {

	private DME2ServerWSConnection dme2wsConnection;
	
	
	private String dme2ServiceName;

	
	public String getDme2ServiceName() {
		return dme2ServiceName;
	}
	public void setDme2ServiceName(String dme2ServiceName) {
		this.dme2ServiceName = dme2ServiceName;
	}
	public DME2ServerWSConnection getDme2wsConnection() {
		return dme2wsConnection;
	}
	public void setDme2wsConnection( DME2ServerWSConnection dme2wsConnection ) {
		this.dme2wsConnection = dme2wsConnection;
	}
	public abstract void onMessage(String data)throws IOException;
	
	public abstract void onMessage(byte[] data, int offset, int length) throws IOException;
	public abstract void onClose(int closeCode,String message);
	public abstract void onOpen(DME2ServerWSConnection  dme2wsConnection);
	
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return (DME2ServerWebSocketHandler)super.clone();
	}
}
