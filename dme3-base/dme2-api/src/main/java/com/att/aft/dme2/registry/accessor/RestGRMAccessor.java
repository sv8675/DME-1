package com.att.aft.dme2.registry.accessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.registry.dto.GRMEndpoint;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2CustomXGCalConverter;
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
import com.att.scld.grm.v1.DeleteServiceEndPointRequest;
import com.att.scld.grm.v1.FindClientJVMInstanceRequest;
import com.att.scld.grm.v1.FindClientJVMInstanceResponse;
import com.att.scld.grm.v1.FindRunningServiceEndPointRequest;
import com.att.scld.grm.v1.FindRunningServiceEndPointResponse;
import com.att.scld.grm.v1.GetRouteInfoRequest;
import com.att.scld.grm.v1.GetRouteInfoResponse;
import com.att.scld.grm.v1.RegisterClientJVMInstanceRequest;
import com.att.scld.grm.v1.UpdateServiceEndPointRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.core.util.Base64;

public class RestGRMAccessor extends AbstractGRMAccessor implements BaseAccessor {
	private static final Logger logger = LoggerFactory.getLogger(RestGRMAccessor.class.getName());
	private static String CREDENTIALS;
	private static List<String> attemptedOffers = null;
	private ObjectMapper mapper;

	public RestGRMAccessor(DME2Configuration config, SecurityContext ctx, IGRMEndPointDiscovery discovery) {
		super(config, ctx, discovery);
		CREDENTIALS = "Basic " + new String(Base64.encode(config.getProperty(DME2Constants.DME2_GRM_USER) + ":"
				+ config.getProperty(DME2Constants.DME2_GRM_PASS)));
		mapper = new ObjectMapper();
	}

