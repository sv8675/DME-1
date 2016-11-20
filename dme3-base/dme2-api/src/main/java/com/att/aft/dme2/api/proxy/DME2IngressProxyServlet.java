/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.proxy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URI;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.GuidGen;

public class DME2IngressProxyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String CLASS_NAME = "com.att.aft.dme2.proxy.DME2IngressProxyServlet";
	private static Logger logger = LoggerFactory.getLogger(CLASS_NAME);
	//private static Configuration config = Configuration.getInstance();
	private static DME2Configuration config2 = new DME2Configuration("DME2IngressProxyServlet");
	
	final int MSG_PARSING_BUFFER = 8096;
	private DME2Manager manager;
	private String scldEnv = null;
	private boolean allowHttpReturnCode = false;
	private static final String REQUESTURI="requestURI" ;
	
	@Override
	public void destroy() {
		logger.info(null, "destroy", LogMessage.PROXY_DESTROYED, CLASS_NAME, manager.getName());
		
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		if (manager == null) {
			@SuppressWarnings("unchecked")
			Enumeration<String> parmNames = config.getInitParameterNames();
			Properties props = new Properties();
			while (parmNames.hasMoreElements()) {
				String name = parmNames.nextElement();
				String value = config.getInitParameter(name);
				if (name != null && value != null) {
					props.setProperty(name,  value);
				}
			}
			scldEnv = System.getProperty("scldEnv");
			String mgrName = props.getProperty("com.att.aft.dme2.manager.name");
			DME2Configuration config2 = new DME2Configuration(mgrName);
			
			try {
				if (mgrName != null) {
					manager = new DME2Manager(mgrName, config2);
				} else {
					// note: this will fail if the DME2Manager was already initialized under the currrent classloader...
					// for most portable, assign each web app its own DME2Manager with unique name
					manager = new DME2Manager(mgrName, config2);
				}
			} catch (DME2Exception e) {
				throw new ServletException("Error initializing DME2Manager", e);
			}		
		}
	}

	@Override
	public void init() throws ServletException {
		if (manager != null) {
			manager = new DME2Manager();
		}
	}

	public DME2IngressProxyServlet(DME2Manager manager) {
		logger.debug( null, "ctor(DME2Manager)", LogMessage.METHOD_ENTER );
		this.manager = manager;
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}
	
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}	
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}
	
	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings({ "unused", "deprecation" })
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
    logger.debug( null, "service", LogMessage.METHOD_ENTER );
		String convId = null;
		String username = null;
		String password = null;
		boolean streamMode = false;
		Continuation continuation = null;
		long endpointReadTimeout = 60000;
		StringBuffer buf = new StringBuffer();

		String version=null;
		String routeOffer = null;
		String partner = null;
		String streamModeStr = null;
		String subContextStr = null;
		try {

			String healthCheck = req.getHeader("DME2HealthCheck");
			if (healthCheck != null) {
				resp.setStatus(200);
				return;
			}
			
			// walk through query data and pull out anything the proxy needs.  we'll remove those items and restore the rest on final url.
			String queryString = req.getQueryString();
			Map<String,String> newQueryMap = new HashMap<String,String>();
			if (queryString != null) {
				String[] queryToks = queryString.split("&");
				for (String qtok : queryToks) {
					String[] pair = qtok.split("=");
					if (pair.length == 2) {
						String key = pair[0];
						if (key.toLowerCase().equals("dme2.endpointreadtimeout")) {
							String endpointReadTimeoutStr = pair[1];
							try {
								endpointReadTimeout = Long
										.parseLong(endpointReadTimeoutStr);
							} catch (Exception e) {
								logger.debug(null, "service", LogMessage.DEBUG_MESSAGE, "Exception",e);
								// ignore error in parsing
							}
							// validate if endpointReadTimeout is more than
							// maximum
							// defined ( 5 mins )
							if (endpointReadTimeout > Integer.parseInt(config2.getProperty(config2.getProperty(DME2Constants.DME2_ENDPOINT_DEF_READ_TIMEOUT)))) {
								endpointReadTimeout = Integer.parseInt(config2.getProperty(config2.getProperty(DME2Constants.DME2_ENDPOINT_DEF_READ_TIMEOUT)));
							}
						}
						else if (key.toLowerCase().equals("dme2.username")) {
							username = URLDecoder.decode(pair[1], "UTF-8");
						}
						else if (key.toLowerCase().equals("dme2.password")) {
							password = URLDecoder.decode(pair[1], "UTF-8");
						}
						else if (key.toLowerCase().equals("dme2.convid")) {
							convId = URLDecoder.decode(pair[1], "UTF-8");
						}
						else if (key.toLowerCase().equals("dme2.streammode")) {
							streamModeStr = URLDecoder.decode(pair[1], "UTF-8");
						}
						else if (key.toLowerCase().equals("dme2.allowhttpcode")) {
							this.allowHttpReturnCode = true;
						}
						else if (key.toLowerCase().equals("subcontext")) {
							subContextStr = pair[1];
						}
						else if (key.toLowerCase().equals("version")) {
							version = pair[1];
							newQueryMap.put(key, pair[1]);
						}
						else if (key.toLowerCase().equals("partner")) {
							partner = pair[1];
							newQueryMap.put(key, pair[1]);
						}
						else if (key.toLowerCase().equals("routeoffer")) {
							routeOffer = pair[1];
							newQueryMap.put(key, pair[1]);
						}
						else {
							newQueryMap.put(key, pair[1]);
						}
					}
				}
			}
			
			if(streamModeStr != null) {
				if(streamModeStr.equalsIgnoreCase("true"))
					streamMode = true;
			}
			else{
				streamMode = Boolean.getBoolean("DME2_PROXY_STREAM_MODE");
			}
			// if scldEnv is found add it as envContext
			if(scldEnv != null && newQueryMap.get("envContext")== null)
				newQueryMap.put("envContext", scldEnv);
			
			String finalQueryString = genQueryString(newQueryMap);
			
			// build the request uri
			DME2URI uniformResource = null;
			String servletPath = req.getServletPath();
			
			if(servletPath != null ) {
				String service = this.getServiceName(servletPath);
				if(service!=null) {
					// servletPath contains service name and validate if service has / in it.
					String elems[] = service.split("/");
					int i = elems.length;
					//http://host:port/service=ServiceName.att.com/cp1/subcontext1/version=1.0/envContext=DEV/partner=abc
					// if request uri has service= pattern in it.
					if (i>1){
						String versionStr = "version="+ this.getField(servletPath, "/version=");
						String envContextStr = "envContext="+ this.getField(servletPath, "/envContext=");
						if(subContextStr == null)
							subContextStr = this.getField(servletPath, "/subContext=");
						String routeStr = this.getField(servletPath, "/partner=")!=null?"partner="+this.getField(servletPath, "/partner="):"routeOffer="+this.getField(servletPath, "/routeOffer=");
						String requestURI = "http://"+ service  + "?" + versionStr + "&" + envContextStr + "&" + routeStr +  "&" + finalQueryString.replaceAll("//", "/");
						uniformResource = new DME2URI(config2, requestURI);
					}
					//http://host:port/service=ServiceName.att.com/version=1.0/envContext=DEV/partner=abc
					else{
						String urlPath = (servletPath + "?" + finalQueryString).replaceAll("//", "/");
						if (req.getServletPath().contains("routeOffer=")) {
							uniformResource = new DME2URI(config2, "http://DME2RESOLVE/" +  urlPath);
						} else {
							uniformResource = new DME2URI(config2, "http://DME2SEARCH/" +  urlPath);
						} 
					}
				}
				else {
					String requestURI = "http://"+req.getServerName()+"/"+(servletPath + "?" + finalQueryString).replaceAll("//", "/");
					uniformResource = new DME2URI(config2, requestURI);
				}
			}
			
			logger.debug( null, "service", " uniformResource : {}", uniformResource);

			
			// generate a uniq trans id if client had not provided one.
			if (convId == null) {
				convId = GuidGen.getId();
			}

			// set logging context
			DME2Constants.setContext(convId, null);
					
			logger.info(null,  "service", "AFT-DME2-6600",
					new ErrorContext().add("requestFrom",
							req.getRemoteHost() + ":" + req.getRemotePort())
							.add("conversationID", convId).add(REQUESTURI,
									req.getRequestURI()));
			
			// Set the continuation timeout
			continuation = ContinuationSupport.getContinuation(req, resp);
			continuation.setTimeout(endpointReadTimeout+1000);
			DME2IngressContinuationListener listener = new DME2IngressContinuationListener(convId, resp);
			continuation.addContinuationListener(listener);
			continuation.suspend(resp);
			GZIPInputStream gis = null;
			String contentEncoding = (String) req.getHeader(config2.getProperty(DME2Constants.DME2_CONTENT_ENCODING_KEY));
			if(contentEncoding != null && contentEncoding.equalsIgnoreCase(config2.getProperty(DME2Constants.DME2_CLIENT_COMPRESS_TYPE)) && Boolean.parseBoolean(config2.getProperty(DME2Constants.DME2_CLIENT_ALLOW_COMPRESS))) {
				try {
					logger.debug(null, "service", "AFT-DME2-6614",
							new ErrorContext().add("requestFrom",
									req.getRemoteHost() + ":" + req.getRemotePort())
									.add("contentEncoding", contentEncoding));
					
					gis = new GZIPInputStream(req.getInputStream());
				} catch(Exception e) {
					continuation.resume();
					logger.error(null, "service", "AFT-DME2-6615",
							new ErrorContext().add(REQUESTURI,buf.toString())
												.add("errorCreatingGZIPStream", e.getMessage()),e);
					resp.sendError(500,"UNABLE TO READ COMPRESSED INPUT MESSAGE;Message="+e.getMessage());
					resp.flushBuffer();
					continuation.complete();
				}
			}
			StringBuilder output = new StringBuilder(MSG_PARSING_BUFFER);

			if (!streamMode) {
				InputStreamReader input = null;
				// read payload
				if (gis != null)
					input = new InputStreamReader(gis);
				else
					input = new InputStreamReader(req.getInputStream());
				final char[] buffer = new char[MSG_PARSING_BUFFER];
				try {
					for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input
							.read(buffer, 0, buffer.length)) {
						output.append(buffer, 0, read);
					}
				} catch (IOException e) {
					continuation.resume();
					logger.error(null, "service", "AFT-DME2-6601",
							new ErrorContext()
									.add(REQUESTURI, buf.toString()).add(
											"initCause", e.getMessage()), e);
					resp.sendError(500, "UNABLE TO READ INPUT MESSAGE;Message="
							+ e.getMessage());
					resp.flushBuffer();
					continuation.complete();
					return;

				}
			}
			
			DME2Client client = null;
			String disableResponseStream = config2.getProperty(DME2Constants.AFT_DME2_DISABLE_INGRESS_REPLY_STREAM, "false");
			Enumeration<String> headerNames = req.getHeaderNames();
			Map<String,String> headerMap = new HashMap<String,String>();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				String value = req.getHeader(headerName);
				headerMap.put(headerName, value);
			}


			logger.debug( null, "service", " uniformResource.getOriginalURL().toString() : {}", uniformResource.getOriginalURI().toString());
			logger.debug( null, "service", " uniformResource.getSubContext().toString() : {}", uniformResource.getSubContext());
			RequestBuilder builder = new RequestBuilder(new URI(uniformResource.getOriginalURI().toString())).withHttpMethod("POST").withReadTimeout(endpointReadTimeout).withReturnResponseAsBytes(true).withAuthCreds("myrealm", username, password).withHeaders(headerMap).withLookupURL(uniformResource.getOriginalURI().toString());	
			
			if(uniformResource.getSubContext() != null){
				builder.withSubContext(uniformResource.getSubContext());
			}
			
			Request request = null;
			//client.setMethod(req.getMethod());
