package com.att.aft.dme2.event;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Utils;


/**
 * DME2InitEventProcessor class provides an implementation for INIT_EVENT type of events.
 * @author po704t
 *
 */

public class DME2InitEventProcessor implements EventProcessor {

	private static final Logger logger = LoggerFactory.getLogger(DME2InitEventProcessor.class.getName());
	private DME2Configuration config;

	public DME2InitEventProcessor(DME2Configuration config) {
		this.config = config;
	}

	/**
	 * This method implements the handleEvent to handle the Init type of events
	 */
	@Override
	public void handleEvent(DME2Event event) throws EventProcessingException {
		logger.info(null, "handleEvent", "enter method - handleEvent");
		try {
			if (EventType.INIT_EVENT.equals(event.getType())) {
				logger.info(null, "handleEvent", "inside handleEvent of Init Event Type");
				addInitEvent(event);
			} else
				return;
		} catch (Throwable e) {
			logger.error(null, "handleEvent", "error inside Init handleEvent", e);
		}
		logger.info(null, "handleEvent", "exit method - handleEvent");
	}

	/**
	 * This method provides the logic to handle Failover events
	 */
	public void addInitEvent(DME2Event event) {
		logger.info(null, "addInitEvent", "enter method - addInitEvent");
		String queueName = event.getQueueName();
		if (!DME2Utils.isInIgnoreList(config, queueName)) {
			long eventTime = System.currentTimeMillis();
			DME2ServiceStats ss = DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName());
			if (ss.lastTouchedTime < eventTime) {
				logger.info(null, "addInitEvent", "inside method - addInitEvent : Changing lastTouchedTime");
				ss.lastTouchedTime = eventTime;
			}
		}
		logger.info(null, "addInitEvent", "exit method - addInitEvent");
	}

}
