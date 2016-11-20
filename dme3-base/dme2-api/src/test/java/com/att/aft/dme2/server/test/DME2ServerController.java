/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.io.File;
import java.util.Properties;

import javax.servlet.Servlet;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2Server;
import com.att.aft.dme2.api.DME2ServerProperties;
import com.att.aft.dme2.config.DME2Configuration;

public class DME2ServerController {
	public static void main(String[] args) throws Exception {
		DME2ServerController controller = new DME2ServerController();

		for (int i = 0; i < args.length; i++) {
			if ("-serverHost".equals(args[i])) {
				controller.setServerHost(args[i + 1]);
			} else if ("-serverPort".equals(args[i])) {
				controller.setServerPort(args[i + 1]);
			} else if ("-registryType".equals(args[i])) {
				controller.setRegistryType(args[i + 1]);
			} else if ("-servletClass".equalsIgnoreCase(args[i])) {
				controller.setServletClass(args[i + 1]);
			} else if ("-serviceName".equals(args[i])) {
				controller.setServiceName(args[i + 1]);
			} else if ("-serviceCity".equals(args[i])) {
				controller.setServiceCity(args[i + 1]);
			} else if ("-serverid".equals(args[i])) {
				controller.setServerId(args[i + 1]);
			} else if ("-killfile".equals(args[i])) {
				controller.setKillFile(args[i + 1]);
			} else if ("-platform".equals(args[i])) {
				controller.setPlatform(args[i + 1]);
			}
		}
		controller.init();
	}

	//String basePropsFile = "src/test/etc/dme2-api.properties";
	String basePropsFile = "dme2-api.properties";

	private String killFile = null;
	private DME2Manager manager = null;
	Properties props = PropsLoader.getProperties(basePropsFile);
	private String registryType = null;
	private DME2Server server = null;
	private String serverHost = null;
	private String serverId = null;
	private String serverPort = null;
	private String platform = null;

	private String serviceCity = null;
	private String serviceName = null;

	private String servletClass = null;

	public DME2ServerController() {

	}

	public String getKillFile() {
		return this.killFile;
	}

	public DME2Manager getManager() {
		return manager;
	}

	public String getRegistryType() {
		return registryType;
	}

	public DME2Server getServer() {
		return server;
	}

	public String getServerHost() {
		return serverHost;
	}

	public String getServerId() {
		return this.serverId;
	}

	public String getServerPort() {
		return serverPort;
	}

	public String getServiceCity() {
		return serviceCity;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getServletClass() {
		return servletClass;
	}

	protected void init() {
		try {
			if (props.containsKey("AFT_DME2_SVCCONFIG_DIR")) {
				System.setProperty("AFT_DME2_SVCCONFIG_DIR",
						props.getProperty("AFT_DME2_SVCCONFIG_DIR"));
			}
			if (props.containsKey("platform")) {
				System.setProperty("platform",
						props.getProperty("platform"));
			}
			if(this.getServerPort() != null && this.getServerPort().contains("-")) {
				// range is present
				System.setProperty("AFT_DME2_PORT_RANGE",this.getServerPort());
			} else if(this.getServerPort() != null){
				System.setProperty("AFT_DME2_PORT",this.getServerPort());
			}
			if(this.getPlatform() != null) {
				System.setProperty("platform", this.getPlatform());
			}
			if (serviceCity.equals("BHAM")) {
				String bhamAFTFile = props.getProperty("AFT_BHAM");
				Properties bhamProps = PropsLoader.getProperties(bhamAFTFile);
				PropsLoader.printProperties(bhamProps);
				PropsLoader.setProperties(bhamProps);
			} else if (serviceCity.equals("CHAR")) {
				String charAFTFile = props.getProperty("AFT_CHAR");
				Properties charProps = PropsLoader.getProperties(charAFTFile);
				PropsLoader.printProperties(charProps);
				PropsLoader.setProperties(charProps);
			} else if (serviceCity.equalsIgnoreCase("JACKSON")) {
				String jackAFTFile = props.getProperty("AFT_JACKSON");
				Properties jackProps = PropsLoader.getProperties(jackAFTFile);
				PropsLoader.printProperties(jackProps);
				PropsLoader.setProperties(jackProps);
			}

			System.out.println("11111111111111111111111111111111111111111");
			startServer();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setKillFile(String file) {
		this.killFile = file;
	}

	public void setManager(DME2Manager manager) {
		this.manager = manager;
	}

	public void setRegistryType(String registryType) {
		this.registryType = registryType;
	}

	public void setServer(DME2Server server) {
		this.server = server;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}

	public void setServiceCity(String serviceCity) {
		this.serviceCity = serviceCity;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setServletClass(String servletClass) {
		this.servletClass = servletClass;
	}

	public void startServer() {
		try {
			DME2Configuration config = new DME2Configuration();
			server = new DME2Server(config);
			System.out.println( " **** GRM VARS : platform " + config.getProperty( "platform" ) + " SCLD_PLATFORM: " + config.getProperty( "SCLD_PLATFORM" ) + " AFT_ENVIRONMENT: " + config.getProperty( "AFT_ENVIRONMENT" ) );
			DME2ServerProperties serverProperties = new DME2ServerProperties(config);		
			serverProperties.setReuseAddress(false);
			
			if (registryType.equals("FS")) {
				props = RegistryFsSetup.init();
				manager = new DME2Manager("DME2Server-FS", new DME2Configuration("DME2Server-FS", props));
			} else {
				manager = new DME2Manager("DME2Server-GRM", new DME2Configuration("DME2Server-GRM", RegistryFsSetup.init()));
			}
			Servlet s = null;
			if (servletClass.equals("EchoServlet")) {
				s = new EchoServlet(serviceName,
						serverId);
			}
			if (servletClass.equals("FailoverServlet")) {
				s = new FailoverServlet(serviceName,
						serverId);
			}
			if (servletClass.equals("EchoResponseServlet")) {
				s = new EchoResponseServlet(serviceName,
						serverId);
			}
			/*if (servletClass.equals("TestMetricsServlet")) {
				s = new TestMetricsServlet(serviceName,
						serverId);
			}*/
			System.out.println("22222222222222222222222222222222");
			manager.bindServiceListener(serviceName, s);

			System.out.println("Server started successfully.");

			File f = new File(killFile);
			System.out.println("Kill file = " + f.getCanonicalPath());
			while (!f.exists()) {
				try {
					System.out.println("Kill file = " + f.getCanonicalPath() + ";exists=" + f.exists());
					Thread.sleep(5000);
				} catch (Exception ex) {
				}
			}
			
			f.delete();

			System.out.println("Stopping server...");
			manager.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
    }
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getPlatform() {
		return platform;
	}
}
