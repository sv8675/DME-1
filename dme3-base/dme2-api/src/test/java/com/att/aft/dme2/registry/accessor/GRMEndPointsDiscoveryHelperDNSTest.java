/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.registry.accessor;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.util.grm.DNSIPResolverTest;
import com.att.aft.dme2.server.test.TestConstants;

@Ignore
public class GRMEndPointsDiscoveryHelperDNSTest  {

	private String protocol = "http";
	private String port = "9127";
	private String path =  "/GRMLWPService/v1";
	private String dnsName = TestConstants.GRM_DNS_SERVER;

	private GRMEndPointsDiscoveryHelperDNS grmEndPointsDiscoveryHelperDNS;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	// This is an integration test it needs DNS server to be on network and have proper mappings to work
	@Ignore
	@Test
	public void testGRMEndPointsDiscoveryHelperDNS() {
		grmEndPointsDiscoveryHelperDNS = new GRMEndPointsDiscoveryHelperDNS(dnsName, protocol, port, path);
		List<String> seedEndpoints = grmEndPointsDiscoveryHelperDNS.getGRMEndpoints();
		assertNotNull(seedEndpoints);
		assertNotEquals(0, seedEndpoints.size());
		for(String url : seedEndpoints) {
			assertTrue(url.contains(path));
		}

		Assert.assertEquals( DNSIPResolverTest.IP_DEV.length, seedEndpoints.size() );
		for (String ip : DNSIPResolverTest.IP_DEV) {
			boolean found = false;
			for (String url : seedEndpoints) {
				if (url.contains(ip)) {
					found = true;
					break;
				}
			}
			if (!found) {
				fail("could not found this ip in results: " + ip);
			}
		}
	}
}
