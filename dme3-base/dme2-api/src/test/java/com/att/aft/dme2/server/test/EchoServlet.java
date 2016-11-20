/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * The Class EchoServlet.
 */
public class EchoServlet extends HttpServlet {
  private static final Logger logger = LoggerFactory.getLogger( EchoServlet.class );

	private static final long serialVersionUID = 4095360973134144105L;

	/** The server id. */
	private String serverId = null;

	/** The service. */
	private String service = null;
	
	/** **/
	private String servletParamValue = null;
	
	private String contextParamValue = null;

	/**
	 * Instantiates a new echo servlet.
	 * 
	 * @param service
	 *            the service
	 * @param serverId
	 *            the server id
	 */
	public EchoServlet(String service, String serverId) {
		this.service = service;
		this.serverId = serverId;
	}
	
	public void init() throws ServletException {
	    // Get the value of an initialization parameter
	    String value = getServletConfig().getInitParameter("testParam");
	    if(value != null) {
	    	servletParamValue = value;
	    }
	    value = getServletContext().getInitParameter("testContextParam");
	    if(value != null) {
	    	contextParamValue = value;
	    }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String charset = req.getCharacterEncoding();
		String outCharSet = req.getHeader("testReturnCharSet");
		String outEchoBack = req.getHeader("testEchoBack");
		String sleepTime = req.getHeader("echoSleepTimeMs");
		String testServletParam = req.getHeader("testReturnServletParam");
		String testContextParam = req.getHeader("testReturnServletContextParam");
		String testReturnFault = req.getHeader("testReturnFault");
    String testRequestNumber = req.getHeader( "testRequestNumber" );
		String testReturnFailoverEnabledFault = req.getHeader("testReturnFailoverEnabledFault");
		String testReturnNonFailoverEnabledFault = req.getHeader("testReturnNonFailoverEnabledFault");
		String testReturnQueryParams = req.getHeader("testReturnQueryParams");

		
		InputStreamReader reader = null;
		if (charset != null) {
			System.out.println("Charset " + charset);
			reader = new InputStreamReader(req.getInputStream(), charset);
		} else {
			reader = new InputStreamReader(req.getInputStream());
		}

		final char[] buffer = new char[8096];
		StringBuilder inputText = new StringBuilder(8096);
		
		try {
			for (int read = reader.read(buffer, 0, buffer.length); read != -1; read = reader.read(buffer, 0, buffer.length)) {
				inputText.append(buffer, 0, read);
			}
		} finally {
			reader.close();
		}

		String requestText = inputText.toString();

		if (outCharSet != null) {
			resp.setContentType("text/plain; charset=UTF-8");
		}

		if (sleepTime != null) {
			try {
				long sleepTimeInMs = Long.parseLong(sleepTime);
        logger.debug( null, "service", "Found sleep time, sleeping for {}", sleepTimeInMs);
				Thread.sleep(sleepTimeInMs);
			} catch (Exception e) {
				// print stack for debug purpose.
				e.printStackTrace();
			}
		}

		PrintWriter writer = resp.getWriter();
		// set as a header so it can be checked on the response for testing purposes
		resp.setHeader("com.att.aft.dme2.test.charset", charset);
		
		if (testReturnFault != null && testReturnFault.equals("true")) {
			writer.println("<?xml version=\"1.0\" ?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns1=\"http://test.com/test\"><soapenv:Body><soapenv:Fault xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>soapenv:Server</faultcode><faultstring>UNEXPECTED_ERROR</faultstring><detail><ns1:APIStatus><StatusCode>1000</StatusCode><Message>UNEXPECTED_ERROR : test </Message></ns1:APIStatus></detail></soapenv:Fault></soapenv:Body></soapenv:Envelope>");
		}
		
		if (testReturnFailoverEnabledFault != null && testReturnFailoverEnabledFault.equals("true")) {
			writer.println("<?xml version=\"1.0\" ?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns1=\"http://test.com/test\"><soapenv:Body><soapenv:Fault xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>soapenv:Server</faultcode><faultstring>UNEXPECTED_ERROR;FailoverRequired=true</faultstring><detail><ns1:APIStatus><StatusCode>1000</StatusCode><Message>UNEXPECTED_ERROR : test </Message></ns1:APIStatus></detail></soapenv:Fault></soapenv:Body></soapenv:Envelope>");
		
			resp.setStatus(500);
			resp.setContentType("text/xml");
		} else if (testReturnNonFailoverEnabledFault != null && testReturnNonFailoverEnabledFault.equals("true")) {
			writer.println("<?xml version=\"1.0\" ?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns1=\"http://test.com/test\"><soapenv:Body><soapenv:Fault xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>soapenv:Server</faultcode><faultstring>UNEXPECTED_ERROR;FailoverRequired=false</faultstring><detail><ns1:APIStatus><StatusCode>1000</StatusCode><Message>UNEXPECTED_ERROR : test </Message></ns1:APIStatus></detail></soapenv:Fault></soapenv:Body></soapenv:Envelope>");
		
			resp.setStatus(500);
			resp.setContentType("text/xml");
		} else if (outEchoBack != null && outEchoBack.equals("true")) {
//			resp.setIntHeader("Content-Length", requestText.length());
			System.out.println("Request Text : " + requestText);
			writer.print(requestText);
		} else if (testServletParam != null) {
			writer.println("EchoServlet:::" + serverId + ":::" + service + "|servletParam=" + this.servletParamValue);
		} else if (testContextParam != null) {
			writer.println("EchoServlet:::" + serverId + ":::" + service + "|contextParam=" + this.contextParamValue);
		} else if (testReturnQueryParams != null && testReturnQueryParams.equalsIgnoreCase("true")) {
      writer.println( "EchoServlet:::" + serverId + ":::" + service + "|queryParams=" + req.getQueryString() );
    } else if ( testRequestNumber != null ) {
      writer.println("EchoServlet:::" + serverId + ":::" + service + "|testRequestNumber=" + testRequestNumber );
    } else {
			writer.println("EchoServlet:::" + serverId + ":::" + service);
		}
		writer.flush();
	}
}