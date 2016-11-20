/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryFS;
import com.att.aft.dme2.test.jms.servlet.EchoResponseServlet;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.StaticCache;
import com.att.aft.dme2.test.jms.util.TestConstants;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.util.DME2Constants;

public class RemoteServiceTest extends JMSBaseTestCase {

  private static final Logger logger = LoggerFactory.getLogger( RemoteServiceTest.class );

	private DME2Configuration config = null;

	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testRemoteQueueRequest() throws Exception {
		ServerLauncher launcher = null;
		try {

			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// start service
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();
			Thread.sleep(3000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// String selector = replyReceiver.getMessageSelector();
			// fail(selector);
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}

		}
	}

	@Test
	public void testRemoteQueueRequest_WithDME2ProtocolClientURI() throws Exception {
		ServerLauncher launcher = null;
		try {
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context
					.lookup("dme2://DME2SEARCH/service=MyService/version=1.0.0/envContext=PROD");
					// remoteQueue =
					// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// start service
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();
			Thread.sleep(3000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// String selector = replyReceiver.getMessageSelector();
			// fail(selector);
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}

		}
	}

	@Test
	@Ignore
	public void testPreferredRouteOffer() throws Exception {
    logger.debug( null, "testPreferredRouteOffer", LogMessage.METHOD_ENTER );
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		ServerLauncher launcher1 = null;
		ServerLauncher launcher2 = null;
		ServerLauncher launcher3 = null;
		super.cleanPreviousEndpoints( TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME, "1.0.0", "DEV" );
		try {

			String server1Str = "http://DME2LOCAL/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/routeOffer=BAU_NE";
			String server2Str = "http://DME2LOCAL/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
			String server3Str = "http://DME2LOCAL/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/routeOffer=BAU_NW";

			String clientStr = "http://DME2SEARCH/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/partner=test1";

			logger.debug( null, "testPreferredRouteOffer", "Launching server 1" );
			launcher1 = new ServerLauncher(null, "-city", "BHAM", "-service", server1Str);
			launcher1.launchTestGRMJMSServer();
			Thread.sleep(3000);

			logger.debug( null, "testPreferredRouteOffer", "Launching server 2" );
			launcher2 = new ServerLauncher(null, "-city", "BHAM", "-service", server2Str);
			launcher2.launchTestGRMJMSServer();
			Thread.sleep(3000);

			logger.debug( null, "testPreferredRouteOffer", "Launching server 3" );
			launcher3 = new ServerLauncher(null, "-city", "BHAM", "-service", server3Str);
			launcher3.launchTestGRMJMSServer();
			Thread.sleep(3000);

			Properties props = RegistryFsSetup.init();
			List<String> defaultConfigs = new ArrayList<String>();
			defaultConfigs.add(JMSConstants.JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME);
			defaultConfigs.add(JMSConstants.DME_API_DEFAULT_CONFIG_FILE_NAME);
//			defaultConfigs.add(JMSConstants.METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME);
			
			DME2Configuration config = new DME2Configuration("TestJMSExchangePreferredRouteOffer", defaultConfigs, null, props);
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}

			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName("com.att.aft.TestJMSExchangePreferredRouteOffer");
			rtInfo.setServiceVersion("*");
			rtInfo.setEnvContext("DEV");
			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups(rtGrps);

			RouteGroup rg1 = new RouteGroup();
			rg1.setName("RG1");
			rg1.getPartner().add("test1");
			rg1.getPartner().add("test2");
			rg1.getPartner().add("test3");

			Route rt1 = new Route();
			rt1.setName("rt1");
			// rt1.setVersionSelector("1.0.0");
			RouteOffer ro1 = new RouteOffer();
			ro1.setActive(true);
			ro1.setSequence(1);
			ro1.setName("BAU_NE");

			Route rt2 = new Route();
			rt2.setName("rt2");
			// rt2.setVersionSelector("2.0.0");
			RouteOffer ro2 = new RouteOffer();
			ro2.setActive(true);
			ro2.setSequence(2);
			ro2.setName("BAU_SE");

			Route rt3 = new Route();
			rt3.setName("rt3");
			RouteOffer ro3 = new RouteOffer();
			ro3.setActive(true);
			ro3.setSequence(3);
			ro3.setName("BAU_SW");

			rt1.getRouteOffer().add(ro1);
			rt1.getRouteOffer().add(ro2);
			rt1.getRouteOffer().add(ro3);

			rtGrps.getRouteGroup();
			rtGrps.getRouteGroup().add(rg1);

			RegistryFsSetup grmInit = new RegistryFsSetup();
			RegistryFsSetup.init();
//			grmInit.saveRouteInfoForPreferedRoute(rtInfo, "DEV");
			Thread.sleep(2000);

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			// ensures the request level handlers are set in dme2Manager level.
			table.put("AFT_DME2_MANAGER_NAME", "TestJMSExchangePreferredRouteOffer");
			table.put(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS,
					"com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
			table.put(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS,
					"com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
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
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS,
					"com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS,
					"com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			logger.debug( null, "testPreferredRouteOffer", "SENDING REQUEST 1" );
			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			logger.debug( null, "testPreferredRouteOffer", "RECEIVING REQUEST 1" );
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
			String traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			assertTrue(traceInfo.contains("/routeOffer=BAU_NE:onResponseCompleteStatus=200"));

			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS,
          "com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS,
          "com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			logger.debug( null, "testPreferredRouteOffer", "SENDING REQUEST 2" );
			sender.send(msg);
			replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			logger.debug( null, "testPreferredRouteOffer", "RECEIVING REQUEST 2" );
			rcvMsg = (TextMessage) replyReceiver.receive(10000);
			traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// PreferredRouteRequest handler sets BAU_SE as preferred offer and
			// hence should be attempted first
			assertTrue("Should contain /routeOffer=BAU_SE:onResponseCompleteStatus=200 but was " + traceInfo, traceInfo.contains("/routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
      msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS,
          "com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
      msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS,
          "com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
      replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

      logger.debug( null, "testPreferredRouteOffer", "SENDING REQUEST 3" );
			sender.send(msg);

			replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
      logger.debug( null, "testPreferredRouteOffer", "RECEIVING REQUEST 3" );
			rcvMsg = (TextMessage) replyReceiver.receive(10000);
			traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// PreferredRouteRequest handler sets BAU_NW now as preferred offer
			// and hence should be attempted first
			assertTrue(traceInfo.contains("/routeOffer=BAU_NW:onResponseCompleteStatus=200"));

		} finally {
			StaticCache.getInstance().setRouteOffer(null);
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
				String portCacheFilePath = config.getProperty("AFT_DME2_PORT_CACHE_FILE", System.getProperty("user.home") + "/.aft/.dme2PortCache");
				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {

			}
		}
	}

	@Test
	@Ignore
	public void testPrimarySequenceDownOnStartup() throws Exception {
		logger.debug( null, "testPrimarySequenceDownOnStartup", LogMessage.METHOD_ENTER );
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("DME2.DEBUG", "true");
		ServerLauncher launcher1 = null;
		ServerLauncher launcher2 = null;
		ServerLauncher launcher3 = null;
		super.cleanPreviousEndpoints( TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME, "1.0.0", "DEV" );
		try {

			String server1Str = "http://DME2LOCAL/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/routeOffer=BAU_NE";
			String server2Str = "http://DME2LOCAL/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";
			String server3Str = "http://DME2LOCAL/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/routeOffer=BAU_NW";

			String clientStr = "http://DME2SEARCH/service="+TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME+"/version=1.0.0/envContext=DEV/partner=test1";

			launcher1 = new ServerLauncher(null, "-city", "BHAM", "-service", server1Str);
			// Don't start the primary server at this point
			// launcher1.launchTestGRMJMSServer();
			// Thread.sleep(3000);

			launcher2 = new ServerLauncher(null, "-city", "BHAM", "-service", server2Str);
			launcher2.launchTestGRMJMSServer();
			Thread.sleep(3000);

			launcher3 = new ServerLauncher(null, "-city", "BHAM", "-service", server3Str);
			launcher3.launchTestGRMJMSServer();
			Thread.sleep(3000);

			Properties props = RegistryFsSetup.init();
			List<String> defaultConfigs = new ArrayList<String>();
			defaultConfigs.add(JMSConstants.JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME);
			defaultConfigs.add(JMSConstants.DME_API_DEFAULT_CONFIG_FILE_NAME);
//			defaultConfigs.add(JMSConstants.METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME);
			
			DME2Configuration config = new DME2Configuration("TestPrimarySequenceDownOnStartup", defaultConfigs, null, props);			

			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}

			RouteInfo rtInfo = new RouteInfo();
			rtInfo.setServiceName(TestConstants.JMS_EXCHANGE_PREFERRED_ROUTEOFFER_SERVICE_NAME);
			rtInfo.setServiceVersion("*");
			rtInfo.setEnvContext("DEV");
			RouteGroups rtGrps = new RouteGroups();
			rtInfo.setRouteGroups(rtGrps);

			RouteGroup rg1 = new RouteGroup();
			rg1.setName("RG1");
			rg1.getPartner().add("test1");
			rg1.getPartner().add("test2");
			rg1.getPartner().add("test3");

			Route rt1 = new Route();
			rt1.setName("rt1");
			// rt1.setVersionSelector("1.0.0");
			RouteOffer ro1 = new RouteOffer();
			ro1.setActive(true);
			ro1.setSequence(1);
			ro1.setName("BAU_NE");

			Route rt2 = new Route();
			rt2.setName("rt2");
			// rt2.setVersionSelector("2.0.0");
			RouteOffer ro2 = new RouteOffer();
			ro2.setActive(true);
			ro2.setSequence(2);
			ro2.setName("BAU_SE");

			Route rt3 = new Route();
			rt3.setName("rt3");
			RouteOffer ro3 = new RouteOffer();
			ro3.setActive(true);
			ro3.setSequence(3);
			ro3.setName("BAU_SW");

			rt1.getRouteOffer().add(ro1);
			rt1.getRouteOffer().add(ro2);
			rt1.getRouteOffer().add(ro3);

			rtGrps.getRouteGroup();
			rtGrps.getRouteGroup().add(rg1);

			RegistryFsSetup grmInit = new RegistryFsSetup();
//			grmInit.saveRouteInfoForPreferedRoute(rtInfo, "DEV");
			Thread.sleep(2000);

			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			// ensures the request level handlers are set in dme2Manager level.
			table.put("AFT_DME2_MANAGER_NAME", "TestPrimarySequenceDownOnStartup");
			//table.put(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS, "com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
			//table.put(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS,"com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
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
			//msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS,"com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
			//msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS,"com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			logger.debug( null, "testPrimarySequenceDownOnStartup", "SENDING FIRST REQUEST" );
			sender.send(msg);
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			logger.debug( null, "testPrimarySequenceDownOnStartup", "RECEIVING FIRST REQUEST" );
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			String traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// Since primary sequence BAU_NE is down, BAU_SE with sequence 2
			// should be attempted as failover option
			assertTrue(traceInfo.contains("/routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS,"com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS,"com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			logger.debug( null, "testPrimarySequenceDownOnStartup", "SENDING SECOND REQUEST" );
			sender.send(msg);
			replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			logger.debug( null, "testPrimarySequenceDownOnStartup", "RECEIVING SECOND REQUEST" );
			rcvMsg = (TextMessage) replyReceiver.receive(30000);
			traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			assertTrue("Expected traceinfo to contain BAU_SE, was " + traceInfo, traceInfo.contains("/routeOffer=BAU_SE:onResponseCompleteStatus=200"));

			sender = session.createSender(remoteQueue);
			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", "test1");
			msg.setStringProperty("AFT_DME2_REQ_TRACE_ON", "true");
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS, "com.att.aft.dme2.test.jms.util.PreferredRouteRequestHandler");
			msg.setStringProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS, "com.att.aft.dme2.test.jms.util.PreferredRouteReplyHandler");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			logger.debug( null, "testPrimarySequenceDownOnStartup", "SENDING THIRD REQUEST" );
			sender.send(msg);
			replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			logger.debug( null, "testPrimarySequenceDownOnStartup", "RECEIVING THIRD REQUEST" );
			rcvMsg = (TextMessage) replyReceiver.receive(10000);
			traceInfo = rcvMsg.getStringProperty("AFT_DME2_REQ_TRACE_INFO");
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// PreferredRouteRequest handler sets BAU_NW now as preferred offer
			// and hence should be attempted first
			assertTrue("Was expecting to see BAU_NW as routeoffer in traceInfo.  Got " + traceInfo, traceInfo.contains("/routeOffer=BAU_NW:onResponseCompleteStatus=200"));

		} finally {
			StaticCache.getInstance().setRouteOffer(null);
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
				String portCacheFilePath =config.getProperty("AFT_DME2_PORT_CACHE_FILE", System.getProperty("user.home") + "/.aft/.dme2PortCache");
				File file = new File(portCacheFilePath);
				file.delete();
			} catch (Exception e) {

			}
      logger.debug( null, "testPrimarySequenceDownOnStartup", LogMessage.METHOD_EXIT );
		}
	}

	/** test ignore failover messages */
	@Test
	@Ignore
	public void testRemoteQueueRequestIgnoreFailoverViaJMSProp() throws Exception {
		ServerLauncher launcher = null;
		try {
			// System.setProperty("DME2.DEBUG", "true");
			System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("DME2.DEBUG", "true");
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.longRunServiceSearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchLongRunTestJMSServer();

			Thread.sleep(5000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("AFT_DME2_EP_READ_TIMEOUT_MS", "29000");
			msg.setStringProperty("com.att.aft.dme2.jms.ignoreFailOverOnExpire", "true");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);
			msg.setJMSExpiration(31000);

			long start = System.currentTimeMillis();
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
				long elapsedTime = System.currentTimeMillis() - start;
				// Currently the longrunListener is forced to sleep for 120000.
				// So make sure any change for below assert
				// is directly depending on LongRunMessageLister impl
				System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
				//assertTrue(elapsedTime > 120000);
				// TextMessage rcvMsg = (TextMessage)
				// replyReceiver.receiveNoWait();
				assertEquals("LongRunMessageListener:::TEST", rcvMsg.getText());
				System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0709"));
				try {
					launcher.destroy();
					Thread.sleep(2000);
				} catch (Exception e1) {

				}

			}
		} finally {
			if (launcher != null)
				launcher.destroy();
		}
	}

