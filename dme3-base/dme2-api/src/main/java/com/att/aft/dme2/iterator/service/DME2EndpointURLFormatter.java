package com.att.aft.dme2.iterator.service;

import com.att.aft.dme2.manager.registry.DME2Endpoint;

public interface DME2EndpointURLFormatter
{
	public String formatURL(DME2Endpoint endpoint);
}
