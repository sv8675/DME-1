/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.jaas.callback.RequestParameterCallback;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.websocket.core.api.WebSocketListener;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.att.aft.dme2.api.util.DME2FilterHolder;
import com.att.aft.dme2.api.util.DME2FilterHolder.RequestDispatcherType;
import com.att.aft.dme2.api.util.DME2ServletHolder;
import com.att.aft.dme2.api.util.DME2ThrottleFilter;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.server.api.websocket.DME2ServerWebSocket;
import com.att.aft.dme2.server.api.websocket.DME2ServerWebSocketHandler;
import com.att.aft.dme2.server.api.websocket.GRMHealthCheckServerWebSocket;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2ExceptionHandler;
import com.att.aft.dme2.util.ErrorContext;

/**
 * The DME2ServiceHolder represents a SERVICE in the DME2-sense and the related artifacts required to start and publish that
 * service.
 */
@SuppressWarnings("deprecation")
public class DME2ServiceHolder
{
	private String context;
	private String serviceURI;
	private String realm;
	private String loginMethod;
	private String[] allowedRoles;
	
	private Properties contextParams;
	
	private DME2Server server;
	private DME2Manager manager;
	private Servlet servlet;
	
	private final boolean active = false;
	private DME2Configuration config;

	private boolean metricsFilterDisabled; // = config.getBoolean(DME2Constants.AFT_DME2_DISABLE_METRICS_FILTER);
	
	private List<String> serviceAliases;
	private List<DME2FilterHolder> filters;
	private List<DME2ServletHolder> servletHolders;
	private List<ServletContextListener> contextListeners;
	private GzipHandler gzipHandler=null;
	
	private static Object serverLock = new Object();

	private static final Logger logger = LoggerFactory.getLogger( DME2ServiceHolder.class );

	private static final String SECURITY_REALM="securityRealm";
	private static final String SERVLET="servlet";
	private static final String SERVICE= "service";
	private static final String TEST ="test";
	private DME2ServerWebSocketHandler dme2WebSocketHandler;
	
	private Float throttlePctPerPartner;
	private Boolean throttleFilterDisabled;
	
	private Properties serviceProperties;

	public Properties getServiceProperties() {
		return serviceProperties;
	}
  
	public void setServiceProperties(Properties props) {
		this.serviceProperties= props;
	}
	  
	public DME2ServerWebSocketHandler getDme2WebSocketHandler() {
		return dme2WebSocketHandler;
	}


	public void setDme2WebSocketHandler( DME2ServerWebSocketHandler dme2WebSocketHandler ) {
		this.dme2WebSocketHandler = dme2WebSocketHandler;
	}


	public DME2ServiceHolder()
	{
		
	}


	public void setManager(DME2Manager manager)
	{
		this.manager = manager;
		this.config = manager.getConfig();
		this.serviceProperties = manager.getServiceProperties();
		metricsFilterDisabled = config.getBoolean(DME2Constants.AFT_DME2_DISABLE_METRICS_FILTER);
	}


	public DME2Manager getManager()
	{
		return this.manager;
	}


	public void setServlet(Servlet servlet)
	{
		this.servlet = servlet;
	}


	public Servlet getServlet()
	{
		return servlet;
	}


	public void setServiceURI(String serviceURI)
	{
		this.serviceURI = serviceURI;
	}


	public void setServiceAliases(List<String> serviceAliases)
	{
		this.serviceAliases = serviceAliases;
	}


	public List<String> getServiceAliases()
	{
		return this.serviceAliases;
	}


	public String getServiceURI()
	{
		return this.serviceURI;
	}


	public void setContext(String context)
	{
		this.context = context;
	}


	public String getContext()
	{
		return context;
	}


	public void setSecurityRealm(String realm)
	{
		this.realm = realm;
	}


	public String getSecurityRealm()
	{
		return realm;
	}


	public void setAllowedRoles(String[] newAllowedRoles)
	{		
		if(newAllowedRoles == null) { 
			this.allowedRoles = null; 
		} else { 
			this.allowedRoles = Arrays.copyOf(newAllowedRoles, newAllowedRoles.length); 
		}
	}


