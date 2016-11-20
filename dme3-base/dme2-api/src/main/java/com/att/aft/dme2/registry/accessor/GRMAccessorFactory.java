package com.att.aft.dme2.registry.accessor;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2ParameterNames;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;

/* Factory class to return the Default GRMAccessor Handler specified in properties file.
 * Singleton class and returns only one instance of the Handler.
 */
public class GRMAccessorFactory {

	private static String grmAccessorHandlerClassName = null;

	private static Map<DME2Configuration, BaseAccessor> grmAccessorHandlerMap = new ConcurrentHashMap<DME2Configuration, BaseAccessor>();

	private static final Logger logger = LoggerFactory.getLogger(GRMAccessorFactory.class.getName());
	private volatile static GRMAccessorFactory instance;
	private static String grmEnvironmentDNS;
	private static IGRMEndPointDiscovery endPointDiscovery;


	/* Static 'instance' method */
	public static GRMAccessorFactory getInstance() {
		GRMAccessorFactory result = instance;
		if (result == null) {
			synchronized (GRMAccessorFactory.class) {
				result = instance;
				if (result == null) {
					instance = result = new GRMAccessorFactory();
				}
			}
		}
		return result;
	}

	/**
	 * Not thread safe
	 */
	public static void close() {
		endPointDiscovery.close();
		getInstance().resetGrmAccessorHandler();
		instance = null;
	}
	/*
	 * This method creates instance of GRMAccessor handler implementation class.
	 * It invokes parameterized constructor with DME2Configuration as argument.
	 * catches instantiation and invocation exceptions and throws DME2Exception
	 * along with appropriate messages.
	 *
	 */

