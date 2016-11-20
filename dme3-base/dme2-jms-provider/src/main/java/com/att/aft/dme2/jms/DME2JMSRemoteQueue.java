/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Queue;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.DME2UniformResource;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSRemoteQueue extends DME2JMSQueue {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSRemoteQueue.class.getName());

	private static final int CONSTANT_1000 = 10000;
	// these JMS message properties become DME2 query string parameters
	private static final String[] SERVICEID_URI_PROPERTIES = { DME2UniformResource.DATA_CONTEXT_KEY,
			DME2UniformResource.PARTNER_KEY, DME2UniformResource.STICKY_SELECTOR_KEY, };

	//private static final String[] QUERYPARAM_URI_PROPERTIES = { DME2UniformResource.MATCH_VERSION_RANGE_KEY };

	private static final String PROPERTYNAME_PREFIX1 = "com.att.aft.dme2.";
	private static final String PROPERTYNAME_PREFIX2 = "com.att.aft.dme2.jms.";

	private final DME2JMSManager manager;
	private final boolean isOriginalURIDirect, isAuditRequests;
	private final String originalURIQuery;
	/**
	 * Allows to configure a reply to queue in the definition for clients that
	 * may be coded to only listen to a fixed queue for all responses
	 */
	private String autoReplyToQueue = null;
	private DME2Configuration config;

	public DME2JMSRemoteQueue(DME2JMSManager manager, URI name) throws JMSException {
		super(manager, name);
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();
		autoReplyToQueue = config.getProperty(JMSConstants.AFT_DME2_JMS_REPLY_QUEUE);
		DME2UniformResource uniformResource = null;

		try {
			String uriStr = name.toString();
			if (!uriStr.startsWith("http://") || !uriStr.startsWith("https://")) {
				uriStr = "http://DME2LOCAL" + uriStr;
			}

			uniformResource = new DME2UniformResource(config, uriStr);
		} catch (MalformedURLException e) {
			throw new DME2JMSException("AFT-DME2-0607",
					new ErrorContext().add("extendedMessage", e.getMessage()).add("URI", name.toString()), e);
		}
		isOriginalURIDirect = uniformResource.getUrlType() == DME2UniformResource.DME2UrlType.DIRECT;
		isAuditRequests = Boolean.valueOf(config.getProperty("AFT_DME2_EXCHANGE_JMS_AUDIT_REQUEST", "true"));
		originalURIQuery = name.getQuery();
	}

	@Override
	public void put(DME2JMSMessage m) throws JMSException {
		logger.debug(null, "put", LogMessage.METHOD_ENTER);
		
		final DME2JMSTextMessage tm = (DME2JMSTextMessage) m;
		if (tm.getJMSMessageID() == null){
			tm.genID();
		}
		
		logger.debug(null, "put", "put text={} props={}", tm.getText(), tm.getProperties());
		if(isAuditRequests) {
			initLoggingContext(m);
			logger.info(null, "put", "AFT-DME2-6109 {}", new ErrorContext().add("QueueURI", getQueueName()));
		}
		
		// prepare client
		final String charset = getProperty(m, "charset");
		long perEndpointTimeoutMs = m.getLongProperty(PROPERTYNAME_PREFIX1 + "perEndpointTimeoutMs");
		if (perEndpointTimeoutMs < 1) {
			perEndpointTimeoutMs = m.getLongProperty(PROPERTYNAME_PREFIX2 + "perEndpointTimeoutMs");
		}
		if (perEndpointTimeoutMs < 1) {
			perEndpointTimeoutMs = CONSTANT_1000;
		}
		
		DME2Client client = null;
		
		try 
		{
			client = new DME2Client(manager.getDME2Manager(), buildRequestURI(m), perEndpointTimeoutMs, charset); 
		} 
		catch (DME2Exception e) 
		{
			throw new DME2JMSException("AFT-DME2-6101", new ErrorContext().add("queueName", getQueueName()).add("extendedMessage",e.getMessage()),e);
		}

		final Map<String,String> requestQueryParams = getMapProperty(m, "queryParams");
		if(requestQueryParams != null){
			client.setQueryParams(requestQueryParams, true);
		}

		client.setHeaders(buildReplyHeaders(m, tm));
		client.setReplyHandler(new DME2JMSHttpReplyHandler(manager));
		client.setPayload(tm.getText());
		
		if(this.getUserName() != null && this.getPassword() != null) {
			client.setCredentials(this.getUserName(), this.getPassword());
		}
		else if(manager.getUserName() != null && manager.getPassword() !=null) {
			client.setCredentials(manager.getUserName(), manager.getPassword());
		}

		// call requested operation
		try 
		{
			client.send();
		} 
		catch (DME2Exception e) 
		{
			throw new DME2JMSException(e);
		}
		logger.debug(null, "put", LogMessage.METHOD_EXIT);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> buildReplyHeaders(DME2JMSMessage m, DME2JMSTextMessage tm) throws JMSException {
		// use non-generic constructor to simplify bulk adding Properties
		// entries
		final HashMap<String, String> replyHeaders = new HashMap(tm.getProperties());
		if (m.getJMSReplyTo() != null) {
			final Queue q = (Queue) m.getJMSReplyTo();
			replyHeaders.put("JMSReplyTo", q.getQueueName());
		} else {
			// auto populate a JMSReplyTo if set
			if (this.autoReplyToQueue != null) {
				replyHeaders.put("JMSReplyTo", this.autoReplyToQueue);
			}
		}
		final String conversationId = getProperty(m, "conversationID");
		if (conversationId != null) {
			replyHeaders.put("JMSConversationID", conversationId);
		}
		return replyHeaders;
	}

	private String buildQueryStringFromMessageProperties(DME2JMSMessage m) throws JMSException {
		/* A Collection of client-side query parameters that DME2 supports */
		Set<String> jmsQueryParamKeys = new HashSet<String>();
		jmsQueryParamKeys.add("matchVersionRange");
		jmsQueryParamKeys.add("DME2NonFailoverStatusCodes");
		jmsQueryParamKeys.add("userName");
		jmsQueryParamKeys.add("password");
		jmsQueryParamKeys.add("preferLocal");
		jmsQueryParamKeys.add("ignoreFailoverOnExpire");
		jmsQueryParamKeys.add("connectTimeoutInMs");

		/* Properties that were set on the DME2JMSMessage object */
		Properties props = m.getProperties();
		Map<String, String> queryParamsMap = new HashMap<String, String>();

		if (props != null) {
			/*
			 * Check if the DME2JMSMessage properties contains one of the
			 * supported query param keys
			 */
			for (String paramKey : jmsQueryParamKeys) {
				String queryParamValue = getProperty(m, paramKey);
				if (queryParamValue != null) {
					queryParamsMap.put(paramKey, queryParamValue);
				}
			}
		}

		StringBuilder queryString = new StringBuilder();

		Iterator<Map.Entry<String, String>> iter = queryParamsMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			String paramKey = entry.getKey();
			String paramValue = entry.getValue();

			queryString.append(paramKey + "=" + paramValue);
			if (iter.hasNext()) {
				queryString.append("&");
			}
		}

		if (queryString.length() > 0) {
			return queryString.toString();
		} else {
			return null;
		}
	}

	/** construct URI based on message parameters and the original queue URI */
	private URI buildRequestURI(DME2JMSMessage m) throws JMSException {
		final StringBuilder uri = new StringBuilder();
		uri.append("http://").append(getQueueHost());
//		uri.append("/").append(getQueueHost());
		if (!getQueueName().startsWith("/")) {
			uri.append("/");
		}
		uri.append(getQueueName());

		final StringBuilder query = new StringBuilder();
		String finalQueryString = originalURIQuery;
		String queryParamsFromMessageProps = buildQueryStringFromMessageProperties(m);

		if (queryParamsFromMessageProps != null) /*
													 * If the DME2JMSTextMessage
													 * props contained query
													 * string data, append it to
													 * the finalQueryString
													 */
		{
			if (finalQueryString == null) {
				finalQueryString = queryParamsFromMessageProps;
			} else {
				finalQueryString = finalQueryString + "&" + queryParamsFromMessageProps;
			}
		}

		if (finalQueryString != null) {
			query.append(finalQueryString);
		}

		// combine original queue URI and request params
		if (!isOriginalURIDirect) {
			// add service identifier items to URI
			for (String key : SERVICEID_URI_PROPERTIES) {
				final String value = getProperty(m, key);
				if (value != null) {
					uri.append("/").append(key).append("=").append(value);
				}
			}

		}
		String dataContext = getProperty(m, DME2UniformResource.DATA_CONTEXT_KEY);
		String partner = getProperty(m, DME2UniformResource.PARTNER_KEY);
		String stickySelectorKey = getProperty(m, DME2UniformResource.STICKY_SELECTOR_KEY);
		String partnerStr;
		String dataContextStr;
		String stickySelectorKeyStr;
		if (partner == null) {
			partnerStr = "";
		} else {
			partnerStr = "/partner=" + partner;
		}
		if (dataContext == null) {
			dataContextStr = "";
		} else {
			dataContextStr = "/dataContext=" + dataContext;
		}
		if (stickySelectorKey == null) {
			stickySelectorKeyStr = "";
		} else {
			stickySelectorKeyStr = "/stickySelectorKey=" + stickySelectorKey;
		}
		uri.append(dataContextStr + partnerStr + stickySelectorKeyStr);

		// add query string to URI
		if (query.length() > 0) {
			uri.append("?").append(query);
		}

		try {
			return new URI(uri.toString());
		} catch (URISyntaxException e1) {
			throw new DME2JMSException("AFT-DME2-6100", new ErrorContext().add("queueName", getQueueName()));
		}
	}

	/** look for JMSProperty using either prefix */
	private String getProperty(DME2JMSMessage m, String name) throws JMSException {
		final String value = m.getStringProperty(PROPERTYNAME_PREFIX1 + name);
		if (value != null) {
			return value;
		}
		return m.getStringProperty(PROPERTYNAME_PREFIX2 + name);
	}
	

	public String getQueryParams(Map<String,String> mapParams, boolean encode) {
		String tmpQueryParams = "";
		if(mapParams==null || mapParams.size()==0) {
			return tmpQueryParams;
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
				this.logger.debug(null, "getQueryParams", "Could not encode parameter: {}", e.toString(),uee);
			}
		}
		tmpQueryParams = sb.toString();
		
		return tmpQueryParams;
	}


	/**
	 * get JMSProperty, decode string value and interpret as key=value entries
	 * in a map
	 */
	private Map<String, String> getMapProperty(DME2JMSMessage m, String name) throws JMSException {
		final String encoded = getProperty(m, name);
		if (encoded == null) {
			return null;
		}

		final Map<String, String> out = new HashMap<String, String>();
		for (String entry : encoded.split("&")) {
			final String[] keyValue = entry.split("=");
			if (keyValue.length == 2) {
				out.put(keyValue[0], keyValue[1]);
			}
			// else silently ignore invalid query syntax?
		}
		return out;
	}

	private void initLoggingContext(DME2JMSMessage msg) {
		String trackingID = null;
		try {
			String msgId = msg.getJMSMessageID();

			String conversationId = msg.getStringProperty("JMSConversationID");
			if (conversationId == null) {
				conversationId = msg.getStringProperty("com.att.aft.dme2.jms.conversationID");
			}

			trackingID = msgId + (conversationId == null ? "" : "(" + conversationId + ")");
			DME2Constants.setContext(trackingID, null);
		} catch (Exception e) {
			logger.warn(null, "initLoggingContext", JMSLogMessage.INIT_CTX_FAIL, e);
		}
	}

	@Override
	public void addListener(DME2JMSMessageConsumer consumer, MessageListener listener, String filter)
			throws JMSException {
		throw new DME2JMSException("AFT-DME2-6103",
				new ErrorContext().add("queueName", this.getQueueName()).add("filter", filter));
	}

	@Override
	public String toString() {
		try {
			return "RemoteQueue: " + this.getQueueName();
		} catch (JMSException e) {
			return "RemoteQueue";
		}
	}

}