	public String[] getAllowedRoles()
	{
		return this.allowedRoles;
	}


	public void setLoginMethod(String loginMethod)
	{
		this.loginMethod = loginMethod;
	}


	public String getLoginMethod()
	{
		return loginMethod;
	}


	public void setFilters(List<DME2FilterHolder> filterBeans)
	{
		this.filters = filterBeans;
	}


	public List<DME2FilterHolder> getFilters()
	{
		return this.filters;
	}


	public void setServletHolders(List<DME2ServletHolder> servlets)
	{
		this.servletHolders = servlets;
	}


	public List<DME2ServletHolder> getServletHolders()
	{
		return this.servletHolders;
	}


	public void setContextListeners(List<ServletContextListener> contextListeners)
	{
		this.contextListeners = contextListeners;
	}


	public List<ServletContextListener> getContextListeners()
	{
		return this.contextListeners;
	}


	public boolean isActive()
	{
		return active;
	}
	
	public DME2Server getServer()
	{
		return server;
	}


	public void setServer(DME2Server server)
	{
		this.server = server;
	}


	public boolean isMetricsFilterEnabled()
	{
		return !metricsFilterDisabled;
	}


	public void enableMetricsFilter()
	{
		this.metricsFilterDisabled = false;
	}


	public void disableMetricsFilter()
	{
		this.metricsFilterDisabled = true;
	}


	public Properties getContextParams()
	{
		return contextParams;
	}


	public void setContextParams(Properties contextParams)
	{
		this.contextParams = contextParams;
	}
	
	public Float getThrottlePctPerPartner() {
		return throttlePctPerPartner;
	}

	public void setThrottlePctPerPartner(Float throttlePctPerPartner) {
		this.throttlePctPerPartner = throttlePctPerPartner;
	}

	public Boolean getThrottleFilterDisabled() {
		return throttleFilterDisabled;
	}

	public void setThrottleFilterDisabled(Boolean throttleFilterDisabled) {
		this.throttleFilterDisabled = throttleFilterDisabled;
	}

	private WebSocketHandler createWebSocketHandler(final DME2ServerWebSocketHandler webSocketHandler, final DME2Manager dme2Manager) throws DME2Exception{
		WebSocketHandler websocketHandler =  new WebSocketHandler() {
			
			public WebSocketListener doWebSocketConnect(HttpServletRequest request,
					String protocol) {
				
				WebSocketListener  webSocket=null;
				 String trackingId=request.getParameter("dme2_tracking_id");
				 if (trackingId==null){
					 trackingId=getTrackingId();
				 }
				try {
					Cookie[] cookies = request.getCookies();
					boolean isHealthCheck=false;
					for (Cookie cookie : cookies) {
						if("healthcheck".equals(cookie.getName())){
							if ("healthcheck".equals(cookie.getValue())){
								isHealthCheck=true;	
							}else {
								ErrorContext ec = new ErrorContext();
								ec.add("Code", "DME2Server.Fault");
								 throw new DME2Exception("AFT-DME2-6712",ec);
							}
							break;
						}
					}
					
					 if (isHealthCheck){
						 webSocket=new  GRMHealthCheckServerWebSocket();
					 }else{
						 webSocket = new DME2ServerWebSocket(webSocketHandler,dme2Manager,trackingId);
					 }
				}catch (Exception e) {
					ErrorContext ec = new ErrorContext();
					ec.add("Code", "DME2Server.Fault");
					ec.add("extendedMessage", e.getMessage());
					ec.add("StackTrace", ExceptionUtils.getStackTrace(e));
					logger.info( null, "createWebSocketHandler", LogMessage.WS_SERVER_WEBSOCKET_HEALTHCHECK_EXCEPTION, ec);
				}
				return webSocket;
			}

			@Override
			public void configure(WebSocketServletFactory factory) {
				// TODO Auto-generated method stub
				
			}
		};
		return websocketHandler;
	}
	
	private static String getTrackingId() {
  		
  		return	  "WS_SERVER_ID_" +  UUID.randomUUID().toString() ;
  }
	
