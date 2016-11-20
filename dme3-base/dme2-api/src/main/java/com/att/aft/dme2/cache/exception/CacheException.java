/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.exception;

public class CacheException extends RuntimeException {
	public enum Severity
	{
		WARN(1),
		ERROR(2);
		
		private int value;
		private Severity(int value) 
		{
			this.value = value;
		}
		public int getValue() 
		{
			return value;
		}
	}
	public enum ErrorCatalogue 
	{
		CACHE_001("unable to retreive cacheable data for cache [Name: %s], [Key: %s], [Value: %s], [Event: %s]", Severity.WARN, "AFT-ERR-TBD"),
		CACHE_002("interrupted the process waiting to retreive cacheable data for cache; will work with the old data [Name: %s], [Key: %s], [Value: %s], [Event: %s]", Severity.WARN, "AFT-ERR-TBD"),
		CACHE_003("Unknown cache event callback [Name: %s], [Event: %s]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_004("unable to reload data [Name: %s], [Key: %s]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_005("unable to obtain lock, some other thread might already be reloading same data [Name: %s], [Key: %s]", Severity.WARN, "AFT-ERR-TBD"),
		CACHE_006("failure to retreive cache data [Key: %s], [Error: %s]", Severity.WARN, "AFT-DME2-0605"),
		CACHE_007("unable to renewAllLeases;[Error: %s]; [%s]", Severity.WARN, "AFT-ERR-TBD"),
		CACHE_008("unable to get DME2EndpointRegistryGRM ;[Error: %s];", Severity.WARN, "AFT-ERR-TBD"),
		CACHE_009("timer cannot invoke method [Error: %s]; Class [%s] Method [%s]", Severity.WARN, "AFT-ERR-TBD"),
		CACHE_010("cache configuration file load failed;[Error: %s]; file path [%s]", Severity.WARN, "AFT-ERR-TBD"),
		CACHE_011("Unknown cache type callback [Name: %s], [Event: %s]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_012("configuration issue; cannot instantiate data handler [Error: %s], [Name: %s]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_013("entry cannot be removed  [Name: %s]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_014("cache by this name already exists  [Name: %s]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_015("cache name cannot be [null]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_016("pre-configured cache type name cannot be [null]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_017("cache configuration for custom cache cannot be [null]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_018("pre-configured cache type cannot be found with cache type name [ %s ]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_019("cache type config file cannot be loaded from the configuration or from the classpath", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_020("cache type config file path is not set properly", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_021("hazelcast cache config file [%s] is not set properly", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_022("Cache persistence/serialize feature is disabled. Skipping operation", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_023("Cache persistence/serialize error while trying to serialize the cache data: Error [%s]", Severity.ERROR, "AFT-ERR-TBD"),
		CACHE_024("Failed to load cache persist file from classpath; file path [%s] has some issue",Severity.WARN, "AFT-ERR-TBD");

		private String message;
		private Severity severity;
		private String aftErrorCode;
		private ErrorCatalogue(String message, Severity severity, String aftErrorCode) 
		{
			this.message = message;
			this.severity = severity;
			this.aftErrorCode = aftErrorCode;
		}
		public String getMessage() 
		{
			return message;
		}
		public Severity getSeverity()
		{
			return severity;
		}
		public String getCode()
		{
			return this.toString();
		}
		public String getAftErrorCode()
		{
			return this.aftErrorCode;
		}
	}	
	private static final long serialVersionUID = 2992095809435518865L;
	private String code = null;
	private String msg = null;
	private String severity = null;

	public CacheException(ErrorCatalogue catalogue, Object... objs) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), objs);
	}

	public CacheException(ErrorCatalogue catalogue, Throwable t, Object... objs) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), t.getLocalizedMessage(),  objs);
	}
	public CacheException(ErrorCatalogue catalogue, Throwable t, String arg1) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), t.getLocalizedMessage(),  arg1);
	}
	public CacheException(ErrorCatalogue catalogue, Throwable t, String arg1, String arg2) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), t.getLocalizedMessage(),  arg1, arg2);
	}
	
	public CacheException(String code, String severity, String format, Object... objs) {
		super("["+severity+":" + code + "]: " + String.format(format, objs));
		this.code = code;
		this.msg = String.format(format, objs);
		this.severity = severity;
	}
	public CacheException(String code, String severity, String format, String arg1) {
		super("["+severity+":" + code + "]: " + String.format(format, arg1));
		this.code = code;
		this.msg = String.format(format, arg1);
		this.severity = severity;
	}
	public CacheException(String code, String severity, String format, String localMsg, String arg1) {
		super("["+severity+":" + code + "]: " + String.format(format, localMsg, arg1));
		this.code = code;
		this.msg = String.format(format, localMsg, arg1);
		this.severity = severity;
	}
	public CacheException(String code, String severity, String format, String localMsg, String arg1, String arg2) {
		super("["+severity+":" + code + "]: " + String.format(format, localMsg, arg1, arg2));
		this.code = code;
		this.msg = String.format(format, localMsg, arg1, arg2);
		this.severity = severity;
	}
	/**
	 * Instantiates a new e http exception.
	 * 
	 * @param code
	 *            the code
	 * @param msg
	 *            the msg
	 */
	/*public CacheException(String code, Throwable msg) {
		super("[" + code + "]: " + msg.getLocalizedMessage(), msg);
		this.code = code;
		this.msg = msg.getLocalizedMessage();
	}
*/
	/**
	 * Gets the error code.
	 * 
	 * @return the error code
	 */
	public String getErrorCode() {
		return code;
	}

	/**
	 * Gets the error message.
	 * 
	 * @return the error message
	 */
	public String getErrorMessage() {
		return msg;
	}

	public CacheException() {
		// TODO Auto-generated constructor stub
	}

}
