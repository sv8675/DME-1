package com.att.aft.dme2.event;

/**
 * This interface provides an interface to handle Metrics API Publishing functionality
 * @author po704t
 *
 */
public interface BaseMetricsPublisher {

	public void publishEvent(DME2Event event, DME2ServiceStats ss);

}
