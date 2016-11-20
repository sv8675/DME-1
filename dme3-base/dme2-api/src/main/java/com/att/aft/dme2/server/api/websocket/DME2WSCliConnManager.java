package com.att.aft.dme2.server.api.websocket;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.http.DME2Exchange;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.request.DmeUniformResource.DmeUrlType;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/**
 * Creates the Jetty websocket connection 
 * Handles the endpoint iteration and the sequencing 
 * Preferred Route Offers are chosen
 * Validates endpoints and marks them as stale appropriately
 * 
 *
 */
public class DME2WSCliConnManager {
	
	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger( DME2WSCliConnManager.class );

	/** The manager over this mgr */
	private final DME2Manager manager;
	
	private DmeUniformResource uniformResource;
	
	private DME2BaseEndpointIterator iterator;
	
	public final static  String SERVICE ="service";

	private static final String AFT_DME2_0702 = null;
	
	private DME2EndpointReference currentEndpointReference;
	
	private boolean attemptingRetry;
	
	private boolean isEndpointResolved;
	
	private final String lookupURI;
	
 	private final StringBuffer routeOfferBuffer = new StringBuffer();
 	 
	private boolean tRACEON = false;
	
	private String subContext;
	/** context to append with resolved host:port */
	private String context;
	
	/** Boolean used to know whether endpoints in local host/container are preferred **/
	private boolean preferLocal = false;
	
	private String hostFromArgs = null;
	
	private static DME2Configuration config = new DME2Configuration();
	
	/** The actual request url (currentEndpoint+subContext+queryParms) */
	private String currentFinalUrl = null;
	
	/** the current complete url */
 	private String url = null;
 	
	/** The time that the actual send to the last endpoint started */
	private long sendStart;

	private String hostname = null;
	
	private int maxTextMessageSize = 32768;
	
	private int maxBinaryMessageSize = 32768;
	
	private DME2CliWebSocket dme2Socket = null;
	
	private long maxConnectionTimeout = 5000; //msecs
	
	private long maxConnIdleTime = 5000; // msecs
	
	private int maxConnDuration = 60; //secs
	
	/** Tracking ID for the request */
	private String trackingID;	
 
 	/** preferredRouteOffer if set by requestHandlers */
 	private String preferredRouteOffer;
 	
 	private boolean handleFailover = true;
 	
	private String queryParams = null;
	
	private boolean logStats = true;
	
	private boolean isUserClose = false;
	
	private int maxConnAttempts = 1;

	private boolean enableTraceRoute = false;
	
	private Properties iteratorProps = null;

	private Object lock = new Object();
	
	private final String DME2_TRACKING_ID = "dme2_tracking_id";
	

	public DME2WSCliConnManager(DME2Manager mgr, DmeUniformResource dme2Uri, DME2WSCliMessageHandler handler) throws DME2Exception {
		this.manager = mgr;
		this.uniformResource = dme2Uri;
		hostFromArgs = config.getProperty(DME2Constants.AFT_DME2_CONTAINER_HOST_KEY);
		this.dme2Socket = new DME2CliWebSocket(handler, manager);
		this.handleFailover = config.getBoolean(DME2Constants.DME2_WS_HANDLE_FAILOVER);
		this.logStats = config.getBoolean(DME2Constants.AFT_DME2_WEBSOCKET_METRICS_COLLECTION);
		this.dme2Socket.setLogStats( logStats );
		this.maxConnAttempts = config.getInt(DME2Constants.AFT_DME2_WS_MAX_RETRY_COUNT);
		this.enableTraceRoute = config.getBoolean(DME2Constants.AFT_DME2_WS_ENABLE_TRACE_ROUTE);
		this.maxTextMessageSize = config.getInt(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_MAX_TEXT_MESSAGE_SIZE);
		this.maxBinaryMessageSize = config.getInt(DME2Constants.AFT_DME2_SERVER_WEBSOCKET_MAX_BINARY_MESSAGE_SIZE);
	
		try {
			hostname = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			logger.debug( null, "ctor", "Exception",e);
			/*Ignoring Exception*/
		}
		
		try {
			this.lookupURI = stripQueryParamsFromURIString( dme2Uri.getUrl().toURI().toString() );
		} catch (URISyntaxException e) {
			ErrorContext ec = new ErrorContext();
			ec.add(SERVICE, uniformResource.getUrl().toString());
			
			DME2Exception ex = new DME2Exception("AFT-DME2-3001", ec);
			logger.error( null, "ctor", "AFT-DME2-3001", ec, ex );
			throw ex;
		}
	}
	
