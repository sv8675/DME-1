package com.att.aft.dme2.request;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;


public class DmeUniformResource
{

  private String preferredVersion;

  public void setPreferredVersion( String preferredVersion ) {
    this.preferredVersion = preferredVersion;
  }

  public String getPreferredVersion() {
    return preferredVersion;
  }

  public enum DmeUrlType
	{

		/** The DIRECT. */
		DIRECT,

		/** The RESOLVABLE. */
		RESOLVABLE,

		/** The SEARCHABLE. */
		SEARCHABLE,
		
		/** LOGICAL STANDARD URI that can be resolved to find other types 
		 *  CAN BE RESOLVE/SEARCH or a URI that has details required to register a endpoint
		 *  in format of regular url pattern*/
		STANDARD
	}
	private static final Logger logger = LoggerFactory.getLogger( DmeUniformResource.class.getName() );
	
	public static final String STICKY_SELECTOR_KEY = "stickySelectorKey";
	public static final String DATA_CONTEXT_KEY = "dataContext";
	public static final String MATCH_VERSION_RANGE_KEY = "matchVersionRange";
	public static final String PARTNER_KEY = "partner";
	public static final String PREFERRED_ROUTE_OFFER= "preferredRouteOffer";	
	private DME2Configuration config;
	private final URL url;	
	private final String host;
	private final String path;
	private String stickySelectorKey;
	private String dataContext;
	private String envContext;
	private String partner;
	private String routeOffer;
	private String service;
	private String bindContext;
	private String subContext;
	private String version;
	private String userName;
	private String password;
	private String realmName;
	private String loginMethod;
	private String queryParams;
	private String logicalService;
	private String namespace;
	private String supportedVersionRange;
	private String driver;
	private String nonFailoverStatusCodesParam;
	
	private long endpointReadTimeout;
	private long roundTripTimeout;
	private long connectTimeout;
	
	private boolean preferLocalEPs;
	private boolean ignoreFailoverOnExpire;
	private boolean useVersionRange = true;

	private String[] allowedRoles;
	private DmeUrlType urlType;
	
	//Web socket specific parameters
	private int wsConnIdleTimeout;
	private int maxMessageSize = 5000;
	
	private final Map<String, String> queryParamsMap = new HashMap<String, String>();
	
	private String preferredRouteOffer;
	private boolean ignoreWsFailover = false;
	
	private Float throttlePctPerPartner; // = config.getFloat(DME2Constants.AFT_DME2_THROTTLE_PCT_PER_PARTNER);
	private Boolean throttleFilterDisabled;

	public DmeUniformResource(DME2Configuration config, URI inURI) throws MalformedURLException
	{
		this(config, inURI.toURL());
	}
	
	public DmeUniformResource(DME2Configuration config, String inURLString) throws MalformedURLException
	{
		this(config, new URL(inURLString));
	}
	
	public DmeUniformResource(DME2Configuration config, String protocol, String host, int port, String path, URLStreamHandler handler) throws MalformedURLException
	{
		this(config, new URL(protocol, host, port, path, handler));
	}
	
	public DmeUniformResource(DME2Configuration config, URL inURL)
	{
		url = inURL;
		host = inURL.getHost();
		urlType = getDmeUrlType(inURL.getHost());
		path = inURL.getPath();
		this.config = config;
    throttlePctPerPartner = config.getFloat(DME2Constants.AFT_DME2_THROTTLE_PCT_PER_PARTNER);
		processResourcePath(inURL.getPath());
	}
	
