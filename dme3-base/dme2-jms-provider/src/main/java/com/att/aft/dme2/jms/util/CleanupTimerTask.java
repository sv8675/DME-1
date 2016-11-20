/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.util;

import java.util.TimerTask;

public class CleanupTimerTask extends TimerTask
{
	private final Runnable task;
	
	public CleanupTimerTask(Runnable task)
	{
		this.task = task;
	}
	
	public void run()
	{
		this.task.run();
	}
}
