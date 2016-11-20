/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;

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
import com.att.aft.dme2.test.jms.util.LocalQueueMsgSelectorListener;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class MessageSelectorToQueue extends JMSBaseTestCase {
	private InitialContext context;
	private QueueConnectionFactory factory;
	private QueueConnection connection;
	private QueueSession session;
	private Queue requestQueue;
	private Queue replyQueue;

	private String requestQStr = "http://DME2LOCAL/service=com.att.aft.MyLocalService/version=1.0.0/envContext=DEV/partner=BAU_ATL";
	private String replyQStr = "http://DME2LOCAL/myreplyQueue";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		System.setProperty("DME2.DEBUG", "true");
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
		context = new InitialContext(table);
	}


	@Test
	public void testMessageSelectorToQueue() throws Exception {
		System.setProperty("AFT_LATITUDE", String.valueOf(Locations.BHAM.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(Locations.BHAM.getLongitude()));

		factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = factory.createQueueConnection();
		session = connection.createQueueSession(true, 0);
		// request q.
		requestQueue = (Queue) context.lookup(requestQStr);
		QueueSender requestSender = session.createSender(requestQueue);
		QueueReceiver requestReceiver = session.createReceiver(requestQueue, "JMSCorrelationID=1");
		LocalQueueMsgSelectorListener listener = new LocalQueueMsgSelectorListener(connection, session, requestQueue,
				"1");
		// add Listener
		requestReceiver.setMessageListener(listener);

		// reply q
		replyQueue = (Queue) context.lookup(replyQStr);
		QueueReceiver replyReceiver = session.createReceiver(replyQueue);

		// msg.
		TextMessage requestMsg = session.createTextMessage();
		requestMsg.setText("JMSQueueTest");
		requestMsg.setJMSReplyTo(replyQueue);
		requestMsg.setJMSMessageID("1");
		// Since this is a local queue testing, setting JMSCorrelationID on
		// client side. For a remote queue, this wouldn't
		// be necessary
		requestMsg.setJMSCorrelationID("1");
		// send the msg.
		requestSender.send(requestMsg);

		Thread.sleep(1000);
		TextMessage replyMsg = (TextMessage) replyReceiver.receiveNoWait();
		System.out.println("replyMsg is: " + replyMsg);
		assertEquals("LocalQueueMessageListener:::JMSQueueTest", replyMsg.getText());
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if (session != null)
			session.close();
		if (connection != null)
			connection.close();
	}
}
