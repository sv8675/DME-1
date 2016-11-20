/*
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * The Class ProcessStreamReader.
 */
public class ProcessStreamReader {

	/*
	 * Inner class to read process input stream...
	 */
	/**
	 * The Class StreamReader.
	 */
	class StreamReader implements Runnable {

		/** The CLAS s_ name. */
		private final String CLASS_NAME = StreamReader.class.getName();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			String line = null;

			try {
				while ((line = reader.readLine()) != null) {
					// logger.logp(Level.INFO, CLASS_NAME, "run", "Stream Type="
					// + streamType + ", line=" + line);
					buffer.append(line);
					buffer.append("\r\n");
					// System.out.println(line);
					line = null;
				}
			} catch (Exception e) {
				if(!e.getMessage().contains("Bad file descriptor"))
					throw new RuntimeException(e);
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}// end of run.
	}// end of StreamReader inner class...

	/** The Constant CLASS_NAME. */
	private static final String CLASS_NAME = ProcessStreamReader.class
			.getName();

	/** The buffer. */
	private StringBuffer buffer = new StringBuffer();

	/** The stream. */
	private InputStream stream;

	/** The stream type. */
	private String streamType;

	/**
	 * Instantiates a new process stream reader.
	 */
	public ProcessStreamReader() {

	}

	/**
	 * Instantiates a new process stream reader.
	 * 
	 * @param streamType
	 *            the stream type
	 * @param stream
	 *            the stream
	 */
	public ProcessStreamReader(String streamType, InputStream stream) {
		this.streamType = streamType;
		this.stream = stream;
	}

	/**
	 * Gets the input stream.
	 * 
	 * @return the input stream
	 */
	public InputStream getInputStream() {
		return this.stream;
	}

	/**
	 * Gets the stream contents.
	 * 
	 * @return the stream contents
	 */
	public String getStreamContents() {
		return buffer.toString();
	}

	/**
	 * Gets the stream type.
	 * 
	 * @return the stream type
	 */
	public String getStreamType() {
		return this.streamType;
	}

	/**
	 * Sets the input stream.
	 * 
	 * @param stream
	 *            the new input stream
	 */
	public void setInputStream(InputStream stream) {
		this.stream = stream;
	}

	/**
	 * Sets the stream type.
	 * 
	 * @param streamType
	 *            the new stream type
	 */
	public void setStreamType(String streamType) {
		this.streamType = streamType;
	}

	/**
	 * Start processing.
	 * 
	 * @return true, if successful
	 */
	public boolean startProcessing() {
		Thread t = new Thread(new StreamReader());
		t.start();
		return true;
	}
}
