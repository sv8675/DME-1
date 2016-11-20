/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.BeforeClass;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.FailoverFactory;
import com.att.aft.dme2.api.http.HttpResponse;
import com.att.aft.dme2.config.DME2Configuration;

public class TestDME2DefaultFailoverHandler {
	private static DME2Configuration configuration = new DME2Configuration();

	private static FailoverHandler failoverHandler;

	@BeforeClass
	public static void setUp() {

		try {
			failoverHandler = FailoverFactory.getFailoverHandler(configuration);
		} catch (DME2Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private SOAPMessage buildSoapMessage() {

		try {
			SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
			SOAPPart soapPart = soapMessage.getSOAPPart();
			SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
			soapEnvelope.addNamespaceDeclaration("scld", "http://scld.att.com/grm/RouteInfoMetaData/v1");
			MimeHeaders mimeHeaders = soapMessage.getMimeHeaders();
			mimeHeaders.addHeader("Content-Type", "text/xml");
			SOAPBody soapBody = soapEnvelope.getBody();
			SOAPFault soapFault = soapBody.addFault();
			soapFault.setFaultCode("fault");
			soapFault.setFaultString("FailoverRequired=true");
			return soapMessage;
		} catch (SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private Map<String, String> getHeaders() {
		SOAPMessage message = buildSoapMessage();
		MimeHeaders headers = message.getMimeHeaders();
		String[] headervalue = headers.getHeader("Content-Type");
		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Content-Type", headervalue[0]);
		return headerMap;
	}

	private String getSoapMessage() {
		SOAPMessage message = buildSoapMessage();
		StringWriter sw = new StringWriter();

		try {
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(message.getSOAPPart()),
					new StreamResult(sw));
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}

		return sw.toString();

	}

	private SOAPMessage buildSoapMessageWithNoFault() {

		try {
			SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
			SOAPPart soapPart = soapMessage.getSOAPPart();
			SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
			soapEnvelope.addNamespaceDeclaration("scld", "http://scld.att.com/grm/RouteInfoMetaData/v1");
			MimeHeaders mimeHeaders = soapMessage.getMimeHeaders();
			mimeHeaders.addHeader("Content-Type", "text/xml");
			return soapMessage;
		} catch (SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private String getSoapMessageWithNoFault() {
		SOAPMessage message = buildSoapMessageWithNoFault();
		StringWriter sw = new StringWriter();

		try {
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(message.getSOAPPart()),
					new StreamResult(sw));
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}

		return sw.toString();

	}

	@Test
	public void testIsFailoverRequired() {

		Map<String, String> respHeaders = getHeaders();
		String replyMessage = getSoapMessage();
		HttpResponse failoverHttpClient = new HttpResponse();
		failoverHttpClient.setReplyMessage(replyMessage);
		failoverHttpClient.setRespCode(501);
		failoverHttpClient.setRespHeaders(respHeaders);
		boolean isFailOverRequired = failoverHandler.isFailoverRequired(failoverHttpClient);
		assertTrue(isFailOverRequired);
	}

	@Test
	public void testIsFailoverRequiredWithNoValidContentType() {

		Map<String, String> respHeaders = new HashMap<String, String>();
		respHeaders.put("Content-Type", "application/json");
		String replyMessage = getSoapMessage();
		HttpResponse failoverHttpClient = new HttpResponse();
		failoverHttpClient.setReplyMessage(replyMessage);
		failoverHttpClient.setRespCode(501);
		failoverHttpClient.setRespHeaders(respHeaders);
		boolean failOverRequired = failoverHandler.isFailoverRequired(failoverHttpClient);
		System.out.println(failOverRequired);
		assertFalse(failOverRequired);
	}


	@Test
	public void testIsFailoverRequiredWithNoValidFault() {

		Map<String, String> respHeaders = getHeaders();
		String replyMessage = getSoapMessageWithNoFault();
		HttpResponse failoverHttpClient = new HttpResponse();
		failoverHttpClient.setReplyMessage(replyMessage);
		failoverHttpClient.setRespCode(501);
		failoverHttpClient.setRespHeaders(respHeaders);
		boolean failOverRequiredWithNoFault = failoverHandler.isFailoverRequired(failoverHttpClient);
		System.out.println(failOverRequiredWithNoFault);
		assertFalse(failOverRequiredWithNoFault);
	}

}
