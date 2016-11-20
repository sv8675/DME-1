/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.NamingException;
import javax.naming.Reference;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.jms.util.DME2JNDIReferenceFactory;
import com.att.aft.dme2.jms.util.DME2JNDIStorableInterface;
import com.att.aft.dme2.jms.util.JMSConstants;
import com.att.aft.dme2.jms.util.JMSLogMessage;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.DME2Constants;
import com.att.aft.dme2.util.ErrorContext;

/**
 * This class represents a LOCAL queue managed by the JMSManager. A Local queue
 * is one where:
 * 
 * 1 - listeners are registered that reside in the same JVM. 2 - get() calls are
 * executed against an in-memory BlockingQueue. 3 - put() calls are executed by
 * first searching for a matching listener and, if none is found, adding to the
 * BlockingQueue 4 - message expiration semantics on the BlockingQueue are
 * honered. 5 - thread pool is incremental grown or shrunk based on the number
 * of registered Listeners.
 */

@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class DME2JMSLocalQueue extends DME2JMSQueue implements DME2JNDIStorableInterface, Externalizable {

	private static final Logger logger = LoggerFactory.getLogger(DME2JMSLocalQueue.class.getName());
	private transient ThreadPoolExecutor threadpool = null;
	private static transient ThreadPoolExecutor tempQueueThreadpool = null;
	private static ThreadFactory tempQueueTFactory;
	private final int corePoolSize = 0;
	private final int maxPoolSize = 1;
	private static final int CONSTANT_TTL = 60000;
	private final long ttl = CONSTANT_TTL;

	private static final int CONSTANT_TEMPQUEUECOREPOOLSIZE = 50;
	private int tempQueueCorePoolSize = CONSTANT_TEMPQUEUECOREPOOLSIZE;

	private static final int CONSTANT_TEMPQUEUEMAXPOOLSIZE = 500;
	private int tempQueueMaxPoolSize = CONSTANT_TEMPQUEUEMAXPOOLSIZE;
	private long tempQueueTtl = CONSTANT_TTL;

	private static final int CONSTANT_MAXPUTRETRY = 10;
	private int maxPutRetry = CONSTANT_MAXPUTRETRY;
	private BlockingQueue<DME2JMSMessage> queueData = null;
	private final List<WaiterNotifier> waiters = Collections.synchronizedList(new ArrayList<WaiterNotifier>());
	private ThreadFactory tFactory;
	private boolean registered = false;
	private boolean opened = true;
	private transient DME2JMSManager manager;
	private boolean isTempQueue = false;

	private static final int CONSTANT_SERVERQDEPTH = 1;
//	private static final int CONSTANT_CLIENTQDEPTH = 10;
	private static final int CONSTANT_CLIENTQDEPTH = 50;	
	private int serverQDepth = CONSTANT_SERVERQDEPTH;
	private int clientQDepth = CONSTANT_CLIENTQDEPTH;
	private static final int CONSTANT_AFT_DME2_MAX_RETRY = 10;
	private static final int CONSTANT_AFT_DME2_TEMPQ_TP_CORE=50;
	private static final int CONSTANT_AFT_DME2_TEMPQ_TP_MAX=500;
	private static byte[] lockObj = new byte[0];
	private DME2Configuration config;

	public DME2JMSLocalQueue() throws JMSException {
		super(null, null);
		logger.debug(null, "DME2JMSLocalQueue", "LocalQueue default constructor");
	}

	protected DME2JMSLocalQueue(DME2JMSManager manager, URI name, boolean isTempQueue) throws JMSException {
		super(manager, name);
		this.isTempQueue = isTempQueue;
		logger.debug(null, "DME2JMSLocalQueue", "LocalQueue args constructor; QueueMgr name={}; isTempQueue={}",
				manager.getDME2Manager().getName(), isTempQueue);
		try {
			this.manager = manager;
			this.config = manager.getDME2Manager().getConfig();
			maxPutRetry = config.getInt(JMSConstants.AFT_DME2_MAX_RETRY);
		} catch (Exception e) {
			maxPutRetry = CONSTANT_AFT_DME2_MAX_RETRY;
		}
		// Get the config for QDepth
		logger.debug(null, "DME2JMSLocalQueue", "value of AFT_DME2_SERVER_QDEPTH is: {}",config.getInt(JMSConstants.AFT_DME2_SERVER_QDEPTH));
		serverQDepth = config.getInt(JMSConstants.AFT_DME2_SERVER_QDEPTH, CONSTANT_SERVERQDEPTH);
		clientQDepth = config.getInt(JMSConstants.AFT_DME2_CLIENT_QDEPTH, CONSTANT_CLIENTQDEPTH);

		tempQueueCorePoolSize = config.getInt(JMSConstants.AFT_DME2_TEMPQ_TP_CORE, CONSTANT_AFT_DME2_TEMPQ_TP_CORE);
		tempQueueMaxPoolSize = config.getInt(JMSConstants.AFT_DME2_TEMPQ_TP_MAX, CONSTANT_AFT_DME2_TEMPQ_TP_MAX);
		tempQueueTtl = config.getInt(JMSConstants.AFT_DME2_TEMPQ_TP_TTL);

		if (this.isClient()) {
			queueData = new LinkedBlockingQueue<DME2JMSMessage>(clientQDepth);
		} else {
			queueData = new LinkedBlockingQueue<DME2JMSMessage>(serverQDepth);
		}
		if (!isTempQueue) {
			tFactory = new ThreadFactory() {

				private int counter = 0;

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					String name = "";
					try {
						name = getQueueName();
					} catch (Exception e) {
						logger.debug(null, "DME2JMSLocalQueue", LogMessage.DEBUG_MESSAGE, "Exception", e);
					}
					t.setName("DME2JMS::ListenerThread[" + name + "]-" + counter++);
					return t;
				}

			};
			logger.debug(null, "DME2JMSLocalQueue", LogMessage.METHOD_ENTER);
			threadpool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, ttl, TimeUnit.MILLISECONDS,
					new SynchronousQueue<Runnable>(true), tFactory);
		} else {
			if (tempQueueThreadpool == null) {
				synchronized (lockObj) {
					if (tempQueueThreadpool == null) {
						tempQueueTFactory = new ThreadFactory() {
							private int counter = 0;

							@Override
							public Thread newThread(Runnable r) {
								Thread t = new Thread(r);
								t.setName("DME2JMS::ListenerThread[TempQueue]-" + counter++);
								return t;
							}

						};
						tempQueueThreadpool = new ThreadPoolExecutor(tempQueueCorePoolSize, tempQueueMaxPoolSize,
								tempQueueTtl, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true),
								tempQueueTFactory);
					}
				}
			}
		}
		// DME2Manager.getInstance().getServer().get
		logger.debug(null, "DME2JMSLocalQueue", "Created: {}", name);
		logger.debug(null, "DME2JMSLocalQueue", LogMessage.METHOD_EXIT);
	}

	/**
	 * Get a matching message from the Queue
	 * 
	 * @param timeout
	 * @param filter
	 * @return
	 * @throws JMSException
	 */
	protected Message get(long timeout, String filter) throws JMSException {
		if (!isOpen()) {
			throw new DME2JMSException("AFT-DME2-5400", new ErrorContext());
		}
		logger.debug(null, "get", LogMessage.METHOD_ENTER, this.getQueueName());
		DME2JMSMessage m = null;
		WaiterNotifier notifier = null;
		try {

			long startTime = System.currentTimeMillis();
			long waitTime = timeout;

			// register a notifier
			notifier = new WaiterNotifier(filter, this);
			synchronized (waiters) {
				waiters.add(notifier);
			}

			// search through memory queue once to make sure the message isn't
			// already in place
			synchronized (queueData) {
				Iterator<DME2JMSMessage> it = queueData.iterator();
				while (it.hasNext()) {
					DME2JMSMessage candidate = it.next();
					// throw out any expired messages

					if (candidate.isExpired()) {
						it.remove();
						continue;
					}

					// if no filter, grab the first thing in the queue
					if (filter == null || candidate.matches(filter)) {
						it.remove();
						m = candidate;
						break;
					}
				}
			}

			// now wait on the waiter
			if (m == null) {
				long elapsed = System.currentTimeMillis() - startTime;
				waitTime = timeout - elapsed;
				if (timeout == 0) {
					m = notifier.get(timeout);
				} else if (waitTime > 0) {
					m = notifier.get(waitTime);
				}
			}

			// log the message
			if (m != null) {
				java.util.Properties debugProps = m.getProperties();
				StringBuffer debugSB = new StringBuffer();
				Enumeration<?> e = debugProps.propertyNames();
				while (e.hasMoreElements()) {
					Object key = e.nextElement();
					Object value = debugProps.get(key);
					if (debugSB.length() > 1) {
						debugSB.append(",");
					}
					debugSB.append(key);
					debugSB.append("=");
					debugSB.append(value);
				}
				logger.debug(null, "get", "get: ", debugSB);
			}

			if (m instanceof DME2JMSErrorMessage) {
				DME2JMSErrorMessage em = (DME2JMSErrorMessage) m;
				if (em.isFastFailNull()) {
					logger.warn(null, "get", "get: ", "AFT-DME2-5408",
							new ErrorContext().add("extendedMessage", em.getJMSException().getMessage())
									.add("queue", getQueueName())
									.add("endpointsAttempted", em.getStringProperty("AFT_DME2_REQ_TRACE_INFO"))
									.add("filter", filter),
							em.getJMSException());
					throw new DME2JMSException("AFT-DME2-5408",
							new ErrorContext().add("extendedMessage", em.getJMSException().getMessage())
									.add("queue", getQueueName())
									.add("endpointsAttempted", em.getStringProperty("AFT_DME2_REQ_TRACE_INFO"))
									.add("filter", filter),
							em.getJMSException());
				} else {
					throw new DME2JMSException("AFT-DME2-5401",
							new ErrorContext().add("extendedMessage", em.getJMSException().getMessage())
									.add("queue", getQueueName()).add("filter", filter),
							em.getJMSException());
				}
			}

			return m;
		} finally {
			if (notifier != null) {
				synchronized (waiters) {
					waiters.remove(notifier);
				}
			}
			logger.debug(null, "get", LogMessage.METHOD_EXIT);
		}
	}

	/**
	 * Put a message on the queue. If any listeners are registered, this method
	 * will acquire an execution thread and execute the listener with the
	 * message.
	 */
	@Override
	public void put(DME2JMSMessage m) throws JMSException {
		logger.debug(null, "put", LogMessage.METHOD_ENTER, this.getQueueName());
		logger.debug(null, "put", "LocalQueue JMSMessage put ", m.getJMSMessageID());
		boolean putDone = false;
		m.setJMSDestination(this);
		// set default priority if not set on message
		if (m.getJMSPriority() < 0) {
			m.setJMSPriority(super.getDefaultPriority());
		}
		// set default jms delivery mode if not set on message
		if (m.getJMSDeliveryMode() != DeliveryMode.NON_PERSISTENT) {
			m.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}
		// set default expiration if not set on the message
		if (m.getJMSExpiration() < 0) {
			m.setJMSExpiration(System.currentTimeMillis() + super.getDefaultTtlMs());
		}

		if (this.waiters.size() > 0) {
			synchronized (waiters) {
				for (Iterator<WaiterNotifier> it = waiters.iterator(); it.hasNext();) {
					WaiterNotifier waiter = it.next();
					if (waiter.matches(m)) {
						if (waiter.notify(m)) {
							it.remove();
							putDone = true;
							break;
						} else {
							// retry with
							retryWaiters(m);
							break;
						}
					}
				}
			}
		}
		boolean isReceiveToService = m.getBooleanProperty("com.att.aft.dme2.jms.isReceiveToService");
		logger.debug(null, "put", "Queue put: isReceiveToService=" + isReceiveToService);
		// handle queues with no listeners (actually queue it locally in memory
		// - UNLESS this is an inbound call to a service (requiring a listener))
		if (!putDone && super.getListeners().size() == 0 && this.isClient()) {
			if (m.getJMSMessageID() == null) {
				m.genID();
			}
			synchronized (queueData) {
				// first see if any waiters blocking for this
				logger.debug(null, "put", "Waiters size for queue {}|{}={}", this.getQueueName(), m.getJMSMessageID(), this.waiters.size());
				logger.debug(null, "put", "LocalQueue JMSMessage queueData added {}", m.getJMSMessageID());
				try {
					queueData.add(m);
				} catch (IllegalStateException e) {
					throw new DME2JMSServiceUnavailableException("AFT-DME2-5409", new ErrorContext()
							.add("queueName", this.getQueueName()).add("queueDepth", queueData.size() + ""), e);
				}
				putDone = true;
			}
		}

		// handle queues with listeners (dispatch to a listener on a thread)
		if (!putDone) {
			// Sync from here
			if (!isTempQueue && threadpool.getMaximumPoolSize() == 1 && super.getListeners().size() == 0) {
				throw new DME2JMSServiceUnavailableException("AFT-DME2-5401",
						new ErrorContext().add("queueName", this.getQueueName()));
			}
			MessageListenerHolder holder = null;
			try {

				holder = checkoutListener(m);
				// assign msg to holder itself if its of type JMSDefaultListener

				if (holder == null) {
					Exception e = new DME2JMSServiceUnavailableException("AFT-DME2-5402",
							new ErrorContext().add("queueName", this.getQueueName()));
//					logger.error( null, "put", "holder was null in {}", this.getQueueName(), e );
					throw e;
				}

				/* Set message on holder obj to be processed next. */
				if (!isTempQueue) {
					threadpool.submit(new MessageArrivalProcessor(this, m, holder));
				} else {
					tempQueueThreadpool.submit(new MessageArrivalProcessor(this, m, holder));
				}
				putDone = true;
			} catch (RejectedExecutionException e) {
				this.checkinHolder(holder);
				throw new DME2JMSServiceUnavailableException("AFT-DME2-5403",
						new ErrorContext().add("queueName", this.getQueueName()));
			} catch (Throwable th) {
				this.checkinHolder(holder);
				throw new DME2JMSServiceUnavailableException("AFT-DME2-5403",
						new ErrorContext().add("queueName", this.getQueueName()).add("errorOnListenerSubmit",
								"Exception on threadPool task submit"));
			} finally {
				if (!putDone) {
					this.checkinHolder(holder);
				}
			}
		}
		if (putDone) {
			// Ignore any error in collecting stats.
			// Should not interfere regular execution path
			try {
				if (m instanceof DME2JMSTextMessage && !this.isClient()) {
					DME2JMSTextMessage tm = (DME2JMSTextMessage) m;
					long msgSize = tm.getText().length();

					HashMap<String, Object> props = new HashMap<String, Object>();
					props.put(DME2Constants.MSG_SIZE, msgSize);
					props.put(DME2Constants.EVENT_TIME, System.currentTimeMillis());
					props.put(DME2Constants.CREATE_TIME, this.getCreateTime());
					props.put(DME2Constants.REQUEST_EVENT, true);
					props.put(DME2Constants.QUEUE_NAME, this.getQueueName());
					props.put(DME2Constants.DME2_INTERFACE_PROTOCOL, config.getProperty(DME2Constants.AFT_DME2_INTERFACE_JMS_PROTOCOL));
					if (m.getJMSCorrelationID() != null) {
						props.put(DME2Constants.MESSAGE_ID, m.getJMSCorrelationID());
					} else {
						props.put(DME2Constants.MESSAGE_ID, m.getJMSMessageID());
					}
					manager.getDME2Manager().postStatEvent(props);
				}
			} catch (Exception e) {
				//
				logger.debug(null, "put", "AFT-DME2-5407", new ErrorContext().add("requestQueue", this.getQueueName()),
						e);
			}
		}
		logger.debug(null, "put", LogMessage.METHOD_EXIT);
	}

	/**
	 * Create a listener associated with the queue // Add, remove, check needs
	 * to sync on same mutex.
	 */
	@Override
	public synchronized void addListener(DME2JMSMessageConsumer consumer, MessageListener listener, String filter)
			throws JMSException {
		logger.debug(null, "addListener", LogMessage.METHOD_ENTER);

		super.addListener(consumer, listener, filter);
		// reset threadpool size to match listener size
		if (super.getListeners().size() > 0 && !isTempQueue) {
			threadpool.setMaximumPoolSize(super.getListeners().size());
		}
		try {
			DME2Manager dm = manager.getDME2Manager();
			// If the queue is not registered with JMSServlet already
			// then create a new instance and bind it
			if (!registered && !this.isClient()) {
				
				logger.debug(null, "addListener", "Binding jms servlet to Queue={}", this.getQueueName());

				// Passing in the complete DME2 URI from client as such
				// to propagate the query params to registry API.
				DME2JMSServiceHolder serviceHolder = new DME2JMSServiceHolder(this);
				serviceHolder.setServiceURI(this.getQueueNameURI());
				serviceHolder.setServlet(new DME2JMSServlet(manager));
				serviceHolder.setSecurityRealm(this.getRealmName());
				serviceHolder.setAllowedRoles(getAllowedRoles());
				serviceHolder.setLoginMethod(getLoginMethod());
				serviceHolder.setManager(dm);
				dm.bindService(serviceHolder);
				registered = true;				
			}
		} catch (DME2Exception e) {
			throw new DME2JMSException("AFT-DME2-5403", new ErrorContext().add("queueName", this.getQueueName()), e);
		}
		if (listener != null) {
			logger.debug(null, "addListener", JMSLogMessage.QUEUE_ADDLISTENER, getQueueName(), listener.getClass(),
					filter);
		}
		logger.debug(null, "addListener", LogMessage.METHOD_EXIT);

	}

	public void addMessageToQueue(DME2JMSMessage m) {
		queueData.add(m);
	}

	/**
	 * Remove a listener associated with the Q
	 */
	@Override
	public synchronized void removeListener(DME2JMSMessageConsumer consumer) throws JMSException {
		logger.debug(null, "removeListener", LogMessage.METHOD_ENTER);
		if (consumer != null) {
			logger.info(null, "removeListener", JMSLogMessage.QUEUE_REMOVELISTNR, getQueueName(), consumer.getClass(),
					consumer.getMessageListener(), getListeners().size());
		}
		if (super.getListeners().size() > 1) { // if its 1, leave maxpoolsize=1
			// as 0 is not valid
			if (!isTempQueue) {
				threadpool.setMaximumPoolSize(super.getListeners().size() - 1);
			}
		} else {
			try {
				// only unbind the service listener if we have no more JMS
				// listeners to handle requests
				if (super.getListeners().size() <= 0 && !this.isClient()) {
					manager.getDME2Manager().unbindServiceListener(this.getQueueName());
					registered = false;
				}
			} catch (DME2Exception e) {
				throw new DME2JMSException("AFT-DME2-5404", new ErrorContext().add("queueName", this.getQueueName()),
						e);
			}
		}
		super.removeListener(consumer);
		logger.debug(null, "removeListener", LogMessage.METHOD_EXIT);

	}

	@Override
	public synchronized void removeListener(DME2JMSMessageConsumer consumer, MessageListener listener)
			throws JMSException {
		logger.debug(null, "removeListener", LogMessage.METHOD_ENTER);
		if (super.getListeners().size() > 1) { // if its 1, leave maxpoolsize=1
			// as 0 is not valid
			if (!isTempQueue) {
				threadpool.setMaximumPoolSize(super.getListeners().size() - 1);
			}
		} else {
			try {
				// only unbind the service listener if we have no more JMS
				// listeners to handle requests
				if (super.getListeners().size() <= 0 && !this.isClient()) {
					manager.getDME2Manager().unbindServiceListener(this.getQueueName());
				}
			} catch (DME2Exception e) {
				throw new DME2JMSException("AFT-DME2-5405", new ErrorContext().add("queueName", this.getQueueName()),
						e);
			}
		}
		super.removeListener(consumer, listener);
		logger.debug(null, "removeListener", LogMessage.METHOD_EXIT);
	}

	/**
	 * String representation of object
	 */
	@Override
	public String toString() {
		try {
			return "LocalQueue: " + this.getQueueName();
		} catch (JMSException e) {
			return "LocalQueue";
		}
	}

	private boolean retryWaiters(DME2JMSMessage m) throws DME2JMSServiceUnavailableException, JMSException {
		boolean putDone = false;
		for (int i = 0; i < maxPutRetry; i++) {
			synchronized (waiters) {
				for (Iterator<WaiterNotifier> it = waiters.iterator(); it.hasNext();) {
					WaiterNotifier waiter = it.next();
					if (waiter.matches(m)) {
						if (waiter.notify(m)) {
							it.remove();
							putDone = true;
							logger.info(null, "retryWaiters", "Code=Server.Retry.NoWaiters;RetryAttempt={}", i);
							break;
						}
					}
				}
			}
			if (putDone) {
				break;
			}
		}
		if (!putDone) {
			throw new DME2JMSServiceUnavailableException("AFT-DME2-5406",
					new ErrorContext().add("requestQueue", this.getQueueName()));
		}
		return putDone;
	}

	protected boolean isOpen() {
		return opened;
	}

	protected void close() {
		this.opened = false;
	}

	@Override
	public void setProperties(Properties props) {
		buildFromProperties(props);
	}

	@Override
	public Properties getProperties() {
		return null;
	}
	
	public void buildFromProperties(Properties props) {
		// TODO Auto-generated method stub

	}

	public void populateProperties(Properties props) {
		// TODO Auto-generated method stub

	}

	@Override
	public Reference getReference() throws NamingException {
		return DME2JNDIReferenceFactory.createReference(this.getClass().getName(), this);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		Properties props = (Properties) in.readObject();
		if (props != null) {
			setProperties(props);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(getProperties());

	}

	/**
	 * to checkin listener object that's checked out
	 */
	private void checkinHolder(MessageListenerHolder holder) {
		if (holder != null) {
			if (!holder.isReceiver()) {
				holder.checkin();
			}
		}
	}
}

