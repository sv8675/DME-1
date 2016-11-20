/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import java.util.Properties;

import javax.naming.Referenceable;

/**
 * Faciliates objects to be stored in JNDI as properties
 */

public interface DME2JNDIStorableInterface extends Referenceable {

    /**
     * set the properties for this instance as retrieved from JNDI
     *
     * @param properties
     */

    void setProperties(Properties properties);

    /**
     * Get the properties from this instance for storing in JNDI
     *
     * @return
     */

    Properties getProperties();

}