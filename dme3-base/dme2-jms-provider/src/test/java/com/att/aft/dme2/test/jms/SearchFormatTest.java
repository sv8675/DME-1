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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class SearchFormatTest extends JMSBaseTestCase
{
	private ServerLauncher launcher = null;

    @Before
    public void setUp()
	throws Exception
	{
		super.setUp();
		launcher = new ServerLauncher(null, "-city","BHAM");
		launcher.launchTestJMSServer();
		Thread.sleep(2000);
	}
	
    @After
    public void tearDown() throws Exception
	{
		if(launcher != null)
		{
			launcher.destroy();
			launcher = null;
		}
		super.tearDown();
	}

    @Test
    public void testURISearchFormat() throws Exception
	{
		Properties props = RegistryFsSetup.init();
		Hashtable<String,Object> table = new Hashtable<String,Object>();
        for (Object key: props.keySet()) {
        	table.put((String)key, props.get(key));
        }
	    table.put("java.naming.factory.initial", TestConstants.jndiClass);
	    table.put("java.naming.provider.url", TestConstants.jndiUrl);
	    InitialContext context = new InitialContext(table);
	    QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
	    QueueConnection connection = factory.createQueueConnection();
	    QueueSession session = connection.createQueueSession(true, 0);
	    Queue remoteQueue = (Queue)context.lookup(TestConstants.dme2SearchStr);

		QueueSender sender = session.createSender(remoteQueue);
		Queue replyToQueue = session.createTemporaryQueue();
		QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
		
		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner",TestConstants.partner);
		msg.setJMSReplyTo(replyToQueue);
		
		sender.send(msg);
		try{ Thread.sleep(1000); }catch(Exception ex){}
		TextMessage rcvMsg = (TextMessage)replyReceiver.receive(123000);
		System.out.println("response message is: "+rcvMsg.getText());
		assertEquals("LocalQueueMessageListener:::TEST",rcvMsg.getText());
	}
	
    @Ignore
    @Test
    public void testURIResolveFormat() throws Exception
	{
		Properties props = RegistryFsSetup.init();
		Hashtable<String,Object> table = new Hashtable<String,Object>();
        for (Object key: props.keySet()) {
        	table.put((String)key, props.get(key));
        }
        table.put("java.naming.factory.initial", TestConstants.jndiClass);
	    table.put("java.naming.provider.url", TestConstants.jndiUrl);
	    InitialContext context = new InitialContext(table);
	    QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
	    QueueConnection connection = factory.createQueueConnection();
	    QueueSession session = connection.createQueueSession(true, 0);
	    Queue remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

	    // start service

		QueueSender sender = session.createSender(remoteQueue);
		Queue replyToQueue = session.createTemporaryQueue();
		QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
		
		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner",TestConstants.partner);
		msg.setJMSReplyTo(replyToQueue);
		
		sender.send(msg);
		try{ Thread.sleep(10000); }catch(Exception ex){}
		TextMessage rcvMsg = (TextMessage)replyReceiver.receive(3000);
		assertEquals("LocalQueueMessageListener:::TEST",rcvMsg.getText());
	}
	
}
