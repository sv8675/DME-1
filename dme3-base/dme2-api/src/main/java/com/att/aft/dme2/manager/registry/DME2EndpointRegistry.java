package com.att.aft.dme2.manager.registry;

import java.util.List;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.scld.grm.types.v1.ClientJVMInstance;

/**
 * Client-facing DME2 Endpoint Registry Interface (Should really be DME2EndpointRegistryAdapter)
 */
public interface DME2EndpointRegistry {


  /**
   * Publish an endpoint based upon the criteria provided
   *
   * @param service Service name
   * @param path Service path
   * @param host Service host
   * @param port Service port
   * @param latitude Service location latitude
   * @param longitude Service location longitude
   * @param protocol Service access protocol
   * @throws DME2Exception
   */
  public void publish(String service, String path,String host, int port, double latitude, double longitude, String protocol) throws
      DME2Exception;

  /**
   * Publish an endpoint based upon the criteria provided
   *
   * @param service Service name
   * @param path Service path
   * @param host Service host
   * @param port Service port
   * @param latitude Service location latitude
   * @param longitude Service location longitude
   * @param protocol Service access protocol
   * @param updateLease Whether to renew the service lease
   * @throws DME2Exception
   */
  public void publish(String service, String path,String host, int port, double latitude, double longitude, String protocol, boolean updateLease) throws
      DME2Exception;

  /**
   * Publish an endpoint based upon the criteria provided
   *
   * @param service Service name
   * @param path Service path
   * @param host Service host
   * @param port Service port
   * @param protocol Service access protocol
   * @param props Service Properties
   * @throws DME2Exception
   */
  public void publish(String service, String path,String host,int port, String protocol, Properties props) throws
      DME2Exception;

  /**
   * Publish an endpoint based upon the criteria provided
   *
   * @param service Service name
   * @param path Service path
   * @param host Service host
   * @param port Service port
   * @param protocol Service access protocol
   * @throws DME2Exception
   */
  public void publish(String service, String path,String host,int port, String protocol) throws DME2Exception;

  /**
   * Publish an endpoint based upon the criteria provided
   *
   * @param service Service name
   * @param path Service path
   * @param host Service host
   * @param port Service port
   * @param protocol Service access protocol
   * @param updateLease Whether to renew the service lease
   * @throws DME2Exception
   */
  public void publish(String service, String path,String host,int port, String protocol, boolean updateLease) throws
      DME2Exception;

  /**
   * Unpublish (remove) an endpoint
   * @param serviceName Service name
   * @param host Service host
   * @param port Service port
   * @throws DME2Exception
   */
  public void unpublish(String serviceName, String host, int port) throws DME2Exception;

  /**
   * Find endpoints associated with the given criteria
   *
   * @param serviceName Service name
   * @param serviceVersion Service version
   * @param envContext Environment context
   * @param routeOffer Route offer
   * @return List of endpoints
   * @throws DME2Exception
   */
  public List<DME2Endpoint> findEndpoints( String serviceName, String serviceVersion, String envContext, String routeOffer ) throws
      DME2Exception;

  /**
   * Retrieves the route info associated with the given criteria
   *
   * @param serviceName Service name
   * @param serviceVersion Service version
   * @param envContext Environment context
   * @return Route Info
   * @throws DME2Exception
   */
  public DME2RouteInfo getRouteInfo(String serviceName, String serviceVersion, String envContext) throws DME2Exception;

  /**
   * Set or renew the lease on an endpoint.
   *
   * @param endpoint the endpoint to lease
   * @throws DME2Exception
   */
  public void lease(DME2Endpoint endpoint) throws DME2Exception;

  /**
   * Refresh cached data stored by the endpoint registry implementation
   */
  public void refresh();

  /**
   * Shutdown tasks
   */
  public void shutdown();

  /**
   * Performs the explicit post-constructor initialization (or implicit construct initialization) of the Endpoint
   * Registry based upon the given Properties.
   *
   * @param properties Properties to use in initialization
   * @throws DME2Exception
   */
  public void init(Properties properties) throws DME2Exception;

  public double[] getDistanceBands();

  /**
   * Publish service URL
   * @param serviceURI Full service path (/service=X/envContext=Y/etc.)
   * @param contextPath Servlet prefix
   * @param hostAddress host address
   * @param port host port
   * @param latitude service lat
   * @param longitude service long
   * @param protocol url protocol
   * @param props service properties
   * @param updateLease unused
   * @throws DME2Exception
   */
  public void publish(String serviceURI, String contextPath, String hostAddress, int port, double latitude, double longitude, String protocol, Properties props, boolean updateLease) throws DME2Exception;

  /**
   * 
   * @param serviceKey
   * @param version
   * @param env
   * @param routeOffer
   * @return
   * @throws DME2Exception
   */
  public DME2Endpoint[] find( String serviceKey, String version, String env, String routeOffer) throws DME2Exception;

  public void registerJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception;
  public void updateJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception;
  public void deregisterJVM(String envContext, ClientJVMInstance instanceInfo) throws DME2Exception;
  public List<ClientJVMInstance> findRegisteredJVM(String envContext, Boolean activeOnly, String hostAddress, String mechID, String processID) throws DME2Exception;
}
