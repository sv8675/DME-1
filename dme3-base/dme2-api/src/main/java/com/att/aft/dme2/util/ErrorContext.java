package com.att.aft.dme2.util;

import java.util.HashMap;

/**
 * Extends Map to allow repeated method calls to add()
 */
public class ErrorContext extends HashMap<String,String>{

	private static final long serialVersionUID = 1L;
	
	public ErrorContext add(String key, String value) {
		super.put(key, value);
		return this;
	}


}
