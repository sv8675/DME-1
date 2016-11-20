package com.att.aft.dme2.event;

import java.util.ArrayList;

/**
 * It provides the EventManagement interface.
 * interface EventManager
 *
 */
public interface EventManager {
	public void registerEventProcessor(String eventType, EventProcessor listener);

	public void unRegisterEventProcessor(String eventType, EventProcessor listener);

	public ArrayList<EventProcessor> getListeners(String eventType);

	public void postEvent(DME2Event event);
}
