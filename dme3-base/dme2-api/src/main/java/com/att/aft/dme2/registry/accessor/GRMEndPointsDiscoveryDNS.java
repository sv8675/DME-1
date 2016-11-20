package com.att.aft.dme2.registry.accessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.util.DME2CollectionUtils;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2ParameterNames;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.OfferCache;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;

/**
 * this class contains the algorithm to get GRM servers from GRM using Seeds from DNS.
 *
 * @author ar671m
 */
public class GRMEndPointsDiscoveryDNS implements IGRMEndPointDiscovery {


	public class CacheTimerTask extends TimerTask {
		@Override
		public void run() {
			if ( manager == null ) {
				try {
					manager = DME2Manager.getDefaultInstance();
				} catch ( DME2Exception e ) {
					//GRMEndPointsDiscoveryDNS.LOGGER.log(Level.WARNING, "Can't create DME2Manager to use for sorting GRMServer Endpoints", e);
					logger
					.warn( null, "CacheTimerTask.run", "Can't create DME2Manager to use for sorting GRMServer Endpoints", e );
				}
			}
			GRMEndPointsDiscoveryDNS.this.refreshGRMServerListFromGRMSeeds();
		}
	}

	private static final Logger logger = LoggerFactory.getLogger( GRMEndPointsDiscoveryDNS.class );
	private DME2Configuration config;

	private volatile static GRMEndPointsDiscoveryDNS instance;
	private final OfferCache offerCache; // keeps list of stale GRM Endpoints
	private GRMEndPointsDiscoveryHelperGRM grmEndPointsDiscoveryHelperGRM;
	private GRMEndPointsCache grmEndPointsCache;
	private DME2Manager manager;
	private Timer timerCacheRefresh;
	private TimerTask timerTask;
	private final String protocolAddress;

	private  String grmSeedProtocol;
	private  String grmSeedPort;
	private  String grmSeedPath;
	private  String grmServiceVersion;
	private  String staticGRMEndpoint;
	
	private String grmEdgeNodePort;
	private String grmEdgeContextPath;

