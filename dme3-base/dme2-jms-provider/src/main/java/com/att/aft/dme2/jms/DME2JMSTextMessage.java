/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.util.Enumeration;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.TextMessage;

public class DME2JMSTextMessage extends DME2JMSMessage implements TextMessage {

	private String text = null;
	
	@Override
	public String getText() throws JMSException {
		return text;
	}

	@Override
	public void setText(String text) throws JMSException {
		this.text = text;
	}
	
	@Override
	public void clearBody() throws JMSException {
		text = null;
	}
	
	@Override
	public String toString() {
		try {
			return "HttpJMSTextMessage: JMSMessageID=" + this.getJMSMessageID() + "; JMSCorrelationID=" + this.getJMSCorrelationID();
		} catch (JMSException e) {
			return super.toString();
		}
	}
	
	public DME2JMSTextMessage copy() throws JMSException{
		
		DME2JMSTextMessage copy = new DME2JMSTextMessage();
		Properties properties = this.getProperties();
		Enumeration<?> propertyNames = this.getPropertyNames();
		while (propertyNames.hasMoreElements()) {
			 String name = propertyNames.nextElement().toString();
			 String value = properties.getProperty(name);
			 copy.setStringProperty(name, value);
		}
		copy.setText(this.getText());
		if(this.getJMSCorrelationID()!= null){
			copy.setJMSCorrelationID(this.getJMSCorrelationID());
		}
		if(this.getJMSCorrelationIDAsBytes() != null){
			copy.setJMSCorrelationIDAsBytes(this.getJMSCorrelationIDAsBytes());
		}
		
		copy.setJMSDeliveryMode(this.getJMSDeliveryMode());
		if(this.getJMSDestination() != null){
			copy.setJMSDestination(this.getJMSDestination());
		}

		copy.setJMSExpiration(this.getJMSExpiration());
		if(this.getJMSMessageID() != null){
			copy.setJMSMessageID(this.getJMSMessageID());
		}
		
		copy.setJMSPriority(this.getJMSPriority());
		copy.setJMSRedelivered(this.getJMSRedelivered());
		if(this.getJMSReplyTo() != null){
			copy.setJMSReplyTo(this.getJMSReplyTo());
		}
		
		copy.setJMSTimestamp(this.getJMSTimestamp());
		if(this.getJMSType() != null){
			copy.setJMSType(this.getJMSType());
		}
		return copy;
	}

}
