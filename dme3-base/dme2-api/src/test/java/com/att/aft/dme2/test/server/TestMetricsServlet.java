/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.test.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.util.DME2Constants;

/**
 * The Class EchoServlet.
 */
public class TestMetricsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

//	private static com.att.aft.metrics.collector.MetricsCollector contCollector;

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
	public TestMetricsServlet(String service, String serverId) {
		String containerName="com.att.test.MetricsTestCollectorClient"; 
		String containerVersion="1.0.0"; 
		String containerRO="TESTRO"; 
		String containerEnv="DEV"; 
		String containerPlat=TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV; 
//		String containerEnv="LAB"; 
//		String containerPlat="SANDBOX-LAB"; 
		String containerHost=null; 
		String containerPid="1234"; 
		String containerPartner="TEST";
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "true");
		System.setProperty("platform", containerPlat);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
		//System.setProperty("platform","NON-PROD");

/**		containerName="com.att.test.MetricsTestCollectorClient"; 
		containerVersion="1.0.0"; 
		containerRO="TESTRO"; 
		containerEnv="LAB"; 
		containerPlat="SANDBOX-LAB"; 
		containerHost=null; 
		containerPid="1234"; 
		containerPartner="TEST";
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("platform", containerPlat);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
		//System.setProperty("platform","NON-LAB")
		System.setProperty("DME2.DEBUG", "true");
*/
//		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
//		System.setProperty("platform", "SANDBOX-LAB");
//		System.setProperty("DME2.DEBUG", "true");
//		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		
		
		try {
			containerHost = InetAddress.getLocalHost().getHostName();
		}catch(Exception e) {
			
		}
		System.setProperty("lrmHost",containerHost);
		System.setProperty("Pid",containerPid);
		System.setProperty("partner",containerPartner);
		this.service = service;
		this.serverId = serverId;
		System.out.println(containerName + " , " + containerVersion + " , " + containerRO + " , " + containerEnv + " , " + 
				containerPlat + " , " + containerHost + " , " + containerPid + " , " + "TEST");
/**		contCollector = com.att.aft.metrics.collector.MetricsCollectorFactory.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
				containerPlat, containerHost, containerPid, "TEST");
		System.out.println("contCollector : " + contCollector);
		System.out.println("contCollector : " + contCollector.getClass());
		System.out.println("contCollector : " + contCollector.getCurrentTimeSlot());
		
		contCollector.setDisablePublish(true);
*/
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
		System.out.println("Got a request...");
		long eventTime = System.currentTimeMillis();
		DME2Configuration config = new DME2Configuration("FS");
		DmeUniformResource resource = null;
		System.out.println("service : " + service);
		try {
			resource = new DmeUniformResource(config, new URI("http://DME2LOCAL/"+service));
		}catch ( Exception e) {
			e.printStackTrace();
		}
		System.out.println("resource : " + resource);
/**		contCollector.addEvent(eventTime, resource.getService(), resource.getVersion(), DME2Constants.DME2_INTERFACE_SERVER_ROLE, "1234", config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL), Event.RESPONSE, "TestMsg", 1000, 900);
		System.out.println("contCollector : " + contCollector);
		System.out.println("contCollector : " + contCollector.getClass());
		System.out.println("contCollector : " + contCollector.getCurrentTimeSlot());
		Timeslot slot = contCollector.getTimeslotForTime(eventTime);

		Collection<SvcProtocolMetrics> metricsc = slot.getAllMetrics();
		Iterator<SvcProtocolMetrics> it = metricsc.iterator();
		String env = null;
		while(it.hasNext()) {
			SvcProtocolMetrics m = it.next();
			System.out.println(m.getService() + ":" + m.getTimeslot() + ":" + m.getMetricsTimeslot());
			env = m.getContainer().getEnv();
		}

		SvcProtocolMetrics metrics = slot.getServiceProtocolMetrics(resource.getService(), resource.getVersion(),DME2Constants.DME2_INTERFACE_SERVER_ROLE, "1234", config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
*/
		PrintWriter writer = resp.getWriter();
		String metrics = "";
		System.out.println("metrics : " + metrics.toString());
		writer.println("EchoServlet:::" + serverId + ":::" + service + "Metrics["+metrics+"]");
		writer.flush();
	}

}
