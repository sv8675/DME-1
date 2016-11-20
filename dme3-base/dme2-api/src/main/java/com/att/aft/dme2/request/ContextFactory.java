package com.att.aft.dme2.request;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.DefaultRequestProcessor;
import com.att.aft.dme2.api.RequestFacade;
import com.att.aft.dme2.api.http.HttpRequestInvoker;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.util.DME2Constants;

public class ContextFactory {
	private static ContextFactory instance = null;

	private DME2Configuration config = new DME2Configuration();
	public RequestContext createContext(DME2Manager mgr, Request request) throws DME2Exception {
		if(mgr!=null){
			this.config = mgr.getConfig();
		}
		/*
		 * String requestType = "HTTP";
		 * 
		 * // determine the request type if (request instanceof HttpRequest) {
		 * requestType = "HTTP"; }
		 */
		initRequestHeaders(request);

		// create logging context
		LoggingContext logContext = createLoggingContext(request);
		
		// create an iteratorfactory,
		// create iterator, register endpoint handlers from request object
		// at this point the endpoints have not been obtained for the specified
		// uri yet
		
		RequestContext context = new RequestContext();
		context.setLogContext(logContext);
		context.setMgr(mgr);
		context.setRequest(request);
		context.setUniformResource(request.getUniformResource());
		return context;
	}


	private void initRequestHeaders(Request request) {
		// make sure there is a MessageID
		if (request.getClientHeaders() == null || request.getClientHeaders().get(DME2Constants.JMSMESSAGEID) == null) {
			request.getClientHeaders().put(DME2Constants.JMSMESSAGEID, "ID:" + UUID.randomUUID().toString());

			if (request.getUniformResource().getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER_CLASS,
						request.getUniformResource().getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER, request.getUniformResource().getPartner());
			} else if (request.getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER_CLASS, request.getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER, request.getPartner());
			}
		} else {
			if (request.getUniformResource().getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER_CLASS,
						request.getUniformResource().getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_REQUEST_PARTNER, request.getUniformResource().getPartner());
			} else if (request.getPartner() != null) {
				request.getClientHeaders().put(DME2Constants.DME2_JMS_REQUEST_PARTNER_CLASS, request.getPartner());
				request.getClientHeaders().put(DME2Constants.DME2_JMS_REQUEST_PARTNER, request.getPartner());
			}
		}
	}
	
	private String getIpAddr(){
		String ip = null;
		try{
			ip = InetAddress.getLocalHost().getHostAddress();
		}catch(Exception e){
		}
		return ip;
	}
	
	// no need for this once singleton object is injected
	public static ContextFactory getInstance() {
		if (instance == null) {
			synchronized (ContextFactory.class) {
				instance = new ContextFactory();
			}
		}
		return instance;
	}

	public RequestFacade createFacade(RequestContext context) {
		RequestFacade facade = null;

		// determine the protocol and create HttpRequestInvoker or
		// WSRequestInvoker
		if (context.getRequest() instanceof HttpRequest) {
			HttpRequestInvoker invoker = new HttpRequestInvoker(context);
			DefaultRequestProcessor defaultRequestProcessor = new DefaultRequestProcessor(invoker);
			defaultRequestProcessor.setPreferLocal(context.getUniformResource().isPreferLocalEPs());
			context.getRequest().setRequestProcessor(defaultRequestProcessor);
			facade = new RequestFacade(context, defaultRequestProcessor);
		}
		return facade;
	}

	private LoggingContext createLoggingContext(Request request) throws DME2Exception {
		LoggingContext context = null;

		try {
			context = new LoggingContext();

			String conversationId = null;
			String trackingId = null;
			boolean sendTraceInfo = false;

			// TODO : what happens if the IDs are not present in header?
			// Shouldn't we create a random number for
			// tracking and conversationId
			if (request instanceof HttpRequest) {
				Map<String, String> headers = ((HttpRequest) request).getRequestHeaders();
				
				String msgId = headers.get(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + DME2Constants.JMSMESSAGEID);
				if (msgId == null) {
					msgId = headers.get(DME2Constants.JMSMESSAGEID);
				}
				conversationId = headers.get(config.getProperty(DME2Constants.DME2_HEADER_PREFIX) + DME2Constants.JMSCONVERSATIONID);
				if (conversationId == null) {
					conversationId = headers.get(DME2Constants.JMSCONVERSATIONID);
				}
				trackingId = msgId + (conversationId == null ? "" : "(" + conversationId + ")")
						+ (request.getStickySelectorKey() == null ? ""
								: "(stickySelector=" + request.getStickySelectorKey() + ")");
				
				Map<String, String> requestHeaders = ((HttpRequest) request).getRequestHeaders();
				String reqTraceOn = requestHeaders.get("AFT_DME2_REQ_TRACE_ON");
				if(reqTraceOn != null){ sendTraceInfo = true; }				
			}
			/*
			 * String reqTraceOn = headers.get("AFT_DME2_REQ_TRACE_ON");
			 * if(reqTraceOn != null){ this.sendTraceInfo = true; }
			 * DME2Constants.setContext(trackingID, null);
			 */
			context.setTrackingId(trackingId);
			context.setConversationId(conversationId);
			context.setSendTraceInfo(sendTraceInfo);

		} catch (Exception e) {
			// LogUtil.getINSTANCE().report(logger, Level.WARNING,
			// LogMessage.EXCH_CTX_FAIL, e);
		}
		return context;
	}
}
