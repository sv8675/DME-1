/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class TestInitParamFilter implements Filter{
	FilterConfig config;
	String serviceName;
	public TestInitParamFilter(String serviceName) {
		this.serviceName = serviceName;
	}
	
	@Override
	public void destroy() {
	
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
	
		chain.doFilter(request, response);
		System.out.println("Filter invoked");
		response.getWriter().println("filterParam=" + this.config.getInitParameter("testFilterParam"));
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		this.config = arg0;
	}
	
}
