package com.att.aft.dme2.cache.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Fork {

	public Fork() {
		// TODO Auto-generated constructor stub
	}
	
	public static ExecutorService createFixedThreadExecutorPool(final String name, final Integer maxThreadCount){
		return Executors.newFixedThreadPool(maxThreadCount, new DefaultForkFactory(name));
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
