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

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class TestJMSAuthentication extends JMSBaseTestCase {
	private ServerLauncher launcher = null;
	/** The manager. */
	DME2Manager manager = null;

	/*
	 * (non-Javadoc)
	 * 
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		System.setProperty("java.security.auth.login.config", "src/test/etc/mylogin.conf");
		System.setProperty("org.eclipse.jetty.util.log.DEBUG", "true");
//		manager = new DME2Manager("TestDME2Manager", RegistryFsSetup.init());
		Locations.BHAM.set();
	}

	/**
	 * Test client authentication.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testJMSAuthentication() throws Exception {
		Locations.BHAM.set();
		Properties props = RegistryFsSetup.init();

		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection("test", "test");
		QueueSession session = connection.createQueueSession(true, 0);
		Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
		// remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

		// start service
		launcher = new ServerLauncher(null, "-city", "BHAM");
		launcher.launchTestJMSAuthServer();
		Thread.sleep(3000);

		QueueSender sender = session.createSender(remoteQueue);

		// Queue replyToQueue = session.createTemporaryQueue();

		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
		Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
		msg.setJMSReplyTo(replyToQueue);

		sender.send(msg);
		// QueueReceiver replyReceiver = session.createReceiver(replyToQueue,
		// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
		QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
		try {
			Thread.sleep(1000);
		} catch (Exception ex) {
		}
		TextMessage rcvMsg = (TextMessage) replyReceiver.receive(3000);
		// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
		assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		try {
			launcher.destroy();
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// String selector = replyReceiver.getMessageSelector();
		// fail(selector);
	}

	@Test
	public void testJMSAuthenticationViaURI() throws Exception {
		Locations.BHAM.set();
		Properties props = RegistryFsSetup.init();

		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection();
		QueueSession session = connection.createQueueSession(true, 0);
		Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr + "?userName=test&password=test");
		// remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

		// start service
		launcher = new ServerLauncher(null, "-city", "BHAM");
		launcher.launchTestJMSAuthServer();
		Thread.sleep(3000);

		QueueSender sender = session.createSender(remoteQueue);

		// Queue replyToQueue = session.createTemporaryQueue();

		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
		Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
		msg.setJMSReplyTo(replyToQueue);

		sender.send(msg);
		// QueueReceiver replyReceiver = session.createReceiver(replyToQueue,
		// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
		QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
		try {
			Thread.sleep(1000);
		} catch (Exception ex) {
		}
		TextMessage rcvMsg = (TextMessage) replyReceiver.receive(3000);
		// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
		assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		try {
			launcher.destroy();
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// String selector = replyReceiver.getMessageSelector();
		// fail(selector);
	}

	/**
	 * Test client authentication.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testJMSClientAuthenticationFailure() throws Exception {
		Locations.BHAM.set();
		Properties props = RegistryFsSetup.init();

		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection();
		QueueSession session = connection.createQueueSession(true, 0);
		Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
		// remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

		// start service
		launcher = new ServerLauncher(null, "-city", "BHAM");
		launcher.launchTestJMSAuthServer();
		Thread.sleep(3000);

		QueueSender sender = session.createSender(remoteQueue);

		// Queue replyToQueue = session.createTemporaryQueue();

		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
		Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
		msg.setJMSReplyTo(replyToQueue);

		sender.send(msg);
		// QueueReceiver replyReceiver = session.createReceiver(replyToQueue,
		// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
		QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
		try {
			Thread.sleep(1000);
		} catch (Exception ex) {
		}
		try {
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(3000);
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("AFT-DME2-5401"));
		}
		try {
			launcher.destroy();
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
		// assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		// String selector = replyReceiver.getMessageSelector();
		// fail(selector);
	}

	public static void main(String a[]) throws Exception {
		TestJMSAuthentication ta = new TestJMSAuthentication();
		ta.setUp();
		ta.testJMSClientAuthenticationFailure();
	}

}
