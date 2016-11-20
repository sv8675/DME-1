/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.strategy.FileConfigurationStrategy;
import com.google.common.collect.Maps;

public class FileConfigurationStrategyTest {

	@Test(expected = ConfigException.class)
	public void testFileStrategyloadConfigsFailureFileNotFound() throws ConfigException {
		FileConfigurationStrategy staticFilestrategyFsIf = new FileConfigurationStrategy("UnknowFile.properties");
		Map<String, Map<String, String>> configsFsIf = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigsFsIf = Maps.newHashMap();
		staticFilestrategyFsIf.loadConfigs(configsFsIf, defaultConfigsFsIf);
	}
	
	@Test(expected = ConfigException.class)
	public void testFileRegisterForRefreshFailureFileNotFound() throws ConfigException {
		FileConfigurationStrategy staticFilestrategyFsRf = new FileConfigurationStrategy("UnknowFile.properties");
		Map<String, Map<String, String>> configsFsRf = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigsFsRf = Maps.newHashMap();
		staticFilestrategyFsRf.registerForRefresh(configsFsRf, defaultConfigsFsRf);
	}
	
	@Test
	public void testFileStrategyloadConfigsSuccess() throws ConfigException {
		Map<String, Map<String, String>> configsFsIs = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigsFsIs = Maps.newHashMap();
		FileConfigurationStrategy staticFilestrategyFsIs = new FileConfigurationStrategy("TestConfig_Static.properties");
		staticFilestrategyFsIs.loadConfigs(configsFsIs, defaultConfigsFsIs);
		
		Assert.assertEquals(";", staticFilestrategyFsIs.getPropertiesConfiguration().getString("DME2_DOMAIN_SEP"));
		Assert.assertEquals("axxxxx", staticFilestrategyFsIs.getPropertiesConfiguration().getString("DME2_GRM_USER"));
		Assert.assertEquals("axxxxxPass", staticFilestrategyFsIs.getPropertiesConfiguration().getString("DME2_GRM_PASS"));
		Assert.assertEquals("false", staticFilestrategyFsIs.getPropertiesConfiguration().getString("DME2_GRM_AUTH"));
		Assert.assertEquals("<routeInfo xmlns=\"http://aft.att.com/dme3/types\">", staticFilestrategyFsIs.getPropertiesConfiguration().getString("DME2_DEF_RTINFO"));
		Assert.assertEquals("true", staticFilestrategyFsIs.getPropertiesConfiguration().getString("DME2.CLDEBUG"));
	}

	
	@Test
	public void testFileStrategyRegisterForRefreshSuccess() throws ConfigException {
		Map<String, Map<String, String>> configsFsRs = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigsFsRs = Maps.newHashMap();
		FileConfigurationStrategy staticFilestrategyFsRs = new FileConfigurationStrategy("TestConfig_Static.properties");
		staticFilestrategyFsRs.loadConfigs(configsFsRs, defaultConfigsFsRs);
		
		Assert.assertEquals(";", staticFilestrategyFsRs.getPropertiesConfiguration().getString("DME2_DOMAIN_SEP"));
		Assert.assertEquals("axxxxx", staticFilestrategyFsRs.getPropertiesConfiguration().getString("DME2_GRM_USER"));
		Assert.assertEquals("axxxxxPass", staticFilestrategyFsRs.getPropertiesConfiguration().getString("DME2_GRM_PASS"));
		Assert.assertEquals("false", staticFilestrategyFsRs.getPropertiesConfiguration().getString("DME2_GRM_AUTH"));
		Assert.assertEquals("<routeInfo xmlns=\"http://aft.att.com/dme3/types\">", staticFilestrategyFsRs.getPropertiesConfiguration().getString("DME2_DEF_RTINFO"));
		Assert.assertEquals("true", staticFilestrategyFsRs.getPropertiesConfiguration().getString("DME2.CLDEBUG"));
	}
	
	
	@Test
	public void testFileStrategyDynamicRegisterForRefreshSuccessWithNewValues() throws ConfigException {
		Map<String, Map<String, String>> configsFsDrs = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigsFsDrs = Maps.newHashMap();
		FileConfigurationStrategy staticFilestrategyFsDrs = new FileConfigurationStrategy("TestConfig_Dynamic.properties");
		staticFilestrategyFsDrs.registerForRefresh(configsFsDrs, defaultConfigsFsDrs);
		
		Assert.assertEquals(";", staticFilestrategyFsDrs.getPropertiesConfiguration().getString("DME2_DOMAIN_SEP"));
		Assert.assertEquals("axxxxx", staticFilestrategyFsDrs.getPropertiesConfiguration().getString("DME2_GRM_USER"));
		Assert.assertEquals("axxxxxPass", staticFilestrategyFsDrs.getPropertiesConfiguration().getString("DME2_GRM_PASS"));
		Assert.assertEquals("false", staticFilestrategyFsDrs.getPropertiesConfiguration().getString("DME2_GRM_AUTH"));
		Assert.assertEquals("<routeInfo xmlns=\"http://aft.att.com/dme3/types\">", staticFilestrategyFsDrs.getPropertiesConfiguration().getString("DME2_DEF_RTINFO"));
		Assert.assertEquals("true", staticFilestrategyFsDrs.getPropertiesConfiguration().getString("DME2.CLDEBUG"));
	}
}
