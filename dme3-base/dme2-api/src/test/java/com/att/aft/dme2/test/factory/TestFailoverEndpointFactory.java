/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.factory;

import org.junit.Assert;
import org.junit.Test;

import com.att.aft.dme2.api.DME2DefaultFailoverEndpoint;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.FailoverEndpointFactory;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverEndpoint;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/*Test class for FailoverEndpointFactory
 * Gets the singleton instance of the factory and retrieves FailoverEndpoint Handler implementation class as specified in properties file
 * Checks for AsserNotnull for the retrieved object and also if the FailoverEndpoint Handler implementation is an instance of DME2DefaultFailoverHandler
 */
public class TestFailoverEndpointFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestFailoverEndpointFactory.class.getName());

	@Test
	public void test() {
		FailoverEndpoint failoverEndpoint = null;
		DME2Configuration configuration = new DME2Configuration();
		try {

			failoverEndpoint = FailoverEndpointFactory.getFailoverEndpointHandler(configuration);
			Assert.assertNotNull(failoverEndpoint);
			Assert.assertTrue(failoverEndpoint instanceof DME2DefaultFailoverEndpoint);
		} catch (DME2Exception e) {
			LOGGER.error(null, DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION
					+ DME2DefaultFailoverEndpoint.class.getName(), DME2Constants.EXCEPTION_HANDLER_MSG, e);
		}
	}

}
