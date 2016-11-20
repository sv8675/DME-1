package com.att.aft.dme2.event;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;


/**
 * The DefaultMetricsPublisher class provides a Default class that implements the functionality to publish metrics to Metrics API.
 * @author po704t
 *
 */
public class DefaultMetricsPublisher implements BaseMetricsPublisher {

	private static DME2Configuration config;
	private static final Logger logger = LoggerFactory.getLogger(DefaultMetricsPublisher.class.getName());
	private static DefaultMetricsCollector collector = new DefaultMetricsCollector();
	
	public DefaultMetricsPublisher(DME2Configuration configuration) {
		config = configuration;
	}
	
/**
 * This method connects to the Factory and gets an instance of getMetricsCollector which will be used to publish events to Metrics API.getMetricsCollector
 */
	public static DefaultMetricsCollector getMetricsCollector(String partner, DME2ServiceStats stats) {
		logger.info(null, "getMetricsCollector", "entering getMetricsCollector");
//		return MetricsCollectorFactory.getMetricsCollector(stats.containerName, stats.containerVersion,
//				stats.containerRO, stats.containerEnv == null ? stats.serviceEnv : stats.containerEnv,
//				stats.containerPlat, stats.containerHost, stats.containerPid, partner);
		return collector;
	}

	/**
	 * This method identifies the type of event passed to it and publishes the event based on other Helper methods.
	 */
	@Override
	public void publishEvent(DME2Event event, DME2ServiceStats ss) {	
		logger.info(null, "publishEvent", "entering publishEvent");
//		if(event.getType().name() == EventType.REPLY_EVENT.name()){
//			addResponseEvent(ss, event);
//		}else if(event.getType().name() == EventType.FAULT_EVENT.name()){
//			addFaultEvent(ss, event);
//		}else if(event.getType().name() == EventType.FAILOVER_EVENT.name()){
//			addFailoverEvent(ss, event);
//		}
		logger.info(null, "publishEvent", "exiting publishEvent");
	}
	
	
	/**
	 * This method adds ResponseEvent to Metrics API.
	 * @param ss
	 * @param event
	 */
	public static void addResponseEvent(DME2ServiceStats ss, DME2Event event) {
		logger.info(null, "addResponseEvent", "entering addResponseEvent");
//		final MetricsCollector contCollector = getMetricsCollector(event.getPartner(), ss);
//		String role = (String) event.getRole();
//		String partner = (String) event.getPartner();
//		if (partner == null) {
//			partner = DME2Constants.DEFAULT_NA_VALUE;
//		}
//		if (role == null) {
//			role = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE);
//		}
//		String protocol = (String) event.getProtocol();
//		if (protocol == null) {
//			protocol = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL);
//		}
//		contCollector.addEvent(System.currentTimeMillis(), ss.service, ss.serviceVersion, role, event.getInterfacePort()+ "", protocol,
//				Event.RESPONSE, event.getMessageId(), event.getElapsedTime(), event.getElapsedTime());
//		if (config.getBoolean(DME2Constants.AFT_DME2_PUBLISH_METRICS)) {
//			contCollector.publish();
//			logger.debug( null, "addResponseEvent", "DMEServiceStats published metrics RESPONSE event");
//		}
		logger.info(null, "addResponseEvent", "exiting addResponseEvent");
	}

	
	/**
	 * This method adds FaultEvent to Metrics API.
	 * @param ss
	 * @param event
	 * 
	 */	
	public static void addFaultEvent(DME2ServiceStats ss, DME2Event event) {
		// contCollector.addEvent(System.currentTimeMillis(), service,
		// serviceVersion, role, port, protocol, Event.FAULT, msgId, elapsed,
		// elapsed);
		// if(DME2Constants.PUBLISH_METRICS) contCollector.publish();
		logger.info(null, "addFaultEvent", "entering addFaultEvent");
//		String role = (String) event.getRole();
//		String partner = (String) event.getPartner();
//		if (partner == null) {
//			partner = DME2Constants.DEFAULT_NA_VALUE;
//		}
//		if (role == null) {
//			role = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE);
//		}
//		String protocol = (String) event.getProtocol();
//		if (protocol == null) {
//			protocol = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL);
//		}
//		final MetricsCollector contCollector = getMetricsCollector(event.getPartner(), ss);
//		contCollector.addEvent(System.currentTimeMillis(), ss.service, ss.serviceVersion, role, event.getInterfacePort()+ "", protocol,
//				Event.FAULT, event.getMessageId(), event.getElapsedTime(), event.getElapsedTime());
//		if (config.getBoolean(DME2Constants.AFT_DME2_PUBLISH_METRICS)) {
//			contCollector.publish();
//			logger.debug( null, "addFaultEvent", "DMEServiceStats published metrics FAULT event");
//		}
		logger.info(null, "addFaultEvent", "exiting addFaultEvent");
	}

	
	/**
	 * This method adds FailoverEvent to Metrics API.
	 * @param ss
	 * @param event
	 * 
	 */		
	public static void addFailoverEvent(DME2ServiceStats ss, DME2Event event) {
		// contCollector.addEvent(System.currentTimeMillis(), service,
		// serviceVersion, role,port, protocol, Event.FAILOVER, msgId, elapsed,
		// elapsed);
		// if(DME2Constants.PUBLISH_METRICS) contCollector.publish();
		logger.info(null, "addFailoverEvent", "entering addFailoverEvent");
//		String role = (String) event.getRole();
//		String partner = (String) event.getPartner();
//		if (partner == null) {
//			partner = DME2Constants.DEFAULT_NA_VALUE;
//		}
//		if (role == null) {
//			role = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_SERVER_ROLE);
//		}
//		String protocol = (String) event.getProtocol();
//		if (protocol == null) {
//			protocol = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL);
//		}
//		final MetricsCollector contCollector = getMetricsCollector(event.getPartner(), ss);
//		contCollector.addEvent(System.currentTimeMillis(), ss.service, ss.serviceVersion, role, event.getInterfacePort()+ "", protocol,
//				Event.FAILOVER, event.getMessageId(), event.getElapsedTime(), event.getElapsedTime());
//		if (config.getBoolean(DME2Constants.AFT_DME2_PUBLISH_METRICS)) {
//			contCollector.publish();
//			logger.debug( null, "addFailoverEvent", "DMEServiceStats published metrics FAILOVER event");
//		}
		logger.info(null, "addFailoverEvent", "exiting addFailoverEvent");
	}


}
