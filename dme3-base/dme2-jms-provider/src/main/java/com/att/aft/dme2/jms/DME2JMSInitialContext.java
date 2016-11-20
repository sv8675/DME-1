/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/***
 * This class provides an initial context implementation that can be used to
 * resolve HTTP JMS specific objects dynamically without requiring an external
 * JNDI provider.
 */
public class DME2JMSInitialContext extends InitialContext {
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSInitialContext.class.getName());

	private transient DME2Manager manager;
	private transient DME2JMSManager jmsManager;
	private Map<String, DME2JMSManager> instances = new HashMap<String, DME2JMSManager>();


	public DME2JMSInitialContext(Hashtable<?, ?> environment) throws NamingException {
		Properties props = new Properties();
		props.putAll(environment);
		String dme2ManagerName = props.getProperty("AFT_DME2_MANAGER_NAME");
		
		if (dme2ManagerName == null) {
			dme2ManagerName = props.getProperty("DME2_MANAGER_NAME");
		}		
		
		DME2JMSManager instance = instances.get(dme2ManagerName);		
		if(instance == null) {

			if (dme2ManagerName == null) {
				try {
					this.jmsManager = DME2JMSManager.getDefaultInstance(props);
				} catch (JMSException e) {
					throw new NamingException("[" + e.getErrorCode()
							+ "] - DME2JMSInitialContext failed to load default DME2JMSManager - " + e.getMessage());
				} catch (Exception ex) {
					throw new NamingException("[" + ex.getMessage()
							+ "] - DME2JMSInitialContext failed to load default DME2JMSManager - " + ex.getMessage());
				}
			} else {
				try {
					List<String> defaultConfigs = new ArrayList<String>();
					defaultConfigs.add(JMSConstants.JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME);
					defaultConfigs.add(JMSConstants.DME_API_DEFAULT_CONFIG_FILE_NAME);
//					defaultConfigs.add(JMSConstants.METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME);
					
					DME2Configuration config = new DME2Configuration(dme2ManagerName, defaultConfigs, null, props);
					manager = new DME2Manager(dme2ManagerName, config);
					this.jmsManager = new DME2JMSManager(manager);
				} catch (DME2Exception e) {
					throw new NamingException(
							"[" + e.getErrorCode() + "] - DME2JMSInitialContext failed to load custom DME2Manager ["
									+ dme2ManagerName + "] - " + e.getMessage());
				}
			}
			
			this.manager = jmsManager.getDME2Manager();
			instances.put(manager.getName(), jmsManager);
			logger.debug(null, "DME2JMSInitialContext", JMSLogMessage.QUEUE_CREATED, manager.getName());
			
		}
	}

	/**
	 * Lookup specific to HTTP JMS Objects. Any attempt to lookup any other
	 * object will fail
	 */
	@Override
	public Object lookup(String path) throws NamingException {
		logger.debug(null, "lookup", LogMessage.METHOD_ENTER);
		logger.debug(null, "lookup", "Looking up [{}]", path);

		// if the container asked for the "root" context, return this object so
		// subsequent lookups will come back here.
		if (path == null || path.length() == 0) {
			return this;
		}

		try {
			if (path.startsWith("qcf://dme2")) {
				return jmsManager.getQCF(path);
			} else if (path.startsWith("xaqcf://dme2")) {
				return jmsManager.getXAQCF(path);
			} else if (path.startsWith("QCFDME2")) {
				return jmsManager.getQCF(path);
			} else if (path.startsWith("tcf://dummy")) {
				return jmsManager.getDummyTCF(path);
			} else {
				return jmsManager.getQueue(path);
			}
		} catch (URISyntaxException e) {
			NamingException n = new NamingException(e.getMessage());
			n.initCause(e);
			throw n;
		} catch (JMSException e) {
			NamingException n = new NamingException(e.getMessage());
			n.initCause(e);
			throw n;
		} finally {
			logger.debug(null, "lookup", LogMessage.METHOD_EXIT);
		}
	}
}
