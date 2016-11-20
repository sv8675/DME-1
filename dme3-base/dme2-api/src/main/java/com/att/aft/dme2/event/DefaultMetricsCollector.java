package com.att.aft.dme2.event;

/**
 * This interface provides an interface to handle Metrics API Publishing functionality
 * @author po704t
 *
 */
public class DefaultMetricsCollector implements BaseMetricsPublisher {

	public void publishEvent(DME2Event event, DME2ServiceStats ss){
		
	}

	public static DefaultMetricsCollector getMetricsCollector(String containerName, String containerVersion,
			String containerRO, String containerEnv, String containerPlat, String containerHost, String containerPid,
			String containerPartner) {
		return null;
	}

}