	private static BaseAccessor createGRMAccessorHandler(DME2Configuration configuration,
			SecurityContext securityContext) throws DME2Exception {
		logger.debug(null, "createGRMAccessorHandler", LogMessage.METHOD_ENTER);

		if (!configuration.getProperty(DME2ParameterNames.GRM_EDGE_DIRECT_HOST).isEmpty()) {
			logger.debug(null, "createGRMAccessorHandler", "Bootstrapping to Direct URL for GRM-Edge with Host: " + configuration.getProperty(DME2ParameterNames.GRM_EDGE_DIRECT_HOST));
			endPointDiscovery = GRMEndPointsDiscoveryDNS.getInstance(configuration.getProperty(DME2ParameterNames.GRM_EDGE_DIRECT_HOST), securityContext, configuration, false, false);
		} else if (!configuration.getProperty(DME2ParameterNames.GRM_EDGE_CUSTOM_DNS).isEmpty()) {
			logger.debug(null, "createGRMAccessorHandler", "Bootstrapping to Direct URL for GRM-Edge with DNS: " + configuration.getProperty(DME2ParameterNames.GRM_EDGE_CUSTOM_DNS));
			endPointDiscovery = GRMEndPointsDiscoveryDNS.getInstance(configuration.getProperty(DME2ParameterNames.GRM_EDGE_CUSTOM_DNS), securityContext, configuration, true, false);
		} else if (!configuration.getProperty(DME2ParameterNames.AFT_DME2_GRM_URLS).isEmpty()) {
			logger.debug(null, "createGRMAccessorHandler", "Bootstrapping to Direct URL AFT_DME2_GRM_URLS: " + configuration.getProperty(DME2ParameterNames.AFT_DME2_GRM_URLS));
			endPointDiscovery = GRMEndPointsDiscoveryDNS.getInstance(configuration.getProperty(DME2ParameterNames.AFT_DME2_GRM_URLS), securityContext, configuration, false, true);
		} else {
			// New Method DNS for Seed & Call GRM to get complete list, cache
			// result & update in background thread
			logger.debug(null, "createGRMAccessorHandler", "Using DNS Bootstrapping.");
			String grmDNSName = configuration.getProperty(DME2ParameterNames.GRM_DNS_BOOTSTRAP);
			if (grmDNSName == null || grmDNSName.isEmpty()) {
				grmDNSName = retrieveGRMDNSName(configuration);
				if (grmDNSName == null || grmDNSName.isEmpty()) {
					logger.error(null, "createGRMAccessorHandler", "{} parameter is mandatory please set a proper value",
							DME2ParameterNames.GRM_DNS_BOOTSTRAP);
					throw new DME2Exception("AFT-DME2-9601", new ErrorContext().add("Missing configuration property name",
							DME2ParameterNames.GRM_DNS_BOOTSTRAP));
				}
			}
			String grmServiceNameInGRM = reverseByDots(grmDNSName) + "."
					+ configuration.getProperty(DME2ParameterNames.GRM_SERVICE_NAME, "GRMLWPRestService");
			;
			String grmEnvironmentParameter = configuration.getProperty(DME2ParameterNames.GRM_ENVIRONMENT);
			String grmEnvironment = grmEnvironmentParameter != null ? grmEnvironmentParameter : grmEnvironmentDNS;
			if (grmEnvironment == null) {
				logger.error(null, "createGRMAccessorHandler", "{} parameter is mandatory please set a proper value",
						DME2ParameterNames.GRM_ENVIRONMENT);
				throw new DME2Exception("AFT-DME2-9601", new ErrorContext().add("Missing configuration property name",
						DME2ParameterNames.GRM_ENVIRONMENT));
			}
			endPointDiscovery = GRMEndPointsDiscoveryDNS.getInstance(grmDNSName, grmServiceNameInGRM, grmEnvironment,
					securityContext, configuration, null);
		}

		try {
			Object grmAccessorHandlerObject = Class.forName(grmAccessorHandlerClassName)
					.getDeclaredConstructor(DME2Configuration.class, SecurityContext.class, IGRMEndPointDiscovery.class)
					.newInstance(configuration, securityContext, endPointDiscovery);

			if (grmAccessorHandlerObject instanceof BaseAccessor) {
				if (endPointDiscovery instanceof GRMEndPointsDiscoveryDNS) {
					// Workaround because of inherent circular dependency
					((GRMEndPointsDiscoveryDNS) endPointDiscovery).getGrmEndPointsDiscoveryHelperGRM()
					.setGrmServiceAccessor((BaseAccessor) grmAccessorHandlerObject);
					if (configuration.getProperty(DME2ParameterNames.GRM_STATIC_ENDPOINT, "false").equalsIgnoreCase("false")
							|| configuration.getProperty(DME2ParameterNames.GRM_EDGE_DIRECT_HOST).isEmpty()
							|| configuration.getProperty(DME2ParameterNames.GRM_EDGE_CUSTOM_DNS).isEmpty()
							|| configuration.getProperty(DME2ParameterNames.AFT_DME2_GRM_URLS).isEmpty()) {
						((GRMEndPointsDiscoveryDNS) endPointDiscovery).refreshGRMServerListFromGRMSeeds();
					}
				}
				return (BaseAccessor) grmAccessorHandlerObject;
			} else {
				ErrorContext ec = new ErrorContext();
				ec.add(DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + BaseAccessor.class.getName(),
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + BaseAccessor.class.getName());
				logger.error(null, "createGRMAccessorHandler", "{} {} {}",
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION, BaseAccessor.class.getName(),
						DME2Constants.EXCEPTION_HANDLER_MSG, ec);
				throw new DME2Exception(DME2Constants.EXCEPTION_HANDLER_MSG, ec);
			}
		} catch (InstantiationException | IllegalAccessException |

				ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e)

		{
			logger.error(null, "createGRMAccessorHandler", "{} {} {}", BaseAccessor.class.getName(),
					DME2Constants.HANDLER_INSTANTIATION_EXCEPTION, DME2Constants.EXCEPTION_HANDLER_MSG, e);
			throw new DME2Exception(BaseAccessor.class.getName() + DME2Constants.HANDLER_INSTANTIATION_EXCEPTION, e);
		} finally {
			logger.debug(null, "createGRMAccessorHandler", LogMessage.METHOD_EXIT);
		}

	}

