package com.att.aft.dme2.registry.accessor;

import java.util.List;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;
import com.att.scld.grm.types.v1.ClientJVMInstance;
import com.att.scld.grm.v1.FindClientJVMInstanceRequest;
import com.att.scld.grm.v1.RegisterClientJVMInstanceRequest;

public interface BaseAccessor {
	public void addServiceEndPoint( ServiceEndpoint req ) throws DME2Exception;
	public void updateServiceEndPoint( ServiceEndpoint req ) throws DME2Exception;
	public void deleteServiceEndPoint( ServiceEndpoint req ) throws DME2Exception;
	public List<ServiceEndpoint> findRunningServiceEndPoint( ServiceEndpoint req ) throws DME2Exception;
	public String getRouteInfo( ServiceEndpoint req ) throws DME2Exception;
	public void registerClientJVMInstance(RegisterClientJVMInstanceRequest req) throws DME2Exception;
	public List<ClientJVMInstance> findClientJVMInstance(FindClientJVMInstanceRequest req) throws DME2Exception;
	
	public IGRMEndPointDiscovery getGrmEndPointDiscovery ();
}