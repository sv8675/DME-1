package com.att.aft.dme2.manager.registry.util;

import java.util.Calendar;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.manager.registry.DME2JDBCEndpoint;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;
import com.att.aft.dme2.util.XMLGregorianCalendarConverter;
import com.att.scld.grm.types.v1.ServiceEndPoint;

/**
 * Utility class for DME2 Endpoint conversion
 */
public class DME2EndpointUtil {

  private DME2EndpointUtil() {

  }

  /**
   * Converts a a Service Endpoint, a Service URI and an expiration time to a DME2 Endpoint
   * @param serviceEndpoint Input GRM ServiceEndpoint
   * @param serviceURI Input Service URI
   * @param expireTime Input Expiration Time
   * @return DME2 Endpoint
   */
  public static DME2Endpoint convertEndpoint( ServiceEndpoint serviceEndpoint, String serviceURI, Long expireTime, double clientLatitude, double clientLongitude ) {
    if ( serviceEndpoint == null ) {
      return null;
    }

    DME2Endpoint endpoint;

    double epLat = Double.valueOf( serviceEndpoint.getLatitude() );
    double epLong = Double.valueOf( serviceEndpoint.getLongitude() );
    double distance = DME2DistanceUtil.calculateDistanceBetween( clientLatitude, clientLongitude, epLat, epLong );

    if ( DME2Protocol.DME2JDBC.equalsIgnoreCase( serviceEndpoint.getProtocol() )) {
      endpoint = new DME2JDBCEndpoint( distance );
      ( (DME2JDBCEndpoint) endpoint ).setDatabaseName( serviceEndpoint.getDmeJDBCDatabaseName() );
      ( (DME2JDBCEndpoint) endpoint ).setHealthCheckUser( serviceEndpoint.getDmeJDBCHealthCheckUser() );
      ( (DME2JDBCEndpoint) endpoint ).setHealthCheckPassword( serviceEndpoint.getDmeJDBCHealthCheckPassword() );
      ( (DME2JDBCEndpoint) endpoint ).setHealthCheckDriver( serviceEndpoint.getDmeJDBCHealthCheckDriver() );
    } else {
      endpoint = new DME2Endpoint( distance );
      endpoint.setContextPath( serviceEndpoint.getContextPath() );
    }

    endpoint.setServiceName( serviceURI );
    if (null != serviceEndpoint.getContextPath()) {
    	endpoint.setPath( serviceEndpoint.getContextPath() );
    } else {
    	endpoint.setPath("");
    }
    endpoint.setHost( serviceEndpoint.getHostAddress() );
    endpoint.setPort( Integer.valueOf( serviceEndpoint.getPort() ) );
    endpoint.setLatitude( Double.valueOf( serviceEndpoint.getLatitude() ) );
    endpoint.setLongitude( Double.valueOf( serviceEndpoint.getLongitude() ) );
    endpoint.setLease( expireTime );
    endpoint.setProtocol( serviceEndpoint.getProtocol().toUpperCase() );
    //endpoint.setDME2Version( serviceEndpoint.getDME2Version() );


    endpoint.setRouteOffer( serviceEndpoint.getRouteOffer() );
    //endpoint.setEndpointProperties( DME2Utils.convertNameValuePairToProperties( serviceEndpoint.getProperties() ) );
    //endpoint.setEndpointProperties( convertEndpointProperties(serviceEndpoint.getAdditionalProperties()) );
    endpoint.setEndpointProperties( serviceEndpoint.getAdditionalProperties() );
    endpoint.setSimpleName( serviceEndpoint.getName() );
/*

    int lmajorVersion = serviceEndpoint.getVersion().getMajor();
    int lminorVersion = serviceEndpoint.getVersion().getMinor();
    String lpatchVersion = serviceEndpoint.getVersion().getPatch();
*/

//    endpoint.setServiceVersion( lmajorVersion + "." + lminorVersion + "." + lpatchVersion );

    endpoint.setServiceVersion( serviceEndpoint.getVersion() );
    return endpoint;
  }

