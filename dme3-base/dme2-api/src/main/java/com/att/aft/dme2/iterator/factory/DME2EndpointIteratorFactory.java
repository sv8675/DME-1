package com.att.aft.dme2.iterator.factory;

import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;
import com.att.aft.dme2.iterator.service.EndpointIteratorBuilder;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2EndpointIteratorFactory {

	private static final Logger logger = LoggerFactory.getLogger(DME2EndpointIteratorFactory.class.getName());
	private volatile static DME2EndpointIteratorFactory instance = null;
	private DME2Manager manager = null;

	DME2EndpointURLFormatter urlFormatter = null;

	EndpointIteratorBuilder endpointIteratorBuilder = null;

	public DME2EndpointIteratorFactory() {

	}

	@Deprecated
	public static DME2EndpointIteratorFactory getInstance(DME2Manager manager) {
        DME2EndpointIteratorFactory result = instance;
		if (result == null) {
			synchronized (DME2EndpointIteratorFactory.class) {
			    result = instance;
				if ( result == null ) { 
				    instance = result = new DME2EndpointIteratorFactory();
			    }
			}
		}
		return result;
	}

	public static DME2EndpointIteratorFactory getInstance() {
        return getInstance( null );

	}

	public DME2BaseEndpointIterator getIterator(String url, Properties props, DME2EndpointURLFormatter urlFormatterImpl)
			throws DME2Exception {
		return getIterator(url, props, urlFormatterImpl, DME2Manager.getDefaultInstance());
	}

	public DME2BaseEndpointIterator getIterator(final String newUrl, Properties props,
			DME2EndpointURLFormatter urlFormatterImpl, DME2Manager manager) throws DME2Exception {
		String url = newUrl;
		logger.debug(null, "getIterator", LogMessage.METHOD_ENTER);
		this.manager = manager;
		this.endpointIteratorBuilder = EndpointIteratorFactory.getDefaultEndpointIteratorBuilder(manager.getConfig());
		this.endpointIteratorBuilder.setManager(manager);
		this.endpointIteratorBuilder.setServiceURI(newUrl);
		this.endpointIteratorBuilder.setUrlFormatter(urlFormatterImpl);
		this.endpointIteratorBuilder.setProps(props);
		DME2BaseEndpointIterator iterator = this.endpointIteratorBuilder.build();
		logger.debug(null, "getIterator", LogMessage.METHOD_EXIT);
		return iterator;
	}
}
