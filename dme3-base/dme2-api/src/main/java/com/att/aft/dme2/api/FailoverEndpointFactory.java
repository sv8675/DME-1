/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.lang.reflect.InvocationTargetException;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverEndpoint;
import com.att.aft.dme2.handler.FailoverHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/* Factory class to return the Default GRMAccessor Handler specified in properties file.
 * Singleton class and returns only one instance of the Handler.
 */

public class FailoverEndpointFactory {

	private static String failoverEndpointHandlerClassName = null;

	private static FailoverEndpoint failoverEndpointFetchingHandler = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(FailoverEndpointFactory.class.getName());

	private static FailoverEndpointFactory failoverEndpointFactory = new FailoverEndpointFactory();

	/* Static 'instance' method */
	public static FailoverEndpointFactory getInstance() {
		return failoverEndpointFactory;
	}

	/*
	 * This method creates instance of FailoverEndpoint handler implementation
	 * class. It invokes parameterized constructor with DME2Configuration as
	 * argument. catches instantiation and invocation exceptions and throws
	 * DME2Exception along with appropriate messages.
	 * 
	 */
	private static FailoverEndpoint createFailoverEndpointFetchingHandler(DME2Configuration configuration)
			throws DME2Exception {

		try {
			Object failoverHandlerObject = Class.forName(failoverEndpointHandlerClassName)
					.getDeclaredConstructor(DME2Configuration.class).newInstance(configuration);

			if (null != failoverHandlerObject && failoverHandlerObject instanceof FailoverEndpoint) {
				return (FailoverEndpoint) failoverHandlerObject;
			}

			else {
				ErrorContext ec = new ErrorContext();
				ec.add(DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + FailoverHandler.class.getName(),
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + FailoverHandler.class.getName());
				LOGGER.error(null,
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + FailoverHandler.class.getName(),
						DME2Constants.EXCEPTION_HANDLER_MSG, ec);
				throw new DME2Exception(DME2Constants.EXCEPTION_HANDLER_MSG, ec);
			}
		} catch (InstantiationException | IllegalAccessException |

		ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e)

		{
			LOGGER.error(null, "createFailoverEndpointFetchingHandler", "{} {} {}", BaseAccessor.class.getName(), DME2Constants.HANDLER_INSTANTIATION_EXCEPTION, DME2Constants.EXCEPTION_HANDLER_MSG, e);
			throw new DME2Exception(BaseAccessor.class.getName() + DME2Constants.HANDLER_INSTANTIATION_EXCEPTION, e);
		}

	}

	/*
	 * Method takes DME2Configuration as parameter and retrieves the
	 * FailoverHandler Implementation class from the properties file. Checks if
	 * FailoverHandler is already instantiated, if not based on the
	 * implementation class returned from properties file, it will instantiate
	 * and return it.
	 */

	public static FailoverEndpoint getFailoverEndpointHandler(DME2Configuration configuration) throws DME2Exception {
		if (null == failoverEndpointFetchingHandler) {
			failoverEndpointHandlerClassName = configuration.getProperty(DME2Constants.FAILOVER_ENDPOINT_HANDLER_IMPL);
			if (null != failoverEndpointHandlerClassName && null == failoverEndpointFetchingHandler) {

				failoverEndpointFetchingHandler = createFailoverEndpointFetchingHandler(configuration);
			}
		}
		return failoverEndpointFetchingHandler;
	}

}
