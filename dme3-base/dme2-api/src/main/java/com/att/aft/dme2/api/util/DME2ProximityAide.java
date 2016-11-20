/*
 * Copyright 2016 AT&T Intellectual Properties, Inc.
 */
package com.att.aft.dme2.api.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * A manager of great circle distance calculations and cached data. The
 * ProximityAide calculates distances between a configured client location and
 * another set of supplied service coordinates, caching a configured number of
 * distance values.
 * 
 * @author AFT Discovery team
 */
public class DME2ProximityAide {

	/** Maximum calculated great circle distance between two points. */
	public static final double CALCULATED_DISTANCE_MAX = 20000.0;
	
	/** Conversion constant from degrees to radians. */
	public static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

	/** The current client latitude location in radians. */
	private static double clientLatitudeRadians = 38.6 * DEGREES_TO_RADIANS;

	/** The current client longitude location in radians. */
	private static double clientLongitudeRadians = -90.2 * DEGREES_TO_RADIANS;
	

	/** Earth radius to employ in distance calculations. */
	public static final double EARTH_RADIUS_IN_KILOMETERS = 6373.0;

	/**
	 * Bands of radii (km) within which service instances are considered equally
	 * distant from the client location.
	 */
	private double[] DISTANCEBANDS = null;

	/**
	 * Default bands of radii (km) within which service instances are considered
	 * equally distant from the client location.
	 */
	private static final double[] DISTANCE_BANDS_DEFAULT = { 0.1, 500.0, 5000.0,
			CALCULATED_DISTANCE_MAX };

	/** Maximum size of the great circle distance cache. */
	private int DISTANCECACHESIZE;

	/** Cache of the great circle distances (km) to various service coordinates. */
	private final ConcurrentMap<String, Double> distanceMap = new ConcurrentHashMap<String, Double>();

	/** Distance keys, ordered from least recently to most recently added. */
	private final ConcurrentLinkedQueue<String> distanceQueue = new ConcurrentLinkedQueue<String>();

	/**
	 * Determines the distance (km) between the current client coordinates and
	 * the specified service latitude and longitude.
	 * 
	 * @param serviceLatitudeDegrees
	 *            the service location in degrees of latitude.
	 * @param serviceLongitudeDegrees
	 *            the service location in degrees of longitude.
	 * @return the distance between the current client coordinates and the
	 *         service latitude and longitude provided.
	 */
	private double calculateDistance(double serviceLatitudeDegrees,
			double serviceLongitudeDegrees) {
		// convert service coordinate degrees to radians
		double serviceLatitudeRadians = serviceLatitudeDegrees
				* DEGREES_TO_RADIANS;
		double serviceLongitudeRadians = serviceLongitudeDegrees
				* DEGREES_TO_RADIANS;

		// determine the difference between the service and client latitudes and
		// longitudes
		double deltaLatitude = serviceLatitudeRadians - clientLatitudeRadians;
		double deltaLongitude = serviceLongitudeRadians
				- clientLongitudeRadians;

		// calculate intermediate results
		double a = Math.pow(Math.sin(deltaLatitude / 2.0), 2.0)
				+ Math.cos(clientLatitudeRadians)
				* Math.cos(serviceLatitudeRadians)
				* Math.pow(Math.sin(deltaLongitude / 2.0), 2.0);
		double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

		// determine the final distance value
		return c * EARTH_RADIUS_IN_KILOMETERS;
	}

	/**
	 * Creates a key used by the distance map and distance queue from the
	 * latitude and longitude values provided.
	 * 
	 * @param serviceLatitudeDegrees
	 *            the service location in degrees of latitude.
	 * @param serviceLongitudeDegrees
	 *            the service location in degrees of longitude.
	 * @return a key value corresponding to the specified latitude and
	 *         longitude.
	 */
	private String getDistanceKey(double serviceLatitudeDegrees,
			double serviceLongitudeDegrees) {
		return (new StringBuffer(32)).append(serviceLatitudeDegrees)
				.append(',').append(serviceLongitudeDegrees).toString();
	}

