package com.att.aft.dme2.util;

import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A catalog of Error ResourceBundles that can be queried with a unique 
 * errorCode and returns the human-readable text.  If no matching text is 
 * found, we return a generic message indicating the error could not be found
 * in the catalog along with the passed context information.
 */
public class ErrorCatalog {
	protected static ErrorCatalog instance = new ErrorCatalog();
	private static Set<ResourceBundle> errorBundles = new HashSet<ResourceBundle>();
	
	protected ErrorCatalog() {	
	}

	public static final ErrorCatalog getInstance() {
		return instance;
	}
	
	public void addErrorTable(ResourceBundle errorBundle) {
		errorBundles.add(errorBundle);
	}
	
	public final String getErrorMessage(String errorCode, ErrorContext context) {
		StringBuffer buf = new StringBuffer();
		String msg = null;
		for (ResourceBundle errorTable: errorBundles) {
			try {
				msg = errorTable.getString(errorCode);
			} catch (MissingResourceException mre) {
			}
			
			if (msg == null) {
				continue;
			}
		}
		
		if (msg == null) {
			msg = "Error occured but no localized message is available in the ErrorCatalog, review error code in documentation for more details.";
		}
		
		buf.append(msg);
		
		if (context != null && context.size() > 0) {
			buf.append(" [Context: " );
			for (String key: context.keySet()) {
				buf.append(key);
				buf.append("=");
				buf.append(context.get(key));
				buf.append(";");
			}
			buf.append("]");
		}
		
		return buf.toString();
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logInfo(Logger logger, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.INFO, errorCode, context, e);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logInfo(Logger logger, String methodName, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.INFO, methodName, errorCode, context, e);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 */
	public static final void logInfo(Logger logger, String errorCode, ErrorContext context) {
		log(logger, Level.INFO, errorCode, context, null);
	}	
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 */
	public static final void logInfo(Logger logger, String methodName, String errorCode, ErrorContext context) {
		log(logger, Level.INFO, methodName, errorCode, context, null);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logWarning(Logger logger, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.WARNING, errorCode, context, e);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logWarning(Logger logger, String methodName, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.WARNING, methodName, errorCode, context, e);
	}
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 */
	public static final void logWarning(Logger logger, String errorCode, ErrorContext context) {
		log(logger, Level.WARNING, errorCode, context, null);
	}	
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 */
	public static final void logWarning(Logger logger, String methodName, String errorCode, ErrorContext context) {
		log(logger, Level.WARNING, methodName, errorCode, context, null);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logSevere(Logger logger, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.SEVERE, errorCode, context, e);

	}

	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logSevere(Logger logger, String methodName, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.SEVERE, methodName, errorCode, context, e);
	}

	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 */
	public static final void logSevere(Logger logger, String errorCode, ErrorContext context) {
		log(logger, Level.SEVERE, errorCode, context, null);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 */
	public static final void logSevere(Logger logger, String methodName, String errorCode, ErrorContext context) {
		log(logger, Level.SEVERE, methodName, errorCode, context, null);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logConfig(Logger logger, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.CONFIG, errorCode, context, e);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logConfig(Logger logger, String methodName, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.CONFIG, methodName, errorCode, context, e);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 */
	public static final void logConfig(Logger logger, String errorCode, ErrorContext context) {
		log(logger, Level.CONFIG, errorCode, context, null);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 */
	public static final void logConfig(Logger logger,String methodName, String errorCode, ErrorContext context) {
		log(logger, Level.CONFIG, methodName, errorCode, context, null);
	}

	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logFine(Logger logger, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.FINE, errorCode, context, e);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	public static final void logFine(Logger logger, String methodName, String errorCode, ErrorContext context, Throwable e) {
		log(logger, Level.FINE, methodName, errorCode, context, e);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param errorCode
	 * @param context
	 */
	public static final void logFine(Logger logger, String errorCode, ErrorContext context) {
		log(logger, Level.FINE, errorCode, context, null);
	}
	
	/**
	 * Logs the provided information using the provided logger after getting the log message for the provided code
	 * @param logger
	 * @param methodName
	 * @param errorCode
	 * @param context
	 */
	public static final void logFine(Logger logger, String methodName, String errorCode, ErrorContext context) {
		log(logger, Level.FINE, methodName, errorCode, context, null);
	}
	
	/**
	 * 
	 * @param logger
	 * @param level
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	private static final void log(Logger logger, Level level, String errorCode, ErrorContext context, Throwable e) {
		if (logger != null) {
			if (e == null) {
				logger.logp(level, logger.getName(), "", "Code="+errorCode+"; " + instance.getErrorMessage(errorCode, context));
			} else {
				logger.logp(level, logger.getName(), "", "Code="+errorCode+"; " + instance.getErrorMessage(errorCode, context), e);
			}
		}		
	}
	
	/**
	 * 
	 * @param logger
	 * @param level
	 * @param methodName
	 * @param errorCode
	 * @param context
	 * @param e
	 */
	private static final void log(Logger logger, Level level, String methodName, String errorCode, ErrorContext context, Throwable e) {
		if (logger != null) {
			if (e == null) {
				logger.logp(level, logger.getName(), methodName, "Code="+errorCode+"; " + instance.getErrorMessage(errorCode, context));
			} else {
				logger.logp(level, logger.getName(), methodName, "Code="+errorCode+"; " + instance.getErrorMessage(errorCode, context), e);
			}
		}		
	}
}