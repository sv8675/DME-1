/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Map;
import java.util.Properties;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.util.ConfigConstants;
import com.google.common.collect.Maps;

public class JVMConfigurationStrategy extends AbstractConfigurationStrategy {

	public void loadConfigs(Map<String, Map<String, String>> configs, final Map<String, Config> defaultConfigs) {
		
		Properties props = System.getProperties();
		Map<String, String> jvmConfigs = Maps.newConcurrentMap();
		for (String propertyName : props.stringPropertyNames()) {
			jvmConfigs.put(propertyName, System.getProperty(propertyName));
		}
		
		jvmConfigs.putAll(System.getenv());
		configs.put(ConfigConstants.JVM_CONFIGS, jvmConfigs);
	}
}

