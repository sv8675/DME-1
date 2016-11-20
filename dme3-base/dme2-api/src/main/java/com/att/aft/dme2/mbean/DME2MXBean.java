package com.att.aft.dme2.mbean;

import java.util.Properties;

public interface DME2MXBean {
	public boolean heartbeat()throws Exception;
	public boolean shutdown()throws Exception;
	public boolean kill()throws Exception;
	public void refresh()throws Exception;
	public String statistics()throws Exception;
	public String[] diagnostics()throws Exception;
	public void dump()throws Exception;
	public void disableMetrics() throws Exception;
	public void enableMetrics() throws Exception;
	public void disableMetricsFilter();
	public void enableMetricsFilter();
	public String getLoggingLevel();
  public void disableThrottleFilter();
  public void enableThrottleFilter();

	//operations
	public void setLoggingLevel(String newLoggingLevel);
	public void setProperty(String key, String value);
	public Properties getProperties();
	public void removeProperty(String key);
}
