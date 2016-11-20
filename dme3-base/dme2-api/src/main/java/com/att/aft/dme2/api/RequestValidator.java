/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.eclipse.jetty.http.HttpMethod;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.BinaryPayload;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2StreamPayload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.FilePayload;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2UrlStreamHandler;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.ServiceValidationHelper;
import com.att.aft.dme2.util.UriHelper;

/**
 * Helper class to validate various objects like DME2Manager, Payload and Request
 * 
 *
 */
public class RequestValidator {
	private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class.getName());
	
	/**
	 * Validate method to validate Request object. It throws an DME2Exception if it fails during validation process
	 * 
	 * @param request
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */
	public static void validate(DME2Configuration config, Request request) throws DME2Exception {
	  URI encodedURI = null;
		URL encodedURL = null;
		String encodedStr = null;

		if (request == null) {
			/*Throw Exception*/
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0703, DME2Constants.EXP_CORE_INVALID_REQ);
		}

		if (request.getLookupUri() == null || "".equals(request.getLookupUri().trim())) {
			/*Throw Exception*/
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0703, DME2Constants.EXP_CORE_INVALID_REQ_URI);
		}

		if(!request.isEncoded()) {
			encodedStr = UriHelper.encodeURIString(request.getLookupUri(), false);
		} else {
			encodedStr = UriHelper.encodeURIString(request.getLookupUri(), true);
		}

		try {
			encodedURI = new URI(encodedStr);
			encodedURL = new URL(encodedURI.getScheme(), encodedURI.getHost(), encodedURI.getPort(),
					UriHelper.appendQueryStringToPath(encodedURI.getPath(), encodedURI.getQuery()),
					new DME2UrlStreamHandler());

		} catch (URISyntaxException e) {
			DME2Exception ex = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0705, e);
			throw ex;
		} catch (MalformedURLException e) {
			DME2Exception ex = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0705, e);
			throw ex;
		}

		ServiceValidationHelper.validateServiceStringIsNonJDBCURL(encodedURL.toString());
		DmeUniformResource dmeUri = new DmeUniformResource(config, encodedURL);
		if(dmeUri.getUrlType() != DmeUniformResource.DmeUrlType.DIRECT) {
			dmeUri.assertValid();
		}
		// override with the client value(s) passed as part of the RequestBuilder, if any exists, else these should be used as per the DmeUniformResource logic 
		if(request.isPresentPreferLocalEPs())
			dmeUri.setPreferLocalEPs(request.isPreferLocalEPs());
		if(request.isPresentUseVersionRange())
			dmeUri.setUseVersionRange(request.isUseVersionRange());
			
		request.setUniformResource(dmeUri);

		if (request instanceof HttpRequest) {
			HttpRequest httpRequest = (HttpRequest)request;
			if(httpRequest.isPresentExchangeRoundTripTimeOut())
				dmeUri.setRoundTripTimeout(httpRequest.getExchangeRoundTripTimeOut());
			
			if (httpRequest.getHttpMethod() == null)
				httpRequest.setHttpMethod(HttpMethod.POST.name());
		}
	}

	/**
 	 * Validate method to validate Payload object. It throws an DME2Exception if it fails during validation process
 	 *
	 * @param payload
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */
	public static void validate(DME2Payload payload) throws DME2Exception {
		String textPayload = null;
		InputStream streamPayLoad = null;
		byte[] bytePayload = null;
		List<String> multiPartFiles = null;
		InputStream streamPayload = null;
		FilePayload filePayload = null;

		if (payload != null)	{
			if (payload instanceof DME2TextPayload)	{
				textPayload = ((DME2TextPayload) payload).getPayload();
			} else if (payload instanceof FilePayload)	{
				filePayload = (FilePayload) payload;

				if (filePayload.isMultipartPayload()) {
					multiPartFiles = filePayload.getMultipartFileNamesWithPaths();
				}
			} else if(payload instanceof BinaryPayload) {
				bytePayload = ((BinaryPayload) payload).getPayload();
			} else if(payload instanceof DME2StreamPayload) {
				streamPayload = ((DME2StreamPayload) payload).getPayload();
			}
			if (textPayload == null && bytePayload == null && streamPayload == null && filePayload == null && (multiPartFiles == null || multiPartFiles.size()==0)) {
				DME2Exception ex = new DME2Exception(DME2Constants.EXP_CORE_MISSING_PAYLOAD, new ErrorContext());
				throw ex;
			}
		} else {
			DME2Exception ex = new DME2Exception(DME2Constants.EXP_CORE_MISSING_PAYLOAD, new ErrorContext());
			throw ex;
		}
	}

	/**
	 * Validate method to validate DME2Manager object. It throws an DME2Exception if it fails during validation process
	 *
	 * @param manager
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */
	public static void validate(DME2Manager manager) throws DME2Exception {
		if (manager == null) { 
			/*Throw Exception*/
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0706, DME2Constants.EXP_CORE_INVALID_MGR);
		}
		
		//TODO add other validation, once DME2Manager is implemented
		
	}
	
}