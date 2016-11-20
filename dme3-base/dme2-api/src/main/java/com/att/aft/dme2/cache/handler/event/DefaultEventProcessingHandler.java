/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 *
 */
package com.att.aft.dme2.cache.handler.event;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.domain.CacheEvent;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.cache.handler.service.CacheEventHandler;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.Fork;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/**
 * @author ab850e
 */
public class DefaultEventProcessingHandler implements CacheEventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger( DefaultEventProcessingHandler.class.getName() );
  private static final String GET_DATA_THREAD_NAME = "DME2_CACHE_GET_DATA";
  //private static final long TIME_TO_WAIT_FOR_DATA_ASYNC_MS = DME2Configuration.getLongProp(DME2Constants.Cache.TIME_TO_WAIT_FOR_DATA_ASYNC_MS);
  private static transient boolean GET_CACHE_DATA_ASYNC;
  private static final ExecutorService CacheableDataAsyncService =
      Fork.createFixedThreadExecutorPool( GET_DATA_THREAD_NAME, 50 );//TBD: need to think about this number
  
  public DefaultEventProcessingHandler(DME2Configuration config) {

	  GET_CACHE_DATA_ASYNC = config.getBoolean( DME2Constants.Cache.GET_CACHE_DATA_ASYNC );
  }

  @Override
  public void onPut( DME2Cache cache, CacheEvent cacheEvent ) {
  }

  @Override
  public void onGet( DME2Cache cache, CacheEvent cacheEvent ) {
  }

  @Override
  public void onUpdate( DME2Cache cache, CacheEvent cacheEvent ) {
  }

  @Override
  public void onRemove( DME2Cache cache, CacheEvent cacheEvent ) {
  }

  @Override
  public void onBeforeRemove( DME2Cache cache, CacheEvent cacheEvent ) {
  }

  @Override
  public void onEviction( DME2Cache cache, CacheEvent cacheEvent ) {
    LOGGER.info( null, "onEviction", "Processing EVENT: [{}] ", cacheEvent.getCacheEventType().toString() );
    throw new CacheException( ErrorCatalogue.CACHE_013, ( (AbstractCache) cache ).getCacheName() );
  }

  @Override
  public void onRefresh( DME2Cache cache, CacheEvent cacheEvent ) {
  }

  @SuppressWarnings("rawtypes")
  private Value populateData( final DME2Cache cache, final CacheEvent cacheEvent ) {
    final Key key = cacheEvent.getCacheOldElement().getKey();
    Value value = null;
    try {
      LOGGER.info( null, "populateData", "trying to get cacheable data {} for cache:{}, {}",
          GET_CACHE_DATA_ASYNC ? "asynchronously" : "synchronously", ( (AbstractCache) cache ).getCacheName(), key );
      value = cache.refreshEntry( key );
    } catch ( CacheException ce ) {
      LOGGER.error( null, "populateData", "cannot get cacheable data: [{}], keeping the old value {}=[{}]", ce.getErrorMessage(),
          cacheEvent.getCacheOldElement().getValue() );
    }
    return value;
  }

  @SuppressWarnings("rawtypes")
  private Value populateDataAsync( final DME2Cache cache, final CacheEvent cacheEvent ) {
    Value newValue = null;
    final Key key = cacheEvent.getCacheOldElement().getKey();
    LOGGER.info( null, "populateDataAsync", "requesting to load cacheable data asynchronously {}, {}",
        ( (AbstractCache) cache ).getCacheName(), key );
    FutureTask<Value> getDataAsyncTask = new FutureTask<Value>( new Callable<Value>() {
      @Override
      public Value call() {
        Value v = populateData( cache, cacheEvent );
        LOGGER.info( null, "populateDataAsync-call", "completed request for loading of cacheable data asynchronously {}, {}",
            ( (AbstractCache) cache ).getCacheName(), key );
        return v;
      }
    } );
    CacheableDataAsyncService.execute( getDataAsyncTask );

    return newValue;
  }

  @SuppressWarnings("rawtypes")
  public void fetchDataAndLoad( final DME2Cache cache, final CacheEvent cacheEvent ) {
    LOGGER.info( null, "fetchDataAndLoad", "EVENT: [{}]", cacheEvent.getCacheEventType().toString() );
    Value newValue = null;
    Key key = cacheEvent.getCacheOldElement().getKey();
    //LOGGER.info(null,null,"start data retreival; setting the old value by default");
    //cache.update(cacheEvent.getCacheOldElement());
    if ( GET_CACHE_DATA_ASYNC ) {
      LOGGER.info( null, "fetchDataAndLoad", "asynchronous data retreival; setting the old value by default" );

      populateDataAsync( cache, cacheEvent );
    } else {
      try {
        //cache.lock(key);
        newValue = populateData( cache, cacheEvent );
        if ( newValue == null ) {
          LOGGER.info( null, "fetchDataAndLoad", "cannot get fresh cache data for cache {}, {}", ( (AbstractCache) cache ).getCacheName(),
              key );
          throw new RuntimeException( "cannot remove data" );
        }

        LOGGER.info( null, "fetchDataAndLoad", "fresh cache data reloaded for cache {}, {}, {}",
            ( (AbstractCache) cache ).getCacheName(), key, newValue );

      } catch ( CacheException ce ) {
        if ( ce.getErrorCode().equals( CacheException.ErrorCatalogue.CACHE_005 ) ) {
          LOGGER.warn( null, "fetchDataAndLoad", "fetchDataAndLoad exception: [{}]", ce.getErrorMessage() );
        } else {
          throw ce;
        }
      } finally {
        //cache.unlock(key);
      }
    }

    LOGGER.info( null, "fetchDataAndLoad", "End fetchDataAndLoad EVENT: [{}]", cacheEvent.getCacheEventType().toString() );
  }
}