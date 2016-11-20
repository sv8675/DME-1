/*
 * Copyright 2011 AT&T Intellectual Properties, Inc.
 * Developed and maintained by the Common Services System Architecture (CSSA) Group
 */
package com.att.aft.dme2.server.jetty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * DME2ExecutorThreadPool extends ExecutorThreadPool in jetty api to override
 * the dispatch method.
 * 
 * @author sk6162
 * 
 */
public class DME2ExecutorThreadPool extends
		org.eclipse.jetty.util.thread.ExecutorThreadPool {

	/** The _executor. */
	private final ExecutorService _executor;

	/**
	 * Instantiates a new e http executor thread pool.
	 * 
	 * @param executor
	 *            the executor
	 */
	public DME2ExecutorThreadPool(ExecutorService executor) {
		_executor = executor;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jetty.util.thread.ExecutorThreadPool#dispatch(java.lang.Runnable
	 * )
	 */
	@Override
	public boolean dispatch(Runnable job) {
		try {
			_executor.execute(job);
			return true;
		} catch (RejectedExecutionException e) {
			throw e;
		}
	}
}
