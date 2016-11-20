/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.api.util.DME2ThrottleConfig;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryFS;
import com.att.aft.dme2.manager.registry.DME2EndpointRegistryGRM;
import com.att.aft.dme2.mbean.DME2CacheMXBean;
import com.att.aft.dme2.mbean.DME2ThrottleMXBean;
import com.att.aft.dme2.mbean.JMXUtil;
import com.att.aft.dme2.server.mbean.DME2MXBean;

class DME2MXBeanMaster implements DME2MXBean {
  private static final Logger logger = LoggerFactory.getLogger( DME2MXBeanMaster.class );
	private static DME2MXBeanMaster master = new DME2MXBeanMaster();
	private final Map<String,DME2Manager> managers = new HashMap<String,DME2Manager>();
	private String loggingLevel = null;
	
	private DME2MXBeanMaster() {
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("JmxInterface:type=dme2", this, DME2MXBean.class);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:name=ManagerMaster", this, DME2MXBean.class);
		//JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=DiagnosticUtil,name=StatisticsAndMonitoring", new DiagnosticUtil(), DiagnosticMXBean.class);
	}
	
	static final DME2MXBeanMaster getInstance() {
		return master;
	}
	
	void addManager(DME2Manager manager)
	{
		managers.put(manager.getName(), manager);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=dme2Manager,name=" + manager.getName(), manager, DME2MXBean.class);
		// TODO: Does this need to be reinstated?
//		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=dme2Cache,name=StaleEndpointCache", manager.getStaleCache(), DME2CacheMXBean.class);
//		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=dme2Cache,name=StaleEndpointCache", (DME2CacheMXBean)manager.getStaleCache(), DME2CacheMXBean.class);
	}
	
	void addRegistryCache(DME2Manager manager, DME2EndpointRegistryFS registry)
	{
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=dme2CacheFS,name=StaleEndpointCache-"+manager.getName(), registry.getStaleEndpointCache().getCache(), DME2CacheMXBean.class);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=dme2CacheFS,name=StaleRouteOfferCache-"+manager.getName(), registry.getStaleRouteOfferCache().getCache(), DME2CacheMXBean.class);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=RegistryCacheFS,name=dme2EndpointCache-"+manager.getName(), registry.getEndpointCache().getCache(), DME2CacheMXBean.class);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=RegistryCacheFS,name=dme2RouteInfoCache-"+manager.getName(), registry.getRouteInfoCache().getCache(), DME2CacheMXBean.class);
	}

	void addRegistryCache(DME2Manager manager, DME2EndpointRegistryGRM registry)
	{
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=dme2CacheGRM,name=StaleEndpointCache-"+manager.getName(), registry.getStaleEndpointCache().getCache(), DME2CacheMXBean.class);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=dme2CacheGRM,name=StaleRouteOfferCache-"+manager.getName(), registry.getStaleRouteOfferCache().getCache(), DME2CacheMXBean.class);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=RegistryCacheGRM,name=dme2StaleEndpointCache-"+manager.getName(), registry.getStaleEndpointCache().getCache(), DME2CacheMXBean.class);
		JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=RegistryCacheGRM,name=dme2StaleRouteInfoCache-"+manager.getName(), registry.getStaleRouteOfferCache().getCache(), DME2CacheMXBean.class);
	}

	void addThrottleConfig(DME2ThrottleConfig throttleConfig, String name)
  {
    logger.debug( null, "addThrottleConfig", "Name: {}", name );

    JMXUtil.iNSTANCE.registerMXAsStandardMBean("com.att.aft.dme2:type=ThrottleConfig,name=DME2ThrottleConfig-"+ name, throttleConfig, DME2ThrottleMXBean.class);
  }

	@Override
	public boolean heartbeat() throws Exception {
		boolean ret = true;
		for (DME2Manager m: managers.values()) {
			if (!m.heartbeat()) {
				ret = false;
			}
		}
		return ret;
	}

	@Override
	public boolean shutdown() throws Exception {
		boolean ret = true;
		for (DME2Manager m: managers.values()) {
			if (!m.shutdown()) {
				ret = false;
			}
		}
		return ret;
	}

	@Override
	public boolean kill() throws Exception {
		boolean ret = true;
		for (DME2Manager m: managers.values()) {
			if (!m.kill()) {
				ret = false;
			}
		}
		return ret;
	}

	@Override
	public void refresh() throws Exception {
		for (DME2Manager m: managers.values()) {
			m.refresh();
		}
	}

	@Override
	public String statistics() throws Exception {
		StringBuffer buf = new StringBuffer();
		for (DME2Manager m: managers.values()) {
			buf.append("dme2Manager-" + m.getName() + "\n");
			buf.append("---------------------------\n");
			buf.append(m.statistics());
			buf.append("---------------------------\n\n\n");
		}
		return buf.toString();
		
	}

	@Override
	public String[] diagnostics() throws Exception {
		List<String> list = new ArrayList<String>();
		for (DME2Manager m: managers.values()) {
			list.add("------ dme2Manager ------");
			list.addAll(Arrays.asList(m.diagnostics()));
		}
		return list.toArray(new String[0]);
	}

	@Override
	public void dump() throws Exception {
		for (DME2Manager m: managers.values()) {
			m.dump();
		}
	}

	@Override
	public String getLoggingLevel() {
		return loggingLevel;
	}

	@Override
	public void setLoggingLevel(String newLoggingLevel) {
		loggingLevel = newLoggingLevel;
		for (DME2Manager m: managers.values()) {
			m.setLoggingLevel(newLoggingLevel);
		}		
	}

	@Override
	public void setProperty(String key, String value) {
		throw new RuntimeException("setProperty can only be called on specific Manager MBeans");
	}

	@Override
	public Properties getProperties() {
		throw new RuntimeException("getProperties can only be called on specific Manager MBeans");
	}

	@Override
	public void removeProperty(String key) {
		throw new RuntimeException("removeProperty can only be called on specific Manager MBeans");		
	}

	@Override
	public void disableMetrics() throws Exception {
		//TODO
	}

	@Override
	public void enableMetrics() throws Exception {
		//TODO
	}

	@Override
	public void disableMetricsFilter() {
		//TODO		
	}

	@Override
	public void enableMetricsFilter() {
		//TODO
	}
	@Override
	public void disableThrottleFilter() {
		//TODO	
	}

	@Override
	public void enableThrottleFilter() {
		//TODO		
	}
}
