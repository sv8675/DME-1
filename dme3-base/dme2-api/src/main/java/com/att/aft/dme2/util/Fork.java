package com.att.aft.dme2.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Fork {

	private static ConcurrentHashMap<String,ExecutorService> fixedThreadExecutorPools = new ConcurrentHashMap<>(  );
	private static ConcurrentHashMap<String,ScheduledExecutorService> scheduledFixedThreadExecutorPools = new ConcurrentHashMap<>(  );

	public Fork() {
		// TODO Auto-generated constructor stub
	}
	
	public synchronized static ExecutorService createFixedThreadExecutorPool(final String name, final Integer maxThreadCount){
		if ( !fixedThreadExecutorPools.containsKey( name )) {
			fixedThreadExecutorPools.put( name, Executors.newFixedThreadPool(maxThreadCount, new DefaultForkFactory(name)) );
		}
		return fixedThreadExecutorPools.get( name );
	}
	public synchronized static ScheduledExecutorService createScheduledFixedThreadExecutorPool(final String name, final Integer maxThreadCount){
		if ( !scheduledFixedThreadExecutorPools.containsKey( name ) ) {
			scheduledFixedThreadExecutorPools
					.put( name, Executors.newScheduledThreadPool( maxThreadCount, new DefaultForkFactory( name ) ) );
		}
		return scheduledFixedThreadExecutorPools.get( name );
	}

	static class DefaultForkFactory implements ThreadFactory{
	    private final ThreadGroup group;
	    private final AtomicInteger threadNumber = new AtomicInteger(1);
	    private final String namePrefix;
	
		public DefaultForkFactory(String name) {
	        SecurityManager s = System.getSecurityManager();
	        group = (s != null) ? s.getThreadGroup() :
	                              Thread.currentThread().getThreadGroup();
	        namePrefix = name  + ":";
		}
	
		@Override
	    public Thread newThread(Runnable r) {
	        Thread t = new Thread(group, r,
	                              namePrefix+threadNumber.getAndIncrement(),
	                              0);
	        if (t.isDaemon())
	            t.setDaemon(false);
	        if (t.getPriority() != Thread.NORM_PRIORITY)
	            t.setPriority(Thread.NORM_PRIORITY);
	        return t;
	    }
	}	

}
