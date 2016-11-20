package com.att.aft.dme2.registry.accessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
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
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.registry.bootstrap.RegistryBootstrap;
import com.att.aft.dme2.registry.bootstrap.RegistryBootstrapFactory;
import com.att.aft.dme2.registry.dto.GRMEndpoint;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.DME2ValidationUtil;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.OfferCache;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;
import com.att.scld.grm.types.v1.ClientJVMInstance;
import com.att.scld.grm.types.v1.ContainerInstance;
import com.att.scld.grm.types.v1.LRM;
import com.att.scld.grm.types.v1.ServiceEndPoint;
import com.att.scld.grm.types.v1.ServiceVersionDefinition;
import com.att.scld.grm.types.v1.VersionDefinition;
import com.att.scld.grm.v1.AddServiceEndPointRequest;
import com.att.scld.grm.v1.AddServiceEndPointResponse;
import com.att.scld.grm.v1.DeleteServiceEndPointRequest;
import com.att.scld.grm.v1.DeleteServiceEndPointResponse;
import com.att.scld.grm.v1.FindClientJVMInstanceRequest;
import com.att.scld.grm.v1.FindClientJVMInstanceResponse;
import com.att.scld.grm.v1.FindRunningServiceEndPointRequest;
import com.att.scld.grm.v1.FindRunningServiceEndPointResponse;
import com.att.scld.grm.v1.FindServiceEndPointBySVDRequest;
import com.att.scld.grm.v1.FindServiceEndPointBySVDResponse;
import com.att.scld.grm.v1.FindServiceEndPointRequest;
import com.att.scld.grm.v1.FindServiceEndPointResponse;
import com.att.scld.grm.v1.GetRouteInfoRequest;
import com.att.scld.grm.v1.GetRouteInfoResponse;
import com.att.scld.grm.v1.RegisterClientJVMInstanceRequest;
import com.att.scld.grm.v1.RegisterClientJVMInstanceResponse;
import com.att.scld.grm.v1.UpdateServiceEndPointRequest;
import com.att.scld.grm.v1.UpdateServiceEndPointResponse;

/**
 * Accesses GRM via SOAP
 */
public class SoapGRMAccessor extends AbstractGRMAccessor implements BaseAccessor {
	private static final Logger logger = LoggerFactory.getLogger( SoapGRMAccessor.class.getName() );
	private static JAXBContext context;
	private static List<String> attemptedOffers = null; // @TODO: used in one unit test & debugging, also exist GRMTopoloogyAccessor can be moved to common ancestor
	private static final String MS = " ms";

	private static synchronized JAXBContext initContext( DME2Configuration config ) {
		JAXBContext context = null;
		try {
			context = JAXBContext.newInstance(
					GetRouteInfoRequest.class,
					GetRouteInfoResponse.class,
					FindServiceEndPointBySVDRequest.class,
					FindServiceEndPointBySVDResponse.class,
					FindRunningServiceEndPointRequest.class,
					FindRunningServiceEndPointResponse.class,
					FindServiceEndPointRequest.class,
					FindServiceEndPointResponse.class,
					AddServiceEndPointRequest.class,
					AddServiceEndPointResponse.class,
					DeleteServiceEndPointRequest.class,
					DeleteServiceEndPointResponse.class, UpdateServiceEndPointRequest.class, UpdateServiceEndPointResponse.class,
					RegisterClientJVMInstanceRequest.class, RegisterClientJVMInstanceResponse.class, ClientJVMInstance.class,
					FindClientJVMInstanceRequest.class, FindClientJVMInstanceResponse.class);
			return context;
		} catch ( Exception e ) {
			return null;
		}
	}

	/**
   +	 * use GRMServiceAccessorFactory#buildGRMServiceAccessor() to build this object.
	 * in the current implementation of IGRMEndPointDiscovery using DNS & GRM, 
	 * there is a dependency on GRMAccessor to get list of GRM server
	 * this creates a logical mutual dependency that is handled properly by creating this class through factory  
	 * 
	 * @see GRMAccessorFactory
	 * @see GRMEndPointsDiscoveryHelperGRM
	 * @param ctx security context
	 * @param grmEndPointDiscovery a reference to object that finds GRM Endpoints
	 */
	protected SoapGRMAccessor(DME2Configuration config, SecurityContext ctx, IGRMEndPointDiscovery grmEndPointDiscovery) { // force client to call factory class to create objects
		super(config, ctx, grmEndPointDiscovery);
		context = initContext( config );
	}

