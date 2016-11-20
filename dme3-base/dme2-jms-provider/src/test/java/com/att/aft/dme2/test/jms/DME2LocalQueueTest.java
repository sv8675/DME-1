/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.jms.DME2JMSInitialContext;
import com.att.aft.dme2.jms.DME2JMSInitialContextFactory;
import com.att.aft.dme2.jms.DME2JMSLocalQueue;
import com.att.aft.dme2.jms.DME2JMSQueueConnection;
import com.att.aft.dme2.jms.DME2JMSQueueConnectionFactory;
import com.att.aft.dme2.jms.DME2JMSQueueReceiver;
import com.att.aft.dme2.jms.DME2JMSQueueSender;
import com.att.aft.dme2.jms.DME2JMSQueueSession;
import com.att.aft.dme2.jms.DME2JMSTextMessage;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.LocalQueueMessageListener;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class DME2LocalQueueTest extends JMSBaseTestCase {
	private DME2JMSInitialContext context;
	private DME2JMSQueueConnectionFactory factory;
	private DME2JMSQueueConnection connection;
	private DME2JMSQueueSession session;
	private DME2JMSLocalQueue requestQueue;
	private DME2JMSLocalQueue replyQueue;

	private LocalQueueMessageListener listener = null;

	// private String requestQStr =
	// "http://DME2LOCAL/service=MyLocalService/version=1.0/envContext=PROD/partner=BAU_ATL/routeOffer=BAU_ATL";
	private String requestQStr = "http://DME2LOCAL/service=MyLocalService/version=1.0/envContext=DEV/partner=BAU_ATL/routeOffer=BAU_ATL";
	private String clientQueueStr = "http://DME2LOCAL/ClientLocalQueue";
	private String replyQStr = "http://DME2LOCAL/service=MyReplyService/version=1.0/envContext=DEV/partner=BAU_SE";
	// private String replyQStr =
	// "http://DME2LOCAL/service=MyReplyService/version=1.0/envContext=PROD/partner=BAU_SE";
	private String clientQReplyStr = "http://DME2LOCAL/ClientLocalReplyQueue";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
		context = (DME2JMSInitialContext) new DME2JMSInitialContextFactory().getInitialContext(table);
	}

	@Test
	public void testCreateLocalQueue() throws Exception {
		factory = (DME2JMSQueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = (DME2JMSQueueConnection) factory.createQueueConnection();
		requestQueue = (DME2JMSLocalQueue) context.lookup(requestQStr);
		if (!(requestQueue instanceof DME2JMSLocalQueue)) {
			fail("Context lookup should have returned a DME2JMSLocalQueue.");
		}
		connection.close();
	}

	@Test
	public void testCreateLocalQueueWithLRMOverride() throws Exception {
		System.setProperty("lrmRO", "DUMMYTEST");
		try {
			factory = (DME2JMSQueueConnectionFactory) context.lookup(TestConstants.clientConn);
			connection = (DME2JMSQueueConnection) factory.createQueueConnection();
			requestQueue = (DME2JMSLocalQueue) context.lookup(requestQStr);
			if (!(requestQueue instanceof DME2JMSLocalQueue)) {
				fail("Context lookup should have returned a DME2JMSLocalQueue.");
			}
			if (requestQueue.toString().indexOf("DUMMYTEST") < 0) {
				fail("Did not find routeOffer=DUMMYTEST routeOffer set");
			}
		} finally {
			System.clearProperty("lrmRO");
			connection.close();
		}
	}

	@Test
	public void testAddRemoveListenerToLocalQueue() throws Exception {
		System.setProperty("AFT_LATITUDE", String.valueOf(Locations.BHAM.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(Locations.BHAM.getLongitude()));

		factory = (DME2JMSQueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = (DME2JMSQueueConnection) factory.createQueueConnection();
		session = (DME2JMSQueueSession) connection.createQueueSession(true, 0);
		requestQueue = (DME2JMSLocalQueue) context.lookup(requestQStr);

		// request queues...
		DME2JMSQueueSender sender = (DME2JMSQueueSender) session.createSender(requestQueue);
		DME2JMSQueueReceiver requestReceiver = (DME2JMSQueueReceiver) session.createReceiver(requestQueue);

		listener = new LocalQueueMessageListener(connection, session, requestQueue);
		// adds the listener once
		requestReceiver.setMessageListener(listener);
		// adds the listener again...
		requestQueue.addListener(requestReceiver, listener, null);
		System.out.println("testAddRemoveListenerToLocalQueue: requestQueue.getListeners().size()="
				+ requestQueue.getListeners().size());

		// reply to queue...
		replyQueue = (DME2JMSLocalQueue) context.lookup(replyQStr);
		DME2JMSQueueReceiver receiver = (DME2JMSQueueReceiver) session.createReceiver(replyQueue);

		String msg = "TEST_LOCALQUEUE";
		DME2JMSTextMessage tm = (DME2JMSTextMessage) session.createTextMessage(msg);
		tm.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		tm.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		tm.setJMSReplyTo(replyQueue);
		tm.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
		tm.setJMSCorrelationID("LocalQueueTest.CORRELATIONID");
		sender.send(tm);
		Thread.sleep(1000);
		DME2JMSTextMessage replyMsg = (DME2JMSTextMessage) receiver.receive(1000);
		assertNotNull(replyMsg);
		assertEquals("LocalQueueMessageListener:::" + msg, replyMsg.getText());

		requestQueue.removeListener(requestReceiver);
	}

	@Test
	public void testAddRemoveListenerToClientLocalQueue() throws Exception {
		System.setProperty("AFT_LATITUDE", String.valueOf(Locations.BHAM.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(Locations.BHAM.getLongitude()));

		factory = (DME2JMSQueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = (DME2JMSQueueConnection) factory.createQueueConnection();
		session = (DME2JMSQueueSession) connection.createQueueSession(true, 0);
		System.out.println("testAddRemoveListenerToClientLocalQueue: requestQueue string=" + this.clientQueueStr);
		requestQueue = (DME2JMSLocalQueue) context.lookup(this.clientQueueStr);

		// request queues...
		DME2JMSQueueSender sender = (DME2JMSQueueSender) session.createSender(requestQueue);
		DME2JMSQueueReceiver requestReceiver = (DME2JMSQueueReceiver) session.createReceiver(requestQueue);

		listener = new LocalQueueMessageListener(connection, session, requestQueue);
		// adds the listener once
		requestReceiver.setMessageListener(listener);
		// adds the listener again...
		// requestQueue.addListener(requestReceiver, listener, null);
		System.out.println("testAddRemoveListenerToClientLocalQueue: requestQueue.getListeners().size()="
				+ requestQueue.getListeners().size());

		// reply to queue...
		replyQueue = (DME2JMSLocalQueue) context.lookup(clientQReplyStr);
		DME2JMSQueueReceiver receiver = (DME2JMSQueueReceiver) session.createReceiver(replyQueue);

		String msg = "TEST_LOCALQUEUE";
		DME2JMSTextMessage tm = (DME2JMSTextMessage) session.createTextMessage(msg);
		tm.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		tm.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		tm.setJMSReplyTo(replyQueue);
		tm.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
		tm.setJMSCorrelationID("LocalQueueTest.CORRELATIONID");
		sender.send(tm);
		Thread.sleep(1000);
		DME2JMSTextMessage replyMsg = (DME2JMSTextMessage) receiver.receive(1000);
		assertNotNull(replyMsg);
		assertEquals("LocalQueueMessageListener:::" + msg, replyMsg.getText());

		requestQueue.removeListener(requestReceiver);
	}

	@Test
	public void testConfigureQueueDrainingThreads() throws Exception {
		System.setProperty("AFT_LATITUDE", String.valueOf(Locations.BHAM.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(Locations.BHAM.getLongitude()));

		factory = (DME2JMSQueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = (DME2JMSQueueConnection) factory.createQueueConnection();
		session = (DME2JMSQueueSession) connection.createQueueSession(true, 0);
		requestQueue = (DME2JMSLocalQueue) context.lookup(requestQStr);

		// request queues...
		DME2JMSQueueSender sender = (DME2JMSQueueSender) session.createSender(requestQueue);
		DME2JMSQueueReceiver requestReceiver = (DME2JMSQueueReceiver) session.createReceiver(requestQueue);

		assertEquals(requestQueue.getListeners().size(), 0);
		// add first listener
		listener = new LocalQueueMessageListener(connection, session, requestQueue);
		requestReceiver.setMessageListener(listener);
		assertEquals(requestQueue.getListeners().size(), 1);
		// add second listener
		listener = new LocalQueueMessageListener(connection, session, requestQueue);
		requestReceiver.setMessageListener(listener);
		// add third listener
		listener = new LocalQueueMessageListener(connection, session, requestQueue);
		requestReceiver.setMessageListener(listener);
		assertEquals(requestQueue.getListeners().size(), 3);
		System.out.println("testConfigureQueueDrainingThreads: requestQueue.getListeners().size()="
				+ requestQueue.getListeners().size());

		// reply to queue...
		replyQueue = (DME2JMSLocalQueue) context.lookup(replyQStr);
		DME2JMSQueueReceiver receiver = (DME2JMSQueueReceiver) session.createReceiver(replyQueue);

		String msg = "TEST_LOCALQUEUE";
		DME2JMSTextMessage tm = (DME2JMSTextMessage) session.createTextMessage(msg);
		tm.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		tm.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		tm.setJMSReplyTo(replyQueue);
		tm.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
		tm.setJMSCorrelationID("LocalQueueTest.CORRELATIONID");
		sender.send(tm);
		Thread.sleep(1000);
		DME2JMSTextMessage replyMsg = (DME2JMSTextMessage) receiver.receive(1000);
		assertNotNull(replyMsg);
		assertEquals("LocalQueueMessageListener:::" + msg, replyMsg.getText());

		requestQueue.removeListener(requestReceiver);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if (listener != null)
			listener.stop();

		if (connection != null) {
			connection.close();
		}
	}
}
