/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.jms.DME2JMSManager;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class TestRemoveHeaders extends JMSBaseTestCase {

	/*
	 * DME2_FILTER_HTTP_HEADERS false
	 */
	@Ignore	
	@Test
	public void testDoNotFilterHeaders() throws Exception {
		try {
			System.out.println("----Starting testDoNotFilterHeaders(). expecting FOO header");
			System.setProperty("DME2_FILTER_HTTP_HEADERS", "false");
			String service = "service=com.att.aft.dme2.test.testRemoveHeaders.DoNotFilterHeader/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

			TextMessage replyMsg = null;
			List<String> headers = new ArrayList<String>();
			replyMsg = send(service);
			System.out.println(replyMsg);
			System.out.println("reply headers: ");
			Enumeration<?> e = replyMsg.getPropertyNames();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				headers.add(key);
				String debugValue = replyMsg.getStringProperty(key);
				System.out.println(key + " : " + debugValue);
			}

			assertTrue("FOO header was improperly stripped", headers.contains("FOO"));
		} finally {
			System.out.println("----Completed  testDoNotFilterHeaders expected FOO Header");
			System.clearProperty("DME2_FILTER_HTTP_HEADERS");
			System.clearProperty("DME2_HTTP_HEADERS_TO_REMOVE");
		}
	}

	/*
	 * DME2_FILTER_HTTP_HEADERS true
	 */
	
	@Test
	@Ignore
	public void testFilterHeaders() throws Exception {
		try {
			System.out.println("----Starting testOverrideContentLengthFilterHeaders() expecting FOO header stripped");
			// do we even need this anymore?
			System.setProperty("DME2_FILTER_HTTP_HEADERS", "true");
			System.setProperty("DME2_HTTP_HEADERS_TO_REMOVE", "FOO");

			String service = "service=com.att.aft.dme2.test.testRemoveHeaders.TestFilterHeaders/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

			TextMessage replyMsg = send(service);
			System.out.println("reply headers: ");
			Enumeration<?> e = replyMsg.getPropertyNames();
			List<String> headers = new ArrayList<String>();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				headers.add(key);
				String debugValue = replyMsg.getStringProperty(key);
				System.out.println(key + " : " + debugValue);
			}

			assertTrue("Received an enexpected null reply", replyMsg != null);
			assertFalse("Expected to strip the FOO header from the response but did not.", headers.contains("FOO"));

		} finally {
			System.out.println("----Completed testOverrideContentLengthFilterHeaders() expected 0 content-length");
			System.clearProperty("DME2_FILTER_HTTP_HEADERS");
			System.clearProperty("DME2_HTTP_HEADERS_TO_REMOVE");
		}
	}

	private TextMessage send(String service) throws Exception {

		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection qConn = qcf.createQueueConnection();
		QueueSession session = qConn.createQueueSession(true, 0);

		String requestQStr = "http://DME2LOCAL/" + service + "?server=true";
		String sendQStr = "http://DME2RESOLVE/" + service;

		// Use createQueue to create local queue.
		Queue serviceQueue = session.createQueue(requestQStr);
		Queue sendQueue = session.createQueue(sendQStr);
		Queue replyQueue = session.createTemporaryQueue();

		TextMessage testMsg = session.createTextMessage("Test Message");
		testMsg.setText("not empty");
		testMsg.setJMSReplyTo(replyQueue);
		testMsg.setStringProperty("Content-Type", "xml");
		// add the header that we intend to test with
		testMsg.setStringProperty("FOO", "BAR");
		try {
			QueueReceiver requestQueueReceiver = session.createReceiver(serviceQueue);
			MessageListener messageListener = new HeaderCopyingReplyListener(session);

			// add Listener
			requestQueueReceiver.setMessageListener(messageListener);
			Thread.sleep(10000);

			QueueSender queueSender = session.createSender(sendQueue);
			queueSender.send(testMsg);
			Thread.sleep(5000);

			QueueReceiver replyQueueReceiver = session.createReceiver(replyQueue);
			TextMessage replyMsg = (TextMessage) replyQueueReceiver.receive(30000);

			return replyMsg;
		} finally {
			((DME2EndpointRegistryGRM) DME2JMSManager.getDefaultInstance().getDME2Manager().getEndpointRegistry())
					.shutdown();
		}
	}

	private class HeaderCopyingReplyListener implements MessageListener {

		private Session session;

		public HeaderCopyingReplyListener(Session session) {
			this.session = session;
		}

		@Override
		public void onMessage(Message m) {
			try {

				System.out.println("request headers: ");
				Enumeration<?> en = m.getPropertyNames();
				while (en.hasMoreElements()) {
					String key = (String) en.nextElement();
					String debugValue = m.getStringProperty(key);
					System.out.println(key + " : " + debugValue);
				}
				Destination d = m.getJMSReplyTo();
				if (d != null) {
					MessageProducer sender = session.createProducer((Queue) d);
					TextMessage replyMsg = session.createTextMessage();
					// dynamically build response and length based on headers.
					replyMsg.setText(((TextMessage) m).getText());
					// copy all of the request headers into the reply headers
					en = m.getPropertyNames();
					while (en.hasMoreElements()) {
						String key = (String) en.nextElement();
						String debugValue = m.getStringProperty(key);
						replyMsg.setStringProperty(key, debugValue);
					}

					replyMsg.setJMSDestination(d);
					replyMsg.setJMSCorrelationID(m.getJMSMessageID());
					// lets add a new reply header
					replyMsg.setStringProperty("BAR", "FOO");
					sender.send(replyMsg);
				}

			} catch (JMSException e) {
				e.printStackTrace();
			}
		}

	}

}
