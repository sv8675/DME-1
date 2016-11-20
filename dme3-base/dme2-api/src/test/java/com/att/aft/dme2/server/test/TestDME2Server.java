/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2Server;
import com.att.aft.dme2.api.DME2ServerProperties;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder.RequestDispatcherType;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.att.aft.dme2.test.EchoReplyHandler;
import com.att.aft.dme2.test.Locations;


/**
 * The Class TestDME2Server.
 */
public class TestDME2Server extends DME2BaseTestCase {

  private long sleepTimeMs = 3000;


  @Before
  public void setUp() {
    super.setUp();
    Locations.BHAM.set();
  }


  private void stopServer( DME2Server server ) {
    if ( server != null ) {
      try {
        server.stop();
      } catch ( Throwable e ) {
      }
    }
  }

  /**
   * Test specific values, including setPort.
   *
   * @throws UnknownHostException
   * @throws DME2Exception
   * @throws InterruptedException
   */

  @Test
  public void testDME2Server() throws IOException, DME2Exception, InterruptedException {
    DME2Server server = null;
    DME2Manager manager = null;
    try {
      manager = new DME2Manager( "TestDME2Server",
          new DME2Configuration( "TestDME2Server", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME,
              RegistryFsSetup.newInit() ) );
      server = manager.getServer();
      DME2ServerProperties serverProperties = server.getServerProperties();

      serverProperties.setHostname( InetAddress.getLocalHost().getHostName() );
      serverProperties.setPort( 14600 );
      serverProperties.setConnectionIdleTimeMs( 12000 );
      serverProperties.setCorePoolSize( 10 );
      serverProperties.setMaxPoolSize( 15 );
      serverProperties.setSocketAcceptorThreads( 3 );
      serverProperties.setThreadIdleTimeMs( 12000 );

      server.start();
      Thread.sleep( sleepTimeMs );
      assertEquals( 14600, serverProperties.getPort().intValue() );
      assertTrue( server.isRunning() );
    } finally {
      stopServer( server );
    }
  }


  /**
   * Test specific values, including setPort.
   *
   * @throws UnknownHostException
   * @throws DME2Exception
   * @throws InterruptedException
   */

  @Test
  public void testDME2ServerGracefulshutdown() throws UnknownHostException, DME2Exception, InterruptedException {
    PropertiesConfiguration props = new PropertiesConfiguration();
    props.setProperty( "AFT_DME2_SEND_DATEHEADER", "true" );
    props.setProperty( "AFT_DME2_SEND_SERVERVERSION", "true" );

    DME2Manager dm1 = new DME2Manager( "TestShutdown",
        new DME2Configuration( "TestShutdown", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME, props ) );
    DME2Server server5 = dm1.getServer();

    server5.getServerProperties().setGracefulShutdownTimeMs( 200 );

    server5.start();
    Thread.sleep( sleepTimeMs );
    assertTrue( server5.getServerProperties().getSendDateheader() );
    assertTrue( server5.getServerProperties().getSendServerversion() );
    //assertTrue(server5.getServerProperties().isRunning());
    try {
      server5.stop();
    } catch ( Exception e ) {

    }
    Thread.sleep( 250 );
    assertFalse( server5.isRunning() );
  }

  /**
   * Test if port is assigned if none is set.
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws IOException
   */


  @Test
  public void testDME2ServerDefaults() throws DME2Exception, InterruptedException, IOException {

    DME2Server server = null;
    DME2Manager manager = null;
    try {
      manager = new DME2Manager( "TestDME2Server",
          new DME2Configuration( "TestDME2Server", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME,
              RegistryFsSetup.newInit() ) );
      server = manager.getServer();
      server.start();
      Thread.sleep( sleepTimeMs );
      if ( server.getServerProperties().getPort() == 0 ) {
        fail( "port=" + server.getServerProperties().getPort() );
      }
      assertTrue( server.isRunning() );
    } finally {
      stopServer( server );
    }

  }

