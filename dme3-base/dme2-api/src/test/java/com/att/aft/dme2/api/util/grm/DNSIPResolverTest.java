/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util.grm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.server.test.TestConstants;


public class DNSIPResolverTest {
  /**
   * define these in your DNS or modify them to match one DNS multiple IP's
   */
  //
  public static final String DNS_DEV_GRM = "";
  public static final String DNS_DEV_LWP = TestConstants.GRM_DNS_SERVER;
  public static final String DNS_TEST = "";
  public static final String DNS_PROD = "";
  public static final String DNS_NONPROD = "";
  public static final String DNS_LAB_DEV_GRM = "";
  public static final String DNS_LAB_DEV_LWP = "";
  public static final String DNS_LAB_TEST_GRM = "";
  public static final String DNS_LAB_TEST_LWP = "";
  public static final String DNS_TEST_GRM = "";
  public static final String DNS_TEST_LWP = "";
  public static final String DNS_NONPROD_GRM = "";
  public static final String DNS_NONPROD_LWP = "";
  public static final String DNS_PROD_GRM = "";
  public static final String DNS_PROD_LWP = "";
  // keep IPS in sorted order
  // keep IPS in sorted order
  public static final String[] IP_LAB_DEV_GRM = { "" };
  public static final String[] IP_LAB_DEV_LWP = { "" };
  public static final String[] IP_LAB_TEST_GRM = { "", "" };
  public static final String[] IP_LAB_TEST_LWP = { "", "" };
  // @TODO fill rest of the domain names as they are defined
  public static final String[] IP_TEST_GRM = {};
  public static final String[] IP_TEST_LWP = {};
  public static final String[] IP_NONPROD_GRM = {};
  public static final String[] IP_NONPROD_LWP = {};
  public static final String[] IP_PROD_GRM = {};
  public static final String[] IP_PROD_LWP = {};
  public static final String[] IP_DEV = { "", ""}; 

  public static final String[] IP_TEST = {}; // @TODO put IPs here to be tests regularly
  public static final String[] IP_PROD = {}; // @TODO put IPs here to be tests regularly
  public static final String[] IP_NONPROD = {}; // @TODO put IPs here to be tests regularly
  public static Map<String, String[]> dnsToIp = new HashMap<String, String[]>();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
//		dnsToIp.put(DNS_DEV_GRM, IP_DEV);
    dnsToIp.put( DNS_DEV_LWP, IP_DEV );
    dnsToIp.put( DNS_TEST, IP_TEST );
    dnsToIp.put( DNS_PROD, IP_PROD );
    dnsToIp.put( DNS_NONPROD, IP_NONPROD );
    dnsToIp.put( DNS_LAB_DEV_GRM, IP_LAB_DEV_GRM );
    dnsToIp.put( DNS_LAB_DEV_LWP, IP_LAB_DEV_LWP );
    dnsToIp.put( DNS_LAB_TEST_GRM, IP_LAB_TEST_GRM );
    dnsToIp.put( DNS_LAB_TEST_LWP, IP_LAB_TEST_LWP );
    dnsToIp.put( DNS_TEST_GRM, IP_TEST_GRM );
    dnsToIp.put( DNS_TEST_LWP, IP_TEST_LWP );
    dnsToIp.put( DNS_NONPROD_GRM, IP_NONPROD_GRM );
    dnsToIp.put( DNS_NONPROD_LWP, IP_NONPROD_LWP );
    dnsToIp.put( DNS_PROD_GRM, IP_PROD_GRM );
    dnsToIp.put( DNS_PROD_LWP, IP_PROD_LWP );
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
  public void test1_IpToString() {
    byte[] ip4 = { 101, 102, 103, 104 };
    byte[] ip6 = { 101, 102, 103, 104, 105, 106 };
    String stip4 = DNSIPResolver.ipToString( ip4 );
    String stip6 = DNSIPResolver.ipToString( ip6 );
    assertEquals( "101.102.103.104", stip4 );
    assertEquals( "101.102.103.104.105.106", stip6 );
  }

  /**
   * to run this test you should modify your local DNS host to return this values for the given address:
   *
   * @TODO in future we will add test for all Seed GRM hosts! so we can make sure all of those hosts are defined
   * properly!
   */
  @Test
  public void test2_TestAllSeedIpsMapped() {
    for ( String dnsName : dnsToIp.keySet() ) {
      testIPsForName( dnsName, dnsToIp.get( dnsName ) );
    }
  }

  private void testIPsForName( String dnsName, String[] expected ) {
    if ( expected.length == 0 ) {
      return;
    }
    try {
      List<String> result = DNSIPResolver.getListIPForName( dnsName );
      assertNotNull( result );
      assertEquals( expected.length, result.size() );
      Collections.sort( result );
      System.out.println( "Ips for name = " + dnsName + " " + result.toString() );
      for ( int i = 0; i < expected.length; i++ ) {
        assertEquals( expected[i], result.get( i ) );
      }
    } catch ( UnknownHostException e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    }
  }

}
