/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Servlet;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.jaas.callback.RequestParameterCallback;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.att.aft.dme2.api.http.DME2QueuedThreadPool;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.request.DmeUniformResource;
import com.att.aft.dme2.server.jetty.DME2SslSelectChannelConnector;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.DME2ExceptionHandler;
import com.att.aft.dme2.util.ErrorContext;

public class DME2Server {
	
		private static final Logger logger = LoggerFactory.getLogger(DME2Server.class.getName());
	
		private static final String SECURITY_REALM="securityRealm";
		private static final String SERVLET="servlet";
		private static final String WEBSOCKET="websocket";
		private static final String SERVICE= "service";
		private static final String TEST ="test";
		private static final String KEY_KEYSTORE_PASSWORD = "AFT_DME2_KEYSTORE_PASSWORD";
		private static final String KEY_TRUSTSTORE_PASSWORD = "AFT_DME2_TRUSTSTORE_PASSWORD";
		private static final String KEY_PASSWORD = "AFT_DME2_KEY_PASSWORD";

		// Error Messages
		private static final String PORT_RANGE_ERROR_CODE = "AFT-DME2-0203";
		private static final String PORT_RANGE_ERROR_MSG = "PORT RANGE DEFINED IN INVALID FORMAT";
		private static final String STOP_ERROR_CODE = "AFT-DME2-0201";
		private static final String STOP_ERROR_MSG = "DME2 SERVER STOP FAILED";
		private DME2ServerProperties serverProperties;
	
		// Fields
		private Server server = null;
		private boolean running = false;
		//private SelectChannelConnector connector = null;
		private ServerConnector connector = null;
		private ContextHandlerCollection root = null;
		private HandlerCollection websocketHandlerCollection = null;
		private String baseAddress = null;
		private DME2Manager manager = null;
		private int persistedPort;
		private String persistedPorts;
		private boolean webSocket=false;
		
		private DME2Configuration config = null;
	  private DME2QueuedThreadPool btp;
		
		public DME2Server(DME2Configuration config) throws DME2Exception {
			this.config = config;
			serverProperties = new DME2ServerProperties(config);
		}
		
		public void setManager(DME2Manager manager) {
			this.manager = manager;
		}
		
		public DME2Manager getManager() {
			return this.manager;
		}
		
		//not supported
		public void setProperties(Properties props) throws DME2Exception {
			//assert(!running);
			//init(props);
		}
		
		public void bindServiceListener(String service, Servlet servlet) throws DME2Exception {
			bindServiceListener(service, servlet, null, null, null);
		}
		