  /**
   * Test AFT_DME2_PORT property.
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws IOException
   */


  @Test
  public void testDME2ServerSetPortProp() throws DME2Exception, InterruptedException, IOException {
    DME2Server server = null;
    DME2Manager manager = null;
    try {
      manager = new DME2Manager( "TestDME2Server",
          new DME2Configuration( "TestDME2Server", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME,
              RegistryFsSetup.newInit() ) );
      server = manager.getServer();
      server.getServerProperties().setPort( 14700 );
      server.start();
      Thread.sleep( sleepTimeMs );
      assertEquals( 14700, server.getServerProperties().getPort().intValue() );
      assertTrue( server.isRunning() );
    } finally {
      try {
        server.stop();
      } catch ( Exception e ) {

      }
    }
  }

  /**
   * Test setPortRange().
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws IOException
   */

  @Test
  public void testDME2ServerSetPortRangeWithDefault() throws DME2Exception, InterruptedException, IOException {
    DME2Server server = null;
    DME2Manager manager = null;
    try {
      manager = new DME2Manager( "TestDME2Server",
          new DME2Configuration( "TestDME2Server", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME,
              RegistryFsSetup.newInit() ) );
      server = manager.getServer();
      String portRange = "5678-5680,5678";
      server.getServerProperties().setPortRange( portRange );

      server.start();
      Thread.sleep( sleepTimeMs );
      int port = server.getServerProperties().getPort().intValue();
      if ( port == 5678 ||
          port == 5679 ||
          port == 5680 ) {
        System.out.println( "port=" + port );
        // check port range is set in server
        assertEquals( portRange, server.getServerProperties().getPortRange() );
      } else {
        fail( "port=" + server.getServerProperties().getPort() + ", not in range: " + portRange );
      }
      assertTrue( server.isRunning() );
    } finally {
      try {
        server.stop();
      } catch ( Exception e ) {

      }
    }
  }

  /**
   * Test setPortRange().
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws IOException
   */

  @Test
  public void testDME2ServerSetPortRangeWODefault() throws DME2Exception, InterruptedException, IOException {
    String portRange = "5778-5780";
    DME2Server server = null;
    DME2Manager manager = null;
    try {
      manager = new DME2Manager( "TestDME2Server",
          new DME2Configuration( "TestDME2Server", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME,
              RegistryFsSetup.newInit() ) );
      server = manager.getServer();
      server.getServerProperties().setPortRange( portRange );

      server.start();
      Thread.sleep( sleepTimeMs );
      boolean portFound = false;
      int port = server.getServerProperties().getPort().intValue();
      if ( port == 5778 ||
          port == 5779 ||
          port == 5780 ) {
        System.out.println( "port=" + port );
        portFound = true;
        // check port range is set in server
        assertEquals( portRange, server.getServerProperties().getPortRange() );
      } else {
        fail( "port=" + server.getServerProperties().getPort() + ", not in range: " + portRange );
      }
      assertTrue( server.isRunning() );
      assertTrue( portFound );

      server.stop();
      Thread.sleep( 3000 );
    } finally {
      stopServer( server );
    }
  }

  /**
   * Test setPortRange().
   *
   * @throws DME2Exception
   * @throws InterruptedException
   * @throws IOException
   */

