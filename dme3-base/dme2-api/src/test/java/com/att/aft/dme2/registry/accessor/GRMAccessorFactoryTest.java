/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.registry.accessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class GRMAccessorFactoryTest {
  @Test
  public void test_buildEnvFromDNSPlatform() {
    GRMAccessorFactory factory = new GRMAccessorFactory();
    assertEquals( "TEST", factory.buildEnvFromDNSPlatform( "this-is-a-test" ) );
  }

  @Test
  public void test_buildEnvFromDNSPlatform_bad() {
    GRMAccessorFactory factory = new GRMAccessorFactory();
    assertNull( factory.buildEnvFromDNSPlatform( null ));
  }
}
