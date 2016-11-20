/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.scld.config;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.att.aft.scld.config.exception.ConfigException;

public class DefaultConfigurationManager extends ConfigurationManager {
	private  DefaultConfigurationManager configManager;
	private Map<String, ExecutorService> executorMap = null;
	
	//use thename of the threadpool to read the config values from the config file 
	//and create an executor service and configure it before returning. 
	//Maintain singleton instances of the executor service of each type - ? Should it be done here or in the manager - ?
	public ExecutorService getExecutor(ThreadPoolType threadPoolType) throws ConfigException {
		//if the executor service is already created then return it. Otherwise create a new one and add it to the map and return it.
		return null;
	}
	
	public DefaultConfigurationManager() throws ConfigException {
		super();		
	}

	/*public DME3EndpointRegistryIntf getEndpointRegistry() {
		//check the configuration and see if we need to use filesystem registry or GRM registry, 
		//create one of them, initialize and return
		
	}
	
	public DME3CacheManagerIntf getCacheManager() {
		//Use the factory and check the configuration and see if we need to use default or ehcache or another cache manager, 
		//create one of them, initialize and return
		
	} */

}
