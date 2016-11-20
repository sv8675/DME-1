package com.att.aft.dme2.manager.registry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2DistanceUtil;
import com.att.aft.dme2.manager.registry.util.DME2RouteInfoUtil;
import com.att.aft.dme2.types.DataPartition;
import com.att.aft.dme2.types.ListDataPartition;
import com.att.aft.dme2.types.Route;
import com.att.aft.dme2.types.RouteGroup;
import com.att.aft.dme2.types.RouteInfo;
import com.att.aft.dme2.types.RouteLocationSelector;
import com.att.aft.dme2.types.RouteOffer;
import com.att.aft.dme2.types.VersionMap;
import com.att.aft.dme2.types.VersionMapInfo;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

public class DME2RouteInfo implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6605370283684623928L;

	private static final Logger logger = LoggerFactory.getLogger( DME2RouteInfo.class );

	private static JAXBContext jaxBContext = null;

	static {
		try {
			jaxBContext = JAXBContext.newInstance( "com.att.aft.dme2.types" );
		} catch ( JAXBException e ) {
			logger.error( null, "static", LogMessage.REPORT_ERROR, "error in static initializer", e );
		}
	}

	private String dme2BootstrapProperties;
	private String dataPartitionKeyPath;
	private Long expirationTime;
	private long cacheTTL;
	private long lastUpdated;
	private String serviceName;
	private String serviceVersion;
	private String envContext;
	private DME2Manager manager;

	/**
	 * The original RouteInfo.
	 */
	private RouteInfo routeInfo;

	private List<VersionMap> versionMaps;

	/**
	 * The has partitions.
	 */
	private boolean hasPartitions = false;

	/**
	 * The has range partitions.
	 */
	private boolean hasRangePartitions = false;

	/**
	 * The has list partitions.
	 */
	private boolean hasListPartitions = false;

	/**
	 * The partition map.
	 */
	private final NavigableMap<String, DataPartition> partitionMap = new TreeMap<String, DataPartition>();

	/**
	 * The ListPartition List
	 */
	private List<ListDataPartition> LDPList = new ArrayList<ListDataPartition>();

	/**
	 * The route group map.
	 */
	private final Map<String, RouteGroup> routeGroupMap = new TreeMap<String, RouteGroup>();

	private final Map<String, DME2RouteOffer> routeOfferMap = new ConcurrentHashMap<String, DME2RouteOffer>();

	private DME2RouteInfo() {

	}

	public DME2RouteInfo( RouteInfo inputRouteInfo, DME2Configuration configuration ) throws DME2Exception {
		this.routeInfo = inputRouteInfo;
		this.cacheTTL = configuration.getInt( "DME2_ROUTEINFO_CACHE_TTL_MS" ); // , 300000 );
		this.expirationTime = this.cacheTTL + System.currentTimeMillis();
		this.lastUpdated = System.currentTimeMillis();

		DME2RouteInfoUtil.convertRouteInfo( this, inputRouteInfo );
	}

	@SuppressWarnings("unchecked")
	public DME2RouteInfo( File file ) throws JAXBException, DME2Exception {
		Unmarshaller unmarshaller = jaxBContext.createUnmarshaller();
		JAXBElement<RouteInfo> element = (JAXBElement<RouteInfo>) unmarshaller.unmarshal( file );
		routeInfo = element.getValue();
		DME2RouteInfoUtil.loadPartitionMap( routeInfo, this );
		DME2RouteInfoUtil.loadRouteGroupMap( routeInfo, this );
	}

	public long lastUpdated() {
		return 0;
	}

	public Long getExpirationTime() {
		return expirationTime;
	}

	public long getCacheTTL() {
		return cacheTTL;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getServiceVersion() {
		return serviceVersion;
	}

	public String getEnvContext() {
		return envContext;
	}

	public void setManager( DME2Manager manager ) {
		this.manager = manager;
	}

	public DME2Manager getManager() {
		return manager;
	}

	public String getDme2BootstrapProperties() {
		return dme2BootstrapProperties;
	}

	public void setDme2BootstrapProperties( String dme2BootstrapProperties ) {
		this.dme2BootstrapProperties = dme2BootstrapProperties;
	}

	public String getDataPartitionKeyPath() {
		return dataPartitionKeyPath;
	}

	public void setDataPartitionKeyPath( String dataPartitionKeyPath ) {
		this.dataPartitionKeyPath = dataPartitionKeyPath;
	}

	public void setExpirationTime( Long expirationTime ) {
		this.expirationTime = expirationTime;
	}

	public void setCacheTTL( long cacheTTL ) {
		this.cacheTTL = cacheTTL;
	}

	public void setServiceName( String serviceName ) {
		this.serviceName = serviceName;
	}

	public void setServiceVersion( String serviceVersion ) {
		this.serviceVersion = serviceVersion;
	}

	public void setEnvContext( String envContext ) {
		this.envContext = envContext;
	}

	public RouteInfo getRouteInfo() {
		return routeInfo;
	}

	public void setRouteInfo( RouteInfo routeInfo ) {
		this.routeInfo = routeInfo;
	}

	public boolean isHasPartitions() {
		return hasPartitions;
	}

	public void setHasPartitions( boolean hasPartitions ) {
		this.hasPartitions = hasPartitions;
	}

	public List<VersionMap> getVersionMaps() {
		return versionMaps;
	}

	public void setVersionMaps( List<VersionMap> versionMaps ) {
		this.versionMaps = versionMaps;
	}

	public NavigableMap<String, DataPartition> getPartitionMap() {
		return partitionMap;
	}

	public boolean isHasRangePartitions() {
		return hasRangePartitions;
	}

	public void setHasRangePartitions( boolean hasRangePartitions ) {
		this.hasRangePartitions = hasRangePartitions;
	}

	public boolean isHasListPartitions() {
		return hasListPartitions;
	}

	public void setHasListPartitions( boolean hasListPartitions ) {
		this.hasListPartitions = hasListPartitions;
	}

	public List<ListDataPartition> getLDPList() {
		return LDPList;
	}

	public void setLDPList( List<ListDataPartition> LDPList ) {
		this.LDPList = LDPList;
	}

	public Map<String, RouteGroup> getRouteGroupMap() {
		return routeGroupMap;
	}

	public List<DME2RouteOffer> getRouteOffers( String envContext,
			String partnerName, String keyValue, String stickySelectorKey )
					throws DME2Exception {
		return getRouteOffers( envContext, partnerName, keyValue, stickySelectorKey, false );
	}

	public List<DME2RouteOffer> getRouteOffers( String envContext2,
			String partnerName, String keyValue, String stickySelectorKey,
			boolean checkDataPartitionRange ) throws DME2Exception {
		return getRouteOffers( envContext2, partnerName, keyValue, stickySelectorKey, checkDataPartitionRange, null );
	}

	public List<DME2RouteOffer> getRouteOffers( String envContext2, String partnerName, String keyValue,
			String stickySelectorKey, boolean checkDataPartitionRange,
			String preferredRouteOffer ) throws DME2Exception {
		boolean addedPreferredRouteOffer = false;

		String partitionName = null;

		if ( keyValue != null && isHasPartitions() ) {
			//On rare occasions the client may want to check range partitions first, in this case they would set the following
			//constant value and we check in reverse order.
			if ( checkDataPartitionRange ) {
				partitionName = checkRangePartitions( keyValue );

				if ( partitionName == null && !isHasListPartitions() ) {
					throw new DME2Exception( DME2Constants.EXC_ROUTE_INFO_NOT_FOUND_IN_PARTITION_RANGES, new ErrorContext()
							.add( DME2Constants.SERVICE, this.getServiceName() )
							.add( DME2Constants.VERSION, this.getServiceVersion() )
							.add( DME2Constants.PARTNER, partnerName )
							.add( DME2Constants.KEY, keyValue ) );
				} else if ( partitionName == null && isHasListPartitions() ) {
					partitionName = checkListPartitions( keyValue );

					if ( partitionName == null ) {
						throw new DME2Exception( DME2Constants.EXC_ROUTE_INFO_NOT_FOUND_IN_PARTITION_LIST, new ErrorContext()
								.add( DME2Constants.SERVICE, this.getServiceName() )
								.add( DME2Constants.VERSION, this.getServiceVersion() )
								.add( DME2Constants.PARTNER, partnerName )
								.add( DME2Constants.KEY, keyValue ) );
					}
				}
			} else {
				partitionName = checkListPartitions( keyValue );

				if ( partitionName == null && !isHasRangePartitions() ) {
					throw new DME2Exception( DME2Constants.EXC_ROUTE_INFO_NOT_FOUND_IN_PARTITION_LIST, new ErrorContext()
							.add( DME2Constants.SERVICE, this.getServiceName() )
							.add( DME2Constants.VERSION, this.getServiceVersion() )
							.add( DME2Constants.PARTNER, partnerName )
							.add( DME2Constants.KEY, keyValue ) );
				} else if ( partitionName == null && isHasRangePartitions() ) {
					partitionName = checkRangePartitions( keyValue );

					if ( partitionName == null ) {
						throw new DME2Exception( DME2Constants.EXC_ROUTE_INFO_NOT_FOUND_IN_PARTITION_RANGES, new ErrorContext()
								.add( DME2Constants.SERVICE, this.getServiceName() )
								.add( DME2Constants.VERSION, this.getServiceVersion() )
								.add( DME2Constants.PARTNER, partnerName )
								.add( DME2Constants.KEY, keyValue ) );
					}
				}
			}
		}


		// get the RouteGroup for the provided partner
		RouteGroup routeGroup = getRouteGroup( partnerName );

		if ( routeGroup == null ) {
			routeGroup = getRouteGroup( "*" );

			if ( routeGroup == null ) {
				throw new DME2Exception( DME2Constants.EXC_ROUTE_INFO_NOT_FOUND_IN_PARTITION_RANGES, new ErrorContext()
						.add( DME2Constants.SERVICE, this.getServiceName() )
						.add( DME2Constants.VERSION, this.getServiceVersion() )
						.add( DME2Constants.PARTNER, partnerName )
						.add( DME2Constants.KEY, keyValue ) );
			}
		}

		// get the candidate routes
		List<Route> routes = routeGroup.getRoute();

		//create list to store the matching route offers
		Map<Integer, DME2RouteOffer> sequencedMap = new TreeMap<Integer, DME2RouteOffer>();

		// find all routeoffers which match the partition and location, if provided. If not provided,
		// the Route will match and all offers will be returned.
		int inactiveCount = 0;
		int totalRouteOffers = 0;

		for ( Route route : routes ) {
			// The selectors defined in the route will be used in CONJUCTION while selecting a route.
			// This had to be changed since ARS doesn't allow multiple values for same selector ( like dataPartitionRef ) in one route
			// <>
			//    <partition name="SE" low="205977" high="205999"/>
			//   	<partition name="E" low="205444" high="205555"/>
			//      <partition name="ATL" low="404707" high="404707"/>
			//
			// <route>
			// 	<dataPartitionRef>SE</dataPartitionRef>
			//  <dataPartitionRef>E</dataPartitionRef>
			// </route>


			if ( !( ( keyValue == null || "".equals( keyValue ) ) && stickySelectorKey == null ) ) {

				// 1. BOTH SELECTORS PROVIDED
				// If client had provided dataContext, but route does not have
				// partitionref defined, skip the route
				// If no dataContext/stickyKey provided, all routes in partner group
				// should be selected
				if ( keyValue != null && stickySelectorKey != null
						&& isHasPartitions() ) {
					if ( ( route.getDataPartitionRef() != null && route
							.getDataPartitionRef().size() == 0 )
							|| route.getStickySelectorKey() == null ) {
						continue;
					}

				}
				// If DME2SEARCH is provided with dataContext, but route does
				// not have any dataPartition configure, skip the route
				/**
				 * DME2SEARCH/service/version/envContext/partner/dataContext=
				 * value/stickySelectorKey=DPSEL <routeGroup
				 * name="datapartition"> <partner>DP</partner> <route
				 * name="DEFAULT1"> <dataPartitionRef>SE</dataPartitionRef>
				 * <stickySelectorKey>DPSEL</stickySelectorKey> <routeOffer
				 * name="DP1" sequence="1" active="true"/> <routeOffer
				 * name="STICKY1" sequence="1" active="true"/> </route> <route
				 * name="DEFAULT2"> <stickySelectorKey>DPSEL</stickySelectorKey>
				 * <routeOffer name="DP_INVALID" sequence="1" active="true"/>
				 * </route>
				 *
				 * </routeGroup>
				 *
				 * With above example, for input with just stickySelectorKey,
				 * DP_INVALID route offer will be chosen If input had both
				 * dataContext and stickySel, then DP1~STICKY1 will be selected.
				 */
				// 2. STICKYKEY PROVIDED, DATACONTEXT NULL
				if ( stickySelectorKey != null && keyValue == null
						&& isHasPartitions() ) {
					if ( route.getDataPartitionRef() != null
							&& route.getDataPartitionRef().size() != 0 ) {
						continue;
					}
				}

				// If route has a stickySelectorKey, but user had not provided a
				// stickySelectorKey but only dataContext, skip the route
				// 3. STICKYKEY NULL, DATACONTEXT PROVIDED
				if ( stickySelectorKey == null && keyValue != null ) {
					if ( route.getStickySelectorKey() != null ) {
						continue;
					}
				}

				// if we have a partition, make sure this route is associated
				// with it
				if ( partitionName != null ) {
					if ( route.getDataPartitionRef() != null
							&& route.getDataPartitionRef().size() > 0 ) {
						if ( !route.getDataPartitionRef()
								.contains( partitionName ) ) {
							continue;
						}
					} else {
						// DME2RouteInfo - adding skip route
						// No dataPartitionRef
						// found, while user provided a dataContext and a
						// partitionName is resolved
						continue;
					}
				}

			}

			// if we have a selector, make sure this route qualifies
			RouteLocationSelector selector = route.getRouteLocationSelector();

			if ( selector != null ) {
				double distance = DME2DistanceUtil.calculateDistanceBetween( DME2AbstractEndpointRegistry.getClientLatitude(),
						DME2AbstractEndpointRegistry.getClientLongitude(), selector.getLatitude(), selector.getLongitude() );
				if ( distance > selector.getMaxDistance() ) {
					continue;
				}
			}

			boolean foundDynamicStickKey = false;

			for ( RouteOffer offer : route.getRouteOffer() ) {
				if ( !offer.isActive()
						|| stickySelectorKey == null
						|| offer.getName() == null
						|| !offer.getName().equalsIgnoreCase( stickySelectorKey )
						|| !offer.isAllowDynamicStickiness() ) {
					continue;
				}
				if ( (preferredRouteOffer != null) && (preferredRouteOffer.equals( offer.getName() ))) {
					addedPreferredRouteOffer = true;
				}

				// routeOffer has same value as stickySelectorKey
				foundDynamicStickKey = true;

				// cache the routeoffer to keep from recreating.
				// this allows us to "round robin" endpoints in this routeoffer
				// until the route info is refreshed again
				final String cacheKey = envContext + "_" + offer + "_" + routeGroup.getName();
				DME2RouteOffer roByKey = routeOfferMap.get( cacheKey );
				if ( roByKey == null ) {
					final String version = applyVersionMapping( offer );
					final String fqName = routeGroup.getName() + "." + route.getName() + "." + offer.getName();
					roByKey = new DME2RouteOffer( this.getServiceName(), version, envContext, offer, fqName, manager );
					routeOfferMap.put( cacheKey, roByKey );
				}

				final DME2RouteOffer roBySequence = sequencedMap.get( roByKey.getSequence() );
				final DME2RouteOffer mergedRO = roBySequence == null ? roByKey.clone() :
					roBySequence.withSearchFilter( roBySequence.getSearchFilter() + "~" + roByKey.getSearchFilter() );
				sequencedMap.put( roByKey.getSequence(), mergedRO );

			}

			// see if route should be selected based on sticky key
			if ( route.getStickySelectorKey() != null && stickySelectorKey != null && !foundDynamicStickKey ) {
				if ( !route.getStickySelectorKey().equals( stickySelectorKey ) ) {
					continue;
				}
			}

			// CSSA-11267 - Service was disobeying stickyRoutes
			// If stickySelector is provided by client and route doesn't have a stickySelector, the route with
			// no stickySelector configured is also being picked up. Below check would avoid that from happening.
			if ( stickySelectorKey != null && route.getStickySelectorKey() == null ) {
				continue;
			}

			totalRouteOffers = totalRouteOffers + route.getRouteOffer().size();

			if ( !foundDynamicStickKey ) {
				// if we got here, all the routes should be added to the final list
				for ( RouteOffer offer : route.getRouteOffer() ) {
					//exclude any non-active offers
					if ( !offer.isActive() ) {
						inactiveCount++;
						continue;
					}

					// Wouldn't this have already happened above?
					if ( preferredRouteOffer != null && preferredRouteOffer.equals( offer.getName() )) {
						addedPreferredRouteOffer = true;
					}

					// cache the routeoffer to keep from recreating.
					// this allows us to "round robin" endpoints in this routeoffer
					// until the route info is refreshed again
					final String cacheKey = envContext + "_" + offer + "_" + routeGroup.getName();
					DME2RouteOffer roByKey = routeOfferMap.get( cacheKey );
					if ( roByKey == null ) {
						final String version = applyVersionMapping( offer );
						final String fqName = routeGroup.getName() + "." + route.getName() + "." + offer.getName();
						roByKey = new DME2RouteOffer( this.getServiceName(), version, envContext, offer, fqName, manager );
						routeOfferMap.put( cacheKey, roByKey );
					}

					final DME2RouteOffer roBySequence = sequencedMap.get( roByKey.getSequence() );
					final DME2RouteOffer mergedRO = roBySequence == null ? roByKey.clone() :
						roBySequence.withSearchFilter( roBySequence.getSearchFilter() + "~" + roByKey.getSearchFilter() );
					sequencedMap.put( roByKey.getSequence(), mergedRO );
				}
			}
		}

		if ((preferredRouteOffer != null) && (!addedPreferredRouteOffer)) {
			RouteOffer pRouteOffer = getRouteOffersForcePreferredRouteOffer(sequencedMap, routeGroup, preferredRouteOffer, partitionName);
			if (pRouteOffer != null) {
				if (!pRouteOffer.isActive()) {
					inactiveCount = inactiveCount + 1;
				} else {
					final String cacheKey = envContext + "_" + pRouteOffer + "_" + routeGroup.getName();
					DME2RouteOffer roByKey = routeOfferMap.get(cacheKey);
					if (roByKey == null)
					{
						final String version = applyVersionMapping(pRouteOffer);
						final String fqName = routeGroup.getName() + "." + pRouteOffer.getName() + "." + pRouteOffer.getName();
						roByKey = new DME2RouteOffer(this.getServiceName(), version, envContext, pRouteOffer, fqName, manager);
						routeOfferMap.put(cacheKey, roByKey);
					}

					final DME2RouteOffer roBySequence = sequencedMap.get(roByKey.getSequence());
					final DME2RouteOffer mergedRO = roBySequence == null ? roByKey.clone() : roBySequence.withSearchFilter(roBySequence.getSearchFilter() + "~" + roByKey.getSearchFilter());
					sequencedMap.put(roByKey.getSequence(), mergedRO);
					totalRouteOffers = totalRouteOffers + 1;
				}

			}
		}

		if ( inactiveCount == totalRouteOffers ) {
			throw new DME2Exception( "AFT-DME2-0103", new ErrorContext()
					.add( DME2Constants.SERVICE, this.getServiceName() )
					.add( DME2Constants.VERSION, this.getServiceVersion() )
					.add( DME2Constants.PARTNER, partnerName )
					.add( DME2Constants.KEY, keyValue ) );
		}

		// sort the final list, by sequence. Note that Offers in multiple Routes
		// with the same sequence #
		// will be sorted together.

		// no need to sort -- TreeMap will return values in ascending order of keys

		return Collections.unmodifiableList( new ArrayList<DME2RouteOffer>( sequencedMap.values() ) );
	}

	private String checkRangePartitions( String keyValue ) {

		String partitionName = null;


		// find the entry that is at or just below the keyValue
		Map.Entry<String, DataPartition> entry = partitionMap.floorEntry( keyValue );

		// no match - immediate exception
		if ( entry == null ) {
			return null;
		}

		// get partition out
		DataPartition partition = entry.getValue();

		// check if the floorEntry we got back is actually the high end of the range - if so, its no match
		if ( !entry.getKey().equals( keyValue ) && partition.getHigh().equals( entry.getKey() ) ) {
			return null;
		}

		partitionName = partition.getName();
		return partitionName;

	}

	private String checkListPartitions( String keyValue ) {

		String partitionName = null;

		// if this service has list partitions, and application provided dataContext is not null, select one
		if ( isHasListPartitions() && keyValue != null ) {

			for ( ListDataPartition dp : LDPList ) {
				//If the list in the data partition has the value we're looking for, then we set partition name
				//and skip the rest of the validations
				if ( dp.getValue().contains( keyValue ) ) {
					partitionName = dp.getName();
					break;
				}
			}
		}

		return partitionName;
	}

	/**
	 * Gets the route group.
	 *
	 * @param partnerName the partner name
	 * @return the route group
	 */
	private RouteGroup getRouteGroup( String partnerName ) {
		return routeGroupMap.get( partnerName );
	}

	private String applyVersionMapping( RouteOffer routeOffer ) {

		List<VersionMap> verMapList = getVersionMappings();


		if ( verMapList != null ) {
			for ( VersionMap verMap : verMapList ) {
				if ( verMap.getName().equals( routeOffer.getVersionMapRef() ) ) {

					for ( VersionMapInfo verMapInfo : verMap.getVersionMapInfo() ) {

						if ( verMapInfo.getFromVersionFilter().matches( "[0-9]+\\.[*]" ) || verMapInfo.getFromVersionFilter()
								.matches( "[0-9]+\\.[*]\\.[*]" ) ) {  //Matches version="73.*" OR "73.*.*"
							String[] fromVersionTokens = verMapInfo.getFromVersionFilter().split( DME2Constants.SLASHSLASH );

							if ( !this.getServiceVersion().contains( "." ) ) {  //This means the client supplied version looks like 73
								if ( fromVersionTokens[0].equals( this.getServiceVersion() ) ) {
									return verMapInfo.getOutgoingVersionFilter();
								}
							} else {    //This means the client supplied version looks like 73.2.4
								String[] clientVersionTokens = this.getServiceVersion().split( DME2Constants.SLASHSLASH );

								if ( clientVersionTokens[0].equals( fromVersionTokens[0] ) ) {
									return verMapInfo.getOutgoingVersionFilter();
								}
							}

						} else if ( verMapInfo.getFromVersionFilter()
								.matches( "[0-9]+\\.[*]\\.[0-9]+" ) ) {  //Matches version="73.*.1"
							String[] fromVersionTokens = verMapInfo.getFromVersionFilter().split( DME2Constants.SLASHSLASH );

							if ( this.getServiceVersion().contains( "." ) ) {
								String[] clientVersionTokens = this.getServiceVersion().split( DME2Constants.SLASHSLASH );

								if ( clientVersionTokens.length == 3 ) {
									if ( clientVersionTokens[0].equals( fromVersionTokens[0] ) &&
											clientVersionTokens[2].equals( fromVersionTokens[2] ) ) {
										return verMapInfo.getOutgoingVersionFilter();
									}
								}

							}


						} else if ( verMapInfo.getFromVersionFilter().matches( "[0-9]+" ) ) {    //Matches version="73"

							if ( !this.getServiceVersion().contains( "." ) ) {
								if ( this.getServiceVersion().equals( verMapInfo.getFromVersionFilter() ) ) {
									return verMapInfo.getOutgoingVersionFilter();
								}
								//Commenting below getServiceVersion to loop through all available/matching versionFilter

							}


						} else if ( verMapInfo.getFromVersionFilter().matches( "[0-9]+\\.[0-9]+" ) ) {    //Matches version="73.1"
							String[] fromVersionTokens = verMapInfo.getFromVersionFilter().split( DME2Constants.SLASHSLASH );

							if ( this.getServiceVersion().contains( "." ) ) {

								String[] clientVersionTokens = this.getServiceVersion().split( DME2Constants.SLASHSLASH );

								if ( clientVersionTokens.length == 2 ) {
									if ( clientVersionTokens[0].equals( fromVersionTokens[0] ) &&
											clientVersionTokens[1].equals( fromVersionTokens[1] ) ) {
										return verMapInfo.getOutgoingVersionFilter();
									}
								}

							}


						} else if ( verMapInfo.getFromVersionFilter()
								.matches( "[0-9]+\\.[0-9]+\\.[0-9]+" ) ) {  //Matches version="73.1.1"
							if ( this.getServiceVersion().equals( verMapInfo.getFromVersionFilter() ) ) {
								return verMapInfo.getOutgoingVersionFilter();
							}
							//Commenting below getServiceVersion to loop through all available/matching versionFilter

						}
					}
				}
			}
		}

		return this.getServiceVersion();
	}

	private List<VersionMap> getVersionMappings() {
		if ( routeInfo == null || routeInfo.getVersionMappings() == null ) {
			return null;
		}

		return routeInfo.getVersionMappings().getVersionMap();
	}

	public RouteOffer getRouteOffersForcePreferredRouteOffer(Map<Integer, DME2RouteOffer> sequencedMap, RouteGroup routeGroup,
			String preferredRouteOffer, String partitionName) throws DME2Exception {
		// get the candidate routes
		List<Route> routes = routeGroup.getRoute();
		boolean foundRouteOffer = false;
		for (Route route : routes) {
			for (RouteOffer offer : route.getRouteOffer()) {
				if ((offer.getName() != null) && (offer.getName().equalsIgnoreCase(preferredRouteOffer))) {
					foundRouteOffer = true;
					//check if offer is active
					if (!offer.isActive()) {
						break;
					}
					return offer;
				}
			}
			if (foundRouteOffer)
				break;
		}
		return null;
	}
}
