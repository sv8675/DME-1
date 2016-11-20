/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.quick;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.att.aft.dme2.jms.DME2JMSInitialContextFactory;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/**
 * A simple server where a provider only has to implement a JMS MessageListener.
 */
@SuppressWarnings({ "PMD.AvoidCatchingThrowable", "PMD.SystemPrintln" })
public class QuickServer extends Thread {
	private static String uSAGE1 = "java " + QuickServer.class.getCanonicalName()
			+ " -d <destination> -t <thread-count> -l <message-listener-class>";
	private static String uSAGE2 = "where: ";
	private static String uSAGE3 = "    -d <destination>    : A DME2 destination address [http://DME2LOCAL/service=../version=../envContext=../routeOffer=..";
	private static String uSAGE4 = "    -t <thread-count>   : The number of listener threads to instantiate";
	private static String uSAGE5 = "    -l <listener-class> : A javax.jms.MessageListener implementation whose onMessage() will be invoked on each message receipt.";
	private static String uSAGE6 = "                          This class MUST have either a default constructor OR a constructor with the following signature:";
	private static String uSAGE7 = "                          (javax.jms.Connection,javax.jms.Destination,javax.jms.Session)";
	private static String uSAGE_ALL = uSAGE1 + '\n' + uSAGE2 + '\n' + uSAGE3 + '\n' + uSAGE4 + '\n' + uSAGE5 + '\n'
			+ uSAGE6 + '\n' + uSAGE7;

	private QueueConnection connection;
	private Queue destination;
	private Constructor<MessageListener> constructor;
	private QueueSession session;
	private QueueReceiver receiver;
	private boolean hasArgs = false;
	private boolean started = false;
	private Throwable cause = null;
	private int threadCount = 0;
	private static final int CONSTANT_100000 = 100000;
	private static final Logger logger = LoggerFactory.getLogger(QuickServer.class.getName());

	public QuickServer(String destinationString, int threadCount, Constructor<MessageListener> constructor,
			boolean hasArgs) throws NamingException, JMSException {
		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("java.naming.factory.initial", DME2JMSInitialContextFactory.class.getName());
		table.put("java.naming.provider.url", "qcf://dme2");

		System.out.println("Getting InitialContext");
		InitialContext context = new InitialContext(table);

		System.out.println("Looking up QueueConnectionFactory");
		QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup("qcf://dme2");

		System.out.println("Looking up request Queue");
		destination = (Queue) context.lookup(destinationString);

		System.out.println("Creating QueueConnection");
		connection = qcf.createQueueConnection();
		this.constructor = constructor;
		this.hasArgs = hasArgs;
		this.threadCount = threadCount;
	}

	public synchronized void start() {
		for (int i = 0; i < threadCount; i++) {
			try {
				session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
				receiver = session.createReceiver(destination);
				MessageListener listener = null;
				if (hasArgs) {
					listener = constructor.newInstance(connection, destination, session);
				} else {
					listener = constructor.newInstance(new Object[0]);
				}
				receiver.setMessageListener(new QuickListener(connection, destination, session, listener));
			} catch (Throwable e) {
				started = false;
				cause = e;
			}
		}
		started = true;
	}

	public boolean isStarted() {
		return started;
	}

	public Throwable getCause() {
		return cause;
	}

	public static void main(String[] args) throws Exception {
		/**
		 * -DAFT_LATITUDE=33.6 -DAFT_LONGITUDE=-86.6 -DAFT_ENVIRONMENT=AFTUAT
		 * -Dplatform=SANDBOX-LAB com.att.aft.dme2.jms.samples.TestServer
		 * -jndiClass com.att.aft.dme2.jms.DME2JMSInitialContextFactory -jndiUrl
		 * qcf://dme2 -serverConn qcf://dme2 -serverDest
		 * http://DME2LOCAL/service=com.att.aft.Demo318Service/version=1.0.0/
		 * envContext=DEV/routeOffer=APPLE_SE -serverThreads 5
		 * 
		 */

		// usage: java -DAFT_LATITUDE=.. -DAFT_LONGITUDE=.. -DAFT_ENVIRONMENT=..
		// com.att.aft.dme2.jms.DME2JMSSimpleServer -d
		// http://DME2LOCAL/service=com.att.aft.Demo318Service/version=1.0.0/envContext=DEV/routeOffer=APPLE_SE
		// -t 5 -l com.att.test.MessageListener | -f <file>
		// http://DME2LOCAL/service=com.att.aft.Demo318Service/version=1.0.0/envContext=DEV/routeOffer=APPLE_SE|com.att.test.MessageListener|5
		String destination = null;
		String threadCountStr = null;
		String listenerClassName = null;
		String filename = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-d")) {
				destination = args[i + 1];
			} else if (args[i].equals("-t")) {
				threadCountStr = args[i + 1];
			} else if (args[i].equals("-l")) {
				listenerClassName = args[i + 1];
			} else if (args[i].equals("-f")) {
				filename = args[i + 1];
			} else if (args[i].equals("-?")) {
				displayUsage();
				System.exit(0);
			}
		}

		List<QuickServer> servers = new ArrayList<QuickServer>();

		if (filename == null) {
			servers.add(launchQuickServer(destination, listenerClassName, threadCountStr));
		} else {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String row;
			while ((row = reader.readLine()) != null) {
				String[] toks = row.split("\\|");
				if (toks.length != DME2Constants.DME2_CONSTANT_THREE) {
					System.err.println("WARNING: Invalid row found: " + row);
				} else {
					servers.add(launchQuickServer(toks[0], toks[1], toks[2]));
				}
			}
		}

		boolean failure = false;
		for (QuickServer server : servers) {
			server.join();
			if (!server.isStarted()) {
				System.err.println("A QuickServer startup failed");
				if (server.getCause() != null) {
					server.getCause().printStackTrace();
				}
				failure = true;
			}
		}

		if (failure) {
			System.err.println("QuickServer Startup Failed - " + new Date());
		} else {
			System.out.println("QuickServer Startup Successful - " + new Date());
			while (true) {
				Thread.sleep(CONSTANT_100000);
			}
		}
	}

	private static QuickServer launchQuickServer(String destination, String listenerClassName, String threadCountStr)
			throws Exception {
		if (destination == null || threadCountStr == null || listenerClassName == null) {
			System.err.println("ERROR: Invalid and/or missing arguments");
			displayUsage();
			System.exit(1);
		}

		Integer threadCount = null;
		try {
			threadCount = Integer.parseInt(threadCountStr);
		} catch (NumberFormatException e) {
			System.err.println("ERROR: Thread count argument must be an integer, not " + threadCountStr);
			System.exit(2);
		}

		if (threadCount < 1) {
			System.err.println("ERROR: Thread count must be 1 or higher");
			System.exit(DME2Constants.DME2_CONSTANT_THREE);
		}

		@SuppressWarnings("unchecked")
		Class<MessageListener> clz = (Class<MessageListener>) QuickServer.class.getClassLoader()
				.loadClass(listenerClassName);
		Constructor<MessageListener> constructor;
		boolean hasArgs = false;
		try {
			constructor = clz.getConstructor(Connection.class, Destination.class, Session.class);
			hasArgs = true;
		} catch (Exception e) {
			constructor = clz.getConstructor(new Class[0]);
			hasArgs = false;
		}

		QuickServer server = new QuickServer(destination, threadCount, constructor, hasArgs);
		server.start();
		return server;
	}

	private static void displayUsage() {
		System.out.println(uSAGE_ALL);
	}
}
