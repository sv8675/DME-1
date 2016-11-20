/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.handler.DME2RestfulHandler;
import com.att.aft.dme2.jms.DME2JMSInitialContext;
import com.att.aft.dme2.jms.DME2JMSInitialContextFactory;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;
import com.att.aft.dme2.test.jms.util.TestThrottleMessageReceiver;
import com.att.aft.dme2.util.DME2Constants;
@net.jcip.annotations.NotThreadSafe

public class TestDME2ThrottleFilterQueueReceiver extends JMSBaseTestCase {
	private static final String SOMEOTHER_PARTNER = "SOMEOTHER_PARTNER";
	private DME2Manager manager;
	private static final int MAX_RECEIVERS = 10;
	private static final double PARTNER_THROTTLE_50_PCT = 50.0;
	private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_50_PCT_THROTTLE = (int) Math.ceil(MAX_RECEIVERS * (PARTNER_THROTTLE_50_PCT / 100.0));
	private static final double PARTNER_THROTTLE_20_PCT = 20.0;
	private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE = (int) Math.ceil(MAX_RECEIVERS * (PARTNER_THROTTLE_20_PCT / 100.0));

	@Before
	public void setUp() throws Exception {
		super.setUp();
		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		DME2JMSInitialContext context = (DME2JMSInitialContext) new DME2JMSInitialContextFactory().getInitialContext(table);
	}