	private static String reverseByDots(String name) {
		String[] parts = name.split("\\.");
		grmEnvironmentDNS = buildEnvFromDNSPlatform(parts[0]);
		// grmSeedServiceName
		StringBuilder b = new StringBuilder(name.length());
		for (int i = parts.length - 1; i >= 0; i--) {
			if (b.length() > 0) {
				b.append(".");
			}
			b.append(parts[i]);
		}
		return b.toString();
	}

	/**
	 * extract environment from domain name of the server
	 *
	 * @param name
	 *            first part of the DNS name
	 * @return
	 */
	protected static String buildEnvFromDNSPlatform(String name) {
		try {
			String[] parts = name.split("-");
			return parts[parts.length - 1].toUpperCase();
		} catch (Exception ex) {
			logger.info(null, "buildEnvFromDNSPlatform", "first part of domain has no - to separate environment", ex);
			return null; // return null if pattern is not proper
		}
	}
	/*
	 * Method takes DME2Configuration as parameter and retrieves the
	 * GRMAccessorHandler Implementation class from the properties file. Checks
	 * if grmAccessorHandler is already instantiated, if not based on the
	 * implementation class returned from properties file, it will instantiate
	 * and return it.
	 *
	 * TODO: Make this generic, this shouldn't just return a GRM Accessor
	 */

	public static BaseAccessor getGrmAccessorHandlerInstance(DME2Configuration configuration,
			SecurityContext securityContext) throws DME2Exception {
		if (!grmAccessorHandlerMap.containsKey(configuration)) {
			grmAccessorHandlerClassName = configuration.getProperty(DME2Constants.GRMACESSOR_HANDLER_IMPL);
			if (null != grmAccessorHandlerClassName) {

				grmAccessorHandlerMap.put(configuration, createGRMAccessorHandler(configuration, securityContext));
			}
		}
		return grmAccessorHandlerMap.get(configuration);
	}

	public void resetGrmAccessorHandler() {
		grmAccessorHandlerMap.clear();
	}

	private static String retrieveGRMDNSName(DME2Configuration configuration) {		
		String platStr = configuration.getProperty("platform");
		String aftEnv = configuration.getProperty("AFT_ENVIRONMENT");
		
		if (platStr != null) {
			if (platStr.isEmpty()) {
				platStr = null;
			}
		}
		
		if (aftEnv != null) {
			if (aftEnv.isEmpty()) {
				aftEnv = null;
			}
		}

		if (aftEnv != null && platStr == null) {
			if (aftEnv.equalsIgnoreCase("AFTUAT")) {
				return configuration.getProperty("DME2_GRM_NONPROD_DNS_NAME");
			} else if (aftEnv.equalsIgnoreCase("AFTPRD")) {
				return configuration.getProperty("DME2_GRM_PROD_DNS_NAME");
			} else
				return null;
		}
		if (aftEnv == null && platStr == null) {
			return null;
		}
		if (platStr != null) {
			if (platStr.equalsIgnoreCase("SANDBOX-DEV")) {
				return configuration.getProperty("DME2_GRM_INFRATEST_DNS_NAME");
			} else if (platStr.equalsIgnoreCase("SANDBOX-LAB")) {
				return configuration.getProperty("DME2_GRM_INFRALAB_DNS_NAME");
			} else if (platStr.equalsIgnoreCase("NON-PROD")) {
				return configuration.getProperty("DME2_GRM_NONPROD_DNS_NAME");
			} else if (platStr.equalsIgnoreCase("PROD")) {
				return configuration.getProperty("DME2_GRM_PROD_DNS_NAME");
			} else
				return null;
		}

		return null;
	}
}