	@SuppressWarnings("unchecked")
	public void start() throws DME2Exception
	{
		try
		{
			/*Check if server is started*/
			if (server == null)
			{
				throw new DME2Exception("AFT-DME2-0013", new ErrorContext().add(SERVICE, serviceURI)
						.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm));
			}

			if (!server.isRunning())
			{
				throw new DME2Exception("AFT-DME2-0014", new ErrorContext().add(SERVICE, serviceURI)
						.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm));
			}
			
			
			if (this.getDme2WebSocketHandler()!=null){
				WebSocketHandler websocketHandler = createWebSocketHandler(this.getDme2WebSocketHandler(),this.getManager());
				ContextHandler context = new ContextHandler();
				String contextPath=getContext();
				if(contextPath==null){
					context.setContextPath("/");
					//contextPath="/";
				}else {
			
					context.setContextPath(contextPath);
				}
				context.setHandler(websocketHandler);
			
				synchronized (this.serverLock)
				{
					server.addHandler(context);
					
					try
					{
						context.start();
					}
					catch (Exception e)
					{

						throw new DME2Exception("AFT-DME2-6714", new ErrorContext().add(SERVICE, serviceURI).add("WEBSOCKET HANDLER", "" + websocketHandler));
					}

				}
				
				
				// register endpoint
				manager.publish(this);
				return;
			}
			
			

			ServletContextHandler con = new ServletContextHandler();
			con.setMaxFormContentSize(server.getServerProperties().getMaxRequestPostSize());

			/*Check security configuration*/
			if (realm != null){
				checkForValidJAASConfiguration(con);
			}
			
			/* Setting context path */
			if (this.getContext() != null){
				con.setContextPath(getContext());
			}
			else {
				con.setContextPath("/");
			}

			/* SCLD-2582 - Implementing ability to perform GZIP compression for DME2 calls based on payload size */
			/*
			 * This is done per service. If "disableCompression" query param is present and is set to false in the service URI,
			 * compression will remain disabled
			 */
			logger.debug( null, "start", "config.getBoolean(DME2Constants.DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH_KEY) : {}",
            config.getBoolean(DME2Constants.DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH_KEY));
			if (config.getBoolean(DME2Constants.DME2_ENABLE_PAYLOAD_COMPRESSION_THRESH_KEY))
			{
				if (!disableCompressionForServiceOverride())
				{
					String serviceString = getServiceString();
					if (!serviceString.startsWith("/")){
						serviceString = "/" + serviceString;
					}
//					DME2FilterHolder holder = createGZIPFilter();
					gzipHandler = createGZIPHandler(con, serviceString);
					con.setGzipHandler(gzipHandler);
//					con.addFilter(holder.getFilterHolder(), holder.getFilterPattern(), holder.getDispatcherType());
					logger.info( null, "start", LogMessage.DEBUG_MESSAGE, "GZIP filter enabled for service: {}", getServiceString());
				}
			}

			/*If Metrics Filter property is NOT disabled, add Metrics Filter to the Servlet Context Handler*/	
			if (!this.metricsFilterDisabled)
			{
				logger.debug( null, "start", LogMessage.ADD_METRICS_FILTER, serviceURI);
				createMetricsFilter(con);
			}
			/* Throttle per service per partner */
			addThrottleFilter(con);
			
			// Add context parameters
			Properties conParams = this.getContextParams();
			if (conParams != null)
			{
				Enumeration<Object> en = conParams.keys();
				while (en.hasMoreElements())
				{
					String key = (String) en.nextElement();
					String value = conParams.getProperty(key);
					con.setInitParameter(key, value);
				}
			}
			