	@Override
	public void registerClientJVMInstance(RegisterClientJVMInstanceRequest req) throws DME2Exception {
		try {
			String request = getSOAPRequest(req);

			invokeGRMService(request);
		} catch (Throwable e) {
			throw this.processException(e, "registerClientJVMInstance", req.getEnv() + "/" + req.getClientJvmInstance().toString());
		}

	}

	@Override
	public List<ClientJVMInstance> findClientJVMInstance(FindClientJVMInstanceRequest req) throws DME2Exception {
		long start = System.currentTimeMillis();
		String reply = null;

		try {
			String request = getSOAPRequest( req );

			reply = invokeGRMService( request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream input = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				FindClientJVMInstanceResponse element =
						FindClientJVMInstanceResponse.class.cast( unmarshaller.unmarshal( input ) );

				//DME2Constants.debugIt("findClientJVMInstance GRMResponseParsingElapsed=" + (System.currentTimeMillis() - start) + MS);
				logger.debug( null, "findClientJVMInstance", "findClientJVMInstance GRMResponseParsingElapsed={} {}",
						( System.currentTimeMillis() - start ), MS );
				return element.getClientJVMInstanceList();
			}

		} catch ( Throwable e ) {
			throw this.processException( e, "findClientJVMInstance", req.getEnv() + "/" + req.toString() );
		}
		return null;
	}
	/**
	 * Adds given ServiceEndpoint in GRM
	 *
	 * @param input ServiceEndpoint
	 */
	public void addServiceEndPoint( ServiceEndpoint input ) throws DME2Exception {
		// build the sep from the endpoint
		ServiceEndPoint sep = buildGRMServiceEndpoint( input );

		// build the lrm.
		LRM lrm = buildLRM( input );

		// build container instance
		ContainerInstance ci = buildContainerInstance( input );

		// now we build the request
		AddServiceEndPointRequest req = new AddServiceEndPointRequest();
		req.setEnv( input.getEnv() );
		req.setLrmRef( lrm );
		req.setServiceEndPoint( sep );
		req.setCheckNcreateParents( true );
		if ( ci.getName() != null && !ci.getName().startsWith( "dummy" ) ) {
			req.setContainerInstanceRef( ci );
		}

		try {
			String request = getSOAPRequest( req );

			logger.debug( null, "addServiceEndPoint", "AddServiceEndpoint {} ", request );

			String reply = invokeGRMService( request );
			logger.debug( null, "addServiceEndPoint", "AddServiceEdnpoint Response {}", reply );
		} catch ( Throwable e ) {
			logger.debug( null, "addServiceEndPoint",
					"Error in invoking GRM addServiceEndpoint", e );
			throw this.processException( e, "addServiceEndPoint", req.getEnv()
					+ "/" + req.getServiceEndPoint().getName() + ":"
					+ req.getServiceEndPoint().getVersion() );
		}
	}

	/**
	 * Updates given ServiceEndpoint in GRM
	 *
	 * @param input ServiceEndpoint
	 */
	public void updateServiceEndPoint( ServiceEndpoint input ) throws DME2Exception {
		// Build a sep from the input.
		ServiceEndPoint sep = buildGRMServiceEndpoint( input );

		LRM lrm = buildLRM( input );
		UpdateServiceEndPointRequest req = new UpdateServiceEndPointRequest();
		req.setUpdateLease( true );
		req.setEnv( input.getEnv() );
		req.setLrmRef( lrm );

		ContainerInstance ci = buildContainerInstance( input );

		if ( ci.getName() != null && !ci.getName().startsWith( "dummy" ) ) {
			req.setContainerInstanceRef( ci );
		}

		req.setServiceEndPoint( sep );

		try {
			String request = getSOAPRequest( req );

			logger.debug( null, "updateServiceEndPoint",
					"UpdateServiceEndpoint " + request );

			invokeGRMService( request );
		} catch ( Throwable e ) {
			throw this.processException( e, "UpdateServiceEndPoint",
					req.getEnv() + "/" + req.getServiceEndPoint().getName()
					+ ":" + req.getServiceEndPoint().getVersion()
					);
		}
	}

	/**
	 * Deletes given ServiceEndpoint from GRM
	 *
	 * @param input ServiceEndpoint
	 */
	public void deleteServiceEndPoint( ServiceEndpoint input ) throws DME2Exception {
		ServiceEndPoint sep = buildGRMServiceEndpoint( input );

		DeleteServiceEndPointRequest req = new DeleteServiceEndPointRequest();
		req.setEnv( input.getEnv() );
		req.getServiceEndPoint().add( sep );

		try {
			String request = getSOAPRequest( req );
			String result = invokeGRMService( request );
			//logger.debug( null, "deleteServiceEndPoint", "SOAP GRM DELETE SEP RESULTS: {}", result );
		} catch ( Throwable e ) {
			throw this.processException( e, "DeleteServiceEndPoint",
					req.getEnv() + "/" + req.getServiceEndPoint().toString() );
		}

	}

