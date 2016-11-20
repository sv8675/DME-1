/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Message;
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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.att.aft.dme2.jms.DME2JMSException;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.TestConstants;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class QueueSessionTest extends JMSBaseTestCase {
	private QueueConnection qConn;
	private Queue queue;
	private String dest = "http://DME2LOCAL/SessionTestQueue";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		qConn = qcf.createQueueConnection();
		queue = (Queue) context.lookup(dest);
	}

	@Test
	public void testCreateQueueSession() throws Exception {
		QueueSession session = qConn.createQueueSession(true, 0);
		session.close();
	}

	@Test
	public void testCreateQueueReceiver() throws Exception {
		QueueSession session = qConn.createQueueSession(true, 0);
		QueueReceiver receiver = session.createReceiver(queue);
		Message msg = receiver.receiveNoWait();
		assertNull(msg);
	}

	@Test
	public void testCreateQueueSender() throws Exception {
		QueueSession session = qConn.createQueueSession(true, 0);
		QueueSender sender = session.createSender(queue);
		TextMessage msg = session.createTextMessage("TEST");
		sender.send(msg);
	}

	@Test
	public void testCreateQueueSender_NullQueue() throws Exception {
		QueueSession session = qConn.createQueueSession(true, 0);
		QueueSender sender = session.createSender(null);
		TextMessage msg = session.createTextMessage("TEST");
		try {
			sender.send(queue, msg);
		} catch (DME2JMSException e) {
			fail("attempt to send a message on a sender with no queue set resulted in an exception when send with a queue was called");
		}
	}

	@Test
	public void testCreateQueueSender_NullQueue_Exception() throws Exception {

		try {
			QueueSession session = qConn.createQueueSession(true, 0);
			QueueSender sender = session.createSender(null);
			TextMessage msg = session.createTextMessage("TEST");
			sender.send(msg);
		} catch (NullPointerException e) {
			return;
		}
		fail("attempt to send a message on a sender with no queue set did not throw an exception as expected");
	}

	@Test
	public void testClosedSessionAccess() throws Exception {
		QueueSession session = qConn.createQueueSession(true, 0);
		session.close();
		try {
			QueueSender sender = session.createSender(queue);
			TextMessage msg = session.createTextMessage("TEST");
			sender.send(msg);
			QueueReceiver receiver = session.createReceiver(queue);
			TextMessage rcvMsg = (TextMessage) receiver.receiveNoWait();
			assertEquals("TEST", rcvMsg.getText());
			fail("Should have failed. Reason=Closed QueueSession.");
		} catch (JMSException e) {
		 	e.printStackTrace();
			assertTrue(e.toString().toUpperCase().indexOf("CLOSED") > -1);

		}
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		qConn.close();
	}
}
