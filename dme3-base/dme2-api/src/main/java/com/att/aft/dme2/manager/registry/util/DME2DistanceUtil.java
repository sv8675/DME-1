package com.att.aft.dme2.manager.registry.util;

/**
 * Utility for distance and distance band calculations
 */
public class DME2DistanceUtil {
  private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;
  public static final double EARTH_RADIUS_IN_KILOMETERS = 6373.0;

  private DME2DistanceUtil() {

  }

  public static double calculateDistanceBetween( double sourceLatitude, double sourceLongitude, double destinationLatitude, double destinationLongitude ) {
    // Keeping this as it was in DME2, though there is Math.toRadians
    double sourceLatitudeRadians = sourceLatitude * DEGREES_TO_RADIANS;
    double sourceLongitudeRadians = sourceLongitude * DEGREES_TO_RADIANS;
    double destinationLatitudeRadians = destinationLatitude * DEGREES_TO_RADIANS;
    double destinationLongitudeRadians = destinationLongitude * DEGREES_TO_RADIANS;

    double deltaLatitude = destinationLatitudeRadians - sourceLatitudeRadians;
    double deltaLongitude = destinationLongitudeRadians - sourceLongitudeRadians;

    double a = Math.pow(Math.sin(deltaLatitude / 2.0), 2.0)
        + Math.cos(sourceLatitudeRadians)
        * Math.cos(destinationLatitudeRadians)
        * Math.pow(Math.sin(deltaLongitude / 2.0), 2.0);

    double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    return c * EARTH_RADIUS_IN_KILOMETERS;
  }
}