	private GRMEndPointsDiscoveryDNS( String dnsName, String servicename, String environment, SecurityContext ctx,
			DME2Configuration configuration, BaseAccessor grmServiceAccessor ) throws DME2Exception {
		config = configuration;
		grmSeedProtocol = config.getProperty( DME2ParameterNames.GRM_SERVER_PROTOCOL );
		grmSeedPort = config.getProperty( DME2ParameterNames.GRM_SERVER_PORT );
		grmSeedPath = config.getProperty( DME2ParameterNames.GRM_SERVER_PATH );
		grmServiceVersion = config.getProperty( DME2ParameterNames.GRM_SERVICE_VERSION );
		staticGRMEndpoint = config.getProperty( DME2ParameterNames.GRM_STATIC_ENDPOINT, "false" );
		offerCache = OfferCache.getInstance();
		manager = null; // this will prevent sort happening in first GRM call to get seed hosts to avoid infinite loop of this calling it self.
		protocolAddress = grmSeedProtocol + ":"; // used in a temporary hack to filter GRM results based on protocol


		// 1) get Seed GRM Endpoints from DNS
		GRMEndPointsDiscoveryHelperDNS grmEndPointsDiscoveryHelperDNS =
				new GRMEndPointsDiscoveryHelperDNS( dnsName, grmSeedProtocol, grmSeedPort, grmSeedPath );
		List<String> seedServersFromDNS = grmEndPointsDiscoveryHelperDNS.getGRMEndpoints();

		// 2) create cache with and initialize with randromized list of DNS seeds
		grmEndPointsCache = GRMEndPointsCache.getInstance(config);
		List<String> seedServersFromDNSRandomized = DME2CollectionUtils.randomizeURLs( seedServersFromDNS );
		grmEndPointsCache.addAllAddressList( seedServersFromDNSRandomized );

		if (staticGRMEndpoint.equalsIgnoreCase("false")) {
			timerCacheRefresh = new Timer();
			timerTask = new CacheTimerTask();
			// 3) load & add GRM Endpoints from Cached File
			try {
				grmEndPointsCache.loadFromFile();
			} catch ( IOException e ) {
				// any error can be ignored, hoping to get server list from DNS
				//LOGGER.log(Level.FINE, "Can't read cached GRM servers Endpoints", e);
				logger.debug( null, "ctor(String,String,String,SecurityContext)", "Can't read cached GRM servers Endpoints", e );
			}

			// 4) if still no server left this is a serious error that stops DME2 from functioning we can possibly use old methods now?
			if ( grmEndPointsCache.getGRMEndpoints().isEmpty() ) {
				//LOGGER.log(Level.SEVERE, "Both cached file and DNS returned no server for setting up GRM seeds");
				logger.error( null, "ctor(String,String,String,SecurityContext)",
						"Both cached file and DNS returned no server for setting up GRM seeds" );
				throw new DME2Exception( "AFT-DME2-9731", new ErrorContext() );
			}
		}



		// 5) now call GRM using seed servers to get other servers list
		if ( "https".equalsIgnoreCase( grmSeedProtocol ) ) {
			ctx.setSSL( true );
		}
		/*   BaseAccessor grmServiceAccessor = GRMAccessorFactory.getGrmAccessorHandlerInstance( config,
            SecurityContext.create( config ) );//, ctx, grmEndPointsCache ); // create a GRMServiceAccessor with Seed Servers
		 */   grmEndPointsDiscoveryHelperGRM =
				 new GRMEndPointsDiscoveryHelperGRM( environment, grmSeedProtocol, servicename, grmServiceVersion,
						 grmServiceAccessor,config );
		 if (staticGRMEndpoint.equalsIgnoreCase("false")) {
			 // 6)
			 /*
			  * start background thread to refresh list and let it create the DME2Manager later.<p>
			  * 
			  * Don't try to set manager for sort now.<p>
			  * 
			  * DME2Manager has indirect dependency on GRMAcessor this is create a circular dependency, that will makes constructor call each other for ever.<ul>
			  * 
			  * <li>DME2Manager.cinit static constructur
			  * 
			  * <li>DME2Manager.getInstance
			  * 
			  * <li>DME2Manager.<init> constructor
			  * 
			  * <li>DME2Manager.initRegistry()
			  * 
			  * <li>DME2EndpointRegistryGRM.init
			  * 
			  * <li>DME2EndpointRegistryGRM.initEndPointAccessor() </ul>
			  */
			 scheduleCacheRefresh();
		 }
	}

	private GRMEndPointsDiscoveryDNS(String hostname, SecurityContext ctx, DME2Configuration configuration, Boolean dns, Boolean directURL) throws DME2Exception {
		config = configuration;
		grmSeedProtocol = config.getProperty( DME2ParameterNames.GRM_SERVER_PROTOCOL );
		grmSeedPort = config.getProperty( DME2ParameterNames.GRM_SERVER_PORT );
		grmSeedPath = config.getProperty( DME2ParameterNames.GRM_SERVER_PATH );
		grmServiceVersion = config.getProperty( DME2ParameterNames.GRM_SERVICE_VERSION );
		staticGRMEndpoint = config.getProperty( DME2ParameterNames.GRM_STATIC_ENDPOINT, "false" );
		offerCache = OfferCache.getInstance();
		manager = null; // this will prevent sort happening in first GRM call to get seed hosts to avoid infinite loop of this calling it self.
		protocolAddress = grmSeedProtocol + ":"; // used in a temporary hack to filter GRM results based on protocol
		
		grmEdgeNodePort = config.getProperty( DME2ParameterNames.GRM_EDGE_NODE_PORT );
		grmEdgeContextPath = config.getProperty( DME2ParameterNames.GRM_EDGE_CONTEXT_PATH );
		
		if (dns) {
			GRMEndPointsDiscoveryHelperDNS grmEndPointsDiscoveryHelperDNS =
					new GRMEndPointsDiscoveryHelperDNS( hostname, grmSeedProtocol, grmEdgeNodePort, grmEdgeContextPath );
			List<String> seedServersFromDNS = grmEndPointsDiscoveryHelperDNS.getGRMEndpoints();
			grmEndPointsCache = GRMEndPointsCache.getInstance(config);
			for (String aURL : seedServersFromDNS) {
				grmEndPointsCache.addEndpointURL(aURL);
			}
			
			grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(seedServersFromDNS, config);

			if ("https".equalsIgnoreCase( grmSeedProtocol )) {
				ctx.setSSL( true );
			}
			
		} else {
			grmEndPointsCache = GRMEndPointsCache.getInstance(config);
			if (directURL) {
				String[] directUrls = hostname.split(",");
				
				for (String aURL : directUrls) {
					grmEndPointsCache.addEndpointURL(aURL);
				}
				
				grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(directUrls, config);
				
				if ("https".equalsIgnoreCase( grmSeedProtocol )) {
					ctx.setSSL( true );
				}
			} else {
				grmEndPointsCache.addEndpointURL(createGRMEdgeURL(hostname, grmEdgeNodePort, grmEdgeContextPath));
				
				grmEndPointsDiscoveryHelperGRM = new GRMEndPointsDiscoveryHelperGRM(createGRMEdgeURL(hostname, grmEdgeNodePort, grmEdgeContextPath), config);
				
				if ("https".equalsIgnoreCase( grmSeedProtocol )) {
					ctx.setSSL( true );
				}
			}
		}
	}