			if (this.servletHolders != null && this.servletHolders.size() > 0)
			{
				@SuppressWarnings("rawtypes")
				ArrayList servletMappings = new ArrayList();
				Iterator<DME2ServletHolder> it = this.servletHolders.iterator();
				while (it.hasNext())
				{
					DME2ServletHolder dme2holder = it.next();

					String servletContextToRegister = dme2holder.getContextPath();
					String servletMapping;
					if (servletContextToRegister == null)
					{
						servletMapping = ("/" + serviceURI).replaceAll("//", "/");
						con.addServlet(dme2holder.getServletHolder(), servletMapping);
					}
					else
					{
						servletMapping = ("/" + servletContextToRegister).replaceAll("//", "/");
						con.addServlet(dme2holder.getServletHolder(), servletMapping);
					}
					
					servletMappings.add(servletMapping);
					
					if (dme2holder.getURLMapping() != null)
					{
						String urlMapping[] = dme2holder.getURLMapping();
						for (int i = 0; i < urlMapping.length; i++)
						{
							//dme2holder.getServletHolder().getRegistration().addMapping(urlMapping[i]);
							if(!servletMappings.contains(urlMapping[i])) {
								con.addServlet(dme2holder.getServletHolder(), urlMapping[i]);
							}
						}
					}
				}
			}
			else
			{
				if (this.context == null)
				{

					if (serviceURI.contains("?"))
					{
						String[] tokens = serviceURI.split("\\?");

						con.addServlet(new ServletHolder(servlet), ("/" + tokens[0]).replaceAll("//", "/"));
					}
					else
					{
						con.addServlet(new ServletHolder(servlet), ("/" + serviceURI).replaceAll("//", "/"));
					}
				}
				else
				{
					con.addServlet(new ServletHolder(servlet), ("/" + this.context).replaceAll("//", "/"));
				}
			}
			if (this.contextListeners != null)
			{
				for (ServletContextListener scl : this.contextListeners)
				{
					con.addEventListener(scl);
				}
			}


			if (this.filters != null)
			{
				for (DME2FilterHolder fh : this.filters)
				{
					con.addFilter(fh.getFilterHolder(), fh.getFilterPattern(), fh.getDispatcherType());
				}
			}

			// Adding the below synchronized block to addHandler and context.start
			// This is being done since we found only one servlet was registered under Jetty
			// container, if 4 different servlets were deployed at exact same time.
			// This issue was discovered as we configured foreign jms queues in WLS, which
			// initialized all the config's at same time, but in runtime only one of the servlet
			// context was resolvable, while rest of them threw a 404
			//
			synchronized (this.serverLock)
			{
				server.addHandler(con);
				con.getServletHandler().setEnsureDefaultServlet( false );
				try
				{
					con.start();
				}
				catch (Exception e)
				{

					throw new DME2Exception("AFT-DME2-0008", new ErrorContext().add(SERVICE, serviceURI).add(SERVLET, "" + servlet));
				}
			}

