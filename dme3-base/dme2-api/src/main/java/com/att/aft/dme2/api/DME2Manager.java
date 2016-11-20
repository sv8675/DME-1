/*
 * Copyright 2016 AT&T Intellectual Properties, Inc.
 */
package com.att.aft.dme2.api;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.att.aft.dme2.api.http.DME2QueuedThreadPool;
import com.att.aft.dme2.api.util.DME2PortFileManager;
import com.att.aft.dme2.api.util.DME2ThreadPoolConfig;
import com.att.aft.dme2.api.util.DME2ThrottleConfig;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.event.DME2CancelRequestEventProcessor;
import com.att.aft.dme2.event.DME2Event;
import com.att.aft.dme2.event.DME2EventDispatcher;
import com.att.aft.dme2.event.DME2EventManager;
import com.att.aft.dme2.event.DME2FailoverEventProcessor;
import com.att.aft.dme2.event.DME2FaultEventProcessor;
import com.att.aft.dme2.event.DME2InitEventProcessor;
import com.att.aft.dme2.event.DME2ReplyEventProcessor;
import com.att.aft.dme2.event.DME2RequestEventProcessor;
import com.att.aft.dme2.event.DME2ServiceStatManager;
import com.att.aft.dme2.event.DME2ServiceStats;
import com.att.aft.dme2.event.EventType;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.DME2LoggingConfig;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistry;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryFS;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryType;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.manager.registry.DME2StaleCacheAdapter;
import com.att.aft.dme2.manager.registry.DME2StaleCacheAdapterFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.DmeUniformResource.DmeUrlType;
import com.att.aft.dme2.server.api.websocket.DME2ServerWebSocketHandler;
import com.att.aft.dme2.server.api.websocket.DME2WSClientFactory;
import com.att.aft.dme2.server.mbean.DME2MXBean;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2ExceptionHandler;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.ErrorContext;

/**
 * 
 * This class encapsulates the core of the DME2 implementation, providing
 * initialize routines, access to endpoint discovery, and access to dynamic
 * registration and de-registration of local service implementations.
 * 
 * 
 * 
 * <p/>
 * <p>
 * <h3>Required Configuration</h3>
 * <table>
 * <tr>
 * <td>Property</td>
 * <td>Required?</td>
 * <td>Default</td>
 * <td>Description</td>
 * <td>Example</td>
 * </tr>
 * <tr>
 * <td>AFT_LATITUDE</td>
 * <td>Y</td>
 * <td>N/A</td>
 * <td>Earth latitude of the local process.</td>
 * <td>-33.646</td>
 * </tr>
 * <tr>
 * <td>AFT_LONGITUDE</td>
 * <td>Y</td>
 * <td>N/A</td>
 * <td>Earth longitude of the local process.</td>
 * <td>89.573</td>
 * </tr>
 * </table>
 * </p>
 *
 * @author rg9975
 */

