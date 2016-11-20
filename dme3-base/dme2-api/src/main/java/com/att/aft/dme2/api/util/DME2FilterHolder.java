/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.eclipse.jetty.servlet.FilterHolder;

public class DME2FilterHolder {
	private FilterHolder fh;
	private String filterPattern;
	private Properties initParams = new Properties();
	public enum RequestDispatcherType {
		ASYNC, ERROR, FORWARD, INCLUDE, REQUEST
	}

	private Collection<DispatcherType> jettyDispatcherTypes = new ArrayList<DispatcherType>();

	public DME2FilterHolder(Filter filter, String filterPattern,
			EnumSet<RequestDispatcherType> dt) {
		this.fh = new FilterHolder(filter);
		this.filterPattern = filterPattern;
		for (RequestDispatcherType dispatchType : dt) {
			DispatcherType jettyDispatcherType = DispatcherType
					.valueOf(dispatchType.name());
			this.jettyDispatcherTypes.add(jettyDispatcherType);
		}
	}

	public FilterHolder getFilterHolder() {
		return fh;
	}

	public String getFilterPattern() {
		return this.filterPattern;
	}

	public EnumSet<DispatcherType> getDispatcherType() {
		return EnumSet.copyOf(jettyDispatcherTypes);
	}

	public Properties getInitParams() {
		return initParams;
	}

	public void setInitParams(Properties initParams) {
		this.initParams = initParams;
		Enumeration<Object> keys = this.initParams.keys();
		while(keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			String value = this.initParams.getProperty(key);
			this.fh.setInitParameter(key, value);
		}
	}
}
