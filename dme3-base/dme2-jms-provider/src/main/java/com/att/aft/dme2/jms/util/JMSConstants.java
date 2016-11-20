/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

public class JMSConstants {
	  public final static String DME_API_DEFAULT_CONFIG_FILE_NAME = "dme-api_defaultConfigs.properties";
	  public final static String JMS_PROVIDER_DEFAULT_CONFIG_FILE_NAME = "jms-provider_defaultConfigs.properties";
//	  public final static String METRICS_COLLECTOR_DEFAULT_CONFIG_FILE_NAME = "metrics-collector_defaultConfigs.properties";
	  public final static String AFT_DME2_RECEIVE_MAX_RETRY = "AFT_DME2_RECEIVE_MAX_RETRY";
	  public final static String AFT_DME2_RETRY_SLEEP = "AFT_DME2_RETRY_SLEEP";
	  public final static String DME2_RETRY_SLEEP = "DME2_RETRY_SLEEP";
	  public static String AFT_DME2_TEMPQ_TP_CORE = "AFT_DME2_TEMPQ_TP_CORE";
	  public static String AFT_DME2_TEMPQ_TP_MAX = "AFT_DME2_TEMPQ_TP_MAX";
	  public static String AFT_DME2_TEMPQ_TP_TTL = "AFT_DME2_TEMPQ_TP_TTL";
	  public static final String AFT_DME2_JMS_REPLY_QUEUE = "autoJMSReplyToQueue";
	  public final static String DME2_RECEIVE_MAX_RETRY = "DME2_RECEIVE_MAX_RETRY";
	  public static final String DME2_JMS_TEMP_QUEUE_REC_CLEANUP = "DME2_JMS_TEMP_QUEUE_REC_CLEANUP";
	  public static final String DME2_DISABLE_TIMESTAMP = "DME2_DISABLE_TIMESTAMP";
	  
	  public final static String DEFAULT_CONFIG_MANAGER_NAME = "DefaultDME2JmsManager";
	  public final static String AFT_DME2_CONT_TIMEOUT = "AFT_DME2_CONT_TIMEOUT";
	  public static final String AFT_DME2_SERVER_REPLY_OVERRIDE_TIMEOUT_MS = "AFT_DME2_SERVER_REPLY_OVERRIDE_TIMEOUT_MS";
	  
	  public static String AFT_DME2_MAX_RETRY = "AFT_DME2_MAX_RETRY";
	  public static String DME2_TEMPQUEUE_IDLETIMEOUT_MS = "DME2_TEMPQUEUE_IDLETIMEOUT_MS";
	  
	  public static String DME2_TEMP_QUEUE_CLEANUP_INTERVAL_MS = "DME2_TEMP_QUEUE_CLEANUP_INTERVAL_MS";
	  public static String DME2_CONT_QUEUE_CLEANUP_INTERVAL_MS = "DME2_CONT_QUEUE_CLEANUP_INTERVAL_MS";
	  public static String AFT_DME2_SERVER_QDEPTH = "AFT_DME2_SERVER_QDEPTH";
	  public static String AFT_DME2_CLIENT_QDEPTH = "AFT_DME2_CLIENT_QDEPTH";
	  public static String serverQDepth = AFT_DME2_SERVER_QDEPTH;
	  public static String clientQDepth = AFT_DME2_CLIENT_QDEPTH;

}
