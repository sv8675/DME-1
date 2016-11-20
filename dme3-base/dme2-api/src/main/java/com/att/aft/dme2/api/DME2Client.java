/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;

import com.att.aft.dme2.api.util.DME2FileUploadInfo;
import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.handler.DefaultAsyncResponseHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.ContextFactory;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.FilePayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.request.RequestContext;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2GRMJVMRegistration;
import com.att.aft.dme2.util.DME2UrlStreamHandler;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.DME2ValidationUtil;
import com.att.aft.dme2.util.ErrorContext;

public class DME2Client implements DME2ClientIntf {
	RequestContext requestContext = null;
	RequestFacade requestFacade = null;
	private static final Logger logger = LoggerFactory.getLogger(DME2Client.class.getName());

	
	Request request = null;
    /** context path to replace the value coming from resolved SEP */
	private String context = null;
	
	/** subContext path to replace the value coming from resolved SEP */
	private String subContext = null;
	
	/** If this property is TRUE, DME2 will return the response as an OutputStream */
	private boolean returnResponseAsBytes = false;
	
	/** payLoda string */
	private String payload;
	private DME2Manager manager;
	private URI newUri;
	private long perEndpointTimeoutMs;
	private String charset;
	private String method;
	private Map<String, String> headers = null;
	private String contentType;
	private DME2Payload dme2Payload = null;
	private String queryParams;
	private String username;
	private String password;
	private AsyncResponseHandlerIntf asyncResponseHandlerIntf;

    private final static String  AFT_DME2_0605 =  "AFT-DME2-0605";
	/**
	 * Constructor with the DME2Manager and Request
	 * 
	 * @param mgr
	 * @param request
	 * @throws com.att.aft.dme2.api.DME2Exception
	 * @throws MalformedURLException 
	 */
	public DME2Client(DME2Manager mgr, Request request) throws DME2Exception, MalformedURLException {
		// call internal method to validate and do the initial setup
		logger.debug( null, "DME2Client", "DME2 URI:" + request.getLookupUri());
		validateAndInitialize(mgr, request);
	}
	
	/**
	 * 
	 * @param mgr
	 * @param request
	 * @throws DME2Exception
	 * @throws MalformedURLException
	 */
	private void validateAndInitialize(DME2Manager mgr, Request request) throws DME2Exception, MalformedURLException {
    logger.debug( null, "validateAndInitialize", LogMessage.METHOD_ENTER );
		// check if DME2Manager is valid
		RequestValidator.validate(mgr);
		// check for any property overrides and let the config manager know.
		// Validate the request object. Use a Validator object if required
		RequestValidator.validate(mgr.getConfig(), request);
		
		// get RequestContext and RequestFacade using ContextFactory for the given DME2Manager and Request
		ContextFactory contextFactory = ContextFactory.getInstance();
		this.requestContext = contextFactory.createContext(mgr, request);
		this.requestFacade = contextFactory.createFacade(this.requestContext);
    DME2GRMJVMRegistration.getInstance( mgr, request.getUniformResource() );
    logger.debug( null, "validateAndInitialize", LogMessage.METHOD_EXIT );
	}
	
	/**
	 * Method to submit the client request data return. It does not wait until the response or read timeout
	 * 
	 * @param payload
	 */
	public void send(DME2Payload payload) throws DME2Exception {
		requestFacade.send(payload);
	}
	
