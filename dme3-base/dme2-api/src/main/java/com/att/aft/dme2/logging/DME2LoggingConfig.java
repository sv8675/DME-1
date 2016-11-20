package com.att.aft.dme2.logging;

import java.util.ResourceBundle;

import com.att.aft.dme2.util.ErrorCatalog;


/** 
 * This utility class is responsible for initializing
 * the DME error table and logging configuration on the startup
 * of a DME Manager instance.
 * */
@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class DME2LoggingConfig {
	private static final Logger logger = LoggerFactory.getLogger(DME2LoggingConfig.class.getCanonicalName());
	private static DME2LoggingConfig loggingConfig;
	
	private static ResourceBundle errorTable;
	
	private DME2LoggingConfig(){
		
	}
	
	public static DME2LoggingConfig getInstance(){
		if(loggingConfig == null){
			loggingConfig = new DME2LoggingConfig();
		}
		
		return loggingConfig;
	}
	
	public ResourceBundle initializeDME2ErrorTable(String baseName){
		
		 if(errorTable == null){
				try {
					errorTable = ResourceBundle.getBundle(baseName);
					ErrorCatalog.getInstance().addErrorTable(errorTable);
				} catch (Throwable e) {
					logger.error( null, "initializeDME2ErrorTable", LogMessage.ERRORTABLE_MISSING, e );
				}
		 }
		 
		 return errorTable;
	}
}

