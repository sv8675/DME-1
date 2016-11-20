/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.handler;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.FailoverEndpointFactory;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.server.test.TestConstants;

public class TestDME2DefaultFailoverEndpoint {

	@Test
	public void test() {
		
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("DME2.DEBUG", "true");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		System.setProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON", "true");
		
		DME2Configuration configuration = new DME2Configuration();
		String serviceURI = "dme2://DME2SEARCH/service=com.att.aft.dme2.test.TestDME2ClientWithDME2Protocol/version=1.0.0/envContext=LAB/partner=TARGET";

		try {

			DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory.getDefaultEndpointIteratorBuilder(configuration)
					.setServiceURI(serviceURI).setProps(new Properties()).setManager(new DME2Manager()).setUrlFormatter(null)
					.build();
			DME2EndpointReference orderedEndpointHolder = FailoverEndpointFactory
					.getFailoverEndpointHandler(configuration).getNextFailoverEndpoint(endpointIterator, false);
			Assert.assertNull(orderedEndpointHolder);
		} catch (DME2Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.clearProperty("AFT_ENVIRONMENT");
			System.clearProperty("AFT_LATITUDE");
			System.clearProperty("AFT_LONGITUDE");
			System.clearProperty("DME2.DEBUG");
			System.clearProperty("platform");
			System.clearProperty("AFT_DME2_HTTP_EXCHANGE_TRACE_ON");
		}
	}
}