  @Test
  public void testDME2ServerSetPortRangeFail() throws DME2Exception, InterruptedException, IOException {
    // all available ports in range should be taken...
    String portRange = "5978-5980,5980";

    DME2Server server = null;
    DME2Server server1 = null;
    DME2Server server2 = null;
    DME2Server server3 = null;
    DME2Manager manager = null;

    try {
      DME2Configuration config =
          new DME2Configuration( "TestDME2Server", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME,
              RegistryFsSetup.newInit() );
      manager = new DME2Manager( "TestDME2Server", config );
      DME2EndpointRegistry registry = manager.getEndpointRegistry();

      server1 = new DME2Server( config );
      server1.getServerProperties().setHostname( "localhost" );
      server1.getServerProperties().setPort( 5978 );
      server1.getServerProperties().setReuseAddress( false );
      server1.start();

      server2 = new DME2Server( config );
      server2.getServerProperties().setHostname( "localhost" );
      server2.getServerProperties().setPort( 5979 );
      server2.getServerProperties().setReuseAddress( false );
      server2.start();

      server3 = new DME2Server( config );
      server3.getServerProperties().setHostname( "localhost" );
      server3.getServerProperties().setPort( 5980 );
      server3.getServerProperties().setReuseAddress( false );
      server3.start();

      Thread.sleep( 10000 );
      assertTrue( server1.isRunning() );
      assertTrue( server2.isRunning() );
      assertTrue( server3.isRunning() );

      server = new DME2Server( config );
      server.getServerProperties().setHostname( "localhost" );
      server.getServerProperties().setPortRange( portRange );
      server.getServerProperties().setReuseAddress( false );
      try {
        server.start();
        Thread.sleep( sleepTimeMs );
        fail( "should have thrown an exception" );
      } catch ( Exception e ) {
        // ok - expect failure...attempting with all ports already taken
        System.out.println( "attempting when all ports already taken: " + e );
        // already have all available ports used - server should not be
        // running or exception should have been thrown
        assertFalse( server.isRunning() );

      }
    } finally {
      stopServer( server );
      stopServer( server1 );
      stopServer( server2 );
      stopServer( server3 );
      Thread.sleep( 2000 );

    }


  }


  /**
   * Test specific values, including setPort.
   *
   * @throws UnknownHostException
   * @throws DME2Exception
   * @throws InterruptedException
   */

  @Test
  public void testDME2ServerDuplicate() throws UnknownHostException, DME2Exception, InterruptedException {
    DME2Configuration config = new DME2Configuration();

    DME2Server server1 = new DME2Server( config );
    server1.getServerProperties().setPort( 5200 );
    server1.getServerProperties().setReuseAddress( false );
    server1.start();

    System.out.println( "server1.getPort()=" + server1.getServerProperties().getPort() );
    Thread.sleep( 20000 );

    // attempt duplicate
    DME2Server server = new DME2Server( config );

    server.getServerProperties().setPort( 5200 );
    boolean gotException = false;
    try {
      server.start();
      Thread.sleep( sleepTimeMs );
      System.out.println( "server.getPort()=" + server.getServerProperties().getPort() );
      assertFalse( server.isRunning() );
    } catch ( Exception e ) {
      // ok - expect failure...attempting duplicate port
      System.out.println( "attempting duplicate port: " + e );
      gotException = true;
    } finally {
      server.stop();
      server1.stop();
    }
    assertTrue( "Should've thrown an exception", gotException );
  }

