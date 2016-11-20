/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
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
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.att.aft.dme2.event.DefaultMetricsCollector;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJMSMetricsCollection extends JMSBaseTestCase{

	private ServerLauncher launcher = null;

    @Before
    public void setUp() throws Exception {
		super.setUp();
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("DME2.DEBUG", "true");
	}

    @Test
    public void testMetricsJMSServer() throws Exception {
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("AFT_DME2_DISABLE_METRICS", "false");		
		System.setProperty("DME2.DEBUG", "true");
		String containerName="com.att.test.MetricsTestCollectorClient"; 
		String containerVersion="1.0.0"; 
		String containerRO="TESTRO"; 
		String containerEnv="DEV"; 
		String containerPlat=TestConstants.GRM_PLATFORM_TO_USE; 
		String containerHost=null; 
		String containerPid="1234"; 
		String containerPartner="TEST";
		System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
		System.setProperty("platform", containerPlat);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
		//System.setProperty("platform","NON-PROD");
		try {
			containerHost = InetAddress.getLocalHost().getHostName();
		}catch(Exception e) {
			
		}
		System.setProperty("lrmHost",containerHost);
		System.setProperty("Pid",containerPid);
		System.setProperty("partner",containerPartner);
		DefaultMetricsCollector collector = DefaultMetricsCollector.getMetricsCollector(containerName, containerVersion, containerRO, containerEnv, 
				containerPlat, containerHost, containerPid, containerPartner);
	
//		collector.setDisablePublish(true);
		Locations.BHAM.set();
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
		// remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

	    // start service
		launcher = new ServerLauncher(null, "-city","BHAM");
		launcher.launchTestMetricsJMSServer();
		Thread.sleep(30000);

		QueueSender sender = session.createSender(remoteQueue);

		// Queue replyToQueue = session.createTemporaryQueue();

		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext",
				TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner",
				TestConstants.partner);
		Queue replyToQueue = (Queue) context
				.lookup("http://DME2LOCAL/clientResponseQueue");
		msg.setJMSReplyTo(replyToQueue);

		sender.send(msg);
		//QueueReceiver replyReceiver = session.createReceiver(replyToQueue,
		//		"JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
		QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
		try {
			Thread.sleep(5000);
		} catch (Exception ex) {
		}
		TextMessage rcvMsg = (TextMessage) replyReceiver.receive(3000);
		//TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
		assertEquals("TestMetricsMessageListener:::TEST", rcvMsg.getText());
		//String selector = replyReceiver.getMessageSelector();
		//fail(selector);
		// check metrics data
		try {
			launcher.destroy(); 
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		Thread.sleep(10000);
		long eventTime = System.currentTimeMillis();

		//System.out.println(collector.getCurrentTimeSlot());
/**		Timeslot slot = collector.getTimeslotForTime(eventTime);
		SvcProtocolMetrics metrics = slot.getServiceProtocolMetrics("MyService", "1.0.0", "SERVER", null, "JMS");
		Collection<SvcProtocolMetrics> metricsc = slot.getAllMetrics();
		Iterator<SvcProtocolMetrics> it = metricsc.iterator();
		while(it.hasNext()) {
			SvcProtocolMetrics m = it.next();
			com.att.aft.metrics.core.Service svc = m.getService();
			System.err.println("Metrics service "+svc);
			System.out.println(m.getService() + ":" + m.getTimeslot() + ":" + m.getMetricsTimeslot());
			assertTrue(svc.getName().equals("MyService") && svc.getRole().equals("CLIENT") && svc.hasProtocol("JMS"));
			return;
		}
		fail("If code gets here, metrics assertion did not work");
*/
	}
	
    @After
    public void tearDown() throws Exception{
		System.clearProperty("platform");
		System.clearProperty("DME2.DEBUG");
		System.clearProperty("lrmRName");
		System.clearProperty("lrmRVer");
		System.clearProperty("lrmRO");
		System.clearProperty("lrmEnv");
		System.clearProperty("AFT_DME2_PUBLISH_METRICS");
		System.clearProperty("DME2.DEBUG");
		super.tearDown();
	}


}
