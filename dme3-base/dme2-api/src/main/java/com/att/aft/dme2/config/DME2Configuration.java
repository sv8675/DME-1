package com.att.aft.dme2.config;


import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.scld.config.ConfigurationManager;
import com.att.aft.scld.config.exception.ConfigException;
import com.att.aft.scld.config.strategy.ConfigurationStrategy;

public class DME2Configuration {

  public final static String DME2_DEFAULT_CONFIG_FILE_NAME = "dme-api_defaultConfigs.properties";
  public final static String DME2_DEFAULT_CONFIG_MANAGER_NAME = "dme2_config_manager";
  private static final Logger logger = LoggerFactory.getLogger( DME2Configuration.class.getName() );

  private ConfigurationManager configManager = null;
  private Properties properties = null;
  private String managerName;

  public DME2Configuration() {
    try {
      configManager = ConfigurationManager.getInstance(DME2_DEFAULT_CONFIG_MANAGER_NAME, DME2_DEFAULT_CONFIG_FILE_NAME);
    } catch (ConfigException e) {
        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );
    }
  }

	public DME2Configuration(String managerName) {
		try {
			this.managerName = managerName;
			configManager = ConfigurationManager.getInstance(managerName, DME2_DEFAULT_CONFIG_FILE_NAME);
		} catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );
		}
	}

	public DME2Configuration(String managerName, String fileName) {
		try {
			this.managerName = managerName;
			configManager = ConfigurationManager.getInstance(managerName, DME2_DEFAULT_CONFIG_FILE_NAME, fileName);
		} catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );

		}
	}

	public DME2Configuration(String managerName, String defaultConfigFileName, String fileName) {
		try {
			this.managerName = managerName;
			configManager = ConfigurationManager.getInstance(managerName, defaultConfigFileName, fileName);
		} catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );
		}
	}
	
	public DME2Configuration(String managerName, List<String> defaultConfigFileNames, String fileName) {
		try {
			this.managerName = managerName;
			configManager = ConfigurationManager.getInstance(managerName, defaultConfigFileNames, fileName, null, null);
		} catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );
		}
	}
	
	public DME2Configuration(String managerName, List<String> defaultConfigFileNames, String fileName, Properties properties) {
		try {
			this.managerName = managerName;
			this.properties = properties;
			
			PropertiesConfiguration propsConfig = new PropertiesConfiguration();
			propsConfig.setDelimiterParsingDisabled(true);
			if(properties != null) {
				Enumeration em = properties.keys();
				while(em.hasMoreElements()){
				  String key = (String)em.nextElement();
				  propsConfig.setProperty(key, properties.getProperty(key));
				}	
			}
			configManager = ConfigurationManager.getInstance(managerName, defaultConfigFileNames, fileName, null, propsConfig);
		} catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );			
		}
	}	

	public DME2Configuration(String managerName, String defaultConfigFileName, List<ConfigurationStrategy> configCommands) {
		try {
			this.managerName = managerName;
			configManager = ConfigurationManager.getInstance(managerName, defaultConfigFileName, configCommands);
		} catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );
		}
	}

  public DME2Configuration( String managerName, String defaultConfigFileName, PropertiesConfiguration userPropsConfig ) {
    try {
    	this.managerName = managerName;
    	configManager = ConfigurationManager.getInstance(managerName, defaultConfigFileName, userPropsConfig);
    } catch (ConfigException e) {
        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );
    }
  }
  
  public DME2Configuration( String managerName, Properties properties ) {
	    try {
	    	this.managerName = managerName;
	    	this.properties = properties;
			PropertiesConfiguration propsConfig = new PropertiesConfiguration();
			propsConfig.setDelimiterParsingDisabled(true);
			Enumeration em = properties.keys();
			while(em.hasMoreElements()){
			  String key = (String)em.nextElement();
			  propsConfig.setProperty(key, properties.getProperty(key));
			}			
 	    	configManager = ConfigurationManager.getInstance(managerName, DME2_DEFAULT_CONFIG_FILE_NAME, propsConfig);
	    } catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );

	        try {
	          configManager = ConfigurationManager.getInstance( managerName );
	        } catch ( ConfigException e2 ) {
	            logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e2 );
	
	        }
	    }
	  }
  
  public DME2Configuration( String managerName, String defaultConfigFileName, String fileName,
                            List<ConfigurationStrategy> configCommands, PropertiesConfiguration userPropsConfig ) {
	  try {
		  this.managerName = managerName;
		  configManager = ConfigurationManager.getInstance(managerName, defaultConfigFileName, fileName, configCommands, userPropsConfig);
	  } catch (ConfigException e) {
	        logger.error( null, "stop", LogMessage.CONFIGURATION_ERROR, e );		  
	  }
	}

  public String getProperty(String propertyName) {
    return configManager.getProperty(propertyName);
  }

  public String getProperty(String propertyName, String defaultValue) {
	  return configManager.getProperty(propertyName, defaultValue);
  }
  
  public int getInt(String propertyName) {
	  return configManager.getInt(propertyName);
  }

  public int getInt(String propertyName, int defaultValue) {
	  return configManager.getInt(propertyName, defaultValue);
  }
  
  public Integer getInteger(String propertyName, Integer defaultValue) {
	  String value = configManager.getProperty(propertyName);
	  if(value !=null && !value.equalsIgnoreCase("")) {
		  return Integer.parseInt(value);
	  }
	  return defaultValue;
  }
  
  public float getFloat(String propertyName) {
	  return configManager.getFloat(propertyName);
  }

  public float getFloat(String propertyName, float defaultValue) {
	  return configManager.getFloat(propertyName, defaultValue);
  }
  
  public long getLong(String propertyName) {
	  return configManager.getLong(propertyName);
  }

  public long getLong(String propertyName, long defaultValue) {
	  return configManager.getLong(propertyName, defaultValue);
  }
  
  public boolean getBoolean(String propertyName) {
	  return configManager.getBoolean(propertyName);
  }

  public boolean getBoolean(String propertyName, boolean defaultValue) {
	  return configManager.getBoolean(propertyName, defaultValue);
  }
  
  public Double getDouble( String propertyName ) {
	  return configManager.getDouble( propertyName );
  }
  
  public Double getDouble( String propertyName, double defaultValue ) {
	  return configManager.getDouble( propertyName, defaultValue );
  }
  
  public Properties getProperties() {
	  return properties;
  }

  public String getManagerName() {
	  return managerName;
  }

  public void setManagerName(String managerName) {
	  this.managerName = managerName;
  }
	
  public void setOverrideProperty(String key, String value) {
	  configManager.setPropertyforJmx(key, value);
  }

}