		public void bindServiceListener(String service, Servlet servlet, String securityRealm, String[] allowedRoles, String loginMethod) throws DME2Exception {
			
			try {
				ServletContextHandler context = new ServletContextHandler();
				if (securityRealm != null) {
					if (allowedRoles == null || allowedRoles.length == 0) {
						throw new DME2Exception("AFT-DME2-0009", new ErrorContext().add(SERVICE, service).add(SERVLET, ""+servlet).add(SECURITY_REALM, securityRealm));
					}
					
					// quickly test the provided realm is a valid JAAS configuration
					try {
						LoginContext ctx = new LoginContext(securityRealm, new CallbackHandler() {
							@Override
							public void handle(Callback[] callbacks) throws IOException,
									UnsupportedCallbackException {
								   for (int i = 0; i < callbacks.length; i++) {
									      if (callbacks[i] instanceof TextOutputCallback) {
									    	  continue;	
								      } else if (callbacks[i] instanceof NameCallback) {
								          NameCallback nc = (NameCallback)callbacks[i];
								          nc.setName(TEST);
								      } else if (callbacks[i] instanceof PasswordCallback) {
								          PasswordCallback pc = (PasswordCallback)callbacks[i];
								          pc.setPassword(TEST.toCharArray());
								      } else if (callbacks[i] instanceof ObjectCallback) {
								    	  ObjectCallback oc = (ObjectCallback)callbacks[i];
								    	  oc.setObject(TEST);
								      } else if (callbacks[i] instanceof RequestParameterCallback) {
								    	  RequestParameterCallback rpc = (RequestParameterCallback)callbacks[i];
								    	  rpc.setParameterName("key");
								    	  rpc.setParameterValues(new ArrayList<String>());
								      } else {
								    	 logger.info( null, "bindServiceListener", LogMessage.SERVER_CALLBACK, callbacks[i]);
								      }
							   }
						}
						
					});
					ctx.login();
				} catch (LoginException e) {
					throw new DME2Exception("AFT-DME2-0012", new ErrorContext().add(SERVICE, service).add(SERVLET, ""+servlet).add(SECURITY_REALM, securityRealm), e);
				}
				
				JAASLoginService loginService = new JAASLoginService(securityRealm);
				try {
					loginService.start();
				} catch (Exception e) {
					throw new DME2Exception("AFT-DME2-0008", new ErrorContext().add(SERVICE, service).add(SERVLET, ""+servlet).add(SECURITY_REALM, securityRealm), e);
				}
				
				// quickly test if the configuration is right...
				try {
					loginService.login(TEST, TEST, null);
				} catch (SecurityException e) {
					if (e.getCause() != null && e.getCause() instanceof IOException) {
						throw new DME2Exception("AFT-DME2-0011", new ErrorContext().add(SERVICE, service).add(SERVLET, ""+servlet).add(SECURITY_REALM, securityRealm), e);
					} 
				} 
				
				if (loginMethod == null) {
					throw new DME2Exception("AFT-DME2-0009", new ErrorContext().add(SERVICE, service).add(SERVLET, ""+servlet).add(SECURITY_REALM, securityRealm));
				}
				
				if (!(loginMethod.equals("BASIC") || loginMethod.equals("CLIENT-CERT"))) {
					throw new DME2Exception("AFT-DME2-0010", new ErrorContext().add(SERVICE, service).add(SERVLET, ""+servlet).add(SECURITY_REALM, securityRealm));
				}
				
				Constraint constraint = new Constraint();
				if (loginMethod.equals("BASIC")) {
					constraint.setName(Constraint.__BASIC_AUTH);
				} else if (loginMethod.equals("CLIENT-CERT")) {
					constraint.setName(Constraint.__CERT_AUTH);
				}
				constraint.setRoles(allowedRoles);
				constraint.setAuthenticate(true);
				
				ConstraintMapping cm = new ConstraintMapping();
				cm.setConstraint(constraint);
				cm.setPathSpec("/*");
				
				ConstraintSecurityHandler sechandler = new ConstraintSecurityHandler();
				sechandler.setRealmName(securityRealm);
				sechandler.setLoginService(loginService);
				sechandler.addConstraintMapping(cm);
				
				context.setSecurityHandler(sechandler);
			}

			context.setContextPath("/");
			context.addServlet(new ServletHolder(servlet), ("/" + service).replaceAll("//", "/"));
			root.addHandler(context);
			try {
				context.start();
			} catch (Exception e) {
				throw new DME2Exception("AFT-DME2-0008", new ErrorContext().add(SERVICE, service).add(SERVLET, ""+servlet));
			}
		}
			catch (Exception e)
			{
				throw DME2ExceptionHandler.handleException(e, service);
			}
		}

  protected Server getServer() { return server; }
		protected void addHandler(ServletContextHandler handler) {
			root.addHandler(handler);
		}
		
		protected void addHandler(ContextHandler handler) {
			websocketHandlerCollection.addHandler(handler);			
		}
		
		public String getBaseAddress() {
			return baseAddress;
		}
		
