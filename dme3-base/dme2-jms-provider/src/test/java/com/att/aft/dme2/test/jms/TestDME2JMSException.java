/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

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

import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.jms.DME2JMSInitialContext;
import com.att.aft.dme2.jms.DME2JMSManager;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;
public class TestDME2JMSException extends JMSBaseTestCase {

	@Test
	public void testCatchUncheckedException_DME2ClientRequest() {
		// String clientStr =
		// "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestCatchUncheckedException/version=abc/envContext=LAB/routeOffer=PRIMARY";
		String clientStr = "http://DME2RESOLVE/service=com.att.aft.dme2.test.TestCatchUncheckedException/version=1.0/envContext=LAB"
				+ "/routeOffer=PRIMARY";

		try {
			Properties props = RegistryFsSetup.init();
			props.put("DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH", "true");
			props.put("DME2_PAYLOAD_COMPRESSION_THRESH_SIZE", "1000");

			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "testCatchUncheckedException_DME2ClientRequest");

			DME2JMSInitialContext context = new DME2JMSInitialContext(table);

			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);

			Queue sendQueue = session.createQueue(clientStr);
			Queue replyQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setJMSReplyTo(replyQueue);
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.test.echoRequestText", "true");
			msg.setText(
					"Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal. "
							+ "Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war."
							+ " We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this. "
							+ "But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. "
							+ "The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. "
							+ "It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- "
							+ "that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom"
							+ " -- and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln -- and that government of the people, by the people, for the people,"
							+ " shall not perish from the earth. Abraham Lincoln -- and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln -- and that government of the people, by the people,"
							+ " for the people, shall not perish from the earth. Abraham Lincoln -- and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln -- "
							+ "and that government of the people, by the people, for the people, shall not perish from the earth. Abraham Lincoln");

			msg.setText(null);
			QueueSender sender = session.createSender(sendQueue);
			if (sender != null) {
				sender.send(msg);
			}
			Thread.sleep(1000);

			QueueReceiver repReceiver = session.createReceiver(replyQueue);
			TextMessage resp = (TextMessage) repReceiver.receive(30000);
			System.err.println(resp.getText());

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("[AFT-DME2-0701]"));
			assertTrue(e.getCause() instanceof DME2Exception);
		} finally {
			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");

		}
	}

	
	@Test
	public void testCatchUncheckedException_DME2JMSManager() throws Exception {
		RegistryFsSetup.init();

		try {
			DME2JMSManager.getDefaultInstance().getQueue(null);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("[AFT-DME2-9000]"));
			assertTrue(e.getCause() instanceof NullPointerException);
		}

		try {
			DME2JMSManager.getDefaultInstance().getQueueFromCache(null);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("[AFT-DME2-9000]"));
			assertTrue(e.getCause() instanceof NullPointerException);
		}
	}

}