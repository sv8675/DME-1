package com.att.aft.dme2.manager.registry;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/**
 * Abstract registry with common methods
 */
public abstract class DME2AbstractEndpointRegistry implements DME2EndpointRegistryAdapter {
  private static final Logger logger = LoggerFactory.getLogger( DME2AbstractEndpointRegistry.class );
  /**
   * Distance bands are ranges of distance that group endpoints together. For instance distance bands of 10, 100, 1000
   * would be three (or four, depending upon if a MAX distance is allowed) groups of endpoints.  Those between 0-10,
   * 10-100, 100-1000 (1000-MAX optional)
   */
  private double[] DISTANCE_BANDS = null;

  /**
   * Maximum calculated great circle distance between two points.
   */
  public static final double CALCULATED_DISTANCE_MAX = 20000.0;

  private static final double[] DISTANCE_BANDS_DEFAULT = { 0.1, 500.0, 5000.0, CALCULATED_DISTANCE_MAX };

  private static Long ROUTEOFFER_STALE_PERIOD_IN_MS = null;
  private DME2Configuration config;
  //private DME2Manager manager;
  protected DME2AbstractRegistryCache<String, Long> staleEndpointCache;
  public DME2AbstractRegistryCache<String, Long> getStaleEndpointCache() {
	return staleEndpointCache;
}

protected DME2AbstractRegistryCache<String, Long> staleRouteOfferCache;
  public DME2AbstractRegistryCache<String, Long> getStaleRouteOfferCache() {
	return staleRouteOfferCache;
}

protected String managerName;

  // These are private because, if these change, we want other things to happen. (distance recalculations)
  private static Double clientLatitude;
  private static Double clientLongitude;

  static {
    staticInit();
  }

  private byte[] lock = new byte[0];

  /**
   * Base constructor
   *
   * @param config      Configuration
   * @param managerName Manager Name
   * @throws DME2Exception
   */
  public DME2AbstractEndpointRegistry( DME2Configuration config, String managerName ) throws DME2Exception {
    if ( managerName == null || managerName.isEmpty() ) {
      throw new DME2Exception( DME2Constants.EXP_REG_NULL_MANAGER,
          "Manager name must not be null or empty during Endpoint Registry construction" );
    }

    this.managerName = managerName;
    this.config = config;

    if ( getClientLatitude() == null || getClientLongitude() == null ) {
      clientLatitude = config.getDouble( DME2Constants.AFT_LATITUDE );
      clientLongitude = config.getDouble( DME2Constants.AFT_LONGITUDE );
      if ( getClientLatitude() == null || getClientLongitude() == null ) {
        // TODO: Proper AFT code
        throw new DME2Exception( "AFT_DME2_0000", DME2Constants.AFT_LATITUDE + " or " + DME2Constants.AFT_LONGITUDE +
            " were null from configuration.  These lat,long values are required." );
      }
    }

    // Set distance bands

    boolean isExcludingOutOfBandEndpoints = config.getBoolean( DME2Constants.DME2_ENDPOINT_BANDS_EXCLUDE_OUT_OF_BAND );
    String bandStrs = config.getProperty( DME2Constants.DME2_ENDPOINT_BANDS );
    if ( bandStrs != null ) {
      String[] bandToks = bandStrs.split( "," );

      double[] distanceBands = new double[bandToks.length];
      for ( int i = 0; i < bandToks.length; i++ ) {
        distanceBands[i] = Double.parseDouble( bandToks[i] );
      }

      if ( distanceBands.length > 0 ) {
        int length = distanceBands.length;
        if ( distanceBands[length - 1] >= CALCULATED_DISTANCE_MAX
            || isExcludingOutOfBandEndpoints ) {
          DISTANCE_BANDS = new double[length];
          System.arraycopy( distanceBands, 0, DISTANCE_BANDS, 0, length );
        } else {
          // add a final maximum calculated distance value
          DISTANCE_BANDS = new double[length + 1];
          System.arraycopy( distanceBands, 0, DISTANCE_BANDS, 0, length );
          DISTANCE_BANDS[length] = CALCULATED_DISTANCE_MAX;
        }
      }
    }
    if ( DISTANCE_BANDS == null ) {
      DISTANCE_BANDS = DISTANCE_BANDS_DEFAULT;
    }
  }