		public boolean isRunning() {
			return running;
		}
		
		
		/**
		 * Start the server/listeners.
		 * @throws com.att.aft.dme2.api.DME2Exception
		 */
		public void start() throws DME2Exception {
			long start = System.currentTimeMillis();
			String defaultPort = null;
			boolean portSet = false;
			logger.debug( null, "start", LogMessage.METHOD_ENTER);

			assert (!running);
			// load configuration values or use defaults
			// these can be overridden by calls to the set* methods.
			String portRange = serverProperties.getPortRange();
			Integer connectionIdleTimeMs = serverProperties.getConnectionIdleTimeMs();
			Integer corePoolSize = serverProperties.getCorePoolSize();
			Integer maxPoolSize = serverProperties.getMaxPoolSize();
			Integer port = serverProperties.getPort();
			Integer socketAcceptorThreads = serverProperties.getSocketAcceptorThreads();
			Integer threadIdleTimeMs = serverProperties.getThreadIdleTimeMs();
			Integer requestBufferSize = serverProperties.getRequestBufferSize();
			Integer responseBufferSize = serverProperties.getResponseBufferSize();
			Boolean reuseAddress = serverProperties.isReuseAddress();
			Boolean useDirectBuffers = serverProperties.isUseDirectBuffers();
			String hostname = serverProperties.getHostname();
			Boolean sslEnable = serverProperties.isSslEnable();
			int maxQueueSize = serverProperties.getMaxQueueSize();
			int maxRequestPostSize = serverProperties.getMaxRequestPostSize();
			int maxRequestHeaderSize = serverProperties.getMaxRequestHeaderSize();
			logger.info( null, "start", LogMessage.SERVER_PARAMS, connectionIdleTimeMs, corePoolSize, maxPoolSize, maxQueueSize,
					threadIdleTimeMs, socketAcceptorThreads, requestBufferSize, responseBufferSize, useDirectBuffers, reuseAddress,
					(maxRequestPostSize > -1 ? maxRequestPostSize : ""), (maxRequestHeaderSize > -1 ? maxRequestHeaderSize : ""));

			InetAddress inetHost;
			try {
				if (hostname == null) {
					inetHost = InetAddress.getLocalHost();
				} else {
					inetHost = InetAddress.getByName(hostname);
				}

				// determine if we want to listen on all IP's or just a specific on
				String listenHost = null;
				String listenIp = null;
				InetAddress ipHost = InetAddress.getByName(inetHost
						.getHostAddress());
				listenHost = ipHost.getCanonicalHostName();
				if(hostname != null){
					listenIp = ipHost.getHostAddress();
				}
				else{
					listenIp = "0.0.0.0";
				}

				
				//TODO fix this
				btp = new DME2QueuedThreadPool();
				btp.setName("DME2::ConnectorDispatchThread");
				btp.setMinThreads(corePoolSize);
				btp.setMaxThreads(maxPoolSize);
				btp.setDaemon(true);
				btp.setIdleTimeout(threadIdleTimeMs);
				//btp.setMaxQueued(maxQueueSize); // by default, maxQueueSize = 0 (no queuing)
				
				if (server == null) {
					server = new Server(btp);
				}

				if (connector == null) {
					connector = createConnector(btp);
				}

				if (listenIp != null) {
					connector.setHost(listenIp);
				}

				server.setConnectors(new Connector[] { connector });
				//server.setThreadPool(btp);
				//server.addBean(btp);
	  			// set sendDateHeader, serverVersion
				Boolean sendDateHeader = serverProperties.getSendDateheader();
				Boolean sendServerVersion = serverProperties.getSendServerversion();
				Integer gracefulShutdownTimeoutMs = serverProperties.getGracefulShutdownTimeMs();
				HttpConfiguration httpConfig = new HttpConfiguration();
				if( sendDateHeader != null) {
					//server.setSendDateHeader(sendDateHeader);
					httpConfig.setSendDateHeader(sendDateHeader);
				}
				if(sendServerVersion != null) {
					//server.setSendServerVersion(sendServerVersion);
					httpConfig.setSendServerVersion(sendServerVersion);
				}
				if(gracefulShutdownTimeoutMs != null) {
					//server.setGracefulShutdown(gracefulShutdownTimeoutMs);
					server.setStopTimeout(gracefulShutdownTimeoutMs);
				}
				// this collection is mutable - we can add/remove contexts from it at runtime
				root = new ContextHandlerCollection();
				websocketHandlerCollection=new HandlerCollection(true);
				if(this.isWebSocket() == false) {
					server.setHandler(root);
				}else{
					server.setHandler(websocketHandlerCollection);
				}
				DME2ServerStopThread shutdown = DME2ServerStopThread.getInstance(this);
				shutdown.setDaemon(true);
				// Add shutdown hook
				Runtime.getRuntime().addShutdownHook(shutdown);

				try {
					if (persistedPorts != null) {
						persistedPort = this
								.getAvailablePersistedPort(persistedPorts);
						if (persistedPort > 0) {
							connector.setPort(persistedPort);
							try {
								server.start();
								if (server.isRunning() && ((ServerConnector)server.getConnectors()[0]).getPort() == persistedPort) {
										//&& server.getConnectors()[0].getPort() == persistedPort) {
									portSet = true;
								}
							} catch (java.net.BindException be) {
								// ignore bind exception till all range fails
								connector = createConnector(btp);
							} catch (Throwable te) {
								// ignore any exception till all range fails
								connector = createConnector(btp);

							}
						}
					}
					if(!portSet) {
					// If there is no explicit port range assigned by user
					// DME2 will assume the default port range of 45000-50000 for non-ssl
					// and 50001-53000 for ssl ports
					if(portRange == null && port == null) {
						if(serverProperties.isSslEnable()) {
							portRange = config.getProperty(DME2Constants.AFT_DME2_SERVER_DEFAULT_SSL_PORT_RANGE);
						}
						else {
							portRange = config.getProperty(DME2Constants.AFT_DME2_SERVER_DEFAULT_PORT_RANGE);
						}
					}
					if (portRange != null) {
						if (portRange.indexOf(DME2Constants.getPORT_RANGE_SEP()) == -1
								&& portRange
										.indexOf(DME2Constants.PORT_DEFAULT_SEP) == -1) {
							throw new DME2Exception(PORT_RANGE_ERROR_CODE,
									PORT_RANGE_ERROR_MSG);
						}
						try {
							String[] portRangeTemp = portRange.split(DME2Constants.PORT_DEFAULT_SEP);
							String portRangeStr = portRangeTemp[0];
							if (portRangeTemp.length > 1) {
								defaultPort = portRangeTemp[1];
							}

							String ports[] = portRangeStr.split(DME2Constants.getPORT_RANGE_SEP());
							String startPortRange = ports[0];
							String endPortRange = ports[1];

							int startPortInt = Integer.parseInt(startPortRange);
							int endPortInt = Integer.parseInt(endPortRange);
							int getAvailablePortAttemps = 0;
							for (int i = startPortInt; i <= endPortInt; i++) {
								int rPort = this.getAvailablePort(startPortInt, endPortInt);
								getAvailablePortAttemps++;
								if(getAvailablePortAttemps > config.getInt(DME2Constants.AFT_DME2_MAX_GETAVAIL_PORT_ATTEMPT)) {
									break;
								}
								connector.setPort(rPort);
								port = rPort;
								try {
									server.start();
								} catch (java.net.BindException be) {
									// ignore bind exception till all range fails
									connector = createConnector(btp);
									server.setConnectors(new Connector[] { connector });
									continue;
								}
								catch (Throwable te) {
									// ignore any exception till all range fails
									connector = createConnector(btp);
									server.setConnectors(new Connector[] { connector });
									continue;
								}
								if(server.isRunning() && ((ServerConnector)server.getConnectors()[0]).getPort() == rPort) {
										//server.getConnectors()[0].getPort() == rPort) {
									portSet = true;
									break;
								}
							}
						} catch (Exception e) {
							throw new DME2Exception("AFT-DME2-2101", new ErrorContext()
						    .add("extendedMessage", e.getMessage())
						    .add("portRange", portRange), e);
						}
						if (!portSet) {
							if (defaultPort != null) {
								connector.setPort(Integer.parseInt(defaultPort));
								server.start();
							} else {
								throw new DME2Exception("AFT-DME2-2104", new ErrorContext()
							    .add("portRange", portRange));
							}
						}
					} else if (port == null || port < 0) {
						connector.setPort(0);
						server.start();
					} else {
						connector.setPort(port);
						server.start();
					}
					}

				} catch (DME2Exception e) {
					throw e;
				} catch (Exception e) {
					throw new DME2Exception("AFT-DME2-2102", new ErrorContext()
				    .add("extendedMessage", e.getMessage())
				    .add("serverPort", connector.getPort()+""), e);
				}
				port = connector.getLocalPort();

				if (hostname == null || hostname.trim().equals("")) {
					hostname = listenIp;
				}
				serverProperties.setHostname(hostname);
				serverProperties.setPort(port);
				if(this.isWebSocket() == false) {
					if (sslEnable) {
						baseAddress = "https://" + listenHost + ":" + port + "/";
					} else {
						baseAddress = "http://" + listenHost + ":" + port + "/";
					}
				}else {

					if (sslEnable) {
						baseAddress = "wss://" + listenHost + ":" + port + "/";
					} else {
						baseAddress = "ws://" + listenHost + ":" + port + "/";
					}
				}

				running = true;
				logger.info( null, "start", LogMessage.SERVER_START, baseAddress);

				// bind all services
				Collection<DME2ServiceHolder> svcs = this.getServices();
				if(svcs != null && svcs.size() >0 ) {
					Iterator<DME2ServiceHolder> it = svcs.iterator();
					while(it.hasNext()) {
						final DME2ServiceHolder holder = it.next();
						holder.start();
					}
				}
			} catch (DME2Exception e) {
				throw e;
			} catch (Exception e) {
				throw new DME2Exception("AFT-DME2-2102", new ErrorContext()
			    .add("extendedMessage", e.getMessage())
			    .add("baseAddress", baseAddress), e);
			}
			logger.debug( null, "start", LogMessage.METHOD_EXIT);
		}
		/**
		 * Returns free threads count in QTP
		 * @return
		 */
		public int getServerPoolIdleThreads() {
			assert (running);
			logger.debug(
					null,
					"getServerPoolIdleThreads",
					"DME2Server isServerLowOnThread={};serverThreads={};serverIdleThreads={}",
					server.getThreadPool().isLowOnThreads(), server.getThreadPool()
							.getThreads(), server.getThreadPool().getIdleThreads());
			return server.getThreadPool().getIdleThreads();
		}
		/**
		 * Returns configured threads count from QTP
		 * @return
		 */
		public int getServerPoolThreads(){
			assert(running);
			return server.getThreadPool().getThreads();
		}

