/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;

import com.att.aft.dme2.api.http.HttpResponse;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FailoverHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/*
 * This class is for DefaultFailoverHanlder implementation.
 * The implementation is specific to HttpResponse
 * It determines if fail over is required based on Content-type, SoapMessage Envelope and Fault String present in SoapBody
 */

public class DME2DefaultFailoverHandler implements FailoverHandler {

	private static DME2Configuration config;

	private static final Logger LOGGER = LoggerFactory.getLogger(DME2DefaultFailoverHandler.class.getName());

	public DME2DefaultFailoverHandler(DME2Configuration configuration) {
		config = configuration;

	}

	/*
	 * Checks to see if failoverRequired for the HttpResponse object. Validates
	 * Content-Type, Soap Envelope and extracts the String related to Fault from
	 * the SoapFaultElement. This string is used to determine if failover is
	 * required or not.
	 */
	private boolean isFailoverRequired(String replyMessage, Integer respCode, Map<String, String> respHeaders) {
		boolean isFailoverRequired = false; /* Value that will be returned */

		String contentType = respHeaders.get("Content-Type");
		boolean isContentTypeValid = false;

		try {
			if (contentType != null
					&& contentType.startsWith(config.getProperty(DME2Constants.AFT_DME2_SOAP_REPLY_CONTENT_TYPE))) {
				isContentTypeValid = true;
			}
			LOGGER.debug(null, "isFailoverRequired",
					"DME2Exchange onResponse received 500 internal error;Content-Type={};ResponseContent={}",
					contentType, replyMessage);

			/*
			 * Check if the content type is valid and the reply is wrapped in a
			 * SOAP envelope
			 */
			if (replyMessage != null && replyMessage.contains(config.getProperty(DME2Constants.AFT_DME2_ENVELOPE_STR))
					&& isContentTypeValid) {
				String responseBytes = replyMessage;

				/* Parse the response string into a SOAP Message object */
				MessageFactory factory = MessageFactory.newInstance();
				SOAPMessage rspMessage = factory.createMessage();

				StreamSource preppedMsgSrc = new StreamSource(new ByteArrayInputStream(responseBytes.getBytes()));
				rspMessage.getSOAPPart().setContent(preppedMsgSrc);
				rspMessage.saveChanges();

				/*
				 * Check if the body of this SOAP Message contains a SOAP Fault
				 * element
				 */
				if (rspMessage.getSOAPBody().hasFault()) {
					SOAPFault fault = rspMessage.getSOAPBody().getFault();
					LOGGER.debug(null, "isFailoverRequired", "DME2xchange getResponse hasFault;{}",
							fault.getTextContent());

					if (fault.getFaultString() != null) {
						String faultString = fault.getFaultString();
						// This property contains values that will determine if
						// we should fail over or not
						String failoverStringToCheck = config.getProperty(DME2Constants.AFT_DME2_FAULT_STRING_FAILOVER);

						if (failoverStringToCheck != null) {
							String[] failoverStrs = failoverStringToCheck.split(",");
							for (int i = 0; i < failoverStrs.length; i++) {
								if (faultString.toLowerCase().contains(failoverStrs[i].toLowerCase())) {
									isFailoverRequired = true;
									break;
								} else {
									isFailoverRequired = false;
								}
							} /* End for loop */
						}
					}
				} /* End if rspMessage.getSOAPBody().hasFault() */
			}
		} catch (Throwable e) {
			/* Ignore Exception */
			if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
				LOGGER.error(null, DME2DefaultFailoverHandler.class.getName(), DME2Constants.EXCEPTION_HANDLER_MSG, e);
			}
		}

		return isFailoverRequired;
	}

	/*
	 * Determines if fail over is required or not. Default implementation
	 * accepts only HttpResponse, so the parameter passed is checked if its an
	 * instance of HttpResponse. Extract the replyMessage, response Headers from
	 * the HttpResposne.
	 */

	@Override
	public boolean isFailoverRequired(DME2Response dme2Response) {
		boolean isFailoverRequired = false;
		if (dme2Response instanceof HttpResponse) {
			HttpResponse failoverHttp = (HttpResponse) dme2Response;
			String replyMessage = failoverHttp.getReplyMessage();
			Integer respCode = failoverHttp.getRespCode();
			Map<String, String> respHeaders = failoverHttp.getRespHeaders();
			if (null != replyMessage && 0 < replyMessage.length() && null != respHeaders) {
				isFailoverRequired = isFailoverRequired(replyMessage, respCode, respHeaders);
			}

		}
		return isFailoverRequired;
	}

}
