/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.DME2NullServlet;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.test.DME2BaseTestCase;

public class TestEndpointRegistryPublish extends DME2BaseTestCase {

	/**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
    @Test
    public void testEndpointRegistryPublish() throws DME2Exception, InterruptedException, UnknownHostException {
		System.err.println("--- START: testEndpointRegistryPublish");
		String service = "com.att.aft.dme2.test.TestEndpointRegistryPublish";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 3267;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

		Properties props = new Properties();
		props.setProperty("AFT_DME2_PORT","33132");
		DME2Manager manager = new DME2Manager("testEndpointRegistryPublish", props);
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName,null, hostname, port, latitude, longitude, "http");

		System.err.println("Service published successfully.");

		Thread.sleep(10000);

		DME2Endpoint[] endpoints  = svcRegistry.find(service, version, envContext, routeOffer);
		//System.out.println(endpoints[0].getServiceName());

		DME2Endpoint found = null;
		for (DME2Endpoint ep : endpoints) {
			if (ep.getHost().equals(hostname) && ep.getPort() == port
					&& ep.getLatitude() == latitude
					&& ep.getLongitude() == longitude) {
				found = ep;
				assertNotNull(ep.getEndpointProperties());
//				assertTrue((ep.getEndpointProperties().size()==0));
			}
		}
		System.err.println("Found registered endpoint: " + found);
		assertNotNull(found);

		svcRegistry.unpublish(serviceName, hostname, port);
	}

    /**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
    @Test
    public void testEndpointPublishWithServletBinding() throws DME2Exception, InterruptedException, UnknownHostException {
		System.err.println("--- START: testEndpointPublishWithServletBinding");
		String service = "com.att.aft.dme2.test.TestEndpointPublishWithServletBinding";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 3267;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "/service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

		Properties props = new Properties();
		props.setProperty("AFT_DME2_PORT1","33132");
		DME2Manager manager = new DME2Manager("TestEndpointPublishWithServletBinding", props);
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		manager.bindServiceListener(serviceName, new DME2NullServlet());

		System.err.println("Service published successfully.");

		Thread.sleep(10000);

		DME2Endpoint[] endpoints  = svcRegistry.find(service, version, envContext, routeOffer);
		//System.out.println(endpoints[0].getServiceName());

		DME2Endpoint found = null;
		for (DME2Endpoint ep : endpoints) {
				found = ep;
				assertNotNull(ep.getEndpointProperties());
//				assertTrue((ep.getEndpointProperties().size()==0));
		}
		System.err.println("Found registered endpoint: " + found);
		assertNotNull(found);

		try{
		manager.getServer().stop();
		} catch(Exception e) {
			//ignore any failure in stop
		}
	}

    /**
	 * 
	 * 
	 * @throws DME2Exception 
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
    @Ignore
    @Test
    public void testEndpointRegistryPublishWithProps() throws DME2Exception, InterruptedException, UnknownHostException {
		System.err.println("--- START: testEndpointRegistryPublishWithProps");
		String service = "com.att.aft.dme2.test.TestEndpointRegistryPublishWithProps";
		String hostname = InetAddress.getLocalHost().getCanonicalHostName();
		int port = 3267;
		double latitude= 33.3739;
		double longitude= 86.7983;

		String version = "1.0.0";
		String envContext = "DEV";
		String routeOffer = "BAU_SE";
		String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;

		Properties props = new Properties();
		props.setProperty("AFT_DME2_PORT1","33132");
		props.setProperty("AFT_DME2_PORT2","33133");
        Properties dme2ManagerProps = new Properties();
		dme2ManagerProps.setProperty("AFT_DME2_PORT",port+"");
		DME2Manager manager = new DME2Manager("testEndpointRegistryPublishWithProps", dme2ManagerProps);
		DME2EndpointRegistry svcRegistry = manager.getEndpointRegistry();
		svcRegistry.publish(serviceName,null, hostname, port, latitude, longitude, "http",props,false);

		System.err.println("Service published successfully.");

		Thread.sleep(10000);

		DME2Endpoint[] endpoints  = svcRegistry.find(service, version, envContext, routeOffer);
		//System.out.println(endpoints[0].getServiceName());

		DME2Endpoint found = null;
		for (DME2Endpoint ep : endpoints) {
			if (ep.getHost().equals(hostname) && ep.getPort() == port
					&& ep.getLatitude() == latitude
					&& ep.getLongitude() == longitude) {
				found = ep;
				assertNotNull(ep.getEndpointProperties());
				assertTrue((ep.getEndpointProperties().size()==2));
                Properties epProps = ep.getEndpointProperties();
				System.out.println(epProps);
				assertTrue(epProps.getProperty("AFT_DME2_PORT1").equals("33132"));
				assertTrue(epProps.getProperty("AFT_DME2_PORT2").equals("33133"));
			}
		}
		System.err.println("Found registered endpoint: " + found);
		assertNotNull(found);

		svcRegistry.unpublish(serviceName, hostname, port);
	}

}
