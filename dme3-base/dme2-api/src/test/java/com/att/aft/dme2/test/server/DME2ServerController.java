/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.server;

import java.io.File;
import java.util.Properties;

import javax.servlet.Servlet;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2Server;
import com.att.aft.dme2.api.DME2ServerProperties;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.server.test.EchoResponseServlet;
import com.att.aft.dme2.server.test.EchoServlet;
import com.att.aft.dme2.server.test.FailoverServlet;
import com.att.aft.dme2.server.test.PropsLoader;
import com.att.aft.dme2.server.test.RegistryFsSetup;
import com.att.aft.dme2.util.DME2Constants;

public class DME2ServerController {
  private static final Logger logger = LoggerFactory.getLogger( DME2ServerController.class );
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
      } else if ("-throttleConfig".equals(args[i])) {
        controller.setDme2ThrottleConfigFile(args[i + 1]);
			} else if ( "-throttleDisabled".equals( args[i] )) {
				controller.dme2ThrottleDisabled = args[i+1];
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
  private String dme2ThrottleConfigFile = null;
	private String dme2ThrottleDisabled = null;

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
        System.setProperty( "SCLD_PLATFORM", props.getProperty( "platform" ));
			}
			if(this.getServerPort() != null && this.getServerPort().contains("-")) {
				// range is present
				System.setProperty("AFT_DME2_PORT_RANGE",this.getServerPort());
			} else if(this.getServerPort() != null){
				System.setProperty("AFT_DME2_PORT",this.getServerPort());
			}
			if(this.getPlatform() != null) {
				System.setProperty("platform", this.getPlatform());
        System.setProperty( "SCLD_PLATFORM", this.getPlatform() );
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
      if(this.getDme2ThrottleConfigFile() != null) {
        			System.setProperty("AFT_DME2_THROTTLE_FILTER_CONFIG_FILE",this.getDme2ThrottleConfigFile());
        			}
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
      System.out.println( " **** AFT_DME2_MAX_POOL_SIZE IS " + config.getProperty( "AFT_DME2_MAX_POOL_SIZE" ) + " (should be " + System.getProperty( "AFT_DME2_MAX_POOL_SIZE" ) + ") ****" );
      System.out.println( " **** SCLD_PLATFORM IS " + config.getProperty( "SCLD_PLATFORM"  ) + " ( should be " + System.getProperty( "SCLD_PLATFORM" )  + ")");
			System.out.println( " **** platform IS " + config.getProperty( "platform" ));
			server = new DME2Server(config);			
			DME2ServerProperties serverProperties = new DME2ServerProperties(config);		
			serverProperties.setReuseAddress(false);

			String managerName;
			Properties properties = new Properties( );

			if (registryType.equals("FS")) {
				props = RegistryFsSetup.init();
				properties = props;
				managerName = "DME2Server-FS";
			} else {
				properties = RegistryFsSetup.init();
				managerName = "DME2Server-GRM";
			}

			if ( dme2ThrottleDisabled != null ) {
				properties.setProperty( DME2Constants.AFT_DME2_DISABLE_THROTTLE_FILTER, dme2ThrottleDisabled );
			}

			manager = new DME2Manager( managerName, new DME2Configuration( managerName, properties ));
			System.out.println( " **** Manager AFT_DME2_MAX_POOL_SIZE IS " + manager.getConfig().getProperty( "AFT_DME2_MAX_POOL_SIZE" ));
			System.out.println( " **** Manager SCLD_PLATFORM IS " + manager.getConfig().getProperty( "SCLD_PLATFORM"  ) );
			System.out.println( " **** Manager platform IS " + manager.getConfig().getProperty( "platform" ));
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
			if (servletClass.equals("TestMetricsServlet")) {
				s = new TestMetricsServlet(serviceName,
						serverId);
			}
			manager.bindServiceListener(serviceName, s);

			System.out.println("Server started successfully.");

			File f = new File(killFile);
			System.out.println("Kill file = " + f.getCanonicalPath());
			while (!f.exists()) {
				try {
					System.out.println("Kill file = " + f.getCanonicalPath() + ";exists=" + f.exists());
					Thread.sleep(5000);
				} catch (Exception ex) {
          logger.error( null, "startServer", "Exception", ex );
        }
			}

      logger.debug( null, "startServer", "Kill file {} exists now: {}", killFile, f.exists() );
			f.delete();

			logger.debug( null, "startServer", "Stopping server...");
			manager.shutdown();
      DME2Manager.getDefaultInstance().shutdown();
		} catch (DME2Exception e) {
			e.printStackTrace();
			//throw new RuntimeException(e);
		} catch (Exception ex) {
			ex.printStackTrace();
			//throw new RuntimeException(ex);
		} finally {
      // We shouldn't have to do this... we need to find the rogue daemon threads
      System.exit(0);
    }
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getPlatform() {
		return platform;
	}

  public String getDme2ThrottleConfigFile() {
    return this.dme2ThrottleConfigFile;
	}

	public void setDme2ThrottleConfigFile(String dme2ThrottleConfigFile) {
		this.dme2ThrottleConfigFile = dme2ThrottleConfigFile;
	}
}
