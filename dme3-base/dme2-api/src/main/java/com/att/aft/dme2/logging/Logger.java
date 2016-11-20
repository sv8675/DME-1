package com.att.aft.dme2.logging;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger class (adapter for slf4j) intended primarily for internal DME logging
 */
public class Logger {
  private static final DateFormat dateFormat =
      new SimpleDateFormat( System.getProperty( "DME_LOGGER_DATE_FORMAT", "yyyy-MM-dd HH:mm:ss.S" ) );
  private static final String MSG_PARAMS = "{} {} {} {} {} {} {} {} ";
  private static final String DEFAULT_HOSTNAME;
  private static final String DEFAULT_PID = ManagementFactory.getRuntimeMXBean().getName();
  private static final String DEFAULT_THREADID = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();

  static {
    String tmpHostname = "Unknown";
    try {
      tmpHostname = InetAddress.getLocalHost().getHostName();
    } catch ( UnknownHostException e ) {
      // Do nothing
    } finally {
      DEFAULT_HOSTNAME = tmpHostname;
    }
  }

  org.slf4j.Logger logger;
  String className;

  /**
   * Create a DME2 Logger
   *
   * @param name class name (usually the one instantiating the logger)
   */
  public Logger( String name ) {
    this.logger = org.slf4j.LoggerFactory.getLogger( name );
    className = name;
  }

  /**
   * Log an error using current timestamp, logging context's tracking id, and logger's class name
   *
   * @param serviceUri Service URI of the request
   * @param method     Method name (of the caller)
   * @param msg        Message to log
   */
  public void error( URI serviceUri, String method, String msg ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
  }

  /**
   * @param serviceUri
   * @param method
   * @param msg
   */
  public void error( URI serviceUri, String method, LogMessage msg ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
  }

  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   */
  public void error( String conversationId, URI serviceUri, String method, String msg ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
  }

  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   */
  public void error( String conversationId, URI serviceUri, String method, LogMessage msg ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
  }

