package com.att.aft.dme2.cache.persistence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheElement.Key;
import com.att.aft.dme2.cache.domain.CacheElement.Value;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.cache.service.CacheSerialization;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.manager.registry.DME2ServiceEndpointData;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.ErrorContext;

public class CacheSerializationToFile implements CacheSerialization {
  private static final Logger LOGGER = LoggerFactory.getLogger( CacheSerializationToFile.class.getName() );
  private int persistAttemptsCounter = 0;
  private static final long DEFAULT_WAIT_TIME_MS = 2000;
  private boolean removePersistedEndpointsOnStartup;
  private ObjectMapper mapper = new ObjectMapper();
  private Object lock = new Object();
  private boolean isEndpointCache = false; 

  public CacheSerializationToFile( final DME2Cache cache, final DME2Configuration config, final boolean isEndpointCache) {
    removePersistedEndpointsOnStartup =
        Boolean.parseBoolean( System.getProperty( DME2Constants.DME2_REMOVE_PERSISTENT_CACHE_ON_STARTUP ) ) ||
            config.getBoolean( DME2Constants.DME2_REMOVE_PERSISTENT_CACHE_ON_STARTUP, false );
    LOGGER.debug( null, "CacheSerializationToFile", LogMessage.DEBUG_MESSAGE, "Removing persisted Endpoints on startup: " + removePersistedEndpointsOnStartup );

    if ( removePersistedEndpointsOnStartup ) {
      removePersistedEndpointCacheAtStartUp( cache, config );
    }
    
    this.isEndpointCache = isEndpointCache;
  }

  public void removePersistedEndpointCacheAtStartUp( final DME2Cache cache, final DME2Configuration config ) {
    LOGGER.debug( null, "removePersistedEndpointCacheAtStartUp", LogMessage.METHOD_ENTER );
    File cachePersistentFile = null;

    try {
      cachePersistentFile = resolveCachePersistenceFile( cache, config, false );
      if ( cachePersistentFile.exists() ) {
        cachePersistentFile.delete();
        LOGGER.debug( null, "removePersistedEndpointCacheAtStartUp", LogMessage.DEBUG_MESSAGE,
            "Successfully removed Endpoints from persistent cache on application startup." );
      }
    } catch ( Exception e ) {
      ErrorContext ec = new ErrorContext();
      ec.add( "persistentCacheFileName: ",
          cachePersistentFile != null ? cachePersistentFile.getAbsolutePath() : "null" );

      DME2Exception exception = new DME2Exception( "AFT-DME2-0616", ec );
      LOGGER.warn( null, "removePersistedEndpointCacheAtStartUp", LogMessage.DEBUG_MESSAGE,
          "Error occurred while attempted to remove Endpoints from persistent cache on application startup.",
          exception );
    }
  }

