/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.test.server;

import java.io.File;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.att.aft.dme2.server.jetty.DME2ExecutorThreadPool;

/**
 * The Class JettyServer.
 */
public class JettyServer {

	/**
	 * The Class ServerStopper.
	 */
	class ServerStopper implements Runnable {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			stopServer();
		}
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws Exception
	 *             the exception
	 */
	public static void main(String[] args) throws Exception {
		String usage = "JettyServer -jettyport <port number> -webapppath <path to war file> -context <context path> -tempdir <directory where jetty should extract the files to>";
		JettyServer js = new JettyServer();
		for (int i = 0; i < args.length; i++) {
			if ("-jettyport".equals(args[i])) {
				js.setPortStr(args[i + 1]);
			} else if ("-serverid".equals(args[i])) {
				js.setServerId(args[i + 1]);
			} else if ("-killfile".equals(args[i])) {
				js.setKillFile(args[i + 1]);
			} else if (args[i].startsWith("-D")) {
				String[] prop = args[i].split("=");
				String propKey = prop[0].replaceFirst("-D", "");
				System.out.println("Setting system property " + propKey + "="
						+ prop[1]);
				System.setProperty(propKey, prop[1]);
			} else if ("-?".equals(args[i])) {
				System.out.println(usage);
				System.exit(0);
			}
		}
		js.init();
	}

	/** The kill file. */
	String killFile = null;

	/** The port str. */
	String portStr = null;

	/** The server. */
	Server server = null;

	/** The server id. */
	String serverId = null;

	/**
	 * Instantiates a new jetty server.
	 */
	public JettyServer() {

	}

	/**
	 * Gets the kill file.
	 * 
	 * @return the kill file
	 */
	public String getKillFile() {
		return killFile;
	}

	/**
	 * Gets the port str.
	 * 
	 * @return the port str
	 */
	public String getPortStr() {
		return portStr;
	}

	/**
	 * Gets the server id.
	 * 
	 * @return the server id
	 */
	public String getServerId() {
		return serverId;
	}

	/**
	 * Inits the.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void init() throws Exception {
		server = new Server();
		ThreadFactory tFactory = new ThreadFactory() {

			private int counter = 0;

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("CustomThreadFactory:::Server-" + counter++);
				return t;
			}

		};
		
		
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 10, 120000,
				TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true),
				tFactory);
		
		server.addBean(new DME2ExecutorThreadPool(threadPool));
		ServerConnector connector = new ServerConnector(server);//,null,null,bufferPool,4,4,http);
		connector.setPort(Integer.parseInt(portStr));
		
		SlowdownServlet servlet = new SlowdownServlet();

		ServletContextHandler root = new ServletContextHandler();
		root.setContextPath("/");
		server.setHandler(root);

		root.addServlet(new ServletHolder(servlet), "/");

		server.setConnectors(new Connector[] { connector });
		server.start();
		System.out.println("JettyServer Daemon started succesfully");
		// Runtime.getRuntime().addShutdownHook(new Thread(new
		// ServerStopper()));
		// server.join();

		File f = new File(killFile);
		while (!f.exists()) {
			System.out.println("Sleeping for 5000 and waiting for " + killFile);
			try {
				Thread.sleep(5000);
			} catch (Exception ex) {
			}
		}
		f.delete();

		// connector.close();
		// System.out.println("Connector closed.");
		server.stop();

		System.exit(0);
	}

	/**
	 * Sets the kill file.
	 * 
	 * @param killFile
	 *            the new kill file
	 */
	public void setKillFile(String killFile) {
		this.killFile = killFile;
	}

	/**
	 * Sets the port str.
	 * 
	 * @param portStr
	 *            the new port str
	 */
	public void setPortStr(String portStr) {
		this.portStr = portStr;
	}

	/**
	 * Sets the server id.
	 * 
	 * @param serverId
	 *            the new server id
	 */
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	/**
	 * Stop server.
	 */
	public void stopServer() {
		try {
			/**
			 * File f = new File(killFile); while(! f.exists()) {
			 * System.out.println("Sleeping for 5000 and waiting for " +
			 * killFile); try{Thread.sleep(5000);}catch(Exception ex){} }
			 * f.delete();
			 * 
			 * System.out.println("server=" + server + ", isRunning=" +
			 * server.isRunning());
			 **/
			if (server != null && server.isRunning()) {
				// System.out.println("stopping server...");
				try {
					server.stop();
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// System.out.println("stopped...");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
