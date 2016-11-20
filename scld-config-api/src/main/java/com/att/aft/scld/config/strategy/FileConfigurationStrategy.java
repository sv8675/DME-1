/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.strategy;

import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.util.ConfigConstants;

public class FileConfigurationStrategy extends AbstractConfigurationStrategy {

	private String fileName = null;
	private PropertiesConfiguration builder = null;

	public FileConfigurationStrategy(String fileName) {
		this.fileName = fileName;
	}

	public void loadConfigs(Map<String, Map<String, String>> configs, final Map<String, Config> defaultConfigs) throws ConfigException {
		try {
			builder = new PropertiesConfiguration();
			builder.setDelimiterParsingDisabled(true);
			builder.setFileName(fileName);
			builder.load();
		} catch (ConfigurationException e) {
			throw new ConfigException(ConfigConstants.CONFIG_ERROR_CODE_FILE_NOT_FOUND, e.getMessage());
		}
	}
	
	public void registerForRefresh(Map<String, Map<String, String>> configs, final Map<String, Config> defaultConfigs) throws ConfigException {
		if(builder == null) {
			loadConfigs(configs, defaultConfigs);
		}
	}

	public PropertiesConfiguration getPropertiesConfiguration() {
		return builder;
	}
}
