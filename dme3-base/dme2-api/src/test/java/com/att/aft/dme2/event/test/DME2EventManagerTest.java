/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.event.test;

import java.lang.management.ManagementFactory;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.event.BaseMetricsPublisher;
import com.att.aft.dme2.event.DME2CancelRequestEventProcessor;
import com.att.aft.dme2.event.DME2Event;
import com.att.aft.dme2.event.DME2EventDispatcher;
import com.att.aft.dme2.event.DME2EventManager;
import com.att.aft.dme2.event.DME2FailoverEventProcessor;
import com.att.aft.dme2.event.DME2InitEventProcessor;
import com.att.aft.dme2.event.DME2ReplyEventProcessor;
import com.att.aft.dme2.event.DME2RequestEventProcessor;
import com.att.aft.dme2.event.DME2ServiceStatManager;
import com.att.aft.dme2.event.DME2ServiceStats;
import com.att.aft.dme2.event.DefaultMetricsCollector;
import com.att.aft.dme2.event.DefaultMetricsPublisher;
import com.att.aft.dme2.event.EventType;
import com.att.aft.dme2.event.MetricsPublisherFactory;
import com.att.aft.dme2.event.test.mbean.Test;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.server.test.RegistryGrmSetup;
import com.att.aft.dme2.server.test.TestConstants;
import com.att.aft.dme2.util.DME2Constants;

import junit.framework.Assert;
import junit.framework.TestCase;

