package com.att.aft.dme2.registry.accessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2ParameterNames;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;

/**
 * this class holds a cache GRMConnection Server Addresses.
 * <p/>
 * i will be initialized first from a call to DNS server and then from a persistence store.
 * <p/>
 * contents will be periodically updated by calling GRM servers, by a background thread.
 * <p/>
 * Endpoints will be stored sorted on distance from client.
 *
 * @author ar671m
 */
public class GRMEndPointsCache implements IGRMEndPointDiscovery {
	// ************************************************************************
	// Internal Properties
	protected String cacheFileName;
	protected int cacheRefreshIntervalInMS;
	protected int cacheRefreshStartDelayInMS;

	// This list is all this class is about: list of GRM Server Endpoints (Cached). OfferCache keeps list of stale servers
	protected List<String> grmServerList = new ArrayList<String>();
	protected Map<String, Boolean> cachedServers = new HashMap<String, Boolean>();

	// ************************************************************************
	// Reference to Other Components:
	private volatile static GRMEndPointsCache instance;
	//private static final Logger LOGGER = DME2LoggingWrapper.getLoggerWrapper( GRMEndPointsCache.class.getName() );
	private static final Logger logger = LoggerFactory.getLogger( GRMEndPointsCache.class );
	//private static final Configuration config = Configuration.getInstance();
	private DME2Configuration config;

	private GRMEndPointsCache( DME2Configuration configuration ) throws DME2Exception {
		config = configuration;
		cacheFileName = getCacheFileName();
		cacheRefreshIntervalInMS =
				Integer.parseInt( config.getProperty( DME2ParameterNames.CACHE_REFRESH_INTERVAL_MS ) ); // 5 minutes
		cacheRefreshStartDelayInMS =
				Integer.parseInt( config.getProperty( DME2ParameterNames.CACHE_REFRESH_START_DELAY_MS ) ); // 1 seconds
	}

	public static GRMEndPointsCache getInstance(DME2Configuration configuration) throws DME2Exception {
		// https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
		GRMEndPointsCache result = instance;
		if ( result == null ) {
			synchronized (GRMEndPointsCache.class) {
				result = instance;
				if ( result == null ) {
					instance = result = new GRMEndPointsCache( configuration );
				}
			}
		}
		return result;
	}

	protected void addEndpointURL( String address ) {
		// second condition is temporary hack to avoid adding servers from other addresses
		// && address.startsWith(protocolAddress)
		if ( !cachedServers.containsKey( address ) ) {
			grmServerList.add( address );
			cachedServers.put( address, true );
		}
	}

	public void clear() {
		grmServerList = new ArrayList<String>();
		cachedServers = new HashMap<String, Boolean>();
	}

	/**
	 * add all servers in newAddressList to existing GRM server list, avoid duplicates
	 *
	 * @param newAddressList
	 */
	public void addAllAddressList( List<String> newAddressList ) {
		if ( grmServerList == null ) { // error that should not happen
			return;
		}
		for ( String address : newAddressList ) {
			addEndpointURL( address );
		}
	}

	public void loadFromFile() throws IOException {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader( new FileReader( cacheFileName ) );
			String address;
			while ( ( address = bufferedReader.readLine() ) != null ) {
				//LogUtil.getINSTANCE().report(LOGGER, Level.CONFIG, LogMessage.DEBUG_MESSAGE, "adding follwing GRM seed from cache persistence storage: " + address);
				logger.debug( null, "loadFromFile", "adding follwing GRM seed from cache persistence storage: {}", address );
				addEndpointURL( address );
			}
		} finally {
			if ( bufferedReader != null ) {
				bufferedReader.close();
			}
		}
	}

	public void writeToFile() throws IOException {
		BufferedWriter bufferedWriter = null;
		File file = new File( cacheFileName );
		logger.debug( null, "writeToFile", "Got cache file {}", cacheFileName );
		if ( file.getParentFile() != null && !file.getParentFile().exists() ) {
			file.getParentFile().mkdirs();
		}
		if ( !file.exists() ) {
			file.createNewFile();
		}

		try {
			bufferedWriter = new BufferedWriter( new FileWriter( cacheFileName, false ) );
			for ( String url : grmServerList ) {
				bufferedWriter.write( url );
				bufferedWriter.newLine();
			}
		} finally {
			if ( bufferedWriter != null ) {
				bufferedWriter.close();
			}
		}
	}

	public void refreshServerList( List<String> newServerList ) throws IOException {
		synchronized ( grmServerList ) {
			grmServerList = newServerList;
		}
		writeToFile();
	}

	/**
	 * we are doing a clone, because GRMServiceAccessor.invoke() is calling iter.remove() on this while it looks like
	 * unnecessary to call remove, better to not remove and not clone!
	 *
	 * @return list of GRMEndPoint
	 */
	@Override
	public List<String> getGRMEndpoints() {
		// when ever the bug is resolved remove above, and uncomment below
		return new ArrayList<String>( grmServerList );
	}

	public String getCacheFileName() {
		logger.debug( null, "getCacheFileName", "Config Value: {}", config.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE ));
		cacheFileName = config.getProperty( DME2ParameterNames.GRM_SERVER_CACHE_FILE, DME2ParameterNames.GRM_SERVER_CACHE_FILE_DEFAULT );
		return cacheFileName;
	}

	public void close() {
		instance = null;
	}
}
