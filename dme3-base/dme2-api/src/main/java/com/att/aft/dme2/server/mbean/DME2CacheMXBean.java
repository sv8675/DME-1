package com.att.aft.dme2.server.mbean;


public interface DME2CacheMXBean 
{
	public void clear();
	public int getCurrentSize();
	public long getCacheTTLValue( String key );
	public long getExpirationTime( String key );
	public String getKeys();
}