//			client.setCredentials(username, password);
			//@SuppressWarnings("unchecked")
	//		client.setHeaders(headerMap);
//			if(this.allowHttpReturnCode){
//				request.setAllowAllHttpReturnCodes(true);
//			}
			if(disableResponseStream.equalsIgnoreCase("true")){
				builder.withReturnResponseAsBytes(false);
			}
			else
			{
				builder.withReturnResponseAsBytes(true);
			}

			if(gis !=null) {
				builder.withHeader(config2.getProperty(DME2Constants.DME2_ACCEPT_ENCODING_KEY), config2.getProperty(DME2Constants.DME2_CLIENT_COMPRESS_TYPE));
			}
			if(subContextStr != null) {
				builder.withSubContext(subContextStr);
			}
			request = builder.build();
			client = new DME2Client(manager, request);
			client.setResponseHandlers(new DME2IngressProxyReplyHandler (uniformResource.toString(), continuation, convId,streamMode, config2));
			if(streamMode) {
				DME2StreamPayload payloadObj =  new DME2StreamPayload(req.getInputStream());
//				client.setPayload(payloadObj);
				client.send(payloadObj);
			}
			else{
//				client.send(new TextPayload(output.toString()));
//				client.setPayload(output.toString(), req.getContentType());
				logger.debug( null, "service", "Input from IngressProxy : {}", output.toString());
                client.send(new DME2TextPayload(output.toString(), req.getContentType()));
			}
		} catch (Exception e) {
			logger.error(null, "service", "AFT-DME2-6602",
					new ErrorContext().add(REQUESTURI,buf.toString())
										.add("initCause", e.getMessage()),e);
			resp.sendError(500,e.getMessage());
			resp.flushBuffer();
			return;
		} catch (Throwable e) {
			logger.error(null, "service", "AFT-DME2-6602",
					new ErrorContext().add(REQUESTURI,buf.toString())
										.add("initCause", e.getMessage()),e);
			resp.sendError(500,e.getMessage());
			resp.flushBuffer();
			return;
		}
	}

	private String genQueryString(Map<String, String> newQueryMap) {
		StringBuffer buf = new StringBuffer();
		for (String key: newQueryMap.keySet()) {
			if (buf.length() == 0) {
				buf.append(key + "=" + newQueryMap.get(key));
			} else {
				buf.append("&" + key + "=" + newQueryMap.get(key));
			}
		}
		return buf.toString();
	}
	
	private String getServiceName ( String servletPath) {
		String serviceName = null;
		int indexOfService = -1;
		String serviceStr = "/service=";
		indexOfService = servletPath.indexOf(serviceStr);
		// service= is found in the servletPath
		if(indexOfService != -1){
		// Identify the next field
		int indexOfNext = servletPath.indexOf("=",indexOfService+serviceStr.length());
			if(indexOfNext != -1){
				// 
				String temp = servletPath.substring(indexOfService, indexOfNext);
				String svc1 = temp.substring(temp.indexOf("=")+1, temp.lastIndexOf("/"));
				return svc1;
			}
		}
		return serviceName;
	}
	
	private String getField (String servletPath, String field){
		int indexOfField=-1;
		indexOfField = servletPath.indexOf(field);
		if(indexOfField != -1){
			// Identify the next field
			int indexOfNext = servletPath.indexOf("=",indexOfField+field.length());
				if(indexOfNext != -1){
					// 
					String temp = servletPath.substring(indexOfField, indexOfNext);
					String fieldVal = temp.substring(temp.indexOf("=")+1, temp.lastIndexOf("/"));
					return fieldVal;
				}
				else {
					String fieldVal = servletPath.substring(indexOfField+field.length());
					return fieldVal;
				}
		}
		return null;
	}
	
	public static void main(String a[]) {
	}
}