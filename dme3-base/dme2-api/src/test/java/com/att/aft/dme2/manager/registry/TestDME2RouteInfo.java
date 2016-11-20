/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.manager.registry;

import java.io.File;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.test.Locations;
import com.att.aft.dme2.types.RouteInfo;

import junit.framework.TestCase;

//import com.att.scld.*;
//import com.att.scld.grm_schemas.*;

/**
 * The Class TestDME2RouteInfo.
 */
public class TestDME2RouteInfo extends TestCase {

	/** The r info. */
	private DME2RouteInfo rInfo = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		Locations.BHAM.set();

		File file = new File( RegistryFsSetup.getSrcConfigDir(), "MyServiceV1.xml");

		if (!file.exists()) {
			throw new Exception(file.getAbsolutePath() + " is not valid.");
		}
		String managerName = RandomStringUtils.randomAlphanumeric( 25 );
		DME2Configuration config = new DME2Configuration( managerName, new Properties(  ) );
		DME2Manager manager = new DME2Manager(managerName, config );
		DME2EndpointRegistryFS loader = new DME2EndpointRegistryFS( config, managerName );
		JAXBElement<RouteInfo> element = loader.readRouteInfo( file );
		rInfo = new DME2RouteInfo( element.getValue(), loader.getConfig() );
		//		rInfo = loader.load(DME2Manager.getDefaultInstance());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		System.clearProperty("AFT_ENVIRONMENT");
		System.clearProperty("AFT_LATITUDE");
		System.clearProperty("AFT_LONGITUDE");
		System.clearProperty("DME2.DEBUG");
		System.clearProperty("platform");
		System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
	}

	/**
	 * Test low high.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testKeyLowMidHigh() throws Exception {

		// low
		List<DME2RouteOffer> roList = rInfo.getRouteOffers("PROD", "TEST", "205444", null);
		assertEquals(2, roList.size());
		assertEquals("BAU.E.SECONDARY_E", roList.get(0).getFqName());
		assertEquals("BAU.E.PRIMARY_E", roList.get(1).getFqName());

		// mid
		roList = rInfo.getRouteOffers("PROD", "TEST", "205500", null);
		assertEquals(2, roList.size());
		assertEquals("BAU.E.SECONDARY_E", roList.get(0).getFqName());
		assertEquals("BAU.E.PRIMARY_E", roList.get(1).getFqName());

		// high
		roList = rInfo.getRouteOffers("PROD", "TEST", "205555", null);
		assertEquals(2, roList.size());
		assertEquals("BAU.E.SECONDARY_E", roList.get(0).getFqName());
		assertEquals("BAU.E.PRIMARY_E", roList.get(1).getFqName());

		// low
		roList = rInfo.getRouteOffers("PROD", "TEST", "205977", null);
		assertEquals(1, roList.size());
		assertEquals("BAU.SE.BAU_SE", roList.get(0).getFqName());

		// mid
		roList = rInfo.getRouteOffers("PROD", "TEST", "205988", null);
		assertEquals(1, roList.size());
		assertEquals("BAU.SE.BAU_SE", roList.get(0).getFqName());

		// high
		roList = rInfo.getRouteOffers("PROD", "TEST", "205999", null);
		assertEquals(1, roList.size());
		assertEquals("BAU.SE.BAU_SE", roList.get(0).getFqName());

		// low/mid/high
		roList = rInfo.getRouteOffers("PROD", "JUNIT", "404707", null);
		assertEquals(1, roList.size());
		assertEquals("BAU.ATL.BAU_ATL", roList.get(0).getFqName());

	}

	/**
	 * Test negative low high.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testKeyNegative() throws Exception {
		String[] keyArray = { "205443", "205556", "205976", "206000", "404708" };
		for (String key : keyArray) {
			try {
				rInfo.getRouteOffers("PROD", "TEST", key, null);
				fail("Should have failed. key=" + key);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue( e.getMessage().indexOf( "[AFT-DME2-0101]" ) > -1 );
			} 
		}
	}

	/**
	 * Test null values.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testNullValues() throws Exception {
		try {
			rInfo.getRouteOffers(null, null, null,null);
			fail("Should have failed. null key.");
		} catch (NullPointerException e) {
		}
	}

	/**
	 * Test route info.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void testRouteInfo() throws Exception {
		assertEquals("PROD", rInfo.getEnvContext());
		assertEquals("/x/y/z", rInfo.getDataPartitionKeyPath());
		assertEquals("MyService", rInfo.getServiceName());
		assertEquals("1.0.0", rInfo.getServiceVersion());

		List<DME2RouteOffer> roList = rInfo.getRouteOffers("PROD", "TEST", "205977", null);
		assertEquals(1, roList.size());
		assertEquals("BAU.SE.BAU_SE", roList.get(0).getFqName());

		roList = rInfo.getRouteOffers("PROD", "JUNIT", "404707", null);
		assertEquals(1, roList.size());
		assertEquals("BAU.ATL.BAU_ATL", roList.get(0).getFqName());
	}

	/**
	 * CSSA-11267 - service disobeying sticky rules
	 */

	@Test
	public void testRouteInfoWithNoStickySelector() throws Exception {

		Locations.BHAM.set();

		File file = new File(RegistryFsSetup.getSrcConfigDir(), "MyServiceV9.xml");
		if (!file.exists()) {
			System.out.println(file.getAbsolutePath() + " is not valid.");
		}
		String managerName = RandomStringUtils.randomAlphanumeric( 25 );
		DME2Configuration config = new DME2Configuration( managerName, new Properties(  ) );
		DME2Manager manager = new DME2Manager(managerName, config );
		DME2EndpointRegistryFS loader = new DME2EndpointRegistryFS( config, managerName );
		JAXBElement<RouteInfo> element = loader.readRouteInfo( file );

		DME2RouteInfo rInfo1 = new DME2RouteInfo( element.getValue(), loader.getConfig() );

		List<DME2RouteOffer> roList = rInfo1.getRouteOffers("PROD", "TEST", "", "P26");
		System.out.println(roList.get(0));
		// When stickySelector is set, the returned route offer list should not have 
		// routeOffer with ROUTE_WITH_NO_STICKY
		System.out.println(roList.get(1));
		System.out.println(roList.get(0).getSearchFilter());

		String sf = roList.get(0).getSearchFilter();
		assertTrue(!sf.contains("ROUTE_WITH_NO_STICKY"));
		assertEquals(2, roList.size());

		roList = rInfo1.getRouteOffers("PROD", "TEST", "", null);
		System.out.println(roList.get(1));
		System.out.println(roList.get(0).getSearchFilter());
		sf = roList.get(0).getSearchFilter();
		assertTrue(sf.contains("ROUTE_WITH_NO_STICKY"));
		assertEquals(2, roList.size());
	}
}
