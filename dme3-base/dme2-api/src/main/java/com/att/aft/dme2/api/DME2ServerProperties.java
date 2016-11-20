/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import com.att.aft.dme2.config.DME2Configuration;

public class DME2ServerProperties {


		// Configuration Keys
		private static final String KEY_PORT_RANGE = "AFT_DME2_PORT_RANGE";
		private static final String KEY_CONN_IDLE_TIMEOUTMS = "AFT_DME2_CONN_IDLE_TIMEOUTMS";
		private static final String KEY_CORE_POOL_SIZE = "AFT_DME2_CORE_POOL_SIZE";
		private static final String KEY_MAX_POOL_SIZE = "AFT_DME2_MAX_POOL_SIZE";
		private static final String KEY_PORT = "AFT_DME2_PORT";
		private static final String KEY_SOCKET_ACCEPTOR_THREADS = "AFT_DME2_SOCKET_ACCEPTOR_THREADS";
		private static final String KEY_THREAD_IDLE_TIME_MS = "AFT_DME2_THREAD_IDLE_TIME_MS";
		private static final String KEY_REQUEST_BUFFER_SIZE = "AFT_DME2_REQUEST_BUFFER_SIZE";
		private static final String KEY_RESPONSE_BUFFER_SIZE = "AFT_DME2_RESPONSE_BUFFER_SIZE";
		private static final String KEY_REUSE_ADDRESS = "AFT_DME2_REUSE_ADDRESS";
		private static final String KEY_USE_DIRECT_BUFFERS = "AFT_DME2_USE_DIRECT_BUFFERS";
		private static final String KEY_HOSTNAME = "AFT_DME2_HOSTNAME";
		private static final String KEY_SEND_DATEHEADER = "AFT_DME2_SEND_DATEHEADER";
		private static final String KEY_SEND_SERVERVERSION = "AFT_DME2_SEND_SERVERVERSION";
		private static final String KEY_GRACEFUL_SHUTDOWN_TIME_MS = "AFT_DME2_GRACEFUL_SHUTDOWN_TIME_MS";
		private static final String KEY_SSL_ENABLE = "AFT_DME2_SSL_ENABLE";
		private static final String KEY_ALLOW_RENEG = "AFT_DME2_ALLOW_RENEGOTIATE";
		private static final String KEY_KEYSTORE = "AFT_DME2_KEYSTORE";
		private static final String KEY_TRUSTSTORE = "AFT_DME2_TRUSTSTORE";
		private static final String KEY_MAX_QUEUE_SIZE = "AFT_DME2_MAX_QUEUE_SIZE";
		private static final String KEY_MAX_REQUEST_POST_SIZE = "AFT_DME2_MAX_REQUEST_POST_SIZE";
		private static final String KEY_MAX_REQUEST_HEADER_SIZE = "AFT_DME2_MAX_REQUEST_HEADER_SIZE";
		private static final String KEY_SSL_TRUST_ALL = "AFT_DME2_SSL_TRUST_ALL";
		private static final String KEY_SSL_CERT_ALIAS ="AFT_DME2_SSL_CERT_ALIAS" ;
		private static final String KEY_SSL_NEED_CLIENT_AUTH = "AFT_DME2_SSL_NEED_CLIENT_AUTH";
		private static final String KEY_SSL_WANT_CLIENT_AUTH = "AFT_DME2_SSL_WANT_CLIENT_AUTH";
		private static final String KEY_SSL_ENABLED_SESSION_CACHING = "AFT_DME2_SSL_ENABLED_SESSION_CACHING";
		private static final String KEY_SSL_SESSION_CACHE_SIZE = "AFT_DME2_SSL_SESSION_CACHE_SIZE";
		private static final String KEY_SSL_SESSION_TIMEOUT = "AFT_DME2_SSL_SESSION_TIMEOUT";
		private static final String KEY_SSL_VALIDATE_PEER_CERTS = "AFT_DME2_SSL_VALIDATE_PEER_CERTS";
		private static final String KEY_SSL_VALIDATE_CERTS = "AFT_DME2_SSL_VALIDATE_CERTS";
		private static final String KEY_SSL_EXCLUDE_PROTOCOLS = "AFT_DME2_SSL_EXCLUDE_PROTOCOLS";
		private static final String KEY_SSL_EXCLUDE_CIPHERSUITES = "AFT_DME2_SSL_EXCLUDE_CIPHERSUITES";
		private static final String KEY_SSL_INCLUDE_PROTOCOLS = "AFT_DME2_SSL_INCLUDE_PROTOCOLS";
		private static final String KEY_SSL_INCLUDE_CIPHERSUITES = "AFT_DME2_SSL_INCLUDE_CIPHERSUITES";
		
