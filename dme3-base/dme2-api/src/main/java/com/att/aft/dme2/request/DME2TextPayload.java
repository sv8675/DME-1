package com.att.aft.dme2.request;

/**
 * 
 */
public class DME2TextPayload extends DME2Payload {

	private String payload;
	public DME2TextPayload(String payload) {
		this.payload = payload;
	}

	public DME2TextPayload(String payload, String contentType) {
		this.payload = payload;
		this.setContentType(contentType);
	}
	
	public String getPayload(){
		return this.payload;
	}
}

