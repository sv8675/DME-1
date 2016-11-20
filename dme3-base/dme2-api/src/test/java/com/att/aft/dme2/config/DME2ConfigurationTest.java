/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.scld.config.dto.ScldConfig;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.strategy.ConfigurationStrategy;
import com.att.aft.scld.config.strategy.FileConfigurationStrategy;
import com.google.common.collect.Lists;

public class DME2ConfigurationTest {

	static {
		System.setProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS", "300,501");
		System.setProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT", "26000");
		System.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "3");
    }

		
	@Test
	public void testCreationOfConfigManagerDefaultConfigFileNotFound() throws ConfigException {
		DME2Configuration configurationManager =  new DME2Configuration("DefaultFileNotFound", "Unknow_default_configs.properties", null, null, null);
		Assert.assertNotNull(configurationManager);
	}
	
	@Test
	public void testCreationOfConfigManagerFileStrategyFileNotFound() throws ConfigException {
		DME2Configuration configurationManager =  new DME2Configuration("FileStrategyFileNotFound", ScldConfig.getInstance().getDefaultConfigFileName(), "Unknow_file_strategy.properties", null, null);
		Assert.assertNotNull(configurationManager);
	}
	
	@Test
	public void testCreationOfConfigManagerDefaultConfig() {
		DME2Configuration config = new DME2Configuration();
		Assert.assertNotNull(config);
	}
	
	@Test
	public void testCreationOfDefaultConfigManager() throws ConfigException {
		DME2Configuration defaultDME2Configuration =  new DME2Configuration(DME2Configuration.DME2_DEFAULT_CONFIG_FILE_NAME);
		Assert.assertNotNull(defaultDME2Configuration);
	}
	
	@Test
	public void testCreationOfMultipleConfigManagers() throws ConfigException {
		DME2Configuration configurationManagerWithAllVariables1 =  new DME2Configuration("ConfigurationManagerWithAllVariables1", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", new ArrayList<ConfigurationStrategy>(), new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithAllVariables1);
		DME2Configuration configurationManagerWithAllVariables2 =  new DME2Configuration("ConfigurationManagerWithAllVariables2", ScldConfig.getInstance().getDefaultConfigFileName(), "fileBasedConfigs.properties", new ArrayList<ConfigurationStrategy>(), new PropertiesConfiguration());
		Assert.assertNotNull(configurationManagerWithAllVariables2);
	}
	
	@Test //Default Configs 
	public void testConfigManagerWithDefaultConfigs() throws ConfigException {
		
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		
		DME2Configuration configurationManagerWithDefaultConfig =  new DME2Configuration("ConfigurationManagerWithDefaultConfig", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		Assert.assertNotNull(configurationManagerWithDefaultConfig);
		assertEquals( "false",
        configurationManagerWithDefaultConfig.getProperty( DME2Constants.AFT_DME2_DISABLE_INGRESS_REPLY_STREAM ) ); //default config
		assertEquals( "lrmRName", configurationManagerWithDefaultConfig.getProperty( "AFT_DME2_CONTAINER_NAME_KEY" ) ); //default config
		assertEquals( "lrmRVer", configurationManagerWithDefaultConfig.getProperty( "AFT_DME2_CONTAINER_VERSION_KEY" ) ); //default config
		assertEquals( "lrmRO", configurationManagerWithDefaultConfig.getProperty( "AFT_DME2_CONTAINER_ROUTEOFFER_KEY" ) ); //default config
//		Assert.assertEquals("DEV", configurationManagerWithDefaultConfig.getProperty("AFT_DME2_CONTAINER_ENV_KEY")); //default config
	}
	
	@Test 
	public void testConfigManagerWithDefaultConfigsWithPipe() throws ConfigException {	
		List<ConfigurationStrategy> configCommands = Lists.newArrayList();
		configCommands.add(new FileConfigurationStrategy("fileBasedConfigs.properties"));
		DME2Configuration configurationManagerWithDefaultConfig =  new DME2Configuration("ConfigurationManagerWithDefaultConfigWithPipe", ScldConfig.getInstance().getDefaultConfigFileName(), configCommands);
		assertEquals( "Content-Disposition: form-data; name=\"upload_file\"; filename=",
        configurationManagerWithDefaultConfig.getProperty( "AFT_DME2_CONTENT_DISP_HEADER" ) ); //default config
	}
	
	@Test 
	public void testOveridenConfigParams() throws ConfigException {	
		DME2Configuration configurationManagerWithDefaultConfig =  new DME2Configuration("ConfigurationManagerWithDefaultConfigWithPipe", "fileBasedConfigs.properties");
		assertEquals( "Content-Disposition: form-data; name=\"upload_file\"; filename=",
        configurationManagerWithDefaultConfig.getProperty( "AFT_DME2_CONTENT_DISP_HEADER" ) ); //from supplied config file
		assertEquals( "DME2FS", configurationManagerWithDefaultConfig.getProperty( "DME2_EP_REGISTRY_CLASS" ) ); //from DME default config

	}

  /**
   * Create a random property name and value and try to override the initial null value
   */
  @Test
  public void testOverrideConfigParam() {
    // Random property name
    String RANDOM_PROPERTY_NAME = "AFT_DME2_PREFERRED_ROUTEOFFER";//RandomStringUtils.randomAlphanumeric( 10 );

    // Random property value
    String RANDOM_PROPERTY_VALUE = RandomStringUtils.randomAlphanumeric( 20 );

    DME2Configuration configuration = new DME2Configuration( );
    String initialValue = configuration.getProperty( RANDOM_PROPERTY_NAME );

    // Override the config value
    configuration.setOverrideProperty( RANDOM_PROPERTY_NAME, RANDOM_PROPERTY_VALUE  );

    // This should be RANDOM_PROPERTY_VALUE
    String changedValue = configuration.getProperty( RANDOM_PROPERTY_NAME );

    assertEquals( RANDOM_PROPERTY_VALUE, changedValue );
    assertNotEquals( RANDOM_PROPERTY_VALUE, initialValue );

    // Set it back, just in case
    configuration.setOverrideProperty( RANDOM_PROPERTY_NAME, initialValue  );

  }
}
