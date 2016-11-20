/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.JMSException;

public class DME2JMSErrorMessage extends DME2JMSTextMessage {
	private JMSException e;
	private boolean fastFailNull = false;

	public DME2JMSErrorMessage(JMSException e) {
		this.e = e;
	}

	public DME2JMSErrorMessage(JMSException e, boolean fastFailNull) {
		this.e = e;
		this.fastFailNull = fastFailNull;
	}

	public JMSException getJMSException() {
		return e;
	}

	public boolean isFastFailNull() {
		return fastFailNull;
	}
}
