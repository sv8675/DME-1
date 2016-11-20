/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.util.DME2ServletResponseWrapper;


public class TestDME2LogFilter implements Filter{

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		// TODO Auto-generated method stub
	    HttpServletRequest req = null;
	    String id = "1";

	    DME2ServletResponseWrapper wrapper = new DME2ServletResponseWrapper(
			    (HttpServletResponse)response);
		PrintWriter out = wrapper.getWriter();
	    chain.doFilter(request, wrapper);
		out.write("TestDME2LogFilter::"+wrapper.toString());
		out.flush();
		out.close();
		
	    
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
