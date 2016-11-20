/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.server;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

public class TestReceiveListener implements MessageListener {
	private Session session;
	private Message replyMessage;
	/** The waiter. */
	private byte[] waiter = new byte[0];
	
	public TestReceiveListener(Session session) {
		this.session = session;
	}

	@Override
	public void onMessage(Message m) {
			this.replyMessage = m;
			System.out.println("got reply=" + m);
			synchronized (waiter) {
				waiter.notify();
			}
	
	}
	
	public Message getResponse(long timeout) throws Exception {
		long start = System.currentTimeMillis();
		synchronized(waiter){
			if (replyMessage != null) {
				return replyMessage;
			}
			try {
				waiter.wait(timeout);
			} catch(InterruptedException ie) {
				long elapsed = System.currentTimeMillis() - start;
			
				if(elapsed < timeout) {
					waiter.wait(timeout-elapsed);
				}
			}
		}
		if (replyMessage != null) {
			return replyMessage;
		}
		throw new Exception("No reply returned");
	}
}

