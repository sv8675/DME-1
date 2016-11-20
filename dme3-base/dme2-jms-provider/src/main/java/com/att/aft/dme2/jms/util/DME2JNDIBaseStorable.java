/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.Reference;

/**
 * Facilitates objects to be stored in JNDI as properties
 */

public abstract class DME2JNDIBaseStorable implements 
         DME2JNDIStorableInterface,Externalizable {

    private Properties properties;

    /**
     * Set the properties that will represent the instance in JNDI
     * 
     * @param props
     */
    protected abstract void buildFromProperties(Properties props);

    /**
     * Initialize the instance from properties stored in JNDI
     * 
     * @param props
     */

    protected abstract void populateProperties(Properties props);

    /**
     * set the properties for this instance as retrieved from JNDI
     * 
     * @param props
     */

    public synchronized void setProperties(Properties props) {
        this .properties = props;
        buildFromProperties(props);
    }

    /**
     * Get the properties from this instance for storing in JNDI
     * 
     * @return the properties
     */

    public synchronized Properties getProperties() {
        if (this .properties == null) {
            this .properties = new Properties();
        }
        populateProperties(this .properties);
        return this .properties;
    }

    /**
     * Retrive a Reference for this instance to store in JNDI
     * 
     * @return the built Reference
     * @throws NamingException if error on building Reference
     */
    public Reference getReference() throws NamingException {
        return DME2JNDIReferenceFactory.createReference(this.getClass()
                .getName(), this );
    }

    /**
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        Properties props = (Properties) in.readObject();
        if (props != null) {
            setProperties(props);
        }

    }

    /**
     * @param out
     * @throws IOException
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(getProperties());

    }

}