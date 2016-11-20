/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.util.ErrorContext;

public class DME2JMSXAResource implements XAResource {

	private static long rID = 0;
	private int tranTimeout = 0;
	private static boolean firstTime = true;
	private static final Logger logger = LoggerFactory.getLogger(DME2JMSXAResource.class.getName());
	private long rid = rID++;

	public DME2JMSXAResource() {
		// if this is the first time this was created in the JVM, we want to
		// notify via log that DME2 doesn't really support XA transactions
		if (firstTime) {
			synchronized (DME2JMSXAResource.class) {
				if (firstTime) {
					firstTime = false;
					logger.info(null, "DME2JMSXAResource", "AFT-DME2-6400", new ErrorContext());
				}
			}
		}
	}

	@Override
	public void commit(Xid arg0, boolean arg1) throws XAException {
		return;
	}

	@Override
	public void end(Xid arg0, int arg1) throws XAException {
		return;
	}

	@Override
	public void forget(Xid arg0) throws XAException {
		return;
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return this.tranTimeout;
	}

	@Override
	public boolean isSameRM(XAResource arg0) throws XAException {
		if (!(arg0 instanceof DME2JMSXAResource)) {
			return false;
		}

		DME2JMSXAResource r1 = (DME2JMSXAResource) arg0;
		if (r1.rid == this.rid) {
			return true;
		}
		return false;
	}

	@Override
	public int prepare(Xid arg0) throws XAException {
		return 0;
	}

	@Override
	public Xid[] recover(int arg0) throws XAException {
		return new Xid[0];
	}

	@Override
	public void rollback(Xid arg0) throws XAException {
		return;
	}

	@Override
	public boolean setTransactionTimeout(int tranTimeout) throws XAException {
		this.tranTimeout = tranTimeout;
		return true;
	}

	@Override
	public void start(Xid arg0, int arg1) throws XAException {
		return;
	}

}