	/**
	 * Method to submit the client request data and waits (or until read timeout) for the response and return it
	 * 
	 * @param payload
	 * @return response data 
	 */
	public Object sendAndWait(DME2Payload payload) throws DME2Exception  {
		AsyncResponseHandlerIntf responseHandler = null;
		
		// check if client has custom response handler, if yes we will use that else we will create default one 
		if(requestContext.getRequest().getResponseHandler() == null) {
			// create default response handler and set in request
			responseHandler = new DefaultAsyncResponseHandler(requestContext.getMgr().getConfig(), requestContext.getUniformResource().getService(),false);
			requestContext.getRequest().setResponseHandler(responseHandler);
		} else {
			responseHandler = requestContext.getRequest().getResponseHandler(); 
		}
		send(payload);
		try {
			// get the response from response handler
			return responseHandler.getResponse(requestContext.getRequest().getReadTimeout());
		} catch (Exception e) {
			DME2Exception ex = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0707, e);
			logger.error(requestContext.getLogContext().getConversationId(), null, DME2Constants.EXP_CORE_AFT_DME2_0707, ex.getErrorMessage());
			throw ex;
		}
	}
	
	
	public void setResponseHandlers(AsyncResponseHandlerIntf responseHandler) {
		requestContext.getRequest().setResponseHandler(responseHandler);
	}

	public void stop() throws DME2Exception  {
		// TODO what logic we should implement here?
	}
	
	
	// New additions for supporting 2.0 method / interfaces
	




	/**
	 * Instantiates a new e http client.
	 * 
	 * @param newUri the uri
	 * @param perEndpointTimeoutMs the per endpoint timeout ms
	 * @throws DME2Exception the e http exception
	 * @throws MalformedURLException 
	 */
	public DME2Client(final URI newUri, long perEndpointTimeoutMs) throws DME2Exception {
	    this(null, newUri, perEndpointTimeoutMs, null, false);
	}
	
	public DME2Client(final URI newUri) throws DME2Exception {
	    this(null, newUri, -1, null, false);
	}

	public DME2Client(DME2Manager manager, final URI newUri, long perEndpointTimeoutMs) throws DME2Exception {
	    this(manager, newUri, perEndpointTimeoutMs, null, false);
	}
	
	public DME2Client(DME2Manager manager, final URI newUri) throws DME2Exception {
	    this(manager, newUri, -1, null, false);
	}

	public DME2Client(DME2Manager manager, final URI newUri, long perEndpointTimeoutMs, String charset) throws DME2Exception {
        this(manager, newUri, perEndpointTimeoutMs, charset, false);
	}
	
	public DME2Client(DME2Manager manager, final URI newUri, String charset) throws DME2Exception {
        this(manager, newUri, -1, charset, false);
	}
	
	public DME2Client(DME2Manager manager, final URI newUri, String charset, boolean returnResponseAsBytes) throws DME2Exception {
		this(manager, newUri, -1, charset, returnResponseAsBytes);
	}

	public DME2Client(DME2Manager manager, final URI newUri, long perEndpointTimeoutMs, String charset, boolean returnResponseAsBytes) throws DME2Exception {

		
		if(newUri != null)
			logger.debug( null, "DME2Client", "DME2 URI:" + newUri.toString());

		if (manager == null) {
				manager = DME2Manager.getDefaultInstance();

			}
			if (newUri == null) 
				throw new DME2Exception(AFT_DME2_0605, new ErrorContext().add("extendedMessage", "uri=null"));
			
			if (perEndpointTimeoutMs < 1) {
				perEndpointTimeoutMs = manager.getConfig().getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS, 240000);
			}
	

			this.manager = manager;
			this.newUri = newUri;
			this.perEndpointTimeoutMs = perEndpointTimeoutMs;
			this.charset = charset;
			this.returnResponseAsBytes = returnResponseAsBytes;
			
		    try {
		      DME2GRMJVMRegistration.getInstance( manager, new DmeUniformResource( manager.getConfig(), uriToURL( newUri ) ));
		    } catch ( MalformedURLException e ) {
		      throw new DME2Exception( DME2Constants.EXP_CORE_AFT_DME2_0707, e );
		    }
    }
	
	public DME2Client(DME2Manager manager, URI uri, String charset, boolean returnResponseAsBytes, boolean isEncoded) throws DME2Exception {
		this(manager, uri, -1, charset, returnResponseAsBytes, isEncoded);
	}

	public DME2Client(DME2Manager manager, URI uri, long perEndpointTimeoutMs, String charset, boolean returnResponseAsBytes, boolean isEncoded) throws DME2Exception {
		try {
			if (newUri == null) 
				throw new DME2Exception(AFT_DME2_0605, new ErrorContext().add("extendedMessage", "uri=null"));
			
			logger.debug( null, "DME2Client", "DME2 URI:" + newUri.toString());
			
			if (perEndpointTimeoutMs < 1) {
				perEndpointTimeoutMs = manager.getConfig().getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS, 240000);
			}

			String encodedStr = DME2Utils.encodeURIString(uri.toString().trim(), isEncoded);
			URI encodedURI = new URI(encodedStr);
			URL encodedURL = uriToURL(encodedURI);
			DME2ValidationUtil.validateServiceStringIsNonJDBCURL(encodedURL.toString());
			
			this.manager = manager;
			this.newUri = encodedURI;
			this.perEndpointTimeoutMs = perEndpointTimeoutMs;
			this.charset = charset;
			this.returnResponseAsBytes = returnResponseAsBytes;
			DME2GRMJVMRegistration.getInstance( manager, new DmeUniformResource( manager.getConfig(), uriToURL( uri ) ) );
		} catch (Exception e) {
			if (!(e instanceof DME2Exception)) {	
				throw new DME2Exception(DME2Constants.EXP_GEN_URI_EXCEPTION, new ErrorContext().add(DME2Constants.EXTENDED_STRING, e.getMessage()).add("URL", uri.toString()), e);
			} else {
				throw (DME2Exception) e;
			}
		}
	}
	
	public DME2Client(DME2Manager manager, URL url, String charset, boolean returnResponseAsBytes, boolean isEncoded) throws DME2Exception {
		this(manager, url, -1, charset, returnResponseAsBytes, isEncoded);
	}
	
	public DME2Client(DME2Manager manager, URL url, long perEndpointTimeoutMs, String charset, boolean returnResponseAsBytes, boolean isEncoded) throws DME2Exception {
		try {
			if (newUri == null) 
				throw new DME2Exception(AFT_DME2_0605, new ErrorContext().add("extendedMessage", "uri=null"));
			
			logger.debug( null, "DME2Client", "DME2 URI:" + newUri.toString());
			
			if (perEndpointTimeoutMs < 1) {
				perEndpointTimeoutMs = manager.getConfig().getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS, 240000);
			}

			String encodedStr = DME2Utils.encodeURIString(url.toString().trim(), isEncoded);
			URI encodedURI = new URI(encodedStr);
            URL encodedURL = uriToURL(encodedURI);
			DME2ValidationUtil.validateServiceStringIsNonJDBCURL(encodedURL.toString());
	
			this.manager = manager;
			this.newUri = encodedURI;
			this.perEndpointTimeoutMs = perEndpointTimeoutMs;
			this.charset = charset;
			this.returnResponseAsBytes = returnResponseAsBytes;
			DME2GRMJVMRegistration.getInstance( manager, request.getUniformResource() );
		} catch (Exception e) {
			if (!(e instanceof DME2Exception)) {	
				throw new DME2Exception(DME2Constants.EXP_GEN_URI_EXCEPTION, new ErrorContext().add(DME2Constants.EXTENDED_STRING, e.getMessage()).add("URL", url.toString()), e);
			} else {
				throw (DME2Exception) e;
			}
		}
	}
	
	private URL uriToURL(URI uri) throws MalformedURLException {
	    return new URL(uri.getScheme(), uri.getHost(), uri.getPort(), DME2Utils.appendQueryStringToPath(uri.getPath(), uri.getQuery()), new DME2UrlStreamHandler());
	}
	
	/**
	 * Send the message and wait for a response until the provided time passes
	 * @param timeoutMs
	 * @return
	 * @throws Exception
	 */
	public String sendAndWait(long timeoutMs) throws Exception {
		// build request object
		buildRequest();
		request.setReadTimeout(timeoutMs);
		// validate input data and do the initialization
		this.validateAndInitialize(manager, request);
		
		if(this.dme2Payload == null)
			this.dme2Payload = new DME2TextPayload(this.payload);
		
		return (String) this.sendAndWait(this.dme2Payload);
	}
	
	/**
	 * Send.
	 * 
	 * @throws DME2Exception
	 *             the DME2Exception
	 */
	public void send() throws DME2Exception {
		buildRequest();
		try {
			this.validateAndInitialize(manager, request);
		} catch (MalformedURLException e) {
			DME2Exception ex = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0707, e);
			logger.error(requestContext.getLogContext().getConversationId(), null, DME2Constants.EXP_CORE_AFT_DME2_0707, ex.getErrorMessage());
			throw ex;
		}
		if(this.dme2Payload == null)
			dme2Payload = new DME2TextPayload(this.payload);
		this.send(dme2Payload);
	}
	
	/**
	 * Method to build the Request object based on the data set in this class
	 * 
	 * @throws DME2Exception
	 */
	private void buildRequest() throws DME2Exception {
		request = new RequestBuilder(newUri).withLookupURL(newUri.toString().trim()).withHttpMethod(method)
				.withContext(context).withSubContext(subContext).withHeaders(headers).withQueryParams(queryParams)
				.withPerEndpointTimeoutMs(perEndpointTimeoutMs).withCharset(charset).withReturnResponseAsBytes(returnResponseAsBytes)
				.withResponseHandlers(asyncResponseHandlerIntf).withAuthCreds(manager.getRealm(), username, password)
				.build();
	}
	
	/**
	 * Sets the headers.
	 * 
	 * @param inHeaders
	 *            the headers
	 */
	public void setHeaders(Map<String, String> inHeaders) {
		if(this.headers == null)
			this.headers = new HashMap<String,String>();
		
		if(MapUtils.isNotEmpty(inHeaders)) {
			for(String header : inHeaders.keySet()) {
				this.headers.put(header, inHeaders.get(header));
			}
		}
	}
	
	public void addHeader(String name, String value) {
		if(headers == null)
			headers = new HashMap<String,String>();
		headers.put(name, value);
	}
	
	/**
	 * Set the HttpExchange HTTP Method
	 * 
	 * @param method
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Sets the payload.
	 * 
	 * @param payload
	 *            the new payload
	 */
	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	/**
	 * Sets the payload with an exact content type (i.e. "text/xml", "test/html", etc)
	 * @param payload
	 * @param contentType
	 */
	public void setPayload(String payload, String contentType) {
		this.payload = payload;
		this.contentType = contentType;
	}
	
	/**
	 * set DME2Payload object
	 * @param payload
	 */

	public void setDME2Payload(DME2Payload payload) {
		this.dme2Payload = payload;
	}
	
	/**
	 * set file for upload with request
	 * @param file
	 */
	public void setUploadFile(String file) {
        this.dme2Payload = new FilePayload(file, false, false);

	}
	
	
	/**
	 * set file for upload using multi part request
	 * @param fileName
	 * @param isFileTypeBinary
	 */
	public void setUploadFileWithMultiPart(String fileName, boolean isFileTypeBinary) {
        this.dme2Payload = new FilePayload( fileName, true, isFileTypeBinary );
	}
	
	/**
	 * set file for upload using multi part request
	 * @param  fileName
	 * @param  isFileTypeBinary
	 * @param  multipartName
	 */
	public void setUploadFileWithMultiPart(String fileName, boolean isFileTypeBinary,String multipartName) {
		this.dme2Payload = new FilePayload( fileName, true, isFileTypeBinary, multipartName );
	}
	
	/**
	 * set file for upload using multi part request
	 * @param  files
	 * @param  isFileTypeBinary
	 */
	public void setUploadFileWithMultiPart(List<DME2FileUploadInfo> files, boolean isFileTypeBinary) {
		List<String> filesUploadList = new ArrayList<String>();
		for(DME2FileUploadInfo file: files) {
			filesUploadList.add(file.getFilepath() + file.getFileName());
		}
		this.dme2Payload = new FilePayload( filesUploadList, true, isFileTypeBinary );
	}
	
	/**
	 * set file for upload using multi part request
	 * @param  file
	 * @param  isFileTypeBinary
	 */
	public void setUploadFileWithMultiPart(DME2FileUploadInfo file, boolean isFileTypeBinary) {
		List<String> filesUploadList = new ArrayList<String>();
		filesUploadList.add(file.getFilepath() + file.getFileName());
		this.dme2Payload = new FilePayload( filesUploadList, true, isFileTypeBinary );
	}
	
	public void setQueryParams(String queryParams) {
		this.queryParams = queryParams;
	}
	
	public void setQueryParams(Map<String,String> mapParams, boolean encode) {
		if(mapParams==null || mapParams.size()==0) {
			this.queryParams="";
			return;
		}
		StringBuffer sb = new StringBuffer(mapParams.size()*2);
		sb.append("?");
		for( Entry<String, String> e: mapParams.entrySet()) {
			try {
				sb.append(e.getKey())
				  .append("=")
				  .append(encode?
						  	URLEncoder.encode((e.getValue()!=null?e.getValue():""),Charset.forName("UTF-8").name())
						  	:(e.getValue()!=null?e.getValue():""))
				  .append("&");
			} catch(UnsupportedEncodingException uee) {
				logger.error("", null, "setQueryParams", uee.getMessage());
				throw new RuntimeException("Could not encode parameter: " + e.toString(), uee);
			}
		}
		this.queryParams = sb.toString();
	}
	
	public String getQueryParams() {
		return this.queryParams;
		
	}
	
	public void setSubContext(String subContext) {
		this.subContext = subContext;
	}
	
	@Deprecated
	public void setUrlQueryParams(String queryParams) {
		this.queryParams = queryParams;
	}
	
	@Deprecated
	public void setUrlQueryParams(Map<String,String> mapParams, boolean encode) {
		if(mapParams==null || mapParams.size()==0) {
			this.queryParams="";
			return;
		}
		StringBuffer sb = new StringBuffer(mapParams.size()*2);
		sb.append("?");
		for( Entry<String, String> e: mapParams.entrySet()) {
			try {
				sb.append(e.getKey())
				  .append("=")
				  .append(encode?
						  	URLEncoder.encode((e.getValue()!=null?e.getValue():""),Charset.forName("UTF-8").name())
						  	:(e.getValue()!=null?e.getValue():""))
				  .append("&");
			} catch(UnsupportedEncodingException uee) {
				logger.error("", null, "setQueryParams", uee.getMessage());
				throw new RuntimeException("Could not encode parameter: " + e.toString(), uee);
			}
		}
		this.queryParams = sb.toString();
	}
	
	@Deprecated
	public void setUrlContextPath(String urlContextPath) {
		this.context = urlContextPath;
	}
	
	public void setAllowAllHttpReturnCodes(Boolean allow) {
		if(allow.booleanValue())
			addHeader(DME2Constants.AFT_DME2_ALLOW_ALL_HTTP_RETURN_CODES, "true");
		else addHeader(DME2Constants.AFT_DME2_ALLOW_ALL_HTTP_RETURN_CODES, "false");
	}
	
	// dme:///C=US,o=SBC,ou=Training,cn=SOASample/1.0.14/PROD/APPLE/205977

	// FORMAT-1, SEARCHABLE
	// (http://DME2SEARCH/service=C=US,o=SBC,ou=Training,cn=SOASample/version=1.0.14/envContext=PROD/dataContext=205977/partner=APPLE
	// normal use case. resolve relevent endpoints using dataContext and partner

	// FORMAT-2, RESOLVABLE
	// (http://DME2RESOLVE/service=?/version=?/envContext=?/routeOffer=?
	// used to skip partion/partner/routeOffer resolution and go directly to a
	// routeOffer

	// FORMAT-3, DIRECT
	// (http://host:port/contextPath/subContextPath?queryParam1=val1
	// used to skip partion/partner/routeOffer resolution and go directly to a
	// routeOffer
	
	/**
	 * Sets the reply handler.
	 * 
	 * @param replyHandler
	 *            the new reply handler
	 */
	public void setReplyHandler(AsyncResponseHandlerIntf replyHandler) {
		this.asyncResponseHandlerIntf = replyHandler;
	}
	
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public void setContext(String context) {
		this.context = context;
	}
	
	public String getContext() {
		return this.context;
	}
}