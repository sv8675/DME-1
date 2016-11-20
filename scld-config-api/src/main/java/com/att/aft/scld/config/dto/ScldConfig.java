/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.dto;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.util.ConfigConstants;
import com.att.aft.scld.config.util.ConfigUtil;

public class ScldConfig {

	private String defaultConfigFileName = "defaultConfigs.properties";
	private String defaultConfigFileDataSeperator = "(?<!\\\\)\\|";
	private String defaultConfigFileType = "properties";
	private int timerForFileStrategy = 10;
	
	private static ScldConfig instance;
	
	private ScldConfig() {
		
	}
	
	public static ScldConfig getInstance() {
		if(instance == null) {
			instance = new ScldConfig();
			instance.loadSetUp();
		} 
		return instance;
	}
	
	public void loadSetUp() {
		PropertiesConfiguration propConfig;
		try {
			propConfig = ConfigUtil.getPropertiesConfiguration(ConfigConstants.CONFIG_API_SETUP_FILE_NAME);
			defaultConfigFileName = propConfig.getString("defaultConfigFileName");
			defaultConfigFileDataSeperator = propConfig.getString("defaultConfigFileDataSeperator");
			defaultConfigFileType = propConfig.getString("defaultConfigFileType");
			timerForFileStrategy = propConfig.getInt("timerForFileStrategy");
		} catch (ConfigException e) {
			e.printStackTrace();
		}
	}

	public String getDefaultConfigFileName() {
		return defaultConfigFileName;
	}


	public String getDefaultConfigFileDataSeperator() {
		return defaultConfigFileDataSeperator;
	}


	public String getDefaultConfigFileType() {
		return defaultConfigFileType;
	}


	public int getTimerForFileStrategy() {
		return timerForFileStrategy;
	}
	
}