  private static void staticInit() {
    String defaultStalePeriod = System.getProperty( DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN );
    if ( defaultStalePeriod != null ) {
      logger.debug( null, "init", "RouteOffer staleness duration found in System Properties. Value = {} minutes",
          defaultStalePeriod );
      try {
        ROUTEOFFER_STALE_PERIOD_IN_MS = Long.parseLong( defaultStalePeriod ) * 60000;
      } catch ( NumberFormatException e ) {
        logger.warn( null, "init",
            "Exception occured while parsing staleness duration provided in System Properties. Provided value was {}",
            defaultStalePeriod );
        ROUTEOFFER_STALE_PERIOD_IN_MS = DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN_DEFAULT * 60000;
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param properties Properties to use in initialization
   * @throws DME2Exception
   */
  @Override
  public void init( Properties properties ) throws DME2Exception {

  }

  /**
   * Returns the encapsulated configuration
   *
   * @return encapsulated configuration
   */
  public DME2Configuration getConfig() {
    return config;
  }

  protected DmeUniformResource buildUniformResource( String protocol, String servicePath, String host, int port )
      throws DME2Exception {
    DmeUniformResource uniformResource = null;

    try {
      if ( servicePath.startsWith( "http://" ) ) {
        uniformResource = new DmeUniformResource( config, servicePath );
      } else {
        String urlStr = null;
        if ( servicePath.startsWith( "/" ) ) {
          urlStr = "http://" + host + ":" + port + servicePath;
        } else {
          urlStr = "http://" + host + ":" + port + "/" + servicePath;
        }
        uniformResource = new DmeUniformResource( config, urlStr );
      }
    } catch ( MalformedURLException e ) {
      throw new DME2Exception( DME2Constants.EXP_GEN_URI_EXCEPTION, new ErrorContext()
          .add( "extendedMessage", e.getMessage() )
              // .add( "manager", this.getManager().getName() )
          .add( "servicePath", servicePath ).add( "host", host )
          .add( "port", "" + port ), e );
    }

    return uniformResource;
  }

  @Override
  public Long getEndpointExpirationTime( String url ) {
    return staleEndpointCache.get( url );
  }

  @Override
  public Boolean isEndpointStale( String url ) {
    return !( staleEndpointCache.get( url ) == null );
  }

  @Override
  public void removeStaleEndpoint( String url ) {
    staleEndpointCache.remove( url );
  }

  @Override
  public void addStaleEndpoint( String url, Long expirationTime ) {
    if ( expirationTime == null || expirationTime == 0 ) {
      expirationTime = DME2Constants.DME2_ENDPOINT_STALENESS_PERIOD_DEFAULT;
    }
    staleEndpointCache.put( url, expirationTime );
  }

  @Override
  public void clearStaleEndpoints() {
    staleEndpointCache.clear();
  }

  @Override
  public Long getRouteOfferExpirationTime( String url ) {
    Long expirationTime = staleRouteOfferCache.get( url );
    logger.debug( null, "getRouteOfferExpirationTime", "Checking route offer expiration time for {} (is {})", url, expirationTime );
    return expirationTime;
  }

  @Override
  public Boolean isRouteOfferStale( String url ) {
    Long expTime = getRouteOfferExpirationTime( url );
    if ( expTime != null ) {
      if ( expTime <= System.currentTimeMillis() ) {
        removeStaleRouteOffer( url );
        logger.debug( null, "isRouteOfferStale", LogMessage.DEBUG_MESSAGE, "Removed stale route offer from cache: {}", url );
        return false;
      }
      return true;
    }

    return false;
  }

  @Override
  public void removeStaleRouteOffer( String url ) {
    logger.debug( null, "removeStaleRouteOffer", "Removing {} from stale route offer cache", url );
    staleRouteOfferCache.remove( url );
    //long removedAt = System.currentTimeMillis();
   	logger.info( null, "removeStaleRouteOffer", "searching it after removal:[{}]",staleRouteOfferCache.get( url ));
    if ( staleRouteOfferCache.get( url ) != null ) {
    	logger.error( null, "removeStaleRouteOffer", "REMOVED STALE ROUTE OFFER` BUT IT IS STILL THERE!" );
      /*while ( staleRouteOfferCache.get( url ) != null ) {

      }
      logger.error( null, "removeStaleRouteOffer", "It took {} ms to remove the route offer!", (System.currentTimeMillis() - removedAt));*/
    }
  }

  @Override
  public void addStaleRouteOffer( String url, Long expirationTime ) {
    if ( ROUTEOFFER_STALE_PERIOD_IN_MS != null ) {
      // This overrides whatever was passed in
      expirationTime = System.currentTimeMillis() + ROUTEOFFER_STALE_PERIOD_IN_MS;
    } else {
      if ( expirationTime == null || expirationTime == 0 ) {
        expirationTime = System.currentTimeMillis() +  DME2Constants.DME2_ROUTEOFFER_STALENESS_IN_MIN_DEFAULT * 60000;
      }
    }
    staleRouteOfferCache.put( url, expirationTime );
    logger.debug( null, "addStaleRouteOffer",
        "Marked RouteOffer stale for service: {}. Staleness duration in milliseconds is: {}", url, expirationTime );
  }

  @Override
  public void clearStaleRouteOffers() {
    logger.debug( null, "clearStaleRouteOffers", LogMessage.METHOD_ENTER );
    staleRouteOfferCache.clear();
    logger.debug( null, "clearStaleRouteOffers", LogMessage.METHOD_EXIT );
  }

  public static Double getClientLatitude() {
    return clientLatitude;
  }

  public static Double getClientLongitude() {
    return clientLongitude;
  }

  @Override
  public double[] getDistanceBands() {
    return DISTANCE_BANDS;
  }

  @Override
  public Set<String> getStaleEndpoints() {
    return convertCacheElementKeySet( staleEndpointCache.getKeySet() );
  }

  @Override
  public Set<String> getStaleRouteOffers() {

    Set<String> keys = new HashSet<String>();
    Set<String> keysToRemove = new HashSet<String>();
    // Because default cache is a synchronized map, we have to do this.
    synchronized( lock ) {
      for ( CacheElement.Key<String> key : staleRouteOfferCache.getKeySet() ) {
        Long expTime = getRouteOfferExpirationTime( key.getKey() );
        if ( expTime != null ) {
          if ( expTime <= System.currentTimeMillis() ) {
            keysToRemove.add( key.getKey() );
          } else {
            keys.add( key.getKey() );
          }
        } else {
          keysToRemove.add( key.getKey() );
        }
      }

      for ( String keyToRemove : keysToRemove ) {
        removeStaleEndpoint( keyToRemove );
      }
    }

    return keys;
  }

  private Set<String> convertCacheElementKeySet( Set<CacheElement.Key> cacheKeys ) {
    Set<String> keys = new HashSet<String>();
    for ( CacheElement.Key key : cacheKeys ) {
      keys.add( (String) key.getKey() );
    }
    return keys;
  }
}
