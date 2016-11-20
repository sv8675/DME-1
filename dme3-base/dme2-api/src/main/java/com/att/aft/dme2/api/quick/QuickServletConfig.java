/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.quick;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class QuickServletConfig implements ServletConfig {
	private Properties parameters = new Properties();
	private String name = null;
	private ServletContext context =null;
	public QuickServletConfig(String name, ServletContext context, Properties props) {
		this.name = name;
		this.context = context;
		parameters = props;
	}

	@Override
	public String getInitParameter(String arg0) {
		return parameters.getProperty(arg0);
	}

	@Override
	public Enumeration getInitParameterNames() {
		return parameters.keys();
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getServletName() {
		return name;
	}

}
