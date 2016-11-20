/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

public class PartnerActiveRequestCounter {

	private static final Logger logger = LoggerFactory.getLogger( PartnerActiveRequestCounter.class );
	private DME2Configuration config;

	private DME2ServiceHolder dme2ServiceHolder;
	private final ConcurrentMap<String, AtomicInteger> partnerVsCurrActiveReqCount = new ConcurrentHashMap<String, AtomicInteger>();

  private DME2ThrottleConfig throttleConfig = DME2ThrottleConfig.getInstance();
  private String serviceName = null;
  private static String SERVICE = "/service=";
  private static String SERVICE2 = "service=";

	public PartnerActiveRequestCounter(DME2ServiceHolder dme2ServiceHolder) {
		this.dme2ServiceHolder = dme2ServiceHolder;
		this.config = dme2ServiceHolder.getManager().getConfig();
    this.serviceName = this.getService(dme2ServiceHolder.getServiceURI());
	}

	public boolean isPartnerWithinMaxAllowedActiveRequests(String requestPartnerName) {
		boolean partnerWithinMaxAllowedRequests = true;
		int maxActiveRequestsPerPartner = getMaxActiveRequestsPerPartner(requestPartnerName);
		if (StringUtils.isNotBlank(requestPartnerName) && maxActiveRequestsPerPartner > 0) {
			logger.debug( null, "isPartnerWithinMaxAllowedActiveRequests", "DME2ThrottleFilter validating request count with maxActive={} partner = {}", maxActiveRequestsPerPartner, requestPartnerName );
			partnerVsCurrActiveReqCount.putIfAbsent(requestPartnerName, new AtomicInteger(0));
			AtomicInteger atomicInteger = partnerVsCurrActiveReqCount.get(requestPartnerName);
			synchronized (atomicInteger) {
				int requestCount = atomicInteger.get() + 1;
				if (requestCount > maxActiveRequestsPerPartner) {
          logger.debug( null, "isPartnerWithinMaxAllowedActiveRequests", "Request count {} exceeds maxActiveRequestsPerPartner {}", requestCount, maxActiveRequestsPerPartner );
					partnerWithinMaxAllowedRequests = false;
				} else {
					atomicInteger.incrementAndGet();
					logger.debug( null, "isPartnerWithinMaxAllowedActiveRequests", "DME2ThrottleFilter Increment:: Request count for partner {} is {}", requestPartnerName, atomicInteger.get() );
				}
        logger.debug( null, "isPartnerWithinMaxAllowedActiveRequests", "currentRequestCount:{}; maxRequestCountAllowed:{}",atomicInteger.get(),maxActiveRequestsPerPartner);
			}
		}
		return partnerWithinMaxAllowedRequests;
	}

	public void decrementPartnerRequestCount(String requestPartnerName) {
		if (StringUtils.isNotBlank(requestPartnerName)) {
			AtomicInteger requestCount = partnerVsCurrActiveReqCount.get(requestPartnerName);
			if (requestCount != null) {
				synchronized (requestCount) {
					int valueAfterDecrement = requestCount.decrementAndGet();
					logger.debug( null, "decrementPartnerRequestCount", "DME2ThrottleFilter Decrement:: Request count for partner {} is {}", requestPartnerName, valueAfterDecrement );
					if (valueAfterDecrement == 0) {
						logger.debug( null, "decrementPartnerRequestCount", "DME2ThrottleFilter value after dec is 0 hence removing");
						partnerVsCurrActiveReqCount.remove(requestPartnerName);
					}
				}
			}
		}
	}

	private Integer getMaxActiveRequestsPerPartner(String requestPartnerName) {
		int maxActiveRequestsPerPartner = 0;
		try {
			if (dme2ServiceHolder != null) {
				int maxPoolSize = dme2ServiceHolder.getMaxPoolSize();
				logger.debug(null, "getMaxActiveRequestsPerPartner", "DME2Throttle getMaxActiveRequestsPerPartner:: Max pool size {}",maxPoolSize);
				Float partnerThrottlePct = getThrottlePercentPerPartner(requestPartnerName);
				maxActiveRequestsPerPartner = (int) Math.ceil(maxPoolSize * (partnerThrottlePct / 100.0));
			}
		} catch (Exception exception) {
			logger.error( null, "getMaxActiveRequestsPerPartner", LogMessage.DEBUG_MESSAGE, exception.getMessage());
		}
		logger.debug( null, "getMaxActiveRequestsPerPartner", "DME2ThrottleFilter is max active req per partner = {}", maxActiveRequestsPerPartner );
		return maxActiveRequestsPerPartner;
	}

