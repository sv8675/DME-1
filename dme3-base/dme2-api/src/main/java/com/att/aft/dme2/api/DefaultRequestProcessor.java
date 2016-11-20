/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.util.UUID;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.api.util.DME2ExchangeRequestContext;
import com.att.aft.dme2.api.util.DME2ExchangeRequestHandler;
import com.att.aft.dme2.api.util.DME2FailoverFaultHandler;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.RequestHandlerIntf;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.IteratorMetricsEvent;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DmeUniformResource.DmeUrlType;
import com.att.aft.dme2.request.HttpRequest;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.request.RequestContext;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2Utils;
import com.att.aft.dme2.util.ErrorContext;
import com.att.aft.dme2.util.InternalConnectionFailedException;

/**
 * Default request processor class. This will be used by default in the core
 * classes if client does not specify customized class for request processor
 *
 */
public class DefaultRequestProcessor implements RequestProcessorIntf {

	public RequestInvokerIntf invoker = null;
	/**
	 * special request flag that is set to force preferred Route offer from
	 * other routes with different sticky key
	 */
	private boolean forcePreferredRouteOffer;
	// public final static String SERVICE ="service";
	private static final Logger logger = LoggerFactory.getLogger(DefaultRequestProcessor.class.getName());
	private DME2Configuration config;

	private DME2EndpointReference currentEndpointReference;
	/** The client. */
	// private static HttpClient client = null;
	/**
	 * Indicates that the request is being retried after a previously
	 * unsuccessful attempt
	 */
	private boolean attemptingRetry;
	/**
	 * String buffer that holds infromation in all RouteOffers tried during the
	 * request. This buffer will be used for trace logging
	 */
	private final StringBuffer routeOfferBuffer = new StringBuffer();
	/**
	 * context to append with resolved host:port
	 */
	private String context;
	private boolean isEndpointResolved;
	/**
	 * The actual request url (currentEndpoint+subContext+queryParms)
	 */
	private String currentFinalUrl = null;
	/**
	 * The time that the actual send to the last endpoint started
	 */
	private long sendStart;
	/**
	 * Captures what parameter is used for timeout, used for logging purpose
	 */
	private String timeoutString;
	/**
	 * Query string provided connect timeout *
	 */
	private long connectTimeout;
	/**
	 * Query string provided endpoint read timeout *
	 */
	private long qendpointReadTimeOut;
	/**
	 * Boolean used to know whether endpoints in local host/container are
	 * preferred
	 */
	private boolean preferLocal = false;
	/**
	 * the current complete url
	 */
	private String url = null;

	IteratorMetricsEvent iteratorMetricsEvent = null;

	/*
	 * private EndpointReference getNextResolvedEndpoint(RequestContext context)
	 * { // port all the code in Exchange that traverses through the iterator
	 * and gets the next available endpoint and resolves it here //this code
	 * will be common to all protocols. Handle the case wherein a fresh iterator
	 * has to be obtained in case of ws vs //using the same iterator in case of
	 * http.
	 *
	 * return null; }
	 */
	public DefaultRequestProcessor(RequestInvokerIntf invoker) {
		this.invoker = invoker;
	}

	/*
	 * This method is used by DME2 Exchange
	 */

	public boolean send(RequestContext context, DME2EndpointReference endpoint, DME2Payload payload)
			throws DME2Exception {

		boolean isEndpointResolved = resolveFinalEndpointURL(context, endpoint);
		if (isEndpointResolved) {
			// 3). call the invoker
			try {
				context.getRequest().setLookupUri(endpoint.getEndpoint().toURLString());
				invoker.execute(ActionType.SEND, context, payload);
			} catch (InternalConnectionFailedException e) {
				// iterator endpoint has to be marked stale and traceroute has
				// to be
				// updated
				// any client supplied reply handlers will have to be called
				// the request will have to be submitted to retry threadpool
				return false;
			}
		}
		return isEndpointResolved;
	}

