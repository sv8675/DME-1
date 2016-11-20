/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

//import org.eclipse.jetty.client.security.Realm;

public class DME2SimpleRealm { // implements Realm{

	private String userName;
	private String password;
	
	public DME2SimpleRealm(String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
	}
	
	//@Override
	public String getCredentials() {
		return this.password;
	}

	//@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	public String getPrincipal() {
		return this.userName;
	}
}
