package com.att.aft.dme2.registry.bootstrap;

import java.util.List;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.registry.dto.GRMEndpoint;

public class DNSRegistryBootstrap implements RegistryBootstrap {

	private DME2Configuration config;

	public DNSRegistryBootstrap(DME2Configuration configuration) {
		this.config = configuration;
	}
	
	public List<GRMEndpoint> getGRMEndpoints(String... urls) throws DME2Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
