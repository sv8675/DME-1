/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.net.URI;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.types.RouteInfo;

import junit.framework.TestCase;

@Ignore
public class TestDME2RouteInfoVersionMapping extends TestCase {
  private static Logger logger = LoggerFactory.getLogger( TestDME2RouteInfoVersionMapping.class );

  public void setUp() throws Exception {
    //System.setProperty("AFT_DME2_GRM_URLS", "http://0.0.0.0:8001/GRMService/v1");
    System.setProperty( "DME2.DEBUG", "true" );
    System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
    System.setProperty( "platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
    RegistryFsSetup.init();
  }


/*	public static void main(String[] args) throws Exception {
    String pattern1 = "[0-9]+"; //Matches version="73"
		String pattern2 = "[0-9]+\\.[0-9]+"; //Matches version="73.1"
		String pattern3 = "[0-9]+\\.[0-9]+\\.[0-9]+"; //Matches version="73.1.1"
		
		String pattern4 = "[0-9]+\\.[*]\\.[*]"; //Matches version="73.*.*"
		String pattern5 = "[0-9]+\\.[*]\\.[0-9]"; //Matches version="73.*.1"
		String pattern6 = "Hello[.]World"; 

		String version = "73.1.7";
		
		if(version.matches(pattern3)){
			System.out.println("This matches.");
		}else{
			System.out.println("This does not match.");
		}
		
		testVersionMapping();

	}*/

  @Test
  @Ignore  
  public static void testVersionMapping() throws Exception {

    System.setProperty( "SCLD_PLATFORM", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
    DME2Manager mgr = null;
    String serviceURI =
        "/service=com.att.aft.dme2.test.TestVersionMapping/version=1.3.5/envContext=LAB/routeOffer=VERSION_MAP";
    //String serviceURI1 = "/service=com.att.aft.dme2.test.TestVersionMapping/version=1.3.5/envContext=LAB/routeOffer=DME2_SECONDARY";
    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_WithDME2Version" );

    try {
      RegistryFsSetup.init();

      RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithVersionMap();

      RegistryFsSetup grmInit = new RegistryFsSetup();
//      grmInit.saveRouteInfoInGRM( new DME2Configuration( "testVersionMapping" ), rtInfo, "LAB" );
      try {
        Thread.sleep( 10000 );
      } catch ( Exception ex ) {
      }

      Properties props = RegistryFsSetup.init();
      // props.put( "AFT_DME2_PORT", "32405" );
      props.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "10000" );

      DME2Configuration config = new DME2Configuration( "TestVersionMapping", props );
      mgr = new DME2Manager( "TestVersionMapping", config );
      mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "TestVersionMapping" ) );

      Thread.sleep( 30000 );

      //Scenario A
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestVersionMapping/version=1.0.5/envContext=LAB/partner=DME2_PARTNER";

      Request request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      DME2Client sender = new DME2Client( mgr, request );

//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 10000);
      DME2TextPayload payload = new DME2TextPayload( "This is a test" );
      //sender.setPayload("This is a test");

      String reply = null;
      logger.debug( null, "testVersionMapping", "Sending first request" );
      reply = (String) sender.sendAndWait( payload );

      assert ( reply.contains( "version=1.3.5" ) );
      reply = null;
      //Scenario B
      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestVersionMapping/version=1/envContext=LAB/partner=DME2_PARTNER";

      request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( mgr, request );

//			DME2Client sender = new DME2Client(mgr, new URI(uriStr), 10000);
      payload = new DME2TextPayload( "This is a test" );

      //sender = new DME2Client(mgr, new URI(uriStr), 10000);
      //sender.setPayload("This is a test");

      try {
        reply = (String) sender.sendAndWait( payload );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertNull( e );
      }
      assert ( reply.contains( "version=1.3.5" ) );
      reply = null;
      //Scenario C
      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestVersionMapping/version=1.9/envContext=LAB/partner=DME2_PARTNER";

//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");

      request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( mgr, request );

      try {
        reply = (String) sender.sendAndWait( payload );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertNull( e );
      }
      assert ( reply.contains( "version=1.3.5" ) );
      reply = null;
      //Scenario D - send to version=99.5.1 and send and wait should fail with no endpoints found (negative)
      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestVersionMapping/version=99.5.1/envContext=LAB/partner=DME2_PARTNER";

//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");

      request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( mgr, request );

      try {
        reply = (String) sender.sendAndWait( payload );
      } catch ( Exception e ) {
        assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) );
      }

      reply = null;
      //Scenario E - use DMERESOLVE using the routeOffer that the service is started with (incorrect version), but has matching versionMap filter, DME2RESOLVE
      // does not use routeInfo
      uriStr =
          "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestVersionMapping/version=78/envContext=LAB/routeOffer=VERSION_MAP";

