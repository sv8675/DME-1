/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.types.DataPartition;
import com.att.aft.dme2.types.DataPartitions;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteGroups;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.types.VersionMap;
import com.att.aft.dme2.types.VersionMapInfo;
import com.att.aft.dme2.types.VersionMappings;

public class RouteInfoCreatorUtil {

  private static final Logger logger = LoggerFactory.getLogger( RouteInfoCreatorUtil.class );

	public static RouteInfo createRouteInfo(String serviceName, String serviceVersion, String env){

		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setDataPartitionKeyPath("xyz");
		routeInfo.setEnvContext(env);
		routeInfo.setServiceName(serviceName);
		//routeInfo.setServiceVersion(serviceVersion);
		
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("DME2_TEST_ROUTE_GROUP");
		routeGroup.getPartner().add("DME2_PARTNER");
		
		Route route = new Route();
		route.setName("DME2_TEST_ROUTE");

		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setName("DME2_PRIMARY");
		routeOffer1.setSequence(1);
		routeOffer1.setStalenessInMins((long) 1);
		
		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setName("DME2_SECONDARY");
		routeOffer2.setSequence(1);
		routeOffer2.setStalenessInMins((long) 1);
		
		
		route.getRouteOffer().add(routeOffer1);
		route.getRouteOffer().add(routeOffer2);
		
		routeGroup.getRoute().add(route);
		
		RouteGroups routeGroups =  new RouteGroups();
		routeGroups.getRouteGroup().add(routeGroup);
		
		routeInfo.setRouteGroups(routeGroups);
		
		return routeInfo;
	}
	
	public static RouteInfo createRouteInfoForRouteOfferFailoverHandlers(){
		
		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setServiceName("com.att.aft.dme2.TestDME2ExchangeRouteOfferFailoverHandlers");
		
		routeInfo.setEnvContext("DEV");
		
		RouteGroups routeGroups = new RouteGroups();
		routeInfo.setRouteGroups(routeGroups);
		
			
		Route route = new Route();
		route.setName("rt1");
		
		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setSequence(1);
		routeOffer1.setName("BAU_NE");
		route.getRouteOffer().add(routeOffer1);
		

		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setSequence(2);
		routeOffer2.setName("BAU_SE");
		
		route.getRouteOffer().add(routeOffer2);
		
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("RG1");
		routeGroup.getPartner().add("test1");
		routeGroup.getPartner().add("test2");
		routeGroup.getPartner().add("test3");
		routeGroup.getRoute().add(route);

		routeGroups.getRouteGroup().add(routeGroup);
		
		return routeInfo;
	}
	
	public static RouteInfo createRouteInfoWithStalenessInMins(String serviceName, String serviceVersion, String env){
		
		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setDataPartitionKeyPath("xyz");
		routeInfo.setEnvContext(env);
		routeInfo.setServiceName(serviceName);
		//routeInfo.setServiceVersion(serviceVersion);
		
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("DME2_TEST_ROUTE_GROUP");
		routeGroup.getPartner().add("DME2_PARTNER");
		
		Route route = new Route();
		route.setName("DME2_TEST_ROUTE");

		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setName("DME2_PRIMARY");
		routeOffer1.setSequence(1);
		routeOffer1.setStalenessInMins((long) 1);
		
		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setName("DME2_SECONDARY");
		routeOffer2.setSequence(1);
		routeOffer2.setStalenessInMins((long) 1);
		
		
		route.getRouteOffer().add(routeOffer1);
		route.getRouteOffer().add(routeOffer2);
		
		routeGroup.getRoute().add(route);
		
		RouteGroups routeGroups =  new RouteGroups();
		routeGroups.getRouteGroup().add(routeGroup);
		
		routeInfo.setRouteGroups(routeGroups);
		
		return routeInfo;
	}
	
	public static RouteInfo createRouteInfoForParseResponseFault(String serviceName, String serviceVersion, String env){
		
		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setEnvContext(env);
		routeInfo.setServiceName(serviceName);
		routeInfo.setServiceVersion(serviceVersion);
		
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("RG_1");
		routeGroup.getPartner().add("FAULT");
		
		Route route = new Route();
		route.setName("R_1");

		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setName("PRIMARY");
		routeOffer1.setSequence(1);
		
		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setName("SECONDARY");
		routeOffer2.setSequence(2);
		
		route.getRouteOffer().add(routeOffer1);
		route.getRouteOffer().add(routeOffer2);
		
		routeGroup.getRoute().add(route);
		
		RouteGroups routeGroups =  new RouteGroups();
		routeGroups.getRouteGroup().add(routeGroup);
		
		routeInfo.setRouteGroups(routeGroups);
		
		return routeInfo;
	}
	
