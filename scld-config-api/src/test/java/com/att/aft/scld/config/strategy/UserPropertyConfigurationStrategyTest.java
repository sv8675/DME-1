/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.util.ConfigConstants;
import com.google.common.collect.Maps;

public class UserPropertyConfigurationStrategyTest {

	private UserPropertyConfigurationStrategy userPropStrategy;
	
	@Test
	public void testUserPropertyConfigLoadConfigsSuccessWithEmptyUserPropConfigs() {
		Map<String, Map<String, String>> configs = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		PropertiesConfiguration propConfigs = new PropertiesConfiguration();
		userPropStrategy = new UserPropertyConfigurationStrategy(propConfigs);
		userPropStrategy.loadConfigs(configs, defaultConfigs);
		
		Assert.assertNotNull(configs.get(ConfigConstants.USER_PROP_CONFIGS));
		Assert.assertNull(configs.get(ConfigConstants.USER_PROP_CONFIGS).get("unknown property"));
	}
	
	@Test
	public void testUserPropertyConfigLoadConfigsSuccessWithUserPropConfigs() {
		Map<String, Map<String, String>> configs = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		
		PropertiesConfiguration propConfigs = new PropertiesConfiguration();
		propConfigs.addProperty("AFT_DME2_DISABLE_INGRESS_REPLY_STREAM", "true");
		propConfigs.addProperty("AFT_DME2_CONTAINER_NAME_KEY", "lrmKeyName");
		propConfigs.addProperty("AFT_DME2_CONTAINER_VERSION_KEY", "lrmKeyVersion");
		userPropStrategy = new UserPropertyConfigurationStrategy(propConfigs);
		userPropStrategy.loadConfigs(configs, defaultConfigs);
		
		Assert.assertNotNull(configs.get(ConfigConstants.USER_PROP_CONFIGS));
		Assert.assertEquals(3, configs.get(ConfigConstants.USER_PROP_CONFIGS).size());
		Assert.assertEquals("true", configs.get(ConfigConstants.USER_PROP_CONFIGS).get("AFT_DME2_DISABLE_INGRESS_REPLY_STREAM"));
		Assert.assertEquals("lrmKeyName", configs.get(ConfigConstants.USER_PROP_CONFIGS).get("AFT_DME2_CONTAINER_NAME_KEY"));
		Assert.assertEquals("lrmKeyVersion", configs.get(ConfigConstants.USER_PROP_CONFIGS).get("AFT_DME2_CONTAINER_VERSION_KEY"));
		Assert.assertNull(configs.get(ConfigConstants.USER_PROP_CONFIGS).get("unknown property"));
	}
	
	@Test
	public void testUserPropertyConfigLoadConfigsSuccessWithUserPropConfigsUnknownProperties() {
		Map<String, Map<String, String>> configs = Maps.newConcurrentMap();
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		
		PropertiesConfiguration propConfigs = new PropertiesConfiguration();
		propConfigs.addProperty("AFT_DME2_CONTAINER_ROUTEOFFER_KEY", "lrmKeyRO");
		
		userPropStrategy = new UserPropertyConfigurationStrategy(propConfigs);
		userPropStrategy.loadConfigs(configs, defaultConfigs);
		
		Assert.assertNotNull(configs.get(ConfigConstants.USER_PROP_CONFIGS));
		Assert.assertNull(configs.get(ConfigConstants.USER_PROP_CONFIGS).get("AFT_DME2_CONTAINER_ROUTEOFFER_UNKNOWN"));
	}
}
