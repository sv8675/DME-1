/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms.samples;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

@SuppressWarnings("PMD.SystemPrintln")
public class TestMessageListener implements MessageListener {
	
	private Session session;
	
	public TestMessageListener(Session session) {
		this.session = session;
	}

	@Override
	public void onMessage(Message m) {
		try {
			System.out.println("Got message: " + m);
			Destination d = m.getJMSReplyTo();
			if (d != null) {
				System.out.println("ReplyTo: " + d);
				MessageProducer sender = session.createProducer((Queue) d);
				Message m2 = session.createTextMessage("this is my reply");
				m2.setJMSDestination(d);
				m2.setJMSCorrelationID(m.getJMSMessageID());
				sender.send(m2);
			}
			
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
