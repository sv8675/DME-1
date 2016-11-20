package com.att.aft.dme2.manager.registry.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

public class VersionTest {

  private static final String DEFAULT_VERSION_STRING = RandomStringUtils.randomAlphanumeric( 10 );

  @Test
  public void test_default_ctor() {
    Version version = new Version();
    assertNull( version.getVersionString() );
  }

  @Test
  public void test_ctor_string() {
    Version version = new Version( DEFAULT_VERSION_STRING );
    assertEquals( DEFAULT_VERSION_STRING, version.getVersionString() );
  }

  @Test
  public void test_compare() {
    Version version1 = new Version( "1.0" );
    Version version2 = new Version( "2.0" );

    List<Version> versionList = Arrays.asList( new Version[] { version1, version2 });
    assertEquals( version1, versionList.get( 0 ));
    Collections.sort( versionList );
    assertEquals( version2, versionList.get( 0 ));
  }

  @Test
  public void test_compare_null() {
    Version version = new Version( DEFAULT_VERSION_STRING );
    assertEquals( -1, version.compareTo( null ));
  }

  @Test
  public void test_compare_null_version() {
    Version version = new Version( DEFAULT_VERSION_STRING );
    assertEquals( -1, version.compareTo( new Version() ));
  }

  @Test
  public void test_compare_this_null() {
    Version version = new Version(  );
    assertEquals( 1, version.compareTo( new Version( DEFAULT_VERSION_STRING )));
  }

  @Test
  public void test_alpha_equal() {
    Version version1 = new Version( "1.aaaaaaa1a" );
    Version version2 = new Version( "1.1" );

    assertEquals( 0, version1.compareTo( version2 ));
  }

  @Test
  public void test_compare_subsections() {
    Version version1 = new Version ( "1.2.3.4.5.6.7" );
    Version version2 = new Version( "1.2.3.4.5.6.8" );
    assertEquals( 1, version1.compareTo( version2 ));
    assertEquals( -1, version2.compareTo( version1 ));
  }
}