/**
 * Instances of this class are used to provide notification for messages that
 * arrive which match a caller to get().
 */
class WaiterNotifier {
	private static final Logger logger = LoggerFactory.getLogger(WaiterNotifier.class.getName());
	private String filter = null;
	private DME2JMSMessage m = null;
	private DME2JMSLocalQueue queue = null;
	private boolean active = false;

	public WaiterNotifier(String filter, DME2JMSLocalQueue queue) {
		this.filter = filter;
		this.queue = queue;
	}

	public boolean matches(DME2JMSMessage m) throws JMSException {
		logger.debug(null, "matches", LogMessage.METHOD_ENTER, this.queue.getQueueName());
		boolean match = m.matches(filter);
		logger.debug(null, "matches", LogMessage.METHOD_EXIT);
		logger.debug(null, "matches", "DME2JMSLocalQueue matches filter={};match={}", filter, match);
		return match;
	}

	public synchronized boolean notify(DME2JMSMessage m) {
		boolean notified = false;
		try {
			try {
				logger.debug(null, "matches", LogMessage.METHOD_ENTER, this.queue.getQueueName());
			} catch (Exception e) {
				// ignore error in reading message id
				logger.debug(null, "matches", LogMessage.DEBUG_MESSAGE, "Exception", e);
			}
			this.m = m;
			try {
				logger.debug(null, "matches", "notifying ", m.getJMSMessageID(), " ReplyTo:", m.getJMSReplyTo());
			} catch (Exception e) {
				// ignore
				logger.debug(null, "matches", LogMessage.DEBUG_MESSAGE, "Exception", e);

			}
			if (active) {
				this.notify();
				notified = true;
			} else {
				return false;
			}
			return notified;
		} finally {
			logger.debug(null, "matches", LogMessage.METHOD_EXIT);
		}
	}

