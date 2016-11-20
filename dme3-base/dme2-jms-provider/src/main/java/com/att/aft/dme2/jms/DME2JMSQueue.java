/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;

import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

@SuppressWarnings("PMD.AvoidCatchingThrowable")
public abstract class DME2JMSQueue implements Queue, java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private List<MessageListenerHolder> listeners = Collections
			.synchronizedList(new LinkedList<MessageListenerHolder>());

	private String name;
	private String queueHost;
	private final URI originalURI;

	private static final int DEFAULT_JMS_PRIORITY = 4;
	private static final int DEFAULTTTLMS = 300000;
	private int defaultPriority = DEFAULT_JMS_PRIORITY;

	private long defaultTtlMs = DEFAULTTTLMS;
	private boolean client = true;
	private long createTime = 0;
	@SuppressWarnings("unused")
	private final DME2JMSManager manager;

	private int directPort;

	private String userName;
	private String password;

	private String realmName;
	private String[] allowedRoles;
	private String loginMethod;
	private boolean qdebug;
  private AtomicInteger activeReceiverCount = new AtomicInteger(0);
	private final Map<String, String> queryParams = new HashMap<String, String>();
	private DME2Configuration config;


	/** The logger. */
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSQueue.class.getName());

	protected DME2JMSQueue(DME2JMSManager manager, URI uri) throws JMSException {
		this.originalURI = uri;
		this.manager = manager;
		this.config = manager.getDME2Manager().getConfig();
		
		if (uri != null && uri.getQuery() != null) {
			String[] pairs = uri.getQuery().split("&");
			for (String pair : pairs) {
				String[] keyValue = pair.split("=");
				if (keyValue.length == 2) {
					if (keyValue[0].equals("defaultTtlMs")) {
						this.defaultTtlMs = Long.parseLong(keyValue[1]);
					} else if (keyValue[0].equals("defaultPriority")) {
						this.defaultPriority = Integer.parseInt(keyValue[1]);
					} else if (keyValue[0].equals("server")) {
						logger.debug(null, "DME2JMSQueue", "DME2JMSQueue is server type ");

						this.client = false;
					} else if (keyValue[0].equals("qdebug")) {
						this.qdebug = true;
					} else if (keyValue[0].equals("realm")) {
						this.realmName = keyValue[1];
					} else if (keyValue[0].equals("loginMethod")) {
						this.loginMethod = keyValue[1];
					} else if (keyValue[0].equals("allowedRoles")) {
						String allowedRolesStr = keyValue[1];
						if (allowedRolesStr != null) {
							this.allowedRoles = allowedRolesStr.split(",");
						}
					} else if (keyValue[0].equals("userName")) {
						this.userName = keyValue[1];
					} else if (keyValue[0].equals("password")) {
						this.password = keyValue[1];
					} else {
						//TODO
						//super.setProperty(keyValue[0], keyValue[1]);
            logger.info( null, "ctor", "Overriding property from queue. Key: {} Value: {}", keyValue[0], keyValue[1] );
            config.setOverrideProperty( keyValue[0], keyValue[1] );
					}
					this.queryParams.put(keyValue[0], keyValue[1]);
				}
			}
		}

    if ( userName == null && password == null && manager.getUserName() != null && manager.getPassword() != null ) {
      userName = manager.getUserName();
      password = manager.getPassword();
    }
		if (uri != null) {
			this.queueHost = uri.getHost();
			this.directPort = uri.getPort();

			/**
			 * String routeOffer = System.getProperty("lrmRO"); // if(routeOffer
			 * != null) { // String tempPath = uri.getPath(); // if(tempPath !=
			 * null) { // DME2URI DME2Uri = new DME2URI(uri); // String
			 * inRouteOffer = DME2Uri.getRouteOffer(); // if(inRouteOffer!=null)
			 * { // routeOffer = routeOffer.replaceAll("'",""); // tempPath =
			 * tempPath.replace("routeOffer="+inRouteOffer,
			 * "routeOffer="+routeOffer); // this.name = tempPath; //
			 * if(DME2Constants.debug) { // System.err.println(new
			 * java.util.Date() + " \t DME2JMSQueue init " + uri.getPath() +
			 * ";lrmRO=" + System.getProperty("lrmRO") //
			 * +";queueName="+this.name); // } // } // else { // this.name =
			 * uri.getPath(); // } // } // } else {
			 */
			this.name = uri.getPath();
			/**
			 * } }
			 */
		}

		// Identify whether queue is of type client
		if (this.name != null) {
			String qname = this.name.toLowerCase();
			if (qname.indexOf("version=") != -1 && qname.indexOf("routeoffer=") != -1
					&& qname.indexOf("service=") != -1) {
				this.client = false;
			}
			logger.debug(null, "DME2JMSQueue", "DME2JMSQueue [{}] - init {}; queue isClient={}", this.getQueueName(),  name,
					 this.isClient());

		}
		this.createTime = System.currentTimeMillis();
	}

	public void setClient(boolean client) {
		this.client = client;
	}

	@Override
	public String getQueueName() throws JMSException {
		return name;
	}

	/**
	 * returns the complete DME2URI strong along with queryParams passed in Will
	 * be utilized for bindServiceListener method call to propagate queryParam
	 * to registry API call
	 * 
	 * @return
	 * @throws JMSException
	 */
	public String getQueueNameURI() throws JMSException {
		String qParams = this.getQueryParamsAsString();
		if (qParams != null && qParams.length() > 0) {
			return name + "?" + qParams;
		}
		return name.toString();
	}

	public long getCreateTime() {
		return this.createTime;
	}

	public URI getDME2URI() {
		return this.originalURI;
	}

	public String getQueueHost() {
		if (this.directPort > 0) {
			return this.queueHost + ":" + directPort;
		} else {
			return this.queueHost;
		}
	}

	public abstract void put(DME2JMSMessage m) throws JMSException;

	public void addListener(DME2JMSMessageConsumer consumer, MessageListener listener, String filter)
			throws JMSException {
		MessageListenerHolder holder = new MessageListenerHolder(consumer, listener, filter, this);
		synchronized (listeners) {
			listeners.add(holder);
		}
		if (this.qdebug) {
			logger.debug(null, "addListener", JMSLogMessage.QUEUE_ADDLISTENER, getQueueName(), listener.getClass(),
					listeners.size());
		}
		logger.debug(null, "addListener", "DME2JMSQueue [{}] - addListener {};size={}", this.getQueueName(), listener,
				 listeners.size());
	}

	public void removeListener(DME2JMSMessageConsumer consumer) throws JMSException {
		synchronized (listeners) {
			Iterator<?> it = listeners.iterator();
			while (it.hasNext()) {
				MessageListenerHolder holder = (MessageListenerHolder) it.next();
				if (holder.isSameListener(consumer)) {
					if (consumer != null) {
						logger.info(null, "removeListener", JMSLogMessage.QUEUE_REMOVELISTNR, getQueueName(),
								consumer.getClass(), consumer.getMessageListener(), getListeners().size());

					}
					it.remove();
				}
			}
		}
		logger.debug(null, "removeListener", "DME2JMSQueue [{}] - removeListener listeners size={}", this.getQueueName(),
				 listeners.size());

	}

	public void removeListener(DME2JMSMessageConsumer consumer, MessageListener listener) throws JMSException {
		synchronized (listeners) {
			Iterator<?> it = listeners.iterator();
			while (it.hasNext()) {
				MessageListenerHolder holder = (MessageListenerHolder) it.next();
				if (holder.isSameListener(listener)) {
					if (this.qdebug) {
						logger.info(null, "removeListener", JMSLogMessage.QUEUE_REMOVE_SAME, getQueueName(),
								consumer.getClass(), listener, listeners.size());
					}
					holder.checkin();
					it.remove();
				}
			}
		}
		logger.debug(null, "removeListener", "DME2JMSQueue [{}] - removeListener {};listeners size={}", this.getQueueName(),  listener,
				 listeners.size());
	}

	public MessageListenerHolder checkoutListener(DME2JMSMessage m) throws JMSException {
		MessageListenerHolder holder = null;
		logger.debug(null, "checkoutListener", "DME2JMSQueue [{}] - checkoutListener ;listeners size={}", this.getQueueName(),  listeners.size());
		synchronized (listeners) {
			Iterator<?> it = listeners.iterator();
			while (it.hasNext()) {
				MessageListenerHolder h = (MessageListenerHolder) it.next();
				logger.debug( null, "checkoutListener", "Checking holder {} - isAvailable: {} matches message {}: {}", h, h.isAvailable(), m, h.matches( m ) );
				try {
					if (h.isAvailable() && h.matches(m)) {
						if (this.qdebug) {
							logger.info(null, "checkoutListener", JMSLogMessage.QUEUE_CHECKOUT, getQueueName(), h,
									listeners.size());
						}
						h.checkout();
						holder = h;
						break;
					}
				} catch (Exception e) {
					logger.error(null, "checkoutListener", JMSLogMessage.CHECKOUT_FAIL, e);
				}
			}
		}
		logger.debug(null, "checkoutListener", "DME2JMSQueue [{}] - checkoutListener Holder={};listeners size={}", this.getQueueName(),
				holder, listeners.size());
		return holder;
	}

	public void setListeners(List<MessageListenerHolder> listeners) throws JMSException {
		this.listeners = listeners;
		if (this.qdebug) {
			logger.info(null, "setListeners", JMSLogMessage.QUEUE_REMOVELISTNR, getQueueName(), listeners,
					listeners.size());
		}
		logger.debug(null, "setListeners", "DME2JMSQueue [", this.getQueueName(), "] - setListeners;listeners size=",
				listeners.size());
	}

	public List<MessageListenerHolder> getListeners() {
		return listeners;
	}

	public int getDefaultPriority() {
		return this.defaultPriority;
	}

	public long getDefaultTtlMs() {
		return this.defaultTtlMs;
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
			this.allowedRoles = Arrays.copyOf(newAllowedRoles, newAllowedRoles.length);
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

	public boolean isClient() {
		return this.client;
	}

	public boolean isQdebug() {
		return qdebug;
	}

	public void setQdebug(boolean qdebug) {
		this.qdebug = qdebug;
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public String getQueryParamsAsString() {
		StringBuffer temp = new StringBuffer();
		for (String key : queryParams.keySet()) {
			temp.append(key + "=" + queryParams.get(key) + "&");
		}
		return temp.toString();
	}

	public DME2Configuration getConfig() {
		return config;
	}

	public void setConfig(DME2Configuration config) {
		this.config = config;
	}

	protected String getUserName() {
		return userName;
	}

	protected void setUserName(String userName) {
		this.userName = userName;
	}

	protected String getPassword() {
		return password;
	}

	protected void setPassword(String password) {
		this.password = password;
	}

  public AtomicInteger getActiveReceiverCount() {
    return activeReceiverCount;
  }
}

class MessageListenerHolder {
	private final MessageListener listener;
	private final String filter;
	private final DME2JMSMessageConsumer consumer;
	private boolean checkedOut = false;
	private boolean receiverType = false;
	private boolean qdebug = false;
	private Message msg = null;
	private final Destination destination;
	/** The logger. */
	private static final Logger logger = LoggerFactory.getLogger(MessageListenerHolder.class.getName());

	public MessageListenerHolder(DME2JMSMessageConsumer consumer, MessageListener listener, String filter,
			Destination dest) {
		this.listener = listener;
		this.filter = filter;
		this.consumer = consumer;
		if (this.listener instanceof DME2JMSDefaultListener) {
			this.receiverType = true;
		}
		this.destination = dest;
		try {
			DME2JMSQueue queue = (DME2JMSQueue) this.destination;
			this.qdebug = queue.isQdebug();
		} catch (Throwable e) { // NOSONAR
			logger.debug(null, "MessageListenerHolder", "Exception",
					new ErrorContext().add("extendedMessage", e.toString()));
		}
	}

	public boolean matches(DME2JMSMessage m) throws JMSException {

		boolean match = m.matches(filter);
		logger.debug(null, "matches", "MessageListenerHolder [{}] - matches filter={};match={}", destination.toString(), filter, match);

		if (this.qdebug) {
			logger.info(null, "matches", JMSLogMessage.QUEUE_MATCHES, destination, filter, match, listener);

		}
		return match;
	}

	public boolean isReceiver() {
		return this.receiverType;
	}

	public void onMessage(DME2JMSMessage m) throws JMSException {
		this.msg = m;
		try {
			listener.onMessage(this.msg);
		} catch (RuntimeException e) {
			logger.error(null, "onMessage", "Exception", e);
		}
		
		if (this.qdebug) {
			logger.info(null, "onMessage", JMSLogMessage.QUEUE_MATCHES, destination, listener);
		}
	}

	public DME2JMSTextMessage copy(DME2JMSMessage message) throws JMSException {

		DME2JMSTextMessage copy = new DME2JMSTextMessage();
		Properties properties = message.getProperties();
		Enumeration<?> propertyNames = message.getPropertyNames();
		while (propertyNames.hasMoreElements()) {
			String name = propertyNames.nextElement().toString();
			String value = properties.getProperty(name);
			copy.setStringProperty(name, value);
		}
		if (message instanceof DME2JMSTextMessage) {
			copy.setText(((DME2JMSTextMessage) message).getText());
		}
		if (message.getJMSCorrelationID() != null) {
			copy.setJMSCorrelationID(message.getJMSCorrelationID());
		}
		if (message.getJMSCorrelationIDAsBytes() != null) {
			copy.setJMSCorrelationIDAsBytes(message.getJMSCorrelationIDAsBytes());

			copy.setJMSDeliveryMode(message.getJMSDeliveryMode());
		}
		if (message.getJMSDestination() != null) {
			copy.setJMSDestination(message.getJMSDestination());
		}

		copy.setJMSExpiration(message.getJMSExpiration());
		if (message.getJMSMessageID() != null) {
			copy.setJMSMessageID(message.getJMSMessageID());
		}

		copy.setJMSPriority(message.getJMSPriority());
		copy.setJMSRedelivered(message.getJMSRedelivered());
		if (message.getJMSReplyTo() != null) {
			copy.setJMSReplyTo(message.getJMSReplyTo());
		}

		copy.setJMSTimestamp(message.getJMSTimestamp());
		if (message.getJMSType() != null) {
			copy.setJMSType(message.getJMSType());
		}
		return copy;
	}

	public boolean acceptsMessage(DME2JMSMessage m) throws JMSException {
		if (m.matches(this.filter)) {
			return true;
		}
		return false;
	}

	public boolean isSameListener(DME2JMSMessageConsumer consumer) {
		if (this.consumer.getID().equals(consumer.getID())) {
			return true;
		}
		return false;
	}

	public boolean isSameListener(MessageListener jlistener) {
		if (jlistener instanceof DME2JMSDefaultListener) {
			if (((DME2JMSDefaultListener) this.listener).getID() == ((DME2JMSDefaultListener) jlistener).getID()) {
				return true;
			}
		}
		return false;
	}

	public synchronized boolean isAvailable() {
		logger.debug(null, "isAvailable", "MessageListenerHolder [{}] - isAvailable {}", destination.toString(),
				!checkedOut);
		if (this.qdebug) {
			logger.info(null, "isAvailable", JMSLogMessage.QUEUE_LISTEN_AVAIL, listener, !checkedOut);
		}
		return !checkedOut;
	}

	public synchronized void checkout() {
		if (this.qdebug) {
			logger.info(null, "checkout", JMSLogMessage.QUEUE_CHECKING_OUT, listener);
		}
		checkedOut = true;
	}

	public synchronized void checkin() {
		if (this.qdebug) {
			logger.info(null, "checkout", JMSLogMessage.QUEUE_CHECKING_IN, listener);
		}
		checkedOut = false;
	}
}
