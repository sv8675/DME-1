/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

/**
 * The Class DME2URI.
 */
public class DME2URI {

	private static final Logger logger = LoggerFactory.getLogger(DME2URI.class
			.getName());

	public static final String STICKY_SELECTOR_KEY = "stickySelectorKey";
	public static final String DATA_CONTEXT_KEY = "dataContext";
	public static final String MATCH_VERSION_RANGE_KEY = "matchVersionRange";
	public static final String PARTNER_KEY = "partner";

	/**
	 * The Enum DME2UriType.
	 */

	public enum DME2UriType {

		/** The DIRECT. */
		DIRECT,

		/** The RESOLVABLE. */
		RESOLVABLE,

		/** The SEARCHABLE. */
		SEARCHABLE,

		/**
		 * LOGICAL STANDARD URI that can be resolved to find other types CAN BE
		 * RESOLVE/SEARCH or a URI that has details required to register a
		 * endpoint in format of regular url pattern
		 */
		STANDARD
	}

	private final URI uri;

	private String stickySelectorKey;
	private String dataContext;
	private String envContext;
	private String partner;
	private String routeOffer;
	private String service;
	private String bindContext;
	private String subContext;
	private DME2UriType type;
	private String version;
	private long endpointReadTimeout;
	private long roundTripTimeout;
	private long connectTimeout;
	private String userName;
	private String password;
	private String realmName;
	private String[] allowedRoles;
	private String loginMethod;
	private boolean preferLocalEPs;
	private boolean ignoreFailoverOnExpire;
	private String queryParams;
	private String logicalService;
	private String namespace;
	private boolean useVersionRange = true;
	private String supportedVersionRange;
	private String driver;
	private DME2Configuration config;

