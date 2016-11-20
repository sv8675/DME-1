/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;

import com.att.aft.dme2.api.DME2ReplyHandler;
import com.att.aft.dme2.api.DME2StreamReplyHandler;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2IngressProxyReplyHandler extends DME2StreamReplyHandler implements DME2ReplyHandler, AsyncResponseHandlerIntf {
		private String service;
		private static final Logger logger = LoggerFactory.getLogger(DME2IngressProxyReplyHandler.class);

		
		private Continuation continuation;
		
		private String correlationId;
		
		private OutputStream responseStream;
		
		private boolean streamMode;
		private String DME2_STREAM_HEADER_INFO = "X-DME2_PROXY_STREAM";
		private DME2Configuration config;

		public DME2IngressProxyReplyHandler(String service, Continuation continuation, String correlationId, boolean streamMode, DME2Configuration config) {
			this.service = service;
			this.continuation = continuation;
			this.correlationId = correlationId;
			this.streamMode = streamMode;
			this.config = config;
		}

		@Override
		public void handleException(Map<String, String> requestHeaders, Throwable e) {
			HttpServletResponse resp = (HttpServletResponse) continuation.getServletResponse();
			try {
				resp = (HttpServletResponse) continuation.getServletResponse();
				resp.sendError(500, e.toString());
				resp.flushBuffer();
			} catch (IOException x) {
				try {
					resp.sendError(500, x.toString());
					resp.flushBuffer();
				} catch (IOException e1) {
					logger.debug(null, "handleException", LogMessage.DEBUG_MESSAGE, "IOException", e1);
				}
				return;
			} finally {
				continuation.complete();
			}
		}

		@Override
		public void handleReply(int responseCode, String responseMessage,
				InputStream in, Map<String, String> requestHeaders,
				Map<String, String> responseHeaders) {
			
			HttpServletResponse resp = (HttpServletResponse) continuation.getServletResponse();
			String response = null;
			BufferedReader reader = null;
			GZIPInputStream gis = null;
			try {
				resp = (HttpServletResponse) continuation.getServletResponse();
				
				String contentEncoding = responseHeaders.get(config.getProperty(DME2Constants.DME2_CONTENT_ENCODING_KEY));
				if(contentEncoding != null && contentEncoding.equalsIgnoreCase(config.getProperty(DME2Constants.DME2_CLIENT_COMPRESS_TYPE)) && Boolean.parseBoolean(config.getProperty(DME2Constants.DME2_CLIENT_ALLOW_COMPRESS))) {
					try{
						 gis = new GZIPInputStream(in);
					} catch(Exception e) {
                        e.printStackTrace();
						try {
							resp.sendError(500, e.toString());
							resp.flushBuffer();
						} catch (IOException e1) {
							logger.debug(null, "handleReply", LogMessage.DEBUG_MESSAGE, "IOException", e1);
						}
						return;
					}
				}
				String charset = findCharSet(responseHeaders);
				if (charset != null) {
					if(gis == null) {
						reader = new BufferedReader(new InputStreamReader(in, charset));
					}
					else {
						reader = new BufferedReader(new InputStreamReader(gis, charset));
					}
				} else {
					if(gis == null) {
						reader = new BufferedReader(new InputStreamReader(in));
					}
					else {
						reader = new BufferedReader(new InputStreamReader(gis));
					}
				}
				
				final char[] buffer = new char[8096];
				StringBuilder inputText = new StringBuilder(8096);

				int n = -1;
				while ((n = reader.read(buffer)) != -1) {
					inputText.append(buffer, 0, n);
				}

				response = inputText.toString();

				if(response != null) {			
					
					logger.info(null, "handleReply", "AFT-DME2-6610",
							new ErrorContext().add("requestURI",service)
									.add("correlationId", correlationId)
									.add("responseSize", response.getBytes().length+""));
					
					Iterator<String> it = responseHeaders.keySet().iterator();
					while(it.hasNext()) {
						String key = it.next();
						String value = responseHeaders.get(key);
						resp.addHeader(key, value);
					}
					if(streamMode){
						resp.addHeader(this.DME2_STREAM_HEADER_INFO, "true");
					}
					resp.setStatus(responseCode);
					resp.getOutputStream().write(response.getBytes());
					resp.flushBuffer();
				}
				
			} catch (IOException e) {
				try {
					resp.sendError(500, e.toString());
					resp.flushBuffer();
				} catch (IOException e1) {
					logger.debug(null, "handleReply", LogMessage.DEBUG_MESSAGE, "IOException",e1);
				}
				return;
			} catch(Exception ex){
				ex.printStackTrace();
			}	finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
					logger.debug(null, "handleReply", LogMessage.DEBUG_MESSAGE, "IOException",e);
				}
				continuation.complete();
			}
		}
		
		private String findCharSet(Map<String,String> parameterMap) {
			String charset = (String)parameterMap.get("Content-Type");
			if (charset == null) {
				charset = (String)parameterMap.get("content-type");
			} 
			if (charset != null) {
				String[] toks = charset.split(";");
				if (toks.length > 1) {
					charset = toks[1];
					String[] toks2 = toks[1].split("=");
					if (toks2.length > 1) {
						charset = toks2[1];
					} else {
						charset = null;
					}
				} else {
					charset = null;
				}
			}
			return charset;
		}

		@Override
		public void handleContent(byte[] bytes)
		{
			try
			{

				if(continuation != null)
				{
					HttpServletResponse servletResp = (HttpServletResponse) continuation.getServletResponse();
					servletResp.setHeader("X-DME2_PROXY_RESPONSE_STREAM", "true");
					responseStream = servletResp.getOutputStream();
					responseStream.write(bytes);
				}
			}
			catch (IOException e)
			{
				logger.error(null, "handleContent", LogMessage.EXCH_READ_HANDLER_FAIL, 
						new ErrorContext().add("requestURI", service).add("correlationId", correlationId), e);
				return;
			}
			
		}

		@Override
		public String getResponse(long timeoutMs) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		
		
	}