@SuppressWarnings("deprecation")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DME2EventManagerTest extends TestCase{

	private static final Logger logger = LoggerFactory.getLogger(DME2EventManagerTest.class.getName());
	  private static final String DEFAULT_VERSION = "1.0.0";
	  private static final String DEFAULT_HOST = "TestHost";
	  private static final String DEFAULT_PATH = "/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
	  private static final int DEFAULT_PORT = 12345;
	  private static final String DEFAULT_SERVICE_NAME = "com.att.test.TestService-2";
	  private static final String DEFAULT_ROUTE_OFFER = "DEFAULT";
	  private static final double DEFAULT_LATITUDE = 1.11;
	  private static final double DEFAULT_LONGITUDE = -2.22;
	  private static final String DEFAULT_PROTOCOL = "http";
	  private static final String DEFAULT_ENV_CONTEXT = "LAB";
	  private static final String OBJECT_NAME = "com.att.dme2:type=Diagnostics";
	  private DME2Configuration config = null;
	  
	public DME2EventManagerTest(String name) {
		super(name);
	    try {
	    	Test mbean = new Test();
	    	config = new DME2Configuration("TestDME2Manager", RegistryGrmSetup.init());
	    	ObjectName objectName = new ObjectName(OBJECT_NAME);
	    	MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
	    	mbeanServer.registerMBean(new StandardMBean(mbean, null, false),objectName);
	    }catch(Exception ex) {
//	    	ex.printStackTrace();
//	    	System.out.println(ex);
	    }
	}

	@BeforeClass
	protected void setUp() throws Exception {
		super.setUp();
//	    System.setProperty( "dme2_api_config", DME2EventManagerTest.class.getResource( "/dme-api_defaultConfigs.properties" ).getFile());
	    System.setProperty("DME2.DEBUG", "true");
	    System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
	    System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
	    System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
	    System.setProperty("AFT_LATITUDE", "33.373900");
	    System.setProperty("AFT_LONGITUDE", "-86.798300");
	    System.setProperty("AFT_DME2_GRM_URLS", TestConstants.GRM_LWP_DEV_DIRECT_HTTP_URLS_TO_USE);
	    System.setProperty("DME2_QS_TIMER_INT","10");
	    System.setProperty("DME2_QS_MSGEXP_INT","5");
	    System.setProperty("AFT_DME2_EVENT_QUEUE_SIZE", "10000");
	    System.setProperty("AFT_DME2_LOG_REJECTED_EVENTS", "false");	   
	    System.setProperty("AFT_DME2_EVENT_PROCESSOR_THREADS","5");
		System.setProperty("lrmEnv", "LAB");
		String containerVersion="1.0.0"; 
		String containerRO="TESTRO"; 
		String containerEnv="DEV"; 
		String containerName="com.att.test.EventUnitTest"; 
		System.setProperty("lrmRName",containerName);
		System.setProperty("lrmRVer",containerVersion);
		System.setProperty("lrmRO",containerRO);
		System.setProperty("lrmEnv",containerEnv);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@SuppressWarnings("deprecation")
	public void test_aInitDME2EventManager() {
		logger.info(null, null, "inside test_aInitDME2EventManager");

		DME2EventManager manager = DME2EventManager.getInstance(config);
		DME2ServiceStatManager.getInstance(config).setDisableCleanup(true);
		Assert.assertNotNull(manager);
		Assert.assertNull(manager.getListeners("Test"));
		Assert.assertNotNull(manager.getDispatcher());
		Assert.assertEquals(manager.getDispatcher().getEventManager(), manager);
		Assert.assertEquals(true, DME2ServiceStatManager.getInstance(config).isDisableCleanup());		
	}

	public void test_aaInitEventProcessors() {
		logger.info(null, null, "inside test_aaInitEventProcessors Processor");

		DME2EventManager manager = DME2EventManager.getInstance(config);
		DME2InitEventProcessor processor2 = new DME2InitEventProcessor(config);
		manager.registerEventProcessor(EventType.INIT_EVENT.getName(), processor2);

		DME2Event event = new DME2Event();
		long tempTime = System.currentTimeMillis();
		event.setEventTime(tempTime);
		event.setQueueName("/service=com.att.test.TestService-4/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setMessageId("50000");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.INIT_EVENT);
		manager.postEvent(event);

		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("50000"));
		Assert.assertEquals(true, (DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getLastTouchedTime() >= tempTime));
	}

	
	@SuppressWarnings("deprecation")
	public void test_xPostEventWithEventDispatcherUp() throws Exception {
		logger.info(null, null, "inside test_xPostEventWithEventDispatcherUp");

		DME2EventManager manager = DME2EventManager.getInstance(config);
		DME2Event event = new DME2Event();
		event.setQueueName("com.att.aft.MetricsService");
		event.setElapsedTime(200l);
		event.setMessageId("1000");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(0, manager.getQueueSize());
		event = new DME2Event();
		event.setQueueName("com.att.aft.MetricsService");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("1001");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(0, manager.getQueueSize());
		event = new DME2Event();
		event.setQueueName("com.att.aft.MetricsService");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("1002");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);

		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(0, manager.getQueueSize());
		event = new DME2Event();
		event.setMessageId("1003");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setQueueName("com.att.aft.MetricsService");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("12");
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(0, manager.getQueueSize());
		event = new DME2Event();
		event.setQueueName("com.att.aft.MetricsService");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("1004");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);

		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(0, manager.getQueueSize());
	}

	public void test_bRegisterEventProcessor() {
		logger.info(null, null, "inside test_bRegisterEventProcessor");

		DME2EventManager manager = DME2EventManager.getInstance(config);
		DME2FailoverEventProcessor processor = new DME2FailoverEventProcessor(config);
		manager.registerEventProcessor(EventType.FAILOVER_EVENT.getName(), processor);
		Assert.assertNotNull(manager.getListeners(EventType.FAILOVER_EVENT.getName()));
		Assert.assertEquals(1, manager.getListeners(EventType.FAILOVER_EVENT.getName()).size());
		manager.unRegisterEventProcessor(EventType.FAILOVER_EVENT.getName(), processor);
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(0, manager.getListeners(EventType.FAILOVER_EVENT.getName()).size());
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void test_dRequestEventProcessors() {
		logger.info(null, null, "inside test_dRequestEvent Processor");
		DME2EventManager manager = DME2EventManager.getInstance(config);
		DME2RequestEventProcessor processor2 = new DME2RequestEventProcessor(config);
		if(manager.getListeners(EventType.REQUEST_EVENT.getName()) == null){
			manager.registerEventProcessor(EventType.REQUEST_EVENT.getName(), processor2);
		}
		DME2Event event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		//event.setQueueName("1com.att.aft.MetricsServic");
		event.setQueueName("/service=1com.att.aft.MetricsServic/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setElapsedTime(200l);
		event.setMessageId("20000");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		// introducing delay for eventprocessor to process
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(true, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("20000")); // messageid
																								// is
																								// unique
		Assert.assertEquals(1, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getRequestCount());

		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.aft.MetricsService/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setElapsedTime(200l);
		event.setMessageId("20001");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("20001"));
		Assert.assertEquals(0, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getRequestCount());
	}

	public void test_eResponseEventProcessors() {
		logger.info(null, null, "inside test_eResonseEvent Processor");
		DME2EventManager manager = DME2EventManager.getInstance(config);
        DME2ServiceStatManager statManager = DME2ServiceStatManager.getInstance(config); 
        Assert.assertNotNull(statManager);
        Assert.assertTrue(statManager instanceof DME2ServiceStatManager);
		DME2RequestEventProcessor processor = new DME2RequestEventProcessor(config);
		if(manager.getListeners(EventType.REQUEST_EVENT.getName()) == null){
			manager.registerEventProcessor(EventType.REQUEST_EVENT.getName(), processor);
		}
		DME2ReplyEventProcessor processor2 = new DME2ReplyEventProcessor(config);
		if(manager.getListeners(EventType.REPLY_EVENT.getName()) == null){
			manager.registerEventProcessor(EventType.REPLY_EVENT.getName(), processor2);
		}
		DME2Event event = null;
		
		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setMessageId("30000");
		event.setClientAddress("");
		//event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
				

		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setMessageId("30001");
		event.setClientAddress("");
		//event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		
		
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
				
		System.out.println("DME2ServiceStatManager.getInstance(config).getRequestmap() :" + DME2ServiceStatManager.getInstance(config).getRequestmap());

		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setElapsedTime(100l);
		event.setMessageId("30000");
		event.setClientAddress("");
		//event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REPLY_EVENT);
		manager.postEvent(event);
		
		try {
			Thread.currentThread().sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.test.TestService-2/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setElapsedTime(200l);
		event.setMessageId("30001");
		event.setClientAddress("");
		event.setReplyMsgSize(50l);
		//event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REPLY_EVENT);
		manager.postEvent(event);
		
		try {
			Thread.currentThread().sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("DME2ServiceStatManager.getInstance(config).getRequestmap() :" + DME2ServiceStatManager.getInstance(config).getRequestmap());

		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("30000"));
		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("30001"));
		Assert.assertEquals(300l, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getTotalElapsed());
		Assert.assertEquals(50l, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getLastReplyMsgSize());
		Assert.assertEquals(2, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getRequestCount());
	}

	public void test_gFailoverEventProcessors() {
		logger.info(null, null, "inside test_gFailoverEventProcessors Processor");

		DME2EventManager manager = DME2EventManager.getInstance(config);
		DME2FailoverEventProcessor processor2 = new DME2FailoverEventProcessor(config);
		manager.registerEventProcessor(EventType.FAILOVER_EVENT.getName(), processor2);

		DME2Event event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.test.TestService-3/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setMessageId("40000");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);

		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.test.TestService-3/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setElapsedTime(100l);
		event.setMessageId("40000");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.FAILOVER_EVENT);
		manager.postEvent(event);

		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Assert.assertEquals(true, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("40000"));
		Assert.assertEquals(1, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getFailoverCount());
	}

	public void test_tDiagnostics() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName(OBJECT_NAME);
        MBeanInfo infos = server.getMBeanInfo(objectName);
		logger.info(null, "test_tDiagnostics", "inside test_tDiagnostics");
		String[] diag = DME2ServiceStatManager.getInstance(config).diagnostics();
		logger.info(null, "test_tDiagnostics", "inside test_tDiagnostics : diag : {}", diag);
		System.out.println("diag - "+ diag);
		System.out.println("diag.length - "+ diag.length);
		for(int i=0; i<diag.length; i++){
			System.out.println("diag.[i] - "+ diag[i]);
		}
		Assert.assertNotNull(diag);
		Assert.assertEquals(false, (diag.length == 0));
		Object stat = server.invoke(objectName, "diagnostics", null, null);
		System.out.println("stat : "+ stat);
        Assert.assertEquals("[Stat", stat.toString().substring(0, 5));
        Assert.assertTrue(stat.toString().contains("[Stat"));
	}
	
	public void test_zZPostEventWithEventDispatcherShutDown() throws Exception {
		logger.info(null, null, "inside test_zPostEventWithEventDispatcherShutDown");
		DME2EventManager manager = DME2EventManager.getInstance(config);
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		DME2EventDispatcher.setStopThreads(true);
		try {
			Thread.currentThread().sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		DME2Event event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("test");
		event.setElapsedTime(200l);
		event.setMessageId("1000000");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		Assert.assertEquals(1, manager.getQueueSize());
		event = new DME2Event();
		event.setQueueName("test");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("1000001");
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		Assert.assertEquals(2, manager.getQueueSize());
		event = new DME2Event();
		event.setQueueName("test");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("1000002");
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		Assert.assertEquals(3, manager.getQueueSize());
		event = new DME2Event();
		event.setQueueName("test");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("1000003");
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		Assert.assertEquals(4, manager.getQueueSize());
		event = new DME2Event();
		event.setQueueName("test");
		event.setEventTime(System.currentTimeMillis());
		event.setElapsedTime(200l);
		event.setMessageId("1000004");
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		Assert.assertEquals(5, manager.getQueueSize());
	}

	
	public void test_yCleanupExpiredMessages() {
		logger.info(null, null, "inside test_aaaCleanupExpiredMessages Processor");
		DME2EventManager manager = DME2EventManager.getInstance(config);
		System.out.println("manager.getListeners(EventType.REQUEST_EVENT.getName()).size() :" + manager.getListeners(EventType.REQUEST_EVENT.getName()));
		System.out.println("manager.getListeners(EventType.REPLY_EVENT.getName()).size() :" + manager.getListeners(EventType.REPLY_EVENT.getName()));		
		DME2ServiceStatManager.getInstance(config).setDisableCleanup(false);
		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).isDisableCleanup());
		DME2RequestEventProcessor processor2 = new DME2RequestEventProcessor(config);
		if(manager.getListeners(EventType.REQUEST_EVENT.getName()) == null){
				manager.registerEventProcessor(EventType.REQUEST_EVENT.getName(), processor2);
		}
		DME2Event event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		//event.setQueueName("1com.att.aft.MetricsServic");
		event.setQueueName("/service=com.att.aft.TestService-10/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setElapsedTime(200l);
		event.setMessageId("70000");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(true, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("70000")); // messageid
																								// is
																								// unique
		Assert.assertEquals(1, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getRequestCount());
		System.out.println("DME2ServiceStatManager.getInstance(config).getCheckInterval() ********* : " + DME2ServiceStatManager.getInstance(config).getCheckInterval());
		System.out.println("DME2ServiceStatManager.getInstance(config).getExpiryInterval() ******** : " +DME2ServiceStatManager.getInstance(config).getExpiryInterval());		
		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName("/service=com.att.aft.TestService-10/version=1.0.0/envContext=LAB/routeOffer=DEFAULT");
		event.setElapsedTime(200l);
		event.setMessageId("70001");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(true, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("70001"));
		Assert.assertEquals(2, DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName()).getRequestCount());
		try {
			Thread.currentThread().sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("70000"));
		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("70001"));
		Assert.assertEquals(true, (DME2ServiceStatManager.getInstance(config).getExpiredCount() >= 2));
		
	}


	public void test_bbServiceStatManager() throws Exception {
		logger.info(null, null, "inside test_bbServiceStatManager");
		DME2EventManager manager = DME2EventManager.getInstance(config);
        DME2ServiceStatManager statManager = DME2ServiceStatManager.getInstance(config); 
        Assert.assertNotNull(statManager);
        Assert.assertTrue(statManager instanceof DME2ServiceStatManager);
		DME2Event event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		String service = "/service=com.att.aft.TestService-33/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
		event.setQueueName(service);
		event.setElapsedTime(200l);
		event.setMessageId("120001");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		try {
			Thread.currentThread().sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		DME2ServiceStats stats = statManager.getServiceStats(service);
        Assert.assertNotNull(stats);
        Assert.assertNotNull(stats.getContainerHost());
        Assert.assertNotNull(stats.getContainerPlat());
        Assert.assertNotNull(stats.getContainerVersion());     
        Assert.assertNotNull(stats.getContainerEnv());
        Assert.assertNotNull(stats.getContainerName());
        Assert.assertNotNull(stats.getContainerRO());     
        Assert.assertNotNull(stats.getContainerVersion());
        Assert.assertNotNull(stats.getStats());                
        Assert.assertTrue(stats.getStats().length > 0); 
        Assert.assertEquals("Statistics for",stats.getStats()[0]);        
        Assert.assertEquals(16,stats.getStats().length);                
        Assert.assertEquals(service,stats.getQueueName());                       
        logger.info(null, null, "exiting test_bbServiceStatManager");
	}
	
	public void test_pMetricsPublisherFactory() throws Exception {
		logger.info(null, null, "inside test_pMetricsPublisherFactory");
		BaseMetricsPublisher publisher = MetricsPublisherFactory.getBaseMetricsPublisherHandlerInstance(new DME2Configuration());
		Assert.assertNotNull(publisher);
		System.out.println("publisher : " + publisher);
        DME2ServiceStatManager statManager = DME2ServiceStatManager.getInstance(config); 
		DME2EventManager manager = DME2EventManager.getInstance(config);
		DME2Event event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		String service = "/service=com.att.aft.TestService-34/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
		event.setQueueName(service);
		event.setElapsedTime(200l);
		event.setMessageId("110001");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);
        Assert.assertTrue(publisher instanceof DefaultMetricsPublisher);
        DefaultMetricsCollector collector = DefaultMetricsPublisher.getMetricsCollector(null, statManager.getServiceStats(service));
//        Assert.assertTrue(collector instanceof MetricsCollector);        
        try{
        	publisher.publishEvent(event, statManager.getServiceStats(service));
           	Assert.assertEquals(true, true);       	
        }catch(Exception ex){
        	Assert.assertEquals(true, false);
        }
        logger.info(null, null, "exiting test_pMetricsPublisherFactory");
	}

	public void test_mCancelRequestEventProcessor() throws Exception {
		logger.info(null, null, "inside test_mCancelRequestEventProcessor");

		DME2EventManager manager = DME2EventManager.getInstance(config);
		Assert.assertNotNull(manager);		
		
		DME2CancelRequestEventProcessor processor2 = new DME2CancelRequestEventProcessor(config);
		manager.registerEventProcessor(EventType.CANCEL_REQUEST_EVENT.getName(), processor2);
		Assert.assertNotNull(processor2);

		DME2ServiceStatManager statManager = DME2ServiceStatManager.getInstance(config); 
		Assert.assertNotNull(statManager);

		DME2Event event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		String service = "/service=com.att.aft.CancelTestService-1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
		event.setQueueName(service);
		event.setElapsedTime(200l);
		event.setMessageId("610001");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.REQUEST_EVENT);
		manager.postEvent(event);

		try {
			Thread.currentThread().sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Assert.assertEquals(true, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("610001"));

		event = new DME2Event();
		event.setEventTime(System.currentTimeMillis());
		service = "/service=com.att.aft.CancelTestService-1/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
		event.setQueueName(service);
		event.setElapsedTime(200l);
		event.setMessageId("610001");
		event.setClientAddress("");
		event.setRole("Client");
		event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL));
		event.setRole(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE));
		event.setType(EventType.CANCEL_REQUEST_EVENT);
		manager.postEvent(event);

		try {
			Thread.currentThread().sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Assert.assertEquals(false, DME2ServiceStatManager.getInstance(config).getRequestmap().containsKey("610001"));
        logger.info(null, null, "exiting test_mCancelRequestEventProcessor");
	}
		
}

