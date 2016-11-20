package com.att.aft.dme2.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/**
 * Class DME2EventManager DME2EventManager is an interface for Event Processing.
 * It allows different modules to submit events to an internal eventQueue. Once
 * the messages are posted to eventQueue, the EventDispatcher calls the event
 * processors to handle various events.
 *
 */
public class DME2EventManager implements EventManager {
	private static final Logger logger = LoggerFactory.getLogger(DME2EventManager.class.getName());
	private DME2Configuration config;
	// instance of event queue which holds the EventQueue
	private EventQueue eventQueue;
	//holds the Event listeners
	private ConcurrentHashMap<String, ArrayList<EventProcessor>> listenersMap = new ConcurrentHashMap<String, ArrayList<EventProcessor>>();
	// holds the instance of DME2EventDispatcher
	private DME2EventDispatcher dispatcher;
	private volatile static DME2EventManager INSTANCE;
	

	/**
	 * Constructor for DME2EventManaher
	 */
	private DME2EventManager(DME2Configuration config) {
		this.config = config;
		int eventQueueSize = config.getInt(DME2Constants.AFT_DME2_EVENT_QUEUE_SIZE);
		logger.debug(null, "ctor(DME2Configuration)", "inside method DME2EventManager() - eventQueueSize : {}", eventQueueSize);
		eventQueue = new EventQueue(eventQueueSize);
		dispatcher = new DME2EventDispatcher(config, eventQueue);
		dispatcher.setEventManager(this);
	}

	/**
	 * This method initializes the DME2EventMaager
	 * 
	 * @param manager
	 */
	public void initDME2EventManager() {
//		this.manager = manager;
	}

	public static DME2EventManager getInstance(DME2Configuration config) {
		DME2EventManager result = INSTANCE;
		if ( result == null ) {
			synchronized ( DME2EventManager.class ) {
        result = INSTANCE;
				if ( result == null ) {
					INSTANCE = result = new DME2EventManager( config );
				}
			}
		}
		return result;
	}

/**
 * It checks if the EventQueue is empty.
 * @return
 */
	public boolean isQueueEmpty() {
		final long total = eventQueue.getSize();
		return total <= 0;
	}

	
/**
 * The postEvent method is called by Event Publishers to submit event to a EventQueue from which Wroker Threads process the events.
 */
	@Override
	public void postEvent(DME2Event event) {
		try {
			logger.debug(null, "postEvent", "inside method postEvent ", event);
			eventQueue.addEvent(event);
		} catch (Exception ex) {
			logger.error(null, "inside method postEvent", "unknown error ", ex);
		}
	}

/**
 * The registerEventProcessor provides a way to register EventProcessors for different Event Type.	
 */
	@Override
	public void registerEventProcessor(String eventType, EventProcessor listener) {
		synchronized (listenersMap) {
			ArrayList<EventProcessor> procesorsList = listenersMap.get(eventType);
			if (procesorsList == null) {
				logger.debug(null, "registerEventProcessor", "inside method registerEventProcessor {}", eventType);
				procesorsList = new ArrayList<EventProcessor>();
				listenersMap.put(eventType, procesorsList);
			}
			if(procesorsList.size() == 0){
				procesorsList.add(listener);
			}
		}
	}

/**
 * The unregisterEventProcessor provides a way to unregister Event Processors for different Event Type.	
 */
	@Override
	public void unRegisterEventProcessor(String eventType, EventProcessor listener) {
		synchronized (listenersMap) {
			ArrayList<EventProcessor> procesorsList = listenersMap.get(eventType);
			logger.debug(null, "unRegisterEventProcessor", "inside method unRegisterEventProcessor {}", procesorsList == null ? 0 : procesorsList.size() );
			if (procesorsList != null && listener != null) {
				logger.debug(null, "unRegisterEventProcessor", "inside method unRegisterEventProcessor {}", eventType);
				procesorsList.remove(listener);
			}
		}
	}

/**
 * The method getListeners provides a method to return all the event processors.	
 */
	@Override
	public ArrayList<EventProcessor> getListeners(String eventType) {
		ArrayList<EventProcessor> procesorsList = listenersMap.get(eventType);
		return procesorsList;
	}

/**'
 *  The getQueueSize provides a way to retrieve the current Event Queue size.
 * @return
 */
	public int getQueueSize() {
		return eventQueue.getSize();
	}

