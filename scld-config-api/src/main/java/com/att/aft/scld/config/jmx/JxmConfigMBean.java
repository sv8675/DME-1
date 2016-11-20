/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.jmx;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import com.att.aft.scld.config.ConfigurationManager;
import com.att.aft.scld.config.exception.ConfigException;

public class JxmConfigMBean extends StandardMBean implements JMXConfigBeanInf {

	public JxmConfigMBean(String managerName, Class<?> mbeanInterface) throws NotCompliantMBeanException {
		super(mbeanInterface);
		this.managerName = managerName;
	}

	public JxmConfigMBean(String managerName) throws NotCompliantMBeanException {
		super(JMXConfigBeanInf.class);
		this.managerName = managerName;
	}
	
	private String managerName;

	public String getProperty(String propertyName) {
		try {
			return ConfigurationManager.getInstance(managerName).getProperty(propertyName);
		} catch (ConfigException e) {
		}
		return null;
	}

	public String setProperty(String propertyName, String propertyValue) throws ConfigException {
		ConfigurationManager.getInstance(managerName).setPropertyforJmx(propertyName, propertyValue);
		return getProperty(propertyName);
	}

	public String getManagerName() {
		return managerName;
	}

	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}
}
