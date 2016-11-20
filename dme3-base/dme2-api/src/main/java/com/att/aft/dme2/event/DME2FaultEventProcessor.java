package com.att.aft.dme2.event;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;


/**
 * DME2FaultEventProcessor class provides an implementation for FAULT_EVENT type of events.
 *
 * @author po704t
 */

public class DME2FaultEventProcessor implements EventProcessor {
  private static final Logger logger = LoggerFactory.getLogger( DME2FaultEventProcessor.class.getName() );
  private DME2Configuration config;

  public DME2FaultEventProcessor( DME2Configuration config ) {
    this.config = config;
  }

  /**
   * This method implements the handleEvent to handle the Fault type of events
   */
  @Override
  public void handleEvent( DME2Event event ) throws EventProcessingException {
    logger.info( null, null, "enter method - handleEvent" );
    try {
      if ( EventType.FAULT_EVENT.equals( event.getType() ) ) {
        logger.info( null, null, "inside handleEvent of FaultEvent Type" );
        addFaultEvent( event );
      } else {
        return;
      }
    } catch ( Throwable e ) {
      logger.error( null, null, "error inside Fault eventHandler ", e );
    }
    logger.info( null, null, "exit method - handleEvent" );
  }

  /**
   * This method provides the logic to handle Fault events
   */
  public void addFaultEvent( DME2Event event ) throws Exception {
    logger.info( null, null, "enter method - addFaultEvent" );
    String queueName = event.getQueueName();
    if ( !DME2Utils.isInIgnoreList( config, queueName ) ) {
      String msgId = (String) event.getMessageId();
      long elapsed = (Long) event.getElapsedTime();
      String role = (String) event.getRole();
      String partner = (String) event.getPartner();
      if ( partner == null ) {
        partner = DME2Constants.DEFAULT_NA_VALUE;
      }
      if ( role == null ) {
        role = config.getProperty( DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE );
      }
      logger.info( null, "addFaultEvent", " inside method addFaultEvent - msgid : {}", msgId );
      String protocol = (String) event.getProtocol();
      if ( protocol == null ) {
        protocol = config.getProperty( DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL );
      }
      String port = (String) event.getInterfacePort();
      DME2ServiceStats ss = DME2ServiceStatManager.getInstance( config ).getServiceStats( event.getQueueName() );
      if ( !DME2ServiceStatManager.getInstance( config ).isDisableMetrics() ) {
        logger.debug( null, "addFaultEvent", "{},{},{},{},{},{},{},{},{},{}", System.currentTimeMillis(), ss.service,
            ss.serviceVersion, role,
            port, protocol, EventType.REPLY_EVENT, msgId, elapsed, elapsed );
        logger.debug( null, "addFaultEvent",
            "MetricsCollectorFactory.getMetricsCollector containerName={},containerVersion={},containerRO={},containerEnv={},containerPlat={},containerHost={},containerPid={}",
            ss.containerName, ss.containerVersion, ss.containerRO, ss.containerEnv, ss.containerPlat, ss.containerHost,
            ss.containerPid );
//				MetricsPublisher.addFaultEvent(partner, ss, role, port, protocol, event, elapsed);
        BaseMetricsPublisher metricsPublisher =
            MetricsPublisherFactory.getInstance().getBaseMetricsPublisherHandlerInstance( config );
        metricsPublisher.publishEvent( event, ss );
      }
    }
    logger.info( null, null, "exit method - addFaultEvent" );
  }

}
