package com.att.aft.dme2.event;

/**
 * It provides the EventGenerator interface. 
 * Any Class generating events should implement this Interface.
 * interface EventGenerator
 *
 */
public interface EventGenerator {

	public void postEvent(DME2Event event);

}
