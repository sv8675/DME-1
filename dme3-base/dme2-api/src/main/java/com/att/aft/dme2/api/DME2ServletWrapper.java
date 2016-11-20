/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2ServletWrapper extends GenericServlet
{

	private static final long serialVersionUID = -8010643090961103789L;
	private static final Logger logger = LoggerFactory.getLogger( DME2ServletWrapper.class.getName() );
	
	private static final String DME2_SERVLET_INIT_PARAM_SERVICE_URI = "DME2_SERVLET_INIT_PARAM_SERVICE_URI";
	private static final String DME2_SERVLET_INIT_PARAM_CONTEXT_PATH = "DME2_SERVLET_INIT_PARAM_CONTEXT_PATH";
	private static final String DME2_SERVLET_INIT_PARAM_PORT = "DME2_SERVLET_INIT_PARAM_PORT";
	private static final String DME2_SERVLET_INIT_PARAM_PROTOCOL = "DME2_SERVLET_INIT_PARAM_PROTOCOL";
	private static final String DME2_SERVLET_INIT_PARAM_HOST = "DME2_SERVLET_INIT_PARAM_HOST";
	
	private static final String DME2_SERVLET_INIT_CONFIG_FILE = "DME2_SERVLET_INIT_CONFIG_FILE";
	private static final String DME2_SERVLET_INIT_CONFIG_DEF_FILE = "dme2-servlet-init.properties";

	private DME2Manager mgr;
	
	/** The initial set of properties that are loaded from the configuration file. */
	private Properties initProperties;
	
	private final String id = generateRandomDME2ManagerID();
		
	/*The final properties that have been checked and validated. These properties will be used to publish the endpoint*/
	private final Map<String, Properties> serviceProperties = new HashMap<String, Properties>(); 
	
	
	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
	{
		StringBuilder builder = new StringBuilder();
		
		for(String key : serviceProperties.keySet())
		{
			builder.append(key);
			builder.append(" = ");
			builder.append(serviceProperties.get(key).toString());
			builder.append("\n\n");
		}
		
		res.getWriter().write(serviceProperties.toString());
		res.getWriter().flush();
	
	}
	
	@Override
	public void init() throws ServletException
	{
		logger.debug(null, "init", LogMessage.METHOD_ENTER);
		super.init();
			
		try
		{
			initProperties = loadServletInitProperties();
			Map<String, Properties> indexedProperties = organizePropertiesByIndex(initProperties);
			
			Set<String> indexes = indexedProperties.keySet();
			for(String index : indexes)
			{
				Properties props = indexedProperties.get(index);
				setServletInitParameters(props, index);
			}	
			
			publishService();
		}
		catch (DME2Exception e)
		{
			logger.error(null, "init", LogMessage.ERROR_PUBLISHING, e);
			
			String exitOnServletInitFailure = System.getProperty("AFT_DME2_SYSTEM_EXIT_ON_SERVLET_INIT_FAILURE", "false");
			if(exitOnServletInitFailure.equalsIgnoreCase("true"))
			{
				logger.info(null, "init", LogMessage.DEBUG_MESSAGE, "System exiting due to exception when initializing DME2ServletWrapper.");
				System.exit(1);
			}
			
			throw new ServletException(e);
		}
		
		logger.debug(null, "init", LogMessage.METHOD_EXIT);
	}
	
	@Override
	public void destroy()
	{
		super.destroy();
		logger.debug(null, "destroy",  LogMessage.METHOD_ENTER);
		
		try
		{

			unpublishService();
		}
		catch (DME2Exception e)
		{
			logger.warn(null, "destroy",  LogMessage.UNPUBLISH_IGNORABLE, e);
		}
		
		logger.debug(null, "destroy", LogMessage.METHOD_EXIT);
	}
	
	
	/** Checks for the required DME2 ServletConfig parameters, performs validations and sets the values locally on the DME2ServletWrapper */
	private void setServletInitParameters(Properties config, String index) throws ServletException
	{
		logger.debug(null, "setServletInitParameters", LogMessage.METHOD_ENTER);
		Properties tempProps = new Properties();
		
		String serviceURI = getServletInitProperty(DME2_SERVLET_INIT_PARAM_SERVICE_URI + "." +  index, config, null);
		if (serviceURI == null || serviceURI.isEmpty()) /* Throw exception if serviceURI String is null or empty */
		{
			ErrorContext ec = new ErrorContext();
			ec.add("serviceURI", serviceURI);
			
			DME2Exception e = new DME2Exception("AFT-DME2-6800", ec);
			throw new ServletException(e);
		}
		tempProps.put(DME2_SERVLET_INIT_PARAM_SERVICE_URI + "." + index, serviceURI);
		
		
		String contextPath = getServletInitProperty(DME2_SERVLET_INIT_PARAM_CONTEXT_PATH + "." +  index, config, null);
		if(contextPath != null) {
			tempProps.put(DME2_SERVLET_INIT_PARAM_CONTEXT_PATH + "." + index, contextPath);
		}
		
		String portStr = getServletInitProperty(DME2_SERVLET_INIT_PARAM_PORT + "." +  index, config, "8080");
			
		/*Convert the port string into in integer*/
		try
		{
			int port = Integer.parseInt(portStr);
			tempProps.put(DME2_SERVLET_INIT_PARAM_PORT + "." + index, String.valueOf(port));
		}
		catch (NumberFormatException e)
		{
			ErrorContext ec = new ErrorContext();
			ec.add("serviceURI", serviceURI);
			ec.add("servicePort", portStr);
			
			DME2Exception ex = new DME2Exception("AFT-DME2-6803", ec);
			throw new ServletException(ex);
		}
		
		
		String protocol = getServletInitProperty(DME2_SERVLET_INIT_PARAM_PROTOCOL + "." +  index, config, DME2Constants.HTTP);	
		tempProps.put(DME2_SERVLET_INIT_PARAM_PROTOCOL + "." + index, protocol);
		
		String hostAddress = null;
		try
		{
			hostAddress = getServletInitProperty(DME2_SERVLET_INIT_PARAM_HOST + "." +  index, config, InetAddress.getLocalHost().getCanonicalHostName());
			tempProps.put(DME2_SERVLET_INIT_PARAM_HOST + "." + index, hostAddress);
		}
		catch (UnknownHostException e)
		{
			ErrorContext ec = new ErrorContext();
			ec.add("serviceURI", serviceURI);
			ec.add("host", hostAddress);
			
			DME2Exception ex = new DME2Exception("AFT-DME2-6501", ec);
			throw new ServletException(ex);
		}
		
		serviceProperties.put(index, tempProps);
		logger.debug(null, "setServletInitParameters", LogMessage.METHOD_EXIT);
	}
	
	
	private static String  generateRandomDME2ManagerID()
	{
		return "DME2ManagerID - " + UUID.randomUUID().toString();
	}
	
	private void publishService() throws DME2Exception
	{
		logger.debug(null, "publishService", LogMessage.METHOD_ENTER);
		
		if(mgr == null)
		{
			DME2Configuration config = new DME2Configuration(id, initProperties);
			mgr = new DME2Manager(id, config);
		}
		
		
		Set<String> indexes = serviceProperties.keySet();
		for(String index : indexes)
		{
			Properties props = serviceProperties.get(index);
			String indexSuffix = "." + index;
			
			String serviceURI =  props.getProperty(DME2_SERVLET_INIT_PARAM_SERVICE_URI + indexSuffix);
			String contextPath = props.getProperty(DME2_SERVLET_INIT_PARAM_CONTEXT_PATH + indexSuffix);
			String hostAddress = props.getProperty(DME2_SERVLET_INIT_PARAM_HOST + indexSuffix);
			String port = props.getProperty(DME2_SERVLET_INIT_PARAM_PORT + indexSuffix);
			String protocol = props.getProperty(DME2_SERVLET_INIT_PARAM_PROTOCOL + indexSuffix);
			
			try
			{
				mgr.getEndpointRegistry().publish(serviceURI, contextPath, hostAddress, Integer.parseInt(port), protocol);
				logger.debug(null, "publishService", LogMessage.PUBLISH_ENDPOINT, hostAddress, port);
			}
			catch (Exception e)
			{
				logger.error(null, "publishService", LogMessage.ERROR_PUBLISHING, serviceURI);
			}	
			
		}
		
		
		logger.debug(null, "publishService", LogMessage.METHOD_EXIT);
	}
	
	private void unpublishService() throws DME2Exception
	{
		logger.debug(null, "unpublishService", LogMessage.METHOD_ENTER);
		
		Set<String> indexes = serviceProperties.keySet();
		for(String index : indexes)
		{
			Properties props = serviceProperties.get(index);
			String indexSuffix = "." + index;
			
			String serviceURI =  props.getProperty(DME2_SERVLET_INIT_PARAM_SERVICE_URI + indexSuffix);
			String hostAddress = props.getProperty(DME2_SERVLET_INIT_PARAM_HOST + indexSuffix);
			String port = props.getProperty(DME2_SERVLET_INIT_PARAM_PORT + indexSuffix);
			
			try
			{
				mgr.getEndpointRegistry().unpublish(serviceURI, hostAddress, Integer.parseInt(port));
				logger.debug(null, "unpublishService", LogMessage.UNPUBLISHED, hostAddress, port);
			}
			catch (Exception e)
			{
				logger.error(null, "unpublishService", LogMessage.ERROR_UNPUBLISHING, serviceURI);
			}
		}
		
		logger.debug(null, "unpublishService", LogMessage.METHOD_EXIT);
	}
	
	
	private Properties loadServletInitProperties() throws DME2Exception
	{
		Properties props = new Properties();

		ClassLoader[] cls = new ClassLoader[] { ClassLoader.getSystemClassLoader(),
				DME2ServletWrapper.class.getClassLoader(), 
				Thread.currentThread().getContextClassLoader() };

		String fileName = resolveServletInitConfigFile();
		boolean isLoaded = false;

		for (ClassLoader cl : cls)
		{
			InputStream in = cl.getResourceAsStream(fileName);

			if(cl.getResource(fileName) != null)
			{
				logger.info(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, "Loading props from: " + cl.getResource(fileName));

				if (in != null)
				{
					try
					{
						props.load(in);
						isLoaded = true;
						logger.info(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, "Successfully loaded props from: " + cl.getResource(fileName));
						break;
					}
					catch (Exception e)
					{
						DME2Exception ex =  new DME2Exception("AFT-DME2-9999", "IOException Occurred while loading configuration", e);
						logger.warn(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, ex);	
					}
					finally
					{
						if(in != null)
						{
							try{ in.close(); }
							catch(IOException e){ 
								logger.debug(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, "IOException",e);
							}
						}
					}
				}
			}
		}

		
		if(isLoaded) {
			return props;
		}
		
		/* Could not load the file from classpath, trying to load from external source */
		InputStream in = null;
		try
		{
			logger.info(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, "Loading props from: " + fileName);
			in = new FileInputStream(fileName);
			props.load(in);
		}
		catch (IOException e)
		{
			DME2Exception ex =  new DME2Exception("AFT-DME2-9999", "IOException Occurred while loading configuration", e);
			logger.warn(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, ex);	
		}
		finally
		{
			if(in != null)
			{
				try{ in.close(); }
				catch(IOException e){ 
					logger.debug(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, "IOException",e);
				}
			}
		}

		return props;

	}
	
	/** Organizes the properties that are read in from the Servlet Init Configuration file. The file can support attributes for multiple services,
	 * so for each service, each property must have an index value appended to the property name 
	 * (example: <b>DME2_SERVLET_INIT_PARAM_SERVICE_URI.1</b>, <b>DME2_SERVLET_INIT_PARAM_SERVICE_URI.2</b>). This method logically groups the properties
	 * by their index number. */
	private Map<String, Properties> organizePropertiesByIndex(Properties properties)
	{
		Map<String, Properties> indexedProperties = new HashMap<String, Properties>();
		
		/*If the properties map is empty at this point, it means that the application was not able to resolve the properties from a config file.
		 * Next step is to see if they were set directly in the web.xml. If not empty, we skip the following block and continue with organizing
		 * the properties that were resolved from the file.*/
		if(properties.isEmpty())
		{
			ServletConfig config = getServletConfig();
			Enumeration<?> e = config.getInitParameterNames();
			
			while(e.hasMoreElements())
			{
				String key = (String) e.nextElement();
				properties.setProperty(key + "." + config.getServletName(), getInitParameter(key));
			}
		}
		
		
		Set<String> propertyIndex = new HashSet<String>();
		Set<Object> propertyKeys = properties.keySet();

		for (Object key : propertyKeys)
		{
			String keyStr = (String) key;
			String[] tokens = keyStr.split("\\.");

			if (tokens.length == 2)
			{
				propertyIndex.add(tokens[1]);
			}
		}

		for (String index : propertyIndex)
		{
			Properties props = new Properties();
			for (Object obj : properties.keySet())
			{
				String key = (String) obj;
				if (key.contains("." + index))
				{
					props.put(key, properties.get(key));
				}

			}

			indexedProperties.put(index, props);
		}

		return indexedProperties;
	}
	
	/** Tries to find the file the load the Servlet Init Configuration from (If provided). This method will check if the file was provided
	 * as part of the init params of the web.xml, or the JVM arguments.*/
	private String resolveServletInitConfigFile()
	{
		String fileName = getInitParameter(DME2_SERVLET_INIT_CONFIG_FILE);
		if(fileName == null)
		{
			fileName = System.getProperty("AFT_DME2_SERVLET_INIT_CONFIG_FILE", DME2_SERVLET_INIT_CONFIG_DEF_FILE);
			
		}

		logger.info(null, "loadServletInitProperties", LogMessage.DEBUG_MESSAGE, "Resolved Servlet init configuration file: " + fileName);		
		return fileName;
	}
	
	
	/** Resolves the property value for a given key. This method checks the properties map first, then the Servlet init configuration. If a value has
	 * not been resolved at this point then the default value is returned (if provided), otherwise null. */
	private String getServletInitProperty(String key, Properties props, String defaultValue)
	{
		String value = null;
		
		if(key == null){
			return defaultValue;
		}
		
		/*Try getting the value from the properties*/
		if(props != null)
		{
			value = props.getProperty(key);
		}
		
		/*If value is null at this point, try fetching it from the Servlet init config*/
		if(value == null)
		{
			value = getInitParameter(key);
		}
		
		/*If still null, use defaultValue*/
		if(value == null) {
			value = defaultValue;
		}
		
		String msg = String.format("Resolved property for key %s. Value is: %s", key, value);
		logger.debug(null, "getServletInitProperty", LogMessage.DEBUG_MESSAGE, msg);
		return value;
	}

}
