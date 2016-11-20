package com.att.aft.dme2.request;


import java.io.InputStream;


/**
 * 
 */
public class DME2StreamPayload extends DME2Payload {

	private InputStream in;

	public DME2StreamPayload(InputStream in) {
		this.in = in;
		this.setPayloadInMemory(false);
	}

	public InputStream getPayload()	{
		return this.in;
	}
	
}