	protected static GRMEndPointsDiscoveryDNS getInstance( String dnsName, String servicename, String environment,
			SecurityContext ctx, DME2Configuration config, BaseAccessor grmServiceAccessor ) throws DME2Exception {
		// https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
		GRMEndPointsDiscoveryDNS result = instance;
		if ( result == null ) {
			synchronized ( GRMEndPointsDiscoveryDNS.class ) {
				result = instance;
				if ( result == null ) {
					instance = result = new GRMEndPointsDiscoveryDNS( dnsName, servicename, environment, ctx, config, grmServiceAccessor );
				}
			}
		}
		return result;
	}

	protected static GRMEndPointsDiscoveryDNS getInstance(String hostname, SecurityContext ctx, DME2Configuration config, Boolean dns, Boolean directURL) throws DME2Exception {
		GRMEndPointsDiscoveryDNS result = instance;
		if ( result == null ) {
			synchronized ( GRMEndPointsDiscoveryDNS.class ) {
				result = instance;
				if ( result == null ) {
					instance = result = new GRMEndPointsDiscoveryDNS(hostname, ctx, config, dns, directURL);
				}
			}
		}
		return result;
	}

	@Override
	public List<String> getGRMEndpoints() throws DME2Exception {
		return grmEndPointsCache.getGRMEndpoints();
	}

	public GRMEndPointsCache getGrmEndPointsCache() {
		return grmEndPointsCache;
	}

	public void setGrmEndPointsCache( GRMEndPointsCache grmEndPointsCache ) {
		this.grmEndPointsCache = grmEndPointsCache;
	}

	public void close() {
		timerTask.cancel();
		instance = null;
	}

	/**
	 * we can move this method and the timer that calls this method to GRMEndPointsDiscoveryDNS to remove a indirect
	 * cyclic dependency, between the two classes
	 */
	public void refreshGRMServerListFromGRMSeeds() {
		try {
			//LogUtil.getINSTANCE().report(LOGGER, Level.CONFIG, LogMessage.DEBUG_MESSAGE, "refreshing GRM server endpoints from GRM.");
			logger.debug( null, "refreshGRMServerListFromGRMSeeds", "refreshing GRM server endpoints from GRM." );
			// Call GRM to get latest GRM Server Endpoints
			List<String> newServerList = grmEndPointsDiscoveryHelperGRM.getGRMEndpoints();
			// update the cache if call was successful, keep old values if network is down so we can retry later without crashing
			if ( newServerList != null && !newServerList.isEmpty() ) {
				newServerList = filterByGRMProtocol(
						newServerList ); // hack to filter endpoints that start with protocol other than what we want
				newServerList = sortIfPossible( newServerList );
				logger.debug( null, "refreshGRMServerListFromGRMSeeds", "new GRM servers endpoints list = {}", newServerList );
				grmEndPointsCache.refreshServerList( newServerList );
			}
		} catch ( IOException e ) {
			// can't write cache to file
			logger.warn( null, "refreshGRMServerListFromGRMSeeds",
					"can't write GRM Cache content to the file please check value set for parameter {}",
					DME2ParameterNames.GRM_SERVER_CACHE_FILE, e );
		}
	}

