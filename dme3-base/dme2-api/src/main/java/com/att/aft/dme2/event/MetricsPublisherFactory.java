package com.att.aft.dme2.event;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/** 
 * Factory class to return the Default MetricsPublisher specified in properties file.
 * Singleton class and returns only one instance of the Handler.
 */
public class MetricsPublisherFactory {

	private static String metricsPublisherHandlerClassName = null;

	private static BaseMetricsPublisher metricsPublisherHandler = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(MetricsPublisherFactory.class.getName());
	private static MetricsPublisherFactory metricsPublisherFactory = new MetricsPublisherFactory();

	/* Static 'instance' method */
	public static MetricsPublisherFactory getInstance() {
		return metricsPublisherFactory;
	}

	/*
	 * This method creates instance of BaseMetricsPublisher handler implementation class.
	 * It invokes parameterized constructor with DME2Configuration as argument.
	 * catches instantiation and invocation exceptions and throws DME2Exception
	 * along with appropriate messages.
	 * 
	 */

	private static BaseMetricsPublisher createMetricsPublisherFactoryHandler(DME2Configuration configuration) throws DME2Exception {

		try {
			Object metricsPublisherHandlerObject = Class.forName(metricsPublisherHandlerClassName)
					.getDeclaredConstructor(DME2Configuration.class).newInstance(configuration);

			if (null != metricsPublisherHandlerObject && metricsPublisherHandlerObject instanceof BaseMetricsPublisher) {
				return (BaseMetricsPublisher) metricsPublisherHandlerObject;
			}

			else {
				ErrorContext ec = new ErrorContext();
				ec.add(DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + BaseMetricsPublisher.class.getName(),
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + BaseMetricsPublisher.class.getName());
				LOGGER.error(null,
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + BaseMetricsPublisher.class.getName(),
						DME2Constants.EXCEPTION_HANDLER_MSG, ec);
				throw new DME2Exception(DME2Constants.EXCEPTION_HANDLER_MSG, ec);
			}
		} catch (Exception e)

		{
			LOGGER.error(null, "createMetricsPublisherFactoryHandler", "{} {} {}", BaseMetricsPublisher.class.getName(), DME2Constants.HANDLER_INSTANTIATION_EXCEPTION,
					DME2Constants.EXCEPTION_HANDLER_MSG, e);
			throw new DME2Exception(BaseMetricsPublisher.class.getName() + DME2Constants.HANDLER_INSTANTIATION_EXCEPTION, e);
		}

	}

	/*
	 * Method takes DME2Configuration as parameter and retrieves the
	 * MetricsPublisher Implementation class from the properties file. Checks
	 * if MetricsPublisherHandler is already instantiated, if not based on the
	 * implementation class returned from properties file, it will instantiate
	 * and return it.
	 */

	public static BaseMetricsPublisher getBaseMetricsPublisherHandlerInstance(DME2Configuration configuration) throws DME2Exception {
		if (null == metricsPublisherHandler) {
			metricsPublisherHandlerClassName = configuration.getProperty(DME2Constants.METRICS_PUBLISHER_HANDLER_IMPL);
			if (null != metricsPublisherHandlerClassName && null == metricsPublisherHandler) {

				metricsPublisherHandler = createMetricsPublisherFactoryHandler(configuration);
			}
		}
		return metricsPublisherHandler;
	}
}
