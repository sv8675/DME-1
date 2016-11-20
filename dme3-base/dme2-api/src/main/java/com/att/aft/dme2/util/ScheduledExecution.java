package com.att.aft.dme2.util;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class ScheduledExecution {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledExecution.class.getName());
	
	public ScheduledExecution() {
		// TODO Auto-generated constructor stub
	}
	public static  ScheduledFuture<?> schedule(final String name, final Integer maxThreadCount, final Runnable command, final long delay){
		LOGGER.debug(null, "schedule", "name=[{}];maxThreadCount=[{}];command=[{}];delay=[{}]",name,maxThreadCount,command,delay);
		return Fork.createScheduledFixedThreadExecutorPool(name, maxThreadCount).scheduleAtFixedRate(command, delay, delay, TimeUnit.MILLISECONDS);
	}
}
