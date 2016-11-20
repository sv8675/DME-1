package com.att.aft.dme2.manager.registry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2DistanceUtil;
import com.att.aft.dme2.manager.registry.util.DME2FileUtil;
import com.att.aft.dme2.util.DME2URIUtils;

/**
 * This is the source object for registries that do not use GRM (or future alternative external sources)
 *
 * TODO: This should ideally only allow "retrieveEndpoints" and "storeEndpoints"
 */

public class DME2FileHandler {
  private static final Logger logger = LoggerFactory.getLogger( DME2FileHandler.class );

  // TODO: Move to constants
  private static final String EXCEPTION_EPREGISTRY_FS_LOCK_FILE = "AFT-DME2-9601";
  private static final String EXCEPTION_EPREGISTRY_FS_IO_EXC = "AFT-DME2-9600";
  private final long defaultCacheStaleness;

  private File baseDir;
  private String service;
  private File sourceFile;
  private List<File> inputFiles;
  private double clientLatitude, clientLongitude;

  /**
   * Constructs a File Handler from the base service with hierarchical lookup.
   *
   * @param dir base directory
   * @param service full service path
   * @param defaultCacheStaleness how long (in ms) before cache goes stale
   * @throws DME2Exception
   */
  public DME2FileHandler( File dir, String service, long defaultCacheStaleness, double clientLatitude, double clientLongitude ) throws DME2Exception {
	  sourceFile = new File( dir, service + ".txt" );
	  if ( !sourceFile.exists() ) {
      try {
        if ( !new File( sourceFile.getParent() ).mkdirs() && !sourceFile.createNewFile() ) {
          throw new DME2Exception( EXCEPTION_EPREGISTRY_FS_IO_EXC, "Unable to create file for service " + service + " with filename " + sourceFile.getName() );
        }
      } catch ( IOException e ) {
        throw new DME2Exception( EXCEPTION_EPREGISTRY_FS_IO_EXC, "Unable to create file for service " + service, e );
      }
    }
    inputFiles = DME2FileUtil.hierarchicalFileLookup( dir, service + ".txt" );
    if ( inputFiles.isEmpty() ) {
      inputFiles.add( sourceFile );
    }
    this.baseDir = dir;
    this.service = service;
    this.defaultCacheStaleness = defaultCacheStaleness;
    this.clientLatitude = clientLatitude;
    this.clientLongitude = clientLongitude;
  }

  /**
   * Returns the oldest (smallest) time value for the list of possible files.
   * @return Time last modified in epoch seconds
   */
  public long getLastModified() {
    return sourceFile.lastModified();
  }

  public List<DME2Endpoint> readEndpoints() throws DME2Exception {
    return propsToEps( readProperties(), service );
  }

  public Properties readProperties() throws DME2Exception {
    Properties props = new Properties();

    for ( File inputFile : inputFiles ) {
      String lockFileName = inputFile.getAbsolutePath();
      // Replace the .txt on the end with .lock
      lockFileName = lockFileName.substring( 0, lockFileName.length() - 4 ) + ".lock";
      File lockfile = new File( lockFileName );
      RandomAccessFile randomAccessFile = null;
      FileLock fileLock = null;


      try {
        randomAccessFile = new RandomAccessFile( lockfile, "rw" );
        fileLock = randomAccessFile.getChannel().tryLock();

        if ( fileLock != null ) {
          Properties theseProps = loadPropsFromFile( inputFile );
          for( String s : theseProps.stringPropertyNames() ) {
            props.setProperty( s, theseProps.getProperty( s ));
          }
        } else {
          throw new DME2Exception( EXCEPTION_EPREGISTRY_FS_LOCK_FILE, "File lock " + lockfile + " is in use" );
        }
      } catch ( IOException e ) {
        throw new DME2Exception( EXCEPTION_EPREGISTRY_FS_IO_EXC, e );
      } finally {
        if ( fileLock != null && fileLock.isValid() ) {
          try {
            fileLock.release();
          } catch ( IOException e ) {
            logger.debug( null, null, "readProperties", LogMessage.DEBUG_MESSAGE, "IO Exception", e );
          }
        }
        if ( randomAccessFile != null ) {
          try {
            randomAccessFile.close();
          } catch ( IOException e ) {
            logger.debug( null, null, "readProperties", LogMessage.DEBUG_MESSAGE, "IO Exception", e );
          }
        }
      }
    }
    return props;
  }

