/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheConfiguration;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.domain.CacheTypeElement;
import com.att.aft.dme2.cache.domain.CacheTypes;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.cache.service.CacheEntryView;
import com.att.aft.dme2.cache.service.CacheSerialization;
import com.att.aft.dme2.cache.service.CacheTaskScheduler;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheableCallback;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.factory.DME2CacheFactory;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2ServiceEndpointData;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;
import com.hazelcast.core.HazelcastInstanceNotActiveException;

public abstract class AbstractCache<M> implements DME2Cache, Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger( AbstractCache.class.getName() );

  private DME2Configuration config = null;
  private boolean enableCacheStats = true;
  protected CacheConfiguration cacheConfig = null;
  protected CacheTypeElement cacheTypeElementConfig = null;
  protected final Map<String, CacheTaskScheduler> cacheScheduleTaskRegister = new HashMap<String, CacheTaskScheduler>();
  protected CacheSerialization cacheSerializer = null;
  private long infrequentEndpointCacheTTL;
  private long endpointLastQueriedInterval;
  private final long[] emptyCacheTTLRefreshDefaultIntervals = new long[]{ 300000, 300000, 300000, 600000, 900000 };
  private long endpointCacheEmptyTTL, cacheEntryTTL;
  private long[] emptyCacheTTLRefreshIntervals;
  private final Map<String, DME2CacheStatsHolder> cacheStats =
      Collections.synchronizedMap( new HashMap<String, DME2CacheStatsHolder>() );
  private final byte[] lock = new byte[0];
  private final byte[] hashlock = new byte[0];

  public AbstractCache( final String cacheName, final String cacheType, final DME2CacheableCallback source,
                        final DME2Configuration config ) {
    LOGGER.debug( null, "AbstractCache", "start" );
    this.config = config;
    this.cacheTypeElementConfig = CacheTypes.getType( cacheType, config );

    if ( cacheTypeElementConfig == null ) {
      throw new CacheException( ErrorCatalogue.CACHE_018, cacheType );
    }

    cacheConfig = CacheConfiguration.getInstance();
    cacheConfig.setCacheName( cacheName );
    cacheConfig.setDataLoader( DME2CacheFactory.getDataHandler( cacheTypeElementConfig ) );
    cacheConfig.setCacheType( cacheTypeElementConfig );
    cacheConfig.setSource( source );

    initializeVariables();

    // Adding shutdown hook to remove timers - CJR 02/09/16
    Runtime.getRuntime().addShutdownHook( new Thread( this ) );
  }

  public AbstractCache( final CacheConfiguration cacheConfig ) {
    this.cacheConfig = cacheConfig;
    initializeVariables();
  }

  public void initializeVariables() {
    endpointLastQueriedInterval = config.getLong( DME2Constants.DME2_SERVICE_LAST_QUERIED_INTERVAL_MS, 900000 );
    infrequentEndpointCacheTTL = config.getLong( DME2Constants.DME2_SEP_CACHE_INFREQUENT_TTL_MS, 900000 * 2 );
    endpointCacheEmptyTTL = config.getLong( DME2Constants.Cache.DME2_SEP_CACHE_EMPTY_TTL_MS, 300000 );

    cacheEntryTTL = getCacheConfig().getCacheType().getTtl() > 0 ? getCacheConfig().getCacheType().getTtl() :
        ( isEndpointCache( this ) || isRouteInfoCache( this ) ? 300000 : 900000 );
    if ( isEndpointCache( this ) && config.getLong( DME2Constants.Cache.DME2_SEP_CACHE_TTL_MS, -1 ) > 0 ) {
      cacheEntryTTL = config.getLong( DME2Constants.Cache.DME2_SEP_CACHE_TTL_MS ); //override for backward compatibility
    } else if ( isStaleEndpointCache( this ) &&
        config.getLong( DME2Constants.Cache.AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS, -1 ) > 0 ) {
      cacheEntryTTL = config.getLong(
          DME2Constants.Cache.AFT_DME2_CLIENT_ENDPOINT_STALENESS_PERIOD_MS ); //override for backward compatibility
    } else if ( isRouteInfoCache( this ) && config.getInt( DME2Constants.Cache.DME2_ROUTEINFO_CACHE_TTL_MS, -1 ) > 0 ) {
      cacheEntryTTL = config.getInt( DME2Constants.Cache.DME2_ROUTEINFO_CACHE_TTL_MS );
    }
    LOGGER.debug( null, "initialize variable", "cache entry ttl initialized: [{}]", cacheEntryTTL );

    emptyCacheTTLRefreshIntervals = getEmptyCacheTTLIntervalsFromProperties(
        config.getProperty( DME2Constants.Cache.DME2_SEP_EMPTY_CACHE_TTL_INTERVALS, null ),
        emptyCacheTTLRefreshDefaultIntervals );
  }

  protected DME2Configuration getConfig() {
    return this.config;
  }

  protected void init() {
    LOGGER.debug( null, "init", "start" );

    warmUpCache();
    createCacheSerializerTimer();
    createCacheRefreshTimer();
    createCacheRemoveUnusedEndpoints();

    LOGGER.debug( null, "init", "complete" );
  }

  //create the task for removing unused entries for the cache
  protected void createCacheRemoveUnusedEndpoints() {
    long unused_ep_removal_delay = -1;
    if ( getCacheConfig().getCacheType() != null &&
        getCacheConfig().getCacheType().getIdleTimeoutCheckInterval() > 0 ) {
      unused_ep_removal_delay = getCacheConfig().getCacheType().getIdleTimeoutCheckInterval();
    } else {
      unused_ep_removal_delay = 300000;
    }
    unused_ep_removal_delay =
        getConfig().getLong( DME2Constants.Cache.DME2_UNUSED_ENDPOINT_REMOVAL_DELAY, unused_ep_removal_delay );

    if ( unused_ep_removal_delay > 0 ) {
      LOGGER.debug( null, "createCacheRemoveUnusedEndpoints", "cache element idle timeout - cache: [{}], [{}]",
          getCacheName(), unused_ep_removal_delay );
      createScheduledTask( "CacheRemoveUnusedEndpoints::" + getCacheName(), true, unused_ep_removal_delay, this,
          "checkNRemoveUnusedEndpoints" );
    }
  }

  public void checkNRemoveUnusedEndpoints() {
    try {
      Set<Key> keyRemovalSet = new HashSet<Key>();
      long unused_ep_timeout_ms = -1;
      if ( getCacheConfig().getCacheType().getIdleTimeout() > 0 ) {
        unused_ep_timeout_ms = getCacheConfig().getCacheType().getIdleTimeout();
      } else {
        unused_ep_timeout_ms = 259200000;
      }
      unused_ep_timeout_ms =
          getConfig().getLong( DME2Constants.Cache.DME2_UNUSED_ENDPOINT_REMOVAL_DURATION_MS, unused_ep_timeout_ms );

      if ( unused_ep_timeout_ms > 0 ) {
        for ( Key key : getKeySet() ) {
          CacheElement element = getEntryView().getEntry( key );

          if ( element != null ) {
            long lastQueried = element.getLastAccessedTime();
            LOGGER.debug( null, "checkNRemoveUnusedEndpoints", "current idle time: [{}]",
                System.currentTimeMillis() - lastQueried );
            if ( ( System.currentTimeMillis() - lastQueried ) > unused_ep_timeout_ms ) {
              /* If time is greater than specified time, then add in the removal key set to be removed eventually */
              LOGGER.debug( null, "checkNRemoveUnusedEndpoints",
                  "Removing endpoints for  {}  after unused interval of {}ms", element.getValue(),
                  unused_ep_timeout_ms );

              keyRemovalSet.add( key );

              LOGGER.debug( null, "checkNRemoveUnusedEndpoints", "[{}]={}; idle timeout= {}", element.getKey(),
                  element.getValue(), unused_ep_timeout_ms );
            }
          }
        }
        //process the keys added for removal from the cache due to idle timeout
        for ( Key k : keyRemovalSet ) {
          remove( k );
        }

      } else {
        LOGGER
            .warn( null, "checkNRemoveUnusedEndpoints", "interval for removing unused endpoints is not properly set" );
      }
    } catch ( Exception e ) {
      LOGGER.debug( null, "checkNRemoveUnusedEndpoints",
          "Error [{}] occurred while checking and removing unused cache entries", e );
			/*Ignoring*/
    }
  }

  //create the task for clearing expired cache entries
  protected void createCacheRemoveExpiredEntries() {
    if ( getCacheConfig().getCacheType() != null && getCacheConfig().getCacheType().getCleanupIntervalMS() > 0 ) {
      LOGGER.debug( null, "createCacheRemoveExpiredEntries", "cache element timer interval - cache: [{}], [{}]",
          getCacheName(), getCacheConfig().getCacheType().getCleanupIntervalMS() );
      createScheduledTask( "createCacheRemoveExpiredEntries::" + getCacheName(), true,
          getCacheConfig().getCacheType().getCleanupIntervalMS(), this, "checkNRemoveExpiredStaleCacheEntries" );
    }
  }

  //create the refresh task for the cache
  protected void createCacheRefreshTimer() {

    long refreshInterval = -1;

    //making sure for backward compatibility the refresh property value is used as override
    if ( isEndpointCache( this ) || isRouteInfoCache( this ) ) {
      if ( isEndpointCache( this ) ) {
        refreshInterval = getConfig().getLong( DME2Constants.Cache.DME2_SEP_REFRESH_CACHE_TIMER_FREQ_MS, -1 );
      } else if ( isRouteInfoCache( this ) ) {
        refreshInterval = getConfig().getLong( DME2Constants.Cache.DME2_ROUTE_INFO_CACHE_TIMER_FREQ_MS, -1 );
      }

      if ( refreshInterval < 0 ) {
        if ( getCacheConfig().getCacheType() != null && getCacheConfig().getCacheType().getRefreshInterval() > 0 ) {
          refreshInterval = getCacheConfig().getCacheType().getRefreshInterval();
        } else {
          refreshInterval = 20000;
        }
      }
    }
    if ( refreshInterval > 0 ) {
      LOGGER
          .debug( null, "createCacheRefreshTimer", "cache element ttl - cache: [{}], ttl: [{}], refresh interval: [{}]",
              getCacheName(), cacheEntryTTL, refreshInterval );
      createScheduledTask( "RefreshCacheData::" + getCacheName(), true, refreshInterval, this, "refresh" );
    }
  }

  //create cache serializer timer task
  protected void createCacheSerializerTimer() {
    try {
      if ( getCacheConfig().getCacheType() != null && getCacheConfig().getCacheType().getPersistFrequencyMS() > 0 ) {
        long cacheSerializerTimer = -1;
        if ( isEndpointCache( this ) ) {
          cacheSerializerTimer =
              getConfig().getLong( DME2Constants.Cache.DME2_PERSIST_CACHED_ENDPOINTS_FREQUENCY_MS, -1 );
        } else if ( isRouteInfoCache( this ) ) {
          cacheSerializerTimer =
              getConfig().getLong( DME2Constants.Cache.DME2_PERSIST_CACHED_ROUTEINFO_FREQUENCY_MS, -1 );
        }

        if ( cacheSerializerTimer < 0 ) {
          cacheSerializerTimer = getCacheConfig().getCacheType().getPersistFrequencyMS();
        }
        if ( cacheSerializerTimer < 0 ) {
          cacheSerializerTimer = 300000;
        }

        if ( !( Boolean.valueOf( System.getProperty( DME2Constants.Cache.DME2_DISABLE_PERSISTENT_CACHE ) ) ||
            config.getBoolean( "DME2_DISABLE_PERSISTENT_CACHE", false ) ) ) {
          if ( cacheSerializer == null ) {
            cacheSerializer = DME2CacheFactory.getCacheSerializer( this, getConfig(), isEndpointCache( this ) );
          }
          createScheduledTask( getCacheName().concat( "-serializer-time" ), true, cacheSerializerTimer, cacheSerializer,
              "persist", this, getConfig() );
          LOGGER.debug( null, "createCacheSerializerTimer", "cache serializer timer has been started for cache: [{}]",
              getCacheName() );
        } else {
          LOGGER.debug( null, "createCacheSerializerTimer",
              "cache serializer timer has not been started because the persistent feature is disabled" );
        }
      } else {
        LOGGER.debug( null, "createCacheSerializerTimer",
            "cache serializer timer has not been started because the persistent feature is not available for this cache: [{}]",
            getCacheName() );
      }

    } catch ( DME2Exception e ) {
      LOGGER.warn( null, "createCacheSerializerTimer", "cache serializer cannot be instantiated", e );
    }
  }

  public static boolean isEndpointCache( final DME2Cache cache ) {
    boolean isEndpointCache = false;
    try {
      isEndpointCache = DME2Constants.Cache.Type.ENDPOINT.equals( cache.getCacheConfig().getCacheType().getName() );
    } catch ( Exception e ) {
    }
    return isEndpointCache;
  }

  public static boolean isStaleEndpointCache( final DME2Cache cache ) {
    boolean isStaleEndpointCache = false;
    try {
      isStaleEndpointCache =
          DME2Constants.Cache.Type.STALE_ENDPOINT.equals( cache.getCacheConfig().getCacheType().getName() );
    } catch ( Exception e ) {
    }
    return isStaleEndpointCache;
  }

  public static boolean isStaleRouteInfoCache( final DME2Cache cache ) {
    boolean isStaleRouteInfoCache = false;
    try {
      isStaleRouteInfoCache =
          DME2Constants.Cache.Type.STALE_ROUTE_INFO.equals( cache.getCacheConfig().getCacheType().getName() );
    } catch ( Exception e ) {
    }
    return isStaleRouteInfoCache;
  }

  public static boolean isRouteInfoCache( final DME2Cache cache ) {
    boolean isRouteInfoCache = false;
    try {
      isRouteInfoCache = DME2Constants.Cache.Type.ROUTE_INFO.equals( cache.getCacheConfig().getCacheType().getName() );
    } catch ( Exception e ) {
    }
    return isRouteInfoCache;
  }

  public void checkNRemoveExpiredStaleCacheEntries() {
    try {
      if ( getCacheConfig().getCacheType().getCleanupIntervalMS() > 0 ) {
        for ( Key key : getKeySet() ) {
          CacheElement element = getEntryView().getEntry( key );

          if ( element != null ) {
            if ( isStaleEndpointCache( this ) && System.currentTimeMillis() > element.getExpirationTime() ) {
							/* If time is greater than specified time, then remove from cache */
              LOGGER.debug( null, "checkNRemoveExpiredEntries", "Removing entry for {} at {}ms", key,
                  element.getExpirationTime() );
              remove( key );
              LOGGER.debug( null, "checkNRemoveExpiredEntries", "Removed entry for {} at {}ms", key,
                  element.getExpirationTime() );
            } else if ( isStaleRouteInfoCache( this ) ) {
              if ( element != null && element.getValue() != null && element.getValue().getValue() != null &&
                  System.currentTimeMillis() > ( (Long) element.getValue().getValue() ).longValue() ) {
								/* If time is greater than specified time, then remove from cache */
                LOGGER.debug( null, "checkNRemoveExpiredEntries", "Removing entry for {} at {}ms", key,
                    ( (Long) element.getValue().getValue() ).longValue() );
                remove( key );
                LOGGER.debug( null, "checkNRemoveExpiredEntries", "Removed entry for {} at {}ms", key,
                    ( (Long) element.getValue().getValue() ).longValue() );
              }
            }
          }
        }
      } else {
        LOGGER.warn( null, "checkNRemoveExpiredEntries", "interval for cache entries is not properly set" );
      }
    } catch ( Exception e ) {
      LOGGER.error( null, "checkNRemoveExpiredEntries", "Error [{}] occurred while checking and clearing cache entries",
          e );
    }
  }

  protected void warmUpCache() {
    try {
      cacheSerializer = DME2CacheFactory.getCacheSerializer( this, getConfig(), isEndpointCache( this ) );
      if ( !cacheSerializer.isStale( this, getConfig() ) ) {
        cacheSerializer.load( this, getConfig() );
      } else {
        refresh();
      }
    } catch ( DME2Exception e ) {
      LOGGER.warn( null, "createCacheSerializerTimer", "cache serializer cannot be instantiated" );
    }
  }

  protected void createScheduledTask( String taskName, boolean isDaemon, long interval, Object object,
                                      String methodName, Object... objs ) {
    if ( cacheScheduleTaskRegister.get( taskName ) != null ) {
      LOGGER.warn( null, "createScheduledTask", "Removing duplicate task {}", taskName );
      cacheScheduleTaskRegister.get( taskName ).cancel();
    }
    cacheScheduleTaskRegister.put( taskName,
        CacheTaskScheduler.scheduleAtFixedRate( taskName, isDaemon, interval, object, methodName, objs ) );
  }

  protected void createScheduledTask( String taskName, boolean isDaemon, long interval, Object object,
                                      String methodName ) {
    if ( cacheScheduleTaskRegister.get( taskName ) != null ) {
      LOGGER.warn( null, "createScheduledTask", "Removing duplicate task {}", taskName );
      cacheScheduleTaskRegister.get( taskName ).cancel();
    }
    cacheScheduleTaskRegister
        .put( taskName, CacheTaskScheduler.scheduleAtFixedRate( taskName, isDaemon, interval, object, methodName ) );
  }

  @Override
  public CacheConfiguration getCacheConfig() {
    return cacheConfig;
  }

  @SuppressWarnings("rawtypes")
  protected void putAllData( final Map<CacheElement.Key, CacheElement.Value> cacheElements ) {
    LOGGER.debug( null, "AbstractCache.putAllData", "start - cache: [{}]", getCacheName() );
    for ( Entry<CacheElement.Key, CacheElement.Value> entry : cacheElements.entrySet() ) {
      if ( isPutAllow( entry.getKey(), entry.getValue() ) ) {
        put( entry.getKey(), entry.getValue() );
      }
    }
    LOGGER.debug( null, "AbstractCache.putAllData", "completed - cache: [{}]", getCacheName() );
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void put( final Key k, final Value v ) {
    LOGGER.debug( null, "AbstractCache.put(k,v)", "start put cache: [{}]", getCacheName() );
    put( k, createElement( k, v ) );
    LOGGER.debug( null, "AbstractCache.put(k,v)", "completed put cache: [{}]", getCacheName() );
  }

  @SuppressWarnings("rawtypes")
  public CacheElement createElement( Key k, Value v ) {
    LOGGER.debug( null, "AbstractCache.createElement", "start - cache: [{}]", getCacheName() );
    long ttl = cacheEntryTTL;
    long expirationTime = ttl > 0 ? getCurrentTimeMS() + cacheEntryTTL : -1;
    CacheElement element = new CacheElement()
        .setKey( k )
        .setCreationTime( getCurrentTimeMS() )
        .setValue( v )
        .setLastAccessedTime( getCurrentTimeMS() )
        .setExpirationTime( expirationTime )
        .setTtl( ttl );

    // TODO: THIS IS A BAND-AID and needs to be fixed!
    // This is here because the ttl and expiration time between DME2ServiceEndpointData and CacheElement are closely
    // related and need to be in sync.  Things such as CacheSerializationToFile create DSED objects directly, thus
    // the necessity to have this here.

    if ( v != null && v.getValue() != null && v.getValue() instanceof DME2ServiceEndpointData ) {
      DME2ServiceEndpointData data = (DME2ServiceEndpointData) v.getValue();
      element.setExpirationTime( data.getExpirationTime() );
      element.setTtl( data.getCacheTTL() );
    }
    LOGGER
        .debug( null, "AbstractCache.createElement", "completed - cache: [{}], element: [{}]", getCacheName(), element );
    return element;
  }

  protected long getCurrentTimeMS() {
    return System.currentTimeMillis();
  }

  /* (non-Javadoc)
   * @see com.att.aft.dme2.cache.domain.DME2Cache#shutdownTimerTask()
   */
  @Override
  public void shutdownTimerTask() {
    LOGGER.debug( null, "AbstractCache.shutdownTimerTask", "start - cache: [{}]", getCacheName() );
    synchronized ( hashlock ) {
      for ( Iterator it = cacheScheduleTaskRegister.values().iterator(); it.hasNext(); ) {
        CacheTaskScheduler scheduler = (CacheTaskScheduler) it.next();
        LOGGER.debug( null, "AbstractCache.shutdownTimerTask", "invoking cancel for the cache level timer task [{}]",
            scheduler.getTaskName() );
        scheduler.cancel();
      }
      cacheScheduleTaskRegister.clear();
    }
    LOGGER.debug( null, "AbstractCache.shutdownTimerTask", "completed - cache: [{}]", getCacheName() );
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Value refreshEntry( Key key ) throws CacheException {
    LOGGER.debug( null, "AbstractCache.refresh key", "start - cache: [{}]", getCacheName() );
    Set<Key> keySet = new HashSet<Key>();
    keySet.add( key );
    refreshKeys( getKeySet() );
    LOGGER.debug( null, "AbstractCache.refresh key", "completed - cache: [{}]", getCacheName() );
    return get( key );
  }

  private Set<Key> removeCacheKeysNotExpired( Set<Key> keySet ) {
    Set<Key> returnKeySet = new HashSet<Key>();
    if ( getKeySet() != null && !getKeySet().isEmpty() ) {
      for ( Key k : keySet ) {
        if ( getExpirationTime( k.getString() ) <= System.currentTimeMillis() ) {
          returnKeySet.add( k );
        }
      }
    }
    return returnKeySet;
  }

  private Set<Key> removeCacheKeysContainingGroupRouteOffer( Set<Key> keySet ) {
    Set<Key> returnKeySet = new HashSet<Key>();
    if ( keySet != null && !keySet.isEmpty() ) {
      for ( Key k : keySet ) {
        Map<String, String> map = DME2Utils.splitServiceURIString( k.getString() );
        if ( map != null && map.containsKey( "routeOffer" ) ) {
          String routeOfferVal = map.get( "routeOffer" );
          // If the cached entry is of type grouped sequence routeOffer
          // E.g service=com.att.aft.DME2Echo/version=1.0/envContext=DEV/routeOffer=D1~D2
          // then do not trigger refresh
          // This logic needs to be revisited, if in future, DME2 sends findRunningEndpoints by
          // single routeOffer. Currently DME2 doesn't query GRM API by individual routeOffer,
          // but GRM supports it though
          if ( routeOfferVal != null && routeOfferVal.contains( DME2Constants.DME2_ROUTE_OFFER_SEP ) ) {
            LOGGER.debug( null, "AbstractCache.refresh", LogMessage.SKIP_REFRESH_ENDPOINTS, k.getString() );
            continue;
          }
        }
        returnKeySet.add( k );
      }
    }
    return returnKeySet;
  }

  private Set<Key> randomizeKeySet( Set<Key> keySet ) {
    Set<Key> returnKeySet = new HashSet<Key>();

    if ( keySet != null ) {
      List<Key> serviceURIList = new ArrayList<Key>();
      serviceURIList.addAll( keySet );

      Collections.shuffle( serviceURIList );

      for ( Key k : serviceURIList ) {
        returnKeySet.add( k );
      }
    }

    return returnKeySet;
  }

  private void refreshEndpointKeys( Set<Key> keySet ) {
    long startTime = 0l;
    setRefreshInProgress( true );
    try {
      if ( getCacheConfig().getDataLoader() != null ) {
        if ( getCacheConfig().getCacheDataSource() != null ) {
          if ( keySet != null && !keySet.isEmpty() ) {
            //remove the keys for which the entry is not yet expired
            keySet = removeCacheKeysNotExpired( keySet );
            //remove the keys for the entry key having group route offer
            keySet = removeCacheKeysContainingGroupRouteOffer( keySet );
            // CSSA-11214 - randomize the ep list being refreshed everytime.
            keySet = randomizeKeySet( keySet );

            if ( keySet != null && !keySet.isEmpty() ) {
              startTime = System.currentTimeMillis();
              @SuppressWarnings("unchecked")
              Map<Key, Pair<CacheElement, Exception>> data =
                  getCacheConfig().getDataLoader().getDataForAllKeys( keySet, getCacheConfig().getCacheDataSource() );
              if ( data != null ) {
                processCacheSourceData( keySet, data, startTime );
                LOGGER.debug( null, "AbstractCache.refreshEndpointKeys", "completed putting all data in the cache: [{}]",
                    getCacheName() );
              } else {
                LOGGER.debug( null, "AbstractCache.refreshEndpointKeys",
                    "completed; but no data found to refresh the cache: [{}]", getCacheName() );
              }
            } else {
              LOGGER.debug( null, "AbstractCache.refreshEndpointKeys",
                  "completed; but no data are found as stale to refresh the cache: [{}]", getCacheName() );
            }
          } else {
            LOGGER.debug( null, "AbstractCache.refreshEndpointKeys",
                "completed; but no key found to refresh the cache: [{}]", getCacheName() );
          }
        } else {
          LOGGER.debug( null, "AbstractCache.refreshEndpointKeys",
              "completed; but no data cache data source to refresh the cache: [{}]", getCacheName() );
        }
      } else {
        LOGGER.debug( null, "AbstractCache.refreshEndpointKeys",
            "completed; but no data loader added to refresh the cache: [{}]", getCacheName() );
      }

    } catch ( CacheException ce ) {
      LOGGER
          .debug( null, "AbstractCache.refreshEndpointKeys", "completed - cache: [{}], Exception: [{}]", getCacheName(),
              ce.getErrorMessage() );
    } catch ( HazelcastInstanceNotActiveException hze ) {
      LOGGER.debug( null, "AbstractCache.refreshEndpointKeys", "hazelcast is probably down!!!" );
    } finally {
      setRefreshInProgress( false );
    }
  }

  private void refreshKeys( Set<Key> keySet ) {
    if ( isEndpointCache( this ) ) {
      refreshEndpointKeys( keySet );
    } else {
      refreshRouteInfoKeys( keySet );
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void refresh() {
    LOGGER.debug( null, "AbstractCache.refresh", "start - cache: [{}]", getCacheName() );
    // refreshKeys( getKeySet() );
    getCacheConfig().getCacheDataSource().refresh();
    LOGGER.debug( null, "AbstractCache.refresh", "completed - cache: [{}]", getCacheName() );
  }

  private DME2CacheStatsHolder initCacheStatHolder( final String uriStr )
      throws MalformedURLException, URISyntaxException {
    DmeUniformResource uniformResource = new DmeUniformResource( getConfig(), new URI( "http://DME3LOCAL/" + uriStr ) );
    String service = uniformResource.getService();
    String version = uniformResource.getVersion();
    String env = uniformResource.getEnvContext();
    String cacheStatsURI = "/service=" + service + "/version=" + version + "/envContext=" + env;
    DME2CacheStatsHolder statsHolder = null;
    statsHolder = this.cacheStats.get( cacheStatsURI );
    if ( statsHolder == null ) {
      statsHolder = new DME2CacheStatsHolder( cacheStatsURI, getConfig() );
      this.cacheStats.put( cacheStatsURI, statsHolder );
    }
    long startTime = System.currentTimeMillis();
    return statsHolder;
  }

  @Override
  public boolean isCacheStatsEnabled() {
    return enableCacheStats;
  }

  @Override
  public void disableCacheStats() {
    enableCacheStats = false;

  }

  @Override
  public void enableCacheStats() {
    enableCacheStats = true;

  }

  public void refreshRouteInfoKeys( Set<Key> keySet ) {
    LOGGER.debug( null, "AbstractCache.refreshRouteInfo", "start - cache: [{}]", getCacheName() );
    setRefreshInProgress( true );
    long startTime = 0l;
    try {
      if ( getCacheConfig().getDataLoader() != null ) {
        if ( getCacheConfig().getCacheDataSource() != null ) {
          if ( keySet != null && !keySet.isEmpty() ) {
            //remove the keys for which the entry is not yet expired
            keySet = removeCacheKeysNotExpired( keySet );

            if ( keySet != null && !keySet.isEmpty() ) {
              startTime = System.currentTimeMillis();
              @SuppressWarnings("unchecked")
              Map<Key, Pair<CacheElement, Exception>> data =
                  getCacheConfig().getDataLoader().getDataForAllKeys( keySet, getCacheConfig().getCacheDataSource() );
              if ( data != null ) {

                for ( Key key : keySet ) {
                  if ( data.get( key ) != null ) {
                    CacheElement v = data.get( key ).getLeft();
                    Exception e = data.get( key ).getRight();

                    if ( e == null && v != null ) {
                      synchronized ( lock ) {
                        put( key, v );
                        LOGGER.debug( null, "AbstractCache.refreshRouteInfo", LogMessage.REFRESH_SERVICE, key );
                        try {
                          initCacheStatHolder( key.getString() )
                              .recordRefreshSuccess( System.currentTimeMillis() - startTime, isCacheStatsEnabled() );
                        } catch ( MalformedURLException e1 ) {
                        } catch ( URISyntaxException e1 ) {
                        }
                      }
                    } else if ( e != null ) {
                      try {
                        LOGGER.debug( null, "AbstractCache.refreshRouteInfo", LogMessage.REFRESH_SVC_FAILED, key );
                        initCacheStatHolder( key.getString() )
                            .recordRefreshFailure( System.currentTimeMillis() - startTime, isCacheStatsEnabled() );
                      } catch ( MalformedURLException e1 ) {
                      } catch ( URISyntaxException e1 ) {
                      }
                    }
                  } else {
                    LOGGER.debug( null, "AbstractCache.refreshRouteInfo", LogMessage.REFRESH_SVC_FAILED, key );
                  }
                }

                LOGGER.debug( null, "AbstractCache.refreshRouteInfo", "completed putting all data in the cache: [{}]",
                    getCacheName() );
              } else {
                LOGGER.debug( null, "AbstractCache.refreshRouteInfo",
                    "completed; but no data found to refresh the cache: [{}]", getCacheName() );
              }
            } else {
              LOGGER.debug( null, "AbstractCache.refreshRouteInfo",
                  "completed; but no data are found as stale to refresh the cache: [{}]", getCacheName() );
            }
          } else {
            LOGGER
                .debug( null, "AbstractCache.refreshRouteInfo", "completed; but no key found to refresh the cache: [{}]",
                    getCacheName() );
          }
        } else {
          LOGGER.debug( null, "AbstractCache.refreshRouteInfo",
              "completed; but no data cache data source to refresh the cache: [{}]", getCacheName() );
        }
      } else {
        LOGGER.debug( null, "AbstractCache.refreshRouteInfo",
            "completed; but no data loader added to refresh the cache: [{}]", getCacheName() );
      }

    } catch ( CacheException ce ) {
      LOGGER.debug( null, "AbstractCache.refreshRouteInfo", "completed - cache: [{}], Exception: [{}]", getCacheName(),
          ce.getErrorMessage() );
    } catch ( HazelcastInstanceNotActiveException hze ) {
      LOGGER.debug( null, "AbstractCache.refreshRouteInfo", "hazelcast is probably down!!!" );
    } finally {
      setRefreshInProgress( false );
    }
    LOGGER.debug( null, "AbstractCache.refreshRouteInfo", "completed - cache: [{}]", getCacheName() );
  }

  @Override
  public String getKeys() {
    LOGGER.debug( null, "AbstractCache.getKeys", "start - cache: [{}]", getCacheName() );
    StringBuffer buffer = new StringBuffer();
    if ( getKeySet() != null ) {
      buffer.append( "[" );
      for ( Key key : getKeySet() ) {
        buffer.append( key.getString() );
        buffer.append( "," );
      }
      buffer.append( "]" );
    } else {
      buffer.append( "Ugh! seems some difficulty in getting the keys now!" );
    }
    LOGGER
        .debug( null, "AbstractCache.getKeys", "end - cache: [{}], cache keys: []", getCacheName(), buffer.toString() );
    return buffer.toString();
  }

  @SuppressWarnings("rawtypes")
  protected void processCacheSourceData( final Set<Key> keySet,
                                         final Map<Key, Pair<CacheElement, Exception>> dataFromSource,
                                         final long startTime ) {
    LOGGER.debug( null, "processCacheSourceData", "start - cache: [{}]", getCacheName() );

    CacheElement v = null;
    Exception e = null;
    CacheElement cacheElement = null;
    CacheEntryView cacheEntryView = getEntryView();
    long ttl = 0;

    if ( dataFromSource != null && !dataFromSource.isEmpty() ) {
      //only process the filtered keyset as provided by the invoker of this method
      for ( Key serviceKey : keySet ) {
        Pair<CacheElement, Exception> entry = dataFromSource.get( serviceKey );

        if ( entry != null && entry.getLeft() != null && entry.getRight() == null ) {
          v = entry.getLeft();
          e = entry.getRight();
          cacheElement = v;
          if ( isPutAllow( serviceKey, v.getValue() ) ) {//only add if the put is allowed at this time
            long lastQueriedAt = 0;
            if ( cacheEntryView != null && cacheEntryView.getEntry( serviceKey ) != null ) {
              lastQueriedAt = cacheEntryView.getEntry( serviceKey ).getLastAccessedTime();
            }

            if ( lastQueriedAt > 0 && config.getBoolean( DME2Constants.AFT_DME2_ENABLE_SELECTIVE_REFRESH, false ) ) {
              if ( ( System.currentTimeMillis() - lastQueriedAt ) >= this.endpointLastQueriedInterval ) {
                ttl = infrequentEndpointCacheTTL;

              } else {
								/*refresh list on standard interval*/
                ttl = cacheEntryTTL;
              }
            } else {
							/*refresh list on standard interval*/
              ttl = cacheEntryTTL;
            }
            cacheElement = cacheElement.setTtl( ttl );
            put( serviceKey, cacheElement );
            try {
              initCacheStatHolder( serviceKey.getString() )
                  .recordRefreshSuccess( System.currentTimeMillis() - startTime, isCacheStatsEnabled() );
            } catch ( Exception statException ) {
            }
          }
        } else {
          revalidateEntryOnFailedRefresh( serviceKey );
        }
      }
    } else {
      for ( Key serviceKey : keySet ) {
        revalidateEntryOnFailedRefresh( serviceKey );
      }
    }
    LOGGER.debug( null, "processCacheSourceData", "completed - processCacheSourceData: [{}]", getCacheName() );
  }

  @Override
  public DME2CacheStats getStats( String serviceName, Integer hourOfDay ) {
	  LOGGER.info( null, "getStats ", "serviceName :" + serviceName + " hourOfDay : " + hourOfDay);
	  LOGGER.info( null, "getStats ", "cacheStats :" + cacheStats);
	  
    if ( serviceName != null && this.cacheStats.get( serviceName ) != null ) {
      DME2CacheStatsHolder stats = this.cacheStats.get( serviceName );
      if ( hourOfDay >= 0 && hourOfDay <= 23 ) {
        return stats.getHourlyStats( hourOfDay );
      }
      return this.cacheStats.get( serviceName ).getStats();
    }
    return null;
  }

  private void revalidateEntryOnFailedRefresh( final Key serviceKey ) {
		/*If we get here, then 0 endpoints returned from GRM.*/
		
		/*DON'T put the new list in the cache - if we already had a list, we'll keep using it until we get new non-zero list back*/
    LOGGER.warn( null, "revalidateEntryOnFailedRefresh", LogMessage.REFRESH_DEFERRED, serviceKey );
    long ttl = endpointCacheEmptyTTL;

    if ( get( serviceKey ) != null ) {
      int emptyCacheRefreshAttemptCount = getEntryView().getEntry( serviceKey ).getEmptyCacheRefreshAttemptCount();
      if ( emptyCacheRefreshAttemptCount == ( emptyCacheTTLRefreshIntervals.length - 1 ) ) {
        //If we get here, it means that we are already on the last refresh interval, so just stay at the same value
        ttl = emptyCacheTTLRefreshIntervals[emptyCacheTTLRefreshIntervals.length - 1];
        String msg = String.format(
            "SEP Empty Cache TTL has already reached the last interval for service %s. TTL value will remain at: %s. Current empty cache refresh attempt count: %s ",
            serviceKey, ttl, emptyCacheRefreshAttemptCount );
        LOGGER.debug( null, "revalidateEntryOnFailedRefresh", LogMessage.DEBUG_MESSAGE, msg );
      } else if ( emptyCacheTTLRefreshIntervals.length == 1 ) {
        //If only 1 interval is defined, keep using it until we get endpoints back.
        ttl = emptyCacheTTLRefreshIntervals[0];
      } else {
        //Increase the count and advance to the next interval in the array
        ++emptyCacheRefreshAttemptCount;
        ttl = emptyCacheTTLRefreshIntervals[emptyCacheRefreshAttemptCount];
        getEntryView().getEntry( serviceKey ).setEmptyCacheRefreshAttemptCount( emptyCacheRefreshAttemptCount );

        String msg = String
            .format( "Advancing to next Emtpy Cache TTL interval value for service %s. New value: %s", serviceKey,
                ttl );
        LOGGER.debug( null, "revalidateEntryOnFailedRefresh", LogMessage.DEBUG_MESSAGE,
            "New empty cache refresh attempt count: " + emptyCacheRefreshAttemptCount );
        LOGGER.debug( null, "revalidateEntryOnFailedRefresh", LogMessage.DEBUG_MESSAGE, msg );
      }
    }

    CacheElement element = getEntryView().getEntry( serviceKey );
    if ( element != null && element.getValue() != null ) {
      DME2ServiceEndpointData value = (DME2ServiceEndpointData) element.getValue().getValue();
      if ( value != null ) {
        value.setCacheTTL( ttl );
        element.setTtl( ttl );
      }
    }
  }

  private long[] getEmptyCacheTTLIntervalsFromProperties( String property, long[] defaultValue ) {
    long[] emptyCacheTTLIntervals = null;
    if ( property == null ) {
      return defaultValue;
    }

    try {
      String[] tokens = property.split( "," );
			
			/*Check if each value to make sure it is a valid long type*/
      for ( String token : tokens ) {
        if ( !DME2Utils.isParseable( token.trim(), Long.class ) ) {
          return defaultValue;
        }
        boolean enforceMinEmptyCacheTTLIntervalValue =
            config.getBoolean( DME2Constants.Cache.DME2_ENFORCE_MIN_EMPTY_CACHE_TTL_INTERVAL_VALUE, true );

        if ( enforceMinEmptyCacheTTLIntervalValue ) {
          if ( Long.parseLong( token ) < 300000 ) {
            String msg = String.format(
                "Interval values cannot be less than 5 minutes. Value provided: %s. Using default interval values of: %s",
                token, Arrays.asList( emptyCacheTTLRefreshDefaultIntervals ) );
            LOGGER.warn( null, "getEmptyCacheTTLIntervalsFromProperties", LogMessage.DEBUG_MESSAGE, msg );
            return defaultValue; /* If any interval value is less than 5 minutes, use the defaults */
          }
        }
      }

      emptyCacheTTLIntervals = new long[tokens.length];
      for ( int i = 0; i < tokens.length; i++ ) {
        emptyCacheTTLIntervals[i] = Long.parseLong( tokens[i].trim() );
      }
    } catch ( Exception e ) {
      LOGGER.debug( null, "getEmptyCacheTTLIntervalsFromProperties", LogMessage.DEBUG_MESSAGE,
          "Error occurred while attempting while resolving Empty SEP Cache TTL Intervals. Using default.", e );
      return defaultValue;
    }

    LOGGER.debug( null, "getEmptyCacheTTLIntervalsFromProperties", LogMessage.DEBUG_MESSAGE,
        "Empty SEP Cache TTL Intervals resolved from properties: " + property );
    return emptyCacheTTLIntervals;
  }

  @Override
  public void run() {
    shutdownTimerTask();
  }

  /**
   * make this cache entry not accessible till the time its explicitly unlocked however, the lock is not for infinite
   * period; the period of lock has a timeout which is determined by the value set for this external variable
   * <i>[com.att.aft.dme2.cache.domain.DME2Constants.Cache.LOCK_TIMEOUT_MS]</i>
   *
   * @param k key of the entry of this cache
   */
  public abstract void lock( Key k );

  /**
   * if the entry is locked, then unlock the cache entry with the specified key
   *
   * @param k key of the cache entry to unlock
   */
  public abstract void unlock( Key k );

  public abstract void put( final Key k, final CacheElement element );

  public abstract M getCacheMap();

  public abstract boolean isPutAllow( Key key, Value value );

  public abstract void setRefreshInProgress( final boolean refreshInProgress );
}