	@Test
	public void testThrottlesAPartnerOnAJMSServiceWithQueueReceiver() throws Exception {
		String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter28/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter28/version=1.0.0/envContext=PROD";

		try {
			// start service with 10 active queue receiver listeners and 20% = 2
			// active threads per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
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
			String servicetoregister = serviceToRegister + "?throttleFilterDisabled=false&throttlePctPerPartner=" + PARTNER_THROTTLE_20_PCT;
			Queue requestQueue = (Queue) context.lookup(servicetoregister);
			Thread.sleep(5000);
			TestThrottleMessageReceiver[] receiverThreads = new TestThrottleMessageReceiver[TestConstants.listenerCount];
			for (int i = 0; i < MAX_RECEIVERS; i++) {
				receiverThreads[i] = new TestThrottleMessageReceiver(connection, session, requestQueue, 10000);
				receiverThreads[i].start();
			}
			Thread.sleep(10000);

			manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in
			// Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 10000l);
			}
			Thread.sleep(5000);
			// verify max active limit reached
			System.out.println("Sending 1 more request");
			DME2RestfulHandler replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
			try {
				replyHandler.getResponse( 10000 );
			} catch(DME2Exception ex) {
				assert(ex.getMessage().contains("AFT-DME2-0703"));
			}
		} catch(DME2Exception ex) {
			assert(ex.getMessage().contains("AFT-DME2-0703"));
		} finally {
			try {
				manager.shutdown();
				Thread.sleep(2000);
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testQueueReceiverThrottlesOnePartnerWhileAllowsOthers() throws Exception {
		String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter29/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter29/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";

		try {
			// start service with 10 active queue receiver listeners and 20% = 2
			// active threads per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
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
			String servicetoregister = serviceToRegister + "?throttleFilterDisabled=false&throttlePctPerPartner=" + PARTNER_THROTTLE_20_PCT;
			Queue requestQueue = (Queue) context.lookup(servicetoregister);
			Thread.sleep(5000);
			TestThrottleMessageReceiver[] receiverThreads = new TestThrottleMessageReceiver[TestConstants.listenerCount];
			for (int i = 0; i < MAX_RECEIVERS; i++) {
				receiverThreads[i] = new TestThrottleMessageReceiver(connection, session, requestQueue, 10000);
				receiverThreads[i].start();
			}
			Thread.sleep(10000);

			manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in
			// Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 20000l);
			}
			Thread.sleep(5000);
			// verify max active limit reached
			System.out.println("Sending 1 more request");
			DME2RestfulHandler replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
			try {
				replyHandler.getResponse( 10000 );
			} catch(DME2Exception ex) {
				assert(ex.getMessage().contains("AFT-DME2-0703"));
			}

			// send another request as different partner
			String uriWithAnotherPartner = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + SOMEOTHER_PARTNER;
			System.out.println("Sending a request as another partner");
			replyHandler = sendARequest(manager, uriWithAnotherPartner);
			DME2RestfulHandler.ResponseInfo responseInfo = replyHandler.getResponse( 10000 );
			assertEquals("Was expecting a 200 OK", DME2Constants.DME2_RESPONSE_STATUS_200, responseInfo.getCode().intValue());

		} finally {
			try {
				manager.shutdown();
				Thread.sleep(2000);
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testQueueReceiverThrottlesAtDifferentPercentsForDifferentServices() throws Exception {
		String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter30/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter30/version=1.0.0/envContext=PROD";

		String serviceToRegister2 = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter31/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr2= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter31/version=1.0.0/envContext=PROD";

		try {
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
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

			// start service with 10 active queue receiver listeners and 20% = 2
			// active threads per partner config
			String serviceWith20PctThrottle = serviceToRegister + "?throttleFilterDisabled=false&throttlePctPerPartner=" + PARTNER_THROTTLE_20_PCT;
			Queue requestQueueFor20PctThrottleSvc = (Queue) context.lookup(serviceWith20PctThrottle);
			Thread.sleep(5000);
			TestThrottleMessageReceiver[] receiverThreadsFor20PctThrottleSvc = new TestThrottleMessageReceiver[TestConstants.listenerCount];
			for (int i = 0; i < MAX_RECEIVERS; i++) {
				receiverThreadsFor20PctThrottleSvc[i] = new TestThrottleMessageReceiver(connection, session, requestQueueFor20PctThrottleSvc, 20000);
				receiverThreadsFor20PctThrottleSvc[i].start();
			}

			// start service with 10 active queue receiver listeners and 50% = 5
			// active threads per partner config
			String serviceWith50PctThrottle = serviceToRegister2 + "?throttleFilterDisabled=false&throttlePctPerPartner=" + PARTNER_THROTTLE_50_PCT;
			Queue requestQueueFor50PctThrottleSvc = (Queue) context.lookup(serviceWith50PctThrottle);
			Thread.sleep(5000);
			TestThrottleMessageReceiver[] receiverThreadsFor50PctThrottleSvc = new TestThrottleMessageReceiver[TestConstants.listenerCount];
			for (int i = 0; i < MAX_RECEIVERS; i++) {
				receiverThreadsFor50PctThrottleSvc[i] = new TestThrottleMessageReceiver(connection, session, requestQueueFor50PctThrottleSvc, 20000);
				receiverThreadsFor50PctThrottleSvc[i].start();
			}

			Thread.sleep(5000);

			manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriForSvcWith20PctThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in
			// Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriForSvcWith20PctThrottle, 10000l);
			}
			Thread.sleep(5000);
			// verify max active limit reached for this partner
			DME2RestfulHandler replyHandler = sendARequest(manager, uriForSvcWith20PctThrottle, 0l);
			try {
				replyHandler.getResponse( 10000 );
			} catch(DME2Exception ex) {
				assert(ex.getMessage().contains("AFT-DME2-0703"));
			}

			String uriForSvcWith50PctThrottle = dme2SearchStr2 + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 1 that will wait in
			// Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_50_PCT_THROTTLE; i++) {
				sendARequest(manager, uriForSvcWith50PctThrottle, 10000l);
			}
			Thread.sleep(5000);
			// verify max active limit reached for this partner
			replyHandler = sendARequest(manager, uriForSvcWith50PctThrottle);
			try {
				replyHandler.getResponse( 10000 );
			} catch(DME2Exception ex) {
				assert(ex.getMessage().contains("AFT-DME2-0703"));
			}

		} finally {
			try {
				manager.shutdown();
				Thread.sleep(2000);
			} catch (Exception e) {
			}
		}
	}

	private DME2RestfulHandler sendARequest(DME2Manager manager, String uriStr, Long milliSecondsToWait) throws DME2Exception, URISyntaxException {
		DME2Client sender = new DME2Client(manager, new URI(uriStr), 30000);
		sender.setAllowAllHttpReturnCodes(true);
		HashMap<String, String> headers = new HashMap<String, String>();
		if (milliSecondsToWait != null) {
			sender.setPayload(milliSecondsToWait.toString());
		} else {
			sender.setPayload("0");
		}
		sender.setHeaders(headers);
		DME2RestfulHandler replyHandler = new DME2RestfulHandler(uriStr);
		sender.setReplyHandler(replyHandler);
		sender.send();
		return replyHandler;
	}

	private DME2RestfulHandler sendARequest(DME2Manager manager, String uriStr) throws DME2Exception, URISyntaxException {
		return sendARequest(manager, uriStr, null);
	}

}