  public boolean persist( DME2Cache cache, DME2Configuration config ) {
    File cachePersistentFile = null;
    LOGGER.debug( null, "persist", LogMessage.METHOD_ENTER, " for cache: {}",
        cache != null ? cache.getCacheName() : "cache is null" );

    try {
      if ( Boolean.valueOf( System.getProperty( DME2Constants.Cache.DME2_DISABLE_PERSISTENT_CACHE ) ) ||
          config.getBoolean( "DME3_DISABLE_PERSISTENT_CACHE", false ) ) {
        LOGGER.debug( null, "persist", "Persistence cache feature is disabled, skipping operation." );
        return false;
      }

			/* No need to persist to file if the Cache is empty */
      if ( cache.getKeySet().isEmpty() ) {
        LOGGER.debug( null, "persist", "Cache is empty, skipping persistence operation." );
        return true;
      }

      waitForRefresh( cache, config );

      cachePersistentFile = resolveCachePersistenceFile( cache, config, true );

      synchronized ( lock ) {
        if ( cachePersistentFile != null ) {

	          /* ObjectMapper will take cache values and convert them into a JSON representation */
          Map<Key, CacheElement> cacheMapToPersist = Collections.synchronizedMap( new HashMap<Key, CacheElement>() );
          for ( Key key : cache.getKeySet() ) {
            cacheMapToPersist.put( key, createCachePersistingElement( key, cache ) );
          }
          try {
            PrintWriter out =
                new PrintWriter( new BufferedWriter( new FileWriter( cachePersistentFile.getAbsolutePath(), true ) ) );
            JsonGenerator g = mapper.getJsonFactory().createJsonGenerator( out );
            mapper.writeValue( cachePersistentFile, cacheMapToPersist.values() );
          } catch ( org.codehaus.jackson.JsonGenerationException mapGenEx ) {
            throw new CacheException( ErrorCatalogue.CACHE_023, mapGenEx, mapGenEx.getMessage() );
          } catch ( org.codehaus.jackson.map.JsonMappingException mapEx ) {
            throw new CacheException( ErrorCatalogue.CACHE_023, mapEx, mapEx.getMessage() );
          } catch ( IOException ioex ) {
            throw new CacheException( ErrorCatalogue.CACHE_023, ioex, ioex.getMessage() );
          }
          LOGGER.debug( null, "persist", LogMessage.DEBUG_MESSAGE,
              "Successfully persisted endpoints to file: " + cachePersistentFile + "Number of endpoints persisted: " +
                  cacheMapToPersist.values().size()
          );
          return true;
        }
      }
    } catch ( Exception ex ) {
      LOGGER.debug( null, "persist", LogMessage.DEBUG_MESSAGE,
          "Error occured while persisting cache {} entries to file: {}",
          cache != null ? cache.getCacheName() : "null", cachePersistentFile );
    }
    LOGGER.debug( null, "persist", LogMessage.METHOD_EXIT );

    return false;
  }

  @Override
  public boolean load( final DME2Cache cache, final DME2Configuration config ) {
    LOGGER.debug( null, "load", LogMessage.METHOD_ENTER, " for cache: {}",
        cache != null ? cache.getCacheName() : "cache is null" );
    boolean loaded = false;
    File cachePersistentFile = null;
    List<CacheElement> persistedData = null;

    try {
      if ( !removePersistedEndpointsOnStartup ) {
          /* If AFT_DME2_DISABLE_PERSISTENT_CACHE_LOAD not true, ignore loading the persisted data from the file */
        if ( !config.getBoolean( DME2Constants.Cache.DME2_DISABLE_PERSISTENT_CACHE_LOAD ) ) {
          LOGGER.debug( null, "load", LogMessage.DEBUG_MESSAGE,
              "[] = false. Persisted Endpoints will NOT be loaded from file. Skipping operation.",
              DME2Constants.Cache.DME2_DISABLE_PERSISTENT_CACHE_LOAD );
        } else {
          cachePersistentFile = resolveCachePersistenceFile( cache, config, false );
          if ( cachePersistentFile == null ) {
            LOGGER.warn( null, "load", LogMessage.DEBUG_MESSAGE,
                "persistent store reference was not provided to warm up cache []. Skipping cache warm up operation.",
                cache != null ? cache.getCacheName() : "null" );
          } else {

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure( DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true );
            mapper.configure( DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false );
            try {
              synchronized ( lock ) {
                persistedData = mapper.readValue( cachePersistentFile, new TypeReference<List<CacheElement>>() {
                } );
                loaded = true;
              }
            } catch ( org.codehaus.jackson.JsonGenerationException mapGenEx ) {
              throw new CacheException( ErrorCatalogue.CACHE_023, mapGenEx, mapGenEx.getMessage() );
            } catch ( org.codehaus.jackson.map.JsonMappingException mapEx ) {
              throw new CacheException( ErrorCatalogue.CACHE_023, mapEx, mapEx.getMessage() );
            } catch ( IOException ioex ) {
              throw new CacheException( ErrorCatalogue.CACHE_023, ioex, ioex.getMessage() );
            }

            LOGGER.debug( null, "load", "Successfully loaded persisted endpoints from file: {}",
                cachePersistentFile.getAbsolutePath() );
	    				/* Dump all of the entries from the persistent store into the main cache */
            for ( CacheElement data : persistedData ) {
              //object mapper deserialized only the main parent CacheELement, all child are essentially returned as Map
              //next we are trying to get the actual objects hydrated from the map as returned Value
              Value v = null;
              if ( data.getValue().getValue() instanceof java.util.Map ) {
                v = convertMapToJson( cache, (Map) data.getValue().getValue() );
              } else {
                v = data.getValue();
              }
              cache.put( data.getKey(), v );
            }
            LOGGER.debug( null, "load", "Cache warmed with #data: {}", cache.getCurrentSize() );
          }
        }
      } else {
        LOGGER.debug( null, "load",
            "Ignoring loading as its been specified to delete the cache persistence file on startup: cache :{}",
            cachePersistentFile != null ? cachePersistentFile.getAbsolutePath() : "NULL" );
      }
    } catch ( Exception e ) {
      LOGGER.debug( null, "load", "Ignoring error while loading persisted endpoints from file:{} ",
          cachePersistentFile != null ? cachePersistentFile.getAbsolutePath() : "NULL", e );
    }
    LOGGER.debug( null, "load", LogMessage.METHOD_EXIT );
    return loaded;
  }