		public void stop() throws DME2Exception {
			logger.debug(null, "stop", LogMessage.METHOD_ENTER);
			if (running) {
				try {
					btp.stop();
				} catch (Exception e ) {
					logger.error( null, "stop", "Error stopping blocking thread pool in server", e );
				} finally {
					logger.debug( null, "stop", "BTP state: {}", btp.getState() );
				}
				try {
					server.stop();
				} catch (Exception e) {
					throw new DME2Exception(STOP_ERROR_CODE, STOP_ERROR_MSG, e);
				} finally {
					logger.debug( null, "stop", "Server state: {}", server.getState() );
				}
			} else {
				logger.warn(null, "stop", LogMessage.SERVER_STOP_WARN);
			}
			connector = null;
			server = null;
			running = false;
			logger.debug(null, "stop", LogMessage.METHOD_EXIT);
		}

		/**
		 * Unregister a local service listener
		 *
		 * @param service
		 */
		public void unbindServiceListener(String service) {
			if(root.getHandlers()==null){
				return;
			}
			for (Handler h : root.getHandlers()) {
				if (h instanceof ContextHandler) {
					ContextHandler ch = (ContextHandler)h;
					if (ch.getContextPath().equals(("/" + service).replaceAll("//", "/"))) {
						root.removeHandler(ch);
					}
				}
			}
		}

