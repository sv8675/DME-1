/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

public class DME2ThrottleFilter implements Filter {
	
	private static final Logger logger = LoggerFactory.getLogger( DME2ThrottleFilter.class );
	private PartnerActiveRequestCounter partnerActiveRequestCounter;
	private DME2ServiceHolder dme2ServiceHolder;

	public static final String MAX_REQUESTS_FOR_PARTNER_MSG = "Application is currently processing maximum allocated request processors for partner.";

	private DME2Configuration config;

	public DME2ThrottleFilter(DME2ServiceHolder dme2ServiceHolder) {
		this.dme2ServiceHolder = dme2ServiceHolder;
		this.partnerActiveRequestCounter = new PartnerActiveRequestCounter(dme2ServiceHolder);
		this.config = dme2ServiceHolder.getManager().getConfig();
	}

	

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		logger.debug( null, "doFilter", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter doFilter called" );
		String partnerName = getPartnerName(servletRequest);
		if (isFilterDisabled() || partnerName==null) {
			logger.debug( null, "doFilter", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter is disabled" );
			filterChain.doFilter(servletRequest, servletResponse);
			logger.debug( null, "doFilter", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter after chain.doFilter called" );
		} else {
			logger.debug( null, "doFilter", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter is enabled" );
			boolean partnerWithinMaxAllowedRequests = partnerActiveRequestCounter.isPartnerWithinMaxAllowedActiveRequests(partnerName);
			if (partnerWithinMaxAllowedRequests) {
				logger.debug( null, "doFilter", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter partner within allowed and hence continuing");
				DME2ThrottleServletResponseWrapper wrapper = new DME2ThrottleServletResponseWrapper((HttpServletResponse) servletResponse, partnerName);
				filterChain.doFilter(servletRequest, wrapper);
				Continuation continuation = ContinuationSupport.getContinuation(servletRequest);
				logger.debug( null, "doFilter", "DME2ThrottleFilter continuation suspended = {}  cont resp wrapp={}", continuation.isSuspended(), continuation.isResponseWrapped());
				
				if (continuation.isSuspended() && continuation.isResponseWrapped()) {
					continuation.addContinuationListener(new ContinuationListenerWaitingForWrappedResponseToFinish(wrapper, partnerActiveRequestCounter));
				} else {
					logger.debug( null, "doFilter", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter decrementing right after message received!!!!!!!!!!" );
					partnerActiveRequestCounter.decrementPartnerRequestCount(partnerName);
				}
			} else {
				logger.error( null, "doFilter", LogMessage.THROTTLE_FILTER_FAILED, dme2ServiceHolder.getServiceURI(), getPartnerName(servletRequest));
				HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
				httpServletResponse.setContentType(MediaType.TEXT_PLAIN);
				httpServletResponse.setCharacterEncoding("UTF-8");
				httpServletResponse.setStatus(DME2Constants.DME2_ERROR_CODE_429);
				ServletOutputStream outputMsg = httpServletResponse.getOutputStream();
				outputMsg.print(MAX_REQUESTS_FOR_PARTNER_MSG);
				return;
			}
		}
	}

	private boolean isFilterDisabled() {
		Boolean userSetQueryParam = null;
		try {
			userSetQueryParam = dme2ServiceHolder.getThrottleFilterDisabled();
		} catch (Exception exception) {
			logger.error( null, "isFilterDisabled", LogMessage.DEBUG_MESSAGE, exception.getMessage());
		}
		logger.debug( null, "isFilterDisabled", "userSetQueryParam: {} config: {}", userSetQueryParam, config.getBoolean(DME2Constants.AFT_DME2_DISABLE_THROTTLE_FILTER, true) );
		if (userSetQueryParam == null) {
			return config.getBoolean(DME2Constants.AFT_DME2_DISABLE_THROTTLE_FILTER, true);
		} else {
			return userSetQueryParam;
		}
	}

	private String getPartnerName(ServletRequest servletRequest) {
		String requestPartnerName = null;
		if (servletRequest instanceof HttpServletRequest) {
			requestPartnerName = ((HttpServletRequest) servletRequest).getHeader(DME2Constants.DME2_REQUEST_PARTNER);
			if (StringUtils.isBlank(requestPartnerName)) {
				requestPartnerName = ((HttpServletRequest) servletRequest).getHeader(DME2Constants.DME2_JMS_REQUEST_PARTNER);
			}
		}
		logger.debug( null, "getPartnerName", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter partner name is " + requestPartnerName);
		return requestPartnerName;
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void destroy() {

	}

	private class ContinuationListenerWaitingForWrappedResponseToFinish implements ContinuationListener {

		private DME2ThrottleServletResponseWrapper dme2ThrottleServletResponseWrapper;
		private PartnerActiveRequestCounter partnerActiveRequestCounter;

		public ContinuationListenerWaitingForWrappedResponseToFinish(DME2ThrottleServletResponseWrapper dme2ThrottleServletResponseWrapper,
				PartnerActiveRequestCounter partnerActiveRequestCounter) {
			super();
			this.dme2ThrottleServletResponseWrapper = dme2ThrottleServletResponseWrapper;
			this.partnerActiveRequestCounter = partnerActiveRequestCounter;
		}

		public void onComplete(Continuation continuation) {
			logger.debug( null, "onComplete", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter got continutation complete hence decrementing");
			decrementPartnerRequestCount();
		}

		public void onTimeout(Continuation continuation1) {
			logger.debug( null, "onTimeout", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter got continutation timeout !!!!!! hence decrementing" );
			decrementPartnerRequestCount();
		}

		private void decrementPartnerRequestCount() {
			try {
				String partnerName = dme2ThrottleServletResponseWrapper.getPartnerName();
				partnerActiveRequestCounter.decrementPartnerRequestCount(partnerName);
			} catch (Exception e) {
				logger.error( null, "decrementPartnerRequestCount", LogMessage.DEBUG_MESSAGE, e.getMessage());
			}
		}

	}

}