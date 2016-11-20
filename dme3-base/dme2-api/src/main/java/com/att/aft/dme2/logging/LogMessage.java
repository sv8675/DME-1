package com.att.aft.dme2.logging;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class LogMessage
{
	// must define prior to first invocation of constructor
	private static final Set<LogMessage> allInstances = new HashSet<LogMessage>();
	//public static final LogMessage SKIP_REFRESH_ENDPOINTS = new LogMessage("DME2-0139", "Code=Trace.AbstractCache.refresh.removeKeysContainingGroupRouteOffer;Cached key routeOffer has grouped sequence entries, Skipping refresh endpoints for {}");
	public static final LogMessage ONCOMPLETE = new LogMessage("DME2-0001", "Continuation onComplete invoked for correlationID: {}");
	public static final LogMessage ON_TIMEOUT = new LogMessage("DME2-0002", "Continuation onTimeout invoked for correlationID: {}");
	public static final LogMessage ON_TIMEOUT_EXCEPTION = new LogMessage("DME2-0003", "Continuation onTimeout received Exception for correlationID: {}");
	public static final LogMessage PROXY_DESTROYED = new LogMessage("DME2-0004", "{} associated with DME2Manager [{}] destroyed.");
	public static final LogMessage ERROR_UNPUBLISHING = new LogMessage("DME2-0005", "OnShutdown: Error unpublishing {}");
	public static final LogMessage UNPUBLISHING_FILE = new LogMessage("DME2-0006", "Unpublishing from file {}");
	public static final LogMessage ERROR_RENEWING_ALL = new LogMessage("DME2-0007", "Code=Exception.DME2EndpointRegistryGRM.renewAllLeases;Exception={}");
	public static final LogMessage ERROR_REFRESHING = new LogMessage("DME2-0008", "Code=Exception.DME2EndpointRegistryGRM.refreshCachedRouteInfo;Exception={}");
	public static final LogMessage ERROR_REFRESH_ALL = new LogMessage("DME2-0009", "Code=Exception.DME2EndpointRegistryGRM.refreshAllCachedEndpoints;Exception={}");
	public static final LogMessage REMOVE_UNUSED = new LogMessage("DME2-0010", "Code=Process.RemoveUnusedEndpoints;DME2Endpoint being removed because is has been marked as unused: {} Endpoints are marked as unused after {} ms.");
	public static final LogMessage ENTER_CODEPOINT = new LogMessage("DME2-0011", "Code=Trace.{}; Entering");
	public static final LogMessage EXIT_CODEPOINT_TIME = new LogMessage("DME2-0012", "Code=Trace.{}; Exiting; elapsed {} ms");
	public static final LogMessage REFRESH_DEFERRED = new LogMessage("DME2-0013", "Code=DMEEndpointRegistryGRM.refreshCachedEndpoint; Refresh for [{}] being defered because returned list was empty.");
	public static final LogMessage CACHED_ENDPOINTS = new LogMessage("DME2-0014", "Code=DMEEndpointRegistryGRM.find; Cached endpoint list for {}; Elapsed={};Endpoint list Size={}");
	public static final LogMessage CACHED_STALE = new LogMessage("DME2-0015", "Code=DME2EndpointRegistryGRM.hasStaleEndpoints; Endpoints cached for service {} are stale: {}");
	public static final LogMessage CACHED_ENDPOINTS_FETCH = new LogMessage("DME2-0016", "Code=DMEEndpointRegistryGRM.fetchEndpoints; Cached endpoint list for {}; Elapsed={};Endpoint list Size={}");
	public static final LogMessage REMOVING_EMPTY = new LogMessage("DME2-0017", "Code=DMEEndpointRegistryGRM.fetchEndpoints; Cached endpoint list for {}; Elapsed={};Endpoint list Size={}");
	public static final LogMessage CACHED_PATH = new LogMessage("DME2-0018", "Code=DMEEndpointRegistryGRM.getRouteInfo; Cached routeInfo for path={}");
	public static final LogMessage EXIT_CODEPOINT = new LogMessage("DME2-0019", "Code=Trace.{}; Exiting");
	public static final LogMessage RENEW_LEASE = new LogMessage("DME2-0020", "Code=Trace.DME2EndpointRegistryGRM.lease; Lease for [{}@{}:{}] was renewed for {} ms");
	public static final LogMessage PUBLISH_OVERRIDE = new LogMessage("DME2-0021", "Code=Trace.DME2EndpointRegistryGRM.publish; Overriding env with value from JVM args;lrmEnv={};env={}");
	public static final LogMessage PUBLISH_ENDPOINT = new LogMessage("DME2-0022", "Code=Trace.DME2EndpointRegistryGRM.publish; Published service endpoint {}:{}");
	public static final LogMessage PUBLISH_LEASE = new LogMessage("DME2-0023", "Code=Trace.DME2EndpointRegistryGRM.publish.lease; Published service endpoint {}:{}");
	public static final LogMessage REFRESH_ENDPOINTS = new LogMessage("DME2-0024", "Code=Trace.DME2EndpointRegistryGRM.refreshCachedDME2Endpoints; Refresh cached endpoints at {}");
	public static final LogMessage UPDATE_ENDPOINTS = new LogMessage("DME2-0025", "Code=Trace.DME2EndpointRegistryGRM.refreshCachedDME2Endpoints;Updating endpoints for {}");
	public static final LogMessage REFRESH_LOOKUP = new LogMessage("DME2-0026", "Refreshed cached endpoints for lookup [{}]");
	public static final LogMessage REFRESH_FAILED = new LogMessage("DME2-0027", "Refresh of cached endpoints for [{}] failed");
	public static final LogMessage CONFIG_ERROR = new LogMessage("DME2-0028", "Code=Trace.DME2Constants; Failure in parsing config;Error={}");
	public static final LogMessage REFRESH_SERVICE = new LogMessage("DME2-0029", "RouteInfo refreshed for service [{}]");
	public static final LogMessage REFRESH_SVC_FAILED = new LogMessage("DME2-0030", "RouteInfo refresh failed for service [{}] because route info table was not found or available from GRM");
	public static final LogMessage RENEW_ALL_START = new LogMessage("DME2-0031", "Renew all lease initialized for {} endpoints");
	public static final LogMessage RENEW_ENDPOINT = new LogMessage("DME2-0032", "Lease refreshed for endpoint [{}]");
	public static final LogMessage RENEW_ENDPT_FAIL = new LogMessage("DME2-0033", "Lease refresh for endpoint [{}] failed");
	public static final LogMessage RENEW_ALL_END = new LogMessage("DME2-0034", "Lease refreshed for {} endpoints in {} ms");
	public static final LogMessage UNPUBLISH_IGNORABLE = new LogMessage("DME2-0035", "Ignorable exception occured during unpublish of endpoints");
	public static final LogMessage UNPUBLISH_ENTER = new LogMessage("DME2-0036", "Code=Trace.DME2EndpointRegistryGRM.unpublish; Entering");
	public static final LogMessage UNPUBLISH_ENV = new LogMessage("DME2-0037", "Code=Trace.DME2EndpointRegistryGRM.unpublish; Overriding env with value from JVM args;lrmEnv={};env={}");
	public static final LogMessage UNPUBLISHED = new LogMessage("DME2-0038", "Code=Trace.DME2EndpointRegistryGRM.unpublish; Unpublished service endpoint {}:{}");
	public static final LogMessage UNPUBLISH_EXIT = new LogMessage("DME2-0039", "Code=Trace.DME2EndpointRegistryGRM.unpublish; Exiting");
	public static final LogMessage FORCE_REFRESH_FAILED = new LogMessage("DME2-0040", "Forced refresh of cached DME2 route information failed with error {}");
	public static final LogMessage FILTER_FAILED = new LogMessage("DME2-0041", "DME2MetricsFilter doFilter exception for serviceName={}");
	public static final LogMessage Q_STATS_FAILED = new LogMessage("DME2-0042", "Code=Exception.DME2QueueStats;Exception={}");
	public static final LogMessage STATS_EVENT_FAILED = new LogMessage("DME2-0043", "Failed to execute an EventProcessor event: {}");
	public static final LogMessage METHOD_ENTER = new LogMessage("DME2-0044", "Entering");
	public static final LogMessage METHOD_EXIT = new LogMessage("DME2-0045", "Exiting");
	public static final LogMessage EVENT_EXCEPTION = new LogMessage("DME2-0046", "EventProcessor uncaught exception occured: {}");
	public static final LogMessage IGNORE_STATS = new LogMessage("DME2-0047", "EventProcessor ignoring stats since service name is in ignore list serviceName={};IgnoreList={}");
	public static final LogMessage PARSE_FAILED_IGNORE = new LogMessage("DME2-0048", "Ignorable exception occured parsing fault response from GRM: {}");
	public static final LogMessage PARSE_IO_EX = new LogMessage("DME2-0049", "Non-fatal IOException closing output stream to GRM: {}");
	public static final LogMessage SERVER_START = new LogMessage("DME2-0050", "Started DME2 Server, Listening at {}");
	public static final LogMessage ADD_METRICS_FILTER = new LogMessage("DME2-0051", "Code=Trace.DME2ServiceHolder.start; Adding DME2MetricsFilter to url {}");
	public static final LogMessage EXCH_ENDPT_FAIL = new LogMessage("DME2-0052", "Code=Trace.DME2Exchange.doTry; Attempt to call [{}] failed with exception [{}] and will be marked stale.  This is informational as other endpoints will be attempted.");
	public static final LogMessage EXCH_RETRY = new LogMessage("DME2-0053", "Code=Trace.DME2Exchange.doTry; Retry attempt with URI [{}] being attempted");
	public static final LogMessage EXCH_AUDIT_RECEIVE = new LogMessage("DME2-0054", "Code=Exchange.Request.Receive.Success;RequestURI={} received successfully and ready to be dispatched; payload size={}");
	public static final LogMessage EXCH_NOTFOUND = new LogMessage("DME2-0055", "Code=Client.preferredRouteOffer.NotFound;preferredRouteOfferSetByRequestHandler={}");
	public static final LogMessage EXCH_FAILOVER = new LogMessage("DME2-0056", "Code=Client.Send;Service={}; Failing over to route offer [{}] from [{}]");
	public static final LogMessage EXCH_SEND_URL = new LogMessage("DME2-0057", "Code=Client.Send;ServerURL={};Timeout={};");
	public static final LogMessage EXCH_OFFER_RESTORE = new LogMessage("DME2-0058", "Code=Client.Send;Service={}; Route offer [{}] was previously marked as being unavailable but is now back to normal");
	public static final LogMessage EXCH_SEND_FAIL = new LogMessage("DME2-0059", "Code=Client.Send.Exception;ServerURL={}; Exception={}");
	public static final LogMessage EXCH_CTX_FAIL = new LogMessage("DME2-0060", "Code=Exception.DME2Exchange.execute;LoggingContext Failed; Error={}");
	public static final LogMessage EXCH_STATUS = new LogMessage("DME2-0061", "DME2Exchange.onResponseComplete; onResponseComplete: status={} for URI {}");
	public static final LogMessage EXCH_RECEIVE_HANDLERS = new LogMessage("DME2-0062", "Code=Client.Receive.Success;ServerURL={};ResponseStatus={};EndpointElapsedMs={};AllEndpointAttemptsElapsedMs={};" +
 "preferredRouteOfferSet={};preferredVersionSet={};RequestHandlersElapsedMs={};ReplyHandlersElapsedMs={};ResponseSize={}");
	public static final LogMessage EXCH_RECEIVE = new LogMessage("DME2-0063", "Code=Client.Receive.Success;ServerURL={};ResponseStatus={};EndpointElapsedMs={};AllEndpointAttemptsElapsedMs={};ResponseSize={}");
	public static final LogMessage EXCH_RECEIVE_EMPTY = new LogMessage("DME2-0064", "Code=Exchange.Request.Receive.Success;RequestURI={} received successfully and ready to be dispatched; payload size={}");
	public static final LogMessage EXCH_FAILOVER_SKIP = new LogMessage("DME2-0065", "Code=Client.Send;Service={}; Skipping failover to route offer [{}] from [{}] because no endpoints were found.");
	public static final LogMessage EXCH_READ_HANDLER_FAIL = new LogMessage("DME2-0066", "Code=Trace.DME2Exchange.{}; Exception reading handlers {}");
	public static final LogMessage EXCH_INVOKE_HANDLER = new LogMessage("DME2-0067", "DME2Exchange.invoke {}; handler={};elapsedTimeInMS={};");
	public static final LogMessage EXCH_INVOKE_FAIL = new LogMessage("DME2-0068", "Executing handler {} failed for handler {};Error={}");
	public static final LogMessage EXCH_MOVE_STALE = new LogMessage("DME2-0069", "Code=Trace.DME2Exchange.loadEndpoints; Moving locally stale endpoint [{}] to untried endpoints list before beginning");
	public static final LogMessage EXCH_ALL_STALE = new LogMessage("DME2-0070", "Code=Trace.DME2Exchange.loadEndpoints; Offer [{}] has all endpoints marked stale");
	public static final LogMessage EXCH_NONE = new LogMessage("DME2-0071", "Code=Trace.DME2Exchange.loadEndpoints; Offer [{}] has no endpoints currently registered");
	public static final LogMessage EXCH_LOAD_FAIL = new LogMessage("DME2-0072", "Code=Client.Send; Warning, exception loading endpoints for offer [{}]");
	public static final LogMessage EXCH_ON_EXCEPTION = new LogMessage("DME2-0073", "Code=Trace.DME2Exchange.onException; onException for {}");
	public static final LogMessage EXCH_ON_EXPIRE = new LogMessage("DME2-0074", "Code=Trace.DME2Exchange.onExpire; onExpire for {}");
	public static final LogMessage EXCH_RESET = new LogMessage("DME2-0075", "Code=Trace.DME2Exchange.resetEndpointPointer; Endpoint {} for retry");
	public static final LogMessage EXCH_RCV_HEADER = new LogMessage("DME2-0076", "Code=Trace.DME2Exchange.onResponseHeader; OnResponseHeader received header {}={} from {}");
	public static final LogMessage EXCH_RETRY_FAIL = new LogMessage("DME2-0077", "Code=DME2ExchangeRetry.run; Exception in retryExchange thread");
	public static final LogMessage SERVER_PARAMS = new LogMessage("DME2-0078", "Starting DME2 Server\n" +
 "Connection Idle Time: {}\n" +
 "Request Dispatcher Core Thread Pool Size: {}\n" +
 "Request Dispatcher Max Thread Pool Size: {}\n" +
 "Request Dispatcher Max Queue Size: {}\n" +
 "Thread Idle Time: {}\n" +
 "Socket Acceptor Threads: {}\n" +
 "Request Buffer Size: {}\n" +
 "Response Buffer Size: {}\n" +
 "Use Direct Buffers: {}\n" +
 "Reuse Address: {}\n" +
 "Max Request Post Size: {}" +
 "Max Request Header Size: {}");
	public static final LogMessage SVC_STATS_FAIL = new LogMessage("DME2-0079", "Unxpected error compiling service stats");
	public static final LogMessage STALENESS_EXPIRED = new LogMessage("DME2-0080", "Code=Trace.DME2Exchange.resetEndpointPointer; Endpoint staleness expired on {}");
	public static final LogMessage VERSION = new LogMessage("DME2-0081", "DME2 Version={}");
	public static final LogMessage VERSION_FAIL = new LogMessage("DME2-0082", "DME2 error loading Version class. Verify whether com.att.aft.dme2.Version class is in classpath");
	public static final LogMessage ERRORTABLE_MISSING = new LogMessage("DME2-0083", "WARNING: DME2 could not locate its errorTable at classpath logical path: com/att/aft/dme2/api/errorTable.properties. " +
 "This means that all errors and exceptions output by DME2 will only include an error code with no meaningful human-readable text. " +
 "Validate that you have all required DME2 jars on the classpath and that the jars have not been altered in any way.");
	public static final LogMessage LOGCONFIG_FAIL = new LogMessage("DME2-0084", "DME2Manager class initialization had a transient failure: {}; some logging/trace settings may not be set as expected.");
	public static final LogMessage PUBLISH_FILE = new LogMessage("DME2-0085", "Publishing to file {}");
	public static final LogMessage DEBUG_MESSAGE = new LogMessage("DME2-0086", "{}");
	public static final LogMessage SERVLET_PARAM_MISSING = new LogMessage("DME2-0087", "{} servlet parameter not set");
	public static final LogMessage SERVLET_RECV = new LogMessage("DME2-0088", "Got a request from [{}]");
	public static final LogMessage SEC_HANDLE_FAIL = new LogMessage("DME2-0089", "DME2DHandler1::handleMessage()");
	public static final LogMessage SERVER_CALLBACK = new LogMessage("DME2-0090", "got: {}");
	public static final LogMessage REPORT_ERROR = new LogMessage("DME2-0091", "{}");
	public static final LogMessage GRM_RETHROW = new LogMessage("DME2-0092", "GRM accessor rethrowing as {}: {}");
	public static final LogMessage GRM_OP_FAIL = new LogMessage("DME2-0093", "Error - {} operation returned result code {} for ServiceEndPoint: {}:{}|{}");
	public static final LogMessage GRM_INVOKE = new LogMessage("DME2-0094", "Attempting to invoke GRM Service with URL: {}");
	public static final LogMessage GRM_IGNORABLE = new LogMessage("DME2-0095", "Ignorable exception occured parsing fault response from GRM: {}");
	public static final LogMessage GRM_VERSION_FAIL = new LogMessage("DME2-0096", "WARNING: AFT Discovery Client version in use {}.{} but needs to be atleast 5.1.3 or higher");
	public static final LogMessage COLLECTOR_PROXY_INIT_FAIL = new LogMessage("DME2-0097", "failed to initialize collector, statistics will not be collected");
	public static final LogMessage THREAD_INTERRUPT = new LogMessage("DME2-0098", "interrupted while {}");
	public static final LogMessage COLLECTOR_BUILD_PATH_FAIL = new LogMessage("DME2-0099", "failed to build sample collector from path: {}");
	public static final LogMessage JMX_OP_FAIL = new LogMessage("DME2-0100", "JMX request failed: {}");
	public static final LogMessage JMX_REGISTER = new LogMessage("DME2-0101", "registering JMX component: {} = {}");
	public static final LogMessage NETWORK_HOSTID_FAIL = new LogMessage("DME2-0103", "failed to get hostname, will be reported as <unknown-host>");
	public static final LogMessage NETWORK_ID_INIT = new LogMessage("DME2-0104", "initialized id={}");
	public static final LogMessage NETWORK_RESET = new LogMessage("DME2-0105", "resetting client and server lists");
	public static final LogMessage NETWORK_ADD_CLIENT = new LogMessage("DME2-0106", "adding client: {}");
	public static final LogMessage NETWORK_ADD_SERVER = new LogMessage("DME2-0107", "adding server: uri={} offer={} address={}");
	public static final LogMessage NETWORK_ADD_SERVER_FAIL = new LogMessage("DME2-0108", "failed to create service description: uri={} offer={} address={}");
	public static final LogMessage EXCH_HANDLER_FAIL = new LogMessage("DME2-0109", "exception in exchange {} handler: {}");
	public static final LogMessage NON_FAILOVER_SC_OVERRIDE = new LogMessage("DME2-0110", "DME2 Non-Failover status code override={}");
	public static final LogMessage ERROR_PUBLISHING = new LogMessage("DME2-0111", "Error occured while attempting to publish service.");
	public static final LogMessage SEP_FAILOVER =  new LogMessage("DME2-0112", "Code=Client.Send;Service={}; Failing over from endpoint [{}] for route offer {}. ResponseCode [{}]. Exception [{}].");
	public static final LogMessage SEP_FAILBACK =  new LogMessage("DME2-0113", "Code=Client.Send; Failing back endpoint(s) [{}]");
	public static final LogMessage WS_CONNECTION_RECEIVE_MSG = new LogMessage("DME2-0114", "WebSocket Message Received over connection with trackingId:{}; uri={}; endpoint={}; Message length:{}");
	public static final LogMessage WS_CONNECTION_SEND_MSG = new LogMessage("DME2-0115", "Attempting to send webSocket Message over connection with trackingId:{}; uri={}; endpoint={}; Message length:{}");
	public static final LogMessage WS_CONNECTION_OPEN_MSG = new LogMessage("DME2-0116", "WebSocket connection open with trackingId:{}; uri={}; endpoint={}");
	public static final LogMessage WS_CONNECTION_CLOSE_MSG = new LogMessage("DME2-0117", "WebSocket connection closed with trackingId:{}; closeCode:{}; uri={}; endpoint={}");
	public static final LogMessage WS_CONVERSATION_CLOSE_MSG = new LogMessage("DME2-0118", "WebSocket conversation ended with trackingId:{}; uri={}; endpoint={}");
	public static final LogMessage WS_CONNECTION_FAIL_MSG = new LogMessage("DME2-0119", "WebSocket connection failed with trackingId:{}; uri={}; endpoint={}; errorMessage={};");
	public static final LogMessage WS_CONN_FAILOVER = new LogMessage("DME2-0120", "Code=Trace.DME2WSConnManager.getnewConnection; Retry attempt with URI [{}]; trackingId={} being attempted");
	public static final LogMessage WS_CLI_HANDLER_EXCEPTION = new LogMessage("DME2-0121", "Code=Trace.DME2WSSocket; Handler returned uncaught exception in method:{}; URI [{}]; endpoint={}; trackingId={}; errorMessage={};");
	public static final LogMessage WS_RETRY_FAIL = new LogMessage("DME2-0122", "Code=DME2WsConnectionRetry.run; Exception in websocket retry thread");
    public static final LogMessage SERVER_STOP_FAIL = new LogMessage("DME2-0123", "Code=DME2Manager.stop; Exception during server stop. ErrorMessage={}");


	public static final LogMessage WS_SERVER_CONNECTION_OPEN_MSG = new LogMessage("DME2-0124", "WebSocket server connection open ");
	public static final LogMessage WS_SERVER_CONNECTION_SEND_MSG = new LogMessage("DME2-0125", "WebSocket server connection onMessage");
	public static final LogMessage WS_SERVER_CONNECTION_CLOSE_MSG = new LogMessage("DME2-0126", "WebSocket server connection closed ");
	public static final LogMessage WS_SERVER_CONNECTION_PARAMETERS = new LogMessage("DME2-0136", "WebSocket server connection parameters ");
	public static final LogMessage WS_SERVER_GRM_HEALTHCHECK_EXCEPTION = new LogMessage("DME2-0137", "Websocket server, GRMHealthCheck exception ");
	
		
	public static final LogMessage WS_SERVER_WEBSOCKET_HANDLER_EXCEPTION = new LogMessage("DME2-0127", "Unhandled application handler exception");
	public static final LogMessage WS_SERVER_WEBSOCKET_METRICS_COLLECTION_EXCEPTION = new LogMessage("DME2-0128", "DME2Websocket Metrics Collection exception, This would not interrupt the functionality of application processing, but review exception message and please report this to support.");
	public static final LogMessage WS_SERVER_WEBSOCKET_ON_MESSAGE_EXCEPTION = new LogMessage("DME2-0129", "DME2Websocket  onMessage exception");
	public static final LogMessage WS_SERVER_WEBSOCKET_ON_MESSAGE_BINARY_EXCEPTION = new LogMessage("DME2-0130", "DME2Websocket  onMessage binary exception");
	public static final LogMessage WS_SERVER_WEBSOCKET_ON_CLOSE_EXCEPTION = new LogMessage("DME2-0131", "DME2Websocket onclose exception");
	public static final LogMessage WS_SERVER_WEBSOCKET_ON_OPEN_EXCEPTION = new LogMessage("DME2-0132", "DME2Websocket handler onOpen exception");
			
	public static final LogMessage WS_SERVER_WEBSOCKET_HEALTHCHECK_EXCEPTION = new LogMessage("DME2-0133", "DME2Websocket , GRMHealthCheck wrong cookie exception or GRMHealthCheckServerWebSocket/DME2ServerWebSocket creation exception");
	public static final LogMessage WS_SERVER_WEBSOCKET_HANDLER_INSTANTIATION_EXCEPTION = new LogMessage("DME2-0134", "DME2Websocket handler instantiation exception");
	public static final LogMessage WS_CONN_RETRY = new LogMessage("DME2-0135", "Code=Trace.DME2WSConnManager.retryConnection; Retry attempt with URI [{}]; trackingId={}; closeCode={}; endpoint={} being attempted");
  public static final LogMessage SERVER_STOP_WARN =
      new LogMessage( "DME2-0138", "Code=DME2Server.stop; Method called when the server was not running." );
  
  public static final LogMessage CONFIGURATION_ERROR = new LogMessage( "DME2-0200", "Code=DME2Configuration.int; Failed to initialize config manager" );

  public static final LogMessage THROTTLE_FILTER_FAILED = new LogMessage("DME2-0140", "DME2ThrottleFilter doFilter exception for serviceName={} and parterName={}");

	public static final LogMessage SKIP_REFRESH_ENDPOINTS = new LogMessage("DME2-0139", "Code=Trace.DME2EndpointRegistryGRM.refreshCachedDME2Endpoints;Cached key routeOffer has grouped sequence entries, Skipping refresh endpoints for {}");
	public static final LogMessage ERROR_REFRESH_TIMEZONE = new LogMessage("DME2-0141", "Code=Trace.DME2Manager.refreshTimezone:{}");
  public static final LogMessage JVM_REGISTER = new LogMessage("DME2-0142", "Code=Trace.{}; Registered JVM Instances. Elapsed Time={} ms.");
	public static final LogMessage ERROR_RENEWING_JVM = new LogMessage("DME2-0143", "Code=Exception.DME2GRMJVMRegistration.scheduleJVMRenewLease;Exception={}");
	public static final LogMessage JVM_FIND = new LogMessage("DME2-0144", "Code=Trace.{}; Found {} JVM Instances. Elapsed Time={} ms.");
	public static final LogMessage ERROR_DEREGISTERING_JVM = new LogMessage("DME2-0145", "Code=Exception.DME2GRMJVMRegistration.DME2DeregisterJVMThread;Exception={}");
	public static final LogMessage ERROR_REGISTERING_JVM = new LogMessage("DME2-0146", "Code=Exception.DME2GRMJVMRegistration.DME2GRMJVMRegistration;Exception={};AttemptNumber={};MaxRetries={}");
  ////////////////////////////////////////////////////////////////////////////////////////
	
	public static Set<LogMessage> values() 
	{
		synchronized(allInstances)
		{
			return new HashSet<LogMessage>(allInstances); 
		}
	}

	public static LogMessage valueOf(String code)
	{
		for(LogMessage msg : values())
		{
			if(msg.code.equals(code)){
				return msg;
			}
		}
		throw new IllegalArgumentException("no LogMessage known with code: " + code);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	private final String code, template;
	private final int argCount;
	
	/** limit instances from being created just anywhere, most code should only refer to static instances
	 *  to prevent accidentally creating more than one instance with the same code
	 */
	protected LogMessage(String code, String template)
	{
		this.code = code;
		this.template = template;
		int count=0, index=template.indexOf("{}");
		while(index>=0)
		{
			count++;
			index = template.indexOf("{}", index+1);
		}
		this.argCount = count;
		
		synchronized(allInstances)
		{
			if( ! allInstances.add(this))
			{
				throw new IllegalArgumentException("attempt to create duplicate log message with code " + code);
			}
		}
	}
	
	@Override
	public String toString() { return code; }
	
	public String toString(Object... args) 
	{
		if(args.length==argCount){
			return String.format(template, args); 
		}
		else{
			return template + ": " + Arrays.asList(args);
		}
	}

	@Override
	public int hashCode() 
	{
		return code.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if( ! (obj instanceof LogMessage)){
			return false;
		}
		final LogMessage other = (LogMessage) obj;
		return (code==null) ? (other.code==null) : (code.equals(other.code));
	}

	public String getCode() {
		return code;
	}

	public String getTemplate() {
		return template;
	}

	public int getArgCount() {
		return argCount;
	}
	
}
