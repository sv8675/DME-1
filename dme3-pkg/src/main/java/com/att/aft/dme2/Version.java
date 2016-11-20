/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class Version 
{
	private static String version = null;
	
	public static synchronized String getVersion() {
		if (version != null) {
			return version;
		}
        InputStream s = null;
        try {
        	s = Version.class.getResourceAsStream("/META-INF/maven/com.att.aft/dme2/pom.properties");
        	Properties p = new Properties();
        	p.load(s);
        	version = p.getProperty("version");
        	s.close();
        } catch (Exception e) {
        	
        }
        if (version == null) {
        	return "UNDETERMINED";
        } else {
            return version;
        }
	}
	
    public static void main( String[] args )
    {
    	Calendar toDay = Calendar.getInstance();
    	int YEAR = toDay.get(Calendar.YEAR);
        System.out.println("AT&T Tools and Frameworks - DME2 Runtime");
        System.out.println("Copyright " + YEAR + ", AT&T Intellectual Properties, Inc.");
        System.out.println("Version: " + getVersion());
        System.exit(0);
    }
}
