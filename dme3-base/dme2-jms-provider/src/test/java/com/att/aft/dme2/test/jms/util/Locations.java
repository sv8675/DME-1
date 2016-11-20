/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;
public enum Locations
{
	BHAM("BHAM","Birmingham",33.373900,-86.798300),
	CHAR("CHAR","Charlotte",35.318900,-80.762200),
	JACK("JACK","Jackson",32.282000,-90.281000);
	
	private String city;
	private String cityName;
	private double latitude;
	private double longitude;
	
	Locations(String city,String cityName,double latitude,double longitude)
	{
		this.city = city;
		this.cityName = cityName;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public String getCity()
	{
		return this.city;
	}
	
	public String getCityFullName()
	{
		return this.cityName;
	}
	
	public double getLatitude()
	{
		return this.latitude;
	}
	
	public double getLongitude()
	{
		return this.longitude;
	}
	
	public void set()
	{
		System.setProperty("AFT_LATITUDE", String.valueOf(this.getLatitude()));
		System.setProperty("AFT_LONGITUDE", String.valueOf(this.getLongitude()));
	}

}
