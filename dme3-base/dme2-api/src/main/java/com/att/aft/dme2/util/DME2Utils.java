package com.att.aft.dme2.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.scld.grm.types.v1.NameValuePair;
import com.att.scld.grm.types.v1.VersionDefinition;

public class DME2Utils {

	private static final Logger logger = LoggerFactory.getLogger(DME2Utils.class.getName());

	private DME2Utils() {

	}

	public static List<NameValuePair> convertPropertiestoNameValuePairs(Properties props)
	{
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

		if(props != null) {
			for(Object obj : props.keySet()) {
				String key = (String) obj;
				String value =  props.getProperty(key);

				NameValuePair nameValPair = new NameValuePair();
				nameValPair.setName(key);
				nameValPair.setValue(value);
				nameValuePairs.add(nameValPair);
			}
		}

		return nameValuePairs;
	}

	public static Properties convertNameValuePairToProperties( List<NameValuePair> properties ) {
		return null;
	}


	public static boolean isInIgnoreList(DME2Configuration config, String queueName){
		String ignoreList = config.getProperty(DME2Constants.AFT_DME2_METRICS_SVC_LIST_IGNORE);
		String splitStr[] = ignoreList.split(",");
		logger.info(null, "isInIgnoreList", " inside isInIgnoreList queueName : {}", queueName);
		for(int i=0; i<splitStr.length;i++) {
			if ( queueName.contains(splitStr[i])) {
				logger.debug( null, "isInIgnoreList", "DME2EventProcessor ignoring stats since service name is in ignore list serviceName={};IgnoreList={}",  queueName, ignoreList);
				logger.debug( null, "isInIgnoreList", LogMessage.IGNORE_STATS, queueName,ignoreList);
				logger.info(null, "isInIgnoreList", " inside isInIgnoreList queueName : {} : is in ignore list ", queueName);
				return true;
			}
		}
		return false;
	}

	public static boolean isCurrentHourMillis(long millis) {
		Calendar cal = Calendar.getInstance( DME2Manager.getTimezone());

		Calendar cal1 = Calendar.getInstance(DME2Manager.getTimezone());
		cal1.setTimeInMillis(millis);
		if (cal1.get(Calendar.HOUR_OF_DAY) == cal.get(Calendar.HOUR_OF_DAY)) {
			return true;
		}

		return false;
	}

	public static String formatClientURIString(final String newInString)
	{

		String inString=newInString;
		final String searchPrefix = "http://DME2SEARCH";
		final String resolvePrefix = "http://DME2RESOLVE";


		if(!inString.startsWith("HTTP") && !inString.startsWith("http") && !inString.startsWith("dme2") && !inString.startsWith("ws"))
		{
			/* if client String doesn't have leading slash, add it */
			if(!inString.startsWith("/")){
				inString = "/" + inString;
			}

			if(inString.contains("partner"))
			{
				inString = searchPrefix + inString;
			}
			else if(inString.contains("routeOffer"))
			{
				inString = resolvePrefix + inString;
			}			
		}

		return inString;
	}

	public static boolean isHostMyLocalHost(String host, boolean isDebugEnabled) {
		logger.debug(null, "isHostMyLocalHost", "inside DME2Utils.isHostMyLocalHost : {}", host);
		InetAddress in = null;
		try {
			in = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			return false;
		}
		// if the address is a valid special local or loop back
		if (in.isAnyLocalAddress() || in.isLoopbackAddress()) {
			logger.debug(null, null, "DME2Constants.isHostMyLocalHost isAnyLocalAddress||isLoopbackAddress;" + host);
			return true;
		}
		// Else if the address is defined on any of local network interface
		try {
			return NetworkInterface.getByInetAddress(in) != null;
		} catch (Throwable e) {
			return false;
		}
	}

	public static String buildServiceURIString(String serviceName, String version, String envContext)
	{
		if (serviceName == null || version == null || envContext == null)
		{
			return null;
		}

		return String.format("/service=%s/version=%s/envContext=%s", serviceName, version, envContext);
	}

