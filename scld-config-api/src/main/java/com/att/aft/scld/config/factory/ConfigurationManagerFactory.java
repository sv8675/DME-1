/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.factory;

import com.att.aft.scld.config.ConfigurationManager;
import com.att.aft.scld.config.exception.ConfigException;

public class ConfigurationManagerFactory {

	public ConfigurationManager getConfigurationManager() throws ConfigException {
		return ConfigurationManager.getInstance();
	}

}