public class DME2Manager implements DME2MXBean, java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private Properties serviceProperties;

	private static final Logger logger = LoggerFactory.getLogger(DME2Manager.class.getName());

	@SuppressWarnings("unused")
	private static ResourceBundle errorTable = DME2LoggingConfig.getInstance()
	.initializeDME2ErrorTable(DME2Constants.DME2_ERROR_TABLE_BASE_NAME);

	private DME2Configuration config;

	private boolean bindServer = false;

	private boolean enableWebSocket = false;

	/**
	 * 
	 * The global instance of the manager.
	 */

	private volatile static DME2Manager instance = null;

	/**
	 * Host where the server, if set, is running.
	 */
	private String hostname = null;

	/**
	 * 
	 * This processes latitude.
	 */

	private double latitude = 0;

	/**
	 * 
	 * This processes longitude.
	 */

	private double longitude = 0;

	/**
	 * 
	 * Port where the server, if set, is running.
	 */

	private Integer port = null;

	/**
	 * 
	 * The local reference to the endpoint registry.
	 */
	private transient DME2EndpointRegistry registry = null;

	/**
	 * The local reference to the stale cache adapter
	 */
	private transient DME2StaleCacheAdapter staleCacheAdapter = null;

	/**
	 * 
	 * The local Http Server used for hosting services locally.
	 */

	private transient DME2Server server = null;

	/**
	 * 
	 * The name of this manager
	 */

	private String name;

	private DME2ServiceStatManager statManager;

	/**
	 * 
	 * Statistics about services running in this manager
	 */

	private DME2ServiceStats stats;

	/**
	 * The HttpClient all exchanges use for client-side requests
	 */
	private transient HttpClient client = null;

	/**
	 * 
	 * The default period that stale endpoints will remain stale before retry
	 */

	private long endpointStalenessPeriodMs = 900000L;

	/**
	 * The default period that stale endpoints will remain stale before retry
	 */
	private final long routeOfferStalenessPeriodMs = 15;

	/**
	 * 
	 * Holds a list of offers which we have sent notices via logging of failure
	 * for already
	 */

	private final Set<String> globalNoticeCache = Collections.synchronizedSet(new TreeSet<String>());

	/**
	 * 
	 * Threadpool mechanism to handle async doTry call while exchange is invoked
	 */

	private transient ThreadPoolExecutor retryThreadpool;

	/**
	 * 
	 * username
	 */
	private String userName = null;

	/**
	 * 
	 * password
	 */

	private String password = null;

	/**
	 * 
	 * 
	 * flag indicating the current running state of this manager
	 */

	private boolean running = false;

	/**
	 * 
	 * 
	 * the charset to use for requests and responses for this manager, unless
	 * overridden by specific API calls or configuration
	 */

	private String charset = null;

	/**
	 * The boolean variable to determine whether the DME2Exchange should ignore
	 * failover if HTTP call times out on a read *
	 */
	private boolean ignoreFailoverOnExpire = false;

	private String processID = null;
	private final static String MANAGER = "manager";
	private final static String SERVICE = "service";
	private final static String PACKAGE = "com.att.aft.dme2.Version";

	private DME2WSClientFactory dme2WsClientFactory = null;
	/**
	 * 
	 * Threadpool mechanism to handle ws connection failover
	 */

	private transient ThreadPoolExecutor wsRetryThreadpool = null;
	private byte[] lockObject = new byte[0];

	/**
	 * register EventManager
	 */
	private DME2EventManager eventManager = null;
	private String realm;

	public String getRealm() {
		return realm;
	}

	// Metrics Constants
	private static boolean disableMetrics = false;

	private static boolean DISABLE_METRICS_FILTER = false;
	private static boolean DISABLE_THROTTLE_FILTER = false;
	private static TimeZone timezone = Calendar.getInstance().getTimeZone();
	private static Locale locale = Locale.getDefault();

	static {
		try {
			DME2Manager.getDefaultInstance();
		} catch (DME2Exception e) {
		}
	/*	try {
			new DME2Manager("Metrics-Configuration", new Properties());
		} catch ( Exception e ) {

		}*/
	}

	public static final DME2Manager getDefaultInstance() throws DME2Exception {
    DME2Manager result = instance;
		if (result == null) {
			synchronized (DME2Manager.class) {
				result = instance;
				if (result == null) {
					instance = result = new DME2Manager("DefaultDME2Manager", new Properties());
				}
			}
		}
		return result;
	}

	public DME2Manager(String name, Properties props) throws DME2Exception {
		setParams(name, new DME2Configuration(name, props));
		this.serviceProperties = props;

	}

	// TODO: Remove this
	public synchronized static DME2Manager initDefaultManager(Properties props) throws DME2Exception {
		if (instance != null) {
			throw new DME2Exception("AFT-DME2-0001", new ErrorContext().add(MANAGER, instance.getName()));

		}
		instance = new DME2Manager("DefaultDME2Manager", props);
		return instance;

	}

	/**
	 * Static accessor to The DME2Manager instance.
	 *
	 * 
	 * @return single instance of DME2Manager
	 * @throws com.att.aft.dme2.api.DME2Exception
	 *             the e http exception
	 */

	@SuppressWarnings("unused")
	public DME2Manager() {
		/*
		 * Jackson JSON processor requires a default constructor in order to
		 * serialize objects
		 */

		try {
			setParams("DefaultDME2Manager", new DME2Configuration("DefaultDME2Manager"));
		} catch (Exception e) {
		}
	}

	private void setParams(String name, DME2Configuration config) throws DME2Exception {
		try {
			this.config = config;
			validateBootProperties();
			this.statManager = DME2ServiceStatManager.getInstance(config);
			if (config != null && !config.getBoolean(DME2Constants.AFT_DME2_COLLECT_SERVICE_STATS)) {
				statManager.setDisableMetrics(true);
			}
			this.retryThreadpool = DME2ThreadPoolConfig.getInstance(this).createExchangeRetryThreadPool();
			printVersion();
			logger.debug(null, "ctor(String,DME2Configuration)", "DME2Manager [{}] initializing...", name);
			this.endpointStalenessPeriodMs = config
					.getLong(DME2Constants.Cache.AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS);
			this.name = name;
			this.server = initServer();
			this.registry = initRegistry();
			this.staleCacheAdapter = DME2StaleCacheAdapterFactory.getStaleCacheAdapter(registry);
			this.charset = config.getProperty(DME2Constants.AFT_DME2_CHARSET);
			this.ignoreFailoverOnExpire = config.getBoolean(DME2Constants.AFT_DME2_IGNORE_FAILOVER_ONEXPIRE);

			this.userName = config.getProperty(DME2Constants.DME2_CRED_AUTH_USERNAME);
			this.password = config.getProperty(DME2Constants.DME2_CRED_AUTH_PASSWORD);

			DME2PortFileManager fmgr = DME2PortFileManager.getInstance(config);
			String persistedPorts = fmgr.getPort(DME2Utils.getRunningInstanceName(config),
					server.getServerProperties().isSslEnable() == true ? true : false);

			if (persistedPorts != null) {
				server.setPersistedPorts(persistedPorts);
			}

			if (server != null && bindServer) {
				if (!server.isRunning()) {
					server.start();

				}
				hostname = server.getServerProperties().getHostname();
				port = server.getServerProperties().getPort();

			}

			String latitudeStr = config.getProperty(DME2Constants.AFT_LATITUDE);
			if (latitudeStr == null) {
				throw new DME2Exception("AFT-DME2-0003", new ErrorContext().add(MANAGER, this.name));

			}

			String longitudeStr = config.getProperty(DME2Constants.AFT_LONGITUDE);
			if (longitudeStr == null) {
				throw new DME2Exception("AFT-DME2-0004", new ErrorContext().add(MANAGER, this.name));
			}

			latitude = Double.parseDouble(latitudeStr);
			longitude = Double.parseDouble(longitudeStr);

			DME2MXBeanMaster.getInstance().addManager(this);

			if (registry instanceof DME2EndpointRegistryFS) {
				DME2MXBeanMaster.getInstance().addRegistryCache(this, (DME2EndpointRegistryFS) registry);
			}

			if (registry instanceof DME2EndpointRegistryGRM) {
				DME2MXBeanMaster.getInstance().addRegistryCache(this, (DME2EndpointRegistryGRM) registry);
			}

			initEventManager();

			logger.debug(null, "ctor(String,DME2Configuration)", "DME2Manager [{}] initialized successfully", name);

			// DME2LoggingPropertyChangeHandler.scheduleLoggingPropertyMonitor();
			this.processID = ManagementFactory.getRuntimeMXBean().getName();
			DME2MXBeanMaster.getInstance().addThrottleConfig(DME2ThrottleConfig.getInstance(config,
					config.getProperty("AFT_DME2_THROTTLE_FILTER_CONFIG_FILE", "dme2-throttle-config.properties")),
					this.getName());

		} catch (Exception e) {
			throw DME2ExceptionHandler.handleException(e, "");
		}
	}

	/*
	 * Validate if all the required boot properties are present, for DME2
	 * Manager to initialize
	 */
	private void validateBootProperties() throws DME2Exception {
		if (null == config.getProperty("AFT_ENVIRONMENT")) {
			throw new DME2Exception("AFT-DME2-0901", new ErrorContext().add(MANAGER, this.name));
		}
		if (null == config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_PLATFORM_KEY, "platform"))) {
			if (null == config.getProperty(DME2Constants.AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY)) {
				throw new DME2Exception("AFT-DME2-0901", new ErrorContext().add(MANAGER, this.name));
			}
		}
		if (null == config.getProperty(DME2Constants.AFT_LATITUDE)) {
			throw new DME2Exception("AFT-DME2-0003", new ErrorContext().add(MANAGER, this.name));
		}
		if (null == config.getProperty(DME2Constants.AFT_LONGITUDE)) {
			throw new DME2Exception("AFT-DME2-0004", new ErrorContext().add(MANAGER, this.name));
		}
	}

	public DME2Manager(String name, DME2Configuration config) throws DME2Exception {
		setParams(name, config);
	}

	private void initEventManager() {

		eventManager = DME2EventManager.getInstance(config);
		registerEventHandlers();
	}

	private void registerEventHandlers() {
		eventManager.registerEventProcessor(EventType.REQUEST_EVENT.getName(), new DME2RequestEventProcessor(config));
		eventManager.registerEventProcessor(EventType.REPLY_EVENT.getName(), new DME2ReplyEventProcessor(config));
		eventManager.registerEventProcessor(EventType.FAULT_EVENT.getName(), new DME2FaultEventProcessor(config));
		eventManager.registerEventProcessor(EventType.TIMEOUT_EVENT.getName(), new DME2FaultEventProcessor(config));
		eventManager.registerEventProcessor(EventType.CANCEL_REQUEST_EVENT.getName(),
				new DME2CancelRequestEventProcessor(config));
		eventManager.registerEventProcessor(EventType.FAILOVER_EVENT.getName(), new DME2FailoverEventProcessor(config));
		eventManager.registerEventProcessor(EventType.INIT_EVENT.getName(), new DME2InitEventProcessor(config));
	}

	public DME2Manager(String name, DME2Configuration config, Properties props) throws DME2Exception {
		setParams(name, config);
		this.serviceProperties = props;

	}

	public Properties getServiceProperties() {
		return serviceProperties;

	}

	public String getProcessID() {
		return processID.contains("@") ? processID.substring(0, processID.indexOf("@")) : processID;
	}

	// TODO Check with Maitrayee and Arindam to enable this code.
	/*
	 * public double getDistanceTo(double serviceLatitudeDegrees, double
	 * serviceLongitudeDegrees) { return
	 * proximityAide.getDistanceTo(serviceLatitudeDegrees,
	 * serviceLongitudeDegrees); }
	 */

	/**
	 * Bind service listener.
	 *
	 * @param service
	 *            the service
	 * @param listenerServlet
	 *            the listener servlet
	 * @throws com.att.aft.dme2.api.DME2Exception
	 *             the e http exception
	 */

	public void bindServiceListener(String service, Servlet listenerServlet) throws DME2Exception {
		bindServiceListener(service, listenerServlet, null, null, null);
	}

	/**
	 * Bind service listener.
	 *
	 * @param service
	 *            the service
	 * @param listenerServlet
	 *            the listener servlet
	 * @param securityRealm
	 *            the JAAS application name to use for security. This must be
	 *            defined in the JAAS configuration.
	 * @param allowedRoles
	 *            the list of allowed roles for the service. The user must be
	 *            associated with the role in the JAAS configuration
	 * @param loginMethod
	 *            the login method. Must be BASIC or CLIENT-CERT. Other types
	 *            are not supported at this time.
	 * @throws com.att.aft.dme2.api.DME2Exception
	 *             the e http exception
	 */

	public void bindServiceListener(String service, Servlet listenerServlet, String securityRealm,
			String[] allowedRoles, String loginMethod) throws DME2Exception {

		try {
			if (statManager != null) {
				this.stats = statManager.getServiceStats(service);
			}
			this.bindServer = true;
			this.realm = securityRealm;

			if (server == null) {
				synchronized (instance) {
					if (server == null) {
						initDefaultServer(service);
					}
				}
			}

			if (server != null) {
				if (!server.isRunning()) {
					try {
						server.start();
					} catch (Exception ex) {
					}
					this.running = true;
				}
			}

			DME2ServiceHolder serviceHolder = new DME2ServiceHolder();
			serviceHolder.setServiceURI(service);
			serviceHolder.setServlet(listenerServlet);
			serviceHolder.setSecurityRealm(securityRealm);
			serviceHolder.setAllowedRoles(allowedRoles);
			serviceHolder.setLoginMethod(loginMethod);
			serviceHolder.setManager(this);
			serviceHolder.setServiceProperties(serviceProperties);
			server.addService(serviceHolder);
		} catch (Exception e) {
			throw DME2ExceptionHandler.handleException(e, service);
		}
	}

	public void bindServiceListener(String service, DME2ServerWebSocketHandler webSocketHandler) throws DME2Exception {
		bindServiceListener(service, webSocketHandler, null, null, null);
	}

	public void bindServiceListener(String service, DME2ServerWebSocketHandler webSocketHandler, String securityRealm,
			String[] allowedRoles, String loginMethod) throws DME2Exception {

		try {
			this.bindServer = true;
			this.enableWebSocket = true;

			if (server == null) {
				synchronized (instance) {
					if (server == null) {
						initDefaultServer(service, true);// websocket is true
					}
				}
			}

			if (server != null) {
				if (!server.isRunning()) {
					server.setWebSocket(true);
					server.start();
					this.running = true;
				}
			}

			if (webSocketHandler != null) {
				webSocketHandler.setDme2ServiceName(service);
			}

			DME2ServiceHolder serviceHolder = new DME2ServiceHolder();
			serviceHolder.setServiceURI(service);
			serviceHolder.setDme2WebSocketHandler(webSocketHandler);
			serviceHolder.setSecurityRealm(securityRealm);
			serviceHolder.setAllowedRoles(allowedRoles);
			serviceHolder.setLoginMethod(loginMethod);
			serviceHolder.setServiceProperties(serviceProperties);
			server.addService(serviceHolder);
		} catch (Exception e) {
			throw DME2ExceptionHandler.handleException(e, service);
		}
	}

	public void bindService(DME2ServiceHolder serviceHolder) throws DME2Exception {

		server.addService(serviceHolder);
		serviceHolder.setServiceProperties(this.serviceProperties);

		if (server == null) {
			synchronized (instance) {
				if (server == null) {
					if (serviceHolder.getDme2WebSocketHandler() != null) {
						initDefaultServer(serviceHolder.getServiceURI(), true);// set
						// websocket
						// true
						// on
						// the
						// server
						this.enableWebSocket = true;
					} else {
						initDefaultServer(serviceHolder.getServiceURI());
						this.bindServer = true;
					}
				}
			}
		}

		if (server != null) {
			if (!server.isRunning()) {
				if (serviceHolder.getDme2WebSocketHandler() != null) {
					server.setWebSocket(true);
				}
				server.start();
				this.running = true;
			}
		}
		DME2ServerWebSocketHandler webSocketHandler = serviceHolder.getDme2WebSocketHandler();
		if (webSocketHandler != null) {
			webSocketHandler.setDme2ServiceName(serviceHolder.getServiceURI());
		}
	}

	/**
	 * Bind service listener
	 *
	 * @param holder
	 *            service holder
	 * @throws DME2Exception
	 *             the http exception
	 */
	public void addService(DME2ServiceHolder holder) throws DME2Exception {

		if (server == null) {
			synchronized (instance) {
				if (server == null) {
					initDefaultServer(DME2Utils.getRunningInstanceName(config));
				}
			}
		}
		holder.setManager(this);

		server.addService(holder);
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * Remove a service holder
	 *
	 * @param holder
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */

	public void removeService(DME2ServiceHolder holder) throws DME2Exception {

		server.removeService(holder);
	}

	/**
	 * Return all service holders
	 *
	 * @return
	 */
	public Collection<DME2ServiceHolder> getServices() {
		return server.getServices();

	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * Get a service holder given the service URI
	 *
	 * @param serviceURI
	 *            service URI
	 * @return Service Holder
	 */
	public DME2ServiceHolder getService(String serviceURI) {
		return server.getService(serviceURI);
	}

	/**
	 * Start the manager. This will start the underlying network server, start
	 * all services registered with this manager, and any other associated web
	 * artifacts (filters, servlet contexts, etc).
	 *
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */

	public void start() throws DME2Exception {
		if (!running) {
			if (registry == null) {
				registry = this.initRegistry();
			}
			server.start();
		}
	}

	/**
	 * Stop the manager. This will unregister all services and stop the network
	 * server. This will not remove the services from this manager - you must
	 * remove using the "removeService" API call - so a subsequent start() would
	 * re-publish the same services with the new network server port.
	 *
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */

	public void stop() throws DME2Exception {
		Exception t = null;
		try {
			if (this.dme2WsClientFactory != null) {
				this.dme2WsClientFactory.stop();
			}
		} catch (Exception e) {
			logger.error(null, "stop", LogMessage.SERVER_STOP_FAIL, e);
			t = e;
		}

		if (running) {
			try {
				server.stop();
			} catch (Exception e) {
				logger.error(null, "stop", LogMessage.SERVER_STOP_FAIL, e);
				t = e;
			}
			try {
				registry.shutdown();
			} catch (Exception e) {
				logger.error(null, "stop", LogMessage.SERVER_STOP_FAIL, e);
				t = e;
			}
			DME2EventDispatcher.setStopThreads(true);
			try {
				Thread.sleep(1000); // TODO: this should be configured to the
				// poll length in EventDispatcher! (?)
			} catch (InterruptedException e) {
				t = e;
			}
			if (t != null) {
				ErrorContext ec = new ErrorContext();
				ec.add("ErrorMessage", t.getMessage());
				throw new DME2Exception("AFT-DME2-3008", ec);
			}
		}
	}

	public DME2Client newClient(URI uri, long connTimeoutMs) throws DME2Exception {

		return new DME2Client(this, uri, connTimeoutMs);
	}

	public DME2Client newClient(URI uri, long connTimeoutMs, String charset) throws DME2Exception {
		return new DME2Client(this, uri, connTimeoutMs, charset);
	}

	/**
	 * Given search criteria for a service, return a list of endpoints, grouped
	 * into bands. Each band represents an ordering of endpoints based on
	 * geo-distance from the location of this running process, where endpoints
	 * within each band are randomized on each call.
	 *
	 * @param uniformResource
	 *            uniform resource
	 * @param endpoints
	 *            array of endpoints
	 * @return the active route offers
	 * @throws DME2Exception
	 *             http exception
	 */
	public List<DME2RouteOffer> getActiveOffers(DmeUniformResource uniformResource, DME2Endpoint[] endpoints)
			throws DME2Exception {
		String serviceName = uniformResource.getService();
		if (endpoints != null) {
			final DME2RouteOffer offer = new DME2RouteOffer(uniformResource.getService(), uniformResource.getVersion(),
					uniformResource.getEnvContext(), "DIRECT", endpoints, this);
			return Arrays.<DME2RouteOffer>asList(offer);

		}
		switch (uniformResource.getUrlType()) {
		case STANDARD:
			if (uniformResource.getPartner() == null) {
				return null;
			}
			// else pass through
		case SEARCHABLE:
			boolean searchKeyWithWildcard = false;

			if (serviceName != null && serviceName.contains("/")) {
				searchKeyWithWildcard = true;
			}
			String serviceKey = uniformResource.getRouteInfoServiceSearchKey();
			return getActiveOffers(serviceKey, uniformResource.getVersion(), uniformResource.getEnvContext(),
					uniformResource.getPartner(), uniformResource.getDataContext(),
					uniformResource.getStickySelectorKey(), searchKeyWithWildcard);
		default:
			return null;

		}
	}

	public List<DME2RouteOffer> getActiveOffers(String service, String version, String envContext, String partnerName,
			String dataContext, String stickySelectorKey) throws DME2Exception {
		return getActiveOffers(service, version, envContext, partnerName, dataContext, stickySelectorKey, false);
	}

	public List<DME2RouteOffer> getActiveOffers(String service, String version, String envContext, String partnerName,
			String dataContext, String stickySelectorKey, boolean searchKeyWithWildcard) throws DME2Exception {
		return getActiveOffers(service, version, envContext, partnerName, dataContext, stickySelectorKey,
				searchKeyWithWildcard, null);
	}

	public List<DME2RouteOffer> getActiveOffers(String service, String version, String envContext, String partnerName,
			String dataContext, String stickySelectorKey, boolean searchKeyWithWildcard, String preferredRouteOffer)
					throws DME2Exception {

		final DME2RouteInfo routeInfo = getRouteInfo(service, version, envContext);
		if (routeInfo == null) {
			throw new DME2Exception("AFT-DME2-0005 {}",
					new ErrorContext().add(SERVICE, service).add("version", version));
		}
		boolean checkDataPartitionRange = Boolean
				.parseBoolean(config.getProperty(DME2Constants.CHECK_DATA_PARTITION_RANGE_FIRST, "false"));
		final List<DME2RouteOffer> offers = routeInfo.getRouteOffers(envContext, partnerName, dataContext,
				stickySelectorKey, checkDataPartitionRange, preferredRouteOffer);
		if (searchKeyWithWildcard && offers != null) {
			for (DME2RouteOffer offer : offers) {
				offer.setSearchWithWildcard(true);
			}
		}
		return offers;
	}

	/**
	 * Get the local reference to the endpoint registry.
	 *
	 * @return the endpoint registry
	 */

	public DME2EndpointRegistry getEndpointRegistry() {
		return registry;
	}

	public DME2Endpoint[] getEndpoints(DmeUniformResource uniformResource) throws DME2Exception {
		if (uniformResource.getUrlType() == DmeUrlType.DIRECT) {
			return getDirectEndpoints(uniformResource);
		}
		final String serviceKey = uniformResource.getRegistryFindEndpointSearchKey();
		if (serviceKey == null) {
			return null;
		}
		return findEndpoints(serviceKey, uniformResource.getVersion(), uniformResource.getEnvContext(),

				uniformResource.getRouteOffer(), uniformResource.isUsingVersionRanges());
	}

	public DME2Endpoint[] findEndpoints(String serviceKey, String version, String env, String routeOffer,
			boolean useVersionRange) throws DME2Exception {
		final List<DME2Endpoint> unfiltered = registry.findEndpoints(serviceKey, version, env, routeOffer);
		if (useVersionRange) {
			return unfiltered.toArray(new DME2Endpoint[unfiltered.size()]);
		}

		final List<DME2Endpoint> filtered = new ArrayList<DME2Endpoint>(unfiltered.size());

		for (DME2Endpoint ep : unfiltered) {
			if (versionMatches(ep.getServiceVersion(), version)) {
				filtered.add(ep);
			}
		}

		DME2Endpoint[] var = new DME2Endpoint[filtered.size()];
		int i = 0;
		for (DME2Endpoint dme2End : filtered) {
			var[i] = dme2End;
			i++;
		}
		return var;
	}

	/**
	 * match in the case where service is 1.2.3 and client requests version 1.2
	 * NOT SUPPORTING: service version 1.2, client request 1.2.3
	 */
	private boolean versionMatches(String endpointVersion, String requestVersion) {
		return endpointVersion.equals(requestVersion) || endpointVersion.startsWith(requestVersion + ".");
	}

	private DME2Endpoint[] getDirectEndpoints(DmeUniformResource uniformResource) {
		final DME2Endpoint ep = new DME2Endpoint(0); // "Distance" is assumed to
		// be zero
		ep.setHost(uniformResource.getOriginalURL().getHost());
		ep.setPort(uniformResource.getOriginalURL().getPort());
		ep.setPath(uniformResource.getSubContext() != null ? uniformResource.getSubContext()
				: uniformResource.getOriginalURL().getPath());
		ep.setServiceName(uniformResource.getOriginalURL().getPath());
		ep.setServiceVersion(uniformResource.getVersion());
		ep.setEnvContext(uniformResource.getEnvContext());
		ep.setRouteOffer(uniformResource.getRouteOffer());
		ep.setContextPath(uniformResource.getSubContext());
		ep.setProtocol(uniformResource.getOriginalURL().getProtocol());
		ep.setSimpleName(uniformResource.getService());
		ep.setDmeUniformResource(uniformResource);
		return new DME2Endpoint[] { ep };

	}

	/**
	 * Gets the hostname.
	 *
	 * 
	 * 
	 * 
	 * 
	 * @return the hostname
	 * 
	 */

	public String getHostname() {
		return hostname;
	}

	/**
	 * 
	 * 
	 * Gets the port.
	 *
	 * @return the port
	 */

	public Integer getPort() {
		return port;

	}

	/**
	 * Gets the route info.
	 *
	 * @param service
	 *            the service
	 * @param version
	 *            the version
	 * @param envContext
	 *            the env context
	 * @return the route info
	 * @throws com.att.aft.dme2.api.DME2Exception
	 *             the e http exception
	 */

	public DME2RouteInfo getRouteInfo(String service, String version, String envContext) throws DME2Exception {
		return registry.getRouteInfo(service, version, envContext);
	}

	/**
	 * Get the local server instance.
	 *
	 * @return the server
	 */
	public DME2Server getServer() {
		return server;
	}

	private void initDefaultServer(String service) throws DME2Exception {
		initDefaultServer(service, false);
	}

	/**
	 * Initialize a default server on the next available port.
	 *
	 * @throws com.att.aft.dme2.api.DME2Exception
	 *             the e http exception
	 */
	private void initDefaultServer(String service, boolean isWebSocket) throws DME2Exception {
		server = new DME2Server(config);
		server.setManager(this);
		server.setWebSocket(isWebSocket);
		String configHost = config.getProperty(DME2Constants.AFT_DME2_HOSTNAME, null);
		String configPort = config.getProperty(DME2Constants.AFT_DME2_PORT, null);
		boolean sslEnabled = config.getBoolean(DME2Constants.AFT_DME2_SSL_ENABLE, false);
		String configPortRange = null;
		if (sslEnabled) {
			configPortRange = config.getProperty(DME2Constants.AFT_DME2_PORT_RANGE,
					config.getProperty(DME2Constants.AFT_DME2_SERVER_DEFAULT_SSL_PORT_RANGE));
		} else {
			configPortRange = config.getProperty(DME2Constants.AFT_DME2_PORT_RANGE,
					config.getProperty(DME2Constants.AFT_DME2_SERVER_DEFAULT_PORT_RANGE));
		}

		DME2PortFileManager fmgr = DME2PortFileManager.getInstance(config);
		String persistedPorts = fmgr.getPort(service, sslEnabled == true ? true : false);

		if (persistedPorts != null) {
			server.setPersistedPorts(persistedPorts);
		}

		if (configHost != null) {
			server.getServerProperties().setHostname(configHost);

		}

		if (configPortRange != null) {
			server.getServerProperties().setPortRange(configPortRange);
		}

		if (configPort != null) {
			server.getServerProperties().setPort(Integer.parseInt(configPort));
		}
		server.start();
		this.hostname = server.getServerProperties().getHostname();
		this.port = server.getServerProperties().getPort();
	}

	/**
	 * 
	 * 
	 * 
	 * Unbind service listener.
	 *
	 * @param service
	 *            the service
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */
	public void unbindServiceListener(String service) throws DME2Exception {
		this.bindServer = true;
		if (server == null) {
			synchronized (instance) {
				if (server == null) {
					initDefaultServer(service);
				}
			}
		}
		registry.unpublish(service, hostname, port);
		server.unbindServiceListener(service);
	}

	/**
	 * Return the manager's name
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Initialize the configured registry
	 *
	 * @return
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */

	private DME2EndpointRegistry initRegistry() throws DME2Exception {
		String registryClassName = config.getProperty(DME2Constants.DME2_EP_REGISTRY_CLASS);

		final DME2EndpointRegistry reg;
		if (registryClassName.equals(DME2Constants.DME2GRM)) {

			reg = DME2EndpointRegistryFactory.getInstance().createEndpointRegistry(
					DME2Constants.DME2GRM.concat("-").concat(getName()), config, DME2EndpointRegistryType.GRM,
					this.getName(), config.getProperties());
		} else if (registryClassName.equals(DME2Constants.DME2FS)) {
			reg = DME2EndpointRegistryFactory.getInstance().createEndpointRegistry(
					DME2Constants.DME2FS.concat("-").concat(getName()), config, DME2EndpointRegistryType.FileSystem,
					this.getName(), config.getProperties());
		} else if (registryClassName.equals(DME2Constants.DME2MEMORY)) {
			reg = DME2EndpointRegistryFactory.getInstance().createEndpointRegistry(
					DME2Constants.DME2MEMORY.concat("-").concat(getName()), config, DME2EndpointRegistryType.Memory,
					this.getName(), config.getProperties());
		} else {
			try {
				reg = (DME2EndpointRegistry) Class.forName(registryClassName).newInstance();
			} catch (Exception e) {
				throw new DME2Exception("AFT-DME2-0006", new ErrorContext().add(MANAGER, this.name), e);
			}
		}
		return reg;
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * Initialize the DME2 network server
	 *
	 * @return
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */
	private DME2Server initServer() throws DME2Exception {
		DME2Server ser = new DME2Server(config);
		ser.setManager(this);
		return ser;
	}

	/**
	 * Sets the manager's credentials. Thsi will be used when client's of this
	 * manager calls other servers.
	 *
	 * @param userName
	 * @param password
	 */
	public void setClientCredentials(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	/**
	 * Return the distance bandss this manager is currently uses for
	 * geographically-based routing
	 */
	public double[] getDistanceBands() {
		return registry.getDistanceBands();
	}

	private void createEvent(final EventType type, final Map<String, Object> props) {
		DME2Event event = new DME2Event();
		switch (type) {
		case REPLY_EVENT:
		case FAULT_EVENT:
			event.setReplyMsgSize((Long) props.get(DME2Constants.MSG_SIZE));
			break;
		case INIT_EVENT:
		case REQUEST_EVENT:
		case FAILOVER_EVENT:
			event.setReqMsgSize((Long) props.get(DME2Constants.MSG_SIZE));
			break;
		}
		event.setType(type);
		event.setMessageId((String) props.get(DME2Constants.MESSAGE_ID) != null
				? (String) props.get(DME2Constants.MESSAGE_ID) : (String) props.get(DME2Constants.DME2_WS_CONNECT_ID));// DME2_WS_CONNECT_ID

		// is
		// for
		// DME2CliWebSocket.onWebSocketConnect(WebSocketConnection
		// conn)
		event.setEventTime((Long) props.get(DME2Constants.EVENT_TIME));

		if ((String) props.get(DME2Constants.DME2_INTERFACE_PROTOCOL) != null
				&& ((String) props.get(DME2Constants.DME2_INTERFACE_PROTOCOL))
				.equals(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL)) {
			event.setProtocol(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL));
		} else {
			event.setProtocol((String) props.get(DME2Constants.DME2_INTERFACE_PROTOCOL));
		}

		event.setProtocol((String) props.get(DME2Constants.DME2_INTERFACE_PROTOCOL));
		event.setRole((String) props.get(DME2Constants.DME2_INTERFACE_ROLE));
		event.setInterfacePort((String) props.get(DME2Constants.DME2_INTERFACE_PORT));
		event.setQueueName((String) props.get(DME2Constants.QUEUE_NAME));
		event.setService((String) props.get(DME2Constants.DME2_WEBSOCKET_SERVICE_NAME));
		event.setElapsedTime((Long) props.get(DME2Constants.ELAPSED_TIME));
		event.setPartner((String) props.get(DME2Constants.DME2_REQUEST_PARTNER));

		eventManager.postEvent(event);
	}

	/**
	 * Post a stat-event
	 */
	public void postStatEvent(Map<String, Object> props) {
		if (config.getBoolean(DME2Constants.AFT_DME2_COLLECT_SERVICE_STATS)) {

			try {
				logger.debug(null, "postStatEvent", "trying to post event");
				EventType type = null;
				if (props != null) {
					logger.debug(null, "postStatEvent", "Event details as being posted: {}", props);

					if (props.get(DME2Constants.FAULT_EVENT) != null
							&& (Boolean) props.get(DME2Constants.FAULT_EVENT)) {
						type = EventType.FAULT_EVENT;
					} else if (props.get(DME2Constants.REPLY_EVENT) != null
							&& (Boolean) props.get(DME2Constants.REPLY_EVENT)) {
						type = EventType.REPLY_EVENT;
					} else if (props.get(DME2Constants.REQUEST_EVENT) != null
							&& (Boolean) props.get(DME2Constants.REQUEST_EVENT)) {
						type = EventType.REQUEST_EVENT;
					} else if (props.get(DME2Constants.FAILOVER_EVENT) != null
							&& (Boolean) props.get(DME2Constants.FAILOVER_EVENT)) {
						type = EventType.FAILOVER_EVENT;
					} else if (props.get(DME2Constants.INIT_EVENT) != null
							&& (Boolean) props.get(DME2Constants.INIT_EVENT)) {
						type = EventType.INIT_EVENT;
					}

					if (type != null) {
						createEvent(type, props);
					} else {
						logger.warn(null, "postStatEvent", "event type is not correct: {}", props);

					}

				} else {
					logger.warn(null, "postStatEvent", "event details as provided is null");
				}

			} catch (Exception e) {
				logger.warn(null, "postStatEvent", "error encountered while handling event");
			}
			// TODO Fix this- needed to verify the above implementation
			// stats.submitEvent(new EventProcessor(stats, props));
		}
	}

	@Override
	public String[] diagnostics() throws Exception {

		ArrayList<String> retList = new ArrayList<String>();
		String[] retStr = null;
		try {
			Iterator<String> it = statManager.getServiceNames().iterator();
			while (it.hasNext()) {
				boolean ignoreQueue = false;
				String serviceName = it.next();
				String splitStr[] = config.getProperty(DME2Constants.AFT_DME2_QLIST_IGNORE).split(",");
				for (int i = 0; i < splitStr.length; i++) {
					if (serviceName != null && serviceName.contains(splitStr[i])) {
						ignoreQueue = true;
						break;
					}
				}
				// If queueName is found in ignore list, ignore getting stats
				if (ignoreQueue) {
					continue;
				}
				DME2ServiceStats sstats = statManager.getServiceStats(serviceName);
				String statsArr[] = sstats.getStats();
				for (int j = 0; j < statsArr.length; j++) {
					retList.add(statsArr[j]);
				}
			}
		} catch (Exception e) {
			logger.debug(null, "diagnostics", LogMessage.SVC_STATS_FAIL, e);
		}
		retStr = new String[retList.size()];
		return retList.toArray(retStr);
	}

	@Override

	public void dump() throws Exception {
		return;
	}

	@Override

	public String getLoggingLevel() {
		// Logger log = LoggerFactory.getLogger( DME2Manager.class.getName() );
		return "info";

	}

	@Override
	public boolean heartbeat() throws Exception {
		return true;

	}

	@Override
	public boolean kill() throws Exception {

		return false;
	}

	@Override

	public void refresh() throws Exception {
		getEndpointRegistry().refresh();
		staleCacheAdapter.clearStaleEndpoints();
	}

	@Override

	public void setLoggingLevel(String newLoggingLevel) {
		// LEVEL:TYPE:OPTION=VALUE;OPTION=VALUE
		// CONFIG:Handler=File;Path=logs
		// CONFIG;Handler=Console
		String[] toks = newLoggingLevel.split(";");
		String level = toks[0];
		String handler = null;
		String path = null;
		if (toks.length > 1) {
			for (String t : toks) {
				String[] pair = t.split("=");
				if (pair.length == 2) {
					String key = pair[0];
					String value = pair[1];
					if (key.toUpperCase().equals("HANDLER")) {
						handler = value;
					} else if (key.toUpperCase().equals("PATH")) {
						path = value;
					}
				}
			}
		}
		if (handler != null && handler.toUpperCase().equals("file")) {
			if (path == null) {
				throw new RuntimeException("FILE type specified but PATH not provided");
			}
		} else if (handler != null && handler.toUpperCase().equals("console")) {
			ConsoleHandler consoleHandler = new ConsoleHandler();
			// Commented as this is not supported in slf4j
			consoleHandler.setLevel(Level.parse(level));
		}
	}

	@Override
	public boolean shutdown() throws Exception {
		logger.debug(null, "shutdown", LogMessage.METHOD_ENTER);
		logger.debug(null, "shutdown", "Shutting down manager {}", name);
		Exception t = null;
		try {
			if (dme2WsClientFactory != null) {
				dme2WsClientFactory.stop();
			}
		} catch (Exception e) {
			logger.error(null, "shutdown", LogMessage.SERVER_STOP_FAIL, e);

			t = e;
		}
		try {
			logger.debug(null, "shutdown", "Shutting down registry for manager {}", name);
			registry.shutdown();
		} catch (Exception e) {
			logger.error(null, "shutdown", LogMessage.SERVER_STOP_FAIL, e);
			t = e;
		}

		try {
			server.stop();
		} catch (Exception e) {
			logger.error(null, "shutdown", LogMessage.SERVER_STOP_FAIL, e);
			t = e;
		}

		if (t != null) {
			throw t;

		}

		return running;

	}

	@Override
	public String statistics() throws Exception {
		return Arrays.toString(diagnostics());

	}

	public HttpClient getClient() throws DME2Exception {
		synchronized (this) {
			if (client == null) {
				int maxConnsPerAddress = config.getInt(DME2Constants.AFT_DME2_CLIENT_MAX_CONNS_PER_ADDRESS);
				int readTimeoutMs = config.getInt(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS);
				int connectTimeoutMs = config.getInt(DME2Constants.AFT_DME2_CLIENT_CONNECT_TIMEOUT_MS);
				int tpMaxIdleTimeMs = config.getInt(DME2Constants.AFT_DME2_CLIENT_TP_MAX_IDLE_TIME_MS);
				int tpMaxThreads = config.getInt(DME2Constants.AFT_DME2_CLIENT_TP_MAX_THREADS);
				int tpMinThreads = config.getInt(DME2Constants.AFT_DME2_CLIENT_TP_MIN_THREADS);
				int tpMaxQueued = config.getInt(DME2Constants.AFT_DME2_CLIENT_TP_MAX_QUEUED);
				int maxBuffers = config.getInt(DME2Constants.AFT_DME2_CLIENT_MAX_BUFFERS);
				// Jetty 9 disabled use direct buffers
				// https://dev.eclipse.org/mhonarc/lists/jetty-users/msg03779.html
				int requestBufferSize = config.getInt(DME2Constants.AFT_DME2_CLIENT_REQ_BUFFER_SIZE);
				int requestHeaderSize = config.getInt(DME2Constants.AFT_DME2_CLIENT_REQ_HEADER_SIZE);
				int responseBufferSize = config.getInt(DME2Constants.AFT_DME2_CLIENT_RSP_BUFFER_SIZE);
				int responseHeaderSize = config.getInt(DME2Constants.AFT_DME2_CLIENT_RSP_HEADER_SIZE);
				// Changing maxRetry to 1 as AUTHENTICATION is failing with
				// retry as zero
				int maxRetries = config.getInt(DME2Constants.AFT_DME2_CLIENT_MAX_RETRIES);
				boolean connectBlocking = config.getBoolean(DME2Constants.AFT_DME2_CLIENT_CONN_BLOCKING);
				boolean allowRenegotiate = config.getBoolean(DME2Constants.AFT_DME2_CLIENT_ALLOW_RENEGOTIATE);
				String clientProxyHost = config.getProperty(DME2Constants.AFT_DME2_CLIENT_PROXY_HOST);
				String clientProxyPort = config.getProperty(DME2Constants.AFT_DME2_CLIENT_PROXY_PORT);
				client = new HttpClient(new SslContextFactory(true));
				client.setMaxConnectionsPerDestination(maxConnsPerAddress);

				// readTimeoutMs, maxBuffers, requestHeaderSize,
				// responseHeaderSize and maxRetries are unused - by design?

				logger.debug(null, "getClient",
						"DME2Manager httpClient maxBuffers={};requestBufferSize={};requestHeaderSize={};responseBufferSize={};responseHeaderSize={};maxRetries={};connectBlocking={}",
						maxBuffers, requestBufferSize, requestHeaderSize, responseBufferSize, responseHeaderSize,

						maxRetries, connectBlocking);

				client.setRequestBufferSize(requestBufferSize);
				client.setResponseBufferSize(responseBufferSize);
				client.setConnectBlocking(connectBlocking);

				if (client.getSslContextFactory() != null) {
					if (isIgnoreSSLConfig()) {
						logger.debug(null, "getClient", "DME2Manager configuring ssl");
						// Allow override only if ssl config is not ignored, by
						// default not all params can be overriden for client
						// side
						// Only AllowRenegotiate is overriden
						configureSsl(client.getSslContextFactory());

					}
					logger.debug(null, "getClient", "DME2Manager.getClient isAllowRenegotiate={}",
							client.getSslContextFactory().isRenegotiationAllowed());
					client.getSslContextFactory().setRenegotiationAllowed(allowRenegotiate);
					logger.debug(null, "getClient", "DME2Manager.getClient setAllowRenegotiate={}", allowRenegotiate);

				}
				// TODO check if we need this
				DME2QueuedThreadPool tp = new DME2QueuedThreadPool();
				tp.setIdleTimeout(tpMaxIdleTimeMs);
				tp.setDaemon(true);
				client.setMaxRequestsQueuedPerDestination(tpMaxQueued);
				tp.setMaxThreads(tpMaxThreads);
				tp.setMinThreads(tpMinThreads);
				client.addBean(tp);
				client.setConnectTimeout(connectTimeoutMs); // connect timeout
				if (clientProxyHost != null && clientProxyPort != null) {
					ProxyConfiguration proxyConfig = client.getProxyConfiguration();
					HttpProxy proxy = new HttpProxy(clientProxyHost, new Integer(clientProxyPort).intValue());
					proxyConfig.getProxies().add(proxy);

				}
				// If username/password is not null
				if (this.userName != null && this.password != null) {
					this.setClientCredentials(this.userName, this.password);

				}

				try {
					client.start();

				} catch (Exception e) {
					throw new DME2Exception("AFT-DME2-0007", new ErrorContext().add(MANAGER, instance.getName()), e);

				}
			}
		}

		return client;

	}

	/**
	 * @return the boolean value indicating whether we should or should not
	 *         ignore the ssl configuration
	 */
	private boolean isIgnoreSSLConfig() {
		return !config.getBoolean(DME2Constants.AFT_DME2_CLIENT_IGNORE_SSL_CONFIG);

	}

	private String[] getPropsArray(String key) {
		String temp = config.getProperty(key);
		if (temp != null) {
			return temp.split(",");

		}
		return null;

	}

	private void configureSsl(SslContextFactory cf) throws DME2Exception {
		String keySslExcludeProtocols = DME2Constants.AFT_DME2_CLIENT_SSL_EXCLUDE_PROTOCOLS;
		String keySslExcludeCipherSuite = DME2Constants.AFT_DME2_CLIENT_SSL_EXCLUDE_CIPHERSUITES;
		String keySslIncludeProtocols = DME2Constants.AFT_DME2_CLIENT_SSL_INCLUDE_PROTOCOLS;
		String keySslIncludeCipherSuite = DME2Constants.AFT_DME2_CLIENT_SSL_INCLUDE_CIPHERSUITES;
		String keystore = getKeyStore();
		String keystorePw = config.getProperty(DME2Constants.KEY_KEYSTORE_PASSWORD);
		String truststore = getTrustStore();
		String truststorePw = config.getProperty(DME2Constants.KEY_TRUSTSTORE_PASSWORD);
		String keyPw = config.getProperty(DME2Constants.KEY_PASSWORD);
		Boolean allowRenegotiate = this.isAllowRenegotiate();
		Boolean trustAll = this.getSslTrustAll();
		String certAlias = this.getSslCertAlias();
		Boolean needClientAuth = this.getNeedClientAuth();
		Boolean wantClientAuth = this.getWantClientAuth();
		Boolean enableSessionCaching = this.isEnableSessionCaching();
		Integer sslSessionCacheSize = this.getSslSessionCacheSize();
		Integer sslSessionTimeout = this.getSslSessionTimeout();
		Boolean validatePeerCerts = this.isSslValidatePeerCerts();
		Boolean validateCerts = this.isValidateCerts();
		String[] excludeProtocols = getPropsArray(keySslExcludeProtocols);
		String[] includeProtocols = getPropsArray(keySslIncludeProtocols);
		String[] excludeCipherSuites = getPropsArray(keySslExcludeCipherSuite);
		String[] includeCipherSuites = getPropsArray(keySslIncludeCipherSuite);
		// check for null. if not set, defer to Jetty defaults
		if (keystore != null) {
			cf.setKeyStorePath(keystore);

		}
		if (StringUtils.isNotBlank(keystorePw)) {
			cf.setKeyStorePassword(keystorePw);
		} else {
			throw new DME2Exception("AFT-DME2-0918",
					new ErrorContext().add(DME2Constants.KEY_KEYSTORE_PASSWORD, keystorePw));

		}
		if (keyPw != null) {
			cf.setKeyManagerPassword(keyPw);

		}
		if (trustAll != null) {
			cf.setTrustAll(trustAll);

		}
		if (certAlias != null) {
			cf.setCertAlias(certAlias);

		}
		if (needClientAuth != null) {
			cf.setNeedClientAuth(needClientAuth);

		}
		if (wantClientAuth != null) {
			cf.setWantClientAuth(wantClientAuth);

		}
		if (enableSessionCaching != null) {
			cf.setSessionCachingEnabled(enableSessionCaching);

		}
		if (sslSessionCacheSize != null) {
			cf.setSslSessionCacheSize(sslSessionCacheSize);

		}
		if (sslSessionTimeout != null) {
			cf.setSslSessionTimeout(sslSessionTimeout);

		}
		if (validatePeerCerts != null) {
			cf.setValidatePeerCerts(validatePeerCerts);

		}
		if (validateCerts != null) {
			cf.setValidateCerts(validateCerts);

		}
		if (excludeProtocols != null) {
			cf.setExcludeProtocols(excludeProtocols);

		}
		if (includeProtocols != null) {
			cf.setIncludeProtocols(includeProtocols);

		}
		if (excludeCipherSuites != null) {
			cf.setExcludeCipherSuites(excludeCipherSuites);

		}
		if (includeCipherSuites != null) {
			cf.setIncludeCipherSuites(includeCipherSuites);

		}
		// probably need to throw an exception if the truststore pw is not set
		if (truststore != null) {
			if (StringUtils.isBlank(truststorePw)) {
				throw new DME2Exception("AFT-DME2-0919",
						new ErrorContext().add(DME2Constants.KEY_TRUSTSTORE_PASSWORD, truststorePw));
			} else {
				cf.setTrustStorePassword(truststorePw);

			}
		} else {
			cf.setTrustStorePassword(keystorePw);

		}
		cf.setRenegotiationAllowed(allowRenegotiate);

	}

	public Boolean isAllowRenegotiate() {
		return config.getBoolean(DME2Constants.KEY_ALLOW_RENEG);

	}

	private String getTrustStore() {
		return config.getProperty(DME2Constants.KEY_TRUSTSTORE);

	}

	private String getKeyStore() {
		return config.getProperty(DME2Constants.KEY_KEYSTORE);

	}

	private Boolean isValidateCerts() {
		return config.getBoolean(DME2Constants.KEY_SSL_VALIDATE_CERTS);

	}

	private Boolean isSslValidatePeerCerts() {
		return config.getBoolean(DME2Constants.KEY_SSL_VALIDATE_PEER_CERTS);

	}

	private Integer getSslSessionTimeout() {
		return config.getInteger(DME2Constants.KEY_SSL_SESSION_TIMEOUT, null);

	}

	private Integer getSslSessionCacheSize() {
		return config.getInteger(DME2Constants.KEY_SSL_SESSION_CACHE_SIZE, null);

	}

	private Boolean isEnableSessionCaching() {
		return config.getBoolean(DME2Constants.KEY_SSL_ENABLED_SESSION_CACHING);

	}

	private Boolean getWantClientAuth() {
		return config.getBoolean(DME2Constants.KEY_SSL_WANT_CLIENT_AUTH);

	}

	private Boolean getNeedClientAuth() {
		return config.getBoolean(DME2Constants.KEY_SSL_NEED_CLIENT_AUTH);

	}

	private String getSslCertAlias() {
		return config.getProperty(DME2Constants.KEY_SSL_CERT_ALIAS);

	}

	private Boolean getSslTrustAll() {
		return config.getBoolean(DME2Constants.KEY_SSL_TRUST_ALL);

	}

	public String getUserName() {
		return userName;

	}

	public boolean isEndpointStale(String url) {
		// TODO: Make sure anything added to the stale endpoint cache sets the
		// expiration time
		// Long staleTime = getEndpointRegistry().getStaleEndpointExpiration(
		// url );
		Long staleTime = staleCacheAdapter.getEndpointExpirationTime(url);
		if (staleTime == null) {
			return false;

		}

		if (staleTime <= System.currentTimeMillis()) {
			// getEndpointRegistry().removeStaleEndpoint( url );
			staleCacheAdapter.removeStaleEndpoint(url);
			logger.debug(null, "isEndpointStale", LogMessage.STALENESS_EXPIRED, url);
			return false;

		}
		return true;

	}

	/**
	 * The below method is to have endpoint list exposed to Registry call to
	 * decide upon whether to refresh the cached list
	 *
	 * 
	 * @param url
	 * @return
	 */
	public boolean isUrlInStaleList(String url) {
		return staleCacheAdapter.isEndpointStale(url);

	}

	/**
	 * @param url
	 */
	public void addStaleEndpoint(String url) {
		staleCacheAdapter.addStaleEndpoint(url, endpointStalenessPeriodMs + System.currentTimeMillis());

	}

	/**
	 * Adds a service endpoint that has been marked stale to the stale cache.
	 *
	 * 
	 * @param url
	 *            The url of the service endpoint that has been marked as stale.
	 * @param newEndpointStalenessPeriod
	 *            The duration (in minutes) that the service endpoint will
	 *            remain in the stale cache before being retried.
	 */
	public void addStaleEndpoint(String url, final Long newEndpointStalenessPeriod) {
		Long endpointStalenessPeriod = newEndpointStalenessPeriod;
		if (endpointStalenessPeriod == null || endpointStalenessPeriod == 0) {
			endpointStalenessPeriod = endpointStalenessPeriodMs;

		}
		staleCacheAdapter.addStaleEndpoint(url, System.currentTimeMillis() + endpointStalenessPeriod);

	}

	/**
	 * @param url
	 */
	public void removeStaleEndpoint(String url) {
		staleCacheAdapter.removeStaleEndpoint(url);

	}

	/**
	 * @param url
	 * @return
	 */
	public boolean isRouteOfferStale(String url) {
		return staleCacheAdapter.isRouteOfferStale(url);

	}

	public void addStaleRouteOffer(String url, Long routeOfferStalenessPeriod) {
		Long stalePeriod = null;
		/* Check if staleness was provided via -D option */
		String stalePeriodStr = System.getProperty(DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN);
		if (stalePeriodStr != null) {
			logger.debug(null, "addStaleRouteOffer", LogMessage.DEBUG_MESSAGE,
					"RouteOffer staleness duration found in System Properties. Value = {} minutes.", stalePeriodStr);
			try {
				/* Convert to string to a Long value */
				stalePeriod = Long.parseLong(stalePeriodStr);
				stalePeriod = stalePeriod * 60000;
			} catch (NumberFormatException e) {
				/*
				 * Ignore Exception and use default value define in DME2Manager
				 */
				logger.debug(null, "addStaleRouteOffer",
						"Exception occured while parsing staleness duration provided in System Properties. Ignoring and using default value of {} minutes.",
						routeOfferStalenessPeriodMs);
				stalePeriod = routeOfferStalenessPeriodMs;

			}
		} else /*
		 * If staleness value was not defined via -D option, use the
		 * value provided in this method
		 */ {
			/*
			 * If stalenessPeriod is not provided in method (null or equal to
			 * 0), default to value defined in DME2Manager
			 */
			stalePeriod = ((routeOfferStalenessPeriod != null && routeOfferStalenessPeriod != 0)

					? routeOfferStalenessPeriod : routeOfferStalenessPeriodMs) * 60000;
		}
		// staleRouteOfferCache.getCache().put(url, System.currentTimeMillis() +
		// (stalePeriod));
		// TODO: Move stalePeriod to registry/cache
		// getEndpointRegistry().cacheStaleRouteOffer( url );
		stalePeriod += System.currentTimeMillis();
		staleCacheAdapter.addStaleRouteOffer(url, stalePeriod);
		logger.debug(null, "addStaleRouteOffer",
				"Marked RouteOffer stale for service: {}. Staleness duration in milliseconds is: {}", url, stalePeriod);

	}

	public void removeStaleRouteOffer(String url) {
		logger.debug(null, "addStaleRouteOffer", "Removing stale RouteOffer for service: {}", url);
		staleCacheAdapter.removeStaleRouteOffer(url);
	}

	/**
	 * Disables metrics for this manager
	 */
	@Override
	public void disableMetrics() {
		// disableMetrics = config.getBoolean( "AFT_DME2_DISABLE_METRICS", false
		// );
		disableMetrics = true;

	}

	/**
	 * Enables metrics for this manager
	 */
	@Override
	public void enableMetrics() {
		// disableMetrics = config.getBoolean( "AFT_DME2_DISABLE_METRICS", false

		// );
		disableMetrics = false;
	}

	/**
	 * Enable metrics Servlet filter for this manager
	 */
	@Override
	public void enableMetricsFilter() {
		// DISABLE_METRICS_FILTER = config.getBoolean(
		// "AFT_DME2_DISABLE_METRICS_FILTER", false );
		DISABLE_METRICS_FILTER = false;

	}

	/**
	 * Disable metrics Servlet filter for this manager
	 */
	@Override
	public void disableMetricsFilter() {
		// DISABLE_METRICS_FILTER = config.getBoolean(
		// "AFT_DME2_DISABLE_METRICS_FILTER", false );
		DISABLE_METRICS_FILTER = true;

	}

	/**
	 * Enable throttle Servlet filter for this manager
	 */
	@Override

	public void enableThrottleFilter() {
		// DISABLE_THROTTLE_FILTER = config.getBoolean(
		// "AFT_DME2_DISABLE_THROTTLE_FILTER", false );
		DISABLE_THROTTLE_FILTER = false;

	}

	/**
	 * 
	 * Disable throttle Servlet filter for this manager
	 */
	@Override
	public void disableThrottleFilter() {

		// DISABLE_THROTTLE_FILTER = config.getBoolean(
		// "AFT_DME2_DISABLE_THROTTLE_FILTER", false );
		DISABLE_THROTTLE_FILTER = true;
	}

	/**
	 * Print the DME2 version to the logger
	 */
	public void printVersion() {
		try {
			Object obj = Class.forName(PACKAGE).newInstance();
			java.lang.reflect.Method method = Class.forName(PACKAGE).getMethod("getVersion");
			Object retVal = method.invoke(obj, (Object[]) null);
			String version = (String) retVal;

			logger.info(null, "printVersion", "DME2 Version={}", version);
			if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
				logger.debug(null, "printVersion", new java.util.Date() + "\t DME2 Version " + version);
			}
		} catch (Exception e) {
			logger.error(null, "printVersion",
					"DME2 error loading Version class. Verify whether {} class is in classpath", PACKAGE);
		}
	}

	/**
	 * Returns the current DME2 version
	 */
	public static String getVersion() {
		String version = null;

		try {
			Object obj = Class.forName(PACKAGE).newInstance();
			java.lang.reflect.Method method = Class.forName(PACKAGE).getMethod("getVersion");
			Object retVal = method.invoke(obj, (Object[]) null);
			version = (String) retVal;
		} catch (Exception e) {
			logger.error(null, "getVersion", LogMessage.VERSION_FAIL);
		}
		return version;
	}

	/*
	 * Given a DME2ServiceHolder, publish the service on this manager's server's
	 * port. This is generally only used by DME2Server implementations when
	 * starting a service's related web artifacts.
	 */
	public void publish(DME2ServiceHolder service) throws DME2Exception {
		try {
			if (!server.isRunning()) {
				throw new DME2Exception("AFT-DME2-1803", new ErrorContext().add(SERVICE, service.getServiceURI()));
			}

			hostname = server.getServerProperties().getHostname();
			if (hostname != null) {
				if (hostname.startsWith("0.")) {
					try {
						hostname = InetAddress.getLocalHost().getByName(InetAddress.getLocalHost().getHostAddress())
								.getHostName();
					} catch (Exception e) {
						logger.debug(null, "publish", LogMessage.DEBUG_MESSAGE, "Exception", e);
					}
				}
			}
			port = server.getServerProperties().getPort();
			// Persist the port for reuse on next start attempt for same service
			DME2PortFileManager fmgr = DME2PortFileManager.getInstance(config);

			fmgr.persistPort(DME2Utils.getRunningInstanceName(config), port,
					server.getServerProperties().isSslEnable() == true ? true : false);

			if (service.getDme2WebSocketHandler() != null) {
				String serviceContext = service.getContext();
				if (config.getBoolean(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_APPEND_CONTEXT)) {
					if (!serviceContext.endsWith("/")) {
						serviceContext = serviceContext + "/";

					}
				}

				/*
				 * if ( server.getServerProperties().isSslEnable() ) {
				 * registry.publish( service.getServiceURI(), serviceContext,
				 * hostname, port, latitude, longitude, "wss",
				 * service.getServiceProperties(), false ); } else {
				 * 
				 * registry.publish( service.getServiceURI(), serviceContext,
				 * hostname, port, latitude, longitude, "ws",
				 * service.getServiceProperties(), false ); }
				 * 
				 */
				if (server.getServerProperties().isSslEnable()) {
					registry.publish(service.getServiceURI(), serviceContext, hostname, port, latitude, longitude,

							"wss");
				} else {
					registry.publish(service.getServiceURI(), serviceContext, hostname, port, latitude, longitude,

							"ws");
				}

			} else {
				/*
				 * if ( server.getServerProperties().isSslEnable() ) {
				 * registry.publish( service.getServiceURI(),
				 * service.getContext(), hostname, port, latitude, longitude,
				 * "https", service.getServiceProperties(), false); } else {
				 * registry.publish( service.getServiceURI(),
				 * service.getContext(), hostname, port, latitude, longitude,
				 * "http", service.getServiceProperties(), false ); }
				 */
				if (server.getServerProperties().isSslEnable()) {
					registry.publish(service.getServiceURI(), service.getContext(), hostname, port, latitude, longitude,

							"https");
				} else {
					registry.publish(service.getServiceURI(), service.getContext(), hostname, port, latitude, longitude,

							"http");
				}
			}
			logger.debug(null, "publish", "AFT-DME2-1800 {}", new ErrorContext().add(SERVICE, service.getServiceURI())
					.add("address", server.getBaseAddress()).add("hostname", hostname).add("port", port + ""));

			/**
			 * Below steps are to allow service alias names/URI's published with
			 * single endpoint for a service.
			 */
			List<String> serviceAliases = service.getServiceAliases();
			if (serviceAliases != null && serviceAliases.size() > 0) {
				Iterator<String> iter = serviceAliases.iterator();
				while (iter.hasNext()) {
					String tsAlias = iter.next();
					String serviceURI = service.getServiceURI();

					if (serviceURI != null && !tsAlias.equals(serviceURI)) {
						if (server.getServerProperties().isSslEnable()) {
							registry.publish(tsAlias, service.getContext(), hostname, port, latitude, longitude,
									"https");
						} else {
							registry.publish(tsAlias, service.getContext(), hostname, port, latitude, longitude,
									"http");
						}

						logger.debug(null, "publish", "AFT-DME2-1801",

								new ErrorContext().add("serviceAlias", tsAlias)
								.add("serviceURI", service.getServiceURI()).add("context", service.getContext())
								.add("address", server.getBaseAddress()).add("hostname", hostname)

								.add("port", port + ""));
					}
				}
			}
		} catch (Exception e) {
			throw DME2ExceptionHandler.handleException(e, service.getServiceURI());

		}
	}

	/**
	 * Given a service, unpublish from the registry. This is generally only used
	 * by DME2Server implementations when stopping a service's related web
	 * artifacts.
	 *
	 * @param service
	 * @throws com.att.aft.dme2.api.DME2Exception
	 */
	public void unpublish(DME2ServiceHolder service) throws DME2Exception {
		try {
			String hostName = server.getServerProperties().getHostname();
			if (hostName != null) {
				if (hostName.startsWith("0.")) {
					try {
						hostName = InetAddress.getLocalHost().getByName(InetAddress.getLocalHost().getHostAddress())
								.getHostName();
					} catch (Exception e) {
						logger.debug(null, "unpublish", LogMessage.DEBUG_MESSAGE, "Exception", e);
					}
				}
			}
			registry.unpublish(service.getServiceURI(), hostName, server.getServerProperties().getPort());
			logger.debug(null, "unpublish", "AFT-DME2-1802", new ErrorContext().add(SERVICE, service.getServiceURI())
					.add("address", server.getBaseAddress()).add("hostname", hostName).add("port", port + ""));
		} catch (Exception e) {
			throw DME2ExceptionHandler.handleException(e, service.getServiceURI());
		}
	}

	public Set<String> getGlobalNoticeCache() {
		return globalNoticeCache;
	}

	public ThreadPoolExecutor getExchangeRetryThreadPool() {
		return this.retryThreadpool;

	}

	public String getCharacterSet() {
		return charset;
	}

	public void setCharacterSet(String charset) {
		this.charset = charset;
	}

	public void setIgnoreFailoverOnExpire(boolean ignoreFailoverOnExpire) {
		this.ignoreFailoverOnExpire = ignoreFailoverOnExpire;

	}

	public boolean isIgnoreFailoverOnExpire() {
		return ignoreFailoverOnExpire;
	}

	public DME2WSClientFactory getWsClientFactory() {
		return dme2WsClientFactory;

	}

	private void configureSsl(DME2WSClientFactory factory) throws DME2Exception {
		configureSsl(factory.getWsClientFactory().getSslContextFactory());

	}

	public DME2WSClientFactory initWSClientFactory() throws DME2Exception {
		if (dme2WsClientFactory != null) {
			return dme2WsClientFactory;

		}

		synchronized (lockObject) {
			if (dme2WsClientFactory == null) {
				dme2WsClientFactory = new DME2WSClientFactory(this);

				if (!config.getBoolean(DME2Constants.AFT_DME2_CLIENT_IGNORE_SSL_CONFIG)) {
					// Allow override only if ssl config is not ignored, by
					// default not all params can be overriden for client side
					// Only AllowRenegotiate is overriden
					configureSsl(dme2WsClientFactory);

				}
				dme2WsClientFactory.start();

			}
		}
		if (wsRetryThreadpool == null) {
			wsRetryThreadpool = DME2ThreadPoolConfig.getInstance(this).createWebSocketRetryFactoryThreadPool();

		}
		return dme2WsClientFactory;

	}

	public ThreadPoolExecutor getWsRetryThreadpool() {
		return this.wsRetryThreadpool;

	}

	public void logStatementsForTesting(String log) {

	}

	public DME2StaleCacheAdapter getStaleCache() {
		return staleCacheAdapter;

	}

	public DME2Configuration getConfig() {
		return config;

	}

	public void setConfig(DME2Configuration config) {
		this.config = config;

	}

	@Override
	public void setProperty(String key, String value) {
		config.setOverrideProperty(key, value);

	}

	@Override
	public Properties getProperties() {
		return this.serviceProperties;

	}

	@Override
	public void removeProperty(String key) {
		this.serviceProperties.remove(key);

	}

	public Long getLongProp(String key, Long defaultValue) {
		return config.getLong(key, defaultValue);

	}

	public Boolean getBoolProp(String key, Boolean defaultValue) {
		return config.getBoolean(key, defaultValue);

	}

	public Integer getIntProp(String key, Integer defaultValue) {
		return config.getInteger(key, defaultValue);

	}

	public String getStringProp(String key, String defaultValue) {
		return config.getProperty(key, defaultValue);

	}

	public String getProp(String key) {
		return config.getProperty(key);

	}

	public static TimeZone getTimezone() {
		return timezone;

	}

	public static Locale getLocale() {
		return locale;

	}
}