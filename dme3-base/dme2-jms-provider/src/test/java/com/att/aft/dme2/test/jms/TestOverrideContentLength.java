/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.jms.DME2JMSManager;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;
import com.att.aft.dme2.util.DME2Constants;

@Ignore
public class TestOverrideContentLength extends JMSBaseTestCase {

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	// Test for empty reply messages, with overwrite headers enabled
	/*
	 * ENABLE_CONTENT_LENGTH = true
	 */
	@Test
	@Ignore
	public void testOverrideContentLengthByDefault() throws Exception {
		try {
			System.out.println("----Starting testOverrideContentLengthByDefault() - expecting 0 content-length");
			String service = "service=com.att.aft.dme2.test.testOverrideContentLength.OverrideContentLengthByDefault/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
			TextMessage replyMsg = send(service);

			System.out.println("reply headers: ");
			Enumeration<?> e = replyMsg.getPropertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String debugValue = replyMsg.getStringProperty(key);
				System.out.println(key + " : " + debugValue);
			}

			assertTrue("Received an enexpected null reply", replyMsg != null);
			assertEquals(String.format("expected a zero length content-length property, and instead got %s",
					replyMsg.getIntProperty("Content-Length")), 0, replyMsg.getIntProperty("Content-Length"));
		} finally {
			System.out.println("----Completed testOverrideContentLengthByDefault() - expected 0 content length");
			System.clearProperty("DME2_FILTER_HTTP_HEADERS");
			System.clearProperty("AFT_DME2_SET_RESLEN");

		}
	}

	// Test for empty reply messages, with overwrite headers enabled
	/*
	 * ENABLE_CONTENT_LENGTH = true
	 */
	@Test
	@Ignore
	public void testOverrideContentLength() throws Exception {
		try {
			System.out.println("----Starting testOverrideContentLength() - expecting 0 content-length");

			System.setProperty("AFT_DME2_SET_RESLEN", "true");

			String service = "service=com.att.aft.dme2.test.testOverrideContentLength.OverrideContentLength/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

			TextMessage replyMsg = send(service);

			System.out.println("reply headers: ");
			Enumeration<?> e = replyMsg.getPropertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String debugValue = replyMsg.getStringProperty(key);
				System.out.println(key + " : " + debugValue);
			}

			assertTrue("Received an enexpected null reply", replyMsg != null);
			assertEquals(String.format("expected a zero length content-length property, and instead got %s",
					replyMsg.getIntProperty("Content-Length")), 0, replyMsg.getIntProperty("Content-Length"));
		} finally {
			System.out.println("----Completed testOverrideContentLength() - expected 0 content length");
			System.clearProperty("DME2_FILTER_HTTP_HEADERS");
			System.clearProperty("AFT_DME2_SET_RESLEN");
		}
	}

	/*
	 * DME2_FILTER_HTTP_HEADERS false && ENABLE_CONTENT_LENGTH = false
	 */

	// There does not appear to be a way to completely eliminate Content-Length in Jetty 9
	@Test
	@Ignore
	public void testInvalidContentLength() throws Exception {
		try {
			System.out.println("----Starting testInvalidContentLength() expecting early EOF");
			System.setProperty( DME2Constants.ENABLE_CONTENT_LENGTH, "false");

			String service = "service=com.att.aft.dme2.test.testOverrideContentLength.InvalidContentLength/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
			TextMessage replyMsg = null;
			try {
				replyMsg = send(service);

				System.out.println(replyMsg);
			} catch (Throwable e) {
				assertTrue("Did not receive the expected exception message",
						e.getMessage().contains(service + ":onException=early EOF"));

				return;
			}

			System.out.println("reply headers: ");
			Enumeration<?> e = replyMsg.getPropertyNames();
			List<String> headers = new ArrayList<String>();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				headers.add(key);
				String debugValue = replyMsg.getStringProperty(key);
				System.out.println(key + " : " + debugValue);
			}
			fail("Expected Exception");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("----Completed testInvalidContentLength() expected early EOF");
			System.clearProperty("AFT_DME2_SET_RESLEN");

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

		TextMessage testMsg = session.createTextMessage("not zero length, or one");
		testMsg.setText("not empty");
		testMsg.setJMSReplyTo(replyQueue);
		try {
			QueueReceiver requestQueueReceiver = session.createReceiver(serviceQueue);
			MessageListener messageListener = new InvalidContentLengthReplyingListener(session);

			// add Listener
			requestQueueReceiver.setMessageListener(messageListener);
			Thread.sleep(5000);

			QueueSender queueSender = session.createSender(sendQueue);
			queueSender.send(testMsg);
			// Thread.sleep(5000);

			QueueReceiver replyQueueReceiver = session.createReceiver(replyQueue);
			TextMessage replyMsg = (TextMessage) replyQueueReceiver.receive(60000);

			return replyMsg;
		} finally {
			((DME2EndpointRegistryGRM) DME2JMSManager.getDefaultInstance().getDME2Manager().getEndpointRegistry())
					.shutdown();
		}
	}

	private class InvalidContentLengthReplyingListener implements MessageListener {

		private Session session;

		public InvalidContentLengthReplyingListener(Session session) {
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
					replyMsg.setText("");
					// copy all of the request headers into the reply headers
					en = m.getPropertyNames();
					while (en.hasMoreElements()) {
						String key = (String) en.nextElement();
						String debugValue = m.getStringProperty(key);
						replyMsg.setStringProperty(key, debugValue);
					}

					replyMsg.setJMSDestination(d);
					replyMsg.setJMSCorrelationID(m.getJMSMessageID());
					// lets set the content length to 1, even though the message
					// is empty.
					replyMsg.setIntProperty("Content-Length", 1);
					sender.send(replyMsg);
				}

			} catch (JMSException e) {
				e.printStackTrace();
			}
		}

	}

}
