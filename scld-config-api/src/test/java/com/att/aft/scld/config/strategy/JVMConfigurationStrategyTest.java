/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.util.ConfigConstants;
import com.google.common.collect.Maps;

public class JVMConfigurationStrategyTest {

	private JVMConfigurationStrategy jvmStrategy;
	
	@Before
	public void setup() {
		jvmStrategy = new JVMConfigurationStrategy();
	}
	
	@Test
	public void testJvmLoadConfigsSuccessWithAZeroSystemPropertiesForJvmConfig() {
		Map<String, Map<String, String>> configs = Maps.newHashMap();
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		jvmStrategy.loadConfigs(configs, defaultConfigs);
		Assert.assertNotNull(configs.get(ConfigConstants.JVM_CONFIGS));
		
		//Assert.assertEquals("Oracle Corporation", configs.get(ConfigConstants.JVM_CONFIGS).get("java.specification.vendor"));
		//Assert.assertEquals("Oracle Corporation", configs.get(ConfigConstants.JVM_CONFIGS).get("java.vendor"));
		Assert.assertEquals("sun.awt.windows.WToolkit", configs.get(ConfigConstants.JVM_CONFIGS).get("awt.toolkit"));
	}
	
	@Test
	public void testJvmLoadConfigsSuccessWithSystemPropertiesForJvmConfig() {
		JVMConfigurationStrategy jvmStrategy = new JVMConfigurationStrategy();
		Map<String, Map<String, String>> configs = Maps.newHashMap();
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		System.setProperty("AFT_DME2_CONTAINER_ENV_KEY", "lrmKeyEnv1");
		
		jvmStrategy.loadConfigs(configs, defaultConfigs);
		Assert.assertNotNull(configs.get(ConfigConstants.JVM_CONFIGS));
		
		//Assert.assertEquals("Oracle Corporation", configs.get(ConfigConstants.JVM_CONFIGS).get("java.specification.vendor"));
		//Assert.assertEquals("Oracle Corporation", configs.get(ConfigConstants.JVM_CONFIGS).get("java.vendor"));
		Assert.assertEquals("sun.awt.windows.WToolkit", configs.get(ConfigConstants.JVM_CONFIGS).get("awt.toolkit"));
		Assert.assertNull(configs.get(ConfigConstants.JVM_CONFIGS).get("AFT_DME2_CONTAINER_ENV_KEY_UNKNOWN"));
		Assert.assertEquals("lrmKeyEnv1", configs.get(ConfigConstants.JVM_CONFIGS).get("AFT_DME2_CONTAINER_ENV_KEY"));
	}
	
	@Test
	public void testJvmLoadConfigsSuccessWithNewPropertyForJvmConfig() {
		JVMConfigurationStrategy jvmStrategy = new JVMConfigurationStrategy();
		Map<String, Map<String, String>> configs = Maps.newHashMap();
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		System.setProperty("AFT_DME2_CONTAINER_PLATFORM_KEY", "lrmKeyPlatform");
		jvmStrategy.loadConfigs(configs, defaultConfigs);
		Assert.assertNotNull(configs.get(ConfigConstants.JVM_CONFIGS));
		Assert.assertEquals("lrmKeyPlatform", configs.get(ConfigConstants.JVM_CONFIGS).get("AFT_DME2_CONTAINER_PLATFORM_KEY"));
		Assert.assertNull(configs.get(ConfigConstants.JVM_CONFIGS).get("AFT_DME2_CONTAINER_PLATFORM_KEY_UNKNOWN"));
		
		System.setProperty("AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY", "SCLD_PLATFORM_JVM");
		Assert.assertNull(configs.get(ConfigConstants.JVM_CONFIGS).get("AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY"));
	}
}
