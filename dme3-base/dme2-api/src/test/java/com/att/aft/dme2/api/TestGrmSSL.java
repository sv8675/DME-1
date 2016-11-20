/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.util.grm.GRMTestAccessor;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.util.DME2Protocol;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.util.DME2Constants;

@Ignore
public class TestGrmSSL  extends DME2BaseTestCase {

  @Before
  public void setUp() {
    try {
      GRMAccessorFactory.getInstance().close();
    } catch (Exception e) {}
    super.setUp();
    System.setProperty("DME2.DEBUG", "true");
    System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE );
    System.setProperty("lrmRName", "com.att.aft.test.DME2TestContainer");
    System.setProperty("lrmRVer", "1.0.0");
    System.setProperty("lrmEnv", "DEV");
    System.setProperty("Pid", "6313");
    //System.setProperty("AFT_DME2_GRM_URLS", "http://localhost:8001/GRMService/v1");

    // DME2Manager manager = new DME2Manager("TestGrm", props);
    // for GRM tests clear these properties defined for all other unit tests in DME2aseTestCase
    //DME2Constants.setGrmUseDefaultUserPassword(false);
    System.clearProperty("AFT_DME2_GRM_USER");
    System.clearProperty("AFT_DME2_GRM_PASS");
    System.setProperty( "DME2_GRM_USER", "");
    System.setProperty( "DME2_GRM_PASS", "" );
  }

  /*
   * (non-Javadoc)
   *
   * ()
   */
  @After
  public void tearDown() {
    try {
      GRMAccessorFactory.getInstance().close();
    } catch (Exception e) {}
    System.clearProperty("lrmRName");
    System.clearProperty("lrmRVer");
    System.clearProperty("lrmEnv");
    System.clearProperty("Pid");
    System.clearProperty("DME2_EP_ACCESSOR_CLASS");
    System.clearProperty("AFT_DME2_GRM_USE_SSL");
  }

  @Test
  public void testDefaultSettingsfailsWithNoUserPass() {
    try{
      String user = System.getProperty("AFT_DME2_GRM_USER");
      String pass = System.getProperty("AFT_DME2_GRM_PASS");
      assertNull("for this test property AFT_DME2_GRM_USER should not have any value", user);
      assertNull("for this test property AFT_DME2_GRM_PASS property should not have any value", pass);
      new DME2Manager("testEmptyGRMUsernameSSL", new Properties());
      fail("test should have failed with empty username resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
  }

  // DME3.x does not use settings inside of DME2Constants (it's only used for constants).  So this test is moot
  @Ignore
  @Test
  public void testDefaultSettingsDoesntfailsWithNoUserPassIfFlagIsSet() throws UnknownHostException, DME2Exception {
    try{
      //DME2Constants.setGrmUseDefaultUserPassword(true);
      Properties props = null;
	try {
		props = RegistryFsSetup.init();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
      GRMTestAccessor.debugRequests=true;

      DME2Manager manager = new DME2Manager("testGRMSSL", props);

      String service = "com.att.aft.dme2.test.TestGRMSSL_DefaultUserPassFlag";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 3267;
      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";

      DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
      String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      svcRegistry.publish(serviceName,"/testGRMSSL", hostname, port, DME2Protocol.HTTP, props);
    } finally {
      //DME2Constants.setGrmUseDefaultUserPassword(false);
    }
  }


  @Test
  @Ignore
  public void testGRMSSL() throws Exception{
    System.err.println("--- START: testGRMSSL");
    System.setProperty( "DME2_GRM_USER", "mxxxxx" );
    System.setProperty( "DME2_GRM_PASS", "mxxxxx");
    Properties props = RegistryFsSetup.init();
    props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
    props.setProperty("AFT_DME2_GRM_USER",  DME2Constants.getGRMUserName());
    props.setProperty("AFT_DME2_GRM_PASS", DME2Constants.getGRMUserPass());
    props.setProperty( "DME2_GRM_USER", props.getProperty( "AFT_DME2_GRM_USER" ) );
    props.setProperty( "DME2_GRM_PASS", props.getProperty( "AFT_DME2_GRM_PASS" ) );
    GRMTestAccessor.debugRequests=true;

    DME2Manager manager = new DME2Manager("testGRMSSL", props);

    String service = "com.att.aft.dme2.test.TestGRMSSL";
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    int port = 3267;
    String version = "1.0.0";
    String envContext = "DEV";
    String routeOffer = "BAU_SE";
    DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
    String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
    svcRegistry.publish(serviceName,"/testGRMSSL", hostname, port, DME2Protocol.HTTP, props);
    System.err.println("--- END:  testGRMSSL");
  }



  @Test
  public void testEmptyGRMUsernameSSL() throws Exception{
    System.err.println("--- START: testEmptyGRMUsernameSSL");

    System.setProperty("AFT_DME2_GRM_USER","");
    System.setProperty("AFT_DME2_GRM_USE_SSL","true");
    try{
      new DME2Manager("testEmptyGRMUsernameSSL", new Properties());
      fail("test should have failed with empty username resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    finally{
      System.clearProperty("AFT_DME2_GRM_USER");
      System.clearProperty("AFT_DME2_GRM_USE_SSL");
    }
    System.err.println("--- END:  testEmptyGRMUsernameSSL");
  }


  @Test
  @Ignore
  public void testEmptyGRMPasswordSSL() throws Exception{
    System.err.println("--- START: testEmptyGRMPasswordSSL");
    Properties props = RegistryFsSetup.init();;
    props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
    props.setProperty("AFT_DME2_GRM_PASS","");
    try{
      new DME2Manager("testEmptyGRMPasswordSSL", props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    System.err.println("--- END:  testEmptyGRMPasswordSSL");
  }

  @Ignore
  @Test
  public void testNulllGRMUsernameSSL() throws Exception{
    System.err.println("--- START: testNullGRMUsernameSSL");

    System.setProperty("AFT_DME2_GRM_PASSWORD","mxxxxx");
    System.setProperty("AFT_DME2_GRM_USE_SSL","true");
    try{
      new DME2Manager("testEmptyGRMUsernameSSL", new Properties());
      fail("test should have failed with empty username resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    finally{
      System.clearProperty("AFT_DME2_GRM_USER");
      System.clearProperty("AFT_DME2_GRM_USE_SSL");
    }
    System.err.println("--- END:  testNullGRMUsernameSSL");
  }

  @Ignore
  @Test
  public void testNullGRMPasswordSSL() throws Exception{
    System.err.println("--- START: testNullGRMPasswordSSL");
    Properties props = RegistryFsSetup.init();;
    props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
    props.setProperty("AFT_DME2_GRM_USER","mxxxxx");
    try{
      new DME2Manager("testEmptyGRMPasswordSSL", props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    System.err.println("--- END:  testNullGRMPasswordSSL");
  }

  @Ignore
  @Test
  public void testNullGRMUsernamePasswordSSL() throws Exception{
    System.err.println("--- START: testNullGRMPasswordSSL");
    Properties props = RegistryFsSetup.init();;
    props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
    try{
      new DME2Manager("testEmptyGRMPasswordSSL", props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    System.err.println("--- END:  testNullGRMPasswordSSL");
  }

  @Ignore
  @Test
  public void testOverrideGRMUsernamePasswordSSL() throws Exception{
    System.err.println("--- START: testOverrideGRMUsernamePasswordSSL");
    Properties props = RegistryFsSetup.init();;
    props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
    props.setProperty("AFT_DME2_GRM_USER","mxxxxx");
    props.setProperty("AFT_DME2_GRM_PASS","mxxxxx");
    try{
      DME2Manager manager = new DME2Manager("testOverrideGRMUsernamePasswordSSL", props);

      String service = "com.att.aft.dme2.test.TestOverrideGRMUsernamePasswordSSL";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 3267;
      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";
      DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
      String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      svcRegistry.publish(serviceName,"/testOverrideGRMUsernamePasswordSSL", hostname, port, DME2Protocol.HTTP, props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find Authentication failed for user mxxxxx in message: %s", e.getMessage()), e.getMessage().contains("Authentication failed for user mxxxxx"));
    }
    System.err.println("--- END:  testOverrideGRMUsernamePasswordSSL");
  }

  @Ignore
  @Test
  public void testOverrideGRMUsernameSSL() throws Exception{
    System.err.println("--- START: testOverrideGRMUsernameSSL");
    Properties props = RegistryFsSetup.init();;
    props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
    props.setProperty("AFT_DME2_GRM_USER","mxxxxx");
    try{
      DME2Manager manager = new DME2Manager("testOverrideGRMUsernameSSL", props);

      String service = "com.att.aft.dme2.test.TestOverrideGRMUsernameSSL";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 3267;
      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";
      DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
      String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      svcRegistry.publish(serviceName,"/testOverrideGRMUsernameSSL", hostname, port, DME2Protocol.HTTP, props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 mxxxxx in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    System.err.println("--- END:  testOverrideGRMUsernameSSL");
  }

  @Ignore
  @Test
  public void testOverrideGRMPasswordSSL() throws Exception{
    System.err.println("--- START: testOverrideGRMPasswordSSL");
    Properties props = RegistryFsSetup.init();;
    props.setProperty("AFT_DME2_GRM_USE_SSL", "true");
    props.setProperty("AFT_DME2_GRM_PASS","mxxxxx");
    String user = System.getProperty("AFT_DME2_GRM_USER");
    try{
      DME2Manager manager = new DME2Manager("testOverrideGRMPasswordSSL", props);
      String service = "com.att.aft.dme2.test.TestOverrideGRMPasswordSSL";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 3267;
      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";
      DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
      String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      svcRegistry.publish(serviceName,"/testOverrideGRMPassword", hostname, port, DME2Protocol.HTTP, props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      e.printStackTrace();
      assertTrue(String.format("Expected to find AFT-DME2-0917 for user %s in message: %s", user,e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    System.err.println("--- END:  testOverrideGRMPasswordSSL");
  }

  @Test
  public void testEmptyGRMUsernameNonSSL() throws Exception{
    System.err.println("--- START: testEmptyGRMUsernameNonSSL");

    System.setProperty("AFT_DME2_GRM_USER","");
    System.setProperty("AFT_DME2_GRM_USE_SSL","false");
    try{
      new DME2Manager("testEmptyGRMUsernameSSL", new Properties());
      fail("test should have failed with empty username resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    finally{
      System.clearProperty("AFT_DME2_GRM_USER");
      System.clearProperty("AFT_DME2_GRM_USE_SSL");
    }
    System.err.println("--- END:  testEmptyGRMUsernameNonSSL");
  }


  @Test
  @Ignore
  public void testEmptyGRMPasswordNonSSL() throws Exception{
    System.err.println("--- START: testEmptyGRMPasswordNonSSL");
    Properties props = RegistryFsSetup.init();
    props.setProperty("AFT_DME2_GRM_PASS","");
    try{
      new DME2Manager("testEmptyGRMPasswordNonSSL", props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find AFT-DME2-0917 in message: %s", e.getMessage()), e.getMessage().contains("AFT-DME2-0917"));
    }
    System.err.println("--- END:  testEmptyGRMPasswordNonSSL");
  }

  @Test
  public void testOverrideGRMUsernamePasswordNonSSL() throws Exception{
    System.err.println("--- START: testOverrideGRMUsernamePasswordNonSSL");
    Properties props = RegistryFsSetup.init();
    props.setProperty("AFT_DME2_GRM_USER","mxxxxx");
    props.setProperty("AFT_DME2_GRM_PASS","mxxxxx");
    props.setProperty( "DME2_GRM_USER", props.getProperty( "AFT_DME2_GRM_USER" ) );
    props.setProperty( "DME2_GRM_PASS", props.getProperty( "AFT_DME2_GRM_PASS" ) );
    try{
      DME2Manager manager = new DME2Manager("testOverrideGRMUsernamePasswordNonSSL", props);

      String service = "com.att.aft.dme2.test.TestOverrideGRMUsernamePasswordNonSSL";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 3267;
      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";
      DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
      String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      svcRegistry.publish(serviceName,"/testOverrideGRMUsernamePasswordNonSSL", hostname, port, DME2Protocol.HTTP, props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find Authentication failed for user mxxxxx in message: %s", e.getMessage()), e.getMessage().contains("A non GRMException Error has occured"));
    }
    System.err.println("--- END:  testOverrideGRMUsernamePasswordNonSSL");
  }

  @Test
  @Ignore
  public void testOverrideGRMUsernameNonSSL() throws Exception{
    System.err.println("--- START: testOverrideGRMUsernameNonSSL");
    // DME3 loads up default DME2Manager statically, so this will fail unless we redo the password
    System.setProperty( "DME2_GRM_USER", "mxxxxx" );
    System.setProperty( "DME2_GRM_PASS", "mxxxxx" );
    Properties props = RegistryFsSetup.init();
    props.setProperty("AFT_DME2_GRM_USER","mxxxxx");
    props.setProperty("AFT_DME2_GRM_PASS", DME2Constants.getGRMUserPass() );
    props.setProperty( "DME2_GRM_USER", props.getProperty( "AFT_DME2_GRM_USER" ));
    props.setProperty( "DME2_GRM_PASS", props.getProperty( "AFT_DME2_GRM_PASS" ));
    try{
      DME2Manager manager = new DME2Manager("testOverrideGRMUsernameNonSSL", props);

      String service = "com.att.aft.dme2.test.TestOverrideGRMUsernameNonSSL";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 3267;
      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";
      DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
      String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      svcRegistry.publish(serviceName,"/testOverrideGRMUsernameNonSSL", hostname, port, DME2Protocol.HTTP, props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find Authentication failed for user mxxxxx in message: %s", e.getMessage()), e.getMessage().contains("A non GRMException Error has occured"));
    }
    System.err.println("--- END:  testOverrideGRMUsernameNonSSL");
  }

  @Test
  @Ignore
  public void testOverrideGRMPasswordNonSSL() throws Exception{
    System.err.println("--- START: testOverrideGRMPasswordNonSSL");
    System.setProperty( "DME2_GRM_USER", "mxxxxx" );
    System.setProperty( "DME2_GRM_PASS", "mxxxxx" );
    Properties props = RegistryFsSetup.init();
    props.setProperty("AFT_DME2_GRM_USER", DME2Constants.getGRMUserName());
    props.setProperty("AFT_DME2_GRM_PASS","mxxxxx");
    props.setProperty( "DME2_GRM_USER", props.getProperty( "AFT_DME2_GRM_USER" ) );
    props.setProperty( "DME2_GRM_PASS", props.getProperty( "AFT_DME2_GRM_PASS" ) );
    String user = DME2Constants.getGRMUserName();
    try{
      DME2Manager manager = new DME2Manager("testOverrideGRMPasswordNonSSL", props);
      String service = "com.att.aft.dme2.test.TestOverrideGRMPasswordNonSSL";
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      int port = 3267;
      String version = "1.0.0";
      String envContext = "DEV";
      String routeOffer = "BAU_SE";
      DME2EndpointRegistryGRM svcRegistry = (DME2EndpointRegistryGRM) manager.getEndpointRegistry();
      String serviceName = "service=" + service + "/version=" + version + "/envContext=" + envContext + "/routeOffer=" + routeOffer;
      svcRegistry.publish(serviceName,"/testOverrideGRMPasswordNonSSL", hostname, port, DME2Protocol.HTTP, props);
      fail("test should have failed with empty password resulting in a DME2Exception");
    } catch( DME2Exception e){
      assertTrue(String.format("Expected to find Authentication failed for user %s in message: %s", user,e.getMessage()), e.getMessage().contains("A non GRMException Error has occured"));
    }
    System.err.println("--- END:  testOverrideGRMPasswordNonSSL");
  }

  //@TODO add test cases for using setGrmUseDefaultUserPassword

}
