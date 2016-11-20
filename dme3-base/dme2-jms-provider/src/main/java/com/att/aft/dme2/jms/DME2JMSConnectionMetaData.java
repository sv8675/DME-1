/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

public class DME2JMSConnectionMetaData implements ConnectionMetaData {

	private static final String PROVIDER = "AT&T";
	private static final int JMS_VERSION_MAJOR = 1;
	private static final int JMS_VERSION_MINOR = 0;
	private static final int PROVIDER_VERSION_MAJOR = 1;
	private static final int PROVIDER_VERSION_MINOR = 0;	
	private final Enumeration<?> JMX_PROPS;
	
	private static DME2JMSConnectionMetaData metaData = new DME2JMSConnectionMetaData();
	
	DME2JMSConnectionMetaData() {
		StringTokenizer tokenizer = new StringTokenizer("");
		JMX_PROPS = tokenizer;
	}
	
	@Override
	public int getJMSMajorVersion() throws JMSException {
		return JMS_VERSION_MAJOR;
	}

	@Override
	public int getJMSMinorVersion() throws JMSException {
		return JMS_VERSION_MINOR;
	}

	@Override
	public String getJMSProviderName() throws JMSException {
		return PROVIDER;
	}

	@Override
	public String getJMSVersion() throws JMSException {
		return JMS_VERSION_MAJOR + "." + JMS_VERSION_MINOR;
	}

	@Override
	public Enumeration<?> getJMSXPropertyNames() throws JMSException {
		return JMX_PROPS;
	}

	@Override
	public int getProviderMajorVersion() throws JMSException {
		return PROVIDER_VERSION_MAJOR;
	}

	@Override
	public int getProviderMinorVersion() throws JMSException {
		return PROVIDER_VERSION_MINOR;
	}

	@Override
	public String getProviderVersion() throws JMSException {
		return PROVIDER_VERSION_MAJOR + "." + PROVIDER_VERSION_MINOR;
	}

	static DME2JMSConnectionMetaData getMETA_DATA() {
		return metaData;
	}

	static void setMETA_DATA(DME2JMSConnectionMetaData mETADATA) {
		metaData = mETADATA;
	}

}
