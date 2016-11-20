/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.util.Fields;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2Constants;

public class DME2MessageHeaderUtils {
	/**
	 * 
	 * @param headers
	 * @return
	 */
	public static void addHeaderPrefix(DME2Configuration config, HttpExchange exchange) {
			String headerPrefix = config.getProperty(DME2Constants.DME2_HEADER_PREFIX);
			// By default OVERRIDE is true
			if(!config.getBoolean(DME2Constants.DME2_OVERRIDE_HEADERS)) {
				return;
			}
			Fields requestField = exchange.getRequest().getParams();
			Set<String> names =  requestField.getNames();
			Iterator<String> it = names.iterator();
			//Enumeration<String> it = requestField.getNames();
			ArrayList<String> keyList = new ArrayList<String>();
			while (it.hasNext()) {
				String key = it.next();
				keyList.add(key);
			}
			String key1= null;
			for(Iterator<String> it1=keyList.iterator();it1.hasNext();key1=it1.next()) {
				if (key1 != null) {
					String value = requestField.get(key1).getValue();
					if (key1.startsWith("com.att.aft.dme2")
							|| key1.startsWith("JMS")) {
						
						exchange.getRequest().header(headerPrefix + key1, value);
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
	public static Map<String, String> removeHeaderPrefix(DME2Configuration config, 
			Map<String, String> headers) {
		String headerPrefix = config.getProperty(DME2Constants.DME2_HEADER_PREFIX);
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
					if (key.startsWith(config.getProperty(DME2Constants.DME2_HEADER_PREFIX)
							+ "com.att.aft.dme2")
							|| key.startsWith(config.getProperty(DME2Constants.DME2_HEADER_PREFIX)
									+ "JMS")) {
						String modKey = key.replace(headerPrefix, "");
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
