package com.att.aft.dme2.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2FileUtil {
  private static final Logger logger = LoggerFactory.getLogger( DME2FileUtil.class );

  private DME2FileUtil() {

  }

  public static Properties loadPropsFromFile( File file ) throws IOException {
    file.getParentFile().mkdir();
    file.createNewFile();
    Properties props = new Properties();
    FileInputStream is = null;
    try {
      is = new FileInputStream( file );
      props.load( is );
    } finally {
      if ( is != null ) {
        is.close();
      }
    }
    return props;
  }
}
