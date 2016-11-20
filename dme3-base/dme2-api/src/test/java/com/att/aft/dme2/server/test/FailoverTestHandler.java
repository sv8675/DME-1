/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.att.aft.dme2.api.util.DME2ExchangeFaultContext;
import com.att.aft.dme2.handler.DefaultLoggingFailoverFaultHandler;


public class FailoverTestHandler extends DefaultLoggingFailoverFaultHandler{
	private static final Map<String,List<DME2ExchangeFaultContext>> contexts = new HashMap<String,List<DME2ExchangeFaultContext>>();
	
	
	@Override
	public void handleEndpointFailover(DME2ExchangeFaultContext context) {
		addOrUpdate(context);
		super.handleEndpointFailover(context);
		//System.err.println(LogMessage.SEP_FAILOVER.toString(context.getService(),context.getRequestURL(),context.getRouteOffer(),context.getResponseCode(),context.getException()));
	}
	
	public static Map<String,List<DME2ExchangeFaultContext>> getFaultContexts(){
		return contexts;
	}
	
	@Override
	public void handleRouteOfferFailover(DME2ExchangeFaultContext context) {
		super.handleRouteOfferFailover(context);
		addOrUpdate(context);
		String template = "Failing over route offer %s for service %s";
		//System.err.println(LogMessage.DEBUG_MESSAGE.toString(String.format(template,context.getRouteOffer(),context.getService())));
	}
	
	private void addOrUpdate(DME2ExchangeFaultContext context){
		List<DME2ExchangeFaultContext> ctx = contexts.get(context.getRequestURL());
		
		if(ctx != null){
			ctx.add(context);
		} else{
			ctx = new ArrayList<DME2ExchangeFaultContext>();
			ctx.add(context);
			contexts.put(context.getRequestURL(), ctx);
		}
	}
}
