/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.registry.accessor;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2ParameterNames;

public class GRMEndPointsDiscoveryDNSTest {
  String dnsName = System.getProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP );
  String serviceName = System.getProperty( DME2ParameterNames.GRM_SERVICE_NAME, "GRMLWPService" );
  String environment = System.getProperty( DME2ParameterNames.GRM_ENVIRONMENT, "lab" );
      // valid values: dev, test, prod, nonprod

  GRMEndPointsDiscoveryDNS discovery;

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

  @Test
  public void testGetGRMEndpoints() throws DME2Exception {
    DME2Configuration config = new DME2Configuration();
    SecurityContext ctx = SecurityContext.create( config );
    discovery = GRMEndPointsDiscoveryDNS.getInstance( dnsName, serviceName, environment, ctx, config, null );

    List<String> grmEndpoints = discovery.getGRMEndpoints();
    assertNotNull( grmEndpoints );
    assertNotEquals( 0, grmEndpoints.size() );
  }

}
