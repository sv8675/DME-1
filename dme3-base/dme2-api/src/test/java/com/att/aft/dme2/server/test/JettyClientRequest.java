/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The Class JettyClientRequest.
 */
public class JettyClientRequest implements Runnable {

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/** The my id. */
	String myId = null;

	/** The weburl. */
	String weburl = null;

	/**
	 * Instantiates a new jetty client request.
	 * 
	 * @param id
	 *            the id
	 * @param url
	 *            the url
	 */
	public JettyClientRequest(int id, String url) {
		myId = "JettyClientRequest:" + id;
		this.weburl = url;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		try
		{
			long start = System.currentTimeMillis();
			URL url = new URL(weburl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setDefaultUseCaches(false);
			connection.setRequestMethod("POST");
			// connection.setConnectTimeout(1000);

			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line = null;
			StringBuffer sb = new StringBuffer();
			while ((line = reader.readLine()) != null)
			{
				sb.append(line);
			}
			long end = System.currentTimeMillis();
			ResultCounter.passed++;
			System.out.println(this.myId + " passed. Time taken by " + this.myId + "=" + (end - start));
		}
		catch (Exception e)
		{
			ResultCounter.failed++;
			System.err.println(this.myId + " failed.");
			e.printStackTrace();
		}

	}

}
