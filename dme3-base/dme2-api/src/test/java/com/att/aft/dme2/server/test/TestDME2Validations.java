/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2ValidationUtil;

import junit.framework.TestCase;

public class TestDME2Validations extends TestCase
{

	public void testValidateServiceURIString()
	{
		try
		{
			String s1 = "/service=com.att.aft.dme2.test.TestValidateServiceURI/version=1.0.0/envContext=LAB/routeOffer=DEFAULT";
			String s2 = "/service=com.att.aft.dme2.test.TestValidateServiceURI/version=1.0.PATCH/envContext=LAB/routeOffer=DEFAULT";
			String s3 = "/service=com.att.aft.dme2.test.TestValidateServiceURI/version=1.0.UNDER_SCORE/envContext=LAB/routeOffer=DEFAULT";
			String s4 = "/service=com.att.aft.dme2.test.TestValidateServiceURI/version=1.0.DASH-/envContext=LAB/routeOffer=DEFAULT";
			String s5 = "/service=com.att.aft.dme2.test.TestValidateServiceURI/version=84.0.656/envContext=LAB/routeOffer=DEFAULT";
			
			DME2Configuration config = new DME2Configuration();
			System.out.println("Validating serviceURI: " + s1);
			DME2ValidationUtil.validateServiceStringFormat(config, s1);
			System.out.println("Validation of serviceURI successful: " + s1 + "\n");
			
			System.out.println("Validating serviceURI: " + s2);
			DME2ValidationUtil.validateServiceStringFormat(config, s2);
			System.out.println("Validation of serviceURI successful: " + s2 + "\n");
			
			System.out.println("Validating serviceURI: " + s3);
			DME2ValidationUtil.validateServiceStringFormat(config, s3);
			System.out.println("Validation of serviceURI successful: " + s3 + "\n");
			
			System.out.println("Validating serviceURI: " + s4);
			DME2ValidationUtil.validateServiceStringFormat(config, s4);
			System.out.println("Validation of serviceURI successful: " + s4 + "\n");
			
			System.out.println("Validating serviceURI: " + s5);
			DME2ValidationUtil.validateServiceStringFormat(config, s5);
			System.out.println("Validation of serviceURI successful: " + s5 + "\n");
		}
		catch (Exception e)
		{
			fail("Validation for ServiceURI string failed.");
		}
	}
	
	
	public void testValidateServiceVersionString()
	{
		try
		{
			String s1 = "1.3.5";
			String s2 = "1.1.PATCH";
			String s3 = "1.0.UNDER_SCORE";
			String s4 = "1.0.DASH-";
			String s5 = "1000.1000.656";
			
			DME2Configuration config = new DME2Configuration();

			System.out.println("Validating version string: " + s1);
			DME2ValidationUtil.validateVersionFormat(config, s1);
			System.out.println("Validation of version string successful: " + s1 + "\n");
			
			System.out.println("Validating version string: " + s2);
			DME2ValidationUtil.validateVersionFormat(config, s2);
			System.out.println("Validation of version string successful: " + s2 + "\n");
			
			System.out.println("Validating version string: " + s3);
			DME2ValidationUtil.validateVersionFormat(config, s3);
			System.out.println("Validation of version string successful: " + s3 + "\n");
			
			System.out.println("Validating version string: " + s4);
			DME2ValidationUtil.validateVersionFormat(config, s4);
			System.out.println("Validation of version string successful: " + s4 + "\n");
			
			System.out.println("Validating version string: " + s5);
			DME2ValidationUtil.validateVersionFormat(config, s5);
			System.out.println("Validation of version string successful: " + s5 + "\n");
			
			
			
			
		}
		catch (Exception e)
		{
			fail("Validation for version string failed.");
		}
	}
	
	public void testValidateServiceVersionString_DisableValidation()
	{
		System.setProperty("AFT_DME2_SKIP_SERVICE_URI_VALIDATION","true");
		DME2Configuration config = new DME2Configuration();

		try
		{
			String s1 = "A1.B3.@#^&5";
			System.out.println("Validating version string: " + s1);
			DME2ValidationUtil.validateVersionFormat(config, s1);
			System.out.println("Validation of version string skipped: " + s1 + "\n");
		}
		finally
		{
			System.clearProperty("AFT_DME2_SKIP_SERVICE_URI_VALIDATION");
		}
		
	}
	
}
