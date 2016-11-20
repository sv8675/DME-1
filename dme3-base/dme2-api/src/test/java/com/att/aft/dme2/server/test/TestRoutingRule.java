/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.util.List;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.DME2NullServlet;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.helper.AvailableEndpoints;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.test.Locations;

import junit.framework.TestCase;

/**
 * The Class TestRoutingRule.
 */
public class TestRoutingRule extends TestCase {

	/** The manager. */
	DME2Manager manager = null;

	/** The myservice_route offer1. */
	String myservice_routeOffer1 = null;

	/** The myservice_route offer2. */
	String myservice_routeOffer2 = null;

	/** The myservice_route offer3. */
	String myservice_routeOffer3 = null;

	/**
	 * Instantiates a new test routing rule.
	 * 
	 * @param name
	 *            the name
	 */
	public TestRoutingRule(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		Properties props = RegistryFsSetup.init();
    //props.setProperty( DME2Constants.DME2_EP_REGISTRY_CLASS, DME2Constants.DME2FS );
    props.setProperty( "AFT_DME2_EP_REGISTRY_FS_DIR" ,  System.getProperty( "user.dir" ) + "/src/test/etc/svc_config" );

		DME2Configuration config = new DME2Configuration("TestRoutingRule", props);			

		manager = new DME2Manager("TestRoutingRule", config);
		
//		manager = new DME2Manager("TestRoutingRule", RegistryFsSetup.init());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * <ul>
	 * <li> 45.	Endpoint repository access will be authenticated by the allowed IP address list configured by the service providers</li>
	 * <li> 46.	DME2 shall allow SOA Cloud infrastructure to execute health check for an endpoint without invoking application stack</li>
	 * <li> 47.	DME2 provides a health check operation to test the health of application server that does not involve service provider implementation.</li>
	 * <li> 48.	DME2 shall associate the endpoints registered to be part of SOA Cloud components</li>
	 * </ul>
	 *
	 * @throws Exception
	 */
	public void testRouteRule() throws Exception {
		Locations.BHAM.set();

		try{
			myservice_routeOffer1 = TestConstants.myservice_routeOffer1;
			manager.bindServiceListener(myservice_routeOffer1, new EchoServlet(
					myservice_routeOffer1, "routeOffer1"));
	
			myservice_routeOffer2 = TestConstants.myservice_routeOffer2;
			manager.bindServiceListener(myservice_routeOffer2, new EchoServlet(
					myservice_routeOffer2, "routeOffer2"));
	
			myservice_routeOffer3 = TestConstants.myservice_routeOffer3;
			manager.bindServiceListener(myservice_routeOffer3, new EchoServlet(
					myservice_routeOffer3, "routeOffer3"));
	
			DME2RouteInfo data = manager.getRouteInfo("MyService", "1.0.0", "PROD");
	
			String[] envs = new String[] { "PROD" };
			String[] partners = new String[] { "APPLE" };
			String[] keyValues = new String[] { "205977" };
	
			for (String env : envs) {
				for (String partner : partners) {
					for (String key : keyValues) {
						List<DME2RouteOffer> routeOffers = data.getRouteOffers(env, partner, key,null);
						if (routeOffers == null) {
							fail("no route offers found for key=" + key);
						} else {
							for (DME2RouteOffer routeOffer : routeOffers) {
								DME2Endpoint[][] eps = AvailableEndpoints.find(routeOffer, true, manager);
								int counter = 0;
								String expected = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
								for (DME2Endpoint[] band : eps) {
									for (DME2Endpoint ehep : band) {
										assertEquals(String.format("Did not find expected string [%s] in the endpoint path [%s]",expected, ehep.getPath()),expected, ehep.getPath());
										counter++;
									}
								}
							}
						}
					}
				}
			}
		}
		finally {
			try {
				manager.unbindServiceListener(myservice_routeOffer1);
			}
			catch (Exception e) {
			}
			try {
				manager.unbindServiceListener(myservice_routeOffer2);
			}
			catch (Exception e) {
			}
			try {
				manager.unbindServiceListener(myservice_routeOffer3);
			}
			catch (Exception e) {
			}
			try {
				manager.shutdown();
			}
			catch (Exception e) {
			}
		}
	}
	
	//TEST_ALL_INACTIVE_OFFERS

	public void testRouteRuleAllOffersInactive() throws Exception {
		Locations.BHAM.set();

		myservice_routeOffer1 = TestConstants.myservice_routeOffer1;
		myservice_routeOffer2 = TestConstants.myservice_routeOffer2;
		myservice_routeOffer3 = TestConstants.myservice_routeOffer3;

		manager.bindServiceListener(myservice_routeOffer1, new EchoServlet(myservice_routeOffer1, "routeOffer1"));

		manager.bindServiceListener(myservice_routeOffer2, new EchoServlet(myservice_routeOffer2, "routeOffer2"));

		manager.bindServiceListener(myservice_routeOffer3, new EchoServlet(myservice_routeOffer3, "routeOffer3"));

		DME2RouteInfo data = manager.getRouteInfo("MyService", "1.0.0", "PROD");

		// String[] envs = new String[] { "PROD", "UAT", "TEST" };
		// String[] partners = new String[] { "APPLE", "WALMART", "ESTORE" };
		// String[] keyValues = new String[] { "205977", "205988", "404707",
		// "988378", "2059779224", "20597" };

		String[] envs = new String[] { "PROD" };
		String[] partners = new String[] { "TEST_ALL_INACTIVE_OFFERS" };
		String[] keyValues = new String[] { "205977" };

		for (String env : envs) {
			for (String partner : partners) {
				for (String key : keyValues) {
					try {
						List<DME2RouteOffer> routeOffers = data.getRouteOffers(env, partner, key,null);
						fail("Got routeOffers back when all offers were set to null in routeInfo.xml");
					}
					catch (DME2Exception e) {
						assertTrue(String.format("Did not find expected string [AFT-DME2-0103} in exception message ", e.getMessage()),e.getMessage().indexOf("AFT-DME2-0103") > -1);
					}
					//fail("Got routeOffers back when all offers were set to null in routeInfo.xml");
				}
			}
		}

	}
	
	
	/**
	 * Use DME2NullServlet rather than EchoServlet.
	 * 49.	DME2 will associate the published service endpoints with the container endpoint and node instance of Cloud components, in appropriate conditions
	 *
	 * @throws Exception
	 */
	public void testRouteRuleNoBusinessLogic() throws Exception {
		try{
			Locations.BHAM.set();
			try {
				Properties props = new Properties();
				props.setProperty("DME2_EP_REGISTRY_CLASS", "com.att.aft.dme2.api.util.DME2EndpointRegistryFS");
	
				DME2Configuration config = new DME2Configuration("testClientRequest", props);			
	
				manager = new DME2Manager("testClientRequest", config);
			} catch (Exception ex) {
			}
	
			myservice_routeOffer1 = TestConstants.myservice_routeOffer1;
			manager.bindServiceListener(myservice_routeOffer1, new DME2NullServlet(
					myservice_routeOffer1));
	
			myservice_routeOffer2 = TestConstants.myservice_routeOffer2;
			manager.bindServiceListener(myservice_routeOffer2, new DME2NullServlet(
					myservice_routeOffer2));
	
			myservice_routeOffer3 = TestConstants.myservice_routeOffer3;
			manager.bindServiceListener(myservice_routeOffer3, new DME2NullServlet(
					myservice_routeOffer3));
	
			DME2RouteInfo data = manager.getRouteInfo("MyService", "1.0.0", "PROD");
	
			// String[] envs = new String[] { "PROD", "UAT", "TEST" };
			// String[] partners = new String[] { "APPLE", "WALMART", "ESTORE" };
			// String[] keyValues = new String[] { "205977", "205988", "404707",
			// "988378", "2059779224", "20597" };
	
			String[] envs = new String[] { "PROD" };
			String[] partners = new String[] { "APPLE" };
			String[] keyValues = new String[] { "205977" };
	
			for (String env : envs) {
				for (String partner : partners) {
					for (String key : keyValues) {
						try {
							List<DME2RouteOffer> routeOffers = data.getRouteOffers(
									env, partner, key, null);
							if (routeOffers == null) {
								fail("no route offers found for key=" + key);
							} else {
								for (DME2RouteOffer routeOffer : routeOffers) {
									DME2Endpoint[][] eps = AvailableEndpoints.find(routeOffer, true, manager);
									int counter = 0;
									String expected = "service=MyService/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
									for (DME2Endpoint[] band : eps) {
										for (DME2Endpoint ehep : band) {
											assertEquals(String.format("Did not find expected string [%s] in the endpoint path [%s]",expected, ehep.getPath()),expected, ehep.getPath());
										}
										counter++;
									}
								}
							}
						}catch (DME2Exception e) {
							fail(e.getMessage());
						}
					}
				}
			}
		}finally {
			try {
				manager.unbindServiceListener(myservice_routeOffer1);
			}
			catch (Exception e) {
			}
			try {
				manager.unbindServiceListener(myservice_routeOffer2);
			}
			catch (Exception e) {
			}
			try {
				manager.unbindServiceListener(myservice_routeOffer3);
			}
			catch (Exception e) {
			}
			try {
				manager.shutdown();
			}
			catch (Exception e) {
			}
		}
	}//method
}

