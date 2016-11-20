/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.instrument;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import com.att.aft.dme2.jms.DME2JMSMessage;
import com.att.aft.dme2.jms.DME2JMSServlet;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;


@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class JMSEventSampler extends EventSampler {
	// minimize sampler noise by only reporting failure the first time
	private static boolean notYetReportedError = true;

	private static final Logger logger = LoggerFactory.getLogger(JMSEventSampler.class.getName());

	private static JMSEventSampler iNSTANCE = new JMSEventSampler();

	public static final String SUCCESS_STEP = "success";
	public static final String FAILED_STEP = "failure";

	public static final String ENDPOINT_MSGPROP = "X-EVENTSAMPLER-ENDPOINT";
	public static final String RECEIVETIME_MSGPROP = "X-EVENTSAMPLER-RECEIVETIME";

	private Map<String, String> getDetails(DME2JMSMessage m) throws JMSException {
//		final String sender = m.getStringProperty(NetworkCrawler.JMX_HOSTNAME_MSGPROP);
		final String endpoint = m.getStringProperty(ENDPOINT_MSGPROP);
		String partner = m.getStringProperty("DME2_REQUEST_PARTNER");
		if (partner == null) {
			partner = m.getStringProperty("DME2_JMS_REQUEST_PARTNER");
		}

		final Map<String, String> details = new HashMap<String, String>();
		// put(details, "client", sender);
//		put(details, "endpoint", endpoint);
//		put(details, "partner", partner);

		return details;
	}

	private final String MSGHTTPTIME = DME2JMSServlet.class.getCanonicalName() + ":http-time";
	private final String MSGQUEUETIME = DME2JMSServlet.class.getCanonicalName() + ":queue-time";
	private final String MSGHANDLETIME = DME2JMSServlet.class.getCanonicalName() + ":service-time";
//
//	public void reportMessageHandling(DME2JMSMessage m, Timer timer) {
//		final SampleCollector c = getCollector();
//		if (c == null) {
//			return;
//		}
//
//		try {
//			final Date now = new Date();
//			final long createTime = m.getJMSTimestamp();
//			final long receiveTime = m.getLongProperty(RECEIVETIME_MSGPROP);
//
//			final long totalElapsed = now.getTime() - createTime;
//			final long serviceElapsed = timer.getTotal();
//			final long httpElapsed = receiveTime - createTime; // plus/minus
//																// clock
//																// differences
//			final long queueElapsed = totalElapsed - serviceElapsed - httpElapsed;
//
//			final Map<String, String> details = getDetails(m);
//
//			final List<Sample> samples = new ArrayList<Sample>();
//			samples.add(new ValueSample(now, new SampleType(MSGHTTPTIME, details), httpElapsed));
//			samples.add(new ValueSample(now, new SampleType(MSGQUEUETIME, details), queueElapsed));
//
//			final String outcome = timer.getSteps().get(0).id;
//			final Map<String, String> copy = new HashMap<String, String>(details);
//			put(copy, "outcome", outcome);
//			samples.add(new ValueSample(now, new SampleType(MSGHANDLETIME, copy), serviceElapsed));
//
//			c.add(samples);
//		} catch (Throwable t) {
//			if (notYetReportedError) {
//				logger.error(null, "reportMessageHandling", "failed to report piccolo statistics", t);
//				notYetReportedError = false;
//			}
//		}
//	}

	public static JMSEventSampler getINSTANCE() {
		return iNSTANCE;
	}

	public static void setINSTANCE(final JMSEventSampler newINSTANCE) {
		iNSTANCE = newINSTANCE;
	}
}
