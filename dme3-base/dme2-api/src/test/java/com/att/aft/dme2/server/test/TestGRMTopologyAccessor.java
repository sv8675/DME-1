/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import org.junit.Ignore;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.registry.accessor.BaseAccessor;

import junit.framework.TestCase;

@Ignore
public class TestGRMTopologyAccessor 
extends TestCase
{
	BaseAccessor subject;
	
	public void setUp() 
	throws DME2Exception
	{
/**		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		System.setProperty("platform", "SANDBOX-LAB");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("AFT_DME2_GRM_TOPOLOGY_URLS", "http://zldv0432.vci.att.com:8080/rest");
		Properties props = RegistryFsSetup.init();

		DME2Configuration config = new DME2Configuration("test", props);			

		DME2Manager mgr = new DME2Manager();
		subject = GRMAccessorFactory.getGrmAccessorHandlerInstance(config); */
	}
	
	public void testGetEndpoints()
	throws DME2Exception
	{
/**		ServiceEndpoint sep = new ServiceEndpoint();
		sep.setName("com.att.aft.DME#CREchoService");
		sep.setRouteOffer("BAU");
		List<ServiceEndpoint> result = subject.findRunningServiceEndPoint(sep);
		assertFalse(result.isEmpty()); */
	}
}
