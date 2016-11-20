package com.att.aft.dme2.iterator;

import java.util.Iterator;

import com.att.aft.dme2.iterator.domain.DME2EndpointReference;

public interface DME2BaseEndpointIterator extends Iterator<DME2EndpointReference> {

	void setRouteOffersTried(String str);

	String getRouteOffersTried();
}