	/**
	 * Retrieves the cached distance from the configured client location to the
	 * provided latitude and longitude coordinates of a service endpoint,
	 * calculating a new value if necessary.
	 * 
	 * @param serviceLatitudeDegrees
	 *            the service location in degrees of latitude.
	 * @param serviceLongitudeDegrees
	 *            the service location in degrees of longitude.
	 * @return the cached distance value associated with the specified key, or
	 *         -1.0 if no cached value exists.
	 */
	public double getDistanceTo(double serviceLatitudeDegrees,
			double serviceLongitudeDegrees) {
		double result = -1.0;
		String distanceKey = getDistanceKey(serviceLatitudeDegrees,
				serviceLongitudeDegrees);
		Double cachedDistance = distanceMap.get(distanceKey);
		if (cachedDistance == null) {
			result = calculateDistance(serviceLatitudeDegrees,
					serviceLongitudeDegrees);
			setDistanceTo(serviceLatitudeDegrees, serviceLongitudeDegrees,
					result);
		} else {
			result = cachedDistance.doubleValue();
		}
		return result;
	}

	/**
	 * Updates the distance corresponding to the provided latitude and longitude
	 * values.
	 * 
	 * @param serviceLatitudeDegrees
	 *            the service location in degrees of latitude.
	 * @param serviceLongitudeDegrees
	 *            the service location in degrees of longitude.
	 * @param distanceValue
	 *            the distance value associated with the specified latitude and
	 *            longitude.
	 */
	private void setDistanceTo(double serviceLatitudeDegrees,
			double serviceLongitudeDegrees, double distanceValue) {
		String distanceKey = getDistanceKey(serviceLatitudeDegrees,
				serviceLongitudeDegrees);
		distanceMap.put(distanceKey, distanceValue);
		distanceQueue.add(distanceKey);
		int distanceQueueSize = distanceQueue.size();
		if (distanceQueueSize > DISTANCECACHESIZE) {
			synchronized (distanceQueue) {
				if (distanceQueueSize > 0) {
					String cachedDistanceKey = distanceQueue.remove();
					distanceMap.remove(cachedDistanceKey);
				}
			}
		}
	}

	/**
	 * Sets the client coordinate and cache size values.
	 * 
	 * @param clientLatitudeDegrees
	 *            the client location in degrees of latitude.
	 * @param clientLongitudeDegrees
	 *            the client location in degrees of longitude.
	 * @param distanceCacheSize
	 *            the maximum number of calculated great circle distances to
	 *            cache.
	 * @param distanceBands
	 *            the distance bands
	 * @param isExcludingOutOfBandEndpoints
	 *            the is excluding out of band endpoints
	 */
	public DME2ProximityAide(double clientLatitudeDegrees,
			double clientLongitudeDegrees, int distanceCacheSize,
			double[] distanceBands, boolean isExcludingOutOfBandEndpoints) {
		// establish client coordinates and cache size
		clientLatitudeRadians = clientLatitudeDegrees * DEGREES_TO_RADIANS;
		clientLongitudeRadians = clientLongitudeDegrees * DEGREES_TO_RADIANS;
		DISTANCECACHESIZE = distanceCacheSize;
		if (distanceBands != null) {
			int length = distanceBands.length;
			if (distanceBands[length - 1] >= CALCULATED_DISTANCE_MAX
					|| isExcludingOutOfBandEndpoints) {
				DISTANCEBANDS = new double[length];
				System.arraycopy(distanceBands, 0, DISTANCEBANDS, 0, length);
			} else {
				// add a final maximum calculated distance value
				DISTANCEBANDS = new double[length + 1];
				System.arraycopy(distanceBands, 0, DISTANCEBANDS, 0, length);
				DISTANCEBANDS[length] = CALCULATED_DISTANCE_MAX;
			}
		} else {
			DISTANCEBANDS = DISTANCE_BANDS_DEFAULT;
		}
	}

	public double[] getDistanceBands() {
		return DISTANCEBANDS;
		
	}
}
