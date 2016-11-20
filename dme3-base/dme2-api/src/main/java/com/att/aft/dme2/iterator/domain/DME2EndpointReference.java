package com.att.aft.dme2.iterator.domain;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class DME2EndpointReference {
	private static final Logger logger = LoggerFactory.getLogger(DME2EndpointReference.class.getName());
	private DME2Manager manager;
	private DME2Endpoint endpoint;
	private DME2RouteOffer routeOffer;
	private Integer sequence;
	private Double distanceBand;

	public DME2Manager getManager() {
		return manager;
	}

	public DME2EndpointReference(DME2Manager manager, DME2Endpoint endpoint) {
		this.manager = manager;
		this.endpoint = endpoint;
	}

	public DME2EndpointReference setManager(DME2Manager manager) {
		this.manager = manager;
		return this;
	}

	public DME2Endpoint getEndpoint() {
		return endpoint;
	}

	public DME2EndpointReference setEndpoint(DME2Endpoint endpoint) {
		this.endpoint = endpoint;
		return this;
	}

	public DME2RouteOffer getRouteOffer() {
		return routeOffer;
	}

	public DME2EndpointReference setRouteOffer(DME2RouteOffer routeOffer) {
		this.routeOffer = routeOffer;
		return this;
	}

	public Integer getSequence() {
		return sequence;
	}

	public DME2EndpointReference setSequence(Integer sequence) {
		this.sequence = sequence;
		return this;
	}

	public Double getDistanceBand() {
		return distanceBand;
	}

	public DME2EndpointReference setDistanceBand(Double distanceBand) {
		this.distanceBand = distanceBand;
		return this;
	}

	public DME2EndpointReference() {
	}

	public boolean isStale() {
		return manager.isUrlInStaleList(endpoint.getServiceEndpointID());
	}

	public void setStale() {
		Long stalenessDuration = 0L;
		if (routeOffer != null) {
			if (routeOffer.getRouteOffer() != null) {
				stalenessDuration = routeOffer.getRouteOffer().getStalenessInMins();
				if (stalenessDuration != null) {
					stalenessDuration = (stalenessDuration * 60000); // Converting
																		// to
																		// milliseconds.
				}
			}
		}

		String msg = String.format("Marking Endpoint %s stale.", endpoint.getServiceEndpointID());

		logger.debug(null, "setStale", LogMessage.DEBUG_MESSAGE, msg);
		manager.addStaleEndpoint(endpoint.getServiceEndpointID(), stalenessDuration != null ? stalenessDuration : 0);
	}
}
