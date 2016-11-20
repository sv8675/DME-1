/*
 * Copyright 2011, 2016 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.api.util;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * The Class DME2NullServlet.
 */
public class DME2NullServlet implements Servlet {

	private static final Logger logger = LoggerFactory.getLogger( DME2NullServlet.class );
	
	/** The service. */
	private String service = null;
	private ServletConfig config = null;

	/**
	 * Instantiates a new e http null servlet.
	 * 
	 * @param service
	 *            the service
	 */
	public DME2NullServlet(String service) {
		this.service = service;
	}
	
	public DME2NullServlet() {
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#destroy()
	 */
	@Override
	public void destroy() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#getServletConfig()
	 */
	@Override
	public ServletConfig getServletConfig() {
		return config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#getServletInfo()
	 */
	@Override
	public String getServletInfo() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
		String s2 = config.getInitParameter("AFT_DME2_SERVICE");
		if (s2 == null && service == null) 
		{
			logger.warn( null, "init", LogMessage.SERVLET_PARAM_MISSING, "AFT_DME2_SERVICE");
		}
		service = s2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 * javax.servlet.ServletResponse)
	 */
	@Override
	public void service(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		logger.debug( null, "service", LogMessage.SERVLET_RECV, request.getRemoteAddr());
		response.getWriter().println(
				"<html><title>" + service + "</title><body><h3>Service: "
						+ service + "</h3><br/><h3>Timestamp: "
						+ System.currentTimeMillis() + "</h3></body></html>");
		response.flushBuffer();
		return;
	}

}
