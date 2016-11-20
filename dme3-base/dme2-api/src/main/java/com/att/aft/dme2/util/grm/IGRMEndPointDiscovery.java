package com.att.aft.dme2.util.grm;
import java.util.List;

import com.att.aft.dme2.api.DME2Exception;

/**
 * This Interface will be implemented by any class that be used to get list of GRM Server Endpoint addresses these classes can be injected into GRMServiceAccessor so it can use
 * them to get list of GRMServers the implementations may include:
 * <ul>
 * <li>{@link com.att.aft.dme2.registry.accessor.GRMEndPointsDiscoveryAFT},
 * <li>{@link com.att.aft.dme2.registry.accessor.GRMEndPointsDiscoveryDNS}
 * <li>{@link com.att.aft.dme2.registry.accessor.GRMEndPointsCache}
 * <li>{@link com.att.aft.dme2.registry.accessor.GRMEndPointsDiscoveryHelperDNS}
 * </ul>
 * not all above can be injected in GRMServiceAccessor, some are returning partial list, some need special building process so this complex building process is handled by
 * {@link GRMServiceAccessorFactory}
 * 
 * @author ar671m
 *
 */
public interface IGRMEndPointDiscovery {
    public List<String> getGRMEndpoints() throws DME2Exception;
    public void close();
}
