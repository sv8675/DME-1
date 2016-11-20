/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.jmx;

import com.att.aft.scld.config.ConfigurationManager;
import com.att.aft.scld.config.exception.ConfigException;

public class JConsoleManualTest {

	public static void main(String[] args) throws ConfigException, InterruptedException {
		ConfigurationManager localTestConfigManager = ConfigurationManager.getInstance("localTestConfigManager");
		System.out.println("============= CHECK_DATA_PARTITION_RANGE_FIRST: " + localTestConfigManager.getProperty("CHECK_DATA_PARTITION_RANGE_FIRST"));
		
		//Open the jconsole and run the setProperty operation
		Thread.sleep(120000);
		System.out.println("============= CHECK_DATA_PARTITION_RANGE_FIRST: " + ConfigurationManager.getInstance("localTestConfigManager").getProperty("CHECK_DATA_PARTITION_RANGE_FIRST"));
	}

}
