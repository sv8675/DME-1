package com.att.aft.dme2.util;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.logging.LoggingContext;

@SuppressWarnings("PMD.AvoidCatchingThrowable")

public class DME2Constants {
	public static final String VERSION = "version";
	public static final String DME2_CRED_AUTH_USERNAME = "DME2_CRED_AUTH_USERNAME";
	public static final String AFT_DME2_CHARSET = "AFT_DME2_CHARSET";
	public static final String DME2_CRED_AUTH_PASSWORD = "DME2_CRED_AUTH_PASSWORD";
	public static final String AFT_DME2_IGNORE_FAILOVER_ONEXPIRE = "AFT_DME2_IGNORE_FAILOVER_ONEXPIRE";
	public static final String AFT_LATITUDE = "AFT_LATITUDE";
	public static final String AFT_LONGITUDE = "AFT_LONGITUDE";
	public static final String DME2_ENDPOINT_BANDS = "DME2_ENDPOINT_BANDS";
	public static final String DME2_ENDPOINT_BANDS_EXCLUDE_OUT_OF_BAND = "DME2_ENDPOINT_BANDS_EXCLUDE_OUT_OF_BAND";
	public static final String AFT_DME2_HOSTNAME = "AFT_DME2_HOSTNAME";
	public static final String AFT_DME2_PORT = "AFT_DME2_PORT";
	public static final String AFT_DME2_SSL_ENABLE = "AFT_DME2_SSL_ENABLE";
	public static final String AFT_DME2_PORT_RANGE = "AFT_DME2_PORT_RANGE";
	public static final String DME2_EP_REGISTRY_CLASS = "DME2_EP_REGISTRY_CLASS";
	public static final String DME2GRM = "DME2GRM";
	public static final String DME2FS = "DME2FS";
	public static final String DME2MEMORY = "DME2MEMORY";
	public static final String DME2GRMREST = "DME2GRMREST";
	public static final String AFT_DME2_CLIENT_MAX_CONNS_PER_ADDRESS = "AFT_DME2_CLIENT_MAX_CONNS_PER_ADDRESS";
	public static final String AFT_DME2_CLIENT_CONNECT_TIMEOUT_MS = "AFT_DME2_CLIENT_CONNECT_TIMEOUT_MS";
	public static final String AFT_DME2_CLIENT_TP_MAX_IDLE_TIME_MS = "AFT_DME2_CLIENT_TP_MAX_IDLE_TIME_MS";
	public static final String AFT_DME2_CLIENT_TP_MAX_THREADS = "AFT_DME2_CLIENT_TP_MAX_THREADS";
	public static final String AFT_DME2_CLIENT_TP_MIN_THREADS = "AFT_DME2_CLIENT_TP_MIN_THREADS";
	public static final String AFT_DME2_CLIENT_TP_MAX_QUEUED = "AFT_DME2_CLIENT_TP_MAX_QUEUED";
	public static final String AFT_DME2_CLIENT_MAX_BUFFERS = "AFT_DME2_CLIENT_MAX_BUFFERS";
	public static final String AFT_DME2_CLIENT_REQ_BUFFER_SIZE = "AFT_DME2_CLIENT_REQ_BUFFER_SIZE";
	public static final String AFT_DME2_CLIENT_REQ_HEADER_SIZE = "AFT_DME2_CLIENT_REQ_HEADER_SIZE";
	public static final String AFT_DME2_CLIENT_RSP_BUFFER_SIZE = "AFT_DME2_CLIENT_RSP_BUFFER_SIZE";
	public static final String AFT_DME2_CLIENT_RSP_HEADER_SIZE = "AFT_DME2_CLIENT_RSP_HEADER_SIZE";
	public static final String AFT_DME2_CLIENT_MAX_RETRIES = "AFT_DME2_CLIENT_MAX_RETRIES";
	public static final String AFT_DME2_CLIENT_CONN_BLOCKING = "AFT_DME2_CLIENT_CONN_BLOCKING";
	public static final String AFT_DME2_CLIENT_ALLOW_RENEGOTIATE = "AFT_DME2_CLIENT_ALLOW_RENEGOTIATE";
	public static final String AFT_DME2_CLIENT_PROXY_HOST = "AFT_DME2_CLIENT_PROXY_HOST";
	public static final String AFT_DME2_CLIENT_PROXY_PORT = "AFT_DME2_CLIENT_PROXY_PORT";
	public static final String AFT_DME2_CLIENT_IGNORE_SSL_CONFIG = "AFT_DME2_CLIENT_IGNORE_SSL_CONFIG";
	public static final String AFT_DME2_CLIENT_SSL_EXCLUDE_PROTOCOLS = "AFT_DME2_CLIENT_SSL_EXCLUDE_PROTOCOLS";
	public static final String AFT_DME2_CLIENT_SSL_EXCLUDE_CIPHERSUITES = "AFT_DME2_CLIENT_SSL_EXCLUDE_CIPHERSUITES";
	public static final String AFT_DME2_CLIENT_SSL_INCLUDE_PROTOCOLS = "AFT_DME2_CLIENT_SSL_INCLUDE_PROTOCOLS";
	public static final String AFT_DME2_CLIENT_SSL_INCLUDE_CIPHERSUITES = "AFT_DME2_CLIENT_SSL_INCLUDE_CIPHERSUITES";
	public static final String AFT_DME2_SERVICE_STATS_NA_VALUE = "AFT_DME2_SERVICE_STATS_NA_VALUE";
	public static final String AFT_DME2_CLIENT_MAX_RETRY_RECURSION = "AFT_DME2_CLIENT_MAX_RETRY_RECURSION";
	public static final String AFT_DME2_HTTP_EXCHANGE_TRACE_ON = "AFT_DME2_HTTP_EXCHANGE_TRACE_ON";
	public static final String AFT_DME2_THROTTLE_RESPONSE_CHECK = "AFT_DME2_THROTTLE_RESPONSE_CHECK";
  public static final String THROTTLE_FILTER_CONFIG_FILE = "AFT_DME2_THROTTLE_FILTER_CONFIG_FILE";
	public static final String AFT_DME2_PUBLISH_METRICS_PROCESSOR_THREADS = "AFT_DME2_PUBLISH_METRICS_PROCESSOR_THREADS";
	public static final String AFT_DME2_PUBLISH_METRICS_QUEUE_SIZE = "AFT_DME2_PUBLISH_METRICS_QUEUE_SIZE";
	public static final String DME2_DEBUG = "DME2.DEBUG";
	public static final String AFT_DME2_DEFAULT_RO = "AFT_DME2_DEFAULT_RO";
	public static final String DME2_GRM_USER = "DME2_GRM_USER";
	public static final String DME2_GRM_PASS = "DME2_GRM_PASS";
	public static final String DME2_RO_SEP = "DME2_RO_SEP";
	public static final String AFT_DME2_INTERFACE_SERVER_ROLE = "AFT_DME2_INTERFACE_SERVER_ROLE";
	public static final String AFT_DME2_INTERFACE_CLIENT_ROLE = "AFT_DME2_INTERFACE_CLIENT_ROLE";
	public static final String AFT_DME2_INTERFACE_JMS_PROTOCOL = "AFT_DME2_INTERFACE_JMS_PROTOCOL";
	public static final String AFT_DME2_INTERFACE_HTTP_PROTOCOL = "AFT_DME2_INTERFACE_HTTP_PROTOCOL";
	public static final String CHECK_DATA_PARTITION_RANGE_FIRST = "CHECK_DATA_PARTITION_RANGE_FIRST";

