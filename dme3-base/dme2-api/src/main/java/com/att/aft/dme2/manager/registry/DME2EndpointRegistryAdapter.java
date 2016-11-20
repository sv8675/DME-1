package com.att.aft.dme2.manager.registry;

/**
 * "Server-facing" interface for registry. The naming for this and the client-facing version should actually be
 * reversed, but clients have become used to "EndpointRegistry", so this one will become the adapter.  It is comprised
 * of both the stale and live cache versions, so it has both client and server-facing capabilities
 */
public interface DME2EndpointRegistryAdapter extends DME2EndpointRegistry, DME2StaleCacheAdapter {
}
