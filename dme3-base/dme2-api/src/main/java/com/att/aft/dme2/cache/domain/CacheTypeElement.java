/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.cache.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class CacheTypeElement 
{
	@XmlAttribute
	private String name;
	private String dataHandler;
	@XmlElement(name="element_ttl")
	private long ttl=0;
	@XmlElement(name="refresh_interval")
	private long refreshInterval=0;

	@XmlElement(name="idle_timeout_check_interval")
	private long idleTimeoutCheckInterval=0;
	@XmlElement(name="idle_timeout")
	private long idleTimeout=0;
	@XmlElement(name="failed_refresh_retry_ttls")
	private long failedRefreshRetryTTLIntervals=0;
	@XmlElement(name="persistenceHandler")
	private String persistenceHandler;
	@XmlElement(name="dme_sep_cache_infrequent_ttl_ms")
	private long infrequentEndpointCacheTTL;
	@XmlElement(name="persist_frequency_ms")
	private long persistFrequencyMS;
	@XmlElement(name="cleanup_interval_ms")
	private long cleanupIntervalMS;

	public long getCleanupIntervalMS() {
		return cleanupIntervalMS;
	}

	public void setCleanupIntervalMS(long cleanupIntervalMS) {
		this.cleanupIntervalMS = cleanupIntervalMS;
	}

	public long getPersistFrequencyMS() {
		return persistFrequencyMS;
	}

	public void setPersistFrequencyMS(long persistFrequencyMS) {
		this.persistFrequencyMS = persistFrequencyMS;
	}

	public long getInfrequentEndpointCacheTTL() {
		return infrequentEndpointCacheTTL;
	}

	public void setInfrequentEndpointCacheTTL(long infrequentEndpointCacheTTL) {
		this.infrequentEndpointCacheTTL = infrequentEndpointCacheTTL;
	}

	public String getPersistenceHandler() {
		return persistenceHandler;
	}

	public void setPersistenceHandler(String persistenceHandler) {
		this.persistenceHandler = persistenceHandler;
	}

	public long getRefreshInterval() {
		return refreshInterval;
	}

	public long getTtl()
	{
		return ttl;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public String getDataHandlerClassName()
	{
		return dataHandler;
	}

	public long getIdleTimeout() {
		return idleTimeout;
	}

	public long getIdleTimeoutCheckInterval() {
		return idleTimeoutCheckInterval;
	}

	public void setDataHandler(String dataHandler) {
		this.dataHandler = dataHandler;
	}

	public void setIdleTimeoutCheckInterval(long idleTimeoutCheckInterval) {
		this.idleTimeoutCheckInterval = idleTimeoutCheckInterval;
	}

	public void setIdleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public void setFailedRefreshRetryTTLIntervals(
		long failedRefreshRetryTTLIntervals) {
	this.failedRefreshRetryTTLIntervals = failedRefreshRetryTTLIntervals;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CacheTypeElement [name=" + name + ", dataHandler=" + dataHandler + ", ttl=" + ttl + ", refreshInterval="
				+ refreshInterval + ", idleTimeoutCheckInterval=" + idleTimeoutCheckInterval + ", idleTimeout="
				+ idleTimeout + ", failedRefreshRetryTTLIntervals=" + failedRefreshRetryTTLIntervals
				+ ", persistenceHandler=" + persistenceHandler + ", infrequentEndpointCacheTTL="
				+ infrequentEndpointCacheTTL + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataHandler == null) ? 0 : dataHandler.hashCode());
		result = prime * result + (int) (failedRefreshRetryTTLIntervals ^ (failedRefreshRetryTTLIntervals >>> 32));
		result = prime * result + (int) (idleTimeout ^ (idleTimeout >>> 32));
		result = prime * result + (int) (idleTimeoutCheckInterval ^ (idleTimeoutCheckInterval >>> 32));
		result = prime * result + (int) (infrequentEndpointCacheTTL ^ (infrequentEndpointCacheTTL >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((persistenceHandler == null) ? 0 : persistenceHandler.hashCode());
		result = prime * result + (int) (refreshInterval ^ (refreshInterval >>> 32));
		result = prime * result + (int) (ttl ^ (ttl >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheTypeElement other = (CacheTypeElement) obj;
		if (dataHandler == null) {
			if (other.dataHandler != null)
				return false;
		} else if (!dataHandler.equals(other.dataHandler))
			return false;
		if (failedRefreshRetryTTLIntervals != other.failedRefreshRetryTTLIntervals)
			return false;
		if (idleTimeout != other.idleTimeout)
			return false;
		if (idleTimeoutCheckInterval != other.idleTimeoutCheckInterval)
			return false;
		if (infrequentEndpointCacheTTL != other.infrequentEndpointCacheTTL)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (persistenceHandler == null) {
			if (other.persistenceHandler != null)
				return false;
		} else if (!persistenceHandler.equals(other.persistenceHandler))
			return false;
		if (refreshInterval != other.refreshInterval)
			return false;
		if (ttl != other.ttl)
			return false;
		return true;
	}

	
	
}