	public DME2EventDispatcher getDispatcher() {
		return dispatcher;
	}
	
	public static void main(String args[]) {
		DME2Configuration config = new DME2Configuration();
		DME2EventManager eventManager = new DME2EventManager(config);
		DME2InitEventProcessor initProcessor = new DME2InitEventProcessor(config);
		DME2RequestEventProcessor reqProcessor = new DME2RequestEventProcessor(config);
		DME2ReplyEventProcessor resProcessor = new DME2ReplyEventProcessor(config);
		DME2FailoverEventProcessor failProcessor = new DME2FailoverEventProcessor(config);
		eventManager.registerEventProcessor(EventType.REQUEST_EVENT.getName(), reqProcessor);
		eventManager.registerEventProcessor(EventType.REPLY_EVENT.getName(), resProcessor);
		eventManager.registerEventProcessor(EventType.INIT_EVENT.getName(), initProcessor);
		eventManager.registerEventProcessor(EventType.FAILOVER_EVENT.getName(), failProcessor);
		HashMap<String, Object> props = null;
		for (int i = 0; i < 101; i++) {
			try {
				int k = 0;
				DME2Event event = new DME2Event();
				event.setType(EventType.INIT_EVENT);
				event.setMessageId("" + i + ":" + k);
				event.setReqMsgSize((long) i);
				event.setQueueName("test");
				event.setElapsedTime((long) (i));
				// event.setRole(DME2Constants.DME2_INTERFACE_CLIENT_ROLE);
				event.setElapsedTime((long) (i));
				event.setProtocol("http");
				event.setClientAddress("clientAddress");
				event.setEventTime(System.currentTimeMillis());
				eventManager.postEvent(event);
				logger.debug(null, "main", "adding event - {}:{}" , i , k);
				k++;
				event = new DME2Event();
				event.setType(EventType.REQUEST_EVENT);
				event.setMessageId("" + i + ":" + k);
				event.setReqMsgSize((long) i);
				event.setQueueName("test");
				event.setElapsedTime((long) (i));
				// event.setRole(DME2Constants.DME2_INTERFACE_CLIENT_ROLE);
				event.setElapsedTime((long) (i));
				event.setProtocol("http");
				event.setClientAddress("clientAddress");
				event.setEventTime(System.currentTimeMillis());
				eventManager.postEvent(event);
				logger.debug(null, "main", "adding event - {}:{}" , i, k);
				k++;
				event = new DME2Event();
				event.setType(EventType.FAILOVER_EVENT);
				event.setMessageId("" + i + ":" + k);
				event.setReqMsgSize((long) i);
				event.setQueueName("test");
				event.setInterfacePort("9090");
				event.setElapsedTime((long) (i));
				// event.setRole(DME2Constants.DME2_INTERFACE_CLIENT_ROLE);
				event.setProtocol("http");
				event.setClientAddress("clientAddress");
				event.setEventTime(System.currentTimeMillis());
				eventManager.postEvent(event);
				logger.debug( null, "main", "adding event - {}:{}", i, k);
				k++;
				event = new DME2Event();
				event.setMessageId("" + i + ":" + k);
				event.setType(EventType.REPLY_EVENT);
				event.setMessageId("" + i + ":" + k);
				event.setReplyMsgSize((long) i);
				event.setQueueName("test");
				event.setElapsedTime((long) (i));
				// event.setRole(DME2Constants.DME2_INTERFACE_CLIENT_ROLE);
				event.setElapsedTime((long) (i));
				event.setProtocol("http");
				event.setClientAddress("clientAddress");
				event.setEventTime(System.currentTimeMillis());
				eventManager.postEvent(event);
				event.setEventProps(props);
				eventManager.postEvent(event);
        logger.debug(null, "main", "adding event - {}:{}" , i, k);
				k++;
				/**
				 * event = new Event(); event.setType(EventType.REQUEST_EVENT);
				 * eventManager.eventQueue.addEvent(event); event = new Event();
				 * event.setType(EventType.FAULT_EVENT);
				 * eventManager.eventQueue.addEvent(event);
				 */
				if (i % 100 == 1)
					Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
		try {
			Thread.sleep(1);
			logger.debug(null, null, "main method - destroying worker threads");
			DME2EventDispatcher.setStopThreads(true);
		} catch (Exception e) {
			logger.error(null, null, "main method - destroying worker threads", e);
		}

	}

}
