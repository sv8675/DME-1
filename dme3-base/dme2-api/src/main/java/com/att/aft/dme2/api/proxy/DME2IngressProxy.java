/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.proxy;

import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.quick.QuickServer;
import com.att.aft.dme2.api.quick.QuickServletConfig;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2IngressProxy {
  private static final Logger logger = LoggerFactory.getLogger( DME2IngressProxy.class );

	public static void main(String[] args) throws DME2Exception, InstantiationException, IllegalAccessException, ClassNotFoundException, ServletException {
		String skipExit = "false";
		
		try {
			String port = null;
			String env = null;
			
			for(int i=0; i<args.length; i++) {
				if(args[i].equals("-p"))
					port = args[i+1];
			}
			
			
			// java QuickServer -f config.properties 
			Properties props = new Properties();
			skipExit = getProperty(props, "AFT_DME2_PROXY_SKIPEXIT", "false");
			String clzName = getProperty(props, "AFT_DME2_QUICKSERVER_SERVLET", "com.att.aft.dme2.api.proxy.DME2IngressProxyServlet");
			if(port == null) {
				port = getProperty(props,"AFT_DME2_PORT","21210");
			}
			
			String realm = getProperty(props, "AFT_DME2_QUICKSTART_REALM", null);
			
			
			String aftEnv = getProperty(props,"AFT_ENVIRONMENT", null);
			if(aftEnv == null) {
				logger.error( null, "main", "AFT_ENVIRONMENT property is required");
				System.exit(1);
			}
			
			if(aftEnv.equals("AFTUAT")) {
				env = "UAT";
			}
			else if(aftEnv.equals("AFTPRD")) {
				env = "PROD";
			}
			else {
        logger.error( null, "main", "AFT_ENVIRONMENT value {} is not valid", aftEnv);
				System.exit(1);
			}
			
			String service = getProperty(props, "AFT_DME2_QUICKSTART_SERVICE", "service=com.att.aft.DME2IngressProxy/version=1.0.0/envContext="+env+"/routeOffer=DEFAULT");
      logger.debug( null, "main", " service = {}", service);
			String[] allowedRoles = null;
			String loginMethod = null;
			if (realm != null) {
				//String securityRealm, String[] allowedRoles, String loginMethod
				String allowedRolesStr = getProperty(props, "AFT_DME2_QUICKSTART_ALLOWEDROLES", null);
				if (allowedRolesStr == null) {
          logger.error( null, "main", "AFT_DME2_QUICKSTART_ALLOWEDROLES is required when AFT_DME2_QUICKSTART_REALM is set");
					System.exit(1);
				}
				allowedRoles = allowedRolesStr.split(",");
				loginMethod = getProperty(props, "AFT_DME2_QUICKSTART_LOGINMETHOD", "BASIC");
			}
			
			if (service != null) {
				props.put("AFT_DME_QUICKSTART_SERVICE", service);
			} else {
        logger.error( null, "main", "AFT_DME_QUICKSTART_SERVICE must be set");
				System.exit(1);
			}		
			
			if(port == null ) {
        logger.error( null, "main", "Usage: DME2IngressProxy -p <port>");
				System.exit(1);
			}
			
			props.put("AFT_DME2_PORT",port);
			DME2Configuration config2 = new DME2Configuration("DME2IngressProxyManager"+port, props);
			DME2Manager manager = new DME2Manager("DME2IngressProxyManager"+port, config2, props);
			
			//DME2Manager manager = new DME2Manager("DME2IngressProxyManager", props);
			@SuppressWarnings("unchecked")
			Class<Servlet> clz = (Class<Servlet>)QuickServer.class.getClassLoader().loadClass(clzName);
      logger.debug( null, "main", " clzName******************************************************** = {}", clzName);
			Servlet listenerServlet = clz.getDeclaredConstructor(DME2Manager.class).newInstance(manager);
			ServletConfig config = new QuickServletConfig(service, null, props);
			listenerServlet.init(config);
			DME2ServiceHolder holder = new DME2ServiceHolder();
			holder.setContext("/");
			holder.setManager(manager);
			holder.setSecurityRealm(realm);
			holder.setAllowedRoles(allowedRoles);
			holder.setLoginMethod(loginMethod);
			holder.setServiceURI(service);
			holder.setServlet(listenerServlet);
			manager.bindService(holder);
			manager.start();
			
			while(true) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					if (skipExit.equals("false")) System.exit(0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (skipExit.equals("false")) System.exit(1);
		} finally {
			if (skipExit.equals("false")) System.exit(0);
		}
	}
	
	private static final String getProperty(Properties props, String key, String defaultValue) {
		String value = props.getProperty(key);
		if (value == null) {
			value = System.getProperty(key, defaultValue);
		}
		return value;
	}
}