  /**
   * Build a DME2 Endpoint
   * @param uniformResource Uniform Resource
   * @param contextPath Context Path
   * @param env Environment
   * @param host Host
   * @param port Port
   * @param protocol Protocol
   * @param latitude Lat
   * @param longitude Long
   * @param props Additional properties
   * @return DME2 Endpoint
   */
  public static DME2Endpoint buildDME2Endpoint( double clientLatitude, double clientLongitude, DmeUniformResource uniformResource, String contextPath, String env,
                                                String host, int port, String protocol, double latitude,
                                                double longitude,
                                                Properties props ) {
    double distance = DME2DistanceUtil.calculateDistanceBetween( clientLatitude, clientLongitude, latitude, longitude );

    DME2Endpoint endpoint;
    if ( protocol.equalsIgnoreCase( DME2Protocol.DME2JDBC ) ) {
      endpoint = new DME2JDBCEndpoint( distance );
      if ( props != null ) {
        if ( props.containsKey( DME2Constants.KEY_DME2_JDBC_DATABASE_NAME ) ) {
          ( (DME2JDBCEndpoint) endpoint )
              .setDatabaseName( (String) props.get( DME2Constants.KEY_DME2_JDBC_DATABASE_NAME ) );
        }

        if ( props.containsKey( DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER ) ) {
          ( (DME2JDBCEndpoint) endpoint )
              .setHealthCheckUser( (String) props.get( DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_USER ) );
        }

        if ( props.containsKey( DME2Constants.KEY_DME2_JDBC_DATABASE_NAME ) ) {
          ( (DME2JDBCEndpoint) endpoint )
              .setHealthCheckPassword( (String) props.get( DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_PASSWORD ) );
        }

        if ( props.containsKey( DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER ) ) {
          ( (DME2JDBCEndpoint) endpoint )
              .setHealthCheckDriver( (String) props.get( DME2Constants.KEY_DME2_JDBC_HEALTHCHECK_DRIVER ) );
        }
      }
    } else {
      endpoint = new DME2Endpoint( distance );
    }

    endpoint.setContextPath( contextPath );
    endpoint.setSimpleName( uniformResource.getService() );
    endpoint.setServiceName( uniformResource.getService() );
    endpoint.setEnvContext( env );
    endpoint.setHost( host );
    endpoint.setPort( port );
    endpoint.setPath( contextPath );
    endpoint.setLatitude( latitude );
    endpoint.setLongitude( longitude );
    //endpoint.setDME2Version( DME2Manager.getVersion() );
    endpoint.setProtocol( protocol );
    endpoint.setEndpointProperties( props );
    endpoint.setDmeUniformResource( uniformResource );
    endpoint.setRouteOffer( uniformResource.getRouteOffer() );
    endpoint.setServiceVersion( uniformResource.getVersion() );

    Calendar now = Calendar.getInstance( DME2Manager.getTimezone() );

    return endpoint;
  }

