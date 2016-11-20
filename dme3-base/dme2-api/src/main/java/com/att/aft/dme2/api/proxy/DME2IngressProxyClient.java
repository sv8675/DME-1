/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2IngressProxyClient {
  private static Logger logger = LoggerFactory.getLogger( DME2IngressProxyClient.class );
  /**
   * @param args
   */
  public static void main( String[] args ) throws Exception {
    int timeout = 120000;
    String payload = "ProxyTest request";
    HttpURLConnection conn = null;
    URL url = null;
    InputStream istream = null;
    url = new URL(
        "http://localhost:5678/service=com.att.aft.DME2IngressProxy/version=1.0.0/envContext=UAT/routeOffer=DEFAULT?service=com.att.aft.DME2CREchoService&version=1.1.0&envContext=UAT&routeOffer=BAU" );
    conn = (HttpURLConnection) url.openConnection();
    conn.setConnectTimeout( timeout );
    conn.setReadTimeout( timeout );
    conn.setDoInput( true );
    conn.setDoOutput( true );
    OutputStream out = conn.getOutputStream();
    out.write( payload.getBytes() );
    out.flush();
    out.close();

    int respCode = conn.getResponseCode();
    logger.debug( null, "main", " Resp code : {}", respCode );
    if ( respCode != 200 ) {
      InputStream estream = conn.getErrorStream();
      InputStreamReader input = new InputStreamReader( estream );
      final char[] buffer = new char[8096];
      StringBuilder output = new StringBuilder( 8096 );
      try {
        for ( int read = input.read( buffer, 0, buffer.length ); read != -1; read = input
            .read( buffer, 0, buffer.length ) ) {
          output.append( buffer, 0, read );
        }
      } catch ( IOException e ) {
        // send error
        e.printStackTrace();
      }
      logger.debug( null, "main", "Output: {}", output );
      throw new Exception( " Service Call Failed " );
    }
    //conn.setDoInput(true);
    istream = conn.getInputStream();

    InputStreamReader input = new InputStreamReader( istream );
    final char[] buffer = new char[8096];
    StringBuilder output = new StringBuilder( 8096 );
    try {
      for ( int read = input.read( buffer, 0, buffer.length ); read != -1; read = input.read( buffer, 0, buffer.length ) ) {
        output.append( buffer, 0, read );
      }
    } catch ( IOException e ) {
      // send error
      e.printStackTrace();
    }

    logger.debug( null, "main", "Output = {}", output.toString() );
  }

}