	public synchronized DME2JMSMessage get(long ttl) throws JMSException {
		String trackingId = null;
		active = true;
		try {
			logger.debug(null, "get", LogMessage.METHOD_ENTER, this.queue.getQueueName());
		} catch (Exception e) {
			// ignore error in reading message id
			logger.debug(null, "get", LogMessage.DEBUG_MESSAGE, "Exception", e);
		}
		if (m == null) {
			try {
				this.wait(ttl);
				if (m != null) {
					logger.debug(null, "get", "Wait ending {} ReplyTo: {}", m.getJMSMessageID(), m.getJMSReplyTo());
				}
			} catch (InterruptedException e) {
				logger.debug(null, "get", LogMessage.THREAD_INTERRUPT);
			}
		}
		active = false;

		if (m != null) {
			String queueName = this.queue.getQueueName();
			String msgId = m.getStringProperty("JMSMessageID");
			String conversationId = m.getStringProperty("JMSConversationID");
			trackingId = msgId + (conversationId == null ? "" : "(" + conversationId + ")");
			if (queueName != null) {
				DME2Constants.setContext(queueName + DME2Constants.LOGRECORDSEP + trackingId, null);
			} else {
				DME2Constants.setContext(trackingId, null);
			}
		}
		logger.debug(null, "get", LogMessage.METHOD_EXIT, this.queue.getQueueName());
		return m;
	}
}

