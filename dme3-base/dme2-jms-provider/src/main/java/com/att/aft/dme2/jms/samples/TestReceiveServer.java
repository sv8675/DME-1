/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;


@SuppressWarnings("PMD.SystemPrintln")
public class TestReceiveServer {

  private static final Logger logger = LoggerFactory.getLogger(TestReceiveServer.class);
  private String jndiClass = null;
  private String jndiUrl = null;
  private String serverConn = null;
  private String serverDest = null;
  private int threads = 0;
  private int receiveTimeout = 0;

  private QueueConnection connection = null;
  private static final int CONSTANT_30000 = 30000;
  private static final int CONSTANT_5000 = 5000;


  public TestReceiveServer( String jndiClass, String jndiUrl, String serverConn, String serverDest, int threads,
                            int receiveTimeout ) throws Exception {
    logger.debug( null, "ctor", LogMessage.METHOD_ENTER );
    logger.info( null, "ctor", "jndiClass={} jndiUrl={} serverConn={} serverDest={} threads={} receiveTimeout={}", jndiClass, jndiUrl, serverConn, serverDest, threads, receiveTimeout );
    this.jndiClass = jndiClass;
    this.jndiUrl = jndiUrl;
    this.serverConn = serverConn;
    this.serverDest = serverDest;
    this.threads = threads;
    this.receiveTimeout = receiveTimeout;
    logger.debug( null, "ctor", LogMessage.METHOD_EXIT );
  }

  public void start() throws JMSException, javax.naming.NamingException {
    logger.debug( null, "start", LogMessage.METHOD_ENTER );
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    table.put( "java.naming.factory.initial", jndiClass );
    table.put( "java.naming.provider.url", jndiUrl );

    System.out.println( "Getting InitialContext" );
    InitialContext context = new InitialContext( table );

    System.out.println( "Looking up QueueConnectionFactory" );
    QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup( serverConn );

    System.out.println( "Looking up request Queue" );
    Queue requestQueue = (Queue) context.lookup( serverDest );

    System.out.println( "Creating QueueConnection" );
    connection = qcf.createQueueConnection();

    listeners = new TestReceiveServerListener[threads];
    for ( int i = 0; i < threads; i++ ) {
      listeners[i] = new TestReceiveServerListener( connection, requestQueue, receiveTimeout );
      listeners[i].start();
    }
    logger.debug( null, "start", LogMessage.METHOD_EXIT );
  }

  private TestReceiveServerListener[] listeners = null;

  public void stop() throws JMSException {
    logger.debug( null, "stop", LogMessage.METHOD_ENTER );
    if ( listeners != null ) {
      for ( int i = 0; i < threads; i++ ) {
        //listeners[i].interrupt();
        listeners[i].stop1();
      }
    }
    listeners = null;
    connection.close();
    connection = null;
    logger.debug( null, "stop", LogMessage.METHOD_EXIT );
  }

  public static void main( String[] args ) throws Exception {
    logger.debug( null, "main", LogMessage.METHOD_ENTER );
    String jndiClass = null;
    String jndiUrl = null;
    String serverConn = null;
    String serverDest = null;
    String serverThreadsStr = "0";
    String receiveTimeoutStr = null;

    System.out.println( "Starting HttpJMS TestServer" );

    String usage =
        "TestServer -jndiClass <jndiClass> -jndiUrl <jndiUrl> -serverConn <url> -serverDest <url> -serverThreads <n> -receiveTimeout <value>";

    for ( int i = 0; i < args.length; i++ ) {
      if ( "-jndiClass".equals( args[i] ) ) {
        jndiClass = args[i + 1];
      } else if ( "-jndiUrl".equals( args[i] ) ) {
        jndiUrl = args[i + 1];
      } else if ( "-serverConn".equals( args[i] ) ) {
        serverConn = args[i + 1];
      } else if ( "-serverDest".equals( args[i] ) ) {
        serverDest = args[i + 1];
      } else if ( "-serverThreads".equals( args[i] ) ) {
        serverThreadsStr = args[i + 1];
      } else if ( "-receiveTimeout".equals( args[i] ) ) {
        receiveTimeoutStr = args[i + 1];
      } else if ( "-?".equals( args[i] ) ) {
        System.out.println( usage );
        System.exit( 0 );
      }
    }

    int serverThreads = Integer.parseInt( serverThreadsStr );
    int receiveTimeout = CONSTANT_30000;
    if ( receiveTimeoutStr != null ) {
      receiveTimeout = Integer.parseInt( receiveTimeoutStr );
    }

    System.out.println( "Running with following arguments:" );
    System.out.println( "    JNDI Provider Class: " + jndiClass );
    System.out.println( "    JNDI Provider URL: " + jndiUrl );
    System.out.println( "    Server Connection: " + serverConn );
    System.out.println( "    Server Destination: " + serverDest );
    System.out.println( "    Server Threads: " + serverThreads );
    System.out.println( "    Server Receive timeout: " + receiveTimeout );

    TestReceiveServer server = null;
    if ( serverThreads > 0 ) {
      System.out.println( "Starting listeners..." );
      server = new TestReceiveServer( jndiClass, jndiUrl, serverConn, serverDest, serverThreads, receiveTimeout );
      server.start();
    } else {
      System.out.flush();
      System.err.println( "No thread count specified, cannot start server" );
      System.exit( 1 );
    }

    while ( true ) {
      Thread.sleep( CONSTANT_5000 );
    }

  }
}


