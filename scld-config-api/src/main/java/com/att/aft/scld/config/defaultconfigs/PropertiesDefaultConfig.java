/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.defaultconfigs;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.dto.ScldConfig;
import com.att.aft.scld.config.dto.Config.ConfigType;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.util.ConfigUtil;
import com.google.common.collect.Maps;

public class PropertiesDefaultConfig implements DefaultConfig {

	public Map<String, Config> loadDefaultConfigs(String defaultFileConfigName) throws ConfigException {
		ScldConfig configApi = ScldConfig.getInstance();
		
		Map<String, Config> defaultConfigs = Maps.newHashMap();
		PropertiesConfiguration defaultConfig = ConfigUtil.getPropertiesConfiguration(defaultFileConfigName);
		for (Iterator<String> propsIter = defaultConfig.getKeys(); propsIter.hasNext();) {
			 String propKey = propsIter.next();
			 String[] propertyInfo = defaultConfig.getString(propKey).split(configApi.getDefaultConfigFileDataSeperator());
			 
			 if(propertyInfo.length == 2) {
				 defaultConfigs.put(propKey, new Config(propertyInfo[0], Config.getConfigType(propertyInfo[1]), ConfigType.APP.toString().equalsIgnoreCase(propertyInfo[1]) ? Boolean.TRUE : Boolean.FALSE)); 
			 } else if(propertyInfo.length >= 3) {
				 defaultConfigs.put(propKey, new Config(propertyInfo[0], Config.getConfigType(propertyInfo[1]), Boolean.valueOf(propertyInfo[2])));
			 } else {
				 defaultConfigs.put(propKey, new Config(propertyInfo[0], ConfigType.APP, Boolean.TRUE));
			 }
		}
		return defaultConfigs;
	}	
}
