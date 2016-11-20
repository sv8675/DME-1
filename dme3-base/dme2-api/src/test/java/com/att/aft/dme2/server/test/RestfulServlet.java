/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;


public class RestfulServlet extends HttpServlet
{

	private static final long serialVersionUID = 1L;
	int counter = 0;


	public void service(HttpServletRequest req, HttpServletResponse rsp)
	{
		InputStreamReader reader = null;
		String charset = req.getCharacterEncoding();
		
		if (charset != null)
		{
			try
			{
				reader = new InputStreamReader(req.getInputStream(), charset);
			}
			catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				reader = new InputStreamReader(req.getInputStream());
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		final char[] buffer = new char[512];
		
		StringBuilder inputText = new StringBuilder(512);
		try
		{
			for (int read = reader.read(buffer, 0, buffer.length); read != -1; read = reader.read(buffer, 0,
					buffer.length))
			{
				inputText.append(buffer, 0, read);
				counter++;
				rsp.getOutputStream().write(new String("Chunked response " + counter).getBytes());
			}
			rsp.getOutputStream().flush();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				// System.out.println("request 2");
				reader.close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	public static void main(String a[]) throws Exception
	{
		Properties props = new Properties();
		props.setProperty("AFT_DME2_PORT", "19999");
		props.setProperty("AFT_LATITUDE", "33");
		System.setProperty("AFT_LATITUDE", "33");
		props.setProperty("AFT_LONGITUDE", "34");
		System.setProperty("AFT_LONGITUDE", "34");
		props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		props.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		// System.setProperty("DME2.DEBUG","true");

		DME2Configuration config = new DME2Configuration("SRMgr", props);			
		
		DME2Manager mgr = new DME2Manager("SRMgr", config);
		String svcURI = "service=com.att.aft.dme2.SpeechRestfulServlet/version=1.0.0/envContext=DEV/routeOffer=DEFAULT";

		// Create service holder for each service registration
		DME2ServiceHolder svcHolder = new DME2ServiceHolder();
		svcHolder.setServiceURI(svcURI);
		svcHolder.setManager(mgr);
		svcHolder.setContext("/RSServlet");

		RestfulServlet echoServlet = new RestfulServlet();
		String pattern[] = { "/rsservlet" };
		DME2ServletHolder srvHolder = new DME2ServletHolder(echoServlet, pattern);
		srvHolder.setContextPath("/rsservlet");

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
		while (true)
		{
			Thread.sleep(4000);
		}

	}
}
