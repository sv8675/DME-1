/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.util;

import junit.framework.Assert;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.util.ConfigUtil;

public class ConfigUtilTest {
	
	@Test
	public void testGetPropertiesConfigurationSuccessFileFound() throws ConfigException {
		PropertiesConfiguration propConfigs = ConfigUtil.getPropertiesConfiguration("fileBasedConfigs.properties");
		Assert.assertNotNull(propConfigs);
		Assert.assertEquals("1115,1111,1112,1113,1116", propConfigs.getString("AFT_DME2_FAILOVER_WS_CLOSE_CDS"));
		Assert.assertEquals("1111,1112,1113", propConfigs.getString("AFT_DME2_RETRY_WS_CLOSE_CDS"));
		Assert.assertEquals("25000", propConfigs.getString("AFT_DME2_DEF_WS_IDLE_TIMEOUT"));
		Assert.assertEquals("2", propConfigs.getString("AFT_DME2_WS_MAX_RETRY_COUNT"));
		Assert.assertEquals("false", propConfigs.getString("AFT_DME2_WS_ENABLE_TRACE_ROUTE"));
		Assert.assertNull(propConfigs.getString("unknownField"));
	}
	
	@Test
	public void testGetPropertiesConfigurationSuccessFileFoundLargeFile() throws ConfigException {
		PropertiesConfiguration propConfigs = ConfigUtil.getPropertiesConfiguration("TestConfigs_LargeFile.properties");
		Assert.assertNotNull(propConfigs);
		Assert.assertNull(propConfigs.getString("unknownField"));
		for(int i = 1; i <= 5000 ; i++) {
			Assert.assertEquals("localhost" + i, propConfigs.getString("dbName" + i));
		}
	}
	
	@Test(expected = ConfigException.class)
	public void testGetPropertiesConfigurationForFailureFileNotFound() throws ConfigException {
		PropertiesConfiguration propConfigs = ConfigUtil.getPropertiesConfiguration("UnknowConfig.properties");
		Assert.assertNull(propConfigs);
	}
}
