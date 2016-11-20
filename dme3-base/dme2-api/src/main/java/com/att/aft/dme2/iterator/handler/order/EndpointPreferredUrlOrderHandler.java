package com.att.aft.dme2.iterator.handler.order;

import java.util.Map;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.iterator.helper.EndpointPreference;
import com.att.aft.dme2.iterator.service.DME2EndpointURLFormatter;
import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;

public class EndpointPreferredUrlOrderHandler implements IteratorEndpointOrderHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointPreferredUrlOrderHandler.class.getName());
	private String preferredURL = null; 
	private DME2EndpointURLFormatter urlFormatter=null;
	
	public EndpointPreferredUrlOrderHandler(final String preferredURL, final DME2EndpointURLFormatter urlFormatter) {
		this.urlFormatter = urlFormatter;
		this.preferredURL = preferredURL;
	}
	
	@Override
	public Map<Integer, Map<Double, DME2Endpoint[]>> order(Map<Integer, Map<Double, DME2Endpoint[]>> orderedEndpointByRouteOfferSeqMap) throws DME2Exception {
		LOGGER.info(null, "order", "start");
		
		long startTime = System.currentTimeMillis();
		LOGGER.info(null, "order", "get endpoint map: {}", orderedEndpointByRouteOfferSeqMap);
		LOGGER.info(null, "order", "resolvePreferredConnection:{}", preferredURL);

		if(orderedEndpointByRouteOfferSeqMap!=null){
			orderedEndpointByRouteOfferSeqMap=EndpointPreference.organizeEndpointsByPreferredURL(orderedEndpointByRouteOfferSeqMap, getPreferredUrl(), getUrlFormatter());
		}
		
		LOGGER.info(null, "orderRouteOffers", "end;elapsed time:{}",(System.currentTimeMillis()-startTime));
		return orderedEndpointByRouteOfferSeqMap;
	}
	
	protected String getPreferredUrl(){
		return this.preferredURL;
	}

	protected DME2EndpointURLFormatter getUrlFormatter(){
		return urlFormatter;
	}
}