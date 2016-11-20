/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class RemoteServiceTest_testContinuationReplyTest extends JMSBaseTestCase {

  private static final Logger logger = LoggerFactory.getLogger( RemoteServiceTest_testContinuationReplyTest.class );

  private DME2Configuration config = null;

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }
  /**
   * The below test case is to test a reply using DME2REPLY pattern
   * ContinuationQueueTestMessageListener onMessage method uses DME2REPLY
   * lookup for sending reply. Also the onMessage sleeps for 2 mins to
   * validate the continuation queue is not removed by
   * cleanupContinuationQueue thread pre emptive
   *
   * @throws Exception
   */


  @Test
  public void testContinuationReplyTest() throws Exception {
    Locations.BHAM.set();
    ServerLauncher launcher = null;
    try {
      Properties props = RegistryFsSetup.init();

      Hashtable<String,Object> table = new Hashtable<String,Object>();
      for (Object key: props.keySet()) {
        table.put((String)key, props.get(key));
      }
      table.put("java.naming.factory.initial", TestConstants.jndiClass);
      table.put("java.naming.provider.url", TestConstants.jndiUrl);
      InitialContext context = new InitialContext(table);
      QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
      QueueConnection connection = factory.createQueueConnection();
      QueueSession session = connection.createQueueSession(true, 0);
      Queue remoteQueue = (Queue)context.lookup(TestConstants.continuationReplyServiceSearchStr);
      // remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

      launcher = new ServerLauncher(null, "-city","BHAM");
      launcher.launchContinuationReplyJMSServer();

      Thread.sleep(10000);

      QueueSender sender = session.createSender(remoteQueue);

      // Queue replyToQueue = session.createTemporaryQueue();

      TextMessage msg = session.createTextMessage();
      msg.setText("TEST");
      msg.setStringProperty("com.att.aft.dme2.jms.dataContext",
          TestConstants.dataContext);
      msg.setStringProperty("com.att.aft.dme2.jms.partner",
          TestConstants.partner);
      msg.setLongProperty("com.att.aft.dme2.perEndpointTimeoutMs", 140000);
      Queue replyToQueue = (Queue) context
          .lookup("http://DME2LOCAL/clientResponseQueue");
      msg.setJMSReplyTo(replyToQueue);
      msg.setJMSExpiration(125000);

      long start = System.currentTimeMillis();
      sender.send(msg);
      //QueueReceiver replyReceiver = session.createReceiver(replyToQueue,
      //		"JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
      QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
      try {
        Thread.sleep(1000);
      } catch (Exception ex) {
      }
      TextMessage rcvMsg = (TextMessage) replyReceiver.receive(160000);
      long elapsedTime = System.currentTimeMillis() - start;
      // Currently the longrunListener is forced to sleep for 90000. So make sure any change for below assert
      // is directly depending on LongRunMessageLister impl
      System.out.println(" ContinuationReplyMessageListener responded after " + elapsedTime + " ms");
      assertTrue(elapsedTime>120000);
      //TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
      assertEquals("ContinuationQueueTestMessageListener:::TEST", rcvMsg.getText());
      System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
      //String selector = replyReceiver.getMessageSelector();
      //fail(selector);
    }
    finally {
      TestConstants.removePortCache();
      if(launcher != null) {
        try {
          launcher.destroy();
        }
        catch(Exception e){}
      }
    }
  }
}
