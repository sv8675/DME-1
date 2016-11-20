package com.att.aft.dme2.iterator.exception;

public class IteratorException extends RuntimeException{
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
	public IteratorException() {
	}
	public enum IteratorErrorCatalogue 
	{
		ITERATOR_001("serviceUri cannot be null", Severity.ERROR, "AFT-ERR-TBD"),
		ITERATOR_002("metrics collection has not been started, please start using EndpointIteratorMetricsCollection.start()", Severity.ERROR, "AFT-ERR-TBD"),
		ITERATOR_003("Conversation Id cannot be null or empty", Severity.ERROR, "AFT-ERR-TBD"),
		ITERATOR_004("Config cannot be null or empty", Severity.ERROR, "AFT-ERR-TBD");
		private String message;
		private Severity severity;
		private String aftErrorCode;
		private IteratorErrorCatalogue(String message, Severity severity, String aftErrorCode) 
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

	public IteratorException(IteratorErrorCatalogue catalogue, Object... objs) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), objs);
	}

	public IteratorException(IteratorErrorCatalogue catalogue, Throwable t, Object... objs) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), t.getLocalizedMessage(),  objs);
	}
	public IteratorException(IteratorErrorCatalogue catalogue, Throwable t, String arg1) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), t.getLocalizedMessage(),  arg1);
	}
	public IteratorException(IteratorErrorCatalogue catalogue, Throwable t, String arg1, String arg2) { 
		this(catalogue.getCode(), catalogue.getSeverity().toString(), catalogue.getMessage(), t.getLocalizedMessage(),  arg1, arg2);
	}
	
	public IteratorException(String code, String severity, String format, Object... objs) {
		super("["+severity+":" + code + "]: " + String.format(format, objs));
		this.code = code;
		this.msg = String.format(format, objs);
		this.severity = severity;
	}
	public IteratorException(String code, String severity, String format, String arg1) {
		super("["+severity+":" + code + "]: " + String.format(format, arg1));
		this.code = code;
		this.msg = String.format(format, arg1);
		this.severity = severity;
	}
	public IteratorException(String code, String severity, String format, String localMsg, String arg1) {
		super("["+severity+":" + code + "]: " + String.format(format, localMsg, arg1));
		this.code = code;
		this.msg = String.format(format, localMsg, arg1);
		this.severity = severity;
	}
	public IteratorException(String code, String severity, String format, String localMsg, String arg1, String arg2) {
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
}
