package com.att.aft.dme2.event;

import java.util.ArrayList;
import java.util.concurrent.ThreadFactory;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

/**
 * The DME2EventDispatcher class provides a configured set of threads which process the events.
 * It picks up events from the EventQueue and executes the EventListeners for each event.
 * @author po704t
 *
 */

public class DME2EventDispatcher implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(DME2EventDispatcher.class.getName());
	private DME2Configuration config;
	int maxPoolSize;
						// 5);

	/**
	 * stopThreads is a flag which causes the background threads to quit processing events when a server shutdown request has beem semt.
	 * The usage is DME2EventDispatcher.setStopThreads(true)
	 */
	public static boolean stopThreads;

	public static boolean isStopThreads() {
		return stopThreads;
	}

	public static void setStopThreads(boolean stopThreads) {
		DME2EventDispatcher.stopThreads = stopThreads;
	}

	private transient final EventQueue eventQueue;

	private transient ThreadFactory threadFactory;

	private transient DME2EventManager eventManager;

	public DME2EventManager getEventManager() {
		return eventManager;
	}

	public void setEventManager(DME2EventManager eventManager) {
		this.eventManager = eventManager;
	}

/**
 * The DME2EventDispatcher constructor initiates the DME2EventDispatcher clas. 
 * @param queue
 */
	public DME2EventDispatcher(DME2Configuration config, EventQueue queue) {
		this.config = config;
		stopThreads = false;
		eventQueue = queue;
		threadFactory = new DefaultThreadFactory();
		maxPoolSize = config.getInt(DME2Constants.AFT_DME2_EVENT_PROCESSOR_THREADS);
		logger.debug(null,  null, "inside DME2EventDispatcher() - maxPoolSize : {}", maxPoolSize);
		for (int i = 0; i < maxPoolSize; i++) {
			Thread workerThread = threadFactory.newThread(new Worker());
			workerThread.start();
		}
    Runtime.getRuntime().addShutdownHook( new Thread( this ) );
	}

  @Override
  public void run() {
    stopThreads = true;
  }

  class UEHLogger implements Thread.UncaughtExceptionHandler {
		public void uncaughtException(Thread t, Throwable e) {
			logger.error(null, "uncaughtException", "inside UEHLogger - uncaughtException method {} threw exception: ", t, e);
		}
	}

	
/**
 * The Worker class implements the Worker thread.
 * Worker threads process events from the EventQueue and invoke the Eventlisteners registered for each EventType.
 * 
 * @author po704t
 *
 */
	class Worker implements Runnable {

		public void run() {
			while (!stopThreads) {
				try {
					final DME2Event event = eventQueue.pollEvent();
					if (event != null) {
//						logger.debug(null, null, "inside worker thread -  method run "
//								+ Thread.currentThread().getName() + " queue size - " + eventQueue.getSize());
//						logger.debug(null, null,
//								"inside worker thread -  method run " + Thread.currentThread().getName()
//										+ " for event - " + event.getMessageId() + " : " + event.getType().getName());
						ArrayList<EventProcessor> procesorsList = eventManager.getListeners(event.getType().getName());
						if (procesorsList != null) {
							for (EventProcessor processor : procesorsList) {
									processor.handleEvent(event);
							}
						} else {
//							logger.debug(null, null,
//									"inside worker thread -  method run " + Thread.currentThread().getName()
//											+ " no listener registered - skipping processing - "
//											+ eventQueue.getSize());
						}
					} else {
//						logger.debug(null, null,
//								"inside worker thread -  method run " + Thread.currentThread().getName()
//										+ " going to repoll for new events - " + eventQueue.getSize());
					}
				} catch (EventProcessingException ex) {
					logger.error(null, null, "exception in worker thread - method run ", ex);
				} catch (Throwable e) {
					logger.error(null, null, "exception in worker thread - method run ", e);
				}
			}
//			logger.info(null, "run", "Closing Worker Threads: -  method run {} queue size - {}", Thread.currentThread().getName(), eventQueue.getSize());
		}
	}

/**
 * The DefaultThreadFactory class provides an interface for creating Event Processing Worker Threads. 
 * @author po704t
 *
 */
	class DefaultThreadFactory implements ThreadFactory {
		int count = 0;
		final String namePrefix;

		public DefaultThreadFactory() {
			namePrefix = "pool-" + "-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(Thread.currentThread().getThreadGroup(), r, "DME2::EventWorkerThread-" + count, 0);
			count++;
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			t.setUncaughtExceptionHandler(new UEHLogger());
			logger.debug(null, "newThread", "inside DefaultThreadFactory -  method newThread");
			return t;
		}
	}

}