			// register endpoint
			manager.publish(this);
			
		}
		catch (Exception e)
		{
			throw DME2ExceptionHandler.handleException( e, serviceURI );
		}
	}

	
	
	public void stop() throws DME2Exception
	{
		server.unbindServiceListener(this.getServiceURI());
		manager.unpublish(this);
	}

	
	private void createMetricsFilter(ServletContextHandler context)
	{
		ArrayList<DispatcherType> dlist = new ArrayList<DispatcherType>();
		dlist.add(DispatcherType.REQUEST);
		dlist.add(DispatcherType.FORWARD);
		dlist.add(DispatcherType.ASYNC);
		
		//TODO use DME2MetricsFilter
		//context.addFilter(new FilterHolder(new DME2MetricsFilter(serviceURI)), serviceURI, EnumSet.copyOf(dlist));
	}

	private void addThrottleFilter(ServletContextHandler context){
		ArrayList<RequestDispatcherType> dlist1 = new ArrayList<RequestDispatcherType>();
  	dlist1.add( RequestDispatcherType.REQUEST);
		dlist1.add( RequestDispatcherType.FORWARD);
		dlist1.add( RequestDispatcherType.ASYNC);

		String serviceString = getServiceString();
		if (!serviceString.startsWith("/")){
			serviceString = "/" + serviceString;
		}
		DME2FilterHolder filterHolder = new DME2FilterHolder(new DME2ThrottleFilter(this), serviceString, EnumSet.copyOf(dlist1));
		context.addFilter(filterHolder.getFilterHolder(), filterHolder.getFilterPattern(), filterHolder.getDispatcherType());
	}

	public int getMaxPoolSize(){
		logger.debug( null, "getMaxPoolSize", LogMessage.DEBUG_MESSAGE, "DME2ThrottleFilter servlet max pool size called");
		return server.getServerProperties().getMaxPoolSize();
	}

	private GzipHandler createGZIPHandler(ServletContextHandler context, String patterns)
	{
		ArrayList<RequestDispatcherType> dispTypeList = new ArrayList<RequestDispatcherType>();
		dispTypeList.add( RequestDispatcherType.REQUEST);
		dispTypeList.add( RequestDispatcherType.FORWARD);
		dispTypeList.add( RequestDispatcherType.ASYNC);

		String mimeTypes = config.getProperty(DME2Constants.DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES_KEY);

		String[] mimes;
    if ( mimeTypes != null ) {
      mimes = mimeTypes.split( "," );
    } else {
      mimes = new String[0];
    }
		int minGzipSize = config.getInt(DME2Constants.DME2_PAYLOAD_COMPRESSION_THRESH_SIZE_KEY);

		Properties params = new Properties();
		params.put("mimeTypes", mimeTypes);
		params.put("minGzipSize", String.valueOf(minGzipSize));
		params.put("methods", "GET,POST,PUT");
		GzipHandler gzipHandler = new GzipHandler();
		gzipHandler.setIncludedMimeTypes(mimes);
		gzipHandler.addIncludedMethods("GET", "POST", "PUT");
		gzipHandler.setMinGzipSize(minGzipSize);
//		gzipHandler.setExcludedAgentPatterns("777");
		gzipHandler.setIncludedPaths(patterns);
		//gzipHandler.setHandler(context);
		return gzipHandler;
		//context.getServer().setHandler(gzipHandler);
		//gzipHandler.add
		//DME2FilterHolder filterHolder = new DME2FilterHolder(gzipHandler, getServiceString(),
		//		EnumSet.copyOf(dispTypeList), params);
		//filterHolder.setInitParams(params);

	}
	
	
	private DME2FilterHolder createGZIPFilter()
	{
		ArrayList<RequestDispatcherType> dispTypeList = new ArrayList<RequestDispatcherType>();
		dispTypeList.add( RequestDispatcherType.REQUEST);
		dispTypeList.add( RequestDispatcherType.FORWARD);
		dispTypeList.add( RequestDispatcherType.ASYNC);

		String mimeTypes = config.getProperty(DME2Constants.DME2_COMPRESSION_ACCEPTABLE_MIME_TYPES_KEY);
		int minGzipSize = config.getInt(DME2Constants.DME2_PAYLOAD_COMPRESSION_THRESH_SIZE_KEY);

		Properties params = new Properties();
		params.put("mimeTypes", mimeTypes);
		params.put("minGzipSize", String.valueOf(minGzipSize));
		params.put("methods", "GET,POST,PUT");

		DME2FilterHolder filterHolder = new DME2FilterHolder(new GzipFilter(), getServiceString(),
				EnumSet.copyOf(dispTypeList));
		filterHolder.setInitParams(params);

		return filterHolder;
	}


	private boolean disableCompressionForServiceOverride()
	{
		String serviceStr = serviceURI;
		try
		{
			URI uri = new URI(serviceStr);
			String queryParams = uri.getQuery();

			if (queryParams != null)
			{
				Map<String, String> paramsMap = new HashMap<String, String>();

				if (queryParams.contains("&"))
				{
					String[] tokens = queryParams.split("&");
					for (String tok : tokens)
					{
						String[] keyValuePair = tok.split("=");
						if (keyValuePair.length == 2)
						{
							paramsMap.put(keyValuePair[0], keyValuePair[1]);
						}
					}
				}
				else
				{
					String[] keyValuePair = queryParams.split("=");
					if (keyValuePair.length == 2)
					{
						paramsMap.put(keyValuePair[0], keyValuePair[1]);
					}
				}

				if (paramsMap.containsKey("disableCompression"))
				{
					String val = paramsMap.get("disableCompression");
					if (val.equalsIgnoreCase("true"))
					{
						return true;
					}
				}
			}
		}
		catch (URISyntaxException e)
		{
			/* If exception occurs, log the error and return true so that dynamic compression will be disabled */
			logger.error( null, "disableCompressionForServiceOverride", LogMessage.REPORT_ERROR, "An error occurred while processing the query strings for URI: {}", serviceStr);
			return true;
		}

		return false;
	}


	private String getServiceString()
	{
		String str = serviceURI;
		String serviceStr = null;

		if (str.contains("?"))
		{
			String[] tokens = str.split("\\?");
			serviceStr = tokens[0];

			if (!serviceStr.startsWith("/"))
			{
				serviceStr = "/" + serviceStr;
			}

			return serviceStr;
		}

		return str;
	}
	
	
	private void checkForValidJAASConfiguration(ServletContextHandler context) throws DME2Exception
	{
		if (allowedRoles == null || allowedRoles.length == 0)
		{
			throw new DME2Exception("AFT-DME2-0012", new ErrorContext().add(SERVICE, serviceURI)
					.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm));
		}

		/* Test the provided realm is a valid JAAS configuration */
		try
		{
			LoginContext ctx = createLoginContext();
			ctx.login();
		}
		catch (LoginException e)
		{
			throw new DME2Exception("AFT-DME2-0015", new ErrorContext().add(SERVICE, serviceURI)
					.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm), e);
		}

		JAASLoginService loginService = new JAASLoginService(realm);
		
		try
		{
			loginService.start();
		}
		catch (Exception e)
		{
			throw new DME2Exception("AFT-DME2-0008", new ErrorContext().add(SERVICE, serviceURI)
					.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm), e);
		}

		/* Test if the configuration is right... */
		try
		{
			loginService.login(TEST, TEST, null);
		}
		catch (SecurityException e)
		{
			if (e.getCause() != null && e.getCause() instanceof IOException)
			{
				throw new DME2Exception("AFT-DME2-0011", new ErrorContext().add(SERVICE, serviceURI)
						.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm), e);
			}
		}

		if (loginMethod == null)
		{
			throw new DME2Exception("AFT-DME2-0010", new ErrorContext().add(SERVICE, serviceURI)
					.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm));
		}

		if (!(loginMethod.equals("BASIC") || loginMethod.equals("CLIENT-CERT")))
		{
			throw new DME2Exception("AFT-DME2-0010", new ErrorContext().add(SERVICE, serviceURI)
					.add(SERVLET, "" + servlet).add(SECURITY_REALM, realm));
		}

		Constraint constraint = new Constraint();
		
		if (loginMethod.equals("BASIC"))
		{
			constraint.setName(Constraint.__BASIC_AUTH);
		}
		else if (loginMethod.equals("CLIENT-CERT"))
		{
			constraint.setName(Constraint.__CERT_AUTH);
		}
		
		constraint.setRoles(allowedRoles);
		constraint.setAuthenticate(true);

		ConstraintMapping cm = new ConstraintMapping();
		cm.setConstraint(constraint);
		cm.setPathSpec("/*");

		ConstraintSecurityHandler sechandler = new ConstraintSecurityHandler();
		sechandler.setRealmName(realm);
		sechandler.setLoginService(loginService);
		sechandler.addConstraintMapping(cm);

		context.setSecurityHandler(sechandler);
	}
	
	
	private LoginContext createLoginContext() throws LoginException
	{
		LoginContext ctx = new LoginContext(realm, new CallbackHandler()
		{
			@Override
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
			{
				for (int i = 0; i < callbacks.length; i++)
				{
					if (callbacks[i] instanceof TextOutputCallback)
					{
						continue;	
					}
					else if (callbacks[i] instanceof NameCallback)
					{
						NameCallback nc = (NameCallback) callbacks[i];
						nc.setName(TEST);
					}
					else if (callbacks[i] instanceof PasswordCallback)
					{
						PasswordCallback pc = (PasswordCallback) callbacks[i];
						pc.setPassword(TEST.toCharArray());
					}
					else if (callbacks[i] instanceof ObjectCallback)
					{
						ObjectCallback oc = (ObjectCallback) callbacks[i];
						oc.setObject(TEST);
					}
					else if (callbacks[i] instanceof RequestParameterCallback)
					{
						RequestParameterCallback rpc = (RequestParameterCallback) callbacks[i];
						rpc.setParameterName("key");
						rpc.setParameterValues(new ArrayList<String>());
					}
				}
			}

		});
		
		return ctx;
	}
}
