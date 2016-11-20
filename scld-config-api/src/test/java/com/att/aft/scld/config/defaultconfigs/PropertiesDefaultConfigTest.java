/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.defaultconfigs;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.att.aft.scld.config.dto.Config;
import com.att.aft.scld.config.dto.Config.ConfigType;
import com.att.aft.scld.config.dto.ScldConfig;
import com.att.aft.scld.config.exception.ConfigException;

public class PropertiesDefaultConfigTest {

	
	@Test
	public void testLoadDefaultConfigsSuccess() throws ConfigException {
		PropertiesDefaultConfig config = new PropertiesDefaultConfig();
		Map<String, Config> defaultConfigs = config.loadDefaultConfigs(ScldConfig.getInstance().getDefaultConfigFileName());
		Assert.assertNotNull(defaultConfigs);
	}
	
	
	private void assertConfig(Config expected, Config actual) {
		Assert.assertEquals(expected.getDefaultValue(), actual.getDefaultValue());
		Assert.assertEquals(expected.getType(), actual.getType());
		Assert.assertEquals(expected.isUpdatable(), actual.isUpdatable());
		
	}
	
	@Test
	public void testLoadDefaultConfigsSuccessAllProperties() throws ConfigException {
		PropertiesDefaultConfig config = new PropertiesDefaultConfig();
		Map<String, Config> defaultConfigs = config.loadDefaultConfigs(ScldConfig.getInstance().getDefaultConfigFileName());
		Assert.assertNotNull(defaultConfigs);
		
		assertConfig(new Config("false", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_DISABLE_INGRESS_REPLY_STREAM"));
		assertConfig(new Config("lrmRName", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_NAME_KEY"));
		assertConfig(new Config("lrmRVer", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_VERSION_KEY"));
		assertConfig(new Config("lrmRO", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_ROUTEOFFER_KEY"));
		assertConfig(new Config("lrmEnv", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_ENV_KEY"));
		assertConfig(new Config("platform", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_PLATFORM_KEY"));
		assertConfig(new Config("SCLD_PLATFORM", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY"));
		assertConfig(new Config("lrmHost", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_HOST_KEY"));
		assertConfig(new Config("Pid", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_PID_KEY"));
		assertConfig(new Config("\r\n", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CRLF"));
		assertConfig(new Config("Content-Disposition: form-data; name=\"upload_file\"; filename=", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTENT_DISP_HEADER"));
		assertConfig(new Config("Content-Type: application/octet-stream", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_MULTIPART_TYPE"));
		assertConfig(new Config("multipart/form-data;boundary=", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_MULTIPART_CTYPE"));
		assertConfig(new Config("Content-Type", ConfigType.APP, true), defaultConfigs.get("AFT_DME2_CTYPE_HEADER"));
		assertConfig(new Config("Content-Length", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CLEN_HEADER"));
		assertConfig(new Config("200,401", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_NON_FAILOVER_HTTP_SCS"));
		assertConfig(new Config("TRUE", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_LOOKUP_NON_FAILOVER_SC"));
		assertConfig(new Config("FALSE", ConfigType.APP, true), defaultConfigs.get("CHECK_DATA_PARTITION_RANGE_FIRST"));
		assertConfig(new Config("/service=,/subContext=", ConfigType.SYSTEM, false), defaultConfigs.get("DME2_URI_FIELD_WITH_PATH_SEP"));
		assertConfig(new Config("FALSE", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_SKIP_SERVICE_URI_VALIDATION"));
		assertConfig(new Config("AFT-DME2-0902,GRMSVC-9*", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_GRM_FAILOVER_ERROR_CODES"));
		assertConfig(new Config("1015,1011,1012,1013,1006", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_FAILOVER_WS_CLOSE_CDS"));
		assertConfig(new Config("1011,1012,1013", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_RETRY_WS_CLOSE_CDS"));
		assertConfig(new Config("24000", ConfigType.APP, true), defaultConfigs.get("AFT_DME2_DEF_WS_IDLE_TIMEOUT"));
		assertConfig(new Config("1", ConfigType.SYSTEM, true), defaultConfigs.get("AFT_DME2_WS_MAX_RETRY_COUNT"));
		assertConfig(new Config("true", ConfigType.APP, true), defaultConfigs.get("AFT_DME2_WS_ENABLE_TRACE_ROUTE"));
		assertConfig(new Config(".", ConfigType.SYSTEM, false), defaultConfigs.get("DME2_DOMAIN_SEP"));
		assertConfig(new Config("~", ConfigType.SYSTEM, false), defaultConfigs.get("DME2_RO_SEP"));
		assertConfig(new Config(".", ConfigType.SYSTEM, false), defaultConfigs.get("DME2_NAME_SEP"));
		assertConfig(new Config("-", ConfigType.SYSTEM, false), defaultConfigs.get("DME2_PORT_RANGE_SEP"));
		assertConfig(new Config(",", ConfigType.SYSTEM, false), defaultConfigs.get("DME2_PORT_SEP"));
		assertConfig(new Config("dummyContainerIID", ConfigType.SYSTEM, false), defaultConfigs.get("lrmiid"));
		assertConfig(new Config("dummyContainer", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_CONTAINER_RN"));
		assertConfig(new Config("DEFAULT", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_DEFAULT_RO"));
		assertConfig(new Config("dummyLRMInstance", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_LRM_INST"));
		assertConfig(new Config("FALSE", ConfigType.SYSTEM, false), defaultConfigs.get("DME2.DEBUG"));
		assertConfig(new Config("TRUE", ConfigType.SYSTEM, false), defaultConfigs.get("AFT_DME2_ALLOW_EMPTY_SEP_GRM"));
		assertConfig(new Config("Ash}|ok", ConfigType.APP, true), defaultConfigs.get("DME2_DOMAIN_TEST_PIPE"));
		
	}
	
	@Test(expected = ConfigException.class)
	public void testLoadDefaultConfigsFailure() throws ConfigException {
		PropertiesDefaultConfig config = new PropertiesDefaultConfig();
		Map<String, Config> defaultConfigs = config.loadDefaultConfigs("unknwo");
		Assert.assertNull(defaultConfigs);
	}
	
}
