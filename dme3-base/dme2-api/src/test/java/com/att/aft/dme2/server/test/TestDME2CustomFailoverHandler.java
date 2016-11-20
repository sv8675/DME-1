/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import com.att.aft.dme2.api.DME2Response;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverHandler;

public class TestDME2CustomFailoverHandler implements FailoverHandler{
	
	public TestDME2CustomFailoverHandler() {	
	}
	
	public TestDME2CustomFailoverHandler(DME2Configuration config) {	
	}

	@Override
	public boolean isFailoverRequired(DME2Response dme2Response) {
		// TODO Auto-generated method stub
		return false;
	}

}
