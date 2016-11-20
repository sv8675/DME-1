/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jetty.client.util.BasicAuthentication;

public class DME2BasicAuthentication extends BasicAuthentication
{

	/*@Deprecated
	public DME2BasicAuthentication(Realm realm) throws IOException
	{
		super(realm);
	}*/

	public DME2BasicAuthentication(URI uri, String realm, String user, String password) throws IOException
	{
		super(uri, realm, user, password);
	}
	
}
