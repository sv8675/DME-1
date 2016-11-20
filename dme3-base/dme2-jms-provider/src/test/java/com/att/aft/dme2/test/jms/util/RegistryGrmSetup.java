/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.test.jms.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.OfferCache;
import com.att.scld.grm.types.v1.Result;
import com.att.scld.grm.types.v1.ResultCode;
import com.att.scld.grm.types.v1.ServiceDefinition;
import com.att.scld.grm.v1.GetRouteInfoRequest;
import com.att.scld.grm.v1.GetRouteInfoResponse;
import com.att.scld.grm.v1.LockRouteInfoForEditRequest;
import com.att.scld.grm.v1.LockRouteInfoForEditResponse;
import com.att.scld.grm.v1.SaveNReleaseRouteInfoRequest;
import com.att.scld.grm.v1.SaveNReleaseRouteInfoResponse;

/**
 * Perform set-up operations for the DME2 GRM Registry.
 */
public class RegistryGrmSetup {
	private static DME2Configuration config;
	private static final Logger logger = LoggerFactory.getLogger(RegistryGrmSetup.class.getName());
	final static int DEFAULT_TIMEOUT = 60000;
	private String envLetter = null;
	private String urls = null;
	private static final JAXBContext context = initContext();

	/**
	 * Initialize environment - especially required system properties.
	 * 
	 * @throws IOException
	 */
	public static Properties init() throws IOException {
		Properties props = new Properties();
		props.setProperty("DME2_EP_REGISTRY_CLASS", "DME2GRM");
		props.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		props.setProperty("DME2.DEBUG", "true");
		//props.setProperty("DME2_MANAGER_NAME", "TestManager");
		props.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		props.setProperty("AFT_LATITUDE", "33.373900");
		props.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("AFT_ENVIRONMENT", "AFTUAT");
		System.setProperty("AFT_LATITUDE", "33.373900");
		System.setProperty("AFT_LONGITUDE", "-86.798300");
		System.setProperty("platform", TestConstants.GRM_PLATFORM_TO_USE);
		
		List<String> defaultConfigs = new ArrayList<String>();
		defaultConfigs.add(JMSConstants.JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME);
		defaultConfigs.add(JMSConstants.DME_API_DEFAULT_CONFIG_FILE_NAME);
//		defaultConfigs.add(JMSConstants.METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME);
		
		config = new DME2Configuration("DME2GRM", defaultConfigs, null, props);

		return props;
	}

