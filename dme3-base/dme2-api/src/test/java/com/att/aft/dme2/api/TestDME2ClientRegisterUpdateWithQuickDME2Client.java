/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import static com.att.aft.dme2.logging.LogMessage.METHOD_ENTER;
import static com.att.aft.dme2.logging.LogMessage.METHOD_EXIT;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.api.quick.QuickClient;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Constants;
import com.att.scld.grm.types.v1.ClientJVMInstance;

public class TestDME2ClientRegisterUpdateWithQuickDME2Client extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestDME2ClientRegisterUpdateWithQuickDME2Client.class );

	@Before
	public void setUp() {
		super.setUp();
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("org.eclipse.jetty.util.UrlEncoding.charset","UTF-8");
		System.setProperty("metrics.debug", "true");
		System.setProperty("SCLD_PLATFORM", "SANDBOX-DEV");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty( DME2Constants.DME2_GRM_USER, "mxxxxx");
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
	public void testDME2ClientRegisterUpdateWithQuickDME2Client() throws Exception {
    logger.debug( null, "testDME2ClientRegisterUpdateWithQuickDME2Client", METHOD_ENTER );
		DME2Manager mgr = null;

		try {

			mgr = new DME2Manager("com.att.aft.dme2.test.testDME2ClientRegisterUpdateWithQuickDME2Client", new Properties());
			DME2EndpointRegistry svcRegistry = mgr.getEndpointRegistry();

			Thread quickClient = new Thread(new Runnable() {
				public void run() {
					try {
						System.setProperty("DME2_JVM_LEASE_RENEW_FREQUENCY_MS", "3000");
						String[] args = {"-t", "10000", "-s", "http://DME2RESOLVE/service=com.att.aft.DME2CREchoService/version=1.5.0/envContext=LAB/routeOffer=BAU", "-m", "TESTMESSAGE", "-sleep", "100000" };
						QuickClient.main(args);
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			});
			quickClient.start();

			XMLGregorianCalendar expTime = null;

			Thread.sleep(6000);

			String pid = ManagementFactory.getRuntimeMXBean().getName();
			String pidToCheck = pid.contains("@") ? pid.substring(0, pid.indexOf("@")) : pid;
			System.out.println("Expecting Process ID: " + pidToCheck);

			List<ClientJVMInstance> jvmList = svcRegistry.findRegisteredJVM("LAB", true, InetAddress.getLocalHost().getCanonicalHostName(), System.getProperty(DME2Constants.DME2_GRM_USER, "mxxxxx"), null);

			if (quickClient.isAlive()) {
				Boolean verified = false;
				for (ClientJVMInstance instance : jvmList) {
					System.out.println("Returned Process ID: " + instance.getProcessId());
					if (instance.getProcessId().equalsIgnoreCase(pidToCheck)) {
						verified = true;
						expTime = instance.getExpirationTime();
						break;
					}
				}
				assertTrue(verified);
			} else {
				assertTrue(false);
			}

			Thread.sleep(60000);

			jvmList = svcRegistry.findRegisteredJVM("LAB", true, InetAddress.getLocalHost().getCanonicalHostName(), System.getProperty(DME2Constants.DME2_GRM_USER, "mxxxxx"), null);

			Boolean verified = false;
			for (ClientJVMInstance instance : jvmList) {
				System.out.println("Returned Process ID: " + instance.getProcessId());
				if (instance.getProcessId().equalsIgnoreCase(pidToCheck)) {
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

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				mgr.shutdown();
			} catch (Exception e){}
      logger.debug( null, "testDME2ClientRegisterUpdateWithQuickDME2Client", METHOD_EXIT );
		}
	}
}
