/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

import com.att.aft.scld.config.defaultconfigs.DefaultConfig;
import com.att.aft.scld.config.defaultconfigs.PropertiesDefaultConfig;
import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.dto.ScldConfig;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.strategy.ConfigurationStrategy;
import com.att.aft.scld.config.strategy.FileConfigurationStrategy;
import com.att.aft.scld.config.strategy.GRMPullConfgurationStrategy;
import com.att.aft.scld.config.strategy.JMXConfigurationStrategy;
import com.att.aft.scld.config.strategy.JVMConfigurationStrategy;
import com.att.aft.scld.config.strategy.UserPropertyConfigurationStrategy;
import com.att.aft.scld.config.util.ConfigConstants;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ConfigurationManager implements ConfigurationIntf {
	
	//global variables will be static 
	private static Map<String, Config> defaultConfigs = Maps.newConcurrentMap();
	private static Map<String, Map<String, String>> jvmConfigs = Maps.newHashMap();
	
	
	private static Map<String, ConfigurationManager> configManagerMap = Maps.newConcurrentMap();
	private Map<String, Map<String, String>> configs = Maps.newConcurrentMap();
	private Map<String, String> currentConfigs = Maps.newConcurrentMap();
	
	
	private String managerName = null;
	private List<String> defaultFileConfigNames = null;
	private String fileConfigName = null;
	private List<ConfigurationStrategy> configCommands = Lists.newArrayList();
	protected FileConfigurationStrategy fileStrategy = null;
	private PropertiesConfiguration userPropsConfig = null;
	
	static {
		try {
			new ConfigurationManager();
		} catch (ConfigException e) {e.printStackTrace();}
	}
	
	protected ConfigurationManager() throws ConfigException {
		this.managerName = ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME;
		this.defaultFileConfigNames = Lists.newArrayList(ScldConfig.getInstance().getDefaultConfigFileName());
		this.loadDefaultConfigs(ScldConfig.getInstance().getDefaultConfigFileName());
		new JVMConfigurationStrategy().loadConfigs(jvmConfigs, defaultConfigs);
		configManagerMap.put(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME, this);
	}
	
	protected ConfigurationManager(String managerName, List<String> defaultFileConfigNames, String fileName, List<ConfigurationStrategy> configCommands, PropertiesConfiguration userPropsConfig) throws ConfigException {
		this.managerName = managerName;
		this.defaultFileConfigNames = CollectionUtils.isEmpty(defaultFileConfigNames) ? Lists.newArrayList(ScldConfig.getInstance().getDefaultConfigFileName()) : defaultFileConfigNames;
		this.fileConfigName = fileName;
		this.userPropsConfig = userPropsConfig;
		if(CollectionUtils.isNotEmpty(configCommands)) {
			this.configCommands.addAll(configCommands);
		}
	}
	
	public static ConfigurationManager getInstance() throws ConfigException {
		return getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME);
	}
	
	public static ConfigurationManager getInstance(String managerName) throws ConfigException {
		return getInstance(managerName, ScldConfig.getInstance().getDefaultConfigFileName(), null, null, null);
	}
	
	public static ConfigurationManager getInstance(String managerName, String defaultConfigFileName) throws ConfigException {
		return getInstance(managerName, defaultConfigFileName, null, null, null);
	}
	
	public static ConfigurationManager getInstance(String managerName, String defaultConfigFileName, String fileName) throws ConfigException {
		return getInstance(managerName, defaultConfigFileName, fileName, null, null);
	}
	
	public static ConfigurationManager getInstance(String managerName, String defaultConfigFileName, List<ConfigurationStrategy> configCommands) throws ConfigException {
		return getInstance(managerName, defaultConfigFileName, null, configCommands, null);
	}
	
	public static ConfigurationManager getInstance(String managerName, PropertiesConfiguration userPropsConfig) throws ConfigException {
		return getInstance(managerName, ScldConfig.getInstance().getDefaultConfigFileName(), null, null, userPropsConfig);
	}
	
	public static ConfigurationManager getInstance(String managerName, String defaultConfigFileName, PropertiesConfiguration userPropsConfig) throws ConfigException {
		return getInstance(managerName, defaultConfigFileName, null, null, userPropsConfig);
	}

	public static ConfigurationManager getInstance(String managerName, String defaultConfigFileName, String fileName, List<ConfigurationStrategy> configCommands, PropertiesConfiguration userPropsConfig)  throws ConfigException {
		if(!configManagerMap.containsKey(managerName)) {
			ConfigurationManager configManager = new ConfigurationManager(managerName, Lists.newArrayList(defaultConfigFileName), fileName, configCommands, userPropsConfig);
			configManager.initialize();
			configManagerMap.put(managerName, configManager);
		}
		return configManagerMap.get(managerName);
	}
	
	public static ConfigurationManager getInstance(String managerName, List<String> defaultConfigFileNames, String fileName, List<ConfigurationStrategy> configCommands, PropertiesConfiguration userPropsConfig)  throws ConfigException {
		if(!configManagerMap.containsKey(managerName)) {
			ConfigurationManager configManager = new ConfigurationManager(managerName, defaultConfigFileNames, fileName, configCommands, userPropsConfig);
			configManager.initialize();
			configManagerMap.put(managerName, configManager);
		}
		return configManagerMap.get(managerName);
	}
	
	protected void initialize() throws ConfigException {
		if(CollectionUtils.isNotEmpty(defaultFileConfigNames)) {
			for(String defaultFileName : defaultFileConfigNames) {
				loadDefaultConfigs(defaultFileName);
			}
		}
		
		createStrategy();
		invokeStrategy();
		loadCurrentConfigs();
	}

	
	private void createStrategy() {
	
		Map<String, String> jvm = Maps.newConcurrentMap();
		Map<String, String> userProp = Maps.newConcurrentMap();
		Map<String, String> grmPull = Maps.newConcurrentMap();
		Map<String, String> jmx = Maps.newConcurrentMap();
		
		configs.put(ConfigConstants.JVM_CONFIGS, jvm);
		configs.put(ConfigConstants.USER_PROP_CONFIGS, userProp);
		configs.put(ConfigConstants.GRM_PULL_CONFIGS, grmPull);
		configs.put(ConfigConstants.JMX_CONFIGS, jmx);
		
		if (CollectionUtils.isEmpty(configCommands)) {
			configCommands = Lists.newArrayList();
			
			if (StringUtils.isNotBlank(fileConfigName)) {
				fileStrategy = new FileConfigurationStrategy(fileConfigName);
				configCommands.add(fileStrategy);
			}
			
			configCommands.add(new JVMConfigurationStrategy());
			if (userPropsConfig != null) {
				configCommands.add(new UserPropertyConfigurationStrategy(userPropsConfig));
			}
			configCommands.add(new GRMPullConfgurationStrategy());
			configCommands.add(new JMXConfigurationStrategy(managerName));
		} else {
			for (ConfigurationStrategy configStrategy : configCommands) {
				if (configStrategy instanceof FileConfigurationStrategy) {
					fileStrategy = (FileConfigurationStrategy) configStrategy;
				}
			}
		}
	}
	
	private void invokeStrategy() throws ConfigException {
		
		for(ConfigurationStrategy configurationStrategy : configCommands) {
				configurationStrategy.loadConfigs(configs, defaultConfigs);
				configurationStrategy.registerForRefresh(configs, defaultConfigs);
		}
	}
	
	private void loadDefaultConfigs(String defaultFileConfigName) throws ConfigException {
		
		DefaultConfig config = null;
		ScldConfig configApi = ScldConfig.getInstance();
		
		if(configApi.getDefaultConfigFileType().equalsIgnoreCase("properties")) {
			config = new PropertiesDefaultConfig();
			Map<String, Config> propDefaultConfigs = config.loadDefaultConfigs(defaultFileConfigName);
			defaultConfigs.putAll(propDefaultConfigs);
		} else {
			// we can support more default config types like Json or xml
		}
	}
	
	private void loadCurrentConfigs() {
		
		currentConfigs.clear();
		
		//jvm Configs
		Map<String, String> jvm = configs.get(ConfigConstants.JVM_CONFIGS);
		for(String propertyName : jvm.keySet()) {
			currentConfigs.put(propertyName, jvm.get(propertyName));
		}
		
		//user Property Configs
		Map<String, String> userProp = configs.get(ConfigConstants.USER_PROP_CONFIGS);
		for(String propertyName : userProp.keySet()) {
			currentConfigs.put(propertyName, userProp.get(propertyName));
		}
		
		// grm Pull

		// jmx
		Map<String, String> jmx = configs.get(ConfigConstants.JMX_CONFIGS);
		for (String propertyName : jmx.keySet()) {
			currentConfigs.put(propertyName, jmx.get(propertyName));
		}
	}

	public int getInt(String propertyName) {
		return Integer.parseInt(getProperty(propertyName));
	}
	
	public int getInt(String propertyName, int defaultValue) {
		String configValue = getProperty(propertyName);
		return ((configValue != null)? Integer.parseInt(configValue):defaultValue);
	}		

	public long getLong(String propertyName) {
		return Long.parseLong(getProperty(propertyName));
	}

	public long getLong(String propertyName, long defaultValue) {
		String configValue = getProperty(propertyName);
		return ((configValue != null)? Long.parseLong(configValue):defaultValue);
	}	
	
	public double getDouble(String propertyName) {
		return Double.parseDouble(getProperty(propertyName));
	}

	public double getDouble(String propertyName, double defaultValue) {
		String configValue = getProperty(propertyName);
		return ((configValue != null)? Double.parseDouble(configValue):defaultValue);
	}		

	public void saveToFile() {
		//dump the configs to a file....is this required?
		
	}

	public float getFloat(String propertyName) {
		return Float.parseFloat(getProperty(propertyName));
	}
	
	public float getFloat(String propertyName, float defaultValue) {
		String configValue = getProperty(propertyName);
		return ((configValue != null)? Float.parseFloat(configValue):defaultValue);
	}	

	public boolean getBoolean(String propertyName) {
		return Boolean.parseBoolean(getProperty(propertyName));
	}
	
	public boolean getBoolean(String propertyName, boolean defaultValue) {
		String configValue = getProperty(propertyName);
		return ((configValue != null)? Boolean.parseBoolean(configValue):defaultValue);
	}
	
	public String getProperty(String propertyName) {
		if(currentConfigs.containsKey(propertyName)) {
			return currentConfigs.get(propertyName);
		} else if(fileStrategy != null && fileStrategy.getPropertiesConfiguration() != null && StringUtils.isNotBlank(fileStrategy.getPropertiesConfiguration().getString(propertyName))) {
			return fileStrategy.getPropertiesConfiguration().getString(propertyName);
		} 
		return defaultConfigs.get(propertyName) != null ? defaultConfigs.get(propertyName).getDefaultValue() : null;
	}
	
	public String getProperty(String propertyName, String defaultValue) {
		String configValue = getProperty(propertyName);
		return StringUtils.isNoneBlank(configValue) ? configValue:defaultValue;
	}	
	
	public void setPropertyforJmx(String key, String value) {
		Config config = defaultConfigs.get(key);
		if (config != null && config.isUpdatable()) {
			if (StringUtils.isBlank(value)) {
				if (configs.get(ConfigConstants.JMX_CONFIGS).containsKey(key)) {
					configs.get(ConfigConstants.JMX_CONFIGS).remove(key);
					currentConfigs.remove(key);
					if (configs.get(ConfigConstants.GRM_PULL_CONFIGS).containsKey(key)) {
						currentConfigs.put(key, configs.get(ConfigConstants.GRM_PULL_CONFIGS).get(key));
					} else if (configs.get(ConfigConstants.USER_PROP_CONFIGS).containsKey(key)) {
						currentConfigs.put(key, configs.get(ConfigConstants.USER_PROP_CONFIGS).get(key));
					} else if (configs.get(ConfigConstants.JVM_CONFIGS).containsKey(key)) {
						currentConfigs.put(key, configs.get(ConfigConstants.JVM_CONFIGS).get(key));
					}
				}
			} else {
				configs.get(ConfigConstants.JMX_CONFIGS).put(key, value);
				currentConfigs.put(key, value);
			}
		}
	}
	
	public void setPropertyforGrmPull(String key, String value) {
		Config config = defaultConfigs.get(key);
		if (config != null && config.isUpdatable()) {
			if (!configs.get(ConfigConstants.JMX_CONFIGS).containsKey(key)) {
				configs.get(ConfigConstants.GRM_PULL_CONFIGS).put(key, value);
				currentConfigs.put(key, value);
			}
		}
	}

	protected String getManagerName() {
		return managerName;
	}
}
