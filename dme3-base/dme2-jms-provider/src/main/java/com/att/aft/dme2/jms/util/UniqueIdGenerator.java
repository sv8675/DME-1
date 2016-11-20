/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.server.UID;
 
public class UniqueIdGenerator 
{
	private static final int CONSTANT_50=50;
	// generate and return a unique id for this host.
	public static URI getUniqueTemporaryQueueURI()
	{
		UID uid = new UID();
		try 
		{
			URI uri = new URI( "http://DME2LOCAL/" + uid.toString());
			return uri;
		} 
		catch (URISyntaxException e) 
		{
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args)
	{
		for(int i=0; i<CONSTANT_50; i++)
		{
			System.out.println(UniqueIdGenerator.getUniqueTemporaryQueueURI());
		}
	}
}
