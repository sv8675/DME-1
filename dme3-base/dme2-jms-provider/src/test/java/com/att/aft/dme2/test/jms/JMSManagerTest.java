/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.util.HashMap;

import javax.jms.DeliveryMode;

import org.junit.Test;

import com.att.aft.dme2.jms.DME2JMSException;
import com.att.aft.dme2.jms.DME2JMSLocalQueue;
import com.att.aft.dme2.jms.DME2JMSManager;
import com.att.aft.dme2.jms.DME2JMSQueue;
import com.att.aft.dme2.jms.DME2JMSRemoteQueue;
import com.att.aft.dme2.jms.DME2JMSTextMessage;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;

public class JMSManagerTest extends JMSBaseTestCase {
	@Test
	public void testLocalQueue() throws Exception {
		DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
		DME2JMSLocalQueue queue = (DME2JMSLocalQueue) manager.getQueue("http://DME2LOCAL/SessionLocalQueue");
		assertEquals("/SessionLocalQueue", queue.getQueueName());
	}

	@Test
	public void testRemoteQueue() throws Exception {
		DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
		DME2JMSRemoteQueue queue = (DME2JMSRemoteQueue) manager.getQueue("http://REMOTE/SessionRemoteQueue");
		assertEquals("/SessionRemoteQueue", queue.getQueueName());
	}

	@Test
	public void testNullQueueName() throws Exception {
		DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
		try {
			manager.getQueue(null);
			fail("Should have failed. Queue name null.");
		} catch (DME2JMSException e) {
			assertTrue(e.getCause() instanceof NullPointerException);
		}
	}

	@Test
	public void testEmptyQueueName() throws Exception {
		DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
		DME2JMSQueue queue = manager.getQueue("");
		assertEquals("", queue.getQueueName());
	}

	@Test
	public void testTextMessage() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("com.att.aft.dme2.jms.dataContext", "205977");
		map.put("com.att.aft.dme2.jms.partner", "APPLE");

		DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
		DME2JMSTextMessage msg = (DME2JMSTextMessage) manager.createMessage(map, 0, "Test Manager createMessage()");
		assertEquals("APPLE", msg.getStringProperty("com.att.aft.dme2.jms.partner"));
	}

	@Test
	public void testTextMessageFromStream() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("JMSMessageID", "TEST_JMSMessageID");
		map.put("JMSCorrelationID", "TEST_JMSCorrelationID");
		map.put("JMSType", "TEST_JMSType");
		map.put("JMSDeliveryMode", String.valueOf(DeliveryMode.NON_PERSISTENT));
		map.put("JMSTimestamp", String.valueOf(System.currentTimeMillis()));
		String msgContents = "TEST MESSAGE.";
		ByteArrayInputStream bis = new ByteArrayInputStream(msgContents.getBytes());
		DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
		DME2JMSTextMessage msg = (DME2JMSTextMessage) manager.createMessage(bis, map);
		assertEquals("TEST MESSAGE.", msg.getText());
		assertEquals(DeliveryMode.NON_PERSISTENT, msg.getJMSDeliveryMode());
		assertEquals("TEST_JMSMessageID", msg.getJMSMessageID());
	}

	@Test
	public void testStreamMessage() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("JMSMessageID", "TEST_JMSMessageID");
		map.put("JMSCorrelationID", "TEST_JMSCorrelationID");
		map.put("JMSType", "TEST_JMSType");
		map.put("JMSDeliveryMode", String.valueOf(DeliveryMode.NON_PERSISTENT));
		map.put("JMSTimestamp", String.valueOf(System.currentTimeMillis()));

		String msgContents = "TEST MESSAGE.";
		ByteArrayInputStream bis = new ByteArrayInputStream(msgContents.getBytes());
		DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
		DME2JMSTextMessage msg = (DME2JMSTextMessage) manager.createMessage(bis, map);
		BufferedOutputStream bos = new BufferedOutputStream(System.out);
		manager.streamMessage(msg, bos);
		// works only with the flush...
		bos.flush();
		bos.close();
	}

	 @Test
	 public void testErrorMessage() throws Exception {
	 HashMap<String, String> map = new HashMap<String, String>();
	 map.put("JMSMessageID", "TEST_JMSMessageID");
	 map.put("JMSCorrelationID", "TEST_JMSCorrelationID");
	 map.put("JMSType", "TEST_JMSType");
	 map.put("JMSDeliveryMode", String.valueOf(DeliveryMode.NON_PERSISTENT));
	 map.put("JMSTimestamp", String.valueOf(System.currentTimeMillis()));
	
	 try {
	 int i = 10 / 0;
	 fail("Should have failed. division by zero.");
	 } catch (Exception ex) {
	 DME2JMSManager manager = DME2JMSManager.getDefaultInstance();
	 DME2JMSTextMessage msg = (DME2JMSTextMessage)
	 manager.createErrorMessage(ex, map);
	 assertEquals("TEST_JMSCorrelationID", msg.getJMSCorrelationID());
	 }
	 }
}
