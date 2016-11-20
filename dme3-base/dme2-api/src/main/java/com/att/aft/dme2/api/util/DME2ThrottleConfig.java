/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.mbean.DME2ThrottleMXBean;
import com.att.aft.dme2.util.DME2Constants;

public class DME2ThrottleConfig implements DME2ThrottleMXBean {
  private volatile static DME2ThrottleConfig dme2ThrottleConfig;
  private static Properties props = new Properties();
  private static Logger logger = LoggerFactory.getLogger  ( DME2ThrottleConfig.class.getName() );
  private Float defthrottlePctPerPartner;
  private DME2Configuration config;

  public DME2ThrottleConfig( DME2Configuration configuration ) {
    logger.debug( null, "ctor", LogMessage.METHOD_ENTER );
    config = configuration;
    defthrottlePctPerPartner = config.getFloat( DME2Constants.AFT_DME2_THROTTLE_PCT_PER_PARTNER );
    logger.debug( null, "ctor", "defthrottlePctPerPartner is {}", defthrottlePctPerPartner );
    logger.debug( null, "ctor", LogMessage.METHOD_EXIT );
  }

  public static DME2ThrottleConfig getInstance( DME2Configuration configuration, String fileName ) {
    // https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
    DME2ThrottleConfig result = dme2ThrottleConfig;
    if ( result == null ) {
      synchronized (DME2ThrottleConfig.class) {
        result = dme2ThrottleConfig;
        if ( result == null ) {
          try {
            dme2ThrottleConfig = result = new DME2ThrottleConfig( configuration );
            props = loadProps( fileName );
          } catch ( Exception e ) {
            logger.error( null, "getInstance", "Error loading throttle config" );
            throw e;
          }
        }
      }
    }

    return result;
  }

  // This is not good - if this is null, there will be unexplained NPE's

  public static DME2ThrottleConfig getInstance() {
    //if ( dme2ThrottleConfig == null ) {
      //throw new RuntimeException( "DME2ThrottleConfig was null!" );
    //}
    return dme2ThrottleConfig;
  }


  public void registerForRefresh() {

  }

  public float getThrottleConfig( String service, String partner ) {
    logger.debug( null, "getThrottleConfig", LogMessage.METHOD_ENTER );
    logger.debug( null, "getThrottleConfig", "Get throttle for service {} and partner {}", service, partner );
    if ( ( props != null ) && ( service != null ) & ( partner != null ) ) {
      String throttle = (String) props.get( service + "." + partner );
      if ( throttle != null ) {
        logger.debug( null, "getThrottleConfig", "Throttle: {}", throttle );
        return Float.parseFloat( throttle );
      } else {
        logger.debug( null, "getThrottleConfig", "Throttle null" );
        return -1;
      }
    }
    logger.debug( null, "getThrottleConfig", "Throttle 0" );
    return 0;
  }

  @Override
  public float getThrottleConfigForPartner( String service, String partner ) {
    return getThrottleConfig( service, partner );
  }

  @Override
  public void setThrottleConfigForPartner( String service, String partner, float value ) {
    logger.debug( null, "setThrottleConfigForPartner", LogMessage.METHOD_ENTER );
    logger.debug( null, "setThrottleConfigForPartner", "Service: {} Partner: {} Value: {}", service, partner, value );
    if ( ( value > 0 ) && ( value <= 100 ) ) {
      props.setProperty( service + "." + partner, String.valueOf( value ) );
    }
    logger.debug( null, "setThrottleConfigForPartner", LogMessage.METHOD_EXIT );
  }

  private static Properties loadProps( String fileName ) {
    ClassLoader[] cls = new ClassLoader[]{
        ClassLoader.getSystemClassLoader(),
        DME2ThrottleConfig.class.getClassLoader(),
        Thread.currentThread().getContextClassLoader() };

    Properties props = new Properties();
    boolean loaded = false;
    // Check system classpath
    for ( ClassLoader cl : cls ) {
      InputStream in = cl.getResourceAsStream( fileName );
      logger.debug( null, "loadProps", "Loading from: {}", cl.getResource( fileName ) );
      if ( in != null ) {
        try {
          logger.debug( null, "loadProps", "Loading {}: {}", cl.toString(), fileName );
          props.load( in );
          loaded = true;
          in.close();
          break;
        } catch ( IOException e ) {
          logger.error( null, "loadProps", "Exception", e );
        }
      }
    }
    if ( loaded ) {
      return props;
    }

		/* Could not load the file from classpath, trying to load from external source */
    InputStream in = null;
    try {
      logger.debug( null, "loadProps", "Loading props from: {}", fileName );
      in = new FileInputStream( fileName );
      props.load( in );
      logger.debug( null, "loadProps", "Successfully Loaded throttling props from: {}", fileName );
    } catch ( IOException e ) {
      logger.debug( null, "loadProps", "Exception", e );
      logger.debug( null, "loadProps", "Error loading throttling props from: {},  using default throttle setting",
          fileName );
    } finally {
      if ( in != null ) {
        try {
          in.close();
        } catch ( IOException e ) {
          logger.debug( null, "loadProps", "IOException", e );
        }
      }
    }

    return props;

  }
}
