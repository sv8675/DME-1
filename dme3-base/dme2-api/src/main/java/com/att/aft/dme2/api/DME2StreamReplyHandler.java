/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.io.InputStream;
import java.util.Map;

import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public abstract class DME2StreamReplyHandler implements AsyncResponseHandlerIntf
{
	private static final Logger logger = LoggerFactory.getLogger( DME2StreamReplyHandler.class );
	private byte[] waiter = new byte[0];
	private Exception e = null;
	
	private String responseCode;
	
	
	@Override
	public void handleException(Map<String, String> requestHeaders, Throwable e)
	{
		this.e = (Exception) e;
		synchronized (waiter)
		{
			waiter.notify();
		}

	}


	@Override
	public void handleReply(int responseCode, String responseMessage, InputStream in, Map<String, String> requestHeaders, Map<String, String> responseHeaders)
	{
		this.responseCode = responseCode + "";
		
		synchronized (waiter)
		{
			waiter.notify();
		}
	}
	
	public String getResponse(long timeout) throws Exception
	{
		long start = System.currentTimeMillis();
		
		synchronized (waiter)
		{
			if(responseCode != null)
			{
				return responseCode;
			}
			
			if(e != null)
			{
				if(e instanceof DME2Exception )
				{
					DME2Exception exception = new DME2Exception(((DME2Exception) e).getErrorCode(), ((DME2Exception) e).getErrorMessage(), e);
					throw exception;
				}
				else
				{
					throw e;
				}
				
			}
			
			try
			{
				waiter.wait(timeout);
			}
			catch (InterruptedException ie)
			{
				long elapsed = System.currentTimeMillis() - start;
				logger.debug( null, "getResponseCode",
            "DME2SimpleReplyHandler interruptedException. ElapsedTime={} ;timeoutMs={}", elapsed, timeout );

				if (elapsed < timeout)
				{
					waiter.wait(timeout - elapsed);
				}
			}
			
			if(responseCode != null)
			{
				return responseCode;
			}
			
			if (e != null)
			{
				if (!(e instanceof Exception))
				{
					throw new RuntimeException(e);
				}
				else
				{
					throw (Exception) e;
				}
			}
			
		}
		
		throw new DME2Exception("AFT-DME2-0999", new ErrorContext().add("timeoutMs", timeout + ""),
				new Exception("Service call timed-out"));
	}
	
	
	public abstract void handleContent(byte[] bytes);

	/**
	 * The below method should be overriden by applications that intend to get response code
	 * and headers as part of handleContent invocation
	 */
	public void handleContent(byte[] bytes, int responseCode, Map<String, String> requestHeaders, Map<String, String> responseHeaders) {
		handleContent(bytes);
	}
}
