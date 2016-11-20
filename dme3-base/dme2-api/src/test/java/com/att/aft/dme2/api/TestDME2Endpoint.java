/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *  Developed and maintained by the Common Services System Architecture (CSSA) Group
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.util.List;
import java.util.Properties;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.util.DME2DistanceUtil;
import com.att.aft.dme2.server.test.GRMFindRunningResponseServlet;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.test.Locations;

import junit.framework.TestCase;

public class TestDME2Endpoint extends TestCase
{

	public void testCompareDME2Endpoint() throws Exception 
	{
		Locations.BHAM.set();
		double bhamLat = Locations.BHAM.getLatitude();
		double bhamLong = Locations.BHAM.getLongitude();

		String servicePath = "service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/routeOffer=BAU_SE";
		DME2Endpoint endpoint = new DME2Endpoint(servicePath, DME2DistanceUtil.calculateDistanceBetween( 0, 0, bhamLat, bhamLong ));
		endpoint.setLatitude(bhamLat);
		endpoint.setLongitude(bhamLong);

		DME2Endpoint endpoint1 = new DME2Endpoint(servicePath, DME2DistanceUtil.calculateDistanceBetween( 0, 0, bhamLat, bhamLong ));
		endpoint1.setLatitude(bhamLat);
		endpoint1.setLongitude(bhamLong);

		assertEquals(0, endpoint.compareTo(endpoint1));
	}


	public void testDME2Endpoint() throws Exception 
	{
		Locations.BHAM.set();
		DME2Endpoint endpoint = new DME2Endpoint("service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/routeOffer=BAU_SE", 5d);
		assertEquals("MyService", endpoint.getServiceName());
		assertEquals("1.0.0", endpoint.getServiceVersion());
//		assertEquals("205977", endpoint.getDataContext());
		assertEquals("BAU_SE", endpoint.getRouteOffer());
	}

	public void testNegativeCompareDME2Endpoint() throws Exception 
	{
		Locations.BHAM.set();
		double bhamLat = Locations.BHAM.getLatitude();
		double bhamLong = Locations.BHAM.getLongitude();
		
		DME2Endpoint endpoint = new DME2Endpoint("service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE_SE", DME2DistanceUtil.calculateDistanceBetween( 0, 0, bhamLat, bhamLong ));
		endpoint.setLatitude(bhamLat);
		endpoint.setLongitude(bhamLong);

		double charLat = Locations.CHAR.getLatitude();
		double charLong = Locations.CHAR.getLongitude();
		DME2Endpoint endpoint1 = new DME2Endpoint("service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE_SE", DME2DistanceUtil.calculateDistanceBetween( 0, 0, charLat, charLong ));
		endpoint1.setLatitude(charLat);
		endpoint1.setLongitude(charLong);

		if (endpoint.compareTo(endpoint1) == 0)
		{
			fail("End points should not be equal.");
		}
	}
	
	public void testDME2EndpointEquals() throws Exception 
	{
		Locations.BHAM.set();
		DME2Endpoint endpoint = new DME2Endpoint("service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE_SE", 5d);
		endpoint.setHost("localhost");
		endpoint.setPort(2233);
		endpoint.setContextPath("/abc");
		
		DME2Endpoint endpoint1 = new DME2Endpoint("service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE_SE", 5d);
		endpoint1.setHost("localhost");
		endpoint1.setPort(2233);
		endpoint1.setContextPath("/abc");

		if (!endpoint.equals(endpoint1)) 
		{
			fail("End points should be equal.");
		}
	}
	
	public void testNegativeDME2EndpointEquals() throws Exception 
	{
		Locations.BHAM.set();
		DME2Endpoint endpoint = new DME2Endpoint("service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE_SE", 5d);
		endpoint.setHost("localhost");
		endpoint.setPort(2233);
		endpoint.setContextPath("/abc");
		
		DME2Endpoint endpoint1 = new DME2Endpoint("service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE_SE", 5d);
		endpoint1.setHost("localhost");
		endpoint1.setPort(2234);
		endpoint1.setContextPath("/abc");

		if (endpoint.equals(endpoint1)) 
		{
			fail("End points should not be equal.");
		}
	}
 

	public void invalidEndpointsFromGRM() throws Exception 
//	@org.junit.Ignore("ignored") @Test public void testInvalidEndpointsFromGRM() throws Exception 
	{
		DME2Manager mgr = null;
		String service = "service=com.att.aft.dme2.test.GRMSimulator/version=1.0.0/envContext=LAB/routeOffer=BAU";
		System.setProperty("AFT_DME2_GRM_URLS",	"http://localhost:52521/service=com.att.aft.dme2.test.GRMSimulator/version=1.0.0/envContext=LAB/routeOffer=BAU");
		
		try
		{
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PORT", "52521");

			DME2Configuration config = new DME2Configuration("testInvalidEndpointsFromGRM", props);			
			mgr = new DME2Manager("testInvalidEndpointsFromGRM", config);
			
//			mgr = new DME2Manager("testInvalidEndpointsFromGRM", props);
			mgr.bindServiceListener(service, new GRMFindRunningResponseServlet());
			Thread.sleep(2000);
			
			Properties props1 = RegistryFsSetup.init();
			DME2Configuration config1 = new DME2Configuration("testInvalidEndpointsFromGRMClient", props1);			
			
			//DME2Manager mgr1 = new DME2Manager("testInvalidEndpointsFromGRMClient", props1);
			DME2Manager mgr1 = new DME2Manager("testInvalidEndpointsFromGRMClient", config);
			
			List<DME2Endpoint> epList = mgr1.getEndpointRegistry().findEndpoints("com.att.aft.DME2CREchoService", "69", "LAB", "BAU");
			
			// The GRM simulator has 2 endpoints, one of them is poison endpoint with invalid lat/long properties.
			// So expected result is to have only 1 endpoint if DME2 filterInvalidEndpoints logic is working as expected
			assertTrue(epList != null && epList.size() == 1);
			assertTrue(epList.get(0).getLatitude() == 37.66);
		}
		finally
		{
			System.clearProperty("AFT_DME2_GRM_URLS");

			try
			{
				mgr.unbindServiceListener(service);
			}
			catch (Exception e)	{}
			
			try
			{
				mgr.getServer().stop();
			}
			catch (Exception e)	{}
		}
	}
}