  private CacheElement createCachePersistingElement( Key k, DME2Cache cache ) {
    LOGGER.debug( null, "createElement", "start" );
    CacheElement element = new CacheElement()
        .setKey( k )
        .setValue( cache.getEntryView().getEntry( k ).getValue() );

    LOGGER.debug( null, "createElement", "completed element: [{}]", element );
    return element;
  }

  private void waitForRefresh( final DME2Cache cache, final DME2Configuration config ) {
    if ( cache.isRefreshing() ) {
    	
    	String waitSchedule = isEndpointCache?config.getProperty(DME2Constants.Cache.DME2_PERSIST_CACHED_ENDPOINTS_DELAY_MS): config.getProperty(DME2Constants.Cache.DME2_PERSIST_CACHED_ROUTEINFO_DELAY_MS) ;
    	
    	if(waitSchedule!=null && !waitSchedule.isEmpty()){
    		threadWait( Integer.parseInt( waitSchedule ) );
    		
    	}else{
    		waitSchedule = config.getProperty( DME2Constants.Cache.CACHE_PERSISTENCE_WAIT_SCHEDULE_FOR_REFRESH_MS );

	    	if ( waitSchedule != null ) {
	    		try {
	    			StringTokenizer tokens = new StringTokenizer( waitSchedule );
	    			while ( tokens.hasMoreTokens() ) {
	    				LOGGER.warn( null, "waitForRefresh",
	    						"Delaying peristCachedEndpoints operation for {}ms so that the refeshEndpoints operation can complete. Attempt# {}",
	    						true, persistAttemptsCounter );
	    				threadWait( Integer.parseInt( tokens.nextToken() ) );
	    			}
	    		} catch ( NullPointerException | NumberFormatException ex ) {
	    			LOGGER.warn( null, "waitForRefresh", "Cache persistence wait schedule has not been configured properly, {}",
	    					waitSchedule );
	    			cachePersistenceDefaultWaitForRefresh();
	    		}
	    	} else {
	    		cachePersistenceDefaultWaitForRefresh();
	    	}
    	}
    }
  }

  private void threadWait( final long waitTime ) {
    try {
      Thread.sleep( waitTime );
    } catch ( InterruptedException e ) {
      LOGGER.warn( null, "waitForRefresh", "Cache persistence wait was interrupted." );
      cachePersistenceDefaultWaitForRefresh();
    }
  }

