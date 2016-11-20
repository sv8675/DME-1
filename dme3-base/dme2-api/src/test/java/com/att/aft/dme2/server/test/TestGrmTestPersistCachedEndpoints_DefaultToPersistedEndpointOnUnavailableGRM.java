/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
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
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointCacheGRM;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.util.DME2UnitTestUtil;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
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
public class TestGrmTestPersistCachedEndpoints_DefaultToPersistedEndpointOnUnavailableGRM  extends DME2BaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger( TestGrmTestPersistCachedEndpoints_DefaultToPersistedEndpointOnUnavailableGRM.class );

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

    Properties props =null;
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
  public void testPersistCachedEndpoints_DefaultToPersistedEndpointOnUnavailableGRM() {
    logger.debug( null, "testPersistCachedEndpoints_DefaultToPersistedEndpointOnUnavailableGRM", LogMessage.METHOD_ENTER );
    System.setProperty( "SCLD_PLATFORM", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
    DME2Manager mgr = null;

    System.setProperty( "AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE );
    System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
    System.setProperty( "AFT_LATITUDE", "33.373900" );
    System.setProperty( "AFT_LONGITUDE", "-86.798300" );
    System.setProperty( DME2Constants.Cache.CACHE_ENABLE_PERSISTENCE, "true" );

    try {
      Properties props = new Properties();
      String cacheFile = "src/test/resources/cached-endpoints-unavailable-grm.ser";

      props.put( DME2Constants.DME2_CACHED_ENDPOINTS_FILE, cacheFile );

      try {
        // The below is done as workaround to avoid the first time failure
        // If the cached endpoint file is older by 1 hr, DME2 endpoint registry code
        // will attempt to invoke GRM still.
        // Modifying the timestamp to recent time, will allow registry to consume this cached
        // file and avoid calling GRM
        File f = new File( cacheFile );
        f.setLastModified( System.currentTimeMillis() - 1 );
      } catch ( Exception e ) {
        // Ignore any error in modifying file timestamp

        e.printStackTrace();
      }

      DME2Configuration config = new DME2Configuration( "TestPersistCachedEndpoints", props );

      // Set the grm accessor to null so it gets recreated (this is bad!)
      //DME2UnitTestUtil.setFinalStatic( GRMAccessorFactory.class.getDeclaredField( "grmAccessorHandler" ), null, null );
      GRMAccessorFactory.getInstance().close();
      mgr = new DME2Manager( "TestPersistCachedEndpoints", config );

//			mgr = new DME2Manager("TestPersistCachedEndpoints", props);

			 /*Check if Endpoint cache contains the service. They should have been load from the file when the cache was initialized*/
/**      Map<String, DME2ServiceEndpointData> endpointCache = ((DME2EndpointRegistryGRM) mgr.getEndpointRegistry()).getRegistryEndpointCache().getCache();
 System.out.println("Contents of Endpoint cache: " + endpointCache);
 assertTrue(!endpointCache.isEmpty()); //Could contain two entries, one for routeoffer TEST_1 and one for routeoffer DEFAULT
 */
      DME2EndpointCacheGRM endpointCache = (DME2EndpointCacheGRM) DME2UnitTestUtil
          .getPrivate( DME2EndpointRegistryGRM.class.getDeclaredField( "endpointCache" ), mgr.getEndpointRegistry() );
      assertTrue( endpointCache.getCurrentSize() > 0 );
      String serviceUrl =
          "http://DME2RESOLVE/service=com.att.aft.DME2CREchoService/version=1.5.0/envContext=LAB/routeOffer=BAU";

      Request request = new HttpRequest.RequestBuilder( new URI( serviceUrl ) )
          .withHttpMethod( "POST" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( false )
          .withLookupURL( serviceUrl )
          .build();

      DME2Client client = new DME2Client( mgr, request );
      DME2Payload payload = new DME2TextPayload( "TEST" );
      client.sendAndWait( payload );

    } catch ( Exception e ) {
      e.printStackTrace();
			/*Expecting this error since the service isn't up. Just validating that a call to GRM wasn't made
			 * and DME2 attempted to use the persisted endpoints to attempt the request*/
      assertTrue( e.getMessage().contains( "[AFT-DME2-0703]" ) );
    } finally {
      System.clearProperty( "AFT_DME2_GRM_URLS" );
      System.clearProperty( "AFT_ENVIRONMENT" );
      System.clearProperty( "AFT_LATITUDE" );
      System.clearProperty( "AFT_LONGITUDE" );
      System.clearProperty( DME2Constants.Cache.CACHE_ENABLE_PERSISTENCE );
      logger.debug( null, "testPersistCachedEndpoints_DefaultToPersistedEndpointOnUnavailableGRM", LogMessage.METHOD_EXIT );
    }
  }
}
