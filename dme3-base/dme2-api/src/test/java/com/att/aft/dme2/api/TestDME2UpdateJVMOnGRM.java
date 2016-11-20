/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContextListener;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.server.test.DME2TestContextListener;
import com.att.aft.dme2.server.test.EchoResponseServlet;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;
import com.att.scld.grm.types.v1.ClientJVMInstance;

@Ignore
public class TestDME2UpdateJVMOnGRM extends DME2BaseTestCase {

	@Before
	public void setUp() {
		super.setUp();
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("org.eclipse.jetty.util.UrlEncoding.charset","UTF-8");
		System.setProperty("metrics.debug", "true");
		System.setProperty("SCLD_PLATFORM", "SANDBOX-DEV");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty(DME2Constants.DME2_GRM_USER, "mxxxxx");
		System.setProperty(DME2Constants.DME2_GRM_PASS, "mxxxxx");
		System.setProperty("lrmEnv", "LAB");
	}

	@After
	public void tearDown() {
		super.tearDown();
		System.clearProperty("AFT_DME2_PUBLISH_METRICS");
		System.clearProperty("org.eclipse.jetty.util.UrlEncoding.charset");
		System.clearProperty("metrics.debug");
		System.clearProperty("SCLD_PLATFORM");
		System.clearProperty("DME2.DEBUG");
		System.clearProperty(DME2Constants.DME2_GRM_USER);
		System.clearProperty(DME2Constants.DME2_GRM_PASS);
		System.clearProperty("lrmEnv");
	}

	@Test
	public void testDME2ClientUpdateJVM() throws Exception {

		DME2Manager mgr = null;
		DME2EndpointRegistry svcRegistry = null;

		String serviceName = "com.att.aft.dme2.test.testDME2ServerUpdateJVM";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";
		String serviceURI = DME2Utils.buildServiceURIString( serviceName, serviceVersion, envContext, routeOffer );
		String processID = null;

		try {
			Properties props = new Properties();
			props.setProperty("AFT_DME2_PORT", "32405");
			props.setProperty("DME2_JVM_LEASE_RENEW_FREQUENCY_MS", "3000");

			mgr = new DME2Manager("com.att.aft.dme2.test.testDME2ServerUpdateJVM", props);

			String pattern[] = {"/test"};
			DME2ServletHolder servletHolder = new DME2ServletHolder(new EchoResponseServlet(serviceURI, "testID"), pattern);

			List<DME2ServletHolder> servletHolderList = new ArrayList<DME2ServletHolder>();
			servletHolderList.add(servletHolder);

			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(serviceURI);
			svcHolder.setManager(mgr);
			svcHolder.setServletHolders(servletHolderList);

			DME2TestContextListener contextListener = new DME2TestContextListener();

			ArrayList<ServletContextListener> contextList = new ArrayList<ServletContextListener>();
			contextList.add(contextListener);
			svcHolder.setContextListeners(contextList);

			mgr.getServer().start();
			mgr.bindService(svcHolder);

			Thread.sleep(3000);

			DME2Endpoint[] endpoints = mgr.getEndpointRegistry().find(serviceName, serviceVersion, envContext, routeOffer);
			System.out.println("Number of Endpoints returned from GRM = " + endpoints.length);
			assertTrue(endpoints.length == 1);
			System.out.println(endpoints[0].toURLString());

			String clientURI = String.format("http://%s:%s%s",  InetAddress.getLocalHost().getCanonicalHostName(), "32404", serviceURI);

			svcRegistry = mgr.getEndpointRegistry();

			DME2Client client = new DME2Client(mgr, new URI(clientURI), 300000);
			GregorianCalendar gcal = new GregorianCalendar();
			gcal.add(Calendar.HOUR_OF_DAY, 2);
			XMLGregorianCalendar expTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
			client.setContext("/test");
			client.setPayload("THIS IS A TEST");
			client.setMethod("GET");
			client.setAllowAllHttpReturnCodes(true);

			processID = mgr.getProcessID();
			System.out.println("Expecting Process ID: " + processID);

			Thread.sleep(65000);

			List<ClientJVMInstance> jvmList = svcRegistry.findRegisteredJVM("LAB", true, null, System.getProperty(
          DME2Constants.DME2_GRM_USER, "mxxxxx"), null);

			Boolean verified = false;
			for (ClientJVMInstance instance : jvmList) {
				System.out.println("Returned Process ID: " + instance.getProcessId());
				if (instance.getProcessId().equalsIgnoreCase(processID)) {
					if (instance.getExpirationTime().compare(expTime) == DatatypeConstants.GREATER) {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy MMM dd hh:mm:ss z");
						Calendar calendar1 = expTime.toGregorianCalendar();
						formatter.setTimeZone(calendar1.getTimeZone());
						Calendar calendar2 = instance.getExpirationTime().toGregorianCalendar();
						formatter.setTimeZone(calendar2.getTimeZone());
						String dateString1 = formatter.format(calendar1.getTime());
						String dateString2 = formatter.format(calendar2.getTime());
						System.out.println("Expiration time when registered: " + dateString1);
						System.out.println("Expiration time on instance: " + dateString2);
						verified = true;
						break;
					}
				}
			}
			assertTrue(verified);

		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				mgr.unbindServiceListener(serviceURI);
				mgr.shutdown();
			} catch (Exception e){}
		}
	}
}