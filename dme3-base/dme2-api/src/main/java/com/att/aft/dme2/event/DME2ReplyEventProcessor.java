package com.att.aft.dme2.event;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;


/**
 * DME2ReplyEventProcessor class provides an implementation for REPLY_EVENT type of events.
 *
 * @author po704t
 */
public class DME2ReplyEventProcessor implements EventProcessor {

  private static final Logger logger = LoggerFactory.getLogger( DME2ReplyEventProcessor.class.getName() );
  private DME2Configuration config;

  public DME2ReplyEventProcessor( DME2Configuration config ) {
    this.config = config;
  }

  /**
   * This method implements the handleEvent to handle the Reply type of events
   */
  @Override
  public void handleEvent( DME2Event event ) throws EventProcessingException {
    logger.info( null, null, "enter method - handleEvent" );
    try {
      if ( EventType.REPLY_EVENT.equals( event.getType() ) ) {
        logger.info( null, null, "inside handleEvent of ReplyEvent Type" );
        addReplyEvent( event );
      } else {
        return;
      }
    } catch ( Throwable e ) {
      logger.error( null, null, "error inside ReplyEvent Handler", e );
    }
    logger.info( null, null, "exit method - handleEvent" );
  }

  /**
   * This method provides the logic to handle Reply events
   */
  public void addReplyEvent( DME2Event event ) throws Exception {
    String queueName = event.getQueueName();
    logger.debug( null, "addReplyEvent", "enter method - addReplyEvent : {}", queueName );
    logger.debug( null, "addReplyEvent", "enter method - addReplyEvent : DME2ServiceStatManager.getInstance(config).getRequestmap() :" + DME2ServiceStatManager.getInstance(config).getRequestmap());
    
    if ( !DME2Utils.isInIgnoreList( config, queueName ) ) {
      long eventTime = (Long) event.getEventTime();
      String msgId = (String) event.getMessageId();
      long elapsed = (Long) event.getElapsedTime();
      long replyMsgSize = (Long) event.getReplyMsgSize();
      String partner = (String) event.getPartner();
      String role = (String) event.getRole();
      if ( partner == null ) {
        partner = DME2Constants.DEFAULT_NA_VALUE;
      }
      if ( role == null ) {
        role = config.getProperty( DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE );
      }
      String protocol = (String) event.getProtocol();
      if ( protocol == null ) {
        protocol = config.getProperty( DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL );
      }
      String port = (String) event.getInterfacePort();
      DME2ServiceStats ss = DME2ServiceStatManager.getInstance( config ).getServiceStats( event.getQueueName() );
      logger.debug( null, "addReplyEvent", " inside method addReplyEvent - msgid : {}", msgId );
      if ( !DME2ServiceStatManager.getInstance( config ).isDisableMetrics() ) {
        logger.debug( null, "addReplyEvent", "{},{},{},{},{},{},{},{},{},{}", System.currentTimeMillis(), ss.service,
            ss.serviceVersion, role,
            port, protocol, EventType.REPLY_EVENT, msgId, elapsed, elapsed );
        logger.debug( null, "addReplyEvent",
            "MetricsCollectorFactory.getMetricsCollector containerName={},containerVersion={},containerRO={},containerEnv={},containerPlat={},containerHost={},containerPid={}",
            ss.containerName, ss.containerVersion, ss.containerRO, ss.containerEnv, ss.containerPlat, ss.containerHost,
            ss.containerPid );
//				MetricsPublisher.addResponseEvent(partner, ss, role, port, protocol, event, elapsed);
        BaseMetricsPublisher metricsPublisher =
            MetricsPublisherFactory.getInstance().getBaseMetricsPublisherHandlerInstance( config );
        metricsPublisher.publishEvent( event, ss );
      }
      DME2Event re;
      synchronized ( DME2ServiceStatManager.getInstance( config ).getStatsObjLock() ) {
        logger.debug( null, null,
            " inside method addReplyEvent - removing message from requestmap msgid : " + msgId );
        re = DME2ServiceStatManager.getInstance( config ).getRequestmap().remove( msgId );
      }

      logger.debug( null, "addReplyEvent", "DME2ServiceStats.addReplyEvent {}; hashCode {};props=",
          DME2ServiceStatManager.getInstance( config ).getRequestmap().size(),
          DME2ServiceStatManager.getInstance( config ).getRequestmap().hashCode(), event );

      if ( re != null && role.equals( config.getProperty( DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE ) ) ) {
        logger.debug( null, null, "inside method - addReplyEvent : DME2_INTERFACE_SERVER_ROLE " );
        ss.setLastRequestElapsed( elapsed );
        ss.replyCount++;
        ss.setLastTouchedTime( eventTime );
        ss.setLastReplyMsgSize( replyMsgSize );

        if ( ss.totalElapsed == -1 ) {
          ss.totalElapsed = elapsed;
        } else {
          ss.totalElapsed = ss.totalElapsed + elapsed;
        }
        if ( ss.minElapsed == -1 ) {
          ss.minElapsed = elapsed;
        }
        if ( ss.maxElapsed == -1 ) {
          ss.maxElapsed = elapsed;
        }
        if ( elapsed > ss.maxElapsed ) {
          ss.maxElapsed = elapsed;
        }
        if ( elapsed < ss.minElapsed ) {
          ss.minElapsed = elapsed;
        }
      }
    }
    logger.debug( null, null, "exit method - addReplyEvent" );
  }

}
