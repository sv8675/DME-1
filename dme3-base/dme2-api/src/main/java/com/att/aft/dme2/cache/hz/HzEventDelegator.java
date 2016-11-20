package com.att.aft.dme2.cache.hz;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import com.att.aft.dme2.cache.AbstractCache;
import com.att.aft.dme2.cache.domain.CacheConfiguration;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.cache.domain.CacheEvent;
import com.att.aft.dme2.cache.domain.CacheEventType;
import com.att.aft.dme2.cache.exception.CacheException;
import com.att.aft.dme2.cache.exception.CacheException.ErrorCatalogue;
import com.att.aft.dme2.cache.handler.service.CacheEventHandler;
import com.att.aft.dme2.cache.service.DME2Cache;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.map.listener.MapClearedListener;
import com.hazelcast.map.listener.MapEvictedListener;

public class HzEventDelegator {
	private static final Logger LOGGER = LoggerFactory.getLogger(HzEventDelegator.class.getName());
	public DME2Cache cache;
	private CacheEventHandler eventHandler;

	public HzEventDelegator(DME2Cache cache, CacheConfiguration cacheConfig) 
	{
		this.cache = cache;
		this.eventHandler = cacheConfig.getEventHandler();
	}

	public void delegateEvent(CacheEvent cacheEvent)
	{
		LOGGER.debug(null,"delegateEvent","Delegate Cache EVENT: [{}]", cacheEvent.getCacheEventType().toString());
		try{
		//cache.lock(cacheEvent.getCacheOldElement().getKey());
		
		CacheEventType eventType = cacheEvent.getCacheEventType();
		
		switch(eventType)
		{
		case EVICT: 
			eventHandler.onEviction(cache,cacheEvent);
			break;
		default:
			throw new CacheException(ErrorCatalogue.CACHE_003,((AbstractCache)cache).getCacheName(), eventType);
		}
		
		}finally{
			//cache.unlock(cacheEvent.getCacheOldElement().getKey());
		}
	}

	class Interceptor implements MapInterceptor, Serializable 
	{
		private static final long serialVersionUID = 5521073120342397279L;
		private final AtomicLong AL_number = new AtomicLong(Math.round(Math.random()));
		private String randomId = String.valueOf(AL_number.incrementAndGet()).concat("-DME2ElementInterceptor");
		public Interceptor()
		{

		}

		private void convertToCacheEvent(CacheEventType event, Object oldValue, Object newValue)
		{
			LOGGER.debug(null,"convertToCacheEvent", "Event [{}]",event.toString());
			CacheEvent cacheEvent = new CacheEvent();
			cacheEvent.setCacheOldElement((CacheElement)oldValue);
			cacheEvent.setCacheNewElement((CacheElement)newValue);
			cacheEvent.setCacheEventType(event);
			
			delegateEvent(cacheEvent);
		}

		@Override
		public Object interceptGet( Object value ) 
		{
			return null; //null => no change
		}
		@Override
		public void afterGet( Object value ) 
		{
		}

		@Override
		public Object interceptPut( Object oldValue, Object newValue ) 
		{
			return null; //null => no change
		}

		@Override
		public void afterPut( Object value ) 
		{
		}

		@Override
		public Object interceptRemove( Object removedValue )
		{
			LOGGER.info(null,"interceptRemove", "removingValue - [{}]",removedValue.toString());
			try{
			convertToCacheEvent(CacheEventType.EVICT, removedValue, removedValue);
			}catch(Exception e)
			{
				LOGGER.info(null,"interceptRemove", "exception - [{}]",e.getMessage());
				throw e;
			}
			return removedValue;
		}
		@Override
		public void afterRemove( Object value ) 
		{
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((randomId == null) ? 0 : randomId.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Interceptor other = (Interceptor) obj;
			if (randomId == null) {
				if (other.randomId != null)
					return false;
			} else if (!randomId.equals(other.randomId))
				return false;
			return true;
		}
	}

	public class Listener  implements EntryAddedListener<Object, CacheElement>,
							EntryRemovedListener<Object, CacheElement>,
							EntryUpdatedListener<Object, CacheElement>,
							EntryEvictedListener<Object, CacheElement> ,
							MapEvictedListener,
							MapClearedListener
	{

		public Listener(){
		}

		private void delegateMapEvent(MapEvent event){
		}
		
		private void convertToCacheEvent(EntryEvent<Object, CacheElement> event)
		{
			LOGGER.debug(null,"convertToCacheEvent","Listen EVENT [{}]: preparing cache event; Existing value: [{}], New Value: [{}]", event.getEventType().toString(), event.getOldValue(), event.getValue());
			CacheEvent cacheEvent = new CacheEvent();

			cacheEvent.setCacheNewElement(event.getValue());
			cacheEvent.setCacheOldElement(event.getOldValue());

			switch(event.getEventType())
			{
			case EVICTED:
				cacheEvent.setCacheEventType(CacheEventType.EVICT);
				break;
			default:
				throw new CacheException(CacheException.ErrorCatalogue.CACHE_003, ((AbstractCache)cache).getCacheName(), event.getEventType());
			}

			delegateEvent(cacheEvent);
		}

		@Override
		public void mapCleared(MapEvent event) 
		{
			delegateMapEvent(event);
		}

		@Override
		public void mapEvicted(MapEvent event) 
		{
			delegateMapEvent(event);
		}

		@Override
		public void entryEvicted(EntryEvent<Object, CacheElement> event) 
		{
/*			LOGGER.debug(null,"{}: Start eviction: [{}]",cache.getCacheName(), event.getOldValue());
			
			convertToCacheEvent(event);
			LOGGER.debug(null,"{}: End eviction: [{}]",cache.getCacheName(), event.getOldValue());
*/		}

		@Override
		public void entryUpdated(EntryEvent<Object, CacheElement> event) 
		{
		}

		@Override
		public void entryRemoved(EntryEvent<Object, CacheElement> event) 
		{
		}

		@Override
		public void entryAdded(EntryEvent<Object, CacheElement> event) 
		{
		}
	}
}