	/**
	 * Adds given ServiceEndpoint in GRM
	 * 
	 * @param input
	 *            ServiceEndpoint
	 */
	public void addServiceEndPoint(ServiceEndpoint input) throws DME2Exception {
		// build the sep from the endpoint
		ServiceEndPoint sep = buildGRMServiceEndpoint(input);

		// build the lrm.
		LRM lrm = buildLRM(input);

		// build container instance
		ContainerInstance ci = buildContainerInstance(input);

		// now we build the request
		AddServiceEndPointRequest req = new AddServiceEndPointRequest();
		req.setEnv(input.getEnv());
		req.setLrmRef(lrm);
		req.setServiceEndPoint(sep);
		req.setCheckNcreateParents(true);
		if (ci.getName() != null && !ci.getName().startsWith("dummy")) {
			req.setContainerInstanceRef(ci);
		}

		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(req);

			logger.debug(null, "addServiceEndPoint", "AddServiceEndpoint {}", jsonRequest);

			invokeGRMService(AddServiceEndPointRequest.class.getSimpleName(), jsonRequest);
		} catch (Throwable e) {
			logger.debug(null, "addServiceEndPoint", "Error in invoking GRM addServiceEndpoint", e);
			throw this.processException(e, "addServiceEndPoint", req.getEnv() + "/" + req.getServiceEndPoint().getName()
					+ ":" + req.getServiceEndPoint().getVersion());
		}
	}

	private String invokeGRMService(String requestName, String request) throws DME2Exception {
		long start = System.currentTimeMillis();

		ArrayList<String> offerList = new ArrayList<String>();

		OfferCache offerCache = OfferCache.getInstance();
		String response = null;

		List<String> endpointsToAttempt = grmEndPointDiscovery.getGRMEndpoints();

		/* Used for test and debugging purposes */
		if (System.getProperty("AFT_DME2_DEBUG_GRM_EPS") != null && !request.contains("Metrics")
				&& !request.contains("delete")) {
			attemptedOffers = new ArrayList<String>();
		}

		// holders for "last" error
		String grmCode = null;
		String grmMessage = null;
		String faultMessage = null;

		long startTime = System.currentTimeMillis();
		int iterationCount = 0;
		String skipStaleOffers = config.getProperty(DME2Constants.DME2_SKIP_STALE_GRM_OFFERS);

		/* Now iterating over available GRM endpoints to attempt request */
		Iterator<String> iter = endpointsToAttempt.iterator();
		while (iter.hasNext()) {
			if (System.currentTimeMillis() - startTime >= this.overallTimeout) {
				break; // we've used all our time up!
			}

			String currentGRMEndpoint = (String) iter.next();
			String finalURL = getFinalRequestURL(currentGRMEndpoint,
					config.getProperty(requestName).substring(0, config.getProperty(requestName).indexOf("@")));
			String method = config.getProperty(requestName).substring(config.getProperty(requestName).indexOf("@") + 1,
					config.getProperty(requestName).length());
			logger.debug(null, "invokeGRMService",
					String.format("Preparing to invoke GRM with URL: %s. -- Attempt number: [%s]", currentGRMEndpoint,
							iterationCount));

			/* Used for test and debugging purposes */
			// TODO: Move the getProp outside the loop and make it boolean
			if (System.getProperty("AFT_DME2_DEBUG_GRM_EPS") != null && !request.contains("Metrics")
					&& !request.contains("delete")) {
				attemptedOffers.add(currentGRMEndpoint);
			}

			if (skipStaleOffers.equalsIgnoreCase("true")) {
				if (offerCache.isStale(currentGRMEndpoint)) {
					continue;
				}
			}

			HttpURLConnection conn = null;
			URL url = null;
			InputStream istream = null;

			try {
				logger.debug(null, "invokeGRMService",
						"GRMService connectTimeout \t{}\t ;GRMService readTimeout \t{}; GRM Rest URL \t{}",
						connectTimeout, readTimeout, finalURL);

				url = new URL(finalURL);
				conn = (HttpURLConnection)url.openConnection();
				conn.setConnectTimeout(connectTimeout);
				conn.setReadTimeout(readTimeout);

				if (url.getProtocol().equalsIgnoreCase("https")) {
					((HttpsURLConnection)conn).setHostnameVerifier(AllowAllHostnameVerifier.INSTANCE);
				}

				sendRequest(conn, request, method);
				
				int respCode = conn.getResponseCode();

				if (respCode != 200) {
					String faultResponse = null;
					
					if (conn.getInputStream() != null) {
						faultResponse = parseResponse(conn.getInputStream());
					} else if (conn.getErrorStream() != null) {
						faultResponse = parseResponse(conn.getErrorStream());
					} else {
						faultResponse = conn.getResponseMessage();
					}
					
					logger.debug(null, "invokeGRMService", "FaultResponse from GRMService \t{}", faultResponse);

					throw new Exception(
							" GRM Service Call Failed; StatusCode=" + respCode + "; GRMFaultResponse=" + faultResponse);
				}

				logger.debug(null, "invokeGRMService", "ElapsedTime from GRMService \t"
						+ (System.currentTimeMillis() - start) + " GRM Endpoint Used=" + currentGRMEndpoint);

				response = parseResponse(conn.getInputStream());
				return response;
			} catch (SocketTimeoutException e) {
				if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
					logger.debug(null, null, "invokeGRMService", "SocketTimeoutException from GRMService for offer {}",
							currentGRMEndpoint, e);
					logger.error(null, null, "invokeGRMService", "AFT-DME2-0914",
							new ErrorContext().add("GRM URL", currentGRMEndpoint), e);
				}

				iter.remove();
				offerCache.setStale(currentGRMEndpoint);
				offerList.add(currentGRMEndpoint);
			} catch (ConnectException e) {
				if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
					logger.debug(null, null, "invokeGRMService", "ConnectException from GRMService for offer {}",
							currentGRMEndpoint, e);
					logger.warn(null, null, "invokeGRMService", "AFT-DME2-0913",
							new ErrorContext().add("GRM URL", currentGRMEndpoint), e);
				}

				iter.remove();
				offerCache.setStale(currentGRMEndpoint);
				offerList.add(currentGRMEndpoint);
			} catch (Throwable e) {
				logger.debug(null, "invokeGRMService", "Error in invoking GRM", e);
				String faultResponse = null;

				if (conn != null) {
					InputStream err = conn.getErrorStream();

					try {
						faultResponse = parseResponse(err);
					} catch (Exception ex) {
						// ignore any exception in reading error stream
					}

					if (faultResponse != null) {
						try {
							JSONObject jsonFaultResponse = new JSONObject(faultResponse);
							JSONObject jsonFaultString = jsonFaultResponse.getJSONObject("faultString");
							grmCode = (String) jsonFaultString.get("grmcode");
							grmMessage = (String) jsonFaultString.get("message");
							faultMessage = (String) jsonFaultString.get("faultMessage");
						} catch (JSONException e1) {
							try {
								JSONObject jsonFaultResponse = new JSONObject(faultResponse);
								grmCode = (String) jsonFaultResponse.get("code");
								grmMessage = (String) jsonFaultResponse.get("message");
								faultMessage = (String) jsonFaultResponse.get("message");
							} catch (JSONException e2) {
								logger.debug(null, "invokeGRMService", "Error in invoking GRM: JSONException", e2);
							}
						}

					}
				}

				if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
					logger.debug(null, "invokeGRMService",
							"Throwable from GRMService for offer {}; GRMFaultResponse={}", currentGRMEndpoint,
							faultResponse, e);
					logger.warn(null, null, "invokeGRMService", "AFT-DME2-0915",
							new ErrorContext().add("GRM URL", currentGRMEndpoint), e);
				}

				iter.remove();
				offerCache.setStale(currentGRMEndpoint);
				offerList.add(currentGRMEndpoint);
			} finally {
				// close the stream if open
				if (istream != null) {
					try {
						istream.close();
					} catch (IOException e) {
						logger.debug(null, "invokeGRMService", "Non-fatal IOException closing output stream to GRM", e);

					}
				}
			}
		}

		offerCache.removeStaleness(offerList);

		if (grmCode != null && grmMessage != null) {
			throw new DME2Exception(grmCode, grmMessage);
		} else if (faultMessage != null) {
			if (faultMessage.contains("GRMSVC")) {
				String[] faultInfo = getGRMFaultInfo(faultMessage);

				if (faultInfo != null) {
					throw new DME2Exception(faultInfo[0], faultInfo[1]);
				}
			}

			// can't intepret the faultMessage, throw it "as-is"
			throw new DME2Exception("AFT-DME2-0916", new ErrorContext()
					.add("GRM URLs", Arrays.asList(grmURLs).toString()).add("faultMessage", faultMessage));
		} else {
			if (grmURLs != null) {
				throw new DME2Exception("AFT-DME2-0902",
						new ErrorContext().add("GRM URLs", Arrays.asList(grmURLs).toString()));
			} else {
				throw new DME2Exception("AFT-DME2-0902",
						new ErrorContext().add("GRM URLs", "No GRM URL Found"));
			}
		}
	}

	private String getFinalRequestURL(String currentGRMEndpoint, String contextPath) {
		if (currentGRMEndpoint.endsWith("/")) {
			return currentGRMEndpoint.concat(contextPath);
		} else {
			if (contextPath.startsWith("/")) {
				return currentGRMEndpoint.concat(contextPath);
			} else {
				return currentGRMEndpoint.concat("/").concat(contextPath);
			}
		}
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

	protected void sendRequest(HttpURLConnection conn, String request, String method) throws Exception {
		logger.debug(null, "sendRequest", "Request JSON to GRM | {}", request);

		conn.setRequestMethod(method);
		if (config.getBoolean(DME2Constants.DME2_GRM_AUTH)) {
			conn.setRequestProperty("Authorization", CREDENTIALS);
		}
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoInput(true);
		conn.setDoOutput(true);

		OutputStream out = conn.getOutputStream();
		out.write(request.getBytes());
		out.flush();
		out.close();
	}

	/**
	 * Updates given ServiceEndpoint in GRM
	 * 
	 * @param input
	 *            ServiceEndpoint
	 */
	public void updateServiceEndPoint(ServiceEndpoint input) throws DME2Exception {
		// Build a sep from the input.
		ServiceEndPoint sep = buildGRMServiceEndpoint(input);

		LRM lrm = buildLRM(input);
		UpdateServiceEndPointRequest req = new UpdateServiceEndPointRequest();
		req.setUpdateLease(true);
		req.setEnv(input.getEnv());
		req.setLrmRef(lrm);

		ContainerInstance ci = buildContainerInstance(input);

		if (ci.getName() != null && !ci.getName().startsWith("dummy")) {
			req.setContainerInstanceRef(ci);
		}

		req.setServiceEndPoint(sep);

		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(req);

			logger.debug(null, "updateServiceEndPoint", "UpdateServiceEndpoint {}", jsonRequest);

			invokeGRMService(UpdateServiceEndPointRequest.class.getSimpleName(), jsonRequest);
		} catch (Throwable e) {
			throw this.processException(e, "UpdateServiceEndPoint", req.getEnv() + "/"
					+ req.getServiceEndPoint().getName() + ":" + req.getServiceEndPoint().getVersion());
		}
	}

	/**
	 * Deletes given ServiceEndpoint from GRM
	 * 
	 * @param input
	 *            ServiceEndpoint
	 */
	public void deleteServiceEndPoint(ServiceEndpoint input) throws DME2Exception {
		ServiceEndPoint sep = buildGRMServiceEndpoint(input);

		DeleteServiceEndPointRequest req = new DeleteServiceEndPointRequest();
		req.setEnv(input.getEnv());
		req.getServiceEndPoint().add(sep);

		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(req);

			logger.debug(null, "deleteServiceEndPoint", "DeleteServiceEndPoint {}", jsonRequest);

			invokeGRMService(DeleteServiceEndPointRequest.class.getSimpleName(), jsonRequest);
		} catch (Throwable e) {
			throw this.processException(e, "DeleteServiceEndPoint",
					req.getEnv() + "/" + req.getServiceEndPoint().toString());
		}

	}

	private ServiceEndPoint buildGRMServiceEndpoint(ServiceEndpoint input) {

		ServiceEndPoint sep = new ServiceEndPoint();
		sep.setName(input.getName());
		sep.setVersion(buildVersionDefinition(input.getVersion()));
		sep.setListenPort(input.getPort());
		sep.setProtocol(input.getProtocol());
		sep.setLatitude(input.getLatitude());
		sep.setLongitude(input.getLongitude());
		sep.setHostAddress(input.getHostAddress());
		sep.setContextPath(input.getContextPath());
		sep.setRouteOffer(input.getRouteOffer());
		sep.getProperties().addAll(DME2URIUtils.convertPropertiestoNameValuePairs(input.getAdditionalProperties()));
		sep.setDME2Version(input.getDmeVersion());
		sep.setClientSupportedVersions(input.getClientSupportedVersions());
		sep.setDME2JDBCDatabaseName(input.getDmeJDBCDatabaseName());
		sep.setDME2JDBCHealthCheckUser(input.getDmeJDBCHealthCheckUser());
		sep.setDME2JDBCHealthCheckPassword(input.getDmeJDBCHealthCheckPassword());
		sep.setExpirationTime(input.getRegistrationTime());

		/*
		 * StatusInfo statusInfo = new StatusInfo();
		 * statusInfo.setStatus(Status.fromValue(input.getStatusInfo().getStatus
		 * ())); sep.setStatusInfo(statusInfo);
		 */
		return sep;
	}

	private ContainerInstance buildContainerInstance(ServiceEndpoint input) {
		ContainerInstance contInstance = new ContainerInstance();
		String containerName = input.getContainerName();
		String containerVersion = input.getContainerVersion();
		String containerRouteOffer = input.getContainerRouteOffer();
		String pid = input.getPid();
		String containerHost = input.getContainerHost();

		if (containerName == null) {
			containerName = config.getProperty(DME2Constants.AFT_DME2_CONTAINER_NAME_KEY) != null
					? config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_NAME_KEY)) : null;
		}

		if (containerVersion == null) {
			containerVersion = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_VERSION_KEY));
		}

		if (containerRouteOffer == null) {
			containerRouteOffer = config
					.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_ROUTEOFFER_KEY));
		}

		if (pid == null) {
			pid = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_PID_KEY));
		}

		if (containerHost == null) {
			containerHost = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_HOST_KEY));
			if (containerHost == null) {
				containerHost = input.getHostAddress();
			}
		}

		contInstance.setName(containerName);
		contInstance.setVersion(buildVersionDefinition(containerVersion));
		contInstance.setRouteOffer(containerRouteOffer);
		contInstance.setProcessId(pid);
		contInstance.setHostAddress(containerHost);

		return contInstance;
	}

	private LRM buildLRM(ServiceEndpoint request) {
		LRM lrm = new LRM();
		String port = request.getPort();
		if (port == null) {
			port = config.getProperty("lrmPort");// "7200");
		}

		lrm.setHostAddress(request.getHostAddress());
		lrm.setListenPort(port);

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

	public String getDiscoveryURL() throws DME2Exception {
		if (discoveryURL == null) {
			discoveryURL = "aftdsc:///?service=SOACloudEndpointRegistryGRMLWPService&version=1.0&bindingType=http&envContext={ENV}";
			discoveryURL = discoveryURL.replace("{ENV}", getEnvLetter());

		}
		return discoveryURL;
	}

	public String getEnvLetter() {
		if (envLetter != null) {
			return envLetter;
		}

		String platStr = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY));

		if (platStr == null) {
			platStr = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_PLATFORM_KEY));
		}

		String aftEnv = config.getProperty("AFT_ENVIRONMENT");

		if (aftEnv != null && platStr == null) {
			if (aftEnv.equalsIgnoreCase("AFTUAT")) {
				envLetter = "T";
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
				envLetter = null;
			}
		}
		return envLetter;
	}

	public List<String> getGRMEndpoints(List<GRMEndpoint> endPoints) {

		// Holds the active non-stale endpoints that will be used to invoke GRM
		ArrayList<String> activeOfferList = new ArrayList<String>();

		// Holds endpoints that have been marked stale due to some error
		// condition
		ArrayList<String> staleOfferList = new ArrayList<String>();

		OfferCache offerCache = OfferCache.getInstance();

		for (GRMEndpoint endPoint : endPoints) {
			String endpoint = endPoint.getAddress();

			/*
			 * Get each endpoint from the EndpointReference Iterator. Check if
			 * each endpoint from the Iterator is stale. If it is not, then add
			 * it to the active offer list. If it is, then add it to the stale
			 * offer list.
			 */
			if (!offerCache.isStale(endpoint)) {
				activeOfferList.add(endpoint);
			} else {
				staleOfferList.add(endpoint);
			}
		} // End iteration

		/*
		 * If the stale offer list has endpoints, add them to the END of the
		 * active offer list, so they will be tried last
		 */
		if (!staleOfferList.isEmpty()) {
			logger.debug(null, "getGRMEndpoints",
					"Adding stale GRM offers to the end of the active offer list. Stale Offers="
							+ staleOfferList.toString());

			activeOfferList.addAll(activeOfferList.size(), staleOfferList);
		}

		logger.debug(null, "getGRMEndpoints",
				"////// Resolved the following GRM Endpoints from Discovery: " + activeOfferList.toString());

		return activeOfferList;
	}

	/*public List<GRMEndpoint> getGRMEndpoints(String discoveryURL, String directURL) throws DME2Exception {
		String url = null;

		if (config.getProperty("AFT_DME2_FORCE_GRM_LOOKUP") != null) {
			grmURLs = null;
		}

		if (directURL != null) {
			url = directURL;
			logger.warn(null, "getGRMEndpointIterator", "AFT-DME2-0912",
					new ErrorContext().add("overrideGRMUrls", url));
		} else {
			url = discoveryURL;
		}

		if (url != null) {
			grmURLs = url.split(",");
		}

		RegistryBootstrap fetcher = RegistryBootstrapFactory.getRegistryBootstrapHandler(config);
		return fetcher.getGRMEndpoints(grmURLs);
	}*/

	public String[] getGRMFaultInfo(String faultMessage) {
		String[] faultInfo = null;

		if (faultMessage.contains("GRMSVC")) {
			// Assume this message format: [GRMSVC-nnnn]
			// The dash we delim on is second in string
			int delim = faultMessage.indexOf("]");

			if (delim > 0) {
				String grmCode = faultMessage.substring(0, delim + 1).trim();
				grmCode = grmCode.replace("[", "").replace("]", "");
				String grmMessage = faultMessage.substring(delim + 3).trim();

				faultInfo = new String[] { grmCode, grmMessage };
				return faultInfo;
			}
		}
		return null;
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

		logger.error(null, null, method, LogMessage.GRM_RETHROW, code, e);
		return new DME2Exception(code, new ErrorContext().add("extendedData", data).add("exceptionMessage", e.getMessage()));
	}

	/**
	 * Fetches list of running endpoint for the given service given
	 * ServiceEndpoint from GRM
	 * 
	 * @param input
	 *            ServiceEndpoint
	 * @return List<ServiceEndpoint> - List of running end points
	 */
	public List<ServiceEndpoint> findRunningServiceEndPoint(ServiceEndpoint input) throws DME2Exception {
		ServiceEndPoint sep = buildGRMServiceEndpoint(input);

		FindRunningServiceEndPointRequest req = new FindRunningServiceEndPointRequest();
		req.setEnv(input.getEnv());
		req.setServiceEndPoint(sep);

		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(req);

			logger.debug(null, "findRunningServiceEndPoint", "FindRunningServiceEndPoint {}", jsonRequest);

			String reply = invokeGRMService(FindRunningServiceEndPointRequest.class.getSimpleName(), jsonRequest);

			logger.debug(null, "findRunningServiceEndPoint", "FindRunningServiceEndPoint Response {}", reply);

			if (reply != null) {
				// convert to response type
				FindRunningServiceEndPointResponse resp = new FindRunningServiceEndPointResponse();
				Gson gsonForFindRunning = new GsonBuilder()
						.registerTypeAdapter(XMLGregorianCalendar.class, new DME2CustomXGCalConverter.Serializer())
						.registerTypeAdapter(XMLGregorianCalendar.class, new DME2CustomXGCalConverter.Deserializer())
						.create();
				resp = gsonForFindRunning.fromJson(reply.replace("ServiceEndPointList", "serviceEndPointList"),
						FindRunningServiceEndPointResponse.class);

				return convertToServiceEndpointList(resp.getServiceEndPointList());
			} else {
				return null;
			}
		} catch (Throwable e) {
			throw this.processException(e, "findRunningServiceEndPoint",
					input.getEnv() + "/" + input.getName() + ":" + input.getDmeVersion());
		}
	}

	private List<ServiceEndpoint> convertToServiceEndpointList(List<ServiceEndPoint> serviceEndPointList) {

		List<ServiceEndpoint> serviceEndPoints = new ArrayList<ServiceEndpoint>();
		for (ServiceEndPoint sep : serviceEndPointList) {
			serviceEndPoints.add(buildServiceEndpoint(sep));
		}
		return serviceEndPoints;
	}

	public String getRouteInfo(ServiceEndpoint input) throws DME2Exception {
		ServiceVersionDefinition svd = new ServiceVersionDefinition();
		svd.setName(input.getName());
		svd.setVersion(buildVersionDefinition(input.getVersion()));

		GetRouteInfoRequest req = new GetRouteInfoRequest();
		req.setEnv(input.getEnv());
		req.setServiceVersionDefinition(svd);

		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(req);

			logger.debug(null, "getRouteInfo", "GetRouteInfo {}", jsonRequest);

			String reply = invokeGRMService(GetRouteInfoRequest.class.getSimpleName(), jsonRequest);

			if (reply != null) {
				// convert to response type
				GetRouteInfoResponse resp = mapper.readValue(reply, GetRouteInfoResponse.class);

				return resp.getRouteInfoXml();
			} else {
				return null;
			}
		} catch (Throwable e) {
			throw this.processException(e, "getRouteInfo", input.getEnv() + "/" + input.getName());
		}
	}

	@Override
	public void registerClientJVMInstance(RegisterClientJVMInstanceRequest req) throws DME2Exception {
		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(req);

			logger.debug(null, "registerClientJVMInstance", "RegisterClientJVMInstance {}", jsonRequest);

			invokeGRMService(RegisterClientJVMInstanceRequest.class.getSimpleName(), jsonRequest);
		} catch (Throwable e) {
			throw this.processException(e, "registerClientJVMInstance", req.getEnv() + "/" + req.toString());
		}
	}

	@Override
	public List<ClientJVMInstance> findClientJVMInstance(FindClientJVMInstanceRequest req) throws DME2Exception {
		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(req);

			logger.debug(null, "findClientJVMInstance", "FindClientJVMInstance {}", jsonRequest);

			String reply = invokeGRMService(FindClientJVMInstanceRequest.class.getSimpleName(), jsonRequest);

			if (reply != null) {
				// convert to response type
				FindClientJVMInstanceResponse resp = mapper.readValue(reply, FindClientJVMInstanceResponse.class);

				return resp.getClientJVMInstanceList();
			} else {
				return null;
			}
		} catch (Throwable e) {
			throw this.processException(e, "findClientJVMInstance", req.getEnv() + "/" + req.toString());
		}
	}

	private ServiceEndpoint buildServiceEndpoint(ServiceEndPoint input) {

		ServiceEndpoint sep = new ServiceEndpoint();
		sep.setContextPath(input.getContextPath());
		sep.setName(input.getName());
		sep.setVersion(input.getVersion().getMajor() + "." + input.getVersion().getMinor() + "."
				+ input.getVersion().getPatch());
		sep.setPort(input.getListenPort());
		sep.setProtocol(input.getProtocol());
		sep.setLatitude(input.getLatitude());
		sep.setLongitude(input.getLongitude());
		sep.setHostAddress(input.getHostAddress());
		sep.setContextPath(input.getContextPath());
		sep.setRouteOffer(input.getRouteOffer());
		sep.setAdditionalProperties(DME2URIUtils.convertNameValuePairToProperties(input.getProperties()));
		sep.setDmeVersion(input.getDME2Version());
		sep.setClientSupportedVersions(input.getClientSupportedVersions());
		sep.setDmeJDBCDatabaseName(input.getDME2JDBCDatabaseName());
		sep.setDmeJDBCHealthCheckUser(input.getDME2JDBCHealthCheckUser());
		sep.setDmeJDBCHealthCheckPassword(input.getDME2JDBCHealthCheckPassword());
		sep.setExpirationTime(input.getExpirationTime());
		sep.setRegistrationTime(input.getRegistrationTime());

		return sep;
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
