/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import junit.framework.Assert;

import org.junit.Test;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.util.ConfigConstants;

public class JMXConfigurationStrategyTest {

	@Test
	public void testRegisterForRefreshMBeanRegisteredSuccess() throws ConfigException, IntrospectionException, InstanceNotFoundException, MalformedObjectNameException, ReflectionException {
		JMXConfigurationStrategy jmxStrategy = new JMXConfigurationStrategy("jmxConfigurationManagerSuccess");
		Map<String, Map<String, String>> configs = null;
		Map<String, Config> defaultConfigs = null;
		jmxStrategy.registerForRefresh(configs, defaultConfigs);
		MBeanInfo mBeanInfo = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName(ConfigConstants.JMX_MBEAN_TYPE + ",name=JmxConfigurationManager" + "-" + "jmxConfigurationManagerSuccess"));
		Assert.assertNotNull(mBeanInfo);
	}
	
	@Test(expected = InstanceNotFoundException.class)
	public void testRegisterForRefreshMBeanRegisteredFailure() throws ConfigException, IntrospectionException, InstanceNotFoundException, MalformedObjectNameException, ReflectionException {
		JMXConfigurationStrategy jmxStrategy = new JMXConfigurationStrategy("jmxConfigurationManagerFailure");
		Map<String, Map<String, String>> configs = null;
		Map<String, Config> defaultConfigs = null;
		jmxStrategy.registerForRefresh(configs, defaultConfigs);
		MBeanInfo mBeanInfo = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName(ConfigConstants.JMX_MBEAN_TYPE + ",name=JmxConfigurationManager" + "-" + "jmxConfigurationManagerF"));
		Assert.assertNull(mBeanInfo);
	}
	
	@Test
	public void testRegisterForRefreshMBeanRegisteredCheckOperationsSuccess() throws ConfigException, IntrospectionException, InstanceNotFoundException, MalformedObjectNameException, ReflectionException {
		JMXConfigurationStrategy jmxStrategy = new JMXConfigurationStrategy("jmxConfigurationManager");
		Map<String, Map<String, String>> configs = null;
		Map<String, Config> defaultConfigs = null;
		jmxStrategy.registerForRefresh(configs, defaultConfigs);
		MBeanInfo mBeanInfo = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName(ConfigConstants.JMX_MBEAN_TYPE + ",name=JmxConfigurationManager" + "-" + "jmxConfigurationManager"));
		Assert.assertNotNull(mBeanInfo);
		Assert.assertEquals(2, mBeanInfo.getOperations().length);
		Assert.assertEquals("setProperty", mBeanInfo.getOperations()[0].getName());
		Assert.assertEquals("getProperty", mBeanInfo.getOperations()[1].getName());
	}
}
