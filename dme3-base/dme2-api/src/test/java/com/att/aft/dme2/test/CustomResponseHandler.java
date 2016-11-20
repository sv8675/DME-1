/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test;

import java.io.InputStream;
import java.util.Map;

import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class CustomResponseHandler implements AsyncResponseHandlerIntf {

  private static Logger logger = LoggerFactory.getLogger(CustomResponseHandler.class.getName());
  private Throwable e = null;
  private final String service;
  private boolean allowAllHttpReturnCodes = false;

  public CustomResponseHandler(String service) {
    this.service = service;
  }

  public CustomResponseHandler(String service, boolean allowAllHttpReturnCodes) {
    this.service = service;
    this.allowAllHttpReturnCodes = allowAllHttpReturnCodes;
  }

  /**
   *
   * @param timeoutMs
   * @return
   * @throws Exception
   */
  public String getResponse(long timeoutMs) throws Exception {
	  logger.debug(null, "getResponse", "In CustomResponseHandler.getResponse");
	  return "CustomResponseHandler.getResponse testing";
  }

  @Override
  public void handleException(Map<String, String> requestHeaders, Throwable e) {
	  logger.debug(null, "handleException", "In CustomResponseHandler.handleException");
  }

  @Override
  public void handleReply(int responseCode, String responseMessage, InputStream in, Map<String, String> requestHeaders, Map<String, String> responseHeaders) {
	  logger.debug(null, "handleReply", "In CustomResponseHandler.handleReply");
  }
  
}