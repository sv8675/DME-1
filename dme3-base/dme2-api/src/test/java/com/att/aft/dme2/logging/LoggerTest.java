/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith( PowerMockRunner.class )
@PrepareForTest({URI.class})
public class LoggerTest {
  private static final Class DEFAULT_CLASS = Class.class;
  private static final String DEFAULT_CLASS_NAME = DEFAULT_CLASS.getName();
  private static final String RANDOM_CLASS_NAME = RandomStringUtils.randomAlphabetic( 10 );
  private static final String DEFAULT_CONVERSATION_ID = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_SERVICE_URI = RandomStringUtils.randomAlphanumeric( 50 );
  private static final String DEFAULT_METHOD_NAME = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_MSG = RandomStringUtils.randomAlphanumeric( 30 );
  private static final String DEFAULT_ARG_1 = RandomStringUtils.randomAlphanumeric( 20 );
  private static final String DEFAULT_ARG_2 = RandomStringUtils.randomAlphanumeric( 20 );
  private static final Object[] DEFAULT_ARG_MULTI = new String[3];
  
  static {
    for ( int i = 0; i < DEFAULT_ARG_MULTI.length; i++ ) {
      DEFAULT_ARG_MULTI[i] = RandomStringUtils.randomAlphanumeric( 10 );
    }
  }

  private Logger defaultLogger = new Logger( RANDOM_CLASS_NAME );
  private URI mockUri;

  @BeforeClass
  public static void setUp() {

  }

  @AfterClass
  public static void tearDown() {

  }

  @Before
  public void setUpTest() {
    mockUri = PowerMockito.mock( URI.class );
  }

  @After
  public void tearDownTest() {

  }

  @Test
  public void test_ctor( ) {
    Logger logger = LoggerFactory.getLogger( DEFAULT_CLASS_NAME );
    assertNotNull( logger.logger );
    //assertEquals( "NOP", logger.logger.getName() );
    assertEquals( DEFAULT_CLASS_NAME, logger.className );
  }

  @Test
  public void test_error_string_message_no_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.error( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_error_string_message_one_arg() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.error( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_error_string_message_two_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.error( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1, DEFAULT_ARG_2 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_error_string_message_many_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.error( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_MULTI );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_warn_string_message_no_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.warn( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_warn_string_message_one_arg() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.warn( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_warn_string_message_two_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.warn( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1, DEFAULT_ARG_2 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_warn_string_message_many_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.warn( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_MULTI );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_info_string_message_no_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.info( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_info_string_message_one_arg() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.info( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_info_string_message_two_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.info( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1, DEFAULT_ARG_2 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_info_string_message_many_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.info( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_MULTI );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_debug_string_message_no_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.debug( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_debug_string_message_one_arg() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.debug( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_debug_string_message_two_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.debug( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_1, DEFAULT_ARG_2 );

    // verify
    PowerMockito.verifyStatic();
  }

  @Test
  public void test_debug_string_message_many_args() {
    // record
    Mockito.when( mockUri.toASCIIString() ).thenReturn( DEFAULT_SERVICE_URI );

    // play
    defaultLogger.debug( DEFAULT_CONVERSATION_ID, mockUri, DEFAULT_METHOD_NAME, DEFAULT_MSG, DEFAULT_ARG_MULTI );

    // verify
    PowerMockito.verifyStatic();
  }
}

