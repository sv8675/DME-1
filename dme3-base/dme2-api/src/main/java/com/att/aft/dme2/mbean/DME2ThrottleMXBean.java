package com.att.aft.dme2.mbean;

public interface DME2ThrottleMXBean {
  	public float getThrottleConfigForPartner(String service, String partner);
  	public void setThrottleConfigForPartner(String service, String partner, float value);
}