	public static RouteInfo createRouteInfoWithPreferredRouteOffer(){
		
		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setServiceName("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer");
		//routeInfo.setServiceVersion("*");
		routeInfo.setEnvContext("DEV");
		
		RouteGroups routeGroups = new RouteGroups();
		routeInfo.setRouteGroups(routeGroups);
		
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("RG1");
		routeGroup.getPartner().add("test1");
		routeGroup.getPartner().add("test2");
		routeGroup.getPartner().add("test3");
		
		Route route = new Route();
		route.setName("rt1");
		
		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setSequence(1);
		routeOffer1.setName("BAU_NE");
		
		Route route2 = new Route();
		route2.setName("rt2");
		
		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setSequence(2);
		routeOffer2.setName("BAU_SE");
		
		Route route3 = new Route();
		route3.setName("rt3");
		
		RouteOffer routeOffer3 = new RouteOffer();
		routeOffer3.setActive(true);
		routeOffer3.setSequence(3);
		routeOffer3.setName("BAU_NW");
		
		route.getRouteOffer().add(routeOffer1);
		route.getRouteOffer().add(routeOffer2);
		route.getRouteOffer().add(routeOffer3);
		
		routeGroup.getRoute().add(route);
		//routeGroup.getRoute().add(route2);
		//routeGroup.getRoute().add(route3);
		
		routeGroups.getRouteGroup();
		routeGroups.getRouteGroup().add(routeGroup);
		
		return routeInfo;
	}
	
	
public static RouteInfo createRouteInfoWithPreferredRouteOfferAndAllROFails(){
		
		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setServiceName("com.att.aft.dme2.TestDME2ExchangePreferredRouteOfferAndAllROFails");
		//routeInfo.setServiceVersion("*");
		routeInfo.setEnvContext("DEV");
		
		RouteGroups routeGroups = new RouteGroups();
		routeInfo.setRouteGroups(routeGroups);
		
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("RG1");
		routeGroup.getPartner().add("test1");
		routeGroup.getPartner().add("test2");
		routeGroup.getPartner().add("test3");
		
		Route route = new Route();
		route.setName("rt1");
		
		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setSequence(1);
		routeOffer1.setName("BAU_NE");
		
		Route route2 = new Route();
		route2.setName("rt2");
		
		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setSequence(2);
		routeOffer2.setName("BAU_SE");
		
		Route route3 = new Route();
		route3.setName("rt3");
		
		RouteOffer routeOffer3 = new RouteOffer();
		routeOffer3.setActive(true);
		routeOffer3.setSequence(3);
		routeOffer3.setName("BAU_NW");
		
		route.getRouteOffer().add(routeOffer1);
		route.getRouteOffer().add(routeOffer2);
		route.getRouteOffer().add(routeOffer3);
		
		routeGroup.getRoute().add(route);
		//routeGroup.getRoute().add(route2);
		//routeGroup.getRoute().add(route3);
		
		routeGroups.getRouteGroup();
		routeGroups.getRouteGroup().add(routeGroup);
		
		return routeInfo;
	}

public static RouteInfo createRouteInfoWithFailoverWhenPrimaryDownOnStart(){
	
	RouteInfo routeInfo = new RouteInfo();
	routeInfo.setServiceName("com.att.aft.dme2.TestDME2ExchangeFailoverWhenPrimaryDown");
	//routeInfo.setServiceVersion("*");
	routeInfo.setEnvContext("DEV");
	
	RouteGroups routeGroups = new RouteGroups();
	routeInfo.setRouteGroups(routeGroups);
	
	RouteGroup routeGroup = new RouteGroup();
	routeGroup.setName("RG1");
	routeGroup.getPartner().add("test1");
	routeGroup.getPartner().add("test2");
	routeGroup.getPartner().add("test3");
	
	Route route = new Route();
	route.setName("rt1");
	
	RouteOffer routeOffer1 = new RouteOffer();
	routeOffer1.setActive(true);
	routeOffer1.setSequence(1);
	routeOffer1.setName("BAU_NE");
	
	Route route2 = new Route();
	route2.setName("rt2");
	
	RouteOffer routeOffer2 = new RouteOffer();
	routeOffer2.setActive(true);
	routeOffer2.setSequence(2);
	routeOffer2.setName("BAU_SE");
	
	Route route3 = new Route();
	route3.setName("rt3");
	
	RouteOffer routeOffer3 = new RouteOffer();
	routeOffer3.setActive(true);
	routeOffer3.setSequence(3);
	routeOffer3.setName("BAU_NW");
	
	route.getRouteOffer().add(routeOffer1);
	route.getRouteOffer().add(routeOffer2);
	route.getRouteOffer().add(routeOffer3);
	
	routeGroup.getRoute().add(route);
	//routeGroup.getRoute().add(route2);
	//routeGroup.getRoute().add(route3);
	
	routeGroups.getRouteGroup();
	routeGroups.getRouteGroup().add(routeGroup);
	
	return routeInfo;
}

public static RouteInfo createRouteInfoWithFailoverSequenceOnSameRequest(){
	
	RouteInfo routeInfo = new RouteInfo();
	routeInfo.setServiceName("com.att.aft.dme2.TestDME2ExchangeFailoverSequenceOnSameRequest");
	//routeInfo.setServiceVersion("*");
	routeInfo.setEnvContext("DEV");
	
	RouteGroups routeGroups = new RouteGroups();
	routeInfo.setRouteGroups(routeGroups);
	
	RouteGroup routeGroup = new RouteGroup();
	routeGroup.setName("RG1");
	routeGroup.getPartner().add("test1");
	routeGroup.getPartner().add("test2");
	routeGroup.getPartner().add("test3");
	
	Route route = new Route();
	route.setName("rt1");
	
	RouteOffer routeOffer1 = new RouteOffer();
	routeOffer1.setActive(true);
	routeOffer1.setSequence(1);
	routeOffer1.setName("PRIM");
	
	Route route2 = new Route();
	route2.setName("rt2");
	
	RouteOffer routeOffer2 = new RouteOffer();
	routeOffer2.setActive(true);
	routeOffer2.setSequence(2);
	routeOffer2.setName("SECOND");
	
	Route route3 = new Route();
	route3.setName("rt3");
	
	RouteOffer routeOffer3 = new RouteOffer();
	routeOffer3.setActive(true);
	routeOffer3.setSequence(3);
	routeOffer3.setName("THIRD");
	
	route.getRouteOffer().add(routeOffer1);
	route.getRouteOffer().add(routeOffer2);
	route.getRouteOffer().add(routeOffer3);
	
	routeGroup.getRoute().add(route);
	//routeGroup.getRoute().add(route2);
	//routeGroup.getRoute().add(route3);
	
	routeGroups.getRouteGroup();
	routeGroups.getRouteGroup().add(routeGroup);
	
	return routeInfo;
}
	