	// Parts of the service path
	public static final String SERVICE_PATH_KEY_SERVICE = "service";
	public static final String SERVICE_PATH_KEY_ROUTE_OFFER = "routeOffer";
	public static final String SERVICE_PATH_KEY_VERSION = "version";
	public static final String SERVICE_PATH_KEY_ENV_CONTEXT = "envContext";
  public static final String AFT_DME2_NON_FAILOVER_HTTP_REST_SCS_DEFAULT =
      "AFT_DME2_NON_FAILOVER_HTTP_SCS_DEFAULT(REST)";
  public static final String AFT_DME2_CLIENT_USE_DIRECT_BUFFERS = "AFT_DME2_CLIENT_USE_DIRECT_BUFFERS";
  public static final String AFT_DME2_NON_FAILOVER_HTTP_REST_SCS = "AFT_DME2_NON_FAILOVER_HTTP_REST_SCS";
  public static final String AFT_DME2_PREFERRED_VERSION = "AFT_DME2_PREFERRED_VERSION";
  public static final String AFT_DME2_FLAG_FORCE_PREFERRED_ROUTE_OFFER = "AFT_DME2_FORCE_PREFERRED_ROUTE_OFFER";
  public static final String EXC_REGISTRY_JAXB = "AFT-DME2-0404";
	public static final String AFT_DME2_STRICTLY_ENFORCE_ROUNDTRIP_TIMEOUT =
			"AFT_DME2_STRICTLY_ENFORCE_ROUNDTRIP_TIMEOUT";
	public static final String AFT_DME2_TIME_TO_ABANDON_REQUEST = "AFT_DME2_TIME_TO_ABANDON_REQUEST";
	public static final String DME2_LOCAL_CLIENT_QUEUE_EXPIRES_AFTER = "DME2_LOCAL_CLIENT_QUEUE_EXPIRES_AFTER";

	// End service path parts
	private static String CLASSNAME = DME2Constants.class.getName();
	public static final String TRUE = "true";
	public static final String TWENTYFOURTHOUSAND = "240000";
	public static final String FALSE = "false";

	// Logging Constants
	private static final Logger logger = LoggerFactory.getLogger(CLASSNAME);
	private static final Logger clientEventLogger = LoggerFactory.getLogger("com.att.aft.dme2.events.client.summary");
	private static final Logger routeOfferLogger = LoggerFactory.getLogger("com.att.aft.dme2.events.client.routeoffer");
  private static String grmUseDefaultUserPassword = "AFT_DME2_USE_DEFAULT_GRM_USER_PASSWORD";

	// Service Stats Constants
	public static final long CHECK_INTERVAL = 120;
	public static final long EXPIRY_INTERVAL = 300;
	public static final Logger debugLogger = LoggerFactory.getLogger("com.att.aft.dme2.debug");
	public static final Logger debugClientLogger = LoggerFactory.getLogger("com.att.aft.dme2.debug-client");
	public static final Logger debugTempQLogger = LoggerFactory.getLogger("com.att.aft.dme2.debug-tempq");
	public static String DME2_GRM_AUTH = "DME2_GRM_AUTH";

	public static final String AFT_DME2_ALLOW_EMPTY_SEP_GRM = "AFT_DME2_ALLOW_EMPTY_SEP_GRM";

	private static String nameSep = ".";
	public static String PORT_DEFAULT_SEP = ",";
	private static String portRangeSep = "-";

	// Queue being initialized first time
	public static final String INIT_EVENT = "INIT_EVENT";
	public static final String REQUEST_EVENT = "REQUEST_EVENT";
	public static final String REPLY_EVENT = "REPLY_EVENT";
	public static final String FAULT_EVENT = "FAULT_EVENT";
	public static final String FAILOVER_EVENT = "FAILOVER_EVENT";

	public static final String CREATE_TIME = "CREATE_TIME";
	public static final String EVENT_TIME = "EVENT_TIME";
	public static final String ELAPSED_TIME = "ELAPSED_TIME";
	public static final String MSG_SIZE = "MSG_SIZE";
	public static final String CLIENT_ADDR = "CLIENT_ADDR";

	public static final String QUEUE_NAME = "QUEUE_NAME";
	public static final String MESSAGE_ID = "MESSAGE_ID";
	public static final String JMS = "JMS";

	public static final String AFT_DME2_EP_READ_TIMEOUT = "AFT_DME2_EP_READ_TIMEOUT";
	public static final String AFT_DME2_DEF_ROUNDTRIP_TIMEOUT_MS = "AFT_DME2_DEF_ROUNDTRIP_TIMEOUT_MS";
	public static final String AFT_DME2_ROUNDTRIP_TIMEOUT_MS = "AFT_DME2_ROUNDTRIP_TIMEOUT_MS";
	public static final String AFT_DME2_SERVER_REPLY_DEFAULT_TIMEOUT_MS = "AFT_DME2_SERVER_REPLY_DEFAULT_TIMEOUT_MS";

	public static final String DME2_INTERFACE_WEBSOCKET_PROTOCOL = "DME2_INTERFACE_WEBSOCKET_PROTOCOL";
	public static final String DME2_WEBSOCKET_SERVICE_NAME = "DME2_WEBSOCKET_SERVICE_NAME";
	public static final String AFT_DME2_WEBSOCKET_METRICS_COLLECTION = "AFT_DME2_WEBSOCKET_METRICS_COLLECTION";
	public static final String AFT_DME2_INTERFACE_WS_PROTOCOL = "AFT_DME2_INTERFACE_WS_PROTOCOL";

	public static final String AFT_DME2_SERVER_WEBSOCKET_CONNECTION_MAXIDLETIME = "AFT_DME2_SERVER_WEBSOCKET_CONNECTION_MAXIDLETIME";
	public static final String AFT_DME2_SERVER_WEBSOCKET_MAX_TEXT_MESSAGE_SIZE = "AFT_DME2_SERVER_WEBSOCKET_MAX_TEXT_MESSAGE_SIZE";
	public static final String AFT_DME2_SERVER_WEBSOCKET_MAX_BINARY_MESSAGE_SIZE = "AFT_DME2_SERVER_WEBSOCKET_MAX_BINARY_MESSAGE_SIZE";
	public static final String AFT_DME2_SERVER_WEBSOCKET_TRACKING_ID = "trackingId";
	public static final String AFT_DME2_SERVER_WEBSOCKET_SERVER_SERVICE_NAME = "websocketServiceName";
	public static final String AFT_DME2_SERVER_WEBSOCKET_SERVER_PORT = "websocketPort";
	public static final String AFT_DME2_CLIENT_IGNORE_CONTENT_CHECK = "AFT_DME2_CLIENT_IGNORE_CONTENT_CHECK";
	public static final String AFT_DME2_CLIENT_IGNORE_CONTENT_LENGTH_BYTE_SIZE = "AFT_DME2_CLIENT_IGNORE_CONTENT_LENGTH_BYTE_SIZE";
	public static final String AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE = "AFT_DME2_CLIENT_IGNORE_RESPONSE_CONTENT_TYPE";
	public static final String AFT_DME2_SERVER_WEBSOCKET_APPEND_CONTEXT = "AFT_DME2_SERVER_WEBSOCKET_APPEND_CONTEXT";
	public static final String RESPONSE_CONTENT_TYPE = "application/octet-stream";
	public static final String DME2_INTERFACE_ROLE = "DME2_INTERFACE_ROLE";
	public static final String DME2_INTERFACE_PROTOCOL = "DME2_INTERFACE_PROTOCOL";
	public static final String DME2_REQUEST_PARTNER = "DME2_REQUEST_PARTNER";
	public static final String DME2_JMS_REQUEST_PARTNER = "DME2_JMS_REQUEST_PARTNER";
	public static final String DME2_JMS_REQUEST_PARTNER_CLASS = "com.att.aft.dme2.jms.partner";
	public static final String DME2_REQUEST_PARTNER_CLASS = "com.att.aft.dme2.partner";
	public static final String DME2_JMS_REQUEST_CHARSET_CLASS = "com.att.aft.dme2.jms.charset";
	public static final String DME2_AFT_CLASS = "com.att.aft.DME2";

