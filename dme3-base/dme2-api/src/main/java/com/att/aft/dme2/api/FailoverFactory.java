/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.lang.reflect.InvocationTargetException;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/* Factory class to return the Default GRMAccessor Handler specified in properties file.
 * Singleton class and returns only one instance of the Handler.
 */
public class FailoverFactory {
	private static String failoverHandlerClassName = null;

	private static FailoverHandler failoverHandler = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(FailoverFactory.class.getName());
	private static FailoverFactory failoverFactory = new FailoverFactory();

	/* Static 'instance' method */
	public static FailoverFactory getInstance() {
		return failoverFactory;
	}

	/*
	 * This method creates instance of FailoverHandler handler implementation
	 * class. It invokes parameterized constructor with DME2Configuration as
	 * argument. catches instantiation and invocation exceptions and throws
	 * DME2Exception along with appropriate messages.
	 * 
	 */
	private static FailoverHandler createFailoverHandler(DME2Configuration configuration) throws DME2Exception {

		try {
			Object failoverHandlerObject = Class.forName(failoverHandlerClassName)
					.getDeclaredConstructor(DME2Configuration.class).newInstance(configuration);

			if (null != failoverHandlerObject && failoverHandlerObject instanceof FailoverHandler) {
				return (FailoverHandler) failoverHandlerObject;
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
			LOGGER.error(null, "createFailoverHandler", "{} {} {}", BaseAccessor.class.getName(), DME2Constants.HANDLER_INSTANTIATION_EXCEPTION, DME2Constants.EXCEPTION_HANDLER_MSG, e);
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

	public static FailoverHandler getFailoverHandler(DME2Configuration configuration) throws DME2Exception {
		if (null == failoverHandler) {
			failoverHandlerClassName = configuration.getProperty(DME2Constants.FAILOVER_HANDLER_IMPL);
			if (null != failoverHandlerClassName && null == failoverHandler) {

				failoverHandler = createFailoverHandler(configuration);
			}
		}
		return failoverHandler;
	}
	
	public static void testFailoverFactory() {
		failoverHandler = null;
	}
}
