/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.server;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

import org.apache.commons.io.FileUtils;

import com.att.aft.dme2.test.jms.util.Copier;
import com.att.aft.dme2.test.jms.util.LocalQueueMessageListener;
import com.att.aft.dme2.test.jms.util.Locations;
import com.att.aft.dme2.test.jms.util.RegistryFsSetup;
import com.att.aft.dme2.test.jms.util.TestConstants;

public class TestReceiveJMSServer {
	private InitialContext context;
	private QueueConnectionFactory factory;
	private QueueConnection connection;
	private QueueSession session;
	private Queue requestQueue;

	private String city = null;
	private String killFile = null;

	public TestReceiveJMSServer() throws Exception {
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

	public void init() throws Exception {
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
		requestQueue = (Queue) context.lookup(TestConstants.serviceRcvToRegister);
		Copier.copyRoutingFile("service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE", "MyServiceV1");
		LocalQueueMessageListener[] listeners = new LocalQueueMessageListener[TestConstants.listenerCount];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = new LocalQueueMessageListener(connection, session, requestQueue);
			listeners[i].start();
		}

		System.out.println("TestJMSServer started successfully...");

		File f = new File(killFile);
		while (!f.exists()) {
			try {
				Thread.sleep(5000);
			} catch (Exception ex) {
			}
			System.out.println("Sleeping for 5000 and waiting for kill file " + getKillFile());
		}

		f.delete();
		System.out.println("TestJMSServer destroyed.");
	}

	public static void main(String[] args) throws Exception {
		TestJMSServer server = new TestJMSServer();

		// String city = null;
		if (args.length == 0)
			server.setCity("BHAM");
		else {
			for (int i = 0; i < args.length; i++) {
				if ("-city".equals(args[i]))
					server.setCity(args[i + 1]);
				else if ("-killfile".equals(args[i]))
					server.setKillFile(args[i + 1]);
			}
		}

		String currDir = new File(".").getAbsolutePath();
		String configDir = currDir.substring(0, currDir.length() - 1) + "/src/test/etc/svc_config";
		System.setProperty("AFT_DME2_SVCCONFIG_DIR", "file:///" + configDir);
		System.setProperty("DME2_EP_REGISTRY_CLASS", "DME2FS");
		String destDir = currDir.substring(0, currDir.length() - 1) + "/dme2-fs-registry";
		configCopy(configDir, destDir, "service=MyService/version=1.0.0/envContext=PROD/routeInfo.xml");
		Copier.copyRoutingFile("service=MyService/version=1.0.0/envContext=PROD/routeOffer=BAU_SE", "MyServiceV1");
		server.init();
	}

	private static void configCopy(String configDir, String destDir, String path) throws IOException {
		File srcFile = new File(configDir + File.separator + path);
		File destFile = new File(destDir + File.separator + path);
		FileUtils.copyFile(srcFile, destFile);
	}
}
