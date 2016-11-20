/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

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

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.registry.accessor.SoapGRMAccessor;
import com.att.aft.dme2.types.ObjectFactory;
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
	private static final String CLASSNAME = SoapGRMAccessor.class.getName();
	private static final Logger logger = LoggerFactory.getLogger( CLASSNAME );
	final static int DEFAULT_TIMEOUT = 60000;
	private String envLetter = null;
	private String urls = null;

	private static final JAXBContext context = initContext();
	private static DME2Configuration config;

	/**
	 * Initialize environment - especially required system properties.
	 */
	public static Properties init() {
		return init( TestConstants.GRM_PLATFORM_TO_USE );
	}

	public static Properties init( String platform ) {
		Properties props = new Properties();
		props.setProperty( "DME2_EP_REGISTRY_CLASS", "DME2GRM" );
		props.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
		props.setProperty( "AFT_LATITUDE", "33.373900" );
		props.setProperty( "AFT_LONGITUDE", "-86.798300" );

		System.setProperty( "AFT_ENVIRONMENT", "AFTUAT" );
		System.setProperty( "AFT_LATITUDE", "33.373900" );
		System.setProperty( "AFT_LONGITUDE", "-86.798300" );
		System.setProperty( "platform", platform );

		config = new DME2Configuration( "DME2GRM", props );

		return props;
	}

	/**
	 * @param req
	 * @return
	 * @throws JAXBException
	 * @throws SOAPException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws Exception
	 */
	public static String getSOAPRequest( Object req ) throws JAXBException,
	SOAPException, ParserConfigurationException, SAXException,
	IOException {
		long start = System.currentTimeMillis();

		// marshaller = initMarshaller();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
		marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
		StringWriter sw = new StringWriter();
		marshaller.marshal( req, sw );
		System.err.println( new java.util.Date()
				+ " getSOAPRequest getSOAPRequest Marshal="
				+ ( System.currentTimeMillis() - start ) + " ms" );

		SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();
		SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
		SOAPHeader header = null;

		if ( config.getBoolean( DME2Constants.DME2_GRM_AUTH ) ) {
			System.err.println( new java.util.Date()
					+ " getSOAPRequest Adding WSSecurity header" );
			soapEnvelope
			.addNamespaceDeclaration(
					"wsu",
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" );

			if ( soapEnvelope.getHeader() == null ) {
				header = soapEnvelope.addHeader();
			} else {
				header = soapEnvelope.getHeader();
			}

			String namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
			SOAPElement securityElement = header.addHeaderElement( soapEnvelope
					.createName( "Security", "wsse", namespace ) );
			securityElement.addNamespaceDeclaration( "", namespace );
			SOAPElement usernameTokenElement = securityElement
					.addChildElement( soapEnvelope.createName( "UsernameToken",
							"wsse", namespace ) );
			usernameTokenElement.addNamespaceDeclaration( "", namespace );

			SOAPElement usernameElement = usernameTokenElement.addChildElement(
					"Username", "wsse" );
			SOAPElement passwordElement = usernameTokenElement.addChildElement(
					"Password", "wsse" );

			usernameElement.setValue( config.getProperty( DME2Constants.DME2_GRM_USER ) );
			passwordElement.setValue( config.getProperty( DME2Constants.DME2_GRM_PASS ) );
			soapMessage.saveChanges();
		}

		SOAPBody soapBody = soapEnvelope.getBody();
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		docBuilderFactory.setNamespaceAware( true );
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

		soapBody.addDocument( docBuilder.parse( new ByteArrayInputStream( sw
				.getBuffer().toString().getBytes() ) ) );
		soapMessage.saveChanges();
		ByteArrayOutputStream soapMsgWriter = new ByteArrayOutputStream();
		soapMessage.writeTo( soapMsgWriter );
		System.err.println( new java.util.Date()
				+ " getSOAPRequest getSOAPRequest SOAPMessageBuild="
				+ ( System.currentTimeMillis() - start ) + " ms" );
		return new String( soapMsgWriter.toByteArray() );
	}

	private String getEnvLetter( DME2Configuration config ) {
		if ( envLetter != null ) {
			return envLetter;
		}
		String platStr = config.getProperty( "platform" );
		String aftEnv = config.getProperty( "AFT_ENVIRONMENT" );

		if ( aftEnv != null && platStr == null ) {
			if ( aftEnv.equalsIgnoreCase( "AFTUAT" ) ) {
				// Modifying env letter for D so that test cases always use
				// INFRA TEST or LAB
				envLetter = "D";
			} else if ( aftEnv.equalsIgnoreCase( "AFTPRD" ) ) {
				envLetter = "P";
			} else {
				envLetter = null;
			}
		}
		if ( aftEnv == null && platStr == null ) {
			envLetter = null;
		}
		if ( platStr != null ) {
			if ( platStr.equalsIgnoreCase( "SANDBOX-DEV" ) ) {
				envLetter = "D";
			} else if ( platStr.equalsIgnoreCase( "SANDBOX-LAB" ) ) {
				envLetter = "L";
			} else if ( platStr.equalsIgnoreCase( "NON-PROD" ) ) {
				envLetter = "T";
			} else if ( platStr.equalsIgnoreCase( "PROD" ) ) {
				envLetter = "P";
			} else {
				// assume default SANDBOX-DEV which supports DEV and TEST that's on there.
				envLetter = "D";
			}
		}

		return envLetter;
	}

	private String getGrmUrl( DME2Configuration config ) {
		if ( urls == null ) {
			synchronized ( this ) {
				if ( urls == null ) {
					urls = System.getProperty( "AFT_DME2_GRM_URLS" );
					if ( urls == null ) {
						urls = config.getProperty( "AFT_DME2_GRM_URLS",
								"aftdsc:///?service=SOACloudEndpointRegistryGRMLWPService&version=1.0&bindingType=http&envContext=" +
										getEnvLetter( config ) );
						// This is a hack, because we need to be able to use setOverrideProperty in other places, but don't
						// have a true default
						if ( StringUtils.isEmpty( urls ) ) {
							urls =
									"aftdsc:///?service=SOACloudEndpointRegistryGRMLWPService&version=1.0&bindingType=http&envContext=" +
											getEnvLetter( config );
						}
					} else {
						logger.info( null, "getGrmUrl", "Using override GRM URLs: {}", urls );
					}
				}
			}
		}
		return urls;
	}

	/**
	 * @param request
	 * @return
	 * @throws DME2Exception
	 * @throws Exception
	 */
	private String invokeGRMService( DME2Configuration config, String request ) throws DME2Exception {
		ArrayList<String> offerList = new ArrayList<String>();
		long start = System.currentTimeMillis();
		String envLetter = getEnvLetter( config );
		if ( envLetter == null ) {
			throw new DME2Exception( "AFT-DME2-0901",
					new ErrorContext().add( "urls", this.urls ) );
		}

		BaseAccessor grm = GRMAccessorFactory.getInstance().getGrmAccessorHandlerInstance(config, SecurityContext.create(config));
		Iterator<String> it = grm.getGrmEndPointDiscovery().getGRMEndpoints().iterator();
		OfferCache offerCache = OfferCache.getInstance();

		while ( it.hasNext() ) {
			HttpURLConnection conn = null;
			URL url = null;
			InputStream istream = null;
			String offer = null;
			// The inner try..catch is for offer-specific errors
			try {
				offer = it.next();
				System.err
				.println( new java.util.Date()
						+ " GRMAccessor.invokeGRMService;Attempting Offer: "
						+ offer );
				url = new URL( offer );
				if ( offerCache.isStale( offer ) ) {
					continue;
				}
				conn = (HttpURLConnection) url.openConnection();
				// Set the connection headers
				// setUrlRequestHeaders(conn);
				int timeout = DEFAULT_TIMEOUT;
				conn.setConnectTimeout( timeout );
				conn.setReadTimeout( timeout );
				sendRequest( conn, request );
				istream = conn.getInputStream();
				int respCode = conn.getResponseCode();
				if ( respCode != 200 ) {
					String faultResponse = parseResponse( istream );
					System.err.println( new java.util.Date()
							+ " FaultResponse from GRMService "
							+ faultResponse );
					throw new Exception( " GRM Service Call Failed; StatusCode="
							+ respCode );
				}
				System.err.println( new java.util.Date()
						+ " ElapsedTime from GRMService "
						+ ( System.currentTimeMillis() - start ) );
				String response = parseResponse( istream );

				return response;
			} // Catch errors from service response payload
			catch ( java.net.ConnectException e ) {
				System.err
				.println( new java.util.Date()
						+ " ConnectException from GRMService for offer "
						+ offer + ";Exception=" + e.getMessage() );
				e.printStackTrace();
				logger
				.error( null, null, "Code=Exception.GRMAccessor.invokeGRMService;OfferString="
						+ offer + ";Error=" + e.toString() );
				// this will mark the Offer as stale from an error
				it.remove();
				offerCache.setStale( offer );
				offerList.add( offer );
			} // Catch server down event exception
			catch ( java.net.SocketTimeoutException e ) {
				System.err
				.println( new java.util.Date()
						+ " SocketTimeoutException from GRMService for offer "
						+ offer + ";Exception=" + e.getMessage() );
				e.printStackTrace();
				logger
				.error( null, null, "Code=Exception.GRMAccessor.invokeGRMService;OfferString="
						+ offer + ";Error=" + e.toString() );
				it.remove();
				offerCache.setStale( offer );
				offerList.add( offer );
			} // Catch any failover event exception
			catch ( Throwable e ) {
				System.err.println( new java.util.Date()
						+ " Throwable from GRMService for offer "
						+ offer + ";Exception=" + e.getMessage() );
				e.printStackTrace();
				logger
				.error( null, null, "Code=Exception.GRMAccessor.invokeGRMService;OfferString="
						+ offer + ";Error=" + e.toString() );
				it.remove();
				offerCache.setStale( offer );
				offerList.add( offer );

			} finally {
				// close the stream if open
				if ( istream != null ) {
					try {
						istream.close();
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
			}
		} // end while offer loop
		offerCache.removeStaleness( offerList );
		throw new DME2Exception( "AFT-DME2-0902", new ErrorContext().add( "urls", this.urls ) );
	}

	private String parseResponse( InputStream istream ) throws IOException {
		byte[] buffer = new byte[1024];
		int iter_length = 0; // number of characters read for each iter
		int total_length = 0; // total length read from stream
		StringBuffer strBuffer = new StringBuffer();
		while ( iter_length != -1 ) {
			iter_length = istream.read( buffer, 0, 1024 );
			if ( iter_length >= 0 ) {
				String tmpStr = new String( buffer, 0, iter_length );
				total_length = total_length + iter_length;
				strBuffer.append( tmpStr );
			}
		}
		return strBuffer.toString();
	}

	protected void sendRequest( HttpURLConnection conn, String request )
			throws Exception {
		System.err.println( new java.util.Date()
				+ " Request XML to GRM | " + request );
		conn.setDoInput( true );
		conn.setDoOutput( true );
		OutputStream out = conn.getOutputStream();
		out.write( request.getBytes() );
		out.flush();
		out.close();
	}

	public void saveRouteInfoInGRM( DME2Configuration config, RouteInfo routeInfo, String env ) {

		try {
			// Create a Service Definition
			ServiceDefinition serviceDef = new ServiceDefinition();
			serviceDef.setName( routeInfo.getServiceName() );

			// Create saveRouteInfoRequest
			SaveNReleaseRouteInfoRequest request = new SaveNReleaseRouteInfoRequest();
			request.setEnv( env );
			request.setServiceDefinition( serviceDef );
			request.setUserId( "ts6388" );

			StringWriter sw = new StringWriter();

			//Marshall routeInfo into XML
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
			marshaller.marshal(
					new JAXBElement<RouteInfo>( new QName( "http://aft.att.com/dme2/types", "routeInfo" ), RouteInfo.class,
							routeInfo ), sw );
			request.setRouteInfoXml( sw.toString() );

			String formattedSOAPRequest = getSOAPRequest( request );
			invokeGRMService( config, formattedSOAPRequest );
			Thread.sleep( 10000 );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	public boolean saveRouteInfo( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//String xmlContent = new String();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal(
					new JAXBElement<RouteInfo>( new QName( "http://aft.att.com/dme2/types", "routeInfo" ), RouteInfo.class,
							rtInfo ), sw );

			//marshaller.marshal(new JAXBElement<RouteInfo>(new QName("uri","local"), RouteInfo.class, rtInfo), sw);
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.MyService\"  envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<partner>test3</partner>" );
			sb.append( "<route name=\"test1\">" );
			sb.append( "<versionSelector>1.0.0</versionSelector>" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"test2\">" );
			sb.append( "<versionSelector>2.0.0</versionSelector>" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"test3\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	public boolean saveRouteInfoCacheAttemptsRefreshRouteInfo( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//String xmlContent = new String();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.TestGrmRefreshAttemptsCachedRouteInfo\"  envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<partner>test3</partner>" );
			sb.append( "<route name=\"test1\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"BAU_SE1\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"BAU_NE\" sequence=\"2\" active=\"true\"/>" );

			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	public boolean saveRouteInfoForRestfulROFailover( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.test.TestRestfulService1\"  envContext=\"LAB\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"TEST2\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<route name=\"test2\">" );
			sb
			.append( "<routeOffer name=\"TEST1\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"TEST2\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	public boolean saveRouteInfoForRestfulSearchROFailover( DME2Configuration config, RouteInfo rtInfo,
			String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			/*sb
						.append("<routeInfo serviceName=\"com.att.aft.dme2.test.TestRestfulService2\"  envContext=\"LAB\" xmlns=\"http://aft.att.com/dme2/types\">");
				sb.append("<dataPartitions>");
				sb.append("<dataPartitionKeyPath>/x/y/z</dataPartitionKeyPath>");
			   		sb.append("<dataPartition name=\"SE\" low=\"205977\" high=\"205999\"/>");
			   		sb.append("<dataPartition name=\"E\" low=\"205444\" high=\"205555\"/>");    
			   		sb.append("<dataPartition name=\"MW\" low=\"404707\" high=\"404707\"/>");
			   	sb.append("</dataPartitions>");
				sb.append("<routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>");
				sb.append("<route name=\"DEFAULT\">");
				sb.append("<dataPartitionRef>SE</dataPartitionRef>");
				sb
						.append("<routeOffer name=\"TEST2\" sequence=\"1\" active=\"true\"/>");
				sb.append("</route>");
				sb.append("</routeGroup>");
				sb.append("<routeGroup name=\"test1\">");
				sb.append("<partner>test1</partner>");
				sb.append("<partner>test2</partner>");
				sb.append("<route name=\"test2\">");
				sb.append("<dataPartitionRef>MW</dataPartitionRef>");
				sb
						.append("<routeOffer name=\"TEST1\" sequence=\"1\" active=\"true\"/>");
				sb
				.append("<routeOffer name=\"TEST2\" sequence=\"2\" active=\"true\"/>");
				sb.append("</route>");

				sb.append("<route name=\"stickyKey\">");
				sb.append("<dataPartitionRef>MW</dataPartitionRef>");
				sb.append("<stickySelectorKey>SSKEY1</stickySelectorKey>");
				sb
						.append("<routeOffer name=\"TEST2\" sequence=\"1\" active=\"true\"/>");
				sb
				.append("<routeOffer name=\"TEST1\" sequence=\"2\" active=\"true\"/>");
				sb.append("</route>");

				sb.append("<route name=\"justStickyKey\">");
				sb.append("<stickySelectorKey>SSKEY2</stickySelectorKey>");
				sb.append("<routeOffer name=\"TEST3\" sequence=\"1\" active=\"true\"/>");
				sb.append("</route>");


				sb.append("</routeGroup>");


				sb.append("</routeGroups>");
				sb.append("</routeInfo>");*/
			//StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.test.TestRestfulService2\"  envContext=\"LAB\" xmlns=\"http://aft.att.com/dme2/types\">" );
			sb.append( "<dataPartitions>" );
			sb.append( "<dataPartitionKeyPath>/x/y/z</dataPartitionKeyPath>" );
			sb.append( "<dataPartition name=\"SE\" low=\"205977\" high=\"205999\"/>" );
			sb.append( "<dataPartition name=\"E\" low=\"205444\" high=\"205555\"/>" );
			sb.append( "<dataPartition name=\"MW\" low=\"404707\" high=\"404707\"/>" );
			sb.append( "</dataPartitions>" );
			sb.append( "<routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb.append( "<dataPartitionRef>SE</dataPartitionRef>" );
			sb
			.append( "<routeOffer name=\"TEST2\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<route name=\"test2\">" );
			sb.append( "<dataPartitionRef>MW</dataPartitionRef>" );
			sb
			.append( "<routeOffer name=\"TEST1\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"TEST2\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"stickyKey\">" );
			sb.append( "<dataPartitionRef>MW</dataPartitionRef>" );
			sb.append( "<stickySelectorKey>SSKEY1</stickySelectorKey>" );
			sb
			.append( "<routeOffer name=\"TEST2\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"TEST1\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"justStickyKey\">" );
			sb.append( "<stickySelectorKey>SSKEY2</stickySelectorKey>" );
			sb.append( "<routeOffer name=\"TEST3\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "</routeGroup>" );

			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}

	}

	public boolean saveRouteInfoForPreferedRoute( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer\"  envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<partner>test3</partner>" );
			sb.append( "<route name=\"test1\">" );
			sb
			.append( "<routeOffer name=\"BAU_NE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"test2\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"test3\">" );
			sb
			.append( "<routeOffer name=\"BAU_NW\" sequence=\"3\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			System.out.println( "request=" + request );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	public boolean saveRouteInfoForStickyContextRoute( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.TestStickySelectoryKeyDME2JDBC\"  envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\">" );
			sb.append( "<routeGroups>" );
			sb.append( "<routeGroup name=\"WALMART\">" );
			sb.append( "<partner>WALMART</partner>" );
			sb.append( "<route name=\"DAL\">" );
			sb.append( "<stickySelectorKey>SOUTH</stickySelectorKey>" );
			sb.append( "<routeOffer name=\"WALMART_SOUTH\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"NY\">" );
			sb.append( "<stickySelectorKey>NORTH</stickySelectorKey>" );
			sb.append( "<routeOffer name=\"WALMART_NORTH\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"ATL\">" );
			sb.append( "<stickySelectorKey>EAST</stickySelectorKey>" );
			sb.append( "<routeOffer name=\"WALMART_ATL\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			System.out.println( "request=" + request );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	public boolean saveRouteInfoForDataContextRoute( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.TestStickyDataContextDME2JDBC\" serviceVersion=\"1.0.0\" envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\">" );

			sb.append( "<dataPartitions>" );
			sb.append( "<dataPartitionKeyPath>/x/y/z</dataPartitionKeyPath>" );
			sb.append( "<dataPartition name=\"SE\" low=\"205977\" high=\"205999\"/>" );
			sb.append( "<dataPartition name=\"E\" low=\"205444\" high=\"205555\"/>" );
			sb.append( "<dataPartition name=\"MW\" low=\"404707\" high=\"404707\"/>" );
			sb.append( "</dataPartitions>" );
			sb.append( "<routeGroups>" );
			sb.append( "<routeGroup name=\"TARGET\">" );
			sb.append( "<partner>TARGET</partner>" );
			sb.append( "<route name=\"SE\">" );
			sb.append( "<dataPartitionRef>SE</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"TARGET_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"E\">" );
			sb.append( "<dataPartitionRef>E</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"TARGET_E\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "<route name=\"MW\">" );
			sb.append( "<dataPartitionRef>MW</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"TARGET_MW\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );

			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			System.out.println( "request=" + request );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	public boolean saveRouteInfoForPreferedRouteWith_NO_BAU_SE( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer\" envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<partner>test3</partner>" );
			sb.append( "<route name=\"test1\">" );
			sb
			.append( "<routeOffer name=\"BAU_NE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			//sb.append("<route name=\"test2\">");
			//sb
			//		.append("<routeOffer name=\"BAU_SE\" sequence=\"2\" active=\"true\"/>");
			//sb.append("</route>");
			sb.append( "<route name=\"test3\">" );
			sb
			.append( "<routeOffer name=\"BAU_NW\" sequence=\"3\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	/**
	 * Adding a method that should suffice to lock and edit any route info passed to it, since the other methods here have
	 * hard coded route xml
	 */
	public boolean saveRouteInfo2( DME2Configuration config, RouteInfo routeInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( routeInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, routeInfo ), sw );
			req1.setRouteInfoXml( sw.toString() );

			StringWriter writer = new StringWriter();
			ObjectFactory objectFactory = new ObjectFactory();
			JAXBElement<RouteInfo> routeInfoDoc = objectFactory.createRouteInfo( routeInfo );
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			marshaller.marshal( routeInfoDoc, writer );
			req1.setRouteInfoXml( writer.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}

	}

	public boolean saveRouteInfoForROFailover( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.ROFailOverService\"  envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<route name=\"test2\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"ATL\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	private String getResponse( String xml )
			throws SOAPException, DOMException, DME2Exception, TransformerFactoryConfigurationError, TransformerException {
		String replyString = null;
		System.err.println( new java.util.Date()
				+ " Response XML from GRM | " + xml );
		MessageFactory factory = MessageFactory.newInstance();
		SOAPMessage rspMessage = factory.createMessage();
		StreamSource preppedMsgSrc = new StreamSource( new ByteArrayInputStream(
				xml.getBytes() ) );
		rspMessage.getSOAPPart().setContent( preppedMsgSrc );
		rspMessage.saveChanges();

		if ( rspMessage.getSOAPBody().hasFault() ) {
			SOAPFault fault = rspMessage.getSOAPBody().getFault();
			System.err.println( new java.util.Date()
					+ " GRMAccessor getResponse hasFault;"
					+ fault.getTextContent() );
			// special handling - we are returning the remote code/exception data
			throw new DME2Exception( fault.getFaultString(), fault
					.getDetail().getTextContent() );
		} else {
			SOAPBody soapBody = rspMessage.getSOAPBody();
			Node node = soapBody.getFirstChild();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.transform( new DOMSource( node ), new StreamResult( bos ) );
			replyString = new String( bos.toByteArray() );
		}
		return replyString;
	}

	private DME2Exception processException( Throwable e, String method, String data ) {
		String logcode = null;
		String code = null;

		if ( e instanceof JAXBException ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".JAXB;Error=";
			code = "AFT-DME2-0903";
		} else if ( e instanceof SOAPException ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".SOAP;Error=";
			code = "AFT-DME2-0904";
		} else if ( e instanceof ParserConfigurationException ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".ParseConfig;Error=";
			code = "AFT-DME2-0905";
		} else if ( e instanceof SAXException ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".Parse;Error=";
			code = "AFT-DME2-0906";
		} else if ( e instanceof IOException ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".IO;Error=";
			code = "AFT-DME2-0907";
		} else if ( e instanceof DOMException ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".DOM;Error=";
			code = "AFT-DME2-0908";
		} else if ( e instanceof TransformerFactoryConfigurationError ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".TRANSFORM;Error=";
			code = "AFT-DME2-0909";
		} else if ( e instanceof TransformerException ) {
			logcode = "Code=Exception.GRMAccessor." + method + ".TRANSFORM;Error=";
			code = "AFT-DME2-0910";
		} else if ( e instanceof DME2Exception ) {
			return (DME2Exception) e;
		} else {
			logcode = "Code=Exception.GRMAccessor." + method + ".UNKNOWN;Error=";
			code = "AFT-DME2-0911";
		}

		logger.error( null, "processException", logcode, e );
		return new DME2Exception( code, new ErrorContext()
				.add( "extendedData", data ) );
	}

	private static synchronized JAXBContext initContext() {
		JAXBContext context = null;
		try {
			context = JAXBContext.newInstance( GetRouteInfoRequest.class,
					GetRouteInfoResponse.class, LockRouteInfoForEditRequest.class,
					LockRouteInfoForEditResponse.class, ServiceDefinition.class,
					RouteInfo.class, SaveNReleaseRouteInfoResponse.class, SaveNReleaseRouteInfoRequest.class );
			return context;
		} catch ( Exception e ) {
			return null;
		}
	}

	public boolean saveRouteInfoForRoundTripFailover( DME2Configuration config, RouteInfo rtInfo,
			String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.TestExchangeRoundTripTimeoutFailover\"  envContext=\"LAB\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<route name=\"test2\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"BAU_NE\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}

	}

	public boolean saveRouteInfoForEofFailover( DME2Configuration config, RouteInfo rtInfo, String env )
			throws Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb
			.append(
					"<routeInfo serviceName=\"com.att.aft.dme2.TestEofExceptionFailOver\" envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\"><routeGroups><routeGroup name=\"DEFAULT\"><partner>SET</partner>" );
			sb.append( "<route name=\"DEFAULT\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<route name=\"test2\">" );
			sb
			.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb
			.append( "<routeOffer name=\"ATL\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this
			.processException( e, "TestEofExceptionFailOver", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}

	}

	public boolean saveRouteInfoWithDataPartition( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb.append( "<routeInfo serviceName=\"" + rtInfo.getServiceName() +
					"\"  envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\">" );
			sb.append( "<dataPartitions>" );
			sb.append( "<dataPartitionKeyPath>x/y/z</dataPartitionKeyPath>" );
			sb.append( "<dataPartition name=\"GLRRouting\" low=\"GLR\" high=\"GLR\"/>" );
			sb.append( "<dataPartition name=\"DSRRouting\" low=\"DSR\" high=\"DSR\"/>" );
			sb.append( "<dataPartition name=\"PACRouting\" low=\"PAC\" high=\"PAC\"/>" );
			sb.append( "<dataPartition name=\"AKTRouting\" low=\"AKT\" high=\"AKT\"/>" );
			sb.append( "</dataPartitions>" );
			sb.append( "<routeGroups>" );
			sb.append( "<routeGroup name=\"DEFAULT\"><partner>SET</partner>" );

			sb.append( "<route name=\"DEFAULT\">" );
			sb.append( "<dataPartitionRef>GLRRouting</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT1\">" );
			sb.append( "<dataPartitionRef>DSRRouting</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT2\">" );
			sb.append( "<dataPartitionRef>PACRouting</dataPartitionRef>" );
			sb.append( "<stickySelectorKey>Q24A</stickySelectorKey>" );
			sb.append( "<routeOffer name=\"BAU_SW\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT3\">" );
			sb.append( "<dataPartitionRef>AKTRouting</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"BAU_SW\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "</routeGroup>" );

			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<route name=\"test2\">" );
			sb.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "<routeOffer name=\"ATL\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

	public boolean saveRouteInfoWithListAndRangeDataPartition( DME2Configuration config,
			RouteInfo rtInfo, String env ) throws DME2Exception {
		LockRouteInfoForEditRequest req = new LockRouteInfoForEditRequest();
		String userId = "ts6388";
		try {

			ServiceDefinition sd = new ServiceDefinition();
			sd.setName( rtInfo.getServiceName() );
			req.setEnv( env );
			req.setServiceDefinition( sd );
			req.setUserId( userId );

			String request = RegistryGrmSetup.getSOAPRequest( req );
			String reply = invokeGRMService( config, request );
			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp
						.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				LockRouteInfoForEditResponse element = LockRouteInfoForEditResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					// Ignore exception since it might be due to previous lock attempts did not 
					// release. But save and release step below would fail for diff user
					// throw new Exception(res.getResultText());
				}
			}

			SaveNReleaseRouteInfoRequest req1 = new SaveNReleaseRouteInfoRequest();
			req1.setEnv( env );
			req1.setServiceDefinition( sd );
			req1.setUserId( userId );
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
			StringWriter sw = new StringWriter();

			marshaller.marshal( new JAXBElement<RouteInfo>( new QName( "uri", "local" ), RouteInfo.class, rtInfo ), sw );
			StringBuffer sb = new StringBuffer();
			sb.append( "<routeInfo serviceName=\"" + rtInfo.getServiceName() +
					"\"  envContext=\"DEV\" xmlns=\"http://aft.att.com/dme2/types\">" );
			sb.append( "<dataPartitions>" );
			sb.append( "<dataPartitionKeyPath>x/y/z</dataPartitionKeyPath>" );
			sb.append( "<listDataPartition name=\"LISTRouting\">" );
			sb.append( "<value>LISTVAL1</value>" );
			sb.append( "<value>LISTVAL2</value>" );
			sb.append( "<value>LISTVAL3</value>" );
			sb.append( "</listDataPartition>" );
			sb.append( "<dataPartition name=\"GLRRouting\" low=\"GLR\" high=\"GLR\"/>" );
			sb.append( "<dataPartition name=\"DSRRouting\" low=\"DSR\" high=\"DSR\"/>" );
			sb.append( "<dataPartition name=\"PACRouting\" low=\"PAC\" high=\"PAC\"/>" );
			sb.append( "<dataPartition name=\"AKTRouting\" low=\"AKT\" high=\"AKT\"/>" );
			sb.append( "</dataPartitions>" );
			sb.append( "<routeGroups>" );
			sb.append( "<routeGroup name=\"DEFAULT\"><partner>SET</partner>" );

			sb.append( "<route name=\"DEFAULT\">" );
			sb.append( "<dataPartitionRef>GLRRouting</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT1\">" );
			sb.append( "<dataPartitionRef>DSRRouting</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT2\">" );
			sb.append( "<dataPartitionRef>PACRouting</dataPartitionRef>" );
			sb.append( "<stickySelectorKey>Q24A</stickySelectorKey>" );
			sb.append( "<routeOffer name=\"BAU_SW\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT3\">" );
			sb.append( "<dataPartitionRef>AKTRouting</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"BAU_SW\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT4\">" );
			sb.append( "<dataPartitionRef>LISTRouting</dataPartitionRef>" );
			sb.append( "<routeOffer name=\"BAU_NW\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "<route name=\"DEFAULT6\">" );
			sb.append( "<stickySelectorKey>Q24A</stickySelectorKey>" );
			sb.append( "<routeOffer name=\"BAU_SW\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "</route>" );

			sb.append( "</routeGroup>" );

			sb.append( "<routeGroup name=\"test1\">" );
			sb.append( "<partner>test1</partner>" );
			sb.append( "<partner>test2</partner>" );
			sb.append( "<route name=\"test2\">" );
			sb.append( "<routeOffer name=\"BAU_SE\" sequence=\"1\" active=\"true\"/>" );
			sb.append( "<routeOffer name=\"ATL\" sequence=\"2\" active=\"true\"/>" );
			sb.append( "</route>" );
			sb.append( "</routeGroup>" );
			sb.append( "</routeGroups>" );
			sb.append( "</routeInfo>" );
			//req1.setRouteInfoXml(sw.toString());
			req1.setRouteInfoXml( sb.toString() );

			request = RegistryGrmSetup.getSOAPRequest( req1 );
			reply = invokeGRMService( config, request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				SaveNReleaseRouteInfoResponse element1 = SaveNReleaseRouteInfoResponse.class
						.cast( unmarshaller.unmarshal( input ) );

				Result res = element1.getResult();
				if ( res.getResultCode() == ResultCode.FAIL ) {
					throw new Exception( res.getResultText() );
				}
			}
			Thread.sleep( 10000 );
			return true;
		} catch ( Throwable e ) {
			e.printStackTrace();
			throw this.processException( e, "saveRouteInfo", req.getEnv() + "/" + req.getServiceDefinition().getName() );
		}
	}

}
