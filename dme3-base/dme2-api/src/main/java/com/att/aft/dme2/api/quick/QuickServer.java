/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.quick;

import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;

/**
 * A simple server where a provider only has to implement a JMS MessageListener.
 */
public class QuickServer {
	public static void main(String[] args) throws DME2Exception, InstantiationException, IllegalAccessException, ClassNotFoundException, ServletException {
		try {
			// java QuickServer -f config.properties 
			Properties props = new Properties();
			String clzName = getProperty(props, "AFT_DME2_QUICKSERVER_SERVLET", "com.att.aft.dme2.server.util.DME2NullServlet");
			String service = getProperty(props, "AFT_DME2_QUICKSTART_SERVICE", null);
			String realm = getProperty(props, "AFT_DME2_QUICKSTART_REALM", null);
			
			String[] allowedRoles = null;
			String loginMethod = null;
			if (realm != null) {
				//String securityRealm, String[] allowedRoles, String loginMethod
				String allowedRolesStr = getProperty(props, "AFT_DME2_QUICKSTART_ALLOWEDROLES", null);
				if (allowedRolesStr == null) {
					System.err.println("AFT_DME2_QUICKSTART_ALLOWEDROLES is required when AFT_DME2_QUICKSTART_REALM is set");
					System.exit(1);
				}
				allowedRoles = allowedRolesStr.split(",");
				
				loginMethod = getProperty(props, "AFT_DME2_QUICKSTART_LOGINMETHOD", "BASIC");
			}
			
			if (service != null) {
				props.put("AFT_DME2_QUICKSTART_SERVICE", service);
				props.put("AFT_DME2_SERVICE", service);
			} else {
				System.err.println("AFT_DME2_QUICKSTART_SERVICE must be set");
				System.exit(1);
			}		
			DME2Configuration config1 = new DME2Configuration("QuickServerManager");
			DME2Manager manager = new DME2Manager("QuickServerManager", config1);
			
			@SuppressWarnings("unchecked")
			Class<Servlet> clz = (Class<Servlet>)QuickServer.class.getClassLoader().loadClass(clzName); 
			Servlet listenerServlet = clz.newInstance();
			ServletConfig config = new QuickServletConfig(service, null, props);
			listenerServlet.init(config);
			if (realm != null) {
				manager.bindServiceListener(service, listenerServlet, realm, allowedRoles, loginMethod);
			} else {
				manager.bindServiceListener(service, listenerServlet);
			}
			
			while(true) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					System.exit(0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			System.exit(0);
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
