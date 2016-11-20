/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.ServletHolder;

public class DME2ServletHolder{
	private ServletHolder servletHolder;
	private String urlPattern[];
	private String contextPath;
	private Properties initParams = new Properties();
	
	
	public DME2ServletHolder(Servlet s, String[] newUrlMappingPattern) {
		servletHolder = new ServletHolder(s);
		 if(newUrlMappingPattern == null) { 
			 this.urlPattern = null; 
		 }else { 
			 this.urlPattern = Arrays.copyOf(newUrlMappingPattern, newUrlMappingPattern.length); 
		  } 
	}
	
	public DME2ServletHolder(Servlet s) {
		servletHolder = new ServletHolder(s);
	}
	
	public String[] getURLMapping() {
		return this.urlPattern;
	}
	
	public ServletHolder getServletHolder() {
		return this.servletHolder;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getContextPath() {
		return contextPath;
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
			this.servletHolder.setInitParameter(key, value);
		}
	}
}
