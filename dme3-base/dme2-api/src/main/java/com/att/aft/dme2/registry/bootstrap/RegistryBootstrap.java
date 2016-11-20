package com.att.aft.dme2.registry.bootstrap;

import java.util.List;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.registry.dto.GRMEndpoint;

public interface RegistryBootstrap {
	public abstract List<GRMEndpoint> getGRMEndpoints(String... urls) throws DME2Exception;
}