		private final Map<String,DME2ServiceHolder> services = new ConcurrentHashMap<String,DME2ServiceHolder>();

		public void addService(DME2ServiceHolder holder) throws DME2Exception
		{
			try
			{
				if(holder != null)
				{
					if(holder.getContext() == null)
					{
						// Check whether the serviceURI string carries any bindContext param
						try
						{
							String servicePath = holder.getServiceURI();
							DmeUniformResource uniformResouce = null;
							try {
								uniformResouce = new DmeUniformResource(config, servicePath);
							}catch(MalformedURLException e) {
								if(!servicePath.startsWith("/")) /*Check if contextPath has leading slash*/
								{
									servicePath = "/" + servicePath;
								}
								String completeServiceURI = null;
								if(!servicePath.startsWith("http://") && !servicePath.startsWith("dme2://") ) {
								/*Complete service URI will contain the full URI scheme needed to create the DmeUniformResource*/
								completeServiceURI = "http://DME2LOCAL" + servicePath;
								}
								else {
									completeServiceURI = servicePath;
								}
								uniformResouce = new DmeUniformResource(config, completeServiceURI);
							}

							if(holder.getDme2WebSocketHandler()!=null&& holder.getContext()==null){
								holder.setContext(uniformResouce.getPath());
							}

							if( uniformResouce != null && uniformResouce.getBindContext() != null)
							{
								holder.setContext(uniformResouce.getBindContext());
							}
              logger.debug( null, "addService", "Throttle Pct Per Partner: {}", uniformResouce.getThrottlePctPerPartner() );
							holder.setThrottlePctPerPartner(uniformResouce.getThrottlePctPerPartner());
							holder.setThrottleFilterDisabled(uniformResouce.getThrottleFilterDisabled());
						}
						catch(Exception e)
						{
							// ignore any exception in getting bindContext here
							logger.warn( null, "addService", "AFT-DME2-2103", new ErrorContext().add("ServiceURI", holder.getServiceURI()),e);
						}
					}
					services.put(holder.getServiceURI(), holder);
					holder.setServer(this);
					if (this.running)
					{
						holder.start();
					}
				}
			}
			catch (Exception e)
			{
				DME2ExceptionHandler.handleException(e, holder.getServiceURI());
			}
		}

		public Collection<DME2ServiceHolder> getServices() {
			return services.values();
		}

		public void removeService(final DME2ServiceHolder newHolder) throws DME2Exception {
			DME2ServiceHolder holder= newHolder;
			holder = services.get(holder.getServiceURI());
			if (holder != null) {
				if (holder.isActive()) {
					holder.stop();
				}
				services.remove(holder.getServiceURI());
			}
		}

		public DME2ServiceHolder getService(String serviceURI) {
			return services.get(serviceURI);
		}

		public String getPersistedPorts() {
			return persistedPorts;
		}

		public void setPersistedPorts(String persistedPorts) {
			this.persistedPorts = persistedPorts;
		}

