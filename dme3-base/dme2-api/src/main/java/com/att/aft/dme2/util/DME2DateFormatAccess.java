package com.att.aft.dme2.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2DateFormatAccess {
  final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
  private static Logger logger = LoggerFactory.getLogger( DME2DateFormatAccess.class.getName() );

  private ThreadLocal<DateFormat> df;

  public DME2DateFormatAccess( DME2Configuration config ) {
    df = new ThreadLocal<DateFormat>() {

      @Override
      public DateFormat get() {
        return super.get();
      }

      @Override
      protected DateFormat initialValue() {
        SimpleDateFormat sdf = new SimpleDateFormat( ISO_FORMAT );
        if ( config.getBoolean( DME2Constants.AFT_DME2_ALLOW_CLIENT_SEND_TZ_OVERRIDE ) ) {
          sdf.setTimeZone(
              TimeZone.getTimeZone( config.getProperty( DME2Constants.AFT_DME2_CLIENT_SEND_TIMESTAMP_TZ_KEY ) ) );
        }
        return sdf;
      }

      @Override
      public void remove() {
        super.remove();
      }

      @Override
      public void set( DateFormat value ) {
        super.set( value );
      }

    };
  }

  public Date convertStringToDate( String dateString ) {
    try {
      return df.get().parse( dateString );
    } catch ( Exception e ) {
      //logger.log( Level.WARNING, "Error in formatting string to date", e );
      logger.warn( null, "convertStringToDate", "Error in formatting string to date", e );
      // Ignore any parse exception and return null
      return null;
    }
  }

  public String convertDateToString( Date date ) {
    try {
      return df.get().format( date );
    } catch ( Exception e ) {
      //logger.log( Level.WARNING, "Error in formatting date to string", e );
      logger.warn( null, "convertDateToString", "Error in formatting date to string", e );
      // Ignore any parse exception and return null
      return null;
    }
  }

}
