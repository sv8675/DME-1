package com.att.aft.dme2.util;

import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;

public class ServiceValidationHelper {

	public static void validateServiceStringIsNonJDBCURL(String inStr) throws DME2Exception {
		/* If the input URL string is a JDBC URL, throw an Exception. DME2Client and JMS provider do not accept JDBC URLs */

		if (inStr.contains("/driver=")) {
			throw new DME2Exception("AFT-DME2-9706", new ErrorContext().add("URL String", inStr));
		}
	}
	
	
	public static void validateJDBCEndpointRequiredFields(Properties props, String serviceURI) throws DME2Exception {
		/* If database name is not provided, throw exception */
		if(props.getProperty(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME) == null) {
			throw new DME2Exception("AFT-DME2-0614", new ErrorContext().add("URL String", serviceURI));
		}
		
		/* If health check user and password are provide, then health check driver MUST be present. If not, throw exception */
		if(props.getProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER) != null && props.getProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_PASSWORD) != null) {
			if(props.getProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER) == null) {
				throw new DME2Exception("AFT-DME2-0615", new ErrorContext().add("URL String", serviceURI));
			}
		}
	}
}