	public void connect() throws DME2Exception {
        // initialize fields for logging context information
		initLoggingContext();

		Properties iteratorProps = new Properties();	
		if(preferredRouteOffer != null) {
			iteratorProps.put(DME2Constants.DME2_PREFERRED_ROUTEOFFER, preferredRouteOffer);
		}
		
		//TODO this get fixed during merging
		//iterator = (EndpointIterator) EndpointIteratorFactory.getBaseIterator(uniformResource.toString(), iteratorProps, null, manager);
		
 		/*Checking if the Iterator has next. If it doesn't, it means that no endpoints were resolved*/		
		if(!iterator.hasNext()) {
			/*Throw Exception*/
			ErrorContext ec = new ErrorContext();
			ec.add(SERVICE, lookupURI);
			if(iterator.getRouteOffersTried() != null) {
				ec.add("routeOffersTried", iterator.getRouteOffersTried());
			}
			
			DME2Exception e = new DME2Exception(AFT_DME2_0702, ec);
			logger.error( null, "connect", DME2Exchange.AFT_DME2_0702, ec, e );
			throw e;
		}
		
		/*Use the enpoints returned in the DME2EndpointIterator to service the client request. 
		 * The iterator has already organized and grouped the endpoints by RouteOffer, sequence, and distance.*/
		
		while(iterator.hasNext()) {
			resolveFinalRequestURLFromIterator();
			
			if(!isEndpointResolved) {
				/*Throw Exception*/
				ErrorContext ec = new ErrorContext();
				ec.add(SERVICE, lookupURI);
				ec.add("routeOffersTried", routeOfferBuffer.toString());
				
				DME2Exception e = new DME2Exception(AFT_DME2_0702, ec);
				logger.error( null, "connect", DME2Exchange.AFT_DME2_0702, ec, e );
				throw e;
			}
			
			try {
				logger.debug( null, "connect", "CREATE_WS_CONNECTION", this.getURL());
				this.sendStart = System.currentTimeMillis();

				WebSocketClient wsClient = manager.getWsClientFactory().getWsClientFactory(); //.newWebSocketClient();        
				wsClient.setMaxTextMessageBufferSize(maxTextMessageSize);
				wsClient.setMaxBinaryMessageBufferSize(maxBinaryMessageSize);
		        dme2Socket.setWsClient( wsClient );
		        dme2Socket.setUri( uniformResource );
		        dme2Socket.setEndpoint( currentFinalUrl );
		        dme2Socket.setMaxConnectionIdleTime( (int) maxConnIdleTime );
		        dme2Socket.setTrackingId( trackingID );
		        dme2Socket.setWsConnMgr( this );
		        		        
		        if (isEnableTraceRoute()) {		        	
		        	dme2Socket.getEpTraceRoute().append(DME2CliWebSocket.EP + currentFinalUrl + ";");
		        	if (currentEndpointReference.getRouteOffer().getRouteOffer() != null) {
		        		dme2Socket.getEpTraceRoute().append("routeOffer=" + currentEndpointReference.getRouteOffer().getSearchFilter() + "]");
		        	} else {
		        		dme2Socket.getEpTraceRoute().append("]");
		        	}
		        }

		        // Our socket endpoint (the client side)
		        wsClient.connect( dme2Socket, new URI(currentFinalUrl)).get(getMaxConnectionTimeout(),TimeUnit.MILLISECONDS);
				//wsClient.open(new URI(currentFinalUrl), dme2Socket).get(getMaxConnectionTimeout(),TimeUnit.MILLISECONDS);

		        logger.debug( null, "connect", "WS_CONNECTION_CONVERSATION ENDED" + ";TrackingId=" + this.trackingID + ";URL=" + currentFinalUrl + ";elapsed=" + (System.currentTimeMillis() - sendStart));
				logger.debug( null, "connect", LogMessage.WS_CONVERSATION_CLOSE_MSG, this.trackingID, currentFinalUrl);
			
				/*
				 * Client successfully submitted the request. (This doesn't mean the the entire transaction was successful, just
				 * the submit portion. Response could potentially return in error)
				 */
				return; 
				
			} catch (Exception e) {
				debugIt("WS_CONNECT_EXCEPTION", e.getClass().getCanonicalName() + ";" + e.getMessage());
				logger.debug( null, "connect", LogMessage.WS_CONNECTION_FAIL_MSG, this.trackingID, currentFinalUrl, e.getMessage());
				if (isEnableTraceRoute())
					dme2Socket.getEpTraceRoute().append(DME2CliWebSocket.EP + currentFinalUrl + ":WS_CONNECT_EXCEPTION]");
				iterator.setStale();
			}
							
		} //End iterator.hasNext()
		
		/*If we get here, client.send() did not work. We can check to see if all elements have been exhausted in the Iterator at this point.
		 * If so, throw and exception indicating all elements have been tried, otherwise continue along in the iterator and attempt the next element.*/
		if(iterator.isAllElementsExhausted())
		{
			debugIt("connect", "WS_ENDPOINTS_EXHAUSTED");

			if (this.isLogStats()) {
				/* Post request statistics to Metrics Service */
				HashMap<String, Object> props1 = new HashMap<String, Object>();
				props1.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
				props1.put(DME2Constants.FAULT_EVENT, true);
				props1.put(DME2Constants.QUEUE_NAME, this.lookupURI);
				props1.put(DME2Constants.ELAPSED_TIME, 0);
				props1.put(DME2Constants.MESSAGE_ID, this.trackingID);
				props1.put(DME2Constants.DME2_INTERFACE_PROTOCOL, DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
				props1.put(DME2Constants.DME2_INTERFACE_ROLE, config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
				props1.put(DME2Constants.DME2_INTERFACE_PORT, 0); //TODO - there will not be any endpoitn with port here
				props1.put(DME2Constants.FAULT_EVENT, true);
	
				if (uniformResource.getPartner() != null)
				{
					props1.put(DME2Constants.DME2_REQUEST_PARTNER, this.uniformResource.getPartner());
				}

				logger.debug( null, "connect", "DME2Exchange postFaultEvent {}", props1);
				manager.postStatEvent(props1);
			}
			/*Throw Exception*/
			ErrorContext ec = new ErrorContext();
			ec.add(SERVICE, lookupURI);
			
			DME2Exception e = new DME2Exception("AFT-DME2-0703", ec);
			logger.error( null, "connect", "AFT-DME2-0703", ec, e );
			throw e;

		}
		logger.debug( null, "connect", LogMessage.METHOD_EXIT);
	}
	

	/*Returns the final Endpoint URL from the iterator that will be used to service the request.*/
	private void resolveFinalRequestURLFromIterator() throws DME2Exception
	{
		/*isEndpointResolved = false;
		while(iterator.hasNext())
		{			
			DME2EndpointReference next = iterator.next(); 
			//if the next endpoint reference has a different route offer and a different sequence, log the failover 
			if(currentEndpointReference != null 
					&& next.getRouteOffer() != currentEndpointReference.getRouteOffer() 
					&& next.getSequence() != currentEndpointReference.getSequence()){ 
 
				////LogUtil.getINSTANCE().report(logger, Level.INFO, LogMessage.EXCH_FAILOVER, lookupURI, next.getRouteOffer().getSearchFilter(), currentEndpointReference.getRouteOffer().getSearchFilter()); 
				debugIt(LogMessage.EXCH_FAILOVER.toString(lookupURI, next.getRouteOffer().getSearchFilter(), currentEndpointReference.getRouteOffer().getSearchFilter()));
			} 
 
			//now we use the next one 
			currentEndpointReference = next;

			
			if(attemptingRetry)	{
				
				debugIt("CURRENT_RETRY_ROUTE_OFFER", currentEndpointReference.getRouteOffer() != null ? currentEndpointReference.getRouteOffer().getSearchFilter() : null);
				debugIt("CURRENT_RETRY_SEQUENCE", currentEndpointReference.getSequence());
				debugIt("CURRENT_RETRY_ENDPOINT", currentEndpointReference.getEndpoint().toURLString());
				debugIt("CURRENT_RETRY_DISTANCE_BAND", currentEndpointReference.getDistanceBand().toString());
			} else {
				debugIt("CURRENT_ROUTE_OFFER", currentEndpointReference.getRouteOffer() != null ? currentEndpointReference.getRouteOffer().getSearchFilter() : null);
				debugIt("CURRENT_SEQUENCE", currentEndpointReference.getSequence());
				debugIt("CURRENT_ENDPOINT", currentEndpointReference.getEndpoint().toURLString());
				debugIt("CURRENT_DISTANCE_BAND", currentEndpointReference.getDistanceBand().toString());
			}
					
			Appending value of currently used routeOffer to the routeOfferBuffer which will be used as trace information
			if(currentEndpointReference.getRouteOffer() != null) {
				if(routeOfferBuffer.length() == 0)
				{
					routeOfferBuffer.append(currentEndpointReference.getRouteOffer().getSearchFilter());
				}
				else
				{
					if(!routeOfferBuffer.toString().contains(currentEndpointReference.getRouteOffer().getSearchFilter()))
					{
						routeOfferBuffer.append(",");
						routeOfferBuffer.append(currentEndpointReference.getRouteOffer().getSearchFilter());
					}
				}
			}
			
			
			Check endpoint context path to see if it matches a restful URI pattern if client had 
			 * provided a context path or subContext path, then we would try to match using pattern
			if (this.getContext() != null && this.getDME2URLType() == DME2UrlType.STANDARD) {
				if (!this.matchServletPath(this.getContext(), this.getSubContext(), currentEndpointReference.getEndpoint().getContextPath()))
				{
					continue;
				}
			} else if(this.getDmeUniformResource() != null && this.getDME2URLType() != DME2UrlType.DIRECT) {
				String service = this.getDmeUniformResource().getService();
				if(service != null) {
					if(service.contains("/")) {
						String cp = service.substring(service.indexOf("/"));
						if (!this.matchServletPath(cp, this.getSubContext(), currentEndpointReference.getEndpoint().getContextPath()))
						{
							continue;
						}
						else{
							this.setContext(cp);
						}
					}
				}
			} 

			
			 Check if client preferred to use to use endpoints on the this (local) host/container. 
			if (preferLocal) {
				String epHost = currentEndpointReference.getEndpoint().getHost();
				if (epHost.equalsIgnoreCase(hostFromArgs) || DME2Constants.isHostMyLocalHost(hostFromArgs))
				{
					Replacing original host with the localhost for this endpoint
					currentEndpointReference.getEndpoint().setHost(hostFromArgs);
					
					debugIt("      . LOAD_LOCAL_SEP_OK", currentEndpointReference.getEndpoint().toURLString());
					if (isEnableTraceRoute())
						dme2Socket.getEpTraceRoute().append(DME2CliWebSocket.EP + currentEndpointReference.getEndpoint().toURLString() + ":preferredLocal];");
				}
			} else {
				debugIt("      . LOAD_SEP_OK", currentEndpointReference.getEndpoint().toURLString());	
			}
			
			
			Setting the final URL to use for the client request
			if(this.getDmeUniformResource() != null && this.getDME2URLType() != DME2UrlType.DIRECT) {
				this.setURL(currentEndpointReference.getEndpoint().toURLString(currentEndpointReference.getEndpoint().getPath(), "", this.getQueryParams()));
			} else {
				this.setURL(currentEndpointReference.getEndpoint().toURLString(this.getContext(), this.getSubContext(), this.getQueryParams()));				
			}
			
			isEndpointResolved = true;
			break;
		} 
		
		*/
		
	}

	private void debugIt(String method, String key, String i) {
		if (tRACEON) {
			debugIt(method, key + ":" + i);
		}
		
	}

	private void debugIt(String method, String key, int i) {
		if (tRACEON) {
			debugIt(method, key + ":" + i);
		}
		
	}

	private void debugIt(String method, String message) {
		if (tRACEON && dme2Socket.getConnection() != null) {
      logger.debug( null, method, "WSObjReference: {} DME2 WS trackingId: {} - {}", this.hashCode(), trackingID, message );
		} else if (tRACEON) {
      logger.debug( null, method, "WSObjReference: {} {}", this.hashCode(), message );
		}
	}
	
	public void setContext(String context) {
		this.context = context;
	}

	public String getContext() {
		return context;
	}

	public String getSubContext() {
		return (this.subContext!=null?this.subContext:"");
	}

	public void setSubContext(String subContext) {
		this.subContext = subContext;
	}
	
	/**
	 * 
	 * @param endpointPaths
	 * @return
	 */
	private boolean matchServletPath(String context, String subContext, String endpointPaths)
	{
		String clientURLContext = context != null ? context : "" + "/" + subContext != null ? subContext : "";
		clientURLContext = clientURLContext.replaceAll("//", "/");
		
		String[] contextPaths = endpointPaths.split(",");

		for (int j = 0; j < contextPaths.length; j++)
		{
			String toks[] = contextPaths[j].split("/");
			StringBuffer pathToCompare = new StringBuffer();

			for (int i = 0; i < toks.length; i++)
			{

				if (toks[i].length() > 0)
				{
					if (toks[i].startsWith("{") && toks[i].endsWith("}"))
					{
						pathToCompare.append("/.*");
					}
					else if (toks[i].startsWith("(") && toks[i].endsWith(")"))
					{
						pathToCompare.append("/.*");
					}
					else
					{
						pathToCompare.append("/" + toks[i]);
					}
				}
			}

			if (pathToCompare.length() > 0)
			{
				if (clientURLContext.matches(pathToCompare.toString())){
					return true;
				}
			}
		}
		
		return false;
	}

	public DmeUniformResource getDmeUniformResource() {
		return uniformResource;
	}

	public void setDmeUniformResource(DmeUniformResource uniformResource) {
		this.uniformResource = uniformResource;
	}

	public DmeUrlType getDME2URLType() 
	{
		return uniformResource.getUrlType();
	}
	
	public void setPreferLocal(boolean preferLocal) {
		this.preferLocal = preferLocal;
	}

	public boolean isPreferLocal() {
		return preferLocal;
	}	
	
	public void setURL(String url) 
	{
		if (url.contains("?")) {
			String urlStr = url.substring(0, url.lastIndexOf("?"));
			String queryStr = url.substring(url.lastIndexOf("?"), url.length());
			url = url + queryStr + "&" + DME2_TRACKING_ID + "=" + trackingID;
		} else {
			url = url + "?" + DME2_TRACKING_ID + "=" + trackingID;;
		}
		
		String protocol = url.substring(0, url.lastIndexOf("://") + 3 );
		String newUrl = url.replace(protocol, protocol.toLowerCase());
		
		if (((uniformResource.toString().startsWith("ws://")) || (uniformResource.toString().startsWith("wss://")) ||
				(uniformResource.toString().startsWith("WS://")) || (uniformResource.toString().startsWith("WSS://")) ) &&
				(uniformResource.getUrlType() == DmeUrlType.DIRECT)) {
			String ipProtocol = uniformResource.toString().substring(0, uniformResource.toString().lastIndexOf("://") + 3);
			newUrl = url.replace(protocol, ipProtocol);
		}
		
		this.url = newUrl;
		this.currentFinalUrl = newUrl;
		
		debugIt("WS_SET_URL",this.url);
	}

	private String getURL() {
		return this.currentFinalUrl;
	}


	public int getMaxTextMessageSize() {
		return this.maxTextMessageSize;
	}

	public void setMaxTextMessageSize(int maxTextMessageSize) {
		this.maxTextMessageSize = maxTextMessageSize;
	}

	public int getMaxBinaryMessageSize() {
		return this.maxBinaryMessageSize;
	}

	public void setMaxBinaryMessageSize(int maxBinaryMessageSize) {
		this.maxBinaryMessageSize = maxBinaryMessageSize;
	}

	public long getMaxConnectionTimeout() {
		return maxConnectionTimeout;
	}

	public void setMaxConnectionTimeout(long val) {
		this.maxConnectionTimeout = val;
	}

	public long getMaxConnIdleTime() {
		return maxConnIdleTime;
	}

	public void setMaxConnIdleTime(long maxConnIdleTime) {
		this.maxConnIdleTime = maxConnIdleTime;
	}	
	
	public void close() throws DME2Exception {
		if ( dme2Socket.getConnection() != null)
			dme2Socket.getConnection().close();
	}

	public int getMaxConnDuration() {
		return maxConnDuration;
	}

	public void setMaxConnDuration(int maxConnDuration) {
		this.maxConnDuration = maxConnDuration;
	}
	
	private void initLoggingContext() {
		try {
			String conversationId = UUID.randomUUID().toString();

			trackingID = "WS_ID_" +  conversationId + 
							(this.getStickySelectorKey() == null ? "" : "(stickySelector=" + this.getStickySelectorKey() + ")");
			DME2Constants.setContext(trackingID, null);
		} catch (Exception e) {
			logger.warn( null, "initLoggingContext", LogMessage.EXCH_CTX_FAIL, e );
		}		
	}


	/**
	 * 
	 * @return
	 */
	public String getStickySelectorKey() {
		return uniformResource.getStickySelectorKey();
	}

	public String getPreferredRouteOffer() {
		return preferredRouteOffer;
	}

	public void setPreferredRouteOffer(String preferredRouteOffer) {
		this.preferredRouteOffer = preferredRouteOffer;
	}

	public boolean isHandleFailover() {
		return this.handleFailover;
	}


	public void failoverConnection() throws DME2Exception {
		
		if (isEnableTraceRoute())
			dme2Socket.getEpTraceRoute().append(DME2CliWebSocket.EP + this.currentFinalUrl + ":onException=connection closed" + "];");
		
		iterator.setStale();
		//check if all endpoints have been attempted in the list
		
		//TODO fix this
		//iterator = (EndpointIterator) EndpointIteratorFactory.getBaseIterator(uniformResource.toString(), iteratorProps, null, manager);
		//iterator = (EndpointIterator) EndpointIteratorFactory.getInstance().getIterator(queryData, endpointData, staleKeyPrefix);
		
		//create a new dme2websocket otherwise websocketclient returns the previous closecode if that endpoint
		//is not up when opened
		dme2Socket = createDME2WebSocket( dme2Socket );
       
		while(iterator.hasNext()) {
			//Attempt to send the request.
			try
			{
				resolveFinalRequestURLFromIterator();

				logger.debug( null, "failoverConnection", LogMessage.WS_CONN_FAILOVER, getURL(), this.trackingID);

				DME2Constants.setContext(trackingID, null);

				debugIt("CREATE_WS_CONNECTION", this.getURL());
				this.sendStart = System.currentTimeMillis();

		        dme2Socket.setUri( uniformResource );
		        dme2Socket.setEndpoint( currentFinalUrl );
		        if (isEnableTraceRoute()) {
	        		dme2Socket.getEpTraceRoute().append(DME2CliWebSocket.EP + currentFinalUrl + "; routeOffer=" + currentEndpointReference.getRouteOffer().getSearchFilter() + "]");
		        }
		        // Our socket endpoint (the client side)
		        dme2Socket.getWsClient().connect( dme2Socket, new URI(currentFinalUrl)).get(getMaxConnectionTimeout(),TimeUnit.MILLISECONDS);
				//dme2Socket.getWsClient().open(new URI(currentFinalUrl), dme2Socket).get(getMaxConnectionTimeout(),TimeUnit.MILLISECONDS);
				return; 					
			} catch (Exception e) {
				debugIt("WS_CONNECT_EXCEPTION", e.getClass().getCanonicalName() + ";" + e.getMessage());
				logger.debug( null, "failoverConnection", LogMessage.WS_CONNECTION_FAIL_MSG, this.trackingID, currentFinalUrl, e.getMessage());
				if (isEnableTraceRoute()) {
	        		dme2Socket.getEpTraceRoute().append(DME2CliWebSocket.EP + currentFinalUrl + ":WS_CONNECT_EXCEPTION]");
		        }
			}
		}		
		

		/*Check if all endpoints have been exhausted at this point. */
		if(iterator.isAllElementsExhausted())
		{
			debugIt("failoverConnection","DO_WS_FAILOVER_ENDPOINTS_EXHAUSTED");

			try {
				if (this.isLogStats()) {
					/* Post request statistics to Metrics Service */
					HashMap<String, Object> props = new HashMap<String, Object>();
					props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
					props.put(DME2Constants.FAULT_EVENT, true);
					props.put(DME2Constants.QUEUE_NAME, this.lookupURI);
					props.put(DME2Constants.DME2_INTERFACE_PORT, new URI(currentFinalUrl).getPort() + "");
					props.put(DME2Constants.FAULT_EVENT, true);
					props.put(DME2Constants.ELAPSED_TIME, 0);
					props.put(DME2Constants.MESSAGE_ID, this.trackingID);
					props.put(DME2Constants.DME2_INTERFACE_PROTOCOL, DME2Constants.DME2_WS_INTERFACE_PROTOCOL);
					props.put(DME2Constants.DME2_INTERFACE_ROLE, config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE));
	
					if (uniformResource.getPartner() != null)
					{
						props.put(DME2Constants.DME2_REQUEST_PARTNER, this.uniformResource.getPartner());
					}

					logger.debug( null, "failoverConnection", "DME2Exchange postFaultEvent {}", props);
					manager.postStatEvent(props);
				}
			} catch (Exception e1) {
				ErrorContext ec = new ErrorContext();
				ec.add("Code", "DME2Client.Fault");
				ec.add("extendedMessage", e1.getMessage());

				logger.debug( null, "failoverConnection", "AFT-DME2-5101", ec );
			}	
							

			String endpointsAttempted = dme2Socket.getEpTraceRoute() != null ? this.dme2Socket.getEpTraceRoute().toString() : null;

			ErrorContext ec = new ErrorContext();
			ec.add(SERVICE, lookupURI);
			ec.add("TRACKING_ID", this.trackingID);
			ec.add("URI", lookupURI);
			
			if(endpointsAttempted != null)
			{
				ec.add("endpointsAttempted", endpointsAttempted);
			}

			DME2Exception dme2Exception = new DME2Exception(DME2Constants.DME2_ALL_EP_FAILED_MSGCODE, ec);
			logger.error( null, "failoverConnection", "AFT-DME2-0703", ec, dme2Exception );

			throw dme2Exception;
		}
		logger.debug( null, "failoverConnection", LogMessage.METHOD_EXIT );
	}

	public boolean isFailoverRequired(String replyMessage, int respCode)
	{
		boolean isFailoverRequired = false; /* Value that will be returned */
		if (config.getProperty(DME2Constants.AFT_DME2_FAILOVER_WS_CLOSE_CDS).contains(String.valueOf(respCode))) {
			isFailoverRequired = true;
		}

		return isFailoverRequired;
	}
	
	public boolean isRetryRequired(String replyMessage, int respCode)
	{
		boolean isRetryRequired = false; /* Value that will be returned */
		if (config.getProperty(DME2Constants.AFT_DME2_RETRY_WS_CLOSE_CDS).contains(String.valueOf(respCode))) {
			isRetryRequired = true;
		}

		return isRetryRequired;
	}
	
	public void retryConnection() throws DME2Exception {
		//DO not add trace for retry to avoid too big a trace info due to idletimeouts
		//Attempt to send the request.
		int i = 0;
		
		dme2Socket = createDME2WebSocket( dme2Socket );
		  		
		while (i < maxConnAttempts) {
			try
			{
				debugIt("CREATE_WS_CONNECTION", this.getURL());
				this.sendStart = System.currentTimeMillis();
	
		        dme2Socket.setUri( uniformResource );
		        dme2Socket.setEndpoint( currentFinalUrl );
		       
		        // Our socket endpoint (the client side)
		        dme2Socket.getWsClient().connect( dme2Socket, new URI(currentFinalUrl)).get(getMaxConnectionTimeout(),TimeUnit.MILLISECONDS);
				//dme2Socket.getWsClient().open(new URI(currentFinalUrl), dme2Socket).get(getMaxConnectionTimeout(),TimeUnit.MILLISECONDS);
	
				return; 					
			} catch (Exception e) {
				debugIt("WS_CONNECT_EXCEPTION", e.getClass().getCanonicalName() + ";" + e.getMessage());
				logger.debug( null, "retryConnection", LogMessage.WS_CONNECTION_FAIL_MSG, this.trackingID, currentFinalUrl, e.getMessage() );
				if ((this.isHandleFailover()) && (i == maxConnAttempts - 1)) {
					failoverConnection();
				} else if((!this.isHandleFailover()) && (i == maxConnAttempts - 1)) {	
					ErrorContext ec = new ErrorContext();
					ec.add("Code", "DME2Client.Fault");
					ec.add("extendedMessage", "Retry failed and failover is disabled.");
					ec.add("WS_TRACKING_ID", this.trackingID);
					ec.add("URI", lookupURI);
					ec.add("ERROR_MESSAGE", e.getMessage());
					DME2Exception dme2Exception = new DME2Exception("AFT-DME2-3009", ec);
					logger.error( null, "retryConnection", "AFT-DME2-3009", ec, e);
					throw dme2Exception;
				}
			}
		}
	}

	public String getQueryParams() {
		return (this.queryParams != null ? this.queryParams : "");
	}

	public void setQueryParams(String queryParams) {
		this.queryParams = queryParams;
	}
	
	public void setQueryParams(Map<String,String> mapParams, boolean encode) {
		if(mapParams==null || mapParams.size()==0) {
			this.queryParams="";
			return;
		}
		StringBuffer sb = new StringBuffer(mapParams.size()*2);
		sb.append("?");
		for( Entry<String, String> e: mapParams.entrySet()) {
			try {
				sb.append(e.getKey())
				  .append("=")
				  .append(encode?
						  	URLEncoder.encode((e.getValue()!=null?e.getValue():""),Charset.forName("UTF-8").name())
						  	:(e.getValue()!=null?e.getValue():""))
				  .append("&");
			} catch(UnsupportedEncodingException uee) {
				debugIt("Could not encode parameter:", e  + ";" + uee);
			}
		}
		this.queryParams = sb.toString();
	}
	
	
	private String stripQueryParamsFromURIString(String uriString)
	{
		int indexOfQuery = uriString.indexOf("?");
		
		if (indexOfQuery > 0)
		{
			return uriString.substring(0, indexOfQuery);
		}
		
		return uriString;
	}

	public boolean isLogStats() {
		return this.logStats;
	}

	public boolean isUserClose() {
		return this.isUserClose;
	}

	public void setUserClose(boolean isUserClose) {
		this.isUserClose = isUserClose;
	}

	public boolean isEnableTraceRoute() {
		return this.enableTraceRoute;
	}

	public DME2CliWebSocket getDme2Socket() {
		return dme2Socket;
	}
	
	public Object getLock() {
		return lock;
	}
	
    public String getEpTraceRoute() {
    	if ( dme2Socket != null)
    		return dme2Socket.getEpTraceRoute().toString();
    	return "";
    	
    }
    
    public DME2CliWebSocket createDME2WebSocket(DME2CliWebSocket dme2Socket) {
    	DME2CliWebSocket socket = new DME2CliWebSocket(dme2Socket.getHandler(), manager);
    	socket.getEpTraceRoute().append( dme2Socket.getEpTraceRoute() );
    	socket.setWsClient(dme2Socket.getWsClient());
    	socket.setWsConnMgr( dme2Socket.getWsConnMgr() );
    	socket.setTrackingId(dme2Socket.getTrackingID());
    	socket.setHandler( dme2Socket.getHandler() );
    	socket.setMaxConnectionIdleTime((int)dme2Socket.getMaxConnectionIdleTime());
    	socket.setLogStats( dme2Socket.isLogStats() );
    	return socket;
    }

	public void setHandleFailover(boolean handleFailover) {
		this.handleFailover = handleFailover;
	}
    
    
}
