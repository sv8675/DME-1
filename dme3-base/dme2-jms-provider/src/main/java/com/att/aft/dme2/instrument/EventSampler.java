/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.instrument;

@SuppressWarnings("PMD.AvoidCatchingThrowable")
public class EventSampler // implements JMX controls
{
	// minimize sampler noise by only reporting failure the first time
//	private static boolean notYetReportedError = true;
//	private static final Logger logger = LoggerFactory.getLogger(EventSampler.class.getName());
//
//	public static final String SEND_ROUTEOFFER_STEP_PREFIX = "offer:";
//	public static final String SEND_URL_STEP_PREFIX = "send:";
//	public static final String FAILED_URL_STEP_PREFIX = "fail:";
//	public static final String SUCCESS_URL_STEP = "success";
//	public static final String TRIED_URL_STEP = "tried";
//
//	public static EventSampler iNSTANCE = new EventSampler();
//
//	protected SampleCollector getCollector() {
//		return CollectorProxy.INSTANCE.getCollector();
//	}
//
//	/////////////////////////////////////////////////////////////////////////////////////////
//
//	private final String cOUNTREQUESTTYPE = DME2Client.class.getCanonicalName() + ":requests";
//	private final String oFFERCOUNTTYPE = DME2Client.class.getCanonicalName() + ":offer-count";
//	private final String eNDPOINTSCOUNTTYPE = DME2Client.class.getCanonicalName() + ":endpoint-count";
//	private final String eXECUTESTEPTIME = DME2Client.class.getCanonicalName() + ":send-step-time";
//	private final String eXECUTESENDTIME = DME2Client.class.getCanonicalName() + ":send-time";
//
//	private final String sENDFAILTIME = DME2Exchange.class.getCanonicalName() + ":send-fail-time";
//	private final String sENDFAILCOUNT = DME2Exchange.class.getCanonicalName() + ":send-exceptions";
//	private final String sENDRETRYTIME = DME2Exchange.class.getCanonicalName() + ":send-wait-time";
//	private final String sENDRETRYCOUNT = DME2Exchange.class.getCanonicalName() + ":send-retries";
//	private final String sENDSERVERTIME = DME2Exchange.class.getCanonicalName() + ":server-time";
//	private final String sENDTOTALTIME = DME2Exchange.class.getCanonicalName() + ":total-time";
//	private final String sENDTOTALCOUNT = DME2Exchange.class.getCanonicalName() + ":total-count";
//	private final String sENDURLFAILED = DME2Exchange.class.getCanonicalName() + ":url-fail-count";
//	private final String sENDURLTRIED = DME2Exchange.class.getCanonicalName() + ":url-tried-count";
//
//	private static final String fAILMSG = "failed to report piccolo statistics";
//	private static final String uRL = "url";
//
//	protected static void put(Map<String, String> map, String key, String value) {
//		if (value != null) {
//			map.put(key, value);
//		}
//	}
//
//	private Map<String, String> getDetails(DME2UniformResource uniformResource) {
//		final Map<String, String> details = new HashMap<String, String>();
//		put(details, "type", uniformResource.getUrlType().name());
//		put(details, "service", uniformResource.getService());
//		put(details, "version", uniformResource.getVersion());
//		put(details, "partner", uniformResource.getPartner());
//		put(details, "envcontext", uniformResource.getEnvContext());
//		put(details, "datacontext", uniformResource.getDataContext());
//		put(details, "routeoffer", uniformResource.getRouteOffer());
//		put(details, "bindcontext", uniformResource.getBindContext());
//		put(details, "subcontext", uniformResource.getSubContext());
//		put(details, "stickyselectorkey", uniformResource.getStickySelectorKey());
//		put(details, "user", uniformResource.getUserName());
//		return Collections.unmodifiableMap(details);
//	}
//
//	public void reportClientRequest(DME2UniformResource uniformResource) {
//		try {
//			// copy reference in case collector is changed in another thread
//			final SampleCollector c = getCollector();
//			if (c == null) {
//				return;
//			}
//
//			final SampleType type = new SampleType(cOUNTREQUESTTYPE, getDetails(uniformResource));
//			final Sample sample = new Sample(new Date(), type);
//			c.add(sample);
//		} catch (Throwable t) {
//			if (notYetReportedError) {
//				logger.error(null, "reportClientRequest", fAILMSG, t);				
//				notYetReportedError = false;
//			}
//
//		}
//	}
//
//	public void reportClientExecute(Timer timer, DME2UniformResource uniformResource, List<?> offers,
//			DME2Endpoint[] endpoints) {
//		try {
//			final SampleCollector c = getCollector();
//			if (c == null) {
//				return;
//			}
//
//			final Date now = new Date();
//			final Map<String, String> details = getDetails(uniformResource);
//			final List<Sample> samples = new ArrayList<Sample>();
//			if (offers != null) {
//				samples.add(new ValueSample(now, new SampleType(oFFERCOUNTTYPE, details), offers.size()));
//			}
//			if (endpoints != null) {
//				samples.add(new ValueSample(now, new SampleType(eNDPOINTSCOUNTTYPE, details), endpoints.length));
//			}
//			for (Step step : timer.getSteps()) {
//				final Map<String, String> copy = new HashMap<String, String>(details);
//				copy.put("step", step.id);
//				samples.add(new ValueSample(now, new SampleType(eXECUTESTEPTIME, copy), step.elapsed));
//			}
//			samples.add(new ValueSample(now, new SampleType(eXECUTESENDTIME, details), timer.getTotal()));
//			c.add(samples);
//		} catch (Throwable t) {
//			if (notYetReportedError) {
//				logger.error(null, "reportClientExecute", fAILMSG, t);
//				notYetReportedError = false;
//			}
//
//		}
//	}
//
//	public void reportExchangeFailure(DME2UniformResource uniformResource, Timer timer) {
//		try {
//			final SampleCollector c = getCollector();
//			if (c == null) {
//				return;
//			}
//
//			final Map<String, String> details = getDetails(uniformResource);
//			final List<Sample> samples = new ArrayList<Sample>();
//			addExchangeCompleteSamples(samples, uniformResource, timer, details);
//			c.add(samples);
//		} catch (Throwable t) {
//			if (notYetReportedError) {
//				logger.error(null, "reportExchangeFailure", fAILMSG, t);
//				notYetReportedError = false;
//			}
//
//		}
//	}
//
//	public void reportExchangeSuccess(int code, String url, DME2UniformResource uniformResource, Timer timer) {
//		try {
//			final SampleCollector c = getCollector();
//			if (c == null) {
//				return;
//			}
//
//			final List<Step> steps = timer.getSteps();
//			final Step lastStep = steps.get(steps.size() - 1);
//
//			final Map<String, String> details = getDetails(uniformResource);
//			final List<Sample> samples = new ArrayList<Sample>();
//			addExchangeCompleteSamples(samples, uniformResource, timer, details);
//
//			// one additional sample to report success, track URL
//			final Map<String, String> copy = new HashMap<String, String>(details); // don't
//																					// change
//																					// reference
//																					// in
//																					// other
//																					// samples
//			put(copy, uRL, url);
//			put(copy, "http-response", Integer.toString(code));
//			samples.add(new ValueSample(new Date(), new SampleType(sENDSERVERTIME, copy), lastStep.elapsed));
//
//			c.add(samples);
//		} catch (Throwable t) {
//			if (notYetReportedError) {
//				logger.error(null, "reportExchangeSuccess", fAILMSG, t);
//				notYetReportedError = false;
//			}
//
//		}
//	}
//
//	private void addExchangeCompleteSamples(Collection<Sample> samples, DME2UniformResource uniformResource,
//			Timer timer, Map<String, String> details) {
//		final Date now = new Date();
//		long failTime = 0, retryTime = 0;
//		int failCount = 0, retryCount = 0, sendCount = 0;
//		String url = null;
//		for (Step step : timer.getSteps()) {
//			final String id = step.id;
//			if (id.equals(SEND_URL_STEP_PREFIX)) {
//				url = step.details.get(uRL);
//			} else if (id.equals(FAILED_URL_STEP_PREFIX)) {
//				failTime += step.elapsed;
//				failCount++;
//
//				final String exception = step.details.get("exception");
//
//				final Map<String, String> copy = new HashMap<String, String>(details);
//				put(copy, uRL, url);
//				put(copy, "exception", exception);
//				samples.add(new ValueSample(now, new SampleType(sENDURLFAILED, copy), step.elapsed));
//			} else if (TRIED_URL_STEP.equals(id)) {
//				retryTime += step.elapsed;
//				retryCount++;
//
//				final Map<String, String> copy = new HashMap<String, String>(details);
//				put(copy, uRL, url);
//				samples.add(new ValueSample(now, new SampleType(sENDURLTRIED, copy), step.elapsed));
//			} else if (SUCCESS_URL_STEP.equals(id)) {
//				sendCount++;
//			}
//		}
//
//		samples.add(new ValueSample(now, new SampleType(sENDFAILTIME, details), failTime));
//		samples.add(new ValueSample(now, new SampleType(sENDFAILCOUNT, details), failCount));
//		samples.add(new ValueSample(now, new SampleType(sENDRETRYTIME, details), retryTime));
//		samples.add(new ValueSample(now, new SampleType(sENDRETRYCOUNT, details), retryCount));
//		samples.add(new ValueSample(now, new SampleType(sENDTOTALCOUNT, details), sendCount));
//		samples.add(new ValueSample(now, new SampleType(sENDTOTALTIME, details), timer.getTotal()));
//	}
//
//	// public void reportMessage(Logger msgLogger, Level level, LogMessage msg,
//	// MessageHandlingConfi cfg)
//	// {
//	// try
//	// {
//	// final SampleCollector c = getCollector();
//	// if(c==null){
//	// return;
//	// }
//	//
//	// final Map<String,String> details = new HashMap<String,String>(4);
//	// details.put("logger", msgLogger.getName());
//	// details.put("level", level.getName());
//	// details.put("code", msg.getCode());
//	// details.put("loggable", Boolean.toString(msgLogger.isLoggable(level)));
//	// if(cfg!=null)
//	// {
//	// details.put("logger-enabled", Boolean.toString(cfg.isUseLogger()));
//	// details.put("stdout-enabled", Boolean.toString(cfg.isUseStdout()));
//	// details.put("stderr-enabled", Boolean.toString(cfg.isUseStderr()));
//	// details.put("collector-enabled", Boolean.toString( !
//	// cfg.getCollectorConfig().equals(LogMessageConfig.NONE)));
//	// }
//	//
//	// Sample sample = new Sample(new Date(), new
//	// SampleType("com.att.aft.dme2.api.util.LogUtil:message", details));
//	// c.add(sample);
//	// }
//	// catch(Throwable t)
//	// {
//	// if(notYetReportedError)
//	// {
//	// logger.log(Level.WARNING, fAILMSG, t);
//	// notYetReportedError = false;
//	// }
//	//
//	// }
//	// }
}
