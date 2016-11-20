/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.jmx.JMXConfigBeanInf;
import com.att.aft.scld.config.jmx.JxmConfigMBean;
import com.att.aft.scld.config.util.ConfigConstants;

public class JMXConfigurationStrategy extends AbstractConfigurationStrategy {

	private String managerName;
	private JMXConfigBeanInf jmxConfigBean;
	
	public JMXConfigurationStrategy(String managerName) {
		this.managerName = managerName;
	}
	
	public JMXConfigBeanInf getJMXConfigBean() {
		return jmxConfigBean;
	}

	public void registerForRefresh(Map<String, Map<String, String>> configs, Map<String, Config> defaultConfigs) throws ConfigException {
		try {
			ObjectName jmxName = new ObjectName(ConfigConstants.JMX_MBEAN_TYPE + ",name=JmxConfigurationManager" + "-" + managerName);
			jmxConfigBean = new JxmConfigMBean(managerName);
			ManagementFactory.getPlatformMBeanServer().registerMBean(jmxConfigBean, jmxName);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
			throw new ConfigException(ConfigConstants.CONFIG_ERROR_CODE_JMX_CREATTION, e);
		} catch (InstanceAlreadyExistsException e) {
			e.printStackTrace();
			throw new ConfigException(ConfigConstants.CONFIG_ERROR_CODE_JMX_CREATTION, e);
		} catch (MBeanRegistrationException e) {
			e.printStackTrace();
			throw new ConfigException(ConfigConstants.CONFIG_ERROR_CODE_JMX_CREATTION, e);
		} catch (NotCompliantMBeanException e) {
			e.printStackTrace();
			throw new ConfigException(ConfigConstants.CONFIG_ERROR_CODE_JMX_CREATTION, e);
		}
	}

	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}
	
	public static void main(String args[]) throws ConfigException, InterruptedException {
		JMXConfigurationStrategy jvm = new JMXConfigurationStrategy("jmxConsoleConfigManager");
		Map<String, Map<String, String>> configs = null;
		Map<String, Config> defaultConfigs = null;
		jvm.registerForRefresh(configs, defaultConfigs);
		Thread.sleep(Long.MAX_VALUE);
	}
}
