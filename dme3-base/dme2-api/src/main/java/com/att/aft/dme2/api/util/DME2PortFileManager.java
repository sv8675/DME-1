/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2PortFileManager {
	private static final Logger logger = LoggerFactory.getLogger( DME2PortFileManager.class );
	private static DME2PortFileManager instance;
	private DME2Configuration config;
	private static String portCacheFilePath = null;
	private static String sslPortCacheFilePath = null;

	private File portFile;
	private File sslPortFile;
	
	private final static String PORT="Port";
	private final static String SERVICE="Service";
	
	private DME2PortFileManager(DME2Configuration config) {		
		try {
			this.config = config;
			portCacheFilePath = config.getProperty(DME2Constants.AFT_DME2_PORT_CACHE_FILE,
					System.getProperty("user.home") + "/.aft/.dme2PortCache");
			File file = new File(portCacheFilePath);
			File dir = file.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			sslPortCacheFilePath = config.getProperty(
					DME2Constants.AFT_DME2_SSL_PORT_CACHE_FILE, System.getProperty("user.home")
							+ "/.aft/.dme2PortCache-ssl");
			File file1 = new File(sslPortCacheFilePath);
			File dir1 = file1.getParentFile();
			if (!dir1.exists()) {
				dir1.mkdirs();
			}
			
			portFile = new File(portCacheFilePath);
			sslPortFile = new File(sslPortCacheFilePath);		
		} catch(Exception e){
			logger.error(null, "DME2PortFileManager", "AFT-DME2-6704", new ErrorContext().add("DME2PortFileManager", ""),
					e);
		}		
		
	}

	public static synchronized DME2PortFileManager getInstance(DME2Configuration config) {
		if (instance == null){
			instance = new DME2PortFileManager(config);
		}
		return instance;
	}

	/**
	 * 
	 * @param service
	 * @return
	 */
	public String getPort(String service, boolean sslEnabled) {
		if (!config.getBoolean(DME2Constants.AFT_DME2_ALLOW_PORT_CACHING, true)) {
			// Port caching is disabled, so ignore file based port lookup
			return null;
		}
		logger.debug( null, "getPort", "DME2PortFileManager.getPort Service={}; sslEnabled={}", service, sslEnabled);
		if (service == null){
			return null;
		}
		String port = null;
		try {
			if (sslEnabled) {
				port = getPortFromFile(service, sslPortFile);
				logger.debug( null, "getPort",
				        "DME2PortFileManager.getPort Service={}; sslEnabled={};sslPortFile lookup port={};sslPortFile name={}",
				        service, sslEnabled, port, sslPortFile.getAbsolutePath() );
			} else {
				port = getPortFromFile(service, portFile);
		        logger.debug( null, "getPort",
		                "DME2PortFileManager.getPort Service={}; sslEnabled={};portFile lookup port={};portFile name={}", service,
		                sslEnabled, port, portFile.getAbsolutePath() );

			} 
			return port;
		} catch (Exception e) {
			// log error
			return null;
		}
	}

	/**
	 * 
	 * @param service
	 * @param fileName
	 * @return
	 */
	private String getPortFromFile(String service, File fileName) {
		String port = null;
		FileInputStream in = null;
		Properties props = new Properties();
		if (portFile.exists()) {
			try {
				in = new FileInputStream(fileName);
				props.load(in);
			} catch (Exception e) {
				return null;
			}
		}
		port = props.getProperty(service);
		if (port != null) {
			logger.debug(null, "DME2PortFileManager", DME2Constants.EXP_AFT_DME2_6703, new ErrorContext().add(SERVICE, service).add(PORT, String.valueOf(port)));			
			return port;
		}

		return port;
	}

	/**
	 * 
	 * @param path
	 * @param lockWaitIterations
	 * @param lockWaitSleepInterval
	 * @return
	 * @throws Exception
	 */
	private PortFileLockManager acquireLock(String path,
			long lockWaitIterations, long lockWaitSleepInterval)
			throws Exception {
		PortFileLockManager pfm = PortFileLockManager.getInstance(path,
				lockWaitIterations, lockWaitSleepInterval);
		pfm.acquire();
		return pfm;
	}

	/**
	 * 
	 * @param service
	 * @param port
	 * @throws DME2Exception
	 */
	public synchronized void persistPort(String service, Integer port,
			boolean sslEnabled) throws DME2Exception {
		if (!config.getBoolean(DME2Constants.AFT_DME2_ALLOW_PORT_CACHING, true)) {
			// Port caching is disabled, so ignore persisting port to the file
			return;
		}
		FileInputStream in = null;
		PortFileLockManager pfm = null;
		if (service == null || port == null) {
			return;
		}
		try {
			Properties props = new Properties();
			File file = null;
			if (sslEnabled) {
				file = sslPortFile;
			} else {
				file = portFile;
			}
			if (file.exists()) {
				in = new FileInputStream(file);
				props.load(in);
			} else {
				props.store(new FileOutputStream(file), "Initial creation--"
						+ new Date());
				in = new FileInputStream(file);
			}

			if (sslEnabled) {
				pfm = this.acquireLock(sslPortCacheFilePath,
						config.getLong(DME2Constants.DME2_PORT_FILELOCK_WAIT_ITER),
						config.getLong(DME2Constants.DME2_PORT_FILELOCK_WAIT_INTERVAL));
			} else {
				pfm = this.acquireLock(portCacheFilePath,
						config.getLong(DME2Constants.DME2_PORT_FILELOCK_WAIT_ITER),
						config.getLong(DME2Constants.DME2_PORT_FILELOCK_WAIT_INTERVAL));
			}
			String portStr = props.getProperty(service);
			if (portStr != null) {
				if (!portStr.contains(String.valueOf(port))) {
					props.setProperty(service,
							portStr + "," + String.valueOf(port));
					props.store(new FileOutputStream(file),
							"Published by DME2 at --" + new Date());
				} else {
					// port str is part of existing value. Log debug msg and
					// return.
					logger.debug(null, "DME2PortFileManager", DME2Constants.EXP_AFT_DME2_6701, new ErrorContext().add(SERVICE, service).add(PORT, String.valueOf(port)));
				}
			} else {
				props.setProperty(service, String.valueOf(port));
				props.store(new FileOutputStream(file),
						"Published by DME2 at --" + new Date());
			}
			logger.debug(null, "DME2PortFileManager", DME2Constants.EXP_AFT_DME2_6702, new ErrorContext().add(SERVICE, service).add(PORT, String.valueOf(port)));
		} catch (Exception e) {
			// log error
			logger.error(null, "DME2PortFileManager", DME2Constants.EXP_AFT_DME2_6700, new ErrorContext().add(SERVICE, service).add(PORT, String.valueOf(port)), e);
			throw new DME2Exception( DME2Constants.EXP_AFT_DME2_6700,
					new ErrorContext().add(SERVICE, service).add(PORT,
							String.valueOf(port)), e);
		} finally {
			if (pfm != null) {
				try {
					pfm.release();
				} catch (Exception e) {
					logger.debug(null, "DME2PortFileManager", LogMessage.DEBUG_MESSAGE, "Exception",e);
					// ignore any error in releasing lock
				}
			}

		}
	}
}
