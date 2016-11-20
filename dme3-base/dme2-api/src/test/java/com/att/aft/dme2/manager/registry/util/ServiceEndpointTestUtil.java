/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry.util;

import java.util.Properties;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.registry.dto.StatusInfo;

public class ServiceEndpointTestUtil {
  private static final String DEFAULT_DME_JDBC_HEALTH_CHECK_USER = RandomStringUtils.randomAlphanumeric(10);
  private static final String DEFAULT_VERSION = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_DME_JDBC_DATABASE_NAME = RandomStringUtils.randomAlphanumeric( 15 );
  private static final String DEFAULT_HOST_ADDRESS = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_CONTEXT_PATH = RandomStringUtils.randomAlphanumeric( 10 );
  private static final XMLGregorianCalendar DEFAULT_EXPIRATION_TIME = null;
  private static final Properties DEFAULT_ADDITIONAL_PROPERTIES = null;
  private static final String DEFAULT_CLIENT_SUPPORTED_VERSIONS = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_CONTAINER_HOST = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_CONTAINER_NAME = RandomStringUtils.randomAlphabetic( 20 );
  private static final String DEFAULT_CONTAINER_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 25 );
  private static final String DEFAULT_CONTAINER_VERSION = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_CONTAINER_VERSION_DEFINITION_NAME = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_CREATED_BY = RandomStringUtils.randomAlphanumeric( 10 );
  private static final XMLGregorianCalendar DEFAULT_CREATED_TIMESTAMP = null;
  private static final String DEFAULT_DME_JDBC_HEALTH_CHECK_PASSWORD = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_DME_JDBC_HEALTH_CHECK_DRIVER = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_DME_VERSION = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_ENV = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_PID = RandomStringUtils.randomAlphanumeric( 10 );
  private static final String DEFAULT_PORT = Integer.toString( RandomUtils.nextInt( 1000 ) );
  private static final String DEFAULT_ROUTE_OFFER = RandomStringUtils.randomAlphanumeric( 10 );
  private static final StatusInfo DEFAULT_STATUS_INFO = null;
  private static final String DEFAULT_UPDATED_BY = RandomStringUtils.randomAlphanumeric( 10 );
  private static final XMLGregorianCalendar DEFAULT_UPDATED_TIMESTAMP = null;
  private static final String DEFAULT_LATITUDE = Double.toString( RandomUtils.nextDouble() );
  private static final String DEFAULT_LONGITUDE = Double.toString( RandomUtils.nextDouble() );
  private static final String DEFAULT_PROTOCOL = RandomStringUtils.randomAlphabetic( 5 );

  public static ServiceEndpoint createDefaultServiceEndpoint() {
    return createServiceEndpoint( DEFAULT_DME_JDBC_HEALTH_CHECK_USER, DEFAULT_VERSION, DEFAULT_DME_JDBC_DATABASE_NAME, DEFAULT_HOST_ADDRESS, DEFAULT_CONTEXT_PATH, DEFAULT_EXPIRATION_TIME, DEFAULT_ADDITIONAL_PROPERTIES, DEFAULT_CLIENT_SUPPORTED_VERSIONS, DEFAULT_CONTAINER_HOST,
        DEFAULT_CONTAINER_NAME, DEFAULT_CONTAINER_ROUTE_OFFER, DEFAULT_CONTAINER_VERSION, DEFAULT_CONTAINER_VERSION_DEFINITION_NAME, DEFAULT_CREATED_BY, DEFAULT_CREATED_TIMESTAMP, DEFAULT_DME_JDBC_HEALTH_CHECK_PASSWORD, DEFAULT_DME_JDBC_HEALTH_CHECK_DRIVER, DEFAULT_DME_VERSION, DEFAULT_ENV,
        DEFAULT_PID, DEFAULT_PORT, DEFAULT_ROUTE_OFFER, DEFAULT_STATUS_INFO, DEFAULT_UPDATED_BY, DEFAULT_UPDATED_TIMESTAMP,
        DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_PROTOCOL );
  }

  private static ServiceEndpoint createServiceEndpoint( String dmeJdbcHealthCheckUser, String version,
                                                        String dmeJdbcDatabaseName, String hostAddress,
                                                        String contextPath, XMLGregorianCalendar expirationTime,
                                                        Properties additionalProperties,
                                                        String clientSupportedVersions, String containerHost,
                                                        String containerName, String containerRouteOffer,
                                                        String containerVersion, String containerVersionDefinitionName,
                                                        String createdBy, XMLGregorianCalendar createdTimestamp,
                                                        String dmeJdbcHealthCheckPassword,
                                                        String dmeJdbcHealthCheckDriver, String dmeVersion, String env,
                                                        String pid, String port, String routeOffer,
                                                        StatusInfo statusInfo, String updatedBy,
                                                        XMLGregorianCalendar updatedTimestamp, String latitude,
                                                        String longitude, String protocol ) {
    ServiceEndpoint serviceEndpoint = new ServiceEndpoint();
    serviceEndpoint.setDmeJDBCHealthCheckUser( dmeJdbcHealthCheckUser );
    serviceEndpoint.setVersion( version );
    serviceEndpoint.setDmeJDBCDatabaseName( dmeJdbcDatabaseName );
    serviceEndpoint.setHostAddress( hostAddress );
    serviceEndpoint.setContextPath( contextPath );
    serviceEndpoint.setExpirationTime( expirationTime );
    serviceEndpoint.setAdditionalProperties( additionalProperties );
    serviceEndpoint.setClientSupportedVersions( clientSupportedVersions );
    serviceEndpoint.setContainerHost( containerHost );
    serviceEndpoint.setContainerName( containerName );
    serviceEndpoint.setContainerRouteOffer( containerRouteOffer );
    serviceEndpoint.setContainerVersion( containerVersion );
    serviceEndpoint.setContainerVersionDefinitionName( containerVersionDefinitionName );
    serviceEndpoint.setCreatedBy( createdBy );
    serviceEndpoint.setCreatedTimestamp( createdTimestamp );
    serviceEndpoint.setDmeJDBCHealthCheckPassword( dmeJdbcHealthCheckPassword );
    serviceEndpoint.setDmeJDBCHealthCheckDriver( dmeJdbcHealthCheckDriver );
    serviceEndpoint.setDmeVersion( dmeVersion );
    serviceEndpoint.setEnv( env );
    serviceEndpoint.setPid( pid );
    serviceEndpoint.setPort( port );
    serviceEndpoint.setRouteOffer( routeOffer );
    serviceEndpoint.setStatusInfo( statusInfo );
    serviceEndpoint.setUpdatedBy( updatedBy );
    serviceEndpoint.setUpdatedTimestamp( updatedTimestamp );
    serviceEndpoint.setLatitude( latitude );
    serviceEndpoint.setLongitude( longitude );
    serviceEndpoint.setProtocol( protocol );


    return serviceEndpoint;
  }
}