	/**
	 * This method is the starting point to process the client call Send. It
	 * does mainly below 3 things. 1). Initialize invoker 2). Get next resolved
	 * end point 3). Call execute method on invoker
	 *
	 * @param context
	 *            RequestContext object
	 * @param payload
	 *            Payload object
	 */
	public void send(RequestContext context, DME2Payload payload) throws DME2Exception {
		// 1). initialize invoker
		invoker.init(context, ActionType.SEND, payload);

		// 2). get next resolved endpoint

		/* Creating the Iterator. */
		String conversationID = context.getLogContext().getConversationId();
		DME2BaseEndpointIterator iterator = null;
		DME2EndpointURLFormatter urlFormatterImpl = null;
		DME2Manager manager = context.getMgr();
		config = manager.getConfig();

		// invoke request handlers
		// String preferredRouteOffer = getPreferredRouteOffer(
		// context.getRequest() );
		invokeRequestHandlers(context.getRequest());

		iterator = EndpointIteratorFactory.getDefaultEndpointIteratorBuilder(config)
				.setServiceURI(context.getRequest().getLookupUri()).setProps(manager.getProperties())
				.setManager(manager).setUrlFormatter(urlFormatterImpl)
				.setIteratorEndpointOrderHandlers(((HttpRequest)context.getRequest()).getEndpointOrderHandlers())
				.setIteratorRouteOfferOrderHandlers(((HttpRequest)context.getRequest()).getIteratorRouteOfferOrderHandlers()).build();

		/*
		 * Checking if the Iterator has next. If it doesn't, it means that no
		 * endpoints were resolved
		 */
		if (!iterator.hasNext()) {

			// TODO: metrics when no endpoints are available
			// context.getRequestMetricsEventHolder().getIteratorMetricsEvent().setServiceUri(serviceUri);

			/* Throw Exception */
			ErrorContext ec = new ErrorContext();
			ec.add(DME2Constants.SERVICE, context.getRequest().getLookupUri());
			if (iterator.getRouteOffersTried() != null) {
				ec.add(DME2Constants.ROUTE_OFFER_TRIED, iterator.getRouteOffersTried());
			}

			DME2Exception e = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0702, ec);
			logger.error(conversationID, null, DME2Constants.EXP_CORE_AFT_DME2_0702, e.getErrorMessage());
			throw e;
		}

		/*
		 * Use the enpoints returned in the DME2EndpointIterator to service the
		 * client request. The iterator has already organized and grouped the
		 * endpoints by RouteOffer, sequence, and distance.
		 */

		while (iterator.hasNext() && !isEndpointResolved) {

			DME2EndpointReference next = iterator.next();

			resolveFinalEndpointURL(context, next);
			// resolveFinalRequestURLFromIterator(context, iterator);

		} // End iterator.hasNext()

		if (!isEndpointResolved) {
			/* Throw Exception */
			ErrorContext ec = new ErrorContext();
			ec.add(DME2Constants.SERVICE, context.getRequest().getLookupUri());
			ec.add(DME2Constants.ROUTE_OFFER_TRIED, routeOfferBuffer.toString());

			DME2Exception e = new DME2Exception(DME2Constants.EXP_CORE_AFT_DME2_0702, ec);
			logger.error(conversationID, null, DME2Constants.EXP_CORE_AFT_DME2_0702, e.getErrorMessage());
			throw e;
		}

		this.setReadTimeout(context);

		// try {
		this.sendStart = System.currentTimeMillis();
		// check context.getMgr().getGlobalNoticeCache().add -- find this
		// out...usage is switching between primary and secondary route offers
		// logging stale point is restored
		String searchFilter = "";
		if (iterator.getCurrentDME2RouteOffer() != null
				&& iterator.getCurrentDME2RouteOffer().getSearchFilter() != null) {
			searchFilter = iterator.getCurrentDME2RouteOffer().getSearchFilter();
		}
		if (iterator.getCurrentDME2EndpointRouteOffer() != null && context.getMgr().getGlobalNoticeCache()
				.remove(context.getRequest().getLookupUri() + ":" + searchFilter)) {
			logger.info(context.getLogContext().getConversationId(), null, LogMessage.EXCH_OFFER_RESTORE.toString(),
					searchFilter);
		}

		/*
		 * Client successfully submitted the request. (This doesn't mean the the
		 * entire transaction was successful, just the submit portion. Response
		 * could potentially return in error)
		 */
		// return;

