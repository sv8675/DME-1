package com.att.aft.dme2.logging;

/**
 * LoggerFactory for DME logger
 */
public class LoggerFactory {
  private LoggerFactory() {

  }

  /**
   * Retrieve a logger
   *
   * @param clazz Class to associate with logger
   * @return Logger
   */
  public static Logger getLogger( Class clazz ) {
    Logger logger = LoggerFactory.getLogger( clazz.getName() );
    return logger;
  }

  /**
   * Retrieve a logger
   *
   * @param name Logger name (usually class of caller)
   * @return logger
   */
  public static Logger getLogger( String name ) {
    return new Logger( name );
  }
}
