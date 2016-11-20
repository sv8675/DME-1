/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class DME2ThrottleServletResponseWrapper extends HttpServletResponseWrapper {
	private String partnerName;

	public DME2ThrottleServletResponseWrapper(HttpServletResponse response, String partnerName) throws IOException {
		super(response);
		this.partnerName = partnerName;
	}

	public String getPartnerName() {
		return partnerName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((partnerName == null) ? 0 : partnerName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DME2ThrottleServletResponseWrapper other = (DME2ThrottleServletResponseWrapper) obj;
		if (partnerName == null) {
			if (other.partnerName != null)
				return false;
		} else if (!partnerName.equals(other.partnerName))
			return false;
		return true;
	}
	
	

}
