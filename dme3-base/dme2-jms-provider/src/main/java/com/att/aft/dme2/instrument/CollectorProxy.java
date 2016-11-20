/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.instrument;

/**
 * singleton wrapper for SampleCollector
 * 
 * can change SampleCollector, but need to make sure all callers use the same
 * one so changes in filters and combining intervals is consistent.
 * 
 * mostly non-blocking and mostly thread-safe, it is possible that one thread
 * gets an old collector after another thread has called reset()
 */
public class CollectorProxy {
//	private static final Logger logger = LoggerFactory.getLogger(CollectorProxy.class.getName());
//
//	public static final String PICCOLO_ENABLED_PROP = "PICCOLO_ENABLED";
//
//	public static final String PICCOLO_CONFIGFILE_PROP = "PICCOLO_CONFIG";
//	public static final String[] PICCOLO_CONFIGFILE_DEFAULTS = { "piccolo.properties", "bootstrap.properties",
//			"aft.properties" };
//
//	public static final CollectorProxy INSTANCE = new CollectorProxy();
//
//	private SampleCollector delegate;
//
//	private CollectorProxy() {
//		try {
//			reset(false);
//		} catch (RuntimeException e) {
//			// avoid loop between metrics reporting a message and piccolo
//			// recording stats for messages
//			logger.error(null, "CollectorProxy", "failed to initialize piccolo collector", e);
//		}
//	}
//
//	public SampleCollector getCollector() {
//		return delegate;
//	}
//
//	public void disable() {
//		setCollector(null, false);
//	}
//
//	public void reset(boolean drainQueue) {
//		setCollector(buildCollector(), drainQueue);
//	}
//
//	public void setCollector(SampleCollector newConnector, boolean drainQueue) {
//		final SampleCollector oldCollector;
//
//		synchronized (this) {
//			oldCollector = delegate;
//			delegate = newConnector;
//		}
//
//		if (oldCollector == null) {
//			return;
//		}
//		try {
//			oldCollector.shutdown(drainQueue);
//		} catch (InterruptedException e) {
//			final int queueSize = oldCollector.getQueueSize();
//			if (queueSize > 0) {
//				logger.warn(null, "setCollector", LogMessage.THREAD_INTERRUPT,
//						"shutting down old collector, queue was not empty");
//			} else {
//				logger.warn(null, "setCollector", LogMessage.THREAD_INTERRUPT, "shutting down old collector");
//			}
//		}
//	}
//
//	private static SampleCollector buildCollector() {
//		final Boolean enabled = Boolean.valueOf(Configuration.getInstance().getProperty(PICCOLO_ENABLED_PROP, "false"));
//		if (!enabled) {
//			return null;
//		}
//
//		// use property to identify config file, or default filenames
//		final String propFilename = Configuration.getInstance().getProperty(PICCOLO_CONFIGFILE_PROP);
//		final String[] filenames = propFilename == null ? PICCOLO_CONFIGFILE_DEFAULTS : new String[] { propFilename };
//
//		for (String filename : filenames) {
//			// try filename as a file or resource
//			try {
//				return buildCollector(filename);
//			} catch (Exception e) {
//				logger.info(null, "buildCollector", LogMessage.COLLECTOR_BUILD_PATH_FAIL, filename, e);
//			}
//
//			// if it was relative,
//			// try again as absolute path to file or resource
//			if (!filename.startsWith("/")) {
//				try {
//					return buildCollector("/" + filename);
//				} catch (Exception e) {
//					logger.error(null, "buildCollector", LogMessage.COLLECTOR_BUILD_PATH_FAIL, "/" + filename, e);
//				}
//			}
//		}
//
//		throw new IllegalStateException("failed to build sample collector");
//	}
//
//	/**
//	 * given a filename, build a collector with properties file. try to find the
//	 * file (in this order): as an absolute filesystem path; or as a resource in
//	 * this package; or as a resource in the root package
//	 */
//	private static SampleCollector buildCollector(String filename) throws IOException {
//		InputStream in = null;
//		final File file = new File(filename);
//		if (file.isAbsolute() && file.isFile() && file.canRead()) {
//			in = new FileInputStream(filename);
//			return buildCollectorAndClose(in);
//		}
//
//		in = EventSampler.class.getClassLoader().getResourceAsStream(filename);
//		if (in != null) {
//			return buildCollectorAndClose(in);
//		}
//
//		in = ClassLoader.getSystemResourceAsStream(filename);
//		if (in != null) {
//			return buildCollectorAndClose(in);
//		}
//
//		throw new IllegalStateException("no properties found to build piccolo collector");
//	}
//
//	private static SampleCollector buildCollectorAndClose(final InputStream newIn) throws IOException {
//		InputStream in = newIn;
//		try {
//			if (!(in instanceof BufferedInputStream)) {
//				in = new BufferedInputStream(in);
//			}
//			final Properties props = new Properties();
//			props.load(in);
//			return new CollectorBuilder(props).build();
//		} finally {
//			in.close();
//		}
//	}
//
//	public String describe() {
//		final SampleCollector copy = delegate;
//		if (copy == null) {
//			return "statistics collection has been disabled";
//		}
//
//		final SimpleDateFormat formatter = getDateFormatter();
//		final StringBuilder out = new StringBuilder();
//		final Set<WindowDescription> windows = copy.getWindows();
//
//		out.append("collecting statistics over ").append(windows.size()).append(" windows:");
//		for (WindowDescription window : windows) {
//			final String start = window.start == null ? "" : formatter.format(window.start);
//			final String end = window.end == null ? "" : formatter.format(window.end);
//			out.append(String.format("\n%s: %d samples in %d intervals of %d ms: %s - %s", window.name,
//					window.sampleCount, window.intervalCount, window.duration, start, end));
//		}
//		out.append("\nqueue size: ").append(copy.getQueueSize());
//		return out.toString();
//	}
//
//	public String describeStatistics(String window) {
//		final SampleCollector copy = delegate;
//		if (copy == null) {
//			return "statistics collection has been disabled";
//		}
//		return "types found in window '" + window + "' and count of distinct values for each detail:\n"
//				+ copy.describeStatistics(window);
//	}
//
//	public String getCategoryStatistics(String window, String query) {
//		final SampleCollector copy = delegate;
//		if (copy == null) {
//			return "statistics collection has been disabled";
//		}
//
//		// get stats and translate to strings
//		final Map<SampleType, Statistics> stats = copy.getCategoryStatistics(window, Query.fromString(query));
//		if (stats.isEmpty()) {
//			return "no samples were found";
//		}
//		final Map<String, String> sortedStats = new TreeMap<String, String>(); // display
//																				// in
//																				// category
//																				// order
//		for (Map.Entry<SampleType, Statistics> entry : stats.entrySet()) {
//			final String details = entry.getKey().getDetails().toString();
//			final String key = details.substring(1, details.length() - 1);
//			sortedStats.put(key, entry.getValue().toString());
//		}
//		return toString(sortedStats);
//	}
//
//	public String getSeriesStatistics(String window, String query) {
//		final SampleCollector copy = delegate;
//		if (copy == null) {
//			return "statistics collection has been disabled";
//		}
//
//		final SimpleDateFormat formatter = getDateFormatter();
//
//		// get stats and translate to strings
//		int skippedIntervals = 0;
//		final Map<Date, Statistics> stats = copy.getSeriesStatistics(window, Query.fromString(query));
//		if (stats.isEmpty()) {
//			return "no samples were found";
//		}
//		final Map<String, String> sortedStats = new TreeMap<String, String>(); // display
//																				// in
//																				// category
//																				// order
//		for (Map.Entry<Date, Statistics> entry : stats.entrySet()) {
//			final String key = formatter.format(entry.getKey());
//			final Statistics value = entry.getValue();
//			if (value != null) {
//				sortedStats.put(key, value.toString());
//			} else {
//				skippedIntervals++;
//			}
//		}
//		final String description = toString(sortedStats);
//		if (skippedIntervals > 0) {
//			return description + "\n" + skippedIntervals + " intervals with no matching samples were not displayed";
//		} else {
//			return description;
//		}
//	}
//
//	private SimpleDateFormat getDateFormatter() {
//		final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		formatter.setTimeZone(TimeZone.getDefault());
//		return formatter;
//	}
//
//	private String toString(final Map<String, String> sortedStats) {
//		final StringBuilder out = new StringBuilder();
//		for (Map.Entry<String, String> entry : sortedStats.entrySet()) {
//			if (out.length() > 0) {
//				out.append("\n");
//			}
//			out.append(entry.getKey()).append("\t").append(entry.getValue());
//		}
//		return out.toString();
//	}

	/////////////////////////////////////////////////////////////////////////////////////
}
