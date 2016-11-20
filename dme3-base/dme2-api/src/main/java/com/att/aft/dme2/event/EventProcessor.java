package com.att.aft.dme2.event;

/**
 * The interface EventProcessor provides a method for various EventProcessors.
 * interface EventProcessor
 *
 */
public interface EventProcessor {
	public void handleEvent(DME2Event event) throws EventProcessingException;
}
