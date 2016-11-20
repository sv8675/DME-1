/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.event.test.mbean;

import java.util.Arrays;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.event.DME2ServiceStatManager;

public class Test implements TestMBean {		
	
	@Override
	public String diagnostics() throws Exception{
		DME2Configuration config = new DME2Configuration();
		return Arrays.toString(DME2ServiceStatManager.getInstance(config).diagnostics());
	}
}
