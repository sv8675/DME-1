package com.att.aft.dme2.iterator;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.event.EventProcessor;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.domain.IteratorCreatingAttributes;
import com.att.aft.dme2.iterator.exception.IteratorException;
import com.att.aft.dme2.iterator.exception.IteratorException.IteratorErrorCatalogue;
import com.att.aft.dme2.iterator.factory.EndpointIteratorFactory;
import com.att.aft.dme2.iterator.helper.AvailableEndpoints;
import com.att.aft.dme2.iterator.helper.EndpointPreference;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;
import com.att.aft.dme2.iterator.service.EndpointIteratorBuilder;
import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.iterator.service.IteratorRouteOfferOrderHandler;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class DefaultEndpointIteratorBuilder implements EndpointIteratorBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEndpointIteratorBuilder.class.getName());
	private String initialUrl;
	private Properties props;

	private List<IteratorEndpointOrderHandler> endpointOrderHandlers = new ArrayList<IteratorEndpointOrderHandler>();
	private List<IteratorRouteOfferOrderHandler> routeOfferOrderHandlers = new ArrayList<IteratorRouteOfferOrderHandler>();
	private DME2EndpointURLFormatter urlFormatter;
	private DME2Manager manager;
	private DmeUniformResource uniformResource = null;
	private String preferredRouteOffer = null;
	private String preferredUrl = null;
	private EventProcessor requestEventProcessor = null;
	private EventProcessor replyEventProcessor = null;
	private EventProcessor faultEventProcessor = null;
	private EventProcessor timeoutEventProcessor = null;
	private DME2Configuration config;
  private String preferredVersion;
  private String preferredRouteOfferForced;

  public DefaultEndpointIteratorBuilder(DME2Configuration config) {
		this.config = config;
	}

	public DmeUniformResource getUniformResource() {
		return uniformResource;
	}

	public void setUniformResource(DmeUniformResource uniformResource) {
		this.uniformResource = uniformResource;
	}

	public String getPreferredUrl() {
		return preferredUrl;
	}

	private String getDefaultPreferredUrl() {
		return EndpointPreference.resolvePreferredConnection(config, getProps());
	}

	public void setPreferredUrl(String preferredUrl) {
		this.preferredUrl = preferredUrl;
	}

	private String getDefaultRouteOfferPreference() {
		return EndpointPreference.resolvePreferredRouteOffer(config, getProps());
	}

	public String getPreferredRouteOffer() {
		return preferredRouteOffer;
	}

	public void setPreferredRouteOffer(String preferredRouteOffer) {
		this.preferredRouteOffer = preferredRouteOffer;
	}

	public List<IteratorRouteOfferOrderHandler> getIteratorRouteOfferOrderHandlers() {
		return routeOfferOrderHandlers;
	}

	public List<IteratorEndpointOrderHandler> getEndpointOrderHandlers() {
		return endpointOrderHandlers;
	}

	public String getInitialUrl() {
		return initialUrl;
	}

	public DefaultEndpointIteratorBuilder setServiceURI(String initialUrl) {
		this.initialUrl = initialUrl;
		return this;
	}

	public Properties getProps() {
		return props;
	}

	public DefaultEndpointIteratorBuilder setProps(Properties props) {
		this.props = props;
		return this;
	}

	public DME2EndpointURLFormatter getUrlFormatter() {
		return urlFormatter;
	}

	public DefaultEndpointIteratorBuilder setUrlFormatter(DME2EndpointURLFormatter urlFormatter) {
		this.urlFormatter = urlFormatter;
		return this;
	}

	public DME2Manager getManager() {
		return manager;
	}

	public DefaultEndpointIteratorBuilder setManager(DME2Manager manager) {
		this.manager = manager;
		return this;
	}

	public DME2BaseEndpointIterator build() throws DME2Exception {
		return buildIterator();
	}

	private DME2BaseEndpointIterator buildIterator() throws DME2Exception {
		LOGGER.debug(null, "getIterator", LogMessage.METHOD_ENTER);
		long startTime = System.currentTimeMillis();
		List<DME2RouteOffer> routeOffers = null;
		DME2Endpoint[] directEndpoints = null;
		ListMultimap<Integer, DME2RouteOffer> routeOffersGroupedBySequence = null;
		SortedMap<Integer, DME2Endpoint[]> unorderedEndpointByRouteOfferSeqMap = null;
		SortedMap<Integer, Map<Double, DME2Endpoint[]>> endpointsGroupedBySequence = new TreeMap<Integer, Map<Double, DME2Endpoint[]>>(); // Map
		// list that contains endpoints organized by their associated routeOffers sequence. Endpoints contained in the list
		// are also organized by their geographical distance.
		IteratorCreatingAttributes iteratorCreatingAttributes = new IteratorCreatingAttributes();

		LOGGER.debug(null, "getIterator", "initialized");

		processParamas();
		uniformResource = mapResource();
		
		if(uniformResource.getUrlType() ==  DmeUniformResource.DmeUrlType.SEARCHABLE) { 
			routeOffers = this.manager.getActiveOffers(uniformResource.getRouteInfoServiceSearchKey(), uniformResource.getVersion(), uniformResource.getEnvContext(), uniformResource.getPartner(), uniformResource.getDataContext(), uniformResource.getStickySelectorKey(), false, preferredRouteOfferForced); 
		} else if(uniformResource.getUrlType() ==  DmeUniformResource.DmeUrlType.RESOLVABLE || uniformResource.getUrlType() ==  DmeUniformResource.DmeUrlType.DIRECT || uniformResource.getUrlType() ==  DmeUniformResource.DmeUrlType.STANDARD) {
			if(directEndpoints == null && uniformResource.getUrlType() ==  DmeUniformResource.DmeUrlType.STANDARD && uniformResource.getPartner() != null) {
				routeOffers = this.manager.getActiveOffers(uniformResource, null);
			} else {
				directEndpoints = this.manager.getEndpoints(uniformResource);				
			}
		}
			
		
//		routeOffers = getActiveRouteOffers();

		if (routeOffers != null && routeOffers.size() > 0) {
			LOGGER.debug(null, "buildIterator", "Found {} route offers", routeOffers.size());
			// adding the default routeoffer ordering in the start before
			// exposing this routeoffer data set for client specific ordering,
			// this also arranges the route offers in groups relevant to their
			// sequences
			routeOfferOrderHandlers.add(0, EndpointIteratorFactory.getDefaultRouteOfferOrderHandler(routeOffers));

			// adding the preferred routeoffer ordering in the chain after the
			// default ordering
			/*if (getPreferredRouteOffer() != null) {
				LOGGER.debug(null, "getIterator", LogMessage.DEBUG_MESSAGE, "Preferred routeOffer found! Reorganizing endpoints based on the following preferred routeOffer: " + getPreferredRouteOffer());
				routeOfferOrderHandlers.add(1,
						EndpointIteratorFactory.getPreferredRouteOfferOrderHandler(getPreferredRouteOffer()));
			}*/

			for (IteratorRouteOfferOrderHandler routeOfferOrderHandler : routeOfferOrderHandlers) {
				if (routeOfferOrderHandler != null) {
					routeOffersGroupedBySequence = (ListMultimap<Integer, DME2RouteOffer>) routeOfferOrderHandler
							.order(routeOffersGroupedBySequence);
				}
			}

			unorderedEndpointByRouteOfferSeqMap = AvailableEndpoints.findByRouteOffer(config,
					routeOffersGroupedBySequence, manager, uniformResource);
		} else {
			LOGGER.debug(null, "buildIterator", "No route offers found, going direct path");
			directEndpoints = AvailableEndpoints.findUnorderedEndpoints(manager, uniformResource);
			if (directEndpoints != null && directEndpoints.length > 0) {
				LOGGER.debug(null, "buildIterator", "Found {} direct endpoints", directEndpoints.length);
				DME2RouteOffer routeOffer = new DME2RouteOffer(uniformResource.getService(),
						uniformResource.getVersion(), uniformResource.getEnvContext(), uniformResource.getRouteOffer(),
						null, this.manager);
				routeOffersGroupedBySequence = ArrayListMultimap.create();
				routeOffersGroupedBySequence.put(1, routeOffer);
				unorderedEndpointByRouteOfferSeqMap = new TreeMap<Integer, DME2Endpoint[]>();

				unorderedEndpointByRouteOfferSeqMap.put(1, directEndpoints);
			} else {
				LOGGER.debug(null, "buildIterator", "No direct endpoints found");
			}
		}

		if (unorderedEndpointByRouteOfferSeqMap != null && unorderedEndpointByRouteOfferSeqMap.size() > 0) {
			LOGGER.debug(null, "buildIterator", "Found {} endpoints", unorderedEndpointByRouteOfferSeqMap.size());
			for (Map.Entry<Integer, DME2Endpoint[]> entry : unorderedEndpointByRouteOfferSeqMap.entrySet()) {
				Integer sequence = entry.getKey();
				DME2Endpoint[] endpoints = entry.getValue();

				if (endpoints != null && endpoints.length > 0) {
					for (DME2Endpoint endpoint : endpoints) {
						if (endpoint != null) {
							LOGGER.debug(null, "buildIterator", "Sequence: {} Endpoint Service Name: {} ", sequence,
									endpoint.getServiceName());
						} else {
							LOGGER.debug(null, "buildIterator", "Sequence: {} Endpoint null", sequence);
						}
					}
				} else {
					LOGGER.debug(null, "buildIterator", "No endpoints found for sequence {}", sequence);
				}
			}
		} else {
			LOGGER.debug(null, "buildIterator", "No endpoints found");
		}

		// adding the default endpoint ordering in the front of the chain
		int idx = 0;
		endpointOrderHandlers.add(idx++, EndpointIteratorFactory
				.getDefaultEndpointOrderHandler(unorderedEndpointByRouteOfferSeqMap, getManager()));
		
		if (getPreferredRouteOffer() != null) {
			endpointOrderHandlers.add(idx++,
					EndpointIteratorFactory.getEndpointPreferredRouteOfferOrderHandler(getPreferredRouteOffer()));
		}
		
		if (getPreferredUrl() != null) {
			endpointOrderHandlers.add(idx++,
					EndpointIteratorFactory.getEndpointPreferredUrlOrderHandler(getPreferredUrl(), getUrlFormatter()));
		}

		for (IteratorEndpointOrderHandler endpointOrderHandler : endpointOrderHandlers) {
			if (endpointOrderHandler != null) {
				endpointsGroupedBySequence = (SortedMap<Integer, Map<Double, DME2Endpoint[]>>) endpointOrderHandler
						.order(endpointsGroupedBySequence);
			}
		}

		List<DME2EndpointReference> endpointHolders = AvailableEndpoints.createOrderedEndpointHolders(manager,
				routeOffersGroupedBySequence, endpointsGroupedBySequence);

		StringBuffer routeOfferNames = new StringBuffer();
		if (routeOffersGroupedBySequence != null) {
			for (DME2RouteOffer ro : routeOffersGroupedBySequence.values()) {
				if (ro.getRouteOffer() != null) {
					routeOfferNames.append(ro.getRouteOffer().getName()).append(":");
				}
			}
		}

		iteratorCreatingAttributes.setManager(manager);
		iteratorCreatingAttributes.setEndpointHolders(endpointHolders);
		iteratorCreatingAttributes
				.setQueryParamMinActiveEndPoint(uniformResource.getQueryParamsMap().get("minActiveEndPoints"));
		iteratorCreatingAttributes.setRequestEventProcessor(requestEventProcessor);
		iteratorCreatingAttributes.setReplyEventProcessor(replyEventProcessor);
		iteratorCreatingAttributes.setFaultEventProcessor(faultEventProcessor);
		iteratorCreatingAttributes.setTimeoutEventProcessor(timeoutEventProcessor);
		iteratorCreatingAttributes.setConfig(config);

		DME2BaseEndpointIterator endpointIterator = EndpointIteratorFactory
				.getDefaultIterator(iteratorCreatingAttributes);

		if (endpointHolders != null && endpointHolders.size() <= 0) {
			endpointIterator.setRouteOffersTried(routeOfferNames.toString());
		}

		LOGGER.debug(null, "buildIterator", "end; elapsed time:{}ms", System.currentTimeMillis() - startTime);
		LOGGER.debug(null, "buildIterator", LogMessage.METHOD_EXIT);
		return endpointIterator;
	}

	/**
	 * verifies and assigns default values as needed
	 */
	private void processParamas() throws DME2Exception {
		assignDefaults();
		verify();
	}

	private void verify() throws DME2Exception {
		if (config == null) {
			throw new IteratorException(IteratorErrorCatalogue.ITERATOR_004);
		}
		verifyUrl();
	}

  private void assignDefaults() throws DME2Exception {
    if ( manager == null ) {
      manager = new DME2Manager();
    }
    if ( getPreferredRouteOffer() == null ) {
			String defaultRouteOfferPreference = getDefaultRouteOfferPreference();
			setPreferredRouteOffer(
					defaultRouteOfferPreference != null && !defaultRouteOfferPreference.isEmpty() ? defaultRouteOfferPreference :
							null );
		}
    if ( getPreferredUrl() == null ) {
			String defaultPreferredUrl = getDefaultPreferredUrl();
      setPreferredUrl( defaultPreferredUrl != null && !defaultPreferredUrl.isEmpty()
          ? defaultPreferredUrl : null );
    }
    if ( getPreferredVersion() == null ) {
      setPreferredVersion( resolvePreferredVersion() );
    }
    if ( getPreferredRouteOfferForced() == null ) {
      setPreferredRouteOfferForced( resolvePreferredRouteOfferOverride() );
    }
  /*  if ( getPreferredVersion() != null ) {
      uniformResource.setPreferredVersion( preferredVersion );
    }*/
  }

	private void verifyUrl() throws DME2Exception {
		if (initialUrl == null) {
			ErrorContext ec = new ErrorContext();
			ec.add("searchString", initialUrl);
			ec.add("errorDetail", "Lookup URL String cannot be null.");

			LOGGER.error(null, "verifyUrl", "AFT-DME2-9000: {}", ec);
			throw new DME2Exception("AFT-DME2-9000", ec);
		}
	}

	private DmeUniformResource mapResource() throws DME2Exception {
		DmeUniformResource uniformResource = null;
		try {
			if (initialUrl.startsWith("dme2://")) {
				initialUrl = initialUrl.replace("dme2://", "http://");
			} else if (initialUrl.startsWith("ws://")) {
				initialUrl = initialUrl.replace("ws://", "http://");
			} else if (initialUrl.startsWith("wss://")) {
				initialUrl = initialUrl.replace("wss://", "http://");
			}
			uniformResource = new DmeUniformResource(config, initialUrl);
		} catch (MalformedURLException e) {
			throw new DME2Exception(DME2Constants.EXP_GEN_URI_EXCEPTION,
					new ErrorContext().add("extendedMessage", e.getMessage()).add("URL", initialUrl), e);
		}
		return uniformResource;
	}

	private List<DME2RouteOffer> getActiveRouteOffers() throws DME2Exception {
		LOGGER.debug(null, "getActiveRouteOffers", LogMessage.METHOD_ENTER);
		LOGGER.debug(null, "getActiveRouteOffers", "DMEUrlType is {}. Resolving RouteOffers.", uniformResource.getUrl());
		switch (uniformResource.getUrlType()) {
		case SEARCHABLE:
			LOGGER.debug(null, "getActiveRouteOffers", LogMessage.DEBUG_MESSAGE, String.format("DMEUrlType is %s. Resolving RouteOffers.", uniformResource.getUrlType())); 
			return manager.getActiveOffers(uniformResource.getRouteInfoServiceSearchKey(), uniformResource.getVersion(),
					uniformResource.getEnvContext(), uniformResource.getPartner(), uniformResource.getDataContext(),
					uniformResource.getStickySelectorKey(), false, preferredRouteOfferForced );
		case STANDARD:
			LOGGER.debug(null, "getActiveRouteOffers", LogMessage.DEBUG_MESSAGE, String.format("DMEUrlType is %s. Resolving Endpoints directly.", uniformResource.getUrlType()));
			if (uniformResource.getPartner() != null) {
				LOGGER.debug(null, "getActiveRouteOffers",
						"Used for getting active offers DMEUrlType option url type: {}. partner: {}",
						uniformResource.getUrlType(), uniformResource.getPartner());
				return manager.getActiveOffers(uniformResource, null);
			}
		default:
			LOGGER.debug(null, "getActiveRouteOffers", "Not serviced DMEUrlType option {}. ",
					uniformResource.getUrlType());
		}
		LOGGER.debug(null, "getActiveRouteOffers", LogMessage.METHOD_EXIT);
		return null;
	}

	@Override
	public EndpointIteratorBuilder addIteratorEndpointOrderHandler(
			IteratorEndpointOrderHandler iteratorEndpointOrderHandler) {
		endpointOrderHandlers.add(iteratorEndpointOrderHandler);
		return this;
	}

	@Override
	public EndpointIteratorBuilder addIteratorRouteOfferOrderHandler(
			IteratorRouteOfferOrderHandler iteratorRouteOfferOrderHandler) {
		routeOfferOrderHandlers.add(iteratorRouteOfferOrderHandler);
		return this;
	}

	@Override
	public EndpointIteratorBuilder setIteratorEndpointOrderHandlers(List<IteratorEndpointOrderHandler> iteratorEndpointOrderHandlers) {
		endpointOrderHandlers.addAll(iteratorEndpointOrderHandlers);
		return this;
	}
	
	@Override
	public EndpointIteratorBuilder setIteratorRouteOfferOrderHandlers(List<IteratorRouteOfferOrderHandler> iteratorRouteOfferOrderHandlers) {
		routeOfferOrderHandlers.addAll(iteratorRouteOfferOrderHandlers);
		return this;
	}
	@Override
	public EndpointIteratorBuilder setRequestEventProcessor(EventProcessor requestEventProcessor) {
		this.requestEventProcessor = requestEventProcessor;
		return this;
	}

	@Override
	public EndpointIteratorBuilder setReplyEventProcessor(EventProcessor replyEventProcessor) {
		this.replyEventProcessor = replyEventProcessor;
		return this;
	}

	@Override
	public EndpointIteratorBuilder setFaultEventProcessor(EventProcessor faultEventProcessor) {
		this.faultEventProcessor = faultEventProcessor;
		return this;
	}

	@Override
	public EndpointIteratorBuilder setTimeoutEventProcessor(EventProcessor timeoutEventProcessor) {
		this.timeoutEventProcessor = timeoutEventProcessor;
		return this;
	}

  private String resolvePreferredVersion() {
	    return System.getProperty(DME2Constants.AFT_DME2_PREFERRED_VERSION, config.getProperty(DME2Constants.AFT_DME2_PREFERRED_VERSION ));
	}

  private String resolvePreferredRouteOfferOverride() {
		String preferredRouteOfferForced = null;
		if(getPreferredRouteOffer() != null)
		{
			if (config.getBoolean( DME2Constants.Iterator.AFT_DME2_FLAG_FORCE_PREFERRED_ROUTE_OFFER)) {
				preferredRouteOfferForced = getPreferredRouteOffer();
			}
		}
		return preferredRouteOfferForced;
	}

  public String getPreferredVersion() {
    return preferredVersion;
  }

  public void setPreferredVersion( String preferredVersion ) {
    this.preferredVersion = preferredVersion;
  }

  public String getPreferredRouteOfferForced() {
    return preferredRouteOfferForced;
  }

  public void setPreferredRouteOfferForced( String preferredRouteOfferForced ) {
    this.preferredRouteOfferForced = preferredRouteOfferForced;
  }
}