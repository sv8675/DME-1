/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
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

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.handler.DME2RestfulHandler;
import com.att.aft.dme2.test.jms.util.JMSBaseTestCase;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.ServerLauncher;
import com.att.aft.dme2.test.jms.util.TestConstants;
import com.att.aft.dme2.test.jms.util.TestThrottleMessageListener;
import com.att.aft.dme2.util.DME2Constants;

@net.jcip.annotations.NotThreadSafe

public class TestDME2ThrottleFilter extends JMSBaseTestCase {

	private static final Long WAIT_5_SECONDS = 5000l;
	private static final int MAX_LISTENERS = TestConstants.listenerCount;
	private static final double PARTNER_THROTTLE_50_PCT = 50.0;
	private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_50_PCT_THROTTLE = (int) Math.ceil(MAX_LISTENERS * (PARTNER_THROTTLE_50_PCT/100.0));
	private static final double PARTNER_THROTTLE_20_PCT = 20.0;
	private static final int MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE = (int) Math.ceil(MAX_LISTENERS * (PARTNER_THROTTLE_20_PCT / 100.0));

	private ServerLauncher launcher = null;
	private DME2Manager manager;

	@After
	public void testTearDown() throws Exception {
		super.tearDown();
		if ( manager != null && manager.getServer().isRunning() ) {
			manager.shutdown();
		}
	}
	@Test
	public void testThrottlesAPartnerAsJMSServer() throws Exception {
		try {
			String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter21/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
			String dme2SearchStr = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter21/version=1.0.0/envContext=PROD";
			DME2RestfulHandler replyHandler = null;
			DME2RestfulHandler.ResponseInfo responseInfo = null;

			// start service with 10 active listeners and 20% = 2 active threads
			// per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
			//props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");
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
			TestThrottleMessageListener[] listeners = new TestThrottleMessageListener[TestConstants.listenerCount];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listeners[i] = new TestThrottleMessageListener(connection, session, requestQueue);
				listeners[i].start();
			}
			Thread.sleep(10000);
			
			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 10000L);
			}
			Thread.sleep(5000);
			// verify max active limit reached
			System.out.println("Sending 1 more request");
            try {
                replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
	            responseInfo = replyHandler.getResponse( 50000 );
	            fail("after maxing out threads, should have tried failover and thrown an exception, but return with status code: " + responseInfo.getCode());
            } catch (DME2Exception e) {
	            assertEquals( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("******** Got 429 as expected and a failover happened ");
            }
		} finally {
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
			} catch (Exception e) {
			}
		}
	}


	@Test
	public void testAllowsAPartnerAfterMaxActiveRequestsBelowLimit() throws Exception {
		try {
			String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter22/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
			String dme2SearchStr = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter22/version=1.0.0/envContext=PROD";
			DME2RestfulHandler replyHandler = null;
			DME2RestfulHandler.ResponseInfo responseInfo = null;

			// start service with 10 active listeners and 20% = 2 active threads
			// per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
			//props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");			
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
			TestThrottleMessageListener[] listeners = new TestThrottleMessageListener[TestConstants.listenerCount];
			System.out.println("Adding "+MAX_LISTENERS+" listeners");
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listeners[i] = new TestThrottleMessageListener(connection, session, requestQueue);
				listeners[i].start();
			}
			Thread.sleep(10000);
			System.out.println("Sleeping "+WAIT_5_SECONDS+" for listeners to start");
			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			System.out.println("Sending "+MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE+" requests");
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 10000l);
			}
            Thread.sleep(5000);
			// verify max active limit reached
			System.out.println("Sending 1 more request");
			try {
	            replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
	            responseInfo = replyHandler.getResponse( 9000 );
	            fail("after maxing out threads, should have tried failover and thrown an exception, but return with status code: " + responseInfo.getCode());
	        } catch (DME2Exception e) {
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("******** Got 429 as expected and a failover happened ");
			}

			// wait for 5 seconds for all requests to get processed
			System.out.println("Waiting for 15 secs for all requests to get processed");
			Thread.sleep(15000);

			// send another one and it should go through successfully
			System.out.println("Sending 1 request to get 200");
			replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
			responseInfo = replyHandler.getResponse( 10000l );
			assertEquals("Was expecting a 200 OK!", DME2Constants.DME2_RESPONSE_STATUS_200, responseInfo.getCode().intValue());

		} finally {
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testThrottleOnePartnerUponMaxActiveReqButAllowsOthersBelowLimit() throws Exception {
		try {
			String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter23/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
			String dme2SearchStr = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter23/version=1.0.0/envContext=PROD";
			DME2RestfulHandler replyHandler = null;
			DME2RestfulHandler.ResponseInfo responseInfo = null;

			// start service with 10 active listeners and 20% = 2 active threads
			// per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
			//props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");			
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
			TestThrottleMessageListener[] listeners = new TestThrottleMessageListener[TestConstants.listenerCount];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listeners[i] = new TestThrottleMessageListener(connection, session, requestQueue);
				listeners[i].start();
			}
			Thread.sleep(WAIT_5_SECONDS);

			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 30000l);
			}
			Thread.sleep(10000);
			// verify max active limit reached for this partner
			try {
				replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
	            responseInfo = replyHandler.getResponse( 8000 );
	            fail("after maxing out threads, should have tried failover and thrown an exception, but return with status code: " + responseInfo.getCode());
	        } catch (DME2Exception e) {
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("******** Got 429 as expected and a failover happened ");
			}

			// send request as another partner
			String uriWithAnotherPartner = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=ANOTHERPARTNER";
			replyHandler = sendARequest(manager, uriWithAnotherPartner);
			responseInfo = replyHandler.getResponse( 15000 );
			assertEquals("Was expecting a 200 OK!", DME2Constants.DME2_RESPONSE_STATUS_200, responseInfo.getCode().intValue());

		} finally {
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
			} catch (Exception e) {
			}
		}
	}

	@Test
	@Ignore
	public void testDisableThrottleFilterWithJvmArgs() throws Exception {

		String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter24/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter24/version=1.0.0/envContext=PROD";

		try {
			System.setProperty("throttleFilterDisabled", "true");
			System.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
			System.setProperty("DME2.DEBUG", "true");
			String containerName = "com.att.test.DME2ThrottleFilterClient";
			String containerVersion = "1.0.0";
			String containerRO = "TESTRO";
			String containerEnv = "PROD";
//			String containerPlat = "SANDBOX-LAB";
			String containerPlat = "SANDBOX-DEV";
			String containerHost = null;
			String containerPid = "1234";
			String containerPartner = "TEST";
			System.setProperty("platform", containerPlat);
			System.setProperty("DME2.DEBUG", "true");
			System.setProperty("lrmRName", containerName);
			System.setProperty("lrmRVer", containerVersion);
			System.setProperty("lrmRO", containerRO);
			System.setProperty("lrmEnv", containerEnv);
			// System.setProperty("platform","NON-PROD");
			try {
				containerHost = InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {

			}
			System.setProperty("lrmHost", containerHost);
			System.setProperty("Pid", containerPid);
			System.setProperty("partner", containerPartner);
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_DISABLE_THROTTLE_FILTER", "true");

			// start service with throttleFilterDisabled as true
			launcher = new ServerLauncher(null, "-city", "BHAM", "-serviceToRegister", serviceToRegister, "-throttleFilterDisabled", "true");
			launcher.launchTestDME2ThrottleFilterJMSServer();
			Thread.sleep(10000);
			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 10 seconds

			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 10000L);
			}
			Thread.sleep(5000);
			// send another request and it should go through successfully
			DME2RestfulHandler replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
			DME2RestfulHandler.ResponseInfo responseInfo = replyHandler.getResponse( 3000 );
			assertEquals("Was expecting a 200 OK!", DME2Constants.DME2_RESPONSE_STATUS_200, responseInfo.getCode().intValue());

		} finally {
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testSamePartnerWithMultipleServicesWithDiffThrottlePercentsIsThrottled() throws Exception {
		String serviceToRegister1 = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter26/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr1= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter26/version=1.0.0/envContext=PROD";

		String serviceToRegister2 = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter27/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr2= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter27/version=1.0.0/envContext=PROD";

		DME2RestfulHandler replyHandler = null;
		DME2RestfulHandler.ResponseInfo responseInfo = null;
		TestThrottleMessageListener[] listenersFor20PctSvc = null, listenersFor50PctSvc = null;

		try {
			// start service with 20% of 10 = 2 active threads per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
//			props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");			
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			//start service with 20% throttle per partner
			QueueSession sessionSvc1 = connection.createQueueSession(true, 0);
			String serviceWith20PctThrottle = serviceToRegister1 + "?throttleFilterDisabled=false&throttlePctPerPartner=" + PARTNER_THROTTLE_20_PCT;
			Queue requestQueueFor20PctSvc = (Queue) context.lookup(serviceWith20PctThrottle);
			listenersFor20PctSvc = new TestThrottleMessageListener[MAX_LISTENERS];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listenersFor20PctSvc[i] = new TestThrottleMessageListener(connection, sessionSvc1, requestQueueFor20PctSvc);
				listenersFor20PctSvc[i].start();
			}
			Thread.sleep(WAIT_5_SECONDS);

			// start service with 50% of 10 = 5 active threads per partner config
			QueueSession sessionSvc2 = connection.createQueueSession(true, 0);
			String serviceWith50PctThrottle = serviceToRegister2 + "?throttleFilterDisabled=false&throttlePctPerPartner=" + PARTNER_THROTTLE_50_PCT;
			Queue requestQueueFor50PctSvc = (Queue) context.lookup(serviceWith50PctThrottle);
			listenersFor50PctSvc = new TestThrottleMessageListener[MAX_LISTENERS];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listenersFor50PctSvc[i] = new TestThrottleMessageListener(connection, sessionSvc2, requestQueueFor50PctSvc);
				listenersFor50PctSvc[i].start();
			}

			Thread.sleep(WAIT_5_SECONDS);

			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriForSvcWith20PctThrottle = dme2SearchStr1 + "/dataContext=" + TestConstants.dataContext + "/partner=PTE";
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriForSvcWith20PctThrottle, 30000L);
			}
			Thread.sleep(5000);
			// verify max active limit reached for this partner
			try {
				replyHandler = sendARequest(manager, uriForSvcWith20PctThrottle);
	            responseInfo = replyHandler.getResponse( 2000 );
	            fail("after maxing out threads, should have tried failover and thrown an exception, but return with status code: " + responseInfo.getCode());
	        } catch (DME2Exception e) {
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("********Got AFT-DME2-0703 in exception during failover as expected as partner has reached throttle limit**********");
			}

			String uriForSvcWith50PctThrottle = dme2SearchStr2 + "/dataContext=" + TestConstants.dataContext + "/partner=PTE";
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 5 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_50_PCT_THROTTLE; i++) {
				sendARequest(manager, uriForSvcWith50PctThrottle, 30000l);
			}
			Thread.sleep(5000);
			// verify max active limit reached for this partner
			try {
	            replyHandler = sendARequest(manager, uriForSvcWith50PctThrottle);
			    replyHandler.getResponse( 2000 );
	            fail("after maxing out threads should have tried failover and thrown an exception and not reach this line");
	        } catch (DME2Exception e) {
				e.printStackTrace();
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("******** Got 429 as expected and a failover happened ");
			}

		} finally {
			try {
				if ( listenersFor20PctSvc != null ) {
					synchronized ( listenersFor20PctSvc ) {
						for ( int i = 0; i < listenersFor20PctSvc.length; i++ ) {
							listenersFor20PctSvc[i].getReceiver().close();
						}
					}
				}
				if ( listenersFor50PctSvc != null ) {
					synchronized ( listenersFor50PctSvc ) {
						for ( int i = 0; i < listenersFor50PctSvc.length; i++ ) {
							listenersFor50PctSvc[i].getReceiver().close();
						}
					}
				}
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			try {
				if ( launcher != null ) {
					launcher.destroy();

					Thread.sleep( WAIT_5_SECONDS );
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if ( manager != null ) {
					manager.shutdown();

					Thread.sleep( 10000 );
					if ( manager != null && manager.getServer() != null && manager.getServer().isRunning() ) {
						manager.getServer().stop();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testThrottlesAPartnerAsJMSServerWithContinuationTimeOut() throws Exception {
		String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter25/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
		String dme2SearchStr = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter25/version=1.0.0/envContext=PROD";

		try {
			// start service with 10 active listeners and 20% = 2 active threads
			// per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
//			props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");
			//Set endpoint read timeout of 10 ms for client call
			props.put("AFT_DME2_EP_READ_TIMEOUT_MS", "10000");
			props.setProperty("AFT_DME2_EP_READ_TIMEOUT_MS", "10000");
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
			TestThrottleMessageListener[] listeners = new TestThrottleMessageListener[TestConstants.listenerCount];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listeners[i] = new TestThrottleMessageListener(connection, session, requestQueue);
				listeners[i].start();
			}
			Thread.sleep(10000);

			DME2Manager manager = new DME2Manager("RegistryFsSetup", props);
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 10000l);
			}
			Thread.sleep(12000);
			// verify max active limit reached
			System.out.println("Sending 1 more request");
			DME2RestfulHandler replyHandler = sendARequest(manager, uriWithPartnerToThrottle, 0l);
			/*This request should have taken it above the throttle limit but as continuation timeout occurred due to AFT_DME2_EP_READ_TIMEOUT_MS values, 
			 the counter is decremented. So allow this request*/
			DME2RestfulHandler.ResponseInfo responseInfo = replyHandler.getResponse( 10000 );
			assertEquals("Was expecting a 200 ok!", DME2Constants.DME2_RESPONSE_STATUS_200, responseInfo.getCode().intValue());

		} finally {
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
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
		/*try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		sender.send();
		return replyHandler;
	}

	private DME2RestfulHandler sendARequest(DME2Manager manager, String uriStr) throws DME2Exception, URISyntaxException {
		return sendARequest(manager, uriStr, null);
	}

	@After
	public void tearDown() throws Exception {
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

  @Test
  public void testThrottlesAPartnerAsJMSServerViaPartnerConfig() throws Exception {
		try {
			String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter32/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
			String dme2SearchStr = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter32/version=1.0.0/envContext=PROD";
			DME2RestfulHandler replyHandler = null;
			DME2RestfulHandler.ResponseInfo responseInfo = null;

			// start service with 10 active listeners and 20% = 2 active threads
			// per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
//			props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");			
			System.setProperty("AFT_DME2_THROTTLE_FILTER_CONFIG_FILE", "src/test/etc/dme2-throttle-config.properties");

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
			String servicetoregister = serviceToRegister + "?throttleFilterDisabled=false";
			Queue requestQueue = (Queue) context.lookup(servicetoregister);
			TestThrottleMessageListener[] listeners = new TestThrottleMessageListener[TestConstants.listenerCount];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listeners[i] = new TestThrottleMessageListener(connection, session, requestQueue);
				listeners[i].start();
			}
			Thread.sleep(10000);

			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 10000L);
			}
			Thread.sleep(5000);
			// verify max active limit reached
			System.out.println("Sending 1 more request");
            try {
                replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
	            responseInfo = replyHandler.getResponse( 2000 );
	            fail("after maxing out threads, should have tried failover and thrown an exception, but return with status code: " + responseInfo.getCode());
            } catch (DME2Exception e) {
							e.printStackTrace();
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("******** Got 429 as expected and a failover happened ");
            }
		} finally {
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testThrottleOnePartnerUponMaxActiveReqButAllowsOthersBelowLimitViaPartnerConfigFile() throws Exception {
		try {
			String serviceToRegister = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter33/version=1.0.0/envContext=PROD/routeOffer=BAU_SE";
			String dme2SearchStr = "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter33/version=1.0.0/envContext=PROD";
			DME2RestfulHandler replyHandler = null;
			DME2RestfulHandler.ResponseInfo responseInfo = null;

			// start service with 10 active listeners and 20% = 2 active threads
			// per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");
//			props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");			
			System.setProperty("AFT_DME2_THROTTLE_FILTER_CONFIG_FILE", "src/test/etc/dme2-throttle-config.properties");

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
			String servicetoregister = serviceToRegister + "?throttleFilterDisabled=false";
			Queue requestQueue = (Queue) context.lookup(servicetoregister);
			TestThrottleMessageListener[] listeners = new TestThrottleMessageListener[TestConstants.listenerCount];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listeners[i] = new TestThrottleMessageListener(connection, session, requestQueue);
				listeners[i].start();
			}
			Thread.sleep(WAIT_5_SECONDS);

			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriWithPartnerToThrottle = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=" + TestConstants.partner;
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriWithPartnerToThrottle, 30000l);
			}
			Thread.sleep(10000);
			// verify max active limit reached for this partner
			try {
				replyHandler = sendARequest(manager, uriWithPartnerToThrottle);
	            responseInfo = replyHandler.getResponse( 8000 );
	            fail("after maxing out threads, should have tried failover and thrown an exception, but return with status code: " + responseInfo.getCode());
	        } catch (DME2Exception e) {
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("******** Got 429 as expected and a failover happened ");
			}

			// send request as another partner
			String uriWithAnotherPartner = dme2SearchStr + "/dataContext=" + TestConstants.dataContext + "/partner=ANOTHERPARTNER";
			replyHandler = sendARequest(manager, uriWithAnotherPartner);
			responseInfo = replyHandler.getResponse( 15000 );
			assertEquals("Was expecting a 200 OK!", DME2Constants.DME2_RESPONSE_STATUS_200, responseInfo.getCode().intValue());

		} finally {
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testSamePartnerWithMultipleServicesWithDiffThrottlePercentsIsThrottledViaPartnerConfig() throws Exception {
		String serviceToRegister1 = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter26/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttleFilterDisabled=false";
		String dme2SearchStr1= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter26/version=1.0.0/envContext=PROD";

		String serviceToRegister2 = "http://DME2LOCAL/service=com.att.aft.dme2.api.TestDME2ThrottleFilter27/version=1.0.0/envContext=PROD/routeOffer=BAU_SE?throttleFilterDisabled=false";
		String dme2SearchStr2= "http://DME2SEARCH/service=com.att.aft.dme2.api.TestDME2ThrottleFilter27/version=1.0.0/envContext=PROD";
		DME2RestfulHandler replyHandler = null;
		DME2RestfulHandler.ResponseInfo responseInfo = null;
		TestThrottleMessageListener[] listenersFor20PctSvc = null, listenersFor50PctSvc = null;

		try {
			System.setProperty("AFT_DME2_THROTTLE_FILTER_CONFIG_FILE", "src/test/etc/dme2-throttle-config.properties");

			// start service with 20% of 10 = 2 active threads per partner config
			Locations.BHAM.set();
			Properties props = RegistryFsSetup.init();
			props.setProperty("AFT_DME2_PUBLISH_METRICS", "false");

//			props.setProperty("AFT_DME2_THROTTLE_PCT_PER_PARTNER", "20");
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			for (Object key : props.keySet()) {
				table.put((String) key, props.get(key));
			}
			table.put("java.naming.factory.initial", TestConstants.jndiClass);
			table.put("java.naming.provider.url", TestConstants.jndiUrl);
			InitialContext context = new InitialContext(table);
			QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
			QueueConnection connection = factory.createQueueConnection();
			//start service with 20% throttle per partner
			QueueSession sessionSvc1 = connection.createQueueSession(true, 0);
			String serviceWith20PctThrottle = serviceToRegister1;
			Queue requestQueueFor20PctSvc = (Queue) context.lookup(serviceWith20PctThrottle);
			listenersFor20PctSvc = new TestThrottleMessageListener[MAX_LISTENERS];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listenersFor20PctSvc[i] = new TestThrottleMessageListener(connection, sessionSvc1, requestQueueFor20PctSvc);
				listenersFor20PctSvc[i].start();
			}
			Thread.sleep(WAIT_5_SECONDS);

			// start service with 50% of 10 = 5 active threads per partner config
			QueueSession sessionSvc2 = connection.createQueueSession(true, 0);
			String serviceWith50PctThrottle = serviceToRegister2;
			Queue requestQueueFor50PctSvc = (Queue) context.lookup(serviceWith50PctThrottle);
			listenersFor50PctSvc = new TestThrottleMessageListener[MAX_LISTENERS];
			for (int i = 0; i < MAX_LISTENERS; i++) {
				listenersFor50PctSvc[i] = new TestThrottleMessageListener(connection, sessionSvc2, requestQueueFor50PctSvc);
				listenersFor50PctSvc[i].start();
			}

			Thread.sleep(WAIT_5_SECONDS);

			DME2Manager manager = new DME2Manager("RegistryFsSetup", RegistryFsSetup.init());
			String uriForSvcWith20PctThrottle = dme2SearchStr1 + "/dataContext=" + TestConstants.dataContext + "/partner=PTE";
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 2 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_20_PCT_THROTTLE; i++) {
				sendARequest(manager, uriForSvcWith20PctThrottle, 30000L);
			}
			Thread.sleep(5000);
			// verify max active limit reached for this partner
			try {
				replyHandler = sendARequest(manager, uriForSvcWith20PctThrottle);
	            responseInfo = replyHandler.getResponse(2000);
	            fail("after maxing out threads, should have tried failover and thrown an exception, but return with status code: " + responseInfo.getCode());
	        } catch (DME2Exception e) {
				e.printStackTrace();
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("********Got AFT-DME2-0703 in exception during failover as expected as partner has reached throttle limit**********");
			}

			String uriForSvcWith50PctThrottle = dme2SearchStr2 + "/dataContext=" + TestConstants.dataContext + "/partner=PTE";
			// send MAX_ACTIVE_THREADS_PER_PARTNER = 5 that will wait in Listeners onMessage for 15 seconds
			for (int i = 0; i < MAX_ACTIVE_THREADS_PER_PARTNER_WITH_50_PCT_THROTTLE; i++) {
				sendARequest(manager, uriForSvcWith50PctThrottle, 30000l);
			}
			Thread.sleep(5000);
			// verify max active limit reached for this partner
			try {
	            replyHandler = sendARequest(manager, uriForSvcWith50PctThrottle);
			    replyHandler.getResponse(2000);
	            fail("after maxing out threads should have tried failover and thrown an exception and not reach this line");
	        } catch (DME2Exception e) {
	            assertEquals(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, e.getErrorCode());
	            assertTrue(e.getMessage().contains("onResponseCompleteStatus=429"));
	            System.out.println("******** Got 429 as expected and a failover happened ");
			}

		} finally {
			try {
				if ( listenersFor20PctSvc != null ) {
					synchronized ( listenersFor20PctSvc ) {
						for ( int i = 0; i < listenersFor20PctSvc.length; i++ ) {
							listenersFor20PctSvc[i].getReceiver().close();
						}
					}
				}
				if ( listenersFor50PctSvc != null ) {
					synchronized ( listenersFor50PctSvc ) {
						for ( int i = 0; i < listenersFor50PctSvc.length; i++ ) {
							listenersFor50PctSvc[i].getReceiver().close();
						}
					}
				}
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			try {
				launcher.destroy();
				Thread.sleep(WAIT_5_SECONDS);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				manager.shutdown();
				Thread.sleep(10000);
				if ( manager != null && manager.getServer() != null && manager.getServer().isRunning() ) {
					manager.getServer().stop();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