//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");

      request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( mgr, request );

      try {
        reply = (String) sender.sendAndWait( payload );
      } catch ( Exception e ) {
        assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) );
      }
      reply = null;
      //Scenario F - use DMERESOLVE using the routeOffer that the service is started with (correct version)
      uriStr =
          "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestVersionMapping/version=1.3.5/envContext=LAB/routeOffer=VERSION_MAP";

//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");

      request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( mgr, request );

      try {
        reply = (String) sender.sendAndWait( payload );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertNull( e );
      }
      assert ( reply.contains( "version=1.3.5" ) );

      reply = null;
      //Scenario F - use DMESEARCH to resolve a matching versionMap that's can result in multiple line items matching and picks the closest one
      uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestVersionMapping/version=78.2/envContext=LAB/partner=DME2_PARTNER";

//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");

      request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      sender = new DME2Client( mgr, request );

      try {
        reply = (String) sender.sendAndWait( payload );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) );
        //assertNull(e);
      }
      assertNull( reply );

      reply = null;
    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( Exception e ) {

      }
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
    }
  }

  /*
  public void testVersionMapping_ScenarioF() throws Exception {
    System.setProperty( "SCLD_PLATFORM", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV );
    DME2Manager mgr = null;
    String serviceURI =
        "/service=com.att.aft.dme2.test.TestVersionMapping/version=1.3.5/envContext=LAB/routeOffer=VERSION_MAP";
    //String serviceURI1 = "/service=com.att.aft.dme2.test.TestVersionMapping/version=1.3.5/envContext=LAB/routeOffer=DME2_SECONDARY";
    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Registration_WithDME2Version" );

    try {
      RegistryFsSetup.init();

      RouteInfo rtInfo = RouteInfoCreatorUtil.createRouteInfoWithVersionMap();

      RegistryFsSetup grmInit = new RegistryFsSetup();
      grmInit.saveRouteInfoInGRM( new DME2Configuration( "testVersionMapping" ), rtInfo, "LAB" );
      try {
        Thread.sleep( 10000 );
      } catch ( Exception ex ) {
      }

      Properties props = RegistryFsSetup.init();
      // props.put( "AFT_DME2_PORT", "32405" );
      props.put( "AFT_DME2_EP_READ_TIMEOUT_MS", "10000" );

      DME2Configuration config = new DME2Configuration( "TestVersionMapping", props );
      mgr = new DME2Manager( "TestVersionMapping", config );
      mgr.bindServiceListener( serviceURI, new EchoServlet( serviceURI, "TestVersionMapping" ) );

      Thread.sleep( 30000 );
      //Scenario F - use DMESEARCH to resolve a matching versionMap that's can result in multiple line items matching and picks the closest one
      String uriStr =
          "http://DME2SEARCH/service=com.att.aft.dme2.test.TestVersionMapping/version=78/envContext=LAB/partner=DME2_PARTNER";

//			sender = new DME2Client(mgr, new URI(uriStr), 10000);
//			sender.setPayload("This is a test");

      Request request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 20000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).build();

      DME2Client sender = new DME2Client( mgr, request );
      String reply = null;

      try {
        logger.debug( null, "testVersionMapping", "sending final request" );
        reply = (String) sender.sendAndWait(  new DME2TextPayload( "This is a test" ) );
      } catch ( Exception e ) {
        e.printStackTrace();
        assertTrue( e.getMessage().contains( "AFT-DME2-0702" ) );
        //assertNull(e);
      }
      logger.debug( null, "testVersionMapping", "reply={}", reply );
      assertNotNull( reply );
      assertTrue ( reply.contains( "version=1.3.5" ) );
     //assertNull( reply ); // This is actually going to 1.3.6 not 1.3.5 ...
    } finally {
      try {
        mgr.unbindServiceListener( serviceURI );
      } catch ( Exception e ) {

      }
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
    }
  }
  */
}
