/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.Test;

import com.att.aft.dme2.jms.DME2JMSManager;
import com.att.aft.dme2.jms.DME2JMSQueueReceiver;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class TemporaryQueueTestDeleteTempQueue extends JMSBaseTestCase {

	public void setup() throws Exception {
		super.setUp();
		System.setProperty("-Dlog4j.configuration", "file:src/main/config/log4j-console.properties");
	}

	@Test
	public void testDeleteTempQueueWithCleanupDisabled() throws Exception {
		System.setProperty("DME2_JMS_TEMP_QUEUE_REC_CLEANUP", "false");

		TemporaryQueue destQ = null;
		TemporaryQueue replyQ = null;

		QueueConnectionFactory qcf = null;
		QueueConnection qConn = null;
		QueueSession session = null;

		QueueSender sender = null;

		try {
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			qConn = qcf.createQueueConnection();
			session = qConn.createQueueSession(true, 0);

			destQ = session.createTemporaryQueue();
			replyQ = session.createTemporaryQueue();

			TextMessage message = session.createTextMessage();
			message.setJMSReplyTo(replyQ);
			message.setText("TEST");
			message.setStringProperty("com.att.aft.dme2.jms.dataContext", "205977");
			message.setStringProperty("com.att.aft.dme2.jms.partner", "APPLE");

			sender = session.createSender(destQ);
			sender.send(message);

			final QueueReceiver receiver = session.createReceiver(replyQ);

			List<DME2JMSQueueReceiver> receivers = DME2JMSManager.getDefaultInstance().getQueueReceivers(replyQ);
			assertNull(receivers); // Since flag is false, this should be null.
									// Receivers shouldn't be added to map

			final AtomicBoolean failed = new AtomicBoolean(false);
			final AtomicBoolean ended = new AtomicBoolean(false);

			// Calling receive on new thread. Then on main thread, close the
			// replyQ while the receiver is still trying to receive the message
			Thread thr = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						receiver.receive(60000);
						ended.set(true);
					} catch (JMSException e) {
						e.printStackTrace();
						failed.set(true);
					}
				}
			});

			thr.setDaemon(true);
			thr.start();

			Thread.sleep(2000);

			if (((DME2JMSQueueReceiver) receiver).isReceiverWaiting()) {
				// Delete tempQueue. Shouldn't throw an exception
				replyQ.delete();
				assertFalse(failed.get());
			} else {
				fail();
			}

			long duration = System.currentTimeMillis() + 60000;
			while (!ended.get()) {
				if (System.currentTimeMillis() > duration && !ended.get()) {
					fail("Receiver failed to timeout.");
				}
				Thread.sleep(1000);
			}
		} finally {
			System.clearProperty("DME2_JMS_TEMP_QUEUE_REC_CLEANUP");
			System.clearProperty("DME2.tempqueue.idletimeoutms");

			JMSBaseTestCase.closeJMSResources(qConn, session, destQ, sender, null);
			JMSBaseTestCase.closeJMSResources(null, null, replyQ, null, null);

		}
	}

}
