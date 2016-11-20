package com.att.aft.dme2.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.att.aft.dme2.iterator.service.IteratorEndpointOrderHandler;
import com.att.aft.dme2.iterator.service.IteratorRouteOfferOrderHandler;

public class HttpRequest extends Request {
	
//	public HttpRequest(HttpClient client, HttpConversation conversation,
//			URI uri) {
	public HttpRequest(URI uri) {
		super(uri);
	}

//	DME2FaultHandlerIntf faultHandlers[];
	protected long exchangeRoundTripTimeOut;
	protected long connectTimeout;
	protected long perEndpointTimeoutMs;
	protected String httpMethod;
	boolean presentExchangeRoundTripTimeOut = false;
	private List<IteratorEndpointOrderHandler> endpointOrderHandlers = new ArrayList<IteratorEndpointOrderHandler>();
	private List<IteratorRouteOfferOrderHandler> routeOfferOrderHandlers = new ArrayList<IteratorRouteOfferOrderHandler>();

	
    public boolean isPresentExchangeRoundTripTimeOut() {
		return presentExchangeRoundTripTimeOut;
	}

	public static final class RequestBuilder extends Request.RequestBuilder<HttpRequest, RequestBuilder> {

 //       public RequestBuilder(HttpClient client, HttpConversation conversation, URI uri) {
		public RequestBuilder(URI uri) {
			super(uri);
		}

		public RequestBuilder withExchangeRoundTripTimeOut(long exchangeRoundTripTimeOut) {
            obj.exchangeRoundTripTimeOut = exchangeRoundTripTimeOut; 
            obj.presentExchangeRoundTripTimeOut = true;
            return thisObj;
        }
        
		public RequestBuilder withPerEndpointTimeoutMs(long perEndpointTimeoutMs) {
            obj.perEndpointTimeoutMs = perEndpointTimeoutMs; 
            return thisObj;
        }
		
        public RequestBuilder withHeaders(Map<String, String> inHeaders) {	
        	Map<String, String> headers = null;
			if (MapUtils.isNotEmpty(inHeaders)) {
				if (obj.headers == null) {
					headers = new HashMap<String, String>(inHeaders);
					obj.headers = headers;
				} else {
					obj.headers.putAll(inHeaders);
				}
			}
        	
        	return thisObj;        	
        }
        
        public RequestBuilder withHeader(String name, String value) {
        	Map<String, String> headers = null;
        	if (obj.headers == null) {
        		headers = new HashMap<String,String>();
        		headers.put(name,  value);
               	obj.headers = headers;                
        	} else {
        		obj.headers.put(name, value);
        	}
        	
        	return thisObj; 
        }
        
        /*public RequestBuilder withResponseHandlers(AsyncResponseHandlerIntf responseHandler) {
        	obj.responseHandler = responseHandler;
        	return thisObj;
        }*/
        
        public RequestBuilder withHttpMethod(String method) {
        	obj.httpMethod = method;
        	return thisObj;
    	}
        
		@Override
		protected HttpRequest createObj(URI uri) {
			return new HttpRequest(uri);
		}

		@Override
		protected RequestBuilder getThis() {			
			return this;
		}
		 
		public RequestBuilder withIteratorRouteOfferOrderHandler(IteratorRouteOfferOrderHandler handler) {
			if (obj.routeOfferOrderHandlers == null) 
				obj.routeOfferOrderHandlers = new ArrayList<IteratorRouteOfferOrderHandler> ();
			obj.routeOfferOrderHandlers.add(handler);
			return thisObj;
		}
		
		public RequestBuilder withIteratorEndpointOrderHandler(IteratorEndpointOrderHandler handler) {
			if (obj.endpointOrderHandlers == null) 
				obj.endpointOrderHandlers = new ArrayList<IteratorEndpointOrderHandler> ();
			obj.endpointOrderHandlers.add(handler);
			return thisObj;
		}
		
		public RequestBuilder withIteratorRouteOfferOrderHandlers(List<IteratorRouteOfferOrderHandler> handlers) {
			if (obj.routeOfferOrderHandlers == null) 
				obj.routeOfferOrderHandlers = new ArrayList<IteratorRouteOfferOrderHandler> ();
			obj.routeOfferOrderHandlers.addAll(handlers);
			return thisObj;
		}
		
		public RequestBuilder withIteratorEndpointOrderHandlers(List<IteratorEndpointOrderHandler> handlers) {
			if (obj.endpointOrderHandlers == null) 
				obj.endpointOrderHandlers = new ArrayList<IteratorEndpointOrderHandler> ();
			obj.endpointOrderHandlers.addAll(handlers);
			return thisObj;
		}
    }
    
    //protected HttpRequest() {    	
    //}
     
    public long getExchangeRoundTripTimeOut() { 
    	return exchangeRoundTripTimeOut; 
    }

	public  Map<String, String> getRequestHeaders() {
		if (headers == null)
			return new HashMap<String, String>();
		return headers;
	}
    
    public long getConnectTimeout() {
		return connectTimeout;
	}

	public long getPerEndpointTimeoutMs() {
		return perEndpointTimeoutMs;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}
 
	public List<IteratorRouteOfferOrderHandler> getIteratorRouteOfferOrderHandlers() {
		return routeOfferOrderHandlers;
	}

	public List<IteratorEndpointOrderHandler> getEndpointOrderHandlers() {
		return endpointOrderHandlers;
	}
	
	public void setIteratorEndpointOrderHandler(
			IteratorEndpointOrderHandler iteratorEndpointOrderHandler) {
		endpointOrderHandlers.add(iteratorEndpointOrderHandler);
	}

	public void setIteratorRouteOfferOrderHandler(
			IteratorRouteOfferOrderHandler iteratorRouteOfferOrderHandler) {
		routeOfferOrderHandlers.add(iteratorRouteOfferOrderHandler);
	}
}
