/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
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
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Constants;

/**
 * Moved out of TestGrm on the hunch that being in its own test class would help it to pass
 */

@Ignore
public class TestGrmTestPersistCachedEndpoints_OutdatedPersistedFile_FetchFromGRM extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestGrmTestPersistCachedEndpoints_OutdatedPersistedFile_FetchFromGRM.class );

  /**
   * The bham_1_ launcher.
   */
  public ServerControllerLauncher bham_1_Launcher;

  /**
   * The bham_2_ launcher.
   */
  public ServerControllerLauncher bham_2_Launcher;

  /**
   * The bham_3_ launcher.
   */
  public ServerControllerLauncher bham_3_Launcher;

  /**
   * The char_1_ launcher.
   */
  public ServerControllerLauncher char_1_Launcher;

  @Before
  public void setUp() {
    super.setUp();
    System.setProperty( "AFT_LATITUDE", "33.373900" );
    System.setProperty( "AFT_LONGITUDE", "-86.798300" );
    System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
    System.setProperty( "SCLD_PLATFORM", TestConstants.GRM_PLATFORM_TO_USE );

    Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
    DME2Configuration config = new DME2Configuration( "TestGrm", props );
    try {
      DME2Manager mgr = new DME2Manager( "TestGrm", config );
    } catch ( DME2Exception e ) {
      throw new RuntimeException( e );
    }
    System.clearProperty( "AFT_DME2_GRM_URLS" );
  }

  @After
  public void tearDown() {
    if ( bham_1_Launcher != null ) {
      bham_1_Launcher.destroy();
    }

    if ( bham_2_Launcher != null ) {
      bham_2_Launcher.destroy();
    }

    if ( char_1_Launcher != null ) {
      char_1_Launcher.destroy();
    }
    System.clearProperty( "lrmRName" );
    System.clearProperty( "lrmRVer" );
    System.clearProperty( "lrmEnv" );
    System.clearProperty( "Pid" );
    System.clearProperty( "DME2_EP_ACCESSOR_CLASS" );
    super.tearDown();
  }

  @Test
  public void testPersistCachedEndpoints_OutdatedPersistedFile_FetchFromGRM()
      throws DME2Exception, MalformedURLException, URISyntaxException {
    DME2Manager mgr = null;

    System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
    System.setProperty( "AFT_LATITUDE", "33.373900" );
    System.setProperty( "AFT_LONGITUDE", "-86.798300" );
    //System.setProperty( "platform", TestConstants.GRM_PLATFORM_TO_USE );
    System.setProperty( "SCLD_PLATFORM", "SANDBOX-DEV" );
    try {

      File f = new File( "src/test/resources/cached-endpoints-out-of-date.ser" );
      if ( !f.exists() ) {
        fail( "File: src/test/resources/cached-endpoints-out-of-date.ser does not exist to complete test with." );
      }
      Calendar cal = Calendar.getInstance();

      f.setLastModified( cal.getTimeInMillis());

      System.setProperty( DME2Constants.DME2_CACHED_ENDPOINTS_FILE, "src/test/resources/cached-endpoints-out-of-date.ser");
      Properties props = RegistryFsSetup.init();
      props.put( DME2Constants.DME2_CACHED_ENDPOINTS_FILE, "src/test/resources/cached-endpoints-out-of-date.ser" );

      DME2Configuration config = new DME2Configuration( "testPersistCachedEndpoints_OutdatedPersistedFile_FetchFromGRM", props );

      mgr = new DME2Manager( "testPersistCachedEndpoints_OutdatedPersistedFile_FetchFromGRM", config );

      String uriStr =
          "http://DME2RESOLVE/service=com.att.aft.DME3CREchoService/version=1.5.0/envContext=TEST/routeOffer=BAU";

      Request request =
          new HttpRequest.RequestBuilder( new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      DME2Client client = new DME2Client( mgr, request );
      DME2Payload payload = new DME2TextPayload( "TEST" );

      String response = (String) client.sendAndWait( payload );
      System.out.println( response );

    } catch(Exception e)	{
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      System.clearProperty( "AFT_ENVIRONMENT" );
      System.clearProperty( "AFT_LATITUDE" );
      System.clearProperty( "AFT_LONGITUDE" );
    }
  }

}
