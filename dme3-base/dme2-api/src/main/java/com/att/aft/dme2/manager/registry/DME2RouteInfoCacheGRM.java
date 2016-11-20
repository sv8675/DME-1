package com.att.aft.dme2.manager.registry;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.cache.DME2CacheStats;
import com.att.aft.dme2.cache.DME2CacheStatsHolder;
import com.att.aft.dme2.cache.domain.CacheElement;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2URIUtils;

public class DME2RouteInfoCacheGRM extends DME2AbstractRegistryCache<String, DME2RouteInfo>  {
  private static final Logger logger = LoggerFactory.getLogger( DME2RouteInfoCacheGRM.class );
  private transient DME2EndpointRegistryGRM registry;
  private boolean enableCacheStats = true;
  private final Map<String, DME2CacheStatsHolder> cacheStats = Collections
      .synchronizedMap( new HashMap<String, DME2CacheStatsHolder>() );
  private boolean isRefreshInProgress;

  public DME2RouteInfoCacheGRM( DME2Configuration config, DME2EndpointRegistryGRM registry, String managerName ) throws DME2Exception {
    super( config, DME2RouteInfo.class, DME2EndpointRegistryType.GRM, registry, managerName, false );
    this.registry = registry;
  }

  @Override
  public CacheElement fetchFromSource( CacheElement.Key<String> requestValue ) throws DME2Exception {
    String service = requestValue.getKey();
    Map<String, String> serviceMap = DME2URIUtils.splitServiceURIString( service );
    // TODO: Use enums, not strings
    return createCacheElement( createCacheKey(service), createCacheValue( registry
        .fetchRouteInfo( serviceMap.get( "service" ), serviceMap.get( "version" ), serviceMap.get( "envContext" ) ) ));
  }

  public void shutdownTimerTask() {
    logger.debug( null, "shutdownTimerTask", LogMessage.METHOD_ENTER );
    cache.shutdownTimerTask();
    logger.debug( null, "shutdownTimerTask", LogMessage.METHOD_EXIT );
  }

  public void refresh() {
    try
    {
      isRefreshInProgress = true;
      refreshAllCachedRouteInfo();
      isRefreshInProgress = false;
    }
    catch (Exception e)
    {
      logger.warn( null, "refresh",LogMessage.REFRESH_SVC_FAILED, e);
    }
  }

  private void refreshAllCachedRouteInfo() throws Exception
  {
    String cacheStatsURI = null;

    long startTime =0L;
    if(cache.getCurrentSize() > 0)
    {
      List<String> serviceURIs = new ArrayList<String>();
      for ( CacheElement.Key<String> key : cache.getKeySet() ) {
        serviceURIs.add( key.getKey() );
      }
      for(String uriStr : serviceURIs)
      {
        if(uriStr != null)
        {
					/*If this routeInfo is expired, then make a call to GRM to retrieve it*/
          if(isRouteInfoExpired(uriStr))
          {
            DmeUniformResource uniformResource = new DmeUniformResource(config, new URI("http://DME2LOCAL/" + uriStr));
            String service = uniformResource.getService();
            String version = uniformResource.getVersion();
            String env = uniformResource.getEnvContext();
            cacheStatsURI = "/service=" +service + "/version=" + version + "/envContext="+env;
            DME2CacheStatsHolder statsHolder = null;
            statsHolder = this.cacheStats.get(cacheStatsURI);
            if(statsHolder == null) {
              statsHolder = new DME2CacheStatsHolder(cacheStatsURI, config);
              this.cacheStats.put(cacheStatsURI,statsHolder);
            }
            startTime = System.currentTimeMillis();
            DME2RouteInfo routeInfo = null;
            try {
              routeInfo = registry.fetchRouteInfo(service, version, env);
              statsHolder.recordRefreshSuccess(System.currentTimeMillis()-startTime, this.isCacheStatsEnabled());
            }catch(DME2Exception dmeEx) {
              statsHolder.recordRefreshFailure(System.currentTimeMillis()-startTime,this.isCacheStatsEnabled());
              throw dmeEx;
            }

            if(routeInfo != null)
            {
                cache.put(createCacheKey(  uriStr ), createCacheValue(  routeInfo));
              logger.debug( null, "refreshAllCachedRouteInfo", LogMessage.REFRESH_SERVICE, uriStr);
            }
            else
            {
              logger.debug( null, "refreshAllCachedRouteInfo", LogMessage.REFRESH_SVC_FAILED, uriStr);
            }
          }
        }
      }
    }
  }

  private boolean isRouteInfoExpired(String serviceURI)
  {
		/*Check DME2RouteInfo cache and get the expiration time. The time has expired, fetch the RouteInfo*/
    Long expirationTime = null;
    String uriString = null;

    if(serviceURI.startsWith("/"))
    {
      uriString = serviceURI;
    }
    else
    {
      uriString = "/" + serviceURI;
    }

    DME2RouteInfo routeInfo = (DME2RouteInfo) cache.get(createCacheKey(uriString)).getValue();
    if(routeInfo != null)
    {
      expirationTime = routeInfo.getExpirationTime();
      if(expirationTime <= System.currentTimeMillis()) {
        return true;
      }
    }

    return false;
  }

  @Override
	public DME2CacheStats getStats(String serviceName, Integer hourOfDay) {
		if(serviceName != null && this.cacheStats.get(serviceName) != null) {
			DME2CacheStatsHolder stats = this.cacheStats.get(serviceName);
			if(hourOfDay >=0 && hourOfDay <=23) {
				return stats.getHourlyStats(hourOfDay);
			}
			return this.cacheStats.get(serviceName).getStats();
		}
		return null;
	}

	@Override
	public void disableCacheStats() {
		enableCacheStats = false;
	}

	@Override
	public void enableCacheStats() {
		enableCacheStats = true;
	}

	@Override
	public boolean isCacheStatsEnabled() {
		return enableCacheStats;
	}
}

