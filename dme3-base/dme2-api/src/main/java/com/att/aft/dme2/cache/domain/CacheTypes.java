/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.domain;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;

@XmlRootElement(name="CacheTypes", namespace="CacheTypes")
@XmlAccessorType(XmlAccessType.FIELD)
public class CacheTypes 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheTypes.class.getName());

	private static final Map<String, CacheTypeElement> cacheTypeMap = new HashMap<String, CacheTypeElement>();
	@XmlElement(name="CacheType")
	private List<CacheTypeElement> typeList;
	private DME2Configuration config = null;
	
	private CacheTypes(){}

	public static CacheTypes getInstance(final DME2Configuration config)
	{
		
		return CacheTypesLoader.loadCacheTypesConfigXML(config);
	}
	
	public static CacheTypeElement getType(final String typeName, final DME2Configuration config)
	{
		for(CacheTypeElement type: getCacheTypes(config))
		{
			if(typeName!=null && typeName.equalsIgnoreCase(type.getName()))
			{
				return type;
			}
		}
		return null;
	}
	
	public static List<CacheTypeElement> getCacheTypes(final DME2Configuration config)
	{
		return CacheTypes.getInstance(config).typeList;
	}

	public static List<String> getCacheTypeNames(final DME2Configuration config)
	{
		List<String> keys = new ArrayList<String>();
		for(CacheTypeElement type: getCacheTypes(config))
		{
			keys.add(type.getName());
		}
		return keys;
	}

	static class CacheTypesLoader 
	{
		//public static final CacheTypes cacheTypes = loadCacheTypesConfigXML();

		private static CacheTypes loadCacheTypesConfigXML(final DME2Configuration config)
		{
			CacheTypes cacheTypes = null;

			JAXBContext jc;
			try {
				jc = JAXBContext.newInstance(CacheTypes.class);

				Unmarshaller unmarshaller = jc.createUnmarshaller();
				
				File cacheTypeConfigXmlFile = null;
				
				//TODO: redundant to have two config param to configure cache-types. refactor to have only 1 name
				
				try{
					LOGGER.debug(null, "loadCacheTypesConfigXML", "loading cache type config file from dme2configuration setup");
					cacheTypeConfigXmlFile = new File(config.getProperty(DME2Constants.Cache.CACHE_TYPE_CONFIG_FILE_PATH));
					LOGGER.debug(null, "loadCacheTypesConfigXML", "loaded cache type config file from dme2configuration setup: [{}]", cacheTypeConfigXmlFile.getAbsoluteFile());
				}catch(NullPointerException npe){
					throw new CacheException(ErrorCatalogue.CACHE_020);
				}
				
				if(cacheTypeConfigXmlFile!=null && !cacheTypeConfigXmlFile.isFile()){
					LOGGER.debug(null, "loadCacheTypesConfigXML", "cache type config file is not available through the dme2configuration setup; loading cache type config file [{}] from classpath", config.getProperty(DME2Constants.Cache.CACHE_TYPE_CONFIG_FILE_NAME));
					//if the file is not present as absolute path, then try to load it from the classpath
					URL cacheTypeConfigFileUrl = CacheTypesLoader.class.getResource(config.getProperty(DME2Constants.Cache.CACHE_TYPE_CONFIG_FILE_NAME));
					if(cacheTypeConfigFileUrl!=null){
						try{
							cacheTypeConfigXmlFile = new File( cacheTypeConfigFileUrl.getFile() );
							LOGGER.debug(null, "loadCacheTypesConfigXML", "successfully loaded cache type config file from classpath location [{}] ", cacheTypeConfigFileUrl.getFile());
						}catch(NullPointerException npe){
							LOGGER.debug(null, "loadCacheTypesConfigXML", "failed to load cache type config file from classpath; path [{}] of the resource [{}] has some issue", cacheTypeConfigFileUrl.getFile(), config.getProperty(DME2Constants.Cache.CACHE_TYPE_CONFIG_FILE_NAME));
							throw new CacheException(ErrorCatalogue.CACHE_020);
						}
					}else{
						LOGGER.debug(null, "loadCacheTypesConfigXML", "failed to load cache type config file from classpath");
					}
				}else{
					LOGGER.debug(null, "loadCacheTypesConfigXML", "successfully loaded cache type config file from dme2configuration setup");
				}
				if(cacheTypeConfigXmlFile!=null){
					cacheTypes = (CacheTypes)  unmarshaller.unmarshal(CacheTypesLoader.class.getResourceAsStream( config.getProperty(DME2Constants.Cache.CACHE_TYPE_CONFIG_FILE_NAME)));
					Marshaller marshaller = jc.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
					marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "CacheTypes.xsd");
				} else if (!cacheTypeConfigXmlFile.isFile()) {
					InputStream in = null;
					BufferedReader reader = null;
					try {
						in = CacheTypes.class.getResourceAsStream("/conf/cache-types.xml"); 
						reader = new BufferedReader(new InputStreamReader(in));
						cacheTypes = (CacheTypes) unmarshaller.unmarshal(reader);
						Marshaller marshaller = jc.createMarshaller();
						marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
						marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "CacheTypes.xsd");
					} finally {
						try {
							in.close();	
						} catch (Exception e) {}
						try {
							reader.close();
						} catch (Exception e) {}
					}
				}else{
					throw new CacheException(ErrorCatalogue.CACHE_019);
				}
			} catch (JAXBException e) 
			{
			}
			
			return cacheTypes;
		}

	}
}
