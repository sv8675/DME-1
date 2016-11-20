/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
/**
 * 
 */
package com.att.aft.dme2.cache.domain;


import java.io.Serializable;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * wrapper object of {@link com.att.aft.dme2.cache.domain.CacheElement.Value} to hold additional details  
 * related to the cache entry to be used for admin operations 
 * @author ab850e
 *
 */
@SuppressWarnings("rawtypes")
public class CacheElement implements Serializable  
{
	private static final Logger logger = LoggerFactory.getLogger( CacheElement.class );
	private static final long serialVersionUID = -6305361521575262096L;
	private boolean markedForRemoval = false;
	private long lastAccessedTime = 0L;
	private long expirationTime = 0L;
	private long creationTime = 0L;
	private long ttl = 0L;
	private Key key;
	private Value value;
	private int emptyCacheRefreshAttemptCount = -1;


	public boolean isMarkedForRemoval() {
		return markedForRemoval;
	}

	public void setMarkedForRemoval(boolean markedForRemoval) {
		this.markedForRemoval = markedForRemoval;
	}

	public int getEmptyCacheRefreshAttemptCount() {
		return emptyCacheRefreshAttemptCount;
	}

	public void setEmptyCacheRefreshAttemptCount(int emptyCacheRefreshAttemptCount) {
		this.emptyCacheRefreshAttemptCount = emptyCacheRefreshAttemptCount;
	}

	/**
	 * @return the ttl
	 */
	public long getTtl() {
		return ttl;
	}

	/**
	 * @param ttl the ttl to set
	 */
	public CacheElement setTtl(long ttl) {
		this.ttl = ttl;
		//logger.debug( null, "setTtl", "setting ttl for {} {} to {}", this, getKey().getKey(), ttl );
		setExpirationTime( System.currentTimeMillis() + getTtl());
		return this;
	}

	public Value getValue() {
		return value;
	}

	public CacheElement setValue(Value value) {
		this.value = value;
		return this;
	}

	public Key getKey()
	{
		return this.key;
	}
	
	public CacheElement setKey(Key key)
	{
		this.key = key;
		return this;
	}
	
	/**
	 * key object holder for cache
	 * @author ab850e
	 * @param <K> Key for the entry in cache for which the value has to be retrieved
	 */
	public static class Key<K> implements Serializable{
		private static final long serialVersionUID = 8000655792870489708L;
		private K key;

		public Key() {
		}

		public Key(K key) {
			this.key=key;
		}

		public K getKey() {
			return key;
		}
		public String getString() {
			
			if(key instanceof String)
			{
				return (String)key;
			}else
			{
				return key.toString();
			}
		}

		public void setKey(K key) {
			this.key = key;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("key [key=");
			builder.append(key);
			builder.append("]");
			return builder.toString();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			if(key.toString().equals("")){
				
			}
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
			Key other = (Key) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}
	}

	/**
	 * value object holder for the entry value of the cache
	 * @author ab850e
	 *
	 * @param <V> value of an entry in the cache
	 */
	public static class Value<V>  implements Serializable
	{
		private static final long serialVersionUID = -8835066406701834360L;
		
		private V value;

		public Value() {
		}

		public Value(V value) {
			this.value=value;
		}

		public V getValue() {
			return this.value;
		}

		public void setValue(V value) {
			this.value = value;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Value [value=");
			builder.append(value);
			builder.append("]");
			return builder.toString();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			Value other = (Value) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
	}

	public CacheElement() {
		
	}
	
	public long getLastAccessedTime() {
		return lastAccessedTime;
	}
	public CacheElement setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
		return this;
	}
	public long getExpirationTime() {
		return expirationTime;
	}
	public CacheElement setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
		//logger.debug( null, "setExpirationTime", "setting expiration time for {} to {}", getKey().getKey(), this.expirationTime );
		return this;
	}
	public long getCreationTime() {
		return creationTime;
	}
	public CacheElement setCreationTime(long creationTime) {
		this.creationTime = creationTime;
		return this;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "CacheElement [markedForRemoval=" + markedForRemoval + ", lastAccessedTime=" + lastAccessedTime
				+ ", expirationTime=" + expirationTime + ", creationTime=" + creationTime + ", ttl=" + ttl + ", key="
				+ key + ", value=" + value + ", emptyCacheRefreshAttemptCount=" + emptyCacheRefreshAttemptCount + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if( markedForRemoval )
			return false;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheElement other = (CacheElement) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
