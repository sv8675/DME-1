/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FailoverServlet extends HttpServlet 
{

	private static final long serialVersionUID = 1L;

	/** The server id. */
	private String serverId = null;

	/** The service. */
	private String service = null;

	/**
	 * Instantiates a new echo servlet.
	 * 
	 * @param service
	 *            the service
	 * @param serverId
	 *            the server id
	 */
	public FailoverServlet(String service, String serverId) {
		this.service = service;
		this.serverId = serverId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		// set as a header so it can be checked on the response for testing purposes
		String charset = req.getCharacterEncoding();
		resp.setHeader("com.att.aft.dme2.test.charset", charset);	
		
		if(serverId.equals("Fail_500"))
		{
			resp.sendError(500);
			resp.flushBuffer();
		}
		else
		{
			resp.sendError(503);
			resp.flushBuffer();
		}
		
		return;
	}

}