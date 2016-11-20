package com.att.aft.dme2.manager.registry;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;

/**
 * DME2 Endpoint Registry Factory
 */

public class DME2EndpointRegistryFactory {
  private static DME2EndpointRegistryFactory factory = new DME2EndpointRegistryFactory();
  private static Map<MapKey,DME2EndpointRegistry> endpointRegistryMap = new ConcurrentHashMap<MapKey,DME2EndpointRegistry>();

  private DME2EndpointRegistryFactory() {

  }

  /**
   * Get factory instance
   * @return factory instance
   */
  public static DME2EndpointRegistryFactory getInstance() {
    return factory;
  }

  /**
   * Create an endpoint registry from the given properties
   *
   * @param containerName Container name
   * @param config DME2Configuration
   * @param type DME2 Endpoint Registry Type
   * @param managerName DME2 Manager name
   * @param props Additional Properties
   * @return DME2 Endpoint Registry
   * @throws DME2Exception
   */
  public DME2EndpointRegistry createEndpointRegistry( String containerName, DME2Configuration config, DME2EndpointRegistryType type, String managerName, Properties props )
      throws DME2Exception {
    if ( containerName == null ) {
      throw new UnsupportedOperationException( "Attempted to create endpoint registry with null container name" );
    }
    if ( type == null ) {
      throw new UnsupportedOperationException( "Attempted to create unknown DME endpoint registry with null type" );
    }
    MapKey mapKey = new MapKey( containerName, type );
    DME2EndpointRegistry endpointRegistry;
    switch ( type ) {
      case FileSystem:
        endpointRegistry = getEndpointRegistry( mapKey );
        if ( endpointRegistry == null ) {
          endpointRegistry = new DME2EndpointRegistryFS(config, managerName);
          storeEndpointRegistry( mapKey, endpointRegistry );
        }
        break;
      case GRM:
        endpointRegistry = getEndpointRegistry( mapKey );
        if ( endpointRegistry == null ) {
          endpointRegistry = new DME2EndpointRegistryGRM(config, managerName);
          storeEndpointRegistry( mapKey, endpointRegistry );
        }
        break;
      case Memory:
        endpointRegistry = getEndpointRegistry( mapKey );
        if ( endpointRegistry == null ) {
          endpointRegistry = new DME2EndpointRegistryMemory( config, managerName );
          storeEndpointRegistry( mapKey, endpointRegistry );
        }
        break;
      case GRMREST:
        endpointRegistry = getEndpointRegistry( mapKey );
        if ( endpointRegistry == null ) {
          //endpointRegistry = new DME2EndpointRegistryGRMREST( config, managerName );
          storeEndpointRegistry( mapKey, endpointRegistry );
        }
          break;
      default:
        throw new UnsupportedOperationException(
            "Attempted to create unknown DME endpoint registry with type " + type.toString() );
    }
    endpointRegistry.init( props );
    return endpointRegistry;
  }

  private synchronized void storeEndpointRegistry( MapKey mapKey, DME2EndpointRegistry registry ) {
    endpointRegistryMap.put( mapKey, registry );
  }

  private DME2EndpointRegistry getEndpointRegistry( MapKey key ) {
    return endpointRegistryMap.get( key );
  }

  class MapKey {
    private String containerName;
    private DME2EndpointRegistryType registryType;

    MapKey( String containerName, DME2EndpointRegistryType registryType ) {
      this.containerName = containerName;
      this.registryType = registryType;
    }

    @Override
    public boolean equals( Object o ) {
      if ( this == o ) {
        return true;
      }
      if ( o == null || getClass() != o.getClass() ) {
        return false;
      }

      MapKey mapKey = (MapKey) o;

      if ( !containerName.equals( mapKey.containerName ) ) {
        return false;
      }
      if ( registryType != mapKey.registryType ) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = containerName.hashCode();
      result = 31 * result + registryType.hashCode();
      return result;
    }
  }
}