  @Ignore
  @Test
  public void testDME2PortPersist() throws Exception {

    System.setProperty( "lrmRName", "com.att.aft.DME2PortPersistContainer" );
    System.setProperty( "lrmRVer", "1.0.0" );
    System.setProperty( "lrmEnv", "LAB" );
    System.setProperty( "lrmRO", "PORT" );
    System.setProperty( "platform", "SANDBOX-DEV" );
    System.setProperty( "Pid", "6313" );
    System.setProperty( "DME2.DEBUG", "true" );
    System.setProperty( "AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Server_DME2PortPersist" );

    Properties props = new Properties();
    props.setProperty( "DME2.DEBUG", "true" );
    DME2Manager mgr = new DME2Manager( "testDME2PortPersist", new DME2Configuration( "testDME2PortPersist", props ) );
    try {
      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder
          .setServiceURI( "service=com.att.aft.DME2PortPersistTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT" );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/ServletHolderTest" );

      EchoResponseServlet echoServlet = new EchoResponseServlet(
          "service=com.att.aft.DME2PortPersistTest/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/", "1" );
      String pattern[] = { "/test", "/servletholder" };
      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/servletholdertest" );


      EchoResponseServlet echoServlet1 = new EchoResponseServlet(
          "service=com.att.aft.DME2PortPersistTest1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/", "1" );
      String pattern1[] = { "/test1", "/servletholder1" };
      DME2ServletHolder srvHolder1 = new DME2ServletHolder( echoServlet1, pattern1 );
      srvHolder1.setContextPath( "/servletholdertest1" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );
      shList.add( srvHolder1 );
      // If context is set, DME2 will use this for publishing as context with
      // endpoint registration, else serviceURI above will be used
      //svcHolder.setContext("/FilterTest");
      // Below is to disable the default metrics filter thats added to
      // capture DME2 Metrics event of http traffic. By default MetricsFilter
      // is enabled
      // svcHolder.disableMetricsFilter();

      // Adding a Log filter to print incoming msg.
      TestDME2LogFilter filter = new TestDME2LogFilter();
      ArrayList<RequestDispatcherType> dlist = new ArrayList<RequestDispatcherType>();
      dlist.add( DME2FilterHolder.RequestDispatcherType.REQUEST );
      dlist.add( DME2FilterHolder.RequestDispatcherType.FORWARD );

      DME2FilterHolder filterHolder = new DME2FilterHolder( filter,
          "/FilterTest", EnumSet.copyOf( dlist ) );
      List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
      flist.add( filterHolder );

      //svcHolder.setContext("/ServletHolder");
      svcHolder.setFilters( flist );
      svcHolder.setServletHolders( shList );

      //mgr.addService(svcHolder);
      mgr.getServer().start();
      mgr.bindService( svcHolder );

      Thread.sleep( 12000 );

      // Invokes the above registered FilterTest service by resolving
      // endpoints via SOA registry

      Request request = new RequestBuilder(new URI(
          "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=DEFAULT" ) )
          .withHttpMethod( "POST" )
          .withReadTimeout( 40000 ).withReturnResponseAsBytes( false ).withLookupURL(
              "http://DME2RESOLVE/service=MyService/version=1.0.0/envContext=PROD/routeOffer=DEFAULT" )
          .withSubContext("/test")
          .build();


      DME2Client client = new DME2Client( mgr, request );
      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setResponseHandlers( replyHandler );
      //client.setSubContext("/test");
      //client.setSubContext("/servletholdertest");
      //client.setPayload(new TextPayload("test"));

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "DME2PortPersistTest/" ) );
      Thread.sleep( 400 );

      // Invokes the above registered FilterTest service by resolving
      // endpoints via SOA registry

     Request request1 = new RequestBuilder(new URI(
          "http://DME2RESOLVE/service=com.att.aft.DME2PortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" ) )
          .withHttpMethod( "POST" ).withReadTimeout( 40000 ).withReturnResponseAsBytes( false ).withLookupURL(
              "http://DME2RESOLVE/service=com.att.aft.DME2PortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" )
          .withSubContext("/servletholdertest")
          .build();

      DME2Client client1 = new DME2Client( mgr, request1 );
      EchoReplyHandler replyHandler1 = new EchoReplyHandler();
      client1.setResponseHandlers( replyHandler1 );
      //client1.setContext("/test");
      //client1.setSubContext("/servletholdertest");
      //client1.setPayload(new TextPayload("test"));

