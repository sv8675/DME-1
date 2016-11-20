package com.att.aft.dme2.logging;

import java.util.LinkedHashMap;


/**
 * Contains contextual information used for logging purposes.
 * Values set here are local to the current running thread.  
 * Values needed on another thread must be moved there by the execution code.
 */
public class LoggingContext {

	private static ThreadLocal<LinkedHashMap<String,String>> ctxThreadLocal = new ThreadLocal<LinkedHashMap<String,String>>();
	
	/**
	 * Put a new value or override an existing value in the current thread's context
	 * @param key
	 * @param value
	 */
	public static void addContextParam(String name, String value) {
		LinkedHashMap<String, String> ctxMap = getCurrentCtxMap();
		ctxMap.put(name, value);
	}
	
	/**
	 * Return the internal map of the current context
	 * @return
	 */
	public static LinkedHashMap<String,String> getCurrentCtxMap() {
		LinkedHashMap<String,String> ctxMap = ctxThreadLocal.get();
		synchronized (LoggingContext.class) {
			if (ctxMap == null) {
				ctxMap = new LinkedHashMap<String, String>();
				ctxThreadLocal.set(ctxMap);
			}
		}
		return ctxMap;
	}
	
	/**
	 * Put a new value or override an existing value in the current thread's context
	 * @param key
	 * @param value
	 */
	public static void put(String key, String value) {
		addContextParam(key, value);
	}
	
	/**
	 * Get the value of an entry in the current context.  If the context does 
	 * not contain the value, return null.
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String get(String key) {
		return get(key, null);
	}
	
	/**
	 * Get the value of an entry in the current context.  If the context does 
	 * not contain the value, return the provided defaultValue.
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String get(String key, String defaultValue) {
		LinkedHashMap<String,String> map = ctxThreadLocal.get();
		if (map == null) {
			return defaultValue;
		}
		String value = map.get(key);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}
	
	/**
	 * Destroy and recreate the current thread's context
	 */
	public static void reset() {
		LinkedHashMap<String,String> ctxMap = new LinkedHashMap<String, String>();
		ctxThreadLocal.set(ctxMap);
	}
	
	/**
	 * Clear out any currently cached context for the thread
	 */
	public static void clear() {
		ctxThreadLocal.remove();
	}
	
	public static final String TRACKINGID = "trackingid";
	public static final String USER = "user";
	
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger("com.att.test.Logger");
		
		// record operation start time
		long start = System.currentTimeMillis();
		try {

			// set context
			LoggingContext.put(LoggingContext.TRACKINGID, "ID:38273823743823");
			LoggingContext.put(LoggingContext.USER, "xy9876");

			// do stuff here ....
			
			// record elapsed time
			long elapsed = System.currentTimeMillis() - start;
			
			// log success
			logger.info(null, "LoggingContext.main", "Code=Server.Reply; Result=Success; Elapsed="+elapsed+"; Service=MyService; Version=MyVersion;");

		} catch (Throwable e) {
			// record elapsed time
			long elapsed = System.currentTimeMillis() - start;
			
			// log success
			logger.warn(null, "LoggingContext.main", "Code=Server.Reply; Result=Fault; Elapsed="+elapsed+"; Service=MyService; Version=MyVersion; Exception=" + e.toString());
			logger.warn(null, "LoggingContext.main", "Thrown Exception", e);
		} finally {
			LoggingContext.clear();
		}
	}
}