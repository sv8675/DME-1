/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.util.DME2Utils;

public class DME2ClientSpringImpl {

	private DME2Manager manager;
	
	private Properties props;
	
	private Properties jvmprops;

	private String dme2uri;

	private long perEndpointTimeoutMs;
	
	private String charset;
	
	private boolean returnResponseAsBytes;
	
	private boolean isEncoded;
	
	private String method;
	
	private DME2Client dme2Client;
	
	private DME2Payload dme2Payload;
	
	private String queryParamsString;
	
	private Map<String,String> queryParamsMap;
	
	private boolean queryParamsMapEncode;
	
	private Map<String, String> headers;
	
	private String username;
	
	private String password;
	
	private String context;
	
	private String subcontext;
	
	private AsyncResponseHandlerIntf asyncResponseHandlerIntf;

	public DME2Client getDME2Client() throws DME2Exception, URISyntaxException {
		
		if (jvmprops != null) {
			for (Object key : jvmprops.keySet()) {
				System.setProperty((String) key, jvmprops.getProperty((String) key));
			}
		}
		
		if (isEncoded) {
			dme2uri = DME2Utils.encodeURIString(dme2uri.trim(), isEncoded);
		}
		
		manager = DME2Manager.getDefaultInstance();
		
		if (props != null) {
			for (Object key : props.keySet()) {
				manager.setProperty((String) key, props.getProperty((String) key));
			}
		}
		
		dme2Client = new DME2Client(manager, new URI(dme2uri), perEndpointTimeoutMs, charset, returnResponseAsBytes);
		
		if (method != null) {
			dme2Client.setMethod(method);
		}
		
		if (dme2Payload != null) {
			dme2Client.setDME2Payload(dme2Payload);
		}
		
		if (queryParamsString != null) {
			dme2Client.setQueryParams(queryParamsString);
		}
		
		if (queryParamsMap != null) {
			dme2Client.setQueryParams(queryParamsMap, queryParamsMapEncode);
		}
		
		dme2Client.setHeaders(headers);
		
		if (username != null || password != null) {
			dme2Client.setCredentials(username, password);
		}
		
		if (asyncResponseHandlerIntf != null) {
			dme2Client.setReplyHandler(asyncResponseHandlerIntf);
		}
		
		return dme2Client;
	}
	
	public DME2Client getDME2Client(Request request) throws DME2Exception, MalformedURLException {
		
		for (Object key : jvmprops.keySet()) {
			System.setProperty((String) key, jvmprops.getProperty((String) key));
		}
		
		manager = DME2Manager.getDefaultInstance();
		
		if (props != null) {
			for (Object key : props.keySet()) {
				manager.setProperty((String) key, props.getProperty((String) key));
			}
		}
				
		dme2Client = new DME2Client(manager, request);
		
		if (dme2Payload != null) {
			dme2Client.setDME2Payload(dme2Payload);
		}
		
		if (queryParamsString != null) {
			dme2Client.setQueryParams(queryParamsString);
		}
		
		if (queryParamsMap != null) {
			dme2Client.setQueryParams(queryParamsMap, queryParamsMapEncode);
		}
		
		dme2Client.setHeaders(headers);
		
		if (username != null || password != null) {
			dme2Client.setCredentials(username, password);
		}
		
		if (asyncResponseHandlerIntf != null) {
			dme2Client.setReplyHandler(asyncResponseHandlerIntf);
		}
		
		return dme2Client;
	}
	
	public void addHeader(String name, String value) {
		if(headers == null)
			headers = new HashMap<String,String>();
		headers.put(name, value);
	}
	
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public void setReplyHandler(AsyncResponseHandlerIntf replyHandler) {
		this.asyncResponseHandlerIntf = replyHandler;
	}
	
	public void setProps(Properties props) {
		this.props = props;
	}
	
	public void setJvmprops(Properties jvmprops) {
		this.jvmprops = jvmprops;
	}

	public void setDme2uri(String dme2uri) {
		this.dme2uri = dme2uri;
	}

	public void setPerEndpointTimeoutMs(long perEndpointTimeoutMs) {
		this.perEndpointTimeoutMs = perEndpointTimeoutMs;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setReturnResponseAsBytes(boolean returnResponseAsBytes) {
		this.returnResponseAsBytes = returnResponseAsBytes;
	}

	public void setEncoded(boolean isEncoded) {
		this.isEncoded = isEncoded;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setDme2Payload(DME2Payload dme2Payload) {
		this.dme2Payload = dme2Payload;
	}

	public void setQueryParamsString(String queryParamsString) {
		this.queryParamsString = queryParamsString;
	}

	public void setQueryParamsMap(Map<String, String> queryParamsMap) {
		this.queryParamsMap = queryParamsMap;
	}

	public void setQueryParamsMapEncode(boolean queryParamsMapEncode) {
		this.queryParamsMapEncode = queryParamsMapEncode;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public void setContext(String context) {
		this.context = context;
	}
	
	public void setSubcontext(String subcontext) {
		this.subcontext = subcontext;
	}
}