	public static RouteInfo createRoutInfoWithPreferredRouteOfferNotFound(){
		
		RouteInfo routeInfo = new RouteInfo();
		routeInfo.setServiceName("com.att.aft.dme2.TestDME2ExchangePreferredRouteOffer");
		//routeInfo.setServiceVersion("*");
		routeInfo.setEnvContext("DEV");
		
		RouteGroups routeGroups = new RouteGroups();
		routeInfo.setRouteGroups(routeGroups);
		
		
		RouteGroup routeGroup1 = new RouteGroup();
		routeGroup1.setName("RG1");
		routeGroup1.getPartner().add("test1");
		routeGroup1.getPartner().add("test2");
		routeGroup1.getPartner().add("test3");
		
		Route route1 = new Route();
		route1.setName("rt1");
		//rt1.setVersionSelector("1.0.0");
		
		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setSequence(1);
		routeOffer1.setName("BAU_NE");
		
		Route route2 = new Route();
		route2.setName("rt2");
		//rt2.setVersionSelector("2.0.0");
		
		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setSequence(2);
		routeOffer2.setName("BAU_SE");
		
		Route route3 = new Route();
		route3.setName("rt3");
		
		RouteOffer routeOffer3 = new RouteOffer();
		routeOffer3.setActive(true);
		routeOffer3.setSequence(3);
		routeOffer3.setName("BAU_SW");
		
		route1.getRouteOffer().add(routeOffer1);
		
		// Commenting BAU_SE ro add so that preferred route offer is not found in list 
		//rt1.getRouteOffer().add(ro2);
		route1.getRouteOffer().add(routeOffer3);
		routeGroup1.getRoute().add(route1);
		routeGroups.getRouteGroup();
		routeGroups.getRouteGroup().add(routeGroup1);
		
		return routeInfo;
		
	}
	
