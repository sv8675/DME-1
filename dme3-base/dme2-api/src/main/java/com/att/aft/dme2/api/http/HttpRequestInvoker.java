/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.StringUtil;

import com.att.aft.dme2.api.ActionType;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.RequestInvokerIntf;
import com.att.aft.dme2.api.SimpleRealm;
import com.att.aft.dme2.api.util.DME2DateFormatAccess;
import com.att.aft.dme2.api.util.DME2DateTimeFormatUtil;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.DefaultNullAsyncResponseHandler;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.BinaryPayload;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.FilePayload;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.RequestContext;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.InternalConnectionFailedException;
import com.google.common.collect.Maps;

/**
 * Invoker class for Http type request
 *
 */
public class HttpRequestInvoker implements RequestInvokerIntf {

	public final static String AFT_DME2_0712 = "AFT-DME2-0712";
	private static final String AFT_DME2_0715 = "AFT-DME2-0715";
	public final static String INPUTFILE = "inputFile";
	public final static String HANDLER_NAME = "handlerName";

	public static final String contentDispositionHeaderName = "Content-Disposition: form-data; name=\"";
	public static final String contentDispositionHeaderFile = "\"; filename=\"";

	public final static String SERVICE = "service";

	private DME2Exchange exchange;
	public final static String CHAR_SET = "; charset=";
	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(HttpRequestInvoker.class.getName());
	/**
	 * The client.
	 */
	private static HttpClient client = null;
	/**
	 * The Constant NULL_REPLY_HANDLER.
	 */
	private static final DefaultNullAsyncResponseHandler NULL_REPLY_HANDLER = new DefaultNullAsyncResponseHandler();
	private DME2Configuration config;

	private DME2DateFormatAccess dformat;

	public HttpRequestInvoker(RequestContext context) {
		this.config = context.getMgr().getConfig();
	}

