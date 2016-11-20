/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.util.ConfigConstants;
import com.google.common.collect.Maps;

public class UserPropertyConfigurationStrategy extends AbstractConfigurationStrategy {

	private PropertiesConfiguration userPropConfigs;
	
	public UserPropertyConfigurationStrategy(PropertiesConfiguration userPropConfigs) {
		this.userPropConfigs = userPropConfigs;
	}

	public void loadConfigs(Map<String, Map<String, String>> configs, final Map<String, Config> defaultConfigs) {
		
		Map<String, String> userPropConfigsMap = configs.get(ConfigConstants.USER_PROP_CONFIGS);
		
		if(userPropConfigsMap == null) {
			userPropConfigsMap = Maps.newConcurrentMap();
		}
		
		for (Iterator<String> userPropsIter = userPropConfigs.getKeys(); userPropsIter.hasNext();) {
			String propKey = userPropsIter.next();
			userPropConfigsMap.put(propKey, userPropConfigs.getString(propKey));
		}
		configs.put(ConfigConstants.USER_PROP_CONFIGS, userPropConfigsMap);
	}
}
