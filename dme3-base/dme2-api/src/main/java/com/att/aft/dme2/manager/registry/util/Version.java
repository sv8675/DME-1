package com.att.aft.dme2.manager.registry.util;

/**
 * Not really a "true" version comparator (which would take non-numeric arguments into account a bit more
 * consistently), but it should serve our purposes.  This one will just remove any non-numeric pieces
 *
 * This also sorts in REVERSE normal order (highest to lowest)
 */
public class Version implements Comparable<Version> {
  private String versionString;

  public Version() {
  }

  public Version( String versionString ) {
    this.versionString = versionString;
  }

  public String getVersionString() {
    return versionString;
  }

  public void setVersionString( String versionString ) {
    this.versionString = versionString;
  }


  /**
   * {@inheritDoc}
   * @param o Object to compare
   * @return -1, 0, 1 for less, equal, greater
   */
  @Override
  public int compareTo( Version o ) {
    if ( o == null || o.getVersionString() == null ) {
      return -1;
    }
    if ( getVersionString() == null ) {
      return 1;
    }

    String thisVersion = getVersionString();
    String thatVersion = o.getVersionString();

    // "Scrub" the non-numeric characters

    thisVersion = thisVersion.replaceAll( "[^0-9\\.]+", "" );
    thatVersion = thatVersion.replaceAll( "[^0-9\\.]+", "" );

    String[] thesePieces = thisVersion.split( "\\." );
    String[] thosePieces = thatVersion.split( "\\." );

    int i = 0;
    for ( String thisPiece : thesePieces ) {
      String thatPiece = "0";
      if ( thosePieces.length > i ) {
        thatPiece = thosePieces[i++];
      }

      int thisNum = 0;
      int thatNum = 0;

      try {
        thisNum = Integer.valueOf( thisPiece );
      } catch ( NumberFormatException e ) {
      }
      try {
        thatNum = Integer.valueOf( thatPiece );
      } catch ( NumberFormatException e ) {

      }
      if ( thisNum < thatNum ) {
        return 1;
      } else if ( thisNum > thatNum ) {
        return -1;
      }
    }
    return 0;
  }
}
