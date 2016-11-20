/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * Converts objects implementing JNDIStorable into a property fields so they can
 * be stored and regenerated from JNDI
 */
public class DME2JNDIReferenceFactory implements ObjectFactory {

	private static final Logger logger = LoggerFactory.getLogger(DME2JNDIReferenceFactory.class.getName());

	/**
	 * This will be called by a JNDIprovider when a Reference is retrieved from
	 * a JNDI store - and generates the orignal instance
	 * 
	 * @param object
	 *            the Reference object
	 * @param name
	 *            the JNDI name
	 * @param nameCtx
	 *            the context
	 * @param environment
	 *            the environment settings used by JNDI
	 * @return the instance built from the Reference object
	 * @throws Exception
	 *             if building the instance from Reference fails (usually class
	 *             not found)
	 */
	public Object getObjectInstance(Object object, Name name, Context nameCtx, Hashtable<?, ?> environment)
			throws Exception {
		Object result = null;
		if (object instanceof Reference) {
			Reference reference = (Reference) object;
			logger.info(null, "getObjectInstance", "Getting instance of " + reference.getClassName());

			Class<?> theClass = loadClass(this, reference.getClassName());
			if (DME2JNDIStorableInterface.class.isAssignableFrom(theClass)) {

				DME2JNDIStorableInterface store = (DME2JNDIStorableInterface) theClass.newInstance();
				Properties properties = new Properties();
				for (Enumeration<?> iter = reference.getAll(); iter.hasMoreElements();) {

					StringRefAddr addr = (StringRefAddr) iter.nextElement();
					properties.put(addr.getType(), (addr.getContent() == null) ? "" : addr.getContent());

				}
				store.setProperties(properties);
				result = store;
			}
		} else {
			logger.error(null, "getObjectInstance", "Object " + object + " is not a reference - cannot load");
			throw new RuntimeException("Object " + object + " is not a reference");
		}
		return result;
	}

	/**
	 * Create a Reference instance from a JNDIStorable object
	 * 
	 * @param instanceClassName
	 * @param po
	 * @return
	 * @throws NamingException
	 */

	public static Reference createReference(String instanceClassName, DME2JNDIStorableInterface po)
			throws NamingException {
		logger.info(null, "createReference", "Creating reference: " + instanceClassName + "," + po);
		Reference result = new Reference(instanceClassName, DME2JNDIReferenceFactory.class.getName(), null);
		try {
			Properties props = po.getProperties();
			for (Enumeration<?> iter = props.propertyNames(); iter.hasMoreElements();) {
				String key = (String) iter.nextElement();
				String value = props.getProperty(key);
				javax.naming.StringRefAddr addr = new javax.naming.StringRefAddr(key, value);
				result.add(addr);
			}
		} catch (Exception e) {
			logger.error(null, "createReference", e.getMessage());
			throw new NamingException(e.getMessage());
		}
		return result;
	}

	/**
	 * Retrieve the class loader for a named class
	 * 
	 * @param thisObj
	 * @param className
	 * @return
	 * @throws ClassNotFoundException
	 */

	public static Class<?> loadClass(Object thisObj, String className) throws ClassNotFoundException {
		// tryu local ClassLoader first.
		ClassLoader loader = thisObj.getClass().getClassLoader();
		Class<?> theClass;
		if (loader != null) {
			theClass = loader.loadClass(className);
		} else {
			// Will be null in jdk1.1.8
			// use default classLoader
			theClass = Class.forName(className);
		}
		return theClass;
	}

}
