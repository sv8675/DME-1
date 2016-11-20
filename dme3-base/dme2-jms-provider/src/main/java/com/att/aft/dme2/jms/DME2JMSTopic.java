/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSubscriber;


public class DME2JMSTopic implements Topic, TopicSubscriber, TopicPublisher, java.io.Serializable{

	private static final long serialVersionUID = 1L;

	public DME2JMSTopic() {
	}

	@Override
	public String getTopicName() throws JMSException {
		
		return "DME2 Dummy Topic - DO NOT USE";
	}

	@Override
	public void setDisableMessageID(boolean value) throws JMSException {
		
		
	}

	@Override
	public boolean getDisableMessageID() throws JMSException {
		
		return false;
	}

	@Override
	public void setDisableMessageTimestamp(boolean value) throws JMSException {
		
		
	}

	@Override
	public boolean getDisableMessageTimestamp() throws JMSException {
		
		return false;
	}

	@Override
	public void setDeliveryMode(int deliveryMode) throws JMSException {
		
		
	}

	@Override
	public int getDeliveryMode() throws JMSException {
		
		return 0;
	}

	@Override
	public void setPriority(int defaultPriority) throws JMSException {
		
		
	}

	@Override
	public int getPriority() throws JMSException {
		
		return 0;
	}

	@Override
	public void setTimeToLive(long timeToLive) throws JMSException {
		
		
	}

	@Override
	public long getTimeToLive() throws JMSException {
		
		return 0;
	}

	@Override
	public Destination getDestination() throws JMSException {
		
		return null;
	}

	@Override
	public void send(Message message) throws JMSException {
		
		
	}

	@Override
	public void send(Message message, int deliveryMode, int priority,
			long timeToLive) throws JMSException {
		
		
	}

	@Override
	public void send(Destination destination, Message message)
			throws JMSException {
		
		
	}

	@Override
	public void send(Destination destination, Message message,
			int deliveryMode, int priority, long timeToLive)
			throws JMSException {
		
		
	}

	@Override
	public String getMessageSelector() throws JMSException {
		
		return null;
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		
		return null;
	}

	@Override
	public void setMessageListener(MessageListener listener)
			throws JMSException {
		
		
	}

	@Override
	public Message receive() throws JMSException {
		
		return null;
	}

	@Override
	public Message receive(long timeout) throws JMSException {
		
		return null;
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		
		return null;
	}

	@Override
	public void close() throws JMSException {
		
		
	}

	@Override
	public void publish(Message message) throws JMSException {
		
		
	}

	@Override
	public void publish(Message message, int deliveryMode, int priority,
			long timeToLive) throws JMSException {
		
		
	}

	@Override
	public void publish(Topic topic, Message message) throws JMSException {
		
		
	}

	@Override
	public void publish(Topic topic, Message message, int deliveryMode,
			int priority, long timeToLive) throws JMSException {
		
		
	}

	@Override
	public Topic getTopic() throws JMSException {
		
		return this;
	}

	@Override
	public boolean getNoLocal() throws JMSException {
		
		return false;
	}

	
}