	public boolean saveRouteInfoForPreferedRoute(RouteInfo rtInfo, String env) throws DME2Exception {
		System.setProperty("DME2.DEBUG", "true");
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName(rtInfo.getServiceName());
			req.setEnv(env);
			req.setServiceDefinition(sd);
			req.setUserId(userId);

			String request = this.getSOAPRequest(req);
			String reply = invokeGRMService(request);
			if (reply != null) {
				String temp = getResponse(reply);
				InputStream input = new ByteArrayInputStream(temp.getBytes("UTF-8"));
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast(unmarshaller.unmarshal(input));

				Result res = element.getResult();
				if (res.getResultCode() == ResultCode.FAIL) {
					// Ignore exception since it might be due to previous lock
					// attempts did not
					// release. But save and release step below would fail for
					// diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv(env);
			req1.setServiceDefinition(sd);
			req1.setUserId(userId);
			Marshaller marshaller = context.createMarshaller();
			String xmlContent = new String();
			// marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
			StringWriter sw = new StringWriter();

			marshaller.marshal(new JAXBElement<RouteInfo>(new QName("uri", "local"), RouteInfo.class, rtInfo), sw);
			StringBuffer sb = new StringBuffer();
//			sb.append(
//					"<routeInfo serviceName=\"com.att.aft.dme2.test.TestCatchUncheckedException\" serviceVersion=\"1.0\" envContext=\"LAB\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>");			
			sb.append(
					"<routeInfo serviceName=\"com.att.aft.TestJMSExchangePreferredRouteOffer\" serviceVersion=\"*\" envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>");
			sb.append("<route name=\"DEFAULT\">");
			sb.append("<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>");
			sb.append("</route>");
			sb.append("</routeGroup>");
			sb.append("<routeGroup name=\"test1\">");
			sb.append("<partner>test1</partner>");
			sb.append("<partner>test2</partner>");
			sb.append("<partner>test3</partner>");
			sb.append("<route name=\"test1\">");
			sb.append("<routeOffer name=\"BAU_NE\" sequence=\"1\" active=\"true\"/>");
			sb.append("</route>");
			sb.append("<route name=\"test2\">");
			sb.append("<routeOffer name=\"BAU_SE\" sequence=\"2\" active=\"true\"/>");
			sb.append("</route>");
			sb.append("<route name=\"test3\">");
			sb.append("<routeOffer name=\"BAU_NW\" sequence=\"3\" active=\"true\"/>");
			sb.append("</route>");
			sb.append("</routeGroup>");
			sb.append("</routeGroups>");
			sb.append("</routeInfo>");
			// req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml(sb.toString());

			request = this.getSOAPRequest(req1);
			reply = invokeGRMService(request);

			if (reply != null) {
				String temp = getResponse(reply);
				InputStream input = new ByteArrayInputStream(temp.getBytes("UTF-8"));
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast(unmarshaller.unmarshal(input));

				Result res = element1.getResult();
				if (res.getResultCode() == ResultCode.FAIL)
					throw new Exception(res.getResultText());
			}

			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			throw this.processException(e, "saveRouteInfoForPreferedRoute",
					req.getEnv() + "/" + req.getServiceDefinition().getName());
		}
	}

	/**
	 * 
	 * @param req
	 * @return
	 * @throws JAXBException
	 * @throws SOAPException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws Exception
	 */
	public static String getSOAPRequest(Object req)
			throws JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException {
		long start = System.currentTimeMillis();

		// marshaller = initMarshaller();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
		StringWriter sw = new StringWriter();
		marshaller.marshal(req, sw);

		System.err.println(new java.util.Date() + "\t getSOAPRequest getSOAPRequest Marshal="
				+ (System.currentTimeMillis() - start) + " ms");

		SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();
		SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
		SOAPHeader header = null;
		if (DME2Constants.GRMAUTHENABLED) {
			System.err.println(new java.util.Date() + "\t getSOAPRequest Adding WSSecurity header");

			soapEnvelope.addNamespaceDeclaration("wsu",
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

			if (soapEnvelope.getHeader() == null)
				header = soapEnvelope.addHeader();
			else
				header = soapEnvelope.getHeader();

			String namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
			SOAPElement securityElement = header
					.addHeaderElement(soapEnvelope.createName("Security", "wsse", namespace));
			securityElement.addNamespaceDeclaration("", namespace);
			SOAPElement usernameTokenElement = securityElement
					.addChildElement(soapEnvelope.createName("UsernameToken", "wsse", namespace));
			usernameTokenElement.addNamespaceDeclaration("", namespace);

			SOAPElement usernameElement = usernameTokenElement.addChildElement("Username", "wsse");
			SOAPElement passwordElement = usernameTokenElement.addChildElement("Password", "wsse");

//			usernameElement.setValue("wsse");
//			passwordElement.setValue("wsse"); 
			usernameElement.setValue(config.getProperty(DME2Constants.DME2_GRM_USER));
			passwordElement.setValue(config.getProperty(DME2Constants.DME2_GRM_PASS));
			
			soapMessage.saveChanges();
		}

		SOAPBody soapBody = soapEnvelope.getBody();
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

		soapBody.addDocument(docBuilder.parse(new ByteArrayInputStream(sw.getBuffer().toString().getBytes())));
		soapMessage.saveChanges();
		ByteArrayOutputStream soapMsgWriter = new ByteArrayOutputStream();
		soapMessage.writeTo(soapMsgWriter);
		System.err.println(new java.util.Date() + "\t getSOAPRequest getSOAPRequest SOAPMessageBuild="
				+ (System.currentTimeMillis() - start) + " ms");

		return new String(soapMsgWriter.toByteArray());
	}

	private String getEnvLetter() {
		if (envLetter != null) {
			return envLetter;
		}
		String platStr = config.getProperty("platform");
		String aftEnv = config.getProperty("AFT_ENVIRONMENT");

		if (aftEnv != null && platStr == null) {
			if (aftEnv.equalsIgnoreCase("AFTUAT")) {
				// Modifying env letter for D so that test cases always use
				// INFRA TEST or LAB
				envLetter = "D";
			} else if (aftEnv.equalsIgnoreCase("AFTPRD")) {
				envLetter = "P";
			} else {
				envLetter = null;
			}
		}
		if (aftEnv == null && platStr == null) {
			envLetter = null;
		}
		if (platStr != null) {
			if (platStr.equalsIgnoreCase("SANDBOX-DEV")) {
				envLetter = "D";
			} else if (platStr.equalsIgnoreCase("SANDBOX-LAB")) {
				envLetter = "L";
			} else if (platStr.equalsIgnoreCase("NON-PROD")) {
				envLetter = "T";
			} else if (platStr.equalsIgnoreCase("PROD")) {
				envLetter = "P";
			} else {
				// assume default SANDBOX-DEV which supports DEV and TEST that's
				// on there.
				envLetter = "D";
			}
		}

		return envLetter;
	}

	private String getGrmUrl() {
		if (urls == null) {
			synchronized (this) {
				if (urls == null) {
					urls = System.getProperty("AFT_DME2_GRM_URLS");
					if (urls == null) {
						urls = config.getProperty("AFT_DME2_GRM_URLS",
								"aftdsc:///?service=SOACloudEndpointRegistryGRMLWPService&version=1.0&bindingType=http&envContext="
										+ getEnvLetter());
					} else {
						logger.info(null, "getGrmUrl", "Using override GRM URLs: " + urls);
					}
				}
			}
		}
		return urls;
	}

	/**
	 * 
	 * @param request
	 * @return
	 * @throws DME2Exception
	 * @throws Exception
	 */
	private String invokeGRMService(String request) throws DME2Exception {
		ArrayList<String> offerList = new ArrayList<String>();
		long start = System.currentTimeMillis();
		String envLetter = getEnvLetter();
		if (envLetter == null) {
			throw new DME2Exception("AFT-DME2-0901", new ErrorContext().add("urls", this.urls));
		}

		BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create(config));
		Iterator<String> it = grm.getGrmEndPointDiscovery().getGRMEndpoints().iterator();
		OfferCache offerCache = OfferCache.getInstance();

		while (it.hasNext()) {
			HttpURLConnection conn = null;
			URL url = null;
			InputStream istream = null;
			String offer = null;
			// The inner try..catch is for offer-specific errors
			try {
				offer = it.next();
				System.err.println(new java.util.Date() + "\t GRMAccessor.invokeGRMService;Attempting Offer: " + offer);
				url = new URL(offer);
				if (offerCache.isStale(offer)) {
					continue;
				}
				conn = (HttpURLConnection) url.openConnection();
				// Set the connection headers
				// setUrlRequestHeaders(conn);
				int timeout = DEFAULT_TIMEOUT;
				conn.setConnectTimeout(timeout);
				conn.setReadTimeout(timeout);
				sendRequest(conn, request);
				istream = conn.getInputStream();
				int respCode = conn.getResponseCode();
				if (respCode != 200) {
					String faultResponse = parseResponse(istream);
					System.err.println(new java.util.Date() + "\t FaultResponse from GRMService \t" + faultResponse);
					throw new Exception(" GRM Service Call Failed; StatusCode=" + respCode);
				}
				System.err.println(new java.util.Date() + "\t ElapsedTime from GRMService \t"
						+ (System.currentTimeMillis() - start));
				String response = parseResponse(istream);

				return response;
			} // Catch errors from service response payload
			catch (java.net.ConnectException e) {
				System.err.println(new java.util.Date() + "\t ConnectException from GRMService for offer \t" + offer
						+ ";Exception=" + e.getMessage());
				e.printStackTrace();
				logger.error(null, "invokeGRMService",
						"Code=Exception.GRMAccessor.invokeGRMService;OfferString=" + offer + ";Error=" + e.toString());
				// this will mark the Offer as stale from an error
				it.remove();
				offerCache.setStale(offer);
				offerList.add(offer);
			} // Catch server down event exception
			catch (java.net.SocketTimeoutException e) {
				System.err.println(new java.util.Date() + "\t SocketTimeoutException from GRMService for offer \t"
						+ offer + ";Exception=" + e.getMessage());
				e.printStackTrace();
				logger.error(null, "invokeGRMService",
						"Code=Exception.GRMAccessor.invokeGRMService;OfferString=" + offer + ";Error=" + e.toString());
				it.remove();
				offerCache.setStale(offer);
				offerList.add(offer);
			} // Catch any failover event exception
			catch (Throwable e) {

				System.err.println(new java.util.Date() + "\t Throwable from GRMService for offer \t" + offer
						+ ";Exception=" + e.getMessage());
				e.printStackTrace();

				logger.error(null, "invokeGRMService",
						"Code=Exception.GRMAccessor.invokeGRMService;OfferString=" + offer + ";Error=" + e.toString());
				it.remove();
				offerCache.setStale(offer);
				offerList.add(offer);

			} finally {
				// close the stream if open
				if (istream != null) {
					try {
						istream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} // end while offer loop
		offerCache.removeStaleness(offerList);
		throw new DME2Exception("AFT-DME2-0902", new ErrorContext().add("urls", this.urls));
	}

	private String parseResponse(InputStream istream) throws IOException {
		byte[] buffer = new byte[1024];
		int iter_length = 0; // number of characters read for each iter
		int total_length = 0; // total length read from stream
		StringBuffer strBuffer = new StringBuffer();
		while (iter_length != -1) {
			iter_length = istream.read(buffer, 0, 1024);
			if (iter_length >= 0) {
				String tmpStr = new String(buffer, 0, iter_length);
				total_length = total_length + iter_length;
				strBuffer.append(tmpStr);
			}
		}
		return strBuffer.toString();
	}

	protected void sendRequest(HttpURLConnection conn, String request) throws Exception {
		System.err.println(new java.util.Date() + "\t Request XML to GRM | " + request);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		OutputStream out = conn.getOutputStream();
		out.write(request.getBytes());
		out.flush();
		out.close();
	}

	private String getResponse(String xml) throws SOAPException, DOMException, DME2Exception,
			TransformerFactoryConfigurationError, TransformerException {
		String replyString = null;
		System.err.println(new java.util.Date() + "\t Response XML from GRM | " + xml);
		MessageFactory factory = MessageFactory.newInstance();
		SOAPMessage rspMessage = factory.createMessage();
		StreamSource preppedMsgSrc = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
		rspMessage.getSOAPPart().setContent(preppedMsgSrc);
		rspMessage.saveChanges();

		if (rspMessage.getSOAPBody().hasFault()) {
			SOAPFault fault = rspMessage.getSOAPBody().getFault();
			System.err.println(new java.util.Date() + "\t GRMAccessor getResponse hasFault;" + fault.getTextContent());
			// special handling - we are returning the remote code/exception
			// data
			throw new DME2Exception(fault.getFaultString(), fault.getDetail().getTextContent());
		} else {
			SOAPBody soapBody = rspMessage.getSOAPBody();
			Node node = soapBody.getFirstChild();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(node), new StreamResult(bos));
			replyString = new String(bos.toByteArray());
		}
		return replyString;
	}

	private DME2Exception processException(Throwable e, String method, String data) {
		String logcode = null;
		String code = null;

		if (e instanceof JAXBException) {
			logcode = "Code=Exception.GRMAccessor." + method + ".JAXB;Error=";
			code = "AFT-DME2-0903";
		} else if (e instanceof SOAPException) {
			logcode = "Code=Exception.GRMAccessor." + method + ".SOAP;Error=";
			code = "AFT-DME2-0904";
		} else if (e instanceof ParserConfigurationException) {
			logcode = "Code=Exception.GRMAccessor." + method + ".ParseConfig;Error=";
			code = "AFT-DME2-0905";
		} else if (e instanceof SAXException) {
			logcode = "Code=Exception.GRMAccessor." + method + ".Parse;Error=";
			code = "AFT-DME2-0906";
		} else if (e instanceof IOException) {
			logcode = "Code=Exception.GRMAccessor." + method + ".IO;Error=";
			code = "AFT-DME2-0907";
		} else if (e instanceof DOMException) {
			logcode = "Code=Exception.GRMAccessor." + method + ".DOM;Error=";
			code = "AFT-DME2-0908";
		} else if (e instanceof TransformerFactoryConfigurationError) {
			logcode = "Code=Exception.GRMAccessor." + method + ".TRANSFORM;Error=";
			code = "AFT-DME2-0909";
		} else if (e instanceof TransformerException) {
			logcode = "Code=Exception.GRMAccessor." + method + ".TRANSFORM;Error=";
			code = "AFT-DME2-0910";
		} else if (e instanceof DME2Exception) {
			return (DME2Exception) e;
		} else {
			logcode = "Code=Exception.GRMAccessor." + method + ".UNKNOWN;Error=";
			code = "AFT-DME2-0911";
		}
		logger.error(null, "processException", logcode + e.toString(), e);
		return new DME2Exception(code, new ErrorContext().add("extendedData", data));
	}

	private static synchronized JAXBContext initContext() {
		JAXBContext context = null;
		try {
			context = JAXBContext.newInstance(GetRouteInfoRequest.class, GetRouteInfoResponse.class,
					LockRouteInfoForEditRequest.class, LockRouteInfoForEditResponse.class, ServiceDefinition.class,
					RouteInfo.class, SaveNReleaseRouteInfoResponse.class, SaveNReleaseRouteInfoRequest.class);
			return context;
		} catch (Exception e) {
			return null;
		}
	}
}
