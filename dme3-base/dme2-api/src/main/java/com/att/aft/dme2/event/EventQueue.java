package com.att.aft.dme2.event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * The EventQueue holds a queue for events.
 * The DME2EventManager sends all the events to EventQueue and returns so the events can be process in an asynchronous fashion.
 * @author po704t
 *
 */
public class EventQueue {

	private final BlockingQueue<DME2Event> eventQueue;

/**
 * Constructor for event Queue
 */
	public EventQueue() {
		eventQueue = new LinkedBlockingQueue<DME2Event>();
	}

	/**
	 * Constructor for event Queue with a fixed queue length
	 */
	public EventQueue(int capacity) {
		eventQueue = new LinkedBlockingQueue<DME2Event>(capacity);
	}

/**
 * Returns an unprocessed event from the Queue
 */
	public DME2Event getEvent() throws EventQueueException {
		try {
			return (DME2Event) eventQueue.take();
		} catch (InterruptedException e) {
			throw new EventQueueException(e);
		}
	}

	
/**
 * The pollEvent method returns events to the event processor and if there are no pending events, waits for a specific amount of time and returns.
 * @return
 * @throws EventQueueException
 */
	public DME2Event pollEvent() throws EventQueueException {
		try {
			return (DME2Event) eventQueue.poll(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new EventQueueException(e);
		}
	}

	
/**
 * The addEvent method adds a new DME2Event to the EventQueue.
 * @param event
 * @throws EventQueueException
 */
	public void addEvent(DME2Event event) throws EventQueueException {
		try {
			eventQueue.put(event);
		} catch (InterruptedException e) {
			EventQueueException we = new EventQueueException(e.getMessage());
			throw we;
		}
	}

	
/**
 * The getSize returns the size of the Event Queue.	
 * @return
 */
	public int getSize() {
		return eventQueue.size();
	}

}
