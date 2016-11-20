/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

public class CleanupScheduler
{
	private Timer timer;
	private final Hashtable<Runnable, TimerTask> timerTasks = new Hashtable<Runnable, TimerTask>();

	public CleanupScheduler(String name)
	{
		this.timer = new Timer(name, true);
	}

	public synchronized void schedulePeriodically(final Runnable task, long period)
	{
		TimerTask timerTask = new CleanupTimerTask(task);
		timer.schedule(timerTask, period, period);
		timerTasks.put(task, timerTask);
	}
}