  private void cachePersistenceDefaultWaitForRefresh() {
    LOGGER
        .warn( null, "waitForRefresh", "Waiting for default time {}ms for refresh to complete", DEFAULT_WAIT_TIME_MS );
    try {
      Thread.sleep( DEFAULT_WAIT_TIME_MS );
    } catch ( InterruptedException e ) {
      LOGGER.warn( null, "waitForRefresh", "Cache persistence default wait was interrupted." );
    }
  }

  private String getCachePersistenceFileNameWithAbsolutePath( final DME2Configuration config, final DME2Cache cache ) {
    String instanceName = DME2Utils.getRunningInstanceName( config );
    String cachedEndpointsFile = null;

    if ( instanceName != null ) {
      if ( !instanceName.startsWith( File.separator ) ) {
        instanceName = File.separator + instanceName;
      }

      String cachePersistenceDirName = resolveCachePersistenceDirName( config );
      if ( cachePersistenceDirName != null ) {
        LOGGER.debug( null, "getCachePersistenceFileNameWithAbsolutePath", "directory name = {}",
            cachePersistenceDirName );
        cachedEndpointsFile = buildCachePeristenceFileName( cachePersistenceDirName, instanceName,
            cache.getCacheConfig().getCacheType().getName() );
      } else {
        cachedEndpointsFile = null;
        LOGGER.warn( null, "getCachePersistenceFileNameWithAbsolutePath",
            "cannot find any directory name for cache persistence" );
      }
    }

    if ( cachedEndpointsFile == null ) {
      cachedEndpointsFile = getCacheFileNameFromUserConfig( cache, config );
    }

    return cachedEndpointsFile;
  }

  private String buildCachePeristenceFileName( final String cachePersistenceDirName, final String instanceName,
                                               final String cacheType ) {
    String cachedEndpointsFile = null;
    String endpointCacheExt =
        cachePersistenceDirName + ".aft" + File.separator + instanceName + File.separator + ".cached-endpoints.ser";
    String routeInfoCacheExt =
        cachePersistenceDirName + ".aft" + File.separator + instanceName + File.separator + ".cached-routeinfo.ser";

    switch ( cacheType ) {
      case DME2Constants.Cache.Type.ENDPOINT:
        cachedEndpointsFile = endpointCacheExt;
        break;
      case DME2Constants.Cache.Type.ROUTE_INFO:
        cachedEndpointsFile = routeInfoCacheExt;
        break;
    }
    return cachedEndpointsFile;
  }

  private String getCacheFileNameFromUserConfig( final DME2Cache cache, final DME2Configuration config ) {
    String cachePersistenceFilePropName = null;
    String cachedPersistenceFileName = null;

    switch ( cache.getCacheConfig().getCacheType().getName() ) {
      case DME2Constants.Cache.Type.ENDPOINT:
        cachePersistenceFilePropName = DME2Constants.DME2_CACHED_ENDPOINTS_FILE;
        break;
      case DME2Constants.Cache.Type.ROUTE_INFO:
        cachePersistenceFilePropName = DME2Constants.DME2_CACHED_ROUTEINFO_FILE;
        break;
    }

    if ( cachePersistenceFilePropName != null ) {
      cachedPersistenceFileName = System.getProperty( cachePersistenceFilePropName );
      if ( cachedPersistenceFileName == null ) {
        cachedPersistenceFileName = config.getProperty( cachePersistenceFilePropName, null );
        LOGGER.debug( null, "resolvePersistedEndpointsFile", LogMessage.DEBUG_MESSAGE,
            "Value of Persistent Cache File: " + cachedPersistenceFileName );
      }
    }
    return cachedPersistenceFileName;
  }