  /**
   * Log an error using current timestamp, logging context's tracking id, and logger's class name
   *
   * @param serviceUri Service URI of the request
   * @param method     Method name (of the caller)
   * @param msg        Message to log
   * @param arg1       Optional argument
   */
  public void error( URI serviceUri, String method, String msg, Object arg1 ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1 ) );
  }

  /**
   * Log an error using current timestamp, logging context's tracking id, and logger's class name
   *
   * @param serviceUri Service URI of the request
   * @param method     Method name (of the caller)
   * @param msg        Message to log
   * @param arg1       Optional argument
   */
  public void error( URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1 ) );
  }

  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   */
  public void error( String conversationId, URI serviceUri, String method, String msg, Object arg1 ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
  }

  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   */
  public void error( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
  }

  /**
   * Log an error using current timestamp, logging context's tracking id, and logger's class name
   *
   * @param serviceUri
   * @param method
   * @param msg
   * @param arg1
   * @param arg2
   */
  public void error( URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log an error using current timestamp, logging context's tracking id, and logger's class name
   *
   * @param serviceUri Service URI of the request
   * @param method     Method name (of the caller)
   * @param msg        Message to log
   * @param arg1       Optional argument
   */
  public void error( URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void error( String conversationId, URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void error( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log an error using current timestamp, logging context's tracking id, and logger's class name
   *
   * @param serviceUri
   * @param method
   * @param msg
   * @param args
   */
  public void error( URI serviceUri, String method, String msg, Object... args ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
  }

  /**
   * Log an error using current timestamp, logging context's tracking id, and logger's class name
   *
   * @param serviceUri Service URI of the request
   * @param method     Method name (of the caller)
   * @param msg        Message to log
   * @param args       Optional argument
   */
  public void error( URI serviceUri, String method, LogMessage msg, Object... args ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
  }


  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param args           Optional arguments
   */
  public void error( String conversationId, URI serviceUri, String method, String msg, Object... args ) {
    logger.error( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, args ) );
  }

  /**
   * Log an error
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param args           Optional arguments
   */
  public void error( String conversationId, URI serviceUri, String method, LogMessage msg, Object... args ) {
    logger.error( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, args ) );
  }

  public void warn( URI serviceUri, String method, String msg ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
  }

  public void warn( URI serviceUri, String method, LogMessage msg ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
  }


  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   */
  public void warn( String conversationId, URI serviceUri, String method, String msg ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
  }

  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   */
  public void warn( String conversationId, URI serviceUri, String method, LogMessage msg ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
  }

  public void warn( URI serviceUri, String method, String msg, Object arg1 ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1 ) );
  }

  public void warn( URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method , arg1 ));
  }

  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   */
  public void warn( String conversationId, URI serviceUri, String method, String msg, Object arg1 ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
  }

  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   */
  public void warn( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
  }

  public void warn( URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
  }

  public void warn( URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void warn( String conversationId, URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void warn( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
  }

  public void warn( URI serviceUri, String method, String msg, Object... args ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
  }

  public void warn( URI serviceUri, String method, LogMessage msg, Object... args ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
  }

  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param args           Optional arguments
   */
  public void warn( String conversationId, URI serviceUri, String method, String msg, Object... args ) {
    logger.warn( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, args  ) );
  }

  /**
   * Log a warning
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param args           Optional arguments
   */
  public void warn( String conversationId, URI serviceUri, String method, LogMessage msg, Object... args ) {
    logger.warn( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, args ) );
  }

  public void info( URI serviceUri, String method, String msg ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
  }

  public void info( URI serviceUri, String method, LogMessage msg ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   */
  public void info( String conversationId, URI serviceUri, String method, String msg ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   */
  public void info( String conversationId, URI serviceUri, String method, LogMessage msg ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
  }

  public void info( URI serviceUri, String method, String msg, Object arg1 ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1 ) );
  }

  public void info( URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1 ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   */
  public void info( String conversationId, URI serviceUri, String method, String msg, Object arg1 ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   */
  public void info( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
  }

  public void info( URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
  }

  public void info( URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void info( String conversationId, URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void info( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
  }

  public void info( URI serviceUri, String method, String msg, Object... args ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
  }

  public void info( URI serviceUri, String method, LogMessage msg, Object... args ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param args           Optional arguments
   */
  public void info( String conversationId, URI serviceUri, String method, String msg, Object... args ) {
    logger.info( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, args ) );
  }

  /**
   * Log an informational statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param args           Optional arguments
   */
  public void info( String conversationId, URI serviceUri, String method, LogMessage msg, Object... args ) {
    logger.info( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
        DEFAULT_THREADID, conversationId, serviceUri, className, method, args ) );
  }

  public void debug( URI serviceUri, String method, String msg ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
    }
  }

  public void debug( URI serviceUri, String method, LogMessage msg ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   */
  public void debug( String conversationId, URI serviceUri, String method, String msg ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   */
  public void debug( String conversationId, URI serviceUri, String method, LogMessage msg ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method ) );
    }
  }

  public void debug( URI serviceUri, String method, String msg, Object arg1 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1 ) );
    }
  }

  public void debug( URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1 ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   */
  public void debug( String conversationId, URI serviceUri, String method, String msg, Object arg1 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   */
  public void debug( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1 ) );
    }
  }

  public void debug( URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
    }
  }

  public void debug( URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, arg1, arg2 ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void debug( String conversationId, URI serviceUri, String method, String msg, Object arg1, Object arg2 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param arg1           Optional argument
   * @param arg2           Optional argument
   */
  public void debug( String conversationId, URI serviceUri, String method, LogMessage msg, Object arg1, Object arg2 ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method, arg1, arg2 ) );
    }
  }

  public void debug( URI serviceUri, String method, String msg, Object... args ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
    }
  }

  public void debug( URI serviceUri, String method, LogMessage msg, Object... args ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, LoggingContext.get( LoggingContext.TRACKINGID ), serviceUri, className, method, args ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            Message to log
   * @param args           Optional arguments
   */
  public void debug( String conversationId, URI serviceUri, String method, String msg, Object... args ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method, args ) );
    }
  }

  /**
   * Log an debugging statement
   *
   * @param conversationId Conversation/Tracking ID
   * @param serviceUri     Service URI of the request
   * @param method         Method name (of the caller)
   * @param msg            LogMessage template to use
   * @param args           Optional arguments
   */
  public void debug( String conversationId, URI serviceUri, String method, LogMessage msg, Object... args ) {
    if ( logger.isDebugEnabled() ) {
      logger.debug( formatMessage( msg.getTemplate() ), formatArgs( System.currentTimeMillis(), DEFAULT_HOSTNAME, DEFAULT_PID,
          DEFAULT_THREADID, conversationId, serviceUri, className, method, args ) );
    }
  }

  private static Object[] formatArgs( Date date, String hostname, String pid, String threadId, String conversationId,
                                      URI serviceUri, Class clazz, Method method, Object... args ) {
    return formatArgs( date, hostname, pid, threadId, conversationId, serviceUri, formatClass( clazz ),
        formatMethod( method ), args );
  }

  private static Object[] formatArgs( Long date, String hostname, String pid, String threadId, String conversationId,
                                      URI serviceUri, Class clazz, Method method, Object... args ) {
    return formatArgs( new Date( date ), hostname, pid, threadId, conversationId, serviceUri, formatClass( clazz ),
        formatMethod( method ), args );
  }

  private static Object[] formatArgs( Date date, String hostname, String pid, String threadId, String conversationId,
                                      URI serviceUri, String clazz, String method, Object... args ) {
    Object[] newArgs = new Object[8 + args.length];
    newArgs[0] = formatDate( date );
    newArgs[1] = hostname;
    newArgs[2] = pid;
    newArgs[3] = threadId;
    newArgs[4] = conversationId;
    newArgs[5] = formatServiceUri( serviceUri );
    newArgs[6] = clazz;
    newArgs[7] = method;
    int i = 8;
    for ( Object o : args ) {
      newArgs[i++] = o;
    }
    return newArgs;
  }

  private static Object[] formatArgs( Date date, String hostname, String pid, String threadId, String conversationId,
                                      URI serviceUri, Class clazz, String method, Object... args ) {
    return formatArgs( date, hostname, pid, threadId, conversationId, serviceUri, formatClass( clazz ), method, args );
  }

  private static Object[] formatArgs( Long date, String hostname, String pid, String threadId, String conversationId,
                                      URI serviceUri, Class clazz, String method, Object... args ) {
    return formatArgs( new Date( date ), hostname, pid, threadId, conversationId, serviceUri, formatClass( clazz ),
        method, args );
  }

  private static Object[] formatArgs( Long date, String hostname, String pid, String threadId,
                                      String conversationId, URI serviceUri, String className, String method,
                                      Object... args ) {
    return formatArgs( new Date( date ), hostname, pid, threadId, conversationId, serviceUri, className, method, args );
  }

  private static String formatMethod( Method method ) {
    return method == null ? "NULL" : method.getName();
  }

  private static String formatClass( Class clazz ) {
    return clazz == null ? "NULL" : clazz.getName();
  }

  private static String formatServiceUri( URI serviceUri ) {
    return serviceUri == null ? "NULL" : serviceUri.toASCIIString();
  }

  private static String formatDate( Date date ) {
    return dateFormat.format( date );
  }

  private static String formatMessage( String msg ) {
    return MSG_PARAMS + msg;
  }

}