		// Defaults moved to dme2_defaultConfigs.properties
		/*private static final Integer DEFAULT_THREAD_IDLE_TIME_MS = 120000;
		private static final Integer DEFAULT_SOCKET_ACCEPTOR_THREADS = 5;
		private static final Integer DEFAULT_RESPONSE_BUFFER_SIZE = 32768;
		private static final Integer DEFAULT_REQUEST_BUFFER_SIZE = 8096;
		private static final Integer DEFAULT_MAX_POOL_SIZE = 256;
		private static final Integer DEFAULT_CORE_POOL_SIZE = 20;
		private static final Integer DEFAULT_CONN_IDLE_TIMEOUTMS = 120000;
		private static final Boolean DEFAULT_SSL_ENABLE = false;
		private static final Boolean DEFAULT_ALLOW_RENEG = true;
		private static final Integer DEFAULT_MAX_QUEUE_SIZE = 0; 
		private static final Integer DEFAULT_MAX_REQUEST_HEADER_SIZE = 65536;
		private static final Integer DEFAULT_MAX_REQUEST_POST_SIZE = 200000;*/
		
		private DME2Configuration config;
		private String hostName;
		private int maxPoolSize;
		private Integer port;
		private int corePoolSize;
		private int connectionIdleTime;
		private String portRange;
		private int requestBufferSize;
		private int responseBufferSize;
		private int socketAcceptorThread;
		private int gracefulShutdownTimeMs;
		private int threadIdleTimeMs;
		private boolean reuseAddress;
		private boolean sslEnable;
		private boolean useDirectBuffers;
		private int maxQueueSize;
		private boolean allowRange;
		private int maxRequestHeaderSize;
		private int maxRequestPostSize;
		private String trustStore;
		private String keyStore;
		private boolean validCertificate;
		private boolean validSslCertificate;
		private int sslSessiontimeout;
		private int sslSessionCacheSize;
		private boolean sslSessionCaching;		
		
		public DME2ServerProperties(DME2Configuration config) {
			this.config = config;
		}
			

		/**
		 * @return the maxConnectionIdleTimeMs
		 */
		public int getConnectionIdleTimeMs() {
			return config.getInt(KEY_CONN_IDLE_TIMEOUTMS, connectionIdleTime);
		}

		/**
		 * @return the corePoolSize
		 */
		public int getCorePoolSize() {
			return config.getInt(KEY_CORE_POOL_SIZE, corePoolSize);
		}

		/**
		 * Return the hostname for this server
		 * @return
		 */
		public String getHostname() {
			return config.getProperty(KEY_HOSTNAME, hostName);
		}

		/**
		 * @return the maxPoolSize
		 */
		public int getMaxPoolSize() {
			return config.getInt(KEY_MAX_POOL_SIZE, maxPoolSize);
		}
				
		/**
		 * Return the port for this server
		 * @return
		 */
		public Integer getPort() {
			return config.getInteger(KEY_PORT, port);
		}
		
		/**
		 * @return the requestBufferSize
		 */
		public int getRequestBufferSize() {
			return config.getInt(KEY_REQUEST_BUFFER_SIZE, requestBufferSize);
		}

		/**
		 * @param requestBufferSize the requestBufferSize to set
		 */		
		public void setRequestBufferSize(int requestBufferSize) {
			this.requestBufferSize = requestBufferSize;
		}

		/**
		 * @return the responseBufferSize
		 */
		public int getResponseBufferSize() {
			return config.getInt(KEY_RESPONSE_BUFFER_SIZE, responseBufferSize);
		}

		/**
		 * @param responseBufferSize the responseBufferSize to set
		 */		
		public void setResponseBufferSize(int responseBufferSize) {
			this.responseBufferSize = responseBufferSize;
		}	

		/**
		 * return the port range used when starting this server
		 * @return
		 */
		public String getPortRange() {
			return config.getProperty(KEY_PORT_RANGE, portRange);
		}

		/**
		 * Get the number of socket acceptor threads
		 * @return
		 */
		public int getSocketAcceptorThreads() {
			return config.getInt(KEY_SOCKET_ACCEPTOR_THREADS, socketAcceptorThread);
		}

		/**
		 * Get the idle thread ttl
		 * @return
		 */
		public int getThreadIdleTimeMs() {
			return config.getInt(KEY_THREAD_IDLE_TIME_MS, threadIdleTimeMs);
		}
		
		/**
		 * @param maxConnectionIdleTimeMs
		 *            the maxConnectionIdleTimeMs to set
		 */
		public void setConnectionIdleTimeMs(int connectionIdleTimeMs) {
			this.connectionIdleTime = connectionIdleTimeMs;
		}

		/**
		 * @param corePoolSize
		 *            the corePoolSize to set
		 */
		public void setCorePoolSize(int corePoolSize) {
			this.corePoolSize = corePoolSize;
		}

		/**
		 * Set the hostname to use when starting the server
		 * @param host
		 */		
		public void setHostname(String host) {
			this.hostName = host;
		}