	/**
	 * ???
	 * 
	 * @param resolvedUrl
	 *            ???
	 * @param context
	 *            ???
	 * @param iterator
	 *            ???
	 * @throws DME2Exception
	 */
	public void createExchange(String resolvedUrl, RequestContext context, DME2BaseEndpointIterator iterator)
			throws DME2Exception {
		try {
			exchange = new DME2Exchange(context.getMgr(), resolvedUrl, context.getRequest().getReadTimeout(),
					context.getRequest().getCharset(), context.getRequest().getClientHeaders());
			exchange.setIterator(iterator);
			exchange.setRequestContext(context);
			exchange.setReturnResponseAsBytes(context.getRequest().isReturnResponseAsBytes());
			if (context.getRequest().getUniformResource() == null)
				throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0016,
						DME2Constants.EXP_CORE_INVALID_DMERESOURCE);
			exchange.setExchangeRoundTripTimeOut(context.getRequest().getUniformResource().getRoundTripTimeout());
			if (context.getRequest().getUniformResource().isIgnoreFailoverOnExpire()) {
				exchange.setIgnoreFailoverOnExpire(true);
			}
			exchange.setPreferLocalEPs(context.getRequest().getUniformResource().isPreferLocalEPs());
			dformat = new DME2DateFormatAccess(context.getMgr().getConfig());
		} catch (DME2Exception e) {
			logger.error(null, "createExchange", e.getMessage());
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0007, e.getMessage());
		}
	}

	private void copyClientHeaders(HttpRequest request) {
		logger.info(null, "copyClientHeaders", "start");
		if (request != null && request.getClientHeaders() != null) {
			logger.info(null, "copyClientHeaders", "found headers to copy");
			if (request.getClientHeaders() == null) {
				request.setHeaders(new HashMap<String, String>());
			}
			for (Entry<String, String> entry : request.getClientHeaders().entrySet()) {
				if (entry != null && entry.getKey() != null && entry.getValue() != null
						&& request.getClientHeaders() != null) {
					logger.info(null, "copyClientHeaders", "copied header: {}={}", entry.getKey(), entry.getValue());
					request.getClientHeaders().put(entry.getKey(), entry.getValue());
				}
			}
		}
		logger.info(null, "copyClientHeaders", "complete");
	}

	@Override
	public void init(RequestContext context, ActionType action, DME2Payload payload) throws DME2Exception {
		HttpRequest request = (HttpRequest) context.getRequest();

//		copyClientHeaders(request);

		// initialize the exchange object with details from the request object
		if (context.getRequest().getRealm() != null) {
			try {
				DmeBasicAuthentication auth = new DmeBasicAuthentication(context.getRequest().getRealm());
				// TODO this method is not there.
				// auth.setCredentials(exchange);
			} catch (IOException e) {
				throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0997, DME2Constants.EXP_CORE_AUTH_ERR, e);
			}
		}

		/* Check for null reply handler */
		if (request.getResponseHandler() == null) {
			logger.debug(context.getLogContext().getConversationId(), null, "", DME2Constants.NO_REPLY_HANDLER_SET);
			request.setResponseHandler(NULL_REPLY_HANDLER);
		} else {
			// make sure a reply to queue name is set so if the other side is
			// JMS it knows we need an answer
			if (request.getClientHeaders() == null || request.getClientHeaders().get(DME2Constants.JMS_REPLY_TO) == null) {
				// debugIt("NO_JMS_REPLY_TO_SET");
				logger.debug(context.getLogContext().getConversationId(), null, "", DME2Constants.NO_JMS_REPLY_TO_SET);
				request.getClientHeaders().put(DME2Constants.JMS_REPLY_TO,
						DME2Constants.HTTP_DME2_LOCAL + UUID.randomUUID().toString());
			}
		}

		// make sure there is a MessageID
		if (request.getClientHeaders() == null || request.getClientHeaders().get(DME2Constants.JMSMESSAGEID) == null) {
			// this.dme2InterfaceProtocol =
			// DME2Constants.DME2_INTERFACE_HTTP_PROTOCOL;

			if (request.getClientHeaders() == null) {
				request.setHeaders(new HashMap<String, String>());
			}

			request.getClientHeaders().put(DME2Constants.JMSMESSAGEID, "ID:" + UUID.randomUUID().toString());

			if (request.getUniformResource().getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER_CLASS,
						request.getUniformResource().getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER, request.getUniformResource().getPartner());
			} else if (request.getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER_CLASS, request.getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER, request.getPartner());
			}
		} else {
			// this.dme2InterfaceProtocol =
			// DME2Constants.DME2_INTERFACE_JMS_PROTOCOL;
			if (request.getUniformResource().getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER_CLASS,
						request.getUniformResource().getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER, request.getUniformResource().getPartner());
			} else if (request.getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_JMS_REQUEST_PARTNER_CLASS, request.getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_JMS_REQUEST_PARTNER, request.getPartner());
			}
		}

		/* Save reply queue to work correctly with non-JMS server */
		// this.replyTo = this.headers.get("JMSReplyTo");

		// this.messageID = this.headers.get(JMSMESSAGEID);
		// this.correlationID = this.headers.get(JMSCORRELATIONID);