  public void storeProperties( Properties props, boolean includeTimestamp ) throws DME2Exception {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter( new FileWriter( sourceFile ) );
      String msg = includeTimestamp ? "" + System.currentTimeMillis() : "";
      props.store( writer, msg );
    } catch ( IOException e ) {
      throw new DME2Exception( EXCEPTION_EPREGISTRY_FS_IO_EXC, e );
    } finally {
      if ( writer != null ) {
        writer.close();
      }
    }
  }

  private Properties loadPropsFromFile( File inputFile ) throws IOException {
    inputFile.getParentFile().mkdir();
    inputFile.createNewFile();
    Properties props = new Properties();
    FileInputStream is = null;
    try {
      is = new FileInputStream( inputFile );
      props.load( is );
    } finally {
      if ( is != null ) {
        is.close();
      }
    }
    return props;
  }


  List<DME2Endpoint> propsToEps( Properties props, String path ) {
    List<DME2Endpoint> list = new ArrayList<DME2Endpoint>();

    ENDPOINT_LOOP:
    for ( Object obj : props.keySet() ) {
      String key = (String) obj;
      String[] hostPort = key.split( "," );
      String valueStr = props.getProperty( key );
      String[] valuePairs = valueStr.split( ";" );

      // Need the lat/long first

      Double latitude = null, longitude = null;

      for ( String pairStr : valuePairs ) {
        String[] pair = pairStr.split("=");
        if ( pair.length != 2 ) {
          continue;
        }
        if ( pair[0].equals( "latitude" ) ) {
          if ( pair[1] == null ) {
            logger.error( null, "propsToEps",
                "Invalid endpoint found for URI: {}. Detailed message: Latitude is null", path );
            continue ENDPOINT_LOOP; // If latitutde is null, break this loop and continue to next endpoint
          }
          try {
            latitude = Double.parseDouble( pair[1] );
          } catch ( NumberFormatException e ) {
            //If latitutde is null, break this loop and continue to next endpoint
            logger.error( null, null, "propsToEps",
                "Invalid endpoint found for URI: {}. Detailed message: NumberFormatException - {}", path,
                e.getMessage() );
            continue ENDPOINT_LOOP;
          }
        } else if ( pair[0].equals( "longitude" ) ) {
          if ( pair[1] == null ) {
            logger.error( null, "propsToEps",
                "Invalid endpoint found for URI: {}. Detailed message: Longitude is null", path );
            continue ENDPOINT_LOOP; //If longitude is null, break this loop and continue to next endpoint
          }
          try {
            longitude = Double.parseDouble( pair[1] );
          } catch ( NumberFormatException e ) {
            //If latitutde is null, break this loop and continue to next endpoint
            logger.error( null, null, "propsToEps",
                "Invalid endpoint found for URI: {}. Detailed message: NumberFormatException - {}", path,
                e.getMessage() );
            continue ENDPOINT_LOOP;
          }
        }
      }

      DME2Endpoint ep;
      double distance;
      if ( latitude == null || longitude == null ) {
        logger.error( null, "propsToEps", "Latitude or longitude null for {}", path );
        continue;
      } else {
        distance =
            DME2DistanceUtil.calculateDistanceBetween( clientLatitude, clientLongitude, latitude, longitude );
      }
      if ( valueStr.contains( "protocol=dme2jdbc" ) ) {
        //ep = new DME2JDBCEndpoint(super.getManager());
        ep = new DME2JDBCEndpoint( distance );
      } else {
        //ep = new DME2Endpoint(super.getManager());
        ep = new DME2Endpoint( distance );
      }

      ep.setHost( hostPort[0] );
      ep.setPort( Integer.parseInt( hostPort[1] ) );
      ep.setPath( path );
      ep.setServiceName( path );

      Map<String, String> map = DME2URIUtils.splitServiceURIString( path );
      ep.setSimpleName( map.get( "service" ) );
      ep.setServiceVersion( map.get( "version" ) );
      ep.setEnvContext( map.get( "envContext" ) );

      ep.setEndpointProperties( new Properties(  ) );
      for ( String pairStr : valuePairs ) {
        String[] pair = null;

        if ( pairStr.contains( "routeOffer" ) ) {
          pair = pairStr.split( "=" );
          ep.setRouteOffer( pair[1] );
        }

				/*ContextPath may contain the '=' character, so we need to handle this in a special way to prevent the valuePairs from being incorrectly split*/
        if ( pairStr.contains( "contextPath" ) ) {
          pair = pairStr.split( "=", 2 );
          ep.setContextPath( pair[1] );
          ep.setPath( pair[1] );
        } else {
          pair = pairStr.split( "=" );
        }

        if ( pair[0].equals( "latitude" ) ) {
          if ( pair[1] == null ) {
            logger.error( null, "propsToEps",
            		LogMessage.DEBUG_MESSAGE, "Invalid endpoint found for URI: {}. Detailed message: Latitude is null", path );
            break; // If latitutde is null, break this loop and continue to next endpoint
          }
          try {
            ep.setLatitude( Double.parseDouble( pair[1] ) );
          } catch ( NumberFormatException e ) {
            //If latitutde is null, break this loop and continue to next endpoint
            logger.error( null, "propsToEps", LogMessage.DEBUG_MESSAGE, "Invalid endpoint found for URI: {}. Detailed message: NumberFormatException - {}", path,
                e.getMessage() );
            break;
          }
        } else if ( pair[0].equals( "longitude" ) ) {
          if ( pair[1] == null ) {

            logger.error( null, "propsToEps", LogMessage.DEBUG_MESSAGE, 
                "Invalid endpoint found for URI: {}. Detailed message: Longitude is null", path );
            break; //If longitude is null, break this loop and continue to next endpoint
          }
          try {
            ep.setLongitude( Double.parseDouble( pair[1] ) );
          } catch ( NumberFormatException e ) {
            //If latitutde is null, break this loop and continue to next endpoint

            logger.error( null, "propsToEps", LogMessage.DEBUG_MESSAGE, 
                "Invalid endpoint found for URI: {}. Detailed message: NumberFormatException - {}", path,
                e.getMessage() );
            break;
          }
        } else if ( pair[0].equals( "lease" ) ) {
          long t = Long.parseLong( pair[1] );
          ep.setLease( t );
        } else if ( pair[0].equals( "protocol" ) ) {
          ep.setProtocol( pair[1] );
        } else if ( pair[0].equals( "DME2JDBCDatabaseName" ) ) {
          ( (DME2JDBCEndpoint) ep ).setDatabaseName( pair[1] );
        } else if ( pair[0].equals( "DME2JDBCHealthCheckUser" ) ) {
          ( (DME2JDBCEndpoint) ep ).setHealthCheckUser( pair[1] );
        } else if ( pair[0].equals( "DME2JDBCHealthCheckPassword" ) ) {
          ( (DME2JDBCEndpoint) ep ).setHealthCheckPassword( pair[1] );
        } else if ( pair[0].equals( "DME2JDBCHealthCheckDriver" ) ) {
          ( (DME2JDBCEndpoint) ep ).setHealthCheckDriver( pair[1] );
        } else {
          ep.getEndpointProperties().setProperty( pair[0], pair[1] );
        }
      }


      if ( System.currentTimeMillis() - ep.getLease() < defaultCacheStaleness ) {
        list.add( ep );
      }
    }
    return list;
  }
}