	//public static final String DME2_HEADER_PREFIX = "DME2_HEADER_PREFIX";
	public static final String DME2_HEADER_PREFIX = "DME2_HEADER_PREFIX";

	public static final String DME2_OVERRIDE_HEADERS = "DME2_OVERRIDE_HEADERS";
	public static final String DME2_ALL_EP_FAILED_MSGCODE = "AFT-DME2-0703";
	public static final String AFT_DME2_EP_CONN_TIMEOUT = "AFT_DME2_EP_CONN_TIMEOUT";
	public static final String DME2_INTERFACE_PORT = "DME2_INTERFACE_PORT";

	public static final String AFT_DME2_DISABLE_INGRESS_REPLY_STREAM = "AFT_DME2_DISABLE_INGRESS_REPLY_STREAM";

	// Metrics Constants
	public static String AFT_DME2_DISABLE_METRICS = "AFT_DME2_DISABLE_METRICS";
	public static String AFT_DME2_DISABLE_METRICS_FILTER = "AFT_DME2_DISABLE_METRICS_FILTER";

	public static final String AFT_DME2_PUBLISH_METRICS = "AFT_DME2_PUBLISH_METRICS";
	public static final String AFT_DME2_PUBLISH_METRICS_INTERVAL = "AFT_DME2_PUBLISH_METRICS_INTERVAL";
	public static final String AFT_DME2_METRICS_SVC_LIST_IGNORE = "AFT_DME2_METRICS_SVC_LIST_IGNORE";
	public static final String AFT_DME2_PARSE_FAULT = "AFT_DME2_PARSE_FAULT";
	public static final String AFT_DME2_FAULT_STRING_FAILOVER = "AFT_DME2_FAULT_STRING_FAILOVER";
	public static final String AFT_DME2_ENVELOPE_STR = "AFT_DME2_ENVELOPE_STR";
	public static final String AFT_DME2_SOAP_REPLY_CONTENT_TYPE = "AFT_DME2_SOAP_REPLY_CONTENT_TYPE";
	public static final String DME2_FAILOVER_HANDLER = "AFT_DME2_FAILOVER_HANDLER";
	public static final String DME2_DEFAULT_FAILOVER_HANDLER = "com.att.aft.dme2.api.DME2DefaultFailoverHandler";
	public static final String GRM_CONNECT_TIMEOUT = "AFT_DME2_GRM_CONNECT_TIMEOUT";
	public static final String AFT_DME2_GRM_READ_TIMEOUT = "AFT_DME2_GRM_READ_TIMEOUT";
	public static final String AFT_DME2_GRM_OVERALL_TIMEOUT = "AFT_DME2_GRM_OVERALL_TIMEOUT";
	public static final String AFT_DME2_QLIST_IGNORE = "AFT_DME2_QLIST_IGNORE";
	private static boolean clientDebug = false;

	public static final String AFT_DME2_DISCCLT_MAJOR = "AFT_DME2_DISCCLT_MAJOR";
	public static final String AFT_DME2_DISCCLT_MINOR = "AFT_DME2_DISCCLT_MINOR";
	public static final String AFT_DME2_DISCCLT_PATCH = "AFT_DME2_DISCCLT_PATCH";
	public static final String AFT_DME2_FAST_CACHE_EP_ELIGIBLE_COUNT = "AFT_DME2_FAST_CACHE_EP_ELIGIBLE_COUNT";
	public static final String AFT_DME2_FAST_CACHE_STALE_EP_ELIGIBLE_COUNT = "AFT_DME2_FAST_CACHE_STALE_EP_ELIGIBLE_COUNT";
	public static final String DME2_IGNORE_FAILOVER_STREAM_PAYLOAD_MSGCODE = "AFT-DME2-0717";

	public static final String DME2_IGNORE_FAILOVER_ONEXPIRE_MSGCODE = "AFT-DME2-0709";
	public static final String AFT_DME2_COLLECT_SERVICE_STATS = "AFT_DME2_COLLECT_SERVICE_STATS";
	public static final String AFT_DME2_CONFIGURE_CUSTOM_CONNECTOR = "AFT_DME2_CONFIGURE_CUSTOM_CONNECTOR";

	public static final String AFT_DME2_SERVER_DEFAULT_PORT_RANGE = "AFT_DME2_SERVER_DEFAULT_PORT_RANGE";
	public static final String AFT_DME2_SERVER_DEFAULT_SSL_PORT_RANGE = "AFT_DME2_SERVER_DEFAULT_SSL_PORT_RANGE";
	public static final String AFT_DME2_COMPRESS_ENCODING = "AFT_DME2_COMPRESS_ENCODING";
	public static final String AFT_DME2_ALLOW_COMPRESS_ENCODING = "AFT_DME2_ALLOW_COMPRESS_ENCODING";
	public static final String AFT_DME2_CONTENT_ENCODING_KEY = "AFT_DME2_CONTENT_ENCODING_KEY";
	public static final String AFT_DME2_MAX_GETAVAIL_PORT_ATTEMPT = "AFT_DME2_MAX_GETAVAIL_PORT_ATTEMPT";
	public static final String AFT_DME2_GETAVAIL_PORT_RANGE = "AFT_DME2_GETAVAIL_PORT_RANGE";
	public static final String AFT_DME2_ALLOW_INVOKE_HANDLERS = "AFT_DME2_ALLOW_INVOKE_HANDLERS";
	public static final String AFT_DME2_EXCHANGE_REQUEST_HANDLERS_KEY = "AFT_DME2_EXCHANGE_REQUEST_HANDLERS_KEY";
	public static final String AFT_DME2_EXCHANGE_REQUEST_HANDLERS = "AFT_DME2_EXCHANGE_REQUEST_HANDLERS";
	public static final String AFT_DME2_EXCHANGE_REPLY_HANDLERS_KEY = "AFT_DME2_EXCHANGE_REPLY_HANDLERS_KEY";
	public static final String AFT_DME2_EXCHANGE_REPLY_HANDLERS = "AFT_DME2_EXCHANGE_REPLY_HANDLERS";	
	public static final String AFT_DME2_EXCHANGE_FAILOVER_HANDLERS_KEY = "AFT_DME2_EXCHANGE_FAILOVER_HANDLERS_KEY";
	public static final String AFT_DME2_ENABLE_FAILOVER_LOGGING = "AFT_DME2_ENABLE_FAILOVER_LOGGING";
	public static final String AFT_DME2_EXCH_ON_EXCEPTION_RESP_CODE = "AFT_DME2_EXCH_ON_EXCEPTION_RESP_CODE";
	public static final String AFT_DME2_EXCH_ON_EXPIRE_RESP_CODE = "AFT_DME2_EXCH_ON_EXPIRE_RESP_CODE";
	public static final String AFT_DME2_EXCH_INVOKE_FAILED_RESP_CODE = "AFT_DME2_EXCH_INVOKE_FAILED_RESP_CODE";
	public static final String AFT_DME2_EXCHANGE_ALLOW_RETRY_CURR_URL = "AFT_DME2_EXCHANGE_ALLOW_RETRY_CURR_URL";
	public static final String EXP_CORE_MISSING_PAYLOAD = "AFT-DME2-0701";
	
