package com.att.aft.dme2.registry.bootstrap;

import java.lang.reflect.InvocationTargetException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/* Factory class to return the Default RegistryBootstrap Handler specified in properties file.
 * Singleton class and returns only one instance of the Handler.
 */
public class RegistryBootstrapFactory {

	private static String registryBootstrapHandlerClassName = null;

	private static RegistryBootstrap registryBootstrapAccessorHandler = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistryBootstrapFactory.class.getName());

	private static RegistryBootstrapFactory registryBootstrapFactory = new RegistryBootstrapFactory();

	/* Static 'instance' method */
	public static RegistryBootstrapFactory getInstance() {
		return registryBootstrapFactory;
	}

	/*
	 * This method creates instance of RegistryBootstrap handler implementation
	 * class. It invokes parameterized constructor with DME2Configuration as
	 * argument. catches instantiation and invocation exceptions and throws
	 * DME2Exception along with appropriate messages.
	 * 
	 */
	private static RegistryBootstrap createRegistryBootstrapHandler(DME2Configuration configuration, String... urls)
			throws DME2Exception {

		try {
			Object registryBootstrapHandlerObject = Class.forName(registryBootstrapHandlerClassName)
					.getDeclaredConstructor(DME2Configuration.class).newInstance(configuration);

			if (null != registryBootstrapHandlerObject && registryBootstrapHandlerObject instanceof RegistryBootstrap) {
				return (RegistryBootstrap) registryBootstrapHandlerObject;
			}

			else {
				ErrorContext ec = new ErrorContext();
				ec.add(DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + RegistryBootstrap.class.getName(),
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + RegistryBootstrap.class.getName());
				LOGGER.error(null,
						DME2Constants.HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION + RegistryBootstrap.class.getName(),
						DME2Constants.EXCEPTION_HANDLER_MSG, ec);
				throw new DME2Exception(DME2Constants.EXCEPTION_HANDLER_MSG, ec);
			}
		} catch (InstantiationException | IllegalAccessException |

		ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e)

		{
			LOGGER.error(null, "createRegistryBootstrapHandler", "{} {} {}", RegistryBootstrap.class.getName(),DME2Constants.HANDLER_INSTANTIATION_EXCEPTION,
					DME2Constants.EXCEPTION_HANDLER_MSG, e);
			throw new DME2Exception(RegistryBootstrap.class.getName() + DME2Constants.HANDLER_INSTANTIATION_EXCEPTION,
					e);
		}

	}

	/*
	 * Method takes DME2Configuration as parameter and retrieves the
	 * RegistryBootstrapHandler Implementation class from the properties file.
	 * Checks if RegistryBootstrapHandler is already instantiated, if not based
	 * on the implementation class returned from properties file, it will
	 * instantiate and return it.
	 */
	public static RegistryBootstrap getRegistryBootstrapHandler(DME2Configuration configuration) throws DME2Exception {
		if (null == registryBootstrapAccessorHandler) {
			registryBootstrapHandlerClassName = configuration.getProperty(DME2Constants.REGISTRYBOOTSTRAP_HANDLER_IMPL);
			if (null != registryBootstrapHandlerClassName && null == registryBootstrapAccessorHandler) {

				registryBootstrapAccessorHandler = createRegistryBootstrapHandler(configuration);
			}
		}
		return registryBootstrapAccessorHandler;
	}

}
