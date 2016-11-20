/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.manager.registry.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DME2FileUtilTest {
  private static final File BASE_DIR = new File( System.getProperty( "user.dir" ) + "/src/test/resources" );
  private static final String[] FILES_TO_USE = new String[] {
      "/service=MyService/version=1.0/envContext=LAB/routeInfo.xml",
      "/service=MyService/version=1.1/envContext=LAB/routeInfo.xml",
      "/service=MyService/version=1.1/envContext=NOTLAB/routeInfo.xml",
      "/service=MyService/version=11.0/envContext=LAB/routeInfo.xml"
  };

  @BeforeClass
  public static void setUp() {
    for ( String s : FILES_TO_USE ) {
      File f = new File( BASE_DIR.getAbsolutePath() + s );
      f.mkdirs();
      try {
        if ( !f.exists() && !f.createNewFile() ) {
          fail( "Couldn't create file " + f.getAbsolutePath() );
        }
      } catch ( IOException e ) {
        e.printStackTrace();
        fail( "Couldn't create file " + f.getAbsolutePath() );
      }
    }
  }

  @AfterClass
  public static void tearDown() throws IOException {
    for ( String s : FILES_TO_USE ) {
      if ( !new File( BASE_DIR.getAbsolutePath() + s ).delete() ) {
        System.err.println( "Couldn't remove file " + s );
      }
    }
  }

  @Test
  public void test_null_dir() {
    List<File> files = DME2FileUtil.hierarchicalFileLookup( null, null );
    assertNotNull( files );
    assertEquals( files.size(), 0 );
  }

  @Test
  public void test_null_path() {
    List<File> files = DME2FileUtil.hierarchicalFileLookup( BASE_DIR, null );
    assertNotNull( files );
    assertEquals( files.size(), 0 );
  }

  @Test
  public void test_zero_results() {
    List<File> files = DME2FileUtil.hierarchicalFileLookup( BASE_DIR, "/service=MyService/version=3.0/envContext=LAB/routeInfo.xml" );
    assertNotNull( files );
    assertEquals( files.size(), 0 );
  }

  @Test
  public void test_one_result() {
    List<File> files = DME2FileUtil.hierarchicalFileLookup( BASE_DIR, "/service=MyService/version=1.0/envContext=LAB/routeInfo.xml" );
    assertNotNull( files );
    assertEquals(1, files.size() );

    File file = files.get( 0 );
    assertNotNull( file );
    assertEquals( new File( BASE_DIR.getAbsolutePath() + "/service=MyService/version=1.0/envContext=LAB/routeInfo.xml" ).getAbsolutePath(), file.getAbsolutePath() );
  }

  @Test
  public void test_two_results() {
    List<File> files = DME2FileUtil.hierarchicalFileLookup( BASE_DIR, "/service=MyService/version=1/envContext=LAB/routeInfo.xml" );
    assertNotNull( files );
    assertEquals( 2, files.size() );

    for ( File f : files ) {
      assertNotNull( f );
      assertTrue(
          f.getAbsolutePath().equals( new File( BASE_DIR.getAbsolutePath() + "/service=MyService/version=1.0/envContext=LAB/routeInfo.xml" ).getAbsolutePath() ) ||
              f.getAbsolutePath().equals( new File( BASE_DIR.getAbsolutePath() + "/service=MyService/version=1.1/envContext=LAB/routeInfo.xml" ).getAbsolutePath() ));
    }
  }
}