	public static String buildServiceURIString(String serviceName, String version, String envContext, String routeOffer)
	{
		if (serviceName == null || version == null || envContext == null || routeOffer == null)
		{
			return null;
		}

		return String.format("/service=%s/version=%s/envContext=%s/routeOffer=%s", serviceName, version, envContext, routeOffer);
	}

	public static String encodeURIString(String uriStr, boolean isEncoded) 
	{
		String encodedStr = uriStr;

		try
		{
			String uriPrefix = null;
			int contextPathIndex = -1;

			if(uriStr.contains("/service"))
			{
				contextPathIndex = uriStr.indexOf("/service", 0);
				uriPrefix = uriStr.substring(0, contextPathIndex); /* Extract the prefix from the URI string (i.e. http://DME2SEARCH or http://DMELOCAL)*/
			}
			else if (uriStr.contains("%2Fservice")) 
			{
				contextPathIndex = uriStr.indexOf("%2Fservice", 0);
				uriPrefix = URLDecoder.decode(uriStr.substring(0, contextPathIndex), "UTF-8"); /* Extract the prefix from the URI string (i.e. http://DME2SEARCH or http://DMELOCAL)*/
			}

			if(!isEncoded)
			{
				/* Get the contextPath from the URI string and encode special characters */
				String contextPath = uriStr.substring(contextPathIndex);
				String encodedContextPath = URLEncoder.encode(contextPath, "UTF-8");
				String finalEncodedURIStr = uriPrefix + encodedContextPath;
				encodedStr =  finalEncodedURIStr.replace("%2F", "/").replace("%3D", "=").replace("%3F", "?").replace("%26", "&");
			}
			else
			{
				/* If we get here, the URI input string was already encoded*/
				String encodedContextPath = uriStr.substring(contextPathIndex);
				String finalEncodedURIStr = uriPrefix + encodedContextPath;
				encodedStr =  finalEncodedURIStr.replace("%2F", "/").replace("%3D", "=").replace("%3F", "?").replace("%26", "&");
			}
		}
		catch (Exception e)
		{
			logger.warn( null, "encodeURIString", "AFT-DME2-0607", new ErrorContext().add("URI", uriStr), e);
		}

		return encodedStr;
	}

	public static boolean isParseable(String value, Class<?> targetType)
	{
		try
		{
			if (targetType == Integer.class)
			{
				Integer.parseInt(value);
			}
			else if (targetType == Long.class)
			{
				Long.parseLong(value);
			}
			else if (targetType == Double.class)
			{
				Double.parseDouble(value);
			}
		}
		catch(NumberFormatException e)
		{
			return false;
		}

		return true;
	}

	/**Adds query string to the URI path string. If no query string was passed in then the path is just returned.*/
	public static String appendQueryStringToPath(String path,final String newQueryStr)
	{
		String queryStr=newQueryStr;
		if(queryStr == null){
			return path;
		}

		if(!queryStr.startsWith("?")){
			queryStr = "?" + queryStr;
		}
		return path + queryStr;
	}

	public static Map<String, String> splitServiceURIString(final String newServiceURI)
	{
		String serviceURI=newServiceURI;
		Map<String, String> serviceURIValues = new HashMap<String, String>();

		if(!serviceURI.startsWith("/")){
			serviceURI = "/" + serviceURI;
		}
		String[] toks = serviceURI.split("/");

		for(String tok : toks)
		{
			if(tok.contains("="))
			{
				String[] pair = tok.split("=");
				String key = pair[0];
				String value = pair[1];

				serviceURIValues.put(key, value);
			}
		}

		return serviceURIValues;
	}

