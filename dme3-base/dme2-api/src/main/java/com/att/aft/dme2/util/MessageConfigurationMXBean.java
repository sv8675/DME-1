package com.att.aft.dme2.util;

public interface MessageConfigurationMXBean 
{
	public void setMessageConfiguration( String code, String config ) throws Exception;
	public void setDefaultMessageConfiguration( String config ) throws Exception;
	
	public String getMessageConfiguration( String code ) throws Exception;
	public String getDefaultMessageConfiguration() throws Exception;
}
