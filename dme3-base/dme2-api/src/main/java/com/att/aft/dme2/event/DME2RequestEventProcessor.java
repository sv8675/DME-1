package com.att.aft.dme2.event;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Utils;


/**
 * DME2RequestEventProcessor class provides an implementation for REQUEST_EVENT type of events.
 * @author po704t
 *
 */
public class DME2RequestEventProcessor implements EventProcessor {

	private static final Logger logger = LoggerFactory.getLogger(DME2RequestEventProcessor.class.getName());
	private DME2Configuration config;

	public DME2RequestEventProcessor(DME2Configuration config) {
		this.config = config;
	}


	/**
	 * This method implements the handleEvent to handle the Request type of events
	 */
	@Override
	public void handleEvent(DME2Event event) throws EventProcessingException {
		logger.info(null, "handleEvent", "enter method - handleEvent");
		try {
			if (EventType.REQUEST_EVENT.equals(event.getType())) {
				logger.info(null, "handleEvent", "inside handleEvent of Request Type");
				addRequestEvent(event);
			} else
				return;
		} catch (Throwable e) {
			logger.error(null, "handleEvent", "error inside Request event handler", e);
		}
		logger.info(null, "handleEvent", "exit method - handleEvent");
	}


	/**
	 * This method provides the logic to handle Request events
	 */
	public void addRequestEvent(DME2Event event) {
		String queueName = event.getQueueName();
		logger.info(null, "addRequestEvent", "enter method - addRequestEvent : {}", queueName);
		if (!DME2Utils.isInIgnoreList(config, queueName)) {
			long eventTime = (Long) event.getEventTime();
			long reqMsgSize = (Long) event.getReqMsgSize();
			String msgId = (String) event.getMessageId();
			DME2ServiceStats ss = DME2ServiceStatManager.getInstance(config).getServiceStats(event.getQueueName());
			logger.info(null, "addRequestEvent", " inside method addRequestEvent - msgid : {}", msgId);
			synchronized (DME2ServiceStatManager.getInstance(config).getStatsObjLock()) {
				logger.info(null, "addRequestEvent", " inside method addRequestEvent - add event in request map msgid : {}", msgId);
				DME2ServiceStatManager.getInstance(config).getRequestmap().put(msgId, event);
				ss.lastTouchedTime = eventTime;
				ss.requestCount++;
				if (DME2Utils.isCurrentHourMillis(eventTime)) {
					ss.currentHourRequestCount++;
				} else {
					ss.currentHourRequestCount = 0;
				}
				ss.lastRequestMsgSize = reqMsgSize;
			}

			logger.debug( null, "addRequestEvent", "DME2ServiceStats addRequestEvent {}; hashCode={}", DME2ServiceStatManager.getInstance(config).getRequestmap().size(), DME2ServiceStatManager.getInstance(config).getRequestmap().hashCode());
		}
		logger.info(null, "addRequestEvent", "exit method - addRequestEvent");
	}
}