	/**
	 *
	 * @return
	 */
	public static String getRunningInstanceName(DME2Configuration config) {
		// if this container is LRM managed, use LRM provided JVM args to decide on serviceName
		// If application user is setting a uniq name to use, use it as override for LRM attributes

		String userProvidedServiceNameStr = config.getProperty("AFT_DME2_PF_SERVICE_NAME");

		if(userProvidedServiceNameStr != null){
			return userProvidedServiceNameStr;
		}

		String resName = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_NAME_KEY));

		if(resName != null) {
			// Chances are this is a LRM managed container
			String resVer = config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_VERSION_KEY));
			String resEnv= config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_ENV_KEY));
			String resRO= config.getProperty(config.getProperty(DME2Constants.AFT_DME2_CONTAINER_ROUTEOFFER_KEY));
			if(resName != null && resVer != null && resRO != null && resEnv != null) {
				return resName + "/" + resVer + "/" + resEnv + "/" + resRO;
			}
		}
		else {
			return null;
		}
		return null;
	}	

	public static Object loadClass(DME2Configuration config, String url, String handlerName) throws Exception {
		Object obj = null;
		Class<?> cls = null;

		try {
			cls = Thread.currentThread().getContextClassLoader().loadClass(handlerName);
			obj = cls.newInstance();
			return obj;
		} catch (Exception e) {
			// if any exception, try loading from ClassLoader
			if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
				logger.error(null, "loadClass", DME2Constants.AFT_DME2_0712,
						new ErrorContext().add("ServerURL", url).add("handlerName", handlerName),
						e);
			}
		}
		try {
			// Check system classloader
			cls = Class.forName(handlerName);
			obj = cls.newInstance();
			return obj;
		} catch (Exception e) {
			// if any exception, try loading from ClassLoader
			if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
				logger.error(null, "loadClass", DME2Constants.AFT_DME2_0712,
						new ErrorContext().add("ServerURL", url).add("handlerName", handlerName),
						e);
			}
		}
		try {
			// try with current class classLoader
			cls = DME2Utils.class.getClassLoader().loadClass(handlerName);
			obj = cls.newInstance();
			return obj;
		} catch (Exception e) {
			if (config.getBoolean(DME2Constants.DME2_DEBUG)) {
				logger.error(null, "loadClass", DME2Constants.AFT_DME2_0712,
						new ErrorContext().add("ServerURL", url).add("handlerName", handlerName),
						e);
			}
			throw new DME2Exception(DME2Constants.AFT_DME2_0712,
					new ErrorContext().add("ServerURL", url).add("handlerName", handlerName));
		}
	}

	public static String[] getFailoverHandlers(DME2Configuration config, Map<String, String> headers) {
		String defaultHandler = "com.att.aft.dme2.handler.DefaultLoggingFailoverFaultHandler"; 

		Set<String> replyHandlers = new HashSet<String>();
		if(headers != null) {
			// get the configured handlers from the request
			String handlers = headers.get(config.getProperty(DME2Constants.AFT_DME2_EXCHANGE_FAILOVER_HANDLERS_KEY));


			if (handlers != null && handlers.length() > 0) {
				try {
					for (String handler : handlers.split(",")) {
						replyHandlers.add(handler);
					}
				} catch (Exception e) {
					logger.debug(null, "getFailoverHandlers", LogMessage.EXCH_READ_HANDLER_FAIL, "getFailoverHandlers", e);
				}
			}
		}

		// if we are configured for logging failovers
		if (config.getBoolean(DME2Constants.AFT_DME2_ENABLE_FAILOVER_LOGGING)) {
			replyHandlers.add(defaultHandler);
		}

		logger.debug(null, "getFailoverHandlers", "FAILOVER_HANDLERS_CHAIN_HEADER_PROPERTY", replyHandlers.toString());

		if (replyHandlers.size() > 0) {
			return replyHandlers.toArray(new String[0]);
		} else {
			return null;
		}
	}

	public static Map<String,String> getQueryParamsAsMap(String qp){
		if(qp == null) {
			return new HashMap<String,String>();
		}
		else {
			String[] qpArr = qp.split("&");
			HashMap<String,String> qpMap = new HashMap<String,String>();
			for(int i=0;i<qpArr.length;i++) {
				String qpParamArr[] = qpArr[i].split("=");
				if(qpParamArr != null && qpParamArr.length==2) {
					qpMap.put(qpParamArr[0], qpParamArr[1]);
				}
			}
			return qpMap;
		}
	}

	public static VersionDefinition buildVersionDefinition(DME2Configuration config, String version){
		DME2ValidationUtil.validateVersionFormat(config, version);

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
}