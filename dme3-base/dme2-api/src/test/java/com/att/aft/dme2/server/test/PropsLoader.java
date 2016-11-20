/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The Class PropsLoader.
 */
public class PropsLoader {

	/**
	 * Gets the properties.
	 * 
	 * @param filePath
	 *            the file path
	 * @return the properties
	 */
	public static Properties getProperties(String filePath) {
		File f = new File(filePath);
		if (!f.exists()) {
			// Try loading from Classpath
			return getPropertiesFromStream(filePath);
		}

		try {
			FileInputStream inputStream = new FileInputStream(f);
			Properties props = new Properties();
			props.load(inputStream);
			inputStream.close();
			return props;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Properties getPropertiesFromStream(String filePath) {
		try {
			InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(filePath);
			//FileInputStream inputStream = new FileInputStream(f);
			Properties props = new Properties();
			props.load(inputStream);
			inputStream.close();
			return props;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * Prints the properties.
	 * 
	 * @param props
	 *            the props
	 */
	public static void printProperties(Properties props) {
		Enumeration e = props.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = props.getProperty(key);
			System.out.println(key + "=" + value);
		}

	}

	/**
	 * Sets the properties.
	 * 
	 * @param props
	 *            the new properties
	 */
	public static void setProperties(Properties props) {
		Enumeration e = props.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = props.getProperty(key);
			System.setProperty(key, value);
			// System.out.println("Set system property " + key + "=" + value);
		}
	}

}
