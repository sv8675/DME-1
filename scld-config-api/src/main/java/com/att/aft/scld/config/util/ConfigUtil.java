/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.att.aft.scld.config.exception.ConfigException;

public class ConfigUtil {

	public static PropertiesConfiguration getPropertiesConfiguration(String fileName) throws ConfigException {
		PropertiesConfiguration propConfigs = null;
		try {
			propConfigs = new PropertiesConfiguration();
			propConfigs.setDelimiterParsingDisabled(true);
			propConfigs.setFileName(fileName);
			propConfigs.load();
		} catch (ConfigurationException e) {
			throw new ConfigException(ConfigConstants.CONFIG_ERROR_CODE_FILE_NOT_FOUND, e);
		}
		return propConfigs;
	}
}