	private DmeUrlType getDmeUrlType(String host)
	{
		DmeUrlType type = null;

		if (host == null || host.toUpperCase().equals("DME2SEARCH"))
		{
			type = DmeUrlType.SEARCHABLE;
		}

		/*
		 * FORMAT 2 - RESOLVABLE: (http://DME2RESOLVE/service=?/version=?/envContext=?/routeOffer=?) Used to skip
		 * partition/partner/routeOffer resolution and go directly to a routeOffer
		 */

		else if (host.toUpperCase().equals("DME2RESOLVE"))
		{
			type = DmeUrlType.RESOLVABLE;
		}

		/*
		 * FORMAT 3 - DIRECT: (http://host:port/service=?/version=?/envContext=?/routeOffer=?) Point to a specific host:port and
		 * call the service there. No lookup/resolution.
		 */

		else
		{
			type = DmeUrlType.DIRECT;
		}
		
		return type;
	}
	
	
	private void processResourcePath(String inPath)
	{
		String[] toks = inPath.split("/");
		String inSubContext = null;
		service = getField(inPath, "/service=");
		version = getField(inPath, "/version=");
		partner = getField(inPath, "/partner=");
		envContext = getField(inPath, "/envContext=");
		dataContext = getField(inPath, "/dataContext=");
		routeOffer = getField(inPath, "/routeOffer=");
		bindContext = getField(inPath, "/bindContext=");
		stickySelectorKey = getField(inPath, "/stickySelectorKey=");
		namespace = getField(inPath, "/namespace=");
		driver = getField(inPath, "/driver=");
		preferredRouteOffer = getField(inPath, "/preferredRouteOffer=");
		
		for (String tok : toks) 
		{
			
			if (inSubContext != null)
			{
				inSubContext = inSubContext + "/" + tok;
				continue;
			}
			
			
			final String[] pair = tok.split("=");
			final String key = pair[0];

			if (key.equalsIgnoreCase("subcontext"))
			{
				inSubContext = "";
				continue;
			}
			
		}// End Loop
		
		// If service is not found from context path from DME2RESOLVE or DME2SEARCH or provided DME2LOCAL/URI string then
		// the URI path could be of format http://servicename/ContextPath?version=&envContext=&partner=&routeOffer=&stickySelectorKey=
		// Identify service name and additional details from URI hostname & query params
		
		if ( (service == null) || ( version == null && envContext == null))
		{
			// Check query params to see if version/envContext/routeOffer/partner info are available from there.
			String qParams = url.getQuery();
			if (qParams != null)
			{
				String[] queryToks = qParams.split("&");

				for (String qtok : queryToks)
				{
					String[] pair = qtok.split("=");

					if (pair.length == 2)
					{
						final String key = pair[0];
						final String value = pair[1];
						
						if (key.equalsIgnoreCase("routeoffer")){
							routeOffer = value;
						}
						else if (key.equalsIgnoreCase("version")) {
							version = value;
						}
						else if (key.equalsIgnoreCase("envcontext")) {
							envContext = value;
						}
						else if (key.equalsIgnoreCase("stickyselectorkey")){
							stickySelectorKey = value;
						}
						else if (key.equalsIgnoreCase(DATA_CONTEXT_KEY)){
							dataContext = value;
						}
						else if (key.equalsIgnoreCase(PARTNER_KEY))	{
							partner = value;
						}
						else if (key.equalsIgnoreCase("ns")){
							namespace = value;
						}
						else if (key.equalsIgnoreCase("subcontext")){
							inSubContext = value;
						}
						else if (key.equalsIgnoreCase("preferredRouteOffer")){
							preferredRouteOffer = value;
						}
					}
				}
			}
			// If serviceName is not derived in above steps, then the URI is of type STANDARD
			// where hostname will be used as service.
			if(this.urlType != DmeUrlType.RESOLVABLE && this.urlType != DmeUrlType.SEARCHABLE) {
				if (version != null && envContext != null && (routeOffer != null || partner != null) && service == null)
				{				
				String uriServiceName = url.getHost();
				int port = url.getPort();
				String serviceUri = null;

				if (this.getNamespace() == null){
					serviceUri = getReversed(uriServiceName);
				}
				else{
					serviceUri = uriServiceName;
				}

				if (url.getPath() != null && !url.getPath().equals("/"))
				{
					this.bindContext = url.getPath();
				}
				
				// / Append bind context to service name
				String cpath = null;
				if (this.bindContext != null)
				{
					cpath = this.bindContext.startsWith("/") ? this.bindContext.substring(1, this.bindContext.length())
							: this.bindContext;
				}
				
				if (this.bindContext != null && cpath != null)
				{
					this.service = serviceUri + "/" + cpath.replaceAll("\\.", "\\\\\\\\.");
				}
				else
				{
					this.service = serviceUri;
				}
				
				// Append bind context to service name
		
				// If the service name is successfully retrieved from the URI hostname, then its using standard web address/uri
				if (this.service != null && port<=0){
					urlType = DmeUrlType.STANDARD;
				}
			}
		  }
		}
		subContext = inSubContext;
		processQueryString(url.getQuery());
	}
	
