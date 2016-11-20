/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class ConnectionTest extends JMSBaseTestCase
{
    @Before
    public void setUp() throws Exception
	{
		super.setUp();
		System.setProperty("log4j.configuration", "file:src/main/config/log4j-console.properties");
		Locations.BHAM.set();
	}

    @Test
    public void testJMSConnection() throws Exception
	{
		Properties props = RegistryFsSetup.init();
		Hashtable<String,Object> table = new Hashtable<String,Object>();
        for (Object key: props.keySet()) {
        	table.put((String)key, props.get(key));
        }
        table.put("java.naming.factory.initial", TestConstants.jndiClass);
        table.put("java.naming.provider.url", TestConstants.jndiUrl);
        InitialContext context = new InitialContext(table);
        QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
        QueueConnection qConn = qcf.createQueueConnection();
        QueueSession session = qConn.createQueueSession(true, 0);
        Queue queue = (Queue) context.lookup("http://DME2LOCAL/ConnectionTestQueue");
        QueueSender sender = session.createSender(queue);
        QueueReceiver receiver = session.createReceiver(queue);
        TextMessage msg = session.createTextMessage();
        String str = "Connection Test";
        msg.setText(str);
        
        sender.send(msg);
        
        TextMessage rcvMsg = (TextMessage)receiver.receiveNoWait();
        assertNotNull(rcvMsg);
        assertEquals(str, rcvMsg.getText());
	}
	
    @Test
    public void testClosedConnection() throws Exception
	{
		Properties props = RegistryFsSetup.init();
		Hashtable<String,Object> table = new Hashtable<String,Object>();
        for (Object key: props.keySet()) {
        	table.put((String)key, props.get(key));
        }
        table.put("java.naming.factory.initial", TestConstants.jndiClass);
        table.put("java.naming.provider.url", TestConstants.jndiUrl);
        InitialContext context = new InitialContext(table);
        QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
        QueueConnection qConn = qcf.createQueueConnection();
        
        // close the connection
        qConn.close();
        
        try
        {
	        QueueSession session = qConn.createQueueSession(true, 0);
	        Queue queue = (Queue) context.lookup("http://DME2LOCAL/ConnectionTestQueue");
	        QueueSender sender = session.createSender(queue);
	        QueueReceiver receiver = session.createReceiver(queue);
	        TextMessage msg = session.createTextMessage();
	        String str = "Connection Test";
	        msg.setText(str);
	        
	        sender.send(msg);
	        
	        TextMessage rcvMsg = (TextMessage)receiver.receiveNoWait();
	        assertEquals(str, rcvMsg.getText());
	        fail("Should have failed. closed connection.");
        }
        catch (JMSException e)
        {
        	
        }
	}
	
    @Test
    public void testClientID() throws Exception
	{
		Properties props = RegistryFsSetup.init();
		Hashtable<String,Object> table = new Hashtable<String,Object>();
        for (Object key: props.keySet()) {
        	table.put((String)key, props.get(key));
        }
        table.put("java.naming.factory.initial", TestConstants.jndiClass);
        table.put("java.naming.provider.url", TestConstants.jndiUrl);
        InitialContext context = new InitialContext(table);
        QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
        QueueConnection qConn = qcf.createQueueConnection();
        String clientID = "TEST";
        qConn.setClientID(clientID);
        
        assertEquals(clientID, qConn.getClientID());
	}

}
