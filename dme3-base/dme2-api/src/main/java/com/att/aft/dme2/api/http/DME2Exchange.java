/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ReplyHandler;
import com.att.aft.dme2.api.DME2StreamReplyHandler;
import com.att.aft.dme2.api.FailoverEndpointFactory;
import com.att.aft.dme2.api.FailoverFactory;
import com.att.aft.dme2.api.RequestProcessorIntf;
import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2ExchangeReplyHandler;
import com.att.aft.dme2.api.util.DME2ExchangeResponseContext;
import com.att.aft.dme2.api.util.DME2FailoverFaultHandler;
import com.att.aft.dme2.api.util.DME2FileUploadInfo;
import com.att.aft.dme2.api.util.DME2NullReplyHandler;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.handler.FailoverEndpoint;
import com.att.aft.dme2.handler.FailoverHandler;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.IteratorMetricsEvent;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.DmeUniformResource.DmeUrlType;
import com.att.aft.dme2.request.RequestContext;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2DateFormatAccess;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.ErrorContext;

public class DME2Exchange implements Listener {

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DME2Exchange.class.getName());
	private final long timeToAbandonRequest;

	private boolean returnResponseAsBytes = false;
	private DME2Configuration config;
	private DME2BaseEndpointIterator iterator;
	private DME2EndpointReference currentEndpointReference;
	/**
	 * boolean to avoid endpoints from being marked stale for EofException
	 */
	private boolean markStale = true;
	/**
	 * The messageID associated with this exchange. This will be set by the
	 * caller or generated if not set when execute() is called.
	 */
	private String messageID = null;
	/**
	 * The correlationID associated with this exchange. This will be set by the
	 * caller or null.
	 */
	private String correlationID = null;
	private String replyTo;
	/**
	 * Captures failures with ep's attempted *
	 */
	private final StringBuffer epTraceRoute = new StringBuffer();
	/**
	 * Check var for JMX interface response
	 */
	private boolean checkResponseContent = true;
	private int iGNORECONTENTLENGTHVALUE = 1;
	private String iGNORECONTENTTYPEVALUE = DME2Constants.RESPONSE_CONTENT_TYPE;
	public final static String EP = "[EP=";
	public final static String EXCEPTION = "exception";
	public final static String AFT_DME2_REQ_TRACE_INFO = "AFT_DME2_REQ_TRACE_INFO";
	public final static String SERVICE = "service";
	public final static String EPREFERENCES = "[EPREFERENCES=[%s]];";
	public final static String MINACTIVEENDPOINTS = "[MINACTIVEENDPOINTS=%s];";
	public final static String JMSMESSAGEID = "JMSMessageID";
	public final static String JMSCORRELATIONID = "JMSCorrelationID";
	public static final String AFT_DME2_0702 = "AFT_DME2_0702";
	public final static String AFT_DME2_0710 = "AFT-DME2-0710";
	public final static String SERVERURL = "serverURL";
	public final static String ENDPOINT_ELAPSED_MS = "EndpointElapsedMs";
	public final static String AFT_DME2_ROUNDTRIP_TIMEOUT_MS = "AFT_DME2_ROUNDTRIP_TIMEOUT_MS";
	public final static String AFT_DME2_EP_READ_TIMEOUT_MS = "AFT_DME2_EP_READ_TIMEOUT_MS";
	public final static String REQUESTURL = ";requestUrl=";
	public final static String AFT_DME2_0712 = "AFT-DME2-0712";
	public final static String HANDLER_NAME = "handlerName";
	public final static String CHAR_SET = "; charset=";
	public final static String AFT_DME2_0715 = "AFT-DME2-0715";
	public final static String INPUTFILE = "inputFile";
	private Boolean allowAllHttpReturnCodes = false;
	/**
	 * Set to true when we want to retry the current URL
	 */
	private boolean retryCurrentURL = false;
	private boolean isIgnoreFailoverOnExpire = false;
	/**
	 * Holds a manager-wide configuration of offers that failed and we published
	 * notices for already
	 */
	private static Set<String> globalNoticeCache = null;
	/**
	 * Marked on the first return from send successfully. Needed as exchange
	 * interaction calls are recursive so duplicate logs are generated.
	 */
	private boolean successAlready = false;
	/**
	 * Check var for request handler being invoked
	 */
	private boolean requestHandlersInvoked = false;
	/**
	 * Long requestHandlers elapsedTime *
	 */
	private long requestHandlersElapsedTime = 0;
	/**
	 * preferredVersion if set by requestHandlers
	 */
	private String preferredVersion;
	/**
	 * preferredRouteOffer if set by requestHandlers
	 */
	private String preferredRouteOffer;

	/**
	 * Check var for reply handler being invoked
	 */
	private boolean replyHandlersInvoked = false;
	/**
	 * The time that the execute method was first called
	 */
	private long executeStart;
	/**
	 * The URI the caller used to attempt this call
	 */
	private String lookupURI;
	/**
	 * Long replyHandlers elapsedTime *
	 */
	private long replyHandlersElapsedTime = 0;
	private String currentFinalUrl = "";
	/**
	 * Boolean used to know whether reply header needs to carry traceInfo *
	 */
	private boolean sendTraceInfo = false;
	/**
	 * Counts how many times we've recursed into the doTry method
	 */
	private final int recursiveCounter = 0;
	/**
	 * The time that the actual send to the last endpoint started
	 */
	private long sendStart;
	/**
	 * Tracking ID for the request
	 */
	private String trackingID;
	/**
	 * The exception.
	 */
	private Throwable exception = null;
	/**
	 * The response fields.
	 */
	private final HttpFields responseFields = new HttpFields();
	/**
	 * Below three variables used for streaming based response, where
	 * handleContent will be invoked mulitple times passing the status and
	 * headers
	 */
	private int responseStatus = -1;
	private Map<String, String> requestHeaders = new HashMap<String, String>();
	private Map<String, String> responseHeaders = new HashMap<String, String>();
	/**
	 * The headers.
	 */
	private Map<String, String> headers = new HashMap<String, String>();
	/**
	 * Allowed HTTP Status codes override by DME2URI query param *
	 */
	private String nonFailoverStatusCodesParam;
	/**
	 * Payload obj
	 */
	private DME2Payload payloadObj;
	/**
	 * The Constant NULL_REPLY_HANDLER.
	 */
	// private static final DefaultNullAsyncResponseHandler NULL_REPLY_HANDLER =
	// new DefaultNullAsyncResponseHandler();
	private AsyncResponseHandlerIntf responseHandler = null;

	private DME2Manager manager;
	private String charset = null;
	private String url = null;
	private String hostname = null;
	private int maxRecursiveCounter = 25;
	private String hostFromArgs = null;
	private boolean tRACEON = true;
	private long perEndpointTimeout = 10000;
	private String multiPartFile = null;
	private String multiPartFileName = null;
	private final List<String> multiPartFiles;
	private final List<DME2FileUploadInfo> fileUploadInfoList;
	private Boolean checkThrottleResponseContent = true;

	private RequestContext requestContext;

	private byte[] _responseContent;

	public RequestContext getRequestContext() {
		return requestContext;
	}

	public void setRequestContext(RequestContext requestContext) {
		this.requestContext = requestContext;
		responseHandler = requestContext.getRequest().getResponseHandler();
	}

	/**
	 * Captures what parameter is used for roundTrip timeout, used for logging
	 * purpose
	 */
	private String roundTripTimeoutString;
	/**
	 * Query string provided exchange round trip timeout *
	 */
	private long exchangeRoundTripTimeOut;

	/**
	 * partner name from request uri or msg header
	 */
	private String requestPartnerName;

	/**
	 * will have HTTP or JMS as value
	 */
	private String dme2InterfaceProtocol;

	/**
	 * flag indicating we have reached the max round trip value
	 */
	private boolean roundTripTimedout = false;

	/**
	 * Captures what parameter is used for timeout, used for logging purpose
	 */
	private String timeoutString;
	/**
	 * Query string provided endpoint read timeout *
	 */
	private long qendpointReadTimeOut;

	/**
	 * Query string provided connect timeout *
	 */
	private long connectTimeout;

	/**
	 * The client.
	 */
	private static HttpClient client = null;

	/**
	 * The Constant NULL_REPLY_HANDLER.
	 */
	private static final DME2ReplyHandler NULL_REPLY_HANDLER = new DME2NullReplyHandler();
	/**
	 * The reply handler.
	 */
	private DME2ReplyHandler replyHandler = NULL_REPLY_HANDLER;
	// DateFormat accessor
	private DME2DateFormatAccess dformat;
	private boolean isPreferLocalEPs;
	private boolean strictlyEnforceRoundTripTimeout;

	public void setPreferLocalEPs(boolean isPreferLocalEPs) {
		this.isPreferLocalEPs = isPreferLocalEPs;
		if(isPreferLocalEPs)
			this.epTraceRoute.append(EP + this.currentFinalUrl + ":preferredLocal];");

	}

	public long getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	DME2Exchange(DME2Manager manager, String lookupURI, long perEndpointTimeout, String charset,
			Map<String, String> _headers) throws DME2Exception {

		logger.debug(null, "DME2Exchange", LogMessage.METHOD_ENTER);
		this.manager = manager;
		this.config = manager.getConfig();
		dformat = new DME2DateFormatAccess(config);
		this.isIgnoreFailoverOnExpire = config.getBoolean(DME2Constants.AFT_DME2_IGNORE_FAILOVER_ONEXPIRE);
		this.checkResponseContent = config.getBoolean(DME2Constants.AFT_DME2_CLIENT_IGNORE_CONTENT_CHECK);
		this.maxRecursiveCounter = config.getInt(DME2Constants.AFT_DME2_CLIENT_MAX_RETRY_RECURSION);
		this.tRACEON = config.getBoolean(DME2Constants.AFT_DME2_HTTP_EXCHANGE_TRACE_ON);
		this.iGNORECONTENTLENGTHVALUE = config.getInt(DME2Constants.AFT_DME2_CLIENT_IGNORE_CONTENT_LENGTH_BYTE_SIZE);
		this.iGNORECONTENTTYPEVALUE = config.getProperty(DME2Constants.AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE);
		this.perEndpointTimeout = perEndpointTimeout;
		this.multiPartFiles = new ArrayList<String>();
		this.fileUploadInfoList = new ArrayList<DME2FileUploadInfo>();
		this.checkThrottleResponseContent = config.getBoolean(DME2Constants.AFT_DME2_THROTTLE_RESPONSE_CHECK);
		this.strictlyEnforceRoundTripTimeout = config.getBoolean( DME2Constants.AFT_DME2_STRICTLY_ENFORCE_ROUNDTRIP_TIMEOUT );
		this.timeToAbandonRequest = config.getLong( DME2Constants.AFT_DME2_TIME_TO_ABANDON_REQUEST );

		client = manager.getClient();

		/* Remove the queryParams from the URI String */
		this.lookupURI = stripQueryParamsFromURIString(lookupURI);
		this.currentFinalUrl = lookupURI;

		globalNoticeCache = manager.getGlobalNoticeCache();

		if (MapUtils.isNotEmpty(_headers)) {
			this.requestHeaders.putAll(_headers);
			this.headers.putAll(_headers);
		}

		try {
			hostname = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			logger.debug(null, "DME2Exchange", LogMessage.DEBUG_MESSAGE, "Exception", e);
			/* Ignoring Exception */
		}

		hostFromArgs = config.getProperty(DME2Constants.AFT_DME2_CONTAINER_HOST_KEY);

		this.charset = charset;
		if (this.charset == null) {
			this.charset = manager.getCharacterSet();
		}

		// make sure there is a MessageID
		if (this.headers == null || this.headers.get(JMSMESSAGEID) == null) {
			this.dme2InterfaceProtocol = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL);

		} else {
			this.dme2InterfaceProtocol = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL);
		}

		if ("true".equalsIgnoreCase(this.headers.get(DME2Constants.AFT_DME2_ALLOW_ALL_HTTP_RETURN_CODES))) {
			this.allowAllHttpReturnCodes = true;
		}

		logger.debug(null, "DME2Exchange", LogMessage.METHOD_EXIT);
	}

	public long getExchangeRoundTripTimeOut() {
		return this.exchangeRoundTripTimeOut;
	}

	public void setExchangeRoundTripTimeOut(long exchangeRoundTripTimeOut) {
		this.exchangeRoundTripTimeOut = exchangeRoundTripTimeOut;
	}

	private String stripQueryParamsFromURIString(String uriString) {
		int indexOfQuery = uriString.indexOf("?");

		if (indexOfQuery > 0) {
			return uriString.substring(0, indexOfQuery);
		}

		return uriString;
	}

	private void handleException(Map<String, String> headers, Throwable t) {
		try {
			responseHandler.handleException(headers, t);
		} catch (Exception e) {
			logger.warn(null, "handleException", LogMessage.EXCH_HANDLER_FAIL, EXCEPTION, replyHandler, e);
		}
	}

	@Override
	public void onBegin(Response response) {
	}

	@Override
	public boolean onHeader(Response response, HttpField field) {
		logger.debug(null, "onHeader", LogMessage.EXCH_RCV_HEADER, response, field, getURL());
		responseFields.add(field.getName(), field.getValue());
		return true;
	}

	@Override
	public void onHeaders(Response response) {
	}

	@Override
	public void onSuccess(Response response) {
		logger.debug(null, "onSuccess", LogMessage.METHOD_ENTER);
		logger.debug(null, "onSuccess", LogMessage.METHOD_EXIT);
	}

	@Override
	public void onFailure(Response response, Throwable x) {

		long executeComplete = System.currentTimeMillis();

		logger.debug(null, "onFailure", LogMessage.METHOD_ENTER);
		logger.debug(null, "onFailure", "ON_FAILURE_ENTER:{}", this.recursiveCounter);

		currentEndpointReference = iterator.getCurrentEndpointReference();
		// end failure collecting metrics
		if (iterator != null && iterator.getCurrentEndpointReference() != null) {
			iterator.endFailure(createIteratorMetricsEvent(null, iterator.getCurrentEndpointReference()));
		}
		this.epTraceRoute.append(EP + this.currentEndpointReference.getEndpoint() != null ? currentEndpointReference.getEndpoint().toURLString() : this.currentFinalUrl + ":onException=" + x.getMessage() + "];" );

		addTraceInfoToResponseHeaders();

		if (x.getMessage() != null && x.getMessage().contains("Connection refused")) {
			onConnectionFailed(x, response);
			return;
		} else if (x.getMessage() != null && x.getMessage().contains("Total timeout elapsed")) {

			if (this.payloadObj instanceof DME2StreamPayload) {
				ErrorContext ec = new ErrorContext();
				ec.add(SERVICE, lookupURI);
				ec.add(SERVERURL, this.getURL());
				ec.add(ENDPOINT_ELAPSED_MS, String.valueOf((executeComplete - sendStart)));

				Throwable exception = new DME2Exception("AFT-DME2-0718", ec);
				handleException(convertRequestHeadersAsMap(response.getRequest().getHeaders()), exception);
				return;
			}
			if (isIgnoreFailoverOnExpire()) {
				// There should not be any failover attempts if
				// ignoreFailoveronExpire is set to true reply with exception
				ErrorContext errCtx = new ErrorContext();
				errCtx.add(SERVICE, lookupURI);
				errCtx.add(SERVERURL, this.getURL());
				errCtx.add(ENDPOINT_ELAPSED_MS, String.valueOf(executeComplete - sendStart));

				exception = new DME2Exception(DME2Constants.DME2_IGNORE_FAILOVER_ONEXPIRE_MSGCODE, errCtx);
				response.getRequest().header(DME2Constants.AFT_DME2_REQ_TRACE_INFO, epTraceRoute.toString());
				invokeReplyHandlersFault(
						Integer.parseInt(
								config.getProperty(DME2Constants.AFT_DME2_EXCH_INVOKE_FAILED_RESP_CODE, "-10")),
						iterator.getCurrentDME2EndpointRouteOffer(), response.getRequest().getURI().getQuery(),
						response.getHeaders(), response.getRequest().getHeaders(), exception);
				handleException(convertRequestHeadersAsMap(response.getRequest().getHeaders()), exception);
				return;
			}
		}

		// end failure collecting metrics
		iterator.endSuccess(createIteratorMetricsEvent(null, currentEndpointReference));

		this.exception = x;

		int responseStatus = -1;

		if (this.payloadObj instanceof DME2StreamPayload) /*
		 * Throw exception
		 * if payload object
		 * is a stream
		 * payload we dont
		 * do failover for
		 * stream payload
		 */ {
			try {
				responseStatus = response.getStatus();
			} catch (Exception ex) {
				logger.debug(null, "onFailure", LogMessage.DEBUG_MESSAGE, "Exception", ex);
				// Ignore any error in getting response status
			}

			ErrorContext ec = new ErrorContext();
			ec.add(SERVICE, lookupURI);
			ec.add(SERVERURL, response.getRequest().getURI().getQuery());
			ec.add(ENDPOINT_ELAPSED_MS, (executeComplete - sendStart) + "");
			ec.add("HttpResponseStatus", "" + responseStatus);

			this.epTraceRoute.append(EP + this.currentFinalUrl + ":onException=" + x.getMessage() + "];");
			addTraceInfoToResponseHeaders();

			Throwable dme2Exception = null;
			if (responseStatus == 401) {
				dme2Exception = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0707, ec);
			} else {
				dme2Exception = new DME2Exception(DME2Constants.DME2_IGNORE_FAILOVER_STREAM_PAYLOAD_MSGCODE, ec, x);
			}
			Map<String, String> lheaders = convertResponseHeadersAsMap(response.getHeaders());
			handleException(lheaders, dme2Exception);
			return;
		}

		// invokeEndpointFaultHandlers()
		String routeOffer = null;
		if (currentEndpointReference != null && currentEndpointReference.getRouteOffer() != null
				&& currentEndpointReference.getRouteOffer().getRouteOffer() != null) {
			routeOffer = currentEndpointReference.getRouteOffer().getRouteOffer().getName();
		}

		invokeReplyHandlersEndPointFault(config.getInt(DME2Constants.AFT_DME2_EXCH_ON_EXCEPTION_RESP_CODE), routeOffer,
				response.getRequest().getURI().getQuery(), response.getHeaders(), response.getRequest().getHeaders(),
				x);

		/*
		 * Special handling for an occasional EofException that jetty can
		 * exhibit under load this is non-deterministic. We will just retry the
		 * endpoint once when it happens if (this.exception instanceof
		 * org.eclipse.jetty.io.EofException)
		 */
		Object obj = null;

		try {
			obj = Class.forName("org.eclipse.jetty.io.EofException");
		} catch (ClassNotFoundException e) {
			logger.debug(null, "onFailure", LogMessage.DEBUG_MESSAGE, "ClassNotFoundException", e);

			// Ignore any exception in loading class
		}

		if (this.exception instanceof org.eclipse.jetty.io.EofException
				|| (this.exception.getClass().isInstance(obj))) {
			if (retryCurrentURL) {
				retryCurrentURL = false;
			} else {
				if (config.getBoolean(DME2Constants.AFT_DME2_EXCHANGE_ALLOW_RETRY_CURR_URL)) {
					retryCurrentURL = true;
				} else {
					retryCurrentURL = false;
				}

				debugIt("ON_EXCEPTION_EOF_DOTRY", this.recursiveCounter + "");
				this.markStale = false;

				manager.getExchangeRetryThreadPool().submit(new DME2ExchangeRetry(this, response));
				return;
			}
		} else {
			this.markStale = true;
			retryCurrentURL = false;
			DME2Constants.setContext(trackingID, null);

			logger.error(null, "onFailure", "AFT-DME2-0705", new ErrorContext().add("ServerURL", this.getURL()),
					this.exception);
			logger.debug(null, "onFailure", "ON_EXCEPTION_DOTRY:{}", this.recursiveCounter);

			/*
			 * Commenting doTry below. onException might be called in the same
			 * thread execution where client.send was invoked and this lead to
			 * deadlock with mutex lock obtained on HttpConnection object So
			 * retry will be attempted on a new thread in case of onException
			 * scenario.
			 */

			manager.getExchangeRetryThreadPool().submit(new DME2ExchangeRetry(this, response));
			return;
		}

		logger.debug(null, "onFailure", LogMessage.METHOD_EXIT);
		this.markStale = true;

		// Set Endpoint stale.
		iterator.setStale();

		/*
		 * Commenting doTry below. onException might be called in the same
		 * thread execution where client.send was invoked and this lead to
		 * deadlock with mutex lock obtained on HttpConnection object So retry
		 * will be attempted on a new thread in case of onException scenario
		 */

		manager.getExchangeRetryThreadPool().submit(new DME2ExchangeRetry(this, response));
	}

	protected void onConnectionFailed(Throwable x, Response response) {
		logger.debug(null, "onConnectionFailed", "ON_CONNECTION_FAILED_ENTER", this.recursiveCounter);
		logger.debug(null, "onConnectionFailed", LogMessage.METHOD_ENTER);
		logger.debug(null, "onConnectionFailed", AFT_DME2_0710, new ErrorContext().add("ServerURL", this.getURL()), x);

		this.exception = x;

		/*
		 * Below change was required for scenario where a request fails
		 * onException if endpoint is shutdown abruptly which triggers a EOF
		 * Retry of the exception might enter onConnectionFailed with CONN
		 * REFUSED and retryCurrentURL being true stops failover from happening
		 * as it hits max recursive condition and no further endpoints to try
		 */

		/*
		 * If this is set to true, set it to false to prevent a retry from
		 * happening using the current URL that produced this onConnectionFailed
		 * scenario
		 */
		if (retryCurrentURL) {
			retryCurrentURL = false;
		}
		this.markStale = true;

		if (x.getMessage() != null) {
			debugIt("ON_CONNECTION_FAILED", x.getMessage());
			this.epTraceRoute.append(EP
					+ (getUrl() == null ? iterator.getCurrentEndpointReference().getEndpoint().toURLString() : getUrl())
					+ ":onConnectionFailed=" + x.getMessage() + "]; ");
		} else {
			debugIt("ON_CONNECTION_FAILED", x.toString());
			this.epTraceRoute.append(EP + currentFinalUrl + ":onConnectionFailed=" + x.toString() + "]; ");
		}

		// Set iterator element stale and remove it.
		iterator.setStale();
		iterator.remove();

		if (iterator != null && iterator.getCurrentEndpointReference() != null) {
			iterator.endFailure(createIteratorMetricsEvent(null, iterator.getCurrentEndpointReference()));
		}

		// invokeEndpointFaultHandlers
		invokeReplyHandlersEndPointFault(config.getInt(DME2Constants.AFT_DME2_EXCH_ON_EXCEPTION_RESP_CODE),
				(currentEndpointReference.getRouteOffer() == null || currentEndpointReference.getRouteOffer().getRouteOffer() == null) ? ""
						: currentEndpointReference.getRouteOffer().getRouteOffer().getName(),
						response.getRequest().getURI().getQuery(), response.getHeaders(), response.getRequest().getHeaders(),
						x);

		/*
		 * Commenting doTry below. onConnectionFailed() might be called in the
		 * same thread execution where client.send() was invoked and this lead
		 * to deadlock with mutex lock obtained on HttpConnection object, so
		 * retry will be attempted on a new thread in case of connectionFailed
		 * scenario
		 */
		addTraceInfoToResponseHeaders();
		manager.getExchangeRetryThreadPool().submit(new DME2ExchangeRetry(this, response));
		logger.debug(null, "onConnectionFailed", LogMessage.METHOD_EXIT);
	}

	private void addTraceInfoToResponseHeaders() {
		if (this.sendTraceInfo) {
			addToResponseHeader(AFT_DME2_REQ_TRACE_INFO, this.epTraceRoute.toString());
			logger.debug(null, "addTraceInfoToResponseHeaders", "epTraceRoute: {}", epTraceRoute);
		}
	}

	private void addTraceInfoToRequestHeaders() {
		if (this.sendTraceInfo) {
			addToRequestHeader(AFT_DME2_REQ_TRACE_INFO, this.epTraceRoute.toString());
			logger.debug(null, "addTraceInfoToRequestHeaders", "epTraceRoute: {}", epTraceRoute);
		}
	}

	private void addToResponseHeader(String code, String message) {
		//		if (null != responseHeaders) {
		//			boolean addtoMap = true;
		//			Set<String> mapKeys = responseHeaders.keySet();
		//			Iterator<String> keyIterator = mapKeys.iterator();
		//			while (keyIterator.hasNext()) {
		//				String key = keyIterator.next();
		//				String value = responseHeaders.get(key);
		//				if (message.equals(value)) {
		//					addtoMap = false;
		//					break;
		//				}
		//			}
		//			if (addtoMap) {
		//				responseHeaders.put(code, message);
		//			}
		//		}

		responseHeaders.put(code, message);
	}

	private synchronized void addToRequestHeader(String code, String message) {
		// if (null != requestHeaders) {
		// boolean addtoMap = true;
		// Set<String> mapKeys = requestHeaders.keySet();
		// Iterator<String> keyIterator = mapKeys.iterator();
		// while (keyIterator.hasNext()) {
		// String key = keyIterator.next();
		// String value = requestHeaders.get(key);
		// if (message.equals(value)) {
		// addtoMap = false;
		// break;
		// }
		// }
		// if (addtoMap) {
		// requestHeaders.put(code, message);
		// }
		// }
		//
		// // responseHeaders.put(code, message);
		// }
		requestHeaders.put(code, message);
	}

	private boolean isExpired(String responseContent) {
		boolean expired = false;
		if (responseContent != null && responseContent.contains("Continuation timed out")) {
			expired = true;
		}
		return expired;
	}

	private boolean checkIfCustomFailoverHandlerExists() {
		FailoverHandler failOverHandler = null;
		try {
			failOverHandler = loadFailoverHandler();
		} catch (DME2Exception e) {
			// TODO Auto-generated catch block
			logger.debug(null, "checkIfCustomFailoverHandlerExists", DME2Constants.ON_RESPONSE_COMPLETE, e);
			return false;
		}
		String failoverHandlerClassName = config.getProperty(DME2Constants.FAILOVER_HANDLER_IMPL);
		if (null != failOverHandler && failoverHandlerClassName.equals(failOverHandler.getClass().getName())) {
			return true;
		}
		return false;
	}

	@Override
	public void onComplete(Result result) {
		logger.debug(null, "onComplete", LogMessage.METHOD_ENTER);
		boolean failureMetricsEventRaised = false;
		long executeComplete = System.currentTimeMillis();
		logger.debug(null, "onComplete", "{}:{}", DME2Constants.ON_RESPONSE_STATUS, result.getResponse().getStatus());
		logger.debug(null, "onComplete", "{}:{}", DME2Constants.ON_RESPONSE_COMPLETE, recursiveCounter);

		currentEndpointReference = iterator.getCurrentEndpointReference();

		// end success collecting metrics
		if (currentEndpointReference != null) {
			iterator.endSuccess(createIteratorMetricsEvent(null, currentEndpointReference));
		}
		int responseStatus = result.getResponse().getStatus();
		if (200 != responseStatus) {
			boolean customFailOverHandlerExists = checkIfCustomFailoverHandlerExists();
			if (customFailOverHandlerExists) {
				boolean hasRetried = retryIfRequired(result);
				if (hasRetried) {
					return;
				}
			}
		}

		if (result.getResponse().getStatus() < 6) {
			logger.debug(null, "onResponseComplete", "{}:{}", DME2Constants.ON_RESPONSE_STATUS_EXIT,
					result.getResponse().getStatus());
			return;
		}

		// Caching getResponseStatus as multiple attempt for this method call
		// failed sporadically with errors as below:
		// Throwable occurred: java.lang.IllegalStateException: Response not
		// received yet at
		// com.att.aft.dme2.internal.jetty.client.CachedExchange.getResponseStatus(CachedExchange.java:41)
		// at
		// com.att.aft.dme2.api.DME2Exchange.onResponseComplete(DME2Exchange.java:1111)

		String responseContent = result.getResponse().getReason();
		// Check if exchange expired
		if (isExpired(responseContent)) {
			if (isIgnoreFailoverOnExpire()) {
				// There should not be any failover attempts if
				// ignoreFailoveronExpire is set to true reply with exception
				ErrorContext errCtx = new ErrorContext();
				errCtx.add(SERVICE, lookupURI);
				errCtx.add(SERVERURL, this.getURL());
				errCtx.add(ENDPOINT_ELAPSED_MS, String.valueOf(executeComplete - sendStart));

				exception = new DME2Exception(DME2Constants.DME2_IGNORE_FAILOVER_ONEXPIRE_MSGCODE, errCtx);
				result.getRequest().header(DME2Constants.AFT_DME2_REQ_TRACE_INFO, epTraceRoute.toString());
				invokeReplyHandlersFault(
						Integer.parseInt(
								config.getProperty(DME2Constants.AFT_DME2_EXCH_INVOKE_FAILED_RESP_CODE, "-10")),
						iterator.getCurrentDME2EndpointRouteOffer(), result.getRequest().getURI().getQuery(),
						result.getResponse().getHeaders(), result.getRequest().getHeaders(), exception);
				handleException(convertRequestHeadersAsMap(result.getRequest().getHeaders()), exception);
				return;
			}
		}

		byte[] responseContentBytes = responseContent != null ? responseContent.getBytes() : null;

		boolean isFailoverResponseCode = isFailoverResponseCode(responseStatus, this.allowAllHttpReturnCodes);

		epTraceRoute.append(EP + this.currentFinalUrl + ":onResponseCompleteStatus=" + responseStatus + "];");
		StringBuffer buffer = new StringBuffer();
		String delim = "";
		String pattern = "[RO:%s|SEQ:%s]";

		for (DME2EndpointReference ep : iterator.getEndpointReferenceList()) {
			if (ep.getEndpoint() != null && ep.getEndpoint().getRouteOffer() != null && ep.getRouteOffer() != null
					&& ep.getRouteOffer().getRouteOffer() != null) {
				buffer.append(delim);
				buffer.append(String.format(pattern, ep.getEndpoint().getRouteOffer(),
						ep.getRouteOffer().getRouteOffer().getSequence()));
				delim = ",";
			}
		}
		epTraceRoute.append(String.format(DME2Constants.EPREFERENCES, buffer));
		epTraceRoute.append(String.format(DME2Constants.MINACTIVEENDPOINTS, iterator.getMinActiveEndPoints()));

		addTraceInfoToResponseHeaders();

		// if REST failover
		if (isFailoverResponseCode && this.allowAllHttpReturnCodes) {
			// this passes through in 2.x ...
			try {
				doTry(result.getResponse());
			} catch (DME2Exception e) {
				// Do we want to do this?
				throw new RuntimeException(e);
			}
			return;
		}

		// If reply status code is 500 and PARSE_FAULT_REPLY is enabled
		if (responseStatus == 500 && config.getBoolean(DME2Constants.AFT_DME2_PARSE_FAULT) && !isFailoverResponseCode) {
			try {
				FailoverHandler failOverHandler = loadFailoverHandler();
				parseFaultResponse(failOverHandler, responseStatus, responseContent, responseContentBytes,
						executeComplete, result);
				return;
			} catch (Throwable e) {
				/* ignore any exception in parsing soap message or fault */
				logger.debug("", null, "onComplete", LogMessage.DEBUG_MESSAGE,
						DME2Constants.EXP_CORE_AFT_PARSE_FAULT_RES, e);
			}
		}

		// make sure we don't have any old exception lingering
		exception = null;

		logger.debug(null, "onComplete", "allowAllHttpReturnCodes: {} sendTraceInfo: {}", this.allowAllHttpReturnCodes,
				sendTraceInfo);

		// Authorization exception
		if (!allowAllHttpReturnCodes && responseStatus == 401 && !isFailoverResponseCode) {
			exception = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0707, new ErrorContext()
					.add(DME2Constants.SERVICE, lookupURI).add("ServerURL", result.getRequest().getURI().getQuery()));
			result.getRequest().header(DME2Constants.AFT_DME2_REQ_TRACE_INFO, epTraceRoute.toString());
			invokeReplyHandlersFault(config.getInt(DME2Constants.AFT_DME2_EXCH_INVOKE_FAILED_RESP_CODE),
					iterator.getCurrentDME2EndpointRouteOffer(), result.getRequest().getURI().getQuery(),
					result.getResponse().getHeaders(), result.getRequest().getHeaders(), exception);
			handleException(convertRequestHeadersAsMap(result.getRequest().getHeaders()), exception);
			return;
		}

		logger.debug(null, "onComplete", "allowAllHttpReturnCodes: {} sendTraceInfo: {}", this.allowAllHttpReturnCodes,
				sendTraceInfo);
		logger.debug(null, "onComplete", "config AFT_DME2_LOOKUP_NON_FAILOVER_SC: {}",
				config.getBoolean(DME2Constants.AFT_DME2_LOOKUP_NON_FAILOVER_SC));
		logger.debug(null, "onComplete", "isFailoverResponseCode(responseStatus): {}", isFailoverResponseCode);
		logger.debug(null, "onComplete", "responseStatus: {}", responseStatus);

		if (allowAllHttpReturnCodes || (config.getBoolean(DME2Constants.AFT_DME2_LOOKUP_NON_FAILOVER_SC)
				? !isFailoverResponseCode : (responseStatus == 200))) {
			logger.debug(null, "onResponseComplete", "{}:{}", DME2Constants.ON_RESPONSE_STATUS,
					result.getResponse().getStatus());
			DME2Constants.setContext(trackingID, null);

			Map<String, String> responseHeaderMap = convertResponseHeadersAsMap(result.getResponse().getHeaders());
			// byte[] responseContentInBytes =
			// result.getResponse().getReason().getBytes(); //
			// super.getResponseContentBytes();

			if (result.isSucceeded() && result.getResponse() instanceof ContentResponse) {
				ContentResponse contentResponse = (ContentResponse) result.getResponse();
				_responseContent = contentResponse.getContentAsString().getBytes();

			}

			// Validate for JMX port returned response
			// On CSI QC env, the service listen port on restart got assigned as
			// JMX port
			// for some other instance and jmx interface was still returning a 1
			// byte response
			// with 200 http response code
			checkResponseContent = config.getBoolean(DME2Constants.AFT_DME2_CLIENT_IGNORE_CONTENT_CHECK);
			if (checkResponseContent) {
				String conType = responseHeaderMap.get("Content-Type");
				if (conType == null) {
					conType = responseHeaderMap.get("Content-type");
				}

				if (conType == null) {
					conType = responseHeaderMap.get("content-type");
				}

				if (conType != null && _responseContent != null) {
					iGNORECONTENTLENGTHVALUE = config
							.getInt(DME2Constants.AFT_DME2_CLIENT_IGNORE_CONTENT_LENGTH_BYTE_SIZE);
					iGNORECONTENTTYPEVALUE = config
							.getProperty(DME2Constants.AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE);
					int responseContentLength = _responseContent.length;
					// JMX interface response returned 1 bytes and Content-type:
					// application/octet-stream we are ignoring it by default
					// unless checkResponseContent is turned true.
					if (responseContentLength == iGNORECONTENTLENGTHVALUE && conType.contains(iGNORECONTENTTYPEVALUE)) {
						logger.debug(null, "onResponseComplete",
								DME2Constants.ON_RESPONSE_IGNORE_CONTENT_LENGTH + ":" + responseContentLength);
						logger.debug(null, "onResponseComplete",
								DME2Constants.ON_RESPONSE_IGNORE_CONTENT_TYPE + ":" + conType);
						exception = new Exception("Request to [" + "http://" + result.getRequest().getURI()
								+ "] returned HTTP response, but with ignorable contentType [" + conType
								+ "] and contentLength [" + responseContentLength
								+ "]; validate that the endpoint is hosting a valid server port if no aother endpoints are available for failover");

						if (retryCurrentURL) {
							retryCurrentURL = false;
						}

						if (payloadObj instanceof DME2StreamPayload) {
							Throwable dme2Exception = new DME2Exception(
									DME2Constants.DME2_IGNORE_FAILOVER_STREAM_PAYLOAD_MSGCODE,
									new ErrorContext().add(DME2Constants.SERVICE, lookupURI)
									.add(DME2Constants.SERVERURL, result.getRequest().getURI().getQuery())
									.add(DME2Constants.ENDPOINT_ELAPSED_MS, (executeComplete - sendStart) + ""),
									exception);

							Map<String, String> lheaders = convertRequestHeadersAsMap(result.getRequest().getHeaders());
							handleException(lheaders, dme2Exception);
							return;
						}

						retryIfRequired(result);
						return;
					}
				}
			}

			if (!successAlready) {

				logger.debug(null, "onComplete", LogMessage.EXCH_SEND_URL, getURL(), timeoutString);

				if (iterator.getCurrentDME2EndpointRouteOffer() != null && globalNoticeCache
						.remove(lookupURI + ":" + iterator.getCurrentDME2RouteOffer().getService())) {
					logger.info("", null, "onComplete", LogMessage.EXCH_OFFER_RESTORE, lookupURI,
							iterator.getCurrentDME2RouteOffer().getSearchFilter());
				}
				successAlready = true;
			}

			// Glue handling code to ensure interop between JMS and non-JMS
			// clients/servers
			if (replyTo == null
					&& getPrefixStrippedHeaderValue(responseHeaderMap, DME2Constants.JMSDESTINATION) != null) {
				replyTo = (String) getPrefixStrippedHeaderValue(responseHeaderMap, DME2Constants.JMSDESTINATION);
			}
			if (replyTo != null) {
				responseHeaderMap.put(DME2Constants.JMSDESTINATION, replyTo);
			} else {/* Save reply queue to work correctly with non-JMS server */
				replyTo = (String) getPrefixStrippedHeaderValue(
						convertRequestHeadersAsMap(result.getRequest().getHeaders()), DME2Constants.JMS_REPLY_TO);
				if (replyTo != null) {
					responseHeaderMap.put(DME2Constants.JMSDESTINATION, replyTo);
				}
			}

			if (responseHeaderMap.get(DME2Constants.JMSCORRELATIONID) == null) {
				if (correlationID != null) {
					responseHeaderMap.put(DME2Constants.JMSCORRELATIONID, correlationID);
				} else {
					if (messageID != null) {
						responseHeaderMap.put(DME2Constants.JMSCORRELATIONID, messageID);
					}
				}
			}

			// Could be DME2HealthCheck response if response is null
			if (_responseContent != null) {
				logger.debug("", null, "onResponseComplete",
						DME2Constants.ON_RESPONSE_STATUS_200_REPLY + ":" + _responseContent.length);

				HttpFields responseHeaders = result.getResponse().getHeaders();
				HttpFields requestHeaders = result.getRequest().getHeaders();

				invokeReplyHandlers(responseStatus, iterator.getCurrentDME2EndpointRouteOffer(),
						result.getRequest().getURI().getQuery(), responseHeaders, requestHeaders);
				if (replyHandlersInvoked || requestHandlersInvoked) {
					logger.info(null, "onResponseComplete", LogMessage.EXCH_RECEIVE_HANDLERS,
							getURL(), responseStatus, (executeComplete - sendStart),
							(executeComplete - executeStart), preferredRouteOffer, preferredVersion,
							(requestHandlersInvoked ? requestHandlersElapsedTime : ""),
							(replyHandlersInvoked ? replyHandlersElapsedTime : ""), _responseContent.length);
				} else {
					logger.info(null, "onResponseComplete", LogMessage.EXCH_RECEIVE,
							getURL(), responseStatus, (executeComplete - sendStart),
							(executeComplete - executeStart), _responseContent == null ? 0 : _responseContent.length);
				}

				addTraceInfoToResponseHeaders();

				logger.debug(null, "onComplete", "inside handleReply convertResponseHeadersAsMap(responseHeaders): {}",
						convertResponseHeadersAsMap(responseHeaders));
				responseHandler.handleReply(responseStatus, "", new ByteArrayInputStream(_responseContent),
						convertRequestHeadersAsMap(requestHeaders), responseHeaderMap);

				// Post to metrics
				// TODO change to new metrics calling?

				/*
				 * try { long msgSize = responseContentInBytes.length;
				 * 
				 * HashMap<String, Object> props = new HashMap<String,
				 * Object>(); props.put(DME2Constants.MSG_SIZE, msgSize);
				 * props.put(DME2Constants.EVENT_TIME,
				 * System.currentTimeMillis());
				 * props.put(DME2Constants.REPLY_EVENT, true);
				 * props.put(DME2Constants.QUEUE_NAME, constructDME2ServiceStatsURI(this.lookupURI));
				 * props.put(DME2Constants.DME2_INTERFACE_PORT,
				 * this.getAddress().getPort() + "");
				 * props.put(DME2Constants.ELAPSED_TIME, (executeComplete -
				 * executeStart)); props.put(DME2Constants.MESSAGE_ID,
				 * this.messageID); props.put(DME2Constants.DME2_INTERFACE_ROLE,
				 * DME2Constants.DME2_INTERFACE_CLIENT_ROLE);
				 * props.put(DME2Constants.DME2_INTERFACE_PROTOCOL,
				 * this.dme2InterfaceProtocol);
				 * props.put(DME2Constants.REPLY_EVENT, true);
				 * 
				 * if (this.getRequestPartnerName() != null) {
				 * props.put(DME2Constants.DME2_REQUEST_PARTNER,
				 * this.getRequestPartnerName()); }
				 * 
				 * DME2Constants.debugIt("DmeExchange postReplyEvent ", props);
				 * manager.postStatEvent(props); } catch (Exception e1) {
				 * logger.error("", null, "onResponseComplete",
				 * e1.getMessage()); }
				 */
			} else {
				debugIt("ON_RESPONSE_STATUS_REPLY_SIZE", "0");
				logger.debug("", null, "onResponseComplete", "{}:0", DME2Constants.ON_RESPONSE_STATUS_REPLY_SIZE);

				logger.info(null, "onComplete", LogMessage.EXCH_RECEIVE, getURL(), responseStatus,
						(executeComplete - sendStart), (executeComplete - executeStart), 0);

				addTraceInfoToResponseHeaders();

				try {
					responseHandler.handleReply(responseStatus, "", new ByteArrayInputStream("".getBytes("UTF-8")),
							requestHeaders, responseHeaderMap);
				} catch (UnsupportedEncodingException e) {
					logger.warn("", null, "onComplete", LogMessage.EXCH_HANDLER_FAIL, "reply", replyHandler, e);
				}
			}

			// Make sure the Endpoint is removed from stale list if present -
			// its working now.
			iterator.removeStaleIteratorElement(currentEndpointReference.getEndpoint().getServiceEndpointID());
		} else {
			debugIt("ON_RESPONSE_EXCEPTION_RETURN_CODE", responseStatus);
			debugIt("ON_RESPONSE_EXCEPTION_RETURN_MESSAGE",
					_responseContent != null ? _responseContent.toString() : null);

			logger.debug(null, "onComplete", DME2Constants.ON_RESPONSE_EXCEPTION_RETURN_CODE + responseStatus);
			logger.debug(null, "onComplete", DME2Constants.ON_RESPONSE_EXCEPTION_RETURN_MESSAGE,
					_responseContent != null ? _responseContent.toString() : null);

			exception = new Exception(
					"Request to [" + "http://" + result.getRequest().getURI().getQuery() + "] returned HTTP ["
							+ responseStatus + "]; " + DME2Constants.EXP_CORE_AFT_VALIDATE_ENDPOINT_RUNNING);

			if (retryCurrentURL) {
				retryCurrentURL = false;
			}

			if (payloadObj instanceof DME2StreamPayload) {
				ErrorContext errCtx = new ErrorContext();
				errCtx.add(DME2Constants.SERVICE, result.getRequest().getURI().getQuery());
				Throwable dme2Exception = new DME2Exception(DME2Constants.DME2_IGNORE_FAILOVER_STREAM_PAYLOAD_MSGCODE,
						errCtx, exception);
				Map<String, String> lheaders = convertRequestHeadersAsMap(result.getRequest().getHeaders());
				responseHandler.handleException(lheaders, dme2Exception);
				return;
			}

			try {
				doTry(result.getResponse());
			} catch (DME2Exception e) {
			}
		}
		logger.debug(null, "onComplete", LogMessage.METHOD_EXIT);

	}

	private Object getPrefixStrippedHeaderValue(final Map headerMap, final String headerName) {
		Object returnValue = null;
		if (headerMap.get(headerName) != null) {
			returnValue = headerMap.get(headerName);
		} else if (headerMap.get(config.getProperty(DME2Constants.DME2_HEADER_PREFIX).concat(headerName)) != null) {
			returnValue = headerMap.get(config.getProperty(DME2Constants.DME2_HEADER_PREFIX).concat(headerName));
		}
		return returnValue;
	}

	private String[] getExchangeReplyHandlers() {
		String replyHandlers = this.headers.get(config.getProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS_KEY));
		debugIt("DME2Exchange.getExchangeReplyHandlers", replyHandlers);

		if (replyHandlers != null && replyHandlers.length() > 0) {
			// Found jms property in message that carries ignore from client
			try {
				String[] replyHandlersArr = replyHandlers.split(",");
				debugIt("REPLY_HANDLERS_CHAIN_HEADER_PROPERTY", replyHandlers + "");
				return replyHandlersArr;
			} catch (Exception e) {

				logger.debug(null, "getExchangeReplyHandlers", LogMessage.EXCH_READ_HANDLER_FAIL,
						"getExchangeReplyHandlers", e);
				return null;
			}
		} else {
			replyHandlers = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_EXCHANGE_REPLY_HANDLERS_KEY));
			if (replyHandlers != null && replyHandlers.length() > 0) {
				try {
					String[] replyHandlersArr = replyHandlers.split(",");
					debugIt("REPLY_HANDLERS_CHAIN_MGR_PROPERTY", replyHandlers + "");
					return replyHandlersArr;
				} catch (Exception e) {
					logger.debug(null, "getExchangeReplyHandlers", LogMessage.EXCH_READ_HANDLER_FAIL,
							"getExchangeReplyHandlers", e);
					return null;
				}
			}
		}
		return null;
	}

	private String[] getAllExchangeReplyHandlers() {
		return (String[]) ArrayUtils.addAll(getExchangeReplyHandlers(),
				DME2Utils.getFailoverHandlers(config, this.headers));
	}

	public Map<String, String> convertRequestHeadersAsMap(HttpFields httpFields) {
		Map<String, String> _headers = new HashMap<String, String>();
		if (httpFields == null) {
			return _headers;
		}
		Enumeration<String> e1 = httpFields.getFieldNames();
		while (e1.hasMoreElements()) {
			String key = e1.nextElement();
			_headers.put(key, httpFields.get(key));
		}

		if (MapUtils.isNotEmpty(requestHeaders)) {
			_headers.putAll(requestHeaders);
		}
		return _headers;
	}

	public Map<String, String> convertResponseHeadersAsMap(HttpFields httpFields) {
		Map<String, String> _headers = new HashMap<String, String>();
		if (httpFields == null) {
			return _headers;
		}
		Enumeration<String> e1 = httpFields.getFieldNames();
		while (e1.hasMoreElements()) {
			String key = e1.nextElement();
			if (key.equalsIgnoreCase("Content-Type")) {
				String charset = httpFields.get(key).substring(httpFields.get(key).indexOf("charset=") + "charset=".length()).trim();
				if (charset != null) {
					String s = charset.replaceAll("'", "").replaceAll("\"", "");
					_headers.put(key, httpFields.get(key).substring(0, httpFields.get(key).indexOf("charset=") + "charset=".length()) + s);
				}
			} else {
				_headers.put(key, httpFields.get(key));
			}
		}

		if (MapUtils.isNotEmpty(responseHeaders)) {
			_headers.putAll(responseHeaders);
		}
		return _headers;
	}

	public boolean retryIfRequired(Result result) {

		boolean isFailoverRequired = false;
		HttpResponse httpResponse = new HttpResponse();
		httpResponse.buildResponseObject(result);

		try {
			isFailoverRequired = FailoverFactory.getFailoverHandler(this.config).isFailoverRequired(httpResponse);
			if (isFailoverRequired) {
				doTry(result.getResponse());
			}
		} catch (DME2Exception e) {
			logger.error(null, "retryIfRequired", e.getMessage());

		}
		return isFailoverRequired;
	}

	// private HttpResponse buildHttpResponse(Result result) {
	// HttpResponse httpResponse = new HttpResponse();
	//
	// httpResponse.setReplyMessage(result.getResponse().toString());
	// httpResponse.setRespCode(result.getResponse().getStatus());
	// HttpFields httpFields = result.getResponse().getHeaders();
	// Map<String, String> headersMap = convertResponseHeadersAsMap(httpFields);
	// httpResponse.setRespHeaders(headersMap);
	// return httpResponse;
	//
	// }

	public void doTry(Response response) throws DME2Exception {
		logger.debug(null, "doTry", LogMessage.METHOD_ENTER);

		long roundTripTimeout = this.getRoundTripTimeout();
		boolean isEndpointResolved = false;

		/* Set the current element stale before proceeding to the next */
		if (!retryCurrentURL && !isThrottledResponse()) {
			iterator.setStale();
		}

		logger.debug(null, "doTry", "iterator.isAllElementsExhausted: {} roundTripTimeout: {}",
				iterator.isAllElementsExhausted(), roundTripTimeout);

		/* Check if all endpoints have been exhausted at this point. */
		if (iterator.isAllElementsExhausted()) {
			postStatisticsToMetrics(roundTripTimeout, response);

			addTraceInfoToRequestHeaders();
			String endpointsAttempted = this.epTraceRoute != null ? this.epTraceRoute.toString() : null;

			ErrorContext ec = new ErrorContext();
			ec.add(SERVICE, lookupURI);
			ec.add("roundTripTimeoutInMs", "" + roundTripTimeout);

			if (endpointsAttempted != null) {
				ec.add("endpointsAttempted", endpointsAttempted);
			}
			if (tRACEON) {
				ec.add("EndpointTrace", this.epTraceRoute.toString());
			}

			DME2Exception dme2Exception = new DME2Exception(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, ec);
			invokeReplyHandlersFault(config.getInt(DME2Constants.AFT_DME2_EXCH_INVOKE_FAILED_RESP_CODE),
					iterator.getCurrentDME2EndpointRouteOffer(), response.getRequest().getURI().getQuery(),
					response.getHeaders(), response.getRequest().getHeaders(), dme2Exception);
			handleException(convertResponseHeadersAsMap(response.getHeaders()), dme2Exception);
			return;
		}

		logger.debug(null, "doTry", "iterator.isAllElementsExhausted: {}", iterator.isAllElementsExhausted());
		logger.debug(null, "doTry", "iterator.getCurrentEndpointReference().getEndpoint().toURLString(): {}",
				iterator.getCurrentEndpointReference().getEndpoint().toURLString());
		logger.debug(null, "doTry", "roundTripTimeout: {}", roundTripTimeout);
		logger.debug(null, "doTry", "ElapsedTime < roundTripTimeout? : {}",
				((System.currentTimeMillis() - executeStart) < roundTripTimeout));

		/* Continue traversing thru the Iterator from wherever we left off */
		if (iterator.hasNext() && !roundTripTimedout) {
			// && ((System.currentTimeMillis() - executeStart) <
			// roundTripTimeout)) {
			// Log a message about the failed endpoint
			if (exception != null) {
				logger.debug(null, "doTry", LogMessage.EXCH_ENDPT_FAIL, getURL(), exception.toString());
			}

			/* Reset the Exchange status */
			try {
				logger.debug(null, "doTry", "DO_TRY_RESET");
				// this.reset();
			} catch (Exception e) {
				logger.debug(null, "doTry", "DO_TRY_RESET_FAILED", e);
			}

			RequestProcessorIntf requestProcessor = getRequestContext().getRequest().getRequestProcessor();
			DME2EndpointReference nextEndpoint = getCurrentEndpointReference();
			if (!retryCurrentURL) {
				// Advance the Iterator position and object and set the URL from
				// the next element.

				logger.debug(null, "doTry", "Inside if loop (!retryCurrentURL)");

				FailoverEndpoint endpoint = null;

				try {
					endpoint = FailoverEndpointFactory.getFailoverEndpointHandler(this.config);

					nextEndpoint = endpoint.getNextFailoverEndpoint(iterator, retryCurrentURL);

					setUrl(nextEndpoint.getEndpoint().toURLString());
					logger.debug(null, "doTry", "setUrl the while loop, nextEndpoint.getEndpoint().toURLString(): {}",
							nextEndpoint.getEndpoint().toURLString());

					// start collecting metrics
					iterator.start(createIteratorMetricsEvent(null, nextEndpoint));

					// add logic to handle if endpoint is not resolved.
					isEndpointResolved = requestProcessor.send(this.requestContext, nextEndpoint, this.payloadObj);

				} catch (DME2Exception e) {
					logger.error(null, DME2Exchange.class.getName(), DME2Constants.EXCEPTION_HANDLER_MSG, e);
					throw new DME2Exception(DME2Exchange.class.getName() + DME2Constants.EXCEPTION_HANDLER_MSG, e);

				}

			} else {
				logger.debug(null, "doTry", LogMessage.DEBUG_MESSAGE, "doTry() - Retrying current URL: " + getURL());
				setUrl(getURL());

				logger.debug(null, "doTry", LogMessage.EXCH_RETRY, getURL());

				DME2Constants.setContext(trackingID, null);
				if ( strictlyEnforceRoundTripTimeout ) {
					this.setReadTimeoutOnRetry();
				} else {
					this.setReadTimeout();
				}
				// Attempt to send the request.
				try {
					logger.debug(null, "doTry", "DO_TRY_CLIENT_SEND_ATTEMPT");
					this.sendStart = System.currentTimeMillis();
					// TODO: Ok, where does this really need to go?

					// start collecting metrics
					iterator.start(createIteratorMetricsEvent(null, nextEndpoint));
					this.getRequestContext().getRequest().getClientHeaders().put(DME2Constants.AFT_DME2_CLIENT_SEND_TIMESTAMP_KEY,
							dformat.convertDateToString(new Date()));
					// add logic to handle if endpoint is not resolved.
					isEndpointResolved = requestProcessor.send(this.requestContext, nextEndpoint, this.payloadObj);
					logger.debug(null, "doTry",
							"CLIENT_SEND_ATTEMPT_ELAPSED messageID={};correlationID={};URL={};elapsed={}", this.messageID,
							this.correlationID, this.getURL(), (System.currentTimeMillis() - sendStart));
				} catch (IllegalStateException e) {
					logger.debug(null, "doTry", "ILLEGAL_STATE_EXCEPTION_IGNORABLE" + e.toString(), e);
				} catch (Throwable th) {
					logger.debug(null, "doTry", "DO_TRY_CLIENT_SEND_THROWABLE", th.toString());
					this.exception = th;
				}
			}

		} // End while loop
		String conversationID = requestContext.getLogContext().getConversationId();
		if (!isEndpointResolved) {
			// implement callback let the consumer know about none of the
			// endpoint is resolved.
			/* Throw Exception */
			ErrorContext ec = new ErrorContext();
			ec.add(DME2Constants.SERVICE, requestContext.getRequest().getLookupUri());

			DME2Exception e = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0702, ec);
			logger.error(conversationID, null, DME2Constants.EXP_CORE_AFT_DME2_0702 + "{}", e.getErrorMessage());
			responseHandler.handleException(convertResponseHeadersAsMap(response.getHeaders()), e);
			throw e;
		}

		if (((System.currentTimeMillis() - executeStart) > roundTripTimeout)) {
			logger.debug(null, "doTry", "DO_TRY_ROUNDTRIP_TIMEOUT_REACHED");
			roundTripTimedout = true;
		}

		/*
		 * If this is true, the round trip timeout limit was exceed. Throw
		 * exception
		 */
		if (roundTripTimedout) {
			logger.debug(null, "doTry", "DO_TRY_THROW_ROUNDTRIP_TIMEDOUT_EXCEPTION");
			addTraceInfoToResponseHeaders();

			Throwable dme2Exception = new DME2Exception("AFT-DME2-0713",
					new ErrorContext().add(SERVICE, lookupURI).add("roundTripTimeOutInMs", this.roundTripTimeoutString)
					.add("timedOutAfter", "" + (System.currentTimeMillis() - executeStart)));

			this.invokeReplyHandlersFault(config.getInt(DME2Constants.INVOKE_FAILED_RSP_CODE),
					iterator.getCurrentDME2EndpointRouteOffer(), this.getURL(), response.getHeaders(),
					response.getRequest().getHeaders(), dme2Exception);
			handleException(this.headers, dme2Exception);
		}
		logger.debug(null, "doTry", LogMessage.METHOD_EXIT);

	}

	private boolean isThrottledResponse() {
		if (checkThrottleResponseContent) {
			// Why would this be needed?
			/*
			 * String responseContent = null; try { responseContent =
			 * this.getResponseContent(); } catch (UnsupportedEncodingException
			 * e) { debugIt("IS_THROTTLED_RESPONSE_THROWABLE", e.getMessage());
			 * }
			 */
			return DME2Constants.DME2_ERROR_CODE_429 == this.getResponseStatus();
		} else {
			return false;
		}
	}

	private IteratorMetricsEvent createIteratorMetricsEvent(final String conversationId,
			final DME2EndpointReference orderedEndpointHolder) {
		String role = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE);// DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE;
		IteratorMetricsEvent iteratorMetricsEvent = new IteratorMetricsEvent();

		if (orderedEndpointHolder != null && orderedEndpointHolder.getEndpoint() != null) {
			iteratorMetricsEvent.setClientIp(orderedEndpointHolder.getEndpoint().getHost());
			iteratorMetricsEvent.setProtocol(this.dme2InterfaceProtocol);
			iteratorMetricsEvent.setServiceUri(orderedEndpointHolder.getEndpoint().toURLString());
		}
		iteratorMetricsEvent.setConversationId(conversationId);
		iteratorMetricsEvent.setRole(role);
		iteratorMetricsEvent.setEventTime(System.currentTimeMillis());
		iteratorMetricsEvent.setPartner(getRequestPartnerName());

		return iteratorMetricsEvent;
	}

	private void postStatisticsToMetrics(long roundTripTimeout, Response response) {

		logger.debug(null, "postStatisticsToMetrics", "DO_TRY_ENDPOINTS_EXHAUSTED");

		try {
			/* Post request statistics to Metrics Service */
			HashMap<String, Object> props = new HashMap<String, Object>();
			props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
			props.put(DME2Constants.FAULT_EVENT, true);
			props.put(DME2Constants.QUEUE_NAME, constructDME2ServiceStatsURI(this.lookupURI));
			props.put(DME2Constants.ELAPSED_TIME, (System.currentTimeMillis() - executeStart));
			props.put(DME2Constants.MESSAGE_ID, this.messageID);

			props.put(DME2Constants.DME2_INTERFACE_PROTOCOL, this.dme2InterfaceProtocol);
			props.put(DME2Constants.DME2_INTERFACE_ROLE,
					config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
			props.put(DME2Constants.DME2_INTERFACE_PORT, response.getRequest().getPort() + "");
			props.put(DME2Constants.FAULT_EVENT, true);

			if (this.getRequestPartnerName() != null) {
				props.put(DME2Constants.DME2_REQUEST_PARTNER, this.getRequestPartnerName());
			}

			manager.postStatEvent(props);
			return;
		} catch (Exception e1) {
			ErrorContext ec = new ErrorContext();
			ec.add("Code", "DME2Client.Fault");
			ec.add("extendedMessage", e1.getMessage());
			logger.debug(null, "postStatisticsToMetrics", "AFT-DME2-5101", ec);
		}
	}

	private String getURL() {
		return this.currentFinalUrl;
	}

	/**
	 * Set endpoint read timeout for client call Setting up
	 * AFT_DME2_EP_READ_TIMEOUT_MS will be allowed as below. 1) in the root
	 * config file. applies to ALL services unless its overriden below.
	 * Overrides a coded default. 2) in the queue URI as a query parameter;
	 * overrides #1 3) as a JMS property (dme2-jms) or HTTP header property
	 * (dme2-api); overrides #2
	 */
	private void setReadTimeout() {
		String jmsPropertyEndpointTimeout = this.headers.get(AFT_DME2_EP_READ_TIMEOUT_MS);
		long timeout = 0;
		if (jmsPropertyEndpointTimeout != null) {
			// Found jms property in message that carries timeout from client
			try {
				timeout = Long.parseLong(jmsPropertyEndpointTimeout);
			} catch (Exception e) {
				// use default
				timeout = config.getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS);
			}
			this.timeoutString = "JMSHeader-AFT_DME2_EP_READ_TIMEOUT_MS=" + timeout;
		} else if (this.qendpointReadTimeOut > 0) {
			// Found jms property in message that carries timeout from client
			this.timeoutString = "URIQueryString-endpointReadTimeout=" + this.qendpointReadTimeOut;
			timeout = this.qendpointReadTimeOut;
		} else {
			// Use default value
			timeout = config.getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS);
			this.timeoutString = "CFG-AFT_DME2_EP_READ_TIMEOUT_MS=" + timeout;
		}
		debugIt("EXECUTE_SET_READ_TIMEOUT", this.timeoutString);

		// Set Connect timeout
		if (this.getConnectTimeout() > 0) {
			client.setConnectTimeout((int) this.getConnectTimeout());
			this.setConnectTimeout((int) this.getConnectTimeout());
		}
	}

	private void setReadTimeoutOnRetry() {
		String jmsPropertyEndpointTimeout = this.headers.get( AFT_DME2_EP_READ_TIMEOUT_MS );
		long timeout = 0;
		if ( jmsPropertyEndpointTimeout != null ) {
			// Found jms property in message that carries timeout from client
			try {
				timeout = Long.parseLong( jmsPropertyEndpointTimeout );
			} catch ( Exception e ) {
				// use default
				timeout = config.getLong( AFT_DME2_EP_READ_TIMEOUT_MS );
			}
			this.timeoutString = "JMSHeader-AFT_DME2_EP_READ_TIMEOUT_MS=" + timeout;
		} else if ( this.qendpointReadTimeOut > 0 ) {
			// Found jms property in message that carries timeout from client
			this.timeoutString = "URIQueryString-endpointReadTimeout=" + this.qendpointReadTimeOut;
		}
		long timeLeft = ( this.getRoundTripTimeout() - ( System.currentTimeMillis() - executeStart ) );
		long minTimeout = Math.min( timeout, timeLeft );
		if ( minTimeout < timeout ) {
			debugIt( "EXECUTE_SET_READ_TIMEOUT_ON_RETRY_TIME_LEFT {}", String.valueOf( timeLeft ) );
			if ( minTimeout > this.timeToAbandonRequest ) {
				if ( this.getConnectTimeout() > 0 ) {
					debugIt( "EXECUTE_CLIENT_SPECIFIED_CONNECT_TIMEOUT {}", this.getConnectTimeout() + "" );
					long finalConnectTimeout = Math.min( this.getConnectTimeout(), minTimeout );
					if ( minTimeout > finalConnectTimeout ) {
						minTimeout = ( minTimeout - finalConnectTimeout );
					}
					debugIt( "EXECUTE_SET_CONNECT_TIMEOUT_ON_RETRY {}", String.valueOf( finalConnectTimeout ) );
					client.setConnectTimeout( (int) finalConnectTimeout );
					this.setConnectTimeout( (int) finalConnectTimeout );
				}
				this.addToRequestHeader( AFT_DME2_EP_READ_TIMEOUT_MS, minTimeout + "" );
				client.setConnectTimeout( minTimeout );
				this.setConnectTimeout( minTimeout );
				debugIt( "EXECUTE_SET_READ_TIMEOUT_ON_RETRY", minTimeout + "" );
			} else {
				debugIt( "EXECUTE_NEXT_RETRY_HAS_TOO_LITTLE_TIME", String.valueOf( minTimeout ) );
				roundTripTimedout = true;
				debugIt( "EXECUTE_SET_ROUNDTRIP_TIMEOUT_AS_TRUE" );
			}

		} else {
			this.setReadTimeout();
		}
	}

	public String getRequestPartnerName() {
		if (requestPartnerName != null) {
			return requestPartnerName;
		} else {
			String partner = this.headers.get("com.att.aft.dme2.partner");
			if (partner == null) {
				partner = this.headers.get("com.att.aft.dme2.jms.partner");
			}
			if (partner == null) {
				partner = requestContext.getRequest().getClientHeaders().get(DME2Constants.DME2_REQUEST_PARTNER);
			}
			requestPartnerName = partner;
			return partner;
		}
	}

	private long getRoundTripTimeout() {
		String jmsPropertyRTTimeout = this.headers.get(AFT_DME2_ROUNDTRIP_TIMEOUT_MS);
		long timeout = 0;
		if (jmsPropertyRTTimeout != null) {
			// Found jms property in message that carries timeout from client
			try {
				timeout = Long.parseLong(jmsPropertyRTTimeout);
			} catch (Exception e) {
				// use default
				timeout = config.getLong(DME2Constants.AFT_DME2_ROUNDTRIP_TIMEOUT_MS);

			}
			this.roundTripTimeoutString = "JMSHeader-AFT_DME2_ROUNDTRIP_TIMEOUT_MS=" + timeout;
		} else if (this.exchangeRoundTripTimeOut > 0) {
			// Found jms property in message that carries timeout from client
			this.roundTripTimeoutString = "URIQueryString-roundTripTimeout=" + this.exchangeRoundTripTimeOut;
			timeout = this.exchangeRoundTripTimeOut;
		} else {
			// Use default value
			timeout = config.getLong(DME2Constants.AFT_DME2_ROUNDTRIP_TIMEOUT_MS);
			this.roundTripTimeoutString = "CFG-AFT_DME2_ROUNDTRIP_TIMEOUT_MS=" + timeout;

		}
		// this.addRequestHeader(AFT_DME2_ROUNDTRIP_TIMEOUT_MS, timeout + "");
		debugIt("EXECUTE_SET_ROUNDTRIP_TIMEOUT", this.roundTripTimeoutString);

		return timeout;
	}

	@Override
	public void onContent(Response response, ByteBuffer content) {
		// no need to implement this method as we are implementing
		// onContent(Response response, ByteBuffer content, Callback callback)
	}

	private byte[] mergeChunkData(ByteBuffer content) {
		ByteArrayOutputStream outputStream = null;
		try {
			outputStream = new ByteArrayOutputStream();
			if (_responseContent != null && _responseContent.length > 0) {
				outputStream.write(_responseContent);
			}
			outputStream.write(BufferUtil.toArray(content));
			return outputStream.toByteArray();
		} catch (IOException e) {
		}
		return null;
	}

	@Override
	public void onContent(Response response, ByteBuffer content, Callback callback) {
		logger.debug(null, "onContent", LogMessage.METHOD_ENTER);
		logger.debug(null, "onContent", "!isReturnResponseAsBytes(): {}", !isReturnResponseAsBytes());
		if (!isReturnResponseAsBytes()) {
			logger.debug(null, "onContent", "Content1");
			String disableIngressResponseStream = config.getProperty(DME2Constants.AFT_DME2_DISABLE_INGRESS_REPLY_STREAM);

			if (disableIngressResponseStream != null && disableIngressResponseStream.equalsIgnoreCase("true")) {
				_responseContent = mergeChunkData(content);

			} else if (!(replyHandler instanceof DME2StreamReplyHandler)) {
				_responseContent = mergeChunkData(content);
			}

		} else {
			logger.debug(null, "onContent", "Content2");
			if (response.getStatus() == 200) {
				if (this.responseStatus == -1) {
					responseStatus = response.getStatus();
					requestHeaders = convertRequestHeadersAsMap(response.getRequest().getHeaders());
					responseHeaders = convertResponseHeadersAsMap(response.getHeaders());
				}
				// SCLD-4335, invoking handleContent with status, headers
				// passed.
				// We abstracted handleContent(bytes []) prior to this change
				// and are expecting
				// applications to implement the abstract method

				// ((DME2StreamReplyHandler)
				// replyHandler).handleContent(content.asArray());

				byte[] bytes = new byte[content.remaining()];
				content.get(bytes);

				if (responseHandler instanceof DME2StreamReplyHandler) {
					((DME2StreamReplyHandler) responseHandler).handleContent(bytes, this.responseStatus,
							this.requestHeaders, this.responseHeaders);
				}
			} else {
				_responseContent = BufferUtil.toArray(content);
			}

		}

		callback.succeeded();
	}

	/**
	 * Checks responseStatus against codes that are not determined to result in
	 * failover By Default 200 and 401 status codes will not be attempted
	 * failover and all other status codes will be attempted failover
	 *
	 * @param responseStatus
	 * @return
	 */
	private boolean isFailoverResponseCode(int responseStatus, boolean isRestfulCall) {
		// Ignore looking up status code from config and return false always, so
		// that
		// the code satisfies this condition and ignores new functionality of
		// overrding
		// default successful http status codes
		if (!config.getBoolean(DME2Constants.AFT_DME2_LOOKUP_NON_FAILOVER_SC)) {
			logger.debug(null, "isFailoverResponseCode", "{}:false", DME2Constants.AFT_DME2_LOOKUP_NON_FAILOVER_SC);
			return false;
		}

		String scCodes = getNonFailoverStatusCodes(isRestfulCall);
		if (scCodes != null) {
			String scCodesArr[] = scCodes.split(",");
			String rs = responseStatus + "";
			for (int i = 0; i < scCodesArr.length; i++) {
				// each code can be a range, if separated by a dash eg: 500-599
				// for all 5xx codes
				String sc = scCodesArr[i].trim();
				if (sc.equalsIgnoreCase(rs)) {
					return false;
				} else if (sc.contains("-")) {
					try {
						String[] range = sc.split("-");
						int low = Integer.parseInt(range[0]);
						int high = Integer.parseInt(range[1]);
						if (low <= responseStatus && responseStatus <= high) {
							return false;
						}
					} catch (Exception ex) { // ignore any bad format
					}
				}
			}
		}
		return true;
	}

	/**
	 * return list of accepted status codes, that DME2 will not attempt failover
	 *
	 * @return
	 */
	private String getNonFailoverStatusCodes(boolean isRestfulCall) {
		// Check if header carries a value for each request
		String nonFailoverStatusCodes = this.headers.get(DME2Constants.AFT_DME2_NON_FAILOVER_HTTP_SCS);
		if (nonFailoverStatusCodes != null) {
			logger.debug(null, "getSuccessStatusCodes", "{}:{}", DME2Constants.AFT_DME2_NON_FAILOVER_HTTP_SCS_HEADER,
					nonFailoverStatusCodes);
			return nonFailoverStatusCodes;
		}
		// Check if query param override is provided as part of DME2 URI
		if (this.getNonFailoverStatusCodesParam() != null) {
			logger.debug(null, "getSuccessStatusCodes", "{}:{}",
					DME2Constants.AFT_DME2_NON_FAILOVER_HTTP_SCS_QUERYPARAM, this.getNonFailoverStatusCodesParam());
			return this.getNonFailoverStatusCodesParam();
		}
		// Check if a -D or AFT config file override is provided, else use
		// default value
		if (isRestfulCall) {
			nonFailoverStatusCodes = config.getProperty(DME2Constants.AFT_DME2_NON_FAILOVER_HTTP_REST_SCS);
			logger.debug(null, "getNonFailoverStatusCodes", "{}:{}",
					DME2Constants.AFT_DME2_NON_FAILOVER_HTTP_REST_SCS_DEFAULT, nonFailoverStatusCodes);
		} else {
			nonFailoverStatusCodes = config.getProperty(DME2Constants.AFT_DME2_NON_FAILOVER_HTTP_SCS);
			logger.debug(null, "getSuccessStatusCodes", "{}:{}", DME2Constants.AFT_DME2_NON_FAILOVER_HTTP_SCS_DEFAULT,
					nonFailoverStatusCodes);
		}
		return nonFailoverStatusCodes;
	}

	public String getNonFailoverStatusCodesParam() {
		return nonFailoverStatusCodesParam;
	}

	public void setNonFailoverStatusCodesParam(String nonFailoverStatusCodesParam) {
		this.nonFailoverStatusCodesParam = nonFailoverStatusCodesParam;
	}

	public Boolean getAllowAllHttpReturnCodes() {
		return allowAllHttpReturnCodes;
	}

	public void setAllowAllHttpReturnCodes(Boolean allowAllHttpReturnCodes) {
		this.allowAllHttpReturnCodes = allowAllHttpReturnCodes;
	}

	private boolean invokeReplyHandlersEndPointFault(int responseCode, String cOffer, String requestUrl,
			HttpFields responseHeaders, HttpFields requestHeaders, Throwable e) {
		if (!config.getBoolean(DME2Constants.DME2_ALLOW_INVOKE_HANDLERS)) {
			return false;
		}
		currentEndpointReference = iterator.getCurrentEndpointReference();

		String[] replyHandlers = getAllExchangeReplyHandlers();
		if (replyHandlers != null) {
			// Create reply context object
			String routeOffer = null;
			String version = null;
			if (this.currentEndpointReference != null && this.currentEndpointReference.getEndpoint() != null) {
				routeOffer = this.currentEndpointReference.getEndpoint().getRouteOffer();
				version = this.currentEndpointReference.getEndpoint().getServiceVersion();
			}
			DME2ExchangeFaultContext ctxData = new DME2ExchangeFaultContext(this.lookupURI, responseCode,
					convertRequestHeadersAsMap(requestHeaders), routeOffer, version, this.lookupURI, e);
			logger.debug(null, "invokeReplyHandlersEndPointFault", "ResponseCode={};routeOffer={}{}{}", responseCode,
					routeOffer, REQUESTURL, requestUrl);
			for (int i = 0; i < replyHandlers.length; i++) {
				long start = System.currentTimeMillis();
				String handlerName = replyHandlers[i];
				try {

					Object obj = DME2Utils.loadClass(config, this.getURL(), handlerName);
					if (obj instanceof DME2ExchangeReplyHandler) {
						DME2ExchangeReplyHandler handler = (DME2ExchangeReplyHandler) obj;
						handler.handleEndpointFault(ctxData);
						logger.debug(null, "invokeReplyHandlersEndPointFault" + handlerName,
								LogMessage.EXCH_INVOKE_HANDLER);

					} else if (obj instanceof DME2FailoverFaultHandler) {
						DME2FailoverFaultHandler handler = (DME2FailoverFaultHandler) obj;
						handler.handleEndpointFailover(ctxData);
						logger.debug(null, "handleEndpointFailover" + handlerName, LogMessage.EXCH_INVOKE_HANDLER);
					}
				} catch (Throwable e1) {
					// ignore exception in loading classname or invoking
					// handleRequest
					logger.warn(null, handlerName, "handleEndpointFault" + LogMessage.EXCH_INVOKE_FAIL, e1);

				}
			}
		} else {
			return false;
		}
		return true;
	}

	private boolean invokeReplyHandlersFault(int responseCode, String cOffer, String requestUrl,
			HttpFields responseHeaders, HttpFields requestHeaders, Throwable e) {
		if (!config.getBoolean(DME2Constants.DME2_ALLOW_INVOKE_HANDLERS)) {
			return false;
		}

		String[] replyHandlers = this.getExchangeReplyHandlers();
		if (replyHandlers != null) {
			String routeOffer = null;
			String version = null;
			if (this.currentEndpointReference != null && this.currentEndpointReference.getEndpoint() != null) {
				routeOffer = this.currentEndpointReference.getEndpoint().getRouteOffer();
				version = this.currentEndpointReference.getEndpoint().getServiceVersion();
			}

			// Create reply context object
			DME2ExchangeFaultContext ctxData = new DME2ExchangeFaultContext(this.lookupURI, responseCode,
					convertRequestHeadersAsMap(requestHeaders), routeOffer, version, requestUrl, e);

			logger.debug(null, "invokeReplyHandlersFault", "ResponseCode={};routeOffer={}{}{}", responseCode,
					routeOffer, REQUESTURL, requestUrl);

			long start = System.currentTimeMillis();
			for (int i = 0; i < replyHandlers.length; i++) {
				String handlerName = replyHandlers[i];
				try {
					// Try loading class name
					Object obj = DME2Utils.loadClass(config, this.getURL(), handlerName);
					if (obj instanceof DME2ExchangeReplyHandler) {
						DME2ExchangeReplyHandler handler = (DME2ExchangeReplyHandler) obj;
						handler.handleFault(ctxData);
						logger.debug(null, "invokeReplyHandlersFault", LogMessage.EXCH_INVOKE_HANDLER, "handleFault",
								handlerName, (System.currentTimeMillis() - start));

					}
				} catch (Throwable e1) {
					/* Ignore exception */
					logger.warn(null, "invokeReplyHandlersFault", LogMessage.EXCH_INVOKE_FAIL, "handleFault",
							handlerName, e1);
				}
			}

		} else {
			return false;
		}

		return true;
	}

	private boolean invokeReplyHandlers(int responseCode, String cOffer, String requestUrl, HttpFields responseHeaders,
			HttpFields requestHeaders) {
		if (!config.getBoolean(DME2Constants.DME2_ALLOW_INVOKE_HANDLERS)) {
			return false;
		}

		String[] replyHandlers = this.getExchangeReplyHandlers();
		if (replyHandlers != null) {
			String version = null;
			String routeOffer = null;
			if (currentEndpointReference != null && currentEndpointReference.getEndpoint() != null) {
				routeOffer = currentEndpointReference.getEndpoint().getRouteOffer();
				version = currentEndpointReference.getEndpoint().getServiceVersion();
			}

			// Create reply context object
			DME2ExchangeResponseContext ctxData = new DME2ExchangeResponseContext(this.lookupURI, responseCode,
					convertRequestHeadersAsMap(requestHeaders), convertResponseHeadersAsMap(responseHeaders),
					routeOffer, version, requestUrl);

			logger.debug(null, "invokeReplyHandlers", "ResponseCode={};routeOffer={};version={}{}{}", responseCode,
					routeOffer, version, REQUESTURL, requestUrl);

			long start = System.currentTimeMillis();
			for (int i = 0; i < replyHandlers.length; i++) {
				String handlerName = replyHandlers[i];
				try {
					// Try loading class name
					Object obj = DME2Utils.loadClass(config, this.getURL(), handlerName);
					if (obj instanceof DME2ExchangeReplyHandler) {
						DME2ExchangeReplyHandler handler = (DME2ExchangeReplyHandler) obj;
						handler.handleReply(ctxData);
						replyHandlersInvoked = true;
						logger.debug(null, "invokeReplyHandlers", LogMessage.EXCH_INVOKE_HANDLER, "handleReply",
								handlerName, (System.currentTimeMillis() - start));
						debugIt(handlerName, "handleResponse invoked");
					}
				} catch (Throwable e) {
					/* Ignore exception */
					logger.warn(null, "invokeReplyHandlers", LogMessage.EXCH_INVOKE_FAIL, "handleReply", handlerName,
							e);
				}
			}

			if (replyHandlersInvoked) {
				this.replyHandlersElapsedTime = System.currentTimeMillis() - start;
			}
		} else {
			return false;
		}

		return true;
	}

	private FailoverHandler loadFailoverHandler() throws DME2Exception {

		FailoverHandler failoverHandler = FailoverFactory.getFailoverHandler(config);
		return failoverHandler;
	}

	private void parseFaultResponse(FailoverHandler handler, int responseStatus, String responseContent,
			byte[] responseContentBytes, long executeComplete, Result result) throws Exception {
		HttpResponse httpResponse = new HttpResponse();
		httpResponse.buildResponseObject(result);
		HttpFields responseHeaders = result.getResponse().getHeaders();
		HttpFields requestHeaders = result.getRequest().getHeaders();
		boolean isFailoverRequired = handler.isFailoverRequired(httpResponse);

		if (!isFailoverRequired) {

			addTraceInfoToResponseHeaders();

			boolean repHandlersInvoked = this.invokeReplyHandlers(responseStatus,
					iterator.getCurrentDME2EndpointRouteOffer(), this.getURL(), result.getResponse().getHeaders(),
					result.getRequest().getHeaders());

			if (repHandlersInvoked || requestHandlersInvoked) {
				logger.debug(null, "parseFaultResponse", LogMessage.EXCH_RECEIVE_HANDLERS, getURL(), responseStatus,
						(executeComplete - sendStart), (executeComplete - executeStart), preferredRouteOffer,
						preferredVersion, (requestHandlersInvoked ? requestHandlersElapsedTime : ""),
						(repHandlersInvoked ? replyHandlersElapsedTime : ""), responseContentBytes.length);
			} else {
				logger.debug(null, "parseFaultResponse", LogMessage.EXCH_RECEIVE, getURL(), responseStatus,
						(executeComplete - sendStart), (executeComplete - executeStart),
						responseContentBytes == null ? 0 : responseContentBytes.length);
			}

			if (responseContentBytes != null) {
				handleReply(responseStatus, responseContent, new ByteArrayInputStream(responseContentBytes),
						convertRequestHeadersAsMap(requestHeaders), convertResponseHeadersAsMap(responseHeaders));
			} else {
				if (this.payloadObj instanceof DME2StreamPayload) {
					Throwable dme2Exception = new DME2Exception(
							DME2Constants.DME2_IGNORE_FAILOVER_STREAM_PAYLOAD_MSGCODE,
							new ErrorContext().add(SERVICE, lookupURI).add(SERVERURL, this.getURL())
							.add(ENDPOINT_ELAPSED_MS, (executeComplete - sendStart) + ""),
							exception);

					Map<String, String> lheaders = convertResponseHeadersAsMap(responseHeaders);
					handleException(lheaders, dme2Exception);
					return;
				} else {
					handleReply(responseStatus, responseContent, null, convertRequestHeadersAsMap(requestHeaders),
							convertResponseHeadersAsMap(responseHeaders));
				}
			}
		} else {
			/* If failover is required, attempt retry */
			doTry(result.getResponse());
		}
	}

	private void handleReply(int code, String message, InputStream in, Map<String, String> requestHeaders,
			Map<String, String> responseHeaders) {
		try {
			replyHandler.handleReply(code, message, in, requestHeaders, responseHeaders);
		} catch (Exception e) {
			logger.warn(null, "handleReply", LogMessage.EXCH_HANDLER_FAIL, "reply", replyHandler, e);
		}
	}

	public boolean isReturnResponseAsBytes() {
		return returnResponseAsBytes;
	}

	public void setReturnResponseAsBytes(boolean returnResponseAsBytes) {
		this.returnResponseAsBytes = returnResponseAsBytes;
	}

	public DME2Configuration getConfig() {
		return config;
	}

	public void setConfig(DME2Configuration config) {
		this.config = config;
	}

	public DME2BaseEndpointIterator getIterator() {
		return iterator;
	}

	public void setIterator(DME2BaseEndpointIterator iterator) {
		this.iterator = iterator;
	}

	public DME2EndpointReference getCurrentEndpointReference() {
		return currentEndpointReference;
	}

	public void setCurrentEndpointReference(DME2EndpointReference currentEndpointReference) {
		this.currentEndpointReference = currentEndpointReference;
	}

	public boolean isMarkStale() {
		return markStale;
	}

	public void setMarkStale(boolean markStale) {
		this.markStale = markStale;
	}

	public String getMessageID() {
		return messageID;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

	public String getCorrelationID() {
		return correlationID;
	}

	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public boolean isCheckResponseContent() {
		return checkResponseContent;
	}

	public void setCheckResponseContent(boolean checkResponseContent) {
		this.checkResponseContent = checkResponseContent;
	}

	public int getiGNORECONTENTLENGTHVALUE() {
		return iGNORECONTENTLENGTHVALUE;
	}

	public void setiGNORECONTENTLENGTHVALUE(int iGNORECONTENTLENGTHVALUE) {
		this.iGNORECONTENTLENGTHVALUE = iGNORECONTENTLENGTHVALUE;
	}

	public String getiGNORECONTENTTYPEVALUE() {
		return iGNORECONTENTTYPEVALUE;
	}

	public void setiGNORECONTENTTYPEVALUE(String iGNORECONTENTTYPEVALUE) {
		this.iGNORECONTENTTYPEVALUE = iGNORECONTENTTYPEVALUE;
	}

	public boolean isRetryCurrentURL() {
		return retryCurrentURL;
	}

	public void setRetryCurrentURL(boolean retryCurrentURL) {
		this.retryCurrentURL = retryCurrentURL;
	}

	public boolean isIgnoreFailoverOnExpire() {

		String jmsPropertyIgnoreFailoverOnExpire = this.headers.get("com.att.aft.dme2.jms.ignoreFailOverOnExpire");

		if (jmsPropertyIgnoreFailoverOnExpire != null) {
			// Found jms property in message that carries ignore from client
			try {
				isIgnoreFailoverOnExpire = Boolean.parseBoolean(jmsPropertyIgnoreFailoverOnExpire);
			} catch (Exception e) {
				// use default
				isIgnoreFailoverOnExpire = config.getBoolean("AFT_DME2_IGNORE_FAILOVER_ONEXPIRE", false);
			}
		}
		debugIt("IS_IGNORE_FAILOVER_ONEXPIRE:", String.valueOf(isIgnoreFailoverOnExpire));
		return isIgnoreFailoverOnExpire;
	}

	public void setIgnoreFailoverOnExpire(boolean isIgnoreFailoverOnExpire) {
		this.isIgnoreFailoverOnExpire = isIgnoreFailoverOnExpire;
	}

	public static Set<String> getGlobalNoticeCache() {
		return globalNoticeCache;
	}

	public static void setGlobalNoticeCache(Set<String> globalNoticeCache) {
		DME2Exchange.globalNoticeCache = globalNoticeCache;
	}

	public boolean isSuccessAlready() {
		return successAlready;
	}

	public void setSuccessAlready(boolean successAlready) {
		this.successAlready = successAlready;
	}

	public boolean isRequestHandlersInvoked() {
		return requestHandlersInvoked;
	}

	public void setRequestHandlersInvoked(boolean requestHandlersInvoked) {
		this.requestHandlersInvoked = requestHandlersInvoked;
	}

	public long getRequestHandlersElapsedTime() {
		return requestHandlersElapsedTime;
	}

	public void setRequestHandlersElapsedTime(long requestHandlersElapsedTime) {
		this.requestHandlersElapsedTime = requestHandlersElapsedTime;
	}

	public String getPreferredRouteOffer() {
		return preferredRouteOffer;
	}

	public void setPreferredRouteOffer(String preferredRouteOffer) {
		this.preferredRouteOffer = preferredRouteOffer;
	}

	public boolean isReplyHandlersInvoked() {
		return replyHandlersInvoked;
	}

	public void setReplyHandlersInvoked(boolean replyHandlersInvoked) {
		this.replyHandlersInvoked = replyHandlersInvoked;
	}

	public long getExecuteStart() {
		return executeStart;
	}

	public void setExecuteStart(long executeStart) {
		this.executeStart = executeStart;
	}

	public String getLookupURI() {
		return lookupURI;
	}

	public void setLookupURI(String lookupURI) {
		this.lookupURI = lookupURI;
	}

	public long getReplyHandlersElapsedTime() {
		return replyHandlersElapsedTime;
	}

	public void setReplyHandlersElapsedTime(long replyHandlersElapsedTime) {
		this.replyHandlersElapsedTime = replyHandlersElapsedTime;
	}

	public String getCurrentFinalUrl() {
		return currentFinalUrl;
	}

	public void setCurrentFinalUrl(String currentFinalUrl) {
		this.currentFinalUrl = currentFinalUrl;
	}

	public boolean isSendTraceInfo() {
		return sendTraceInfo;
	}

	public void setSendTraceInfo(boolean sendTraceInfo) {
		this.sendTraceInfo = sendTraceInfo;
	}

	public long getSendStart() {
		return sendStart;
	}

	public void setSendStart(long sendStart) {
		this.sendStart = sendStart;
	}

	public String getTrackingID() {
		return trackingID;
	}

	public void setTrackingID(String trackingID) {
		this.trackingID = trackingID;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	public int getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
	}

	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(Map<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String, String> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public DME2Payload getPayloadObj() {
		return payloadObj;
	}

	public void setPayloadObj(DME2Payload payloadObj) {
		this.payloadObj = payloadObj;
	}

	public AsyncResponseHandlerIntf getResponseHandler() {
		return responseHandler;
	}

	public void setResponseHandler(AsyncResponseHandlerIntf responseHandler) {
		this.responseHandler = responseHandler;
	}

	public DME2Manager getManager() {
		return manager;
	}

	public void setManager(DME2Manager manager) {
		this.manager = manager;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
		this.currentFinalUrl = url;
	}

	public void setLookupUrl(String lookupURI) {
		this.lookupURI = stripQueryParamsFromURIString(lookupURI);
		this.currentFinalUrl = lookupURI;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getMaxRecursiveCounter() {
		return maxRecursiveCounter;
	}

	public void setMaxRecursiveCounter(int maxRecursiveCounter) {
		this.maxRecursiveCounter = maxRecursiveCounter;
	}

	public String getHostFromArgs() {
		return hostFromArgs;
	}

	public void setHostFromArgs(String hostFromArgs) {
		this.hostFromArgs = hostFromArgs;
	}

	public boolean istRACEON() {
		return tRACEON;
	}

	public void settRACEON(boolean tRACEON) {
		this.tRACEON = tRACEON;
	}

	public long getPerEndpointTimeout() {
		return perEndpointTimeout;
	}

	public void setPerEndpointTimeout(long perEndpointTimeout) {
		this.perEndpointTimeout = perEndpointTimeout;
	}

	public String getMultiPartFile() {
		return multiPartFile;
	}

	public void setMultiPartFile(String multiPartFile) {
		this.multiPartFile = multiPartFile;
	}

	public String getMultiPartFileName() {
		return multiPartFileName;
	}

	public void setMultiPartFileName(String multiPartFileName) {
		this.multiPartFileName = multiPartFileName;
	}

	public Boolean getCheckThrottleResponseContent() {
		return checkThrottleResponseContent;
	}

	public void setCheckThrottleResponseContent(Boolean checkThrottleResponseContent) {
		this.checkThrottleResponseContent = checkThrottleResponseContent;
	}

	public StringBuffer getEpTraceRoute() {
		return epTraceRoute;
	}

	public static String getEp() {
		return EP;
	}

	public static String getAftDmeReqTraceInfo() {
		return AFT_DME2_REQ_TRACE_INFO;
	}

	public static String getEpreferences() {
		return EPREFERENCES;
	}

	public static String getAftDme20702() {
		return AFT_DME2_0702;
	}

	public static String getAftDme20710() {
		return AFT_DME2_0710;
	}

	public static String getEndpointElapsedMs() {
		return ENDPOINT_ELAPSED_MS;
	}

	public static String getAftDme2EpReadTimeoutMs() {
		return AFT_DME2_EP_READ_TIMEOUT_MS;
	}

	public static String getAftDme20712() {
		return AFT_DME2_0712;
	}

	public static String getCharSet() {
		return CHAR_SET;
	}

	public static String getAftDme20715() {
		return AFT_DME2_0715;
	}

	public int getRecursiveCounter() {
		return recursiveCounter;
	}

	public HttpFields getResponseFields() {
		return responseFields;
	}

	public List<String> getMultiPartFiles() {
		return multiPartFiles;
	}

	public List<DME2FileUploadInfo> getFileUploadInfoList() {
		return fileUploadInfoList;
	}

	private String generateUniqueTransactionReference() {
		StringBuffer uniqueReference = new StringBuffer();

		uniqueReference.append(this.hashCode());
		uniqueReference.append("-");
		uniqueReference.append(UUID.randomUUID().toString());

		return uniqueReference.toString();
	}

	private void debugIt(String key, String i) {
		if (tRACEON) {
			debugIt(key + ":" + i);
		}

	}

	private void debugIt(String key, int i) {
		if (tRACEON) {
			debugIt(key + ":" + i);
		}

	}

	private void debugIt(String message) {
		if (tRACEON) {
			System.out.println(
					"[" + new Date() + "] - ThreadID:" + Thread.currentThread().getName() + " - ExchangeObjReference:"
							+ this.hashCode() + "  {" + this.messageID + " - " + this.correlationID + "} - " + message);
		}
	}

	private void debugIt(String key, Exception e) {
		if (tRACEON) {
			debugIt(key, e.toString());
		}
	}

	private String constructDME2ServiceStatsURI(String lookupURI) {
		if (this.requestContext.getUniformResource().getUrlType() == DmeUrlType.STANDARD) {
			String returnString = this.requestContext.getUniformResource().getOriginalURL().getProtocol() + "://" + this.requestContext.getUniformResource().getHost() + (currentEndpointReference.getEndpoint().getContextPath().startsWith("/") ? "" : "/") + currentEndpointReference.getEndpoint().getContextPath() + "?version=" + this.requestContext.getUniformResource().getVersion() + "&envcontext=" + this.requestContext.getUniformResource().getEnvContext();
			if (null != this.requestContext.getUniformResource().getRouteOffer()) returnString = returnString + "&routeoffer=" + this.requestContext.getUniformResource().getRouteOffer();
			else returnString = returnString + "&partner=" + this.requestContext.getUniformResource().getPartner();
			return returnString;
		} else { return lookupURI; }
	}
}