		private int getAvailablePort(int aStart, int aEnd) {

			if (aStart > aEnd) {
				throw new IllegalArgumentException("Start cannot exceed End.");
			}

			// Avoiding looking up by range when range is <= 100 ( default value for DME2_GETAVAIL_PORT_RANGE ) ports
			if( (aEnd-aStart) <= config.getInt(DME2Constants.AFT_DME2_GETAVAIL_PORT_RANGE)) {
				for(int i=aStart;i<=aEnd;i++) {
					if(available(i)) {
						return i;
					}
				}
				// All ports in provided range were tried and it did not work, so returning end range to
				// have binding fail.
				return aEnd;
			}

			// get the range, casting to long to avoid overflow problems
			long range = (long) aEnd - (long) aStart + 1;
			Random aRandom = new Random();
			int port = 0;

			if (port == 0) {
				while ((port = (int) ((long) (range * aRandom.nextDouble()) + aStart)) <= aEnd
						&& !available(port)){
					continue;
				}
			}

			return port;

		}

		/**
		 *
		 * @param persistedPorts
		 * @return
		 */
		private int getAvailablePersistedPort(String persistedPorts) {

			int defport = 0;
			if(persistedPorts == null){
				return 0;
			}
			String[] ports = persistedPorts.split(",");
			for ( int i = 0; i< ports.length;i++) {
				try {
					int port = Integer.parseInt(ports[i]);
					if(available(port)) {
						return port;
					}
				} catch ( NumberFormatException nfe) {
					// ignore parse failure and proceed to next avail port
					logger.debug( null, "getAvailablePersistedPort", LogMessage.DEBUG_MESSAGE, "NumberFormatException",nfe);
				}
			}
			return defport;

		}

		//check the old method createConnectorOld this is the new Jetty 9 upgrade method		
		private ServerConnector createConnector(DME2QueuedThreadPool btp) throws UnknownHostException {
			// load configuration values or use defaults
			// these can be overridden by calls to the set* methods.
			Integer connectionIdleTimeMs = serverProperties.getConnectionIdleTimeMs();
			Integer socketAcceptorThreads = serverProperties.getSocketAcceptorThreads();
			Integer requestBufferSize = serverProperties.getRequestBufferSize();
			Integer responseBufferSize = serverProperties.getResponseBufferSize();
			Boolean reuseAddress = serverProperties.isReuseAddress();
			Boolean useDirectBuffers = serverProperties.isUseDirectBuffers();
			String hostname = serverProperties.getHostname();
			Boolean sslEnable = serverProperties.isSslEnable();
			
			HttpConfiguration httpConfiguration = new HttpConfiguration();
			httpConfiguration.setRequestHeaderSize(serverProperties.getMaxRequestHeaderSize());
			if (requestBufferSize != null) {
				//connector.setRequestBufferSize(requestBufferSize);
			}
			if (responseBufferSize != null) {
				httpConfiguration.setOutputBufferSize(responseBufferSize);
			}
			
			httpConfiguration.setRequestHeaderSize(serverProperties.getMaxRequestHeaderSize());
			
			if (config.getBoolean(DME2Constants.AFT_DME2_CONFIGURE_CUSTOM_CONNECTOR)) {
				if (sslEnable) {
					/*DME2SslSelectChannelConnector sslc = new DME2SslSelectChannelConnector();
					configureSsl(sslc.getSslContextFactory());
					connector = sslc;*/
				} else {
					//connector = new DME2SelectChannelConnector();
				}
			} else {
				if (sslEnable) {
					SslContextFactory sslContextFactory = new SslContextFactory();
					configureSsl(sslContextFactory);
					connector = new ServerConnector(server, sslContextFactory, new HttpConnectionFactory(httpConfiguration));
				} else {
					connector = new ServerConnector(server, socketAcceptorThreads, socketAcceptorThreads, new HttpConnectionFactory(httpConfiguration));
				}
			}
 
			InetAddress inetHost;
			if (hostname == null) {
				inetHost = InetAddress.getLocalHost();
			} else {
				inetHost = InetAddress.getByName(hostname);
			}

			// determine if we want to listen on all IP's or just a specific on
			String listenIp = null;
			InetAddress ipHost = InetAddress.getByName(inetHost.getHostAddress());
			if(hostname != null){
				listenIp = ipHost.getHostAddress();
			}
			else{
				listenIp = "0.0.0.0";
			}

			if (listenIp != null) {
				connector.setHost(listenIp);
			}
			
			connector.setIdleTimeout(connectionIdleTimeMs);
			connector.setReuseAddress(reuseAddress);
			//server.manage(btp);
			server.setConnectors(new Connector[] { connector });
			return connector;
		}

