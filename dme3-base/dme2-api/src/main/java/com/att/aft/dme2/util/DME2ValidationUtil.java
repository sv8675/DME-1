package com.att.aft.dme2.util;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;

public class DME2ValidationUtil {

	public static void validateServiceStringFormat(DME2Configuration configuration, String inStr) {
		if (configuration.getProperty(DME2Constants.AFT_DME2_SKIP_SERVICE_URI_VALIDATION) != null) {
			return;
		}

		Pattern pattern = Pattern
				.compile("/service=[A-Za-z0-9\\.]+/version=[0-9]+.[0-9]+.[A-Za-z0-9_-]+/envContext=[A-Z]+/routeOffer=[A-Za-z0-9]+(\\?clientSupportedVersions=[0-9]+.[0-9]+,[0-9]+.[0-9]+)?");
		Matcher matcher = pattern.matcher(inStr);

		if (!matcher.matches()) {
			throw new RuntimeException(
					"DME2.Validation.Exception; Context path is not properly formated. Correct format is: /service=[SERVICE_NAME]/version=[VERSION]/envContext=[ENV]/routeOffer=[ROUTE_OFFER]<?clientSupportedVersions=[X.X,X.X]>");
		}
	}

	public static void validateVersionFormat(DME2Configuration configuration, String inStr) {
		if (configuration.getProperty(DME2Constants.AFT_DME2_SKIP_SERVICE_URI_VALIDATION) != null) {
			return;
		}

		Pattern pattern = Pattern.compile("[0-9]+.[0-9]+.[A-Za-z0-9_-]+");
		Matcher matcher = pattern.matcher(inStr);

		if (!matcher.matches()) {
			throw new RuntimeException(
					"DME2.Validation.Exception; Version string is not properly formated. Correct format is: [MAJOR].[MINOR].[PATCH]");
		}
	}

	public static void validateServiceStringIsNonJDBCURL(String inStr)
			throws DME2Exception {
		/*
		 * If the input URL string is a JDBC URL, throw an Exception. DME2Client
		 * and JMS provider do not accept JDBC URLs
		 */

		if (inStr.contains("/driver=")) {
			throw new DME2Exception("AFT-DME2-9706", new ErrorContext().add(
					"URL String", inStr));
		}
	}

	public static void validateJDBCEndpointRequiredFields(Properties props,
			String serviceURI) throws DME2Exception {
		/* If database name is not provided, throw exception */
		if (props.getProperty(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME) == null) {
			throw new DME2Exception("AFT-DME2-0614", new ErrorContext().add(
					"URL String", serviceURI));
		}

		/*
		 * If health check user and password are provide, then health check
		 * driver MUST be present. If not, throw exception
		 */
		if (props.getProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER) != null
				&& props.getProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_PASSWORD) != null) {
			if (props
					.getProperty(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER) == null) {
				throw new DME2Exception("AFT-DME2-0615",
						new ErrorContext().add("URL String", serviceURI));
			}
		}
	}
}
