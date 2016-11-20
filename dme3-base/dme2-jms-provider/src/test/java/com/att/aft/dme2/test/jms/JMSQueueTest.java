/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

import com.att.aft.dme2.jms.DME2JMSQueueConnection;
import com.att.aft.dme2.jms.DME2JMSQueueConnectionFactory;
import com.att.aft.dme2.jms.DME2JMSRemoteQueue;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class JMSQueueTest extends JMSBaseTestCase {
	private InitialContext context;
	private QueueConnectionFactory factory;
	private QueueConnection connection;
	private QueueSession session;
	private Queue requestQueue;
	private Queue requestQueueA;
	private Queue requestQueueB;
	private Queue requestQueueC;
	private String requestQStr = "http://DME2LOCAL/service=com.att.aft.MyLocalService/version=1.0.0/envContext=DEV/partner=BAU_ATL";
	private String requestQStrA = "http://DME2LOCAL/service=com.att.aft.MyMultiLocalQueueA/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String resolveQStrA = "http://DME2RESOLVE/service=com.att.aft.MyMultiLocalQueueA/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String requestQStrB = "http://DME2LOCAL/service=com.att.aft.MyMultiLocalQueueB/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String requestQStrC = "http://DME2LOCAL/service=com.att.aft.MyMultiLocalQueueC/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";

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
	public void testCreateQueue() throws Exception {
		factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = factory.createQueueConnection();
		session = connection.createQueueSession(true, 0);
		requestQueue = (Queue) context.lookup(requestQStr);
	}

	@Test
	public void testCreateResolveQueueWithLRMOverride() throws Exception {
		System.setProperty("lrmRO", "DUMMYTEST");
		try {
			factory = (DME2JMSQueueConnectionFactory) context.lookup(TestConstants.clientConn);
			connection = (DME2JMSQueueConnection) factory.createQueueConnection();
			requestQueue = (DME2JMSRemoteQueue) context.lookup(resolveQStrA);

			if (requestQueue.toString().indexOf("DUMMYTEST") > -1) {
				fail("Oh no found routeOffer=DUMMYTEST routeOffer set in a DME2Remote URI resolved from a DME2RESOLVE URI");
			}
			if (requestQueue.toString().indexOf("BAU_ATL") < 0) {
				fail("Oh no somehow we are completely missing the BAU_ATL routeOffer information from this uri (unexpected)");
			}
		} finally {
			System.clearProperty("lrmRO");
			connection.close();
		}
	}

	@Test
	public void testMultipleQueue() throws Exception {
		factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = factory.createQueueConnection();
		session = connection.createQueueSession(true, 0);
		requestQueueA = (Queue) context.lookup(requestQStrA);
		requestQueueB = (Queue) context.lookup(requestQStrB);
		requestQueueC = (Queue) context.lookup(requestQStrC);
	}

	@Test
	public void testQueueSendReceiveMessages() throws Exception {
		factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = factory.createQueueConnection();
		session = connection.createQueueSession(true, 0);
		requestQueue = (Queue) context.lookup(requestQStr);
		QueueSender sender = session.createSender(requestQueue);
		QueueReceiver receiver = session.createReceiver(requestQueue);
		TextMessage message = session.createTextMessage();
		message.setText("JMSQueueTest");
		message.setJMSExpiration(System.currentTimeMillis() + 10000);
		sender.send(message);

		Thread.sleep(1000);

		TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
		assertEquals("JMSQueueTest", rcvMsg.getText());
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
