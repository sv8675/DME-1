package com.att.aft.dme2.cache.service;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class CacheTaskScheduler extends Timer{
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheTaskScheduler.class.getName());
	private String taskName;
	
	public CacheTaskScheduler(String taskName, boolean isDaemon)
	{
		super(taskName, isDaemon);
	}
	
	public String getTaskName()
	{
		return this.taskName;
	}
	private static Method getMethodInstance(final Object classObject, final String method, final Object[] objs)
	{
		LOGGER.debug(null, "CacheTaskScheduler.getMethodInstance", "start");
		Method methodInvoke = null;
		if(classObject != null)
		{
			Class c = classObject.getClass();
			LOGGER.debug(null,"{}", c.getSimpleName());
			Method[] allMethods = c.getDeclaredMethods();
			for (final Method m : allMethods) 
			{
				String mname = m.getName();
				//LOGGER.info(null, "CacheTaskScheduler.getMethodInstance", "params: [{}]",m.getParameterTypes().length);
				if (mname.equalsIgnoreCase(method)&& ((objs==null&&m.getParameterTypes().length==0)||(objs!=null))) 
				{
					LOGGER.debug(null, "CacheTaskScheduler.getMethodInstance", "found method [{}] as requested",method);
					m.setAccessible(true);
					
					methodInvoke = m;
					break;
				}
			}
		}else
		{
			LOGGER.warn(null, "CacheTaskScheduler.getMethodInstance", "warn - classObject is not initialized");
		}
		LOGGER.debug(null, "CacheTaskScheduler.getMethodInstance", "complete");
		return methodInvoke;
	}
	public static CacheTaskScheduler scheduleAtFixedRate(final String taskName, final boolean isDaemon, final long interval, final Object classObject, final String method) 
	{
		Object[] objs = null;
		return scheduleAtFixedRate(taskName, isDaemon, interval, classObject, method, objs);
	}
	public static CacheTaskScheduler scheduleAtFixedRate(final String taskName, final boolean isDaemon, final long interval, final Object classObject, final String method, final Object... objs) 
	{
		LOGGER.debug(null, "CacheTaskScheduler.scheduleAtFixedRate", "start");
		CacheTaskScheduler schedule = null;
		final Method methodToInvoke = getMethodInstance(classObject, method, objs);
		
		if(methodToInvoke!=null)
		{
			LOGGER.debug(null, "scheduleAtFixedRate","invoking method:{}, Object params: [{}]",methodToInvoke.getName(),objs);
			schedule = new CacheTaskScheduler(taskName, isDaemon);
			schedule.taskName = taskName;	
			schedule.scheduleAtFixedRate(new TimerTask()
			{
				@Override
				public void run()
				{
					try{
						if(methodToInvoke.getParameterTypes().length==0)
						{
							LOGGER.debug(null, "scheduleAtFixedRate","taskname {} method with no param", taskName);
							methodToInvoke.invoke(classObject);
						}else 
						{
							LOGGER.debug(null, "scheduleAtFixedRate","taskname {} objs is not null; param size:{}",taskName, objs.length);
							methodToInvoke.invoke(classObject, objs);
						}
					}catch (Exception e)
					{
						LOGGER.debug(null, "CacheTaskScheduler.scheduleAtFixedRate", "exception - {}, {}, {}", e.getMessage(), methodToInvoke, classObject);
						if(!(e.getCause() instanceof CacheException))
						{	
							throw new CacheException(ErrorCatalogue.CACHE_009,e,classObject.getClass().getSimpleName(), method);
						}
					}
				}
	
			}, interval, interval);
		
		}else
		{
			LOGGER.debug(null, "CacheTaskScheduler.scheduleAtFixedRate", "exception - method not found");
			throw new CacheException(ErrorCatalogue.CACHE_009,new Exception("method not found"),classObject!=null?classObject.getClass().getSimpleName():null, method);
		}
		LOGGER.debug(null, "CacheTaskScheduler.scheduleAtFixedRate", "completed");
		return schedule;
	}
}