		/*
		 * } catch (IOException io) {
		 * //sendTimer.completeStep(EventSampler.FAILED_URL_STEP_PREFIX,
		 * Collections.<String, String> singletonMap(EXCEPTION,
		 * io.getClass().getCanonicalName()));
		 * //logger.error(context.getLogContext().getConversationId(), null,
		 * DME2Constants.EXP_CORE_AFT_DME2_0704, io.getMessage());
		 * //this.exception = io; }
		 */

		try {
			// start collecting metrics
			iterator.setMetricsConversationId(getConversationId(context));
			iterator.start(createIteratorMetricsEvent(null, currentEndpointReference.getEndpoint().getProtocol(),
					currentEndpointReference.getEndpoint().toURLString(),
					currentEndpointReference.getEndpoint().getHost(), context));
		} catch (Exception metricsException) {
			logger.info(context.getLogContext().getConversationId(), null,
					"DefaultRequestProcessor.send: error in capturing start metrics for the iterator endpoint reference: {}",
					currentEndpointReference != null ? currentEndpointReference.toString() : null);
		}

		// 3). call the invoker
		try {
			context.getRequest().setLookupUri(this.getUrl());
			this.invoker.createExchange(this.getUrl(), context, iterator);
			invoker.execute(ActionType.SEND, context, payload);
		} catch (InternalConnectionFailedException e) {
			// iterator endpoint has to be marked stale and traceroute has to be
			// updated
			// any client supplied reply handlers will have to be called
			// the request will have to be submitted to retry threadpool
		}
	}

	private String getConversationId(final RequestContext context) {
		String conversationId = null;

		try {
			if (conversationId == null) {
				conversationId = ((HttpRequest) context.getRequest()).getRequestHeaders().get(DME2Constants.JMSMESSAGEID);
			}
		} catch (Exception e) {
		}
		return conversationId;
	}

	private void invokeRequestHandlers(Request request) {
		if (!config.getBoolean(DME2Constants.AFT_DME2_ALLOW_INVOKE_HANDLERS)) {
			return;
		}
		String preferredRouteOffer = null;
		String preferredVersion = null;

		String[] requestHandlers = getExchangeRequestHandlers(request);
		String requestUrl = request.getLookupUri();
		if (requestHandlers != null) {
			long start = System.currentTimeMillis();

			// Create request context object
			DME2ExchangeRequestContext ctxData = new DME2ExchangeRequestContext(requestUrl, request.getReadTimeout(),
					DME2Utils.getQueryParamsAsMap(request.getQueryParams()), request.getClientHeaders());

			for (int i = 0; i < requestHandlers.length; i++) {
				String handlerName = requestHandlers[i];
				try {
					// Try loading class name
					Object obj = DME2Utils.loadClass(config, requestUrl, handlerName);

					if (obj != null && obj instanceof RequestHandlerIntf) {
						RequestHandlerIntf handler = (RequestHandlerIntf) obj;
						preferredRouteOffer = handler.getPreferredRouteOffer(request);

						logger.debug(null, "getPreferredRouteOffer", LogMessage.EXCH_INVOKE_HANDLER, "handleRequest",
								handlerName, (System.currentTimeMillis() - start));
						logger.debug(null, "getPreferredRouteOffer", "{}:handleRequest invoked", handlerName);
					} else if (obj != null && obj instanceof DME2ExchangeRequestHandler) {
						DME2ExchangeRequestHandler handler = (DME2ExchangeRequestHandler) obj;
						try {
							handler.handleRequest(ctxData);
						} catch (Throwable e) {
							// ignore exception in loading classname or invoking
							// handleRequest
							logger.debug(null, "getPreferredRouteOffer",
									"{}:handleRequest invoke failed with exception:", handlerName, e);
							logger.warn(null, "getPreferredRouteOffer", LogMessage.EXCH_INVOKE_FAIL, "handleRequest",
									handlerName, e);
						}
						preferredRouteOffer = ctxData.getPreferredRouteOffer();
						logger.debug(null, "getPreferredRouteOffer", "{}:handleRequest invoked", handlerName);
					}
				} catch (Throwable e) {
					// ignore exception in loading classname or invoking
					// handleRequest
					logger.debug(null, "getPreferredRouteOffer", "{}:handleRequest invoke failed with exception:",
							handlerName, e);
					logger.warn(null, "getPreferredRouteOffer", LogMessage.EXCH_INVOKE_FAIL, "handleRequest",
							handlerName, e);
				}
			}

			preferredVersion = ctxData.getPreferredVersion();
			// if preferredrouteoffer was found then make sure that you set a
			// flag that indicates that the routeoffer was set by client
			forcePreferredRouteOffer = ctxData.isForcePreferredRouteOffer();

			// if (requestHandlersInvoked)
			// {
			// this.requestHandlersElapsedTime = System.currentTimeMillis() -
			// start;
			// }
		}
		logger.debug(null, "getPreferredRouteOffer", "Returning {} as preferred route offer", preferredRouteOffer);
		// Go ahead and perform the config "puts" here
		if (preferredRouteOffer != null) {
			config.setOverrideProperty(DME2Constants.DME2_PREFERRED_ROUTEOFFER, preferredRouteOffer);
		}
		if (preferredVersion != null) {
			config.setOverrideProperty(DME2Constants.AFT_DME2_PREFERRED_VERSION, preferredVersion);
		}
		config.setOverrideProperty(DME2Constants.AFT_DME2_FLAG_FORCE_PREFERRED_ROUTE_OFFER,
				forcePreferredRouteOffer ? "true" : "false");
	}

	private String[] getExchangeRequestHandlers(Request request) {
		String requestHandlers = request.getClientHeaders()
				.get(config.getProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS_KEY));

		logger.debug(null, "getExchangeRequestHandlers", "{}:handleRequest invoked", requestHandlers);
		if (requestHandlers != null && requestHandlers.length() > 0) {
			// Found jms property in message that carries ignore from client
			try {
				String[] requestHandlersArr = requestHandlers.split(",");
				// debugIt("REQUEST_HANDLERS_CHAIN_HEADER_PROPERTY",requestHandlers
				// +"");
				// debugIt("REQUEST_HANDLERS_CHAIN_HEADERS_SIZE",requestHandlersArr.length
				// );
				return requestHandlersArr;
			} catch (Exception e) {
				logger.debug(null, "getExchangeRequestHandlers", LogMessage.EXCH_READ_HANDLER_FAIL,
						"getExchangeRequestHandlers", e);
				return null;
			}
		} else {
			requestHandlers = config
					.getProperty(config.getProperty(DME2Constants.AFT_DME2_EXCHANGE_REQUEST_HANDLERS_KEY), "");

			if (requestHandlers != null && requestHandlers.length() > 0) {
				String[] requestHandlersArr = requestHandlers.split(",");
				logger.debug(null, "getExchangeRequestHandlers", "REQUEST_HANDLERS_CHAIN_MGR_PROPERTY",
						requestHandlers);
				return requestHandlersArr;
			}
		}
		return null;
	}

	/**
	 * Set endpoint read timeout for client call Setting up
	 * AFT_DME2_EP_READ_TIMEOUT_MS will be allowed as below. 1) in the root
	 * config file. applies to ALL services unless its overriden below.
	 * Overrides a coded default. 2) in the queue URI as a query parameter;
	 * overrides #1 3) as a JMS property (dme2-jms) or HTTP header property
	 * (dme2-api); overrides #2
	 *
	 * @throws DME2Exception
	 */
	private void setReadTimeout(RequestContext context) throws DME2Exception {
		context.getRequest();
		// String jmsPropertyEndpointTimeout =
		// this.headers.get(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS);
		long jmsPropertyEndpointTimeout = context.getRequest().getReadTimeout();
		long timeout = 0;
		if (jmsPropertyEndpointTimeout > 0) {
			// Found jms property in message that carries timeout from client
			try {
				timeout = jmsPropertyEndpointTimeout;
			} catch (Exception e) {
				// use default
				timeout = config.getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS);
			}
			this.timeoutString = "JMSHeader-AFT_DME2_EP_READ_TIMEOUT_MS=" + timeout;
		} else if (this.qendpointReadTimeOut > 0) {
			// Found jms property in message that carries timeout from client
			this.timeoutString = "URIQueryString-endpointReadTimeout=" + this.qendpointReadTimeOut;
			timeout = this.qendpointReadTimeOut;
		} else {
			// Use default value
			timeout = config.getLong(DME2Constants.AFT_DME2_EP_READ_TIMEOUT_MS);
			this.timeoutString = DME2Constants.CFG_AFT_DME2_EP_READ_TIMEOUT_MS + "=" + timeout;
		}

		context.getRequest().setReadTimeout(timeout);

	}

	private boolean invokeReplyHandlersRouteOfferFailover(RequestContext context, String routeOffer, String version) {
		if (!config.getBoolean(DME2Constants.DME2_ALLOW_INVOKE_HANDLERS, true)) {
			return false;
		}
		String[] failoverHandlers = DME2Utils.getFailoverHandlers(config, context.getRequest().getClientHeaders());
		if (failoverHandlers != null) {
			String requestUrl = context.getRequest().getLookupUri();
			DME2ExchangeFaultContext ctxData = new DME2ExchangeFaultContext(requestUrl, 0,
					context.getRequest().getClientHeaders(), routeOffer, version, requestUrl, null);
			logger.debug(null, "DefaultRequestProcessor.invokeReplyHandlersRouteOfferFailover",
					"invokeReplyHandlersRouteOfferFault routeOffer={};requestUrl={}", routeOffer,
					context.getRequest().getLookupUri());
			for (int i = 0; i < failoverHandlers.length; i++) {
				long start = System.currentTimeMillis();
				String handlerName = failoverHandlers[i];
				try {
					Object obj = DME2Utils.loadClass(config, requestUrl, handlerName);
					if (obj instanceof DME2FailoverFaultHandler) {
						DME2FailoverFaultHandler handler = (DME2FailoverFaultHandler) obj;
						handler.handleRouteOfferFailover(ctxData);
						logger.debug(null, "DefaultRequestProcessor.invokeReplyHandlersRouteOfferFailover",
								LogMessage.EXCH_INVOKE_HANDLER, "handleRouteOfferFailover", handlerName,
								(System.currentTimeMillis() - start));
					} 

				} catch (Throwable e1) {
					// ignore exception in loading classname or invoking
					// handleRequest
					logger.warn(null, "DefaultRequestProcessor.invokeReplyHandlersRouteOfferFailover",
							LogMessage.EXCH_INVOKE_FAIL, "handleRouteOfferFailover", handlerName, e1);

				}
			}
		} else {
			return false;
		}
		return true;
	}

	/*
	 * Returns the final Endpoint URL from the iterator that will be used to
	 * service the request.
	 */
	private void resolveFinalRequestURLFromIterator(RequestContext context, DME2BaseEndpointIterator iterator) {
		boolean isEndpointResolved = false;
		while (iterator.hasNext()) {
			DME2EndpointReference next = iterator.next();

			// if the next endpoint reference has a different route offer and a
			// different sequence, log the failover
			if (currentEndpointReference != null && next.getRouteOffer() != currentEndpointReference.getRouteOffer()
					&& next.getSequence() != currentEndpointReference.getSequence()) {

				logger.info(null, LogMessage.EXCH_FAILOVER.toString(), context.getRequest().getLookupUri());
				// invoke the the failover method passing the route offer that
				// is failing.
				String routeOffer = null;
				String version = null;
				if (currentEndpointReference != null) {
					if (currentEndpointReference.getRouteOffer() != null
							&& currentEndpointReference.getRouteOffer().getRouteOffer() != null) {
						routeOffer = currentEndpointReference.getRouteOffer().getRouteOffer().getName();
					}
					if (currentEndpointReference.getEndpoint() != null) {
						version = currentEndpointReference.getEndpoint().getServiceVersion();
					}
				}
				invokeReplyHandlersRouteOfferFailover(context, routeOffer, version);
			}

			// now we use the next one
			currentEndpointReference = next;

			if (attemptingRetry) {

				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_RETRY_ROUTE_OFFER,
						currentEndpointReference.getRouteOffer() != null
								? currentEndpointReference.getRouteOffer().getSearchFilter() : null);
				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_RETRY_SEQUENCE,
						currentEndpointReference.getSequence());
				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_RETRY_ENDPOINT,
						currentEndpointReference.getEndpoint().toURLString());
				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_RETRY_DISTANCE_BAND,
						currentEndpointReference.getDistanceBand().toString());
			} else {
				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_ROUTE_OFFER,
						currentEndpointReference.getRouteOffer() != null
								? currentEndpointReference.getRouteOffer().getSearchFilter() : null);
				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_SEQUENCE,
						currentEndpointReference.getSequence());
				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_ENDPOINT,
						currentEndpointReference.getEndpoint().toURLString());
				logger.debug(null, "resolveFinalRequestURLFromIterator", DME2Constants.CURRENT_DISTANCE_BAND,
						currentEndpointReference.getDistanceBand().toString());
			}

			/*
			 * Appending value of currently used routeOffer to the
			 * routeOfferBuffer which will be used as trace information
			 */
			if (currentEndpointReference.getRouteOffer() != null) {
				if (routeOfferBuffer.length() == 0) {
					routeOfferBuffer.append(currentEndpointReference.getRouteOffer().getSearchFilter());
				} else {
					if (!routeOfferBuffer.toString()
							.contains(currentEndpointReference.getRouteOffer().getSearchFilter())) {
						routeOfferBuffer.append(",");
						routeOfferBuffer.append(currentEndpointReference.getRouteOffer().getSearchFilter());
					}
				}
			}

			/*
			 * Check endpoint context path to see if it matches a restful URI
			 * pattern if client had provided a context path or subContext path,
			 * then we would try to match using pattern
			 */
			if (this.getContext() != null
					&& context.getUniformResource().getUrlType().name().equals(DmeUrlType.STANDARD)) {
				if (!this.matchServletPath(this.getContext(), context.getRequest().getSubContext(),
						currentEndpointReference.getEndpoint().getContextPath())) {
					continue;
				}
			} else if (context.getUniformResource().getUrlType().name().equals(DmeUrlType.DIRECT)) {
				String service = context.getUniformResource().getService();
				if (service != null) {
					if (service.contains("/")) {
						String cp = service.substring(service.indexOf("/"));
						if (!this.matchServletPath(cp, context.getUniformResource().getSubContext(),
								currentEndpointReference.getEndpoint().getContextPath())) {
							continue;
						} else {
							this.setContext(cp);
						}
					}
				}
			}

			String hostFromArgs = config.getProperty(DME2Constants.AFT_DME2_CONTAINER_HOST_KEY);

			/*
			 * Check if client preferred to use to use endpoints on the this
			 * (local) host/container.
			 */
			if (preferLocal) {
				String epHost = currentEndpointReference.getEndpoint().getHost();
				if (epHost.equalsIgnoreCase(hostFromArgs)
						|| DME2Utils.isHostMyLocalHost(hostFromArgs, config.getBoolean(DME2Constants.DME2_DEBUG))) {
					/*
					 * Replacing original host with the localhost for this
					 * endpoint
					 */
					currentEndpointReference.getEndpoint().setHost(hostFromArgs);
					logger.debug(null, DME2Constants.LOAD_LOCAL_SEP_OK,
							currentEndpointReference.getEndpoint().toURLString());
				}
			} else {
				logger.debug(null, DME2Constants.LOAD_LOCAL_SEP_OK,
						currentEndpointReference.getEndpoint().toURLString());
			}

			/* Setting the final URL to use for the client request */
			this.setUrl(currentEndpointReference.getEndpoint().toURLString(this.getContext(),
					context.getRequest().getSubContext(), context.getRequest().getQueryParams()));
			isEndpointResolved = true;
			break;
		} // End iterator.hasNext()

	}

	private boolean resolveFinalEndpointURL(RequestContext context, DME2EndpointReference next) {
		// if the next endpoint reference has a different route offer and a
		// different sequence, log the failover
		if (currentEndpointReference != null && next.getRouteOffer() != currentEndpointReference.getRouteOffer()
				&& next.getSequence() != currentEndpointReference.getSequence()) {

			logger.info(null, LogMessage.EXCH_FAILOVER.toString(), context.getRequest().getLookupUri());
			// invoke the the failover method passing the route offer that
			// is failing.
			String routeOffer = null;
			String version = null;
			if (currentEndpointReference != null) {
				if (currentEndpointReference.getRouteOffer() != null
						&& currentEndpointReference.getRouteOffer().getRouteOffer() != null) {
					routeOffer = currentEndpointReference.getRouteOffer().getRouteOffer().getName();
				}
				if (currentEndpointReference.getEndpoint() != null) {
					version = currentEndpointReference.getEndpoint().getServiceVersion();
				}
			}
			invokeReplyHandlersRouteOfferFailover(context, routeOffer, version);
		}

		// now we use the next one
		currentEndpointReference = next;

		if (attemptingRetry) {

			logger.debug(null, DME2Constants.CURRENT_RETRY_ROUTE_OFFER, currentEndpointReference.getRouteOffer() != null
					? currentEndpointReference.getRouteOffer().getSearchFilter() : null);
			logger.debug(null, "resolveFinalEndpointURL", DME2Constants.CURRENT_RETRY_SEQUENCE,
					currentEndpointReference.getSequence());
			logger.debug(null, "resolveFinalEndpointURL", DME2Constants.CURRENT_RETRY_ENDPOINT,
					currentEndpointReference.getEndpoint().toURLString());
			logger.debug(null, "resolveFinalEndpointURL", DME2Constants.CURRENT_RETRY_DISTANCE_BAND,
					currentEndpointReference.getDistanceBand().toString());
		} else {
			logger.debug(null, "resolveFinalEndpointURL", "{} {}", DME2Constants.CURRENT_ROUTE_OFFER,
					currentEndpointReference.getRouteOffer() != null
							? currentEndpointReference.getRouteOffer().getSearchFilter() : null);
			logger.debug(null, "resolveFinalEndpointURL", "{} {}", DME2Constants.CURRENT_SEQUENCE,
					currentEndpointReference.getSequence());
			logger.debug(null, "resolveFinalEndpointURL", "{} {}", DME2Constants.CURRENT_ENDPOINT,
					currentEndpointReference.getEndpoint().toURLString());
			logger.debug(null, "resolveFinalEndpointURL", "{} {}", DME2Constants.CURRENT_DISTANCE_BAND,
					currentEndpointReference.getDistanceBand().toString());
		}

		/*
		 * Appending value of currently used routeOffer to the routeOfferBuffer
		 * which will be used as trace information
		 */
		if (currentEndpointReference.getRouteOffer() != null) {
			if (routeOfferBuffer.length() == 0) {
				routeOfferBuffer.append(currentEndpointReference.getRouteOffer().getSearchFilter());
			} else {
				if (!routeOfferBuffer.toString().contains(currentEndpointReference.getRouteOffer().getSearchFilter())) {
					routeOfferBuffer.append(",");
					routeOfferBuffer.append(currentEndpointReference.getRouteOffer().getSearchFilter());
				}
			}
		}

		/*
		 * Check endpoint context path to see if it matches a restful URI
		 * pattern if client had provided a context path or subContext path,
		 * then we would try to match using pattern
		 */
		if (this.getContext() != null && context.getUniformResource().getUrlType().equals(DmeUrlType.STANDARD)) {
			if (!this.matchServletPath(this.getContext(), context.getRequest().getSubContext(),
					currentEndpointReference.getEndpoint().getContextPath())) {
				return false;
			}
		}

		// else if
		// (context.getUniformResource().getUrlType().name().equals(DmeUrlType.DIRECT))
		// {
		else if (context.getUniformResource() != null
				&& !DmeUrlType.DIRECT.equals(context.getUniformResource().getUrlType())) {
			String service = context.getUniformResource().getService();
			if (service != null) {
				if (service.contains("/")) {
					String cp = service.substring(service.indexOf("/"));
					if (!this.matchServletPath(cp, context.getUniformResource().getSubContext(),
							currentEndpointReference.getEndpoint().getContextPath())) {
						return false;
					} else {
						this.setContext(cp);
					}
				}
			}
		}

		String hostFromArgs = config.getProperty(DME2Constants.AFT_DME2_CONTAINER_HOST_KEY);

		/*
		 * Check if client preferred to use to use endpoints on the this (local)
		 * host/container.
		 */
		if (preferLocal) {
			String epHost = currentEndpointReference.getEndpoint().getHost();
			if (epHost.equalsIgnoreCase(hostFromArgs)
					|| DME2Utils.isHostMyLocalHost(hostFromArgs, config.getBoolean(DME2Constants.DME2_DEBUG))) {
				/*
				 * Replacing original host with the localhost for this endpoint
				 */
				currentEndpointReference.getEndpoint().setHost(hostFromArgs);
				logger.debug(null, DME2Constants.LOAD_LOCAL_SEP_OK,
						currentEndpointReference.getEndpoint().toURLString());
			}
		} else {
			logger.debug(null, DME2Constants.LOAD_LOCAL_SEP_OK, currentEndpointReference.getEndpoint().toURLString());
		}

		/* Setting the final URL to use for the client request */

		this.setUrl(currentEndpointReference.getEndpoint().toURLString(
				this.getContext() == null ? context.getRequest().getContext() : this.getContext(),
				// Changed due to setContext above being in this object, not in
				// the original request. See DME2 for comparison
				// context.getRequest().getContext()
				context.getRequest().getSubContext(), context.getRequest().getQueryParams()));
		isEndpointResolved = true;
		return isEndpointResolved;
	}

	/**
	 * @param endpointPaths
	 * @return
	 */
	private boolean matchServletPath(String context, String subContext, String endpointPaths) {
		String clientURLContext = context != null ? context : "" + "/" + subContext != null ? subContext : "";
		clientURLContext = clientURLContext.replaceAll("//", "/");

		String[] contextPaths = endpointPaths.split(",");
		
		if (contextPaths.length == 1) {
			if (contextPaths[0].equalsIgnoreCase("/")) {
				return true;
			}
		}

		for (int j = 0; j < contextPaths.length; j++) {
			String toks[] = contextPaths[j].split("/");
			StringBuffer pathToCompare = new StringBuffer();

			for (int i = 0; i < toks.length; i++) {

				if (toks[i].length() > 0) {
					if (toks[i].startsWith("{") && toks[i].endsWith("}")) {
						pathToCompare.append("/.*");
					} else if (toks[i].startsWith("(") && toks[i].endsWith(")")) {
						pathToCompare.append("/.*");
					} else {
						pathToCompare.append("/" + toks[i]);
					}
				}
			}

			if (pathToCompare.length() > 0) {
				if (clientURLContext.matches(pathToCompare.toString())) {
					return true;
				}
			}
		}

		return false;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getCurrentFinalUrl() {
		return currentFinalUrl;
	}

	public void setCurrentFinalUrl(String currentFinalUrl) {
		this.currentFinalUrl = currentFinalUrl;
	}

	public void sendAndWait(RequestContext context, DME2Payload payload) throws DME2Exception {
		// TODO ?? Auto-generated method stub, call same send
		send(context, payload);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
		this.currentFinalUrl = url;
	}

	public long getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public boolean isPreferLocal() {
		return preferLocal;
	}

	public void setPreferLocal(boolean preferLocal) {
		this.preferLocal = preferLocal;
	}

	public void retry(RequestContext context, DME2Payload payload) throws DME2Exception {
		// TODO Auto-generated method stub

	}

	public void failover(RequestContext context, DME2Payload payload) throws DME2Exception {
		// TODO Auto-generated method stub

	}

	private String generateUniqueTransactionReference() {
		StringBuffer uniqueReference = new StringBuffer();

		uniqueReference.append(this.hashCode());
		uniqueReference.append("-");
		uniqueReference.append(UUID.randomUUID().toString());

		return uniqueReference.toString();
	}

	private IteratorMetricsEvent createIteratorMetricsEvent(final String conversationId, final String protocol,
			final String serviceUri, final String clientIp, RequestContext context) {
		String role = config.getProperty(DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE);// DME2Constants.AFT_DME2_INTERFACE_CLIENT_ROLE;
		IteratorMetricsEvent iteratorMetricsEvent = new IteratorMetricsEvent();

		iteratorMetricsEvent.setClientIp(clientIp);
		iteratorMetricsEvent.setConversationId(conversationId);
		iteratorMetricsEvent.setProtocol(protocol);
		iteratorMetricsEvent.setRole(role);
		iteratorMetricsEvent.setServiceUri(serviceUri);
		iteratorMetricsEvent.setEventTime(System.currentTimeMillis());
		String partner = context.getRequest().getClientHeaders().get(DME2Constants.DME2_REQUEST_PARTNER);
		if (partner == null) {
			partner = context.getRequest().getClientHeaders().get(DME2Constants.DME2_REQUEST_PARTNER_CLASS);
		}
		iteratorMetricsEvent.setPartner(partner);
		return iteratorMetricsEvent;
	}

}