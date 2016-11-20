/**
 * 
 */
package com.att.aft.dme2.factory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.domain.CacheTypeElement;
import com.att.aft.dme2.cache.domain.CacheTypes;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.cache.handler.service.CacheEventHandler;
import com.att.aft.dme2.cache.handler.service.CacheableDataHandler;
import com.att.aft.dme2.cache.service.CacheSerialization;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.cache.service.DME2CacheManager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/**
 * 
 */
public class DME2CacheFactory 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DME2CacheFactory.class.getName());

	private DME2CacheFactory() {
	}
	
	public static CacheSerialization getCacheSerializer(final DME2Cache cache, final DME2Configuration config, final boolean isEndpointCache) throws DME2Exception{
		Object cacheSerializationInstance = null;
		
		String userClass = config.getProperty(DME2Constants.Cache.CACHE_SERIALIZER_CLASS);

		LOGGER.debug( null, "getCacheSerializer", "Attempting to instantiate cache serializer{}", userClass );
		try {
			cacheSerializationInstance = (CacheSerialization) Class.forName(userClass).getDeclaredConstructor(DME2Cache.class, DME2Configuration.class, boolean.class).newInstance(cache, config, isEndpointCache);
			LOGGER.debug(null,"getCacheManager", "created: cacheManagerInstance [{}];", cacheSerializationInstance);
			
			if(null != cacheSerializationInstance && cacheSerializationInstance instanceof CacheSerialization) {
				LOGGER.debug(null,"getCacheManager", "end:success");
				return (CacheSerialization) cacheSerializationInstance;
			}else {
				ErrorContext ec = new ErrorContext();
				ec.add(DME2Constants.Cache.CACHE_SERIALIZATION_EXCEPTION + userClass,
						DME2Constants.Cache.CACHE_SERIALIZER_IMPLEMENTATION_EXCEPTION + userClass);
				LOGGER.error(null,
						DME2Constants.Cache.CACHE_SERIALIZER_IMPLEMENTATION_EXCEPTION + userClass,
						DME2Constants.Cache.EXCEPTION_HANDLER_MSG, ec);
				LOGGER.debug(null,"getCacheSerializer", "end:failure");
				throw new DME2Exception(DME2Constants.Cache.EXCEPTION_HANDLER_MSG, ec);
			}
		} catch (IllegalAccessException | ClassNotFoundException 
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | InstantiationException e) {
			
			LOGGER.error(null, "getCacheSerializer", "{} {} {}", userClass, DME2Constants.Cache.CACHE_SERIALIZATION_EXCEPTION,
					DME2Constants.Cache.EXCEPTION_HANDLER_MSG, e);
			LOGGER.debug(null,"getCacheSerializer", "end:failure");
			throw new DME2Exception(userClass + DME2Constants.Cache.CACHE_SERIALIZATION_EXCEPTION,e);
		}
	}

	/**
	 * instantiate cache manager based on the specified configuration  
	 * @param config 
	 * @return instance of the cache manager using which cache would be created and operated
	 * @throws DME2Exception
	 */
	public static DME2CacheManager getCacheManager(DME2Configuration config) throws DME2Exception
	{
		Object cacheManagerInstance = null;
		
		String userCacheClass = config.getProperty(DME2Constants.Cache.DME2_CACHE_MANAGER);

		LOGGER.debug( null, "getCacheManager", "Attempting to instantiate cache {}", userCacheClass );
		try {
			//cacheManagerInstance = (DME2CacheManager) Class.forName(userCacheClass).getDeclaredConstructor(DME2Configuration.class).newInstance(config);
			Method getInstanceMethod = Class.forName(userCacheClass).getDeclaredMethod("getInstance", DME2Configuration.class);
			cacheManagerInstance = getInstanceMethod.invoke(null, config);
			LOGGER.debug(null,"getCacheManager", "created: cacheManagerInstance [{}];", cacheManagerInstance);
			
			if(null != cacheManagerInstance && cacheManagerInstance instanceof DME2CacheManager) {
				LOGGER.debug(null,"getCacheManager", "end:success");
				return (DME2CacheManager) cacheManagerInstance;
			}else {
				ErrorContext ec = new ErrorContext();
				ec.add(DME2Constants.Cache.CACHE_INSTANTIATION_EXCEPTION + userCacheClass,
						DME2Constants.Cache.CACHE_INTERFACE_IMPLEMENTATION_EXCEPTION + userCacheClass);
				LOGGER.error(null,
						DME2Constants.Cache.CACHE_INTERFACE_IMPLEMENTATION_EXCEPTION + userCacheClass,
						DME2Constants.Cache.EXCEPTION_HANDLER_MSG, ec);
				LOGGER.debug(null,"getCacheManager", "end:failure");
				throw new DME2Exception(DME2Constants.Cache.EXCEPTION_HANDLER_MSG, ec);
			}
		} catch (IllegalAccessException | ClassNotFoundException 
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			
			LOGGER.error(null, "getCacheManager", "{} {} {}", userCacheClass, DME2Constants.Cache.CACHE_INSTANTIATION_EXCEPTION,
					DME2Constants.Cache.EXCEPTION_HANDLER_MSG, e);
			LOGGER.debug(null,"getCacheManager", "end:failure");
			throw new DME2Exception(userCacheClass + DME2Constants.Cache.CACHE_INSTANTIATION_EXCEPTION,e);
		}
	}

	@SuppressWarnings("rawtypes")
	public static CacheableDataHandler getDataHandler(final CacheTypeElement cacheType)
	{	
		LOGGER.debug(null,"DME2CacheFactory.getDataHandler", "start: cache [{}];", cacheType.getName());
		CacheableDataHandler dataHandler = null;
		if(cacheType!=null)
		{
			String dataHandlerClassName = cacheType.getDataHandlerClassName();
			try
			{
				LOGGER.debug(null,"DME2CacheFactory.getDataHandler", "cacheType.getDataHandlerClass:[{}], cache: [{}];", dataHandlerClassName, cacheType.getName());
				Object classObj = null;
				if(dataHandlerClassName!=null && !dataHandlerClassName.isEmpty())
				{
					classObj = Class.forName(dataHandlerClassName).newInstance();
					if(classObj instanceof CacheableDataHandler)
					{
						//LOGGER.info(null,"DME2CacheFactory.getDataHandler", "method: [{}], cacheType.obj[{}], cache: [{}];", method, classObj, cacheType.getName());
						LOGGER.debug(null,"DME2CacheFactory.getDataHandler", "cacheType.obj[{}], cache: [{}];", classObj, cacheType.getName());

						//method.invoke(classObj, cacheType);

						dataHandler = (CacheableDataHandler)classObj;
					}
				}
			}catch(Exception e)
			{
				throw new CacheException(ErrorCatalogue.CACHE_012, e.getMessage(), cacheType.getName());
			}

			LOGGER.debug(null,"DME2CacheFactory.getDataHandler", "end: cache [{}];", cacheType.getName());
		}
		return dataHandler;
	}

	public static CacheEventHandler getEventHandler(String cacheName, final DME2Configuration config)
	{
		CacheEventHandler eventHandler = null;
		LOGGER.debug(null, "DME2CacheFactory.getEventHandler", "start: [{}]", cacheName);
		CacheTypeElement cacheType = CacheTypes.getType(cacheName, config);
		if(cacheType!=null)
		{
			String eventHandlerClass = cacheType.getDataHandlerClassName();
		}

		LOGGER.debug(null, "DME2CacheFactory.getEventHandler", "end: [{}]", cacheName);
		return eventHandler;
	}

	private static Method getMethodInstance(final Object classObject, final String method)
	{
		LOGGER.debug(null, "DME2CacheFactory.getMethodInstance", "start");
		Method methodInvoke = null;
		if(classObject != null)
		{
			Class c = classObject.getClass();
			LOGGER.debug(null,"getMethodInstance", "Method: {}", c.getSimpleName());
			Method[] allMethods = c.getDeclaredMethods();
			for (final Method m : allMethods) 
			{
				String mname = m.getName();
				LOGGER.debug(null, "DME2CacheFactory.getMethodInstance", "method: [{}]",mname);
				if (mname.equalsIgnoreCase(method)) 
				{
					LOGGER.debug(null, "DME2CacheFactory.getMethodInstance", "found method [{}] as requested",method);
					m.setAccessible(true);
					methodInvoke = m;
					break;
				}
			}
		}else
		{
			LOGGER.warn(null, "DME2CacheFactory.getMethodInstance", "warn - classObject is not initialized");
		}
		LOGGER.debug(null, "DME2CacheFactory.getMethodInstance", "complete");
		return methodInvoke;
	}
}