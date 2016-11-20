/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.jms;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.XATopicSession;
import javax.transaction.xa.XAResource;

import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;

public class DME2JMSTopicSession implements TopicSession, XATopicSession {

	private static final Logger logger = LoggerFactory.getLogger(DME2JMSTopicSession.class.getName());

	private static final DME2JMSTopic topic = new DME2JMSTopic();
	private MessageListener listener;

	public DME2JMSTopicSession() {
		logger.warn(null, "DME2JMSTopicSession", "*****  Creating Dummy Topic Session, WILL NOT BE USABLE!! *****");
	}

	protected DME2JMSTopicSession(DME2JMSQueueConnection connection, boolean transacted, int ackMode) {
		logger.warn(null, "DME2JMSTopicSession", "*****  Creating Dummy Topic Session, WILL NOT BE USABLE!! *****");
	}

	@Override
	public Session getSession() throws JMSException {
		return this;
	}

	@Override
	public XAResource getXAResource() {
		return null;
	}

	@Override
	public BytesMessage createBytesMessage() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public MapMessage createMapMessage() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public Message createMessage() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public ObjectMessage createObjectMessage() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public StreamMessage createStreamMessage() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TextMessage createTextMessage() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TextMessage createTextMessage(String text) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public boolean getTransacted() throws JMSException {
		return false;
	}

	@Override
	public int getAcknowledgeMode() throws JMSException {
		return 0;
	}

	@Override
	public void commit() throws JMSException {
	}

	@Override
	public void rollback() throws JMSException {
	}

	@Override
	public void close() throws JMSException {
	}

	@Override
	public void recover() throws JMSException {
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return listener;
	}

	@Override
	public void setMessageListener(MessageListener listener) throws JMSException {
		this.listener = listener;
	}

	@Override
	public void run() {
	}

	@Override
	public MessageProducer createProducer(Destination destination) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal)
			throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public Queue createQueue(String queueName) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public QueueBrowser createBrowser(Queue queue) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TopicSession getTopicSession() throws JMSException {
		return this;
	}

	@Override
	public Topic createTopic(String topicName) throws JMSException {
		logger.warn(null, "createTopic", "*****  Returning Dummy Topic Connection Topic for: " + topicName + " *****");
		return topic;
	}

	@Override
	public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TopicSubscriber createSubscriber(Topic topic, String messageSelector, boolean noLocal) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal)
			throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TopicPublisher createPublisher(Topic topic) throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		throw new DME2JMSNotImplementedException("Not available in Dummy Topic emulation");
	}

	@Override
	public void unsubscribe(String name) throws JMSException {
		logger.warn(null, "unsubscribe", "*****  Unsubscribing Dummy Topic Connection subscriber " + name + "*****");
	}

}
