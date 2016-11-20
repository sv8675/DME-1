package com.att.aft.dme2.request;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.RequestProcessorIntf;
import com.att.aft.dme2.api.SimpleRealm;
import com.att.aft.dme2.handler.AsyncResponseHandlerIntf;
import com.att.aft.dme2.util.ErrorContext;

public abstract class Request {// extends org.eclipse.jetty.client.HttpRequest {

/*	protected Request() {
		super(null, null, null);
	}

	protected Request(HttpClient client, HttpConversation conversation, URI uri) {
		super(client, conversation, uri);
	}
*/
	protected Request(URI uri) {		
	}
	protected String context;
	protected String subContext;
	protected String queryParams;
	protected String lookupUri;
	protected String charset;
	private String stickySelectorKey;
	protected boolean preferLocalEPs;
	protected boolean ignoreFailoverOnExpire;
	protected boolean useVersionRange = true;
	protected String partner;
	protected String routeOffer;
	protected boolean isEncoded;
	protected SimpleRealm realm;
	protected String preferredRouteOffer;
	protected Properties configs;
	protected long readTimeout;
	protected boolean returnResponseAsBytes = false;
	protected AsyncResponseHandlerIntf responseHandler = null;
	protected RequestProcessorIntf requestProcessor;
	private DmeUniformResource uniformResource;
	protected Map<String, String> headers;
	protected boolean presentPreferLocalEPs = false;
	protected boolean presentUseVersionRange = false;
	
	public boolean isPresentUseVersionRange() {
		return presentUseVersionRange;
	}

	public boolean isPresentPreferLocalEPs() {
		return presentPreferLocalEPs;
	}

	public RequestProcessorIntf getRequestProcessor() {
		return requestProcessor;
	}

	public void setRequestProcessor(RequestProcessorIntf requestProcessor) {
		this.requestProcessor = requestProcessor;
	}

	public AsyncResponseHandlerIntf getResponseHandler() {
		return responseHandler;
	}

	public void setResponseHandler(AsyncResponseHandlerIntf responseHandler) {
		this.responseHandler = responseHandler;
	}

	protected abstract static class RequestBuilder<T extends Request, B extends RequestBuilder<T, B>> {
		protected T obj;
		protected B thisObj;

		public RequestBuilder(URI uri) {
			obj = createObj(uri);
			thisObj = getThis();
		}

		public B withContext(String context) {
			obj.context = context;
			return thisObj;
		}

		public B withSubContext(String subContext) {
			obj.subContext = subContext;
			return thisObj;
		}

		public B withQueryParams(String queryParams) {
			obj.queryParams = queryParams;
			return thisObj;
		}

		/*public B withPartner(String partner) {
			obj.partner = partner;
			return thisObj;
		}*/

		public B withUseVersionRange(boolean useVersionRange) {
			obj.useVersionRange = useVersionRange;
			obj.presentUseVersionRange = true;
			return thisObj;
		}

		public B withPreferLocalEPs(boolean preferLocalEPs) {
			obj.preferLocalEPs = preferLocalEPs;
			obj.presentPreferLocalEPs = true;
			return thisObj;
		}

		public B withAuthCreds(String realmName, String username, String password) {
			SimpleRealm realm = new SimpleRealm(realmName, username, password);
			obj.realm = realm;
			return thisObj;
		}

		public B withPreferredRouteOffer(String preferredRouteOffer) {
			obj.preferredRouteOffer = preferredRouteOffer;
			return thisObj;
		}

		public B withCharset(String charset) {
			obj.charset = charset;
			return thisObj;
		}

		public B withLookupURL(String lookupUri) throws DME2Exception {
			if (lookupUri == null) 
				throw new DME2Exception("AFT-DME2-0605", new ErrorContext().add("extendedMessage", "uri=null"));
			obj.lookupUri = lookupUri.trim();
			return thisObj;
		}

		public B withReadTimeout(long readTimeout) {
			obj.readTimeout = readTimeout;
			return thisObj;
		}

		public B withReturnResponseAsBytes(boolean returnResponseAsBytes) {
			obj.returnResponseAsBytes = returnResponseAsBytes;
			return thisObj;
		}

		public B withResponseHandlers(AsyncResponseHandlerIntf responseHandler) {
			obj.responseHandler = responseHandler;
			return thisObj;
		}

		public T build() {
			return obj;
		}


		protected abstract T createObj(URI uri);


		protected abstract B getThis();
	}

	// protected Request() {
	// }

	public String getContext() {
		return context;
	}

	public String getSubContext() {
		return subContext;
	}

	public String getQueryParams() {
		return queryParams;
	}

	public String getStickySelectorKey() {
		return stickySelectorKey;
	}

	public boolean isPreferLocalEPs() {
		return preferLocalEPs;
	}

	public boolean isIgnoreFailoverOnExpire() {
		return ignoreFailoverOnExpire;
	}

	public boolean isUseVersionRange() {
		return useVersionRange;
	}

	public String getPartner() {
		return partner;
	}

	public String getRouteOffer() {
		return routeOffer;
	}

	public boolean isEncoded() {
		return isEncoded;
	}

	public String getLookupUri() {
		return lookupUri;
	}
	
	public void setLookupUri(String lookupUri) {
		this.lookupUri = lookupUri;
	}

	public SimpleRealm getRealm() {
		return realm;
	}

	public String getPreferredRouteOffer() {
		return preferredRouteOffer;
	}

	public void setPreferredRouteOffer(String preferredRouteOffer) {
		this.preferredRouteOffer = preferredRouteOffer;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public long getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(long readTimeout) {
//		super.timeout(readTimeout, TimeUnit.MILLISECONDS);
		this.readTimeout = readTimeout;		
	}

	public boolean isReturnResponseAsBytes() {
		return returnResponseAsBytes;
	}

	public void setReturnResponseAsBytes(boolean returnResponseAsBytes) {
		this.returnResponseAsBytes = returnResponseAsBytes;
	}

	public DmeUniformResource getUniformResource() {
		return uniformResource;
	}

	public void setUniformResource(DmeUniformResource uniformResource) {
		this.uniformResource = uniformResource;
	}
	
	public Map<String, String> getClientHeaders() {
	    if (this.headers == null)
	    	this.headers =  new HashMap<String, String>();
	    return this.headers;	
	}

}
