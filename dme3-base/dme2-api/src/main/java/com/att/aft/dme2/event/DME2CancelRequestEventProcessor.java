package com.att.aft.dme2.event;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Utils;


/**
 * DME2CancelRequestEventProcessor class provides an implementation for CANCEL_REQUEST_EVENT type of events.
 *
 * @author po704t
 */
public class DME2CancelRequestEventProcessor implements EventProcessor {

  private static final Logger logger = LoggerFactory.getLogger( DME2CancelRequestEventProcessor.class.getName() );
  private DME2Configuration config;

  public DME2CancelRequestEventProcessor( DME2Configuration config ) {
    this.config = config;
  }

  /**
   * This method implements the handleEvent to handle the CancelRequest type of events
   */
  @Override
  public void handleEvent( DME2Event event ) throws EventProcessingException {
    logger.info( null, null, "enter method - handleEvent" );
    try {
      if ( EventType.CANCEL_REQUEST_EVENT.equals( event.getType() ) ) {
        logger.info( null, null, "inside handleEvent of CancelRequest Type" );
        addCancelRequestEvent( event );
      } else {
        return;
      }
    } catch ( Throwable e ) {
      logger.error( null, null, "error inside CancelRequest event handler", e );
    }
    logger.info( null, null, "exit method - handleEvent" );
  }

  /**
   * This method provides the logic to handle CancelRequest events
   */
  public void addCancelRequestEvent( DME2Event event ) {
    String queueName = event.getQueueName();
    logger.info( null, "addCancelRequestEvent", "enter method - addCancelRequestEvent : {}", queueName );
    if ( !DME2Utils.isInIgnoreList( config, queueName ) ) {
      long eventTime = (Long) event.getEventTime();
      String msgId = (String) event.getMessageId();
      DME2ServiceStats ss = DME2ServiceStatManager.getInstance( config ).getServiceStats( event.getQueueName() );
      logger.info( null, "addCancelRequestEvent", " inside method addCancelRequestEvent - msgid : {}", msgId );
      synchronized ( DME2ServiceStatManager.getInstance( config ).getStatsObjLock() ) {
        logger.info( null, "addCancelRequestEvent", " inside method addCancelRequestEvent - cancel request map msgid : {}", msgId );
        DME2ServiceStatManager.getInstance( config ).getRequestmap().remove( msgId );
        ss.lastTouchedTime = eventTime;
      }
      logger.debug( null, "addCancelRequestEvent", "DME2ServiceStats addCancelRequestEvent {}; hashCode={}",
          DME2ServiceStatManager.getInstance( config ).getRequestmap().size(),
          DME2ServiceStatManager.getInstance( config ).getRequestmap().hashCode() );
    }
    logger.info( null, "addCancelRequestEvent", "exit method - addCancelRequestEvent" );
  }
}