  private File getFile( String fileNameWithPath, final DME2Configuration config, boolean bcreate ) {
    File cachePersistentFile = null;
    try {
    	try {
    		cachePersistentFile = new File( fileNameWithPath );
    	} catch (Exception e) {
    		PrintWriter writer = new PrintWriter(fileNameWithPath, "UTF-8");
    		writer.println("");
    		writer.close();
    		cachePersistentFile = new File( fileNameWithPath );
    	}
      
	  if( !(cachePersistentFile.isFile() && cachePersistentFile.exists()) ) {
		  if(bcreate){
			if ( cachePersistentFile.getParentFile() != null ) {
				cachePersistentFile.getParentFile().mkdirs();
				LOGGER.debug( null, "getFile", "trying to create file [{}] for cache persistence",cachePersistentFile.getName() );
				cachePersistentFile.createNewFile();
				LOGGER.debug( null, "getFile", "created file [{}] for cache persistence",cachePersistentFile.getAbsolutePath() );
			}else{
				cachePersistentFile = new File( resolveCachePersistenceDirName(config).concat(fileNameWithPath));
				if( !(cachePersistentFile.isFile() && cachePersistentFile.exists()) ) {
					LOGGER.debug( null, "getFile", "trying to create file [{}] for cache persistence relative to user dir",cachePersistentFile.getName() );
					cachePersistentFile.createNewFile();
					LOGGER.debug( null, "getFile", "created file [{}] for cache persistence relative to user dir",cachePersistentFile.getAbsolutePath() );
				}
			}
		  }else{
			  if(fileNameWithPath!=null && !(fileNameWithPath.startsWith("\\") || fileNameWithPath.startsWith("/"))){
				  fileNameWithPath = ("/").concat(fileNameWithPath);
			  }
					  cachePersistentFile = new File( this.getClass().getResource(fileNameWithPath).getFile() );
			  LOGGER.debug( null, "getFile", "created file [{}] for cache persistence",cachePersistentFile.getAbsolutePath() );
		  }
	  }
    }catch(Exception e){
    	LOGGER.error( null, "getFile", "error: [{}] -- while processing cache file [{}] for persistence",cachePersistentFile!=null?cachePersistentFile.getName():"null",e.getMessage() );
    }
    return cachePersistentFile;
  }

  private File resolveCachePersistenceFile( final DME2Cache cache, final DME2Configuration config, boolean bcreate ) {
    String cachePersistenceDirName = null;
    String instanceName = cache.getCacheName();
    File cachePersistentFile = null;

    if ( instanceName != null ) {
      LOGGER.debug( null, "resolveCachePersistenceFile", "directory name = {}", cachePersistenceDirName );
      cachePersistentFile = getFile( getCachePersistenceFileNameWithAbsolutePath( config, cache ), config, bcreate );
    } else {
      cachePersistentFile = null;
      LOGGER.debug( null, "resolveCachePersistenceFile", "cache name is not available in the cache {}", cache );
    }
    return cachePersistentFile;
  }

  public String hashString( final String str ) {
    String hexStr = null;
    try {
      if ( str != null ) {
        hexStr = DigestUtils.sha256Hex( str );
        LOGGER.debug( null, "hashString", "Hex format of [{}] using SHA256: [{}]", str, hexStr );

        return hexStr;
      }
    } catch ( Exception e ) {
      LOGGER.debug( null, "hashString", "Error [{}], converting input [{}] ", e, str );
    }

    return str;
  }

