/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.net.InetAddress;
import java.util.List;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.scld.grm.types.v1.VersionDefinition;

import junit.framework.TestCase;

@Ignore
public class TestDME2PublishHostAddress extends TestCase {

	public void setUp() {
//		super.setUp();
	}

	/**
	 * The below test case is tied with build host being aldi004
	 * since it has dependency on using a logical address to validate the host address
	 */

	  @Ignore
	  @Test
	public void publishWithLrmHost() {
    if ( !isBuildHostAldi004() ) {
      return;
    }
		// Below host is a logical address on build host aldi004
		String hostName = "";
		System.setProperty("lrmHost", hostName);
		DME2Manager manager = null;
		String serviceName = "com.att.aft.dme2.test.TestPublishWithLrmHost";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";

		String service = "/service="+serviceName + "/version=" + serviceVersion + "/envContext=" + envContext
				+ "/routeOffer=" + routeOffer;
		try {
			Properties props = RegistryFsSetup.init();
			// Set a lower lease renew frequency
			props.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "30000");

			DME2Configuration config = new DME2Configuration("testPublishWithLrmHost", props);			
			manager = new DME2Manager("testPublishWithLrmHost", config);
						
//			manager = new DME2Manager("testPublishWithLrmHost", props);

			EchoServlet servlet = new EchoServlet(service, "5");
			manager.bindServiceListener(service, servlet);

			// Validate the endpoint resolved is using lrmHost value, but not
			// phsyical host name.
			List<ServiceEndpoint> epList = getFindRunningResponse(serviceName,serviceVersion,envContext);
			boolean epFound = false;
			long leaseTime = 0L;
			for (ServiceEndpoint ep : epList) {
				System.out.println("EP address: " + ep.getHostAddress());
				assertTrue(ep.getHostAddress().equalsIgnoreCase(hostName));
				leaseTime = ep.getExpirationTime().toGregorianCalendar().getTimeInMillis();
				System.out.println("EP leaseTime: " + leaseTime);
				epFound = true;
			}
			assertTrue(epFound);

			Thread.sleep(31000);
			// Validate the endpoint resolved had been renewed and still holding
			// lrmHostname in it.
			epList = getFindRunningResponse(serviceName,serviceVersion,envContext);
			epFound = false;
			for (ServiceEndpoint ep : epList) {
				System.out.println("EP address: " + ep.getHostAddress());
				assertTrue(ep.getHostAddress().equalsIgnoreCase(hostName));
				System.out.println("EP leaseTime: " + ep.getExpirationTime().toGregorianCalendar().getTimeInMillis());
				assertTrue(leaseTime < ep.getExpirationTime().toGregorianCalendar().getTimeInMillis());
				epFound = true;
			}

			assertTrue(epFound);

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("lrmHost");
			try {
				if (manager != null)
					manager.unbindServiceListener(service);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	  @Ignore
	  @Test
	  public void testPublishWithPhysicalHost() {
		String hostName = null;
		DME2Manager manager = null;
		String serviceName = "com.att.aft.dme2.test.TestPublishWithPhysicalHost";
		String serviceVersion = "1.0.0";
		String envContext = "LAB";
		String routeOffer = "TEST";

		String service = "/service="+serviceName + "/version=" + serviceVersion + "/envContext=" + envContext
				+ "/routeOffer=" + routeOffer;
		try {
			hostName = InetAddress.getLocalHost().getCanonicalHostName();
		
			Properties props = RegistryFsSetup.init();
			// Set a lower lease renew frequency
			props.setProperty("DME2_SEP_LEASE_RENEW_FREQUENCY_MS", "30000");
			
			DME2Configuration config = new DME2Configuration("testPublishWithPhysicalHost", props);			
			manager = new DME2Manager("testPublishWithPhysicalHost", config);
			
//			manager = new DME2Manager("testPublishWithPhysicalHost", props);

			EchoServlet servlet = new EchoServlet(service, "5");
			manager.bindServiceListener(service, servlet);

			// Validate the endpoint resolved is physical hostName value
			//  host name.
			List<ServiceEndpoint> epList = getFindRunningResponse(serviceName,serviceVersion,envContext);
			boolean epFound = false;
			long leaseTime = 0L;
			System.out.println("Hostname = " + hostName);
			for (ServiceEndpoint ep : epList) {
				System.out.println("EP address: " + ep.getHostAddress());
				assertTrue(ep.getHostAddress().startsWith(hostName));
				System.out.println("EP leaseTime: " + leaseTime);
				leaseTime = ep.getExpirationTime().toGregorianCalendar().getTimeInMillis();
				epFound = true;
			}
			assertTrue(epFound);

			Thread.sleep(31000);
			// Validate the endpoint resolved had been renewed and still holding
			// physical hostname in it.
			epList = getFindRunningResponse(serviceName,serviceVersion,envContext);
			epFound = false;
			for (ServiceEndpoint ep : epList) {
				System.out.println("EP address: " + ep.getHostAddress());
				assertTrue(ep.getHostAddress().equalsIgnoreCase(hostName));
				System.out.println("EP leaseTime: " + ep.getExpirationTime().toGregorianCalendar().getTimeInMillis());
				assertTrue(leaseTime < ep.getExpirationTime().toGregorianCalendar().getTimeInMillis());
				epFound = true;
			}

			assertTrue(epFound);

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("lrmHost");
			try {
				if (manager != null)
					manager.unbindServiceListener(service);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private boolean isBuildHostAldi004() {
		try {
			String runHost = InetAddress.getLocalHost().getHostName();
			System.out.println("Execution host : " + runHost);
			if (runHost.startsWith("aldi004")) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
private VersionDefinition getVersionDefinition(String version) {
		
		int majorVersion=0;
		int minorVersion=0;
		
		String patchVersion = null;
		
		VersionDefinition vd = new VersionDefinition();
		
		if (version != null) {
			String[] tmpVersion = version.split("\\.");
			
			if (tmpVersion.length == 3) {
				majorVersion = Integer.parseInt(tmpVersion[0]);
				minorVersion = Integer.parseInt(tmpVersion[1]);
				patchVersion = tmpVersion[2];
			}
			
			if (tmpVersion.length == 2) {
				majorVersion = Integer.parseInt(tmpVersion[0]);
				minorVersion = Integer.parseInt(tmpVersion[1]);
				patchVersion = null;
			}
			
			if (tmpVersion.length == 1) {
				majorVersion = Integer.parseInt(tmpVersion[0]);
				minorVersion = -1;
				patchVersion = null;
			}
		}

		vd.setMajor(majorVersion);
		vd.setMinor(minorVersion);
		vd.setPatch(patchVersion);
		
		return vd;
	}

	private List<ServiceEndpoint> getFindRunningResponse(String serviceName, String serviceVersion,String envContext) throws DME2Exception {
		//BaseGRMServiceAccessor grm = new GRMServiceAccessor();
		DME2Configuration config = new DME2Configuration("testPublishWithPhysicalHost", new Properties());			

		BaseAccessor grm = GRMAccessorFactory.getGrmAccessorHandlerInstance( config, SecurityContext.create(config) );

		//FindRunningServiceEndPointRequest fsvdReq = new FindRunningServiceEndPointRequest();
		//fsvdReq.setEnv(envContext);
		ServiceEndpoint sep = new ServiceEndpoint();
		sep.setName(serviceName);
		sep.setVersion(serviceVersion);
		sep.setEnv(envContext);
		
		//FindServiceEndPointBySVDRequest svdReq = new FindServiceEndPointBySVDRequest();
		//svdReq.setRetrieveRunningEpsOnly(true);
		
		//VersionDefinition vd = this.getVersionDefinition(serviceVersion);

		//ServiceVersionDefinition svd = new ServiceVersionDefinition();
		//svd.setName(serviceName);
		//svd.setVersion(vd);
		//sep.setVersion(vd);
		
		//fsvdReq.setServiceEndPoint(sep);

		//svdReq.setServiceVersionDefinition(svd);
		//svdReq.setEnv(envContext);
		
		return grm.findRunningServiceEndPoint(sep);
	}
}
