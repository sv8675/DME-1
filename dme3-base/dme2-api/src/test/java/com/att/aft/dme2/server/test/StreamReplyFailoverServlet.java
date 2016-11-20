/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StreamReplyFailoverServlet extends HttpServlet
{

	private static final long serialVersionUID = 6706705657265492466L;
	private String routeOffer;
	
	public StreamReplyFailoverServlet(String routeOffer)
	{
		this.routeOffer = routeOffer;
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		
			if(routeOffer.equals("FAIL"))
			{
				resp.setStatus(503);
				resp.sendError(503);
			}
			else
			{
				resp.getOutputStream().write(("SUCCESSFUL REQUEST").getBytes());
			}

	}

}