	/**
	 * Fetches list of running endpoint for the given service given ServiceEndpoint from GRM
	 *
	 * @param input ServiceEndpoint
	 * @return List<ServiceEndpoint> - List of running end points
	 */
	public List<ServiceEndpoint> findRunningServiceEndPoint( ServiceEndpoint input ) throws DME2Exception {
		ServiceEndPoint sep = buildGRMServiceEndpoint( input );

		FindRunningServiceEndPointRequest req = new FindRunningServiceEndPointRequest();
		req.setEnv( input.getEnv() );
		req.setServiceEndPoint( sep );

		try {
			String request = getSOAPRequest( req );
			String reply = invokeGRMService( request );

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream ipStream = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				FindRunningServiceEndPointResponse element =
						FindRunningServiceEndPointResponse.class.cast( unmarshaller.unmarshal( ipStream ) );
				return convertToServiceEndpointList( element.getServiceEndPointList() );
			} else {
				return null;
			}
		} catch ( Throwable e ) {
			throw this.processException( e, "findRunningServiceEndPoint",
					input.getEnv() + "/" + input.getName() + ":" + input.getDmeVersion() );
		}
	}

	private List<ServiceEndpoint> convertToServiceEndpointList( List<ServiceEndPoint> serviceEndPointList ) {

		List<ServiceEndpoint> serviceEndPoints = new ArrayList<ServiceEndpoint>();
		for ( ServiceEndPoint sep : serviceEndPointList ) {
			serviceEndPoints.add( buildServiceEndpoint( sep ) );
		}
		return serviceEndPoints;
	}

	public String getRouteInfo( ServiceEndpoint input ) throws DME2Exception {
		ServiceVersionDefinition svd = new ServiceVersionDefinition();
		svd.setName( input.getName() );
		svd.setVersion( buildVersionDefinition( input.getVersion() ) );

		GetRouteInfoRequest req = new GetRouteInfoRequest();
		req.setEnv( input.getEnv() );
		req.setServiceVersionDefinition( svd );

		try {
			long start = System.currentTimeMillis();
			String request = this.getSOAPRequest( req );
			logger.debug( null, "getRouteInfo",
					"getRouteInfo getSOAPRequestJAXB=" + ( System.currentTimeMillis() - start ) + MS );
			start = System.currentTimeMillis();
			String reply = invokeGRMService( request );
			logger.debug( null, "getRouteInfo",
					"getRouteInfo getGRMResponseElapsed=" + ( System.currentTimeMillis() - start ) + MS );

			start = System.currentTimeMillis();

			if ( reply != null ) {
				String temp = getResponse( reply );
				InputStream ipStream = new ByteArrayInputStream( temp.getBytes( "UTF-8" ) );
				Unmarshaller unmarshaller = context.createUnmarshaller();
				GetRouteInfoResponse element = GetRouteInfoResponse.class.cast( unmarshaller.unmarshal( ipStream ) );
				logger.debug( null, "getRouteInfo",
						"getRouteInfo GRMResponseParsingElapsed=" + ( System.currentTimeMillis() - start ) + MS );

				return element.getRouteInfoXml();
			} else {
				return null;
			}
		} catch ( Throwable e ) {
			throw this.processException( e, "getRouteInfo", input.getEnv() + "/" + input.getName() );
		}
	}

	private String getResponse( String xml )
			throws SOAPException, DOMException, DME2Exception, TransformerFactoryConfigurationError, TransformerException {
		String replyString = null;

		logger.debug( null, "getResponse", "Response XML from GRM | "
				+ xml );

		MessageFactory factory = MessageFactory.newInstance();
		SOAPMessage rspMessage = factory.createMessage();
		StreamSource preppedMsgSrc = new StreamSource( new ByteArrayInputStream(
				xml.getBytes() ) );
		rspMessage.getSOAPPart().setContent( preppedMsgSrc );
		rspMessage.saveChanges();

		if ( rspMessage.getSOAPBody().hasFault() ) {
			SOAPFault fault = rspMessage.getSOAPBody().getFault();

			logger.debug(
					null,
					"getResponse",
					"GRMAccessor getResponse hasFault;"
							+ fault.getTextContent()
					);
			// special handling - we are returning the remote code/exception
			// data

			String errorCode = null;
			String errorMessage = null;

			if ( fault.getFaultString() != null ) {
				int i = fault.getFaultString().indexOf( "," );

				if ( i > -1 ) {
					errorCode = fault.getFaultString().substring( 0, i ).trim();
					errorMessage = fault.getFaultString().substring( i + 1 )
							.trim();

					if ( errorCode != null && errorCode.equals( "GRMSVC-2004" ) ) {
						// return reply since GRMSVC-2004 can be ignored
						// Ex [GRMSVC-2004]: Your trying to add a
						// ServiceEndPoint=com.att.test.FastFailService:1.1.0:135.70.251.142|9595:DEV
						// that already exists for request
						// addServiceEndPointRequest., [LDAP: error code 68 -
						// Entry Already Exists]

						SOAPBody soapBody = rspMessage.getSOAPBody();
						Node node = soapBody.getFirstChild();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						Transformer transformer = TransformerFactory
								.newInstance().newTransformer();
						transformer.transform( new DOMSource( node ),
								new StreamResult( bos ) );
						replyString = new String( bos.toByteArray() );

						return replyString;
					}
				} else {
					errorCode = "AFT-DME2-0612";
					errorMessage = fault.getFaultString();
				}
			} else {
				errorCode = "AFT-DME2-0612";
				errorMessage = "An unknown fault was returned from the GRM Service call: "
						+ fault.getFaultCode();
			}
			// throw DME2ErrorCatalog.createException(errorCode, new
			// ErrorContext().add("errorMessage", errorMessage));
			throw new DME2Exception( errorCode, new ErrorContext().add(
					"errorMessage", errorMessage ) );
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

	private String getSOAPRequest( Object req ) throws JAXBException,
	SOAPException, ParserConfigurationException, SAXException,
	IOException {
		long start = System.currentTimeMillis();

		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
		marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
		StringWriter sw = new StringWriter();
		marshaller.marshal( req, sw );
		logger.debug(
				null,
				"getSOAPRequest",
				"getSOAPRequest getSOAPRequest Marshal="
						+ ( System.currentTimeMillis() - start ) + MS
				);

		SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();
		SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
		SOAPHeader header = null;

		if ( config.getBoolean( DME2Constants.DME2_GRM_AUTH ) ) {
			logger.debug( null, "getSOAPRequest",
					"getSOAPRequest Adding WSSecurity header" );

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

		logger.debug(
				null,
				"getSOAPRequest",
				"getSOAPRequest getSOAPRequest SOAPMessageBuild="
						+ ( System.currentTimeMillis() - start ) + MS
				);
		return new String( soapMsgWriter.toByteArray() );
	}

	public List<String> getGRMEndpoints( List<GRMEndpoint> endPoints ) {

		// Holds the active non-stale endpoints that will be used to invoke GRM
		ArrayList<String> activeOfferList = new ArrayList<String>();

		// Holds endpoints that have been marked stale due to some error
		// condition
		ArrayList<String> staleOfferList = new ArrayList<String>();

		OfferCache offerCache = OfferCache.getInstance();

		for ( GRMEndpoint endPoint : endPoints ) {
			String endpoint = endPoint.getAddress();

			/*
			 * Get each endpoint from the EndpointReference Iterator. Check if
			 * each endpoint from the Iterator is stale. If it is not, then add
			 * it to the active offer list. If it is, then add it to the stale
			 * offer list.
			 */
			if ( !offerCache.isStale( endpoint ) ) {
				activeOfferList.add( endpoint );
			} else {
				staleOfferList.add( endpoint );
			}
		}// End iteration

		/*
		 * If the stale offer list has endpoints, add them to the END of the
		 * active offer list, so they will be tried last
		 */
		if ( !staleOfferList.isEmpty() ) {
			logger.debug( null, "getGRMEndpoints",
					"Adding stale GRM offers to the end of the active offer list. Stale Offers="
							+ staleOfferList.toString()
					);
			activeOfferList.addAll( activeOfferList.size(), staleOfferList );
		}

		logger.debug( null, "getGRMEndpoints",
				"////// Resolved the following GRM Endpoints from Discovery: "
						+ activeOfferList.toString()
				);
		return activeOfferList;
	}

	public String getDiscoveryURL() throws DME2Exception {
		if ( discoveryURL == null ) {
			discoveryURL =
					"aftdsc:///?service=SOACloudEndpointRegistryGRMLWPService&version=1.0&bindingType=http&envContext={ENV}";
			discoveryURL = discoveryURL.replace( "{ENV}", getEnvLetter() );
			logger.warn( null, "getDiscoveryURL", "DISCOVERY URL: {}", discoveryURL );
		}
		return discoveryURL;
	}

	public String getEnvLetter() {
		if ( envLetter != null ) {
			return envLetter;
		}

		String platStr = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY ) );

		if ( platStr == null ) {
			platStr = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_PLATFORM_KEY ) );
		}

		String aftEnv = config.getProperty( "AFT_ENVIRONMENT" );

		if ( aftEnv != null && platStr == null ) {
			if ( aftEnv.equalsIgnoreCase( "AFTUAT" ) ) {
				envLetter = "T";
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
				envLetter = null;
			}
		}
		return ( envLetter == null ? "" : envLetter );
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
			logcode = "Code=Exception.GRMAccessor." + method
					+ ".ParseConfig;Error=";
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
			logcode = "Code=Exception.GRMAccessor." + method
					+ ".TRANSFORM;Error=";
			code = "AFT-DME2-0909";
		} else if ( e instanceof TransformerException ) {
			logcode = "Code=Exception.GRMAccessor." + method
					+ ".TRANSFORM;Error=";
			code = "AFT-DME2-0910";
		} else if ( e instanceof DME2Exception ) {
			return (DME2Exception) e;
		} else {
			logcode = "Code=Exception.GRMAccessor." + method
					+ ".UNKNOWN;Error=";
			code = "AFT-DME2-0911";
		}

		logger.error( null, method, LogMessage.GRM_RETHROW, code, e );
		ErrorContext errCtx = new ErrorContext();
		errCtx.add( "extendedData", data );
		errCtx.add( "exceptionMessage", e.getMessage() );
		return new DME2Exception( code, errCtx );
	}

	private String invokeGRMService( String request ) throws DME2Exception {
		long start = System.currentTimeMillis();

		ArrayList<String> offerList = new ArrayList<String>();

		OfferCache offerCache = OfferCache.getInstance();
		String response = null;

		@SuppressWarnings( "unchecked" )
		List<String> endpointsToAttempt = grmEndPointDiscovery.getGRMEndpoints();

		/* Used for test and debugging purposes */
		boolean logAttemptedOffers = System.getProperty("AFT_DME2_DEBUG_GRM_EPS") != null && !request.contains("Metrics") && !request.contains("delete");
		if (logAttemptedOffers) {
			attemptedOffers = new ArrayList<String>();
		}

		// holders for "last" error
		String grmCode = null;
		String grmMessage = null;
		String faultMessage = null;

		long startTime = System.currentTimeMillis();
		int iterationCount = 0;
		String skipStaleOffers = config.getProperty( DME2Constants.DME2_SKIP_STALE_GRM_OFFERS );//, "false");

		/* Now iterating over available GRM endpoints to attempt request */
		Iterator<String> iter = endpointsToAttempt.iterator();
		while ( iter.hasNext() ) {
			if ( System.currentTimeMillis() - startTime >= this.overallTimeout ) {
				break; // we've used all our time up!
			}

			String currentGRMEndpoint = (String) iter.next().trim();
			logger.debug( null, "invokeGRMService", "Preparing to invoke GRM with URL: {}. -- Attempt number: [{}]",
					currentGRMEndpoint, iterationCount );

			/* Used for test and debugging purposes */
			if ( logAttemptedOffers ) {
				attemptedOffers.add( currentGRMEndpoint );
			}


			if ( skipStaleOffers.equalsIgnoreCase( "true" ) ) {
				if ( offerCache.isStale( currentGRMEndpoint ) ) {
					continue;
				}
			}

			HttpURLConnection conn = null;
			URL url = null;
			InputStream istream = null;

			try {
				logger.debug( null, "invokeGRMService", "GRMService connectTimeout \t" + connectTimeout+ "\t ;GRMService readTimeout \t" + readTimeout );

				logger.debug( null, "invokeGRMService", "GRM url {}", currentGRMEndpoint );
				url = new URL( currentGRMEndpoint );
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout( connectTimeout );
				conn.setReadTimeout( readTimeout );

				if(secCtx.isSSL() || url.getProtocol().equalsIgnoreCase("https")){
					((HttpsURLConnection) conn).setHostnameVerifier(AllowAllHostnameVerifier.INSTANCE);
				}

				sendRequest( conn, request );

				istream = conn.getInputStream();
				int respCode = conn.getResponseCode();

				if ( respCode != 200 ) {
					String faultResponse = parseResponse( istream );

					logger.debug( null, "invokeGRMService", "FaultResponse from GRMService \t{}", faultResponse );
					throw new Exception( " GRM Service Call Failed; StatusCode="
							+ respCode + "; GRMFaultResponse=" + faultResponse );
				}

				logger.debug(
						null,
						"invokeGRMService",
						"ElapsedTime from GRMService \t"
								+ ( System.currentTimeMillis() - start )
								+ " GRM Endpoint Used=" + currentGRMEndpoint
						);

				response = parseResponse( istream );
				logger.debug( null, "invokeGRMService", "Response: {}", response );
				return response;
			} catch ( SocketTimeoutException e ) {
				if ( config.getBoolean( DME2Constants.DME2_DEBUG ) ) {
					logger.debug( null, "invokeGRMService", "SocketTimeoutException from GRMService for offer {}", currentGRMEndpoint, e );
					logger.error( null, "invokeGRMService", "AFT-DME2-0914 {}", new ErrorContext().add( "GRM URL", currentGRMEndpoint ), e );
				}

				iter.remove();
				offerCache.setStale( currentGRMEndpoint );
				offerList.add( currentGRMEndpoint );
			} catch ( ConnectException e ) {
				if ( config.getBoolean( DME2Constants.DME2_DEBUG ) ) {
					logger.debug( null, "invokeGRMService", "ConnectException from GRMService for offer {}", currentGRMEndpoint, e );
					logger.warn( null, "invokeGRMService", "AFT-DME2-0913 {}", new ErrorContext().add( "GRM URL", currentGRMEndpoint ), e );
				}

				iter.remove();
				offerCache.setStale( currentGRMEndpoint );
				offerList.add( currentGRMEndpoint );
			} catch ( Throwable e ) {
				logger.debug( null, "invokeGRMService", "Error in invoking GRM", e );
				String faultResponse = null;

				if ( conn != null ) {
					InputStream err = conn.getErrorStream();

					try {
						faultResponse = parseResponse( err );
					} catch ( Exception ex ) {
						// ignore any exception in reading error stream
						logger.debug( null, "invokeGRMService", LogMessage.DEBUG_MESSAGE, "Exception", ex );
					}

					if ( faultResponse != null ) {

						int faultstringIndex = faultResponse.indexOf( "<faultstring>" );

						if ( faultstringIndex > -1 ) {
							XMLInputFactory inputFactory = XMLInputFactory.newInstance();
							inputFactory.setProperty( XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false );
							StringReader stringReader = new StringReader( faultResponse );
							XMLStreamReader reader = null;

							try {
								reader = inputFactory.createXMLStreamReader( stringReader );

								String element = null;
								while ( reader.hasNext() ) {
									try {
										reader.nextTag();
									} catch ( Exception xe ) {
										if ( element != null && element.equals( "code" ) ) {
											grmCode = reader.getText();
										} else if ( element != null && element.equals( "message" ) ) {
											grmMessage = reader.getText();
										} else if ( element != null && element.equals( "faultmessage" ) ) {
											faultMessage = reader.getText();
										}
										element = null;
										continue;
									}

									if ( reader.isStartElement() ) {
										if ( reader.getNamespaceURI() != null && reader.getNamespaceURI().equals( "http://scld.att.com/grm/v1" ) ) {
											if ( reader.getLocalName() != null && reader.getLocalName().equals( "code" ) ) {
												element = "code";
											} else if ( reader.getLocalName() != null && reader.getLocalName().equals( "message" ) ) {
												element = "message";
											}
										} else if ( reader.getNamespaceURI() != null && reader.getNamespaceURI().startsWith( "http://schemas.xmlsoap.org/soap/envelope" ) ) {
											if ( reader.getLocalName() != null && reader.getLocalName().equals( "faultstring" ) ) {
												element = "faultmessage";
											}
										} else if ( reader.getLocalName() != null && reader.getLocalName().equals( "faultstring" ) ) {
											element = "faultmessage";
										}
									} else if ( element != null && reader.isCharacters() ) {
										if ( element.equals( "code" ) ) {
											grmCode = reader.getText();
										} else if ( element.equals( "message" ) ) {
											grmMessage = reader.getText();
										} else if ( element.equals( "faultmessage" ) ) {
											faultMessage = reader.getText();
										}
										element = null;
									}
								}
							} catch ( Exception x ) {
								logger.debug( null, "invokeGRMService", LogMessage.GRM_IGNORABLE, x );
							}
						} // faultstringIndex > -1
					}
				}

				if ( config.getBoolean( DME2Constants.DME2_DEBUG ) ) {
					logger.debug( null, "invokeGRMService", "Throwable from GRMService for offer " + currentGRMEndpoint + "; GRMFaultResponse=" + faultResponse, e );
					logger.warn( null, "invokeGRMService", "AFT-DME2-0915", new ErrorContext().add( "GRM URL",currentGRMEndpoint ), e );
				}

				iter.remove();
				offerCache.setStale( currentGRMEndpoint );
				offerList.add( currentGRMEndpoint );
			} finally {
				// close the stream if open
				if ( istream != null ) {
					try {
						istream.close();
					} catch ( IOException e ) {
						logger.debug( null, "invokeGRMService", "Non-fatal IOException closing output stream to GRM", e );
					}
				}
			}
		}

		offerCache.removeStaleness( offerList );

		if ( grmCode != null && grmMessage != null ) {
			throw new DME2Exception( grmCode, grmMessage );
		} else if ( faultMessage != null ) {
			if ( faultMessage.contains( "GRMSVC" ) ) {
				String[] faultInfo = getGRMFaultInfo( faultMessage );

				if ( faultInfo != null ) {
					throw new DME2Exception( faultInfo[0], faultInfo[1] );
				}
			}

			// can't intepret the faultMessage, throw it "as-is"
			throw new DME2Exception("AFT-DME2-0916", new ErrorContext().add("GRM URLs", endpointsToAttempt.toString()).add("faultMessage", faultMessage));
		} else {
			throw new DME2Exception("AFT-DME2-0902", new ErrorContext().add("GRM URLs",  endpointsToAttempt.toString()));
		}
	}

	public String[] getGRMFaultInfo( String faultMessage ) {
		String[] faultInfo = null;

		if ( faultMessage.contains( "GRMSVC" ) ) {
			// Assume this message format: [GRMSVC-nnnn]
			// The dash we delim on is second in string
			int delim = faultMessage.indexOf( "]" );

			if ( delim > 0 ) {
				String grmCode = faultMessage.substring( 0, delim + 1 ).trim();
				grmCode = grmCode.replace( "[", "" ).replace( "]", "" );
				String grmMessage = faultMessage.substring( delim + 3 ).trim();

				faultInfo = new String[]{ grmCode, grmMessage };
				return faultInfo;
			}
		}
		return null;
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

	protected void sendRequest( HttpURLConnection conn, String request ) throws Exception {
		logger.debug( null, "sendRequest", "Request XML to GRM | " + request );

		conn.setDoInput( true );
		conn.setDoOutput( true );

		OutputStream out = conn.getOutputStream();
		out.write( request.getBytes() );
		out.flush();
		out.close();
	}

	public List<GRMEndpoint> getGRMEndpoints( String discoveryURL, String directURL ) throws DME2Exception {
		String url = null;

		if ( config.getProperty( "AFT_DME2_FORCE_GRM_LOOKUP" ) != null ) {
			grmURLs = null;
		}

		if ( directURL != null ) {
			url = directURL;
			logger.warn( null, "getGRMEndpointIterator", "AFT-DME2-0912", new ErrorContext().add( "overrideGRMUrls", url ) );
		} else {
			url = discoveryURL;
		}

		if ( url != null ) {
			grmURLs = url.split( "," );
		}

		RegistryBootstrap fetcher = RegistryBootstrapFactory.getRegistryBootstrapHandler( config );
		return fetcher.getGRMEndpoints( grmURLs );
	}

	private ServiceEndpoint buildServiceEndpoint( ServiceEndPoint input ) {
		ServiceEndpoint sep = new ServiceEndpoint();
		sep.setContextPath( input.getContextPath() );
		sep.setName( input.getName() );
		sep.setVersion( input.getVersion().getMajor() + "." + input.getVersion().getMinor() + "." + input.getVersion().getPatch() );
		sep.setPort( input.getListenPort() );
		sep.setProtocol( input.getProtocol() );
		sep.setLatitude( input.getLatitude() );
		sep.setLongitude( input.getLongitude() );
		sep.setHostAddress( input.getHostAddress() );
		sep.setContextPath( input.getContextPath() );
		sep.setRouteOffer( input.getRouteOffer() );
		sep.setAdditionalProperties( DME2URIUtils.convertNameValuePairToProperties( input.getProperties() ) );
		sep.setDmeVersion( input.getDME2Version() );
		sep.setClientSupportedVersions( input.getClientSupportedVersions() );
		sep.setDmeJDBCDatabaseName( input.getDME2JDBCDatabaseName() );
		sep.setDmeJDBCHealthCheckUser( input.getDME2JDBCHealthCheckUser() );
		sep.setDmeJDBCHealthCheckPassword( input.getDME2JDBCHealthCheckPassword() );
		sep.setExpirationTime( input.getExpirationTime() );

		return sep;
	}

	private ServiceEndPoint buildGRMServiceEndpoint( ServiceEndpoint input ) {
		ServiceEndPoint sep = new ServiceEndPoint();
		sep.setName( input.getName() );
		sep.setVersion( buildVersionDefinition( input.getVersion() ) );
		sep.setListenPort( input.getPort() );
		sep.setProtocol( input.getProtocol() );
		sep.setLatitude( input.getLatitude() );
		sep.setLongitude( input.getLongitude() );
		sep.setHostAddress( input.getHostAddress() );
		sep.setContextPath( input.getContextPath() );
		//In DME2 container route offer takes precedence over service route offer, keeping it same
		String containerRouteOffer = input.getContainerRouteOffer();
		if ( containerRouteOffer == null ) {
			containerRouteOffer = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ROUTEOFFER_KEY ) );
		}
		String serviceRouteOffer = input.getRouteOffer();
		sep.setRouteOffer( containerRouteOffer != null ? containerRouteOffer : serviceRouteOffer );
		sep.getProperties().addAll( DME2URIUtils.convertPropertiestoNameValuePairs( input.getAdditionalProperties() ) );
		sep.setDME2Version( input.getDmeVersion() );
		sep.setClientSupportedVersions( input.getClientSupportedVersions() );
		sep.setDME2JDBCDatabaseName( input.getDmeJDBCDatabaseName() );
		sep.setDME2JDBCHealthCheckUser( input.getDmeJDBCHealthCheckUser() );
		sep.setDME2JDBCHealthCheckPassword( input.getDmeJDBCHealthCheckPassword() );
		sep.setExpirationTime( input.getExpirationTime() );

		/*StatusInfo statusInfo = new StatusInfo();
		statusInfo.setStatus(Status.fromValue(input.getStatusInfo().getStatus()));
		sep.setStatusInfo(statusInfo);
		 */
		return sep;
	}

	private ContainerInstance buildContainerInstance( ServiceEndpoint input ) {
		ContainerInstance contInstance = new ContainerInstance();
		String containerName = input.getContainerName();
		String containerVersion = input.getContainerVersion();
		String containerRouteOffer = input.getContainerRouteOffer();
		String pid = input.getPid();
		String containerHost = input.getContainerHost() != null ? input.getContainerHost() : input.getHostAddress();

		if ( containerName == null ) {
			containerName = config.getProperty( DME2Constants.AFT_DME2_CONTAINER_NAME_KEY ) != null ?
					config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_NAME_KEY ) ) : null;
		}
		if ( containerVersion == null ) {
			containerVersion = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_VERSION_KEY ) );
		}
		if ( containerRouteOffer == null ) {
			containerRouteOffer = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_ROUTEOFFER_KEY ) );
		}
		if ( pid == null ) {
			pid = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_PID_KEY ) );
		}
		if ( containerHost == null ) {
			containerHost = config.getProperty( config.getProperty( DME2Constants.AFT_DME2_CONTAINER_HOST_KEY ) );
		}

		contInstance.setName( containerName );
		contInstance.setVersion( buildVersionDefinition( containerVersion ) );
		contInstance.setRouteOffer( containerRouteOffer );
		contInstance.setProcessId( pid );
		contInstance.setHostAddress( containerHost );

		return contInstance;
	}

	private LRM buildLRM( ServiceEndpoint request ) {
		LRM lrm = new LRM();
		String port = request.getPort();
		if ( port == null ) {
			port = config.getProperty( "lrmPort" );// "7200");
		}

		lrm.setHostAddress( request.getHostAddress() );
		lrm.setListenPort( port );

		return lrm;
	}

	private VersionDefinition buildVersionDefinition( String version ) {
		if ( version == null ) {
			return null;
		}
		DME2ValidationUtil.validateVersionFormat( config, version );

		int majorVersion = 0;
		int minorVersion = 0;

		String patchVersion = null;

		VersionDefinition vd = new VersionDefinition();

		if ( version != null ) {
			String[] tmpVersion = version.split( "\\." );

			if ( tmpVersion.length == DME2Constants.DME2_CONSTANT_THREE ) {
				majorVersion = Integer.parseInt( tmpVersion[0] );
				minorVersion = Integer.parseInt( tmpVersion[1] );
				patchVersion = tmpVersion[2];
			}

			if ( tmpVersion.length == DME2Constants.DME2_CONSTANT_TWO ) {
				majorVersion = Integer.parseInt( tmpVersion[0] );
				minorVersion = Integer.parseInt( tmpVersion[1] );
				patchVersion = null;
			}

			if ( tmpVersion.length == DME2Constants.DME2_CONSTANT_ONE ) {
				majorVersion = Integer.parseInt( tmpVersion[0] );
				minorVersion = -1;
				patchVersion = null;
			}
		}

		vd.setMajor( majorVersion );
		vd.setMinor( minorVersion );
		vd.setPatch( patchVersion );

		return vd;
	}

	private static enum AllowAllHostnameVerifier implements HostnameVerifier {
		INSTANCE;

		// An all-trusting host name verifier
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
	
	public IGRMEndPointDiscovery getGrmEndPointDiscovery () {
		return grmEndPointDiscovery;
	}
}