	// DME2 SSL Constants
	public static final String KEY_KEYSTORE_PASSWORD = "AFT_DME2_CLIENT_KEYSTORE_PASSWORD";
	public static final String KEY_TRUSTSTORE_PASSWORD = "AFT_DME2_CLIENT_TRUSTSTORE_PASSWORD";
	public static final String KEY_PASSWORD = "AFT_DME2_CLIENT_KEY_PASSWORD";
	public static final String KEY_ALLOW_RENEG = "AFT_DME2_CLIENT_ALLOW_RENEGOTIATE";
	public static final String KEY_KEYSTORE = "AFT_DME2_CLIENT_KEYSTORE";
	public static final String KEY_TRUSTSTORE = "AFT_DME2_CLIENT_TRUSTSTORE";
	public static final String KEY_SSL_TRUST_ALL = "AFT_DME2_CLIENT_SSL_TRUST_ALL";
	public static final String KEY_SSL_CERT_ALIAS = "AFT_DME2_CLIENT_SSL_CERT_ALIAS";
	public static final String KEY_SSL_NEED_CLIENT_AUTH = "AFT_DME2_CLIENT_SSL_NEED_CLIENT_AUTH";
	public static final String KEY_SSL_WANT_CLIENT_AUTH = "AFT_DME2_CLIENT_SSL_WANT_CLIENT_AUTH";
	public static final String KEY_SSL_ENABLED_SESSION_CACHING = "AFT_DME2_CLIENT_SSL_ENABLED_SESSION_CACHING";
	public static final String KEY_SSL_SESSION_CACHE_SIZE = "AFT_DME2_CLIENT_SSL_SESSION_CACHE_SIZE";
	public static final String KEY_SSL_SESSION_TIMEOUT = "AFT_DME2_CLIENT_SSL_SESSION_TIMEOUT";
	public static final String KEY_SSL_VALIDATE_PEER_CERTS = "AFT_DME2_CLIENT_SSL_VALIDATE_PEER_CERTS";
	public static final String KEY_SSL_VALIDATE_CERTS = "AFT_DME2_CLIENT_SSL_VALIDATE_CERTS";
	public static final Boolean DEFAULT_ALLOW_RENEG = true;
	public static final String AFT_DME2_ENABLE_SELECTIVE_REFRESH = "AFT_DME2_ENABLE_SELECTIVE_REFRESH";
	public static final String DME2_SERVICE_LAST_QUERIED_INTERVAL_MS = "DME2_SERVICE_LAST_QUERIED_INTERVAL_MS";
	public static final String DME2_SEP_CACHE_INFREQUENT_TTL_MS = "DME2_SEP_CACHE_INFREQUENT_TTL_MS";

	public static final String ON_RESPONSE_STATUS_REPLY_SIZE = "ON_RESPONSE_STATUS_REPLY_SIZE";
	public static final String ON_RESPONSE_EXCEPTION_RETURN_CODE = "ON_RESPONSE_EXCEPTION_RETURN_CODE";
	public static final String ON_RESPONSE_EXCEPTION_RETURN_MESSAGE = "ON_RESPONSE_EXCEPTION_RETURN_MESSAGE";
	public static final String AFT_DME2_REQ_TRACE_INFO = "AFT_DME2_REQ_TRACE_INFO";
	public static final String AFT_DME2_ALLOW_ALL_HTTP_RETURN_CODES = "AFT_DME2_ALLOW_ALL_HTTP_RETURN_CODES";

	// Runtime args keys
	public static final String AFT_DME2_CONTAINER_NAME_KEY = "AFT_DME2_CONTAINER_NAME_KEY";
	public static final String AFT_DME2_CONTAINER_VERSION_KEY = "AFT_DME2_CONTAINER_VERSION_KEY";
	public static final String AFT_DME2_CONTAINER_ROUTEOFFER_KEY = "AFT_DME2_CONTAINER_ROUTEOFFER_KEY";
	public static final String AFT_DME2_CONTAINER_ENV_KEY = "AFT_DME2_CONTAINER_ENV_KEY";
	public static final String AFT_DME2_CONTAINER_PLATFORM_KEY = "AFT_DME2_CONTAINER_PLATFORM_KEY";
	public static final String AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY = "AFT_DME2_CONTAINER_SCLD_PLATFORM_KEY";
	public static final String AFT_DME2_CONTAINER_HOST_KEY = "AFT_DME2_CONTAINER_HOST_KEY";
	public static final String AFT_DME2_CONTAINER_PID_KEY = "AFT_DME2_CONTAINER_PID_KEY";

	public static final String AFT_DME2_CRLF = "AFT_DME2_CRLF";
	public static final String contentDispositionHeader = "Content-Disposition: form-data; name=\"upload_file\"; filename=\"";
	public static final String AFT_DME2_CONTENT_DISP_HEADER = "AFT_DME2_CONTENT_DISP_HEADER";

	public static final String contentDispositionHeaderName = "Content-Disposition: form-data; name=\"";
	public static final String contentDispositionHeaderFile = "\"; filename=\"";
	// contentDispositionHeaderName);
	// contentDispositionHeaderFile);
	public static final String AFT_DME2_MULTIPART_TYPE = "AFT_DME2_MULTIPART_TYPE";
	public static final String AFT_DME2_MULTIPART_CTYPE = "AFT_DME2_MULTIPART_CTYPE";
	public static final String AFT_DME2_CTYPE_HEADER = "AFT_DME2_CTYPE_HEADER";
	public static final String AFT_DME2_CLEN_HEADER = "AFT_DME2_CLEN_HEADER";
	public static final String DME2_URI_FIELD_WITH_PATH_SEP_DEF = "/service=,/subContext=";
	public static final String DME2_URI_FIELD_WITH_PATH_SEP = "DME2_URI_FIELD_WITH_PATH_SEP";

	// Key used to enable/disable GRM Topology Service invocation
	public static final String KEY_ENABLE_GRM_TOPOLOGY_SERVICE = "KEY_ENABLE_GRM_TOPOLOGY_SERVICE";
	public static final String KEY_GRM_ERROR_CODE_OVERRIDE = "KEY_GRM_ERROR_CODE_OVERRIDE";
	public static final String GRM_FAILOVER_CODE_PREFIX = "GRMSVC-9";

	public static final String DME2_PAYLOAD_COMPRESSION_THRESH_SIZE_KEY = "DME2_PAYLOAD_COMPRESSION_THRESH_SIZE";
//	public static final int DME2_PAYLOAD_COMPRESSION_THRESH_SIZE_DEF = 1200;