	protected List<String> sortIfPossible( List<String> grmServerList ) {
		if ( manager == null ) {
			return grmServerList;
		}
		try {
			return sort( grmServerList );
		} catch ( Exception ex ) {
			// not re-throwing exception since sort is not a deal breaker system can still continue with wrong order
			//LOGGER.log(Level.SEVERE, "Can't get GRM server end points to sort them based on location", ex);
			logger.error( null, "sortIfPossible", "Can't get GRM server end points to sort them based on location", ex );
			return grmServerList;
		}
	}

	protected List<String> sort( List<String> grmServerList ) throws Exception {
		DME2Endpoint[] endpoints = convertURLsToEndPoints( grmServerList ).toArray( new DME2Endpoint[ grmServerList.size() ] );
		SortedMap<Double, DME2Endpoint[]> endpointsGroupedByDistance = DME2CollectionUtils.organizeEndpoints( endpoints );

		List<String> activeGRMServerList = new LinkedList<String>();
		List<String> staleGRMServerList = new LinkedList<String>();

		for ( DME2Endpoint[] endpointsDistance : endpointsGroupedByDistance.values() ) {
			for ( DME2Endpoint endpoint : endpointsDistance ) {
				String endPointURL = endpoint.getDmeUniformResource().toString();
				if ( offerCache.isStale( endPointURL ) ) {
					staleGRMServerList.add( endPointURL );
				} else {
					activeGRMServerList.add( endPointURL );
				}
			}
		}
		List<String> retArrayList = new ArrayList<String>( activeGRMServerList.size() + staleGRMServerList.size() );
		retArrayList.addAll( activeGRMServerList );
		staleGRMServerList.addAll( activeGRMServerList );
		return retArrayList;
	}

	protected void scheduleCacheRefresh() {
		logger.debug( null, "scheduleCacheRefresh", "cacheRefreshStartDelayInMS: {}",
				grmEndPointsCache.cacheRefreshStartDelayInMS );
		logger.debug( null, "scheduleCacheRefresh", "cacheRefreshIntervalInMS: {}",
				grmEndPointsCache.cacheRefreshIntervalInMS );
		timerCacheRefresh.schedule( timerTask, grmEndPointsCache.cacheRefreshStartDelayInMS,
				grmEndPointsCache.cacheRefreshIntervalInMS ); // delay in milliseconds
	}

	@SuppressWarnings("unchecked")
	protected List<DME2Endpoint> convertURLsToEndPoints( List<String> grmServerList )
			throws DME2Exception, MalformedURLException {
		List<DME2Endpoint> allGRMServerEndpoints = new LinkedList<DME2Endpoint>();
		for ( String address : grmServerList ) {
			DmeUniformResource uniformResource = new DmeUniformResource( config, address );
			DME2Endpoint[] endpoints = manager.getEndpoints( uniformResource );
			allGRMServerEndpoints.addAll( Arrays.asList( endpoints ) );
		}
		return allGRMServerEndpoints;
	}

	// this is for a temporary hack to fix bug in GRM server returning both HTTP and HTTPS protocols
	private final List<String> filterByGRMProtocol( List<String> grmServerList ) {
		List<String> grmServersThisProtocol = new ArrayList<String>( grmServerList.size() );
		for ( String address : grmServerList ) {
			if ( address.startsWith( protocolAddress ) ) {
				grmServersThisProtocol.add( address );
				logger.debug( null, "filterByGRMProtocol", "returning filtered address: {}", address );
			}
		}
		logger.debug( null, "filterByGRMProtocol", "returning from filter for protocol {}", protocolAddress );
		return grmServersThisProtocol;
	}
	// end of temporary hack

	private String createGRMEdgeURL(String hostname, String port, String context) {
		StringBuilder buff = new StringBuilder();
		buff.append("https://");
		buff.append(hostname);
		buff.append(":");
		buff.append(port);
		buff.append(context);
		return buff.toString();
	}

	public GRMEndPointsDiscoveryHelperGRM getGrmEndPointsDiscoveryHelperGRM() {
		return grmEndPointsDiscoveryHelperGRM;
	}
}
