/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class DME2EndpointTestUtil {
  private static final String DEFAULT_CONTEXT_PATH = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 15 );
  private static final double DEFAULT_LATITUDE = RandomUtils.nextDouble();
  private static final long DEFAULT_LEASE = RandomUtils.nextLong();
  private static final double DEFAULT_LONGITUDE = RandomUtils.nextDouble();
  private static final String DEFAULT_PATH = RandomStringUtils.randomAlphanumeric( 30 );
  private static final int DEFAULT_PORT = RandomUtils.nextInt();
  private static final String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphanumeric( 5 );
  private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 25 );
  private static final String DEFAULT_SERVICE_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_SIMPLE_NAME = RandomStringUtils.randomAlphanumeric( 10 );

  public static DME2Endpoint createDefaultDME2Endpoint() {
    DME2Endpoint endpoint = new DME2Endpoint( RandomUtils.nextDouble());
    endpoint.setContextPath( DEFAULT_CONTEXT_PATH );
    endpoint.setEnvContext( DEFAULT_ENV_CONTEXT );
    endpoint.setHost( DEFAULT_HOST );
    endpoint.setLatitude( DEFAULT_LATITUDE );
    endpoint.setLease( DEFAULT_LEASE );
    endpoint.setLongitude( DEFAULT_LONGITUDE );
    endpoint.setPath( DEFAULT_PATH );
    endpoint.setPort( DEFAULT_PORT );
    endpoint.setProtocol( DEFAULT_PROTOCOL );
    endpoint.setRouteOffer( DEFAULT_ROUTE_OFFER );
    endpoint.setServiceName( DEFAULT_SERVICE_NAME );
    endpoint.setSimpleName( DEFAULT_SIMPLE_NAME );
    return endpoint;
  }

  public static List<DME2Endpoint> createDefaultDME2EndpointList() {
    List<DME2Endpoint> endpoints = new ArrayList<DME2Endpoint>();
    endpoints.add( createDefaultDME2Endpoint() );
    return endpoints;
  }

  public static Boolean compare( List<DME2Endpoint> actualEndpoints, List<DME2Endpoint> expectedEndpoints ) {
    if ( actualEndpoints == null ) {
      return ( expectedEndpoints == null );
    } else if ( expectedEndpoints == null ) {
      return false;
    }
    if ( actualEndpoints.size() != expectedEndpoints.size() ) {
      return false;
    }

    List<Integer> matchedIndexes = new ArrayList<Integer>();
    for ( DME2Endpoint actualEndpoint : actualEndpoints ) {
      boolean foundMatch = false;
      for ( int i = 0; i < expectedEndpoints.size(); i++ ) {
        if ( matchedIndexes.contains( i ) ) {
          continue;
        }
        if ( DME2EndpointTestUtil.compare( actualEndpoint, expectedEndpoints.get( i ) ) ) {
          matchedIndexes.add( i );
          foundMatch = true;
          break;
        }
      }
      if ( !foundMatch ) {
        return false;
      }
    }

    return matchedIndexes.size() == expectedEndpoints.size();
  }

  private static boolean compare( DME2Endpoint actualEndpoint, DME2Endpoint expectedEndpoint ) {
    if ( actualEndpoint == null ) {
      return ( expectedEndpoint == null );
    } else if ( expectedEndpoint == null ) {
      return false;
    } else {
      return Objects.equals( actualEndpoint.getLatitude(), expectedEndpoint.getLatitude() ) &&
          Objects.equals( actualEndpoint.getCached(), expectedEndpoint.getCached() ) &&
          Objects.equals( actualEndpoint.getContextPath(), expectedEndpoint.getContextPath() ) &&
          Objects.equals( actualEndpoint.getEndpointProperties(), expectedEndpoint.getEndpointProperties() ) &&
          Objects.equals( actualEndpoint.getHost(), expectedEndpoint.getHost() ) &&
          Objects.equals( actualEndpoint.getLease(), expectedEndpoint.getLease() ) &&
          Objects.equals( actualEndpoint.getLongitude(), expectedEndpoint.getLongitude() ) &&
          Objects.equals( actualEndpoint.getPath(), expectedEndpoint.getPath() ) &&
          Objects.equals( actualEndpoint.getProtocol(), expectedEndpoint.getProtocol() ) &&
          Objects.equals( actualEndpoint.getRouteOffer(), expectedEndpoint.getRouteOffer() ) &&
          Objects.equals( actualEndpoint.getPort(), expectedEndpoint.getPort() ) &&
          Objects.equals( actualEndpoint.getSimpleName(), expectedEndpoint.getSimpleName() ) &&
          Objects.equals( actualEndpoint.getServiceName(), expectedEndpoint.getServiceName() );
    }
  }

}
