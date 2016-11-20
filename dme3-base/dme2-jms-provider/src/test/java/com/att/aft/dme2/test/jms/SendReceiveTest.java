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

import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class SendReceiveTest extends JMSBaseTestCase {
	private QueueConnection qConn;
	private String dest = "http://DME2LOCAL/SessionTestQueue";

	@Test
	public void testReceiverWakeup() throws Exception {
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		qConn = qcf.createQueueConnection();
		final Queue queue = (Queue) context.lookup(dest);

		final QueueSession session = qConn.createQueueSession(true, 0);
		final String msg = "TEST MSG";

		Thread t = new Thread() {
			public void run() {
				try {
					Thread.sleep(10000);
					QueueSender sender = session.createSender(queue);
					TextMessage txtMsg = session.createTextMessage(msg);
					sender.send(txtMsg);
					// System.out.println("sent");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.start();

		QueueReceiver receiver = session.createReceiver(queue);
		TextMessage rcvMsg = (TextMessage) receiver.receive(60000);
		assertEquals(msg, rcvMsg.getText());
	}

	@Test
	public void testSendReceiveFromDifferentConnections() throws Exception {

	}
}
