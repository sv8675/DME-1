package com.att.aft.dme2.manager.registry;

import java.util.Set;

public interface DME2StaleCacheAdapter {
  public Long getEndpointExpirationTime( String url );
  public Boolean isEndpointStale( String url );
  public void removeStaleEndpoint( String url );
  public void addStaleEndpoint( String url, Long expirationTime );
  public void clearStaleEndpoints();
  public Set<String> getStaleEndpoints();

  public Long getRouteOfferExpirationTime( String url );
  public Boolean isRouteOfferStale( String url );
  public void removeStaleRouteOffer( String url );
  public void addStaleRouteOffer( String url, Long expirationTime );
  public void clearStaleRouteOffers();
  public Set<String> getStaleRouteOffers();
}
