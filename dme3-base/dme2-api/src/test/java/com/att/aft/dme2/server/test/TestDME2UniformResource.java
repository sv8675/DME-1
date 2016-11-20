/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.test.DME2BaseTestCase;

public class TestDME2UniformResource extends DME2BaseTestCase {
  DME2Configuration config = new DME2Configuration();

  /*
   * (non-Javadoc)
   *
   * ()
   */
  @Before
  public void setUp() {
    // set required system properties...
    System.setProperty( "AFT_DME2_SVCCONFIG_DIR", RegistryFsSetup.getSrcConfigDir() );

    TestDME2UniformResource.class.getClassLoader().setClassAssertionStatus(
        DmeUniformResource.class.getName(), true );
  }

  /*
   * (non-Javadoc)
   *
   * ()
   */
  @After
  public void tearDown() {
    DmeUniformResource.class.getClassLoader().clearAssertionStatus();
  }

  /**
   * Test direct uri.
   *
   * @throws Exception the exception
   */

  @Ignore
  @Test
  public void directURI() throws Exception {
    String directStr = "";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( directStr ) );
    uniformResource.assertValid();
  }

  /**
   * @throws Exception
   */

  @Test
  public void testNaturalURI() throws Exception {
    String directStr =
        "http://CustomerSupport.cust.att.com/subContext/?version=1.0.0&envContext=UAT&routeOffer=APPLE_SE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( directStr ) );
    uniformResource.assertValid();
    assertEquals( uniformResource.getService(), "com.att.cust.CustomerSupport/subContext/" );
    assertEquals( uniformResource.getVersion(), "1.0.0" );
    assertEquals( uniformResource.getEnvContext(), "UAT" );
    assertEquals( uniformResource.getRouteOffer(), "APPLE_SE" );
  }

  /**
   * @throws Exception
   */


  @Test
  public void testNaturalURIWithSearch() throws Exception {
    String directStr =
        "http://CustomerSupport.cust.att.com/subContext/?version=1.0.0&envContext=UAT&partner=TEST&stickySelectorKey=ABC&dataContext=404";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( directStr ) );
    uniformResource.assertValid();
    assertEquals( uniformResource.getService(), "com.att.cust.CustomerSupport/subContext/" );
    assertEquals( uniformResource.getVersion(), "1.0.0" );
    assertEquals( uniformResource.getEnvContext(), "UAT" );
    assertNull( uniformResource.getRouteOffer() );
    assertEquals( uniformResource.getPartner(), "TEST" );
    assertEquals( uniformResource.getStickySelectorKey(), "ABC" );
    assertEquals( uniformResource.getDataContext(), "404" );

  }

  /**
   * @throws Exception
   */

  @Test
  public void testSearchableURIWithRestService() throws Exception {
    String directStr =
        "http://DME2SEARCH/service=com.att.cust.CustomerSupport/subContext/version=1.0.0/envContext=UAT/partner=TEST/stickySelectorKey=ABC/dataContext=404";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( directStr ) );
    uniformResource.assertValid();
    assertEquals( uniformResource.getService(), "com.att.cust.CustomerSupport/subContext" );
    assertEquals( uniformResource.getVersion(), "1.0.0" );
    assertEquals( uniformResource.getEnvContext(), "UAT" );
    assertNull( uniformResource.getRouteOffer() );
    assertEquals( uniformResource.getPartner(), "TEST" );
    assertEquals( uniformResource.getStickySelectorKey(), "ABC" );
    assertEquals( uniformResource.getDataContext(), "404" );
  }

  /**
   * @throws Exception
   */

  @Test
  public void testNaturalURIWithNoPartnerRouteOffer() throws Exception {
    String directStr = "http://CustomerSupport.cust.att.com/subContext/?version=1.0.0&envContext=UAT";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( directStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should've failed assert" );
    } catch ( Exception e ) {
      e.printStackTrace();
      assertTrue( "Got " + e.getMessage() + " when expecting AFT-DME2-9704", e.getMessage().contains( "AFT-DME2-9704" ) );
    }
  }

  /**
   * Test invalid resolve uri.
   *
   * @throws Exception the exception
   */

  @Test
  public void testInvalidResolveURI_MissingRouteOfferValue() throws Exception {
    String resolveStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( resolveStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null routeOffer." );
    } catch ( DME2Exception e ) {
    }

  }

  @Test
  public void testInvalidResolveURI_MissingEnvContext() throws Exception {
    String resolveStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/routeOffer=APPLE_SE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( resolveStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null envContext." );
    } catch ( DME2Exception e ) {
    }
  }

  @Test
  public void testInvalidResolveURI_MissingVersion() throws Exception {
    String resolveStr = "http://DME2RESOLVE/service=MyService/version=/envContext=PROD/routeOffer=";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( resolveStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null version." );
    } catch ( DME2Exception e ) {
    }
  }

  @Test
  public void testInvalidResolveURI_MissingService() throws Exception {
    String resolveStr = "http://DME2RESOLVE/version=1.0.0/envContext=PROD/routeOffer=APPLE_SE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( resolveStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null service." );
    } catch ( DME2Exception e ) {
    }
  }

  @Test
  public void testInvalidResolveURI_MissingHostname() throws Exception {
    String resolveStr = "http://service=MyService/version=1.0.0/envContext=PROD/routeOffer=";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( resolveStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. No type provided." );
    } catch ( DME2Exception e ) {
    }
  }

  /**
   * Test invalid search uri.
   *
   * @throws Exception the exception
   */

  @Test
  public void testInvalidSearchURI_MissingPartner() throws Exception {
    String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null partner." );
    } catch ( DME2Exception e ) {
    }
  }

  @Test
  public void testInvalidSearchURI_MissingService() throws Exception {
    String searchStr = "http://DME2SEARCH/service=/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null service" );
    } catch ( DME2Exception e ) {
    }
  }

  @Test
  public void testInvalidSearchURI_MissingVersion() throws Exception {
    String searchStr = "http://DME2SEARCH/service=MyService/envContext=PROD/dataContext=205977/partner=APPLE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null version" );
    } catch ( DME2Exception e ) {
    }
  }

  @Test
  public void testInvalidSearchURI_MissingEnvContext() throws Exception {
    String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/dataContext=205977/partner=APPLE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. null envContext" );
    } catch ( DME2Exception e ) {
    }

  }

  @Test
  public void testInvalidSearchURI_MissingDataContextOK() throws Exception {
    String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/partner=APPLE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    try {
      uniformResource.assertValid();
    } catch ( DME2Exception e ) {
      fail( "Should NOT have failed. null dataContext" );
    }
  }

  /**
   * Test invalid uri.
   *
   * @throws Exception the exception
   */

  @Test
  public void testInvalidURI() throws Exception {
    String uriStr = "ftp://cbs.it.att.com/service=MyService/version=1.0/envContext=PROD,routeOffer=APPLE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( uriStr ) );
    try {
      uniformResource.assertValid();
      fail( "Should have failed. Unsupported protocol and type." );
    } catch ( DME2Exception e ) {
    }
  }

  /**
   * Test resolve uri.
   *
   * @throws Exception the exception
   */

  @Test
  public void testResolveURI() throws Exception {
    String resolveStr = "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( resolveStr ) );
    uniformResource.assertValid();
  }

  /**
   * Test search uri.
   *
   * @throws Exception the exception
   */


  @Test
  public void testSearchURI() throws Exception {
    String searchStr =
        "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977/partner=APPLE";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    uniformResource.assertValid();
  }


  @Test
  public void testSearchURIByFieldPositionWithOverride() throws Exception {
    System.setProperty( "DME2_URI_FIELD_WITH_PATH_SEP", "/version=" );
    try {
      // envContext has ending slash in URI
      String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/";
      DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
      assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );

      // envContext has no ending slash in URI
      searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD";
      uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
      assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );

      // routeOffer has null value, ending with version=value/
      searchStr = "http://DME2RESOLVE/envContext=PROD/routeOffer=/version=1.2.0/service=com.att.aft.MyService/abc/";
      uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
      assertTrue( uniformResource.getRouteOffer() == null );
      System.out.println( uniformResource.getService() );
      assertTrue( uniformResource.getService().equals( "com.att.aft.MyService/abc/" ) );

    } finally {
      System.clearProperty( "DME2_URI_FIELD_WITH_PATH_SEP" );
    }
  }


  @Test
  public void testSearchURIByFieldPosition() throws Exception {
    // envContext has ending slash in URI
    String searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );

    // serviceName at last with additional slash
    searchStr = "http://DME2SEARCH/version=/envContext=PROD/stickySelectorKey=SSK1/service=com.att.aft.MyService/";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );
    System.out.println( uniformResource.getService() );
    assertTrue( uniformResource.getService().equals( "com.att.aft.MyService" ) );

    // envContext has no ending slash in URI
    searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );

    // routeOffer has null value, ending with version=value/
    searchStr = "http://DME2RESOLVE/service=MyService/envContext=PROD/routeOffer=/version=1.2.0/";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getRouteOffer() == null );
    assertTrue( uniformResource.getVersion().equals( "1.2.0" ) );

    // routeOffer has null value, ending with version=value
    searchStr = "http://DME2RESOLVE/service=MyService/envContext=PROD/routeOffer=/version=1.2.0";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getRouteOffer() == null );
    assertTrue( uniformResource.getVersion().equals( "1.2.0" ) );

    // routeOffer has null value, service at end of URI
    searchStr = "http://DME2RESOLVE/envContext=PROD/version=1.2.0///service=com.att.aft.CR/abc/";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getRouteOffer() == null );
    assertTrue( uniformResource.getVersion().equals( "1.2.0" ) );
    assertTrue( uniformResource.getService().equals( "com.att.aft.CR/abc/" ) );

    // routeOffer has null value, service name has // at end
    searchStr = "http://DME2RESOLVE/envContext=PROD///version=1.2.0///service=com.att.aft.CR/abc//";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );
    assertTrue( uniformResource.getVersion().equals( "1.2.0" ) );
    assertTrue( uniformResource.getService().equals( "com.att.aft.CR/abc//" ) );

    // No inputs provided with DME2URI
    searchStr = "http://DME2RESOLVE/";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext() == null );
    assertTrue( uniformResource.getVersion() == null );
    assertTrue( uniformResource.getService() == null );

    // dataContext
    searchStr = "http://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD/dataContext=205977//";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );
    assertTrue( uniformResource.getDataContext().equals( "205977" ) );

    // stickyKey
    searchStr = "http://DME2SEARCH/service=MyService/version=/envContext=PROD/stickySelectorKey=SSK1";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );
    assertTrue( uniformResource.getStickySelectorKey().equals( "SSK1" ) );


    // service and subContext
    searchStr =
        "http://DME2SEARCH/service=com.att.aft.MyService/restful/abc/version=/envContext=PROD/stickySelectorKey=SSK1/subContext=/abc/def";
    uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );
    assertTrue( uniformResource.getStickySelectorKey().equals( "SSK1" ) );
    System.out.println( uniformResource.getSubContext() );
    assertTrue( uniformResource.getSubContext().equals( "/abc/def" ) );
    assertTrue( uniformResource.getService().equals( "com.att.aft.MyService/restful/abc" ) );
  }


  // This test is not present in DME2 and uniformresource has no "preferred version".
  @Test
  @Ignore
  public void testPreferredVersion() throws Exception {
    // routeOffer has null value, service name has // at end
    String searchStr = "http://DME2RESOLVE/envContext=PROD///version=1.2.3///service=com.att.aft.CR/abc//";
    DmeUniformResource uniformResource = new DmeUniformResource( config, new URI( searchStr ) );
    uniformResource.setPreferredVersion("4.5.6");
    assertTrue( uniformResource.getEnvContext().equals( "PROD" ) );
    assertTrue( uniformResource.getVersion().equals( "4.5.6" ) );
    assertTrue( uniformResource.getService().equals( "com.att.aft.CR/abc//" ) );
  }
}