		/**
		 *
		 * @return
		 * @throws java.net.UnknownHostException
		 */
		/*private ServerConnector createConnectorOld(DME2QueuedThreadPool btp) throws UnknownHostException {
			// load configuration values or use defaults
			// these can be overridden by calls to the set* methods.
			Integer connectionIdleTimeMs = serverProperties.getConnectionIdleTimeMs();
			Integer socketAcceptorThreads = serverProperties.getSocketAcceptorThreads();
			Integer requestBufferSize = serverProperties.getRequestBufferSize();
			Integer responseBufferSize = serverProperties.getResponseBufferSize();
			Boolean reuseAddress = serverProperties.isReuseAddress();
			Boolean useDirectBuffers = serverProperties.isUseDirectBuffers();
			String hostname = serverProperties.getHostname();
			Boolean sslEnable = serverProperties.isSslEnable();
			
			if (DME2Constants.CONFIGURE_CUSTOM_CONNECTOR) {
				if (sslEnable) {
					DME2SslSelectChannelConnector sslc = new DME2SslSelectChannelConnector();
					configureSsl(sslc.getSslContextFactory());
					connector = sslc;
				} else {
					connector = new DME2SelectChannelConnector();
				}
			} else {
				if (sslEnable) {
					SslSelectChannelConnector sslc = new SslSelectChannelConnector();
					configureSsl(sslc.getSslContextFactory());
					connector = sslc;
				} else {
					connector = new ServerConnector();
				}
			}

			InetAddress inetHost;
			if (hostname == null) {
				inetHost = InetAddress.getLocalHost();
			} else {
				inetHost = InetAddress.getByName(hostname);
			}

			// determine if we want to listen on all IP's or just a specific on
			String listenIp = null;
			InetAddress ipHost = InetAddress.getByName(inetHost
					.getHostAddress());
			if(hostname != null){
				listenIp = ipHost.getHostAddress();
			}
			else{
				listenIp = "0.0.0.0";
			}

			if (listenIp != null) {
				connector.setHost(listenIp);
			}

			connector.setThreadPool(btp);	
			connector.setMaxIdleTime(connectionIdleTimeMs);
			connector.setAcceptors(socketAcceptorThreads);
			connector.setUseDirectBuffers(useDirectBuffers);
			connector.setReuseAddress(reuseAddress);

			connector.setStatsOn(true);
			if (requestBufferSize != null) {
				connector.setRequestBufferSize(requestBufferSize);
			}
			if (responseBufferSize != null) {
				connector.setResponseBufferSize(responseBufferSize);
			}
			
			connector.setRequestHeaderSize(serverProperties.getMaxRequestHeaderSize());
			
			server.setConnectors(new Connector[] { connector });
			return connector;
		} */