      String reply1 = (String) client1.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply1 );
      assertTrue( reply1.contains( "DME2PortPersistTest/" ) );

    } finally {
      mgr.getServer().stop();
      System.clearProperty( "lrmRName" );
      System.clearProperty( "lrmRVer" );
      System.clearProperty( "lrmEnv" );
      System.clearProperty( "lrmRO" );
      System.clearProperty( "platform" );
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
    }
  }
  
  @Test
  public void testDME2SSLNonSSLPortPersist() throws Exception {

	System.setProperty("lrmRName", "com.att.aft.DME2PortSSLNonSSLPersistContainer");
	System.setProperty("lrmRVer", "1.0.0");
	System.setProperty("lrmEnv", "LAB");
	System.setProperty("lrmRO", "PORT");
	System.setProperty("platform", TestConstants.SCLD_PLATFORM_FOR_SANDBOX_DEV);
	System.setProperty("Pid", "6313");
	System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	//System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Server_DME2SSLNonSSLPortPersist");
		
	Properties props = new Properties();
	props.setProperty("DME2.DEBUG", "false");
	props.setProperty("AFT_DME2_KEYSTORE", "m2e.jks");
	props.setProperty("AFT_DME2_KEY_PASSWORD", "password");
	props.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
	props.setProperty("AFT_DME2_SSL_ENABLE", "true");
	props.setProperty("AFT_DME2_ALLOW_RENEGOTIATE", "true");
	DME2Manager mgr = new DME2Manager("testDME2SSLPortPersist",props);

    Properties props1 = new Properties();
    props1.setProperty( "DME2.DEBUG", "false" );
	props1.setProperty("AFT_DME2_ALLOW_PORT_CACHING", "false");

	DME2Configuration config1 = new DME2Configuration( "testDME2NonSSLPortPersist", props1);
    DME2Manager mgr1 = new DME2Manager( "testDME2NonSSLPortPersist", config1, props1 );
    
    try {
      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder
          .setServiceURI( "service=com.att.aft.DME2SSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/SSLServletHolderTest" );

      EchoResponseServlet echoServlet = new EchoResponseServlet(
          "service=com.att.aft.DME2SSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT/", "1" );
      String pattern[] = { "/testssl", "/sslservletholder" };
      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/sslservletholdertest" );


      DME2ServiceHolder svcHolder1 = new DME2ServiceHolder();
      svcHolder1
          .setServiceURI(
              "service=com.att.aft.DME2NonSSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" );
      svcHolder1.setManager( mgr1 );
      svcHolder1.setContext( "/NonSSLServletHolderTest" );

      EchoResponseServlet echoServlet1 = new EchoResponseServlet(
          "service=com.att.aft.DME2NonSSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT/", "1" );
      String pattern1[] = { "/testnonssl", "/nonsslservletholder" };
      DME2ServletHolder srvHolder1 = new DME2ServletHolder( echoServlet1, pattern1 );
      srvHolder1.setContextPath( "/nonsslservletholdertest" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );
      List<DME2ServletHolder> shList1 = new ArrayList<DME2ServletHolder>();
      shList1.add( srvHolder1 );
      svcHolder.setServletHolders( shList );
      svcHolder1.setServletHolders( shList1 );
      //mgr.addService(svcHolder);
      mgr.getServer().start();
      mgr.bindService( svcHolder );

      mgr1.getServer().start();
      mgr1.bindService( svcHolder1 );

      Thread.sleep( 12000 );

      // Invokes the above registered FilterTest service by resolving
      // endpoints via SOA registry
      Request request = new RequestBuilder(new URI(
          "http://DME2RESOLVE/service=com.att.aft.DME2SSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" ) )
          .withHttpMethod( "POST" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL(
              "http://DME2RESOLVE/service=com.att.aft.DME2SSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" )
          .withSubContext("/testssl")
          .build();

      DME2Client client = new DME2Client( mgr, request );

      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setResponseHandlers( replyHandler );
      //client.setSubContext("/testssl");
      //client.setSubContext("/servletholdertest");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "DME2SSLPortPersistTest/" ) );
      Thread.sleep( 400 );

      // Invokes the above registered FilterTest service by resolving
      // endpoints via SOA registry

      Request request1 = new RequestBuilder(new URI(
          "http://DME2RESOLVE/service=com.att.aft.DME2NonSSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" ) )
          .withHttpMethod( "POST" ).withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL(
              "http://DME2RESOLVE/service=com.att.aft.DME2NonSSLPortPersistTest/version=1.0.0/envContext=LAB/routeOffer=PORT" )
          .withSubContext("/testnonssl")
          .build();

      DME2Client client1 = new DME2Client( mgr1, request1 );
      EchoReplyHandler replyHandler1 = new EchoReplyHandler();
      client1.setResponseHandlers( replyHandler1 );
      //client1.setContext("/nonsslservletholdertest");
      //client1.setSubContext("/testnonssl");
      //client1.setPayload("test");

      String reply1 = (String) client1.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply1 );
      assertTrue( reply1.contains( "DME2NonSSLPortPersistTest/" ) );

      String portCacheFilePath = config1.getProperty( "AFT_DME2_PORT_CACHE_FILE",
          System.getProperty( "user.home" ) + "/.aft/.dme2PortCache" );
      File file = new File( portCacheFilePath );

      String resName = System.getProperty( "lrmRName" );
      String serviceTocheck = null;
      if ( resName != null ) {
        // Chances are this is a LRM managed container
        String resVer = System.getProperty( "lrmRVer" );
        String resEnv = System.getProperty( "lrmEnv" );
        String resRO = System.getProperty( "lrmRO" );
        if ( resName != null && resVer != null && resRO != null && resEnv != null ) {
          serviceTocheck = resName + "/" + resVer + "/" + resEnv + "/" + resRO;
        }
      }
      assertTrue( serviceTocheck != null );
      //assertTrue(DME2PortFileManager.getInstance().getPort(serviceTocheck, true)!=null);
      //assertTrue(DME2PortFileManager.getInstance().getPort(serviceTocheck, false)!=null);

    } finally {
      try {
        mgr.getServer().stop();
      } catch ( Exception e ) {
        //
      }
      try {
        mgr1.getServer().stop();
      } catch ( Exception e ) {
        //
      }
      System.clearProperty( "lrmRName" );
      System.clearProperty( "lrmRVer" );
      System.clearProperty( "lrmEnv" );
      System.clearProperty( "lrmRO" );
      System.clearProperty( "platform" );
      System.clearProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON" );
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );
    }
  }

  @Test
  public void testDME2PortPersistTurnOff() throws Exception {

	  System.setProperty("lrmRName", "com.att.aft.DME2PortPersistTurnOffContainer");
	  System.setProperty("lrmRVer", "1.0.0");
	  System.setProperty("lrmEnv", "LAB");
	  System.setProperty("lrmRO", "PORT");
	  System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
	  System.setProperty("Pid", "6313");
	  System.setProperty("AFT_DME2_ALLOW_PORT_CACHING", "false");
	  System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	  System.setProperty("AFT_DME2_PF_SERVICE_NAME", "com.att.aft.TestDME2Server_DME2PortPersistTurnOff");
		
	  Properties props = new Properties();
	  props.setProperty("DME2.DEBUG", "false");
	  props.setProperty("AFT_DME2_KEYSTORE", "m2e.jks");
	  props.setProperty("AFT_DME2_KEY_PASSWORD", "password");
	  props.setProperty("AFT_DME2_KEYSTORE_PASSWORD", "password");
	  props.setProperty("AFT_DME2_SSL_ENABLE", "true");
	  props.setProperty("AFT_DME2_ALLOW_RENEGOTIATE", "true");
	  props.setProperty("AFT_DME2_ALLOW_PORT_CACHING", "false");
	  DME2Manager mgr = new DME2Manager("testDME2SSLPortPersistTO",props);

    

    Properties props1 = new Properties();
    props1.setProperty( "DME2.DEBUG", "false" );
    props1.setProperty( "AFT_DME2_ALLOW_PORT_CACHING", "false" );

    DME2Configuration config1 = new DME2Configuration( "testDME2NonSSLPortPersistTO", props1 );
    DME2Manager mgr1 = new DME2Manager( "testDME2NonSSLPortPersistTO", config1);
    String svcURI = "service=com.att.aft.DME2SSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT";
    String svcURI1 = "service=com.att.aft.DME2NonSSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT/";
    try {
      // Create service holder for each service registration
      DME2ServiceHolder svcHolder = new DME2ServiceHolder();
      svcHolder
          .setServiceURI( svcURI );
      svcHolder.setManager( mgr );
      svcHolder.setContext( "/SSLServletHolderTest" );

      EchoResponseServlet echoServlet = new EchoResponseServlet(
          svcURI, "1" );
      String pattern[] = { "/testssl", "/sslservletholder" };
      DME2ServletHolder srvHolder = new DME2ServletHolder( echoServlet, pattern );
      srvHolder.setContextPath( "/sslservletholdertest" );


      DME2ServiceHolder svcHolder1 = new DME2ServiceHolder();
      svcHolder1
          .setServiceURI( svcURI1 );
      svcHolder1.setManager( mgr1 );
      svcHolder1.setContext( "/NonSSLServletHolderTest" );

      EchoResponseServlet echoServlet1 = new EchoResponseServlet(
          svcURI1, "1" );
      String pattern1[] = { "/testnonssl", "/nonsslservletholder" };
      DME2ServletHolder srvHolder1 = new DME2ServletHolder( echoServlet1, pattern1 );
      srvHolder1.setContextPath( "/nonsslservletholdertest" );

      List<DME2ServletHolder> shList = new ArrayList<DME2ServletHolder>();
      shList.add( srvHolder );
      List<DME2ServletHolder> shList1 = new ArrayList<DME2ServletHolder>();
      shList1.add( srvHolder1 );
      svcHolder.setServletHolders( shList );
      svcHolder1.setServletHolders( shList1 );
      //mgr.addService(svcHolder);
      mgr.getServer().start();
      mgr.bindService( svcHolder );

      mgr1.getServer().start();
      mgr1.bindService( svcHolder1 );

      Thread.sleep( 12000 );

      // Invokes the above registered FilterTest service by resolving
      // endpoints via SOA registry

      String uriStr =
          "http://DME2RESOLVE/service=com.att.aft.DME2SSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT";
      Request request =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withSubContext("/testssl").build();

      //Request request = new RequestBuilder(mgr.getClient(), new HttpConversation(), new URI("http://DME2RESOLVE/service=com.att.aft.DME2SSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT")).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL("http://DME2RESOLVE/service=com.att.aft.DME2SSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT").build();


      DME2Client client = new DME2Client( mgr, request );

      EchoReplyHandler replyHandler = new EchoReplyHandler();
      client.setResponseHandlers( replyHandler );
      //client.setSubContext("/testssl");
      //client.setSubContext("/servletholdertest");
      //client.setPayload("test");

      String reply = (String) client.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply );
      assertTrue( reply.contains( "DME2SSLPortPersistTOTest/" ) );
      Thread.sleep( 400 );

      // Invokes the above registered FilterTest service by resolving
      // endpoints via SOA registry

      uriStr =
          "http://DME2RESOLVE/service=com.att.aft.DME2NonSSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT";
      Request request1 =
          new RequestBuilder(new URI( uriStr ) ).withHttpMethod( "POST" )
              .withReadTimeout( 30000 ).withReturnResponseAsBytes( false ).withLookupURL( uriStr ).withSubContext("/testnonssl").build();

      //request = new RequestBuilder(mgr.getClient(),
      //		new HttpConversation(), new URI("http://DME2RESOLVE/service=com.att.aft.DME2NonSSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT")).withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false).withLookupURL("http://DME2RESOLVE/service=com.att.aft.DME2NonSSLPortPersistTOTest/version=1.0.0/envContext=LAB/routeOffer=PORT").build();

      DME2Client client1 = new DME2Client( mgr1, request1 );
      EchoReplyHandler replyHandler1 = new EchoReplyHandler();
      client1.setResponseHandlers( replyHandler1 );
      //client1.setContext("/nonsslservletholdertest");
      //client1.setSubContext("/testnonssl");
      //client1.setPayload("test");

      String reply1 = (String) client1.sendAndWait( new DME2TextPayload( "test" ) );
      System.out.println( "Reply from EchoServlet " + reply1 );
      assertTrue( reply1.contains( "DME2NonSSLPortPersistTOTest/" ) );

      String portCacheFilePath = mgr.getStringProp("AFT_DME2_PORT_CACHE_FILE", System.getProperty( "user.home" ) + "/.aft/.dme2PortCache" );
      File file = new File( portCacheFilePath );

      String resName = System.getProperty( "lrmRName" );
      String serviceTocheck = null;
      if ( resName != null ) {
        // Chances are this is a LRM managed container
        String resVer = System.getProperty( "lrmRVer" );
        String resEnv = System.getProperty( "lrmEnv" );
        String resRO = System.getProperty( "lrmRO" );
        if ( resName != null && resVer != null && resRO != null && resEnv != null ) {
          serviceTocheck = resName + "/" + resVer + "/" + resEnv + "/" + resRO;
        }
      }
      assertTrue( serviceTocheck != null );
      //assertTrue(DME2PortFileManager.getInstance().getPort(serviceTocheck, true)==null);
      //assertTrue(DME2PortFileManager.getInstance().getPort(serviceTocheck, false)==null);

    } finally {
      try {
        mgr.unbindServiceListener( svcURI );
      } catch ( Exception e ) {
        //
      }
      try {
        mgr.unbindServiceListener( svcURI1 );
      } catch ( Exception e ) {
        //
      }
      try {
        mgr.getServer().stop();
      } catch ( Exception e ) {
        //
      }
      try {
        mgr1.getServer().stop();
      } catch ( Exception e ) {
        //
      }
      System.clearProperty( "lrmRName" );
      System.clearProperty( "lrmRVer" );
      System.clearProperty( "lrmEnv" );
      System.clearProperty( "lrmRO" );
      System.clearProperty( "platform" );
      System.clearProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON" );
      System.clearProperty( "AFT_DME2_ALLOW_PORT_CACHING" );
      System.clearProperty( "AFT_DME2_PF_SERVICE_NAME" );

    }
  }


  @Test
  public void testShutdownOfStoppedServer() throws DME2Exception, InterruptedException {

    PropertiesConfiguration props = new PropertiesConfiguration();
    props.setProperty( "AFT_DME2_SEND_DATEHEADER", "true" );
    props.setProperty( "AFT_DME2_SEND_SERVERVERSION", "true" );
    DME2Manager mgr = new DME2Manager( "TestShutdownOfStoppedServer",
        new DME2Configuration( "TestShutdownOfStoppedServer", DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME,
            props ) );
    DME2Server server = mgr.getServer();

    server.getServerProperties().setGracefulShutdownTimeMs( 200 );

    server.start();
    Thread.sleep( sleepTimeMs );
    assertTrue( server.getServerProperties().getSendDateheader() );
    assertTrue( server.getServerProperties().getSendServerversion() );
    //assertTrue(server.isRunning());
    try {
      server.stop();
    } catch ( Exception e ) {

    }
    Thread.sleep( 250 );
    assertFalse( server.isRunning() );
    //call stop on the server a second time. fail if an exception is thrown.
    try {
      server.stop();
    } catch ( Exception e ) {
      fail( "Unexpected exception on second shutdown" );
    }
    Thread.sleep( 250 );
    assertFalse( server.isRunning() );

  }


  public static void main( String[] a ) throws Exception {
    TestDME2Server dme2Server = new TestDME2Server();
    dme2Server.testDME2ServerSetPortRangeFail();
  }

}