  private Float getThrottlePercentPerPartner(String requestPartnerName) {
    logger.debug( null, "getThrottlePercentPerPartner", LogMessage.METHOD_ENTER );
		Float partnerThrottlePct = DME2ThrottleConfig.getInstance().getThrottleConfig(serviceName, requestPartnerName);

		if (partnerThrottlePct == null || partnerThrottlePct <= 0) {
      partnerThrottlePct = dme2ServiceHolder.getThrottlePctPerPartner();
      logger.debug( null, "getThrottlePercentPerPartner", "After service holder, throttle filter pct: {}", partnerThrottlePct );
    }
		if (partnerThrottlePct == null || partnerThrottlePct <= 0)
			partnerThrottlePct = config.getFloat( DME2Constants.AFT_DME2_THROTTLE_PCT_PER_PARTNER );

		logger.debug( null, "getThrottlePercentPerPartner", "DME2ThrottleFilter throttle pct for partner: {}={}", requestPartnerName, partnerThrottlePct);
    logger.debug( null, "getThrottlePercentPerPartner", LogMessage.METHOD_EXIT );
		return partnerThrottlePct;
	}

  private String getService (String servletPath){
		String fieldsWithSlash = config.getProperty("DME2_URI_FIELD_WITH_PATH_SEP",DME2Constants.DME2_URI_FIELD_WITH_PATH_SEP_DEF);
		boolean canEndWithSlash=false;
		if(fieldsWithSlash != null){
		 canEndWithSlash = fieldsWithSlash.contains(SERVICE);
		}
		return getService(servletPath,canEndWithSlash);
	}

	private String getService (String servletPath, boolean canEndWithSlash){
		int indexOfField=-1;
		String field = SERVICE;
		if (servletPath.startsWith("/"))
			indexOfField = servletPath.toLowerCase().indexOf(SERVICE);

		else {
			indexOfField = servletPath.toLowerCase().indexOf(SERVICE2);
			field = SERVICE2;
		}

		if(indexOfField != -1){
			try {
				String fieldVal = null;
				// Identify the next field using = sign. Requirement is all input keys will only have = appended to it. 
				// service=com.att.aft.Restful/restful/context/envContext=TEST/version=1.0.0/
				int indexOfNext = servletPath.indexOf("=",indexOfField+field.length());
				// If a next field is found
				if(indexOfNext != -1){
					// Parse till next field
					// com.att.aft.Restful/restful/context/envContext
					String temp = servletPath.substring(indexOfField, indexOfNext);
					// temp value "com.att.aft.Restful/restful/context/envContext"
					fieldVal = temp.substring(temp.indexOf("=")+1, temp.lastIndexOf("/"));
					// field value "service=com.att.aft.Restful/restful/context"

				}
				else {
					// Most of the fields cannot end with / in it except serviceName or subContext fields
					// Identify if servletPath ends with / for a field that should not carry / in it still
					// E.g service=com.att.csi.m2e.Echo/version=1.0.0/envContext=TEST/
					if(!canEndWithSlash && servletPath.endsWith("/")) {
						fieldVal = servletPath.substring(indexOfField+field.length(),servletPath.length()-1);
					} else {
						fieldVal = servletPath.substring(indexOfField+field.length());
					}
				}
				// If values carry "/" in it for fields that do not have / supported in its name E.g version, envContext,partner,routeOffer
				// replace all / with ""
				if(!canEndWithSlash ){
					if(fieldVal != null && fieldVal.contains("/")) {
						fieldVal = fieldVal.replaceAll("/", "");
					}
				}
				// If field is not null, contains / , only once and at the end of the string, then replace that with blank
				// E.g DME2SEARCH/version=1.0/envContext=PROD/service=com.att.aft.MyService/
				if(fieldVal != null && fieldVal.contains("/") && (fieldVal.replaceAll("/","").length()==fieldVal.length()-1) &&  fieldVal.indexOf("/") == fieldVal.length()-1){
					fieldVal = fieldVal.replace("/", "");
				}
				return fieldVal!=null?(fieldVal.length()>0?fieldVal:null):null;
			} catch(Exception e){
				// Return null for any unhandled exception in parsing the fields while looking for index
				return null;
			}
		}
		return null;
	}
}
