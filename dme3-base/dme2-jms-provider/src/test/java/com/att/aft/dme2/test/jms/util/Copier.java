/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.test.jms.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


/**
 * The Class Copier.
 */
public class Copier
{
	
	/** The dir. */
	private static File dir;
	
	/**
	 * Copy routing file.
	 *
	 * @param path the path
	 * @param serviceName the service name
	 */
	public static void copyRoutingFile(String path, String serviceName)
	{
		dir = new File(System.getProperty("AFT_DME2_EP_REGISTRY_FS_DIR", "dme2-fs-registry"));
		if(! dir.exists())
			dir.mkdirs();
		
		String[] toks = path.split("/");
		String service = null;
		String version = null;
		String envContext = null;
		service = toks[0];
		version = toks[1];
		envContext = toks[2];

		File targetFile = new File(dir,service + "/" + version + "/" + envContext);
		if(! targetFile.exists())
			targetFile.mkdirs();
		targetFile = new File(dir, service + "/" +version + "/" + envContext +"/routeInfo.xml");
		if(targetFile.exists())
		{
			targetFile.delete();
		}
		
		File currDir = new File(System.getProperty("user.dir"));
		String configFile = currDir.getAbsolutePath() + "/src/test/etc/svc_config/" + serviceName + ".xml";
		File fileToCopy = new File(configFile);
		
		try
        {
	        BufferedReader reader = new BufferedReader(new FileReader(fileToCopy));
	        BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile));
	        String line = null;
	        while( (line = reader.readLine()) != null )
	        {
	        	writer.write(line);
	        }
	        reader.close();
	        writer.flush();
	        writer.close();
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        	throw new RuntimeException(e);
        }
	}
}
