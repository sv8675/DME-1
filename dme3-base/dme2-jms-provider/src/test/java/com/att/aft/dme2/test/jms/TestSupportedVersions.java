/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
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
import javax.naming.NamingException;

import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.samples.TestReceiveServer;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;

@Ignore
public class TestSupportedVersions extends JMSBaseTestCase {

	DME2Configuration config = new DME2Configuration();

	/**
	 * test case for supportedVersionsRange for DME2 JMS endpoints
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NamingException
	 * @throws JMSException
	 */

	@Test
	public void testJMSSupportedVersions() throws Exception {
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		ServerLauncher launcher1 = null;
		ServerLauncher launcher2 = null;
		ServerLauncher launcher3 = null;

//		//cleanPreviousEndpoints("com.att.aft.TestJMSSupportedVersionsRange", "28.2.0", "DEV");
//		//cleanPreviousEndpoints("com.att.aft.TestJMSSupportedVersionsRange", "30.0.0", "DEV");
//		//cleanPreviousEndpoints("com.att.aft.TestJMSSupportedVersionsRange", "32.0.0", "DEV");

		try {
			// Endpoint that has supportedVersionRange
			String server1Str = "http://DME2LOCAL/service=com.att.aft.TestJMSSupportedVersionsRange/version=28.2.0/envContext=DEV/routeOffer=BAU_NE?supportedVersionRange=18.0,28.2";
			String server2Str = "http://DME2LOCAL/service=com.att.aft.TestJMSSupportedVersionsRange/version=30.0.0/envContext=DEV/routeOffer=BAU_SE";
			String server3Str = "http://DME2LOCAL/service=com.att.aft.TestJMSSupportedVersionsRange/version=32.0.0/envContext=DEV/routeOffer=BAU_NW";

			// client version 19 should result in being processed by BAU_NE
			// routeOffer
			String clientStr = "http://DME2SEARCH/service=com.att.aft.TestJMSSupportedVersionsRange/version=19/envContext=DEV/partner=test1";
			// client version 30, should be processed by BAU_SE only.
			String clientStr1 = "http://DME2SEARCH/service=com.att.aft.TestJMSSupportedVersionsRange/version=30/envContext=DEV/partner=test1";
			// client version 17, does not fit any of supportedVersionRange and
			// should be failing with no endpoints.
			String clientStr2 = "http://DME2SEARCH/service=com.att.aft.TestJMSSupportedVersionsRange/version=17/envContext=DEV/partner=test1";

			launcher1 = new ServerLauncher(null, "-city", "BHAM", "-service", server1Str); // ,
																						// "-killfile",
																						// "killfile1");

			launcher1.launchTestGRMJMSServer();
			Thread.sleep(5000);

			launcher2 = new ServerLauncher(null, "-city", "BHAM", "-service", server2Str); // ,
																						// "-killfile",
																						// "killfile2");
			launcher2.launchTestGRMJMSServer();
			Thread.sleep(5000);

			launcher3 = new ServerLauncher(null, "-city", "BHAM", "-service", server3Str); // ,
																						// "-killfile",
																						// "killfile3");
			launcher3.launchTestGRMJMSServer();
			Thread.sleep(5000);

			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			// ensures the request level handlers are set in dme2Manager level.
			table.put("AFT_DME2_MANAGER_NAME", "testJMSSupportedVersions");

			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(clientStr);
			QueueSender sender = session.createSender(remoteQueue);
			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);
			try {
				Thread.sleep(5000);
			} catch (Exception ex) {
			}
			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);

			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			String traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// client version 19 should result in being processed by BAU_NE
			// routeOffer
			assertTrue(traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));

			remoteQueue = (Queue) context.lookup(clientStr1);
			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			rcvMsg = (TextMessage) replyReceiver.receive(10000);
			traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			System.out.println("traceInfo=" + traceInfo);
			// client version 30, should be processed by BAU_SE only.
			assertTrue(traceInfo.contains("/routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			remoteQueue = (Queue) context.lookup(clientStr2);
			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			try {
				sender.send(msg);
				replyReceiver = session.createReceiver(replyToQueue);
				try {
					Thread.sleep(1000);
				} catch (Exception ex) {
				}
				rcvMsg = (TextMessage) replyReceiver.receive(10000);
				assertTrue(rcvMsg == null);
			} catch (Exception e) {
				// client version 17, does not fit any of supportedVersionRange
				// and should be failing with no endpoints.
				assertTrue(e.getMessage().contains("AFT-DME2-0702"));
			}

		} finally {
			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");
			try {
				if (launcher1 != null)
					launcher1.destroy();
			} catch (Exception e) {
			}
			try {
				if (launcher2 != null)
					launcher2.destroy();
			} catch (Exception e) {
			}
			try {
				if (launcher3 != null)
					launcher3.destroy();
			} catch (Exception e) {
			}

			try {
				DME2Configuration config = new DME2Configuration();
				String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE",
						System.getProperty("user.home") + "/.aft/.dme2PortCache");
				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {

			}
		}
	}

	/**
	 * test case for supportedVersionsRange with DME2 JMS QueueReceiver type
	 * endpoints
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NamingException
	 * @throws JMSException
	 */
	@Test
	public void testJMSSupportedVersionsWithReceiver()
			throws Exception {
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		ServerLauncher launcher1 = null;
		ServerLauncher launcher2 = null;
		ServerLauncher launcher3 = null;
		TestReceiveServer server1 = null;
		TestReceiveServer server2 = null;
		TestReceiveServer server3 = null;
//		//cleanPreviousEndpoints( "com.att.aft.testJMSSupportedVersionsWithReceiver", "28.2.0", "DEV" );
//		//cleanPreviousEndpoints( "com.att.aft.testJMSSupportedVersionsWithReceiver", "30.0.0", "DEV" );
//		//cleanPreviousEndpoints( "com.att.aft.testJMSSupportedVersionsWithReceiver", "32.0.0", "DEV" );
		try {
			// Endpoint that has supportedVersionRange
			String server1Str = "http://DME2LOCAL/service=com.att.aft.testJMSSupportedVersionsWithReceiver/version=28.2.0/envContext=DEV/routeOffer=BAU_NE?supportedVersionRange=18.0,28.2&server=true";
			String server2Str = "http://DME2LOCAL/service=com.att.aft.testJMSSupportedVersionsWithReceiver/version=30.0.0/envContext=DEV/routeOffer=BAU_SE?server=true";
			String server3Str = "http://DME2LOCAL/service=com.att.aft.testJMSSupportedVersionsWithReceiver/version=32.0.0/envContext=DEV/routeOffer=BAU_NW?server=true";

			// client version 19 should result in being processed by BAU_NE
			// routeOffer
			String clientStr = "http://DME2SEARCH/service=com.att.aft.testJMSSupportedVersionsWithReceiver/version=19/envContext=DEV/partner=test1";
			// client version 30, should be processed by BAU_SE only.
			String clientStr1 = "http://DME2SEARCH/service=com.att.aft.testJMSSupportedVersionsWithReceiver/version=30/envContext=DEV/partner=test1";
			// client version 17, does not fit any of supportedVersionRange and
			// should be failing with no endpoints.
			String clientStr2 = "http://DME2SEARCH/service=com.att.aft.testJMSSupportedVersionsWithReceiver/version=17/envContext=DEV/partner=test1";

			Properties props = RegistryFsSetup.init();
			// -jndiClass com.att.aft.dme2.jms.DME2JMSInitialContextFactory
			// -jndiUrl qcf://dme2 -conn qcf://dme2 -dest
			// http://DME2SEARCH/service=com.att.aft.DME2CREchoService/version=1.1.0/envContext=UAT/partner=has
			// -replyTo http://DME2LOCAL/clientResponseQueue -threads 1
			// -pauseTime 1000 -maxc 20
			String jndiClass = "com.att.aft.dme2.jms.DME2JMSInitialContextFactory";
			String jndiUrl = "qcf://dme2";
			String serverConn = "qcf://dme2:";
			int serverThreads = 5;
			int receiveTimeout = 60000;

			try {
				server1 = new TestReceiveServer(jndiClass, jndiUrl, serverConn, server1Str, serverThreads,
						receiveTimeout);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			server1.start();

			try {
				server2 = new TestReceiveServer(jndiClass, jndiUrl, serverConn, server2Str, serverThreads,
						receiveTimeout);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			server2.start();

			try {
				server3 = new TestReceiveServer(jndiClass, jndiUrl, serverConn, server3Str, serverThreads,
						receiveTimeout);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			server3.start();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}

			try {
				Thread.sleep(5000);
			} catch (Exception ex) {
			}

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			// ensures the request level handlers are set in dme2Manager level.
			table.put("AFT_DME2_MANAGER_NAME", "testJMSSupportedVersionsWithReceiver");

			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(clientStr);
			QueueSender sender = session.createSender(remoteQueue);
			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			String traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertTrue(rcvMsg.getText().contains("TEST; Receiver: PID@HOST:"));
			// client version 19 should result in being processed by BAU_NE
			// routeOffer
			assertTrue(traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));

			remoteQueue = (Queue) context.lookup(clientStr1);
			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			rcvMsg = (TextMessage) replyReceiver.receive(10000);
			traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertTrue(rcvMsg.getText().contains("TEST; Receiver: PID@HOST:"));
			System.out.println("traceInfo=" + traceInfo);
			// client version 30, should be processed by BAU_SE only.
			assertTrue(traceInfo.contains("/routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			remoteQueue = (Queue) context.lookup(clientStr2);
			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			try {
				sender.send(msg);
				replyReceiver = session.createReceiver(replyToQueue);
				try {
					Thread.sleep(1000);
				} catch (Exception ex) {
				}
				rcvMsg = (TextMessage) replyReceiver.receive(10000);
				assertTrue(rcvMsg == null);
			} catch (Exception e) {
				// client version 17, does not fit any of supportedVersionRange
				// and should be failing with no endpoints.
				assertTrue(e.getMessage().contains("AFT-DME2-0702"));
			}

		} finally {
			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");
			try {
				if (server1 != null)
					server1.stop();
			} catch (Exception e) {
			}
			try {
				if (server2 != null)
					server2.stop();
			} catch (Exception e) {
			}
			try {
				if (server3 != null)
					server3.stop();
			} catch (Exception e) {
			}

			try {
				String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE",
						System.getProperty("user.home") + "/.aft/.dme2PortCache");
				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {

			}
		}
	}

	@Test
	public void testRangeVsExactVersion() throws Exception {
//		super.cleanPreviousEndpoints( "com.att.aft.testRangeVsExactVersion", "28.2.0", "DEV" );
//		super.cleanPreviousEndpoints( "com.att.aft.testRangeVsExactVersion", "30.0.0", "DEV" );
//		super.cleanPreviousEndpoints( "com.att.aft.testRangeVsExactVersion", "32.0.0", "DEV" );
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		ServerLauncher launcher1 = null;
		ServerLauncher launcher2 = null;
		ServerLauncher launcher3 = null;
		//cleanPreviousEndpoints("com.att.aft.testRangeVsExactVersion", "28.2.0", "DEV");
		//cleanPreviousEndpoints("com.att.aft.testRangeVsExactVersion", "30.0.0", "DEV");
		//cleanPreviousEndpoints("com.att.aft.testRangeVsExactVersion", "32.0.0", "DEV");
		try {
			// Endpoint that has supportedVersionRange
			String server1Str = "http://DME2LOCAL/service=com.att.aft.testRangeVsExactVersion/version=28.2.0/envContext=DEV/routeOffer=BAU_NE?supportedVersionRange=18.0,28.2";
			String server2Str = "http://DME2LOCAL/service=com.att.aft.testRangeVsExactVersion/version=30.0.0/envContext=DEV/routeOffer=BAU_SE"; // ?supportedVersionRange=18.0,38.2";
			String server3Str = "http://DME2LOCAL/service=com.att.aft.testRangeVsExactVersion/version=32.0.0/envContext=DEV/routeOffer=BAU_NW";

			launcher1 = new ServerLauncher(null, "-city", "BHAM", "-service", server1Str);// ,
																					// "-killfile",
																					// "killfile1");

			launcher1.launchTestGRMJMSServer();
			Thread.sleep(5000);

			launcher2 = new ServerLauncher(null, "-city", "BHAM", "-service", server2Str);// ,
																					// "-killfile",
																					// "killfile2");
			launcher2.launchTestGRMJMSServer();
			Thread.sleep(5000);

			launcher3 = new ServerLauncher(null, "-city", "BHAM", "-service", server3Str);// ,
																					// "-killfile",
																					// "killfile3");
			launcher3.launchTestGRMJMSServer();
			Thread.sleep(5000);

			// versions registered:
			// NE) 28.2.0 supporting 18.0,28.2
			// SE) 30.0.0
			// NW) 32.0.0

			// NE includes in supported range
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=19/envContext=DEV/partner=test1",
					"/routeOffer=BAU_NE:onResponseCompleteStatus=200");

			// SE matches major
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=30/envContext=DEV/partner=test1",
					"/routeOffer=BAU_SE:onResponseCompleteStatus=200");

			// SE matches major and minor
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=30.0/envContext=DEV/partner=test1",
					"/routeOffer=BAU_SE:onResponseCompleteStatus=200");

			// SE matches major but not minor
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=30.1/envContext=DEV/partner=test1",
					"AFT-DME2-0702");

			// matches none
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=17/envContext=DEV/partner=test1",
					"AFT-DME2-0702");

			// NE includes in supported range but not exact match
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=19/envContext=DEV/partner=test1?matchVersionRange=false",
					"AFT-DME2-0702");

			// SE still matches major
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=30/envContext=DEV/partner=test1?matchVersionRange=false",
					"/routeOffer=BAU_SE:onResponseCompleteStatus=200");

			// NE matches major
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=28/envContext=DEV/partner=test1?matchVersionRange=false",
					"/routeOffer=BAU_NE:onResponseCompleteStatus=200");

			// NE matches major and minor
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=28.2/envContext=DEV/partner=test1?matchVersionRange=false",
					"/routeOffer=BAU_NE:onResponseCompleteStatus=200");

			// NE matches major and minor
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=28.2.0/envContext=DEV/partner=test1?matchVersionRange=false",
					"/routeOffer=BAU_NE:onResponseCompleteStatus=200");

			// NE matches major and minor
			assertTraceContains(
					"http://DME2SEARCH/service=com.att.aft.testRangeVsExactVersion/version=28.2.1/envContext=DEV/partner=test1?matchVersionRange=false",
					"AFT-DME2-0702");

		} finally {
			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");
			try {
				if (launcher1 != null)
					launcher1.destroy();
			} catch (Exception e) {
			}
			try {
				if (launcher2 != null)
					launcher2.destroy();
			} catch (Exception e) {
			}
			try {
				if (launcher3 != null)
					launcher3.destroy();
			} catch (Exception e) {
			}

			try {
				String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE",
						System.getProperty("user.home") + "/.aft/.dme2PortCache");
				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {

			}
		}
	}

	@Test
	public void testMatchVersionRangeEnabled() throws Exception {
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");

		ServerLauncher launcher1 = null;
		QueueConnection connection = null;
		QueueSession session = null;
		Queue remoteQueue = null;
		QueueSender sender = null;
		Queue replyToQueue = null;

		//cleanPreviousEndpoints("com.att.aft.testMatchVersionRangeEnabled", "28.2.0", "DEV");
		try {
			String server1Str = "http://DME2LOCAL/service=com.att.aft.testMatchVersionRangeEnabled/version=28.2.0/envContext=DEV/routeOffer=BAU_NE?supportedVersionRange=18.0,28.2";

			launcher1 = new ServerLauncher(null, "-city", "BHAM", "-service", server1Str);// ,
																					// "-killfile",
																					// "killfile1");
			launcher1.launchTestGRMJMSServer();
			Thread.sleep(5000);

			String uri = "http://DME2SEARCH/service=com.att.aft.testMatchVersionRangeEnabled/version=19/envContext=DEV/partner=test1?foo=bar&this=that";

			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet())
				table.put((String) key, props.get(key));
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			// ensures the request level handlers are set in dme2Manager level.
			table.put("AFT_DME2_MANAGER_NAME", "TestJMSSupportedVersionsRange");

			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			connection = factory.createQueueConnection();
			session = connection.createQueueSession(true, 0);
			remoteQueue = (Queue) context.lookup(uri);
			sender = session.createSender(remoteQueue);

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.dme2NonFailoverStatusCodes", "200,404");

			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);

			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		} finally {
			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");

			try {
				if (launcher1 != null)
					launcher1.destroy();
			} catch (Exception e) {
			}

			try {
				String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE",
						System.getProperty("user.home") + "/.aft/.dme2PortCache");

				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {
			}

			try {
				connection.close();
			} catch (Exception e) {
			}

			try {
				session.close();
			} catch (Exception e) {
			}

			try {
				sender.close();
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testMatchVersionRangeEnabledWithJMSHeader() throws Exception {
//		super.cleanPreviousEndpoints( "com.att.aft.TestJMSSupportedVersionsRange1", "28.2.0", "DEV" );
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");

		ServerLauncher launcher1 = null;
		QueueConnection connection = null;
		QueueSession session = null;
		Queue remoteQueue = null;
		QueueSender sender = null;
		Queue replyToQueue = null;

		try {
			String server1Str = "http://DME2LOCAL/service=com.att.aft.TestJMSSupportedVersionsRange1/version=28.2.0/envContext=DEV/routeOffer=BAU_NE?supportedVersionRange=18.0,28.2";

			launcher1 = new ServerLauncher(null, "-city", "BHAM", "-service", server1Str);// ,
																					// "-killfile",
																					// "killfile1");
			launcher1.launchTestGRMJMSServer();
			Thread.sleep(5000);

			String uri = "http://DME2SEARCH/service=com.att.aft.TestJMSSupportedVersionsRange1/version=19/envContext=DEV/partner=test1?foo=bar&this=that";

			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet())
				table.put((String) key, props.get(key));
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			// ensures the request level handlers are set in dme2Manager level.
			table.put("AFT_DME2_MANAGER_NAME", "TestJMSSupportedVersionsRange1");

			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			connection = factory.createQueueConnection();
			session = connection.createQueueSession(true, 0);
			remoteQueue = (Queue) context.lookup(uri);
			sender = session.createSender(remoteQueue);

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.matchVersionRange", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.dme2NonFailoverStatusCodes", "200,404");

			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);

			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		} finally {
			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");

			try {
				if (launcher1 != null)
					launcher1.destroy();
			} catch (Exception e) {
			}

			try {
				String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE",
						System.getProperty("user.home") + "/.aft/.dme2PortCache");

				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {
			}

			try {
				connection.close();
			} catch (Exception e) {
			}

			try {
				session.close();
			} catch (Exception e) {
			}

			try {
				sender.close();
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testMatchVersionRangeDisabledWithJMSHeader() throws Exception {
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");

		ServerLauncher launcher1 = null;
		QueueConnection connection = null;
		QueueSession session = null;
		Queue remoteQueue = null;
		QueueSender sender = null;
		Queue replyToQueue = null;

		try {
			String server1Str = "http://DME2LOCAL/service=com.att.aft.TestJMSSupportedVersionsRange/version=28.2.0/envContext=DEV/routeOffer=BAU_NE?supportedVersionRange=18.0,28.2";

			launcher1 = new ServerLauncher(null, "-city", "BHAM", "-service", server1Str);// ,
																					// "-killfile",
																					// "killfile1");
			launcher1.launchTestGRMJMSServer();
			Thread.sleep(5000);

			String uri = "http://DME2SEARCH/service=com.att.aft.TestJMSSupportedVersionsRange/version=19/envContext=DEV/partner=test1?foo=bar&this=that";

			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet())
				table.put((String) key, props.get(key));
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			// ensures the request level handlers are set in dme2Manager level.

			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			connection = factory.createQueueConnection();
			session = connection.createQueueSession(true, 0);
			remoteQueue = (Queue) context.lookup(uri);
			sender = session.createSender(remoteQueue);

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty("com.att.aft.dme2.jms.matchVersionRange", "false");
			msg.setStringProperty("com.att.aft.dme2.jms.dme2NonFailoverStatusCodes", "200,404");

			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			fail("Error occured - Expecting an exception to be thrown with error code: AFT-DME2-0702 (No endpoints were registered...)");
		} catch (Exception e) {
			System.out.println("--- Actual error that was returned: " + e.getMessage());
			assertTrue(e.getMessage().contains("AFT-DME2-0702"));
			assertTrue(e.getMessage().contains("version=19"));
		} finally {
			System.clearProperty("DME2_EP_TTL_MS");
			System.clearProperty("DME2_RT_TTL_MS");
			System.clearProperty("DME2_LEASE_REG_MS");
			System.clearProperty("platform");

			try {
				if (launcher1 != null)
					launcher1.destroy();
			} catch (Exception e) {
			}

			try {
				String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE",
						System.getProperty("user.home") + "/.aft/.dme2PortCache");

				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {
			}

			try {
				connection.close();
			} catch (Exception e) {
			}

			try {
				session.close();
			} catch (Exception e) {
			}

			try {
				sender.close();
			} catch (Exception e) {
			}
		}
	}

	private void assertTraceContains(String uri, String expected) throws Exception {
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet())
			table.put((String) key, props.get(key));
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		// ensures the request level handlers are set in dme2Manager level.
		table.put("AFT_DME2_MANAGER_NAME", "TestJMSSupportedVersionsRange");

		InitialContext context = new InitialContext(table);
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection();
		QueueSession session = connection.createQueueSession(true, 0);
		Queue remoteQueue = (Queue) context.lookup(uri);
		QueueSender sender = session.createSender(remoteQueue);
		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
		msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
		Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
		msg.setJMSReplyTo(replyToQueue);
		Thread.sleep(5000);

		try {
			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);

			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			String traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// client version 19 should result in being processed by BAU_NE
			// routeOffer
			if (expected != null)
				assertTrue("trace: " + traceInfo, traceInfo.contains(expected));
			else
				assertNull("trace: " + traceInfo, traceInfo);
		} catch (Exception e) {
			assertTrue("exception: " + e, e.getMessage().contains(expected));

		}

	}

}
