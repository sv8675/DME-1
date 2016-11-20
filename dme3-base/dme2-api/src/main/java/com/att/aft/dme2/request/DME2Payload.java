package com.att.aft.dme2.request;


import java.util.Properties;


/**
 * Base class for setting dme2Payload
 */
public abstract class DME2Payload {	
	private String contentType;
	private Properties props = new Properties();
	private boolean isPayloadInMemory = true;
	
	public static enum DmePayloadType {
		TEXT_PAYLOAD,
		FILE_PAYLOAD,
		STREAM_PAYLOAD,
		BINARY_PAYLOAD;
	}

	public DME2Payload() {}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Properties getProperties() {
		return props;
	}

	public void setProperty(String key, String value) {
		this.props.setProperty(key, value);
	}

	public boolean isPayloadInMemory() {
		return isPayloadInMemory;
	}

	public void setPayloadInMemory(boolean isPayloadInMemory) {
		this.isPayloadInMemory = isPayloadInMemory;
	}

}
