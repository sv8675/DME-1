/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class TestJMSHeaders extends JMSBaseTestCase {

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  /**
   * test empty reply message
   */
  @Test
  public void testJMSHeaderReplyMessage() throws Exception {
    ServerLauncher launcher = null;
    try {
      //System.setProperty("DME2.DEBUG", "true");
      System.setProperty( "AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true" );
      Locations.BHAM.set();
      Properties props = RegistryFsSetup.init();
      Hashtable<String, Object> table = new Hashtable<String, Object>();
      for ( Object key : props.keySet() ) {
        table.put( (String) key, props.get( key ) );
      }
      table.put( "DME2.DEBUG", "true" );
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      QueueConnection connection = factory.createQueueConnection();
      QueueSession session = connection.createQueueSession( true, 0 );
      Queue remoteQueue = (Queue) context.lookup( TestConstants.jmsHeaderServiceResolveStr );
      // remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

      launcher = new ServerLauncher( null, "-city", "BHAM" );
      launcher.launchJMSHeaderReplyJMSServer();

      Thread.sleep( 5000 );

      QueueSender sender = session.createSender( remoteQueue );

      // Queue replyToQueue = session.createTemporaryQueue();

      TextMessage msg = session.createTextMessage();
      msg.setText( "TEST" );
      msg.setStringProperty( "com.att.aft.dme2.jms.dataContext",
          TestConstants.dataContext );
      msg.setStringProperty( "com.att.aft.dme2.jms.partner",
          TestConstants.partner );
      msg.setStringProperty( "AFT_DME2_EP_READ_TIMEOUT_MS", "29001" );
      Queue replyToQueue = (Queue) context
          .lookup( "http://DME2LOCAL/clientResponseQueue" );
      msg.setJMSReplyTo( replyToQueue );
      msg.setJMSExpiration( 31000 );

      long start = System.currentTimeMillis();
      sender.send( msg );
      //QueueReceiver replyReceiver = session.createReceiver(replyToQueue,
      //		"JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
      Thread.sleep( 5000 );  
      QueueReceiver replyReceiver = session.createReceiver( replyToQueue );
         
        TextMessage rcvMsg = (TextMessage) replyReceiver.receive( 130000 );
        long elapsedTime = System.currentTimeMillis() - start;
        // Currently the longrunListener is forced to sleep for 120000. So make sure any change for below assert
        // is directly depending on LongRunMessageLister impl
        //TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
        assertEquals( "TestJMSHeaderReplyListener:::TEST", rcvMsg.getText() );
        String sentTime = rcvMsg.getStringProperty( "CLIENT_SENT_TIME" );
        String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
        SimpleDateFormat dformat = new SimpleDateFormat( ISO_FORMAT );
        Date sentDate = dformat.parse( sentTime );
        System.out.println( "DME2Client request send time :" + sentTime );
        assertTrue( sentDate.after( new Date( start ) ) && sentDate.before( new Date() ) );
        assertEquals( "29001", rcvMsg.getStringProperty( "CLIENT_EP_READ_TIMEOUT" ) );
        System.out.println( "TestEmptyReplyListener responded after " + elapsedTime + " ms" );

        // Send another request with default ep read timeout and validate
        sender = session.createSender( remoteQueue );

        // Queue replyToQueue = session.createTemporaryQueue();

        msg = session.createTextMessage();
        msg.setText( "TEST" );
        msg.setStringProperty( "com.att.aft.dme2.jms.dataContext",
            TestConstants.dataContext );
        msg.setStringProperty( "com.att.aft.dme2.jms.partner",
            TestConstants.partner );
//        msg.setStringProperty( "AFT_DME2_EP_READ_TIMEOUT_MS", "29001" );
        replyToQueue = (Queue) context
            .lookup( "http://DME2LOCAL/clientResponseQueue1" );
        msg.setJMSReplyTo( replyToQueue );
        msg.setJMSExpiration( 31000 );

        start = System.currentTimeMillis();
        sender.send( msg );
//        Thread.sleep( 5000 );  
        replyReceiver = session.createReceiver( replyToQueue );
//        Thread.sleep( 10000 );
        rcvMsg = (TextMessage) replyReceiver.receive( 130000 );
        elapsedTime = System.currentTimeMillis() - start;
        // Currently the longrunListener is forced to sleep for 120000. So make sure any change for below assert
        // is directly depending on LongRunMessageLister impl
        //TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
        System.out.println("reply message is: "+rcvMsg.getText() );
        assertEquals( "TestJMSHeaderReplyListener:::TEST", rcvMsg.getText() );
        sentTime = rcvMsg.getStringProperty( "CLIENT_SENT_TIME" );
        System.out.println( "DME2Client request send time :" + sentTime );
        sentDate = dformat.parse( sentTime );
        assertTrue( sentDate.after( new Date( start ) ) && sentDate.before( new Date() ) );
        // Default ep read timeout used by DME2
        assertEquals( "240000", rcvMsg.getStringProperty( "CLIENT_EP_READ_TIMEOUT" ) );
        System.out.println( "TestEmptyReplyListener responded after " + elapsedTime + " ms" );

    } finally {
      if ( launcher != null ) {
        launcher.destroy();
      }
    }
  }


  @After
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
