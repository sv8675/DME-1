/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.factory;

import org.junit.Assert;
import org.junit.Test;

import com.att.aft.dme2.api.DME2DefaultFailoverHandler;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.FailoverFactory;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/*Test class for FailoverFactory
 * Gets the singleton instance of the factory and retrieves Failover Handler implementation class as specified in properties file
 * Checks for AsserNotnull for the retrieved object and also if the Failover Handler implementation is an instance of DME2DefaultFailoverHandler
 */
public class TestFailoverFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestFailoverFactory.class.getName());

	@Test
	public void test() {
		FailoverHandler failoverHandler = null;
		DME2Configuration configuration = new DME2Configuration();
		try {

			failoverHandler = FailoverFactory.getFailoverHandler(configuration);
			Assert.assertNotNull(failoverHandler);
			Assert.assertTrue(failoverHandler instanceof DME2DefaultFailoverHandler);
		} catch (DME2Exception e) {
			LOGGER.error(null, DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION
					+ DME2DefaultFailoverHandler.class.getName(), DME2Constants.EXCEPTION_HANDLER_MSG, e);
		}
	}

}
