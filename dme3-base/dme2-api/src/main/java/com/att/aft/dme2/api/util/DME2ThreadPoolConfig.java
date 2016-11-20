/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2ThreadPoolConfig {
	private static final Logger logger = LoggerFactory.getLogger( DME2ThreadPoolConfig.class );
	
	private DME2Configuration dme2Config;
	private static Map<DME2Manager, DME2ThreadPoolConfig> thrPoolConfigMap = new HashMap<DME2Manager, DME2ThreadPoolConfig>();
	
	private transient ThreadFactory retryThreadFactory;
	private transient ThreadPoolExecutor retryThreadpool;
	
	private int retryThreadCorePoolSize;
	private int retryThreadMaxPoolSize;
	private long retryThreadTTL;
	
	private transient DME2Manager manager;
	
	private transient ThreadPoolExecutor webSocketThreadpool;
	private transient ThreadFactory wsThreadFactory;
	private int wsFactoryThreadCorePoolSize;
	private int wsFactoryThreadMaxPoolSize;
	private long wsFactoryThreadTTL;
	
	private transient ThreadFactory wsRetryThreadFactory;
	private transient ThreadPoolExecutor wsRetryThreadpool;
	
	private int wsRetryThreadCorePoolSize;
	private int wsRetryThreadMaxPoolSize;
	private long wsRetryThreadTTL;
	
	DME2ThreadPoolConfig(DME2Manager manager){
		this.dme2Config = manager.getConfig();
		this.manager = manager;
		retryThreadCorePoolSize = dme2Config.getInt("DME2_EXCHANGE_RETRY_TPOOL_CORESIZE");
		retryThreadMaxPoolSize = dme2Config.getInt("DME2_EXCHANGE_RETRY_TPOOL_MAXSIZE");
		retryThreadTTL = dme2Config.getInt("DME2_EXCHANGE_RETRY_TPOOL_TTL");
		wsFactoryThreadCorePoolSize = dme2Config.getInt("DME2_WS_FACTORY_TPOOL_CORESIZE");
		wsFactoryThreadMaxPoolSize = dme2Config.getInt("DME2_WS_FACTORY_TPOOL_MAXSIZE");
		wsFactoryThreadTTL = dme2Config.getInt("DME2_WS_FACTORY_TPOOL_TTL");
		wsRetryThreadCorePoolSize = dme2Config.getInt("DME2_WS_RETRY_TPOOL_CORESIZE");
		wsRetryThreadMaxPoolSize = dme2Config.getInt("DME2_WS_RETRY_TPOOL_MAXSIZE");
		wsRetryThreadTTL = dme2Config.getInt("DME2_WS_RETRY_TPOOL_TTL");
	}
	
	public static DME2ThreadPoolConfig getInstance(DME2Manager manager){
		
		DME2ThreadPoolConfig config = thrPoolConfigMap.get(manager);
		
		if(config == null){
			config = new DME2ThreadPoolConfig(manager);			
			thrPoolConfigMap.put(manager, config);
		}
		
		return config;
	}
	
	public ThreadFactory createThreadFactory(){
		
		if(retryThreadFactory == null){
			
			retryThreadFactory = new ThreadFactory(){
				private int counter = 0;

				@Override
				public Thread newThread(Runnable r) {
					
					Thread t = new Thread(r);
					String name = "";
					
					try {
						 name = Thread.currentThread().getName();
					} catch (Exception e) {
						logger.debug( null, "createThreadFactory", LogMessage.DEBUG_MESSAGE, "Exception",e);
					}
					
					t.setName("DME2::ExchangeRetryThread[" + name + "] - " + counter++);
					t.setDaemon(true);
					return t;
				}
			};
		}
		return retryThreadFactory;
	}
	
	public ThreadPoolExecutor createExchangeRetryThreadPool(){
		if(retryThreadpool == null){
			
			retryThreadpool = new ThreadPoolExecutor(retryThreadCorePoolSize, retryThreadMaxPoolSize, retryThreadTTL,
					TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true), this.createThreadFactory());
		}
		return retryThreadpool;
	}

	public void setManager(DME2Manager manager) {
		this.manager = manager;
	}
	
	public ThreadPoolExecutor createWebSocketFactoryThreadPool(){
		if(webSocketThreadpool == null) {			
			webSocketThreadpool = new ThreadPoolExecutor(wsFactoryThreadCorePoolSize, wsFactoryThreadMaxPoolSize, wsFactoryThreadTTL,
					TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true), this.createWsThreadFactory());
		}
		return webSocketThreadpool;
	}

	private ThreadFactory createWsThreadFactory(){
		
		if(wsThreadFactory == null){
			
			wsThreadFactory = new ThreadFactory(){
				private int counter = 0;

				@Override
				public Thread newThread(Runnable r) {
					
					Thread t = new Thread(r);
					String name = "";
					
					try {
						 name = Thread.currentThread().getName();
					} catch (Exception e) {
						logger.debug( null, "createWsThreadFactory", LogMessage.DEBUG_MESSAGE, "Exception",e);
					}
					
					t.setName("DME2::WebSocketThread[" + name + "] - " + counter++);
					t.setDaemon(true);
					return t;
				}
			};
		}
		return wsThreadFactory;
	}
	
	public ThreadPoolExecutor createWebSocketRetryFactoryThreadPool(){
		if(wsRetryThreadpool == null) {			
			wsRetryThreadpool = new ThreadPoolExecutor(wsRetryThreadCorePoolSize, wsRetryThreadMaxPoolSize, wsRetryThreadTTL,
					TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true), this.createWsRetryThreadFactory());
		}
		return wsRetryThreadpool;
	}

	private ThreadFactory createWsRetryThreadFactory(){
		
		if(wsRetryThreadFactory == null){
			
			wsRetryThreadFactory = new ThreadFactory(){
				private int counter = 0;

				@Override
				public Thread newThread(Runnable r) {
					
					Thread t = new Thread(r);
					String name = "";
					
					try {
						 name = Thread.currentThread().getName();
					} catch (Exception e) {
						logger.debug( null, "createWsRetryThreadFactory", LogMessage.DEBUG_MESSAGE, "Exception",e);
					}
					
					t.setName("DME2::WebSocketRetryThread[" + name + "] - " + counter++);
					t.setDaemon(true);
					return t;
				}
			};
		}
		return wsRetryThreadFactory;
	}
}
