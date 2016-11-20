package com.att.aft.dme2.registry.accessor;

import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;

public abstract class AbstractGRMAccessor implements BaseAccessor {
	/**
	 * This class is used to access GRM, use GRMServiceAccessorFactory to create instance
	 *
	 * @see GRMAccessorFactory
	 */

	protected static String envLetter;
	protected final SecurityContext secCtx;
	protected IGRMEndPointDiscovery grmEndPointDiscovery;
	public static int connectTimeout;
	public static int readTimeout;
	public static int overallTimeout;

	protected DME2Configuration config;
	protected String discoveryURL;
	public String[] grmURLs;

	/**
	 * use GRMServiceAccessorFactory#buildGRMServiceAccessor() to build this object. in the current implementation of
	 * IGRMEndPointDiscovery using DNS & GRM, there is a dependency on GRMAccessor to get list of GRM server this creates
	 * a logical mutual dependency that is handled properly by creating this class through factory
	 *
	 * @param ctx                  security context
	 * @param grmEndPointDiscovery a reference to object that finds GRM Endpoints
	 * @see com.att.aft.dme2.registry.accessor.GRMAccessorFactory
	 * @see GRMEndPointsDiscoveryHelperGRM
	 */
	protected AbstractGRMAccessor( DME2Configuration config, SecurityContext ctx,
			IGRMEndPointDiscovery grmEndPointDiscovery ) { // force client to call factory class to create objects
		super();
		this.secCtx = ctx;
		this.grmEndPointDiscovery = grmEndPointDiscovery;
		this.config = config;
		connectTimeout = config.getInt( DME2Constants.GRM_CONNECT_TIMEOUT );
		readTimeout = config.getInt( DME2Constants.AFT_DME2_GRM_READ_TIMEOUT );
		overallTimeout = config.getInt( DME2Constants.AFT_DME2_GRM_OVERALL_TIMEOUT );
	}

}
