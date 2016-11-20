/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.InitialContext;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.test.jms.util.FastFailoverResultCounter;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;
import com.att.aft.dme2.test.jms.util.TestJMSRequestThread;
import com.att.aft.dme2.util.DME2Constants;

@Ignore
public class TestFastFailover_testConnectTimeoutFastFail  extends JMSBaseTestCase {
  private ServerLauncher launcher = null;
  private ServerLauncher launcher1 = null;
  Properties grmProps = null;
  String[] includeLibs = new String[] { "slf4j-api", "dme2", "log4j", "scld", "commons-configuration", "commons-lang", "commons-io",
      "commons-logging", "google", "commons-collection", "metrics", "jms", "jetty", "hazelcast", "jackson",
      "javax.servlet", "discovery", "backport" };
  InitialContext context;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    grmProps = RegistryFsSetup.init();
    try {
      cleanPreviousEndpoints( "com.att.test.FastFailService", "1.1.0", "DEV" );
    } catch ( Exception e ) {

    }
    Locations.BHAM.set();
    Properties props = RegistryFsSetup.init();
    props.setProperty("AFT_DME2_CLIENT_QDEPTH", "30");
    props.setProperty( DME2Constants.AFT_DME2_CLIENT_TP_MAX_QUEUED, "20" );
	/*	props.setProperty( DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS, "14000" );
		System.setProperty( DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS, "14000" );*/
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    for (Object key : props.keySet()) {
      table.put((String) key, props.get(key));
    }
    table.put("java.naming.factory.initial", TestConstants.jndiClass);
    table.put("java.naming.provider.url", TestConstants.jndiUrl);
    context = new InitialContext(table);
  }
  @Test
  public void testConnectTimeoutFastFail() throws Exception {
    Locations.BHAM.set();
    FastFailoverResultCounter.reset();
    Properties props = RegistryFsSetup.init();
//		props.setProperty("AFT_DME2_CORE_POOL_SIZE", "1");
//		props.setProperty("AFT_DME2_MAX_POOL_SIZE", "1");
//		props.setProperty("AFT_DME2_SOCKET_ACCEPTOR_THREADS", "1");
    Hashtable<String, Object> table = new Hashtable<String, Object>();
    for (Object key : props.keySet()) {
      table.put((String) key, props.get(key));
    }
    table.put("java.naming.factory.initial", TestConstants.jndiClass);
    table.put("java.naming.provider.url", TestConstants.jndiUrl);
    InitialContext context = new InitialContext(table);
		/*QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection();
		connection.createQueueSession(true, 0);
		context.lookup(TestConstants.dme2ConnTimeoutResolveStr);
		*/

    // start service
    launcher = new ServerLauncher(includeLibs, "-city", "BHAM", "-connectTimeout");
    launcher.launchFastFailTestJMSServer();
    Thread.sleep(30000);

    launcher1 = new ServerLauncher(includeLibs, "-city", "BHAM");
    launcher1.launchFastFailRespondTestJMSServer();
    Thread.sleep(30000);

    for (int i = 0; i < 1; i++) {
      TestJMSRequestThread ti = new TestJMSRequestThread(i + "", TestConstants.dme2FailSvcResolveStr, context);
      Thread t = new Thread(ti);
      t.setDaemon(true);
      t.start();
    }
    try {
      Thread.sleep(90000);
    } catch (Exception ex) {
    }
    System.out.println("FastFailover9595=" + FastFailoverResultCounter.fastFail9595 + ";FastFailover9596="
        + FastFailoverResultCounter.fastFail9596);

    try {
      launcher.destroy();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    try {
      launcher1.destroy();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    try {
      Thread.sleep(5000);
    } catch (Exception ex) {
    }
    System.out.println("Expected failover on 9595, but was " + FastFailoverResultCounter.fastFail9595);
    assertTrue("Expected failover on 9595, but was " + FastFailoverResultCounter.fastFail9595, FastFailoverResultCounter.fastFailover9595);

  }

}
