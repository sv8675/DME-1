/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Config {
		
	public static enum ConfigType {
		APP, SYSTEM
	}

	private ConfigType type;
	private boolean updatable = true;
	private String defaultValue;
	
	public Config(String defaultValue, ConfigType type, boolean updatable) {
		this.defaultValue = defaultValue;
		this.type = type;
		this.updatable = updatable;
	}

	public ConfigType getType() {
		return type;
	}

	public boolean isUpdatable() {
		return this.updatable;
	}
	
	public static ConfigType getConfigType(String type) {
		for(ConfigType configType : ConfigType.values()) {
			if(configType.toString().equalsIgnoreCase(type)) {
				return configType;
			}
		}
		return null;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
