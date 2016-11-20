/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.api.http;

import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class DME2QueuedThreadPool extends QueuedThreadPool {

	private int counter = 1;

	/**
	 * Creates a new named thread for the thread pool
	 */
	@Override
	protected Thread newThread(Runnable runnable) {
		Thread t = new Thread(runnable);
		t.setName("DME2::ClientExchangeThread-" + counter++);
		t.setDaemon(true);
		return t;
	}

}
