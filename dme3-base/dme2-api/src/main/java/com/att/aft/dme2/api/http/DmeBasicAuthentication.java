/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.http;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jetty.client.util.BasicAuthentication;

import com.att.aft.dme2.api.SimpleRealm;

public class DmeBasicAuthentication extends BasicAuthentication
{
	public DmeBasicAuthentication(URI uri, String realm, String user, String password) {
		super(uri, realm, user, password);
	}

	public DmeBasicAuthentication(SimpleRealm realm) throws IOException {
		super(null, realm.getPrincipal(), realm.getId(), realm.getCredentials());
	}

}
