package com.att.aft.dme2.registry.accessor;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.att.aft.dme2.api.util.grm.DNSIPResolver;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;

/**
 * This class get IP of of Seed GRM servers from DNS server and creates a list of Endpoint URLs from them and returns
 * it. for list configurable properties of the class refer to constructor description
 *
 * @author ar671m
 */
public class GRMEndPointsDiscoveryHelperDNS implements IGRMEndPointDiscovery {
  // ************************************************************************
  // Internal Properties
  private final String grmServersEnvDNSName;
  private final String grmSeedProtocol;
  private final String grmSeedPort;
  private final String grmSeedPath;
  // ************************************************************************
  // Reference to Other Components:
  private static final Logger logger = LoggerFactory.getLogger( GRMEndPointsDiscoveryHelperDNS.class );

  /**
   * @param grmServersEnvDNSName DNS name of seed GRM server for this environment to use for lookup, should come from
   *                             parameters
   * @param grmSeedProtocol    protocol part of URL Endpoint of seed GRM servers, should come from parameters
   * @param grmSeedPort        service port part of URL Endpoint of seed GRM servers, should come from parameters
   * @param grmSeedPath        service path part of URL Endpoint of seed GRM servers, should come from parameters
   */
  protected GRMEndPointsDiscoveryHelperDNS( String grmServersEnvDNSName, String grmSeedProtocol, String grmSeedPort,
                                            String grmSeedPath ) {
    logger.debug( null, "ctor", LogMessage.METHOD_ENTER );
    logger.debug( null, "ctor", "DNSNAME: {} SEED PROTOCOL: {} SEED PORT: {} SEED PATH: {}", grmServersEnvDNSName,
        grmSeedProtocol, grmSeedPort, grmSeedPath );
    this.grmServersEnvDNSName = grmServersEnvDNSName;
    this.grmSeedProtocol = grmSeedProtocol;
    this.grmSeedPort = grmSeedPort;
    this.grmSeedPath = grmSeedPath;
    logger.debug( null, "ctor", LogMessage.METHOD_EXIT );
  }

  @Override
  public List<String> getGRMEndpoints() {
    try {
      logger.debug( null, "getGRMEndpoints", "Getting Seed GRM servers from following DNS Name: {}",
          grmServersEnvDNSName );
      List<String> listGRMServerIPs = DNSIPResolver.getListIPForName( grmServersEnvDNSName );
      return convertIPListToURL( listGRMServerIPs );
    } catch ( UnknownHostException ex ) {
      // if can't load list from DNS Server, we can hope the Seed GRM list is filled from cache!
      logger.error( null, "getGRMEndpoints", "Can't get GRM Server list from DNS = {}", grmServersEnvDNSName, ex );
      return new ArrayList<String>( 0 );
    }
  }

  private List<String> convertIPListToURL( List<String> ipList ) {
    List<String> urlList = new ArrayList<String>( ipList.size() );
    for ( String ip : ipList ) {
      String address = ipToGRMServerEndpointURL( ip );
      logger.debug( null, "convertIPListToURL", "Found GRM Seed host ip {} , adding following address as seed: {}", ip,
          address );
      urlList.add( address );
    }
    return urlList;
  }

  private String ipToGRMServerEndpointURL( String ip ) {
    StringBuilder buff = new StringBuilder();
    buff.append( grmSeedProtocol );
    buff.append( "://" );
    buff.append( ip );
    buff.append( ":" );
    buff.append( grmSeedPort );
    buff.append( grmSeedPath );
    return buff.toString();
  }

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
}