	/** test ignore failover messages via query uri query param */

	@Ignore
	@Test
	public void testRemoteQueueRequestIgnoreFailoverViaQueryParam() throws Exception {
		ServerLauncher launcher = null;
		try {
			// System.setProperty("DME2.DEBUG", "true");
			System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("DME2.DEBUG", "true");
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context
					.lookup(TestConstants.longRunServiceSearchStr + "?ignoreFailoverOnExpire=true");
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchLongRunTestJMSServer();

			Thread.sleep(20000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("AFT_DME2_EP_READ_TIMEOUT_MS", "29000");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);
			msg.setJMSExpiration(31000);

			long start = System.currentTimeMillis();
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
				long elapsedTime = System.currentTimeMillis() - start;
				// Currently the longrunListener is forced to sleep for 120000.
				// So make sure any change for below assert
				// is directly depending on LongRunMessageLister impl
				System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
				assertTrue(elapsedTime > 120000);
				// TextMessage rcvMsg = (TextMessage)
				// replyReceiver.receiveNoWait();
				assertEquals("LongRunMessageListener:::TEST", rcvMsg.getText());
				System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0709"));
				try {
					launcher.destroy();
					Thread.sleep(2000);
				} catch (Exception e1) {

				}
			}
		} finally {
			TestConstants.removePortCache();
			if (launcher != null)
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
		}

	}

	/** test ignore failover messages via jvm */
	@Test
