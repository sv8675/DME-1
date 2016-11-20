package com.att.aft.dme2.iterator.metrics;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.event.DME2CancelRequestEventProcessor;
import com.att.aft.dme2.event.DME2Event;
import com.att.aft.dme2.event.DME2EventManager;
import com.att.aft.dme2.event.DME2FaultEventProcessor;
import com.att.aft.dme2.event.DME2ReplyEventProcessor;
import com.att.aft.dme2.event.DME2RequestEventProcessor;
import com.att.aft.dme2.event.EventType;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.IteratorCreatingAttributes;
import com.att.aft.dme2.iterator.domain.IteratorMetricsEvent;
import com.att.aft.dme2.iterator.domain.IteratorMetricsEventHolder;
import com.att.aft.dme2.iterator.exception.IteratorException;
import com.att.aft.dme2.iterator.exception.IteratorException.IteratorErrorCatalogue;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ScheduledExecution;

public class DefaultEndpointIteratorMetricsCollection extends AbstractEndpointIteratorMetricsCollection {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultEndpointIteratorMetricsCollection.class.getName());
	private DME2Configuration config;
	private static String DEFAULT_EVENT_ROLE;
	private static String DEFAULT_EVENT_PROTOCOL;
	private static int MAX_THREAD_COUNT_TIMEOUT_CHECKER;
	private static long EVENT_TIMEOUT_TOTAL_WAITING_MS;
	private static long EVENT_CHECKER_SCHEDULER_DELAY_MS;
	private static Boolean DISABLE_METRICS;
	private DME2EventManager eventManager;
	private ScheduledFuture<?> eventTimeoutCheckerHandle = null;
	// private boolean bEndInvoked = false;
	private String uniqueId = null;

	private long startTime = System.currentTimeMillis();
	private Map<String, IteratorMetricsEventHolder> startEventMap = new ConcurrentHashMap<String, IteratorMetricsEventHolder>();

	public DefaultEndpointIteratorMetricsCollection(DME2Manager manager,
			List<DME2EndpointReference> endpointReferenceList, String queryParamMinActiveEndPoint)
					throws DME2Exception {
		config = manager.getConfig();
		if (null != config) {
			throw new IteratorException(IteratorErrorCatalogue.ITERATOR_004);
		}
		IteratorCreatingAttributes iteratorCreatingAttributes = new IteratorCreatingAttributes();
		init(iteratorCreatingAttributes);

	}

	public DefaultEndpointIteratorMetricsCollection(final IteratorCreatingAttributes iteratorCreatingAttributes) {
		if (iteratorCreatingAttributes.getConfig() == null) {
			throw new IteratorException(IteratorErrorCatalogue.ITERATOR_004);
		}

		config = iteratorCreatingAttributes.getConfig();
		init(iteratorCreatingAttributes);
	}

	private void init(final IteratorCreatingAttributes iteratorCreatingAttributes) {

		// check only once
		if ( DISABLE_METRICS == null ) {
			DISABLE_METRICS = config.getBoolean( DME2Constants.AFT_DME2_DISABLE_METRICS );
		}

		if ( !DISABLE_METRICS ) {
			DEFAULT_EVENT_ROLE = config.getProperty( DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE );
			DEFAULT_EVENT_PROTOCOL = config.getProperty( DME2Constants.AFT_DME2_INTERFACE_HTTP_PROTOCOL );
			MAX_THREAD_COUNT_TIMEOUT_CHECKER = config.getInt( DME2Constants.Iterator.MAX_THREAD_COUNT_TIMEOUT_CHECKER );
			EVENT_TIMEOUT_TOTAL_WAITING_MS = config.getLong( DME2Constants.Iterator.EVENT_TIMEOUT_TOTAL_WAITING_MS );
			EVENT_CHECKER_SCHEDULER_DELAY_MS = config.getLong( DME2Constants.Iterator.EVENT_CHECKER_SCHEDULER_DELAY_MS );

			eventManager = DME2EventManager.getInstance( config );

			registerEventHandlers( iteratorCreatingAttributes );
			generateUniqueTransactionReference();
			scheduleEventTimeoutChecker();
		}
	}

	private IteratorMetricsEventHolder getIteratorMetricsEventHolder(final IteratorMetricsEvent iteratorMetricsEvent) {
		IteratorMetricsEventHolder iteratorMetricsEventHolder = new IteratorMetricsEventHolder();
		iteratorMetricsEventHolder.setIteratorMetricsEvent(iteratorMetricsEvent);
		return iteratorMetricsEventHolder;
	}

	@Override
	public void start(final IteratorMetricsEvent iteratorMetricsEvent) {
		if ( !DISABLE_METRICS ) {
			IteratorMetricsEventHolder iteratorMetricsEventHolder = getIteratorMetricsEventHolder( iteratorMetricsEvent );
			try {
				verify( iteratorMetricsEventHolder );
				assignStartEventDefaults( iteratorMetricsEventHolder );

				// if start invoked in loop without calling end, then cancel all
				// existing events
				// if(!bEndInvoked){
				cancelActiveEvents();
				// }
				// to keep track of active events
				startEventMap.put( iteratorMetricsEventHolder.getIteratorMetricsEvent().getServiceUri(),
						iteratorMetricsEventHolder );

				// reset/start time; used during raising event when the elapsed time
				// is not set explicitly
				startTime = System.currentTimeMillis();
				createEvent( iteratorMetricsEventHolder );
			} catch ( Exception e ) {
				LOGGER.warn( null, "start", "error while creating event: ", iteratorMetricsEvent );
				throw e;
			}
		}
	}

	@Override
	public void endSuccess(final IteratorMetricsEvent iteratorMetricsEvent) {
		if ( !DISABLE_METRICS ) {
			IteratorMetricsEventHolder iteratorMetricsEventHolder = getIteratorMetricsEventHolder( iteratorMetricsEvent );
			verify( iteratorMetricsEventHolder );
			assignEndSuccesEventDefaults( iteratorMetricsEventHolder );
			end( iteratorMetricsEventHolder );
		}
	}

	@Override
	public void endFailure(final IteratorMetricsEvent iteratorMetricsEvent) {
		if ( !DISABLE_METRICS ) {
			IteratorMetricsEventHolder iteratorMetricsEventHolder = getIteratorMetricsEventHolder( iteratorMetricsEvent );
			verify( iteratorMetricsEventHolder );
			assignEndFailureEventDefaults( iteratorMetricsEventHolder );
			end( iteratorMetricsEventHolder );
		}
	}

	private void assignStartEventDefaults(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		iteratorMetricsEventHolder.setEventType(EventType.REQUEST_EVENT);
		assignEventDefaults(iteratorMetricsEventHolder);
	}

	private void assignEndSuccesEventDefaults(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		iteratorMetricsEventHolder.setEventType(EventType.REPLY_EVENT);
		assignEventDefaults(iteratorMetricsEventHolder);
	}

	private void assignEndFailureEventDefaults(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		iteratorMetricsEventHolder.setEventType(EventType.FAULT_EVENT);
		assignEventDefaults(iteratorMetricsEventHolder);
	}

	private void assignTimeoutEventDefaults(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		iteratorMetricsEventHolder.setEventType(EventType.TIMEOUT_EVENT);
		iteratorMetricsEventHolder.setElapsedTime(
				System.currentTimeMillis() - iteratorMetricsEventHolder.getIteratorMetricsEvent().getEventTime());
		iteratorMetricsEventHolder.getIteratorMetricsEvent().setEventTime(System.currentTimeMillis());
	}

	private void assignEventDefaults(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		if (iteratorMetricsEventHolder.getIteratorMetricsEvent().getClientIp() == null
				|| iteratorMetricsEventHolder.getIteratorMetricsEvent().getClientIp().isEmpty()) {
			iteratorMetricsEventHolder.getIteratorMetricsEvent().setClientIp(getIpAddr());
		}
		if (iteratorMetricsEventHolder.getIteratorMetricsEvent().getEventTime() == -1L) {
			iteratorMetricsEventHolder.getIteratorMetricsEvent().setEventTime(System.currentTimeMillis());
		}
		iteratorMetricsEventHolder.setElapsedTime(System.currentTimeMillis() - startTime);
		if (iteratorMetricsEventHolder.getIteratorMetricsEvent().getProtocol() == null
				|| iteratorMetricsEventHolder.getIteratorMetricsEvent().getProtocol().isEmpty()) {
			iteratorMetricsEventHolder.getIteratorMetricsEvent().setProtocol(DEFAULT_EVENT_PROTOCOL);
		}
		if (iteratorMetricsEventHolder.getIteratorMetricsEvent().getRole() == null
				|| iteratorMetricsEventHolder.getIteratorMetricsEvent().getRole().isEmpty()) {
			iteratorMetricsEventHolder.getIteratorMetricsEvent().setRole(DEFAULT_EVENT_ROLE);
		}
	}

	private void end(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		try {
			// bEndInvoked = true;
			verifyStartInvoked(iteratorMetricsEventHolder.getIteratorMetricsEvent().getServiceUri());
			createEvent(iteratorMetricsEventHolder);
			startEventMap.remove(iteratorMetricsEventHolder.getIteratorMetricsEvent().getServiceUri());
		} catch (Exception e) {
			LOGGER.warn(null, "end", "error while creating event: [{}]", iteratorMetricsEventHolder);
		}
	}

	private void timedOut(final String serviceUri) {
		IteratorMetricsEventHolder iteratorMetricsEventHolder = null;
		try {
			LOGGER.info(null, "timedOut", "start:service URI [{}]", serviceUri);
			iteratorMetricsEventHolder = startEventMap.get(serviceUri);
			assignTimeoutEventDefaults(iteratorMetricsEventHolder);
			createEvent(iteratorMetricsEventHolder);
			LOGGER.info(null, "timedOut", "end:service URI [{}]", serviceUri);
		} catch (Exception e) {
			LOGGER.warn(null, "timedOut", "error while creating event: [{}]", iteratorMetricsEventHolder);
		}
	}

	private void verifyStartInvoked(final String serviceUri) {
		if (!startEventMap.containsKey(serviceUri)) {// end invoked but somehow
														// the start event is
														// missing
			LOGGER.warn(null, "verifyStartInvoked",
					"Iterator metrics collection was never started for this service URI [{}]", serviceUri);
		}
	}

	private void createEvent(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		LOGGER.info(null, "createEvent", "creating event: [{}]", iteratorMetricsEventHolder);
		DME2Event event = new DME2Event();

		event.setEventTime(iteratorMetricsEventHolder.getIteratorMetricsEvent().getEventTime());
		event.setType(iteratorMetricsEventHolder.getEventType());
		event.setQueueName(iteratorMetricsEventHolder.getIteratorMetricsEvent().getServiceUri());
		event.setElapsedTime(iteratorMetricsEventHolder.getElapsedTime());
		event.setMessageId(iteratorMetricsEventHolder.getIteratorMetricsEvent().getConversationId());
		event.setClientAddress(iteratorMetricsEventHolder.getIteratorMetricsEvent().getClientIp());
		event.setRole(iteratorMetricsEventHolder.getIteratorMetricsEvent().getRole());
		event.setProtocol(iteratorMetricsEventHolder.getIteratorMetricsEvent().getProtocol());
		event.setPartner(iteratorMetricsEventHolder.getIteratorMetricsEvent().getPartner());
		eventManager.postEvent(event);
	}

	private void cancelActiveEvents() {
		LOGGER.info(null, "cancelExistingEvents", "start");
		LOGGER.debug(null, "cancelExistingEvents", " before: [{}]", startEventMap.toString());
		try {
			for (Entry<String, IteratorMetricsEventHolder> entry : startEventMap.entrySet()) {
				LOGGER.debug(null, "cancelExistingEvents", "[{}]", entry.getValue());
				cancelEvent(entry.getValue());
			}
			startEventMap.clear();
		} catch (Exception e) {
			LOGGER.warn(null, "cancelExistingEvents", "error: [{}]", e.getMessage());
		}

		LOGGER.debug(null, "cancelExistingEvents", "after: [{}]", startEventMap.toString());
		LOGGER.info(null, "cancelExistingEvents", "end");
	}

	private void cancelEvent(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		LOGGER.info(null, "createEvent", "creating event: [{}]", iteratorMetricsEventHolder);
		DME2Event event = new DME2Event();

		event.setType(EventType.CANCEL_REQUEST_EVENT);

		// set elapsed time before overriding event time so that we can get the
		// previous event time to calculate the elapsed time
		event.setElapsedTime(
				System.currentTimeMillis() - iteratorMetricsEventHolder.getIteratorMetricsEvent().getEventTime());
		event.setEventTime(System.currentTimeMillis());
		event.setQueueName(iteratorMetricsEventHolder.getIteratorMetricsEvent().getServiceUri());
		event.setMessageId(iteratorMetricsEventHolder.getIteratorMetricsEvent().getConversationId());
		event.setClientAddress(iteratorMetricsEventHolder.getIteratorMetricsEvent().getClientIp());
		event.setRole(iteratorMetricsEventHolder.getIteratorMetricsEvent().getRole());
		event.setProtocol(iteratorMetricsEventHolder.getIteratorMetricsEvent().getProtocol());
		event.setPartner(iteratorMetricsEventHolder.getIteratorMetricsEvent().getPartner());
		eventManager.postEvent(event);
	}

	private String getIpAddr() {
		String ip = null;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			LOGGER.debug(null, "getIpAddr", "cannot resolve local host address");
		}
		return ip;
	}

	private void registerEventHandlers(final IteratorCreatingAttributes iteratorCreatingAttributes) {
		if (iteratorCreatingAttributes != null) {
			if (iteratorCreatingAttributes.getRequestEventProcessor() != null) {
				eventManager.registerEventProcessor(EventType.REQUEST_EVENT.getName(),
						iteratorCreatingAttributes.getRequestEventProcessor());
			} else {
				eventManager.registerEventProcessor(EventType.REQUEST_EVENT.getName(),
						new DME2RequestEventProcessor(config));
			}
			if (iteratorCreatingAttributes.getReplyEventProcessor() != null) {
				eventManager.registerEventProcessor(EventType.REPLY_EVENT.getName(),
						iteratorCreatingAttributes.getReplyEventProcessor());
			} else {
				eventManager.registerEventProcessor(EventType.REPLY_EVENT.getName(),
						new DME2ReplyEventProcessor(config));
			}
			if (iteratorCreatingAttributes.getFaultEventProcessor() != null) {
				eventManager.registerEventProcessor(EventType.FAULT_EVENT.getName(),
						iteratorCreatingAttributes.getFaultEventProcessor());
			} else {
				eventManager.registerEventProcessor(EventType.FAULT_EVENT.getName(),
						new DME2FaultEventProcessor(config));
			}
			if (iteratorCreatingAttributes.getTimeoutEventProcessor() != null) {
				eventManager.registerEventProcessor(EventType.TIMEOUT_EVENT.getName(),
						iteratorCreatingAttributes.getTimeoutEventProcessor());
			} else {
				eventManager.registerEventProcessor(EventType.TIMEOUT_EVENT.getName(),
						new DME2FaultEventProcessor(config));
			}
			if (iteratorCreatingAttributes.getCancelRequestEventProcessor() != null) {
				eventManager.registerEventProcessor(EventType.CANCEL_REQUEST_EVENT.getName(),
						iteratorCreatingAttributes.getCancelRequestEventProcessor());
			} else {
				eventManager.registerEventProcessor(EventType.CANCEL_REQUEST_EVENT.getName(),
						new DME2CancelRequestEventProcessor(config));
			}
		} else {
			LOGGER.error(null, "registerEventHandlers", "iteratorCreatingAttributes is null");
		}
	}

	private void verify(final IteratorMetricsEventHolder iteratorMetricsEventHolder) {
		if (iteratorMetricsEventHolder.getIteratorMetricsEvent().getServiceUri() == null) {
			throw new IteratorException(IteratorException.IteratorErrorCatalogue.ITERATOR_001);
		}
		if (iteratorMetricsEventHolder.getIteratorMetricsEvent().getConversationId() == null
				|| iteratorMetricsEventHolder.getIteratorMetricsEvent().getConversationId().isEmpty()) {
			if (uniqueId == null || uniqueId.isEmpty()) {
				LOGGER.error(null, "verify", "Conversation Id is null");
				throw new IteratorException(IteratorException.IteratorErrorCatalogue.ITERATOR_003);
			} else {
				iteratorMetricsEventHolder.getIteratorMetricsEvent().setConversationId(uniqueId);
			}
		}
	}

	private String generateUniqueTransactionReference() {
		StringBuffer uniqueReference = new StringBuffer();

		uniqueReference.append(this.hashCode());
		uniqueReference.append("-");
		uniqueReference.append(UUID.randomUUID().toString());

		// transactionReference = uniqueReference.toString();

		return uniqueReference.toString();

	}

	private void timeoutAllServicesIfExpired(boolean awaitExpiration) {
		LOGGER.info(null, "timeoutAllServicesIfExpired", "start");
		LOGGER.debug(null, "timeoutAllServicesIfExpired", " before: [{}]", startEventMap.toString());
		try {
			List<String> keysToRemove = new ArrayList<String>();
			for (Entry<String, IteratorMetricsEventHolder> entry : startEventMap.entrySet()) {
				LOGGER.debug(null, "timeoutAllServicesIfExpired",
						"awaitExpiration:[{}], timeout check for serviceUri [{}]", awaitExpiration, entry.getKey());
				if (awaitExpiration) {
					if (entry.getValue().getIteratorMetricsEvent().getTimeOutMS() == -1L) {
						LOGGER.debug(null, "timeoutAllServicesIfExpired",
								"Default Timeout:[{}], Service Uri: [{}], Active time: [{}]",
								EVENT_TIMEOUT_TOTAL_WAITING_MS, entry.getKey(),
								System.currentTimeMillis() - entry.getValue().getIteratorMetricsEvent().getEventTime());
						if (System.currentTimeMillis() - entry.getValue().getIteratorMetricsEvent()
								.getEventTime() >= EVENT_TIMEOUT_TOTAL_WAITING_MS) {
							timedOut(entry.getKey());
							keysToRemove.add(entry.getKey());
						} else {
							LOGGER.debug(null, "timeoutAllServicesIfExpired", "serviceUri [{}] is active",
									entry.getKey());
						}
					} else {
						LOGGER.debug(null, "timeoutAllServicesIfExpired",
								"Client timeout:[{}], Service Uri: [{}], Active time: [{}]",
								entry.getValue().getIteratorMetricsEvent().getTimeOutMS(), entry.getKey(),
								System.currentTimeMillis() - entry.getValue().getIteratorMetricsEvent().getEventTime());
						if (System.currentTimeMillis()
								- entry.getValue().getIteratorMetricsEvent().getEventTime() >= entry.getValue()
										.getIteratorMetricsEvent().getTimeOutMS()) {
							timedOut(entry.getKey());
							keysToRemove.add(entry.getKey());
						} else {
							LOGGER.debug(null, "timeoutAllServicesIfExpired", "serviceUri [{}] is active",
									entry.getKey());
						}
					}
				} else {
					timedOut(entry.getKey());
					keysToRemove.add(entry.getKey());
				}
			}
			for (String keyToRemove : keysToRemove) {
				LOGGER.info(null, "timeoutAllServicesIfExpired", "serviceUri [{}] is active", keysToRemove);
				startEventMap.remove(keyToRemove);
			}
			keysToRemove.clear();
		} catch (Exception e) {
			LOGGER.warn(null, "timeoutAllServicesIfExpired", "error: [{}]", e.getMessage());
		}

		LOGGER.debug(null, "timeoutAllServicesIfExpired", "after: [{}]", startEventMap.toString());
		LOGGER.info(null, "timeoutAllServicesIfExpired", "end");
	}

	/**
	 * set raiseTimeoutWithoutCheck as True when timeout forcefully, specially
	 * during finalize.
	 * <p>
	 * False when timeout check needed as per the configured time to wait before
	 * actually raising timeout.
	 * 
	 * @param raiseTimeoutWithoutCheck
	 */
	private void scheduleEventTimeoutChecker() {
		LOGGER.info(null, "scheduleEventTimeoutChecker", "timeout check");

		final Runnable timeoutChecker = new Runnable() {
			public void run() {
				LOGGER.info(null, "scheduleEventTimeoutChecker", "start timeout checker");
				timeoutAllServicesIfExpired(true);
				LOGGER.info(null, "scheduleEventTimeoutChecker", "complete timeout checker");
			}
		};
		eventTimeoutCheckerHandle = ScheduledExecution.schedule("iterator-metricsevent-timeoutchecker",
				MAX_THREAD_COUNT_TIMEOUT_CHECKER, timeoutChecker, EVENT_CHECKER_SCHEDULER_DELAY_MS);
	}

	@Override
	protected void finalize() throws Throwable {
		LOGGER.info(null, "finalize", "checking timedout start events");
		try {
			eventTimeoutCheckerHandle.cancel(true);
		} catch (Exception e) {

		}
		timeoutAllServicesIfExpired(false);
	}

	@Override
	public Set<String> getActiveServices() {
		return startEventMap != null ? startEventMap.keySet() : new HashSet<String>();
	}

	@Override
	public void setMetricsConversationId(final String uniqueId) {
		this.uniqueId = uniqueId;

	}
}