/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.iterator.test.util;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2URIUtils;


public class DME2EndpointTestUtil {

    public DME2EndpointTestUtil() {
		// TODO Auto-generated constructor stub
	}

	
    public static DME2Endpoint createDefaultDME2Endpoint(String serviceName,String context,String extraContext,String queryString,String host, int port, String path, String protocol) {
		  String DEFAULT_CONTEXT_PATH = RandomStringUtils.randomAlphanumeric( 20 );
		  String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 10 );
		  String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 15 );
		  String DEFAULT_SERVICE_VERSION = RandomStringUtils.randomAlphanumeric( 5 );
		  double DEFAULT_LATITUDE = RandomUtils.nextDouble();
		  long DEFAULT_LEASE = RandomUtils.nextLong();
		  double DEFAULT_LONGITUDE = RandomUtils.nextDouble();
		  String DEFAULT_PATH = RandomStringUtils.randomAlphanumeric( 30 );
		  int DEFAULT_PORT = RandomUtils.nextInt();	
		  String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphanumeric( 5 );
		  String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 25 );
		  String DEFAULT_SERVICE_NAME = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", DEFAULT_SERVICE_VERSION, "LAB", "A1");
		  String DEFAULT_SIMPLE_NAME = RandomStringUtils.randomAlphanumeric( 10 );

		  
		  DME2Endpoint endpoint = new DME2Endpoint( RandomUtils.nextDouble() );
		    endpoint.setContextPath( context );
		    endpoint.setEnvContext( extraContext );
		    endpoint.setHost( host );
		    endpoint.setLatitude( DEFAULT_LATITUDE );
		    endpoint.setLease( DEFAULT_LEASE );
		    endpoint.setLongitude( DEFAULT_LONGITUDE );
		    endpoint.setPath( path );
		    endpoint.setPort( port );
		    endpoint.setProtocol( protocol );
		    endpoint.setRouteOffer( DEFAULT_ROUTE_OFFER );
		    endpoint.setServiceName( serviceName==null?DEFAULT_SERVICE_NAME:serviceName );
		    endpoint.setSimpleName( DEFAULT_SIMPLE_NAME );
		    return endpoint;
		  }   
    
    public static DME2Endpoint createDefaultDME2Endpoint(String serviceName) {
		  String DEFAULT_CONTEXT_PATH = RandomStringUtils.randomAlphanumeric( 20 );
		  String DEFAULT_ENV_CONTEXT = RandomStringUtils.randomAlphanumeric( 10 );
		  String DEFAULT_HOST = RandomStringUtils.randomAlphanumeric( 15 );
		  String DEFAULT_SERVICE_VERSION = RandomStringUtils.randomAlphanumeric( 5 );
		  double DEFAULT_LATITUDE = RandomUtils.nextDouble();
		  long DEFAULT_LEASE = RandomUtils.nextLong();
		  double DEFAULT_LONGITUDE = RandomUtils.nextDouble();
		  String DEFAULT_PATH = RandomStringUtils.randomAlphanumeric( 30 );
		  int DEFAULT_PORT = RandomUtils.nextInt();
		  String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphanumeric( 5 );
		  String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 25 );
		  String DEFAULT_SERVICE_NAME = DME2URIUtils.buildServiceURIString("com.att.aft.dme2.test.TestDME2EndpointIterator_ResolveEndpoints", DEFAULT_SERVICE_VERSION, "LAB", "A1");
		  String DEFAULT_SIMPLE_NAME = RandomStringUtils.randomAlphanumeric( 10 );

		  
		  DME2Endpoint endpoint = new DME2Endpoint( RandomUtils.nextDouble() );
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
		    endpoint.setServiceName( serviceName==null?DEFAULT_SERVICE_NAME:serviceName );
		    endpoint.setSimpleName( DEFAULT_SIMPLE_NAME );
		    return endpoint;
		  }
	  
	  public static DME2Endpoint[] createDefaultDME2Endpoints(int size) {
		  
		  DME2Endpoint[] endpoints = new DME2Endpoint[size];
		  DME2Endpoint endpoint = null;
		  
		  for(int i=0;i<size;i++)
		  {
			  endpoint = createDefaultDME2Endpoint((String)null);
			  endpoints[i] = endpoint;
		  }
		  
		  return endpoints;
	  
	  }
	  public static DME2Endpoint[] createDefaultDME2Endpoints(int size,String context,String extraContext,String queryString,String host, int port, String path, String protocol) {
		  
		  DME2Endpoint[] endpoints = new DME2Endpoint[size];
		  DME2Endpoint endpoint = null;
		  
		  for(int i=0;i<size;i++)
		  {
			  if(i==size-2){
				  endpoint = createDefaultDME2Endpoint((String)null,context,extraContext,queryString, host, port, path, protocol);
			  }else{
				  endpoint = createDefaultDME2Endpoint((String)null);
			  }
			  endpoints[i] = endpoint;
		  }
		  
		  return endpoints;
	  
	  }
	  
	  
	  public static DME2Endpoint createDefaultDME2Endpoint(DmeUniformResource uniformResource) {
		  String serviceName = null;
		  DME2Endpoint endpoint = createDefaultDME2Endpoint(serviceName);
		  endpoint.setDmeUniformResource(uniformResource);
		  return endpoint;
		 }
}