@Ignore	
	public void remoteQueueRequestIgnoreFailoverViaJVMarg() throws Exception {
		ServerLauncher launcher = null;
		try {
			// System.setProperty("DME2.DEBUG", "true");
			System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			System.setProperty("AFT_DME2_IGNORE_FAILOVER_ONEXPIRE", "true");
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("DME2.DEBUG", "true");
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.longRunServiceSearchStrAFT);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchLongRunTestJMSServer();

			Thread.sleep(20000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("AFT_DME2_EP_READ_TIMEOUT_MS", "29000");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);
			msg.setJMSExpiration(31000);

			long start = System.currentTimeMillis();
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {

				TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
				//TextMessage rcvMsg = (TextMessage) replyReceiver.receive(80000);
				long elapsedTime = System.currentTimeMillis() - start;
				// Currently the longrunListener is forced to sleep for 120000.
				// So make sure any change for below assert
				// is directly depending on LongRunMessageLister impl
				System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
				//assertTrue(elapsedTime > 120000);
				// TextMessage rcvMsg = (TextMessage)
				// replyReceiver.receiveNoWait();
				assertEquals("LongRunMessageListener:::TEST", rcvMsg.getText());
				System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0709"));
				try {
					launcher.destroy();
					Thread.sleep(2000);
				} catch (Exception e1) {

				}
			}
		} finally {
			System.clearProperty("AFT_DME2_IGNORE_FAILOVER_ONEXPIRE");
			TestConstants.removePortCache();
			if (launcher != null)
				launcher.destroy();
		}

	}

	/** trademark symbol testing */
	@Test

	public void testRemoteQueueRequestUTF8Client() throws Exception {
		ServerLauncher launcher = null;
		Locations.BHAM.set();
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection();
		QueueSession session = connection.createQueueSession(true, 0);
		Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
		// remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);
		try {
			// start service
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();
			Thread.sleep(3000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();
			String utf8String = null;
			File f = new File("src/test/resources/utf8data.txt");
			String tempData = null;
			StringBuffer strBuf = new StringBuffer();
			if (f.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(f));
				while ((tempData = br.readLine()) != null) {
					strBuf.append(tempData);
				}
				utf8String = strBuf.toString();
			}
			TextMessage msg = session.createTextMessage();
			msg.setText(utf8String);
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty(DME2Constants.DME2_JMS_REQUEST_PARTNER_CLASS, TestConstants.partner);
			msg.setStringProperty(DME2Constants.DME2_JMS_REQUEST_CHARSET_CLASS, "UTF-8");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			// make sure the server got OUR charset
			assertEquals(rcvMsg.getStringProperty("com.att.aft.dme2.jms.test.charset"), "UTF-8");
		} finally {
			if (launcher != null)
				launcher.destroy();
		}
		// String selector = replyReceiver.getMessageSelector();
		// fail(selector);
	}

	/** trademark testing server */
	@Test

	public void testRemoteQueueRequestUTF8Server() throws Exception {
		Locations.BHAM.set();
		ServerLauncher launcher = null;

		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection();
		QueueSession session = connection.createQueueSession(true, 0);
		Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
		// remoteQueue = (Queue)context.lookup(TestConstants.dme2ResolveStr);

		// start service
		// TestJMSServer server = new TestJMSServer();
		try {
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();

			Thread.sleep(2000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();
			String utf8String = null;
			File f = new File("src/test/resources/utf8data.txt");
			String tempData = null;
			StringBuffer strBuf = new StringBuffer();
			if (f.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(f));
				while ((tempData = br.readLine()) != null) {
					strBuf.append(tempData);
				}
				utf8String = strBuf.toString();
			}
			TextMessage msg = session.createTextMessage();
			msg.setText(utf8String);
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("com.att.aft.dme2.jms.charset", "UTF-8");
			msg.setStringProperty("com.att.aft.dme2.jms.test.useForReplyCharSet", "UTF-8");
			msg.setStringProperty("com.att.aft.dme2.jms.test.echoRequestText", "true");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			// make sure the server got OUR charset
			System.out.println(rcvMsg.getText()); // for visual check...
			assertEquals("UTF-8", rcvMsg.getStringProperty("com.att.aft.dme2.jms.charset"));
			assertEquals(utf8String, rcvMsg.getText());
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}
		}

	}

	@Test
	public void testRemoteQueueRequest4() throws Exception {
		ServerLauncher launcher = null;
		try {
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();

			Thread.sleep(3000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
			// String selector = replyReceiver.getMessageSelector();
			// fail(selector);
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}

		}
	}

	@Test
	@Ignore
	public void longRunTest() throws Exception {
		Locations.BHAM.set();
		ServerLauncher launcher = null;
		try {
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.longRunServiceSearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchLongRunTestJMSServer();

			Thread.sleep(20000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setLongProperty("com.att.aft.dme2.perEndpointTimeoutMs", 140000);
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);
			msg.setJMSExpiration(125000);

			long start = System.currentTimeMillis();
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(160000);
			long elapsedTime = System.currentTimeMillis() - start;
			// Currently the longrunListener is forced to sleep for 90000. So
			// make sure any change for below assert
			// is directly depending on LongRunMessageLister impl
			System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
			assertTrue(elapsedTime > 120000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertEquals("LongRunMessageListener:::TEST", rcvMsg.getText());
			System.out.println(" LongRunMessageListener responded after " + elapsedTime + " ms");
			// String selector = replyReceiver.getMessageSelector();
			// fail(selector);
		} finally {
			TestConstants.removePortCache();
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {
				}
			}
		}
	}

	@Test
	public void testRemoteQueueRequest3() throws Exception {
		Locations.BHAM.set();
		ServerLauncher launcher = null;
		try {
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// run the server in bham.
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();

			Thread.sleep(10000);

			QueueSender sender = session.createSender(remoteQueue);
			Queue replyToQueue = session.createTemporaryQueue();
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setJMSReplyTo(replyToQueue);

			sender.send(msg);
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
			}
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}

		}
	}

	@Test
	public void testJMSCorrelationID() throws Exception {

		Locations.BHAM.set();
		ServerLauncher launcher = null;
		try {
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);

			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// run the server in bham.
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();

			Thread.sleep(10000);

			System.out.println("Looking up reply Queue");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			QueueConnection conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			QueueSession session = conn.createQueueSession(true, 0);

			System.out.println("Creating MessageProducer");
			QueueSender sender = session.createSender(remoteQueue);
			TextMessage message = session.createTextMessage();

			// Data context and partner are required if the routing affinity has
			// to be with data or partner based
			message.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			message.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			message.setText("TEST");

			message.setJMSReplyTo(replyToQueue);

			sender.send(message);
			
			Thread.sleep(3000);

			QueueReceiver consumer = session.createReceiver(replyToQueue,
					"JMSCorrelationID = '" + message.getJMSMessageID() + "'");
			// TextMessage rcvMsg = (TextMessage) consumer.receiveNoWait();
			TextMessage rcvMsg = (TextMessage) consumer.receive(60000);
			System.out.println("testJMSCorrelationID consumer.getMessageSelector()=" + consumer.getMessageSelector());
			System.out.println("testJMSCorrelationID rcvMsg.getJMSCorrelationID()=" + rcvMsg.getJMSCorrelationID());
			assertEquals(message.getJMSMessageID(), rcvMsg.getJMSCorrelationID());
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}

		}
	}

	@Test
	public void testJMSCorrelationIDNegative() throws Exception {

		Locations.BHAM.set();
		ServerLauncher launcher = null;
		try {
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// run the server in bham.
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestJMSServer();

			Thread.sleep(10000);

			System.out.println("Looking up reply Queue");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			QueueConnection conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			QueueSession session = conn.createQueueSession(true, 0);

			System.out.println("Creating MessageProducer");
			QueueSender sender = session.createSender(remoteQueue);
			TextMessage message = session.createTextMessage();

			// Data context and partner are required if the routing affinity has
			// to be with data or partner based
			message.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			message.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			message.setText("TEST");

			message.setJMSReplyTo(replyToQueue);

			sender.send(message);
			
			Thread.sleep(10000);

			// add X so that we shouldn't find the msg - negative test
			QueueReceiver consumer = session.createReceiver(replyToQueue,
					"JMSCorrelationID = '" + message.getJMSMessageID() + "X'");
			// TextMessage rcvMsg = (TextMessage) consumer.receiveNoWait();
			TextMessage rcvMsg = (TextMessage) consumer.receive(40000);
			System.out.println(
					"testJMSCorrelationIDNegative consumer.getMessageSelector()=" + consumer.getMessageSelector());
			// shouldn't find msg - so should be null
			assertNull(rcvMsg);
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}

		}
	}

	@Test

	public void testQNameRouteOfferViaJVMArgs() throws Exception {
		ServerLauncher launcher = null;
		Locations.BHAM.set();
		try {
			Properties props1 = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props1.keySet()) {
				table.put((String) key, props1.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2SearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// run the server in bham.
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchTestLrmROJMSServer();

			Thread.sleep(10000);
			config = new DME2Configuration("testQNameRouteOfferViaJVMArgs", RegistryFsSetup.init());
			DME2EndpointRegistry registry = new DME2EndpointRegistryFS(config, "testQNameRouteOfferViaJVMArgs");
			String currDir = (new File(System.getProperty("user.dir"))).getAbsolutePath();
			String srcConfigDir = currDir + File.separator + "src" + File.separator + "test" + File.separator + "etc"
					+ File.separator + "svc_config";
			String fsDir = currDir + "/dme2-fs-registry";

			Properties props = new Properties();
			props.setProperty("AFT_DME2_SVCCONFIG_DIR", "file:///" + srcConfigDir);
			props.setProperty("AFT_DME2_EP_REGISTRY_FS_DIR", fsDir);
			props.setProperty("DME2_EP_REGISTRY_CLASS", "DME2FS");
			props.setProperty("platform", "SANDBOX-DEV");
			props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
			props.setProperty("AFT_LATITUDE", "33.373900");
			props.setProperty("AFT_LONGITUDE", "-86.798300");
			props.setProperty("DME2_MANAGER_NAME", "TestManager");
			System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
			System.setProperty("AFT_LATITUDE", "33.373900");
			System.setProperty("AFT_LONGITUDE", "-86.798300");
			config = new DME2Configuration("testQNameRouteOfferViaJVMArgs", props);
			DME2Manager manager = new DME2Manager("testQNameRouteOfferViaJVMArgs", config);
			registry = (DME2EndpointRegistry) manager.getEndpointRegistry();
			List<DME2Endpoint> endpoints = registry.findEndpoints("MyService", "1.0.0", "DEV", "BAU_SE_1");
			for (DME2Endpoint endpoint : endpoints) {
				System.out
						.println("Endpoint retrieved should have routeOffer in path as BAU_SE_1 " + endpoint.getPath());
				assertTrue(endpoint.getPath().endsWith("routeOffer=BAU_SE_1"));
			}
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {

				}
			}

		}

	}

	@Test
	public void testJMSUnavailable() throws Exception {
		Locations.BHAM.set();
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		InitialContext context = new InitialContext(table);
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		QueueConnection connection = factory.createQueueConnection();
		QueueSession session = connection.createQueueSession(true, 0);
		// Queue remoteQueue =
		// (Queue)context.lookup(TestConstants.dme2SearchStr+"1");
		// Queue remoteQueue =
		// (Queue)context.lookup("http://DME2RESOLVE/service=aa/version=1.0/envContext=TEST/routeOffer=P1");
		Queue remoteQueue = (Queue) context.lookup(TestConstants.dme2ResolveStr + "1");

		QueueSender sender = session.createSender(remoteQueue);

		TextMessage msg = session.createTextMessage();
		msg.setText("TEST");
		msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
		msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
		Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
		msg.setJMSReplyTo(replyToQueue);
		QueueReceiver replyReceiver = null;
		try {
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			replyReceiver = session.createReceiver(replyToQueue);

			Thread.sleep(1000);
		} catch (Exception ex) {
			ex.printStackTrace();
			assertTrue(ex.getMessage().contains("AFT-DME2-0702"));
			return;
		}
		try {
			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertNull(rcvMsg);
		} catch (Exception e) {
			// since JMS provider is not available, receive should throw an
			// exception
			// for fast fail condition
			assertTrue(e.getMessage().contains("AFT-DME2-5401"));
		}
		// String selector = replyReceiver.getMessageSelector();
		// fail(selector);
	}

	@Test
	public void testRemoteJMSQueueMessageSelector() throws Exception {
		ServerLauncher launcher = null;
		try {
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.remoteMsgSelectorResolveStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// run the server in bham.
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchRemoteMsgSelectorJMSServer();

			Thread.sleep(10000);

			System.out.println("Looking up reply Queue");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			QueueConnection conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			QueueSession session = conn.createQueueSession(true, 0);

			System.out.println("Creating MessageProducer");
			QueueSender sender = session.createSender(remoteQueue);
			TextMessage message = session.createTextMessage();

			message.setText("TEST");
			message.setJMSReplyTo(replyToQueue);
			message.setJMSMessageID("1");

			sender.send(message);
			
			

			QueueReceiver consumer = session.createReceiver(replyToQueue,
					"JMSCorrelationID = '" + message.getJMSMessageID() + "'");
			// TextMessage rcvMsg = (TextMessage) consumer.receiveNoWait();
			TextMessage rcvMsg = (TextMessage) consumer.receive(40000);
			System.out.println("testJMSCorrelationID consumer.getMessageSelector()=" + consumer.getMessageSelector());
			System.out.println("testJMSCorrelationID rcvMsg.getJMSCorrelationID()=" + rcvMsg.getJMSCorrelationID());
			assertEquals(message.getJMSMessageID(), rcvMsg.getJMSCorrelationID());
			assertEquals("LocalQueueMessageListener:::TEST", rcvMsg.getText());
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {
				}
			}
		}
	}

	@Test
	@Ignore
	public void testJMSSearchURI() throws Exception {
		String name = "service=com.att.aft.TestJMSSearchURI/version=1.0.0/envContext=DEV/routeOffer=BAU_SE";

		// SEARCH URI with / at end
		String cname = "http://DME2SEARCH/service=com.att.aft.TestJMSSearchURI/version=1.0.0/envContext=DEV/";
		DME2Manager manager = null;
		try {
			System.setProperty("DME2.DEBUG", "true");
			System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			Properties props = RegistryFsSetup.init();
			config = new DME2Configuration("TestJMSSearchURI", props);
			manager = new DME2Manager("TestJMSSearchURI", config);

			manager.bindServiceListener(name, new EchoResponseServlet(name, "bau_se_1"), null, null, null);
			Thread.sleep(5000);

			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			Queue remoteQueue = (Queue) context.lookup(cname);

			System.out.println("Looking up reply Queue");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			QueueConnection conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			QueueSession session = conn.createQueueSession(true, 0);

			QueueSender sender = session.createSender(remoteQueue);

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("com.att.aft.dme2.jms.queryParams", "TEST_ARG1=PARAM 1&TEST_ARG2=PARAM 2");

			msg.setJMSReplyTo(replyToQueue);
			QueueReceiver replyReceiver = null;
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			replyReceiver = session.createReceiver(replyToQueue);

			Thread.sleep(1000);

			TextMessage rcvMsg = (TextMessage) replyReceiver.receive(10000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertTrue(rcvMsg.getText().contains("TEST_ARG:"));

			// NO SLASH at end of DME2 URI
			cname = "http://DME2SEARCH/service=com.att.aft.TestJMSSearchURI/version=1.0.0/envContext=DEV";
			remoteQueue = (Queue) context.lookup(cname);

			System.out.println("Looking up reply Queue");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			session = conn.createQueueSession(true, 0);

			sender = session.createSender(remoteQueue);

			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("com.att.aft.dme2.jms.queryParams", "TEST_ARG1=PARAM 1&TEST_ARG2=PARAM 2");

			msg.setJMSReplyTo(replyToQueue);
			replyReceiver = null;
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			replyReceiver = session.createReceiver(replyToQueue);

			Thread.sleep(1000);

			rcvMsg = (TextMessage) replyReceiver.receive(10000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertTrue(rcvMsg.getText().contains("TEST_ARG:"));

			// NO SLASH at end of DME2 URI, version provided
			cname = "http://DME2SEARCH/version=1.0.0/envContext=DEV/service=com.att.aft.TestJMSSearchURI/";
			remoteQueue = (Queue) context.lookup(cname);

			System.out.println("Looking up reply Queue");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			session = conn.createQueueSession(true, 0);

			sender = session.createSender(remoteQueue);

			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("com.att.aft.dme2.jms.queryParams", "TEST_ARG1=PARAM 1&TEST_ARG2=PARAM 2");

			msg.setJMSReplyTo(replyToQueue);
			replyReceiver = null;
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			replyReceiver = session.createReceiver(replyToQueue);

			Thread.sleep(1000);

			rcvMsg = (TextMessage) replyReceiver.receive(10000);
			// TextMessage rcvMsg = (TextMessage) replyReceiver.receiveNoWait();
			assertTrue(rcvMsg.getText().contains("TEST_ARG:"));

			// Partner not provided in URI as well as msg property
			cname = "http://DME2SEARCH/service=com.att.aft.TestJMSSearchURI/version=1.0.0/envContext=DEV";
			remoteQueue = (Queue) context.lookup(cname);

			System.out.println("Looking up reply Queue");
			replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			session = conn.createQueueSession(true, 0);

			sender = session.createSender(remoteQueue);

			msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			// msg.setStringProperty("com.att.aft.dme2.jms.partner",
			// TestConstants.partner);
			msg.setStringProperty("com.att.aft.dme2.jms.queryParams", "TEST_ARG1=PARAM 1&TEST_ARG2=PARAM 2");

			msg.setJMSReplyTo(replyToQueue);
			replyReceiver = null;
			try {
				sender.send(msg);
				// QueueReceiver replyReceiver =
				// session.createReceiver(replyToQueue,
				// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
				replyReceiver = session.createReceiver(replyToQueue);

				Thread.sleep(1000);

				rcvMsg = (TextMessage) replyReceiver.receive(10000);
				// TextMessage rcvMsg = (TextMessage)
				// replyReceiver.receiveNoWait();
				assertTrue(rcvMsg == null);
			} catch (Exception e) {
				// URI Invalid: partner not found
				assertTrue(e.getMessage().contains("AFT-DME2-9703"));
			}

		} finally {
			System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
			System.clearProperty("DME2.DEBUG");
			manager.unbindServiceListener(name);
		}
	}

	@Test
	public void testRemoteJMSQueueMessageSelectorFailure() throws Exception {
		ServerLauncher launcher = null;
		try {
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.remoteMsgSelectorResolveStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			// run the server in bham.
			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchRemoteMsgSelectorJMSServer();

			Thread.sleep(10000);

			System.out.println("Looking up reply Queue");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");

			System.out.println("Creating QueueConnection");
			QueueConnection conn = qcf.createQueueConnection();

			System.out.println("Creating Session");
			QueueSession session = conn.createQueueSession(true, 0);

			System.out.println("Creating MessageProducer");
			QueueSender sender = session.createSender(remoteQueue);
			TextMessage message = session.createTextMessage();

			message.setText("TEST");
			message.setJMSReplyTo(replyToQueue);
			// message.setJMSMessageID("1");

			sender.send(message);

			QueueReceiver consumer = session.createReceiver(replyToQueue,
					"JMSCorrelationID = '" + message.getJMSMessageID() + "'");
			// TextMessage rcvMsg = (TextMessage) consumer.receiveNoWait();
			TextMessage rcvMsg = (TextMessage) consumer.receive(10000);
			System.out.println("testJMSCorrelationID consumer.getMessageSelector()=" + consumer.getMessageSelector());
			System.out.println("testJMSCorrelationID rcvMsg.getJMSCorrelationID()=" + rcvMsg.getJMSCorrelationID());
			assertEquals(message.getJMSMessageID(), rcvMsg.getJMSCorrelationID());
			assertTrue(rcvMsg.getText().contains("MISMATCH"));
		} finally {
			if (launcher != null) {
				try {
					launcher.destroy();
				} catch (Exception e) {
				}
			}
		}
	}

	/** test empty reply message */
	@Test
	@Ignore
	public void testEmptyReplyMessage() throws Exception {
		ServerLauncher launcher = null;
		try {
			// System.setProperty("DME2.DEBUG", "true");
			System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("DME2.DEBUG", "true");
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			table.put("AFT_DME2_MANAGER_NAME", "GRMRegistry");
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			QueueSession session = connection.createQueueSession(true, 0);
			Queue remoteQueue = (Queue) context.lookup(TestConstants.emptyReplyServiceSearchStr);
			// remoteQueue =
			// (Queue)context.lookup(TestConstants.dme2ResolveStr);

			launcher = new ServerLauncher(null, "-city", "BHAM");
			launcher.launchEmptyReplyTestJMSServer();

			Thread.sleep(20000);

			QueueSender sender = session.createSender(remoteQueue);

			// Queue replyToQueue = session.createTemporaryQueue();

			TextMessage msg = session.createTextMessage();
			msg.setText("TEST");
			msg.setStringProperty("com.att.aft.dme2.jms.dataContext", TestConstants.dataContext);
			msg.setStringProperty("com.att.aft.dme2.jms.partner", TestConstants.partner);
			msg.setStringProperty("AFT_DME2_EP_READ_TIMEOUT_MS", "29000");
			Queue replyToQueue = (Queue) context.lookup("http://DME2LOCAL/clientResponseQueue");
			msg.setJMSReplyTo(replyToQueue);
			msg.setJMSExpiration(31000);

			long start = System.currentTimeMillis();
			sender.send(msg);
			// QueueReceiver replyReceiver =
			// session.createReceiver(replyToQueue,
			// "JMSCorrelationID = '" + msg.getJMSMessageID() + "'");
			QueueReceiver replyReceiver = session.createReceiver(replyToQueue);
			try {
				TextMessage rcvMsg = (TextMessage) replyReceiver.receive(30000);
				long elapsedTime = System.currentTimeMillis() - start;
				// Currently the longrunListener is forced to sleep for 120000.
				// So make sure any change for below assert
				// is directly depending on LongRunMessageLister impl
				System.out.println(" EmptyReplyMessageListener responded after " + elapsedTime + " ms");
				// TextMessage rcvMsg = (TextMessage)
				// replyReceiver.receiveNoWait();
				assertEquals("", rcvMsg.getText());
				System.out.println(" EmptyReplyMessageListener responded after " + elapsedTime + " ms");
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(e.getMessage().contains("AFT-DME2-0709"));
				try {
					launcher.destroy();
					Thread.sleep(2000);
				} catch (Exception e1) {

				}

			}
		} finally {
			if (launcher != null)
				launcher.destroy();
		}
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public static void main(String a[]) throws Exception {
		RemoteServiceTest rt = new RemoteServiceTest();
		rt.setUp();
		rt.longRunTest();
	}
}
