package com.att.aft.dme2.manager.registry;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2URIUtils;

/**
 * Base DME2 Endpoint used for service lookup
 */
public class DME2Endpoint implements Serializable, Comparable<DME2Endpoint> {
	private int port;
	private String serviceName;
	private String simpleName;
	private String routeOffer;
	private double latitude;
	private double longitude;
	private long lease;
	private String protocol;
	private String contextPath;
	private String host;
	private String path;
	private String serviceVersion;
	private String envContext;
	private Boolean cached;
	private Properties endpointProperties;
	private String DME2Version;
	private DmeUniformResource dmeUniformResource;
	private String supportedVersionRange;
	// DME2Endpoint (the object, not the concept) has a 1:1 correspondence (in
	// the JVM) to a client location.
	// This may well change in time, in which case a map would need to be set up
	// (at a higher level) to keep track of
	// these distances
	private Double distance = null;

	public DME2Endpoint(double distanceToClient) {
		distance = distanceToClient;
	}

	public DME2Endpoint() {
	}

	/**
	 * Constructor with service path to disassemble
	 * 
	 * @param servicePath
	 *            service path
	 */
	public DME2Endpoint(String servicePath, double distanceToClient) {
		Map<String, String> serviceMap = DME2URIUtils.splitServiceURIString(servicePath);
		this.setServiceName(serviceMap.get(DME2Constants.SERVICE_PATH_KEY_SERVICE));
		this.setRouteOffer(serviceMap.get(DME2Constants.SERVICE_PATH_KEY_ROUTE_OFFER));
		this.setServiceVersion(serviceMap.get(DME2Constants.SERVICE_PATH_KEY_VERSION));
		this.setEnvContext(serviceMap.get(DME2Constants.SERVICE_PATH_KEY_ENV_CONTEXT));
		this.setPath(servicePath);
		distance = distanceToClient;
	}

	/**
	 * Get service version
	 * 
	 * @return service version
	 */
	public String getServiceVersion() {
		return serviceVersion;
	}

	/**
	 * Get env context
	 * 
	 * @return env context
	 */
	public String getEnvContext() {
		return envContext;
	}

