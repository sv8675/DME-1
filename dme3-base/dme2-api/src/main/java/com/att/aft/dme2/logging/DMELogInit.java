package com.att.aft.dme2.logging;

import org.slf4j.ILoggerFactory;

/**
 * The primary intention of this class is detection of a missing or invalid slf4j implementation.  Having a valid slf4j
 * implementation is imperative to properly logging all necessary DME information.  This class attempts to mimic the
 * SLF4J LoggerFactory methods that locate the necessary bindings.  This isn't a great solution, but it's either this or
 * intercept the System.err messages to see if it contains the SLF4J error message
 * <p/>
 * Created by cr618c on 9/3/2015.
 */
public class DMELogInit {
  private DMELogInit() {
  }

  public static void init() {
    try {
      Class c = Class.forName( "org.slf4j.impl.StaticLoggerBinder" );
      //c.getMethod("getLoggerFactory");
      ILoggerFactory loggerFactory =
          (ILoggerFactory) c.getMethod("getLoggerFactory").invoke(c.getMethod("getSingleton").invoke(null));
      String loggerClassName = loggerFactory.getClass().getName();
    } catch ( Exception e ) {
      throw new Error(
          "An slf4j binding is required to use DME.\n" +
          "Please provide one somewhere in the classpath.\n" +
          "Compatible loggers include log4j, logback and slf4j-simple among others."
      );
    }
  }
}
