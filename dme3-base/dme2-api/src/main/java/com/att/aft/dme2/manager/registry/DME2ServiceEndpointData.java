package com.att.aft.dme2.manager.registry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DME2ServiceEndpointData implements Serializable {
  private static final long serialVersionUID = -1383341415412425070L;
  private long cacheTTL;
  private long lastQueried;
  private long expirationTime;
  private int emptyCacheRefreshAttemptCount = -1;

  private String serviceURI;

  private List<DME2Endpoint> endpointList = new ArrayList<DME2Endpoint>();

  @SuppressWarnings("unused")
  private DME2ServiceEndpointData() {

  }

  public DME2ServiceEndpointData( List<DME2Endpoint> endpointList, String serviceURI, long cacheTTL,
                                  long lastQueried ) {
    this.endpointList = endpointList;
    this.serviceURI = serviceURI;
    this.cacheTTL = cacheTTL;
    this.lastQueried = lastQueried;
    this.expirationTime = cacheTTL + System.currentTimeMillis();
  }

  public long getCacheTTL() {
    return cacheTTL;
  }

  public void setCacheTTL( long cacheTTL ) {
    this.cacheTTL = cacheTTL;
    this.expirationTime = cacheTTL + System.currentTimeMillis();
  }

  public long getLastQueried() {
    return lastQueried;
  }

  public void setLastQueried( long lastQueried ) {
    this.lastQueried = lastQueried;
  }

  public List<DME2Endpoint> getEndpointList() {
    return endpointList;
  }

  public void setEndpointList( List<DME2Endpoint> endpointList ) {
    this.endpointList = endpointList;
  }

  public String getServiceURI() {
    return serviceURI;
  }

  public void setServiceURI( String serviceURI ) {
    this.serviceURI = serviceURI;
  }

  public long getExpirationTime() {
    return expirationTime;
  }

  public int getEmptyCacheRefreshAttemptCount() {
    return emptyCacheRefreshAttemptCount;
  }

  public void setEmptyCacheRefreshAttemptCount( int emptyCacheRefreshAttemptCount ) {
    this.emptyCacheRefreshAttemptCount = emptyCacheRefreshAttemptCount;
  }
}