	public static final String DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES_KEY = "DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES";
//	public static final String DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES = "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml";

	public static final String DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH_KEY = "DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH";
	public static final boolean DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH_DEF = false;

	public static final String DME2_SKIP_STALE_GRM_OFFERS = "DME2_RETRY_STALE_GRM_OFFERS";
	public static final boolean DME2_SKIP_STALE_GRM_OFFERS_DEF = true;

	// DME2 JDBC Constants
	public static final String KEY_DME2_JDBC_DATABASE_NAME = "KEY_DME2_JDBC_DATABASE_NAME";
	public static final String KEY_DME2_JDBC_HEALTHCHECK_USER = "KEY_DME2_JDBC_HEALTHCHECK_USER";
	public static final String KEY_DME2_JDBC_HEALTHCHECK_PASSWORD = "KEY_DME2_JDBC_HEALTHCHECK_PASSWORD";
	public static final String KEY_DME2_JDBC_HEALTHCHECK_DRIVER = "KEY_DME2_JDBC_HEALTHCHECK_DRIVER";

  // Registry
	public static final String KEY_SEP_LEASE_EXPIRATION_OVERRIDE_MIN = "KEY_SEP_LEASE_EXPIRATION_OVERRIDE_MIN";
  public static final String DME2_SEP_LEASE_LENGTH_MS = "DME2_SEP_LEASE_LENGTH_MS";

	public static final String DME2_ERROR_TABLE_BASE_NAME = "com/att/aft/dme2/api/errorTable";

	public static final String AFT_DME2_SKIP_SERVICE_URI_VALIDATION = "AFT_DME2_SKIP_SERVICE_URI_VALIDATION";
	public static final String DME2_ROUTEOFFER_STALENESS_IN_MIN = "AFT_DME2_ROUTEOFFER_STALENESS_IN_MIN";
	public static final long DME2_ROUTEOFFER_STALENESS_IN_MIN_DEFAULT = 15;

	public static final String DME2_CACHED_ENDPOINTS_FILE = "AFT_DME2_CACHED_ENDPOINTS_FILE";
	public static final String DME2_REMOVE_PERSISTENT_CACHE_ON_STARTUP = "AFT_DME2_REMOVE_PERSISTENT_CACHE_ON_STARTUP";

	public static final String DME2_PERFORM_GRM_HEALTH_CHECK = "AFT_DME2_PERFORM_GRM_HEALTH_CHECK";
	public static final String DME2_GRM_HEALTH_CHECK_CONN_TIMEOUT = "AFT_DME2_GRM_HEALTH_CHECK_CONN_TIMEOUT";
	public static final String DME2_GRM_HEALTH_CHECK_READ_TIMEOUT = "AFT_DME2_GRM_HEALTH_CHECK_READ_TIMEOUT";

	public static final String DME2_CACHED_ROUTEINFO_FILE = "AFT_DME2_CACHED_ROUTEINFO_FILE";

	public static final String DME2_PREFERRED_ROUTEOFFER = "AFT_DME2_PREFERRED_ROUTEOFFER";

	public static final String DME2_REGISTER_STATIC_ENDPOINT = "DME2_REGISTER_STATIC_ENDPOINT";
	public static final String DME2_ENDPOINT_STALENESS_PERIOD = "AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS";
	public static final long DME2_ENDPOINT_STALENESS_PERIOD_DEFAULT = 90000L;

	public static final String AFT_DME2_GRM_FAILOVER_ERROR_CODES = "AFT_DME2_GRM_FAILOVER_ERROR_CODES";

	public static final int DME2_CONSTANT_ONE = 1;
	public static final int DME2_CONSTANT_TWO = 2;
	public static final int DME2_CONSTANT_THREE = 3;
	public static final int DME2_CONSTANT_FOUR = 4;
	public static final int DME2_CONSTANT_FIVE = 5;
	public static final int DME2_CONSTANT_SIX = 6;
	public static final int DME2_CONSTANT_SEVEN = 7;
	public static final int DME2_CONSTANT_EIGHT = 8;
	public static final int DME2_CONSTANT_NINE = 9;

	public static final int DME2_ERROR_CODE_503 = 503;
	public static final int DME2_ERROR_CODE_404 = 404;
	public static final int DME2_ERROR_CODE_500 = 500;
	public static final int DME2_ERROR_CODE_429 = 429;

	public static final int DME2_RESPONSE_STATUS_200 = 200;

	// DME2 Websocket constants
	public static final String DME2_WS_CONNECT_ID = "AFT_DME2_CONNECT_ID";
	public static final String DME2_WS_INTERFACE_PROTOCOL = "ws";
	public static final String DME2_WS_HANDLE_FAILOVER = "AFT_DME2_WS_HANDLE_FAILOVER";
	// public static final String DME2_WEBSOCKET_METRICS_COLLECTION =
	// "DME2_WEBSOCKET_METRICS_COLLECTION";
	public static final String AFT_DME2_FAILOVER_WS_CLOSE_CDS = "AFT_DME2_FAILOVER_WS_CLOSE_CDS";
	public static final String AFT_DME2_RETRY_WS_CLOSE_CDS = "AFT_DME2_RETRY_WS_CLOSE_CDS";
	public static final String AFT_DME2_DEF_WS_IDLE_TIMEOUT = "AFT_DME2_DEF_WS_IDLE_TIMEOUT";
	public static final String AFT_DME2_WS_MAX_RETRY_COUNT = "AFT_DME2_WS_MAX_RETRY_COUNT";
	public static final String AFT_DME2_WS_ENABLE_TRACE_ROUTE = "AFT_DME2_WS_ENABLE_TRACE_ROUTE";

	public static final String EXCEPTION_HANDLER_MSG = "AFT-DME2-9000";
	public static final String DME2_MIN_ACTIVE_END_POINTS = "DME2_MIN_ACTIVE_END_POINTS";

	public static final String HTTP = "http";
	public static final String EXTENDED_STRING = "extendedMessage";
	public static final String MANAGER = "manager";
	public static final String SERVICE = "service";
	public static final String SERVERURL = "serverURL";
	public static final String ENDPOINT_ELAPSED_MS = "EndpointElapsedMs";
	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String PARTNER = "partner";
	public static final String KEY = "key";
	public static final String SLASHSLASH = "\\.";

	public final static String JMSMESSAGEID = "JMSMessageID";
	public final static String JMSCORRELATIONID = "JMSCorrelationID";
	public final static String JMSDESTINATION = "JMSDestination";
	public final static String DME2_ROUTE_OFFER_SEP = "~";
	public final static String JMSCONVERSATIONID = "JMSConversationID";
	public final static String JMS_REPLY_TO = "JMSReplyTo";
	public final static String HTTP_DME2_LOCAL = "http://DME2LOCAL/";
	public final static String EPREFERENCES = "[EPREFERENCES=[%s]];";
	public final static String MINACTIVEENDPOINTS = "[MINACTIVEENDPOINTS=%s];";
	public final static String AFT_DME2_LOOKUP_NON_FAILOVER_SC = "AFT_DME2_LOOKUP_NON_FAILOVER_SC";
	public final static String AFT_DME2_NON_FAILOVER_HTTP_SCS_HEADER = "AFT_DME2_NON_FAILOVER_HTTP_SCS_HEADER";
	public final static String AFT_DME2_NON_FAILOVER_HTTP_SCS_QUERYPARAM = "AFT_DME2_NON_FAILOVER_HTTP_SCS_QUERYPARAM";
	public final static String AFT_DME2_NON_FAILOVER_HTTP_SCS_DEFAULT = "AFT_DME2_NON_FAILOVER_HTTP_SCS_DEFAULT";
	public final static String AFT_DME2_NON_FAILOVER_HTTP_SCS = "AFT_DME2_NON_FAILOVER_HTTP_SCS";
  public static final String DME2_NON_FAILOVER_HTTP_REST_SCS = "AFT_DME2_NON_FAILOVER_HTTP_REST_SCS";

