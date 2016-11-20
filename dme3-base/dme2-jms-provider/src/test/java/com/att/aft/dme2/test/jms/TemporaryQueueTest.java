/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.jms.DME2JMSException;
import com.att.aft.dme2.jms.DME2JMSManager;
import com.att.aft.dme2.jms.DME2JMSQueue;
import com.att.aft.dme2.jms.DME2JMSQueueReceiver;
import com.att.aft.dme2.jms.DME2JMSTemporaryQueue;
import com.att.aft.dme2.test.jms.server.TestReceiveListener;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.LocalQueueMessageListener;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class TemporaryQueueTest extends JMSBaseTestCase {
  public void setup() throws Exception {
    super.setUp();
    System.setProperty( "-Dlog4j.configuration", "file:src/main/config/log4j-console.properties" );
  }

  @Test
  public void testCreateTemporaryQueue() throws Exception {
    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;
    TemporaryQueue tempQ = null;
    QueueSender sender = null;
    QueueReceiver receiver = null;

    Hashtable<String, Object> table = new Hashtable<String, Object>();
    table.put( "java.naming.factory.initial", TestConstants.jndiClass );
    table.put( "java.naming.provider.url", TestConstants.jndiUrl );

    try {

      InitialContext context = new InitialContext( table );
      qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      qConn = qcf.createQueueConnection();
      session = qConn.createQueueSession( true, 0 );
      tempQ = session.createTemporaryQueue();
      sender = session.createSender( tempQ );
      receiver = session.createReceiver( tempQ );

      TextMessage message = session.createTextMessage();
      message.setText( "TEST" );
      message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
      message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );
      message.setStringProperty( "dme2.tempqueue.idletimeoutms", "10000" );
      message.setJMSExpiration( System.currentTimeMillis() + 1000 );
      sender.send( message );
      TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
      assertEquals( "TEST", rcvMsg.getText() );

    } catch ( Exception e ) {
      fail( e.getMessage() );
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, receiver );

    }
  }

  @Test
  public void testTemporaryQueueIdleTimeoutSetPerMessage() throws Exception {
    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;
    TemporaryQueue tempQ = null;
    QueueSender sender = null;
    QueueReceiver receiver = null;

    Hashtable<String, Object> table = new Hashtable<String, Object>();
    table.put( "java.naming.factory.initial", TestConstants.jndiClass );
    table.put( "java.naming.provider.url", TestConstants.jndiUrl );

    try {

      try {

        InitialContext context = new InitialContext( table );
        qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
        qConn = qcf.createQueueConnection();
        session = qConn.createQueueSession( true, 0 );
        tempQ = session.createTemporaryQueue();
        sender = session.createSender( tempQ );
        receiver = session.createReceiver( tempQ );

        TextMessage message = session.createTextMessage();
        message.setText( "TEST" );
        message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
        message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );
        message.setStringProperty( "dme2.tempqueue.idletimeoutms", "10000" );
        message.setJMSExpiration( System.currentTimeMillis() + 1000 );
        sender.send( message );
        TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
        assertEquals( "TEST", rcvMsg.getText() );

      } catch ( Exception e ) {
        fail( e.getMessage() );
      } finally {
        JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, receiver );
      }

      try {

        InitialContext context = new InitialContext( table );
        qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
        qConn = qcf.createQueueConnection();
        session = qConn.createQueueSession( true, 0 );
        tempQ = session.createTemporaryQueue();
        sender = session.createSender( tempQ );
        receiver = session.createReceiver( tempQ );

        TextMessage message = session.createTextMessage();
        message.setText( "TEST" );
        message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
        message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );
        message.setStringProperty( "dme2.tempqueue.idletimeoutms", "1" );
        message.setJMSExpiration( System.currentTimeMillis() + 1000 );
        sender.send( message );
        TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
        assertEquals( "TEST", rcvMsg.getText() );

      } catch ( Exception e ) {
        assertTrue( e.getMessage().contains( "AFT-DME2-6300" ) || e.getMessage().contains( "AFT-DME2-6301" ) );
      } finally {
        JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, receiver );
      }

    } catch ( Exception e ) {
      fail( e.getMessage() );
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, receiver );
    }
  }

  @Test
  public void testTemporaryQueueConsumedBySameConnection() throws Exception {
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    table.put( "java.naming.factory.initial", TestConstants.jndiClass );
    table.put( "java.naming.provider.url", TestConstants.jndiUrl );
    InitialContext context = new InitialContext( table );

    QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
    QueueConnection qConn = qcf.createQueueConnection();
    QueueSession session = qConn.createQueueSession( true, 0 );
    TemporaryQueue tempQ = session.createTemporaryQueue();
    QueueSender sender = session.createSender( tempQ );

    TextMessage message = session.createTextMessage();
    message.setText( "TEST" );
    message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
    message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );
    sender.send( message );

    QueueConnection conn2 = null;
    QueueSession session2 = null;
    QueueReceiver rcvr2 = null;

    try {
      conn2 = qcf.createQueueConnection();
      session2 = conn2.createQueueSession( true, 0 );
      rcvr2 = session2.createReceiver( tempQ );
      TextMessage reply = (TextMessage) rcvr2.receive( 1000 );
      fail( "Should have failed. Temporary queue was created by a different connection." );
    } catch ( DME2JMSException e ) {
      String s = e.getMessage();
      if ( s.indexOf( "Attempt to create a receiver" ) < 0 ) {
        fail( "Incorrect message returned" );
      }
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, null );
      JMSBaseTestCase.closeJMSResources( conn2, session2, tempQ, null, rcvr2 );
    }
  }

  @Test
  public void testFailsForClosedConnection() throws Exception {
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    table.put( "java.naming.factory.initial", TestConstants.jndiClass );
    table.put( "java.naming.provider.url", TestConstants.jndiUrl );
    InitialContext context = new InitialContext( table );

    QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
    QueueConnection qConn = qcf.createQueueConnection();
    QueueSession session = qConn.createQueueSession( true, 0 );

    TemporaryQueue tempQ = session.createTemporaryQueue();
    String tempQName = tempQ.getQueueName();
    QueueSender sender = session.createSender( tempQ );

    TextMessage message = session.createTextMessage();
    message.setText( "TEST" );
    message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
    message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );
    qConn.close();
    Thread.sleep( 1000 );
    try {
      sender.send( message );
      fail( "Should have failed.  The connection is already closed." );
    } catch ( javax.jms.JMSException e ) {
      assert ( e.toString().toLowerCase().indexOf( "closed" ) > -1 );
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, null );
    }
  }

  @Test
  public void testIdleTimeoutReaper() throws Exception {
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    table.put( "java.naming.factory.initial", TestConstants.jndiClass );
    table.put( "java.naming.provider.url", TestConstants.jndiUrl );
    InitialContext context = new InitialContext( table );
    QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
    QueueConnection qConn = qcf.createQueueConnection();
    QueueSession session = qConn.createQueueSession( true, 0 );
    TemporaryQueue tempQ = session.createTemporaryQueue();
    String tempQName = tempQ.getQueueName();
    QueueSender sender = session.createSender( tempQ );
    TextMessage message = session.createTextMessage();
    message.setText( "TEST" );
    message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
    message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );

    // by default temporary queues can be idle only for a minute...
    // sleep for one minute+...
    Thread.sleep( 70000 );
    try {
      sender.send( message );
      fail( "Should have failed.  The scheduler in DME2JMSManager should have deleted the idle temporary queue." );
    } catch ( javax.jms.JMSException e ) {
      String msg = e.getMessage();
      // Expected message is about queue been closed already
      if ( msg != null && msg.indexOf( "PUT operation as the queue had been closed already" ) != -1 ) {
        System.out.println( e.getMessage() );
      } else {
        fail( "JMSException received with message" + e.getMessage() );
      }
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, null );
    }
  }

  @Test
  public void testAccessDeletedQueue() throws Exception {
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    table.put( "java.naming.factory.initial", TestConstants.jndiClass );
    table.put( "java.naming.provider.url", TestConstants.jndiUrl );
    InitialContext context = new InitialContext( table );
    QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
    QueueConnection qConn = qcf.createQueueConnection();
    QueueSession session = qConn.createQueueSession( true, 0 );
    TemporaryQueue tempQ = session.createTemporaryQueue();
    String qName = tempQ.getQueueName();
    QueueSender sender = session.createSender( tempQ );
    QueueReceiver receiver = session.createReceiver( tempQ );
    TextMessage message = session.createTextMessage();
    message.setText( "TEST" );
    message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
    message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );
    sender.send( message );

    tempQ.delete();

    try {
      TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
      assertEquals( "TEST", rcvMsg.getText() );
      fail( "Should have failed. Reason=closed queue." );
    } catch ( Exception e ) {
      String msg = e.getMessage();
      // Expected message is about queue been closed already
      if ( msg != null && msg.indexOf( "GET operation as the queue had been closed already" ) != -1 ) {
        System.out.println( e.getMessage() );
      } else {
        fail( "JMSException received with message" + e.getMessage() );
      }
      // assertEquals("[AFT-DME2-6300]: Temporary queue http://DME2LOCAL"
      // + qName + "is not available for GET operation.", e.getMessage());
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, tempQ, sender, null );
    }

  }

  @Test
  public void testDeleteTempQueueWithBoundListeners() throws Exception {
    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;

    TemporaryQueue destQ = null;
    TemporaryQueue replyQ = null;

    QueueSender sender = null;
    QueueReceiver receiver = null;

    try {
      Hashtable<String, Object> table = new Hashtable<String, Object>();
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      qConn = qcf.createQueueConnection();
      session = qConn.createQueueSession( true, 0 );

      destQ = session.createTemporaryQueue();
      replyQ = session.createTemporaryQueue();

      TextMessage message = session.createTextMessage();
      message.setJMSReplyTo( replyQ );
      message.setText( "TEST" );
      message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
      message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );

      sender = session.createSender( destQ );
      sender.send( message );

      receiver = session.createReceiver( replyQ );
      receiver.setMessageListener( new LocalQueueMessageListener( qConn, session, replyQ ) );

      DME2JMSQueueReceiver rec = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ ).get( 0 );
      assertNotNull( rec ); /* Checking if replyQ has associated receiver */
      assertTrue( rec.hasListeners() ); /*
                       * Checking if associated receiver
											 * has listener attached
											 */

			/* This should throw an exception */
      replyQ.delete();

      fail( "Error occured - Excepting a [AFT-DME2-6302] exception to be thrown" );
    } catch ( Exception e ) {
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "[AFT-DME2-6302]" ) );

    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, destQ, sender, receiver );
      JMSBaseTestCase.closeJMSResources( null, null, replyQ, null, null );
    }
  }

  @Test
  public void testDeleteTempQueueWithBoundReceiver() throws Exception {
    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;

    TemporaryQueue destQ = null;
    TemporaryQueue replyQ = null;

    QueueSender sender = null;

    try {
      Hashtable<String, Object> table = new Hashtable<String, Object>();
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      qConn = qcf.createQueueConnection();
      session = qConn.createQueueSession( true, 0 );

      destQ = session.createTemporaryQueue();
      replyQ = session.createTemporaryQueue();

      TextMessage message = session.createTextMessage();
      message.setJMSReplyTo( replyQ );
      message.setText( "TEST" );
      message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
      message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );

      sender = session.createSender( destQ );
      sender.send( message );

      final QueueReceiver receiver = session.createReceiver( replyQ );

      // Calling receive on new thread. Then on main thread, close the
      // replyQ while the receiver is still trying to receive the message
      Thread thr = new Thread( new Runnable() {

        @Override
        public void run() {
          try {
            receiver.receive();
          } catch ( JMSException e ) {
            fail( e.getMessage() );
          }
        }
      } );

      thr.start();

      Thread.sleep( 2000 );

      // This should throw an exception
      replyQ.delete();

      fail( "Error occured - Excepting a [AFT-DME2-6302] exception to be thrown" );
    } catch ( Exception e ) {
      assertTrue( e.getMessage().contains( "[AFT-DME2-6302]" ) );
      DME2JMSQueueReceiver rec = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ ).get( 0 );
      assertNotNull( rec ); // Checking if replyQ has associated receiver
      assertTrue( rec.isReceiverWaiting() ); // Checking if associated
      // receiver has listener
      // attached
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, destQ, sender, null );
      JMSBaseTestCase.closeJMSResources( null, null, replyQ, null, null );
    }
  }

  @Test
  public void testDeleteTempQueueAfterClosingReceiver() throws Exception {
    TemporaryQueue destQ = null;
    TemporaryQueue replyQ = null;

    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;

    QueueSender sender = null;
    QueueReceiver receiver = null;

    try {
      Hashtable<String, Object> table = new Hashtable<String, Object>();
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      qConn = qcf.createQueueConnection();
      session = qConn.createQueueSession( true, 0 );

      destQ = session.createTemporaryQueue();
      replyQ = session.createTemporaryQueue();

      TextMessage message = session.createTextMessage();
      message.setJMSReplyTo( replyQ );
      message.setText( "TEST" );
      message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
      message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );

      sender = session.createSender( destQ );
      sender.send( message );

      receiver = session.createReceiver( replyQ );
      DME2JMSQueueReceiver rec = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ ).get( 0 );
      assertNotNull( rec ); // Checking if replyQ has associated receiver

      receiver.receive( 1000 );

      receiver.close();
      replyQ.delete();
      destQ.delete();

      List<DME2JMSQueueReceiver> receivers = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ );
      assertNull( receivers ); // Since the above receiver was closed, there
      // shouldn't be any in the list
    } catch ( Exception e ) {
      fail( e.getMessage() );
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, destQ, sender, receiver );
      JMSBaseTestCase.closeJMSResources( null, null, replyQ, null, null );
    }
  }

  @Test
  public void testDeleteTempQueueWithMultipleReceivers() throws Exception {
    TemporaryQueue destQ = null;
    TemporaryQueue replyQ = null;

    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;

    QueueSender sender = null;
    QueueReceiver receiver = null;
    QueueReceiver receiver2 = null;

    try {
      Hashtable<String, Object> table = new Hashtable<String, Object>();
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      qConn = qcf.createQueueConnection();
      session = qConn.createQueueSession( true, 0 );

      destQ = session.createTemporaryQueue();
      replyQ = session.createTemporaryQueue();

      TextMessage message = session.createTextMessage();
      message.setJMSReplyTo( replyQ );
      message.setText( "TEST" );
      message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
      message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );

      sender = session.createSender( destQ );
      sender.send( message );

      receiver = session.createReceiver( replyQ );
      receiver2 = session.createReceiver( replyQ );

      List<DME2JMSQueueReceiver> receivers = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ );
      assertNotNull( receivers ); // Checking if replyQ has associated
      // receivers
      assertEquals( 2, DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ ).size() );

      // Close first receiver
      receiver.close();

      // Confirm first receiver was closed
      receivers = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ );
      assertNotNull( receivers ); // Checking if replyQ has associated
      // receivers
      assertEquals( 1, DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ ).size() );

      // Close 2nd receiver
      receiver2.close();

      // Confirm 2nd receiver was closed
      receivers = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ );
      assertNull( receivers ); // Checking if replyQ has associated
      // receivers

      // Delete tempQueue
      replyQ.delete();
      destQ.delete();

    } catch ( Exception e ) {
      fail( e.getMessage() );
    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, destQ, sender, receiver );
      JMSBaseTestCase.closeJMSResources( null, null, replyQ, null, receiver2 );
    }
  }

  @Ignore
  @Test
  public void testDeleteTempQueueWithCleanupDisabled() throws Exception {
    System.setProperty( "DME2_JMS_TEMP_QUEUE_REC_CLEANUP", "false" );

    TemporaryQueue destQ = null;
    TemporaryQueue replyQ = null;

    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;

    QueueSender sender = null;

    try {
      Hashtable<String, Object> table = new Hashtable<String, Object>();
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      qConn = qcf.createQueueConnection();
      session = qConn.createQueueSession( true, 0 );

      destQ = session.createTemporaryQueue();
      replyQ = session.createTemporaryQueue();

      TextMessage message = session.createTextMessage();
      message.setJMSReplyTo( replyQ );
      message.setText( "TEST" );
      message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
      message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );

      sender = session.createSender( destQ );
      sender.send( message );

      final QueueReceiver receiver = session.createReceiver( replyQ );

      List<DME2JMSQueueReceiver> receivers = DME2JMSManager.getDefaultInstance().getQueueReceivers( replyQ );
      assertNull( receivers ); // Since flag is false, this should be null.
      // Receivers shouldn't be added to map

      final AtomicBoolean failed = new AtomicBoolean( false );
      final AtomicBoolean ended = new AtomicBoolean( false );

      // Calling receive on new thread. Then on main thread, close the
      // replyQ while the receiver is still trying to receive the message
      Thread thr = new Thread( new Runnable() {

        @Override
        public void run() {
          try {
            receiver.receive( 60000 );
            ended.set( true );
          } catch ( JMSException e ) {
            e.printStackTrace();
            failed.set( true );
          }
        }
      } );

      thr.setDaemon( true );
      thr.start();

      Thread.sleep( 2000 );

      if ( ( (DME2JMSQueueReceiver) receiver ).isReceiverWaiting() ) {
        // Delete tempQueue. Shouldn't throw an exception
        replyQ.delete();
        assertFalse( failed.get() );
      } else {
        fail();
      }

      long duration = System.currentTimeMillis() + 60000;
      while ( !ended.get() ) {
        if ( System.currentTimeMillis() > duration && !ended.get() ) {
          fail( "Receiver failed to timeout." );
        }
        Thread.sleep( 1000 );
      }
    } finally {
      System.clearProperty( "DME2_JMS_TEMP_QUEUE_REC_CLEANUP" );
      System.clearProperty( "DME2.tempqueue.idletimeoutms" );

      JMSBaseTestCase.closeJMSResources( qConn, session, destQ, sender, null );
      JMSBaseTestCase.closeJMSResources( null, null, replyQ, null, null );

    }
  }

  @Test
  public void testCleanupTemporaryQueueCachedConnection() throws Exception {
    System.setProperty( "DME2.tempqueue.idletimeoutms", "10000" );
    System.setProperty( "DME2_TEMP_QUEUE_CLEANUP_INTERVAL_MS", "15000" );
    System.setProperty( "AFT_DME2_MANAGER_NAME", "testCleanupTemporaryQueueCachedConnection" );

    try {
      DME2JMSManager.getDefaultInstance().getLocalQueues().clear();
      System.out.println( "Starting size of local queues cache (Should be zero): "
          + DME2JMSManager.getDefaultInstance().getLocalQueues().size() );

      Hashtable<String, Object> table = new Hashtable<String, Object>();
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      QueueConnection qConn = qcf.createQueueConnection();
      final QueueSession session = qConn.createQueueSession( true, 0 );

      final AtomicBoolean failed = new AtomicBoolean( false );

      Runnable r = new Runnable() {
        @Override
        public void run() {
          try {
            session.createTemporaryQueue();
          } catch ( Exception e ) {
            e.printStackTrace();
            failed.set( true );
          }
        }
      };

      long duration = ( System.currentTimeMillis() + 30000 );

      while ( System.currentTimeMillis() < duration ) {
        Thread t = new Thread( r );
        t.setDaemon( true );
        t.start();
        Thread.sleep( 5000 );
        assertFalse( t.isAlive() );
        assertTrue( DME2JMSManager.getDefaultInstance().getLocalQueues().size() > 0 );
      }

      assertFalse( failed.get() );
      Thread.sleep( 300000 );

      Map<String, DME2JMSQueue> localQueueMap = DME2JMSManager.getDefaultInstance().getLocalQueues();
      List<DME2JMSTemporaryQueue> tempQueues = new ArrayList<DME2JMSTemporaryQueue>();

      System.out.println( "Current size of local queue cache: "
          + DME2JMSManager.getDefaultInstance().getLocalQueues().size() );

      for ( DME2JMSQueue queue : localQueueMap.values() ) {
        if ( queue instanceof DME2JMSTemporaryQueue ) {
          tempQueues.add( (DME2JMSTemporaryQueue) queue );
          System.out.println( queue );
        }
      }
      System.out.println( "TempQueue list size is: " + tempQueues.size() );
      assertEquals( 0, tempQueues.size() );
    } finally {
      System.clearProperty( "DME2.tempqueue.idletimeoutms" );
      System.clearProperty( "DME2_TEMP_QUEUE_CLEANUP_INTERVAL_MS" );
    }
  }

  @Test
  public void testTempQueueWithBoundListeners() throws Exception {
    QueueConnectionFactory qcf = null;
    QueueConnection qConn = null;
    QueueSession session = null;

    TemporaryQueue destQ = null;
    TemporaryQueue replyQ = null;

    QueueSender sender = null;
    QueueReceiver sreceiver = null;
    QueueReceiver receiver = null;

    try {
      Hashtable<String, Object> table = new Hashtable<String, Object>();
      table.put( "java.naming.factory.initial", TestConstants.jndiClass );
      table.put( "java.naming.provider.url", TestConstants.jndiUrl );
      InitialContext context = new InitialContext( table );
      qcf = (QueueConnectionFactory) context.lookup( TestConstants.clientConn );
      qConn = qcf.createQueueConnection();
      session = qConn.createQueueSession( true, 0 );

      destQ = session.createTemporaryQueue();
      replyQ = session.createTemporaryQueue();

      sreceiver = session.createReceiver( destQ );

      sreceiver.setMessageListener( new LocalQueueMessageListener( qConn, session, destQ ) );

      TextMessage message = session.createTextMessage();
      message.setJMSReplyTo( replyQ );
      message.setText( "TEST" );
      message.setStringProperty( "com.att.aft.dme2.jms.dataContext", "205977" );
      message.setStringProperty( "com.att.aft.dme2.jms.partner", "APPLE" );

      sender = session.createSender( destQ );

      receiver = session.createReceiver( replyQ );
      TestReceiveListener listener = new TestReceiveListener( session );
      receiver.setMessageListener( listener );
      sender.send( message );
      javax.jms.Message msg = listener.getResponse( 10000 );
      assertNotNull( msg ); /* Checking if replyQ has associated receiver */

			/* This should throw an exception */
      replyQ.delete();

      fail( "Error occured - Excepting a [AFT-DME2-6302] exception to be thrown" );
    } catch ( Exception e ) {
      e.printStackTrace();
      assertTrue( e.getMessage().contains( "[AFT-DME2-6302]" ) );

    } finally {
      JMSBaseTestCase.closeJMSResources( qConn, session, destQ, sender, receiver );
      JMSBaseTestCase.closeJMSResources( null, null, replyQ, null, null );
    }
  }

}
