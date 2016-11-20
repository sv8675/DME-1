/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;

public class GWServlet extends HttpServlet {
	/** The server id. */
	private String serverId = null;

	/** The service. */
	private String service = null;
	
	public GWServlet(String service, String serverId) {
		this.service = service;
		this.serverId = serverId;
	}

	
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		InputStream ins = null;
		InputStreamReader reader = null;
		String charset = req.getCharacterEncoding();
		String outCharSet = req.getHeader("testReturnCharSet");
		String sleepTime = req.getHeader("echoSleepTimeMs");
		String testReturnFault = req.getHeader("testReturnFault1");
		String testReturnStream = req.getHeader("testReturnStream");
		String testCallDownstream = req.getHeader("testCallDownstream");
		if (testCallDownstream != null  || testReturnStream != null) {
			ins = req.getInputStream();
		}
		else {
			if (charset != null) {
				reader = new InputStreamReader(req.getInputStream(), charset);
			} else {
				reader = new InputStreamReader(req.getInputStream());
			}
		}
		if (outCharSet != null) {
			resp.setContentType("text/plain; charset=UTF-8");
		}
		if (testCallDownstream != null) {
			try {
				Properties props = new Properties();
				DME2Configuration config = new DME2Configuration("JettyClient", props);			
				
				DME2Manager mgr = new DME2Manager("JettyClient", config);

				//DME2Manager mgr = new DME2Manager("JettyClient", props);
				String clientUri = "http://DME2SEARCH/service=com.att.aft.dme2.SpeechRestfulServlet/version=1/envContext=DEV/routeOffer=BAU/partner=abc";

				Enumeration<String> headers = req.getHeaderNames();
				HashMap<String, String> hmHeaders = new HashMap<String, String>();
				while (headers.hasMoreElements()) {
					String headerName = (String) headers.nextElement();
					String headerValue = req.getHeader(headerName);
					hmHeaders.put(headerName, headerValue);
				}

				Request request = new RequestBuilder(new URI(clientUri)).withReadTimeout(20000).withHeaders(hmHeaders).withSubContext("rsservlet").withReturnResponseAsBytes(false).withLookupURL(clientUri).build();
				DME2Client client = new DME2Client(mgr, request);
				
//				DME2Client client = new DME2Client(mgr, new URI(clientUri), 10000);
				DME2StreamPayload inPayload = new DME2StreamPayload(ins);
//				client.setDME2Payload(inPayload);
//				client.setHeaders(hmHeaders);
//				client.setSubContext("rsservlet");
				String response = (String) client.sendAndWait(inPayload);
				resp.getWriter().print(response);
				return;
			} catch (DME2Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		String requestText = null;
		ByteArrayOutputStream bos = null;
		if(ins != null && testReturnStream != null) {
			bos = new ByteArrayOutputStream();
			byte[] byteArr = new byte[8096];
			for (int read = ins.read(byteArr, 0, byteArr.length); read != -1; read = ins
					.read(byteArr, 0, byteArr.length)) {
				bos.write(byteArr, 0, read);
			}
			
		}
		else {
		final char[] buffer = new char[8096];
		StringBuilder inputText = new StringBuilder(8096);
		try {
			for (int read = reader.read(buffer, 0, buffer.length); read != -1; read = reader
					.read(buffer, 0, buffer.length)) {
				inputText.append(buffer, 0, read);
			}
		} finally {
			reader.close();
		}

		requestText = inputText.toString();
		}
		if(sleepTime != null ) {
			try {
			long sleepTimeInMs = Long.parseLong(sleepTime);
			Thread.sleep(sleepTimeInMs);
			}catch(Exception e) {
				// print stack for debug purpose.
				e.printStackTrace();
			}
		}
		
			//PrintWriter writer = resp.getWriter();
				OutputStream os = resp.getOutputStream();
				//System.out.println("CHAR encoding: " + resp.getCharacterEncoding());
				// set as a header so it can be checked on the response for testing purposes
				if (testReturnFault != null && testReturnFault.equals("true")) {
					//throw new RuntimeException("FORCED EXCEPTION");
					resp.setStatus(500);
					//String s = "<?xml version=\"1.0\" ?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns1=\"http://test.com/test\"><soapenv:Body><soapenv:Fault xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>soapenv:Server</faultcode><faultstring>UNEXPECTED_ERROR</faultstring><detail><ns1:APIStatus><StatusCode>1000</StatusCode><Message>UNEXPECTED_ERROR : test </Message></ns1:APIStatus></detail></soapenv:Fault></soapenv:Body></soapenv:Envelope>" + "/n";
					//os.write(s.getBytes());
					//writer.println("<?xml version=\"1.0\" ?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns1=\"http://test.com/test\"><soapenv:Body><soapenv:Fault xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><faultcode>soapenv:Server</faultcode><faultstring>UNEXPECTED_ERROR</faultstring><detail><ns1:APIStatus><StatusCode>1000</StatusCode><Message>UNEXPECTED_ERROR : test </Message></ns1:APIStatus></detail></soapenv:Fault></soapenv:Body></soapenv:Envelope>");
				}
				/*
				else if (outEchoBack != null && outEchoBack.equals("true")) {
					os = resp.getOutputStream();
					if(requestText != null && charset != null)
						os.write(requestText.getBytes(charset));
					else
						os.write(requestText.getBytes());
				}*/
				else if(testReturnStream != null && bos != null) {
					String s = "EchoServlet:::GWServlet" + serverId + ":::" + service + ";size="+ bos.size() +"\n";
					os.write(s.getBytes());
				}
				else {
					String s = "EchoServlet:::GWServlet" + serverId + ":::" + service +"\n";
					os.write(s.getBytes());
					//writer.println("EchoServlet:::" + serverId + ":::" + service);
				}
				os.flush();
	}

	public static void main(String a[]) throws Exception {
		Properties props = new Properties();
		props.setProperty("AFT_DME2_PORT", "9999");
		props.setProperty("AFT_LATITUDE", "33");
		System.setProperty("AFT_LATITUDE", "33");
		props.setProperty("AFT_LONGITUDE", "34");
		System.setProperty("AFT_LONGITUDE", "34");
		props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		props.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");

		DME2Configuration config = new DME2Configuration("GWMgr", props);			
		
		DME2Manager mgr = new DME2Manager("GWMgr", config);

//		DME2Manager mgr = new DME2Manager("GWMgr", props);
		String svcURI = "service=com.att.aft.dme2.GWServletTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

		// Create service holder for each service registration
		DME2ServiceHolder svcHolder = new DME2ServiceHolder();
		svcHolder.setServiceURI(svcURI);
		svcHolder.setManager(mgr);
		svcHolder.setContext("/GWServlet");

		GWServlet echoServlet = new GWServlet("1",svcURI);
		String pattern[] = { "/gwservlet" };
		DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
		srvHolder.setContextPath("/gwservlet");

		List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
		shList.add(srvHolder);
		// If context is set, DME2 will use this for publishing as context with
		// endpoint registration, else serviceURI above will be used
		// svcHolder.setContext("/FilterTest");
		// Below is to disable the default metrics filter thats added to
		// capture DME2 Metrics event of http traffic. By default MetricsFilter
		// is enabled
		// svcHolder.disableMetricsFilter();

		svcHolder.setServletHolders(shList);

		// mgr.addService(svcHolder);
		mgr.getServer().start();
		mgr.bindService(svcHolder);

		Thread.sleep(4000);
		while (true) {
			Thread.sleep(4000);
		}

	}
}