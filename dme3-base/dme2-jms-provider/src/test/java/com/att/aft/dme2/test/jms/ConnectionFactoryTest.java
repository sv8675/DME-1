/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;

import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class ConnectionFactoryTest extends JMSBaseTestCase {

	@Test
	public void testQueueConnectionFactory() throws Exception {
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		Connection connection = qcf.createConnection();
		assertNotNull(connection);

		connection = qcf.createConnection("cbsuser", "cbspassword");
		assertNotNull(connection);
	}


	@Test
	public void testInvalidLookupStr() throws Exception {
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		// QueueConnectionFactory qcf = (QueueConnectionFactory)
		// context.lookup("qcf://dme2/http://www.google.com");
		Object o = context.lookup("http://www.google.com");

		assertFalse(o instanceof QueueConnectionFactory);
	}
	
	
	@Test
	public void testInvalidFactoryClass() throws Exception {
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", "com.att.aft.dme2.jms.InvalidInitialContextFactory");
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		try {
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection qConn = qcf.createQueueConnection();
			fail("Should have failed. invalid value for java.naming.factory.initial.");
		} catch (Exception e) {
		}
	}
	
	
	@Test
	public void testInvalidProviderUrl() throws Exception {
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", "http://dme2");
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		// fail("Should have failed. Invalid provider url.");

	}
}
