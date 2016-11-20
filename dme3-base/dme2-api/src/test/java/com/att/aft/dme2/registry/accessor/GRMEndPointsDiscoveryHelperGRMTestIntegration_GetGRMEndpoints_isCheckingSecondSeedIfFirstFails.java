/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.registry.accessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.test.DME2BaseTestCase2;
import com.att.aft.dme2.util.DME2ParameterNames;

public class GRMEndPointsDiscoveryHelperGRMTestIntegration_GetGRMEndpoints_isCheckingSecondSeedIfFirstFails extends
    DME2BaseTestCase2 {
  private static final Logger logger = LoggerFactory.getLogger( GRMEndPointsDiscoveryHelperGRMTestIntegration_GetGRMEndpoints_isCheckingSecondSeedIfFirstFails.class );

  private String dnsName;
  private String serviceName;
  private String environment;
  private String protocol;
  private String port;
  private String version;
  private String path;

  private SecurityContext sc = null;
  private GRMEndPointsCache cache = null;
  private DME2Configuration config;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    setupGRMDNSDiscovery();

    dnsName = System.getProperty( DME2ParameterNames.GRM_DNS_BOOTSTRAP); 
    serviceName = "";
    environment = System.getProperty(DME2ParameterNames.GRM_ENVIRONMENT );
    protocol = System.getProperty(DME2ParameterNames.GRM_SERVER_PROTOCOL, "http");
    port = System.getProperty(DME2ParameterNames.GRM_SERVER_PORT, "9127");
    version = System.getProperty(DME2ParameterNames.GRM_SERVICE_VERSION);
    path = System.getProperty(DME2ParameterNames.GRM_SERVER_PATH, "/GRMLWPService/v1");
    config = new DME2Configuration(  );

    try {
      sc = SecurityContext.create(config);
    } catch (DME2Exception e) {
      fail("Can't create security context");
    }
    cache = GRMEndPointsCache.getInstance(config);
  }

  @After
  public void tearDown() throws Exception {
    cache.clear();
    super.tearDown();
  }

  @Test
  public void testIntegration_GetGRMEndpoints_isCheckingSecondSeedIfFirstFails() throws DME2Exception {
    final String badGRMSeedEndping = "http://localhost:9271/GRMLWPService/v1";
    // put them in cache after a bad EndPoint
    cache.addEndpointURL(badGRMSeedEndping);
    cache.addAllAddressList(getSeedEndpoint());
    // make sure first item returned is bad endpoint!
    String firstCachedEndpoints = cache.getGRMEndpoints().iterator().next();
    assertEquals(badGRMSeedEndping, firstCachedEndpoints);
    // make an GRMServiceAccessor from it
    BaseAccessor grmServiceAccessor = GRMAccessorFactory.getGrmAccessorHandlerInstance( config,
        SecurityContext.create( config ) );// new SoapGRMAccessor(sc, cache);
    // make GRMEndPointsDiscoveryHelperGRM
    GRMEndPointsDiscoveryHelperGRM grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(environment, protocol, serviceName, version, grmServiceAccessor, config);
    List<String> grmEndpointsInGRM = grmEndPointsDiscoveryHelperGRM.getGRMEndpoints();
    assertNotNull(grmEndpointsInGRM);
    assertNotEquals(0, grmEndpointsInGRM.size());
  }

  private List<String> getSeedEndpoint() {
    // get GRM seeds from DNS
    GRMEndPointsDiscoveryHelperDNS grmEndPointsDiscoveryHelperDNS = new GRMEndPointsDiscoveryHelperDNS(dnsName, protocol, port, path);
    List<String> seedEndpoints = grmEndPointsDiscoveryHelperDNS.getGRMEndpoints();
    return seedEndpoints;
  }
}