	public static RouteInfo createRouteInfoWithVersionMap(){
		RouteInfo routeInfo = new RouteInfo();
		//routeInfo.setDataPartitionKeyPath("xyz");
		routeInfo.setEnvContext("LAB");
		routeInfo.setServiceName("com.att.aft.dme2.test.TestVersionMapping");
		//routeInfo.setServiceVersion("*");
		
		VersionMap verMap = new VersionMap();
		//TODO: Add versionMapping 
		//Modifying version map name to avoid conflict with GRMTest case
		verMap.setName("DME2VersionMapTest-2.6");
		
		VersionMapInfo vmp1 = new VersionMapInfo();
		vmp1.setFromVersionFilter("1.*");
		vmp1.setOutgoingVersionFilter("1.3.5");
		
		verMap.getVersionMapInfo().add(vmp1);
		
		VersionMapInfo vmp2 = new VersionMapInfo();
		vmp2.setFromVersionFilter("78");
		vmp2.setOutgoingVersionFilter("1.3.5");
		verMap.getVersionMapInfo().add(vmp2);
		
		VersionMapInfo vmp3 = new VersionMapInfo();
		vmp3.setFromVersionFilter("78.*");
		vmp3.setOutgoingVersionFilter("1.3.6");
		verMap.getVersionMapInfo().add(vmp3);
		
		VersionMapInfo vmp4 = new VersionMapInfo();
		vmp4.setFromVersionFilter("23.1");
		vmp4.setOutgoingVersionFilter("1.6.5");
		verMap.getVersionMapInfo().add(vmp4);
		
		List<VersionMap> vMapList = new ArrayList<VersionMap>();
		vMapList.add(verMap);
		
		VersionMappings vMappings = new VersionMappings();
		
		routeInfo.setVersionMappings(vMappings);
		routeInfo.getVersionMappings().getVersionMap().addAll(vMapList);
		
		RouteGroup routeGroup = new RouteGroup();
		routeGroup.setName("DME2_TEST_ROUTE_GROUP");
		routeGroup.getPartner().add("DME2_PARTNER");
		
		Route route = new Route();
		route.setName("DME2_TEST_ROUTE");

		RouteOffer routeOffer1 = new RouteOffer();
		routeOffer1.setActive(true);
		routeOffer1.setName("VERSION_MAP");
		routeOffer1.setSequence(1);
		routeOffer1.setStalenessInMins((long) 1);
		//Modifying version map name to avoid conflict with GRMTest case
		routeOffer1.setVersionMapRef("DME2VersionMapTest-2.6");
		
		RouteOffer routeOffer2 = new RouteOffer();
		routeOffer2.setActive(true);
		routeOffer2.setName("DME2_SECONDARY");
		routeOffer2.setSequence(2);
		routeOffer2.setStalenessInMins((long) 1);
		
		
		route.getRouteOffer().add(routeOffer1);
		route.getRouteOffer().add(routeOffer2);
		
		routeGroup.getRoute().add(route);
		
		RouteGroups routeGroups =  new RouteGroups();
		routeGroups.getRouteGroup().add(routeGroup);
		
		routeInfo.setRouteGroups(routeGroups);
		
		return routeInfo;
	}
	
	private static DataPartitions createDataPartitions(){
		
		DataPartitions partitions = new DataPartitions();
		List<DataPartition> dpList = new ArrayList<DataPartition>();

		DataPartition partition1 = new DataPartition();	
		partition1.setName("E");
		partition1.setLow("205977");
		partition1.setHigh("205999");
		
		DataPartition partition2 = new DataPartition();
		partition2.setName("W");
		partition2.setLow("205977");
		partition2.setHigh("205999");
		
		DataPartition partition3 = new DataPartition();
		partition3.setName("MW");
		partition3.setLow("205977");
		partition3.setHigh("205999");
		
		dpList.add(partition1);
		dpList.add(partition2);
		dpList.add(partition3);
		
		partitions.getDataPartition().addAll(dpList);
		
		return partitions;
	}
	