	public final static String HANDLE_ROUTE_OFFER_FAILOVER = "handleRouteOfferFailover";
	public final static String HANDLE_ROUTE_OFFER_FAILOVER_INVOKED = "handleRouteOfferFailover invoked";
	
	
	// Exception codes - do we have some descriptions for these?
  public final static String EXC_ROUTE_INFO_NOT_FOUND_IN_PARTITION_RANGES = "AFT-DME2-0101";
  public final static String EXC_ROUTE_INFO_NOT_FOUND_IN_PARTITION_LIST = "AFT-DME2-0105";
  public final static String EXP_CORE_AFT_DME2_0702 = "AFT-DME2-0702";
	public final static String EXP_CORE_AFT_DME2_0703 = "AFT-DME2-0703";
	public final static String EXP_CORE_AFT_DME2_0704 = "AFT-DME2-0704";
	public final static String EXP_CORE_AFT_DME2_0705 = "AFT-DME2-0705";
	public final static String EXP_CORE_AFT_DME2_0706 = "AFT-DME2-0706";
	public final static String EXP_CORE_AFT_DME2_0707 = "AFT-DME2-0707";
	public final static String EXP_CORE_AFT_DME2_0710 = "AFT-DME2-0710";
	public final static String EXP_CORE_INVALID_REQ = "Invalid request object";
	public final static String EXP_CORE_INVALID_REQ_URI = "Invalid URI";
	public final static String EXP_CORE_INVALID_PAYLOAD = "Invalid payoad object";
	public final static String EXP_CORE_NULL_PAYLOAD = "Payload is null";
	public final static String EXP_CORE_INVALID_MGR = "Invalid manager object";
	public final static String EXP_CORE_AFT_DME2_0997 = "AFT-DME2-0997";
	public final static String EXP_CORE_AFT_DME2_0998 = "AFT-DME2-0998";
	public final static String EXP_CORE_AFT_DME2_0999 = "AFT-DME2-0999";
	public final static String EXP_CORE_AUTH_ERR = "Error occured while establishing authentication.";
	public final static String EXP_CORE_AFT_DME2_9703 = "AFT-DME2-9703";
	public final static String EXP_CORE_AFT_DME2_9704 = "AFT-DME2-9704";
	public final static String EXP_CORE_AFT_DME2_9705 = "AFT-DME2-9705";
	public final static String EXP_CORE_AFT_DME2_9700 = "AFT-DME2-9700";
	public final static String EXP_CORE_AFT_DME2_9701 = "AFT-DME2-9701";
	public final static String EXP_CORE_AFT_DME2_9702 = "AFT-DME2-9702";
	public final static String EXP_CORE_AFT_DME2_0007 = "AFT-DME2-0007";
	public final static String EXP_CORE_AFT_DME2_0016 = "AFT-DME2-0016";
	public final static String EXP_CORE_INVALID_DMERESOURCE = "Invalid DmeUniformResource object";
	public final static String EXP_CORE_AFT_SERVICE_CALL_TIMEDOUT = "Service call timedout";
	public final static String EXP_CORE_AFT_CONTENT_ENCODING = "Content-Encoding set to gzip;UNABLE TO PARSE GZIP'D RESPONSE MESSAGE";
	public final static String EXP_CORE_AFT_UNABLE_READ_RES = "UNABLE TO READ RESPONSE MESSAGE";
	public final static String EXP_CORE_AFT_VALIDATE_ENDPOINT_RUNNING = "validate that the endpoint is running if no aother endpoints are available for failover.";
	public final static String EXP_CORE_AFT_PARSE_FAULT_RES = "Parse Fault Response Failed";
	public final static String EXP_CORE_AFT_DME2_0718 = "AFT-DME2-0718";
	public final static String EXP_AFT_DME2_6700 = "AFT-DME2-6700";
	public final static String EXP_AFT_DME2_6702 = "AFT-DME2-6702";
	public final static String EXP_AFT_DME2_6701 = "AFT-DME2-6701";
	public final static String EXP_AFT_DME2_6703 = "AFT-DME2-6703";
	public final static String EXP_AFT_DME2_0720 = "AFT-DME2-0720";

	// General Exception Codes

	public final static String EXP_GEN_URI_EXCEPTION = "AFT-DME2-0607";

	// Registry Exception Codes

	public final static String EXP_REG_ROUTE_INFO_FILE_NOT_FOUND = "AFT-DME2-9602";
	public final static String EXP_REG_NULL_MANAGER = "AFT-DME2-9603";

	public final static String ROUTE_OFFER_TRIED = "routeOffersTried";
	public final static String AFT_DME2_EP_READ_TIMEOUT_MS = "AFT_DME2_EP_READ_TIMEOUT_MS";
	public final static String CFG_AFT_DME2_EP_READ_TIMEOUT_MS = "CFG_AFT_DME2_EP_READ_TIMEOUT_MS";
	public final static String EXECUTE_SET_READ_TIMEOUT = "EXECUTE_SET_READ_TIMEOUT";
	public final static String NO_REPLY_HANDLER_SET = "NO_REPLY_HANDLER_SET";
	public final static String NULL_REPLY_HANDLER = "NULL_REPLY_HANDLER";
	public final static String NO_JMS_REPLY_TO_SET = "NO_JMS_REPLY_TO_SET";
	public final static String ON_RESPONSE_COMPLETE = "ON_RESPONSE_COMPLETE";
	public final static String ON_RESPONSE_STATUS_EXIT = "ON_RESPONSE_STATUS_EXIT";
	public final static String ON_RESPONSE_STATUS = "ON_RESPONSE_STATUS";
	public final static String ON_RESPONSE_IGNORE_CONTENT_LENGTH = "ON_RESPONSE_IGNORE_CONTENT_LENGTH";
	public final static String ON_RESPONSE_IGNORE_CONTENT_TYPE = "ON_RESPONSE_IGNORE_CONTENT_TYPE";
	public final static String ON_RESPONSE_STATUS_200_REPLY = "ON_RESPONSE_STATUS_200_REPLY";

	public final static String CURRENT_RETRY_ROUTE_OFFER = "CURRENT_RETRY_ROUTE_OFFER";
	public final static String CURRENT_RETRY_SEQUENCE = "CURRENT_RETRY_SEQUENCE";
	public final static String CURRENT_RETRY_ENDPOINT = "CURRENT_RETRY_ENDPOINT";
	public final static String CURRENT_RETRY_DISTANCE_BAND = "CURRENT_RETRY_DISTANCE_BAND";
	public final static String CURRENT_ROUTE_OFFER = "CURRENT_ROUTE_OFFER";
	public final static String CURRENT_SEQUENCE = "CURRENT_SEQUENCE";
	public final static String CURRENT_ENDPOINT = "CURRENT_ENDPOINT";
	public final static String CURRENT_DISTANCE_BAND = "CURRENT_DISTANCE_BAND";
	public final static String LOAD_LOCAL_SEP_OK = "LOAD_LOCAL_SEP_OK";
	public final static String LOAD_SEP_OK = "LOAD_SEP_OK";