		/**
		 * @param maxPoolSize
		 *            the maxPoolSize to set
		 */
		public void setMaxPoolSize(int maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public void setPortRange(String portRange) {
			this.portRange = portRange;
		}

		public void setSocketAcceptorThreads(int socketAcceptorThreads) {
			this.socketAcceptorThread = socketAcceptorThreads;
		}

		public void setThreadIdleTimeMs(int threadIdleTimeMs) {
			this.threadIdleTimeMs = threadIdleTimeMs;
		}
		
		/**
		 * 
		 * @param gracefulShutdownTimeMs
		 */
		public void setGracefulShutdownTimeMs(int gracefulShutdownTimeMs) {
			this.gracefulShutdownTimeMs = gracefulShutdownTimeMs;
		}

		/**
		 * @return the useDirectBuffers
		 */
		public boolean isUseDirectBuffers() {
			return config.getBoolean(KEY_USE_DIRECT_BUFFERS, useDirectBuffers);
		}

		/**
		 * @param useDirectBuffers the useDirectBuffers to set
		 */
		public void setUseDirectBuffers(boolean useDirectBuffers) {
			this.useDirectBuffers = useDirectBuffers;
		}
		
		/**
		 * @return the reuseAddress
		 */
		public boolean isReuseAddress() {
			return config.getBoolean(KEY_REUSE_ADDRESS, reuseAddress);
		}

		/**
		 * @param reuseAddress the reuseAddress to set
		 */
		public void setReuseAddress(boolean reuseAddress) {
			this.reuseAddress = reuseAddress;
		}	
		
		/**
		 * Returns current state of SSL
		 * @return
		 */
		public boolean isSslEnable() {
			return config.getBoolean(KEY_SSL_ENABLE, sslEnable);
		}
		
		/**
		 * Turn SSL on or off.  Default is false.
		 * @param sslEnable
		 */
		public void setSslEnable(boolean sslEnable) {
			this.sslEnable = sslEnable;
		}
		
		
		public int getMaxRequestHeaderSize() {
			return config.getInt(KEY_MAX_REQUEST_HEADER_SIZE, maxRequestHeaderSize);
		}

		public int getMaxRequestPostSize() {
			return config.getInt(KEY_MAX_REQUEST_POST_SIZE, maxRequestPostSize);
		}

		public int getMaxQueueSize() {
			return config.getInt(KEY_MAX_QUEUE_SIZE, maxQueueSize);
		}
		
		public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		public Boolean isAllowRenegotiate() {
			return config.getBoolean(KEY_ALLOW_RENEG, allowRange);
		}
		
		public void setAllowRenegotiate(Boolean allowRange) {
			this.allowRange = allowRange;
		}

		public String getTrustStore() {
			return config.getProperty(KEY_TRUSTSTORE, trustStore);
		}

		public String getKeyStore() {
			return config.getProperty(KEY_KEYSTORE, keyStore);
		}
		
		public String[] getExcludeProtocols() {
			return getPropsArray(KEY_SSL_EXCLUDE_PROTOCOLS);
		}
		
		public String[] getIncludeProtocols() {
			return getPropsArray(KEY_SSL_INCLUDE_PROTOCOLS);

		}
		
		public String[] getExcludeCiperSuites() {
			return getPropsArray(KEY_SSL_EXCLUDE_CIPHERSUITES);
		}
		
		public String[] getIncludeCiperSuites() {
			return getPropsArray(KEY_SSL_INCLUDE_CIPHERSUITES);
		}
		
		private String[] getPropsArray(String key){
			String temp = config.getProperty(key);
			if(temp != null){
				return temp.split(",");
			}
			return null;
		}
		

		public Boolean isValidateCerts() {
			return config.getBoolean(KEY_SSL_VALIDATE_CERTS, validCertificate);
		}

		public Boolean isSslValidatePeerCerts() {
			return config.getBoolean(KEY_SSL_VALIDATE_PEER_CERTS, validSslCertificate);
		}

		public Integer getSslSessionTimeout() {
			return new Integer(config.getInt(KEY_SSL_SESSION_TIMEOUT, sslSessiontimeout)); 
		}

		public Integer getSslSessionCacheSize() {
			return new Integer(config.getInt(KEY_SSL_SESSION_CACHE_SIZE, sslSessionCacheSize)); 
		}

		public Boolean isEnableSessionCaching() {
			return config.getBoolean(KEY_SSL_ENABLED_SESSION_CACHING, sslSessionCaching);
		}

		public Boolean getWantClientAuth() {
			return config.getBoolean(KEY_SSL_WANT_CLIENT_AUTH);
		}

		public Boolean getNeedClientAuth() {
			return config.getBoolean(KEY_SSL_NEED_CLIENT_AUTH);
		}

		public String getSslCertAlias() {
			return config.getProperty(KEY_SSL_CERT_ALIAS);
		}

		public Boolean getSslTrustAll() {
			return config.getBoolean(KEY_SSL_TRUST_ALL);
		}
		
		public Boolean getSendDateheader() {
			return config.getBoolean(KEY_SEND_DATEHEADER);
		}

		public Boolean getSendServerversion() {
			return config.getBoolean( KEY_SEND_SERVERVERSION);
		}

		public Integer getGracefulShutdownTimeMs() {
			return new Integer(config.getInt(KEY_GRACEFUL_SHUTDOWN_TIME_MS, gracefulShutdownTimeMs));
		}

		public DME2Configuration getConfig() {
			return config;
		}	
}