  private String resolveCachePersistenceDirName( final DME2Configuration config ) {
    String cachePersistenceDirName = null;

    LOGGER.debug( null, "resolveCachePersistenceDirName",
        "cache persistence directory name being retreived from config {}",
        DME2Constants.Cache.CACHE_FILE_PERSISTENCE_DIR );

    //try to get the cache persistence directory from the config
    cachePersistenceDirName = config.getProperty( DME2Constants.Cache.CACHE_FILE_PERSISTENCE_DIR );
    if ( !resolveDirectory( cachePersistenceDirName ) ) {
      LOGGER.debug( null, "resolveCachePersistenceDirName",
          "cache persistence directory name being retreived from the system property \"user.home\"" );
      try {
        //try to get the cache persistence directory from the config
        cachePersistenceDirName = System.getProperty( "user.home" );
      } catch ( SecurityException se ) {
        LOGGER.warn( null, "resolvePersistedFileName",
            "cache persistence directory property \"user.home\" as set in the system property do not have correct privilege to be retreived" );
      }
      if ( !resolveDirectory( cachePersistenceDirName ) ) {
        //try to get the default cache persistence directory
        LOGGER.debug( null, "resolveCachePersistenceDirName",
            "default cache persistence directory \"\tmp\" is being used" );
        cachePersistenceDirName = "\tmp";
        if ( !resolveDirectory( cachePersistenceDirName ) ) {
          //last chance failed so set dir as null
          cachePersistenceDirName = null;
        }
      }
    }

    if ( cachePersistenceDirName != null && !cachePersistenceDirName.endsWith( File.separator ) ) {
      cachePersistenceDirName = cachePersistenceDirName.concat( File.separator );
    }

    return cachePersistenceDirName;
  }

  private boolean resolveDirectory( String dirName ) {
    boolean created = false;
    try {
      File f = new File( dirName );
      try {
        if ( !f.exists() ) {
          LOGGER.debug( null, "createDirectory", "cache persistence directory [{}] does not exist, attempting to create",
              dirName );
          f.mkdirs();
          created = true;
        } else {
          if ( !f.isDirectory() ) {
            LOGGER.warn( null, "createDirectory",
                "cache persistence directory [{}] in the configuration is not a directory, probably some file exists with the same name",
                dirName );
          } else {
            created = true;
          }
        }
      } catch ( SecurityException se ) {
        LOGGER.warn( null, "createDirectory",
            "cache persistence directory [{}] creation failed! Proper access privilege needs to be provided", dirName );
      }
    } catch ( NullPointerException npe ) {
      LOGGER.warn( null, "createDirectory", "cache persistence directory - [{}] creation failed!", dirName );
    }
    return created;
  }

  @Override
  public boolean isStale( DME2Cache cache, DME2Configuration config ) {
    File cachePersistentFile = resolveCachePersistenceFile( cache, config, false );
    if ( cachePersistentFile == null ) {
      LOGGER.warn( null, "load", "persistent store reference was not provided to check stale persistent cache []",
          cache != null ? cache.getCacheName() : "null" );
    } else {
      if ( System.currentTimeMillis() - cachePersistentFile.lastModified() >
          config.getLong( DME2Constants.Cache.CACHE_SERIALIZED_FILE_STALE_TIME_MS ) ) {
        return true;
      }
    }
    return false;
  }

  private Value convertMapToJson( final DME2Cache cache, final Map jsonMap ) {
    Value v = null;
    String json = "";

    try {
      ObjectMapper mapper = new ObjectMapper();

      // convert map to JSON string
      json = mapper.writeValueAsString( jsonMap );

      switch ( cache.getCacheConfig().getCacheType().getName() ) {
        case DME2Constants.Cache.Type.ENDPOINT:
          DME2ServiceEndpointData dME3ServiceEndpointData = mapper.readValue( json, DME2ServiceEndpointData.class );
          v = new Value<DME2ServiceEndpointData>( dME3ServiceEndpointData );
          break;
        case DME2Constants.Cache.Type.ROUTE_INFO:
          DME2RouteInfo dME3RouteInfo = mapper.readValue( json, DME2RouteInfo.class );
          v = new Value<DME2RouteInfo>( dME3RouteInfo );
          break;
        default:
      }
    } catch ( JsonGenerationException e ) {
      throw new CacheException( ErrorCatalogue.CACHE_023, e, e.getMessage() );
    } catch ( JsonMappingException e ) {
      throw new CacheException( ErrorCatalogue.CACHE_023, e, e.getMessage() );
    } catch ( IOException e ) {
      throw new CacheException( ErrorCatalogue.CACHE_023, e, e.getMessage() );
    }
    return v;
  }
}