	public static final String DME2_LOGGING_CONFIG_RELOAD_TIMER_TASK_SCHEDULE_INTERVAL = "DME2_LOGGING_CONFIG_RELOAD_TIMER_TASK_SCHEDULE_INTERVAL";
	public static final String DME2_LOGGING_CONFIG_RELOAD_TIMER_TASK_SCHEDULE_INTERVAL_DEFAULT = "60000";

	public static final String AFT_DME2_THROTTLE_PCT_PER_PARTNER = "AFT_DME2_THROTTLE_PCT_PER_PARTNER";

	// Throttle filter
	public static String AFT_DME2_DISABLE_THROTTLE_FILTER = "AFT_DME2_DISABLE_THROTTLE_FILTER";
	public static String DEFAULT_NA_VALUE;
	public static boolean DME2_LOOKUP_NON_FAILOVER_SC = false;

	public static final String AFT_DME2_CLIENT_SEND_TIMESTAMP_KEY = "AFT_DME2_CLIENT_SEND_TIMESTAMP_KEY";
	public static final String AFT_DME2_CLIENT_SEND_TIMESTAMP_TZ_KEY = "AFT_DME2_CLIENT_SEND_TIMESTAMP_TZ_KEY";
	public static final String AFT_DME2_ALLOW_CLIENT_SEND_TZ_OVERRIDE = "AFT_DME2_ALLOW_CLIENT_SEND_TZ_OVERRIDE";

	// public static final String DME2_METRICS_SVC_LIST_IGNORE = null;
	public static final boolean DISABLE_METRICS = false;

	public static final String AFT_DME2_EVENT_QUEUE_SIZE = "AFT_DME2_EVENT_QUEUE_SIZE";
	public static final String AFT_DME2_EVENT_PROCESSOR_THREADS = "AFT_DME2_EVENT_PROCESSOR_THREADS";
	public static final String AFT_DME2_LOG_REJECTED_EVENTS = "AFT_DME2_LOG_REJECTED_EVENTS";

	// Factory Handler Constants
	public static final String FAILOVER_ENDPOINT_HANDLER_IMPL = "FAILOVER_ENDPOINT_HANDLER_IMPL";
	public static final String FAILOVER_HANDLER_IMPL = "FAILOVER_HANDLER_IMPL";
	public static final String GRMACESSOR_HANDLER_IMPL = "GRMACESSOR_HANDLER_IMPL";
	public static final String REGISTRYBOOTSTRAP_HANDLER_IMPL = "REGISTRYBOOTSTRAP_HANDLER_IMPL";
	public static final String ENDPOINTORDERING_HANDLER_IMPL = "ENDPOINTORDERING_HANDLER_IMPL";

	// Factory Handler Exception Messages
	public static final String HANDLER_INSTANTIATION_EXCEPTION = " implementation Class can't be instantiated";
	public static final String HANDLER_INTERFACE_IMPLEMENTATION_EXCEPTION = "HandlerObject must implement interface - ";

	// Factory Handler For Metrics Publisher
	public static final String METRICS_PUBLISHER_HANDLER_IMPL = "METRICS_PUBLISHER_HANDLER_IMPL";

	// Constants fOR PortFileManager
	public static final String AFT_DME2_PORT_CACHE_FILE = "AFT_DME2_PORT_CACHE_FILE";
	public static final String AFT_DME2_SSL_PORT_CACHE_FILE = "AFT_DME2_SSL_PORT_CACHE_FILE";
	public static final String AFT_DME2_ALLOW_PORT_CACHING = "AFT_DME2_ALLOW_PORT_CACHING";
	public static final String DME2_PORT_FILELOCK_WAIT_ITER = "AFT_DME2_PORT_FILELOCK_WAIT_ITER";
	public static final String DME2_PORT_FILELOCK_WAIT_INTERVAL = "AFT_DME2_PORT_FILELOCK_WAIT_INTERVAL";

	// DME2Exchange properties
	public static final String INVOKE_FAILED_RSP_CODE = "AFT_DME2_EXCH_INVOKE_FAILED_RESP_CODE";
	public static final String DME2_ALLOW_INVOKE_HANDLERS = "AFT_DME2_ALLOW_INVOKE_HANDLERS";
	public static final String DME2_INTERFACE_SERVER_ROLE = "SERVER";
	public static boolean DME2_ALLOW_EMPTY_SEP_GRM = false;

	// JMS properties
	public static String PORTDEFAULTSEP = ",";
	public static String LOGRECORDSEP = "^";
	public static boolean GRMAUTHENABLED = true;
	public static final String ENABLE_CONTENT_LENGTH = "AFT_DME2_SET_RESLEN";
	public static final String DME2_REMOVE_QUEUE_CONN_CACHE = "AFT_DME2_REMOVE_QUEUE_CONN_CACHE";
	public static final String AFT_DME2_CONT_QUEUE_EXPIRES_AFTER = "AFT_DME2_CONT_QUEUE_EXPIRES_AFTER";
	public static final String AFT_DME2_SERVER_REPLY_OVERRIDE_TIMEOUT_MS = "AFT_DME2_SERVER_REPLY_OVERRIDE_TIMEOUT_MS";
	
	
	public static final String DME2_CONTENT_ENCODING_KEY = "DME2_CONTENT_ENCODING_KEY";
	public static final String DME2_CLIENT_COMPRESS_TYPE = "DME2_CLIENT_COMPRESS_TYPE";
	public static final String DME2_CLIENT_ALLOW_COMPRESS = "DME2_CLIENT_ALLOW_COMPRESS";
	public static final String DME2_ENDPOINT_DEF_READ_TIMEOUT = "AFT_DME2_ENDPOINT_DEF_READ_TIMEOUT";
	public static final String DME2_ACCEPT_ENCODING_KEY = "DME2_ACCEPT_ENCODING_KEY";
	
	public final static String AFT_DME2_0712 = "AFT-DME2-0712";
	private final static String GRMUserPass = "mxxxxx";
	private final static String GRMUserName = "mxxxxx";
	
	public static final String DNS_LAB_TEST_LWP = "DNS_LAB_TEST_LWP";
	public static final String DNS_TEST_LWP = "DNS_TEST_LWP";
	public static final String DNS_NONPROD_LWP = "DNS_NONPROD_LWP";
	public static final String DNS_PROD_LWP = "DNS_PROD_LWP";

	public static String getNAME_SEP() {
		return nameSep;
	}

	public static void setNAME_SEP(String nAMESEP) {
		nameSep = nAMESEP;
	}

	public static String getPORT_RANGE_SEP() {
		return portRangeSep;
	}

	public static void setPORT_RANGE_SEP(String pORTRANGESEP) {
		portRangeSep = pORTRANGESEP;
	}

	public static String getGRMUserPass() {
		return GRMUserPass;
	}

