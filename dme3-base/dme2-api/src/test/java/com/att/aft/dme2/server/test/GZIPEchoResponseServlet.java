/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The Class EchoServlet.
 */
public class GZIPEchoResponseServlet extends HttpServlet {

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
	public GZIPEchoResponseServlet(String service, String serverId) {
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
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String charset = req.getCharacterEncoding();
		PrintWriter writer = resp.getWriter();
		// set as a header so it can be checked on the response for testing purposes
		resp.setHeader("com.att.aft.dme2.test.charset", charset);
		GZIPInputStream gis = new GZIPInputStream(req.getInputStream());
		InputStreamReader ir = new InputStreamReader(gis);
		
		final char[] buffer = new char[8096];
		StringBuilder output = new StringBuilder(8096);
		try {
			for (int read = ir.read(buffer, 0, buffer.length); read != -1; read = ir
					.read(buffer, 0, buffer.length)) {
				output.append(buffer, 0, read);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		System.out.println("output from Gzip echo response servlet " + output);
		writer.println("EchoServlet:::" + serverId + ":::" + service + ";Request=" + output.toString());
		writer.flush();
	}

}
