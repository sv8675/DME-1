/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms;

import java.io.File;
import java.util.Hashtable;
import java.util.Properties;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

import com.att.aft.dme2.jms.DME2JMSLocalQueue;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;
import com.att.aft.dme2.test.jms.util.TestThrottleMessageListener;

public class TestDME2ThrottleFilterJMSServer {
	private InitialContext context;
	private QueueConnectionFactory factory;
	private QueueConnection connection;
	private QueueSession session;
	private Queue requestQueue;
	private String serviceToRegister;
	private String city = null;
	private String killFile = null;
	private Boolean disableThrottleFilter;
	private Double throttlePctPerPartner;

	public TestDME2ThrottleFilterJMSServer() throws Exception {
	}
	public void init() throws Exception {
		start();

		File f = new File(killFile);
		while (!f.exists()) {
			try {
				Thread.sleep(5000);
			} catch (Exception ex) {
			}
			System.out.println("Sleeping for 5000 and waiting for kill file " + getKillFile());
		}

//		stop();
//		f.delete();
//		System.out.println("TestJMSHeaderReplyJMSServer destroyed.");
	}

	public void start() throws Exception {
		if (city.equals("BHAM"))
			Locations.BHAM.set();
		else if (city.equals("CHAR"))
			Locations.CHAR.set();
		else if (city.equals("JACK"))
			Locations.JACK.set();

		Properties props = RegistryFsSetup.init();
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			table.put((String) key, props.get(key));
		}
		table.put("java.naming.factory.initial", TestConstants.jndiClass);
		table.put("java.naming.provider.url", TestConstants.jndiUrl);
		context = new InitialContext(table);
		factory = (QueueConnectionFactory) context.lookup(TestConstants.clientConn);
		connection = factory.createQueueConnection();
		session = connection.createQueueSession(true, 0);
		String servicetoregister = this.serviceToRegister;
		if (getThrottlePctPerPartner() != null && getDisableThrottleFilter() != null) {
			servicetoregister = servicetoregister + "?throttleFilterDisabled=" + getDisableThrottleFilter() + "&throttlePctPerPartner=" + getThrottlePctPerPartner();
		} else if (getThrottlePctPerPartner() != null) {
			servicetoregister = servicetoregister + "?throttlePctPerPartner=" + getThrottlePctPerPartner();
		} else if (getDisableThrottleFilter() != null) {
			servicetoregister = servicetoregister + "?throttleFilterDisabled=" + getDisableThrottleFilter();
		}
		requestQueue = (Queue) context.lookup(servicetoregister);
		TestThrottleMessageListener[] listeners = new TestThrottleMessageListener[TestConstants.listenerCount];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = new TestThrottleMessageListener(connection, session, requestQueue);
			listeners[i].start();
		}
		System.out.println("TestDME2ThrottleFilterJMSServer.init(): requestQueue.getListeners().size()=" + ((DME2JMSLocalQueue) requestQueue).getListeners().size());

		System.out.println("TestDME2ThrottleFilterJMSServer started successfully...");

		File f = new File(killFile);
		while (!f.exists()) {
			try {
				Thread.sleep(5000);
			} catch (Exception ex) {
			}
			System.out.println("Sleeping for 5000 and waiting for kill file " + getKillFile());
		}

		f.delete();
		System.out.println("TestDME2ThrottleFilterJMSServer destroyed.");
	}

	public static void main(String[] args) throws Exception {
		TestDME2ThrottleFilterJMSServer server = new TestDME2ThrottleFilterJMSServer();

		// String city = null;
		if (args.length == 0)
			server.setCity("BHAM");
		else {
			for (int i = 0; i < args.length; i++) {
				if ("-city".equals(args[i]))
					server.setCity(args[i + 1]);
				else if ("-killfile".equals(args[i]))
					server.setKillFile(args[i + 1]);
				else if ("-disableThrottleFilter".equals(args[i]))
					server.setDisableThrottleFilter(Boolean.valueOf(args[i + 1]));
				else if ("-throttlePctPerPartner".equals(args[i]))
					server.setThrottlePctPerPartner(Double.valueOf(args[i + 1]));
				else if ("-serviceToRegister".equals(args[i]))
					server.setServiceToRegister(args[i + 1]);
			}
		}

		server.init();
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCity() {
		return this.city;
	}

	public void setKillFile(String killFile) {
		this.killFile = killFile;
	}

	public String getKillFile() {
		return this.killFile;
	}

	public Boolean getDisableThrottleFilter() {
		return disableThrottleFilter;
	}

	public void setDisableThrottleFilter(Boolean disableThrottleFilter) {
		this.disableThrottleFilter = disableThrottleFilter;
	}

	public Double getThrottlePctPerPartner() {
		return throttlePctPerPartner;
	}

	public void setThrottlePctPerPartner(Double throttlePctPerPartner) {
		this.throttlePctPerPartner = throttlePctPerPartner;
	}

	public String getServiceToRegister() {
		return serviceToRegister;
	}

	public void setServiceToRegister(String serviceToRegister) {
		this.serviceToRegister = serviceToRegister;
	}

}