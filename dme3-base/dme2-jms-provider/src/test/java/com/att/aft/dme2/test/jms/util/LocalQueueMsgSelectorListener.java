/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.test.jms.util;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

public class LocalQueueMsgSelectorListener implements javax.jms.MessageListener
{
	private QueueConnection connection = null;
	private Queue dest = null;
	private QueueReceiver receiver = null;
	private QueueSession session = null;
	private String msgSelector = null;
	
	public LocalQueueMsgSelectorListener(QueueConnection connection, QueueSession session, Queue dest, String msgSelector)
	{
		this.connection = connection;
		this.session = session;
		this.dest = dest;
		this.msgSelector = msgSelector;
	}
	
    public void start() throws JMSException 
    {
        session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
        receiver = session.createReceiver(dest);
        receiver.setMessageListener(this);
    }
    
    public void stop() throws JMSException 
    {
        //receiver.setMessageListener(null);
        try
        {
        	if(receiver != null)
        		receiver.close();
        }
        catch (Exception e)
        {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        try
        {
	        if(session != null)
	        	session.close();
        }
        catch (Exception e)
        {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
    }

    public void setReceiver(QueueReceiver receiver)
    {
    	this.receiver = receiver;
    }
    
	@Override
    public void onMessage(Message msg)
    {
	    try
        {
	        Queue replyTo = (Queue)msg.getJMSReplyTo();
	        if(replyTo != null)
	        {
	        	TextMessage replyMsg = session.createTextMessage();

	        	replyMsg.setJMSCorrelationID(msg.getJMSMessageID());
	        	replyMsg.setJMSExpiration(System.currentTimeMillis() + 60000);
	        	
	        	MessageProducer producer = session.createProducer(replyTo);
	        	if(msg.getJMSMessageID().equals(msgSelector)) {
		        	replyMsg.setText("LocalQueueMessageListener:::" + ((TextMessage)msg).getText());
	        		producer.send(replyMsg);
		        	producer.close();
		        	System.out.println("LocalQueueMessageListener thread=" + Thread.currentThread().getId() + " responded to " + replyTo.getQueueName());	
	        	}
	        	else {
		        	replyMsg.setText("MSG SELECTOR MISMATCH");
	        		producer.send(replyMsg);
		        	producer.close();
		        	System.out.println("LocalQueueMessageListener msgSelector mismatch thread=" + Thread.currentThread().getId() + " responded to " + replyTo.getQueueName());
	        	}
	        }
	        else
	        {
	        	
	        }
        }
        catch (JMSException e)
        {
        	e.printStackTrace();
        	throw new RuntimeException(e);
        }
	    
    }

}

