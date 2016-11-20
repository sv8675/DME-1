/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2JMSServiceHolder extends DME2ServiceHolder {

	private static final Logger logger = LoggerFactory.getLogger( DME2JMSServiceHolder.class );
	private DME2JMSQueue dme2JMSQueue;

	public DME2JMSServiceHolder(DME2JMSQueue dme2JMSQueue) {
		this.dme2JMSQueue = dme2JMSQueue;
	}

	@Override
	public int getMaxPoolSize() {
		logger.debug(null, "getMaxPoolSize", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter JMS queue receivers");
		logger.debug(null, "getMaxPoolSize", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter JMS max pool size =" + dme2JMSQueue != null ? dme2JMSQueue.getListeners().size() : 0);	
		return dme2JMSQueue != null ? dme2JMSQueue.getListeners().size() : 0;
	}
}
