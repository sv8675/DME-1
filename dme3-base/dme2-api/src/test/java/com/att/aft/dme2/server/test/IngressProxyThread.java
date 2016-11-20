/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

public class IngressProxyThread implements Runnable{

	String[] args;
	public IngressProxyThread(String args[]) {
		this.args = args;
	}
	
	public void run() {
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		//System.setProperty("platform", "NON-PROD");
		System.setProperty("platform", "SANDBOX-DEV");
		System.setProperty("AFT_LATITUDE", "33");
		System.setProperty("AFT_LONGITUDE", "44");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");

		try {
			com.att.aft.dme2.api.proxy.DME2IngressProxy.main(args);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
