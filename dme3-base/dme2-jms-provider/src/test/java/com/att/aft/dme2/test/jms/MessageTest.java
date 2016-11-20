/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder.RequestDispatcherType;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.test.jms.servlet.EchoResponseServlet;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.LocalQueueMessageListener;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class MessageTest extends JMSBaseTestCase {

	@Test
	public void testTextMessage() throws Exception {
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
		TemporaryQueue tempQ = session.createTemporaryQueue();
		QueueSender sender = session.createSender(tempQ);
		QueueReceiver receiver = session.createReceiver(tempQ);
		TextMessage message = session.createTextMessage();
		message.setText("TEST");
		message.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		message.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		sender.send(message);
		// TextMessage rcvMsg = (TextMessage)receiver.receiveNoWait();
		TextMessage rcvMsg = (TextMessage) receiver.receive(3000);
		assertEquals("TEST", rcvMsg.getText());

		assertNull(receiver.receiveNoWait());
	}

	@Test
	public void testQDepth() throws Exception {
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
		TemporaryQueue tempQ = session.createTemporaryQueue();
		QueueSender sender = session.createSender(tempQ);
		QueueReceiver receiver = session.createReceiver(tempQ);
		TextMessage message = session.createTextMessage();
		message.setText("TEST");
		message.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		message.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		int counter = 0;
		for (int i = 0; i < 20; i++) {
			try {
				sender.send(message);
				counter++;
			} catch (Exception e) {
				System.out.println(" No of messages in q " + counter);
				e.printStackTrace();
				assertTrue(counter == 10);
				assertTrue(e.getMessage().contains("AFT-DME2-5409"));
				return;
			}
		}
		fail("if test reaches here, then q depth limit of 10 did not work");
	}

	@Test
	public void testMessageExpiry() throws Exception {

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
		TemporaryQueue tempQ = session.createTemporaryQueue();
		QueueSender sender = session.createSender(tempQ);
		QueueReceiver receiver = session.createReceiver(tempQ);
		TextMessage message = session.createTextMessage();
		// message.setJMSExpiration(10000);
		message.setText("TEST");
		message.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		message.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		sender.setTimeToLive(1000);
		sender.send(message);
		Thread.sleep(100);
		// TextMessage rcvMsg = (TextMessage)receiver.receiveNoWait();
		TextMessage rcvMsg = (TextMessage) receiver.receive(3000);
		assertEquals("TEST", rcvMsg.getText());

		message = session.createTextMessage();
		// message.setJMSExpiration(10000);
		message.setText("TEST");
		message.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		message.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		sender.setTimeToLive(1000);
		sender.send(message);
		Thread.sleep(2000);
		// TextMessage rcvMsg = (TextMessage)receiver.receiveNoWait();
		rcvMsg = (TextMessage) receiver.receive(3000);
		assertNull(rcvMsg);
	}

	@Test
	public void testTextMessageWithGetQueue() throws Exception {
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

		String requestQStr1 = "http://DME2LOCAL/service=com.att.aft.MyQueueService1/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String sendQStr1 = "http://DME2RESOLVE/service=com.att.aft.MyQueueService1/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

		String requestQStr2 = "http://DME2LOCAL/service=com.att.aft.MyQueueService2/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String sendQStr2 = "http://DME2RESOLVE/service=com.att.aft.MyQueueService2/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

		// Use createQueue to create local queue.
		Queue requestQueue1 = session.createQueue(requestQStr1);
		Queue sendQueue1 = session.createQueue(sendQStr1);
		Queue replyQueue1 = session.createTemporaryQueue();

		// Context.lookup will do DME2JMSManager.getQueue
		Queue requestQueue2 = (Queue) context.lookup(requestQStr2);
		Queue sendQueue2 = (Queue) context.lookup(sendQStr2);
		Queue replyQueue2 = session.createTemporaryQueue();

		TextMessage msg1 = session.createTextMessage("CBS1");
		msg1.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);		
		// msg1.setStringProperty("ORG", "CBS2");
		msg1.setJMSReplyTo(replyQueue1);

		TextMessage msg2 = session.createTextMessage("CBS2");
		// msg2.setStringProperty("ORG", "CBS2");
		msg2.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);					
		msg2.setJMSReplyTo(replyQueue2);

		QueueReceiver reqReceiver1 = session.createReceiver(requestQueue1);
		LocalQueueMessageListener listener1 = new LocalQueueMessageListener(qConn, session, requestQueue1);
		// add Listener
		reqReceiver1.setMessageListener(listener1);

		QueueSender sender1 = session.createSender(sendQueue1);
		sender1.send(msg1);
		Thread.sleep(1000);

		QueueReceiver repReceiver1 = session.createReceiver(replyQueue1);
		TextMessage rmsg1 = (TextMessage) repReceiver1.receive(60000);
		System.out.println(rmsg1.getText());
		assertTrue(rmsg1.getText().equals("LocalQueueMessageListener:::CBS1"));
		// assertEquals(rmsg1.getText(),);

		QueueSender sender2 = session.createSender(sendQueue2);
		QueueReceiver reqReceiver2 = session.createReceiver(requestQueue2);
		QueueReceiver repReceiver2 = session.createReceiver(replyQueue2);

		LocalQueueMessageListener listener2 = new LocalQueueMessageListener(qConn, session, requestQueue2);
		// add Listener
		reqReceiver2.setMessageListener(listener2);
		sender2.send(msg2);

		Thread.sleep(1000);

		TextMessage rmsg2 = (TextMessage) repReceiver2.receive(60000);
		System.out.println(rmsg2.getText());
		assertTrue(rmsg2.getText().equals("LocalQueueMessageListener:::CBS2"));
	}

	@Test
	public void testJMSGZIPCompression() throws Exception {
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		
		List<String> defaultConfigs = new ArrayList<String>();
		defaultConfigs.add(JMSConstants.JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME);
		defaultConfigs.add(JMSConstants.DME_API_DEFAULT_CONFIG_FILE_NAME);
//		defaultConfigs.add(JMSConstants.METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME);
		
		DME2Configuration config = new DME2Configuration("TestJMSGZIPCompress", defaultConfigs, null, props);
		DME2Manager mgr = new DME2Manager("TestJMSGZIPCompress", config);
		try {
			// Create service holder for each service registration
			DME2ServiceHolder svcHolder = new DME2ServiceHolder();
			svcHolder.setServiceURI(
					"service=com.att.aft.TestJMSGZIPCompression/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
			svcHolder.setManager(mgr);
			svcHolder.setServlet(new EchoResponseServlet(
					"service=com.att.aft.TestJMSGZIPCompression/version=1.0.0/envContext=LAB/routeOffer=DEFAULT/",
					"1"));

			// If context is set, DME2 will use this for publishing as context
			// with
			// endpoint registration, else serviceURI above will be used
			// svcHolder.setContext("/TestJMSGZIPCompression");
			// Below is to disable the default metrics filter thats added to
			// capture DME2 Metrics event of http traffic. By default
			// MetricsFilter
			// is enabled
			svcHolder.disableMetricsFilter();

			// Adding a Log filter to print incoming msg.
			org.eclipse.jetty.servlets.GzipFilter filter = new org.eclipse.jetty.servlets.GzipFilter();
			ArrayList<RequestDispatcherType> dlist = new ArrayList<RequestDispatcherType>();
			dlist.add(DME2FilterHolder.RequestDispatcherType.REQUEST);
			dlist.add(DME2FilterHolder.RequestDispatcherType.FORWARD);
			dlist.add(DME2FilterHolder.RequestDispatcherType.ASYNC);

			DME2FilterHolder filterHolder = new DME2FilterHolder(filter, "/*", EnumSet.copyOf(dlist));
			List<DME2FilterHolder> flist = new ArrayList<DME2FilterHolder>();
			flist.add(filterHolder);

			svcHolder.setFilters(flist);
			mgr.getServer().start();
			mgr.bindService(svcHolder);

			Thread.sleep(400);

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection qConn = qcf.createQueueConnection();
			QueueSession session = qConn.createQueueSession(true, 0);

			String sendQStr1 = "http://DME2RESOLVE/service=com.att.aft.TestJMSGZIPCompression/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";

			// Use createQueue to create local queue.
			Queue sendQueue1 = session.createQueue(sendQStr1);
			Queue replyQueue1 = session.createTemporaryQueue();

			TextMessage msg1 = session.createTextMessage("Sending EchoTest for JMS Compression");
			// msg1.setStringProperty("ORG", "CBS2");
			msg1.setStringProperty("Accept-Encoding", "gzip");
			msg1.setJMSReplyTo(replyQueue1);

			QueueSender sender1 = session.createSender(sendQueue1);
			sender1.send(msg1);
			Thread.sleep(1000);

			QueueReceiver repReceiver1 = session.createReceiver(replyQueue1);
			TextMessage rmsg1 = (TextMessage) repReceiver1.receive(60000);
			System.out.println(rmsg1.getText());
			assertTrue(rmsg1.getText().contains("Sending EchoTest for JMS Compression"));

		} finally {
			mgr.getServer().stop();
		}
	}

	@Test
	public void testHealthCheckNoBusinessLogic() throws Exception {
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

		String requestQStr1 = "http://DME2LOCAL/service=com.att.aft.HealthCheckService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
		String sendQStr1 = "http://DME2RESOLVE/service=com.att.aft.HealthCheckService/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

		// Use createQueue to create local queue.
		Queue requestQueue1 = session.createQueue(requestQStr1);
		Queue sendQueue1 = session.createQueue(sendQStr1);
		Queue replyQueue1 = session.createTemporaryQueue();

		TextMessage msg1 = session.createTextMessage("CBS1");
		// msg1.setStringProperty("DME2HealthCheck", "DME2HealthCheck");
		msg1.setJMSReplyTo(replyQueue1);
		msg1.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);	

		QueueReceiver reqReceiver1 = session.createReceiver(requestQueue1);
		LocalQueueMessageListener listener1 = new LocalQueueMessageListener(qConn, session, requestQueue1);
		// add Listener
		reqReceiver1.setMessageListener(listener1);

		QueueSender sender1 = session.createSender(sendQueue1);
		sender1.send(msg1);
		Thread.sleep(1000);

		DME2Manager manager = new DME2Manager();
		DME2Client sender = null;
		// DME2Client sender = new DME2Client(manager, new URI(sendQStr1),
		// 30000);
		// HashMap<String,String> hMap = new HashMap<String,String>();
		// hMap.put("DME2HealthCheck", "DME2HealthCheck");
		// sender.setHeaders(hMap);
		// sender.setPayload("this is a test");
		// String reply = sender.sendAndWait(30000);
		// System.out.println("Reply " + reply);
		// assertTrue((reply.length()==0));

		QueueReceiver repReceiver1 = session.createReceiver(replyQueue1);
		TextMessage rmsg2 = (TextMessage) repReceiver1.receive(60000);
		System.out.println(rmsg2.getText());
		assertTrue(rmsg2.getText().equals("LocalQueueMessageListener:::CBS1"));
		// assertEquals(rmsg1.getText(),);

	}

	@Test
	public void testNullMessage() throws Exception {
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
		TemporaryQueue tempQ = session.createTemporaryQueue();
		QueueSender sender = session.createSender(tempQ);
		QueueReceiver receiver = session.createReceiver(tempQ);
		TextMessage message = session.createTextMessage();
		message.setText(null);
		message.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
		message.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");
		sender.send(message);
		TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
		assertNull(rcvMsg.getText());
	}

	@Test
	public void testMessageSelectorNonCorrellationId() throws Exception {
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

		TextMessage msg1 = session.createTextMessage("CBS");
		msg1.setStringProperty("ORG", "CBS");
		TextMessage msg2 = session.createTextMessage("CSI");
		msg2.setStringProperty("ORG", "CSI");

		String messageSelector = "ORG=CBS";
		QueueSender sender = session.createSender(requestQueue);
		QueueReceiver receiver = session.createReceiver(requestQueue, messageSelector);
		sender.send(msg1);
		sender.send(msg2);
		TextMessage msg = (TextMessage) receiver.receiveNoWait();
		// DME2-FR-37: DME2 shall support message selectors based on JMS
		// CorrelationID
		// Other message selectors will *not* be supported
		assertNull(msg);
		// assertEquals("CBS",msg.getText());
		// msg = (TextMessage) receiver.receiveNoWait();
		// assertNull(msg.getText());
	}

}
