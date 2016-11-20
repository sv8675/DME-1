package com.att.aft.dme2.manager.registry.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * File utility for hierarchical lookups
 */
public class DME2FileUtil {

  private DME2FileUtil() {

  }


  /**
   * This method allows for a regular-expression in a particular part of a subdirectory similar to DirectoryScanner
   * Currently this assumes that the hierarchical lookup is done by version number but this can be generalized
   * to perform other hierarchical tasks as well.
   *
   * For example: if /service=MyService/version=1.0/envContext=DEV/routeInfo.xml were used as a straight regex against
   *  a list of files that regex would be /service=MyService/version=1\.0(\..*?)/envContent=DEV/routeInfo\.xml
   *
   * @param path Original Path created for search
   * @return List of files matching criteria
   */
  public static List<File> hierarchicalFileLookup( File currentDir, String path ) {
    List<File> files = new ArrayList<File>();
    if ( path == null || path.isEmpty() || currentDir == null || !currentDir.exists() ) {
      return files;
    }

    // Remove the leading slash
    if ( path.charAt( 0 ) == '/' ) {
      path = path.substring( 1 );
    }

    String[] pathPieces = path.split( "/" );
    String firstPiece = pathPieces[0];

    if ( firstPiece.startsWith( "version=" ) ) {
      // Search for files in the current directory matching the new regex
      for ( File f : currentDir.listFiles() ) {
        if ( f.isDirectory() ) {
          String fileName = f.getName();
          if ( fileName.equals( firstPiece ) || fileName.startsWith( firstPiece + "." )) {
            files.addAll( hierarchicalFileLookup( new File( currentDir + "/" + fileName ), StringUtils.join( Arrays
                .copyOfRange( pathPieces, 1, pathPieces.length ), "/" ) ));
          }
        }
      }
    } else if ( pathPieces.length > 1 ) {
      // We're not yet to the file itself, keep going
      files.addAll( hierarchicalFileLookup( new File( currentDir.getAbsolutePath() + "/" + firstPiece ), StringUtils.join( Arrays
          .copyOfRange( pathPieces, 1, pathPieces.length ), "/")));
    } else {
      File file = new File( currentDir + "/" + firstPiece );
      if ( file.exists() ){
        files.add( file );
      }
    }
    return files;
  }
}
