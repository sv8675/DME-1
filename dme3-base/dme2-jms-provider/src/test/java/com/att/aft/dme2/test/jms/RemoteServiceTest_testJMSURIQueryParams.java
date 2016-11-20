/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

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

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.test.jms.servlet.EchoResponseServlet;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class RemoteServiceTest_testJMSURIQueryParams extends JMSBaseTestCase {

  private static final Logger logger = LoggerFactory.getLogger( RemoteServiceTest_testJMSURIQueryParams.class );

  private DME2Configuration config = null;

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testJMSURIQueryParams() throws Exception {
    String name = "service=com.att.aft.TestJMSURIQueryParams/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
    String cname = "http://DME2RESOLVE/service=com.att.aft.TestJMSURIQueryParams/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
    DME2Manager manager = null;
    try {
      System.setProperty("DME2.DEBUG", "true");
      System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
      Properties props = RegistryFsSetup.init();
      config = new DME2Configuration("testJMSURIQueryParams", props);
      manager = new DME2Manager("testJMSURIQueryParams", config);

      manager.bindServiceListener(name, new EchoResponseServlet(name, "bau_se_1"), null, null, null);
      Thread.sleep(5000);

      Hashtable<String, Object> table = new Hashtable<String, Object>();
      for (Object key : props.keySet()) {
        table.put((String) key, props.get(key));
      }
      table.put("java.naming.factory.initial", TestConstants.jndiClass);
      table.put("java.naming.provider.url", TestConstants.jndiUrl);
      InitialContext context = new InitialContext(table);
      QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
      Queue remoteQueue = (Queue) context.lookup(cname);

      System.out.println("Looking up reply Queue");
      Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

      System.out.println("Creating QueueConnection");
      QueueConnection conn = qcf.createQueueConnection();

      System.out.println("Creating Session");
      QueueSession session = conn.createQueueSession(true, 0);

      QueueSender sender = session.createSender(remoteQueue);

      TextMessage msg = session.createTextMessage();
      msg.setText("TEST");
      msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
      msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
      msg.setStringProperty("com.att.aft.dme2.jms.queryParams", "TEST_ARG1=PARAM 1&TEST_ARG2=PARAM 2");

      msg.setJMSReplyTo(replyToQueue);
      QueueReceiver replyReceiver = null;
      sender.send(msg);
      // QueueReceiver replyReceiver =
      // session.createReceiver(replyToQueue,
      // "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
      replyReceiver = session.createReceiver(replyToQueue);

      Thread.sleep(1000);

      TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
      // TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
      assertTrue(rcvMsg.getText().contains("TEST_ARG:"));

    } finally {
      manager.unbindServiceListener(name);
      System.clearProperty("DME2.DEBUG");
      System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
    }
  }

}
