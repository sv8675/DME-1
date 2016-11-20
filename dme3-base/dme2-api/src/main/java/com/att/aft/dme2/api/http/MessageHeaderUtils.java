/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.util.Fields;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.util.DME2Constants;
import com.google.common.base.Splitter;

public class MessageHeaderUtils {
	/**
	 * 
	 * @param headers
	 * @return
	 */
	public static void addHeaderPrefix(DME2Configuration config, HttpExchange exchange) {
			// By default OVERRIDE is true
			if(!config.getBoolean(DME2Constants.DME2_OVERRIDE_HEADERS)) {
				return;
			}
			Fields requestField = exchange.getRequest().getParams();
			Set<String> names =  requestField.getNames();
			//Enumeration<String> it = requestField.getFieldNames();
			ArrayList<String> keyList = new ArrayList<String>();
			Iterator<String> it = names.iterator();
			while (it.hasNext()) {
				String key = it.next();
				keyList.add(key);
			}
			String key1= null;
			for(Iterator<String> it1=keyList.iterator();it1.hasNext();key1=it1.next()) {
				if (key1 != null) {
					String value = requestField.get(key1).getValue();
					if (key1.startsWith(DME2Constants.DME2_AFT_CLASS) || key1.startsWith(DME2Constants.JMS)) {
						exchange.getRequest().header(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + key1, value);
						//exchange.setRequestHeader(headerPrefix + key1, value);
						requestField.remove(key1);
					}
				}
			}
	 }
	
	/**
	 * 
	 * @param headers
	 * @return
	 */
	public static void addHeaderPrefix(DME2Configuration config, HttpRequest request) {
			// By default OVERRIDE is true
			if(!config.getBoolean(DME2Constants.DME2_OVERRIDE_HEADERS)) {
				return;
			}
			String queryParam = request.getQueryParams();
			if (queryParam != null) {
				Iterable<String> params = Splitter.on('&')
											       .trimResults()
											       .omitEmptyStrings()
											       .split(queryParam);
				
				Map<String, String> pMap = new HashMap<String, String>();
				Iterator<String> it = params.iterator();
				while (it.hasNext()) {
					String param = it.next();
					List<String> keyValues = Splitter.on('=')
														 .trimResults()
												         .omitEmptyStrings()
												         .splitToList(queryParam);
					pMap.put(keyValues.get(0), keyValues.get(1));
				}
				
				for(String key: pMap.keySet()) {
					if (key != null) {
						String value = pMap.get(key);
						if (key.startsWith(DME2Constants.DME2_AFT_CLASS) || key.startsWith(DME2Constants.JMS)) {
							request.getClientHeaders().put(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + key, value);
							//exchange.setRequestHeader(headerPrefix + key1, value);
							request.getClientHeaders().remove(key);
						}
					}
				}
			}
	 }

	/**
	 * 
	 * @param headers
	 * @return
	 */
	public static Map<String, String> removeHeaderPrefix(DME2Configuration config, 
			Map<String, String> headers) {
		// By default OVERRIDE is true
		if(!config.getBoolean(DME2Constants.DME2_OVERRIDE_HEADERS)) {
			return headers;
		}
		Map<String, String> modHeaders = new HashMap<String, String>();
		if (headers.keySet() != null) {
			Iterator<String> it = headers.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				if (key != null) {
					String value = headers.get(key);
					if (key.startsWith(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + DME2Constants.DME2_AFT_CLASS)
							|| key.startsWith(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + DME2Constants.JMS)) {
						String modKey = key.replace(config.getProperty(DME2Constants.DME2_HEADER_PREFIX), "");
						modHeaders.put(modKey, value);
					}
					else{
						modHeaders.put(key, value);
					}
				}
			}
		}
		return modHeaders;
	}
}
