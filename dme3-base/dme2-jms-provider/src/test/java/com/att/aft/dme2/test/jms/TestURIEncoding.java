/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertTrue;
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.jms.DME2JMSInitialContext;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;
import com.att.aft.dme2.util.DME2Utils;

public class TestURIEncoding extends JMSBaseTestCase {

	private ServerLauncher launcher = null;

	@Before
	public void setUp() throws Exception {

		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");

		// System.setProperty("AFT_DME2_CLIENT_PROXY_HOST", "127.0.0.1");
		// System.setProperty("AFT_DME2_CLIENT_PROXY_PORT", "9999");
		RegistryFsSetup.init();
	}
	
	@Ignore
	@Test
	public void testJMSClientRequest_WithEncodedURI() throws Exception {
		String serverStr = "http://DME2LOCAL/service=com.att.aft.dme2.test.TestJMSClientRequestWithURIEncoding/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
		String clientStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestJMSClientRequestWithURIEncoding/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
		String queryStr = "?key=word1 word2";

		String encodedClientStr = DME2Utils.encodeURIString((clientStr + queryStr), false);

		try {
			Properties props = RegistryFsSetup.init();

			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}

			launcher = new ServerLauncher(null, "-city", "BHAM", "-service", serverStr);
			launcher.launchTestGRMJMSServer();
			Thread.sleep(2000);

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "testJMSClientRequestWithURIEncoding");
			
			DME2JMSInitialContext context = new DME2JMSInitialContext(table);

			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);

			Queue sendQueue = session.createQueue(encodedClientStr);
			Queue replyQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setJMSReplyTo(replyQueue);
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);			
			msg.setStringProperty("com.att.aft.dme2.jms.test.echoRequestText", "true");
			msg.setText(
					"Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.");

			QueueSender sender = session.createSender(sendQueue);
			sender.send(msg);

			Thread.sleep(1000);

			QueueReceiver repReceiver = session.createReceiver(replyQueue);
			TextMessage resp = (TextMessage) repReceiver.receive(1380000);
			System.err.println(resp.getText());
			assertTrue(resp.getText().contains("Four score and seven years ago"));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				launcher.destroy();
			} catch (Exception e) {
			}
		}
	}
	
	@Test
	public void testJMSClientRequest_WithNonEncodedURI() throws Exception {
		/*
		 * This should fail since client are expected to encode the client URI
		 * beforehand if the URI String contains special chars that need ecoding
		 */
		String serverStr = "http://DME2LOCAL/service=com.att.aft.dme2.test.TestJMSClientRequestWithURIEncoding/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
		String clientStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestJMSClientRequestWithURIEncoding/version=1.0.0/envContext=LAB/routeOffer=PRIMARY";
		String queryStr = "?key=word1 word2";

		try {
			Properties props = RegistryFsSetup.init();

			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			launcher = new ServerLauncher(null, "-city", "BHAM", "-service", serverStr);
			launcher.launchTestGRMJMSServer();
			Thread.sleep(2000);

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "testJMSClientRequestWithURIEncoding");
			
			DME2JMSInitialContext context = new DME2JMSInitialContext(table);

			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);

			Queue sendQueue = session.createQueue(clientStr + queryStr);
			Queue replyQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setJMSReplyTo(replyQueue);
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.test.echoRequestText", "true");
			msg.setText(
					"Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.");

			QueueSender sender = session.createSender(sendQueue);
			sender.send(msg);

			Thread.sleep(1000);

			QueueReceiver repReceiver = session.createReceiver(replyQueue);
			TextMessage resp = (TextMessage) repReceiver.receive(30000);
			System.err.println(resp.getText());
			fail("This test case should have thrown a URISyntaxException due to invalid character in the URI. URI string = "
					+ clientStr + queryStr);

		} catch (Exception e) {
			assertTrue(e.getCause().getCause().getMessage().contains("Illegal character in query"));
			// assertTrue(e.getMessage().contains("Illegal character in
			// query"));
		} finally {
			try {
				launcher.destroy();
			} catch (Exception e) {
			}
		}
	}
}
