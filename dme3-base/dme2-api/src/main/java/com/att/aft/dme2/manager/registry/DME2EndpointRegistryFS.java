package com.att.aft.dme2.manager.registry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.collections.CollectionUtils;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2DistanceUtil;
import com.att.aft.dme2.manager.registry.util.DME2FileUtil;
import com.att.aft.dme2.manager.registry.util.DME2Protocol;
import com.att.aft.dme2.manager.registry.util.Version;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.DME2ValidationUtil;
import com.att.aft.dme2.util.ErrorContext;
import com.att.scld.grm.types.v1.ClientJVMInstance;

/**
 * DME2 Endpoint Registry for File Systems
 */
public class DME2EndpointRegistryFS extends DME2AbstractEndpointRegistry implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(DME2EndpointRegistryFS.class);
	protected static final long DEFAULT_CACHE_STALENESS = 60 * 1000 * 5;
	protected static final String CONFIG_KEY_FILE = "AFT_DME2_EP_REGISTRY_FS_DIR";
	protected static final String HTTP_PREFIX = "http://";
	protected static final String FORWARD_SLASH = "/";
	protected static final String HOST_PORT_SEPARATOR = ":";
	protected DME2EndpointCacheFS endpointCache;
	public DME2EndpointCacheFS getEndpointCache() {
		return endpointCache;
	}
	protected DME2RouteInfoCacheFS routeInfoCache;

	public DME2RouteInfoCacheFS getRouteInfoCache() {
		return routeInfoCache;
	}
	private Unmarshaller unmarshaller;
  private boolean isMarshalling = false;
	private static final String SCHEMA_NAMESPACE = "com.att.aft.dme2.types";

	/**
	 * The last update time ms map.
	 */
	private final Map<String, Long> lastUpdateTimeMsMap = Collections.synchronizedMap(new HashMap<String, Long>());
	private File dir;
	private final List<DME2Endpoint> localPublishedList = new CopyOnWriteArrayList<DME2Endpoint>();

	/**
	 * Basic constructor
	 * 
	 * @param config
	 *            DME2Configuration
	 * @param managerName
	 *            Manager Name
	 * @throws DME2Exception
	 */
	public DME2EndpointRegistryFS(DME2Configuration config, String managerName) throws DME2Exception {
		super(config, managerName);
		endpointCache = new DME2EndpointCacheFS(config, this, managerName, false);
		routeInfoCache = new DME2RouteInfoCacheFS(config, this, managerName);
		staleEndpointCache = new DME2StaleCache(config, DME2Endpoint.class, DME2EndpointRegistryType.FileSystem, this,
				managerName);
		staleRouteOfferCache = new DME2StaleCache(config, DME2RouteOffer.class, DME2EndpointRegistryType.FileSystem,
				this, managerName);
		if (unmarshaller == null) {
			unmarshaller = createUnmarshaller();
		}
	}

  private Unmarshaller createUnmarshaller() throws DME2Exception {
    JAXBContext jaxBContext;
    try {
      jaxBContext = JAXBContext.newInstance(SCHEMA_NAMESPACE);
    } catch (JAXBException e) {
      throw new DME2Exception("AFT-DME2-1550",
          new ErrorContext().add("extendedMessage", e.getMessage()).add("manager", managerName), e);
    }

    try {
      return jaxBContext.createUnmarshaller();
    } catch (JAXBException e) {
      throw new DME2Exception("AFT-DME2-1551",
          new ErrorContext().add("extendedMessage", e.getMessage()).add("manager", managerName), e);
    }
  }

  /**
	 * {@inheritDoc}
	 * 
	 * @param properties
	 *            Properties to use in initialization
	 * @throws DME2Exception
	 */
	@Override
	public void init(Properties properties) throws DME2Exception {
		super.init(properties);
		String fileName = getConfig().getProperty(CONFIG_KEY_FILE);
		if (fileName == null) {
			fileName = "dme2-fs-registry";
		}
		dir = new File(fileName);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		Runtime.getRuntime().addShutdownHook(new Thread(this));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param serviceName
	 *            Service name
	 * @param serviceVersion
	 *            Service version
	 * @param envContext
	 *            Environment context
	 * @param routeOffer
	 *            Route offer
	 * @return List of DME2 Endpoints
	 * @throws DME2Exception
	 */
	@Override
	public List<DME2Endpoint> findEndpoints(String serviceName, String serviceVersion, String envContext,
			String routeOffer) throws DME2Exception {
		// Build service string
		String service = DME2URIUtils.buildServiceURIString(serviceName, serviceVersion, envContext, routeOffer);

		DME2FileHandler fileHandler = new DME2FileHandler(dir, service, DEFAULT_CACHE_STALENESS, getClientLatitude(),
				getClientLongitude());

		Long sourceLastUpdateTimeMs = lastUpdateTimeMsMap.get(service);
		List<DME2Endpoint> endpoints = null;

		if (sourceLastUpdateTimeMs == null || sourceLastUpdateTimeMs >= fileHandler.getLastModified()) {
			endpoints = endpointCache.getEndpoints(service);
		}
		if (endpoints == null || endpoints.isEmpty()) {
			endpoints = fetchEndpointsFromSource(fileHandler);
			endpointCache.putEndpoints(service, endpoints);
			lastUpdateTimeMsMap.put(service, fileHandler.getLastModified());
		}
		return endpoints;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param serviceName
	 *            Service name
	 * @param serviceVersion
	 *            Service version
	 * @param envContext
	 *            Environment context
	 * @return DME2 Route Info
	 * @throws DME2Exception
	 */
	@Override
	public DME2RouteInfo getRouteInfo(String serviceName, String serviceVersion, String envContext)
			throws DME2Exception {
		String path = DME2URIUtils.buildServiceURIString(serviceName, serviceVersion, envContext);
		File file = new File(dir, path + "/routeInfo.xml");

		DME2RouteInfo routeInfo = routeInfoCache.get(path);

		if (routeInfo == null || routeInfo.lastUpdated() < file.lastModified()) {
			routeInfo = fetchRouteInfoFromSource(path);
			routeInfoCache.put(path, routeInfo);
		}

		return routeInfo;
	}

	private List<DME2Endpoint> fetchEndpointsFromSource(DME2FileHandler fileHandler) throws DME2Exception {
		return fileHandler.readEndpoints();
	}

	protected List<DME2Endpoint> fetchEndpointsFromSource(String service) throws DME2Exception {
		return fetchEndpointsFromSource(
				new DME2FileHandler(dir, service, DEFAULT_CACHE_STALENESS, getClientLatitude(), getClientLongitude()));
	}

	protected DME2RouteInfo fetchRouteInfoFromSource(String path) throws DME2Exception {
		JAXBElement<RouteInfo> element = null;

		// SCLD-4879 - Hierarchical version lookup

		List<File> matchingFiles = DME2FileUtil.hierarchicalFileLookup(dir, path + "/routeInfo.xml");
		Map<Version, DME2RouteInfo> routeInfoMap = new HashMap<Version, DME2RouteInfo>();
		for (File file : matchingFiles) {
			element = readRouteInfo(file);
			routeInfoMap.put(new Version(element.getValue().getServiceVersion()),
					new DME2RouteInfo(element.getValue(), getConfig()));
		}
		ArrayList<Version> sortedVersionList = new ArrayList<Version>(routeInfoMap.keySet());
		Collections.sort(sortedVersionList);
		if (sortedVersionList.isEmpty()) {
			throw new DME2Exception(DME2Constants.EXP_REG_ROUTE_INFO_FILE_NOT_FOUND,
					new ErrorContext().add("path", path).add("path", path));

		} else {
			return routeInfoMap.get(sortedVersionList.get(0));
		}
	}

  // Marshalling is NOT thread safe!!  If no one is using the already-created unmarshaller, go ahead and use it, but
  // make sure everyone knows that it is in use.  If it is already in use, create a new one.
	protected synchronized JAXBElement<RouteInfo> readRouteInfo(File file) throws DME2Exception {
    boolean isMarshallingWasSet = false;
		try {
      Unmarshaller currentUnmarshaller;
      if ( !isMarshalling ) {
        isMarshalling = true;
        isMarshallingWasSet = true;
        currentUnmarshaller = unmarshaller;
      } else {
        currentUnmarshaller = createUnmarshaller();
      }
      StreamSource ss = new StreamSource( file );
      return (JAXBElement<RouteInfo>) currentUnmarshaller.unmarshal( ss );
		} catch (JAXBException e) {
			throw new DME2Exception("AFT-DME2-1552", new ErrorContext().add("extendedMessage", e.getMessage()), e);
		} finally {
      if ( isMarshallingWasSet ) isMarshalling = false;
    }
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param service
	 *            Service name
	 * @param path
	 *            Service path
	 * @param host
	 *            Service host
	 * @param port
	 *            Service port
	 * @param latitude
	 *            Service location latitude
	 * @param longitude
	 *            Service location longitude
	 * @param protocol
	 *            Service access protocol
	 * @throws DME2Exception
	 */
	@Override
	public void publish(String service, String path, String host, int port, double latitude, double longitude,
			String protocol) throws DME2Exception {
		publish(service, path, host, port, latitude, longitude, protocol, null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param service
	 *            Service name
	 * @param path
	 *            Service path
	 * @param host
	 *            Service host
	 * @param port
	 *            Service port
	 * @param latitude
	 *            Service location latitude
	 * @param longitude
	 *            Service location longitude
	 * @param protocol
	 *            Service access protocol
	 * @param updateLease
	 *            Whether to renew the service lease
	 * @throws DME2Exception
	 */
	@Override
	public void publish(String service, String path, String host, int port, double latitude, double longitude,
			String protocol, boolean updateLease) throws DME2Exception {
		publish(service, path, host, port, latitude, longitude, protocol, null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param service
	 *            Service name
	 * @param path
	 *            Service path
	 * @param host
	 *            Service host
	 * @param port
	 *            Service port
	 * @param protocol
	 *            Service access protocol
	 * @param props
	 *            Service Properties
	 * @throws DME2Exception
	 */
	@Override
	public void publish(String service, String path, String host, int port, String protocol, Properties props)
			throws DME2Exception {
		publish(service, path, host, port, getConfig().getDouble("AFT_LATITUDE"),
				getConfig().getDouble("AFT_LONGITUDE"), protocol, props);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param service
	 *            Service name
	 * @param path
	 *            Service path
	 * @param host
	 *            Service host
	 * @param port
	 *            Service port
	 * @param protocol
	 *            Service access protocol
	 * @throws DME2Exception
	 */
	@Override
	public void publish(String service, String path, String host, int port, String protocol) throws DME2Exception {
		publish(service, path, host, port, getConfig().getDouble("AFT_LATITUDE"),
				getConfig().getDouble("AFT_LONGITUDE"), protocol, null);
	}

	@Override
	public void publish(String serviceURI, String contextPath, String hostAddress, int port, double latitude,
			double longitude, String protocol, Properties props, boolean updateLease) throws DME2Exception {
		publish(serviceURI, contextPath, hostAddress, port, latitude, longitude, protocol, props);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param service
	 *            Service name
	 * @param path
	 *            Service path
	 * @param host
	 *            Service host
	 * @param port
	 *            Service port
	 * @param protocol
	 *            Service access protocol
	 * @param updateLease
	 *            Whether to renew the service lease
	 * @throws DME2Exception
	 */
	@Override
	public void publish(String service, String path, String host, int port, String protocol, boolean updateLease)
			throws DME2Exception {
		publish(service, path, host, port, getConfig().getLong("AFT_LATITUDE"), getConfig().getLong("AFT_LONGITUDE"),
				protocol, null);
	}

	private void publish(String servicePath, String contextPath, String host, int port, double latitude,
			double longitude, String protocol, Properties inProps) throws DME2Exception {
		String urlStr = null;
		double distance = DME2DistanceUtil.calculateDistanceBetween(getClientLatitude(), getClientLongitude(), latitude,
				longitude);
		DmeUniformResource uniformResource = buildUniformResource(protocol, servicePath, host, port);
		int queryIndexStart;
		if ((queryIndexStart = servicePath.indexOf('?')) != -1) {
			servicePath = servicePath.substring(0, queryIndexStart);
		}

		String serviceContext = "service=" + uniformResource.getService() + "/version=" + uniformResource.getVersion()
				+ "/envContext=" + uniformResource.getEnvContext() + "/routeOffer=" + uniformResource.getRouteOffer();

		DME2FileHandler fileHandler = new DME2FileHandler(dir, serviceContext, DEFAULT_CACHE_STALENESS,
				getClientLatitude(), getClientLongitude());
		logger.debug(null, "publish",  LogMessage.PUBLISH_FILE, serviceContext + ".txt");
		Properties props = fileHandler.readProperties();

		String propsKey = host + "," + port;
		props.setProperty(propsKey,
				"latitude=" + latitude + ";longitude=" + longitude + ";lease=" + System.currentTimeMillis()
						+ ";protocol=" + protocol + ";contextPath=" + (contextPath == null ? servicePath : contextPath)
						+ ";routeOffer=" + uniformResource.getRouteOffer());

		DME2Endpoint ep = null;
		if (DME2Protocol.DME2JDBC.equals(protocol)) {
			try {
				DME2ValidationUtil.validateJDBCEndpointRequiredFields(inProps, servicePath);
			} catch (DME2Exception e) {
				throw new DME2Exception(e.getErrorCode(), e.getErrorMessage());
			}

			// ep = new DME2JDBCEndpoint(this.getManager());
			ep = new DME2JDBCEndpoint(distance);
			if (inProps != null) {
				StringBuffer buff = new StringBuffer();

				if (inProps.containsKey(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME)) {
					((DME2JDBCEndpoint) ep)
							.setDatabaseName((String) props.get(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME));
					buff.append("DME2JDBCDatabaseName=" + inProps.get(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME) + ";");
				}

				if (inProps.containsKey(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER)) {
					((DME2JDBCEndpoint) ep)
							.setHealthCheckUser((String) props.get(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER));
					buff.append("DME2JDBCHealthCheckUser=" + inProps.get(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER)
							+ ";");
				}

				if (inProps.containsKey(DME2Constants.KEY_DME2_JDBC_DATABASE_NAME)) {
					((DME2JDBCEndpoint) ep).setHealthCheckPassword(
							(String) props.get(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_PASSWORD));
					buff.append("DME2JDBCHealthCheckPassword="
							+ inProps.get(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_PASSWORD) + ";");
				}

				if (inProps.containsKey(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER)) {
					((DME2JDBCEndpoint) ep)
							.setHealthCheckDriver((String) inProps.get(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER));
					buff.append("DME2JDBCHealthCheckDriver="
							+ inProps.get(DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER) + ";");
				}

				for (String propName : inProps.stringPropertyNames()) {
					buff.append(propName + "=" + inProps.get(propName));
				}
				String endpointProps = props.getProperty(propsKey);
				props.setProperty(propsKey, endpointProps + ";" + buff.toString());
			}
		} else {
			ep = new DME2Endpoint(serviceContext, distance);
		}

		ep.setHost(host);
		ep.setPort(port);
		ep.setLatitude(latitude);
		ep.setLongitude(longitude);
		ep.setLease(System.currentTimeMillis());
		ep.setProtocol(protocol);

		if (contextPath == null) {
			ep.setContextPath(servicePath);
		} else {
			ep.setContextPath(contextPath);
		}

		localPublishedList.add(ep);

		fileHandler.storeProperties(props, true);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param serviceName
	 *            Service name
	 * @param host
	 *            Service host
	 * @param port
	 *            Service port
	 * @throws DME2Exception
	 */
	@Override
	public void unpublish(String serviceName, String host, int port) throws DME2Exception {

		DME2FileHandler fileHandler = new DME2FileHandler(dir, serviceName, DEFAULT_CACHE_STALENESS,
				getClientLatitude(), getClientLongitude());
	    logger.debug(null, "unpublish",  LogMessage.UNPUBLISHING_FILE, serviceName + ".txt");

		Properties props = fileHandler.readProperties();

		props.remove(host + "," + port);
		for (Iterator<DME2Endpoint> it = localPublishedList.iterator(); it.hasNext();) {
			DME2Endpoint ep = it.next();
			if (ep.getHost() != null && ep.getHost().equalsIgnoreCase(host) && ep.getPort() == port) {
				localPublishedList.remove(ep);
			}
		}

		fileHandler.storeProperties(props, false);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param endpoint
	 *            the endpoint to lease
	 * @throws DME2Exception
	 */
	@Override
	public void lease(DME2Endpoint endpoint) throws DME2Exception {
		publish(endpoint.getPath(), endpoint.getContextPath(), endpoint.getHost(), endpoint.getPort(),
				endpoint.getLatitude(), endpoint.getLongitude(), endpoint.getProtocol(),
				endpoint.getEndpointProperties());
		synchronized (localPublishedList) {
			localPublishedList.remove(endpoint);
		}
	}

	private void refreshPublishedDME2Endpoints() {
		for (DME2Endpoint ep : localPublishedList) {
			try {
				lease(ep);
			} catch (DME2Exception e) {
				logger.debug(null, null, "refreshPublishedDME2Endpoints", LogMessage.DEBUG_MESSAGE, "DME2Exception", e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refresh() {
		refreshPublishedDME2Endpoints();
		endpointCache.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		for (DME2Endpoint ep : this.localPublishedList) {
			try {
				this.unpublish(ep);
			} catch (Exception e) {
				logger.warn(null, "run", LogMessage.ERROR_UNPUBLISHING, ep);
			}
		}
	}

	private void unpublish(DME2Endpoint ep) throws DME2Exception {
		unpublish(ep.getPath(), ep.getHost(), ep.getPort());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		shutdown();
	}
	
	@Override
	public DME2Endpoint[] find(String serviceKey, String version, String env,
			String routeOffer) throws DME2Exception {
		List<DME2Endpoint> endpoints = findEndpoints(serviceKey, version, env, routeOffer);
		DME2Endpoint[] endpointArray = null;
		if(CollectionUtils.isNotEmpty(endpoints)) {
			endpointArray = endpoints.toArray(new DME2Endpoint[endpoints.size()]);
		}
		return endpointArray;
	 }

  @Override
	public void registerJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception {
		// Do Nothing
	}
	@Override
	public void updateJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception {
		// Do Nothing
	}
	@Override
	public void deregisterJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception {
		// Do Nothing
	}
	@Override
	public List<ClientJVMInstance> findRegisteredJVM(String envContext, Boolean activeOnly, String hostAddress, String mechID, String processID) throws DME2Exception {
		// Do Nothing
		return null;
	}
}