		private void configureSsl(SslContextFactory cf) {
			String keystore = serverProperties.getKeyStore();
			String keystorePw = serverProperties.getConfig().getProperty(KEY_KEYSTORE_PASSWORD);
			String truststore = serverProperties.getTrustStore();
			String truststorePw = serverProperties.getConfig().getProperty(KEY_TRUSTSTORE_PASSWORD);
			String keyPw = serverProperties.getConfig().getProperty(KEY_PASSWORD);
			Boolean allowRenegotiate = serverProperties.isAllowRenegotiate();
			Boolean trustAll = serverProperties.getSslTrustAll();
	        String certAlias = serverProperties.getSslCertAlias();
	        Boolean needClientAuth = serverProperties.getNeedClientAuth();
	        Boolean wantClientAuth = serverProperties.getWantClientAuth();
	        Boolean enableSessionCaching = serverProperties.isEnableSessionCaching();
	        Integer sslSessionCacheSize = serverProperties.getSslSessionCacheSize();
	        Integer sslSessionTimeout = serverProperties.getSslSessionTimeout();
	        Boolean validatePeerCerts = serverProperties.isSslValidatePeerCerts();
	        Boolean validateCerts = serverProperties.isValidateCerts();
	        String[] excludeProtocols = serverProperties.getExcludeProtocols();
	        String[] excludeCipherSuites = serverProperties.getExcludeCiperSuites();
	        String[] includeProtocols = serverProperties.getIncludeProtocols();
	        String[] includeCipherSuites = serverProperties.getIncludeCiperSuites();
			
	        // check for null.  if not set, defer to Jetty defaults
	        if (keystore != null) {
	        	cf.setKeyStorePath(keystore);
	        }
	        if (keystorePw != null){
	        	cf.setKeyStorePassword(keystorePw);
	        }
	        if (keyPw != null) {
	        	cf.setKeyManagerPassword(keyPw);
	        }
			if (trustAll != null){
				cf.setTrustAll(trustAll);
			}
			if (certAlias != null){
				cf.setCertAlias(certAlias);
			}
			if (needClientAuth != null) {
				cf.setNeedClientAuth(needClientAuth);
			}
			if (wantClientAuth != null){
				cf.setWantClientAuth(wantClientAuth);
			}
			if (enableSessionCaching != null){
				cf.setSessionCachingEnabled(enableSessionCaching);
			}
			if (sslSessionCacheSize != null){
				cf.setSslSessionCacheSize(sslSessionCacheSize);
			}
			if (sslSessionTimeout != null){
				cf.setSslSessionTimeout(sslSessionTimeout);
			}
			if (validatePeerCerts != null){
				cf.setValidatePeerCerts(validatePeerCerts);
			}
			if (validateCerts != null){
				cf.setValidateCerts(validateCerts);
			}
	        if (truststore != null) {
	        	//TODO needs to fix it
	        	//cf.setTrustStore(truststore);
	        	cf.setTrustStorePassword(truststorePw);
	        } else {
	        	//TODO needs to fix it
	        	//cf.setTrustStore(keystore);
	        	cf.setTrustStorePassword(keystorePw);
	        }
	        if(excludeProtocols != null){
	        	cf.setExcludeProtocols(excludeProtocols);
	        }
	        if(includeProtocols != null){
	        	cf.setIncludeProtocols(includeProtocols);
	        }
	        if(excludeCipherSuites != null){
	        	cf.setExcludeCipherSuites(excludeCipherSuites);
	        }
	        if(includeCipherSuites != null){
	        	cf.setIncludeCipherSuites(includeCipherSuites);
	        }
	        
	        cf.setRenegotiationAllowed(allowRenegotiate);
			
		}
		/**
		 * 
		 * @param port
		 * @return
		 */
		private boolean available(int port) {
		logger.debug( null, "available", "DME2Server.available;Trying available for {}", port);
		ServerSocket ss = null;
		DatagramSocket ds = null;
			try {
				ss = new ServerSocket(port);
				ss.setReuseAddress(true);
				ds = new DatagramSocket(port);
				ds.setReuseAddress(true);
				return true;
			} 
			catch (IOException e) {
				logger.debug( null, "available", LogMessage.DEBUG_MESSAGE, "IOException",e);
			} 
			finally {
				if (ds != null) {
					ds.close();
				}
				if (ss != null) {
					try {
						ss.close();
					} 
					catch (IOException e) { 
						logger.debug( null, "available", LogMessage.DEBUG_MESSAGE, "IOException",e);
						//should not be thrown 
					}
				}
			}
			return false;
		}

		@SuppressWarnings("resource")
		public String [] getSSLExcludeProtocol(){
			if(server != null && server.isRunning()) {
				Connector[] cs = server.getConnectors();
				for(Connector conn: cs) {
					/*
					if(conn instanceof ServerConnector) {
						ServerConnector sslc = (ServerConnector) conn;
						return (String[]) sslc.getProtocols().toArray();
					}*/
					
					if(conn instanceof DME2SslSelectChannelConnector) {
						DME2SslSelectChannelConnector sslc = (DME2SslSelectChannelConnector) conn;
						return sslc.getSslContextFactory().getExcludeProtocols();
					}
				}
			}
			return null;
		}	
		
		public HandlerCollection getWebsocketHandlerCollection() {
			return websocketHandlerCollection;
		}

		public void setWebsocketHandlerCollection(
				HandlerCollection websocketHandlerCollection) {
			this.websocketHandlerCollection = websocketHandlerCollection;
		}
		
		public boolean isWebSocket() {
			return webSocket;
		}

		public void setWebSocket(boolean webSocket) {
			this.webSocket = webSocket;
		}

		public DME2ServerProperties getServerProperties() {
			return serverProperties;
		}
}

class DME2ServerStopThread extends Thread {
	private static DME2ServerStopThread instance = null;
	private static final Logger logger = LoggerFactory.getLogger(DME2ServerStopThread.class.getName());
	private static DME2Server server = null;

	public static DME2ServerStopThread getInstance(DME2Server server) {
		if (instance == null) {
			return new DME2ServerStopThread(server);
		}
		return instance;
	}

	private DME2ServerStopThread(DME2Server server) {
		DME2ServerStopThread.server = server;
	}

	@Override
	public void run() {
    logger.debug( null, "run", "Intiating server shutdown - shutdown hook activated" );
		try {
			server.stop();
		} catch (Exception e) {
			logger.error( null, "run", "AFT-DME2-2100", new ErrorContext()
		     .add("serverAddress", server.getBaseAddress())
		     .add("serverHostname", server.getServerProperties().getHostname())
		     .add("serverPort", server.getServerProperties().getPort()+""),e);
			
		}
	}
}
