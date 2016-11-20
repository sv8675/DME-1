/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

//import org.eclipse.jetty.client.security.Realm;

public class SimpleRealm { //implements Realm {

	private String userName;
	private String password;
	private String realmName;
	public SimpleRealm(String realmName, String userName, String password) {
		super();
		this.realmName = realmName;
		this.userName = userName;
		this.password = password;
	}
	
	public String getCredentials() {
		return this.password;
	}
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPrincipal() {
		return this.userName;
	}
	
	public String getRealmName() {
		return realmName;
	}
}