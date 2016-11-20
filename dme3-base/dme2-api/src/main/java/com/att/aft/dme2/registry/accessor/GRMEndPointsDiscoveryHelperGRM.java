package com.att.aft.dme2.registry.accessor;

import java.util.ArrayList;
import java.util.List;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.util.DME2EndpointUtil;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.util.DME2ParameterNames;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;
import com.att.scld.grm.types.v1.ServiceEndPoint;
import com.att.scld.grm.types.v1.VersionDefinition;
import com.att.scld.grm.v1.FindRunningServiceEndPointRequest;

/**
 * Responsibility of this class is to use a list of Seed GRM server endpoints as input, call GRM Servers to get latest
 * list of available GRM server endpoints. this seed can come from a DNS call, previous calls to the class, or a
 * persistence cache
 *
 * @author ar671m
 */
public class GRMEndPointsDiscoveryHelperGRM implements IGRMEndPointDiscovery {
	private static final Logger logger = LoggerFactory.getLogger( GRMEndPointsDiscoveryHelperGRM.class );
	// ************************************************************************
	// Reference to Other Components:
	protected ServiceEndPoint sep; // used in getGRMServersFromGRM
	protected FindRunningServiceEndPointRequest req; // used in getGRMServersFromGRM
	protected BaseAccessor grmServiceAccessor;
	// an instance of GRMServiceAccessor used to fetch list of active GRM servers
	private DME2Configuration config;
	private String grmedgeURL;
	private List<String> grmedgeURLFromDNS;
	private String[] grmedgeAFTDirectURLList;

	/**
	 * constructor method should only be called from @see GRMConnectionFactory only one instance of this class will be
	 * created by factory
	 *
	 * @param protocol
	 * @param port
	 * @param path
	 * @param grmServiceAccessor
	 */
	protected GRMEndPointsDiscoveryHelperGRM( String environment, String protocol, String serviceName, String version,
			BaseAccessor grmServiceAccessor, DME2Configuration configuration ) {
		config = configuration;
		this.grmServiceAccessor = grmServiceAccessor;
		buildFindGRMServiceEndPointRequest( environment, protocol, serviceName, version );

	}
	
	protected GRMEndPointsDiscoveryHelperGRM(String grmedgeDirectURL, DME2Configuration configuration) {
		config = configuration;
		grmedgeURL = grmedgeDirectURL;
	}
	
	protected GRMEndPointsDiscoveryHelperGRM(List<String> grmedgeDNSURLs, DME2Configuration configuration) {
		config = configuration;
		grmedgeURLFromDNS = grmedgeDNSURLs;
	}
	
	protected GRMEndPointsDiscoveryHelperGRM(String[] grmedgeAFTDirectURLs, DME2Configuration configuration) {
		config = configuration;
		grmedgeAFTDirectURLList = grmedgeAFTDirectURLs;
	}

	@Override
	public List<String> getGRMEndpoints() {
		List<String> newGRMServers = new ArrayList<String>();
		if (!config.getProperty(DME2ParameterNames.GRM_EDGE_DIRECT_HOST).isEmpty()) {
			newGRMServers.add(grmedgeURL);
		} else if (!config.getProperty(DME2ParameterNames.GRM_EDGE_CUSTOM_DNS).isEmpty()) {
			for (String aURL : grmedgeURLFromDNS) {
				newGRMServers.add(aURL);
			}
		} else if (!config.getProperty(DME2ParameterNames.AFT_DME2_GRM_URLS).isEmpty()) {
			for (String aURL : grmedgeAFTDirectURLList) {
				newGRMServers.add(aURL);
			}
		} else {
			try {
				ServiceEndpoint sep = DME2EndpointUtil.convertGrmEndpointToAccessorEndpoint(req.getServiceEndPoint());
				sep.setEnv( req.getEnv() );
				List<ServiceEndpoint> endPoints = grmServiceAccessor.findRunningServiceEndPoint( sep );
				if ( endPoints != null && !endPoints.isEmpty() ) {
					// the sorting will happen in cache after all servers listed are merged
					for ( ServiceEndpoint sepNew : endPoints ) {
						if (config.getBoolean(DME2ParameterNames.OVERRIDE_GRM_SERVER_PATH)) {
							sepNew.setContextPath(config.getProperty( DME2ParameterNames.GRM_SERVER_PATH ));
						}
						newGRMServers.add( convertServiceEndPointToString( sepNew ) );
					}
				} else {
					logger.debug( null, "getGRMEndpoints", "call to following GRM Seed Server returned with no result: " );
				}
			} catch ( DME2Exception e ) {
				logger.debug( null, "getGRMEndpoints",
						"call to following GRM Seed Server to get other GRM servers failed with following exception: ", e );
			}
			if ( newGRMServers.isEmpty() ) {
				//LOGGER.log(Level.SEVERE, "Call to GRM seed servers to get other GRM server did not return any result.");
				logger.debug( null, "getGRMEndpoints",
						"Call to GRM seed servers to get other GRM server did not return any result." );
				// 74 may be it would be good to through a DME2 exception if no GRM server is found in Seeds
			}
		}
		return newGRMServers;
	}

	/*
	 * build a FindRunningServiceEndPointRequest that would be reused to find GRM Servers periodically all common fields filled here, other fields are filled in
	 * uriToServiceEndPoint from URL of seed host per server
	 */
	private void buildFindGRMServiceEndPointRequest( String environment, String protocol, String serviceName,
			String version ) {
		VersionDefinition vd = DME2Utils.buildVersionDefinition( config, version );

		sep = new ServiceEndPoint();
		sep.setProtocol( protocol );
		sep.setVersion( vd );
		sep.setName( serviceName );

		req = new FindRunningServiceEndPointRequest();
		req.setServiceEndPoint( sep );
		req.setEnv( environment ); // LAB
	}

	private String convertServiceEndPointToString( ServiceEndpoint sep ) {
		return sep.getProtocol() + "://" + sep.getHostAddress() + ":" + sep.getPort() + sep.getContextPath();
	}

	public void setGrmServiceAccessor( BaseAccessor grmServiceAccessor ) {
		this.grmServiceAccessor = grmServiceAccessor;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}
}