	private void processQueryString(String inQueryStr)
	{
		if (inQueryStr != null) 
		{
			final String[] queryToks = inQueryStr.split("&");
			
			for (String qtok : queryToks) 
			{
				final String[] pair = qtok.split("=");
				
				if(pair.length==2) 
				{
					final String key = pair[0];
					final String value = pair[1];
					if (key.equalsIgnoreCase("endpointreadtimeout")) 
					{
						try 
						{
							// validate if endpointReadTimeout is more than maximum
							// defined ( 5 mins )
							this.endpointReadTimeout = Math.min(Long.parseLong(value), config.getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT));
						}catch (Exception e) {
							logger.debug( null, "processQueryString", "Exception", e );
						/* ignore error in parsing */ 
						}
					} 
					else if (key.equalsIgnoreCase("roundtriptimeout"))
					{
						try 
						{
							// validate if roundTripTimeout is more than maximum
							// defined ( 4 mins )
							this.roundTripTimeout = Math.min(Long.parseLong(value), config.getLong(DME2Constants.AFT_DME2_DEF_ROUNDTRIP_TIMEOUT_MS));
						} 
						catch (Exception e)
						{
							
							this.roundTripTimeout = config.getLong(DME2Constants.AFT_DME2_DEF_ROUNDTRIP_TIMEOUT_MS);
						}
					} 
					else if (key.equalsIgnoreCase("preferlocal")){
						preferLocalEPs = true;
					}
					else if (key.equalsIgnoreCase("ignorefailoveronexpire")){
						ignoreFailoverOnExpire = true;
					}
					else if (key.equalsIgnoreCase("ignoreWsfailover")){
						ignoreWsFailover = true;
					}
					else if (key.equalsIgnoreCase("connecttimeoutinms")) 
					{
						try 
						{
							// validate if connectTime is more than maximum
							// defined ( 4 secs )
							this.connectTimeout = Math.min(Long.parseLong(value), config.getLong(DME2Constants.AFT_DME2_EP_CONN_TIMEOUT));
						} 
						catch (Exception e) {
              logger.debug( null, "processQueryString", "Exception",e);
						/* ignore error in parsing */ }
					} 
					else if (key.equalsIgnoreCase("username")) 	{
						userName = value;
					}
					else if (key.equalsIgnoreCase("password")) {
						password = value;
					}
					else if (key.equalsIgnoreCase("realm")) 	{
						realmName = value;
					}
					else if (key.equalsIgnoreCase("loginmethod")) {
						loginMethod = value;
					}
					else if (key.equalsIgnoreCase("allowedroles") && value!=null){
						allowedRoles = value.split(",");
					}
					else if(key.equalsIgnoreCase(MATCH_VERSION_RANGE_KEY)) 	{
						useVersionRange = Boolean.parseBoolean(value);
					}
					else if(key.equalsIgnoreCase("supportedVersionRange")) 	{
						supportedVersionRange = value;
					}
					else if(key.equalsIgnoreCase("dme2NonFailoverStatusCodes")) {
						nonFailoverStatusCodesParam = value;
					} else if (key.equalsIgnoreCase("wsConnIdleTimeout")) {
            try {
              // validate if connectTime is more than maximum
              // defined ( 4 secs )
              this.wsConnIdleTimeout =
                  Integer.parseInt( value ) == 0 ? config.getInt( DME2Constants.AFT_DME2_DEF_WS_IDLE_TIMEOUT ) :
                      Integer.parseInt( value );
            } catch ( Exception e ) {
              logger.debug( null, "processQueryString", "Exception", e );
            }
          } else if ( "throttleFilterDisabled".equalsIgnoreCase( key )) {
            try {
              throttleFilterDisabled = Boolean.valueOf( value );
            } catch ( Exception e ) {
              logger.error( null, "processQueryString", "Exception processing throttleFilterDisabled", e );
            }
          } else if ( "throttlePctPerPartner".equalsIgnoreCase( key )) {
            logger.debug( null, "processQueryString", "Found throttlePctPerPartner of {}", value );
            try {
              throttlePctPerPartner = Float.valueOf( value );
            } catch ( Exception e ) {
              this.throttlePctPerPartner = config.getFloat( DME2Constants.AFT_DME2_THROTTLE_PCT_PER_PARTNER );
              logger.error( null, "processQueryString", "Exception processing throttlePctPerPartner", e );
            }
					} else {
						if (queryParams==null) {
							queryParams = key + "=" + value;							
						}
						else{
							queryParams += "&" + key + "=" + value;
						}
					}
					
					queryParamsMap.put(key, value);
				}
			}
		}
	}
	
	/**
	 * Will return reversed string split by . E.g input =
	 * TestService.dme2.att.com , return = com.att.dme2.TestService
	 * 
	 * @param uriServiceName
	 * @return
	 */
	private String getReversed(String uriServiceName) {
		if(isIPAddress(uriServiceName)) {
			return null;
		}
		String domainNameArr[] = uriServiceName.split("\\.");
		StringBuffer reversedString = new StringBuffer();
		for (int i = domainNameArr.length - 1; i >= 0; i--) {
				if (i != 0){
					reversedString.append(domainNameArr[i] + ".");
				}
				else{
					reversedString.append(domainNameArr[i]);
				}
		}
		return reversedString.toString();
	}

	/**
	 * Returns true if input string represents an ip addresss.
	 * @param ipAddress
	 * @return
	 */
	private boolean isIPAddress(String ipAddress) {
		String[] tokens = ipAddress.split("\\.");
		if (tokens.length != 4) {
			return false;
		}
		for (String str : tokens) {
			try{
			int i = Integer.parseInt(str);
			if ((i < 0) || (i > 255)) {
				return false;
			}
			} catch(Exception e){
				return false;
			}
		}
		return true;
	}
	
	public String getRegistryFindEndpointSearchKey()
	{
		
		if(!service.contains("/")) {
			return service;
		}
		final String uriServiceName = service.split("/", 2)[0];
		if(namespace == null){
			return uriServiceName + "*";
		}
		else{
			return namespace + DME2Constants.getNAME_SEP() + uriServiceName + "*";
		}
	}
	
	public String getRouteInfoServiceSearchKey()
	{
		final String uriServiceName = service.split("/", 2)[0];
		if(namespace == null) {
			return uriServiceName;
		}
		else {
			return namespace + DME2Constants.getNAME_SEP() + uriServiceName;
		}
	}


	/**
	 * Assert valid.
	 */
	public void assertValid() throws DME2Exception {
		ErrorContext ec = new ErrorContext().add("URL", url.toString());

		switch (urlType) {
			case SEARCHABLE:
				if (partner == null){
					throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_9703, ec);
				}
				break;
	
			case RESOLVABLE:
			case DIRECT:
			case STANDARD:
				if (routeOffer == null && partner == null) {
					throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_9704, ec);
				}
				break;
				
			default:
				throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_9705, ec);
		}			
		if (service == null){
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_9700, ec);
		}
		if (version == null) {
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_9701, ec);
		}
		if (envContext == null) {
			throw new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_9702, ec);
		}
	}

	public URL getUrl()
	{
		return url;
	}

	public String getHost()
	{
		return host;
	}

	public String getPath()
	{
		return path;
	}

	public String getDataContext()
	{
		return dataContext;
	}

	public String getEnvContext()
	{
		return envContext;
	}

	public String getPartner()
	{
		return partner;
	}

	public String getRouteOffer()
	{
		return routeOffer;
	}

	public String getService()
	{
		return service;
	}

	public String getBindContext()
	{
		return bindContext;
	}

	public String getSubContext()
	{
		return subContext;
	}

	public void setSubContext(String subContext)
	{
		this.subContext = subContext;
	}

	public String getVersion()
	{
		return preferredVersion == null ? version : preferredVersion;
	}

	public String getUserName()
	{
		return userName;
	}

	public String getPassword()
	{
		return password;
	}

	public String getRealmName()
	{
		return realmName;
	}

	public String getLoginMethod()
	{
		return loginMethod;
	}

	public String getQueryParams()
	{
		return queryParams;
	}

	public String getLogicalService()
	{
		return logicalService;
	}

	public String getNamespace()
	{
		return namespace;
	}

	public String getSupportedVersionRange()
	{
		return supportedVersionRange;
	}
	
	public String getNonFailoverStatusCodesParam()
	{
		return nonFailoverStatusCodesParam;
	}

	public long getEndpointReadTimeout()
	{	
		return endpointReadTimeout;
	}

	public long getRoundTripTimeout()
	{
		return roundTripTimeout;
	}

	public long getConnectTimeout()
	{
		return connectTimeout;
	}

	public boolean isPreferLocalEPs()
	{
		return preferLocalEPs;
	}

	public boolean isIgnoreFailoverOnExpire()
	{
		return ignoreFailoverOnExpire;
	}

	public boolean isUseVersionRange()
	{
		return useVersionRange;
	}

	public String[] getAllowedRoles()
	{
		return allowedRoles;
	}

	public DmeUrlType getUrlType()
	{
		return urlType;
	}

	public String getStickySelectorKey()
	{
		return stickySelectorKey;
	}
	
	public URL getOriginalURL(){
		return url;
	}
	

	public boolean isUsingVersionRanges()
	{
		return useVersionRange;
	}
	
	@Override
	public String toString() {
		return url.toString();
	}

	public Map<String, String> getQueryParamsMap()
	{
		return queryParamsMap;
	}

	public String getDriver()
	{
		return driver;
	}
	
	private String getField (String servletPath, String field){
		String fieldsWithSlash = config.getProperty(DME2Constants.DME2_URI_FIELD_WITH_PATH_SEP,DME2Constants.DME2_URI_FIELD_WITH_PATH_SEP_DEF);
		boolean canEndWithSlash=false;
		if(fieldsWithSlash != null){
		 canEndWithSlash = fieldsWithSlash.contains(field);
		}
		return getField(servletPath,field,canEndWithSlash);
	}
	
	private String getField (String servletPath, String field, boolean canEndWithSlash){
		int indexOfField=-1;
		indexOfField = servletPath.toLowerCase().indexOf(field.toLowerCase());
		if(indexOfField != -1){
			try {
				String fieldVal = null;
			// Identify the next field using = sign. Requirement is all input keys will only have = appended to it. 
			// service=com.att.aft.Restful/restful/context/envContext=TEST/version=1.0.0/
			int indexOfNext = servletPath.indexOf("=",indexOfField+field.length());
				// If a next field is found
				if(indexOfNext != -1){
					// Parse till next field
					// com.att.aft.Restful/restful/context/envContext
					String temp = servletPath.substring(indexOfField, indexOfNext);
					// temp value "com.att.aft.Restful/restful/context/envContext"
					fieldVal = temp.substring(temp.indexOf("=")+1, temp.lastIndexOf("/"));
					// field value "service=com.att.aft.Restful/restful/context"
				
				}
				else {
					// Most of the fields cannot end with / in it except serviceName or subContext fields
					// Identify if servletPath ends with / for a field that should not carry / in it still
					// E.g service=com.att.csi.m2e.Echo/version=1.0.0/envContext=TEST/
					if(!canEndWithSlash && servletPath.endsWith("/")) {
							fieldVal = servletPath.substring(indexOfField+field.length(),servletPath.length()-1);
					}
					else{
							fieldVal = servletPath.substring(indexOfField+field.length());
					}
				}
				// If values carry "/" in it for fields that do not have / supported in its name E.g version, envContext,partner,routeOffer
				// replace all / with ""
				if(!canEndWithSlash ){
					if(fieldVal != null && fieldVal.contains("/")) {
						fieldVal = fieldVal.replaceAll("/", "");
					}
				}
				// If field is not null, contains / , only once and at the end of the string, then replace that with blank
				// E.g DME2SEARCH/version=1.0/envContext=PROD/service=com.att.aft.MyService/
				if(fieldVal != null && fieldVal.contains("/") && (fieldVal.replaceAll("/","").length()==fieldVal.length()-1) &&  fieldVal.indexOf("/") == fieldVal.length()-1){
							fieldVal = fieldVal.replace("/", "");
				}
				return fieldVal!=null?(fieldVal.length()>0?fieldVal:null):null;
			} catch(Exception e){
				// Return null for any unhandled exception in parsing the fields while looking for index
				return null;
			}
		}
		return null;
	}

	public long getWsConnIdleTimeout() {
		return wsConnIdleTimeout;
	}

	public int getMaxMessageSize() {
		return maxMessageSize;
	}

	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	public String getPreferredRouteOffer() {
		return this.preferredRouteOffer;
	}

	public boolean isIgnoreWsFailover() {
		return ignoreWsFailover;
	}
	
	public Float getThrottlePctPerPartner() {
		return throttlePctPerPartner;
	}

	public void setThrottlePctPerPartner(Float throttlePctPerPartner) {
		this.throttlePctPerPartner = throttlePctPerPartner;
	}

	public Boolean getThrottleFilterDisabled() {
		return throttleFilterDisabled;
	}

	public void setThrottleFilterDisabled(Boolean throttleFilterDisabled) {
		this.throttleFilterDisabled = throttleFilterDisabled;
	}
	
	public void setPreferLocalEPs(boolean preferLocalEPs) {
		this.preferLocalEPs = preferLocalEPs;
	}
	
	public void setUseVersionRange(boolean useVersionRange) {
		this.useVersionRange = useVersionRange;
	}
	public void setRoundTripTimeout(long roundTripTimeout) {
		this.roundTripTimeout = roundTripTimeout;
	}
}