	public static String getGRMUserName() {
		return GRMUserName;
	}

	public static class Cache {
		public static class Type{
			public static final String ENDPOINT = "EndpointCache";
			public static final String ROUTE_INFO = "RouteInfoCache";
			public static final String STALE_ENDPOINT = "StaleEndpointCache";
			public static final String STALE_ROUTE_INFO = "StaleRouteOfferCache";
		}
		
		public static final String DME2_PERSIST_CACHED_ENDPOINTS_FREQUENCY_MS = "DME2_PERSIST_CACHED_ENDPOINTS_FREQUENCY_MS";
		public static final String DME2_PERSIST_CACHED_ROUTEINFO_FREQUENCY_MS = "DME2_PERSIST_CACHED_ROUTEINFO_FREQUENCY_MS";
		public static final String DME2_ROUTEINFO_CACHE_TTL_MS = "DME2_ROUTEINFO_CACHE_TTL_MS";
		public static final String DME2_UNUSED_ENDPOINT_REMOVAL_DURATION_MS = "DME2_UNUSED_ENDPOINT_REMOVAL_DURATION_MS";
		public static final String DME2_UNUSED_ENDPOINT_REMOVAL_DELAY ="DME2_UNUSED_ENDPOINT_REMOVAL_DELAY";
		public static final String AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS = "AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS";
		public static final String DME2_SEP_CACHE_TTL_MS = "DME2_SEP_CACHE_TTL_MS";
		public static final String DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS = "DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS";
		public static final String DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS = "DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS";
		public static final String DME2_DISABLE_PERSISTENT_CACHE = "AFT_DME2_DISABLE_PERSISTENT_CACHE";
		public static final String DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE="DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE";
		public static final String DME2_SEP_CACHE_EMPTY_TTL_MS = "DME2_SEP_CACHE_EMPTY_TTL_MS";
		public static final String DME2_SEP_EMPTY_CACHE_TTL_INTERVALS= "DME2_SEP_EMPTY_CACHE_TTL_INTERVALS";
		public static final String DISABLE_CACHE_STATS = "AFT_DME2_DISABLE_CACHE_STATS";
		public static final String CACHE_TTL_MS = "CACHE_TTL_MS";
		public static final String CACHE_IDLE_TIMEOUT_MS = "cacheidletimeout";
		public static final String CACHE_CONFIG_FILE_PATH_WITH_NAME = "CACHE_CONFIG_FILE_PATH_WITH_NAME";
		public static final String CACHE_ELEMENT_TTL_MS = "CACHE_ELEMENT_TTL_MS";
		public static final String TIME_TO_WAIT_FOR_DATA_ASYNC_MS = "TIME_TO_WAIT_FOR_DATA_ASYNC_MS";
		public static final String GET_CACHE_DATA_ASYNC = "GET_CACHE_DATA_ASYNC";
		public static final String LOCK_TIMEOUT_MS = "LOCK_TIMEOUT_MS";
		public static final String REGISTRY_LEASE_RENEW_FREQUENCY_MS = "REGISTRY_LEASE_RENEW_FREQUENCY_MS";
		public static final String DME_URI_PART = "DME_URI_PART";
		public static final String DME2_CACHE_MANAGER = "DME2_CACHE_MANAGER";
		public static final String CACHE_DEFAULT_PROFILE = "CACHE_DEFAULT_PROFILE";
		public static final String CACHE_TYPE_CONFIG_FILE_PATH = "CACHE_TYPE_CONFIG_FILE_PATH";
		public static final String CACHE_TYPE_CONFIG_FILE_PATH_DEFAULT = "/conf/cache-types.xml";
		public static final String CACHE_TYPE_CONFIG_FILE_NAME = "CACHE_TYPE_CONFIG_FILE_NAME";
		public static final String CACHE_TYPE_CONFIG_FILE_NAME_DEFAULT = "/conf/cache-types.xml";
		public static final String HZ_CACHE_CONFIG_FILE_NAME = "/hazelcast-config.xml";

		public static final String CACHE_INSTANTIATION_EXCEPTION = " cache implementation Class can't be instantiated";
		public static final String CACHE_SERIALIZATION_EXCEPTION = " cache serializer Class can't be instantiated";
		public static final String CACHE_INTERFACE_IMPLEMENTATION_EXCEPTION = "cache implementation Class must implement - ";
		public static final String CACHE_SERIALIZER_IMPLEMENTATION_EXCEPTION = "cache serializer Class must implement - ";

		public static final String EXCEPTION_HANDLER_MSG = "AFT-DME2-9000";

		public static final String DME2_PERSIST_CACHED_ENDPOINTS_DELAY_MS = "DME2_PERSIST_CACHED_ENDPOINTS_DELAY_MS";
		public static final String DME2_PERSIST_CACHED_ROUTEINFO_DELAY_MS = "DME2_PERSIST_CACHED_ROUTEINFO_DELAY_MS";
		public static final String CACHE_ENABLE_PERSISTENCE = "CACHE_ENABLE_PERSISTENCE";
		public static final String CACHE_FILE_PERSISTENCE_DIR = "CACHE_FILE_PERSISTENCE_DIR";
		public static final String CACHE_PERSISTENCE_WAIT_SCHEDULE_FOR_REFRESH_MS = "CACHE_PERSISTENCE_WAIT_SCHEDULE_FOR_REFRESH_MS";
		public static final String DME2_DISABLE_PERSISTENT_CACHE_LOAD = "AFT_DME2_DISABLE_PERSISTENT_CACHE_LOAD";
		public static final String CACHE_SERIALIZER_CLASS = "CACHE_SERIALIZER_CLASS";
		public static final String CACHE_SERIALIZED_FILE_EXTN = "CACHE_SERIALIZED_FILE_EXTN";
		public static final String CACHE_SERIALIZED_FILE_STALE_TIME_MS = "CACHE_SERIALIZED_FILE_STALE_TIME_MS";
	}

	public static class Iterator {
		public static final String AFT_DME2_PREFERRED_ROUTEOFFER = "AFT_DME2_PREFERRED_ROUTEOFFER";
		public static final String AFT_DME2_PREFERRED_URL = "AFT_DME2_PREFERRED_URL";
		public static final String AFT_DME2_FLAG_FORCE_PREFERRED_ROUTE_OFFER = "AFT_DME2_FORCE_PREFERRED_ROUTE_OFFER";
		public static final String AFT_DME2_ENDPOINT_URL_FORMATTER_IMPL_CLASS = "AFT_DME2_ENDPOINT_URL_FORMATTER_IMPL_CLASS";
		public static final String MAX_THREAD_COUNT_TIMEOUT_CHECKER = "MAX_THREAD_COUNT_TIMEOUT_CHECKER";
		public static final String EVENT_TIMEOUT_TOTAL_WAITING_MS = "EVENT_TIMEOUT_TOTAL_WAITING_MS";
		public static final String EVENT_CHECKER_SCHEDULER_DELAY_MS = "EVENT_CHECKER_SCHEDULER_DELAY_MS";
	}

	public static void setContext(String trackingId, String userName) {
		String user = System.getProperty("user.name");
		LoggingContext.put(LoggingContext.TRACKINGID, trackingId);
		if (userName != null) {
			LoggingContext.put(LoggingContext.USER, userName);
		} else {
			LoggingContext.put(LoggingContext.USER, user);
		}

	}
}
