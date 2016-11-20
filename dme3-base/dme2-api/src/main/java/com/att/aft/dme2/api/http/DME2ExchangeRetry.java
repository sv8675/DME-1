/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.http;

import org.eclipse.jetty.client.api.Response;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2ExchangeRetry implements Runnable
{
	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger( DME2ExchangeRetry.class.getName() );
	private DME2Exchange exchange;
	private Response response;

	DME2ExchangeRetry(DME2Exchange exchange, Response response)
	{
		this.exchange = exchange;
		this.response = response;
	}


	@Override
	public void run()
	{
		try
		{
			logger.debug( null, "run", "Inside run method in DME2ExchangeRetry");
			exchange.doTry(response);
		}
		catch (Exception e)
		{
			logger.debug( null, "run", LogMessage.EXCH_RETRY_FAIL, e);
		}
	}


	DME2Exchange getExchange() {
		return exchange;
	}


	void setExchange(DME2Exchange exchange) {
		this.exchange = exchange;
	}

}