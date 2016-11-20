/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.jmx;

import javax.management.NotCompliantMBeanException;

import junit.framework.Assert;

import org.junit.Test;

import com.att.aft.scld.config.ConfigurationManager;
import com.att.aft.scld.config.dto.ScldConfig;
import com.att.aft.scld.config.exception.ConfigException;

public class JxmConfigMBeanTest {

	static {
		System.setProperty("AFT_DME2_NON_FAILOVER_HTTP_SCS", "300,501");
		System.setProperty("AFT_DME2_DEF_WS_IDLE_TIMEOUT", "26000");
		System.setProperty("AFT_DME2_WS_MAX_RETRY_COUNT", "3");
    }
	
	@Test
	public void testJxmConfigMBeanSetPropertySuccess() throws NotCompliantMBeanException, ConfigException {
		ConfigurationManager configManagerSet = ConfigurationManager.getInstance("configManagerSet", ScldConfig.getInstance().getDefaultConfigFileName());
		JxmConfigMBean jxmMBean = new JxmConfigMBean("configManagerSet");
		Assert.assertEquals("true", jxmMBean.setProperty("CHECK_DATA_PARTITION_RANGE_FIRST", "true"));
		Assert.assertEquals("true", configManagerSet.getProperty("CHECK_DATA_PARTITION_RANGE_FIRST"));
	}
	
	@Test
	public void testJxmConfigMBeanGetPropertySuccess() throws NotCompliantMBeanException, ConfigException {
		ConfigurationManager configManagerGet = ConfigurationManager.getInstance("configManagerGet", ScldConfig.getInstance().getDefaultConfigFileName());
		JxmConfigMBean jxmMBean = new JxmConfigMBean("configManagerGet");
		jxmMBean.setProperty("CHECK_DATA_PARTITION_RANGE_FIRST", "true");
		Assert.assertEquals("true", jxmMBean.getProperty("CHECK_DATA_PARTITION_RANGE_FIRST"));
		Assert.assertEquals("true", configManagerGet.getProperty("CHECK_DATA_PARTITION_RANGE_FIRST"));
	}
	
	@Test
	public void testJxmConfigMBeanSetPropertyFailure() throws NotCompliantMBeanException, ConfigException {
		ConfigurationManager configManagerSetF = ConfigurationManager.getInstance("configManagerSetF", ScldConfig.getInstance().getDefaultConfigFileName());
		JxmConfigMBean jxmMBean = new JxmConfigMBean("configManagerSetF");
		Assert.assertNull(jxmMBean.setProperty("CHECK_DATA_PARTITION_RANGE_FIRST_UNKNOWN", "true"));
		Assert.assertNull(configManagerSetF.getProperty("CHECK_DATA_PARTITION_RANGE_FIRST_UNKNOWN"));
	}
	
	@Test
	public void testJxmConfigMBeanGetPropertyFailure() throws NotCompliantMBeanException, ConfigException {
		ConfigurationManager configManagerGetF = ConfigurationManager.getInstance("configManagerGetF", ScldConfig.getInstance().getDefaultConfigFileName());
		@SuppressWarnings("unused")
		JxmConfigMBean jxmMBean = new JxmConfigMBean("configManagerGetF");
		Assert.assertNull(configManagerGetF.getProperty("CHECK_DATA_PARTITION_RANGE_FIRST_UNKNOWN"));
	}	
}
