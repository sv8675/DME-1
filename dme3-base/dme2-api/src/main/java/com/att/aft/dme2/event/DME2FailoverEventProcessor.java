package com.att.aft.dme2.event;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;


/**
 * DME2FailoverEventProcessor class provides an implementation for FAILOVER_EVENT type of events.
 * @author po704t
 *
 */

public class DME2FailoverEventProcessor implements EventProcessor {
	private static final Logger logger = LoggerFactory.getLogger(DME2FailoverEventProcessor.class.getName());
	private DME2Configuration config;

	public DME2FailoverEventProcessor(DME2Configuration config) {
		this.config = config;
	}

/**
 * This method implements the handleEvent to handle the FAILOVER type of events
 */
	@Override
	public void handleEvent(DME2Event event) throws EventProcessingException {
		logger.info(null, "handleEvent", "enter method - handleEvent");
		try {
			if (EventType.FAILOVER_EVENT.equals(event.getType())) {
				logger.info(null, "handleEvent", "inside handleEvent of FailoverEvent Type");
				addFailoverEvent(event);
			} else
				return;
		} catch (Throwable e) {
			logger.error(null, "handleEvent", "error inside Failover", e);
		}
		logger.info(null, "handleEvent", "exit method - handleEvent");
	}

	/**
	 * This method provides the logic to handle Failover events
	 */
	public void addFailoverEvent(DME2Event event) throws Exception {
		logger.info(null, "addFailoverEvent", "enter method - addFailoverEvent");
		String queueName = event.getQueueName();
		if (!DME2Utils.isInIgnoreList(config, queueName)) {
			String msgId = (String) event.getMessageId();
			long elapsed = (Long) event.getElapsedTime();
			String role = (String) event.getRole();
			DME2ServiceStats ss = DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName());
			logger.info(null, "addFailoverEvent", "method - addFailoverEvent - msgid : {}", msgId);
			String partner = (String) event.getPartner();
			if (partner == null) {
				partner = DME2Constants.DEFAULT_NA_VALUE;
			}
			if (role == null) {
				role = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE);
			}
			if (role.equals(config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE))) {
				ss.failoverCount++;
			}
			String protocol = (String) event.getProtocol();
			if (protocol == null) {
				protocol = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL);
			}
			String port = (String) event.getInterfacePort();

			if (!DME2ServiceStatManager.getInstance(config).isDisableMetrics()) {
				logger.debug( null, "addFailoverEvent", "{},{},{},{},{},{},{},{},{},{}", System.currentTimeMillis(),  ss.service,  ss.serviceVersion,  role,
            port,  protocol,  EventType.REPLY_EVENT,  msgId,  elapsed,  elapsed);
				logger.debug( null, "addFailoverEvent", "MetricsCollectorFactory.getMetricsCollector containerName={},containerVersion={},containerRO={},containerEnv={},containerPlat={},containerHost={},containerPid={}", ss.containerName, ss.containerVersion, ss.containerRO, ss.containerEnv, ss.containerPlat, ss.containerHost, ss.containerPid);
				BaseMetricsPublisher metricsPublisher = MetricsPublisherFactory.getInstance().getBaseMetricsPublisherHandlerInstance(config);
				metricsPublisher.publishEvent(event, ss);
			}
		}
		logger.info(null, "addFailoverEvent", "exit method - addFailoverEvent");
	}

}