	private static URI toURI(String rawURI) throws DME2Exception {
		if (rawURI == null) {
			throw new DME2Exception(rawURI, new ErrorContext());
		}

		try {
			final String encodedURI = rawURI.replaceAll("\\{", "%7B")
					.replaceAll("\\}", "%7D");
			return new URI(encodedURI);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);

		}
	}

	public DME2URI(DME2Configuration config, String uriStr) throws DME2Exception {
		this(toURI(uriStr));
		this.config = config;
	}

	public DME2URI(URI uri) {
		this.uri = uri;
		String host = uri.getHost();

		/*
		 * FORMAT 1 - SEARCHABLE:
		 * (http://DME2SEARCH/service=?/version=?/envContext
		 * =?/dataContext=?/partner=?) Normal use case. resolve relevent
		 * endpoints using dataContext and partner
		 */

		if (host == null || host.toUpperCase().equals("DME2SEARCH")) {
			type = DME2UriType.SEARCHABLE;
		}

		/*
		 * FORMAT 2 - RESOLVABLE:
		 * (http://DME2RESOLVE/service=?/version=?/envContext=?/routeOffer=?)
		 * Used to skip partition/partner/routeOffer resolution and go directly
		 * to a routeOffer
		 */

		else if (host.toUpperCase().equals("DME2RESOLVE")) {
			type = DME2UriType.RESOLVABLE;
		}

		/*
		 * FORMAT 3 - DIRECT:
		 * (http://host:port/service=?/version=?/envContext=?/routeOffer=?)
		 * Point to a specific host:port and call the service there. No
		 * lookup/resolution.
		 */

		else {
			type = DME2UriType.DIRECT;
		}

		String path = uri.getPath();

		String[] toks = path.split("/");
		String inSubContext = null;

		for (String tok : toks) {

			if (inSubContext != null) {
				inSubContext = inSubContext + "/" + tok;
				continue;
			}

			final String[] pair = tok.split("=");
			final String key = pair[0];

			if (key.equalsIgnoreCase("subcontext")) {
				inSubContext = "";
				continue;
			}

			if (pair.length == 2) {
				final String value = pair[1];

				if (key.equalsIgnoreCase("service")) {
					service = value;
				} else if (key.equalsIgnoreCase("version")) {
					version = value;
				} else if (key.equalsIgnoreCase(PARTNER_KEY)) {
					partner = value;
				} else if (key.equalsIgnoreCase("envcontext")) {
					envContext = value;
				} else if (key.equalsIgnoreCase(DATA_CONTEXT_KEY)) {
					dataContext = value;
				} else if (key.equalsIgnoreCase("routeoffer")) {
					routeOffer = value;
				} else if (key.equalsIgnoreCase("bindcontext")) {
					this.setBindContext(value);
				} else if (key.equalsIgnoreCase("subcontext")) {
					inSubContext = value;
				} else if (key.equalsIgnoreCase(STICKY_SELECTOR_KEY)) {
					stickySelectorKey = value;
				} else if (key.equalsIgnoreCase("ns")) {
					this.setNamespace(value);
				} else if (key.toLowerCase().equals("driver")) {
					driver = value;
				}
			}
		}
		// If service is not found from context path from DME2RESOLVE or
		// DME2SEARCH or provided DME2LOCAL/URI string then
		// the URI path could be of format
		// http://servicename/ContextPath?version=&envContext=&partner=&routeOffer=&stickySelectorKey=
		// Identify service name and additional details from URI hostname &
		// query params
		if (service == null && version == null && envContext == null) {
			// Check query params to see if
			// version/envContext/routeOffer/partner info are available
			// from there.
			String qParams = uri.getQuery();
			if (qParams != null) {
				String[] queryToks = qParams.split("&");

				for (String qtok : queryToks) {
					String[] pair = qtok.split("=");

					if (pair.length == 2) {
						final String key = pair[0];
						final String value = pair[1];
						if (key.equalsIgnoreCase("routeoffer")) {
							routeOffer = value;
						} else if (key.equalsIgnoreCase("version")) {
							version = value;
						} else if (key.equalsIgnoreCase("envcontext")) {
							envContext = value;
						} else if (key.equalsIgnoreCase(PARTNER_KEY)) {
							partner = value;
						} else if (key.equalsIgnoreCase("ns")) {
							this.setNamespace(value);
						}
					}
				}
			}
			if (version != null && envContext != null
					&& (routeOffer != null || partner != null)) {
				String uriServiceName = uri.getHost();
				String serviceUri = null;
				if (this.getNamespace() == null) {
					serviceUri = this.getReversed(uriServiceName);
				} else {
					serviceUri = uriServiceName;
				}

				if (uri.getPath() != null && !uri.getPath().equals("/")) {
					this.bindContext = uri.getPath();
				}
				// / Append bind context to service name
				String cpath = null;
				if (this.bindContext != null) {
					cpath = this.bindContext.startsWith("/") ? this.bindContext
							.substring(1, this.bindContext.length())
							: this.bindContext;
				}
				if (this.bindContext != null && cpath != null) {
					this.service = serviceUri + "/"
							+ cpath.replaceAll("\\.", "\\\\\\\\.");
				} else {
					this.service = serviceUri;
				}
				// Append bind context to service name

				// If the service name is successfully retrieved from the URI
				// hostname, then its using standard web address/uri
				if (this.service != null) {
					this.type = DME2UriType.STANDARD;
				}
			}
		}

		this.setSubContext(inSubContext);

		String queryString = uri.getQuery();

		if (queryString != null) {
			final String[] queryToks = queryString.split("&");

			for (String qtok : queryToks) {
				final String[] pair = qtok.split("=");

				if (pair.length == 2) {
					final String key = pair[0];
					final String value = pair[1];
					if (key.equalsIgnoreCase("endpointreadtimeout")) {
						try {
							// validate if endpointReadTimeout is more than
							// maximum
							// defined ( 5 mins )
							this.endpointReadTimeout = Math
									.min(Long.parseLong(value), config.getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT));
						} catch (Exception e) {
							logger.debug(null, null, "DME2URI",
									LogMessage.DEBUG_MESSAGE, "Exception", e);
							/* ignore error in parsing */
						}
					} else if (key.equalsIgnoreCase("roundtriptimeout")) {
						try {
							// validate if roundTripTimeout is more than maximum
							// defined ( 4 mins )
							this.roundTripTimeout = Math
									.min(Long.parseLong(value), config.getLong(DME2Constants.AFT_DME2_DEF_ROUNDTRIP_TIMEOUT_MS));
						} catch (Exception e) {

							this.roundTripTimeout = config.getLong(DME2Constants.AFT_DME2_DEF_ROUNDTRIP_TIMEOUT_MS);
						}
					} else if (key.equalsIgnoreCase("preferlocal")) {
						preferLocalEPs = true;
					} else if (key.equalsIgnoreCase("ignorefailoveronexpire")) {
						ignoreFailoverOnExpire = true;
					} else if (key.equalsIgnoreCase("connecttimeoutinms")) {
						try {
							// validate if connectTime is more than maximum
							// defined ( 4 secs )
							this.connectTimeout = Math
									.min(Long.parseLong(value), config.getLong(DME2Constants.AFT_DME2_EP_CONN_TIMEOUT));
						} catch (Exception e) {
							logger.debug(null, null, "DME2URI",
									LogMessage.DEBUG_MESSAGE, "Exception", e);
							/* ignore error in parsing */}
					} else if (key.equalsIgnoreCase("username")) {
						this.setUserName(value);
					} else if (key.equalsIgnoreCase("password")) {
						this.setPassword(value);
					} else if (key.equalsIgnoreCase("realm")) {
						this.setRealmName(value);
					} else if (key.equalsIgnoreCase("loginmethod")) {
						this.setLoginMethod(value);
					} else if (key.equalsIgnoreCase("allowedroles")
							&& value != null) {
						this.setAllowedRoles(value.split(","));
					} else if (key.equalsIgnoreCase(MATCH_VERSION_RANGE_KEY)) {
						useVersionRange = Boolean.parseBoolean(value);
					} else if (key.equalsIgnoreCase("supportedVersionRange")) {
						supportedVersionRange = value;
					} else {
						if (queryParams == null) {
							queryParams = key + "=" + value;
						} else {
							queryParams += "&" + key + "=" + value;
						}
					}
				}
			}
		}
	}

	/**
	 * Assert valid.
	 */
	public void assertValid() throws DME2Exception {
		ErrorContext ec = new ErrorContext().add("URI", uri.toString());

		switch (type) {
		case SEARCHABLE:
			if (partner == null) {
				throw new DME2Exception("AFT-DME2-9703", ec);
			}
			break;

		case RESOLVABLE:
		case DIRECT:
		case STANDARD:
			if (routeOffer == null && partner == null) {
				throw new DME2Exception("AFT-DME2-9704", ec);
			}
			break;

		default:
			throw new DME2Exception("AFT-DME2-9705", ec);
		}
		if (service == null) {
			throw new DME2Exception("AFT-DME2-9700", ec);
		}
		if (version == null) {
			throw new DME2Exception("AFT-DME2-9701", ec);
		}
		if (envContext == null) {
			throw new DME2Exception("AFT-DME2-9702", ec);
		}
	}

	/**
	 * Gets the data context.
	 * 
	 * @return the dataContext
	 */
	public String getDataContext() {
		return dataContext;
	}

	/**
	 * Gets the env context.
	 * 
	 * @return the envContext
	 */
	public String getEnvContext() {
		return envContext;
	}

	/**
	 * Gets the partner.
	 * 
	 * @return the partner
	 */
	public String getPartner() {
		return partner;
	}

	/**
	 * Gets the route offer.
	 * 
	 * @return the routeOffer
	 */
	public String getRouteOffer() {
		return routeOffer;
	}

	/**
	 * Gets the service.
	 * 
	 * @return the service
	 */
	public String getService() {
		return service;
	}

	/**
	 * Gets the type.
	 * 
	 * @return the type
	 */
	public DME2UriType getType() {
		return type;
	}

	/**
	 * Gets the version.
	 * 
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the data context.
	 * 
	 * @param dataContext
	 *            the dataContext to set
	 */
	public void setDataContext(String dataContext) {
		this.dataContext = dataContext;
	}

	/**
	 * Sets the env context.
	 * 
	 * @param envContext
	 *            the envContext to set
	 */
	public void setEnvContext(String envContext) {
		this.envContext = envContext;
	}

	/**
	 * Sets the partner.
	 * 
	 * @param partner
	 *            the partner to set
	 */
	public void setPartner(String partner) {
		this.partner = partner;
	}

	/**
	 * Sets the route offer.
	 * 
	 * @param routeOffer
	 *            the routeOffer to set
	 */
	public void setRouteOffer(String routeOffer) {
		this.routeOffer = routeOffer;
	}

	/**
	 * Sets the service.
	 * 
	 * @param service
	 *            the service to set
	 */
	public void setService(String service) {
		this.service = service;
	}

	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            the new type
	 */
	public void setType(DME2UriType type) {
		this.type = type;
	}

	/**
	 * Sets the version.
	 * 
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	public URI getOriginalURI() {
		return this.uri;
	}

	/**
	 * stickySelectorKey for a specific route
	 * 
	 * @param stickySelectorKey
	 */
	public void setStickySelectorKey(String stickySelectorKey) {
		this.stickySelectorKey = stickySelectorKey;
	}

	/**
	 * stickySelectorKey for a specific route
	 * 
	 * @return
	 */
	public String getStickySelectorKey() {
		return this.stickySelectorKey;
	}

	/**
	 * read timeout for a endpoint read from URI
	 * 
	 * @return
	 */
	public long getEndpointReadTimeout() {
		return endpointReadTimeout;
	}

	/**
	 * set read timeout for a endpoint
	 * 
	 * @param endpointReadTimeout
	 */
	public void setEndpointReadTimeout(long endpointReadTimeout) {
		this.endpointReadTimeout = endpointReadTimeout;
	}

	/**
	 * 
	 * @return
	 */
	public long getRoundTripTimeout() {
		return roundTripTimeout;
	}

	/**
	 * 
	 * @param roundTripTimeout
	 */
	public void setRoundTripTimeout(long roundTripTimeout) {
		this.roundTripTimeout = roundTripTimeout;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	/**
	 * Connect timeout for each endpoint
	 * 
	 * @return
	 */
	public long getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * Connect timeout for each endpoint
	 * 
	 * @param connectTimeout
	 */
	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}

	public String getRealmName() {
		return realmName;
	}

	public void setAllowedRoles(String[] newAllowedRoles) {

		if (newAllowedRoles == null) {
			this.allowedRoles = null;
		} else {
			this.allowedRoles = Arrays.copyOf(newAllowedRoles,
					newAllowedRoles.length);
		}
	}

	public String[] getAllowedRoles() {
		return allowedRoles;
	}

	public void setLoginMethod(String loginMethod) {
		this.loginMethod = loginMethod;
	}

	public String getLoginMethod() {
		return loginMethod;
	}

	@Override
	public String toString() {
		return uri.toString();
	}

	public boolean isLocalPreferred() {
		return this.preferLocalEPs;
	}

	/**
	 * to allow a context binding different from service URI
	 * 
	 * @param bindContext
	 */
	public void setBindContext(String bindContext) {
		this.bindContext = bindContext;
	}

	/**
	 * 
	 * @return
	 */
	public String getBindContext() {
		return bindContext;
	}

	/**
	 * to allow a subContext attached to contextPath published
	 * 
	 * @param subContext
	 */
	public void setSubContext(String subContext) {
		this.subContext = subContext;
	}

	/**
	 * 
	 * @return
	 */
	public String getSubContext() {
		return subContext;
	}

	/**
	 * 
	 * @param ignoreFailoverOnExpire
	 */
	public void setIgnoreFailoverOnExpire(boolean ignoreFailoverOnExpire) {
		this.ignoreFailoverOnExpire = ignoreFailoverOnExpire;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isIgnoreFailoverOnExpire() {
		return ignoreFailoverOnExpire;
	}

	public String getQueryParams() {
		return this.queryParams;
	}

	public String getSupportedVersionRange() {
		return supportedVersionRange;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	/**
	 * will return reversed string split by . E.g input =
	 * TestService.DME2.att.com , return = com.att.DME2.TestService
	 * 
	 * @param uriServiceName
	 * @return
	 */
	private String getReversed(String uriServiceName) {
		if (isIPAddress(uriServiceName)) {
			return null;
		}
		String domainNameArr[] = uriServiceName.split("\\.");
		StringBuffer reversedString = new StringBuffer();
		for (int i = domainNameArr.length - 1; i >= 0; i--) {
			if (i != 0) {
				reversedString.append(domainNameArr[i] + ".");
			} else {
				reversedString.append(domainNameArr[i]);
			}
		}
		return reversedString.toString();
	}

	/**
	 * returns true if input string represents an ip addresss.
	 * 
	 * @param ipAddress
	 * @return
	 */
	private boolean isIPAddress(String ipAddress) {
		String[] tokens = ipAddress.split("\\.");
		if (tokens.length != 4) {
			return false;
		}
		for (String str : tokens) {
			try {
				int i = Integer.parseInt(str);
				if ((i < 0) || (i > 255)) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	public String getLogicalService() {
		return logicalService;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public boolean isUsingVersionRanges() {
		return useVersionRange;
	}

	public String getRegistryServiceSearchKey() {
		if (type == DME2UriType.DIRECT || type == DME2UriType.SEARCHABLE) {
			return null;
		}
		if (type == DME2UriType.RESOLVABLE) {
			return service;
		}
		if (routeOffer != null && bindContext == null) {
			return service;
		}
		if (type == DME2UriType.STANDARD
				&& (routeOffer == null || !service.contains("/"))) {
			return null;
		}

		final String uriServiceName = service.split("/", 2)[0];
		if (namespace == null) {
			return uriServiceName + "*";
		} else {
			return namespace + DME2Constants.getNAME_SEP() + uriServiceName
					+ "*";
		}
	}

}
