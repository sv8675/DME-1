/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class MessageTest_testMessageProperties extends JMSBaseTestCase {

  @Test
  public void testMessageProperties() throws Exception {
    Properties props = RegistryFsSetup.init();
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    for (Object key : props.keySet()) {
      table.put((String) key, props.get(key));
    }
    table.put("java.naming.factory.initial", TestConstants.jndiClass);
    table.put("java.naming.provider.url", TestConstants.jndiUrl);
    InitialContext context = new InitialContext(table);
    QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
    QueueConnection qConn = qcf.createQueueConnection();
    QueueSession session = qConn.createQueueSession(true, 0);
    String requestQStr = "http://DME2LOCAL/service=MyQueueService/version=1.0/envContext=PROD/partner=BAU_SE";
    Queue requestQueue = (Queue) context.lookup(requestQStr);
    QueueSender sender = session.createSender(requestQueue);
    QueueReceiver receiver = session.createReceiver(requestQueue);

    byte X_FF = -1;
    double d = -0.1234567890;
    float f = (float) 1.01234;
    long curr = System.currentTimeMillis();
    TextMessage msg = session.createTextMessage();
    msg.setBooleanProperty("DME2-boolean-true", true);
    msg.setBooleanProperty("DME2-boolean-false", false);
    msg.setByteProperty("DME2-byte", X_FF);
    msg.setDoubleProperty("DME2-double", d);
    msg.setFloatProperty("DME2-float", f);
    msg.setIntProperty("DME2-int", 1);
    msg.setJMSCorrelationID("DME2-correlationid");
    msg.setJMSDeliveryMode( DeliveryMode.PERSISTENT);
    msg.setJMSDestination(requestQueue);
    msg.setJMSExpiration(System.currentTimeMillis() + 10000);
    msg.setJMSMessageID("DME2-msgid");
    msg.setJMSPriority( Message.DEFAULT_PRIORITY);
    msg.setJMSRedelivered(false);
    msg.setJMSTimestamp(curr);
    msg.setJMSType("DME2");

    sender.send(msg);
    try {
      Thread.sleep(1000);
    } catch (Exception ex) {
    }
    TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
    assertEquals(true, rcvMsg.getBooleanProperty("DME2-boolean-true"));
    assertEquals(false, rcvMsg.getBooleanProperty("DME2-boolean-false"));
    assertEquals(X_FF, rcvMsg.getByteProperty("DME2-byte"));
    assertEquals(d, rcvMsg.getDoubleProperty("DME2-double"), 0);
    assertEquals(f, rcvMsg.getFloatProperty("DME2-float"), 0);
    assertEquals(1, rcvMsg.getIntProperty("DME2-int"));
    assertEquals("DME2-correlationid", rcvMsg.getJMSCorrelationID());
    // assertEquals(DeliveryMode.PERSISTENT, rcvMsg.getJMSDeliveryMode());
    assertEquals("DME2-msgid", rcvMsg.getJMSMessageID());
    assertEquals(Message.DEFAULT_PRIORITY, rcvMsg.getJMSPriority());
    assertEquals(false, rcvMsg.getJMSRedelivered());
    assertEquals("DME2", rcvMsg.getJMSType());
  }
}
