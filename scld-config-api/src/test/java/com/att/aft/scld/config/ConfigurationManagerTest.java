/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;

import junit.framework.Assert;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

import com.att.aft.scld.config.dto.ScldConfig;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.jmx.JMXConfigBeanInf;
import com.att.aft.scld.config.strategy.ConfigurationStrategy;
import com.att.aft.scld.config.strategy.FileConfigurationStrategy;
import com.att.aft.scld.config.strategy.JMXConfigurationStrategy;
import com.att.aft.scld.config.strategy.JVMConfigurationStrategy;
import com.att.aft.scld.config.strategy.UserPropertyConfigurationStrategy;
import com.att.aft.scld.config.util.ConfigConstants;
import com.google.common.collect.Lists;

public class ConfigurationManagerTest {

	static {
		System.setProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS", "300,501");
		System.setProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT", "26000");
		System.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "3");
    }

	
	@Test(expected = ConfigException.class)
	public void testCreationOfConfigManagerDefaultConfigFileNotFound() throws ConfigException {
		ConfigurationManager configurationManager =  ConfigurationManager.getInstance("DefaultFileNotFound", "Unknow_default_configs.properties", null, null, null);
		Assert.assertNotNull(configurationManager);
	}
	
	@Test(expected = ConfigException.class)
	public void testCreationOfConfigManagerFileStrategyFileNotFound() throws ConfigException {
		ConfigurationManager configurationManager =  ConfigurationManager.getInstance("FileStrategyFileNotFound", ScldConfig.getInstance().getDefaultConfigFileName(), "Unknow_file_strategy.properties", null, null);
		Assert.assertNotNull(configurationManager);
	}
	
	@Test
	public void testCreationOfDefaultConfigManagerWithNoArg() throws ConfigException {
		ConfigurationManager defaultConfigurationManager =  ConfigurationManager.getInstance();
		Assert.assertNotNull(defaultConfigurationManager);
		Assert.assertEquals(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME, defaultConfigurationManager.getManagerName());
	}
	
	@Test
	public void testCreationOfDefaultConfigManager() throws ConfigException {
		ConfigurationManager defaultConfigurationManager =  ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME);
		Assert.assertNotNull(defaultConfigurationManager);
	}
	
	@Test
	public void testCreationOfDefaultConfigManagerWithNameAndDefaultConfigFileName() throws ConfigException {
		ConfigurationManager configurationManagerWithNameAndDefaultOverrided =  ConfigurationManager.getInstance("ConfigurationManagerWithNameAndDefaultOverrided", "defaultConfigs_override.properties");
		Assert.assertNotNull(configurationManagerWithNameAndDefaultOverrided);
	}	

	@Test
	public void testCreationOfConfigManagerWithNameAndDefaultConfigAndCommands() throws ConfigException {
		ConfigurationManager configManagerWithNameAndDefaultConfigAndCommands =  ConfigurationManager.getInstance("ConfigManagerWithNameAndDefaultConfigAndCommands", "defaultConfigs_override.properties", new ArrayList<ConfigurationStrategy>());
		Assert.assertNotNull(configManagerWithNameAndDefaultConfigAndCommands);
	}
	
	@Test
	public void testCreationOfConfigManagerWithNameAndUserProp() throws ConfigException {
		ConfigurationManager configurationManagerWithNameAndUserProp =  ConfigurationManager.getInstance("ConfigurationManagerWithNameAndUserProp", new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithNameAndUserProp);
	}
	
	@Test
	public void testCreationOfConfigManagerWithDefaultConfigs() throws ConfigException {
		ConfigurationManager configurationMaager =  ConfigurationManager.getInstance("ConfigurationManager", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", null, new PropertiesConfiguration());
		Assert.assertNotNull(configurationMaager);
	}
	
	@Test
	public void testCreationOfConfigManagerWithDefaultConfigsOverride() throws ConfigException {
		ConfigurationManager configurationManagerWithOverrideDefaultConfig =  ConfigurationManager.getInstance("ConfigurationManagerWithOverrideDefaultConfig", "defaultConfigs_override.properties", "fileBasedConfigs.properties", null, new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithOverrideDefaultConfig);
	}
	
	@Test
	public void testCreationOfConfigManagerWithFileName() throws ConfigException {
		List<String> fileNames = Lists.newArrayList();
		ConfigurationManager configurationManagerWithFileConfig =  ConfigurationManager.getInstance("ConfigurationManagerWithFileConfig", fileNames, "fileBasedConfigs.properties", null, null);
		Assert.assertNotNull(configurationManagerWithFileConfig);
	}
	
	@Test
	public void testCreationOfConfigManagerWithUserProp() throws ConfigException {
		List<String> fileNames = Lists.newArrayList();
		ConfigurationManager configurationManagerWithUserProp =  ConfigurationManager.getInstance("ConfigurationManagerWithUserProp", fileNames, null, null, new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithUserProp);
	}
		
	@Test
	public void testCreationOfConfigManagerWithAllVariables() throws ConfigException {
		ConfigurationManager configurationManagerWithAllVariables =  ConfigurationManager.getInstance("ConfigurationManagerWithAllVariables", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", new ArrayList<ConfigurationStrategy>(), new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithAllVariables);
	}
	
	@Test
	public void testCreationOfMultipleConfigManagers() throws ConfigException {
		ConfigurationManager configurationManagerWithAllVariables1 =  ConfigurationManager.getInstance("ConfigurationManagerWithAllVariables1", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", new ArrayList<ConfigurationStrategy>(), new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithAllVariables1);
		ConfigurationManager configurationManagerWithAllVariables2 =  ConfigurationManager.getInstance("ConfigurationManagerWithAllVariables2", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", new ArrayList<ConfigurationStrategy>(), new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithAllVariables2);
	}
	
	@Test
	public void testSameConfigManagerWhenCalledTwice() throws ConfigException {
		ConfigurationManager configurationManagerCalledTwice1 =  ConfigurationManager.getInstance("ConfigurationManagerCalledTwice1", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", new ArrayList<ConfigurationStrategy>(), new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerCalledTwice1);
		ConfigurationManager configurationManagerCalledTwice2 =  ConfigurationManager.getInstance("ConfigurationManagerCalledTwice2", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", new ArrayList<ConfigurationStrategy>(), new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerCalledTwice2);
		Assert.assertEquals(configurationManagerCalledTwice1, ConfigurationManager.getInstance("ConfigurationManagerCalledTwice1"));
		Assert.assertEquals(configurationManagerCalledTwice2, ConfigurationManager.getInstance("ConfigurationManagerCalledTwice2"));
	}
	
	
	@Test //Default Configs 
	public void testConfigManagerWithDefaultConfigs() throws ConfigException {
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		
		ConfigurationManager configurationManagerWithDefaultConfig =  ConfigurationManager.getInstance("ConfigurationManagerWithDefaultConfig", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configurationManagerWithDefaultConfig);
		Assert.assertEquals("false", configurationManagerWithDefaultConfig.getProperty("AFT_DME2_DISABLE_INGRESS_REPLY_STREAM")); //default config
		Assert.assertEquals("lrmRName", configurationManagerWithDefaultConfig.getProperty("AFT_DME2_CONTAINER_NAME_KEY")); //default config
		Assert.assertEquals("lrmRVer", configurationManagerWithDefaultConfig.getProperty("AFT_DME2_CONTAINER_VERSION_KEY")); //default config
		Assert.assertEquals("lrmRO", configurationManagerWithDefaultConfig.getProperty("AFT_DME2_CONTAINER_ROUTEOFFER_KEY")); //default config
		Assert.assertEquals("lrmEnv", configurationManagerWithDefaultConfig.getProperty("AFT_DME2_CONTAINER_ENV_KEY")); //default config
	}
	
	
	@Test 
	public void testConfigManagerWithDefaultConfigsWithPipe() throws ConfigException {
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		
		ConfigurationManager configurationManagerWithDefaultConfig =  ConfigurationManager.getInstance("ConfigurationManagerWithDefaultConfigWithPipe", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertEquals("Ash}|ok", configurationManagerWithDefaultConfig.getProperty("DME2_DOMAIN_TEST_PIPE")); //default config
		Assert.assertEquals("Content-Disposition: form-data; name=\"upload_file\"; filename=", configurationManagerWithDefaultConfig.getProperty("AFT_DME2_CONTENT_DISP_HEADER")); //default config
	}
	

	@Test //Default Configs Overrided By Another Default Configs
	public void testConfigManagerWithDefaultOverridedShouldHaveBothDefaultConfigs() throws ConfigException {
		ConfigurationManager configurationManagerWithBothDefaultOverrided =  ConfigurationManager.getInstance("ConfigurationManagerWithBothDefaultOverrided", "defaultConfigs_override.properties", null, null, null);
		Assert.assertNotNull(configurationManagerWithBothDefaultOverrided);
		Assert.assertEquals("false", configurationManagerWithBothDefaultOverrided.getProperty("AFT_DME2_DISABLE_INGRESS_REPLY_STREAM")); //default config
		Assert.assertEquals("lrmRName", configurationManagerWithBothDefaultOverrided.getProperty("AFT_DME2_CONTAINER_NAME_KEY")); //default config
		Assert.assertEquals("lrmRVer", configurationManagerWithBothDefaultOverrided.getProperty("AFT_DME2_CONTAINER_VERSION_KEY")); //default config
		Assert.assertEquals("lrmRO", configurationManagerWithBothDefaultOverrided.getProperty("AFT_DME2_CONTAINER_ROUTEOFFER_KEY")); //default config
		Assert.assertEquals("lrmEnv", configurationManagerWithBothDefaultOverrided.getProperty("AFT_DME2_CONTAINER_ENV_KEY")); //default config
		
		Assert.assertEquals("FALSE", configurationManagerWithBothDefaultOverrided.getProperty("DME2.TEMPQ_DEBUG")); //override Property
		Assert.assertEquals("mxxxxx", configurationManagerWithBothDefaultOverrided.getProperty("DME2_GRM_USER")); //override Property
		Assert.assertEquals("mxxxxx", configurationManagerWithBothDefaultOverrided.getProperty("DME2_GRM_PASS")); //override Property
		Assert.assertEquals("TRUE", configurationManagerWithBothDefaultOverrided.getProperty("DME2_GRM_AUTH")); //override Property
	}
	
	
	@Test
	public void testConfigManagerWithDefaultConfigsOverridedByFileBased() throws ConfigException {
		
		ConfigurationManager configManagerWithDefaultOverridedByByFileStrategy =  ConfigurationManager.getInstance("ConfigManagerWithDefaultOverridedByByFileStrategy", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", null, null);
		Assert.assertNotNull(configManagerWithDefaultOverridedByByFileStrategy);

		Assert.assertEquals("1015,1011,1012,1013,1006", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS")); //default config
		Assert.assertEquals("1011,1012,1013", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_RETRY_WS_CLOSE_CDS")); //default config
		
		Assert.assertEquals("1115,1111,1112,1113,1116", configManagerWithDefaultOverridedByByFileStrategy.getProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS")); // overrided by FileBased
		Assert.assertEquals("1111,1112,1113", configManagerWithDefaultOverridedByByFileStrategy.getProperty("AFT_DME2_RETRY_WS_CLOSE_CDS")); //overrided by FileBased
		
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		
		
		ConfigurationManager configManagerWithDefaultOverridedByByFileStrategyCreatedByListOfCommands =  ConfigurationManager.getInstance("ConfigManagerWithDefaultOverridedByByFileStrategyCreatedByListOfCommands", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithDefaultOverridedByByFileStrategyCreatedByListOfCommands);

		Assert.assertEquals("1015,1011,1012,1013,1006", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS")); //default config
		Assert.assertEquals("1011,1012,1013", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_RETRY_WS_CLOSE_CDS")); //default config
		
		Assert.assertEquals("1115,1111,1112,1113,1116", configManagerWithDefaultOverridedByByFileStrategyCreatedByListOfCommands.getProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS")); // overrided by FileBased
		Assert.assertEquals("1111,1112,1113", configManagerWithDefaultOverridedByByFileStrategyCreatedByListOfCommands.getProperty("AFT_DME2_RETRY_WS_CLOSE_CDS")); //overrided by FileBased
	}
	
	@Test
	public void testConfigManagerWithDefaultConfigsOverridedByJvm() throws ConfigException {
		//properties added in the setup method
		ConfigurationManager configManagerWithDefaultConfigsOverridedByJvm =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigsOverridedByJvm", ScldConfig.getInstance().getDefaultConfigFileName(), null, null, null);
		Assert.assertNotNull(configManagerWithDefaultConfigsOverridedByJvm);
		Assert.assertEquals("300,501", configManagerWithDefaultConfigsOverridedByJvm.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); //overrided by jvm
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new JVMConfigurationStrategy());
		
		ConfigurationManager configManagerWithDefaultConfigsOverridedByJvmCreatedByListOfCommands =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigsOverridedByJvmCreatedByListOfCommands", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithDefaultConfigsOverridedByJvmCreatedByListOfCommands);
		Assert.assertEquals("300,501", configManagerWithDefaultConfigsOverridedByJvm.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); //overrided by jvm
	}
	
	@Test
	public void testConfigManagerWithDefaultConfigsOverridedByUserProp() throws ConfigException {
	
		PropertiesConfiguration userProp = new PropertiesConfiguration();
		userProp.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "4");
		ConfigurationManager configManagerWithDefaultConfigsOverridedByUserProp =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigsOverridedByUserProp", ScldConfig.getInstance().getDefaultConfigFileName(), null, null, userProp);
		
		Assert.assertNotNull(configManagerWithDefaultConfigsOverridedByUserProp);
		Assert.assertEquals("1", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //default config
		Assert.assertEquals("4", configManagerWithDefaultConfigsOverridedByUserProp.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //overrided by UserProp
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new UserPropertyConfigurationStrategy(userProp));
		
		ConfigurationManager configManagerWithDefaultConfigsOverridedByUserPropCreatedByListOfCommands =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigsOverridedByUserPropCreatedByListOfCommands", ScldConfig.getInstance().getDefaultConfigFileName(), null, null, userProp);
		
		Assert.assertNotNull(configManagerWithDefaultConfigsOverridedByUserPropCreatedByListOfCommands);
		Assert.assertEquals("1", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //default config
		Assert.assertEquals("4", configManagerWithDefaultConfigsOverridedByUserPropCreatedByListOfCommands.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //overrided by UserProp
	}
	
	//@Test
	public void testConfigManagerWithDefaultConfigOverrideByGrmPull() throws ConfigException { /*TBD */ }
	
	@Test
	public void testConfigManagerWithDefaultConfigsOverridedByJmxIfPropertyUpdatable() throws ConfigException, IOException, JMException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException { 
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithDefaultConfigsOverridedByJmx");
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithDefaultConfigsOverridedByJmx =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigsOverridedByJmx", ScldConfig.getInstance().getDefaultConfigFileName(), null, configCommands, null);
		Assert.assertNotNull(configManagerWithDefaultConfigsOverridedByJmx);
		Assert.assertEquals("Content-Type", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_CTYPE_HEADER")); //default config
		
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_CTYPE_HEADER", "Content-Type-New");
		
		Assert.assertEquals("Content-Type-New", configManagerWithDefaultConfigsOverridedByJmx.getProperty("AFT_DME2_CTYPE_HEADER")); //overrided by Jmx
		
		jxmConfigMBean.setProperty("AFT_DME2_CTYPE_HEADER", null);
		Assert.assertEquals("Content-Type", configManagerWithDefaultConfigsOverridedByJmx.getProperty("AFT_DME2_CTYPE_HEADER")); // removed from current configs since property value is null
		
	}
	
	@Test
	public void testConfigManagerWithDefaultConfigsOverridedByJmxIfPropertyNotUpdatable() throws ConfigException, IOException, JMException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException { 
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithDefaultConfigsOverridedByJmxPropertyNotUpdatable");
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithDefaultConfigsOverridedByJmxPropertyNotUpdatable =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigsOverridedByJmxPropertyNotUpdatable", ScldConfig.getInstance().getDefaultConfigFileName(), null, configCommands, null);
		Assert.assertNotNull(configManagerWithDefaultConfigsOverridedByJmxPropertyNotUpdatable);
		Assert.assertEquals("Content-Length", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_CLEN_HEADER")); //default config
		
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_CLEN_HEADER", "Content-Length-New");
		
		Assert.assertEquals("Content-Length", configManagerWithDefaultConfigsOverridedByJmxPropertyNotUpdatable.getProperty("AFT_DME2_CLEN_HEADER")); // not overrided by Jmx because property is not updatable
	}
	
	
	@Test
	public void testConfigManagerWithFileBasedOverridedByJvm() throws ConfigException {
		ConfigurationManager configManagerWithFileBasedOverridedByJvm =  ConfigurationManager.getInstance("ConfigManagerWithFileBasedOverridedByJvm", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", null, null);
		Assert.assertNotNull(configManagerWithFileBasedOverridedByJvm);
		Assert.assertEquals("26000", configManagerWithFileBasedOverridedByJvm.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT")); //overrided by Jvm
		Assert.assertEquals("3", configManagerWithFileBasedOverridedByJvm.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT"));	//overrided by Jvm
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		configCommands.add(new JVMConfigurationStrategy());
		
		ConfigurationManager configManagerWithFileBasedOverridedByJvmCreatedByListOfCommands =  ConfigurationManager.getInstance("ConfigManagerWithFileBasedOverridedByJvmCreatedByListOfCommands", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithFileBasedOverridedByJvmCreatedByListOfCommands);
		Assert.assertEquals("26000", configManagerWithFileBasedOverridedByJvmCreatedByListOfCommands.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT")); //overrided by Jvm
		Assert.assertEquals("3", configManagerWithFileBasedOverridedByJvmCreatedByListOfCommands.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT"));	//overrided by Jvm
	}
	
	@Test
	public void testConfigManagerWithFileBasedOverridedByUserProp() throws ConfigException {
		
		PropertiesConfiguration userProp = new PropertiesConfiguration();
		userProp.setProperty("AFT_DME2_WS_ENABLE_TRACE_ROUTE", "true");
		
		ConfigurationManager configManagerWithFileBasedOverridedByUserProp =  ConfigurationManager.getInstance("ConfigManagerWithFileBasedOverridedByUserProp", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", null, null);
		Assert.assertNotNull(configManagerWithFileBasedOverridedByUserProp);
		ConfigurationManager configManagerWithFileBasedOverridedByUserPropOverrided =  ConfigurationManager.getInstance("ConfigManagerWithFileBasedOverridedByUserPropOverrided", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", null, userProp);
		Assert.assertNotNull(configManagerWithFileBasedOverridedByUserPropOverrided);
		
		Assert.assertEquals("false", configManagerWithFileBasedOverridedByUserProp.getProperty("AFT_DME2_WS_ENABLE_TRACE_ROUTE")); // FileBased		
		Assert.assertEquals("true", configManagerWithFileBasedOverridedByUserPropOverrided.getProperty("AFT_DME2_WS_ENABLE_TRACE_ROUTE")); //overrided by UserProp
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		configCommands.add(new UserPropertyConfigurationStrategy(userProp));
		
		ConfigurationManager configManagerWithFileBasedOverridedByUserPropOverridedCreatedByListOfCommands =  ConfigurationManager.getInstance("ConfigManagerWithFileBasedOverridedByUserPropOverridedCreatedByListOfCommands", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithFileBasedOverridedByUserPropOverridedCreatedByListOfCommands);
		
		Assert.assertEquals("true", configManagerWithFileBasedOverridedByUserPropOverridedCreatedByListOfCommands.getProperty("AFT_DME2_WS_ENABLE_TRACE_ROUTE")); //overrided by UserProp
		
	}
	
	//@Test
	public void testConfigManagerWithFileBasedOverridedByGrmPull() throws ConfigException { /*TBD */ }
	
	@Test
	public void testConfigManagerWithFileBasedOverridedByJmx() throws ConfigException { 
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		
		
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithFileBasedOverridedByJmx");
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithFileBasedOverridedByJmx =  ConfigurationManager.getInstance("ConfigManagerWithFileBasedOverridedByJmx", ScldConfig.getInstance().getDefaultConfigFileName(), null, configCommands, null);
		Assert.assertNotNull(configManagerWithFileBasedOverridedByJmx);
		Assert.assertEquals("1", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //default config
		
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "3");
		
		Assert.assertEquals("3", configManagerWithFileBasedOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //overrided by Jmx
		
		jxmConfigMBean.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", null);
		Assert.assertEquals("2", configManagerWithFileBasedOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); // removed from current configs since property value is null
	}
	
	
	@Test
	public void testConfigManagerWithFileBasedOverridedByJmxPropertyNotUpdatable() throws ConfigException { 
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		
		
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithFileBasedOverridedByJmxPropertyNotUpdatable");
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithFileBasedOverridedByJmxPropertyNotUpdatable =  ConfigurationManager.getInstance("ConfigManagerWithFileBasedOverridedByJmxPropertyNotUpdatable", ScldConfig.getInstance().getDefaultConfigFileName(), null, configCommands, null);
		Assert.assertNotNull(configManagerWithFileBasedOverridedByJmxPropertyNotUpdatable);
		Assert.assertEquals("1015,1011,1012,1013,1006", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS")); //default config
		
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS", "3");
		
		Assert.assertEquals("1115,1111,1112,1113,1116", configManagerWithFileBasedOverridedByJmxPropertyNotUpdatable.getProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS")); //not overrided by Jmx since property is not updatable
		
		jxmConfigMBean.setProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS", null);
		Assert.assertEquals("1115,1111,1112,1113,1116", configManagerWithFileBasedOverridedByJmxPropertyNotUpdatable.getProperty("AFT_DME2_FAILOVER_WS_CLOSE_CDS")); // not overrided by Jmx since property is not updatable
	}
	
	
	@Test
	public void testConfigManagerWithJvmOverridedByUserProp()  throws ConfigException {
		
		PropertiesConfiguration userProp = new PropertiesConfiguration();
		userProp.setProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT", "28000");
		userProp.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "11");
		
		ConfigurationManager configManagerWithJvmOverridedByUserProp =  ConfigurationManager.getInstance("ConfigManagerWithJvmOverridedByUserProp", ScldConfig.getInstance().getDefaultConfigFileName(), null, null, null);
		Assert.assertNotNull(configManagerWithJvmOverridedByUserProp);
		ConfigurationManager configManagerWithJvmOverridedByUserPropOverrided =  ConfigurationManager.getInstance("ConfigManagerWithJvmOverridedByUserPropOverrided", ScldConfig.getInstance().getDefaultConfigFileName(), null, null, userProp);
		Assert.assertNotNull(configManagerWithJvmOverridedByUserPropOverrided);
		Assert.assertEquals("26000", configManagerWithJvmOverridedByUserProp.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT")); // Jvm
		Assert.assertEquals("3", configManagerWithJvmOverridedByUserProp.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT"));	// Jvm
		Assert.assertEquals("28000", configManagerWithJvmOverridedByUserPropOverrided.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT")); // UserProp
		Assert.assertEquals("11", configManagerWithJvmOverridedByUserPropOverrided.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT"));	// UserProp
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new JVMConfigurationStrategy());
		configCommands.add(new UserPropertyConfigurationStrategy(userProp));
		
		ConfigurationManager configManagerWithJvmOverridedByUserPropOverridedCreatedByListOfCommands =  ConfigurationManager.getInstance("ConfigManagerWithJvmOverridedByUserPropOverridedListOfCommands", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithJvmOverridedByUserPropOverridedCreatedByListOfCommands);
		Assert.assertEquals("28000", configManagerWithJvmOverridedByUserPropOverridedCreatedByListOfCommands.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT")); // UserProp
		Assert.assertEquals("11", configManagerWithJvmOverridedByUserPropOverridedCreatedByListOfCommands.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT"));	// UserProp
		
	}
	
	//@Test
	public void testConfigManagerWithJvmOverridedByGrmPull()  throws ConfigException { /*TBD */ }
	
	@Test
	public void testConfigManagerWithJvmOverridedByJmx()  throws ConfigException { 
		
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithJvmOverridedByJmx");
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new JVMConfigurationStrategy());
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithJvmOverridedByJmx =  ConfigurationManager.getInstance("ConfigManagerWithJvmOverridedByJmx", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithJvmOverridedByJmx);
		
		Assert.assertEquals("1", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //default config
		Assert.assertEquals("3", configManagerWithJvmOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); // Jvm
		
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "7");
		
		Assert.assertEquals("7", configManagerWithJvmOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //overrided by Jmx
		
		jxmConfigMBean.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", null);
		Assert.assertEquals("3", configManagerWithJvmOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); // removed from current configs since property value is null
	}

	
	@Test
	public void testConfigManagerWithJvmOverridedByJmxPropertyNotUpdatable()  throws ConfigException { 
		
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithJvmOverridedByJmxPropertyNotUpdatable");
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new JVMConfigurationStrategy());
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithJvmOverridedByJmx =  ConfigurationManager.getInstance("ConfigManagerWithJvmOverridedByJmxPropertyNotUpdatable", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithJvmOverridedByJmx);
		
		Assert.assertEquals("200,401", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); //default config
		Assert.assertEquals("300,501", configManagerWithJvmOverridedByJmx.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); // Jvm
		
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS", "311,511");
		
		Assert.assertEquals("300,501", configManagerWithJvmOverridedByJmx.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); //overrided by Jmx
		
		jxmConfigMBean.setProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS", null);
		Assert.assertEquals("300,501", configManagerWithJvmOverridedByJmx.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); // not removed from current configs since property is not updatable
	}

	//@Test
	public void testConfigManagerWithUserPropOverridedByGrmPull()  throws ConfigException { /*TBD */ }
	
	@Test
	public void testConfigManagerWithUserPropOverridedByJmx()  throws ConfigException { 
		
		PropertiesConfiguration userProp = new PropertiesConfiguration();
		userProp.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "11");
		
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithUserPropOverridedByJmx");
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new UserPropertyConfigurationStrategy(userProp));
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithUserPropOverridedByJmx =  ConfigurationManager.getInstance("ConfigManagerWithUserPropOverridedByJmx", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithUserPropOverridedByJmx);
		Assert.assertEquals("11", configManagerWithUserPropOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT"));	// UserProp
	
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "20");
		
		Assert.assertEquals("20", configManagerWithUserPropOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); //overrided by Jmx
		
		jxmConfigMBean.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", null);
		Assert.assertEquals("11", configManagerWithUserPropOverridedByJmx.getProperty("AFT_DME2_WS_MAX_RETRY_COUNT")); // removed from current configs since property value is null
	}
	
	@Test
	public void testConfigManagerWithUserPropOverridedByJmxPropertyNotUpdatable()  throws ConfigException { 
		
		PropertiesConfiguration userProp = new PropertiesConfiguration();
		userProp.setDelimiterParsingDisabled(true);
		userProp.setProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS", "300,501");
		
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithUserPropOverridedByJmxPropertyNotUpdatable");
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		UserPropertyConfigurationStrategy userPropertyConfigurationStrategy = new UserPropertyConfigurationStrategy(userProp);
		configCommands.add(userPropertyConfigurationStrategy);
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithUserPropOverridedByJmx =  ConfigurationManager.getInstance("ConfigManagerWithUserPropOverridedByJmxPropertyNotUpdatable", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configManagerWithUserPropOverridedByJmx);
		Assert.assertEquals("300,501", configManagerWithUserPropOverridedByJmx.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS"));	// UserProp
	
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS", "20");
		
		Assert.assertEquals("300,501", configManagerWithUserPropOverridedByJmx.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); //overrided by Jmx
		
		jxmConfigMBean.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", null);
		Assert.assertEquals("300,501", configManagerWithUserPropOverridedByJmx.getProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS")); // not removed from current configs since property is not updatable
	}

	//@Test
	public void testConfigManagerWithGrmPullOverridedByJmx()  throws ConfigException { /*TBD */ }

	@Test
	public void testConfigManagerWithDefaultConfigOverridedByFileJvmUserPropJmx()  throws ConfigException { 
		
		PropertiesConfiguration userProp = new PropertiesConfiguration();
		userProp.setProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT", "27000");
		
			
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		
		ConfigurationManager configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx1 =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigOverridedByFileJvmUserPropJmx1", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		configCommands.add(new JVMConfigurationStrategy());
		
		ConfigurationManager configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx2 =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigOverridedByFileJvmUserPropJmx2", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		configCommands.add(new UserPropertyConfigurationStrategy(userProp));
		
		ConfigurationManager configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx3 =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigOverridedByFileJvmUserPropJmx3", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		
		JMXConfigurationStrategy jmxConfig = new JMXConfigurationStrategy("ConfigManagerWithDefaultConfigOverridedByFileJvmUserPropJmx4");
		configCommands.add(jmxConfig);
		
		ConfigurationManager configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx4 =  ConfigurationManager.getInstance("ConfigManagerWithDefaultConfigOverridedByFileJvmUserPropJmx4", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		JMXConfigBeanInf jxmConfigMBean = jmxConfig.getJMXConfigBean();
		jxmConfigMBean.setProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT", "28000");
		
		
		
		Assert.assertEquals("24000", ConfigurationManager.getInstance(ConfigConstants.DEFAULT_CONFIG_MANAGER_NAME).getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT"));	// Default Config - 24000
		Assert.assertEquals("25000", configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx1.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT"));  					// File Based - 25000
		Assert.assertEquals("26000", configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx2.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT"));  					// Jvm Based - 26000
		Assert.assertEquals("27000", configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx3.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT"));  					// UserProp Based - 27000
		Assert.assertEquals("28000", configManagerWithDefaultConfigOverridedByFileJvmUserPropJmx4.getProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT"));  					// Jmx Based - 28000
	}	
}