	/**
	 * Set host
	 * 
	 * @param host
	 *            service host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Set port
	 * 
	 * @param port
	 *            service port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get port
	 * 
	 * @return service port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set path
	 * 
	 * @param path
	 *            service path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Set service name
	 * 
	 * @param serviceName
	 *            service name
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Get service name
	 * 
	 * @return service name
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * Set simple name
	 * 
	 * @param simpleName
	 *            simple name
	 */
	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}

	/**
	 * Get simple name
	 * 
	 * @return simple name
	 */
	public String getSimpleName() {
		return simpleName;
	}

	/**
	 * Set service version
	 * 
	 * @param serviceVersion
	 *            service version
	 */
	public void setServiceVersion(String serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	/**
	 * Set env context
	 * 
	 * @param envContext
	 *            env context
	 */
	public void setEnvContext(String envContext) {
		this.envContext = envContext;
	}

	/**
	 * Set route offer
	 * 
	 * @param routeOffer
	 *            route offer
	 */
	public void setRouteOffer(String routeOffer) {
		this.routeOffer = routeOffer;
	}

	/**
	 * Get route offer
	 * 
	 * @return route offer
	 */
	public String getRouteOffer() {
		return routeOffer;
	}

	/**
	 * Set context path
	 * 
	 * @param contextPath
	 *            context path
	 */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * Set latitude
	 * 
	 * @param latitude
	 *            latitude
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Get latitude
	 * 
	 * @return latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Set longitude
	 * 
	 * @param longitude
	 *            longitude
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Get longitude
	 * 
	 * @return longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Set lease expiration time
	 * 
	 * @param lease
	 *            lease expiration time
	 */
	public void setLease(long lease) {
		this.lease = lease;
	}

	/**
	 * Get lease expiration time
	 * 
	 * @return lease expiration time
	 */
	public long getLease() {
		return lease;
	}

	/**
	 * Set service protocol
	 * 
	 * @param protocol
	 *            service protocol
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Get service protocol
	 * 
	 * @return service protocol
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * Get service path
	 * 
	 * @return service path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get context path
	 * 
	 * @return context path
	 */
	public String getContextPath() {
		return contextPath;
	}

	/**
	 * Get service host
	 * 
	 * @return service host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get if is cached
	 * 
	 * @return true or false if cached
	 */
	public Boolean getCached() {
		return cached;
	}

	/**
	 * Set if cached
	 * 
	 * @param cached
	 *            true or false if/if not cached
	 */
	public void setCached(Boolean cached) {
		this.cached = cached;
	}

	/**
	 * Get endpoint properties
	 * 
	 * @return endpoint properties
	 */
	public Properties getEndpointProperties() {
		return endpointProperties;
	}

	/**
	 * Set endpoint properties
	 * 
	 * @param endpointProperties
	 *            endpoint properties
	 */
	public void setEndpointProperties(Properties endpointProperties) {
		this.endpointProperties = endpointProperties;
	}

	/**
	 * To url string.
	 *
	 * @return the string
	 */
	public String toURLString() {
		if (protocol == null) {
			protocol = "http";
		}

		if (path != null && path.startsWith("/")) {
			return protocol + "://" + host + ":" + port + path;
		} else {
			return protocol + "://" + host + ":" + port + "/" + path;
		}
	}

	/**
	 * Set DME2 Version
	 * 
	 * @param DME2Version
	 *            DME2 Version
	 */
	public void setDME2Version(String DME2Version) {
		this.DME2Version = DME2Version;
	}

	/**
	 * Get DME2 Version
	 * 
	 * @return DME2 Version
	 */
	public String getDME2Version() {
		return DME2Version;
	}

	/**
	 * Set DME2 Uniform Resource
	 * 
	 * @param DmeUniformResource
	 *            DME2 Uniform Resource
	 */
	public void setDmeUniformResource(DmeUniformResource DmeUniformResource) {
		this.dmeUniformResource = DmeUniformResource;
	}

	/**
	 * Get DME2 Uniform Resource
	 * 
	 * @return DME2 Uniform Resource
	 */
	public DmeUniformResource getDmeUniformResource() {
		return this.dmeUniformResource;
	}

	/**
	 * Get distance in km
	 * 
	 * @return distance in km
	 */
	public double getDistance() {
		return distance;
	}

	/**
	 * Get Service Endpoint ID
	 * 
	 * @return Service Endpoint ID
	 */
	public String getServiceEndpointID() {
		if (getDmeUniformResource() != null) {
			if (getDmeUniformResource().getUrlType() == DmeUniformResource.DmeUrlType.DIRECT) {
				return getDmeUniformResource().toString();
			}
		}

		String format = "%s:%s:%s|%s-%s";
		int i = serviceName.indexOf("/envContext=");
		String env = "";
		if (i != -1) {
			int end = serviceName.indexOf("/", i + 1);
			if (end == -1) {
				end = serviceName.length();
			}
			String subStr = serviceName.substring(i, end);

			String[] tokens = subStr.split("=");
			env = tokens[1];
		}
		return String.format(format, simpleName, serviceVersion, String.valueOf(port), host, env);
	}

	/**
	 * Overloaded toURL String
	 * 
	 * @param context
	 *            Service context
	 * @param extraContext
	 *            extra context
	 * @param queryString
	 *            query string
	 * @return URL string
	 */
	public String toURLString(String context, String extraContext, String queryString) {

		if (protocol == null) {
			protocol = "http";
		}

		String url = null;

		// If client provided context is present, use it to override the path
		// from resolving.
		if (context != null && context.length() > 0) {
			if (context.startsWith("/")) {
				url = protocol + "://" + host + ":" + port + context;
			} else {
				url = protocol + "://" + host + ":" + port + "/" + context;
			}
		} else if (path != null && path.startsWith("/")) {
			url = protocol + "://" + host + ":" + port + path;
		} else {
			url = protocol + "://" + host + ":" + port + "/" + path;
		}

		// see if the pre-modified URL has query strings already
		int qsIndex = url.indexOf("?");
		String qsExtra = "";
		String curl = url;
		if (qsIndex > -1) {
			url = url.substring(0, qsIndex);
			qsExtra = curl.substring(qsIndex + 1);
		}

		if (extraContext != null && extraContext.length() > 0) {
			if (extraContext.startsWith("/")) {
				if (!url.endsWith("/")) {
					url = url + extraContext;
				} else {
					url = url + extraContext.substring(extraContext.indexOf("/") + 1, extraContext.length());
				}
			} else {
				url = url + "/" + extraContext;
			}
		}

		if (queryString != null && queryString.length() > 0) {
			if (extraContext != null && extraContext.startsWith("?")) {
				url = url + "?" + qsExtra + queryString.substring(1);
			} else {
				if (qsExtra.isEmpty()) {
					if (queryString.startsWith("?")) {
						url = url + queryString;
					} else {
						url = url + "?" + queryString;
					}
				} else {
					if (qsExtra.startsWith("?")) {
						url = url + qsExtra + "&" + queryString;
					} else {
						url = url + "?" + qsExtra + "&" + queryString;
					}
				}
			}
		}

		return url;
	}

	public String getSupportedVersionRange() {
		return supportedVersionRange;
	}

	public void setSupportedVersionRange(String supportedVersionRange) {
		this.supportedVersionRange = supportedVersionRange;
	}

	@Override
	public int compareTo(DME2Endpoint o) {
		if (o == null) {
			return -1; // This endpoint should be considered "closer" than a
						// null endpoint.
		} else {
			return (int) (getDistance() - o.getDistance());
		}
	}

	public boolean equals(Object other) {
		final DME2Endpoint de = (DME2Endpoint) other;
		return this.getHost().equals(de.getHost()) && this.getPort() == de.getPort()
				&& this.getContextPath().equals(de.getContextPath());
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

}
