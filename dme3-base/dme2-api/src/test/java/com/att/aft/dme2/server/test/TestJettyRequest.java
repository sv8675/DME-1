/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.Locations;


/**
 * The Class TestJettyRequest.
 */
public class TestJettyRequest extends DME2BaseTestCase {

  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static void main( String[] args ) {
    // TODO Auto-generated method stub

  }

  /**
   * The launcher.
   */
  private ServerControllerLauncher launcher = null;

  /**
   * Instantiates a new test jetty request.
   */
  public TestJettyRequest() {
    super();
    // TODO Auto-generated constructor stub
  }


  /**
   * public void testDummy() throws Exception { System.out.println("Success"); }
   *
   * @throws Exception the exception
   */

  @Before
  public void setUp() {
    super.setUp();
    System.setProperty( "AFT_DME2_SVCCONFIG_DIR", RegistryFsSetup.getSrcConfigDir() );
    Locations.BHAM.set();
  }

  /*
   * (non-Javadoc)
   *
   * ()
   */
  @After
  public void tearDown() {
    if ( launcher != null ) {
      launcher.destroy();
    }
    super.tearDown();
  }

  /**
   * Test jetty request.
   */

  @Test
  public void testJettyRequest() {
    String[] bham_1_bau_se_args = { "-jettyport", "4989", "-serverid",
        "JettyServer-Test" };

    launcher = new ServerControllerLauncher( bham_1_bau_se_args );
    launcher.launchWebServer();
    try {
      Thread.sleep( 1000 );
    } catch ( Exception ex ) {
    }

    for ( int i = 0; i < 10; i++ ) {
      JettyClientRequest request = new JettyClientRequest( i,
          "http://localhost:4989" );
      Thread t = new Thread( request );
      t.start();
    }

    try {
      Thread.sleep( 10000 );
    } catch ( Exception ex ) {
    }
    // By default Jetty assigns 10 threads out of which 4 are assigned as
    // socket acceptor. So on sending 10 requests at same time, 4 are bound
    // to fail - Jetty 8
    // By default acceptor threads are blocked so 4 threads are failed  - Jetty 8
    // By default acceptor threads are not blocked so zero threads are failed  - Jetty 9
    String os = System.getProperty( "os.name" ).toLowerCase();
    if ( os != null ) {
      if ( os.indexOf( "nix" ) >= 0 || os.indexOf( "nux" ) >= 0 ) {
        assertEquals( 0, ResultCounter.failed );
      }
      if ( os.indexOf( "win" ) >= 0 ) {
        assertEquals( 0, ResultCounter.failed );
      }
    }
    //assertEquals(4, ResultCounter.failed);
  }

}
