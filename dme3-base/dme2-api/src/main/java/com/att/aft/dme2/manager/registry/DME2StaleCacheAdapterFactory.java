package com.att.aft.dme2.manager.registry;

public class DME2StaleCacheAdapterFactory {
  private DME2StaleCacheAdapterFactory() {

  }

  public static DME2StaleCacheAdapter getStaleCacheAdapter( DME2EndpointRegistry registry ) {
    if ( registry != null && registry instanceof DME2EndpointRegistryAdapter ) {
      return (DME2StaleCacheAdapter) registry;
    }
    throw new UnsupportedOperationException( "Registry does not support use of stale cache adapter" );
  }
}
