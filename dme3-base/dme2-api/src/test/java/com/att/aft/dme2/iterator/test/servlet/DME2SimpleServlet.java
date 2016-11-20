/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.servlet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DME2SimpleServlet extends HttpServlet
{

	private static final long serialVersionUID = 1L;
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		PrintWriter writer = resp.getWriter();
		InputStreamReader inStreamReader = new InputStreamReader(req.getInputStream());
		
		final char[] buffer = new char[8096];
		StringBuilder output = new StringBuilder(8096);
		try
		{
			for (int read = inStreamReader.read(buffer, 0, buffer.length); read != -1; read = inStreamReader.read(buffer, 0, buffer.length))
			{
				output.append(buffer, 0, read);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		
		String responseStr = "[DME2SimpleServletResponse = " + output.toString() + "]";
		writer.println(responseStr);
		writer.flush();
	}

}
