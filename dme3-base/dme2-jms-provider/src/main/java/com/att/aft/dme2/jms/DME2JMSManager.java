/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.CleanupScheduler;
import com.att.aft.dme2.jms.util.DME2JMSExceptionHandler;
import com.att.aft.dme2.jms.util.DME2UniformResource;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2UrlStreamHandler;
import com.att.aft.dme2.util.ErrorContext;

@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class DME2JMSManager implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = LoggerFactory.getLogger( DME2JMSManager.class.getName() );

  private volatile static DME2JMSManager instance = null;
  private final int MSG_PARSING_BUFFER = 8096;
  private final byte[] lock = new byte[0];

  private final Map<String, DME2JMSQueueConnectionFactory> qcfs = Collections
      .synchronizedMap( new HashMap<String, DME2JMSQueueConnectionFactory>() );
  private final Map<String, DME2JMSXAQueueConnectionFactory> xaqcfs = Collections
      .synchronizedMap( new HashMap<String, DME2JMSXAQueueConnectionFactory>() );
  private final Map<String, DME2JMSTopicConnectionFactory> tcfs = Collections
      .synchronizedMap( new HashMap<String, DME2JMSTopicConnectionFactory>() );

  private final Map<String, DME2JMSQueue> localQueues = Collections
      .synchronizedMap( new HashMap<String, DME2JMSQueue>() );

  private final Map<QueueSession, List<String>> tempQueueSessions = Collections
      .synchronizedMap( new HashMap<QueueSession, List<String>>() );

  private final Map<QueueConnection, List<String>> tempQueueConnections = Collections
      .synchronizedMap( new HashMap<QueueConnection, List<String>>() );

  private final Map<String, DME2JMSContinuationQueue> contQueues = Collections
      .synchronizedMap( new HashMap<String, DME2JMSContinuationQueue>() );

  /*
   * Holds a list of DME2QueueReceiver references that are associated with a
   * given Queue
   */
  private final Map<String, List<DME2JMSQueueReceiver>> tempQueueReceivers = Collections
      .synchronizedMap( new HashMap<String, List<DME2JMSQueueReceiver>>() );

  private final static int TEMP_QUEUE_IDLE_TIMEOUT = 2;

  private static final int CONSTANT_TEMPQUEUECLEANUPINTERVAL = 300000;
  private static final int CONSTANT_CONTQUEUECLEANUPINTERVAL = 240000;

  private int tempQueueCleanupInterval = CONSTANT_TEMPQUEUECLEANUPINTERVAL;
  private int contQueueCleanupInterval = CONSTANT_CONTQUEUECLEANUPINTERVAL;

  private final DME2Manager manager;

  private String userName;
  private String password;

  private static final String EXTENDEDMESSAGE = "extendedMessage";
  private static final String ROUTEOFFER = "routeOffer=";
  private static final String SLASHSLASH = "\\?";
  private static final String DME2JMSMANAGER = ";DME2JMSManager=";
  private static final String AFTDME26110 = "AFT-DME2-6110";

  private DME2Configuration config;

  protected DME2JMSManager( DME2Manager manager ) {

    logger.debug( null, "DME2JMSManager", LogMessage.METHOD_ENTER );

    this.manager = manager;
    this.config = manager.getConfig();
    this.manager.disableMetricsFilter(); // by default disable metrics
    // filter for jms
    logger.debug( null, "ctor", "manager: {}", this.manager.getName() );
    logger.debug( null, "ctor", "cleanup constants are: {}", JMSConstants.DME2_TEMP_QUEUE_CLEANUP_INTERVAL_MS );
    tempQueueCleanupInterval = config.getInt( JMSConstants.DME2_TEMP_QUEUE_CLEANUP_INTERVAL_MS, CONSTANT_TEMPQUEUECLEANUPINTERVAL );
    logger.debug( null, "ctor", "tempQueueCleanupInterval: {}", tempQueueCleanupInterval );
    contQueueCleanupInterval = config.getInt( JMSConstants.DME2_CONT_QUEUE_CLEANUP_INTERVAL_MS, CONSTANT_CONTQUEUECLEANUPINTERVAL );
    logger.debug( null, "ctor", "contQueueCleanupInterval: " + contQueueCleanupInterval );

    new CleanupScheduler( "DME2::CleanupIdleTempQueues" ).schedulePeriodically( new Runnable() {
      @Override
      public void run() {
        try {
          cleanupTemporaryQueues( TEMP_QUEUE_IDLE_TIMEOUT );
        } catch ( Throwable e ) {
          logger.warn( null, "DME2JMSManager",
              "Code=Exception.DME2JMSManager.cleanupTemporaryQueues;Exception=" + e.toString() );
        }
      }
    }, tempQueueCleanupInterval );

    new CleanupScheduler( "DME2::CleanupContinuationQueues" ).schedulePeriodically( new Runnable() {
      @Override
      public void run() {
        try {
          cleanupContinuationQueues();
        } catch ( Throwable e ) {
          logger.warn( null, "DME2JMSManager",
              "Code=Exception.DME2JMSManager.cleanupContinuationQueues;Exception={}", e.toString() );
        }
      }
    }, contQueueCleanupInterval );
    logger.debug( null, "DME2JMSManager", LogMessage.METHOD_EXIT );

  }

  public static DME2JMSManager getDefaultInstance() throws JMSException {
    return getDefaultInstance( null );
  }

  public static DME2JMSManager getDefaultInstance( Properties props ) throws JMSException {
    DME2JMSManager result = instance;
    try {
      if ( result == null ) {
        synchronized ( DME2JMSManager.class ) {
          result = instance;
          if ( result == null ) {
            List<String> defaultConfigs = new ArrayList<String>();
            defaultConfigs.add( JMSConstants.JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME );
            defaultConfigs.add( JMSConstants.DME_API_DEFAULT_CONFIG_FILE_NAME );
//            defaultConfigs.add( JMSConstants.METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME );

            DME2Configuration config =
                new DME2Configuration( JMSConstants.DEFAULT_CONFIG_MANAGER_NAME, defaultConfigs, null, props );
            DME2Manager manager = new DME2Manager( JMSConstants.DEFAULT_CONFIG_MANAGER_NAME, config );
            // disable metrics filter for JMS
            manager.disableMetricsFilter();
            instance = result = new DME2JMSManager( manager );
          }
        }
      }
    } catch ( Exception e ) {
      throw DME2JMSExceptionHandler.handleException( e, "Not Specified" );
    }

    return result;
  }

  protected void addTemporaryQueue( URI uri, DME2JMSQueue temporaryQueue ) throws JMSException {
    logger.debug( null, "addTemporaryQueue", LogMessage.METHOD_ENTER );
    try {
      logger.debug( null, "addTemporaryQueue", "Adding to localQueues, key={}", uri );

      // Add code to cache queue connection and session associated with
      // temp Queues.
      DME2JMSTemporaryQueue tempQueue = (DME2JMSTemporaryQueue) temporaryQueue;
      QueueSession session = tempQueue.getQueueSession();
      QueueConnection connection = tempQueue.getQueueConnection();

      List<String> cachedQueueSessions = this.tempQueueSessions.get( session );
      if ( cachedQueueSessions != null ) {
        if ( !cachedQueueSessions.contains( tempQueue.getURI().toString() ) ) {
          synchronized ( this.tempQueueSessions ) {
            cachedQueueSessions.add( tempQueue.getURI().toString() );
          }
        }
      } else {
        ArrayList<String> queueList = new ArrayList<String>();
        queueList.add( tempQueue.getURI().toString() );
        synchronized ( this.tempQueueSessions ) {
          this.tempQueueSessions.put( session, queueList );
        }
      }

      List<String> cachedQueueConnections = this.tempQueueConnections.get( connection );
      if ( cachedQueueConnections != null ) {
        if ( !cachedQueueConnections.contains( tempQueue.getURI().toString() ) ) {
          synchronized ( this.tempQueueConnections ) {
            cachedQueueConnections.add( tempQueue.getURI().toString() );
          }
        }
      } else {
        ArrayList<String> queueList = new ArrayList<String>();
        queueList.add( tempQueue.getURI().toString() );
        synchronized ( this.tempQueueConnections ) {
          this.tempQueueConnections.put( connection, queueList );
        }
      }
	  
      synchronized ( this.localQueues ) {
        this.localQueues.put( uri.toString(), temporaryQueue );
      }
    } catch ( Exception e ) {
      throw DME2JMSExceptionHandler.handleException( e, uri.toString() );
    }
    logger.debug( null, "addTemporaryQueue", LogMessage.METHOD_EXIT );
  }

  protected DME2JMSQueue getTemporaryQueue( URI uri ) {
    return this.localQueues.get( uri );
  }

  protected void removeTemporaryQueue( URI uri ) throws JMSException {
    long start = System.currentTimeMillis();
    logger.debug( null, "removeTemporaryQueue", JMSLogMessage.QUEUE_REMOVETMP, uri );

    try {
      logger.debug( null, "removeTemporaryQueue",
          "Code=Trace.DME2JMSManager.removeTemporaryQueue;Removing temporary queue {}", uri );
      synchronized ( this.localQueues ) {
        if ( this.localQueues.containsKey( uri.toString() ) ) {
          logger.debug( null, "removeTemporaryQueue",
              "Code=Trace.DME2JMSManager.removeTemporaryQueue; Removing temporary queue {}", uri );
          DME2JMSQueue queue = this.localQueues.remove( uri.toString() );
          this.tempQueueReceivers.remove( queue );
        }
      }

      boolean deleteQueueFromConnCache = config.getBoolean( DME2Constants.DME2_REMOVE_QUEUE_CONN_CACHE );
      if ( deleteQueueFromConnCache ) {
        ArrayList<QueueConnection> connList = new ArrayList<QueueConnection>();
        synchronized ( this.tempQueueConnections ) {
          connList.addAll( this.tempQueueConnections.keySet() );
        }
        if ( connList != null ) {
          for ( QueueConnection qc : connList ) {
            List<String> qs = this.tempQueueConnections.get( qc );
            ArrayList<String> qnameList = new ArrayList<String>();
            qnameList.addAll( qs );
            try {
              if ( qnameList.contains( uri.toURL().toString() ) ) {
                synchronized ( this.tempQueueConnections ) {
                  logger.debug( null, "removeTemporaryQueue",
                      "DME2JMSManager.removeTemporaryQueue;Removing temporary queue {} from connection cache list",
                      uri.toString() );
                  qs.remove( uri.toURL().toString() );
                }
              }
            } catch ( MalformedURLException e ) {
              // Ignore errors for uri.toURL()
              logger.debug( null, "removeTemporaryQueue", "MalformedURLException {}",
                  new ErrorContext().add( EXTENDEDMESSAGE, e.toString() ) );
            }
          }
        }
        logger.debug( null, "removeTemporaryQueue", "DME2JMSManager.removeTemporaryQueue; elapsedTime={} ms",
            ( System.currentTimeMillis() - start ) );
      }
    } catch ( Exception e ) {
      throw DME2JMSExceptionHandler.handleException( e, uri.toString() );
    }
  }

  /**
   * @param session
   * @throws JMSException
   */
  protected void closeTemporaryQueues( javax.jms.QueueSession session ) throws JMSException {
    logger.debug( null, "closeTemporaryQueues", LogMessage.METHOD_ENTER );
    logger.debug( null, "closeTemporaryQueues", JMSLogMessage.QUEUE_CLOSETMP, session );
    List<String> queueSessionList = this.tempQueueSessions.get( session );
    if ( queueSessionList != null ) {
      for ( String qs : queueSessionList ) {
        DME2JMSQueue queue = this.localQueues.get( qs );
        if ( queue instanceof DME2JMSTemporaryQueue ) {
          removeTemporaryQueue( ( (DME2JMSTemporaryQueue) queue ).getURI() );
        }
      }
    }
    synchronized ( this.tempQueueSessions ) {
      this.tempQueueSessions.remove( session );
    }
    logger.debug( null, "closeTemporaryQueues", LogMessage.METHOD_EXIT );
  }// end of closeTemporaryQueues

  /**
   * @param connection
   * @throws JMSException
   */
  protected void closeTemporaryQueues( javax.jms.QueueConnection connection ) throws JMSException {
    logger.debug( null, "closeTemporaryQueues", LogMessage.METHOD_ENTER );
    logger.debug( null, "closeTemporaryQueues", "Closing temporary queues for connection ", connection );

    List<String> queueConnsList = this.tempQueueConnections.get( connection );
    if ( queueConnsList != null ) {
      for ( String qs : queueConnsList ) {
        DME2JMSQueue queue = this.localQueues.get( qs );
        if ( queue instanceof DME2JMSTemporaryQueue ) {
          DME2JMSTemporaryQueue tmpQueue = (DME2JMSTemporaryQueue) queue;
          QueueSession queueSession = tmpQueue.getQueueSession();
          removeTemporaryQueue( ( (DME2JMSTemporaryQueue) queue ).getURI() );
          synchronized ( this.tempQueueSessions ) {
            this.tempQueueSessions.remove( queueSession );
          }
        }
      }
    }

    synchronized ( this.tempQueueConnections ) {
      this.tempQueueConnections.remove( connection );
    }
    logger.debug( null, "closeTemporaryQueues", LogMessage.METHOD_EXIT );
  }

  public DME2JMSQueue getQueue( final String newQueuePath ) throws JMSException, URISyntaxException {
    String queuePath = newQueuePath;
    logger.debug( null, "getQueue", "queuePath [{}].", queuePath );
    logger.debug( null, "getQueue", LogMessage.METHOD_ENTER, queuePath );

    DME2JMSQueue lqueue = null;

    try {
      URI uri = new URI( queuePath );
      if ( queuePath.contains( "//DME2LOCAL/" ) ) {

        // Get LRM provided RO
        String lrmRO = System.getProperty( "lrmRO" );

        // swap routeOffer in queuePath...
        DME2UniformResource uniformResource;
        try {
          uniformResource = new DME2UniformResource( config, new URL( uri.getScheme(), uri.getHost(), uri.getPort(),
              queuePath, new DME2UrlStreamHandler() ) );
        } catch ( MalformedURLException e ) {
          throw new DME2JMSException( "AFT-DME2-0607",
              new ErrorContext().add( EXTENDEDMESSAGE, e.getMessage() ).add( "URI", uri.toString() ), e );
        }

        if ( lrmRO != null ) {
          String tempPath = uri.getPath();
          String qstr = uri.getQuery();
          if ( tempPath != null ) {
            String inRouteOffer = uniformResource.getRouteOffer();
            if ( inRouteOffer != null ) {
              lrmRO = lrmRO.replaceAll( "'", "" );
              tempPath = tempPath.replace( ROUTEOFFER + inRouteOffer, ROUTEOFFER + lrmRO );
              logger
                  .debug( null, "getQueue", "Changed queuePath [{}] to [{}] due to local LRM configuration", queuePath,
                      tempPath );

              if ( qstr != null ) {
                queuePath = tempPath + "?" + qstr;
              } else {
                queuePath = tempPath;
              }
            }
          }
        }

        String uriPath = uri.toString();
        String tmpUri[] = uriPath.split( SLASHSLASH );
        String queuePathUri = tmpUri[0];

        lqueue = this.localQueues.get( queuePathUri );
        if ( lqueue == null ) {
          String queuePathScheme = queuePath.replace( "http://", "DME2://" );
          uri = new URI( queuePathScheme );
          uriPath = uri.toString();
          tmpUri = uriPath.split( SLASHSLASH );
          queuePathUri = tmpUri[0];
          lqueue = this.localQueues.get( queuePathUri );
        }

        // If we don't find queue in all above pattern, create new local
        // queue
        if ( lqueue == null ) {
          logger.debug( null, "getQueue", "queuePathUri={} {} {};localQueues={}", queuePathUri, DME2JMSMANAGER, this,
              this.localQueues );
          synchronized ( this.localQueues ) {
            lqueue = this.localQueues.get( uri.getPath() );
            if ( lqueue == null ) {
              logger
                  .debug( null, "getQueue", "uri.getPath={} {} {};localQueues={}", uri.getPath(), DME2JMSMANAGER, this,
                      this.localQueues );

              if ( uri != null ) {
                try {
                  logger.debug( null, "getQueue", "queuePath [{}] uri [{}]", queuePath, uri.toString() );

                } catch ( Exception e ) {
                  logger.debug( null, "getQueue", "Exception",
                      new ErrorContext().add( EXTENDEDMESSAGE, e.toString() ) );

                }
              }

              lqueue = new DME2JMSLocalQueue( this, uri, false );
              this.localQueues.put( queuePathUri, lqueue );
            }
          }
          // Add init event for queue
          HashMap<String, Object> props = new HashMap<String, Object>();
          props.put( DME2Constants.EVENT_TIME, System.currentTimeMillis() );
          props.put( DME2Constants.INIT_EVENT, true );
          props.put( DME2Constants.QUEUE_NAME, lqueue.getQueueName() );
          props.put( DME2Constants.DME2_INTERFACE_PROTOCOL,
              config.getProperty( DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL ) );
          this.getDME2Manager().postStatEvent( props );
        }
      } else if ( queuePath.contains( "//DME2REPLY/" ) ) {
        String uriPath = new URI( queuePath ).getPath();
        StringTokenizer tokens = new StringTokenizer( uriPath, "/" );

        if ( tokens.countTokens() > 0 ) {
          String corrId = tokens.nextToken();
          lqueue = contQueues.get( corrId );
          logger.debug( null, "getQueueFromCache", "Returning continuation queue for corrId=", corrId,
              "; Continuation queue=", lqueue );

        }
      } else {
        lqueue = new DME2JMSRemoteQueue( this, uri );
      }
    } catch ( Exception e ) {
      throw DME2JMSExceptionHandler.handleException( e, queuePath );
    }
    logger.debug( null, "getQueueFromCache", LogMessage.METHOD_EXIT );
    return lqueue;
  }

  /**
   * This method will be invoked for api stack where queue should not be created again like while reply is being
   * processed.
   *
   * @param queuePath
   * @return
   * @throws JMSException
   * @throws URISyntaxException
   */
  public DME2JMSQueue getQueueFromCache( final String newQueuePath ) throws JMSException, URISyntaxException {
    String queuePath = newQueuePath;
    logger.debug( null, "getQueueFromCache", LogMessage.METHOD_ENTER, queuePath );
    DME2JMSQueue lqueue = null;

    try {
      URI uri = new URI( queuePath );
      if ( queuePath.contains( "//DME2LOCAL/" ) ) {
        // Get LRM provided RO
        String lrmRO = System.getProperty( "lrmRO" );

        // swap routeOffer in queuePath...
        DME2UniformResource uniformResource;
        try {
          uniformResource = new DME2UniformResource( config, uri );
        } catch ( MalformedURLException e ) {
          throw new DME2JMSException( "AFT-DME2-0607",
              new ErrorContext().add( EXTENDEDMESSAGE, e.getMessage() ).add( "URI", uri.toString() ), e );
        }

        if ( lrmRO != null ) {
          String tempPath = uri.getPath();
          if ( tempPath != null ) {
            String inRouteOffer = uniformResource.getRouteOffer();
            if ( inRouteOffer != null ) {
              lrmRO = lrmRO.replaceAll( "'", "" );
              tempPath = tempPath.replace( ROUTEOFFER + inRouteOffer, ROUTEOFFER + lrmRO );
              logger.debug( null, "getQueueFromCache",
                  "DME2JMSManager.getQueueFromCache(..) - Changed queuePath [", queuePath, "] to [",
                  tempPath, "] due to local LRM configuration." );
              queuePath = tempPath;
            }
          }
        }

        String uriPath = uri.toString();
        String tmpUri[] = uriPath.split( SLASHSLASH );
        String queuePathUri = tmpUri[0];

        lqueue = this.localQueues.get( queuePathUri );
        if ( lqueue == null ) {
          String queuePathScheme = queuePath.replace( "http://", "DME2://" );
          uri = new URI( queuePathScheme );
          uriPath = uri.toString();
          tmpUri = uriPath.split( SLASHSLASH );
          queuePathUri = tmpUri[0];
          lqueue = this.localQueues.get( queuePathUri );
        }

        // If we don't find queue in all above pattern, return null
        if ( lqueue == null ) {
          logger.debug( null, "getQueueFromCache", "queuePathUri=", queuePathUri, DME2JMSMANAGER, this,
              ";localQueues=", this.localQueues );
          synchronized ( this.localQueues ) {
            lqueue = this.localQueues.get( uri.getPath() );
            if ( lqueue == null ) {
              logger.debug( null, "getQueueFromCache", "getQueueFromCache uri.getPath=", uri.getPath(),
                  DME2JMSMANAGER, this, ";localQueues=", this.localQueues );
              return lqueue;
            }
          }
        }
        return lqueue;
      } else if ( queuePath.contains( "//DME2REPLY/" ) ) {
        String uriPath = new URI( queuePath ).getPath();
        StringTokenizer tokens = new StringTokenizer( uriPath, "/" );
        if ( tokens.countTokens() > 0 ) {
          String corrId = tokens.nextToken();
          lqueue = contQueues.get( corrId );
          logger.debug( null, "getQueueFromCache", "Returning continuation queue for corrId=", corrId,
              "; Continuation queue=", lqueue );

          return lqueue;
        }
      }
    } catch ( Exception e ) {
      throw DME2JMSExceptionHandler.handleException( e, queuePath );
    }

    return lqueue;
  }

  public DME2JMSQueue getQueue( String queuePath, boolean client ) throws JMSException, URISyntaxException {
    // format: http://host:port/queue/name
    logger.debug( null, "getQueue", LogMessage.METHOD_ENTER, queuePath );

    URI uri = new URI( queuePath );
    if ( queuePath.startsWith( "http://DME2LOCAL/" ) ) {
      DME2JMSQueue lqueue = this.localQueues.get( uri.toString() );
      if ( lqueue == null ) {
        synchronized ( this.localQueues ) {
          lqueue = this.localQueues.get( uri.getPath() );
          if ( lqueue == null ) {
            lqueue = new DME2JMSLocalQueue( this, uri, false );
            lqueue.setClient( client );
            this.localQueues.put( uri.toString(), lqueue );
          }
        }
      }
      return lqueue;
    } else {
      // Check whether the URI has username,password already
      // If not present, append it
      DME2UniformResource uniformResource;
      try {
        uniformResource = new DME2UniformResource( config,
            new URL( uri.getScheme(), uri.getHost(), uri.getPort(), queuePath, new DME2UrlStreamHandler() ) );
      } catch ( MalformedURLException e ) {
        throw new DME2JMSException( "AFT-DME2-0607",
            new ErrorContext().add( EXTENDEDMESSAGE, e.getMessage() ).add( "URI", uri.toString() ), e );
      }

      StringBuffer modUriStr = new StringBuffer();
      if ( uniformResource.getUserName() == null && uniformResource.getPassword() == null ) {
        modUriStr.append( uri.toString() );
        if ( modUriStr.toString().contains( "?" ) ) {
          if ( this.userName != null && this.password != null ) {
            modUriStr.append( "&username=" + this.userName );
            modUriStr.append( "&password=" + this.password );
          }
        } else {
          if ( this.userName != null && this.password != null ) {
            modUriStr.append( "?username=" + this.userName );
            modUriStr.append( "&password=" + this.password );
          }
        }

      }
      logger.debug( null, "getQueue", "modUriString=", modUriStr );
      return new DME2JMSRemoteQueue( this, new URI( modUriStr.toString() ) );
    }
  }

  public DME2JMSMessage createMessage( Map<String, String> headers, int code, String text ) throws JMSException {
    logger.debug( null, "createMessage", LogMessage.METHOD_ENTER );

    DME2JMSTextMessage m = new DME2JMSTextMessage();
    m.setText( text );
    for ( String key : headers.keySet() ) {
      m.setStringProperty( key, headers.get( key ) );
    }
    logger.debug( null, "createMessage", LogMessage.METHOD_EXIT );
    return m;
  }

  public DME2JMSMessage createMessage( InputStream inputStream, Map<String, String> parameterMap )
      throws JMSException, URISyntaxException, UnsupportedEncodingException {
    String charset = null;
    try {
      charset = parameterMap.get( "Content-Type" );
      if ( charset == null ) {
        charset = parameterMap.get( "content-type" );
      }
      if ( charset != null ) {
        String[] toks = charset.split( ";" );
        if ( toks.length > 1 ) {
          charset = toks[1];
          String[] toks2 = toks[1].split( "=" );
          if ( toks2.length > 1 ) {
            charset = toks2[1];
          } else {
            charset = null;
          }
        } else {
          charset = null;
        }
      }
    } catch ( Exception e ) {
      DME2JMSExceptionHandler.handleException( e, "Not Specified" );
    }
    logger.debug( null, "createMessage", "charset is: {}", charset );
    return createMessage( inputStream, parameterMap, charset );
  }

  public DME2JMSMessage createMessage( InputStream inputStream, Map<String, String> parameterMap,
                                       final String newCharset )
      throws JMSException, URISyntaxException, UnsupportedEncodingException {
    logger.debug( null, "createMessage", LogMessage.METHOD_ENTER );
    String charset = newCharset;
    // create message object
    DME2JMSTextMessage m = null;
    try {
      if ( charset == null ) {
        charset = manager.getCharacterSet();
      }

      m = new DME2JMSTextMessage();

      // set properties
      for ( String key : parameterMap.keySet() ) {
        String value = parameterMap.get( key );
        if ( key == null || value == null ) {
          continue;
        }
        if ( "JMSMessageID".equals( key ) ||
            config.getProperty( DME2Constants.DME2_HEADER_PREFIX ).concat( "JMSMessageID" ).equals( key ) ) {
          //if ("JMSMessageID".equals(key) ) {
          m.setJMSMessageID( value );
        } else if ( "JMSCorrelationID".equals( key ) ||
            config.getProperty( DME2Constants.DME2_HEADER_PREFIX ).concat( "JMSCorrelationID" ).equals( key ) ) {
          //} else if ("JMSCorrelationID".equals(key)) {
          m.setJMSCorrelationID( value );
        } else if ( "JMSType".equals( key ) ) {
          m.setJMSType( value );
        } else if ( "JMSDeliveryMode".equals( key ) ) {
          m.setJMSDeliveryMode( Integer.parseInt( value ) );
        } else if ( "JMSExpiration".equals( key ) ) {
          m.setJMSExpiration( Long.parseLong( value ) );
        } else if ( "JMSPriority".equals( key ) ) {
          m.setJMSPriority( Integer.parseInt( value ) );
        } else if ( "JMSTimestamp".equals( key ) ) {
          m.setJMSTimestamp( Long.parseLong( value ) );
        } else if ( "JMSDestination".equals( key ) ||
            config.getProperty( DME2Constants.DME2_HEADER_PREFIX ).concat( "JMSDestination" ).equals( key ) ) {
          m.setJMSDestination( getQueue( value ) );
        } else if ( "JMSReplyTo".equals( key ) ) {
          m.setJMSReplyTo( getQueue( value ) );
        } else if ( "JMSRedelivered".equals( key ) ) {
          m.setJMSRedelivered( Boolean.parseBoolean( value ) );
        } else {
          m.setStringProperty( key, value );
        }
      }

      GZIPInputStream gis = null;
      if ( parameterMap != null
          && parameterMap.get( config.getBoolean( DME2Constants.AFT_DME2_CONTENT_ENCODING_KEY ) ) != null
          && config.getBoolean( DME2Constants.AFT_DME2_ALLOW_COMPRESS_ENCODING ) ) {
        if ( parameterMap.get( config.getProperty( DME2Constants.AFT_DME2_CONTENT_ENCODING_KEY ) )
            .equalsIgnoreCase( config.getProperty( DME2Constants.AFT_DME2_COMPRESS_ENCODING ) ) ) {
          try {
            gis = new GZIPInputStream( inputStream );
          } catch ( Exception e ) {
            JMSException j = new JMSException( "UNABLE TO READ COMPRESSED INPUT JMS MESSAGE" );
            j.initCause( e );
            throw new DME2JMSException( "AFT-DME2-5501",
                new ErrorContext().add( "manager", this.manager.getName() ), j );
          }
        }
      }

      BufferedReader reader = null;
      try {
        if ( charset != null && !( charset.equals( "null" ) ) ) {
          logger.debug( null, "createMessage(InputStream,Map<String,String>,String)", "charset is: {}", charset );
          if ( gis != null ) {
            reader = new BufferedReader( new InputStreamReader( gis, charset ) );
          } else {
            reader = new BufferedReader( new InputStreamReader( inputStream, charset ) );
          }
        } else {
          if ( gis == null ) {
            reader = new BufferedReader( new InputStreamReader( inputStream ) );
          } else {
            reader = new BufferedReader( new InputStreamReader( gis ) );
          }
        }

        final char[] buffer = new char[MSG_PARSING_BUFFER];
        StringBuilder inputText = new StringBuilder( MSG_PARSING_BUFFER );

        int n = -1;
        while ( ( n = reader.read( buffer ) ) != -1 ) {
          inputText.append( buffer, 0, n );
        }
        m.setText( inputText.toString() );
      } catch ( IOException e ) {
        JMSException j = new JMSException( "UNABLE TO READ INPUT JMS MESSAGE" );
        j.initCause( e );
        throw new DME2JMSException( "AFT-DME2-5501", new ErrorContext().add( "manager", this.manager.getName() ),
            j );
      }
    } catch ( Exception e ) {
      DME2JMSExceptionHandler.handleException( e, "Not Specified" );
    }
    logger.debug( null, "createMessage", LogMessage.METHOD_EXIT );
    return m;
  }

  public void streamMessage( DME2JMSMessage m, PrintWriter writer ) throws JMSException, IOException {
    logger.debug( null, "streamMessage", LogMessage.METHOD_ENTER );
    DME2JMSTextMessage m1 = (DME2JMSTextMessage) m;
    logger.debug( null, "streamMessage", JMSLogMessage.CONTENTS, m1.getText() );
    writer.print( m1.getText() );
    logger.debug( null, "streamMessage", LogMessage.METHOD_EXIT );
    return;
  }

  public void streamMessage( DME2JMSMessage m, OutputStream outputStream ) throws JMSException, IOException {
    logger.debug( null, "streamMessage", LogMessage.METHOD_ENTER );
    DME2JMSTextMessage m1 = (DME2JMSTextMessage) m;
    logger.debug( null, "streamMessage", JMSLogMessage.CONTENTS, m1.getText() );
    String charset = m.getStringProperty( "com.att.aft.dme2.charset" );
    if ( charset == null ) {
      charset = m.getStringProperty( "com.att.aft.dme2.jms.charset" );
      if ( charset == null ) {
        charset = manager.getCharacterSet();
      }
    }
    if ( charset != null && ( charset.equals( "null" ) ) ) {
      charset = null;
    }

    if ( charset == null ) {
      outputStream.write( m1.getText().getBytes() );
    } else {
      OutputStreamWriter writer = new OutputStreamWriter( outputStream, charset );
      writer.append( m1.getText() );
      writer.close();
    }
    logger.debug( null, "streamMessage", LogMessage.METHOD_EXIT );
    return;
  }

  protected synchronized DME2JMSQueueConnectionFactory getQCF( String path ) throws URISyntaxException {
    logger.debug( null, "getQCF", LogMessage.METHOD_ENTER );
    DME2JMSQueueConnectionFactory factory = qcfs.get( path );
    if ( factory == null ) {
      factory = new DME2JMSQueueConnectionFactory( this, path );
      qcfs.put( path, factory );
    }
    logger.debug( null, "getQCF", LogMessage.METHOD_EXIT );
    return factory;
  }

  protected synchronized DME2JMSXAQueueConnectionFactory getXAQCF( String path ) throws URISyntaxException {
    logger.debug( null, "getXAQCF", LogMessage.METHOD_ENTER );
    DME2JMSXAQueueConnectionFactory factory = xaqcfs.get( path );
    if ( factory == null ) {
      factory = new DME2JMSXAQueueConnectionFactory( this, path );
      qcfs.put( path, factory );
    }
    logger.debug( null, "getXAQCF", LogMessage.METHOD_EXIT );
    return factory;
  }

  protected synchronized DME2JMSTopicConnectionFactory getDummyTCF( String path ) throws URISyntaxException {
    logger.debug( null, "getDummyTCF", LogMessage.METHOD_ENTER );
    DME2JMSTopicConnectionFactory factory = tcfs.get( path );
    if ( factory == null ) {
      factory = new DME2JMSTopicConnectionFactory();
      tcfs.put( path, factory );
    }
    logger.debug( null, "getDummyTCF", LogMessage.METHOD_EXIT );
    return factory;
  }

  public DME2JMSMessage createErrorMessage( Throwable e, Map<String, String> responseHeaders ) throws JMSException {
    logger.debug( null, "createErrorMessage", LogMessage.METHOD_ENTER );
    JMSException j = new DME2JMSException( e.getMessage() );
    j.initCause( e );

    DME2JMSErrorMessage m = null;

    if ( j.getMessage() != null && j.getMessage().contains( DME2Constants.DME2_ALL_EP_FAILED_MSGCODE ) ) {
      // create a "fast fail" message. This will indicate to the
      // queue.get() method to return
      // a null but do it immediately. Basically shortcut the client
      // timeout because we know
      // that no message will be returned in this scenario.
      m = new DME2JMSErrorMessage( j, true );
      String traceInfo = responseHeaders.get( "AFT_DME2_REQ_TRACE_INFO" );
      if ( traceInfo != null ) {
        m.setStringProperty( "AFT_DME2_REQ_TRACE_INFO", traceInfo );
      }
    } else {
      m = new DME2JMSErrorMessage( j );
    }
    String c = responseHeaders.get( "JMSCorrelationID" );
    if ( c != null ) {
      m.setJMSCorrelationID( c );
    } else {
      String msgId = responseHeaders.get( "JMSMessageID" );
      if ( msgId == null ) {
        msgId = responseHeaders.get( config.getProperty( DME2Constants.DME2_HEADER_PREFIX ) + "JMSMessageID" );
      }
      m.setJMSCorrelationID( msgId );
    }
    logger.debug( null, "createErrorMessage", LogMessage.METHOD_EXIT );
    return m;

  }

  protected void cleanupTemporaryQueues( int policy ) {
    logger.debug( null, "cleanupTemporaryQueues", LogMessage.METHOD_ENTER );
    logger.debug( null, "cleanupTemporaryQueues",
        "DME2JMSManager.cleanupTemporaryQueues entering; localQueues size={}", this.localQueues.size() );

    List<String> qList = new ArrayList<String>();
    synchronized ( this.localQueues ) {
      qList.addAll( this.localQueues.keySet() );
    }
    java.util.ArrayList<DME2JMSTemporaryQueue> closeableQs = new java.util.ArrayList<DME2JMSTemporaryQueue>();
	java.util.ArrayList<DME2JMSLocalQueue> localCloseableQs = new java.util.ArrayList<DME2JMSLocalQueue>();

    for ( String qname : qList ) {
      DME2JMSQueue queue = this.localQueues.get( qname );
      logger.debug( null, "cleanupTemporaryQueues", "DME2JMSManager.cleanupTemporaryQueues; qname=", qname,
          ";queueObj=", queue );
      if ( queue instanceof DME2JMSTemporaryQueue ) {
        DME2JMSTemporaryQueue tempQueue = (DME2JMSTemporaryQueue) queue;
        javax.jms.QueueConnection qConnection = tempQueue.getQueueConnection();
        javax.jms.QueueSession qSession = tempQueue.getQueueSession();
        logger.debug( null, "cleanupTemporaryQueues",
            "DME2JMSManager.cleanupTemporaryQueues; qname={};qsession={};qConn={}", qname,
            qSession, qConnection );

        if ( qConnection == null || qSession == null ) {
          closeableQs.add( tempQueue );
        } else {
          logger.debug( null, "cleanupTemporaryQueues", "DME2JMSManager.cleanupTemporaryQueues; qname={} isClosed()" );

          switch ( policy ) {
            case TEMP_QUEUE_IDLE_TIMEOUT: {
              if ( tempQueue.isClosed() ) {
                closeableQs.add( tempQueue );
              }
              break;
            }
          }// end of switch
        }
      } else if ( queue instanceof DME2JMSLocalQueue ) {
        try {
          DME2JMSLocalQueue lqueue = (DME2JMSLocalQueue) this.localQueues.get( qname );

          //if this is a local queue created for restful calls then clean them up if they have expired
          if ( qname.contains( "DME2LOCAL" ) &&
              ( ( !qname.contains( "version=" ) && !qname.contains( "routeoffer=" ) && !qname.contains( "service=" ) ) ) &&
              ( lqueue.isClient() ) ) {
            // If create time had exceeded expiry time, remove it from cache.
            if ( ( lqueue.getCreateTime() +
                this.manager.getConfig().getLong( DME2Constants.DME2_LOCAL_CLIENT_QUEUE_EXPIRES_AFTER ) ) <= System.currentTimeMillis() ) {
              try {
                logger.debug( null, "cleanupTemporaryQueues",
                    "Adding LocalQueue to be removed with createTime={}; QueueName={}",
                    lqueue.getCreateTime(), lqueue.getQueueName() );
                localCloseableQs.add( lqueue );
              } catch ( Exception e ) {
                //ignore error in getting getQueueName for debug
                logger.debug( null, "cleanupTemporaryQueue", "Exception {}",
                    new ErrorContext().add( EXTENDEDMESSAGE, e.toString() ) );
              }

              for ( DME2JMSLocalQueue lq : localCloseableQs ) {
                logger.debug( null, "cleanupTemporaryQueue", "Removing local queue with qname {}", lq.getQueueName() );
                synchronized ( localQueues ) {
                  localQueues.remove( lq );
                }
              }
            }
          }
        }catch( JMSException e){
          logger.error( null, "cleanupTemporaryQueue", "AFT-DME2-6302 {}", new ErrorContext().add( "Queue", qname ),
              e );
        }
      } // end of if queue instance of DME2tmpq
    } // end of for loop.
    logger
        .debug( null, "cleanupTemporaryQueues", "Removing from localQueues, closeableQs size={}", closeableQs.size() );

    for ( DME2JMSTemporaryQueue q : closeableQs ) {
      logger.debug( null, "cleanupTemporaryQueues", "Deleting temporary queue {}", q );
      QueueSession session = q.getQueueSession();
      QueueConnection connection = q.getQueueConnection();

      try {
        q.delete();

        synchronized ( this.localQueues ) {
          this.localQueues.remove( q.getURI().toString() );
          this.tempQueueReceivers.remove( q.getQueueName() );
        }
        synchronized ( this.tempQueueSessions ) {
          this.tempQueueSessions.remove( session );
        }
        synchronized ( this.tempQueueConnections ) {
          this.tempQueueConnections.remove( connection );
        }
        logger.debug( null, "cleanupTemporaryQueues",
            "DME2JMSManager.cleanupTemporaryQueues key={} removed temporary queue successfully", q.getURI() );

      } catch ( JMSException e ) {
        logger.debug( null, "cleanupTemporaryQueues", "AFT-DME2-6302 {}",
            new ErrorContext().add( "Queue", q.getURI().toString() ), e );
      }

    }

    long start = System.currentTimeMillis();
    ArrayList<QueueSession> sessionList = new ArrayList<QueueSession>();
    synchronized ( this.tempQueueSessions ) {
      sessionList.addAll( this.tempQueueSessions.keySet() );
    }

    int count = 0;
    if ( sessionList != null ) {
      for ( QueueSession qs : sessionList ) {
        List<String> qlist = this.tempQueueSessions.get( qs );
        ArrayList<String> qnameList = new ArrayList<String>();
        qnameList.addAll( qlist );
        for ( String qname : qnameList ) {
          if ( this.localQueues.get( qname ) == null ) {
            // this means session is not closed, but queue got
            // deleted.
            synchronized ( this.tempQueueSessions ) {
              logger.debug( null, "cleanupTemporaryQueues", "deleting queue {}", qname );
              qlist.remove( qname );
              count++;
            }
          }
        }
      }
    }
    logger.debug( null, "cleanupTemporaryQueues", "deleted cached queue sessions;count={};elapsedTime={} ms", count, ( System.currentTimeMillis() - start ) );

    start = System.currentTimeMillis();
    ArrayList<QueueConnection> connList = new ArrayList<QueueConnection>();
    synchronized ( this.tempQueueConnections ) {
      connList.addAll( this.tempQueueConnections.keySet() );
    }
    count = 0;
    if ( connList != null ) {
      for ( QueueConnection qc : connList ) {
        List<String> qs = this.tempQueueConnections.get( qc );
        ArrayList<String> qnameList = new ArrayList<String>();
        qnameList.addAll( qs );
        for ( String qname : qnameList ) {
          if ( this.localQueues.get( qname ) == null ) {
            // this means connection is not closed, but queue got
            // deleted.
            synchronized ( this.tempQueueConnections ) {
              logger.debug( null, "cleanupTemporaryQueues", "deleting queue {}", qname );
              qs.remove( qname );
              count++;
            }
          }
        }
      }
    }
    logger.debug( null, "cleanupTemporaryQueues", "deleted cached connection queues;count={};elapsedTime={} ms", count,
        ( System.currentTimeMillis() - start ) );
    logger.debug( null, "cleanupTemporaryQueues", LogMessage.METHOD_EXIT );
  }

  /**
   * Clean the continuation queues saved
   */
  protected void cleanupContinuationQueues() {
    logger.debug( null, "cleanupContinuationQueues", LogMessage.METHOD_ENTER );

    List<String> qList = new ArrayList<String>();
    qList.addAll( this.contQueues.keySet() );
    java.util.ArrayList<String> removableQs = new java.util.ArrayList<String>();
    for ( String msgId : qList ) {
      DME2JMSQueue queue = contQueues.get( msgId );
      if ( queue instanceof DME2JMSContinuationQueue ) {
        DME2JMSContinuationQueue tempQueue = (DME2JMSContinuationQueue) queue;
        // If create time had exceeded expiry time, remove it from
        // cache.
        if ( ( tempQueue.getCreateTime() + config.getLong( "AFT_DME2_CONT_QUEUE_EXPIRES_AFTER" ) <= System
            .currentTimeMillis() ) ) {
          try {
            logger.debug( null, "cleanupContinuationQueues",
                "Adding ContQueue to be removed with createTime={}; QueueName={}; msgId={}", tempQueue.getCreateTime(), tempQueue.getQueueName(), msgId );
          } catch ( Exception e ) {
            // ignore error in getting getQueueName for debug
            logger.debug( null, "cleanupContinuationQueues", "Exception {}",
                new ErrorContext().add( EXTENDEDMESSAGE, e.toString() ) );
          }
          removableQs.add( msgId );
        }
      } // end of if queue instance of DME2contqueue
    } // end of for loop.
    for ( String msgid : removableQs ) {
      logger.debug( null, "cleanupContinuationQueues", "Deleting continuation queue for message id {}", msgid );
      synchronized ( contQueues ) {
        contQueues.remove( msgid );
      }
    }
    logger.debug( null, "cleanupContinuationQueues", LogMessage.METHOD_EXIT );
  }

  public boolean removeContinuation( String msgId ) {
    synchronized ( contQueues ) {
      contQueues.remove( msgId );
      return true;
    }
  }

  public void addContinuation( String msgId, DME2JMSContinuationQueue contQueue ) {
    contQueues.put( msgId, contQueue );
  }

  public DME2Manager getDME2Manager() {
    return manager;
  }

  public void setClientCredentials( String username, String password ) {
    this.userName = username;
    this.password = password;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword( String password ) {
    this.password = password;
  }

  public void addQueueReceiverToMap( Queue queue, DME2JMSQueueReceiver receiver ) {
    try {
      List<DME2JMSQueueReceiver> receivers = this.tempQueueReceivers.get( queue.getQueueName() );
      synchronized ( lock ) {
        if ( receivers == null ) {
          receivers = new ArrayList<DME2JMSQueueReceiver>();
          receivers.add( receiver );
          this.tempQueueReceivers.put( queue.getQueueName(), receivers );
        } else {
          receivers.add( receiver );
        }
      }

    } catch ( JMSException e ) {
      logger.warn( null, "addQueueReceiverToMap", AFTDME26110 + " {}", new ErrorContext(), e );
    }
  }

  public void removeQueueReceiverFromMap( Queue queue, DME2JMSQueueReceiver receiver ) {
    try {
      List<DME2JMSQueueReceiver> receivers = tempQueueReceivers.get( queue.getQueueName() );
      synchronized ( lock ) {
        if ( receivers != null && !receivers.isEmpty() ) {
          if ( receivers.contains( receiver ) ) {
            receivers.remove( receiver );
            if ( receivers.size() == 0 ) {
              tempQueueReceivers.remove( queue.getQueueName() );
            }
          }
        }
      }
    } catch ( JMSException e ) {
      logger.warn( null, "removeQueueReceiverFromMap", AFTDME26110 + " {}", new ErrorContext(), e );
    }
  }

  public boolean containsQueueReceivers( Queue queue ) {
    try {
      List<DME2JMSQueueReceiver> receivers = tempQueueReceivers.get( queue.getQueueName() );
      if ( receivers != null && receivers.size() > 0 ) {
        return true;
      }
    } catch ( JMSException e ) {
      logger.warn( null, "containsQueueReceivers", AFTDME26110 + " {}", new ErrorContext(), e );
    }

    return false;
  }

  public List<DME2JMSQueueReceiver> getQueueReceivers( Queue queue ) {
    List<DME2JMSQueueReceiver> receivers = null;

    try {
      receivers = this.tempQueueReceivers.get( queue.getQueueName() );
    } catch ( JMSException e ) {
      logger.warn( null, "containsQueueReceivers", AFTDME26110 + " {}", new ErrorContext(), e );
    }

    return receivers;
  }

  public Map<String, DME2JMSQueue> getLocalQueues() {
    Map<String, DME2JMSQueue> readOnlyMap = Collections.unmodifiableMap( localQueues );
    return localQueues;
  }

  int getMSG_PARSING_BUFFER() {
    return MSG_PARSING_BUFFER;
  }

}