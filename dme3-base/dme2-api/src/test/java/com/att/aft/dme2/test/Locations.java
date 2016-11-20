/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.test;

/**
 * The Enum Locations.
 */
public enum Locations {

	/** The BHAM. */
	BHAM("BHAM", "Birmingham", 33.373900, -86.798300),

	/** The CHAR. */
	CHAR("CHAR", "Charlotte", 35.318900, -80.762200),

	/** The JACK. */
	JACK("JACK", "Jackson", 32.282000, -90.281000);

	/** The city. */
	private String city;

	/** The city name. */
	private String cityName;

	/** The latitude. */
	private double latitude;

	/** The longitude. */
	private double longitude;

	/**
	 * Instantiates a new locations.
	 * 
	 * @param city
	 *            the city
	 * @param cityName
	 *            the city name
	 * @param latitude
	 *            the latitude
	 * @param longitude
	 *            the longitude
	 */
	Locations(String city, String cityName, double latitude, double longitude) {
		this.city = city;
		this.cityName = cityName;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Gets the city.
	 * 
	 * @return the city
	 */
	public String getCity() {
		return this.city;
	}

	/**
	 * Gets the city full name.
	 * 
	 * @return the city full name
	 */
	public String getCityFullName() {
		return this.cityName;
	}

	/**
	 * Gets the latitude.
	 * 
	 * @return the latitude
	 */
	public double getLatitude() {
		return this.latitude;
	}

	/**
	 * Gets the longitude.
	 * 
	 * @return the longitude
	 */
	public double getLongitude() {
		return this.longitude;
	}

	/**
	 * Sets the.
	 */
	public void set() {
		System.setProperty("AFT_LATITUDE", String.valueOf(this.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(this.getLongitude()));
	}

}
