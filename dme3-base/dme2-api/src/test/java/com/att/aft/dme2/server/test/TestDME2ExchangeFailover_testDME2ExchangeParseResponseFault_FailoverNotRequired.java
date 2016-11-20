/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;

public class TestDME2ExchangeFailover_testDME2ExchangeParseResponseFault_FailoverNotRequired extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger(TestDME2ExchangeFailover_testDME2ExchangeParseResponseFault_FailoverNotRequired.class);

  @Before
  public void setUp() {
    super.setUp();
    System.setProperty("AFT_DME2_COLLECT_SERVICE_STATS", "false");
  }

  @After
  public void tearDown() {
    super.tearDown();

    System.clearProperty("AFT_DME2_COLLECT_SERVICE_STATS");
    System.clearProperty("com.sun.management.jmxremote.authenticate");
    System.clearProperty("com.sun.management.jmxremote.ssl");
    System.clearProperty("com.sun.management.jmxremote.port");
  }
  @Test
  @Ignore
  public void testDME2ExchangeParseResponseFault_FailoverNotRequired() throws Exception {
    DME2Manager mgr_1 = null;
    DME2Manager mgr_2 = null;

    cleanPreviousEndpoints("com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault", "1.0.0", "DEV");
    String serviceURI_1 = "/service=com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
    String serviceURI_2 = "/service=com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault/version=1.0.0/envContext=LAB/routeOffer=SECONDARY";
    String clientURI = "http://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault/version=1.0.0/envContext=LAB/partner=FAULT";

    cleanPreviousEndpoints("com.att.aft.dme2.test.TestDME2ExchangeParseResponseFault", "1.0.0", "DEV");
    try {
      Properties props = RegistryFsSetup.init();
      props.put("AFT_DME2_PARSE_FAULT", "true");
      props.put("DME2_LOOKUP_NON_FAILOVER_SC", "false");

      mgr_1 = new DME2Manager("testDME2ExchangeParseResponseFault_1",
          new DME2Configuration("testDME2ExchangeParseResponseFault_1", props));
      mgr_1.bindServiceListener(serviceURI_1, new EchoServlet(serviceURI_1, "Fail_500"));

      mgr_2 = new DME2Manager("testDME2ExchangeParseResponseFault_2",
          new DME2Configuration("testDME2ExchangeParseResponseFault_2", props));
      mgr_2.bindServiceListener(serviceURI_2, new EchoResponseServlet(serviceURI_2, ""));

      Thread.sleep(3000);

      EchoReplyHandler handler = new EchoReplyHandler();
      Request request = new HttpRequest.RequestBuilder(new URI(clientURI))
          .withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL(clientURI)
          .withHeader("testReturnNonFailoverEnabledFault", "true").withHeader("AFT_DME2_REQ_TRACE_ON", "true")
          .build();

      DME2Client client = new DME2Client(mgr_1, request);
      client.setResponseHandlers(handler);
      client.send(new DME2TextPayload("THIS IS A TEST"));

      Thread.sleep(3000);

      String resp = handler.getResponse(120000);
      System.out.printf("Got back response from service: %s", resp);
      assertTrue(resp.contains("UNEXPECTED_ERROR"));

    } finally {
      try {
        mgr_1.unbindServiceListener(serviceURI_1);
      } catch (DME2Exception e) {

      }

      try {
        mgr_2.unbindServiceListener(serviceURI_2);
      } catch (DME2Exception e) {

      }
      try {
        mgr_1.stop();
      } catch (Exception e) {
      }
      try {
        mgr_2.stop();
      } catch (Exception e) {
      }
    }
  }
}
