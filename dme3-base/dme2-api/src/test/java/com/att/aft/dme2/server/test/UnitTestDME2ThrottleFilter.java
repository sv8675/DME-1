/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DME2ServiceHolder;
import com.att.aft.dme2.api.util.DME2ThrottleFilter;
import com.att.aft.dme2.api.util.DME2ThrottleServletResponseWrapper;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2Constants;

public class UnitTestDME2ThrottleFilter {

	private static final String TEST_PARTNER_NAME = "TEST_PARTNER_NAME";
	private static final String TEST_SERVICE_NAME = "TEST_SERVICE_NAME";
	private static final int MAX_ACTIVE_REQUESTS_PER_PARTNER = 10;
	private static final int INVALID_MAX_ACTIVE_REQUESTS_PER_PARTNER = 0;

	private DME2ThrottleFilter dme2ThrottleFilter;
  private DME2Configuration mockDME2Configuration;

	@Test
	public void allowsRequestForPartnerWhenPartnerNameNotAvailable() throws Exception {
		// prepare
		DME2ServiceHolder mockDME2ServiceHolder = mock( DME2ServiceHolder.class );
		DME2Manager mockDME2Manager = mock( DME2Manager.class );
    mockDME2Configuration = mock( DME2Configuration.class );
		when( mockDME2ServiceHolder.getManager() ).thenReturn(mockDME2Manager);
    when( mockDME2Manager.getConfig() ).thenReturn( mockDME2Configuration  );
    when( mockDME2ServiceHolder.getServiceURI() ).thenReturn( "" );
		dme2ThrottleFilter = new DME2ThrottleFilter(mockDME2ServiceHolder);
		HttpServletRequest mockServletRequest = mock( HttpServletRequest.class );
		HttpServletResponse mockServletResponse = mock( HttpServletResponse.class );
		FilterChain mockFilterChain = mock( FilterChain.class );

		// perform
		dme2ThrottleFilter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain);

		// assert filter chain was advanced forward
		Mockito.verify(mockFilterChain).doFilter(mockServletRequest, mockServletResponse);
	}

	@Test
	public void allowsRequestWhenMaxRequestsLimitIsInvalid() throws Exception {
		// prepare
		DME2ServiceHolder mockDME2ServiceHolder = mock( DME2ServiceHolder.class );
		DME2Manager mockDME2Manager = mock( DME2Manager.class );
    mockDME2Configuration = mock( DME2Configuration.class );
		when( mockDME2ServiceHolder.getMaxPoolSize() ).thenReturn(INVALID_MAX_ACTIVE_REQUESTS_PER_PARTNER);
		when( mockDME2ServiceHolder.getManager() ).thenReturn(mockDME2Manager);
    when( mockDME2Manager.getConfig() ).thenReturn( mockDME2Configuration );
    when( mockDME2ServiceHolder.getServiceURI() ).thenReturn( "" );

		dme2ThrottleFilter = new DME2ThrottleFilter(mockDME2ServiceHolder);
		HttpServletRequest mockServletRequest = mock( HttpServletRequest.class );
		HttpServletResponse mockServletResponse = mock( HttpServletResponse.class );
		
		DME2ThrottleServletResponseWrapper wrapper = new DME2ThrottleServletResponseWrapper(mockServletResponse, TEST_PARTNER_NAME);
		
		FilterChain mockFilterChain = mock( FilterChain.class );
		when( mockServletRequest.getHeader( DME2Constants.DME2_REQUEST_PARTNER ) ).thenReturn(TEST_PARTNER_NAME);
		// perform
		dme2ThrottleFilter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain);

		// assert filter chain was advanced forward
		Mockito.verify(mockFilterChain).doFilter(mockServletRequest, wrapper);
	}

	@Test
	public void returnsHttp429IfPartnerExceededMaxActiveRequestsLimit() throws Exception {
		// prepare
		final DME2Manager mockDME2Manager = mock( DME2Manager.class );
		DME2ServiceHolder mockDME2ServiceHolder = mock( DME2ServiceHolder.class );
		DME2Configuration mockConfig = mock( DME2Configuration.class );
		when( mockConfig.getFloat( DME2Constants.AFT_DME2_THROTTLE_PCT_PER_PARTNER ) ).thenReturn(80f);
		
		when( mockDME2ServiceHolder.getManager() ).thenReturn(mockDME2Manager);
		when( mockDME2ServiceHolder.getManager().getConfig() ).thenReturn(mockConfig);
		when( mockDME2ServiceHolder.getMaxPoolSize() ).thenReturn(MAX_ACTIVE_REQUESTS_PER_PARTNER);
		when( mockDME2Manager.getService( TEST_SERVICE_NAME ) ).thenReturn(mockDME2ServiceHolder);
    when( mockDME2ServiceHolder.getServiceURI() ).thenReturn( "" );

    dme2ThrottleFilter = new DME2ThrottleFilter(mockDME2ServiceHolder);

		HttpServletRequest mockServletRequest = mock( HttpServletRequest.class );
		HttpServletResponse mockServletResponse = mock( HttpServletResponse.class );
		when( mockServletResponse.getOutputStream() ).thenReturn( mock( ServletOutputStream.class ));
		FilterChain mockFilterChain = mock( FilterChain.class );
		when( mockServletRequest.getHeader( DME2Constants.DME2_REQUEST_PARTNER ) ).thenReturn(TEST_PARTNER_NAME);
		final Object synchronizationObject = new Object();

		// perform call for 10 * 80% times
		for (int i = 0; i < (MAX_ACTIVE_REQUESTS_PER_PARTNER - 2); i++) {
			new Thread() {
				@Override
				public void run() {
					HttpServletRequest localMockServletRequest = mock( HttpServletRequest.class );
					HttpServletResponse localMockServletResponse = mock( HttpServletResponse.class );
					when( localMockServletRequest.getHeader( DME2Constants.DME2_REQUEST_PARTNER ) ).thenReturn(TEST_PARTNER_NAME);
					FilterChain localMockFilterChain = new FilterChain() {
						@Override
						public void doFilter(ServletRequest servletrequest, ServletResponse servletresponse) throws IOException, ServletException {
							try {
								synchronized (synchronizationObject) {
									synchronizationObject.wait();
								}
							} catch (InterruptedException e) {
							}
						}
					};
					try {
						dme2ThrottleFilter.doFilter(localMockServletRequest, localMockServletResponse, localMockFilterChain);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();

		}
		// wait for above threads to
		Thread.sleep(1000);
		// perform one more time exceeding limit
		dme2ThrottleFilter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain);

		// assert filter chain was advanced forward
		Mockito.verify(mockServletResponse).setStatus(DME2Constants.DME2_ERROR_CODE_429);
		synchronized (synchronizationObject) {
			synchronizationObject.notifyAll();
		}
	}

}