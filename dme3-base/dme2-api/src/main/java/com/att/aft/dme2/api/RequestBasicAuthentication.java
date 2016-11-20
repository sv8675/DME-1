/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.net.URI;

import org.eclipse.jetty.client.util.BasicAuthentication;

public class RequestBasicAuthentication extends BasicAuthentication
{
	public RequestBasicAuthentication(URI uri, String realm, String user, String password) {
		super(uri, realm, user, password);
	}

	//public DmeBasicAuthentication(Realm realm) throws IOException {
	//	super(realm);
	//}
}