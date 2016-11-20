/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.handler;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;


public class DME2RestfulHandler implements AsyncResponseHandlerIntf
{

	private String service = null;
	private Throwable e = null;
	private final byte[] waiter = new byte[0];
	private final ResponseInfo rinfo = new ResponseInfo();

	private static final Logger logger = LoggerFactory.getLogger(DME2RestfulHandler.class.getName());

	public DME2RestfulHandler(String service)
	{
		this.service = service;
	}


	@Override
	public void handleException(Map<String, String> requestHeaders, Throwable e)
	{
		this.e = e;
		
		synchronized (waiter)
		{
			waiter.notify();
		}
	}

	@Override
	public ResponseInfo getResponse(long timeoutMs) throws Exception
	{
    logger.debug( null, "getResponse", LogMessage.METHOD_ENTER );
		long start = System.currentTimeMillis();
		
		synchronized (waiter)
		{
			if (this.rinfo.body != null)
			{
				return this.rinfo;
			}
			else if (e != null)
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
			
			
			try
			{
        logger.debug( null, "getResponse", "Waiting {} ms", timeoutMs);
				waiter.wait(timeoutMs);
			}
			catch (InterruptedException ie)
			{
				long elapsed = System.currentTimeMillis() - start;
				logger.debug( null, "getResponse", "DME2RestfulHandler interruptedException. ElapsedTime={}; timeoutMs={}", elapsed, timeoutMs);
				if (elapsed < timeoutMs)
				{
					waiter.wait(timeoutMs - elapsed);
				}
			}
			
			if (this.rinfo.body != null)
			{
				return this.rinfo;
			}
			else if (e != null)
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

      logger.error( null, "getResponse", "Timeout" );
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0999, new ErrorContext().add("service", service).add("timeoutMs", timeoutMs + ""), new Exception("Service call timedout"));
		}
	}


	@Override
	public void handleReply(int responseCode, String responseMessage, InputStream in,
			Map<String, String> requestHeaders, Map<String, String> responseHeaders)
	{

		this.rinfo.code = responseCode;
		
		if (responseHeaders != null)
		{
			this.rinfo.headers = new HashMap<String, String>(responseHeaders.size());
			
			for (Entry<String, String> eSet : responseHeaders.entrySet()){
				this.rinfo.headers.put(eSet.getKey(), eSet.getValue());
			}
		}

		StringBuilder output = new StringBuilder();
		
		try
		{
			String charset = findCharSet(responseHeaders);
			BufferedReader br;
			
			if (charset != null)
			{
				br = new BufferedReader(new InputStreamReader(in, charset));
			}
			else
			{
				br = new BufferedReader(new InputStreamReader(in));
			}

			String line;
			
			while ((line = br.readLine()) != null)
			{
				output.append(line);
				output.append("\n");
			}
		}
		catch (IOException io)
		{
			this.e = new Exception("UNABLE TO READ RESPONSE MESSAGE");
			this.e.initCause(io);
		}
		
		if (responseMessage != null)
		{
			output.append(responseMessage);
		}
		
		this.rinfo.body = output.toString().trim().replaceAll("Error 401", "Error 401 Unauthorized");
		
		synchronized (waiter)
		{
			waiter.notify();
		}
	}
	
	
	private String findCharSet(Map<String, String> parameterMap)
	{
		if (parameterMap == null)
		{
			return null;
		}
			
		String charset = parameterMap.get("Content-Type");
		
		if (charset == null)
		{
			charset = parameterMap.get("content-type");
		}
		
		if (charset != null)
		{
			String[] toks = charset.split(";");
			
			if (toks.length > 1)
			{
				charset = toks[1];
				String[] toks2 = toks[1].split("=");
				
				if (toks2.length > 1)
				{
					charset = toks2[1];
				}
				else
				{
					charset = null;
				}
			}
			else
			{
				charset = null;
			}
		}
		return charset;
	}


	public static ResponseInfo callService(String service, Integer timeoutMs, String httpMethod, String urlContext,
			Map<String, String> queryParams, Map<String, String> requestHeaders, String requestPayload)
			throws Exception
	{
		DME2RestfulHandler replyHandler = new DME2RestfulHandler(service);
		
		DME2Client sender = new DME2Client(new URI(service), timeoutMs);
		sender.setAllowAllHttpReturnCodes(true);// Don't break on code!=200
		sender.setMethod(httpMethod);
		sender.setSubContext(urlContext);
		sender.setQueryParams(queryParams, true); // URLEncode the values
		sender.setHeaders(requestHeaders);
		sender.setPayload(requestPayload);
		sender.setReplyHandler(replyHandler);
		sender.send();

		ResponseInfo reply = replyHandler.getResponse(timeoutMs);
		return reply;
	}


	public static ResponseInfo callService(String service, Integer timeoutMs, String httpMethod, String urlContext,
			Map<String, String> queryParams, Map<String, String> requestHeaders, DME2Payload requestPayload)
			throws Exception
	{
		return callService_mod(service, timeoutMs, httpMethod,  urlContext,	queryParams, requestHeaders, requestPayload, null, null);
	}
	
	
	public static ResponseInfo callService(String service, Integer timeoutMs, String httpMethod, String urlContext,
			Map<String, String> queryParams, Map<String, String> requestHeaders, DME2Payload requestPayload, String username, String password)
			throws Exception
	{	
		return callService_mod(service, timeoutMs, httpMethod,  urlContext,	queryParams, requestHeaders, requestPayload, username, password);
	}
	
	private static ResponseInfo callService_mod(String service, Integer timeoutMs, String httpMethod, String urlContext,
			Map<String, String> queryParams, Map<String, String> requestHeaders, DME2Payload requestPayload, String username, String password)
			throws Exception
	{
		
		DME2RestfulHandler replyHandler = new DME2RestfulHandler(service);
		
		DME2Client sender = new DME2Client(new URI(service), timeoutMs);
		sender.setAllowAllHttpReturnCodes(true);// Don't break on code!=200
		sender.setMethod(httpMethod);
		sender.setSubContext(urlContext);
		sender.setDME2Payload(requestPayload);
		sender.setReplyHandler(replyHandler);
		
		if(requestHeaders != null)
		{
			sender.setHeaders(requestHeaders);
		}
		
		if(queryParams != null)
		{
			/*URL Encode the values*/
			sender.setQueryParams(queryParams, true); 
		}
		
		if(username != null && password != null)
		{
			sender.setCredentials(username, password);
		}
		
		sender.send();

		ResponseInfo reply = replyHandler.getResponse(timeoutMs);
		return reply;
	}


	public class ResponseInfo
	{
		private String body;
		private Integer code;
		private Map<String, String> headers;

		public String header(String name)
		{
			return headers.get(name);
		}


		@Override
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append(this.code).append("\n");
			
			if (this.headers != null)
			{
				for (Entry<String, String> eSet : this.headers.entrySet())
				{
					if (eSet.getValue().contains(",<"))
					{
						
						for (String s : eSet.getValue().split(",<"))
						{
							sb.append(eSet.getKey()).append("=").append(s.charAt(0) == '<' ? "" : "<").append(s).append("\n");
						}
					}
					else
					{
						sb.append(eSet.getKey()).append("=").append(eSet.getValue()).append("\n");
					}
				}
			}
				
			sb.append(this.body);
			return sb.toString();
		}


		public String getBody() {
			return body;
		}


		public void setBody(String body) {
			this.body = body;
		}


		public Integer getCode() {
			return code;
		}


		public void setCode(Integer code) {
			this.code = code;
		}


		public Map<String, String> getHeaders() {
			return headers;
		}


		public void setHeaders(Map<String, String> headers) {
			this.headers = headers;
		}
	}
}
