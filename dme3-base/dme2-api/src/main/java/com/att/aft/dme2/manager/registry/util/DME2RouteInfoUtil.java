package com.att.aft.dme2.manager.registry.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2RouteInfo;
import com.att.aft.dme2.types.DataPartition;
import com.att.aft.dme2.types.DataPartitions;
import com.att.aft.dme2.types.ListDataPartition;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/**
 * Route info conversion utility class
 */
public class DME2RouteInfoUtil {
  private final static Logger logger = LoggerFactory.getLogger( DME2RouteInfoUtil.class );

  private DME2RouteInfoUtil() {}

  /**
   * Combines a DME2 RouteInfo object and a base RouteInfo object into a DME2 RouteInfo object
   * @param routeInfo routeinfo to combine
   * @param input RouteInfo
   * @return the original DME2RouteInfo (actually unnecessary to return)
   * @throws DME2Exception
   */
  public static DME2RouteInfo convertRouteInfo( DME2RouteInfo routeInfo, RouteInfo input ) throws DME2Exception {
    if ( input != null ) {
      routeInfo.setServiceName( input.getServiceName() );
      routeInfo.setServiceVersion( input.getServiceVersion() );
      routeInfo.setEnvContext( input.getEnvContext() );
      routeInfo.setDataPartitionKeyPath( input.getDataPartitionKeyPath() );
      routeInfo.setDme2BootstrapProperties( input.getDme2BootStrapData() );
      loadPartitionMap( input, routeInfo );
      loadRouteGroupMap( input, routeInfo );
    }

    return routeInfo;
  }

  public static void loadPartitionMap( RouteInfo input, DME2RouteInfo routeInfo ) {
    DataPartitions dps = input.getDataPartitions();
    List<DataPartition> list = null;
    List<ListDataPartition> ldpList = null;

    if ( dps != null ) {
      list = dps.getDataPartition();
      ldpList = dps.getListDataPartition();
    }

    if ( ( list == null || list.size() == 0 ) && ( ldpList == null || ldpList.size() == 0 ) ) {
      return;
    }

    routeInfo.setHasPartitions( true );

    if ( list != null && list.size() > 0 ) {
      for ( DataPartition p : list ) {
        if ( p.getLow().compareTo( p.getHigh() ) > 0 ) {
          logger.info( null, "loadPartitionMap", "AFT-DME2-0104",
              new ErrorContext().add( DME2Constants.SERVICE, routeInfo.getServiceName() )
                  .add( DME2Constants.VERSION, routeInfo.getServiceVersion() ).add( "partition", p.getName() )
          );
        }

        // put low/high in map so we can validate in range on lookups
        routeInfo.getPartitionMap().put( p.getLow(), p );
        routeInfo.getPartitionMap().put( p.getHigh(), p );
      }

      routeInfo.setHasRangePartitions( true );
    }

    if ( ldpList != null && list.size() > 0 ) {
      routeInfo.setLDPList( ldpList );
      routeInfo.setHasListPartitions( true );
    }
  }

  public static void loadRouteGroupMap(RouteInfo input, DME2RouteInfo routeInfo) throws DME2Exception {
    if (input.getRouteGroups() == null) {
      throw new DME2Exception("AFT-DME2-0102", new ErrorContext().add(DME2Constants.SERVICE, routeInfo.getServiceName()).add(
          DME2Constants.VERSION, routeInfo.getServiceVersion()));
    }

    List<RouteGroup> routeGroups = input.getRouteGroups().getRouteGroup();

    if (routeGroups == null || routeGroups.size() == 0) {
      throw new DME2Exception("AFT-DME2-0102", new ErrorContext().add(DME2Constants.SERVICE, routeInfo.getServiceName()).add(
          DME2Constants.VERSION, routeInfo.getServiceVersion()));
    }

    Map<String, RouteGroup> tempMap = new TreeMap<String, RouteGroup>();
    for (RouteGroup rg : routeGroups) {
      rg.getName();
      List<String> partners = rg.getPartner();

      for (String partner : partners) {
        tempMap.put(partner, rg);
      }
    }

    routeInfo.getRouteGroupMap().putAll(tempMap);
  }
}
