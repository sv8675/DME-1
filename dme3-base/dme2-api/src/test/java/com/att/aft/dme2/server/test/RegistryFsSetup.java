/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;

import com.att.aft.dme2.util.DME2Constants;

/**
 * Perform set-up operations for the DME2 File Registry.
 */
public class RegistryFsSetup {

  private static String currDir = ( new File( System.getProperty( "user.dir" ) ) ).getAbsolutePath();
  private static String srcConfigDir =
      currDir + File.separator + "src" + File.separator + "test" + File.separator + "etc" + File.separator +
          "svc_config";
  private static String fsDir = currDir + "/dme2-fs-registry";

  /**
   * Initialize environment - especially required system properties.
   *
   * @throws IOException
   */
  public static Properties init() throws IOException {
    Properties props = new Properties();
    props.setProperty( "AFT_DME2_SVCCONFIG_DIR", "file:///" + srcConfigDir );
    props.setProperty( "AFT_DME2_EP_REGISTRY_FS_DIR", fsDir );
    //props.setProperty( "DME2_EP_REGISTRY_CLASS", "DME2FS" );
    props.setProperty( DME2Constants.DME2_EP_REGISTRY_CLASS, DME2Constants.DME2FS );
    props.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
    props.setProperty( "platform", "SANDBOX-DEV" );
    props.setProperty( "AFT_LATITUDE", "33.373900" );
    props.setProperty( "AFT_LONGITUDE", "-86.798300" );
    System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
    System.setProperty( "AFT_LATITUDE", "33.373900" );
    System.setProperty( "AFT_LONGITUDE", "-86.798300" );
    System.out.println( "Inside init of RegistryFS" );

    copyRoutingFile( "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.afttest3.DataPartitionEnabler/version=1.0.0/envContext=LAB/routeOffer=DP1" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestRefreshCachedRouteInfo/version=1.0.0/envContext=PROD/routeOffer=PRIMARY" );
    copyEndpointFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestFindAndFilterInvalidEndpoints/version=1.0.0/envContext=LAB",
        "routeOffer=null.txt" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter1/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter2/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter3/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter4/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter5/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter6/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter7/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter9/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter10/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter11/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter12/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter13/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter14/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    System.out.println( "Inside exiting init of RegistryFS" );
    return props;
  }

  public static void cleanup() {
    File fsFileDir = new File( fsDir + File.separator );
    try {
      FileUtils.cleanDirectory( fsFileDir );
    } catch ( IOException e ) {
    }
  }

  public static PropertiesConfiguration newInit() throws IOException {

    PropertiesConfiguration props = new PropertiesConfiguration();
    //Properties props = new Properties();
    props.setProperty( "AFT_DME2_SVCCONFIG_DIR", "file:///" + srcConfigDir );
    props.setProperty( "AFT_DME2_EP_REGISTRY_FS_DIR", fsDir );
    props.setProperty( "DME2_EP_REGISTRY_CLASS", "DME2FS" );
    props.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
    props.setProperty( "AFT_LATITUDE", "33.373900" );
    props.setProperty( "AFT_LONGITUDE", "-86.798300" );
    props.setProperty( "AFT_DME2_CONTAINER_NAME_KEY", "" );
    System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
    System.setProperty( "AFT_LATITUDE", "33.373900" );
    System.setProperty( "AFT_LONGITUDE", "-86.798300" );

    copyRoutingFile( "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.afttest3.ExchangeFailover/version=1.0.0/envContext=LAB/routeOffer=FO1" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.afttest3.DataPartitionEnabler/version=1.0.0/envContext=LAB/routeOffer=DP1" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestRefreshCachedRouteInfo/version=1.0.0/envContext=LAB/routeOffer=PRIMARY" );
    copyEndpointFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestFindAndFilterInvalidEndpoints/version=1.0.0/envContext=LAB",
        "routeOffer=null.txt" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter1/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter2/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter3/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter4/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter5/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter6/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter7/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter8/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );
    copyRoutingFile(
        "http://DME2RESOLVE/service=com.att.aft.dme2.api.TestDME2ThrottleFilter9/version=1.0.0/envContext=PROD/routeOffer=BAU_SE" );

    return props;
  }

  /**
   * Copy routeInfo.xml file for service to Registry FS directory.
   * <p>
   * Example http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE
   *
   * @param servicePath
   * @throws IOException
   */
  public static void copyRoutingFile( String serviceUri ) throws IOException {
    String[] toks = serviceUri.split( "/" );
    String service = toks[3];
    String version = toks[4];
    String envContext = toks[5];

    String routeFilePath =
        service + File.separator + version + File.separator + envContext + File.separator + "routeInfo.xml";

    File srcFile = new File( srcConfigDir + File.separator + routeFilePath );
    File destFile = new File( fsDir + File.separator + routeFilePath );
    System.out.println( "srcFile.getAbsolutePath()=" + srcFile.getAbsolutePath() );
    System.out.println( "destFile.getAbsolutePath()=" + destFile.getAbsolutePath() );
    FileUtils.copyFile( srcFile, destFile );
  }

  public static void copyEndpointFile( String serviceUri, String fileName ) throws IOException {
    String[] toks = serviceUri.split( "/" );
    String service = toks[3];
    String version = toks[4];
    String envContext = toks[5];

    String routeFilePath = service + File.separator + version + File.separator + envContext + File.separator + fileName;

    File srcFile = new File( srcConfigDir + File.separator + routeFilePath );
    File destFile = new File( fsDir + File.separator + routeFilePath );
    System.out.println( "srcFile.getAbsolutePath()=" + srcFile.getAbsolutePath() );
    System.out.println( "destFile.getAbsolutePath()=" + destFile.getAbsolutePath() );
    FileUtils.copyFile( srcFile, destFile );
  }

  /**
   * @return the srcConfigDir
   */
  public static String getSrcConfigDir() {
    return srcConfigDir;
  }

}
