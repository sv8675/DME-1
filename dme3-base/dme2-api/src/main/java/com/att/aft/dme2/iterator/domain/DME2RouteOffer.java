/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 */
package com.att.aft.dme2.iterator.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.helper.AvailableEndpoints;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.types.RouteOffer;


/**
 * The Class RouteOfferHolder.
 */
public class DME2RouteOffer implements java.io.Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6514516982372370452L;
	private String envContext;
	private String fqName;
	private DME2Endpoint[] hardCodedEndpoints;
	private RouteOffer routeOffer;
	private String service;
	private String version;
	private boolean searchWithWildcard;

	/**
	 * The search filter to use when looking up endpoints for this route offer.
	 * Normally this is just the name but in cases where a merged routeoffer is
	 * needed it may be a comma-separated list
	 */
	private String searchFilter;

	private DME2Manager manager;

	/**
	 * Instantiates a new http route offer from a named route offer key
	 */
	public DME2RouteOffer(String service, String version, String envContext, RouteOffer routeOffer, String fqName,
			DME2Manager manager) {
		this(service, version, envContext, fqName, routeOffer.getName(), routeOffer, (DME2Endpoint[]) null, manager);
	}

	/**
	 * Instantiates a new e http route offer from specific endpoints
	 */
	public DME2RouteOffer(String service, String version, String envContext, String fqName, DME2Endpoint[] endpoints,
			DME2Manager manager) {
		this(service, version, envContext, fqName, fqName, null, endpoints, manager);
	}

	private DME2RouteOffer(String service, String version, String envContext, String fqName, String searchFilter,
			RouteOffer routeOffer, DME2Endpoint[] endpoints, DME2Manager manager) {
		this.service = service;
		this.version = version;
		this.envContext = envContext;
		this.fqName = fqName;
		this.hardCodedEndpoints = endpoints;
		this.manager = manager;
		this.searchFilter = searchFilter;
		this.routeOffer = routeOffer;
	}

	@SuppressWarnings("unused")
	private DME2RouteOffer() {
		/*
		 * Jackson JSON processor requires a default constructor in order to
		 * serialze objects
		 */
	}

	public DME2Manager getManager() {
		return this.manager;
	}

	/**
	 * Gets the env context.
	 * 
	 * @return the envContext
	 */
	public String getEnvContext() {
		return envContext;
	}

	/**
	 * Gets the fq name.
	 * 
	 * @return the fq name
	 */
	public String getFqName() {
		return fqName;
	}

	/**
	 * Gets the route offer.
	 * 
	 * @return the routeOffer
	 */
	public RouteOffer getRouteOffer() {
		return routeOffer;
	}

	/**
	 * Gets the sequence.
	 * 
	 * @return the sequence
	 */
	public Integer getSequence() {
		if (routeOffer == null) {
			return 1;
		}
		return routeOffer.getSequence();
	}

	/**
	 * returns true if and only if the DME2Manager is the same AND the
	 * searchFilter matches
	 * 
	 * @param o
	 * @return
	 */

	public boolean equals(DME2RouteOffer o) // NOSONAR
	{
		return o != null && this.searchFilter.equals(o.searchFilter);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Gets the service.
	 * 
	 * @return the service
	 */
	public String getService() {
		return service;
	}

	/**
	 * Gets the version.
	 * 
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Checks if is active.
	 * 
	 * @return true, if is active
	 */
	public boolean isActive() {
		if (routeOffer == null) {
			return true;
		} else {
			return this.routeOffer.isActive();
		}
	}

	/**
	 * To exploded string.
	 * 
	 * @return the string
	 */
	public String toExplodedString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("SERVICE=" + this.getService() + "; VERSION=" + this.getVersion() + "; ENV="
				+ this.getEnvContext() + "; ROUTEOFFER=" + this.getFqName() + "; SEQUENCE=" + getSequence() + "\n");
		DME2Endpoint[][] eps;
		try {
			eps = AvailableEndpoints.find(this, true, this.getManager());
		} catch (DME2Exception e) {
			buffer.append("  |-- ERROR: " + e.toString());
			return buffer.toString();
		}

		int counter = 0;
		boolean started = false;
		for (DME2Endpoint[] band : eps) {
			if (band.length == 0) {
				counter++;
				continue;
			}
			if (started) {
				buffer.append("\n");
			} else {
				started = true;
			}
			buffer.append("  |-- BAND=" + counter + "\n");
			for (DME2Endpoint element : band) {
				buffer.append("      |-- EP=" + element);
			}
			counter++;
		}
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(
				"Service=" + this.getService() + "; Version=" + this.getVersion() + "; env=" + this.getEnvContext()
						+ "; RouteOfferFilter=" + this.getSearchFilter() + "; Sequence=" + getSequence());
		return buffer.toString();
	}

	public DME2RouteOffer withSearchFilter(String newSearchFilter) {
		return new DME2RouteOffer(service, version, envContext, fqName, newSearchFilter, routeOffer,
				hardCodedEndpoints, manager);
	}

	public String getSearchFilter() {
		return searchFilter;
	}

	@Override
	public DME2RouteOffer clone() {
		return new DME2RouteOffer(service, version, envContext, fqName, searchFilter, routeOffer, hardCodedEndpoints,
				manager);
	}

	public DME2Endpoint[] getHardCodedEndpoints() {
		return hardCodedEndpoints;
	}

	public boolean isSearchWithWildcard() {
		return searchWithWildcard;
	}

	public void setSearchWithWildcard(boolean searchWithWildcard) {
		this.searchWithWildcard = searchWithWildcard;
	}

	/**
	 * Gets the available endpoints.
	 * 
	 * @return the available endpoints
	 * @throws com.att.aft.dme2.manager.registry.DME2Endpoint
	 *             the e http exception
	 */
	public DME2Endpoint[][] getAvailableEndpoints(boolean useVersionRange) throws DME2Exception {
		// TODO: Need to make this "roundrobin" the endpoints in each band.
		// by returning a sliding list to each call to getAvailableEndpoints.
		// this can probably be done with a linked list:
		// also the call to registry.find each time will need to
		// be altered to compare/add/remove from the linked lists
		// if the endpoints have changed since the last call.
		String serviceKey = null;
		if (this.searchWithWildcard) {
			serviceKey = getService() + "*";
		} else {
			serviceKey = getService();
		}

		final DME2Endpoint[] eps = hardCodedEndpoints != null ? hardCodedEndpoints
				: manager.findEndpoints(serviceKey, getVersion(), envContext, searchFilter, useVersionRange);
		return organize(eps);
	}
	

	// Organize the list into "bands" based on distance, then randomize the
	// content of each band
	/**
	 * split a flat list of endpoints into groups by distance (see manager.getDistanceBands())
	 * shuffle endpoints within each band and return as a list of bands, each a list of endpoints
	 * 
	 * @param eps
	 * @return the e http endpoint[][]
	 */
	public DME2Endpoint[][] organize(DME2Endpoint[] eps) {
		Map<Integer, List<DME2Endpoint>> bandLists = new HashMap<Integer, List<DME2Endpoint>>();

		double[] bands = manager.getDistanceBands();

		for (int i = 0; i < bands.length; i++) {
			bandLists.put(i, new ArrayList<DME2Endpoint>());
		}

		for (DME2Endpoint ep : eps) {
			double distance = ep.getDistance();
			for (int i = 0; i < bands.length; i++) {
				if (distance < bands[i]) {
					// workaround - sometimes we seem to get all eps back for multiple offers at once..
					bandLists.get(i).add(ep);
					break;
				}
			}
		}

		DME2Endpoint[][] bandedEps = new DME2Endpoint[bands.length][0];

		int counter = 0;
		for (List<DME2Endpoint> list : bandLists.values()) {
			Collections.shuffle(list);
			bandedEps[counter] = list.toArray(new DME2Endpoint[list.size()]);
			counter++;
		}

		return bandedEps;
	}
}
