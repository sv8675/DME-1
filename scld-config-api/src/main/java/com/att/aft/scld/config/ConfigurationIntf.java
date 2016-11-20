/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config;

public interface ConfigurationIntf {

	public  String getProperty(String propertyName);

	public  int getInt(String propertyName);

	public  float getFloat(String propertyName);
	
	public  long getLong(String propertyName);

	public  boolean getBoolean(String propertyName);
	
}
