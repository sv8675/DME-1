/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Map;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.exception.ConfigException;

public abstract class AbstractConfigurationStrategy implements ConfigurationStrategy {

	public void loadConfigs(Map<String, Map<String, String>> configs, Map<String, Config> defaultConfigs) throws ConfigException {
	}
	
	public void registerForRefresh(Map<String, Map<String, String>> configs, Map<String, Config> defaultConfigs) throws ConfigException {
	}

}