//		request.method(request.getHttpMethod());

		// convert header map to request headers
		/*
		 * if (request.getHeaders() != null) {
		 * 
		 * Iterator<HttpField> iter = request.getHeaders().iterator();
		 * 
		 * while (iter.hasNext()) { HttpField httpField = iter.next();
		 * logger.debug(context.getLogContext().getConversationId(), null,
		 * "init", "HEADER " + httpField.getName() + "=" +
		 * httpField.getValue()); request.getParams().add(httpField.getName(),
		 * httpField.getValue()); }
		 * 
		 * 
		 * for (String key : request.getHeaders().keySet()) { String value =
		 * request.getHeaders().get(key);
		 * logger.debug(context.getLogContext().getConversationId(), null,
		 * "init", "HEADER " + key + "=" + value);
		 * exchange.getRequestFields().add(key, value); }
		 * 
		 * }
		 */
		// setExchangePayload( context, payload );
		// add header prefixes to all non-standard headers
		MessageHeaderUtils.addHeaderPrefix(config, request);

	}

	/**
	 * Returns read timeout
	 * 
	 * @return readTimeout
	 */
	private long getReadTimeout(HttpRequest request) {
		long readTimeoutMs = request.getPerEndpointTimeoutMs();
    logger.debug( null, "getReadTimeout", "Request getPerEndpointTimoutMs: {}", readTimeoutMs );
		if (readTimeoutMs == 0) {
			readTimeoutMs = request.getReadTimeout();
			logger.debug( null, "getReadTimeout", "Request getReadTimeout: {}", readTimeoutMs );
		}

		if (readTimeoutMs == 0) {
			readTimeoutMs = config.getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS);
			logger.debug( null, "getReadTimeout", "Config value for {}: {}", DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS, readTimeoutMs );
		}

		return readTimeoutMs;
	}

	/**
	 * This method send the request data using Jetty's class and process the
	 * response data using the listener classes This will be called from the
	 * processor class
	 *
	 * @param action
	 * @param context
	 * @param payload
	 */
	public void execute(ActionType action, RequestContext context, DME2Payload payload)
			throws InternalConnectionFailedException, DME2Exception {
		HttpRequest request = (HttpRequest) context.getRequest();
		long readtimeout = request.getReadTimeout();
		Map<String, String> httpFields = request.getClientHeaders();
		String headerTimeoutValue = httpFields.get("AFT_DME2_EP_READ_TIMEOUT_MS");
		if ( StringUtils.isBlank( headerTimeoutValue )) {
			httpFields.put("AFT_DME2_EP_READ_TIMEOUT_MS", new Long(readtimeout).toString());
		}
		HttpClient client = context.getMgr().getClient(); // new HttpClient();

		try {

			// client.start();
			Request jettyRequest = client.newRequest(request.getLookupUri());
			// set read timeout
			jettyRequest.timeout(getReadTimeout(request), TimeUnit.MILLISECONDS);
			logger.debug( null, "execute", "Set CONNECTION timeout to {}", getReadTimeout( request ));
			populateHeaders(payload, context, jettyRequest);
			populatePayload(payload, context, jettyRequest);
			exchange.setLookupUrl(request.getLookupUri());
			if (context.getLogContext() != null) {
				exchange.setSendTraceInfo(context.getLogContext().isSendTraceInfo());
			}

			/*
			 * ContentProvider contentProvider = jettyRequest.getContent(); for
			 * ( ByteBuffer bb = null; contentProvider.iterator().hasNext(); ) {
			 * bb = contentProvider.iterator().next(); byte[] bytes = new
			 * byte[bb.remaining()]; bb.get( bytes ); logger.debug( null,
			 * "execute", "Content: {}", new String(bytes) ); }
			 */

			// return jettyRequest.send();
			// jettyRequest.onRequestSuccess( )
			logger.debug(null, "execute", "Exchange.getCurrentFinalUrl : {} request.getLookupUri: {} ",
					exchange.getCurrentFinalUrl(), request.getLookupUri());
			exchange.setExecuteStart(System.currentTimeMillis());
			exchange.setSendStart(System.currentTimeMillis());
			jettyRequest.send(exchange);
		} catch (DME2Exception e) {
			throw e;
		} catch (Exception e) {
			throw new DME2Exception("AFT-DME2-0000", e);
		}
	}

	private void populatePayload(DME2Payload payload, RequestContext context, Request jettyRequest)
			throws DME2Exception {
		if (payload != null) {
			exchange.setPayloadObj(payload);
			if (payload instanceof DME2TextPayload) {
				DME2TextPayload textPayload = (DME2TextPayload) payload;
				processTextPayload(context, textPayload, jettyRequest);
			} else if (payload instanceof DME2StreamPayload) {
				DME2StreamPayload streamPayload = (DME2StreamPayload) payload;
				processStreamPayload(context, streamPayload, jettyRequest);
			} else if (payload instanceof FilePayload) {
				FilePayload filePayload = (FilePayload) payload;
				processFilePayload(context, filePayload, jettyRequest);
			} else if (payload instanceof BinaryPayload) {
				BinaryPayload binaryPayload = (BinaryPayload) payload;
				jettyRequest
						.content(new InputStreamContentProvider(new ByteArrayInputStream(binaryPayload.getPayload())));
			}
		}
	}

	public void populateHeaders(DME2Payload payload, RequestContext context, Request jettyRequest) {
		String contentType = payload.getContentType();
		String charset = context.getRequest().getCharset();
		String ctypeHeader = config.getProperty(DME2Constants.AFT_DME2_CTYPE_HEADER);
		Map<String, String> headers = context.getRequest().getClientHeaders();
		Map<String, String> reqHeaders = context.getRequest().getClientHeaders();

		if (headers == null) {
			headers = Maps.newHashMap();
		}
		if (charset == null) {
			if (contentType != null) {
				headers.put(ctypeHeader, contentType);
			}
		} else {
			if (contentType != null) {
				headers.put(ctypeHeader, contentType + CHAR_SET + charset);
			} else {
				headers.put(ctypeHeader, "text/plain; charset=" + charset);
			}
		}

		if (context.getMgr().getConfig().getBoolean(DME2Constants.AFT_DME2_SSL_ENABLE, Boolean.FALSE)) {
			jettyRequest.scheme("https");
		} else {
			jettyRequest.scheme("http");
		}

		jettyRequest.header(config.getProperty(DME2Constants.AFT_DME2_CLIENT_SEND_TIMESTAMP_KEY,
				"AFT_DME2_CLIENT_REQ_SEND_TIMESTAMP"), DME2DateTimeFormatUtil.convertDateTimeToString(ZonedDateTime.now(), config));

		// set method type... default to POST for anything other then GET
		if ("GET".equalsIgnoreCase(((HttpRequest)context.getRequest()).getHttpMethod()))
			jettyRequest.method(HttpMethod.GET);
		else
			jettyRequest.method(HttpMethod.POST);

		SimpleRealm realm = context.getRequest().getRealm();

		if (realm != null) {
			headers.put(HttpHeader.AUTHORIZATION.asString(),
					"Basic " + B64Code.encode(realm.getPrincipal() + ":" + realm.getCredentials(), StringUtil.__UTF8));
		}

		Set<String> keySet = reqHeaders.keySet();

		if (keySet != null) {
			for (String key: keySet) {
				// jettyRequest.header(headerName, reqHeaders.get(headerName));
				headers.put(key, reqHeaders.get(key));
			}
		}

		for (String key : headers.keySet()) {
			jettyRequest.header(key, headers.get(key));
		}
	}

	/*
	 * setExchangePayload(context, payload); // set timeout PER ENDPOINT // TODO
	 * no timeout method //
	 * exchange.setTimeout(request.getPerEndpointTimeoutMs());
	 * 
	 * // add header prefixes to all non-standard headers
	 * MessageHeaderUtils.addHeaderPrefix(config, request);
	 * 
	 * }
	 */

	/*
	 * @Override public Object execute(ActionType action, RequestContext
	 * context, Payload payload) throws InternalConnectionFailedException,
	 * DME2Exception {
	 * 
	 * HttpRequest request = (HttpRequest) context.getRequest();
	 * request.setReturnResponseAsBytes(returnResponseAsBytes);
	 * 
	 * 
	 * //try { //exchange.setRequestURI(context.getRequest().getLookupUri());
	 * exchange.setReturnResponseAsBytes(context.getRequest().
	 * isReturnResponseAsBytes());
	 * exchange.setResponseHandler(context.getRequest().getResponseHandler());
	 * exchange.setNonFailoverStatusCodesParam(context.getUniformResource().
	 * getNonFailoverStatusCodesParam());
	 * exchange.setIgnoreFailoverOnExpire(context.getRequest().
	 * isIgnoreFailoverOnExpire());
	 * exchange.setLookupURI(context.getRequest().getLookupUri());
	 * 
	 * //client.send(exchange);
	 * 
	 * org.eclipse.jetty.client.api.Request request =
	 * client.newRequest(context.getRequest().getLookupUri()); CompleteListener
	 * listener = new CompleteListener() {
	 * 
	 * @Override public void onComplete(Result result) { // TODO Auto-generated
	 * method stub } }; request.send(listener);
	 * 
	 * //} catch (IOException e) { //
	 * logger.error(context.getLogContext().getConversationId(), null,
	 * "Execute", e.getMessage()); //} return null; }
	 */

	private void processFilePayload(RequestContext context, FilePayload payload, Request request) throws DME2Exception {
		// HttpRequest request = (HttpRequest) context.getRequest();
		List<String> uploadFiles = payload.getMultipartFileNamesWithPaths();

		if (payload.isMultipart() && (uploadFiles != null) && (uploadFiles.size() > 0)) {
			for (String fileInfo : uploadFiles) {
				File file = new File(fileInfo);
				if (!file.exists()) {
					throw new DME2Exception(DME2Constants.EXP_AFT_DME2_0720,
							new ErrorContext()
									.add(SERVICE, context.getRequest().getUniformResource().getUrl().toString())
									.add("uploadfilepath", file.getPath()).add("uploadfilename", file.getName()));
				}
			}

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			String boundary = Long.toHexString(System.currentTimeMillis());
			ByteBuffer byteBuffer = null;
			try {
				for (String fileName : uploadFiles) {
					if (payload.isBinaryFile()) {
						String requestStr = createMultiPartString(fileName, boundary, context.getRequest().getCharset(),
								payload.getMultiPartFileName());
						byteArrayOutputStream.write(requestStr.getBytes());
					} else {
						byte[] requestBytes = createMultiPartBytes(fileName, boundary,
								context.getRequest().getCharset(), payload.getMultiPartFileName());
						byteArrayOutputStream.write(requestBytes);
					}
				}

				StringBuffer tail = new StringBuffer();

				tail.append("--").append(boundary).append("--").append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
				tail.append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
				byteArrayOutputStream.write(tail.toString().getBytes());

				ByteArrayInputStream contentStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
				/*
				 * byteBuffer = ByteBuffer.wrap(
				 * byteArrayOutputStream.toByteArray() );
				 * 
				 * ContentListener requestContentList = new ContentListener() {
				 * 
				 * @Override public void onContent(
				 * org.eclipse.jetty.client.api.Request request, ByteBuffer
				 * content ) {
				 * 
				 * } };
				 * 
				 * 
				 * requestContentList.onContent( request, byteBuffer );
				 */

				logger.debug(null, "processFilePayload", "Config AFT_DME2_CLEN_HEADER: {}",
						config.getProperty(DME2Constants.AFT_DME2_CLEN_HEADER));
				logger.debug(null, "processFilePayload", "Config AFT_DME2_CTYPE_HEADER: {}",
						config.getProperty(DME2Constants.AFT_DME2_CTYPE_HEADER));
				logger.debug(null, "processFilePayload", "Config AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE: {}",
						config.getProperty(DME2Constants.AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE));

				request.getHeaders().add(new HttpField(config.getProperty(DME2Constants.AFT_DME2_CLEN_HEADER),
						String.valueOf(byteArrayOutputStream.toByteArray().length)));
				request.getHeaders().add(new HttpField(config.getProperty(DME2Constants.AFT_DME2_CTYPE_HEADER),
						"multipart/form-data;boundary=" + boundary));

				request.content(new InputStreamContentProvider(contentStream));

				// exchange.setRequestHeader(DME2Constants.CONTENT_LEN_HEADER,
				// String.valueOf(byteArrayOutputStream.toByteArray().length));
				// exchange.setRequestContentSource(contentStream);
				// exchange.setRequestHeader(DME2Constants.CONTENT_TYPE_HEADER,
				// DME2Constants.MULTI_PART_CONTENT_TYPE + boundary);
			} catch (Exception e) {
				handleException((HttpRequest) context.getRequest(), e);
			}
		} else {
			try {

				File file = new File(payload.getFileName());
				if (!file.exists()) {
					throw new DME2Exception(DME2Constants.EXP_AFT_DME2_0720,
							new ErrorContext()
									.add(SERVICE, context.getRequest().getUniformResource().getUrl().toString())
									.add("uploadfilepath", file.getPath()).add("uploadfilename", file.getName()));
				}
				request.content(new InputStreamContentProvider(new FileInputStream(file)));
			} catch (Exception e) {
				handleException((HttpRequest) context.getRequest(), e);
			}
		}
	}

	private void processStreamPayload(RequestContext context, DME2StreamPayload payload, Request request)
			throws DME2Exception {
		// TODO see processBinaryPayload
		// exchange.setRequestContentSource(payload.getPayload());
		request.content(new InputStreamContentProvider(payload.getPayload()));
	}

	private void processBinaryPayload(RequestContext context, BinaryPayload payload, Request request)
			throws DME2Exception {
		// Buffer b = new ByteArrayBuffer(payload.getPayload());
		// exchange.setRequestContent(b);

		/*
		 * ByteBuffer buffer = ByteBuffer.wrap( payload.getPayload() );
		 * ContentListener requestContentList = new ContentListener() {
		 * 
		 * @Override public void onContent( org.eclipse.jetty.client.api.Request
		 * request, ByteBuffer content ) {
		 * 
		 * } }; requestContentList.onContent( context.getRequest(), buffer );
		 */
		request.content(new ByteBufferContentProvider(ByteBuffer.wrap(payload.getPayload())), payload.getContentType());
	}

	private void processTextPayload(RequestContext context, DME2TextPayload payload, Request request)
			throws DME2Exception {
		String encodingType = request.getHeaders().get(config.getProperty(DME2Constants.AFT_DME2_CONTENT_ENCODING_KEY));

		if (encodingType != null && config.getBoolean(DME2Constants.AFT_DME2_ALLOW_COMPRESS_ENCODING)) {
			try {
				GZIPOutputStream gzipOutputStream = null;
				ByteArrayOutputStream bos = null;

				bos = new ByteArrayOutputStream();

				try {
					gzipOutputStream = new GZIPOutputStream(bos);
					gzipOutputStream.write(payload.getPayload().getBytes());
					logger.debug(null, "processTextPayload", "Inside Text Payload Process: {}", payload.getPayload());
					gzipOutputStream.close();

				} catch (IOException e) {
					throw e;
				}
				request.content(new InputStreamContentProvider(new ByteArrayInputStream(bos.toByteArray())));
			} catch (IOException e) {
				throw new DME2Exception("AFT-DME2-0711",
						new ErrorContext().add(SERVICE, context.getRequest().getLookupUri())
								.add(DME2Constants.AFT_DME2_CONTENT_ENCODING_KEY, encodingType));
			}
		} else {
			if (payload != null && payload.getPayload() != null) {

				String charset = context.getRequest().getCharset();
				if (charset == null) {
					request.content(
							new InputStreamContentProvider(new ByteArrayInputStream(payload.getPayload().getBytes())),
							payload.getContentType());
				} else {
					try {
						request.content(new InputStreamContentProvider(
								new ByteArrayInputStream(payload.getPayload().getBytes(charset))));
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						throw new DME2Exception("AFT-DME2-0711",
								new ErrorContext().add(SERVICE, context.getRequest().getLookupUri())
										.add(DME2Constants.AFT_DME2_CHARSET, charset));
					}
				}
			}
		}
	}

	private String createMultiPartString(String fileName, String boundary, String charset, String multiPartFileName)
			throws DME2Exception {
		StringBuffer writer = new StringBuffer();
		File textFile = new File(fileName);
		String name = textFile.getName();

		writer.append("--" + boundary).append(config.getProperty(DME2Constants.AFT_DME2_CRLF));

		if (multiPartFileName != null && multiPartFileName.trim().length() != 0) {
			writer.append(contentDispositionHeaderName + multiPartFileName + contentDispositionHeaderFile
					+ textFile.getName() + "\"").append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		} else {
			writer.append(config.getProperty(DME2Constants.AFT_DME2_CONTENT_DISP_HEADER) + textFile.getName() + "\"")
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		}

		if (charset == null) {
			writer.append(config.getProperty(DME2Constants.AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE))
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF))
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		} else {
			writer.append(
					config.getProperty(DME2Constants.AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE) + CHAR_SET + charset)
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF))
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		}

		BufferedReader reader = null;
		try {
			try {
				if (charset == null) {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile)));
				} else {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), charset));
				}

				for (String line; (line = reader.readLine()) != null;) {
					writer.append(line).append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
				}
			} catch (IOException e) {
				if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
					logger.error(null, "createMultiPartString", AFT_DME2_0715,
							new ErrorContext().add("ServerURL", "???").add(INPUTFILE, textFile.getName()), e);
				}
				logger.debug(null, "createMultiPartString", "ERROR_PARSING_INPUT_FILE_FOR_UPLOAD", e);
				// throw new DME2Exception(AFT_DME2_0715, new
				// ErrorContext().add(SERVICE, lookupURI).add(INPUTFILE,
				// textFile.getName()));
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException logOrIgnore) {
					if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
						logger.error(null, "createMultiPartString", "AFT-DME2-0716",
								new ErrorContext().add("ServerURL", "???").add(INPUTFILE, textFile.getName()),
								logOrIgnore);
					}
				}
			}
		}
		return writer.toString();
	}

	private byte[] createMultiPartBytes(String fileName, String boundary, String charset, String multiPartFileName)
			throws DME2Exception {
		StringBuffer header = new StringBuffer();
		File binaryFile = new File(fileName);
		String name = binaryFile.getName();

		header.append("--" + boundary).append(config.getProperty(DME2Constants.AFT_DME2_CRLF));

		if (multiPartFileName != null && multiPartFileName.trim().length() != 0) {
			header.append(contentDispositionHeaderName + multiPartFileName + contentDispositionHeaderFile
					+ binaryFile.getName() + "\"").append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		} else {
			header.append(config.getProperty(DME2Constants.AFT_DME2_CONTENT_DISP_HEADER) + binaryFile.getName() + "\"")
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		}

		if (charset == null) {
			header.append(config.getProperty(DME2Constants.AFT_DME2_MULTIPART_TYPE))
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF))
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		} else {
			header.append(config.getProperty(DME2Constants.AFT_DME2_MULTIPART_TYPE) + CHAR_SET + charset)
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF))
					.append(config.getProperty(DME2Constants.AFT_DME2_CRLF));
		}

		byte[] headerBytes = header.toString().getBytes();

		// Write file content to byte array
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream input = null;
		try {
			try {
				input = new FileInputStream(fileName);
				byte[] buffer = new byte[1024];
				for (int length = 0; (length = input.read(buffer)) > 0;) {
					bos.write(buffer, 0, length);
				}
				bos.write(config.getProperty(DME2Constants.AFT_DME2_CRLF).getBytes());
				bos.flush();
				bos.close();
			} catch (IOException e) {
				if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
					// TODO: Is there a URL here to use?
					logger.error(null, "createMultiPartBytes", AFT_DME2_0715,
							new ErrorContext().add("ServerURL", "???").add(INPUTFILE, binaryFile.getName()), e);
				}
				logger.debug(null, "createMultiPartBytes", "ERROR_PARSING_INPUT_FILE_FOR_UPLOAD", e);
				// throw new DME2Exception(AFT_DME2_0715, new
				// ErrorContext().add(SERVICE, lookupURI).add(INPUTFILE,
				// binaryFile.getName()));
			}
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException logOrIgnore) {
					if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
						// TODO: Is there a URL here to use?
						logger.error(null, "createMultiPartBytes", "AFT-DME2-0716",
								new ErrorContext().add("ServerURL", "???").add(INPUTFILE, binaryFile.getName()),
								logOrIgnore);
					}
				}
			}
		}
		StringBuffer tail = new StringBuffer();

		// Concatenate header/body/tail
		ByteArrayOutputStream mpData = new ByteArrayOutputStream();
		try {
			mpData.write(headerBytes);
			mpData.write(bos.toByteArray());
			mpData.write(tail.toString().getBytes());
		} catch (IOException e) {
			if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
				// TODO: Is there a URL here to use?
				logger.error(null, "createMultiPartBytes", AFT_DME2_0715,
						new ErrorContext().add("ServerURL", "???").add(INPUTFILE, binaryFile.getName()), e);
			}
			logger.debug(null, "createMultiPartBytes", "ERROR_BUILDING_MULTIPART_FOR_UPLOAD", e);
			// throw new DME2Exception(AFT_DME2_0715, new
			// ErrorContext().add(SERVICE,
			// lookupURI).add("multiPartRequestBytes","failed").add(INPUTFILE,
			// binaryFile.getName()));
		}
		return mpData.toByteArray();
	}

	private void handleException(HttpRequest request, Throwable t) {
		try {
			// completeExchangeTimer("tried");
			// EventSampler.iNSTANCE.reportExchangeFailure(uniformResource,
			// sendTimer);
			exchange.getResponseHandler().handleException(request.getClientHeaders(), t);
		} catch (Exception e) {
			logger.warn(null, Level.WARNING.getName(), LogMessage.EXCH_HANDLER_FAIL, e);
		}
	}
}