/**
 * Runnable for handling allowing listeners to handle messages in a connection
 * pool
 */
class MessageArrivalProcessor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(MessageArrivalProcessor.class.getName());
	private final DME2JMSMessage m;
	private final MessageListenerHolder h;
	private boolean qdebug = false;

	public MessageArrivalProcessor(DME2JMSQueue q, DME2JMSMessage m, MessageListenerHolder holder) {
		logger.debug(null, "MessageArrivalProcessor", LogMessage.METHOD_ENTER);
		this.m = m;
		this.h = holder;
		if (q != null) {
			this.qdebug = q.isQdebug();
		}
		logger.debug(null, "MessageArrivalProcessor", LogMessage.METHOD_EXIT);
	}

	@Override
	public void run() {
		logger.debug(null, "run", LogMessage.METHOD_ENTER);
		try {
			logger.debug(null, "run", "MessageArrivalProcessor run {}", m.getJMSMessageID());
			if (this.qdebug) {
				logger.info(null, "run", JMSLogMessage.QUEUE_RUN, m.getJMSMessageID());
			}
			h.onMessage(m);
			// reset holder message to null
		} catch (Throwable e) { // NOSONAR
			logger.warn(null, "run", JMSLogMessage.QUEUE_RUN_EX, e);
		} finally {
			// For receiver type service provider, since the listener object
			// will be not re-used, checkin is avoided below to avoid
			// race conditions
			if (!h.isReceiver()) {
				if (m != null) {
					logger.debug(null, "run", "!h.isReceiver checkin invoked");
					if (this.qdebug) {
						logger.info(null, "run", JMSLogMessage.QUEUE_RUN_HOLDER, h);
					}
				}
				h.checkin();
			}
			logger.debug(null, "run", LogMessage.METHOD_EXIT);
		}
	}
}
