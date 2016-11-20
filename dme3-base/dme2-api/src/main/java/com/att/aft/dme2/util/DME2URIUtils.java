package com.att.aft.dme2.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.scld.grm.types.v1.NameValuePair;

public class DME2URIUtils {
  private static final Logger logger = LoggerFactory.getLogger( DME2URIUtils.class.getName() );
  private static final String FORMATTED_PATH =
      "/" + DME2Constants.SERVICE_PATH_KEY_SERVICE + "=%s/" + DME2Constants.SERVICE_PATH_KEY_VERSION + "=%s/" +
          DME2Constants.SERVICE_PATH_KEY_ENV_CONTEXT + "=%s";
  private static final String FORMATTED_PATH_WITH_ROUTE_OFFER =
      FORMATTED_PATH + "/" + DME2Constants.SERVICE_PATH_KEY_ROUTE_OFFER + "=%s";

  public static String buildServiceURIString( String serviceName,
                                              String version, String envContext ) {
    if ( serviceName == null || version == null || envContext == null ) {
      return null;
    }

    return String.format( FORMATTED_PATH, serviceName, version, envContext );
  }

  public static String buildServiceURIString( String serviceName,
                                              String version, String envContext, String routeOffer ) {
    if ( routeOffer == null ) {
      return buildServiceURIString( serviceName, version, envContext );
    }
    if ( serviceName == null || version == null || envContext == null ) {
      return null;
    }

    return String.format( FORMATTED_PATH_WITH_ROUTE_OFFER, serviceName, version, envContext, routeOffer );
  }

  public static String encodeURIString( String uriStr, boolean isEncoded ) {
    String encodedStr = uriStr;

    try {
      String uriPrefix = null;
      int contextPathIndex = -1;

      if ( uriStr.contains( "/" + DME2Constants.SERVICE_PATH_KEY_SERVICE ) ) {
        //Extract the prefix from the URI string (i.e. http://DME2SEARCH or http://DMELOCAL)
        contextPathIndex = uriStr.indexOf( "/" + DME2Constants.SERVICE_PATH_KEY_SERVICE, 0 );
        uriPrefix = uriStr.substring( 0, contextPathIndex );
      } else if ( uriStr.contains( "%2F" + DME2Constants.SERVICE_PATH_KEY_SERVICE ) ) {
        //Extract the prefix from the URI string (i.e. http://DME2SEARCH or http://DMELOCAL)
        contextPathIndex = uriStr.indexOf( "%2F" + DME2Constants.SERVICE_PATH_KEY_SERVICE, 0 );
        uriPrefix = URLDecoder.decode( uriStr.substring( 0, contextPathIndex ), "UTF-8" );
      }

      if ( !isEncoded ) {
        /*
         * Get the contextPath from the URI string and encode special
				 * characters
				 */
        String contextPath = uriStr.substring( contextPathIndex );
        String encodedContextPath = URLEncoder.encode( contextPath, "UTF-8" );
        String finalEncodedURIStr = uriPrefix + encodedContextPath;
        encodedStr = finalEncodedURIStr.replace( "%2F", "/" ).replace( "%3D", "=" ).replace( "%3F", "?" ).replace(
            "%26", "&" );
      } else {
				/* If we get here, the URI input string was already encoded */
        String encodedContextPath = uriStr.substring( contextPathIndex );
        String finalEncodedURIStr = uriPrefix + encodedContextPath;
        encodedStr = finalEncodedURIStr.replace( "%2F", "/" ).replace( "%3D", "=" ).replace( "%3F", "?" ).replace(
            "%26", "&" );
      }
    } catch ( Exception e ) {
      logger.warn( null, null, "encodeURIString", DME2Constants.EXP_GEN_URI_EXCEPTION,
          new ErrorContext().add( "URI", uriStr ), e );
    }

    return encodedStr;
  }

  public static String formatClientURIString( final String newInString ) {
    String inString = newInString;
    final String searchPrefix = "http://DME2SEARCH";
    final String resolvePrefix = "http://DME2RESOLVE";

    if ( !inString.startsWith( "http" ) && !inString.startsWith( "dme2" )
        && !inString.startsWith( "ws" ) ) {
			/* if client String doesn't have leading slash, add it */
      if ( !inString.startsWith( "/" ) ) {
        inString = "/" + inString;
      }

      if ( inString.contains( "partner" ) ) {
        inString = searchPrefix + inString;
      } else if ( inString.contains( DME2Constants.SERVICE_PATH_KEY_ROUTE_OFFER ) ) {
        inString = resolvePrefix + inString;
      }
    }

    return inString;
  }

  public static boolean isParseable( String value, Class<?> targetType ) {
    try {
      if ( targetType == Integer.class ) {
        Integer.parseInt( value );
      } else if ( targetType == Long.class ) {
        Long.parseLong( value );
      } else if ( targetType == Double.class ) {
        Double.parseDouble( value );
      }
    } catch ( NumberFormatException e ) {
      return false;
    }

    return true;
  }

  /**
   * Adds query string to the URI path string. If no query string was passed in then the path is just returned.
   */
  public static String appendQueryStringToPath( String path, final String newQueryStr ) {
    String queryStr = newQueryStr;
    if ( queryStr == null ) {
      return path;
    }

    if ( !queryStr.startsWith( "?" ) ) {
      queryStr = "?" + queryStr;
    }
    return path + queryStr;
  }

  public static List<NameValuePair> convertPropertiestoNameValuePairs( Properties props ) {
    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

    if ( props != null ) {
      for ( Object obj : props.keySet() ) {
        String key = (String) obj;
        String value = props.getProperty( key );

        NameValuePair nameValPair = new NameValuePair();
        nameValPair.setName( key );
        nameValPair.setValue( value );

        nameValuePairs.add( nameValPair );
      }
    }

    return nameValuePairs;
  }

  public static Properties convertNameValuePairToProperties( List<NameValuePair> nameValuePairs ) {
    Properties props = new Properties();

    if ( nameValuePairs != null && !nameValuePairs.isEmpty() ) {
      for ( NameValuePair nameValuePair : nameValuePairs ) {
        props.setProperty( nameValuePair.getName(),
            nameValuePair.getValue() );
      }
    }
    return props;
  }

  public static Map<String, String> splitServiceURIString( final String newServiceURI ) {
    String serviceURI = newServiceURI;
    Map<String, String> serviceURIValues = new HashMap<String, String>();

    if ( !serviceURI.startsWith( "/" ) ) {
      serviceURI = "/" + serviceURI;
    }
    String[] toks = serviceURI.split( "/" );

    for ( String tok : toks ) {
      if ( tok.contains( "=" ) ) {
        String[] pair = tok.split( "=" );
        String key = pair[0];
        String value = pair[1];
        serviceURIValues.put( key, value );
      }
    }

    return serviceURIValues;
  }

  public static String buildUniformResourceStr( String serviceName, String host, int port ) {
    String urlStr = null;
    if ( serviceName.startsWith( DME2Constants.HTTP ) ) {
      urlStr = serviceName;
    } else {
      if ( serviceName.startsWith( "/" ) ) {
        urlStr = DME2Constants.HTTP + host + ":" + port + serviceName;
      } else {
        urlStr = DME2Constants.HTTP + host + ":" + port + "/" + serviceName;
      }
    }
    return urlStr;
  }
}
