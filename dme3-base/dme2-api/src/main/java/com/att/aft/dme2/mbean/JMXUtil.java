package com.att.aft.dme2.mbean;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;


public class JMXUtil 
{
	private static final Logger logger = LoggerFactory.getLogger( JMXUtil.class.getCanonicalName() );

	public static JMXUtil iNSTANCE = new JMXUtil();

	private final MBeanServer jmxPlatform = ManagementFactory.getPlatformMBeanServer();

	public <T> void registerMXAsStandardMBean(String name, T mxBean, Class<T> mxInterface)
	{
		logger.debug( null, null, "registerMXAsStandardMBean", LogMessage.JMX_REGISTER, name, mxBean );
		try 
		{
			final ObjectName objectName = new ObjectName(name);
			jmxPlatform.registerMBean(new StandardMBean(mxBean, mxInterface, true), objectName);
		} 
		catch(Exception e) 
		{
			// ignore attempts to re-register
			if(e instanceof InstanceAlreadyExistsException || e.getCause() instanceof InstanceAlreadyExistsException){
				logger.debug( null, "registerMXAsStandardMBean", "Already Registered {}", new ErrorContext().add("DME2MBean", name));
				return;
			}

			// otherwise log errors
			logger.error( null, "registerMXAsStandardMBean", "AFT-DME2-1900 {}", new ErrorContext().add("DME2MBean", name), e);
		}	
	}

	public <T> void registerMXBean(String name, T mxBean)
	{
		try 
		{
			final ObjectName objectName = new ObjectName(name);
			jmxPlatform.registerMBean(mxBean, objectName);
		} 
		catch(Exception e) 
		{
			// ignore attempts to re-register
			if(e instanceof InstanceAlreadyExistsException || e.getCause() instanceof InstanceAlreadyExistsException){
				return;
			}

			// otherwise log errors
			logger.error( null, "registerMXBean", "AFT-DME2-1900 {}", new ErrorContext().add("DME2MBean", name), e);
		}	
	}
}
