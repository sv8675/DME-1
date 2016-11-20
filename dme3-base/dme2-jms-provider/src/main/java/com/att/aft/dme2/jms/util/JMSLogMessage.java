/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import com.att.aft.dme2.logging.LogMessage;

public class JMSLogMessage extends LogMessage {
	protected JMSLogMessage(String code, String template) {
		super(code, template);
	}

	public static final JMSLogMessage QUICK_RECOW = new JMSLogMessage("DME2-JMS-0001",
			"AFTDME2.QUICK.RECOW - Processed OneWay MessageID={}, CorrelationID={}, ElapsedMs={}");
	public static final JMSLogMessage QUICK_RECRR = new JMSLogMessage("DME2-JMS-0002",
			"AFTDME2.QUICK.RECRR - Processed RequestReply MessageID={}, CorrelationID={}, ReplyTo={}, ElapsedMs={}");
	public static final JMSLogMessage QUICK_JMSEX = new JMSLogMessage("DME2-JMS-0003",
			"AFTDME2.QUICK.JMSEX - Error on Message MessageID={}, CorrelationID={}, ReplyTo={}, ElapsedMs={}");
	public static final JMSLogMessage CLIENT_TIMEOUT = new JMSLogMessage("DME2-JMS-0004",
			"AFTJMSCLT.TIMEOUT - [{}] TIMEOUT after waiting {} ms for RequestReply MessageID={}, ReplyTo={}");
	public static final JMSLogMessage CLIENT_DATA = new JMSLogMessage("DME2-JMS-0005",
			"AFTJMSCLT.DATA - Expecting back [{}], got [{}]");
	public static final JMSLogMessage CLIENT_SUCCESS = new JMSLogMessage("DME2-JMS-0006",
			"AFTJMSCLT.SUCCESS - [{}] SUCCESS, RESPONSE [{}], ELAPSED [{}], Request MessageID [{}], CorrelationID [{}], ReplyTo [{}]");
	public static final JMSLogMessage CLIENT_MISMATCH = new JMSLogMessage("DME2-JMS-0007",
			"AFTJMSCLT.MISMATCH - [{}] MISMATCH response after {} for RequestReply MessageID={}, CorrelationID={}, ReplyTo={}");
	public static final JMSLogMessage CLIENT_JMSEX_RCV = new JMSLogMessage("DME2-JMS-0008",
			"AFTJMSCLT.JMSEXCEPTION - [{}] JMSEXCEPTION on receive(): {}");
	public static final JMSLogMessage CLIENT_FATAL = new JMSLogMessage("DME2-JMS-0009",
			"AFTJMSCLT.FATAL - [{}] FATAL response {}");
	public static final JMSLogMessage CLIENT_RECOW = new JMSLogMessage("DME2-JMS-0010",
			"AFTJMSSVR.RECOW - [{}] Processed OneWay MessageID={}, CorrelationID={}");
	public static final JMSLogMessage CLIENT_RECRR = new JMSLogMessage("DME2-JMS-0011",
			"AFTJMSSVR.RECRR - [{}] Processed RequestReply MessageID={}, CorrelationID={}, ReplyTo={}");
	public static final JMSLogMessage CLIENT_EX = new JMSLogMessage("DME2-JMS-0012", "[{}] exception: {}");
	public static final JMSLogMessage QUEUE_CREATED = new JMSLogMessage("DME2-JMS-0013",
			"New DME2InitialContext created for DME2 manager [{}]");
	public static final JMSLogMessage QUEUE_ADDLISTENER = new JMSLogMessage("DME2-JMS-0014",
			"listener.add - Queue: {}; Class: {}; Filter: {}");
	public static final JMSLogMessage QUEUE_REMOVELISTNR = new JMSLogMessage("DME2-JMS-0015",
			"DME2JMSLocalQueue listener.remove - Queue:{}; Class:{};Listener:{};Listener size: {}");
	public static final JMSLogMessage QUEUE_RUN = new JMSLogMessage("DME2-JMS-0016", "MessageArrivalProcessor run {}");
	public static final JMSLogMessage QUEUE_RUN_EX = new JMSLogMessage("DME2-JMS-0017",
			"MessageListener uncaught exception occured: {}");
	public static final JMSLogMessage QUEUE_RUN_HOLDER = new JMSLogMessage("DME2-JMS-0018",
			"MessageArrivalProcessor.run - Holder {} checkin");
	public static final JMSLogMessage QUEUE_REMOVETMP = new JMSLogMessage("DME2-JMS-0019",
			"Code=Trace.DME2JMSManager.removeTemporaryQueue;Removing temporary queue {}");
	public static final JMSLogMessage QUEUE_CLOSETMP = new JMSLogMessage("DME2-JMS-0020",
			"Closing temporary queues for session {}");
	public static final JMSLogMessage QUEUE_ADDLISTENER2 = new JMSLogMessage("DME2-JMS-0021",
			"DME2JMSQueue addListener - Queue:{};Listener:{};Listeners size: {}");
	public static final JMSLogMessage QUEUE_REMOVE_SAME = new JMSLogMessage("DME2-JMS-0022",
			"DME2JMSQueue listener.remove(consumer,listener) - Queue:{}; Class:{};Listener:{} isSameListener true, checking in holder;Listener size: {}");
	public static final JMSLogMessage QUEUE_CHECKOUT = new JMSLogMessage("DME2-JMS-0023",
			"DME2JMSQueue checkoutListener(message) - Queue:{};ListenerHolder:{} isAvailable true, holder matches message filter; checking out holder;Listener size: {}");
	public static final JMSLogMessage QUEUE_SETLISTNRS = new JMSLogMessage("DME2-JMS-0024",
			"DME2JMSQueue setListeners(listeners) - Queue:{};Listeners:{};Listener size: {}");
	public static final JMSLogMessage QUEUE_MATCHES = new JMSLogMessage("DME2-JMS-0025",
			"MessageListenerHolder [{}] - matches filter={};match={};Listener={}");
	public static final JMSLogMessage QUEUE_ON_MESSAGE = new JMSLogMessage("DME2-JMS-0026",
			"MessageListenerHolder listener onMessage invoked on listener obj [{}]");
	public static final JMSLogMessage QUEUE_LISTEN_AVAIL = new JMSLogMessage("DME2-JMS-0027",
			"MessageListenerHolder isAvailable listener [{}];isAvailable: {}");
	public static final JMSLogMessage QUEUE_CHECKING_IN = new JMSLogMessage("DME2-JMS-0028",
			"MessageListenerHolder checkout listener [{}];checkedOut=true");
	public static final JMSLogMessage QUEUE_CHECKING_OUT = new JMSLogMessage("DME2-JMS-0029",
			"MessageListenerHolder checkin listener [{}];checkedOut=false");
	public static final JMSLogMessage QUEUE_INVOKE = new JMSLogMessage("DME2-JMS-0030",
			"Code=Trace.DME2JMSQueueReceiver.receive; QueueReceiver invoking queue {} get with message selector {}");
	public static final JMSLogMessage FACTORY_INIT = new JMSLogMessage("DME2-JMS-0031",
			"Initializing DME2JMSXAQueueConnectionFactory");
	public static final JMSLogMessage SESSION_CREATED = new JMSLogMessage("DME2-JMS-0032",
			"DME2JMSQueueSession created from connection [{}]");
	public static final JMSLogMessage SESSION_LISTENER = new JMSLogMessage("DME2-JMS-0033",
			"DME2JMSQueueSession.setListener() called with [{}]");
	public static final JMSLogMessage SESSION_NO_NEXTMSG = new JMSLogMessage("DME2-JMS-0034",
			"DME2JMSQueueSession.run called with no distinquished message set.");
	public static final JMSLogMessage SESSION_NO_LISTENER = new JMSLogMessage("DME2-JMS-0035",
			"DME2JMSQueueSession.run called with no explicit default listener set on the session.");
	public static final JMSLogMessage SERVLET_RETRY = new JMSLogMessage("DME2-JMS-0036",
			"Code=Trace.DME2JMSServlet.retryMessage;Retry count:{}: max {} for {} reply queue {}");
	public static final JMSLogMessage NETWORK_CLIENT_FAIL = new JMSLogMessage("DME2-JMS-0037",
			"failed to get client info from JMS message");
	public static final JMSLogMessage CONTINUATION_FAIL = new JMSLogMessage("DME2-JMS-0038",
			"Continuation {} received JMSxception for correlationID: {}");
	public static final JMSLogMessage INIT_CTX_FAIL = new JMSLogMessage("DME2-JMS-0039",
			"Code=Exception.DME2JMSRemoteQueue.initLoggingContext;LoggingContext Failed; Error={}");
	public static final JMSLogMessage CONTENTS = new JMSLogMessage("DME2-JMS-0040", "message contents: {}");
	public static final JMSLogMessage CHECKOUT_FAIL = new JMSLogMessage("DME2-JMS-0041",
			"Exception in checkoutListener");
	public static final JMSLogMessage MAX_RETRIES = new JMSLogMessage("DME2-JMS-0042",
			"Max retries attempted with no success, returning failover;Retry count: {}: max {} for {}, reply queue {}");
}