	public static RouteInfo createRouteInfoWithVersionSelector(){
		RouteInfo routeInfo=new RouteInfo();
		routeInfo.setEnvContext("LAB");
		routeInfo.setServiceName("com.att.dme2.test.TestVersionSelector_InputsDoNotMatch");
		
		Route route1=new Route();
		route1.setName("DME2_RTE_1");
		route1.setVersionSelector("1.0.0");
		
		RouteOffer routeOffer1=new RouteOffer();
		routeOffer1.setName("RO_PRIMARY");
		routeOffer1.setActive(true);
		routeOffer1.setSequence(2);
		
		RouteOffer routeOffer2=new RouteOffer();
		routeOffer2.setName("SAMPLE");
		routeOffer2.setActive(true);
		routeOffer2.setSequence(2);
		
		Route route2=new Route();
		route2.setName("DME2_RTE_2");
		route2.setVersionSelector("1.0.1");
		
		RouteOffer routeOffer3=new RouteOffer();
		routeOffer3.setName("RO_SECONDARY");
		routeOffer3.setActive(true);
		routeOffer3.setSequence(1);
		
		route1.getRouteOffer().add(routeOffer1);
		route1.getRouteOffer().add(routeOffer2);
		route2.getRouteOffer().add(routeOffer3);
		
		RouteGroup routeGroup=new RouteGroup();
		routeGroup.setName("DME2_RG_1");
		routeGroup.getPartner().add("*");
		routeGroup.getRoute().add(route1);
		routeGroup.getRoute().add(route2);
		
		RouteGroups routeGroups=new RouteGroups();
		routeGroups.getRouteGroup().add(routeGroup);
		
		
		routeInfo.setRouteGroups(routeGroups);
		
		return routeInfo;
	}
  public static RouteGroup buildRouteGroup(String name, List<Route> routes, String... partners){
    RouteGroup rg = new RouteGroup();
    rg.setName(name);
    for(String partner : partners){
      rg.getPartner().add(partner);
    }
    if(routes != null){
      rg.getRoute().addAll(routes);
    }
    return rg;
  }
  public static RouteGroups buildRouteGroups(List<RouteGroup> groups){
    RouteGroups routeGroups = new RouteGroups();
    if(groups != null){
      routeGroups.getRouteGroup().addAll(groups);
    }
    return routeGroups;
  }
  public static RouteGroups buildRouteGroups(RouteGroup group){
    List<RouteGroup> groups = new ArrayList<RouteGroup>();
    groups.add(group);
    return buildRouteGroups(groups);
  }
  public static RouteInfo buildRouteInfo(String env, String serviceName, RouteGroups groups){
    RouteInfo routeInfo=new RouteInfo();
    routeInfo.setEnvContext(env);
    if(serviceName != null){
      routeInfo.setServiceName(serviceName);
    }
    routeInfo.setRouteGroups(groups);

    logger.debug( null, "buildRouteInfo", "{}", routeInfo );

    return routeInfo;
  }
  public static RouteGroup buildRouteGroup(String name, Route route, String... partners){
    List<Route> routes = new ArrayList<Route>();
    routes.add(route);
    return buildRouteGroup(name, routes, partners);
  }
  public static Route buildRoute(String name, String versionSelector, String stickySelectorKey, List<RouteOffer> routeOffers){
    Route route = new Route();
    route.setName(name);
    if(versionSelector != null){
      route.setVersionSelector(versionSelector);
    }
    if(stickySelectorKey != null){
      route.setStickySelectorKey(stickySelectorKey);
    }

    if(routeOffers != null){
      route.getRouteOffer().addAll(routeOffers);
    }

    return route;
  }

  public static RouteOffer buildRouteOffer(String name, int sequence, boolean isActive, boolean allowDynamicStickiness, long stalenessInMinutes){
    RouteOffer routeOffer = new RouteOffer();
    routeOffer.setName(name);
    routeOffer.setSequence(sequence);
    routeOffer.setActive(isActive);
    routeOffer.setAllowDynamicStickiness(allowDynamicStickiness);
    //dont set it if we are using the default (0).
    if(stalenessInMinutes >=1){
      routeOffer.setStalenessInMins(stalenessInMinutes);
    }
    return routeOffer;
  }

  public static RouteOffer buildRouteOffer(String name, int sequence, long stalenessInMinutes){
    //the defaults. Matches the schema defaults.
    return buildRouteOffer(name, sequence, true, false, stalenessInMinutes);
  }

  public static RouteOffer buildRouteOffer(String name, int sequence){
    return buildRouteOffer(name, sequence,0l);
  }

  public static List<RouteOffer> buildRouteOffers(int sequence, String... names){
    List<RouteOffer> offers = new LinkedList<RouteOffer>();
    for(String name : names){
      offers.add(buildRouteOffer(name, sequence));
    }
    return offers;
  }

}