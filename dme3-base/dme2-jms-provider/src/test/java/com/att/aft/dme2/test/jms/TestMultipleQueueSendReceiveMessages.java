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
import com.att.aft.dme2.test.jms.util.LocalQueueMessageListener;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class TestMultipleQueueSendReceiveMessages extends JMSBaseTestCase {
	private InitialContext context;
	private QueueConnectionFactory factory;
	private QueueConnection connection;
	private QueueSession session;
	private Queue requestQueueA;
	private Queue clientDestA;
	private Queue replyQueueA;

	private Queue requestQueueB;
	private Queue clientDestB;
	private Queue replyQueueB;

	private Queue requestQueueC;
	private Queue clientDestC;
	private Queue replyQueueC;

	private String requestQStrA = "http://DME2LOCAL/service=com.att.aft.MyMultiLocalQueueA/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String resolveQStrA = "http://DME2RESOLVE/service=com.att.aft.MyMultiLocalQueueA/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String replyQStrA = "http://DME2LOCAL/myreplyQueueA";

	private String requestQStrB = "http://DME2LOCAL/service=com.att.aft.MyMultiLocalQueueB/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String resolveQStrB = "http://DME2RESOLVE/service=com.att.aft.MyMultiLocalQueueB/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String replyQStrB = "http://DME2LOCAL/myreplyQueueB";

	private String requestQStrC = "http://DME2LOCAL/service=com.att.aft.MyMultiLocalQueueC/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String resolveQStrC = "http://DME2RESOLVE/service=com.att.aft.MyMultiLocalQueueC/version=1.0.0/envContext=DEV/routeOffer=BAU_ATL";
	private String replyQStrC = "http://DME2LOCAL/myreplyQueueC";

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
	public void testMultipleQueueSendReceiveMessages() throws Exception {
		factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = factory.createQueueConnection();
		session = connection.createQueueSession(true, 0);
		// request qA.
		requestQueueA = (Queue) context.lookup(requestQStrA);
		clientDestA = (Queue) context.lookup(resolveQStrA);
		QueueSender requestSenderA = session.createSender(clientDestA);
		QueueReceiver requestReceiverA = session.createReceiver(requestQueueA);
		LocalQueueMessageListener listenerA = new LocalQueueMessageListener(connection, session, requestQueueA);
		// add Listener
		requestReceiverA.setMessageListener(listenerA);

		// reply q
		replyQueueA = (Queue) context.lookup(replyQStrA);
		QueueReceiver replyReceiverA = session.createReceiver(replyQueueA);

		// msg.
		TextMessage requestMsgA = session.createTextMessage();
		requestMsgA.setText("JMSQueueTestA");
		requestMsgA.setJMSReplyTo(replyQueueA);
		requestMsgA.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");		
		requestMsgA.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
		// send the msg.
		requestSenderA.send(requestMsgA);

		Thread.sleep(50000);
		TextMessage replyMsgA = (TextMessage) replyReceiverA.receiveNoWait();
		assertEquals("LocalQueueMessageListener:::JMSQueueTestA", replyMsgA.getText());

		// request qB.
		requestQueueB = (Queue) context.lookup(requestQStrB);
		clientDestB = (Queue) context.lookup(resolveQStrB);
		QueueSender requestSenderB = session.createSender(clientDestB);
		QueueReceiver requestReceiverB = session.createReceiver(requestQueueB);
		LocalQueueMessageListener listenerB = new LocalQueueMessageListener(connection, session, requestQueueB);
		// add Listener
		requestReceiverB.setMessageListener(listenerB);

		// reply q
		replyQueueB = (Queue) context.lookup(replyQStrB);
		QueueReceiver replyReceiverB = session.createReceiver(replyQueueB);

		// msg.
		TextMessage requestMsgB = session.createTextMessage();
		requestMsgB.setText("JMSQueueTestB");
		requestMsgB.setJMSReplyTo(replyQueueB);
		requestMsgB.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
		requestMsgB.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");				
		// send the msg.
		requestSenderB.send(requestMsgB);

		Thread.sleep(50000);
		TextMessage replyMsgB = (TextMessage) replyReceiverB.receiveNoWait();
		assertEquals("LocalQueueMessageListener:::JMSQueueTestB", replyMsgB.getText());

		// request qB.
		requestQueueC = (Queue) context.lookup(requestQStrC);
		clientDestC = (Queue) context.lookup(resolveQStrC);
		QueueSender requestSenderC = session.createSender(clientDestC);
		QueueReceiver requestReceiverC = session.createReceiver(requestQueueC);
		LocalQueueMessageListener listenerC = new LocalQueueMessageListener(connection, session, requestQueueC);
		// add Listener
		requestReceiverC.setMessageListener(listenerC);

		// reply q
		replyQueueC = (Queue) context.lookup(replyQStrC);
		QueueReceiver replyReceiverC = session.createReceiver(replyQueueC);

		// msg.
		TextMessage requestMsgC = session.createTextMessage();
		requestMsgC.setText("JMSQueueTestC");
		requestMsgC.setJMSReplyTo(replyQueueC);
		requestMsgC.setJMSMessageID(String.valueOf(System.currentTimeMillis()));
		// send the msg.
		requestSenderC.send(requestMsgC);

		Thread.sleep(10000);
		TextMessage replyMsgC = (TextMessage) replyReceiverC.receiveNoWait();
		assertEquals("LocalQueueMessageListener:::JMSQueueTestC", replyMsgC.getText());

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
