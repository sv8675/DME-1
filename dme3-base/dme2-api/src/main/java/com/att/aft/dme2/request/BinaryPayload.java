package com.att.aft.dme2.request;

import java.util.Arrays;

public class BinaryPayload extends DME2Payload {
	private byte[] bytes;
	
	public BinaryPayload(byte[] newBytes) {
		if(newBytes == null) { 
			this.bytes = null; 
		} else { 
			this.bytes = Arrays.copyOf(newBytes, newBytes.length); 
		} 
	}
	
	public byte[] getPayload() {
		return bytes;
	}
}