  /**
   * Converts a DME2 Endpoint to a GRM Service Endpoint
   * @param publishedEndpoint DME2 Endpoint
   * @return Service Endpoint
   */
  public static ServiceEndpoint convertToServiceEndpoint( DME2Configuration config, DME2Endpoint publishedEndpoint ) {
    ServiceEndpoint serviceEndpoint = new ServiceEndpoint();
    serviceEndpoint.setName( publishedEndpoint.getServiceName() );
    serviceEndpoint.setVersion( publishedEndpoint.getServiceVersion() );
    serviceEndpoint.setPort( String.valueOf( publishedEndpoint.getPort() ));
    serviceEndpoint.setProtocol( publishedEndpoint.getProtocol() );
    serviceEndpoint.setLatitude( String.valueOf( publishedEndpoint.getLatitude() ));
    serviceEndpoint.setLongitude( String.valueOf( publishedEndpoint.getLongitude() ));
    serviceEndpoint.setHostAddress( publishedEndpoint.getHost() );
    serviceEndpoint.setContextPath( publishedEndpoint.getContextPath() );
    serviceEndpoint.setRouteOffer( publishedEndpoint.getRouteOffer() );
   // serviceEndpoint.setAdditionalProperties( publishedEndpoint.getEndpointProperties() );
    serviceEndpoint.setEnv( publishedEndpoint.getEnvContext() );
  // serviceEndpoint.setDmeVersion( publishedEndpoint.getDME2Version() );

    Map<String, String> queryParams = publishedEndpoint.getDmeUniformResource().getQueryParamsMap();
    if ( queryParams.get( "supportedVersionRange" ) != null ) {
      serviceEndpoint.setClientSupportedVersions( queryParams.get( "supportedVersionRange" ) );
    }

    if ( publishedEndpoint instanceof DME2JDBCEndpoint ) {
      DME2JDBCEndpoint jdbcEndpoint = (DME2JDBCEndpoint) publishedEndpoint;
      serviceEndpoint.setDmeJDBCDatabaseName( jdbcEndpoint.getDatabaseName() );
      serviceEndpoint.setDmeJDBCHealthCheckUser( jdbcEndpoint.getHealthCheckUser() );
      serviceEndpoint.setDmeJDBCHealthCheckPassword( jdbcEndpoint.getHealthCheckPassword() );
      serviceEndpoint.setDmeJDBCHealthCheckDriver( jdbcEndpoint.getHealthCheckDriver() );
    }

    Properties additionalProperties = publishedEndpoint.getEndpointProperties();
    
    Calendar now = Calendar.getInstance( DME2Manager.getTimezone());

    long nowMs = System.currentTimeMillis();
    //Check if lease expiration values have been provided in the properties
    if ( additionalProperties != null &&
        additionalProperties.get( DME2Constants.KEY_SEP_LEASE_EXPIRATION_OVERRIDE_MIN ) != null ) {
    	now.add( Calendar.SECOND, Integer
    	          .parseInt( (String) additionalProperties.get( DME2Constants.KEY_SEP_LEASE_EXPIRATION_OVERRIDE_MIN ) ));

    } else if (publishedEndpoint instanceof DME2JDBCEndpoint || ( additionalProperties != null &&
        Boolean.valueOf( additionalProperties.getProperty( DME2Constants.DME2_REGISTER_STATIC_ENDPOINT ) ) ) ) {
      //Set expiration time to 09-09-9999 To represent static endpoint
    	now.set( Calendar.YEAR, +9999 );
        now.set( Calendar.MONTH, +8 );
        now.set( Calendar.DAY_OF_MONTH, +9 );

    } else {
      //Set expiration time to default time of 30 min
    	now.add( Calendar.MILLISECOND, config.getInt( DME2Constants.DME2_SEP_LEASE_LENGTH_MS ) );
      //now.add( Calendar.MILLISECOND, 15000 );      
    }

    if ( additionalProperties != null ) {
      serviceEndpoint.setAdditionalProperties( additionalProperties );
    }
    serviceEndpoint.setExpirationTime( XMLGregorianCalendarConverter.asXMLGregorianCalendar( now.getTime() ) );

    return serviceEndpoint;
  }

  public static ServiceEndpoint convertGrmEndpointToAccessorEndpoint( ServiceEndPoint input ) {

    ServiceEndpoint sep = new ServiceEndpoint();
    sep.setContextPath( input.getContextPath() );
    sep.setName( input.getName() );
    sep.setVersion(
        input.getVersion().getMajor() + "." + input.getVersion().getMinor() + "." + input.getVersion().getPatch() );
    sep.setPort( input.getListenPort() );
    sep.setProtocol( input.getProtocol() );
    sep.setLatitude( input.getLatitude() );
    sep.setLongitude( input.getLongitude() );
    sep.setHostAddress( input.getHostAddress() );
    sep.setContextPath( input.getContextPath() );
    sep.setRouteOffer( input.getRouteOffer() );
    sep.setAdditionalProperties( DME2URIUtils.convertNameValuePairToProperties( input.getProperties() ) );
    sep.setDmeVersion( input.getDME2Version() );
    sep.setClientSupportedVersions( input.getClientSupportedVersions() );
    sep.setDmeJDBCDatabaseName( input.getDME2JDBCDatabaseName() );
    sep.setDmeJDBCHealthCheckUser( input.getDME2JDBCHealthCheckUser() );
    sep.setDmeJDBCHealthCheckPassword( input.getDME2JDBCHealthCheckPassword() );
    sep.setExpirationTime( input.getExpirationTime() );

    return sep;
  }
}
