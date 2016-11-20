/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static com.att.aft.dme2.test.jms.util.TestConstants.jndiClass;
import static com.att.aft.dme2.test.jms.util.TestConstants.jndiUrl;

import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;

public class JMSInitialContextTest extends JMSBaseTestCase {
  @Test
  public void testInitialContext() throws NamingException {
    Hashtable table = new Hashtable();
    table.put("java.naming.factory.initial", jndiClass);
    table.put("java.naming.provider.url", jndiUrl);

    System.out.println("Getting InitialContext");
    InitialContext context = new InitialContext(table);
  }
}
