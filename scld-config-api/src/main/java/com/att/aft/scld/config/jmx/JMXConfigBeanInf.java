/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config.jmx;

import com.att.aft.scld.config.exception.ConfigException;

public interface JMXConfigBeanInf {
	String getProperty(String propertyName) throws ConfigException;
	String setProperty(String propertyName, String propertyValue) throws ConfigException;
}
